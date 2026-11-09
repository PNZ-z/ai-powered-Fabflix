#!/usr/bin/env python3
"""B1.5a embedding evaluation harness (stdlib only, user-approved D-4).

Scope: measure ``recall@5`` (the D8 gate), ``MRR@10`` and task-level success for movie-card
embeddings over the frozen 21,087-row snapshot, using the 60-query golden set in
``docs/ai/golden/queries.b1-5.json``.

This is the single harness for both B1.5a stages. Stage 1 (golden set) only needs ``--dry-run``;
stage 2 runs the full 21,087-card build with ``--run-embedding --max-cards 21087``. The
evaluation input is still the frozen golden set in ``docs/ai/golden/queries.b1-5.json``.

Modes
-----
``--dry-run``    (default) DB export, card rendering, fingerprints, plan. No network, no vectors.
``--self-test``  Exercise cosine top-k / recall@5 / MRR@10 on deterministic hash vectors.
                 Labelled SELF-TEST: these numbers are NOT a model measurement.
``--check-endpoint``  Probe the Ollama endpoint (``/api/tags``) and record model digests.
``--probe-truncation``  Find where ``prompt_eval_count`` saturates (plan `OPEN-a2`).
``--cold-start``  Unload each model (``keep_alive: 0``) and time the first call back in.
``--card-stats``  Card count/character-length statistics per template (report §2.2 evidence).
``--determinism`` Re-embed the same queries several times and compare the vectors bit for bit.
``--run-embedding``   Build vectors for at most ``--max-cards`` cards and score the golden set.

Data contract: cards follow ``docs/architecture/movie-cards.md`` §2 (``movie-card/v1``,
``zh-full`` primary, ``en-short`` control). Fingerprints follow ``docs/ai/README.md`` §2:
``template_hash`` / ``db_snapshot_hash`` / ``schema_hash = N/A`` (the A route uses no tools).

Only ``docs/ai/golden/**`` is read as evaluation input; no development documentation is chunked,
embedded or indexed (user decision D-1/D-5).
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import sys
import time
import urllib.error
import urllib.request
from array import array
from heapq import nlargest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from check_golden import (  # noqa: E402  (shared read-only DB helper, no side effects)
    QUERIES_PATH,
    REPO_ROOT,
    SNAPSHOT_EXPORT_SQL,
    ReadOnlyMysql,
    db_snapshot_hash,
    load_json,
    sha256_of,
)

CARD_VERSION = "movie-card/v1"
SCHEMA_HASH = "N/A"  # A route: no tool schema is used
OLLAMA_URL = "http://localhost:11434"
DEFAULT_MODELS = ("bge-m3:latest", "qwen3-embedding:0.6b")
MODEL_DIMS = {"bge-m3:latest": 1024, "qwen3-embedding:0.6b": 1024}
# Measured with ``--probe-truncation`` (2026-11-09, this endpoint): ``prompt_eval_count`` saturates
# at these values regardless of the declared ``context_length`` (8192 / 32768). ``OPEN-a2``.
MEASURED_TRUNCATION = {"bge-m3:latest": 2048, "qwen3-embedding:0.6b": 4095}
EVAL_TOP_K = 5
EVAL_MRR_K = 10
EMBED_BATCH = 32
DEFAULT_KEEP_ALIVE = "30m"
DEFAULT_OUT_DIR = REPO_ROOT / ".pi" / "artifacts"

# movie-cards.md §2.1. ``null_segments`` are the literal spans that a missing value replaces, so a
# movie without genres never renders as "Genres: ." and one without a rating never renders as
# "Rating: /10".
TEMPLATES = {
    "zh-full": {
        "template_id": "zh-full",
        "literal": "电影《{title}》（{year}）导演 {director}；类型：{genres}；主演：{stars}（前 3）；评分 {rating}/10。",
        "field_order": "title,year,director,genres,stars,rating",
        "null_placeholder": "评分 暂无",
        "null_segments": {
            "rating": ("评分 {rating}/10", "评分 暂无"),
            "genres": ("类型：{genres}", "类型：暂无"),
            # MC-U8 (empty stars) is not finalised in movie-cards.md §9; this is the provisional
            # rendering and is kept explicit so a later decision only touches this table.
            "stars": ("主演：{stars}（前 3）", "主演：暂无（前 3）"),
        },
    },
    "en-short": {
        "template_id": "en-short",
        "literal": "{title} ({year}) directed by {director}. Genres: {genres}. Rating: {rating}.",
        "field_order": "title,year,director,genres,rating",
        "null_placeholder": "Rating: N/A",
        "null_segments": {
            "rating": ("Rating: {rating}", "Rating: N/A"),
            "genres": ("Genres: {genres}", "Genres: N/A"),
        },
    },
}


class EvalError(RuntimeError):
    """Raised for unrecoverable evaluation errors (endpoint down, contract violation)."""


# --------------------------------------------------------------------------------------
# Fingerprints (docs/ai/README.md §2)
# --------------------------------------------------------------------------------------

def template_hash(template_id: str) -> str:
    spec = TEMPLATES[template_id]
    payload = "|".join([
        CARD_VERSION, spec["template_id"], spec["literal"], spec["field_order"],
        spec["null_placeholder"],
    ])
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


# --------------------------------------------------------------------------------------
# Snapshot export and card rendering
# --------------------------------------------------------------------------------------

def export_snapshot(mysql: ReadOnlyMysql) -> dict[str, dict]:
    """Rows keyed by movie id: id/title/year/director/genres/stars3/rating (business order)."""
    snapshot: dict[str, dict] = {}
    for rid, title, year, director, genres_sorted, _stars_dict_order, rating in mysql.select(
        SNAPSHOT_EXPORT_SQL
    ):
        snapshot[rid] = {
            "id": rid,
            "title": title,
            "year": int(year),
            "director": director,
            "genres": [g for g in genres_sorted.split(",")] if genres_sorted != "NULL" else [],
            "stars3": [],  # filled by attach_top_stars (business order, see below)
            "rating": None if rating == "NULL" else float(rating),
            "genres_sorted": genres_sorted,
            "stars3_sorted": _stars_dict_order,
        }
    return snapshot


def attach_top_stars(mysql: ReadOnlyMysql, snapshot: dict[str, dict]) -> None:
    """Top-3 stars per movie: star_movie_count DESC, name ASC (movie-cards.md §1.1).

    ``stars_in_movies`` has no appearance-count column, so the count is derived from the same
    table (a star's total number of movie links), matching MovieListRepository's ordering.

    Ids are matched case-insensitively: the snapshot stores ``movies.id`` in one case and 176
    ``stars_in_movies.movie_id`` rows in another (e.g. movies.id='AAN14' vs
    stars_in_movies.movie_id='AAn14'). MySQL's default collation joins them, so a case-sensitive
    Python join would silently drop the stars of 36 movies (3,388 instead of 3,352 cards without
    stars). ``movies.id`` has no case-collisions, so casefolded keys are unambiguous.
    """
    links = mysql.select(
        "SELECT sm.movie_id, s.name FROM stars_in_movies sm JOIN stars s ON s.id = sm.star_id"
    )
    per_star: dict[str, int] = {}
    per_movie: dict[str, list[str]] = {}
    for movie_id, star_name in links:
        per_star[star_name] = per_star.get(star_name, 0) + 1
        per_movie.setdefault(movie_id.casefold(), []).append(star_name)
    for row in snapshot.values():
        names = per_movie.get(row["id"].casefold(), [])
        names.sort(key=lambda name: (-per_star[name], name))
        row["stars3"] = names[:3]


def render_card(row: dict, template_id: str) -> str:
    spec = TEMPLATES[template_id]
    text = spec["literal"]
    for field, (span, null_text) in spec["null_segments"].items():
        if field == "rating" and row["rating"] is None:
            text = text.replace(span, null_text)
        elif field == "genres" and not row["genres"]:
            text = text.replace(span, null_text)
        elif field == "stars" and not row["stars3"]:
            text = text.replace(span, null_text)
    return text.format(
        title=row["title"],
        year=row["year"],
        director=row["director"] or "N/A",
        genres=", ".join(row["genres"]),
        stars=", ".join(row["stars3"]),
        rating="" if row["rating"] is None else f"{row['rating']:.1f}",
    )


# --------------------------------------------------------------------------------------
# Ollama client (urllib only; no requests dependency)
# --------------------------------------------------------------------------------------

class OllamaClient:
    def __init__(self, base_url: str = OLLAMA_URL, timeout_s: float = 180.0) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout_s = timeout_s

    def _post(self, path: str, payload: dict) -> dict:
        request = urllib.request.Request(
            self.base_url + path,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        return self._send(request)

    def _get(self, path: str) -> dict:
        return self._send(urllib.request.Request(self.base_url + path, method="GET"))

    def _send(self, request: urllib.request.Request) -> dict:
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_s) as response:
                return json.loads(response.read().decode("utf-8"))
        except (urllib.error.URLError, TimeoutError, ConnectionError) as exc:
            raise EvalError(
                f"cannot reach {request.full_url}: {exc}. On macOS, if curl works but this "
                f"fails, it is the per-app local-network permission rather than a dead endpoint."
            ) from exc

    def tags(self) -> dict[str, dict]:
        return {model["name"]: model for model in self._get("/api/tags").get("models", [])}

    def embed(self, model: str, texts: list[str], keep_alive: str | None = None) -> tuple[list[list[float]], dict]:
        """Embed one batch; returns (vectors, raw response counters)."""
        payload: dict = {"model": model, "input": texts}
        if keep_alive is not None:
            payload["keep_alive"] = keep_alive
        response = self._post("/api/embed", payload)
        vectors = response.get("embeddings") or []
        if len(vectors) != len(texts):
            raise EvalError(f"{model}: {len(vectors)} embeddings for {len(texts)} inputs")
        return vectors, response


# --------------------------------------------------------------------------------------
# Retrieval and metrics
# --------------------------------------------------------------------------------------

def cosine_ranking(query_vector: list[float], matrix: list[array], norms: list[float],
                   ids: list[str], top_k: int) -> list[tuple[str, float]]:
    """Top-k by cosine similarity. ``math.sumprod`` keeps 21k x 1024 dots inside C."""
    query_norm = math.sqrt(math.sumprod(query_vector, query_vector)) or 1.0
    scored = [
        (math.sumprod(query_vector, vector) / (query_norm * (norm or 1.0)), index)
        for index, (vector, norm) in enumerate(zip(matrix, norms))
    ]
    return [(ids[index], similarity) for similarity, index in nlargest(top_k, scored)]


def percentile(values: list[float], fraction: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    low, high = math.floor(position), math.ceil(position)
    if low == high:
        return ordered[low]
    return ordered[low] * (high - position) + ordered[high] * (position - low)


def score_query(ranked_ids: list[str], acceptable: list[str]) -> tuple[bool, int | None]:
    """(hit in top-5, 1-based rank of the first acceptable id or None)."""
    accepted = set(acceptable)
    for rank, movie_id in enumerate(ranked_ids[:EVAL_MRR_K], start=1):
        if movie_id in accepted:
            return rank <= EVAL_TOP_K, rank
    return False, None


def summarise(results: list[dict]) -> dict:
    """recall@5 (D8 gate), MRR@10 and task-level success, overall and per track."""
    def block(rows: list[dict]) -> dict:
        if not rows:
            return {"n": 0, "recall@5": None, "mrr@10": None, "task_success": None}
        hits = sum(1 for row in rows if row["hit_at_5"])
        reciprocal = sum(1.0 / row["rank"] for row in rows if row["rank"])
        return {
            "n": len(rows),
            "recall@5": round(hits / len(rows), 4),
            "mrr@10": round(reciprocal / len(rows), 4),
            "task_success": round(hits / len(rows), 4),
        }

    summary = {"overall": block(results)}
    for track in ("zh_zh", "zh_en", "en_en"):
        summary[track] = block([row for row in results if row["track"] == track])
    return summary


# --------------------------------------------------------------------------------------
# Modes
# --------------------------------------------------------------------------------------

def load_golden() -> dict:
    document = load_json(QUERIES_PATH)
    queries = document["queries"]
    if len(queries) != 60:
        raise EvalError(f"golden set has {len(queries)} queries, expected 60 (run check_golden.py)")
    return document


def sample_ids(golden: dict, snapshot: dict[str, dict], limit: int) -> list[tuple[str, str]]:
    """Sample rows with the reason they were picked: one golden hit per query, then boundaries."""
    picked: list[tuple[str, str]] = []
    seen: set[str] = set()
    for case in golden["queries"]:
        for movie_id in case["acceptable_movie_ids"]:
            if movie_id in snapshot and movie_id not in seen:
                picked.append((movie_id, f"golden {case['id']} acceptable"))
                seen.add(movie_id)
                break
        if len(picked) >= limit:
            break
    for label, wanted in (
        ("no rating", lambda row: row["rating"] is None),
        ("no genres", lambda row: not row["genres"]),
        ("no stars", lambda row: not row["stars3"]),
    ):
        extra = next((movie_id for movie_id in sorted(snapshot)
                      if wanted(snapshot[movie_id]) and movie_id not in seen), None)
        if extra:
            picked.append((extra, label))
            seen.add(extra)
    return picked


def check_golden_ids(golden: dict, snapshot: dict[str, dict]) -> None:
    known = set(snapshot)
    missing = {case["id"]: [mid for mid in case["acceptable_movie_ids"] if mid not in known]
               for case in golden["queries"]}
    missing = {case: ids for case, ids in missing.items() if ids}
    if missing:
        raise EvalError(f"golden ids absent from the snapshot: {missing}")
    print(f"golden acceptable ids resolvable in snapshot: "
          f"{sum(len(case['acceptable_movie_ids']) for case in golden['queries'])} ids, 0 missing")


def print_fingerprints(snapshot: dict[str, dict], export_rows: list[list[str]]) -> dict:
    fingerprints = {
        "card_version": CARD_VERSION,
        "db_snapshot_hash": db_snapshot_hash(export_rows),
        "row_count": len(snapshot),
        "year_range": f"{min(row['year'] for row in snapshot.values())}-"
                      f"{max(row['year'] for row in snapshot.values())}",
        "movies_without_genres": sum(1 for row in snapshot.values() if not row["genres"]),
        "movies_without_rating": sum(1 for row in snapshot.values() if row["rating"] is None),
        "movies_without_stars": sum(1 for row in snapshot.values() if not row["stars3"]),
        "schema_hash": SCHEMA_HASH,
        "template_hash": {tpl: template_hash(tpl) for tpl in TEMPLATES},
    }
    print("fingerprints:")
    for key, value in fingerprints.items():
        print(f"  {key}: {value}")
    return fingerprints


def run_dry(args: argparse.Namespace) -> int:
    golden = load_golden()
    mysql = ReadOnlyMysql()
    export_rows = mysql.select(SNAPSHOT_EXPORT_SQL)
    snapshot = export_snapshot(mysql)
    attach_top_stars(mysql, snapshot)
    print(f"snapshot: {len(snapshot)} cards from {golden['snapshot']['source']}")
    print(f"golden:   {golden['golden_version']} "
          f"sha256={sha256_of(QUERIES_PATH)}")
    check_golden_ids(golden, snapshot)
    print_fingerprints(snapshot, export_rows)

    picked = sample_ids(golden, snapshot, args.sample)
    print(f"\nsample cards ({len(picked)} ids):")
    for movie_id, reason in picked:
        print(f"  [{movie_id}] ({reason})")
        print(f"    zh-full  : {render_card(snapshot[movie_id], 'zh-full')}")
        print(f"    en-short : {render_card(snapshot[movie_id], 'en-short')}")

    templates = ["zh-full", "en-short"] if args.template == "both" else [args.template]
    tokens_per_card = {"zh-full": 48.0, "en-short": 25.3}  # bge-m3 measurements (movie-cards.md §2.2)
    print("\nplanned matrix (NOT executed in dry-run):")
    for model in args.models:
        for template in templates:
            tokens = len(snapshot) * tokens_per_card[template]
            vectors_mb = len(snapshot) * MODEL_DIMS.get(model, 1024) * 4 / (1024 ** 2)
            print(f"  {model:<24} {template:<8} cards={len(snapshot)} "
                  f"~{tokens/1e6:.2f}M tokens ~{vectors_mb:.0f} MB fp32")
    print(f"\nd8 gate: recall@{EVAL_TOP_K} >= 0.80 (pass/fail); MRR@{EVAL_MRR_K} and task-level "
          f"success are reported but do not decide the gate.")
    print("dry-run complete: no embedding was requested, no vectors were built.")
    return 0


def run_self_test(args: argparse.Namespace) -> int:
    """Exercise the retrieval maths on deterministic hash vectors (no model is involved)."""
    golden = load_golden()
    cases = golden["queries"][:max(args.sample, 1)]

    def fake_vector(text: str) -> list[float]:
        """32-dim deterministic vector - only exercises the ranking code, not semantics."""
        return [byte / 255.0 for byte in hashlib.sha256(text.encode("utf-8")).digest()]

    movie_ids = sorted({movie_id for case in cases for movie_id in case["acceptable_movie_ids"]})
    matrix = [array("f", fake_vector(f"card:{movie_id}")) for movie_id in movie_ids]
    norms = [math.sqrt(math.sumprod(vector, vector)) for vector in matrix]
    results = []
    for case in cases:
        ranked = cosine_ranking(fake_vector(case["query"]), matrix, norms, movie_ids, EVAL_MRR_K)
        hit, rank = score_query([movie_id for movie_id, _ in ranked], case["acceptable_movie_ids"])
        results.append({"id": case["id"], "track": case["track"], "hit_at_5": hit, "rank": rank,
                        "top": [movie_id for movie_id, _ in ranked[:EVAL_TOP_K]]})
    print("SELF-TEST ONLY - hash vectors, not model output; numbers below are not measurements.")
    for row in results:
        print(f"  {row['id']} {row['track']} hit@5={row['hit_at_5']} rank={row['rank']} "
              f"top5={row['top']}")
    print(f"  summary: {json.dumps(summarise(results), ensure_ascii=False)}")
    return 0


def run_check_endpoint(args: argparse.Namespace) -> int:
    client = OllamaClient(args.base_url)
    tags = client.tags()
    for model in args.models:
        info = tags.get(model)
        if info is None:
            print(f"  {model:<24} MISSING (not installed or not loaded)")
            continue
        details = info.get("details", {})
        print(f"  {model:<24} digest={info.get('digest', '')} "
              f"size={info.get('size')} modified={info.get('modified_at')} "
              f"family={details.get('family')} quant={details.get('quantization_level')} "
              f"embedding_length={details.get('embedding_length')} "
              f"context={details.get('context_length')}")
    return 0


def run_probe_truncation(args: argparse.Namespace) -> int:
    """Measure where ``prompt_eval_count`` stops growing (plan `OPEN-a2`).

    A single input is padded with repeated card-like sentences until the reported token count
    saturates; the largest count that still grows is a lower bound for the truncation threshold.
    """
    client = OllamaClient(args.base_url)
    sentence = "Movie Title (2001) directed by Some Director. Genres: Drama. Rating: 7.5. "
    for model in args.models:
        print(f"model {model}")
        previous = 0
        for repeats in (50, 100, 200, 400, 800, 1200, 1600):
            text = sentence * repeats
            _, raw = client.embed(model, [text], keep_alive=args.keep_alive)
            counted = raw.get("prompt_eval_count")
            print(f"  chars={len(text):>7} prompt_eval_count={counted} "
                  f"(delta {None if counted is None else counted - previous})")
            if counted is not None and counted == previous:
                print("  saturated: no further growth -> truncation threshold reached")
                break
            previous = counted if counted is not None else previous
    return 0


def run_cold_start(args: argparse.Namespace) -> int:
    client = OllamaClient(args.base_url)
    for model in args.models:
        cold_seconds = measure_cold_start(client, model, args.keep_alive)
        print(f"  {model}: cold start {cold_seconds:.2f} s (keep_alive:0 unload, then first call)")
    return 0


def measure_cold_start(client: OllamaClient, model: str, keep_alive: str) -> float:
    client.embed(model, ["cold start probe"], keep_alive="0")
    time.sleep(3.0)
    started = time.monotonic()
    client.embed(model, ["cold start probe"], keep_alive=keep_alive)
    return time.monotonic() - started


def embed_cards(client: OllamaClient, model: str, ids: list[str], cards: list[str],
                args: argparse.Namespace) -> dict:
    """Embed every card once; returns vectors + build statistics."""
    vectors: list[array] = []
    batch_seconds: list[float] = []
    batch_tokens: list[int] = []
    prompt_tokens = 0
    counted_batches = 0
    truncated_batches = 0
    threshold = args.truncation_threshold or MEASURED_TRUNCATION.get(model, 8192)
    started = time.monotonic()
    batches = range(0, len(cards), args.batch_size)
    for number, offset in enumerate(batches, start=1):
        batch = cards[offset:offset + args.batch_size]
        batch_started = time.monotonic()
        batch_vectors, raw = client.embed(model, batch, keep_alive=args.keep_alive)
        batch_seconds.append(time.monotonic() - batch_started)
        vectors.extend(array("f", vector) for vector in batch_vectors)
        counted = raw.get("prompt_eval_count")
        batch_tokens.append(counted if isinstance(counted, int) else 0)
        if isinstance(counted, int):
            prompt_tokens += counted
            counted_batches += 1
            if counted / len(batch) >= threshold:
                truncated_batches += 1
        total_batches = (len(cards) + args.batch_size - 1) // args.batch_size
        if total_batches <= 12 or number % 50 == 0 or offset + args.batch_size >= len(cards):
            elapsed = time.monotonic() - started
            print(f"    batch {number}/{total_batches} "
                  f"cards={min(offset + args.batch_size, len(cards))} "
                  f"batch_s={batch_seconds[-1]:.2f} elapsed={elapsed:.1f}s "
                  f"tokens={prompt_tokens}", flush=True)
    build_seconds = time.monotonic() - started
    norms = [math.sqrt(math.sumprod(vector, vector)) for vector in vectors]
    # The first batch of a fresh model carries load/compile cost; steady-state throughput is
    # computed without it so the report can separate "model load" from "embedding rate".
    steady_seconds = sum(batch_seconds[1:]) if len(batch_seconds) > 1 else 0.0
    steady_tokens = sum(batch_tokens[1:]) if len(batch_tokens) > 1 else 0
    return {
        "ids": ids,
        "vectors": vectors,
        "norms": norms,
        "dim": len(vectors[0]) if vectors else 0,
        "build_seconds": round(build_seconds, 2),
        "batches": len(batch_seconds),
        "batch_seconds_p50": round(percentile(batch_seconds, 0.5) or 0.0, 3),
        "batch_seconds_p95": round(percentile(batch_seconds, 0.95) or 0.0, 3),
        "batch_seconds_max": round(max(batch_seconds) if batch_seconds else 0.0, 3),
        "prompt_eval_count_total": prompt_tokens,
        "counted_batches": counted_batches,
        "first_batch_seconds": round(batch_seconds[0], 3) if batch_seconds else 0.0,
        "tokens_per_second": round(prompt_tokens / build_seconds, 1) if build_seconds else None,
        "tokens_per_second_steady": round(steady_tokens / steady_seconds, 1) if steady_seconds else None,
        "truncated_batches": truncated_batches,
        "truncation_threshold": threshold,
        "vector_bytes_fp32": len(vectors) * (len(vectors[0]) if vectors else 0) * 4,
    }


def score_template(client: OllamaClient, model: str, template_id: str, index: dict,
                   golden: dict, args: argparse.Namespace) -> list[dict]:
    """Embed the 60 queries and score every one of them against this build."""
    rows = []
    latencies: list[float] = []
    for case in golden["queries"]:
        started = time.monotonic()
        query_vectors, _ = client.embed(model, [case["query"]], keep_alive=args.keep_alive)
        latencies.append(time.monotonic() - started)
        ranked = cosine_ranking(query_vectors[0], index["vectors"], index["norms"], index["ids"],
                               EVAL_MRR_K)
        hit, rank = score_query([movie_id for movie_id, _ in ranked], case["acceptable_movie_ids"])
        rows.append({
            "id": case["id"], "track": case["track"], "intent": case["intent"],
            "cjk_alias_only": bool(case.get("cjk_alias_only")),
            "hit_at_5": hit, "rank": rank,
            "top": [movie_id for movie_id, _ in ranked[:EVAL_TOP_K]],
            "scores": [round(score, 6) for _, score in ranked[:EVAL_TOP_K]],
        })
    for row in rows:
        row["zero_result"] = not row["top"]
    index["query_latency_s"] = {
        "n": len(latencies),
        "p50": round(percentile(latencies, 0.5) or 0.0, 4),
        "p95": round(percentile(latencies, 0.95) or 0.0, 4),
        "mean": round(sum(latencies) / len(latencies), 4) if latencies else None,
        "max": round(max(latencies), 4) if latencies else None,
    }
    return rows


def write_combination(out_dir: Path, stem: str, header: dict, rows: list[dict]) -> None:
    (out_dir / f"{stem}__summary.json").write_text(
        json.dumps(header, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with (out_dir / f"{stem}__queries.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["query_id", "track", "intent", "cjk_alias_only", "hit_at_5", "rank",
                         "zero_result", "top5", "scores"])
        for row in rows:
            writer.writerow([row["id"], row["track"], row["intent"], row["cjk_alias_only"],
                             row["hit_at_5"], row["rank"], row["zero_result"],
                             " ".join(row["top"]), " ".join(map(str, row["scores"]))])


def gate_rows(by_template: dict[str, list[dict]]) -> list[dict]:
    """D8 pairing: zh_zh queries score against zh-full cards, zh_en/en_en against en-short."""
    rows = []
    for row in by_template["zh-full"]:
        if row["track"] == "zh_zh":
            rows.append(dict(row, template_id="zh-full"))
    for row in by_template["en-short"]:
        if row["track"] in ("zh_en", "en_en"):
            rows.append(dict(row, template_id="en-short"))
    return rows


def run_card_stats(args: argparse.Namespace) -> int:
    """Print card counts and character-length statistics (the numbers quoted in report §2.2)."""
    mysql = ReadOnlyMysql()
    snapshot = export_snapshot(mysql)
    attach_top_stars(mysql, snapshot)
    print(f"cards={len(snapshot)}")
    for template_id in ("zh-full", "en-short"):
        lengths = sorted(len(render_card(row, template_id)) for row in snapshot.values())
        median = percentile(lengths, 0.5)
        print(f"{template_id}: chars min={lengths[0]} median={median:.0f} max={lengths[-1]} "
              f"mean={sum(lengths)/len(lengths):.1f}")
    for label, predicate in (("no genres", lambda row: not row["genres"]),
                             ("no rating", lambda row: row["rating"] is None),
                             ("no stars", lambda row: not row["stars3"])):
        print(f"{label}: {sum(1 for row in snapshot.values() if predicate(row))}")
    return 0


def run_determinism(args: argparse.Namespace) -> int:
    """Re-embed the same queries ``--repeat`` times and report bit-level identity."""
    golden = load_golden()
    client = OllamaClient(args.base_url)
    queries = [case["query"] for case in golden["queries"][: args.sample]]
    for model in args.models:
        runs = []
        for _ in range(args.repeat):
            vectors, _ = client.embed(model, queries, keep_alive=args.keep_alive)
            runs.append(vectors)
        identical = 0
        max_delta = 0.0
        for index in range(len(queries)):
            first = runs[0][index]
            if all(run[index] == first for run in runs[1:]):
                identical += 1
            for run in runs[1:]:
                max_delta = max(max_delta, max(abs(a - b) for a, b in zip(first, run[index])))
        dims = len(runs[0][0])
        norms = [math.sqrt(math.sumprod(vector, vector)) for vector in runs[0]]
        print(f"{model}: queries={len(queries)} runs={args.repeat} dim={dims} "
              f"identical_vectors={identical}/{len(queries)} max_abs_delta={max_delta} "
              f"l2_norm_min={min(norms):.6f} l2_norm_max={max(norms):.6f}")
    return 0


def run_embedding(args: argparse.Namespace) -> int:
    if args.max_cards <= 0:
        raise EvalError("--run-embedding requires a positive --max-cards bound")
    golden = load_golden()
    mysql = ReadOnlyMysql()
    export_rows = mysql.select(SNAPSHOT_EXPORT_SQL)
    snapshot = export_snapshot(mysql)
    attach_top_stars(mysql, snapshot)
    check_golden_ids(golden, snapshot)

    fingerprints = {
        "card_version": CARD_VERSION,
        "db_snapshot_hash": db_snapshot_hash(export_rows),
        "template_hash": {tpl: template_hash(tpl) for tpl in TEMPLATES},
        "schema_hash": SCHEMA_HASH,
        "golden_version": golden["golden_version"],
        "golden_sha256": sha256_of(QUERIES_PATH),
        "golden_snapshot_block": golden["snapshot"],
    }
    print(f"fingerprints: {json.dumps(fingerprints, ensure_ascii=False)}", flush=True)

    client = OllamaClient(args.base_url)
    tags = client.tags()
    templates = ["zh-full", "en-short"] if args.template == "both" else [args.template]
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    ids = sorted(snapshot)[: args.max_cards]

    for model in args.models:
        if model not in tags:
            raise EvalError(f"{model} is not installed on the endpoint")
        info = tags[model]
        digest = info.get("digest", "")
        model_dims = info.get("details", {}).get("embedding_length") or MODEL_DIMS.get(model)
        print(f"\n=== model {model} digest={digest} dim={model_dims} ===", flush=True)
        cold_seconds = measure_cold_start(client, model, args.keep_alive)
        print(f"  cold start (keep_alive:0 then first call): {cold_seconds:.2f} s", flush=True)

        by_template: dict[str, list[dict]] = {}
        build_stats: dict[str, dict] = {}
        for template_id in templates:
            cards = [render_card(snapshot[movie_id], template_id) for movie_id in ids]
            print(f"  building {len(cards)} cards for {template_id}", flush=True)
            index = embed_cards(client, model, ids, cards, args)
            build_stats[template_id] = {key: value for key, value in index.items()
                                        if key not in ("ids", "vectors", "norms")}
            rows = score_template(client, model, template_id, index, golden, args)
            by_template[template_id] = rows
            summary = summarise(rows)
            header = {
                "model": model, "model_digest": digest, "dim": index["dim"],
                "template_id": template_id, "template_hash": template_hash(template_id),
                "cards": len(ids), "build": build_stats[template_id],
                "query_latency_s": index["query_latency_s"], "cold_start_s": round(cold_seconds, 2),
                "zero_result_queries": sum(1 for row in rows if row["zero_result"]),
                "keep_alive": args.keep_alive, "endpoint": args.base_url,
                "fingerprints": fingerprints, "summary": summary,
            }
            write_combination(out_dir, f"{model.replace(':', '_')}__{template_id}", header, rows)
            print(f"  {template_id}: recall@5={summary['overall']['recall@5']} "
                  f"mrr@10={summary['overall']['mrr@10']} "
                  f"build={build_stats[template_id]['build_seconds']}s "
                  f"tokens={build_stats[template_id]['prompt_eval_count_total']} "
                  f"q_p50={index['query_latency_s']['p50']}s q_p95={index['query_latency_s']['p95']}s",
                  flush=True)
            del index

        if all(template in by_template for template in ("zh-full", "en-short")):
            rows = gate_rows(by_template)
            summary = summarise(rows)
            gate = {
                "model": model, "model_digest": digest, "dim": model_dims,
                "pairing": {"zh_zh": "zh-full", "zh_en": "en-short", "en_en": "en-short"},
                "cold_start_s": round(cold_seconds, 2),
                "zero_result_queries": sum(1 for row in rows if row["zero_result"]),
                "cjk_alias_only": [{"id": row["id"], "hit_at_5": row["hit_at_5"]}
                                   for row in rows if row["cjk_alias_only"]],
                "fingerprints": fingerprints, "summary": summary,
                "rows": [{"id": row["id"], "track": row["track"], "template_id": row["template_id"],
                          "hit_at_5": row["hit_at_5"], "rank": row["rank"],
                          "top": row["top"]} for row in rows],
            }
            (out_dir / f"{model.replace(':', '_')}__gate.json").write_text(
                json.dumps(gate, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            print(f"  GATE (D8 pairing): recall@5={summary['overall']['recall@5']} "
                  f"mrr@10={summary['overall']['mrr@10']}", flush=True)
        # free the server-side model before the next candidate
        client.embed(model, ["unload"], keep_alive="0")

    print("\nall combinations finished", flush=True)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dry-run", action="store_true",
                        help="default mode: export snapshot, render sample cards, print plan")
    parser.add_argument("--self-test", action="store_true",
                        help="exercise retrieval maths with hash vectors (not model output)")
    parser.add_argument("--check-endpoint", action="store_true", help="probe Ollama /api/tags")
    parser.add_argument("--probe-truncation", action="store_true",
                        help="find where prompt_eval_count saturates (plan OPEN-a2)")
    parser.add_argument("--cold-start", action="store_true",
                        help="unload each model (keep_alive:0) and time the first call")
    parser.add_argument("--card-stats", action="store_true",
                        help="print card counts and character-length statistics")
    parser.add_argument("--determinism", action="store_true",
                        help="re-embed the same queries and compare vectors bit for bit")
    parser.add_argument("--repeat", type=int, default=3,
                        help="repetitions for --determinism (default 3)")
    parser.add_argument("--run-embedding", action="store_true",
                        help="build vectors and score the golden set (requires --max-cards)")
    parser.add_argument("--max-cards", type=int, default=0,
                        help="embed at most this many cards (21087 for the full build)")
    parser.add_argument("--models", default=",".join(DEFAULT_MODELS),
                        help="comma-separated Ollama model names")
    parser.add_argument("--template", default="both", choices=("both", "zh-full", "en-short"))
    parser.add_argument("--sample", type=int, default=3, help="sample cards / self-test queries")
    parser.add_argument("--batch-size", type=int, default=EMBED_BATCH)
    parser.add_argument("--truncation-threshold", type=int, default=0,
                        help="per-card prompt_eval_count at or above this counts as a truncation; "
                             "0 = use the value measured by --probe-truncation for that model")
    parser.add_argument("--keep-alive", default=DEFAULT_KEEP_ALIVE,
                        help="Ollama keep_alive sent with every request (avoids re-loads)")
    parser.add_argument("--base-url", default=OLLAMA_URL)
    parser.add_argument("--out", default=str(DEFAULT_OUT_DIR), help="CSV/JSON output directory")
    args = parser.parse_args()
    args.models = [model.strip() for model in args.models.split(",") if model.strip()]

    if args.run_embedding:
        print("!! full/partial embedding build requested by the operator")
        return run_embedding(args)
    if args.check_endpoint:
        return run_check_endpoint(args)
    if args.card_stats:
        return run_card_stats(args)
    if args.determinism:
        return run_determinism(args)
    if args.probe_truncation:
        return run_probe_truncation(args)
    if args.cold_start:
        return run_cold_start(args)
    if args.self_test:
        return run_self_test(args)
    return run_dry(args)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except EvalError as error:
        print(f"eval error: {error}", file=sys.stderr)
        sys.exit(2)

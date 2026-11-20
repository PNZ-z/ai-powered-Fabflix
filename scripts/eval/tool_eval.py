#!/usr/bin/env python3
"""B1.5b tool-routing evaluation harness (stdlib only, D-4).

What this file measures: whether a variant selects the right tool, fills the right slots,
refuses when it must, keeps arguments inside ``docs/ai/tool-schema.b1-5.json``, and whether the
executed rows are grounded in the catalog. Stage 2 adds the full 60 + 15 run, the G1 second hop
(seed resolution), grounding re-check, latency percentiles and G2's replay of G0's calls.

Variants (B1.5b plan §5, B1.5c plan §4.3)
-----------------------------------------
``G0``  pure LLM routing: the model picks the tool and the arguments, the server only executes.
``G1``  deterministic routing in front: keyword/dictionary pre-classification, enum mapping,
        coverage check and controlled slot filling, no model call at all.
``G1-T`` G1 plus the query-time title resolver (B1.5c decision C4=D): alias/cache, then 1-3 English
        candidates from the local model **only for a CJK title the catalog cannot match exactly**,
        then catalog exact/prefix verification, then clarification. ``G1`` is the baseline that
        never resolves a Chinese title (``model_calls = 0`` there, always).
``G2``  G0's routing plus server-side schema validation and enum/alias mapping (the model still
        chooses; the server rejects or maps). G2 reuses G0's requests, so it never doubles them.
``G3``  **not implemented this period** (``OPEN-b4`` = optional / not required). Asking for it
        exits 2 and says "not measured" instead of silently substituting G2.

Execution semantics (B1.5c, plan §5.1/§6.2/§7)
---------------------------------------------
A multi-value ``genres[]`` / ``preferred_genres[]`` means **all of them** (AND). Values are
canonicalized first, so ``Sci-Fi`` and ``Science Fiction`` are one condition, never two. Director
equality follows ``docs/ai/aliases/person-normalization.json``: ``~`` becomes a space, comparison is
case-folded, and an abbreviation (``M.Mann``) equals a full name only when the surname and the first
initial agree - a bare surname is never expanded (``Spielberg`` is not ``Steven Spielberg``;
plan §5.4). ``find_similar_movies`` narrows the pool by the constraints the **router extracted from
the query text** and only then ranks (same director, shared genre count, |year difference|, rating);
the golden annotations are read by ``score_case`` and by nothing else.

Alias data is loaded from ``docs/ai/aliases/*.json`` (``aliases_version``); the harness keeps no
hard-coded genre or person table.

Tool execution coverage
-----------------------
``search_movies_by_title`` / ``filter_movies`` / ``get_movie_card`` run read-only SQL.
``find_similar_movies`` and ``recommend_movies`` run a **deterministic evaluation stand-in** so the
15 ``similar_movies`` and 10 ``recommend_by_preference`` cases can be measured at all:

* ``recommend_movies`` filters by the explicit preferences it receives (genres / year range /
  rating / director / seed ids) and orders by rating, which is what the tool description says it
does. The tool's arguments are the user's constraints, so no extra semantics are invented.
* ``find_similar_movies`` narrows the candidate pool with the constraints the router extracted
from the query text (genres AND, year window, director) and then ranks it (same director desc,
shared genre count desc, |year difference| asc, rating desc) and never returns the seed. The
narrowing is a ``WHERE`` clause, so ``SIMILAR_POOL`` truncation can never drop a row the user
asked for (plan §6.2). User-stated conditions are enforced **server-side** (``R7-2``: a stated
condition may not be silently dropped), because the tool schema has no parameters to carry them.

**The ranking weights above are an evaluation stand-in, not the approved production ranking.**
This file contains no production code; the production ``find_similar_movies`` / ``recommend_movies``
ranking remains the decision of the implementation issue (``OPEN-b5``). The report states the
stand-in and reports routing-level numbers separately from task-level numbers.

``--self-test`` feeds scripted tool calls through the G0/G2 scoring path and scripted candidates
through the G1-T resolver. It exercises the plumbing (validation, enum mapping, coverage states,
canonical genre AND, director equality, catalog verification, scoring, the "execution never reads
a golden annotation" sentinel) without a model and is labelled SELF-TEST: those records are NOT a
model measurement.

``G2 --reuse-g0 G0.json`` replays G0's recorded calls instead of calling the model again, so G2
differs from G0 **only** by the server-side layer (one ablation variable, no second sampling).

Usage
-----
    python3 scripts/eval/tool_eval.py --dry-run
    python3 scripts/eval/tool_eval.py --variant G1 --cases both --allow-full-run --out G1.json
    python3 scripts/eval/tool_eval.py --variant G0 --cases both --allow-full-run \\
        --base-url http://host:11434 --out G0.json
    python3 scripts/eval/tool_eval.py --variant G2 --cases both --allow-full-run \\
        --reuse-g0 G0.json --out G2.json
    python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \\
        --queries-path docs/ai/golden/queries-heldout.b1-5c.json \\
        --routing-path docs/ai/golden/query-routing-heldout.b1-5c.json \\
        --no-cache --allow-full-run --out G1T-cold.json
    python3 scripts/eval/tool_eval.py --variant G3            # exits 2: not measured

Exit code 0 = the requested mode finished, 2 = refused / unavailable input.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from check_golden import REPO_ROOT, ReadOnlyMysql, load_json  # noqa: E402
from check_heldout import HELDOUT_PATH as HELDOUT_QUERIES_PATH  # noqa: E402  (independence check)
from check_schema import (  # noqa: E402  (fixtures + shared hash/enum helpers)
    GENRE_ALIASES_PATH,
    PERSON_NORMALIZATION_PATH,
    QUERIES_PATH,
    ROUTING_PATH,
    TITLE_ALIASES_PATH,
    TOOL_CASES_PATH,
    TOOL_SCHEMA_PATH,
    genre_enum,
    schema_hash,
)

DEFAULT_MODEL = "qwen2.5:7b"
DEFAULT_BASE_URL = os.environ.get("EVAL_OLLAMA_BASE_URL", "http://localhost:11434")
DEFAULT_TIMEOUT_S = 180.0
TEMPERATURE = 0
# The prompt text below is unchanged from the stage-1 skeleton; only the id was renamed now that
# the harness carries the measurement stage. The report records prompt_sha256, not only the id.
PROMPT_ID = "b1-5b-inline/v1"
PROMPT_TEXT = (
    "You are the movie catalog assistant of Fabflix. You answer only from the results of the "
    "provided tools and never invent titles, years, directors, genres, stars, ratings or movie "
    "ids. Answer in the language of the user."
)
YEAR_MIN, YEAR_MAX = 196, 2024
RATING_COVERAGE_FROM = 2001
SAMPLED_DEFAULT_LIMIT = 5

# --------------------------------------------------------------------------------------
# Alias / normalization data (B1.5c: external files, no hard-coded table)
# --------------------------------------------------------------------------------------
# ``docs/ai/aliases/genre-aliases.json``      user genre word -> canonical key (an enum value)
# ``docs/ai/aliases/person-normalization.json`` director equality rule (surname + first initial)
# ``docs/ai/aliases/title-aliases.json``      verified Chinese title -> movie id (frozen empty, C10)
GENRE_ALIAS_DOC = load_json(GENRE_ALIASES_PATH)
PERSON_NORMALIZATION_DOC = load_json(PERSON_NORMALIZATION_PATH)
TITLE_ALIAS_DOC = load_json(TITLE_ALIASES_PATH)


def canonical_key_map(alias_doc: dict) -> dict[str, str]:
    """enum value -> canonical key (identity for every value outside a canonical group)."""
    mapping: dict[str, str] = {}
    for group in alias_doc.get("canonical_groups", []):
        for value in group:
            mapping[value] = group[0]
    return mapping


def canonical_member_lists(alias_doc: dict) -> dict[str, list[str]]:
    """canonical key -> every catalog value that denotes it."""
    return {group[0]: list(group) for group in alias_doc.get("canonical_groups", [])}


GENRE_CANONICAL = canonical_key_map(GENRE_ALIAS_DOC)
GENRE_CANONICAL_MEMBERS = canonical_member_lists(GENRE_ALIAS_DOC)
# Deterministic routing vocabulary (G1). Chinese genre words must be mapped server-side: the
# measured baseline failure was the model submitting genre values in Chinese (scout §实测 3).
GENRE_ALIASES = {term.casefold(): list(values)
                 for term, values in GENRE_ALIAS_DOC["aliases"].items()}


def sql_literal(value: object) -> str:
    return "'" + str(value).replace("'", "''") + "'"


def canonical_genres(values: list | tuple) -> list[str]:
    """Fold a genre list onto canonical keys and drop duplicates (plan §5.1, oracle P2-1).

    ``Sci-Fi`` and ``Science Fiction`` become one key, so a multi-value filter needs one row
    match, not two. Literal expansion would require a row to carry both spellings (0 rows).
    """
    keys: list[str] = []
    for value in values:
        key = GENRE_CANONICAL.get(str(value), str(value))
        if key not in keys:
            keys.append(key)
    return keys


def canonical_genre_case_sql(column: str = "g.name") -> str:
    """SQL expression folding a catalog genre value onto its canonical key."""
    variants = {value: key for key, group in GENRE_CANONICAL_MEMBERS.items()
                for value in group[1:]}
    if not variants:
        return column
    branches = " ".join(f"WHEN {column} = {sql_literal(value)} THEN {sql_literal(key)}"
                        for value, key in sorted(variants.items()))
    return f"CASE {branches} ELSE {column} END"


def genre_member_sql(keys: list[str]) -> str:
    members = sorted({member for key in keys
                      for member in GENRE_CANONICAL_MEMBERS.get(key, [key])})
    return ",".join(sql_literal(member) for member in members)


def genre_and_sql(keys: list[str], movie_column: str = "m.id") -> str:
    """"Every canonical key present" as one SQL predicate (canonicalize first, then AND)."""
    return (f"{movie_column} IN (SELECT gm.movie_id FROM genres_in_movies gm "
            f"JOIN genres g ON g.id = gm.genre_id "
            f"WHERE g.name IN ({genre_member_sql(keys)}) GROUP BY gm.movie_id "
            f"HAVING COUNT(DISTINCT {canonical_genre_case_sql()}) = {len(keys)})")


# ``X.Lastname`` / ``X.Y.Lastname`` abbreviations: initials first, surname last.
DIRECTOR_INITIALS_RE = re.compile(r"^((?:[a-z]\.\s*)+)(.*)$")
DIRECTOR_SUFFIX_RE = re.compile(r"\s+(?:jr|sr)\.?$", re.IGNORECASE)


def director_parts(value: str) -> tuple[str, str | None]:
    """(surname, first initial or None) after ``~``->space, case-fold and suffix removal."""
    text = (value or "").replace("~", " ")
    text = DIRECTOR_SUFFIX_RE.sub("", text.strip())
    text = re.sub(r"\s+", " ", text).strip().casefold()
    if not text:
        return "", None
    match = DIRECTOR_INITIALS_RE.match(text)
    if match:
        initials = re.findall(r"[a-z]", match.group(1))
        return match.group(2).strip(" ."), (initials[0] if initials else None)
    tokens = text.split(" ")
    if len(tokens) == 1:
        return tokens[0], None
    return " ".join(tokens[1:]), tokens[0][0]


def directors_equal(left: str, right: str) -> bool:
    """``person-normalization.json`` equality: same surname, same first initial.

    A bare surname never equals a full name (``Spielberg`` != ``Steven Spielberg``), because that
    would replace the answer set: Q17 is 5 rows for the full name and 29 for the surname.
    """
    if normalized(left) == normalized(right):
        return True
    left_surname, left_initial = director_parts(left)
    right_surname, right_initial = director_parts(right)
    return (bool(left_surname) and left_surname == right_surname
            and left_initial is not None and left_initial == right_initial)


def alias_pattern(token: str) -> re.Pattern:
    """Word-bounded match for Latin terms, substring match for CJK terms.

    Substring matching on a short Latin alias such as ``war`` would fire inside ``toward`` or
    ``Edward`` and silently add a genre the user never named.
    """
    if token.isascii():
        return re.compile(r"(?<![a-z0-9])" + re.escape(token) + r"(?![a-z0-9])")
    return re.compile(re.escape(token))


GENRE_ALIAS_PATTERNS = [(token, alias_pattern(token))
                        for token in sorted(GENRE_ALIASES, key=len, reverse=True)]

# G1-T resolver (plan §4.3/§4.5/§4.6): local model, strict JSON, 1-3 English candidates.
RESOLVER_PROMPT_ID = "b1-5c-title-resolver/v1"
RESOLVER_PROMPT = (
    "You convert a movie title written in Chinese characters into the English original title used "
    "by an English-language movie catalog (the catalog stores English titles only, so transliterate "
    "the well-known original title, do not describe the plot). Reply with strict JSON only, no prose "
    "and no markdown: "
    '{"candidates": ["Title A", "Title B"]} with 1 to 3 candidates, most likely first. Use only '
    "English original titles. Never translate a genre word, a person name or the word 'movie'."
)
RESOLVER_TIMEOUT_S = 8.0
RESOLVER_CACHE_VERSION = "b1-5c-resolver-cache/v1"
DEFAULT_RESOLVER_CACHE = REPO_ROOT / ".pi" / "artifacts" / "b1-5c-resolver-cache.json"
HAN_RE = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")


def has_han(text: str | None) -> bool:
    """True when the text carries Chinese characters (not CJK punctuation such as 《》)."""
    return bool(HAN_RE.search(text or ""))
TRANSACTION_PATTERNS = (r"买.{0,20}票", r"退.{0,4}款", r"下.{0,4}单", r"付.{0,4}款")
REFUSAL_RULES = (
    # "剧情" alone is also the Drama genre word ("剧情片"), so the plot branch needs a plot verb.
    ("plot", ("剧情是什么", "剧情介绍", "剧情简介", "剧情如何", "讲了什么", "讲什么",
              "故事梗概", "故事讲了", "plot", "what is it about", "synopsis")),
    ("transaction", ("买票", "购票", "下单", "支付", "退款", "订单", "ticket", "refund", "checkout")),
    ("internal", ("项目", "开发文档", "用了什么模型", "系统提示", "工具定义", "system prompt",
                  "tool definition", "which model")),
    ("external", ("豆瓣", "imdb", "rotten tomatoes", "烂番茄")),
    # B1.5e F-1: fields the catalog cannot answer (R4-1 lists 获奖/票房 as must-refuse). Markers are
    # whole phrases matched as substrings, so "score" (rating), "music" (genre), "released"
    # ("released in 1999") and bare "studio" ("Studio Ghibli") are deliberately absent.
    ("out_of_catalog", ("票房", "预算", "发行公司", "配乐", "作曲", "奥斯卡", "获奖", "奖项",
                        "box office", "grossed", "budget", "distributor", "which studio",
                        "who composed", "composer", "academy award", "oscars")),
)
SIMILAR_MARKERS = ("类似", "相似", "像", "那种", "这种", "那样", "这样", "一样的",
                   "like", "similar", "loved", "love ")
RECOMMEND_MARKERS = ("推荐", "recommend", "suggest")
CHINESE_DECADES = {"五十": 1950, "六十": 1960, "七十": 1970, "八十": 1980, "九十": 1990, "二十": 2020}
COVERAGE = {"year_min": YEAR_MIN, "year_max": YEAR_MAX,
            "rating_coverage_from": RATING_COVERAGE_FROM}
# Optional follow-up of the deterministic router: resolve a title, then use its id as the seed.
SEED_FOLLOW_UP = "find_similar_movies"
# Candidate pool for the find_similar_movies stand-in before result constraints are applied.
SIMILAR_POOL = 300
# Seed lookup is a server-side step, so it may ask for the schema maximum instead of the
# user-facing default of 5 (the tool description allows a server-chosen limit).
SEED_SEARCH_LIMIT = 10


class EvalError(Exception):
    """Raised for unrecoverable evaluation errors (endpoint down, broken fixture)."""


def repo_relative(path: Path | str) -> str:
    """Path relative to the repository when possible, absolute otherwise (report/record field)."""
    resolved = Path(path).resolve()
    try:
        return resolved.relative_to(REPO_ROOT).as_posix()
    except ValueError:
        return str(resolved)


# --------------------------------------------------------------------------------------
# Fixtures and cases
# --------------------------------------------------------------------------------------

def load_fixtures(queries_path: Path = QUERIES_PATH,
                  routing_path: Path = ROUTING_PATH) -> tuple[dict, dict, dict, dict]:
    """Schema, tool cases, queries and routing annotations (paths are overridable for held-out)."""
    schema_doc = load_json(TOOL_SCHEMA_PATH)
    cases_doc = load_json(TOOL_CASES_PATH)
    queries_doc = load_json(queries_path)
    routing_doc = load_json(routing_path)
    recorded = schema_doc.get("schema_hash")
    recomputed = schema_hash(schema_doc["tools"])
    if recorded != recomputed:
        raise EvalError(f"schema_hash mismatch: recorded {recorded}, recomputed {recomputed}")
    annotates = (routing_doc.get("annotates") or {}).get("sha256")
    actual = hashlib.sha256(queries_path.read_bytes()).hexdigest()
    if annotates != actual:
        # Pairs the routing annotations with the queries they were written for. Without this a
        # held-out run could silently score the frozen annotations against another query file.
        raise EvalError(
            f"{repo_relative(routing_path)}.annotates.sha256 {annotates} != "
            f"sha256({repo_relative(queries_path)}) {actual}"
        )
    return schema_doc, cases_doc, queries_doc, routing_doc


def build_cases(cases_doc: dict, queries_doc: dict, routing_doc: dict, selection: str,
                ids: list[str] | None) -> list[dict]:
    """Normalise both fixture shapes into one case record."""
    cases: list[dict] = []
    if selection in ("tool", "both"):
        for case in cases_doc["tool_cases"]:
            cases.append({
                "case_id": case["id"],
                "kind": "tool",
                "text": case["query"],
                "lang": case["lang"],
                "intent": case["category"],
                "expected_tools": case.get("expected_tools", []),
                "required_slots": {},
                "accept_alternative_slots": [],
                "result_constraints": case.get("result_constraints", {}),
                "expected_refusal": case["expected_behavior"] == "refuse_no_tool",
                "allows_clarification": False,
                "cjk_title": False,
                "expected_behavior": case["expected_behavior"],
                "expected_error_code": case.get("expected_error_code", "NONE"),
                "acceptable_movie_ids": case.get("acceptable_movie_ids", []),
            })
    if selection in ("queries", "both"):
        queries = {query["id"]: query for query in queries_doc["queries"]}
        for entry in routing_doc["annotations"]:
            query = queries[entry["query_id"]]
            acceptable_ids = query.get("acceptable_movie_ids", [])
            refusal = bool(entry["expected_refusal"])
            if not acceptable_ids and not refusal:
                # B1.5a froze an acceptable set for every non-refusal query. Scoring without it
                # silently turned task success into "returned any row", which is how
                # Q12/Q16/Q35/Q55/Q56 were reported as successes while missing the golden set.
                raise EvalError(
                    f"{entry['query_id']} has no acceptable_movie_ids in the queries file; "
                    f"task-level success cannot be scored"
                )
            cases.append({
                "case_id": entry["query_id"],
                "kind": "query",
                "text": query["query"],
                "lang": query["lang"],
                "intent": entry["intent"],
                "expected_tools": entry["expected_tools"],
                "required_slots": entry["required_slots"],
                "accept_alternative_slots": entry.get("accept_alternative_slots", []),
                "result_constraints": entry.get("result_constraints", {}) or {},
                "expected_refusal": entry["expected_refusal"],
                "allows_clarification": bool(query.get("allows_clarification")
                                              or entry.get("allows_clarification")),
                "cjk_title": bool(query.get("cjk_title")),
                "expected_behavior": ("refuse_no_tool" if entry["expected_refusal"]
                                      else "call_tools_and_answer"),
                "expected_error_code": "NONE",
                "acceptable_movie_ids": acceptable_ids,
            })
    if ids:
        if isinstance(ids, str):
            ids = ids.split(",")
        wanted = {case_id.strip().upper() for case_id in ids if case_id.strip()}
        cases = [case for case in cases if case["case_id"] in wanted]
    return cases


# --------------------------------------------------------------------------------------
# Ollama client (urllib only, same shape as embed_eval.py)
# --------------------------------------------------------------------------------------

class OllamaClient:
    def __init__(self, base_url: str, timeout_s: float = DEFAULT_TIMEOUT_S) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout_s = timeout_s

    def _post(self, path: str, payload: dict) -> dict:
        request = urllib.request.Request(
            self.base_url + path,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_s) as response:
                return json.loads(response.read().decode("utf-8"))
        except (urllib.error.URLError, TimeoutError, ConnectionError) as exc:
            raise EvalError(
                f"cannot reach {request.full_url}: {exc}. On macOS, if curl works but this fails, "
                f"it is the per-app local-network permission rather than a dead endpoint."
            ) from exc

    def chat_with_tools(self, model: str, messages: list[dict], tools: list[dict]) -> tuple[dict, float]:
        payload = {
            "model": model,
            "messages": messages,
            "tools": tools,
            "stream": False,
            "options": {"temperature": TEMPERATURE},
        }
        started = time.perf_counter()
        response = self._post("/api/chat", payload)
        return response, (time.perf_counter() - started) * 1000.0

    def generate_json(self, model: str, system_prompt: str, user_text: str,
                      timeout_s: float) -> tuple[str | None, float, bool]:
        """One strict-JSON completion over ``/api/chat``: (content, latency_ms, timed_out).

        ``/api/chat`` is the same endpoint the routing variants use, so the resolver shares their
        transport and credential path; ``format: json`` enforces strict JSON output. A timeout is
        a degraded outcome the resolver turns into a clarification (plan §4.6), so it is reported
        instead of raising; an unreachable endpoint stays fatal.
        """
        payload = {
            "model": model,
            "messages": [{"role": "system", "content": system_prompt},
                         {"role": "user", "content": user_text}],
            "stream": False,
            "format": "json",
            "options": {"temperature": TEMPERATURE},
        }
        request = urllib.request.Request(
            self.base_url + "/api/chat",
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        started = time.perf_counter()
        try:
            with urllib.request.urlopen(request, timeout=timeout_s) as response:
                body = json.loads(response.read().decode("utf-8"))
        except (TimeoutError, ConnectionError):
            return None, (time.perf_counter() - started) * 1000.0, True
        except urllib.error.URLError as exc:
            if isinstance(exc.reason, TimeoutError):
                return None, (time.perf_counter() - started) * 1000.0, True
            raise EvalError(f"cannot reach {request.full_url}: {exc}") from exc
        content = (body.get("message") or {}).get("content")
        return content, (time.perf_counter() - started) * 1000.0, False

    def tags(self) -> dict[str, str]:
        """model name -> digest, so the report can pin the model it actually measured."""
        request = urllib.request.Request(self.base_url + "/api/tags", method="GET")
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_s) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except (urllib.error.URLError, TimeoutError, ConnectionError) as exc:
            raise EvalError(f"cannot reach {request.full_url}: {exc}") from exc
        return {model["name"]: model.get("digest") for model in payload.get("models", [])}


def _replayed_digest(replay: dict) -> str | None:
    for record in replay.values():
        digest = record.get("model_digest")
        if digest:
            return digest
    return None


def parse_tool_calls(message: dict) -> list[dict]:
    calls = []
    for raw in message.get("tool_calls") or []:
        function = raw.get("function") or {}
        arguments = function.get("arguments")
        if isinstance(arguments, str):
            try:
                arguments = json.loads(arguments)
            except json.JSONDecodeError:
                arguments = {"__unparsed__": arguments}
        calls.append({"name": function.get("name"), "arguments": arguments or {}})
    return calls


# --------------------------------------------------------------------------------------
# Schema validation (G2) and enum mapping
# --------------------------------------------------------------------------------------

def tool_parameters(schema_doc: dict, name: str) -> dict | None:
    for tool in schema_doc["tools"]:
        if tool["function"]["name"] == name:
            return tool["function"]["parameters"]
    return None


def validate_call(schema_doc: dict, name: str, arguments: dict) -> list[str]:
    """Server-side argument validation against the fixture (types, enum, bounds, anyOf)."""
    params = tool_parameters(schema_doc, name)
    if params is None:
        return [f"unknown tool {name!r}"]
    problems: list[str] = []
    properties = params.get("properties", {})
    for key in params.get("required", []):
        if key not in arguments:
            problems.append(f"missing required {key!r}")
    for key, value in arguments.items():
        spec = properties.get(key)
        if spec is None:
            problems.append(f"unknown field {key!r}")
            continue
        expected = spec.get("type")
        if expected == "integer" and not (isinstance(value, int) and not isinstance(value, bool)):
            problems.append(f"{key}: expected integer, got {type(value).__name__}")
            continue
        if expected == "number" and (isinstance(value, bool)
                                     or not isinstance(value, (int, float))):
            problems.append(f"{key}: expected number, got {type(value).__name__}")
            continue
        if expected == "string" and not isinstance(value, str):
            problems.append(f"{key}: expected string, got {type(value).__name__}")
            continue
        if expected == "boolean" and not isinstance(value, bool):
            problems.append(f"{key}: expected boolean, got {type(value).__name__}")
            continue
        if expected == "array":
            if not isinstance(value, list):
                problems.append(f"{key}: expected array, got {type(value).__name__}")
                continue
            item_enum = spec.get("items", {}).get("enum")
            if item_enum is not None:
                for item in value:
                    if item not in item_enum:
                        problems.append(f"{key}: {item!r} not in enum")
            if spec.get("minItems") and len(value) < spec["minItems"]:
                problems.append(f"{key}: fewer than {spec['minItems']} items")
            if spec.get("maxItems") and len(value) > spec["maxItems"]:
                problems.append(f"{key}: more than {spec['maxItems']} items")
            continue
        if "enum" in spec and value not in spec["enum"]:
            problems.append(f"{key}: {value!r} not in enum")
        if isinstance(value, (int, float)) and not isinstance(value, bool):
            if "minimum" in spec and value < spec["minimum"]:
                problems.append(f"{key}: {value} < minimum {spec['minimum']}")
            if "maximum" in spec and value > spec["maximum"]:
                problems.append(f"{key}: {value} > maximum {spec['maximum']}")
    for branch in params.get("anyOf", []):
        if any(key in arguments for key in branch["required"]):
            break
    else:
        if params.get("anyOf"):
            problems.append("anyOf: none of the required condition keys is present")
    return problems


def map_genres(schema_doc: dict, values: list) -> tuple[list, list]:
    """Map user-language genre words onto canonical enum keys (server-side alias table)."""
    enum = genre_enum(schema_doc)
    mapped, unmapped = [], []
    for value in values:
        if value in enum:
            for key in canonical_genres([value]):
                if key not in mapped:
                    mapped.append(key)
            continue
        alias = GENRE_ALIASES.get(str(value).strip().casefold())
        if alias:
            for key in canonical_genres(alias):
                if key not in mapped:
                    mapped.append(key)
        else:
            unmapped.append(value)
    return mapped, unmapped


def year_bounds(arguments: dict) -> tuple[int | None, int | None]:
    lower = arguments.get("year_from")
    upper = arguments.get("year_to")
    year_range = arguments.get("year_range")
    if isinstance(year_range, dict):
        lower = year_range.get("from")
        upper = year_range.get("to")
    return (lower if isinstance(lower, int) else None,
            upper if isinstance(upper, int) else None)


def coverage_state(arguments: dict) -> tuple[str | None, dict]:
    """The three mutually exclusive states of ``ai-user-workflows.md`` §3.1 (R7-1).

    ``OUT_OF_RANGE``: a syntactically valid year outside 196-2024.
    ``EMPTY_RESULT``: a rating filter whose upper year bound is below the rating coverage start,
    i.e. a legal but unsatisfiable query - it must carry ``coverage.rating_coverage_from``.
    An open-ended ``year_from`` therefore never triggers it on its own.
    """
    lower, upper = year_bounds(arguments)
    if any(year is not None and (year < YEAR_MIN or year > YEAR_MAX) for year in (lower, upper)):
        return "OUT_OF_RANGE", {"coverage": COVERAGE}
    if arguments.get("min_rating") is not None and upper is not None \
            and upper < RATING_COVERAGE_FROM:
        return "EMPTY_RESULT", {"coverage": COVERAGE,
                                "note": "no title from 2000 or earlier has a rating row"}
    return None, {}


# --------------------------------------------------------------------------------------
# Tool execution (read-only SQL; see the module docstring for the two stand-ins)
# --------------------------------------------------------------------------------------

MOVIE_COLUMNS = """m.id, m.title, m.year, m.director,
        (SELECT GROUP_CONCAT(DISTINCT g.name ORDER BY g.name)
           FROM genres_in_movies gm JOIN genres g ON g.id = gm.genre_id
          WHERE gm.movie_id = m.id) AS genres,
        r.rating"""
MOVIE_FIELDS = ("id", "title", "year", "director", "genres", "rating")


def constraint_problems(row: dict, constraints: dict | None) -> list[str]:
    """Result-constraint violations of one row (``result_constraint_vocabulary``).

    ``genres`` means **all** of the listed genres after canonicalization (plan §5.1): a row must
    carry one genre per canonical key. Reading it as "at least one" is what made Q19/Q30/P-like
    multi-genre cases accept the wrong rows.
    """
    if not constraints:
        return []
    problems: list[str] = []
    genres = constraints.get("genres")
    if genres:
        row_keys = set(canonical_genres(row["genres"]))
        missing = [key for key in canonical_genres(genres) if key not in row_keys]
        if missing:
            problems.append(f"genres {row['genres']} miss {missing} of {genres}")
    if isinstance(constraints.get("year_from"), int) and row["year"] < constraints["year_from"]:
        problems.append(f"year {row['year']} < {constraints['year_from']}")
    if isinstance(constraints.get("year_to"), int) and row["year"] > constraints["year_to"]:
        problems.append(f"year {row['year']} > {constraints['year_to']}")
    if constraints.get("min_rating") is not None:
        rating = row["rating"]
        if rating is None or rating + 1e-9 < float(constraints["min_rating"]):
            problems.append(f"rating {row['rating']} < {constraints['min_rating']}")
    director = constraints.get("director")
    if director and not directors_equal(row["director"], director):
        problems.append(f"director {row['director']!r} != {director!r}")
    return problems


def constraint_sql(constraints: dict | None, executor: "ToolExecutor") -> list[str]:
    """SQL predicates for the user-stated result conditions (server-side enforcement)."""
    if not constraints:
        return []
    clauses: list[str] = []
    genres = constraints.get("genres")
    if genres:
        clauses.append(genre_and_sql(canonical_genres(genres)))
    if isinstance(constraints.get("year_from"), int):
        clauses.append(f"m.year >= {constraints['year_from']}")
    if isinstance(constraints.get("year_to"), int):
        clauses.append(f"m.year <= {constraints['year_to']}")
    if constraints.get("min_rating") is not None:
        clauses.append(f"r.rating >= {float(constraints['min_rating'])}")
    if constraints.get("director"):
        clauses.append(executor.director_predicate(str(constraints["director"])))
    return clauses


class ToolExecutor:
    def __init__(self, mysql: ReadOnlyMysql | None) -> None:
        self.mysql = mysql
        self._directors: list[str] | None = None

    # -- catalog-derived director equality (person-normalization.json) -------------------
    def catalog_directors(self) -> list[str]:
        """Every distinct director string, read once and reused by every comparison."""
        if self._directors is None:
            if self.mysql is None:
                self._directors = []
            else:
                self._directors = [row[0] for row in
                                   self.mysql.select("SELECT DISTINCT director FROM movies")
                                   if row[0] not in ("NULL", "")]
        return self._directors

    def matching_directors(self, value: str) -> list[str]:
        return [name for name in self.catalog_directors() if directors_equal(name, value)]

    def director_predicate(self, value: str) -> str:
        """``m.director`` predicate for one name using the equality rule, never an expansion."""
        names = self.matching_directors(value)
        if not names:
            return "1 = 0"
        return "m.director IN (" + ",".join(sql_literal(name) for name in names) + ")"

    def execute(self, name: str, arguments: dict, constraints: dict | None = None) -> dict:
        if self.mysql is None:
            return {"status": "not_executed", "reason": "--skip-db"}
        if name == "search_movies_by_title":
            return self._search(arguments)
        if name == "filter_movies":
            return self._filter(arguments)
        if name == "get_movie_card":
            return self._card(arguments)
        if name == "find_similar_movies":
            return self._similar(arguments, constraints)
        if name == "recommend_movies":
            return self._recommend(arguments, constraints)
        return {"status": "error", "error": "INVALID_ARGUMENT", "fields": {"field": "tool"}}

    @staticmethod
    def _row_from(row: list[str]) -> dict:
        return {"id": row[0], "title": row[1], "year": int(row[2]), "director": row[3],
                # the mysql CLI prints the string NULL for a missing value, not an empty field
                "genres": [] if row[4] in ("NULL", "") else [g for g in row[4].split(",") if g],
                "rating": None if row[5] in ("NULL", "") else float(row[5])}

    def _rows(self, sql: str) -> list[dict]:
        return [self._row_from(row) for row in self.mysql.select(sql)]

    def _ranked_rows(self, sql: str, extras: tuple[str, ...]) -> list[dict]:
        """Rows plus the trailing derived columns (match rank / overlap / same director)."""
        rows = []
        for row in self.mysql.select(sql):
            record = self._row_from(row)
            for offset, name in enumerate(extras, start=len(MOVIE_FIELDS)):
                record[name] = row[offset]
            rows.append(record)
        return rows

    def _search(self, arguments: dict) -> dict:
        """Exact match first, then prefix, then substring (plan §7): ``Heat`` is not truncated."""
        query = str(arguments.get("query", ""))
        literal = sql_literal(query)
        limit = int(arguments.get("limit", SAMPLED_DEFAULT_LIMIT))
        rank = (f"CASE WHEN LOWER(m.title) = LOWER({literal}) THEN 0 "
                f"WHEN LOWER(m.title) LIKE CONCAT(LOWER({literal}), '%') THEN 1 ELSE 2 END")
        sql = (f"SELECT {MOVIE_COLUMNS}, {rank} AS match_rank "
               f"FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id "
               f"WHERE m.title LIKE CONCAT('%', {literal}, '%')")
        if isinstance(arguments.get("year"), int):
            sql += f" AND m.year = {arguments['year']}"
        sql += f" ORDER BY match_rank, m.title, m.year LIMIT {limit}"
        rows = self._ranked_rows(sql, ("match_rank",))
        matches = {0: "exact", 1: "prefix", 2: "substring"}
        return {"status": "EMPTY_RESULT" if not rows else "OK", "results": rows,
                "match": "none" if not rows else matches.get(int(rows[0]["match_rank"]), "partial")}

    def title_candidates(self, candidate: str, limit: int = 10) -> list[dict]:
        """Catalog rows whose title equals the candidate (preferred) or starts with it.

        This is the resolver's step 3 (plan §4.3): extract/prefix only, never substring, so a
        candidate has to name something the catalog actually contains.
        """
        literal = sql_literal(candidate)
        rank = (f"CASE WHEN LOWER(m.title) = LOWER({literal}) THEN 0 ELSE 1 END")
        sql = (f"SELECT {MOVIE_COLUMNS}, {rank} AS match_rank "
               f"FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id "
               f"WHERE m.title LIKE CONCAT({literal}, '%') "
               f"ORDER BY match_rank, m.title, m.year LIMIT {limit}")
        rows = self._ranked_rows(sql, ("match_rank",))
        for row in rows:
            row["match"] = "exact" if int(row["match_rank"]) == 0 else "prefix"
        return rows

    def exact_title_ids(self, title: str, limit: int = 25) -> list[dict]:
        """Rows whose title **is** this title, no prefix rows: the parse-outcome oracle (plan §4.5)."""
        sql = (f"SELECT {MOVIE_COLUMNS} FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id "
               f"WHERE LOWER(m.title) = LOWER({sql_literal(title)}) "
               f"ORDER BY m.title, m.year LIMIT {limit}")
        return self._rows(sql)

    def _filter(self, arguments: dict) -> dict:
        state, meta = coverage_state(arguments)
        if state is not None:
            return {"status": state, "results": [], **meta}
        where = []
        genres = arguments.get("genres") or []
        if genres:
            where.append(genre_and_sql(canonical_genres(genres)))
        if isinstance(arguments.get("year_from"), int):
            where.append(f"m.year >= {arguments['year_from']}")
        if isinstance(arguments.get("year_to"), int):
            where.append(f"m.year <= {arguments['year_to']}")
        if arguments.get("min_rating") is not None:
            where.append(f"r.rating >= {float(arguments['min_rating'])}")
        limit = int(arguments.get("limit", SAMPLED_DEFAULT_LIMIT))
        sql = (f"SELECT {MOVIE_COLUMNS} FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id")
        if where:
            sql += " WHERE " + " AND ".join(where)
        sql += f" ORDER BY m.title, m.year LIMIT {limit}"
        rows = self._rows(sql)
        return {"status": "EMPTY_RESULT" if not rows else "OK", "results": rows,
                "applied_filters": {key: value for key, value in arguments.items()
                                    if key not in ("limit", "sort")},
                "coverage": COVERAGE}

    def _card(self, arguments: dict) -> dict:
        movie_id = sql_literal(arguments.get("movie_id", ""))
        rows = self._rows(f"SELECT {MOVIE_COLUMNS} FROM movies m "
                          f"LEFT JOIN ratings r ON r.movie_id = m.id WHERE m.id = {movie_id}")
        if not rows:
            return {"status": "NOT_FOUND", "results": []}
        return {"status": "OK", "results": rows}

    def _similar(self, arguments: dict, constraints: dict | None) -> dict:
        """Evaluation stand-in: narrow by the router's constraints, then rank (plan §6.2).

        Order matters. Narrowing first is what reproduces the golden answer sets: ranking alone
        drops the "shared genre + year window" cases to 0-1 of 3 hits (scout §3.2). The
        ``SIMILAR_POOL`` cut happens after the ``WHERE``, so it can only drop the tail of a pool
        the user already narrowed.

        ``same_primary_genre_only`` is accepted by the schema and **ignored**: narrowing the seed
        genre set to one key would change the approved semantics (plan §6.2 / ``OPEN-b5``).
        """
        seed_id = sql_literal(arguments.get("movie_id", ""))
        seed_rows = self._rows(f"SELECT {MOVIE_COLUMNS} FROM movies m "
                               f"LEFT JOIN ratings r ON r.movie_id = m.id WHERE m.id = {seed_id}")
        if not seed_rows:
            return {"status": "NOT_FOUND", "results": [], "seed": None,
                    "reason": "movie_id is not a catalog id"}
        seed = seed_rows[0]
        seed_keys = canonical_genres(sorted(seed["genres"]))
        limit = int(arguments.get("limit", SAMPLED_DEFAULT_LIMIT))

        # User-stated conditions narrow the pool in SQL, not after ranking (R7-2).
        where = [f"m.id <> {seed_id}"] + constraint_sql(constraints, self)
        overlap_expr = "0"
        seed_genre_join = ""
        if seed_keys:
            overlap_expr = "COALESCE(og.overlap, 0)"
            seed_genre_join = (
                f" LEFT JOIN (SELECT gm.movie_id AS movie_id, "
                f"COUNT(DISTINCT {canonical_genre_case_sql()}) AS overlap "
                f"FROM genres_in_movies gm JOIN genres g ON g.id = gm.genre_id "
                f"WHERE g.name IN ({genre_member_sql(seed_keys)}) "
                f"GROUP BY gm.movie_id) og ON og.movie_id = m.id")
        same_director = self.director_predicate(seed["director"])
        sql = (f"SELECT {MOVIE_COLUMNS}, {overlap_expr} AS overlap, ({same_director}) AS same_director "
               f"FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id{seed_genre_join} "
               f"WHERE {' AND '.join(where)} "
               f"ORDER BY same_director DESC, overlap DESC, ABS(m.year - {seed['year']}), "
               f"r.rating IS NULL, r.rating DESC, m.title LIMIT {SIMILAR_POOL}")
        pool = self._ranked_rows(sql, ("overlap", "same_director"))
        ranked = [row for row in pool if not constraint_problems(row, constraints)]
        rows = ranked[:limit]
        return {
            "status": "OK" if rows else "EMPTY_RESULT",
            "results": rows,
            "seed": seed,
            "seed_excluded": all(row["id"] != seed["id"] for row in rows),
            "candidate_pool": len(pool),
            "constraints_applied": constraints or {},
            "ranking": "same director desc, shared seed genre count desc, |year diff| asc, "
                       "rating desc (evaluation stand-in)",
        }

    def _recommend(self, arguments: dict, constraints: dict | None) -> dict:
        """Filter by the explicit preferences the call carries; order by rating."""
        where = []
        preferred = arguments.get("preferred_genres") or []
        if preferred:
            where.append(genre_and_sql(canonical_genres(preferred)))
        year_range = arguments.get("year_range")
        if isinstance(year_range, dict):
            if isinstance(year_range.get("from"), int):
                where.append(f"m.year >= {year_range['from']}")
            if isinstance(year_range.get("to"), int):
                where.append(f"m.year <= {year_range['to']}")
        if arguments.get("min_rating") is not None:
            where.append(f"r.rating >= {float(arguments['min_rating'])}")
        if arguments.get("director"):
            where.append(self.director_predicate(str(arguments["director"])))
        seeds = [sql_literal(seed) for seed in arguments.get("seed_movie_ids") or []]
        if seeds and arguments.get("exclude_seed", True):
            where.append(f"m.id NOT IN ({','.join(seeds)})")
        limit = int(arguments.get("limit", SAMPLED_DEFAULT_LIMIT))
        sql = f"SELECT {MOVIE_COLUMNS} FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id"
        if where:
            sql += " WHERE " + " AND ".join(where)
        sql += f" ORDER BY r.rating IS NULL, r.rating DESC, m.title LIMIT {limit}"
        rows = [row for row in self._rows(sql) if not constraint_problems(row, constraints)]
        return {"status": "OK" if rows else "EMPTY_RESULT", "results": rows,
                "applied_preferences": {key: value for key, value in arguments.items()
                                        if key != "limit"},
                "ranking": "rating desc (evaluation stand-in)"}

    def ground(self, rows: list[dict]) -> tuple[bool, list[str]]:
        """Re-read every displayed row from the catalog and compare all six card fields."""
        if not rows or self.mysql is None:
            return True, []
        ids = sorted({str(row["id"]).replace("'", "''") for row in rows})
        quoted = ",".join("'" + movie_id + "'" for movie_id in ids)
        stored = {row["id"]: row for row in
                  self._rows(f"SELECT {MOVIE_COLUMNS} FROM movies m "
                             f"LEFT JOIN ratings r ON r.movie_id = m.id "
                             f"WHERE m.id IN ({quoted})")}
        problems: list[str] = []
        for row in rows:
            truth = stored.get(row["id"])
            if truth is None:
                problems.append(f"{row['id']}: not in catalog")
                continue
            for field in ("title", "year", "director", "rating"):
                if row[field] != truth[field]:
                    problems.append(f"{row['id']}.{field}: {row[field]!r} != {truth[field]!r}")
            if {normalized(genre) for genre in row["genres"]} != \
                    {normalized(genre) for genre in truth["genres"]}:
                problems.append(f"{row['id']}.genres: {row['genres']} != {truth['genres']}")
        return not problems, problems


# --------------------------------------------------------------------------------------
# G1 deterministic router
# --------------------------------------------------------------------------------------

def detect_refusal(text: str) -> str | None:
    lowered = text.lower()
    for category, markers in REFUSAL_RULES:
        if any(marker in lowered for marker in markers):
            return category
    if any(re.search(pattern, text) for pattern in TRANSACTION_PATTERNS):
        return "transaction"
    return None


def extract_genres(text: str) -> list[str]:
    """Canonical genre keys named in the text, using the alias file's vocabulary.

    Longer terms win (``musical`` before ``music``, ``sports`` before ``sport``) and Latin terms
    are matched on word boundaries, so ``war`` cannot fire inside ``toward``.
    """
    remaining = text.casefold()
    found: list[str] = []
    for token, pattern in GENRE_ALIAS_PATTERNS:
        if not pattern.search(remaining):
            continue
        for key in canonical_genres(GENRE_ALIASES[token]):
            if key not in found:
                found.append(key)
        remaining = pattern.sub(" ", remaining)
    return found


def extract_years(text: str) -> dict:
    slots: dict = {}
    explicit_range = re.search(
        r"(?:从\s*)?(?<!\d)(\d{4})\s*(?:年)?\s*(?:到|至|~)\s*(\d{4})\s*年?", text)
    if explicit_range:
        slots["year_from"], slots["year_to"] = (int(explicit_range.group(1)),
                                                int(explicit_range.group(2)))
    decade_zh = re.search(r"([五六七八九二])十\s*年代", text)
    if decade_zh:
        start = CHINESE_DECADES[decade_zh.group(1) + "十"]
        slots["year_from"], slots["year_to"] = start, start + 9
    decade = re.search(r"\b(19|20)(\d0)\s*'?s\b", text, re.IGNORECASE)
    if decade:
        start = int(decade.group(1) + decade.group(2))
        slots["year_from"], slots["year_to"] = start, start + 9
    explicit_decade = re.search(r"(19\d0|20\d0)\s*年代", text)
    if explicit_decade:
        start = int(explicit_decade.group(1))
        slots["year_from"], slots["year_to"] = start, start + 9
    short_decade = re.search(r"(?<!\d)(\d{2})\s*年代", text)
    if short_decade:
        decade = int(short_decade.group(1))
        start = 1900 + decade if decade >= 30 else 2000 + decade
        slots["year_from"], slots["year_to"] = start, start + 9
    between = re.search(r"(?:from|between)\s+(\d{4})\s*(?:to|and)\s+(\d{4})", text, re.IGNORECASE)
    if between:
        slots["year_from"], slots["year_to"] = int(between.group(1)), int(between.group(2))
    after = re.search(r"(\d{4})\s*(?:年)?\s*(?:以后|之后|后)", text) or \
        re.search(r"after\s+(\d{4})", text, re.IGNORECASE)
    if after:
        slots.setdefault("year_from", int(after.group(1)))
        # Open-ended above: the catalog ends at YEAR_MAX, so the range is the coverage window.
        slots.setdefault("year_to", YEAR_MAX)
    if "year_from" in slots and "year_to" in slots and slots["year_from"] > slots["year_to"]:
        slots["year_from"], slots["year_to"] = slots["year_to"], slots["year_from"]
    if not slots:
        # One stated year narrows to that year alone (T11: 2005 年 = year_from/year_to 2005).
        single = re.search(r"(?<!\d)(\d{4})\s*年", text) or \
            re.search(r"\bin\s+(\d{4})\b", text, re.IGNORECASE)
        if single and YEAR_MIN <= int(single.group(1)) <= YEAR_MAX:
            slots["year_from"] = slots["year_to"] = int(single.group(1))
    return slots


def extract_rating(text: str) -> float | None:
    match = re.search(r"([\d.]+)\s*分以上", text) or \
        re.search(r"rated\s+(?:at\s+least\s+)?([\d.]+)", text, re.IGNORECASE) or \
        re.search(r"([\d.]+)\s*(?:or higher|\+)", text, re.IGNORECASE)
    if match:
        return float(match.group(1))
    if "高分" in text or "highly rated" in text.lower():
        return 8.0
    return None


TITLE_PREFIX_MARKERS = (r"以\s*[A-Z][A-Za-z0-9'.:!/-]*(?:\s+[A-Za-z0-9'.:!/-]+)*\s*开头",
                        r"(?:title|name)\s+starts?\s+with", r"titles?\s+that\s+start")
EN_TITLE_RUN = r"([A-Z][A-Za-z0-9'.:!/-]*(?:\s+[A-Za-z0-9'.:!/-]+)*)"
# A verb of taste opens a title as often as an ordinary clause ("I liked Trainspotting" vs
# "movies I enjoyed in the 1990s"), so this pattern is kept separate: ``extract_title`` tests the
# capture's first token before accepting it.
TITLE_VERB_PATTERN = r"(?:like|liked|love|loved|enjoyed|watched)\s+" + EN_TITLE_RUN
TITLE_PATTERNS = (
    r"《([^》]+)》",
    r"(?:called|叫)\s*([A-Za-z][A-Za-z0-9' .:!/-]*?)(?:\s*的|\s*$|\s*\?|，|。)",
    r"以\s*" + EN_TITLE_RUN + r"\s*开头",
    EN_TITLE_RUN + r"\s*系列",
    r"(?:find|show|get|tell me about)\s+(?:me\s+)?(?:the\s+)?(?:movie|film)\s+" + EN_TITLE_RUN,
    r"(?:movie|film)\s+called\s+" + EN_TITLE_RUN,
    r"(?:something|anything|films?|movies?)\s+(?:like|similar to)\s+" + EN_TITLE_RUN,
    TITLE_VERB_PATTERN,
    r"(?:title|name)\s+starts?\s+with\s+" + EN_TITLE_RUN,
)
TITLE_CUTS = (r"\s+[-–—]\s+.*$", r"\s+anything similar.*$", r"\s+similar.*$",
              r"\s*系列.*$", r"\s+this (?:movie|film).*$")
# The marker that makes a bare Latin run a title candidate when no TITLE_PATTERNS entry matched.
LAST_RESORT_TITLE_MARKER = r"《|找|查|有没有|想看|is there|find\b|show me"
# Words that open a request but are never a title. Only the run's first token is tested, so
# "The Big Lebowski" stays a title while "Show me ..." cannot become one.
TITLE_STOPWORDS = frozenset({"show", "find", "get", "tell", "me", "i"})
# A clause word in the first token of a ``TITLE_VERB_PATTERN`` capture means the capture is the
# rest of the sentence ("movies I enjoyed **in** the 1990s"), not a title. Pronouns are absent:
# "I, Robot" starts with one.
FUNCTION_WORDS = frozenset({
    "a", "all", "an", "any", "as", "at", "be", "been", "but", "by", "for", "from", "had",
    "has", "have", "how", "in", "is", "it", "its", "my", "of", "on", "our", "she", "should",
    "some", "that", "the", "their", "them", "this", "those", "to", "was", "were", "what",
    "when", "where", "which", "who", "why", "with", "your",
})
# The same test against the genre vocabulary the alias file already defines: "I enjoyed comedy
# films from the 1990s" names Comedy, and masking "comedy films from the 1990s" as a title would
# delete both the genre and the decade from the stated constraints.
GENRE_TITLE_WORDS = frozenset(GENRE_ALIASES) | frozenset(
    target.casefold() for targets in GENRE_ALIASES.values() for target in targets)
# Nationality/language adjectives: "Recommend some Italian documentaries from the 1960s" names a
# genre, not a director, and a director predicate would turn the request into ``1 = 0``. Kept in
# the router, not in genre-aliases.json: those keys are genre vocabulary and are asserted against
# the catalog enum (check_schema.py: unsupported = enum - targets - variants - keys).
DEMONYM_STOPLIST = frozenset({
    "italian", "french", "japanese", "chinese", "korean", "german", "british", "english",
    "american", "indian", "spanish", "mexican", "canadian", "australian", "russian", "soviet",
    "swedish", "danish", "norwegian", "polish", "brazilian", "argentine", "iranian",
    "israeli", "turkish", "greek", "egyptian", "thai", "african", "european",
})
EN_DIRECTOR_RUN = r"([A-Z][A-Za-z.'-]*(?:\s+[A-Z][A-Za-z.'-]*){0,2})"
# Genre nouns a "recommend some <Name> <genre> ..." phrase can carry, taken from the alias file so
# the router keeps no private vocabulary. "recommend some Japanese movies" must not turn
# "Japanese" into a director name, so a bare movies/films is deliberately not in this list.
EN_GENRE_NOUNS = "|".join(re.escape(term) for term in
                          sorted(GENRE_ALIASES, key=len, reverse=True) if term.isascii())
DIRECTOR_PATTERNS = (
    r"(?:directed by|director)\s+([A-Z][A-Za-z.'-]*(?:\s+[A-Z][A-Za-z.'-]*)*)",
    r"(?:films?|movies?)\s+by\s+([A-Z][A-Za-z.'-]*(?:\s+[A-Z][A-Za-z.'-]*)*)",
    r"\bby\s+([A-Z][A-Za-z.'-]*(?:\s+[A-Z][A-Za-z.'-]*)*)",
    r"([A-Z][A-Za-z.'-]*(?:\s+[A-Z][A-Za-z.'-]*){0,2})\s*(?:执导|导演)",
    # 推荐几部 <Name> 的/在/从/1970 年代... - the name is followed by a clause, not only by 的.
    r"(?:推荐|想看|找)\s*(?:几部|一些)?\s*" + EN_DIRECTOR_RUN + r"\s*(?:的|在|从|(?=\d{4}))",
    # Recommend some <Name> thrillers from ... - the name is followed by a genre noun. The verb
    # carries both cases because a request often starts the sentence ("Recommend some ...").
    r"(?:[Rr]ecommend|suggest)\s+(?:me\s+)?(?:some|a few|any)?\s*" + EN_DIRECTOR_RUN
    + r"(?=\s+(?:\d{4}|" + EN_GENRE_NOUNS + r"))",
)


def has_title_prefix(text: str) -> bool:
    return any(re.search(pattern, text, re.IGNORECASE) for pattern in TITLE_PREFIX_MARKERS)


def opens_clause(value: str) -> bool:
    """True when a verb capture starts with a word no title starts with."""
    tokens = value.split()
    if not tokens:
        return True
    first = normalized(tokens[0])
    return first in FUNCTION_WORDS or first in GENRE_TITLE_WORDS


def extract_title(text: str) -> str | None:
    for pattern in TITLE_PATTERNS:
        match = re.search(pattern, text, re.IGNORECASE)
        if not match:
            continue
        value = match.group(1).strip()
        for cut in TITLE_CUTS:
            value = re.sub(cut, "", value, flags=re.IGNORECASE)
        value = value.strip()
        if not value:
            continue
        # A clause tail is not a title: keep scanning and let the later patterns and the
        # last-resort branch decide, so the stated genres/years are not masked away below.
        if pattern == TITLE_VERB_PATTERN and opens_clause(value):
            continue
        return value
    # Last resort for a bare Latin title in a lookup request ("找一下 The Big Lebowski 这部片",
    # "查一下 The Bridge on the River Kwai 这部片"). The run is searched **after** the marker: a
    # clause such as "I really liked Trainspotting" must not become a seed title, and a
    # sentence-shaped run ("Show me history films ...") is not a title at all.
    marker = re.search(LAST_RESORT_TITLE_MARKER, text, re.IGNORECASE)
    if not marker:
        return None
    run = re.search(EN_TITLE_RUN, text[marker.end():])
    if not run:
        return None
    value = run.group(1).strip()
    aliases = {normalized(name) for names in GENRE_ALIASES.values() for name in names}
    first_token = normalized(value.split()[0]) if value.split() else ""
    # ``EN_TITLE_RUN`` constrains only its first character to ``[A-Z]`` and this search carries no
    # IGNORECASE, but it can still start inside a word, so the boundary is checked on the original
    # text rather than on a folded copy.
    start = marker.end() + run.start()
    # ``str.isalnum()`` is true for Han characters, but a Han character before a Latin run is a
    # word boundary in the sense that matters here ("查一下The Matrix"): only a Latin word interior
    # must reject the match.
    starts_word = (start == 0 or not text[start - 1].isalnum()
                   or not text[start - 1].isascii())
    if value and starts_word and first_token not in TITLE_STOPWORDS \
            and first_token not in aliases and normalized(value) not in aliases:
        return value
    return None


def extract_director(text: str) -> str | None:
    """Name as typed. The catalog stores Latin director values (``Scorsese``, ``M.Mann``)."""
    for pattern in DIRECTOR_PATTERNS:
        for match in re.finditer(pattern, text):
            value = match.group(1).strip(" .,")
            # A demonym in front of a genre noun ("Recommend some Italian documentaries from the
            # 1960s") is not a director name; skip it and keep looking.
            if value and normalized(value) not in DEMONYM_STOPLIST:
                return value
    return None


def route_deterministically(case: dict) -> dict:
    """Controlled action for one case: refuse / clarify / coverage / tool plan (no model)."""
    text = case["text"]
    lowered = text.lower()
    category = detect_refusal(text)
    if category:
        return {"action": "refuse", "reason": category, "tool_calls": []}
    director = extract_director(text)
    title = extract_title(text)
    if title and director and normalized(title) == normalized(director):
        title = None
    # A genre/year word inside the title is not a stated constraint: "The Addams Family - horror
    # films from 1991 to 1992" names Horror, not Family. Mask the **returned** title string, not
    # the raw pattern span - TITLE_CUTS already trimmed the capture to the title itself, so the
    # rest of the sentence (here "horror ... 1991 to 1992") survives and still counts.
    masked = text.replace(title, " ", 1) if title else text
    genres = extract_genres(masked)
    years = extract_years(masked)
    rating = extract_rating(masked)
    constraints: dict = {}
    if genres:
        constraints["genres"] = genres
    if "year_from" in years:
        constraints["year_from"] = years["year_from"]
    if "year_to" in years:
        constraints["year_to"] = years["year_to"]
    if rating is not None:
        constraints["min_rating"] = rating
    similar = any(marker in lowered for marker in SIMILAR_MARKERS)
    recommend = any(marker in lowered for marker in RECOMMEND_MARKERS)
    single_year = (years.get("year_from") if years.get("year_from") is not None
                   and years.get("year_from") == years.get("year_to") else None)

    if similar and title:
        # find_similar_movies has no genre/year/rating/director parameter: the stated conditions
        # narrow the candidate pool server-side as result constraints (R7-2, plan §6.2). The
        # director the query names is one of those conditions ("... Michael Mann 的其他片子");
        # without it the title's own ambiguity (Heat 1995 vs 1972) cannot be resolved.
        seed_constraints = dict(constraints)
        if director:
            seed_constraints["director"] = director
        return {"action": "tools",
                "tool_calls": [{"name": "search_movies_by_title",
                                "arguments": {"query": title, "limit": SEED_SEARCH_LIMIT}}],
                "follow_up": SEED_FOLLOW_UP,
                "seed_title": title,
                "result_constraints": seed_constraints}
    if recommend:
        preferences: dict = {}
        if genres:
            preferences["preferred_genres"] = genres
        if "year_from" in years or "year_to" in years:
            preferences["year_range"] = {"from": years.get("year_from", YEAR_MIN),
                                         "to": years.get("year_to", YEAR_MAX)}
        if rating is not None:
            preferences["min_rating"] = rating
        if director:
            preferences["director"] = director
        if not preferences:
            return {"action": "clarify", "question": "genre / decade / rating?", "tool_calls": []}
        state, meta = coverage_state({"year_range": preferences.get("year_range"),
                                      "min_rating": preferences.get("min_rating")})
        if state == "OUT_OF_RANGE":
            return {"action": "explain_coverage", "error_state": state, "meta": meta,
                    "candidate_arguments": preferences, "tool_calls": []}
        return {"action": "tools",
                "tool_calls": [{"name": "recommend_movies", "arguments": preferences}],
                "result_constraints": dict(constraints)}
    if title and (has_title_prefix(text) or single_year is not None):
        # A title plus one stated year is a lookup with the schema's own disambiguation argument
        # (search_movies_by_title.year), not a genre/year filter.
        arguments: dict = {"query": title}
        if single_year is not None:
            arguments["year"] = single_year
        return {"action": "tools",
                "tool_calls": [{"name": "search_movies_by_title", "arguments": arguments}],
                "result_constraints": {key: value for key, value in constraints.items()
                                       if key not in ("year_from", "year_to")}}
    filters: dict = {}
    if genres:
        filters["genres"] = genres
    if "year_from" in years:
        filters["year_from"] = years["year_from"]
    if "year_to" in years:
        filters["year_to"] = years["year_to"]
    if rating is not None:
        filters["min_rating"] = rating
    if filters:
        state, meta = coverage_state(filters)
        if state == "OUT_OF_RANGE":
            return {"action": "explain_coverage", "error_state": state, "meta": meta,
                    "candidate_arguments": filters, "tool_calls": []}
        return {"action": "tools",
                "tool_calls": [{"name": "filter_movies", "arguments": filters}]}
    if title:
        return {"action": "tools",
                "tool_calls": [{"name": "search_movies_by_title",
                                "arguments": {"query": title}}]}
    return {"action": "clarify", "question": "which movie intent?", "tool_calls": []}


# --------------------------------------------------------------------------------------
# Scoring
# --------------------------------------------------------------------------------------

def normalized(value: str) -> str:
    return re.sub(r"[\s'’`\"《》]+", " ", value).strip().casefold()


def slots_match(case: dict, called: list[dict]) -> tuple[bool, list[str]]:
    """Compare the expected slots with what was actually passed (alternatives allowed)."""
    expected_sets = [case["required_slots"]] + case.get("accept_alternative_slots", [])
    if not case["required_slots"]:
        return True, []
    by_tool = {call["name"]: call["arguments"] for call in called}
    problems: list[str] = []
    for slots in expected_sets:
        candidate_problems: list[str] = []
        for key, value in slots.items():
            if key == "seed_title":
                search = by_tool.get("search_movies_by_title", {})
                if normalized(str(search.get("query", ""))) != normalized(value):
                    candidate_problems.append(f"seed_title: {search.get('query')!r} != {value!r}")
                continue
            tool = ("recommend_movies" if key in
                    ("preferred_genres", "year_range", "director")
                    or ("recommend_movies" in case["expected_tools"]
                        and "filter_movies" not in case["expected_tools"])
                    else "filter_movies")
            if key in ("query", "year"):
                tool = "search_movies_by_title"
            arguments = by_tool.get(tool, {})
            actual = arguments.get(key)
            if key in ("genres", "preferred_genres"):
                # Canonical keys, so "Sci-Fi" and "Science Fiction" are the same condition.
                if not isinstance(actual, list) or set(canonical_genres(actual)) != \
                        set(canonical_genres(value)):
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
            elif key == "year_range":
                if not isinstance(actual, dict) or actual.get("from") != value["from"] or \
                        actual.get("to") != value["to"]:
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
            elif key == "query":
                if normalized(str(actual)) != normalized(value):
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
            elif key == "director":
                if not isinstance(actual, str) or not directors_equal(actual, value):
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
            elif key == "min_rating":
                if actual is None or abs(float(actual) - float(value)) > 1e-9:
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
            else:
                if actual != value:
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
        if not candidate_problems:
            return True, []
        problems = candidate_problems
    return False, problems


def text_constraints(text: str) -> dict:
    """The result conditions the user actually stated, derived from the text alone.

    A variant can only be required to honour what the query says. Annotation constraints that
    the text does not state (a golden "neighbourhood" window, a person name behind a similar
    film) are reported separately instead of being scored as misses.
    """
    constraints: dict = {}
    genres = extract_genres(text)
    if genres:
        constraints["genres"] = genres
    years = extract_years(text)
    if "year_from" in years:
        constraints["year_from"] = years["year_from"]
    if "year_to" in years:
        constraints["year_to"] = years["year_to"]
    rating = extract_rating(text)
    if rating is not None:
        constraints["min_rating"] = rating
    director = extract_director(text)
    if director:
        constraints["director"] = director
    return constraints


def score_case(case: dict, called: list[dict], schema_problems: list[str],
               execution: list[dict], behavior: str, error_state: str | None,
               executor: ToolExecutor, answer: str = "",
               disambiguation_candidates: list | None = None) -> dict:
    expected_tools = case["expected_tools"]
    called_names = [call["name"] for call in called]
    if expected_tools:
        tools_ok = bool(called_names) and set(called_names) <= set(expected_tools)
        tools_complete = set(expected_tools) <= set(called_names)
    else:
        tools_ok = not called_names
        tools_complete = not called_names
    slots_ok, slot_problems = slots_match(case, called)
    if case["expected_behavior"] == "refuse_no_tool":
        behavior_ok = not called_names
    elif case["expected_behavior"] == "ask_one_clarifying_question":
        behavior_ok = behavior in ("clarify", "refuse") or not called_names
    elif case["expected_behavior"] == "explain_coverage_limit":
        behavior_ok = error_state in ("EMPTY_RESULT", "OUT_OF_RANGE")
    else:
        # the expected steps are checked through tools_ok/tools_complete; a disambiguation
        # question after a multi-hit title search is the contract behaviour (R2-3), not a miss
        behavior_ok = tools_ok
    deferred = any(item.get("status") == "deferred" for item in execution)
    expected_ids = set(case.get("acceptable_movie_ids", []))
    rows = [row for item in execution if item.get("displayed", True)
            for row in item.get("results", [])]
    returned_ids = {row["id"] for row in rows}
    accepted_hit = bool(expected_ids & returned_ids) if expected_ids else None
    constraints = case.get("result_constraints") or {}
    stated = text_constraints(case["text"])
    scored = {key: value for key, value in constraints.items() if key in stated}
    constraint_violations = [f"{row['id']} {row['title']}: {problem}"
                             for row in rows
                             for problem in constraint_problems(row, scored)]
    grounding_ok, grounding_problems = executor.ground(rows)
    clarify = clarification_shape(execution, answer, disambiguation_candidates)
    return {
        "tools_expected": expected_tools,
        "tools_called": called_names,
        "tools_ok": tools_ok,
        "tools_complete": tools_complete,
        "slots_ok": slots_ok,
        "slot_problems": slot_problems,
        "schema_ok": not schema_problems,
        "schema_problems": schema_problems,
        "behavior_ok": behavior_ok,
        "error_state_expected": case["expected_error_code"],
        "error_state_observed": error_state,
        "acceptable_hit": accepted_hit,
        "rows_returned": len(rows),
        "seed_excluded": all(item.get("seed_excluded", True) for item in execution),
        "result_constraints": constraints,
        "result_constraints_scored": scored,
        "result_constraints_unscored": sorted(set(constraints) - set(stated)),
        "result_constraint_violations": constraint_violations,
        "result_constraint_ok": None if not scored else not constraint_violations,
        "grounding_ok": grounding_ok,
        "grounding_problems": grounding_problems,
        "execution_deferred": deferred,
        # ``allows_clarification`` (plan §5.3): strict task success stays the main figure; a
        # "list the candidates + ask exactly one question" answer is reported next to it.
        "clarification_style": bool(behavior == "clarify" and clarify["complete"]
                                    and clarify["questions"] == 1),
        "clarification_candidates": clarify["candidates"],
        "passed": bool(tools_ok and tools_complete and slots_ok and not schema_problems
                       and behavior_ok and not deferred),
    }


def clarification_shape(execution: list[dict], answer: str,
                        extra_candidates: list | None = None) -> dict:
    """Does the answer list the candidates (title + year + director) and ask exactly one question?

    R2-3 requires exactly that when a title matches more than one row; the
    ``allows_clarification`` cases are scored on this shape (plan §5.3).
    """
    candidates: list[dict] = []
    seen: set = set()
    for candidate in ([{"id": row.get("id"), "title": row.get("title"), "year": row.get("year"),
                        "director": row.get("director")}
                       for item in execution for row in item.get("results", [])]
                      + list(extra_candidates or [])):
        if candidate.get("id") in seen:
            continue
        seen.add(candidate.get("id"))
        candidates.append(candidate)
    complete = len(candidates) >= 2 and all(
        all(candidate.get(field) not in (None, "") for field in ("title", "year", "director"))
        for candidate in candidates)
    return {"candidates": candidates, "complete": complete,
            "questions": answer_text_question_count(answer)}


def is_task_success(case: dict, record: dict) -> bool:
    """Plan §6 task-level success: the right call, executed and grounded, nothing invented.

    Refusal / clarification / coverage cases have no rows: their task success is the required
    behaviour itself (a refusal with no tool call, one question, the expected error state).
    """
    expected = case["expected_behavior"]
    if expected == "refuse_no_tool":
        return bool(record["behavior_ok"]) and not record["tools_called"]
    if expected == "ask_one_clarifying_question":
        return bool(record["behavior_ok"]) and not record["schema_problems"]
    if expected == "explain_coverage_limit":
        return record["error_state_observed"] == case["expected_error_code"]
    if not record["passed"] or record["execution_deferred"]:
        return False
    if record["rows_returned"] < 1 or record["grounding_ok"] is False:
        return False
    if record["result_constraint_ok"] is False:
        return False
    if record["acceptable_hit"] is False:
        return False
    return True


def failure_modes(case: dict, record: dict) -> list[str]:
    """Plan §6 failure taxonomy, limited to what the harness can observe."""
    modes: list[str] = []
    expected = set(case["expected_tools"])
    called = set(record["tools_called"])
    if not called and expected:
        modes.append("no_tool_call")
    if called - expected:
        modes.append("wrong_tool" if not called & expected else "extra_tool")
    if expected - called and called:
        modes.append("incomplete_tool_sequence")
    if record["schema_problems"]:
        modes.append("enum_language" if any("not in enum" in problem
                                            for problem in record["schema_problems"])
                     else "bad_args")
    if not record["slots_ok"]:
        modes.append("slot_mismatch")
    if case["expected_refusal"] and called:
        modes.append("under_refusal")
    if not case["expected_tools"] and called and not case["expected_refusal"]:
        modes.append("unrequested_tool")
    if case["expected_error_code"] != "NONE" \
            and record["error_state_observed"] != case["expected_error_code"]:
        modes.append("coverage_confusion")
    if case["expected_behavior"] == "ask_one_clarifying_question" and not record["behavior_ok"]:
        modes.append("no_clarification")
    if record["prose_id_hallucinations"]:
        modes.append("hallucinated_id")
    if record["result_constraint_ok"] is False:
        modes.append("result_constraint_violation")
    if record["grounding_ok"] is False:
        modes.append("grounding_violation")
    if record["acceptable_hit"] is False:
        modes.append("acceptable_set_miss")
    if case["expected_behavior"] == "call_tools_and_answer" and record["rows_returned"] == 0:
        modes.append("empty_result")
    if record["execution_deferred"]:
        modes.append("not_executed")
    return modes


# --------------------------------------------------------------------------------------
# Self-test (scripted responses; NOT a model measurement)
# --------------------------------------------------------------------------------------

# Keys are case texts from the fixtures so the scripted reply cannot drift from the case.
SELF_TEST_SCRIPT = {
    "《The Matrix》的剧情是什么？": [],  # must be refused without a tool call
    "2005 年评分 8 分以上的喜剧": [
        {"function": {"name": "filter_movies",
                      "arguments": {"genres": ["Comedy"], "year_from": 2005,
                                    "year_to": 2005, "min_rating": 8}}},
    ],
    # Chinese genre value on purpose: G0 must fail schema validation, G2 must map it to the enum
    "1960 年代的犯罪片": [
        {"function": {"name": "filter_movies",
                      "arguments": {"genres": ["犯罪"], "year_from": 1960, "year_to": 1969}}},
    ],
}


class ScriptedClient:
    """Returns canned assistant messages so the scoring path can run without a model."""

    def chat_with_tools(self, model: str, messages: list[dict], tools: list[dict]) -> tuple[dict, float]:
        text = messages[-1]["content"]
        if text not in SELF_TEST_SCRIPT:
            raise EvalError(f"self-test has no scripted reply for {text!r}")
        return {"message": {"role": "assistant", "content": "", "tool_calls": SELF_TEST_SCRIPT[text]},
                "prompt_eval_count": 0, "eval_count": 0}, 0.0


# Canned resolver replies for the G1-T plumbing test. They are scripted *model output*, not an
# alias table: "北非谍影" appears in no fixture, so a verification here cannot turn into an answer
# key for a measured case. A real G1-T run never loads this class.
SELF_TEST_RESOLVER_REPLIES = {
    "北非谍影": (json.dumps({"candidates": ["Casablanca"]}), False),
    "无法校验的译本": (json.dumps({"candidates": ["Not In This Catalog 12345"]}), False),
    "格式错误的译本": ("this is not json", False),
    "多余的字段译本": (json.dumps({"candidates": ["Casablanca"], "note": "extra"}), False),
    "超时的译本": (None, True),
}


class ScriptedResolverClient:
    """Canned resolver completions; runs the resolver without a model or a network call."""

    def __init__(self) -> None:
        self.calls: list[str] = []

    def generate_json(self, model: str, system_prompt: str, user_text: str,
                      timeout_s: float) -> tuple[str | None, float, bool]:
        title = user_text.rsplit("Chinese title:", 1)[-1].strip()
        self.calls.append(title)
        if title not in SELF_TEST_RESOLVER_REPLIES:
            raise EvalError(f"self-test has no scripted resolver reply for {title!r}")
        body, timed_out = SELF_TEST_RESOLVER_REPLIES[title]
        return body, 1.0, timed_out


# The user-utterance strings the self-test feeds to the router and the resolver. ``check_self_test_
# independence`` fails if one of them equals a held-out query text: the held-out fixture may not
# become part of rule development.
SELF_TEST_SAMPLES = {
    "year-range": "2002 到 2003 年评分 7 分以上的战争片",
    "genre-alias": "想看一部战争片",
    "title-year": "帮我找 1946 年那部 Notorious",
    "multi-genre": "动作科幻片",
    "war-boundary": "a movie toward Edward",
    "musical": "musical",
    "music": "music",
    "rating-at-least": "movies rated at least 6",
    "title-verb-zh": "查一下 Casablanca 这部片",
    "genre-not-director": "Recommend some Japanese movies",
    "director-clause-zh": "推荐几部 Kurosawa 在 1950 年代拍的剧情片",
    "director-clause-en": "Recommend some Kubrick thrillers from 1958 to 1959",
    "out-of-catalog": "《Casablanca》的票房是多少？",
    "not-refusal-score": "movies with a user score of 8 or higher",
    "not-refusal-music": "recommend some music documentaries from the 1970s",
    "not-refusal-released": "movies released in 1999",
    "not-refusal-studio": "Studio Ghibli films from the 1980s",
    "title-region-genres": "movies like The Family Stone - comedies from 2005 to 2007",
    "seed-title-liked": "I liked Rear Window, anything else like it?",
    "seed-title-enjoyed": "We enjoyed Casablanca, can you suggest something similar?",
    "seed-title-watched": "I watched Vertigo, anything else?",
    "sentence-not-title-show": "Show me drama films from 1985 to 1986",
    "sentence-not-title-find": "Find Comedy films from 1985 to 1986",
    "title-clause-decade": "movies I enjoyed in the 1990s",
    "title-clause-genre": "I enjoyed comedy films from the 1990s",
    "title-verb-keeps-title": "I really liked Trainspotting - any similar films?",
    "title-glued-zh": "查一下The Matrix",
    "plural-genre": "suggest a few documentaries from the 1970s",
    "demonym-documentaries": "Recommend some Italian documentaries from the 1960s",
    "demonym-comedies": "Recommend some Italian comedies from the 1960s",
    "similar-derived": "Can you find something similar to The Apartment?",
    "similar-explicit": "Any movies like The Apartment - Comedy from 1960 to 1969?",
    "similar-one-genre": "I liked Jaws, anything similar?",
    "similar-many-genres": "I liked Blue Velvet, anything similar?",
    "similar-stated-genres": "Any movies like The Apartment - comedies?",
    "resolver-cjk": "帮我找《北非谍影》这部电影",
    "resolver-latin": "帮我找《The Vikings》这部电影",
    "resolver-zh-title": "北非谍影",
    "resolver-han": "卡萨布兰卡",
    "resolver-invalid": "格式错误的译本",
    "resolver-extra-field": "多余的字段译本",
    "resolver-rejected": "无法校验的译本",
    "resolver-timeout": "超时的译本",
    "replay-miss": "不在回放记录里的译本",
}
SELF_TEST_UTTERANCES = tuple(SELF_TEST_SCRIPT) + tuple(SELF_TEST_SAMPLES.values())


def check_self_test_independence() -> list[str]:
    """No self-test assertion may use a held-out query text (review P1-5, plan §8.3)."""
    heldout = load_json(HELDOUT_QUERIES_PATH)
    texts = {normalized(query["query"]) for query in heldout["queries"]}
    return [f"self-test utterance {utterance!r} is a held-out query text"
            for utterance in SELF_TEST_UTTERANCES if normalized(utterance) in texts]


# --------------------------------------------------------------------------------------
# Variant runners
# --------------------------------------------------------------------------------------

def run_g0(case: dict, client: OllamaClient, model: str, tools: list[dict]) -> dict:
    messages = [{"role": "system", "content": PROMPT_TEXT}, {"role": "user", "content": case["text"]}]
    response, latency_ms = client.chat_with_tools(model, messages, tools)
    message = response.get("message", {})
    return {
        "tool_calls": parse_tool_calls(message),
        "content": message.get("content", ""),
        "latency_ms": round(latency_ms, 1),
        "prompt_eval_count": response.get("prompt_eval_count"),
        "eval_count": response.get("eval_count"),
    }


def evaluate_calls(called: list[dict], schema_doc: dict, executor: ToolExecutor,
                   constraints: dict | None = None,
                   reject_invalid: bool = False) -> tuple[list[str], list[dict], str | None]:
    schema_problems: list[str] = []
    execution: list[dict] = []
    error_state = None
    for call in called:
        problems = [f"{call['name']}: {problem}" for problem in
                    validate_call(schema_doc, call["name"], call["arguments"])]
        schema_problems.extend(problems)
        if problems and reject_invalid:
            # G2: the server refuses the arguments instead of executing them (plan §5).
            execution.append({"status": "INVALID_ARGUMENT", "results": [],
                              "problems": problems})
            error_state = "INVALID_ARGUMENT"
            continue
        state, _meta = coverage_state(call["arguments"])
        if state:
            error_state = state
        execution.append(executor.execute(call["name"], call["arguments"], constraints))
    if called and error_state is None:
        for item in execution:
            if item.get("status") in ("EMPTY_RESULT", "OUT_OF_RANGE", "NOT_FOUND", "error"):
                error_state = item["status"]
                break
    return schema_problems, execution, error_state


# --------------------------------------------------------------------------------------
# G1-T: query-time title resolution (plan §4.3-§4.6)
# --------------------------------------------------------------------------------------

def answer_text_question_count(answer: str) -> int:
    return (answer or "").count("?") + (answer or "").count("？")


def parse_resolver_candidates(text: str | None) -> list[str] | None:
    """Strict ``{"candidates": [1-3 English titles]}``; anything else is a failed step.

    Plan §4.3: extra fields, zero candidates, more than three, non-strings or (near-)empty
    titles are rejected - the resolver must never guess its way past a malformed answer.
    """
    if not text:
        return None
    try:
        payload = json.loads(text)
    except json.JSONDecodeError:
        return None
    if not isinstance(payload, dict) or set(payload) != {"candidates"}:
        return None
    candidates = payload["candidates"]
    if not isinstance(candidates, list) or not 1 <= len(candidates) <= 3:
        return None
    cleaned: list[str] = []
    for candidate in candidates:
        if not isinstance(candidate, str):
            return None
        value = candidate.strip()
        if len(value) < 2 or has_han(value) or value in cleaned:
            return None
        cleaned.append(value)
    return cleaned


ANNOTATION_KEYS = ("required_slots", "accept_alternative_slots", "result_constraints",
                   "acceptable_movie_ids")


class AnnotatedCaseGuard:
    """Case view for the execution half that refuses to hand out a golden annotation.

    Plan §6.1/`B15c-3`: the executor may only use slots and constraints the router derived from the
    query text. Reading ``required_slots`` / ``result_constraints`` / ``acceptable_movie_ids``
    while executing means the harness is scoring an oracle, so it must be impossible. The guard
    wraps the *case mapping*, because wrapping the values would still let the lookup succeed.
    """

    def __init__(self, case: dict) -> None:
        self._case = case

    @staticmethod
    def _reject(key: str):
        raise EvalError(f"execution read the golden annotation {key!r}")

    def __getitem__(self, key):
        if key in ANNOTATION_KEYS:
            self._reject(key)
        return self._case[key]

    def get(self, key, default=None):
        if key in ANNOTATION_KEYS:
            self._reject(key)
        return self._case.get(key, default)

    def __contains__(self, key):
        if key in ANNOTATION_KEYS:
            self._reject(key)
        return key in self._case

    def __iter__(self):
        return iter(self._case)

    def keys(self):
        return self._case.keys()


def guard_case_annotations(case: dict) -> AnnotatedCaseGuard:
    return AnnotatedCaseGuard(case)


class TitleResolver:
    """Alias/cache -> local model candidates -> catalog verification -> clarification.

    Only a CJK title without a catalog exact hit may reach the model (plan §4.3 hard constraint
    1); every other query keeps ``model_calls = 0``. Unverified candidates never reach
    ``search_movies_by_title``: the resolver returns either a catalog row or nothing.
    """

    def __init__(self, client, model: str, executor: "ToolExecutor",
                 cache_path: Path | None = None, use_cache: bool = True,
                 replay_path: Path | None = None, timeout_s: float = RESOLVER_TIMEOUT_S,
                 alias_doc: dict | None = None) -> None:
        self.client = client
        self.model = model
        self.executor = executor
        self.cache_path = cache_path
        self.use_cache = use_cache
        self.timeout_s = timeout_s
        self.alias_doc = alias_doc if alias_doc is not None else TITLE_ALIAS_DOC
        self.prompt_sha256 = hashlib.sha256(RESOLVER_PROMPT.encode("utf-8")).hexdigest()
        self.model_digest: str | None = None
        self.replay = None
        self.replay_model: str | None = None
        self.cache: dict[str, dict] = {}
        # A cache entry is evidence for the prompt/model that produced it and for nothing else
        # (plan §4.4/§4.6). Mismatched entries are dropped instead of being replayed as this run's
        # decision; a replay file with mismatched metadata is fatal, because replay exists to
        # reproduce exactly one recorded run.
        self.cache_status = "disabled" if not use_cache else "absent"
        self.cache_problems: list[str] = []
        if replay_path is not None:
            self.replay = load_json(Path(replay_path))
            self.replay_model = self.replay.get("model")
            self.cache_problems = self.metadata_problems(self.replay)
            if self.cache_problems:
                raise EvalError(
                    f"--replay-resolver {repo_relative(replay_path)} does not match this run: "
                    + "; ".join(self.cache_problems)
                )
            self.cache = dict(self.replay.get("entries") or {})
            self.model_digest = self.replay.get("model_digest")
            self.cache_status = "replay"
        elif use_cache and cache_path is not None and Path(cache_path).is_file():
            recorded = load_json(Path(cache_path))
            self.cache_problems = self.metadata_problems(recorded)
            if self.cache_problems:
                self.cache_status = "stale-ignored"
                print(f"warning: ignoring {repo_relative(cache_path)}: "
                      + "; ".join(self.cache_problems), file=sys.stderr)
            else:
                self.cache = dict(recorded.get("entries") or {})
                # A warm run never contacts the model, so it inherits the digest the cold run pinned.
                self.model_digest = recorded.get("model_digest")
                self.cache_status = "loaded"

    def metadata_problems(self, recorded: dict) -> list[str]:
        """Why a cache/replay file cannot be trusted as this run's evidence (empty = trusted)."""
        problems: list[str] = []
        if recorded.get("cache_version") != RESOLVER_CACHE_VERSION:
            problems.append(f"cache_version {recorded.get('cache_version')!r} != "
                            f"{RESOLVER_CACHE_VERSION!r}")
        if recorded.get("prompt_sha256") != self.prompt_sha256:
            problems.append(f"prompt_sha256 {recorded.get('prompt_sha256')!r} != "
                            f"{self.prompt_sha256!r}")
        if recorded.get("prompt_id") != RESOLVER_PROMPT_ID:
            problems.append(f"prompt_id {recorded.get('prompt_id')!r} != {RESOLVER_PROMPT_ID!r}")
        if recorded.get("model") != self.model:
            problems.append(f"model {recorded.get('model')!r} != {self.model!r}")
        if "model_digest" not in recorded:
            problems.append("model_digest is not recorded")
        return problems

    def alias_entry(self, zh_title: str) -> dict | None:
        for entry in self.alias_doc.get("entries") or []:
            if normalized(entry.get("zh_title", "")) == normalized(zh_title):
                return entry
        return None

    @staticmethod
    def user_text(zh_title: str) -> str:
        return f"Chinese title: {zh_title}"

    def request_sha256(self, zh_title: str) -> str:
        request = f"{RESOLVER_PROMPT}\n\n{self.user_text(zh_title)}"
        return hashlib.sha256(request.encode("utf-8")).hexdigest()

    def _choose(self, rows: list[dict], step: str, record: dict,
                constraints: dict | None = None) -> dict:
        """One catalog row -> resolved; several distinct ids -> clarification (plan §4.3).

        Before declaring the candidates ambiguous, the clues the query itself gave (year window,
        director) narrow them (plan §7). Only a still-ambiguous set is handed back as a question:
        a resolved title and a stated condition together pick one row, never a guess.
        """
        exact = [row for row in rows if row.get("match") == "exact"]
        pool = exact or rows
        narrowed = pool
        if len({row["id"] for row in pool}) > 1:
            narrowed = narrow_seed_candidates(pool, constraints)
        if narrowed is not pool:
            record["narrowed_by"] = {key: value for key, value in (constraints or {}).items()
                                     if key in ("year_from", "year_to", "director")}
            record["narrowed_to"] = [row["id"] for row in narrowed]
        ids = {row["id"] for row in narrowed}
        if len(ids) == 1:
            chosen = narrowed[0]
            record.update({"step": step, "chosen_movie_id": chosen["id"],
                           "chosen_title": chosen["title"],
                           "catalog_verified": [self._verified(row) for row in pool],
                           "outcome": "resolved"})
            return record
        record.update({"step": "clarify", "catalog_verified": [self._verified(row) for row in pool],
                       "outcome": "ambiguous", "degraded_to": "clarify"})
        return record

    @staticmethod
    def _verified(row: dict) -> dict:
        return {"id": row["id"], "title": row["title"], "year": row["year"],
                "director": row["director"], "match": row.get("match")}

    def resolve(self, zh_title: str, constraints: dict | None = None) -> dict:
        record: dict = {
            "triggered": True, "zh_title": zh_title, "step": None, "outcome": "unresolved",
            "llm_candidates": [], "catalog_verified": [], "chosen_movie_id": None,
            "chosen_title": None, "model": self.model, "temperature": TEMPERATURE,
            "prompt_id": RESOLVER_PROMPT_ID, "request_sha256": self.request_sha256(zh_title),
            "cache": "disabled" if not self.use_cache else "miss",
            "latency_ms": 0.0, "timed_out": False, "degraded_to": None, "model_calls": 0,
        }
        # Step 1a is the verified alias table (frozen empty by C10). It is data, not a cache, so
        # --no-cache does not skip it; step 1b is the run cache, which --no-cache does skip.
        entry = self.alias_entry(zh_title)
        if entry is not None:
            card = self.executor.execute("get_movie_card",
                                         {"movie_id": entry.get("movie_id", "")})
            card_rows = card.get("results") or []
            if card_rows:
                record.update({"step": "alias", "chosen_movie_id": card_rows[0]["id"],
                               "chosen_title": card_rows[0]["title"],
                               "catalog_verified": [self._verified(card_rows[0])],
                               "outcome": "resolved", "cache": "alias"})
                return record
        if self.replay is not None and zh_title not in self.cache:
            # Replay must reproduce the recorded decisions or fail: falling through would call the
            # model and silently turn a replay into a second cold run (plan §4.6).
            raise EvalError(
                f"--replay-resolver has no recorded decision for {zh_title!r}; refusing to call "
                f"the model during a replay"
            )
        if self.use_cache:
            cached = self.cache.get(zh_title)
            if cached is not None:
                record.update(dict(cached))
                record.update({"cache": "replay" if self.replay is not None else "hit",
                               "model_calls": 0})
                return record

        started = time.perf_counter()
        direct = self.executor.title_candidates(zh_title)
        if direct:
            record = self._choose(direct, "catalog-exact", record, constraints)
            record["latency_ms"] = round((time.perf_counter() - started) * 1000.0, 1)
            self._remember(zh_title, record)
            return record
        if not has_han(zh_title) or self.client is None:
            # No CJK title (or no model configured): searching the text as typed is the baseline
            # behaviour, not a resolution failure. ``model_calls`` stays 0.
            record.update({"step": "passthrough", "outcome": "not_applicable"})
            return record

        text, latency_ms, timed_out = self.client.generate_json(
            self.model, RESOLVER_PROMPT, self.user_text(zh_title), self.timeout_s)
        record["latency_ms"] = round(latency_ms, 1)
        record["model_calls"] = 1
        if timed_out:
            record.update({"step": "llm-timeout", "timed_out": True, "outcome": "timeout",
                           "degraded_to": "clarify"})
            self._remember(zh_title, record)
            return record
        candidates = parse_resolver_candidates(text)
        if candidates is None:
            record.update({"step": "llm-invalid", "outcome": "invalid_json",
                           "degraded_to": "clarify"})
            self._remember(zh_title, record)
            return record
        record["llm_candidates"] = candidates
        verified: list[dict] = []
        for candidate in candidates:
            for row in self.executor.title_candidates(candidate):
                verified.append(row)
        record["catalog_verified"] = [self._verified(row) for row in verified]
        if not verified:
            record.update({"step": "llm", "outcome": "rejected", "degraded_to": "clarify"})
        else:
            record = self._choose(verified, "llm", record, constraints)
        self._remember(zh_title, record)
        return record

    def _remember(self, zh_title: str, record: dict) -> None:
        if not self.use_cache:
            return
        self.cache[zh_title] = {key: value for key, value in record.items()
                                if key not in ("latency_ms", "cache")}

    def save(self) -> None:
        if not self.use_cache or self.replay is not None or self.cache_path is None:
            return
        path = Path(self.cache_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        payload = {"cache_version": RESOLVER_CACHE_VERSION,
                   "prompt_id": RESOLVER_PROMPT_ID, "prompt_sha256": self.prompt_sha256,
                   "model": self.model, "model_digest": self.model_digest,
                   "entries": self.cache}
        path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                        encoding="utf-8")


def resolve_route_title(route: dict, resolver: TitleResolver) -> dict:
    """Replace the CJK title the router extracted with a catalog-verified English title."""
    calls = route.get("tool_calls") or []
    if not calls or calls[0].get("name") != "search_movies_by_title":
        return {"route": route, "record": None}
    zh_title = str(calls[0]["arguments"].get("query", ""))
    if not has_han(zh_title):
        return {"route": route, "record": {"triggered": False, "zh_title": zh_title,
                                           "outcome": "not_applicable", "model_calls": 0}}
    # The year/director clue the query stated (the router's own constraints, never the golden
    # annotation) disambiguates several catalog rows before the resolution is called ambiguous.
    record = resolver.resolve(zh_title, route.get("result_constraints") or None)
    if record.get("chosen_movie_id") is None:
        question = {
            "timeout": "which English title do you mean?",
            "invalid_json": "which English title do you mean?",
            "rejected": "which English title do you mean?",
            "ambiguous": "which of these did you mean?",
        }.get(record.get("outcome"), "which English title do you mean?")
        candidates = record.get("catalog_verified") or []
        listed = "; ".join(f"{row['title']} ({row['year']}, {row['director']})"
                           for row in candidates[:3])
        answer = f"{question} {listed}".strip() if listed else question
        return {"route": {"action": "clarify", "tool_calls": []}, "record": record,
                "answer": answer}
    arguments = dict(calls[0]["arguments"])
    arguments["query"] = record["chosen_title"]
    resolved = dict(route)
    resolved["tool_calls"] = [{"name": "search_movies_by_title", "arguments": arguments}] \
        + list(calls[1:])
    if route.get("seed_title"):
        resolved["seed_title"] = record["chosen_title"]
    return {"route": resolved, "record": record}


def narrow_seed_candidates(candidates: list[dict], constraints: dict | None) -> list[dict]:
    """Disambiguate multi-hit titles with the year/director clues the query itself gave.

    The year clue is a **window** (``year_from``/``year_to``), not a single year: "1976 到 1978 年"
    and "1950 年代" both narrow a candidate list without naming one row's exact year.
    """
    narrowed = candidates
    constraints = constraints or {}
    year_from = constraints.get("year_from")
    year_to = constraints.get("year_to")
    if isinstance(year_from, int) or isinstance(year_to, int):
        low = year_from if isinstance(year_from, int) else YEAR_MIN
        high = year_to if isinstance(year_to, int) else YEAR_MAX
        by_year = [row for row in narrowed if low <= row["year"] <= high]
        if by_year:
            narrowed = by_year
    director = constraints.get("director")
    if director:
        by_director = [row for row in narrowed if directors_equal(row["director"], director)]
        if by_director:
            narrowed = by_director
    return narrowed


def disambiguation_answer(candidates: list[dict], prompt: str = "which one do you mean?") -> str:
    listing = "; ".join(f"{row['title']} ({row['year']}, {row['director']})"
                        for row in candidates[:3])
    return f"{prompt} {listing}".strip() if listing else prompt


def run_g1(case: dict, schema_doc: dict, executor: ToolExecutor,
           resolver: TitleResolver | None = None) -> dict:
    """Deterministic route, the optional G1-T title resolution, then the seed hop."""
    route = route_deterministically(case)
    extra: dict = {"route": {key: value for key, value in route.items() if key != "tool_calls"},
                   "latency_ms": 0.0, "model_calls": 0}
    if resolver is not None:
        resolution = resolve_route_title(route, resolver)
        route = resolution["route"]
        extra["resolver"] = resolution["record"]
        extra["model_calls"] = int((resolution["record"] or {}).get("model_calls", 0))
        if resolution.get("answer"):
            return {"called": [], "execution": [], "schema_problems": [], "behavior": "clarify",
                    "error_state": None,
                    "extra": {**extra, "answer": resolution["answer"],
                              "disambiguation_candidates":
                                  (resolution["record"] or {}).get("catalog_verified", [])[:3]}}
    action = route["action"]
    if action == "refuse":
        return {"called": [], "execution": [], "schema_problems": [], "behavior": "refuse",
                "error_state": None,
                "extra": {**extra, "answer": f"[refusal: {route['reason']}]"}}
    if action == "clarify":
        return {"called": [], "execution": [], "schema_problems": [], "behavior": "clarify",
                "error_state": None,
                "extra": {**extra, "answer": f"[one question: {route['question']}]"}}
    if action == "explain_coverage":
        return {"called": [], "execution": [], "schema_problems": [],
                "behavior": "explain_coverage", "error_state": route["error_state"],
                "extra": {**extra,
                          "answer": f"[coverage {route['error_state']} {route['meta']}]",
                          "candidate_arguments": route["candidate_arguments"]}}

    constraints = route.get("result_constraints") or None
    called = list(route["tool_calls"])
    schema_problems, execution, error_state = evaluate_calls(called, schema_doc, executor,
                                                             constraints)
    behavior = "tools"
    rows = [row for item in execution for row in item.get("results", [])]
    if route.get("follow_up") == SEED_FOLLOW_UP:
        candidates = [row for row in rows
                      if normalized(row["title"]) == normalized(route["seed_title"])] or rows
        candidates = narrow_seed_candidates(candidates, constraints)
        seed = candidates[0] if len(candidates) == 1 else None
        if seed is None:
            # R2-3: more than one plausible title - list candidates and ask, do not guess.
            behavior = "clarify"
            extra["disambiguation_candidates"] = [
                {"id": row["id"], "title": row["title"], "year": row["year"],
                 "director": row["director"]} for row in candidates[:3]]
            extra["answer"] = disambiguation_answer(candidates)
        else:
            for item in execution:
                item["displayed"] = False
            # B1.5e F-5 (R-2, owner ruling ③b2): when the text states no director slot and the seed
            # has exactly two canonical genres, the seed's genres and its whole decade narrow the
            # follow-up; explicit text keys still win key by key, and an empty derived pool is not
            # widened back to "rank only". A seed with one or with three-plus canonical genres is
            # not derived: one genre is the same-director case (Q16/Q35/Q56 pass on ranking), and
            # ANDing Blue Velvet's Comedy+Drama+Fantasy+Psyc collapses the pool (RK-5).
            # Q12 vs Q34 is the approved exception: both route to `seed_title` + empty constraints
            # with a two-genre seed and cannot be told apart without reading the annotation, so the
            # R-2 semantics win and Q12 (same director, no narrowing) is the single accepted
            # in-sample regression. Only the route (text) and the seed row (catalog) are read here -
            # never an annotation.
            stated = constraints or {}
            seed_genres = canonical_genres(seed["genres"])
            derived: dict = {}
            derive = not stated.get("director") and len(seed_genres) == 2
            if derive and not stated.get("genres"):
                derived["genres"] = seed_genres
            if derive and "year_from" not in stated and "year_to" not in stated:
                decade = seed["year"] - seed["year"] % 10
                derived["year_from"], derived["year_to"] = decade, decade + 9
            effective = {**derived, **stated}
            follow = {"name": SEED_FOLLOW_UP, "arguments": {"movie_id": seed["id"]}}
            problems, execution2, error2 = evaluate_calls([follow], schema_doc, executor,
                                                          effective)
            schema_problems += problems
            execution += execution2
            called.append(follow)
            extra["seed"] = {"id": seed["id"], "title": seed["title"]}
            extra["derived_constraints"] = derived
            extra["effective_constraints"] = effective
            extra["seed_genre_count"] = len(seed_genres)
            if error2:
                error_state = error2
    elif constraints:
        # A stated condition the tool cannot carry (a genre next to a title prefix) is applied
        # to the returned rows server-side instead of being dropped (R7-2).
        for item in execution:
            if item.get("results"):
                item["results"] = [row for row in item["results"]
                                   if not constraint_problems(row, constraints)]
                item["constraints_applied"] = constraints
                if not item["results"] and item.get("status") == "OK":
                    item["status"] = "EMPTY_RESULT"
                    error_state = "EMPTY_RESULT"
        rows = [row for item in execution for row in item.get("results", [])]
    if behavior == "tools" and called and called[-1]["name"] == "search_movies_by_title":
        # R2-3 only requires a question when several catalog rows claim the same title. A single
        # exact row next to prefix/substring noise is not ambiguous, so it is answered directly.
        query = str(called[-1]["arguments"].get("query", ""))
        exact_rows = [row for row in rows if normalized(row["title"]) == normalized(query)]
        ambiguous = len({row["id"] for row in exact_rows}) > 1 or \
            (not exact_rows and len(rows) > 1)
        if ambiguous:
            candidates = exact_rows or rows
            behavior = "clarify"
            extra["disambiguation_candidates"] = [
                {"id": row["id"], "title": row["title"], "year": row["year"],
                 "director": row["director"]} for row in candidates[:3]]
            extra["answer"] = disambiguation_answer(candidates)
    extra.setdefault("answer", f"[server-rendered {len(rows)} rows]")
    return {"called": called, "execution": execution, "schema_problems": schema_problems,
            "behavior": behavior, "error_state": error_state, "extra": extra}


def run_case(case: dict, variant: str, client: OllamaClient | None, model: str,
             schema_doc: dict, executor: ToolExecutor,
             replay: dict | None = None, resolver: TitleResolver | None = None,
             guard_annotations: bool = False) -> dict:
    tools = schema_doc["tools"]
    record: dict = {"case_id": case["case_id"], "kind": case["kind"],
                    "intent": case["intent"], "variant": variant, "query": case["text"],
                    "allows_clarification": bool(case.get("allows_clarification"))}
    behavior = "answer"
    error_state = None
    called: list[dict] = []
    execution: list[dict] = []
    schema_problems: list[str] = []

    if variant in ("G1", "G1-T"):
        # ``B15c-3``: the execution half gets guarded annotations, so any read of a golden slot or
        # constraint raises instead of quietly turning the run into an oracle lookup.
        executable = guard_case_annotations(case) if guard_annotations else case
        result = run_g1(executable, schema_doc, executor, resolver)
        called = result["called"]
        execution = result["execution"]
        schema_problems = result["schema_problems"]
        behavior = result["behavior"]
        error_state = result["error_state"]
        record.update(result["extra"])
    else:
        if replay is not None:
            # G2 replays G0's recorded calls: the server layer is the only changed variable.
            called = [{"name": call["name"], "arguments": dict(call["arguments"])}
                      for call in replay["tool_calls"]]
            record.update({"latency_ms": replay.get("latency_ms"),
                           "answer": replay.get("answer", ""), "reused_from": "G0",
                           "prompt_eval_count": replay.get("prompt_eval_count"),
                           "eval_count": replay.get("eval_count"), "model_calls": 0})
        else:
            if client is None:
                raise EvalError("G0/G2 need an Ollama endpoint; pass --base-url")
            g0 = run_g0(case, client, model, tools)
            called = g0["tool_calls"]
            record.update({"latency_ms": g0["latency_ms"], "answer": g0["content"],
                           "prompt_eval_count": g0["prompt_eval_count"],
                           "eval_count": g0["eval_count"], "model_calls": 1})
        behavior = "tools" if called else "answer"
        if variant == "G2" and called:
            mapped: list[dict] = []
            for call in called:
                arguments = dict(call["arguments"])
                for key in ("genres", "preferred_genres"):
                    if isinstance(arguments.get(key), list):
                        genres, unmapped = map_genres(schema_doc, arguments[key])
                        if unmapped:
                            record.setdefault("mapping_notes", []).append(
                                f"{key}: unmapped {unmapped}")
                        arguments[key] = genres
                mapped.append({"name": call["name"], "arguments": arguments})
            called = mapped
            record["mapped_tool_calls"] = mapped
        schema_problems, execution, error_state = evaluate_calls(
            called, schema_doc, executor, reject_invalid=(variant == "G2"))

    record["tool_calls"] = called
    record["execution"] = execution
    record["behavior"] = behavior
    record.update(score_case(case, called, schema_problems, execution, behavior, error_state,
                             executor, answer=record.get("answer") or "",
                             disambiguation_candidates=record.get("disambiguation_candidates")))
    annotate_parse_outcome(case, record, executor)
    returned_ids = {row["id"] for item in execution for row in item.get("results", [])}
    record["prose_id_hallucinations"] = sorted(
        {match for match in re.findall(r"\btt\d{5,}\b", record.get("answer") or "")}
        - returned_ids)
    record["failure_modes"] = failure_modes(case, record)
    record["task_success"] = is_task_success(case, record)
    return record


def annotate_parse_outcome(case: dict, record: dict, executor: ToolExecutor) -> None:
    """Score the resolver's decision against the catalog (plan §4.5 parse metrics).

    Runs after execution, so it may read the golden title; it resolves that title through the same
    catalog lookup the resolver used and asks whether the chosen id is one of its rows. A chosen
    id outside that set is a ``wrong_movie`` - the red-line figure that must be attributed case by
    case whenever it is above zero.
    """
    resolver = record.get("resolver") or {}
    if not resolver.get("triggered"):
        record["parse_ok"] = None
        record["wrong_movie"] = None
        return
    titles: list[str] = []
    for slots in [case["required_slots"], *case.get("accept_alternative_slots", [])]:
        for key in ("query", "seed_title"):
            value = slots.get(key)
            if isinstance(value, str) and value not in titles:
                titles.append(value)
    expected_ids: set[str] = set()
    for title in titles:
        # Exact title rows only: a prefix hit would accept "Superman II" as a correct resolution of
        # the seed "Superman" and hide a real wrong-movie (plan §4.5 red-line metric). Prefix rows
        # are the fallback only when the title has no exact row at all.
        rows = executor.exact_title_ids(title) or executor.title_candidates(title, limit=25)
        expected_ids |= {row["id"] for row in rows}
    chosen = resolver.get("chosen_movie_id")
    record["parse_expected_ids"] = sorted(expected_ids)
    # A triggered resolution that ends in a clarification is a parse failure (plan §4.5), not an
    # unmeasured case: counting it as "no attempt" would flatter the parse success rate.
    record["parse_ok"] = bool(chosen is not None and chosen in expected_ids)
    record["wrong_movie"] = bool(chosen is not None and chosen not in expected_ids)


# --------------------------------------------------------------------------------------
# Modes
# --------------------------------------------------------------------------------------

def print_dry_run(schema_doc: dict, cases_doc: dict, queries_doc: dict,
                  routing_doc: dict, queries_path: Path, routing_path: Path,
                  resolver_cache_path: Path) -> None:
    enum = genre_enum(schema_doc)
    alias_targets = sorted({value for values in GENRE_ALIASES.values() for value in values})
    alias_canonical_keys = {GENRE_CANONICAL.get(value, value) for value in alias_targets}
    print(f"schema:            {TOOL_SCHEMA_PATH.relative_to(REPO_ROOT)} "
          f"({schema_doc['contract_version']})")
    print(f"schema_hash:       {schema_doc['schema_hash']}")
    print(f"genre enum:        {len(enum)} values")
    print(f"tools:             {[tool['function']['name'] for tool in schema_doc['tools']]}")
    print(f"queries:           {repo_relative(queries_path)} "
          f"({len(queries_doc['queries'])} queries, "
          f"{queries_doc.get('heldout_version') or queries_doc.get('golden_version')})")
    print(f"routing:           {repo_relative(routing_path)} "
          f"({len(routing_doc['annotations'])} entries, {routing_doc['routing_version']}, "
          f"annotates sha256 {routing_doc['annotates']['sha256'][:12]}…)")
    print(f"tool cases:        {len(cases_doc['tool_cases'])} "
          f"({cases_doc['golden_version']})")
    print(f"prompt:            {PROMPT_ID} sha256 "
          f"{hashlib.sha256(PROMPT_TEXT.encode()).hexdigest()[:12]}…")
    print(f"resolver:          {RESOLVER_PROMPT_ID} sha256 {hashlib.sha256(RESOLVER_PROMPT.encode()).hexdigest()[:12]}… "
          f"timeout {RESOLVER_TIMEOUT_S:g}s, cache {repo_relative(resolver_cache_path)}")
    print(f"genre aliases:     {len(GENRE_ALIASES)} terms -> "
          f"{len(alias_targets)} targets / {len(alias_canonical_keys)} canonical keys "
          f"({GENRE_ALIAS_DOC['aliases_version']}); canonical groups "
          f"{GENRE_ALIAS_DOC.get('canonical_groups')}; "
          f"{len(GENRE_ALIAS_DOC.get('unsupported') or [])} unsupported")
    print(f"title aliases:     {len(TITLE_ALIAS_DOC.get('entries') or [])} entries "
          f"({TITLE_ALIAS_DOC['aliases_version']}, frozen empty by C10)")
    print("variants:          G0 pure LLM / G1 deterministic / G1-T G1 + query-time title "
          "resolver / G2 G0 + server validation / G3 not measured")
    print("execution:         search_movies_by_title, filter_movies, get_movie_card (SQL); "
          "find_similar_movies, recommend_movies via deterministic evaluation stand-in")
    print("model calls:       G0 every case; G1 and G1-T non-CJK 0; G1-T CJK title 1 candidate "
          "request per unresolved title (timeout -> clarification)")
    print("\ncase plan (first 15):")
    for case in build_cases(cases_doc, queries_doc, routing_doc, "both", None)[:15]:
        print(f"  {case['case_id']:<4} {case['kind']:<6} {case['expected_behavior']:<28} "
              f"tools={case['expected_tools']} slots={case['required_slots']}")
    print("\ndry run: no network call, no SQL, no evaluation run.")


def percentile(values: list[float], fraction: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, int(round(fraction * (len(ordered) - 1)))))
    return round(ordered[index], 1)


def summarise(records: list[dict]) -> dict:
    latencies = [record["latency_ms"] for record in records
                 if isinstance(record.get("latency_ms"), (int, float))
                 and record["latency_ms"] > 0]
    modes: dict[str, int] = {}
    for record in records:
        for mode in record["failure_modes"]:
            modes[mode] = modes.get(mode, 0) + 1

    def block(subset: list[dict]) -> dict:
        return {"cases": len(subset),
                "task_success": sum(1 for r in subset if r["task_success"]),
                "routing_success": sum(1 for r in subset if r["passed"])}

    return {
        "cases": len(records),
        "task_success": sum(1 for r in records if r["task_success"]),
        "tools_ok": sum(1 for r in records if r["tools_ok"]),
        "tools_complete": sum(1 for r in records if r["tools_complete"]),
        "slots_ok": sum(1 for r in records if r["slots_ok"]),
        "schema_ok": sum(1 for r in records if r["schema_ok"]),
        "behavior_ok": sum(1 for r in records if r["behavior_ok"]),
        "grounding_ok": sum(1 for r in records if r["grounding_ok"]),
        "result_constraints_checked": sum(1 for r in records
                                          if r["result_constraint_ok"] is not None),
        "result_constraints_ok": sum(1 for r in records if r["result_constraint_ok"]),
        "deferred": sum(1 for r in records if r["execution_deferred"]),
        "model_calls": sum(int(r.get("model_calls", 0)) for r in records),
        "latency_samples": len(latencies),
        "latency_p50_ms": percentile(latencies, 0.5),
        "latency_p95_ms": percentile(latencies, 0.95),
        "failure_modes": dict(sorted(modes.items(), key=lambda item: -item[1])),
        "by_kind": {kind: block([r for r in records if r["kind"] == kind])
                    for kind in sorted({r["kind"] for r in records})},
        "by_intent": {intent: block([r for r in records if r["intent"] == intent])
                      for intent in sorted({r["intent"] for r in records})},
        # Routing and resolution stay separate (plan §4.5): routing = tool/slot choice, resolution
        # = did the Chinese title end up on the right catalog row. "routing_success" is the only
        # name for the routing figure (it used to be recorded twice, as "passed" and here).
        "routing_success": sum(1 for r in records if r["passed"]),
        "clarification_style_success": sum(1 for r in records
                                           if r.get("clarification_style")
                                           and r.get("allows_clarification")),
        "resolver": resolver_summary(records),
    }


def resolver_summary(records: list[dict]) -> dict:
    triggered = [r for r in records if (r.get("resolver") or {}).get("triggered")]
    latencies = [(r.get("resolver") or {}).get("latency_ms") for r in triggered
                 if isinstance((r.get("resolver") or {}).get("latency_ms"), (int, float))
                 and ((r.get("resolver") or {}).get("latency_ms") or 0) > 0]
    outcomes: dict[str, int] = {}
    steps: dict[str, int] = {}
    for record in triggered:
        resolver = record["resolver"]
        outcomes[resolver.get("outcome")] = outcomes.get(resolver.get("outcome"), 0) + 1
        steps[resolver.get("step")] = steps.get(resolver.get("step"), 0) + 1
    wrong = [r["case_id"] for r in triggered if r.get("wrong_movie")]

    def rate(numerator: int, denominator: int) -> float | None:
        return round(numerator / denominator, 4) if denominator else None

    return {
        "triggered": len(triggered),
        "parse_success": sum(1 for r in triggered if r.get("parse_ok")),
        "parse_success_rate": rate(sum(1 for r in triggered if r.get("parse_ok")),
                                   len(triggered)),
        "candidate_reject": sum(1 for r in triggered
                                if (r["resolver"].get("outcome") == "rejected")),
        "candidate_reject_rate": rate(sum(1 for r in triggered
                                          if r["resolver"].get("outcome") == "rejected"),
                                      len(triggered)),
        "wrong_movie": len(wrong),
        "wrong_movie_rate": rate(len(wrong), len(triggered)),
        "wrong_movie_cases": wrong,
        "fallback_to_clarify": sum(1 for r in triggered
                                   if r["resolver"].get("degraded_to") == "clarify"),
        "fallback_rate": rate(sum(1 for r in triggered
                                  if r["resolver"].get("degraded_to") == "clarify"),
                              len(triggered)),
        "timeouts": sum(1 for r in triggered if r["resolver"].get("timed_out")),
        "alias_hits": sum(1 for r in triggered if r["resolver"].get("cache") == "alias"),
        # Only a recorded decision counts as a cache hit: an alias hit is the (empty by C10) alias
        # table, not the run cache, and counting it would inflate the warm-run figure.
        "cache_hits": sum(1 for r in triggered
                          if r["resolver"].get("cache") in ("hit", "replay")),
        "cache_hit_rate": rate(sum(1 for r in triggered
                                   if r["resolver"].get("cache") in ("hit", "replay")),
                               len(triggered)),
        "model_calls": sum(int(r["resolver"].get("model_calls", 0)) for r in triggered),
        "latency_samples": len(latencies),
        "latency_p50_ms": percentile(latencies, 0.5),
        "latency_p95_ms": percentile(latencies, 0.95),
        "outcomes": dict(sorted(outcomes.items(), key=lambda item: -item[1])),
        "steps": dict(sorted(steps.items(), key=lambda item: -item[1])),
    }


def self_test_case(case_id: str, text: str, expected_tools: list[str],
                   slots: dict | None = None, acceptable: list[str] | None = None,
                   allows_clarification: bool = False, expected_behavior: str = "call_tools_and_answer",
                   result_constraints: dict | None = None) -> dict:
    """Minimal case record for the scripted self-test (mirrors ``build_cases``)."""
    return {
        "case_id": case_id, "kind": "query", "text": text,
        "lang": "zh" if has_han(text) else "en", "intent": "search_by_title",
        "expected_tools": expected_tools, "required_slots": slots or {},
        "accept_alternative_slots": [], "result_constraints": result_constraints or {},
        "expected_refusal": False, "allows_clarification": allows_clarification,
        "cjk_title": has_han(text), "expected_behavior": expected_behavior,
        "expected_error_code": "NONE", "acceptable_movie_ids": acceptable or [],
    }


def check_semantics(executor: ToolExecutor, schema_doc: dict) -> tuple[dict, list[str]]:
    """Pure-function and catalog assertions for the B1.5c execution semantics."""
    checks: dict[str, str] = {}
    problems: list[str] = []

    def expect(label: str, condition: bool, detail: str = "") -> None:
        checks[label] = "ok" if condition else f"FAILED {detail}".strip()
        if not condition:
            problems.append(f"{label}: {detail}" if detail else label)

    def seed_year_of(run: dict) -> int | None:
        """Year of the row the seed title search resolved to (first row of the lookup)."""
        items = run.get("execution") or []
        rows = (items[0].get("results") or []) if items else []
        return rows[0].get("year") if rows else None

    expect("canonical: Sci-Fi == Science Fiction", canonical_genres(["Sci-Fi", "Science Fiction"]) == ["Sci-Fi"])
    expect("canonical: order independent", canonical_genres(["Science Fiction", "Sci-Fi"]) == ["Sci-Fi"])
    expect("canonical: unrelated keys kept", canonical_genres(["Action", "Sci-Fi"]) == ["Action", "Sci-Fi"])
    one_key = genre_and_sql(["Sci-Fi"])
    expect("genre AND counts canonical keys", "HAVING COUNT(DISTINCT CASE" in one_key
           and one_key.rstrip().endswith("= 1)"), one_key)
    expect("genre AND retrieves both spellings", "'Sci-Fi'" in one_key and "'Science Fiction'" in one_key)
    expect("genre AND: two keys are two conditions", genre_and_sql(["Action", "Sci-Fi"]).rstrip().endswith("= 2)"))
    # R-1: the canonical group is only worth having if the *retrieval* half unions the members.
    # B1.5f labels `filter_movies{genres:["Music"]}` by the union (25 rows), so a regression to
    # "only Music" must fail here rather than as an unexplainable blind-set miss.
    expect("genre AND: Music includes Musical",
           "'Music'" in genre_member_sql(["Music"])
           and "'Musical'" in genre_member_sql(["Music"]), genre_member_sql(["Music"])),
    expect("director: abbreviation == full name", directors_equal("M.Mann", "Michael Mann"))
    expect("director: different initial", not directors_equal("Ant.Mann", "Michael Mann"))
    expect("director: bare surname never expands", not directors_equal("Mann", "Michael Mann"))
    expect("director: Q17 full name != surname", not directors_equal("Steven Spielberg", "Spielberg"))
    expect("director: ~ is a space", directors_equal("Woody~Allen", "Woody Allen"))
    expect("director: identical strings equal", directors_equal("Hitchcock", "Hitchcock"))
    # The harness must implement the frozen data file, not a private paraphrase of it.
    recorded = {item.get("id"): item for item in PERSON_NORMALIZATION_DOC.get("assertions", [])}
    for assertion_id in ("q17-spielberg-not-equal", "initial-abbreviation-equal",
                         "bare-surname-not-equal"):
        assertion = recorded.get(assertion_id) or {}
        left, right = assertion.get("left"), assertion.get("right")
        expect(f"director: {assertion_id} matches the data file",
               isinstance(left, str) and isinstance(right, str)
               and directors_equal(left, right) is bool(assertion.get("equal")),
               f"{left!r} vs {right!r} recorded equal={assertion.get('equal')}")
    expect("route: Chinese year range", extract_years(SELF_TEST_SAMPLES["year-range"])
           == {"year_from": 2002, "year_to": 2003})
    expect("route: genre alias file", extract_genres(SELF_TEST_SAMPLES["genre-alias"]) == ["War"])
    expect("route: no false war match", extract_genres(SELF_TEST_SAMPLES["war-boundary"]) == [])
    # B15c-5's split survives in the alias data: `musical` still targets the `Musical` enum value
    # and `music` the `Music` value. B1.5e's R-1 canonical group folds them for retrieval and slot
    # comparison only, which is asserted separately below.
    expect("route: musical != music",
           GENRE_ALIASES[SELF_TEST_SAMPLES["musical"]] == ["Musical"]
           and GENRE_ALIASES[SELF_TEST_SAMPLES["music"]] == ["Music"])
    expect("route: Chinese genre words",
           extract_genres(SELF_TEST_SAMPLES["multi-genre"]) == ["Action", "Sci-Fi"],
           str(extract_genres(SELF_TEST_SAMPLES["multi-genre"])))
    expect("route: title + year goes to the search tool",
           route_deterministically(
               self_test_case("S", SELF_TEST_SAMPLES["title-year"], []))["tool_calls"]
           == [{"name": "search_movies_by_title",
                "arguments": {"query": "Notorious", "year": 1946}}])
    expect("route: rated at least N", extract_rating(SELF_TEST_SAMPLES["rating-at-least"]) == 6.0)
    expect("route: title lookup verb 查",
           route_deterministically(self_test_case(
               "S", SELF_TEST_SAMPLES["title-verb-zh"], []))["tool_calls"]
           == [{"name": "search_movies_by_title", "arguments": {"query": "Casablanca"}}])
    expect("route: director named as 推荐几部 <Name> 在…",
           extract_director(SELF_TEST_SAMPLES["director-clause-zh"]) == "Kurosawa")
    expect("route: director named as Recommend some <Name> <genre>",
           extract_director(SELF_TEST_SAMPLES["director-clause-en"]) == "Kubrick")
    expect("route: a genre word is not a director name",
           extract_director(SELF_TEST_SAMPLES["genre-not-director"]) is None)
    out_of_catalog = route_deterministically(
        self_test_case("S-OOC", SELF_TEST_SAMPLES["out-of-catalog"], []))
    misfires = [key for key in ("not-refusal-score", "not-refusal-music", "not-refusal-released",
                                "not-refusal-studio")
                if detect_refusal(SELF_TEST_SAMPLES[key]) is not None]
    expect("route: out-of-catalog request refuses without a tool",
           out_of_catalog["action"] == "refuse" and out_of_catalog["tool_calls"] == []
           and not misfires,
           f"action={out_of_catalog['action']!r} misfires={misfires}")
    title_region = route_deterministically(
        self_test_case("S-TITLE", SELF_TEST_SAMPLES["title-region-genres"], []))
    expect("route: genre words inside a title are not constraints",
           title_region.get("result_constraints")
           == {"genres": ["Comedy"], "year_from": 2005, "year_to": 2007},
           str(title_region.get("result_constraints")))
    seed_titles = {key: extract_title(SELF_TEST_SAMPLES[key])
                   for key in ("seed-title-liked", "seed-title-enjoyed", "seed-title-watched")}
    expect("route: seed title is the title, not the clause",
           list(seed_titles.values()) == ["Rear Window", "Casablanca", "Vertigo"],
           str(seed_titles))
    sentence_titles = {key: extract_title(SELF_TEST_SAMPLES[key])
                       for key in ("sentence-not-title-show", "sentence-not-title-find")}
    expect("route: a sentence is not a title", set(sentence_titles.values()) == {None},
           str(sentence_titles))
    # A verb of taste also opens an ordinary clause. If such a capture became the title, the
    # masking step in route_deterministically would delete the stated genre/years as well.
    decade_clause = route_deterministically(
        self_test_case("S-CLAUSE-DECADE", SELF_TEST_SAMPLES["title-clause-decade"], []))
    expect("route: a verb clause keeps its stated years",
           extract_title(SELF_TEST_SAMPLES["title-clause-decade"]) is None
           and decade_clause["tool_calls"]
           == [{"name": "filter_movies", "arguments": {"year_from": 1990, "year_to": 1999}}],
           f"title={extract_title(SELF_TEST_SAMPLES['title-clause-decade'])!r} "
           f"calls={decade_clause['tool_calls']}")
    genre_clause = route_deterministically(
        self_test_case("S-CLAUSE-GENRE", SELF_TEST_SAMPLES["title-clause-genre"], []))
    expect("route: a verb clause keeps its stated genre and years",
           extract_title(SELF_TEST_SAMPLES["title-clause-genre"]) is None
           and genre_clause["tool_calls"]
           == [{"name": "filter_movies",
                "arguments": {"genres": ["Comedy"], "year_from": 1990, "year_to": 1999}}],
           f"title={extract_title(SELF_TEST_SAMPLES['title-clause-genre'])!r} "
           f"calls={genre_clause['tool_calls']}")
    expect("route: a verb before a real title keeps the title",
           extract_title(SELF_TEST_SAMPLES["title-verb-keeps-title"]) == "Trainspotting",
           repr(extract_title(SELF_TEST_SAMPLES["title-verb-keeps-title"])))
    expect("route: a Latin title glued to Chinese text",
           extract_title(SELF_TEST_SAMPLES["title-glued-zh"]) == "The Matrix",
           repr(extract_title(SELF_TEST_SAMPLES["title-glued-zh"])))
    expect("route: plural genre word",
           extract_genres(SELF_TEST_SAMPLES["plural-genre"]) == ["Documentary"],
           str(extract_genres(SELF_TEST_SAMPLES["plural-genre"])))
    demonym = route_deterministically(
        self_test_case("S-DEM", SELF_TEST_SAMPLES["demonym-documentaries"], []))
    expect("route: a demonym is not a director",
           extract_director(SELF_TEST_SAMPLES["demonym-documentaries"]) is None
           and extract_director(SELF_TEST_SAMPLES["demonym-comedies"]) is None
           and extract_genres(SELF_TEST_SAMPLES["demonym-documentaries"]) == ["Documentary"]
           and demonym["tool_calls"]
           == [{"name": "recommend_movies",
                "arguments": {"preferred_genres": ["Documentary"],
                              "year_range": {"from": 1960, "to": 1969}}}],
           str(demonym["tool_calls"]))
    expect("route: musical and music share one canonical key",
           canonical_genres(["Musical"]) == ["Music"], str(canonical_genres(["Musical"])))
    expect("resolver: strict JSON accepted",
           parse_resolver_candidates('{"candidates": ["A Title"]}') == ["A Title"])
    expect("resolver: extra field rejected",
           parse_resolver_candidates('{"candidates": ["A Title"], "note": "x"}') is None)
    expect("resolver: zero candidates rejected",
           parse_resolver_candidates('{"candidates": []}') is None)
    expect("resolver: four candidates rejected",
           parse_resolver_candidates('{"candidates": ["A","B","C","D"]}') is None)
    expect("resolver: non-JSON rejected", parse_resolver_candidates("not json") is None)
    expect("resolver: Chinese candidate rejected",
           parse_resolver_candidates('{"candidates": ["卡萨布兰卡"]}') is None)
    expect("resolver: CJK detection ignores 《》",
           has_han("《The Vikings》") is False
           and has_han(SELF_TEST_SAMPLES["resolver-han"]) is True)
    if executor.mysql is None:
        checks["catalog checks"] = "skipped (--skip-db)"
        return checks, problems

    heat = executor.execute("search_movies_by_title", {"query": "Heat", "limit": 10})
    titles = [row["title"] for row in heat["results"]]
    expect("search: exact rows first (Heat)", titles[:2] == ["Heat", "Heat"], str(titles))
    expect("search: match reported", heat["match"] == "exact", str(heat["match"]))
    expect("filter: canonical Sci-Fi is reachable",
           executor.execute("filter_movies", {"genres": ["Sci-Fi", "Science Fiction"], "limit": 5})["results"] != [])
    expect("filter: multi genre is AND",
           all({"Action", "Sci-Fi"} <= set(row["genres"]) for row in
               executor.execute("filter_movies", {"genres": ["Action", "Sci-Fi"], "limit": 5})["results"]))
    expect("director: catalog matching is not an expansion",
           set(executor.matching_directors("Michael Mann")) == {"M.Mann", "Michael Mann"}
           and executor.matching_directors("Mann") == [])
    recommended = executor.execute(
        "recommend_movies",
        {"preferred_genres": ["Comedy"], "year_range": {"from": 1950, "to": 1959},
         "director": "B.Wilder", "limit": 5}, {})
    expect("recommend: director normalization narrows the answer set",
           bool(recommended["results"])
           and all(directors_equal(row["director"], "B.Wilder") for row in recommended["results"]),
           str([row["director"] for row in recommended["results"]]))
    expect("recommend: multi genre is AND",
           all({"Comedy"} <= set(row["genres"])
               for row in executor.execute("recommend_movies",
                                           {"preferred_genres": ["Comedy", "Romance"],
                                            "year_range": {"from": 1950, "to": 1959},
                                            "limit": 5}, {})["results"]))
    seed = {"movie_id": "MM33"}  # Heat (1995), Michael Mann: stored as M.Mann
    similar = executor.execute("find_similar_movies", dict(seed, limit=5), {})
    expect("similar: seed excluded", all(row["id"] != "MM33" for row in similar["results"]))
    expect("similar: same director first", bool(similar["results"])
           and directors_equal(similar["results"][0]["director"], "M.Mann"),
           str([row["director"] for row in similar["results"]]))
    narrowed = executor.execute("find_similar_movies", dict(seed, limit=5),
                                {"genres": ["Action", "Crime"], "year_from": 1990, "year_to": 1999})
    expect("similar: constraints narrow before ranking",
           all(not constraint_problems(row, {"genres": ["Action", "Crime"], "year_from": 1990,
                                            "year_to": 1999})
               for row in narrowed["results"])
           and narrowed["candidate_pool"] <= SIMILAR_POOL,
           f"pool={narrowed.get('candidate_pool')}")
    derived_run = run_g1(self_test_case("S-SEED", SELF_TEST_SAMPLES["similar-derived"],
                                        ["search_movies_by_title"]), schema_doc, executor)
    derived = derived_run["extra"].get("derived_constraints") or {}
    effective = derived_run["extra"].get("effective_constraints") or {}
    followed = [row for item in derived_run["execution"][1:] for row in item.get("results", [])]
    seed_id = (derived_run["extra"].get("seed") or {}).get("id")
    seed_year = seed_year_of(derived_run)
    expect("similar: derived constraints come from the seed",
           bool(derived.get("genres"))
           and derived_run["extra"].get("seed_genre_count") == len(derived["genres"])
           and effective == derived and bool(followed)
           and all(not constraint_problems(row, effective) for row in followed)
           and all(row["id"] != seed_id for row in followed)
           and seed_year is not None
           and derived.get("year_from") == seed_year - seed_year % 10
           and derived.get("year_to") == seed_year - seed_year % 10 + 9,
           f"derived={derived} effective={effective} rows={len(followed)} seed={seed_id} "
           f"seed_year={seed_year}")
    explicit_run = run_g1(self_test_case("S-EXPLICIT", SELF_TEST_SAMPLES["similar-explicit"],
                                         ["search_movies_by_title"]), schema_doc, executor)
    explicit_extra = explicit_run["extra"]
    expect("similar: explicit text constraints win",
           explicit_extra.get("derived_constraints") == {}
           and explicit_extra.get("effective_constraints")
           == (explicit_extra.get("route") or {}).get("result_constraints"),
           f"derived={explicit_extra.get('derived_constraints')} "
           f"effective={explicit_extra.get('effective_constraints')}")
    # The two gate branches of R-2 (owner ruling ③b2): a seed with one or with three-plus
    # canonical genres must not derive anything. Without these two the `len(seed_genres) == 2`
    # guard - the reason Q16/Q35/Q55/Q56 stay green - had no assertion at all.
    one_genre_run = run_g1(self_test_case("S-ONE-GENRE", SELF_TEST_SAMPLES["similar-one-genre"],
                                         ["search_movies_by_title"]), schema_doc, executor)
    one_genre_extra = one_genre_run["extra"]
    expect("similar: a one-genre seed derives nothing",
           one_genre_extra.get("seed_genre_count") == 1
           and one_genre_extra.get("derived_constraints") == {}
           and one_genre_extra.get("effective_constraints")
           == (one_genre_extra.get("route") or {}).get("result_constraints"),
           f"seed_genre_count={one_genre_extra.get('seed_genre_count')} "
           f"derived={one_genre_extra.get('derived_constraints')} "
           f"effective={one_genre_extra.get('effective_constraints')}")
    many_genre_run = run_g1(self_test_case("S-MANY-GENRES", SELF_TEST_SAMPLES["similar-many-genres"],
                                           ["search_movies_by_title"]), schema_doc, executor)
    many_genre_extra = many_genre_run["extra"]
    expect("similar: a three-plus genre seed derives nothing",
           (many_genre_extra.get("seed_genre_count") or 0) >= 3
           and many_genre_extra.get("derived_constraints") == {}
           and many_genre_extra.get("effective_constraints")
           == (many_genre_extra.get("route") or {}).get("result_constraints"),
           f"seed_genre_count={many_genre_extra.get('seed_genre_count')} "
           f"derived={many_genre_extra.get('derived_constraints')} "
           f"effective={many_genre_extra.get('effective_constraints')}")
    # Text that names genres but no year keeps the derived decade and the stated genres: the
    # derivation is per key (`{**derived, **stated}`), not a whole-dict replacement.
    stated_genres_run = run_g1(
        self_test_case("S-STATED-GENRES", SELF_TEST_SAMPLES["similar-stated-genres"],
                       ["search_movies_by_title"]), schema_doc, executor)
    stated_genres_extra = stated_genres_run["extra"]
    mixed_derived = stated_genres_extra.get("derived_constraints") or {}
    mixed_effective = stated_genres_extra.get("effective_constraints") or {}
    mixed_stated = (stated_genres_extra.get("route") or {}).get("result_constraints") or {}
    mixed_seed_year = seed_year_of(stated_genres_run)
    expect("similar: stated genres keep the derived decade",
           "genres" not in mixed_derived
           and mixed_seed_year is not None
           and mixed_derived.get("year_from") == mixed_seed_year - mixed_seed_year % 10
           and mixed_derived.get("year_to") == mixed_seed_year - mixed_seed_year % 10 + 9
           and mixed_stated.get("genres") == ["Comedy"]
           and mixed_effective == {**mixed_derived, **mixed_stated},
           f"derived={mixed_derived} stated={mixed_stated} effective={mixed_effective} "
           f"seed_year={mixed_seed_year}")
    # B15e-5 asks for the DB half too: a demonym must not turn the 1960s pool into ``1 = 0``.
    demonym_run = run_g1(self_test_case("S-DEMONYM-DB", SELF_TEST_SAMPLES["demonym-documentaries"],
                                        ["recommend_movies"]), schema_doc, executor)
    demonym_rows = [row for item in demonym_run["execution"] for row in item.get("results", [])]
    expect("recommend: a demonym phrase keeps a non-empty 1960s pool",
           bool(demonym_rows)
           and all(not constraint_problems(row, {"genres": ["Documentary"],
                                              "year_from": 1960, "year_to": 1969})
                   for row in demonym_rows),
           f"rows={len(demonym_rows)}")
    return checks, problems


def check_annotation_sentinel(schema_doc: dict, cases: list[dict], executor: ToolExecutor) -> list[str]:
    """``B15c-3``: executing every case must never read a golden annotation."""
    problems: list[str] = []
    probe = guard_case_annotations(cases[0] if cases else {"case_id": "probe", "text": ""})
    for key in ANNOTATION_KEYS:
        try:
            probe[key]
        except EvalError:
            continue
        problems.append(f"the annotation guard does not reject {key!r} (the sentinel would be "
                        f"vacuous)")
    for case in cases:
        try:
            run_g1(guard_case_annotations(case), schema_doc, executor)
        except EvalError as error:
            problems.append(f"{case['case_id']}: {error}")
    return problems


def run_resolver_self_test(schema_doc: dict, executor: ToolExecutor) -> tuple[dict, list[str]]:
    """G1-T plumbing: scripted candidates through the real catalog, no model, no network."""
    report: dict = {}
    problems: list[str] = []
    if executor.mysql is None:
        report["status"] = "skipped (--skip-db: catalog verification needs the catalog)"
        return report, problems
    resolver = TitleResolver(ScriptedResolverClient(), "scripted", executor, use_cache=False)
    cases = [
        ("S-CJK", SELF_TEST_SAMPLES["resolver-cjk"], {"query": "Casablanca"}),
        ("S-LATIN", SELF_TEST_SAMPLES["resolver-latin"], {"query": "The Vikings"}),
    ]
    for case_id, text, slots in cases:
        case = self_test_case(case_id, text, ["search_movies_by_title"], slots=slots)
        record = run_case(case, "G1-T", None, "scripted", schema_doc, executor, resolver=resolver,
                          guard_annotations=True)
        report[case_id] = {"model_calls": record["model_calls"],
                           "resolver": record.get("resolver"),
                           "tools_called": record["tools_called"],
                           "parse_ok": record["parse_ok"], "answer": record.get("answer")}
    for case_id, zh, expected_outcome in (
        ("S-INVALID", SELF_TEST_SAMPLES["resolver-invalid"], "invalid_json"),
        ("S-EXTRA-FIELD", SELF_TEST_SAMPLES["resolver-extra-field"], "invalid_json"),
        ("S-REJECTED", SELF_TEST_SAMPLES["resolver-rejected"], "rejected"),
        ("S-TIMEOUT", SELF_TEST_SAMPLES["resolver-timeout"], "timeout"),
    ):
        resolution = resolver.resolve(zh)
        report[case_id] = {"outcome": resolution["outcome"], "step": resolution["step"],
                           "model_calls": resolution["model_calls"],
                           "degraded_to": resolution["degraded_to"]}
        if resolution["outcome"] != expected_outcome:
            problems.append(f"{case_id}: outcome {resolution['outcome']!r} != {expected_outcome!r}")
        if resolution["chosen_movie_id"] is not None:
            problems.append(f"{case_id}: a failed resolution must not choose a movie id")
        case = self_test_case(case_id, f"帮我找《{zh}》这部电影", ["search_movies_by_title"])
        record = run_case(case, "G1-T", None, "scripted", schema_doc, executor, resolver=resolver)
        report[case_id]["behaviour"] = record["behavior"]
        report[case_id]["tools_called"] = record["tools_called"]
        if record["tools_called"]:
            problems.append(f"{case_id}: an unverified title must not reach "
                            f"search_movies_by_title (called {record['tools_called']})")
        if record["behavior"] != "clarify":
            problems.append(f"{case_id}: behaviour {record['behavior']!r} != 'clarify'")

    with tempfile.TemporaryDirectory() as scratch:
        cache_file = Path(scratch) / "resolver-cache.json"
        warm = TitleResolver(ScriptedResolverClient(), "scripted", executor,
                             cache_path=cache_file)
        first = warm.resolve(SELF_TEST_SAMPLES["resolver-zh-title"])
        warm.save()
        second = warm.resolve(SELF_TEST_SAMPLES["resolver-zh-title"])
        replay = TitleResolver(None, "scripted", executor, replay_path=cache_file)
        replayed = replay.resolve(SELF_TEST_SAMPLES["resolver-zh-title"])
        report["S-CACHE"] = {"cache": second["cache"], "model_calls": second["model_calls"],
                             "chosen_movie_id": second["chosen_movie_id"]}
        report["S-REPLAY"] = {"cache": replayed["cache"],
                             "model_calls": replayed["model_calls"],
                             "chosen_movie_id": replayed["chosen_movie_id"]}
        if first["chosen_movie_id"] != "C127":
            problems.append(f"S-CACHE: first resolution chose {first['chosen_movie_id']}")
        if second["cache"] != "hit" or second["model_calls"] != 0:
            problems.append(f"S-CACHE: warm run is {second['cache']} with "
                            f"{second['model_calls']} model calls")
        if replayed["cache"] != "replay" or replayed["model_calls"] != 0 \
                or replayed["chosen_movie_id"] != "C127":
            problems.append(f"S-REPLAY: replay is {replayed['cache']} with "
                            f"{replayed['model_calls']} model calls, chose "
                            f"{replayed['chosen_movie_id']}")
        # A cache written under another prompt or another model is not this run's evidence: it is
        # ignored instead of replayed, and an explicit replay of it is refused (plan §4.4).
        stale_file = Path(scratch) / "stale-cache.json"
        stale_file.write_text(json.dumps(dict(load_json(cache_file), prompt_sha256="0" * 64)),
                              encoding="utf-8")
        stale = TitleResolver(ScriptedResolverClient(), "scripted", executor,
                              cache_path=stale_file)
        report["S-CACHE"]["stale"] = {"status": stale.cache_status,
                                      "entries": len(stale.cache),
                                      "problems": stale.cache_problems}
        if stale.cache_status != "stale-ignored" or stale.cache:
            problems.append("S-CACHE: a cache recorded under another prompt must be ignored")
        other_model = TitleResolver(ScriptedResolverClient(), "another-model", executor,
                                    cache_path=cache_file)
        if other_model.cache or not other_model.cache_problems:
            problems.append("S-CACHE: a cache recorded for another model must be ignored")
        try:
            TitleResolver(None, "scripted", executor, replay_path=stale_file)
        except EvalError:
            pass
        else:
            problems.append("S-REPLAY: a replay file recorded under another prompt must be "
                            "refused")
        try:
            replay.resolve(SELF_TEST_SAMPLES["replay-miss"])
        except EvalError:
            pass
        else:
            problems.append("S-REPLAY: a replay miss must fail instead of calling the model")

    # S-AMBIG-WINDOW: a title with several exact catalog rows is narrowed by the year window the
    # query itself stated before the resolution is called ambiguous (plan §7). Heat is 1995 (MM33)
    # and 1972 (PMo15); without a clue the resolver must ask, with the window it must pick 1995.
    window_resolver = TitleResolver(ScriptedResolverClient(), "scripted", executor, use_cache=False)
    unresolved = window_resolver.resolve("Heat")
    windowed = window_resolver.resolve("Heat", {"year_from": 1990, "year_to": 1999})
    report["S-AMBIG-WINDOW"] = {
        "without_clue": {"outcome": unresolved["outcome"], "model_calls": unresolved["model_calls"],
                          "candidates": [row["id"] for row in unresolved["catalog_verified"]]},
        "with_window": {"outcome": windowed["outcome"], "model_calls": windowed["model_calls"],
                        "chosen_movie_id": windowed["chosen_movie_id"],
                        "narrowed_to": windowed.get("narrowed_to")},
    }
    if unresolved["outcome"] != "ambiguous" or unresolved["model_calls"] != 0:
        problems.append(f"S-AMBIG-WINDOW: a multi-row title without a clue must be ambiguous "
                        f"({unresolved['outcome']!r}, {unresolved['model_calls']} model calls)")
    if windowed["outcome"] != "resolved" or windowed["chosen_movie_id"] != "MM33" \
            or windowed.get("narrowed_to") != ["MM33"] or windowed["model_calls"] != 0:
        problems.append(f"S-AMBIG-WINDOW: the 1990-1999 window must resolve Heat to MM33 "
                        f"({windowed['outcome']!r}, chose {windowed['chosen_movie_id']!r}, "
                        f"narrowed to {windowed.get('narrowed_to')})")

    happy = report["S-CJK"]
    if happy["model_calls"] != 1:
        problems.append(f"S-CJK: model_calls {happy['model_calls']} != 1")
    if (happy["resolver"] or {}).get("chosen_title") != "Casablanca":
        problems.append(f"S-CJK: chosen_title {happy['resolver']} is not the verified catalog title")
    if happy["parse_ok"] is not True:
        problems.append(f"S-CJK: parse_ok {happy['parse_ok']} is not True")
    latin = report["S-LATIN"]
    if latin["model_calls"] != 0:
        problems.append(f"S-LATIN: a non-CJK title must not call the model ({latin['model_calls']})")
    if (latin["resolver"] or {}).get("outcome") != "not_applicable":
        problems.append(f"S-LATIN: resolver outcome {latin['resolver']} is not not_applicable")
    return report, problems


def run_self_test(args: argparse.Namespace, schema_doc: dict, cases_doc: dict,
                  queries_doc: dict, routing_doc: dict) -> int:
    print("SELF-TEST: scripted tool calls and scripted resolver candidates, not model output; "
          "no model was called.")
    executor = ToolExecutor(None if args.skip_db else ReadOnlyMysql())
    payload: dict = {"mode": "SELF-TEST (scripted, not a model measurement)",
                     "schema_hash": schema_doc["schema_hash"], "runs": [], "assertions": {}}
    failures: list[str] = []

    semantics, problems = check_semantics(executor, schema_doc)
    payload["assertions"]["semantics"] = semantics
    print(f"  semantics: {len(semantics)} checks, {len(problems)} problem(s)")
    for label, outcome in semantics.items():
        print(f"    {label:<46} {outcome}")
    failures.extend(problems)

    independence = check_self_test_independence()
    payload["assertions"]["self_test_independence"] = {
        "utterances": len(SELF_TEST_UTTERANCES), "problems": independence}
    print(f"  self-test independence: {len(SELF_TEST_UTTERANCES)} utterance(s) vs the held-out "
          f"queries, {len(independence)} problem(s)")
    failures.extend(independence)

    built = build_cases(cases_doc, queries_doc, routing_doc, "both", None)
    sentinel = check_annotation_sentinel(schema_doc, built, executor)
    payload["assertions"]["annotation_sentinel"] = {
        "cases": len(built), "problems": sentinel}
    print(f"  annotation sentinel: {len(built)} cases executed, {len(sentinel)} problem(s)")
    failures.extend(sentinel)

    resolver_report, resolver_problems = run_resolver_self_test(schema_doc, executor)
    payload["assertions"]["resolver"] = resolver_report
    print(f"  resolver pipeline: {resolver_report.get('status', 'ran')} , "
          f"{len(resolver_problems)} problem(s)")
    for case_id in ("S-CJK", "S-LATIN", "S-INVALID", "S-EXTRA-FIELD", "S-REJECTED", "S-TIMEOUT",
                    "S-CACHE", "S-REPLAY", "S-AMBIG-WINDOW"):
        if case_id in resolver_report:
            print(f"    {case_id:<14} {resolver_report[case_id]}")
    failures.extend(resolver_problems)

    cases = [case for case in built if case["text"] in SELF_TEST_SCRIPT]
    if len(cases) != len(SELF_TEST_SCRIPT):
        raise EvalError("self-test cases are missing from the fixtures")
    for variant in ("G0", "G2"):
        records = [run_case(case, variant, ScriptedClient(), "scripted", schema_doc, executor)
                   for case in cases]
        summary = summarise(records)
        payload["runs"].append({"variant": variant, "summary": summary, "records": records})
        print(f"  {variant}: {summary}")
        for record in records:
            print(f"    {record['case_id']:<4} schema_ok={record['schema_ok']!s:<5} "
                  f"tools_ok={record['tools_ok']!s:<5} error={record['error_state_observed']} "
                  f"rows={sum(len(item.get('results', [])) for item in record['execution'])} "
                  f"problems={record['schema_problems']}")
    if args.out:
        out_path = Path(args.out)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        payload["failures"] = failures
        out_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                            encoding="utf-8")
        print(f"written: {out_path}")
    if failures:
        print(f"\nSELF-TEST FAILED ({len(failures)} problem(s)):", file=sys.stderr)
        for failure in failures:
            print(f"  - {failure}", file=sys.stderr)
        return 1
    print("\nSELF-TEST OK: execution semantics, the annotation sentinel and the G1-T resolver "
          "pipeline behave as specified (no model was called).")
    return 0


def run_variant(args: argparse.Namespace, schema_doc: dict, cases_doc: dict,
                queries_doc: dict, routing_doc: dict,
                queries_path: Path, routing_path: Path) -> int:
    cases = build_cases(cases_doc, queries_doc, routing_doc, args.cases, args.ids)
    if not cases:
        raise EvalError("no case matched the selection")
    if args.limit:
        cases = cases[: args.limit]
    if len(cases) > args.full_run_threshold and not args.allow_full_run:
        raise EvalError(
            f"refusing to evaluate {len(cases)} cases above --full-run-threshold "
            f"{args.full_run_threshold}: use --limit, or --allow-full-run once the full run is "
            f"approved."
        )
    replay: dict[str, dict] = {}
    if args.reuse_g0:
        if args.variant != "G2":
            raise EvalError("--reuse-g0 only applies to --variant G2")
        replay = {record["case_id"]: record
                  for record in load_json(Path(args.reuse_g0))["records"]}
        missing = [case["case_id"] for case in cases if case["case_id"] not in replay]
        if missing:
            raise EvalError(f"{args.reuse_g0} has no G0 record for {missing[:5]}")
    client = None
    if args.variant in ("G0", "G2") and not replay:
        client = OllamaClient(args.base_url, timeout_s=args.timeout)
    executor = ToolExecutor(None if args.skip_db else ReadOnlyMysql())
    resolver = None
    if args.variant == "G1-T":
        # A replay never contacts the network: the resolver raises on a title it has no recorded
        # decision for, so it does not get a client at all (plan §4.6).
        resolver = TitleResolver(
            None if args.replay_resolver else OllamaClient(args.base_url, timeout_s=args.timeout),
            args.model, executor,
            cache_path=Path(args.resolver_cache), use_cache=not args.no_cache,
            replay_path=Path(args.replay_resolver) if args.replay_resolver else None,
            timeout_s=args.resolver_timeout)
    records = [run_case(case, args.variant, client, args.model, schema_doc, executor,
                        replay.get(case["case_id"]), resolver=resolver,
                        guard_annotations=args.assert_no_annotation_read)
               for case in cases]
    model_calls = sum(int(record.get("model_calls", 0)) for record in records)
    # A warm G1-T run never contacts the model, so it reports the digest the cold run pinned.
    model_digest = resolver.model_digest if resolver is not None else None
    if args.variant in ("G0", "G2") and client is not None:
        model_digest = client.tags().get(args.model)
    elif replay:
        model_digest = _replayed_digest(replay)
    elif resolver is not None:
        if resolver.replay is not None:
            model_digest = resolver.model_digest
        elif model_calls:
            # Pinned only when the resolver actually asked the model, so a CJK-free run stays offline.
            model_digest = resolver.client.tags().get(args.model)
    for record in records:
        record["model_digest"] = model_digest
    if resolver is not None:
        resolver.model_digest = model_digest
        resolver.save()
    summary = summarise(records)
    payload = {
        "variant": args.variant,
        "contract_version": schema_doc["contract_version"],
        "schema_hash": schema_doc["schema_hash"],
        "model": ((resolver.replay_model or args.model)
                  if args.variant == "G1-T" and resolver is not None and resolver.replay
                  else args.model if args.variant in ("G0", "G2", "G1-T") else None),
        "model_digest": model_digest,
        "base_url": args.base_url if args.variant in ("G0", "G2", "G1-T") else None,
        "temperature": TEMPERATURE,
        "prompt_id": PROMPT_ID,
        "prompt_sha256": hashlib.sha256(PROMPT_TEXT.encode()).hexdigest(),
        "queries_path": repo_relative(queries_path),
        "routing_path": repo_relative(routing_path),
        "queries_sha256": hashlib.sha256(queries_path.read_bytes()).hexdigest(),
        "routing_sha256": hashlib.sha256(routing_path.read_bytes()).hexdigest(),
        # The alias files are inputs too: the report has to pin the revision they were read at.
        "alias_files": {
            "genre_aliases": {"path": repo_relative(GENRE_ALIASES_PATH),
                              "aliases_version": GENRE_ALIAS_DOC["aliases_version"],
                              "sha256": hashlib.sha256(GENRE_ALIASES_PATH.read_bytes()).hexdigest()},
            "person_normalization": {
                "path": repo_relative(PERSON_NORMALIZATION_PATH),
                "normalization_version": PERSON_NORMALIZATION_DOC["normalization_version"],
                "sha256": hashlib.sha256(
                    PERSON_NORMALIZATION_PATH.read_bytes()).hexdigest()},
            "title_aliases": {"path": repo_relative(TITLE_ALIASES_PATH),
                             "aliases_version": TITLE_ALIAS_DOC["aliases_version"],
                             "sha256": hashlib.sha256(
                                 TITLE_ALIASES_PATH.read_bytes()).hexdigest()},
        },
        "resolver": {
            "enabled": resolver is not None,
            "prompt_id": RESOLVER_PROMPT_ID,
            "prompt_sha256": hashlib.sha256(RESOLVER_PROMPT.encode()).hexdigest(),
            "timeout_s": args.resolver_timeout if resolver is not None else None,
            "cache_path": (repo_relative(args.resolver_cache)
                           if resolver is not None and not args.no_cache else None),
            "replay_resolver": args.replay_resolver,
            "no_cache": bool(args.no_cache),
            "cache_state_at_load": resolver.cache_status if resolver is not None else None,
            "cache_problems": resolver.cache_problems if resolver is not None else [],
            "embedding": "disabled (plan §4.3 step 4 is optional and off by default)",
            "alias_entries": len(TITLE_ALIAS_DOC.get("entries") or []),
            "aliases_version": TITLE_ALIAS_DOC["aliases_version"],
        },
        "annotation_guard": bool(args.assert_no_annotation_read),
        "db_snapshot": cases_doc.get("snapshot"),
        "reused_from": args.reuse_g0,
        "summary": summary,
        "records": records,
    }
    print(f"variant {args.variant}: {summary}")
    for record in records:
        print(f"  {record['case_id']:<4} task={record['task_success']!s:<5} "
              f"routing={record['passed']!s:<5} "
              f"tools={record['tool_calls'] and [c['name'] for c in record['tool_calls']]} "
              f"slots_ok={record['slots_ok']} schema_ok={record['schema_ok']} "
              f"behavior_ok={record['behavior_ok']} rows={record['rows_returned']} "
              f"error={record['error_state_observed']}")
        if record["slot_problems"]:
            print(f"        slot problems: {record['slot_problems']}")
        if record["schema_problems"]:
            print(f"        schema problems: {record['schema_problems']}")
        if record["failure_modes"]:
            print(f"        failure modes: {record['failure_modes']}")
        if record.get("resolver"):
            print(f"        resolver: {record['resolver']}")
    if args.out:
        out_path = Path(args.out)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                            encoding="utf-8")
        print(f"written: {out_path}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dry-run", action="store_true",
                        help="default: validate fixtures and print the plan (no network, no SQL)")
    parser.add_argument("--self-test", action="store_true",
                        help="run scripted tool calls through the G0/G2 path and scripted candidates "
                             "through the G1-T resolver, plus the execution/semantics assertions "
                             "(NOT a model measurement)")
    parser.add_argument("--variant", choices=("G0", "G1", "G1-T", "G2", "G3"),
                        help="variant to run: G0 pure LLM, G1 deterministic, G1-T deterministic + "
                             "query-time title resolver, G2 G0 + server validation, G3 not measured")
    parser.add_argument("--cases", choices=("tool", "queries", "both"), default="tool",
                        help="which fixture to run (default: the 15 tool cases)")
    parser.add_argument("--queries-path", default=str(QUERIES_PATH),
                        help="query fixture to score (default: the frozen 60 queries; pass the "
                             "held-out file for a held-out run)")
    parser.add_argument("--routing-path", default=str(ROUTING_PATH),
                        help="routing annotations for --queries-path (default: the frozen 60; its "
                             "annotates.sha256 must match the query file)")
    parser.add_argument("--ids", help="comma-separated case ids, e.g. T01,T11,Q05,H01")
    parser.add_argument("--limit", type=int, default=0, help="evaluate at most N cases")
    parser.add_argument("--allow-full-run", action="store_true",
                        help="allow more cases than --full-run-threshold (approved full runs only)")
    parser.add_argument("--full-run-threshold", type=int, default=10,
                        help="case count above which --allow-full-run is required (default 10)")
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--timeout", type=float, default=DEFAULT_TIMEOUT_S,
                        help="timeout for a routing call (G0/G2)")
    parser.add_argument("--resolver-timeout", type=float, default=RESOLVER_TIMEOUT_S,
                        help=f"timeout for one G1-T candidate request (default {RESOLVER_TIMEOUT_S:g}s); "
                             "a timeout degrades to a clarification")
    parser.add_argument("--resolver-cache", default=str(DEFAULT_RESOLVER_CACHE),
                        help="G1-T run cache, written outside the repository by default")
    parser.add_argument("--no-cache", action="store_true",
                        help="G1-T cold run: neither read nor write the resolver cache")
    parser.add_argument("--replay-resolver",
                        help="G1-T: replay the recorded resolver decisions in this JSON instead of "
                             "calling the model (model_calls stays 0); a title with no recorded "
                             "decision, or a file recorded under another prompt/model, is a hard "
                             "error rather than a model call")
    parser.add_argument("--assert-no-annotation-read", action="store_true",
                        help="fail the run if the execution half reads a golden annotation "
                             "(required_slots / result_constraints / acceptable_movie_ids)")
    parser.add_argument("--skip-db", action="store_true", help="no SQL; execution is recorded as skipped")
    parser.add_argument("--reuse-g0",
                        help="G2 only: replay the tool calls recorded in this G0 JSON instead of "
                             "calling the model again")
    parser.add_argument("--out", help="write the run record as JSON to this path")
    args = parser.parse_args()
    queries_path = Path(args.queries_path).resolve()
    routing_path = Path(args.routing_path).resolve()

    schema_doc, cases_doc, queries_doc, routing_doc = load_fixtures(queries_path, routing_path)
    if args.variant == "G3":
        print("G3 (server-rendered answers + one retry) is out of scope this period "
              "(plan OPEN-b4 = optional/not required): not measured.", file=sys.stderr)
        return 2
    if args.no_cache and args.replay_resolver:
        raise EvalError("--no-cache and --replay-resolver contradict each other: pick one")
    if args.self_test:
        return run_self_test(args, schema_doc, cases_doc, queries_doc, routing_doc)
    if args.variant is None:
        print_dry_run(schema_doc, cases_doc, queries_doc, routing_doc, queries_path, routing_path,
                      Path(args.resolver_cache))
        return 0
    return run_variant(args, schema_doc, cases_doc, queries_doc, routing_doc, queries_path,
                       routing_path)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except EvalError as error:
        print(f"eval error: {error}", file=sys.stderr)
        sys.exit(2)

#!/usr/bin/env python3
"""Validate the B1.5c held-out fixtures (30 queries, their routing annotations and the alias files).

Stdlib only. The frozen B1.5a/B1.5b artifacts are read, never written: the held-out set lives in its
own files with its own version so that appending/altering `docs/ai/golden/queries.b1-5.json` (whose
SHA-256 `query-routing.b1-5.json` binds) can never happen by accident.

Nine independent assertions
---------------------------
1. **structure** — the held-out file validates against ``$defs/heldoutQuerySet`` of
   ``docs/ai/golden-set.schema.json`` (``H01``–``H30`` id pattern, required fields, no extra fields),
   ``heldout_version`` and the ``snapshot`` block match the frozen snapshot, and the recorded
   ``quota`` equals the recomputed one.
2. **ids and tracks** — exactly ``H01``–``H30`` in order, no duplicate query text, 10/10/10 across
   ``zh_zh``/``zh_en``/``en_en`` with ``lang`` consistent with the track.
3. **intent quota** — search 5 / filter 9 / similar 7 / recommend 3 / clarify-or-refuse 6
   (4 refusals + 2 clarifications), 4 ``multi_slot`` cross-marks, 2 ``cjk_title`` cases.
4. **slice exclusion** — no acceptable id appears in the frozen 60 queries or the 15 tool cases, and
   no held-out seed title is one of the frozen 15 ``similar_movies`` seeds.
5. **alias files** — ``title-aliases.json`` is frozen empty (``entries: []``, decision C10) and the
   two ``cjk_title`` queries do not appear in it; ``genre-aliases.json`` /
   ``person-normalization.json`` carry a version and the Q17 5-vs-29 assertion.
6. **synthetic rows and decades** — no ``tt9000001…tt9000008`` / ``tt0499470`` / ``tt0499471`` id or
   title (the ``database/dev-seed.sql`` rows), no 2020s row, 3–5 cases entirely in the 1990s and at
   most 2 in the 2010s. Needs the catalog; ``--skip-db`` skips it.
7. **frozen SHA-256** — the queries/routing files still hash to the values recorded here, and
   ``annotates.sha256`` binds the held-out queries (not the frozen 60).
8. **routing consistency** — annotation ids/intents/slots/tools/constraints mirror the queries and
   the ``schema_contract`` records the current v3 hash.
9. **catalog referential** — every acceptable id exists in the catalog and the recorded ``rows=``
   count matches the id list.

Usage
-----
    python3 scripts/eval/check_heldout.py              # every check, including the database
    python3 scripts/eval/check_heldout.py --skip-db    # file-level checks only
    python3 scripts/eval/check_heldout.py --quiet      # summary only

Exit code 0 = every check passed.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from check_golden import REPO_ROOT, ReadOnlyMysql, load_json, validate  # noqa: E402
from check_schema import (  # noqa: E402  (frozen artefacts + shared vocabularies)
    ARG_SLOTS,
    ROUTING_ONLY_SLOTS,
    TOOL_NAMES,
    TOOL_SCHEMA_PATH,
    YEAR_MAX,
    YEAR_MIN,
    genre_enum,
)

GOLDEN_SCHEMA_PATH = REPO_ROOT / "docs" / "ai" / "golden-set.schema.json"
HELDOUT_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "queries-heldout.b1-5c.json"
HELDOUT_ROUTING_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "query-routing-heldout.b1-5c.json"
FROZEN_QUERIES_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "queries.b1-5.json"
FROZEN_TOOL_CASES_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "tool-cases.b1-5.json"
FROZEN_ROUTING_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "query-routing.b1-5.json"
GENRE_ALIASES_PATH = REPO_ROOT / "docs" / "ai" / "aliases" / "genre-aliases.json"
PERSON_NORMALIZATION_PATH = REPO_ROOT / "docs" / "ai" / "aliases" / "person-normalization.json"
TITLE_ALIASES_PATH = REPO_ROOT / "docs" / "ai" / "aliases" / "title-aliases.json"

# One-shot freeze (plan §8.5): the numbers were written down before the measurement run and may only
# change with a heldout_version bump plus a "run N" note in the report.
FROZEN_HELDOUT_SHA256 = "d471c57a3b27d93f499003573ad690b08580691e911fe5127e35b76cd191abe4"
FROZEN_HELDOUT_ROUTING_SHA256 = "5608dd4c774f09d5b8e9d1f0dd7bf99602539aac0474dc170b1648b393f34aba"

EXPECTED_IDS = [f"H{i:02d}" for i in range(1, 31)]
TRACKS = ("zh_zh", "zh_en", "en_en")
EXPECTED_PER_TRACK = 10
TRACK_LANG = {"zh_zh": "zh", "zh_en": "zh", "en_en": "en"}
INTENT_QUOTA = {
    "search_by_title": 5,
    "filter_catalog": 9,
    "similar_movies": 7,
    "recommend_by_preference": 3,
    "clarify_or_refuse": 6,
}
EXPECTED_REFUSALS = 4
EXPECTED_CLARIFICATIONS = 2
EXPECTED_MULTI_SLOT = 4
EXPECTED_CJK = 2
DECADE_1990S_RANGE = (3, 5)
DECADE_2010S_MAX = 2

# database/dev-seed.sql synthetic rows and the Movie1/Movie2 leftovers (scout §2.4)
SYNTHETIC_IDS = {"tt9000001", "tt9000002", "tt9000003", "tt9000004", "tt9000005",
                 "tt9000006", "tt9000007", "tt9000008", "tt0499470", "tt0499471"}
SYNTHETIC_TITLES = {
    "Neon Horizon", "Midnight Circuit", "Paper Moons", "Quiet Signal", "Last Train Home",
    "Laughing Algorithm", "The Empty Theater", "Zero Rating", "Movie1", "Movie2",
}

INTENT_TOOLS = {
    "search_by_title": ["search_movies_by_title"],
    "filter_catalog": ["filter_movies"],
    "similar_movies": ["search_movies_by_title", "find_similar_movies"],
    "recommend_by_preference": ["recommend_movies"],
    "clarify_or_refuse": [],
}
CLARIFICATION_TOOLS = ["search_movies_by_title"]
RESULT_CONSTRAINTS = ("genres", "year_from", "year_to", "min_rating", "director")
ROW_REF_PATTERN = re.compile(r"rows=(\d+) \| ids=(.*)$")
ANNOTATION_KEYS = {
    "query_id", "intent", "expected_refusal", "golden_predicate", "expected_tools",
    "required_slots", "accept_alternative_slots", "result_constraints", "allows_clarification",
    "notes",
}
ROOT_KEYS = {
    "$comment", "routing_version", "annotates", "schema_contract", "slot_vocabulary",
    "result_constraint_vocabulary", "annotations",
}


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _is_int(value: object) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _is_number(value: object) -> bool:
    return _is_int(value) or isinstance(value, float)


# --------------------------------------------------------------------------------------
# 1. structure
# --------------------------------------------------------------------------------------

def check_structure(queries_doc: dict, golden_schema: dict) -> list[str]:
    errors: list[str] = []
    for violation in validate(queries_doc, golden_schema["$defs"]["heldoutQuerySet"],
                              golden_schema, "heldout-queries"):
        errors.append(f"golden-set.schema.json: {violation}")

    if not re.fullmatch(r"b1-5c-heldout/v[0-9]+", queries_doc.get("heldout_version", "")):
        errors.append(f"heldout_version {queries_doc.get('heldout_version')!r} is not b1-5c-heldout/vN")
    frozen_snapshot = load_json(FROZEN_QUERIES_PATH).get("snapshot")
    if queries_doc.get("snapshot") != frozen_snapshot:
        errors.append("snapshot block differs from queries.b1-5.json (the catalog snapshot must be shared)")

    quota = queries_doc.get("quota", {})
    recomputed_intents = {name: 0 for name in INTENT_QUOTA}
    per_track = {track: 0 for track in TRACKS}
    for case in queries_doc.get("queries", []):
        recomputed_intents[case["intent"]] += 1
        per_track[case["track"]] += 1
    if quota.get("tracks") != per_track:
        errors.append(f"quota.tracks {quota.get('tracks')} != recomputed {per_track}")
    if quota.get("intents") != recomputed_intents:
        errors.append(f"quota.intents {quota.get('intents')} != recomputed {recomputed_intents}")
    clarifications = sum(1 for c in queries_doc.get("queries", []) if c.get("allows_clarification"))
    split = quota.get("clarify_or_refuse_split", {})
    if split != {"refusal": EXPECTED_REFUSALS, "clarification": clarifications}:
        errors.append(f"quota.clarify_or_refuse_split {split} != 4 refusals / {clarifications} clarifications")
    return errors


# --------------------------------------------------------------------------------------
# 2. ids and tracks
# --------------------------------------------------------------------------------------

def check_ids_and_tracks(queries_doc: dict) -> list[str]:
    errors: list[str] = []
    queries = queries_doc.get("queries", [])
    ids = [case.get("id") for case in queries]
    if ids != EXPECTED_IDS:
        errors.append(f"held-out ids {ids} != H01..H30 (order matters: annotations are positional)")
    if len(set(ids)) != len(ids):
        errors.append("held-out ids are not unique")

    texts: dict[str, str] = {}
    per_track = {track: 0 for track in TRACKS}
    for case in queries:
        cid = case.get("id")
        track = case.get("track")
        if track not in TRACK_LANG:
            errors.append(f"{cid}: unknown track {track!r}")
            continue
        per_track[track] += 1
        expected_lang = TRACK_LANG[track]
        if case.get("lang") != expected_lang:
            errors.append(f"{cid}: lang {case.get('lang')!r} != {expected_lang!r} for track {track}")
        text = case.get("query", "")
        if text in texts:
            errors.append(f"{cid}: duplicate query text (already used by {texts[text]})")
        texts[text] = cid
    for track, count in per_track.items():
        if count != EXPECTED_PER_TRACK:
            errors.append(f"track {track}: {count} cases, expected {EXPECTED_PER_TRACK}")
    return errors


# --------------------------------------------------------------------------------------
# 3. intent quota and cross marks
# --------------------------------------------------------------------------------------

def check_intent_quota(queries_doc: dict) -> list[str]:
    errors: list[str] = []
    queries = queries_doc.get("queries", [])
    counts = {name: 0 for name in INTENT_QUOTA}
    refusals = clarifications = multi_slot = cjk = 0
    for case in queries:
        cid = case.get("id")
        intent = case.get("intent")
        counts[intent] = counts.get(intent, 0) + 1
        refusal = bool(case.get("expects_no_match"))
        if refusal:
            refusals += 1
            if intent != "clarify_or_refuse":
                errors.append(f"{cid}: expects_no_match is only for clarify_or_refuse cases")
            if case.get("acceptable_movie_ids"):
                errors.append(f"{cid}: a refusal has no acceptable movie ids")
        elif not case.get("acceptable_movie_ids"):
            errors.append(f"{cid}: a non-refusal case needs acceptable_movie_ids")
        if case.get("allows_clarification"):
            clarifications += 1
            if intent != "clarify_or_refuse":
                errors.append(f"{cid}: allows_clarification is only for clarify_or_refuse cases")
        if case.get("multi_slot"):
            multi_slot += 1
            if intent not in ("filter_catalog", "similar_movies"):
                errors.append(f"{cid}: multi_slot cross-mark belongs to filter_catalog/similar_movies")
        if case.get("cjk_title"):
            cjk += 1
            if intent not in ("search_by_title", "similar_movies", "recommend_by_preference"):
                errors.append(f"{cid}: cjk_title has no title slot for intent {intent}")
    for name, expected in INTENT_QUOTA.items():
        if counts.get(name) != expected:
            errors.append(f"intent {name}: {counts.get(name)} cases, expected {expected}")
    if refusals != EXPECTED_REFUSALS:
        errors.append(f"{refusals} refusal cases, expected {EXPECTED_REFUSALS}")
    if clarifications != EXPECTED_CLARIFICATIONS:
        errors.append(f"{clarifications} clarification cases, expected {EXPECTED_CLARIFICATIONS}")
    if multi_slot != EXPECTED_MULTI_SLOT:
        errors.append(f"{multi_slot} multi_slot cases, expected {EXPECTED_MULTI_SLOT}")
    if cjk != EXPECTED_CJK:
        errors.append(f"{cjk} cjk_title cases, expected {EXPECTED_CJK}")
    return errors


# --------------------------------------------------------------------------------------
# 4. slice exclusion against the frozen 60 + 15
# --------------------------------------------------------------------------------------

def check_slice_exclusion(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    frozen_ids: set[str] = set()
    for path, key in ((FROZEN_QUERIES_PATH, "queries"), (FROZEN_TOOL_CASES_PATH, "tool_cases")):
        for case in load_json(path).get(key, []):
            frozen_ids |= set(case.get("acceptable_movie_ids") or [])
    frozen_seeds = {
        annotation["required_slots"]["seed_title"]
        for annotation in load_json(FROZEN_ROUTING_PATH).get("annotations", [])
        if (annotation.get("required_slots") or {}).get("seed_title")
    }
    for case in queries_doc.get("queries", []):
        cid = case["id"]
        overlap = sorted(set(case.get("acceptable_movie_ids") or []) & frozen_ids)
        if overlap:
            errors.append(f"{cid}: acceptable ids overlap the frozen 75: {overlap}")
    for annotation in routing_doc.get("annotations", []):
        seed = (annotation.get("required_slots") or {}).get("seed_title")
        if seed and seed in frozen_seeds:
            errors.append(f"{annotation['query_id']}: seed title {seed!r} is already a frozen B1.5b seed")
    return errors


# --------------------------------------------------------------------------------------
# 5. alias files (C10 empty title table, genre/person data files)
# --------------------------------------------------------------------------------------

def check_aliases(queries_doc: dict) -> list[str]:
    errors: list[str] = []
    title_doc = load_json(TITLE_ALIASES_PATH)
    if not re.fullmatch(r"b1-5c-title-aliases/v[0-9]+", title_doc.get("aliases_version", "")):
        errors.append(f"title-aliases aliases_version {title_doc.get('aliases_version')!r} is not vN")
    entries = title_doc.get("entries")
    if entries != []:
        errors.append(
            f"title-aliases.entries must be the empty list (decision C10 froze the table empty); "
            f"found {len(entries or [])} entries"
        )
    for index, entry in enumerate(entries or []):
        for key in ("zh_title", "movie_id", "source", "verified_by"):
            if not entry.get(key):
                errors.append(f"title-aliases.entries[{index}]: missing {key!r}")
    entry_schema = title_doc.get("entry_schema", {})
    for key in ("verified_by", "source"):
        if key not in entry_schema:
            errors.append(f"title-aliases.entry_schema does not document {key!r}")

    zh_titles = {entry.get("zh_title") for entry in entries or []}
    cjk_cases = [case for case in queries_doc.get("queries", []) if case.get("cjk_title")]
    for case in cjk_cases:
        for zh_title in zh_titles:
            if zh_title and zh_title in case["query"]:
                errors.append(
                    f"{case['id']}: held-out CJK title {zh_title!r} is pre-seeded in title-aliases "
                    f"(that would make the case measure the table, not the resolver)"
                )

    genre_doc = load_json(GENRE_ALIASES_PATH)
    if not re.fullmatch(r"b1-5c-aliases/v[0-9]+", genre_doc.get("aliases_version", "")):
        errors.append(f"genre-aliases aliases_version {genre_doc.get('aliases_version')!r} is not vN")
    person_doc = load_json(PERSON_NORMALIZATION_PATH)
    if not re.fullmatch(r"b1-5c-person-normalization/v[0-9]+",
                        person_doc.get("normalization_version", "")):
        errors.append(
            f"person-normalization normalization_version "
            f"{person_doc.get('normalization_version')!r} is not vN"
        )
    q17 = {item.get("id"): item for item in person_doc.get("assertions", [])}
    assertion = q17.get("q17-spielberg-not-equal")
    if assertion is None:
        errors.append("person-normalization.json is missing the Q17 5-vs-29 assertion")
    elif (assertion.get("left_rows"), assertion.get("right_rows"), assertion.get("equal")) != (5, 29, False):
        errors.append(
            f"Q17 assertion must record 5 vs 29 with equal=False, got "
            f"{assertion.get('left_rows')} vs {assertion.get('right_rows')} equal={assertion.get('equal')}"
        )
    prohibited = {rule.get("id") for rule in person_doc.get("rules", []) if rule.get("applies") is False}
    if "surname_expansion_prohibited" not in prohibited:
        errors.append("person-normalization.json must record surname_expansion_prohibited as applies=false")
    return errors


# --------------------------------------------------------------------------------------
# 6. synthetic rows and decade distribution
# --------------------------------------------------------------------------------------

def check_years(queries_doc: dict, mysql: ReadOnlyMysql | None) -> list[str]:
    errors: list[str] = []
    all_ids = sorted({movie_id for case in queries_doc["queries"]
                      for movie_id in case.get("acceptable_movie_ids") or []})
    for movie_id in all_ids:
        if movie_id in SYNTHETIC_IDS:
            errors.append(f"synthetic dev-seed id {movie_id} appears in the held-out answers")
    if mysql is None:
        return errors

    if not all_ids:
        return errors + ["no acceptable ids to check against the catalog"]
    quoted = ",".join(f"'{movie_id}'" for movie_id in all_ids)
    rows = mysql.select(
        f"SELECT id, title, year FROM movies WHERE id IN ({quoted})"
    )
    year_by_id = {row[0]: int(row[2]) for row in rows}
    for row in rows:
        if row[1] in SYNTHETIC_TITLES:
            errors.append(
                f"{row[0]}: synthetic dev-seed title {row[1]!r} appears in the held-out answers"
            )
    missing = [movie_id for movie_id in all_ids if movie_id not in year_by_id]
    if missing:
        errors.append(f"{', '.join(missing)}: acceptable ids that do not exist in the catalog")

    per_decade: dict[str, int] = {}
    for case in queries_doc["queries"]:
        years = [year_by_id[movie_id] for movie_id in case.get("acceptable_movie_ids") or []
                 if movie_id in year_by_id]
        if not years:
            continue
        if any(year >= 2020 for year in years):
            errors.append(f"{case['id']}: a 2020s row is not allowed (7 rows, 5 of them synthetic)")
        if all(1990 <= year <= 1999 for year in years):
            per_decade["1990s"] = per_decade.get("1990s", 0) + 1
        if all(2010 <= year <= 2019 for year in years):
            per_decade["2010s"] = per_decade.get("2010s", 0) + 1
    low, high = DECADE_1990S_RANGE
    if not low <= per_decade.get("1990s", 0) <= high:
        errors.append(f"{per_decade.get('1990s', 0)} cases in the 1990s, expected {low}-{high}")
    if per_decade.get("2010s", 0) > DECADE_2010S_MAX:
        errors.append(
            f"{per_decade.get('2010s', 0)} cases in the 2010s, at most {DECADE_2010S_MAX} allowed"
        )
    return errors


# --------------------------------------------------------------------------------------
# 7. frozen hashes
# --------------------------------------------------------------------------------------

def check_frozen_hashes(routing_doc: dict) -> list[str]:
    errors: list[str] = []
    queries_sha = sha256_of(HELDOUT_PATH)
    routing_sha = sha256_of(HELDOUT_ROUTING_PATH)
    if FROZEN_HELDOUT_SHA256.startswith("__"):
        errors.append("FROZEN_HELDOUT_SHA256 has not been filled in")
    elif queries_sha != FROZEN_HELDOUT_SHA256:
        errors.append(
            f"held-out queries sha256 {queries_sha} != frozen {FROZEN_HELDOUT_SHA256} "
            f"(a frozen fixture changed: bump heldout_version and record the run number)"
        )
    if FROZEN_HELDOUT_ROUTING_SHA256.startswith("__"):
        errors.append("FROZEN_HELDOUT_ROUTING_SHA256 has not been filled in")
    elif routing_sha != FROZEN_HELDOUT_ROUTING_SHA256:
        errors.append(
            f"held-out routing sha256 {routing_sha} != frozen {FROZEN_HELDOUT_ROUTING_SHA256}"
        )
    annotates = routing_doc.get("annotates", {})
    if annotates.get("sha256") != queries_sha:
        errors.append(
            f"annotates.sha256 {annotates.get('sha256')} != current held-out queries {queries_sha}"
        )
    if annotates.get("file") != "docs/ai/golden/queries-heldout.b1-5c.json":
        errors.append(
            f"annotates.file {annotates.get('file')!r} must bind the held-out file, never the frozen 60"
        )
    return errors


# --------------------------------------------------------------------------------------
# 8. routing consistency
# --------------------------------------------------------------------------------------

def check_slot_values(cid: str, label: str, slots: dict, enum: list[str]) -> list[str]:
    errors: list[str] = []
    for key, value in slots.items():
        if key in ("genres", "preferred_genres"):
            if not isinstance(value, list) or not value:
                errors.append(f"{cid}: {label}.{key} must be a non-empty list")
                continue
            if len(set(value)) != len(value):
                errors.append(f"{cid}: {label}.{key} contains duplicates")
            for genre in value:
                if genre not in enum:
                    errors.append(f"{cid}: {label}.{key} value {genre!r} is not in the derived enum")
        elif key in ("year_from", "year_to"):
            if not _is_int(value) or not YEAR_MIN <= value <= YEAR_MAX:
                errors.append(f"{cid}: {label}.{key} must be an integer in {YEAR_MIN}-{YEAR_MAX}")
        elif key == "year":
            if not _is_int(value) or not YEAR_MIN <= value <= YEAR_MAX:
                errors.append(f"{cid}: {label}.year must be an integer in {YEAR_MIN}-{YEAR_MAX}")
        elif key == "min_rating":
            if not _is_number(value) or not 0 <= value <= 10:
                errors.append(f"{cid}: {label}.min_rating must be a number in 0-10")
        elif key == "year_range":
            if not isinstance(value, dict) or set(value) != {"from", "to"}:
                errors.append(f"{cid}: {label}.year_range must be {{from,to}}")
                continue
            for bound in ("from", "to"):
                if not _is_int(value[bound]) or not YEAR_MIN <= value[bound] <= YEAR_MAX:
                    errors.append(f"{cid}: {label}.year_range.{bound} must be in {YEAR_MIN}-{YEAR_MAX}")
            if _is_int(value["from"]) and _is_int(value["to"]) and value["from"] > value["to"]:
                errors.append(f"{cid}: {label}.year_range.from > to")
        elif key in ("director", "query", "seed_title"):
            if not isinstance(value, str) or len(value) < 2:
                errors.append(f"{cid}: {label}.{key} must be a string of at least 2 characters")
        else:
            errors.append(f"{cid}: {label} uses unknown slot {key!r}")
    return errors


def check_routing(queries_doc: dict, routing_doc: dict, schema_doc: dict) -> list[str]:
    errors: list[str] = []
    for key in routing_doc:
        if key not in ROOT_KEYS:
            errors.append(f"routing: unexpected root key {key!r}")
    if not re.fullmatch(r"b1-5c-routing/v[0-9]+", routing_doc.get("routing_version", "")):
        errors.append(f"routing_version {routing_doc.get('routing_version')!r} is not b1-5c-routing/vN")
    annotates = routing_doc.get("annotates", {})
    if annotates.get("heldout_version") != queries_doc.get("heldout_version"):
        errors.append("annotates.heldout_version differs from the held-out queries file")
    contract = routing_doc.get("schema_contract", {})
    if contract.get("schema_hash") != schema_doc.get("schema_hash"):
        errors.append("schema_contract.schema_hash differs from tool-schema.b1-5.json")
    if contract.get("contract_version") != schema_doc.get("contract_version"):
        errors.append("schema_contract.contract_version differs from tool-schema.b1-5.json")

    vocabulary = routing_doc.get("slot_vocabulary", {})
    expected_vocabulary = set(ARG_SLOTS) | set(ROUTING_ONLY_SLOTS) | {"year"}
    if set(vocabulary) != expected_vocabulary:
        errors.append(f"slot_vocabulary {sorted(vocabulary)} != {sorted(expected_vocabulary)}")

    enum = genre_enum(schema_doc)
    queries = queries_doc.get("queries", [])
    annotations = routing_doc.get("annotations", [])
    if [entry.get("query_id") for entry in annotations] != [case["id"] for case in queries]:
        errors.append("annotation ids do not match the held-out query ids in order")
    by_id = {case["id"]: case for case in queries}
    for entry in annotations:
        qid = entry.get("query_id")
        case = by_id.get(qid)
        if case is None:
            errors.append(f"{qid}: no such held-out query")
            continue
        for key in entry:
            if key not in ANNOTATION_KEYS:
                errors.append(f"{qid}: unexpected key {key!r}")
        if entry.get("intent") != case["intent"]:
            errors.append(f"{qid}: intent {entry.get('intent')!r} != query intent {case['intent']!r}")
        if entry.get("golden_predicate") != case["snapshot_row_refs"][0]:
            errors.append(f"{qid}: golden_predicate differs from the query's first row ref")
        refusal = entry.get("expected_refusal")
        if not isinstance(refusal, bool):
            errors.append(f"{qid}: expected_refusal must be a boolean")
        elif refusal != bool(case.get("expects_no_match")):
            errors.append(f"{qid}: expected_refusal must mirror expects_no_match")
        if bool(entry.get("allows_clarification")) != bool(case.get("allows_clarification")):
            errors.append(f"{qid}: allows_clarification differs from the query case")

        tools = entry.get("expected_tools", [])
        if len(set(tools)) != len(tools):
            errors.append(f"{qid}: expected_tools contains duplicates")
        for tool in tools:
            if tool not in TOOL_NAMES:
                errors.append(f"{qid}: unknown expected tool {tool!r}")
        if refusal:
            expected_tools = []
        elif case.get("allows_clarification"):
            expected_tools = CLARIFICATION_TOOLS
        else:
            expected_tools = INTENT_TOOLS[case["intent"]]
        if sorted(tools) != sorted(expected_tools):
            errors.append(f"{qid}: expected_tools {tools} != {expected_tools}")

        slots = entry.get("required_slots", {})
        if not isinstance(slots, dict):
            errors.append(f"{qid}: required_slots must be an object")
            slots = {}
        if not slots and not refusal:
            errors.append(f"{qid}: a non-refusal case needs non-empty required_slots")
        errors.extend(check_slot_values(qid, "required_slots", slots, enum))
        for key in slots:
            owners = ARG_SLOTS.get(key) or ROUTING_ONLY_SLOTS.get(key) or ()
            if key == "year":
                owners = ("search_movies_by_title",)
            if not any(owner in tools for owner in owners):
                errors.append(f"{qid}: slot {key!r} belongs to {list(owners)}, not in expected_tools")
            if key == "seed_title" and "search_movies_by_title" not in tools:
                errors.append(f"{qid}: seed_title requires search_movies_by_title for resolution")

        alternatives = entry.get("accept_alternative_slots", [])
        if not isinstance(alternatives, list):
            errors.append(f"{qid}: accept_alternative_slots must be a list")
            alternatives = []
        for index, alternative in enumerate(alternatives):
            if not isinstance(alternative, dict) or not alternative:
                errors.append(f"{qid}: accept_alternative_slots[{index}] must be a non-empty object")
                continue
            if set(alternative) - set(slots):
                errors.append(
                    f"{qid}: accept_alternative_slots[{index}] keys {sorted(alternative)} are not a "
                    f"subset of required_slots keys {sorted(slots)}"
                )
            errors.extend(check_slot_values(qid, f"accept_alternative_slots[{index}]", alternative, enum))

        constraints = entry.get("result_constraints") or {}
        for key in constraints:
            if key not in RESULT_CONSTRAINTS:
                errors.append(f"{qid}: unknown result constraint {key!r}")
        if constraints:
            errors.extend(check_slot_values(qid, "result_constraints", constraints, enum))
            if case["intent"] != "similar_movies":
                errors.append(f"{qid}: result_constraints are only used by similar_movies cases")
            if "search_movies_by_title" not in tools:
                errors.append(f"{qid}: result_constraints need the seed resolution tool")
        if not isinstance(entry.get("notes"), str) or len(entry["notes"]) < 10:
            errors.append(f"{qid}: notes must explain the expectation (>=10 characters)")
    return errors


# --------------------------------------------------------------------------------------
# 9. referential check against the catalog
# --------------------------------------------------------------------------------------

def check_db_ids(queries_doc: dict, mysql: ReadOnlyMysql | None) -> list[str]:
    errors: list[str] = []
    for case in queries_doc["queries"]:
        match = ROW_REF_PATTERN.search(case["snapshot_row_refs"][0])
        if not match:
            errors.append(f"{case['id']}: first snapshot_row_ref does not record 'rows=N | ids=...'")
            continue
        recorded_rows = int(match.group(1))
        recorded_ids = [part for part in match.group(2).split(",") if part and part != "(none)"]
        if recorded_rows != len(recorded_ids):
            errors.append(
                f"{case['id']}: row ref says rows={recorded_rows} but lists {len(recorded_ids)} ids"
            )
        if recorded_ids != case["acceptable_movie_ids"]:
            errors.append(f"{case['id']}: row ref ids differ from acceptable_movie_ids")
    if mysql is None:
        return errors

    all_ids = sorted({movie_id for case in queries_doc["queries"]
                      for movie_id in case.get("acceptable_movie_ids") or []})
    if not all_ids:
        return errors
    quoted = ",".join(f"'{movie_id}'" for movie_id in all_ids)
    present = {row[0] for row in mysql.select(f"SELECT id FROM movies WHERE id IN ({quoted})")}
    for movie_id in all_ids:
        if movie_id not in present:
            errors.append(f"acceptable_movie_ids: {movie_id} does not exist in the catalog")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--skip-db", action="store_true",
                        help="file-level checks only (no mysql, no year/decade checks)")
    parser.add_argument("--quiet", action="store_true", help="summary only")
    args = parser.parse_args()

    queries_doc = load_json(HELDOUT_PATH)
    routing_doc = load_json(HELDOUT_ROUTING_PATH)
    golden_schema = load_json(GOLDEN_SCHEMA_PATH)
    schema_doc = load_json(TOOL_SCHEMA_PATH)
    mysql = None if args.skip_db else ReadOnlyMysql()

    checks = [
        ("1 structure", lambda: check_structure(queries_doc, golden_schema)),
        ("2 ids and tracks", lambda: check_ids_and_tracks(queries_doc)),
        ("3 intent quota", lambda: check_intent_quota(queries_doc)),
        ("4 slice exclusion", lambda: check_slice_exclusion(queries_doc, routing_doc)),
        ("5 alias files", lambda: check_aliases(queries_doc)),
        ("6 synthetic rows and decades", lambda: check_years(queries_doc, mysql)),
        ("7 frozen sha256", lambda: check_frozen_hashes(routing_doc)),
        ("8 routing consistency", lambda: check_routing(queries_doc, routing_doc, schema_doc)),
        ("9 catalog referential", lambda: check_db_ids(queries_doc, mysql)),
    ]
    errors: list[str] = []
    if not args.quiet:
        print(f"held-out file:   {HELDOUT_PATH.relative_to(REPO_ROOT)}")
        print(f"sha256:          {sha256_of(HELDOUT_PATH)}")
        print(f"routing file:    {HELDOUT_ROUTING_PATH.relative_to(REPO_ROOT)}")
        print(f"sha256:          {sha256_of(HELDOUT_ROUTING_PATH)}")
        print(f"contract:        {schema_doc.get('contract_version')} / {schema_doc.get('schema_hash')}")
    for name, check in checks:
        problems = check()
        if problems:
            errors.extend(f"[{name}] {problem}" for problem in problems)
        if not args.quiet:
            print(f"  check {name}: {'OK' if not problems else f'{len(problems)} problem(s)'}")
    if mysql is None and not args.quiet:
        print("  (--skip-db: year, decade and catalog-existence checks are not exercised)")

    if errors:
        print(f"\nFAILED ({len(errors)} problems):")
        for error in errors:
            print(f"  - {error}")
        return 1
    print("\nOK: 30 held-out queries, their routing annotations and the alias files are consistent.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

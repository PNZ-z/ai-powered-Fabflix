#!/usr/bin/env python3
"""Validate the B1.5b fixtures: tool schema, tool cases and query routing annotations.

Stdlib only (user-approved D-4). Four independent checks:

1. ``docs/ai/tool-schema.b1-5.json`` — the single machine-readable contract: five tools,
   ``additionalProperties: false``, ``required`` vs object-level ``anyOf`` per tool, numeric
   ``minimum``/``maximum``, a derived genre ``enum``, and a ``schema_hash`` recomputed from
   ``tools[]`` (``docs/ai/README.md`` §2.3).
2. ``docs/ai/golden/tool-cases.b1-5.json`` — 15 cases (6 refusal / 4 clarification / 5
   multi_slot), validated against ``docs/ai/golden-set.schema.json`` (``$defs/toolCaseSet``),
   with every ``acceptable_movie_ids`` entry present in the catalog.
3. ``docs/ai/golden/query-routing.b1-5.json`` — routing expectations for the 60 queries frozen
   by B1.5a, keyed by ``query_id``. The file records the SHA-256 of
   ``docs/ai/golden/queries.b1-5.json`` and this checker asserts it: B1.5b must not change the
   frozen golden set, otherwise the A/B comparison stops being pairable.
4. The genre ``enum`` is compared with ``SELECT DISTINCT name FROM genres`` so a hand-edited
   enum cannot drift from the database (skipped with ``--skip-db``).

The script is read-only: it issues ``SELECT`` statements through the ``mysql`` CLI.

Usage
-----
    python3 scripts/eval/check_schema.py              # every check, including the database
    python3 scripts/eval/check_schema.py --skip-db    # file-level checks only
    python3 scripts/eval/check_schema.py --quiet      # summary only

Exit code 0 = every check passed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from check_golden import REPO_ROOT, ReadOnlyMysql, load_json, validate  # noqa: E402

TOOL_SCHEMA_PATH = REPO_ROOT / "docs" / "ai" / "tool-schema.b1-5.json"
GOLDEN_SCHEMA_PATH = REPO_ROOT / "docs" / "ai" / "golden-set.schema.json"
QUERIES_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "queries.b1-5.json"
TOOL_CASES_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "tool-cases.b1-5.json"
ROUTING_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "query-routing.b1-5.json"

TOOL_NAMES = (
    "search_movies_by_title",
    "filter_movies",
    "find_similar_movies",
    "recommend_movies",
    "get_movie_card",
)
# "at least one condition" is expressed with anyOf for these two; they must NOT carry `required`
ANY_OF_TOOLS = {
    "filter_movies": ("genres", "year_from", "year_to", "min_rating"),
    "recommend_movies": (
        "preferred_genres", "year_range", "min_rating", "director", "seed_movie_ids",
    ),
}
REQUIRED_SLOTS = {
    "search_movies_by_title": ("query",),
    "find_similar_movies": ("movie_id",),
    "get_movie_card": ("movie_id",),
}
LIMIT_MAX = {
    "search_movies_by_title": 10,
    "filter_movies": 20,
    "find_similar_movies": 20,
    "recommend_movies": 20,
}
ERROR_STATES = ("INVALID_ARGUMENT", "OUT_OF_RANGE", "EMPTY_RESULT")
YEAR_MIN = 196
YEAR_MAX = 2024
RATING_COVERAGE_FROM = 2001
EXPECTED_GENRE_COUNT = 56

CATEGORY_COUNTS = {"refusal": 6, "clarification": 4, "multi_slot": 5}
TOOL_CASE_IDS = [f"T{i:02d}" for i in range(1, 16)]
QUERY_IDS = [f"Q{i:02d}" for i in range(1, 61)]

# Routing vocabularies (documented in the routing file itself)
# slot -> tools that declare it in the fixture (min_rating exists on both filtering tools)
ARG_SLOTS = {
    "query": ("search_movies_by_title",),
    "genres": ("filter_movies",),
    "year_from": ("filter_movies",),
    "year_to": ("filter_movies",),
    "min_rating": ("filter_movies", "recommend_movies"),
    "preferred_genres": ("recommend_movies",),
    "year_range": ("recommend_movies",),
    "director": ("recommend_movies",),
}
ROUTING_ONLY_SLOTS = {"seed_title": ("find_similar_movies",)}
RESULT_CONSTRAINTS = ("genres", "year_from", "year_to", "min_rating", "director")
INTENT_TOOLS = {
    "search_by_title": ["search_movies_by_title"],
    "filter_catalog": ["filter_movies"],
    "similar_movies": ["search_movies_by_title", "find_similar_movies"],
    "recommend_by_preference": ["recommend_movies"],
    "explain_recommendation": ["get_movie_card"],
    "clarify_or_refuse": [],
}


def schema_hash(tools: list) -> str:
    """``docs/ai/README.md`` §2.3: sha256 over the canonical JSON of ``tools[]``."""
    canonical = json.dumps(tools, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _is_int(value: object) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _is_number(value: object) -> bool:
    return _is_int(value) or isinstance(value, float)


def _tools_by_name(schema_doc: dict) -> dict[str, dict]:
    return {tool["function"]["name"]: tool["function"] for tool in schema_doc["tools"]}


def genre_enum(schema_doc: dict) -> list[str]:
    params = _tools_by_name(schema_doc)["filter_movies"]["parameters"]
    return params["properties"]["genres"]["items"]["enum"]


# --------------------------------------------------------------------------------------
# 1. tool schema fixture
# --------------------------------------------------------------------------------------

def check_tool_schema(schema_doc: dict, mysql: ReadOnlyMysql | None) -> list[str]:
    errors: list[str] = []
    contract_version = schema_doc.get("contract_version", "")
    if not contract_version.startswith("b1.5-tools/v"):
        errors.append(f"contract_version {contract_version!r} is not a b1.5-tools/vN value")

    tools = schema_doc.get("tools")
    if not isinstance(tools, list) or len(tools) != len(TOOL_NAMES):
        errors.append(f"expected {len(TOOL_NAMES)} tools, got {len(tools or [])}")
        return errors
    names = tuple(tool["function"]["name"] for tool in tools)
    if sorted(names) != sorted(TOOL_NAMES):
        errors.append(f"tool names {names} do not match {TOOL_NAMES}")

    computed_hash = schema_hash(tools)
    if schema_doc.get("schema_hash") != computed_hash:
        errors.append(
            f"schema_hash mismatch: recorded {schema_doc.get('schema_hash')}, computed {computed_hash}"
        )

    for name, function in _tools_by_name(schema_doc).items():
        params = function["parameters"]
        if params.get("type") != "object":
            errors.append(f"{name}: parameters.type must be 'object'")
        if params.get("additionalProperties") is not False:
            errors.append(f"{name}: additionalProperties must be false")
        required = params.get("required", [])
        properties = params.get("properties", {})
        if name in ANY_OF_TOOLS:
            if required:
                errors.append(f"{name}: must not use `required` (its 'at least one' is anyOf)")
            branches = params.get("anyOf")
            if not branches:
                errors.append(f"{name}: missing object-level anyOf")
            else:
                branch_keys = tuple(sorted(key for branch in branches for key in branch["required"]))
                if branch_keys != tuple(sorted(ANY_OF_TOOLS[name])):
                    errors.append(
                        f"{name}: anyOf requires {branch_keys}, expected {sorted(ANY_OF_TOOLS[name])}"
                    )
                for branch in branches:
                    for key in branch["required"]:
                        if key not in properties:
                            errors.append(f"{name}: anyOf requires undeclared property {key!r}")
        else:
            if tuple(required) != REQUIRED_SLOTS.get(name, required):
                errors.append(f"{name}: required {required} != expected {REQUIRED_SLOTS.get(name)}")
            if "anyOf" in params:
                errors.append(f"{name}: unexpected anyOf (only {sorted(ANY_OF_TOOLS)} use it)")
        for key, spec in properties.items():
            if not isinstance(spec, dict) or "type" not in spec:
                errors.append(f"{name}.{key}: property without a type")

    by_name = _tools_by_name(schema_doc)
    for name in ("filter_movies", "recommend_movies"):
        for key in ("year_from", "year_to"):
            spec = by_name[name]["parameters"]["properties"].get(key)
            if spec is None:
                continue
            if (spec.get("minimum"), spec.get("maximum")) != (YEAR_MIN, YEAR_MAX):
                errors.append(f"{name}.{key}: bounds {(spec.get('minimum'), spec.get('maximum'))}"
                              f" != ({YEAR_MIN}, {YEAR_MAX})")
    year_range = by_name["recommend_movies"]["parameters"]["properties"]["year_range"]
    for key, spec in year_range["properties"].items():
        if (spec.get("minimum"), spec.get("maximum")) != (YEAR_MIN, YEAR_MAX):
            errors.append(f"recommend_movies.year_range.{key}: bounds are not {YEAR_MIN}-{YEAR_MAX}")
    for name in ("filter_movies", "recommend_movies"):
        spec = by_name[name]["parameters"]["properties"]["min_rating"]
        if (spec.get("minimum"), spec.get("maximum")) != (0, 10):
            errors.append(f"{name}.min_rating: bounds are not 0-10")
    for name, maximum in LIMIT_MAX.items():
        spec = by_name[name]["parameters"]["properties"].get("limit")
        if spec is None:
            errors.append(f"{name}: missing limit")
            continue
        if spec.get("maximum") != maximum or spec.get("minimum") != 1:
            errors.append(f"{name}.limit: bounds {(spec.get('minimum'), spec.get('maximum'))}"
                          f" != (1, {maximum})")

    enum = genre_enum(schema_doc)
    if len(enum) != EXPECTED_GENRE_COUNT:
        errors.append(f"genre enum has {len(enum)} values, expected {EXPECTED_GENRE_COUNT}")
    if len(set(enum)) != len(enum):
        errors.append("genre enum contains duplicates")
    if enum != sorted(enum, key=str.casefold):
        errors.append("genre enum is not sorted (case-insensitive, matching the DB ORDER BY)")
    recommend_enum = by_name["recommend_movies"]["parameters"]["properties"]["preferred_genres"]["items"]["enum"]
    if recommend_enum != enum:
        errors.append("preferred_genres enum differs from filter_movies genres enum")

    error_contract = schema_doc.get("error_contract", {})
    for state in ERROR_STATES:
        if state not in error_contract:
            errors.append(f"error_contract is missing {state}")

    snapshot = schema_doc.get("db_snapshot", {})
    if (snapshot.get("year_min"), snapshot.get("year_max")) != (YEAR_MIN, YEAR_MAX):
        errors.append("db_snapshot year bounds do not match the year contract")
    if snapshot.get("ratings_covered_from_year") != RATING_COVERAGE_FROM:
        errors.append(f"db_snapshot.ratings_covered_from_year != {RATING_COVERAGE_FROM}")
    if snapshot.get("genre_distinct_names") != len(enum):
        errors.append("db_snapshot.genre_distinct_names disagrees with the enum length")

    if mysql is not None:
        live = [row[0] for row in mysql.select("SELECT DISTINCT name FROM genres ORDER BY name")]
        if live != enum:
            missing = sorted(set(live) - set(enum))
            extra = sorted(set(enum) - set(live))
            errors.append(
                f"genre enum differs from the database (missing {missing}, not in DB {extra})"
            )
        if len(live) != EXPECTED_GENRE_COUNT:
            errors.append(f"database has {len(live)} distinct genre names, expected {EXPECTED_GENRE_COUNT}")
    return errors


# --------------------------------------------------------------------------------------
# 2. tool cases
# --------------------------------------------------------------------------------------

def check_tool_cases(cases_doc: dict, schema_doc: dict, golden_schema: dict,
                     queries_doc: dict, mysql: ReadOnlyMysql | None) -> list[str]:
    errors: list[str] = []
    for violation in validate(cases_doc, golden_schema, golden_schema, "$"):
        errors.append(f"golden-set.schema.json: {violation}")

    if cases_doc.get("snapshot") != queries_doc.get("snapshot"):
        errors.append("tool-case snapshot block differs from the one in queries.b1-5.json")
    if not cases_doc.get("golden_version", "").startswith("b1-5-tool-cases/v"):
        errors.append(f"golden_version {cases_doc.get('golden_version')!r} is not b1-5-tool-cases/vN")

    cases = cases_doc.get("tool_cases", [])
    ids = [case.get("id") for case in cases]
    if ids != TOOL_CASE_IDS:
        errors.append(f"tool case ids {ids} != {TOOL_CASE_IDS}")

    counts = {category: 0 for category in CATEGORY_COUNTS}
    enum = genre_enum(schema_doc)
    all_ids: list[str] = []
    for case in cases:
        cid = case.get("id")
        category = case.get("category")
        counts[category] = counts.get(category, 0) + 1
        behavior = case.get("expected_behavior")
        tools = case.get("expected_tools", [])
        error_code = case.get("expected_error_code")
        if error_code is None:
            errors.append(f"{cid}: expected_error_code is required by the B1.5b annotation")
        elif error_code in ERROR_STATES and category != "clarification":
            errors.append(f"{cid}: {error_code} only belongs to the clarification category")
        for tool in tools:
            if tool not in TOOL_NAMES:
                errors.append(f"{cid}: unknown tool {tool!r}")
        if category == "refusal":
            if behavior != "refuse_no_tool":
                errors.append(f"{cid}: refusal case must use refuse_no_tool, got {behavior!r}")
            if tools:
                errors.append(f"{cid}: refusal case must not expect tools, got {tools}")
            if error_code != "NONE":
                errors.append(f"{cid}: refusal case must use error code NONE, got {error_code!r}")
        elif category == "clarification":
            if behavior not in ("ask_one_clarifying_question", "explain_coverage_limit"):
                errors.append(f"{cid}: clarification case has behavior {behavior!r}")
            if behavior == "explain_coverage_limit" and error_code not in ("EMPTY_RESULT", "OUT_OF_RANGE"):
                errors.append(f"{cid}: explain_coverage_limit needs EMPTY_RESULT or OUT_OF_RANGE")
            if behavior == "explain_coverage_limit" and error_code == "EMPTY_RESULT" and not tools:
                errors.append(f"{cid}: EMPTY_RESULT coverage can only come from a tool result")
            if error_code == "OUT_OF_RANGE" and tools:
                errors.append(
                    f"{cid}: an out-of-coverage year cannot be expressed in a schema-valid tool "
                    f"call (year bounds stop at {YEAR_MAX}); expect no tools"
                )
        elif category == "multi_slot":
            if behavior != "call_tools_and_answer":
                errors.append(f"{cid}: multi_slot case must use call_tools_and_answer, got {behavior!r}")
            if not tools:
                errors.append(f"{cid}: multi_slot case must expect at least one tool")
            if not case.get("acceptable_movie_ids"):
                errors.append(f"{cid}: multi_slot case needs acceptable_movie_ids")
            if error_code != "NONE":
                errors.append(f"{cid}: multi_slot case must use error code NONE, got {error_code!r}")
        else:
            errors.append(f"{cid}: unknown category {category!r}")
        for movie_id in case.get("acceptable_movie_ids", []):
            if not (1 <= len(movie_id) <= 10):
                errors.append(f"{cid}: movie id {movie_id!r} breaks the VARCHAR(10) contract")
            all_ids.append(movie_id)
        genres = case.get("result_genres") or []
        for genre in genres:
            if genre not in enum:
                errors.append(f"{cid}: genre {genre!r} is not in the derived enum")

    for category, expected in CATEGORY_COUNTS.items():
        if counts.get(category) != expected:
            errors.append(f"category {category}: {counts.get(category)} cases, expected {expected}")

    if mysql is not None and all_ids:
        unique_ids = sorted(set(all_ids))
        quoted = ",".join(f"'{movie_id}'" for movie_id in unique_ids)
        present = {row[0] for row in mysql.select(f"SELECT id FROM movies WHERE id IN ({quoted})")}
        for movie_id in unique_ids:
            if movie_id not in present:
                errors.append(f"acceptable_movie_ids: {movie_id} does not exist in the catalog")
    return errors


# --------------------------------------------------------------------------------------
# 3. query routing annotations
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


def check_routing(routing_doc: dict, queries_doc: dict, schema_doc: dict) -> list[str]:
    errors: list[str] = []
    root_keys = {
        "$comment", "routing_version", "annotates", "schema_contract",
        "slot_vocabulary", "result_constraint_vocabulary", "annotations",
    }
    for key in routing_doc:
        if key not in root_keys:
            errors.append(f"routing: unexpected root key {key!r}")
    if not routing_doc.get("routing_version", "").startswith("b1-5-routing/v"):
        errors.append(f"routing_version {routing_doc.get('routing_version')!r} is not b1-5-routing/vN")

    annotates = routing_doc.get("annotates", {})
    if annotates.get("sha256") != sha256_of(QUERIES_PATH):
        errors.append(
            f"annotates.sha256 {annotates.get('sha256')} != current queries file "
            f"{sha256_of(QUERIES_PATH)} (B1.5a's frozen golden set must not change)"
        )
    if annotates.get("golden_version") != queries_doc.get("golden_version"):
        errors.append("annotates.golden_version differs from queries.b1-5.json")
    contract = routing_doc.get("schema_contract", {})
    if contract.get("schema_hash") != schema_doc.get("schema_hash"):
        errors.append("schema_contract.schema_hash differs from tool-schema.b1-5.json")
    if contract.get("contract_version") != schema_doc.get("contract_version"):
        errors.append("schema_contract.contract_version differs from tool-schema.b1-5.json")

    vocabulary = routing_doc.get("slot_vocabulary", {})
    documented = set(vocabulary)
    expected_vocabulary = set(ARG_SLOTS) | set(ROUTING_ONLY_SLOTS)
    if documented != expected_vocabulary:
        errors.append(f"slot_vocabulary {sorted(documented)} != {sorted(expected_vocabulary)}")

    enum = genre_enum(schema_doc)
    queries = queries_doc.get("queries", [])
    annotations = routing_doc.get("annotations", [])
    ids = [entry.get("query_id") for entry in annotations]
    if ids != [query["id"] for query in queries]:
        errors.append("annotation ids do not match the 60 query ids in order")

    by_id = {query["id"]: query for query in queries}
    for entry in annotations:
        qid = entry.get("query_id")
        query = by_id.get(qid)
        if query is None:
            errors.append(f"{qid}: no such query in queries.b1-5.json")
            continue
        allowed_keys = {
            "query_id", "intent", "expected_refusal", "golden_predicate", "expected_tools",
            "required_slots", "accept_alternative_slots", "result_constraints", "notes",
        }
        for key in entry:
            if key not in allowed_keys:
                errors.append(f"{qid}: unexpected key {key!r}")
        if entry.get("intent") != query["intent"]:
            errors.append(f"{qid}: intent {entry.get('intent')!r} != golden intent {query['intent']!r}")
        if entry.get("golden_predicate") != query["snapshot_row_refs"][0]:
            errors.append(f"{qid}: golden_predicate differs from the golden set's first row ref")
        refusal = entry.get("expected_refusal")
        if not isinstance(refusal, bool):
            errors.append(f"{qid}: expected_refusal must be a boolean")
        elif refusal != (query["intent"] == "clarify_or_refuse"):
            errors.append(f"{qid}: expected_refusal must mirror clarifying/refusing intents only")

        tools = entry.get("expected_tools", [])
        if len(set(tools)) != len(tools):
            errors.append(f"{qid}: expected_tools contains duplicates")
        for tool in tools:
            if tool not in TOOL_NAMES:
                errors.append(f"{qid}: unknown expected tool {tool!r}")
        expected_for_intent = INTENT_TOOLS.get(query["intent"], [])
        if sorted(tools) != sorted(expected_for_intent):
            errors.append(
                f"{qid}: expected_tools {tools} != routing rule for intent "
                f"{query['intent']} {expected_for_intent}"
            )
        if refusal and tools:
            errors.append(f"{qid}: a refusing query must not expect tools")
        if not refusal and not tools:
            errors.append(f"{qid}: a non-refusing query must expect at least one tool")

        slots = entry.get("required_slots", {})
        if not isinstance(slots, dict) or not slots:
            errors.append(f"{qid}: required_slots must be a non-empty object")
            slots = {}
        errors.extend(check_slot_values(qid, "required_slots", slots, enum))
        for key in slots:
            owners = ARG_SLOTS.get(key) or ROUTING_ONLY_SLOTS.get(key) or ()
            if not any(owner in tools for owner in owners):
                errors.append(
                    f"{qid}: slot {key!r} belongs to {list(owners)}, which is not in expected_tools"
                )
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
            extra = set(alternative) - set(slots)
            dropped = set(slots) - set(alternative)
            if extra or dropped - {"year_to"}:
                errors.append(
                    f"{qid}: accept_alternative_slots[{index}] keys {sorted(alternative)} must be a "
                    f"subset of required_slots keys {sorted(slots)} and may only drop 'year_to'"
                )
            errors.extend(check_slot_values(qid, f"accept_alternative_slots[{index}]", alternative, enum))

        constraints = entry.get("result_constraints", {})
        if constraints is not None and not isinstance(constraints, dict):
            errors.append(f"{qid}: result_constraints must be an object")
            constraints = {}
        for key in constraints or {}:
            if key not in RESULT_CONSTRAINTS:
                errors.append(f"{qid}: unknown result constraint {key!r}")
        if constraints:
            errors.extend(check_slot_values(qid, "result_constraints", constraints, enum))
            if "filter_movies" in tools or "recommend_movies" in tools:
                errors.append(
                    f"{qid}: result_constraints are only for tools that cannot take the condition "
                    f"as an argument (filter_movies/recommend_movies can)"
                )

        if not isinstance(entry.get("notes"), str) or len(entry["notes"]) < 10:
            errors.append(f"{qid}: notes must explain the expectation (>=10 characters)")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--skip-db", action="store_true",
                        help="file-level checks only (no mysql, no id existence check)")
    parser.add_argument("--quiet", action="store_true", help="summary only")
    args = parser.parse_args()

    schema_doc = load_json(TOOL_SCHEMA_PATH)
    golden_schema = load_json(GOLDEN_SCHEMA_PATH)
    queries_doc = load_json(QUERIES_PATH)
    cases_doc = load_json(TOOL_CASES_PATH)
    routing_doc = load_json(ROUTING_PATH)

    mysql = None if args.skip_db else ReadOnlyMysql()

    errors: list[str] = []
    errors.extend(check_tool_schema(schema_doc, mysql))
    errors.extend(check_tool_cases(cases_doc, schema_doc, golden_schema, queries_doc, mysql))
    errors.extend(check_routing(routing_doc, queries_doc, schema_doc))

    print(f"contract_version: {schema_doc.get('contract_version')}")
    print(f"schema_hash:      {schema_doc.get('schema_hash')} (recomputed "
          f"{schema_hash(schema_doc.get('tools', []))})")
    print(f"genre enum:       {len(genre_enum(schema_doc))} values")
    categories = {}
    for case in cases_doc.get("tool_cases", []):
        categories[case["category"]] = categories.get(case["category"], 0) + 1
    print(f"tool cases:       {len(cases_doc.get('tool_cases', []))} {categories}")
    print(f"routing entries:  {len(routing_doc.get('annotations', []))} (golden sha256 "
          f"{routing_doc.get('annotates', {}).get('sha256')})")
    print(f"sha256 {TOOL_CASES_PATH.relative_to(REPO_ROOT)}: {sha256_of(TOOL_CASES_PATH)}")
    print(f"sha256 {ROUTING_PATH.relative_to(REPO_ROOT)}: {sha256_of(ROUTING_PATH)}")

    if errors:
        print(f"\nFAILED ({len(errors)} problems):")
        for error in errors:
            print(f"  - {error}")
        return 1
    print("\nOK: tool schema, tool cases, routing annotations and the golden-set hash are valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

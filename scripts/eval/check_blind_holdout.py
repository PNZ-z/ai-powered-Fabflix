#!/usr/bin/env python3
"""Validate the B1.5d blind holdout (30 queries ``B01``-``B30`` and their routing annotations).

Stdlib only, read-only. This script is the *only* new file in ``scripts/eval/`` for B1.5d and it
deliberately sits outside the scoring path: it imports no scoring code (``tool_eval`` is never
imported), ``tool_eval.py`` never calls it, and it writes no file. It reads the two blind fixtures,
the frozen context they must stay compatible with (the 105 earlier cases, the alias files, the
tool contract, the ``H*``-shaped golden schema is *not* used here) and the catalog snapshot.

Twelve independent assertion groups
-----------------------------------
1. **structure** — both files exist and parse, no unknown top-level key, the version/created/
   snapshot/quota blocks are present, ``snapshot`` equals the frozen ``queries.b1-5.json`` snapshot,
   and every case carries exactly the documented field set with the documented types.
2. **ids** — exactly ``B01``-``B30`` in order, no duplicate, and the routing ``query_id`` list is
   the same list in the same order.
3. **tracks and text** — 10/10/10 across ``zh_zh``/``zh_en``/``en_en``, ``lang`` consistent with the
   track, no duplicate query text, and no query text identical to an existing case once punctuation
   and case are normalised (the 105 earlier cases).
4. **intent quota** — search 5 / filter 9 / similar 7 / recommend 3 / clarify-or-refuse 6, and the
   routing intent mirrors the query intent case by case.
5. **cross marks** — ``multi_slot`` is exactly 4 and only on ``filter_catalog``/``similar_movies``;
   ``cjk_title`` is exactly 2, only on an intent whose annotation has a title slot (``query`` or
   ``seed_title``), never on a refusal, and the two Chinese titles are two new ones (not the five
   already used) that carry CJK characters; ``known_gap`` records ``R-1`` and ``R-2`` exactly once.
   No other case may carry a Chinese title in ``《…》`` — the cold-run model-call budget of 2 depends
   on it.
6. **refusal and clarification shapes** — 4 refusals (``expects_no_match``, empty acceptable set, no
   expected tool, empty ``required_slots``) and 2 clarifications (``allows_clarification``, non-empty
   acceptable set, a title slot, at least one expected tool).
7. **exclusion** — no acceptable id appears in the 105 earlier cases (per-id disjointness, the
   target reading of plan §3.4.3, with no opt-out flag), no acceptable set equals an earlier one, no
   seed title is an earlier seed/title, no catalog row of a seed title is used by an earlier case,
   and no ``database/dev-seed.sql`` synthetic row appears.
8. **decades** — no 2020s row, 3-5 cases entirely in the 1990s, at most 1 in the 2010s; a 1990s case
   may not constrain ``min_rating`` (ratings only cover 2001+) unless it is an explicitly declared
   empty-result boundary case, and every ``min_rating`` case really has 2001+ rows.
9. **aliases** — ``title-aliases.json`` is still frozen empty (decision C10) and neither Chinese
   title of this batch is pre-seeded there.
10. **bindings and hashes** — ``annotates.sha256`` binds *this* blind queries file, the contract
    block records ``b1.5-tools/v3`` with its schema hash, the snapshot records the frozen
    ``db_snapshot_hash``, both fixtures match the SHA-256 frozen before the first run, and the slot
    vocabularies use the shared vocabulary. The checker cannot report its own SHA-256 (that is
    self-referential); it is frozen externally in the plan appendix and the pre-run freeze record.
11. **catalog referential** — every acceptable id exists, and the recorded ``rows=`` count equals
    the number of ids listed. Needs the catalog; ``--skip-db`` skips this group together with the
    year/decade and person groups.
12. **read-only self-attestation** — the MySQL helper refuses anything that is not a single SELECT,
    and this script opens no file for writing.

Usage
-----
    python3 scripts/eval/check_blind_holdout.py              # every check, including the database
    python3 scripts/eval/check_blind_holdout.py --skip-db    # file-level checks only
    python3 scripts/eval/check_blind_holdout.py --quiet      # summary only

Exit code 0 = every check passed.
"""

from __future__ import annotations

import argparse
import ast
import hashlib
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from check_golden import REPO_ROOT, ReadOnlyMysql, load_json  # noqa: E402
from check_schema import (  # noqa: E402  (frozen artefacts + shared vocabularies)
    ARG_SLOTS,
    RATING_COVERAGE_FROM,
    RESULT_CONSTRAINTS,
    ROUTING_ONLY_SLOTS,
    TOOL_NAMES,
    TOOL_SCHEMA_PATH,
    YEAR_MAX,
    YEAR_MIN,
    genre_enum,
)

BLIND_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "queries-blind.b1-5d.json"
BLIND_ROUTING_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "query-routing-blind.b1-5d.json"
FROZEN_QUERIES_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "queries.b1-5.json"
FROZEN_TOOL_CASES_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "tool-cases.b1-5.json"
FROZEN_ROUTING_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "query-routing.b1-5.json"
HELDOUT_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "queries-heldout.b1-5c.json"
HELDOUT_ROUTING_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "query-routing-heldout.b1-5c.json"
TITLE_ALIASES_PATH = REPO_ROOT / "docs" / "ai" / "aliases" / "title-aliases.json"

# Frozen before the first measurement run (plan §4.3); the checker itself is frozen externally.
FROZEN_BLIND_SHA256 = "21522516b57c8d514b233b45a71329c6e7cef335371ce7805e121058de41c694"
FROZEN_BLIND_ROUTING_SHA256 = "556761b415c2f947e1aa96df75636a78d22f45b411a89dfdeaa96e82a0d43dbe"
BLIND_VERSION = "b1-5d-blind/v1"
ROUTING_VERSION = "b1-5d-blind-routing/v1"
DB_SNAPSHOT_HASH = "8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925"
EXPECTED_CONTRACT_VERSION = "b1.5-tools/v3"
EXPECTED_SCHEMA_HASH = "712c0e34c37b6de140c13fcc7d3de15e67ac1f131f2bb6e8751a4f5e4df3474a"

EXPECTED_IDS = [f"B{i:02d}" for i in range(1, 31)]
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
DECADE_2010S_MAX = 1
MULTI_SLOT_INTENTS = ("filter_catalog", "similar_movies")
CJK_INTENTS = ("search_by_title", "similar_movies", "recommend_by_preference")
TITLE_SLOTS = ("query", "seed_title")
KNOWN_GAPS = ("R-1", "R-2")
# The five Chinese titles the earlier batches already used; the blind batch must not repeat them.
USED_CJK_TITLES = {"肖申克的救赎", "超人", "黑客帝国", "阿甘正传", "泰坦尼克号"}

# database/dev-seed.sql synthetic rows and the Movie1/Movie2 leftovers
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
CASE_REQUIRED_KEYS = {
    "id", "track", "query", "lang", "intent", "acceptable_movie_ids", "labeling_note",
    "snapshot_row_refs",
}
CASE_OPTIONAL_KEYS = {
    "expects_no_match", "cjk_title", "allows_clarification", "multi_slot", "known_gap",
}
QUERY_ROOT_KEYS = {"$comment", "blind_version", "created", "snapshot", "quota", "queries"}
ROUTING_ROOT_KEYS = {
    "$comment", "routing_version", "annotates", "schema_contract", "slot_vocabulary",
    "result_constraint_vocabulary", "annotations",
}
ANNOTATION_KEYS = {
    "query_id", "intent", "expected_refusal", "golden_predicate", "expected_tools",
    "required_slots", "accept_alternative_slots", "result_constraints", "allows_clarification",
    "notes",
}
CJK_TITLE_PATTERN = re.compile(r"《([^》]*)》")
ROW_REF_PATTERN = re.compile(r"rows=(\d+) \| ids=(.*)$")
PUNCTUATION_PATTERN = re.compile(r"[\W_]+", re.UNICODE)


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def normalise_text(text: str) -> str:
    """Lowercase and drop punctuation/whitespace so 'identical modulo punctuation' is detectable."""
    return PUNCTUATION_PATTERN.sub("", text.casefold())


def has_cjk(text: str) -> bool:
    return any("\u4e00" <= char <= "\u9fff" for char in text)


def _is_int(value: object) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _is_number(value: object) -> bool:
    return _is_int(value) or isinstance(value, float)


def cjk_titles_of(case: dict) -> list[str]:
    return [title for title in CJK_TITLE_PATTERN.findall(case["query"]) if has_cjk(title)]


# --------------------------------------------------------------------------------------
# 1. structure
# --------------------------------------------------------------------------------------

def check_structure(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    for key in queries_doc:
        if key not in QUERY_ROOT_KEYS:
            errors.append(f"queries: unexpected top-level key {key!r}")
    for key in ROUTING_ROOT_KEYS:
        if key not in routing_doc:
            errors.append(f"routing: missing top-level key {key!r}")
    for key in routing_doc:
        if key not in ROUTING_ROOT_KEYS:
            errors.append(f"routing: unexpected top-level key {key!r}")

    if queries_doc.get("blind_version") != BLIND_VERSION:
        errors.append(f"blind_version {queries_doc.get('blind_version')!r} != {BLIND_VERSION!r}")
    if routing_doc.get("routing_version") != ROUTING_VERSION:
        errors.append(f"routing_version {routing_doc.get('routing_version')!r} != {ROUTING_VERSION!r}")
    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", str(queries_doc.get("created", ""))):
        errors.append(f"created {queries_doc.get('created')!r} is not an ISO date")

    frozen_snapshot = load_json(FROZEN_QUERIES_PATH).get("snapshot")
    snapshot = queries_doc.get("snapshot")
    if snapshot != frozen_snapshot:
        errors.append("snapshot block differs from queries.b1-5.json (the catalog snapshot is shared)")
    if (snapshot or {}).get("db_snapshot_hash") != DB_SNAPSHOT_HASH:
        errors.append(
            f"snapshot.db_snapshot_hash {(snapshot or {}).get('db_snapshot_hash')!r} != "
            f"{DB_SNAPSHOT_HASH}"
        )

    quota = queries_doc.get("quota")
    if not isinstance(quota, dict):
        errors.append("quota must be an object")
    else:
        for key in ("tracks", "intents", "clarify_or_refuse_split", "multi_slot", "cjk_title"):
            if key not in quota:
                errors.append(f"quota is missing {key!r}")

    cases = queries_doc.get("queries")
    if not isinstance(cases, list) or len(cases) != 30:
        errors.append(f"queries must be a list of 30 cases, found {len(cases or [])}")
        return errors

    for index, case in enumerate(cases):
        label = case.get("id") or f"queries[{index}]"
        if not isinstance(case, dict):
            errors.append(f"{label}: a case must be an object")
            continue
        missing = CASE_REQUIRED_KEYS - set(case)
        extra = set(case) - CASE_REQUIRED_KEYS - CASE_OPTIONAL_KEYS
        if missing:
            errors.append(f"{label}: missing required field(s) {sorted(missing)}")
        if extra:
            errors.append(f"{label}: unknown field(s) {sorted(extra)}")
        query = case.get("query")
        if not isinstance(query, str) or not 2 <= len(query) <= 200:
            errors.append(f"{label}: query must be a string of 2-200 characters")
        note = case.get("labeling_note")
        if not isinstance(note, str) or len(note) < 10:
            errors.append(f"{label}: labeling_note must explain the expectation (>=10 characters)")
        refs = case.get("snapshot_row_refs")
        if not isinstance(refs, list) or not refs or not all(
            isinstance(ref, str) and len(ref) >= 5 for ref in refs
        ):
            errors.append(f"{label}: snapshot_row_refs must be a non-empty list of strings")
        ids = case.get("acceptable_movie_ids")
        if not isinstance(ids, list) or len(ids) > 25:
            errors.append(f"{label}: acceptable_movie_ids must be a list of at most 25 ids")
        elif len(set(ids)) != len(ids):
            errors.append(f"{label}: acceptable_movie_ids contains duplicates")
        elif not all(isinstance(movie_id, str) and 1 <= len(movie_id) <= 10 for movie_id in ids):
            errors.append(f"{label}: acceptable_movie_ids holds a non-string or oversized id")
        for flag in ("expects_no_match", "cjk_title", "allows_clarification", "multi_slot"):
            if flag in case and not isinstance(case[flag], bool):
                errors.append(f"{label}: {flag} must be a boolean")
        if "known_gap" in case and case["known_gap"] not in KNOWN_GAPS:
            errors.append(f"{label}: known_gap {case['known_gap']!r} is not one of {list(KNOWN_GAPS)}")
    return errors


# --------------------------------------------------------------------------------------
# 2. ids
# --------------------------------------------------------------------------------------

def check_ids(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    ids = [case.get("id") for case in queries_doc.get("queries", [])]
    if ids != EXPECTED_IDS:
        errors.append(f"ids {ids} != B01..B30 (order matters: the run record is positional)")
    if len(set(ids)) != len(ids):
        errors.append("ids are not unique")
    annotation_ids = [entry.get("query_id") for entry in routing_doc.get("annotations", [])]
    if annotation_ids != ids:
        errors.append(f"routing query_ids {annotation_ids} != query ids {ids}")
    if len(set(annotation_ids)) != len(annotation_ids):
        errors.append("routing query_ids are not unique")
    return errors


# --------------------------------------------------------------------------------------
# 3. tracks, lang and text
# --------------------------------------------------------------------------------------

def check_tracks(queries_doc: dict) -> list[str]:
    errors: list[str] = []
    per_track = {track: 0 for track in TRACKS}
    texts: dict[str, str] = {}
    normalised: dict[str, str] = {}
    for case in queries_doc.get("queries", []):
        cid = case.get("id")
        track = case.get("track")
        if track not in TRACK_LANG:
            errors.append(f"{cid}: unknown track {track!r}")
            continue
        per_track[track] += 1
        if case.get("lang") != TRACK_LANG[track]:
            errors.append(f"{cid}: lang {case.get('lang')!r} != {TRACK_LANG[track]!r} for {track}")
        text = case.get("query", "")
        if text in texts:
            errors.append(f"{cid}: duplicate query text (already used by {texts[text]})")
        texts[text] = cid
        key = normalise_text(text)
        if key in normalised:
            errors.append(f"{cid}: query text equals {normalised[key]} modulo punctuation")
        normalised[key] = cid
        if track == "en_en" and has_cjk(text):
            errors.append(f"{cid}: an en_en query must not contain CJK characters")
        if track != "en_en" and not has_cjk(text):
            errors.append(f"{cid}: a {track} query should be written in Chinese")
    for track, count in per_track.items():
        if count != EXPECTED_PER_TRACK:
            errors.append(f"track {track}: {count} cases, expected {EXPECTED_PER_TRACK}")
    return errors


# --------------------------------------------------------------------------------------
# 4. intent quota
# --------------------------------------------------------------------------------------

def check_intent_quota(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    counts = {name: 0 for name in INTENT_QUOTA}
    for case in queries_doc.get("queries", []):
        intent = case.get("intent")
        if intent not in INTENT_QUOTA:
            errors.append(f"{case.get('id')}: intent {intent!r} is outside the blind quota")
            continue
        counts[intent] += 1
    for name, expected in INTENT_QUOTA.items():
        if counts[name] != expected:
            errors.append(f"intent {name}: {counts[name]} cases, expected {expected}")

    by_id = {case.get("id"): case for case in queries_doc.get("queries", [])}
    for entry in routing_doc.get("annotations", []):
        qid = entry.get("query_id")
        case = by_id.get(qid)
        if case is None:
            continue
        if entry.get("intent") != case.get("intent"):
            errors.append(f"{qid}: routing intent {entry.get('intent')!r} != query intent "
                          f"{case.get('intent')!r}")
    return errors


# --------------------------------------------------------------------------------------
# 5. cross marks (multi_slot, cjk_title, known_gap)
# --------------------------------------------------------------------------------------

def check_cross_marks(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    annotations = {entry.get("query_id"): entry for entry in routing_doc.get("annotations", [])}
    multi_slot = 0
    cjk = 0
    gaps: dict[str, list[str]] = {gap: [] for gap in KNOWN_GAPS}
    chinese_titles: list[tuple[str, str]] = []

    for case in queries_doc.get("queries", []):
        cid = case.get("id")
        intent = case.get("intent")
        annotation = annotations.get(cid) or {}
        slots = annotation.get("required_slots") or {}

        if case.get("multi_slot"):
            multi_slot += 1
            if intent not in MULTI_SLOT_INTENTS:
                errors.append(f"{cid}: multi_slot belongs to filter_catalog/similar_movies only "
                              f"(intent {intent})")
        if case.get("cjk_title"):
            cjk += 1
            if intent not in CJK_INTENTS:
                errors.append(f"{cid}: cjk_title on an intent without a title slot ({intent})")
            if case.get("expects_no_match"):
                errors.append(f"{cid}: a refusal must not be marked cjk_title")
            if not any(slot in slots for slot in TITLE_SLOTS):
                errors.append(f"{cid}: cjk_title requires a query/seed_title slot in the routing")
            titles = cjk_titles_of(case)
            if len(titles) != 1:
                errors.append(f"{cid}: a cjk_title case needs exactly one 《…》 title, found {titles}")
            else:
                chinese_titles.append((cid, titles[0]))
        if "known_gap" in case:
            gaps[case["known_gap"]].append(cid)

    if multi_slot != EXPECTED_MULTI_SLOT:
        errors.append(f"{multi_slot} multi_slot cases, expected {EXPECTED_MULTI_SLOT}")
    if cjk != EXPECTED_CJK:
        errors.append(f"{cjk} cjk_title cases, expected {EXPECTED_CJK}")
    for gap, owners in gaps.items():
        if len(owners) != 1:
            errors.append(f"known_gap {gap}: {len(owners)} case(s) {owners}, expected exactly 1")
    if gaps["R-1"]:
        case = next(c for c in queries_doc["queries"] if c.get("id") == gaps["R-1"][0])
        if case.get("intent") != "filter_catalog":
            errors.append(f"{case['id']}: the R-1 probe must be a filter_catalog case")
        if "musical" not in case.get("query", "").casefold():
            errors.append(f"{case['id']}: the R-1 probe must contain the term 'musicals'")
        refs = " ".join(case.get("snapshot_row_refs") or [])
        if "Musical" not in refs or "Music" not in refs:
            errors.append(f"{case['id']}: the R-1 probe must record both the Music and the Musical "
                          f"reading in snapshot_row_refs")
    if gaps["R-2"]:
        case = next(c for c in queries_doc["queries"] if c.get("id") == gaps["R-2"][0])
        if case.get("intent") != "similar_movies":
            errors.append(f"{case['id']}: the R-2 probe must be a similar_movies case")

    # every other case must stay off Chinese titles: the cold run may only resolve the 2 cjk cases
    for case in queries_doc.get("queries", []):
        if case.get("cjk_title"):
            continue
        smuggled = cjk_titles_of(case)
        if smuggled:
            errors.append(f"{case['id']}: Chinese title(s) {smuggled} outside the 2 cjk_title cases "
                          f"(it would add a resolver call to the frozen cold-run budget)")
    seen_titles: dict[str, str] = {}
    for cid, title in chinese_titles:
        if title in USED_CJK_TITLES:
            errors.append(f"{cid}: Chinese title {title!r} was already used by an earlier batch")
        if title in seen_titles:
            errors.append(f"{cid}: Chinese title {title!r} also used by {seen_titles[title]}")
        seen_titles[title] = cid
    return errors


# --------------------------------------------------------------------------------------
# 6. refusal and clarification shapes
# --------------------------------------------------------------------------------------

def check_answer_shapes(routing_doc: dict, queries_doc: dict) -> list[str]:
    errors: list[str] = []
    annotations = {entry.get("query_id"): entry for entry in routing_doc.get("annotations", [])}
    refusals = clarifications = 0
    for case in queries_doc.get("queries", []):
        cid = case.get("id")
        annotation = annotations.get(cid) or {}
        tools = annotation.get("expected_tools")
        slots = annotation.get("required_slots")
        if case.get("expects_no_match"):
            refusals += 1
            if case.get("acceptable_movie_ids"):
                errors.append(f"{cid}: a refusal has no acceptable movie ids")
            if case.get("intent") != "clarify_or_refuse":
                errors.append(f"{cid}: expects_no_match belongs to clarify_or_refuse cases")
            if annotation.get("expected_refusal") is not True:
                errors.append(f"{cid}: a refusal needs expected_refusal = true")
            if tools != []:
                errors.append(f"{cid}: a refusal must expect no tool, found {tools}")
            if slots != {}:
                errors.append(f"{cid}: a refusal must have empty required_slots, found {slots}")
            if case.get("allows_clarification"):
                errors.append(f"{cid}: a refusal cannot also allow clarification")
        elif not case.get("acceptable_movie_ids"):
            errors.append(f"{cid}: a non-refusal case needs a non-empty acceptable set "
                          f"(tool_eval refuses to build such a case)")
        if case.get("allows_clarification"):
            clarifications += 1
            if case.get("intent") != "clarify_or_refuse":
                errors.append(f"{cid}: allows_clarification belongs to clarify_or_refuse cases")
            if not case.get("acceptable_movie_ids"):
                errors.append(f"{cid}: a clarification keeps a non-empty acceptable set")
            if annotation.get("allows_clarification") is not True:
                errors.append(f"{cid}: the routing must mirror allows_clarification = true")
            if annotation.get("expected_refusal") is not False:
                errors.append(f"{cid}: a clarification is not a refusal")
            if not tools:
                errors.append(f"{cid}: a clarification needs at least one expected tool")
            if not any(slot in (slots or {}) for slot in TITLE_SLOTS):
                errors.append(f"{cid}: a clarification needs a query/seed_title slot")
    if refusals != EXPECTED_REFUSALS:
        errors.append(f"{refusals} refusal cases, expected {EXPECTED_REFUSALS}")
    if clarifications != EXPECTED_CLARIFICATIONS:
        errors.append(f"{clarifications} clarification cases, expected {EXPECTED_CLARIFICATIONS}")
    return errors


# --------------------------------------------------------------------------------------
# 7. exclusion against the 105 earlier cases
# --------------------------------------------------------------------------------------

def earlier_cases() -> list[dict]:
    cases: list[dict] = []
    for path, key in ((FROZEN_QUERIES_PATH, "queries"), (FROZEN_TOOL_CASES_PATH, "tool_cases"),
                      (HELDOUT_PATH, "queries")):
        cases.extend(load_json(path).get(key, []))
    return cases


def check_exclusion(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    previous = earlier_cases()
    if len(previous) != 105:
        errors.append(f"expected 105 earlier cases, found {len(previous)}")

    previous_texts = {normalise_text(case.get("query", "")): case.get("id") for case in previous}
    previous_id_sets = {frozenset(case.get("acceptable_movie_ids") or []) for case in previous}
    previous_ids = {movie_id for case in previous for movie_id in case.get("acceptable_movie_ids") or []}

    previous_titles: set[str] = set()
    for path in (FROZEN_ROUTING_PATH, HELDOUT_ROUTING_PATH):
        for annotation in load_json(path).get("annotations", []):
            slots = annotation.get("required_slots") or {}
            for key in TITLE_SLOTS:
                if isinstance(slots.get(key), str):
                    previous_titles.add(slots[key].casefold())

    for case in queries_doc.get("queries", []):
        cid = case.get("id")
        text_key = normalise_text(case.get("query", ""))
        if text_key in previous_texts:
            errors.append(f"{cid}: query text repeats {previous_texts[text_key]} "
                          f"(identical modulo punctuation)")
        ids = case.get("acceptable_movie_ids") or []
        overlap = sorted(set(ids) & previous_ids)
        if overlap:
            errors.append(f"{cid}: acceptable ids already used by the 105 earlier cases: {overlap}")
        if ids and frozenset(ids) in previous_id_sets:
            errors.append(f"{cid}: acceptable id set equals an earlier case's set")
        for movie_id in ids:
            if movie_id in SYNTHETIC_IDS:
                errors.append(f"{cid}: synthetic dev-seed id {movie_id} in the acceptable set")

    for entry in routing_doc.get("annotations", []):
        qid = entry.get("query_id")
        seed = (entry.get("required_slots") or {}).get("seed_title")
        if not isinstance(seed, str):
            continue
        if seed.casefold() in previous_titles:
            errors.append(f"{qid}: seed title {seed!r} is already used by an earlier case")
        if seed.casefold() in {t.casefold() for t in SYNTHETIC_TITLES}:
            errors.append(f"{qid}: seed title {seed!r} is a synthetic dev-seed row")
    return errors


# --------------------------------------------------------------------------------------
# 8. decades, min_rating coverage and catalog referential
# --------------------------------------------------------------------------------------

def case_years(case: dict, year_by_id: dict[str, int]) -> list[int]:
    return [year_by_id[movie_id] for movie_id in case.get("acceptable_movie_ids") or []
            if movie_id in year_by_id]


def check_decades(queries_doc: dict, routing_doc: dict, mysql: ReadOnlyMysql | None) -> list[str]:
    errors: list[str] = []
    if mysql is None:
        return errors
    annotations = {entry.get("query_id"): entry for entry in routing_doc.get("annotations", [])}

    all_ids = sorted({movie_id for case in queries_doc["queries"]
                      for movie_id in case.get("acceptable_movie_ids") or []})
    if not all_ids:
        return ["no acceptable ids to check against the catalog"]
    quoted = ",".join(f"'{movie_id}'" for movie_id in all_ids)
    rows = mysql.select(f"SELECT id, title, year FROM movies WHERE id IN ({quoted})")
    year_by_id = {row[0]: int(row[2]) for row in rows}
    title_by_id = {row[0]: row[1] for row in rows}

    missing = [movie_id for movie_id in all_ids if movie_id not in year_by_id]
    if missing:
        errors.append(f"acceptable ids that do not exist in the catalog: {', '.join(missing)}")
    for movie_id, title in title_by_id.items():
        if title in SYNTHETIC_TITLES:
            errors.append(f"{movie_id}: synthetic dev-seed title {title!r} in the acceptable set")

    per_decade: dict[str, int] = {}
    for case in queries_doc["queries"]:
        cid = case["id"]
        years = case_years(case, year_by_id)
        annotation = annotations.get(cid) or {}
        slots = annotation.get("required_slots") or {}
        constraints = annotation.get("result_constraints") or {}
        uses_rating = "min_rating" in slots or "min_rating" in constraints
        if any(year >= 2020 for year in years):
            errors.append(f"{cid}: a 2020s row is not allowed (7 rows, 5 of them synthetic)")
        if years and all(1990 <= year <= 1999 for year in years):
            per_decade["1990s"] = per_decade.get("1990s", 0) + 1
            if uses_rating:
                errors.append(f"{cid}: a 1990s case may not constrain min_rating "
                              f"(ratings only cover {RATING_COVERAGE_FROM}+)")
        if years and all(2010 <= year <= 2019 for year in years):
            per_decade["2010s"] = per_decade.get("2010s", 0) + 1
        if uses_rating and years and not case.get("expects_no_match"):
            early = [movie_id for movie_id in case["acceptable_movie_ids"]
                     if year_by_id.get(movie_id, 0) < RATING_COVERAGE_FROM]
            if early:
                errors.append(f"{cid}: min_rating case contains pre-{RATING_COVERAGE_FROM} rows "
                              f"{early} that cannot have a rating")

    low, high = DECADE_1990S_RANGE
    if not low <= per_decade.get("1990s", 0) <= high:
        errors.append(f"{per_decade.get('1990s', 0)} cases in the 1990s, expected {low}-{high}")
    if per_decade.get("2010s", 0) > DECADE_2010S_MAX:
        errors.append(f"{per_decade.get('2010s', 0)} cases in the 2010s, at most {DECADE_2010S_MAX}")
    return errors


def check_probe_evidence(queries_doc: dict, mysql: ReadOnlyMysql | None) -> list[str]:
    """The two pre-declared probes must be true to the catalog, not prose."""
    errors: list[str] = []
    if mysql is None:
        return errors
    by_id = {case["id"]: case for case in queries_doc["queries"]}

    r1 = next((case for case in queries_doc["queries"] if case.get("known_gap") == "R-1"), None)
    if r1 is not None:
        total = mysql.select(
            "SELECT (SELECT COUNT(DISTINCT m.id) FROM movies m "
            "JOIN genres_in_movies gm ON gm.movie_id = m.id JOIN genres g ON g.id = gm.genre_id "
            "WHERE g.name = 'Music'), (SELECT COUNT(DISTINCT m.id) FROM movies m "
            "JOIN genres_in_movies gm ON gm.movie_id = m.id JOIN genres g ON g.id = gm.genre_id "
            "WHERE g.name = 'Musical')"
        )[0]
        refs = " ".join(r1.get("snapshot_row_refs") or [])
        if f"{total[0]} rows for genres" not in refs or f"{total[1]} rows for genres" not in refs:
            errors.append(f"{r1['id']}: the R-1 probe must record the catalog-wide Music "
                          f"({total[0]}) and Musical ({total[1]}) row counts")
        musical_ref = next((ref for ref in r1.get("snapshot_row_refs") or []
                            if "Musical" in ref and ref.startswith("predicate:")), None)
        if musical_ref is None:
            errors.append(f"{r1['id']}: the R-1 probe must record the Musical reading as a predicate")

    r2 = next((case for case in queries_doc["queries"] if case.get("known_gap") == "R-2"), None)
    if r2 is not None:
        years = [int(row[0]) for row in mysql.select(
            "SELECT m.year FROM movies m WHERE m.id IN ("
            + ",".join(f"'{movie_id}'" for movie_id in r2["acceptable_movie_ids"]) + ")"
        )] if r2.get("acceptable_movie_ids") else []
        if years and not all(years[0] // 10 == year // 10 for year in years):
            errors.append(f"{r2['id']}: the R-2 acceptable set must stay inside one decade")
    return errors


def check_rows_refs(queries_doc: dict, mysql: ReadOnlyMysql | None) -> list[str]:
    errors: list[str] = []
    for case in queries_doc["queries"]:
        match = ROW_REF_PATTERN.search(case["snapshot_row_refs"][0])
        if not match:
            errors.append(f"{case['id']}: first snapshot_row_ref does not record 'rows=N | ids=...'")
            continue
        recorded_rows = int(match.group(1))
        recorded_ids = [part for part in match.group(2).split(",") if part and part != "(none)"]
        if recorded_rows != len(recorded_ids):
            errors.append(f"{case['id']}: row ref says rows={recorded_rows} but lists "
                          f"{len(recorded_ids)} ids")
        if recorded_ids != case["acceptable_movie_ids"]:
            errors.append(f"{case['id']}: row ref ids differ from acceptable_movie_ids")
    return errors


# --------------------------------------------------------------------------------------
# 9. alias table
# --------------------------------------------------------------------------------------

def check_aliases(queries_doc: dict) -> list[str]:
    errors: list[str] = []
    title_doc = load_json(TITLE_ALIASES_PATH)
    entries = title_doc.get("entries")
    if entries != []:
        errors.append(f"title-aliases.entries must stay the empty list (decision C10); found "
                      f"{len(entries or [])} entries")
    pre_seeded = {entry.get("zh_title") for entry in entries or []}
    for case in queries_doc.get("queries", []):
        if not case.get("cjk_title"):
            continue
        for title in cjk_titles_of(case):
            if title in pre_seeded:
                errors.append(f"{case['id']}: Chinese title {title!r} is pre-seeded in "
                              f"title-aliases.json (that would measure the table, not the resolver)")
    return errors


# --------------------------------------------------------------------------------------
# 10. bindings, contract and frozen hashes
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
        elif key in ("year_from", "year_to", "year"):
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
                    errors.append(f"{cid}: {label}.year_range.{bound} is outside the coverage")
            if _is_int(value["from"]) and _is_int(value["to"]) and value["from"] > value["to"]:
                errors.append(f"{cid}: {label}.year_range.from > to")
        elif key in ("director", "query", "seed_title"):
            if not isinstance(value, str) or len(value) < 2:
                errors.append(f"{cid}: {label}.{key} must be a string of at least 2 characters")
        else:
            errors.append(f"{cid}: {label} uses unknown slot {key!r}")
    return errors


def check_bindings(queries_doc: dict, routing_doc: dict, schema_doc: dict) -> list[str]:
    errors: list[str] = []
    queries_sha = sha256_of(BLIND_PATH)
    routing_sha = sha256_of(BLIND_ROUTING_PATH)

    if FROZEN_BLIND_SHA256.startswith("__"):
        errors.append("FROZEN_BLIND_SHA256 has not been filled in")
    elif queries_sha != FROZEN_BLIND_SHA256:
        errors.append(f"blind queries sha256 {queries_sha} != frozen {FROZEN_BLIND_SHA256} "
                      f"(a frozen fixture changed: bump blind_version and record the run number)")
    if FROZEN_BLIND_ROUTING_SHA256.startswith("__"):
        errors.append("FROZEN_BLIND_ROUTING_SHA256 has not been filled in")
    elif routing_sha != FROZEN_BLIND_ROUTING_SHA256:
        errors.append(f"blind routing sha256 {routing_sha} != frozen {FROZEN_BLIND_ROUTING_SHA256}")

    annotates = routing_doc.get("annotates", {})
    if annotates.get("sha256") != queries_sha:
        errors.append(f"annotates.sha256 {annotates.get('sha256')} != current blind queries "
                      f"{queries_sha}")
    if annotates.get("file") != "docs/ai/golden/queries-blind.b1-5d.json":
        errors.append(f"annotates.file {annotates.get('file')!r} must bind the blind queries file")
    if annotates.get("blind_version") != queries_doc.get("blind_version"):
        errors.append("annotates.blind_version differs from the blind queries file")

    contract = routing_doc.get("schema_contract", {})
    if contract.get("contract_version") != EXPECTED_CONTRACT_VERSION:
        errors.append(f"schema_contract.contract_version {contract.get('contract_version')!r} != "
                      f"{EXPECTED_CONTRACT_VERSION!r}")
    if contract.get("schema_hash") != EXPECTED_SCHEMA_HASH:
        errors.append(f"schema_contract.schema_hash {contract.get('schema_hash')!r} != "
                      f"{EXPECTED_SCHEMA_HASH}")
    if schema_doc.get("schema_hash") != EXPECTED_SCHEMA_HASH:
        errors.append("tool-schema.b1-5.json no longer carries the frozen v3 schema_hash")
    if schema_doc.get("contract_version") != EXPECTED_CONTRACT_VERSION:
        errors.append("tool-schema.b1-5.json no longer carries the frozen contract_version")

    vocabulary = routing_doc.get("slot_vocabulary", {})
    expected_vocabulary = set(ARG_SLOTS) | set(ROUTING_ONLY_SLOTS) | {"year"}
    if set(vocabulary) != expected_vocabulary:
        errors.append(f"slot_vocabulary {sorted(vocabulary)} != {sorted(expected_vocabulary)}")
    constraint_vocabulary = routing_doc.get("result_constraint_vocabulary", {})
    if set(constraint_vocabulary) != set(RESULT_CONSTRAINTS):
        errors.append(f"result_constraint_vocabulary {sorted(constraint_vocabulary)} != "
                      f"{sorted(RESULT_CONSTRAINTS)}")
    return errors


def check_routing(queries_doc: dict, routing_doc: dict, schema_doc: dict) -> list[str]:
    errors: list[str] = []
    enum = genre_enum(schema_doc)
    by_id = {case["id"]: case for case in queries_doc.get("queries", [])}
    for entry in routing_doc.get("annotations", []):
        qid = entry.get("query_id")
        case = by_id.get(qid)
        if case is None:
            errors.append(f"{qid}: no such blind query")
            continue
        for key in entry:
            if key not in ANNOTATION_KEYS:
                errors.append(f"{qid}: unexpected annotation key {key!r}")
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
            expected = []
        elif case.get("allows_clarification"):
            expected = CLARIFICATION_TOOLS
        else:
            expected = INTENT_TOOLS[case["intent"]]
        if sorted(tools) != sorted(expected):
            errors.append(f"{qid}: expected_tools {tools} != {expected}")

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
                errors.append(f"{qid}: accept_alternative_slots[{index}] keys "
                              f"{sorted(alternative)} are not a subset of required_slots keys "
                              f"{sorted(slots)}")
            errors.extend(check_slot_values(qid, f"accept_alternative_slots[{index}]", alternative,
                                            enum))

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
# 11. read-only self-attestation
# --------------------------------------------------------------------------------------

READ_ONLY_FORBIDDEN_ATTRS = {
    "write_text", "write_bytes", "touch", "mkdir", "unlink", "rmdir", "rename", "replace",
}
READ_ONLY_FORBIDDEN_MODULES = {"subprocess", "shutil", "tempfile", "pickle"}


def check_read_only(mysql: ReadOnlyMysql | None) -> list[str]:
    """Assert the checker itself cannot write: no write calls in the AST, no write-capable module."""
    errors: list[str] = []
    tree = ast.parse(Path(__file__).read_text(encoding="utf-8"))
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                if alias.name.split(".")[0] in READ_ONLY_FORBIDDEN_MODULES:
                    errors.append(f"checker imports {alias.name!r}: this script must stay read-only")
        elif isinstance(node, ast.ImportFrom):
            module = (node.module or "").split(".")[0]
            if module in READ_ONLY_FORBIDDEN_MODULES:
                errors.append(f"checker imports from {node.module!r}: this script must stay read-only")
        elif isinstance(node, ast.Call):
            func = node.func
            if isinstance(func, ast.Name) and func.id == "open":
                errors.append("checker calls open(): this script must stay read-only")
            elif isinstance(func, ast.Attribute) and func.attr in READ_ONLY_FORBIDDEN_ATTRS:
                errors.append(f"checker calls .{func.attr}(): this script must stay read-only")
    if mysql is None:
        return errors
    for statement in ("UPDATE movies SET year = 0", "DELETE FROM movies",
                      "INSERT INTO genres VALUES (1)"):
        try:
            mysql.select(statement)
        except ValueError:
            continue
        errors.append(f"ReadOnlyMysql accepted a write statement: {statement!r}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--skip-db", action="store_true",
                        help="file-level checks only (no mysql, no year/decade/catalog checks)")
    parser.add_argument("--quiet", action="store_true", help="summary only")
    args = parser.parse_args()

    queries_doc = load_json(BLIND_PATH)
    routing_doc = load_json(BLIND_ROUTING_PATH)
    schema_doc = load_json(TOOL_SCHEMA_PATH)
    mysql = None if args.skip_db else ReadOnlyMysql()

    checks = [
        ("1 structure", lambda: check_structure(queries_doc, routing_doc)),
        ("2 ids", lambda: check_ids(queries_doc, routing_doc)),
        ("3 tracks and text", lambda: check_tracks(queries_doc)),
        ("4 intent quota", lambda: check_intent_quota(queries_doc, routing_doc)),
        ("5 cross marks", lambda: check_cross_marks(queries_doc, routing_doc)),
        ("6 refusal and clarification", lambda: check_answer_shapes(routing_doc, queries_doc)),
        ("7 exclusion", lambda: check_exclusion(queries_doc, routing_doc)),
        ("8 decades and rating coverage", lambda: check_decades(queries_doc, routing_doc, mysql)),
        ("9 alias table", lambda: check_aliases(queries_doc)),
        ("10 bindings and hashes", lambda: check_bindings(queries_doc, routing_doc, schema_doc)),
        ("11 routing annotations", lambda: check_routing(queries_doc, routing_doc, schema_doc)),
        ("12 catalog referential", lambda: check_rows_refs(queries_doc, mysql)),
        ("13 probe evidence", lambda: check_probe_evidence(queries_doc, mysql)),
        ("14 read-only", lambda: check_read_only(mysql)),
    ]
    errors: list[str] = []
    if not args.quiet:
        print(f"blind queries:   {BLIND_PATH.relative_to(REPO_ROOT)}")
        print(f"sha256:          {sha256_of(BLIND_PATH)}")
        print(f"blind routing:   {BLIND_ROUTING_PATH.relative_to(REPO_ROOT)}")
        print(f"sha256:          {sha256_of(BLIND_ROUTING_PATH)}")
        print(f"blind_version:   {queries_doc.get('blind_version')} / {routing_doc.get('routing_version')}")
        print(f"contract:        {schema_doc.get('contract_version')} / {schema_doc.get('schema_hash')}")
    for name, check in checks:
        problems = check()
        if problems:
            errors.extend(f"[{name}] {problem}" for problem in problems)
        if not args.quiet:
            print(f"  check {name}: {'OK' if not problems else f'{len(problems)} problem(s)'}")
    if mysql is None and not args.quiet:
        print("  (--skip-db: decade, rating-coverage, catalog-existence and probe evidence "
              "checks are not exercised)")

    if errors:
        print(f"\nFAILED ({len(errors)} problems):")
        for error in errors:
            print(f"  - {error}")
        return 1
    print("\nOK: 30 blind queries, their routing annotations, the exclusions, the shapes, the "
          "frozen hashes and the catalog evidence are consistent.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Validate the B1.5f blind holdout (30 queries ``C01``-``C30`` and their routing annotations).

Stdlib only, read-only. This script is the *only* new file in ``scripts/eval/`` for B1.5f and it sits
outside the scoring path: it imports no scoring code (``tool_eval`` is never imported), ``tool_eval.py``
never calls it, and it writes no file. It reads the two new fixtures, the frozen context they must stay
compatible with (the 135 scored earlier cases, the three earlier routing files, the alias tables, the
tool contract) and the catalog snapshot.

``scripts/eval/check_blind_holdout.py`` (the B1.5d checker) is **not** modified and not parameterised:
its SHA-256 is part of B1.5d's frozen evidence. This file re-implements the assertion groups for the new
prefix and adds the B1.5f-specific ones.

Eighteen assertion groups
------------------------
1. **structure** — both files parse, no unknown top-level key, the version/created/snapshot/quota blocks
   are present and the quota block equals the recomputed counts, the snapshot equals the frozen
   ``queries.b1-5.json`` snapshot, and every case carries the documented field set with the documented
   types.
2. **ids** — exactly ``C01``-``C30`` in order, no duplicate, and the routing ``query_id`` list mirrors it.
3. **tracks and text** — 10/10/10 across ``zh_zh``/``zh_en``/``en_en``, ``lang`` consistent with the
   track, no duplicate text, and no text identical (modulo punctuation/case) to an earlier case.
4. **intent quota** — search 5 / filter 9 / similar 7 / recommend 3 / clarify-or-refuse 6 (4 refusals +
   2 clarifications), and the routing intent mirrors the query intent case by case.
5. **cross marks** — ``multi_slot`` is exactly 4 and only on ``filter_catalog``/``similar_movies``;
   ``cjk_title`` is exactly 2, only on an intent whose annotation has a title slot, never on a refusal,
   each carrying exactly one ``《…》`` Chinese title, both new; no other case may smuggle a ``《…》``
   title (the cold-run model-call budget of 2 depends on it).
6. **refusal and clarification shapes** — refusals have no acceptable ids, no expected tool and empty
   slots; clarifications keep a non-empty acceptable set, a title slot and at least one expected tool.
7. **exclusion** — no acceptable id appears in the 135 scored earlier cases (per-id disjointness, no
   opt-out flag), no acceptable set equals an earlier one, no text/seed repeats an earlier one, and the
   batch is internally duplicate-free.
8. **decades and rating coverage** — no 2020s row, 3-5 cases entirely in the 1990s (none constraining
   ``min_rating``), at most 1 in the 2010s, and every ``min_rating`` case has rated 2001+ rows only.
9. **alias table** — ``title-aliases.json`` is still frozen empty (decision C10) and neither Chinese
   title of this batch is pre-seeded there.
10. **bindings and hashes** — ``annotates.sha256`` binds *this* queries file, the contract block records
    ``b1.5-tools/v3`` with its schema hash, the snapshot records the frozen ``db_snapshot_hash``, both
    fixtures match the SHA-256 frozen before the first run, and the slot vocabularies are the shared ones.
    The checker cannot report its own SHA-256 (self-referential); it is frozen externally.
11. **routing annotations** — ``golden_predicate`` mirrors the query's first row ref, the expected tools
    follow the intent, slots belong to the expected tools, and result constraints stay on similar cases.
12. **catalog referential** — every acceptable id exists, the recorded ``rows=`` equals the id count, and
    the acceptable set is *re-derived* from the routing annotation (predicate + seed row) and compared
    id-for-id. Needs the catalog; ``--skip-db`` skips it with groups 8, 15 and 16.
13. **labelling conventions** — every ``similar_movies`` note states ``same director`` / ``shared genre ×
    window`` / ``hybrid`` plus its predicate and ``rows=`` (``BF-6``); ``P-3``/``P-4`` must be
    ``same director``.
14. **probe inventory** — ``P-1``…``P-8`` each present the documented number of times, and ``P-4``
    carries the expected-to-fail declaration (``known_gap = R-2/Q12-unsat`` + ``expected_failure``).
15. **``R-1`` union reading** — the ``P-1`` case records both the ``Music`` and the ``Musical`` readings
    and the union; the catalog is re-queried for all three counts and for the catalog-wide counts, and
    the union must be strictly larger than the ``Music`` reading (a recall expansion, not an
    equivalence). With ``--run-record`` the actual top-5 is read back and must be a subset of the union.
16. **seed genre counts and director sets** — the ``P-2`` seed has exactly two canonical genres and its
    acceptable set equals the derived genre × decade set; the ``P-3`` seed has three or more canonical
    genres plus a known director, and its acceptable set equals that director's other catalog rows
    (5-25); the ``P-4`` acceptable set is the same director construction.
17. **seed-title independence** — the three earlier routing files contribute 29 distinct seed titles
    (B1.5d's checker only scanned two of them); no batch seed repeats one, and the batch seeds are
    pairwise distinct.
18. **read-only self-attestation** — the MySQL helper refuses anything that is not a single SELECT, and
    this script opens no file for writing.

Usage
-----
    python3 scripts/eval/check_blind_holdout_b15f.py                  # every check, including the database
    python3 scripts/eval/check_blind_holdout_b15f.py --skip-db        # file-level checks only
    python3 scripts/eval/check_blind_holdout_b15f.py --quiet          # summary only
    python3 scripts/eval/check_blind_holdout_b15f.py --run-record R   # also assert top-5 ⊆ R-1 union
    python3 scripts/eval/check_blind_holdout_b15f.py --repo-root DIR  # run against a copied tree

Exit code 0 = every check passed.
"""

from __future__ import annotations

import argparse
import ast
import hashlib
import json
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
    YEAR_MAX,
    YEAR_MIN,
    genre_enum,
)

# Overridden by --repo-root (used by the mutation self-test against a copied tree).
ROOT = REPO_ROOT

BLIND_VERSION = "b1-5f-blind/v1"
ROUTING_VERSION = "b1-5f-blind-routing/v1"
# Frozen before the first measurement run; the checker itself is frozen externally (it cannot report
# its own hash without changing it).
FROZEN_QUERIES_SHA256 = "2e36bbf5ce03cba9a5e3eee297115ebfa99c9add1bee14bdcd8113941b6e121a"
FROZEN_ROUTING_SHA256 = "a74e383bbaf80e46d91469c426a7577662f4194e96059f441ee8f3b38363fb40"
DB_SNAPSHOT_HASH = "8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925"
EXPECTED_CONTRACT_VERSION = "b1.5-tools/v3"
EXPECTED_SCHEMA_HASH = "712c0e34c37b6de140c13fcc7d3de15e67ac1f131f2bb6e8751a4f5e4df3474a"
CHECKER_B15D_SHA256 = "8259cc8dd924ce5a51f09dacbfed36a02a18be9e6ad7f161c630dfaacef37caf"

EXPECTED_IDS = [f"C{i:02d}" for i in range(1, 31)]
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
PROBE_QUOTA = {"P-1": 1, "P-2": 1, "P-3": 1, "P-4": 1, "P-5": 2, "P-6": 1, "P-7": 1, "P-8": 2}
KNOWN_GAPS = {"R-1", "R-2", "R-2/Q12-unsat"}
EXPECTED_FAILURE_GAP = "R-2/Q12-unsat"
CONVENTIONS = ("same director", "shared genre × window", "hybrid")
# The Chinese titles earlier batches already used: the five from B1.5c plus the two from B1.5d.
USED_CJK_TITLES = {"肖申克的救赎", "超人", "黑客帝国", "阿甘正传", "泰坦尼克号", "乱世佳人", "异形"}

# 135 scored earlier cases (the 5 negative.b1-5.json entries are not scored and stay out).
BASELINE_FILES = (
    ("queries.b1-5.json", "queries"),
    ("tool-cases.b1-5.json", "tool_cases"),
    ("queries-heldout.b1-5c.json", "queries"),
    ("queries-blind.b1-5d.json", "queries"),
)
BASELINE_CASE_COUNT = 135
BASELINE_ID_UNION = 718
BASELINE_SEED_FILES = ("query-routing.b1-5.json", "query-routing-heldout.b1-5c.json",
                       "query-routing-blind.b1-5d.json")
BASELINE_SEED_COUNT = 29

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
    "expects_no_match", "cjk_title", "allows_clarification", "multi_slot", "probe", "known_gap",
    "expected_failure",
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

GENRE_ALIASES = json.loads((ROOT / "docs" / "ai" / "aliases" / "genre-aliases.json")
                           .read_text("utf-8"))
CANONICAL_GROUPS = GENRE_ALIASES["canonical_groups"]


# --------------------------------------------------------------------------------------
# small helpers (read-only counterparts of the B1.5d checker helpers)
# --------------------------------------------------------------------------------------

def golden_path(name: str) -> Path:
    return ROOT / "docs" / "ai" / "golden" / name


def alias_path(name: str) -> Path:
    return ROOT / "docs" / "ai" / "aliases" / name


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
    return [title for title in CJK_TITLE_PATTERN.findall(case.get("query", "")) if has_cjk(title)]


def sql_literal(value: str) -> str:
    """Quote a string for a SELECT literal without using str.replace (the read-only AST check)."""
    escaped = "".join("\\'" if char == "'" else char for char in value)
    return "'%s'" % escaped


def canonical(names) -> list[str]:
    mapping = {}
    for group in CANONICAL_GROUPS:
        for name in group:
            mapping[name] = group[0]
    return sorted({mapping.get(name, name) for name in names})


def group_members(genre: str) -> list[str]:
    """Every catalog value that denotes this genre (a canonical group is one family)."""
    for group in CANONICAL_GROUPS:
        if genre in group:
            return list(group)
    return [genre]


# --------------------------------------------------------------------------------------
# catalog access (read-only)
# --------------------------------------------------------------------------------------

def ids_filter(mysql: ReadOnlyMysql, genres=(), lo=None, hi=None, min_rating=None,
               director=None, exclude=(), literal=()) -> list[str]:
    """Canonical-AND genre filter (groups expanded) with an inclusive year window."""
    clauses = []
    for index, genre in enumerate(genres):
        listed = ",".join("'%s'" % name for name in group_members(genre))
        clauses.append(
            "(SELECT COUNT(DISTINCT g{0}.name) FROM genres_in_movies gm{0} "
            "JOIN genres g{0} ON g{0}.id = gm{0}.genre_id "
            "WHERE gm{0}.movie_id = m.id AND g{0}.name IN ({1})) >= 1"
            .format(index, listed)
        )
    for index, genre in enumerate(literal):
        clauses.append(
            "(SELECT COUNT(*) FROM genres_in_movies gm{0} JOIN genres g{0} ON g{0}.id = gm{0}.genre_id "
            "WHERE gm{0}.movie_id = m.id AND g{0}.name = '{1}') >= 1".format(index + 10, genre)
        )
    if lo is not None:
        clauses.append("m.year >= %d" % lo)
    if hi is not None:
        clauses.append("m.year <= %d" % hi)
    if min_rating is not None:
        clauses.append("r.rating >= %s" % min_rating)
    if director is not None:
        clauses.append("m.director = %s" % sql_literal(director))
    if exclude:
        clauses.append("m.id NOT IN (%s)" % ",".join("'%s'" % movie_id for movie_id in exclude))
    sql = ("SELECT DISTINCT m.id FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id WHERE "
           + " AND ".join(clauses) + " ORDER BY m.id")
    return [row[0] for row in mysql.select(sql)]


def ids_any(mysql: ReadOnlyMysql, genres, lo=None, hi=None, exclude=()) -> list[str]:
    """Union retrieval: rows carrying at least one of the listed genres (canonical groups expanded).

    This is the R-1 reading (Music ∪ Musical is a recall expansion, not an equivalence).
    """
    members: list[str] = []
    for genre in genres:
        for name in group_members(genre):
            if name not in members:
                members.append(name)
    clauses = ["g.name IN (%s)" % ",".join("'%s'" % name for name in members)]
    sql = ("SELECT DISTINCT m.id FROM movies m JOIN genres_in_movies gm ON gm.movie_id = m.id "
           "JOIN genres g ON g.id = gm.genre_id WHERE " + " AND ".join(clauses))
    if lo is not None:
        sql += " AND m.year >= %d" % lo
    if hi is not None:
        sql += " AND m.year <= %d" % hi
    if exclude:
        sql += " AND m.id NOT IN (%s)" % ",".join("'%s'" % movie_id for movie_id in exclude)
    return [row[0] for row in mysql.select(sql + " ORDER BY m.id")]


def movie_rows(mysql: ReadOnlyMysql, ids) -> dict[str, dict]:
    if not ids:
        return {}
    quoted = ",".join("'%s'" % i for i in sorted(ids))
    rows = mysql.select("SELECT m.id, m.title, m.year, m.director FROM movies m WHERE m.id IN (%s)"
                        % quoted)
    return {row[0]: {"id": row[0], "title": row[1], "year": int(row[2]), "director": row[3]}
            for row in rows}


def genres_of(mysql: ReadOnlyMysql, movie_id: str) -> list[str]:
    return [row[0] for row in mysql.select(
        "SELECT g.name FROM genres_in_movies gm JOIN genres g ON g.id = gm.genre_id "
        "WHERE gm.movie_id = '%s' ORDER BY g.name" % movie_id)]


def seed_row(mysql: ReadOnlyMysql, title: str) -> dict | None:
    rows = movie_rows(mysql, [r[0] for r in mysql.select(
        "SELECT id FROM movies WHERE title = %s" % sql_literal(title))])
    if len(rows) != 1:
        return None
    return next(iter(rows.values()))


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
        errors.append(f"routing_version {routing_doc.get('routing_version')!r} != "
                      f"{ROUTING_VERSION!r}")
    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", str(queries_doc.get("created", ""))):
        errors.append(f"created {queries_doc.get('created')!r} is not an ISO date")

    frozen_snapshot = load_json(golden_path("queries.b1-5.json")).get("snapshot")
    snapshot = queries_doc.get("snapshot")
    if snapshot != frozen_snapshot:
        errors.append("snapshot block differs from queries.b1-5.json (the snapshot is shared)")
    if (snapshot or {}).get("db_snapshot_hash") != DB_SNAPSHOT_HASH:
        errors.append(f"snapshot.db_snapshot_hash {(snapshot or {}).get('db_snapshot_hash')!r} != "
                      f"{DB_SNAPSHOT_HASH}")

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
        for flag in ("expects_no_match", "cjk_title", "allows_clarification", "multi_slot",
                     "expected_failure"):
            if flag in case and not isinstance(case[flag], bool):
                errors.append(f"{label}: {flag} must be a boolean")
        if "known_gap" in case and case["known_gap"] not in KNOWN_GAPS:
            errors.append(f"{label}: known_gap {case['known_gap']!r} is not one of "
                          f"{sorted(KNOWN_GAPS)}")
        if "probe" in case and case["probe"] not in PROBE_QUOTA:
            errors.append(f"{label}: probe {case['probe']!r} is not one of {sorted(PROBE_QUOTA)}")

    quota = queries_doc.get("quota")
    if not isinstance(quota, dict):
        errors.append("quota must be an object")
        return errors
    for key in ("tracks", "intents", "clarify_or_refuse_split", "multi_slot", "cjk_title"):
        if key not in quota:
            errors.append(f"quota is missing {key!r}")
    recomputed_tracks = {track: 0 for track in TRACKS}
    recomputed_intents = {intent: 0 for intent in INTENT_QUOTA}
    for case in cases:
        if case.get("track") in recomputed_tracks:
            recomputed_tracks[case["track"]] += 1
        if case.get("intent") in recomputed_intents:
            recomputed_intents[case["intent"]] += 1
    if quota.get("tracks") != recomputed_tracks:
        errors.append(f"quota.tracks {quota.get('tracks')} != recomputed {recomputed_tracks}")
    if quota.get("intents") != recomputed_intents:
        errors.append(f"quota.intents {quota.get('intents')} != recomputed {recomputed_intents}")
    if quota.get("multi_slot") != sum(1 for case in cases if case.get("multi_slot")):
        errors.append("quota.multi_slot differs from the number of marked cases")
    if quota.get("cjk_title") != sum(1 for case in cases if case.get("cjk_title")):
        errors.append("quota.cjk_title differs from the number of marked cases")
    split = quota.get("clarify_or_refuse_split") or {}
    if split.get("refusal") != sum(1 for case in cases if case.get("expects_no_match")):
        errors.append("quota.clarify_or_refuse_split.refusal differs from the refusal count")
    if split.get("clarification") != sum(1 for case in cases if case.get("allows_clarification")):
        errors.append("quota.clarify_or_refuse_split.clarification differs from the count")
    return errors


# --------------------------------------------------------------------------------------
# 2. ids
# --------------------------------------------------------------------------------------

def check_ids(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    ids = [case.get("id") for case in queries_doc.get("queries", [])]
    if ids != EXPECTED_IDS:
        errors.append(f"ids {ids} != C01..C30 (order matters: the run record is positional)")
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
            errors.append(f"{case.get('id')}: intent {intent!r} is outside the quota")
            continue
        counts[intent] += 1
    for name, expected in INTENT_QUOTA.items():
        if counts[name] != expected:
            errors.append(f"intent {name}: {counts[name]} cases, expected {expected}")

    by_id = {case.get("id"): case for case in queries_doc.get("queries", [])}
    for entry in routing_doc.get("annotations", []):
        case = by_id.get(entry.get("query_id"))
        if case is None:
            continue
        if entry.get("intent") != case.get("intent"):
            errors.append(f"{case['id']}: routing intent {entry.get('intent')!r} != query intent "
                          f"{case.get('intent')!r}")
    return errors


# --------------------------------------------------------------------------------------
# 5. cross marks (multi_slot, cjk_title)
# --------------------------------------------------------------------------------------

def check_cross_marks(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    annotations = {entry.get("query_id"): entry for entry in routing_doc.get("annotations", [])}
    multi_slot = 0
    cjk = 0
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
                errors.append(f"{cid}: a cjk_title case needs exactly one 《…》 title, found "
                              f"{titles}")
            else:
                chinese_titles.append((cid, titles[0]))
        elif cjk_titles_of(case):
            errors.append(f"{cid}: Chinese title(s) {cjk_titles_of(case)} outside the "
                          f"{EXPECTED_CJK} cjk_title cases (it would add a resolver call to the "
                          f"frozen cold-run budget)")

    if multi_slot != EXPECTED_MULTI_SLOT:
        errors.append(f"{multi_slot} multi_slot cases, expected {EXPECTED_MULTI_SLOT}")
    if cjk != EXPECTED_CJK:
        errors.append(f"{cjk} cjk_title cases, expected {EXPECTED_CJK}")
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

def check_answer_shapes(queries_doc: dict, routing_doc: dict) -> list[str]:
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
# 7. exclusion against the 135 scored earlier cases
# --------------------------------------------------------------------------------------

def baseline_cases() -> list[dict]:
    cases: list[dict] = []
    for name, key in BASELINE_FILES:
        cases.extend(load_json(golden_path(name)).get(key, []))
    return cases


def baseline_seed_titles() -> set[str]:
    titles: set[str] = set()
    for name in BASELINE_SEED_FILES:
        for annotation in load_json(golden_path(name)).get("annotations", []):
            seed = (annotation.get("required_slots") or {}).get("seed_title")
            if isinstance(seed, str):
                titles.add(seed.casefold())
    return titles


def check_exclusion(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    previous = baseline_cases()
    if len(previous) != BASELINE_CASE_COUNT:
        errors.append(f"expected {BASELINE_CASE_COUNT} scored earlier cases, found {len(previous)}")

    previous_texts = {normalise_text(case.get("query", "")): case.get("id") for case in previous}
    previous_id_sets = {frozenset(case.get("acceptable_movie_ids") or []) for case in previous}
    previous_ids = {movie_id for case in previous
                    for movie_id in case.get("acceptable_movie_ids") or []}
    if len(previous_ids) != BASELINE_ID_UNION:
        errors.append(f"the scored earlier cases use {len(previous_ids)} distinct ids, expected "
                      f"{BASELINE_ID_UNION}")

    previous_titles: set[str] = set()
    for name in BASELINE_SEED_FILES:
        for annotation in load_json(golden_path(name)).get("annotations", []):
            slots = annotation.get("required_slots") or {}
            for key in TITLE_SLOTS:
                if isinstance(slots.get(key), str):
                    previous_titles.add(slots[key].casefold())

    seen_texts: dict[str, str] = {}
    seen_sets: dict[frozenset, str] = {}
    for case in queries_doc.get("queries", []):
        cid = case.get("id")
        text_key = normalise_text(case.get("query", ""))
        if text_key in previous_texts:
            errors.append(f"{cid}: query text repeats {previous_texts[text_key]} "
                          f"(identical modulo punctuation)")
        if text_key in seen_texts:
            errors.append(f"{cid}: query text duplicates {seen_texts[text_key]} inside the batch")
        seen_texts[text_key] = cid
        ids = case.get("acceptable_movie_ids") or []
        overlap = sorted(set(ids) & previous_ids)
        if overlap:
            errors.append(f"{cid}: acceptable ids already scored by the 135 earlier cases: "
                          f"{overlap[:8]}")
        if ids and frozenset(ids) in previous_id_sets:
            errors.append(f"{cid}: acceptable id set equals an earlier case's set")
        if ids and frozenset(ids) in seen_sets:
            errors.append(f"{cid}: acceptable id set equals {seen_sets[frozenset(ids)]} inside the "
                          f"batch")
        if ids:
            seen_sets[frozenset(ids)] = cid
        for movie_id in ids:
            if movie_id in SYNTHETIC_IDS:
                errors.append(f"{cid}: synthetic dev-seed id {movie_id} in the acceptable set")

    seeds: dict[str, str] = {}
    for entry in routing_doc.get("annotations", []):
        qid = entry.get("query_id")
        seed = (entry.get("required_slots") or {}).get("seed_title")
        if not isinstance(seed, str):
            continue
        key = seed.casefold()
        if key in previous_titles:
            errors.append(f"{qid}: seed title {seed!r} is already used by an earlier case")
        if key in seeds:
            errors.append(f"{qid}: seed title {seed!r} already used by {seeds[key]} in this batch")
        seeds[key] = qid
        if key in {title.casefold() for title in SYNTHETIC_TITLES}:
            errors.append(f"{qid}: seed title {seed!r} is a synthetic dev-seed row")
    return errors


# --------------------------------------------------------------------------------------
# 8. decades and rating coverage
# --------------------------------------------------------------------------------------

def check_decades(queries_doc: dict, routing_doc: dict, mysql: ReadOnlyMysql | None) -> list[str]:
    errors: list[str] = []
    if mysql is None:
        return errors
    annotations = {entry.get("query_id"): entry for entry in routing_doc.get("annotations", [])}
    all_ids = sorted({movie_id for case in queries_doc["queries"]
                      for movie_id in case.get("acceptable_movie_ids") or []})
    rows = movie_rows(mysql, all_ids)
    missing = [movie_id for movie_id in all_ids if movie_id not in rows]
    if missing:
        errors.append(f"acceptable ids that do not exist in the catalog: {', '.join(missing)}")
    for movie_id, row in rows.items():
        if row["title"] in SYNTHETIC_TITLES:
            errors.append(f"{movie_id}: synthetic dev-seed title {row['title']!r} in the set")

    per_decade: dict[str, int] = {}
    for case in queries_doc["queries"]:
        cid = case["id"]
        years = [rows[movie_id]["year"] for movie_id in case.get("acceptable_movie_ids") or []
                 if movie_id in rows]
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
        if uses_rating:
            rating = slots.get("min_rating", constraints.get("min_rating"))
            early = [movie_id for movie_id in case["acceptable_movie_ids"]
                     if rows.get(movie_id, {}).get("year", 0) < RATING_COVERAGE_FROM]
            if early:
                errors.append(f"{cid}: min_rating case contains pre-{RATING_COVERAGE_FROM} rows "
                              f"{early[:5]} that cannot have a rating")
            for movie_id in case["acceptable_movie_ids"]:
                hit = mysql.select("SELECT rating FROM ratings WHERE movie_id = '%s' "
                                   "AND rating >= %s" % (movie_id, rating))
                if not hit:
                    errors.append(f"{cid}: row {movie_id} is not rated >= {rating}")

    low, high = DECADE_1990S_RANGE
    if not low <= per_decade.get("1990s", 0) <= high:
        errors.append(f"{per_decade.get('1990s', 0)} cases in the 1990s, expected {low}-{high}")
    if per_decade.get("2010s", 0) > DECADE_2010S_MAX:
        errors.append(f"{per_decade.get('2010s', 0)} cases in the 2010s, at most "
                      f"{DECADE_2010S_MAX}")
    return errors


# --------------------------------------------------------------------------------------
# 9. alias table
# --------------------------------------------------------------------------------------

def check_aliases(queries_doc: dict) -> list[str]:
    errors: list[str] = []
    title_doc = load_json(alias_path("title-aliases.json"))
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
    queries_sha = sha256_of(golden_path("queries-blind.b1-5f.json"))
    routing_sha = sha256_of(golden_path("query-routing-blind.b1-5f.json"))

    if FROZEN_QUERIES_SHA256.startswith("__"):
        errors.append("FROZEN_QUERIES_SHA256 has not been filled in")
    elif queries_sha != FROZEN_QUERIES_SHA256:
        errors.append(f"blind queries sha256 {queries_sha} != frozen {FROZEN_QUERIES_SHA256} "
                      f"(a frozen fixture changed: bump blind_version and record the run number)")
    if FROZEN_ROUTING_SHA256.startswith("__"):
        errors.append("FROZEN_ROUTING_SHA256 has not been filled in")
    elif routing_sha != FROZEN_ROUTING_SHA256:
        errors.append(f"blind routing sha256 {routing_sha} != frozen {FROZEN_ROUTING_SHA256}")

    annotates = routing_doc.get("annotates", {})
    if annotates.get("sha256") != queries_sha:
        errors.append(f"annotates.sha256 {annotates.get('sha256')} != current blind queries "
                      f"{queries_sha}")
    if annotates.get("file") != "docs/ai/golden/queries-blind.b1-5f.json":
        errors.append(f"annotates.file {annotates.get('file')!r} must bind the B1.5f queries file")
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

    b15d = Path(__file__).resolve().parent / "check_blind_holdout.py"
    if b15d.is_file() and sha256_of(b15d) != CHECKER_B15D_SHA256:
        errors.append(f"check_blind_holdout.py sha256 {sha256_of(b15d)} != {CHECKER_B15D_SHA256} "
                      f"(the B1.5d checker is part of B1.5d's frozen evidence)")
    return errors


# --------------------------------------------------------------------------------------
# 11. routing annotations
# --------------------------------------------------------------------------------------

def check_routing(queries_doc: dict, routing_doc: dict, schema_doc: dict) -> list[str]:
    errors: list[str] = []
    enum = genre_enum(schema_doc)
    by_id = {case["id"]: case for case in queries_doc.get("queries", [])}
    for entry in routing_doc.get("annotations", []):
        qid = entry.get("query_id")
        case = by_id.get(qid)
        if case is None:
            errors.append(f"{qid}: no such query")
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
# 12. catalog referential + re-derivation from the annotation
# --------------------------------------------------------------------------------------

def rederive(case: dict, entry: dict, mysql: ReadOnlyMysql) -> list[str] | None:
    """Recompute the acceptable set from the routing annotation; None when not applicable."""
    intent = case["intent"]
    slots = entry.get("required_slots") or {}
    constraints = entry.get("result_constraints") or {}
    probe = case.get("probe")
    if case.get("expects_no_match"):
        return []
    if case.get("allows_clarification"):
        title = slots.get("query")
        if not isinstance(title, str):
            return None
        rows = movie_rows(mysql, [row[0] for row in mysql.select(
            "SELECT id FROM movies WHERE title = %s" % sql_literal(title))])
        return sorted(rows)
    if intent == "search_by_title":
        title = slots.get("query")
        if not isinstance(title, str):
            return None
        clauses = ["title = %s" % sql_literal(title)]
        if _is_int(slots.get("year")):
            clauses.append("year = %d" % slots["year"])
        rows = mysql.select("SELECT id FROM movies WHERE " + " AND ".join(clauses) + " ORDER BY id")
        return [row[0] for row in rows]
    if intent in ("filter_catalog", "recommend_by_preference"):
        genres = slots.get("genres") or slots.get("preferred_genres") or []
        year_from = slots.get("year_from")
        year_to = slots.get("year_to")
        year_range = slots.get("year_range") or {}
        lo = year_from if year_from is not None else year_range.get("from")
        hi = year_to if year_to is not None else year_range.get("to")
        if not genres and lo is None and hi is None and slots.get("director") is None:
            return None
        if case.get("known_gap") == "R-1":
            return ids_any(mysql, genres, lo, hi)
        return ids_filter(mysql, genres, lo, hi, slots.get("min_rating"), slots.get("director"))
    if intent == "similar_movies":
        seed_title = slots.get("seed_title")
        if not isinstance(seed_title, str):
            return None
        seed = seed_row(mysql, seed_title)
        if seed is None:
            return None
        if constraints.get("director"):
            return ids_filter(mysql, director=constraints["director"], exclude=[seed["id"]])
        if constraints.get("genres") or constraints.get("year_from") is not None \
                or constraints.get("year_to") is not None:
            return ids_filter(mysql, constraints.get("genres") or [],
                              constraints.get("year_from"), constraints.get("year_to"),
                              constraints.get("min_rating"), exclude=[seed["id"]])
        seed_genres = canonical(genres_of(mysql, seed["id"]))
        if probe in ("P-2",):
            decade = seed["year"] // 10 * 10
            return ids_filter(mysql, seed_genres, decade, decade + 9, exclude=[seed["id"]])
        if probe in ("P-3", "P-4"):
            if not seed["director"]:
                return None
            return ids_filter(mysql, director=seed["director"], exclude=[seed["id"]])
        return None
    return None


def check_catalog_referential(queries_doc: dict, routing_doc: dict,
                              mysql: ReadOnlyMysql | None) -> list[str]:
    errors: list[str] = []
    annotations = {entry.get("query_id"): entry for entry in routing_doc.get("annotations", [])}
    for case in queries_doc.get("queries", []):
        cid = case["id"]
        ids = case.get("acceptable_movie_ids") or []
        if len(ids) > 25:
            errors.append(f"{cid}: {len(ids)} acceptable ids exceed the 25-id cap")
        match = ROW_REF_PATTERN.search(case["snapshot_row_refs"][0])
        if not match:
            errors.append(f"{cid}: first snapshot_row_ref does not record 'rows=N | ids=...'")
            continue
        recorded_rows = int(match.group(1))
        recorded_ids = [part for part in match.group(2).split(",") if part and part != "(none)"]
        if recorded_rows != len(recorded_ids):
            errors.append(f"{cid}: row ref says rows={recorded_rows} but lists "
                          f"{len(recorded_ids)} ids")
        if recorded_ids != ids:
            errors.append(f"{cid}: row ref ids differ from acceptable_movie_ids")
        if mysql is None:
            continue
        derived = rederive(case, annotations.get(cid) or {}, mysql)
        if derived is None:
            errors.append(f"{cid}: the annotation does not determine a re-derivable predicate")
        elif derived != ids:
            errors.append(f"{cid}: acceptable set {ids} != re-derived set {derived} "
                          f"(the recorded predicate and the annotation disagree)")
    return errors


# --------------------------------------------------------------------------------------
# 13. labelling conventions
# --------------------------------------------------------------------------------------

def check_conventions(queries_doc: dict) -> list[str]:
    errors: list[str] = []
    for case in queries_doc.get("queries", []):
        if case.get("intent") != "similar_movies":
            continue
        cid = case["id"]
        note = case.get("labeling_note", "")
        found = [name for name in CONVENTIONS if name in note]
        if not found:
            errors.append(f"{cid}: a similar_movies note must state one of {list(CONVENTIONS)}")
        if "predicate:" not in note and "predicate:" not in " ".join(case["snapshot_row_refs"]):
            errors.append(f"{cid}: a similar_movies note must name its predicate")
        if "rows=" not in note and "rows=" not in " ".join(case["snapshot_row_refs"]):
            errors.append(f"{cid}: a similar_movies note must record rows=")
        if case.get("probe") in ("P-3", "P-4") and "same director" not in note:
            errors.append(f"{cid}: the {case['probe']} probe must use the 'same director' "
                          f"convention")
    return errors


# --------------------------------------------------------------------------------------
# 14. probe inventory
# --------------------------------------------------------------------------------------

def check_probe_inventory(queries_doc: dict) -> list[str]:
    errors: list[str] = []
    found: dict[str, list[str]] = {probe: [] for probe in PROBE_QUOTA}
    for case in queries_doc.get("queries", []):
        probe = case.get("probe")
        if probe:
            found.setdefault(probe, []).append(case["id"])
    for probe, expected in PROBE_QUOTA.items():
        owners = found.get(probe, [])
        if len(owners) != expected:
            errors.append(f"probe {probe}: {len(owners)} case(s) {owners}, expected {expected}")
    for probe, owners in found.items():
        if probe not in PROBE_QUOTA and owners:
            errors.append(f"unknown probe {probe!r} on {owners}")

    first = {probe: owners[0] for probe, owners in found.items() if owners}
    by_id = {case["id"]: case for case in queries_doc.get("queries", [])}
    p1 = by_id.get(first.get("P-1", ""))
    if p1 is not None:
        if p1.get("intent") != "filter_catalog":
            errors.append(f"{p1['id']}: P-1 must be a filter_catalog case")
        if "musical" not in p1.get("query", "").casefold():
            errors.append(f"{p1['id']}: P-1 must contain the musicals/musical term")
        if p1.get("known_gap") != "R-1":
            errors.append(f"{p1['id']}: P-1 must declare known_gap = R-1 (found "
                          f"{p1.get('known_gap')!r})")
    p2 = by_id.get(first.get("P-2", ""))
    if p2 is not None:
        if p2.get("intent") != "similar_movies":
            errors.append(f"{p2['id']}: P-2 must be a similar_movies case")
        if p2.get("known_gap") != "R-2":
            errors.append(f"{p2['id']}: P-2 must declare known_gap = R-2 (found "
                          f"{p2.get('known_gap')!r})")
    p3 = by_id.get(first.get("P-3", ""))
    if p3 is not None and p3.get("intent") != "similar_movies":
        errors.append(f"{p3['id']}: P-3 must be a similar_movies case")
    p4 = by_id.get(first.get("P-4", ""))
    if p4 is not None:
        if p4.get("intent") != "similar_movies":
            errors.append(f"{p4['id']}: P-4 must be a similar_movies case")
        if p4.get("known_gap") != EXPECTED_FAILURE_GAP:
            errors.append(f"{p4['id']}: P-4 must declare known_gap = {EXPECTED_FAILURE_GAP!r} "
                          f"(found {p4.get('known_gap')!r})")
        if p4.get("expected_failure") is not True:
            errors.append(f"{p4['id']}: P-4 must carry expected_failure = true")
    for cid in found.get("P-5", []):
        case = by_id[cid]
        if not case.get("expects_no_match"):
            errors.append(f"{cid}: P-5 cases must be refusals (expects_no_match)")
    for cid in found.get("P-6", []):
        if by_id[cid].get("intent") != "similar_movies":
            errors.append(f"{cid}: P-6 must be a similar_movies case")
    for cid in found.get("P-7", []):
        case = by_id[cid]
        if case.get("intent") != "filter_catalog":
            errors.append(f"{cid}: P-7 must be a filter_catalog case")
        if "documentaries" not in case.get("query", "").casefold():
            errors.append(f"{cid}: P-7 must use the plural genre word documentaries")
    p8 = found.get("P-8", [])
    for cid in p8:
        if not by_id[cid].get("cjk_title"):
            errors.append(f"{cid}: P-8 cases must be the cjk_title cases")
    if sorted(p8) != sorted(case["id"] for case in queries_doc["queries"]
                            if case.get("cjk_title")):
        errors.append(f"P-8 owners {p8} != the cjk_title cases")
    return errors


# --------------------------------------------------------------------------------------
# 15. R-1 union reading
# --------------------------------------------------------------------------------------

def check_r1_union(queries_doc: dict, mysql: ReadOnlyMysql | None,
                   run_record: Path | None) -> list[str]:
    errors: list[str] = []
    p1 = next((case for case in queries_doc["queries"] if case.get("probe") == "P-1"), None)
    if p1 is None:
        return ["no P-1 case to check"]
    refs = p1.get("snapshot_row_refs") or []
    joined = " ".join(refs)
    for genre in ("Music", "Musical"):
        if genre not in joined:
            errors.append(f"{p1['id']}: the R-1 probe must record the {genre} reading")
    if "catalog-wide" not in joined:
        errors.append(f"{p1['id']}: the R-1 probe must record the catalog-wide readings")
    if mysql is None:
        return errors

    union_ids: list[str] = []
    music_ids: list[str] = []
    musical_ids: list[str] = []
    for ref in refs:
        match = ROW_REF_PATTERN.search(ref)
        if not match:
            continue
        ids = [part for part in match.group(2).split(",") if part]
        if int(match.group(1)) != len(ids):
            errors.append(f"{p1['id']}: row ref records rows={match.group(1)} but lists "
                          f"{len(ids)} ids")
        if "(union 口径)" in ref:
            union_ids = ids
        elif "⊇ {Music}" in ref and "Musical" not in ref:
            music_ids = ids
        elif "⊇ {Musical}" in ref:
            musical_ids = ids
    if not union_ids or not music_ids or not musical_ids:
        errors.append(f"{p1['id']}: the R-1 probe must record the union and both single readings "
                      f"with their ids")
        return errors

    years = re.search(r"year >= (\d+) AND year <= (\d+)", p1["snapshot_row_refs"][0])
    if not years:
        errors.append(f"{p1['id']}: the R-1 predicate must record an explicit year window")
        return errors
    lo, hi = int(years.group(1)), int(years.group(2))
    sql_music = sorted(ids_filter(mysql, literal=["Music"], lo=lo, hi=hi))
    sql_musical = sorted(ids_filter(mysql, literal=["Musical"], lo=lo, hi=hi))
    sql_union = sorted(set(sql_music) | set(sql_musical))
    if sorted(music_ids) != sql_music:
        errors.append(f"{p1['id']}: the Music reading {len(music_ids)} ids != catalog "
                      f"{len(sql_music)} rows for {lo}-{hi}")
    if sorted(musical_ids) != sql_musical:
        errors.append(f"{p1['id']}: the Musical reading {len(musical_ids)} ids != catalog "
                      f"{len(sql_musical)} rows for {lo}-{hi}")
    if sorted(union_ids) != sql_union:
        errors.append(f"{p1['id']}: the union reading {len(union_ids)} ids != catalog "
                      f"{len(sql_union)} rows for {lo}-{hi}")
    if sorted(union_ids) != sorted(p1["acceptable_movie_ids"]):
        errors.append(f"{p1['id']}: acceptable_movie_ids must equal the union reading")
    if not set(sql_union) > set(sql_music):
        errors.append(f"{p1['id']}: the union must be strictly larger than the Music reading "
                      f"(R-1 is a recall expansion, not an equivalence)")
    all_music = ids_filter(mysql, literal=["Music"])
    all_musical = ids_filter(mysql, literal=["Musical"])
    if f"{len(all_music)} rows for genres ⊇ {{Music}}" not in joined:
        errors.append(f"{p1['id']}: the catalog-wide Music count ({len(all_music)}) is missing or "
                      f"stale")
    if f"{len(all_musical)} rows for genres ⊇ {{Musical}}" not in joined:
        errors.append(f"{p1['id']}: the catalog-wide Musical count ({len(all_musical)}) is missing "
                      f"or stale")

    if run_record is not None:
        record = load_json(run_record)
        entry = next((item for item in record.get("cases", [])
                      if item.get("id") == p1["id"] or item.get("query_id") == p1["id"]), None)
        if entry is None:
            errors.append(f"run record {run_record} has no case {p1['id']}")
        else:
            top5 = entry.get("top5") or entry.get("top_5") or entry.get("returned_movie_ids") or []
            outside = [movie_id for movie_id in top5 if movie_id not in sql_union]
            if outside:
                errors.append(f"{p1['id']}: the actual top-5 {outside} is outside the {len(sql_union)}"
                              f"-row union")
    return errors


# --------------------------------------------------------------------------------------
# 16. seed genre counts and director sets
# --------------------------------------------------------------------------------------

def check_seed_sets(queries_doc: dict, routing_doc: dict, mysql: ReadOnlyMysql | None) -> list[str]:
    errors: list[str] = []
    if mysql is None:
        return errors
    annotations = {entry.get("query_id"): entry for entry in routing_doc.get("annotations", [])}
    by_probe = {case.get("probe"): case for case in queries_doc["queries"] if case.get("probe")}

    def seed_of(case: dict) -> dict | None:
        annotation = annotations.get(case["id"]) or {}
        title = (annotation.get("required_slots") or {}).get("seed_title")
        if not isinstance(title, str):
            errors.append(f"{case['id']}: the probe needs a seed_title slot")
            return None
        seed = seed_row(mysql, title)
        if seed is None:
            errors.append(f"{case['id']}: seed title {title!r} is not a single catalog row")
        return seed

    p2 = by_probe.get("P-2")
    if p2 is not None:
        seed = seed_of(p2)
        if seed is not None:
            seed_genres = canonical(genres_of(mysql, seed["id"]))
            if len(seed_genres) != 2:
                errors.append(f"{p2['id']}: the P-2 seed has {len(seed_genres)} canonical genres "
                              f"{seed_genres}, expected exactly 2 (R-2 derives only for 2-genre "
                              f"seeds)")
            decade = seed["year"] // 10 * 10
            expected = ids_filter(mysql, seed_genres, decade, decade + 9, exclude=[seed["id"]])
            if expected != p2["acceptable_movie_ids"]:
                errors.append(f"{p2['id']}: acceptable set != the seed genre × decade set "
                              f"{len(expected)} rows")
            if not 1 <= len(expected) <= 25:
                errors.append(f"{p2['id']}: the derived set has {len(expected)} rows (cap 25)")

    for probe in ("P-3", "P-4"):
        case = by_probe.get(probe)
        if case is None:
            continue
        seed = seed_of(case)
        if seed is None:
            continue
        seed_genres = canonical(genres_of(mysql, seed["id"]))
        if not seed["director"]:
            errors.append(f"{case['id']}: the {probe} seed has no director")
        if probe == "P-3":
            if len(seed_genres) < 3:
                errors.append(f"{case['id']}: the P-3 seed has {len(seed_genres)} canonical genres, "
                              f"expected 3 or more (the ③b2 no-derive boundary)")
        else:
            if len(seed_genres) != 2:
                errors.append(f"{case['id']}: the P-4 seed has {len(seed_genres)} canonical genres, "
                              f"expected exactly 2 (the Q12 shape)")
        if not seed["director"]:
            continue
        expected = ids_filter(mysql, director=seed["director"], exclude=[seed["id"]])
        if expected != case["acceptable_movie_ids"]:
            errors.append(f"{case['id']}: acceptable set != the director's other catalog rows "
                          f"({len(expected)} rows)")
        if not 5 <= len(expected) <= 25:
            errors.append(f"{case['id']}: the director has {len(expected)} other rows, expected 5-25")
    return errors


# --------------------------------------------------------------------------------------
# 17. seed-title independence
# --------------------------------------------------------------------------------------

def check_seed_independence(queries_doc: dict, routing_doc: dict) -> list[str]:
    errors: list[str] = []
    previous = baseline_seed_titles()
    if len(previous) != BASELINE_SEED_COUNT:
        errors.append(f"the three earlier routing files contribute {len(previous)} seed titles, "
                      f"expected {BASELINE_SEED_COUNT}")
    seeds = [((entry.get("required_slots") or {}).get("seed_title"), entry.get("query_id"))
             for entry in routing_doc.get("annotations", [])
             if isinstance((entry.get("required_slots") or {}).get("seed_title"), str)]
    seen: dict[str, str] = {}
    for seed, qid in seeds:
        key = seed.casefold()
        if key in previous:
            errors.append(f"{qid}: seed title {seed!r} repeats an earlier seed")
        if key in seen:
            errors.append(f"{qid}: seed title {seed!r} duplicates {seen[key]}")
        seen[key] = qid
    if not seeds:
        errors.append("the batch has no seed titles (similar cases need one)")
    return errors


# --------------------------------------------------------------------------------------
# 18. read-only self-attestation
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


# --------------------------------------------------------------------------------------
# main
# --------------------------------------------------------------------------------------

def main() -> int:
    global ROOT
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--skip-db", action="store_true",
                        help="file-level checks only (no mysql, no catalog/decade/probe-SQL checks)")
    parser.add_argument("--quiet", action="store_true", help="summary only")
    parser.add_argument("--run-record", type=Path, default=None,
                        help="optional tool_eval run record: assert the R-1 probe's actual top-5 is a "
                             "subset of the union")
    parser.add_argument("--repo-root", type=Path, default=None,
                        help="read the fixtures from another copy of the tree (mutation self-test)")
    args = parser.parse_args()
    if args.repo_root is not None:
        ROOT = args.repo_root.resolve()

    queries_doc = load_json(golden_path("queries-blind.b1-5f.json"))
    routing_doc = load_json(golden_path("query-routing-blind.b1-5f.json"))
    schema_doc = load_json(ROOT / "docs" / "ai" / "tool-schema.b1-5.json")
    mysql = None if args.skip_db else ReadOnlyMysql()

    checks = [
        ("1 structure", lambda: check_structure(queries_doc, routing_doc)),
        ("2 ids", lambda: check_ids(queries_doc, routing_doc)),
        ("3 tracks and text", lambda: check_tracks(queries_doc)),
        ("4 intent quota", lambda: check_intent_quota(queries_doc, routing_doc)),
        ("5 cross marks", lambda: check_cross_marks(queries_doc, routing_doc)),
        ("6 refusal and clarification", lambda: check_answer_shapes(queries_doc, routing_doc)),
        ("7 exclusion", lambda: check_exclusion(queries_doc, routing_doc)),
        ("8 decades and rating coverage", lambda: check_decades(queries_doc, routing_doc, mysql)),
        ("9 alias table", lambda: check_aliases(queries_doc)),
        ("10 bindings and hashes", lambda: check_bindings(queries_doc, routing_doc, schema_doc)),
        ("11 routing annotations", lambda: check_routing(queries_doc, routing_doc, schema_doc)),
        ("12 catalog referential", lambda: check_catalog_referential(queries_doc, routing_doc, mysql)),
        ("13 labelling conventions", lambda: check_conventions(queries_doc)),
        ("14 probe inventory", lambda: check_probe_inventory(queries_doc)),
        ("15 R-1 union reading", lambda: check_r1_union(queries_doc, mysql, args.run_record)),
        ("16 seed sets", lambda: check_seed_sets(queries_doc, routing_doc, mysql)),
        ("17 seed-title independence", lambda: check_seed_independence(queries_doc, routing_doc)),
        ("18 read-only", lambda: check_read_only(mysql)),
    ]
    errors: list[str] = []
    if not args.quiet:
        print(f"blind queries:   {golden_path('queries-blind.b1-5f.json').relative_to(ROOT)}")
        print(f"sha256:          {sha256_of(golden_path('queries-blind.b1-5f.json'))}")
        print(f"blind routing:   {golden_path('query-routing-blind.b1-5f.json').relative_to(ROOT)}")
        print(f"sha256:          {sha256_of(golden_path('query-routing-blind.b1-5f.json'))}")
        print(f"blind_version:   {queries_doc.get('blind_version')} / "
              f"{routing_doc.get('routing_version')}")
        print(f"contract:        {schema_doc.get('contract_version')} / "
              f"{schema_doc.get('schema_hash')}")
    for name, check in checks:
        problems = check()
        if problems:
            errors.extend(f"[{name}] {problem}" for problem in problems)
        if not args.quiet:
            print(f"  check {name}: {'OK' if not problems else f'{len(problems)} problem(s)'}")
    if not args.quiet:
        if mysql is None:
            print("  (--skip-db: decade, rating-coverage, catalog-existence, re-derivation, R-1 "
                  "union and seed-set checks are not exercised)")
        if args.run_record is None:
            print("  (no --run-record: the R-1 probe's actual top-5 ⊆ union is not exercised)")
    if errors:
        print(f"\nFAILED ({len(errors)} problems):")
        for error in errors:
            print(f"  - {error}")
        return 1
    print("\nOK: 30 blind queries, their routing annotations, the exclusions, the shapes, the "
          "labelling conventions, the eight probe groups, the R-1 union reading, the seed sets, "
          "the frozen hashes and the catalog evidence are consistent.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

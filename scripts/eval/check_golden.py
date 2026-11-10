#!/usr/bin/env python3
"""Validate the B1.5 golden set against its JSON Schema and the live catalog snapshot.

Stdlib only (user-approved D-4). Two independent checks:

1. Structural: ``docs/ai/golden/queries.b1-5.json`` is validated against
   ``docs/ai/golden-set.schema.json``; ``docs/ai/golden/negative.b1-5.json`` is
   validated against ``$defs/snapshotRef`` + ``$defs/queryCase`` (see
   ``NEGATIVE_ID_PATTERN`` for the one intentional deviation).
2. Referential: every ``acceptable_movie_ids`` entry must exist in the catalog, and
   the ``snapshot`` block must match the live snapshot (row count, year range, genre
   counts, rows without rating) plus the frozen ``db_snapshot_hash``
   (``docs/ai/README.md`` §2.2, ``snapshot_export_version = 1``).

The script is read-only: it issues ``SELECT`` statements through the ``mysql`` CLI.
It never writes to the database.

Usage
-----
    python3 scripts/eval/check_golden.py                    # schema + DB checks
    python3 scripts/eval/check_golden.py --skip-hash        # skip the 21k-row hash
    python3 scripts/eval/check_golden.py --ids-file ids.txt # validate ids offline
    python3 scripts/eval/check_golden.py --quiet            # summary only

Exit code 0 = every check passed.
"""

from __future__ import annotations

import argparse
import datetime
import hashlib
import json
import os
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SCHEMA_PATH = REPO_ROOT / "docs" / "ai" / "golden-set.schema.json"
QUERIES_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "queries.b1-5.json"
NEGATIVE_PATH = REPO_ROOT / "docs" / "ai" / "golden" / "negative.b1-5.json"

TRACKS = ("zh_zh", "zh_en", "en_en")
EXPECTED_PER_TRACK = 20
EXPECTED_QUERIES = 60
EXPECTED_NEGATIVES = 5
# track -> language of the *query text* (the card language is implied by the track)
TRACK_LANG = {"zh_zh": "zh", "zh_en": "zh", "en_en": "en"}
MIN_CJK_ALIAS_CASES = 3
DECADES = (1960, 1970, 1980, 1990, 2000, 2010, 2020)
# $defs/queryCase fixes ids to Q01..Q60; the negative file is a separate artifact that
# the schema has no root shape for, so its ids are N01..N05 under the same item shape.
QUERY_ID_PATTERN = r"^Q(0[1-9]|[1-5][0-9]|60)$"
NEGATIVE_ID_PATTERN = r"^N0[1-9]$"

# Deliberately prefixed with EVAL_: the repository .env exports FABFLIX_DB_USER/PASSWORD for the
# application, and those must not silently become the evaluation credentials.
DB_HOST = os.environ.get("EVAL_DB_HOST", "127.0.0.1")
DB_PORT = os.environ.get("EVAL_DB_PORT", "3307")
DB_USER = os.environ.get("EVAL_DB_USER", "root")
DB_NAME = os.environ.get("EVAL_DB_NAME", "moviedb")

# docs/ai/README.md §2.2 — snapshot_export_version = 1 (frozen)
SNAPSHOT_EXPORT_SQL = """SELECT m.id, m.title, m.year, m.director,
       (SELECT GROUP_CONCAT(DISTINCT g.name ORDER BY g.name)
          FROM genres_in_movies gm JOIN genres g ON g.id = gm.genre_id
         WHERE gm.movie_id = m.id) AS genres_sorted,
       (SELECT GROUP_CONCAT(s.name ORDER BY s.name)
          FROM (SELECT st.name
                  FROM stars_in_movies sm
                  JOIN stars st ON st.id = sm.star_id
                 WHERE sm.movie_id = m.id
                 ORDER BY st.name
                    LIMIT 3) s) AS stars3_sorted,
       r.rating
  FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id
 ORDER BY m.id"""


class SchemaError(Exception):
    """Raised when the JSON Schema uses a keyword this validator does not implement."""


# --------------------------------------------------------------------------------------
# Minimal JSON Schema validator (only the keywords golden-set.schema.json actually uses)
# --------------------------------------------------------------------------------------

_ANNOTATIONS = {
    "$schema", "$id", "$defs", "title", "description", "$comment", "default",
    "deprecated", "examples", "format",
}
_UNSUPPORTED_HINTS = {"allOf", "anyOf", "if", "then", "else", "patternProperties", "propertyNames"}


def _is_integer(value: object) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _is_number(value: object) -> bool:
    return (isinstance(value, int) and not isinstance(value, bool)) or isinstance(value, float)


def _resolve_ref(root: dict, ref: str) -> dict:
    if not ref.startswith("#/"):
        raise SchemaError(f"only local $ref is supported, got {ref!r}")
    node = root
    for part in ref[2:].split("/"):
        if part not in node:
            raise SchemaError(f"unresolvable $ref {ref!r}")
        node = node[part]
    return node


def validate(instance: object, schema: dict, root: dict, path: str = "$") -> list[str]:
    """Return a list of human-readable violations (empty list = valid)."""
    if "$ref" in schema:
        return validate(instance, _resolve_ref(root, schema["$ref"]), root, path)

    for keyword in _UNSUPPORTED_HINTS:
        if keyword in schema:
            raise SchemaError(f"{path}: schema keyword {keyword!r} is not implemented")

    errors: list[str] = []

    if "oneOf" in schema:
        branches = schema["oneOf"]
        branch_errors = [validate(instance, branch, root, path) for branch in branches]
        matches = [errs for errs in branch_errors if not errs]
        if len(matches) != 1:
            errors.append(f"{path}: matches {len(matches)} of oneOf branches, expected exactly 1")
            if not matches:
                # report the branch that shares the most required keys with the instance,
                # otherwise the diagnostics describe an unrelated root shape
                def overlap(branch: dict) -> int:
                    node = _resolve_ref(root, branch["$ref"]) if "$ref" in branch else branch
                    if not isinstance(instance, dict):
                        return 0
                    return len(set(node.get("required", [])) & set(instance))

                closest = branch_errors[max(range(len(branches)), key=lambda i: overlap(branches[i]))]
                errors.extend(f"{path}: {err}" for err in closest[:10])
    if "const" in schema and instance != schema["const"]:
        errors.append(f"{path}: expected const {schema['const']!r}, got {instance!r}")
    if "enum" in schema and instance not in schema["enum"]:
        errors.append(f"{path}: {instance!r} not in enum {schema['enum']!r}")

    expected_type = schema.get("type")
    if expected_type is not None:
        types = expected_type if isinstance(expected_type, list) else [expected_type]
        ok = False
        for name in types:
            if name == "object" and isinstance(instance, dict):
                ok = True
            elif name == "array" and isinstance(instance, list):
                ok = True
            elif name == "string" and isinstance(instance, str):
                ok = True
            elif name == "integer" and _is_integer(instance):
                ok = True
            elif name == "number" and _is_number(instance):
                ok = True
            elif name == "boolean" and isinstance(instance, bool):
                ok = True
            elif name == "null" and instance is None:
                ok = True
        if not ok:
            errors.append(f"{path}: expected type {expected_type!r}, got {type(instance).__name__}")
            return errors

    if isinstance(instance, str):
        if "minLength" in schema and len(instance) < schema["minLength"]:
            errors.append(f"{path}: shorter than minLength {schema['minLength']}")
        if "maxLength" in schema and len(instance) > schema["maxLength"]:
            errors.append(f"{path}: longer than maxLength {schema['maxLength']}")
        if "pattern" in schema and not re.search(schema["pattern"], instance):
            errors.append(f"{path}: {instance!r} does not match {schema['pattern']!r}")
        if schema.get("format") == "date":
            try:
                datetime.date.fromisoformat(instance)
            except ValueError:
                errors.append(f"{path}: {instance!r} is not an ISO date")

    if _is_number(instance):
        if "minimum" in schema and instance < schema["minimum"]:
            errors.append(f"{path}: {instance} < minimum {schema['minimum']}")
        if "maximum" in schema and instance > schema["maximum"]:
            errors.append(f"{path}: {instance} > maximum {schema['maximum']}")

    if isinstance(instance, list):
        if "minItems" in schema and len(instance) < schema["minItems"]:
            errors.append(f"{path}: {len(instance)} items < minItems {schema['minItems']}")
        if "maxItems" in schema and len(instance) > schema["maxItems"]:
            errors.append(f"{path}: {len(instance)} items > maxItems {schema['maxItems']}")
        if schema.get("uniqueItems"):
            seen: set[str] = set()
            for item in instance:
                key = json.dumps(item, sort_keys=True, ensure_ascii=False)
                if key in seen:
                    errors.append(f"{path}: duplicate item {item!r}")
                seen.add(key)
        if "items" in schema:
            for index, item in enumerate(instance):
                errors.extend(validate(item, schema["items"], root, f"{path}[{index}]"))

    if isinstance(instance, dict):
        properties = schema.get("properties", {})
        for name in schema.get("required", []):
            if name not in instance:
                errors.append(f"{path}: missing required property {name!r}")
        additional = schema.get("additionalProperties", True)
        for name, value in instance.items():
            if name in properties:
                errors.extend(validate(value, properties[name], root, f"{path}.{name}"))
            elif additional is False:
                errors.append(f"{path}: unexpected property {name!r}")
            elif isinstance(additional, dict):
                errors.extend(validate(value, additional, root, f"{path}.{name}"))

    for keyword in schema:
        if keyword in {
            "$ref", "oneOf", "type", "properties", "required", "additionalProperties",
            "items", "minItems", "maxItems", "uniqueItems", "pattern", "minLength",
            "maxLength", "enum", "const", "minimum", "maximum",
        } or keyword in _ANNOTATIONS:
            continue
        raise SchemaError(f"{path}: schema keyword {keyword!r} is not implemented")

    return errors


# --------------------------------------------------------------------------------------
# Database access (read-only SELECT through the mysql CLI)
# --------------------------------------------------------------------------------------

def _read_env_password() -> str:
    """MYSQL_PWD, else MYSQL_ROOT_PASSWORD from the repo .env (git-ignored)."""
    password = os.environ.get("MYSQL_PWD") or os.environ.get("MYSQL_ROOT_PASSWORD")
    if password:
        return password
    env_file = REPO_ROOT / ".env"
    if env_file.is_file():
        for line in env_file.read_text(encoding="utf-8").splitlines():
            if line.startswith("MYSQL_ROOT_PASSWORD="):
                return line.split("=", 1)[1].strip()
    raise SystemExit("no MySQL password: set MYSQL_PWD or MYSQL_ROOT_PASSWORD (or .env)")


class ReadOnlyMysql:
    """Executes a single SELECT via the mysql CLI; refuses anything that is not read-only."""

    def __init__(self) -> None:
        self.password = _read_env_password()

    def select(self, sql: str) -> list[list[str]]:
        statement = sql.strip().rstrip(";")
        if not re.match(r"(?is)^(select|with)\b", statement):
            raise ValueError("only SELECT statements are allowed")
        if ";" in statement:
            raise ValueError("exactly one statement is allowed")
        env = dict(os.environ, MYSQL_PWD=self.password)
        completed = subprocess.run(
            [
                "mysql", "-h", DB_HOST, "-P", str(DB_PORT), "-u", DB_USER,
                "-B", "-N", DB_NAME, "-e", statement,
            ],
            capture_output=True, text=True, env=env,
        )
        if completed.returncode != 0:
            raise SystemExit(f"mysql failed: {completed.stderr.strip()}")
        return [line.split("\t") for line in completed.stdout.splitlines() if line]


# --------------------------------------------------------------------------------------
# Golden set checks
# --------------------------------------------------------------------------------------

def load_json(path: Path) -> dict:
    if not path.is_file():
        raise SystemExit(f"missing file: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def db_snapshot_hash(rows: list[list[str]]) -> str:
    """sha256 over the normalised snapshot export (``docs/ai/README.md`` §2.2, version 1).

    Single implementation shared by this checker and ``embed_eval.py`` so the report, the
    golden-set ``snapshot`` block and the dry-run headprint can never drift apart.
    """
    digests = []
    for rid, title, year, director, genres_sorted, stars3_sorted, rating in rows:
        normalised_rating = "NULL" if rating in ("NULL", "") else f"{float(rating):.1f}"
        line = "\u001f".join([rid, title, year, director, genres_sorted, stars3_sorted,
                              normalised_rating])
        digests.append(hashlib.sha256(line.encode("utf-8")).hexdigest())
    return hashlib.sha256("\n".join(sorted(digests)).encode("utf-8")).hexdigest()


def check_structure(schema: dict) -> list[str]:
    errors: list[str] = []
    queries_doc = load_json(QUERIES_PATH)
    errors.extend(validate(queries_doc, schema, schema, "queries-file"))

    negative_doc = load_json(NEGATIVE_PATH)
    if set(negative_doc) != {"golden_version", "snapshot", "negatives"}:
        errors.append(f"negatives-file: unexpected top-level keys {sorted(negative_doc)}")
    if not re.search(r"^b1-5-negative/v[0-9]+$", negative_doc.get("golden_version", "")):
        errors.append("negatives-file: golden_version must match ^b1-5-negative/v[0-9]+$")
    errors.extend(validate(negative_doc.get("snapshot"), schema["$defs"]["snapshotRef"], schema,
                           "negatives-file.snapshot"))
    item_schema = json.loads(json.dumps(schema["$defs"]["queryCase"]))
    item_schema["properties"]["id"]["pattern"] = NEGATIVE_ID_PATTERN
    # A negative case has no acceptable movie (expects_no_match), so the queryCase
    # minItems=1 is relaxed to 0 here. ``golden-set.schema.json`` defines no root shape
    # for the negative artifact; this relaxation is the only deviation from queryCase.
    item_schema["properties"]["acceptable_movie_ids"]["minItems"] = 0
    for index, case in enumerate(negative_doc.get("negatives", [])):
        errors.extend(validate(case, item_schema, schema, f"negatives-file.negatives[{index}]"))
        if case.get("expects_no_match") is not True:
            errors.append(f"negatives-file.negatives[{index}]: expects_no_match must be true")
    return errors


def check_composition(queries_doc: dict, negative_doc: dict) -> tuple[list[str], str]:
    """Track/intent counts, id uniqueness, track<->lang consistency. Returns (errors, summary)."""
    errors: list[str] = []
    queries = queries_doc["queries"]
    if len(queries) != EXPECTED_QUERIES:
        errors.append(f"expected {EXPECTED_QUERIES} queries, got {len(queries)}")

    per_track = {track: 0 for track in TRACKS}
    per_intent: dict[str, int] = {}
    ids: set[str] = set()
    alias_cases = 0
    for case in queries:
        if case["id"] in ids:
            errors.append(f"{case['id']}: duplicate id")
        ids.add(case["id"])
        per_track[case["track"]] = per_track.get(case["track"], 0) + 1
        per_intent[case["intent"]] = per_intent.get(case["intent"], 0) + 1
        if case["track"] not in TRACK_LANG:
            errors.append(f"{case['id']}: unknown track {case['track']!r}")
        elif case["lang"] != TRACK_LANG[case["track"]]:
            errors.append(
                f"{case['id']}: lang {case['lang']!r} does not match track {case['track']!r} "
                f"(query language {TRACK_LANG[case['track']]!r})"
            )
        if case.get("cjk_alias_only"):
            alias_cases += 1
        if case.get("expects_no_match"):
            errors.append(f"{case['id']}: a positive query must not set expects_no_match")

    for track, count in per_track.items():
        if count != EXPECTED_PER_TRACK:
            errors.append(f"track {track}: expected {EXPECTED_PER_TRACK} queries, got {count}")
    if alias_cases < MIN_CJK_ALIAS_CASES:
        errors.append(f"expected >= {MIN_CJK_ALIAS_CASES} cjk_alias_only cases, got {alias_cases}")
    if len(negative_doc["negatives"]) != EXPECTED_NEGATIVES:
        errors.append(f"expected {EXPECTED_NEGATIVES} negatives, got {len(negative_doc['negatives'])}")

    expected_ids = {f"Q{index:02d}" for index in range(1, EXPECTED_QUERIES + 1)}
    if ids != expected_ids:
        errors.append(f"query ids are not Q01..Q60 (missing {sorted(expected_ids - ids)})")

    summary = " ".join(f"{k}={v}" for k, v in sorted(per_track.items()))
    summary += " | " + " ".join(f"{k}={v}" for k, v in sorted(per_intent.items()))
    summary += f" | cjk_alias_only={alias_cases} negatives={len(negative_doc['negatives'])}"
    return errors, summary


def check_ids_against_db(
    queries_doc: dict,
    negative_doc: dict,
    mysql: ReadOnlyMysql | None,
    ids_file: Path | None,
    quiet: bool,
) -> list[str]:
    errors: list[str] = []
    known: set[str] | None = None
    if ids_file is not None:
        known = {line.strip() for line in ids_file.read_text(encoding="utf-8").splitlines() if line.strip()}
        print(f"id source: {ids_file} ({len(known)} ids)")

    facts: dict[str, tuple[str, str, str, str]] = {}
    if mysql is not None:
        rows = mysql.select(
            "SELECT m.id, m.title, m.year, IFNULL(r.rating,'NULL'), "
            "IFNULL((SELECT GROUP_CONCAT(DISTINCT g.name ORDER BY g.name) FROM genres_in_movies gm "
            "  JOIN genres g ON g.id = gm.genre_id WHERE gm.movie_id = m.id),'') "
            "FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id"
        )
        known = {row[0] for row in rows}
        facts = {row[0]: (row[1], row[2], row[3], row[4]) for row in rows}
        print(f"id source: live catalog ({len(known)} rows)")

    if known is None:
        print("id source: (none) - referential checks skipped")
        return errors

    decades_seen: set[int] = set()
    unrated = 0
    zero_genre = 0
    for case in queries_doc["queries"] + negative_doc["negatives"]:
        missing = [mid for mid in case["acceptable_movie_ids"] if mid not in known]
        if missing:
            errors.append(f"{case['id']}: ids not in snapshot: {missing}")
        if quiet:
            continue
        print(f"  {case['id']} {case['track']:<5} n={len(case['acceptable_movie_ids']):<2} "
              f"{case['intent']:<26} {truncate(case['query'], 42)}")
        for mid in case["acceptable_movie_ids"]:
            if mid not in facts:
                continue
            title, year, rating, genres = facts[mid]
            decade = (int(year) // 10) * 10
            decades_seen.add(decade)
            if rating == "NULL":
                unrated += 1
            if not genres:
                zero_genre += 1
            print(f"      {mid:<10} {year} r={rating:<5} [{genres or 'N/A'}] {truncate(title, 44)}")

    if not quiet:
        print(f"positive ids spanning decades: {sorted(decades_seen)}")
        print(f"positive id rows without rating: {unrated}; without any genre: {zero_genre}")
        absent = [decade for decade in DECADES if decade not in decades_seen]
        if absent:
            print(f"note: no acceptable id in decades {absent}")
    return errors


def check_snapshot_block(queries_doc: dict, negative_doc: dict, mysql: ReadOnlyMysql,
                         verify_hash: bool) -> list[str]:
    errors: list[str] = []
    snapshot = queries_doc["snapshot"]
    if snapshot != negative_doc["snapshot"]:
        errors.append("snapshot block differs between queries and negatives files")

    live = mysql.select(
        "SELECT (SELECT COUNT(*) FROM movies), "
        "(SELECT MIN(year) FROM movies), (SELECT MAX(year) FROM movies), "
        "(SELECT COUNT(*) FROM genres), (SELECT COUNT(DISTINCT name) FROM genres), "
        "(SELECT COUNT(*) FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id "
        " WHERE r.rating IS NULL), "
        "(SELECT MIN(m.year) FROM movies m JOIN ratings r ON r.movie_id = m.id)"
    )[0]
    movies, year_min, year_max, genre_rows, genre_distinct, no_rating, rating_from = live
    live_values = {
        "movies": int(movies),
        "year_min": int(year_min),
        "year_max": int(year_max),
        "genre_rows": int(genre_rows),
        "genre_distinct_names": int(genre_distinct),
        "movies_without_rating": int(no_rating),
        "ratings_covered_from_year": int(rating_from),
    }
    for key, value in live_values.items():
        if key in snapshot and snapshot[key] != value:
            errors.append(f"snapshot.{key}: file says {snapshot[key]}, live DB says {value}")

    if verify_hash:
        rows = mysql.select(SNAPSHOT_EXPORT_SQL)
        computed = db_snapshot_hash(rows)
        print(f"db_snapshot_hash computed={computed}")
        if "db_snapshot_hash" in snapshot and snapshot["db_snapshot_hash"] != computed:
            errors.append(
                f"snapshot.db_snapshot_hash mismatch: file {snapshot['db_snapshot_hash']}, "
                f"computed {computed}"
            )
    return errors


def truncate(text: str, width: int) -> str:
    return text if len(text) <= width else text[: width - 1] + "…"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--skip-hash", action="store_true",
                        help="do not recompute db_snapshot_hash over all 21,087 rows")
    parser.add_argument("--ids-file", type=Path,
                        help="offline id list (one movie id per line) instead of the live catalog")
    parser.add_argument("--skip-db", action="store_true",
                        help="schema/structure checks only (no mysql, no id existence check)")
    parser.add_argument("--quiet", action="store_true", help="per-query listing off")
    args = parser.parse_args()

    schema = load_json(SCHEMA_PATH)
    queries_doc = load_json(QUERIES_PATH)
    negative_doc = load_json(NEGATIVE_PATH)

    errors: list[str] = []
    errors.extend(check_structure(schema))
    composition_errors, composition_summary = check_composition(queries_doc, negative_doc)
    errors.extend(composition_errors)
    print(f"composition: {composition_summary}")

    mysql = None
    if args.ids_file is None and not args.skip_db:
        mysql = ReadOnlyMysql()
    if not args.skip_db:
        errors.extend(check_ids_against_db(queries_doc, negative_doc, mysql, args.ids_file,
                                           args.quiet))
        if mysql is not None:
            errors.extend(check_snapshot_block(queries_doc, negative_doc, mysql,
                                               verify_hash=not args.skip_hash))

    print(f"golden_version:  {queries_doc['golden_version']} / {negative_doc['golden_version']}")
    print(f"sha256 {QUERIES_PATH.relative_to(REPO_ROOT)}:  {sha256_of(QUERIES_PATH)}")
    print(f"sha256 {NEGATIVE_PATH.relative_to(REPO_ROOT)}: {sha256_of(NEGATIVE_PATH)}")

    if errors:
        print(f"\nFAILED ({len(errors)} problems):")
        for error in errors:
            print(f"  - {error}")
        return 1
    print("\nOK: schema, composition, id existence, snapshot block and hashes all valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

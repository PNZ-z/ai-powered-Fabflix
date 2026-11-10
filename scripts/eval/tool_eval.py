#!/usr/bin/env python3
"""B1.5b tool-routing evaluation harness — stage 1 skeleton (stdlib only, D-4).

What this file measures: whether a variant selects the right tool, fills the right slots,
refuses when it must, and keeps arguments inside ``docs/ai/tool-schema.b1-5.json``. It does
**not** measure answer prose quality, latency percentiles or the full 75-case set in this
stage: stage 1 is the skeleton plus a ``--limit``-bounded smoke run, and the full G0/G1/G2 run
is a separate, deliberately approved step (``--allow-full-run`` is required for it).

Variants (plan ``docs/plans/b1-5b-tool-routing.md`` §5)
------------------------------------------------------
``G0``  pure LLM routing: the model picks the tool and the arguments, the server only executes.
``G1``  deterministic routing in front: keyword/dictionary pre-classification, enum mapping,
        coverage check and controlled slot filling, no model call at all.
``G2``  G0's routing plus server-side schema validation and enum/alias mapping (the model still
        chooses; the server rejects or maps). G2 reuses G0's requests, so it never doubles them.
``G3``  **not implemented this period** (``OPEN-b4`` = optional / not required). Asking for it
        exits 2 and says "not measured" instead of silently substituting G2.

Tool execution coverage in stage 1
----------------------------------
``search_movies_by_title`` / ``filter_movies`` / ``get_movie_card`` run read-only SQL so the smoke
run produces real rows and real coverage states. ``find_similar_movies`` and ``recommend_movies``
are recorded but **not executed**: their ranking and ``similarity_reason`` semantics are an
implementation decision that belongs to the production issue, not to this evaluation skeleton.
Cases that need them are stored with ``execution: "deferred"`` and never counted as passed.

``--self-test`` feeds scripted tool calls through the G0/G2 scoring path. It exercises the
plumbing (validation, enum mapping, coverage states, scoring) without a model and is labelled
SELF-TEST: those records are NOT a model measurement.

Usage
-----
    python3 scripts/eval/tool_eval.py --dry-run
    python3 scripts/eval/tool_eval.py --variant G1 --cases tool --limit 3 --ids T01,T11
    python3 scripts/eval/tool_eval.py --variant G0 --cases tool --limit 3 --base-url http://host:11434
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
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from check_golden import REPO_ROOT, ReadOnlyMysql, load_json  # noqa: E402
from check_schema import (  # noqa: E402  (fixtures + shared hash/enum helpers)
    QUERIES_PATH,
    ROUTING_PATH,
    TOOL_CASES_PATH,
    TOOL_SCHEMA_PATH,
    genre_enum,
    schema_hash,
)

DEFAULT_MODEL = "qwen2.5:7b"
DEFAULT_BASE_URL = os.environ.get("EVAL_OLLAMA_BASE_URL", "http://localhost:11434")
DEFAULT_TIMEOUT_S = 180.0
TEMPERATURE = 0
PROMPT_ID = "stage1-inline/v1"
PROMPT_TEXT = (
    "You are the movie catalog assistant of Fabflix. You answer only from the results of the "
    "provided tools and never invent titles, years, directors, genres, stars, ratings or movie "
    "ids. Answer in the language of the user."
)
YEAR_MIN, YEAR_MAX = 196, 2024
RATING_COVERAGE_FROM = 2001
SAMPLED_DEFAULT_LIMIT = 5

# Deterministic routing vocabulary (G1). Chinese genre words must be mapped server-side: the
# measured baseline failure was the model submitting  genre values in Chinese (scout §实测 3).
GENRE_ALIASES = {
    "动作": ["Action"], "action": ["Action"],
    "冒险": ["Adventure"], "adventure": ["Adventure"],
    "动画": ["Animation"], "动画片": ["Animation"], "animation": ["Animation"], "animated": ["Animation"],
    "传记": ["Biography"], "biography": ["Biography"],
    "喜剧": ["Comedy"], "comedy": ["Comedy"], "comedies": ["Comedy"],
    "犯罪": ["Crime"], "crime": ["Crime"],
    "纪录片": ["Documentary"], "documentary": ["Documentary"],
    "剧情": ["Drama"], "drama": ["Drama"],
    "家庭": ["Family"], "family": ["Family"],
    "恐怖": ["Horror"], "horror": ["Horror"],
    "音乐": ["Music"], "musical": ["Music"], "musicals": ["Music"],
    "悬疑": ["Mystery"], "mystery": ["Mystery"],
    "黑色": ["Noir"], "noir": ["Noir"], "film noir": ["Noir"],
    "爱情": ["Romance"], "romance": ["Romance"], "romantic": ["Romance"],
    "科幻": ["Sci-Fi"], "sci-fi": ["Sci-Fi"], "science fiction": ["Sci-Fi"],
    "惊悚": ["Thriller"], "thriller": ["Thriller"], "thrillers": ["Thriller"],
    "西部": ["Western"], "western": ["Western"], "westerns": ["Western"],
}
TRANSACTION_PATTERNS = (r"买.{0,20}票", r"退.{0,4}款", r"下.{0,4}单", r"付.{0,4}款")
REFUSAL_RULES = (
    ("plot", ("剧情", "讲了什么", "讲什么", "故事", "plot", "what is it about", "synopsis")),
    ("transaction", ("买票", "购票", "下单", "支付", "退款", "订单", "ticket", "refund", "checkout")),
    ("internal", ("项目", "开发文档", "用了什么模型", "系统提示", "工具定义", "system prompt",
                  "tool definition", "which model")),
    ("external", ("豆瓣", "imdb", "rotten tomatoes", "烂番茄")),
)
SIMILAR_MARKERS = ("类似", "相似", "像", "like", "similar", "loved", "love ")
RECOMMEND_MARKERS = ("推荐", "recommend", "suggest")
CHINESE_DECADES = {"五十": 1950, "六十": 1960, "七十": 1970, "八十": 1980, "九十": 1990, "二十": 2020}
COVERAGE = {"year_min": YEAR_MIN, "year_max": YEAR_MAX,
            "rating_coverage_from": RATING_COVERAGE_FROM}


class EvalError(Exception):
    """Raised for unrecoverable evaluation errors (endpoint down, broken fixture)."""


# --------------------------------------------------------------------------------------
# Fixtures and cases
# --------------------------------------------------------------------------------------

def load_fixtures() -> tuple[dict, dict, dict]:
    schema_doc = load_json(TOOL_SCHEMA_PATH)
    cases_doc = load_json(TOOL_CASES_PATH)
    routing_doc = load_json(ROUTING_PATH)
    recorded = schema_doc.get("schema_hash")
    recomputed = schema_hash(schema_doc["tools"])
    if recorded != recomputed:
        raise EvalError(f"schema_hash mismatch: recorded {recorded}, recomputed {recomputed}")
    return schema_doc, cases_doc, routing_doc


def build_cases(cases_doc: dict, routing_doc: dict, selection: str,
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
                "expected_behavior": case["expected_behavior"],
                "expected_error_code": case.get("expected_error_code", "NONE"),
                "acceptable_movie_ids": case.get("acceptable_movie_ids", []),
            })
    if selection in ("queries", "both"):
        queries = {query["id"]: query for query in load_json(QUERIES_PATH)["queries"]}
        for entry in routing_doc["annotations"]:
            cases.append({
                "case_id": entry["query_id"],
                "kind": "query",
                "text": queries[entry["query_id"]]["query"],
                "lang": queries[entry["query_id"]]["lang"],
                "intent": entry["intent"],
                "expected_tools": entry["expected_tools"],
                "required_slots": entry["required_slots"],
                "accept_alternative_slots": entry.get("accept_alternative_slots", []),
                "result_constraints": entry.get("result_constraints", {}),
                "expected_refusal": entry["expected_refusal"],
                "expected_behavior": ("refuse_no_tool" if entry["expected_refusal"]
                                      else "call_tools_and_answer"),
                "expected_error_code": "NONE",
                "acceptable_movie_ids": [],
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
    """Map user-language genre words onto the derived enum (server-side alias table)."""
    enum = genre_enum(schema_doc)
    mapped, unmapped = [], []
    for value in values:
        if value in enum:
            mapped.append(value)
            continue
        alias = GENRE_ALIASES.get(value.strip().lower())
        if alias:
            mapped.extend(item for item in alias if item not in mapped)
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
# Tool execution (read-only SQL; two tools deferred, see the module docstring)
# --------------------------------------------------------------------------------------

MOVIE_COLUMNS = """m.id, m.title, m.year, m.director,
        (SELECT GROUP_CONCAT(DISTINCT g.name ORDER BY g.name)
           FROM genres_in_movies gm JOIN genres g ON g.id = gm.genre_id
          WHERE gm.movie_id = m.id) AS genres,
        r.rating"""


class ToolExecutor:
    def __init__(self, mysql: ReadOnlyMysql | None) -> None:
        self.mysql = mysql

    def execute(self, name: str, arguments: dict) -> dict:
        if name in ("find_similar_movies", "recommend_movies"):
            return {"status": "deferred",
                    "reason": "ranking semantics belong to the production issue; not executed in stage 1"}
        if self.mysql is None:
            return {"status": "not_executed", "reason": "--skip-db"}
        if name == "search_movies_by_title":
            return self._search(arguments)
        if name == "filter_movies":
            return self._filter(arguments)
        if name == "get_movie_card":
            return self._card(arguments)
        return {"status": "error", "error": "INVALID_ARGUMENT", "fields": {"field": "tool"}}

    def _rows(self, sql: str) -> list[dict]:
        return [
            {"id": row[0], "title": row[1], "year": int(row[2]), "director": row[3],
             "genres": [g for g in (row[4] or "").split(",") if g],
             "rating": None if row[5] in ("NULL", "") else float(row[5])}
            for row in self.mysql.select(sql)
        ]

    def _search(self, arguments: dict) -> dict:
        query = str(arguments.get("query", "")).replace("'", "''")
        limit = int(arguments.get("limit", SAMPLED_DEFAULT_LIMIT))
        sql = (f"SELECT {MOVIE_COLUMNS} FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id "
               f"WHERE m.title LIKE '%{query}%'")
        if isinstance(arguments.get("year"), int):
            sql += f" AND m.year = {arguments['year']}"
        sql += f" ORDER BY m.title, m.year LIMIT {limit}"
        rows = self._rows(sql)
        return {"status": "EMPTY_RESULT" if not rows else "OK", "results": rows,
                "match": "none" if not rows else "partial"}

    def _filter(self, arguments: dict) -> dict:
        state, meta = coverage_state(arguments)
        if state is not None:
            return {"status": state, "results": [], **meta}
        where = []
        genres = arguments.get("genres") or []
        if genres:
            quoted = ",".join("'" + str(genre).replace("'", "''") + "'" for genre in genres)
            where.append(f"m.id IN (SELECT gm.movie_id FROM genres_in_movies gm JOIN genres g "
                         f"ON g.id = gm.genre_id WHERE g.name IN ({quoted}) "
                         f"GROUP BY gm.movie_id HAVING COUNT(DISTINCT g.name) = {len(genres)})")
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
        movie_id = str(arguments.get("movie_id", "")).replace("'", "''")
        rows = self._rows(f"SELECT {MOVIE_COLUMNS} FROM movies m "
                          f"LEFT JOIN ratings r ON r.movie_id = m.id WHERE m.id = '{movie_id}'")
        if not rows:
            return {"status": "NOT_FOUND", "results": []}
        return {"status": "OK", "results": rows}


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
    lowered = text.lower()
    found: list[str] = []
    for token in sorted(GENRE_ALIASES, key=len, reverse=True):
        if token in lowered:
            for genre in GENRE_ALIASES[token]:
                if genre not in found:
                    found.append(genre)
            lowered = lowered.replace(token, " ")
    return found


def extract_years(text: str) -> dict:
    slots: dict = {}
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
    between = re.search(r"(?:from|between)\s+(\d{4})\s*(?:to|and)\s+(\d{4})", text, re.IGNORECASE)
    if between:
        slots["year_from"], slots["year_to"] = int(between.group(1)), int(between.group(2))
    after = re.search(r"(\d{4})\s*(?:年)?\s*(?:以后|之后|后)", text) or \
        re.search(r"after\s+(\d{4})", text, re.IGNORECASE)
    if after:
        slots.setdefault("year_from", int(after.group(1)))
    if "year_from" in slots and "year_to" in slots and slots["year_from"] > slots["year_to"]:
        slots["year_from"], slots["year_to"] = slots["year_to"], slots["year_from"]
    return slots


def extract_rating(text: str) -> float | None:
    match = re.search(r"([\d.]+)\s*分以上", text) or \
        re.search(r"rated\s+([\d.]+)", text, re.IGNORECASE) or \
        re.search(r"([\d.]+)\s*(?:or higher|\+)", text, re.IGNORECASE)
    if match:
        return float(match.group(1))
    if "高分" in text or "highly rated" in text.lower():
        return 8.0
    return None


def extract_title(text: str) -> str | None:
    quoted = re.search(r"《([^》]+)》", text)
    if quoted:
        return quoted.group(1).strip()
    called = re.search(r"(?:called|叫)\s*([A-Za-z][A-Za-z0-9' .:!/-]*?)(?:\s*的|\s*$|\s*\?|，|。)", text)
    if called:
        return called.group(1).strip()
    starts = re.search(r"starts? with\s+([A-Za-z][A-Za-z0-9' .:!/-]*)$", text, re.IGNORECASE)
    if starts:
        return starts.group(1).strip()
    return None


def route_deterministically(case: dict) -> dict:
    """Return the controlled action for one case: refuse / clarify / tool call (no model)."""
    text = case["text"]
    category = detect_refusal(text)
    if category:
        return {"action": "refuse", "reason": category, "tool_calls": []}
    title = extract_title(text)
    similar = any(marker in text.lower() for marker in SIMILAR_MARKERS)
    if similar and title:
        return {"action": "tools", "tool_calls": [
            {"name": "search_movies_by_title", "arguments": {"query": title}},
        ], "requires_seed": title}
    genres = extract_genres(text)
    years = extract_years(text)
    rating = extract_rating(text)
    if any(marker in text.lower() for marker in RECOMMEND_MARKERS):
        preferences = {}
        if genres:
            preferences["preferred_genres"] = genres
        if "year_from" in years or "year_to" in years:
            preferences["year_range"] = {"from": years.get("year_from", YEAR_MIN),
                                         "to": years.get("year_to", YEAR_MAX)}
        if rating is not None:
            preferences["min_rating"] = rating
        if not preferences:
            return {"action": "clarify", "question": "genre / decade / rating?", "tool_calls": []}
        state, meta = coverage_state(preferences)
        if state == "OUT_OF_RANGE":
            return {"action": "explain_coverage", "error_state": state, "meta": meta,
                    "candidate_arguments": preferences, "tool_calls": []}
        return {"action": "tools", "tool_calls": [
            {"name": "recommend_movies", "arguments": preferences}]}
    filters = {}
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
        return {"action": "tools", "tool_calls": [{"name": "filter_movies", "arguments": filters}]}
    if title:
        return {"action": "tools", "tool_calls": [
            {"name": "search_movies_by_title", "arguments": {"query": title}}]}
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
                    ("preferred_genres", "year_range", "director") else "filter_movies")
            if key == "query":
                tool = "search_movies_by_title"
            arguments = by_tool.get(tool, {})
            actual = arguments.get(key)
            if key in ("genres", "preferred_genres"):
                if not isinstance(actual, list) or {normalized(g) for g in actual} != \
                        {normalized(g) for g in value}:
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
            elif key == "year_range":
                if not isinstance(actual, dict) or actual.get("from") != value["from"] or \
                        actual.get("to") != value["to"]:
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
            elif key == "query":
                if normalized(str(actual)) != normalized(value):
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
            elif key == "min_rating":
                if actual is None or abs(float(actual) - float(value)) > 1e-9:
                    candidate_problems.append(f"{key}: {actual!r} != {value!r}")
            elif actual != value:
                candidate_problems.append(f"{key}: {actual!r} != {value!r}")
        if not candidate_problems:
            return True, []
        problems = candidate_problems
    return False, problems


def score_case(case: dict, called: list[dict], schema_problems: list[str],
               execution: list[dict], behavior: str, error_state: str | None) -> dict:
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
    returned_ids = {row["id"] for item in execution for row in item.get("results", [])}
    accepted_hit = bool(expected_ids & returned_ids) if expected_ids else None
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
        "execution_deferred": deferred,
        "grounding": "not_checked (stage 1)",
        "passed": bool(tools_ok and tools_complete and slots_ok and not schema_problems
                       and behavior_ok and not deferred),
    }


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


def run_case(case: dict, variant: str, client: OllamaClient | None, model: str,
             schema_doc: dict, executor: ToolExecutor) -> dict:
    tools = schema_doc["tools"]
    record: dict = {"case_id": case["case_id"], "variant": variant, "query": case["text"]}
    behavior = "answer"
    error_state = None
    called: list[dict] = []
    execution: list[dict] = []

    if variant == "G1":
        route = route_deterministically(case)
        record["route"] = {key: value for key, value in route.items()
                           if key not in ("tool_calls",)}
        behavior = {"refuse": "refuse", "clarify": "clarify", "tools": "tools",
                    "explain_coverage": "explain_coverage"}[route["action"]]
        called = route["tool_calls"]
        record["latency_ms"] = 0.0
        if behavior == "refuse":
            record["answer"] = f"[refusal: {route['reason']}]"
        elif behavior == "explain_coverage":
            error_state = route["error_state"]
            record["answer"] = (f"[coverage: {route['error_state']} {route['meta']}, "
                                f"args not expressible: {route['candidate_arguments']}]")
    else:
        if client is None:
            raise EvalError("G0/G2 need an Ollama endpoint; pass --base-url")
        g0 = run_g0(case, client, model, tools)
        called = g0["tool_calls"]
        behavior = "tools" if called else "answer"
        record.update({"latency_ms": g0["latency_ms"], "answer": g0["content"],
                       "prompt_eval_count": g0["prompt_eval_count"],
                       "eval_count": g0["eval_count"]})
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

    schema_problems: list[str] = []
    for call in called:
        schema_problems.extend(f"{call['name']}: {problem}" for problem in
                               validate_call(schema_doc, call["name"], call["arguments"]))
        state, _meta = coverage_state(call["arguments"])
        if state:
            error_state = state
        execution.append(executor.execute(call["name"], call["arguments"]))
    record["tool_calls"] = called
    record["execution"] = execution
    if called and error_state is None:
        for item in execution:
            if item.get("status") in ("EMPTY_RESULT", "OUT_OF_RANGE", "NOT_FOUND", "error"):
                error_state = item["status"]
                break
    record.update(score_case(case, called, schema_problems, execution, behavior, error_state))
    return record


# --------------------------------------------------------------------------------------
# Modes
# --------------------------------------------------------------------------------------

def print_dry_run(schema_doc: dict, cases_doc: dict, routing_doc: dict) -> None:
    enum = genre_enum(schema_doc)
    print(f"schema:            {TOOL_SCHEMA_PATH.relative_to(REPO_ROOT)} "
          f"({schema_doc['contract_version']})")
    print(f"schema_hash:       {schema_doc['schema_hash']}")
    print(f"genre enum:        {len(enum)} values")
    print(f"tools:             {[tool['function']['name'] for tool in schema_doc['tools']]}")
    print(f"tool cases:        {len(cases_doc['tool_cases'])} "
          f"({cases_doc['golden_version']})")
    print(f"routing entries:   {len(routing_doc['annotations'])} "
          f"({routing_doc['routing_version']}, golden sha256 "
          f"{routing_doc['annotates']['sha256'][:12]}…)")
    print(f"prompt:            {PROMPT_ID} sha256 "
          f"{hashlib.sha256(PROMPT_TEXT.encode()).hexdigest()[:12]}…")
    print("variants:          G0 pure LLM / G1 deterministic / G2 G0 + server validation "
          "/ G3 not measured")
    print("execution:         search_movies_by_title, filter_movies, get_movie_card (SQL); "
          "find_similar_movies, recommend_movies deferred")
    print("\ncase plan (first 15):")
    for case in build_cases(cases_doc, routing_doc, "both", None)[:15]:
        print(f"  {case['case_id']:<4} {case['kind']:<6} {case['expected_behavior']:<28} "
              f"tools={case['expected_tools']} slots={case['required_slots']}")
    print("\ndry run: no network call, no SQL, no evaluation run.")


def summarise(records: list[dict]) -> dict:
    total = len(records)
    return {
        "cases": total,
        "tools_ok": sum(1 for r in records if r["tools_ok"]),
        "tools_complete": sum(1 for r in records if r["tools_complete"]),
        "slots_ok": sum(1 for r in records if r["slots_ok"]),
        "schema_ok": sum(1 for r in records if r["schema_ok"]),
        "behavior_ok": sum(1 for r in records if r["behavior_ok"]),
        "passed": sum(1 for r in records if r["passed"]),
        "deferred": sum(1 for r in records if r["execution_deferred"]),
    }


def run_self_test(args: argparse.Namespace, schema_doc: dict, cases_doc: dict,
                  routing_doc: dict) -> int:
    print("SELF-TEST: scripted tool calls, not model output; no model was called.")
    cases = [case for case in build_cases(cases_doc, routing_doc, "both", None)
             if case["text"] in SELF_TEST_SCRIPT]
    if len(cases) != len(SELF_TEST_SCRIPT):
        raise EvalError("self-test cases are missing from the fixtures")
    executor = ToolExecutor(None if args.skip_db else ReadOnlyMysql())
    payload = {"mode": "SELF-TEST (scripted, not a model measurement)",
               "schema_hash": schema_doc["schema_hash"], "runs": []}
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
        out_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                            encoding="utf-8")
        print(f"written: {out_path}")
    return 0


def run_variant(args: argparse.Namespace, schema_doc: dict, cases_doc: dict,
                routing_doc: dict) -> int:
    cases = build_cases(cases_doc, routing_doc, args.cases, args.ids)
    if not cases:
        raise EvalError("no case matched the selection")
    if args.limit:
        cases = cases[: args.limit]
    if len(cases) > args.full_run_threshold and not args.allow_full_run:
        raise EvalError(
            f"refusing to evaluate {len(cases)} cases: stage 1 is a smoke run. Use --limit, or "
            f"--allow-full-run once the full run is approved."
        )
    client = None
    if args.variant in ("G0", "G2"):
        client = OllamaClient(args.base_url, timeout_s=args.timeout)
    executor = ToolExecutor(None if args.skip_db else ReadOnlyMysql())
    records = [run_case(case, args.variant, client, args.model, schema_doc, executor)
               for case in cases]
    summary = summarise(records)
    payload = {
        "variant": args.variant,
        "contract_version": schema_doc["contract_version"],
        "schema_hash": schema_doc["schema_hash"],
        "model": args.model if client else None,
        "base_url": args.base_url if client else None,
        "temperature": TEMPERATURE,
        "prompt_id": PROMPT_ID,
        "prompt_sha256": hashlib.sha256(PROMPT_TEXT.encode()).hexdigest(),
        "summary": summary,
        "records": records,
    }
    print(f"variant {args.variant}: {summary}")
    for record in records:
        print(f"  {record['case_id']:<4} passed={record['passed']!s:<5} "
              f"tools={record['tool_calls'] and [c['name'] for c in record['tool_calls']]} "
              f"slots_ok={record['slots_ok']} schema_ok={record['schema_ok']} "
              f"behavior_ok={record['behavior_ok']} error={record['error_state_observed']}")
        if record["slot_problems"]:
            print(f"        slot problems: {record['slot_problems']}")
        if record["schema_problems"]:
            print(f"        schema problems: {record['schema_problems']}")
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
                        help="run scripted tool calls through the G0/G2 path (NOT a model measurement)")
    parser.add_argument("--variant", choices=("G0", "G1", "G2", "G3"), help="variant to run")
    parser.add_argument("--cases", choices=("tool", "queries", "both"), default="tool",
                        help="which fixture to run (default: the 15 tool cases)")
    parser.add_argument("--ids", help="comma-separated case ids, e.g. T01,T11,Q05")
    parser.add_argument("--limit", type=int, default=0, help="evaluate at most N cases")
    parser.add_argument("--allow-full-run", action="store_true",
                        help="allow more cases than --full-run-threshold (approved full runs only)")
    parser.add_argument("--full-run-threshold", type=int, default=10,
                        help="case count above which --allow-full-run is required (default 10)")
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--timeout", type=float, default=DEFAULT_TIMEOUT_S)
    parser.add_argument("--skip-db", action="store_true", help="no SQL; execution is recorded as skipped")
    parser.add_argument("--out", help="write the run record as JSON to this path")
    args = parser.parse_args()

    schema_doc, cases_doc, routing_doc = load_fixtures()
    if args.variant == "G3":
        print("G3 (server-rendered answers + one retry) is out of scope this period "
              "(plan OPEN-b4 = optional/not required): not measured.", file=sys.stderr)
        return 2
    if args.self_test:
        return run_self_test(args, schema_doc, cases_doc, routing_doc)
    if args.variant is None:
        print_dry_run(schema_doc, cases_doc, routing_doc)
        return 0
    return run_variant(args, schema_doc, cases_doc, routing_doc)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except EvalError as error:
        print(f"eval error: {error}", file=sys.stderr)
        sys.exit(2)

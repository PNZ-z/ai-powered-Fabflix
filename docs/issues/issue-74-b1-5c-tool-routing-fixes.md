# B1.5c — B-route evaluation fixes + catalog-validated Chinese title resolution + independent held-out re-measurement（**已批准 / issue [#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74) 已创建 / 待实施**）

> **文档状态：🟢 主 agent 已批准（r3）；GitHub issue [#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74) 已于 2026-11-19 由 `github-operator` 创建（milestone #3、无 labels、state OPEN）；**实施未开始（待实施）**。** 本文件现为 **issue #74 的规范记录**（原发布草稿 `proposed-b1-5c-tool-routing-fixes.md` 已按编号改名，正文内容未改）。oracle 增量复核（`.pi/artifacts/oracle-b1-5c-delta.md`）的 **B-1（主门槛绑定变体）与 C10（别名表来源）已在 r3 落实**；发布前检查清单**已全部完成**，含创建后的回读确认（`closingIssuesReferences` 等价检查为空、#72 仍 OPEN）。本文件**不声称**任何实施/评测已完成，也**不声称** worker 已启动。
>
> | 元数据 | 值 |
> | --- | --- |
> | 文件类型 | **`docs/issues/issue-74-b1-5c-tool-routing-fixes.md`**（issue #74 的规范记录；原发布草稿 `docs/issues/proposed-b1-5c-tool-routing-fixes.md` 已于 2026-11-19 按编号改名，内容未改） |
> | 计划来源 | `docs/plans/b1-5c-tool-routing-fixes.md`（r4：issue #74 已回填；**未实施**，worker 未启动、未获实施许可） |
> | 上级 / 前置 | **issue [#72](https://github.com/PNZ-z/ai-powered-Fabflix/issues/72)（OPEN，B1.5b 评测 G1 82.7% FAIL）**；计划 `docs/plans/b1-5b-tool-routing.md`；报告 `docs/ai/reports/b1-5b-tool-routing.md` |
> | umbrella | `docs/plans/b1-5-movie-retrieval-eval.md` |
> | 关联规范 | `docs/architecture/ai-user-workflows.md`（§3 U1 中文译名策略、§3.1 年份三态、§4.2 `GenreEnum`/多值语义、§1.6 实测回填、§9 `WU-9`） |
> | 目标 milestone | **#3 `Phase 3 — AI-powered platform`**（与 #72 一致） |
> | Labels | **无**（与 #72 一致） |
> | GitHub issue | ✅ **[#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74)**（2026-11-19 创建；state **OPEN**；milestone **#3 `Phase 3 — AI-powered platform`**；**无 labels**；issue 侧 closing 引用检查 = 空） |
> | 与 #72 的关系 | **续做 / 跟进**（用户决策 C8）；正文用 `Refs #72`；**正文不含任何 GitHub closing keyword 与 `#72` 的组合**；#72 保持 OPEN |
> | 关键决策（用户） | **C4 = D**（查询时片名解析，hybrid）；**C5**（中文译名计入分母 + 单列不含数字）；**C8**（新开本 issue）；**C9**（30 条 held-out + Wilson CI） |
> | 关键裁定（主 agent，oracle B-1 / C10） | **主门槛 = held-out `G1-T` 任务级 ≥90%（27/30 全分母）**；held-out `G1` 为 **baseline**（不含中文解析）；in-sample `G1` 仅作前后对照；**C10 = 别名表冻结为空**（不预置 Q21–Q23） |
> | 批准状态 | 🟢 **主 agent 已批准创建（r3）→ issue #74 已创建（2026-11-19）**；**实施未获批准、未开始**（worker 未启动，无脚本/fixture/报告改动，#74 保持 OPEN） |
> | `last_updated` | 2026-11-19（**r4：回填 issue #74 编号/URL/milestone/labels 与创建后回读结果；`## GitHub issue body` 未改**。r3：按 oracle B-1 + C10 修订：`G1-T` 主门槛、`G1` baseline、/30 与 /28 + Wilson CI；`title-aliases.json` 空表、删除 `seed-in-sample` 候选） |
> **发布前检查清单（已全部完成，含创建后的回读确认）**
>
> - [x] oracle 已对计划与本正文做增量复核（`.pi/artifacts/oracle-b1-5c-delta.md`：r2 的 5 P1 + 7 P2 **全部关闭**），B-1 与 C10 两条阻塞已由主 agent 裁定并回填本文件
> - [x] `OPEN-C10` 已裁定：`title-aliases.json` **冻结时为空**，不预置 Q21–Q23 的 3 条已知译名（正文已写空表口径）
> - [x] 主 agent 已明确标记"可发布"（🟢 批准创建）
> - [x] 正文无占位符、无未决二选一、无未经批准的范围/验收承诺
> - [x] 正文**不含** closing keyword + `#72` 组合：只写 `Refs #72` 并保留 "leaves #72 open"
> - [x] 标题/正文未声称任何评测已运行、任何阈值已达标、#72 已关闭
> - [x] **（创建后回读，2026-11-19 完成）** `github-operator` 创建 #74 后由主 agent 回读：issue 侧 closing 引用检查（`closedByPullRequestsReferences`）`totalCount: 0` / `nodes: []` → **空**（GraphQL 的 `Issue` 无 `closingIssuesReferences` 字段，该字段属 `PullRequest`）；**#72 回读仍 OPEN**（milestone #3、labels 空；timeline 仅出现 `cross-referenced` 事件，无 `closed`/`connected` 事件）。证据 = `.pi/artifacts/github-operator-b1-5c-issue.md`；结果已回填 `Internal tracking`

---

## GitHub issue title

```
[B1.5c] Fix evaluation-level tool-routing defects, add catalog-validated Chinese title resolution, and re-measure on an independent held-out set
```

## GitHub issue body

````markdown
## Goal

Close the two gaps left by the B1.5b tool-routing evaluation (`docs/ai/reports/b1-5b-tool-routing.md`, issue #72): its deterministic-routing variant (G1) scored **62/75 = 82.7%**, below the agreed **≥90%** gate, and that number is **in-sample** — the refusal dictionary, genre aliases, decade parsing and seed-resolution rules were all tuned on the same 75 cases.

This issue therefore (1) fixes the **evaluation-level** defects that account for most of the shortfall, (2) adds a **catalog-validated Chinese title resolution** variant (`G1-T`) so a Chinese translated title can be resolved at query time without shipping an external alias dataset, and (3) measures G1 and G1-T again on a **newly frozen, independent 30-case held-out set** that was never used to develop rules. It delivers evaluation code, fixtures, a report and spec updates only.

The **primary result is `G1-T` on the held-out set**: it is the delivery candidate and the variant the ≥90% gate applies to. **`G1` on the held-out set is the baseline** — it performs no Chinese title resolution, so the two Chinese-title cases cannot pass under it (its ceiling is 28/30). In-sample `G1` is reported only as a before/after control and must never be used to claim the gate is met. The report's main table lists `G1` and `G1-T` side by side, labelled "delivery candidate = G1-T; G1 excludes Chinese title resolution", and gives both the 30-case and the 28-case (Chinese titles excluded) denominator with a Wilson interval for each.

**No production code, no `src/main` change, no `pom.xml` or CI change, no MCP server.** The similarity/recommendation executor remains an evaluation stand-in (B1.5b `OPEN-b5`), and the Chinese title resolver built here is an **evaluation stand-in** as well: the production resolver belongs to B-① as a separate component and is not implemented here.

## Background (measured, not assumed)

- B1.5b G1 = **62/75 = 82.7%** (gate ≥90% = ≥68/75, short by 6 cases) while the **routing layer alone was 74/75 = 98.7%** — routing and task success are reported separately. Intent-level: refusals 6/6, clarifications 4/4, multi-slot 5/5, filter 19/20, title 12/15, recommend 9/10, **similar 7/15**. Pure-LLM routing (G0) 52.0%, G2 replay 50.7%, G3 not measured. Grounding 75/75, seed exclusion 15/15, `rating: null` never rendered as 0.
- All 13 G1 failures are deterministic (`model_calls = 0`) and fall into four groups: **8** caused by evaluation stand-in semantics (multi-value `genres[]` matched with OR where the golden predicate is AND, no same-director signal, seed truncated by title search), **3** Chinese translated titles (the catalog has no CJK text at all), **1** genre semantic variant (`Sci-Fi` 457 titles vs `Science Fiction` 2), **1** a grading-scope conflict (a clarification that is correct per rule R2-3 but scores as task failure).
- Verified with read-only SQL on the 21,087-title snapshot: applying the golden `result_constraints` with **AND** reproduces the acceptable set exactly for **11 of the 15** similar cases — **Q13, Q14, Q15, Q33, Q34, Q36, Q37, Q53, Q54, Q55, Q57** — and for **10/10** recommend cases. (Earlier drafts said 12/15; the per-case verification names 11, and no twelfth case can be named. If a twelfth is ever claimed it must be named with its SQL.) But adding a same-director ranking **without** narrowing by the declared constraints first drops the top-5 hit rate of 8 similar cases to 0/3–1/3, so the order (narrow, then rank) is load-bearing, not cosmetic.
- `search_movies_by_title` orders by `m.title, m.year` with no exact > prefix > substring priority, so `Heat` (2 exact rows, 6 prefix, 32 substring) loses its exact match to `A Corner in Wheat`/`Blind Heat`/`Body Heat` inside `limit=10`; the seed then resolves to the 1972 `Heat` and the case fails three ways at once.
- The runner cannot evaluate a second fixture today: `scripts/eval/tool_eval.py`'s `main()` has no `--queries-path`/`--routing-path` (the fixture paths are imported by `build_cases()`), and `--variant` only accepts G0/G1/G2/G3. Both must be added, with the defaults still pointing at the frozen 60-case set.
- Data hygiene limits the held-out design: years are heavily skewed (2000s 9,069 / 2010s 94 / 2020s 7, with 1990s still holding 2,370 real titles), **5 of the 7 titles from the 2020s are synthetic rows injected by `database/dev-seed.sql`** (`tt9000001…tt9000008`, and B1.5b's Q11/Q39/T14 acceptable sets depend on them), director strings are dirty (10,888 distinct values, 1,398 surnames with more than one string, 788 `X.Lastname` abbreviations, 30 containing `~`), and 950 titles are duplicated.
- Alias coverage is thin: genre aliases cover 43 wordforms for only 17 of 56 enum values, with **no** alias at all for `Fantasy` (492 titles), `War` (125), `Sport` (103) or `History` (215), and `musical → Music` wrongly merges `Musical` (139) into `Music` (619). Director normalization cannot simply widen sets: `Spielberg` is 29 titles while `Steven Spielberg` is 5, and Q17's golden set is the 5.
- The year/enum contract is self-contradictory: the fixture sets `maximum: 2024` on year fields, so "2030" is schema-invalid and server validation can only return `INVALID_ARGUMENT`, while the spec requires `OUT_OF_RANGE` + `coverage` for valid-but-out-of-coverage values. B1.5b's G2 case T10 regressed on exactly this conflict.

## Scope

- [ ] **Fix evaluation semantics** in `scripts/eval/tool_eval.py` only: multi-value `genres`/`preferred_genres` under one decided semantics with genre canonicalization applied **before** the AND comparison (a canonical group such as `Sci-Fi`/`Science Fiction` counts once; it must not be expanded into two AND conditions); `find_similar_movies` narrowed by **router-extracted** constraints first, then ranked by same-director > shared-genre count > year distance > rating, with a same-director fallback when no constraints are declared and seed always excluded; `search_movies_by_title` exact > prefix > substring with year/director clues used to disambiguate multi-candidate seeds and a clarification when no clue exists; director normalization limited to same-surname-plus-matching-initial equivalence (`M.Mann` ↔ `Michael Mann`), never a bare-surname expansion.
- [ ] **Keep execution and grading data streams separate**: execution uses only slots/constraints that the router extracts from the query **text**; golden annotations (`required_slots`, `result_constraints`, `acceptable_movie_ids`) are read **only** by `score_case`. A test must prove the execution path never reads an annotation (sentinel/guard).
- [ ] **Move aliases into versioned data files** (`docs/ai/aliases/*.json` with `aliases_version`), covering the four missing genres, splitting `Musical` from `Music`, declaring a canonical group for `Sci-Fi`/`Science Fiction`, and explicitly declaring the 39 long-tail/noise names as unsupported **with an executable assertion** (they must never appear as an alias target or inside a canonical group, and every alias target must be one of the 56 enum values).
- [ ] **Add the `G1-T` variant (catalog-validated Chinese title resolution)** as an evaluation stand-in: (1) verified alias/cache hit, (2) local LLM proposes **1–3 English candidates** as strict JSON at `temperature=0`, (3) **catalog exact/prefix verification decides** which candidate may call `search_movies_by_title`, (4) optional embedding/fuzzy candidates (off by default), (5) otherwise clarify (ask for the English title or list candidates). Rules: the model is called **only** when a title slot contains CJK and no exact catalog hit exists, so English/genre/year/director queries keep `model_calls = 0`; unverified candidates are never treated as the user's title; a timeout or invalid JSON degrades to clarification; results are cached and replayable; the alias table only accepts entries whose target movie id is verified in the catalog, and it ships **frozen empty** (decision C10): entries may only come from catalog-verified resolutions produced at run time, and no known translation — neither the two held-out Chinese titles nor the in-sample Q21–Q23 cases — is pre-seeded as an evaluation seed.
- [ ] **Settle three contract items** and record the outcome in `docs/architecture/ai-user-workflows.md`: (a) multi-value genre semantics plus canonicalization; (b) whether the year bounds move out of the schema fixture so the three error states (`INVALID_ARGUMENT` / `OUT_OF_RANGE` / `EMPTY_RESULT`) are mutually exclusive and reachable — the year fields keep `type: integer` and a lower bound, only the `maximum: 2024` is removed, the server then judges 196–2024 coverage, and `contract_version`/`schema_hash` are bumped with the report stating the numbers are not comparable to the previous contract; (c) whether a clarification that the rules require may count as success via an explicit per-case annotation, reported **alongside** (never instead of) the strict task-success number. The Chinese-title strategy in §3 U1 is rewritten to the resolution-with-verification wording above.
- [ ] **Make the runner able to evaluate the new fixture**: add `--queries-path`/`--routing-path` (defaults unchanged: the frozen 60-case set) and `--variant G1-T`, plus a replay/no-cache option; the report must show the actual `--help` lines and the commands it ran.
- [ ] **Freeze an independent 30-case held-out set** (`H01`–`H30`, 10 `zh_zh` / 10 `zh_en` / 10 `en_en`) in new files, with a routing file bound to them, plus a validator that asserts: no acceptable-id overlap with the existing 75 cases; no seed title reuse; **no synthetic `dev-seed.sql` rows** (`tt9000001…tt9000008`, `Movie1`/`Movie2`) and no 2020s cases; decades dominated by the 1950s–1980s **plus 3–5 cases in the 1990s** (2010s at most 1–2); the unused genres `Fantasy`/`War`/`Sport`/`History` and the unused single-string directors (e.g. `Hitchcock`, `Cukor`, `R.Stevens`, `Curtiz`); at least two duplicate-title disambiguation cases; at least two boundary cases over zero-genre or unrated titles; and **exactly 2 Chinese-title cases that are absent from the alias table** (they test translation generalisation, not the cache).
- [ ] **Label and cross-check** the held-out set: every case carries an executable predicate, row counts, verified ids and a note; a reviewer who did not develop the rules checks at least 10 cases, including every clarification and Chinese-title case.
- [ ] **Re-run** G1 on the 75-case in-sample set (before/after control), G1 on the held-out set (the **baseline**), and **G1-T on the held-out set — the primary gate — with a cold run first (empty alias table, empty cache) and a warm/cached run second**; G2 by replaying G0 calls (`model_calls = 0`); G0 only if a fresh baseline is needed; G3 stays out of scope and is reported as "not measured".
- [ ] **Report** in `docs/ai/reports/b1-5c-tool-routing-fixes.md`: before/after in-sample numbers, the held-out **`G1-T`** number as the primary result and the held-out **`G1`** number as the baseline, **routing success and title-resolution success reported separately**, the wrong-translation rate split into "no catalog candidate" and "resolved to the wrong movie", resolver latency p50/p95, cache hit rate and fallback rate, per-failure attribution, the Chinese-title figure reported both with (30-case denominator) and without (28-case denominator) the two Chinese cases, the Wilson confidence interval for both, the frozen hashes, the run count, and an explicit statement that the executor and the resolver are stand-ins.
- [ ] **Honesty constraints**: no external Chinese alias dataset and no invented Chinese alias mappings are introduced; every alias entry must be verified against the catalog. Annotating the held-out queries' expected results is **expected and required** — it documents what is acceptable (with an executable predicate and SQL-verified ids), it does not manufacture alias data.

## Non-goals

- **No production code**: no `src/main/**`, no `pom.xml`, no CI change. Similarity/recommendation signals stay evaluation-only (`OPEN-b5`).
- **No production title resolver**: resolving Chinese titles in production belongs to B-① as a separate component; here it is an evaluation stand-in, and its numbers are not a production SLA.
- **No MCP server** (decision D-6): nothing is started, nothing is published.
- **No G3** (server-rendered answers plus one retry): optional this period; if not run, it is reported as "not measured", never substituted by G2.
- **No external Chinese/translated-title data source** (Wikidata/TMDB/IMDb akas) and **no hand-fabricated Chinese alias mappings**: an alias table or cache entry is only admissible when its target movie id is verified in the catalog, the alias table ships **frozen empty** (decision C10), and neither the held-out Chinese titles nor the in-sample Q21–Q23 translations are pre-seeded into it. Held-out query annotation by the maintainer is allowed and required.
- **No new external API, no DB writes, no schema migrations.**
- **No changes** to the frozen B1.5a/B1.5b artifacts: `docs/ai/golden/queries.b1-5.json`, `query-routing.b1-5.json`, `tool-cases.b1-5.json`, `docs/ai/reports/b1-5b-tool-routing.md`, `docs/ai/README.md`. The held-out set lives in new files precisely because the routing file is hash-bound to the frozen query set.
- **No threshold, denominator or gate changes**: if the held-out result is below ≥90%, that is reported as a result, not adjusted away.

## Acceptance criteria

| # | Criterion | How it is verified |
| --- | --- | --- |
| 1 | Multi-value genre semantics are implemented as decided (canonicalize first, then AND), and the SQL-verified reproduction holds: acceptable sets match exactly for the **11 named** similar cases (Q13/Q14/Q15/Q33/Q34/Q36/Q37/Q53/Q54/Q55/Q57) and 10/10 recommend cases; no literal expansion of one canonical group into two AND conditions | Fixed assertions + report SQL |
| 2 | Similarity ranking follows the required order (router constraints → same-director/shared-genre/year/rating → same-director fallback → seed excluded), and the "rank without narrowing" counter-example can no longer occur | Assertions + in-sample similar count |
| 3 | Execution never reads golden annotations: `result_constraints`/`required_slots`/`acceptable_movie_ids` reach only `score_case`; a sentinel test proves zero accesses; a query whose text yields no constraints fails or clarifies honestly instead of borrowing the annotation | Sentinel test + code review |
| 4 | Title search prioritises exact > prefix > substring; both `Heat` exact candidates reach the candidate set and the seed no longer resolves to the 1972 title | Assertions + Q36 outcome |
| 5 | Aliases live in versioned data files; `Fantasy`/`History`/`Sport`/`War` are covered; `Musical` and `Music` are separate; `Sci-Fi`/`Science Fiction` form one canonical group; the 39 noise names are declared unsupported **and asserted** absent from alias targets/canonical groups | File review + assertions |
| 6 | Director normalization never *widens* the accepted set: `M.Mann` ≡ `Michael Mann`, but `Mann` ≢ `Michael Mann` and `Spielberg` (29) ≢ `Steven Spielberg` (5) with Q17 asserted explicitly; bare surnames are recall/display only | Assertions + report note |
| 7 | `G1-T` implements the five steps: strict JSON, 1–3 candidates, `temperature=0`, catalog exact/prefix verification before any `search_movies_by_title` call, timeout → clarification, cache + replay; **non-CJK cases assert `model_calls = 0`**; unverified candidates never reach the tool | Assertions + run record |
| 8 | Routing success and resolution success are reported separately; the wrong-translation rate is split into `candidate_reject_rate` and `wrong_movie_rate`; resolver latency p50/p95, cache hit rate and fallback rate are reported; any `wrong_movie_rate > 0` is attributed case by case | Report tables + run record |
| 9 | The held-out runner is executable: `--queries-path`/`--routing-path` exist (defaults still the frozen 60) and `--variant G1-T` runs; the report pastes the actual `--help` lines, the commands and their output | `--help` excerpt + run artefacts |
| 10 | The held-out set exists with ids `H01`–`H30` (10/10/10), its SHA-256 is frozen before the run, and every contamination rule is an assertion that passes — including exactly 2 Chinese-title cases absent from the alias table, 3–5 cases in the 1990s, and no 2020s/synthetic rows | `check_heldout.py` output |
| 11 | **Held-out `G1-T` ≥ 90%** (27/30, full denominator) is the primary result, because `G1-T` is the delivery candidate; **held-out `G1` is reported as the baseline** and is labelled as excluding Chinese title resolution, and in-sample `G1` appears only as a before/after control. The report's main table lists `G1` and `G1-T` side by side and gives both the 30-case and the 28-case (Chinese titles excluded) denominator, each with a Wilson confidence interval | Report main table |
| 12 | The year contract is settled: only `maximum: 2024` is removed (type `integer` and a lower bound stay), `contract_version`/`schema_hash` are bumped, `check_schema.py` distinguishes schema bounds from coverage bounds, and the server returns `OUT_OF_RANGE` outside 196–2024; the report states previous numbers are not comparable | Spec diff + case results |
| 13 | In-sample numbers before and after the fixes are reported side by side with per-failure attribution; G2 is a replay (`model_calls = 0`); G3 and MCP are marked "not measured"/"not applicable" | Report |
| 14 | The report separates in-sample, held-out, stand-in-executor and stand-in-resolver results, and does not use in-sample numbers to claim the gate is met | Report boundaries section |
| 15 | Freeze and reproducibility are recorded: held-out/alias file hashes, run count, `model` tag + digest, prompt hash, and both a cold and a warm (cached) run so the cache is shown not to change the conclusion; cold-start latency reported separately | Report + plan appendix |
| 16 | No production/pom/CI change, no MCP, no DB writes; `ai-user-workflows.md` is updated per its §7.1 triggers (§3 U1, §3.1, §4.2, §1.6, §8 change log, §9 `WU-9` status, `last_updated`) | `git diff --stat` + file review |

## Dependencies

- Read-only access to the Docker MySQL snapshot (`fabflix-mysql`, host port 3307) for verification and coverage metadata; no writes.
- **Ollama with `qwen2.5:7b` is now on the critical path** for `G1-T` (only for the Chinese-title cases); `qwen3-embedding` is optional and off by default. G1 and the deterministic part of the held-out measurement make **zero model calls**.
- The B1.5b golden set and report stay frozen as the in-sample regression baseline.
- The alias-table provenance question is **settled (decision C10)**: `title-aliases.json` is frozen empty, no known translation is pre-seeded, and entries may only come from catalog-verified resolutions produced at run time.
- Oracle risk review of the plan plus an owner decision on the remaining contract items (`OPEN-C1`/`OPEN-C2`/`OPEN-C3`/`OPEN-C6`/`OPEN-C7` are stated in this issue as single recommended options, not open alternatives).

## Risks

- **Gate variant confusion.** The ≥90% gate applies to `G1-T` only; `G1` cannot exceed 28/30 by construction because it does not resolve Chinese titles. The primary number must therefore always be labelled with its variant, and `G1` must never be presented as the deliverable result.
- **The gate is tight.** Two Chinese cases must be resolved through the model path, so ≥90% (27/30) leaves little room, and n=30 gives a wide interval (27/30 → Wilson 95% ≈ [74.4%, 96.5%]). The report must state this rather than imply precision.
- **Rule contamination.** If rules are adjusted after seeing held-out failures, the set is no longer held out. The frozen hash plus a recorded run count is the control; a revised set must be re-versioned and reported as a later run. The primary number must come from a **cold** run (empty cache, empty alias table).
- **Widening a graded set.** Surname-based normalization would merge `Spielberg` (29) into `Steven Spielberg` (5) and silently break Q17; only same-surname-plus-matching-initial equivalence is allowed.
- **Narrow-then-rank order.** Ranking first is measurably worse (8 similar cases drop to 0/3–1/3); this is an implementation constraint, not a preference.
- **Wrong translation.** A candidate can pass prefix verification yet point at the wrong movie; the wrong-movie rate is the red-line metric and must be attributed case by case. If it is unacceptable, the fallback is alias/cache + clarification only, with the model path marked experimental.
- **Model non-determinism and latency.** `temperature=0` is not strictly deterministic and cold-start latency is unmeasured; the cache, replay, recorded model digest and separate cold/warm runs are the controls.
- **Bumping the contract.** Moving the year bound out of the schema fixture breaks hash comparability with the previous report; both versions must be presented.
- **Label bias.** A single labeller removes rule contamination but not labelling bias; reviewer spot-checks and an explicit limitation note are required.
- **Synthetic-data leakage.** The 2020s decade is mostly `dev-seed.sql` rows; held-out cases must not rely on them, and the validator must enforce that.
- **Scope creep.** Production similarity, a production resolver, external Chinese data, MCP and G3 are all out of scope; any of them requires a separate, separately approved issue.

## Notes

- Plan: `docs/plans/b1-5c-tool-routing-fixes.md`. Parent evaluation: issue #72 and `docs/plans/b1-5b-tool-routing.md`. Umbrella: `docs/plans/b1-5-movie-retrieval-eval.md`.
- Tool and routing contracts are specified in `docs/architecture/ai-user-workflows.md` (a spec, not a runtime prompt).
- Gate variant: the **≥90% gate applies to held-out `G1-T`** (the delivery candidate); held-out `G1` is the **baseline** and excludes Chinese title resolution; in-sample `G1` is a before/after control only. The alias table is frozen **empty** and is never seeded with known translations.
- Tracking: `Refs #72`. This issue **leaves #72 open**; whether #72 is closed after this work is a separate decision. Nothing here asserts that the ≥90% gate has been met.
````

## Internal tracking

> **本分区不发布到 GitHub。**

| 项 | 值 |
| --- | --- |
| 来源 | 主 agent 2026-11-19 委派（B1.5c：B 路线修复 + 查询时中文片名解析 + 独立 held-out 重测）；输入 = B1.5b 报告（G1 82.7% FAIL）+ `.pi/artifacts/scout-b1-5c.md`（SQL 实测）+ `.pi/artifacts/oracle-b1-5c-review.md`（5 P1 / 7 P2） |
| 关联计划 | `docs/plans/b1-5c-tool-routing-fixes.md`（r4：issue 已回填；**未实施**，待主 agent 批准实施后交 worker） |
| 前序 issue | **#72**（OPEN）——`docs/issues/issue-72-b1-5b-tool-routing.md`；本 issue 为其**续做**；正文用 `Refs #72`，**不含任何 closing keyword 与 `#72` 的组合** |
| 用户决策 | **C4 = D**（查询时解析：别名/缓存 → 本地 LLM 候选 → 目录校验 → 可选模糊 → 澄清）；**C5**（中文译名计入分母 + 单列 28 分母数字）；**C8**（新开本 issue，`Refs #72`，#72 保持 OPEN）；**C9**（30 条 + Wilson CI） |
| 主 agent 裁定（r3，按 oracle 增量复核） | **B-1**：主门槛 = held-out `G1-T` 任务级 ≥90%（27/30 全分母）；held-out `G1` = baseline（不含中文解析，上限 28/30）；in-sample `G1` = 前后对照；/30 与 /28 + Wilson CI 并列。**C10**：`title-aliases.json` 冻结时**为空**，不预置 Q21–Q23；条目只来自运行期目录校验通过的解析结果。C4=D、C5、C8、C9 与 C1–C3、C6/C7 不变 |
| 目标 milestone | **#3 `Phase 3 — AI-powered platform`** |
| Labels | **无** |
| GitHub issue | ✅ **[#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74)** — 2026-11-19 由 `github-operator` 创建，state **OPEN**，milestone **#3 `Phase 3 — AI-powered platform`**，**无 labels**；本文件档名已于同日改为 `docs/issues/issue-74-b1-5c-tool-routing-fixes.md` |
| 正文发布一致性（回填核实，2026-11-19） | **部分一致，存在 2 处差异（已核实，未修正）**。远端正文（`gh issue view 74 --json body` 只读回读）= 94 行 / 21,839 字符 / sha256 `9226d2e85d4631f6a6c6d7764717119f76e3e1c92be0be457f961eb523197dba`（与 `github-operator` 报告一致）；本地 `## GitHub issue body` 现为 95 行 / sha256 `27706f260c931cb…8d7861f01`。差异仅两句**冗余强调语**：① Scope 的 “Re-run” 条 —— 远端未标 `G1` baseline / `G1-T` primary gate（本地有）；② Notes —— 远端缺 ”Gate variant“ 条（本地有）。**实质未变**：Goal 第 2 段与 AC-11 均已写明“主结果 = held-out `G1-T`；`G1` = baseline；in-sample = 前后对照；/30 与 /28 + Wilson CI”。按本次授权**未编辑远端**，留给主 agent 裁定（接受现状 / 授权 operator 同步） |
| 实施状态 | ❌ **未实施 / 待实施**：worker 未启动；无 `scripts/eval/**`、`docs/ai/**` 变更，无 held-out fixture，无报告；#74 保持 OPEN。本文件**不声称**任何 AC 已满足 |
| 发布正文自检 | 正文只陈述**已实测**事实（B1.5b 报告 + scout SQL）；未声称任何 B1.5c 结果已产生；≥90% 门槛写作既有已批准阈值，且**明确绑定 `G1-T` 为主结果、`G1` 为 baseline**；`year` 边界与多值语义写作**单一推荐方案**而非二选一；别名表写作**冻结为空**（C10），正文无 `seed-in-sample` 选项；外部中文数据与"手工编造别名"明确列为 non-goal；similar 复现数统一为 **11/15 并点名**；正文不含 `Closes`/`Fixes` 等 closing keyword；无占位符、无端点地址、无密钥 |
| **发布后必做（P1-3 复核）** | ✅ **已完成（2026-11-19）**：`github-operator` 创建 #74 后，主 agent 回读确认 issue 侧 closing 引用为空（`closedByPullRequestsReferences` `totalCount: 0`）且 **#72 仍 OPEN**（无 `closed`/`connected` 事件）；正文无 `Closes`/`Fixes` 类关键字。若将来出现误关联，仍应立即编辑正文并复读（不得靠 PR 关联绕过） |
| 实施许可 | ❌ **尚未给出**。主 agent 只批准了“按本文件创建 issue”；worker 需等到主 agent 在计划 §15 记录实施许可并下发交接包后才能启动 |
| 创建者 | `github-operator`（主 agent 已于 r3 标记“可发布”；**已于 2026-11-19 创建 #74**，编号/URL/milestone 由主 agent 回传，docs-writer 已完成改名与回填） |

> **回填记录（r4，2026-11-19）**：本次只改**本地记录**（标题状态行 + 元数据表 + 检查清单最后一项 + `Internal tracking`），**未修改 `## GitHub issue body`**（也未编辑远端正文）。据 `github-operator` 报告（`.pi/artifacts/github-operator-b1-5c-issue.md`）：创建前无 B1.5c 重复 issue、关键词扫描无 closing keyword + `#72` 组合、正文无占位符；远端标题 = `[B1.5c] Fix evaluation-level tool-routing defects, add catalog-validated Chinese title resolution, and re-measure on an independent held-out set`。**另需注意**：本地正文与远端正文在 2 处冗余强调句上不一致（详见上表“正文发布一致性”行），实质验收无条件差异，**待主 agent 裁定**。**不声称** B1.5c 已开始或任何评测数字已产生。

### 待确认项（发布前必须清空；已发布，下表为**实施期待定**状态）

| ID | 待确认 | 归属 | 状态 |
| --- | --- | --- | --- |
| `OPEN-C1` | 多值 `genres[]` = canonicalize + AND；`Sci-Fi`/`Science Fiction` 同一 canonical group（不字面展开） | oracle + 主 agent | 🟡 待复核 |
| `OPEN-C2` | `year` 只去 `maximum: 2024`（保留 `integer` + 下限）→ `b1.5-tools/v3` + hash bump | oracle + 主 agent | 🟡 待复核 |
| `OPEN-C3` | 澄清类判分口径（`allows_clarification`，严格口径仍为主） | oracle + 主 agent | 🟡 待复核 |
| `OPEN-C4` | 中文译名策略 | **用户** | ✅ **D（查询时解析）** |
| `OPEN-C5` | 中文译名计入分母 + 单列不含数字 | 用户/主 agent | ✅ **已定** |
| `OPEN-C6` | 人名归一：只允许"姓同 + 首字母一致"；禁止纯姓氏扩展；Q17 5 vs 29 断言 | oracle | 🟡 待复核（已按 P1-5 收紧） |
| `OPEN-C7` | 别名词表形态：独立 JSON + `aliases_version` + `unsupported` 断言 | 主 agent | 🟡 待确认 |
| `OPEN-C8` | 是否新开 issue | **用户** | ✅ **新开，`Refs #72`** |
| `OPEN-C9` | held-out N = 30 + Wilson CI | 用户/主 agent | ✅ **已定** |
| **`OPEN-C10`** | `title-aliases.json` 是否允许 `seed-in-sample`（预置 Q21–Q23 的 3 条已知译名） | oracle + 用户 → 主 agent 裁定 | ✅ **已关闭：空表**（冻结时 `entries: []`，不预置任何已知译名；条目只由运行期目录校验通过的解析结果产生；`seed-in-sample` 候选已撤销） |
| `OPEN-C11` | 解析成功率/错译率是否设门槛 | 主 agent/用户 | 🟡 待定（建议只披露、不设门槛） |

> `OPEN-C10` 已由主 agent 裁定关闭（空表）。`OPEN-C1`–`OPEN-C3`/`OPEN-C6`/`OPEN-C7`/`OPEN-C11` 在**已发布的 #74 正文中均以单一推荐方案陈述**（不构成二选一），**本次回填未改变它们的任何状态**，仍属实施前的待复核/待确认项（以主 agent 在计划 §14/§15 的最终记录为准）。issue **#74 已创建（OPEN）**，但本文件**不声称**任何 AC 已满足、**不声称**实施已开始或完成、**不声称** #72 已关闭。

# [B1.5d] Blind holdout for the `G1-T` routing variant（**🔴 已执行 / FAIL / 报告完成 / PR 待建**）

> **文档状态：🔴 实施完成 = 报告完成，主门槛 FAIL（25/30 < 27/30）；PR 待建，`#76` 可在 PR 合并后关闭；`#74` 保持 OPEN。** 报告 = `docs/ai/reports/b1-5d-blind-holdout.md`（已完成）；计划 = `docs/plans/b1-5d-blind-holdout.md`（r4 已回填结果）。**本文件不发布新内容、不编辑远端 issue、不含 closing keyword。**
>
> | 元数据 | 值 |
> | --- | --- |
> | 文件类型 | `docs/issues/issue-76-b1-5d-blind-holdout.md`（规范记录；已创建；**`## GitHub issue title` / `## GitHub issue body` 未改动**） |
> | 计划来源 | `docs/plans/b1-5d-blind-holdout.md`（**r4：实施结果回填**，2026-11-20；r3 为批准版） |
> | 前序 | **#74（OPEN，保持 OPEN）** — `docs/issues/issue-74-b1-5c-tool-routing-fixes.md`；报告 `docs/ai/reports/b1-5c-tool-routing-fixes.md` |
> | 目标 milestone | **#3 `Phase 3 — AI-powered platform`** |
> | Labels | **无** |
> | 关联关键词 | 正文用 **`Refs #74`**；**不含任何 closing keyword**（`Closes`/`Fixes`/`Resolves`）与 `#74` 的组合 |
> | 审批状态 | 🟢 **主 agent 已批准发布并创建 #76**（r3；oracle 两轮复核后） |
> | **实施状态** | 🔴 **报告完成 / 主门槛 FAIL**：`G1-T`（冷跑）**25/30 = 83.3%**（Wilson [66.4%, 92.7%]）**< 27/30**；`G1` baseline 23/30；冻结 `2026-11-20T08:47:04Z` < 首次运行 `08:52:34Z`；**PR 待建**；**#76 可在 PR 合并后关闭** |

---

## GitHub issue title

```
[B1.5d] Verify the G1-T routing variant on a blind 30-case holdout that never informed rule development
```

## GitHub issue body

````markdown
## Goal

B1.5c (`docs/ai/reports/b1-5c-tool-routing-fixes.md`, issue #74) reports **`G1-T` 30/30** and **`G1` 28/30** on its 30-case held-out set, but that set is **not blind**: during stage 1c the same fixtures were visible and were used to repair routing vocabulary and disambiguation — the H18 query text was reworded (`悬疑片` → `惊悚片`), and the routing markers for H06/H12/H18/H24/H28 plus H16's year-window narrowing were added **after** those held-out cases had been read. The number is therefore a **post-fix regression on a seen fixture**, not a generalisation estimate (report §0 item 4, §7.1 item 4, §9 `R-8`).

This issue runs the missing check: build a **new 30-case blind holdout** that never informed rule development, freeze its hashes, and measure `G1` and `G1-T` on it **exactly once**. The rules, evaluation code, alias files, contract, thresholds and the golden-set JSON schema are frozen on `main` @ `0761845` and **must not change** as part of this work.

This issue **validates B1.5c's generalisation only**. It adds no feature, fixes no failure it finds, and does not change the OPEN state of #74 — whether #74 is closed is a separate decision taken after the result exists.

## Background (measured, not assumed)

- B1.5c held-out results: `G1-T` **30/30 = 100%** [Wilson 88.6%, 100%], `G1` (baseline, no Chinese resolution) **28/30 = 93.3%**, `G0`/`G2` 14/30 = 46.7%; in-sample `G1` 62/75 → 67/75. All `G1-T`/`G1` runs carried `--assert-no-annotation-read` and `annotation_guard = true`.
- Because `G1` already scores 28/30 on the same set, `G1-T`'s **net** contribution there is the 2 Chinese-title cases, and `G1` clears the same 90% gate on the /30 denominator by itself — so the current numbers cannot separate "the rules generalise" from "this fixture is easy".
- The B1.5c held-out **covers two known risk areas zero times** (§7.1 item 1): "similar" queries whose text declares no genre/year constraint (in-sample Q34/Q53/Q54 still fail because the execution path may not read annotations — `R-2`), and the `Musical`/`Music` alias conflict (`R-1`, Q48). This issue therefore **pre-declares one probe case for each** instead of leaving them unmeasured.
- The catalog is a 21,087-title snapshot (`db_snapshot_hash 8b16c047…f925`, years 196–2024). Years are heavily skewed (2000s 9,069 / 2010s 94 / 2020s 7) and **5 of the 7 titles from the 2020s are synthetic rows** injected by `database/dev-seed.sql` (`tt9000001…tt9000008`) — they must not be used. 950 titles appear more than once, and director strings are dirty (788 `X.Lastname` abbreviations, 30 containing `~`).
- **Ratings only exist from 2001** (`db_snapshot.ratings_covered_from_year = 2001`, 13,082 titles without a rating), so any 1990s case that constrains `min_rating` would be empty by construction rather than by routing behaviour — the checker rejects that shape.
- `title-aliases.json` is frozen empty (`entries: []`, decision C10), so a Chinese title can only be resolved through the LLM-candidate plus catalog-verification path — never through pre-seeded aliases.

## Blind protocol (the point of the issue)

1. **No measurement before freeze.** While the new cases are being authored, `scripts/eval/tool_eval.py` is **never** run against the new fixtures — no variant, no `--ids`/`--limit` sampling, no `--dry-run`. Authoring uses **read-only SQL** against the Docker snapshot plus the frozen artifacts, nothing else.
2. **Quota before sampling.** The track/intent quotas and the required coverage list are fixed in the plan **before the first case is written**; no case is added or dropped because of how it looks like it will score.
3. **Freeze, then run.** **Three** SHA-256 values — the two new fixture files **and the new checker script** — are recorded in the plan appendix and in the run record **before** the first measurement, together with evidence that the first run started after the freeze (commit time, or `git hash-object` blob plus file mtime).
4. **One run per combination.** `G1` runs once, `G1-T` cold runs once, `G1-T` warm runs once (cache consistency, not an independent measurement), plus at most one optional replay. There is no re-run after seeing a failure.
5. **Cold means cold.** The cold run uses its **own** resolver cache file and starts from an absent cache (`cache_state_at_load = "absent"`); the B1.5c cache files are neither read nor moved. One warm-up model request (recorded with its `keep_alive` setting) is made before the cold run and is not counted as a case.
6. **No rule changes afterwards.** A failure is attributed (rule gap / labelling error / known gap `R-1`/`R-2`/`R-4`) and reported as-is. If a fix is warranted, it belongs to a separate **B1.5e** issue with a **different** blind set; this issue's number stays frozen.
7. **Stated limits.** The case author necessarily knows that the rules exist and roughly what they cover; blindness excludes "these cases tuned the rules", not "the author knows the shape of the rules". The author must **not** read the B1.5c failure analysis (§5/§7) and the report must list which files the author did read. n = 30 and single-author bias also remain limitations.

## Scope

- [ ] **Author a new 30-case blind holdout** in new files — `docs/ai/golden/queries-blind.b1-5d.json` (`blind_version: b1-5d-blind/v1`) and `docs/ai/golden/query-routing-blind.b1-5d.json` (its `annotates.sha256` binds the former) — with ids **`B01`–`B30`, 10/10/10** across `zh_zh`/`zh_en`/`en_en`, and intent quota `search_by_title` 5 / `filter_catalog` 9 / `similar_movies` 7 / `recommend_by_preference` 3 / `clarify_or_refuse` 6 (4 refusals + 2 clarifications), 4 of the cases also marked `multi_slot`, exactly 2 marked `cjk_title`.
- [ ] **Pre-declare two known-gap probes inside the 30** (marked in the fixture with a `known_gap` field and a labelling note, never explained after the fact): one `similar_movies` case whose text declares **no** genre/year constraint, whose acceptable set is defined by the seed's own genres × decade (`R-2`; the seed must be an English title so the cold-run model-call budget stays at 2), and one `filter_catalog` case containing `musicals` (`R-1`), annotated on the `Music` reading while the SQL evidence records rows for **both** the `Music` and `Musical` interpretations plus the catalog-wide counts of each (B1.5c measured `Musical` 139 / `Music` 619 — re-verify at labelling time).
- [ ] **Enforce the exclusion and shape rules as assertions**: no query text identical (modulo punctuation) to any of the existing 105 cases (60 frozen queries + 15 tool cases + 30 B1.5c held-out); no shared seed title/seed id; no acceptable-id set identical to an existing set (per-id disjointness as the target, escalated rather than loosened if infeasible); no `dev-seed.sql` synthetic rows; no 2020s case; every acceptable id verified by read-only SQL with the predicate and `rows=` recorded.
- [ ] **Enforce the shape rules the B1.5c fixture got away with**: refusals must be `expects_no_match: true` with an empty acceptable set, `expected_refusal: true` and no expected tools; clarifications must be `allows_clarification: true` with a **non-empty** acceptable set and a title slot; `multi_slot` may only appear on `filter_catalog`/`similar_movies`; `cjk_title` may only appear on an intent whose routing annotation actually has a `query`/`seed_title` slot; no 1990s case may constrain `min_rating` unless it is an explicitly declared empty-result boundary case.
- [ ] **Cover the risks the B1.5c held-out missed**: exactly 2 Chinese-title cases whose translations are absent from the (empty) alias table **and** differ from `肖申克的救赎`/`超人`/`黑客帝国`/`阿甘正传`/`泰坦尼克号`; at least 2 abbreviated/dirty director strings; at least 2 duplicate-title seeds that must be disambiguated by a year or director clue in the query text; at least 2 genre-variant cases (`Sci-Fi`/`Science Fiction` and one of the previously thin genres); at least 2 boundary cases (zero-genre or unrated titles); **3–5 cases entirely in the 1990s**, at most 1 in the 2010s.
- [ ] **Add exactly one read-only checker** — `scripts/eval/check_blind_holdout.py` — that asserts structure, ids, fields, booleans, quotas, the exclusion rules, the 1990s/`min_rating` rule, the refusal/clarification shapes, the `multi_slot`/`cjk_title` ownership rules, the alias table still being empty, the `annotates.sha256` binding, the contract hash, the snapshot hash and catalog referential integrity. It must not import `tool_eval.py`, must not be called by it, and must not be wired into the scoring path. `docs/ai/golden-set.schema.json` is **not** modified (its `$defs` id patterns are `H*`-specific; the new ids are `B*`).
- [ ] **Freeze before measuring** and record the **three** SHA-256 values (queries, routing, checker), the `db_snapshot_hash`, and the per-file hashes proving `scripts/eval/tool_eval.py`, the existing `check_*.py`, `docs/ai/golden-set.schema.json`, `docs/ai/aliases/*`, `docs/ai/tool-schema.b1-5.json` and `contract_version b1.5-tools/v3` are unchanged from `0761845` (`tool_eval.py` must show an **empty diff** against that commit).
- [ ] **Run once, in this order**: `G1` on the blind 30 (`model_calls = 0` per case) → one warm-up model request (recorded, not a case) → `G1-T` cold with `--resolver-cache .pi/artifacts/b1-5d-eval/resolver-cache.json` after `rm -f` on that path (`cache_state_at_load = "absent"`, exactly 2 case-level model calls) → `G1-T` warm on the same cache path (`cache_state_at_load = "loaded"`, 0 model calls). Every `tool_eval.py` run carries `--assert-no-annotation-read`. `G0`, `G2`, `G3` and MCP stay out of scope and are reported as "not measured"/"not implemented".
- [ ] **Prove the cold/warm pair agrees**: for all 30 cases the cold and warm runs must produce identical `task_success` and identical `resolver.chosen_movie_id`; any difference is reported as a cache-consistency failure, not smoothed over. The three B1.5c resolver-cache files must have unchanged hashes and mtimes afterwards.
- [ ] **Report** in `docs/ai/reports/b1-5d-blind-holdout.md`: the blind definition and its limits (including which files the author read); `G1-T` (cold) as the **primary gate** result and `G1` as the **baseline**, side by side; a denominator table with explicit membership for **`/30`** (primary), **`/28` non-CJK**, the **probe-free** denominator and any additional sensitivity, each with a Wilson 95% interval and none of them usable to claim the gate; a separate probe table for the `R-1`/`R-2` cases showing both `R-1` readings; routing success and title-resolution success reported separately with the resolver metrics from B1.5c (parse success, `candidate_reject_rate`, `wrong_movie_rate`, latency p50/p95, cache hits, fallbacks); per-case failure attribution with the classifying note; the run-count table; the frozen hashes, the freeze timestamp, the first-run timestamp, `model` tag + digest, `prompt_sha256`, cache SHA, the warm-up request and its `keep_alive`; and an explicit statement that the executor and resolver are evaluation stand-ins.
- [ ] **Keep the record honest**: no thresholds, denominators, case set or gate variants change; no failed case is dropped; no re-run after a failure; the B1.5c report, plan, fixtures, alias files and issue body are not edited. Only an index row may be appended to `docs/ai/README.md` once the report exists.

## Non-goals

- **No rule, code or data changes**: `scripts/eval/tool_eval.py`, the existing `scripts/eval/check_schema.py` / `check_golden.py` / `check_heldout.py`, `docs/ai/golden-set.schema.json`, `docs/ai/aliases/*.json`, `docs/ai/tool-schema.b1-5.json`, the B1.5a/B1.5b/B1.5c fixtures and any threshold stay byte-identical to `0761845`. The single new file in `scripts/eval/` is the read-only blind-holdout checker.
- **No change to the golden-set JSON schema.** The checker validates the blind files with its own assertions; adding a `B*` `$defs` entry would fork the schema used by the frozen `H*` set.
- **No feature work and no production code**: no `src/main/**`, no `pom.xml`, no CI, no MCP server (D-6), no DB writes, no schema migrations.
- **No `G0`/`G2`/`G3`**: not measured in this issue (owner decision D-3), reported as "not measured".
- **No new Chinese/translated-title data source** and no pre-seeded aliases: `title-aliases.json` stays frozen empty (C10); the two Chinese cases must go through the LLM-candidate + catalog-verification path.
- **No fixing what this issue finds**, including the `R-1` `Musical`/`Music` conflict and the `R-2` unconstrained-similarity gap: any repair is a separate B1.5e issue with a different blind set.
- **No change to #74's state**: the body uses `Refs #74` only; whether #74 is closed is decided after the result exists and is not promised here. This issue itself may be closed when its report is delivered.

## Acceptance criteria

| # | Criterion | How it is verified |
| --- | --- | --- |
| 1 | The blind set exists as `B01`–`B30`, 10/10/10 tracks, with the stated intent quota, 4 `multi_slot` and exactly 2 `cjk_title` cases | Checker output |
| 2 | Every exclusion rule of Scope is an executable assertion and passes, including no 2020s/synthetic rows, per-id disjointness, and the 2 Chinese titles differing from the five previously used ones | Checker output |
| 3 | The required coverage items are met: abbreviated/dirty directors, duplicate-title disambiguation, genre variants, boundary cases, 3–5 cases in the 1990s | Checker output + per-case notes |
| 4 | Shape rules hold: refusals are `expects_no_match: true` with empty acceptable sets and no tools; clarifications are `allows_clarification: true` with non-empty acceptable sets; `multi_slot` only on `filter_catalog`/`similar_movies`; `cjk_title` only where a title/seed slot exists; no 1990s case constrains `min_rating` unless it is a declared empty-result boundary case | Checker output |
| 5 | The two known-gap probes (`R-2` unconstrained similar, `R-1` `musicals`) are pre-declared in the fixture and reported in their own table, with SQL row counts for both `Music` and `Musical` readings; the 90% gate still applies to the full `/30` denominator | Fixture + report probe table |
| 6 | Three SHA-256 values (queries, routing, checker) are recorded **before** the first run, the fixtures and checker are unchanged afterwards, and the report shows the first run started after the freeze | Plan appendix + run record + report timeline |
| 7 | **`G1-T` (cold) ≥ 90% (27/30, full `/30` denominator) is the primary result**; `G1` is the **baseline**; the report's main table lists both, labelled with their variants | Report main table |
| 8 | The report gives `/30`, the `/28` non-CJK sensitivity and the probe-free sensitivity with explicit membership and a Wilson 95% interval each, drops no failing case, and makes **no** gate claim from any denominator other than `/30` | Report denominator table |
| 9 | Every failure is attributed case by case (rule gap / labelling error / known gap `R-1`/`R-2`/`R-4`), and no rule is changed in response | Report failure table |
| 10 | The report defines "blind" operationally, states what it can and cannot exclude, discloses n = 30, single author, the author's prior knowledge of the rules, and **which files the author read** | Report boundaries section |
| 11 | `G1` has `model_calls = 0` for all 30 cases; the cold `G1-T` run has exactly 2 case-level model calls and `cache_state_at_load = "absent"` on `.pi/artifacts/b1-5d-eval/resolver-cache.json`; the warm run has 0 case-level model calls and `cache_state_at_load = "loaded"`; the warm-up request is recorded separately with its `keep_alive` | Run records |
| 12 | Cold and warm runs agree on `task_success` and `resolver.chosen_movie_id` for all 30 cases, and the B1.5c resolver-cache files are byte-identical afterwards | Report consistency table + before/after hashes |
| 13 | Every run has `annotation_guard = true`; the run-count table shows exactly one run per (variant × cache state) combination and no re-run after any failure | Run records + report |
| 14 | `git diff` and per-file hashes show zero change to evaluation code, the existing checkers, the golden-set schema, aliases, contract, thresholds and all earlier fixtures/reports; `tool_eval.py` has an empty diff against `0761845` | Diff + hash comparison against `0761845` |
| 15 | The only new script is the read-only `scripts/eval/check_blind_holdout.py`: it imports no scoring code, is not called by `tool_eval.py`, and writes no files | `git status` + the script's own assertions |
| 16 | The report exists at `docs/ai/reports/b1-5d-blind-holdout.md` with the exact commands, run counts, frozen hashes, freeze/first-run timestamps, model tag + digest, and states `G0`/`G2` as "not measured" | Report |

## Dependencies

- Read-only access to the Docker MySQL snapshot (`fabflix-mysql`, host port 3307, `moviedb`, 21,087 titles) for labelling and verification; no writes.
- Ollama `qwen2.5:7b` for one warm-up request plus the cold `G1-T` run (the 2 Chinese-title cases only); `qwen3-embedding` remains disabled. `G1`, the warm run and any replay make zero model calls.
- The B1.5c artifacts stay frozen as the thing being tested: `tool_eval.py`, the existing checkers, `golden-set.schema.json`, the alias files, contract `b1.5-tools/v3`, and all 105 existing cases.
- Owner decisions D-1…D-7 are already taken (checker-only; both probes included; no G0/G2; #74 stays open; author knowledge accepted and disclosed; `B01`–`B30` / `b1-5d-blind/v1`; per-id disjointness escalated if infeasible) and are recorded in the plan §9.

## Risks

- **The result may well be below the gate.** B1.5c's 30/30 was measured on a fixture it had helped repair; a blind set can legitimately come out lower. That is a result to report, not a reason to adjust the threshold, the cases or the run.
- **Tight n.** Two Chinese cases must be resolved through the model path, so ≥90% leaves three failures of room, and n = 30 gives a wide interval: 27/30 is Wilson 95% ≈ [74.4%, 96.5%] and even 30/30 gives [88.6%, 100%], whose lower bound is below the gate. The report must say so and must not claim "95% confidence of ≥90%".
- **The two probes can be mistaken for generalisation evidence.** Both are pre-declared known gaps: one fails because no text constraint exists to route on (`R-2`), the other because the `Musical`/`Music` split conflicts with the frozen annotation convention (`R-1`). Their failures must be attributed as such, quantified separately, and never used to re-tune rules; the gate still lives on `/30`.
- **Selection bias creeping back in.** Sampling must follow the quota written down first; the coverage list exists specifically so the set is not "shaped like what the rules do well" (B1.5c's similar cases all declared text constraints).
- **Per-id disjointness may be infeasible** for large acceptable sets, since the 75 in-sample cases already use 462 ids. If the assertion cannot hold, that is a decision to escalate, not to soften silently.
- **Cache leakage into the "cold" run.** A stale or shared resolver cache would silently turn the primary figure into a warm figure; the run therefore uses its own cache path, starts from an absent cache, and the B1.5c cache files are checked before and after.
- **Freeze timing.** A fixture or checker edited after the first run voids the blindness; hence three hashes plus commit/blob time evidence. The checker cannot embed its own hash (that is self-referential); its SHA-256 is frozen externally in the plan appendix and the pre-run commit/blob record.
- **Labelling errors** can masquerade as rule failures. Every case carries a SQL predicate and row count; a failure is re-checked against the annotation first, and a corrected note never replaces the original number.
- **Scope creep.** Fixing a discovered failure, adding a variant, running G0/G2 for contrast, editing the schema, or touching #74's state all require a separate decision.

## Notes

- Plan: `docs/plans/b1-5d-blind-holdout.md` (r2). Predecessor: `docs/plans/b1-5c-tool-routing-fixes.md` and its report `docs/ai/reports/b1-5c-tool-routing-fixes.md` (§7.1 item 4, §9 `R-8`).
- Gate variant: the **≥90% (27/30) gate applies to `G1-T` (cold), on the `/30` denominator**; `G1` is the baseline and cannot exceed 28/30 by construction. This issue does not define a new threshold — it reuses the approved one on a new sample.
- Denominators are labelled by membership, not just by number: `/30` primary, `/28` = the two Chinese-title cases removed, the probe-free sensitivity = the two known-gap probes removed. They are not comparable to each other or to B1.5c's `/28`, and none of them but `/30` can carry a PASS claim.
- Contracts and semantics are specified in `docs/architecture/ai-user-workflows.md` (a spec, not a runtime prompt); this issue changes none of them.
- Tracking: `Refs #74`. This issue **leaves #74 open**; nothing here asserts that the ≥90% gate has been met on a blind sample, and nothing here asserts that #74 is done.
````

## Internal tracking

> **本分区不发布到 GitHub。**

| 项 | 值 |
| --- | --- |
| 来源 | 主 agent 2026-11-19 委派：B1.5d = 用新的 blind holdout 验证 B1.5c `G1-T` 的泛化；输入 = B1.5c 报告（§0 第 4 条、§7.1 第 4 条、§9 `R-8`）+ 计划 `docs/plans/b1-5c-tool-routing-fixes.md` §17 |
| 关联计划 | `docs/plans/b1-5d-blind-holdout.md`（**r4：实施结果回填**，2026-11-20） |
| 前序 issue | **#74（OPEN）** — `docs/issues/issue-74-b1-5c-tool-routing-fixes.md`；正文只写 `Refs #74` 并明确 "leaves #74 open" |
| 现状 | **已创建 = [#76](https://github.com/PNZ-z/ai-powered-Fabflix/issues/76)**（milestone #3、无 labels）；实施已完成、报告已完成、主门槛 **FAIL**；**PR 待建**（本轮 docs-writer **未** 建 PR/未提交）；**#76 可在 PR 合并后关闭**；**#74 保持 OPEN** |
| 目标 milestone | **#3 `Phase 3 — AI-powered platform`**（与 #72/#74 一致） |
| Labels | **无**（与 #72/#74 一致） |
| 正文版本 | **r2**（2026-11-19）：按 oracle B1.5d 评审 5 个 P1 + P2 重写 Scope / AC / Risks；`D-1`…`D-7` 已由主 agent 裁定并写入正文（**本次未改正文**） |
| 审批状态 | 🟢 **主 agent 已批准发布**；已由 `github-operator` 创建 #76 |
| **实施状态** | 🔴 **报告完成 / FAIL**（详见下方“实施结果回填”）；**本文件不声称 #76 已关闭**（PR 未建） |

### 决策落点（`D-1`…`D-7` 已裁定，不再是开放项）

| ID | 裁定 | 正文落点 |
| --- | --- | --- |
| `D-1` | **checker-only**：新增只读 `scripts/eval/check_blind_holdout.py`；**不改** `golden-set.schema.json`；不改 `tool_eval.py`/既有 `check_*.py`/别名/契约/阈值 | Scope 第 6 条、Non-goals 第 1–2 条、AC 14/15 |
| `D-2` | 纳入 **1 条 `R-2`（无文本约束 similar）+ 1 条 `R-1`（`Musical`/`Music`）探针**，预声明；主门槛仍 **27/30（`/30`）**；另报去探针敏感性；**不得**用 28 条宣称 PASS | Scope 第 2 条、AC 5/8、Risks 第 3 条 |
| `D-3` | **不跑 `G0`/`G2`**，报告写"未测" | Scope 第 8 条、Non-goals 第 4 条、AC 16 |
| `D-4` | 结果后定 #74 / B1.5e；**B1.5d issue 完成后可关**，**#74 不自动关** | Non-goals 第 7 条、Notes 末条 |
| `D-5` | 接受"生成者知道规则形状"并披露；生成者**不读** B1.5c 失败分析 | Blind protocol 第 7 条、AC 10 |
| `D-6` | **`B01`–`B30`**、`blind_version: b1-5d-blind/v1` | Scope 第 1 条 |
| `D-7` | 逐 id 不重叠不可行 → **上交裁定，不擅自放宽** | Scope 第 3 条、Risks 第 5 条 |

### oracle B1.5d 评审项 → 正文闭合点

| 评审项 | 级别 | 正文落点 |
| --- | --- | --- |
| 缓存/预热未定义 | P1 | Blind protocol 第 5 条、Scope 第 8/9 条、AC 11/12、Risks "Cache leakage" |
| `R-1`/`R-2` 未进 Scope/AC | P1 | Scope 第 2 条、AC 4/5/8、Risks 第 3 条 |
| checker vs schema 冲突 | P1 | Scope 第 6 条、Non-goals 第 2 条、AC 15 |
| 断言不足 | P1 | Scope 第 4 条、AC 4 |
| 冻结范围不足 | P1 | Blind protocol 第 3 条、Scope 第 7 条、AC 6/14 |
| "不改 check_*"表述 / `one shot` / `/28` 混淆 / Wilson / 标注者披露 / issue 关闭语义 | P2 | Non-goals 第 1 条与末条、Blind protocol 第 4/7 条、AC 8/10、Risks 第 2 条、Notes 第 3 条 |

### 正文自检（发布前须重跑）

- [x] 正文只陈述**已落盘事实**（B1.5c 报告数字与 §0/§7.1 边界、目录快照/脏数据/评分覆盖事实）；**未声称**任何 B1.5d 结果已产生
- [x] 正文**不含** `Closes`/`Fixes`/`Resolves` 等 closing keyword；只写 `Refs #74` 与 "leaves #74 open"
- [x] 门槛沿用已批准的 `G1-T` ≥27/30，**未新设**阈值/分母/变体；明确只有 `/30` 可承载 PASS 结论
- [x] 外部中文数据、手工别名、生产代码、MCP、G0/G2/G3、schema 改动均写为 non-goal
- [x] 无端点具体地址（写作 `http://<host>:11434` 的形式）、无密钥、无 `.pi/` 路径承诺（仅记录缓存文件名，属实现细节，不含主机信息）
- [x] oracle 两轮复核 + 主 agent 标记可发布 + 用户授权"开始"；可交 `github-operator` 创建

### 实施结果回填（2026-11-20，r4；**未改正文、未编辑远端 issue**）

| 项 | 值 |
| --- | --- |
| 实施状态 | 🔴 **报告完成 / 主门槛 FAIL** |
| 主门槛结果 | `G1-T`（冷跑）**25/30 = 83.3%**（Wilson [66.4%, 92.7%]）**< 27/30（90%）→ FAIL**；差 **2 条** |
| baseline | `G1` **23/30 = 76.7%**（Wilson [59.1%, 88.2%]）；路由 24/30；`model_calls = 0` |
| 敏感性口径 | `/28` 去 2 条 CJK = 23/28 = 82.1%；`/28` 去 2 条探针 = 25/28 = 89.3%；**两者都不得用于宣称 PASS** |
| 一致性 | 冷/热/回放逐例 `task_success` + `resolver.chosen_movie_id` **完全相同**；冷 `absent`/`model_calls 2`、热 `loaded`/`0`、回放 `replay`/`0` |
| 失败归因（5 条） | `B10` 越界请求未拒绝（真实规则缺口）；`B22` = `R-1`（预声明）；`B23` = `R-2` + `seed_title` 抽取（预声明；`R-2` 本批未被干净测量）；`B26` similar 约束求交致空集（真实执行语义缺口）；`B27` `Documentary` 未抽取（真实 genre 词汇缺口） |
| `G1-T` 净增 | **+2**（`B01`/`B14`，均中文译名）；`G1-T` 失败集是 `G1` 失败集的**真子集**，无回退 |
| 冻结 / 首次运行 | `2026-11-20T08:47:04Z` / `08:52:34Z`（首次运行晚于冻结）；三个冻结 SHA 见计划附录 A；`tool_eval.py` 对 `0761845` **空 diff** |
| 产出 | 报告 = `docs/ai/reports/b1-5d-blind-holdout.md`；计划回填 = `docs/plans/b1-5d-blind-holdout.md` §10 + 附录 A；日志 = `docs/logs/2026-11-20-issue-76-b1-5d-blind-holdout.md` |
| #74 | **保持 OPEN**（本 issue 不关闭 #74，也不声称其达标） |
| PR | **待建**（本轮 docs-writer **未** 提交/推送/建 PR） |
| #76 关闭 | **可在 PR 合并后关闭**；本文件**不声称已关闭**，也**不含** closing keyword |
| 未测项 | `G0`/`G2` = 未测（`D-3`）；`G3` 未测；MCP 未实现；**B-① 未开始** |
| 遗留验证缺口 | `B15d-11` 的“B1.5c 缓存前后 hash 对照”以 mtime 近似满足（回放未显式传 `--resolver-cache`，无写入发生）；其余见报告 §7.6 |

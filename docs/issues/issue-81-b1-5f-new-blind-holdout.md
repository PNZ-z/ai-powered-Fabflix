# [B1.5f] Measure the `G1-T` gate on a new 30-case blind holdout (`C01`–`C30`)（**🔴 已执行 / FAIL / 报告完成 / PR 待建 / GitHub #81 `OPEN`**）

> **状态：已执行完毕，结论 = FAIL，issue 保持 `OPEN` 直至 PR 合并。** 30 条 `C01`–`C30` 已生成并冻结（`2026-11-20T23:52:22Z`），首次运行 `23:54:53Z`，每个 (variant × 缓存) 只跑一次。**主门槛 `G1-T` 冷跑 = 24/30 = 80.0%（Wilson [62.7%, 90.5%]）< 27/30 → FAIL**；`G1` baseline 23/30 = 76.7%。报告 = `docs/ai/reports/b1-5f-new-blind-holdout.md`（实测证据）；计划 `docs/plans/b1-5f-new-blind-holdout.md`（r3：§11 结果回填）。
>
> 本文件是 #81 的规范记录：下方 **`GitHub issue body` 区**为**已发布正文的逐行副本**（本轮 `gh issue view 81 --json body` 实浏与本地 diff **为空**；r2 的 3 处补丁已位于远端，**无需再 `gh issue edit`**）。**`Internal tracking` 区不发布**。正文**不写**任何 closing keyword（`Refs #74` only）；**#74 保持 OPEN**。
>
> | 元数据 | 值 |
> | --- | --- |
> | 文件类型 | `docs/issues/issue-81-b1-5f-new-blind-holdout.md`（规范记录 + 实施回填） |
> | 计划来源 | `docs/plans/b1-5f-new-blind-holdout.md`（r3：已执行 / FAIL / 报告完成） |
> | 目标 milestone | **#3 `Phase 3 — AI-powered platform`** |
> | Labels | **无** |
> | 关联关键词 | 正文用 **`Refs #74`**；**不含**任何 closing keyword |
> | 实施状态 | 🔴 **报告完成 / FAIL / 待 PR**；`G1-T` 冷 **24/30**；6 条失败（`C01`/`C05`/`C12`/`C16`/`C17`/`C18`）；**无规则/别名/契约/阈值改动** |
> | 关闭时机 | **#81 可在 PR 合并后关闭**（本文件不自行关闭、不写 closing keyword）；**#74 保持 OPEN** |
> | 不声称 | 不声称泛化、不声称生产可用、不声称 B-① 可开始；不声称 `Q12` 例外已被复现（`C18` 走 `clarify`）；不声称阈值/分母/用例被调整（均未调） |

---

## GitHub issue title

```
[B1.5f] Measure the G1-T gate on a new 30-case blind holdout (C01-C30) that never informed rule development
```

## GitHub issue body

````markdown
## Goal

Answer the one question B1.5e left open: **does the existing rule set (`main` @ `7915fcd`) meet the `G1-T` gate of ≥ 27/30 on a batch of 30 cases that never informed any rule or annotation decision?**

The batch is written and frozen *before* the first run, run **once** per (variant × cache state), and then only attributed — never used to tune rules. `G1` is the baseline; `G1-T` (cold) on the full `/30` denominator is the gate.

This issue produces a new holdout, one cold measurement, a report, and a documentation record. It deliberately does **not** change any rule, threshold, denominator, fixture, report or contract from B1.5a–B1.5e.

## Background (measured, not assumed)

**Where the project stands.** B1.5e landed in PR #79 (`main` @ `7915fcd`) and closed the five routing/execution gaps that the B1.5d blind holdout exposed (`B10` out-of-catalog refusal, `B22` `R-1` `Music`/`Musical`, `B23` `R-2` plus seed-title extraction, `B26` genre words inside a title, `B27` plural genre nouns). Its evidence is **in-sample only**: `G1` 67/75 → 70/75 (routing 73 → 74/75), self-test 43 → 61 checks, four checkers green, with `Q12` as the single owner-approved regression. B1.5e also stated plainly that generalisation is **still unmeasured** and can only be answered by a fresh blind batch.

**Why a new batch, and why it must be new.** B1.5d's 30 cases are a **seen** fixture the moment their failures informed rule changes (B1.5e changed rules precisely because of `B10`/`B22`/`B23`/`B26`/`B27`); re-running B1.5d would be post-fix regression, not evidence. B1.5c's held-out 30 are likewise seen. The gate therefore needs 30 cases that (a) were authored after the fixes landed, (b) by an author who did not write those fixes, and (c) never had any variant executed against them before they were frozen.

**Baselines that may be cited only as history, never as a gate.** B1.5d `G1-T` (cold) 25/30 = 83.3% → FAIL against ≥27/30; B1.5d `G1` 23/30. B1.5c's own batch (30/30) and B1.5d's post-B1.5e re-run (28/30) are seen fixtures. Snapshot: 21,087 titles, `db_snapshot_hash 8b16c047…f925`, ratings only from 2001, `aliases_version b1-5c-aliases/v2` (the `["Music","Musical"]` group landed in B1.5e).

**Two semantic facts the new batch must respect (both from B1.5e):**

1. `R-1` is a **recall expansion, not an equivalence**: a `musicals` filter must be annotated on the **union** (`Music` ∪ `Musical`). For the 1980–1982 window the union is **13 rows** (`Music` 12 + `Musical` 1 = `RMY15`); the checker records the actual top-5 and asserts it is a subset of that union. Annotating the B1.5d-style 24-row `Music` reading would mis-judge correct behaviour.
2. `R-2` shipped as **③b2**: derived narrowing happens **only** when the seed has **exactly two** canonical genres and the text states no director (whole decade, explicit keys win, no fallback on an empty pool). One-genre seeds and 3+-genre seeds derive nothing; the 3+-genre case is a **designed boundary**, so it is annotated on a set that exists independently of the executor's ordering: a 3+-genre seed **with a known director**, query text asking only for "movies similar to X" (no text constraints), acceptable set = **that director's other catalog rows** (fixed SQL predicate, `rows=`, `5 <= n <= 25`), convention `same director`. The case passes only if the ranked top-5 all fall inside that set — i.e. the ranking (`same_director DESC`) hits without any derived narrowing.

**Why one probe is pre-declared as expected-to-fail.** `Q12` and `Q34` produce byte-identical route artifacts (`{seed_title, result_constraints: {}}`, both seeds with 2 canonical genres) while their frozen annotations disagree (same director vs shared genre × window). A text-only executor cannot satisfy both, so B1.5e accepted `Q12` as a single owner-approved regression. This batch therefore includes **one pre-declared "same director + 2-genre seed + no text constraint" similar case**, expected to fail, purely to reproduce that known-unsatisfiable shape. It is not counted as an *unexpected* failure — but it is still counted in the `/30` denominator and **it consumes one of the three failures the gate tolerates**: with `P-4` failing as declared, only **two** unexpected failures remain. The report must say so in those words; 29/30 with `P-4` failing is a pass that has used two of its three allowed failures, not a 30/30.

## Scope

> Item IDs match the plan (`docs/plans/b1-5f-new-blind-holdout.md`) sections; `P-*` are the mandatory probes.

- [ ] **`S-1` Two new fixtures** — `docs/ai/golden/queries-blind.b1-5f.json` (`blind_version: b1-5f-blind/v1`, `created`, `snapshot`, `quota`, `queries[30]`, **IDs `C01`–`C30`**) and `docs/ai/golden/query-routing-blind.b1-5f.json` (`routing_version: b1-5f-blind-routing/v1`, `annotates.sha256` binding the queries file, `schema_contract` = `b1.5-tools/v3` / `712c0e34…474a`). New prefix is mandatory: the B1.5d checker hard-codes `B01`–`B30`.
- [ ] **`S-2` Quotas (identical to B1.5c/B1.5d so the batches stay comparable, no case reused)** — tracks `zh_zh` 10 / `zh_en` 10 / `en_en` 10; intents `search_by_title` 5 / `filter_catalog` 9 / `similar_movies` 7 / `recommend_by_preference` 3 / `clarify_or_refuse` 6 (4 refusals + 2 clarifications); `multi_slot` exactly **4** (only on `filter_catalog`/`similar_movies`); `cjk_title` exactly **2** (only on an intent with a title slot); decades: **1990s 3–5**, 2010s ≤ 1, **no 2020s**.
- [ ] **`S-3` Mandatory probes (8 groups, 10 of the 30 cases)** —
  **`P-1`** `musicals` filter = `R-1`, annotated on the **union** (13 rows for 1980–1982: 12 `Music` + 1 `Musical`; top-5 ⊆ acceptable set, both readings recorded); **`P-2`** no-text-constraint similar = `R-2`, seed with **exactly 2 canonical genres**, annotated **shared genre × same decade**, English seed title only (a CJK seed would add a third resolver call and break the cold-run budget); **`P-3`** 3+-genre-seed similar **with a known director**, annotated **same director** (acceptable set = that director's other catalog rows, fixed SQL + `rows=`, `5 <= n <= 25`), exercising the ③b2 no-derive boundary; **`P-4`** the pre-declared **same-director + 2-genre seed + no text constraint** probe, marked expected-to-fail, reproducing the `Q12` exception; **`P-5`** out-of-catalog refusals ×**2** (one Chinese **box-office** request, one English), `expects_no_match` + empty acceptable set + no expected tool, **and no `《…》` title inside them**; **`P-6`** similar whose text carries a genre word inside the title, annotated **shared genre × window** with the title's genre word excluded; **`P-7`** one plural genre noun filter (only plural forms already landed); **`P-8`** two brand-new CJK translated titles (not in `title-aliases.json`, which stays `entries: []`, and not among the five Chinese titles earlier batches already used).
- [ ] **`S-4` Labeling convention is a freeze precondition** — every `similar_movies` case's `labeling_note` must name its convention explicitly (`same director` / `shared genre × window` / `hybrid`), with the SQL predicate and `rows=`. A similar case without a stated convention **must not be frozen**: otherwise the next batch reproduces the same undecidable shape.
- [ ] **`S-5` Exclusion** — no query text (normalised), seed title, seed `movie_id`, or acceptable-id set may repeat any of the **135 scored earlier cases** (`queries.b1-5.json` 60 + `tool-cases.b1-5.json` 15 + `queries-heldout.b1-5c.json` 30 + `queries-blind.b1-5d.json` 30; the 5 `negative.b1-5.json` entries are not scored); per-id disjointness against the **718** ids those cases already use is the target reading; no `database/dev-seed.sql` synthetic rows; no 2020s case; 1990s cases must not constrain `min_rating` (ratings start in 2001) unless the case is an explicitly declared empty-result boundary.
- [ ] **`S-6` Frozen before the first run** — three SHA-256 (queries, routing, the new checker) plus a commit SHA (or `git hash-object -w` blob + `stat` mtime) recorded in `.pi/artifacts/b1-5f-freeze.json` with `tool_eval_runs = 0`; the report must show **first-run start time > freeze time**. Missing that evidence voids the batch as blind evidence.
- [ ] **`S-7` Run exactly once per combination** — `G1` (baseline, 0 model calls); `G1-T` **cold** on a dedicated resolver-cache path that is emptied first (`cache_state_at_load = "absent"`, **2** model calls — the two CJK cases); `G1-T` **warm** (`"loaded"`, 0 calls, cache-consistency check, not an independent measurement); optional **replay** (0 calls) which must pass `--resolver-cache` explicitly and only consume the cold-run artifact. One warm-up model request is logged separately. `G0`/`G2`/`G3`/MCP are **not run** and are reported as untested.
- [ ] **`S-8` Denominators, pre-registered** — `/30` (gate), `/28` excluding the two CJK cases, `/28` excluding the `R-1`+`R-2` probes (same member definition as B1.5d's de-probe sensitivity), and optionally `/27` excluding `P-1`/`P-2`/`P-4`. Members are listed explicitly; Wilson 95% intervals are computed; **no `/28` or `/27` figure may be used to claim a PASS**.
- [ ] **`S-9` Checker without touching B1.5d's** — a new read-only `scripts/eval/check_blind_holdout_b15f.py` that imports only the read-only helpers from `check_blind_holdout.py`, re-implements the 12 assertion groups for `C01`–`C30`, and adds four: `labeling_note` conventions, probe-inventory completeness (including the expected-to-fail declaration), the **union** `R-1` reading (13 rows; the B1.5d "24-row `Music`" assertion must **not** be inherited), and seed genre counts. Its exclusion group must cover **all 135** earlier cases. `check_blind_holdout.py` stays byte-identical (`8259cc8d…7caf`); parameterising it is rejected because it would rewrite B1.5d's frozen evidence.
- [ ] **`S-10` Independent author, evidence whitelist** — the batch is authored in a session that did **not** write the B1.5e fixes, may not execute any variant while authoring, and may read only: this issue, the plan, the tool contract and slot vocabulary, the alias files, the existing fixtures (for exclusion/format only), the checkers, the B1.5e report §1/§8/§9/§11 and B1.5e plan §7, and read-only MySQL. It must **not** read the B1.5e report §2–§7 (gap-by-gap changes, `R-1`/`R-2` semantics, in-sample before/after, self-test tables), its diff, `.pi/artifacts/b1-5e/**`, or `tool_eval.py`. The report lists the files actually read; if independence cannot be established, the batch is downgraded to a seen-fixture regression.
- [ ] **`S-11` Report + demo page** — `docs/ai/reports/b1-5f-new-blind-holdout.md` with reproduce commands, run counts, `model` tag + digest, `prompt_sha256`, cache SHAs, warm-up record, the file list the author read, the gate verdict, the denominator table and per-case attribution; plus a static LAN change page (`demo/iterations/b1-5f/index.html`, linked from `demo/index.html`, served by `scripts/demo/serve.sh` on `0.0.0.0:4173`, verified with `curl` from the LAN IP) that shows only aggregate numbers and says plainly that it is not the full application.

## Non-goals

- **No rule change of any kind.** `scripts/eval/tool_eval.py`, `docs/ai/aliases/*`, `check_schema.py`, thresholds and denominators are frozen for this issue. A failure is attributed, not fixed; a fix would need its own issue **and another new batch**.
- **No production change**: no `src/main/**`, no `pom.xml`, no CI, no DB writes, no MCP, no migrations. The similar/recommend executor and the title resolver remain evaluation stand-ins, so derived narrowing exists only in the stand-in — nothing here may be read as production behaviour.
- **No contract or schema change**: `docs/ai/tool-schema.b1-5.json`, `contract_version b1.5-tools/v3`, `schema_hash`, and `docs/ai/golden-set.schema.json` stay byte-identical. Probe declarations live in checker-local fields, not in the shared schema.
- **No edits to any B1.5a–B1.5e fixture, plan, report or checker**, and no re-annotation of `Q12`/`Q48` or any other existing case.
- **B1.5c's held-out set, B1.5d and B1.5e's in-sample numbers are not gates.** They are seen fixtures and may only be cited as history; no `/28` figure may be presented as a pass.
- **No claim of generalisation, production-readiness, or that B-① may start.** n = 30, single annotator, no second reviewer, stand-in executors.
- **No issue-graph changes beyond this issue**: #74 stays OPEN and is referenced with `Refs #74` only; this issue does not close, resolve or supersede it.

## Acceptance criteria

| # | Criterion | How it is verified |
| --- | --- | --- |
| 1 | Both fixtures exist with `C01`–`C30` in order, 10/10/10 tracks, intents 5/9/7/3/6, `multi_slot` 4, `cjk_title` 2, 4 refusals / 2 clarifications | new checker groups 1–6 |
| 2 | All eight probe groups (`P-1`…`P-8`) are present exactly as specified, each carrying its pre-declared reading; `P-4` carries the expected-to-fail declaration | checker groups 13–14 + the probe table in the report |
| 3 | **Every** `similar_movies` case states its labelling convention (`same director` / `shared genre × window` / `hybrid`) with a SQL predicate and `rows=`; a case without one is not frozen | checker group 13 |
| 4 | Exclusion holds against all **135** scored earlier cases (no repeated text, seed title, seed id or acceptable-id set; per-id disjointness against 718 ids as the target reading), no synthetic rows, no 2020s case, 1990s 3–5, 2010s ≤ 1 | checker groups 7–8 |
| 5 | Three SHA-256 are recorded with a commit/blob + mtime, `tool_eval_runs = 0` during authoring, and the report shows first run **after** the freeze | `.pi/artifacts/b1-5f-freeze.json` + report timeline |
| 6 | **Gate**: `G1-T` (cold) scores **≥ 27/30 task-level** on `/30`; `G1` is reported as the baseline; cold run has `model_calls = 2`, warm run `0`, and cold/warm agree case by case; `annotation_guard` is true for every run | run artifacts + report main table |
| 7 | The denominator table lists `/30`, `/28 (non-CJK)`, `/28 (de-probe)` and optionally `/27`, names their members, and gives computed Wilson 95% intervals; no `/28`/`/27` figure is used to claim a PASS | report §denominators |
| 8 | Every failure is attributed per case (rule gap / annotation problem / pre-declared probe), annotation problems keep their original number with a separate corrected note, and **no rule was changed and nothing was re-run after seeing results** | report failure table + run-count table |
| 9 | Zero change to `tool_eval.py`, the alias files, `tool-schema.b1-5.json`/`schema_hash`, `golden-set.schema.json`, the existing `check_*.py` (including `check_blind_holdout.py` = `8259cc8d…7caf`) and every B1.5a–B1.5e fixture, plan and report; the only new script is the B1.5f checker | `git diff --stat` + per-file hashes |
| 10 | The report states that `R-1` is a recall expansion (union annotation, not equivalence), that 3+-genre seeds intentionally derive nothing, and that the same-director probe is a known-unsatisfiable shape rather than a new defect | report wording check |
| 11 | The report explicitly says generalisation is claimed **only** for these 30 cases, that the derived-narrowing semantics live in the evaluation stand-in, that B-① is not started, and that #74 remains OPEN | report conclusion check |
| 12 | The LAN change page exists, is linked as the latest iteration, is reachable from the LAN IP at port 4173, shows only aggregate numbers, and states that it is not the full application | demo files + `curl` output |

## Dependencies

- The owner-approved semantics this batch measures, all already landed and **not** to be re-opened here: the `["Music","Musical"]` canonical group with `aliases_version b1-5c-aliases/v2` (recall expansion, union annotation), `R-2` as ③b2 (derive only for exactly-two-genre seeds with no stated director; no fallback on an empty pool), and `Q12` as the single approved regression.
- Read-only MySQL (`fabflix-mysql`, host 3307, `moviedb`) for every acceptable set, row count and seed-genre check, and Ollama `qwen2.5:7b` for the single warm-up plus the two CJK resolver calls.
- `main` @ `7915fcd` (B1.5e merged) as the frozen baseline whose behaviour is being measured.
- An authoring session independent of the B1.5e fixes, and a `check_blind_holdout_b15f.py` free of `tool_eval` imports (assertions only, never in the scoring path).

## Risks

- **The gate may fail.** Given B1.5d's 25/30 and B1.5e's in-sample gains, 25–28/30 is a realistic outcome. A failure is reported as-is with per-case attribution; rules, cases, thresholds and denominators are not adjusted and the batch is not re-run. Any fix then needs a new issue and **yet another** fresh batch.
- **Probe load reduces statistical power.** 10 of 30 cases are pre-declared probes; `P-4` is expected to fail and **still consumes one of the three allowed failures** (only two unexpected failures remain). This is disclosed up front; the gate still runs on `/30`.
- **Author contamination.** Knowing the probe inventory is unavoidable (it is published here), so blind quality rests on the author not having written the fixes and not reading the fix details or artifacts. The read-file list is published; if independence cannot be shown, the batch is downgraded.
- **Exclusion feasibility.** 718 acceptable ids are already in use, so per-id disjointness is harder each round. If it proves impossible it goes to the owner for a ruling; the checker offers no opt-out flag.
- **Annotation ambiguity on 3+-genre seeds.** "Ranked, not narrowed" is the correct reading of ③b2, but annotating it as "should narrow" would make the case fail by construction. The convention must be settled before freezing.
- **CJK budget.** Only two cases may carry a `《…》` title; a title smuggled into a refusal or clarification case would trigger a third resolver call and break the pre-registered cold-run budget.
- **Fingerprints differ from B1.5d.** The alias file and evaluation code changed in B1.5e, so this batch's numbers are not directly comparable with B1.5d's; per-file hashes are recorded and comparisons are limited to "history".
- **A duplicated checker can drift.** The new script repeats assertion logic by design (to keep B1.5d's frozen evidence intact), so mutation checks must prove each new assertion actually fails when broken.
- **The demo page can be mistaken for product evidence.** It carries aggregate numbers only, never DB hosts, credentials, endpoints or raw ids, and says explicitly that it is not the full application.

## Notes

- Plan and full decision log: `docs/plans/b1-5f-new-blind-holdout.md` (r2: oracle P1-1/P1-2 addressed; pending oracle quick re-check and final owner approval). Its remaining open items (`OI-f1`…`OI-f3`) are internal and are not part of this issue's scope commitment; `OD-f1`…`OD-f5` are already decided and recorded there.
- Predecessors: `docs/plans/b1-5d-blind-holdout.md` (blind discipline `BL-1`…`BL-7`, quotas, exclusion, freeze and run templates) and `docs/plans/b1-5e-routing-gap-fixes.md` §7 (the probe inventory this batch inherits).
- `Refs #74`: the `R-1`/`R-2` semantics measured here originate there. This issue does not close, resolve or supersede #74 and makes no claim about its gate.
- Nothing in this issue asserts that any measurement has been taken yet; the batch is not generated, not frozen and not run at the time of filing.
````

## Internal tracking

> 以下内容**不发布**到 GitHub。

| 项 | 值 |
| --- | --- |
| 来源 | 主 agent 交办（B1.5f = 新 30 条 blind holdout 验证 B1.5e 修复；新前缀 `C01`–`C30`、新 `blind_version`、生成者独立、只跑一次）；用户已授权创建 issue（"可以"） |
| 关联计划 | `docs/plans/b1-5f-new-blind-holdout.md`（**r3：已执行 / FAIL / 报告完成；§11 = 结果回填**） |
| 关联 issue | **GitHub [#81](https://github.com/PNZ-z/ai-powered-Fabflix/issues/81) — `OPEN`，milestone #3，无 labels**；正文 = `Refs #74`（**#74 保持 OPEN**，无 closing keyword）；#76/#78 已随各自 PR 关闭。**r3 实测：远端正文与本地正文区逐行 diff 为空，附录 D 的 3 处补丁已位于远端（无需再 `gh issue edit`）** |
| 目标 milestone | **#3 `Phase 3 — AI-powered platform`** |
| Labels | **无** |
| 实施状态 | 🔴 **报告完成 / FAIL / PR 待建；可在 PR 合并后关闭**。实测证据：`.pi/artifacts/b1-5f-freeze.json`（`tool_eval_runs = 0`、三 SHA）、`.pi/artifacts/b1-5f-eval/*.json` + `run-timeline.txt`、`shasum -a 256`（三 SHA 与冻结值一致）、`git diff --stat 7915fcd -- scripts/eval docs/ai` = **空**。数字：`G1` 23/30、**`G1-T` 冷 24/30 → FAIL**、热/回放 24/30 逐例一致；6 条失败 `C01`/`C05`/`C12`/`C16`/`C17`/`C18` |
| 审批状态 | 🟢 发布已发生（#81 存在，`gh issue view 81` 可复核）；本地文档 = **r3 实施回填完毕**；**正文无待更正项**（附录 D 保留作历史） |
| 证据基础 | `git log`（`7915fcd`、PR #79、`bd7e285`）；`docs/ai/reports/b1-5e-routing-gap-fixes.md` §1/§8/§9/§11；`docs/plans/b1-5e-routing-gap-fixes.md` §7.1/§7.3；`docs/plans/b1-5d-blind-holdout.md` §2–§7/§10；`docs/ai/reports/b1-5d-blind-holdout.md` §0/§1.4；`scripts/eval/check_blind_holdout.py`（`:84-130`、`:257-263`、`:486-546`、`:610-645`；SHA `8259cc8d…7caf`）；本轮实读四个 golden 文件（60/15/30/30 = **135** 计分用例）、可接受 id 并集 **718**、三个 routing 文件的 **29 个不同 `seed_title`**；`gh issue view 74`（OPEN / milestone #3）、`gh issue view 81`（OPEN / milestone #3） |
| 已裁定 | ① 排除基线 = **135 条计分 fixture + 5 条未评分 negatives**（165 作废）；② `P-3` = 3+ canonical genres + 已知导演，可接受集 = 同导演其他目录行（固定 SQL + `rows=`，`same director`）；③ `P-4` 留在 `/30`，预期失败**占 3 条容错中的 1 条**（只剩 2 条非预期失败空间）；④ 批准新增只读 B1.5f checker（B1.5d checker byte-identical）+ 变异自测 ≥5；⑤ `/27` 仅在报告可选输出且成员逐条列名，不得宣称 PASS；⑥ `P-1` top-5 ⊆ 13 行 union 为 checker 硬断言；⑦ 探针形状知识必须披露（n=30、单人标注） |
| 不发布内容 | 本节、计划 §9 的 `OD-f*`、内部行号与任何未验证数字；正文中不得出现 `.pi/` 运行产物路径以外的内部证据 |

---

## 附录 D — #81 正文更正记录（**r3 实测：已全部生效**；此节仅作历史保留）

> **应用状态（r3 实测，2026-11-20）**：`gh issue view 81 --json body` 与本地 `GitHub issue body` 区逐行 `difflib` diff **为空**；校验计数：`ranked, not narrowed` = **0**、`same director` = **6**、`165` = **0**、`two unexpected failures` = **1**、`consumes one of the three` = **2**、`pending oracle review` = **0**、`(r1` = **0**、`OI-f1` = **1**、`Closes`/`Fixes`/`Resolves` = **0**。
>
> 即 D-1/D-2/D-3 **三处均已应用于远端 #81**，**无需再 `gh issue edit`**；下面的 BEFORE/AFTER 保留为历史补丁记录（不再作为待办）。P-1 窗口（1980–1982 / 13 行）在远端正文中也已是新值（`1948` 计数 0、`1980` 计数 2、`13 rows` 计数 3）。
> 本节位于 `Internal tracking` 之后，**不发布**到 GitHub。

### D-1 Background 第 2 条末句（P1-1 残留：把 `P-3` 说成 "ranked, not narrowed"）

**BEFORE（与 #81 正文逐字一致）**

~~~text
2. `R-2` shipped as **③b2**: derived narrowing happens **only** when the seed has **exactly two** canonical genres and the text states no director (whole decade, explicit keys win, no fallback on an empty pool). One-genre seeds and 3+-genre seeds derive nothing; the 3+-genre case is a **designed boundary**, so it is annotated on a set that exists independently of the executor's ordering: a 3+-genre seed **with a known director**, query text asking only for "movies similar to X" (no text constraints), acceptable set = **that director's other catalog rows** (fixed SQL predicate, `rows=`, `5 <= n <= 25`), convention `same director`. The case passes only if the ranked top-5 all fall inside that set — i.e. the ranking (`same_director DESC`) hits without any derived narrowing.
~~~

**AFTER（建议替换为）**

~~~text
2. `R-2` shipped as **③b2**: derived narrowing happens **only** when the seed has **exactly two** canonical genres and the text states no director (whole decade, explicit keys win, no fallback on an empty pool). One-genre seeds and 3+-genre seeds derive nothing; the 3+-genre case is a **designed boundary**, so it is annotated on a set that exists independently of the executor's ordering: a 3+-genre seed **with a known director**, query text asking only for "movies similar to X" (no text constraints), acceptable set = **that director's other catalog rows** (fixed SQL predicate, `rows=`, `5 <= n <= 25`), convention `same director`. The case passes only if the ranked top-5 all fall inside that set — i.e. the ranking (`same_director DESC`) hits without any derived narrowing.
~~~

### D-2 "Why one probe is pre-declared as expected-to-fail" 段末句（缺容错算术）

**BEFORE（与 #81 正文逐字一致）**

~~~text
It is not counted as an unexpected failure and must not be used to change rules.
~~~

**AFTER（建议替换为）**

~~~text
It is not counted as an *unexpected* failure — but it is still counted in the `/30` denominator and **it consumes one of the three failures the gate tolerates**: with `P-4` failing as declared, only **two** unexpected failures remain. The report must say so in those words; 29/30 with `P-4` failing is a pass that has used two of its three allowed failures, not a 30/30.
~~~

### D-3 Notes 段（仍写 `r1, pending oracle review`；判卷无影响但已与事实不符）

**BEFORE（与 #81 正文逐字一致）**

~~~text
- Plan and full decision log: `docs/plans/b1-5f-new-blind-holdout.md` (r1, pending oracle review). Its unresolved items (`OD-f1`…`OD-f5`) are internal and are not part of this issue's scope commitment.
~~~

**AFTER（建议替换为）**

~~~text
- Plan and full decision log: `docs/plans/b1-5f-new-blind-holdout.md` (r2: oracle P1-1/P1-2 addressed; pending oracle quick re-check and final owner approval). Its remaining open items (`OI-f1`…`OI-f3`) are internal and are not part of this issue's scope commitment; `OD-f1`…`OD-f5` are already decided and recorded there.
~~~

### D-4 自检结果（r3 实测，全部通过）

> **应用状态（r3 实测，2026-11-20）**：本地 `GitHub issue body` 区与远端 `gh issue view 81 --json body` **逐行 diff 为空** ⇒ **远端 #81 已应用全部 3 处替换**（本节原先的“远端尚未应用”已被本次实测推翻，保留为历史）。

1. 正文 `ranked, not narrowed` = **0**；`same director` = **6**；`165` = **0** ✅
2. `two unexpected failures` = **1**、`consumes one of the three` = **2** ✅
3. `pending oracle review` = **0**、`(r1` = **0**、`OI-f1` = **1** ✅
4. 标题/milestone（#3）/labels（无）/`Refs #74`（无 closing keyword：`Closes`/`Fixes`/`Resolves` 均 0）**均未变** ✅
5. P-1 窗口：`1948` = **0**、`1980` = **2**、`13 rows` = **3** ⇒ 远端已是 1980–1982 / 13 行口径 ✅

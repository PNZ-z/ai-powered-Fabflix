# B1.5a — Evaluate movie-card embeddings for user-side movie retrieval（issue #69）

> **文档状态：🟢 issue #69 已创建（2026-11-09）、**评测实施已完成（2026-11-09，报告实测：D8 FAIL）**；尚未产出任何用户功能**，本文件为规范来源。
> 下面 `## GitHub issue body` 分区是**已逐字发布**到 GitHub 的正文（**2026-11-09 唯一例外：数据事实修正把正文的“2000 年及以后”改为“2001 年及以后”，共 2 处，见 `last_updated`**；其余回填未修改该分区）；`## Internal tracking` 分区**未发布**（只存在于本仓库）。
> **远端事实（由主 agent 于 2026-11-09 回传）**：issue **#69** — https://github.com/PNZ-z/ai-powered-Fabflix/issues/69 ；milestone **#3 `Phase 3 — AI-powered platform`**；**labels 空**。文档已由 `docs/issues/proposed-b1-5a-movie-card-embeddings.md` 改名为 `docs/issues/issue-69-b1-5a-movie-card-embeddings.md`，编号不再未知。
> 本文件**不声称**：评测已运行、任何阈值已达标、远端 checkbox 已勾选（**checkbox 状态未核实**——创建时正文的 checklist 未勾选）。
>
> | 元数据 | 值 |
> | --- | --- |
> | 文件类型 | `docs/issues/issue-<编号>-*` 规范记录（已发布的规范来源） |
> | 计划来源 | `docs/plans/b1-5a-movie-card-embeddings.md`（🟢 已批准 / 已建 issue / **已实施（评测完成，D8 FAIL）**；见其 §16）；总纲 = `docs/plans/b1-5-movie-retrieval-eval.md` |
> | 关联规范 | `docs/architecture/movie-cards.md`、`docs/architecture/ai-user-workflows.md` |
> | 共用契约 | `docs/ai/README.md`、`docs/ai/golden-set.schema.json`；60 条金标准集与 B1.5b **共享** |
> | GitHub issue | **#69** — https://github.com/PNZ-z/ai-powered-Fabflix/issues/69 （已创建 2026-11-09） |
> | 目标 milestone | **#3 `Phase 3 — AI-powered platform`**（远端已确认，由主 agent 回传） |
> | Labels | **无**（远端 labels 为空，与 #66 一致） |
> | 批准状态 | ✅ **已批准并已发布**：oracle delta 复核的 3 项已在 r4 修复、主 agent 批准发布；`github-operator` 已创建 #69 |
> | 实施状态 | ✅ **已完成（评测实施，2026-11-09）**：脚本 `scripts/eval/embed_eval.py` / `check_golden.py` 已跑、报告已生成 **`docs/ai/reports/b1-5a-embedding-eval.md`**；**结论：D8（`recall@5` ≥ 80%）FAIL**（`bge-m3` **0.4833 = 29/60**、`qwen3-embedding:0.6b` **0.4167 = 25/60**；阈值/分母未改）。**不交付用户功能**（本 issue 本就只交评测） |
> | `last_updated` | 2026-11-09（**r8：`OPEN-a4` 措辞与报告 §16 对齐**——撤销“倾向只建 `en-short`”，改为“模板优劣因模型而异、没有普适单一模板；双索引收益不足以支撑 +82.4 MiB 与约 2× 构建成本，最终选择权在主 agent”。**r7：回填实施完成状态**——报告 `docs/ai/reports/b1-5a-embedding-eval.md` 实测完成、**D8 FAIL（0.4833 / 0.4167）**；`OPEN-a2` 由实测关闭（`bge-m3` 2048 / `qwen3` 4095 tokens）；`README.md` 第 **13/14/16** 行已按计划 + 实测修正落地；**r6：已发布正文事实修正 2000→2001**——AC-5 与 Risks 两处（`2001-onwards`、`11,975 movies from 196–2000`）；**远端 #69 正文仍为旧口径，待 `github-operator` 同步**。r5：回填 issue #69 编号/URL/milestone/labels 与状态。r4：AC-4/AC-6 统一 D8 门槛口径、关闭 `OPEN-a1`/`OPEN-a5`/`OPEN-U2`） |
>
> **发布前检查清单（已完成；#69 于 2026-11-09 创建）**
>
> - [x] oracle 已复核拆分后的计划与本文 → **delta 复核的 3 项已在 r4 文档修复**（`db_snapshot_hash` 的 SQL 无效 / 卡片统计旧口径 / D8 门槛口径）
> - [x] 主 agent 已裁定 `OPEN-a1`/`OPEN-U2`（**D8 gate = top-5 / `recall@5` ≥80%，任务级成功为次要**）与 `OPEN-a5`（主 agent 为标注/复核人）
> - [x] 正文无占位符、无未决承诺（正文只承诺评测交付；`OPEN-a2`/`OPEN-a3`/`OPEN-a4` 的具体选择**未写进正文**，属实施期裁定）
> - [x] 主 agent 显式批准发布 → 已由 `github-operator` 创建 **#69**
>
> 已发布后的对应关系：`## GitHub issue body` = #69 正文的规范来源；`## Internal tracking` = 本地跟踪，**不发布**。

---

## GitHub issue title

```
[B1.5a] Evaluate movie-card embeddings for user-side movie retrieval
```

## GitHub issue body

````markdown
## Goal

Measure, with reproducible numbers, whether embedding **movie catalog cards** retrieves movies that a user would accept for Chinese and English queries — so that the future user-facing retrieval feature has a measured basis instead of an assumption.

This issue delivers **an evaluation and a report only**. No user-facing feature, no production endpoint, no vector store in production code, and no changes to `src/main`, `pom.xml`, or CI.

Scope premise (decided): users ask about **movies**. Repository development documentation is **not** user-facing corpus and is never chunked, embedded, indexed, or retrieved here.

## Scope

- [ ] Build `movie-card/v1` card text for the **whole catalog snapshot** — 21,087 movies, years 196–2024 — with the `zh-full` template as the primary document and `en-short` as the control (templates are specified in `docs/architecture/movie-cards.md`).
- [ ] Embed with **two** candidates, both already installed and both 1024-dimensional: `bge-m3` and `qwen3-embedding:0.6b` (measured: 639,150,858 bytes, Q8_0, `embedding_length=1024`, verified by `/api/show` and by an `/api/embed` response of length 1024).
- [ ] Run one frozen **60-case golden set** — 20 Chinese query → Chinese-template card, 20 Chinese query → English card, 20 English query → English card — shared verbatim with B1.5b so the two routes can be paired per query. Every positive case must be verified against the snapshot before it is frozen.
- [ ] Report **`recall@5` first** — it is the sole pass/fail metric against the D8 gate — then `MRR@10`, and then the task-level success rate as a **secondary** explanatory metric (at least one acceptable movie id appears in the returned top-5 set, and every displayed field matches the database). Also record p50/p95 latency (model-loaded and cold), build time, token volume, vector memory, zero-result rate and any silent-truncation incidents.
- [ ] Record three fingerprints for every number block: `template_hash`, `db_snapshot_hash`, and `schema_hash` (not applicable here — state `N/A` explicitly). Algorithms are fixed in `docs/ai/README.md`.
- [ ] State a **pass/fail** verdict against the already-approved quality gate: catalog semantic retrieval **`recall@5` / top-5 hit rate ≥80%**. The threshold, denominator and query tracks may not be re-negotiated after seeing the numbers, and the task-level success rate may not be substituted for that verdict.
- [ ] Report the Chinese-translated-title track separately, including its failure rate. The catalog stores English original titles only, so "The Matrix" is reachable while "黑客帝国" generally is not; the report must show this rather than excuse it.
- [ ] Update `README.md` **lines 13–14 in this same PR** with the exact replacement text recorded in the plan, so the roadmap stops describing a development-document corpus. (The replacement text is frozen in `docs/plans/b1-5a-movie-card-embeddings.md` §9; leaving it to a later issue would keep the old, superseded wording live.)

## Non-goals

- **No tools, tool calling, routing or refusal evaluation** — that is B1.5b. This issue does not call the chat model's `tools` interface.
- **No MCP server, no `@Tool` implementation, no `src/main/**` changes** — deferred to the feature issue, which needs its own approval.
- **No development-document corpus.** No chunking, embedding, indexing or retrieval of `docs/**` or `README.md`.
- **No production vector store, no API endpoint, no UI.**
- **No alias table for Chinese translated titles.** Introducing one is a new data source and needs separate approval; this issue only measures and reports the gap.
- **No claim that any number is measured before it is.** Extrapolated build cost must be labelled as extrapolation.

## Acceptance criteria

| # | Criterion | How it is verified |
| --- | --- | --- |
| 1 | The 60-case golden set is versioned, frozen with its SHA-256, and covers 20/20/20 across the three tracks | File review + `docs/ai/golden-set.schema.json` validation |
| 2 | Every positive case is verified reachable in the 21,087-movie snapshot, and the verification evidence is recorded | Report appendix |
| 3 | **Both** multilingual embedding candidates (installed and measured, 1024 dimensions each) are compared across Chinese→Chinese, Chinese→English and English→English | Report tables |
| 4 | **`recall@5` (the D8 decision metric)**, `MRR@10`, and the task-level success rate (secondary/explanatory only) are recorded per (model × template) — together with p50/p95 (loaded and cold), build time, tokens, vector memory, zero-result rate and truncation count — each with `template_hash` and `db_snapshot_hash` (`schema_hash` = N/A, stated) | Report |
| 5 | The report states the dataset actually used (21,087) and its coverage (196–2024; 61 genre rows / 56 distinct names; 13,082 movies without a rating; ratings only for 2001-onwards titles), and labels every extrapolation | Report |
| 6 | An explicit **pass/fail** verdict against the ≥80% **`recall@5` / top-5** gate, with no post-hoc change of threshold, denominator or track, and no substitution of the secondary task-level success rate | Report conclusion |
| 7 | The report states explicitly that no development documentation was used as corpus, with the runner configuration as evidence | Report + script config |
| 8 | Measured, extrapolated and unverified items are separated | Report and workflow log |
| 9 | **Endpoint publication: not applicable.** This issue publishes no HTTP endpoint and starts no server process, so the MCP/tool-authentication question does not arise. If a later issue introduces such an endpoint, it must ship a separate, separately approved authentication design | Report statement |
| 10 | Chinese-translated-title hit rate is reported separately with its failure rate | Report |

## Dependencies

- A reachable Ollama endpoint with `bge-m3` and `qwen3-embedding:0.6b` installed. The endpoint has no authentication and must be reachable from the run host; availability is environmental and must be re-confirmed before each run.
- The Docker MySQL snapshot (`fabflix-mysql`, host port 3307) for `db_snapshot_hash` and for verifying that golden-set positives exist.
- Human labelling of the 60 queries (accepted-result sets). The labelling/review owner is the coordinating maintainer; the implementation worker only generates candidates and verifies reachability with SQL. Because this is single-labeler labelling, the report must state that the figures are directional (n=60) rather than significance-tested. The 9,052-row / 2001–2019 instance is history only and is not a measurement source.

## Risks

- **Endpoint unavailable.** Runs slip; numbers are never invented to fill the gap. On macOS, a client without Local Network permission fails with "No route to host" while `curl` works — diagnose that before blaming the endpoint.
- **Silent truncation.** Per-input limits differ between the measured value (2,048 tokens) and the declared `context_length` (8,192). The actual limit must be confirmed before the formal run; a single card (~48 tokens) is far below either, but the report must state the confirmed limit.
- **Latency outliers.** A 13× outlier was observed on a warm batch previously, so medians plus worst case are required.
- **Chinese translated titles mostly miss.** The catalog has English titles only; this is a measured boundary, not a defect to hide. Fixing it needs an alias table and separate approval.
- **Rating coverage is time-bounded.** Ratings exist only for 2001-onwards titles: all 11,975 movies from 196–2000 have none (including the 58 movies from 2000). Any "old decade + rating filter" query is unsatisfiable by construction and must not be encoded as a positive case.
- **Same dimensionality across candidates.** Both models produce 1024-d vectors, so a "they look the same" impression is plausible; per-track and per-query paired reporting is required, not a single aggregate.
- **Scope creep.** The moment this issue adds a production endpoint or UI it has become the feature issue without approval.

## Notes

- Companion plan: `docs/plans/b1-5a-movie-card-embeddings.md`. Umbrella (shared dataset, shared metric, decision rules): `docs/plans/b1-5-movie-retrieval-eval.md`.
- The sibling evaluation `[B1.5b]` covers structured tools and deterministic routing and reuses the same 60 queries; the two reports feed one decision memo.
- Evaluation contract artifacts live in `docs/ai/` (hash algorithms, golden-set schema); scripts go to `scripts/eval/` and are stdlib-only. PR CI neither runs them nor needs the local machine.
- **Relationship to `AC-B-12`:** the previous `AC-B-12` (embedding language capability measured over a documentation corpus) has been re-scoped by the accepted decision D-7 into two route-level criteria. **This issue carries the embedding criterion only**; it does not claim to close the old `AC-B-12`, and its corpus is the movie catalog — never `docs/**` or `README.md`.
````

## Internal tracking

> ⚠️ **远端待同步（2026-11-09 数据事实修正 2000→2001）**：本地 `## GitHub issue body` 的 AC-5 与 Risks **已按实测修正**（`ratings only for 2001-onwards titles`；`all 11,975 movies from 196–2000 have none (including the 58 movies from 2000)`）。依据：3307 只读 SQL 实测 `MIN(year)` among rated = **2001**、2000 年 58 部**全部**无评分、196–2000 共 **11,975** 部无评分（196–1999 的 11,917 + 2000 的 58）、8,005 部有评分。**远端 issue #69 正文仍是旧口径**（本文件不代表远端已更新）→ 如需在 GitHub 上体现，由主 agent 交 `github-operator` 编辑正文或追加评论；同一口径已同步到 `docs/architecture/movie-cards.md`、`docs/architecture/ai-user-workflows.md`、`docs/plans/b1-5a-movie-card-embeddings.md` 与 `docs/ai/README.md`。

> **本分区不发布到 GitHub。**

| 项 | 值 |
| --- | --- |
| 来源 | 用户 2026-11-09 批准 D-1/D-2/D-3/D-4/D-7/D-9/B-4/B-9；oracle 2026-11-09 的 9 项阻塞（B-1…B-9）修复 |
| 关联计划 | `docs/plans/b1-5a-movie-card-embeddings.md`（🟢 已批准 / 已建 issue / **已实施（D8 FAIL）**） |
| 总纲 | `docs/plans/b1-5-movie-retrieval-eval.md`（umbrella / 决策备忘录） |
| 兄弟 issue | `docs/issues/proposed-b1-5b-tool-routing.md`（**仍为草稿**：待 G1 修复/复核后另建，**不随本 issue 发布**） |
| 目标 milestone | **#3 `Phase 3 — AI-powered platform`** |
| Labels | **无** |
| GitHub issue | **#69** — https://github.com/PNZ-z/ai-powered-Fabflix/issues/69 |
| 创建者 | `github-operator`（2026-11-09，按主 agent 批准的 r4 草稿创建） |
| 创建后动作 | ✅ 已回填编号/URL/milestone/labels，文件已改名为 `docs/issues/issue-69-b1-5a-movie-card-embeddings.md` |
| 当前跟踪状态 | ✅ **评测实施完成（2026-11-09）**：报告 = `docs/ai/reports/b1-5a-embedding-eval.md`；D8 FAIL；next = 主 agent 据报告更新 umbrella 决策备忘录，并决定 B1.5b（工具/路由）是否/何时启动（**B1.5b 尚未实施**） |
| 评测结果（判定口径） | **D8 `recall@5` ≥ 80%（唯一 pass/fail）**：`bge-m3:latest` **0.4833（29/60，MRR@10 0.4334）**、`qwen3-embedding:0.6b` **0.4167（25/60，MRR@10 0.3134）** → **两个候选均不通过**；任务级成功率与 `recall@5` 数值恒等（次要指标） |
| 分意图实测（配对口径） | `search_by_title` 15 条：`bge-m3` 13/15、`qwen3` **15/15**；`filter_catalog` 20 条：9/20 vs **2/20**；`similar_movies` 15 条：3/15 vs 3/15；`recommend_by_preference` 10 条：4/10 vs 5/10 |
| 报告链接 | `docs/ai/reports/b1-5a-embedding-eval.md`（含复现指纹：`template_hash` `c5990dc4…`/`bc3bcce6…`、`db_snapshot_hash` `8b16c047…`、`golden_sha256` `0c2f4c5e…be079`、`schema_hash` = N/A） |
| `README.md` 改动 | ✅ 已改 **第 13/14/16 行**（`git diff` 仅此 3 行）：13 = 标题改为 “structured search (doable now) + semantic similarity search (**under evaluation**)” + 评分 `2001-onwards` + 尾部注明 “`recall@5` 0.48 < 0.80 target” 并链到报告；14 = 用户侧只做电影目录、开发文档不是语料；16 = 推荐归属用户侧目标、多轮仍推迟（D-7）。其余行未动 |

### 待确认项（发布后仍跟踪；不在正文承诺内）

| ID | 待确认 | 归属 | 状态 |
| --- | --- | --- | --- |
| `OPEN-U1` | B 路线交付形态门槛（与本 issue 无关，但 memo 需要） | 用户/oracle | 🟡 待定（不影响 #69） |
| `OPEN-U2` | 任务级成功率是否等价于 D8 的 top-5 命中率口径 | oracle | ✅ **已裁定（2026-11-09）**：不等价；D8 gate = top-5 / `recall@5` ≥80%，任务级成功为次要指标，不纳入 A 的通过条件 |
| `OPEN-a1` | 同上（本 issue 视角） | 主 agent/oracle | ✅ **已裁定**：同上（与 `OPEN-U2` 同口径） |
| `OPEN-a2` | 截断阈值（2,048 vs 8,192） | 实测 | ✅ **已由实测关闭（2026-11-09）**：`bge-m3` = **2,048** tokens（`prompt_eval_count` 902→1,802→2,048 后饱和；`/api/tags` 的 8192 不可信）、`qwen3-embedding:0.6b` = **4,095** tokens（非 32,768）；本评测 659 batch × 4 组合**静默截断 0 次** |
| `OPEN-a3` | 中文译名别名表是否引入 | 用户 | 🟡 未批准（本 issue 不做；正文 Non-goals 已写明） |
| `OPEN-a4` | 双模板双索引 vs 单索引 | 主 agent | 🟡 待定（**未写进正文**），但已有实测数据：`bge-m3` 四组合中 `zh-full` **31/60 (0.5167)** ≥ `en-short` **29/60 (0.4833)**，且**逐轨道 ≥**（zh_zh 11 vs 11 / zh_en 9 vs 8 / en_en 11 vs 10）；`qwen3` 则 `en-short` **26/60** > `zh-full` **23/60** → **模板优劣因模型而异，没有普适的单一模板**；而双索引的收益不足以支撑 **+82.4 MiB** 内存与约 **2× 构建成本** → **最终选择权在主 agent**（本 issue 不给结论） |
| `OPEN-a5` | 60 条标注人 / 复核人 | 主 agent | ✅ **已指派（2026-11-09）**：**主 agent 担任标注/复核人**；worker 只生成候选 + SQL 验证；报告注明“单人标注、n=60 方向性” |

### 发布正文自检

- [x] 无"MCP 已实现/端点可达"表述（本 issue 不涉及端点，AC-9 写 N/A）
- [x] 无虚构 issue/PR/提交编号
- [x] 无"第二模型未就绪"或"维度未知"的过时表述（已实测 1024 维、已安装）
- [x] 无"评测已运行/阈值已达标"表述
- [x] 无端点地址、token、密钥
- [x] 无开发文档语料表述（明确排除）
- [x] **正文事实修正（2026-11-09）**：评分覆盖起点 2000→**2001**（AC-5 与 Risks 两处），不再声称 2000 年有评分；其余正文/验收未改

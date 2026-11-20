# B1.5b — Evaluate structured tools and deterministic routing for user-side movie retrieval（issue #72 规范记录）

> **文档状态：🟢 GitHub issue [#72](https://github.com/PNZ-z/ai-powered-Fabflix/issues/72) 已创建（2026-11-10；milestone #3、无 labels）/ **评测已完成（2026-11-19）— G1 任务级 62/75 = 82.7%，未达 ≥90% 门槛（FAIL）；报告 = `docs/ai/reports/b1-5b-tool-routing.md`**；**issue 保持 OPEN（未达 AC，后续修复另跟踪）**；本文件**不声称已关闭**，也**不声称 B-① 已交付**。**
>
> | 元数据 | 值 |
> | --- | --- |
> | 文件类型 | `docs/issues/issue-72-*` 规范记录（原发布草稿 `proposed-b1-5b-tool-routing.md` 已改名回填编号；下方 `## GitHub issue body` 为该 issue 已发布的正文，**本次回填未改动**） |
> | 计划来源 | `docs/plans/b1-5b-tool-routing.md`（🟢 已批准（2026-11-10，主 agent）/ 已建 issue #72 / **评测已完成（G1 82.7% FAIL，报告已出）**​）；总纲 = `docs/plans/b1-5-movie-retrieval-eval.md` |
> | 关联规范 | `docs/architecture/ai-user-workflows.md`（工具契约、`R7` 服务端强制、year/enum 契约、runtime prompt）、`docs/architecture/movie-cards.md` |
> | 共用契约 | `docs/ai/tool-schema.b1-5.json`（**机器可读 schema fixture**，`schema_hash` 已记录）、`docs/ai/README.md`、`docs/ai/golden-set.schema.json` |
> | GitHub issue | **已创建**：[#72](https://github.com/PNZ-z/ai-powered-Fabflix/issues/72)（2026-11-10，由 `github-operator` 发布） |
> | 目标 milestone | **#3 `Phase 3 — AI-powered platform`** |
> | Labels | **无** |
> | 批准状态 | 🟢 **主 agent 已批准创建（2026-11-10）**；issue **#72 已按本文件“GitHub issue title/body”发布**；实施状态 = **评测已执行（2026-11-19）**：**G1 未达 ≥90% 门槛（FAIL）→ issue #72 保持 OPEN**（状态以远端为准，本文件不声称已关闭） |
> | `last_updated` | 2026-11-19（r7：**回填评测结果** — G1 **62/75 = 82.7% FAIL**、G0 52.0%、G2 50.7%、G3 未测；报告已出；issue 保持 OPEN；**`## GitHub issue body` 未改动**）。2026-11-10（r6：**回填 GitHub issue #72** — 文件改名 `issue-72-b1-5b-tool-routing.md`、状态→已创建、内部跟踪补 URL/milestone/labels、发布清单标为完成；**`## GitHub issue body` 未改动**）。r5：状态→已批准创建；发布前检查清单标为满足；`OPEN-b1` = **≥90%** 已裁定、`OPEN-b4` 只留可选 G3） |
>
> **发布检查清单（✅ 已完成 — issue [#72](https://github.com/PNZ-z/ai-powered-Fabflix/issues/72) 已于 2026-11-10 创建）**
>
> - [x] oracle 2026-11-09 **delta 条件已满足**：G1 为必做（不是可选）、`required`/对象级 `anyOf` 口径已写入标题/正文/验收
> - [x] 主 agent 已裁定 `OPEN-b1`（交付候选形态 **≥90%** 任务级成功率；G0 58% 不得交付）与 `OPEN-b4`（**G3 本期可选/不要求**，未做须写“未测”；G1/G2/G0 必做）
> - [x] 60 条共享金标准集已由 B1.5a 冻结（`docs/ai/golden/queries.b1-5.json`，`golden_version = b1-5-golden/v1`，sha256 `0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079`）；15 条工具用例由本 issue 新建
> - [x] 主 agent 显式标记“可发布”（2026-11-10：批准创建）
> - [x] `github-operator` 已按本文件 `## GitHub issue title` / `## GitHub issue body` 创建 **issue #72**（2026-11-10；milestone #3、无 labels）；正文内容未在本文件回填中修改

---

## GitHub issue title

```
[B1.5b] Evaluate structured tools and deterministic routing for user-side movie retrieval
```

## GitHub issue body

````markdown
## Goal

Measure how well **structured, read-only movie tools** can serve user-side movie retrieval when the server — not the model — owns intent classification, refusal, enum mapping and argument validation, and decide whether pure-LLM tool selection is acceptable as a delivery form.

This issue delivers **an evaluation and a report only**. No user-facing feature, no production endpoint, no MCP server, and no changes to `src/main`, `pom.xml`, or CI.

Two decisions frame the work and are not re-opened here: users ask about **movies**, and repository development documentation is **not** user-facing corpus; and delivery routing must be **deterministic first**, with the model only filling slots or choosing from controlled options.

## Background (measured, not assumed)

- A 12-case single-turn probe at `temperature=0`, using a hand-transcribed five-tool schema, scored **7/12 fully correct (58%)**: two wrong tool selections (a plot question routed to title search — the required refusal never happened; a genre word used as a title query), three bad argument sets (a Chinese genre value, an invented `min_rating`, a tool call issued together with a clarification).
- Replaying an `INVALID_ARGUMENT` once did **not** self-correct; post-tool answers invented attributes absent from the result and translated titles.
- Warm tool-call latency is 0.38–1.94 s, cold start **39.10 s** (default `keep_alive` 5 minutes); five tools cost ~598 prompt tokens, of which the schemas are ~500.
- The catalog snapshot holds **21,087** movies, years **196–2024**, **61 genre rows / 56 distinct names**, and **13,082** movies without a rating — and ratings exist only for **2001**-onwards titles, so all 11,975 movies from 196–2000 (including all 58 titles from 2000) have none.
- `genres` contains duplicate names (`Action`, `Comedy`, `Drama`, `Horror`, `Thriller` appear twice) and a semantic variant pair: `Sci-Fi` has **457** movies while `Science Fiction` has **2**. An enum copied from the raw table is therefore wrong; it must be derived with `DISTINCT` and canonicalized deliberately.
- Movie ids are opaque `VARCHAR(10)`; only 9,073 of 21,087 start with `tt`, so the prefix cannot be assumed.

## Scope

- [ ] Treat **`docs/ai/tool-schema.b1-5.json`** as the single machine-readable evaluation contract: `additionalProperties: false`, numeric `minimum`/`maximum`, `genres.items.enum` derived from the database, and **explicit per-tool “at least one” semantics** — `required` for `search_movies_by_title` / `find_similar_movies` / `get_movie_card`, and an object-level `anyOf` for `filter_movies` (`genres` / `year_from` / `year_to` / `min_rating`) and `recommend_movies` (`preferred_genres` / `year_range` / `min_rating` / `director` / `seed_movie_ids`). Every variant uses the same file and the report records `schema_hash`.
- [ ] Run the **60 shared queries** (the same file B1.5a uses, so results can be paired per query) plus **15 tool cases**: 6 refusals, 4 clarifications, 5 multi-slot combinations.
- [ ] Run **G0 (pure LLM routing), G1 (deterministic routing in front) and G2 (server-side schema validation + enum/alias mapping + `required`/`anyOf` semantics)** as the mandatory variants. **G1 is mandatory** because it is the only variant that produces a measured number for the approved delivery form (D-9: deterministic routing first); its minimal acceptable shape is in-script deterministic pre-classification/refusal plus controlled slot filling — no production code required. **G3 (server-rendered answers + at most one automatic retry)** remains optional; anything not run must be reported as "not measured" rather than substituted.
- [ ] Report the **task-level success rate** as the primary metric (at least one acceptable movie id appears in the final answer, and every displayed field matches the database), with intent accuracy, tool selection, per-slot exact match, schema-compliance rate, grounding violations, refusal correctness, latency and a failure-mode tally as explanatory metrics only.
- [ ] Implement and verify the **year and enum contract**: wrong type/syntax, unknown fields and out-of-enum values return `INVALID_ARGUMENT`; syntactically valid values outside the data coverage (196–2024) return `OUT_OF_RANGE` with `coverage`; valid in-range queries with no matches return `EMPTY_RESULT` with `coverage` (including `rating_coverage_from`). These three states are mutually exclusive, and none of them may be silently converted into a bare empty list.
- [ ] Ground answers server-side: the six catalog fields are rendered from database results, and no fabricated title, rating or id — and no attribute outside those fields — is tolerated.
- [ ] Report the 7/12 baseline and the post-mitigation results **side by side**, with the failure taxonomy behind each number.
- [ ] State a routing verdict: pure-LLM routing is **not** a delivery form (measured 58%); the proposed form is deterministic routing with controlled slot filling, and its measured task-level success rate is reported against the **agreed ≥90%** task-level gate.

## Non-goals

- **No MCP server.** No MCP starter is added, no server is started, no `/sse` or `/mcp` endpoint is published. MCP is reconsidered later as a deployment question; if it is ever introduced, its authentication design is a separate, separately approved trust-boundary change.
- **No embedding, vector index or card build** — that is B1.5a.
- **No production endpoint, UI, `src/main/**` change, `pom.xml` change or CI change.**
- **No multi-turn conversation, memory, personalization, write tools, approvals or audit.**
- **No development-document corpus and no questions about this project's development process.**
- **No Chinese-title alias table.** It is a new data source and needs separate approval; this issue measures and reports the gap.

## Acceptance criteria

| # | Criterion | How it is verified |
| --- | --- | --- |
| 1 | `docs/ai/tool-schema.b1-5.json` is the only schema used, with DB-derived `enum`, **explicit `required`/`anyOf` per tool**, `additionalProperties: false`, numeric `minimum`/`maximum`, and a recorded `schema_hash` | File review + hash recomputation |
| 2 | The 60 shared queries and 15 tool cases (6 refusal / 4 clarification / 5 multi-slot) are versioned and frozen; every positive case is verified reachable in the snapshot | Fixture validation + SQL evidence |
| 3 | **G0, G1 and G2 are all run** against the same `schema_hash`; G3, if unrun, is explicitly marked "not measured" | Report |
| 4 | Task-level success rate plus intent, tool selection, per-slot exact match, schema compliance, grounding, refusal correctness, latency and failure modes are reported, with the 7/12 baseline alongside post-mitigation numbers | Report |
| 5 | The year/enum contract behaves exactly as specified: `INVALID_ARGUMENT` for malformed or out-of-enum input, `OUT_OF_RANGE` + `coverage` outside 196–2024, `EMPTY_RESULT` + `coverage` for valid-but-empty, mutually exclusive | Report + case results |
| 6 | Grounding is zero-tolerance: fabricated ids/ratings/titles, field rewriting or translation, or any attribute outside the six catalog fields fails the case and is classified | Report failure table |
| 7 | All 6 refusal cases behave correctly; under-refusal and over-refusal are counted separately | Report |
| 8 | The routing verdict is explicit: pure-LLM routing is not a delivery form (measured), and the proposed deterministic form comes with its measured task-level success rate | Report conclusion |
| 9 | **MCP endpoints: not applicable to this issue.** Nothing is published, so no anonymous tool endpoint exists; if an MCP endpoint is introduced later, it requires a dedicated authentication design and approval as a new trust boundary | Report statement |
| 10 | Measured, unmeasured and unverified items are separated; an unreachable endpoint or an unrun variant is never reported as done | Report and workflow log |

## Dependencies

- A reachable Ollama endpoint with `qwen2.5:7b` (Q4_K_M, `capabilities` includes `tools`); no authentication, so it must stay on a controlled network.
- The Docker MySQL snapshot (`fabflix-mysql`, host port 3307) for grounding checks and coverage metadata.
- The 60-case golden set frozen by B1.5a (`golden_version` + SHA-256), so both routes answer the same queries.
- The delivery-form threshold is **agreed at ≥90%** task-level success (approved 2026-11-10); **G3 stays optional and is not required this period**, and anything not run is reported as "not measured". **G1 is not optional**: it is the measured basis for the proposed deterministic delivery form.

## Risks

- **G2 may still fall short.** Server-side enforcement is the treatment, not prompt tweaks; if it cannot lift the number, the delivery form degrades to deterministic routing with form-style filtering and the memo must say so.
- **Enum canonicalization drives the result.** `Sci-Fi` (457 movies) versus `Science Fiction` (2) can swing hit rates; the report must state whether mapping was applied and report both groups rather than one.
- **Rating coverage is time-bounded.** Pre-2001 rating filters are unsatisfiable (no title from 2000 or earlier has a rating row); the contract returns `EMPTY_RESULT` with `coverage.rating_coverage_from`, and the model may not describe that as "no such movies exist".
- **Cold start dominates p95.** 39.10 s on first call with a 5-minute `keep_alive`; loaded and cold runs must be reported separately.
- **Hand-transcribed schemas already failed once** (a free-string `genres`, empty `required`, leading directly to enum-language errors). The fixture file — not a Markdown table — is the only contract.
- **Model drift.** A changed model digest invalidates comparability; the digest must be recorded and a non-reproducible run must say so.
- **Scope creep.** Adding an MCP server or a production endpoint converts this into a feature issue without approval.

## Notes

- Companion plan: `docs/plans/b1-5b-tool-routing.md`. Umbrella (shared dataset, shared metric, decision rules): `docs/plans/b1-5-movie-retrieval-eval.md`.
- The routing and tool contracts are specified in `docs/architecture/ai-user-workflows.md`; that document is a **spec**, not a prompt injected at runtime — the runtime prompt is a separate, versioned, compact artifact.
- The sibling evaluation `[B1.5a]` covers card embeddings; both reports feed one decision memo.
- **Relationship to `AC-B-12`:** the previous `AC-B-12` (embedding language capability over a documentation corpus) has been **re-scoped by the accepted decision D-7** into two route-level criteria. **This issue carries the tool-routing criterion** (intent / tool selection / slots / grounding / refusal / latency / schema compliance); it does **not** claim to close the old `AC-B-12`, and it evaluates no embedding at all. MCP is deliberately out of scope (accepted decision D-6): structured tools are exercised through a script or an in-process tool definition first.
````

## Internal tracking

> **本分区不发布到 GitHub。**

| 项 | 值 |
| --- | --- |
| 来源 | 用户 2026-11-09 批准 D-1/D-3/D-4/D-6/D-7/D-9/B-4/B-9；oracle 2026-11-09 的 9 项阻塞（B-1…B-9）修复 + 同日 delta 复核 2 项（r4 已修）；**主 agent 2026-11-10 裁定 `OPEN-b1`（≥90%）/ `OPEN-b4`（仅 G3，可选）并批准创建本 issue** |
| 关联计划 | `docs/plans/b1-5b-tool-routing.md`（🟢 已批准（2026-11-10，主 agent）/ 已建 issue #72 / **评测已完成（G1 82.7% FAIL）**；r8） |
| 总纲 | `docs/plans/b1-5-movie-retrieval-eval.md`（umbrella；其 `OPEN-U1` 对应的 ≥90% 门槛由本次 `OPEN-b1` 裁定，umbrella 本文件本次未同步修改） |
| 兄弟 issue | `docs/issues/issue-69-b1-5a-movie-card-embeddings.md`（**A 路线已完成**：PR #70 已合并、报告 D8 FAIL；60 条金标准集由它冻结） |
| 机器可读契约 | `docs/ai/tool-schema.b1-5.json`（`schema_hash = 8e72f74120e43c5f4a40b1f8815d952d0bf029338a94e328047c60907fd8a567`，`contract_version = b1.5-tools/v2`）；**`filter_movies`/`recommend_movies` 的 `anyOf` 已逐项复核存在**；r3 因评分覆盖起点 2000→2001 的文本修正使 hash 由 `8eea424b…44cd` 变为现值（见 `docs/ai/README.md` §4） |
| 目标 milestone | **#3 `Phase 3 — AI-powered platform`** |
| Labels | **无** |
| GitHub issue | ✅ **已创建**：[#72](https://github.com/PNZ-z/ai-powered-Fabflix/issues/72) — milestone **#3**、**无 labels**（2026-11-10 由 `github-operator` 按本文件 `GitHub issue title` / `GitHub issue body` 发布；远端标题/正文以 GitHub 为准，本文件不做远端修改） |
| 实施状态 | 🟡 **评测已完成（2026-11-19）：G1 62/75 = 82.7% — 未达 ≥90% 门槛（FAIL）**；G0 39/75 = 52.0%、G2 38/75 = 50.7%（回放 G0）、G3 **未测**；接地 75/75；报告 = `docs/ai/reports/b1-5b-tool-routing.md`。**因未达验收门槛，issue #72 保持 OPEN，后续修复（别名/种子消歧/相似信号 + 独立 held-out 集）另跟踪**；本文件**不声称 issue 已关闭**，也**不声称 B-① 已交付/已开始** |
| 评测结论摘要 | ① 路由层正确 74/75 = 98.7%，但任务级 82.7%；② 13 条失败：8 条来自评测 **stand-in** 执行器（similar/recommend 无生产实现）、3 条中文译名（Q21–Q23）、1 条枚举语义变体（Q11）、1 条“澄清 vs 任务成功”口径冲突（Q57）；③ **同集 in-sample**，不是 held-out 泛化估计；④ 判分缺陷（`acceptable_movie_ids` 写死为空）修复后原 92.0%/PASS 作废 |
| 验收对照 | 报告 §12：`B15b-1/2/3/6/7/9/10` 已满足；`B15b-4`/`B15b-5` **部分**；`B15b-8` 数字已给出但**未过 90% 门槛（FAIL）** |
| 创建者 | `github-operator`（已由主 agent 标记“可发布”并于 2026-11-10 完成创建） |
| 创建后动作 | ✅ 已回填编号/URL，并已将文件由 `docs/issues/proposed-b1-5b-tool-routing.md` 改名为 `docs/issues/issue-72-b1-5b-tool-routing.md`；✅ **2026-11-19 已回填评测结果**（G1 FAIL、报告已出、issue 保持 OPEN）——**本文件不修改远端 issue**，远端状态/评论由主 agent 与 `github-operator` 决定 |

### 待确认项

| ID | 待确认 | 归属 | 状态 |
| --- | --- | --- | --- |
| `OPEN-b1` | 交付形态任务级成功率门槛 | 用户/oracle | 🟢 **已关闭（2026-11-10 主 agent）**：**≥90%**；G0 的 58% 不得交付 |
| `OPEN-b2` | 端到端 p95 目标（D8：先实测后定） | 实测后定 | 🟡 待测 |
| `OPEN-b3` | 别名映射词表（中文类型词、`Sci-Fi`↔`Science Fiction`）与维护者 | 实现时定 | 🟡 待定 |
| `OPEN-b4` | **G3** 是否纳入本期（**G1 已必做，不在本项内**） | 主 agent | 🟢 **已裁定（2026-11-10）**：**本期可选 / 不要求**，未做写“未测” |
| `OPEN-b5` | `same_primary_genre_only` 保留与否（`ai-user-workflows.md` 已给确定性定义：seed 的字典序首个 genre） | oracle | 🟡 待复核 |
| `OPEN-U3` | 60 条标注人 / 复核人 | 主 agent | 🟡 待指派 |

> **issue #72 已建、评测已跑后的剩余未决**：`OPEN-b2`（p95 目标，**冷启动未测**）、`OPEN-b3`（别名词表，已实测三类别名缺口）、`OPEN-b5`（`same_primary_genre_only`，生产相似信号未实现）、`OPEN-U3`（标注人）均**仍未解决**；它们不影响“issue 已创建 + 评测已执行”的事实，但也是 **G1 未过门槛的已知原因之一**，修完前**不得**写成已解决或“已达 90%”。

### 发布正文自检

- [x] AC-3 已改为 **G0/G1/G2 必做**（G1 = 确定性路由前置，AC-8 的数字来源；G3 仍可选）
- [x] AC-1 / Scope 已把“显式 required”写清为 **`required` 或对象级 `anyOf`**（两工具无 `required`、用 `anyOf`）
- [x] 标题**不含** "MCP-style"，改为 "structured tools and deterministic routing"
- [x] 不写"replaces AC-B-12"；改写为"re-scoped per the accepted D-7 decision"（正文未声称关闭旧 AC-B-12）
- [x] AC-2 条件化已解除（第二个 embedding 模型与本 issue 无关；本 issue 明确不碰 embedding）
- [x] AC-9 写"MCP 端点 N/A；若引入另立安全验收"
- [x] 无虚构编号、无端点地址、无 token/密钥
- [x] 无“评测已运行/阈值已达标/oracle 已复核”表述；正文只将阈值写作 **agreed ≥90%**（已批准），不写“待定/待确认”

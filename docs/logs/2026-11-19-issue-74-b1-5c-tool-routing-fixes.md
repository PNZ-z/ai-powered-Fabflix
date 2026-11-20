# 工作流日志 — issue #74 / B1.5c 评测层修复 + `G1-T` 中文片名解析 + held-out 重测（**评测完成 = provisional；held-out 非盲测**）

| 项 | 值 |
| --- | --- |
| 完成日期 | **2026-11-19** |
| Issue | [#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74)（milestone #3、无 labels；**状态保持 OPEN** —— 主门槛结果只能算 provisional、blind holdout 未做、`OPEN-C3` 未落码；本日志**不声称**已关闭） |
| 前序 issue | [#72](https://github.com/PNZ-z/ai-powered-Fabflix/issues/72)（B1.5b，**OPEN**；本轮**未**改动其状态） |
| 计划 | `docs/plans/b1-5c-tool-routing-fixes.md`（**r5：实施结果回填**）；总纲 `docs/plans/b1-5-movie-retrieval-eval.md`（r10） |
| 评测报告 | `docs/ai/reports/b1-5c-tool-routing-fixes.md`（**已实测完成**；报告正文由主 agent 维护，**本轮文档更新未改报告正文**） |
| 分支 / 提交 | 分支 **`codex/b1-5c-tool-routing-fixes`**；实施提交 **`78f5740`**（held-out fixture + 契约 v3）、**`734eef5`**（评测层修复 + `G1-T` + in-sample/held-out 运行）、**`9e348de`**（**主 agent 在本次文档写作过程中**一次性提交评测报告 + 本轮 `docs/` 回填）。本日志只记录可核实事实；`docs-writer` **未执行任何 git 写操作**。**PR 未创建** |
| 评测运行 | 由 worker 执行（`scripts/eval/tool_eval.py` 等；命令见报告 §10）；**本轮文档更新未重跑任何评测**，也不声称独立复现 |
| 关键结论 | held-out `G1-T` **30/30 = 100%**（≥27/30 → **provisional PASS**）；held-out `G1` baseline **28/30 = 93.3%**；`G0` / `G2`（回放）**14/30 = 46.7%**；in-sample `G1` **62/75 → 67/75 = 89.3%**（路由 73/75）；**`G3` 未测、MCP 未实现**；**无生产代码（B-① 未开始），执行器与解析器均为评测 stand-in** |
| **关键 caveat** | **held-out 30 条在 stage 1c 已可见并被用于修规则**（H18 查询文本改词；H06/H12/H18/H24/H28/H16 的 pattern/年份消歧在看过该 fixture 后补）⇒ 30/30 是 **post-fix regression on a seen fixture，不是盲测、不是泛化估计**。要声称泛化 ≥90% 必须另建 **blind holdout（B1.5d，未做）** |
| 未提交/待处理 | 本日志与 5 份文档改动（见 §1.1）处于**未提交**状态；`.idea/` 为无关未跟踪目录（**不应**一并提交）；**blind holdout（B1.5d）未立项**、**PR 未创建** |

---

## 1. 修改了哪些文件及潜在问题

### 1.1 逐文件改动（本轮 = 仅 `docs/` 内的**状态与口径更正**，不改任何数字）

| 文件 | 改动目的与内容 |
| --- | --- |
| `docs/ai/README.md` | 把 r8 的 **`G1-T` 30/30 → PASS** 口径更正为 **provisional PASS / post-fix、非盲测**：① `last_updated` 新增 **r9** 段，写明 held-out 在 stage 1c 可见并用于补路由词/消歧（H18 改词；H06/H12/H18/H24/H28/H16 的 pattern/年份消歧）、30/30 是 post-fix regression、Wilson 下界 88.6% < 90%、要声称泛化 ≥90% 必须另建 **blind holdout（B1.5d，未做）**、#74 保持 OPEN；② §1 报告行删除“**独立** held-out”措辞，改为“held-out 30 条（**该 fixture 事后可见 → 非盲测**）”，判定改 `provisional PASS`，并补 `G1` 28/30、`G0`/`G2` 14/30、in-sample 67/75、`G3`/MCP 未测、stand-in 与 caveat；③ §4 修订记录新增 **r9** 行（含同轮数字），并把 **r8** 行的 `30/30 PASS` 标注为“r9 更正为 provisional PASS”。**未改任何 fixture、`schema_hash`、阈值、分母** |
| `docs/plans/b1-5c-tool-routing-fixes.md` | ① 标题与 §2.1 删除“**独立** held-out”口径；② 文件头状态由“已批准 / 待实施”改为 **🟡 评测已完成 = provisional / held-out 非盲测 / blind holdout（B1.5d）待做 / #74 保持 OPEN**，并记录分支、两个提交与报告；③ 元数据 `文档类型`/`last_updated`/`状态`/`基线 / 实施分支`/`关联 GitHub issue`/`产物` 同步为实际结果；④ **新增 §17 实施结果回填**（17.1 状态、17.2 实测数字表 + Wilson CI、17.3 held-out 污染证据、17.4 计划-实现差异逐项、17.5 未测/待办）；⑤ §14 新增 **r5 状态说明**（`OPEN-C1/C2/C6/C7` 已落地、`OPEN-C3`/`OPEN-C11` 未落、新增 B1.5d）；⑥ §16 新增 **r5** 修订行；⑦ 文末“不声称”段改为只否认**泛化/交付/B-①/#72 关闭**。**§10 验收门槛（≥27/30）与分母（30）未改** |
| `docs/issues/issue-74-b1-5c-tool-routing-fixes.md` | 仅改**文件头状态与 `## Internal tracking`**：状态行 → 评测已完成（provisional）/ 非盲测 / B1.5d 待做 / **#74 保持 OPEN**；`计划来源`/`批准状态`/`last_updated` 同步；`实施状态` 由“未实施”改为“评测完成 = provisional”，逐项记录数字、caveat、未裁定项（`OPEN-C3`/`OPEN-C11`/Q48）、blind holdout 未做、PR 未创建；`实施许可` 改为“已给出并已执行（关闭 #74 / 建 PR / B-① 立项仍需主 agent 决定）”；新增 **r5 回填记录**（列明证据来源）。**`## GitHub issue title` / `## GitHub issue body` 完全未改**（`git diff` 已核对：变更全部位于 body 区块之外），**也未编辑远端 issue** |
| `docs/plans/b1-5-movie-retrieval-eval.md`（umbrella） | ① `last_updated` → **r10**（B1.5c 评测完成但 provisional / 非盲测；下一步 = blind holdout 再定 B-①；B-① 未开始；#74 保持 OPEN）；② `issue 草稿 / 已建 issue` 行 → #74“已创建、评测已完成（provisional PASS、非盲测）、保持 OPEN”；③ `状态` 行新增 B1.5c 例外；④ 新增 **r10 回填段**（实测数字 + caveat + 下一步）；⑤ §6 流程图把 B1.5c 标为“事后可见 → 非盲测 / 报告已产生”，并新增 **B1.5d（blind holdout，未立项）**节点；⑥ §6 进度 bullet 与 §7 memo 当前状态同步（memo **仍不能定稿**）；⑦ 文末“不声称”段重写。**共享数据集/指标/门槛未改** |
| `docs/plans/ai-roadmap-overview.md` | ① 改写第 12 条为“B1.5c 已立项并**已完成评测（provisional，非盲测）**”；② 新增 **第 13 条**记录本轮实测（`G1-T` 30/30 / `G1` 28/30 / `G0`·`G2` 14/30 / in-sample 67/75 / `G3`·MCP 未测 / stand-in）+ 污染 caveat + “下一步 = blind holdout（B1.5d），B-① 未开始”，并明确不声称已泛化/已交付；③ §M4 的 ⑪ 段追加 **⑫** 同日更新（M4.1 前置再增 B1.5d）。**第 8/10/11 条历史表述未改写** |
| `docs/architecture/ai-user-workflows.md` | **最小一致性修正（仅 1 处）**：文件头 `last_updated` 的 r7 说明把 `G1-T` **30/30** 的判定从 `（≥ 27/30 → PASS）` 改为 **`（≥ 27/30 → provisional PASS：held-out 在阶段 1c 可见 → post-fix、非盲测；blind holdout 未做）`**。该文件 §1.7 正文（主 agent 于此前的提交中写入）**已自带 provisional / 非盲测 caveat**，§修订记录 r7 行也已写 `provisional PASS`，故本次只补文件头一处，**未改 §1.6、§3 U1、§4.2、`WU-9` 或任何阈值/分母** |
| `docs/logs/2026-11-19-issue-74-b1-5c-tool-routing-fixes.md` | **本文件**（新建工作流日志） |

> **本轮明确未改**：`docs/ai/reports/b1-5c-tool-routing-fixes.md`（报告正文，主 agent 已自行修正为 provisional）、`scripts/eval/**`、`docs/ai/golden/**`、`docs/ai/aliases/**`、`docs/ai/tool-schema.b1-5.json`、`src/main/**`、`pom.xml`、CI、`README.md`；未提交、未推送、未创建/编辑 GitHub issue 或 PR。

### 1.2 结果的**性质**（这是本轮所有文档改动的唯一原因）

- **实测数字本身没有被修改，被修改的是它的“可声称强度”。** 30/30 = 100% 仍然是实测值，但它**不是盲测结果**：held-out 30 条（`H01`–`H30`）在 **stage 1c** 中已经可见，并**被用来改进规则**：
  - **`H18` 的查询文本被改词**（“悬疑片”→“惊悚片”）——可在提交 `734eef5` 的 `docs/ai/golden/queries-heldout.b1-5c.json` diff 中核对；同一提交把 routing 文件 `annotates.sha256` 由 `9933b0cc…535f` 改为 `d471c57a…abe4`，即 **fixture 在全量运行之前被编辑过**；
  - **`H06`/`H12`/`H18`/`H24`/`H28`/`H16` 的通用 pattern / 年份消歧是在看过这些 held-out 用例之后补的**（报告 §0 第 4 条、附录 A 逐例矩阵注明“阶段 1c 修复”）。
- 因此只能表述为 **post-fix regression on a seen fixture / provisional PASS**；计划 §8.5 原本要求的“跑完不得按失败回调规则”**未被完全遵守**（已如实记录，未回滚、未粉饰）。
- 由此产生的**唯一合法下一步**：**blind holdout（B1.5d）** —— 未参与任何规则修复、独立标注、冻结后不得回看修规则；**本轮未做，也未立项**。

### 1.3 实测数字（与报告 §1.1 一致；引用时必须带变体名与 caveat）

| 变体 / 样本 | 结果 | Wilson 95% | 备注 |
| --- | --- | --- | --- |
| **held-out `G1-T`（冷跑）** | **30/30 = 100%** | [88.6%, 100.0%] | 主门槛 → **provisional PASS**；仅 H01/H16 调模型（`model_calls = 2`） |
| held-out `G1-T`（热跑 / 回放） | 30/30 = 100% | 同上 | `model_calls = 0`；缓存不改变结论 |
| **held-out `G1`（baseline）** | **28/30 = 93.3%** | [78.7%, 98.2%] | 不含中文解析，2 条中文用例必败；**非门槛对象**，受同一污染影响 |
| held-out `G0` | **14/30 = 46.7%** | [30.2%, 63.9%] | 纯 LLM 路由不可交付（D-9） |
| held-out `G2`（回放 G0） | **14/30 = 46.7%** | [30.2%, 63.9%] | 无改善 |
| in-sample `G1` | **67/75 = 89.3%**（修复前 62/75 = 82.7%） | [80.3%, 94.5%] | **非门槛**；路由层 73/75 = 97.3% |
| `G3` / MCP | **未测 / 未实现** | — | 与计划 non-goal 一致 |

- 解析指标：`triggered 2 / parse_success 2/2 / wrong_movie 0/2`，冷跑 p50 156.1 ms / p95 488.3 ms（**模型已预热，冷启动延迟仍未测**）。**n = 2 是逐例披露，不是中文解析成功率估计。**
- 契约：`b1.5-tools/v3`、`schema_hash 712c0e34…474a`；held-out 与 aliases 的 sha256 本轮复核未变（queries `d471c57a…abe4`、routing `5608dd4c…4aba`、genre `f7126f02…c203`、person `06316859…dd98`、title `98bfca9b…cbe4`）。

### 1.4 仍存在的风险、边界与未验证项

| # | 项 | 状态 |
| --- | --- | --- |
| 1 | **held-out 已被污染**（stage 1c 可见 + H18 改词 + 看过失败后补路由/消歧） | ❌ 不可修复，只能靠 **blind holdout（B1.5d）**替代；本轮**未做** |
| 2 | 30/30 的 **Wilson 下界 88.6% < 90%** | ⚠️ 统计上不能声称“≥90% 已有 95% 置信”；n=30 功效有限 |
| 3 | `G1-T` 相对 `G1` 的**净增量只有 2 条中文用例** | ⚠️ 不能把“`G1-T` 达标”读成“中文解析已验证到 90% 级别” |
| 4 | held-out 对“**无文本约束的相似查询**”（Q34/Q53/Q54）与“**`Musical`/`Music` 拆分矛盾**”**覆盖为 0** | ⚠️ 30/30 不能外推到这两类风险 |
| 5 | **in-sample Q48 回归**（`musical → Musical` 与冻结标注 `Music` 冲突） | ⚠️ 未裁定（报告 §5.4 给了 (a)/(b)/(c) 三个选项，均需主 agent/用户批准） |
| 6 | **`OPEN-C3`（澄清式成功 / `allows_clarification`）未落码** | ⚠️ Q57 类用例恒计失败；未越界改判分 |
| 7 | **`OPEN-C11`（解析成功率/错译率是否设门槛）未定** | ⚠️ 本轮仅披露（n=2），未设门槛 |
| 8 | `G3` 未测、MCP 未实现、embedding 步 4 关闭、**冷启动延迟未测**（`OPEN-b2`） | ⚠️ 全部如实记为“未测/未实现”，未用 G2 替代 G3 |
| 9 | 相似/推荐执行器与中文解析器都是**评测 stand-in**；**无生产代码**，B-① 未开始 | ⚠️ 数字不是生产 SLA |
| 10 | **`docs/architecture/ai-user-workflows.md` 的口径一致性** | ✅ **本轮已处理**：§1.7 正文与 §修订记录 r7（主 agent 写入）已自带 provisional / 非盲测；文件头 r7 说明的一处 `PASS` 已改为 `provisional PASS`（本轮唯一改动） |
| 11 | **#74 仍 OPEN，PR 未创建**；两个实施提交已在本分支但未开 PR | ⚠️ 交付流程下一步（主 agent / `github-operator`） |

> **未发现其他已知遗留问题**（除上表 11 项）：本轮改动均为文档状态与口径更正，不涉及源码、测试、fixture 或配置；所有数字与 `sha256` 均以报告与 `git show` 为准，未新增未经实测的断言。

---

## 2. 新增知识点 / 重点学习对象

### 2.1 评测方法论

- **“held-out”不等于“blind holdout”**：held-out 只说明“不在训练/调参样本里”，但只要规则作者**看过这批用例**并据此改规则，它就不再是盲测。本项目里 `H18` 的改词、`H06/H12/H18/H24/H28/H16` 的消歧补丁就是“污染”的可审计证据。对应位置：`docs/plans/b1-5c-tool-routing-fixes.md` §8.5 / §17.3、`docs/ai/reports/b1-5c-tool-routing-fixes.md` §0 第 4 条。**学习价值**：能否诚实标注“这个数字能声称什么”决定了评测结论是否可被外部复核。
- **post-fix regression vs. generalization estimate**：同一个 30/30，前者只能说明“在当前 fixture 上不回退”，后者才能支撑“泛化到未见查询”。写作时必须二选一，不能用前者冒充后者。
- **Wilson 置信区间与 n=30 的功效边界**：30/30 的点估计 100% 但 Wilson 95% 下界只有 88.6%——**点估计过线 ≠ 区间下界过线**。对应位置：本日志 §1.3 的 CI 列、报告 §1.1。**学习价值**：小样本评测里“100%”常被误读为“确定过门槛”，CI 是必要的诚实披露。

### 2.2 版本与冻结纪律（可审计性）

- **冻结哈希必须与内容编辑顺序一起看**：`docs/ai/golden/query-routing-heldout.b1-5c.json` 的 `annotates.sha256` 从 `9933b0cc…535f` 变成 `d471c57a…abe4`，说明**queries 文件在全量运行前被编辑过**——这正是“冻结”没有被遵守的直接证据。对应位置：`git show 734eef5 -- docs/ai/golden/queries-heldout.b1-5c.json`。**学习价值**：只要保留 hash 绑定 + 提交记录，“是否在跑前改过 fixture”是可被第三方判定的，不必依赖口头说明。
- **`git show --stat` / `git log --oneline` 作为证据而非 worker 口头摘要**：本轮状态回填里的分支名、两个提交、改动文件清单都来自 `git show --stat`，而不是转述。对应位置：本日志元数据表“分支 / 提交”。

### 2.3 文档状态机与“不发布内部记录”的分区写法

- **issue 文档的 `## GitHub issue title` / `## GitHub issue body` 与 `## Internal tracking` 分区**：前者是**可直接发布**的远端正文来源，后者是本地跟踪（实施状态、裁定、回填记录）。本轮只改后者，并用 `git diff -U0` 逐行确认 **body 区块零改动**。对应位置：`docs/issues/issue-74-b1-5c-tool-routing-fixes.md`。**学习价值**：文档既是“本地记录”又是“发布草稿”，分区是避免把内部未决假设带上 GitHub 的最低成本手段。
- **状态降级要留痕，不要静默改口径**：`PASS` → `provisional PASS` 的每一次更正都写入了 `docs/ai/README.md` §4 修订记录（r9）与计划 §16（r5），并保留 r8 的旧表述并标注“r9 更正”。**学习价值**：让后来的读者能看出“曾经声称过什么、什么时候因为什么被降级”，比直接覆盖更可信。

### 2.4 评测契约与指纹（本轮读代码时涉及的概念，未改动）

- **`schema_hash` / `contract_version` 的契约绑定**：`b1.5-tools/v2 → v3` 只去掉 `year` 的 `maximum: 2024`，就使 `schema_hash`、`file_sha256` 全部变化，**v2 与 v3 的数字不可逐例比较**。对应位置：`docs/ai/README.md` §2.3、计划 §5.2。**学习价值**：评测口径是“契约的一个版本”，改契约就要声明新旧数字不可比。
- **`annotation_guard`（执行流不读 golden 标注）**：执行只允许用**从查询文本抽出的槽位**，golden 标注只进 `score_case`，7 份 run record 都记为 `annotation_guard = true`。对应位置：报告 §6、计划 §6.1。**学习价值**：这是“评测不能偷看答案”的可执行化（哨兵断言），比口头约定强。

---

> **本日志不声称**：held-out `G1-T` 的 30/30 是盲测或泛化估计、泛化 ≥90% 已达成、B-① 已开始或已交付、`G3`/MCP 已做、`OPEN-C3`/`OPEN-C11` 已裁定、Q48 回归已修复、中文解析已达某成功率（n=2）、#74 或 #72 已关闭、PR 已创建或已推送。

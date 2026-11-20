# B1.5c — B 路线评测层修复 + 查询时片名解析（G1-T）+ held-out 重测（**事后可见 → 非盲测**）

> **文档状态：🟡 评测已完成（r5，**provisional**）/ held-out **非盲测** / blind holdout 待做（B1.5d）/ [issue #74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74) **保持 OPEN**。** 本文件原是**方案文档**（r4 状态 = 已批准 / 已建 issue / 待实施）；r5 回填了**实际执行结果**：worker 已在分支 `codex/b1-5c-tool-routing-fixes` 完成评测层修复与 `G1-T`（提交 `78f5740`、`734eef5`），并已跑完 in-sample 与 held-out 全量（报告 `docs/ai/reports/b1-5c-tool-routing-fixes.md`）。但 **held-out 30 条在 stage 1c 中可见并被用于修规则**（H18 查询文本被改词；H06/H12/H18/H24/H28/H16 的 pattern/年份消歧在看过该 fixture 后补），因此 **主门槛 30/30 = 100% 只能记为 provisional PASS / post-fix regression，不是独立盲测、不是泛化证据**；要声称泛化 ≥90% 必须另建 **blind holdout（B1.5d，未做）**。规范记录 = `docs/issues/issue-74-b1-5c-tool-routing-fixes.md`。实测数字与计划-实现差异见 **§17**，Open 项见 §14。
>
> | 元数据 | 值 |
> | --- | --- |
> | 文档类型 | 计划 / 方案 + **实施结果回填**（**评测层修复 + 查询时片名解析变体 + held-out 重测**；仍然**只做 B 路线**、**不改 `src/main`**；r5：**结果性质 = provisional / 非盲测**） |
> | `last_updated` | 2026-11-19（**r5**：回填**实际执行结果**——评测已完成但 **provisional**、held-out 在 stage 1c 可见并用于修规则故**非盲测**、blind holdout（B1.5d）**待做**、#74 **保持 OPEN**；新增 §17 记录实测数字与计划-实现差异，§14/§16 同步。r4：回填 **GitHub issue #74** 编号/URL/milestone/labels 与 issue 文档改名。r3：oracle B-1 → **主门槛 = held-out `G1-T` ≥90%（27/30 全分母）**、`G1` 为 baseline、in-sample `G1` 仅前后对照；oracle C10/B-2 → **`title-aliases.json` 冻结时为空**。r2 变更见 §16） |
> | 状态 | 🟡 **评测已完成 = provisional（r5）/ held-out 非盲测（stage 1c 可见并用于补路由词与消歧）/ blind holdout B1.5d 待做 / [issue #74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74) 保持 OPEN**（**不声称**已关闭、已交付或已泛化） |
> | 基线 / 实施分支 | `main` @ `f84746b`（B1.5c 开工时的基线）；**实际实施分支 = `codex/b1-5c-tool-routing-fixes`**（`78f5740` 加 held-out fixture + 契约 v3；`734eef5` 评测层修复 + `G1-T` + in-sample/held-out 运行） |
> | 前置任务 | `docs/plans/b1-5b-tool-routing.md`（🟢 已批准 / **评测已完成，G1 82.7% FAIL**）；报告 `docs/ai/reports/b1-5b-tool-routing.md` |
> | 上级（umbrella） | `docs/plans/b1-5-movie-retrieval-eval.md`（共享数据集 / 共享指标 / 决策标准） |
> | 关联规范 | `docs/architecture/ai-user-workflows.md`（§3 U1 中文译名策略、§3.1 年份三态、§4.2 `GenreEnum` 与多值语义、§1.6 B1.5b 回填、§9 `WU-9`） |
> | 关联 GitHub issue | **前序 #72（OPEN，未关闭）** — `docs/issues/issue-72-b1-5b-tool-routing.md`（正文 `Refs #72`、不关闭）；**本任务 = [#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74)（OPEN，评测已完成但 **provisional**、**保持 OPEN**）** — 规范记录 = `docs/issues/issue-74-b1-5c-tool-routing-fixes.md` |
> | 关键证据 | `.pi/artifacts/scout-b1-5c.md`（2026-11-19 只读 SQL 实测）；`.pi/artifacts/oracle-b1-5c-review.md`（5 P1 / 7 P2）；`.pi/artifacts/oracle-b1-5c-delta.md`（增量复核：P1/P2 全关闭 + 两条阻塞 **B-1 主门槛变体** / **C10 别名表来源**）；B1.5b 报告 §6/§7/§13；架构文档 §1.6 |
> | 产物 | 计划：报告 + held-out fixture + 别名/解析数据文件（见 §9）。**实际已产生（r5）**：`docs/ai/reports/b1-5c-tool-routing-fixes.md`、`docs/ai/golden/queries-heldout.b1-5c.json`（`d471c57a…abe4`）、`docs/ai/golden/query-routing-heldout.b1-5c.json`（`5608dd4c…4aba`）、`docs/ai/aliases/*.json` ×3、`scripts/eval/check_heldout.py`；运行产物在 `.pi/artifacts/b1-5c-eval/`（不入库） |

---

## 1. 背景与证据

### 1.1 B1.5b 的实测结果（前置事实，不重复测量）

| 变体 | 任务级成功 | 门槛 | 判定 |
| --- | --- | --- | --- |
| **G1（确定性路由前置）** | **62/75 = 82.7%** | ≥90%（`OPEN-b1`，≥68/75，差 **6 例 / 7.3pp**） | **FAIL** |
| G0（纯 LLM 路由） | 39/75 = 52.0% | 不得作为交付形态（D-9） | 一致 |
| G2（G0 + schema 校验，回放） | 38/75 = 50.7% | 仅解释 | 无改善（−1 例） |
| G3（服务端渲染 + 一次重试） | **未测** | 本期可选（`OPEN-b4`） | — |
| **G1-T（本轮新增候选变体 = 交付候选）** | **未实施、未测** | ≥90%（27/30，**主门槛**，见 §4.5 / §10 `B15c-11`） | — |

- 分意图任务级成功：拒绝 6/6、澄清 4/4、多槽位 5/5、筛选 19/20、片名 12/15、偏好推荐 9/10、**相似 7/15**。
- 路由层（工具/槽位选择，不含任务成功）**74/75 = 98.7%**；接地 75/75；`rating: null` 未转 0（133 行）；`find_similar_movies` 排除 seed 15/15。
- 但 G1 的 82.7% 是**同集 in-sample**：拒绝词典、类型别名表、年代解析、`SIMILAR_MARKERS`、种子解析策略都是**在这 75 条上反复调优**得到的（报告 §11.1）。

### 1.2 G1 的 13 条失败与可修复性（scout §3 逐条 SQL 验证）

| 分组 | 用例 | 条数 | 机制 | 修复层级 |
| --- | --- | --- | --- | --- |
| **A. 相似/推荐执行器语义** | Q12、Q16、Q19、Q35、Q36、Q37、Q55、Q56 | **8** | 多值 `genres[]` 走 OR（golden 要 AND）+ 替身排序无"同导演"优先 + 种子被 title 截断挤掉 | **评测层**（不改 `src/main`） |
| **B. 中文译名** | Q21、Q22、Q23 | 3 | 目录**无中文**（`movies.title/director`、`stars.name` 均无真 CJK） | **C4=D：查询时解析（§4）** |
| **C. 类型语义变体** | Q11 | 1 | `Sci-Fi`(457) vs `Science Fiction`(2)；golden 是两值并集 | 评测层（canonicalize + 多值语义） |
| **D. 判分口径** | Q57 | 1 | `The Shining` 两行（`StK15` 1979 Kubrick / `MkG15` 1997 Garris）；路由器按 R2-3 **正确地澄清**，但 golden 可接受集是 Kubrick 12 部 → 判失败 | **判分口径决策**（§5.3） |

**A 组可修复性（scout §3.1/§3.3，SQL 实测）**：

- 把 golden 的 `result_constraints` 按 **AND** 施加后，15 条 similar 中 **11 条**可接受集合**逐条精确复现**：**Q13、Q14、Q15、Q33、Q34、Q36、Q37、Q53、Q54、Q55、Q57**（scout §3.1 表中逐条 ✅ 的 11 条）；10 条 recommend 为 **10/10**（Q17/Q18/Q19/Q20/Q38/Q39/Q40/Q58/Q59/Q60）。
  - **数字裁定（oracle P1-4）**：scout 的**汇总句**写“12/15”，但其**逐条表**只点名 **11** 条（Q16 为 ❌、Q12/Q35/Q56 为 `rc = null`）→ **本计划一律使用 11/15**，不使用 12；若后续要写 12，必须**点名第 12 条并附 SQL 输出**，否则以 11 为准。
- 剩余 3 条（Q12/Q35/Q56，`result_constraints = null`）与 Q16（单 genre、池 456 部）需要**同导演信号**作为收缩/排序条件。
- **关键反例**：若只加"同导演优先"排序而**不先按约束 AND 收窄**，Q13/Q14/Q15/Q33/Q34/Q37/Q53/Q54 的 top-5 命中会掉到 **1/3、1/3、0/3、0/3、0/4、0/4、0/3、0/4**（scout §3.2）→ **顺序不可颠倒**（见 §6、R-3）。

### 1.3 三条此前未记录的数据事实（影响 held-out 设计）

1. **年份极度偏斜**：2000s = **9,069** 部（占 43%）、2010s = **94**、2020s = **7**；1950s–1990s 各 1,285–2,370 部（scout §2.1）。
2. **2020s 的 7 行里 5 行是合成数据**：`database/dev-seed.sql` 注入 `tt9000001…tt9000008`；**B1.5b golden 的 Q11/Q39/T14 可接受集合依赖这些行**（scout §2.4）→ held-out **不得**依赖它们。
3. **导演/片名形态脏**：10,888 个 distinct director 值，其中 **1,398 个姓氏下有 >1 个字符串**；788 个 `X.Lastname` 缩写式；30 个含 `~`（`Woody~Allen`）；**950 个 title 有 >1 行**（2,120 行）（scout §2.2/§2.3）。

### 1.4 为什么必须重做一套独立样本

- 现有 60 + 15 条已被规则开发反复使用（报告 §11.1、架构文档 §1.6），**不可清洗**；继续在同一批上调参只会重复"in-sample 数字漂亮、泛化未知"的问题。
- 因此 B1.5c 的交付价值 = **① 修掉可修的评测层缺陷；② 用从未参与规则开发的 30 条 held-out 给出一个诚实的泛化估计；③ 用 G1-T 回答"中文译名能否在查询时被可靠解析"**。
- **不声称**：修完就一定能过 90%；held-out 与 G1-T 的真实数字在跑之前**未知**。

---

## 2. 目标与非目标

### 2.1 目标（一句话）

> 在不改生产代码的前提下，修掉 B1.5b 暴露的**评测层语义缺陷**（多值类型 AND、相似/推荐排序、种子消歧、类型与人名归一），按用户决策 **C4=D** 增加一个**查询时片名解析变体 G1-T**（别名/缓存 → 本地 LLM 生成英文候选 → 目录 exact/prefix 校验 → 可选 embedding/模糊 → 澄清），然后用**冻结的 30 条 held-out** 重测 G1 与 G1-T（r1–r4 原写“独立 held-out”；**r5 据实修正：该 fixture 在 stage 1c 已可见并用于修规则，故不是独立盲测**，见 §17.3）。

### 2.2 In scope

1. **评测层修复**（只在 `scripts/eval/`、`docs/ai/` 内）：`filter_movies`/`recommend_movies` 多值类型语义、`find_similar_movies` 排序、`search_movies_by_title` 匹配优先级、导演归一、类型别名补全与 canonicalize。
2. **契约裁定落地**（文本 + 必要 fixture 变更）：多值 `genres[]` 语义、`year` 边界 vs `OUT_OF_RANGE`、Q57/澄清类判分口径、别名的"召回 vs 判定"边界。
3. **别名/归一数据独立成文件**：`docs/ai/aliases/*.json` + 版本号；**只做库内可推导归一**，不引入任何外部中文数据。
4. **C4=D：查询时片名解析（G1-T）**：评测层实现解析流水线（§4）、指标（§4.5）、缓存与超时降级（§4.6）；**只在片名槽位含 CJK 时调用模型**。
5. **运行入口可执行**：`tool_eval.py` 新增 `--queries-path` / `--routing-path`（默认仍指向冻结的 60 条）与 `--variant G1-T`（oracle P1-1）。
6. **held-out 30 条**：新文件、独立 ID、独立冻结哈希、切片互斥于现有 75 条；主 agent 标注 + reviewer 抽查复核；其中 **2 条中文译名不在别名表**，专测翻译泛化。
7. **重跑与报告**：**`G1-T` held-out = 主门槛**、`G1` held-out = baseline、`G1` in-sample = 修复前后对照（必要时加 `G1-T` in-sample 对照）；G2（`--reuse-g0` 回放）；新报告 `docs/ai/reports/b1-5c-tool-routing-fixes.md`。
8. **文档同步**：`ai-user-workflows.md` §3 U1（中文译名策略改写为 C4=D 口径）、§3.1/§4.2（三态与规范化）、§1.6 追加 B1.5c、§8 变更清单、§9 `WU-9` 状态；issue #72 状态说明。

### 2.3 Out of scope（硬边界）

| 不做 | 理由 |
| --- | --- |
| **任何 `src/main/**`、`pom.xml`、CI 改动** | 生产相似/推荐与生产 title resolver 都未实现；B1.5c 仍只做评测 |
| **生产 title resolver 组件** | 属 B-① 的独立组件（§4.2）；本任务只**评测**一个评测层替身 |
| **MCP server** | D-6 已批准不做；若引入另立安全验收 issue |
| **G3（服务端渲染 + 一次重试）** | `OPEN-b4` 已裁定本期可选；未做须写"未测" |
| **外部中文别名数据**（Wikidata / TMDB / IMDb akas） | `WU-9` 要求**另立 issue 批准**（新数据来源 + 许可）；C4=D **刻意避开**该路径 |
| **手工编造中文别名映射** | 别名表/缓存只接受**目录校验通过**的条目（§4.4）；held-out 查询标注**允许且必需**（不是"编造别名"） |
| **生产相似/推荐信号实现**（同导演、共享 star 权重等） | 属 B-①；本任务只改**评测替身**，且报告必须声明替身 ≠ 生产 |
| 修改 B1.5b 报告 / `queries.b1-5.json` / `query-routing.b1-5.json` / `tool-cases.b1-5.json` / `docs/ai/README.md` | 冻结产物与 B1.5a 配对基线；任何改动都会破坏 `annotates.sha256` 绑定与 A/B 可配对 |
| 修改历史日志、umbrella 已完成段落 | 只在必要时追加 dated 段落 |

---

## 3. 总体候选方案与取舍

| 方案 | 内容 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- | --- |
| **A（推荐）评测层修复 + G1-T + held-out（r5 注：实际**非盲测**）** | 只改评测脚本/fixture/文档；held-out 冻结重测；中文译名走查询时解析 | 不碰生产代码、可回退、可复现；同时回答"缺陷修完后泛化是多少"与"中文译名能否解析" | 结论仍是"替身执行器 + 替身解析器下"的数字；G1-T 引入模型不确定性；**r5 实况：该 fixture 在 stage 1c 可见并用于修规则 → 只能算 post-fix regression（§17.3）** | **推荐** |
| B 评测层修复 + 生产相似/推荐实现 | 把同导演/共享类型信号落进 `src/main` | 结论更接近生产 | 超出本任务与 #72 范围；需要新生产 issue、集成测试、契约一致性测试 | **不采纳**（另立 issue） |
| C 只记录缺口、不修 | 报告里写"已知缺口 13 条"，不动代码 | 成本最低 | 8 条可修缺陷留着；held-out 无法回答"修完如何"；#72 无法推进 | **不采纳** |
| D 先补中文别名数据再重测 | 引入外部/人工中文别名表 | Q21–Q23 可回收 | 未批准的数据来源 + 许可（`WU-9`）；会拖住 8 条**本来可修**的缺陷 | **不采纳**（与用户 C4=D 的"查询时解析"不同：D 方案依赖**外部数据**） |

> 注意：§3 的 "D 方案"指**外部中文别名数据**这一总体路线，与 **用户决策编号 C4 = D**（查询时解析）不是同一件事；C4 的选项对比见 §4.2。

---

## 4. C4=D：查询时片名解析（用户已批准方向；oracle 增量复核已确认实现细节，见 §15）

### 4.1 决策记录

| 项 | 内容 | 来源 |
| --- | --- | --- |
| 决策 | **C4 = D：查询时片名解析（hybrid）** | 用户（2026-11-19，经主 agent 转达） |
| 含义 | 中文片名不在目录内时，**在查询时**用"确定性别名/缓存 → 本地 LLM 生成英文候选 → 目录校验 → 可选模糊 → 澄清"解析，而不是引入外部/人工中文别名表，也不是接受缺口 | 同上 |
| 影响 | 新增评测变体 **`G1-T`**；中文片名用例的 `model_calls > 0`；新增解析类指标（§4.5）；`ai-user-workflows.md` §3 U1 与 §9 `WU-9` 需要改写 | 本计划 §4/§9/§10/附录 B |
| 别名表来源 | **C10 已裁定：`title-aliases.json` 冻结时为空**（`entries: []`）；条目只来自运行期“目录校验通过”的解析结果，**不得**预置已知译名（Q21–Q23） | 主 agent（2026-11-19，按 oracle C10/B-2 建议） |
| 仍未定 | 解析成功率/错译率是否要**设门槛**（`OPEN-C11`，建议不设） | §14 |

### 4.2 与 A/B/C 的对比：为什么选 D

| 选项 | 内容 | 优点 | 缺点/风险 | 用户裁定 |
| --- | --- | --- | --- | --- |
| A | 评测 fixture 内加 3 条人工别名（fixture-only） | Q21–Q23 立刻可回收 | 评测测了一份**生产不存在**的数据 → 数字失真；本质是"改答案" | 不采纳 |
| B | 接受 3 条中文失败 + 报告含/不含两个数 | 零新数据、最诚实 | 中文能力永远为 0；held-out 门槛几乎要求其余零失误 | 不采纳 |
| C | 引入外部中文别名数据（Wikidata/TMDB/IMDb akas） | 覆盖最好 | 新数据来源 + 许可（`WU-9` 要求另立 issue）；IMDb akas 可 `tconst` 链接率仅 ~43% | 不采纳（另立数据 issue） |
| **D（用户选定）** | **查询时解析（hybrid）**：别名/缓存 → 本地 LLM 候选 → 目录校验 → 可选 embedding/模糊 → 澄清 | 不引入外部数据/许可问题；候选**必须**通过目录校验，等价于"不许猜"的可执行版本；可缓存、可回退、可度量；生产形态（B-① 的 title resolver）与本评测同一形状 | 引入模型调用 → 不确定性、延迟、错译；需要新的指标与更严的审计 | ✅ **C4 = D** |

**为什么 D 优于 A/B/C**：

1. **A 是伪造数据**，会把评测变成"读答案"；**B 放弃能力**；**C 触碰未批准的许可边界**。
2. D 把"是否允许翻译"从**数据问题**变成**可验证的解析问题**：LLM 只负责**提候选**，是否采信由**目录 exact/prefix 校验**决定 → 译名不会被当成"用户给的原名"直接透传（这正是 §3 U1 现行禁令要防的事，见 §4.7）。
3. D 同时给出**路由成功率与解析成功率**两个正交指标（§4.5），不会用解析的功劳掩盖路由的缺陷，也不会反过来用路由的 98.7% 掩盖中文解析能力。

**与生产的关系（必须写进报告）**：生产 **B-①** 的 title resolver 是**独立组件**（在 `src/main` 内，不在本任务范围）；B1.5c 只做**评测层替身**并给出指标。因此：

- 本任务的 G1-T 数字 = "在评测替身解析器 + 本地 `qwen2.5:7b` + 目录校验下的中文解析能力"，**不等于**生产 resolver 的 SLA；
- 评测替身与生产 resolver 的**形状**（五步、校验前置、超时降级、缓存）应保持一致，以便 B-① 直接复用评测结论；**但 B-① 的实现、性能、并发、缓存存储、观测都不在本次范围**。

### 4.3 `G1-T` 解析流水线（五步，顺序固定）

| 步 | 名称 | 输入 → 输出 | 是否调用模型 | 失败时的行为 |
| --- | --- | --- | --- | --- |
| 1 | **别名/缓存命中** | 归一化后的中文片名 → `movie_id` | 否 | 未命中 → 步 2 |
| 2 | **本地 LLM 生成英文候选** | 中文片名 → **严格 JSON** `{"candidates": ["Title A", ...]}`，**1–3 个**，`temperature=0` | **是**（仅此步） | 非法 JSON / 0 候选 / 超时 → 步 5（澄清）；**不得**把未校验候选当用户输入 |
| 3 | **目录 exact/prefix 校验** | 候选 → 目录中 exact 命中优先，其次 prefix；命中才允许调 `search_movies_by_title` | 否 | 全部未命中 → 步 4；部分命中但**多个不同 `movie_id`** → 交给 §7 的消歧/澄清 |
| 4 | **embedding / 模糊候选（可选）** | 中文片名 → 目录 title 的模糊/向量候选 | 可能（`qwen3-embedding`，**默认关闭**） | 关闭或未命中 → 步 5 |
| 5 | **澄清** | 请用户给英文原名，或列出候选（片名+年份+导演）+ **一个问题** | 否 | 终态；计为**解析失败**、但**行为正确**（按 R2-3） |

**硬约束**：

1. **只有片名槽位含 CJK 且目录无 exact 命中时才允许调用模型**；英文/类型/年代/导演查询的 `model_calls` 必须为 **0**（断言，`B15c-7`）。
2. LLM 输出必须**严格 JSON、1–3 个候选、纯英文标题**；解析失败/字段多余/候选超限 → 视为该步失败（不得容错猜测）。
3. **校验前置**：未通过目录 exact/prefix 的候选**禁止**进入 `search_movies_by_title`；候选一律不得回写成"用户的原文"。
4. 步 4 的 embedding 是**可选增强**，默认关闭；启用时必须记录模型名/tag/维度，且**不得**引入外部 API。
5. 步 1 的"别名表"只接受**目录校验通过**的条目（§4.4）；held-out 的 2 条中文用例**冻结时不得**在表内。

### 4.4 别名表 / 缓存：来源与允许边界（防"伪造数据"）

| 数据 | 位置 | 允许内容 | 禁止 |
| --- | --- | --- | --- |
| **title 别名表** | `docs/ai/aliases/title-aliases.json`（新，`aliases_version`） | 只允许**目录校验通过**的 `{zh_title → movie_id}`；每条含 `verified_by`（命令/SQL）与 `source`；**冻结时为空**（`entries: []`，**C10 已裁定**） | 外部数据源条目；未校验条目；把 held-out 的中文标题预先写入；**把 Q21–Q23 等已知译名预置成评测种子** |
| **运行期解析缓存** | `.pi/artifacts/b1-5c-resolver-cache.json`（不入库） | 一次运行内的候选/校验结果，用于**同 run 复用**与**回放** | 写入 DB；跨版本混用而不声明 |
| **类型/人名别名** | `docs/ai/aliases/genre-aliases.json`、`person-normalization.json` | 库内可推导归一 + 显式 `unsupported` 列表（§5.5） | 长尾/噪声名的伪造映射 |

- **C10 已裁定 = 空表**：`title-aliases.json` 冻结时为空（`entries: []`），**不预置** Q21–Q23 的 3 条已知译名；别名条目只能来自运行期“目录校验通过”的解析结果（缓存/记录），**不得**作为评测种子。因此报告里**不存在**“别名命中”这一中文恢复来源：中文恢复只能来自 `G1-T` 步 2 的 LLM 解析（步 1 在冷跑必然 miss）。若日后要预置条目，必须重新裁定并 bump `aliases_version`，且该次数字**不得**作为泛化证据。
- **held-out 主数字必须是冷启动运行**（别名表为空、缓存为空，走步 2），热运行只用于证明缓存不改变结论（§10 `B15c-15`）。

### 4.5 指标定义（路由与解析**分开**，不得合并成一个"成功率"）

| 指标 | 定义 | 分母 | 备注 |
| --- | --- | --- | --- |
| **路由成功率** | 工具/槽位选择正确（沿用 B1.5b 的路由层口径，**与解析结果无关**） | 全部 30 条 | 与 B1.5b 的 74/75 = 98.7% 同口径 |
| **解析成功率** | 中文片名最终解析到**正确 `movie_id`** | **触发解析的用例数**（含 CJK 片名），同时给 /30 | 终态澄清 = 解析失败（但行为可分列） |
| **任务级成功率** | `score_case` 的严格任务成功 | 30（全分母） | **主门槛只对 `G1-T` 生效**（交付候选）；`G1` 是 baseline 对照；C5：含译名，另给“不含译名”的 28 分母 |
| **错译率** | 拆两个子项：`candidate_reject_rate`（候选全部不在目录，被步 3 拒绝）与 `wrong_movie_rate`（通过校验但解析到**错片**） | 触发 LLM 的用例数 | `wrong_movie_rate` 是**红线指标**：任何 >0 都必须逐条归因 |
| **延迟** | 解析层 wall-clock **p50 / p95**，按步分列（模型调用 / 目录校验 / 模糊） | 触发解析的用例 | 冷启动（首次加载模型）单列，不与热态混合 |
| **缓存命中率** | 步 1 命中 / 触发解析的用例 | 同上 | 冷跑应为 0 |
| **fallback 率** | 进入步 4 或步 5 的用例占比 | 同上 | 超时/非法 JSON 单独计数 |

- **门槛（oracle B-1 已统一）**：**主门槛 = held-out `G1-T` 任务级 ≥90%（27/30，全分母）**——`G1-T` 是**交付候选**，只有它能解析 2 条中文译名用例；**held-out `G1` 是 baseline**（确定性路由、`model_calls = 0`、**不含中文解析**，2 条中文用例在 `G1` 下必然失败，理论上限 28/30）；**in-sample `G1` 只作修复前后对照**，不得用来宣称门槛达成。报告主表必须**同列 `G1` / `G1-T`** 并显式写“**交付候选 = G1-T；G1 不含中文解析**”，两列都同时给 **/30 与 /28（不含 2 条中文译名）** 两个分母与 **Wilson 95% CI**。
- **解析成功率与错译率是披露指标，本计划不新增门槛**（`OPEN-C11`：若要设门槛须用户/主 agent 裁定，避免“用口径提分”）。
- 所有指标必须与**变体名**一起出现（`G1` / `G1-T`），不得只写"成功率"。

### 4.6 成本、可复现性与降级

- **调用预算**：in-sample 3 条 + held-out 2 条中文片名 → 每次冷跑 **≤5 次 LLM 调用**（每次 1–3 候选、`temperature=0`、输出短）；**其余 25/72 条用例 `model_calls = 0`**。G1（确定性）与 G2（回放）仍为 0。
- **依赖变化**：G1-T 使 **Ollama `qwen2.5:7b` 成为本任务的必做依赖**（此前只有重跑 G0 才需要）；`qwen3-embedding` 仍可选。必须记录 `model` tag + digest；冷启动延迟（`OPEN-b2` 未测）单列披露。
- **可复现性**：`temperature=0` 仍非严格确定性 → 必须记录 `prompt_sha256`、候选、`resolver_step`、`model_digest`，并提供**回放**（`--replay-resolver <json>` 或缓存命中），使结论可被第三方复算。报告需给出**冷跑 + 热跑（缓存命中）**两次的任务级/解析成功率，证明缓存不改变结论。
- **超时降级**：单次模型调用超时（默认 8 s，可配置并写入 run record）→ 放弃该候选 → 步 5 澄清；**不得**退回"猜一个片名"或"把中文当英文传下去"。
- **失败可观测**：run record 每条保存 `resolver{step, zh_title, llm_candidates[], catalog_verified[], chosen_movie_id, model, model_digest, temperature, prompt_sha256, cache, latency_ms, timeout, degraded_to}`。

### 4.7 与现行规范的关系（必须同步改写，否则自相矛盾）

- `docs/architecture/ai-user-workflows.md` §3 U1 现写"目录只存英文原片名（无别名表）→ 不保证命中；**不得**自行把译名翻成英文后当成用户给的原名（属猜测）"。
- C4=D **不是**取消这条禁令，而是给它一个**可执行的替代**：LLM 只能**提候选**，候选必须**通过目录校验**才能进入 `search_movies_by_title`；校验失败 → 澄清。因此 §3 U1 需要改写为"**查询时解析（候选 + 目录校验）**，未校验候选不得当作用户原名"。
- `WU-9`（“别名表 = 新数据来源 + 许可，须另立 issue”）状态需更新为：**外部别名数据仍未批准**；C4=D 走“查询时解析”，**不引入外部数据**，因此不需要该批准；`title-aliases.json` 的条目来源边界已由主 agent 按 **C10 裁定：冻结时为空**（§4.4）。

---

## 5. 契约裁定项（C1 / C2 / C3 / C6 / C7）

> 以下均为**候选**，标"建议"的是 docs-writer 的推荐理由，**不是批准结论**；由 oracle 复核 + 主 agent 裁定。

### 5.1 `genres[]` 多值语义 + `Sci-Fi`/`Science Fiction` 规范化（`OPEN-C1`，含 oracle P2-1）

| 选项 | 内容 | 证据 | 影响 |
| --- | --- | --- | --- |
| **A（建议）统一 AND** | `filter_movies` 与 `recommend_movies` 的多值 `genres`/`preferred_genres` 一律"同时满足" | recommend **10/10**、similar **11/15** 用 AND 精确复现（scout §3.1/§3.3）；`_filter` 现状已是 AND（`HAVING COUNT(DISTINCT g.name) = len(genres)`） | Q19 直接修复；"喜剧或动作"这类 OR 意图**无法表达**（记为已知缺口） |
| B 统一 OR | 与 `constraint_sql` 现状一致 | Q19 继续失败（实测 5 行全错） | 不建议 |
| C 显式参数 `genre_match: "all"\|"any"` | 表达力最强 | fixture 现无该字段，`additionalProperties:false` 会拒 | 需要 schema bump + G0/G2 提示词同步；**本期不做**（记为未来项） |
| D filter=AND / recommend=OR（现状） | 一意图两语义 | 报告 R-1 已实测失败 | 不可取 |

**`Sci-Fi` / `Science Fiction` 的处理（修正 oracle P2-1）**：

1. **先 canonicalize 再 AND**：`canonical_genres()` 把两值映射到**同一个 canonical key**（casefold + `-`/空格统一 → `sci-fi`），**对 canonical key 去重后**再做"多值 AND"。
2. **禁止字面展开成两个 AND 条件**：把"科幻"展开成 `{Sci-Fi, Science Fiction}` 后逐一 AND 会要求同时含两值 → 0 行（`Sci-Fi` 457 部、`Science Fiction` 2 部）。
3. SQL 实现形态：`IN ('Sci-Fi','Science Fiction')` 用于**取行**，`COUNT(DISTINCT canonical(g.name)) = len(canonical_keys)` 用于 **AND 判定**；同一 canonical key 的多行只计一次。
4. `docs/ai/aliases/genre-aliases.json` 显式记录 `canonical_groups: [["Sci-Fi","Science Fiction"]]`，可审计。
5. **不改 fixture 文本**（语义写入 `ai-user-workflows.md` §4.2 的服务端语义）→ `schema_hash` 不变，报告可与 B1.5b 逐例对照。

### 5.2 `year` 边界 vs `OUT_OF_RANGE`（`OPEN-C2`，含 oracle P2-3）

| 选项 | 改动 | 代价 | 结论 |
| --- | --- | --- | --- |
| **A（建议，按 oracle P2-3 修正）只去掉上界** | `tool-schema.b1-5.json` 的 `year`(57-61)/`year_from`(171-176)/`year_to`(177-182)/`year_range.{from,to}`(342-353) **保留 `type: integer` + `minimum: 0`，只删除 `maximum: 2024`** → bump `contract_version = b1.5-tools/v3` + 重算 `schema_hash`；`check_schema.py:198-204` 的边界断言改为"下限 = 0/1、**无上界**"，并区分 **schema 边界** 与 **覆盖边界**（`YEAR_MIN/YEAR_MAX = 196/2024` 仍用于**覆盖判定**、routing 标注边界 362-373 保留） | 中：fixture + validator + hash + 报告声明"与 B1.5b 不可逐例比较" | **建议**：只有这样三态（`INVALID_ARGUMENT`/`OUT_OF_RANGE`/`EMPTY_RESULT`）才**真正互斥且可达**；`2030` 合法进 schema，由服务端判 `OUT_OF_RANGE`；负数/荒谬年份仍被 `minimum` 挡住 |
| B 保留边界，改写为"覆盖检查先于 schema 校验" | 只改文本 | 低（不动 fixture/hash） | 备选：契约仍自相矛盾（schema 说 2030 非法，规范说 2030 应得 `OUT_OF_RANGE`），只是把矛盾藏进执行顺序 |
| C 不动 | — | 零 | 不采纳（`coverage_confusion` 会继续出现在 G2/T10） |

**风险**：`contract_version`/`schema_hash` 变化会切断与 B1.5b 的 hash 可比性 → 报告必须**同时**给出"修复前 82.7%（v2 hash）/ 修复后（v3 hash）"，并声明这是**不同契约版本**下的数字。

### 5.3 Q57 / 澄清类判分口径（`OPEN-C3`）

| 选项 | 内容 | 影响 |
| --- | --- | --- |
| A 现状（澄清 = 任务失败） | `behavior_ok=True, task_success=False` | 与 R2-3（不得任选其一）**直接冲突**，系统性压低"依法澄清" |
| **B（建议）新增 `allows_clarification` 标注 + 单列口径** | 仅对"查询文本**没有**消歧线索、golden 却假定某一片"的用例标 `true`：只要"列出候选（片名+年份+导演）+ 只问一个问题"即算**澄清式成功**；**严格任务成功仍为主口径**，两个数字并列 | 需扩 `docs/ai/golden-set.schema.json`（`queryCase.properties` 增加可选字段）+ `score_case`/`is_task_success`；**必须逐条写理由** |
| C 给消歧线索（"The Shining 1979 那部"） | 不改口径，靠实现消歧 | `StK15`（9 主演 / 1 类型）vs `MkG15`（0/0）确实可分，但用"主演数/类型数"当偏好是**不可声明的启发式**，与 R7-2 冲突 |
| D 从分母剔除 | 75→74 | 报告已声明"不换分母"，需用户裁定 |

**建议 B**，并附加两条硬约束：① `allows_clarification` 只允许在**查询文本无消歧线索**时使用，标注必须写"为什么文本不足以消歧"；② **主门槛数字用严格口径**，澄清式成功只作并列披露。

### 5.4 别名的两个用途：召回扩展 vs 判定集合（`OPEN-C6`，含 oracle P1-5）

- **事实**：`constraint_problems()`(418) 现用 `normalized()` **精确**比较 director；而人名别名扩大会**改变集合大小**——Q17 的 golden 是 `Steven Spielberg`（**5** 部），库里 `Spielberg`（**29** 部）是**另一个集合**（scout §2.2/§4.2）。
- **裁定建议（按 oracle P1-5 收紧）**：

| 规则 | 允许 | 禁止 |
| --- | --- | --- |
| 等价判定 | **姓相同 且 首字母一致** 的"缩写 ↔ 全名"（`M.Mann` ↔ `Michael Mann`）；`~`→空格；casefold；`jr./sr.` 归一 | **纯姓氏扩展**：`Spielberg` ≠ `Steven Spielberg`；`Mann` ≠ `Michael Mann` 的"任意 Mann" |
| 用途 | 纯姓氏仅用于**召回扩展/展示**（候选列表、提示） | 纯姓氏用于**判定集合**（`result_constraints.director` 的相等判定） |
| 例外 | 库内该姓氏下只有**一个**字符串时，等价判定自然成立（如 `Hitchcock` 唯一） | 用"年份/片名"去猜某个缩写对应谁（属猜测，应澄清） |

- **必须写成断言**（`B15c-6`）：Q17 的 `Steven Spielberg` = **5** 部、`Spielberg` = **29** 部，两者**不相等**；`M.Mann` 与 `Michael Mann` 归一后相等；`Mann` 与 `Michael Mann` **不**相等。

### 5.5 别名词表的数据形态 + `unsupported` 的可执行断言（`OPEN-C7`，含 oracle P2-5）

- **建议**：独立数据文件 `docs/ai/aliases/genre-aliases.json`、`docs/ai/aliases/person-normalization.json`、`docs/ai/aliases/title-aliases.json`，各带 `aliases_version`；脚本读取，不再硬编码（现 `GENRE_ALIASES` 在 `tool_eval.py:101`，43 词形 / 17 目标）。其中 `title-aliases.json` 按 **C10 裁定冻结时为空**（`entries: []`），但 `aliases_version` 与 `verified_by` / `source` 字段仍必须存在；条目只由运行期目录校验通过的解析结果产生（§4.4），**不预置** Q21–Q23。
- **`unsupported` 必须是断言，不是文字**：`genre-aliases.json` 增加 `unsupported: [...]`（scout 列出的 39 个长尾/噪声名），并在 `check_schema.py`（或 `check_heldout.py`）中断言：
  1. `unsupported` 中的任何值**不得**出现在 `aliases` 的**目标**里；
  2. `unsupported` 中的任何值**不得**出现在 `canonical_groups` 中；
  3. `aliases` 的**目标**必须全部是 `tools[].items.enum` 内的 56 个值之一（即不得凭空造 genre）。
- 理由：可审计、可声明"无外部数据"；避免"改词表 = 改脚本"的重跑歧义。

---

## 6. 相似/推荐执行（评测替身）与**数据流边界**（oracle P1-2）

### 6.1 两条数据流必须分离（硬约束）

| 流 | 输入 | 输出 | 允许读取 golden 标注？ |
| --- | --- | --- | --- |
| **执行流** | query **文本** → `route_deterministically()` 抽出的 slots/constraints（含 §4 的解析结果） | 工具调用 + 结果行 | **否** |
| **判分流** | 执行流输出 + golden（`required_slots` / `result_constraints` / `acceptable_movie_ids` / `allows_clarification`） | `score_case` 的通过/失败 | **是**（仅此处） |

- B1.5b 的 `result_constraints` 是 `query-routing.b1-5.json` 的**标注**；若 `_similar` 用它做收窄，就是把答案当 oracle（oracle P1-2）。
- **因此 §6.2 的"收窄"只用 router 从**query 文本**抽出的 constraints**；golden constraints **只**进 `score_case`。
- 若某条查询的 router **抽不出** constraints → **如实失败/澄清**，**不得**用 golden 补齐。
- **可执行断言**（`B15c-3`）：`test_executor_ignores_annotations()` —— 把 routing doc 的 `result_constraints`/`acceptable_movie_ids` 替换为哨兵（访问即抛错/计数），断言执行路径 0 次访问；另可加 `--assert-no-annotation-read` 运行开关。

### 6.2 执行顺序（必须按此顺序实现）

1. **先用 router 抽出的约束收窄池**：`genres` 按 §5.1 的 canonicalize + AND、年份窗口、导演按 §5.4 归一后匹配（`constraint_sql`(447) 现用 OR，需改 AND）；
2. **再在池内排序**：`同导演(归一后) DESC > 共享 genre 数 DESC > |year 差| ASC > rating DESC(NULL 最后) > title`（`_similar`(545)）；
3. **无约束 / 池为空时回退**：同导演优先（覆盖 Q12/Q35/Q56/Q16 这类 golden 规则是"同导演"、但标注里没写进 `result_constraints` 的用例）；
4. **必须排除 seed**（`seed.id ∉ results[].id`，已实测 15/15 成立，回退后不得破坏）；
5. `SIMILAR_POOL = 300`(139) 的池截断必须发生在**约束收窄之后**（否则大池用例仍会被截断误伤）。

**明确不做**：共享 star 作为相似信号（`stars3_sorted` 是**字典序前 3**而非业务 top-3，覆盖仅 84%，噪声大 → scout §5）、`same_primary_genre_only` 语义变更（`OPEN-b5`）。

### 6.3 已知限制（必须写进报告）

- B1.5b 的 `text_constraints()`(924) 与 `constraint_problems()`(418) **共用同一套正则**（"约束是否被文本表述"与"约束是否被满足"同源）→ 判分自洽循环仍未解决。本轮**只保证执行侧不读 golden**，判分循环作为**已知限制**披露，不在本轮修复（修它需要独立的标注复核，属另一任务）。

---

## 7. 种子消歧与 `search_movies_by_title`

| 修复 | 内容 | 对应失败 |
| --- | --- | --- |
| 匹配优先级 | `_search()` 的 `ORDER BY m.title, m.year` 改为 **exact > prefix > substring**，再按 year/director；保留 `limit` 语义 | Q36（`Heat`：exact 2 行、prefix 6、substring 32，现被 `A Corner in Wheat` 等挤掉） |
| 多候选 + 线索 | 存在 >1 个 exact 候选时，用查询里的**年份/导演线索**收窄；无线索则**列出候选 + 一个问题**（R2-3 现有行为，不猜） | Q36（`Heat` 1995 `MM33` vs 1972 `PMo15`）、Q57（`The Shining` 两行） |
| 不用"主演数/类型数"当偏好 | `StK15`(9 主演/1 类型) vs `MkG15`(0/0) 虽可分，但属不可声明的启发式 | Q57 走 §5.3 的口径决策 |
| 中文片名入口 | 含 CJK 的 title 槽位先走 §4 解析；解析产物是**英文候选/已校验 id**，`search_movies_by_title` 的 schema 语义（"catalog's English original form", `tool-schema` 55 行）**不变** | Q21–Q23、held-out 2 条中文用例 |

---

## 8. held-out 30 条设计

### 8.1 文件与 ID（**新文件，不改冻结集**）

| 文件 | 内容 |
| --- | --- |
| `docs/ai/golden/queries-heldout.b1-5c.json` | 30 条查询，ID `H01`–`H30`，`heldout_version = b1-5c-heldout/v1` |
| `docs/ai/golden/query-routing-heldout.b1-5c.json` | 30 条 golden 谓词 + `annotates.sha256` **绑定 held-out queries 文件**（不绑 `queries.b1-5.json`） |
| `docs/ai/golden-set.schema.json` | 扩展：新增 held-out query 分支（ID pattern `^H(0[1-9]\|[12][0-9]\|30)$`）+ 可选字段 `allows_clarification`；**不改**现有 `queryCase` 的 `^Q…$` 约束 |
| `scripts/eval/check_heldout.py`（新，stdlib-only） | held-out 自检：结构、ID 唯一、**切片互斥**、无合成行、**2 条中文用例不在别名表**、冻结 sha256 记录 |

> **为什么不追加进 `queries.b1-5.json`**：`check_schema.py` 断言 `query-routing.annotates.sha256 == sha256(queries.b1-5.json)`；追加会破坏 B1.5a 冻结集与 A/B 可配对（scout §1.3/§8）。`check_golden.py` 的 `EXPECTED_QUERIES=60` / `MIN_CJK_ALIAS_CASES=3` / `^Q…$` 也只适用于冻结集 → held-out 用**自己的**校验器。

### 8.2 配额（合计 30）

| 维度 | 配额 |
| --- | --- |
| track | zh_zh **10** / zh_en **10** / en_en **10** |
| 意图 | `similar_movies` **5**、`filter_catalog` **7**、`search_by_title` **5**、`recommend_by_preference` **3**、`clarify_or_refuse`（拒绝 **4** / 澄清 **2**）= **6** → 小计 26 |
| 交叉标记 | `multi_slot: true` **4** 条（意图计入 `filter_catalog`, `similar_movies`，**单独统计**，不另设意图）→ 合计 **30** |
| **中文译名（C4=D 核心）** | `cjk_title: true` **固定 2 条**（oracle P2-2：不是 3），**均不在** `title-aliases.json`，专测翻译泛化；至少 1 条是 `search_by_title` 主用例，至多 1 条作为 similar/recommend 的种子 |
| 已知缺口 | 上述 2 条**计入分母**（C5），另单列"不含译名"（28 分母）的数字（§10 `B15c-11`） |

### 8.3 切片互斥与污染控制（`check_heldout.py` 断言）

1. held-out 的 `acceptable_movie_ids` 与 60 条 queries + 15 条 tool cases 的 `acceptable_movie_ids` **无交集**；
2. held-out 种子标题**不是**现有 15 条 similar 的种子；
3. **禁止** `tt9000001…tt9000008`、`Movie1/Movie2`（`tt0499470/tt0499471`）等 `dev-seed.sql` 合成/测试行；
4. **年代分布**：1950s/1960s/1970s/1980s 为主，**必须含 1990s 3–5 条**（oracle P2-4：1990s 有 2,370 部真实数据，且 B1.5b 的 similar 大量在 1990s，全排除会降低代表性），2000s 可少量；**2010s 最多 1–2 条**（仅 94 部）、**2020s 0 条**（7 行里 5 行是合成数据）；
5. 类型优先**未被 fixture 使用过**的 **Fantasy 492 / War 125 / Sport 103 / History 215**（这 4 个在 B1.5c **会先补中英文别名**，否则会如实暴露缺口）；
6. 导演优先**未被使用**且**单一字符串**的 `Hitchcock 83 / Cukor 54 / R.Stevens 53 / Cline 44 / Curtiz 42 / Lubitsch 40 / B.Edwards 39 / DeMille 37`（其中 `R.Stevens`/`B.Edwards`/`J.Ford` 是缩写式，天然测归一）；
7. 至少 2 条用**重名标题**（950 个 title 有 >1 行）构造种子消歧；
8. 至少 2 条覆盖**0 genre 片（3,318 部）**或**无评分片（13,082 部）**的边界行为；
9. **中文译名防污染**：§8.2 的 2 条 `cjk_title` 标题**不得**出现在 `docs/ai/aliases/title-aliases.json`（冻结前断言；冻结后写入即视为污染，见 §8.5）。

### 8.4 标注与复核

- 每条必须有：`labeling_note`（为何这些结果可接受）、`snapshot_row_refs`（**可执行 SQL 谓词 + 行数 + ids**）、`acceptable_movie_ids`（SQL 实测，`queries.b1-5.json` 同格式）。
- **标注人**：主 agent（沿用 `OPEN-U3` 已关闭的单人标注口径）。**held-out 查询标注是允许且必需的**（与"不编造中文别名数据"是两件事）。
- **复核**：至少 1 名**未参与规则开发**的 reviewer 抽查 **≥10 条**（含全部 `allows_clarification` 与 `cjk_title` 条目），逐条给出"谓词/SQL/可接受集合"复核结论；报告记录复核范围与差异处理。
- **披露**：单一标注人只解决**规则污染**，不解决**标注偏差**（scout §6.3）→ 报告必须写这条限制。

### 8.5 一次性冻结纪律（硬约束）

- 跑之前把 held-out queries/routing、`title-aliases.json`、别名文件的 sha256 写进本计划附录 A + 报告；**跑完之后不得按失败用例回调规则**。
- **主数字必须来自冷启动运行**（别名表为空、解析缓存为空 → 走 G1-T 步 2）；热运行（缓存命中）只用于证明缓存不改变结论。
- 若必须修改 held-out：`heldout_version` bump（`v2`…），报告注明"**第 N 次运行**"，并保留每次运行的 hash 与数字；不得只报最后一次。
- **不得**用 held-out 的失败条目去改别名/排序/解析规则——那会使 held-out 立即失效（75 条的教训）。因故障重跑必须清缓存或带 `--no-cache`。

---

## 9. 拟修改文件（实施阶段；oracle 已复核，需主 agent 批准实施后才可交给 worker）

| 路径 | 动作 | 备注 |
| --- | --- | --- |
| `scripts/eval/tool_eval.py` | **改**：`--queries-path`/`--routing-path`/`--variant G1-T`/`--replay-resolver`/`--no-cache`；`load_fixtures()`(153)/`build_cases()`(164) 接受路径；`constraint_sql`(447) / `_recommend`(589) 多值 → canonicalize + AND；`_similar`(545) 先收窄后排序；`_search`(497) exact>prefix>substring；`extract_director`(773) + §5.4 归一；`GENRE_ALIASES`(101) → 读外部别名文件；`coverage_state`(389) 与 schema 校验顺序；`score_case`(949)/`is_task_success`(1012) 支持 `allows_clarification`；新增 G1-T 解析层（§4）与指标 | 唯一核心落点；**不改 `src/main`** |
| `docs/ai/aliases/genre-aliases.json`（新） | 类型别名 + `canonical_groups` + `unsupported` + `aliases_version` | 替代硬编码常量 |
| `docs/ai/aliases/person-normalization.json`（新） | 人名归一规则与例外（§5.4） | 只做库内可推导 |
| `docs/ai/aliases/title-aliases.json`（新） | 已校验中文片名 → `movie_id`；`aliases_version`；**冻结时为空（C10 已裁定，`entries: []`）** | 条目只来自运行期目录校验通过的解析结果；边界见 §4.4 |
| `docs/ai/golden/queries-heldout.b1-5c.json`（新） | 30 条 held-out 查询（含 2 条 `cjk_title`） | ID `H01`–`H30` |
| `docs/ai/golden/query-routing-heldout.b1-5c.json`（新） | 30 条 golden 谓词 | 绑定 held-out queries 的 sha256 |
| `docs/ai/golden-set.schema.json` | **改**：held-out 分支 + `allows_clarification` | 保留 `^Q…$` 原约束 |
| `scripts/eval/check_heldout.py`（新） | §8.3 的 9 条断言（含 CJK 不在别名表） | stdlib-only |
| `scripts/eval/check_schema.py` | **改**：`year` 边界断言 = "下限 0/1、无上界"（198-204）+ 区分 schema/覆盖边界；新增 `unsupported` 与 `canonical_groups` 断言；保留 `QUERY_IDS`(82)、`EXPECTED_GENRE_COUNT`(78)、routing 标注边界(362-373) | 必须同一次改动 bump `contract_version` + `schema_hash` |
| `docs/ai/tool-schema.b1-5.json` | **改**：`b1.5-tools/v3` + 只去 `maximum: 2024`（保留 `minimum`）+ 新 `schema_hash` | 报告声明与 v2 不可比 |
| `docs/ai/reports/b1-5c-tool-routing-fixes.md`（新） | 报告：in-sample 前后 + held-out（G1 / G1-T）+ 解析/错译/延迟/缓存指标 + 失败归因 + 已知缺口 + 复现命令 | 不改 B1.5b 报告 |
| `docs/architecture/ai-user-workflows.md` | **改**：§3 U1（C4=D 解析口径）、§3.1（覆盖判定位置）、§4.2（多值语义/canonicalize/别名边界）、§1.6 追加 B1.5c、§8 变更清单一行、§9 `WU-9` 状态 | §7.1 第 1/2/7 项触发，必须同 PR 更新 |
| `docs/plans/b1-5c-tool-routing-fixes.md`、`docs/issues/*`、`docs/logs/*` | 文档回填（docs-writer） | — |

**明确不得修改**：`src/main/**`、`pom.xml`、`.github/workflows/ci.yml`、`docs/ai/golden/queries.b1-5.json`、`query-routing.b1-5.json`、`tool-cases.b1-5.json`、`docs/ai/reports/b1-5b-tool-routing.md`、`docs/ai/README.md`、历史日志。

---

## 10. 验收条件（B15c，草案）

| ID | 条件 | 验证方式 |
| --- | --- | --- |
| **B15c-1** | 多值类型语义按裁定实现（建议 canonicalize + AND），并**逐条复现** scout §3.1/§3.3：similar **11/15**（Q13/Q14/Q15/Q33/Q34/Q36/Q37/Q53/Q54/Q55/Q57）、recommend **10/10** 的可接受集合精确匹配；`Sci-Fi`/`Science Fiction` 经 canonical key 去重后比较（**不得**字面展开成两个 AND 条件） | 固定断言 + 报告附 SQL |
| **B15c-2** | 相似排序按 §6.2 的**顺序**实现（router 约束先收窄 → 同导演>共享类型>年份>rating → 无约束回退同导演 → 排除 seed），且"只加排序不收窄"的反例不再出现 | 单测断言 + in-sample similar 通过数 |
| **B15c-3** | **执行阶段不得读取 golden 标注**：`result_constraints`/`required_slots`/`acceptable_movie_ids` 只进 `score_case`；哨兵测试证明执行路径 0 次访问；router 抽不出约束时如实失败/澄清 | 单测（哨兵）+ 代码评审 |
| **B15c-4** | `search_movies_by_title` 的 exact>prefix>substring 成立；`Heat` 的 2 个 exact 候选进入候选集，Q36 的种子解析不再落到 `PMo15` | 断言 + Q36 结果 |
| **B15c-5** | 别名独立成文件 + `aliases_version`；补 Fantasy/History/Sport/War；拆分 Musical/Music；`Sci-Fi`≡`Science Fiction`（canonical group）；`unsupported` 的 39 个噪声名**有可执行断言**（不得出现在 alias 目标或 canonical group） | 文件评审 + `check_schema.py` 断言 |
| **B15c-6** | 人名归一按 §5.4：只做"姓相同 + 首字母一致"的缩写↔全名归一；**禁止纯姓氏扩展**；断言 **Q17：`Steven Spielberg` = 5 部 vs `Spielberg` = 29 部，不相等**；`M.Mann` ≡ `Michael Mann` 但 `Mann` ≢ `Michael Mann` | 断言 + 报告说明 |
| **B15c-7** | **G1-T 按 §4.3 五步实现**：严格 JSON + 1–3 候选 + `temperature=0` + 目录 exact/prefix 校验前置 + 超时→澄清 + 缓存；**非 CJK 用例 `model_calls = 0`**（断言）；未校验候选不得进入 `search_movies_by_title` | 断言 + run record |
| **B15c-8** | **路由成功率与解析成功率分开报告**；错译率拆 `candidate_reject_rate` / `wrong_movie_rate`；延迟 **p50/p95**（按步）+ 缓存命中率 + fallback 率；`wrong_movie_rate > 0` 必须逐条归因 | 报告表 + run record |
| **B15c-9** | **held-out 命令可执行**：`tool_eval.py` 新增 `--queries-path`/`--routing-path`（默认仍指向冻结 60 条）与 `--variant G1-T`；报告粘贴实际 `--help` 片段 + 命令 + 输出；若改用 `eval_heldout.py` 必须显式说明 | `--help` 输出 + 运行记录 |
| **B15c-10** | held-out 30 条存在、ID `H01`–`H30`（10/10/10）、冻结 sha256 已记录、§8.3 的 **9 条**互斥/禁用在 `check_heldout.py` 中为**断言**并通过（含 2 条 `cjk_title` 不在别名表、含 1990s 3–5 条、无 2020s/合成行） | 脚本输出 |
| **B15c-11** | **主门槛只对 `G1-T` 生效：held-out `G1-T` 任务级 ≥90%（27/30，全分母）**；**held-out `G1` 为 baseline**（`model_calls = 0`、**不含中文解析**，2 条中文用例在 `G1` 下必然失败）；in-sample `G1` 仅作修复前后对照。报告主表**同列 `G1` / `G1-T`**，显式写“**交付候选 = G1-T；G1 不含中文解析**”；两列都同时给 **/30 与 /28**（不含 2 条中文译名）两个分母 + **Wilson 95% CI** | 报告主表 |
| **B15c-12** | `year` 契约按 §5.2 落地：只去 `maximum: 2024`、保留 `integer` + `minimum`，`contract_version = b1.5-tools/v3` + `schema_hash` 重算 + `check_schema.py` 同步；服务端 196–2024 之外判 `OUT_OF_RANGE`；T10 行为正确；报告声明与 v2 不可比 | 规范 diff + 用例结果 |
| **B15c-13** | in-sample（75 条）修复前后的数字并列（v2 hash vs v3 hash）；失败归因逐条说明；G2 用 `--reuse-g0` 回放（`model_calls = 0`）；G3/MCP 未做写"未测"/"未实现" | 报告 |
| **B15c-14** | 报告诚实区分 **in-sample / held-out / 评测替身执行器 / 评测替身解析器**，声明"替身 ≠ 生产相似/推荐"与"替身 ≠ 生产 title resolver（B-①）"；不得据 in-sample 数字宣称达标 | 报告 §边界 段 |
| **B15c-15** | 冻结与可复现：报告写出 held-out/别名文件 sha256、运行次数、`heldout_version`、`model` tag + digest、`prompt_sha256`；给出**冷跑与热跑（缓存命中）**两次的任务级/解析成功率，证明缓存不改变结论；冷启动延迟单列 | 报告 + 附录 A |
| **B15c-16** | 无生产代码/pom/CI 改动；无 MCP；无 DB 写入；`ai-user-workflows.md` 按 §9 触发项更新（§3 U1 / §3.1 / §4.2 / §1.6 / §8 / §9 `WU-9`，`last_updated`） | `git diff --stat` + 文件评审 |

> 门槛**不因结果调整**：held-out `G1-T` < 90% 时，报告如实给数字与归因，**不得**下调阈值、换分母、剔除失败条目（2 条中文译名已按 C5 计入分母）；也不得改以 `G1` 代替 `G1-T` 作为主结果。

---

## 11. 测试与验证计划（**参数需先实现**，见 `B15c-9`）

```bash
# 0) 实施后确认运行入口真的存在（oracle P1-1：当前 main() 无这两个参数）
python3 scripts/eval/tool_eval.py --help | grep -E -- "--queries-path|--routing-path|--variant"

# 1) 契约自检（文件级，无需 DB）
python3 scripts/eval/check_schema.py --skip-db
python3 scripts/eval/check_heldout.py --skip-db

# 2) 冻结集与快照一致性（需 MySQL 3307，只读）
python3 scripts/eval/check_golden.py
python3 scripts/eval/check_heldout.py          # 含 acceptable ids 存在性、污染与"CJK 不在别名表"断言

# 3) 干跑（无网络/无 SQL）
python3 scripts/eval/tool_eval.py

# 4) 脚本自检（脚本化工具调用，不是模型测量）
python3 scripts/eval/tool_eval.py --self-test

# 5) in-sample 重测（G1，model_calls = 0）
python3 scripts/eval/tool_eval.py --variant G1 --cases queries --allow-full-run \
  --out .pi/artifacts/b1-5c-g1-insample.json

# 6) held-out 重测（G1 确定性变体 = baseline，model_calls = 0）
python3 scripts/eval/tool_eval.py --variant G1 --cases queries \
  --queries-path docs/ai/golden/queries-heldout.b1-5c.json \
  --routing-path docs/ai/golden/query-routing-heldout.b1-5c.json \
  --allow-full-run --out .pi/artifacts/b1-5c-g1-heldout.json

# 7) held-out G1-T（**主门槛**：中文解析；冷跑：空缓存 + 空别名表；模型仅用于 2 条 CJK 用例）
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path docs/ai/golden/queries-heldout.b1-5c.json \
  --routing-path docs/ai/golden/query-routing-heldout.b1-5c.json \
  --no-cache --allow-full-run --out .pi/artifacts/b1-5c-g1t-heldout-cold.json

# 8) held-out G1-T 热跑（缓存命中，证明缓存不改变结论）+ 回放复算
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path docs/ai/golden/queries-heldout.b1-5c.json \
  --routing-path docs/ai/golden/query-routing-heldout.b1-5c.json \
  --allow-full-run --out .pi/artifacts/b1-5c-g1t-heldout-warm.json

# 9) G2 回放（model_calls 应为 0）
python3 scripts/eval/tool_eval.py --variant G2 --cases queries --reuse-g0 .pi/artifacts/b1-5c-g0.json \
  --queries-path docs/ai/golden/queries-heldout.b1-5c.json \
  --routing-path docs/ai/golden/query-routing-heldout.b1-5c.json \
  --allow-full-run --out .pi/artifacts/b1-5c-g2-heldout.json

# 10) G0（可选；如需新基线才跑，需 Ollama qwen2.5:7b）
```

- **成本**：G1 in-sample/held-out **`model_calls = 0`**；G1-T **仅 2 条 held-out CJK（+3 条 in-sample，若跑对照）**调用模型，其余为 0；G2 用 `--reuse-g0` 零模型调用。
- **先决**：Ollama `qwen2.5:7b` 必须在位（G1-T 的新增依赖）；`qwen3-embedding` 仅在显式启用步 4 时需要。
- **验收证据形态**：`.pi/artifacts/*.json` 运行记录（不入库）+ 报告表格 + `check_*.py` 输出。

---

## 12. 依赖、工期

### 12.1 依赖

- Docker MySQL `fabflix-mysql`（宿主 3307，只读）——现状快照 21,087 部 / 196–2024 / 56 distinct genre。
- **Ollama `qwen2.5:7b`**（G1-T 步 2 必需；记录 tag/digest）；`qwen3-embedding` 可选。
- 主 agent 完成 30 条 held-out 标注（工作量主要在这一项）。
- oracle 风险关卡：r2 的 5 P1 + 7 P2 已由 `.pi/artifacts/oracle-b1-5c-delta.md` 复核为**全部关闭**，其**两条阻塞 B-1（主门槛变体）与 C10（别名表来源）已按主 agent 裁定在 r3 落实**；仍需主 agent 对 `OPEN-C1`/`OPEN-C2`/`OPEN-C3`/`OPEN-C6`/`OPEN-C7` 的意见做最终记录。

### 12.2 工期（粗估；不含 oracle/用户等待时间）——**5–7 人日**（oracle：4–5.5 偏乐观）

| 工作项 | 估时 |
| --- | --- |
| 评测层语义修复（canonicalize+AND、先收窄后排序、搜索优先级、seed 消歧）+ **执行/判分数据流拆分** + self-test 断言 | 1.0–1.5 人日 |
| **G1-T 解析层**（LLM 候选 + 目录校验 + 缓存 + 超时降级 + 解析指标） | 1.0–1.5 人日 |
| 运行入口与回放（`--queries-path`/`--routing-path`/`--variant G1-T`/`--replay-resolver`/`--no-cache`） | 0.5 人日 |
| 别名/归一数据文件 + `unsupported`/`canonical_groups` 断言 | 0.5 人日 |
| 契约落地（§3.1/§4.2 spec + v3 fixture bump + `check_schema.py` 同步） | 0.5 人日 |
| held-out 30 条标注 + SQL 复核 + 冻结哈希 + `check_heldout.py` | 1.0–1.5 人日 |
| reviewer 抽查（≥10 条）+ 重跑（in-sample / held-out / G1-T 冷热 / G2 回放）+ 报告 | 0.5–1.0 人日 |
| **合计** | **5.0–7.0 人日** |

> 若 `OPEN-C4` 改回 C（外部中文数据）或加入 G0 冷启动实测，需另计并另立 issue。G3 不计入关键路径。

---

## 13. 风险与回退

| ID | 风险 | 概率/影响 | 缓解 | 回退 |
| --- | --- | --- | --- | --- |
| R-1 | **held-out 门槛过紧**（**主门槛只对 `G1-T` 生效**；`G1` 是 baseline，2 条中文必然失败 → `G1` 理论上限 28/30）：2 条中文译名必须靠 LLM 解析成功，否则几乎无余量(2 条全败 → 28/30 = 93.3% 仍可过;1 条败 + 1 条其他失败 → 28/30;2 条都败 + 2 条其他失败 → 26/30 = 86.7% FAIL);n=30 的 Wilson 95% CI 很宽(27/30 = **[74.4%, 96.5%]**) | **高**/高 | 明示 C5 口径(计入分母);单列 28 分母数字;报告给 CI 并声明 n=30 的统计局限 | 不接受则需用户批准"known-gap 不计入分母"或提高 N(均属口径变更,须显式记录) |
| R-2 | **规则污染复发**：看完 held-out 失败再回调规则 | 中/高 | §8.5 冻结纪律 + 报告记录运行次数/hash；**主数字必须来自冷跑** | 一旦回调，该 held-out 作废，需新版本重跑并如实标注"第 N 次" |
| R-3 | **排序与收窄顺序颠倒**导致 in-sample 反而下降 | 中/高 | §6.2 顺序写成 AC（`B15c-2`）+ 反例断言（scout §3.2 的数字） | 按反例回退到"先收窄后排序" |
| R-4 | 人名归一**扩大判定集合**，与 Q17 golden 冲突 | 中/高 | §5.4 只允许"姓同 + 首字母一致"；`B15c-6` 的 5 vs 29 断言 | 关闭人名归一，仅保留 `~`→空格 |
| R-5 | 改 fixture（`OPEN-C2 = A`）导致 `check_schema.py` 立刻红、且与 B1.5b 数字不可比 | 中/中 | 同一次改动内 bump `contract_version` + `schema_hash` + 同步断言；报告并列两个 hash | 回退到 `OPEN-C2 = B`（只改文本，不碰 fixture） |
| R-6 | 标注偏差（单人标注） | 中/中 | reviewer 抽查 ≥10 条；报告披露该限制 | 若需更强结论，双人独立标注（成本更高） |
| R-7 | 合成数据泄漏（`tt9xxx`/`Movie1/2`、2020s）进入 held-out | 中/高 | `check_heldout.py` 断言 + §8.3 规则 | 替换条目并 bump `heldout_version` |
| R-8 | `SIMILAR_POOL` 截断在收窄前生效，误伤大池用例 | 中/中 | §6.2 第 5 条：截断必须在收窄之后 | 调整顺序，重跑 |
| R-9 | 范围蔓延（生产 resolver / 生产相似 / 外部中文数据 / MCP / G3） | 中/高 | §2.3 硬边界；任何越界方向必须停下问主 agent | 拆出另立 issue |
| R-10 | **G1-T 的错译**：LLM 给出"看起来对但错片"的候选（通过 prefix 校验但仍是错片） | 中/高 | 步 3 用 exact 优先；多候选不同 `movie_id` → 澄清；`wrong_movie_rate` 逐条归因；`temperature=0` + 严格 JSON | 若错译率不可接受，回退到"步 1 + 步 5"（仅别名/缓存 + 澄清），并把 LLM 路径标为实验性 |
| R-11 | **G1-T 的模型不确定性/延迟**：`temperature=0` 仍非严格确定；冷启动延迟未测（`OPEN-b2`） | 中/中 | 缓存 + 回放 + 记录 `model_digest`/`prompt_sha256`；冷跑与热跑分开报告；冷启动单列 | 只报热跑会失真 → 必须同时报冷跑；若模型不可用，G1-T 记"未测"，**不得**用 G1 替代 |
| R-12 | **别名表被当成“补答案”**（把 held-out 的中文标题或 Q21–Q23 已知译名写进别名表） | 中/高 | §4.4/§8.3-9 断言 + 冻结 hash + 冷跑主数字；**C10 已裁定别名表冻结为空** | 一旦写入,held-out 该条作废,需 bump 版本重跑 |
| R-13 | `check_schema.py` 的 `MIN_CJK_ALIAS_CASES=3` 等冻结集常量被误用于 held-out | 低/中 | held-out 用自己的校验器；`check_heldout.py` 独立 ID 正则 | 修正校验器，不动冻结集 |

---

## 14. 开放决策（`OPEN-C*`）

| ID | 问题 | 归属 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| `OPEN-C1` | 多值 `genres[]` 语义（统一 AND）+ `Sci-Fi`/`Science Fiction` **canonicalize 去重**（不字面展开）+ 是否改 fixture 文本 | oracle + 主 agent | 🟡 待复核 | §5.1；建议 AND + canonical group + **不改 fixture 文本** |
| `OPEN-C2` | `year` 边界：只去 `maximum`（保留 integer + `minimum`）→ v3 + hash bump | oracle + 主 agent | 🟡 待复核 | §5.2；建议 A（按 P2-3 修正后的形态） |
| `OPEN-C3` | 澄清类判分口径（`allows_clarification`，严格口径仍为主） | oracle + 主 agent | 🟡 待复核 | §5.3 |
| **`OPEN-C4`** | 中文译名策略 | **用户** | ✅ **已裁定 = D：查询时解析（hybrid）** | §4；实现细节已由 oracle 增量复核确认（§15） |
| `OPEN-C5` | 中文译名条目是否计入 held-out 分母 | 用户/主 agent | ✅ **已裁定：计入分母 + 单列"不含译名"数字** | §8.2 / `B15c-11` |
| `OPEN-C6` | 人名别名用于判定还是只用于召回 | oracle | 🟡 待复核 | §5.4；建议"姓同+首字母一致"才算归一，**禁止纯姓氏扩展** |
| `OPEN-C7` | 别名词表形态：独立 JSON + 版本号 | 主 agent | 🟡 待确认 | §5.5 |
| **`OPEN-C8`** | 是否为 B1.5c 新开 GitHub issue | **用户** | ✅ **已裁定：新开 issue，`Refs #72`，#72 保持 OPEN**（→ **已执行：[#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74)，2026-11-19 创建，OPEN**） | 见 `docs/issues/issue-74-b1-5c-tool-routing-fixes.md` 的 `Internal tracking` |
| `OPEN-C9` | held-out 的 N：30 条是否足够 | 用户/主 agent | ✅ **已裁定：30 条 + Wilson CI 声明** | §10 `B15c-11` |
| **`OPEN-C10`** | `title-aliases.json` 是否允许 `seed-in-sample`（把 Q21–Q23 的 3 条已知译名预置）条目 | 主 agent（按 oracle C10/B-2 建议） | ✅ **已关闭：空表**——冻结时 `entries: []`，**不预置**任何已知译名；条目只能来自运行期“目录校验通过”的解析结果（缓存/记录），**不得**作为评测种子 | §4.4 / §5.5；“允许 seed-in-sample”的候选**已撤销**，不再作为可选项 |
| **`OPEN-C11`** | 解析成功率/错译率是否要**设门槛** | 主 agent/用户 | 🟡 待定 | 建议**不设**（仅披露），避免"用口径提分"；若设门槛需明确数值与分母 |

> **§14 状态说明（r4，2026-11-19 回填 issue #74）**：`OPEN-C8` 已执行完毕——B1.5c 的 GitHub issue = **[#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74)**（OPEN，milestone #3，无 labels），规范记录 = `docs/issues/issue-74-b1-5c-tool-routing-fixes.md`。其余状态的**数字/裁定一个未变**：`OPEN-C1`/`OPEN-C2`/`OPEN-C3`/`OPEN-C6`/`OPEN-C7`/`OPEN-C11` 仍为实施前的待复核/待确认项（在已发布的 #74 正文中以**单一推荐方案**陈述），`OPEN-C4`/`C5`/`C8`/`C9`/`C10` 为已裁定。**建 issue 不等于实施批准**：worker 未启动。
>
> **§14 状态说明（r5，2026-11-19 回填实施结果）**：实施**已经发生**（分支 `codex/b1-5c-tool-routing-fixes`，提交 `78f5740` + `734eef5`），评测**已全量跑完**且报告已生成；但**主门槛结果只能记为 provisional PASS**（held-out 在 stage 1c 可见并用于修规则 → 非盲测），详见 §17。因此：
> - **`OPEN-C1`/`OPEN-C2`/`OPEN-C6`/`OPEN-C7` 已落地并实测**（多值 canonicalize+AND、`year` 去 `maximum` + `b1.5-tools/v3`、人名只“姓同+首字母一致”、别名独立文件 + `unsupported` 断言），验收证据见报告 §6/§8；其“待复核”标记不再阻塞实施，但**如需推翻仍需主 agent 裁定**。
> - **`OPEN-C3`（澄清式成功）仍未落码** → Q57 类用例仍计任务失败（报告 §7.2）。
> - **`OPEN-C11`（解析门槛）仍未定**：本轮仅披露（`parse_success 2/2`、`wrong_movie 0/2`，**n=2**），未设门槛。
> - **新增待办 = blind holdout（B1.5d）**：未参与任何规则修复、独立标注、冻结后不得回看修规则；本轮**未做**。
> - **#74 保持 OPEN**：不声称已满足全部 AC、不声称已泛化 ≥90%、不声称已交付。

---

## 15. oracle 审查状态（r2：P1/P2 落点）

| oracle 项 | 落点（本文件） | 处理 |
| --- | --- | --- |
| **P1-1** held-out 运行参数不存在 | §2.2-5、§9（`--queries-path`/`--routing-path`/`--variant G1-T`）、§11-0、`B15c-9` | ✅ 已改为"**实施时新增**这两个参数（或 `eval_heldout.py`），**默认仍指向冻结 60 条**"，并要求报告粘贴实际 `--help` + 命令输出 |
| **P1-2** golden constraints 泄漏进执行 | §6.1（执行流/判分流分离）、§6.2-1、`B15c-3` | ✅ 执行只用 **router 从 query 文本抽出的 constraints**；golden constraints **只**进 `score_case`；新增哨兵测试断言 |
| **P1-3** issue 正文会误关 #72 | issue 草稿 Notes 段、`Internal tracking` | ✅ 改为 "**leaves #72 open**；whether #72 is closed after this work is a separate decision."；正文**不出现** `Closes #72`；**创建后回读已完成（2026-11-19）**：issue 侧 closing 引用检查为空（`closedByPullRequestsReferences` `totalCount: 0`）、#72 无 `closed`/`connected` 事件（仅 `cross-referenced`） |
| **P1-4** similar 12 vs 11 | §1.2、§10 `B15c-1`、附录 A、issue 草稿 Background/AC | ✅ 全部改为 **11/15** 并**点名** 11 条；12 只在"若要点名第 12 条并附 SQL"的条件句里出现 |
| **P1-5** C6 人名归一与 Q17 冲突 | §5.4、`B15c-6`、R-4 | ✅ 只允许"姓同 + 首字母一致"；**禁止纯姓氏扩展**；新增 Q17 **5 vs 29** 断言；纯姓氏仅召回/展示 |
| **P2-1** Sci-Fi/SF 不能字面展开 | §5.1 第 1–3 条、`B15c-1`、`B15c-5` | ✅ 先 canonicalize 成同一 key，再对多值做 AND 去重 |
| **P2-2** 中文译名 2–3 条 → 固定 2 | §8.2、R-1、`B15c-11` | ✅ 固定 **2** 条；给含（/30）/不含（/28）两个数；报告 Wilson CI |
| **P2-3** year 保留 integer + minimum | §5.2、`B15c-12`、§9 | ✅ 保留 `integer` + `minimum`，只去 `maximum: 2024`；服务端 196–2024 判 `OUT_OF_RANGE`；bump `contract_version`/`schema_hash` |
| **P2-4** held-out 缺 1990s | §8.3-4、`B15c-10` | ✅ 加入 **1990s 3–5 条** |
| **P2-5** `unsupported` 需可执行断言 | §5.5、`B15c-5`、§9（`check_schema.py`） | ✅ 三条断言：不得作 alias 目标、不得进 canonical group、alias 目标 ⊆ 56 enum |
| **P2-6** C4/C8 需用户答复 | §14 | ✅ 用户已裁定：C4=D、C8=新开 issue（`Refs #72`，#72 OPEN） |
| **P2-7** issue "no hand-labelling" 自相矛盾 | issue 草稿 Non-goals | ✅ 改为"不手工编造中文别名数据；**held-out 查询标注是允许且必需的**" |
| 工期 4–5.5 → 5–7 人日 | §12.2 | ✅ 已改为 **5.0–7.0 人日** |
| **B-1（P1）主门槛变体未统一** | §4.1 / §4.5 / §10 `B15c-11` / §11-6、§11-7 注释 / §1.1 `G1-T` 行 | ✅ **主门槛 = held-out `G1-T` 任务级 ≥90%（27/30 全分母）**；held-out `G1` = baseline（不含中文解析）；in-sample `G1` = 前后对照；报告主表同列 `G1` / `G1-T`，两者均给 /30 与 /28 + Wilson CI |
| **C10（oracle B-2）`title-aliases.json` 来源** | §4.1 / §4.4 / §4.7 / §5.5 / §9 / §14 `OPEN-C10` | ✅ **已裁定 = 空表**：冻结时 `entries: []`，不预置 Q21–Q23；条目只由运行期目录校验通过的解析结果产生；“允许 seed-in-sample”候选已删除 |
| **oracle 状态** | — | ✅ **r2 的 5 P1 + 7 P2 已由 `.pi/artifacts/oracle-b1-5c-delta.md` 复核关闭；该增量复核的 B-1（主门槛）与 C10（别名表）已在 r3 落实** → 本计划无待 oracle 复核项 |
| **主 agent 状态** | — | 🟢 **已批准创建 issue，#74 已于 2026-11-19 创建（OPEN）**；🟡 **未批准实施**：worker 尚未启动，本文件**不构成**实施许可，且本任务**未实施**（无脚本/fixture/报告变更） |

---

## 16. 修订记录

| 版本 | 日期 | 变更 |
| --- | --- | --- |
| **r1** | 2026-11-19 | 初稿（🟡 待 oracle 审查 / 未批准）。基于 B1.5b 报告（G1 82.7% FAIL）+ scout SQL 实测；提出评测层修复、四项契约裁定、中文译名三选项（建议 B）、held-out 30 条、12 条 AC、10 条风险、9 项开放决策。中文译名建议"接受缺口"。 |
| **r2** | 2026-11-19 | **按 oracle 5 P1 + 7 P2 与用户决策修订**：① 新增 **§4 C4=D 查询时解析**（五步流水线、别名/缓存来源边界、路由 vs 解析分离指标、成本/可复现/超时降级、与 §3 U1/`WU-9` 的关系）+ 新变体 `G1-T`；② 修 P1-1（新增 `--queries-path`/`--routing-path`，命令可执行）、P1-2（执行/判分数据流分离 + 哨兵断言）、P1-3（issue 用 "leaves #72 open"）、P1-4（**12/15 → 11/15** 并点名）、P1-5（人名归一禁止纯姓氏扩展 + Q17 5 vs 29 断言）；③ 修 P2-1（canonicalize 去重而非字面 AND）、P2-2（中文译名固定 2 条 + 含/不含 + Wilson CI）、P2-3（year 只去 `maximum`）、P2-4（held-out 加 1990s 3–5 条）、P2-5（`unsupported` 三断言）、P2-7（issue Non-goals 措辞）；④ 工期 **4–5.5 → 5.0–7.0 人日**；⑤ 新增 `OPEN-C10`（别名表来源）/`OPEN-C11`（解析门槛）与 3 条新风险（R-10…R-12）；⑥ 状态仍为 🟡 待 oracle 复核 / 未批准 / issue 未创建。**不改任何源码/fixture/报告/历史日志** |
| **r3** | 2026-11-19 | **按 oracle 增量复核（`.pi/artifacts/oracle-b1-5c-delta.md`）的 B-1 与 C10 修订**：① **B-1**——`B15c-11`/§1.1/§2.2/§4.5/§11 统一为“**主门槛 = held-out `G1-T` 任务级 ≥90%（27/30 全分母）**；held-out `G1` = baseline（不含中文解析）；in-sample `G1` = 前后对照”，报告主表**同列 G1 / G1-T**，两者均给 **/30 与 /28 + Wilson CI**；② **C10**——§4.1/§4.4/§4.7/§5.5/§9/§14 改为“**`title-aliases.json` 冻结时为空**”，**删除“允许 `seed-in-sample`”候选**，不预置 Q21–Q23 的 3 条已知译名；③ 状态改为“oracle 条件已落实；待主 agent 批准实施 / 待建 issue / **未实施**”。**C4=D、C5、C8、C9 与 C1–C3、C6/C7 均保持不变**；**不改任何源码/fixture/报告/历史日志** |
| **r5** | 2026-11-19 | **回填实施结果（provisional）**：① 文件头与元数据表状态改为 **🟡 评测已完成 = provisional / held-out 非盲测 / blind holdout（B1.5d）待做 / #74 保持 OPEN**；标题与 §2.1 删除“独立 held-out”口径（r1–r4 的“独立”已据实修正）；② 新增 **§17 实施结果回填**（实测数字、held-out 污染证据、计划-实现差异、未测项、下一步）；③ §14 新增 r5 状态说明（`OPEN-C1/C2/C6/C7` 已落地、`OPEN-C3`/`OPEN-C11` 未落、新增 B1.5d）；④ 文末“不声称”段改为只否认**泛化/交付/B-①/#72 关闭**。**不改任何源码/fixture/报告数字**（报告正文由主 agent 维护），**不声称**已泛化或已交付 |
| **r4** | 2026-11-19 | **回填 GitHub issue #74（不改方案与验收口径）**：① 文件头与元数据表状态改为 **🟢 已批准（r3）/ 已建 [#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74)/ 待实施**，`last_updated` 与“关联 GitHub issue”行回填 #74（OPEN、milestone #3、无 labels）；② §14 新增**状态说明**（`OPEN-C8` 已执行，#74 已建；其余 OPEN 项状态**未变**，建 issue ≠ 实施批准）；③ §15 “主 agent 状态”行同步；④ 文末“不声称”段改为只否认**实施/评测未发生**，#72 仍不声称已关闭。**规范记录已改名** = `docs/issues/issue-74-b1-5c-tool-routing-fixes.md`。**不改任何源码/fixture/报告/历史日志**，**不声称已开始或已完成实施** |

---

## 17. 实施结果回填（r5，2026-11-19，**provisional 评测**）

> 本节记录**实际执行结果**与**计划-实现差异**。数字来源 = `docs/ai/reports/b1-5c-tool-routing-fixes.md`（实测完成）；本节**不改** §10 的验收口径、门槛（≥27/30）与分母（30）。

### 17.1 一句话状态

**评测已完成 = provisional**：主门槛 held-out `G1-T` **30/30 = 100%**（≥27/30 → **provisional PASS**），但 **held-out 在 stage 1c 已可见并被用于修路由词/消歧 → 30/30 是 post-fix regression on a seen fixture，不是独立盲测、不是泛化估计**。**blind holdout（B1.5d）未做**；**[issue #74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74) 保持 OPEN**。

### 17.2 实测数字（唯一口径 = 报告 §1.1）

| 变体 / 样本 | 结果 | Wilson 95% | 说明 |
| --- | --- | --- | --- |
| **held-out `G1-T`（冷跑）** | **30/30 = 100%** | [88.6%, 100.0%] | 主门槛 → **provisional PASS**；仅 H01/H16 调模型（`model_calls = 2`） |
| held-out `G1-T`（热跑 / 回放） | 30/30 = 100% | 同上 | `model_calls = 0`；缓存不改变结论 |
| **held-out `G1`（baseline）** | **28/30 = 93.3%** | [78.7%, 98.2%] | 不含中文解析，2 条中文用例必败；**不是门槛对象**，且受同一污染影响 |
| held-out `G0` | **14/30 = 46.7%** | [30.2%, 63.9%] | 纯 LLM 路由不可交付（D-9） |
| held-out `G2`（回放 G0） | **14/30 = 46.7%** | [30.2%, 63.9%] | 无改善（净 0 例） |
| in-sample `G1` | **67/75 = 89.3%**（修复前 62/75 = 82.7%） | [80.3%, 94.5%] | **非门槛**；路由层 73/75 = 97.3% |
| `G3` / MCP | **未测 / 未实现** | — | 与 §2.3 一致 |

- 解析：`triggered 2 / parse_success 2/2 / wrong_movie 0/2`（**n=2**，非估计），冷跑 p50 156.1 ms / p95 488.3 ms（**模型已预热，冷启动延迟仍未测**）。
- 契约：`b1.5-tools/v3`、`schema_hash 712c0e34…474a`；held-out/别名文件 sha256 本次复核未变（queries `d471c57a…abe4`、routing `5608dd4c…4aba`、aliases ×3 同报告 §3）。

### 17.3 held-out 污染（必须与任何数字同时出现）

- held-out 30 条（`H01`–`H30`）在 **stage 1c** 中可见，并**被用于改查询文本/补通用 pattern**：**`H18` 的查询文本被从“悬疑片”改为“惊悚片”**（提交 `734eef5` 的 `docs/ai/golden/queries-heldout.b1-5c.json` diff 可核：routing 中绑定的旧 sha256 `9933b0cc…535f` → 新 `d471c57a…abe4`，即 **fixture 在全量运行前被编辑过**）；`H06/H12/H18/H24/H28/H16` 的 pattern / 年份消歧是在**看过这些 held-out 用例之后**补的。
- ⇒ §8.5 的“一次性冻结纪律 / 跑完不得按失败回调规则”**实际未被完全遵守**：记录在案的冻结哈希是**改词之后**的版本，但**规则是在该 fixture 可见时定的**。故只能称 **post-fix regression**。
- ⇒ 要声称“held-out ≥90% / 泛化 ≥90%”，必须另建 **blind holdout（B1.5d）**：未参与任何规则修复、独立标注、冻结后不得回看修规则；本轮**未做**。

### 17.4 计划与实现的差异（逐项）

| 计划项 | 实际 | 性质 |
| --- | --- | --- |
| 主门槛 = held-out `G1-T` ≥27/30 | 30/30，但 fixture 已被用于修规则 | ⚠️ **口径降级为 provisional**（纪律缺口，非阈值/分母调整） |
| §8.5 跑前冻结、不得按失败回调 | H18 文本在全量跑前被改；pattern/年份消歧在看过 held-out 后补 | ❌ **纪律未遵守**（已如实记录，未回滚） |
| `B15c-1` 多值 canonicalize + AND | 已实现并实测（Q11、Q19 修复） | ✅ |
| `B15c-3` 执行流不读标注 | 7 份 run record `annotation_guard = true` | ✅ |
| `B15c-5` 拆分 `Musical`/`Music` | 已按契约拆分 → **in-sample Q48 回归**（冻结标注把 “musicals” 归入 `Music`，与拆分决策矛盾） | ⚠️ **新增已知回归，待主 agent 裁定（报告 §5.4）** |
| `OPEN-C3` 澄清式成功 | **未落码** → Q57 类仍计失败 | ⚠️ 未做（未越界改判分） |
| G3 / MCP | 未测 / 未实现 | ✅ 与 non-goal 一致 |
| in-sample `G1-T` | **未跑**（仅 `G1` 前后对照） | ⚠️ 未做（非门槛所需） |

### 17.5 未测 / 待办

1. **blind holdout（B1.5d）**：唯一能把“泛化 ≥90%”从 provisional 变成可声称结论的路径；本轮**未做**。
2. `G3`（未测）、MCP（未实现）、embedding 步 4（关闭）、**冷启动延迟**（`OPEN-b2` 仍未测）。
3. `OPEN-C3`（`allows_clarification`）与 `OPEN-C11`（解析门槛）仍未裁定。
4. **Q48 `Musical`/`Music` 矛盾**、**无文本约束的相似查询**（Q34/Q53/Q54）两类风险 held-out **覆盖为 0**，需新版本 held-out 或单独裁定。
5. **PR 未创建**：分支 `codex/b1-5c-tool-routing-fixes` 尚未开 PR，`#74` 保持 OPEN。

---

## 附录 A:计划期已确认的证据数字(实施前可复查)

| 事实 | 数值 | 来源 |
| --- | --- | --- |
| B1.5b G1 任务级 | 62/75 = 82.7%（门槛 ≥90% = ≥68/75） | B1.5b 报告 §5.1 |
| **主门槛（r3 裁定，oracle B-1）** | held-out **`G1-T`** 任务级 ≥90%（27/30 全分母）= **交付候选**；held-out **`G1`** = **baseline**（不含中文解析，2 条中文必败 → 上限 28/30）；in-sample `G1` = 修复前后对照；三者均须给 /30 与 /28 两分母 + Wilson CI | 主 agent（2026-11-19） |
| 路由层正确率 | 74/75 = 98.7%（与任务级分开） | 架构文档 §1.6 |
| similar/recommend 失败 | 8 条（Q12/Q16/Q19/Q35/Q36/Q37/Q55/Q56） | 报告 §7 |
| AND 语义可复现 | similar **11/15** 精确（Q13/Q14/Q15/Q33/Q34/Q36/Q37/Q53/Q54/Q55/Q57，scout §3.1 逐条 ✅）；recommend 10/10 精确 | scout §3.1/§3.3 |
| 只加"同导演排序"的反例 | 8 条 similar 的 top-5 命中降到 1/3、1/3、0/3、0/3、0/4、0/4、0/3、0/4 | scout §3.2 |
| 目录中文 | **0**（`movies.title/director`、`stars.name` 无真 CJK） | scout §2.1 |
| 类型别名缺口 | 缺 Fantasy(492)/War(125)/Sport(103)/History(215)；`musical→Music` 错并 | scout §1.1/§6.2 |
| 年份偏斜 | 2000s 9,069 / 2010s 94 / 2020s 7（其中 5 行为 `tt9xxx` 合成）；1990s 2,370 | scout §2.1/§2.4 |
| 导演字符串 | 10,888 distinct；1,398 个姓氏多字符串；788 缩写式；30 含 `~`；`Spielberg` 29 vs `Steven Spielberg` 5 | scout §2.2 |
| 重名标题 | 950 个 title 有 >1 行（2,120 行） | scout §2.3 |
| `Heat` 检索 | exact 2 / prefix 6 / substring 32 | scout §2.3 |
| fixture 约束 | `year` 字段 `minimum:196, maximum:2024`（`tool-schema` 57-61/171-182/342-353），由 `check_schema.py:198-204` 断言；`EXPECTED_GENRE_COUNT=56`；`QUERY_IDS=Q01..Q60`；`contract_version=b1.5-tools/v2` | scout §1.3 + 只读核对 |
| 运行入口 | `tool_eval.py:main()`(1476) **无** `--queries-path/--routing-path`；`--variant` choices = G0/G1/G2/G3（无 G1-T） | 只读核对 |
| Wilson 95% CI（供报告用） | 27/30 = [74.4%, 96.5%]；28/30 = [78.7%, 98.2%]；26/30 = [70.3%, 94.7%]；26/28 = [77.4%, 98.0%]；28/28 = [87.9%, 100.0%]（**`G1` 与 `G1-T` 两列都要给**） | 本计划计算（Wilson，z=1.96） |
| held-out 冻结哈希 | **待冻结**（跑前写入本表 + 报告）：queries / routing / `title-aliases.json` / 别名文件 | — |

---

## 附录 B：需要同步改写的规范段落（实施时）

| 文件:段 | 现状 | C4=D / 本轮裁定后的写法 |
| --- | --- | --- |
| `ai-user-workflows.md` §3 U1「中文译名策略」 | "无别名表 → 不保证命中；**不得**自行把译名翻成英文后当成用户给的原名" | "**查询时解析**：候选（别名/缓存 → LLM）→ **目录 exact/prefix 校验** → 命中才检索；未通过校验的候选**不得**当作用户原名；全部未命中 → 澄清；超时/非法输出 → 澄清" |
| 同文件 §3.1 年份三态 | schema 层先拒 2030 | "schema 只管**形状**（`integer` + `minimum`）；覆盖 196–2024 由服务端判 `OUT_OF_RANGE`；三态互斥且可达" |
| 同文件 §4.2 `GenreEnum`/多值语义 | 规范化"待实现时定稿" | "多值 = **canonicalize 后 AND**；`Sci-Fi`/`Science Fiction` 属同一 canonical group；`unsupported` 名单不得作别名目标" |
| 同文件 §1.6 | B1.5b 回填 | 追加 B1.5c 实测（in-sample 前后 / held-out G1 / G1-T 解析指标 / 已知缺口） |
| 同文件 §8 变更清单 | — | 追加一行（2026-xx-xx，B1.5c） |
| 同文件 §9 `WU-9` | 🟡 未批准（"引入别名表需另立 issue"） | 更新：**外部中文别名数据仍未批准**；C4=D 的查询时解析**不引入外部数据**，已由用户批准；`title-aliases.json` **冻结时为空**（C10 已裁定） |

---

> 本文件**不声称**：held-out `G1-T` 的 30/30 是**独立盲测或泛化估计**（它是 post-fix regression on a seen fixture）、**泛化 ≥90% 已达成**、B-① 已交付或已开始、`G3`/MCP 已做、`OPEN-C3` 已落码、中文解析已达某成功率（n=2）、#72 已关闭。
> 已确认的事实仅限：oracle 增量复核（`.pi/artifacts/oracle-b1-5c-delta.md`）判定 r2 的 5 P1 + 7 P2 **全部关闭**并给出 B-1（主门槛变体）与 C10（别名表来源）两条阻塞；主 agent 已据此裁定 **G1-T 为主门槛 / 别名表为空表**（r3）；**实施已发生**（分支 `codex/b1-5c-tool-routing-fixes`：`78f5740` 加 held-out fixture + 契约 v3、`734eef5` 评测层修复 + `G1-T`），全量跑完并生成报告；**但 held-out 在 stage 1c 可见且被用于修规则 → 主门槛只能记 provisional PASS**（§17.3）；**blind holdout（B1.5d）未做**；**GitHub issue [#74](https://github.com/PNZ-z/ai-powered-Fabflix/issues/74) 保持 OPEN**（状态以远端为准）。

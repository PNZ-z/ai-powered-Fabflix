# B1.5d — Blind holdout 验证 B1.5c 的 `G1-T`（**🔴 已执行 / FAIL / 报告完成**）

> **🔴 已执行（2026-11-20）：`G1-T`（冷跑）在 blind 30 条上 25/30 = 83.3%（Wilson [66.4%, 92.7%]）< 27/30 → FAIL；B1.5c 的 30/30 未泛化。报告 = `docs/ai/reports/b1-5d-blind-holdout.md`（已完成）。下一步 = B1.5e（修复 + 新盲集），**不得**用本批失败条目改规则后重跑同一集；#74 保持 OPEN，#76 报告完成（PR 待建）。**
>
> 规则与代码已冻结在 `main` @ `0761845`；B1.5d **只生成一套新的 30 条 blind holdout、只新增一个只读 checker、跑一次**，用来回答"B1.5c 的 `G1-T` 在**未参与规则开发**的样本上是否仍 ≥90%"。**未修改** `scripts/eval/tool_eval.py`、既有 `check_*.py`、`docs/ai/golden-set.schema.json`、别名、契约与任何阈值；**未新增功能**；**未改变 issue #74 的 OPEN 状态**。
>
> | 元数据 | 值 |
> | --- | --- |
> | 文档类型 | 计划 / 方案（含盲测纪律、配额、只读 checker 规格、验收）+ **实施结果回填（§10）** |
> | `last_updated` | 2026-11-20（**r4：实施结果回填**——30 条已生成并冻结、四个运行产物已产生、主门槛 **FAIL**、报告已完成；r3 为批准版） |
> | 状态 | 🔴 **已执行 / 主门槛 FAIL（25/30 < 27/30）/ 报告完成**；冻结与运行记录见 §10 与附录 A；`B15d-11` 有一项以 mtime 近似满足（见 §10.6） |
> | 前序 / 上级 | B1.5c：`docs/plans/b1-5c-tool-routing-fixes.md`（评测完成 = **provisional / 非盲测**）；报告 `docs/ai/reports/b1-5c-tool-routing-fixes.md`（held-out `G1-T` 30/30、`G1` 28/30，但该 held-out 在 stage 1c 已被用于修规则：H18 改词、H06/H12/H18/H24/H28 路由词、H16 年份消歧）——其 §7.1 第 4 条与 §9 `R-8` 明确要求新建 blind holdout |
> | 关联 issue | **#74（OPEN，保持 OPEN）** — `docs/issues/issue-74-b1-5c-tool-routing-fixes.md`；本任务的新 issue = **[#76](https://github.com/PNZ-z/ai-powered-Fabflix/issues/76)**（规范记录 `docs/issues/issue-76-b1-5d-blind-holdout.md`），正文只写 `Refs #74`、**不含 closing keyword**；#76 状态 = 报告完成 / **PR 待建** / 可在 PR 合并后关闭 |
> | 冻结基线 | `main` @ `0761845`（PR #75 之后）：`tool_eval.py` / 既有 `check_*.py` / `docs/ai/golden-set.schema.json` / `docs/ai/aliases/*` / `tool-schema.b1-5.json`（`contract_version b1.5-tools/v3`）的逐文件 SHA-256 见 **附录 B（已实测）** |

---

## 0. r2 修订摘要（oracle B1.5d 评审 → 本文件落点）

| # | 评审项 | 级别 | 落点 | 收敛结果 |
| --- | --- | --- | --- | --- |
| 1 | **缓存/预热未定义**：`G1-T` 冷跑会用默认缓存路径 `.pi/artifacts/b1-5c-resolver-cache.json`，可能污染/复用 B1.5c 证据；预热无记录 | P1 | §5.2 / §5.4 / `B15d-11` / `B15d-12` | 冷跑使用**独立** `--resolver-cache .pi/artifacts/b1-5d-eval/resolver-cache.json`，跑前 `rm -f`，`cache_state_at_load=absent`；热跑同路径 `loaded`；**不动** B1.5c 缓存；预热 1 次模型请求（不计 case）；`G1` 全 30 条 `model_calls=0`、`G1-T` 冷=2 / 热=0；冷热逐例 `task_success` + `chosen_movie_id` 必须一致 |
| 2 | **`R-1` / `R-2` 只写在风险里，没进 Scope / 验收** | P1 | §3.3.1 / `B15d-13` / §7 | 30 条中预声明 **2 条已知缺口探针**（1 条无文本约束 similar = `R-2`；1 条 `musicals`/`Music` = `R-1`）；主门槛仍 **30/30（27/30）**；另报**去探针 28 条**敏感性 + 非 CJK 28 条敏感性；**不得**用 28 条宣称 PASS；失败必须归因到 `R-1`/`R-2` |
| 3 | **"改 schema"与 checker 角色不清**；`B01`–`B30` 与 `$defs` 的 `^H…$` 模式冲突 | P1 | §4 / `B15d-14` | **checker-only（`D-1`）**：**不改** `golden-set.schema.json`；只新增只读 `scripts/eval/check_blind_holdout.py`，自己实现结构/ID/字段/布尔/配额/排除/年代/拒绝/澄清/`multi_slot`/CJK 断言；ID `B01`–`B30`（`D-6`） |
| 4 | **断言不足**（1990s × `min_rating`、拒绝/澄清布尔、`multi_slot`/`cjk_title` 归属） | P1 | §3.4 / §4.2 | 1990s 用例**不得**依赖 `min_rating`（空集只能作显式声明的边界用例）；拒绝 = `expects_no_match:true` + 空可接受集 + 无 tool；澄清 = `allows_clarification:true` + 非空可接受集；`multi_slot` 只属 `filter_catalog`/`similar_movies`；`cjk_title` 只属有 title/seed 槽的 intent |
| 5 | **冻结范围太窄**（漏 checker、漏 schema、缺"首次运行晚于冻结"证据） | P1 | §4.3 / `B15d-14` | 冻结 **3 个 SHA**（queries / routing / checker）+ 记录 **schema hash 不变**；首次运行前把 fixture + checker 落进提交（或 `git hash-object -w` + `mtime`），报告给出"首次运行时间 > 冻结时间"证据；`tool_eval.py` 对 `0761845` diff 为空；记录 `db_snapshot_hash` |
| P2-a | "不改 `check_*`" 表述会被新增 checker 打脸 | P2 | §1 / §4 | 改为"**不改既有** `tool_eval.py` / `check_schema.py` / `check_golden.py` / `check_heldout.py` / 别名 / 契约 / 阈值；**只新增只读 checker**"；`docs/ai/README.md` 允许在报告生成后**追加索引行**（r10），不改既有行 |
| P2-b | `/28` 语义混淆 | P2 | §7 | `/28` 只作**非 CJK** 敏感性；去探针口径显式写成员；可选 `/29`；**不得**与 G1 的 `/28` 比强弱，**不得**用任何 28/29 分母宣称主门槛 PASS |
| P2-c | Wilson 表述 | P2 | §7.2 | `27/30` 下界约 **74%**（精确值 74.4%，主 agent 记作 ~73%）；**禁止**"95% 置信 ≥90%"一类表述 |
| P2-d | "one shot" 含混 | P2 | §2 `BL-5` / §5.3 | 改为"**每个 (variant × cache state) 一次**：`G1` 1 次 + `G1-T` 冷 1 次 + 热 1 次（+ 可选回放 1 次）" |
| P2-e | 标注者独立性未披露到"看过什么" | P2 | §2 / `BL-7` | 生成者**不读** B1.5c 报告 §7 失败分析；只拿配额/覆盖/排除清单/契约/checker；报告必须披露"标注时看过哪些文件" |
| P2-f | issue 关闭语义 | P2 | §9.1 `D-4` / issue 草稿 | B1.5d issue 完成后**可关**；**#74 不自动关**；正文 `Refs #74`，无 closing keyword |

> 本表只记录**已由主 agent 裁定**的收敛结果；任何"待复核"的地方都在 §9.3 明列，不在此表伪装成定论。

---

## 1. 目标与非目标

**目标（一句话）**：在不改任何规则/代码/契约/阈值的前提下，新建 **30 条从未参与规则开发的 blind holdout**（`B01`–`B30`，10/10/10，其中 2 条为预声明的已知缺口探针），**冻结 SHA-256 后按 §5.3 的次数表运行**（每个 (variant × 缓存状态) 各一次：`G1` 1 + `G1-T` 冷 1 + 热 1，+ 可选回放 1），给出 `G1-T` 的任务级数字 + Wilson CI + 逐条归因，从而判断 B1.5c 的 ≥90% 是否只是**在见过的样本上的 post-fix regression**。

| 不做 | 理由 |
| --- | --- |
| 改 `scripts/eval/tool_eval.py`、**既有** `check_schema.py` / `check_golden.py` / `check_heldout.py`、`docs/ai/golden-set.schema.json`、别名、`tool-schema.b1-5.json`、任何阈值/分母 | 改它就是"看完答案再改规则"；`D-1` 明确 checker-only |
| 新增功能 / 生产代码 / `src/main` / `pom.xml` / CI / MCP / G3 | 与 B1.5c 边界相同（`OPEN-b4`） |
| 复用或改写 B1.5c 的 30 条、B1.5b 的 75 条；改 B1.5c 报告/计划/issue 正文；改 B1.5c 的解析缓存文件 | 已污染的样本不可清洗；历史记录只追加不修改；B1.5c 缓存是证据 |
| 跑 `G0` / `G2` | `D-3`：不跑，报告写"未测" |
| 关闭 #74 / 声称其已达标 / 在 B1.5d 内修任何失败 | `D-4`：关闭与修复都是结果之后的独立决定；需修复 → **另立 B1.5e + 另一套新盲集**（§8） |
| 用 28/29 分母宣称主门槛 PASS | 主门槛只在 `/30` 上评判（§7） |

**唯一新增产物**：`scripts/eval/check_blind_holdout.py`（只读断言脚本，不参与路由/判分，不被 `tool_eval.py` 调用）。

---

## 2. "blind" 的定义与纪律（本任务核心）

**可操作定义**：这 30 条查询的文本、标注与可接受集合在**第一次运行 `tool_eval.py` 之前**就已完整冻结并记录 SHA-256；生成/标注期间**没有任何人运行任何变体**去看结果，因此用例选择不是"按执行结果筛选"的产物。

| # | 纪律 | 可执行控制 |
| --- | --- | --- |
| `BL-1` | 标注期间**禁止**对新 fixture 运行 `tool_eval.py`（任何变体、任何 `--ids`/`--limit`/`--dry-run` 试跑） | 运行次数表（§5.3）从 0 起；报告给出"首次运行时间晚于冻结时间"的证据（§4.3） |
| `BL-2` | 只允许**只读 SQL**（`fabflix-mysql` / 3307 / `moviedb`）核对可接受集合、行数、年份、类型、重复片名 | 每条用例记 `rows=` + 验证谓词摘要（沿用 `check_heldout.py` 的 `labeling_note` 形式） |
| `BL-3` | 抽样规则**先定后抽**：配额与覆盖表先于第一条写完；**不得**因为"看起来会过/会挂"而替换用例 | 配额表（§3.2）与覆盖表（§3.3）先于 fixture 存在 |
| `BL-4` | 冻结 = **三个 SHA-256**（queries / routing / checker）+ `blind_version`；**冻结后才允许第一次运行** | 哈希写入附录 A、issue 跟踪与报告；运行前 `sha256sum` 输出存档 |
| `BL-5` | **每个 (variant × cache state) 组合只跑一次**：`G1` 1 次、`G1-T` 冷 1 次、`G1-T` 热 1 次（+ 可选回放 1 次）。看结果后**不重跑**、不换用例、不改规则 | 报告运行次数表；fixture/checker 若改动 → bump `blind_version` 并记 "run 2"，**run 2 不作盲测证据** |
| `BL-6` | 结果出来后**不回头改规则**；失败归因为"规则缺口 / 标注问题 / 已知缺口 `R-1`/`R-2`/`R-4`" | 需修复 → 另立 **B1.5e**；B1.5d 数字保持原样 |
| `BL-7` | **生成者不读 B1.5c 报告 §7（失败分析）**，只允许读：配额与覆盖清单、排除清单、契约文档（`tool-schema.b1-5.json` / `query-routing` 的 slot 词汇）、checker、只读目录 | 报告"标注者独立性"节必须列出**实际读过的文件清单**；违反则本批 30 条不作盲测证据 |

> **盲测的已知边界（必须写进报告，`D-5` 已裁定接受）**：生成者必然知道规则的**存在与大致形状**（`D-5` 明确接受并披露），所以 blind 只排除"**用这批样本调过参**"，不能排除"生成者知道规则大致覆盖什么"。这是本任务能达到的最强形式；n=30、单人标注偏差（`OPEN-U3`）仍是限制。**报告必须同时披露：生成者是否读过 B1.5c 报告 §7（按 `BL-7` 应为"未读"）**。

---

## 3. 样本、配额与排除规则

### 3.1 文件与 ID

| 项 | 值 |
| --- | --- |
| 查询文件（新） | `docs/ai/golden/queries-blind.b1-5d.json`：`blind_version: b1-5d-blind/v1`（`D-6`）、`created`、`snapshot`、`quota`、`queries[30]` |
| 路由文件（新） | `docs/ai/golden/query-routing-blind.b1-5d.json`：`annotates.sha256` 绑定上一文件，`schema_contract` 记 v3 hash（`712c0e34…474a`） |
| ID | **`B01`–`B30`**（不与 `Q01–Q60` / `T01–T15` / `H01–H30` 冲突；不用 `H1-xx`）见 `D-6` |
| 快照 | 必须等于 `db_snapshot_hash = 8b16c047…f925`（21,087 部）；若变了 → 重新冻结并声明"与前次不可比" |

### 3.2 配额（与 B1.5c 同口径便于比较，但**不复用其任何用例**）

| 维度 | 配额 |
| --- | --- |
| track | `zh_zh` 10 / `zh_en` 10 / `en_en` 10 |
| intent | `search_by_title` 5 / `filter_catalog` 9 / `similar_movies` 7 / `recommend_by_preference` 3 / `clarify_or_refuse` 6（拒绝 4 + 澄清 2） |
| 交叉标记 | `multi_slot` 4（含在上面 26 条内，不额外加条）/ `cjk_title` **恰好 2** |
| 年代 | **1990s 3–5 条**；2010s ≤1；**禁止 2020s**；其余以 1950s–1980s 为主 |

### 3.3 必须覆盖的风险点（B1.5c 未覆盖或覆盖不足）

| 覆盖项 | 要求 |
| --- | --- |
| 中文译名（不在别名表） | **2 条**，且与 `H01 肖申克的救赎`、`H16 超人`、in-sample `Q21 黑客帝国`/`Q22 阿甘正传`/`Q23 泰坦尼克号` **全不同**；别名表冻结为空 → 必然走 LLM 路径，测"翻译泛化"而非缓存 |
| 人名缩写 / 脏导演串 | ≥2 条（`X.Lastname` 缩写式、含 `~` 的串、同名不同写法）；只允许"姓同 + 首字母一致"归一，**禁止**纯姓氏扩展 |
| 同名种子 | ≥2 条：同 title 有 >1 行，**必须**靠查询中的年份/导演线索收窄（"无解重名"属澄清用例，另计） |
| 类型变体 | ≥2 条（`Sci-Fi`/`Science Fiction` canonical group；`Fantasy`/`War`/`Sport`/`History` 等薄覆盖值）；校验 canonicalize-then-AND |
| 多槽位 | 4 条（类型 + 年代 + 评分/导演组合） |
| 拒绝 / 澄清 | 拒绝 4 / 澄清 2；澄清必须是真心无解，不得靠标注放水 |
| 边界 | ≥2 条：零类型片 / 无评分片 |

#### 3.3.1 预声明的 2 条已知缺口探针（`D-2` 裁定：纳入）

这 2 条**计入 30 条分母与主门槛**，并在 fixture 里以 `known_gap` 字段 + `labeling_note` **预声明**，不是事后解释。

| 探针 | 形状 | 标注口径 | 预期 |
| --- | --- | --- | --- |
| **`R-2` 探针**（1 条，`similar_movies`） | 查询文本**不声明任何类型/年代约束**（只有"类似 X"），种子为**英文片名**（**不引入 CJK title**，保持冷跑模型调用预算 = 2；见 §5.4） | 可接受集合 = **种子自身的类型 × 种子年代**（沿用 Q34/Q53/Q54 的口径），SQL 谓词 + `rows=` 记录；`result_constraints` 可写入 routing 以便审阅，但**不计入判分**（`tool_eval.py` 的 `text_constraints()` 在文本无约束时得到 `result_constraints_scored = {}`），报告须贴出该 evidence | **大概率失败**（`R-2`：执行流不得读标注，无法从种子元数据派生收窄），失败**必须**归因 `R-2` |
| **`R-1` 探针**（1 条，`filter_catalog`，`en_en`） | 含 `musicals` 的查询（与 Q48 的 `1990s romantic musicals` **文本不同**、年代/类型组合不同） | 标注沿用 **B1.5a/Q48 的 `Music` 口径**；同时用 SQL 记录 **两种解释**的行数（`{…, Music}` 与 `{…, Musical}`）与 `Musical` / `Music` 全库行数（B1.5c 记录：`Musical` 139 / `Music` 619，标注时须复核） | `genre-aliases.json` 把 `musical → Musical`，与 `B15c-5` 拆分冲突未裁定（`R-1`），**大概率失败**；失败归因 `R-1`，**不得**据此改别名或标注 |

> **诚实性要求**：`R-1` 探针的 golden 口径本身是争议对象（§B1.5c §5.4 的 (a)/(b)/(c) 三选项未裁定）。因此报告必须**并列**两种解释下的命中情况，并明确"本探针的失败不证明 `G1-T` 泛化不足，只量化 `R-1` 未裁定的代价"。

### 3.4 排除规则与结构约束（硬断言，全部由 §4 的 checker 执行）

1. query 文本不得与既有 105 条（60 frozen + 15 tool + 30 B1.5c held-out）任何一条相同或仅差标点；
2. 种子片名 / 种子 `movie_id` 不得与既有 105 条重复；
3. 可接受 id 集合不得与既有任一集合完全相同；**逐 id 不重叠为目标口径**——确认不可行时**上交裁定**（`D-7`），**不擅自放宽**；
4. 不得使用 `database/dev-seed.sql` 合成行（`tt9000001…tt9000008`、`Movie1`/`Movie2`）；
5. 不得出现 2020s 用例；1990s **3–5 条**；2010s ≤1；
6. 所有 `acceptable_movie_ids` 必须 SQL 可复现（谓词 + `rows=` 记录），且全部存在于目录；
7. **1990s 用例不得依赖 `min_rating`**：快照 `ratings_covered_from_year = 2001`（`movies_without_rating = 13082`），任何 1990s 片都没有评分行 → 带 `min_rating` 的 1990s 用例必然空集。唯一例外 = **显式声明的空集边界用例**（`expects_no_match: true` + 空 `acceptable_movie_ids` + `labeling_note` 写明"预期空集"），checker 只接受这两种形态；
8. **拒绝**用例必须三件一致：queries 侧 `expects_no_match: true`、`acceptable_movie_ids: []`；routing 侧 `expected_refusal: true`、`expected_tools: []`、`required_slots: {}`；
9. **澄清**用例必须：queries 侧 `allows_clarification: true` 且 `acceptable_movie_ids` **非空**；routing 侧 `expected_refusal: false`、`expected_tools` 非空、`required_slots` 含 title 槽（`query` 或 `seed_title`）。注：非拒绝用例缺可接受集会直接触发 `tool_eval.py` 的 `EvalError`（`build_cases`），因此本条同时是"评测可跑"的前置条件；`tool_eval.py` 从 queries 侧读 `cjk_title`、从 queries 或 routing 侧读 `allows_clarification`；
10. **`multi_slot` 只允许出现在 `filter_catalog` / `similar_movies`** 两类 intent 上（其余 intent 出现即失败）；
11. **`cjk_title: true` 只允许出现在"有 title 槽"的 intent** 上：routing 的 `required_slots` 必须含 `query` 或 `seed_title`（拒绝类用例不得标 `cjk_title`）；且这 2 条片名不得出现在 `title-aliases.json`（该表仍为 `entries: []`）。

### 3.5 生成者可见输入白名单（`BL-7` 的执行形式）

| 允许读 | 禁止读 |
| --- | --- |
| `docs/ai/golden-set.schema.json`（形状参考）、`docs/ai/tool-schema.b1-5.json`（canonical genre enum）、既有 `query-routing*.json` 的 `slot_vocabulary`、`docs/architecture/ai-user-workflows.md` 的契约段、`scripts/eval/check_blind_holdout.py`、只读 MySQL | `docs/ai/reports/b1-5c-tool-routing-fixes.md` **§5/§7（失败分析与归因）**、任何运行产物（`.pi/artifacts/b1-5c-eval/*`、`.pi/artifacts/b1-5c-resolver-cache*.json`） |

---

## 4. 冻结产物、只读 checker 与冻结时序

### 4.1 `D-1` = checker-only 的边界

| 项 | 决定 |
| --- | --- |
| `docs/ai/golden-set.schema.json` | **不改**。理由：现有 `$defs/heldoutQueryCase.id` 是 `^H(0[1-9]|[12][0-9]|30)$`，新增 `$defs/blindQuerySet` 会让同一套断言同时服务 `H*` 与 `B*` 模式，产生分叉；改 schema 还会动到 `check_schema.py`/`check_golden.py` 的输入契约。故 **schema 保持字节不变**（hash 见附录 B 且报告须复述） |
| 新增 `scripts/eval/check_blind_holdout.py` | **唯一**新增脚本；只读；stdlib + 复用 `check_golden`/`check_schema` 的**只读辅助**（`REPO_ROOT`、`load_json`、`ReadOnlyMysql`、`TOOL_NAMES`、`genre_enum`、`YEAR_MIN/YEAR_MAX`、`TITLE_ALIASES_PATH`）；**不 import `tool_eval`**，不含任何路由/判分逻辑，不被 `tool_eval.py` 调用 |
| 既有 `check_*.py` / `tool_eval.py` / 别名 / 契约 / 阈值 | **零改动**（`B15d-14`） |
| `docs/ai/README.md` | 报告生成后可**追加**索引行（r10）+ 变更日志行；**不改既有行** |

### 4.2 `check_blind_holdout.py` 的断言清单（结构 / ID / 字段 / 布尔 / 配额 / 排除 / 年代 / 拒绝 / 澄清 / 多槽 / CJK）

1. **结构**：两个文件存在、JSON 可解析、无未知顶层键；queries 顶层含 `blind_version`（`b1-5d-blind/v1`）、`created`、`snapshot`、`quota`、`queries`；routing 顶层含 `annotates`、`schema_contract`、`slot_vocabulary`、`annotations`；单条 case 的必填字段齐全（对齐 `$defs/heldoutQueryCase` 的字段集，但**用本脚本自带校验**，不经 JSON Schema）。
2. **ID**：恰为 `B01`…`B30` 且**按序无缺无重**；routing 的 `query_id` 集合与 queries 的 `id` 集合相等且一一对应。
3. **track / lang**：10/10/10，`lang` 与 track 一致（`zh_*`→`zh`，`en_en`→`en`）；query 文本无重复。
4. **intent 配额**：`search_by_title` 5 / `filter_catalog` 9 / `similar_movies` 7 / `recommend_by_preference` 3 / `clarify_or_refuse` 6；queries 与 routing 的 `intent` 逐条一致。
5. **布尔与交叉标记**：`multi_slot` = 4 且只在 `filter_catalog`/`similar_movies`；`cjk_title` = 2 且只在有 title 槽的 intent（§3.4.11）；记录 `known_gap` 值（`R-1` 恰 1 条、`R-2` 恰 1 条）。
6. **拒绝形态**（§3.4.8）与**澄清形态**（§3.4.9）：4 / 2 条，逐条断言。
7. **排除规则**（§3.4.1–3、§3.4.4）：文本归一化后不与 105 条重复；种子片名/id 不重复；可接受集合不完全相同；**逐 id 不重叠**（目标口径；不可行时以 `--allow-id-overlap` 之外的**硬失败**呈现，交由主 agent 裁定，脚本不提供放水开关）；不含合成行。
8. **年代**（§3.4.5、§3.4.7）：1990s 3–5 条、2010s ≤1、无 2020s；1990s 用例的 `min_rating` 断言；空集边界用例必须是显式声明形态。
9. **别名表**：`title-aliases.json` 仍 `entries: []`（C10），2 条 CJK 片名不在表内（`alias_hits = 0` 前置条件）。
10. **绑定与指纹**：`annotates.sha256` 绑定 blind queries 文件（不是 `queries.b1-5.json`）；`schema_contract.schema_hash = 712c0e34…474a`（v3）；`snapshot.db_snapshot_hash = 8b16c047…f925`；两 fixture 的 SHA-256 与 §附录 A 记录一致；**checker 自身**的 SHA-256 由外部冻结记录（提交 / `git hash-object` + mtime）保证，脚本不自证自身 hash（自指 hash 不可实现）。
11. **目录存在性**（需 DB，`--skip-db` 可跳）：每个可接受 id 存在，记录的 `rows=` 与 id 数一致。
12. **只读自证**：脚本不写任何文件、不执行任何 SQL 写语句（`ReadOnlyMysql` 只允许 `SELECT`）。

用法与既有 checker 同形：`python3 scripts/eval/check_blind_holdout.py [--skip-db] [--quiet]`，退出码 0 = 全通过。

### 4.3 冻结对象与冻结时序（`B15d-14`）

| 项 | 要求 |
| --- | --- |
| 冻结对象（3 个 SHA-256） | `queries-blind.b1-5d.json`、`query-routing-blind.b1-5d.json`、`scripts/eval/check_blind_holdout.py` |
| 附带上报的不变量 | `docs/ai/golden-set.schema.json` hash **不变**（`8a67d834…9c0c`）；`tool_eval.py` 对 `0761845` **diff 为空**（`178d9370…8bb5`）；既有 `check_*.py`、别名 ×3、`tool-schema.b1-5.json` hash 不变（附录 B） |
| 冻结方式（二选一，报告写明用了哪种） | (a) 首次运行前把 fixture + checker **落进一次提交**，记录 commit SHA 与提交时间；(b) 未提交时用 `git hash-object -w` 记录 blob SHA + `stat` 记录 mtime |
| 时序证据 | 报告必须给出"**首次运行开始时间 > 冻结时间**"的可核对证据（commit time / blob 时间 / mtime + 运行产物的 `created_at`/文件时间）；缺该证据则本批结果**不作盲测证据** |
| 快照 | 两个 fixture 内 `snapshot.db_snapshot_hash` 与报告、`--out` 产物的 `db_snapshot` 三处一致 |

---

## 5. 运行计划

### 5.1 命令模板（与 B1.5c 报告 §10 同形，只换路径 + 加独立缓存路径）

```bash
# 0) 冻结与自检（运行前）
sha256sum docs/ai/golden/queries-blind.b1-5d.json \
          docs/ai/golden/query-routing-blind.b1-5d.json \
          scripts/eval/check_blind_holdout.py
python3 scripts/eval/check_schema.py --quiet      # 既有 checker 不变、仍须通过
python3 scripts/eval/check_golden.py --quiet
python3 scripts/eval/check_heldout.py             # B1.5c 证据仍完好
python3 scripts/eval/check_blind_holdout.py       # 新增只读断言（12 组）

# 1) G1 baseline（0 模型调用）—— 30 条全 model_calls = 0
python3 scripts/eval/tool_eval.py --variant G1 --cases queries \
  --queries-path docs/ai/golden/queries-blind.b1-5d.json \
  --routing-path docs/ai/golden/query-routing-blind.b1-5d.json \
  --allow-full-run --assert-no-annotation-read \
  --out .pi/artifacts/b1-5d-eval/blind-G1.json

# 2) 预热（1 次模型请求，不计入任何 case）
curl -sS http://<host>:11434/api/generate -d '{"model":"qwen2.5:7b","prompt":"ping","stream":false,"keep_alive":"10m"}' >/dev/null
#    → 报告记录：请求次数 1、`keep_alive` 值、预热完成时间

# 3) G1-T 冷跑（主数字）：独立缓存路径，跑前清空
mkdir -p .pi/artifacts/b1-5d-eval
rm -f .pi/artifacts/b1-5d-eval/resolver-cache.json
#    断言：不得对 .pi/artifacts/b1-5c-resolver-cache*.json 做任何 mv/rm/写
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path docs/ai/golden/queries-blind.b1-5d.json \
  --routing-path docs/ai/golden/query-routing-blind.b1-5d.json \
  --resolver-cache .pi/artifacts/b1-5d-eval/resolver-cache.json \
  --base-url http://<host>:11434 --allow-full-run --assert-no-annotation-read \
  --out .pi/artifacts/b1-5d-eval/blind-G1T-cold.json
#    期望 resolver.cache_state_at_load = "absent"，case 级 model_calls 合计 2

# 4) G1-T 热跑：同路径同变体（缓存一致性，非独立测量）
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path docs/ai/golden/queries-blind.b1-5d.json \
  --routing-path docs/ai/golden/query-routing-blind.b1-5d.json \
  --resolver-cache .pi/artifacts/b1-5d-eval/resolver-cache.json \
  --base-url http://<host>:11434 --allow-full-run --assert-no-annotation-read \
  --out .pi/artifacts/b1-5d-eval/blind-G1T-warm.json
#    期望 resolver.cache_state_at_load = "loaded"，model_calls 合计 0

# 5) 可选回放（0 模型调用，可复算）
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path docs/ai/golden/queries-blind.b1-5d.json \
  --routing-path docs/ai/golden/query-routing-blind.b1-5d.json \
  --replay-resolver .pi/artifacts/b1-5d-eval/blind-G1T-cold.json \
  --allow-full-run --assert-no-annotation-read \
  --out .pi/artifacts/b1-5d-eval/blind-G1T-replay.json
```

端点按 `docs/ai/README.md` §3 写作 `<host>:11434`；所有 `tool_eval.py` 运行必须带 `--assert-no-annotation-read`。

### 5.2 缓存与预热协议（oracle P1-1 的落点）

| 要求 | 实现 |
| --- | --- |
| 冷跑缓存隔离 | `--resolver-cache .pi/artifacts/b1-5d-eval/resolver-cache.json`（**独立路径**），跑前 `rm -f`；不带 `--no-cache`（空缓存 + 写盘 = 冷跑） |
| 冷跑证据 | 产物 `resolver.cache_path` = 上述路径、`resolver.cache_state_at_load = "absent"`、`resolver.no_cache = false` |
| 热跑证据 | 同路径、`resolver.cache_state_at_load = "loaded"`、`resolver.cache_problems = []` |
| B1.5c 缓存不动 | 运行前后对 `.pi/artifacts/b1-5c-resolver-cache.json`、`.stage1b.json`、`.stage1c.json` 取 `shasum` + `stat`，值不变写进报告 |
| 预热 | 1 次模型请求（§5.1 步骤 2），**不计入任何 case 的 `model_calls`**；报告单列"预热 1 次 + `keep_alive` 值 + 时间"，并声明冷启动延迟**仍未测**（沿用 `OPEN-b2`） |
| 冷/热一致性 | 对 30 条逐例比对 `records[].task_success` 与 `records[].resolver.chosen_movie_id`；**任一条不一致 = 缓存一致性失败**，如实报告（不回滚、不重跑冷跑） |
| 回放（可选） | `--replay-resolver <冷跑产物>`，`model_calls = 0`；用于复算，不新增测量 |

### 5.3 运行次数表（预登记，事后不得增加）

| 组合 | 次数 | 性质 |
| --- | --- | --- |
| 预热（模型请求） | **1** | 不计 case，不计入 30 条任何 `model_calls` |
| `G1` × blind 30 | **1** | baseline（非门槛对象），`model_calls = 0` |
| `G1-T` × blind 30（冷） | **1** | **主数字 / 门槛对象**，`model_calls = 2` |
| `G1-T` × blind 30（热） | **1** | 缓存一致性，不视为独立测量，`model_calls = 0` |
| `G1-T` 回放（同一缓存） | 0–1（可选） | 可复算，`model_calls = 0` |
| `G0` / `G2` | **0** | `D-3`：不跑，报告写"未测" |

### 5.4 模型调用预算（预登记）

- `G1`：**0**（30/30 条，逐条断言）；
- `G1-T` 冷：**2**（只有 2 条 `cjk_title` 用例触发解析器）；
- `G1-T` 热：**0**（2 条均命中缓存）；
- 回放：**0**；
- 预热：**1 次模型请求**（不计 case）；
- **总计 ≤ 4 次模型请求**（1 预热 + 2 冷 + 0 + 0–1 回放外，回放为 0）。

> **推论性结构约束**：`R-2` 探针的种子必须是**英文片名**，否则会新增第 3 条 CJK 解析调用、破坏 `cjk_title = 2` 与"冷跑 = 2 次调用"的预登记预算（见 §3.3.1）。

---

## 6. 验收条件

| ID | 条件 | 验证 |
| --- | --- | --- |
| `B15d-1` | `B01`–`B30` 两个新 fixture 存在，10/10/10，配额与 §3.3 覆盖项全满足 | checker 输出 |
| `B15d-2` | §3.4 的 11 条排除/结构规则为可执行断言并通过（含 2 条 CJK 与既有 5 条中文不同、无 2020s/合成行、逐 id 不重叠） | 同上 |
| `B15d-3` | 冻结 SHA-256 在**首次运行前**记录，运行后 fixture 未改 | 附录 A + 报告时间线 |
| `B15d-4` | **`G1-T` 冷跑任务级 ≥90%（27/30，`/30` 全分母）= 主门槛**；`G1` 为 baseline；报告同列两变体并标注变体名 | 报告主表 |
| `B15d-5` | 报告给 `/30`（主）、`/28(非CJK)`、`/28(去探针)`（+ 可选 `/29`）四个口径的成员定义与数值 + Wilson 95% CI；不剔除任何失败条目；**不得**用 28/29 分母宣称 PASS | 报告分母表 |
| `B15d-6` | 失败**逐条归因**（规则缺口 / 标注 / 已知缺口 `R-1`/`R-2`/`R-4`），且**不以归因为由改规则** | 报告失败表 |
| `B15d-7` | 报告写明 blind 定义、可达边界、不可控项（生成者知识、单人标注、n=30），并披露生成者**实际读过的文件**（`BL-7`） | 报告边界节 |
| `B15d-8` | 全部运行 `annotation_guard = true`；`G1` 逐条 `model_calls = 0`；`G1-T` 冷 = 2 / 热 = 0 | run record 逐条 |
| `B15d-9` | **无**生产代码/pom/CI/MCP/G3/DB 写入；`tool_eval.py` 对 `0761845` diff 为空 | `git diff --stat` + 逐文件 hash（附录 B） |
| `B15d-10` | 报告落在 `docs/ai/reports/b1-5d-blind-holdout.md`，含复现命令、运行次数、`model` tag+digest、`prompt_sha256`、缓存 SHA、预热与 `keep_alive` | 报告 |
| `B15d-11` | 缓存隔离：冷跑 `cache_state_at_load = absent` 且路径为 `.pi/artifacts/b1-5d-eval/resolver-cache.json`；热跑 `loaded`；B1.5c 三个缓存文件 hash/mtime **不变** | run record + 运行前后 `shasum`/`stat` 对照 |
| `B15d-12` | 冷/热逐例一致：30/30 条 `task_success` 与 `resolver.chosen_movie_id` 完全相同；不一致即如实报告为缓存一致性失败 | 报告一致性表（脚本比对可贴） |
| `B15d-13` | 2 条已知缺口探针（`R-1`/`R-2`）在 fixture 中**预声明**（`known_gap` + `labeling_note`），并在报告中单列其命中/失败与归因；主门槛仍以 `/30` 判定 | fixture + 报告探针表 |
| `B15d-14` | **checker-only**：`golden-set.schema.json` hash 不变；仅新增 `check_blind_holdout.py`，既有 `check_*.py`/`tool_eval.py`/别名/契约/阈值零改动；3 个 SHA 冻结 + "首次运行 > 冻结"证据 | `git diff`/hash 表 + 报告时间线 |
| `B15d-15` | 报告显式写 `G0`/`G2` = **未测**（`D-3`），且不以未测项推断结论 | 报告 |

> **门槛口径不新设**：27/30 沿用 B1.5c 已批准的 `G1-T` 主门槛；本任务只换样本，不换阈值、分母或变体定义。

---

## 7. 分母与统计口径

### 7.1 四个分母（成员必须显式列出）

| 口径 | 成员定义 | 用途 | 允许的表述 |
| --- | --- | --- | --- |
| **`/30`** | 全部 30 条（含 2 条探针、含 2 条 CJK） | **主门槛**：`G1-T` 冷 ≥ 27/30 | 可宣称 PASS/FAIL |
| `/28(非CJK)` | 30 − 2 条 `cjk_title`（与 B1.5c 的 `/28` 同义） | 与 B1.5c 的 `/28` 口径**同名对照**，量化 CJK 影响 | 仅敏感性，**不宣称 PASS** |
| `/28(去探针)` | 30 − 2 条已知缺口探针（`R-1` + `R-2`） | 量化"已知未裁定缺口"对数字的影响 | 仅敏感性，**不宣称 PASS**；报告须注明与 `/28(非CJK)` 数值可能巧合相等但成员不同 |
| `/29`（可选） | 30 − 1 条（须显式写明剔除哪一条） | 主 agent 允许追加的第三敏感口径 | 仅敏感性，**不宣称 PASS** |

**禁止**：① 把任一口径与 B1.5c 或 G1 的 `/28` 比"强弱"（成员不同、不可比）；② 用 `/28`/`/29` 的结果宣称主门槛 PASS；③ 在任何口径上剔除失败条目。

### 7.2 Wilson 95% 区间（`z = 1.96`，报告须给真实计算结果）

| 观测 | 比例 | Wilson 95% |
| --- | --- | --- |
| 27/30 | 90.0% | **[74.4%, 96.5%]**（下界约 74%，主 agent 记作 ~73%） |
| 28/30 | 93.3% | [78.7%, 98.2%] |
| 29/30 | 96.7% | [83.3%, 99.4%] |
| 30/30 | 100% | [88.6%, 100%] |
| 28/28 | 100% | [87.9%, 100%] |
| 27/28 | 96.4% | [82.3%, 99.4%] |

> **禁止表述**："95% 置信下 ≥90%"。`27/30` 的下界 ≈74%，`30/30` 的下界 88.6% 也低于 90%——与 B1.5c `R-5` 同一结论，必须复述。

---

## 8. 依赖、风险与回退

**依赖**：只读 `fabflix-mysql`（3307/`moviedb`）；Ollama `qwen2.5:7b`（1 次预热 + 2 条 CJK 冷跑；embedding 步 4 仍关闭）；B1.5c 的冻结产物作为"不得改动"的对照；`D-1`…`D-7` 已由主 agent 裁定；oracle 快速复核；issue 创建需用户授权。

| ID | 风险 | 处理 |
| --- | --- | --- |
| `R-d1` | **结果低于门槛**（很可能，因为 B1.5c 30/30 是在见过的样本上） | 如实报告；不改规则/用例、不重跑；需修复 → 另立 **B1.5e** + 新盲集 |
| `R-d2` | §3.4.3 逐 id 不重叠对大可接受集合可能不可行（in-sample 已用 462 个 id） | 抽样时先查重；确认不可行 → **上交裁定**（`D-7`），**不擅自放宽** |
| `R-d3` | 标注错误被当成规则失败（或反之） | 每条附 SQL 谓词 + `rows=`；失败先复核标注，按"标注问题"归因并**保留原数字**（另记 corrected 版本，不替换主数字） |
| `R-d4` | 2 条 CJK 的 LLM 表现主导结论（n=2） | 只做逐例披露，**不**估计中文解析成功率；CJK 计入 `/30` 分母 |
| `R-d5` | 抽样仍偏向"规则擅长的形状"（B1.5c 的 similar 全部带文本约束） | §3.3 强制注入同名种子、缩写人名、canonical 类型组、边界；并用 `R-2` 探针（`D-2`）覆盖"无文本约束"这一空白 |
| `R-d6` | 事后"补跑一次看看"破坏盲测 | §5.3 预登记；报告写"无按失败回调规则的任何重跑" |
| `R-d7` | 快照与 B1.5c 不同 → 数字不可比 | 记录 `db_snapshot_hash`；不一致则声明不可比 |
| **`R-d8`（新）** | **缓存泄漏**：冷跑复用 B1.5c 缓存或残留上次缓存 → 主数字不是冷跑 | §5.2 独立路径 + `rm -f` + `cache_state_at_load=absent` 断言（`B15d-11`）；B1.5c 三文件 hash/mtime 前后对照 |
| **`R-d9`（新）** | **探针污染结论**：`R-1`/`R-2` 未裁定的语义缺口被读成"泛化不足" | 预声明 `known_gap`；报告并列两种 `R-1` 解释的命中、单列探针表、并同时给 `/30` 与 `/28(去探针)`；结论措辞限定在"未裁定缺口的代价" |
| **`R-d10`（新）** | **冻结时序失效**：先跑后冻（或冻了又改）→ 盲测无效 | §4.3：3 个 SHA + 提交/`hash-object`+mtime + 报告"首次运行 > 冻结"证据；改 checker 也须重新冻结（外部记录其 SHA-256 + 提交/`hash-object` 证据；脚本不自证自身 hash） |
| `R-d11` | 预热未记录 → 冷启动延迟与"冷跑"混淆 | 预热单列 1 次请求 + `keep_alive` 记录；冷启动延迟仍标未测（`OPEN-b2`） |

**回退**：任何失败/异常都只影响"数字"与"结论措辞"，不影响仓库状态——本任务不写生产代码、不改契约，最坏情况是 `git checkout` 两个新 fixture 与 checker（未提交时直接删除）。

---

## 9. 决策与裁定

### 9.1 主 agent 已裁定（`D-1`…`D-7`）——不再是开放问题

| ID | 问题 | **裁定** | 影响章节 |
| --- | --- | --- | --- |
| `D-1` | 是否允许附加只读 checker / 是否改 schema | **checker-only**：新增只读 `scripts/eval/check_blind_holdout.py`；**不改** `golden-set.schema.json`（避免 `B01` 与 `H*` 模式冲突）；不改 `tool_eval.py`/别名/契约/阈值 | §4.1、§6 `B15d-14` |
| `D-2` | 是否纳入 `R-2` / `R-1` 探针 | **纳入 1 条 `R-2`（无文本约束 similar）+ 1 条 `R-1`（`Musical`/`Music`）探针**，预声明为已知缺口；主门槛仍 **30/30（27/30）**；另报去探针 28 条敏感性；**不得**用 28 条宣称主门槛 PASS | §3.3.1、§7、§6 `B15d-13` |
| `D-3` | 是否跑 `G0`/`G2` | **不跑**，报告写"未测" | §5.3、§6 `B15d-15` |
| `D-4` | 结果后 #74 的处置 / 是否立 B1.5e | **结果后定**；B1.5d issue 完成后**可关**；**#74 不自动关** | §1、issue 草稿 |
| `D-5` | 是否接受"标注者知道规则形状" | **接受并披露**；生成者不读 B1.5c 失败分析，只拿配额/覆盖/排除清单/契约/checker | §2、§3.5、`BL-7` |
| `D-6` | ID 前缀与版本串 | **`B01`–`B30`**，`blind_version: b1-5d-blind/v1` | §3.1 |
| `D-7` | 逐 id 不重叠不可行时的口径 | **上交裁定，不擅自放宽**（checker 不提供放水开关） | §3.4.3、`R-d2` |

### 9.2 oracle B1.5d 评审项的处置

| 评审项 | 级别 | 处置 |
| --- | --- | --- |
| 缓存/预热未定义 | P1 | **已修**（§5.2、§5.3、`B15d-11`/`B15d-12`） |
| `R-1`/`R-2` 未进 Scope/AC | P1 | **已修**（§3.3.1、§7、`B15d-13`） |
| checker 角色/schema 冲突 | P1 | **已修**（§4.1 checker-only、`B15d-14`） |
| 断言不足（1990s×`min_rating`、拒绝/澄清布尔、交叉标记归属） | P1 | **已修**（§3.4.7–11、§4.2） |
| 冻结范围不足 | P1 | **已修**（§4.3、`B15d-14`） |
| "不改 `check_*`"表述、"one shot"、`/28` 混淆、Wilson、标注者披露、issue 关闭语义 | P2 | **已修**（§0 P2-a…P2-f 对应章节） |

### 9.3 仍待复核的小项（oracle 快速复核 / 主 agent 批准）

| # | 待确认 | 本文件的建议口径 | 若不确认的后果 |
| --- | --- | --- | --- |
| `Q-1` | 路由文件版本字段名：`routing_version: b1-5d-blind-routing/v1` + `annotates.blind_version: b1-5d-blind/v1`（`D-6` 只定了 queries 侧串） | 如上 | 仅是命名一致性，不影响门槛；但 fixture 一旦冻结再改名即需 bump 版本 |
| `Q-2` | `/29` 是否要报，以及剔除哪一条 | 不报（`/30` + 两个 `/28` 已足够）；若主 agent 要报，须显式列成员 | 多一列敏感性，不影响主门槛 |
| `Q-3` | `R-2` 探针的种子是否允许与既有 105 条的**种子**重复（`§3.4.2` 禁止） | 仍禁止重复种子（换一个新种子片），否则与"不可复用"纪律冲突 | 若允许，需在 fixture 与报告中显式豁免并说明理由 |
| `Q-4` | `known_gap` 字段是新 fixture 的私有字段（不改 schema，checker 自己校验） | 采用 `known_gap: "R-1" / "R-2"`（其余用例缺省） | 无字段则"预声明"不可断言，`B15d-13` 退化为文字约定 |

---

## 10. 状态与实施结果回填（r4，2026-11-20）

**状态：🔴 已执行 / 主门槛 FAIL / 报告完成。** r3 的批准范围已全部执行；**阈值、分母、配额、验收条件（`B15d-1`…`B15d-15`）与变体定义一律未改**。

### 10.1 主结果（温度 0、单次运行；详细表见报告 §3）

| 变体 / 缓存状态 | `/30`（主分母） | Wilson 95% | `/28` 去 CJK | `/28` 去探针 | 路由层 | `model_calls` | 判定 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| **`G1-T` 冷跑（主门槛对象）** | **25/30 = 83.3%** | [66.4%, 92.7%] | 23/28 = 82.1% | 25/28 = 89.3% | 26/30 | **2** | ❌ **FAIL（< 27/30）** |
| `G1-T` 热跑 | 25/30 = 83.3% | 同上 | 23/28 | 25/28 | 26/30 | 0 | 与冷跑逐例一致 |
| `G1-T` 回放 | 25/30 = 83.3% | 同上 | 23/28 | 25/28 | 26/30 | 0 | 与冷跑逐例一致 |
| `G1`（baseline） | 23/30 = 76.7% | [59.1%, 88.2%] | 23/28 = 82.1% | 23/28 = 82.1% | 24/30 | 0 | baseline（非门槛） |

- **主门槛结论：FAIL。** 25/30 的点估计 83.3% 低于 90%，**差 2 条**；任何 `/28` 口径（含 25/28 = 89.3%）**都不得**用于宣称 PASS。
- **`G1-T` 相对 `G1` 净增 +2**（`B01`/`B14`，均中文译名）；`G1-T` 的 5 条失败是 `G1` 7 条失败的**真子集**，无回退。
- **5 条失败归因**：`B10` 越界请求未拒绝（真实规则缺口）、`B22` = `R-1`（预声明）、`B23` = `R-2` + `seed_title` 抽取（预声明；`R-2` 本批**未被干净测量**）、`B26` similar 约束求交致空集（真实执行语义缺口）、`B27` `Documentary` 未抽取（真实 genre 词汇缺口）。**未据任何失败改规则。**
- `G0` / `G2` = **未测**（`D-3`）；`G3` 未测；MCP 未实现。

### 10.2 冻结记录（首次运行前）

| 项 | 值 |
| --- | --- |
| `frozen_at_utc` | **2026-11-20T08:47:04Z**；首次运行 **08:52:34Z**（→ 首次运行晚于冻结） |
| 冻结方式 | **(b) 未提交**：`git hash-object -w` blob + `stat` mtime（任务禁止 commit/push） |
| queries | `21522516b57c8d514b233b45a71329c6e7cef335371ce7805e121058de41c694`（blob `9b1a5f5a…f51c`，31,705 B） |
| routing | `556761b415c2f947e1aa96df75636a78d22f45b411a89dfdeaa96e82a0d43dbe`（blob `757085f0…9d52`，27,109 B） |
| checker | `8259cc8dd924ce5a51f09dacbfed36a02a18be9e6ad7f161c630dfaacef37caf`（blob `9ebee94e…c53d`，48,924 B） |
| `db_snapshot_hash` | `8b16c047…f925`（fixture / 冻结记录 / 4 个运行产物一致） |
| 冻结期工具运行 | `tool_eval_runs = 0`；冻结后复核（08:50:01Z）三个 SHA 与冻结值一致 |
| 基线不变量 | `tool_eval.py` 对 `0761845` **空 diff**；既有 checker ×3、schema、别名 ×3、契约（v3 / `712c0e34…474a`）hash 全部等于附录 B |
| checker | **14 组断言全过**（`--skip-db` 与全量均 exit 0）；负向自测捕获全部变异体（计数两处记录不一致，见报告 §7.6） |

### 10.3 运行记录（预登记口径，事后未增加测量）

| 组合 | 次数 | `model_calls` | 备注 |
| --- | --- | --- | --- |
| 预热 | 1 | 不计 case | `keep_alive: 10m`，08:52:45Z；冷启动延迟仍未测（`OPEN-b2`） |
| `G1` | 1 | 0（逐条） | 08:52:34Z |
| `G1-T` 冷 | 1 | 2（`B01`/`B14`） | 08:52:52Z；`cache_state_at_load = "absent"`；专用路径 `.pi/artifacts/b1-5d-eval/resolver-cache.json` |
| `G1-T` 热 | 1 | 0 | 08:53:05Z；`loaded`；`cache_hits = 2` |
| 回放（错误调用） | 1 | 0 | **08:53:49Z：把 run JSON 当 replay cache → 0 个 case 执行，不是测量**，产物未保留 |
| 回放（正确） | 1 | 0 | 08:54:08Z；`replay`；与冷跑逐例一致 |
| `G0`/`G2`/`G3`/MCP | 0 | — | 未测 / 未实现 |

**模型请求合计 = 1（预热）+ 2（冷跑）= 3**，与 §5.4 预算一致；`qwen2.5:7b` digest `845dbda0…697e`；路由 prompt `58c8b708…2310`、解析 prompt `8d6d9e4f…b041`；B1.5c 的三个缓存文件 hash/mtime **未变**。

### 10.4 计划 vs 实现的差异（逐项）

| 计划项 | 实现 | 差异 |
| --- | --- | --- |
| §3.1 文件/ID/版本串 | 已按 `B01`–`B30`、`b1-5d-blind/v1`（routing `b1-5d-blind-routing/v1`）落盘 | 无 |
| §3.2 配额 / §3.3 覆盖 | 全部满足（含 2 条 CJK、4 条 `multi_slot`、1990s 5 条、无 2020s） | 无 |
| §3.4 排除规则 | 逐 id 与既有 105 条（486 个 id）**交集 0**，无需 `D-7` 上交 | 无（目标口径达成） |
| §4.1 checker-only | 仅新增只读 checker；schema 未改 | 无 |
| §5.2 缓存隔离 | 冷/热用专用路径且状态正确 | 无；**回放未显式传 `--resolver-cache`**（落回默认 B1.5c 缓存路径，实际未写入，仅 mtime 为证） |
| §5.3 运行次数 | 每组合 1 次；1 次错误回放调用 **0 case 执行** | 多 1 次**无效**调用（已如实记录，不构成重跑） |
| `B15d-11` | 冷 `absent` / 热 `loaded` 满足；B1.5c 缓存未变 | **缺运行前后 hash 快照对照**（以 mtime 近似） |
| `B15d-4`（主门槛） | **25/30 → FAIL** | 结果低于门槛，**阈值/分母/用例未调整** |

### 10.5 未决与后续

- `R-1`（`Musical`/`Music`）、`R-2`（是否允许从种子元数据派生收窄）、`B23` 的 `seed_title` 抽取规则、`OPEN-C3`（澄清式成功口径）**仍未裁定**；`B10`/`B26`/`B27` 的修复属 **B1.5e**。
- **B1.5e 必须使用新的 blind 集**；**严禁**用本批失败条目改规则后重跑本批（会使其变成 seen fixture）。
- #74 **保持 OPEN**；#76 = 报告完成 / **PR 待建** / 可在 PR 合并后关闭；**B-① 未开始**。

### 10.6 仍存验证缺口（详情见报告 §7.6）

回放的缓存路径卫生、回放端点记录为 `localhost`、预热无独立产物、错误回放调用无产物保留、checker 负向自测计数不一致、生成者 `grep` 曾命中 B1.5c 报告 5 行（盲测独立性披露项）。以上均**不影响**主门槛判定，但引用对应细节时须连带说明。

---

## 附录 A — B1.5d 冻结哈希（**已冻结：2026-11-20T08:47:04Z**，早于首次运行 08:52:34Z）

| 文件 | SHA-256 |
| --- | --- |
| `docs/ai/golden/queries-blind.b1-5d.json` | `21522516b57c8d514b233b45a71329c6e7cef335371ce7805e121058de41c694`（冻结 `2026-11-20T08:47:04Z`；blob `9b1a5f5a…f51c`） |
| `docs/ai/golden/query-routing-blind.b1-5d.json` | `556761b415c2f947e1aa96df75636a78d22f45b411a89dfdeaa96e82a0d43dbe`（blob `757085f0…9d52`） |
| `scripts/eval/check_blind_holdout.py` | `8259cc8dd924ce5a51f09dacbfed36a02a18be9e6ad7f161c630dfaacef37caf`（冻结 `08:49:52Z`；blob `9ebee94e…c53d`） |
| `db_snapshot_hash`（两 fixture 与报告三处一致） | `8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925` |

## 附录 B — 冻结基线对照哈希（2026-11-19 r2 实测，`main` @ `0761845`）

`git diff 0761845 -- scripts/eval docs/ai/golden-set.schema.json docs/ai/tool-schema.b1-5.json docs/ai/aliases docs/ai/golden` **输出为空**（本文件 r2 修订时实测）。

| 文件 | SHA-256 | 说明 |
| --- | --- | --- |
| `scripts/eval/tool_eval.py` | `178d9370d1ec3db8a529a781d614b633177ee4950af5f4cc475cfa9a34f08bb5` | 判分/路由逻辑，**不得改动** |
| `docs/ai/golden-set.schema.json` | `8a67d834c576e4be8c4fe9e159a33f7bba1425d51458592c40d4693bea7c9c0c` | `D-1` checker-only → **保持此值** |
| `scripts/eval/check_schema.py` | `8edd56d84a8d331f265a1e6fd7bfd41f157221145f3032674140633f83cd1821` | 既有 checker，不改 |
| `scripts/eval/check_golden.py` | `0ab8a0173ceb6c4e668ada863982b826c94a3a795160727307a8aafa96b40708` | 既有 checker，不改 |
| `scripts/eval/check_heldout.py` | `d436f5ca23526dbe9e968d45694350cabe90fafdb52283e575c2a19eeeccddd4` | 既有 checker，不改 |
| `docs/ai/tool-schema.b1-5.json` | `0ba15269de06172963f4513cc11a308c3684a5e10808d2ceb3289d525e9b1169` | `schema_hash = 712c0e34…474a`（v3） |
| `docs/ai/aliases/genre-aliases.json` | `f7126f022e69b2748a1a33407505bd22f62e366d2edf916e752c4ba642ecc203` | 冻结 |
| `docs/ai/aliases/person-normalization.json` | `06316859130167c644a4993fe6a869e21fcd5a4a88e92addb8aa91ef35e2dd98` | 冻结 |
| `docs/ai/aliases/title-aliases.json` | `98bfca9b6f17e112d25318212ad6aa3c886534f4e9be3617a228a198650bcbe4` | 冻结为空（`entries: []`，C10） |

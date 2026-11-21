# B1.5f — 新盲集（`C01`–`C30`）重测 `G1-T` 门槛（**🔴 r3：已执行 / FAIL / 报告完成**）

> **状态：本计划已执行完毕，结论 = FAIL。** 30 条 `C01`–`C30` 已生成、冻结（`2026-11-20T23:52:22Z`）并**只跑一次**（首次运行 `23:54:53Z`）；主门槛 `G1-T` 冷跑 **24/30 = 80.0%（Wilson [62.7%, 90.5%]）< 27/30 → FAIL**。**执行结果回填见 §11**；报告 = `docs/ai/reports/b1-5f-new-blind-holdout.md`。**本批数字自此冻结**，不得用本批失败条目改规则后重跑同一集。以下 §1–§10 保留 r2 的计划正文（含 oracle P1-1/P1-2 落点与主 agent 裁定），历史结论不改。
>
> | 元数据 | 值 |
> | --- | --- |
> | 文档类型 | 计划 / 方案（新盲集 + 一次冷跑 + 报告；**不做规则修复**） |
> | `last_updated` | 2026-11-20（r3 结果回填；§1–§10 的 r2 正文日期保留原样） |
> | 状态 | 🔴 **已执行 / FAIL / 报告完成**（fixture 与 checker 已生成并冻结；`G1`/`G1-T` 冷/热/回放各 1 次；**无规则/别名/契约/阈值改动**；issue **#81** = 报告完成 / PR 待建，可在 PR 合并后关闭；**#74 保持 OPEN**） |
> | 前置事实 | B1.5e 已合并（PR **#79**，`main` @ **`7915fcd`**）：in-sample `G1` **67/75 → 70/75**；B1.5d 的 5 条失败（`B10`/`B22`/`B23`/`B26`/`B27`）已修；**`Q12` 是唯一获批净回归**；B1.5d `G1` 28/30、held-out B1.5c `G1` 28/30 **均为 seen，仅记录**。报告 `docs/ai/reports/b1-5e-routing-gap-fixes.md`，计划 `docs/plans/b1-5e-routing-gap-fixes.md` |
> | 关联 issue | **GitHub [#81](https://github.com/PNZ-z/ai-powered-Fabflix/issues/81)**（`OPEN`、milestone **#3**、无 labels、正文 `Refs #74`、**无 closing keyword**）；规范记录 `docs/issues/issue-81-b1-5f-new-blind-holdout.md`。**r3 状态：#81 = 报告完成 / FAIL / PR 待建**，`gh issue view 81 --json body` 实测其正文已含 r2 的 3 处更正（`ranked, not narrowed` 计数 0、`two unexpected failures` 出现、`pending oracle review` 计数 0）且与本地正文逐行一致。#74 **保持 OPEN**；#76/#78 已随各自 PR 关闭 |
> | 前序契约 / 证据 | 契约：`b1-5c-tool-routing-fixes.md`（主门槛 = `G1-T` ≥ 27/30，`/30` 全分母）、`b1-5d-blind-holdout.md`（`BL-1`…`BL-7`、配额/排除/冻结/运行模板）、`b1-5e-routing-gap-fixes.md` §7.1/§7.3（探针清单与 checker 依赖）；命名沿用 B1.5 系列序号，建号后补 `issue-<编号>-`。证据 = `git log`（`7915fcd`、PR #79）、B1.5e 报告 §1/§8/§9/§11、B1.5e 计划 §7、B1.5d 计划 §2–§7、B1.5d 报告 §0/§1.4、`check_blind_holdout.py`（`:84-130`、`:396-420`、`:486-546`、`:610-645`）、本轮直接读取四个 golden 文件核对条数（60+15+30+30）与可接受 id 并集（**718**） |
> | 本文的验收口径 | 主门槛只有一条：**`G1-T` 冷跑 ≥ 27/30**。其余 AC 都是**本批证据完整性**条件，不是门槛，也不能被引作泛化结论 |
> | 执行结果（r3） | `G1` **23/30 = 76.7%**（路由 23/30、0 模型调用）；**`G1-T` 冷 24/30 = 80.0%**（路由 24/30、2 次模型调用、缓存 `absent`）**→ FAIL**；热/回放 24/30、0 调用、逐例一致；6 条失败 = `C01`/`C05`/`C12`/`C16`/`C17`/`C18`；`/28` 非CJK 23/28、`/28` 去 `P-1`/`P-2` 22/28、`/27` 22/27（**均不得宣称 PASS**）。详见 §11 |

### r2 修订记录（2026-11-22）

来源：`.pi/artifacts/oracle-b1-5f-review.md`（P1-1、P1-2 + P2-1…P2-8）与主 agent 裁定。**本轮只改文档：未生成任何 fixture、未运行任何评测命令、未创建或修改任何 GitHub issue（仅用 `gh issue view 81` 做只读核验）。**

| # | 评审点 | r2 落点 |
| --- | --- | --- |
| `RV-1` | **P1-1**：`P-3` 的 "按排序、不收窄" 没有可冻结的可接受集（无法判分、不可复现） | §3.3 `P-3` 改为 **3+ canonical genres + 已知导演**、查询文本只说"类似 X"（无文本约束）；标注 = **该导演的其他目录行**（固定 SQL 谓词 + `rows=`），convention = `same director`；③b2 **不派生**，靠 `_similar` 的 `same_director DESC` 排序命中。§4.2 组 16、`B15f-2` 同步 |
| `RV-2` | **P1-2**：135 vs 165 排除基线仍是"待确认" | §3.4 定稿为 **135 条计分 fixture（60+15+30+30）+ 5 条未评分 negatives**；**165 的表述删除**（标为交办稿笔误、已被取代），`OD-f2` 关闭；issue 文档同步删除"待确认"行（已发布 #81 亦无 `165`） |
| `RV-3` | `P-4` 与容错算术 | §3.3 / §6 `B15f-5` / §7：`P-4` **预期失败仍占 `/30` 的 3 条容错中的 1 条**；报告必须写"**只剩 2 条非预期失败空间**"（29/30 ≠ 30/30） |
| `RV-4` | `P-1` 的 union 断言须可复核 | §3.3 / §4.2 组 15：**top-5 ⊆ 13 行 union** 为 checker 硬断言，并记录实际 top-5（`R-1` 是召回扩大，不是等价替换） |
| `RV-5` | checker 形态与自证 | §4.1 方案 A 定为已裁定；§4.4 新增 **变异自测（≥5 个变异必须被捕获）**；`check_blind_holdout.py` **byte-identical**（`8259cc8d…7caf`） |
| `RV-6` | 种子独立性 | §3.4 新增 **seed-title 与既有 29 个不同种子片名零重叠**（本轮实测：`query-routing.b1-5.json` 15 + `query-routing-heldout.b1-5c.json` 7 + `query-routing-blind.b1-5d.json` 7 = **29 个互不相同的 `seed_title`**），§4.2 组 17 |
| `RV-7` | 盲测边界披露 | §2 边界注 + §6 `B15f-9`：报告必须明说**标注者知道探针形状**（探针清单本身公开）、n = 30、单人标注、无第二标注人；**不得声称"标注者对修复一无所知"** |
| `RV-8` | 分母成员显式 | §7：`/27` 仅在报告选择输出时出现，**成员必须逐条列名**（`P-1`/`P-2`/`P-4`）；**任何 `/28`/`/27` 不得用于宣称 PASS** |
| `RV-9` | issue 已在并发流程中提前创建 | `gh issue view 81` 实测 **#81 OPEN**（milestone #3），本地草稿已改名为 `docs/issues/issue-81-b1-5f-new-blind-holdout.md`；主 agent 裁定**方案 A**（承认 #81、不收尾为 🟡、不关闭）。#81 已发布正文与 r2 有 **3 处不一致**（Background 第 2 条的 "ranked, not narrowed"、P-4 段缺容错句、Notes 仍写 `r1, pending oracle review`），**逐字补丁 = issue 文档附录 D**，**待主 agent 决定是否 `gh issue edit` 更正**（本文不自行改 GitHub） |

---

## 1. 目标与非目标

**目标**：用一套**从未参与过任何规则开发**的新 30 条（`C01`–`C30`）一次性冷跑 `G1-T`，回答 B1.5e 之后唯一未回答的问题——**`≥ 27/30` 的门槛是否在新样本上成立**，并留下可复核的冻结、运行与归因记录。

| 不做（非目标） | 理由 |
| --- | --- |
| **不修任何规则**：不改 `scripts/eval/tool_eval.py`、别名、契约、阈值、分母 | 与 `BL-6` 同纪律；失败只归因，修复另立任务 + 另一套新盲集 |
| **不改任何生产代码**：`src/main/**`、`pom.xml`、CI、DB 写入、迁移 | 相似/推荐执行器与中文解析器都是评测替身 |
| **不改 B1.5a–B1.5e 的任何 fixture / 计划 / 报告 / checker** | 已合并证据；`check_blind_holdout.py` 的 SHA `8259cc8d…7caf` 是 B1.5d 证据的一部分 |
| **不生成、不运行、不回填 B1.5e 的修复** | B1.5e 已在 `7915fcd` 落地；B1.5f 只**测量**其结果，不得借测量改实现（§2 `BF-4`） |
| **不把 B1.5c held-out / B1.5d / B1.5e 的 in-sample 数字当门槛；不改 `Q48`/`Q12`；不声称泛化 / 生产可用 / B-① 可开始** | 均已 seen（B1.5d `G1-T` 25/30 只作历史引用），`Q12` 是已批准例外而非待修缺陷；n = 30、单人标注、替身执行器 → 结论只覆盖这 30 条 |

---

## 2. "blind" 的定义与纪律（本任务 core）

**可操作定义**：30 条的文本、标注、可接受集合在**第一次运行 `tool_eval.py` 之前**已完整冻结并记录 SHA-256；标注期间**没有任何人**运行过任何变体，且标注者**不是** B1.5e 修复的作者。

| # | 纪律 | 可执行控制 |
| --- | --- | --- |
| `BF-1` | **生成者独立**：与 B1.5e 修复的作者不是同一人/同一会话 | 报告"标注者独立性"节记录会话与角色；若无法分离，本批结果**降级为 seen 回归**并如实标注 |
| `BF-2` | **不读修复细节**：禁止读 B1.5e 报告 **§2/§3/§4/§5/§6/§7**（五个缺口、逐项改动、`R-1`/`R-2` 语义、in-sample 前后对照、自测表、指纹），禁止读其 diff（`8cabb2d`/`a7c2f7a`）、`.pi/artifacts/b1-5e/**`、`scripts/eval/tool_eval.py` 正文 | 报告必须列出**实际读过的文件清单**（`BL-7` 形式）；允许读：B1.5e 报告 §1/§8/§9/§11、B1.5e 计划 §7、本文、契约、`check_*` checker、只读 MySQL |
| `BF-3` | **标注期零运行**（`BL-1`：禁止任何 `--ids`/`--limit`/`--dry-run` 试跑）+ **只读 SQL 标注**（`BL-2`：谓词 + `rows=`） | 运行次数表从 0 起；报告给"首次运行时间 > 冻结时间"证据；`ReadOnlyMysql` 拒绝非 `SELECT` |
| `BF-4` | **每个 (variant × 缓存状态) 只跑一次**；不重跑、不换用例、不改规则；**本任务对 `scripts/eval/` 的唯一新增是 B1.5f 的只读 checker** | §5.3 预登记；`git diff --stat` 证明 `tool_eval.py` 零改动；fixture 若改动 → bump `blind_version` 记 "run 2"，**不作盲测证据** |
| `BF-5` | **失败只归因**（规则缺口 / 标注问题 / 预声明探针）；标注问题保留原数字，另记 corrected 版，**不替换** | 报告失败表逐条列出 |
| `BF-6` | **similar 标注必须写明约定**：`same director` / `shared genre × window` / `hybrid` | 缺约定的 similar 用例**不得冻结**；checker 硬断言（§4.2 组 13） |

> **已知边界（沿用 B1.5d `D-5`，r2 按 oracle `P2-1` 加强）**：生成者必然知道"规则的存在与大致形状"，且本任务的**探针清单本身就泄露了被覆盖的形态**（`musicals`、无文本约束 similar、越界请求、片名含类型词、复数类型词、同导演预声明失败）——blind 排除的是"用这批样本调过参"，**不能排除"标注者知道覆盖面"**。因此报告必须原句写入："**标注者知道探针形状（探针清单随计划/issue 同时公开）；blind 仅意味着这 30 条从未被任何变体跑过、也未据此调参；不得声称标注者对修复一无所知**"；并披露 **n = 30、单人标注、无第二标注人**。违反此披露的结论句一律不得发布。

---

## 3. 样本、配额与排除

### 3.1 文件与 ID

| 项 | 值 |
| --- | --- |
| 查询 / 路由文件（新） | `docs/ai/golden/queries-blind.b1-5f.json`（`blind_version: b1-5f-blind/v1`、`created`、`snapshot`、`quota`、`queries[30]`）与 `query-routing-blind.b1-5f.json`（`routing_version: b1-5f-blind-routing/v1`、`annotates.sha256` 绑定上一文件、`schema_contract` 记 v3 / `712c0e34…474a`） |
| ID | **`C01`–`C30`**（新前缀，不与 `Q`/`T`/`H`/`B` 冲突；B1.5d checker 的 `EXPECTED_IDS` 是 `B` 前缀，故必须参数化或换脚本） |
| 快照 / 别名基线 | 必须等于 `db_snapshot_hash = 8b16c047…f925`（21,087 部，196–2024），变了 → 重冻并声明"不可比"；`aliases_version = b1-5c-aliases/v2`（B1.5e 落地的 `["Music","Musical"]` 同族）**不改**，只记 SHA 作本批指纹 |

### 3.2 配额（与 B1.5c/B1.5d 同口径，便于对照；**不复用任何既有用例**）

| 维度 | 配额 |
| --- | --- |
| track | `zh_zh` 10 / `zh_en` 10 / `en_en` 10 |
| intent | `search_by_title` 5 / `filter_catalog` 9 / `similar_movies` 7 / `recommend_by_preference` 3 / `clarify_or_refuse` 6（拒绝 4 + 澄清 2） |
| 交叉标记 | `multi_slot` **4**（只在 `filter_catalog`/`similar_movies`）/ `cjk_title` **恰好 2**（只在有 title 槽的 intent） |
| 年代 / 禁项 | **1990s 3–5 条**；2010s ≤1；**禁止 2020s**；其余以 1950s–1980s 为主。禁：合成行（`tt9000001…tt9000008`、`Movie1`/`Movie2`）、既有 5 个中文译名（`肖申克的救赎`/`超人`/`黑客帝国`/`阿甘正传`/`泰坦尼克号`） |

### 3.3 必含探针（覆盖预算：**8 组，占 30 条中的 10 条**）

| # | 探针 | intent 槽位 | 条数 | 标注口径（冻结前必须写进 `labeling_note`） | 预期 |
| --- | --- | --- | --- | --- | --- |
| `P-1` | `musicals` filter = **`R-1`** | `filter_catalog` | 1 | **union（`Music` ∪ `Musical`）**：1980–1982 窗口 = **13 行**（`Music` 12 + `Musical` 1 = `RMY15`）；`acceptable_movie_ids` = 13 行全集；`snapshot_row_refs` 同时记 `Music`/`Musical` 两个读数。**checker 硬断言：top-5 ⊆ 13 行 union**（不只 `acceptable_hit` 非空），并在报告里记录实际 top-5；若 top-5 里出现那唯一一行 `Musical`，union 仍正确。**窗口从 r2 的 1948–1950/25 改为 1980–1982/13，原因 = 旧窗口的 24 个 Music id 与 B1.5d B22 可接受集逐一相同，违反 135 排除基线；主 agent 批准 B。**（**r3 已落地并实测**：`C22` 实际 top-5 = `CSr8`/`PaB10`/`RFM12`/**`RMY15`**/`HJS10`，5/5 ⊆ 13 行 union，≡ 唯一 `Musical` 行出现在结果里；见 §11.4） | 通过（B1.5e 已落码）；**不得**用 B1.5d 的 24 行 `Music` 口径（`R-1` 是召回扩大，不是等价替换） |
| `P-2` | 无文本约束 similar = **`R-2`** | `similar_movies` | 1 | 种子**恰好 2 个 canonical genres**；按 **shared genre × 同年代**标注（`R-2` ③b2 会派生 seed 全部类型 + 整十年）；种子必须是**英文片名**且**不与既有 29 个种子片名重复**（否则破坏冷跑 2 次调用预算 / 污染独立性） | 通过 |
| `P-3` | 种子 **3+ canonical genres + 已知导演** similar（检验 ③b2 边界：**不派生**） | `similar_movies` | 1 | 查询文本**只说"类似 X"**（无文本约束、不点导演名，`result_constraints = {}`）；`acceptable_movie_ids` = **该导演的其他目录行全集**（固定 SQL 谓词 `SELECT id ... WHERE director = <seed director> AND id <> <seed id>` + `rows=n` + id 列表），`labeling_note` 的 convention = **`same director`**。③b2 因 `seed_genres ≥ 3` **不派生**，通过靠 `_similar` 的 `ORDER BY same_director DESC, …`（`tool_eval.py:866-872`）命中。**选择约束**：`5 ≤ n ≤ 25`（`n ≤ 25` 是 checker `:257-263` 的 `acceptable_movie_ids` 上限；`n ≥ 5` 才能断言 **top-5 ⊆ 可接受集**）；并加断 `seed.genres ≥ 3` 与 `director` 非空 | 通过；若目录里找不到同时满足"3+ canonical genres + 已知导演 + 5–25 行其他目录行"的种子 → **暂停交主 agent**，**不得**退回"任意 top-5"或"按排序"叙事（那不可判分） |
| `P-4` | **`Q12` 同导演探针** | `similar_movies` | 1 | **同导演**标注 + 种子 2 类型 + **零文本约束**（与 `Q12` 同形）；fixture 里**预声明"已知不可满足/预期失败"**，且 `acceptable_movie_ids` = 该导演的其他目录行（SQL + `rows=`） | **预期失败**；用于复现 `Q12` 例外；**不计入"未预期失败"**，也不得据此改规则——但它**仍占 `/30` 的 3 条容错中的 1 条**（见 §6 `B15f-5`） |
| `P-5` | 越界请求 ×**2**（中文**票房** 1 条 + 英文 1 条） | `clarify_or_refuse`（拒绝） | 2 | `expects_no_match: true` + 空可接受集 + 无 expected tool；**两条查询都不得出现 `《…》` 或任何 CJK 片名**（否则触发第 3 次 resolver 调用），英文那条也不得夹 CJK | 通过（`F-1` 拒绝规则）；运行后断言冷跑**恰好 2 次**模型调用 |
| `P-6` | 片名含**类型词**的 similar | `similar_movies` | 1 | **shared genre × window**：类型只取文本显式声明的那一个（`F-2` 掩码后），片名里的类型词不得成为约束；种子**英文片名**且不与既有 29 个种子片名重复 | 通过 |
| `P-7` | **复数类型词** filter | `filter_catalog` | 1 | 直接谓词（`documentaries → Documentary` 一类，只允许已落码的复数词形） | 通过 |
| `P-8` | **2 条 CJK 译名** | 有 title 槽的 intent | 2 | 两个**全新**中文片名（不在 `title-aliases.json`，该表仍为 `entries: []`） | 通过 / 失败都只逐例披露（n = 2，不估计中文成功率） |

**探针预算核算**：similar 7 = `P-2`+`P-3`+`P-4`+`P-6`+3 条自由；filter 9 = `P-1`+`P-7`+7 条自由；拒绝 4 = `P-5`（2 条）+2 条自由；澄清 2、recommend 3 自由；search 5 中 2 条给 `P-8`；4 条 `multi_slot` 从 filter/similar 的自由额度出。**探针放不进配额时先交主 agent，不得挤掉配额断言。**

**容错算术（预登记，防止事后放宽）**：主门槛 `≥ 27/30` ⇒ 最多 3 条失败。这批里 **`P-4` 是预声明失败**，占掉其中 1 条 ⇒ **只剩 2 条非预期失败空间**；报告必须原句写出这句话，并禁止用"`P-4` 不算失败"把 29/30 讲成 30/30。

### 3.4 排除规则（硬断言）

**权威基线（`OD-f2` 已裁定，交办稿的 "165" 已废弃）**：既有计分用例 = **135 条**，逐文件枚举如下（本轮实读校验）：`queries.b1-5.json` **60** + `tool-cases.b1-5.json` **15** + `queries-heldout.b1-5c.json` **30** + `queries-blind.b1-5d.json` **30**；另有 `negative.b1-5.json` **5 条未评分**用例（不参与排除断言，也不参与分母）。**不存在 165 这个数**：它是交办稿的笔误，已被本节取代；checker 断言的是"四个文件的并集 = 135"，不是神奇数字 165。

1. query 文本（归一化去标点/大小写后）不得与上述 **135 条计分用例**任一相同；`negative.b1-5.json` 5 条未评分用例**不计入**该断言。
2. 种子片名 / 种子 `movie_id` 不得与上述用例重复；可接受 id 集合不得与任一既有集合完全相同；**逐 id 不重叠为目标口径**（既有并集 **718** 个 id，本轮实测）。
3. **seed-title 独立性单列硬断言**：三个 routing 文件里的 `seed_title` 本轮实测共 **29 个互不相同的片名**（`query-routing.b1-5.json` 15 + `query-routing-heldout.b1-5c.json` 7 + `query-routing-blind.b1-5d.json` 7；`tool-cases.b1-5.json` 无 `seed_title` 槽），本批每个种子的片名（casefold 后）**必须不在该 29 集合且彼此不同**。
4. 不得使用合成行（§3.2 禁项）、不得出现 2020s、1990s 3–5 条、2010s ≤1；**1990s 用例不得依赖 `min_rating`**（评分只覆盖 2001+），除非是显式声明的空集边界用例（`expects_no_match: true` + 空可接受集 + `labeling_note` 写明）。
5. 拒绝用例三件一致；澄清用例必须有非空可接受集 + title 槽（否则 `build_cases` 直接 `EvalError`）；除 2 条 `cjk_title` 外**任何用例不得含 `《…》`**（冷跑 2 次调用预算前提）；每条可接受集必须 SQL 可复现（谓词 + `rows=` + id 列表）且全部存在于目录，且**每个可接受集 ≤ 25 个 id**（沿用 checker `:257-263` 的上限）。
6. 本批内部也不得重复：30 条之间 query 文本、种子片名、可接受 id 集合两两不同。

> **`OD-f2` 已关闭**（r2）：以"四文件枚举 = 135 计分 + 5 未评分 negatives + 718 id 并集 + 29 个既有种子片名"为唯一口径。交办稿的 **165 作废**，不得到处引用；若后续发现新文件改变了这个数，**先重开本节并重冻**，不得让 checker 静默地接受新数。

### 3.5 `labeling_note` 约定（硬断言）

**三种约定**：`same director` = 同导演的目录行（`P-3`、`P-4`；`P-4` 为已知不可满足形态）；`shared genre × window` = 共享 canonical 类型 × 同年代/窗口（`P-2`/`P-6`）；`hybrid` = 两者混用，必须逐条写清哪部分是导演、哪部分是类型/窗口。**缺约定的 similar 用例不得冻结**；`P-3`/`P-4` 必须带固定 SQL 谓词 + `rows=` + 与该谓词一致的 id 全集（checker 会重算行数）；非 similar 用例也必须记 `rows=` + 谓词（沿用 `BL-2`）。

> **不再使用"按排序 / ranked"作为约定名**：r1 里 `P-3` 的 "按排序、不收窄" 无法形成可接受集（oracle P1-1），r2 已改为 `same director` + 可复现 SQL。任何写成"输出前 5 名即为正确答案"的标注 **一律不可冻结**。

---

## 4. 冻结产物与 checker

### 4.1 B1.5d checker 不动；B1.5f 用新脚本（方案 A 已由主 agent 裁定）

| 方案 | 内容 | 评价 |
| --- | --- | --- |
| **A（已裁定采用）** | 新增只读 `scripts/eval/check_blind_holdout_b15f.py`，**import** `check_blind_holdout` 的只读辅助（`load_json`/`sha256_of`/`normalise_text`/`cjk_titles_of`/`ReadOnlyMysql`），自己实现 12 组断言 + 5 组新断言（共 17 组） | `check_blind_holdout.py` **byte-identical**（SHA `8259cc8d…7caf`），B1.5d 证据完好；代价 = 断言实现有重复（约 400–450 行），因此**强制以 §4.4 变异自测证明断言真的会红** |
| B | 复制一份脚本再改常量 | 与 A 等价但完全重复 950 行，弃 |
| ~~C/D~~ | C：给 `check_blind_holdout.py` 加 `--queries-path/--ids-prefix/--blind-version` 参数；D：只靠人工核对 | **均否决**：C 改它的 SHA 就改写了 B1.5d 的冻结证据（`B15d-14`）；D 使 `BF-6`/§3.3 的预声明无法断言 |

> `OD-f1` 已裁定：采用方案 A，**只读断言工具，不参与路由/判分**（与 B1.5d 同一论证），并通过 `git diff --stat` + SHA `8259cc8d…7caf` 证明 `check_blind_holdout.py` 未被改过。

### 4.2 B1.5f checker 断言清单（相对 B1.5d 的 12 组）

沿用（换成 `C01`–`C30` + 新版本串 + 新 SHA 常量）：**结构 / ID / track-lang / intent 配额 / 布尔与交叉标记 / 拒绝与澄清形态 / 排除 / 年代 / 别名空表 / 绑定与指纹 / 目录存在性 / 只读自证**。

| 新增组 | 断言 |
| --- | --- |
| 13 `labeling_note` 约定 | 每条 `similar_movies` 的 note 必须命中 `same director` / `shared genre × window` / `hybrid` 之一，并带谓词 + `rows=`；缺失即失败（`BF-6`）；**`P-3` 的 convention 必须是 `same director`** |
| 14 探针清单完整性 | `P-1`…`P-8` 各命中一次（`P-5`/`P-8` 各 **2** 条，后者 = `cjk_title`，由原组 5 覆盖）；`P-4` 必须带"预期失败"预声明字段（`probe: "R-2/Q12-unsat"` 或等价 `known_gap` 扩展值，checker-local 字段，**不改 schema**） |
| 15 `R-1` union 读数 + top-5 子集 | `P-1` 的 `snapshot_row_refs` 必须同时含 `Music` 与 `Musical` 读数、union 谓词的 `rows=13`（1980–1982 窗口；Music 12 + Musical 1 = RMY15），SQL 复核 union 行数与记录一致；**并且硬断言该用例的 top-5 ⊆ 13 行 union**（从 run record 读回并校验，不只检查 `acceptable_hit` 非空）；**B1.5d checker 的"24 行 / `Music` 口径"断言不得被继承** |
| 16 种子类型数 + `P-3` 可接受集 | `P-2` 种子 = 恰好 2 个 canonical genres（DB 段，走 `genre_enum`/canonical 折叠）；`P-3` 种子 **≥ 3 个 canonical genres 且 `director` 非空**，其 `acceptable_movie_ids` 必须**等于** SQL `director = <seed director> AND id <> <seed id>` 的全集（checker 重算行数与集合相等，`5 ≤ rows ≤ 25`），且 top-5 ⊆ 该集合 |
| 17 seed-title 独立性 | 从三个既有 routing 文件汇出 **29** 个 `seed_title`（`query-routing.b1-5.json` / `query-routing-heldout.b1-5c.json` / `query-routing-blind.b1-5d.json`；B1.5d 版 `:508-537` 只扫了前两个文件），断言本批每个种子片名不在该集合，且本批内部种子片名两两不同 |

**排除组必须升级**：B1.5d 版硬编码 `len(previous) != 105`（`:501`）与三个 earlier 文件（`:490-496`）；B1.5f 版必须把 `queries-blind.b1-5d.json`（30）与 `query-routing-blind.b1-5d.json` 纳入 → **135 条计分用例 + 29 个种子片名 + 718 id 并集**，并断言 B1.5f 自身内部无重复；`negative.b1-5.json` 的 5 条**不计分、不进排除断言**。

### 4.3 冻结对象与冻结时序

| 项 | 要求 |
| --- | --- |
| 3 个 SHA-256 + 冻结记录 | `queries-blind.b1-5f.json`、`query-routing-blind.b1-5f.json`、`scripts/eval/check_blind_holdout_b15f.py`（checker 不自证自身 hash，同 B1.5d）；记录文件 `.pi/artifacts/b1-5f-freeze.json`（`tool_eval_runs = 0`、三 SHA、快照、mtime） |
| 附加上报的不变量 | `tool_eval.py`、别名 ×3、`tool-schema.b1-5.json`（v3 / `712c0e34…474a`）、`golden-set.schema.json`、既有 `check_*.py`（含 `check_blind_holdout.py` `8259cc8d…7caf`）、B1.5a–B1.5e 全部 fixture/计划/报告 hash **不变** |
| 冻结方式与时序 | (a) 首次运行前把 fixture + checker **落进一次提交**记 commit SHA + 时间，或 (b) 未提交时 `git hash-object -w` 记 blob SHA + `stat` 记 mtime；报告必须给出"**首次运行开始时间 > 冻结时间**"的可核对证据，缺此证据 → 本批**不作盲测证据** |

### 4.4 checker 变异自测（强制，≥ 5 个变异必须被捕获）

新 checker 是从 B1.5d 重实现的，**必须证明它真的会红**：在临时目录拷一份 fixture + checker，逐个注入下列变异并记录实际报错信息，报告里给出"变异 → 失败断言 → 退出码"表。**少于 5 个变异被捕获 → checker 不可用于冻结**。

| 变异 | 期望被捕获的断言 |
| --- | --- |
| `MUT-1` 把 `C07` 改成 `B07`（或改 ID 顺序） | 组 2（ID = `C01`–`C30`） |
| `MUT-2` 把 `P-1` 的 `Music` 读数改回 **12 行**单口径 | 组 15（union 13 行 + top-5 ⊆ union） |
| `MUT-3` 删掉一条 similar 用例的 `labeling_note` 约定 | 组 13 |
| `MUT-4` 删掉 `P-4` 的"预期失败"预声明 | 组 14 |
| `MUT-5` 把 `P-3` 的 `acceptable_movie_ids` 换成"与导演无关的任意 5 行" | 组 16（= 导演其他目录行全集） |
| `MUT-6` 把某条 `seed_title` 换成既有 29 个之一 | 组 17 |
| `MUT-7` 改 `P-2` 种子到 3 个 genre（或 `P-3` 到 2 个） | 组 16（种子类型数） |
| `MUT-8` 移除一条 `《…》` 禁令内的 CJK 片名 / 或把只读 SQL 换成写操作 | 组 5（`cjk_title` = 2）或组 12（只读自证） |
---

## 5. 运行计划

### 5.1 命令模板（只换路径与缓存目录）

```bash
Q=docs/ai/golden/queries-blind.b1-5f.json; R=docs/ai/golden/query-routing-blind.b1-5f.json; O=.pi/artifacts/b1-5f-eval; K=$O/resolver-cache.json
sha256sum $Q $R scripts/eval/check_blind_holdout_b15f.py            # 0) 三 SHA（与冻结记录比对）
python3 scripts/eval/check_schema.py --quiet && python3 scripts/eval/check_golden.py --quiet && python3 scripts/eval/check_heldout.py && python3 scripts/eval/check_blind_holdout.py && python3 scripts/eval/check_blind_holdout_b15f.py
python3 scripts/eval/tool_eval.py --variant G1 --cases queries --queries-path $Q --routing-path $R --allow-full-run --assert-no-annotation-read --out $O/blind-G1.json
curl -sS http://<host>:11434/api/generate -d '{"model":"qwen2.5:7b","prompt":"ping","stream":false,"keep_alive":"10m"}' >/dev/null   # 1) 预热
mkdir -p $O && rm -f $K                                            # 2) 冷跑：cache_state_at_load 必须 absent，model_calls 合计 2
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries --queries-path $Q --routing-path $R --resolver-cache $K --base-url http://<host>:11434 --allow-full-run --assert-no-annotation-read --out $O/blind-G1T-cold.json
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries --queries-path $Q --routing-path $R --resolver-cache $K --base-url http://<host>:11434 --allow-full-run --assert-no-annotation-read --out $O/blind-G1T-warm.json   # 3) 热跑：loaded，model_calls 0
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries --queries-path $Q --routing-path $R --resolver-cache $K --replay-resolver $O/blind-G1T-cold.json --allow-full-run --assert-no-annotation-read --out $O/blind-G1T-replay.json   # 4) 可选回放
```

**B1.5d 的两处操作缺陷必须避免**：① 回放必须**显式传 `--resolver-cache`**，且 `--replay-resolver` 只能吃**冷跑产物**（B1.5d 有一次把 run JSON 当 cache 的无效调用）；② 运行前后对 `.pi/artifacts/b1-5c-resolver-cache*.json` 取 `shasum` + `stat`，证明未被动过。

### 5.2 缓存与预热协议

| 项 | 要求 |
| --- | --- |
| 冷跑 | 专用路径 `$K`，跑前 `rm -f`，**不带** `--no-cache`；`resolver.cache_state_at_load = "absent"`、`cache_path` = 专用路径 |
| 热跑 | 同路径、`"loaded"`、`cache_problems = []`；逐例比对 `task_success` 与 `resolver.chosen_movie_id`，任一条不一致 = 缓存一致性失败（**如实报告、不回滚、不重跑**） |
| 预热 | 1 次模型请求，单列时间 + `keep_alive` 值；**冷启动延迟仍未测**（沿用 `OPEN-b2`） |

### 5.3 运行次数表（预登记，事后不得增加）

| 组合 | 次数 | 性质 |
| --- | --- | --- |
| `G1` × 30（+ 预热 1 次模型请求，不计 case） | **1** | baseline，`model_calls = 0` |
| `G1-T` × 30（冷） | **1** | **主数字 / 门槛对象**，`model_calls = 2` |
| `G1-T` × 30（热） | **1** | 缓存一致性，非独立测量，`model_calls = 0` |
| `G1-T` 回放 | 0–1（可选） | 可复算，`model_calls = 0`；`G0`/`G2`/`G3`/MCP = **0**（报告写"未测"） |

### 5.4 模型调用预算（预登记）

`G1` = 0；`G1-T` 冷 = **2**（只有 2 条 `cjk_title` 触发解析器）；热 = 0；回放 = 0；预热 = 1 → **总计 ≤ 4 次模型请求**。任何新增 CJK 片名（尤其越界请求里夹 `《…》`）都会打破该预算 → §3.4 第 5 条已禁止。

---

## 6. 验收条件

| ID | 条件 | 验证 |
| --- | --- | --- |
| `B15f-1` | 两个新 fixture 存在：`C01`–`C30`，10/10/10，intent 5/9/7/3/6，`multi_slot` 4，`cjk_title` 2，拒绝 4 / 澄清 2 | checker（§4.2 组 1–6） |
| `B15f-2` | §3.3 的 `P-1`…`P-8` 全部命中且各恰好 1 处（`P-5`/`P-8` 各 2 条），每条带预声明口径；**每条 similar 的 `labeling_note` 都写明约定 + 谓词 + `rows=`**（缺失即不可冻结）；`P-3` 必须带 **导演其他目录行**的固定 SQL 与 `rows=n`（`5 ≤ n ≤ 25`）、convention = `same director`；`P-4` 必须带"预期失败"预声明 | checker 组 13/14/16 + 探针表 |
| `B15f-3` | 排除：与 **135 条计分用例**（60+15+30+30）文本/种子/可接受集零重叠（既有 id 并集 **718**），**种子片名与既有 29 个不同种子零重叠且本批内部两两不同**，无合成行、无 2020s、1990s 3–5、2010s ≤1；`negative.b1-5.json` 5 条**不计分、不入断言**；**不出现 165** | checker 组 7–8 + 17 |
| `B15f-4` | 冻结：3 个 SHA-256 + 提交/`hash-object`+mtime 记录在案；**首次运行晚于冻结**；标注期 `tool_eval_runs = 0` | `.pi/artifacts/b1-5f-freeze.json` + 报告时间线 |
| `B15f-5` | **主门槛**：`G1-T`（冷）**任务级 ≥ 27/30**（`/30` 全分母）；`G1` 为 baseline；冷跑 `model_calls = 2`、热跑 = 0、逐例一致；**`P-4` 预期失败仍占 3 条容错中的 1 条，报告必须写"只剩 2 条非预期失败空间"**（不允许把 29/30 讲成 30/30） | 报告主表 + run record |
| `B15f-6` | 分母口径：`/30`（主）、`/28（非CJK）`、`/28（去探针）`（+ 可选 `/27`），**成员逐条列名**（`/28（去探针）` = 去 `P-1`/`P-2`；`/27` = 再去 `P-4`）+ Wilson 95% CI 实数；**任何 `/28`/`/27` 不得用于宣称 PASS**；若不报 `/27` 就不得出现该行 | 报告分母表（§7） |
| `B15f-7` | 失败**逐条归因**（规则缺口 / 标注问题 / 预声明探针）；标注问题保留原数字并另记 corrected；**不因归因改规则、不重跑** | 报告失败表 |
| `B15f-8` | 零改动：`tool_eval.py`、别名 ×3、`tool-schema.b1-5.json`/`schema_hash`、`golden-set.schema.json`、既有 `check_*.py`（含 `check_blind_holdout.py` **byte-identical** = `8259cc8d…7caf`）、B1.5a–B1.5e 全部 fixture/计划/报告 hash 不变；唯一新增脚本 = B1.5f checker；B1.5d 25/30 与 held-out B1.5c 28/30 只作**历史引用** | `git diff --stat` + hash 表 + 措辞检查 |
| `B15f-9` | 报告 `docs/ai/reports/b1-5f-new-blind-holdout.md`：复现命令、运行次数、`model` tag + digest、`prompt_sha256`、缓存 SHA、预热与 `keep_alive`、生成者读过文件清单；**必须原句披露**"标注者知道探针形状、blind 仅排除用这 30 条调参"与"n = 30、单人标注、无第二标注人"；**结论边界**：不声称泛化 / 生产可用 / B-① 可开始；`docs/architecture/` 本轮不改 | 报告结构 + 措辞检查 |
| `B15f-10` | demo 页：`demo/iterations/b1-5f/index.html` 存在、`demo/index.html` 指向最新、`scripts/demo/serve.sh` 在 `0.0.0.0:4173` 起服务、`curl` 从局域网 IP 验证；显式声明"演示站不是完整应用"；只出现聚合数字，**不出现 DB 主机/凭据/Ollama 端点/原始 id** | demo 文件 + `curl` 输出 |
| `B15f-11` | **checker 变异自测**：§4.4 至少 **5 个变异被捕获**（实际列出变异→失败断言→退出码），且 `check_blind_holdout.py` 自始至终 byte-identical | 变异自测记录 + `shasum -a 256` |

---

## 7. 分母与统计口径

| 口径 | 成员定义 | 用途 | 允许的表述 |
| --- | --- | --- | --- |
| **`/30`** | 全部 30 条（含 2 条 CJK、含全部预声明探针） | **主门槛**：`G1-T` 冷 ≥ 27/30 | 可宣称 PASS/FAIL |
| `/28（非CJK）` | 30 − 2 条 `cjk_title`（成员逐条列名） | 与 B1.5c/B1.5d 的 `/28` 同名对照 | 仅敏感性，**不宣称 PASS** |
| `/28（去探针）` | 30 − `P-1`（`R-1`）− `P-2`（`R-2`）——与 B1.5d 的"去 2 条已知缺口探针"同成员定义，便于对照 | 量化已知缺口探针的影响 | 仅敏感性，**不宣称 PASS**；须注明与 `/28（非CJK）` 成员不同 |
| `/27（可选，仅在报告确实输出时出现）` | 30 − **`P-1` − `P-2` − `P-4`**（`P-4` = `Q12` 同导演预声明失败）；**这三个成员必须在报告里逐个列名** | 隔离"已知不可满足"用例的影响 | 仅敏感性，**不宣称 PASS**；不得与 B1.5d 的 `/28` 比强弱；**未列成员就不得报 `/27`** |

**容错口径（重要）**：`/30` 门槛下最多允许 3 条失败；**`P-4` 预声明失败占其中 1 条** ⇒ 只有 **2 条**非预期失败空间。因此 `29/30` 是"已用掉 2 条容错"的 PASS，**不是** 30/30。

Wilson 95%（`z = 1.96`，实数）：`27/30 = 90.0% [74.4%, 96.5%]`、`28/30 = 93.3% [78.7%, 98.2%]`、`29/30 = 96.7% [83.3%, 99.4%]`、`30/30 = 100% [88.6%, 100%]`、`26/30 = 86.7% [70.3%, 94.7%]`、`26/28 = 92.9% [77.4%, 98.0%]`、`27/28 = 96.4% [82.3%, 99.4%]`、`28/28 = 100% [87.9%, 100%]`、`26/27 = 96.3% [81.7%, 99.3%]`、`27/27 = 100% [87.5%, 100%]`。**禁止表述**：① "95% 置信下 ≥90%"（`27/30` 下界 ≈ 74.4%）；② 用 `/28`/`/27` 宣称主门槛 PASS；③ 在任何口径上剔除失败条目；④ 把本批 `/28` 与 B1.5c/B1.5d 的 `/28` 比"强弱"（成员不同）。

---

## 8. 依赖、风险与回退

**依赖**：只读 `fabflix-mysql`（3307/`moviedb`）；Ollama `qwen2.5:7b`（1 次预热 + 2 条 CJK 冷跑）；`main` @ `7915fcd`（B1.5e 已合并）；本文（r2）获批 + 新 checker 获批；issue **#81 已创建**（生成/冻结需主 agent 放行）。

| ID | 风险 | 处理 |
| --- | --- | --- |
| `RK-f1` | **门槛不过**（很可能 25–28 区间） | 如实报告；不改规则/用例、不重跑；需修复 → 另立任务 + **另一套**新盲集（B1.5f 随即成为 seen） |
| `RK-f2` | **生成者污染**：读了 B1.5e 修复细节或自己就是修复作者 | `BF-1`/`BF-2` + 报告列实际读过的文件；无法分离 → 降级为 seen 回归 |
| `RK-f3` | **探针吃掉分母**（10/30 是预声明探针）且 `P-4` 必然失败 | 冻结前声明 + 报告单列"预声明失败"；但 `P-4` **仍占 1 条容错** → 只剩 2 条非预期失败空间；`/30` 仍是主门槛，`/27` 只作敏感性 |
| `RK-f4` | **排除不可行**：718 个 id 已被用过，逐 id 不重叠更难 | 先查重；确认不可行 → **交主 agent 裁定**，checker 不提供放水开关（沿用 `D-7`） |
| `RK-f5` | **`P-3` 无可用种子**（找不到同时满足 3+ canonical genres + 已知导演 + 5–25 行其他目录行的种子），或标注仍写成"应收窄/按排序" | 先只读 SQL 枚举；无合适种子 → **暂停交主 agent**，不得退回不可判分的标注；checker 组 16 硬断言集合相等 + top-5 子集 |
| `RK-f5b` | **CJK 预算被打破**（越界请求里夹 `《…》` 或 CJK 片名 → 第 3 次模型调用） | §3.4 第 5 条禁令 + checker 的 "smuggled CJK" 断言 + 冷跑后硬断言 `model_calls = 2` |
| `RK-f6` | **新 checker 与断言脱节**（复制后漏改常量）或 **`P-1` 口径错**（写成 B1.5d 的 24 行 `Music` 口径） | **§4.4 变异自测 ≥ 5 个变异必须被捕获**（错误 ID / 12 行回退 / 缺约定 / `P-4` 无预声明 / `P-3` 任意 5 行 / seed 重复）；组 15 硬断言 union 13 行 + **top-5 ⊆ union** |
| `RK-f7` | **指纹与冻结**：别名/`tool_eval.py` 在 B1.5d 之后已变 + "先跑后冻"风险 | 记录本批全部 hash，与 B1.5d 数字**不并列比较**；§4.3 时序证据；改 fixture/checker 任一 → 重冻并 bump `blind_version` |
| `RK-f8` | **demo 页被当成产品证据 / 泄露内部信息** | 页面只放聚合数字与边界说明；显式写"演示站不是完整应用"；不出现 DB/Ollama/凭据/原始 id |

**回退**：本任务不写生产代码、不改契约；最坏情况是 `git checkout` 两个新 fixture + 新 checker（未提交时直接删除）。已冻结并跑过的批次不撤回，只如实记录。

---

## 9. 决策记录与剩余未决项

### 9.1 已由主 agent 裁定（r2 落实，不再作为 open question）

| ID | 问题 | 裁定结果 |
| --- | --- | --- |
| `OD-f1` | B1.5f checker 形态 | **方案 A**：新增 `check_blind_holdout_b15f.py`（import 只读辅助 + 自实现 17 组）；`check_blind_holdout.py` **byte-identical**；并附 §4.4 **变异自测 ≥ 5 个变异被捕获** |
| `OD-f2` | 排除基线 135 / 165 | **135 条计分**（`queries.b1-5.json` 60 + `tool-cases.b1-5.json` 15 + `queries-heldout.b1-5c.json` 30 + `queries-blind.b1-5d.json` 30）+ **5 条未评分 negatives**；**删除 165**（交办稿笔误） |
| `OD-f3` | `P-3`（3+ 类型种子）的期望语义 | **3+ canonical genres + 已知导演 + 查询无文本约束**；可接受集 = **同导演其他目录行**（固定 SQL + `rows=`）、convention = **`same director`**；③b2 不派生、靠排序命中 |
| `OD-f4` | `P-4` 留在 `/30` 还是剔除；`/27` 是否报 | **留在 `/30`**，但**占用 3 条容错中的 1 条** → 只剩 2 条非预期失败空间；`/27` 可选，**若报则成员 `P-1`/`P-2`/`P-4` 必须逐条列名** |
| `OD-f5` | 预声明字段 | 沿用 `known_gap` 扩值（checker-local，**不改 schema**） |

### 9.2 仍待确认（不阻塞 oracle 快速复核，但阻塞生成）

| ID | 事项 | 处理 |
| --- | --- | --- |
| `OI-f1` | 目录里是否存在满足 `P-3` 种子约束的候选（3+ canonical genres + 已知导演 + **5–25** 行其他目录行） | 生成阶段用只读 SQL 先枚举；无候选 → **暂停交主 agent**，不得退为不可判分的标注 |
| `OI-f2` | `P-2`/`P-6` 的英文种子能否同时满足 `5–25` 行可接受集与既有 29 个种子片名不重叠 | 同样先只读 SQL 枚举；不可行则交主 agent |
| `OI-f3` | `docs/issues/issue-81-b1-5f-new-blind-holdout.md` 与**已发布的 #81 正文**是否需更正 | **已闭合（r3）**：`gh issue view 81 --json body` 实测远端正文已含 r2 的 3 处更正，且与本地正文逐行 diff 为空 → **无需再 `gh issue edit`**；#81 仍 `OPEN`，状态改为**报告完成 / PR 待建** |

---

## 10. 与 B1.5e 的边界（重申）

1. **不生成、不修改、不"顺手修"B1.5e 的任何实现**：`tool_eval.py`、`genre-aliases.json`（`b1-5c-aliases/v2`）、`check_schema.py` 本任务**零改动**；`R-1`/`R-2`/`Q12` 例外是**既有事实**，只测量。B1.5f 只回答一个问题：`main` @ `7915fcd` 的规则在新 30 条上是否 ≥ 27/30。
2. **B1.5f 之后仍不声称**：不声称"泛化 ≥90%"、不声称生产可用（派生收窄只在评测替身里）、不声称 B-① 可开始、不声称 #74 已完成；失败不自动产生新任务，需修复时另立任务并配**又一套**新盲集。
3. **关卡不变**：本文（r2）→ oracle **快速复核**（只验 P1-1/P1-2 落点与新增断言）→ 主 agent 批准 → 独立会话标注 → 冻结 → 单次运行 → 报告 → 三路 reviewer 只看文档/证据一致性。**r2 阶段未生成 fixture、未运行任何命令、未创建 issue**；r3（本回填）只改 `docs/`。

---

## 11. 执行结果回填（r3，2026-11-20）

> 依据：`.pi/artifacts/b1-5f-freeze.json`、`.pi/artifacts/b1-5f-eval/{blind-G1,blind-G1T-cold,blind-G1T-warm,blind-G1T-replay}.json` + `run-timeline.txt`、`shasum -a 256`、`git diff --stat 7915fcd -- scripts/eval docs/ai`（空）。完整证据与逐条归因见报告 `docs/ai/reports/b1-5f-new-blind-holdout.md`。

### 11.1 结论

**FAIL。** `G1-T`（冷跑）在 30 条 `C01`–`C30` 上 **24/30 = 80.0%（Wilson [62.7%, 90.5%]）< 27/30 → 未达主门槛**。B1.5e 的修复**没有泛化**；且本批失败里有 **2 条是 B1.5e `F-2` title guard 过度修正引入的回归**（`C12` `Western Union`、`C16` `War Games`）。

### 11.2 主表（四个变体）

| 变体 / 缓存 | `/30` | `/28` 非CJK（`C01`/`C11`） | `/28` 去 `P-1`/`P-2`（`C22`/`C25`） | `/27` 再去 `P-4`（`C18`） | 路由 | 模型调用 | 判定 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| **`G1-T` 冷** | **24/30 = 80.0% [62.7, 90.5]** | 23/28 = 82.1% [64.4, 92.1] | 22/28 = 78.6% [60.5, 89.8] | 22/27 = 81.5% [63.3, 91.8] | 24/30 | 2 | ❌ **FAIL** |
| `G1-T` 热 | 24/30 | 23/28 | 22/28 | 22/27 | 24/30 | 0 | 逐例一致 |
| `G1-T` 回放 | 24/30 | 23/28 | 22/28 | 22/27 | 24/30 | 0 | 逐例一致 |
| `G1` baseline | 23/30 = 76.7% [59.1, 88.2] | 23/28 = 82.1% [64.4, 92.1] | **21/28 = 75.0% [56.6, 87.3]** | **21/27 = 77.8% [59.2, 89.4]** | 23/30 | 0 | baseline（非门槛） |

`G0`/`G2`/`G3`/MCP = **未测 / 未实现**。三个敏感性口径**均不得宣称 PASS**。

### 11.3 六条失败与归因

| 用例 | 现象 | 归因 |
| --- | --- | --- |
| `C01`（`P-8`） | LLM 候选 `Fly Away High` 被目录拒绝 → 无工具调用、回澄清 | 真实中文译名**幻觉**缺口（`wrong_movie = 0`，行为不钝；`title-aliases.json` 仍空） |
| `C05` | `seed_title = None` → 降级为 `filter_movies{Fantasy, 1993–1995}` | **中文“喜欢”未触发 seed 抽取**（动词触发词只有英文） |
| `C12` | `query = None` → `filter_movies{genres:["Western"]}` | **B1.5e title guard 过度修正（回归）**：首词 `Western` 是类型别名 → 整段被判从句 |
| `C16`（`P-6`） | `seed_title = None` → `filter_movies{genres:["War","Adventure"], 1983–1984}` → 0 行 | **同一个 title guard 回归**（首词 `War`）；`War` 成为约束是“片名没被抽取”的机械后果，**不是**新的类型词泄漏 |
| `C17` | 同 `C05` | **中文“喜欢”未触发 seed 抽取** |
| `C18`（`P-4`） | `action = "clarify"`，`find_similar_movies` 未调用 | **预声明失败，但机制未命中**：seed 抽取先失败，`Q12` 语义**未被真正行使**（不得声称 `Q12` 已被复现） |

### 11.4 探针结果

- **成立**：`P-1`（`C22`，union 13 行，top-5 = `CSr8`/`PaB10`/`RFM12`/**`RMY15`**/`HJS10` ⇒ 5/5 ⊆ union，实际行使了 union 召回）、`P-2`（`C25`，③b2 派生 `{Biography,Drama} × 1980–1989`，`candidate_pool = 9`）、`P-3`（`C04`，3+ 类型不派生，`candidate_pool = 300`，top-5 全 `same_director = 1`）、`P-5`（`C07`/`C28` 越界拒绝，2 次）、`P-7`（`C23` `documentaries → Documentary`）。
- **失败**：`P-4`（`C18`，见上）、`P-6`（`C16`，title guard 回归）、`P-8` 的 1/2（`C01` 幻觉；`C11` 成功，`OW1`）。
- **B1.5e 的两个语义落码（`R-1` union / `R-2` ③b2）在本批确实生效**；B1.5e 的真实回归在 title guard，不在 `R-1`/`R-2`。

### 11.5 与计划的偏离（已批准）

| 项 | 计划（r2） | 实际 | 批准 |
| --- | --- | --- | --- |
| `P-1` 窗口 | 1948–1950，union **25** 行 | **1980–1982，union 13 行**（`Music` 12 + `Musical` 1 = `RMY15`），与 135 基线重叠 0 | 主 agent 裁定 B（生成阶段）；已**回填**到 §3.3 / §4.2 组 15，checker 从 SQL 重算行数、不硬编码 25 |
| 冻结方式 | (a) 提交 或 (b) `hash-object` + mtime | **(b)**（任务禁止 commit）：三 blob SHA + mtime 记入 `.pi/artifacts/b1-5f-freeze.json` | 计划内允许 |

### 11.6 未做 / 仍存在的缺口

`BF-1`/`BF-2`（生成者独立 / 未读修复细节）**无法硬证明**（无“实际读过文件清单”落证）→ 报告已降级措辞；回放**未显式传 `--resolver-cache`**（`model_calls = 0`、缺运行前后 hash 对照）；回放 `base_url` 记为 `localhost`；预热无独立产物；冷启动延迟（`OPEN-b2`）仍未测。详见报告 §8.4。

### 11.7 下一步

1. **B1.5g**：修 `C01`（译名幻觉回退路径）、`C05`/`C17`（中文 seed 触发词）、`C12`/`C16`（title guard 双向判据），**并保持 `Q12` 例外不回退**。
2. **B1.5g 必须配全新盲集 B1.5h**（新前缀/新冻结/跑前不可见）。**严禁**用 B1.5f 失败条目改规则后重跑 `C01`–`C30`。
3. **B1.5f 数字冻结**；任何重跑只能标为 seen 回归。
4. **issue**：#81 = 报告完成 / PR 待建（可在 PR 合并后关闭）；**#74 保持 OPEN**；**B-① 未开始**。


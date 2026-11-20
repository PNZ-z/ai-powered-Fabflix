# B1.5d — Blind holdout 验证 B1.5c 的 `G1-T`（**FAIL：25/30 = 83.3% < 27/30**）

> **文档状态：🔴 实测完成（2026-11-20）。主门槛 = blind 30 条上的 `G1-T`（冷跑）任务级 **25/30 = 83.3%**（Wilson [66.4%, 92.7%]）**< 90%（27/30）→ FAIL**。B1.5c 的 **30/30 没有泛化**到未参与规则开发的样本上。本报告**不修规则、不改阈值/分母/用例、不重跑**；issue **#74 保持 OPEN**，issue **#76 的报告完成（PR 待建，#76 可在 PR 合并后关闭）**。**
>
> | 元数据 | 值 |
> | --- | --- |
> | 文档类型 | 评测报告（实测数字 + 判定；**不改阈值、不改分母、不剔除失败条目、不据失败改规则**） |
> | `last_updated` | 2026-11-20（首版；blind 30 条**首次且唯一**一次运行） |
> | 所属 | issue [#76](https://github.com/PNZ-z/ai-powered-Fabflix/issues/76)（`Refs #74`；#74 **保持 OPEN**）；计划 `docs/plans/b1-5d-blind-holdout.md`（r3 已批准，本次回填结果） |
> | 判定口径 | **任务级成功率**（`score_case`，严格口径：`tools_ok`/`slots_ok`/`schema_ok`/`behavior_ok`/`grounding_ok`/`acceptable_hit` 全部通过才算成功）；**主门槛 = `G1-T`（冷跑）≥ 90%（27/30，`/30` 全分母）**；`G1` = baseline（不含中文解析） |
> | 金标准集 | blind = `docs/ai/golden/queries-blind.b1-5d.json`（30 条，`B01`–`B30`，`b1-5d-blind/v1`）+ `docs/ai/golden/query-routing-blind.b1-5d.json`（`b1-5d-blind-routing/v1`）；二者在首次运行前已冻结（§2） |
> | 数据快照 | Docker `fabflix-mysql` 宿主 3307 / `moviedb`，21,087 部（196–2024），`db_snapshot_hash = 8b16c047…f925`（四个运行产物一致） |
> | 端点 / 模型 | `http://<host>:11434`（Ollama），`qwen2.5:7b` digest `845dbda0ea48ed749caafd9e6037047aa19acfcfd82e704d7ca97d631a0b697e`；**运行前预热 1 次**（不计 case，见 §7.3） |
> | 脚本（零改动） | `scripts/eval/tool_eval.py`（`178d9370…8bb5`，对 `0761845` **空 diff**）；新增的只读 `scripts/eval/check_blind_holdout.py` 只做断言、不参与路由/判分 |
> | 运行产物 | `.pi/artifacts/b1-5d-eval/{blind-G1,blind-G1T-cold,blind-G1T-warm,blind-G1T-replay}.json`（`.pi/` 不入库）；冻结记录 `.pi/artifacts/b1-5d-freeze.json` |
> | 未做 | **`G0` / `G2` 未测**（计划 `D-3`）；**`G3` 未测**、**MCP 未实现**（全程）；**无生产代码 / 无 `pom.xml` / 无 CI / 无 DB 写** |

---

## 0. 结论

1. **判定：FAIL。** `G1-T`（冷跑）在 blind 30 条上 **25/30 = 83.3%**，低于主门槛 **27/30（90%）**，差 **2 条**。
2. **B1.5c 的 30/30 没有泛化。** B1.5c 的 held-out（`H01`–`H30`）在 stage 1c 中可见并**被用于修规则**（H18 改词、H06/H12/H18/H24/H28 路由词、H16 年份消歧），因此那是 **post-fix regression on a seen fixture**；本批 30 条**在规则冻结之后生成、首次运行之前冻结**，得到的 83.3% 才是这批规则在"未被用来调参"的样本上的水平。
3. **本批不修任何规则。** 5 条失败（`B10`/`B22`/`B23`/`B26`/`B27`）逐条归因（§4、§5），其中 **3 条是真实规则/执行语义缺口**（`B10` 越界请求未拒绝、`B26` similar 约束求交产生空集、`B27` `Documentary` 未抽取），**2 条是预声明的已知缺口探针**（`B22` = `R-1`、`B23` = `R-2`）。修它们属于 **B1.5e**，且必须配**另一套新的 blind 集**。
4. **`G1-T` 相对 `G1` 的净增量只有 +2 条**（`B01`、`B14`，均中文译名），而 `G1` baseline 本身是 **23/30 = 76.7%**。因此 25/30 不能被读成"中文解析达到 83%"，也不能被读成"规则大体可用"——它低于门槛。
5. **issue 状态**：#76 = **报告完成（本文件）/ PR 待建 / 可在 PR 合并后关闭**；**#74 保持 OPEN**（B1.5c 未达标、`R-1`/`R-2`/`OPEN-C3` 未裁定）；**B1.5e 待立项**（§8）；**B-① 仍未开始、未建 issue**。

---

## 1. 盲测定义、纪律与限制

### 1.1 可操作定义

这 30 条查询的文本、标注与可接受集合，在**第一次运行 `tool_eval.py` 之前**已完整写成文件并记录 SHA-256（§2）；生成/标注期间**没有运行任何变体**去看结果，因此用例集合不是"按执行结果筛选"的产物。规则、评测代码、别名、契约与阈值在 `main` @ `0761845` 上冻结，本任务**零改动**（§7.2）。

### 1.2 纪律（计划 §2 的执行情况）

| 纪律 | 要求 | 执行情况 |
| --- | --- | --- |
| `BL-1` | 标注期间禁止对新 fixture 运行任何变体（含 `--ids`/`--limit`/`--dry-run`） | ✅ 冻结记录 `tool_eval_runs = 0`；首次运行 `08:52:34Z` **晚于**冻结 `08:47:04Z`（§2.2） |
| `BL-2` | 只允许只读 SQL 核对可接受集合 | ✅ 每条 case 带 `predicate … \| rows=N \| ids=…`；`ReadOnlyMysql` 在连接前拒绝写语句 |
| `BL-3` | 配额与覆盖先定后抽 | ✅ 配额/覆盖写在计划 §3.2–§3.3，先于 fixture 存在 |
| `BL-4` | 冻结 = 三个 SHA-256 + 版本串，冻结后才允许首次运行 | ✅ §2.1 |
| `BL-5` | 每个 (variant × 缓存状态) 只跑一次；看结果后不重跑、不换用例、不改规则 | ✅ §7.3（含 1 次**未执行任何 case** 的错误回放调用，已如实列明，不是测量） |
| `BL-6` | 结果出来后不回头改规则 | ✅ 本任务**未改任何**规则/别名/契约/阈值；失败只归因 |
| `BL-7` | 生成者不读 B1.5c 报告的失败分析（§5/§7） | ⚠️ **部分达成**：生成者**未读** §5/§7 正文，但一次跨文件 `grep` 命中过该报告 5 行（§1.1 变体表、`B15c-15`、`R-3`/`R-8` 风险行、命令模板）。详见 §1.3 |

### 1.3 标注者独立性与可达边界（必读）

- **生成者实际读过的文件**（生成阶段披露，本报告复述）：计划 `docs/plans/b1-5d-blind-holdout.md`、issue #76 正文、`docs/ai/golden-set.schema.json`、`docs/ai/tool-schema.b1-5.json`、`docs/ai/aliases/*.json`、既有 fixture（仅用于排除与格式，105 条）、`scripts/eval/check_heldout.py`（只学断言风格）、`check_schema.py`/`check_golden.py`（复用只读辅助）、`docs/architecture/ai-user-workflows.md` 契约段、只读 MySQL。
- **明确未读**：`scripts/eval/tool_eval.py`（任何形式）、`docs/ai/reports/b1-5c-tool-routing-fixes.md` §5/§7 正文、`.pi/artifacts/b1-5c-eval/*`、B1.5c 的解析缓存文件。
- **可达边界（`D-5` 已裁定接受）**：生成者**知道规则存在与大致形状**（`multi_slot`/`cjk_title`/canonical genre group/种子消歧/别名表为空/路由词表）——blind 排除的是"**用这批样本调过参**"，**不能**排除"标注者知道规则大致覆盖什么"。上表中那次 `grep` 命中是唯一的越界披露点。
- **其他限制**：**单人标注**、**n = 30**（统计力有限，§6.4）、**无第二标注人复核**；相似/推荐执行器与中文解析器都是**评测替身（stand-in）**，不代表生产实现。

### 1.4 预声明的已知缺口探针

30 条中 **2 条**在 fixture 里以 `known_gap` + `labeling_note` **预声明**（不是事后解释），并**计入** `30` 的分母与主门槛：

| 探针 | 用例 | 形状 | 声明口径 |
| --- | --- | --- | --- |
| `R-1` | `B22` | `musicals from 1948 to 1950` | golden 按 **`Music`** 口径（rows=24）标注；同时用 SQL 并列记录 `Musical` 口径（rows=1）与全库 `Music` 619 / `Musical` 139 |
| `R-2` | `B23` | `I really liked Trainspotting, can you find something similar?` | 文本**不声明**任何类型/年代约束；可接受集 = 种子类型 × 种子年代（`{Comedy,Noir}` × 1990s，rows=11） |

---

## 2. 冻结与时间线

### 2.1 冻结对象（首次运行前记录，运行后复核未变）

| 文件 | SHA-256 | 字节 | mtime (UTC) |
| --- | --- | --- | --- |
| `docs/ai/golden/queries-blind.b1-5d.json` | `21522516b57c8d514b233b45a71329c6e7cef335371ce7805e121058de41c694` | 31,705 | 2026-11-20T08:46:32Z |
| `docs/ai/golden/query-routing-blind.b1-5d.json` | `556761b415c2f947e1aa96df75636a78d22f45b411a89dfdeaa96e82a0d43dbe` | 27,109 | 2026-11-20T08:46:32Z |
| `scripts/eval/check_blind_holdout.py` | `8259cc8dd924ce5a51f09dacbfed36a02a18be9e6ad7f161c630dfaacef37caf` | 48,924 | 2026-11-20T08:49:52Z |

- **冻结方式 = (b)（未提交）**：任务禁止 commit/push，因此用 `git hash-object -w` 记录 blob SHA（`9b1a5f5a…f51c` / `757085f0…9d52` / `9ebee94e…c53d`）+ `stat` mtime，写入 `.pi/artifacts/b1-5d-freeze.json`。**checker 不自证自身 hash**（自指不可实现），其 SHA-256 由该外部记录保证。
- 冻结后的三个观察值与冻结值逐一相等（生成阶段 `post_freeze_verification`，`08:50:01Z` 复核）；**运行结束后未再改**这几个文件（`git status` 仍为未跟踪的新增文件，见 §7.2）。
- `db_snapshot_hash` 三处一致：两个 fixture 的 `snapshot.db_snapshot_hash`、冻结记录、四个运行产物的 `db_snapshot.db_snapshot_hash` 均为 `8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925`。

### 2.2 时间线（"首次运行晚于冻结"的证据）

| 时刻 (UTC) | 事件 | 证据 |
| --- | --- | --- |
| 2026-11-20T08:46:32Z | 两个 fixture 落盘（mtime） | `b1-5d-freeze.json` |
| 2026-11-20T08:47:04Z | **冻结记录写入**（`frozen_at_utc`） | `b1-5d-freeze.json` |
| 2026-11-20T08:49:52Z | checker 落盘（mtime），此后未改 | 同上 |
| 2026-11-20T08:50:01Z | 冻结后复核：checker 全量 + `--skip-db` 均 exit 0；三个 SHA 与冻结值一致；`tool_eval_runs = 0` | `post_freeze_verification` |
| **2026-11-20T08:52:34Z** | **首次 `tool_eval.py` 运行开始**（`G1`） | `.pi/artifacts/b1-5d-eval/run-timeline.txt` |
| 2026-11-20T08:52:45Z | 预热 1 次模型请求（`keep_alive: 10m`，不计 case） | 运行阶段记录（§7.3；**无独立产物**，仅时间线/报告记录） |
| 2026-11-20T08:52:52Z | `G1-T` 冷跑开始（缓存 `absent`） | 同上；产物 mtime `08:52:55Z` |
| 2026-11-20T08:53:05Z | `G1-T` 热跑开始（缓存 `loaded`） | 产物 mtime `08:53:07Z` |
| 2026-11-20T08:53:49Z | ❌ **错误的回放调用**（把 run JSON 当 replay cache）：**0 个 case 执行，不是测量** | `run-timeline.txt`（`replay_start`） |
| 2026-11-20T08:54:08Z | 正确的回放（`--replay-resolver .pi/artifacts/b1-5d-eval/resolver-cache.json`） | 产物 mtime `08:54:10Z` |

- **结论：`08:52:34Z`（首次运行）> `08:47:04Z`（冻结）**，且 `tool_eval.py` 对 `0761845` **diff 为空**（`178d9370…8bb5`），既有 `check_schema.py`/`check_golden.py`/`check_heldout.py`、`golden-set.schema.json`（`8a67d834…9c0c`）、三个别名文件、`tool-schema.b1-5.json`（`b1.5-tools/v3` / `712c0e34…474a`）的 SHA-256 与冻结基线逐一相等。
- 既有 checker 与新增 checker 全绿：`check_schema.py --quiet` = 0、`check_golden.py --quiet` = 0（并复算 `db_snapshot_hash`）、`check_heldout.py` = 0、**`check_blind_holdout.py` 14 组断言全过**（结构/ID/track+文本/配额/交叉标记/拒绝+澄清形态/排除规则/年代+评分覆盖/别名表/绑定与冻结 hash/routing 注解/目录引用/探针证据/只读自证）。另有负向自测：内存中制造的违规变异体全部被对应断言捕获（冻结记录记 19 项、生成阶段报告 §4.2 记 22 个变异体；**两个计数未复核统一**，见 §7.6）。

---

## 3. 主表

### 3.1 四个变体 × 四个分母

> 分母成员**显式**列出；`/28` 的两种口径**成员不同**（一个去掉 2 条中文用例，一个去掉 2 条探针），**不可互相替代、不可与 B1.5c 的 `/28` 比强弱**；**只有 `/30` 能承载 PASS/FAIL 结论**。

| 变体 / 缓存状态 | `/30`（主分母） | Wilson 95% | `/28` 去 2 条 CJK（`B01`/`B14`） | Wilson 95% | `/28` 去 2 条探针（`B22`/`B23`） | Wilson 95% | 路由层 | 模型调用 | 缓存状态 | 判定 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **`G1-T` 冷跑**（主门槛对象） | **25/30 = 83.3%** | [66.4%, 92.7%] | 23/28 = 82.1% | [64.4%, 92.1%] | 25/28 = 89.3% | [72.8%, 96.3%] | 26/30 | **2** | `absent`（`.pi/artifacts/b1-5d-eval/resolver-cache.json`） | ❌ **FAIL（< 27/30）** |
| `G1-T` 热跑（同缓存） | 25/30 = 83.3% | [66.4%, 92.7%] | 23/28 = 82.1% | [64.4%, 92.1%] | 25/28 = 89.3% | [72.8%, 96.3%] | 26/30 | **0** | `loaded`；`cache_hits = 2` | 与冷跑**逐例一致**（§3.3） |
| `G1-T` 回放（同一缓存） | 25/30 = 83.3% | [66.4%, 92.7%] | 23/28 = 82.1% | [64.4%, 92.1%] | 25/28 = 89.3% | [72.8%, 96.3%] | 26/30 | **0** | `replay`；`cache_hits = 2` | 与冷跑**逐例一致** |
| **`G1`**（baseline，无中文解析） | **23/30 = 76.7%** | [59.1%, 88.2%] | 23/28 = 82.1% | [64.4%, 92.1%] | 23/28 = 82.1% | [64.4%, 92.1%] | 24/30 | **0** | 不适用（`resolver.enabled = false`） | baseline（**不是**门槛对象） |
| `G0` / `G2` | — | — | — | — | — | — | — | — | — | ⬜ **未测**（计划 `D-3`） |
| `G3` / MCP | — | — | — | — | — | — | — | — | — | ⬜ **未测 / 未实现** |

**必读的三条限制**：

1. **`/30` 是唯一门槛口径。** `25/30 = 83.3%` 的点估计低于 90%，判定 **FAIL**；`/28` 的任何数值（含 89.3%）**都不得**用来宣称主门槛达标——这一点与 B1.5c `R-5` 同一口径。
2. **去探针口径（25/28 = 89.3%）仍然 < 90%**，而且它**不构成**"接近达标"的证据：该口径同时移除了 2 条失败用例，属于**最有利于模型的敏感性分析**，其上限是 `96.3%`、下界 `72.8%`——区间横跨门槛，**不能**反推"再修 1 条就达标"。
3. **`G1` 的两个 `/28` 数值恰好都是 23/28（82.1%）**纯属巧合：`G1` 的 2 条 CJK 用例**都失败**（去掉它们不减少分子），2 条探针**也都失败**（去掉它们同样不减少分子），所以两种口径都等于 23/28。**成员不同、数值相同**，不可混用。

### 3.2 派生口径的算法（可逐例复算）

- **`/28` 去 CJK** = 30 条移除 `B01`（`zh_zh`，《乱世佳人》）与 `B14`（`zh_en`，《异形》）后重算：`G1` 剩余 28 条中成功 23 条（`B01`/`B14` 都失败 → 分子不减）；`G1-T` 剩余 28 条中成功 23 条（`B01`/`B14` 都通过 → 分子由 25 减 2）。
- **`/28` 去探针** = 30 条移除 `B22`（`R-1`）与 `B23`（`R-2`）后重算：`G1` 剩余 28 条中成功 23 条（两条都失败 → 分子不减）；`G1-T` 剩余 28 条中成功 **25** 条（两条都失败 → 移除它们**不减少分子**，分母由 30 变 28）。
- 四个变体的逐例结果见附录 A；Wilson 区间按 `z = 1.96`（双侧 95%）计算，与本报告 §3.1 的数值一一对应。

### 3.3 冷 / 热 / 回放逐例一致性（`B15d-12`）

- 30/30 条 `task_success` **完全相同**；`resolver.chosen_movie_id` **完全相同**（`B01` → `VF44`、`B14` → `RS2`，其余 28 条两者皆为 `null`）。**无缓存一致性失败。**
- 冷跑 2 次模型调用、`cache_hits = 0`；热跑 0 次调用、`cache_hits = 2`；回放 0 次调用、`cache_hits = 2`。三者 `task_success = 25` 一致。
- **B1.5c 的解析缓存文件未被改动**：`.pi/artifacts/b1-5c-resolver-cache.json`（`7f88929a…84a2`）、`.stage1b.json`（`982d5bfa…c7c94`）、`.stage1c.json`（`7f88929a…84a2`）的 mtime 分别为 `2026-11-20T06:03:44Z` / `05:25:06Z` / `05:55:46Z`，**全部早于冻结（08:47:04Z）与所有运行**。⚠️ 回放运行**未显式传 `--resolver-cache`**，其可写缓存路径落回默认的 `.pi/artifacts/b1-5c-resolver-cache.json`；由于两条解析全部命中 replay、无写入发生（mtime 未变可证），但**本批未在运行前后对该文件做 hash 对照快照**（计划 `B15d-11` 的这一项只以 mtime 证据近似满足），列为遗留风险（§7.6）。

### 3.4 分意图（`G1` vs `G1-T` 冷跑）

| 意图 | 用例 | `G1` | `G1-T`（冷） | 变化 |
| --- | --- | --- | --- | --- |
| 拒绝 `refusal` | 4（`B10`/`B19`/`B28`/`B29`） | 3/4 | 3/4 | 0（`B10` 两者皆失败） |
| 澄清 `clarification` | 2（`B20`/`B30`） | 2/2 | 2/2 | 0 |
| 筛选 `filter_catalog` | 9 | 8/9 | 8/9 | 0（`B22` 两者皆失败） |
| 片名 `search_by_title` | 5 | 4/5 | **5/5** | **+1（`B01`）** |
| 偏好推荐 `recommend_by_preference` | 3 | 2/3 | 2/3 | 0（`B27` 两者皆失败） |
| 相似 `similar_movies` | 7 | 4/7 | 5/7 | **+1（`B14`）** |
| **合计** | **30** | **23/30** | **25/30** | **+2** |

- **净增量 +2，两条都是中文译名用例**（`B01` 乱世佳人→Gone with the Wind、`B14` 异形→Alien）。`G1` 无解析层，这两条必然失败，所以 `G1` 的 `/30` 上限就是 28/30。
- **没有任何用例在 `G1-T` 上回退**：`G1-T` 的 5 条失败（`B10`/`B22`/`B23`/`B26`/`B27`）是 `G1` 的 7 条失败的**真子集**。

### 3.5 解析（title resolution）指标 — `G1-T` 冷跑（n = 2，逐例披露，**不是**中文解析成功率估计）

| 指标 | 值 |
| --- | --- |
| `triggered` | 2（`B01`、`B14`；预热后冷启动延迟**未测**） |
| `parse_success` | 2/2（`llm_candidates`：`Gone with the Wind` / `Alien`） |
| `catalog_verified` | 2/2（`match = exact`；`VF44` / `RS2`） |
| `wrong_movie` | **0/2**（`wrong_movie_cases = []`） |
| `candidate_reject_rate` | 0.0（无候选被拒） |
| `fallback_to_clarify` | 0（`fallback_rate = 0.0`） |
| `alias_hits` | **0**（`title-aliases.json` 仍 `entries: []`，全部走 LLM + 目录校验路径） |
| `timeouts` | 0（`timeout_s = 8.0`） |
| latency | p50 **244.2 ms**（`B14`）、p95 **363.2 ms**（`B01`）——**仅 2 个样本**，p95 即最大值 |
| 请求指纹 | `request_sha256`：`B01` = `c7898bd3…8fee`、`B14` = `5075362d…6de3`；`prompt_sha256`（resolver）= `8d6d9e4f91259f025c7fa1e996a5ceb38ec7552100ba7ede4aacb31b305bf041`，`prompt_id = b1-5c-title-resolver/v1` |

---

## 4. 失败逐条（5 条 + `G1` 额外的 2 条）

> 归因口径：**真实规则/执行缺口** / **已知缺口 `R-1`/`R-2`** / **标注问题**。本批**没有**发现标注错误；**没有任何规则因这些失败被修改**。

### 4.1 `G1-T`（冷）失败的 5 条

| # | 用例 | 查询 | 预期（golden） | 实际 | 失败模式 | 归因 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | **`B10`** | `《The Lion King》当年的票房是多少` | **拒绝**：`expected_refusal: true`、`expected_tools: []`、可接受集空 | 调用了 `search_movies_by_title(query="The Lion King")`，返回 1 行（`RgA10`，1994） | `wrong_tool`、`under_refusal` | **真实规则缺口**（越界请求未拒绝） |
| 2 | **`B22`** | `musicals from 1948 to 1950` | `filter_movies{genres:[Music], 1948–1950}`，rows = **24** | `filter_movies{genres:["Musical"], 1948–1950}`，rows = **1**（`ChW4`） | `slot_mismatch`、`acceptable_set_miss` | **`R-1`（预声明已知缺口）** |
| 3 | **`B23`** | `I really liked Trainspotting, can you find something similar?` | `search_movies_by_title` + `find_similar_movies`；可接受集 = 种子类型 × 种子年代（`{Comedy,Noir}` × 1990s，rows = 11） | 路由把 `seed_title` 抽成整句 `I really liked Trainspotting` → 检索 0 行 → 回 `which one do you mean?`，`EMPTY_RESULT` | `incomplete_tool_sequence`、`slot_mismatch`、`acceptable_set_miss`、`empty_result` | **`R-2`（预声明）+ slot extraction 缺陷（叠加）** |
| 4 | **`B26`** | `movies like The Addams Family - horror films from 1991 to 1992` | 种子 `BS1` + `find_similar_movies`；可接受集 = `year 1991–1992 ∧ Horror ∧ ¬BS1`，rows = **22** | 路由正确（`seed_title` 抽取正确、`tools` 序列完整），但执行期把**种子自身的类型**（`{Family, Horror}`）与文本约束一并作为 AND 条件 → `candidate_pool = 0` → `EMPTY_RESULT` | `acceptable_set_miss`、`empty_result` | **真实 similar 执行语义缺口**（**不是** `R-1`/`R-2`：本例文本**有**显式类型+年代约束） |
| 5 | **`B27`** | `recommend some documentaries from the 1960s` | `recommend_movies{preferred_genres:["Documentary"], year_range 1960–1969}`，rows = 25 | `recommend_movies{year_range 1960–1969}`——`preferred_genres = None` | `slot_mismatch`（`preferred_genres: None != ['Documentary']`） | **真实 genre 词汇/槽位缺口**（`documentaries` 未映射到 canonical `Documentary`） |

**逐条补充（必须与上表同读）**：

- **`B10`**：越界项是**票房（box office）**——不在目录字段（`title/year/director/genres/stars/rating`）内，属于必须拒绝且**不得调工具**的请求。实际行为是"照常检索片名"，因此即使用户问题的一半（片名）能被满足，整体仍是 `under_refusal`。这与 B1.5a/B1.5b 的拒绝类缺口同源：拒绝规则依赖关键词表，而"票房/box office"未在表内。该例在 `G1` 与 `G1-T` 上**行为完全相同**（失败与解析层无关）。
- **`B22`（`R-1`）**：`genre-aliases.json` 把 `musical → Musical`，而 B1.5a/Q48 沿用的标注口径是 `Music`。本批 SQL 证据并列记录：`{Music} ∧ 1948–1950` = **24 行**、`{Musical} ∧ 1948–1950` = **1 行**、全库 `Music` = **619** / `Musical` = **139**。**本例失败不证明"泛化不足"**，它量化的是 `R-1` 未裁定的代价（§5.1）。
- **`B23`（`R-2`）**：`R-2` 的原始形态是"文本无约束 → 执行流无法从种子元数据派生收窄"。但本例**叠加了更早的失败**：`seed_title` 抽取把载体短语（`I really liked … can you find something similar?`）一起当成片名，检索直接 0 行，`find_similar_movies` 根本没被调用。因此**本例不能单独量化 `R-2`**——`R-2` 在本批仍属**未被干净测量**（§5.2 如实记录）。
- **`B26`**：这是本批**信息量最大**的一条。路由层完全正确（`routing_success = true`、`tools_ok = true`、`slots_ok = true`、`schema_ok = true`），失败发生在执行期把 `result_constraints`（文本显式约束 `Horror` + `1991–1992`）与种子自身类型（`Family`, `Horror`）**同时**当作 AND 条件，交集为空。预期 22 行、实际 0 行——属于**执行语义**问题，与"文本无约束"（`R-2`）或"类型拆分"（`R-1`）都不同。
- **`B27`**：`recommend_movies` 只带上了年代窗口，漏掉 `preferred_genres`。**注意**：该例 `acceptable_hit = true`（返回的 5 行里碰巧包含可接受集中的 `MAp7`），但严格口径下 `slots_ok = false` 即任务失败——**"检索碰巧命中"不等于任务成功**，本报告不把它当作通过，也不把它当作纯检索缺陷。

### 4.2 `G1` 相对 `G1-T` 多出的 2 条失败（均 = 中文译名）

| # | 用例 | 查询 | `G1` 实际 | `G1-T` 实际 | 归因 |
| --- | --- | --- | --- | --- | --- |
| 6 | **`B01`** | `帮我找一下《乱世佳人》这部电影` | `search_movies_by_title(query="乱世佳人")` → 0 行，`EMPTY_RESULT`；`slot_mismatch`（`query: '乱世佳人' != 'Gone with the Wind'`） | ✅ 解析器产出 `Gone with the Wind`（目录校验 `exact`，`VF44`），1 行命中 | **设计内**：`G1` 无解析层（`resolver.enabled = false`），中文译名必然失败 |
| 7 | **`B14`** | `跟《异形》类似的科幻片，1979 年的` | `seed_title = '异形'` → 0 行；工具序列不完整 | ✅ `异形` → `Alien`（`RS2`），`find_similar_movies` 命中 5 行 | 同上 |

- `G1` 的 7 条失败 = `G1-T` 的 5 条 + `B01` + `B14`；`G1` 与 `G1-T` 的失败集合是**严格包含**关系，说明 `G1-T` 没有引入新失败。
- **`G1` 的 `routing_success = 24/30`**（失败的 6 条中 `B26` 路由正确），而 `G1-T` 是 26/30（`B01`/`B14` 路由也随解析成功）。

### 4.3 SQL / 预期行数（与失败条目相关）

| 用例 | 谓词（只读 SQL 复核） | rows |
| --- | --- | --- |
| `B22` | `year 1948–1950 AND genres ⊇ {Music}`（标注口径） | 24 |
| `B22` | `year 1948–1950 AND genres ⊇ {Musical}`（规则实际口径） | 1 |
| `B23` | `year 1990–1999 AND genres ⊇ {Comedy,Noir} AND id NOT IN (DBo10)` | 11 |
| `B26` | `year 1991–1992 AND genres ⊇ {Horror} AND id NOT IN (BS1)` | 22（实际 0） |
| `B27` | `year 1960–1969 AND genres ⊇ {Documentary}` | 25（实际 5，且未带类型） |
| `B10` | 无目录谓词——越界请求（拒绝，不调工具） | 0 |

---

## 5. 已知缺口探针（预声明，`B15d-13`）

### 5.1 `R-1`（`B22`）：两种解释并列

| 解释 | 谓词 | rows | 本例命中 | 定位 |
| --- | --- | --- | --- | --- |
| **(a) `Music`（本批 golden 口径，沿用 B1.5a/Q48）** | `year 1948–1950 ∧ genres ⊇ {Music}` | **24** | ✅（若按此口径则本例通过） | 与 `genre-aliases.json` 的 `musical → Musical` 冲突 |
| **(b) `Musical`（规则当前实际产出）** | `year 1948–1950 ∧ genres ⊇ {Musical}` | **1**（`ChW4`） | ❌ | 别名表的映射目标 |
| 全库规模对照 | `genres ⊇ {Music}` / `genres ⊇ {Musical}` | **619 / 139** | — | 两种值是**不同的目录类型**，不是同义写法 |

- **结论：`R-1` 仍未裁定**（B1.5c §5.4 的 (a) 改标注 / (b) 改别名 / (c) 记为已知缺口 三个选项都没有被批准）。本批**没有**改别名、**没有**改标注、**没有**在报告里宣布哪种解释正确。
- **可声称的**："若采用 `Music` 口径，`B22` 会通过，`G1-T` 变成 26/30 = 86.7%（Wilson [70.3%, 94.7%]）—— **仍低于 27/30**。" 这一句是**敏感性说明**，**不是**主门槛结论，也**不能**写成"只差 1 条"。
- **不可声称的**：任何"规则已正确"或"标注有错"的判断；`R-1` 的裁定权在主 agent / 用户。

### 5.2 `R-2`（`B23`）：本批**未能干净测量**

| 项 | 事实 |
| --- | --- |
| 预声明形态 | 文本无任何类型/年代约束；可接受集 = 种子类型（`Comedy,Noir`）× 种子年代（1990s），rows = 11 |
| 设计意图 | 检验"执行流不得读标注"时，能否从种子元数据派生收窄（B1.5c `R-2`） |
| 实际发生 | `seed_title` 被抽成整句 → 种子检索 0 行 → **`find_similar_movies` 未被调用** → 4 个失败模式叠加 |
| 因此 | 本例**同时**暴露 `R-2`（文本无约束）**与** slot extraction（片名抽取）两个缺口；`R-2` 的"约束派生"代价在本批**被更早的失败掩盖**，**仍是未测量项** |
| 不做的处置 | 不改用例（改写会作废盲测）、不因为"它同时暴露了抽取缺陷"就重跑、不把它读成 `R-2` 已量化 |

- `B23` 的 `known_gap` 归因保持不变（预声明为 `R-2`），但报告的结论措辞限定为：**"`R-2` 在本批未被干净测量"**，而不是"`R-2` 已确认失败/已确认代价"。

---

## 6. 解释：为什么 25/30 是真实信号，以及它**不能**说明什么

### 6.1 为什么把 25/30 当作真实信号

- **时序**：冻结 `08:47:04Z` 严格早于首次运行 `08:52:34Z`（§2.2），且 `tool_eval.py` 对 `0761845` **空 diff**、既有 checker/别名/契约/阈值 hash 逐一不变——被测对象在本批运行期间**没有被动过**。
- **样本独立性**：30 条在规则冻结之后生成；查询文本、种子片名、可接受集合与既有 105 条**逐条不重叠**（232 个可接受 id 与既有 486 个 id 交集为 0），无合成行、无 2020s 用例。
- **一次一组合**：`G1`、`G1-T` 冷、`G1-T` 热、回放各 1 次，**看结果后没有重跑、没有换用例、没有改规则**（唯一一次额外调用是 08:53:49Z 的错误回放，**0 个 case 执行**，不产生测量）。
- **失败模式内部一致**：`G1-T` 的 5 条失败是 `G1` 7 条失败的子集；`G1-T` 只把 2 条中文用例从失败变成功，其余 28 条判定与 `G1` 完全一致——这排除了"解析层引入新回归"这一解释。
- **失败可复核**：5 条失败各有 SQL 谓词 + 行数 + 逐例记录（§4.3、附录 A），且都不依赖时序/网络/缓存（回放与热跑给出完全相同结果）。

### 6.2 为什么 B1.5c 的 30/30 不能与本批并列

- B1.5c 的 held-out 30 条**在 stage 1c 可见并被用于修规则**：`H18` 查询文本由"悬疑片"改为"惊悚片"、`H06`/`H12`/`H18`/`H24`/`H28` 的路由词与 `H16` 的年份窗口消歧都在**看过该 fixture 之后**补的。因此那是 **post-fix regression on a seen fixture**。
- 本批 30 条**没有**参与任何规则开发，且**换掉了 B1.5c 未覆盖的形状**（无文本约束的相似查询、`musicals`、`documentaries`、越界请求、`Woody~Allen` 脏导演串、同名种子、零类型/无评分边界）。
- **两者的差异（30/30 vs 25/30）至少部分来自样本选择**，这就是本任务存在的理由；**不能**把它读成"规则退步"（本批期间零改动）。

### 6.3 `G1-T` 相对 `G1` 的净增量

- **+2 条，全部是中文译名用例（n = 2）**。`G1` baseline 在本批已经是 23/30 = 76.7%，而 `G1-T` 25/30 = 83.3%——**两条路线都没过 90%**。
- 因此本批的结论只能是"**`G1-T` 未达标**"，**不能**是"`G1-T` 相对 `G1` 有价值"或"中文解析达标 83%"。n = 2 的两个成功样本**不足以**估计中文解析能力（与 B1.5c §7.1 第 3 条同口径）。

### 6.4 统计力（n = 30）

| 观测 | 点估计 | Wilson 95% | 与 90% 门槛的关系 |
| --- | --- | --- | --- |
| 25/30（本批主数字） | 83.3% | [66.4%, 92.7%] | **点估计 < 90% → FAIL**；区间上界 > 90%，故不能反推"真实能力远低于 90%" |
| 27/30（门槛） | 90.0% | [74.4%, 96.5%] | 即便达标，区间下界也只有 74.4% |
| 25/28（去探针） | 89.3% | [72.8%, 96.3%] | 敏感性口径，**不可宣称 PASS** |
| 30/30（B1.5c seen fixture） | 100% | [88.6%, 100.0%] | 区间下界仍 < 90% |

- **禁止表述**："统计证明 ≥90%"（任何口径都做不到）；同样**禁止**把"25/30 的区间上界 92.7% > 90%"读成"实际可能达标"——门槛判定按**点估计**，不是按区间覆盖。
- **禁止**因为"只差 2 条"而降低门槛、扩大分母或剔除失败条目。

### 6.5 本批**不能**说明的内容

- 不能说明中文解析器的生产能力（n = 2，且为**评测替身**）；不能说明相似/推荐的生产质量（执行器是**stand-in**，`B26` 的失败正是 stand-in 语义缺口）；不能说明 `G0`/`G2`/`G3` 的表现（**未测**）；不能说明 MCP（**未实现**）；不能说明 B-① 的任何交付状态（**未开始**）。

---

## 7. 范围、复现与运行记录

### 7.1 实际执行的命令（与计划 §5.1 同形）

```bash
# 0) 冻结与自检（运行前）
shasum -a 256 docs/ai/golden/queries-blind.b1-5d.json \
              docs/ai/golden/query-routing-blind.b1-5d.json \
              scripts/eval/check_blind_holdout.py
python3 scripts/eval/check_schema.py --quiet          # exit 0（既有，未改）
python3 scripts/eval/check_golden.py --quiet          # exit 0（既有，未改；复算 db_snapshot_hash）
python3 scripts/eval/check_heldout.py                 # exit 0（B1.5c 证据完好）
python3 scripts/eval/check_blind_holdout.py           # exit 0（14 组断言）

# 1) G1 baseline（0 模型调用）
python3 scripts/eval/tool_eval.py --variant G1 --cases queries \
  --queries-path docs/ai/golden/queries-blind.b1-5d.json \
  --routing-path docs/ai/golden/query-routing-blind.b1-5d.json \
  --allow-full-run --assert-no-annotation-read \
  --out .pi/artifacts/b1-5d-eval/blind-G1.json

# 2) 预热（1 次模型请求，keep_alive=10m，不计入任何 case）
curl -sS http://<host>:11434/api/generate \
  -d '{"model":"qwen2.5:7b","prompt":"ping","stream":false,"keep_alive":"10m"}' >/dev/null

# 3) G1-T 冷跑（主数字）：独立缓存路径，跑前清空
mkdir -p .pi/artifacts/b1-5d-eval
rm -f .pi/artifacts/b1-5d-eval/resolver-cache.json
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path docs/ai/golden/queries-blind.b1-5d.json \
  --routing-path docs/ai/golden/query-routing-blind.b1-5d.json \
  --resolver-cache .pi/artifacts/b1-5d-eval/resolver-cache.json \
  --base-url http://<host>:11434 --allow-full-run --assert-no-annotation-read \
  --out .pi/artifacts/b1-5d-eval/blind-G1T-cold.json

# 4) G1-T 热跑（同路径同变体；缓存一致性，非独立测量）
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path docs/ai/golden/queries-blind.b1-5d.json \
  --routing-path docs/ai/golden/query-routing-blind.b1-5d.json \
  --resolver-cache .pi/artifacts/b1-5d-eval/resolver-cache.json \
  --base-url http://<host>:11434 --allow-full-run --assert-no-annotation-read \
  --out .pi/artifacts/b1-5d-eval/blind-G1T-warm.json

# 5) 回放（0 模型调用；第一次调用参数错误 → 0 case 执行，见 §7.3）
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path docs/ai/golden/queries-blind.b1-5d.json \
  --routing-path docs/ai/golden/query-routing-blind.b1-5d.json \
  --replay-resolver .pi/artifacts/b1-5d-eval/resolver-cache.json \
  --allow-full-run --assert-no-annotation-read \
  --out .pi/artifacts/b1-5d-eval/blind-G1T-replay.json
```

- 端点按 `docs/ai/README.md` §3 写作 `<host>:11434`。所有 `tool_eval.py` 运行都带 `--assert-no-annotation-read`，四个产物 `annotation_guard = true`。

### 7.2 零改动证明

| 项 | 证据 |
| --- | --- |
| `scripts/eval/tool_eval.py` | `178d9370d1ec3db8a529a781d614b633177ee4950af5f4cc475cfa9a34f08bb5`；对 `0761845` **`git diff` 为空** |
| 既有 checker ×3 | `check_schema.py` `8edd56d8…1821`、`check_golden.py` `0ab8a017…0708`、`check_heldout.py` `d436f5ca…ddd4` —— 与冻结基线逐一相等，且运行 exit 0 |
| `docs/ai/golden-set.schema.json` | `8a67d834c576e4be8c4fe9e159a33f7bba1425d51458592c40d4693bea7c9c0c`（**未改**，`$defs` 的 `H*` 模式不变） |
| 别名 ×3 | genre `f7126f02…c203`、person `06316859…dd98`、title `98bfca9b…cbe4`（仍 `entries: []`，实测 `alias_hits = 0`） |
| 契约 | `tool-schema.b1-5.json` `0ba15269…1169`，`contract_version b1.5-tools/v3`，`schema_hash 712c0e34…474a` |
| 唯一新增脚本 | `scripts/eval/check_blind_holdout.py`（只读；不 import `tool_eval`、不被其调用、AST 自证无写操作） |
| 生产面 | **无** `src/main/**`、**无** `pom.xml`、**无** CI、**无** MCP、**无** G3、**无** DB 写入（只读 `SELECT`）；`git status` 仅 5 个未跟踪的新增文件（2 fixture + 1 checker + 计划 + issue 文档）+ 无关的 `.idea/` |

### 7.3 运行次数（预登记，事后未增加；含错误调用）

| 组合 | 次数 | `model_calls` | 性质 |
| --- | --- | --- | --- |
| 预热（模型请求） | **1** | 不计 case | `keep_alive: 10m`；`08:52:45Z`；**冷启动延迟仍未测**（`OPEN-b2`） |
| `G1` × blind 30 | **1** | **0**（逐条 0） | baseline，非门槛对象 |
| `G1-T` × blind 30（冷） | **1** | **2**（`B01`/`B14`） | **主数字 / 门槛对象**；`cache_state_at_load = "absent"` |
| `G1-T` × blind 30（热） | **1** | **0** | 缓存一致性（`loaded`，`cache_hits = 2`） |
| `G1-T` 回放（正确） | **1** | **0** | 可复算（`replay`，`cache_hits = 2`） |
| ❌ 回放（参数错误的第 1 次尝试，`08:53:49Z`） | 1 | **未产生任何 case 执行** | 把 run JSON 当成 replay cache；**0 cases executed → 不是测量**，不构成"重跑"；产物未保留（仅 `run-timeline.txt` 记录） |
| `G0` / `G2` / `G3` / MCP | **0** | — | 未测 / 未实现（`D-3`） |

- **模型总请求数 = 1（预热）+ 2（冷跑）= 3**，与计划 §5.4 的预算（≤4）一致；热跑与回放为 0。
- 运行指纹：`model = qwen2.5:7b`、`model_digest = 845dbda0ea48ed749caafd9e6037047aa19acfcfd82e704d7ca97d631a0b697e`、`temperature = 0`、`prompt_id = b1-5b-inline/v1`、`prompt_sha256 = 58c8b708d919920cf85becce7594bfc4ffa7795599bc923151e86ba9e10e2310`；解析器 `prompt_id = b1-5c-title-resolver/v1`、`prompt_sha256 = 8d6d9e4f…b041`。
- 缓存指纹：`.pi/artifacts/b1-5d-eval/resolver-cache.json` 的 SHA-256 = `d74d818ce560af9a159f25fc72bfe439d20241090814575f4c0c5bd186f817a3`（1,761 B）；B1.5c 的三个缓存文件 hash 见 §3.3（**未变**）。

### 7.4 复现时的注意事项

- 冷跑**必须**在空的独立缓存路径上跑（`rm -f` + `cache_state_at_load = "absent"`），否则会把热跑数字当成主数字。
- **不要**重跑本批任何组合：一旦重跑并据此改规则，本批即作废（`BL-5`/`BL-6`）。修复走 B1.5e + **新** blind 集。
- 结果依赖快照 `8b16c047…f925`；快照变化则**不可比**。

### 7.5 G0/G2/G3/MCP 的显式状态

`G0`、`G2` = **未测**（计划 `D-3` 决定不跑，本报告不以未测项推断任何结论）；`G3` = **未测**；MCP = **未实现**。

### 7.6 本批遗留的**验证缺口**（如实列出，不粉饰）

| # | 项 | 影响 |
| --- | --- | --- |
| 1 | 回放运行**未显式传 `--resolver-cache`**，可写缓存路径落回默认的 B1.5c 缓存文件 | 该文件 mtime（`06:03:44Z`）早于所有运行、hash 与 `.stage1c.json` 相同，可证**未写入**；但**缺运行前后的 hash 快照对照**，`B15d-11` 的这一项只以 mtime 近似满足 |
| 2 | 回放的 `base_url` 记录为 `localhost`（冷/热为局域网 `<host>`） | 回放 `model_calls = 0`，对结果无影响；但端点不一致属卫生问题，已记录 |
| 3 | 预热请求**无独立产物** | 只能引用运行时间线/记录；`keep_alive` 值（10m）与次数（1）来自运行阶段记录 |
| 4 | 错误回放调用**无产物保留** | "0 case 执行"依据运行时间线 + 运行阶段记录，无法从产物二次核实 |
| 5 | checker 负向自测计数两处不一致（冻结记录 19 / 生成阶段报告 22） | 不影响 14 组正向断言全过；如需引用，须先统一口径 |
| 6 | 生成者的 `grep` 曾命中 B1.5c 报告 5 行（§1.3） | 盲测独立性披露项，**已如实写入**，不改变本批可用性判断 |

---

## 8. 下一步（建议，需主 agent / 用户裁定）

1. **B1.5e（修复 + 新盲集）**：修 `B10`（越界请求需拒绝、不调工具）、`B26`（similar 约束求交语义：显式约束与种子类型应取交还是以显式约束为准）、`B27`（`documentaries` 等描述式/复数类型词 → canonical genre），并**先裁定** `R-1`（`Musical`/`Music`）与 `R-2`（是否允许从种子元数据派生收窄）以及 `B23` 的 `seed_title` 抽取规则。
2. **B1.5e 必须使用一套新的 blind 集**（新 ID、新冻结、跑前不可见）。**严禁**用 B1.5d 的失败条目改规则后重跑**同一集**——那会把本批变成 seen fixture，等于重复 B1.5c 的问题。
3. **`R-1`/`R-2` 需先裁定再落码**：`R-1` 是"改冻结标注"（bump 版本）还是"改别名"还是"记为已知缺口"；`R-2` 是语义决策（执行流是否允许读种子元数据派生收窄），不应由实现顺手决定。
4. **issue 状态**：#76 = 报告完成（本文件），**PR 待建**，**#76 可在 PR 合并后关闭**；**#74 保持 OPEN**（B1.5c 未达标、`R-1`/`R-2`/`OPEN-C3` 未裁定）；B1.5d 的报告**不关闭** #74。
5. **B-① 未开始**：本报告不构成 B-① 的准入证据；执行器与解析器仍是 stand-in，`G0`/`G2`/`G3`/MCP 缺失。

---

## 附录 A — 30 条清单与逐例结果（✅ = 任务级成功）

| id | track | intent | query | 可接受数 | known_gap | `G1` | `G1-T`（冷） |
| --- | --- | --- | --- | --- | --- | --- | --- |
| B01 | zh_zh | search_by_title | 帮我找一下《乱世佳人》这部电影 | 1 |  | ❌ | ✅ |
| B02 | zh_zh | filter_catalog | 90 年代的家庭片有哪些 | 6 |  | ✅ | ✅ |
| B03 | zh_zh | filter_catalog | 1944 到 1948 年的黑色电影有哪些 | 14 |  | ✅ | ✅ |
| B04 | zh_zh | similar_movies | 像《House on Haunted Hill》那样的恐怖片，1997 到 1999 年的 | 21 |  | ✅ | ✅ |
| B05 | zh_zh | similar_movies | 喜欢《Cabaret》，想看类似的音乐片，1972 到 1974 年的 | 4 |  | ✅ | ✅ |
| B06 | zh_zh | recommend_by_preference | 推荐几部 B.Wilder 在 1960 年代拍的喜剧片 | 4 |  | ✅ | ✅ |
| B07 | zh_zh | search_by_title | 帮我找《Lawrence of Arabia》这部片 | 1 |  | ✅ | ✅ |
| B08 | zh_zh | filter_catalog | 1950 年代的悬疑片有哪些 | 4 |  | ✅ | ✅ |
| B09 | zh_zh | similar_movies | 像《Solaris》那样的电影，还想看 Tarkovsky 的其他片子 | 6 |  | ✅ | ✅ |
| B10 | zh_zh | clarify_or_refuse（拒绝） | 《The Lion King》当年的票房是多少 | 0 |  | ❌ | ❌ |
| B11 | zh_en | search_by_title | 帮我找 2000 年那部 American Psycho | 1 |  | ✅ | ✅ |
| B12 | zh_en | search_by_title | 查一下 The Graduate 这部电影 | 1 |  | ✅ | ✅ |
| B13 | zh_en | filter_catalog | 2002 到 2004 年评分 7 分以上的体育片 | 14 |  | ✅ | ✅ |
| B14 | zh_en | similar_movies | 跟《异形》类似的科幻片，1979 年的 | 5 |  | ❌ | ✅ |
| B15 | zh_en | filter_catalog | 1993 到 1994 年的动画片 | 9 |  | ✅ | ✅ |
| B16 | zh_en | filter_catalog | 1940 年代的喜剧悬疑片 | 19 |  | ✅ | ✅ |
| B17 | zh_en | similar_movies | 像《Goldfinger》那样的冒险片，1963 到 1965 年的 | 11 |  | ✅ | ✅ |
| B18 | zh_en | recommend_by_preference | 推荐几部 Woody Allen 在 1970 年代执导的喜剧片 | 4 |  | ✅ | ✅ |
| B19 | zh_en | clarify_or_refuse（拒绝） | The Silence of the Lambs 拿过几座奥斯卡 | 0 |  | ✅ | ✅ |
| B20 | zh_en | clarify_or_refuse（澄清） | 帮我找那部叫 The Thomas Crown Affair 的电影 | 2 |  | ✅ | ✅ |
| B21 | en_en | search_by_title | Can you find the movie Singin' in the Rain? | 1 |  | ✅ | ✅ |
| B22 | en_en | filter_catalog | musicals from 1948 to 1950 | 24 | **R-1** | ❌ | ❌ |
| B23 | en_en | similar_movies | I really liked Trainspotting, can you find something similar? | 11 | **R-2** | ❌ | ❌ |
| B24 | en_en | filter_catalog | crime movies from 2010 to 2019 rated 6 or higher | 6 |  | ✅ | ✅ |
| B25 | en_en | filter_catalog | science fiction movies from 1955 to 1957 | 14 |  | ✅ | ✅ |
| B26 | en_en | similar_movies | movies like The Addams Family - horror films from 1991 to 1992 | 22 |  | ❌ | ❌ |
| B27 | en_en | recommend_by_preference | recommend some documentaries from the 1960s | 25 |  | ❌ | ❌ |
| B28 | en_en | clarify_or_refuse（拒绝） | Which studio released Jaws? | 0 |  | ✅ | ✅ |
| B29 | en_en | clarify_or_refuse（拒绝） | Who composed the score for Vertigo? | 0 |  | ✅ | ✅ |
| B30 | en_en | clarify_or_refuse（澄清） | Is there a movie called Anastasia? | 2 |  | ✅ | ✅ |

**结构复核**：track 10/10/10；intent `search_by_title` 5 / `filter_catalog` 9 / `similar_movies` 7 / `recommend_by_preference` 3 / `clarify_or_refuse` 6（拒绝 4 + 澄清 2）；`multi_slot` = B04/B13/B17/B24；`cjk_title` = B01/B14；可接受 id 合计 232、无重复；无 2020s 用例；1990s 5 条；`G1-T` 冷/热/回放逐例结果完全相同。

## 附录 B — 冻结哈希与运行产物

| 项 | 值 |
| --- | --- |
| `queries-blind.b1-5d.json` | `21522516b57c8d514b233b45a71329c6e7cef335371ce7805e121058de41c694` |
| `query-routing-blind.b1-5d.json` | `556761b415c2f947e1aa96df75636a78d22f45b411a89dfdeaa96e82a0d43dbe` |
| `check_blind_holdout.py` | `8259cc8dd924ce5a51f09dacbfed36a02a18be9e6ad7f161c630dfaacef37caf` |
| `db_snapshot_hash` | `8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925` |
| `b1-5d-eval/resolver-cache.json` | `d74d818ce560af9a159f25fc72bfe439d20241090814575f4c0c5bd186f817a3`（1,761 B） |
| B1.5c 缓存（未变） | `b1-5c-resolver-cache.json` `7f88929a…84a2`、`.stage1b.json` `982d5bfa…c7c94`、`.stage1c.json` `7f88929a…84a2` |
| 运行产物 | `blind-G1.json`、`blind-G1T-cold.json`、`blind-G1T-warm.json`、`blind-G1T-replay.json`（均带 `annotation_guard = true`、`db_snapshot` 四份一致） |
| 时间线 | `run-timeline.txt`：`first_run_start=08:52:34Z`、`cold_start=08:52:52Z`、`warm_start=08:53:05Z`、`replay_start=08:53:49Z`（错误调用）、`replay2_start=08:54:08Z`（正确回放） |

## 附录 C — 验收条件对照（计划 §6）

| ID | 条件 | 状态 | 证据 |
| --- | --- | --- | --- |
| `B15d-1` | 30 条、10/10/10、配额与覆盖满足 | ✅ | checker 组 1–5；附录 A |
| `B15d-2` | §3.4 排除/结构规则为可执行断言并通过 | ✅ | checker 组 7–9、12；逐 id 与既有 105 条交集 0 |
| `B15d-3` | 冻结 SHA 在首次运行前记录，运行后 fixture 未改 | ✅ | §2.1、§2.2 |
| `B15d-4` | **`G1-T` 冷 ≥ 27/30 = 主门槛**；`G1` 为 baseline | ✅（已判定） | §3.1：**25/30 → FAIL** |
| `B15d-5` | `/30` + 两个 `/28` 口径成员与数值 + Wilson；不剔除失败；不得用 28 宣称 PASS | ✅ | §3.1、§3.2、§6.4 |
| `B15d-6` | 失败逐条归因，且不以归因为由改规则 | ✅ | §4、§5；零规则改动（§7.2） |
| `B15d-7` | 写明 blind 定义、边界、不可控项、生成者读过的文件 | ✅（含 1 处越界披露） | §1.1–§1.3 |
| `B15d-8` | 全部 `annotation_guard = true`；`G1` 逐条 0 调用；冷 2 / 热 0 | ✅ | §3.1、§7.3 |
| `B15d-9` | 无生产代码/pom/CI/MCP/G3/DB 写；`tool_eval.py` 空 diff | ✅ | §7.2 |
| `B15d-10` | 报告含复现命令、运行次数、model tag+digest、`prompt_sha256`、缓存 SHA、预热与 `keep_alive` | ✅ | §2、§7.1、§7.3 |
| `B15d-11` | 缓存隔离：冷 `absent` + 指定路径；热 `loaded`；B1.5c 缓存 hash/mtime 不变 | ⚠️ 部分 | 冷/热 ✅；B1.5c 缓存**未变**以 mtime 为证，**缺运行前后 hash 快照**（§7.6 第 1 项） |
| `B15d-12` | 冷/热逐例 `task_success` + `chosen_movie_id` 一致 | ✅ | §3.3（含回放三方一致） |
| `B15d-13` | 2 条探针预声明、单列命中/失败与归因；主门槛仍按 `/30` | ✅ | §1.4、§5 |
| `B15d-14` | checker-only：schema 未改；既有脚本/别名/契约/阈值零改动；3 SHA 冻结 + 时序证据 | ✅ | §2、§7.2 |
| `B15d-15` | 显式写 `G0`/`G2` = 未测，且不据未测项推断结论 | ✅ | §3.1、§7.5 |

## 附录 D — 本报告**不声称**的内容

- 不声称 `G1-T` 达标（**实测 25/30 = 83.3% < 27/30 → FAIL**），也不声称"接近达标"或"只差 2 条就达标"。
- 不声称 B1.5c 的 30/30 是泛化结果（它是 seen fixture 上的 post-fix regression）。
- 不声称 `R-1`/`R-2` 已被裁定或已量化（`R-2` 在本批**未被干净测量**）。
- 不声称任何阈值、分母、用例集合或变体定义被调整（**均未调整**）。
- 不声称中文解析、相似推荐或任何生产能力（解析器/执行器均为 **stand-in**，`n = 2`）。
- 不声称 `G0`/`G2`/`G3`/MCP 有任何结果（未测/未实现），也不据未测项推断。
- 不声称 B-① 已开始或已交付；不声称 #74 可关闭；不声称 #76 已关闭（**PR 待建**）。

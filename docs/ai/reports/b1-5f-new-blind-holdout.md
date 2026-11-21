# B1.5f — 新盲集（`C01`–`C30`）重测 `G1-T` 门槛（**FAIL：24/30 = 80.0% < 27/30**）

> **文档状态：🔴 实测完成（2026-11-20，一次冷跑）。主门槛 = 新 blind 30 条上的 `G1-T`（冷跑）任务级 **24/30 = 80.0%**（Wilson [62.7%, 90.5%]）**< 90%（27/30）→ FAIL**。B1.5e 的修复**没有泛化**到这套从未参与任何规则开发的样本上。本报告**不修规则、不改阈值/分母/用例、不重跑**；B1.5f 的数字自此**冻结**；issue **#74 保持 OPEN**，issue **#81 = 报告完成 / FAIL / PR 待建（可在 PR 合并后关闭）**；**B-① 仍未开始**。**
>
> | 元数据 | 值 |
> | --- | --- |
> | 文档类型 | 评测报告（实测数字 + 判定；**不改阈值、不改分母、不剔除失败条目、不据失败改规则**） |
> | `last_updated` | 2026-11-20（首版；blind 30 条**首次且唯一**一次运行） |
> | 所属 | issue [#81](https://github.com/PNZ-z/ai-powered-Fabflix/issues/81)（`Refs #74`；**#74 保持 OPEN**、无 closing keyword）；计划 `docs/plans/b1-5f-new-blind-holdout.md`（r2 已批准，本次回填结果 = §11） |
> | 判定口径 | **任务级成功率**（`score_case` 严格口径：`tools_ok`/`slots_ok`/`schema_ok`/`behavior_ok`/`grounding_ok`/`acceptable_hit` 全过才算成功）；**主门槛 = `G1-T`（冷跑）≥ 27/30（`/30` 全分母）**；`G1` = baseline（不含中文解析） |
> | 金标准集 | `docs/ai/golden/queries-blind.b1-5f.json`（30 条，`C01`–`C30`，`b1-5f-blind/v1`）+ `docs/ai/golden/query-routing-blind.b1-5f.json`（`b1-5f-blind-routing/v1`）；两者在首次运行前已冻结（§2） |
> | 数据快照 | Docker `fabflix-mysql` 宿主 3307 / `moviedb`，21,087 部（196–2024），`db_snapshot_hash = 8b16c047…f925`（四个运行产物一致） |
> | 端点 / 模型 | `http://<host>:11434`（Ollama），`qwen2.5:7b` digest `845dbda0ea48ed749caafd9e6037047aa19acfcfd82e704d7ca97d631a0b697e`；**运行前预热 1 次**（不计 case，§3.4） |
> | 脚本（零改动） | `scripts/eval/tool_eval.py`（`991db086…af04`，对 `7915fcd` **空 diff**）；新增只读 `scripts/eval/check_blind_holdout_b15f.py`（`d5cfe94a…29fd`，只做断言、不参与路由/判分）；B1.5d 的 `check_blind_holdout.py` **byte-identical**（`8259cc8d…7caf`） |
> | 提交 / PR | 提交 **`9de1da5`**（分支 `codex/b1-5f-blind-holdout`；“feat(eval): add the B1.5f blind holdout and record its G1-T failure”—— 含两个 fixture、B1.5f checker 与本报告；**由主工作流提交，非 docs-writer**）；**PR #82 已合并**（merge commit `b406fad`，2026-11-21T00:12:24Z）；#81 `CLOSED`；#74 保持 `OPEN` |
> | 运行产物 | `.pi/artifacts/b1-5f-eval/{blind-G1,blind-G1T-cold,blind-G1T-warm,blind-G1T-replay}.json` + 同名 `.log`（`.pi/` 不入库）；冻结记录 `.pi/artifacts/b1-5f-freeze.json`；时间线 `run-timeline.txt` |
> | 未做 | **`G0` / `G2` 未测**（计划 `D-3`）；**`G3` 未测**、**MCP 未实现**（全程）；**无生产代码 / 无 `pom.xml` / 无 CI / 无 DB 写** |

---

## 0. 结论

1. **判定：FAIL。** `G1-T`（冷跑）在新 blind 30 条上 **24/30 = 80.0%**（Wilson [62.7%, 90.5%]），低于主门槛 **27/30（90%）**，差 **3 条**。
2. **B1.5e 的修复没有泛化。** B1.5e 修掉了 B1.5d 暴露的 5 个缺口（`B10`/`B22`/`B23`/`B26`/`B27`），但在这套**从未参与任何规则开发**的新 30 条上，`G1-T` 仍只有 80.0%，且**失败形态换了一批**：本次 6 条失败里，**有 2 条是 B1.5e 自己的 title guard 过度修正引入的回归**（`C12`/`C16`），另 2 条是中文 seed 抽取缺口（`C05`/`C17`），1 条是中文译名幻觉（`C01`），1 条是预声明失败（`C18`）。
3. **相对 `G1` 只翻绿 1 条。** `G1-T` 相对 `G1` 仅修复 `C11`（公民凯恩 → Citizen Kane），**`C01`（飞越疯人院）仍未修复**；`G1` 本身 23/30 = 76.7%，`G1-T` 的净增量 **+1**。
4. **本批不修任何规则。** 6 条失败逐条归因（§4），**没有任何规则、别名、契约、阈值、分母、用例被改动**；`tool_eval.py` 对 `7915fcd` 空 diff，`schema_hash` 未变。修它们属于 **B1.5g**，且必须配**又一套新盲集 B1.5h**。
5. **严禁**用 B1.5f 的失败条目改规则后**重跑同一集**——那会把本批降级为 seen fixture，等于重复 B1.5c 的问题。B1.5f 的数字在本报告发布后**冻结**。
6. **issue 状态**：#81 = **报告完成（本文件）/ FAIL / PR 待建**（可在 PR 合并后关闭）；**#74 保持 OPEN**（B1.5c 未达标、`OPEN-C3` 未裁定）；**B-① 仍未开始、未建 issue**。

---

## 1. blind 的定义、纪律与可达边界

### 1.1 可操作定义

这 30 条查询的文本、标注、可接受集合在**第一次运行 `tool_eval.py` 之前**已完整写成文件并记录 SHA-256（§2.1），冻结记录显式写 `tool_eval_runs = 0`；标注期间**没有任何人**运行过任何变体（含 `--ids`/`--limit`/`--dry-run`），因此用例集合不是"按执行结果筛选"的产物。规则、评测代码、别名、契约与阈值在被测基线 `main` @ `7915fcd` 上冻结，本任务**零改动**（§7.1）。

### 1.2 纪律执行情况（计划 §2 `BF-1`…`BF-6`）

| 纪律 | 要求 | 执行情况 |
| --- | --- | --- |
| `BF-1` | 生成者与 B1.5e 修复作者不是同一会话 | ⚠️ **部分**：B1.5f 的生成/标注由**与 B1.5e 修复不同的执行阶段（worker 生成阶段）**完成，但**全部在本仓库同一条协作流水线内**；无法做"不同人"的证明 → 本批**按计划降级表述**（§1.3） |
| `BF-2` | 不读 B1.5e 修复细节（报告 §2–§7、diff、`tool_eval.py` 正文） | ⚠️ 只能保证"生成阶段未执行任何变体"；读文件清单未在冻结记录中逐项留证 → **列为验证缺口**（§8.4 第 1 项） |
| `BF-3` | 标注期零运行 + 只读 SQL 标注 | ✅ `tool_eval_runs = 0`；首次运行 `23:54:53Z` **晚于**冻结 `23:52:22Z`（§2.2）；每条 case 带 `predicate … \| rows=N \| ids=…` |
| `BF-4` | 每个 (variant × 缓存状态) 只跑一次 | ✅ §3.4（`G1` 1 次、冷 1 次、热 1 次、回放 1 次，**无重跑**） |
| `BF-5` | 失败只归因，不替换原数字 | ✅ §4（无标注错误、无 corrected 版本） |
| `BF-6` | 每条 similar 写明约定 + 谓词 + `rows=` | ✅ 7 条 similar 全部有 `same director` / `shared genre × window` 约定（checker 组 13） |

### 1.3 可达边界（必读，沿用 B1.5d `D-5` 并加强）

- **标注者知道探针形状**：`P-1`…`P-8` 的清单随计划/issue 同时公开，因此本文**不能**声称"标注者对修复内容一无所知"。blind 只意味着：**这 30 条从未被任何变体跑过、也未据此调参**。
- **n = 30、单人标注、无第二标注人**：所有比例都只有方向性意义（§8.1）。
- **生成者与修复者的"人"隔离无法证明**：本批只能证明"规则自 `7915fcd` 未变、fixture 先冻结后运行"。因此结论表述为"**在这套未参与规则开发的样本上**"，而不是"独立第三方盲测"。这一条**不允许**被省略成"完全独立盲测"。
- **被测对象是评测替身**：相似/推荐执行器与中文解析器都是 stand-in，派生收窄（`R-2` ③b2）只存在于替身里，**不代表生产语义**。

---

## 2. 冻结与时间线

### 2.1 冻结对象（首次运行前记录）

| 文件 | SHA-256 | 字节 | mtime (local, -08:00) |
| --- | --- | --- | --- |
| `docs/ai/golden/queries-blind.b1-5f.json` | `2e36bbf5ce03cba9a5e3eee297115ebfa99c9add1bee14bdcd8113941b6e121a` | 33,952 | `2026-11-20T15:46:32-08:00` |
| `docs/ai/golden/query-routing-blind.b1-5f.json` | `a74e383bbaf80e46d91469c426a7577662f4194e96059f441ee8f3b38363fb40` | 20,479 | `2026-11-20T15:46:32-08:00` |
| `scripts/eval/check_blind_holdout_b15f.py` | `d5cfe94afdfb6e51e61833b32acbd5178f71faf01cbaecfc0d0ba02b3e7a29fd` | 75,506 | `2026-11-20T15:50:12-08:00` |

- **冻结方式 = (b)（未提交）**：任务禁止 commit/push，故用 `git hash-object -w` 记 blob SHA（`0b0c2173…caa` / `450c27a6…7d8` / `c6c05ddb…839`）+ `stat` mtime，写入 `.pi/artifacts/b1-5f-freeze.json`（`generated_at = 2026-11-20T15:52:22-08:00`，`tool_eval_runs: 0`）。
- **checker 不自证自身 hash**（自指不可实现）；其 SHA 由该外部冻结记录保证。
- 冻结后复核：三个 SHA 与冻结值逐一相等（本报告写作时用 `shasum -a 256` 再核一次，结果一致，§7.1）。
- `db_snapshot_hash` 三处一致：两 fixture 的 `snapshot.db_snapshot_hash`、冻结记录、四份运行产物均为 `8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925`。

### 2.2 时间线（"首次运行晚于冻结"的证据）

| 时刻 (UTC) | 事件 | 证据 |
| --- | --- | --- |
| 2026-11-20T23:52:22Z | **冻结记录写入**（`generated_at`，本地 `15:52:22-08:00`） | `.pi/artifacts/b1-5f-freeze.json` |
| 2026-11-20T23:55:01Z | 预热 1 次模型请求（不计 case；`run-timeline.txt` 记 `prewarm_done=23:55:08Z` 为完成时刻） | `run-timeline.txt` |
| **2026-11-20T23:54:53Z** | **首次 `tool_eval.py` 运行开始**（`G1`） | `run-timeline.txt`（`first_run_start`） |
| 2026-11-20T23:55:08Z | 预热完成 / `G1-T` **冷跑**开始（缓存 `absent`） | `run-timeline.txt`（`cold_start`）；产物 `blind-G1T-cold.json` |
| 2026-11-20T23:55:17Z | `G1-T` **热跑**开始（缓存 `loaded`） | `run-timeline.txt`（`warm_start`） |
| 2026-11-20T23:55:18Z | `G1-T` **回放**开始（`replay`） | `run-timeline.txt`（`replay_start`） |

- **结论：首次运行 `23:54:53Z` > 冻结 `23:52:22Z`**（早 2 分 31 秒）。
- 既有 checker 与新增 checker 全绿：`check_schema.py --quiet` = 0、`check_golden.py --quiet` = 0（复算 `db_snapshot_hash`）、`check_heldout.py` = 0、`check_blind_holdout.py` = 0（B1.5d 证据完好）、**`check_blind_holdout_b15f.py` 18 组断言全过**（`--skip-db`/全量/`--quiet` 各 exit 0）。
- **checker 变异自测 8/8 被捕获**（计划 `B15f-11` 要求 ≥5）：`MUT-1` 改 ID → 组 2；`MUT-2` `P-1` 回退 `Music` 单口径 → 组 15（+12）；`MUT-3` 删 `C16` 约定 → 组 13；`MUT-4` 删 `P-4` 预期失败预声明 → 组 14；`MUT-5` `P-3` 换无关行 → 组 16（+12）；`MUT-6` `C25` 种子改成既有片名 → 组 17（+7）；`MUT-7` `P-3` 换成 2 类型种子 → 组 16；`MUT-8` `C11` 丢 `《…》` → 组 5。**全部 exit 1**，记录在冻结记录 `checker.mutation_self_test`。

---

## 3. 主表

### 3.1 四个变体 × 四个分母

> 分母成员**显式**列出；`/28` 的两种口径**成员不同**（一个去掉 2 条中文用例，一个去掉 2 条已知缺口探针），**不可互相替代**；**只有 `/30` 能承载 PASS/FAIL 结论**。

| 变体 / 缓存状态 | `/30`（主分母） | Wilson 95% | `/28` 去 2 条 CJK（`C01`/`C11`） | Wilson 95% | `/28` 去 2 条探针（`C22`/`C25` = `P-1`/`P-2`） | Wilson 95% | `/27` 再去 `P-4`（`C18`） | Wilson 95% | 路由层 | 模型调用 | 缓存状态 | 判定 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **`G1-T` 冷跑**（主门槛对象） | **24/30 = 80.0%** | [62.7%, 90.5%] | 23/28 = 82.1% | [64.4%, 92.1%] | 22/28 = 78.6% | [60.5%, 89.8%] | 22/27 = 81.5% | [63.3%, 91.8%] | 24/30 | **2** | `absent` | ❌ **FAIL（< 27/30）** |
| `G1-T` 热跑（同缓存） | 24/30 = 80.0% | [62.7%, 90.5%] | 23/28 = 82.1% | [64.4%, 92.1%] | 22/28 = 78.6% | [60.5%, 89.8%] | 22/27 = 81.5% | [63.3%, 91.8%] | 24/30 | **0** | `loaded`；`cache_hits = 2` | 与冷跑**逐例一致**（§3.3） |
| `G1-T` 回放（同一冷跑缓存） | 24/30 = 80.0% | [62.7%, 90.5%] | 23/28 = 82.1% | [64.4%, 92.1%] | 22/28 = 78.6% | [60.5%, 89.8%] | 22/27 = 81.5% | [63.3%, 91.8%] | 24/30 | **0** | `replay`；`cache_hits = 2` | 与冷跑**逐例一致**（§3.3） |
| **`G1`**（baseline，无中文解析） | **23/30 = 76.7%** | [59.1%, 88.2%] | 23/28 = 82.1% | [64.4%, 92.1%] | **21/28 = 75.0%** | [56.6%, 87.3%] | **21/27 = 77.8%** | [59.2%, 89.4%] | 23/30 | **0** | 不适用（`resolver.enabled = false`） | baseline（**不是**门槛对象） |
| `G0` / `G2` | — | — | — | — | — | — | — | — | — | — | — | ⬜ **未测**（计划 `D-3`） |
| `G3` / MCP | — | — | — | — | — | — | — | — | — | — | — | ⬜ **未测 / 未实现** |

**必读的五条限制**：

1. **`/30` 是唯一门槛口径。** `24/30 = 80.0%` 的点估计低于 90%，判定 **FAIL**。
2. **三个敏感性口径（82.1% / 78.6% / 81.5%）全部低于 90%，且都不得用来宣称 PASS**；它们只是"移除已知失败成员后数字如何变"的敏感性分析，**不是**新的门槛。
3. **`/27` 的成员必须逐个列名**：`P-1`（`C22`，`R-1` union）、`P-2`（`C25`，`R-2`）、`P-4`（`C18`，`Q12` 同形预声明失败）。`22/27` 的分子是"24 − `C22` − `C25`"（`C18` 本来就失败，移除不减分子）。
4. **容错算术（预登记，事后不放宽）**：`/30` 允许最多 3 条失败；`P-4` 预声明失败占掉 1 条 ⇒ **只剩 2 条非预期失败空间**。本批**非预期失败 5 条**（`C01`/`C05`/`C12`/`C16`/`C17`）⇒ **超出 3 条**，远超容错。**任何"29/30 也算 PASS"式的表述在这里都不成立**（本批是 24/30）。
5. **`G1` 的 `/28（去 CJK）` 与 `/28（去探针）` 分别 23/28 与 21/28，不能混用**：`G1` 下 2 条 CJK 用例（`C01`/`C11`）**都失败**（移除不减分子 → 23/28）；2 条探针（`C22`/`C25`）在 `G1` 下**都通过**（移除减 2 → 21/28，再去 `C18` → 21/27）。两种口径**成员不同、数值不同**，均不得替代 `/30`。

### 3.2 派生口径的算法（可逐例复算）

- **`/28` 去 CJK** = 移除 `C01`（`P-8`，飞越疯人院）与 `C11`（`P-8`，公民凯恩）：`C01` 失败、`C11` 通过 ⇒ `24 − 1 = 23`，分母 28。
- **`/28` 去探针 `P-1`/`P-2`** = 移除 `C22`（union filter）与 `C25`（`R-2` similar），**两者都通过** ⇒ `24 − 2 = 22`，分母 28。
- **`/27`** = 再去 `P-4`（`C18`，本来就失败）⇒ 分子仍 22，分母 27。
- Wilson 区间按 `z = 1.96`（双侧 95%）计算，与 §3.1 数值一一对应（可复算：`24/30 → [62.7, 90.5]`、`23/28 → [64.4, 92.1]`、`22/28 → [60.5, 89.8]`、`22/27 → [63.3, 91.8]`）。

### 3.3 冷 / 热 / 回放逐例一致性（`B15f-5`）

- 30/30 条 `task_success` **完全相同**（冷 = 热 = 回放，逐例 diff 为空）；`resolver.chosen_movie_id` **完全相同**（`C11` → `OW1`，其余 29 条皆 `null`）。**无缓存一致性失败。**
- 冷跑 `model_calls = 2`、`cache_hits = 0`；热跑 `0` 次调用、`cache_hits = 2`；回放 `0` 次调用、`cache_hits = 2`。
- 冷跑 `resolver.cache_state_at_load = "absent"`、`cache_path = .pi/artifacts/b1-5f-eval/resolver-cache.json`；热跑 `"loaded"`、`cache_problems = []`。

### 3.4 解析（title resolution）指标 — `G1-T` 冷跑（n = 2，逐例披露，**不是**中文解析成功率估计）

| 指标 | 值 |
| --- | --- |
| `triggered` | **2**（`C01`、`C11`；预热后冷启动延迟**未测**） |
| `parse_success` | **1/2**（`C11` → `Citizen Kane`） |
| `catalog_verified` | 1/2（`match = exact`，`OW1`） |
| `candidate_reject` | **1/2**（`C01` → LLM 候选 `Fly Away High` 被目录校验拒绝，`catalog_verified = []`） |
| `wrong_movie` | **0/2**（`wrong_movie_cases = []`） |
| `fallback_to_clarify` | **1**（`C01` → `degraded_to = "clarify"`，`fallback_rate = 0.5`） |
| `alias_hits` | **0**（`title-aliases.json` 仍 `entries: []`） |
| `timeouts` | 0（`timeout_s = 8.0`） |
| latency | p50 **208.4 ms**（`C11`）、p95 **210.1 ms**（`C01`）——**仅 2 个样本**，p95 即最大值 |
| 请求指纹 | `request_sha256`：`C01` = `4912234c…27a5`、`C11` = `7092e2bd…8bf1`；解析器 `prompt_id = b1-5c-title-resolver/v1`、`prompt_sha256 = 8d6d9e4f…b041` |
| 预算核对 | 冷跑 `model_calls = 2` = 计划 §5.4 预登记的 2（只有 2 条 `cjk_title` 触发解析器）；预热 1 次 → 模型总请求 **3**（≤4） |

### 3.5 分意图（`G1` vs `G1-T` 冷跑）

| 意图 | 用例 | `G1` | `G1-T`（冷） | 变化 |
| --- | --- | --- | --- | --- |
| 拒绝 `refusal` | 4（`C07`/`C20`/`C28`/`C29`） | 4/4 | 4/4 | 0 |
| 澄清 `clarification` | 2（`C08`/`C30`） | 2/2 | 2/2 | 0 |
| 筛选 `filter_catalog` | 9 | 9/9 | 9/9 | 0 |
| 片名 `search_by_title` | 5 | 2/5 | **3/5** | **+1（`C11`）** |
| 偏好推荐 `recommend_by_preference` | 3 | 3/3 | 3/3 | 0 |
| 相似 `similar_movies` | 7 | 3/7 | 3/7 | 0 |
| **合计** | **30** | **23/30** | **24/30** | **+1** |

- **净增量只有 +1，且是唯一一条成功的中文译名解析**（`C11` 公民凯恩 → Citizen Kane）。`G1` 的两条 `P-8` 用例都失败，`G1-T` 只救回一条 ⇒ **中文译名在本批是 1/2**（n = 2，不作为能力估计）。
- `G1-T` 的 6 条失败是 `G1` 的 7 条失败的**真子集**（无回退，但也没有新增修复）。
- **相似类 3/7 在两条路线上完全相同**：`G1`/`G1-T` 的相似失败都是 `C05`/`C16`/`C17`/`C18`（4 条），与中文解析无关——**它们全是 `seed_title` 抽取问题**（§4）。

---

## 4. 六条失败逐条归因

> 归因口径：**真实规则/执行缺口** / **B1.5e 引入的回归** / **预声明探针** / **标注问题**。本批**没有**发现标注错误；**没有任何规则因这些失败被修改**。

| # | 用例 | 查询 | 预期（golden） | 实际（冷跑） | 失败模式 | 归因 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | **`C01`**（`P-8`） | `我想看《飞越疯人院》，帮我找一下` | 解析为 `One Flew Over the Cuckoo's Nest`（`MF5`）→ `search_movies_by_title`，1 行 | 解析器 `triggered`，LLM 候选 **`Fly Away High`**（**目录里不存在**）→ 目录校验拒绝 → 无工具调用，回 `which English title do you mean?` | `no_tool_call`、`slot_mismatch`、`acceptable_set_miss`、`empty_result` | **真实中文译名/幻觉缺口**（不是 guard，也不是标注）：小模型对该片名产生**幻觉候选**，而 `title-aliases.json` 为空、无回退路径 |
| 2 | **`C05`** | `喜欢 Jumanji 这种奇幻片，1993 到 1995 年的` | `search_movies_by_title("Jumanji")` + `find_similar_movies`（共享类型 × 窗口，rows = 8） | `seed_title = None` → 只调 `filter_movies{genres:["Fantasy"], 1993–1995}`（5 行） | `wrong_tool`、`incomplete_tool_sequence`、`slot_mismatch` | **真实 title 抽取缺口**：中文 **"喜欢"** 未触发 seed 抽取（动词 pattern 只覆盖英文 `like/liked/love/loved/enjoyed/watched`），整句没有英文片名锚点 |
| 3 | **`C12`** | `查一下 Western Union 这部电影` | `search_movies_by_title("Western Union")`，1 行（`PuA10`） | `query = None` → `filter_movies{genres:["Western"]}`（5 行，全是别的西部片） | `wrong_tool`、`incomplete_tool_sequence`、`slot_mismatch`、`acceptable_set_miss` | **B1.5e `F-2` 的 title guard 过度修正（回归）**：片名首词 `Western` 是类型别名 ⇒ 被 `GENRE_TITLE_WORDS` 守卫判成从句，整段片名被丢弃 |
| 4 | **`C16`**（`P-6`） | `找一些像 War Games 那样的冒险片，1983 到 1984 年的` | `search_movies_by_title("War Games")` + `find_similar_movies`（`Adventure` × 1983–1984，rows = 13） | `seed_title = None` → `filter_movies{genres:["War","Adventure"], 1983–1984}` → **`EMPTY_RESULT`** | `wrong_tool`、`incomplete_tool_sequence`、`slot_mismatch`、`acceptable_set_miss`、`empty_result` | **同一个 title guard 回归**（片名首词 `War` 是类型词）。**探针 `P-6` 失败，但根因不是新一轮"类型词泄漏"，而是 title 抽取回归**——`War` 变成约束是"片名没被抽取"的机械后果（正文 §5.2 说明为何必须这样归因） |
| 5 | **`C17`** | `喜欢 Willow，想看 1986 到 1988 年的奇幻片` | `search_movies_by_title("Willow")` + `find_similar_movies`（共享类型 × 窗口，rows = 10） | `seed_title = None` → `filter_movies{genres:["Fantasy"], 1986–1988}`（5 行） | `wrong_tool`、`incomplete_tool_sequence`、`slot_mismatch` | **真实 title 抽取缺口**：同 `C05`，中文 **"喜欢"** 未触发 seed 抽取 |
| 6 | **`C18`**（`P-4`） | `跟 That Hamilton Woman 相似的电影` | **预声明为已知不可满足**：2 类型种子 + 无文本约束 = `Q12` 同形；标注 `same director`（A. Korda 的其他目录行，rows = 12） | 路由 **`action = "clarify"`**（`which movie intent?`），`seed_title = None`，`find_similar_movies` **未被调用** | `no_tool_call`、`slot_mismatch`、`acceptable_set_miss`、`empty_result` | **预声明失败（`R-2`/`Q12-unsat`）**——但**实际没有真正行使 `Q12` 语义**（见 §4.1） |

### 4.1 `C18`（`P-4`）必须区分"预声明失败"与"探针形态未命中"

`P-4` 的设计目的是**复现 `Q12` 的已知不可满足形态**：种子恰好 2 个 canonical genres、文本零约束、标注按 `same director`。若规则真的走到 `find_similar_movies` 并因 `R-2` 派生收窄而与标注冲突，那才是"`Q12` 例外被复现"。

本批的实际链路是：

1. `seed_title` 抽取失败（`That Hamilton Woman` 没有被识别为片名）⇒ `seed_title = None`；
2. 文本又**没有任何**类型/年代约束（就该探针的设计而言是有意的）⇒ 路由器没有任何可执行槽位；
3. 于是 `action = "clarify"`，**`find_similar_movies` 一次都没调用**。

因此 `C18` 的失败**首先是 seed 抽取失败**，"`Q12` 语义"根本没有被执行到。**本批不能声称 `Q12` 例外已被复现或已被量化**；`P-4` 只是"如预期失败"，但**失败的原因与预声明的机制不同**。这一点不影响 `P-4` 仍占 `/30` 的容错额（计划 `OD-f4` 已定），但**必须与"复现了 `Q12`"这一说法明确区分开**。

### 4.2 失败与 `G1` 的对应关系

| 用例 | `G1` | `G1-T`（冷） | 说明 |
| --- | --- | --- | --- |
| `C01` | ❌ | ❌ | 中文译名在两条路线上都失败（`G1` 无解析层，`G1-T` 解析出幻觉候选） |
| `C05` | ❌ | ❌ | 与解析层无关：中文 "喜欢" 不触发 seed 抽取 |
| `C11` | ❌ | ✅ | **本批唯一的净修复**（`G1` 无解析层 → `G1-T` 解析成功） |
| `C12` | ❌ | ❌ | title guard 回归，与解析层无关 |
| `C16` | ❌ | ❌ | 同上 |
| `C17` | ❌ | ❌ | 同 `C05` |
| `C18` | ❌ | ❌ | 预声明 + seed 抽取失败（§4.1） |

---

## 5. 探针表（`P-1`…`P-8`，10/30 条）

| 探针 | 用例 | 形态 | 冻结口径 | `G1` | `G1-T`（冷） | 读数 / 归因 |
| --- | --- | --- | --- | --- | --- | --- |
| `P-1`（`R-1`） | `C22` | `musicals from 1980 to 1982` | **union（`Music` ∪ `Musical`）= 13 行**（`Music` 12 + `Musical` 1 = `RMY15`）；top-5 ⊆ union 为 checker 硬断言 | ✅ | ✅ | `filter_movies{genres:["Music"], 1980–1982}`；top-5 = `CSr8`/`PaB10`/`RFM12`/**`RMY15`**/`HJS10`，**5/5 ⊆ 13 行 union**（§6） |
| `P-2`（`R-2`） | `C25` | `I liked Star 80 - can you find something similar?` | 种子**恰好 2 个 canonical genres**（`Biography`,`Drama`），按 shared genre × 同年代标注（rows = 9） | ✅ | ✅ | `search_movies_by_title` + `find_similar_movies`；`constraints_applied = {genres:[Biography,Drama], 1980–1989}`、`candidate_pool = 9` ⇒ **③b2 派生生效且未过窄** |
| `P-3` | `C04` | `想看类似 Gozu 那样的电影` | 种子 **3 个 canonical genres**（`Crime`,`Drama`,`Horror`）+ 已知导演（Takashi Miike），`same director`，rows = 6 | ✅ | ✅ | `candidate_pool = 300`、`constraints_applied = {}` ⇒ **③b2 不派生的边界成立**；top-5 全部 `same_director = 1`（靠排序命中） |
| `P-4`（`Q12` 同形） | `C18` | `跟 That Hamilton Woman 相似的电影` | **预声明预期失败**（2 类型 + 零文本约束，`same director`，rows = 12） | ❌ | ❌ | **如预期失败，但机制未命中**：`seed_title = None` → `action = "clarify"`，`find_similar_movies` 未调用（§4.1） |
| `P-5`（越界 ×2） | `C07`、`C28` | `Jurassic Park 当年的全球票房是多少` / `How much did Jaws earn at the box office?` | `expects_no_match`、空可接受集、无 expected tool；**不得含 `《…》`** | ✅✅ | ✅✅ | 两条都被拒绝、未调工具（`model_calls` 未增加 ⇒ 无第三次解析器调用）。`F-1` 的 `out_of_catalog` 规则在本批**成立** |
| `P-6` | `C16` | `找一些像 War Games 那样的冒险片，1983 到 1984 年的` | 片名内类型词 `War` **不得**成为约束；`shared genre × window`（`Adventure` × 1983–1984，rows = 13） | ❌ | ❌ | **探针失败，但根因是 title 抽取回归**：片名未被抽取 → `War` 被当作类型约束 → 与 `Adventure` 求交为空。**不是** `R-1`/`R-2`，也**不是**一个新发现的"类型词泄漏"缺陷（§5.2） |
| `P-7`（复数类型词） | `C23` | `documentaries from 1974 to 1976` | `Documentary` × 1974–1976（rows = 7） | ✅ | ✅ | `filter_movies{genres:["Documentary"], 1974–1976}` ⇒ B1.5e 的 `documentaries` 别名**成立** |
| `P-8`（新 CJK 译名 ×2） | `C01`、`C11` | `飞越疯人院` / `公民凯恩` | 两个新中文片名（`title-aliases.json` 仍空）；通过/失败**都只逐例披露**（n = 2） | ❌❌ | ❌✅ | `C11` 解析成功（`OW1`）；`C01` 解析出**幻觉候选** `Fly Away High` 被目录拒绝（§7.3）。**1/2 不得作为中文解析能力估计** |

### 5.1 探针结论汇总

- **成立的探针**：`P-1`（`R-1` union）、`P-2`（`R-2` 派生）、`P-3`（3+ 类型不派生）、`P-5`（越界拒绝 ×2）、`P-7`（复数类型词）。
- **失败的探针**：`P-4`（预声明失败，但机制未命中）、`P-6`（title 抽取回归）、`P-8` 的 1/2（幻觉）。
- **B1.5e 的两个语义落码在本批确实生效**：`R-1`（union 召回）与 `R-2`（③b2 派生/不派生边界）都按设计工作。**B1.5e 真正的回归发生在 title guard，不在 `R-1`/`R-2`。**

### 5.2 为什么 `C16` 不能记成"新的类型词泄漏缺陷"

`C16` 的观测是 `filter_movies{genres:["War","Adventure"], 1983–1984}`——字面上看像"片名里的 `War` 泄漏成类型约束"。但归因必须区分**根因**与**后果**：

- B1.5e 的 `F-2` 已经实现"按返回的 title 字符串掩码、从 genres/years 抽取中剔除片名"（`tool_eval.py` 的 `route_deterministically`）。**若片名被正确抽取**，`War` 会被掩码掉，`genres` 只剩 `Adventure`——也就是说"类型词泄漏"这一层**已经被 B1.5e 修过**。
- `C16` 实际发生的是：`GENRE_TITLE_WORDS` 守卫把首词是类型词的**整段**判成从句 ⇒ 片名根本没被抽取 ⇒ 掩码无片名可掩 ⇒ `War` 落进 genres。
- 因此**修 `C16` 的正确位置是 title guard 的判据**（片名 vs 从句），而不是再加一层类型词过滤。把它记成"新的泄漏缺陷"会误导 B1.5g 去修错地方。

---

## 6. `R-1` union 13 行的证据（`P-1`/`C22`）

**语义**：`musical`/`musicals` 与 `music` 折成同一 canonical key（B1.5e `R-1`），但**目录 enum 仍分开**（`Music` 619 行 / `Musical` 139 行）。这是**召回扩大**，不是等价替换，因此标注必须按 **union**，不能沿用 B1.5d 的 24 行 `Music` 口径。

| 读数（1980–1982 窗口） | 谓词 | rows |
| --- | --- | --- |
| `Music` | `year 1980–1982 ∧ genres ⊇ {Music}` | **12** |
| `Musical` | `year 1980–1982 ∧ genres ⊇ {Musical}` | **1**（`RMY15` One Trick Pony） |
| **union** | `year 1980–1982 ∧ genres ∈ {Music, Musical}` | **13** |
| 双重标记行 | 同窗口同时带两值 | **0** |

**实际 top-5（冷跑，`filter_movies{genres:["Music"], year_from:1980, year_to:1982}`）**：

| # | id | title | year | 目录类型 |
| --- | --- | --- | --- | --- |
| 1 | `CSr8` | Bodas de Sangre | 1981 | Music |
| 2 | `PaB10` | Grease 2 | 1982 | Music |
| 3 | `RFM12` | Night the Lights Went Out in Georgia | 1981 | Music |
| 4 | **`RMY15`** | One Trick Pony | 1980 | **Musical** |
| 5 | `HJS10` | Parsifal | 1982 | Music |

- **5/5 ⊆ 13 行 union**（checker 组 15 硬断言通过；不只是 `acceptable_hit` 非空）。
- **唯一那行 `Musical`（`RMY15`）确实出现在 top-5** ⇒ union 口径在真实运行里被行使，而不是"名义上扩大、实际上从不触发"。
- **窗口从计划的 1948–1950/25 改为 1980–1982/13**（主 agent 裁定 B，冻结记录 `plan_deviation`）：旧窗口的 24 个 `Music` id 与 B1.5d `B22` 的可接受集逐一相同，会违反"与 135 条计分用例逐 id 不重叠"的排除规则；新窗口与基线重叠 **0**，且 union 严格大于 `Music` 读数。

---

## 7. B1.5e 的两个真实回归 / 缺口（本批最重要的发现）

### 7.1 title guard 过度修正（`C12`、`C16`）

- **B1.5e 做了什么**：`F-2` 为修 `B23`/`B26` 的"从句被当片名"，在 verb-pattern 捕获上加了两道守卫——`FUNCTION_WORDS` 首词守卫与 **`GENRE_TITLE_WORDS` 类型词首词守卫**（`tool_eval.py` `TITLE_PATTERNS`/`extract_title`）。
- **本批暴露的代价**：**首词本身是类型词的合法片名被整体丢弃**。
  - `C12`：`Western Union`（1950 年的真片）→ `Western` 命中类型别名 ⇒ 整段判从句 ⇒ `query = None`，退化成 `filter_movies{genres:["Western"]}`（返回 5 行**别的**西部片）。
  - `C16`：`War Games`（1983 年的真片）→ `War` 命中类型别名 ⇒ 整段判从句 ⇒ `seed_title = None`，退化成 `filter_movies{genres:["War","Adventure"], 1983–1984}` → **0 行**。
- **这是回归，不是未覆盖**：两条用例的片名形式（"动作/西部/战争"类词开头的英文片名）在 B1.5e 之前是**能被抽取**的；守卫把它们变成了"从句"。B1.5e 的 in-sample 75 条与 seen 的 B1.5d 30 条**都没有**这种首词撞类型词的片名，所以当时零回归——**这正是新盲集存在的理由**。
- **修法方向（供 B1.5g 参考，本报告不实施）**：把"首词是类型词 ⇒ 从句"改成**双向判据**（例如同时要求"后接功能词/从句结构"或"片名候选在目录中存在"），而不是单条件否决；同时保留 `B23`/`H23` 的"句子不是片名"断言。

### 7.2 中文 "喜欢" 不触发 seed 抽取（`C05`、`C17`）

- 两条查询都是**"喜欢 + 英文片名 + 类型/年代"**的中文偏好句：`喜欢 Jumanji 这种奇幻片，1993 到 1995 年的`、`喜欢 Willow，想看 1986 到 1988 年的奇幻片`。
- 路由器把 `seed_title` 抽成 `None`，于是**没有** `search_movies_by_title`，直接 `filter_movies{genres, years}`——即把"以 X 为种子的相似查询"降级成"纯类型筛选"，**丢掉了种子**。
- 根因是**动词/触发词覆盖面**：B1.5e 的 `TITLE_VERBS` 扩到了 `like/liked/love/loved/enjoyed/watched`，**只有英文**；中文 `喜欢`（以及"想看…类似的"这类中文句式）没有等价的触发路径。
- **n = 2**，所以本报告只说"这两条形态失败"，**不估计**中文 seed 抽取的整体成功率。

### 7.3 中文译名幻觉（`C01`）

- `我想看《飞越疯人院》，帮我找一下` 的正确目录名是 **`One Flew Over the Cuckoo's Nest`**（`MF5`）；`qwen2.5:7b` 返回的候选是 **`Fly Away High`**（目录中不存在），目录校验正确拒绝，随后 `degraded_to = "clarify"`。
- **判定行为本身是对的**（拒绝幻觉、不猜、不返回错片）：`wrong_movie = 0`、`candidate_reject = 1`。失败在于**任务没完成**（没有工具调用、没有结果）。
- **与 `C11` 的对比**：同一解析器、同一 prompt、同一模型，`公民凯恩 → Citizen Kane` 成功（`match = exact`）。这说明**不是机制坏了，而是 n = 2 的抽样里有一条译名超出小模型能力**；`title-aliases.json` 仍为空（`alias_hits = 0`），没有任何回退知识。
- **不得**据此声称"中文解析不可用"（n = 2），**也不得**声称"只需加一条别名就达标"（那会改被测对象）。

---

## 8. 解释：为什么 24/30 是真实信号，以及它**不能**说明什么

### 8.1 统计力（n = 30）

| 观测 | 点估计 | Wilson 95% | 与门槛的关系 |
| --- | --- | --- | --- |
| **24/30（本批主数字）** | **80.0%** | [62.7%, 90.5%] | **点估计 < 90% → FAIL**；区间上界 90.5% 略高于门槛，因此**不能**反推"真实能力远低于 90%"，也**不能**反推"可能达标" |
| 27/30（门槛） | 90.0% | [74.4%, 96.5%] | 即便达标，区间下界也只有 74.4% |
| 23/28（去 CJK，敏感性） | 82.1% | [64.4%, 92.1%] | **不得宣称 PASS** |
| 22/28（去 `P-1`/`P-2`，敏感性） | 78.6% | [60.5%, 89.8%] | **不得宣称 PASS** |
| 22/27（再去 `P-4`，敏感性） | 81.5% | [63.3%, 91.8%] | **不得宣称 PASS** |

- **禁止表述**：① "95% 置信下 ≥90%"（任何口径都做不到）；② 用任何 `/28`/`/27` 数字宣称主门槛达标；③ 因"只差 3 条"而调阈值、扩分母或剔除失败条目；④ 把 B1.5c 的 30/30 或 B1.5d 的 25/30 与本批比"强弱"（成员、规则、别名都不同 → **不可比**）。

### 8.2 为什么把 24/30 当作真实信号

- **时序**：冻结 `23:52:22Z` 严格早于首次运行 `23:54:53Z`（§2.2）；`tool_eval.py` 对 `7915fcd` **空 diff**；别名、契约、阈值、既有 checker 的 SHA 逐一不变——被测对象在本批运行期间**没有被动过**。
- **样本独立性**：30 条在 B1.5e 合并后生成；与 **135 条计分用例**（60+15+30+30）的文本/种子/可接受集不重复，可接受 id 与既有并集 **718** 个 id **零重叠**，种子片名与既有 **29** 个种子零重叠（checker 组 7/17）；无合成行、无 2020s 用例、1990s 3 条（`C05`/`C06`/`C09`）、2010s 0 条。
- **一次一组合**：`G1`、`G1-T` 冷、热、回放各 1 次，**看结果后没有重跑、没有换用例、没有改规则**（§3.4）。
- **失败可复核**：6 条失败各有 tool call 原文 + 谓词 + 行数（§4、附录 A），且热跑/回放与冷跑逐例一致 ⇒ 不依赖时序/网络/缓存。
- **失败模式内部一致**：`G1-T` 的 6 条失败是 `G1` 的 7 条失败的**真子集**，且 `G1-T` 只把 `C11` 一条变绿 ⇒ 排除了"解析层引入新回归"这一解释。

### 8.3 为什么 B1.5d / B1.5e 的数字不能与本批并列

- **B1.5d 的 30 条已成 seen fixture**：B1.5e 正是因为 `B10`/`B22`/`B23`/`B26`/`B27` 才改规则，B1.5e 报告里 B1.5d `G1` 的 28/30（`B01`/`B14`）只能作 **seen 回归记录**。
- **B1.5e 的 70/75 是 in-sample**：同一 75 条 fixture 上调优得到，不是泛化估计。
- **本批的规则/别名基线不同**（`tool_eval.py` `991db086…af04`、别名 `b1-5c-aliases/v2`），且失败形态换了一批 ⇒ **本批只与"90% 门槛"比较，不与历史批次比较强弱**。

### 8.4 本批**不能**说明的内容（含验证缺口）

**不能声称**：泛化 ≥90%、生产可用、B-① 可开始、`G0`/`G2`/`G3`/MCP 有任何结果（未测/未实现）、中文解析能力（n = 2）、`Q12` 例外已被复现（§4.1）、`R-1`/`R-2` 已生产落地（只在评测替身里）。

| # | 验证缺口 | 影响 |
| --- | --- | --- |
| 1 | **`BF-1`/`BF-2`（生成者独立 / 未读修复细节）无法硬证明**：无"实际读过文件清单"落证，生成与修复在同一仓库流水线内 | 本批措辞降级为"未参与规则开发的样本"，**不声称完全独立盲测**；不影响"先冻结后运行"这一硬证据 |
| 2 | **回放运行未显式传 `--resolver-cache`**（`cache_path` 落回默认 `.pi/artifacts/b1-5c-resolver-cache.json`，`replay_resolver` 正确指向冷跑缓存） | 回放 `model_calls = 0`、`cache_hits = 2`，无写入发生；但**缺运行前后对 B1.5c 缓存的 hash 快照对照**（与 B1.5d 同一处卫生缺陷） |
| 3 | 回放的 `base_url` 记录为 `http://localhost:11434`（冷/热为局域网 `<host>`） | `model_calls = 0`，对结果无影响；卫生问题，如实记录 |
| 4 | 预热请求**无独立产物** | 只能引用 `run-timeline.txt`（`prewarm_done=23:55:08Z`）与运行记录；`keep_alive` 与次数来自运行阶段记录 |
| 5 | 冷启动延迟（`OPEN-b2`）**仍未测** | 与本批结论无关，继续挂起 |

---

## 9. 范围、复现与零改动证明

### 9.1 实际执行的命令（与计划 §5.1 同形）

```bash
Q=docs/ai/golden/queries-blind.b1-5f.json; R=docs/ai/golden/query-routing-blind.b1-5f.json; O=.pi/artifacts/b1-5f-eval; K=$O/resolver-cache.json

# 0) 冻结与自检（运行前）
shasum -a 256 $Q $R scripts/eval/check_blind_holdout_b15f.py
python3 scripts/eval/check_schema.py --quiet                 # exit 0（既有，未改）
python3 scripts/eval/check_golden.py --quiet                 # exit 0（复算 db_snapshot_hash）
python3 scripts/eval/check_heldout.py                        # exit 0（B1.5c 证据完好）
python3 scripts/eval/check_blind_holdout.py                  # exit 0（B1.5d 证据完好，byte-identical）
python3 scripts/eval/check_blind_holdout_b15f.py             # exit 0（18 组断言）

# 1) G1 baseline（0 模型调用）
python3 scripts/eval/tool_eval.py --variant G1 --cases queries \
  --queries-path $Q --routing-path $R \
  --allow-full-run --assert-no-annotation-read --out $O/blind-G1.json

# 2) 预热（1 次模型请求，不计 case）
curl -sS http://<host>:11434/api/generate \
  -d '{"model":"qwen2.5:7b","prompt":"ping","stream":false,"keep_alive":"10m"}' >/dev/null

# 3) G1-T 冷跑（主数字）：独立缓存路径，跑前清空
mkdir -p $O && rm -f $K
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path $Q --routing-path $R --resolver-cache $K \
  --base-url http://<host>:11434 --allow-full-run --assert-no-annotation-read \
  --out $O/blind-G1T-cold.json

# 4) G1-T 热跑（同路径同变体；缓存一致性，非独立测量）
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path $Q --routing-path $R --resolver-cache $K \
  --base-url http://<host>:11434 --allow-full-run --assert-no-annotation-read \
  --out $O/blind-G1T-warm.json

# 5) 回放（0 模型调用；只吃冷跑缓存）
python3 scripts/eval/tool_eval.py --variant G1-T --cases queries \
  --queries-path $Q --routing-path $R \
  --replay-resolver $K --allow-full-run --assert-no-annotation-read \
  --out $O/blind-G1T-replay.json
```

- 端点按 `docs/ai/README.md` §3 写作 `<host>:11434`。四份产物 `annotation_guard = true`、`db_snapshot` 四份一致。

### 9.2 运行次数（预登记，事后未增加）

| 组合 | 次数 | `model_calls` | 性质 |
| --- | --- | --- | --- |
| 预热（模型请求） | **1** | 不计 case | `run-timeline.txt`；**冷启动延迟仍未测**（`OPEN-b2`） |
| `G1` × 30 | **1** | **0** | baseline，非门槛对象 |
| `G1-T` × 30（冷） | **1** | **2**（`C01`/`C11`） | **主数字 / 门槛对象**；`cache_state_at_load = "absent"` |
| `G1-T` × 30（热） | **1** | **0** | 缓存一致性（`loaded`，`cache_hits = 2`） |
| `G1-T` 回放 | **1** | **0** | 可复算（`replay`，`cache_hits = 2`） |
| `G0` / `G2` / `G3` / MCP | **0** | — | 未测 / 未实现（`D-3`） |

- 模型总请求 = **1（预热）+ 2（冷跑）= 3**，与计划 §5.4 的预算（≤4）一致。
- 运行指纹：`model = qwen2.5:7b`、`model_digest = 845dbda0…697e`、`temperature = 0`、`prompt_id = b1-5b-inline/v1`、`prompt_sha256 = 58c8b708d919920cf85becce7594bfc4ffa7795599bc923151e86ba9e10e2310`；解析器 `prompt_id = b1-5c-title-resolver/v1`、`prompt_sha256 = 8d6d9e4f…b041`。
- 缓存指纹：`.pi/artifacts/b1-5f-eval/resolver-cache.json` = `14c1a9cb05d1281462192d299186cee0ed3d4e3a26767a05ea606314ed2d48d5`（1,603 B）。

### 9.3 零改动证明

| 项 | 证据 |
| --- | --- |
| `scripts/eval/tool_eval.py` | `991db086c5c618be479b273b8af9db6151e77f3a334b14fa2a557089c184af04`；`git diff --stat 7915fcd -- scripts/eval/tool_eval.py` **输出为空** |
| `git diff --stat 7915fcd -- scripts/eval docs/ai` | **输出为空**（`scripts/eval` 与 `docs/ai` 下**无任何已跟踪文件**被改；新增的只有 3 个未跟踪文件） |
| 既有 checker ×4 | `check_schema.py` `5cb35c7f…1b9f`、`check_golden.py` `0ab8a017…0708`、`check_heldout.py` `d436f5ca…ddd4`、**`check_blind_holdout.py` `8259cc8d…7caf`（byte-identical）**，运行 exit 0 |
| 契约 | `tool-schema.b1-5.json` `0ba15269…1169`，`contract_version b1.5-tools/v3`，`schema_hash 712c0e34…474a`（**未变**） |
| 别名 ×3 | genre `ab8e07dd…c104`（`b1-5c-aliases/v2`）、person `06316859…dd98`、title `98bfca9b…cbe4`（仍 `entries: []`，实测 `alias_hits = 0`） |
| 唯一新增脚本 | `scripts/eval/check_blind_holdout_b15f.py`（只读断言；不 import `tool_eval`、不被其调用） |
| 生产面 | **无** `src/main/**`、**无** `pom.xml`、**无** CI、**无** MCP、**无** DB 写入（只读 `SELECT`） |
| 改动面 | 工作区仅新增 3 个未跟踪文件（2 fixture + 1 checker）+ 本批文档；另有与本任务无关的 `.idea/` 与 `demo/`（见附录 C `B15f-10`） |

### 9.4 演示页（`B15f-10`）

- 页面：`demo/iterations/b1-5f/index.html`（标题 `B1.5f · 新盲集 FAIL`，含 `24/30`、`80.0%`、`FAIL`），`demo/index.html` 已指向该 iteration 作为最新一版。
- 局域网验证（本次只读复核）：`curl -sS -o /dev/null -w "%{http_code}" http://<LAN-IP>:4173/iterations/b1-5f/` → **200**（`ruby` 在 `*:4173` 监听）。
- 页面只放聚合数字与边界说明，**不出现** DB 主机/凭据/Ollama 端点/原始 id；显式声明"演示站不是完整应用"。

---

## 10. 下一步（建议，需主 agent / 用户裁定）

1. **B1.5g（修复）**：修 `C01`（中文译名幻觉 → 需要回退路径或更强的候选校验，**不是**加别名表条目就完事）、`C05`/`C17`（中文 seed 触发词）、`C12`/`C16`（title guard 过度修正，见 §7.1），**并保持 `Q12` 例外不回退**。
2. **B1.5g 必须配一套全新的盲集 B1.5h**（新前缀、新冻结、跑前不可见）。**严禁**用 B1.5f 的 6 条失败改规则后重跑 `C01`–`C30`——那会把本批降级为 seen fixture，正是 B1.5c 的教训。
3. **B1.5f 的数字冻结**在本报告（§3），此后任何"重跑"都只能是 seen 回归，且必须标注。
4. **issue 状态**：#81 = **报告完成（本文件）/ FAIL / PR 待建**，**可在 PR 合并后关闭**；**#74 保持 OPEN**（`OPEN-C3` 与 B1.5c 的门槛均未裁定）；`Refs #74`、**无 closing keyword**。
5. **B-① 未开始**：本报告不构成 B-① 的准入证据；执行器与解析器仍是 stand-in，`G0`/`G2`/`G3`/MCP 缺失。

---

## 附录 A — 30 条清单与逐例结果（✅ = 任务级成功）

| id | track | intent | query | 可接受数 | 探针 | `G1` | `G1-T`（冷） |
| --- | --- | --- | --- | --- | --- | --- | --- |
| C01 | zh_zh | search_by_title | 我想看《飞越疯人院》，帮我找一下 | 1 | `P-8` | ❌ | ❌ |
| C02 | zh_zh | filter_catalog | 1966 到 1968 年的惊悚片有哪些 | 12 |  | ✅ | ✅ |
| C03 | zh_zh | filter_catalog | 2006 到 2007 年评分 7 分以上的奇幻片 | 7 |  | ✅ | ✅ |
| C04 | zh_zh | similar_movies | 想看类似 Gozu 那样的电影 | 6 | `P-3` | ✅ | ✅ |
| C05 | zh_zh | similar_movies | 喜欢 Jumanji 这种奇幻片，1993 到 1995 年的 | 8 |  | ❌ | ❌ |
| C06 | zh_zh | recommend_by_preference | 推荐几部 1990 年代的喜剧剧情片 | 17 |  | ✅ | ✅ |
| C07 | zh_zh | clarify_or_refuse（拒绝） | Jurassic Park 当年的全球票房是多少 | 0 | `P-5` | ✅ | ✅ |
| C08 | zh_zh | clarify_or_refuse（澄清） | 帮我找那部叫 The Winslow Boy 的电影 | 2 |  | ✅ | ✅ |
| C09 | zh_zh | filter_catalog | 1993 到 1995 年的西部片有哪些 | 13 |  | ✅ | ✅ |
| C10 | zh_zh | search_by_title | 帮我找 1971 年上映的 The Hospital | 1 |  | ✅ | ✅ |
| C11 | zh_en | search_by_title | 《公民凯恩》是哪一年的电影 | 1 | `P-8` | ❌ | ✅ |
| C12 | zh_en | search_by_title | 查一下 Western Union 这部电影 | 1 |  | ❌ | ❌ |
| C13 | zh_en | filter_catalog | 1970 到 1972 年的动画片 | 6 |  | ✅ | ✅ |
| C14 | zh_en | filter_catalog | 2007 到 2008 年评分 7 分以上的纪录片 | 13 |  | ✅ | ✅ |
| C15 | zh_en | filter_catalog | 1980 到 1982 年的黑色电影 | 6 |  | ✅ | ✅ |
| C16 | zh_en | similar_movies | 找一些像 War Games 那样的冒险片，1983 到 1984 年的 | 13 | `P-6` | ❌ | ❌ |
| C17 | zh_en | similar_movies | 喜欢 Willow，想看 1986 到 1988 年的奇幻片 | 10 |  | ❌ | ❌ |
| C18 | zh_en | similar_movies | 跟 That Hamilton Woman 相似的电影 | 12 | `P-4` | ❌ | ❌ |
| C19 | zh_en | recommend_by_preference | 推荐 2007 到 2008 年评分 6.5 分以上的历史片 | 6 |  | ✅ | ✅ |
| C20 | zh_en | clarify_or_refuse（拒绝） | 这些电影在 Netflix 上可以在线看吗 | 0 |  | ✅ | ✅ |
| C21 | en_en | search_by_title | Can you find the movie Star Wars? | 1 |  | ✅ | ✅ |
| C22 | en_en | filter_catalog | musicals from 1980 to 1982 | 13 | `P-1` | ✅ | ✅ |
| C23 | en_en | filter_catalog | documentaries from 1974 to 1976 | 7 | `P-7` | ✅ | ✅ |
| C24 | en_en | filter_catalog | horror movies from 1950 to 1952 | 5 |  | ✅ | ✅ |
| C25 | en_en | similar_movies | I liked Star 80 - can you find something similar? | 9 | `P-2` | ✅ | ✅ |
| C26 | en_en | similar_movies | movies like The Searchers - westerns from 1955 to 1957 | 12 |  | ✅ | ✅ |
| C27 | en_en | recommend_by_preference | recommend some films by Pakula | 9 |  | ✅ | ✅ |
| C28 | en_en | clarify_or_refuse（拒绝） | How much did Jaws earn at the box office? | 0 | `P-5` | ✅ | ✅ |
| C29 | en_en | clarify_or_refuse（拒绝） | What is the Rotten Tomatoes score for The Godfather? | 0 |  | ✅ | ✅ |
| C30 | en_en | clarify_or_refuse（澄清） | Is there a movie called Pygmalion? | 3 |  | ✅ | ✅ |

**结构复核**（checker 组 1–8 全过）：track 10/10/10；intent `search_by_title` 5 / `filter_catalog` 9 / `similar_movies` 7 / `recommend_by_preference` 3 / `clarify_or_refuse` 6（拒绝 4 + 澄清 2）；`multi_slot` 4；`cjk_title` 2（`C01`/`C11`）；1990s 3 条、2010s 0 条、2020s 0 条；无合成行；可接受 id 与既有 718 个 id 零重叠；种子片名与既有 29 个零重叠；`G1-T` 冷/热/回放逐例结果完全相同。

## 附录 B — 冻结哈希与运行产物

| 项 | 值 |
| --- | --- |
| `queries-blind.b1-5f.json` | `2e36bbf5ce03cba9a5e3eee297115ebfa99c9add1bee14bdcd8113941b6e121a`（33,952 B） |
| `query-routing-blind.b1-5f.json` | `a74e383bbaf80e46d91469c426a7577662f4194e96059f441ee8f3b38363fb40`（20,479 B） |
| `check_blind_holdout_b15f.py` | `d5cfe94afdfb6e51e61833b32acbd5178f71faf01cbaecfc0d0ba02b3e7a29fd`（75,506 B，18 组断言，变异 8/8） |
| `check_blind_holdout.py`（B1.5d，未改） | `8259cc8dd924ce5a51f09dacbfed36a02a18be9e6ad7f161c630dfaacef37caf` |
| `tool_eval.py`（未改） | `991db086c5c618be479b273b8af9db6151e77f3a334b14fa2a557089c184af04` |
| `db_snapshot_hash` | `8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925` |
| `schema_hash` | `712c0e34c37b6de140c13fcc7d3de15e67ac1f131f2bb6e8751a4f5e4df3474a`（`b1.5-tools/v3`） |
| `b1-5f-eval/resolver-cache.json` | `14c1a9cb05d1281462192d299186cee0ed3d4e3a26767a05ea606314ed2d48d5`（1,603 B） |
| 运行产物 | `blind-G1.json`、`blind-G1T-cold.json`、`blind-G1T-warm.json`、`blind-G1T-replay.json`（均 `annotation_guard = true`，`db_snapshot` 四份一致） |
| 时间线 | `run-timeline.txt`：`first_run_start=23:54:53Z`、`prewarm_done=23:55:08Z`、`cold_start=23:55:08Z`、`warm_start=23:55:17Z`、`replay_start=23:55:18Z` |
| 冻结记录 | `.pi/artifacts/b1-5f-freeze.json`（`tool_eval_runs = 0`、三 SHA、`plan_deviation`、`probes`、`checker.mutation_self_test`） |

## 附录 C — 验收条件对照（计划 §6）

| ID | 条件 | 状态 | 证据 |
| --- | --- | --- | --- |
| `B15f-1` | 30 条、10/10/10、配额与覆盖满足 | ✅ | checker 组 1–6；附录 A |
| `B15f-2` | `P-1`…`P-8` 全部命中、各带预声明口径；`P-4` 带预期失败声明 | ✅ | checker 组 13/14/16；§5 |
| `B15f-3` | 排除：135 条计分用例零重叠（718 id 并集）、29 个既有种子零重叠、无合成行/2020s | ✅ | checker 组 7–8、17 |
| `B15f-4` | 冻结 3 SHA + blob/mtime；首次运行**晚于**冻结；标注期 `tool_eval_runs = 0` | ✅ | §2.1、§2.2 |
| `B15f-5` | **主门槛**：`G1-T` 冷 ≥ 27/30；冷 2 调用、热 0、逐例一致；`P-4` 占 1 条容错 | ✅（已判定） | §3.1：**24/30 → FAIL**；§3.3；§3.1 第 4 条 |
| `B15f-6` | 分母 `/30`+`/28`×2+`/27` 成员列名 + Wilson；不得用 `/28`/`/27` 宣称 PASS | ✅ | §3.1、§3.2、§8.1 |
| `B15f-7` | 失败逐条归因；不以归因为由改规则 | ✅ | §4、§5；零规则改动（§9.3） |
| `B15f-8` | 零改动：`tool_eval.py`、别名 ×3、契约/`schema_hash`、`golden-set.schema.json`、既有 `check_*.py`（含 `8259cc8d…7caf`）、B1.5a–B1.5e 全部 hash | ✅ | §9.3 |
| `B15f-9` | 报告含复现命令、运行次数、model tag+digest、`prompt_sha256`、缓存 SHA、预热记录；**原句披露**"标注者知道探针形状"与"n = 30、单人标注"；结论边界 | ✅（含 §1.3/§8.4 的降级披露） | §1.3、§8.4、§9.1–§9.2 |
| `B15f-10` | demo 页存在、`demo/index.html` 指向最新、4173 局域网可达、只放聚合数字 | ✅ | §9.4（`curl` → 200；页面含 `24/30`/`FAIL`） |
| `B15f-11` | checker 变异自测 ≥5 个被捕获；`check_blind_holdout.py` 始终 byte-identical | ✅ | §2.2（8/8 被捕获，逐条 exit 1） |

## 附录 D — 本报告**不声称**的内容

- 不声称 `G1-T` 达标（**实测 24/30 = 80.0% < 27/30 → FAIL**），也不声称"接近达标"或"只差 3 条"。
- 不声称 B1.5e 已泛化（本批是首次在未参与规则开发的样本上测量，结果 = FAIL）。
- 不声称 `Q12` 例外已被复现或已被量化（`C18` 走的是 `clarify`，未行使 `Q12` 语义，§4.1）。
- 不声称中文解析、相似推荐或任何生产能力（解析器/执行器均为 **stand-in**，n = 2/30）。
- 不声称 `G0`/`G2`/`G3`/MCP 有任何结果（未测/未实现），也不据未测项推断。
- 不声称任何阈值、分母、用例集合或变体定义被调整（**均未调整**）；不声称 B1.5f 可被重跑后仍作盲测证据。
- 不声称 B-① 已开始或已交付；不声称 #74 可关闭；不声称 #81 已关闭（**报告完成，PR 待建**）。

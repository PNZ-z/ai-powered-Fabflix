# B1.5b — 结构化工具 / 工具调用路由评测报告（B 路线）

> **文档状态：🔴 实测完成（2026-11-19，**判分缺陷修复后重跑**）— G1 任务级成功率 62/75 = **82.7%**，**未达 ≥90% 门槛（FAIL）**；G0 52.0%、G2 50.7%、G3 **未测**；已按三路 review 修正 P0 判分缺陷并重跑（原 92.0%/PASS 作废），本报告随 PR 提交。**
>
> | 元数据 | 值 |
> | --- | --- |
> | 文档类型 | 评测报告（实测数字 + 结论；**不改计划正文、不改阈值、不改分母**） |
> | `last_updated` | **2026-11-19**（同日晚间修复判分缺陷后重跑；上一版 92.0%/PASS 已作废，见 §1.1） |
> | 所属 | issue [#72](https://github.com/PNZ-z/ai-powered-Fabflix/issues/72) / `docs/plans/b1-5b-tool-routing.md`（r7，已批准）；umbrella = `docs/plans/b1-5-movie-retrieval-eval.md` |
> | 判定口径 | **任务级成功率**（主，与 B1.5a 同一口径）；交付候选形态门槛 = **≥90%**（`OPEN-b1`，2026-11-10 主 agent 裁定）；schema 合规门槛 = **≥95%**（roadmap D8）；接地零容忍 |
> | 金标准集 | `docs/ai/golden/queries.b1-5.json`（60 条，`b1-5-golden/v1`，与 B1.5a 同一份、未改动）+ `docs/ai/golden/tool-cases.b1-5.json`（15 条）+ 路由标注 `docs/ai/golden/query-routing.b1-5.json`（60 条，additive） |
> | 数据快照 | Docker `fabflix-mysql` 宿主 3307 / `moviedb`，21,087 部（196–2024），`db_snapshot_hash = 8b16c047…` |
> | 端点 | `http://<host>:11434`（Ollama，局域网主机；按 `docs/ai/README.md` §3 不写具体地址），2026-11-19 实测 |
> | 脚本 | `scripts/eval/tool_eval.py`（stdlib-only，D-4；**本次运行所用版本含判分修复，随本 PR 提交**，见 §13 R-7）、`scripts/eval/check_schema.py`、`scripts/eval/check_golden.py`；CI 不依赖 |
> | 运行产物 | `.pi/artifacts/b1-5b-eval/{G0,G1,G2}.json`、`*.log`、`analysis.txt`（聚合脚本 `analysis.py` 为产物，不入库）；三份产物均为**同一次**修复后运行 |
> | 未做 | **G3（服务端渲染 + 一次重试）未测**；冷启动分组延迟未测；MCP server 未实现；**无任何生产代码 / 用户功能** |

---

## 0. 先说清楚三条边界

1. **本期没有交付任何用户功能。** 本报告只验证“同一份工具 schema 下，三种服务端路由形态各能拿到多少任务级成功率”。`src/main/**`、`pom.xml`、CI、frontend、仓库根 `README.md` 均未改动（本次只写文档：本报告 + `docs/ai/README.md` 的 B1.5b 状态行）；**MCP server 未实现**（D-6），也没有发布任何端点。
2. **G2 不是第二次模型采样。** G2 用 `--reuse-g0` **回放 G0 记录的模型工具调用**，只在服务端额外做 schema / 枚举校验，因此 `model_calls = 0`，其延迟数字是 G0 的**拷贝**而不是新样本。G2 的用途是隔离“服务端校验”这一个变量，不能当作“独立运行的第二次评测”。
3. **G1 的 82.7% 不是泛化估计。** G1 的确定性规则（拒绝词典、类型别名表、年代解析、种子消歧）是在**同一批 75 条 fixture** 上开发并调优的。因此它是**同集（in-sample）结果**，只能说明“这套形态在这批样本上拿到 82.7%”，**不能**推断对真实用户查询有 82.7% 的成功率。详见 §11.1。

---

## 1. 结论（先说判定）

### 1.1 三个变体的总判定

| 变体 | 形态 | 任务级成功率 | 路由正确 | schema 合规 | 模型调用 | 判定 |
| --- | --- | --- | --- | --- | --- | --- |
| **G0** | 纯 LLM 自选工具 + 自填参数（服务端只执行） | **39/75 = 52.0%** | 42/75 = 56.0% | 73/75 = 97.3% | **75** | ❌ **不可作为交付形态**（与 D-9 已批准结论一致） |
| **G1** | **确定性路由前置**（规则拒绝/意图分类 + 受控槽位 + 覆盖检查；LLM 不参与） | **62/75 = 82.7%** | 74/75 = 98.7% | 75/75 = 100% | **0** | ❌ **未达 ≥90% 门槛（FAIL：需要 ≥68/75 = 90.7%，实得 62/75，差 6 例 / 7.3pp）——同集结果，见 §11.1** |
| **G2** | G0 的调用 + **服务端 schema 校验 / 枚举映射**（回放，无新采样） | **38/75 = 50.7%** | 42/75 = 56.0% | 73/75 = 97.3% | **0**（回放） | ❌ **无改善**：相对 G0 净 −1 例 |
| **G3** | G2 + 服务端渲染字段 + 至多 1 次重试 | — | — | — | — | ⬜ **未测**（`OPEN-b4` 本期不要求） |

- **本版数字与上一版不同，原因是判分缺陷修复，不是换口径。** 上一版报告的 G1 = 69/75 = 92.0% / PASS 建立在 `scripts/eval/tool_eval.py` 的 query 分支把 `acceptable_movie_ids` 写死为空列表（`build_cases()`，原 L202）之上：60 条 query 的 `acceptable_hit` 因此恒为 `None`，任务级判定**从未检查 B1.5a 冻结的可接受集合**。修复后按 `query_id` 取 golden 集合（queries 文件未改动），重跑得到 62/75 = 82.7%，**G1 判 FAIL**。阈值（≥90%）、分母（75）、fixture 均未改动（§13 R-7）。
- 指纹：`schema_hash = 8e72f741…a567`（`b1.5-tools/v2`）、`db_snapshot_hash = 8b16c047…f925`、`golden_sha256 = 0c2f4c5e…be079`、`query-routing sha256 = bd862e96…53e5`、`tool-cases sha256 = 86fb98ba…b6a3`、模型 `qwen2.5:7b` digest `845dbda0…697e`、prompt `b1-5b-inline/v1` sha256 `58c8b708…2310`。三个变体**共用同一份 schema**。
- **没有调低阈值、没有换分母**：分母恒为 75（60 查询 + 15 工具用例），G3 未测不参与任何判定；修复前后 G0/G2 的总数（39/38）不变，只有 G1 因可接受集合开始参与判分而下降。

### 1.2 三条可交付的结论

1. **纯 LLM 路由不可交付（52.0%）**：失败集中在“没把该走的工具走完”（`incomplete_tool_sequence` 24 次）、“工具选错/槽位填错”（`wrong_tool` 13、`slot_mismatch` 12）和“执行了但没命中 golden 可接受集合”（`acceptable_set_miss` 24）。这不是靠 prompt 修补能解决的结构性问题——同一模型在 `temperature=0` 下单轮无重试，`similar_movies` **0/15**。
2. **确定性路由前置在路由层面几乎全对（74/75 = 98.7%），但任务级只有 82.7%，未跨 ≥90% 门槛**：拒绝 6/6、澄清 4/4、多槽位 5/5、筛选 19/20、片名 12/15、偏好推荐 9/10、相似 7/15。差值不在“选哪个工具”，而在**执行语义**（相似/推荐的执行器是评测 stand-in，§2.2）和**数据缺口**（中文译名、类型枚举变体）。且**不需要模型参与**（`model_calls = 0`）。
3. **只加“服务端 schema 校验”不够（50.7%，无改善）**：G2 把 2 例非法参数拒成 `INVALID_ARGUMENT` 后没有修复路径，其中 1 例（T10，2030 年）从“能解释覆盖范围”退化成“错误码死路”并新增 `coverage_confusion`。→ 要救 G0 这条路线，需要的是 G3 形态（服务端渲染 + 一次重试），**本期未测**，不能用 G2 的数字代指。

> **不得从本报告得出的结论**：B-① 已交付；G1 在真实用户查询上可达 82.7%；相似/推荐能力已实现（执行器是 stand-in，§2.2）；MCP 形态可行；G2 证明了“schema 校验无效”；G3 会/不会达门槛。

---

## 2. 方法与契约

### 2.1 样本（75 例 = 60 + 15，全部冻结）

| 来源文件 | 条数 | 说明 |
| --- | --- | --- |
| `docs/ai/golden/queries.b1-5.json` | **60** | B1.5a 冻结的同一份（`b1-5-golden/v1`），**文件未改动**，A/B 可按 `query_id` 配对；每条带 `acceptable_movie_ids` |
| `docs/ai/golden/query-routing.b1-5.json` | 60 | **additive** 路由标注：`intent` / `expected_tools[]` / `required_slots{}` / `accept_alternative_slots[]` / `result_constraints{}`（`b1-5-routing/v1`） |
| `docs/ai/golden/tool-cases.b1-5.json` | **15** | 拒绝 6（T01–T06）/ 澄清 4（T07–T10）/ 多槽位 5（T11–T15） |
| 合计分母 | **75** | 三个变体一致 |

- **可接受集合是判分的一部分，不是注释。** 60 条 query 的 `acceptable_movie_ids` 全部非空（修复后由 `tool_eval.py` 读取；缺失即直接报错，不再静默按 `None` 通过）。15 条工具用例中只有 T12 带集合，其余保持原有判定。

### 2.2 三个变体的服务端行为（同一份 schema，差异只在服务端）

| 变体 | 谁决定工具与参数 | 服务端额外动作 | 模型调用 |
| --- | --- | --- | --- |
| **G0** | 模型（Ollama `/api/chat` + `tools`，`temperature=0`，单轮，无重试） | 只执行工具 + 记录轨迹 | 每条 1 次（75） |
| **G1** | **脚本内的确定性路由器**：S1 规则拒绝（剧情/交易/内部文档/外部站点）→ S2 槽位抽取（中文类型词 → enum 别名、年代/年份 → 整数、"高分" → `min_rating`、片名 → `search_movies_by_title` 解析）→ S3 覆盖检查（196–2024 / 评分 2001+）→ S4 直接调工具或产出一次澄清 | 覆盖检查产出 `OUT_OF_RANGE` / `EMPTY_RESULT` + `coverage`；种子片名歧义时列候选 + 一个问题 | **0** |
| **G2** | **回放 G0 的模型调用**（`--reuse-g0 .pi/artifacts/b1-5b-eval/G0.json`） | schema 校验 + 枚举/别名映射（`genres` 中文 → enum；非法/越界参数 → `INVALID_ARGUMENT`，无重试、无修复） | **0（回放）** |
| **G3** | — | — | **未测** |

- **`find_similar_movies` 与 `recommend_movies` 的执行是评测替身（evaluation stand-in），不是生产契约。** 两个工具没有生产实现，本评测为了让 15 条 `similar_movies` 和 10 条 `recommend_by_preference` 能被计分，才在脚本里放了一个确定性替身：
  - `find_similar_movies`：候选池 = 与 seed **至少共享 1 个类型**的行，排序键 =（共享类型数 desc, |年份差| asc, 评分 desc），排除 seed；查询里字面表述的类型/年份/评分再作为结果约束在服务端过滤。
  - `recommend_movies`：按调用携带的偏好过滤（**多类型是 OR**：`id IN (SELECT … WHERE g.name IN (...))`）、按评分降序，排除 seed（若给出）。
  - 因此 **`similar_movies` 7/15、`recommend_by_preference` 9/10 的通过率只代表“在替身执行器下能命中 golden 可接受集合”，不代表生产相似/推荐信号（例如 golden 使用的“同导演”）已被实现**。生产排序属 `OPEN-b5`，本评测不落任何生产代码。
- G1 的规则集合包含：拒绝词典（`plot` / `transaction` / `internal` / `external`）、**17 组类型目标 / 43 个中英词形**的别名表（`科幻 → Sci-Fi` 等）、年代词（五十…九十、二十）、覆盖检查、`SIMILAR_MARKERS` / `RECOMMEND_MARKERS` 意图词、种子解析后 `find_similar_movies` 跟随调用。
- **G1 不调用 LLM**：它的答案是服务端模板串（如 `[server-rendered 0 rows]`、`[refusal: plot]`），所以本报告不对 G1 的“话术质量”下任何结论（§11.3）。

### 2.3 单条用例的判定口径（与 B1.5a 同一任务级口径）

| 指标 | 定义 |
| --- | --- |
| **任务级成功（主）** | 查询/工具用例：同时满足 ① 工具选择与槽位、schema、行为全过；② 展示行 ≥1；③ 接地 OK；④ 无结果约束违规；⑤ 可接受集合（`acceptable_movie_ids`）至少 1 项命中。拒绝类：行为正确且**未调工具**；澄清类：行为正确且无 schema 问题；覆盖解释类：`error_state` == 预期错误码 |
| 路由正确 | 实际调用工具 ⊆ `expected_tools` 且覆盖全部 `expected_tools` |
| 槽位 exact-match | 与 `required_slots`（或 `accept_alternative_slots` 中的一组）逐字段相等 |
| schema 合规 | 模型/路由器给出的参数能通过 `tool-schema.b1-5.json` 校验（含 `enum` / `minimum` / `maximum` / `additionalProperties` / `anyOf`） |
| 行为正确 | 拒绝类不得调工具；澄清类允许"问一个问题"或 `EMPTY_RESULT`/`OUT_OF_RANGE` 解释；其余按 `tools_ok` |
| **接地零容忍** | 每个展示字段（id/title/year/director/genres/stars/rating）回查 DB 比对；任一不符即该条失败 |
| 结果约束 | 只有**在查询文本里被字面表述**的约束才计分（例如 Q36 的 "Michael Mann"）；golden 里标注但查询文本没说的同导演约束记为 `unscored` |

> **口径边界 1（必须知道）**：`result_constraints_checked` 三个变体都只有 **8** 例，因为 "similar" 类的同导演规则多数**没有**在查询文本里出现（Q14/Q15/Q33/Q34/Q37/Q53/Q54/Q55 等的 `year_from`/`genres`/`director` 标注被记为 `unscored`）。因此 `similar_movies` 的通过与否**主要由“可接受集合是否命中 + 是否排除 seed”决定**，不是“同导演规则是否被严格执行”。这是评测口径缺口（§13 R-3）。
>
> **口径边界 2（判分循环）**：“某个约束是否被查询文本表述”与“该约束是否被满足”用的是**同一套正则**（`text_constraints()` 抽取，`constraint_problems()` 校验，`tool_eval.py`），属于**自洽但未独立复核**的判定：正则认不出的表述就不计分，正则认错的表述又会被同一套规则判为满足。只有 8 例参与计分，且**没有第二人/独立标注复核对这 8 例的“文本是否表述”判定**；因此 `result_constraints_ok` 只能作为自洽性证据，不能当作独立验证过的语义正确性结论。
>
> **口径边界 3（澄清类只验形态）**：`ask_one_clarifying_question` 的判定只检查“**是否只问一个问题 / 未调用工具 / 无 schema 问题**”这一行为形态，**不检查提问内容**——问的是不是关键歧义点（片名歧义？年份？导演？）完全没有被判分。所以“澄清 4/4”只表示形态正确。

### 2.4 判定产物

每条用例记录 `tool_calls`（含参数）、`execution`（`status` / `results` / `applied_filters` / `coverage` / `match`）、`tools_ok` / `tools_complete` / `slots_ok` / `schema_ok` / `behavior_ok` / `grounding_ok` / `seed_excluded` / `result_constraint_*` / `acceptable_hit` / `failure_modes[]` / `task_success`。聚合表见 `.pi/artifacts/b1-5b-eval/analysis.txt`。

---

## 3. 复现指纹

| 项 | 值 |
| --- | --- |
| `contract_version` | `b1.5-tools/v2` |
| `schema_hash` | `8e72f74120e43c5f4a40b1f8815d952d0bf029338a94e328047c60907fd8a567` |
| `template_hash` | **N/A**（本路线不使用卡片模板；卡片字段来自 DB 行，见 `movie-cards.md`） |
| `db_snapshot_hash` | `8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925` |
| 快照事实 | 21,087 部 / 年份 **196–2024** / `genres` **61 行 56 distinct** / **13,082 部无评分** / 评分只覆盖 **2001+**（`MIN(year)` among rated = 2001） |
| `golden_version` / `queries sha256` | `b1-5-golden/v1` / `0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079`（**未改动**，`check_schema.py` 的 `annotates.sha256` 绑定校验通过） |
| `routing_version` / `query-routing sha256` | `b1-5-routing/v1` / `bd862e967015c01d8d2dd8ccd0f51369b51eaff990f36fcb474b616f695253e5` |
| `tool-cases sha256` | `86fb98ba126176991ff32c4f6ef57d60426ecad53fd117b5933133d94677b6a3` |
| 模型 / digest | `qwen2.5:7b` / `845dbda0ea48ed749caafd9e6037047aa19acfcfd82e704d7ca97d631a0b697e`（Q4_K_M，capabilities 含 `tools`） |
| 端点 / `temperature` | `http://<host>:11434` / `0` |
| prompt | `b1-5b-inline/v1`，sha256 `58c8b708d919920cf85becce7594bfc4ffa7795599bc923151e86ba9e10e2310`（**不是** `ai-user-workflows.md` §1.4 定义的 `runtime-prompt/v1`，见 §4.2） |
| G2 复用来源 | `reused_from = .pi/artifacts/b1-5b-eval/G0.json`（同一份 G0 轨迹） |
| fixture 自检 | `python3 scripts/eval/check_schema.py` 与 `python3 scripts/eval/check_golden.py`（含 DB 段）在修复后重跑：`schema_hash` / `queries sha256` / `tool-cases sha256` / `routing sha256` / `db_snapshot_hash` 全部与上表一致 → **fixture 未变** |

---

## 4. 端点、模型与 prompt（实测）

### 4.1 模型侧实测

| 项 | G0（75 次调用） |
| --- | --- |
| `prompt_eval_count` | min **1,265** / median **1,272** / max **1,284** tokens（含 system prompt + 5 工具 schema + 用户问题） |
| `eval_count` | min 16 / median 35 / max 80 tokens |
| 模型工具调用总数 | **71** 次（75 例中有 4 例未发起任何工具调用：T01/T03/T04/T05） |
| 实际调用过 `find_similar_movies` 的 similar 用例 | **1/15**（只有 Q12） |

### 4.2 prompt 事实（与规范的关系）

- 运行用 prompt = `PROMPT_ID = b1-5b-inline/v1`，正文是**两句英文指令**（"只从工具结果作答、不编造字段、用用户语言回答"），工具契约通过 Ollama 的 `tools` 字段传入，不在 prompt 里重写。
- **它与 `ai-user-workflows.md` §1.4 的 `runtime-prompt/v1` 不是同一个产物**：§1.4 描述的是"装配进 system message 的 compact prompt 产物"，标注为独立版本；本次实测用的是评测脚手架内的 inline prompt（id 已改名以表明这一点），记录的是 `prompt_sha256` 而不只是 id。
- **预算偏差（实测）**：`prompt_eval_count` 1,265–1,284 tokens，高于 §1.4 的估算目标（指令 ≤350 + schema ~500，总目标 ≤900）。差异主要来自 5 工具 schema 的实际体积。`WU-8`（`runtime-prompt/v1` 的内容与 token 数由谁定稿）**仍待定稿**，本报告不改 prompt。

---

## 5. 主表

指纹见 §3。全部数字为 2026-11-19（判分修复后）单次运行、`temperature=0`、单轮、无重试；G2 为同一次 G0 的回放。

### 5.1 总量

| 变体 | cases | 任务级 | 路由 | tools_ok | tools_complete | slots_ok | schema_ok | behavior_ok | 接地 | model_calls | p50 | p95 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| G0 | 75 | **39 (52.0%)** | 42 (56.0%) | 62 | 47 | 63 | 73 (97.3%) | 62 | **75/75** | 75 | **534.2 ms** | **931.4 ms** |
| G1 | 75 | **62 (82.7%)** | 74 (98.7%) | 75 | 74 | 75 | 75 (100%) | 75 | **75/75** | **0** | — | — |
| G2 | 75 | **38 (50.7%)** | 42 (56.0%) | 62 | 47 | 63 | 73 (97.3%) | 61 | **75/75** | **0（回放）** | 534.2 ms¹ | 931.4 ms¹ |

¹ G2 的延迟是 G0 样本的回放拷贝，**不是新测**，不得当作独立延迟测量。

### 5.2 分意图（任务级成功）

| 意图 | 条数 | G0 | G1 | G2 |
| --- | --- | --- | --- | --- |
| `refusal` | 6 | 4 (66.7%) | **6 (100%)** | 4 (66.7%) |
| `clarification` | 4 | 2 (50.0%) | **4 (100%)** | 1 (25.0%) |
| `multi_slot` | 5 | 3 (60.0%) | **5 (100%)** | 3 (60.0%) |
| `search_by_title` | 15 | 11 (73.3%) | **12 (80.0%)** | 11 (73.3%) |
| `filter_catalog` | 20 | 14 (70.0%) | **19 (95.0%)** | 14 (70.0%) |
| `recommend_by_preference` | 10 | 5 (50.0%) | **9 (90.0%)** | 5 (50.0%) |
| `similar_movies` | 15 | **0 (0%)** | **7 (46.7%)** | **0 (0%)** |

### 5.3 分 kind（查询类 60 / 工具用例类 15）

| kind | G0 | G1 | G2 |
| --- | --- | --- | --- |
| `query`（60） | 30 (50.0%) | **47 (78.3%)** | 30 (50.0%) |
| `tool`（15） | 9 (60.0%) | **15 (100%)** | 8 (53.3%) |

> 读法：G1 相对 G0 的增益**不是**均匀的，主要来自三处结构性修正——① 拒绝/澄清不再越界（`under_refusal`/`no_clarification` 清零）；② `similar` 从 0/15 到 **7/15**（先解析片名再跟随 `find_similar_movies`，但执行器是共享类型/年份排序的**替身**，§2.2，未命中 golden 的“同导演”集合）；③ 中文类型词不再被当成片名（`科幻`/`动画`/`犯罪` 等不再走 `search_movies_by_title`）。片名类只 +1（Q03），因为译名问题（Q21–Q23）与路由无关。**路由层面 74/75 与任务级 62/75 的落差不来自“选错工具”，而来自执行语义与数据缺口。**

---

## 6. 失败模式

| 失败模式 | G0 | G1 | G2 | 说明 |
| --- | --- | --- | --- | --- |
| `incomplete_tool_sequence` | **24** | 1 | **24** | 少调/漏调工具（G0 的 14 例 similar 只调了 `search_movies_by_title`） |
| `wrong_tool` | **13** | 0 | **13** | 该筛选却走片名搜索，或该推荐却走筛选 |
| `slot_mismatch` | **12** | 0 | **12** | 槽位缺失或取值与标注不符 |
| `empty_result` | 10 | 4 | 11 | 调用了工具但**展示 0 行**（含 `EMPTY_RESULT` / `NOT_FOUND` / `OK` 空集，不要求特定错误态） |
| `result_constraint_violation` | 4 | 1 | 3 | 展示结果违反查询里字面表述的约束 |
| `under_refusal` | 2 | 0 | 2 | 该拒绝却调了工具（T02、T06） |
| `unrequested_tool` | 2 | 0 | 2 | — |
| `no_clarification` | 2 | 0 | 2 | 该澄清却直接出结果（T07、T08） |
| `bad_args` | 2 | 0 | 2 | 参数 schema 不合规（T10 年份 2030、Q03 `year: null`） |
| `coverage_confusion` | 0 | 0 | **1** | 把"超出覆盖"说成别的（G2 的 T10） |
| `acceptable_set_miss` | **24** | **13** | **25** | 有展示行（或无行）但**未命中 golden 可接受集合**；判分修复后开始生效，是本版与上一版最大的差异来源 |
| `over_refusal` | 0 | 0 | 0 | G1 6/6 拒绝用例全部正确，无过度拒绝 |
| 接地违规（编造 id/title/rating） | **0** | **0** | **0** | `hallucinated ids = {}`，三变体接地 75/75 |

> `acceptable_set_miss` 与其它模式可叠加（例如 Q12 同时是 `empty_result` + `acceptable_set_miss`），因此计数不等于失败用例数：G0 36 例失败、G1 13 例失败、G2 37 例失败（= 75 − 任务级成功数）。

---

## 7. G1 的 13 个失败（逐条原因）

G1 = 62/75，失败 **13** 条，全部有具体机制，**没有一条是"随机性"**（`model_calls = 0`，结果完全确定）。**其中 8 条由评测替身执行器造成（§2.2），4 条由数据/枚举缺口造成，1 条是“澄清 vs 任务成功”的口径冲突。**

| 用例 | 查询 | 现象 | 原因（证据） | 最小修复指向 |
| --- | --- | --- | --- | --- |
| **Q12** | 和《Pulp Fiction》相似的电影 | seed `QTa3` 解析正确，5 行 (`BS8` Get Shorty / `WhR10` Handgun / `JiA18` Hot Shots! Part Deux / `ADg10` The Moneytree / `JiA15` Hot Shots!) 全部不在可接受集合 | 替身按“共享类型 + 年份接近”排序；golden 规则是**同导演**（Tarantino 名下 5 部） | 生产相似信号（`OPEN-b5`）；评测层面只能如实标注“替身不实现同导演” |
| **Q16** | 喜欢《Back to the Future》这种科幻片 | seed `RZe5` 正确，5 行 1985 年科幻（Cocoon / Explorers / …）非 Zemeckis | 同 Q12（替身） | 同 Q12 |
| **Q19** | 推荐几部 1990 年代的喜剧动作片 | 5 行（`` `Mo' Better Blues `` / 10 Things I Hate About You / 20 Bucks / 200 Cigarettes / A Life Less Ordinary）不在可接受集合 | 替身的 `preferred_genres` 是 **OR**（`genres IN (...)`），golden 要求 **AND**（同时含 Comedy 与 Action）；槽位与路由本身正确（`slots_ok = True`、`tools_complete = True`） | 需在契约里定义多值类型语义（§13 R-1）后统一替身与生产实现 |
| **Q35** | 想找像《Fargo》那样的片子 | seed `JlC16` 正确，5 行 1996 年其它片非 J.Coen | 替身（同 Q12） | 同 Q12 |
| **Q36** | 像《Heat》一样，还想看 Michael Mann 的其他片子 | 种子解析成 `PMo15`（Heat **1972**），5 行 1972 年成人片；`result_constraint_violation = 1` + 可接受集未命中 | **三个叠加缺陷**：① `search_movies_by_title("Heat", limit=10)` 的截断里精确同名行只有 `PMo15`，真正的 `MM33`（1995）在截断外 → 路由器把唯一候选当确定种子；② 用户给的 `Michael Mann` 与库内 `M.Mann` 无别名匹配；③ 替身排序不实现同导演 | 种子解析需“多候选 + 可用线索”（R2-3 扩展）；人名缩写属 `OPEN-b3` |
| **Q37** | 喜欢《Toy Story》这种动画喜剧，有类似的吗 | seed `ILt15` 正确，5 行 1995–96 动画（Caspar / Pocahontas / Dragonheart …）不在可接受集合 | 替身（同 Q12）；golden 是“共享 Comedy+Animation 的 1990s 四部” | 同 Q12 |
| **Q55** | I love Blue Velvet - anything similar? | seed `DLy8` 正确，5 行 2001 年无关片 | 替身（同 Q12）；golden 是同导演 7 部 | 同 Q12 |
| **Q56** | more films like Before Sunrise | seed `RLl15` 正确，5 行 1995 年爱情片 | 替身（同 Q12）；golden 是同导演 4 部 | 同 Q12 |
| **Q11** | 2020 年以后的科幻片 | `genres:[Sci-Fi], year_from:2020` → `EMPTY_RESULT` 0 行 | 2020–2024 的两条可接受行（`tt9000001` Neon Horizon 2021 / `tt9000002` Midnight Circuit 2022）在 `genres` 里写作 **`Science Fiction`**（全网仅 2 部），而 `科幻 → Sci-Fi`（457 部）的别名映射没有覆盖这个语义变体 | 服务端把 `Sci-Fi`/`Science Fiction` 视为同一变体（`OPEN-b3`）；同时须先定义多值 `genres[]` 的 AND/OR 语义（§13 R-1） |
| **Q21** | 找一下《黑客帝国》 | `search_movies_by_title("黑客帝国")` → 0 行 | 目录只有英文原名 `The Matrix`（`AWw10`），**无中文译名别名表**；golden 已标 `cjk_alias_only: true`（预期失败样本） | 别名表属新数据来源 + 许可，须另立 issue（`WU-9`）；不得用"用户会给英文名"回避 |
| **Q22** | 《阿甘正传》是哪部电影 | 同上，0 行 | 同上（`Forrest Gump` / `RZe19`） | 同 Q21 |
| **Q23** | 我想看《泰坦尼克号》 | 同上，0 行 | 同上（`Titanic` 1997 / `JCm28`；另有 1953 版不计入可接受集合） | 同 Q21 |
| **Q57** | I want more movies like The Shining | 只调了 `search_movies_by_title`，列出 2 个候选 + 一个问题，**没调** `find_similar_movies` → `incomplete_tool_sequence` + `acceptable_set_miss` | `The Shining` 有两行（`StK15` 1979 / Kubrick、`MkG15` 1997 / Garris）；路由器按 R2-3 **正确地**选择澄清而不猜，但 golden 的可接受集合是 Kubrick 名下 12 部 → 任务级判失败 | 属"行为正确但任务失败"的用例：要么给消歧线索（年份/导演），要么把该条标注为"允许澄清"（属 golden/判分口径决策，不在本报告裁定范围） |

> 归因小结：**13 条里 8 条（Q12/Q16/Q19/Q35/Q36/Q37/Q55/Q56）的失分直接来自“执行器是评测替身”，而不是 G1 的路由规则；3 条（Q21–Q23）是译名别名固有缺口；1 条（Q11）是枚举语义变体；1 条（Q57）是“澄清 vs 任务成功”的口径冲突。** 换言之，G1 的 82.7% 既受数据缺口限制，也受替身执行器限制；**但两者都不能作为“换成生产实现就能到 90%”的证据**——生产相似/推荐尚未实现（`OPEN-b5`），本报告不下此类推断。

---

## 8. G0 的失败结构（为什么 52%）

- **`similar_movies` 0/15**：15 例里 **14 例只调了 `search_movies_by_title`** 就结束（`incomplete_tool_sequence`），唯一调用 `find_similar_movies` 的是 Q12（`movie_id` 直接传了片名 `"Pulp Fiction"` → `NOT_FOUND` + `slot_mismatch`）。根因是模型不知道"先解析片名再跟随相似工具"这个两步序列，也无法自己产出 `movie_id`。
- **`filter_catalog` 14/20**：6 例失败，其中 3 例把筛选请求做成了片名搜索（Q06、Q07、Q30 典型：`Animation` / `Comedy,Romance` 被当成 title query，`genres`/`year_from`/`year_to` 全为 `null`），另 3 例槽位/可接受集问题（Q11、Q32、Q48）。
- **`recommend_by_preference` 5/10**：5 例失败全部 `wrong_tool + incomplete_tool_sequence + slot_mismatch`（Q18/Q19/Q20/Q38/Q39，把偏好推荐做成 `filter_movies`）。
- **拒绝 4/6**：T02（"帮我买两张《Toy Story》的票"，交易类）、T06（"《The Matrix》的豆瓣评分是多少"，外部站点类）→ `under_refusal`，模型直接去搜片名。
- **澄清 2/4**：T07（"推荐点好看的"）直接推荐、T08（"找那部叫 Matrix 的"）直接出结果，都缺一次澄清。
- **可接受集合未命中 24 例**：集中在 similar（15 例全部未命中，其中 Q36 返回的 `MM33` 恰好是 seed 本身，golden 要求排除 seed）与“选错工具导致 0 行”的筛选/片名用例；`acceptable_set_miss` 是修复后新增的可见失分，上一版报告里这些用例被记成“通过”。
- **唯一被模型稳定做对的**：单槽位片名搜索（11/15）与"合法且可满足"的单条件筛选。

---

## 9. G2 为什么没有改善（50.7%）

G2 只在 G0 之上加"schema 校验 + 枚举映射"，回放同一批模型调用：

| 用例 | G0 | G2 | 变化 |
| --- | --- | --- | --- |
| **T10**（2030 年以后的科幻片） | 任务 ✅（`OUT_OF_RANGE` + `coverage`，但同时 schema 不合规：`year_from 2030 > maximum 2024`） | 任务 ❌（`INVALID_ARGUMENT`，无 `coverage`） | **pass → fail**，新增 `coverage_confusion` |
| Q03（"Before 开头 + 爱情片"） | 失败（`bad_args` + `result_constraint_violation`） | 失败（`bad_args` + `acceptable_set_miss` + `empty_result`） | 失败模式换了（0 行），仍失败 |
| 其余 73 例 | — | — | 完全相同 |

- **净效果 −1 例（39 → 38）**：G2 拒掉了 2 例非法参数（T10 的 2030、Q03 的 `year: null`），但**没有修复/重试路径**，于是把"能给出覆盖范围解释"的那一例变成了硬错误。24 例 `incomplete_tool_sequence`、13 例 `wrong_tool` 一个都没治好——**schema 校验治的是"参数形状"，不是"路由选择"**。G2 的 `acceptable_set_miss` 为 25（= G0 的 24 例 + Q03 被拒后 0 行）。
- **契约冲突（实测暴露）**：fixture 的 `year_from.maximum = 2024` 让"2030 年"在 schema 层就是非法值 → 服务端校验只能回 `INVALID_ARGUMENT`，而 `ai-user-workflows.md` §3.1 要求"合法数值但超出覆盖 → `OUT_OF_RANGE` + `coverage`"。两者**不能同时满足**，必须由计划/契约层裁定（§13 R-2）。本次**未改 fixture**。
- **结论边界**：G2 的结果**不能**用来评价 G3。G3 的服务端渲染 + 一次重试若实现，才有资格讨论"这条路线能否到 90%"。本期 G3 = **未测**。

---

## 10. 边界事实（接地 / seed / 空值 / 别名 / 延迟）

| 项 | 实测 |
| --- | --- |
| **接地零容忍** | 三变体 **75/75**；`hallucinated ids = {}`；模型从未编造 title/rating/id，也未把字段改写/翻译（G0 的失败全部发生在"没走对工具"，不是"编造内容"） |
| **`rating: null` 不得转 0** | 展示行中 `rating` 为 `null` 的有 **133** 行（如 T08 `AWw10`、T12 `SBa15/DPs13/TiB17/ICa20`），全部保持 `null`，无 0 替代 |
| **0 genre 行** | 展示行中 `genres: []` 的有 **3** 行（Q26 `JlC18`、Q44 `RZe22`、Q57 `MkG15`），未跳字段、未编造类型 |
| **seed 排除（T14）** | G1 T14：seed = `tt0482571`《The Prestige》，`seed_excluded = True`，展示 5 行 `tt0212712 / tt0399361 / tt0379786 / tt0289879 / tt0447160` **不含 seed** → §4.2 的"`results[]` 必须排除 seed"实测成立 |
| seed 排除（全量） | 三变体 `seed_excluded` 全部为 `True`（0 例违规）。G1 的 15 例 similar 中 **14 例**调用了 `find_similar_movies`（G0 只有 1 例）；**Q57 未调用**（按消歧规则走澄清路径，无相似结果行，见 §7） |
| **别名缺口** | 中文片名 Q21–Q23 全 0 命中（`cjk_alias_only`）；人名 `Michael Mann` vs 库内 `M.Mann`（Q36）；类型变体 `Sci-Fi`(457) vs `Science Fiction`(2)（Q11）——**三类别名缺口都已实测复现**，且都在本期解决范围外（`OPEN-b3` / `WU-9`） |
| **延迟（G0）** | 75 个样本：**p50 534.2 ms / p95 931.4 ms**，min **263.3 ms** / max **5,807.7 ms**（最大值是本次运行的**第 1 次调用** T01，含模型装载；其余样本最大值 1,720.5 ms。热态，含一次 chat + 一次工具执行） |
| **延迟（G1）** | **未测**：`model_calls = 0`，脚本未记录端到端/DB 单独耗时（`latency_samples = 0`）→ 不能声称"G1 更快"，只能说"没有模型侧调用" |
| **冷启动** | **未测**：`keep_alive:0` 探针脚本（`cold_start_probe.py`，n=5）存在但**没有产生输出记录**，且 n=5 低于 T8 要求的每组 ≥20 → 本报告不报冷启动数字 |

---

## 11. 未测项与不可外推的边界

### 11.1 G1 的 82.7% 是同集结果，不是泛化估计（**最关键的 caveat**）

- G1 的拒绝词典、类型别名表、年代解析规则、`SIMILAR_MARKERS`/`RECOMMEND_MARKERS` 词表、种子解析策略，都是**在开发过程中针对这 75 条 fixture 反复调整**得到的（例如 `剧情是什么` 必须区别于 `剧情片`、`类似/像/那种` 的覆盖、`Heat` 的截断行为）。
- 因此 82.7% 是 **in-sample** 数字。它**能**说明：在这套样本上，"确定性路由 + 受控槽位"这种形态的路由层几乎全对（74/75），任务级上限受执行语义（替身）与数据缺口（别名/枚举变体）限制。它**不能**说明：真实用户查询上也有 82.7%（真实查询的口语化程度、隐含槽位、未收录类型词/缩写远多于 fixture）。
- **B-① 的生产实现必须用独立验证集或真实用户查询重新复测**，并且不得用本数字作为"B-① 已交付"或"B-① 将达 82.7%"的依据。`docs/plans/b1-5b-tool-routing.md` §6 的门槛检查在本形态本样本上**未通过（FAIL）**。
- 同理，G0 的 52% 也是单次、单模型（`qwen2.5:7b`）、单 prompt（`b1-5b-inline/v1`）的实测，不能外推为"任何 LLM 都只有 52%"。

### 11.2 G3 未测

- G3（服务端渲染字段 + 至多 1 次重试）**本期未实现、未运行**，`OPEN-b4` 已裁定为可选/不要求。因此"G2 之上的修复路径是否有效"= 未知；不得用 G2 的 50.7% 代替。

### 11.3 G1 的答案质量未评

- G1 的 `answer` 是服务端模板串（`[refusal: plot]`、`[server-rendered 0 rows]`、`[seed unresolved: …]`），**没有任何模型生成的自然语言**。因此本报告的"拒绝/澄清正确"只表示**行为**正确（不调工具 / 问一个问题 / 给 `coverage`），**不表示**话术可用，也**不表示提问内容正确**。
- **`ask_one_clarifying_question` 只验形态**：判定条件为“未调用工具 / 行为标记为 `clarify` / 无 schema 问题”，**提问是否命中真正的歧义点（片名多版本？年份？导演？）没有判分**（§2.3 口径边界 3）。所以“澄清 4/4”不能读成“澄清问得对”。
- 另：G1 模板串里的行数是渲染时的计数，可能与最终展示/计分行数不一致（例如 Q14、Q36 的 `[server-rendered 10 rows]` 对应 5 行展示），**不作为可用性证据**。

### 11.4 MCP 与本期边界

- **MCP server 未实现**（D-6）：没有启动过 `/sse`、`/mcp`，也没有发布任何端点；MCP 的鉴权缺口（`/mcp`、`/sse` 默认匿名可达，`CustomerAuthenticationFilter` 只护 `/api/boot/**`）**不在本期范围**，将来若引入必须另立安全验收 issue。
- 无生产代码、无 `src/main/**` 改动、无 DB 写入、无 CI 依赖；评测只读 DB。

---

## 12. 验收条件对照（B15b）

| ID | 条件 | 状态 | 证据 / 缺口 |
| --- | --- | --- | --- |
| **B15b-1** | schema fixture 存在且满足 `enum` 由 DB 派生、`required`/**`anyOf`**、`additionalProperties:false`、`min`/`max`；`schema_hash` 已记录 | ✅ 已满足 | `docs/ai/tool-schema.b1-5.json`（v2，`schema_hash 8e72f741…`）+ `scripts/eval/check_schema.py` |
| **B15b-2** | 60 + 15 条已版本化冻结；正样本在快照内可命中 | ✅ 已满足 | `queries` `0c2f4c5e…`（未改动）+ `query-routing` `bd862e96…` + `tool-cases` `86fb98ba…`；每条正样本带 `snapshot_row_refs` |
| **B15b-3** | G0/G1/G2 均已跑、共用同一 `schema_hash`；G3 未做须标"未测" | ✅ 已满足 | §1.1、§3；G3 = 未测（§11.2） |
| **B15b-4** | 任务级 + 意图/工具/槽位/接地/拒绝/延迟/失败模式全部记录；G0 的 7/12 基线与 G2 并列 | 🟡 部分 | 全部指标已记录（§5–§10）。**7/12 = 58% 是 scout 阶段 12 条手工用例的独立基线，口径与本次 75 条 fixture 不同，未做子集复评**；仅作方向对照（52% vs 58%），不声称可严格比较 |
| **B15b-5** | `INVALID_ARGUMENT` / `OUT_OF_RANGE` / `EMPTY_RESULT` 三态互斥并验证 | 🟡 部分 | `OUT_OF_RANGE`（T10/G1）、`EMPTY_RESULT` + `coverage`（T09/Q11/G1）实测成立；**但 fixture `year_from.maximum=2024` 与 §3.1 冲突**，G2 把 2030 判成 `INVALID_ARGUMENT`（§9、§13 R-2）。契约需裁定 |
| **B15b-6** | 接地零容忍 | ✅ 已满足 | 三变体 75/75；`hallucinated ids = {}`（§10） |
| **B15b-7** | 拒绝 6/6；`under_refusal`/`over_refusal` 分别统计 | ✅ 已满足 | G1 6/6，`over_refusal = 0`；G0/G2 4/6、`under_refusal = 2`（T02、T06） |
| **B15b-8** | 路由形态结论 + 确定性路由形态的实测数字 | ✅ 数字已给出（**结论为 FAIL**） | §1.2：纯 LLM 不可交付（52.0%）；提议形态 = 确定性路由 + 受控填充，**82.7%（同集）< 90% 门槛 → 未达标**；带 §11.1 caveat |
| **B15b-9** | 声明未实现 MCP、未发布端点；`schema_hash`/`db_snapshot_hash` 已记录；`template_hash = N/A` | ✅ 已满足 | §0.1、§3、§11.4 |
| **B15b-10** | 如实区分已实测 / 回放 / 未测 | ✅ 已满足 | §0、§5.1 脚注、§10、§11 |

---

## 13. 未决与风险（供主 agent / oracle 裁定）

| ID | 问题 | 证据 | 影响 |
| --- | --- | --- | --- |
| **R-1** | **多值 `genres[]` 的 AND/OR 语义未在契约中定义**。评测执行器 `filter_movies` 按 **AND** 实现（`HAVING COUNT(DISTINCT g.name) = len(genres)`），`recommend_movies` 替身按 **OR** 实现（`g.name IN (...)`），而 Q11 的 golden 谓词是 **OR**（两条可接受行分别落在 `Sci-Fi` / `Science Fiction` 名下）、Q19 的 golden 是 **AND**。 | G0/G2 的 Q11 提交 `["Sci-Fi","Science Fiction"]` → 0 行；G1 提交 `["Sci-Fi"]` → 0 行；G1 Q19 的 OR 结果全部落在可接受集合外 | 影响所有"语义变体类型词"与多类型用例；`ai-user-workflows.md` §4.2 目前只写取值域，未写多值语义 |
| **R-2** | **fixture `year_from/year_to` 的 `maximum/minimum = 2024/196` 与 §3.1「超出覆盖 → `OUT_OF_RANGE` + `coverage`」互相排斥**：schema 层先拒，就不可能产出 `OUT_OF_RANGE`。 | G2 T10：2030 → `INVALID_ARGUMENT`，新增 `coverage_confusion` | 决定 B15b-5 能否"完全满足"；修法（放宽 schema 边界让服务端判覆盖 / 或在 schema 之外做覆盖检查）属契约决策，本次未改 fixture |
| **R-3** | **`result_constraints` 只有 8 例被计分，且"文本是否表述"与"是否满足"由同一套正则判定（判分循环）**：similar 类的同导演规则多在 golden 里标注但查询文本未表述 → `unscored`；被计分的 8 例也没有独立复核。 | 三变体 `result_constraints_checked = 8`，G0 4/8、G1 7/8、G2 5/8（G1 唯一违规是 Q36） | `similar` 的 7/15 主要是"替身排序下命中可接受集合 + 排除 seed"，**不是**"同导演规则被执行"；若要更强结论需独立标注复核 + 改判分口径（属评测设计变更） |
| **R-4** | **Q57 这类"澄清是正确行为但任务级判失败"** 的用例与 R2-3 冲突。 | G1 Q57 `behavior_ok = True`、`task_success = False` | 会系统性压低"依法澄清"形态的分数；是否给该条单独的判定口径属计划层决策 |
| **R-5** | **脚本版本**：本次运行用的 `scripts/eval/tool_eval.py` 含 P0 判分修复，**随本 PR 提交**。 | 报告数字对应的脚本与 fixture 均在本 PR 中；`check_schema.py` / `check_golden.py` 可复验。 | 若脚本与报告不一致，以本 PR 的脚本为准；未来修改判分逻辑必须 bump 报告版本并重跑。 |
| **R-6** | **冷启动延迟未测**（探针脚本存在但无输出，n=5 < 20） | §10 | `OPEN-b2`（p95 目标）仍无法关闭 |
| **R-7** | **判分缺陷（已修）**：`build_cases()` 的 query 分支把 `acceptable_movie_ids` 写死为 `[]`，60 条 query 的 `acceptable_hit` 恒为 `None`，任务级判定从不检查 golden 可接受集合。 | 修复前 G1 = 69/75 = 92.0%（PASS）；修复后 62/75 = 82.7%（FAIL）。修复内容：按 `query_id` 读取 `queries[query_id]["acceptable_movie_ids"]`，缺失即报错；`score_case` 对空集合仍返回 `None`（tool 用例行为不变）。queries 文件未改，fixture 自检哈希不变。 | **上一版 92%/PASS 结论作废**；今后任何"任务级成功"数字都必须确认 `acceptable_hit` 参与判分。是否需要 oracle/主 agent 复盘该缺陷的评审过程，由主 agent 决定 |
| **R-8** | **相似/推荐执行器是评测替身**：`find_similar_movies` 按“共享类型数/年份接近度/评分”排序，`recommend_movies` 按偏好过滤 + 评分排序，均非生产契约（`OPEN-b5`）。 | `execution[].ranking` 字段与 `tool_eval.py` 的 `SIMILAR_POOL` / `_similar` / `_recommend` | `similar_movies` 7/15、`recommend_by_preference` 9/10 的通过率**不代表**生产相似信号（如同导演）已实现；生产实现须另立 issue 并重新评测 |

---

## 14. 复现（命令与产物）

```bash
# 1) 契约自检（默认需要 MySQL；只做文件级断言时加 --skip-db）
python3 scripts/eval/check_schema.py --skip-db   # 文件级断言（含 annotates.sha256 绑定）
python3 scripts/eval/check_schema.py             # 追加 DB：enum ≡ SELECT DISTINCT name、id 存在性
python3 scripts/eval/check_golden.py             # B1.5a 冻结集 + snapshot block + db_snapshot_hash（需 MySQL）
python3 scripts/eval/check_golden.py --skip-db   # 无 DB 的文件级检查

# 2) 干跑（校验 fixture 并打印计划，无网络、无 SQL）
python3 scripts/eval/tool_eval.py --dry-run --variant G0 --cases both

# 3) 实测（端点按 <host> 替换；--allow-full-run 因超过 10 例阈值而必需）
python3 scripts/eval/tool_eval.py --variant G0 --cases both --allow-full-run \
  --base-url http://<host>:11434 --out .pi/artifacts/b1-5b-eval/G0.json
python3 scripts/eval/tool_eval.py --variant G1 --cases both --allow-full-run \
  --out .pi/artifacts/b1-5b-eval/G1.json
# G2 = 回放 G0 的模型调用（不重新采样，model_calls 应为 0）
python3 scripts/eval/tool_eval.py --variant G2 --cases both --allow-full-run \
  --reuse-g0 .pi/artifacts/b1-5b-eval/G0.json --out .pi/artifacts/b1-5b-eval/G2.json

# 4) 聚合（产物脚本，不入库）
python3 .pi/artifacts/b1-5b-eval/analysis.py > .pi/artifacts/b1-5b-eval/analysis.txt
```

G3 **无对应命令**（未实现、未运行）。

---

## 附录 A：逐例矩阵（G0 / G1 / G2，✅ = 任务级成功）

图例：✅ 任务级成功 / ❌ 失败 / `calls` = G0 该条发起的工具调用数；`G0 失败模式` 取自运行记录（修复后）。

| 用例 | 意图 | G0 | G1 | G2 | calls | G0 失败模式 |
| --- | --- | --- | --- | --- | --- | --- |
| T01 | refusal | ✅ | ✅ | ✅ | 0 | — |
| T02 | refusal | ❌ | ✅ | ❌ | 1 | wrong_tool, under_refusal |
| T03 | refusal | ✅ | ✅ | ✅ | 0 | — |
| T04 | refusal | ✅ | ✅ | ✅ | 0 | — |
| T05 | refusal | ✅ | ✅ | ✅ | 0 | — |
| T06 | refusal | ❌ | ✅ | ❌ | 1 | wrong_tool, under_refusal |
| T07 | clarification | ❌ | ✅ | ❌ | 1 | wrong_tool, unrequested_tool, no_clarification |
| T08 | clarification | ❌ | ✅ | ❌ | 1 | no_clarification |
| T09 | clarification | ✅ | ✅ | ✅ | 1 | — |
| T10 | clarification | ✅ | ✅ | ❌ | 1 | wrong_tool, bad_args, unrequested_tool |
| T11 | multi_slot | ✅ | ✅ | ✅ | 1 | — |
| T12 | multi_slot | ❌ | ✅ | ❌ | 1 | acceptable_set_miss, empty_result |
| T13 | multi_slot | ✅ | ✅ | ✅ | 1 | — |
| T14 | multi_slot | ❌ | ✅ | ❌ | 1 | wrong_tool, incomplete_tool_sequence |
| T15 | multi_slot | ✅ | ✅ | ✅ | 1 | — |
| Q01 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q02 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q03 | search_by_title | ❌ | ✅ | ❌ | 1 | bad_args, result_constraint_violation |
| Q04 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q05 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q06 | filter_catalog | ❌ | ✅ | ❌ | 1 | wrong_tool, incomplete_tool_sequence, slot_mismatch, acceptable_set_miss, empty_result |
| Q07 | filter_catalog | ❌ | ✅ | ❌ | 1 | wrong_tool, incomplete_tool_sequence, slot_mismatch, acceptable_set_miss, empty_result |
| Q08 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q09 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q10 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q11 | filter_catalog | ❌ | ❌ | ❌ | 1 | slot_mismatch, acceptable_set_miss, empty_result |
| Q12 | similar_movies | ❌ | ❌ | ❌ | 1 | incomplete_tool_sequence, slot_mismatch, acceptable_set_miss, empty_result |
| Q13 | similar_movies | ❌ | ✅ | ❌ | 1 | incomplete_tool_sequence, result_constraint_violation, acceptable_set_miss |
| Q14 | similar_movies | ❌ | ✅ | ❌ | 1 | incomplete_tool_sequence, result_constraint_violation, acceptable_set_miss |
| Q15 | similar_movies | ❌ | ✅ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q16 | similar_movies | ❌ | ❌ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q17 | recommend_by_preference | ✅ | ✅ | ✅ | 1 | — |
| Q18 | recommend_by_preference | ❌ | ✅ | ❌ | 1 | wrong_tool, incomplete_tool_sequence, slot_mismatch |
| Q19 | recommend_by_preference | ❌ | ❌ | ❌ | 1 | wrong_tool, incomplete_tool_sequence, slot_mismatch |
| Q20 | recommend_by_preference | ❌ | ✅ | ❌ | 1 | wrong_tool, incomplete_tool_sequence, slot_mismatch |
| Q21 | search_by_title | ❌ | ❌ | ❌ | 1 | acceptable_set_miss, empty_result |
| Q22 | search_by_title | ❌ | ❌ | ❌ | 1 | acceptable_set_miss, empty_result |
| Q23 | search_by_title | ❌ | ❌ | ❌ | 1 | acceptable_set_miss, empty_result |
| Q24 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q25 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q26 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q27 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q28 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q29 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q30 | filter_catalog | ❌ | ✅ | ❌ | 1 | wrong_tool, incomplete_tool_sequence, slot_mismatch, acceptable_set_miss, empty_result |
| Q31 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q32 | filter_catalog | ❌ | ✅ | ❌ | 1 | slot_mismatch |
| Q33 | similar_movies | ❌ | ✅ | ❌ | 1 | incomplete_tool_sequence, result_constraint_violation, acceptable_set_miss |
| Q34 | similar_movies | ❌ | ✅ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q35 | similar_movies | ❌ | ❌ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q36 | similar_movies | ❌ | ❌ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q37 | similar_movies | ❌ | ❌ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q38 | recommend_by_preference | ❌ | ✅ | ❌ | 1 | wrong_tool, incomplete_tool_sequence, slot_mismatch |
| Q39 | recommend_by_preference | ❌ | ✅ | ❌ | 1 | wrong_tool, incomplete_tool_sequence, slot_mismatch |
| Q40 | recommend_by_preference | ✅ | ✅ | ✅ | 1 | — |
| Q41 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q42 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q43 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q44 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q45 | search_by_title | ✅ | ✅ | ✅ | 1 | — |
| Q46 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q47 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q48 | filter_catalog | ❌ | ✅ | ❌ | 1 | slot_mismatch, acceptable_set_miss, empty_result |
| Q49 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q50 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q51 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q52 | filter_catalog | ✅ | ✅ | ✅ | 1 | — |
| Q53 | similar_movies | ❌ | ✅ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q54 | similar_movies | ❌ | ✅ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q55 | similar_movies | ❌ | ❌ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q56 | similar_movies | ❌ | ❌ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q57 | similar_movies | ❌ | ❌ | ❌ | 1 | incomplete_tool_sequence, acceptable_set_miss |
| Q58 | recommend_by_preference | ✅ | ✅ | ✅ | 1 | — |
| Q59 | recommend_by_preference | ✅ | ✅ | ✅ | 1 | — |
| Q60 | recommend_by_preference | ✅ | ✅ | ✅ | 1 | — |

合计：G0 **39/75**、G1 **62/75**、G2 **38/75**。

---

## 附录 B：本报告不声称的内容

- 不声称 B-① 已交付、已排期或已知工期；不声称 G1 的 82.7% 是泛化估计（§11.1），也不声称它已跨过 ≥90% 门槛（**它没有跨过**）。
- 不声称相似/推荐能力已实现：两者的执行器是评测替身（§2.2、§13 R-8），`similar 7/15` 不等于“同导演相似检索可用”。
- 不声称 G3 会或不会达标；不声称 G2 的 50.7% 代表"服务端校验无用"（它只代表"校验 + 无修复"这一组合无效）。
- 不声称 MCP 形态可行、端点可达或鉴权已解决；不声称任何生产代码、DB 写入或 CI 依赖已存在。
- 不声称判分口径已独立复核：可接受集合来自 B1.5a 冻结标注（单人标注），`result_constraints` 的“文本是否表述”为同套正则自洽判定（§2.3、R-3），澄清类只验形态（§2.3）。
- 不声称 oracle 已复核本报告数字、主 agent 已裁定 §13 的 R-1…R-8；这些仍待复核。

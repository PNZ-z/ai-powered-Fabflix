# 工作流日志 — B1.5 最终修订与拆分（oracle 9 项阻塞 + 用户批准决策）

## 元数据

| 项目 | 值 |
| --- | --- |
| 日志日期 | **2026-11-09** |
| 任务类型 | **纯文档**（`docs/` only）：按 oracle 9 项阻塞与用户批准 D-1…D-9 最终修订/拆分 B1.5 文档组 |
| GitHub issue | **无**（B1.5a / B1.5b 两份 issue 草稿**均未创建**）→ 使用 `request-YYYY-MM-DD-<slug>` 命名 |
| PR | **无** |
| 分支 / 提交 | 无。改动**未提交、未推送**（工作区基线 `main` @ `85d4675`） |
| 相关计划 | `docs/plans/b1-5-movie-retrieval-eval.md`（umbrella）、`docs/plans/b1-5a-movie-card-embeddings.md`、`docs/plans/b1-5b-tool-routing.md`（均 🟡 待 oracle 复核） |
| 相关规范 | `docs/architecture/ai-user-workflows.md`、`docs/architecture/movie-cards.md`（r3） |
| 评测契约 | `docs/ai/README.md`、`docs/ai/tool-schema.b1-5.json`、`docs/ai/golden-set.schema.json`（新建） |
| 已运行的验证 | 只读 SQL（容器 `fabflix-mysql` 3307）、Ollama `/api/tags` + `/api/embed`、JSON 解析、SHA-256 计算、`grep` 口径扫描（见 §1.3）。**未运行** `mvn verify`、前端测试、任何评测脚本、任何工具调用轮次 |
| 未运行/未核实 | 60 条金标准集内容（待标注）、三次 hash 的实际计算（仅 `schema_hash` 已算）、oracle 复核、B1.5a/b 的评测执行 |

> **本次改动的性质**：文档修订与拆分。**不创建** GitHub issue/PR，**不修改** `README.md`、源码、测试、配置，**不改写** B1/B0 已批准文档历史（只追加 dated 回填）。

---

## 1. 修改了哪些文件及潜在问题

### 1.1 逐文件说明

**新建（7 个）**

| 文件 | 目的与内容 |
| --- | --- |
| `docs/plans/b1-5a-movie-card-embeddings.md` | **A 路线执行计划**：21k 数据集契约（含三个数据陷阱：评分只覆盖 2000+、`id` 非 `tt`、类型名重复与变体）、两个已安装 embedding 候选（均 1024 维）、60 条金标准集规则（20/20/20 + 采样覆盖 + 反例）、任务级成功主指标 + 组件指标、预注册阈值（D8 ≥80%）、成本外推表、三个 fingerprint、`B15a-1…9` 验收、风险与回退、`OPEN-a1…a5`；**§9 给出 `README.md` 第 13/14 行的精确替换文本**（本 issue 不修改 README）。**不碰任何工具/路由** |
| `docs/plans/b1-5b-tool-routing.md` | **B 路线执行计划**：机器可读 schema 契约（`docs/ai/tool-schema.b1-5.json`）+ `schema_hash`；60 条共享查询 + 15 条工具用例（6 拒绝 / 4 澄清 / 5 多槽位）；**G0 与 G2 必做、G1/G3 可选**；年份/枚举/错误码三态契约（`INVALID_ARGUMENT` / `OUT_OF_RANGE`+coverage / `EMPTY_RESULT`+coverage）；确定性路由设计（S1–S5，LLM 只受控选择）；接地零容忍；`B15b-1…10` 验收。**不碰 embedding** |
| `docs/issues/proposed-b1-5a-movie-card-embeddings.md` | A 路线 issue 草稿：`## GitHub issue title` / `## GitHub issue body`（English）/ `## Internal tracking`（milestone #3、无 labels、未创建、待确认项、发布自检）。AC-9 写"本期不发布端点 → N/A" |
| `docs/issues/proposed-b1-5b-tool-routing.md` | B 路线 issue 草稿：标题**去掉 "MCP-style"**，改 "structured tools and deterministic routing"；不写"replaces AC-B-12"；AC-9 写"MCP 端点 N/A，若引入另立安全验收"；不夸大 MCP |
| `docs/ai/tool-schema.b1-5.json` | **评测期机器可读 schema 契约**（`contract_version = b1.5-tools/v1`）：5 个工具、`additionalProperties:false`、显式 `required`、数值 `minimum/maximum`、`genres.items.enum` = **DB 派生的 56 个 distinct 名称**；含 `error_contract` 与 `db_snapshot` 元数据；`schema_hash = 8eea424b…44cd` |
| `docs/ai/golden-set.schema.json` | 金标准集 JSON Schema（`$defs`：`queryCase` / `toolCase` / `snapshotRef`；两种根形状：60 条 `queries` 或 15 条 `tool_cases`；`movieId` 长度 ≤10 且不假设 `tt`） |
| `docs/ai/README.md` | 评测契约索引 + **三个 hash 的精确算法**（`template_hash` / `db_snapshot_hash`（含冻结导出 SQL 与 `snapshot_export_version=1`）/ `schema_hash`）+ 当前 hash 值与维护规则 |
| `docs/logs/2026-11-09-request-b1-5-final-revision.md` | 本日志 |

**改写（3 个）**

| 文件 | 动作与内容 |
| --- | --- |
| `docs/plans/b1-5-movie-retrieval-eval.md` | 由"单一大计划"**改写为 umbrella / 决策备忘录**：共享数据集快照、共享 60+15 样本、共享任务级成功指标与 2×2 配对、决策标准与**先定门槛的决策规则**、`AC-B-12` 按 D-7 的改写说明、拆分顺序（a → b → memo）、用户批准决策台账（D-1…D-10）、文档地图、跨路线风险与 `OPEN-U1…U5`。r2 的执行细节（30 条样本、单 issue `B15-1…10`）标 superseded，**不再复制**（避免双份口径） |
| `docs/issues/proposed-b1-5-movie-retrieval-eval.md` | 改为 **SUPERSEDED 说明页**：列出"原草案中已被取代的 12 项具体内容 → 现在的口径"对照表，明确**不得据此创建 issue**，保留追溯与"仍有效的前提" |
| `docs/architecture/ai-user-workflows.md` | **r3**：新增 §1.4「本文是 spec，不是运行时 prompt」+ compact `runtime-prompt/v1`（组成/禁止注入/token 预算/裁剪规则/版本化）；`R2-4` 与 `R7-1` 统一三态错误契约；`GenreEnum` 改为 **DB 派生 + 当前快照（56 distinct / 61 行）+ 规范化规则**；`§3` 覆盖说明改为 21k；`U1` 增加**中文译名策略**（无别名表 → 不保证命中）；`U2` 澄清规则与 `§4.2` 年份/评分覆盖同步；`U3` 给 `same_primary_genre_only` **确定性定义**（seed 字典序首个 genre，或删除）；`U5` 用 `result_set_id`/显式 `movie_ids` **修复单轮矛盾**；`§4` 追加 D-6（本期不实现 MCP）；`§4.3` 新增 `OUT_OF_RANGE` 行并恢复 `NOT_FOUND`；`§4.5` 明确 fixture 为评测期契约、实施后以注解为真值 + 一致性测试；`§5` `id` 改为不透明；`§7.1` 增加 runtime prompt 与 schema 契约触发项；`§8` 追加 r3 行；`§9` 更新 `WU-4/5/6/7` 状态并新增 `WU-8/9` |
| `docs/architecture/movie-cards.md` | **r3**：§1.2/§1.2.1 改为完整数据集快照（21,087 / 196–2024 / 61 行 56 distinct / 13,082 无评分 / 评分仅 2000+ / `id` 9,073 个 `tt`）；§1.1 `id` 与 `null` 口径修正；§1.3 样例回查确认仍有效（`Chief Zabu` = `tt0094859`）；§2.2 成本表改双模型（同为 1024 维）+ 注明 `qwen3-embedding` token 需重测 + 截断口径冲突（2,048 vs 8,192）；§3 能力矩阵更新类型/年份/评分行；§6 **三 hash 算法定稿**并新增 `genre_rows`/`ratings_covered_from_year`/`embedding_digest`；§8 追加 r3 行；§9 `MC-U1`/`MC-U3` 标已解决、新增 `MC-U6` |

**追加 dated 回填（2 个，历史正文未改写）**

| 文件 | 动作与内容 |
| --- | --- |
| `docs/plans/ai-option-b-spring-ai-rag.md` | 顶部追加 **2026-11-09 执行状态回填（7 条）**：D-5 取消用户侧 B-②、D-7 改写 `AC-B-12`、B-① 口径扩展、D-3 数据口径、D-6 不实现 MCP、README 待改、新文档入口。**inline 标注 superseded**：交付物 B-②、§1.3 heading 与两条 bullet、`fabflix.ai.corpus.paths` 配置行、任务分解 B-② 行（并新增 B1.5a/b 行）、AC-B-12 行（改写为两条验收）、`doc-qa` 评测集行 |
| `docs/plans/ai-roadmap-overview.md` | 顶部追加 **2026-11-09 执行状态回填（8 条）**；inline 标注：§2.3 语料构成、C-4 语料约束 bullet、§4 B-②、**M4.2 整行**、§6 里程碑表后新增"2026-11-09 里程碑修正"段（M4.2 不再交付 / M4.1 前置 = B1.5a+b+memo / M4.3 条件性 / Nightly 触发点改变）、§7 RAG 用例、能力表 RAG 与多语言风险两行 |

### 1.2 9 项 oracle 阻塞的落点

| 阻塞 | 落点 |
| --- | --- |
| **B-1 旧文档冲突** | roadmap + ai-option-b 各加 dated 回填块；inline superseded 于 M4.2、C-4、§2.3、部署语料 bullet、交付物 B-②、`corpus.paths`、doc-qa；README **不改**，精确替换文本写入 `b1-5a` §9（实际行号 **13/14**，简报中的 15/16 为旧编号） |
| **B-2 年份契约统一** | `ai-user-workflows.md` `R2-4` / `R7-1` / `§4.2` 年份与评分行 / `§4.3` 错误表；`b1-5b` §3.1 三态表；`tool-schema.b1-5.json` 的 `minimum/maximum` 与 `error_contract` |
| **B-3 枚举/年份由 DB 派生** | `ai-user-workflows.md` §4.2 `GenreEnum` 派生 SQL + 快照清单 + 规范化规则；`movie-cards.md` §1.2；`tool-schema` 的 enum 由 SQL 生成；全文删除"23 个""2001–2019"作为当前口径 |
| **B-4 共同指标/样本** | umbrella §3（60 + 15 + 5 反例、任务级成功、2×2 配对、方向性声明）；`b1-5a` §4/§5；`b1-5b` §4/§6 |
| **B-5 schema fixture** | `docs/ai/tool-schema.b1-5.json`（新建）+ `schema_hash` + `b1-5b` §2.1 + `ai-user-workflows` §4.5 一致性测试要求 |
| **B-6 fingerprint** | `docs/ai/README.md` §2（三 hash 精确算法）；`movie-cards.md` §6；`b1-5a` §7；`b1-5b` `B15b-9` |
| **B-7 spec vs runtime prompt** | `ai-user-workflows.md` 新增 §1.4（含 token 预算与裁剪规则）；`U5` 矛盾修复（`result_set_id`）；中文人名/片名策略（`U1` + `WU-9`）；`same_primary_genre_only` 定义（`U3`） |
| **B-8 issue 措辞** | 两份新草稿的标题与 AC；`b1-5b` 标题无 "MCP-style"、不写 "replaces AC-B-12"、AC-9 写 N/A；旧草稿改为 superseded 并列出 12 项被取代表述 |
| **B-9 拆分** | 三份 plan（umbrella + a + b）+ 两份 issue 草稿 + 旧草稿 superseded；umbrella 只留共享口径与决策规则 |

### 1.3 已运行的验证（可复核）

| 检查 | 命令/方式 | 结果 |
| --- | --- | --- |
| 数据集口径 | `docker exec fabflix-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -B moviedb -e "..."` | `movies` **21087**；`genres` **61 行 / 56 distinct**；年份 **196–2024**；1990s **2370**；无评分 **13082**；`stars` 76259 / `stars_in_movies` 124958 / `genres_in_movies` 25433 / `ratings` 8005 |
| 重复类型名 | `SELECT name, COUNT(*) ... GROUP BY name HAVING c>1` | `Action`/`Comedy`/`Drama`/`Horror`/`Thriller` 各 2 行 |
| 类型长尾/变体 | `SELECT id, name FROM genres ORDER BY name,id` | `Sci-Fi`(19) **457** 部 vs `Science Fiction`(9005) **2** 部；另有 `anti-Dram`/`AvGa`/`Disa`/`Psyc`/`Porn`/`TVmini`/`verite` 等 |
| 评分覆盖 | 按年代 `SUM(r.rating IS NOT NULL)` | 1960–1999 **全部 0 有评分**；13,082 = 11,917（≤1999 全部）+ 1,165（2000+ 未评分） |
| `id` 形态 | `SUM(id LIKE 'tt%')` | **9,073 / 21,087**；其余如 `AWw10`（The Matrix, 1999） |
| 示例可命中性 | `1990s ∧ (Sci-Fi ∪ Science Fiction)`；`Comedy ∧ year≥2000 ∧ rating≥8` | **55** 部；**84** 部（用于替换"必然空结果"的旧示例） |
| 第二个模型 | `curl /api/tags` + `/api/embed`（`qwen3-embedding:0.6b`） | 已安装 **639,150,858 B**、Q8_0、`embedding_length=1024`；`/api/embed` 返回 **1024** 维向量 |
| `schema_hash` | `sha256(json.dumps(tools, sort_keys, separators=(',',':')))` | `8eea424b657931c97d8a66a85bf42be4d88d3fb1516cca447c5bd49ef5ab44cd`；整文件 `file_sha256 = fabafa2e…5dd5a984` |
| JSON 有效性 | `python3 -c "json.load(...)"` | `tool-schema.b1-5.json` 与 `golden-set.schema.json` 均可解析 |
| 口径残留扫描 | `grep -n "1,054\|9,052\|2001–2019\|23 个英文"` 于 4 份新/改文档 | 仅剩**显式标注为历史对照**的引用（1 处活跃残留已修：`R3-2` 的无评分片数字） |
| 未运行 | `mvn verify` / 前端 lint/test/e2e / 任何评测脚本 / 任何工具调用轮次 / oracle 复核 | 一律**未运行**，文档中不声称已运行 |

### 1.4 遗留风险、边界限制与后续事项

| 项 | 说明 |
| --- | --- |
| **oracle 尚未复核拆分后的版本**（阻断发布） | 三份 plan + 两份 issue 草稿 + 两份规范 r3 均标 🟡；B1.5a/b 的 issue 草稿**不得**交付 `github-operator` 创建 |
| **60 条金标准集内容未产出** | 本次只定义格式（`golden-set.schema.json`）与规则（20/20/20 + 采样覆盖 + 快照验证），**未**逐条标注；标注人/复核人未定（`OPEN-U3`） |
| **`db_snapshot_hash` / `template_hash` 未实际计算** | 算法已冻结（`docs/ai/README.md` §2），但本次**没有运行**导出 SQL 与求和脚本；`schema_hash` 已算并记录 |
| **两个阈值仍需确认** | `OPEN-U1`（B 路线 ≥90%）与 `OPEN-U2`（任务级成功率是否等价 D8 的 top-5 命中率）—— 均为**建议**，未获批准 |
| **截断阈值冲突未解** | scout 实测 2,048 tokens vs `/api/tags` 声明 `context_length=8192`；已写入 `b1-5a` §12 与 `movie-cards.md` §2.2 作为"必须先实测"项（`OPEN-a2`） |
| **中文译名未解决** | 目录只有英文原片名；别名表属新数据来源 + 许可，**未批准**。A 路线中→英轨道会计中→英命中率；B 路线明确"不保证命中" |
| **README 未改** | 按边界只给精确替换文本（`b1-5a` §9）；主 agent/worker 在 B1.5a 的 PR 应用 |
| **历史文档中的"9,052 / 2001–2019 / 23 类型"仍大量存在** | 这是**有意保留**（不改写历史），已通过 dated 回填 + inline superseded 标注；reviewer 需按"是否以 active 口径引用"判断，而不是按字符串存在与否 |
| **B1.5a 与 B1.5b 的接口是"文件级"而非"流程级"** | 60 条共享查询由 B1.5a 先冻结、B1.5b 复用；若两人并行启动，存在 `golden_version` 漂移风险（已写入 umbrella §10 风险表） |
| 已知遗留问题 | **未发现**本次文档改动引入的技术性错误；上述均为已显式标注的未决项而非隐藏缺陷 |

---

## 2. 新增知识点 / 重点学习对象

### 2.1 数据工程 / SQL

| 知识点 | 含义 | 对应位置 | 为什么值得学 |
| --- | --- | --- | --- |
| **`COUNT(*)` 与 `COUNT(DISTINCT name)` 的差距是契约问题，不只是统计问题** | `genres` 61 行但只有 56 个不同名称（5 个名称各有 2 行）→ 枚举若直接 `SELECT name` 会出现重复值，`items.enum` 就带重复项 | `movie-cards.md` §1.2、`tool-schema.b1-5.json` | "行数 ≠ 值域大小"是数据契约最容易被忽略的一层；它直接决定 schema 是否合法 |
| **语义变体在大数据与小数据下后果不同** | `Sci-Fi` 457 部 vs `Science Fiction` 2 部：同一个用户意图，映射到哪一行就差 200 倍结果量 | `ai-user-workflows.md` §4.2 规范化说明 | 说明"规范化/别名"必须显式决策并记录，不能留给模型 |
| **覆盖率是有形状的，不是单一数字** | 21,087 部里 13,082 无评分，且**恰好等于**"2000 年前全部 + 2000 后一部分"→ 任何"旧年代 + 评分"查询在数据上不可满足 | `b1-5a` §2.2、`ai-user-workflows.md` `R2-4` | 评测集的"空结果"必须区分"用户问错了"与"数据不可能有"，否则会把数据缺陷记成模型缺陷 |
| **不透明主键** | 只有 9,073/21,087 个 id 以 `tt` 开头，其余是 `AWw10` 这类 → 任何 `tt` 前缀假设都会漏 | `movie-cards.md` §1.1 | 真实数据集常是混合来源；"看起来像 IMDb id" 不等于全是 |

### 2.2 评测设计 / 统计

| 知识点 | 含义 | 对应位置 | 为什么值得学 |
| --- | --- | --- | --- |
| **任务级成功 vs 组件指标** | 组件指标（recall@5、槽位 exact-match）只能解释"为什么"，不能跨路线比较；只有"可接受结果是否进入最终答案 + 接地"能对齐 A（检索）与 B（工具） | umbrella §3.2 | 跨架构比较最常见的失败就是拿不同量纲的指标对比 |
| **逐条配对（2×2）而非两个总体均值** | 同一批 60 条上分"A中/B中、A中/B不中、A不中/B中、都不中"，能看出"两者各擅长什么"，而均值只会给出一个无法行动的结论 | umbrella §3.2 | n=60 时均值差异极易被离群样本支配；配对表更诚实 |
| **预注册门槛** | 先把 ≥80%（D8 已批准）与"纯 LLM 不可交付"（D-9）写死，再看数据；否则"数据出来再挑标准"能得出任意结论 | umbrella §4 决策规则 | 这是防自欺的最小制度设计 |
| **样本方向性声明** | n=60、单人标注 → 只做方向性判断，不做显著性结论 | umbrella §3.2、`b1-5a` §4.2 | 避免用小样本包装"显著优于" |
| **可接受集合 vs 单一正确答案** | "相似电影"多解，单答案会让指标虚高或虚低 | `b1-5a` §4.2、`golden-set.schema.json` 的 `acceptable_movie_ids` | 生成式/推荐类任务的标注基本要求 |

### 2.3 可复现性（fingerprint）

| 知识点 | 含义 | 对应位置 | 为什么值得学 |
| --- | --- | --- | --- |
| **三类 hash 各自锁住不同的东西** | `template_hash` 锁"模板定义"、`db_snapshot_hash` 锁"数据内容"、`schema_hash` 锁"工具契约"；缺一类就无法定位"数字变了是谁变的" | `docs/ai/README.md` §2 | 单调一个"语料 fingerprint"无法区分模板改动 vs 数据改动 vs schema 改动 |
| **规范化导出 + 逐行哈希再汇总** | 先按 `id` 排序、字段用 `\u001f` 连接、逐行 `sha256`，再对排序后的行哈希串接求整体 hash → 结果与查询顺序/空值表示无关，可稳定复现 | `docs/ai/README.md` §2.2 | 直接对大表 `mysqldump` 做 hash 会被排序/格式噪声破坏，抖动不可解释 |
| **hash 口径要版本化** | `snapshot_export_version` / `contract_version` / `card_version` / `golden_version` 各自 bump，声明"与历史不可比" | 同上 §3 | 没有版本号的 hash 只是"看起来严谨" |

### 2.4 LLM 应用工程

| 知识点 | 含义 | 对应位置 | 为什么值得学 |
| --- | --- | --- | --- |
| **spec ≠ runtime prompt** | 规范给人读（含理由、反例、维护规则），运行时 prompt 是小型产物（≤900 tokens 含 schema）；把规范整篇注入会导致 token 超预算 + 行为随文档修订漂移 | `ai-user-workflows.md` §1.4 | 这是"文档驱动 LLM 行为"最常见的坑 |
| **schema 承载约束，prompt 不重复** | 工具名/参数/枚举/必填放在 JSON Schema 里，prompt 不重写一遍 → 避免双真值 + 重复 token | 同上 §1.4 + §4.5 | 双真值会让"文档说 A、运行做 B"长期存在 |
| **错误码要区分"非法"与"不可满足"** | `INVALID_ARGUMENT`（用户/模型错了）vs `OUT_OF_RANGE`（数据覆盖不够）vs `EMPTY_RESULT`（正常空）；混用会让模型把数据缺陷说成"没有这种电影" | `ai-user-workflows.md` §4.3、`b1-5b` §3.1 | 这是"模型胡说"的一个系统性来源，而且可以纯靠工程消除 |
| **确定性路由 + 受控选择** | 服务端先定意图/拒绝/枚举映射，LLM 只在候选里选或填槽；实测 58% 的纯 LLM 路由因此不可交付 | `b1-5b` §7、umbrella §8 D-9 | 把"模型能力"与"工程可治愈部分"分开，是决定要不要上大模型的前提 |
| **最小会话状态** | `result_set_id`（服务端短期引用）而不是对话记忆，既让"解释推荐理由"可行，又不破坏"单轮"约束 | `ai-user-workflows.md` `U5` | 多轮能力可以从最小状态开始，不必一步跳到会话记忆 |

### 2.5 文档治理

| 知识点 | 含义 | 对应位置 | 为什么值得学 |
| --- | --- | --- | --- |
| **dated 回填 + inline superseded，而不是删历史** | 已批准文档的历史结论不能悄悄改写；在顶部加日期块 + 在 active 段落加 `⚠️ [superseded]`，既保留审计链又阻止误用 | `ai-option-b` / roadmap 顶部与 inline 标注 | 评审者能区分"历史事实"与"当前口径" |
| **拆分时保留一个 umbrella** | 拆分后若每份都重写共享口径，必然漂移；共享数据集/指标/决策规则只留一处，子计划只引用 | `b1-5-movie-retrieval-eval.md` | 多文档体系里"单一事实来源"比"文档齐全"更重要 |
| **superseded 页要列"被取代的具体表述"** | 旧 issue 草稿改成 superseded 时，逐条列出"原文 → 新口径"（12 项），比一句"已废弃"更能防止有人复制旧文本 | `docs/issues/proposed-b1-5-movie-retrieval-eval.md` §2 | 废弃文档的最大风险是被人当模板复用 |

### 2.6 本次**没有**新增的知识点

Java / Spring AI 代码层、前端、构建与 CI 本次**没有**任何新增（未读入新实现、未运行任何测试）；这些属既往工作流已记录内容，本次不重复。**本次没有新增源码层知识点。**

---

## 3. r4 增量修订（同日，按 `oracle-b1-5-delta.md` 复核）

> 来源：`.pi/artifacts/oracle-b1-5-delta.md`（oracle 只读增量复核：Verified 6 项关闭 + 新提 3 项 B1.5a 阻塞 / 2 项 B1.5b 阻塞）。**仍是纯文档修订，未创建 issue、未改源码/配置/测试、未改 `README.md`。**

### 3.1 逐文件改动

| 文件 | 改动 | 目的 |
| --- | --- | --- |
| `docs/ai/README.md` | §2.2 的 `db_snapshot_hash` 导出 SQL 换成**可执行版**（`stars3_sorted` 子查询改为内层 `ORDER BY st.name LIMIT 3` + 外层 `GROUP_CONCAT`），并补“在 3307 实跑 + 样例行”说明；§2.2 说明段与 SQL 对齐（`genres_sorted` 保持 `DISTINCT`/`ORDER BY`、`rating` 缺失 `NULL`、`snapshot_export_version=1`）；§2.3 新增 `required`/`anyOf` 逐工具表；新增 §4 修订记录 | 原 SQL 引用不存在的 `stars_in_movies.star_movie_count`（实跑 `ERROR 1054`），会让 E2 无法产出 `db_snapshot_hash` |
| `docs/architecture/movie-cards.md` | **r4**：§1.2/§1.2.1/§2.1/§3/§3.1 的旧“1.43 / 1.73 / 最多 3”改为实测值（类型 **全量 1.2061 / 有 genre 的片内 1.4313 / 最多 4 / 3,318 无 genre**；主演 **全量 5.9258 / 有主演的片内 7.0458 / 最多 46 / 3,352 无主演**）；§2.1 明确 **0 genre → `Genres: N/A` / `类型：暂无`，不得跳过字段**、4 genre 完整输出；新增 `MC-U7`（已解决）、`MC-U8`（`{stars}` 空值占位符待定）；§8 追加 r4 行 | 旧统计会让人按“最多 3”做截断/校验，与数据（最多 4、3,318 部 0 genre）冲突 |
| `docs/plans/b1-5a-movie-card-embeddings.md` | **r4**：§5.1 重写为“判定指标分工（D8 gate = `recall@5` ≥80%；任务级成功为次要、不纳入通过条件）”；§5.2 把 `recall@5` 移出“解释用指标”；§5.3 阈值表分行；§2.1 统计改精确值；§8 `E3` 改为“2 模板卡片 + 2 模型 embedding（4 组合）”；§10 `B15a-4`/`B15a-6` 同步；`OPEN-a1`/`OPEN-a5` 关闭；§14 复核状态、§15 修订记录 | 修 `B15a-4` 与 `B15a-6` 自相矛盾（“任务级为主”vs“top-5 判定”）、E3 易被读成卡片生成 4 次、标注人未定 |
| `docs/issues/proposed-b1-5a-movie-card-embeddings.md` | **r4**：Scope 首个指标项改为“`recall@5` 优先（唯一 pass/fail）→ `MRR@10` → 任务级成功（次要）”；AC-4/AC-6 同步 D8 口径；Dependencies 写明标注/复核人（主 agent）+ worker 只生成候选 + SQL 验证 + 单人标注方向性；README 项明确“**in this same PR**”；Internal tracking 关 `OPEN-a1`/`OPEN-U2`/`OPEN-a5` | 保证 issue 正文与计划口径一致，且不把次要指标写成验收条件 |
| `docs/plans/b1-5-movie-retrieval-eval.md` | **r4**：§3.2 拆为“D8 门（A 的 `recall@5`）”与“跨路线配对（任务级成功）”两个用途；§2 统计改精确值；§4 决策规则 1、§7 门槛判定同步；`OPEN-U2`/`OPEN-U3` 关闭；顶部 r4 段 + §12 状态 | 共享指标只能有一个来源，且不能让配对指标替代 D8 门 |
| `docs/plans/b1-5b-tool-routing.md` | **r4**：§1.2/§5 把 **G1 改为必做**（G3 仍可选），补“G1 为何必做”与“最小可接受形态”；§6 硬门槛写明确定性形态数字只能来自 G1；§2.1 硬要求拆成 `required` vs 对象级 `anyOf`；§8 T 表拆 T6（G1 必做）/ T6b（G3 可选）；§9 `B15b-1`（`required`/`anyOf` 显式）、`B15b-3`（G0/G1/G2 均跑）；`OPEN-b4` 只决定 G3；§13 状态 + 新增 §14 修订记录 | D-9 已批准“确定性路由优先”，AC-8 要求给出该形态实测数字——G1 若可选则 AC-8 无法满足 |
| `docs/issues/proposed-b1-5b-tool-routing.md` | **r4**：Scope 改为 G0/G1/G2 必做、G3 可选；Scope 首个 bullet 写明 `required` 或对象级 `anyOf`；AC-1/AC-3 同步；Dependencies 与 `OPEN-b4` 只保留 G3；内部表注明 `anyOf` 已复核、`schema_hash` 未变 | 同上，并避免“所有工具 required 非空”的机械验收误判 |
| `docs/plans/ai-option-b-spring-ai-rag.md` | 仅在 `:41`（总结段）**行内**追加 `⚠️ [2026-11-09 superseded（局部）…]` | 顶部已有回填，但只读该段的读者仍会看到“两个等价交付物（含已取消的 B-②）” |
| `docs/plans/ai-roadmap-overview.md` | 仅在 `:113`（可用语料）、`:473`（D3 行）、`:478`（D-语料构成行）追加行内 `⚠️ superseded（局部）` | 同上；不动历史正文 |

### 3.2 r4 的验证证据

| 验证 | 命令/方式 | 结果 |
| --- | --- | --- |
| 导出 SQL 可执行 | `mysql`（127.0.0.1:3307/moviedb，只读）跑 `docs/ai/README.md` §2.2 整条 SQL，`m.id IN ('AA1','AA13','tt0094859')` | **通过**：`AA1` → genres `NULL`/stars `NULL`/rating `NULL`；`AA13` → `Comedy` / `David Tree,Esme Percy,Everley Gregg` / `NULL`；`tt0094859` → `Comedy` / `Allan Arbus,Allen Garfield,Ed Lauter` / `7.9` |
| 类型/主演统计 | 同实例只读 SQL（全量分母 + 有 genre/有主演分母，`COUNT(DISTINCT g.name)`、`COUNT(sm.star_id)`） | 21,087 部；类型链接 25,433 → 全量 **1.206**、有 genre 的 17,769 部 **1.431**、**最多 4**、**3,318 部无 genre**；主演链接 124,958 → 全量 **5.926**、有主演的 17,735 部 **7.046**、**最多 46**、**3,352 部无主演** |
| `schema_hash` / `file_sha256` 复算 | `python3` 按 `docs/ai/README.md` §2.3 算法对 `tools[]` 做 `json.dumps(sort_keys, separators)` + `sha256` | `schema_hash = 8eea424b657931c97d8a66a85bf42be4d88d3fb1516cca447c5bd49ef5ab44cd`、`file_sha256 = fabafa2e…a984`、13,958 B、5 工具 → **与记录一致**；`filter_movies`/`recommend_movies` **均已有 `anyOf`**（各含 4 / 5 个 required 分支），`enum` 各 **56** 项 |
| 旧口径扫描 | `grep -rn "1\\.43\\|1\\.73\\|最多 3" docs/` | 只剩“旧实例已作废”的历史说明与 r4 变更行，无残留当前值 |

### 3.3 r4 遗留问题（如实记录）

1. **oracle delta 的 1.36 / 6.08 无法复现**：本次独立复算得到 1.206/1.431 与 5.926/7.046（未列出能得出 1.36/6.08 的分母）；已按**可复现的分母口径**写定两种口径并关闭 `MC-U7`，由主 agent 在合并前确认是否与 oracle 记录冲突。
2. **`tool-schema.b1-5.json` 的 `anyOf` 本已存在**（oracle delta §3-B2 描述为“没有 anyOf”，与磁盘文件不符）；本次**未修改该文件**（`schema_hash`/`file_sha256` 复算一致），只把语义写进 README §2.3 与 `B15b-1`。若主 agent 希望对此留痕，可在 issue 正文注明“已复核”。
3. **仍未关闭**：`OPEN-a2`（截断阈值）、`OPEN-a3`（译名别名表未批准）、`OPEN-a4`（双模板双索引）、`MC-U2`/`MC-U4`/`MC-U6`/`MC-U8`、`OPEN-b1`（≥90% 门槛）、`OPEN-b2`/`OPEN-b3`/`OPEN-b5`、`OPEN-b4`（G3 是否本期做）、`OPEN-U1`/`OPEN-U4`/`OPEN-U5`。
4. **B1.5a 仍未获准发布**：oracle 对 r4 文档**尚未复核**，主 agent 也未标记“可发布”；两份 issue 草稿仍是 🟡 草稿。
5. **未运行的验证**：任何评测脚本、任何工具调用变体（G0–G3）、60 条金标准集标注、`mvn verify`、前端测试 —— 本次均未运行，不得声称已通过。

### 3.4 r4 新增/显著涉及的知识点

| 知识点 | 含义 | 对应位置 | 为什么值得学 |
| --- | --- | --- | --- |
| **MySQL 相关子查询里取“每组前 N 行”** | 用 `(SELECT st.name … WHERE sm.movie_id = m.id ORDER BY st.name LIMIT 3)` 作为派生表，再在外层 `GROUP_CONCAT` 聚合；没有窗口函数也能表达“每部片取 3 行” | `docs/ai/README.md` §2.2 | 导出哈希这类“必须能跑、必须稳定”的 SQL，比“看起来更聪明”的写法（`ROW_NUMBER` + 不存在的列）更重要 |
| **哈希口径必须与语义口径分离** | `db_snapshot_hash` 只需要**稳定可复现**，不必复刻业务 top-3；两者混用会让“换业务排序”意外改变所有历史 hash | 同上 §2.2 说明段 | 指纹的目的是一致性，不是业务正确性 |
| **`required` vs 对象级 `anyOf`** | “至少一个条件”用 `anyOf`（多个分支各自 `required` 单字段）；若写成 `required` 会强迫模型每次填全字段，反而制造幻觉参数 | `tool-schema.b1-5.json`、`b1-5b` §2.1/§9 | JSON Schema 的“必填”与“至少一个”是两种不同约束，验收条文必须说清是哪一种 |
| **统计数字必须带分母** | “平均 1.2 个 genre/片”若不说分母（全部电影 vs 有 genre 的电影），差 18%；文档必须写“全量 / 条件内”两个口径 | `movie-cards.md` §1.2、`MC-U7` | 评测报告里最常见的不可复现原因就是分母漂移 |
| **主指标与验收门分离** | 跨路线比较用“任务级成功”（共享定义），但 A 的 pass/fail 必须是 D8 原文的 `recall@5`；两者混用会出现“任务级 78% 但 top-5 82%”无法判定的情形 | `b1-5a` §5.1、umbrella §3.2 | 决策规则必须先固定“用哪个数判定”，否则数据出来后必然被重新解释 |

---

## 4. r5 / 数据事实修正（同日，评分覆盖起点 2000 → 2001）

> 触发：3307 实测发现 **评分最早覆盖 2001 年**（不是 2000）。仍为**纯文档 + 契约描述修正**：未改源码/测试/配置、未提交、未改远端 issue。**本节取代 §3 中记录的两个 hash 值。**

### 4.1 实测证据（只读 SQL，`fabflix-mysql` 主机端口 3307，库 `moviedb`）

| 查询 | 结果 |
| --- | --- |
| `MIN(m.year)` / `MAX(m.year)` over `movies ⨝ ratings` | **2001** / 2024 |
| `COUNT(*)` rated movies | **8,005**（= `ratings` 行数，无孤立评分行） |
| `COUNT(*)` rated where `year = 2000` | **0** |
| `COUNT(*)` rated where `year = 2001` | 1,099 |
| `COUNT(*)` unrated where `year BETWEEN 196 AND 2000` | **11,975**（= 196–1999 的 11,917 + 2000 的 **58**） |
| `COUNT(*)` movies where `year = 2000` | 58 |

→ 结论：**196–2000 全部无评分；评分自 2001 年起**。旧表述“只覆盖 2000 年及以后 / 2000+ / 2000-onwards”**错误**。

### 4.2 逐文件改动

| 文件 | 改动 |
| --- | --- |
| `docs/ai/tool-schema.b1-5.json` | `db_snapshot.ratings_covered_from_year` **2000 → 2001**；`db_snapshot.notes` 与 `filter_movies.min_rating.description` 的 “2000-onwards / pre-2000” → **2001-onwards / pre-2001**；新增 `revised` 元数据；`contract_version` **v1 → v2**；`schema_hash` **重算**（`8eea424b…44cd` → **`8e72f741…a567`**）、`file_sha256` → **`b54b0f28…f2c5`**（14,225 B） |
| `docs/ai/README.md` | §2.2 快照性质改为 **2001 年及以后**（含 8,005 部有评分 / `MIN(year)=2001` / 196–2000 11,975 部无评分）；§2.3 的 `contract_version` 与三个 hash 值更新为 v2 现值；新增说明“r3 改了 `tools[]` 内文本 → hash 变化”；§4 修订记录新增 r3 行 |
| `docs/architecture/movie-cards.md`（r5） | §1.1、§1.2、§1.2.1、§3、§3.1 与 §6 `ratings_covered_from_year` 全部改 2001；把“196–1999 全部无评分”补正为 **196–2000（11,975 部）**；§8 新增 r5 行；`last_updated` 前缀 r5 |
| `docs/architecture/ai-user-workflows.md`（r4） | `R2-4`、`R3-2`、`R7-1`、§3 数据边界、§4.2 评分覆盖行、`U4` 权限行、§8 r3 行的 “2000 年及以后 / 2000+” → **2001 年及以后 / 2001+**；`coverage.rating_coverage_from` **2000 → 2001**（4 处）；fixture 版本引用改 v2；§8 新增 r4 行 |
| `docs/plans/b1-5a-movie-card-embeddings.md`（r5） | §2.2 数据陷阱 1（评分自 2001、196–2000 11,975 部无评分）、§9 **README 第 13 行替换文本**（`2001-onwards` / `cannot match 2000 or earlier`）、§10 `B15a-5`、§12 风险表；§15 新增 r5 行 |
| `docs/plans/b1-5-movie-retrieval-eval.md`（r5） | §2 `无评分片` / `ratings` 两行、三条硬事实 1（`rating_coverage_from=2001`、`1960s–2000`）；顶部新增 r5 段 |
| `docs/plans/b1-5b-tool-routing.md`（r5） | §2.2 快照行、§3.1 `EMPTY_RESULT` 行、§7 `S3` 覆盖检查“评分有效起点 2001”、§11 风险表；§2.1 `contract_version` v2；§14 新增 r5 行 |
| `docs/issues/proposed-b1-5b-tool-routing.md` | Background 的 “2000-onwards / 11,917 from 1960–1999” → **2001-onwards / 11,975 from 196–2000**；Risks “Pre-2000” → **Pre-2001**；内部表 `schema_hash`/`contract_version` 更新为 v2 现值 |
| `docs/plans/ai-roadmap-overview.md` / `ai-option-b-spring-ai-rag.md` | 2026-11-09 回填块中的“评分只覆盖 2000+”→ **2001+**（附“二次核定”括注） |
| `docs/issues/issue-69-b1-5a-movie-card-embeddings.md` | **不修改已发布的 `## GitHub issue body`**（保持与远端 #69 一致）；仅在 `## Internal tracking` 顶部加 ⚠️ 待远端同步说明：正文 AC-5 与 Risks 仍写 2000-onwards，正确口径是 2001+，如需修正远端由主 agent 交 `github-operator` |

### 4.3 未改动 / 需他人处理的项

1. **`docs/ai/golden/*.json`（worker 负责）已核对一致**：`queries.b1-5.json` 与 `negative.b1-5.json` 的 `snapshot.ratings_covered_from_year` 均为 **2001**，`db_snapshot_hash = 8b16c047…`；Q09 `labeling_note` 已写“评分约束只用于 2001+”；N01 已写 “year>=2001”。**本次未修改 golden 文件**。
2. **远端 issue #69 正文仍是 2000-onwards**（本地镜像同样保持原样）：需要主 agent 决定是否让 `github-operator` 追加评论/编辑正文。
3. **`contract_version` 已 bump v1 → v2**：因为 `schema_hash` 覆盖 `tools[]`，而 `filter_movies.min_rating.description` 的日期文本在 `tools[]` 内，任何文本修正都会改变 hash。故“schema_hash 不变”的前提不成立；已按 `docs/ai/README.md` §3 规则 1 处理并在 §4 记录新旧 hash。**若主 agent 认为不应 bump 版本号，只需回退 `contract_version` 字符串（hash 仍是新值）。**

### 4.4 验证

- `python3` 复算：`schema_hash = 8e72f741…a567`（= 文件内记录值）、`file_sha256 = b54b0f28…f2c5`、14,225 B、JSON 可解析。
- 全库扫描（`docs/**`，排除 golden/历史日志）：无 “2000 年及以后 / 2000+ / 2000-onwards / pre-2000 / rating_coverage_from=2000” 残留；剩余 “2000” 均为“2000 年 58 部全无评分 / 196–2000 / 2000s 年代统计”等正确表述。
- 10 个被改文件的 Markdown 表格列一致性扫描：全部 OK。

---

## 5. B1.5a 实测完成后的文档回填（2026-11-09，第五轮；纯文档）

> 触发：B1.5a（issue #69）**评测实施完成**，报告 = `docs/ai/reports/b1-5a-embedding-eval.md`（实测，**D8 FAIL**），三路 review 要求回填文档状态/契约。
> 边界：**只改 `docs/`**；未改报告正文、未改 `README.md`、未改源码/脚本；未提交；**未建 issue/PR**；**不声称 B1.5b 已实施**。

### 5.1 实测结论（回填的唯一事实来源）

| 项 | 值 |
| --- | --- |
| D8（`recall@5` ≥ 80%，唯一 pass/fail） | `bge-m3` **0.4833（29/60，MRR@10 0.4334）**；`qwen3-embedding:0.6b` **0.4167（25/60，MRR@10 0.3134）** → **两者均 FAIL**；并集 53.3% 仍 < 80%；阈值/分母未改 |
| 四组合（非判定口径） | `bge`×`zh-full` 0.5167（31）／×`en-short` 0.4833（29）；`qwen3`×`zh-full` 0.3833（23）／×`en-short` 0.4333（26） |
| 分意图（配对口径） | 片名 13/15 vs **15/15**；filter 9/20 vs **2/20**；similar **3/15 vs 3/15**；推荐 4/10 vs 5/10 |
| fingerprint | `template_hash` `c5990dc4…`（zh-full）/ `bc3bcce6…`（en-short）；`db_snapshot_hash` `8b16c047…`；`golden_sha256` `0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079`；`schema_hash` = N/A |
| `OPEN-a2` | 关闭：`bge-m3` **2,048** tokens、`qwen3` **4,095** tokens；659 batch × 4 组合**截断 0 次** |
| `README.md` | 实改 **13/14/16** 三行（13 = structured search + “semantic similarity **under evaluation**（`recall@5` 0.48 < 0.80）” + 报告链接 + `2001-onwards`；14 = 用户侧只做电影目录；16 = 推荐归属用户侧目标、多轮推迟） |

### 5.2 逐文件改动

| 文件 | 改动 |
| --- | --- |
| `docs/ai/README.md`（r4） | 修正金标准集 sha256 误写（原缩写 `0c2f4c5e…eacf` → 实际 `0c2f4c5e…be079`，并给全值）；§1 状态表把 `golden/queries`、`golden/negative`、`reports/b1-5a-embedding-eval.md` 标为**已完成/已冻结/已使用**并写入 D8 FAIL 数字；§2.3 增“B1.5a 已完成、`schema_hash=N/A` 已显式写出”与 fingerprint 指向；§4 增 r4 行 |
| `docs/issues/issue-69-…md`（r7） | 实施状态 `⬜ 未开始` → ✅ **已完成（评测，D8 FAIL）**；`last_updated` r7；内部表新增「评测结果（0.4833/0.4167）」「分意图实测」「报告链接」「`README.md` 13/14/16 改动」四行；`OPEN-a2` 关闭；`OPEN-a4` 以实测数据更新（bge `zh-full` 31 ≥ `en-short` 29 且逐轨道 ≥；qwen3 26 > 23）；状态行/计划来源同步 |
| `docs/plans/b1-5a-…md`（r6） | 头部 → 已完成（D8 FAIL）；`实施状态` → ✅；§11 每个产物标注实际结果；§13 `OPEN-a2` 关闭、`OPEN-a4` 用实测；§14 增“实施（worker）/三路 review”行并重写“不声称”段；**新增 §16 实施结果摘要**（D8 表、分意图、fingerprint、README 三行实际 diff、未关闭项）；§15 增 r6 行 |
| `docs/plans/b1-5-movie-retrieval-eval.md`（r6） | 顶部 r6 段（D8 FAIL 0.4833/0.4167、片名强/filter·similar 弱、**A 不得单独交付**、B1.5b 未开始）；§4 决策表 A 列回填实测（任务级/D8、延迟 35–59 ms 热 · 1.37–2.37 s 冷、构建 29.4 min、中文能力不是头号风险）；决策规则 1 标注“已触发”；§6 进度、§7 memo“现在无法完成”、§9 文档地图状态 + 新增报告行；§12 状态与“不声称”段 |
| `docs/architecture/movie-cards.md`（r6） | §2.2 用实测替换外推：tok/卡 `zh-full` **47.2/50.7**、`en-short` **26.8/29.1**；构建 **5.3–8.9 min/组合**（合计 29.4 min）；截断 **2048/4095**（原“必须重测 / 待 `OPEN-a2`”已删）；§6 回填 `template_hash` `c5990dc4…`/`bc3bcce6…`、`db_snapshot_hash` `8b16c047…`、模型 digest、`build_time_s`；§4 查询成本改实测；§8 增 r6 行 |
| `docs/architecture/ai-user-workflows.md`（r5） | `find_similar_movies` 契约明确 **`results[]` 必须排除 seed**（§3 U3 + §4.2，与 `recommend_movies.exclude_seed` 同义）；**新增 §1.5「B1.5a 实测输入」**（filter/similar 在 dense 卡片上不可用 → `U1`–`U4` 必须由结构化工具承担，B 路线价值来源；seed 排除理由）；§8 增 r5 行 |
| `docs/plans/b1-5b-tool-routing.md`（r5） | **新增 §1.4「A 路线实测输入」**（D8 FAIL、能力分布、归因、seed 排除的三条实施要求、不过度外推）；§2.2 增 `find_similar_movies.results[]` 排除 seed 的返回约束；状态明确 **尚未开始**；G1 必做/`required`-`anyOf`/阈值 `OPEN-b1` 均不变；§14 增 r5 行 |
| `docs/plans/ai-roadmap-overview.md` | 顶部回填新增第 **8** 条（B1.5a 成功/失败结论、README 13/14/16 已改、下一个 = B1.5b）；里程碑修正段追加 ⑥⑦⑧（M4.1 前置只完成一半、A 承担相似电影的兜底 superseded、B1.5b 未开始）；4 处“语义检索承担相似电影/直入口”表述加行内 `⚠️ superseded` |
| `docs/plans/ai-option-b-spring-ai-rag.md` | 顶部回填新增第 **8** 条（同上）；“不声称”句加实测例外；5 处“B-① 语义检索承担相似电影 / 承担 B-③ 直入口”表述加行内 `⚠️ superseded`（含风险表与能力表） |

### 5.3 验证

- 报告与回填数字逐项对照（D8、四组合、分意图、fingerprint、tokens、截断、构建、`OPEN-a4` 数据）——一致。
- `sha256` 复算：`golden/queries.b1-5.json` = `0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079`（原 `…eacf` 缩写错误）、`negative.b1-5.json` = `09725672…75fd`。
- `git diff README.md`：**仅 13/14/16 三行**（与文档描述一致）。
- 9 个被改文件的 Markdown 表格列一致性扫描：全部 OK（行内 superseded 标记已统一放在行尾/末列，未破坏表格与编号列表）。
- `git status`：仅 `docs/**` 改动（+ worker 的 `scripts/`、`.idea/`）；**无暂存**；**未提交**。

### 5.4 遗留

1. **B1.5b 尚未开始**：无脚本/报告/issue；`OPEN-b1`（≥90% 门槛）仍待用户确认。
2. **决策备忘录无法完成**：缺 B 路线实测（2×2 配对与 B 侧门槛）。
3. `OPEN-a3`（别名表未批准）、`OPEN-a4`（数据已给，最终选择待主 agent）、`MC-U6`（genre 规范化）、`MC-U8`（空主演占位符仍暂定）、金标准集 negative 根形状、只跑一轮/单人标注/n=60 的方向性限制。
4. 远端 issue #69 的 checkbox 是否勾选未核实（本文件不改远端）。

---

## 6. 三处文档一致性修正（2026-11-09，第六轮；纯文档）

> 触发：回填后仍有三处与实测/已落地事实不一致。只改 `docs/`；**未改报告正文**、未改 README、未提交。

| 位置 | 修正前 | 修正后 |
| --- | --- | --- |
| `docs/issues/issue-69-…md` `OPEN-a4`（L140） | “…若复活 A 路线，倾向**只建 `en-short`**（单一索引即可覆盖三轨道，且两个模型都不吃亏）” | “**模板优劣因模型而异，没有普适的单一模板**；双索引收益不足以支撑 **+82.4 MiB** 与约 **2× 构建成本** → **最终选择权在主 agent**”（`last_updated` → r8） |
| `docs/plans/b1-5a-…md` §13 `OPEN-a4`（L307） | “→ 若复活 A 路线，**单一 `en-short` 索引**即可覆盖三轨道且对两个模型都不吃亏（省一半构建与内存）” | 同上结论（`bge-m3` `zh-full` 31/60 且逐轨道 ≥ `en-short` 29/60；`qwen3` 相反 26 > 23）；§15 增 r7 行，`last_updated` → r7 |
| `docs/plans/ai-roadmap-overview.md` L21（回填第 7 条） | “第 **13/14** 行需在 **B1.5a 的 PR** 中替换…**本次不改 README**”（与 L22 “已改” 自相矛盾） | “第 **13/14/16** 行的替换**已随 B1.5a 的 PR 完成**（13/14 按 §9 + 实测 `2001-onwards` + 标注 “under evaluation”；16 行按 D-7）；实际 diff = 仅这三行，见报告 §14.1” |
| 附带 | `docs/plans/ai-roadmap-overview.md` 回填列表出现两个 “8.” | 后一条“新文档入口”改为 **9.** |
| 附带 | `docs/plans/b1-5-movie-retrieval-eval.md` 流程图 “PR 内应用 README 第 13/14 行替换” | 追加 “✅ 已完成：实际改 13/14/16 行（见 `b1-5a` §16.4 与报告 §14.1）” |

**核对（`docs/ai/reports/` 端点）**：`grep -rn "192.168\|11434" docs/ai/reports/` → 仅 `http://<host>:11434` 占位符（报告 §0 与复现命令），**无真实 IP/主机地址**；`docs/ai/` 其余文件亦无端点。报告正文未改（worker 的 `http://<host>:11434` 版本保留）。

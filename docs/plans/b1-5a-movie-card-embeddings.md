# B1.5a — 电影卡片 embedding 检索质量评测（A 路线）

> **文档状态：🟢 已批准（r4 内容）+ issue #69 已创建（2026-11-09）+ **评测实施已完成（2026-11-09）——D8 FAIL**。** 本文件的计划内容仍有效；**实测结果不写在这里，见 `docs/ai/reports/b1-5a-embedding-eval.md`**（§16 只做摘要与结论回填）。§2/§3/§9 的事实与 §16 的实测数字都有来源；其余表格（成本外推等）已在 §16/`movie-cards.md` §2.2 被实测替换。
>
> | 元数据 | 值 |
> | --- | --- |
> | 文档类型 | 计划 / 方案（评测 + 质量结论；**只做 A 路线**） |
> | `last_updated` | **2026-11-09**（**r7：`OPEN-a4` 结论改为“没有普适单一模板”**——`bge-m3` 的 `zh-full` 逐轨道 ≥ `en-short`、`qwen3` 相反，故不能推荐“只建 `en-short`”；双索引收益不足以支撑 +82.4 MiB 与约 2× 构建成本，选择权在主 agent。**r6：回填实施完成状态**——报告 `docs/ai/reports/b1-5a-embedding-eval.md` 实测完成、**D8 FAIL（`bge-m3` 0.4833 / `qwen3` 0.4167）**；`OPEN-a2` 关闭（2048 / 4095 tokens）；`OPEN-a4` 以实测数据裁定；`README.md` 13/14/16 行已改；新增 §16 实施结果摘要。r5：回填 GitHub issue **#69** 编号/URL/milestone、把状态从“待复核”改为“已批准 / 已建 issue / 待实施”、§14 增 issue 行；**未回填任何实现结果**。r4：D8 门槛口径统一为 `recall@5`；卡片统计改实测值；E3 措辞；关 `OPEN-a1`/`OPEN-a5`） |
> | 基线 | `main` @ `85d4675`（PR #68 合并后）；工作区另有未跟踪的 `.idea/` 与 `docs/` 未提交改动（本任务产物） |
> | 上级 | `docs/plans/b1-5-movie-retrieval-eval.md`（**umbrella / 决策备忘录**：共享数据集、共享指标、决策标准） |
> | 兄弟 | `docs/plans/b1-5b-tool-routing.md`（**B 路线**：结构化工具/路由；本文件**不碰工具与路由**） |
> | 批准依据 | 用户 2026-11-09 批准 **D-1 / D-2 / D-3 / D-4 / D-7 / D-9 / B-4 / B-9**（见 umbrella §决策表） |
> | 关联规范 | `docs/architecture/movie-cards.md`（卡片数据契约 + fingerprint）、`docs/architecture/ai-user-workflows.md`（用户可见话术与"无中文译名"边界） |
> | 评测契约产物 | `docs/ai/README.md`（hash 规则）、`docs/ai/golden-set.schema.json`（金标准集 schema）、计划中定义的 `docs/ai/golden/queries.b1-5.json`（60 条，与 B1.5b **共享**） |
> | 实测证据 | 本节 §2 的 DB 快照 = 2026-11-09 只读 SQL（容器 `fabflix-mysql`，宿主端口 3307）；模型事实 = Ollama `/api/tags` + `/api/show` + `/api/embed`（2026-11-09）；历史证据 `.pi/artifacts/scout-b1-5.md`、`docs/logs/2026-10-24-b0-local-llm-spike.md` |
> | GitHub issue | ✅ **#69** — https://github.com/PNZ-z/ai-powered-Fabflix/issues/69（milestone **#3**、**无 labels**，2026-11-09 创建）；规范记录 = `docs/issues/issue-69-b1-5a-movie-card-embeddings.md` |
> | 状态 | 🟢 **已批准 / 已建 issue #69 / 实施完成（评测）；A 路线判定 FAIL** |
> | 实施状态 | ✅ **已完成（2026-11-09，仅评测）**：`scripts/eval/embed_eval.py`、`check_golden.py` 已跑；`docs/ai/golden/queries.b1-5.json`（60 条，sha256 `0c2f4c5e…be079`）与 `negative.b1-5.json` 已冻结；**报告 = `docs/ai/reports/b1-5a-embedding-eval.md`（D8 FAIL：0.4833 / 0.4167）**；`README.md` **第 13/14/16 行已改**（13 行另按实测标注“dense 相似检索 under evaluation / `recall@5` 0.48 < 0.80”并链到报告；16 行为 D-7 推荐定位修正）。**未交付任何用户功能**（本 issue 只交评测） |

---

## 1. 目标与非目标

### 1.1 目标（一句话）

在进入 B-① 之前，用**可复现的评测**回答：

> **把电影卡片文本做 embedding 后，中文/英文查询能否稳定检索到"用户可接受的电影"？** —— 能，则 A 路线（含 A 作为混合方案的检索内核）可作为 B-① 的候选架构；不能，则在决策备忘录里如实记为不可行，不交付。

本 issue **只产出数字与结论**，不交付任何用户功能。

### 1.2 In scope

1. 在**完整数据集快照**（21,087 部，见 §2）上构造 `movie-card/v1` 卡片文本（`zh-full` 主测、`en-short` 对照）。
2. 用**两个** embedding 候选（`bge-m3`、`qwen3-embedding:0.6b`，均已安装）分别建索引并跑同一套 **60 条**金标准集。
3. 记录 **`recall@5`（D8 判定指标）** / `MRR@10` / **任务级成功率（次要，解释用）** / 延迟（冷、热）/ 构建耗时 / tokens / 向量内存 / 零结果率 / 静默截断次数。
4. 记录**三个 fingerprint**（`template_hash`、`db_snapshot_hash`、`schema_hash` 不适用 → 写 N/A 并说明）。
5. 评测报告 + 供 umbrella 决策备忘录使用的 A 路线结论（通过/不通过 D8 阈值）。

### 1.3 Out of scope（硬边界）

| 不做 | 理由 |
| --- | --- |
| 工具 / tool calling / 路由 / 拒绝话术评测 | 属 **B1.5b**；本 issue 不调用 `qwen2.5:7b` 的 `tools` |
| MCP server、`@Tool`、任何 `src/main/**` 代码 | 属 B-①/后续；本 issue 仅脚本 + 文档 |
| 生产 UI / API 端点 / 向量库依赖进生产 | 属 B-①，需另批准 |
| 开发文档作为语料（`docs/**`、`README.md` 的切分/embedding/索引） | **用户已批准 D-1/D-5：开发文档不是用户语料**，本 issue 只索引电影卡片 |
| 修改 `README.md` | 不在本 issue 执行；**精确替换文本见 §9**，由 B1.5a 的 PR 应用 |
| 修改 `pom.xml`、CI、任何已批准 B1/B0 文档 | 保持范围可判定 |
| 中文译名别名表 / 外部影片数据导入 | 未批准的**新数据来源**；见 §12 `OPEN-a3` |

---

## 2. 数据契约与快照（2026-11-09 实测，覆盖旧口径）

> **用户已批准 D-3**：测量数据源 = Docker `fabflix-mysql`（宿主端口 **3307**，库 `moviedb`）的**完整数据集**。9,052 部的本地 3306 实例**仅作历史/对照说明**，不再作为主数据。

### 2.1 快照事实（只读 SQL，2026-11-09）

| 项 | 值 | 来源 |
| --- | --- | --- |
| `movies` | **21,087** | `SELECT COUNT(*) FROM movies` |
| `genres` | **61 行 / 56 个 distinct 名称** | `COUNT(*)` / `COUNT(DISTINCT name)` |
| `genres_in_movies` | 25,433（distinct `(movie_id,name)` 亦 25,433） | 去重后无重复对 |
| `stars` / `stars_in_movies` | 76,259 / 124,958 | — |
| `ratings` | 8,005 行 | — |
| 年份范围 | **196–2024**（无 `year IS NULL` 或 `0`） | `MIN/MAX(year)` |
| 1990–1999 | **2,370** 部 | 按年代分组 |
| 其它年代 | 1980s 1,713 / 1970s 1,391 / 2000s 9,069 / 2010s 94 / 2020s 7 | 同上 |
| 无评分片 | **13,082**（占 62%） | `LEFT JOIN ratings WHERE rating IS NULL` |
| 平均/最大类型数 | **全量 1.2061，有 genre 的 17,769 部内 1.4313，最多 4**（分母口径已定，见 `movie-cards.md` `MC-U7`） | 同上（25,433 条链接 ÷ 21,087 部） |
| 无 genre 的片 | **3,318** 部（**必须能生成 0-genre 卡片**，模板规则见 `movie-cards.md` §2.1） | 同上 |
| 平均/最大主演数 | **全量 5.9258，有主演的 17,735 部内 7.0458，最多 46**；**3,352 部无主演行** | 同上（124,958 条链接 ÷ 21,087 部） |
| `rating` 全域 | 1.2–9.7，均值 6.15 | — |

### 2.2 三个"必须写进报告"的数据陷阱（实测，直接影响评测设计）

1. **评分只覆盖 2001 年及以后**：按年代统计，196–2000 的 **11,975 部电影全部没有评分行**（`rated=0`；其中 2000 年 58 部全部无评分，实测定界 `MIN(year)` among rated = **2001**、8,005 部有评分），2000s 有 7,918/9,069 有评分（全部落在 2001–2009）、2010s 83/94、2020s 4/7。
   → 任何"1990 年代 + 评分 ≥8"的查询在**本快照上必然空结果**（实测 `year BETWEEN 1990 AND 1999 AND rating>=8` = **0**）。金标准集**不得**把"旧片 + 评分门槛"当正样本，且该事实必须写进报告与 `ai-user-workflows.md` 的覆盖说明。
2. **`id` 不是统一的 `tt` 编号**：21,087 部里只有 **9,073** 个以 `tt` 开头；其余形如 `AWw10`（`The Matrix`，1999）、`AA1`。
   → 卡片/工具契约一律把 `id` 当**不透明 `VARCHAR(10)` 主键**处理，禁止假设前缀格式；旧文档"`id`（`tt` 编号）"的写法**已修正**（`movie-cards.md` §1.1）。
3. **类型名存在重复行与语义变体**：`Action/Comedy/Drama/Horror/Thriller` 各有 **2 行**（id 1–23 与 9001–9006 两套）；`Science Fiction`(id 9005) 与 `Sci-Fi`(id 19) 是语义变体但**影片数极不对称**（`Sci-Fi` **457** 部 vs `Science Fiction` **2** 部）。另有大量长尾/可疑名称：`anti-Dram`、`AvGa`、`Disa`、`Psyc`、`Porn`、`Porb`、`CA`、`Scat`、`Homo`、`TVmini`、`verite`、`genre1` 等。
   → **枚举必须由 DB 派生，不写死**（B-3）；去重/规范化规则见 `ai-user-workflows.md` §4.2；本 issue 的卡片文本**直接使用 DB 原值**（不规范化），并把该事实记入报告（否则会把"检索没命中"误判为模型问题）。

### 2.3 卡片（唯一语料 = 电影卡片）

- 字段集与模板：`docs/architecture/movie-cards.md` §2（`movie-card/v1`，`zh-full` 主测、`en-short` 对照）。
- **`zh-full` 的真实语义**：数据库片名/人名/类型名**全是英文值**，所以"中文卡片"= **中文模板 + 英文值**。报告不得把它包装成"中文语义检索通过"。
- 卡片文本**不合并多片**（避免 2,048-token 静默截断污染，见 §8 风险）。
- 卡片文本变更 → 必须 bump `card_version` 并重算 `template_hash`。

---

## 3. 模型候选（两个，均已安装并实测）

| 候选 | 安装事实（2026-11-09 `/api/tags` + `/api/show`） | 维度 | 角色 |
| --- | --- | --- | --- |
| **`bge-m3:latest`** | 1,157,672,605 B；family `bert`；F16；`context_length=8192` | **1024**（B0 实测） | 候选 1（既有默认） |
| **`qwen3-embedding:0.6b`** | 639,150,858 B；family `qwen3`；595.78M 参数；**Q8_0**；`context_length=32768`；`capabilities: tools/thinking/embedding` | **1024**（`/api/show` 的 `qwen3.embedding_length=1024`，且 `/api/embed` 实返 1024 维、L2≈1.0） | 候选 2（**用户已批准 D-2 拉取，已完成**） |

- **两个候选维度相同（1024）** → 向量内存与索引结构一致，可直接对比质量差异，无需按维度归一化。
- `qwen3-embedding:0.6b` 的**延迟、冷启动、吞吐均未测** → 必须在本 issue 内实测（不得沿用 `bge-m3` 的 7.64 s 冷启动数字）。
- 其它候选（`multilingual-e5-*`）在 Ollama 库中**不存在**（404，scout §C.2）→ 不得再列入候选清单。

---

## 4. 60 条金标准集（与 B1.5b **共享同一份**）

### 4.1 组成（用户已批准 B-4）

| 轨道 | 条数 | 定义 | 期望标注 |
| --- | --- | --- | --- |
| `zh→zh` | **20** | 中文查询 → `zh-full` 卡片（值仍为英文） | 可接受集合（`movie_id[]`） |
| `zh→en` | **20** | 中文查询 → `en-short` 卡片 / 英文实体（含**中文惯用译名**，如"黑客帝国"） | 可接受集合 |
| `en→en` | **20** | 英文查询 → `en-short` 卡片 | 可接受集合 |

- **总 60 条**，文件：`docs/ai/golden/queries.b1-5.json`（**B1.5a 与 B1.5b 共用**）；schema：`docs/ai/golden-set.schema.json`。
- 每条必备字段：`id`、`track`、`query`、`lang`、`intent`、`acceptable_movie_ids[]`、`labeling_note`、`snapshot_row_refs`（用于证明该集合在快照内可命中）。
- **正样本必须在 21k 快照内可命中**：标注时用只读 SQL 逐条验证 `acceptable_movie_ids` 每个 id 存在且满足语义（如类型词必须在 56 个 distinct 名称内、年份在 196–2024）。
- **采样规则（覆盖层，避免全挤在易命中的头部）**：按意图分布 ≈ 片名解析 15 / 类型+年代筛选 20 / 相似电影 15 / 偏好推荐 10；按年代至少各含 1980s、1990s、2000s 样本；必须包含 **≥3 条"仅靠译名/别名才能命中"的中文用例**（预期多数失败 → 用于暴露真实边界，不计入"不得失败"）。
- **9,052 / 2001–2019 的旧口径**只作历史对照：报告可列"旧实例上该条会/不会命中"，但**不得**作为评分依据。

### 4.2 标注纪律

1. **可接受集合**而非唯一答案（"相似""推荐"多解）；每条的判定依据（共享 genre / 年代 / 同导演 / 同主演 / 评分区间）写进 `labeling_note`。
2. **反例 5 条**（计入 60 条之外，单独一组）：剧情类、纯中文译名、跨数据能力（获奖/票房）、空结果可解释场景 —— 用于证明边界，不作为分母。
3. 标注完成后**冻结**并计算文件 SHA-256，写入报告；标注变更必须 bump `golden_version`。

---

## 5. 指标（D8 判定指标 + 次要指标）

### 5.1 判定指标的分工（2026-11-09 已裁定：D8 gate = top-5）

> **已裁定（`OPEN-a1` 关闭）**：B1.5a 的 **pass/fail 只看 D8 原文口径—— `recall@5` / top-5 命中率 ≥80%**。
> **任务级成功率是次要指标**，只用于 A/B 配对解释与失败模式归因，**不纳入 A 的通过条件**。理由：A 路线返回的是 DB 卡片，接地天然成立；把“LLM 最终答案”纳入判定会把 A 的能力绑到 B 的链路上，而 D8 原文问的就是“目录语义检索 top-5 命中”。

单条 query 的**任务级成功（次要指标）**定义（A/B 同口径，用户已批准 B-4）：

> 单条 query 判 **成功** 的条件（两者同时满足）：
> 1. 该 query 的 `acceptable_movie_ids` 中 **至少 1 个** 出现在**最终结果集**（A 路线 = 返回的 top-5 卡片集合；B 路线 = 最终答案中列出的电影集合）；
> 2. **接地**：出现的每个 `title`/`year`/`director`/`genres`/`stars`/`rating` 都与 DB 值一致（回查校验，零容忍）。
>
> 报告主表按以下顺序呈现：① **`recall@5`（判定用）** ② `MRR@10` ③ 任务级成功率（解释用）；分轨道（zh→zh / zh→en / en→en）+ 总体。

### 5.2 其余指标（记录/解释用）

- **`MRR@10`**（首个可接受结果的倒数排名）——解释用；`recall@5` 是 §5.1 的判定指标，**下移到本小节之外**。
- **零结果率**、**截断次数**（`prompt_eval_count` ≥ 阈值即判失败）。
- **延迟**：query embedding 的 p50/p95，**分"模型已加载"与"冷启动（`keep_alive:0` 后首调）"两组**，每组 ≥20 次。
- **构建**：总耗时、tokens、稳态 tok/s、最坏 batch（记录 13× 离群值）。
- **内存**：每 (模型×模板) 组合的向量占用 = `rows × dim × 4 B`。

### 5.3 预注册阈值（沿用已批准口径，不新造）

| 门 | 阈值 | 出处 |
| --- | --- | --- |
| 目录语义检索 **`recall@5`（top-5 命中率）** | **≥80%** | roadmap D8（用户 2026-10-19 确认）；2026-11-09 裁定为 **B1.5a 唯一 pass/fail 依据** |
| 任务级成功率 | **不作门槛**（次要指标，仅解释） | 本文件 §5.1（`OPEN-a1` 已关闭） |
| 交付形态 | 若任一候选未达阈值 → 报告如实记"未达标"，**不得**调低阈值或换分母 | 本文件 §10 `B15a-6` |

> 阈值来自已批准决策，**不是**本文件新提的数。已裁定的分歧点（“能否用任务级成功替代 top-5”）**已关闭**：不能，D8 gate 固定为 `recall@5` ≥80%；报告仍同时给出 `recall@5` / `MRR@10` / 任务级成功率三个数，但**只有第一个决定 pass/fail**。

---

## 6. 成本与资源（**外推，未实测**）

单卡实测 token（`bge-m3` 的 `prompt_eval_count`）：`zh-full` ≈ **48.0**、`en-short` ≈ **25.3**。

| 组合 | 卡片数 | tokens | 构建耗时（理想 / 预算） | 向量内存（float32, 1024 维） |
| --- | --- | --- | --- | --- |
| 21,087 × `zh-full` | 21,087 | ≈ **1.01 M** | 7–9 min / **15–20 min** | ≈ **86 MB** |
| 21,087 × `en-short` | 21,087 | ≈ **0.53 M** | 4–5 min / — | ≈ **86 MB** |
| 单模型两模板 | — | ≈ **1.55 M** | 11–14 min / 25–35 min | 串行 → 峰值 86 MB |
| **两模型两模板（4 组合）** | — | ≈ **3.09 M** | 21–28 min / **45–60 min** | 串行 → 峰值 86 MB；同时驻留 ≈ 345 MB |

- 依据：稳态 **1,860–2,450 tok/s**；**必须**为 13× 离群值留余量（scout §C.4/E）。
- **以上全部是外推**（本快照上从未跑过完整构建）；报告必须把实测值与外推值分栏。
- 模型驻留：`qwen3-embedding:0.6b`（639 MB）+ `bge-m3`（1.16 GB）；`keep_alive` 默认 5 min 会反复冷启动。

---

## 7. 可复现性（三个 fingerprint）

| hash | 定义（精确算法见 `docs/ai/README.md` §2） | 本 issue 是否产出 |
| --- | --- | --- |
| `template_hash` | `sha256(card_version \| template_id \| 模板字面串 \| 字段顺序 \| null 占位符)` | ✅ 每个模板一个 |
| `db_snapshot_hash` | 按 `movie_id` 排序的逐行 `sha256(归一化字段以 \u001f 连接)` 串接后再 `sha256`；附 `row_count / year_min / year_max / genre_distinct / null_rating_rows` | ✅ |
| `schema_hash` | `sha256(canonical JSON of tools[] )` —— 本 issue **不使用工具** → **N/A**（显式写 N/A，不得留空） | N/A |

- 报告的每个数字块必须带 `(template_hash, db_snapshot_hash, golden_sha256, model+dim, card_version)`。
- **无法复现时必须明说**：例如端点未开机、模型被替换（digest 变化）→ 报告写"未复现 + 原因"，**不得**用旧数字冒充实测。

---

## 8. 执行与验证计划

| 步骤 | 内容 | 位置 | 阻塞 |
| --- | --- | --- | --- |
| E1 | 连通性检查（端点可达、模型 digest 记录）；macOS 本地网络权限假故障先排除 | 本机 | 是 |
| E2 | `db_snapshot_hash` 计算 + 快照元数据落盘 | 本机（只读 SQL，3307） | 是 |
| E3 | 卡片生成（**2 模板**）+ **2 模型 embedding（共 4 组合）**，断言 `prompt_eval_count` 未触发截断 | 本机 | 是 |
| E4 | 金标准集 60 条 schema 校验（`docs/ai/golden-set.schema.json`）+ 冻结哈希 | 本机（stdlib 脚本） | 是 |
| E5 | 逐组合跑 60 条，落 CSV（query_id、top-5 ids、scores、命中判定） | 本机 | 是 |
| E6 | 重复性：同一 query 跑 3 次，top-5 集合稳定性 | 本机 | 否（记录） |
| E7 | 冷启动计时（`keep_alive:0` 卸载后首调），两个模型各一组 | 本机 | 否（记录） |
| E8 | 报告 `docs/ai/reports/b1-5a-embedding-eval.md` | `docs/` | 是 |

**不做**：`mvn verify` 的新增测试（无源码改动）；CI 集成（`AC-B-8` 已定真实调用不进 PR CI）。

脚本落点（用户已批准 D-4）：`scripts/eval/` 下 **stdlib-only Python**（`urllib`，不引入 `requests`）；金标准集与 schema 在 `docs/ai/`；CI **不**依赖本机或评测脚本。

---

## 9. `README.md` 精确替换文本（**本 issue 不修改 README**，由 B1.5a 的 PR 应用）

> 现状（2026-11-09 `README.md` 第 **13**、**14** 行；任务简报中的"第 15/16 行"为旧编号，以文本为准）：

**第 13 行 — 原文**

```text
   - **Movie catalog semantic retrieval (doable now):** embed structured catalog cards — `title`, `year`, `director`, `genres`, `stars`, `rating` — so catalog questions such as "movies similar to X" or "1990s sci-fi" work without any external corpus.
```

**第 13 行 — 替换为**

```text
   - **Movie catalog semantic retrieval and structured search (doable now):** use structured catalog cards — `title`, `year`, `director`, `genres`, `stars`, `rating` — so catalog questions such as "movies similar to X" or "1990s science fiction" work without any external corpus. The catalog snapshot holds 21,087 movies (years 196–2024); ratings exist only for 2001-onwards titles, so rating-filtered questions cannot match 2000 or earlier.
```

**第 14 行 — 原文**

```text
   - **Project/developer knowledge RAG Q&A:** the first-version corpus is the existing `docs/` (11 markdown documents, excluding these AI roadmap drafts) plus this `README.md`; every answer must cite its sources.
```

**第 14 行 — 替换为**

```text
   - **User-side scope is the movie catalog only:** repository development documentation (`docs/`, this `README.md`) is **not** user-facing corpus — it is not chunked, embedded, indexed, or retrieved as Q&A material, and users are not answered questions about this project's development process.
```

**附注（供 PR 作者核对，不写进 README）**

- 替换后第 15 行（"Movie plot/synopsis Q&A is out of scope…"）与第 16 行（"Recommendations and multi-turn…"）**保持不变**；但"Recommendations"已由 D-7 纳入用户侧目标 → 该行需在 **B-① 立项时**再改，本 PR 不改（避免把评测 PR 变成范围变更）。
- 第 7 行（"Items 3 and 4 are planned work: no AI capability exists in the codebase yet"）在 B1.5a/b 完成后仍成立（只多了评测产物，无用户功能）→ 不改。
- 替换文本中"1990s science fiction"在 21k 快照上**可命中**（1990s 且 `Sci-Fi`=55 部 / `Sci-Fi ∪ Science Fiction`=55 部），与旧口径（1990–1999 = 0 行）相反 —— 这是 D-3 换数据集后的直接结果。

---

## 10. 验收条件（B1.5a）

| ID | 条件 | 验证方式 |
| --- | --- | --- |
| **B15a-1** | 60 条金标准集已版本化并冻结哈希，覆盖 20/20/20（zh→zh / zh→en / en→en），每条含可接受集合与标注理由 | 文件评审 + `golden-set.schema.json` 校验 + SHA-256 记录 |
| **B15a-2** | 每条正样本的可接受集合均在 21k 快照内可命中（SQL 验证记录在案） | 报告附验证输出摘要 |
| **B15a-3** | **≥2 个**多语言 embedding 候选（`bge-m3` + `qwen3-embedding:0.6b`）完成 zh→zh / zh→en / en→en 对比 | 报告表格 |
| **B15a-4** | 记录 **`recall@5`（D8 判定用）**、`MRR@10`、**任务级成功率（次要，仅解释）**、p50/p95（热/冷）、构建耗时、tokens、向量内存、零结果率、截断次数，**每组合带三个 fingerprint（`schema_hash` = N/A 并说明）** | 报告表格 |
| **B15a-5** | 明确写出实际数据集规模（21,087）与覆盖（196–2024 / 61 行 56 distinct 类型 / 13,082 无评分 / **评分仅 2001+**，2000 年 0 部有评分）；外推数字单独标注 | 报告 |
| **B15a-6** | 对 D8 阈值（**`recall@5` / top-5 命中率 ≥80%**）给出**通过/不通过**结论；任务级成功率**不得**用作通过条件；不通过时**不得**改阈值、改分母或换轨道掩盖 | 报告结论段 |
| **B15a-7** | 报告显式声明：**未使用任何开发文档作为语料**，未对 `docs/**`/`README.md` 做切分/embedding/索引；脚本配置为证据 | 报告 + 脚本配置 |
| **B15a-8** | 如实区分"已实测 / 外推 / 未验证"；未运行的步骤一律标注 | 报告与工作流日志 |
| **B15a-9** | 中文译名（如"黑客帝国"）的中→英命中率**单独统计**并如实记录（含失败率），不得用"用户会给英文名"回避 | 报告（zh→en 轨道分项） |

---

## 11. 拟修改文件（实施阶段，届时由 worker 执行）

| 路径 | 动作 | 说明 |
| --- | --- | --- |
| `scripts/eval/embed_eval.py` | ✅ **已创建（stdlib-only）** | 连通性、快照哈希、卡片生成、检索、计时 |
| `scripts/eval/check_golden.py` | ✅ **已创建（stdlib-only）** | 金标准集 schema + id 存在性校验 |
| `docs/ai/golden/queries.b1-5.json` | ✅ **已创建并冻结（60 条，与 B1.5b 共享）** | sha256 `0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079` |
| `docs/ai/golden/negative.b1-5.json` | ✅ **已创建并冻结（5 条反例）** | sha256 `09725672…75fd`；不计入分母 |
| `docs/ai/reports/b1-5a-embedding-eval.md` | ✅ **已生成（实测数字）** | 报告：D8 FAIL（0.4833 / 0.4167）+ 四组合主表 + 失败模式 + `OPEN-a2` 实测关闭 |
| `README.md` | ✅ **已修改（第 13/14/16 行）** | 13/14 = §9 替换文本（`2001-onwards`）；16 = “推荐属于用户侧目标、多轮仍推迟”（D-7）｜其余行未动 |
| `docs/architecture/movie-cards.md` | ✅ **已更新（r6）** | 回填实测 token/构建/内存/截断与三个 fingerprint 实际值 |
| `docs/plans/b1-5-movie-retrieval-eval.md` | ✅ **已更新（r6）** | umbrella 回填 A 路线结论（D8 FAIL、A 不得单独交付、B1.5b 为下一步） |

**不得修改**：`docs/plans/ai-option-b-spring-ai-rag.md` 的历史段落（只允许追加 dated 回填，已由 2026-11-09 回填完成）、`b1-spring-ai-integration.md`、`issue-66-*.md`、B0/B1 日志、任何 `src/**`、`pom.xml`、CI。

---

## 12. 风险与回退

| 风险 | 概率/影响 | 缓解 | 回退 |
| --- | --- | --- | --- |
| 端点不可达/被关机 | 中/中 | E1 先做 30 s 连通性检查；记录未运行原因 | 评测顺延，**不得**编造数字 |
| macOS 本地网络权限假故障（`No route to host` 而 `curl` 正常） | 中/低 | 先排除客户端权限 | 换 `curl` 复现 |
| 13× 延迟离群值污染耗时结论 | 高/中 | 每 batch ≥3 次取中位数、记录最坏值 | 报告并列中位数与最坏值 |
| 静默截断污染对比（阈值口径冲突：scout 实测 2,048 vs `/api/tags` 声明 `context_length=8192`） | 中/高 | **本 issue 必须先实测确认截断阈值**，再跑正式评测；单卡 48 tokens 远低于两者，但结论必须写在报告里 | 截断确认后调整卡片粒度并重跑 |
| 中文译名无法命中（无别名表） | **高**/高 | 作为**已知边界**单列统计（`B15a-9`）；不承诺解决 | 若需别名表，须另立 issue 批准新数据来源 |
| 两模型维度相同导致"看起来一样好"的误判 | 中/中 | 逐条配对 + 分轨道统计，禁止只报总体均值 | 补分轨道表 |
| 卡片模板变更导致历史数字不可比 | 中/低 | `template_hash` 强制记录；变更即 bump `card_version` | 旧数字标"不同 template_hash，不可比" |
| 评分覆盖只到 2001+ 导致"年代 × 评分"用例全空 | **已发生（数据事实）** | 金标准集规则（§4.1）与报告声明（§2.2） | 不允许把空结果计为命中 |
| 范围蔓延到 B-① 或 B1.5b | 中/高 | §1.3 硬边界；越界需主 agent 重新批准 | 回滚越界改动 |

---

## 13. 未决问题（本文件范围内）

| ID | 问题 | 状态 |
| --- | --- | --- |
| `OPEN-a1` | "任务级成功率"是否能作为 D8 的"top-5 命中率 ≥80%"的等价口径 | ✅ **已裁定（2026-11-09）：D8 gate = top-5（`recall@5`）≥80%；任务级成功率为次要指标，不纳入 A 的通过条件** |
| `OPEN-a2` | 截断阈值到底是 2,048 还是 `context_length=8192` | ✅ **已由实测关闭（2026-11-09）**：`bge-m3` = **2,048 tokens**（`prompt_eval_count` 902→1,802→2,048 后饱和；`/api/tags` 声明的 8192 不可信）；`qwen3-embedding:0.6b` = **4,095 tokens**（非 32,768）。单卡最长 50.7 tokens → 余量 ≈40×，4 组合 659 batch **截断 0 次** |
| `OPEN-a3` | 中文译名别名表是否引入（新数据来源 + 许可） | 🟡 未批准；本 issue **不做** |
| `OPEN-a4` | `zh-full` 与 `en-short` 是否各建双索引（成本 ×2），还是单索引 + 查询侧适配 | ✅ **已有实测数据（2026-11-09），最终选择仍由主 agent 定**：`bge-m3` **`zh-full` 31/60 (0.5167) ≥ `en-short` 29/60 (0.4833)**，且**逐轨道不低于**（zh_zh 11 vs 11 / zh_en 9 vs 8 / en_en 11 vs 10）；`qwen3` 则 **`en-short` 26/60 (0.4333) > `zh-full` 23/60 (0.3833)**。→ **结论：模板优劣因模型而异，没有普适的单一模板**；双索引的收益不足以支撑 **+82.4 MiB** 内存与约 **2× 构建成本**（单组合实测 5.3–8.9 min）→ **最终选择权在主 agent** |
| `OPEN-a5` | 60 条金标准集的**具体内容**（谁标注、是否第二人复核） | ✅ **已定（2026-11-09）**：**主 agent 担任标注/复核人**；worker 只 **生成候选 + SQL 验证可命中**，不得自行认定标注正确；报告**必须注明“单人标注、n=60 仅作方向性判断”** |

---

## 14. Oracle 复核状态

| 来源 | 内容 | 状态 |
| --- | --- | --- |
| oracle | 2026-11-09 提出 9 项阻塞（B-1…B-9）→ r3 已逐项修复；同日 **delta 复核**再提 3 项（`db_snapshot_hash` SQL 无效 / 卡片统计旧口径 / D8 门槛口径）→ **r4 已修**（SQL 已在 3307 实跑、统计改实测值、`OPEN-a1` 关闭） | 🟢 3 项已修；**未再声明 oracle 二次复核通过** |
| 主 agent | 已裁定（1）按 oracle 9 项阻塞 + D-1…D-9 实施；（2）**D8 gate = top-5 ≥80%（`OPEN-a1` 关闭）**；（3）**`OPEN-a5`：主 agent 为标注/复核人** | 🟡 `OPEN-a4` 仍待裁定（实施期；不影响 #69 建 issue） |
| 主 agent | 2026-11-09 **批准 r4 内容并交付 `github-operator` 创建 issue** | ✅ 已创建 **#69**（milestone #3、无 labels） |
| 实施（worker） | 2026-11-09 **完成评测实施**：脚本 + 金标准集 + 全量 4 组合实测 + 报告；**D8 FAIL（0.4833 / 0.4167）**，阈值/分母未改 | ✅ 报告：`docs/ai/reports/b1-5a-embedding-eval.md` |
| 三路 review | 2026-11-09 正确性 / 测试覆盖 / 必要性与复杂度三路只读审查提出回填文档状态/契约的要求 | 🟡 **本文按该要求回填（r6）**；结论与报告正文以 review 汇总为准 |

> **r6 更新（2026-11-09）**：评测**已实施并完成**（脚本、金标准集、报告、`README.md` 13/14/16 行均已落地）。本文件仍然**不声称**：D8 达标（**实测 FAIL**）、任何用户功能已交付、B1.5b 已开始、oracle 对 r6 已复核。实测数字的唯一来源是 `docs/ai/reports/b1-5a-embedding-eval.md`。

---

## 15. 修订记录

| 版本 | 日期 | 变更 |
| --- | --- | --- |
| r1–r2 | 2026-11-08 | 单份 B1.5 计划阶段的草案 |
| r3 | 2026-11-09 | 拆分自 umbrella；按 oracle 9 项阻塞（B-1…B-9）与用户批准 D-1…D-9 重写 |
| **r4** | 2026-11-09 | 按 oracle **delta 复核**修 4 项：① §5.1/§5.3/§10 `B15a-4`/`B15a-6` **统一 D8 门槛口径 = `recall@5` / top-5 ≥80%**，任务级成功率降为次要指标并明确不纳入通过条件；`OPEN-a1` 关闭为“已裁定”。② §2.1 卡片统计改实测值（**类型全量 1.2061 / 有 genre 的 1.4313 / 最多 4 / 3,318 无 genre；主演全量 5.9258 / 有主演的 7.0458 / 最多 46 / 3,352 无主演**，分母口径已写定于 `MC-U7`）。③ §8 `E3` 改为“**2 模板卡片 + 2 模型 embedding（4 组合）**”，避免读成卡片生成 4 次。④ `OPEN-a5` 关闭（主 agent 为标注/复核人，worker 生成候选 + SQL 验证，报告注明单人标注、n=60 方向性）。**`db_snapshot_hash` 的 SQL 修正落在 `docs/ai/README.md` §2.2**（本文件只引用算法） |
| **r5** | 2026-11-09 | **数据事实修正**：评分覆盖起点 **2000 → 2001**（实测 `MIN(year)` among rated = 2001、2000 年 58 部全无评分、196–2000 共 **11,975** 部无评分）。改动 §2.2 数据陷阱 1、§9 README 第 13 行替换文本（`2001-onwards` / `2000 or earlier`）、§10 `B15a-5`、§12 风险表 |
| **r5** | 2026-11-09 | **仅回填 issue 跟踪信息，不改方案内容**：头部 GitHub issue 行 → **#69** + URL（milestone #3、无 labels）；状态 → 🟢 已批准 / 已建 issue / **待实施**；新增“实施状态 = 未开始”行；§14 增“主 agent 批准并创建 #69”行、oracle delta 行状态改为“3 项已修（未声明二次复核通过）”；删去“issue 已创建”不在本文件的旧否定句 |
| **r6** | 2026-11-09 | **实施完成回填（不改方案、不改阈值）**：头部状态 → 已完成（**D8 FAIL**）；"实施状态" → ✅ 已实施（脚本/金标准集/报告/`README.md` 13/14/16 行）；§11 标注每个产物的实际结果；§13 `OPEN-a2` 关闭（2048 / 4095）、`OPEN-a4` 以实测数据定（bge `zh-full` 31 ≥ `en-short` 29 且逐轨道 ≥；qwen3 26 > 23）；§14 增实施/三路 review 行并重写"不声称"段；新增 §16 实施结果摘要 |
| **r7** | 2026-11-09 | **`OPEN-a4` 措辞与报告 §16 对齐（不改任何实测数字/阈值）**：删除“单一 `en-short` 对两个模型都不吃亏”的推论，改为“**模板优劣因模型而异，没有普适的单一模板**；双索引收益不足以支撑 **+82.4 MiB** 与约 **2× 构建成本** → 最终选择权在主 agent” |

---

## 16. 实施结果摘要（2026-11-09 实测；数字来源 = `docs/ai/reports/b1-5a-embedding-eval.md`）

> 本节**只摘要**，不替代报告。任何引用都必须回到报告并带 fingerprint。

### 16.1 D8 判定（唯一 pass/fail 依据：`recall@5` ≥ 80%）

| 候选（配对口径，n=60） | `recall@5` | 命中 | `MRR@10` | D8 |
| --- | --- | --- | --- | --- |
| `bge-m3:latest` | **0.4833** | 29/60 | 0.4334 | ❌ **FAIL**（差 19 次命中 / 31.7 个百分点） |
| `qwen3-embedding:0.6b` | **0.4167** | 25/60 | 0.3134 | ❌ **FAIL**（差 23 次命中 / 38.3 个百分点） |

- **阈值未调、分母未换、任务级成功率未用于判定**（B15a-6）。两模型并集 32/60 = 53.3%，仍远低于 80%。
- 四组合（每组合对全部 60 条打分，**非判定口径**）：`bge-m3`×`zh-full` 0.5167（31/60）、×`en-short` 0.4833（29）、`qwen3`×`zh-full` 0.3833（23）、×`en-short` 0.4333（26）。

### 16.2 能力分布（决定后续路线的事实）

| 意图 | 条数 | `bge-m3` | `qwen3` | 结论 |
| --- | --- | --- | --- | --- |
| `search_by_title` | 15 | 13（0.867） | **15（1.000）** | **片名/实体检索可用**（且普通 LIKE/全文检索更便宜） |
| `filter_catalog` | 20 | 9（0.450） | **2（0.100）** | **结构化筛选不可用** |
| `similar_movies` | 15 | 3（0.200） | 3（0.200） | **相似检索不可用**（15/15 的 top-5 含 seed 本身） |
| `recommend_by_preference` | 10 | 4（0.400） | 5（0.500） | 偏好推荐不可用 |

- 归因（实测）：短文本 dense 表示**无法承载结构化约束**（AND/年代/评分阈值），类型词退化为标题字面匹配 → **B 路线（工具/结构化过滤）的必要性得到实测支持**。
- 中文译名不是主因：`qwen3` 译名 **3/3**、`bge-m3` 1/3；剔除 3 条译名后 gate 仍只有 49.1% / 38.6%（`OPEN-a3` 未批准，属架构约束）。

### 16.3 复现指纹（与报告 §0 一致）

| 项 | 值 |
| --- | --- |
| `card_version` | `movie-card/v1` |
| `template_hash` | `zh-full` **`c5990dc4b9065241fa221ec80e4f32cc42e8be773507421e29160c5d7b3d9575`** / `en-short` **`bc3bcce652601f503be844a63a0e1b743d96352b56cd2dfc04dade9ba76e585a`** |
| `db_snapshot_hash` | **`8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925`** |
| `golden_sha256` | **`0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079`**（`b1-5-golden/v1`）；反例 `09725672…75fd`（不计入分母） |
| `schema_hash` | **N/A**（A 路线不使用工具，报告中显式写出） |
| 模型 | `bge-m3:latest` / `qwen3-embedding:0.6b`，均 1024 维 |

### 16.4 `README.md` 改动（实际 diff = 仅第 13/14/16 行）

`git diff README.md` → **3 行变更**，无其他行改动：

| 行 | 实际文本要点 | 与计划 §9 的关系 |
| --- | --- | --- |
| **13** | 标题改为「**Movie catalog structured search (doable now) and semantic similarity search (under evaluation)**」；品种/年份口径保留，评分写 **`2001-onwards`**；尾部新增一句「Dense semantic similarity over the card text is still **under evaluation**：currently **`recall@5` 0.48 against the 0.80 target**」+ 报告链接 | 在 §9 替换文本基础上按实测修正两处：`2000-onwards` → **`2001-onwards`**；并**显式标注 dense 相似检索“仍在评估、未达标”**（因为 §9 文本写于实测前） |
| **14** | 「**User-side scope is the movie catalog only**」：开发文档（`docs/`、`README.md`）不是用户语料、不切分/不 embedding/不做问答 | 与 §9 替换文本一致 |
| **16** | 「**Recommendations are part of the user-side movie retrieval/recommendation target**；multi-turn conversational features remain deferred」 | §9 附注原标“本 PR 不改”，实施时按 **D-7** 改（否则与新方向冲突） |

- 计划 §9 的替换文本本身**未回改**（仍写 `2000-onwards`、且未含“under evaluation”句）；**实际落地以上表为准**，差异同时记录在报告 §14.1 与本节——避免形成“两套文本”。
- 结论：README 不再暗示“语义相似检索已可用”；它与 D8 FAIL 的实测结论一致。

### 16.5 未因本次实施而关闭的项

`OPEN-a3`（译名别名表，未批准）、`OPEN-a4`（双/单索引：**已有数据，选择权在主 agent**）、`MC-U8`（空主演占位符仍为暂定 `主演：暂无（前 3）`）、`MC-U6`（genre 是否规范化）、金标准集 negative 根形状、只跑一轮/单人标注/n=60 的方向性限制（详见报告 §13/§16）。

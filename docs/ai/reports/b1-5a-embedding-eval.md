# B1.5a — 电影卡片 embedding 检索质量评测报告（A 路线）

> **文档状态：🟡 实测完成（2026-11-09）— D8 门槛未达标（两个候选均不通过）；待主 agent 复核 + 三路 review。**
>
> | 元数据 | 值 |
> | --- | --- |
> | 文档类型 | 评测报告（实测数字 + 结论；**不改计划正文、不改阈值、不改分母**） |
> | `last_updated` | **2026-11-09** |
> | 所属 | issue #69 / `docs/plans/b1-5a-movie-card-embeddings.md`（r4，已批准）；umbrella = `docs/plans/b1-5-movie-retrieval-eval.md` |
> | 判定口径 | **D8：`recall@5` ≥ 80%（唯一 pass/fail 依据）**；`MRR@10` 与任务级成功率为**次要/解释性**指标（`OPEN-a1` 已裁定） |
> | 金标准集 | `docs/ai/golden/queries.b1-5.json`（`b1-5-golden/v1`，主 agent 已审核批准为**候选集**：单人标注、n=60 方向性） |
> | 数据快照 | Docker `fabflix-mysql` 宿主 3307 / `moviedb`，21,087 部（196–2024） |
> | 端点 | `http://<host>:11434`（Ollama，局域网主机；按 `docs/ai` 规则不写具体地址），2026-11-09 实测 |
> | 脚本 | `scripts/eval/embed_eval.py`、`scripts/eval/check_golden.py`（stdlib-only，D-4）；CI 不依赖 |

---

## 0. 复现指纹（每个数字块都对应下列取值）

| 项 | 值 |
| --- | --- |
| `card_version` | `movie-card/v1` |
| `template_hash`（`zh-full`） | `c5990dc4b9065241fa221ec80e4f32cc42e8be773507421e29160c5d7b3d9575` |
| `template_hash`（`en-short`） | `bc3bcce652601f503be844a63a0e1b743d96352b56cd2dfc04dade9ba76e585a` |
| `db_snapshot_hash` | `8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925` |
| `golden_version` / `golden_sha256` | `b1-5-golden/v1` / `0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079` |
| 反例集 | `b1-5-negative/v1` / sha256 `09725672a2b8fb6ba330613f61533594ed7919b933a55d435c73f043d5fc75fd`（**不计入分母**） |
| `schema_hash` | **N/A**（A 路线不使用工具；脚本常量 `SCHEMA_HASH = "N/A"`，报告中显式写 N/A） |
| 卡片数 | 21,087（全部快照行，`--max-cards 21087`） |
| 快照事实 | 21,087 部 / 年份 **196–2024** / 类型 **61 行 56 distinct** / **13,082 部无评分** / 评分只覆盖 **2001+**（2000 年 0 部有评分） |
| `template_hash` 的**覆盖范围** | 只锁「`card_version` + 模板字面串 + 字段顺序 + **评分** null 占位符」；**genres / stars 的空值段（`类型：暂无`、`Genres: N/A`、`主演：暂无（前 3）`）不在 hash 输入里** → 改这两段文本**必须 bump `card_version`**，否则历史数字会被误当成可比（`docs/ai/README.md` §2.1） |

---

## 1. 结论（先说判定）

> **D8（`recall@5` ≥ 80%）不通过——两个候选都没达到，且差距很大。**

| 候选（D8 配对口径，见 §2.3） | `recall@5` | 命中数 | `MRR@10` | D8 |
| --- | --- | --- | --- | --- |
| `bge-m3:latest`（1024 维） | **0.4833** | 29 / 60 | 0.4334 | ❌ **不通过**（差 19 次命中 / 31.7 个百分点） |
| `qwen3-embedding:0.6b`（1024 维） | **0.4167** | 25 / 60 | 0.3134 | ❌ **不通过**（差 23 次命中 / 38.3 个百分点） |

- 上述数字对应指纹：`template_hash`(zh-full)=`c5990dc4…`、`template_hash`(en-short)=`bc3bcce6…`、`db_snapshot_hash=8b16c047…`、`golden_sha256=0c2f4c5e…`、`card_version=movie-card/v1`、模型维度 1024、`schema_hash=N/A`。
- **没有调低阈值、没有换分母、没有把任务级成功率当判定依据**（B15a-6）。§12 的诊断性数字**全部标注为非判定口径**，只用于解释失败原因。
- 失败不是"差一点"：把两个模型并集起来也只有 **32/60 = 53.3%**；**两模型并集**剔除中文译名 3 条也只有 **29/57 = 50.9%**；**两模型并集**剔除「相似电影」类（标注最严格的一类）也只有 **29/45 = 64.4%**（见 §12）。
- 能用的部分很明确：**片名/实体检索**（`bge-m3` 13/15，`qwen3` **15/15**，含 3 条中文译名全部命中）；**结构化筛选与"相似电影"基本不可用**（`bge-m3` filter 9/20、similar 3/15；`qwen3` filter **2/20**、similar 3/15）。

---

## 2. 方法与契约

### 2.1 语料（唯一）

- 卡片文本 = `movie-card/v1`（`docs/architecture/movie-cards.md` §2）：`zh-full`（主测）与 `en-short`（对照）两模板，21,087 张全量。
- **未使用任何开发文档作为语料**（B15a-7）：脚本只读 `docs/ai/golden/**` 作为评测输入，语料只来自 DB 的 `movies` / `genres` / `stars` / `ratings`；`docs/**`、`README.md` 没有被切分、embedding 或索引。证据 = `scripts/eval/embed_eval.py` 的 `export_snapshot()` / `render_card()`（唯一文本来源是 DB 行）与 `load_golden()`（唯一额外输入是金标准集）。

### 2.2 卡片渲染的实测事实（影响解释）

| 项 | 实测 |
| --- | --- |
| 卡片字符长度（`zh-full`） | min 43 / median 107 / max 192 |
| 卡片字符长度（`en-short`） | min 53 / median 77 / max 149 |
| tokens/卡（`bge-m3` tokenizer） | `zh-full` **47.2**、`en-short` **26.8** |
| tokens/卡（`qwen3-embedding` tokenizer） | `zh-full` **50.7**、`en-short` **29.1** |
| 0 genre 卡片 | 3,318 部渲染为 `类型：暂无` / `Genres: N/A`（未跳字段） |
| 无评分卡片 | 13,082 部渲染为 `评分 暂无` / `Rating: N/A`（未写 0） |
| 无主演卡片 | 3,352 部渲染为 `主演：暂无（前 3）`（`MC-U8` 仍未定稿，见 §16） |

> 统计来源：`python3 scripts/eval/embed_eval.py --card-stats`，输出存 `.pi/artifacts/b1-5a-eval/card-stats.log`（本次复核：`cards=21087` / `zh-full: chars min=43 median=107 max=192` / `en-short: chars min=53 median=77 max=149` / `no genres=3318` / `no rating=13082` / `no stars=3352`）。
>
> 渲染健壮性检查：对全部 21,087 行 × 2 模板渲染，**0 例异常**（含 2 部标题里带 `{…}` 的片，如 `A Ca{\c}ca`；`str.format` 只作用于模板字面串，不会被值里的花括号二次解析；另有 6 部标题本身含单词 `None`，属数据事实，不是渲染空值）。

### 2.3 判定配对（D8 口径，运行前固定）

| 轨道 | 查询语言 | 卡片模板 | 条数 |
| --- | --- | --- | --- |
| `zh_zh` | 中文 | `zh-full` | 20 |
| `zh_en` | 中文 | `en-short` | 20 |
| `en_en` | 英文 | `en-short` | 20 |

- 每组合仍对 **全部 60 条**查询打分（§4，供交叉对照），但**判定只用上表配对**（§5），避免"中文查询配中文模板/英文模板孰优"这种非计划内的比较被当成 gate。
- 单条命中 = 该条 `acceptable_movie_ids` 中至少 1 个出现在 **top-5**；`MRR@10` 用首个可接受结果的倒数排名。
- **任务级成功率**（次要）：A 路线返回的就是 DB 卡片、字段由服务端渲染，接地天然成立，因此本报告中它与 `recall@5` **数值恒等**；这是设计结果，不是巧合，也不改变判定口径。

---

## 3. 端点与模型（实测）

| 项 | `bge-m3:latest` | `qwen3-embedding:0.6b` |
| --- | --- | --- |
| digest | `7907646426070047a77226ac3e684fbbe8410524f7b4a74d02837e43f2146bab` | `ac6da0dfba84a81fdbfbaf330198c33cd77c4cdfc53e8bc50eb581914a15621d` |
| 维度（`/api/tags` 声明 + 实返） | 1024 | 1024 |
| family / 量化 / 体积 | `bert` / F16 / 1,157,672,605 B | `qwen3` / Q8_0 / 639,150,858 B（595.78M 参数） |
| 声明 `context_length` | 8192 | 32768 |
| **实测截断阈值**（`--probe-truncation`） | **2048** tokens | **4095** tokens |
| 实测 L2 范数 | 1.0（查询向量，`determinism.log`） | 1.0（查询向量，`determinism.log`） |

- **`OPEN-a2` 已由实测关闭（部分）**：`prompt_eval_count` 在 `bge-m3` 上从 902 → 1,802 → **2,048** 后不再增长；`qwen3-embedding:0.6b` 从 1,252 → 2,502 → **4,095** 后不再增长。即：**`bge-m3` 的真实上限是 2,048，而不是 `/api/tags` 声明的 8,192**（与 scout 的 2,048 一致）；`qwen3` 是 4,095 而非 32,768。
- 本评测的卡片最长 192 字符 / 50.7 tokens，**距两个阈值都有 40× 余量**：4 个组合的 659 个 batch 中 **静默截断次数 = 0**（阈值按模型分别取 2048 / 4095）。
- 查询向量**确定性**：同一批 10 条查询连发 3 次，向量**逐位相同**（max abs diff = 0.0），top-k 可复现。

---

## 4. 四组合主表（每组合对全部 60 条查询打分）

指纹：`template_hash(zh-full)=c5990dc4…`、`template_hash(en-short)=bc3bcce6…`、`db_snapshot_hash=8b16c047…`、`golden_sha256=0c2f4c5e…`、`card_version=movie-card/v1`、`schema_hash=N/A`。

| 模型 | 模板 | `recall@5` | 命中 | `MRR@10` | 任务级（=recall 见 §2.3） | zh_zh | zh_en | en_en |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `bge-m3:latest` | `zh-full` | **0.5167** | 31/60 | 0.4256 | 0.5167 | 0.55 (11/20) | 0.45 (9/20) | 0.55 (11/20) |
| `bge-m3:latest` | `en-short` | 0.4833 | 29/60 | 0.4103 | 0.4833 | 0.55 (11/20) | 0.40 (8/20) | 0.50 (10/20) |
| `qwen3-embedding:0.6b` | `zh-full` | 0.3833 | 23/60 | 0.3015 | 0.3833 | 0.45 (9/20) | 0.30 (6/20) | 0.40 (8/20) |
| `qwen3-embedding:0.6b` | `en-short` | 0.4333 | 26/60 | 0.3415 | 0.4333 | 0.50 (10/20) | 0.40 (8/20) | 0.40 (8/20) |

读法（实测，不是外推）：

- `bge-m3` 在 `zh-full` 上略好于 `en-short`（31 vs 29），但**每个轨道内部差别很小**（zh_zh 两模板都是 11/20）→ 模板选择不是主要变量。
- `qwen3-embedding` 在 `en-short` 上明显好于 `zh-full`（26 vs 23），说明它的中文模板（中文模板 + 英文值）反而**降低了**排序质量；其 zh_en 轨道在 `zh-full` 上只有 6/20。
- 同一候选跨模板 maxima 也只有 0.5167（`bge-m3`），**任何组合都远低于 0.80**。

---

## 5. D8 判定表（配对口径，唯一 pass/fail 依据）

| 候选 | `recall@5`（n=60） | zh_zh（n=20） | zh_en（n=20） | en_en（n=20） | `MRR@10` | 零结果 | **D8（≥0.80）** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `bge-m3:latest` | **0.4833**（29/60） | 0.55（11/20） | 0.40（8/20） | 0.50（10/20） | 0.4334 | 0 | ❌ **FAIL** |
| `qwen3-embedding:0.6b` | **0.4167**（25/60） | 0.45（9/20） | 0.40（8/20） | 0.40（8/20） | 0.3134 | 0 | ❌ **FAIL** |

原始产物：`.pi/artifacts/b1-5a-eval/{bge-m3_latest,qwen3-embedding_0.6b}__gate.json`（含每条 query 的 top-5 与命中/排名）。

两个候选使用的是**同一份数据快照**：21,087 部 / 年份 196–2024 / 类型 61 行 56 distinct / 13,082 部无评分 / 评分只覆盖 2001+（2000 年 0 部有评分）。同一 `db_snapshot_hash`、同一 `golden_sha256`、同一 `card_version`。

---

## 6. 分意图分解（配对口径）

| 意图 | 条数 | `bge-m3` 命中 | `qwen3` 命中 |
| --- | --- | --- | --- |
| `search_by_title` | 15 | 13（**0.867**） | **15（1.000）** |
| `filter_catalog` | 20 | 9（0.450） | **2（0.100）** |
| `similar_movies` | 15 | 3（0.200） | 3（0.200） |
| `recommend_by_preference` | 10 | 4（0.400） | 5（0.500） |

- **片名类**：两个模型都能做；`qwen3` 15/15（唯一满分的格子），因为它具备真实的跨语言片名知识（§9）。
- **结构化筛选类**：`bge-m3` 勉强（9/20），`qwen3` 几乎全灭（2/20）。`qwen3` 的失败形态高度一致：把中文类型词翻成英文后**去匹配"标题里的词"**而不是 genre 字段（例：Q05「1960 年代的犯罪片」返回 `Crime and Punishment (1936)`、`Crime School (1938)`、`Crime and Punishment (1990)` —— 只有标题带 Crime，年代与 genre 全错）。
- **相似电影**：两个模型都只有 3/15，且 15/15 的 top-5 里都出现了 **seed 电影本身**（§11.1）。

---

## 7. 成本、tokens、内存、延迟（实测）

指纹同上（§4）+ 模型 digest 见 §3。批次大小 32 卡；`keep_alive=30m`（避免运行期反复冷启动）。

| 模型 | 模板 | build 总时长 | batch 数 | 卡片速率 | tokens 合计 | tok/卡 | 稳态 tok/s | batch p50/p95/max (s) | 向量内存 (fp32) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `bge-m3` | `zh-full` | **532.5 s**（8.9 min） | 659 | 39.6 /s | 994,857 | 47.2 | 1,872.8 | 0.801 / 0.870 / 1.031 | 86,372,352 B = **82.4 MiB** |
| `bge-m3` | `en-short` | **516.7 s**（8.6 min） | 659 | 40.8 /s | 564,371 | 26.8 | 1,094.6 | 0.780 / 0.841 / 0.907 | 82.4 MiB |
| `qwen3-embedding` | `zh-full` | **397.0 s**（6.6 min） | 659 | 53.1 /s | 1,068,956 | 50.7 | 2,701.9 | 0.553 / 0.758 / 1.081 | 82.4 MiB |
| `qwen3-embedding` | `en-short` | **315.8 s**（5.3 min） | 659 | 66.8 /s | 613,781 | 29.1 | 1,951.9 | 0.473 / 0.536 / 0.677 | 82.4 MiB |

**4 组合合计构建耗时 = 1,762 s ≈ 29.4 min**（串行；同时驻留峰值 1 个模型的 82.4 MiB + 模型权重）。

查询延迟（每组合 60 次单条 query embedding，含 HTTP 往返；模型常驻）：

| 模型 | 模板 | p50 | p95 | mean | max |
| --- | --- | --- | --- | --- | --- |
| `bge-m3` | `zh-full` | **36.1 ms** | 57.6 ms | 39.1 ms | 68.9 ms |
| `bge-m3` | `en-short` | **35.4 ms** | 42.6 ms | 36.1 ms | 61.0 ms |
| `qwen3-embedding` | `zh-full` | 59.3 ms | 84.1 ms | 56.3 ms | 127.7 ms |
| `qwen3-embedding` | `en-short` | 53.5 ms | 72.3 ms | 52.6 ms | 109.3 ms |

其它必记指标（每组合）：

| 项 | `bge-m3`×2 | `qwen3`×2 |
| --- | --- | --- |
| 静默截断次数 | **0 / 659 batch** | **0 / 659 batch** |
| 截断判定口径 | 每个 batch 取 `prompt_eval_count / len(batch)`（**batch 均值**）与模型实测阈值（2048 / 4095）比较；本规模下卡片约 27–51 tokens，**结构性为 0**，不是碰巧没触发 | 同左 |
| 零结果 query 数 | **0 / 60** | **0 / 60** |
| 13× 延迟离群值 | **未出现**：batch max/p50 = 1.03/0.80 = **1.3×** | max/p50 = 1.081/0.553 ≈ 2.0×（首次 batch 含模型加载） |

> ▸ 与文档外推的差异见 §14。▸ `batch_seconds` 的 p50 就是稳态速率，`first_batch_seconds` 单独记在 `*__summary.json` 的 `build.first_batch_seconds`（0.881 / 0.783 / 0.808 / 0.639 s），因此"模型加载"没有被算进稳态 tok/s。

---

## 8. 冷启动（`keep_alive:0` 卸载后首调，实测）

| 模型 | 运行内实测（4 个 `*__summary.json` 的 `cold_start_s`） | 落盘重复 **5 次**（`cold-start.log`，2026-11-09T02:08Z） | 早期 4 次（控制台输出，**未落盘**） |
| --- | --- | --- | --- |
| `bge-m3:latest` | **2.12 s** | 1.89 / 1.83 / 1.58 / 1.59 / 1.59 s（median **1.59 s**） | 2.43 / 2.37 / 2.35 / 2.10 s（median 2.37 s） |
| `qwen3-embedding:0.6b` | 3.83 s | 1.08 / 1.07 / 1.07 / 1.07 / 1.07 s（median **1.07 s**） | 1.61 / 1.36 / 1.37 / 1.42 s（median 1.37 s） |

- 三组数字不一致有可解释的原因：**运行内**那次紧跟上一模型的 `keep_alive:0` 卸载与显存换出（`qwen3` 的 3.83 s 最典型）；**早期 4 次**在大量构建之后（GPU / 页缓存压力大）；**落盘 5 次**在空闲状态，最接近纯冷启动。
- **记录性偏离**：计划 §5.2 要求冷启动每组 **≥20 次**；本次只有 **n=5（落盘）+4（未落盘）+1（运行内）**，**低于计划**。原因是阶段 2 的时间预算优先给 4 个全量组合；该偏离按 `B15a-8` 如实记录，不影响查询命中判定。
- **不得与 B0 的 `bge-m3` 7.64 s 冷启动对比**：那是另一台机器（本机 `localhost`）的 B0 数字，模型与主机都不同，属不可比，本报告不引用为对比。
- 冷启动只影响评测/索引重建的启动成本，不影响单条查询判定。

---

## 9. 中文译名专项（B15a-9，单列）

| 用例 | 目标 | `bge-m3` | `qwen3-embedding` |
| --- | --- | --- | --- |
| Q21 找一下《黑客帝国》 | `AWw10` The Matrix (1999) | ❌ 未命中（top-5：Pirates of Silicon Valley / Net Games / Dark World / **Hackers (1996)** / Sharkman） | ✅ **rank 2** |
| Q22 《阿甘正传》是哪部电影 | `RZe19` Forrest Gump (1994) | ❌ 未命中（top-5：Gandhi / Ali / Almost Adult / Tomagan / Avgat） | ✅ **rank 1** |
| Q23 我想看《泰坦尼克号》 | `JCm28` Titanic (1997) | ✅ rank 1 | ✅ rank 1 |
| **合计** | 3 条 | **1/3** | **3/3** |

- 结论：**中文译名不是"必然失败"**。`qwen3-embedding:0.6b` 具备真实的译名→英文原片名知识（黑客帝国→The Matrix 排第 2、阿甘正传→Forrest Gump 排第 1）；`bge-m3` 则把「黑客帝国」降级成"黑客"→`Hackers` 这类字面近邻。
- 但译名不是主要瓶颈：即使把这 3 条全部判失败，gate 数字也只从 48.3% 变为 **46.7%**（28/60，把唯一命中的 Q23 也判失败）；若整组剔除则为 49.1%（28/57，见 §12）——**离 80% 仍差得远**。
- 无别名表（`OPEN-a3` 未批准）仍是架构约束：本次只是**实测了边界**，没有解决它。

---

## 10. 数据边界专项：0 genre / 无评分（单列）

| 用例 | 目标 | `bge-m3` | `qwen3` |
| --- | --- | --- | --- |
| Q26 找一下 The Big Lebowski 这部片 | `JlC18`（**0 genre + 无评分**） | ✅ rank 1 | ✅ rank 1 |
| Q44 find the movie Cast Away | `RZe22`（**0 genre + 无评分**） | ✅ rank 1 | ✅ rank 2 |
| Q35 / Q56 / Q57 / Q60 可接受集合内含 0-genre 行 | `JlC15`、`JlC18`（Q35）；`RLl18`（Q56）；`StK1`（Q57）；`MS1`、`MS21`、`MS36`（Q60） | Q35 ✗、Q57 ✗；**Q56 ✅ rank 2，但命中的是 `RLl21`（Romance，非 0-genre）**；Q60 ✅ rank 1，但首位是 `MS29`（Biography，非 0-genre） | 同左（Q56 ✅2 / Q60 ✅1） |

- 结论：**0-genre / 无评分卡片本身可检索**（两个模型都把标题查准），占位符没有破坏片名信号；但“同导演/同类”这类**依赖 genre 字段的相似任务**在 0-genre 行上没有任何信号可用（Q35 的 The Big Lebowski 就是 0 genre）。

---

## 11. 失败模式分析（带证据）

### 11.1 相似电影：模型把 seed 排第 1，然后给"标题近邻"（不是同类片）

`similar_movies` 15 条里，**两个模型的 top-5 都 100% 包含 seed 本身（15/15）**；而可接受集合按标注规则**排除 seed**（同导演 / 共享双类型+同年代），因此这类"正确但不被接受"的结果被判为未命中。

| 用例 | 可接受集合（示例） | `bge-m3` top-5 | `qwen3` top-5 |
| --- | --- | --- | --- |
| Q12 和《Pulp Fiction》相似的电影 | Reservoir Dogs / Jackie Brown / Inglourious Basterds / Kill Bill Vol.2 / Sin City | **Pulp Fiction**, Pulling, Just Like Heaven, Puppy, Puritan | **Pulp Fiction**, Filmatron, Pilak, Paris Belongs to Us, Crime and Punishment |
| Q35 想找像《Fargo》那样的片子 | Miller's Crossing / Barton Fink / The Hudsucker Proxy / … / The Big Lebowski / Blood Simple | **Fargo**, Forced Vengance, Futago, Strange Cargo, Argo | **Fargo**, The Fugitive, After Shock, Strange Cargo, Close Encounters |
| Q53 something like Face/Off | Judge Dredd / Terminator 2 / The Terminator | **Face/Off**, Face to Face, Saving Face, Touch & Go, Saving Face | **Face/Off**, The Opposite of Sex, The Seven Faces of Dr.Lao, Face to Face, Sibling Rivalry |

**这是"标注严格性 + 检索行为"的混合结果，必须在 umbrella 决策时区分**：

- 检索行为是真的不行：除 seed 外，模型给出的是**片名共享词**（Pulling/Puppy、Face to Face/Saving Face、Futago/Strange Cargo），不是"同导演/同类型/同年代"的片；也就是说短卡片上的 dense 相似度在这类任务里退化成**模糊片名匹配**。
- 标注也是真的严格：可接受集合是"同导演"或"双类型+同年代"的**完备集合**，一个语义上合理的"另一部犯罪片"不会被接受。若把 seed 也算可接受，这类会变成 15/15；但这会改判定口径，**本报告不做**（只作 §12 诊断）。
- 佐证：3 条命中的 similar 用例恰好是**查询里显式写了类型词**的（Q13 动作科幻 / Q16 科幻片 / Q56 见附录），说明模型需要类型词才不跑偏。

### 11.2 结构化筛选：类型词被映射到"标题里的词"，或类型对但年代/评分错

| 用例 | `bge-m3` top-5 | `qwen3` top-5 |
| --- | --- | --- |
| Q08 1990 年代的犯罪惊悚片（可接受：Heat/Rising Sun/In the Line of Fire） | The Plastic Nightmare (1990, Horror) / The Haunting of Morella (1990, Horror,Porn) / La maison assassinee (1990, Thriller) / Ghosts Can't Do It (1990, Comedy,Porn) / Howling VI (1990, Horror) —— **年代对、双类型 AND 错** | Crime and Punishment (1990) / Crime Doctor's Strangest Case (1943) / Crime + Punishment in Suburbia (2000) / True Crime (1999) / The Perfect Crime (1998) —— **标题含 crime，年代全错** |
| Q09 2000 年代评分 8 分以上的悬疑片 | 5 部 2000s 恐怖/悬疑（genre 相近，评分/类型门槛未体现） | The Suspect (2006) / The Suspects (2004) / The Score (2001) / The Score (2001) / Crime + Punishment in Suburbia (2000) —— **标题近邻** |
| Q05 1960 年代的犯罪片（`bge-m3` 命中，`qwen3` 未命中） | ✅ 命中（rank 1） | Crime and Punishment (1936) / Crime and Punishment (1990) / Crime School (1938) / I Was A Criminal (1941) … |

- 归因（实测证据支持）：卡片里 `genres` 只是**一个短字段**（全量平均 1.2061 个/片），而 `title` 在文本最前面且与查询词共享子串；**短文本 embedding 对"结构化约束（AND、年代、评分阈值）"没有可用的表示**。这不是调模板能修好的问题（两模板都低于 0.52），也正好是 B 路线（工具/结构化过滤）的价值所在。
- 一个额外事实：`bge-m3` 比 `qwen3` 更"贴字段"（9/20 vs 2/20），因为 `qwen3` 的中文→英文映射更强，反而更彻底地把中文类型词当成了字面检索词。

### 11.3 片名类：两个模型都强，`qwen3` 更强

- `bge-m3` 13/15（失手：Q21 黑客帝国、Q22 阿甘正传）；`qwen3` **15/15**（含 3 条译名）。
- 这条结论对 B-① 有直接含义：**英文片名不值得用向量** —— `title LIKE`/全文索引更便宜、更准、可解释（实测：`title LIKE '%The Matrix%'` = 2 行、`'%Forrest Gump%'` = 1 行、`'%Titanic%'` = 3 行）。
- **但中文译名是另一回事**：SQL 对 `黑客帝国` / `阿甘正传` / `泰坦尼克号` 的 `title LIKE` 命中 **0/3**（分别 0 / 0 / 0 行），而 `qwen3-embedding` 拿到 **3/3**、`bge-m3` 1/3。也就是说，**唯一可能值得保留 A 的子能力是「译名 → 英文原名」的解析**，而它依赖**未批准**的中文别名表（`OPEN-a3`）或模型自身的影片知识（不可保证、不可解释）。这不是本 issue 能批准的方案。
- A 路线的其它价值只能落在「类型 / 年代 / 导演 / 演员」的字段语义上 —— 而 §11.2 显示它恰恰在这一点上最弱。

---

## 12. 诊断性数字（**不属于判定口径**，仅解释用）

> 下列数字**不是** D8 的一部分，**没有被用来改判**。列出是为了让 umbrella 决策看到"失败到底有多结构性"。

| 诊断 | `bge-m3` | `qwen3` | 说明 |
| --- | --- | --- | --- |
| 两模型并集 `recall@5` | — | — | **32/60 = 53.3%**（仍 < 80%） |
| 仅 `bge-m3` 独占命中 | 7 条（Q05/Q11/Q27/Q28/Q31/Q46/Q52） | — | 集中在中英文类型+年代筛选 |
| 仅 `qwen3` 独占命中 | — | 3 条（Q21/Q22/Q38） | 集中在中文译名 |
| 剔除 3 条中文译名（n=57） | 28/57 = 49.1% | 22/57 = 38.6% | 译名不是主因 |
| 同上但用**两模型并集**（n=57） | — | — | **29/57 = 50.9%** |
| 剔除 15 条 similar（n=45，标注最严） | **26/45 = 57.8%** | 22/45 = 48.9% | 去掉最严的一类仍 < 60% |
| 同上但用两模型并集 | — | — | 29/45 = 64.4% |
| 首个可接受结果落在 top-10（而非 top-5） | 32/60 = 53.3% | 28/60 = 46.7% | 提高 k 也救不回来 |
| similar 用例中 seed 出现在 top-5 | **15/15** | **15/15** | 只证明"能找到 seed" |
| 失败用例中 top-5 含"同类型且年代邻近"的片（genre ∩ + 年份 ±5） | **29/31** | **28/35** | 说明模型落在"题材邻域"，但满足不了 AND 约束 |
| 失败用例中 top-5 至少含同类型片 | 30/31 | 33/35 | 同上 |

---

## 13. 实测 / 外推 / 未验证（B15a-8）

**实测 —— 有落盘产物（`.pi/artifacts/b1-5a-eval/`）**

- 4 组合全量 21,087 卡构建：耗时、tokens、tok/卡、稳态 tok/s、batch p50/p95/max、向量内存、截断计数 → `*__summary.json` 的 `build` 块。
- 4 组合 × 60 条查询：top-5、相似度分数、命中/排名、`recall@5`、`MRR@10`、分轨道/分意图汇总、零结果 → `*__queries.csv` + `*__summary.json` + `*__gate.json`。
- 查询延迟 p50/p95/mean/max（每组合 60 次采样，热态，模型常驻）→ `*__summary.json` 的 `query_latency_s`。
- 冷启动：运行内每模型 1 次（`*__summary.json` 的 `cold_start_s`）+ 落盘重复 5 次 → `cold-start.log`。
- 截断阈值（`prompt_eval_count` 饱和点）：`bge-m3` 2048、`qwen3-embedding` 4095（`OPEN-a2`）→ `probe-truncation.log`。
- 卡片字符长度统计（min/median/max、无 genre/评分/主演计数）→ `card-stats.log`（命令 `python3 scripts/eval/embed_eval.py --card-stats`）。
- 查询向量确定性 + L2 范数（每模型 10 条 × 3 次：`identical_vectors=10/10`、`max_abs_delta=0.0`、`l2_norm_min/max=1.000000`）→ `determinism.log`（`--determinism --sample 10 --repeat 3`）。
- 金标准集结构 + id 存在性 + 快照块一致性 + `db_snapshot_hash` 重算 → `check_golden.log`；卡片/指纹 headprint → `dry-run.log`。
- 三个 fingerprint（`template_hash` × 2、`db_snapshot_hash`；`schema_hash=N/A`）+ `golden_sha256` → `check_golden.log`（sha256 两行）与 `*__summary.json`。

**实测 —— 控制台输出、未落盘（不得当作可复算证据）**

- 早期 4 次冷启动测量（2.43 / 2.37 / 2.35 / 2.10 s 与 1.61 / 1.36 / 1.37 / 1.42 s）：只有控制台输出，**未落盘**；§8 已把"落盘 5 次"列为主证据，这一栏保留但标明未落盘。
- 渲染健壮性抽查（2 部标题含 `{…}`、6 部标题含单词 `None`、0 例异常）：临时脚本输出，**未落盘**；结论不影响任何评测数字。

**外推（未实测，禁止直接引用为结论）**

- 若换更大批次（128/256）或独占 GPU 的吞吐/成本：**未测**；本次固定 batch=32。
- 若加中文别名表、或在卡片里把 genre 重复加权：**未测**（属新方案，需重新走计划/批准）。
- 生产环境（Java 侧）的并发、缓存、索引持久化成本：**未测**（本 issue 只测 embedding 检索质量）。

**未验证 / 已知空白**

- **只跑了一轮**（无 3 次重复的稳定性统计，计划里 E6 为"否/记录"档）；确定性只验证了查询向量，未验证 doc 向量跨进程一致性。
- **单人标注、n=60 方向性**（`OPEN-a5`）：主 agent 是标注/复核人，worker 只生成候选 + SQL 验证；n=60 下 1 条 = 1.67 个百分点，**不足以区分 0.48 与 0.55 这类差异**。
- **标注严格性**：similar 类可接受集合为"同导演/双类型+同年代"的完备集（§11.1），对 dense 检索偏严；filter 类为"完整 AND 集合"，同样严格。这是设计选择（可复核、可复现），但会压低绝对数字——已用 §12 诊断量化，**未改判定**。
- **catalog 噪声**：快照年份/类型标签本身有错（The Terminator 1991、A Clockwork Orange 1975、Earthlings 标 Horror 等），金标准集按快照事实标注，模型按现实先验排序时会"看起来错"。
- **`MC-U8`（无主演占位符）** 仍是暂定值 `主演：暂无（前 3）`，未获批准；`{stars}` 排序（业务 top-3 vs 文档 §1.3 字母序样例）也仍未统一（不影响 `template_hash`）。
- **`zh-full` 的"中文"只是中文模板 + 英文值**：本报告的所有"中文"结论都不等于"中文语义检索"，已在 §2.2 与实际卡片文本中体现。

---

## 14. 与先前估计/文档的差异（实测覆盖外推）

| 项 | 先前（计划/文档，外推） | 本次实测 | 判定 |
| --- | --- | --- | --- |
| tokens/卡 `zh-full`（`bge-m3`） | ≈48.0 | **47.2** | ✅ 吻合（−1.7%） |
| tokens/卡 `en-short`（`bge-m3`） | ≈25.3 | **26.8** | ⚠️ +5.9% |
| tokens/卡（`qwen3`） | 文档要求"必须重测" | `zh-full` **50.7** / `en-short` **29.1** | ✅ 已测 |
| `bge-m3` × `zh-full` 构建 | 7–9 min（理想）/ 15–20 min（预算） | **8.9 min** | ✅ 在理想区间 |
| `bge-m3` × `en-short` 构建 | 4–5 min | **8.6 min** | ❌ **约 2× 慢于外推**（外推按 token 速率，实测被 32 卡/batch 的**卡片速率**限制：40.8 卡/s） |
| `qwen3` × `en-short` 构建 | 未外推 | **5.3 min** | — |
| 4 组合合计 | 21–28 min 理想 / 45–60 min 预算 | **29.4 min** | ✅ 在预算内 |
| 稳态 tok/s | 1,860–2,450 | `bge-m3` **1,873**（下沿）/ `qwen3` **2,702**（更快） | 部分超出上沿（`qwen3`） |
| 热态 query 延迟 | 56–70 ms | **35–59 ms**（p50） | ✅ 更好 |
| 冷启动 | `bge-m3` 7.64 s（B0，本机） | **2.1–2.4 s**（远端端点） | 不可比（不同主机/模型加载路径），不得引用为改进 |
| 向量内存 | ≈86 MB（21087×1024×4） | **82.4 MiB = 86,372,352 B** | ✅ 吻合 |
| 13× batch 离群值 | 需留余量 | **未出现**（max/p50 = 1.3×；batch=32 且模型常驻） | 记录：该离群值来自更早的 B0 小样本，本次未复现 |
| 截断阈值 | 2,048 vs 8,192 冲突（`OPEN-a2`） | `bge-m3` = **2,048**、`qwen3` = **4,095** | ✅ `OPEN-a2` 由实测关闭 |
| 2010s/2020s 覆盖 | 2010s 94 部、2020s 7 部 | 同（金标准集合 2010s 命中 1/4 条相关用例、2020s 0–1/2） | 数据事实 |

### 14.1 `README.md` 的三处偏离（相对计划 §9 的替换文本）

本 PR 按计划 §9 替换 `README.md` 第 13/14 行（锚点 = 原文文本，未按行号 `sed`），并叠加主 agent 确认的**三处偏离**。`README.md` 相对 `HEAD` **只改第 13/14/16 行**，未动其他行（已核对 diff）。

**偏离 1 — 评分覆盖起点（已被计划 r5 吸收）**：`ratings exist only for 2000-onwards titles` → **`2001-onwards titles`**，句尾 `cannot match earlier decades` → **`cannot match 2000 or earlier`**。依据：只读 SQL 实测 rated 行的 `MIN(year) = 2001`、2000 年 0 部有评分（`snapshot.ratings_covered_from_year = 2001`）。**计划 r5（2026-11-09）已把这两处写进 §9 与 §10 `B15a-5`**；`docs/architecture/movie-cards.md`（r5）与 `docs/architecture/ai-user-workflows.md`（r4）也已把“2000 年及以后 / 2000+”统一改为 **2001+**（含 196–2000 共 11,975 部无评分）。**无遗留**。

**偏离 2 — 推荐的定位（第 16 行）**：原句 `Recommendations and multi-turn conversational features are deferred to the agent-platform stage.` → **`Recommendations are part of the user-side movie retrieval/recommendation target; multi-turn conversational features remain deferred to the agent-platform stage.`** 依据：用户已批准 D-7 把推荐纳入用户侧目标；原句把所有推荐一律推迟，与新方向冲突。计划 §9 附注原写“本 PR 不改第 16 行”，此处按主 agent 明确指示覆盖该附注。

**偏离 3 — 第 13 行的标题与质量声明（本报告实测驱动）**：计划 §9 的标题是 `**Movie catalog semantic retrieval and structured search (doable now):**`，但本次实测显示 dense 语义相似度**未达质量门槛**（`recall@5` 0.48 vs 0.80），把“semantic retrieval”整体标成 doable now 会与结论冲突。故改为：

> `**Movie catalog structured search (doable now) and semantic similarity search (under evaluation):**` … `Dense semantic similarity over the card text is still **under evaluation**: it currently falls below the retrieval quality gate (`recall@5` 0.48 against the 0.80 target), see [`docs/ai/reports/b1-5a-embedding-eval.md`](docs/ai/reports/b1-5a-embedding-eval.md).`

示例句 `"movies similar to X" or "1990s science fiction"` 与快照事实句保持不变。这是**第三处偏离计划 §9**，须由主 agent 追认；若要求与计划完全逐字一致，则需把计划 §9 同步更新后再合 PR。

---

## 15. 对 umbrella 决策备忘录的含义（不替主 agent 决定）

**可判定的事实部分**

1. **A 路线按当前卡片 + 当前两个候选模型，达不到 D8（`recall@5` ≥ 80%）**：最好 48.3%，两模型并集 53.3%。因此 `B15a-6` 要求如实记"未达标"，**不得**调阈值/换分母。
2. **A 路线在"片名/实体"上可用，在"结构化筛选 + 相似/推荐"上不可用**（§6/§11）。这与 `movie-cards.md` §3.1 的能力矩阵**部分冲突**：文档假设"类型/年代/导演/演员筛选可用"，实测**筛选类只有 10–45%**。
3. B 路线（工具/结构化过滤）的必要性因此**得到实测支持**：结构化约束在短文本 dense 检索里没有可靠表示（§11.2）。
4. 若未来还要保留 A 作为"混合方案的检索内核"，只能定位在**片名/实体召回 + `find_similar_movies` 的候选扩展**，且必须靠 B 路线的工具做最终过滤；这需要主 agent 决定是否写进 umbrella。
5. **对 B1.5b 的契约输入（实测驱动）**：相似类用例显示 dense 检索的 top-5 **15/15 都包含 seed 本身**，而评测的可接受集合按标注规则**排除 seed**。因此 `find_similar_movies.results[]` 必须**显式排除 seed**，且该约束目前**尚未写进 `docs/architecture/ai-user-workflows.md` §4 的工具契约**——docs-writer 会同步；若实现侧不排除，B 路线的相似类用例会出现「把 seed 自己当相似片返回」。

**不能从本报告得出的结论（避免过度外推）**

- 不能推断"任何 embedding 都不行"：只测了 2 个已安装候选、1 种卡片模板族、1 次运行、固定 batch。
- 不能推断"相似/推荐不可做"：只能说"用卡片文本的 dense 相似度做不好"，字段加权（结构化打分）方案未测。
- 不能推断"中文语义检索不可用"：`zh-full` 的值仍是英文（§2.2），这是数据现实。

---

## 16. 遗留风险与待裁定项

| ID | 项 | 状态 |
| --- | --- | --- |
| `OPEN-a2` | 截断阈值 | ✅ **实测关闭**：`bge-m3` 2048 / `qwen3` 4095（§3） |
| `OPEN-a3` | 中文译名别名表 | 🟡 仍未批准；实测显示 `qwen3` 自身可命中 3/3，`bge-m3` 1/3 |
| `OPEN-a4` | `zh-full` 与 `en-short` 是否双索引 | 🟡 数据**不支持**「只建 `en-short`」这种单向结论：`bge-m3` 反而是 `zh-full` **逐轨道 ≥** `en-short`（全 60：31 vs 29；zh_zh 11 vs 11、zh_en 9 vs 8、en_en 11 vs 10）；只有 `qwen3` 是 `en-short` 更优（26 vs 23）→ 只能说**模板优劣因模型而异，没有普适的单一模板**；再叠加 §11.2 的失败模式，双索引的收益不足以支撑 +82.4 MiB 与约 2× 构建成本 |
| `MC-U8` | 无主演占位符 | 🟡 本报告用 `主演：暂无（前 3）`，仍未批准 |
| `MC-U6` | genre 是否规范化（`Sci-Fi` vs `Science Fiction`） | 🟡 未定；本次卡片用 DB 原值 |
| — | `movie-cards.md` §1.3 样卡的 stars 字母序 vs §1.1 业务序 | 🟡 文档待统一（不影响 `template_hash`） |
| — | 金标准集 schema 无 negative 根形状 | 🟡 待裁定（`check_golden.py` 用 `queryCase` + 两处显式放宽） |
| — | 只跑一轮 / 单人标注 / n=60 / 冷启动 n=5 | 🟡 见 §13，属已声明的方向性限制（冷启动低于计划 §5.2 的 ≥20 次） |
| — | `template_hash` **不含** genres / stars 的空值段 | 🟡 改 `类型：暂无` / `Genres: N/A` / `主演：暂无（前 3）` 这三段文本**必须 bump `card_version`**，否则历史数字会被误当成可比（§0） |

---

## 17. 复现（命令与产物）

**前置条件**

- **Python ≥ 3.12**：`embed_eval.py` 用 `math.sumprod`（3.12 新增）做 21k×1024 的 C 级点积；3.11 及以下会 `AttributeError`。
- **MySQL 密码来源**：`MYSQL_PWD` 或 `MYSQL_ROOT_PASSWORD`，否则回落到仓库 `.env` 的 `MYSQL_ROOT_PASSWORD`（`.env` 已 gitignore）；连接参数用 `EVAL_DB_HOST` / `EVAL_DB_PORT` / `EVAL_DB_USER` / `EVAL_DB_NAME`（默认 `127.0.0.1:3307` / `root` / `moviedb`）。**刻意不读 `FABFLIX_DB_USER`**（那是应用账号，会 `Access denied`）。
- **换端点必须重测截断阈值**：§3 的 2048 / 4095 是**这台主机 + 这两个 digest** 的实测值；换模型 digest、换 Ollama 版本或换主机都要先跑 `--probe-truncation` 再跑全量，否则 `truncated_batches` 失去意义。
- 只读：DB 侧只有 `SELECT`（`ReadOnlyMysql.select()` 拒绝非 SELECT 与多语句）；评测不改数据、不写表。

```bash
# 0) 金标准集结构 + id 存在性 + 快照一致性 + db_snapshot_hash（约 1 s，只读 DB）
python3 scripts/eval/check_golden.py

# 0b) 卡片字符长度 / 空值计数（报告 §2.2 的数字来源）
python3 scripts/eval/embed_eval.py --card-stats

# 0c) 查询向量确定性（逐位比较）
python3 scripts/eval/embed_eval.py --determinism --models bge-m3:latest --sample 10 --repeat 3 --base-url http://<host>:11434

# 1) 端点与模型 digest
python3 scripts/eval/embed_eval.py --check-endpoint --base-url http://<host>:11434

# 2) 截断阈值实测（OPEN-a2）
python3 scripts/eval/embed_eval.py --probe-truncation --base-url http://<host>:11434

# 3) 冷启动（keep_alive:0 卸载后首调）
python3 scripts/eval/embed_eval.py --cold-start --base-url http://<host>:11434

# 4) 全量 4 组合（本次实跑；建议后台 + 轮询，单组合 5–9 min）
python3 -u scripts/eval/embed_eval.py --run-embedding --max-cards 21087 --template both \
  --models bge-m3:latest,qwen3-embedding:0.6b --base-url http://<host>:11434 \
  --out .pi/artifacts/b1-5a-eval --batch-size 32
```

产物（本机未提交，`.pi/` 已 gitignore）：

| 文件 | 内容 |
| --- | --- |
| `.pi/artifacts/b1-5a-eval/<model>__<template>__summary.json` | 该组合的 build/latency/fingerprint/分轨道汇总 |
| `.pi/artifacts/b1-5a-eval/<model>__<template>__queries.csv` | 60 行：命中、排名、top-5 id、相似度分数 |
| `.pi/artifacts/b1-5a-eval/<model>__gate.json` | D8 配对口径的 60 条明细 + 汇总 + alias 明细 |
| `.pi/artifacts/b1-5a-eval/check_golden.log` | `check_golden.py` 完整输出（含两个 sha256 与 `OK: schema…` 行，见 §17.1） |
| `.pi/artifacts/b1-5a-eval/card-stats.log` | 卡片长度 / 空值统计输出 |
| `.pi/artifacts/b1-5a-eval/probe-truncation.log` | `OPEN-a2` 饱和点实测输出 |
| `.pi/artifacts/b1-5a-eval/cold-start.log` | 冷启动 5 次重复输出 |
| `.pi/artifacts/b1-5a-eval/determinism.log` | 10 条 × 3 次向量比对输出 |
| `.pi/artifacts/b1-5a-eval/dry-run.log` | `--dry-run --sample 3` 的 headprint（卡片样例 + 指纹 + 计划矩阵） |

### 17.1 金标准集校验的实际输出（`B15a-1` / `B15a-2` / `B15a-3` 输入侧的证据）

命令 `python3 scripts/eval/check_golden.py --quiet`（完整输出见 `.pi/artifacts/b1-5a-eval/check_golden.log`；本次复核运行于 **2026-11-09T02:12Z**（日志首行带时间戳），`exit=0`）：

```text
composition: en_en=20 zh_en=20 zh_zh=20 | filter_catalog=20 recommend_by_preference=10 search_by_title=15 similar_movies=15 | cjk_alias_only=3 negatives=5
id source: live catalog (21087 rows)
db_snapshot_hash computed=8b16c047de5c885a2d90309797c51c0c506311c074927a2d5b3fc5ad162ff925
golden_version:  b1-5-golden/v1 / b1-5-negative/v1
sha256 docs/ai/golden/queries.b1-5.json:  0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079
sha256 docs/ai/golden/negative.b1-5.json: 09725672a2b8fb6ba330613f61533594ed7919b933a55d435c73f043d5fc75fd

OK: schema, composition, id existence, snapshot block and hashes all valid.
```

覆盖的验收点：`B15a-1`（60 条 / 分轨道计数 / 冻结哈希）、`B15a-2`（346 个可接受 id 全部在快照内）、`B15a-3` 的**输入侧**（schema 合法、`snapshot` 块与实库一致、`db_snapshot_hash` 重算吻合）。

---

## 18. 附录：60 条逐条命中矩阵（配对口径；`✅n` = top-5 命中且首个可接受结果排名 n；`✗10内` = 前 10 名内有、但 top-5 无；`✗` = 前 10 名内也没有）

| id | track | intent | query | 可接受数 | bge-m3 | qwen3 |
| --- | --- | --- | --- | --- | --- | --- |
| Q01 | zh_zh | search_by_title | 帮我找《The Matrix》这部电影 | 1 | ✅1 | ✅1 |
| Q02 | zh_zh | search_by_title | 有没有一部叫 Chief Zabu 的电影 | 1 | ✅1 | ✅1 |
| Q03 | zh_zh | search_by_title | 找片名以 Before 开头、而且是爱情片的电影 | 4 | ✅1 | ✅2 |
| Q04 | zh_zh | search_by_title | Toy Story 系列一共有哪几部 | 3 | ✅1 | ✅1 |
| Q05 | zh_zh | filter_catalog | 1960 年代的犯罪片 | 14 | ✅1 | ✗ |
| Q06 | zh_zh | filter_catalog | 1960 年代的动画片 | 7 | ✗ | ✗ |
| Q07 | zh_zh | filter_catalog | 1980 年代的爱情喜剧 | 11 | ✅1 | ✅3 |
| Q08 | zh_zh | filter_catalog | 1990 年代的犯罪惊悚片 | 3 | ✗ | ✗ |
| Q09 | zh_zh | filter_catalog | 2000 年代评分 8 分以上的悬疑片 | 12 | ✗ | ✗ |
| Q10 | zh_zh | filter_catalog | 2010 年以后评分 7.5 分以上的剧情片 | 8 | ✗ | ✗ |
| Q11 | zh_zh | filter_catalog | 2020 年以后的科幻片 | 2 | ✅1 | ✗ |
| Q12 | zh_zh | similar_movies | 和《Pulp Fiction》相似的电影 | 5 | ✗ | ✗ |
| Q13 | zh_zh | similar_movies | 喜欢《Terminator 2》，想看类似的 90 年代动作科幻片 | 3 | ✅2 | ✅2 |
| Q14 | zh_zh | similar_movies | 像《The Game》那样的动作惊悚片 | 3 | ✗ | ✗ |
| Q15 | zh_zh | similar_movies | 喜欢《When Harry Met Sally》那种爱情喜剧 | 3 | ✗ | ✗ |
| Q16 | zh_zh | similar_movies | 喜欢《Back to the Future》这种科幻片 | 3 | ✅2 | ✅2 |
| Q17 | zh_zh | recommend_by_preference | 推荐几部 Steven Spielberg 执导的电影 | 5 | ✅1 | ✅2 |
| Q18 | zh_zh | recommend_by_preference | 推荐 2000 年代评分 8 分以上的冒险片 | 8 | ✗ | ✗ |
| Q19 | zh_zh | recommend_by_preference | 推荐几部 1990 年代的喜剧动作片 | 10 | ✗ | ✗10内 |
| Q20 | zh_zh | recommend_by_preference | 推荐 1980 年代的动画片 | 20 | ✅3 | ✅2 |
| Q21 | zh_en | search_by_title | 找一下《黑客帝国》 | 1 | ✗ | ✅2 |
| Q22 | zh_en | search_by_title | 《阿甘正传》是哪部电影 | 1 | ✗ | ✅1 |
| Q23 | zh_en | search_by_title | 我想看《泰坦尼克号》 | 1 | ✅1 | ✅1 |
| Q24 | zh_en | search_by_title | 找片名叫 Infernal Affairs 的电影 | 1 | ✅1 | ✅2 |
| Q25 | zh_en | search_by_title | 有没有叫 Memories of Murder 的电影 | 1 | ✅1 | ✅1 |
| Q26 | zh_en | search_by_title | 找一下 The Big Lebowski 这部片 | 1 | ✅1 | ✅1 |
| Q27 | zh_en | filter_catalog | 1970 年代的犯罪片 | 8 | ✅1 | ✗ |
| Q28 | zh_en | filter_catalog | 1980 年代的西部片 | 11 | ✅4 | ✗ |
| Q29 | zh_en | filter_catalog | 2000 年代评分 8 分以上的动作片 | 15 | ✗ | ✗ |
| Q30 | zh_en | filter_catalog | 1990 年代的剧情动作片 | 4 | ✗ | ✗ |
| Q31 | zh_en | filter_catalog | 2010 年以后的动画片 | 7 | ✅1 | ✗ |
| Q32 | zh_en | filter_catalog | 2020 年以后评分 8 分以上的电影 | 2 | ✗ | ✗ |
| Q33 | zh_en | similar_movies | 想找像《The Fugitive》那样的动作惊悚片 | 3 | ✗ | ✗ |
| Q34 | zh_en | similar_movies | 喜欢《Men in Black》，有类似的吗 | 4 | ✗ | ✗ |
| Q35 | zh_en | similar_movies | 想找像《Fargo》那样的片子 | 7 | ✗ | ✗ |
| Q36 | zh_en | similar_movies | 像《Heat》一样，还想看 Michael Mann 的其他片子 | 6 | ✗ | ✗10内 |
| Q37 | zh_en | similar_movies | 喜欢《Toy Story》这种动画喜剧，有类似的吗 | 4 | ✗ | ✗ |
| Q38 | zh_en | recommend_by_preference | 推荐 2000 年代评分 8 分以上的传记片 | 17 | ✗ | ✅3 |
| Q39 | zh_en | recommend_by_preference | 推荐 2010 年以后评分 7 分以上的纪录片 | 3 | ✗ | ✗ |
| Q40 | zh_en | recommend_by_preference | 推荐几部 Quentin Tarantino 的电影 | 3 | ✅1 | ✅1 |
| Q41 | en_en | search_by_title | find the movie City of God | 1 | ✅1 | ✅1 |
| Q42 | en_en | search_by_title | is there a film called Mulholland Dr.? | 1 | ✅1 | ✅1 |
| Q43 | en_en | search_by_title | movies whose title starts with The Godfather | 3 | ✅1 | ✅1 |
| Q44 | en_en | search_by_title | find the movie Cast Away | 1 | ✅1 | ✅2 |
| Q45 | en_en | search_by_title | find the movie Ocean's Eleven | 2 | ✅1 | ✅1 |
| Q46 | en_en | filter_catalog | film noir from the 1970s | 6 | ✅1 | ✗ |
| Q47 | en_en | filter_catalog | 1960s westerns that are also comedies | 3 | ✗ | ✗ |
| Q48 | en_en | filter_catalog | 1990s romantic musicals | 4 | ✗10内 | ✗ |
| Q49 | en_en | filter_catalog | thrillers from 2000 to 2009 rated 8.5 or higher | 6 | ✗ | ✗ |
| Q50 | en_en | filter_catalog | horror movies from the 2000s rated 7.5 or higher | 14 | ✅1 | ✅5 |
| Q51 | en_en | filter_catalog | family movies from 2000 to 2009 rated 8 or higher | 5 | ✗10内 | ✗ |
| Q52 | en_en | filter_catalog | sci-fi movies from 2000 to 2009 rated 7.5 or higher | 6 | ✅2 | ✗10内 |
| Q53 | en_en | similar_movies | something like Face/Off | 3 | ✗ | ✗ |
| Q54 | en_en | similar_movies | movies like True Lies | 5 | ✗ | ✗ |
| Q55 | en_en | similar_movies | I love Blue Velvet - anything similar? | 7 | ✗ | ✗ |
| Q56 | en_en | similar_movies | more films like Before Sunrise | 4 | ✅2 | ✅2 |
| Q57 | en_en | similar_movies | I want more movies like The Shining | 12 | ✗ | ✗ |
| Q58 | en_en | recommend_by_preference | recommend animated movies from the 2000s rated 7.5 or higher | 10 | ✗10内 | ✗ |
| Q59 | en_en | recommend_by_preference | recommend mystery movies from the 2000s rated 8.5 or higher | 4 | ✗ | ✗ |
| Q60 | en_en | recommend_by_preference | recommend some films by Scorsese | 23 | ✅1 | ✅1 |

---

## 19. 修订记录

| 日期 | 变更 |
| --- | --- |
| 2026-11-09 | **创建**：4 组合全量实测（21,087 卡 × 2 模板 × 2 模型）+ D8 判定（均不通过）+ 失败模式分析 + `OPEN-a2` 实测关闭 + 诊断性数字（非判定口径） |
| 2026-11-09 | **审查回改（未重跑评测，数字不变）**：① §0/§5 补快照事实与 `template_hash` 覆盖范围（genres/stars 空值段不在 hash 内 → 改动须 bump `card_version`）；② §7 补截断判定口径（batch 均值）；③ §8 冷启动改三栏并记录 n=5 < 计划 ≥20；④ §9 45.8%→**46.7%**、§1 的 29/57 与 29/45 标注「两模型并集」、§12 补并集 n=57 行；⑤ §10 改为带 `movie_id` 的精确表述（Q56 命中 `RLl21`、Q60 命中 `MS29`，两者均非 0-genre 行）；⑥ §11.3 增加限定（英文片名用 `LIKE` 更省；中文译名 SQL 0/3 vs `qwen3` 3/3）；⑦ §13 拆成「有落盘产物 / 控制台未落盘」并列出 6 个日志；⑧ §14.1 改为三处偏离计划 §9（含 README 标题与质量声明）；⑨ §15 增加 `find_similar_movies.results[]` 必须排除 seed 的 B1.5b 契约输入；⑩ §16 修正 `OPEN-a4` 结论；⑪ §17 增加前置条件（Python ≥3.12 / 密码来源 / 换端点须重测截断）与 §17.1 校验实际输出；⑫ 端点地址按规定改为 `http://<host>:11434` |

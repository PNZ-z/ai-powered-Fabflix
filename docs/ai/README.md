# `docs/ai/` — B1.5 评测契约产物（A/B 共用）

> **状态：🟡 草案（2026-11-09，**r2 增量修订**），待 oracle 复核 / 用户批准。**
> 本目录存放**机器可读的评测契约**（不是评测结果、不是生产配置）。评测脚本本身放在 `scripts/eval/`（stdlib-only，用户已批准 D-4），**CI 不依赖**这些文件，也不依赖本机或局域网端点。
>
> | 元数据 | 值 |
> | --- | --- |
> | `last_updated` | **2026-11-09**（**r4：按 B1.5a 实测回填**——修正 `golden/queries.b1-5.json` 的 sha256（原误写 `0c2f4c5e…eacf`，实际 **`0c2f4c5e…be079`**）、把 queries/negative/report 标为已完成、记录报告 D8 FAIL 结论；r2 增量修订：修 `db_snapshot_hash` 的 stars 子查询——原 SQL 引用了不存在的 `stars_in_movies.star_movie_count`；补记 `filter_movies`/`recommend_movies` 的 `anyOf` 语义。`schema_hash` **未变**） |
> | 审批依据 | 用户 2026-11-09 批准 D-4（提交 golden set + schema + 脚本）、D-7（`AC-B-12` 改写为两条路线）、B-4（60 + 15 样本）、B-5（schema fixture）、B-6（fingerprint） |
> | 关联计划 | `docs/plans/b1-5-movie-retrieval-eval.md`（umbrella）、`docs/plans/b1-5a-movie-card-embeddings.md`、`docs/plans/b1-5b-tool-routing.md` |
> | 关联规范 | `docs/architecture/ai-user-workflows.md`、`docs/architecture/movie-cards.md` |

---

## 1. 目录内容与状态

| 文件 | 用途 | 状态 |
| --- | --- | --- |
| `tool-schema.b1-5.json` | **B1.5b 的唯一机器可读工具 schema 契约**（G0–G3 共用同一份，含 `enum` / `required` / `additionalProperties:false` / `minimum` / `maximum` 与 `schema_hash`；`filter_movies`/`recommend_movies` 用对象级 `anyOf` 表达“至少一个筛选/偏好条件”，见 §2.3） | ✅ 已创建（草案） |
| `golden-set.schema.json` | 金标准集与工具用例的 JSON Schema（`queries` 60 条 / `tool_cases` 15 条两种根形状） | ✅ 已创建（草案） |
| `golden/queries.b1-5.json` | 60 条金标准查询（**B1.5a 与 B1.5b 共享**：20 zh→zh / 20 zh→en / 20 en→en） | ✅ **已创建、冻结并被 B1.5a 使用**（`b1-5-golden/v1`，sha256 **`0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079`**；主 agent 已审核批准为**候选集**：单人标注、n=60 方向性） |
| `golden/tool-cases.b1-5.json` | 15 条工具用例（拒绝 6 / 澄清 4 / 多槽位 5） | ⬜ 待 B1.5b 实施时写入 |
| `golden/negative.b1-5.json` | 5 条反例（边界证明，不计入分母） | ✅ 已创建、冻结并被 B1.5a 引用（`b1-5-negative/v1`，sha256 **`09725672a2b8fb6ba330613f61533594ed7919b933a55d435c73f043d5fc75fd`**） |
| `reports/b1-5a-embedding-eval.md` | A 路线评测报告（实测数字） | ✅ **已完成（2026-11-09 实测，4 组合 × 21,087 卡 × 60 条）**；**D8 `recall@5` ≥ 80% 未达标（FAIL）**：`bge-m3` **0.4833（29/60）** / `qwen3-embedding:0.6b` **0.4167（25/60）** |
| `reports/b1-5b-tool-routing.md` | B 路线评测报告（实测数字） | ⬜ 待生成 |

> **本目录不存放**：开发文档语料、`docs/**` 的任何切分/索引、密钥、端点地址。**开发文档不是用户语料**（用户已批准 D-1/D-5）。

---

## 2. 三个 fingerprint 的算法（B-6）

任何进入决策的数字都必须带下列 fingerprint。**算法在此固定，实现脚本必须逐字照做**；改变算法即等于改变口径 → 必须 bump 对应版本号并在报告中说明"与历史数字不可比"。

### 2.1 `template_hash`（卡片模板）

```
template_hash = sha256( utf8( card_version + "|" + template_id + "|" + template_literal + "|" + field_order + "|" + null_placeholder ) )
```

| 参数 | 取值来源 | 例 |
| --- | --- | --- |
| `card_version` | `movie-cards.md` §2 的版本号 | `movie-card/v1` |
| `template_id` | 模板 ID | `zh-full` / `en-short` / `pipe-full` |
| `template_literal` | §2.1 的**字面模板串**（保留 `{placeholder}` 原样，不做任何变量替换） | `电影《{title}》（{year}）导演 {director}；…` |
| `field_order` | 占位符**出现顺序**，逗号连接 | `title,year,director,genres,stars,rating` |
| `null_placeholder` | 缺失评分时的字面替代 token | `评分 暂无`（zh）/ `Rating: N/A`（en） |

- 一个模板一个 `template_hash`；报告按 `(template_id, template_hash)` 成对记录。
- 卡片**文本内容**不入 hash（那是 `db_snapshot_hash` 的职责）；本 hash 只锁"模板定义"。

### 2.2 `db_snapshot_hash`（数据快照）

规范化导出（**导出 SQL 与规范化规则冻结为 `snapshot_export_version = 1`**，脚本必须内含该 SQL）：

```sql
SELECT m.id, m.title, m.year, m.director,
       (SELECT GROUP_CONCAT(DISTINCT g.name ORDER BY g.name)
          FROM genres_in_movies gm JOIN genres g ON g.id = gm.genre_id
         WHERE gm.movie_id = m.id) AS genres_sorted,
       (SELECT GROUP_CONCAT(s.name ORDER BY s.name)
          FROM (SELECT st.name
                  FROM stars_in_movies sm
                  JOIN stars st ON st.id = sm.star_id
                 WHERE sm.movie_id = m.id
                 ORDER BY st.name
                 LIMIT 3) s) AS stars3_sorted,
       r.rating
  FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id
 ORDER BY m.id;
```

- **SQL 有效性已实测（2026-11-09，3307）**：整条导出 SQL 可执行；样例行 `AA1`（`genres_sorted` = `NULL`、`stars3_sorted` = `NULL`、`rating` = `NULL`）、`AA13`（`Comedy` / `David Tree,Esme Percy,Everley Gregg` / `NULL`）、`tt0094859`（`Comedy` / `Allan Arbus,Allen Garfield,Ed Lauter` / `7.9`）。
- **必须使用本版的子查询**：`stars_in_movies` 只有 `(star_id, movie_id)` 两列，**没有**出演数字段；旧草案里的 `ORDER BY sm.star_movie_count DESC` 会直接报 `ERROR 1054 Unknown column`。

逐行规范化（`\u001f` = U+001F）：

```
row = id \u001f title \u001f year \u001f director \u001f genres_sorted \u001f stars3_sorted \u001f rating
rating: 缺失写 NULL；存在则保留一位小数（如 7.9）
row_hash = sha256(utf8(row)).hexdigest()
db_snapshot_hash = sha256(utf8(join(sorted(row_hash), "\n"))).hexdigest()
```

同时记录（可读、便于人核对）：`row_count`、`year_min`、`year_max`、`genre_rows`、`genre_distinct_names`、`movies_without_rating`、`ratings_covered_from_year`。

- 说明：`stars3_sorted` 取的是**按 `name` 字典序最小的 3 位主演**（内层 `ORDER BY st.name LIMIT 3`，外层再 `ORDER BY s.name` 保证输出稳定），**不**复刻业务侧“按出演数排序取前 3”的语义（那是卡片内容口径，见 `movie-cards.md` §1.1）；hash 只需要**稳定且可复现**，不需要与业务排序一致。若团队希望 hash 反映真实 top-3（业务口径），必须 bump `snapshot_export_version = 2` 并重算历史。
- `genres_sorted` 保留 `DISTINCT` + `ORDER BY g.name`；`rating` 缺失写 `NULL`、存在则保留一位小数——这两条与上面的 SQL/规范化规则**逐字对应**，实现脚本不得另行改写。
- **本快照的关键性质**（会影响结论解释）：21,087 部 / 196–2024 / `genres` 61 行 56 distinct / 13,082 部无评分 / **评分行只覆盖 2001 年及以后**（实测定界：`MIN(year)` among rated rows = **2001**；2000 年的 58 部与 196–2000 的 **11,975** 部**全部**无评分；8,005 部有评分）。

### 2.3 `schema_hash`（工具契约）

```
schema_hash = sha256( utf8( json.dumps(tools, sort_keys=True, separators=(",", ":"), ensure_ascii=False) ) )
```

- `tools` = `tool-schema.b1-5.json` 顶层的 `tools` 数组（不含 `$comment` / `db_snapshot` / `error_contract` 等元数据）。
- 当前记录值（**2026-11-09 计算，`contract_version = b1.5-tools/v2`**；v2 仅改描述性事实 2000→2001，见 §4 r3 行）：

| 项 | 值 |
| --- | --- |
| `schema_hash` | `8e72f74120e43c5f4a40b1f8815d952d0bf029338a94e328047c60907fd8a567` |
| 整文件 `file_sha256` | `b54b0f28c4a086b01c64ff634dfbde87caa34688a975b3e74c1256f29b19f2c5` |
| 文件字节数 | 14,225 |
| 工具数 | 5（`search_movies_by_title` / `filter_movies` / `find_similar_movies` / `recommend_movies` / `get_movie_card`） |

- **`required` / `anyOf` 的实际形态（2026-11-09 r2 复核，逐项验证）**：

| 工具 | `required` | 对象级 `anyOf` |
| --- | --- | --- |
| `search_movies_by_title` | `["query"]` | 无 |
| `filter_movies` | **空（故意不用 `required`）** | ✅ `genres` / `year_from` / `year_to` / `min_rating` **至少一个**（`tool-schema.b1-5.json` L80–101） |
| `find_similar_movies` | `["movie_id"]` | 无 |
| `recommend_movies` | **空（故意不用 `required`）** | ✅ `preferred_genres` / `year_range` / `min_rating` / `director` / `seed_movie_ids` **至少一个**（L246–272） |
| `get_movie_card` | `["movie_id"]` | 无 |

- 即：“至少一个筛选/偏好条件”由 **`anyOf`** 表达，而不是把 `required` 写成互斥项（写成 `required` 会强迫模型每次都填全部字段）。因此“**`required` / `anyOf` 显式**”是 B15b-1 的验收口径，**不是**“所有工具 `required` 非空”。
- **r2 未修改 `tool-schema.b1-5.json`**（`anyOf` 已在文件中）；当时的 `schema_hash` 是**复算确认**而非变更。
- **r3（2026-11-09，数据事实修正）改了 fixture 内两处描述性文本**（`db_snapshot.notes` 与 `filter_movies.min_rating.description` 的 2000→2001）→ `schema_hash` **由 `8eea424b…44cd` 变为 `8e72f741…a567`**（`tools[]` 的文本参与 hash；`db_snapshot.ratings_covered_from_year` 本身不影响 hash）。按 §3 规则 1 已 **bump `contract_version` v1 → v2**；没有任何评测/报告使用过 v1 的 hash，故“与上一版不可比”无实际影响，但**报告引用时必须用 v2 的 hash**。

- **A 路线（B1.5a）不使用工具** → 其报告必须写 `schema_hash = N/A`（显式写 N/A，不得留空）。
- **B1.5a 已完成（2026-11-09）**：报告 = `reports/b1-5a-embedding-eval.md`；`schema_hash = N/A` 已按本节要求显式写出；其余 fingerprint 实测值见 `docs/architecture/movie-cards.md` §6（`template_hash` `c5990dc4…`/`bc3bcce6…`、`db_snapshot_hash` `8b16c047…`）与本文件 §2.2。**D8 判定：两个候选均 FAIL**（0.4833 / 0.4167，阈值未调整、分母未更换）。
- 生产实现后以 `@Tool`/`@ToolParam` 生成的 schema 为真值；必须加一致性测试断言"注解生成的 schema ≡ 本 fixture 的语义"，不一致时以代码为准并同步修订本文件 + bump `contract_version`。

---

## 3. 维护规则

1. 改 `tool-schema.b1-5.json` → bump `contract_version`、重算 `schema_hash` 与 `file_sha256`、更新本文件 §2.3 表、在报告注明"与上一版不可比"。
2. 改金标准集内容 → bump `golden_version` 并重算文件 SHA-256（写入报告）。
3. 改导出 SQL 或规范化规则 → bump `snapshot_export_version` 并说明历史数字不可比。
4. 本目录**不得**加入第三方依赖、端点地址、密钥或开发文档内容。

---

## 4. 修订记录

| 日期 | 版本 | 变更 | 是否改变 `schema_hash` |
| --- | --- | --- | --- |
| 2026-11-09 | r1 | 创建：三个 fingerprint 算法、工具 schema fixture、当前 hash 值 | 初版 |
| 2026-11-09 | **r2** | ① `db_snapshot_hash` 的 `stars3_sorted` 子查询改为**可执行版**（原 `sm.star_movie_count` 列不存在，实跑报 `ERROR 1054`），并在 3307 上实测整条 SQL（样例 `AA1`/`AA13`/`tt0094859`）；② 明确 `stars3_sorted` = 按 `name` 字典序前 3（不复刻业务 top-3），`genres_sorted` 仍为 `DISTINCT`+`ORDER BY`，`rating` 缺失 `NULL`/一位小数，`snapshot_export_version` 仍为 `1`；③ §2.3 增加 `required` / `anyOf` 逐工具表（`filter_movies`/`recommend_movies` 的“至少一个条件”由 `anyOf` 表达） | **否**（`anyOf` 本已在 fixture 中，复算 `schema_hash` 与 `file_sha256` 均与记录一致） |
| 2026-11-09 | **r3** | 数据事实修正：评分覆盖起点 **2000 → 2001**（实测 `MIN(year)` among rated = 2001；2000 年 58 部全无评分）。改动 `tool-schema.b1-5.json` 的 `db_snapshot.ratings_covered_from_year` 与两处描述文本；bump `contract_version` **v1 → v2**；重算 hash | **是**（`8eea424b…44cd` → `8e72f741…a567`；`file_sha256` → `b54b0f28…f2c5`，14,225 B） |
| 2026-11-09 | **r4** | 按 B1.5a 实测报告回填：① 修正金标准集 sha256 误写（`0c2f4c5e…eacf` → 实际 `0c2f4c5e2eed33333b804487ddb9473f9fb25362c4271cb7ba5c1d3ac22be079`）；② §1 状态表把 `golden/queries`、`golden/negative`、`reports/b1-5a-embedding-eval.md` 标为**已完成/已使用**；③ 记录报告结论 **D8 FAIL**（`bge-m3` 0.4833 / `qwen3` 0.4167）与 `schema_hash=N/A` 已显式写出 | **否**（未改 `tool-schema.b1-5.json`） |

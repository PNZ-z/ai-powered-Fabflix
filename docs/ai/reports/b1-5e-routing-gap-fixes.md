# B1.5e — 修 B1.5d 暴露的路由/执行缺口并落码 `R-1`/`R-2`（**in-sample 回归报告，不是门槛**）

> | 元数据 | 值 |
> | --- | --- |
> | 日期 | 2026-11-21 |
> | issue | [#78](https://github.com/PNZ-z/ai-powered-Fabflix/issues/78)（`Refs #74`，**#74 保持 OPEN**） |
> | 计划 | `docs/plans/b1-5e-routing-gap-fixes.md`（r2；`OD-1`…`OD-7` + 后续裁定 ③b2） |
> | 前序事实 | B1.5d blind holdout（已合并，PR #77）`G1-T`（冷）**25/30** < 27/30 → FAIL；该 30 条因本任务的规则改动已成 **seen fixture**，数字冻结在 `docs/ai/reports/b1-5d-blind-holdout.md` |
> | 结果性质 | **全部数字都是 in-sample / seen 回归证据**。本报告不声称泛化、不声称 B1.5c/B1.5d/B1.5e 达标、不声称生产代码有任何改动。泛化只能由 B1.5f 的一次冷跑回答，而 **B1.5e 不生成、不冻结、不运行 B1.5f**（本仓库没有任何 B1.5f fixture 或 `blind_version` 引用） |
> | 改动文件 | `scripts/eval/tool_eval.py`、`docs/ai/aliases/genre-aliases.json`、`scripts/eval/check_schema.py`（组断言）、`docs/ai/README.md`（追加一行）、本报告。**零改动**：`src/main/**`、`pom.xml`、CI、`docs/ai/tool-schema.b1-5.json`、`docs/ai/golden-set.schema.json`、`scripts/eval/check_blind_holdout.py`、B1.5a–B1.5d 的任何 fixture/计划/报告 |
> | 契约指纹 | `contract_version b1.5-tools/v3`、`schema_hash 712c0e34c37b6de140c13fcc7d3de15e67ac1f131f2bb6e8751a4f5e4df3474a`（**未变**，`check_schema.py` 重算一致） |
| 本轮修订（三路 reviewer 的已接受意见） | P1：`TITLE_PATTERNS[7]` 加 `FUNCTION_WORDS`/类型词首词守卫（`movies I enjoyed in the 1990s`、`I enjoyed comedy films from the 1990s` 不再被当成片名）＋ 兜底边界修汉字粘连（`查一下The Matrix`）；P1：补齐 `F-5` 闸门/整十年/逐键覆盖、`R-1` union、demonym DB 非空池共 9 个新 label（43 → 61）；P1：新跑一次 held-out `G1`（§8.2）；P2：本文的行数/池量纲、`A12` 未执行、自测条数口径已按 artifact 改正。**行为只在上述守卫生效处变化：135 条 fixture 的路由产物逐条 diff 为零，in-sample 仍 70/75、`Q12` 仍为唯一 similar 回归** |

---

## 1. 结论摘要（in-sample）

| 指标（75 条 in-sample `G1`，`--cases both`） | 修复前 | 修复后 |
| --- | --- | --- |
| `task_success` | **67/75** | **70/75** |
| `routing_success` | 73/75 | 74/75 |
| `similar_movies` | 11/15 | **13/15** |
| `filter_catalog` | 19/20 | **20/20** |
| `tools_ok` / `slots_ok` / `schema_ok` / `behavior_ok` / `grounding_ok` | 75 / 74 / 75 / 75 / 75 | 75 / **75** / 75 / 75 / 75 |
| `annotation_guard` | true | **true**（`--assert-no-annotation-read`） |

- 变绿：`Q34`、`Q48`、`Q53`、`Q54`。
- 变红：**`Q12` 一条**（主 agent 明确批准的例外，§4.3）。
- 其余 70 条判定逐条不变（含 `Q16`/`Q35`/`Q36`/`Q55`/`Q56` 全部保持 ✅，`Q57` 仍 ❌ = `OPEN-C3` 未落码）。
- 本轮（reviewer 意见修复后）重跑的 `after-G1.json` 与修复前一轮**逐用例零差异**（`task_success`/`routing_success`/`rows_returned`/`candidate_pool`/`derived_constraints` 全同），即上表数字不变；对比基准 `.pi/artifacts/b1-5e/after-G1-before-titleguard.json`。

---

## 2. 五个缺口与逐项改动

| # | 缺口（B1.5d） | 改动 | 位置 |
| --- | --- | --- | --- |
| `F-1` | 越界请求（`B10` 票房）不被拒绝 —— `REFUSAL_RULES` 只有 `plot`/`transaction`/`internal`/`external` | 新增 `out_of_catalog` 类：`票房`/`预算`/`发行公司`/`配乐`/`作曲`/`奥斯卡`/`获奖`/`奖项` + `box office`/`grossed`/`budget`/`distributor`/`which studio`/`who composed`/`composer`/`academy award`/`oscars`。**故意不含** `score`（评分）、`music`（类型）、`released`（"released in 1999"）、裸 `studio`（"Studio Ghibli"） | `tool_eval.py` `REFUSAL_RULES` |
| `F-2` | 片名区段污染（`B26`：`The Addams Family` 的 `Family` 被抽成类型）＋ 兜底片名抽取把从句当片名（`B23`、`H23`） | ①`TITLE_PATTERNS` 动词扩为 `like/liked/love/loved/enjoyed/watched`；②`extract_title` 兜底改为**从 marker 之后**搜索 `EN_TITLE_RUN`，并加"首词必须是原文中真正的大写起始词"＋ `TITLE_STOPWORDS`（show/find/get/tell/me/i）＋ 类型别名守卫，因此 `liked …` 从句与 `Show me …`/`Find …` 句子都不再是片名；③路由用**去掉返回片名后的文本**抽 genres/years/rating（按返回的 title 字符串掩码，不按 pattern span）；④**动词语句守卫**（reviewer P1）：verb-pattern 捕获的首词若落在 `FUNCTION_WORDS`（`in`/`at`/`on`/`from`/`of`/`the`/`to`/`this`/`that`/`it`/`as`/`by`/`with`/`for`/`a`/`an`/`my`/`your`/`all`/`some`/`when`/`where`/`how`/`what`/`who`/`was`/`were` 等）或类型词（`comedy` 等别名/枚举词）中，判为从句并**继续后续 pattern 与兜底**，因此 `movies I enjoyed in the 1990s`、`I enjoyed comedy films from the 1990s` 都不再有片名（否则掩码会连类型和年代一起删掉），而 `I really liked Trainspotting` 仍是片名；⑤兜底边界把"前一个字符是汉字"也视为词边界（`查一下The Matrix` → `The Matrix`，`str.isalnum()` 对汉字为真，原写法会拒绝） | `tool_eval.py` `TITLE_PATTERNS`/`TITLE_VERB_PATTERN`/`FUNCTION_WORDS`/`GENRE_TITLE_WORDS`/`extract_title`/`route_deterministically` |
| `F-3a` | `documentaries` 未抽取（`B27`） | 别名表**只**新增 `"documentaries": ["Documentary"]`（唯一新增复数；`dramas`/`horrors`/`histories` 等一律不加） | `docs/ai/aliases/genre-aliases.json` |
| `F-3b` | `Recommend some Italian <genre>` 把国籍形容词抽成导演 → `1 = 0` → 0 行 | 新增路由器本地 `DEMONYM_STOPLIST`（30 个国籍/语言形容词），`extract_director` 逐匹配跳过命中项；`Recommend some Italian documentaries from the 1960s` → `genres ["Documentary"]`、`director None`、`recommend_movies{preferred_genres, year_range 1960-1969}`；同表修复既有的 `Italian comedies` | `tool_eval.py` |
| `F-4` | `R-1`：`musical(s) → Musical` 与冻结标注的 `Music` 口径冲突（`B22`/`Q48`） | `canonical_groups` 增 `["Music","Musical"]`（canonical key = `Music`），`aliases_version = b1-5c-aliases/v2`，`check_schema.py` 增同组断言；**不改**别名目标、`MUSICAL_SPLIT`、任何标注 | 见 §3 |
| `F-5` | `R-2`：无文本约束的相似查询完全不收窄（`B23`/`Q34`/`Q53`/`Q54`） | `run_g1` 种子跟随：文本无 director 槽且**种子 canonical genres 恰好 2** 时，派生 `genres = 种子全部 canonical genres` + `year_from/year_to = 种子整十年`；文本已给的键**逐键覆盖**；派生池空**不回退**；记录 `derived_constraints`/`effective_constraints`/`seed_genre_count` | 见 §4 |

`B26`/`H23` 双锚点（`B15e-3`）实测：`B26` 掩码后 `genres == ["Horror"]`、`years == 1991-1992`（断言 `route: genre words inside a title are not constraints`），`H23` 的 `extract_title` 返回 `None`（断言 `route: a sentence is not a title`）。`H23` 在 held-out 里的判定本轮**实测未变**：`filter_movies{genres:["History"], 1998–1999}`、`task_success true`（§8.2，一次 `G1` 跑，0 model）。

---

## 3. `R-1`：`["Music","Musical"]` 同族（实测面）

**语义**：用户词 `musical` 与 `music` 指同一族，**检索/槽位比较**折成一个 canonical key；**catalog enum 仍分开**（`Music` 619 行 / `Musical` 139 行）。因此这是 **召回扩大（recall expansion），不是等价替换**：任一 `filter_movies{genres:["Music"]}` 现在也会返回 `Musical` 行。B1.5f 的 `R-1` 探针必须按 **union（25 行）** 标注，不能沿用 B1.5d 的 24 行口径。

可复现 SQL（只读快照 21,087 行，`db_snapshot_hash 8b16c047…f925`）：

```sql
-- 1948–1950 ∧ Music
SELECT COUNT(*) FROM movies m WHERE m.year BETWEEN 1948 AND 1950 AND m.id IN (
  SELECT gm.movie_id FROM genres_in_movies gm JOIN genres g ON g.id = gm.genre_id
  WHERE g.name = 'Music');                                   -- = 24
-- 1948–1950 ∧ Musical
... WHERE g.name = 'Musical';                                -- = 1  (ChW4 The Barkleys of Broadway)
-- 1948–1950 ∧ (Music ∨ Musical)
... WHERE g.name IN ('Music','Musical');                     -- = 25
-- 同一窗口同时带两个值（双重标记）的行
... WHERE g.name IN ('Music','Musical') GROUP BY gm.movie_id HAVING COUNT(DISTINCT g.name) = 2;
                                                             -- = 0
```

`_filter` 是 `ORDER BY m.title, m.year LIMIT`，`filter_movies{genres:["Music"], 1948–1950, limit 5}` 的 top-5：

| # | id | title | year | 目录类型 |
| --- | --- | --- | --- | --- |
| 1 | `TG32` | A Connecticut Yankee in King Arthur's Court | 1949 | Music |
| 2 | `RTh23` | A Date With Judy | 1948 | Music |
| 3 | `HH29` | A Song is Born | 1948 | Music |
| 4 | `GSy9` | Annie Get Your Gun | 1950 | Music |
| 5 | `IBe4` | Casbah | 1948 | Music |

- top-5 **全部是 `Music` 行**，`ChW4`（唯一的 `Musical` 行）**不在** top-5。
- B1.5d `B22` 的冻结 `acceptable_movie_ids` 仍是 24 行（不含 `ChW4`），top-5 ∩ 可接受集 = 5/5 → **`acceptable_hit = true`**，`B22` 由 ❌→✅（见 §8）。
- `Q48`（1990s ∧ Romance ∧ Music）= **4 行**（`WA25`/`PJH14`/`IMd17`/`THk14`），由 ❌→✅；`B05`（1972–1974 ∧ Music）= **5 行**，判定不变（✅）。
- 未改：`Q48`/`B22` 标注、`golden_version`/`blind_version`、`MUSICAL_SPLIT`、任何 checker 正则。

---

## 4. `R-2`：派生约束 + 显式优先（裁定 ③b2）

### 4.1 规则

```python
stated   = constraints or {}                       # 只来自 route（文本）
seed_genres = canonical_genres(seed["genres"])     # 只来自 seed 目录行
derive   = not stated.get("director") and len(seed_genres) == 2
derived  = {"genres": seed_genres} 若 derive 且文本没给 genres
         + {"year_from","year_to": 种子整十年} 若 derive 且文本没给年代
effective = {**derived, **stated}                  # 显式键逐键覆盖
```

- **空派生池不回退**（OD-1）：不收窄成"只排序"，如实失败。
- `narrow_seed_candidates` 仍只用**文本**约束（消歧语义不变）。
- 只读 route（文本）+ seed 行（catalog）→ `annotation_guard` 全程 true。

### 4.2 裁定链与最终形态

| 版本 | 规则 | 实测（in-sample 75） | 结果 |
| --- | --- | --- | --- |
| OD-1 原文 | 全部 seed canonical genres + 整十年，逐键覆盖 | 67/75 | 回归 `Q12`/`Q36`/`Q55`/`Q56` → 超出零回归闸门 |
| ③b | 种子 ≤2 类型才派生，其余同上 | 69/75 | 回归 `Q12`/`Q56` → 触发停止条件 |
| **③b2（最终）** | **种子 canonical genres 恰好 2 才派生**，有 director 槽不派生 | **70/75** | 回归仅 `Q12`（已批准例外） |

1 类型种子（`Q16`/`Q35`/`Q56`）与 4 类型种子（`Q55`，Blue Velvet = Comedy+Drama+Fantasy+Psyc）不派生：前者在 in-sample 里 golden 全是"同导演"，后者 AND 后池子塌陷（`RK-5` 实测）。

### 4.3 已批准的例外：`Q12`（唯一一条净回归）

`Q12`（`和《Pulp Fiction》相似的电影`）与 `Q34`（`喜欢《Men in Black》，有类似的吗`）的**路由产物完全相同**：`{seed_title, result_constraints: {}}`，种子都是 2 个 canonical 类型，导演各有 ≥5 条目录行。但冻结标注相反：

- `Q12` 的 `result_constraints: null`，golden 相似规则 = **同导演**（Tarantino 行）→ 期望**不收窄**；
- `Q34` 的 `result_constraints: {genres:[Comedy,Crime], 1990–1999}`，golden = **共享类型×同年代** → 期望**收窄**。

不读 golden 无法区分这两条输入，因此二者不可同时满足。主 agent 裁定：采用 `R-2` 语义，**接受 `Q12` 一条净回归**（写明批准来源：B1.5e 主 agent 例外，非通用放水；`B15e-7b` 其余部分仍然有效——任何第二条 similar 回归都必须停止）。

### 4.4 记账样例（`after-G1.json` 的 `records[]`，字段已扁平化在记录顶层）

| 用例 | seed | `seed_genre_count` | `derived_constraints` | `effective_constraints` | `rows_returned`（不含 seed 行） | 派生 `find_similar_movies` 的 `candidate_pool` | 判定 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `Q34` | `BS10` Men in Black | 2 | `{genres:["Comedy","Crime"], 1990–1999}` | 同派生 | 4 | 4 | ❌→✅ |
| `Q53` | `IWo40` Face/Off | 2 | `{genres:["Action","Sci-Fi"], 1990–1999}` | 同派生 | 3 | 3 | ❌→✅ |
| `Q54` | `JCm25` True Lies | 2 | `{genres:["Action","Romance"], 1990–1999}` | 同派生 | 5 | 5 | ❌→✅ |
| `Q12` | `QTa3` Pulp Fiction | 2 | `{genres:["Action","Comedy"], 1990–1999}` | 同派生 | 5 | 9 | ✅→❌（批准例外） |
| `Q16` | `RZe5` Back to the Future | 1 | `{}` | `{genres:["Sci-Fi"]}`（文本） | 5 | 300（不收窄） | ✅ 不变 |
| `Q35` | `JlC16` Fargo | 1 | `{}` | `{}` | 5 | 300（不收窄） | ✅ 不变 |
| `Q36` | `MM33` Heat | 2（文本已给 director） | `{}` | `{director:"Michael Mann"}`（文本） | 5 | 8 | ✅ 不变 |
| `Q55` | `DLy8` Blue Velvet | 4 | `{}` | `{}` | 5 | 300（不收窄） | ✅ 不变 |
| `Q56` | `RLl15` Before Sunrise | 1 | `{}` | `{}` | 5 | 300（不收窄） | ✅ 不变 |
| `B23` | `DBo10` Trainspotting | 2 | `{genres:["Comedy","Noir"], 1990–1999}` | 同派生 | 5 | 11 | ✅（B1.5d，seen，仅记录） |
| `B26` | `BS1` The Addams Family | 2（文本已给 genres+years） | `{}` | `{genres:["Horror"], 1991–1992}`（文本） | 5 | 22（不收窄） | ✅（B1.5d，seen，仅记录） |

**量纲说明**（reviewer P2-1/P2-2）：本表与 §5.1 的行数一律是 artifact 的 `rows_returned`，即**展示出来的结果行数，不含 `execution[0]` 里那条 `displayed:false` 的 seed 命中行**；`candidate_pool` 是 `find_similar_movies` 服务端先按 `effective` 收窄后的候选行数，两者不是同一个量。

**空派生池用例**：③b2 下 in-sample 与 B1.5d 均**没有**出现"派生池为空"的用例（派生 `candidate_pool`：`Q12` 9、`Q34` 4、`Q53` 3、`Q54` 5、`B23` 11，均非 0；未派生的对照是 `candidate_pool 300` 或文本自带约束的 8/22）。`RK-5`（3+ 类型种子过窄）由"恰好 2 个类型才派生"从结构上规避，但**没有任何回退路径**：若未来出现 2 类型且派生池为空的种子，会如实失败（OD-1）。

**`A12`（空派生池 fallback 实测）本轮未执行**（计划允许的第二种处理）：自测里没有构造一条"2 类型种子且 AND 后为空"的样本，因此"不回退"的**失败路径**仍然零实测证据，只有结构依据 + 上述池大小佐证。该项已列入 §9.9 的遗留风险，交 B1.5f 的 3+ 类型种子探针。

---

## 5. in-sample `G1` 前后对照（75 条，逐条）

### 5.1 所有判定变化

| 用例 | intent | `task_success` | `routing` | `rows_returned`（前 → 后） | 失败模式（修复后） |
| --- | --- | --- | --- | --- | --- |
| `Q12` | similar_movies | ✅ → ❌ | ✅ → ✅ | 5 → 5 | `acceptable_set_miss` |
| `Q34` | similar_movies | ❌ → ✅ | ✅ → ✅ | 5 → 4 | — |
| `Q48` | filter_catalog | ❌ → ✅ | ❌ → ✅ | 0 → 4 | — |
| `Q53` | similar_movies | ❌ → ✅ | ✅ → ✅ | 5 → 3 | — |
| `Q54` | similar_movies | ❌ → ✅ | ✅ → ✅ | 5 → 5 | — |

（行数是 artifact 的 `rows_returned`，不含 `execution[0]` 里那条 `displayed:false` 的 seed 命中行；量纲说明见 §4.4。）

其余 70 条：`task_success` 与 `routing` 均不变。

### 5.2 闸门用例（`B15e-7b`）

| 用例 | 修复前 | 修复后 |
| --- | --- | --- |
| `Q12` | ✅ | ❌（**批准的例外**） |
| `Q16` | ✅ | ✅ |
| `Q35` | ✅ | ✅ |
| `Q36` | ✅ | ✅ |
| `Q55` | ✅ | ✅ |
| `Q56` | ✅ | ✅ |
| `Q57` | ❌ | ❌（`OPEN-C3` 未落码，非本任务范围） |

### 5.3 产物

- 修复前：`.pi/artifacts/b1-5e/before-G1.json`（67/75）
- 修复后：`.pi/artifacts/b1-5e/after-G1.json`（70/75）
- 动词语句守卫落地前的一轮（本轮修订的基线）：`.pi/artifacts/b1-5e/after-G1-before-titleguard.json`（70/75，与本轮逐用例一致）
- 自测修复前/后：`.pi/artifacts/b1-5e/before-self-test.json`（43 checks）/ `.pi/artifacts/b1-5e/self-test.json`（61 checks）；本轮修订前的 52 条基线另存 `.pi/artifacts/b1-5e/self-test-baseline-before-fix.json`
- B1.5d（**seen，仅记录，不作门槛**）：`.pi/artifacts/b1-5e/after-G1-blind-b1-5d.json`
- held-out B1.5c（**seen 回归，仅记录，不作门槛**）：`.pi/artifacts/b1-5e/after-G1-heldout-b1-5c.json`

---

## 6. 自测与 checker

```
$ python3 scripts/eval/tool_eval.py --self-test --out .pi/artifacts/b1-5e/self-test.json
  semantics: 61 checks, 0 problem(s)            # 修复前基线 43 → +18 条新增
  self-test independence: 47 utterance(s) vs the held-out queries, 0 problem(s)
  annotation sentinel: 75 cases executed, 0 problem(s)
  resolver pipeline: ran , 0 problem(s)
SELF-TEST OK

$ python3 scripts/eval/check_schema.py --quiet    # exit 0  (b1.5-tools/v3, schema_hash 未变, 67 词形 / 2 canonical groups / 33 unsupported)
$ python3 scripts/eval/check_golden.py --quiet    # exit 0  (snapshot 21,087 / db_snapshot_hash 未变)
$ python3 scripts/eval/check_heldout.py           # exit 0  (30 条 held-out + 别名一致)
$ python3 scripts/eval/check_blind_holdout.py     # exit 0  (B1.5d 证据未改；脚本本身零改动)
```

新增 18 个 label（43 → 61）；计划 `T-2`/`B15e-8`/AC9 写的"新增 11–12 条"是按 `A1`…`A12` 编号数，实际是 **`A1`–`A11` 全部覆盖、`A12` 未做（§4.4）**，而且 `A1`+`A2`（refuse 正向+反向）、`A5`+`A6`（`Show me`/`Find` 两类）当初合并成单条，所以 label 数与 A 编号数并不一一对应。本轮按 reviewer P1-1/P1-2 补齐后共 **18 个新增 label**（下表前 9 条为初版，后 9 条为本轮补齐/拆分），`A1`–`A11` 的覆盖关系如下：

| `A` 编号 | 断言（label） | 锁住的缺口 | 回归敏感性（已用变异实测） |
| --- | --- | --- | --- |
| `A1`+`A2` | `route: out-of-catalog request refuses without a tool` | `F-1` 正向 + `score`/`music`/`released`/`Studio Ghibli` 四条反向不误拒 | — |
| `A3` | `route: genre words inside a title are not constraints` | `F-2` 片名区段掩码（`The Family Stone` + `comedies` → 只留 `Comedy`） | — |
| `A4` | `route: seed title is the title, not the clause` | `F-2` `liked`/`enjoyed`/`watched` 三种形态 | — |
| `A5`+`A6` | `route: a sentence is not a title` | `F-2` `Show me …` 与 `Find …` 两类都返回 `None`（`H23` 类回归锁） | — |
| `A7` | `route: plural genre word` | `F-3a` `documentaries → Documentary` | — |
| `A8` | `route: a demonym is not a director` | `F-3b` 路由段：`Italian documentaries` / `Italian comedies` | 去掉 demonym skip → 2 条 fail |
| `A9` | `route: musical and music share one canonical key` | `F-4` `canonical_genres(["Musical"]) == ["Music"]` | — |
| `A10` | `similar: derived constraints come from the seed` | `F-5` DB 段：派生非空、`seed_genre_count == len(derived genres)`、**`year_from/year_to` == seed 年份整十年**、返回行满足派生约束且不含 seed | 删掉整十年派生 → fail |
| `A11` | `similar: explicit text constraints win` | `F-5` DB 段：文本自带类型+年代 → `derived == {}`、`effective == route.result_constraints` | — |
| `A12` | **（未做）** | 空派生池不回退 | §4.4/§9.9 |
| 新增 | `genre AND: Music includes Musical` | `F-4`/`R-1` **检索展开**（union），与 `canonical_genres` 折叠是两个函数 | 只查 `Music` → fail |
| 新增 | `route: a verb clause keeps its stated years` | `F-2` ④：`movies I enjoyed in the 1990s` → 无片名 + 保留 1990–1999 | 去掉守卫 → fail |
| 新增 | `route: a verb clause keeps its stated genre and years` | `F-2` ④：`I enjoyed comedy films from the 1990s` → 无片名 + `Comedy` + 1990–1999 | 去掉守卫 → fail |
| 新增 | `route: a verb before a real title keeps the title` | `F-2` ④ 反向控制：`I really liked Trainspotting` → `Trainspotting` | — |
| 新增 | `route: a Latin title glued to Chinese text` | 兜底边界：`查一下The Matrix` → `The Matrix` | 回退 `isalnum()` → fail |
| 新增 | `similar: a one-genre seed derives nothing` | `F-5` 闸门分支一（`Q16`/`Q35`/`Q56` 的保护） | 闸门从 `== 2` 改 `>= 1` → fail |
| 新增 | `similar: a three-plus genre seed derives nothing` | `F-5` 闸门分支二（`Q55`） | 同上 → fail |
| 新增 | `similar: stated genres keep the derived decade` | `F-5` **逐键覆盖**（`{**derived, **stated}`） | 改成整字典替换 → fail |
| 新增 | `recommend: a demonym phrase keeps a non-empty 1960s pool` | `B15e-5` 的 **DB 段**：demonym 不把 1960s 池变成 0 行 | 去掉 demonym skip → `rows=0` fail |

变异实测（临时改代码跑 `check_semantics`，每次只改一处，跑完即时恢复，源文件 md5 未变）：闸门 `==2`→`>=1`、删整十年派生、整字典替换、`genre_member_sql` 只查 key、删动词语句守卫、回退 `starts_word` 的 `isascii` 子句、删 demonym skip —— **七种变异都被对应用例捕获**（每项 failed 集合见本次 worker 记录）。

独立性口径（tests reviewer P2-6）：新文本全是**新写**、不含任何 B1.5d/held-out 原句，但其中 3 条是 seen/held-out 的**模板复刻**（`Show me <genre> films from <Y1> to <Y2>`、`suggest a few <plural genre> from the <decade>`、`I <verb> <Title>, anything similar?`），而 `check_self_test_independence` 只做归一化**全等**比对，因此“不是原句”不等于“模板级独立”；模板级比对列入 B1.5f（`OD-5`/计划 §8.3 已声明本任务不改该检查）。

既有断言中唯一被调整的是 `route: musical != music`：它原本断言 `extract_genres("musical") == ["Musical"]`，而 `extract_genres` 折 canonical key，`R-1` 落码后该断言在语义上不可能成立。现改为在**别名数据层**断言（`GENRE_ALIASES["musical"] == ["Musical"]`、`GENRE_ALIASES["music"] == ["Music"]`），保留 B15c-5"数据 enum 仍分开"的原意；折 canonical 的行为由新增断言 `route: musical and music share one canonical key` 锁定。标签未改名，条数不减。

---

## 7. 指纹与契约

| 项 | 值 |
| --- | --- |
| `aliases_version` | `b1-5c-aliases/v1` → **`b1-5c-aliases/v2`**（只 bump 数字，`check_schema.py:582` / `check_heldout.py:315` 的正则未改） |
| `genre-aliases.json` sha256（旧） | `f7126f022e69b2748a1a33407505bd22f62e366d2edf916e752c4ba642ecc203` |
| `genre-aliases.json` sha256（新） | **`ab8e07ddd773ad5e413eab9b6f4447e7442225f1399bb75f8feddb9870afc104`** |
| 别名规模 | 66 词形 → 22 目标 / 1 canonical group / 33 unsupported **→ 67 词形 → 22 目标 / 2 canonical group / 33 unsupported** |
| `contract_version` / `schema_hash` | `b1.5-tools/v3` / `712c0e34…474a`（**两次 `check_schema.py` 重算一致，未变**） |
| `db_snapshot_hash` | `8b16c047…f925`（未变，21,087 行） |
| `person-normalization` / `title-aliases` | 未改（`0631…dd98` / `98bf…cbe4` 同前） |

因为别名文件 SHA 变了，本报告的所有运行产物指纹**与 B1.5c/B1.5d 不可直接比较**；B1.5f 必须重新冻结基线。

---

## 8. 附：两个 seen fixture 的记录（**均仅记录，不作门槛**）

### 8.1 B1.5d（30 条）

用 `--queries-path docs/ai/golden/queries-blind.b1-5d.json --routing-path docs/ai/golden/query-routing-blind.b1-5d.json` 跑 `G1`（不是 `G1-T`，也不是门槛），仅用于逐条核对本任务点名的用例：

| | 记录的 B1.5d `G1` | 本任务后 |
| --- | --- | --- |
| `task_success` | 23/30 | **28/30** |
| 失败集合 | `B01`/`B10`/`B14`/`B22`/`B23`/`B26`/`B27` | `B01`/`B14`（**均为 B1.5d 既有的、与本任务无关的失败**） |

`B10`/`B19`/`B28`/`B29` 全部 `expected_refusal` 且行为 `refuse`（不再"落空到 clarify"），`B22`/`B23`/`B26`/`B27` 全部 ✅。**这 30 条已是 seen fixture，任何数字都不得写成门槛或泛化证据**；B1.5d 的报告/计划/fixture 未被改动。

### 8.2 held-out B1.5c（30 条，本轮新跑的一次 `G1`，seen 回归）

```bash
python3 scripts/eval/tool_eval.py --variant G1 --cases queries \
  --queries-path docs/ai/golden/queries-heldout.b1-5c.json \
  --routing-path docs/ai/golden/query-routing-heldout.b1-5c.json \
  --allow-full-run --assert-no-annotation-read \
  --out .pi/artifacts/b1-5e/after-G1-heldout-b1-5c.json
```

| 项 | 值 |
| --- | --- |
| `task_success` | **28/30** |
| `routing_success` / `slots_ok` | 28/30（差异来自 `H01`/`H16` 的 slot 比对） |
| `tools_ok` / `schema_ok` / `behavior_ok` / `grounding_ok` | 30 / 30 / 30 / 30 |
| `model_calls` | **0**（`G1` 确定路由，无模型） |
| `annotation_guard` | true |
| 失败用例 | `H01`（`帮我找《肖申克的救赎》这部电影`）、`H16`（`找一些跟《超人》类似的奇幻片，1976 到 1978 年的`）——两条都是**中文片名未过 resolver**（`G1` 不带 `G1-T` 的 resolver，`slot_mismatch` + `EMPTY_RESULT`），与本任务的五个缺口无关；held-out 的原基线是在 `G1-T` 下测的，所以这里的 28/30 **不可与 B1.5c 的 `G1-T` 数字直接比** |

这一跑的目的只有两个：①把"`H23` 在 held-out 里的判定未变"从**未测量的声明**变成实测（`H23` → `filter_movies{genres:["History"], 1998–1999}`，`task_success true`）；②给 demonym / 守卫改动一个 held-out 回归证据（`H23` 以及其余 `filter_movies`/similar 用例全部 ✅，仅 `H01`/`H16` 的中文片名 resolver 槽位失败）。**它是 seen 回归，不是泛化证据**：held-out 在 B1.5c 已作为评测集使用，本轮的规则又是看过它的。

---

## 9. 风险与未决项（如实记录）

1. **`Q12` 净回归（已批准）**：唯一一条，原因见 §4.3。若 B1.5f 出现同类"同导演、文本零约束、2 类型种子"的用例，`R-2` 与这种 golden 语义会再次冲突；B1.5f 的 `R-2` 探针必须**预声明**它按"共享类型 × 同年代"标注。
2. **1 类型种子不派生**：`Q16`/`Q35`/`Q56` 靠 `_similar` 的同导演排序通过。若 B1.5f 抽到"1 类型种子且 golden 期望类型收窄"的用例，会失败——这是 ③b2 的已知边界，不是实现缺陷。
3. **3+ 类型种子不派生**：`Q55`（4 类型）不派生。`RK-5` 的"派生池过窄"被结构规避，但**没有回退**（OD-1）；2 类型但 AND 后为空的种子会如实失败。
4. **`route: musical != music` 断言被调整**（§6 末）。计划 §4 `F-4` 原文说"不需要改该断言"，实测不成立：`extract_genres` 折 canonical key 后该断言必然失败，因此改为数据层断言 + 新增折 canonical 断言。属 R-1 落码的必要后果，已在 diff 中显式可见。
5. **demonym stoplist 可能挡住真导演名**：成员只取整词国籍/语言形容词；本轮 in-sample 75 条 + held-out 30 条（本次新跑）+ B1.5d 30 条的查询文本中没有 demonym 出现在 director 槽位，三轮 seen 回归（in-sample / held-out / B1.5d）的失败都不在 demonym 路径上（held-out 只 `H01`/`H16` 两条中文片名 resolver 失败，见 §8.2），但这是词表法的固有风险。
6. **B1.5d 与 held-out B1.5c 都已是 seen**：本轮用它们核对 `B10`/`B22`/`B23`/`B26`/`B27`/`B28`/`B29` 与 `H23`，所以两者都不能再用作任何门槛或泛化证据。
7. **文档索引已补齐（r3 回填）**：`docs/ai/README.md` 已**追加**两行——别名文件 v2 行（`b1-5c-aliases/v2`，`ab8e07dd…c104`）与**本报告索引行**（`reports/b1-5e-routing-gap-fixes.md`）；表中 B1.5c/B1.5d 的状态行**未改**。`docs/plans/` 与 issue #78 的状态已由 docs-writer 回填为“实现完成 / 待 PR”（见 §9.10 与 §11）。
8. **未实现（明确不在范围）**：通用 director-catalog 校验、其它复数类型词、`check_blind_holdout.py` 参数化、`Q57`/`OPEN-C3`、任何生产代码或 schema 变更。
9. **`A12`（空派生池回退）仍未实测**：自测与两个 seen fixture 里都没有"2 类型种子且派生池为空"的用例，所以 OD-1 的"不回退、如实失败"只有结构依据（§4.4）。B1.5f 需补一条 3+ 类型种子/空池探针。
10. ~~计划与 issue 仍是 ③b2 之前的表述~~ → **r3 已回填**：`docs/plans/b1-5e-routing-gap-fixes.md` §4 `F-5` / §5 / §6 `T-*` / §7.1（新增探针 ⑥）/ §8 AC（`B15e-7`/`7b`/`8`/`9`）/ §10 `OD-1`（③b2 细化 + 历史保留）/ §11（r3 修订记录）与 `docs/issues/issue-78-b1-5e-routing-gap-fixes.md`（`F-5`/AC 7/7b/9、实现状态）已改为**③b2 + `Q12` 唯一例外**，与本报告及实际代码一致。**远端 #78 正文仍为 r2 表述**（本文档不执行远端编辑，由主 agent 决定是否覆盖）。
11. **守卫生效范围的新窄边界**（reviewer P1 修复的副作用）：新增的 `FUNCTION_WORDS` 包含 `who`/`what`/`when` 等，因此 `I loved Who Framed Roger Rabbit` 这类以疑问词开头的真片名会在 verb 路径被当作从句（历史行为已因 `TITLE_STOPWORDS` 对兜底路径有此限制）。本轮 135 条 fixture（75 in-sample + 30 held-out + 30 B1.5d）的路由产物**逐条 diff 为零变化**，但无 fixture 覆盖这种片名。

---

## 10. 复现命令

```bash
python3 scripts/eval/tool_eval.py --variant G1 --cases both --allow-full-run \
    --assert-no-annotation-read --out .pi/artifacts/b1-5e/after-G1.json
python3 scripts/eval/tool_eval.py --self-test --out .pi/artifacts/b1-5e/self-test.json
python3 scripts/eval/check_schema.py --quiet
python3 scripts/eval/check_golden.py --quiet
python3 scripts/eval/check_heldout.py
python3 scripts/eval/check_blind_holdout.py
# B1.5d（seen，仅记录）
python3 scripts/eval/tool_eval.py --variant G1 --cases queries \
    --queries-path docs/ai/golden/queries-blind.b1-5d.json \
    --routing-path docs/ai/golden/query-routing-blind.b1-5d.json \
    --allow-full-run --assert-no-annotation-read \
    --out .pi/artifacts/b1-5e/after-G1-blind-b1-5d.json
# held-out B1.5c（seen 回归，仅记录；`G1` 而非 `G1-T`，0 model）
python3 scripts/eval/tool_eval.py --variant G1 --cases queries \
    --queries-path docs/ai/golden/queries-heldout.b1-5c.json \
    --routing-path docs/ai/golden/query-routing-heldout.b1-5c.json \
    --allow-full-run --assert-no-annotation-read \
    --out .pi/artifacts/b1-5e/after-G1-heldout-b1-5c.json
```

---

## 11. 文档回填与结论边界（r3，docs-writer 收尾）

本节只补文档，不改任何数字、代码、fixture、checker 或阈值。

| 项 | 状态 |
| --- | --- |
| `docs/plans/b1-5e-routing-gap-fixes.md` | **r3 已回填**：状态 = 实现完成 / 待 PR；§4 `F-5` = **③b2**（含 `OD-1` 原文 → ③b → ③b2 三段实测 67/75 → 69/75 → 70/75）；§5 增实测行；§6 `T-1`…`T-7` 与 §8 `B15e-7`/`7b`/`8`/`9`/`10` 写入实际数字；§7.1 新增**探针 ⑥**（同导演标注 + 2 类型种子 + 无文本约束 similar，复现 `Q12` 例外）+ similar `labeling_note` 必须写明标注约定；§10 `OD-1` 改为 ③b2 并保留历史；§11 增加 r3 修订记录 |
| `docs/issues/issue-78-b1-5e-routing-gap-fixes.md` | **已回填**：`F-5` = ③b2 + `Q12` 唯一例外，AC 7/7b/9 与实际一致，Scope 明确 B1.5f 不在本 issue，Implementation status = 实现完成 / 待 PR；**无 closing keyword**。远端 #78 正文仍是 r2 表述（本文档不执行远端编辑） |
| `docs/ai/README.md` | **已追加**两行：别名文件 v2（`b1-5c-aliases/v2`，`ab8e07dd…c104`）与本报告索引；B1.5c/B1.5d 状态行未改 |
| `docs/plans/ai-roadmap-overview.md` | **已追加** B1.5e 实现完成 / 待 PR 的状态条目（B1.5f 未做；不声称 B-①） |
| `docs/logs/2026-11-21-issue-78-b1-5e-routing-gap-fixes.md` | **已建立**（按 AGENTS 两大类：修改文件/潜在问题；新增知识点） |

**本报告结论的边界（重申，不得扩写）**：

1. 所有数字都是 **in-sample / seen 回归证据**，没有任何一个是门槛或泛化估计。
2. **B1.5f 未生成、未冻结、未运行**；仓库内没有 B1.5f fixture 或 `blind_version` 引用，B1.5e **不声称泛化 ≥90%**，也不声称 B1.5c/B1.5d/B1.5e 达标或 B-① 可开始。
3. 派生约束的收窄只存在于**评测替身**（`run_g1` + `evaluate_calls(..., effective)`）里；`find_similar_movies` 的 schema 没有 genre/year 参数，`schema_hash` 未变，**生产语义未实现**，`docs/architecture/` 本轮不改。
4. 未验证项：`A12`（空派生池失败路径）、B1.5f 的参数化与探针、`FUNCTION_WORDS` 对"以疑问词开头的真片名"的新窄边界（无 fixture 覆盖）、`Q57`/`OPEN-C3` 未落码。

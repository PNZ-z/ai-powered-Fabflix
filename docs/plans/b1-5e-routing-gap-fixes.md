# B1.5e — 修 B1.5d 暴露的路由/执行缺口 + 落码 `R-1`/`R-2`（**🟢 r3：实现完成 / 待 PR**）

> **本文是计划与实施记录（r3 = 实现完成后按实际 diff/实测回填）。** r1 经 oracle 只读评审（`.pi/artifacts/oracle-b1-5e-review.md`）提出 **3 项 P1 + 8 项 P2**，主 agent 已裁定 `OD-1`…`OD-7`；r2 回写裁定并作为实施许可交 worker。r3 只做**计划↔实现一致性回填**：`F-5`/`OD-1` 的最终形态是 **③b2（种子 canonical genres 恰好 2 才派生）+ `Q12` 唯一获批净回归**，实测数字写进本文与 `docs/ai/reports/b1-5e-routing-gap-fixes.md`。**仍然不声称泛化**：B1.5f 未生成/未冻结/未运行。
>
> | 元数据 | 值 |
> | --- | --- |
> | 文档类型 | 计划 / 方案 + 实施回填（修复 + 裁定记录 + 测试计划 + 后继盲集边界） |
> | `last_updated` | 2026-11-21（**r3：实现完成 / 待 PR**；同日于 r2 批准之后） |
> | 状态 | 🟢 **实现完成 / 待 PR**：五个缺口已修，`R-1`/`R-2` 已落码；in-sample `G1` **67/75 → 70/75**（routing 73 → **74**/75）；自测 **43 → 61** checks / 75 sentinels / 47 independence utterances / 0 problem；4 个 checker 全绿；held-out B1.5c `G1` **28/30**（失败 `H01`/`H16`，`H23` 通过）；B1.5d `G1` **28/30**（失败 `B01`/`B14`；二者均为 B1.5d 既有、与本任务无关的失败）。**唯一回归 = `Q12`（主 agent 批准）**；`Q57` 仍失败（`OPEN-C3`）｜oracle 3 项 P1 已闭合（§11），`OD-1`…`OD-7` 已裁定（§10），`OD-1` 于实施中细化为 **③b2**（§10 尾注、§11 r3） |
> | 前置事实 | B1.5d 已合并（PR #77，`main` @ `b26f35e`）：blind 30 条上 `G1-T`（冷跑）**25/30 = 83.3%** < 27/30 → **FAIL**；报告 `docs/ai/reports/b1-5d-blind-holdout.md`；计划 `docs/plans/b1-5d-blind-holdout.md` |
> | 关联 issue | **#78（已创建）** = `https://github.com/PNZ-z/ai-powered-Fabflix/issues/78`，规范记录 `docs/issues/issue-78-b1-5e-routing-gap-fixes.md`（正文 `Refs #74`、**无 closing keyword**）；**#74（OPEN，保持 OPEN）**；#76 **已 CLOSED**（B1.5d 报告完成） |
> | 前序契约 | `docs/plans/b1-5c-tool-routing-fixes.md`（主门槛 = `G1-T` ≥ 27/30 全分母）、`docs/plans/b1-5d-blind-holdout.md`（盲测纪律 `BL-1`…`BL-7`） |
> | 命名 / 证据 | 建号前沿用 B1.5 系列序号命名，建号后补 `issue-<编号>-` 前缀（**不猜编号**）。证据 = `.pi/artifacts/scout-b1-5e.md`、`.pi/artifacts/oracle-b1-5e-review.md`、B1.5d 报告 §4/§5/§8；§3 代码行号与 §4 的 fixture/别名事实由本轮直接读源码与 `docs/ai/` 数据文件核对 |
> | 本文的验收口径 | **全部 AC 都是 in-sample 回归证据，不是门槛**。本任务不产生任何"已达标/已泛化"结论；主门槛只能由 B1.5f 的一次冷跑回答，而 B1.5f **不在本任务内**（§7），且 **B1.5f 本轮未生成、未冻结、未运行** |

---

## 1. 目标与非目标

**目标（一句话）**：把 B1.5d 的 5 条失败背后的**真实缺口**修到可回归的最小程度（越界拒绝、片名区段污染、单个复数类型词、种子片名抽取、相似查询的约束派生），并按已裁定的语义把 `R-1`（`Musical`/`Music` 同族）与 `R-2`（无文本约束的相似查询）落码，使**下一套新盲集 B1.5f** 能在"规则未被这批失败调过参"的前提下重测 `G1-T` ≥ 27/30。

| 不做（非目标） | 理由 |
| --- | --- |
| **不修任何生产代码**：`src/main/**`、`pom.xml`、CI、DB 写入、迁移 | 与 B1.5a–B1.5d 同一边界；相似/推荐执行器与解析器都是**评测替身** |
| **不改契约**：`docs/ai/tool-schema.b1-5.json`、`contract_version b1.5-tools/v3`、`schema_hash 712c0e34…474a` | `find_similar_movies` 只有 `movie_id`/`same_primary_genre_only`/`limit`（本轮核对），**没有** genre/year 参数 → 派生约束只能在评测层实现，无 schema bump |
| **不改 B1.5d 的 fixture / 报告 / 计划 / 数字**；不把 B1.5d 当门槛 | B1.5d 一旦用于改规则即成为 seen fixture（`BL-5`/`BL-6`）；其 25/30 冻结在报告里 |
| **不改 B1.5c held-out / in-sample 的标注与门槛**；不改 90% 门槛、不改 `/30` 分母 | 与 `R-5`/`B15d` 同口径；`/28` 任何口径不得用来宣称达标 |
| **B1.5e 不生成、不冻结、不运行 B1.5f**（`OD-4`；P1-3） | 生成者一旦知道修复细节，盲性就只能靠"换人/换时点"（§7.1）；B1.5e 也不引用 B1.5f 的任何 fixture（`OD-5`） |
| **不加 `documentaries` 之外的一串复数**（`OD-3`；P1-2） | 每个复数都会扩大 `DIRECTOR_PATTERNS[5]` 的类型名词前瞻、把更多 `Italian <genre>` 抽成导演 → 见 §4 `F-3` |
| **不做通用 director-catalog 校验**（`OD-3`） | 语义变更（"解析出的名字不是导演就忽略"）需要独立批准；本任务只做 demonym stoplist 的最小行为修复（`F-3b`） |
| **不改 `check_blind_holdout.py`** | 它是 B1.5d 的证据脚本，改动会改变其冻结 SHA；参数化属 B1.5f（§7.3） |
| **不关闭 #74**、不声称 B1.5c/B1.5d 已达标、不声称已泛化 | 泛化只能由 B1.5f 的一次冷跑回答 |

---

## 2. 继承契约（B1.5c/B1.5d 已定，本文不重开）

| # | 契约 | 来源 |
| --- | --- | --- |
| `IC-1` | 主门槛 = **新盲集上 `G1-T` 冷跑 ≥ 27/30（90%，`/30` 全分母）**；`G1` 为 baseline；`/28` 只作敏感性 | B1.5c §4.5 / B1.5d §3.1 |
| `IC-2` | 执行半段**不得读标注**（`annotation_guard`/`--assert-no-annotation-read`）；派生约束只能用 route（文本）+ seed 行（catalog） | B1.5c §6.1 / `ANNOTATION_KEYS` |
| `IC-3` | 盲测纪律 `BL-1`…`BL-7`：先冻结后跑、每 (variant × 缓存状态) 一次、失败只归因不回头改规则 | B1.5d §2 |
| `IC-4` | 拒绝 = 不调工具（`refuse_no_tool` 只看 `not tools_called`）；澄清 = 一个问题；越界请求按 `docs/architecture/ai-user-workflows.md` **R4-1**（含"票房/获奖"）必须拒绝 | 架构文档 §R4 |
| `IC-5` | 类型多值语义 = **canonicalize 后 AND**；`slots_match` 两侧都折 canonical key（本轮核对 `tool_eval.py:167,1198,1223`） | B1.5c §5.1 |
| `IC-6` | 替身执行器 ≠ 生产；`G0`/`G2`/`G3`/MCP 未测/未实现的状态不得改写 | B1.5c §7.2 |

---

## 3. 根因表（含 scout 对 B1.5d 报告的两处纠偏）

| 用例 | 查询（B1.5d） | 真实根因（代码位置） | B1.5d 报告原措辞 |
| --- | --- | --- | --- |
| `B10` | `《The Lion King》当年的票房是多少` | `REFUSAL_RULES`（`tool_eval.py:276-283`）只有 `plot`/`transaction`/`internal`/`external` 四类，**没有票房/预算/发行公司/配乐/获奖**；`detect_refusal`（`:939`）返回 `None` → 片名被 `TITLE_PATTERNS[0]`（`:1026`）抓到 → 走检索 | 一致（真实规则缺口） |
| `B22` | `musicals from 1948 to 1950` | 别名表 `musical(s) → Musical`（`genre-aliases.json:51-52`），冻结标注是 `Music` 口径；`slots_match` 折 canonical 后 `Musical ≠ Music`（组只有 `["Sci-Fi","Science Fiction"]`） | 一致（`R-1` 探针） |
| `B23` | `I really liked Trainspotting, can you find something similar?` | **①** `TITLE_PATTERNS` 的 `(?:like\|love\|loved)\s+`（`:1033`）**不含 `liked`** → 落到兜底（`:1072-1080`），兜底用 `re.search(EN_TITLE_RUN, text)` **从整句开头**搜 → `seed_title = 'I really liked Trainspotting'` → 0 行、`find_similar_movies` 未调；**②** `R-2` 的约束派生缺失（`:1977-2000` 只传文本约束） | 报告已注明"R-2 未被干净测量"，一致 |
| `B26` | `movies like The Addams Family - horror films from 1991 to 1992` | **⚠️ 纠偏：执行器并不把种子类型放进 `WHERE`**（`_similar` `:827-877`：`where = [m.id <> seed] + constraint_sql(constraints)`，种子类型只用于 `overlap` 排序）。真实根因 = **片名区段污染**：`extract_genres` 在整句上跑，把片名里的 `Family` 抽成类型 → `Family ∧ Horror ∧ 1991-92 = 0` | 报告 §4.1 写作"把种子自身类型与文本约束一并 AND"→ **表述错误** |
| `B27` | `recommend some documentaries from the 1960s` | 别名表只有 `documentary`（`:33`），`documentaries` 缺失（本轮核对：66 个键中无 `documentaries`）；`alias_pattern`（`:242`）对拉丁词是词边界**精确**匹配 → `extract_genres = []` → `recommend_movies{year_range}` 缺 `preferred_genres` | 一致 |
| `B28`/`B29`（通过） | `Which studio released Jaws?` / `Who composed the score for Vertigo?` | **⚠️ 纠偏：不是拒绝规则命中**。`REFUSAL_RULES` 里没有 studio/composer 任何词；它们落空到 `{"action":"clarify"}`（`:1187`），而 `is_task_success` 的 `refuse_no_tool` 只要求"没调工具"（`:1368-1375`）→ **偶然通过**。`B10` 之所以失败只因它带 `《The Lion King》` | 报告未区分"拒绝"与"落空到澄清" |

**结论**：`B26` 的修法必须是"类型词不得从片名区段抽取"（或等价抑制），**只做"显式约束优先"改不动 `B26`**；`B10` 的修法必须新增拒绝类别，否则同类新查询仍会 `under_refusal`。

---

## 4. 方案（逐项最小改动）

### `F-1` 越界请求显式拒绝（`B10`）

- 文件/函数：`scripts/eval/tool_eval.py` `REFUSAL_RULES`（`:276-283`）新增 `out_of_catalog` 类；`detect_refusal`（`:939`）无需改（子串匹配已够）。
- marker（只放"目录字段绝对无法回答"的短语；`REFUSAL_RULES` 是**无词边界子串**匹配，短词必炸）：中文 `票房`/`预算`/`发行公司`/`配乐`/`作曲`/`奥斯卡`/`获奖`/`奖项`；英文 `box office`/`grossed`/`budget`/`distributor`/`which studio`/`who composed`/`composer`/`academy award`/`oscars`；**不放** `score`（与评分冲突）、`music`（与 `Music` 类型冲突）、`released`（会命中 `movies released in 1999`）、裸 `studio`（会命中 `Studio Ghibli`）。
- 优先级无需改：`detect_refusal` 已是 `route_deterministically` 第一步（`:1099-1102`），片名前抽取不会抢跑。
- **命中面（r2 已修正表述，P1-3）**：
  - 在 **105 条既有 fixture**（75 in-sample = 60 `queries.b1-5.json` + 15 `tool-cases.b1-5.json`，30 条 `queries-heldout.b1-5c.json`；外加 5 条 `negative.b1-5.json` 不计分母）上，本轮用 `grep` 逐个 marker 核对，**全部 0 命中**；
  - 在 **30 条 B1.5d blind** 上，marker 命中 **恰好** `B10`（票房）、`B19`（奥斯卡）、`B28`（which studio）、`B29`（who composed）**四条**，而四条的路由标注都是 `expected_refusal=true` → **命中是预期行为，不是回归**；
  - r1 的"105+30 零命中"表述**错误，已删除**。正确口径 = **"除 B10/B19/B28/B29 四条预期拒绝外零命中"**，且这四条必须**仍然通过**（见 `B15e-13`）。`B28`/`B29` 从"落空到 clarify"变成"真拒绝"是方向正确的收紧，`refuse_no_tool` 条件下判定不变；`B19` 同理（今天通过，改后仍通过），`B10` 由 ❌→✅。
- `negative.b1-5.json` 的 `N05`（`academy award`）行为由"澄清"变"拒绝"，但 `tool_eval.py` **不执行** `negatives`（本轮 grep 无引用），无判分影响。

### `F-2` 片名区段掩码 + 兜底守卫 + 动词白名单（`B26` + `B23` + `H23`）

- `TITLE_PATTERNS`（`:1033`）扩为 `(?:like|liked|love|loved|enjoyed|watched)\s+`（`OD-7` 裁定：三者全部纳入）。
- `extract_title`（`:1062`）兜底（`:1072-1080`）改为：**从 marker 之后**搜索 `EN_TITLE_RUN`，且加守卫——命中片段必须是“首词大写起始的连续大写词串”，且不得是 stopword/祈使动词（`Show`/`Find`/`Me`/`I`）或类型别名；否则返回 `None`。**守卫对两类触发文本都要成立**（`OD-7` 的“从句不是片名”断言）：
  - 实现约束（易错）：`EN_TITLE_RUN` 只要求**首 token** 大写、后续 token 允许小写，而兜底这一处 `re.search(EN_TITLE_RUN, text)`（`:1074`）**不带** `IGNORECASE`（`TITLE_PATTERNS` 的调用才带 `:1064`）→ 大小写守卫必须直接判定**原始文本里 run 的首 token 是否真正大写起始**，不能依赖正则 flag，也不能用折叠后的匹配字符串。
  - `Show me history films from 1998 to 1999`（`H23`）→ `None`（今天它返回整句，只因走了 `filter_movies` 才无害）；
  - `find`/`show me`/`找`/`查` 类文本同理 → `None`（两类的断言都要写，`T-6` `A5`/`A6`）。
- 掩码实现要点（**易错**）：`extract_title` 返回的是经 `TITLE_CUTS`（`:1036`）截断后的值，而 `B26` 的原始 capture 含 ` - horror films from 1991 to 1992`。因此掩码必须**按返回的 title 字符串做一次性替换**（`text.replace(title, " ", 1)`），**不能**按 `match.group(1)` 的 span 掩码——否则会把 `horror`/`1991-1992` 一起删掉、把 `B26` 从"错"改成"另一种错"。
- `route_deterministically`（`:1094`）：`extract_genres/extract_years/extract_rating` 在**掩码后文本**上跑；`extract_title`/`extract_director` 仍跑原文。
- 验收锚点（`B15e-3` 同时锁两条，oracle §2.2）：
  - `B26` 掩码后 `genres=['Horror']`、`years=1991-1992` → 显式约束生效 → 派生为空 → 22 行；
  - `H23` 掩码前后 `genres=[]`（因 `extract_title` 已返回 `None`，不触发掩码）、`task_success` 不变。

### `F-3` 复数类型词：**只加 `documentaries`** + demonym stoplist（`B27`；P1-2 / `OD-3`）

**`F-3a` 数据层（唯一新增词形）**

- `docs/ai/aliases/genre-aliases.json` 只增 `"documentaries": ["Documentary"]`，`aliases_version` 与 `F-4` 一并 bump 到 `b1-5c-aliases/v2`。
- **不加** `dramas`/`horrors`/`mysteries`/`romances`/`biographies`/`animations`/`fantasies`/`histories`/`adventures`/`crimes`/`wars`/`families`/`actions`（r1 的一串复数已删除）。原因：`EN_GENRE_NOUNS`（`:1042`）由**所有 ASCII 别名键**拼成，`DIRECTOR_PATTERNS[5]` 用它做"`Recommend some <Name> <genre>`"的类型名词前瞻；每多一个复数，就多一批 `Recommend some Italian <plural>` 被抽成 `director='Italian'` → `director_predicate` 返回 `1 = 0` → 0 行。B1.5e 只需要 `B27` 的 `documentaries`，其余复数**另立任务**。
- 不选"代码层复数回退（`ies→y`/去尾 `s`）"：引入隐性词表，违反"词汇只在数据文件"取向。

**`F-3b` demonym stoplist（最小行为修复；`OD-3` 裁定）**

- 现象：`Recommend some Italian documentaries from the 1960s` → `director='Italian'` → 0 行；同一缺陷**今天就存在**于 `comedies`（`Recommend some Italian comedies from the 1960s` 实测 0 行，scout §4），`F-3a` 只是把它复制到 `documentaries`。
- 修法（最小）：新增路由常量 `DEMONYM_STOPLIST`（与 `TITLE_CUTS` 同层的路由器本地停用表），成员为常见**国籍/语言形容词**：`italian`/`french`/`japanese`/`chinese`/`korean`/`german`/`british`/`english`/`american`/`indian`/`spanish`/`mexican`/`canadian`/`australian`/`russian`/`soviet`/`swedish`/`danish`/`norwegian`/`polish`/`brazilian`/`argentine`/`iranian`/`israeli`/`turkish`/`greek`/`egyptian`/`thai`/`african`/`european`。`extract_director`（`:1083`）匹配到值后，若 `normalized(value)` 命中该表则**跳过该匹配继续尝试后续 pattern**，全部命中则返回 `None`。
- 放在代码而非 `genre-aliases.json`：该文件的键集被 `check_schema.py:588-654` 的 `unsupported = enum − targets − variants − keys` 算术严格约束，塞入非类型词会污染 `unsupported` 校验；demonym 属路由器停用词（与既有 stopword/祈使动词处理同类），不属类型词汇表。此选择记入报告，供 reviewer 判定是否算 scope creep。
- **不做**通用 director-catalog 校验（`run_g1` 里"`matching_directors() == []` 就丢槽"）——它是语义变更，另立任务（原 r1 `F-6`，见 §4 `F-6`）。
- 本轮 `grep` 事实：105 条既有 fixture + 30 条 blind 的查询文本中，demonym 只出现在片名 `American Psycho`（B1.5d `B11`）与若干 `labeling_note` 里，**没有任何用例把 demonym 放在 director 槽位** → stoplist 预期不改任何既有用例判定（`T-3` 仍要逐条核实）。
- 负例断言（`A8`）：`extract_director("Recommend some Italian documentaries from the 1960s") is None` 且 `extract_genres(...) == ["Documentary"]`；附带同表断言 `extract_director("Recommend some Italian comedies from the 1960s") is None`（零额外代码，纯覆盖既有缺陷）。

### `F-4` `R-1`：canonical group `["Music","Musical"]`（裁定 `OD-2`）

- `docs/ai/aliases/genre-aliases.json`：`canonical_groups` 增 `["Music","Musical"]` + `notes` 说明"用户词 `musical`/`music` 同族；**数据 enum 仍分开**（139 / 619）"，`aliases_version` = **`b1-5c-aliases/v2`**（只 bump 数字，前缀不动）。
  - 理由：`check_schema.py:582` 与 `check_heldout.py:315` 硬编码 `^b1-5c-aliases/v[0-9]+$`；保持前缀 = 零额外改动，**不改任何 checker 正则**。
  - 运行指纹会变：旧 SHA **`f7126f022e69b2748a1a33407505bd22f62e366d2edf916e752c4ba642ecc203`**（`b1-5c-aliases/v1`，本轮实测核对）→ 新 SHA 由 worker 实现后计算并写入报告；`docs/ai/README.md:25` 的别名行需**追加**索引（不改既有行）。
- 机制已就位，**不需要**改别名目标、`MUSICAL_SPLIT`（`check_schema.py:572,646`）或 `tool_eval.py:2350` 的 `musical != music` 断言：`slots_match` 两侧折 canonical（`:1198`，折 canonical 在 `:1223`），`_filter`/`_recommend`/`_similar` 都走 `canonical_genres`（`:167`），`genre_member_sql`/`genre_and_sql`（`:192,198`）已按 canonical key 展开成员并计数。
- **`B22` 的 25 行 / top-5 必须写成可复现断言（P2-3）**，不能只写"判定不变"：
  - SQL（只读快照 21,087）：`1948–1950 ∧ genre ∈ {Music, Musical}` = **25** 行；`1948–1950 ∧ genre = Music` = 24；`∧ genre = Musical` = 1（多出的行 = `ChW4`）。
  - `_filter` 是 `ORDER BY m.title, m.year LIMIT`（函数起于 `:794`，排序在 `:812`）；实测 **top-5 全部是 `Music` 行**，`ChW4` 不在 top-5 → 冻结 `acceptable_movie_ids`（24 行）∩ top-5 **非空** → `acceptable_hit = true`。报告必须写出这条 SQL、top-5 的 title/year 列表与 `acceptable_hit` 结论。
- 影响面（scout 只读 SQL，快照 21,087）：`B22` 24 → **25** 行（`acceptable_hit` 不变）、`Q48` 仍 **4** 行、`B05` 仍 **5** 行 → 四个受影响用例判定全部不变（worker 实测复核）。
- **语义必须写清（P2-7）**：这是**召回扩大，不是等价替换**——任一 `filter_movies{genres:["Music"]}` 现在也返回 `Musical` 行（619 + 139）；`音乐` 仍指 `Music`，同组后同样展开。报告须写明"not an equivalence, a recall expansion"，并说明 **B1.5f 的 `R-1` 探针按 union 标注**（不能用 B1.5d 的 24 行口径，否则 B1.5f 会把正确行为判成错）。
- `check_schema.py` 增一条"`Music`/`Musical` 同组"显式断言（与现有 Sci-Fi 组断言并列）；`tool_eval.py` 自测增 `canonical_genres(["Musical"]) == ["Music"]`（`A9`）。
- **不改** `Q48`/`B22` 的冻结标注（`R-1` 的语义由"别名族"承载，不改 golden 口径）。

### `F-5` `R-2`：派生约束 + 显式优先（裁定 `OD-1`）

- 位置：`run_g1` 的 seed-follow 块（`:1977-2000`），**不**改 `_similar`（`:827`）的"按给定约束收窄"语义（自测 `similar: constraints narrow before ranking` 依赖它）。
- 规则：seed 选定后
  - **派生闸门（最终形态 = ③b2，`OD-1` 实施中细化）**：只当 `stated` **没有 `director` 槽**且 `len(canonical_genres(seed["genres"])) == 2` **恰好 2 个**类型时才派生。1 个类型（`Q16`/`Q35`/`Q56`）与 3+ 类型（`Q55`）**一律不派生**，文本给了 `director` 也不派生。
    - **裁定链**：`OD-1` 原文="派生全部 canonical 类型、不裁剪"实测 67/75（回归 `Q12`/`Q36`/`Q55`/`Q56`，越过零回归闸门）→ ③b="≤2 类型才派生" 实测 69/75（回归 `Q12`/`Q56`）→ **③b2="恰好 2 类型才派生 + 有 director 槽不派生" 实测 70/75，回归仅 `Q12`（主 agent 批准）**。三段实测见报告 §4.2。
  - 文本无 `genres` → `derived["genres"] = canonical_genres(seed["genres"])`（闸门通过时取种子的全部 canonical 类型；闸门不通过则空）
  - 文本无 `year_from`/`year_to` → `derived["year_from"] = y - y%10`、`derived["year_to"] = 十位+9`（种子**整十年**）
  - `effective = {**derived, **constraints}`（显式键**逐键**覆盖派生键）
  - `narrow_seed_candidates`（`:1908`）仍只用**文本**约束（消歧语义不变）
  - **派生池为空不回退**（`OD-1` 未变）：不降级为"只排序不收窄"，如实失败；`RK-5` 的空池风险由报告 + 断言显式记录，不得静默放宽。
- 记账（P2-1）：`extra["derived_constraints"]`、`extra["effective_constraints"]`、`extra["seed_genre_count"]`；`route` 原样保留 → 可审计"哪些键来自文本、哪些来自种子、种子有几个类型"。
- `IC-2` 满足：只用 route（文本）+ seed 行（catalog），不读 `ANNOTATION_KEYS`。
- 落点只影响 follow-up 调用（`evaluate_calls([follow], …, effective)`），首个 `search_movies_by_title` 不受影响。
- **零回归是硬验收（P1-1，`B15e-7b`）**：F-5 前后，B1.5c in-sample 中当前 `task_success=true` 的 **similar** 用例必须**零回归**——具体是 `Q12`/`Q16`/`Q35`/`Q36`/`Q55`/`Q56`（本轮读 `query-routing.b1-5.json` 核对：`Q12`/`Q35`/`Q56` 无 `result_constraints`；`Q16` 只有 `{genres:["Sci-Fi"]}`；`Q36` 只有 `{director:"M.Mann"}`；`Q55` 只有 `{director:"D.Lynch"}`）。若任一条由 ✅ 变 ❌：**先调整派生规则**（例如只在"无任何文本约束"时派生、或只在缺 `genres` 时派生而年代不派生），重测后仍回归则**停止并交主 agent**，不得把回归写进报告后继续。默认零回归；若主 agent 明确批准"有证据的净回归"，必须在报告里点名用例、原因与批准来源。
- **实际结果（r3 回填）**：`Q16`/`Q35`/`Q36`/`Q55`/`Q56` 全部保持 ✅；**唯一净回归 = `Q12`，已由主 agent 批准**（§4.3 下方、报告 §4.3）。批准依据 = `Q12` 与 `Q34` 的**路由产物逐字节相同**（`{seed_title, result_constraints:{}}`，种子都是 2 个 canonical 类型），而冻结标注相反（`Q12` = 同导演、`Q34` = 类型×同年代）→ 不读 golden 不可区分，因此这是**标注冲突**而非代码缺陷；采用 `R-2` 语义即接受该条失败。**`B15e-7b` 的其余部分仍有效**：任何**第二条** similar 回归都必须停止交主 agent，不能援引此项。
- **`Q12` 例外的 B1.5f 后果**：B1.5f 必须用一条**预声明**探针（"同导演标注 + 2 类型种子 + 无文本约束的 similar"）复现该形态，且所有 similar 的 `labeling_note` 必须写明标注约定（§7.1 ⑥）——否则该例外在下一代盲集里仍不可判定。
- **契约含义（必须写进报告，不得含糊；`OD-6`）**：`find_similar_movies` 的 schema 没有 genre/year 参数，所以"派生收窄"在**生产语义**上只能是服务端默认行为；B1.5e 在评测替身里实现它，**不**代表生产已实现，也不 bump `schema_hash`。架构文档（`docs/architecture/`）**本次不改**，等 B1.5f 结果后再补。

### `F-6` ~~director-catalog 校验~~ → **被否决项（记录用，不实现）**

- 原 r1 的"`run_g1` 里 `matching_directors(value) == []` 就丢弃 `director` 槽"是**通用**语义变更（"解析出的名字不是导演时忽略"），`OD-3` 裁定**不做**。
- 本任务改为 `F-3b` 的最小 demonym stoplist：只挡"国籍/语言形容词被当成导演名"这一类，不动其它名字解析语义。
- 通用 director-catalog 校验另立任务（若 B1.5f 暴露同类新失败再评估）。

### `F-7` 测试与报告落点

- 见 §6（测试）与 §7.2（报告内容）。改动集中在 `scripts/eval/tool_eval.py`、`docs/ai/aliases/genre-aliases.json`、`scripts/eval/check_schema.py`（组断言）+ 新报告 + `docs/ai/README.md` 追加行；**不新增脚本**（B1.5f 的 checker 属 §7.3）。
- 报告 `docs/ai/reports/b1-5e-routing-gap-fixes.md` 必须包含（P2 汇总）：① 五个缺口与逐项改动；② `R-1`/`R-2` 裁定与**实测**波及面；③ in-sample 前后对照（逐条，含 `Q12`/`Q16`/`Q35`/`Q36`/`Q55`/`Q56` 的前后状态）；④ 自测/checker 输出；⑤ `aliases_version = b1-5c-aliases/v2` 与**旧 SHA `f7126f02…c203` → 新 SHA**；⑥ `B22` 的可复现 SQL + top-5 + `acceptable_hit`；⑦ `derived_constraints`/`effective_constraints`/`seed_genre_count` 的样例，**空池用例单列**；⑧ "`R-1` 是召回扩大不是等价替换"；⑨ "B1.5e 不生成/不冻结/不运行 B1.5f，泛化未测"。

---

## 5. `R-1` / `R-2` 的语义与影响（已裁定）

| 项 | `R-1` = `["Music","Musical"]` 同族 | `R-2` = 派生约束 + 显式优先 |
| --- | --- | --- |
| 语义 | 用户说 `musical`/`music` 都指这一族；**数据 enum 仍分开**，检索展开两值。**不是等价替换：召回扩大**（619 + 139） | **③b2**：文本没说类型/年代**且种子 canonical genres 恰好 2 个**（且无 director 槽）时，用种子自身的 canonical 类型 + 整十年补全；文本说了的键**逐键不被覆盖**；派生池空则如实失败，不回退 |
| 裁定来源 | 主 agent `OD-2`：group + `b1-5c-aliases/v2`；不改 `Q48`/`B22` 标注 | 主 agent `OD-1`（不回退、不按 genres ≤2 裁剪）→ **实施中细化为 ③b2**（恰好 2 类型才派生）+ `Q12` 唯一获批净回归；记账保留 `seed_genre_count` |
| 影响面 | 任一 `filter_movies{genres:["Music"]}` 会返回 `Music ∪ Musical` → 召回扩大 | 所有 similar 查询在"文本缺键"时收窄；`Q12/Q35/Q56`（无 `result_constraints`）、`Q16`（只有 genre）、`Q36/Q55`（只有 director）、`Q57`（只 director，已失败）行为都会变（本轮读 fixture 得到，非运行结果） |
| 已验证不变（scout SQL） | `B22`（24→25，top-5 全 `Music`，`acceptable_hit` 仍 true）、`Q48`（4 行）、`B05`（5 行） | `B23`（`Comedy∧Noir ∧ 1990-99` = 12 − seed = 11）、`Q34/Q53/Q54`（scout 只读 SQL 逐条复现 golden 行数） |
| 实测结果（r3） | `B22`/`Q48`/`B05` 判定均不变（`Q48` ❌→✅ 因 `R-1` 召回扩大）；`aliases SHA` 由 `f7126f02…c203` → `ab8e07dd…c104` | `Q34/Q53/Q54` ❌→✅；`Q16/Q35/Q36/Q55/Q56` ✅ 不变；**`Q12` ✅→❌（主 agent 批准的唯一条）**；`B23` ✅（seen，仅记录） |
| 未验证（必须 worker 实测） | `Music` 同组后中文别名 `音乐` 的连带展开；`aliases_version` bump 后两个 checker 的实际输出 | `Q12/Q16/Q35/Q36/Q55/Q56` 的真实判定（可能变差 → `B15e-7b` 零回归闸门）；种子 `genres ≥3` 的空池风险（`RK-5`） |
| 证据强度（P2-6） | `Q48`/`B22`/`B05` 是 **seen in-sample**，只能算"回归修复"，**不能当泛化证据**；`Q34/Q53/Q54` 同理 | 同上：`Q34/Q53/Q54` 变绿只是回归修复，不是泛化 |
| 不变式 | **不改** `Q48`/`B22` 标注、不改 `golden_version`/`blind_version`；不改任何 checker 正则 | **不改** `_similar` 语义、不读标注、不 bump 契约 |
| 后继（B1.5f） | B1.5f 的 `R-1` 探针**按 union 标注**（25 行口径），不沿用 B1.5d 的 24 行 | B1.5f 的 `R-2` 探针需覆盖"种子 3+ 类型"形态，检验是否会过窄 |

---

## 6. 测试计划（B1.5e 只做 in-sample 回归，**不作门槛**）

| # | 内容 | 期望 | 备注 |
| --- | --- | --- | --- |
| `T-1` | `--self-test`：既有自测断言全绿、**条数不减**（实测基线 = **43** 条 semantics checks；75 条 annotation sentinel；independence **24** utterances；本轮已实跑并记录 `.pi/artifacts/b1-5e/before-self-test.json`） | 基线不减（**实测 43 → 61，0 problem**） | 需要 DB，**不**用 `--skip-db` |
| `T-2` | 新增自测断言（`check_semantics` + `SELF_TEST_SAMPLES`/DB 段）。**计划原写 "11–12 条"（按 `A1`…`A12` 编号数），实际落为 18 个新增 label**（`A1`+`A2`、`A5`+`A6` 各自合并成单条，`A12` 未做） | 全绿（**实测 61 checks / 0 problem**，独立 47 utterances） | 见 §6.1；label 与 `A` 编号不是一一对应，报告 §6 给了对照表 |
| `T-3` | 全量 in-sample `G1`（75）对照 | 修复前后并列，**逐条**报变化（**实测 67/75 → 70/75**，routing 73 → 74/75） | 必须逐条列出 `Q12`/`Q16`/`Q35`/`Q36`/`Q55`/`Q56` 的前后 `task_success`（不能只写"已披露"）；**实测**：`Q34/Q48/Q53/Q54` ❌→✅、**`Q12` ✅→❌（唯一，已批准）**、`Q16/Q35/Q36/Q55/Q56` 全部 ✅ 不变、`Q57` 仍失败（`OPEN-C3` 未落码） |
| `T-4` | `check_schema.py --quiet`、`check_golden.py --quiet`、`check_heldout.py`（+ `check_blind_holdout.py`） | 全绿 | 别名表改动后**必跑**；`aliases_version` 保持 `b1-5c-aliases/vN` 前缀，两个硬编码正则**不改**；**实测四个 checker 全部 exit 0**，B1.5d fixture/checker SHA 未变 |
| `T-5` | B1.5c held-out `G1` + `G1-T` 回归 | 只记"回归通过/变化"，**不作门槛** | **实测**：held-out `G1` **28/30**（失败 `H01`/`H16`，均为中文片名 resolver 槽位；`G1` 无 resolver），`H23` ✅ 且判定不变；held-out 已被污染（`R-8`），数字不得用于任何达标声明 |
| `T-6` | 语义断言（§6.1 的 `A1`…`A12`，DB 项放 DB 段） | 全绿 | 文本**新写**，不含 B1.5d/held-out 原句；但其中 3 条是 seen/held-out 的**模板复刻**（`Show me <genre> films from <Y1> to <Y2>`、`suggest a few <plural> from the <decade>`、`I <verb> <Title>, anything similar?`），独立性检查只做归一化全等——模板级比对列入 B1.5f（`OD-5`） |
| `T-7` | 记录性核对（不是新门槛）：demonym 在 fixture 中的出现面、marker 命中面 | 与 §4 `F-1`/`F-3b` 的表述一致 | grep 核对已完成；**实测**：135 条 fixture（75 in-sample + 30 held-out + 30 B1.5d）的路由产物逐条 diff 为零变化 |

### 6.1 新增断言清单（`T-2`/`T-6`）

- **路由层（`check_semantics`，无需 DB）**：
  - `A1` `route: out-of-catalog request refuses without a tool`（`action == "refuse"` 且 `tool_calls == []`；新写的票房/获奖类文本）
  - `A2` `route: out-of-catalog markers do not fire on in-scope words`（负例：`score` / `music` / `released in 1999` / `Studio Ghibli` 类文本**不得**被拒）
  - `A3` `route: genre words inside a title are not constraints`（新写的 similar 文本 → `extract_genres == ["Horror"]` 一类）
  - `A4` `route: seed title is the title, not the clause`（`liked`/`enjoyed`/`watched` 形态的新文本 → title = 片名）
  - `A5` `route: a sentence is not a title (Show me …)`（`extract_title(...) is None`）
  - `A6` `route: a sentence is not a title (find …)`（`extract_title(...) is None`）
  - `A7` `route: plural genre word`（`…documentaries…` → `["Documentary"]`）
  - `A8` `route: a demonym is not a director`（`Recommend some Italian documentaries from the 1960s` → `director is None`、`genres == ["Documentary"]`；附 `Italian comedies` 同表断言）
  - `A9` `route: musical and music share one canonical key`（`canonical_genres(["Musical"]) == ["Music"]`）
- **DB 段**：
  - `A10` `similar: derived constraints come from the seed`（`extra["derived_constraints"]["genres"]` 非空、`extra["seed_genre_count"]` 与 seed 行一致，且返回行满足种子类型+年代）
  - `A11` `similar: explicit text constraints win`（文本自带类型+年代时 `derived_constraints == {}`、`effective_constraints == constraints`）
  - `A12`（若能在快照上稳定构造空派生池）`similar: an empty derived pool is recorded, not silently widened`；构造不出则**不加断言**，改为报告单列该风险与一次实测记录（`RK-5`）

---

## 7. B1.5f 验证计划（新盲集，**B1.5e 之后单独执行**）

### 7.1 门槛与纪律（`OD-4` 裁定）

- 新 30 条（`C01`–`C30` 或等价新前缀、新 `blind_version`），**B1.5e 合并之后**由**新 worker / 新会话**生成、冻结（三个 SHA）、**只跑一次**；主门槛仍 **`G1-T` 冷跑 ≥ 27/30**（`IC-1`）。
- **B1.5e 不生成、不冻结、不运行 B1.5f**，也不引用 B1.5f 的任何 fixture / 路径 / `blind_version`（`OD-5`）；B1.5e 的代码与文档里**不得**出现指向不存在的 B1.5f 文件的引用。
- 生成者不得读 B1.5e 的修复细节（§4/§5）与运行产物；冻结前禁止对新 fixture 跑任何变体（`BL-1`）。
- **B1.5e 不跑 B1.5d 作为门槛**；B1.5d 的 25/30 只作为历史基线引用，且必须注明"该集已 seen"。
- 配额/覆盖沿用 B1.5d §3.2–§3.3，并**新增**本轮缺口的覆盖项：① 越界请求 ≥2（含中文 marker 1 条）；② 类型词出现在片名里的 similar 1 条；③ 复数类型词 1 条；④ 无文本约束的 similar 1 条（`R-2`）、含 `musicals` 的 filter 1 条（`R-1`，**按 union 标注**）；⑤ 种子含 3+ 类型的 similar 1 条（检验 `R-2` 是否过窄）；⑥ **同导演标注 + 2 类型种子 + 无文本约束的 similar 1 条（复现 `Q12` 例外）**。
- **⑥ 的理由与写法（r3 新增；必需，不可省）**：`Q12` 与 `Q34` 的**路由产物逐字节相同**（`{seed_title, result_constraints:{}}`，种子都是 2 个 canonical 类型），但冻结标注相反（同导演 vs 类型×同年代）→ 仅凭文本不可判定。因此 B1.5f 必须**预声明**一条“同导演标注 + 2 类型种子 + 零文本约束”的 similar 用例（预期保持失败；它是 `R-2` 与同导演标注冲突的**已知不可满足**形态的回归探针，不是新发现的缺陷），**并把每一条 similar 用例的 `labeling_note` 写明其约定**：`same director` / `shared genre × window` / 混合（如 `Q16`）。不写约定的 similar 标注在 B1.5f **不得冻结**（否则下一代盲集会重现同一种不可判定）。

### 7.2 本轮不声称

B1.5e 完成后**只能**说"B1.5d 点名的 5 条失败已被针对性修复，并通过 in-sample/回归断言（in-sample `G1` 67/75 → 70/75）"，**不能**说"泛化 ≥90%"、"B1.5c/B1.5d/B1.5e 已达标"或"B-① 可开始"。**实际状态（r3）**：B1.5f **未生成、未冻结、未运行**，仓库内无 B1.5f fixture/`blind_version` 引用（`grep -r b1-5f` 仅命中计划/issue 的边界文字）。

### 7.3 依赖项（B1.5f 自有范围，**B1.5e 不改**）

- `scripts/eval/check_blind_holdout.py` 硬编码 `BLIND_VERSION/ROUTING_VERSION`、B1.5d 路径，并含 `R-1` 探针"必须含 `musicals`"的断言（`:96-97,402-410,614-630`）→ B1.5f 需**参数化或新脚本**；且该探针的期望行数要改为 **union 口径（25 行）**。
- `check_self_test_independence`（`tool_eval.py:1521`）目前只比对 held-out 文本；是否把 B1.5f 路径纳入由 B1.5f 落地（`OD-5`）。

---

## 8. 验收条件（**全部为 in-sample 回归证据，不作门槛**）

| ID | 条件 | 验证方式 |
| --- | --- | --- |
| `B15e-1` | 5 条失败逐条有对应修复点，且**只**改 `scripts/eval/tool_eval.py` + `docs/ai/aliases/genre-aliases.json` + `check_schema.py` 的组断言（+ 报告 + `docs/ai/README.md` 追加行） | diff 清单（**实测**：`git diff --stat` = `docs/ai/README.md`、`docs/ai/aliases/genre-aliases.json`、`scripts/eval/check_schema.py`、`scripts/eval/tool_eval.py` + 新增报告/计划/issue/日志） |
| `B15e-2` | `git diff --stat` 证明 `src/main/**`、`pom.xml`、CI、`docs/ai/tool-schema.b1-5.json`、`schema_hash`、B1.5a–B1.5d 的 fixture/报告、`check_blind_holdout.py` **零改动** | diff + hash（**实测**：`schema_hash 712c0e34…474a` 与 `db_snapshot_hash` 未变；B1.5d fixture/checker SHA 未变） |
| `B15e-3` | **同时**断言两条：`B26` 掩码后 `genres == ["Horror"]`、`years == 1991–1992`；`H23` 的 `extract_title` 返回 `None` 且任务判定不变 | 断言 `A3`/`A5` + 回归记录（**实测**：两条断言均绿；`H23` 在 held-out `G1` 下仍 `filter_movies{genres:["History"], 1998–1999}`、`task_success true`） |
| `B15e-4` | `B10` 类请求 `action == "refuse"` 且不调工具；`score`/`music`/`released`/`studio` 的反向断言成立（不误拒） | 断言 `A1`/`A2`（**实测**：正/反向同条内联全绿；`B19`/`B28`/`B29` 仍 ✅，`B10` ❌→✅） |
| `B15e-5` | **行为写清**：`documentaries` → `["Documentary"]`；`Recommend some Italian documentaries from the 1960s` → `genres == ["Documentary"]`、`director is None`（**不产生** director 槽）、DB 段不因 `1=0` 变 0 行；`Italian comedies` 同表 | 断言 `A7`/`A8` + DB 段记录 |
| `B15e-6` | `R-1` 落码后 `B22`/`Q48`/`B05` 判定不变，且报告含**可复现 SQL**：`1948–1950 ∧ (Music∨Musical) = 25`、top-5 全 `Music`、`ChW4` 不在 top-5、`acceptable_hit = true`；`aliases_version = b1-5c-aliases/v2`；`check_schema.py` 有 `Music`/`Musical` 同组断言；旧 SHA `f7126f02…c203` → 新 SHA 已记录 | SQL + 回归 + `check_schema.py` 输出（**实测**：新 SHA = `ab8e07dd…c104`；`Q48` 4 行 ❌→✅、`B22`/`B05` 判定不变；`check_schema.py --quiet` exit 0） |
| `B15e-7` | `R-2` 落码（**③b2**：仅当种子 canonical genres **恰好 2** 且无 director 槽时派生 genres + 整十年；显式键逐键覆盖；空池不回退）后 `B23` 与 `Q34/Q53/Q54` 通过；`derived_constraints`/`effective_constraints`/`seed_genre_count` 出现在记录里；`annotation_guard` 全 true；派生池空**不回退** | 回归记录（**实测**：`Q34/Q53/Q54` ❌→✅；`B23` ✅ 但属 seen，仅记录；`A12` 空池回退**未实测**，见报告 §4.4/§9.9） |
| `B15e-7b` | **零回归闸门（r3 = ③b2 + `Q12` 唯一例外）**：F-5 前后，B1.5c in-sample 中当前 `task_success=true` 的 similar 用例（`Q12`/`Q16`/`Q35`/`Q36`/`Q55`/`Q56`）中，**除主 agent 明文批准的 `Q12` 一条外全部零回归**；任何**第二条** similar 回归 → 调整派生规则并重测，仍回归则停止交主 agent。`Q12` 的批准依据 = 与 `Q34` 路由产物逐字节相同而标注相反（不读 golden 不可区分） | 前后对照表（逐条 ✅/❌；**实测** `Q16`/`Q35`/`Q36`/`Q55`/`Q56` ✅ 不变，`Q12` ✅→❌ 批准例外） |
| `B15e-8` | 既有自测断言全绿且条数不减（基线 **43** semantics checks / 75 sentinels / 24 independence），新增断言全绿（**`A1`…`A11` 覆盖、`A12` 未做；实际 18 个新增 label，43 → 61 checks**；`A1`+`A2`、`A5`+`A6` 合并） | `--self-test` 输出（修复前/后各一份；**实测 61 checks / 0 problem / independence 47**） |
| `B15e-9` | in-sample `G1` 前后并列，逐条列出所有判定变化（含 `Q12/Q16/Q35/Q36/Q55/Q56` 的前后状态），不隐藏回归 | 前后对照表（**实测 67/75 → 70/75**，routing 73 → 74/75） |
| `B15e-10` | B1.5c held-out 回归只作"回归"记录，不写任何门槛结论 | 报告措辞检查（**实测 held-out `G1` 28/30**，失败 `H01`/`H16`；`H23` ✅） |
| `B15e-11` | B1.5d fixture/报告/计划与 #74 状态未被改动；issue 正文 `Refs #74`、无 closing keyword | diff + issue 文档（**实测**：B1.5a–B1.5d 文件零改动；本 issue 文档无 closing keyword） |
| `B15e-12` | 报告如实声明"B1.5e 不声称泛化；泛化待 B1.5f 一次冷跑"，并写明"`R-1` 不是等价替换，是召回扩大" | 报告 §结论（报告头部 + §11 “结论边界”） |
| `B15e-13` | `F-1` 落地后 **`B19`/`B28`/`B29` 仍通过**（`expected_refusal=true` 三条都过 `refuse_no_tool`），`B10` 由 ❌→✅；marker 在 105 条既有 fixture 上零命中，B1.5d 上仅命中这四条 | 回归表（B1.5d 只作 seen fixture 记录，不作门槛）+ grep 记录（**实测**：B1.5d `G1` 28/30，`B10`/`B19`/`B22`/`B23`/`B26`/`B27`/`B28`/`B29` 全部 ✅；失败仅 `B01`/`B14`） |
| `B15e-14` | **B1.5e 边界**：不生成、不冻结、不运行 B1.5f；不改 `check_blind_holdout.py`；仓库内无指向 B1.5f fixture 的引用 | `git diff --stat` + `grep -r "b1-5f"`（**实测**：仅命中计划/issue 的边界文字，无 fixture 路径或 `blind_version`） |
| `B15e-15` | 所有报告的验收结论措辞都是"in-sample 回归"，未出现"B1.5c/B1.5d/B1.5e 已达标" | 报告措辞检查（报告标题即写明"in-sample 回归报告，不是门槛"） |

---

## 9. 风险与回退

| # | 风险 | 影响 | 缓解 |
| --- | --- | --- | --- |
| `RK-1` | 拒绝 marker 子串误伤（中文短词、英文短语边界） | 误拒正常筛选/片名查询 | marker 只取短语；`T-1`/`T-4` 全量回归；已 grep 105 条 fixture 零命中（§4 `F-1`） |
| `RK-2` | 掩码把真片名里的类型词一起删掉 → 该查询失去类型约束 | `B26` 型查询反向变差 | 掩码只按**返回的 title 字符串**替换；`T-3` 逐条报变化；断言 `A3` |
| `RK-3` | `H23` 回归（兜底给出的伪片名被当片名掩码，吞掉 genres/years） | held-out 1 条 + 同类形态 | 先修兜底守卫再加掩码（顺序不可颠倒）；`A5`/`A6` 上锁；`B15e-3` 双断言 |
| `RK-4` | `R-1` 召回扩大（`Music` 请求返回 `Musical` 行） | 语义/期望集合变化 | **已裁定接受**（`OD-2`）；报告写"召回扩大非等价"；`B22` 用可复现 SQL + top-5 证明判定不变；B1.5f 探针按 union 标注 |
| `RK-5` | 派生约束在种子 `genres ≥3` 或窄年代时 0 行 | 新失败（`R-2` 从"排序太宽"变"过窄"） | `OD-1` 裁定**不回退**；③b2 用"恰好 2 个类型才派生"从结构上避开 3+ 类型种子（`Q55`/4 类型）；`seed_genre_count` 记账；报告**单列**空池用例（实测无空池用例，但**回退路径零实测证据**，`A12` 未做）；B1.5f 配额加"种子 3+ 类型"1 条 |
| `RK-6` | 逐键派生改变 in-sample `Q12/Q16/Q35/Q36/Q55/Q56` 的判定（可能变差） | in-sample 数字波动 | **已发生并已处置**：`OD-1` 原文 67/75（回归 `Q12`/`Q36`/`Q55`/`Q56`）→ ③b 69/75（回归 `Q12`/`Q56`）→ **③b2 70/75（仅 `Q12`）**；`Q12` 由主 agent 批准为唯一例外（§4 `F-5`），其余五条保持 ✅；`T-3`/`B15e-9` 已逐条披露 |
| `RK-7` | B1.5d 污染：用它调参后重跑同一集 | 盲性作废 | 只在 B1.5f 上跑门槛；B1.5d 数字冻结；`B15e-13` 明确"seen fixture，不作门槛" |
| `RK-8` | 别名文件 SHA 变化 → 运行指纹与 B1.5c/B1.5d 不可比 | 报告可比性 | 报告记录旧 SHA `f7126f02…c203` → 新 SHA；`docs/ai/README.md` 追加索引行；B1.5f 重新冻结基线 |
| `RK-9` | demonym stoplist 误挡真导演名（例如姓 `French`/`Young` 之类） | 少抽一个 director 槽 | 成员只取国籍/语言形容词全词；`A8` 之外再跑 `T-1`/`T-3`；若既有用例受影响即视为回归并停下报告 |
| `RK-10` | `F-3a` 被"顺手"扩成一串复数 | 引入一串 `Italian <plural>` 0 行 | `B15e-5` 只认 `documentaries`；diff 里出现其它复数即违反范围 |

**回退方案**：每个 `F-*` 都是独立小改动，任一 `RK-*` 触发时可单独 revert（`F-4`/`F-5` 是语义变更，回退 = 恢复 `b1-5c-aliases/v1` 与"只排序"行为）。

---

## 10. 已裁定决策（主 agent 裁定，2026-11-21；oracle 建议为默认）

| ID | 决策 | 裁定结果 | 落点 |
| --- | --- | --- | --- |
| `OD-1` | 派生池为空是否回退；派生用哪些类型 | **不回退**（未变）；派生规则 = **③b2：仅当种子 canonical genres 恰好 2 个、且文本无 director 槽时**，派生 seed 的全部 canonical genres + **整十年**；文本已给的键逐键覆盖；空池不回退；记录 `seed_genre_count` | §4 `F-5`、`B15e-7`/`B15e-7b`、`RK-5`/`RK-6`、§5 |
| `OD-2` | `R-1` 语义与 `aliases_version` | **canonical group `["Music","Musical"]`**；`aliases_version = b1-5c-aliases/v2`；**不改** `Q48`/`B22` 标注；报告记录旧/新 SHA | §4 `F-4`、`B15e-6`、`RK-8` |
| `OD-3` | director 误抽的处理方式 | **只加 `documentaries`**（不加其它复数）；加 **demonym stoplist** 的最小行为修复 + 负例断言；**不做**通用 director-catalog guard | §4 `F-3a`/`F-3b`/`F-6`、`B15e-5`、`RK-10` |
| `OD-4` | B1.5f 的生成者/时点 | B1.5e **不生成、不冻结、不运行** B1.5f；合并后由**新 worker/新会话**生成 | §7.1、`B15e-14` |
| `OD-5` | `check_self_test_independence` 是否纳入 B1.5f 路径 | B1.5e **不引用** B1.5f fixture；独立性断言由 B1.5f 再改 | §7.3、`B15e-14` |
| `OD-6` | 派生语义是否写入架构文档 | 派生语义写**报告**；架构文档等 B1.5f 之后再补，本任务不改 `docs/architecture/` | §4 `F-5` 末段、`F-7` |
| `OD-7` | 片名抽取动词白名单 | 纳入 `liked`/`enjoyed`/`watched`；加"从句不是片名"断言，**`Show me …` 与 `find …` 两类都锁** | §4 `F-2`、`A4`/`A5`/`A6` |

**`OD-1` 的实现中细化（r3；历史保留，不抹掉原文）**：r2 的 `OD-1` 字面是"派生用种子的**全部** canonical 类型、**不**做 `genres ≤2` 裁剪"。实施阶段按硬验收 `B15e-7b`（零回归）逐次实测后，主 agent 接受**细化**：

| 阶段 | 规则 | in-sample 实测 | 结果 |
| --- | --- | --- | --- |
| `OD-1` 原文 | 全部 canonical genres + 整十年 | 67/75 | 回归 `Q12`/`Q36`/`Q55`/`Q56` → 超过零回归闸门 |
| ③b | `≤2` 类型才派生 | 69/75 | 回归 `Q12`/`Q56` → 触发停止条件 |
| **③b2（最终）** | **恰好 2 类型才派生**；有 director 槽不派生 | **70/75** | 回归仅 `Q12`（主 agent 批准的唯一净回归） |

不变部分：**派生池空不回退**、逐键覆盖、`seed_genre_count` 记账、只读 route + seed 行。`Q12` 的例外依据见 §4 `F-5` 与报告 §4.3；`B15e-7b` 对其余五条的零回归约束**仍完整生效**。

**资料缺口 / 未确认项**（r2 当时的状态，保留历史；**r3 已逐条处置（见下方删除线标注）**）：

1. ~~**本轮未运行任何评测**~~ → **r3：已运行**（in-sample `G1`、`self-test`、4 个 checker、held-out `G1`、B1.5d `G1`），数字见头部元数据表与报告。
2. ~~`B22` 的 25 行 / top-5 需 worker 重新产出~~ → **r3：已用可复现 SQL + top-5 列表回填到报告 §3**（`ChW4` 不在 top-5，`acceptable_hit = true`）。
3. ~~`Q48` 的行数（4）未复核~~ → **r3：已复核为 4 行**（`WA25`/`PJH14`/`IMd17`/`THk14`），判定 ❌→✅。
4. ~~空派生池能否稳定构造未确认~~ → **r3：未构造，`A12` 未做**；③b2 下 in-sample 与 B1.5d 均无空池用例，“不回退”仍只有结构依据（报告 §4.4/§9.9）。
5. ~~`docs/ai/README.md:25` 需追加索引行~~ → **r3：已追加别名 v2 行 + 报告 `b1-5e` 索引行**。
6. B1.5f 的 fixture 前缀 / `blind_version` / checker 参数化方式**仍未定**，属 B1.5f（§7.3）；本任务**未**生成/冻结/运行 B1.5f。
7. ~~自测基数未重跑~~ → **r3：已实测基线 43 semantics / 75 sentinels / 24 independence**，修复后 **61 / 75 / 47**，0 problem。

---

## 11. 修订记录（r1 → r2 → r3）

| # | 来源 | 修订 |
| --- | --- | --- |
| P1-1 | oracle §2.3/§3 | 新增 AC `B15e-7b`（similar 用例零回归闸门 + 停止条件）；`T-3` 改为逐条列 `Q12/Q16/Q35/Q36/Q55/Q56` 前后状态；§4 `F-5` 写明"先调规则、再重测、仍回归则停" |
| P1-2 | oracle §2.2/§3 | `F-3` 收窄为**只加 `documentaries`**（删掉一串复数），新增 `F-3b` demonym stoplist 最小行为修复 + 负例断言 `A8`；`B15e-5` 改写为"行为写清"（不产生 director 槽 + 不 0 行）；原 `F-6` 降级为"被否决项" |
| P1-3 | oracle §2.5/§3 | `F-1` 的"105+30 零命中"改为"除 B10/B19/B28/B29 四条预期拒绝外零命中"（附本轮 grep 证据）；新增 AC `B15e-13`（B19/B28/B29 仍通过）；新增 AC `B15e-14`（不生成/冻结/运行 B1.5f、不改 `check_blind_holdout.py`）；全表加"in-sample 回归，不作门槛"，新增 `B15e-15` |
| P2-1 | oracle §4.1 | `F-5` 记账加 `seed_genre_count`；空池用例报告单列（`RK-5`、`A12`） |
| P2-2 | oracle §4.2 | `F-4` 固定 `aliases_version = b1-5c-aliases/v2`，记录旧 SHA `f7126f02…c203` → 新 SHA，`docs/ai/README.md` 追加行（`RK-8`） |
| P2-3 | oracle §4.3 | `B22` 的"25 行 / top-5 全 `Music` / `acceptable_hit=true`"写成可复现 SQL + 断言（`B15e-6`） |
| P2-4 | oracle §4.4 | 明确 B1.5e **不改** `check_blind_holdout.py`，参数化属 B1.5f（§7.3、`B15e-2`/`B15e-14`） |
| P2-5 | oracle §4.5 | `OD-4` 落实为"新 worker / 新会话生成 B1.5f"（§7.1） |
| P2-6 | oracle §4.6 | `Q48`/`Q34`/`Q53`/`Q54` 明确只作回归、不作泛化（§5 证据强度行） |
| P2-7 | oracle §4.7 | `R-1` 报告写"不是等价替换，召回扩大"；B1.5f 的 `R-1` 探针按 union 标注（§4 `F-4`、§5、§7.3） |
| P2-8 | oracle §4.8 | `F-2` 兜底守卫同时覆盖 `Show me` 与 `find` 两类，`B15e-3` 同时锁 `B26` 与 `H23`（`A5`/`A6`） |
| OD-1…OD-7 | 主 agent 裁定 | §10 由"开放决策"改为"已裁定"，并逐条落到 `F-*`/AC/风险项 |

### r2 → r3（实现完成后回填，2026-11-21）

| # | 来源 | 修订 |
| --- | --- | --- |
| R3-1 | 实施实测 + 主 agent 批准 | **`F-5`/`OD-1` 改为 ③b2**（仅当种子 canonical genres **恰好 2** 且无 director 槽时派生 genres + 整十年）；`OD-1` 原文与 ③b / ③b2 三段实测（67/75 → 69/75 → 70/75）作为历史保留在 §10 尾注；`OD-1` 不变部分（空池不回退、逐键覆盖、`seed_genre_count`）明确保留 |
| R3-2 | 实施实测 + 主 agent 批准 | **`Q12` 改为唯一获批净回归**（`B15e-7b` 从"给对零回归"改为"除 `Q12` 外零回归"）；补充批准依据（`Q12` 与 `Q34` 路由产物逐字节相同而标注相反）；明确“第二条回归仍必须停止” |
| R3-3 | necessity/tests reviewer 的已接受意见 | §7.1 新增**探针 ⑥**（同导演标注 + 2 类型种子 + 无文本约束 similar，复现 `Q12` 例外），并要求 B1.5f 每条 similar 的 `labeling_note` **写明标注约定** |
| R3-4 | 实测回填 | §6 `T-1`/`T-2`/`T-3`/`T-4`/`T-5`/`T-6`/`T-7` 与 `B15e-7`/`7b`/`8`/`9`/`10` 写入实际数字：自测 **43 → 61** checks（18 个新增 label；`A12` 未做）、in-sample **67/75 → 70/75**（routing 73 → 74/75）、held-out `G1` **28/30**（`H01`/`H16`）、`B23`/`Q34`/`Q53`/`Q54` 变绿、`Q57` 仍失败 |
| R3-5 | 实施回填 | 头部状态改为 **实现完成 / 待 PR**；§5 增“实测结果（r3）”行；§9 `RK-5`/`RK-6` 写入实际处置；§10 “资料缺口”逐条标为已处置/仍未确认（`A12`、B1.5f 参数化） |
| R3-6 | 实际代码 | `F-2` 落地时额外加了 **`FUNCTION_WORDS`/`GENRE_TITLE_WORDS` 动词语句守卫**与**汉字粘连边界修复**（`查一下The Matrix`），超出 r2 文字但属 `RK-2`/`RK-3` 的同一风险面，已写入报告 §2 `F-2` |

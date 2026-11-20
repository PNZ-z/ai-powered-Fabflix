# 2026-11-21 · B1.5e（issue #78）— 修 B1.5d 暴露的路由/执行缺口并落码 `R-1`/`R-2`

| 项 | 值 |
| --- | --- |
| 工作流 | B1.5e：评测层五个缺口修复 + `R-1`（`Music`/`Musical` 同族）+ `R-2`（相似查询派生约束） |
| issue | [#78](https://github.com/PNZ-z/ai-powered-Fabflix/issues/78)（OPEN，正文 `Refs #74`、**无 closing keyword**）；关联 **#74（保持 OPEN）**、#76 已 CLOSED |
| 计划 / issue 文档 / 报告 | `docs/plans/b1-5e-routing-gap-fixes.md`（r3）、`docs/issues/issue-78-b1-5e-routing-gap-fixes.md`、`docs/ai/reports/b1-5e-routing-gap-fixes.md` |
| 分支 / 提交 | 分支名与提交号**未确认**（本轮工作区未提交、未推送；`main` 基线为 `b26f35e`，B1.5d 合并 PR #77） |
| PR | **未创建**（实现完成 / 待 PR）；PR 正文须以 issue 文档为准，且**不使用 closing keyword** |
| 状态 | 实现完成 / 待 PR；三路 reviewer 意见已处理；全部验证已实跑 |

> **本文只记录实际发生的事。** 所有数字均来自本轮实际运行的产物（`.pi/artifacts/b1-5e/*.json`）与 `git diff`，不是估算或计划值。

---

## 一、修改了哪些文件及潜在问题

### 1.1 代码与契约文件（worker 实现，已实测）

| 文件 | 实际改动 | 目的 |
| --- | --- | --- |
| `scripts/eval/tool_eval.py`（+328/−25） | ① `REFUSAL_RULES` 新增 `out_of_catalog` 类（中文 `票房`/`预算`/`发行公司`/`配乐`/`作曲`/`奥斯卡`/`获奖`/`奖项`；英文 `box office`/`grossed`/`budget`/`distributor`/`which studio`/`who composed`/`composer`/`academy award`/`oscars`）；② `TITLE_PATTERNS` 动词扩为 `like/liked/love/loved/enjoyed/watched`（拆成 `TITLE_VERB_PATTERN`）；③ 新增 `FUNCTION_WORDS`/`GENRE_TITLE_WORDS`/`opens_clause()` 动词语句守卫，`movies I enjoyed in the 1990s`、`I enjoyed comedy films from the 1990s` 不再被当片名；④ `extract_title` 兜底改为**从 marker 之后**搜索并加"首词必须是原文真大写起始词"、`TITLE_STOPWORDS`、类型词守卫，另修**汉字粘连边界**（`查一下The Matrix` → `The Matrix`）；⑤ 路由改为在**去掉返回片名后的文本**上抽 genres/years/rating（按返回 title 字符串掩码，不按 pattern span）；⑥ 新增 `DEMONYM_STOPLIST`（30 个国籍/语言形容词），`extract_director` 逐匹配跳过；⑦ `run_g1` 种子跟随加 **③b2** 派生（恰好 2 类型 + 无 director 槽）+ 整十年 + 逐键覆盖 + `derived_constraints`/`effective_constraints`/`seed_genre_count` 记账；⑧ 自测新增 18 个 label；⑨ 既有断言 `route: musical != music` 改为数据层断言 | 修 `B10`（越界不拒）、`B26`（片名区段污染）、`B23`（`seed_title` 抽取 + `R-2`）、`B27`（`documentaries` 未抽取 + demonym 被当导演）、`B22`/`Q48`（`R-1`）、`Q34`/`Q53`/`Q54`（`R-2`） |
| `docs/ai/aliases/genre-aliases.json`（+8/−3） | `aliases_version` `b1-5c-aliases/v1` → **`b1-5c-aliases/v2`**；`canonical_groups` 增 `["Music","Musical"]`；别名只新增 `"documentaries": ["Documentary"]`；`notes` 删掉一条**从未在 `check_schema.py` 实现过的过度声明**（"target 不得出现在 canonical group variant 位"），换成真实约束说明 | `R-1` 同族 + `B27` 词形；SHA `f7126f02…c203` → **`ab8e07dd…c104`** |
| `scripts/eval/check_schema.py`（+6） | 追加一条**显式断言**：`Music`/`Musical` 必须同组且 canonical key = `Music` | `R-1` 的组形状上锁（与既有 Sci-Fi 组断言并列） |

### 1.2 文档（本轮 docs-writer 收尾）

| 文件 | 实际改动 |
| --- | --- |
| `docs/plans/b1-5e-routing-gap-fixes.md` | 状态 → **实现完成 / 待 PR**；§4 `F-5` / §5 / §6 `T-1`…`T-7` / §7.1（**新增探针 ⑥** + similar `labeling_note` 必须写明标注约定）/ §7.2 / §8（`B15e-7`/`7b`/`8`/`9`/`10`）/ §9（`RK-5`/`RK-6`）/ §10（`OD-1` = ③b2，**原文与 ③b/③b2 三段实测作为历史保留**；资料缺口逐条标为已处置）/ §11（新增 r2 → r3 修订记录）全部按实际实现回填 |
| `docs/issues/issue-78-b1-5e-routing-gap-fixes.md` | 头部状态 + 元数据（实现状态、实测数字）；`F-5` 改为 **③b2 + `Q12` 唯一例外**（含 `OD-1` → ③b → ③b2 的实测理由）；AC 7 / 7b / 9 对齐；`E-1` 写明 18 个新增 label 与 `A12` 未做；Scope 顶部明确 **B1.5f 不在本 issue**；`Risks`/`Dependencies` 同步；Internal tracking 记录实现状态、超出原文字的实现、**远端正文仍为 r2 表述（未执行远端编辑）** 与未验证项；**不写 closing keyword** |
| `docs/ai/reports/b1-5e-routing-gap-fixes.md` | §9.7（README 索引）与 §9.10（计划/issue 待回填）改为**已回填**并写明当前状态；新增 **§11「文档回填与结论边界」**（回填清单 + 四条结论边界） |
| `docs/ai/README.md` | 新增 `reports/b1-5e-routing-gap-fixes.md` 索引行；`last_updated` 增 **r11** 摘要；`关联计划` 增 b1.5e 计划；别名 v2 行保留（worker 已加）。**B1.5c / B1.5d 的状态行未改** |
| `docs/plans/ai-roadmap-overview.md` | 新增第 **15** 条：B1.5e 实现完成 / 待 PR；**B1.5f 未生成/未冻结/未运行**；派生收窄只在评测替身、生产语义未实现；#74 保持 OPEN；**不声称泛化、不声称 B-① 可开始** |
| `docs/logs/2026-11-21-issue-78-b1-5e-routing-gap-fixes.md` | 本文件 |

### 1.3 明确**未**改动的文件（实测）

`src/main/**`、`pom.xml`、CI、`docs/ai/tool-schema.b1-5.json`（`contract_version b1.5-tools/v3`、`schema_hash 712c0e34…474a` 未变）、`docs/ai/golden-set.schema.json`、`scripts/eval/check_blind_holdout.py`、B1.5a–B1.5d 的全部 fixture / 计划 / 报告（**B1.5d fixture/checker SHA 未变**）、任何阈值与分母（`G1-T ≥ 27/30`、`/30` 全分母不变）。

### 1.4 潜在问题 / 遗留风险（如实记录）

1. **`Q12` 是唯一净回归，且是标注冲突而非代码缺陷**：`Q12` 与 `Q34` 的路由产物逐字节相同（`{seed_title, result_constraints:{}}`，种子均 2 个 canonical 类型），但冻结标注相反（`Q12` = 同导演、`Q34` = 类型×同年代）→ 不读 golden 不可区分。主 agent 批准接受该条失败。**B1.5f 必须以预声明探针复现该形态**（计划 §7.1 ⑥），否则下一代盲集仍不可判定。
2. **`A12`（空派生池不回退）零实测证据**：③b2 下 in-sample 与 B1.5d 都没有"2 类型种子且 AND 后为空"的用例，`--self-test` 也没有该样本 → "不回退、如实失败"只有结构依据；列入 B1.5f。
3. **③b2 的边界是实测边界，不是产品语义**：1 类型种子（`Q16`/`Q35`/`Q56`）与 3+ 类型种子（`Q55`）不派生，靠的是 `_similar` 的同导演排序；若 B1.5f 抽到"1 类型种子且 golden 期望类型收窄"的用例会失败（已知边界，非实现缺陷）。
4. **生产语义未实现**：`find_similar_movies` 只有 `movie_id`/`same_primary_genre_only`/`limit`，"派生收窄"只在评测替身 `run_g1` + `evaluate_calls(..., effective)` 内；`docs/architecture/` 本轮不改，**不能宣称生产已如此**。
5. **`FUNCTION_WORDS` 带来新的窄边界**：含 `who`/`what`/`when` 等，`I loved Who Framed Roger Rabbit` 这类以疑问词开头的真片名会在 verb 路径被判为从句。135 条 fixture 的路由产物逐条 diff 为零变化，但**无 fixture 覆盖这种片名**。
6. **别名 SHA 变化**导致本报告产物指纹与 B1.5c/B1.5d 不可直接比较；B1.5f 必须重新冻结基线。`R-1` 是**召回扩大**（`Music` 请求返回 `Music ∪ Musical`，619 + 139），不是等价替换——B1.5f 的 `R-1` 探针必须按 **union（25 行）** 标注。
7. **`Q57`/`OPEN-C3` 未落码**，仍失败（种子消歧），不在本任务范围。
8. **远端 #78 正文仍是 r2 表述**（`F-5` = "all seed genres, no ≤2 cap"、AC 7b = "none may regress"）。本地规范记录已改为实际 ③b2 + `Q12` 例外；**本轮未发起任何远端编辑**，是否覆盖远端正文由主 agent 决定。PR 正文必须以本地文档为准。
9. **B1.5f 完全未做**：未生成、未冻结、未运行；仓库内 `b1-5f` 只出现在计划/issue 的边界性文字里（无 fixture 路径、无 `blind_version`）。因此**任何"泛化 ≥90%"、"B1.5c/B1.5d/B1.5e 已达标"、"B-① 可开始"的说法都不成立**。
10. **held-out B1.5c 与 B1.5d 都已是 seen fixture**：本轮用它们核对 `H23`、`B10`/`B22`/`B23`/`B26`/`B27`/`B28`/`B29`；两者都不能再作门槛。held-out 的 28/30 是 `G1`（无 resolver），**不可与 B1.5c 的 `G1-T` 数字直接比**。

### 1.5 实际运行的验证（证据）

| 命令 | 结果 |
| --- | --- |
| `tool_eval.py --variant G1 --cases both --allow-full-run --assert-no-annotation-read` | **70/75**（修复前 67/75）；routing 73 → **74**/75；similar 11/15 → **13/15**；filter 19/20 → **20/20**；失败集 = `Q12`（批准）+ `Q21`/`Q22`/`Q23`（中文片名，`G1` 无 resolver）+ `Q57`（`OPEN-C3`）；`annotation_guard = true`；产物 `.pi/artifacts/b1-5e/after-G1.json` / `before-G1.json` |
| `tool_eval.py --self-test` | **61 checks / 0 problem**（修复前 **43**）；`annotation sentinel` **75 cases / 0 problem**；`self-test independence` **47 utterances / 0 problem**（修复前 24） |
| `check_schema.py --quiet` / `check_golden.py --quiet` / `check_heldout.py` / `check_blind_holdout.py` | 全部 exit 0；`schema_hash`、`db_snapshot_hash`、B1.5d fixture/checker SHA 未变 |
| held-out B1.5c `G1`（`queries-heldout.b1-5c.json`，0 model） | **28/30**，失败 `H01`/`H16`（中文片名 resolver 槽位，`G1` 无 resolver）；`H23` ✅ 且判定不变；**仅记录，非门槛** |
| B1.5d `G1`（`queries-blind.b1-5d.json`，0 model） | **28/30**，失败 `B01`/`B14`（均为 B1.5d 既有、与本任务无关的失败）；**仅记录，非门槛** |

---

## 二、新增知识点 / 重点学习对象

### 2.1 评测方法学（Python / 评测脚本设计）

- **"in-sample 回归证据"与"门槛"的区别**：同一批 75 条用例既是开发集又是回归集时，分数只能证明"没退化"，不能证明"泛化"。本项目把主门槛固定为**新盲集上的一次冷跑**（`G1-T` ≥ 27/30，`/30` 全分母），`/28` 之类的敏感性口径明文禁止用于宣称 PASS。位置：报告 §1/§8、计划 §2 `IC-1`。
- **标注（annotation）与实现隔离**：`--assert-no-annotation-read` + `annotation_guard` 保证执行半段只读 route（文本）+ seed 目录行，不读 golden；派生约束因此不能用"答案键"反推。位置：`scripts/eval/tool_eval.py` 的 `ANNOTATION_KEYS` 与 `run_g1`。
- **变异实测（mutation testing）验证断言的有效性**：reviewer 要求"把 `== 2` 改成 `>= 1`、删整十年派生、把逐键覆盖改成整字典替换后断言是否 fail"——七种变异全部被对应用例捕获。这比"断言全绿"更能说明测试真的锁住了规则。位置：报告 §6 表末「回归敏感性（已用变异实测）」。
- **量纲陷阱**：`rows_returned`（展示行，不含 `displayed:false` 的 seed 命中行）、`candidate_pool`（服务端收窄后的候选池）、SQL 计数是三个不同的量。reviewer 抓到"返回行数 +1"的系统性偏差，报告据此加了量纲说明。位置：报告 §4.4「量纲说明」。

### 2.2 正则与文本抽取（Python `re`）

- **"从句 vs 片名"的启发式判定**：动词模式 `(?:like|liked|love|loved|enjoyed|watched)\s+<TitleRun>` 会把 `I enjoyed comedy films from the 1990s` 捕获成"片名"。修法有两层：① **首词停用词/功能词守卫**（`FUNCTION_WORDS` 含 `in`/`at`/`from`/`the`…），② **类型词守卫**（首词落在别名表里就判为从句）。位置：`tool_eval.py` `TITLE_VERB_PATTERN`/`opens_clause()`。
- **大小写不能用 `IGNORECASE` 或折叠后的匹配串判定**：兜底分支的 `re.search(EN_TITLE_RUN, text)` **不带** `IGNORECASE`，而 `EN_TITLE_RUN` 只约束首 token 大写、后续 token 允许小写 → 必须直接判定**原始文本**里 run 的首 token 是否真的大写起始。位置：`extract_title` 兜底。
- **按"返回字符串"掩码而不是按 `match.group(1)` 的 span**：`TITLE_CUTS` 会截断返回值，而原始 capture 含 ` - horror films from 1991 to 1992`；按 span 掩码会把真实约束一并删掉。位置：`route_deterministically`。
- **`str.isalnum()` 对汉字为真**：英文片名紧贴中文（`查一下The Matrix`）时，词边界判定必须把"前一个字符是汉字"也算边界。位置：`extract_title` 兜底边界。

### 2.3 数据契约与 SQL

- **canonical group ≠ 检索 union**：`canonical_genres(["Musical"]) == ["Music"]` 只做"槽位比较折键"，真正让 `filter_movies{genres:["Music"]}` 返回 `Musical` 行的是另一个函数 `genre_member_sql()`（展开成员列表）。两者要分别断言。位置：`tool_eval.py:167` vs `:192`，自测 `route: musical and music share one canonical key` 与 `genre AND: Music includes Musical`。
- **"召回扩大"与"等价替换"的差别**：把两个 enum 值折叠成一族后，请求一个会返回两个（619 + 139 行），命中集合变大但语义不是等价——报告与 issue 必须写明，且下游探针要按 union 标注。位置：报告 §3。
- **`check_schema.py` 的 `unsupported = enum − targets − variants − keys` 算术**：别名表的键集被这条等式严格约束，"随便加一个词"会破坏 `unsupported` 校验；因此 demonym（国籍形容词）放进代码常量而不是数据文件。位置：`check_schema.py` `check_genre_aliases`。
- **顺序敏感的派生规则**：先修兜底守卫、再加掩码（顺序颠倒会让 `H23` 回归）；先算 `derived`、再用 `{**derived, **stated}`（顺序颠倒则显式键被覆盖）。

### 2.4 变更治理（本项目工作流）

- **"零回归闸门 + 例外必须具名"**：AC 7b 从"绝对零回归"改成"除主 agent 批准的 `Q12` 外零回归"，并要求写明用例、原因、批准来源；**第二条回归仍然要停止**。这是防止"回归被写进报告后继续推进"的机制。
- **计划/issue/报告三处必须同步**：实现规则一旦在实施中细化（`OD-1` → ③b2），计划 §4/§10、issue 的 `F-5`/AC、报告 §4 都要改；否则下一位读者会按相反规则实现。位置：本次 r3 回填。
- **远端正文与本地规范记录可能漂移**：GitHub issue 已发布后本地修订，属于"文档已更新、远端未改"，必须在 Internal tracking 显式记录，不能假装远端一致。

---

**结论（只能这样说）**：B1.5e 修掉了 B1.5d 点名的五个评测层缺口并落码 `R-1`/`R-2`，在 in-sample 上由 **67/75 提升到 70/75**，唯一回归 `Q12` 已由主 agent 批准；这些都只是**回归证据**。泛化仍未测量：**B1.5f 未生成、未冻结、未运行**，`G1-T ≥ 27/30` 的主门槛仍未回答，B-① 未开始。

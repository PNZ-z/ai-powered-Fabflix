# 2026-11-21 · B1.5f（issue #81）— 新盲集 `C01`–`C30` 重测 `G1-T` 门槛：**FAIL（24/30 = 80.0%）**

| 项 | 值 |
| --- | --- |
| 工作流 | B1.5f：新 30 条 blind holdout（`C01`–`C30`）+ 一次冷跑 `G1-T` + 报告；**不修任何规则** |
| issue | [#81](https://github.com/PNZ-z/ai-powered-Fabflix/issues/81)（`OPEN`，正文 `Refs #74`、**无 closing keyword**）；关联 **#74 → 保持 OPEN** |
| 计划 / issue 文档 / 报告 | `docs/plans/b1-5f-new-blind-holdout.md`（r3 结果回填 / §11）、`docs/issues/issue-81-b1-5f-new-blind-holdout.md`（实施状态回填）、`docs/ai/reports/b1-5f-new-blind-holdout.md`（**新建**） |
| 分支 / 提交 | 分支 = **`codex/b1-5f-blind-holdout`**；提交 = **`9de1da5`**（“feat(eval): add the B1.5f blind holdout and record its G1-T failure”）—— **由主工作流创建，非 docs-writer**；docs-writer **未执行任何 `git add`/`commit`/`push`**。被测基线 = **`7915fcd`**（B1.5e 合并，PR #79）；PR #82 合并后 `origin/main` = **`b406fad`** |
| PR | **#82 已合并**（2026-11-21T00:12:24Z，merge commit `b406fad`）；#81 **CLOSED**；#74 保持 OPEN |
| 实际运行时刻 | 冻结 `2026-11-20T23:52:22Z`（本地 `15:52:22-08:00`）；`G1` `23:54:53Z`；预热 `23:55:01Z`（完成 `23:55:08Z`）；冷跑 `23:55:08Z`；热跑 `23:55:17Z`；回放 `23:55:18Z` |
| 结果 | **`G1-T` 冷跑 24/30 = 80.0%（Wilson [62.7%, 90.5%]）< 27/30 → FAIL**；`G1` 23/30 = 76.7%；热/回放 24/30 逐例一致 |
| 日志文件名说明 | 按任务指定沿用 B1.5 系列的 `2026-11-21` 命名；**实际运行时刻是 2026-11-20T23:52–23:56Z**（见上） |

> **本文只记录实际发生的事。** 数字来自本轮实际产物（`.pi/artifacts/b1-5f-freeze.json`、`.pi/artifacts/b1-5f-eval/*.json` + `run-timeline.txt`）与 `git`/`shasum` 实测，不是估算或计划值。

---

## 一、修改了哪些文件及潜在问题

### 1.1 本轮 docs-writer 实际改动的文件（只改 `docs/`）

| 文件 | 实际改动 | 目的 |
| --- | --- | --- |
| `docs/ai/reports/b1-5f-new-blind-holdout.md` | **新建**（FAIL 报告）：结论、blind 定义/边界、冻结与时间线、主表（`G1`/`G1-T` 冷/热/回放 × `/30`+`/28`×2+`/27` + Wilson）、分意图表、解析指标（n=2）、6 条失败逐条归因、探针表 `P-1`…`P-8`（含 `P-4` **预声明失败但机制未命中** 的区分）、`R-1` union 13 行证据（实际 top-5）、B1.5e title guard 回归与中文“喜欢”缺口、C01 幻觉、统计力与结论边界、零改动证明、验收条件对照、附录 A 30 条逐例结果 | 为 #81 留下可复核的实测记录，并把"失败在哪一层"写清楚供 B1.5g 使用 |
| `docs/plans/b1-5f-new-blind-holdout.md` | 头部标题/状态 → **🔴 已执行 / FAIL / 报告完成**；`last_updated`、关联 issue（远端正文已一致，`OI-f3` 关闭）、新增"执行结果（r3）"元数据行；§3.3 `P-1` 行补 **r3 已落地实测**（top-5 含 `RMY15`）；§9.2 `OI-f3` 标为已闭合；§10 第 3 条改为"r3 只改 docs"；**新增 §11 执行结果回填**（结论、主表、6 条失败、探针结果、已批准偏离 `P-1` 窗口 1980–82/13、未做/缺口、下一步） | 让计划反映**实际实现**（含主 agent 批准的 `P-1` 窗口偏离），并把 B1.5g/B1.5h 的边界写进计划 |
| `docs/issues/issue-81-b1-5f-new-blind-holdout.md` | 头部标题/状态 → **🔴 已执行 / FAIL / 报告完成 / PR 待建**；元数据新增"实施状态""关闭时机"（PR 合并后可关、#74 OPEN、不写 closing keyword）；Internal tracking 同步（`r3` 实测证据、正文已一致）；**附录 D 更正为"3 处补丁已全部生效"** 并给出实测计数与 `difflib` 空 diff 证据 | 让 #81 的规范记录与实施状态一致，且不虚构远端变更 |
| `docs/ai/README.md` | §1 新增 3 行索引（`queries-blind.b1-5f.json`、`query-routing-blind.b1-5f.json`、`reports/b1-5f-new-blind-holdout.md`，附 SHA/字节/FAIL 结论）；b1-5e 报告行追加"r12 补注"链接；`last_updated` 增 **r12** 摘要；`关联计划` 把 b1-5e 标为 PR #79 已合、加 b1-5f 计划；§4 修订表新增 **r12** 行。**B1.5e / B1.5d / B1.5c 的历史结论行未改** | 让评测产物目录能查到 B1.5f 的 fixture 与 FAIL 结论，且不覆盖历史 |
| `docs/plans/ai-roadmap-overview.md` | 新增第 **16** 条：B1.5f 已执行 / FAIL + 冻结/首跑时刻 + 主表数字 + 6 条失败归因 + 成立的探针 + 下一步 **B1.5g + 新盲集 B1.5h** + #74 OPEN / #81 报告完成 / **B-① 未开始** | 路线图层面记录"未泛化"这一事实与下一步，不声称泛化、不声称 B-① |
| `docs/logs/2026-11-21-issue-81-b1-5f-new-blind-holdout.md` | 本文件 | AGENTS 要求的每次工作流日志（两大类） |

### 1.2 本轮**未**改动（实测证据）

| 项 | 证据 |
| --- | --- |
| `scripts/eval/tool_eval.py` | `991db086c5c618be479b273b8af9db6151e77f3a334b14fa2a557089c184af04`；`git diff --stat 7915fcd -- scripts/eval/tool_eval.py` **输出为空** |
| `scripts/eval` + `docs/ai` 的已跟踪文件 | `git diff --stat 7915fcd -- scripts/eval docs/ai` **输出为空** |
| 既有 checker ×4 | `check_schema.py` `5cb35c7f…1b9f`、`check_golden.py` `0ab8a017…0708`、`check_heldout.py` `d436f5ca…ddd4`、**`check_blind_holdout.py` `8259cc8d…7caf`（byte-identical）** |
| 契约 | `docs/ai/tool-schema.b1-5.json` `0ba15269…1169`；`contract_version b1.5-tools/v3`；`schema_hash 712c0e34…474a`（**未变**） |
| 别名 ×3 | genre `ab8e07dd…c104`（`b1-5c-aliases/v2`）、person `06316859…dd98`、title `98bfca9b…cbe4`（仍 `entries: []`，实测 `alias_hits = 0`） |
| B1.5a–B1.5e 的 fixture/报告 | 冻结记录 `invariants` 表逐一列 SHA；本轮未改 |
| 阈值 / 分母 / 用例 | `G1-T ≥ 27/30`、`/30` 全分母不变；`C01`–`C30` 冻结后未改；**没有为了好看而剔除失败条目** |
| 生产面 | 无 `src/main/**`、无 `pom.xml`、无 CI、无 MCP、无 DB 写入（只读 `SELECT`） |
| B1.5f 的新增产物 | 3 个文件：`docs/ai/golden/queries-blind.b1-5f.json`、`docs/ai/golden/query-routing-blind.b1-5f.json`、`scripts/eval/check_blind_holdout_b15f.py`（生成阶段产出，**非 docs-writer 修改**；已随 `9de1da5` 一并提交） |

### 1.3 实际执行/复核的命令（证据）

| 命令 | 结果 |
| --- | --- |
| `shasum -a 256 queries-blind.b1-5f.json query-routing-blind.b1-5f.json check_blind_holdout_b15f.py` | `2e36bbf5…121a` / `a74e383b…fb40` / `d5cfe94a…29fd` —— **与冻结记录逐一一致** |
| `.pi/artifacts/b1-5f-eval/blind-G1.json`（`--variant G1`，0 model） | **23/30 = 76.7%**；routing 23/30；`model_calls 0`；失败 7 条（`C01`/`C05`/`C11`/`C12`/`C16`/`C17`/`C18`） |
| `.pi/artifacts/b1-5f-eval/blind-G1T-cold.json`（**主门槛**，缓存 `absent`） | **24/30 = 80.0%**；routing 24/30；`model_calls 2`；`resolver.triggered 2`（`C01`/`C11`）、`parse_success 1`、`candidate_reject 1`、`wrong_movie 0`、`fallback_to_clarify 1`；latency p50 208.4 ms / p95 210.1 ms |
| `.pi/artifacts/b1-5f-eval/blind-G1T-warm.json`（`loaded`） | 24/30；`model_calls 0`；`cache_hits 2`；**与冷跑逐例一致**（`task_success` + `chosen_movie_id` 全同） |
| `.pi/artifacts/b1-5f-eval/blind-G1T-replay.json`（`replay`） | 24/30；`model_calls 0`；**与冷跑逐例一致** |
| 四个产物的 `annotation_guard` / `db_snapshot_hash` | 全部 `true` / 全部 `8b16c047…f925` |
| checker 记录（冻结记录 `checker`） | `check_blind_holdout_b15f.py` **18 组断言**（`--skip-db`/全量/`--quiet` 各 exit 0）；**变异自测 8/8 被捕获**（`MUT-1`…`MUT-8`，逐条 exit 1） |
| 分母复算 | `/28` 非CJK **23/28**（`C01` 失败、`C11` 通过 ⇒ 24−1）；`/28` 去 `P-1`/`P-2` **22/28**（两条都通过 ⇒ 24−2）；`/27` 再去 `P-4` **22/27**（`C18` 本就失败）；Wilson 区间已用 `z=1.96` 独立复算并吻合 |
| `gh issue view 81 --json body` vs 本地正文 `difflib` | **diff 为空**；计数 `ranked, not narrowed`=0、`same director`=6、`165`=0、`two unexpected failures`=1、`pending oracle review`=0、`Closes/Fixes/Resolves`=0 |
| `gh issue view 74` | `OPEN`、milestone #3 |
| `curl -o /dev/null -w %{http_code} http://<LAN-IP>:4173/iterations/b1-5f/` | **200**（`ruby` 在 `*:4173` 监听）；`demo/index.html` 已指向该 iteration |

### 1.4 六条失败的归因（与报告 §4 一致）

| 用例 | 现象（实测 tool call / answer） | 归因 |
| --- | --- | --- |
| `C01`（`P-8`） | resolver `triggered`，LLM 候选 `Fly Away High` 被目录拒绝 → **无工具调用**，回 `which English title do you mean?` | 真实中文译名**幻觉**缺口（拒绝行为正确：`wrong_movie = 0`；`title-aliases.json` 仍空，无回退知识） |
| `C05` | `seed_title = None` → `filter_movies{genres:["Fantasy"], 1993–1995}` | **中文“喜欢”未触发 seed 抽取**（动词触发词只有英文 `like/liked/love/loved/enjoyed/watched`） |
| `C12` | `query = None` → `filter_movies{genres:["Western"]}`（5 行别的西部片） | **B1.5e `F-2` title guard 过度修正（回归）**：首词 `Western` 是类型别名 ⇒ `Western Union` 整段被判从句 |
| `C16`（`P-6`） | `seed_title = None` → `filter_movies{genres:["War","Adventure"], 1983–1984}` → **0 行** | **同一个 title guard 回归**（首词 `War`）。`War` 成为约束是"片名没被抽取"的机械后果，**不是**新的类型词泄漏缺陷（若片名被抽取，B1.5e 的掩码会剔掉它） |
| `C17` | `seed_title = None` → `filter_movies{genres:["Fantasy"], 1986–1988}` | **中文“喜欢”未触发 seed 抽取**（同 `C05`） |
| `C18`（`P-4`） | `action = "clarify"`（`which movie intent?`），`find_similar_movies` **未调用** | **预声明失败，但机制未命中**：`seed_title = None` 先失败 ⇒ `Q12` 语义**未被真正行使**；**不得声称 `Q12` 例外已被复现** |

### 1.5 潜在问题 / 遗留风险（如实记录，不粉饰）

1. **B1.5e 的 `F-2` title guard 引入真实回归**：`C12`（`Western Union`）/`C16`（`War Games`）证明"首词是类型词 ⇒ 判为从句"这一单条件否决过强。修法应在 B1.5g 做**双向判据**（同时要求从句结构、或目录存在性校验），并保留 `B23`/`H23` 的"句子不是片名"断言。**本批不修**。
2. **中文 seed 触发词缺口**（`C05`/`C17`）：n = 2，只能说"这两条形态失败"，不能估计中文 seed 抽取整体成功率；也不能据此断言中文全不可用。
3. **中文译名幻觉**（`C01`）：`qwen2.5:7b` 把 `飞越疯人院` 译成不存在的 `Fly Away High`；同一 resolver 在 `公民凯恩 → Citizen Kane` 上成功。**n = 2**，既不能声称"中文解析不可用"，也不能声称"加一条别名就达标"（后者等于改被测对象）。
4. **`P-4` 的"预声明失败"与"探针形态未命中"必须分开写**：本批 `C18` 走的是 `clarify`，`find_similar_movies` 未被调用 ⇒ `Q12` 例外仍**未被复现/量化**；若下一代盲集仍抽到同形样本，需先保证 seed 抽取能过。
5. **`BF-1`/`BF-2` 无法硬证明**：生成/标注与 B1.5e 修复在同一仓库流水线内，且**没有"实际读过文件清单"落证**。因此报告措辞已降级为"未参与规则开发的样本"，**不得**表述为"独立第三方盲测"。
6. **回放缓存卫生**：回放**未显式传 `--resolver-cache`**，`cache_path` 落回默认 `.pi/artifacts/b1-5c-resolver-cache.json`（`replay_resolver` 正确指向冷跑缓存）；`model_calls = 0`、`cache_hits = 2` 表明无写入，但**缺运行前后该文件的 hash 对照**（与 B1.5d 同一处缺陷，未在本批修复）。回放 `base_url` 记为 `localhost`（冷/热为局域网 `<host>`），对结果无影响。
7. **预热无独立产物**：预热时刻只能引用 `run-timeline.txt`（`prewarm_done=23:55:08Z`）；冷启动延迟（`OPEN-b2`）仍未测。
8. **B1.5f 数字必须冻结**：`C01`–`C30` 一旦被用于改规则并重跑，本批即降级为 seen fixture（重复 B1.5c 的错误）。B1.5g 必须配**新盲集 B1.5h**。
9. **`plan_deviation`（`P-1` 窗口 1948–1950/25 → 1980–1982/13）是已批准偏离**：批准来源 = 主 agent 生成阶段裁定 B（理由：旧窗口的 24 个 `Music` id 与 B1.5d `B22` 可接受集逐一相同，违反 135 条计分的逐 id 不重叠规则）。已回填计划 §3.3 / §11 与 `docs/ai/README.md`；**远端 #81 正文实测也已是 1980–1982 / 13 行口径**（`1948` 计数 0）。
10. **验收缺口仍在**：`G0`/`G2`/`G3`/MCP **未测/未实现**；执行器与解析器仍为评测 stand-in，`R-2` 派生收窄只在替身里，**不代表生产语义**。
11. **交付状态提醒**：docs-writer 本身**未 `git add` / `commit` / `push`**；但本轮写作期间主工作流已把工作区（含本文件改动的 docs）提交为 **`9de1da5`** 并切到分支 `codex/b1-5f-blind-holdout`（已推送至同名 origin 分支；**PR #82 已合并**）。工作区剩余 `?? .idea/` 与本任务无关。**未发现其他已知遗留问题**。

---

## 二、新增知识点 / 重点学习对象

### 2.1 评测方法学（盲测设计与统计口径）

- **"盲测"的可操作定义 = 冻结早于首跑 + 首跑只跑一次**，而不是"作者不知情"。本批用 `.pi/artifacts/b1-5f-freeze.json`（`tool_eval_runs = 0`、三 SHA、blob + mtime）把"先冻结后运行"变成可复核证据。位置：报告 §1.1/§2。
- **容错算术必须预登记**：`/30` 最多 3 条失败，`P-4` 预声明失败占 1 条 ⇒ 只剩 2 条非预期失败空间。本批非预期失败 5 条，直接判定超额 —— 这样可以防止事后把"29/30"讲成 30/30。位置：报告 §3.1 第 4 条。
- **Wilson 区间与"点估计判定"分离**：`24/30` 的区间上界 90.5% 略高于 90%，但**门槛按点估计判**；同时禁止用"上界 > 90%"反推"可能达标"。位置：报告 §8.1。
- **敏感性分母必须逐条列名**：`/28` 有两种不同成员（去 CJK vs 去探针），数值可相同但含义不同；`/27` 必列 `P-1`/`P-2`/`P-4`。位置：报告 §3.1/§3.2。

### 2.2 归因方法（把"现象"与"根因"分开）

- **"类型词泄漏" vs "title 抽取回归"**：`C16` 的表面现象是 `genres:["War","Adventure"]`，但根因是片名未被抽取，导致 B1.5e 已有的**片名掩码**没有对象可掩。把现象当根因会让修复打错位置。位置：报告 §5.2。
- **"预声明失败" ≠ "探针命中"**：`C18` 如预期失败，但走的是 `clarify`，`find_similar_movies` 一次没调用 ⇒ 它证明的是 seed 抽取失败，而不是 `Q12` 的语义冲突被复现。位置：报告 §4.1。
- **"行为正确但任务失败"要分开记账**：`C01` 的 resolver 正确拒绝了幻觉（`wrong_movie = 0`），但任务没完成；报告同时写这两件事，避免把"防御正确"读成"任务通过"。

### 2.3 文本抽取与正则（Python `re` 的启发式代价）

- **单条件否决的守卫会制造反向错误**：`GENRE_TITLE_WORDS`（首词是类型词 ⇒ 从句）修好了 `B23`/`B26`，却把 `Western Union`、`War Games` 这类**以类型词开头的真片名**吃掉。经验：抽取守卫最好**双向判据**（正向证据 + 反向否定），并在新增守卫时立刻准备"反例集"。位置：报告 §7.1。
- **中文触发词不能只靠英文动词 pattern**：`喜欢 + 英文片名` 在中文句里没有英文动词锚点，`seed_title` 直接为 `None`。多语言路由必须显式列出中文触发词（`喜欢`/`想看`/`像…那样`）。位置：报告 §7.2。
- **小模型译名的失败模式是"编一个像样的英文名"**：`飞越疯人院 → Fly Away High`。这说明**目录校验（catalog verification）是必需的安全网**，而不是可选优化——它把"错片"变成"澄清"，代价是任务失败但不错答。

### 2.4 变更治理（本项目工作流）

- **"不得用失败条目改规则后重跑同一集"**：这是 B1.5c→B1.5d→B1.5e→B1.5f 反复强调的纪律；本批失败后 B1.5g 必须配新盲集 B1.5h，否则新数字只是 seen 回归。
- **文档三处同步（计划/issue/报告）**：实施中的已批准偏离（`P-1` 窗口）必须同时回填计划 §3.3、§11 与 `README` 索引，否则下一位读者会按旧窗口（1948–1950/25）复核而判失败。
- **远端 issue 与本地记录的一致性要实测再断言**：本轮用 `gh issue view 81 --json body` + `difflib` 证明"本地正文 = 远端正文"，从而把附录 D 从"3 处待更正"更正为"已全部生效"——**没有实测就不该写"已应用/未应用"**。

---

**结论（只能这样说）**：B1.5f 首次在**未参与规则开发**的新 30 条上测量 B1.5e 之后的规则，结果是 **`G1-T` 冷跑 24/30 = 80.0% < 27/30 → FAIL**；失败集中在 **B1.5e title guard 回归**（`C12`/`C16`）、**中文 seed 触发词缺口**（`C05`/`C17`）、**中文译名幻觉**（`C01`）与 1 条预声明失败（`C18`，且未真正命中 `Q12` 语义）。B1.5e 的修复**未泛化**；B1.5f 数字自此冻结，下一步 = **B1.5g（修上述 5 条 + 保持 `Q12` 例外）+ 新盲集 B1.5h**；#74 保持 OPEN，#81 报告完成 / PR 待建，**B-① 未开始**。

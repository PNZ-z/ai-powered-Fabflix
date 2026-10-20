# 工作流日志 — 覆盖率门槛提升到 90（后端 JaCoCo 0.85 → 0.90，前端 lines 88 → 90）

## 元数据

| 项目 | 值 |
| --- | --- |
| 完成日期 | **2026-10-20** |
| 关联 issue / PR | **无**（本次是用户直接批准的独立变更，未开 GitHub issue，**未创建 PR**） |
| 分支 | **`codex/coverage-threshold-90`**（基于 `main` @ `9696848`，即 PR #53 合并后的 `main`） |
| 提交 / 推送 | **未提交、未推送**（工作区改动：见下方 §1；`git status` 另有与本任务无关的 `?? .idea/`） |
| 参与角色 | 主 agent（批准与终验）→ 单一 `worker`（`pom.xml`/`vitest.config.js`/5 个后端测试/CI 与 README 文案）→ 3 路只读 `reviewer` → 同一 `worker` 修复 → `docs-writer`（仅 `docs/` 活文档同步 + 本日志） |
| 批准依据 | 用户在 2026-10-20 明确批准「把覆盖率门槛收紧到 90」并且作为**迁移前置的独立变更**；本日志记录的是**已批准并已实现**的状态，不含未批准的方向 |
| 变更性质 | **只收紧质量门**：不改业务行为、不加新依赖、不扩 `excludes`、不跳过任何测试 |

### 验证证据（谁跑的 / 谁复核的）

| 检查 | 命令 | 结果 | 证据来源 |
| --- | --- | --- | --- |
| 后端全量 + 覆盖率闸门 | `JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=… mvn --batch-mode --no-transfer-progress verify` | **BUILD SUCCESS**；`Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`；`All coverage checks have been met.`；JaCoCo `BUNDLE / LINE` **988 covered / 90 missed / 1078 total = 0.9165**（门槛 0.90 通过） | 主 agent / worker 实测（JDK 21）；**docs-writer 未重跑**，但按同一产物 `target/site/jacoco/jacoco.csv` 与 `target/surefire-reports/TEST-*.xml` 独立汇总复核一致（988/90/1078 = 0.9165；44 个测试类 / 134 用例 / 0 失败 0 错误 0 跳过） |
| 前端单测 | `cd frontend && npm test` | **30 files / 195 tests passed** | 主 agent / worker 实测；docs-writer **未重跑**（工作区无 `frontend/coverage` 产物可复核） |
| 前端覆盖率闸门 | `npm run test:coverage` | **Lines 92.35%（846/916）**（门槛 90 通过；Statements 90.05%、Branches 82.16%、Functions 91.17%） | 同上（主 agent / worker 实测报告） |
| lint / build / e2e | `npm run lint`、`npm run build`、`npm run test:e2e` | 全部通过；Playwright **12 passed** | 同上 |
| 三路 review | — | **无代码 Blocker**；仅提出文档一致性问题（CI 摘要文案、`README.md`、契约文档 §7、issue #44 的 AC-2、未来步骤 issue 的“门槛不变”口径、`GoogleRecaptchaVerifier` 缺口未登记） | 主 agent 汇总；本日志 §1.3 记录其闭合方式 |
| GitHub 远端 | `gh issue view <n> --json body`（只读，#44/#48/#49/#50/#51/#52） | 远端 #44/#48/#50/#51/#52 正文**仍写 `>= 0.85` / `lines >= 88`**（#49 不含门槛数字）；本轮**未做任何 GitHub 写操作** | docs-writer 只读核对，2026-10-20 |

> **未执行 / 无法确认的项**：GitHub Actions 的 CI 运行（**本轮无 PR，未跑 CI**）；Docker 生产 smoke（与本变更无关，未跑）；前端覆盖率产物的独立复算（见上表）；`main` 上的实际状态（改动尚未提交）。

---

## 1. 修改了哪些文件及潜在问题

### 1.1 源码/配置/测试（由 `worker` 实现，非本次文档任务所改）

| 文件 | 改动 | 目的 |
| --- | --- | --- |
| `pom.xml` | JaCoCo `check` 执行 `online-web-application-coverage-check` 的 `LINE / COVEREDRATIO` `minimum` **0.85 → 0.90**（`<phase>verify</phase>` 未变，`<excludes>` 仍为 8 项，`BUNDLE` 口径未变） | 把后端行覆盖率闸门收紧到 90% |
| `frontend/vitest.config.js` | `coverage.thresholds.lines` **88 → 90** | 把前端行覆盖率闸门收紧到 90% |
| `src/test/java/com/fabflix/boot/commerce/CartControllerTest.java` | **+10 行**：新增 `returnsServerErrorWhenCartLookupFails`，让 `cartService.getCart(...)` 抛异常，断言 500 且 `$.errorMessage == "Unable to process commerce request."` | 覆盖 `CommerceApiExceptionHandler` 的 **500 分支**（此前该分支未覆盖） |
| `src/test/java/com/fabflix/boot/admin/AdminApiExceptionHandlerTest.java`（新增） | `MockMvcBuilders.standaloneSetup(new AdminController(service))` + `.setControllerAdvice(new AdminApiExceptionHandler())`，`service.metadata()` 抛异常 → 断言 500 与 `$.status=fail` / `$.message` | 覆盖 admin 包的异常 advice（原本 0 覆盖） |
| `src/test/java/com/fabflix/boot/admin/EmployeeRepositoryTest.java`（新增） | mock `JdbcTemplate` + `ResultSet`，用 `thenAnswer` 把捕获到的 `RowMapper` 应用到 mock `ResultSet`；覆盖“命中”与“无结果 → `Optional.empty()`”两条路径，并 `verify` SQL 常量与绑定参数 | 覆盖员工登录查询（含 RowMapper 映射） |
| `src/test/java/com/fabflix/boot/main/GenreServiceTest.java`（新增） | mock `GenreRepository`，断言 `listGenres()` 原样返回仓库结果并校验调用 | 覆盖 `GenreService` 的 1 行委托逻辑 |
| `src/test/java/com/fabflix/boot/search/AutocompleteRepositoryTest.java`（新增） | 同样的 `thenAnswer` + `RowMapper` 模式，断言 SQL 文本 `EXPECTED_SQL`、绑定参数 `"exa*"`，以及 legacy 形状 `{value, data:{movie_id}}`；另有“无结果 → 空 List” | 覆盖 autocomplete 查询与 DTO 映射 |
| `.github/workflows/ci.yml` | step summary 文案里的 `The 85% gate applies to …` → `The 90% gate applies to …`（仅 1 行字符串） | 让 CI 看到的分支摘要与实际闸门一致（reviewer 发现的一致性问题） |
| `README.md` | “CI enforces an **85% line-coverage gate**” → **90%**（仅 1 行） | 同上：README 是加载门数字的活文档 |

**净效果**：门槛从 0.85/88 提到 0.90/90，并补齐足以守住新门槛的测试（后端用例 127 → 134，全部行覆盖 0.8980 → 0.9165）。

### 1.2 `docs/` 活文档同步（由 `docs-writer` 在本次任务中修改，14 个文件）

| 文件 | 改动的小节 | 内容 |
| --- | --- | --- |
| `docs/architecture/frontend-migration-contracts.md` | **§7**（4 处）、**§12** 表格与后端命令行 | 前端 `thresholds.lines` 88 → **90**（注明 2026-10-20 由用户批准收紧、余量 2.35pp）；后端闸门 0.85 → **0.90**，并把「968/1078 = 0.8980、127 tests、~106 行余量」整段替换为「**988/1078 = 0.9165、134 tests、余量 N ≤ 19 行**」，旧数字降级为历史证据并给出日志链接；`test:coverage` 表格行与「后端命令」行同步为 90 |
| `docs/plans/issue-44-migration-checklist.md` | 新增顶层一节 **「2026-10-20 覆盖率门槛收紧（0.90 / 90）与新的比较基线」**；S1 (b) 加失效提示；S2/S3/S5/S6 的期望值；S3 (a)；S7 的 grep 命令；维护约定新增第 5 条 | 新基线表（后端 134 / 0.9165；前端 195 / 92.35%；e2e 12）、**余量 N ≤ 19 行**、前端余量 2.35pp、**已知覆盖缺口表**（`AdminRepository` 0/39 保留原记录 + **新登记 `GoogleRecaptchaVerifier` 0/17**，并列出 `MovieListRequest`/`MovieSummaryStar`/`SearchService`/`MovieListQueryFactory`）；S1 的 127 / 0.8980 明确标注为「收紧前快照，不再作为 S3/S6 对照基线」，并引用基线记录自身的“对比规则第 6 条失效条件” |
| `docs/plans/issue-44-migration-steps.md` | §3 S1 行与 S3 行、新增「覆盖率门槛更新」提示段、§4-S3 DoD 第 1 条、§4-S6 AC-5、§5.3 AC-2 行、§7 估算假设、§10 质量闸门 | 同上口径：S3/S6 以 0.90/90 与 134/91.65%、195/92.35% 为对照；`excludes` 8 项与 e2e 12 次不变；明确「不得扩 excludes、不得降阈值、不得跳过 e2e」 |
| `docs/issues/issue-44-boot3-java17-jakarta-migration.md` | `## GitHub issue body` 的 Background 门禁句、**AC-2**、AC-5、Scope→Docs、Non-goals、Risk notes（覆盖率预算）、`## Step → acceptance mapping` 的 AC-2 行；顶部说明、`## Internal tracking` 三处“与远端逐字一致”的表述、「剩余待确认」新增第 10 项、内部备注新增一条 | AC-2 改为「门槛已于 2026-10-20 经用户批准收紧为 **0.90**（独立 PR）；本 epic 以收紧后门槛验收；**不放松任何质量门**」，其余 AC 结构与文字保留；**明确记录本地 body 与远端已暂时不一致**（远端仍 0.85/88，需 `github-operator` 授权编辑） |
| `docs/issues/issue-48-core-boot3-jakarta.md` | “Quality gates must not regress” 行、Non-goals 的覆盖率句、AC-7 | 0.85 / 968-0.8980 / 127 → **0.90 / 988-0.9165 / 134**（旧值保留为收紧前快照）；“No coverage-threshold change” 改为「**门槛已在迁移前收紧为 0.90 / 90；本步骤不得降低或绕过**」；AC-7 的“no threshold change” 改为“no lowering of the 0.90/90 thresholds” |
| `docs/issues/issue-49-jdk21-toolchain.md` | Non-goals | 「no coverage-gate changes」→「**不得降低或绕过**已在 2026-10-20 收紧的 0.90 / 90 门槛」 |
| `docs/issues/issue-50-mockito-annotation-cleanup.md` | Background 的闸门句、Internal tracking 事实来源行 | `BUNDLE/LINE >= 0.85` → **0.90**（注明收紧日期与不得绕过）。**说明**：任务清单只点名 #48/#49/#51/#52，此处属同一口径的补漏（否则 S5 正文仍写 0.85），已在交付报告中标注 |
| `docs/issues/issue-51-regression-proof.md` | Background 的参考值块、Non-goals、AC-2 | 参考值改为收紧后 **134 / 0.9165（0.90）** 与 **92.35%（90）**；2026-10-15 的 127 / 0.8980 / 88 明确降级为“history only，不再是比较基线”；`no threshold change` → `no lowering or bypassing` |
| `docs/issues/issue-52-close-out.md` | 已知陈旧点第 4 条、Scope 的 §7 项、Non-goals | 「§7 余量句陈旧」改为「**已于 2026-10-20 随门槛收紧更新**，现为 90 行未覆盖 / 余量 ≈19 行」，本步骤改为**复核**而非首改；Non-goals 明确不得降低/绕过 0.90 / 90 |
| `docs/plans/ai-option-a-boot3-migration.md` | §总结（134 用例 / 0.90 闸门）、§1.1 第 3 条、§1.2 插件行、§6.1 AC-A-1/AC-A-2/AC-A-5、§8 简历价值行 | 所有“当前门槛”表述改为 **0.90 / 90** 并注明 2026-10-20 收紧；AC-A-1 基线由 127 改为 **134**（127 保留为收紧前） |
| `docs/plans/ai-roadmap-overview.md` | §1.3 完成定义、§2.5 测试与质量闸门（后端/预算/前端三条）、C-5 质量闸门约束、§6 M3 行、§12 事实表两行、顶部修订记录 B3 注 | 门槛改 0.90/90；**覆盖率余量口径由 ≈60 行重算为 ≈19 行**（`988/(1078+N) ≥ 0.90`），旧 ≈60 行与更早的“≈52 行”均标注失效但保留为对照 |
| `docs/plans/ai-option-0-local-llm-spike.md` | 事实表两行、§4.4 覆盖率阈值、§7 功能验收第 3 条 | 85%/88% → **0.90/90**（注明收紧日期）+ 后端测试数 127 → 134 + 余量 ≈19 行 |
| `docs/plans/ai-option-b-spring-ai-rag.md` | 事实表闸门行、§5 表格覆盖率行、§6.1 AC-B-1、§7 覆盖率预算 | 同上改为 0.90/90（含 134 / 0.9165 现状值），AC-B-1 明确“不得降低或绕过” |
| `docs/plans/ai-option-c-agent-platform.md` | §1.2 事实表闸门行 | 0.85 / 0.8980 / 88 → **0.90 / 0.9165 / 90** |
| `docs/logs/2026-10-20-coverage-threshold-90.md`（本文件，新增） | — | 本次工作流日志 |

**明确未改动（历史快照 / 已关闭 issue）**：`docs/logs/2026-10-14-*`、`docs/logs/2026-10-15-*`、`docs/logs/2026-10-20-issue-46-baseline.md`、`docs/plans/issue-30/31/32/33-*.md`、`docs/issues/issue-30/31/32/33-*.md`、`docs/issues/issue-46-baseline.md`、`docs/issues/issue-47-dependency-readiness.md`（该文件只以“与基线一致”表述、不含门槛数字）。这些文件里的 `0.85 / 88 / 968-0.8980` 是**当时的事实**，按 AGENTS 的“历史记录不追改”原则保留。

### 1.3 review 提出的文档一致性问题 — 闭合方式

| 问题 | 处理 |
| --- | --- |
| `ci.yml` 摘要文案仍写 85% | ✅ 已由 worker 改为 90%（`.github/workflows/ci.yml`，1 行字符串） |
| `README.md` 仍写 85% | ✅ 已由 worker 改为 90%（1 行） |
| `docs/architecture/frontend-migration-contracts.md` 的 0.85 / 88 | ✅ 本轮 §1.2 已同步 |
| `docs/issues/issue-44-*.md` 的 AC-2 “coverage gate unchanged ≥0.85” | ✅ 本地 body 已改为“已收紧为 0.90、以收紧后门槛验收、不放松任何质量门”；⚠️ **远端未改**（见 §1.4） |
| 未来步骤 issue（#48/#49/#51/#52，另加 #50）的 “no coverage-threshold change” | ✅ 改为“门槛已在迁移前收紧为 0.90/90；本步骤不得降低或绕过”，范围/验收未变 |
| `GoogleRecaptchaVerifier`（0/17）未登记为已知缺口 | ✅ 已登记进 checklist 的新基线节，并注明 `@Profile("!local")` 且测试从未实例化它（`CustomerAuthControllerTest` 注入的是 `RecaptchaVerifier` 接口 mock；`LocalRecaptchaVerifierTest` 只测 local 实现） |

### 1.4 残余风险、边界限制与未验证项

1. **改动未提交/未推送、无 PR**：`codex/coverage-threshold-90` 上的 5 个改动文件 + 4 个新文件仍在工作区；`main` 上**还没有** 0.90 门槛，也**没有**本轮 `docs/` 的同步内容。任何“已生效”的说法在合并前都不成立。
2. **远端 GitHub 与本地文档暂时不一致（已逐个核对）**：docs-writer 于 2026-10-20 用 `gh issue view <n> --json body`（**只读**）逐个核对：#44、#48、#50、#51、#52 的**远端正文仍写着 `>= 0.85` / `lines >= 88` / `0.85 gate`**，而本地 `docs/issues/` 对应文件已改为 0.90 / 90；#49 的正文本身不含门槛数字（只有 “no coverage-gate changes”，本地已改为“不得降低或绕过”），因此 #49 的偏差仅为那一句表述。**恢复一致需要主 agent 授权 `github-operator` 编辑这 5 个 issue**。docs-writer **未做任何 GitHub 写操作**。
3. **后端口径余量只剩 ≈19 行未覆盖代码**：`988/(1078+N) ≥ 0.90 ⇒ N ≤ 19`。新增生产代码若不带测试，会很快把闸门打红；**不得扩 `excludes`（仍 8 项）**，否则等于绕过本次批准。
4. **门槛只在特定命令下生效**：后端闸门绑定在 `verify` 阶段（`mvn verify`/`mvn install` 才跑 `jacoco:check`，`mvn test` **不检查**）；前端阈值只在 `npm run test:coverage`（`vitest run --coverage`）生效，`npm test` **不检查**。因此“本地 `npm test` 绿”不能作为通过闸门的证据。
5. **覆盖率数字仍受 `excludes` 白名单保护**：BUNDLE 口径只统计未被 exclude 的类（8 项：`*XMLParser`、`*InsertWorker`、`*InsertTask*`、`Update*SecurePassword`、`CreateFullTextIndex`、`DatabaseEnvironment`、`Movie`、`dev/CreateLocalCustomer*`）；把这些一次性离线工具算进来会显著拉低比例，本次**没有**改动白名单。
6. **已知覆盖缺口（本次不处理，非回归）**：`AdminRepository` 0/39、`GoogleRecaptchaVerifier` 0/17、`MovieListRequest` 38/52、`MovieSummaryStar` 6/12、`SearchService` 26/30、`MovieListQueryFactory` 45/48（`target/site/jacoco/jacoco.csv`，2026-10-20）。其中 `GoogleRecaptchaVerifier` 是“**结构性**无法被当前测试触达”（`@Profile("!local")` + 无 Spring 上下文测试基座），补测需要在非 local profile 的切片里显式 `new` 它或用 `MockRestServiceServer`，属独立任务。
7. **不再有“安全垫”**：门槛从 0.85 提到 0.90 用掉了原有 ≈60 行余量中的大部分；后续 `#48`（Boot 3 迁移）等步骤一旦新增未覆盖代码就会立刻失败——这正是把基线写入 checklist 的目的，但也是本变更对后续步骤的**硬约束**。
8. **S1 基线记录与新基线并存**：`docs/logs/2026-10-20-issue-46-baseline.md` 作为历史快照**未被改写**（127 / 0.8980），因此任何读者若只看该文件会得到旧门槛口径；已在 checklist 的 S1 节与「2026-10-20 …新的比较基线」节交叉标注，但**旧文件的“唯一基准”措辞未被追改**（按历史记录不改原则）。
9. **前端覆盖率数字未由 docs-writer 独立复算**：工作区没有 `frontend/coverage` 产物，92.35%（846/916）来自主 agent / worker 的实测报告；后端数字已由 `jacoco.csv` + surefire XML 独立复核。
10. **`docs/` 中的 issue 正文改动是“发布草稿”**：按 AGENTS，`github-operator` 以本地文档为准发布；本轮改动**尚未发布**，因此 GitHub 上的 #44/#48–#52 与本仓库文档存在已知偏差，需在发布/编辑后复核。

**未发现其它已知遗留问题**（除上述 10 条外，本轮 `docs/` 改动未触及源码、测试、配置、`AGENTS.md` 或 `.pi/agents/`）。

---

## 2. 新增知识点 / 重点学习对象

### 2.1 构建工具 / JaCoCo

- **`BUNDLE` + `LINE` + `COVEREDRATIO` 的准确含义**（`pom.xml` 的 `jacoco:check`）
  - `element=BUNDLE` 表示“把整个模块所有（未被 exclude 的）类当作**一个整体**统计”，不是“每个类都要达标”，也不是“各测试类结果的平均”。`counter=LINE` 只统计可执行的**行**指令；`value=COVEREDRATIO` = covered / (covered + missed)。
  - 所以本项目实测 988 covered / 90 missed / 1078 total 是**汇总值**：单个类 0% 也能通过，只要整体 ≥ 0.90。
  - 值得学习：这决定了“补测试”的策略——补一个 0 覆盖的大类（如 `AdminRepository` 39 行）能一次性抬高整体比例，而给已覆盖的类加断言**完全不改善**比例。
- **`<excludes>` 是“缩小分母”而不是“提高覆盖率”**：本项目的 8 项 exclude（`*XMLParser`、`*InsertWorker`、`*InsertTask*`、`Update*SecurePassword`、`CreateFullTextIndex`、`DatabaseEnvironment`、`Movie`、`dev/CreateLocalCustomer*`）都是**不在请求路径上**的一次性离线工具；把在线应用类加进来就等于绕过闸门（这正是“不得扩 exclude”的由来）。
- **闸门绑定在生命周期阶段**：`prepare-agent` → `report`（verify）→ `check`（verify）。因此 `mvn test` 不会跑 `check`，`mvn verify` 才会失败。**“跑哪个命令”决定了闸门是否真的生效**——CI 的唯一后端命令是 `mvn --batch-mode --no-transfer-progress verify`，所以 CI 是有效的，本地 `mvn test` 不是。
- **阈值收紧后的余量计算**（可复用的算术）：设现覆盖 `c`、总数 `t`、新阈值 `θ`，新增未覆盖行 `N` 的上限是 `N ≤ c/θ − t`。本例 `988/0.90 − 1078 = 19.8 ⇒ N ≤ 19`。同一公式在旧门槛下是 `968/0.85 − 1078 = 60.8`——这就是“门槛提高把安全垫从 ≈60 行压到 ≈19 行”的来源。做这类变更前**先用这个公式算一遍**，才能判断要不要同时补测试。
- **前端同样的算术 + provider 差异**：`@vitest/coverage-v8` 的 `thresholds.lines` 是**百分比**（92.35 ≥ 90 通过），`include: ['src/**/*.{js,jsx}']` 让**未被任何测试导入**的文件（如 `src/App.jsx`）也进分母（本仓库 `App.jsx` 行覆盖 0% 却仍在分母里），这是防止“只测好测的模块”的关键口径。Branches 82.16% **没有**阈值——所以“coverage 绿”不等于分支都被测到。

### 2.2 测试技巧 / Mockito

- **用 `thenAnswer` 驱动被捕获的 `RowMapper`（无数据库测 JDBC 仓储）**
  - 模式（见 `EmployeeRepositoryTest`、`AutocompleteRepositoryTest`）：
    ```java
    when(jdbcTemplate.query(eq(SQL), any(RowMapper.class), any(Object[].class)))
        .thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(1);          // 第二个参数就是仓储内部传进来的 mapper
            return List.of(mapper.mapRow(resultSet, 0));        // 让 mock ResultSet 走真实映射逻辑
        });
    ```
  - 价值：`JdbcTemplate.query(sql, rowMapper, args)` 的真实 mapper 是**仓储里的私有 lambda/匿名类**，直接拿不到；用 `thenAnswer` 反向“回调”它，就能在**没有 MySQL** 的情况下覆盖 RowMapper 的列读取与 DTO 构造（这正是这两个类原本 0 覆盖的原因）。
  - 配套细节：`any(RowMapper.class)` 会与 `Class<T>` 泛型打架 → 用 `@SuppressWarnings({"unchecked","rawtypes"})`；`ResultSet` 的 `getString("col")` 要按**列名** stub，且 `mapRow` 抛 `SQLException`，测试方法需 `throws Exception`；最后用 `verify(jdbcTemplate).query(eq(SQL), any(RowMapper.class), eq("arg"))` 断言 SQL 常量与绑定参数**没有被悄悄改掉**（比断言返回值的保护面更大）。
- **`standaloneSetup` 不会自动注册 `@ControllerAdvice`**
  - `AdminApiExceptionHandlerTest` 用 `MockMvcBuilders.standaloneSetup(new AdminController(service)).setControllerAdvice(new AdminApiExceptionHandler())`。**不写 `.setControllerAdvice(...)` 时异常会直接抛给测试（或变成 500 但无 JSON body）**，断言 `$.status=fail` 必然失败。
  - 对比：`@WebMvcTest(SomeController.class)` 会扫描并注册 advice，所以同一个 handler 在切片测试里“看起来是好的”。要覆盖异常处理本身，standalone 必须**显式**加 advice（与文档里“standalone 默认没有 filter，需要 `.addFilters(...)`”是同一条陷阱的两种形态）。
- **用“故障注入”补异常分支**：`CartControllerTest` 的 500 用例只有 3 行核心代码（`when(cartService.getCart(any())).thenThrow(...)` + 两条断言），却是把 `CommerceApiExceptionHandler` 从 0 覆盖拉起来的最短路径。**补覆盖率的性价比往往在“异常/空值分支”而不是再写一条 happy path。**

### 2.3 概念 / 流程

- **“收紧质量门”与“历史记录不追改”的边界**：同一份仓库里，`docs/logs/2026-10-15-*`、`2026-10-20-issue-46-baseline.md` 里的 0.85 / 968-0.8980 是**当时事实的证词**，改写会破坏审计链；而 `docs/architecture/*`、`docs/plans/issue-44-*`、未来步骤的 `docs/issues/issue-4x/5x-*` 是**当前约束**，必须改成 0.90 否则会误导实施者。判断规则：**“这条数字是给未来的人当约束用，还是给未来的读者当证据用？”**——前者改，后者留，并在留存处标注“收紧前快照”。
- **基线（baseline）会因“环境/门槛变化”而失效**：S1 的基线记录自己写了“若在 S3 前改动 `pom.xml` 或测试数量，本基线失效，须由主 agent 决定是否重取”。本次正好同时触发了两个条件（阈值 + 127→134 用例）。正确做法不是删掉旧基线（它是证据），而是**新增一份生效的基线**并在 checklist 里指定“S2–S7 的对照基准改为 0.90 / 134 / 0.9165”，同时说明为何旧基线退役。这是“文档状态机”的典型操作。
- **“本地 body 与远端 issue 逐字一致”是一个需要主动维护的断言**：`docs/issues/issue-44-*.md` 顶部原本声称与远端逐字一致；本轮改了 body 但**没有权限改远端**，所以必须显式加“⚠️ 2026-10-20 起不再一致 + 待 `github-operator` 编辑”的注记，否则下一个读者会把本地文档当成远端事实。**任何“与远端一致”的表述都要有对应的核对日期和核对手段（`gh issue view`）。**
- **`@Profile("!local")` 的类为什么测不到**：本仓库**没有任何 `@SpringBootTest`**（只有 `@WebMvcTest` 切片与 `standaloneSetup`），切片只装配 controller 层所需的 bean，像 `GoogleRecaptchaVerifier` 这种“普通 `@Component` + 条件 profile”的类既不会被切片加载、也没有单测 `new` 它 → 结构性 0 覆盖。识别这类缺口要问两个问题：**有没有 Spring 上下文把 bean 装配起来？有没有测试直接构造它？** 两者都没有，就是永久缺口，应登记而不是靠“再跑一次覆盖率”解决。

---

**结论**：本次变更把后端/前端覆盖率门槛收紧到 **0.90 / 90**，用 5 个后端测试文件把后端行覆盖从 0.8980 抬到 **0.9165（134 tests）**，前端保持 **92.35%**；`docs/` 的 14 个活文档已同步到新门槛并建立新基线，历史日志与已关闭 issue 文档按要求未追改。**未提交、未推送、无 PR，远端 GitHub 未同步**——这些是留给主 agent / `github-operator` 的后续动作。

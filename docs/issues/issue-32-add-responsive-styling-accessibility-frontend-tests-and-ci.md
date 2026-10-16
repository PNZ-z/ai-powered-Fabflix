# Issue #32 — Add responsive styling, accessibility, frontend tests, and CI（Issue 跟踪文档）

> **状态：已在 GitHub 存在，无需新建。** 本文档记录远端 issue 的编号/URL/标题/正文原文与本地计划范围；
> 它**不是**发布草稿，`github-operator` **不需**为本 issue 创建新 issue。docs-writer 不修改远端 issue 内容。
>
> **本地计划状态（2026-10-15）：`docs/plans/issue-32-add-responsive-styling-accessibility-frontend-tests-and-ci.md` = 已批准（主 agent，2026-10-15，依据 oracle 报告）→ 实现完成，已通过主 agent 终验（2026-10-15）。**
> **实现状态：已完成并已合并（分支 `codex/issue-31-32-33-m2-completion`，HEAD `fb65eda`；已提交 `092147a`/`fb65eda`、已推送、**PR #42 已于 2026-10-16 合并**（merge commit `d618b64`）；**issue #32 已关闭**（GitHub 依 PR 正文的 `Closes #32` 自动关闭）；合并后远端分支已删除，`main`（`d618b64`）CI 双 job SUCCESS）。**
> **验证状态：前端 `npm test` → 30 files / 195 passed；`npm run test:coverage` → Lines 92.35%（846/916，阈值 88）、Statements 90.05%、Branches 82.16%、Functions 91.17%；`npm run lint` 干净；`npm run build` 成功；`npm run test:e2e` → 12 passed（chromium，360/1440）；Node 22 容器连续两次一致。后端 `mvn verify` 回归 → 127 tests / LINE 0.8980。提交/推送/合并：`092147a`/`fb65eda`、已推送、**PR #42 已合并（`d618b64`，2026-10-16）**；**head `fb65eda` 上 `Build and test (Java 11)` 与 `Frontend (lint, test, coverage, build, e2e)` 均 SUCCESS，合并后 `main`（`d618b64`）上双 job 亦 SUCCESS**。** AC 逐条状态与证据见 §6。

## 1. 远端元数据

| 项目 | 值 |
| --- | --- |
| 编号 | 32 |
| URL | https://github.com/PNZ-z/ai-powered-Fabflix/issues/32 |
| 标题（远端原文） | Add responsive styling, accessibility, frontend tests, and CI |
| Milestone | Phase 2 — React Frontend Migration（与 #31/#33 共用；由主 agent 提供） |
| 获取方式 | 主 agent 用 `gh` 获取的远端原文，并在任务说明中逐字提供（docs-writer 无 GitHub 访问授权，**未**直接查询远端） |
| 远端状态核对 | **已关闭（`CLOSED`）**——2026-10-16 由 GitHub 依 PR #42 正文的 `Closes #32` 自动关闭（主 agent 核验）；docs-writer 未直接查询远端，仍以远端为准 |
| 本地记录文档 | 本文件 |
| 关联计划 | `docs/plans/issue-32-add-responsive-styling-accessibility-frontend-tests-and-ci.md`（**已批准（主 agent，2026-10-15）→ 实施完成，已通过主 agent 终验（2026-10-15）**；实现结果/实际阈值/差异见其 §13） |
| 关联全局约束 | `docs/architecture/frontend-migration-contracts.md`（§7 测试设施与 CI 闸门：**已由本 issue 实施后更新**，新增 §11 前端测试设施与 Node 22 CI job 约定） |
| 实现顺序 | #31 → **#32** → #33；#32 可与 #31 部分并行，但「前端测试覆盖 + CI 跑前端」须在 #31 新页面并入后完整 |

> ⚠️ 以上元数据来自主 agent 提供的远端原文；docs-writer **未**直接读取远端。如与远端当前状态不符，以远端为准并由主 agent 更正。

## 2. 远端 issue 正文原文（逐字保留，供对照）

```markdown
## Goal
Give the migrated React application production-quality user experience and automated quality gates.

## Scope
- Create consistent responsive styles and reusable design tokens
- Add semantic markup, labels, focus states, and keyboard interaction
- Add unit/component tests with API mocking
- Add critical browser-level tests for login, browsing, cart, and employee flows
- Run lint, tests, coverage, and production build in GitHub Actions

## Acceptance criteria
- [ ] Core pages work at desktop and mobile widths
- [ ] Forms and navigation are keyboard usable
- [ ] Automated accessibility checks cover key pages
- [ ] Frontend test and coverage commands run deterministically
- [ ] CI blocks regressions in linting, tests, coverage, or build

## Dependency
Can progress alongside page migration, but completes after all React flows exist.
```

## 3. 用户/主 agent 已批准的范围决定（视为批准范围内）

| 编号 | 已批准决定 | 对应 issue 正文 |
| --- | --- | --- |
| D-32-1 | 引入 **Playwright（仅 chromium）** 做关键浏览器级测试：login / browsing / cart / employee 四条 smoke；API 用 `page.route` mock，**不依赖 MySQL**；CI 新增 `npx playwright install --with-deps chromium` + `npm run test:e2e`；新增相应 devDependencies。 | scope 第 4 条；AC 第 4 条 |
| D-32-2 | 引入 **`@vitest/coverage-v8`** + `test:coverage` 脚本；**先测实际基线**，阈值 = 基线下方 2–5 个百分点且**行覆盖 ≥75%**；CI 阻断（lint / unit tests / coverage / production build / e2e）。 | scope 第 5 条；AC 第 5 条 |
| D-32-3 | **Node 22 LTS** 固定用于 CI 与 Docker（`actions/setup-node`、Dockerfile（#33）；可在 `frontend/package.json` 加 `engines`）。 | scope 第 5 条 |
| D-32-4 | 响应式与无障碍按 scout B1/B2 缺口补齐：design tokens 继续以 `index.css` `:root` 为**单一来源**（不新建设计系统）；全局 `:focus-visible`；关键页面（home/login/movies/cart/checkout/confirmation/employee）在 **360–1440px** 检查与修复；自动化 a11y 检查引入 **`vitest-axe`（或等价、无浏览器依赖方案）** 覆盖关键页面——**若引入该依赖需在计划中写明并已获批准**。**→ 2026-10-15 裁定：不采用 `vitest-axe`，改用 `axe-core` 直连 + 自建 helper（已批准）**。 | scope 第 1、2 条；AC 第 1、2、3 条 |
| 顺序与交付 | 一个分支 `codex/issue-31-32-33-m2-completion`、一个 PR（`Closes #31`/`#32`/`#33`）；顺序 #31 → #32 → #33；每阶段都要跑相应测试。 | issue 正文未规定；由主 agent 传达 |

> **计划批准状态（2026-10-15）**：主 agent 依据 oracle 报告（B1/B2 已闭合，N-32-1..N-32-12 与 Q-1..Q-10 全部裁定）**批准了完整计划**，**未改变 AC-32-1..AC-32-5**。
> **新增依赖已批准（共 3 个 devDependencies）**：`@playwright/test`、`@vitest/coverage-v8`、`axe-core`（+ Playwright 浏览器下载）；**不引入 `vitest-axe`**。
> 其他已定稿要点：**B1** `vitest.config.js` 显式 `test.exclude` 加入 `**/e2e/**`（保留默认排除项）；**B2** a11y 用 `axe-core` + 自建 helper `frontend/src/test/axe.js`；独立 **`frontend` CI job**（`timeout-minutes: 20`）；`engines: { "node": ">=22.12.0" }`；**本 issue 重新生成并提交 `frontend/package-lock.json`**；覆盖率先测再定阈值（行 ≥75%，基线下方 2–5 点）；**架构文档 §7 两条事实由 #32 更新**。
> **已回填（实施终验）**：**覆盖率阈值定稿为 `lines: 88`（实测行覆盖 92.35% = 低于基线 4.35pp、远高于 75% 下限）**。
> **已回填（2026-10-15 PR/CI；2026-10-16 合并）**：CI 配置已落地，且**CI 首跑已发生**——PR **#42**（head `fb65eda`）上 `Frontend (lint, test, coverage, build, e2e)` → **SUCCESS**（新 job 首次运行），`Build and test (Java 11)` → **SUCCESS**；**PR 已于 2026-10-16 合并（`d618b64`），合并后 `main` 上双 job 仍 SUCCESS。**

## 4. 范围（依据 issue + 已批准决定）

### 4.1 In scope

- 一致的响应式样式与可复用 design token（token 单一来源 = `frontend/src/index.css` `:root`）。
- 语义化标记、label、焦点态（全局 `:focus-visible`）、键盘交互。
- 带 API mock 的单元/组件测试。
- 关键浏览器级测试（login / browsing / cart / employee）。
- GitHub Actions 中的 lint / 测试 / 覆盖率 / 生产构建（+ e2e）。
- Node 22 固定（CI 与 Docker）。

### 4.2 非目标（Non-goals）

- 新建设计系统 / CSS-in-JS / Tailwind / 组件库（D-32-4 已批准「以 `index.css` `:root` 为单一来源」）。
- 跨浏览器 e2e（仅 chromium，D-32-1）；e2e 直连 MySQL 或真实后端（D-32-1：用 `page.route` mock）。
- 浏览器视觉回归（截图对比）。
- 后端 API / 契约 / DB schema 的任何改动。
- 生产构建接入 Spring Boot、SPA fallback、Docker（→ #33）。
- 删除 legacy `src/main/webapp/**`（→ #33，且为已批准的破坏性动作）。
- 修改 JaCoCo 后端 85% 闸门或其 excludes 白名单。
- i18n / 多语言文案。

## 5. 依赖

| 依赖 | 现状 | 证据 |
| --- | --- | --- |
| 既有 React 页面（home/login/movies/cart/checkout/confirmation） | 已完成（#28/#29/#30/#34-#39） | `frontend/src/features/**`（26 个测试文件，docs-writer 实测） |
| 员工页面（employee） | **依赖 #31** | 四条 e2e smoke 中的 employee，以及关键页面 a11y 覆盖 |
| 前端 API mock 能力 | 已有（组件测试级） | `vi.stubGlobal('fetch', ...)`；`src/test/setup.js` |
| Playwright 浏览器二进制 | **无**（需新增依赖 + CI 安装步骤） | `frontend/package.json` 无 Playwright（已 grep：0 命中） |
| 覆盖率工具 | **无**（`@vitest/coverage-v8` 未安装） | `frontend/vitest.config.js` 无 `coverage` 段 |
| Node 版本固定 | **无**（`frontend/package.json` 无 `engines`） | docs-writer 复核 |

## 6. 验收条件与跟踪（**状态：全部已完成（主 agent 终验 2026-10-15）**；AC-32-5 的 CI 证据已随 PR #42 首跑回填）

| # | 验收条件（远端原文） | 承载方案（计划） | 状态与证据 |
| --- | --- | --- | --- |
| AC-32-1 | Core pages work at desktop and mobile widths | `index.css` 断点补齐（1024/760 + 新增 480）+ 关键页面 360–1440px 修复；Playwright viewport smoke（仅 360/1440） | ✅ **已完成**：`npm run test:e2e` → **12 passed**（chromium；`expectNoHorizontalOverflow` 断言）；生产 smoke 下 10 条深链均 200 text/html |
| AC-32-2 | Forms and navigation are keyboard usable | 全局 `:focus-visible` + 语义/label 补齐；Playwright 键盘路径 | ✅ **已完成**：`:focus-visible`（`outline: 2px solid var(--accent)`）；e2e 键盘用例 2 条（login `keyboard.type/Tab/Enter`、browsing `Tab`+`Enter`）；组件测试的 label/role 断言全绿 |
| AC-32-3 | Automated accessibility checks cover key pages | **`axe-core` 直连 + `frontend/src/test/axe.js` 的 `expectNoA11yViolations(container)`**（Q-1 裁定，不引入 `vitest-axe`）对 **9 个具体页面**断言 + App shell 的 landmark 断言 | ✅ **已完成**：9 个页面测试文件各含 1 条 axe 断言（Home/Login/MovieList/MovieDetail/Cart/Payment/Confirmation/员工登录/员工仪表盘）＋ `App.test.jsx` 2 条 shell 断言；**jsdom 下 `color-contrast` 全局禁用、页面级渲染禁用 `region`（shell 级启用）→ 对比度仍需手工确认** |
| AC-32-4 | Frontend test and coverage commands run deterministically | `@vitest/coverage-v8` + `test:coverage`、Playwright `test:e2e`（仅 chromium + mock API）、Node 22 固定；**确定性 = 连续两次用例数与覆盖率一致** | ✅ **已完成**：本地 Node 24：195 passed / 30 files、Lines **92.35% (846/916)**、lint 0、build 成功、e2e 12 passed；**Node 22 容器（`node:22-slim` v22.23.3）连续两次一致**：lint 0、195/30、Lines 92.35%、build 成功、e2e 12 passed |
| AC-32-5 | CI blocks regressions in linting, tests, coverage, or build | `.github/workflows/ci.yml` **新增独立 `frontend` job**（lint/test/coverage/build/e2e，`timeout-minutes: 20`）；后端 `mvn verify` 不弱化 | ✅ **已完成**：`frontend` job 已写入 ci.yml（步骤 `npm ci → lint → test → test:coverage → build → npx playwright install --with-deps chromium → test:e2e`）；**PR #42（head `fb65eda`）首次运行 → `Frontend (lint, test, coverage, build, e2e)` SUCCESS，同行后端 `Build and test (Java 11)` 亦 SUCCESS**（来源：主 agent 核验的 GitHub CI 结果，docs-writer 未直接访问 GitHub）；**PR 已于 2026-10-16 合并（`d618b64`），合并后 `main` 上双 job 仍 SUCCESS。** |

> **证据要求（已满足）**：AC 对应的实际命令与结果已填入上表（均为**主 agent 亲跑**：本地 Node 24 + Node 22 容器）；docs-writer 只读复核了配置/测试源码与 diff。
> **事实（已更新，2026-10-16）**：改动已提交（`092147a`/`fb65eda`）、已推送，**PR #42 已于 2026-10-16 合并**（merge commit `d618b64`）；**CI 首跑（head `fb65eda`）两 job 均 SUCCESS，合并后 `main`（`d618b64`）上双 job 亦 SUCCESS**；**issue #32 已由 GitHub 依 PR 正文的 `Closes #32` 自动关闭**（`CLOSED`）；远端分支已删除；里程碑 Phase 2（#2）已关闭（10/10）。
> **已知限制**：① `color-contrast` 无法在 jsdom 自动验证 → 新增暗色色值按 ≥4.5:1 选择，仍需**人工浏览器确认**；② `frontend/src/App.jsx` 行覆盖 **0%**（计入分母，不排除）；③ e2e 全为 `page.route` mock，不代表真实后端/MySQL 往返；④ `oxlint src` **不覆盖** `e2e/`（e2e 未纳入 lint）。

## 7. 关联记录

| 类型 | 位置 | 说明 |
| --- | --- | --- |
| 计划 / 方案 | `docs/plans/issue-32-add-responsive-styling-accessibility-frontend-tests-and-ci.md` | **已批准（主 agent，2026-10-15）→ 实施完成，已通过主 agent 终验（2026-10-15）**；B1/B2 已闭合，N-32-1..N-32-12 与 Q-1..Q-10 已全部裁定；实现结果与差异（实测覆盖率/阈值 88、双 axe helper、e2e 用例计数）见其 **§13** |
| scout 调查报告 | `.pi/artifacts/scout-issues-31-32-33.md` | §B（样式体系、无障碍现状、测试设施、CI、覆盖率工具） |
| 架构约束 | `docs/architecture/frontend-migration-contracts.md` | §4 既有模式、**§7 测试设施与 CI（已更新）**、**§11 SPA fallback（已更新）**、§10 AD-1/AD-2 |
| 工作流日志 | `docs/logs/2026-10-15-issues-31-32-33-m2-completion.md` | **已创建**（三个 issue 共用一份；含实际的 axe 规则边界、覆盖率口径与 Node 22 证据） |
| 远端 issue | #32（见 §1） | **不由 docs-writer 修改**；该 issue 已由 GitHub 依 PR #42 的 `Closes #32` 自动关闭（`CLOSED`，2026-10-16）；PR #42 合并与 CI 结果已回填（本 §7 的 PR 行与计划 §13.4） |
| PR | **#42** — https://github.com/PNZ-z/ai-powered-Fabflix/pull/42 | **已合并（`MERGED`，2026-10-16；merge commit `d618b64`，`mergedAt` = 2026-10-16T04:57:00Z）**：分支 `codex/issue-31-32-33-m2-completion`（HEAD `fb65eda`）→ base `main`；`closingIssuesReferences` = #31/#32/#33，并**据此自动关闭本 issue**；**CI 首跑与合并后 `main` 上两 job 均 SUCCESS**；远端分支已删除 |

## 8. 备注

- 本文件**不发布到 GitHub**；远端 issue 内容不由 docs-writer 修改。
- **2026-10-15（创建时）**：本文件与关联计划由 docs-writer 首次创建；**当时的计划状态 = 待 oracle 审查 / 未批准**；实现未开始、未运行任何测试。**当前状态见下一行的本轮批准记录。**
- **2026-10-15（本轮）**：oracle 只读审查完成（报告 `.pi/artifacts/oracle-issues-31-32-33-plans.md`；结论：#32 需修订后可批准，B1/B2 必须闭合）→ **B1/B2 已按最小修复写入计划；主 agent 批准计划并全部接受 N-32-1..N-32-12/Q-1..Q-10**。
- **2026-10-15（实施终验）**：worker 完成 #32 实现并由同一 worker 处理三路只读审查意见（无 Blocker）后复验；**主 agent 亲跑终验**：本地 Node 24 与 Node 22 容器分别跑通 lint/test/coverage/build/e2e（**195 tests / 30 files**、**Lines 92.35%**、**e2e 12 passed**，容器两次结果一致）；后端回归 `mvn verify` 127 tests / LINE 0.8980。实现细节与差异见计划 §13。
- **2026-10-15（提交 / PR / CI 首跑）**：实现提交 `092147a`、文档提交 `fb65eda`（HEAD `fb65eda`）已推送；**PR #42 已创建**；**CI 首跑两个 job 均 SUCCESS** —— `Frontend (lint, test, coverage, build, e2e)`（本 issue 新增 job 的首次运行）与 `Build and test (Java 11)`。来源：主 agent 核验的 GitHub CI 结果（docs-writer 未直接访问 GitHub）。详见计划 §13.4。
- **2026-10-16（合并 / issue 关闭 / 里程碑关闭）**：**PR #42 已合并**（merge commit `d618b64`；`mergedAt` = 2026-10-16T04:57:00Z）；**issue #32 由 GitHub 依 PR 正文的 `Closes #32` 自动关闭**（`CLOSED`）；远端分支 `codex/issue-31-32-33-m2-completion` 已删除（origin 仅剩 `main`）；本地 `main` 已同步到 `d618b64`；**合并后 `main` 上 CI 双 job（Java 11 + Frontend）SUCCESS**；**里程碑 `Phase 2 — React Frontend Migration`（#2）已关闭**（`open_issues` = 0、`closed_issues` = 10、`closed_at` = 2026-10-16T05:00:27Z）。来源：主 agent 核验的 GitHub 结果（docs-writer 未直接访问 GitHub）。
- **文档一致性（已处理）**：架构文档 §7 原写的「**没有** Playwright / Cypress / Selenium 等 e2e 设施」与「CI 不跑前端（属手动验证门）」**已由本 issue 更新**（Q-7）——现该文档 §7 记录 vitest/Playwright/axe/覆盖率阈值现状，并新增 §11 约定。
- **工作区提醒（已更新）**：`frontend/package-lock.json` 已由本 issue 重新生成并随提交 `092147a` 提交；`.idea/*`（6 个）**提交后仍为未跟踪的本机 IDE 文件**（非本次变更），是否纳管待主 agent 决定。
- docs-writer 无 GitHub 访问授权；§1 元数据来自主 agent 用 `gh` 获取的原文。

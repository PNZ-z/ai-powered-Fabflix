# Issue #31 — Migrate employee dashboard（Issue 跟踪文档）

> **状态：已在 GitHub 存在，无需新建。** 本文档记录远端 issue 的编号/URL/标题/正文原文与本地计划范围；
> 它**不是**发布草稿，`github-operator` **不需**为本 issue 创建新 issue。docs-writer 不修改远端 issue 内容。
>
> **本地计划状态（2026-10-15）：`docs/plans/issue-31-migrate-employee-dashboard.md` = 已批准（主 agent，2026-10-15，依据 oracle 报告）→ 实现完成，已通过主 agent 终验（2026-10-15）。**
> **实现状态：已完成（分支 `codex/issue-31-32-33-m2-completion`，HEAD `fb65eda`；已提交 `092147a`/`fb65eda`、已推送、PR #42 已创建（OPEN，未合并））。**
> **验证状态：后端 `mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS，`Tests run: 127`，JaCoCo BUNDLE LINE 968/110/1078 = 0.8980（闸门 0.85）；前端 `npm test` → 30 files / 195 passed，`npm run test:coverage` → Lines 92.35%，`npm run lint` 干净，`npm run build` 成功，`npm run test:e2e` → 12 passed（本地 Node 24；Node 22 容器两次一致）。提交/推送/PR 与 CI 首跑：提交 `092147a`/`fb65eda`、已推送、**PR #42（OPEN，未合并）**；**CI 首跑（head `fb65eda`）两个 job 均 SUCCESS**。** AC 逐条状态与证据见 §6。

## 1. 远端元数据

| 项目 | 值 |
| --- | --- |
| 编号 | 31 |
| URL | https://github.com/PNZ-z/ai-powered-Fabflix/issues/31 |
| 标题（远端原文） | Migrate employee dashboard |
| Milestone | Phase 2 — React Frontend Migration（与 #32/#33 共用；由主 agent 提供） |
| 获取方式 | 主 agent 用 `gh` 获取的远端原文，并在任务说明中逐字提供（docs-writer 无 GitHub 访问授权，**未**直接查询远端） |
| 远端状态核对 | **未确认**（docs-writer 未查询远端；以远端为准） |
| 本地记录文档 | 本文件 |
| 关联计划 | `docs/plans/issue-31-migrate-employee-dashboard.md`（**已批准（主 agent，2026-10-15）→ 实施完成，已通过主 agent 终验（2026-10-15）**；实现结果与裁定差异见其 §13） |
| 关联全局约束 | `docs/architecture/frontend-migration-contracts.md` |
| 实现顺序 | **#31 → #32 → #33**（用户已批准）；**#31 完成前不得删 legacy** |

> ⚠️ 以上元数据来自主 agent 提供的远端原文；docs-writer **未**直接读取远端。如与远端当前状态不符，以远端为准并由主 agent 更正。

## 2. 远端 issue 正文原文（逐字保留，供对照）

```markdown
## Goal
Move employee authentication and administrative workflows to React.

## Scope
- Build the employee login and protected dashboard routes
- Render database metadata through the existing dashboard API
- Migrate add-star and add-movie forms
- Show stored-procedure results and validation errors clearly
- Keep employee authorization isolated from customer routes

## Acceptance criteria
- [ ] Employee login and logout work through React
- [ ] Unauthorized users cannot access dashboard features
- [ ] Database metadata renders correctly
- [ ] Add-star and add-movie flows handle success and failure
- [ ] Employee flows have automated integration coverage

## Dependency
Depends on the shared authentication and API infrastructure.
```

## 3. 用户/主 agent 已批准的范围决定（视为批准范围内）

| 编号 | 已批准决定 | 对应 issue 正文 |
| --- | --- | --- |
| D-31-1 | 新增员工登出 `GET /api/boot/dashboard/logout`：受 `EmployeeAuthenticationFilter` 保护；**只 `session.removeAttribute("employee")`，不销毁整个 session**；**返回 204**；由 `EmployeeController` 承载，配同步 MockMvc 测试；顾客 `GET /api/boot/logout` 行为**不变**。 | AC 第 1 条 "Employee login and **logout** work through React"；scope 未显式提及登出端点 |
| D-31-2 | 员工登录**不新增 reCAPTCHA**（后端本来不校验）；React 员工登录页字段与 legacy 一致（`username`/`password`）；legacy 员工页的 reCAPTCHA UI 差异**如实记录为已知差异**。 | scope 第 1 条；issue 未提及 reCAPTCHA |
| D-31-3 | 员工路由沿用现有 `ProtectedRoute audience="employee"` 与 `AuthProvider.employee`（`/boot/dashboard/session`）；`/employee/login` 公共、`/employee/dashboard` 受保护；与顾客路由隔离。 | scope 第 1、5 条；AC 第 2 条 |
| 顺序与交付 | 一个分支 `codex/issue-31-32-33-m2-completion`、一个 PR（`Closes #31`/`#32`/`#33`）；顺序 #31 → #32 → #33；**#31 完成前不得删 legacy**；后端新增代码必须配测试（JaCoCo 85% 闸门）。 | issue 正文未规定；由主 agent 传达 |

> **计划批准状态（2026-10-15）**：主 agent 依据 oracle 报告（B6 已闭合、N-31-1..N-31-10 与 Q-1..Q-8 全部裁定）**批准了完整计划**，**未改变 AC-31-1..AC-31-5 的验收范围**。要点：
> ① B6：员工登出测试固定用 `standaloneSetup(controller).addFilters(new EmployeeAuthenticationFilter())`，已登录用例共享同一 `MockHttpSession`（`user`+`employee`）；
> ② 删除 employee 占位机制（`routeDefinitions.js` 两条、`App.test.jsx` 的 `it.each`、`RoutePlaceholderPage.jsx`、`placeholderRoute`）；
> ③ h1 文案 `Employee login`/`Employee dashboard`；登录后用 `await refreshEmployee()`；登录表单加 `required`（**呈现差异，如实记录**）；登出按钮放仪表盘页内；仪表盘完整 loading/空态/错误+重试；
> ④ **不在 #31 补 `AdminRepositoryTest`**，但记为**已知缺口**（metadata 遍历 / `add_movie` 存储过程解析无直接测试）；
> ⑤ 架构文档补员工/顾客登出语义与 filter 归属；新端点与 `AdminController` 的 `@RequestMapping("/api/boot/dashboard")` **不冲突**；员工浏览器级路径由 **#32** 的 e2e 覆盖（本 issue 不得过度宣称 AC-31-5）。

## 4. 范围（依据 issue + 已批准决定）

### 4.1 In scope

- 员工登录页 + 受保护仪表盘路由（React）。
- 通过既有 dashboard API 渲染数据库 metadata。
- add-star / add-movie 表单迁移；存储过程结果与校验错误**清晰展示**。
- 员工授权与顾客路由**隔离**。
- **员工登出端点**（D-31-1）。
- 员工流程的自动化集成覆盖（定义见计划 §6；**仓库目前无浏览器 e2e 设施**，#32 才引入 Playwright）。

### 4.2 非目标（Non-goals）

- 删除或修改任何 legacy `src/main/webapp/**`（`FrontendApiMigrationTest.java:33-34` 钉死 `_dashboard.js`/`dashboard.js`；legacy 员工页是 #31 的行为基准）→ 属 **#33**。
- 给员工登录加 reCAPTCHA（前端控件或后端校验）→ D-31-2 已批准「不新增」。
- 改变顾客 `GET /api/boot/logout` 行为 → D-31-1。
- 修改两个 authentication filter 的路径规则。
- 修改 `AdminService`/`AdminRepository` 的业务语义、字段名或响应形状。
- 员工账号管理 / 审计日志。
- CSRF / Spring Security / JWT（架构文档 §10 AD-1）。
- Playwright / 覆盖率阈值 / CI 前端 job（→ #32）；生产构建 / SPA fallback / Docker / legacy 退役（→ #33）。

## 5. 依赖

| 依赖（issue 正文：shared authentication and API infrastructure） | 仓库现状 | 证据 |
| --- | --- | --- |
| 顾客/员工会话与认证 | 已完成（#35/#36） | `frontend/src/auth/AuthProvider.jsx`、`ProtectedRoute.jsx`；后端 `EmployeeAuthenticationFilter.java`、`SessionController.java:25-35` |
| 共享 API 处理 | 已完成 | `frontend/src/api/apiClient.js`（`apiRequest`、`ApiError`、`credentials:'include'`、204→null） |
| 路由壳 | 已完成（#34） | `frontend/src/app/AppRoutes.jsx`、`routeDefinitions.js` |
| 员工后端接口（登录/metadata/add-star/add-movie） | 已存在 | `src/main/java/com/fabflix/boot/admin/` |
| 员工登出端点 | **本 issue 新增**（D-31-1） | 现状不存在 |
| legacy 员工页（行为参考，**不得删除**） | 必须保留至 #33 | `src/main/webapp/_dashboard.{html,js}`、`dashboard.{html,js}` |

## 6. 验收条件与跟踪（**状态：全部已完成（主 agent 终验 2026-10-15）**）

| # | 验收条件（远端原文） | 承载方案（计划） | 状态与证据 |
| --- | --- | --- | --- |
| AC-31-1 | Employee login and logout work through React | `features/employee/employeeApi.js` + `EmployeeLoginPage` + `EmployeeDashboardPage`（登出）+ 后端 `GET /api/boot/dashboard/logout`（D-31-1） | ✅ **已完成**：`EmployeeController.logout`（`removeAttribute("employee")` → 204）已落地；`EmployeeControllerTest` 新增 2 条（204 + `employee` 移除 + `user` 保留；未登录 401）；前端 `EmployeeLoginPage.test.jsx`(8)、`EmployeeDashboardPage.test.jsx`(16)、`employeeFlow.test.jsx`(1)；`mvn verify` 127 全绿、前端 195 全绿、e2e 12 passed（含 employee 登入/登出）。`EmployeeController` JaCoCo 行 **0 missed / 13 covered** |
| AC-31-2 | Unauthorized users cannot access dashboard features | `ProtectedRoute audience="employee"`（D-31-3）+ 后端 `EmployeeAuthenticationFilter` 的 401（不改 filter） | ✅ **已完成**：`EmployeeAuthenticationFilterTest` 既有断言 + logout 401 新用例（`Employee authentication required.`）；`App.test.jsx`「redirects anonymous employees to the real employee login page」（断言真实登录页与 `required` 字段） |
| AC-31-3 | Database metadata renders correctly | `GET /boot/dashboard?action=metadata` + 每表一张 card 的渲染（字段名 `table_name`/`column_name`/`column_type`） | ✅ **已完成**：`employeeApi.test.js` 断言必带 `action=metadata`；`EmployeeDashboardPage.test.jsx` 多表/列类型原样/空态/重试；生产 smoke：`/employee/dashboard` 200 text/html 且员工登录后 metadata `status:success` |
| AC-31-4 | Add-star and add-movie flows handle success and failure | `action=add-star`/`action=add-movie` + 服务端 `status`/`message` 原样安全展示（**HTTP 200 + `status:"fail"` 也是失败**） | ✅ **已完成**：`EmployeeDashboardPage.test.jsx` 覆盖 add-star 成功/失败、add-movie 成功/业务失败（200+fail）、**401 与网络失败各两条**；`AdminControllerTest`/`AdminServiceTest` 契约回归全绿（`mvn verify` 127） |
| AC-31-5 | Employee flows have automated integration coverage | 计划 §6（后端 MockMvc + 前端页面级 fetch mock；**浏览器级员工路径由 #32 的 `e2e/employee.spec.js` 覆盖**，N-31-10） | ✅ **已完成**：后端 127 全绿（JaCoCo `All coverage checks have been met.`，BUNDLE LINE 0.8980）；前端 30 files / 195 passed；浏览器级 `e2e/employee.spec.js`（登录 → metadata → add-star → 登出）× 2 viewport 已通过（共 12 passed）。**说明**：e2e 全 mock API，不代表真实 MySQL/存储过程往返（见下） |

> **证据要求（已满足）**：AC 对应的实际命令与结果已填入上表（`mvn --batch-mode --no-transfer-progress verify`、`npm test`、`npm run test:coverage`、`npm run lint`、`npm run build`、`npm run test:e2e`），均为**主 agent 亲跑**；docs-writer 只读复核了 `target/surefire-reports/*`（127/0/0/0）与 `target/site/jacoco/jacoco.csv`（LINE 968/110/1078）。
> **已知缺口（已接受，N-31-7）**：本 issue **未**新增 `AdminRepositoryTest` → metadata 遍历与 `add_movie` 存储过程结果集解析**仍无直接测试**；不得在交付报告中声称已覆盖。
> **未发生的事实（已更新）**：本 issue 的改动已提交（`092147a`/`fb65eda`）、已推送、**PR #42 已创建（OPEN，未合并）**；**CI 首跑（head `fb65eda`）`Build and test (Java 11)` 与 `Frontend (lint, test, coverage, build, e2e)` 均 SUCCESS**（来源：主 agent 核验的 GitHub CI 结果，docs-writer 未直接访问 GitHub）。**PR 未合并、issue 未关闭** → 不得声称「已合并」。
> **已批准差异提醒**：员工登录表单带 `required`（legacy `_dashboard.html` 实际也带 `required`；计划文本的「legacy 无 required」判断有误，已在计划 §13.2 更正）；员工登录**无 reCAPTCHA**（D-31-2，legacy 有）→ 不得声称 parity。

## 7. 关联记录

| 类型 | 位置 | 说明 |
| --- | --- | --- |
| 计划 / 方案 | `docs/plans/issue-31-migrate-employee-dashboard.md` | **已批准（主 agent，2026-10-15）→ 实施完成，已通过主 agent 终验（2026-10-15）**；B6 已闭合，N-31-1..N-31-10/Q-1..Q-8 已全部裁定（§10/§11）；实现结果与 4 处裁定差异见其 **§13** |
| scout 调查报告 | `.pi/artifacts/scout-issues-31-32-33.md` | §A（后端 employee 接口、认证/授权、legacy 行为参考、现有测试模式、前端现状与缺口） |
| 架构约束 | `docs/architecture/frontend-migration-contracts.md` | §3（已删占位机制）、§4 三件套约定、§6 错误响应约定、§7 测试设施与 CI 闸门（已更新）、§8 架构守卫、§9 会话/认证（已补员工登出语义）、§10 AD-1/AD-2、**§11 SPA fallback（新增）** |
| 工作流日志 | `docs/logs/2026-10-15-issues-31-32-33-m2-completion.md` | **已创建**（三个 issue 共用一份日志；含「修改了哪些文件及潜在问题」与「新增知识点 / 重点学习对象」两大节） |
| 远端 issue | #31（见 §1） | **不由 docs-writer 修改**；PR #42 与 CI 首跑结果已回填（本 §7 的 PR 行与计划 §13.5） |
| PR | **#42** — https://github.com/PNZ-z/ai-powered-Fabflix/pull/42 | **已创建（OPEN，非 draft，MERGEABLE；未合并）**：分支 `codex/issue-31-32-33-m2-completion`（HEAD `fb65eda`）→ base `main`；GitHub 已识别 `Closes #31`/`#32`/`#33`（`closingIssuesReferences`）；**CI 首跑两个 job 均 SUCCESS** |

## 8. 备注

- 本文件**不发布到 GitHub**；远端 issue 内容不由 docs-writer 修改。
- **2026-10-15（创建时）**：本文件与关联计划由 docs-writer 首次创建；**当时的计划状态 = 待 oracle 审查 / 未批准**；实现未开始、未运行任何测试。**当前状态见下一行的本轮批准记录。**
- **2026-10-15（本轮）**：oracle 只读审查完成 → **主 agent 批准计划并全部接受 N-31-1..N-31-10/Q-1..Q-8**。
- **2026-10-15（实施终验）**：worker 完成 #31 实现并由同一 worker 处理三路只读审查意见（正确性/测试/复杂度均无 Blocker）后复验；**主 agent 亲跑终验**：后端 `mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS、127 tests、JaCoCo LINE 0.8980；前端 195 tests / 92.35% lines / 12 e2e（本地 Node 24 + Node 22 容器两次一致）。AC 状态与证据已写入 §6；计划侧的实现结果与 4 处裁定差异见 `docs/plans/issue-31-migrate-employee-dashboard.md` §13。
- **2026-10-15（提交 / PR / CI 首跑）**：实现提交 `092147a`、文档提交 `fb65eda`（分支 `codex/issue-31-32-33-m2-completion`，HEAD `fb65eda`）已推送；**PR #42 已创建（OPEN，未合并）**；**CI 首跑（head `fb65eda`）两个 job 均 SUCCESS**（`Build and test (Java 11)`、`Frontend (lint, test, coverage, build, e2e)`）。来源：主 agent 核验的 GitHub CI 结果（docs-writer 未直接访问 GitHub）。详见计划 §13.5。
- **仍待回填（更新）**：仅剩 PR 的**合并/关闭状态**（当前 OPEN、未合并）与人工浏览器检查项；**不得**在合并前写「已合并 / issue 已关闭」。
- **待实现时回填（已回填）**：`npm test` 改前/改后用例数（155 tests / 26 files → **195 tests / 30 files**）、`mvn verify` 测试数与 JaCoCo 数字（**127 / 0.8980**）、`npm run lint` 退出码（**0**）。
- **已知差异（已批准）**：legacy 员工登录页有 reCAPTCHA（`_dashboard.html:38-44`、`_dashboard.js:17-25`），React 员工登录页**没有**（D-31-2）；必须在实现完成后的文档与交付报告中如实记录，**不得**声称 parity。
- **顺序约束**：删除 legacy（#33）之前，#31 必须完成；`FrontendApiMigrationTest.java:33-34` 要求 `_dashboard.js`/`dashboard.js` 仍存在。
- docs-writer 无 GitHub 访问授权；§1 元数据来自主 agent 用 `gh` 获取的原文。

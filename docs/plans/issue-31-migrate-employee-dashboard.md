# Issue #31 方案 / 计划 — Migrate employee dashboard

> **实现状态：实施完成，已通过主 agent 终验（2026-10-15）；已提交（`092147a` 实现 + `fb65eda` 文档）、已推送、**PR #42 已于 2026-10-16 合并**（merge commit **`d618b64`**），**issue #31 已关闭**（GitHub 依 PR 正文的 `Closes #31` 自动关闭）。**
> 分支 `codex/issue-31-32-33-m2-completion`（HEAD `fb65eda`）→ PR **#42**（https://github.com/PNZ-z/ai-powered-Fabflix/pull/42，base `main`）；**CI 首跑（head `fb65eda`）两个 job 均 SUCCESS**（见 §13.5）；**合并后远端分支已删除、`main`（`d618b64`）上 CI 双 job（Java 11 + Frontend）亦 SUCCESS**；里程碑 **Phase 2 — React Frontend Migration（#2）已关闭**（10/10）。
> 本文档 §1–§11（及 §12 的批准时记录）保留为**批准时的计划文本**（事实性内容未追改）；实现结果、与计划的逐项差异、终验证据与残余风险见 **§13**。
>
> **文档状态（批准时）：已批准（主 agent，2026-10-15）— 已交 worker 实现。**
>
> 依据 oracle 第一轮只读审查报告 `.pi/artifacts/oracle-issues-31-32-33-plans.md`（#31 章节），**本 issue 的唯一规范缺口 B6 已闭合**，
> 其余建议 N-31-1..N-31-10 与未决问题 Q-1..Q-8 **已由主 agent 全部裁定并写入本文档**，逐行回填见 §11；§10 的「裁定」列已同步。
> 主 agent 裁定依据：oracle 报告「#31：可批准（补齐 B6 的 filter 接线写法即可）」；**本批修订不改变 #31 的验收范围**。
> **批准状态不代表已实现**：本文档是 worker 的实施许可与验收基准；实现是否完成以实际 diff 与测试证据为准（§12 记录未运行项）。
> 文件:行号若与 oracle 报告不一致，**以 oracle 报告（其已复核）为准**并在相关节注明。

## 元数据

| 项目 | 值 |
| --- | --- |
| GitHub issue | #31 — https://github.com/PNZ-z/ai-powered-Fabflix/issues/31 |
| 标题（远端原文） | Migrate employee dashboard |
| Milestone | Phase 2 — React Frontend Migration（与 #32/#33 共用；由主 agent 提供） |
| 来源 | 用户任务 + GitHub issue #31 远端正文（主 agent 用 `gh` 获取）+ scout 报告 `.pi/artifacts/scout-issues-31-32-33.md` + docs-writer 亲自核对的源码 |
| 关联 issue 跟踪文档 | `docs/issues/issue-31-migrate-employee-dashboard.md` |
| 关联全局约束 | `docs/architecture/frontend-migration-contracts.md` |
| 前置（已完成） | #34 路由壳、#35/#36 API+认证、#37 discovery、#38 movie list、#39 detail、#30 cart/checkout/confirmation |
| 后续依赖本 issue | **#33 的「删除 legacy」**——`src/main/webapp/_dashboard.*`、`dashboard.*` 是 #31 的行为 oracle，**#31 完成前不得删除**（scout「风险与依赖顺序」）；#32 的前端测试/覆盖率亦需 #31 的新页面并入后才完整 |
| 分支 / PR 计划 | 分支 `codex/issue-31-32-33-m2-completion`，单个 PR（`Closes #31`、`Closes #32`、`Closes #33`）（用户已批准，见 §1.3「顺序与交付」） |
| 创建日期 | 2026-10-15 |
| 文档状态 | **已批准（主 agent，2026-10-15）— 可交 worker 实现**；依据 oracle 报告，B6（唯一规范缺口）已写入 §5.3/§6.1，N-31-1..N-31-10 与 Q-1..Q-8 已全部裁定（§11 逐行回填） |
| 验收范围 | **本批修订不改变 #31 的 AC-31-1..AC-31-5**；oracle 报告 §7 亦确认 #31 无 Blocker，其余建议为定稿性、不扩大范围 |

## 1. 目标与背景

### 1.1 目标

把**员工（employee）认证与管理工作流**迁到 React（issue 正文 "Goal"）：员工登录、受保护的仪表盘、数据库 metadata 展示、
add-star / add-movie 表单与结果/错误展示，并与顾客路由**隔离**。

### 1.2 背景：现状事实（带文件:行号证据）

> 以下均为 docs-writer **亲自复核**的当前仓库（`main` @ `f6163f3`）事实；与 scout 报告不一致处已注明。

**后端 — 员工登录（现状，不改契约）**

- `src/main/java/com/fabflix/boot/admin/EmployeeController.java:20-34`：`@PostMapping("/api/boot/employee-login")`；
  形参 `@RequestParam(username, required=false)`、`password`（第 22-23 行）。
  - 成功：`request.changeSessionId()`（第 30 行）+ `session.setAttribute("employee", employee.get())`（第 31 行），返回 **200** `AdminResponse.success("Employee login successful.")`（第 33 行）。
  - 失败：**200** + `AdminResponse.failure("Invalid employee email or password.")`（第 28 行）。
  - **服务端不校验 reCAPTCHA**（对比顾客登录 `CustomerAuthController.java:41-46`：缺少/未通过时返回 **403** `LoginResponse.failure("Recaptcha Not Passed")`）。
- `src/main/java/com/fabflix/boot/admin/EmployeeService.java:17-23`：`authenticate(email, password)` 仅 trim + `EmployeeRepository.findByEmail` + `PasswordVerifier.matches`；账号为空/密码为空 → `Optional.empty()`。
- `src/main/java/com/fabflix/boot/admin/EmployeeIdentity.java`：`implements Serializable` + `serialVersionUID`（scout A2；架构文档 §9 的 session 序列化约定）。

**后端 — 仪表盘 API（现状，不改契约）**

- `src/main/java/com/fabflix/boot/admin/AdminController.java:11-12`：类级 `@RequestMapping("/api/boot/dashboard")`（**注意：是 `/api/boot/dashboard`，不是 `/api/boot/dashboard/...` 子路径**）。
  - 第 19-23 行：`@GetMapping(params="action=metadata")` → `MetadataResponse`。
  - 第 25-31 行：`@PostMapping(params="action=add-star")`，`star_name`（必填）、`birth_year`（可选）。
  - 第 33-43 行：`@PostMapping(params="action=add-movie")`，6 个可选 `@RequestParam`：`movie_title`/`movie_year`/`movie_director`/`movie_star_name`/`movie_star_birth_year`/`movie_genre_name`。
  - 三个端点都走 **form-urlencoded**（`@RequestParam`），前端必须用 `URLSearchParams` body（架构文档 §5）。
- `src/main/java/com/fabflix/boot/admin/AdminService.java`
  - `addStar`（第 11-19 行）：空名 → `"Star name cannot be empty."`；`birth_year` 非数字/负数 → `"Birth year must be a valid non-negative number."`；成功 → `"Star inserted successfully. New star ID: " + id"`。
  - `addMovie`（第 21-32 行）：任一必填（title/year/director/starName/genreName）缺失 → `"Please fill in all required fields."`；年非法 → `"Movie year must be a valid non-negative number."`；星生日非法 → `"Star birth year must be a valid non-negative number."`；成功/失败由 repository 决定。
  - 注意：`add-movie` 的**必填校验在 service 层**，因此缺参时仍是 **200 + `status:"fail"`**，不是 400。
- `src/main/java/com/fabflix/boot/admin/AdminRepository.java`
  - `addStar`（第 24-30 行）：`SELECT MAX(id) FROM stars` → `String.format("nm%07d", next)` → `INSERT INTO stars(...)`。**不走存储过程**。
  - `addMovie`（第 32-51 行）：`{CALL add_movie(?, ?, ?, ?, ?, ?)}`（第 35 行），`starBirthYear == null` → `setNull(5, Types.INTEGER)`（第 40 行）；结果集无行/无结果集 → `AdminResponse.failure(...)`（第 42、44 行）；有行时 `"success".equals(status)` → success，否则 failure（第 45-47 行）。
    - **业务失败（如重复电影）= 结果集 `status='fail'`，HTTP 仍 200**（`stored-procedure.sql` 的 `SELECT 'fail' AS status, CONCAT('Movie already exists: ', ...)` 分支；scout A1）。
  - `metadata`（第 53-72 行）：`DatabaseMetaData.getTables/getColumns` 遍历 `TABLE`，列类型拼成 `TYPE_NAME(COLUMN_SIZE)`（第 63-64 行）。**无 `AdminRepositoryTest`**（scout A4：未覆盖 add_movie 解析与 metadata 遍历）。
- 响应 DTO（docs-writer 已读源码确认字段）：
  - `AdminResponse.java:5-18`：`@JsonInclude(NON_NULL)`，字段 `status`（`success`/`fail`）+ `message`。
  - `MetadataResponse.java:5-11`：`status = "success"`（硬编码）+ `tables`。
  - `MetadataTable.java:7`：`@JsonProperty("table_name")` + `columns`；`MetadataColumn.java:6-7`：`@JsonProperty("column_name")` / `@JsonProperty("column_type")`。
- `src/main/java/com/fabflix/boot/admin/AdminApiExceptionHandler.java:10`：`@RestControllerAdvice(assignableTypes = {AdminController.class, EmployeeController.class})` → 任意 `Exception` → **500** `AdminResponse.failure("Unable to complete the employee request.")`（第 17-18 行）。**新增在 `EmployeeController` 上的方法天然被该 advice 覆盖**。

**后端 — 授权边界（现状）**

- `src/main/java/com/fabflix/boot/admin/EmployeeAuthenticationFilter.java:16-19`：`shouldNotFilter` = `!path.startsWith("/api/boot/dashboard")` → **只保护 `/api/boot/dashboard*`**（含 `/api/boot/dashboard/session`，以及本 issue 计划新增的 `/api/boot/dashboard/logout`）。
  - 第 24-31 行：session 无 `EmployeeIdentity` → **401** + `{"status":"fail","message":"Employee authentication required."}`（JSON 由 filter 手写，**不是** `errorMessage` 形状）。
- `src/main/java/com/fabflix/boot/auth/CustomerAuthenticationFilter.java:18-25`：保护所有 `/api/boot/**`，豁免 `login`、`employee-login`、`dashboard*`、`health`。
  → 员工路径归员工 filter；`/api/boot/employee-login` 对两个 filter 都公开。
- `src/main/java/com/fabflix/boot/session/SessionController.java:25-35`：`GET /api/boot/dashboard/session` 读 `session.getAttribute("employee")`，非 `EmployeeIdentity` → **401 空 body**；命中 → `SessionIdentityResponse.employee(email, fullName)`。
- `src/main/java/com/fabflix/boot/session/SessionIdentityResponse.java:20-38`：`customer()` 的 `displayName` 为 **null**，`employee()` 为 `fullName`；JSON 字段 `authenticated`/`role`/`username`/`displayName`（`role` 为 `"employee"`）。
- **员工登出现在不存在**（scout A2，docs-writer 复核：全仓库唯一的登出是 `CustomerAuthController.java:59-69` 的 `GET /api/boot/logout`，用的是 `session.invalidate()` → 会**一并销毁员工会话**，因为同一 session）。

**前端 — 现状**

- `frontend/src/app/routeDefinitions.js:7-23`：`employeePublicRoutes`（`employee/login`）与 `employeeProtectedRoutes`（`employee/dashboard`）两条**占位**定义，`description` 明确写着 "will be migrated in Issue #31"；`routeDefinitions`（第 27 行）= 两条 employee 占位（`customerProtectedRoutes` 已被 #30 置空，第 5 行）。
- `frontend/src/app/AppRoutes.jsx:22-30`（`placeholderRoute`）、第 49 行（`employeePublicRoutes.map(placeholderRoute)`）、第 50-52 行（`<ProtectedRoute audience="employee">` + `employeeProtectedRoutes.map(placeholderRoute)`）。
- `frontend/src/features/placeholders/RoutePlaceholderPage.jsx:3-15`：占位页渲染 `route.title` 作为 `<h1>`——**这正是 `App.test.jsx` 断言「Employee login」标题的来源**。
- `frontend/src/auth/AuthProvider.jsx:30-33,41-43,55-58`：`refreshEmployee()` 读 `/boot/dashboard/session`；提供 `employee`、`refreshEmployee`、`setEmployeeAuthenticated`、`clearEmployee`。
- `frontend/src/auth/ProtectedRoute.jsx:6-26`：`audience="employee"` → session 键 `auth.employee`、未认证跳 `/employee/login`（带 `state.from`）、`error` 时显示 `ErrorState` + `refreshEmployee` 重试。
- `frontend/src/components/layout/AppShell.jsx:14-17`：`employeeLinks` = `/employee/login`、`/employee/dashboard`；第 55-61 行第二套 `<nav aria-label="Employee navigation">`；页脚只在 **customer** 已认证时渲染 `LogoutButton`（第 70-72 行）→ **员工没有登出入口**。
- 可复用：`features/auth/LoginPage.jsx`（表单 + `role="alert"` + 提交中态 + `Navigate` 已认证回跳）、`customerAuthApi.js:3-15`（`URLSearchParams` + `apiRequest`）、`components/states/*`、`apiClient.js:21-33`（**204 → `null`**，第 22-24 行）。
- `frontend/src/App.test.jsx`（docs-writer 已读）：
  - 第 66-72 行 `responseBody(url)` 按 URL 分派，**目前没有 `/boot/dashboard*` 的分派**，未知 URL 落到第 72 行 `return [{ name: 'Drama' }]`（= 类型列表形状）。
  - 第 107 行 `it.each(routeDefinitions)('renders the $title placeholder at $samplePath', ...)`。
  - 第 186-190 行 `it('redirects anonymous employees to employee login')` 断言 `heading 'Employee login'`（当前来自占位页的 `<h1>{route.title}</h1>`）。
- 缺口（scout A5）：无 `features/employee/`（无员工页面、无 `employeeApi.js`）、无员工登出、`App.test.jsx` 无员工 API 分派。

**legacy 行为参考（#31 的行为基准，**不得删除**）**

- `src/main/webapp/_dashboard.html`：员工登录页，`#employee_login_form` 字段 `username`（第 25 行）、`password`（第 31 行），带 Google reCAPTCHA 控件（第 38-44 行）与 `#recaptchaErrorMessage`。
- `src/main/webapp/_dashboard.js`：`submitEmployeeLoginForm` 先检查 `grecaptcha.getResponse()`（第 17-25 行），成功 → `window.location.replace("dashboard.html")`（第 6 行），失败 → 写入 `#employee_login_error_message`（第 8 行）。
  - **行号修正**：scout A3 记为「`grecaptcha.getResponse()` 第 24-31 行、`window.location.replace` 第 8 行」；docs-writer 复核实际为 **第 17-25 行**与**第 6 行**。行为结论不变。
- `src/main/webapp/dashboard.html`：`add_star_form`（`star_name` 第 30 行、`birth_year` 第 41 行、按钮 "Insert Star" 第 47 行）+ `#add_star_message`（第 50 行）、`#metadata_button` "Show Metadata"（第 61 行）+ `#metadata_result`（第 63 行）、`add_movie_form`（6 个字段第 75-133 行）+ `#add_movie_message`（第 143 行）。
- `src/main/webapp/dashboard.js`：`handleMetadataResult`（第 72-131 行）**每表一张 card**（表头 = 表名，表内两列 "Column Name"/"Column Type"，第 107-108 行），空数据 → `"No metadata found."`（第 90 行）；`requestMetadata`（第 138-150 行）`GET api/boot/dashboard?action=metadata`，失败 → `"Error: failed to load database metadata."`（第 147-150 行）；`submitAddStarForm`（第 30-41 行）与 `submitAddMovieForm`（第 182-193 行）POST 到 `api/boot/dashboard`。
- 结论：legacy 的成功/失败都是 **HTTP 200 + `status` 字段驱动的文本消息**，与后端现状一致。

### 1.3 已批准决定（用户/主 agent 已传达；写入本文档作为**已批准范围**）

| 编号 | 已批准决定 |
| --- | --- |
| **D-31-1** | 新增员工登出 `GET /api/boot/dashboard/logout`，由 `EmployeeAuthenticationFilter`（`/api/boot/dashboard*` 前缀）保护；**只 `session.removeAttribute("employee")`，不销毁整个 session**；**返回 204**；由 `EmployeeController` 承载，并配同步 MockMvc 测试。顾客 `GET /api/boot/logout` 行为**不变**。 |
| **D-31-2** | 员工登录**不新增 reCAPTCHA**（后端 `EmployeeController` 本来就不校验；保持契约不变）。legacy 员工页的 reCAPTCHA UI 差异**如实记录为已知差异**。React 员工登录页字段与 legacy 一致（`username`/`password`）。 |
| **D-31-3** | 员工路由沿用现有 `ProtectedRoute audience="employee"` 与 `AuthProvider.employee`（`/boot/dashboard/session`）；`/employee/login` 公共、`/employee/dashboard` 受保护；与顾客路由隔离。 |
| **顺序与交付** | 一个分支 `codex/issue-31-32-33-m2-completion`、一个 PR（`Closes #31`/`Closes #32`/`Closes #33`）；实现顺序 **#31 → #32 → #33**；**#31 完成前不得删 legacy**；每阶段都要跑相应测试；后端新增代码必须配测试（JaCoCo 85% 闸门）。 |

> 以上 4 项来自主 agent 的任务说明。**自 2026-10-15 起，本计划其余内容（文件划分、组件结构、UI 文案、测试清单）已经过 oracle 只读审查并由主 agent 裁定/批准（见 §10/§11）**：本批修订不改变验收范围，仅把实施口径定稿（含 B6 与 N-31-1..N-31-10）。

## 2. 验收条件逐条映射（issue 正文原文 → 本计划如何满足）

| # | issue 验收条件（原文） | 计划承载（已批准） | 验证方式（计划） |
| --- | --- | --- | --- |
| AC-31-1 | Employee login and logout work through React | `features/employee/employeeApi.js` 封装 `POST /boot/employee-login`（`username`/`password`，`URLSearchParams`）与 `GET /boot/dashboard/logout`（D-31-1）；`EmployeeLoginPage` 成功 → `refreshEmployee()` + 跳 `/employee/dashboard`；`EmployeeDashboardPage` 提供登出按钮 → `clearEmployee()` + 跳 `/employee/login` | 后端 `EmployeeControllerTest`（登录既有断言保持 + **logout 新断言**）；前端 `EmployeeLoginPage.test.jsx`、`EmployeeDashboardPage.test.jsx` |
| AC-31-2 | Unauthorized users cannot access dashboard features | 沿用 `ProtectedRoute audience="employee"`（D-31-3）：匿名 → 跳 `/employee/login`（带 `state.from`）；后端 401 由 `EmployeeAuthenticationFilter` 对 `/api/boot/dashboard*` 统一发出（**不改 filter**） | 前端 `App.test.jsx`（匿名 `/employee/dashboard` 重定向）；后端既有 `EmployeeAuthenticationFilterTest`（401 body 契约）保持通过；新增 logout 的 401 分支断言 |
| AC-31-3 | Database metadata renders correctly | `EmployeeDashboardPage` 调 `GET /boot/dashboard?action=metadata`，渲染每表一张 card（表名 + 两列表格 Column Name / Column Type），空 `tables` → `"No metadata found."`；`table_name`/`column_name`/`column_type` 字段名严格按 `MetadataTable`/`MetadataColumn` 的 `@JsonProperty` | 前端 `employeeApi.test.js`（**必须断言 query 参数 `action=metadata`**，因为后端用 `params="action=metadata"` 匹配）+ `EmployeeDashboardPage.test.jsx`（多表/多列渲染、空态、失败态） |
| AC-31-4 | Add-star and add-movie flows handle success and failure | 两个表单分别 POST `action=add-star` / `action=add-movie` + 对应字段；服务端 `status:"success"\|"fail"` + `message` **原样安全展示**（成功与失败都用 `role="status"`/`role="alert"` 区分）；成功后可清空表单（对齐 legacy `reset()`） | 前端 `EmployeeDashboardPage.test.jsx`（成功/失败各两条路径 + 请求 body 断言）；后端 `AdminControllerTest`/`AdminServiceTest` 既有断言作为契约回归护栏（**本 issue 预期不改 admin 业务代码**） |
| AC-31-5 | Employee flows have automated integration coverage | 后端：`EmployeeControllerTest` 覆盖登录（既有）+ logout（新增，含 401 经 filter 的用例）；前端：`employeeFlow` 级别的页面级流程测试（login → dashboard → metadata/表单/登出，`fetch` 按 URL mock） | 后端 `mvn --batch-mode verify`（含 JaCoCo 85% 闸门）；前端 `npm test` + `npm run lint`；**说明**：仓库无浏览器 e2e 设施（#32 才引入 Playwright），本 issue 的 "integration" 沿用 #30 计划的定义（后端 MockMvc + 前端页面级 fetch mock，见 `docs/plans/issue-30-migrate-cart-payment-confirmation-flows.md` §7.4）；**浏览器级员工路径将由 #32 的 `frontend/e2e/employee.spec.js` 覆盖（N-31-10，已裁定）——本 issue 的 PR 描述与文档不得据此过度宣称 AC-31-5 的覆盖强度** |

## 3. 范围（In scope）与 Non-goals（Out of scope）

### 3.1 In scope（已批准）

1. `frontend/src/features/employee/`：员工登录页、员工仪表盘页、`employeeApi.js`（含登录与登出）。
2. 员工路由接线：`AppRoutes.jsx` 中把 `employee/login` 与 `employee/dashboard` 换成显式真实路由，并**删除整个 employee 占位机制**（`routeDefinitions.js` 两条、`App.test.jsx` 的 `it.each`、`RoutePlaceholderPage.jsx`、`AppRoutes.jsx` 的 `placeholderRoute`；Q-3 已裁定，见 §5.2）。
3. 后端新增 `GET /api/boot/dashboard/logout`（D-31-1），并把 `EmployeeControllerTest` 补到覆盖它。
4. `AppShell` **不改结构**；员工登出入口放在员工仪表盘页内（Q-4 已裁定：最小侵入，不动 `AppShell` 双导航，见 §4.2/§5.2）。
5. `App.test.jsx` 的 `responseBody(url)` 增加 `/boot/dashboard*` 分派（架构文档 §3 的 N13 要求），以及员工路由的真实页面/重定向断言。
6. 本 issue 的 `docs/` 文档（计划、issue 跟踪、必要时架构文档增补）。

### 3.2 Non-goals（明确排除）

| 排除项 | 理由 / 证据 |
| --- | --- |
| **删除或修改任何 legacy `src/main/webapp/**` 文件** | `FrontendApiMigrationTest.criticalPagesUseTheirSpringBootEndpoints`（第 33-34 行）要求 `_dashboard.js`、`dashboard.js` **仍存在**且含 `api/boot/employee-login` / `api/boot/dashboard`；且 legacy 员工页是 #31 的行为基准。删除属 **#33**（已批准的破坏性动作见 #33 计划）。 |
| **给员工登录加 reCAPTCHA（前端控件或后端校验）** | D-31-2 已批准「不新增」；legacy 的 reCAPTCHA UI 差异如实记录（§9 R-31-4）。 |
| **改变顾客 `GET /api/boot/logout` 行为** | D-31-1 明确「顾客登出行为不变」；`CustomerAuthController.java:59-69` 保持（其 `/login.html` 重定向问题属 **#33**，见 #33 计划）。 |
| **修改 `EmployeeAuthenticationFilter` / `CustomerAuthenticationFilter` 的路径规则** | 现状已满足 D-31-1/D-31-3；改 filter 会波及顾客路径与既有测试。 |
| **修改 `AdminService`/`AdminRepository` 的业务语义、字段名或响应形状** | 本 issue 目标是前端迁移；契约保持（`AdminResponse`/`MetadataResponse` 形状不变）。若 worker 发现必须改，**暂停并询问主 agent**。 |
| **员工账号管理（新增/删除员工、改密码）、审计日志** | issue 范围未提及；仓库无相关端点。 |
| **CSRF / Spring Security / JWT** | 架构文档 §10 AD-1（路线 C）；#30 已排除；`pom.xml` 无 security starter。 |
| **Playwright / 浏览器级 e2e、覆盖率阈值、CI 前端 job** | 属 **#32**。 |
| **生产构建、SPA fallback、Docker、legacy 退役** | 属 **#33**。 |
| **顾客侧页面/路由的改动** | 与 #31 无关；仅 `AppShell` 导航与 `App.test.jsx` 因路由表变化而联动。 |

## 4. 关键契约与流程

### 4.1 后端 API 契约（现状 → 本 issue）

| 端点 | 现状 | 本 issue |
| --- | --- | --- |
| `POST /api/boot/employee-login`（`username`,`password`） | 200 `AdminResponse`（`success`/`fail`）；成功建 `employee` session 属性并轮换 session id | **不变** |
| `GET /api/boot/dashboard?action=metadata` | 200 `{status:"success", tables:[{table_name, columns:[{column_name, column_type}]}]}` | **不变** |
| `POST /api/boot/dashboard?action=add-star`（`star_name`,`birth_year`） | 200 `AdminResponse` | **不变** |
| `POST /api/boot/dashboard?action=add-movie`（6 个字段） | 200 `AdminResponse`（业务失败也是 200 + `fail`） | **不变** |
| `GET /api/boot/dashboard/session` | 200 `{authenticated, role:"employee", username, displayName}` / 401（filter 出 JSON，standalone 测试下为空 body） | **不变** |
| `GET /api/boot/dashboard/logout` | **不存在** | **新增（D-31-1）**：只 `session.removeAttribute("employee")`，**不 `invalidate()`**；**204**（无 body）；受 `EmployeeAuthenticationFilter` 保护 → 未登录时 **401** `{"status":"fail","message":"Employee authentication required."}`；由 `EmployeeController` 承载、`AdminApiExceptionHandler` 覆盖 |
| `GET /api/boot/logout`（顾客） | `session.invalidate()`；`Accept` 含 `application/json` → 204，否则 302 `/login.html` | **不变** |

> **端点不冲突（N-31-9，已裁定）**：新端点 `GET /api/boot/dashboard/logout` 与 `AdminController` 的**类级** `@RequestMapping("/api/boot/dashboard")`（`AdminController.java:11-12`）**不冲突**——后者只处理**精确路径** `/api/boot/dashboard` 且带 `params="action=..."` 匹配，`/api/boot/dashboard/logout` 是另一条路径。**worker 不得为此改动 `AdminController`**（若仍认为有冲突，暂停并询问主 agent）。
> **语义要点（写入计划的理由）**：员工登出用 `removeAttribute` 而非 `invalidate`（D-31-1），因为同一 session 可能同时持有顾客身份（`user`）——`invalidate()` 会顺带把顾客登出。前端 `apiClient` 已把 **204 解析为 `null`**（`apiClient.js:22-24`），因此 `logoutEmployee()` 的返回值应被忽略。
> **401 形状提醒（#31 的坑）**：员工 filter 的 401 body 是 `{"status":"fail","message":"Employee authentication required."}`，而 `apiClient.errorMessage`（第 35-43 行）优先读 `errorMessage` 再读 `message` → 前端会得到 `"Employee authentication required."` 文案，`ApiError.isUnauthorized === true`。

### 4.2 前端路由与状态（已批准）

- 路由：`/employee/login`（公共，真实页 `EmployeeLoginPage`）、`/employee/dashboard`（`<ProtectedRoute audience="employee">` 内，真实页 `EmployeeDashboardPage`）。
- 状态：`AuthProvider.employee`（`checking`/`authenticated`/`anonymous`/`error`）已在 `App.jsx` 挂载时自动加载一次（`AuthProvider.jsx:41-43`）。
  - 登录成功：**`await refreshEmployee()`（Q-2 已裁定）**——与 `LoginPage.jsx:49` 的顾客做法一致、身份来源唯一；后端登录响应只有 `status`/`message`（`EmployeeController.java:20-34` + `AdminResponse`），**不使用** `setEmployeeAuthenticated`（需自造 `{authenticated, role:'employee', username, displayName}` 形状，不推荐）。
  - 登出：`clearEmployee()` + `navigate('/employee/login', { replace: true })`（对齐 `LogoutButton.jsx:12-24` 的顾客模式）。
- 员工导航：`AppShell.jsx:14-17` 现有两条链接保留，**`AppShell` 不改**；**登出按钮放在员工仪表盘页内**（Q-4 已裁定）。
  - **现状缺口（§1.2 补充）**：`AppShell` 页脚只在**顾客已认证**时渲染 `LogoutButton`（`AppShell.jsx:70-72`），员工**没有任何登出入口**——这正是 #31 必须补的行为缺口，也是把按钮放进仪表盘页的原因。

### 4.3 关键流程与错误/边界

```
[匿名访问 /employee/dashboard]
  → ProtectedRoute audience="employee"：employee.status === 'anonymous'
  → <Navigate to="/employee/login" replace state={{ from: location }} />（ProtectedRoute.jsx:21-23）
  （现有 App.test.jsx:186-190 已断言此重定向；迁移后断言目标页真实标题）

[employee.status === 'error'（后端不可达）]
  → ErrorState + Try again → refreshEmployee()（ProtectedRoute.jsx:17-19）
  → 不得把"后端故障"误判为"未登录"（与顾客侧一致）

[/employee/login]
  → 已认证时 <Navigate> 到 destination（state.from 或 /employee/dashboard）——对齐 LoginPage.jsx:22-25
  → 提交：POST /boot/employee-login（body: username=..&password=..）
      ├─ status=success → refreshEmployee()/setEmployeeAuthenticated → 跳 /employee/dashboard
      ├─ status=fail → role="alert" 显示服务端 message（"Invalid employee email or password."），保留输入
      ├─ 401/403 → 安全错误提示（注意：本端点对两个 filter 都豁免，正常不会 401）
      └─ 网络错误 → ApiError("Unable to reach the Fabflix server.") 安全展示
  → reCAPTCHA：**不渲染**（D-31-2）；不复用 RecaptchaField.jsx

[/employee/dashboard]
  → metadata：GET /boot/dashboard?action=metadata
      ├─ 200 + tables 非空 → 每表一张 card（表名 + Column Name/Column Type 两列表格）
      ├─ 200 + tables 为空 → "No metadata found." 空态（对齐 legacy dashboard.js:90）
      ├─ 401 → 会话失效：清 employee 状态并跳 /employee/login（提示重新登录）
      ├─ 500 → AdminApiExceptionHandler 的 AdminResponse.failure 文案安全展示 + 重试
      └─ 网络错误 → 安全错误 + 重试
  → add-star：POST /boot/dashboard?action=add-star
      ├─ status=success → 成功文案（服务端 message 含新 star id）；清空表单
      ├─ status=fail（"Star name cannot be empty." / "Birth year must be a valid non-negative number."）→ 失败文案，保留输入
      └─ 401 → 同 metadata
  → add-movie：POST /boot/dashboard?action=add-movie
      ├─ status=success → 成功文案（存储过程 message）
      ├─ status=fail（"Please fill in all required fields." / 年非法 / **"Movie already exists: ..."** 来自存储过程）→ 失败文案，保留输入
      └─ 401 → 同 metadata
  → 登出按钮 → GET /boot/dashboard/logout（204）→ clearEmployee() → /employee/login
  → 登出失败（401/网络）→ 安全错误提示，不清本地状态（对齐 LogoutButton 的 try/catch 语义）
```

**边界（必须写进测试的）**

| 场景 | 期望 |
| --- | --- |
| metadata 返回 `tables: []` | 显示 `"No metadata found."`，**不**显示空表格、**不**报错 |
| metadata 返回多表多列 | 每表一张 card；列顺序与响应一致；`column_type` 原样显示（如 `VARCHAR(10)`） |
| add-movie 业务失败（`status:"fail"`，HTTP 200） | 必须显示失败文案（**不得**因为 HTTP 200 就当作成功） |
| add-star `birth_year` 留空 | 允许（可选）；不被前端 `required` 拦住（legacy `dashboard.html:41` 的 birth_year 无 `required`） |
| add-movie 必填留空 | 前端不强制（legacy 无 `required`），由服务端 200 + `fail` 文案承担（**保持 parity，不新增校验**，与 #30 的 U-4 裁定同思路）。**Q-5 已裁定：add-star/add-movie 一律不加前端 `required`/格式校验** |
| 员工**登录**表单 `username`/`password` 留空 | 前端 `required` 拦下（**Q-5 已裁定：仅员工登录表单加 `required`**）。legacy `_dashboard.html:25,31` **无** `required` → 属**新增呈现差异**，必须在本计划、issue 跟踪与交付报告中如实标注（不得声称 parity） |
| 匿名访问 `/employee/dashboard` | 跳 `/employee/login`，不渲染任何仪表盘内容（AC-31-2） |
| 登出后立即访问 `/employee/dashboard` | `clearEmployee()` 使 `employee.status='anonymous'` → 跳登录页；**即使**后端 session 仍持有顾客身份（`user`）也不得进入仪表盘 |
| 员工与顾客同时登录（同一 session） | 员工登出**不得**把顾客状态一起清掉（D-31-1 的 `removeAttribute` 语义）；前端只清 employee 状态 |

## 5. 拟修改 / 新增文件清单（已批准；最终以 worker 实际 diff 为准）

> 本清单已随 §10/§11 的裁定定稿（主 agent 2026-10-15 批准）；§5.2 的删除项（Q-3）已获明确裁定。

> 精确路径；最终以 worker 的实际 diff 为准。标 **(新)** 者为新文件。

### 5.1 后端

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `src/main/java/com/fabflix/boot/admin/EmployeeController.java` | 改 | 新增 `@GetMapping("/api/boot/dashboard/logout")`：`session.removeAttribute("employee")` + `ResponseEntity.noContent().build()`（D-31-1）。**不使用 `invalidate()`**；**不做 302 重定向**（与顾客端点不同，理由见 §4.1） |
| `src/main/java/com/fabflix/boot/admin/AdminApiExceptionHandler.java` | **不改** | `assignableTypes` 已包含 `EmployeeController`（第 10 行） |
| `src/main/java/com/fabflix/boot/admin/EmployeeAuthenticationFilter.java` | **不改** | `/api/boot/dashboard` 前缀已覆盖新端点（第 18 行） |
| `src/main/java/com/fabflix/boot/admin/{AdminController,AdminService,AdminRepository,AdminResponse,Metadata*}.java` | **不改（预期）** | 契约保持；若 worker 认为必须改，**暂停并询问主 agent**。**N-31-9（已裁定）**：新 `logout` 端点与 `AdminController` 的类级 `@RequestMapping("/api/boot/dashboard")` **不冲突**（后者需精确路径 + `params=...`）→ 不得为此改动 `AdminController` |
| `src/main/java/com/fabflix/boot/session/*`、`src/main/java/com/fabflix/boot/auth/*` | **不改** | 只被复用 |
| legacy `src/main/webapp/**` | **不改、不删** | Non-goal（§3.2）；`FrontendApiMigrationTest` 钉死 |

### 5.2 前端

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `frontend/src/features/employee/employeeApi.js` | 新 | `loginEmployee({username, password})`（`URLSearchParams` → `POST /boot/employee-login`）、`logoutEmployee()`（`GET /boot/dashboard/logout`）、`fetchMetadata()`（`GET /boot/dashboard?action=metadata`）、`addStar(values)`、`addMovie(values)`（`URLSearchParams` + `action=...`）。端点常量与参数构造导出为纯函数以便测试（架构文档 §4） |
| `frontend/src/features/employee/EmployeeLoginPage.jsx` | 新 | 员工登录表单（`username`/`password`，**均 `required`**——Q-5 已裁定，属新增呈现差异；**无 reCAPTCHA**）、提交中禁用、`role="alert"` 错误、已认证回跳；`<h1>` 文案 = **`Employee login`**（Q-1 已裁定） |
| `frontend/src/features/employee/EmployeeDashboardPage.jsx` | 新 | metadata 区块（拉取 + 每表 card + **loading/空态/错误+重试** 完整状态机，复用 `components/states/`；Q-7 已裁定）、add-star 表单、add-movie 表单、各自动结果区块、**页内登出按钮**（Q-4）；`<h1>` 文案 = **`Employee dashboard`**（Q-1）；两个表单**不加** `required`/格式校验（Q-5） |
| `frontend/src/app/AppRoutes.jsx` | 改 | `employee/login`、`employee/dashboard` 改为显式真实路由；**删除 `placeholderRoute` 辅助函数**与两条占位映射（Q-3 已裁定） |
| `frontend/src/app/routeDefinitions.js` | 改 | **删除 employee 两条占位定义**；删除后 `routeDefinitions` 导出为空数组（`[]）且仅剩 `AppRoutes.jsx` 与 `App.test.jsx` 两个消费者——两者同批清理（Q-3 已裁定）；若 worker 发现仍有其它消费者，**暂停并询问主 agent** |
| `frontend/src/components/layout/AppShell.jsx` | **不改（Q-4 已裁定）** | 员工导航现状保留；登出按钮放仪表盘页内，**不动 `AppShell`**；**不得**把顾客 `LogoutButton` 的语义混用（顾客登出会 `invalidate()` 整个 session） |
| `frontend/src/App.test.jsx` | 改 | `responseBody(url)` 增加 **`/boot/employee-login`、`/boot/dashboard`（session/metadata/logout 三种形状）** 分派，并避免与 `/boot/cart` 的子串判断混淆（§6.2）；employee 路由真实页面断言；匿名员工重定向断言改为断言真实登录页的 `h1`/表单 label（Q-1）；删除 `it.each(routeDefinitions)`（Q-3） |
| `frontend/src/index.css` | 改（可能） | 员工页面样式（metadata 表格、表单布局）——**必须复用 `index.css` `:root` 的既有 token，不新建设计系统**（#32 D-32-4 同口径） |
| `frontend/src/features/placeholders/RoutePlaceholderPage.jsx` | **删（Q-3 已裁定）** | employee 占位是最后一个消费者（`routeDefinitions` 清空后无引用），删除它同时改善 #32 的覆盖率；**删除文件需在 worker 报告中逐项列出** |
| `frontend/src/features/auth/**` | **不改（预期）** | `RecaptchaField` **不复用**（D-31-2）；`LogoutButton` 是顾客专用 |

### 5.3 测试

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `src/test/java/com/fabflix/boot/admin/EmployeeControllerTest.java` | 改 | 保持既有两条登录断言；**新增** logout：① 已登录 → **204** 且 `employee` 属性被移除；② 顾客 `user` 属性**仍在**（验证 D-31-1 的“不销毁 session”语义）；③ 未登录经 `EmployeeAuthenticationFilter` → **401** + `{"status":"fail","message":"Employee authentication required."}`。**写法固定（B6 已闭合）**：`MockMvcBuilders.standaloneSetup(controller).addFilters(new EmployeeAuthenticationFilter()).build()`（裸 `standaloneSetup` 不注册 filter，照原写法 401 用例会得到 404/200）；已登录两条用例共享**同一个 `MockHttpSession`（同时含 `user` 与 `employee`）**并 `.session(shared)`；未登录用例不带 session。详见 §6.1 |
| `src/test/java/com/fabflix/boot/admin/AdminControllerTest.java`、`AdminServiceTest.java`、`EmployeeServiceTest.java`、`EmployeeAuthenticationFilterTest.java`、`src/test/java/com/fabflix/boot/session/SessionControllerTest.java` | **不改（回归护栏）** | 必须保持通过。**已知缺口（N-31-7，已裁定）**：不新增 `AdminRepositoryTest`（metadata 遍历 / `add_movie` 存储过程结果集解析仍**无直接测试**）；本 issue 不扩大范围，但必须作为**已知缺口**写入 issue 跟踪与交付报告，避免读者误判已覆盖 |
| `src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java` | **不改** | legacy 仍存在（#33 才改） |
| `frontend/src/features/employee/employeeApi.test.js` | 新 | URL/参数构造：`action=metadata`、`action=add-star`、`action=add-movie` 与字段映射、`URLSearchParams` body、`credentials` 由 `apiClient` 统一设置 |
| `frontend/src/features/employee/EmployeeLoginPage.test.jsx` | 新 | 成功提交 → 调用 `refreshEmployee`/`setEmployeeAuthenticated` 并导航；`status:"fail"` → `role="alert"` 文案；提交中禁用；**断言页面不渲染 reCAPTCHA**（D-31-2 的可测表达） |
| `frontend/src/features/employee/EmployeeDashboardPage.test.jsx` | 新 | metadata 多表渲染 / 空态 / 401 / 500 重试；add-star 成功与失败；add-movie 成功与业务失败（`status:"fail"` + 200）；登出调用与导航；请求 body 断言 |
| `frontend/src/features/employee/employeeFlow.test.jsx`（可选） | 新 | 页面级流程：`/employee/login` → 仪表盘 → metadata → 登出（`fetch` 按 URL 分派的有状态假服务），对齐 #30 的 `checkoutFlow.test.jsx` 做法 |
| `frontend/src/App.test.jsx` | 改 | 见 §5.2 |

**测试命令（必须由 worker 实际运行并报告）**：后端 `mvn --batch-mode verify`（含 JaCoCo 行覆盖率 85% 闸门，与 CI 同命令）；前端 `cd frontend && npm test`（`vitest run`）+ `npm run lint`（`oxlint src`）。

**用例数证据（N-31-1，已裁定）**：删除占位机制会造成前端用例数变化 → worker 必须报告**改动前/改动后**的 `npm test` 用例数（**当前基线 = 155 tests / 26 files**，来自 `docs/issues/issue-30-*.md` 记录的最近一次实测）与文件数，并说明差值来源（删除的 `it.each` 用例 vs 新增员工测试）。

### 5.4 文档

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `docs/plans/issue-31-migrate-employee-dashboard.md` | 本文件 | 计划 |
| `docs/issues/issue-31-migrate-employee-dashboard.md` | 新 | issue 跟踪（远端原文 + AC 跟踪 + 非目标） |
| `docs/architecture/frontend-migration-contracts.md` | 改（**Q-8 已裁定：要改**） | 补一条跨 issue 约定：「员工登出用 `removeAttribute`、顾客登出用 `invalidate()`；`/api/boot/dashboard*` 归员工 filter」；按架构文档既有结构写，**不写一次性实现细节**（#33 会再次触及会话与重定向） |
| `docs/logs/2026-10-15-request-31-32-33-*.md`（名称待定） | 实施后 | 工作流日志（本任务阶段**不创建**） |

## 6. 验收与测试计划

### 6.1 后端（MockMvc / Mockito）

> **仓库既有模式（docs-writer 已核实）**：`admin` 包的测试用 **`MockMvcBuilders.standaloneSetup(controller)`**（`AdminControllerTest.java:16,26,36`、`EmployeeControllerTest.java:16,28`），**不是** `@WebMvcTest`；全仓库 **无 `@SpringBootTest`**（已 grep：0 命中）。`EmployeeAuthenticationFilterTest` 直接驱动 filter 对象（scout A4）。新增测试应沿用 `standaloneSetup` 写法，避免引入新的测试基座。
> **覆盖率闸门**：CI 唯一构建步骤是 `mvn --batch-mode --no-transfer-progress verify`（`.github/workflows/ci.yml:36`），JaCoCo 在 `verify` 阶段强制 `BUNDLE / LINE / COVEREDRATIO ≥ 0.85`（`pom.xml:130-171`）。**#30 实施后最近一次实测：947 covered / 110 missed / 1057 total = 0.8959**（`docs/plans/issue-30-*.md` §13.1；docs-writer 未重跑）。新增 `logout` 方法（约 5-8 行）必须被测试触达，否则闸门余量被吃掉。据 oracle 复核：`947/0.85 = 1114.11` → 本批允许的**新增未覆盖行上限约 57 行**（#31 的 `logout` 约 6 行在此额度内，但 #31 与 #33 共享该额度，两者都必须坚持「新增/修改的后端类至少一条测试」）。
> **B6（已闭合）——logout 的 401 无法用裸 `standaloneSetup` 表达**：`MockMvcBuilders.standaloneSetup(controller)` **默认不注册任何 filter**，而 `EmployeeControllerTest.java:16,28` 现有用例均未加 filter。因此 logout 测试的**固定写法**是：
> `MockMvcBuilders.standaloneSetup(controller).addFilters(new EmployeeAuthenticationFilter()).build()`（该 filter 是无依赖 `OncePerRequestFilter`，可直接 `new`；`standaloneSetup` 支持 `addFilters(Filter...)`）。
> - 已登录用例：把 `employee` **与** `user` 一起写进**同一个** `MockHttpSession` 并 `.session(shared)`；断言 **204** + `employee` 被移除 + `user` **仍在**。
> - 未登录用例：不带 session，断言 **401** + body 含 `"Employee authentication required."`（filter 手写 JSON，**不是** `errorMessage` 形状，见 §4.1/R-31-2）。
> - 若不用 `addFilters`，401 用例会得到 404/200，或被迫删掉断言——**不得**这样实现。

必须覆盖的分支清单（**已定稿**）：

1. `EmployeeController.logout`：已登录 → 204（无 body）；`employee` 属性被移除；`user` 属性保留（同一 `MockHttpSession`）；未登录（带 `EmployeeAuthenticationFilter`）→ 401 + `Employee authentication required.`（写法见上方 B6 与 §5.3）。
2. 现有 `login` 两条分支（成功/失败）保持。
3. `EmployeeController` 若使用 `HttpSession` 参数，测试通过 `MockHttpServletRequest` 的 session 断言；无需 service 层单测（本 issue 不加 service 方法）。

### 6.2 前端（vitest + Testing Library）

沿用既有模式（架构文档 §7）：`MemoryRouter` + `AuthContext.Provider` 注入假会话（`employee` 可注入 `authenticatedSession`/`anonymousSession`/`failedSession`）、`vi.stubGlobal('fetch', ...)` 返回 `new Response(...)`、`afterEach` 里 `vi.unstubAllGlobals()`。若使用 `AppShell` 渲染，需注意 **`CartStatusProvider` 包裹**（架构文档 §4 的测试陷阱）。

- `employeeApi.test.js`：纯函数/请求形状断言（含 `action=` 查询参数，**这是本 issue 最易出错点**：后端用 `params="action=metadata"` 精确匹配）。
- `EmployeeLoginPage.test.jsx`：成功/失败/提交中/无 reCAPTCHA。
- `EmployeeDashboardPage.test.jsx`：metadata（多表/空/401/500+重试）、add-star（success/fail）、add-movie（success/fail 含"Movie already exists"类文案）、登出（成功/失败）、请求 body。
- `App.test.jsx`：`responseBody(url)` 增加 **`/boot/employee-login`**、**`/boot/dashboard`（session/metadata/logout 三种形状）** 与 `/boot/cart` 的分派；**注意现有实现用子串判断（`App.test.jsx:80-88`），必须避免 `/boot/dashboard*` 被 `/boot/cart` 或默认分支 `:87 return [{ name: 'Drama' }]` 误捕获**；employee 路由真实页面断言；匿名重定向断言改为断言真实登录页。

### 6.3 AC → 测试/命令 映射

| AC | 后端证据 | 前端证据 | 命令 |
| --- | --- | --- | --- |
| AC-31-1 | `EmployeeControllerTest`（login 既有 + **logout 204/属性移除**） | `EmployeeLoginPage.test.jsx`、`EmployeeDashboardPage.test.jsx`（登出）、（可选）`employeeFlow.test.jsx` | `mvn --batch-mode verify`；`npm test`；`npm run lint` |
| AC-31-2 | `EmployeeAuthenticationFilterTest`（既有 401 契约）+ logout 的 401 用例 | `App.test.jsx`（匿名 `/employee/dashboard` → 登录页） | 同上 |
| AC-31-3 | `AdminControllerTest`（既有 `table_name`/`column_name` 契约） | `EmployeeDashboardPage.test.jsx` + `employeeApi.test.js` | `npm test` |
| AC-31-4 | `AdminControllerTest`/`AdminServiceTest`（既有 fail 文案契约，回归护栏） | `EmployeeDashboardPage.test.jsx`（成功/失败四路径） | `npm test` |
| AC-31-5 | §6.1 全部 + JaCoCo 闸门 | §6.2 全部（页面级流程） | `mvn --batch-mode verify`；`npm test` |

### 6.4 未纳入自动化（如实声明）

- 真实浏览器 + 真实 MySQL + 真实 `JSESSIONID` 的员工 e2e——仓库无此设施（#32 才引入 Playwright）；本 issue 的替代定义见 AC-31-5。
- 真实存储过程 `add_movie` 的数据库往返（`AdminRepository` 无测试，scout A4）——本 issue 不改 repository，**不新增** repository 测试（**Q-6 已裁定：不在 #31 补 `AdminRepositoryTest`，但必须在 issue 跟踪与交付报告中记录为已知缺口**）。

## 7. 依赖

| 依赖 | 状态 | 证据 |
| --- | --- | --- |
| 共享认证 + API 基础设施（issue "Dependency"） | 已完成 | `frontend/src/api/apiClient.js`、`frontend/src/auth/AuthProvider.jsx`、`ProtectedRoute.jsx`（#35/#36）；后端 `EmployeeAuthenticationFilter`、`SessionController` |
| 路由壳 | 已完成 | `AppRoutes.jsx`、`routeDefinitions.js`（#34） |
| 员工后端接口（登录/metadata/add-star/add-movie） | 已存在 | `admin/` 包（`EmployeeController`、`AdminController`、`AdminService`、`AdminRepository`） |
| 员工登出端点 | **本 issue 新增**（D-31-1） | 现状不存在 |
| legacy 员工页面作为行为参考 | 必须保留到 #33 | `FrontendApiMigrationTest.java:33-34` 钉死 |

## 8. 约束

| # | 约束 | 来源 |
| --- | --- | --- |
| C-31-1 | **JaCoCo 行覆盖率闸门 ≥ 0.85（BUNDLE/LINE）**：任何新增/修改的后端类必须配测试；`logout` 方法必须被触达 | `.github/workflows/ci.yml:36`；`pom.xml:130-171` |
| C-31-2 | **全仓库无 `@SpringBootTest`**（已 grep：0 命中）；后端测试用 `standaloneSetup`/`@WebMvcTest`+`@MockBean`/纯 Mockito；不引入新的测试基座 | scout A4；docs-writer 复核 |
| C-31-3 | **`javax.servlet`（不是 `jakarta.servlet`）**；不得新增 servlet/JSP；`pom.xml` 保持 `jar` + `repackage` | `SpringBootOnlyArchitectureTest.java:12-38`；架构文档 §8 |
| C-31-4 | **session 语义**：登录 `changeSessionId()`；session 属性对象必须 `Serializable`（`EmployeeIdentity` 已满足）；**员工登出只用 `removeAttribute`，不 `invalidate()`** | `EmployeeController.java:30-31`、D-31-1、架构文档 §9 |
| C-31-5 | **不得删改 legacy `src/main/webapp/**`**（`FrontendApiMigrationTest.java:33-34` 钉死 `_dashboard.js`/`dashboard.js`） | Non-goal §3.2 |
| C-31-6 | **API 契约形状不变**：`AdminResponse`（`status`/`message`）+ `MetadataResponse`（`status`/`tables[].table_name`/`columns[].column_name`/`column_type`）；业务失败一律 **HTTP 200 + `status:"fail"`**，不是 4xx | 架构文档 §6；`AdminResponse.java`、`Metadata*` |
| C-31-7 | **表单 POST 用 `URLSearchParams` body**（`apiClient` 不设 Content-Type）；后端用 `@RequestParam` 表单编码 | 架构文档 §5；`AdminController.java:26-43` |
| C-31-8 | 前端目录/测试约定：`features/<name>/<Name>Page.jsx` + `<name>Api.js` + 同目录测试；状态复用 `components/states/` | 架构文档 §4 |
| C-31-9 | **`routeDefinitions.js` / `AppRoutes.jsx` / `App.test.jsx` 必须同步改**（架构文档 §3 的联动规则）；`responseBody(url)` 必须按 URL 分派（N13） | 架构文档 §3；`App.test.jsx:66-72` |
| C-31-10 | **不新增 npm 依赖**（本 issue 无需新依赖；Playwright/coverage 属 #32）；不引入 Spring Security/JWT | `frontend/package.json`；架构文档 §10 AD-1 |
| C-31-11 | 样式只用 `frontend/src/index.css` 的既有 `:root` token，不新建设计系统（与 #32 D-32-4 同口径） | D-32-4；`index.css:1-16` |
| C-31-12 | 单实例 session 假设（AD-2）：员工会话为 JVM 内存 session | 架构文档 §10 AD-2 |

## 9. 风险与回退方案

| # | 风险 | 影响 | 缓解 / 回退 |
| --- | --- | --- | --- |
| R-31-1 | **`logout` 语义被误实现为 `invalidate()`** → 顺带登出顾客会话 | 中 | D-31-1 显式写入 §4.1/§5.1；测试断言 `user` 属性仍在（§5.3） |
| R-31-2 | **员工 filter 的 401 是 `status/message` 形状**，与顾客的 `errorMessage` 不同；前端文案/分支若假设 `errorMessage` 会错 | 低-中 | §4.1 已记录；前端按 `ApiError.isUnauthorized` + `message` 处理（`apiClient.js:35-43` 已兜底），测试覆盖 401 |
| R-31-3 | **`action=` 查询参数遗漏** → 后端 `params="action=metadata"` 匹配失败，返回 405/404 | 中 | `employeeApi.test.js` 强制断言 query 参数（§6.2）；`AdminController` 三个端点都用 `params` 匹配 |
| R-31-4 | **legacy 员工页有 reCAPTCHA、React 页没有**（D-31-2 已批准）→ 行为差异（自动化填写门槛降低） | 低（已批准差异） | 如实记录为已知差异（本文件 + issue 跟踪 + 交付报告）；**不得**在 PR 中声称 parity |
| R-31-5 | **前端把 HTTP 200 一律当成功** → add-movie 业务失败被显示成成功 | 中 | §4.3 边界表 + 测试（`status:"fail"` + 200）；`AdminResponse` 语义（架构文档 §6） |
| R-31-6 | **`App.test.jsx:186-190` 断言 h1 文案 "Employee login"** 与新页面的 h1 不一致 → 测试红 | 低 | **已裁定（Q-1）**：登录页 h1 = `Employee login`、仪表盘 = `Employee dashboard`；worker 必须把 `App.test.jsx:186-190` 改为「匿名 `/employee/dashboard` → 落到**真实登录页**并断言其 `h1`/表单 label」（占位页将被删除，旧断言不再成立） |
| R-31-7 | **`it.each(routeDefinitions)` 在 `routeDefinitions` 变空数组时的行为未确认**（docs-writer 未运行 vitest） | 低-中 | **已闭合（oracle N-31-1）**：vitest 5 仅在“整个 suite 无测试”时报错，该 `describe` 还有约 20 个用例 → `it.each([])` 不报错，但属死代码；**已裁定删除该 `it.each` 与整个占位机制**。worker 改为显式真实页面断言，并报告用例数变化 |
| R-31-8 | **员工/顾客导航共存**：`AppShell` 同时渲染两套 nav；员工登录后顾客 nav 仍在 | 低 | 属既有设计；本 issue 不重构导航（**Q-4 已裁定**：登出按钮放仪表盘页内 → `AppShell` 与 `AppShell.test.jsx` **不改**） |
| R-31-9 | **覆盖率闸门变红**：新增后端代码未配测试 | 中 | C-31-1；worker 提交前跑 `mvn --batch-mode verify` |
| R-31-10 | 顾客 `logoutCustomer()` 会 `invalidate()` 整个 session → 员工会被顺带登出 | 低（既有行为） | 本 issue 不改顾客端点（§3.2）；如实记录为既有语义 |

**回退方案**：本 issue 的后端改动只有 `EmployeeController` 的一个新方法（追加式、无 schema/依赖变化）＋前端新增页面；若出现问题，回退该分支即可，**不影响 #30 已合并的功能与 legacy**。删除 legacy 的动作**不在本 issue**（#33），因此 #31 不存在破坏性回退风险。

## 10. 已裁定问题（原未决问题 Q-1..Q-8；主 agent，2026-10-15）

> **本节全部问题已裁定**，依据 oracle 报告 §1.3/§5 与主 agent 裁定；逐条回填见 §11。
> 表格保留「裁定」列作为实施依据；「为什么需要裁定」列为历史记录。

| # | 问题 | 主 agent 裁定（2026-10-15，已全部接受 oracle 建议） | 实施要点（已写入节号） |
| --- | --- | --- | --- |
| Q-1 | **员工登录页与仪表盘的 `<h1>` 文案**（现 `App.test.jsx:186-190` 断言 `'Employee login'`） | **登录页 = `Employee login`；仪表盘 = `Employee dashboard`** | §4.2/§5.2；worker 必须把 `App.test.jsx:186-190` 改为「真实登录页 + `h1`/form label 断言」（占位页删除后旧断言不成立）→ §9 R-31-6 |
| Q-2 | 登录成功后用 `refreshEmployee()` 还是 `setEmployeeAuthenticated(user)` | **用 `await refreshEmployee()`**（身份来源唯一，与 `LoginPage.jsx:49` 顾客做法一致）；**不使用** `setEmployeeAuthenticated` 自造身份形状 | §4.2 |
| Q-3 | **是否在 #31 删除已无消费者的占位机制** | **删除**：`routeDefinitions.js` 的两条 employee 条目、`App.test.jsx` 的 `it.each(routeDefinitions)`、`RoutePlaceholderPage.jsx`、`AppRoutes.jsx` 的 `placeholderRoute`。依据 oracle：`it.each([])` 在 vitest 5 下**不报错**（仅整个 suite 无测试才抛），但确属死代码；删除同时改善 #32 覆盖率 | §5.2/§5.3；**worker 必须报告改动前/后 `npm test` 用例数**（当前基线 155）→ §9 R-31-7 |
| Q-4 | **员工登出入口放哪里** | **仪表盘页内**按钮（最小侵入，不动 `AppShell` 双导航）；`AppShell` 页脚仅在顾客已认证时渲染 `LogoutButton`（`AppShell.jsx:70-72`）→ 员工无出口，属现状缺口，需写入 §1.2/§4.2 | §4.2/§5.2 |
| Q-5 | **add-star/add-movie 前端是否加 `required`/格式校验** | **不加**（保持 parity；错误由服务端 200 + `status:"fail"` 承担）；**但员工登录表单的 `username`/`password` 加 `required`**（legacy `_dashboard.html:25,31` 无 `required` → 属**新增呈现差异**，如实记录） | §4.3 边界表/§5.2 |
| Q-6 | 是否顺便补 `AdminRepositoryTest` | **不在 #31 补**（不扩大范围）；但必须把「metadata 遍历 / `add_movie` 存储过程解析仍无直接测试」写为**已知缺口**（scout A4） | §5.3/§6.4 |
| Q-7 | 员工仪表盘是否需要完整状态机 | **完整 loading / 空态 / 错误 + 重试**（复用 `components/states/`）；⚠️ 若测试经 `AppShell` 渲染，必须包 `CartStatusProvider`（`AppShell.jsx:79-85`）并对 `/boot/cart` 分派 | §5.2/§6.2 |
| Q-8 | 是否在架构文档补「员工登出 vs 顾客登出」约定 | **要补**：「员工登出用 `removeAttribute`、顾客登出用 `invalidate()`；`/api/boot/dashboard*` 归员工 filter」 | §5.4 |

## 11. Oracle 建议与主 agent 决定（已回填）

> 依据：oracle 只读审查报告 `.pi/artifacts/oracle-issues-31-32-33-plans.md`（#31 章节）+ 主 agent 裁定（2026-10-15）。
> oracle 结论：「#31：**可批准**（补齐 B6 的 filter 接线写法即可）——其余为定稿性建议，不改变已批准范围」。
> **下文所有「裁定 = 接受」均为主 agent 的明确决定**；oracle 建议本身不自动生效。

| 来源 | oracle 建议（摘要） | 主 agent 裁定 | 已写入本文档 |
| --- | --- | --- | --- |
| **B6** | logout 的 401 用例无法用裸 `standaloneSetup` 表达（默认不注册 filter）→ 用 `addFilters(new EmployeeAuthenticationFilter())`；已登录用例共享同一 `MockHttpSession`（含 `user` + `employee`）；未登录断言 401 + filter 手写 JSON | **接受（必须）** | §5.3、§6.1、§6.2 |
| N-31-1 | 删除 employee 占位机制（两条 `routeDefinitions`、`App.test.jsx` 的 `it.each`、`RoutePlaceholderPage.jsx`、`placeholderRoute`）；报告改前/后 `npm test` 用例数（当前 155） | **接受** | §3.1、§5.2、§5.3、§10 Q-3、§9 R-31-7 |
| N-31-2 | h1 文案 `Employee login`/`Employee dashboard`；`App.test.jsx:186-190` 改为真实登录页断言；`responseBody(url)` 必须补 `/boot/employee-login`、`/boot/dashboard`（session/metadata/logout 三种形状）分派并与 `/boot/cart` 子串判断区分 | **接受** | §4.2、§5.2、§6.2、§10 Q-1 |
| N-31-3 | 登录后 `await refreshEmployee()` | **接受** | §4.2、§10 Q-2 |
| N-31-4 | add-star/add-movie **不加**前端校验；登录表单加 `required`（记为呈现差异） | **接受** | §4.3、§5.2、§10 Q-5 |
| N-31-5 | 登出按钮放仪表盘页内；在 §1.2 记现状缺口（员工无登出入口） | **接受** | §3.1、§4.2、§5.2、§10 Q-4 |
| N-31-6 | 仪表盘 loading/空态/错误+重试；测试经 `AppShell` 需包 `CartStatusProvider` 并分派 `/boot/cart` | **接受** | §5.2、§6.2、§10 Q-7 |
| N-31-7 | 不在 #31 补 `AdminRepositoryTest`，但记录为已知缺口 | **接受** | §5.3、§6.4、§10 Q-6 |
| N-31-8 | 架构文档补员工/顾客登出语义与 filter 归属 | **接受** | §5.4、§10 Q-8 |
| N-31-9 | 新端与 `AdminController` 的 `@RequestMapping("/api/boot/dashboard")` **不冲突** → 写入 §4.1，避免 worker 误改 `AdminController` | **接受** | §4.1、§5.1 |
| N-31-10 | 注明员工浏览器级路径由 #32 的 `e2e/employee.spec.js` 覆盖，避免 PR 过度宣称 AC-31-5 | **接受** | §2（AC-31-5 行）、§6.4 |
| Q-1..Q-8 | 见 oracle 报告 §5 裁定建议表与 §1.3（含：h1 文案、`refreshEmployee()`、删除占位机制、页内登出入口、不加表单校验、不补 `AdminRepositoryTest`、完整状态机、架构文档增补） | **全部接受**（= 上表 N-31-1..N-31-8 的对应项） | §10（已转为「已裁定问题」表） |
| *其他* | oracle 报告 §1.4「确认无误」的后端契约/授权边界/legacy 基准/覆盖率闸门等事实核对 | **采信作为计划事实基础**（无需改文） | §1.2/§4.1/§6.1 |
| *本批修订* | oracle 报告未要求改变 #31 的验收范围；本次仅补入实施口径 | **接受：AC-31-1..AC-31-5 范围不变** | §2、§12 |

> **规则提醒**：oracle 只读；它提出的建议不自动生效。本文所有「已批准/已裁定」字样均来自主 agent 的裁定，并由 docs-writer 回填。

## 12. 状态（批准时记录 → 已由 §13 更新为实施完成）

- **文档状态（批准时）：已批准（主 agent，2026-10-15）— 可交 worker 实现。** 依据 oracle 报告：B6 已闭合、N-31-1..N-31-10 与 Q-1..Q-8 已全部裁定（§10/§11）；**本批修订未改变 AC-31-1..AC-31-5**。
- **测试状态（批准时）：未运行任何测试。** 本文引用的后端覆盖率 947/1057 = 0.8959 与前端基线 155 tests / 26 files 均来自**已有文档记录（#30 最后一次实测）**，不是批准时新跑的结果。
- **实现前状态（批准时）**：`frontend/src/features/employee/**` 尚不存在；`GET /api/boot/dashboard/logout` 尚不存在；占位机制与 legacy 员工页均未改动。
- **批准时「仍由 worker 确认」的点**：现已在 **§13.4** 全部回填（终验数字）或在 **§13.5** 记为残余项/未验证项。
- **下一步（现已完成）**：worker 已按本文档实现 #31（顺序 #31 → #32 → #33）→ 三个 reviewer 并行审查（无 Blocker）→ 同一 worker 修复并复验 → docs-writer 依实际 diff 与测试输出建立 §13 与工作流日志 `docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`。
- **只写范围**：本文档由 docs-writer 维护；docs-writer **未**修改任何源码/测试/配置，**未**运行测试或构建（§13 的数字来自主 agent 终验实测与本轮只读复核产物），**未**创建或修改 GitHub issue/PR，**未**提交或推送。

## 13. 实现结果与计划差异（主 agent 终验后回填，2026-10-15）

> **状态：实施完成，已通过主 agent 终验（2026-10-15）；已提交（`092147a`/`fb65eda`）、已推送、**PR #42 已于 2026-10-16 合并（merge commit `d618b64`）**、**issue #31 已关闭**；CI 首跑（head `fb65eda`）两个 job 均 SUCCESS，合并后 `main`（`d618b64`）双 job 亦 SUCCESS；里程碑 Phase 2 已关闭（§13.5）。**
> 证据来源：主 agent 亲跑的终验命令与输出（§13.4 标注「主 agent 实测」）、主 agent 核验的 PR #42 合并结果与 CI 运行结果（§13.5）、本轮 docs-writer 的**只读**复核（`git status`/`git diff`、`target/surefire-reports/*`、`target/site/jacoco/jacoco.csv`、`unzip -l target/fablix.jar`、源码与测试文件内容）。
> 本节的数字**均来自实际产物或主 agent 实测报告**；docs-writer 未运行任何测试/构建命令。

### 13.1 实际改动文件（相对 §5 计划清单）

**新增（`frontend/src/features/employee/`，全部落地）**

| 路径 | 状态 | 说明 |
| --- | --- | --- |
| `frontend/src/features/employee/employeeApi.js` | 新（与计划一致） | 端点常量 + `dashboardRequestPath(action)` 纯函数 + `loginEmployee`/`logoutEmployee`/`fetchMetadata`/`addStar`/`addMovie`；`URLSearchParams` body |
| `frontend/src/features/employee/EmployeeLoginPage.jsx` | 新（与计划一致） | `h1` = `Employee login`；`username`/`password` **均 `required`**；无 reCAPTCHA；成功后 `await refreshEmployee()` |
| `frontend/src/features/employee/EmployeeDashboardPage.jsx` | 新（与计划一致，含 1 处裁定差异） | metadata 区块 + add-star/add-movie 表单 + 页内登出；metadata 三态见 §13.2 第 2 条 |
| `frontend/src/features/employee/employeeApi.test.js` | 新 | 6 个用例：端点/`action=` 参数/字段映射 |
| `frontend/src/features/employee/EmployeeLoginPage.test.jsx` | 新 | 8 个用例（含 `required` + **不渲染 reCAPTCHA**、`state.from` 回跳） |
| `frontend/src/features/employee/EmployeeDashboardPage.test.jsx` | 新 | 16 个用例（metadata 多表/空态/重试/401、add-star、add-movie、200+`status:"fail"`、登出成功/失败、**add-star/add-movie 的 401 与网络失败**追加用例、axe） |
| `frontend/src/features/employee/employeeFlow.test.jsx` | 新（计划标「可选」→ 已实现） | 1 个用例：`login → dashboard → metadata → add-star → logout` 全流程（有状态假服务） |
| `src/main/java/com/fabflix/boot/admin/EmployeeController.java` | 改（按 D-31-1） | 新增 `@GetMapping("/api/boot/dashboard/logout")`：`session.removeAttribute("employee")` + `204`（`ResponseEntity.noContent()`），**未用 `invalidate()`** |

**修改**

| 路径 | 说明 |
| --- | --- |
| `frontend/src/app/AppRoutes.jsx` | `employee/login`、`employee/dashboard` 改为真实页面；删除 `placeholderRoute` 与 4 个 `routeDefinitions` 导入 |
| `frontend/src/App.test.jsx` | 删除 `it.each(routeDefinitions)`；`responseBody(url)` 增加 `/boot/employee-login`、`/boot/dashboard/logout`、`/boot/dashboard/session`、`/boot/dashboard`（按「最具体优先」排序，避免被默认电影分支捕获）；新增真实员工页/重定向断言与 2 条 shell axe 断言 |
| `frontend/src/index.css` | 员工页样式（`.employee-dashboard*`、`.metadata-card*`、`.metadata-table`、`.logout-control`）+ 480px 断点必要规则；删除死 CSS `.route-placeholder`/`.route-placeholder__id`（**保留 `.route-placeholder__eyebrow`**，被员工页复用） |
| `src/test/java/com/fabflix/boot/admin/EmployeeControllerTest.java` | 新增 2 条 logout 用例（204 + `employee` 移除 + `user` 保留；未登录 401），写法按 B6：`standaloneSetup(...).addFilters(new EmployeeAuthenticationFilter())` + 共享 `MockHttpSession` |

**删除（Q-3 裁定项，均已落地）**

| 路径 | 说明 |
| --- | --- |
| `frontend/src/app/routeDefinitions.js` | 删除（见 §13.2 第 3 条：**不是**改成空模块） |
| `frontend/src/features/placeholders/RoutePlaceholderPage.jsx` | 删除（最后一个消费者已消失） |

**未改动（与计划一致）**：`AppShell.jsx`、`AdminController`/`AdminService`/`AdminRepository`/`AdminResponse`/`Metadata*`、两个 authentication filter、`AdminApiExceptionHandler`、`apiClient.js`、`LogoutButton`、legacy `src/main/webapp/**`（本 issue 未触碰，退役属 #33）。

### 13.2 与计划文本的 4 处已裁定差异（必须记录，避免计划与实现矛盾）

| # | 计划原文 | 实际实现 | 裁定与理由 |
| --- | --- | --- | --- |
| 1 | §4.3 边界表 + Q-5：「add-star/add-movie **不加** 前端 `required`」（依据 legacy `dashboard.html` 无 `required`） | add-star 的 `star_name`、add-movie 的 `movie_title`/`movie_year`/`movie_director`/`movie_star_name`/`movie_genre_name` **均带 `required`**（`map`: 可选字段 `birth_year`/`movie_star_birth_year` 无 `required`） | **oracle 原判断有误**：legacy `src/main/webapp/dashboard.html` 的对应字段**实有 `required`**（该文件已随 #33 退役，证据只能取自 git 历史）；计划文本的「legacy 无 required」前提不成立 → **恢复 `required`** 以保持 parity。其余口径不变：**不做**格式校验（年/生日非法由服务端 `200 + status:"fail"` 承担）。测试断言见 `EmployeeDashboardPage.test.jsx` 的「marks the legacy-required fields and keeps the optional ones optional」 |
| 2 | §5.2 + Q-7：metadata 区块**复用 `components/states/`** 的 loading/空态/错误+重试 | metadata 三态改为**行内非标题状态**：`<p role="status">Loading database metadata...</p>`、`<p role="alert">{message}</p>` + `Try again` 按钮、空态普通 `<p>No metadata found.</p>` | 复用 `components/states/` 会在页内引入**第二个 `<h1>`**（`PageState`/`ErrorState` 渲染页级标题），与「每页单一 `h1`」的 a11y 断言（#32 axe 规则）冲突 → 改为行内状态。**行为覆盖不变**：loading/空态/错误+重试仍有测试（`retries a failed metadata request`、`shows the empty metadata state...`） |
| 3 | §5.2：`routeDefinitions.js` **删除两条 employee 占位定义**，导出变空数组 `[]` | `frontend/src/app/routeDefinitions.js` **整个文件删除** | 删除两条后该模块**无任何消费者**（`AppRoutes.jsx` 与 `App.test.jsx` 均同批清理）→ 保留空模块属死代码；直接删文件同时改善 #32 的覆盖率分母。计划中「若 worker 发现仍有其它消费者，暂停并询问」条件未触发 |
| 4 | §5.1/§4.1 未规定插件版本与 enforcer 相位 | `pom.xml` 的 `maven-resources-plugin` **保留显式 `<version>3.3.1</version>`**；enforcer `requireFrontendDist` 执行相位为 **`validate`**（计划写「绑定在 `process-resources` 之前」）；`SpaFallbackConfig` 资源链用 **`resourceChain(false)`**（计划未指定） | 本 `pom.xml` **无 `<parent>`**，dependencyBOM 只管理依赖、不管理插件版本 → 显式版本是消除 `plugin.version is missing` 警告与锁定 `copy-resources` 行为所必需（删除会引入警告且依赖 Maven 隐式绑定）；`validate` 早于 `process-resources`，满足「尽早失败」的意图；`resourceChain(false)` 在**不启用无界资源缓存**的前提下仍需 `.addResolver(...)` 注册（Spring 文档语义：`addResolver` 隐含要求 chain 已配置）→ 保留 chain 调用但关缓存，避免长跑进程内存无界增长 |

### 13.3 其他实现细节（计划未逐条规定、实现后确认）

- **`App.test.jsx` 的 URL 分派顺序**：`/boot/dashboard/logout` → `/boot/dashboard/session` → `/boot/dashboard`（最具体优先），且在 `if (url.includes('/boot/cart'))` **之前**不会互相误捕获；`responseBody(url)` 仍保留默认电影行分支。
- **员工 401 语义复用顾客模式**：`EmployeeDashboardPage` 的 `endExpiredSession(error)` 在 `error.isUnauthorized` 时 `clearEmployee()` + 跳 `/employee/login`；**登出失败**则只显示错误、**不清本地状态**（对齐 `LogoutButton`）。
- **前端用例数变化（计划 §5.3 要求报告）**：改动前基线 **155 tests / 26 files**（#30 文档记录）→ 改动后 **195 tests / 30 files**（主 agent 终验实测，本地 Node 24 与 Node 22 容器两次一致）。差值来源：删除 `it.each(routeDefinitions)` 的 2 条参数化用例（占位机制移除）＋新增员工 4 个测试文件的 31 个用例＋`App.test.jsx` 新增 4 条（2 真实页面/重定向 + 2 shell axe）。**说明**：`npm test` 的同一用例在 axe 断言下仍是 1 个 `it`，不额外计数。
- **员工浏览器级路径**：由 #32 的 `frontend/e2e/employee.spec.js` 覆盖（登录 → dashboard → metadata → add-star → 登出），已在 Node 22 容器与本地各跑通（见 §13.4）；本 issue 不据此宣称 AC-31-5 的浏览器级覆盖由自己提供（N-31-10 口径不变）。
- **`AdminRepository` 缺口**：`metadata` 遍历与 `add_movie` 存储过程结果集解析**仍无直接测试**（计划的已知缺口，N-31-7）；本 issue 未新增 `AdminRepositoryTest`。

### 13.4 终验证据（AC-31-1..AC-31-5）

| AC | 证据 | 结果 | 来源 |
| --- | --- | --- | --- |
| AC-31-1 | `EmployeeControllerTest`（login 2 条既有 + **logout 2 条新增**）；`EmployeeLoginPage.test.jsx`(8)、`EmployeeDashboardPage.test.jsx`(16)、`employeeFlow.test.jsx`(1)；e2e `employee.spec.js` 登入/登出 | 后端 `mvn --batch-mode --no-transfer-progress verify` → **BUILD SUCCESS**，`Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`；前端 **195 passed / 30 files**；e2e **12 passed**（含 employee 路径） | 主 agent 实测；docs-writer 只读复核 surefire 汇总 = 127/0/0/0 |
| AC-31-2 | `EmployeeAuthenticationFilterTest`（既有）+ logout 的 401 用例（`Employee authentication required.`）；`App.test.jsx`「redirects anonymous employees to the real employee login page」；e2e 未认证路径 | 同上（后端 127 全绿；前端 195 全绿） | 主 agent 实测 + docs-writer 复核 diff |
| AC-31-3 | `EmployeeDashboardPage.test.jsx`（每表一 card、`column_name`/`column_type` 原样）+ `employeeApi.test.js`（必带 `action=metadata`）；生产 smoke 员工登录 → `action=metadata` | 前端 195 全绿；生产 smoke（#33）`/employee/dashboard` 深链 200 text/html、员工 metadata `status:success` | 主 agent 实测 |
| AC-31-4 | `EmployeeDashboardPage.test.jsx`：add-star 成功/失败、add-movie 成功/`200 + status:"fail"`、401、网络失败；`EmployeeControllerTest`/`AdminControllerTest`/`AdminServiceTest` 契约回归 | 前端 195 全绿；后端 127 全绿 | 主 agent 实测 |
| AC-31-5 | 后端 MockMvc + 前端页面级/流程级测试 + **#32 的浏览器级 employee smoke** | 后端 127 / 前端 195 / e2e 12 全绿；后端 JaCoCo `All coverage checks have been met.`，BUNDLE LINE **968 covered / 110 missed / 1078 total = 0.8980**（闸门 0.85）；`EmployeeController` JaCoCo 行 **0 missed / 13 covered** | 主 agent 实测；docs-writer 复核 `jacoco.csv` 汇总与 `EmployeeController` 行 |

### 13.5 残余风险、未验证项与后续事项

| # | 项 | 状态 |
| --- | --- | --- |
| 1 | **提交 / 推送 / PR / 合并 / issue 关闭 / CI**（原「待 PR 后回填」项，**已回填 2026-10-15；合并结果 2026-10-16 回填**） | **已提交**：实现 `092147a`、文档 `fb65eda`；分支 `codex/issue-31-32-33-m2-completion`（HEAD `fb65eda`）已推送；**PR #42 已于 2026-10-16 合并**（merge commit **`d618b64`**；`mergedAt` = 2026-10-16T04:57:00Z）→ 合并后远端分支 `codex/issue-31-32-33-m2-completion` **已删除**（origin 仅剩 `main`），本地 `main` 已同步到 `d618b64`；**issue #31 已关闭**（GitHub 依 PR 正文的 `Closes #31` 自动关闭，状态 `CLOSED`）；**CI**：head `fb65eda` 首跑与合并后 `main`（`d618b64`）上 `Build and test (Java 11)`、`Frontend (lint, test, coverage, build, e2e)` **均 SUCCESS**；**里程碑 `Phase 2 — React Frontend Migration`（#2）已关闭**（`open_issues` = 0、`closed_issues` = 10、`closed_at` = 2026-10-16T05:00:27Z）。来源：主 agent 核验的 GitHub 结果（docs-writer 未直接访问 GitHub） |
| 2 | **手工颜色对比度** | 无法在 jsdom 自动验证（#32 `color-contrast` 规则禁用，见 #32 §13）；员工页新增的 `.form-success`/`.form-error`/`.logout-control [role='alert']` 暗色色值按 ≥4.5:1 选择，**仍需人工在浏览器确认** |
| 3 | `AdminRepository`（metadata 遍历 / `add_movie` 存储过程解析） | **已知缺口**（计划 Q-6 裁定不补），本 issue 后仍无直接测试 |
| 4 | 真实浏览器 + 真实 MySQL 的员工存储过程往返 | **未自动化**（#32 的 e2e 全部用 `page.route` mock API）；生产 smoke 只验证了 metadata 与登录/登出链路（#33 §13） |
| 5 | 员工登出后顾客会话仍有效的浏览器级验证 | 由后端单测断言（`user` 属性保留）覆盖；**未**在 e2e/生产 smoke 中显式验证「同一浏览器同时持有顾客与员工身份后员工登出」 |
| 6 | 前端 axe 断言的实际生效规则集 | `region` 规则按渲染树区分（见 #32 §13.3），`color-contrast` 全局禁用 → 覆盖强度见 #32 §13.5 |

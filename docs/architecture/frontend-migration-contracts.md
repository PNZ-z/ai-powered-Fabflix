# 前端迁移约定与架构约束（已核实）

> 本文档记录 **Phase 2 React 前端迁移**中跨 issue 持续有效的既有约定与由测试强制的约束。
> 内容全部来自当前代码与测试文件的可核实证据；每条标注证据位置与强制程度：
>
> - **【测试强制】**：有测试会在违规时失败（改代码必须一起改测试）。
> - **【既有模式】**：仓库内一致遵循的写法，无自动化强制，新代码应沿用以免风格分裂。
>
> 本文档**不包含**任何未经批准的方案决策。Issue #30 的计划/实施结果见
> `docs/plans/issue-30-migrate-cart-payment-confirmation-flows.md`（状态：**实施完成，已通过主 agent 终验（2026-10-14）；已提交并推送（`d6b3e07`/`eeb935f`），PR #40 为 OPEN、未合并；PR #40 审查后的 UI/缓存修复提交 `6b637a4` 已提交、待主 agent 推送**，差异与实测见其 §13.1）。
> 若与本文档冲突，以代码和测试为准，并更新本文档。

## 1. 迁移进度与分工基线

- 已完成迁移（源码与占位状态可证）：路由壳 #34（`frontend/src/app/`）、API+认证 #35/#36（`frontend/src/features/auth/`、`frontend/src/api/apiClient.js`）、discovery #37（`frontend/src/features/home/`）、movie list #38、detail #39（`frontend/src/features/movies/`）、**cart / checkout / confirmation #30（`frontend/src/features/cart/`、`frontend/src/features/checkout/`）**。
- 仍为占位：employee login / dashboard（`frontend/src/app/routeDefinitions.js` 的 `employeePublicRoutes`/`employeeProtectedRoutes`，文案指向 #31）。
- 占位实现在 `frontend/src/app/AppRoutes.jsx:19-27`（`placeholderRoute`）+ `frontend/src/features/placeholders/RoutePlaceholderPage.jsx`。【既有模式】

## 2. legacy `src/main/webapp` 保留策略 【测试强制】

- `src/main/webapp` 下的 `.html`/`.js` 页面**不是运行时资源**：`pom.xml` 打包为 `jar`，且 `src/main` 下没有 `src/main/webapp` 的静态资源映射（grep `addResourceHandlers`/`WebMvcConfigurer` 无结果）。它们作为**业务行为参考 / oracle 依据**保留。
- `src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java`：
  - `frontendNoLongerCallsLegacyServletApiPaths`：`src/main/webapp` 下所有 `.js`/`.html` 不得再出现 `api/<非 boot>` 形式的 URL。
  - `criticalPagesUseTheirSpringBootEndpoints`（第 24-35 行）：`login.js`、`main.js`、`movieList.js`、`single-movie.js`、`single-star.js`、`shoppingCart.js`、`payment.js`、`_dashboard.js`、`dashboard.js` **必须仍存在**且分别包含指定 `api/boot/...` 字符串。
- **推论（操作规则）**：删除、移动或重命名这些 legacy 文件会同时破坏上述测试；任何清理 legacy 的工作必须与该测试文件一并修改，且需要单独批准（不属于 #30 范围）。
- **#30 已批准的单一例外（B5 方案 a / #30 计划 D-7）**：允许修改 `src/main/webapp/payment.js`——在提交前先 `GET api/boot/checkout/token`，并把 token 随四个表单字段一起 POST。该测试只做 `contains` 断言（`api/boot/cart`、`api/boot/payment` 字符串保留即可），因此新增 token 调用**不会**破坏它。**除 `payment.js` 外，legacy 文件在 #30 内仍不得删改。**
- **#30 实施结果（已核实）**：`payment.js` 在**页面加载时获取一次 token 并复用**（`payment.js` 的 `loadCheckoutToken()` / 模块级 `checkoutToken`；`api/boot/cart`、`api/boot/payment`、`api/boot/checkout/token` 字符串均保留），`FrontendApiMigrationTest` 在 #30 实施后仍绿；`src/main/webapp` 仅此一文件被改。

## 3. 路由定义与路由测试的联动【测试强制】

- `frontend/src/app/routeDefinitions.js` 导出 `customerPublicRoutes`/`customerProtectedRoutes`/`employeePublicRoutes`/`employeeProtectedRoutes`/`routeDefinitions` 等；每条含 `path`、`samplePath`、`title`、`description`。
- `frontend/src/App.test.jsx:107` 使用 `it.each(routeDefinitions)('renders the $title placeholder at $samplePath')` 断言**每个**路由在 `samplePath` 渲染出标题等于 `route.title` 的占位标题（#30 实施前行号为 85，参数化断言本身未变）。
- `frontend/src/App.test.jsx:172` 起 `it.each(['/cart', '/checkout', '/confirmation'])` 断言匿名访问这三条路由重定向到顾客登录页（#30 前只有 `/cart`，位于第 127-131 行）。
- **推论（操作规则）**：把某条路由从占位替换为真实页面时，必须同时更新 `routeDefinitions.js` 与该 `it.each` 断言（移除该条，或改为真实页面的断言），否则测试变红。`routeDefinitions` 当前被 `AppRoutes.jsx` 与 `App.test.jsx` 共同消费。
- **#30 已裁定的操作方式（N6，供后续 issue 参考）**：把 cart/checkout/confirmation 三条从 `customerProtectedRoutes` **删除**，并在 `AppRoutes.jsx` 内改为显式 `<Route>`；`it.each(routeDefinitions)` 仍由 employee 两条占位覆盖，**不会出现空数组**，因此不必删除该参数化测试。
  - **#30 实施结果（已核实）**：`customerProtectedRoutes` 现为 `[]`（导出与聚合保持不变），`routeDefinitions` 仍含 employee 两条，`it.each` 不空转；`App.test.jsx` 增加三条路由的真实页面断言与 `['/cart','/checkout','/confirmation']` 匿名重定向参数化断言。
- **`responseBody(url)` 分派要求（N13）**：`App.test.jsx` 的通用 `fetch` stub `responseBody(url)`（#30 实施后位于第 81 行；迁移前为第 62-67 行）对未知 URL 一律返回电影行数据；**任何新页面进入 `App.test.jsx` 的渲染路径时，必须扩展 `responseBody` 按 URL 分派**，否则断言会拿到错误形状的数据而失去意义。#30 已按此加入 `cart`/`checkout-token`/`confirmation` 三条分派。

## 4. 页面 / API 模块 / 测试的三件套约定【既有模式】

沿用 `frontend/src/features/movies/` 与 `frontend/src/features/auth/` 的写法：

| 角色 | 约定 | 示例 |
| --- | --- | --- |
| 页面 | `features/<name>/<Name>Page.jsx`，默认导出 | `MovieListPage.jsx`、`StarDetailPage.jsx` |
| API | `features/<name>/<name>Api.js`：模块级端点常量 + 参数构造函数 + 薄 `apiRequest` 封装，导出纯函数便于测试 | `movieListApi.js`、`movieDetailApi.js`、`customerAuthApi.js` |
| 测试 | 与源文件同目录 `<Name>.test.jsx` / `<name>Api.test.js` | `MovieListPage.test.jsx`、`movieListApi.test.js` |
| 状态展示 | 复用 `frontend/src/components/states/`：`LoadingState`、`ErrorState`（带 retry）、`EmptyState`、`PageState`、`NotFoundPage` | `MovieListPage.jsx`、`MovieDetailPage.jsx` |
| 异步状态机 | `status` in `loading`/`success`/`error` + `error` 对象 + `retryCount` 触发 `useEffect` 重跑；effect 内用 `active` 标志防卸载后置状态 | `MovieDetailPage.jsx` |
| 404 语义 | 后端 404（`ApiError.status === 404`）渲染专用 not-found 页，不做 retry（重试无法修复） | `MovieDetailPage.jsx` |
| 金额展示 | **一律经共享模块 `features/cart/money.js` 的 `toCents`/`formatCents` 以整数分求和/格式化**；不在页面内做浮点累加，不复算服务端权威金额 | `money.js`；`CartPage.jsx`、`PaymentPage.jsx`、`ConfirmationPage.jsx` |

- **金额一律走整数分工具【既有模式】（#30 提取）**：服务端下发的是 JSON number（`BigDecimal` 序列化，如 `7.50` → `7.5`），前端先把每个金额转成整数分（`toCents = Math.round(Number(amount) * 100)`）再求和，最后 `formatCents` 格式化为 `$0.00`；**服务端是金额的唯一权威来源**，前端只做展示口径换算，不把换算结果回传。影响范围：任何展示/合计金额的新页面。反例（**不要复制**）：legacy `src/main/webapp/shoppingCart.js` 的浮点累加。
- **列表类 GET 的进程内缓存（TTL + stale-while-revalidate + in-flight 去重）【既有模式】（#30 PR #40 审查后修复提取）**：`frontend/src/features/movies/movieListCache.js` 是仓库内第一个列表页缓存实现，后续列表页若需要同类行为应沿用同一套约定：① **键 = 规范化后端请求路径**（`movieListApi.js` 的 `movieListRequestPath`：endpoint + 后端实际收到的 query，即 `buildMovieListParams` 的输出），使“改变请求的 URL 状态”不会复用同一份数据、“后端看不到的 URL 状态”也不会分裂缓存；② **TTL**（`MOVIE_LIST_CACHE_TTL_MS = 60_000`）内命中则**直接渲染且不发请求**；③ 过期走 **SWR**：先渲染旧数据、后台 revalidate，**失败保留旧数据**（不进入 error 态）；④ 同 key 的并发请求共享同一 Promise（in-flight 去重，顺带消除 React StrictMode 双挂载引发的第二次请求）；⑤ 错误态的 `retry` 必须**先清条目**再重试，否则“重试”会在 TTL 内重放旧结果；⑥ 需要跨浏览器 Back/Forward 触发的页面重建存活的状态（滚动偏移）放 `sessionStorage` 并用独立前缀（`fabflix.movieListScroll:`，与 `fabflix.lastMovieQuery` 区分），而**列表数据本身故意不持久化**；读取要 `try/catch` 降级（隐私模式下 layout 不可用时从顶部打开）。**边界**：进程内内存缓存，刷新即失效、不跨标签页；**不替代服务端权威数据**（管理员新增/修改最多 TTL 内不可见）。实现位置：`movieListCache.js`（导出 `readMovieListEntry` / `loadMovieList` / `clearMovieListEntry` / `clearMovieListCache` / `readMovieListScroll` / `saveMovieListScroll`）、`MovieListPage.jsx`（缓存命中、SWR、retry 清条目、首次成功渲染后恢复滚动）。**测试陷阱**：模块级缓存的生命周期超出组件与单个测试，使用它的测试必须在 `beforeEach`/`afterEach` 调用 `clearMovieListCache()`（参照 `App.test.jsx`），否则出现顺序依赖/伪绿。
- **React StrictMode 下 effect 双执行【既有模式】（#30 提取）**：入口 `frontend/src/main.jsx` 用 `<StrictMode>` 包裹 `App`，开发模式下 effect 会挂载→清理→再挂载，**每次都会重新发请求**。对于**一次性副作用**（如 `GET /boot/checkout/token` 会轮换服务端 token），双执行会让服务端与页面持有的 token 可能不一致（旧 token 在 `checkoutState==completed` 下会触发旧订单重放，或在 `pending` 下被拒为 expired），因此这类页面必须**幂等或提供显式恢复入口**。影响范围：所有取 token/一次性凭据、发写请求或做增量的 mount 副作用。参考实现：`PaymentPage.jsx`（`active` 标志防卸载后置状态；先 GET cart、非空才取 token；失败/过期时用 “Start checkout again” → `retry()` 重取 cart+token；token 未取得前提交按钮 `disabled`）。
- **跨页面共享的顾客 UI 状态用 `AppShell` 内的全局 provider【既有模式】（#30 PR #40 审查后修复提取）**：`frontend/src/components/layout/AppShell.jsx` 把 header 与 `<Outlet/>` 路由页一起包在 `CartStatusProvider` 里，因此**导航栏徽标与页面内控件读同一份状态**。约定：① **跨页面可见的顾客状态**（当前实例：购物车件数）由此类 provider 持有，不在 `AppShell` 与各页面间各自取一份；② **派生展示值尽量在渲染期从 server 数据算出而不本地推测**——徽标数字 = `GET /boot/cart` 各行 **`quantity` 之和（件数，不是行数）**，页面变更后调 provider 的 `requestCountRefresh()` 重新读服务端，而非前端自己 +1/−1；③ **服务端失败不清屏/不归零**：仅 401（会话过期）清零，其它错误保留上次值；④ **状态与产生它的身份绑定**：计数存 `{ sessionKey, count }`，`sessionKey = customer:<email>` / `anonymous`，登出/换顾客后旧值自然不再被读取（无需额外清零 effect）；⑤ **同一时刻只允许一个瞬时 UI（toast）**：状态只存一个对象，每次通知**新建对象**以重置定时器（对象身份作 effect 依赖），偏好持久化用 `localStorage` + `try/catch` 降级。实现位置：`features/cart/CartStatusProvider.jsx`、`features/cart/cartStatusContext.js`（context + `useCartStatus`，与 `authContext.js` 同形）、`features/cart/CartBadge.jsx`（视觉层 `aria-hidden`，真实件数走 Cart 链接的 `aria-label`）。**边界**：徽标为内存态、刷新即重取、不跨标签页同步；jsdom 无布局，toast 定位与窄屏通栏需人工在浏览器确认。测试陷阱：渲染用到 `AddToCartButton`/`CartPage` 的页面级测试必须包 `CartStatusProvider`（测试里用 `AuthContext.Provider`(anonymous) 可使 provider 不发取数请求，从而不改变既有请求次数断言，参照 `CartPage.test.jsx`、`checkoutFlow.test.jsx`）。

## 5. API 客户端契约【既有模式】

`frontend/src/api/apiClient.js`：

- `API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'`；`apiRequest(path)` 会自动加前缀。
- 统一设置 `Accept: application/json`、`credentials: 'include'`（依赖 `JSESSIONID` cookie 认证）。
- **不**自动设置 `Content-Type`：表单 POST 由调用方传 `URLSearchParams` body（对应后端 `@RequestParam` / `@ModelAttribute` 表单编码）；见 `customerAuthApi.js`。
- 失败统一抛 `ApiError`（`name`、`.status`、`.data`、`.isUnauthorized`），消息优先取响应体的 `errorMessage` 再取 `message`。
- 网络不可达 → `ApiError('Unable to reach the Fabflix server.')`；响应不可解析 → `ApiError('The Fabflix server returned an unreadable response.')`。
- 204 响应返回 `null`。

## 6. 后端 → 前端的错误与响应约定【既有模式】

- 业务失败返回 **HTTP 200 + `{"status":"fail","message":<英文文案>}`**（`OperationResponse`，`@JsonInclude(NON_NULL)`）；参数校验失败返回 **400 `{"errorMessage": <message>}`**（`CommerceApiExceptionHandler`）；未认证返回 **401 `{"errorMessage":"Authentication required."}`**（`CustomerAuthenticationFilter`）。
- 前端不要以 HTTP 状态码区分业务失败，必须检查 `status` 字段；`401` 通过 `ApiError.isUnauthorized` 识别。
- 认证边界：`CustomerAuthenticationFilter` 保护所有 `/api/boot/**`，仅放行 `login`、`employee-login`、`dashboard*`、`health`（`shouldNotFilter`）。
- JSON 字段命名混用约定：cart 相关用 snake_case（`movie_id`/`movie_title`，`CartItem` 上显式 `@JsonProperty`），其他多数用 camelCase。**新增字段命名需与所属端点既有风格一致。**

## 7. 前端测试设施现状【测试强制 / 事实】

- 测试运行器：vitest（`npm test` = `vitest run`），环境 jsdom，`setupFiles: './src/test/setup.js'`（jest-dom + `afterEach(cleanup)`）。
- 页面测试模式：`render(<MemoryRouter initialEntries={[...]}><Routes>…</Routes></MemoryRouter>)` + `vi.stubGlobal('fetch', vi.fn(...))` 返回 `new Response(JSON.stringify(body), {status, headers})`；`afterEach` 里 `vi.unstubAllGlobals()` 与 `window.sessionStorage.clear()`。
- `frontend/src/App.test.jsx` 走 `AuthContext.Provider` 注入假会话（`authenticatedSession`/`anonymousSession`/`failedSession`），并为每个测试 stub 全局 `fetch`。
- **没有** Playwright / Cypress / Selenium 等 e2e 设施（`frontend/package.json` 仅 vitest + jsdom）。后端测试仅 surefire + `@WebMvcTest` MockMvc（全仓库无 `@SpringBootTest`）。
- 后端测试模式：`@WebMvcTest(SomeController.class)` + `@MockBean` 依赖 + `@Import(CommerceApiExceptionHandler.class)`；会话通过 `.sessionAttr("user", new AuthenticatedCustomer("viewer@example.com"))` 注入；表单通过 `.param(...)`。
- **`@WebMvcTest` 的跨请求 session 语义（#30 R2-B2，重要陷阱）**：`.sessionAttr(...)` 每次 `perform` 都会在该请求自己的 `getSession()` 上写属性，**每个请求得到新的 `MockHttpSession`**；只有 `.session(sharedMockHttpSession)` 才复用同一 session。**任何需要跨请求保持状态的断言（如 token 签发→提交→重放）必须先把属性写入一个 `MockHttpSession` 实例，再对两次请求都 `.session(shared)`**；`.sessionAttr(...)` 只适用于单一请求用例（如 401）。
- **CI 与后端覆盖率闸门【测试强制】（B1，由 #30 计划 C-13/C-14 提取）**：`.github/workflows/ci.yml` 的唯一构建步骤是 `mvn --batch-mode --no-transfer-progress verify`（第 36 行）；`pom.xml` 的 JaCoCo 插件在 `verify` 阶段强制 `BUNDLE / LINE / COVEREDRATIO ≥ 0.85`（第 111-171 行，含 `excludes` 白名单）。**任何新增/修改的后端类都必须配测试**（DTO getter 经一次 JSON 序列化断言即可覆盖），否则 PR 的 CI 会变红。当前基线（`target/site/jacoco/jacoco.csv`，最近一次 `verify` 产物）：**873 covered / 984 total ≈ 0.8872**，未覆盖余量约 **43 行**。**#30 实施后实测（2026-10-14）：947 covered / 110 missed / 1057 total = 0.8959**，闸门通过（后续 issue 应以最新产物为准）。
- **CI 不跑前端【事实】（B1）**：`npm test`（`vitest run`）与 `npm run lint`（`oxlint src`）**不在** CI 流程内，属**手动验证门**；任何交付报告必须给出实际命令与结果，不得声称未运行的门已通过。

## 8. 架构守卫测试【测试强制】

`src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java`：

- 源码中不得出现 `jakarta.servlet`、`@WebServlet`、`extends HttpServlet`（本项目 `javax.servlet` 由 Spring Boot 提供）。
- `pom.xml` 必须保持 `jar` 打包 + `repackage`，且不得引入 `jakarta.servlet-api`。
- 不得新增 servlet / JSP（仓库中无 `*.jsp`）。

## 9. 会话 / 认证依赖

- 顾客登录会调用 `request.changeSessionId()`（`CustomerAuthController.java:53-54`），即**登录后 session id 轮换**；登录前的匿名 session 与登录后不是同一个。
- `AuthenticatedCustomer.getUsername()` 存放的是**顾客 email**；`PaymentService.placeOrder` 以 email 查 `customers.id`。
- 开发环境依赖 Vite 同源代理把 `/api` 转发到后端（`frontend/vite.config.js`，`frontend/.env.example` 的 `FABFLIX_BACKEND_TARGET`）；仓库内无 CORS / SameSite 配置。
- **无 Spring Security、无 CSRF token**：`pom.xml` 无 `spring-boot-starter-security`，`src/main` 下无 `csrf`/`SameSite`/`CookieSerializer` 配置。任何新增的 cookie 认证写操作都继承这一现状（CSRF 属 #30 明确排除项，作为残余风险记录）。
- **session 存储对象必须 `Serializable`【既有模式】（#30 R2-N1 提取）**：写入 `HttpSession` 的对象实现 `Serializable`（`AuthenticatedCustomer.java:3-6`、`EmployeeIdentity.java:3-6`，均带 `serialVersionUID = 1L`）。原因：Spring Boot 内嵌 Tomcat 的 `StandardManager` 在应用关闭/重启时会持久化 session（`SESSIONS.ser`），不可序列化的 session 属性会产生 `NotSerializableException` 告警并导致该 session 丢失。**新增任何 session 属性对象（如 #30 的 `checkoutOrder` → `OrderSummary`/`OrderLine`）必须遵循。**

## 10. 跨 issue 决策记录【已批准】

> 本节记录经用户/主 agent 确认、超单次 issue 生效的决策；不含未批准的方案。

| # | 决策 | 依据 / 确认者 | 影响范围 |
| --- | --- | --- | --- |
| AD-1 | **认证路线 C：网页端继续 session-based 认证**（`JSESSIONID` cookie + `CustomerAuthenticationFilter`）。**JWT 不作为网页端认证机制**，仅保留给**未来 Spring AI agent 平台的独立 trust boundary**；#30/#31 不得引入 Spring Security / JWT 依赖；任何 JWT 工作必须**另立 issue + 计划 + oracle 审查**。 | 用户已确认；#30 计划 D-10 / C-15 | #30、#31 及后续网页端 issue |
| AD-2 | **部署假设：单 JVM 实例**。`compose.yaml` 只定义 `mysql` 服务（第 1-24 行），`pom.xml` 无 `spring-session`/Redis，session 为 JVM 内存。依赖 session 内存语义的设计（如 #30 的 checkout token 幂等重放）只在该假设下成立；如未来改为多实例 / Spring Session，相关保证必须重新评估并更新本条。 | 主 agent 已核实（#30 U-6） | 所有使用 session 状态的 issue |

# 前端迁移约定与架构约束（已核实）

> 本文档记录 **Phase 2 React 前端迁移**中跨 issue 持续有效的既有约定与由测试强制的约束。
> 内容全部来自当前代码与测试文件的可核实证据；每条标注证据位置与强制程度：
>
> - **【测试强制】**：有测试会在违规时失败（改代码必须一起改测试）。
> - **【既有模式】**：仓库内一致遵循的写法，无自动化强制，新代码应沿用以免风格分裂。
>
> 本文档**不包含**任何未经批准的方案决策。各 issue 的计划/实施结果见：
> `docs/plans/issue-30-migrate-cart-payment-confirmation-flows.md`（**实施完成，已通过主 agent 终验（2026-10-14）；已提交并推送（`d6b3e07`/`eeb935f`），PR #40 为 OPEN、未合并**）、
> `docs/plans/issue-31-migrate-employee-dashboard.md`、`docs/plans/issue-32-add-responsive-styling-accessibility-frontend-tests-and-ci.md`、`docs/plans/issue-33-integrate-production-react-build-and-retire-legacy-frontend.md`
> （**#31/#32/#33：实施完成，已通过主 agent 终验（2026-10-15）；已提交（`092147a`/`fb65eda`）、已推送、PR #42 为 OPEN、未合并；CI 首跑（head `fb65eda`）两个 job 均 SUCCESS**，工作流日志 `docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`）。
> 若与本文档冲突，以代码和测试为准，并更新本文档。
>
> **本轮（2026-10-15，#31/#32/#33）更新摘要**：§1 迁移进度已收尾；§2 legacy 已退役（守卫测试替代钉死测试）；§3 占位/路由联动机制已随占位机制删除而失效；§6 补员工/顾客登出语义与 filter 归属；§7 测试设施/CI 事实全部重写（Playwright/axe/coverage/Node 22 job）；§8 新增退役守卫；§9 补员工登出；**§11/§12 为新增的跨 issue 约定**（SPA fallback、前端测试设施与 CI）。

## 1. 迁移进度与分工基线

- 已完成迁移（源码可证）：路由壳 #34（`frontend/src/app/`）、API+认证 #35/#36（`frontend/src/features/auth/`、`frontend/src/api/apiClient.js`）、discovery #37（`frontend/src/features/home/`）、movie list #38、detail #39（`frontend/src/features/movies/`）、cart / checkout / confirmation **#30**（`frontend/src/features/cart/`、`frontend/src/features/checkout/`）、**employee login/dashboard #31**（`frontend/src/features/employee/`）。
- **不再有占位路由**：`frontend/src/app/routeDefinitions.js` 与 `frontend/src/features/placeholders/RoutePlaceholderPage.jsx` **已删除**（#31，Q-3 裁定）；所有路由在 `frontend/src/app/AppRoutes.jsx` 中显式声明。【测试强制】（`App.test.jsx` 不再引用 `routeDefinitions`）。
- **M2 已收尾（#33）**：生产 React 构建经 Maven `frontend` profile 打入可执行 jar，SPA fallback 由 `SpaFallbackConfig` 提供（§11）；legacy `src/main/webapp/**` 已退役（§2）；Docker Compose 提供 `app` 服务（多阶段构建，运行时非 root）。
- **迁移完成后的「新增页面」操作规则**：新页面在 `AppRoutes.jsx` 显式注册（公共路由或放入 `ProtectedRoute`），并同步在相关页面级测试/`App.test.jsx` 中按 URL 分派 `fetch`（§3 的 N13 规则仍适用）。

## 2. legacy `src/main/webapp` 已退役【测试强制】（#33 D-33-3）

- **现状（2026-10-15，已提交）**：`src/main/webapp/**` 的 **21 个 git 跟踪文件已删除并已提交**（实现提交 `092147a`；`git ls-files src/main/webapp` = **0 行**）。`.html`/`.js` legacy 页面**不再是仓库内容、也不是行为参考**。
- **强制方式（替代原「钉死 legacy」测试）**：`src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java` 已重写为**退役守卫**双保险：
  1. `legacyWebappDirectoryIsRemoved()`：`assertFalse(Files.exists("src/main/webapp"))`；
  2. `noLegacyFrontendFilesRemain()`：**若**目录存在，则遍历断言其中无 `.html`/`.js`。
  - 原 `criticalPagesUseTheirSpringBootEndpoints`（要求 9 个 legacy 文件仍存在）与 `Files.walk` 缺失目录的旧写法均已删除——**不得恢复**。
- **操作规则（新）**：禁止在 `src/main/webapp` 下新增页面/脚本；新增前端内容一律放 `frontend/`（React）。**删除/移动**源码或构建配置时，必须同步检查是否有测试/文档引用已被删除的 legacy 路径（现无此类引用，`grep` 可验证）。
- **两处曾指向 legacy 页面的重定向已改为 SPA 落点【测试强制】（#33 D-33-7）**：顾客非 JSON 登出 `302` 目标 `/login.html` → **`/login`**（`CustomerAuthController`，`CustomerAuthControllerTest`）；`GET /api/boot/jump-back` 的 `302` 目标 `/movieList.html?<query>` → **`/movies?mode=fulltext&<query>`**（`SearchController`，`SearchControllerTest`）。**`mode=fulltext` 不可省**：`lastMovieQuery` 只由全文搜索写入，缺此参数 React 列表页会落到 browse 模式并丢弃 `query`。

<details>
<summary>历史记录：M2 期间（#30 及之前）的 legacy 保留策略（已失效，仅供回溯）</summary>

- `src/main/webapp` 下的 `.html`/`.js` 曾作为**业务行为参考 / oracle 依据**保留（非运行时资源：jar 打包、无静态资源映射）。
- `FrontendApiMigrationTest` 曾以两个测试钉死：(a) legacy `.js`/`.html` 不得出现 `api/<非 boot>` 形式的 URL；(b) `login.js`、`main.js`、`movieList.js`、`single-movie.js`、`single-star.js`、`shoppingCart.js`、`payment.js`、`_dashboard.js`、`dashboard.js` **必须仍存在**且包含指定 `api/boot/...` 字符串。
- #30 曾批准单一例外：修改 `src/main/webapp/payment.js` 预先获取 `/api/boot/checkout/token`。

</details>

## 3. 路由声明与路由测试的联动【测试强制】

- **现状（2026-10-15）：`frontend/src/app/routeDefinitions.js` 已删除**，`AppRoutes.jsx` 内显式声明全部路由（#31 Q-3）；`App.test.jsx` 不再有 `it.each(routeDefinitions)` 参数化断言。
- **推论（新操作规则）**：新增/移除路由时只需同步：`AppRoutes.jsx` + 页面级测试 +（如会进入 `App.test.jsx` 渲染路径）`responseBody(url)` 分派；**不再需要**维护任何 `routeDefinitions` 占位表。
- **`responseBody(url)` 分派要求（N13，仍强制）**：`frontend/src/App.test.jsx` 的通用 `fetch` stub `responseBody(url)` 对未知 URL 一律返回电影行数据；**任何新页面进入 `App.test.jsx` 的渲染路径时，必须扩展 `responseBody` 按 URL 分派**，否则断言会拿到错误形状的数据而失去意义。**分派必须按「最具体优先」排序**：#31 实施中已按 `/boot/dashboard/logout` → `/boot/dashboard/session` → `/boot/dashboard` 的顺序，且不得让 `/boot/dashboard*` 被 `/boot/cart` 或默认分支误捕获。
- **匿名重定向参数化断言仍有效【测试强制】**：`App.test.jsx` 用 `it.each(['/cart','/checkout','/confirmation'])` 断言匿名访问这三条顾客路由重定向到顾客登录页；#31 另加了员工侧断言（匿名 `/employee/dashboard` → 真实员工登录页并断言 `h1`/`required` 字段）。新增受保护路由时应把路径加入对应参数化清单。
- **历史记录（已失效）**：M2 期间曾用 `routeDefinitions.js` 的占位定义 + `it.each(routeDefinitions)('renders the $title placeholder at $samplePath')` 钉死路由标题；把某条路由从占位换成真实页面时必须同时改 `routeDefinitions.js` 与参数化断言——该机制已随占位机制一并删除。

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
- 认证边界（现状，2026-10-15）：`CustomerAuthenticationFilter` 保护所有 `/api/boot/**`，仅放行 `login`、`employee-login`、`dashboard*`、`health`（`shouldNotFilter`）；**`/api/boot/dashboard*`（含 `/api/boot/dashboard/session`、`/api/boot/dashboard/logout`）归 `EmployeeAuthenticationFilter` 保护**，其 401 body 是**手写的** `{"status":"fail","message":"Employee authentication required."}`（**不是** `errorMessage` 形状）。
- **员工登录/登出与顾客登录/登出的语义差异【测试强制】（#31 D-31-1）**：
  - 员工登录：`POST /api/boot/employee-login` → 成功时 `changeSessionId()` + `session.setAttribute("employee", EmployeeIdentity)`，返回 200 `AdminResponse`（**不校验 reCAPTCHA**）。
  - 员工登出：`GET /api/boot/dashboard/logout` → **只 `session.removeAttribute("employee")`，不 `invalidate()`**，返回 **204**。理由：同一 session 可能同时持有顾客身份（`user`），`invalidate()` 会把顾客一并登出（`EmployeeControllerTest` 断言 `user` 属性保留）。
  - 顾客登出：`GET /api/boot/logout` → **`session.invalidate()`**；`Accept` 含 `application/json` → 204，否则 `302 → /login`（**已不再是 `/login.html`**）。
  - 因此：前端不能把「登出」统一写成同一个调用；员工登出复用顾客 `LogoutButton` 会**误杀顾客会话**。
- JSON 字段命名混用约定：cart 相关用 snake_case（`movie_id`/`movie_title`，`CartItem` 上显式 `@JsonProperty`），其他多数用 camelCase。**新增字段命名需与所属端点既有风格一致。**

## 7. 前端测试设施与 CI 现状【测试强制 / 事实】（2026-10-15，#32/#33 实施后重写）

- 测试运行器：vitest（`npm test` = `vitest run`），环境 jsdom，`setupFiles: './src/test/setup.js'`（jest-dom + `afterEach(cleanup)`）。当前规模：**30 个测试文件 / 195 个用例**。
- 页面测试模式：`render(<MemoryRouter initialEntries={[...]}><Routes>…</Routes></MemoryRouter>)` + `vi.stubGlobal('fetch', vi.fn(...))` 返回 `new Response(JSON.stringify(body), {status, headers})`；`afterEach` 里 `vi.unstubAllGlobals()` 与 `window.sessionStorage.clear()`。
- `frontend/src/App.test.jsx` 走 `AuthContext.Provider` 注入假会话（`authenticatedSession`/`anonymousSession`/`failedSession`/`employeeSession`），并为每个测试 stub 全局 `fetch`。
- **e2e：【已具备】Playwright（仅 chromium）**（#32 D-32-1）：`frontend/playwright.config.js` + `frontend/e2e/*.spec.js`（login/browsing/cart/employee，共 6 个用例 × 2 个 viewport = 12 次执行）；**API 全用 `page.route` mock，不依赖后端/MySQL**；`webServer` 用 `npm run build && npm run preview -- --host 127.0.0.1 --port 4173 --strictPort`。命令 `npm run test:e2e`。
- **vitest 与 Playwright 必须隔离【测试强制】**：`frontend/vitest.config.js` 的 `test.exclude` 显式包含 `**/e2e/**` 并**重述 vitest 默认排除项**（覆写 `exclude` 会替换默认值）；否则 `vitest run` 会收集 `e2e/*.spec.js` 并在 `import { test } from '@playwright/test'` 处失败。新增 spec 目录时**必须同步** `exclude`/`playwright.config.js` 的 `testDir`。
- **a11y：【已具备】`axe-core` 直连 + 自建 helper**（#32 Q-1，**不引入 `vitest-axe`**）：`frontend/src/test/axe.js` 导出两个 helper —— `expectNoA11yViolations(container)`（页面级；因页面在测试中单独渲染、没有 `AppShell` 的 `<main>`，**禁用 `region` 规则**）与 `expectNoShellA11yViolations(container)`（包含自身 landmark 的 shell 渲染树，**启用 `region`**）。两个 helper **均禁用 `color-contrast`**：jsdom 无布局/无颜色计算，**对比度必须人工在浏览器确认**（不得声称已自动覆盖）。
- **覆盖率【测试强制】**：`@vitest/coverage-v8`；`frontend/vitest.config.js` 的 `coverage.include = ['src/**/*.{js,jsx}']`、`exclude = ['src/main.jsx','src/test/**','**/*.test.*']`、**`thresholds.lines = 88`**（仅 lines 有阈值）。命令 `npm run test:coverage`；实施终验实测 **Lines 92.35%（846/916）**。**不得扩大 exclude 以人为抬分**；未被测试导入的源文件（如 `src/App.jsx`）**计入分母**。
- 后端测试模式：surefire + MockMvc（全仓库无 `@SpringBootTest`）。既有两套写法：`@WebMvcTest(SomeController.class)` + `@MockBean`（如 commerce/auth/search 包），以及 **`MockMvcBuilders.standaloneSetup(controller)`**（`admin` 包，如 `AdminControllerTest`/`EmployeeControllerTest`）；表单通过 `.param(...)`，会话通过 `.session(...)`/`.sessionAttr(...)`。
- **standalone MockMvc 不注册 filter 的陷阱【测试强制】（#31 B6）**：`standaloneSetup(controller)` **默认没有任何 filter**：需要验证 401/鉴权时必须显式 `.addFilters(new EmployeeAuthenticationFilter())` 等，否则请求会直达 controller（得到 200/404 而非 401）。示例：`EmployeeControllerTest.employeeMvc()`。
- **`@WebMvcTest` 的跨请求 session 语义（#30 R2-B2，重要陷阱）**：`.sessionAttr(...)` 每次 `perform` 都会在该请求自己的 `getSession()` 上写属性，**每个请求得到新的 `MockHttpSession`**；只有 `.session(sharedMockHttpSession)` 才复用同一 session。**任何需要跨请求保持状态的断言（如 token 签发→提交→重放、或「登出只移除 employee 而保留 user」）必须先把属性写入一个 `MockHttpSession` 实例，再对请求 `.session(shared)`**；`.sessionAttr(...)` 只适用于单一请求用例（如 401）。
- **CI 与后端覆盖率闸门【测试强制】**（`.github/workflows/ci.yml`）：后端 job 的唯一构建步骤是 `mvn --batch-mode --no-transfer-progress verify`；`pom.xml` 的 JaCoCo 插件在 `verify` 阶段强制 `BUNDLE / LINE / COVEREDRATIO ≥ 0.85`（含 `excludes` 白名单）。**任何新增/修改的后端类都必须配测试**，否则 PR 的 CI 会变红。**#31/#32/#33 实施后实测（2026-10-15）：968 covered / 110 missed / 1078 total = 0.8980**（未覆盖余量约 **~106 行**，但不要把余量当作“可以不写测试”）；`Tests run: 127`。
- **CI 前端 job【已新增，#32 D-32-1/D-32-2/Q-2】**：`ci.yml` 现在有一个独立的 `frontend` job（`timeout-minutes: 20`、`actions/setup-node@v4` + `node-version: 22` + npm 缓存指 `frontend/package-lock.json`、`working-directory: frontend`），步骤 `npm ci → lint → test → test:coverage → build → npx playwright install --with-deps chromium → test:e2e`，全部阻塞。**后端 job 与 `timeout-minutes: 15` 未变**。命令详情见附录 §12。

## 8. 架构守卫与退役守卫测试【测试强制】

`src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java`：

- 源码中不得出现 `jakarta.servlet`、`@WebServlet`、`extends HttpServlet`（本项目 `javax.servlet` 由 Spring Boot 提供）。
- `pom.xml` 必须保持 `jar` 打包 + `repackage`，且不得引入 `jakarta.servlet-api`。
- 不得新增 servlet / JSP（仓库中无 `*.jsp`）。
- **`SpaFallbackConfig` / `SpaFallbackTest` 不引入 servlet 依赖**（只依赖 Spring MVC 的 `WebMvcConfigurer`/`PathResourceResolver`）→ 上述守卫不受影响。

`src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java`（**#33 重写，语义反转**）：

- 从「钉死 legacy 文件存在且含指定 API 字符串」改为「**legacy 已退役**」：目录不存在 + （若存在则）无 `.html`/`.js`（§2）。
- **推论**：以前“删 legacy 必红”的约束**已失效**；现在反过来 —— **恢复任何 legacy 页面会合入失败**。

## 9. 会话 / 认证依赖

- 顾客登录会调用 `request.changeSessionId()`（`CustomerAuthController.java:53-54`），即**登录后 session id 轮换**；登录前的匿名 session 与登录后不是同一个。
- `AuthenticatedCustomer.getUsername()` 存放的是**顾客 email**；`PaymentService.placeOrder` 以 email 查 `customers.id`。
- 开发环境依赖 Vite 同源代理把 `/api` 转发到后端（`frontend/vite.config.js`，`frontend/.env.example` 的 `FABFLIX_BACKEND_TARGET`）；仓库内无 CORS / SameSite 配置。**生产/容器部署不再是“开发代理”模式**：React 产物由 Spring Boot 同源提供（§11），API 与页面同域，因此不需要 CORS 配置；`base` 仍为 `'/'`。
- **Vite 环境变量是构建期内联**【既有模式】（#33 D-33-5）：`VITE_RECAPTCHA_SITE_KEY`（以及任何 `VITE_*`）在 `npm run build` 时被**内联进产物**，**运行期注入无效**；生产镜像必须在构建前设定。对比：`FABFLIX_DB_*`/`FABFLIX_BOOT_PORT`/`SPRING_PROFILES_ACTIVE` 是 Spring Boot 运行期变量。
- **无 Spring Security、无 CSRF token**：`pom.xml` 无 `spring-boot-starter-security`，`src/main` 下无 `csrf`/`SameSite`/`CookieSerializer` 配置。任何新增的 cookie 认证写操作都继承这一现状（CSRF 属 #30 明确排除项，作为残余风险记录）。
- **session 存储对象必须 `Serializable`【既有模式】（#30 R2-N1 提取）**：写入 `HttpSession` 的对象实现 `Serializable`（`AuthenticatedCustomer.java:3-6`、`EmployeeIdentity.java:3-6`，均带 `serialVersionUID = 1L`）。原因：Spring Boot 内嵌 Tomcat 的 `StandardManager` 在应用关闭/重启时会持久化 session（`SESSIONS.ser`），不可序列化的 session 属性会产生 `NotSerializableException` 告警并导致该 session 丢失。**新增任何 session 属性对象（如 #30 的 `checkoutOrder` → `OrderSummary`/`OrderLine`）必须遵循。**

## 10. 跨 issue 决策记录【已批准】

> 本节记录经用户/主 agent 确认、超单次 issue 生效的决策；不含未批准的方案。

| # | 决策 | 依据 / 确认者 | 影响范围 |
| --- | --- | --- | --- |
| AD-1 | **认证路线 C：网页端继续 session-based 认证**（`JSESSIONID` cookie + `CustomerAuthenticationFilter`）。**JWT 不作为网页端认证机制**，仅保留给**未来 Spring AI agent 平台的独立 trust boundary**；#30/#31 不得引入 Spring Security / JWT 依赖；任何 JWT 工作必须**另立 issue + 计划 + oracle 审查**。 | 用户已确认；#30 计划 D-10 / C-15 | #30、#31 及后续网页端 issue |
| AD-2 | **部署假设：单 JVM 实例**。`compose.yaml` 只定义 `mysql` 服务（第 1-24 行），`pom.xml` 无 `spring-session`/Redis，session 为 JVM 内存。依赖 session 内存语义的设计（如 #30 的 checkout token 幂等重放）只在该假设下成立；如未来改为多实例 / Spring Session，相关保证必须重新评估并更新本条。 | 主 agent 已核实（#30 U-6） | 所有使用 session 状态的 issue |

## 11. 生产静态资源与 SPA fallback 约定【测试强制】（#33 D-33-2/Q-3/N-33-1/N-33-2）

> 适用：任何新增前端路由、静态资源目录、Maven profile 或反向代理相关的改动。

- **实现位置**：`src/main/java/com/fabflix/boot/web/SpaFallbackConfig.java`（`WebMvcConfigurer`，`@Order(Ordered.LOWEST_PRECEDENCE)`；`registry.addResourceHandler("/**").addResourceLocations("classpath:/static/").resourceChain(false).addResolver(new SpaResourceResolver())`）。`resourceChain(false)` = **不启用资源缓存**（避免长跑进程内存无界增长），但 `addResolver` 的注册必须先有 chain。
- **真值表（`shouldFallback(path)`，纯函数，被 `SpaFallbackTest` 逐项断言）**：

| 请求路径 | 结果 | 理由 |
| --- | --- | --- |
| `api`、`api/**` | **不 fallback**（含 `/API/...` 等大小写变体，比较时归一为小写） | `/api/**` 必须保留 controller/filter 语义（401/404 JSON） |
| `error`、`error/**`（含大小写变体） | **不 fallback** | 不干扰 Spring 的 `BasicErrorController` |
| 含 `.` 的路径（如 `/assets/missing.js`、`favicon.ico`） | **不 fallback** | 缺失资源必须是 404，不能变成 HTML（避免 MIME/缓存问题） |
| `/`、空串、`null` | **不 fallback** | 欢迎页/根路径由默认行为处理 |
| 其余路径（`home`、`movies/tt123`、`cart`、`employee/dashboard`、未知 `/missing-page` 等） | **fallback → `classpath:/static/index.html`** | 深链与刷新可用（前端再渲染 `NotFoundPage`） |
| **真实存在的资源**（`index.html`、`assets/index-*.js`、`favicon.svg`） | **直接返回该资源**（resolver 先查真实文件） | 静态资源优先，绝不覆盖 |

- **顺序约定**：`@RequestMapping` 的 controller 天然优先于资源 handler；新增 controller 时**不要**用能匹配 `/api/**` 之外全路径的映射（如 `/**`），否则会抢在 fallback 之前。
- **边界（不得声称）**：`SpaFallbackTest` 只证明**判定逻辑**与**resolver 注册**；「jar 内资源可被服务」「深链返回 HTML」**只有生产 smoke（Docker/`java -jar`）能证明**。#33 的实测结果见 `docs/plans/issue-33-*.md` §7（M2-PROD）。
- **打包约定**：`frontend/dist` **不提交**；`mvn -Pfrontend clean package` 把 `frontend/dist` 复制进 `target/classes/static`，**dist 缺失时 `validate` 相位 enforcer `require-frontend-dist` 使构建失败**（避免打出无 UI 的 jar）。默认 `mvn verify` **不依赖** Node/`dist`（CI 后端 job 因此不需要 Node）。
- **容器约定**：`Dockerfile` 多阶段（`node:22-alpine` → `maven:3.9-eclipse-temurin-11` → `eclipse-temurin:11-jre`），运行阶段以 **非 root（uid/gid 1001）** 启动；`compose.yaml` 的 `app` 服务容器内数据库地址必须是 `mysql:3306`（**不是** `localhost:3307`），并用 `SPRING_PROFILES_ACTIVE=local` 仅为本地/演示绕过外部 reCAPTCHA（生产需 `RECAPTCHA_SECRET_KEY`）。

## 12. 前端命令、Node 版本与部署校验约定【测试强制】（#32/#33）

- **Node 版本**：`frontend/package.json` 的 `engines.node = ">= 22.12.0"`；CI 与 Docker 固定 **Node 22 LTS**。以 Node 22 的结果为准（本地 Node 24 结果作为补充）。
- **命令（工作目录 `frontend/`）**：

| 命令 | 作用 | 通过判据（实施终验实测，2026-10-15） |
| --- | --- | --- |
| `npm ci` | 按 lock 安装 | 成功（CI 的 `frontend` job 第一步） |
| `npm run lint` | `oxlint src` | exit 0、无 warning；**注意：不覆盖 `e2e/`**（不得声称 e2e 已 lint） |
| `npm test` | `vitest run`（**不收集 `e2e/**`**） | 30 files / 195 passed |
| `npm run test:coverage` | vitest + v8 coverage | Lines 92.35%（846/916），阈值 `lines: 88` |
| `npm run build` | `vite build` → `frontend/dist` | 成功（产物由 `-Pfrontend` 打包消费） |
| `npx playwright install --with-deps chromium` | 安装浏览器 | 仅 chromium |
| `npm run test:e2e` | `playwright test`（360/1440 两个 project） | 12 passed |

- **后端命令**：`mvn --batch-mode --no-transfer-progress verify`（含 JaCoCo 85% 闸门）；`mvn -Pfrontend -DskipTests clean package`（生产打包）。
- **仓库/CI 卫生**：`frontend/dist`、`coverage`、`test-results`、`playwright-report` 均**被 git 忽略、不得提交**；Playwright 用例的 API 一律用 `page.route` mock（不依赖真实后端/MySQL）。

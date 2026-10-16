# Issue #30 方案 / 计划 — Migrate cart, payment, and confirmation flows

> **文档状态：已批准（主 agent，2026-10-14）→ 实施完成，已通过主 agent 终验（2026-10-14）→ 已提交并推送（`d6b3e07`、`eeb935f`）→ PR [#40](https://github.com/PNZ-z/ai-powered-Fabflix/pull/40) **已于 2026-10-16 合并（`MERGED`，merge commit `9154749`）**；Issue #30 已自动关闭（`CLOSED`）、远端分支已删除、`main` 上该 merge commit 的 CI SUCCESS（IMPLEMENTED, VERIFIED, PUSHED, MERGED — ISSUE CLOSED）**
>
> 本文档已完成 oracle 第一轮只读审查（报告：`.pi/artifacts/oracle-issue-30-plan-review.md`，含 B1–B6 / N1–N14 / U-1..U-9）与
> **第二轮只读复审**（报告：`.pi/artifacts/oracle-issue-30-plan-review-round2.md`，含 R2-B1 / R2-B2 / R2-N1..N5 / O-1..O-5）；
> 主 agent 已逐条裁定（第一轮见 §12，第二轮见 §12.1），docs-writer 已按裁定完成终稿修订。
>
> **批准条件已满足**：第二轮裁定第 1–12 条（R2-B1、R2-B2 两项 Blocker + R2-N1..N5 + O-1..O-5 + 覆盖率测试陷阱）**已全部写入本文档**；
> §11.1 的 O-1..O-5 已全部关闭（其中 O-3 的**实测**结果已于 2026-10-14 回填，见 §11.1 / §13.1）。
> 文中"已批准 / 已裁定"表示**主 agent 已作出决定**。**实现结果、实际文件清单与测试/覆盖率实测数字集中在 §13.1**（由主 agent 于 2026-10-14 亲自运行终验）；§1–§12 保持**批准时**的原始计划表述，不改写为事后叙述。逐文件变更与遗留风险见工作流日志 `docs/logs/2026-10-14-issue-30-migrate-cart-payment-confirmation-flows.md`。

## 元数据

| 项目 | 值 |
| --- | --- |
| GitHub issue | #30 — https://github.com/PNZ-z/ai-powered-Fabflix/issues/30 |
| 标题（远端原文） | Migrate cart, payment, and confirmation flows |
| Milestone | Phase 2 — React Frontend Migration (#2)（远端原文，转述自主 agent） |
| 来源 | 用户需求 + GitHub issue #30 正文 + scout 报告 + 主 agent 已核实事实 |
| 关联计划 | 本文件 |
| Issue 跟踪文档 | `docs/issues/issue-30-migrate-cart-payment-confirmation-flows.md` |
| 前置迁移 | #34 路由壳、#35/#36 API+auth、#37 discovery、#38 movie list、#39 detail（scout 报告 "Repo state" 节，git log 证据） |
| 后续 issue | #31 employee/admin 迁移（`routeDefinitions.js` 占位描述中出现） |
| 创建日期 | 2026-10-14（本机 `date` 输出） |
| 第一轮 oracle 审查 | `.pi/artifacts/oracle-issue-30-plan-review.md`（B1–B6 / N1–N14 / U-1..U-9）——**已完成**，裁定见 §12 |
| 第二轮 oracle 复审 | `.pi/artifacts/oracle-issue-30-plan-review-round2.md`（R2-B1 / R2-B2 / R2-N1..N5 / O-1..O-5）——**已完成**，裁定见 §12.1；**第一轮 + 第二轮全部闭合** |
| 批准 | **已批准（主 agent，2026-10-14）— 可交 worker 实现**；条件 = §12.1 的 12 条裁定已全部写入本文档。**该批准已于 2026-10-14 落实为实施完成并终验通过（§13.1）** |
| 修订日期 | 2026-10-14（先按第一轮裁定修订，再按第二轮裁定完成终稿；实现完成后补记 §12.1 结果小结与 §13.1） |
| 实施与终验 | 分支 `codex/issue-30-cart-payment-confirmation`（**已提交 `d6b3e07`/`eeb935f` 并推送；PR #40 已创建并于 2026-10-16 合并（`MERGED`，merge commit `9154749`），远端分支已删除、Issue #30 已关闭**）；终验命令与实测数字见 §13.1 |
| 文档状态 | **已批准 → 实施完成，已通过主 agent 终验（2026-10-14）；已提交并推送、PR #40 已创建并于 2026-10-16 合并（`MERGED`，merge commit `9154749`）；Issue #30 已关闭、远端分支已删除**（实现结果与差异见 §13.1，合并状态见 §13） |

## 1. 目标与背景

### 1.1 目标

在 React 前端重建完整的**顾客结账旅程**（cart → checkout → confirmation），**不改变业务行为**（issue 正文 "Goal"）。

当前三个路由只是占位页，占位描述明确写着"will be migrated in Issue #30"：

- `frontend/src/app/routeDefinitions.js`：`cart`（第 4-9 行）、`checkout`（第 10-15 行）、`confirmation`（第 16-21 行）三个 `customerProtectedRoutes` 条目；
- `frontend/src/app/AppRoutes.jsx`：`placeholderRoute` 定义于第 19-27 行，第 41 行以 `{customerProtectedRoutes.map(placeholderRoute)}` 挂载三条占位；页面为 `RoutePlaceholderPage`；
- `frontend/src/App.test.jsx` 第 85 行 `it.each(routeDefinitions)('renders the $title placeholder at $samplePath')` 断言占位标题；第 127-131 行断言匿名访问 `/cart` 重定向顾客登录页。

后端 cart/payment 接口与业务逻辑**已经存在**（`CartController`、`CartService`、`PaymentController`、`PaymentService`、`PaymentRepository`），本 issue 不是从零实现后端，而是在其之上：(a) 迁移前端页面，(b) 按用户已批准的范围补两处后端契约（确认页订单数据、防重复提交幂等），(c) 在 React movie list/detail 上补 "Add to cart" 入口，(d) 为新增/修改的后端类补齐测试（**CI 强制 JaCoCo 行覆盖率闸门，见 C-13**），(e) 允许改动 legacy `src/main/webapp/payment.js` **一个文件**以携带 checkout token（B5 方案 a，见 C-4）。

### 1.2 背景：现状事实（带文件:行号证据）

> 以下均为亲自复核或 scout 报告明确给出文件:行号的实现事实，不含推测。

**后端 — cart**

- `src/main/java/com/fabflix/boot/commerce/CartController.java`
  - `GET /api/boot/cart` → `List<CartItem>`（第 21-24 行），只读 `HttpSession`，无请求参数。
  - `POST /api/boot/cart`，form 参数 `movieId`（可选）、`action`（可选）→ `OperationResponse`（第 26-33 行）。
- `src/main/java/com/fabflix/boot/commerce/CartService.java`
  - session 键字面量 `CART_SESSION_KEY = "cart"`（第 16 行）；cart 是 session 内的 `Map<String,Integer>`（`getOrCreateCart`，第 81-91 行）。**`getOrCreateCart` 是"读-没有-创建-写"，自身未加锁**（U-3 补充裁定：支付路径要求 cart 非空，故该竞态在结账路径不可达；接受并记录为边界）。
  - `updateCart`（第 49-79 行）动作：`add`/`increase`（+1，二者等价）、`decrease`（qty<=1 时移除，否则 -1）、`delete`；未知动作抛 `IllegalArgumentException("Unknown action")`；缺参抛 `"Missing movieId or action"`。每 cart `synchronized (cart)`（第 58 行）。
  - `getCart`（第 24-47 行）：按 movie id 稳定排序，标题来自 `CartRepository.findMoviesByIds`，价格来自 `priceFor`（第 93-96 行，`500 + floorMod(movieId.hashCode(), 1500)` 分 → $5.00–$19.99）。**价格是按 movie id 现算的伪价格；movies/sales 表没有 price 列。** 未知 movie id 被静默丢弃（第 36-44 行）；`getCart` 自身也在 `synchronized (cart)` 内取快照（第 27-29 行），因此可在支付临界区内被重入调用（N4 的可行性依据）。
- `src/main/java/com/fabflix/boot/commerce/CartItem.java`：JSON 字段 `movie_id`、`movie_title`（显式 `@JsonProperty`）、`quantity`、`price`（`price` 为 `BigDecimal`，序列化为 JSON number，例如 7.50 → 7.5）。
- `src/main/java/com/fabflix/boot/commerce/CartRepository.java`：`SELECT id, title FROM movies WHERE id IN (?,...)`；空 cart 短路（第 22-24 行）。

**后端 — payment**

- `src/main/java/com/fabflix/boot/commerce/PaymentController.java`
  - `POST /api/boot/payment`，`@ModelAttribute PaymentRequest`（form 参数 `firstName`/`lastName`/`creditCardNumber`/`expirationDate`，见 `PaymentRequest.java` 第 3-40 行）。
  - 从 `session.getAttribute("user")` 取 `CustomerIdentity`，用 `customer.getUsername()`——**该值是顾客 email**（`AuthenticatedCustomer`；登录在 `request.changeSessionId()` 后写入，`CustomerAuthController.java` 第 53-54 行）。
  - 空 cart → **HTTP 200** `{"status":"fail","message":"Your shopping cart is empty."}`（第 29-31 行）。**注意该 `cart.isEmpty()` 判断在 `synchronized (cart)` 之外——这是 B3 认定的"假成功"缺陷来源之一，本计划将其移入临界区。**
  - 成功时在 `synchronized (cart)` 内清空 session cart（第 34-39 行）；`Map.copyOf(cart)` 快照也在同一临界区内取得（第 35 行）。
  - 响应映射（第 41-50 行）：`SUCCESS` → `{"status":"success","message":"Order placed successfully."}`；`INVALID_PAYMENT` → `{"status":"fail","message":"Invalid payment information."}`；`CUSTOMER_NOT_FOUND` → `{"status":"fail","message":"Customer not found."}`。**全部 HTTP 200。**
  - 当前响应类型 `OperationResponse`（`OperationResponse.java` 第 5-33 行）只含 `status` + 可选 `message`（`@JsonInclude(NON_NULL)`）。
  - **并发现状（B3 事实修正）**：由于快照在临界区内取得、清空也在临界区内，第二个并发请求拿到的快照**必为空**，`insertSales` 不会产生重复行；真实缺陷是第二个请求仍会走完卡匹配 + customer 查询并返回 `{"status":"success"}`（**假成功**），且该隐式保证没有任何测试与显式约束（详见 §4.2）。
- `src/main/java/com/fabflix/boot/commerce/PaymentService.java`：`@Transactional placeOrder(email, cart, request)`；顺序 = 信用卡匹配 → 按 email 查 customer id → 插入 sales（第 17-33 行，`placeOrder` 体在 22-32 行）。
- `src/main/java/com/fabflix/boot/commerce/PaymentRepository.java`（SQL 常量为包可见，被测试断言）：
  - `CARD_SQL` = `SELECT 1 FROM credit_cards WHERE id=? AND first_name=? AND last_name=? AND expiration=?`；
  - `CUSTOMER_SQL` = `SELECT id FROM customers WHERE email=?`；
  - `INSERT_SALE_SQL` = `INSERT INTO sales (customer_id, movie_id, sale_date) VALUES (?, ?, CURDATE())`（第 18-19 行），`insertSales` 把每行 quantity **展开为等量个单行** `batchUpdate`（第 46-54 行）。**没有用到存储过程；`add_movie` 与结账无关。**
- `src/main/java/com/fabflix/boot/commerce/CommerceApiExceptionHandler.java`：`@RestControllerAdvice(assignableTypes={CartController, PaymentController})`（第 12 行）；`IllegalArgumentException` → 400 `{"errorMessage": <message>}`（第 16-19 行）；其他 `RuntimeException` → 500 `{"errorMessage":"Unable to process commerce request."}`（第 21-26 行）。**新建的 `CheckoutController` 目前不被该 advice 覆盖（B4），本计划按方案 (a) 把它加入 `assignableTypes`。**
- `src/main/java/com/fabflix/boot/commerce/PaymentResult.java`：枚举 `SUCCESS, INVALID_PAYMENT, CUSTOMER_NOT_FOUND`。

**后端 — 认证 / 会话 / CSRF**

- `src/main/java/com/fabflix/boot/auth/CustomerAuthenticationFilter.java`：所有 `/api/boot/**` 除 `login`、`employee-login`、`dashboard*`、`health` 外，都要求 `session.getAttribute("user") instanceof CustomerIdentity`，否则 **401** `{"errorMessage":"Authentication required."}`。因此 `/api/boot/cart`、`/api/boot/payment` 受保护。
- `pom.xml` 只有 web + jdbc + mysql + jasypt，**没有 `spring-boot-starter-security`**；`src/main` 下无 `csrf`/`SameSite`/`CookieSerializer` 配置 → **当前没有任何 CSRF token 机制**。
- 前端 `frontend/src/api/apiClient.js` **第 53 行**发送 `credentials: 'include'`（R2-N5 行号修正），靠默认 `JSESSIONID` cookie。

**数据库契约（`createtable.sql`）**

- `credit_cards`：`id varchar(20) PK`、`first_name`、`last_name`、`expiration date`。
- `customers`：`id int AUTO_INCREMENT PK`、`first_name`、`last_name`、`credit_card_id` FK、`address`、`email`、`password`。
- `sales`：`id int AUTO_INCREMENT PK`、`customer_id` FK、`movie_id` FK、`sale_date date`。**没有 quantity 列、没有 price/amount 列。**
- `movies`：`id varchar(10) PK`、`title`、`year`、`director`。**没有 price 列。**
- **不存在 `orders` 表**（已 grep `createtable.sql` 与 `stored-procedure.sql` 确认）。
- `stored-procedure.sql` 只有 `add_movie`（admin 建片用），不参与结账。

**前端 — 现状**

- 路由/外壳：`frontend/src/App.jsx` → `AuthProvider` → `AppRoutes`；`AppRoutes.jsx` 用 `placeholderRoute` 挂三个占位；`frontend/src/components/layout/AppShell.jsx` 顾客导航**已有** `{ to: '/cart', label: 'Cart' }`（第 8 行）。
- `frontend/src/auth/ProtectedRoute.jsx`：匿名访问重定向 `/login` 并带 `state.from`。
- API 客户端：`frontend/src/api/apiClient.js` — `API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'`；`apiRequest` 设置 `Accept: application/json`、`credentials: 'include'`，失败抛 `ApiError`（含 `.status`/`.data`/`.isUnauthorized`，解析 `errorMessage`/`message`）；**不自动设置 Content-Type**，现有表单 POST 走 `URLSearchParams` body（见 `frontend/src/features/auth/customerAuthApi.js`）。
- 可复用的既有模式：`features/movies/movieListApi.js`（端点常量 + 参数构造 + 薄封装，导出纯函数供测试）、`movieDetailApi.js`、`MovieListPage.jsx`/`MovieDetailPage.jsx`（loading/success/error 状态机 + `components/states/` 的 `ErrorState`/`LoadingState`/`EmptyState`/`PageState` + retry 计数）。
- **React 侧没有 "Add to cart"**：grep `frontend/src` 中 cart/payment/confirmation 只命中 `routeDefinitions.js`、`App.test.jsx`、`AppShell.jsx`、`LoginPage.jsx` 文案。`frontend/src/features/movies/MovieTable.jsx` 的 `MovieRow` 只有标题/年份/导演/类型/演员/评分 6 列，无操作列；`MovieDetailPage.jsx` 只有返回链接。

**前端 — 测试与工具链**

- `frontend/package.json`：scripts `dev`/`build`/`lint`（`oxlint src`）/`preview`/`test`（`vitest run`）/`test:watch`；React 19、react-router-dom 7；dev: vitest 5、jsdom、`@testing-library/react` + `user-event` + `jest-dom`。**没有 Playwright/Cypress 等 e2e 设施。**
- `frontend/vitest.config.js`：jsdom，`setupFiles: './src/test/setup.js'`。
- `frontend/src/App.test.jsx`：`renderRoute` helper（`MemoryRouter` + `AuthContext.Provider` 假会话），每测试 stub 全局 `fetch`；第 85 行 `it.each(routeDefinitions)` 占位标题断言；第 127-131 行 `/cart` 匿名重定向断言；第 62-67 行 `responseBody(url)` 对未知 URL 一律返回 `[{ name: 'Drama' }]`（**N13 注意项：cart/checkout/confirmation 成为真实页面后必须按 URL 分派**）。
- 页面级测试模式见 `frontend/src/features/movies/MovieListPage.test.jsx`（`jsonResponse` helper、`requestedUrls`、`vi.stubGlobal('fetch', ...)`、`afterEach` 清理 sessionStorage）。

**后端测试与"钉死"约束**

- 全部测试为 `@WebMvcTest` + `@MockBean`（MockMvc）+ 纯 Mockito service/repo 测试；**没有 `@SpringBootTest`**（已 grep 确认）。
- `src/test/java/com/fabflix/boot/commerce/CartControllerTest.java`：`@WebMvcTest(CartController.class)` + `@Import(CommerceApiExceptionHandler.class)`，`.sessionAttr("user", new AuthenticatedCustomer("viewer@example.com"))`；断言 JSON 契约 `movie_id`/`movie_title`/`quantity`/`price`，以及坏 action 的 400 `errorMessage`。
- `src/test/java/com/fabflix/boot/commerce/PaymentControllerTest.java`：断言成功清空传入 cart、失败保留 cart、空 cart 在调用 `placeOrder` 前短路；用 `.param(...)` 传表单。
- `src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java`：
  - `frontendNoLongerCallsLegacyServletApiPaths` 遍历 `src/main/webapp`，禁止 `api/<非 boot>` 路径；
  - `criticalPagesUseTheirSpringBootEndpoints`（第 24-35 行）**要求 `shoppingCart.js`、`payment.js`、`movieList.js`、`single-movie.js` 等 legacy 文件仍然存在，且分别包含 `api/boot/cart`、`api/boot/payment` 等字符串**（只做 `contains` 断言，不做行级/结构断言）。
    - → **删除/移动 legacy 结账文件会破坏测试**（C-4）。
    - → **在 `payment.js` 中新增 `GET api/boot/checkout/token` 调用不会破坏测试**（`api/boot/cart`、`api/boot/payment` 字符串仍在）——这是 B5 方案 (a) 可行的依据。
- `src/test/java/com/fabflix/boot/commerce/` 现有测试：`CartControllerTest`、`CartRepositoryTest`、`CartServiceTest`、`PaymentControllerTest`、`PaymentRepositoryTest`、`PaymentServiceTest`（均为 `@WebMvcTest`/Mockito 模式，全部必须在改动后保持通过）。
- `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java`：禁止 `jakarta.servlet`、`@WebServlet`、`extends HttpServlet`；pom 必须保持 `jar` + `repackage` 且不得引入 `jakarta.servlet-api`。
- 运行方式：本地 `mvn test`（surefire）；**CI 用 `mvn --batch-mode verify`**，并在 `verify` 阶段强制 JaCoCo 行覆盖率闸门（C-13/C-14）。

**legacy 页面（业务行为参考；本 issue 只改 `payment.js` 一个文件）**

- `src/main/webapp/shoppingCart.html` + `shoppingCart.js`：cart 表格（标题/数量/单价/行小计），+/-/Delete 按钮 `POST api/boot/cart` 带 `movieId`+`action`（`updateCart` 第 37-49 行），总额 = Σ qty×price（**JS 浮点累加**，第 5-16、34 行），"Proceed to Payment" → `payment.html`。
- `src/main/webapp/payment.html` + `payment.js`：字段 `firstName`/`lastName`/`creditCardNumber`/`expirationDate`；提交序列化表单 → `POST api/boot/payment`（第 27-45 行）；`status==="success"` → `confirmation.html`，否则显示 `resultData["message"]`（**第 34-39 行**，R2-N5 行号修正）；总额来自 `GET api/boot/cart`（第 16-25 行）。
  - **B6 事实修正**：`payment.js` 里确实**没有 JS 业务校验**，但 `payment.html` 的四个字段**全部带 HTML5 `required`**（第 26、32、38、44 行），且 expiration 是 **`<input type="date">`**（第 44 行，提交格式 `YYYY-MM-DD`）。因此"非空校验"是**既有行为 parity**（合并到 React 表单不算新增行为），且日期必须保持 `YYYY-MM-DD` 才与 `credit_cards.expiration`（`date` 列）匹配（§5.2 / §6.2）。
- **B5 修订（已批准方案 a）**：`payment.js` 是本 issue **唯一允许修改**的 legacy 文件——提交前先 `GET api/boot/checkout/token`，把 token 与四个表单字段一起 POST。**其余 legacy 文件（`shoppingCart.js`、`confirmation.html`、`movieList.js`、`single-movie.js` 等）仍不得删除或改动。**
- `src/main/webapp/confirmation.html`：**纯静态终态页**，无后端调用，只有 "Your order has been placed successfully." 与返回/登出链接。
- `src/main/webapp/movieList.js`（第 118-133 行）与 `single-movie.js`（第 60-101 行）："Add to cart" POST `action:"add"`，成功/失败用 `alert(...)`。
- 这些文件是**文档/oracle 参考**：项目为 `jar` 打包，没有 `src/main/webapp` 的静态资源映射（grep `addResourceHandlers`/`WebMvcConfigurer` 无结果），legacy 页面在运行时不通过 Spring 提供。

### 1.3 用户已批准的范围决定（主 agent 传达，按此写入；视为已批准范围内的事实）

| 编号 | 已批准决定 |
| --- | --- |
| D-1 | **Add to cart 入口纳入 #30**：#28/#29 迁移的 React movie list 与 movie detail 页需要新增 "Add to cart" 操作，否则 React 购物车无法被填充、验收条件无法端到端成立。 |
| D-2 | **Confirmation 展示服务端订单数据**（issue 正文的 "sale-line display" 按此实现）：支付成功后后端返回订单数据（订单行、金额、日期等），前端确认页展示。**允许扩展支付响应契约。** |
| D-3 | **完整幂等/防重复提交方案**：需要 submit token / idempotency key 之类的新 API 契约；**仅前端禁用按钮不够。** |
| D-4 | **CSRF 保持现状**：明确列为 out-of-scope，并在风险中记录为残余风险（当前无 Spring Security、无 CSRF token）。 |
| D-5 | **幂等语义采用 T1（主 agent 已裁定）**：服务端下发的 `checkoutToken` **兼作 idempotency key**；成功后再次以同一 token 提交**重放同一成功响应**（含缓存订单），不是"第二次一律 fail"。详见 §4.2。 |
| D-6 | **确认页刷新采用 session-backed `GET /api/boot/confirmation`（主 agent 已裁定）**：支付成功时把订单摘要存 session（与 D-5 的 `checkoutOrder` 复用同一份）；有订单 → 200（订单 JSON），无订单 → **204**（`apiClient` 已把 204 解析为 `null`）。**不再使用 sessionStorage**。详见 §4.3。 |
| D-7 | **允许修改 legacy `payment.js` 单个文件（主 agent 已裁定，B5 方案 a）**：提交前先 `GET api/boot/checkout/token`，把 token 与四个表单字段一起 POST；其余 legacy 文件仍不得删改（C-4）。 |
| D-8 | **支付响应契约细节（主 agent 已裁定）**：**扩展 `OperationResponse`** 增加可空 `order` 字段（`@JsonInclude(NON_NULL)` 保证失败响应无该字段），**不新增 `PaymentResponse` 类**；字段名 `order_date`（String `YYYY-MM-DD`）/`total_amount`（BigDecimal scale 2）/`lines[]`（`movie_id`、`movie_title`、`quantity`、`unit_price`、`line_total`）；`checkoutToken` 作为 `PaymentRequest` 新字段由 `@ModelAttribute` 绑定；**不加**机器可读 `code` 字段。 |
| D-9 | **前端行为 parity（主 agent 已裁定）**：React 支付表单四字段 `required` + expiration `type="date"`（保持 `YYYY-MM-DD`），**不做额外格式校验**（业务错误由服务端 `fail` 文案承担）；加购反馈用**内联状态文案**（可测），不用 `alert`，记为"已批准的呈现差异"。 |
| D-10 | **JWT 路线（用户已确认，路线 C）**：网页端继续使用 **session-based 认证**；JWT 仅保留给**未来 Spring AI agent 平台**的独立 trust boundary，**不属于 #30/#31**；本 issue 不引入 Spring Security / JWT 依赖；任何 JWT 工作必须另立 issue + 计划 + oracle 审查。已作为跨 issue 决策写入 `docs/architecture/frontend-migration-contracts.md`（§10）。 |

## 2. 验收条件逐条映射（issue 正文原文 → 本计划如何满足）

| # | issue 验收条件（原文） | 计划承载 | 验证方式（计划） |
| --- | --- | --- | --- |
| AC-1 | Cart quantities and totals update correctly | `cartApi.js` 封装 `GET/POST /api/boot/cart`；`CartPage` 渲染行（标题/数量/单价/行小计）与总额，+ / − / Delete 触发 `action=increase|decrease|delete`，成功后重新 `GET`（对齐 legacy `shoppingCart.js` 行为）；**前端金额用整数分求和（N2）** | 前端 vitest：`CartPage.test.jsx` 断言请求 URL、body（`URLSearchParams` 的 `movieId`/`action`）、渲染数量与总额；后端 `CartControllerTest` 既有契约断言 +（如需）新增 |
| AC-2 | Cart contents persist across navigation and refresh | 复用服务端 **session cart**（`CART_SESSION_KEY="cart"`）；前端不缓存 cart，每次 mount 都重新 `GET`；`apiRequest` 已带 `credentials: 'include'` 使 `JSESSIONID` 复用。**近似验证拆为两条（N12 已裁定；第 ② 条按 R2-B2 改到 `CartServiceTest`）** | ① 前端 vitest：`CartPage` 每次 mount 都重新 `GET /api/boot/cart`（**不缓存**，mount→卸载→再 mount 断言两次请求）；② 后端 `CartServiceTest`（真实 `CartService` + `MockHttpSession` + stub `CartRepository`）：`updateCart(...)` 后 `getCart(session)` 断言行/数量变化。**`@MockBean CartService` 的 `CartControllerTest` 不做 `POST`→`GET` 状态断言**（mock 无状态，无法体现变化，R2-B2）。（**真实浏览器刷新无自动化覆盖**，见 §7.4 / §10 残余风险） |
| AC-3 | Valid payment completes and displays confirmation data | 按 D-2/D-8：`POST /api/boot/payment` 成功响应携带 `order`；`ConfirmationPage` 先取 `location.state?.order`，否则 `GET /boot/confirmation`（D-6），展示订单行/金额/日期 | 后端 `PaymentControllerTest`（成功响应含 order 数据 + 缓存/重放）+ `CheckoutControllerTest`；前端 `PaymentPage.test.jsx`（成功→导航 `/confirmation` 并携带 state）+ `ConfirmationPage.test.jsx`（渲染行/总额/日期 + 204 空态 + session 回退） |
| AC-4 | Invalid card/customer input displays a safe error | 复用后端 `INVALID_PAYMENT`/`CUSTOMER_NOT_FOUND`/空 cart 的 `{"status":"fail","message":...}`；React 表单保留 legacy **`required`（四字段）+ `type="date"`（B6/U-4）**作为对照既有行为的 parity，**不新增格式校验**；前端用 `role="alert"` 显示 `message`，**不回显卡号**；400 `errorMessage`（`CommerceApiExceptionHandler`）走同一安全展示 | 前端 vitest：invalid payment → 显示 "Invalid payment information."、无卡号回显；后端 `PaymentControllerTest` 既有失败分支断言 + "响应体不含卡号/卡字段"的断言（计划项） |
| AC-5 | Critical cart and checkout paths have integration tests | 按 §7.4（**U-8 已裁定**）：integration = 后端 MockMvc（controller + `CustomerAuthenticationFilter` + session 语义）+ 前端页面级 `fetch` mock 流程；**无 e2e 设施，引入需单独批准** | `mvn --batch-mode verify`（编译 + 测试 + JaCoCo 闸门）+ `npm test` + `npm run lint`（后两者是**手动验证门**，不在 CI） |

> **AC-4 的边界（U-4 已裁定）**：legacy `payment.html` 本就有 HTML5 `required` + `type="date"`（B6 事实修正），“保留 required/`type=date` 的 parity”不算新增行为；**是否额外增加格式校验**属产品选择，已裁定为**不加**（业务错误由服务端 `fail` 文案承担，前端仅安全展示）。
> **AC-5 至少覆盖（U-8 已裁定）**：① 经 `CustomerAuthenticationFilter` 的 401；② token 缺失/重放；③ cart→checkout→confirmation 前端流程。未覆盖项（真实浏览器/MySQL/双标签页）如实列入 §10 残余风险。

## 3. 范围（In scope）与 Non-goals（Out of scope）

### 3.1 In scope

1. `/cart` 页面：查看 cart、增/减数量、删除行、空 cart 状态、总额显示。
2. `/checkout` 页面：支付表单（`firstName`/`lastName`/`creditCardNumber`/`expirationDate`，对齐 legacy 字段与 `required`/`type="date"` parity）、提交、错误显示、提交中态。
3. `/confirmation` 页面：展示服务端返回的订单数据 + 无订单数据时的安全空态（含 204 语义）。
4. React movie list / movie detail 的 "Add to cart" 操作（D-1，抽成单一 `AddToCartButton` 共用，N14）。
5. 后端**最小契约变更**：支付成功的订单数据（D-2/D-8）、`GET /api/boot/checkout/token`（D-3）与 `GET /api/boot/confirmation`（D-6）。**不新增订单表、不改 DB schema。**
6. **legacy `src/main/webapp/payment.js` 单文件改动**（D-7）：提交前取 token。
7. 路由接线与随之必经的 `routeDefinitions.js` / `AppRoutes.jsx` / `App.test.jsx` 联动修改（N6）。
8. 新增/更新的后端测试（MockMvc + Mockito）与前端 vitest 测试，**满足 C-13 覆盖率闸门**。
9. 本 issue 的 `docs/` 文档（plan / issue / architecture）。

### 3.2 Out of scope（明确排除）

| 排除项 | 理由 / 证据 |
| --- | --- |
| **CSRF 防护**（引入 Spring Security / CSRF token / SameSite 调整） | D-4 用户已批准排除；作为残余风险记录（当前 `pom.xml` 无 security starter，`src/main` 无 CSRF 配置）。 |
| **JWT / Spring Security 接入网页端** | D-10（路线 C）：网页端继续 session-based 认证；JWT 仅保留给未来 Spring AI agent 平台的独立 trust boundary，**不属于 #30/#31**，需另立 issue + 计划 + oracle 审查。 |
| **删除或改动 legacy `src/main/webapp` 其他结账页面** | `FrontendApiMigrationTest.criticalPagesUseTheirSpringBootEndpoints` 钉死 `shoppingCart.js`/`movieList.js`/`single-movie.js`/`confirmation.html` 等；本 issue **唯一例外是 `payment.js`**（D-7/B5）。 |
| **`orders` 表 / DB schema 变更 / sales 表加 quantity+price 列** | 用户批准范围仅"扩展支付响应契约"（D-2）与 token（D-3）；schema 变更属破坏性迁移，需单独批准。 |
| **DB 唯一约束 / 多实例（多 JVM）下的分布式幂等** | 任务说明已明确排除；**单实例假设已由主 agent 核实（U-6）**：`compose.yaml` 仅定义 `mysql` 服务（第 1-24 行），`pom.xml` 无 `spring-session`/Redis，session 为 JVM 内存。 |
| **真正的浏览器 e2e 测试设施**（Playwright/Cypress/Selenium） | 仓库无此设施（`frontend/package.json` 只有 vitest+jsdom；后端仅 surefire）；引入需单独批准（U-8）。 |
| **价格模型/税/真实金额体系** | 后端无 price 列，价格由 `CartService.priceFor` 由 movie id 哈希决定；引入真实定价属产品决策，未批准。 |
| **i18n / 多语言错误文案** | 后端 message 硬编码英文（`PaymentController`、`CommerceApiExceptionHandler`），仓库无 i18n 层。 |
| **employee / admin（#31）相关改动** | 属 #31。 |
| **修复 `CustomerAuthenticationFilter` 或 session 认证语义** | 非本 issue 目标；仅依赖它。 |
| **重构/重写 `PaymentRepository` SQL 或展开式 `insertSales`** | 非本 issue 目标（保持"按 quantity 展开为多行 sales"与 `INSERT_SALE_SQL` 的 `CURDATE()` 的既有行为，U-1）。 |

## 4. 已裁定的方案（原候选与取舍保留在下）

> 本节原为候选方案；oracle 第一轮审查后主 agent 已逐条裁定（§12），第二轮复审后由主 agent 接受全部 12 条修订并**批准本终稿（§12.1 / §13）**。以下是**已批准的目标形态**。被否决的候选保留在各小节末尾，以免后续重复讨论。

### 4.1 (a) 订单数据契约（D-2 / D-8 / N3 / N4 / U-1 已裁定）

**现状约束**

- 支付响应当前只有 `{status, message}`（`OperationResponse`，`PaymentController` 第 41-50 行）。
- `sales` 表无 quantity/price 列，无 `orders` 表 → **服务端不可能"查回"已下单订单的金额/行数**；订单数据只能在支付成功那一刻由 cart 快照组装并写入 session。
- 价格唯一来源是 `CartService.priceFor`（第 93-96 行，包可见）；标题来自 `CartRepository.findMoviesByIds`。
- 关键时序（**R2-B1 已裁定修订**）：`PaymentController` 在 `synchronized (cart)` 内 `cart.clear()`（第 34-39 行）→ **订单摘要必须在 `placeOrder` 之前组装（“组装先于下单”）**；下单成功后的记账只剩**纯内存操作**（缓存 `checkoutOrder`、`checkoutState=completed`、`cart.clear()`）。摘要组装含一次 DB 查询（`cartService.getCart`），把它放在事务提交**之前**可消除“sales 已提交但记账失败 → 客户端同 token 重试 → 重复下单”的窗口（详见 §4.2 第 4-6 步与“窗口消除说明”）。
- **已知边界（R2-N4，本 issue 不修）**：`cartService.getCart` 会静默丢弃 movies 表中不存在的 movie id（`CartService.java:36-44`），而 `placeOrder` 的插入使用原始 `Map.copyOf(cart)`；极端情况下确认页展示的订单行与实际插入的 sales 行可能不一致。仅记录在 §10 R-15，不改 `getCart`/`insertSales`。
- `sale_date` 用 SQL `CURDATE()` 生成（`INSERT_SALE_SQL` 第 18-19 行）→ 服务端当前**没有**把插入日期读回。

**已定形态（候选 (i) + (ii) 组合）**

- 响应：`{ "status":"success", "message":"Order placed successfully.", "order": { "order_date":"YYYY-MM-DD", "total_amount": <BigDecimal scale 2>, "lines": [ { "movie_id", "movie_title", "quantity", "unit_price", "line_total" } ] } }`。
- 复用 DTO 的**序列化约定（R2-N1）**：`OrderSummary`/`OrderLine` 会被写入 session（`checkoutOrder`），因此必须 `implements Serializable`（建议 `serialVersionUID = 1L`），与既有 session 对象（`AuthenticatedCustomer`、`EmployeeIdentity`）一致（§5.1、C-16）。
- **扩展 `OperationResponse` 增加可空 `order` 字段**（`@JsonInclude(NON_NULL)` 保证失败/非支付响应不出现该字段），**不新增 `PaymentResponse` 类**（N3）。
- `lines[]` 中的 JSON 字段名沿用 cart 端点的 snake_case（架构文档 §6）；字段名**只用 `lines`**（**O-4**：已删除 `sale_lines` 别名说明；issue 原文用词只在 `docs/issues/issue-30-*.md` 保留）。
- 金额：`total_amount`/`unit_price`/`line_total` 用 `BigDecimal`（scale 2），服务端权威（C-8）；**前端不得复算金额**（N2：前端以整数分求和仅用于展示）。
- 摘要来源：**在同一临界区内用 `cartService.getCart(session)` 同源组装**（N4；`getCart` 内部可重入同一 monitor），**不在 controller 重算价格**。
- 日期（U-1）：`order_date` 用 JVM `LocalDate.now()`，**不改** `INSERT_SALE_SQL`（仍 `CURDATE()`）；"午夜边界下 DB/JVM 时区可能差一天"记为低风险（§10）。
- 兼容性：JSON 只增字段；legacy `payment.js` 只读 `status`/`message`，不受影响；`PaymentControllerTest` 需同步新增断言。
- 刷新/直接访问：由 §4.3 的 session-backed `GET /api/boot/confirmation` 承担（D-6）。

**已否决的候选**

| 候选 | 裁定 |
| --- | --- |
| (iii) 纯前端 sessionStorage + 前端复算金额 | 否决：与 D-2 冲突（金额/日期不具权威性）；且 D-6 选择 session-backed 端点后不再需要。 |
| 新增 `PaymentResponse` 类 | 否决（N3）：扩展 `OperationResponse` 少一个类、少一份覆盖率负担（C-13）。 |
| 失败响应增加机器可读 `code` | 否决（N8）：不加；前端不依赖文案字符串匹配之外的新契约。 |

### 4.2 (b) 幂等 / 防重复提交（D-3 落实；**B2 采用 T1**，**B3 已事实修正**）

**现状缺口（B3 修正后的准确描述）**

1. 现状**不会**重复插入 sales：`Map.copyOf(cart)` 在 `synchronized (cart)` 内取得（`PaymentController` 第 34-35 行），清空也在锁内（第 36-38 行），并发第二个请求的快照必为空。**原稿"两个并发 POST 都会调 `insertSales`、sales 重复插入"的表述与代码不符，已删除。**
2. **真实缺陷 1（假成功）**：第二个请求仍会走完 `paymentCardMatches` + `findCustomerIdByEmail`，`insertSales(emptyMap)` 不产生任何行，但返回 `{"status":"success"}`。
3. **真实缺陷 2（隐式保证、无测试）**：该行为依赖"快照在锁内 + 清空在锁内"这一实现细节；任何重构（把快照移出锁、引入 Spring Session/多实例）会静默打破它，且当前没有任何测试断言它。
4. 其余缺口：无 idempotency key、无 DB 唯一约束、无 orders 表（证据见 §1.2）；前端仅禁用按钮无法覆盖双击竞态、双标签页、刷新重提、网络重试、脚本重放。

**token 方案的价值（与原稿不同）**：(a) 把隐式保证变成**显式、可测的契约**；(b) 修掉**假成功**（把 `cart.isEmpty()` 移入临界区）；(c) 让"重复提交"的期望行为可被断言。

**已定方案（T1）：token 兼作 idempotency key，成功后重放同一成功响应**

- session 状态：`checkoutToken`（String）、`checkoutState`（`pending`/`completed`）、`checkoutOrder`（订单摘要，与 §4.3 的 session 订单**复用同一份**）。
- `GET /api/boot/checkout/token`：**在锁内生成新 token 并置 `pending`**（覆盖旧状态；**不清 cart**）。
- `POST /api/boot/payment`（新增 `checkoutToken` 形参，经 `@ModelAttribute PaymentRequest` 绑定，N9）：**在同一个 `synchronized (cart)` 临界区内按顺序**（**R2-B1 已裁定修订：组装先于下单**）：
  1. **校验 token**：缺失/不匹配 → HTTP 200 `{"status":"fail","message":"Your checkout session has expired. Please try again."}`（**O-1 定稿文案**，**不回显 token 值**），不调用 `placeOrder`；
  2. **`state == completed` → 直接重放缓存订单 + `success`**（**必须先于空车判断**，否则重放会被空车短路掉）；
  3. **`cart.isEmpty()` → `fail("Your shopping cart is empty.")`**（**本计划把该判断从锁外移入锁内**——这是 B3 认定的真正行为修正点）；
  4. **组装订单摘要**：在同一锁内用 `cartService.getCart(session)` 组装 `OrderSummary`（**这一步含 DB I/O，必须在 `placeOrder` 之前完成**）；
  5. **`placeOrder(...)`**：调用 `PaymentService.placeOrder`（`@Transactional`，正常返回即已提交）；
  6. **成功后纯内存记账**：`checkoutOrder = summary`、`checkoutState = completed`、`cart.clear()`，返回 `success` + `order`；失败（`INVALID_PAYMENT`/`CUSTOMER_NOT_FOUND`）→ **保持 `pending`、保留 token、不缓存订单、不清 cart**，可改卡重试。

  **窗口消除说明（R2-B1 的核心）**：第 4 步的 DB I/O 若失败，异常在第 5 步之前抛出 → **尚未调用 `placeOrder`，没有任何已提交的 sales**；第 6 步只有内存写入与 `cart.clear()`，不抛 I/O 异常。因此“sales 已提交但记账失败 → 客户端用同一 token 重试 → 重复下单”的窗口被消除。（**可选加固（不强制）**：用 `try/finally` 包裹第 6 步的内存记账，使任何意外 `RuntimeException` 也不会留下“已下单但未记账”的状态；worker 可选实现，若采用需在报告中说明。）
- 同时保留第一道防线：前端提交中禁用按钮 + `navigate(..., {replace:true})`。
- token 轮换（U-3 裁定）：**不在 cart 变更时失效 token**（legacy 也是"页面加载时取总额、提交时用当前 cart"，保持 parity）；下单使用**提交时刻**的 cart 快照。该边界记入 §10。
- 缺 token 的语义（U-3 裁定）：**200 `status:fail`**（与 C-9 一致），**不用 400**。
- 锁与临界区（U-3 裁定）：**复用 `synchronized (cart)` 可接受**（同一 session 内 cart 实例唯一，`getOrCreateCart` 对已存在的 cart 直接返回同一实例）；token 校验、重放判断、空车判断、摘要组装**全部在同一临界区**。**不新增 session 级锁对象。**
- 已知取舍（U-3 补充）：`CartService.getOrCreateCart`（第 81-91 行）的"读-创建-写"未加锁，理论上并发首次创建可得不同 `Map` 实例、使 `synchronized (cart)` 不再互斥；因支付路径要求 cart 非空（已实例化），影响低，**接受并在 §10 记录**。
- 文案（**O-1 已定稿**）：token 缺失/不匹配 → HTTP 200 `{"status":"fail","message":"Your checkout session has expired. Please try again."}`，**不回显 token 值**；`completed` 重放**不新增文案**——返回原 `success` + `"Order placed successfully."` + 原 `order`。原候选文案 `"This checkout has already been processed."` **已删除**（与重放语义矛盾）。UI 呈现为"安全错误且可恢复"（引导回 `/cart` 或重新取 token）。

**T1 相对 T2 的取舍（B2）**：T2（消费后重复提交一律 `fail`）在"响应丢失 + 客户端重试"时序下会把**已成功**的订单报成失败，不符合 D-3 的"完整幂等"；T1 以少量 session 状态换取正确的重放语义。**T1 已由主 agent 裁定采用。**

**必须写进测试的时序表（对应 §7.1 / §7.2）**

| 场景 | 期望行为 | 断言要点 |
| --- | --- | --- |
| 两个并发 POST，携带同一有效 token | 恰好一次 `placeOrder`；另一个为重放 success 或安全 fail；cart 清空一次 | `verify(paymentService, times(1)).placeOrder(...)`；cart 清空一次 |
| 成功后再提交（原 token） | **重放同一成功响应 + 同一 `order`**，不新增 `placeOrder` | `times(1)`；响应 `order` 与首次相同 |
| 新 token + 空 cart | `"Your shopping cart is empty."`，不调用 `placeOrder` | `verify(paymentService, never()).placeOrder(...)` |
| 刷新 `/checkout` 后重新提交（新 token，cart 仍有内容） | 正常下单一次（合法重试，不应被误拒） | `times(1)` |
| 缺 `checkoutToken` / token 不匹配 | HTTP 200 `status:fail` + `"Your checkout session has expired. Please try again."`（O-1），不调用 `placeOrder` | `never()` |
| 支付失败（INVALID_PAYMENT）后改卡重试（同一 token） | 允许，`checkoutState` 保持 `pending` | 第二次 `placeOrder` 仍被调用 |
| cart 在签发 token 后被修改，且 `checkoutState == pending` | 旧 token **仍有效**（已裁定不失效）；下单用提交时刻的 cart | 记为边界，不在测试中断言失效 |
| cart 在签发 token 后被修改，且 `checkoutState == completed` | 旧 token 触发**重放原订单**（新 cart 不会被下单）；必须重新 `GET /checkout/token` 才下新单 | 与 D-5 重放语义一致（oracle 第二轮 §2(h) 推演的表述澄清） |

**已否决的候选**

| 候选 | 裁定 |
| --- | --- |
| (ii) 客户端 UUID idempotency key | 未采用：与服务端下发 token 本质相同但多出"结果缓存"状态；T1 已在 token 上实现重放语义。 |
| (iii) DB 唯一约束 / orders 表 | 本 issue 已排除（无 schema 变更授权，§3.2）。 |
| T2（单次 token，重复一律 fail） | 否决（B2）：响应丢失重试会把已成功订单报失败，不满足"完整幂等"。 |

### 4.3 (c) 确认页数据传递（**D-6 已定：session-backed `GET /api/boot/confirmation`**；U-5 由本方案直接消解）

**已定形态**

- 支付成功时把订单摘要写入 session（与 §4.2 的 `checkoutOrder` **同一份**）。
- 新增 `GET /api/boot/confirmation`：**有订单 → HTTP 200 + 订单 JSON；无订单 → HTTP 204**（`apiClient.js` 已把 204 解析为 `null`，见架构文档 §5）。
- 端点落在 `/api/boot/**`，自动受 `CustomerAuthenticationFilter` 保护；响应 DTO 与支付响应的 `order` **复用同一份**。
- `ConfirmationPage`：优先 `location.state?.order`（`navigate('/confirmation', { replace: true, state: { order } })`，免一次请求），否则 `GET /boot/confirmation`；返回 `null`（204）→ **安全空态**（`EmptyState`："No recent order to display." + 去 `/movies` / 回 `/cart` 链接），**绝不显示假订单**。
- **不再使用 sessionStorage**：删除相关键（如 `fabflix.lastOrder`）、登出清理键、按 customer email 绑定校验等设计；`frontend/src/features/auth/LogoutButton.jsx` **不在本 issue 改动范围**。
- 语义（**R2-N2 已裁定固定，写死为本条不再由 worker 选择**）：`checkoutOrder` 表示**“最近一次成功订单”**，**只要存在就返回 200（即使 `checkoutState == pending`）**；无缓存订单 → **204**；新的成功订单**覆盖**旧订单；会话结束（`session.invalidate()` / 登出）后消失；多标签页共享同一 cookie session，因此不会串号展示。已批准的 UX 结果：用户已开新 checkout（`pending`）但未下单时，确认页仍展示上一笔成功订单（避免“明明下过单却看到空态”）。

**已否决的候选**

| 候选 | 裁定 |
| --- | --- |
| 纯导航 state（无后端端点） | 保留为页面内第一优先来源，但单独使用刷新即丢，故必须搭配 session-backed GET。 |
| 导航 state + `sessionStorage` + 登出清键 + customer 绑定 | 否决（N1）：需扩大前端范围（改 `LogoutButton.jsx`）且存在串号风险；session-backed GET 天然按 session 隔离。 |
| 纯 sessionStorage | 同否决（与 D-2/D-6 不一致）。 |
| URL 参数 + 按订单 id `GET` | 不可行：无 `orders` 表，无法按 id 查。 |

## 5. 拟修改 / 新增文件清单（精确路径）

> 这是**计划清单**（已按 §12 裁定修订），最终以 worker 的实际 diff 为准。

### 5.1 后端（main）

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `src/main/java/com/fabflix/boot/commerce/PaymentController.java` | 改 | 临界区内新顺序（**R2-B1**）：**token 校验 → completed 重放 → 空车判断 → 组装订单摘要（含 `cartService.getCart` 的 DB I/O）→ `placeOrder` → 成功后纯内存记账**（缓存 `checkoutOrder`、`checkoutState=completed`、`cart.clear()`）；保持既有 200/`status`/`message` 语义；**把 `cart.isEmpty()` 从锁外移入锁内（B3）** |
| `src/main/java/com/fabflix/boot/commerce/PaymentService.java` | **不改（签名不变）** | `placeOrder` 既有顺序（卡匹配→customer→插 sales）保持不变；摘要组装在 controller 层用 `cartService.getCart(session)`（N4） |
| `src/main/java/com/fabflix/boot/commerce/PaymentRequest.java` | 改 | 新增 `checkoutToken` 字段（由 `@ModelAttribute` 绑定，N9）；既有 4 个 getter 不变 |
| `src/main/java/com/fabflix/boot/commerce/OperationResponse.java` | 改 | 新增可空 `order` 字段（`@JsonInclude(NON_NULL)` 已保证失败响应无该字段）；**不新增 `PaymentResponse`**（N3/D-8） |
| `src/main/java/com/fabflix/boot/commerce/OrderSummary.java` | 新增 | 订单摘要 DTO（`order_date`、`total_amount`、`lines`），与 `GET /confirmation` 响应复用同一 DTO；**`implements Serializable`（R2-N1，见下方说明）** |
| `src/main/java/com/fabflix/boot/commerce/OrderLine.java` | 新增 | 订单行 DTO（`movie_id`、`movie_title`、`quantity`、`unit_price`、`line_total`）；**`implements Serializable`（R2-N1）** |
| `src/main/java/com/fabflix/boot/commerce/CheckoutTokenService.java`（**O-2 已固定此结构**） | 新增 | **必须存在**：token 签发/校验/重放状态逻辑集中在这个**可单测的小类**；管理 session 属性 `checkoutToken`/`checkoutState`/`checkoutOrder`；临界区嵌入方式见 §4.2/U-3。**worker 不得再改变该结构**；若确有技术阻碍，**暂停并询问主 agent** |
| `src/main/java/com/fabflix/boot/commerce/CheckoutController.java`（**O-2 已固定此结构**） | 新增 | **同时承载** `GET /api/boot/checkout/token`（U-9）**与** `GET /api/boot/confirmation`（D-6）；controller 只做 HTTP 语义与锁内编排，token/状态逻辑委派 `CheckoutTokenService`。**worker 不得再改变该结构**；有阻碍则暂停询问主 agent |
| `src/main/java/com/fabflix/boot/commerce/CommerceApiExceptionHandler.java` | 改（**B4 方案 a**） | `assignableTypes` 加入 `CheckoutController.class`（1 行），保持 400 `{"errorMessage":...}` 契约一致；§5.1 原先的"不改"已作废 |
| `src/main/java/com/fabflix/boot/commerce/PaymentResult.java` | 不改 | 重复提交/重放由 controller 层短路，不新增枚举值 |
| `src/main/java/com/fabflix/boot/commerce/CartService.java` | 不改（除非实现需要） | `getCart`（可重入同一 monitor）+ `priceFor` 已满足 N4；若确需改动，必须同步改 `CartServiceTest`/`CartControllerTest` |

> **R2-N1（session 存储对象必须 `Serializable`）**：`checkoutOrder`（`OrderSummary`/`OrderLine`）存放在 session 中，必须 `implements Serializable`（建议 `serialVersionUID = 1L`），与既有约定一致——`AuthenticatedCustomer.java:3-6`、`EmployeeIdentity.java:3-6` 均为 `implements Serializable`。理由：Spring Boot 内嵌 Tomcat 的 `StandardManager` 在关闭/重启时会持久化 session（`SESSIONS.ser`），不可序列化的 session 属性会产生 `NotSerializableException` 告警并导致该 session 丢失。（已提取为跨 issue 约束：C-16 / 架构文档 §9）

**不要碰**：`src/main/webapp/**`（legacy 页面，钉死；**唯一例外是 `payment.js`，见 §5.4/D-7**）、`createtable.sql`、`stored-procedure.sql`、`pom.xml`（不引入 Spring Security / JWT）。

### 5.2 前端（frontend/src）

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `frontend/src/features/cart/cartApi.js` | 新增 | `fetchCart()`（`GET /boot/cart`）、`updateCartItem(movieId, action)`（`POST /boot/cart`，`URLSearchParams` body，对齐 legacy `shoppingCart.js`） |
| `frontend/src/features/cart/CartPage.jsx` | 新增 | 渲染 cart 表（标题/数量/单价/行小计）+ 总额 + `+`/`−`/Delete + 空态 + 错误/重试 + "Proceed to payment" → `/checkout`；**每次 mount 重新 GET（不缓存，AC-2/N12）**；**金额用整数分求和**（`Math.round(price*100)`，N2） |
| `frontend/src/features/cart/AddToCartButton.jsx` | 新增（**单一组件，N14**） | 被 `MovieTable.jsx` 与 `MovieDetailPage.jsx` **共用**（不重复实现）：POST `movieId`+`action=add`；反馈用**内联状态文案**（U-7，不用 `alert`）；处理 401/其他错误 |
| `frontend/src/features/checkout/paymentApi.js` | 新增 | `fetchCheckoutToken()`（`GET /boot/checkout/token`）、`submitPayment(formValues, token)`（`URLSearchParams` body，含 `checkoutToken`） |
| `frontend/src/features/checkout/PaymentPage.jsx` | 新增 | 支付表单：4 字段均 `required`、expiration `type="date"`（B6）；总额展示（整数分求和）+ 提交中禁用态 + `role="alert"` 服务端错误 + 成功后 `navigate(..., {replace:true, state:{order}})`；**不新增格式校验**（U-4）。**R2-N3：mount 时先 `GET /boot/checkout/token`，仅在成功取得 token 后才允许提交（未取得时提交按钮 `disabled`）；token 获取失败 → 显示安全错误且不得用陈旧 token 提交** |
| `frontend/src/features/checkout/ConfirmationPage.jsx` | 新增 | 优先 `location.state?.order`，否则 `GET /boot/confirmation`；204 → 安全空态；**不使用 sessionStorage**（D-6） |
| ~~`frontend/src/features/checkout/lastOrder.js`~~ | **已移除** | 原稿的 sessionStorage helper 随 D-6 一并取消 |
| `frontend/src/features/movies/MovieTable.jsx` | 改 | 增加操作列渲染 `AddToCartButton`（D-1）；列头文案由 worker 定 |
| `frontend/src/features/movies/MovieDetailPage.jsx` | 改 | 详情页嵌入同一 `AddToCartButton`（D-1） |
| `frontend/src/app/AppRoutes.jsx` | 改 | 新增 `cart`/`checkout`/`confirmation` **显式路由**替换 `customerProtectedRoutes.map(placeholderRoute)` 对这三条占位（N6），保留 `ProtectedRoute audience="customer"` |
| `frontend/src/app/routeDefinitions.js` | 改 | 从 `customerProtectedRoutes` **删除 cart/checkout/confirmation 三条定义**（N6）；`it.each(routeDefinitions)` 仍由 employee 两条占位覆盖，**不会出现空数组** |
| `frontend/src/App.test.jsx` | 改 | 三路由真实页面断言 + **扩展 `responseBody(url)` 按 URL 分派**（N13）；保留匿名 `/cart` 重定向断言（第 127-131 行） |
| `frontend/src/components/layout/AppShell.jsx` | 不改（可选加徽标） | 导航链接已存在；徽标属可选，若做需同步测试 |
| `frontend/src/index.css` | 改（可选） | cart 表/表单/订单展示样式 |

**不要碰**：`frontend/src/api/apiClient.js`（除非确有契约需要，须说明理由；**204→null 已满足 D-6**）、`frontend/src/features/auth/**`（含 `LogoutButton.jsx`，D-6 已裁定不改）、`frontend/vite.config.js`、`frontend/package.json`（不引入新依赖，除非单独批准）。

### 5.3 测试

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `src/test/java/com/fabflix/boot/commerce/PaymentControllerTest.java` | 改 | 成功响应含 `order` 数据（**断言订单行前必须显式 stub `cartService.getCart(...)`**，见 §7.1 覆盖率陷阱）；token 缺失/不匹配安全失败；**同 token 重放返回同一成功响应**；成功仍清空 cart；失败保留 cart；新 token + 空 cart 在 `placeOrder` 前短路；失败响应不含卡号。**跨请求流程必须共享同一 `MockHttpSession`（`.session(shared)`，R2-B2）** |
| `src/test/java/com/fabflix/boot/commerce/CheckoutControllerTest.java`（或 `CheckoutTokenTest.java`） | 新增 | token 签发并写入 session；`GET /confirmation` 200/204；缺 session user 由 filter 401；非法调用 400 `errorMessage`（验证 B4 接入 advice）。**O-2 固定的 `CheckoutTokenService` 需有自己的单测**（签发/校验失败/消费/重放四条分支，见 §7.1.2） |
| `src/test/java/com/fabflix/boot/commerce/PaymentControllerConcurrencyTest.java` | 新增（**O-3 已裁定：文件名可保留，内容改为“直接调用式”**） | **主用确定性 2 线程单测**：`CountDownLatch` + 共享同一 session/cart 实例，**直接调用** `PaymentController`（或 `CheckoutTokenService`）方法而**不经 MockMvc**；断言 `verify(paymentService, times(1)).placeOrder(...)`（另一线程为重放 success 或安全 fail）。**MockMvc 版并发测试列为可选**（`MockHttpSession.attributes` 为普通 `LinkedHashMap`，并发读写属数据竞争，易 flaky）；不稳定时按降级路径处理并**如实记录**；**不得弱化断言** |
| `src/test/java/com/fabflix/boot/commerce/PaymentServiceTest.java` | 改（**如签名/行为变动**，否则不改） | 若 `placeOrder` 签名或事务行为变化，同步更新；否则保持既有断言作为回归护栏 |
| `src/test/java/com/fabflix/boot/commerce/PaymentRepositoryTest.java` | 改（**如 SQL/参数变动**，否则不改） | 按 U-1 不改 `INSERT_SALE_SQL`；如实际改动则同步 |
| `src/test/java/com/fabflix/boot/commerce/CartServiceTest.java` | 改（**新增 AC-2 后端半边，R2-B2**） | 真实 `CartService` + `MockHttpSession` + stub `CartRepository`：`updateCart(...)` 后 `getCart(session)` 断言行/数量变化（既有文件已是此模式）；同时继续作为 `CartService` 回归护栏 |
| `src/test/java/com/fabflix/boot/commerce/CartControllerTest.java` | 通常不改 | 既有契约断言继续作为回归护栏 |
| `src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java` | **不改**（B5 方案 a 下依然绿） | 继续钉死 legacy 文件（`contains` 断言） |
| `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java` | **不改** | 不受本 issue 影响 |
| `frontend/src/features/cart/CartPage.test.jsx` | 新增 | AC-1/AC-2：请求 URL/body、行渲染、总额（整数分）、空态、错误重试、**每次 mount 重新 GET（不缓存）** |
| `frontend/src/features/cart/cartApi.test.js`（或并入页面测试） | 新增（可选） | 参数构造纯函数测试 |
| `frontend/src/features/checkout/PaymentPage.test.jsx` | 新增 | 表单提交（含 `checkoutToken`）、提交中禁用、invalid payment 安全错误、成功导航带 state；**token 未取得前禁止提交 / 取 token 失败不得提交（R2-N3）** |
| `frontend/src/features/checkout/ConfirmationPage.test.jsx` | 新增 | 渲染订单行/总额/日期；`location.state` 缺失时 `GET /boot/confirmation`；**204 → 安全空态**（无假数据） |
| `frontend/src/features/cart/AddToCartButton.test.jsx`（或并入页面测试） | 新增 | 加购 POST `movieId=<id>&action=add`；内联成功/失败文案 |
| `frontend/src/features/movies/MovieListPage.test.jsx` | 改 | 加购按钮存在且 POST 正确（D-1）；**fetch mock 需按 URL 分派**（N14） |
| `frontend/src/features/movies/MovieDetailPage.test.jsx` | 改 | 同上（N14） |
| `frontend/src/App.test.jsx` | 改 | 三路由真实页面 + 匿名重定向；**`responseBody(url)` 必须按 URL 分派**（N13，否则 `/cart` 会拿到电影行形状数据而失去断言意义） |

**测试命令（差异已裁定）**：后端 `mvn --batch-mode verify`（含 JaCoCo 闸门，CI 同命令）；前端 `npm test`、`npm run lint` 为**手动验证门**，worker 报告必须给出实际命令与结果（C-14）。

### 5.4 legacy（仅一个文件）

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `src/main/webapp/payment.js` | 改（**B5 方案 a，D-7**） | 提交前先 `GET api/boot/checkout/token`，把 token 与四个表单字段一起 POST；失败时沿用既有 `#payment_error_message` 显示。**只允许改这一个 legacy 文件**；`api/boot/cart`、`api/boot/payment` 字符串保留，`FrontendApiMigrationTest` 保持绿；`SpringBootOnlyArchitectureTest` 不受影响（`src/main/webapp` 不在其检查范围）。 |

## 6. 关键数据 / API 与用户流程

### 6.1 API 契约（现状 → 计划变更）

| 端点 | 现状 | 计划变更 |
| --- | --- | --- |
| `GET /api/boot/cart` | 200 `[{movie_id, movie_title, quantity, price}]` | 不变 |
| `POST /api/boot/cart`（`movieId`, `action`） | 200 `{status:"success"}`；坏 action 400 `{errorMessage}` | 不变。**token 不在 cart 变更时失效（U-3 裁定）**，响应体与行为不变 |
| `GET /api/boot/checkout/token` | 不存在 | **新增**：200 `{status:"success", token:"<opaque>"}`，session 内置 `checkoutState=pending`（**不清 cart**） |
| `POST /api/boot/payment`（`firstName`,`lastName`,`creditCardNumber`,`expirationDate` + **`checkoutToken`**） | 200 `{status, message}` | **契约扩展**：成功时新增可空 `order` 对象（`order_date`、`total_amount`、`lines[]`）；token 缺失/不匹配 → 200 `fail` + `"Your checkout session has expired. Please try again."`（**O-1 定稿**，不回显 token）；`completed` 重放 → 200 `success` + 原 `"Order placed successfully."` + 同一 `order`（D-5 / O-1） |
| `GET /api/boot/confirmation` | 不存在 | **新增（D-6/N1；R2-N2 语义已固定）**：**只要 session 内存在 `checkoutOrder` 就返回 200** + 订单 JSON（与支付 `order` 同 DTO，**即使 `checkoutState == pending`**）；无缓存订单 → **204**（`apiClient` 解析为 `null`）；新的成功订单覆盖旧订单；登出（`session.invalidate()`）后回到 204 |

所有 `/api/boot/**` 仍受 `CustomerAuthenticationFilter` 保护（未登录 → 401 `{errorMessage:"Authentication required."}`），前端 `apiRequest` 会把 401 暴露为 `ApiError.isUnauthorized`。新端点（`checkout/token`、`confirmation`）**不需改 filter**。

### 6.2 用户流程（含错误/空态/未登录跳转）

**表单字段 parity（B6/U-4）**：React `/checkout` 的 4 个字段均带 `required`，expiration 用 `type="date"`（提交 `YYYY-MM-DD`，与 `credit_cards.expiration` 的 `date` 列及 `database/dev-seed.sql` 的 `'2035-12-31'` 一致）；**不新增格式校验**，业务错误由服务端 `fail` 文案承担并提供安全展示。

```
[未登录访问 /cart 或 /checkout 或 /confirmation]
   → ProtectedRoute audience="customer" → /login（带 state.from），登录后回跳
（现状由 App.test.jsx 第 127-131 行断言；本 issue 需保留此行为）

[/movies 或 /movies/:movieId]
   → 点击 "Add to cart"（D-1，共用 AddToCartButton）
   → POST /boot/cart {movieId, action:add}
   → 成功：内联状态文案（U-7）；401 → 提示重新登录；其他失败 → 安全错误提示
   → 注意：movie 页面的 fetch mock 需按 URL 分派（N13/N14）

[/cart]
   → GET /boot/cart（每次 mount 都重新请求，不缓存）
   → 空数组 → EmptyState（"Your shopping cart is empty." 风格）+ 去 /movies
   → 有行 → 表格（标题/数量/单价/行小计）+ 总额（整数分求和）+ 行内 + / − / Delete
        → 每次操作 POST /boot/cart 成功后重新 GET（对齐 legacy）
   → "Proceed to payment" → /checkout
   → 请求失败 → ErrorState + Try again

[/checkout]
   → GET /boot/cart（展示总额；若空 → 空态并引导 /cart）
   → GET /boot/checkout/token（取得 token 并置 pending，D-3；可重复取，不需要额外的"重新开始结账"端点，U-9）
        ├─ 取得 token → 才允许提交（R2-N3）
        └─ token 获取失败/未返回 → **安全错误 + 提交按钮保持 disabled**；
             绝不用陈旧 token 提交（R2-N3：陈旧 token 在 checkoutState==completed 时会触发旧订单的"假成功"重放）
   → 填 4 字段 → 提交（按钮进入提交中禁用态）
   → POST /boot/payment（含 checkoutToken）
        ├─ status=success + order → 服务端已把订单写入 session →
        │     navigate('/confirmation', {replace:true, state:{order}})
        ├─ status=success（重放，旧 token） → 同上，展示同一订单（不重复下单）
        ├─ status=fail + "Your checkout session has expired. Please try again."（O-1 定稿） → 安全错误 + 重新取 token 或回 /cart
        ├─ status=fail + "Invalid payment information."/"Customer not found." → 表单旁安全错误
        │     （保留输入，checkoutState 保持 pending，token 仍可用）
        ├─ 401 → 引导登录
        └─ 网络错误 → 安全错误 + 可重试

[/confirmation]
   → location.state?.order（优先，来自成功跳转）
   → 否则 GET /boot/confirmation（刷新/直接访问/新标签页均可）
        ├─ 200 + order → 订单日期 + 行（电影/数量/单价/行小计）+ 总额
        │     （R2-N2：只要存在"最近一次成功订单"就返回 200，即使 checkoutState==pending）
        └─ 204（null，无订单） → 安全空态，绝不显示假订单
```

## 7. 验收与测试计划

### 7.1 后端（MockMvc / Mockito）

> **必须项（B1 / C-13）**：CI 的唯一构建步骤是 `mvn --batch-mode --no-transfer-progress verify`（`.github/workflows/ci.yml:36`），其中 JaCoCo 在 `verify` 阶段强制 `BUNDLE / LINE / COVEREDRATIO ≥ 0.85`（`pom.xml` 第 111-171 行）。
> 当前实测：**873 covered / 984 total ≈ 0.8872**（本次修订时从 `target/site/jacoco/jacoco.csv` 重新统计确认），未覆盖余量仅约 **43 行**。
> （实现完成后实测：**947 covered / 110 missed / 1057 total = 0.8959**，闸门通过；见 §13.1。上句 873/984 为批准时的基线，保留以对照。）
> 因此**每个新增/修改的后端类至少要有一条测试触及其代码行与分支**：DTO（`OrderSummary`/`OrderLine`）用一次 JSON 序列化断言即可覆盖 getter；`OperationResponse.order` 的“有/无”两条分支都要断言；`CheckoutTokenService` 的签发/校验/重放/失败分支全部覆盖。
> 新增类不配测试会让 PR 的 CI 直接变红。

> **跨请求测试的 session 机制（R2-B2，必须遵守）**：`MockHttpServletRequestBuilder.sessionAttr(name, value)` 会把属性写进**该请求自己的** session（`request.getSession().setAttribute(...)`），因此**每次 `mockMvc.perform(...)` 都会新建一个 `MockHttpSession`**；只有 `.session(MockHttpSession)` 才复用同一实例。所有 T1 跨请求断言（同 token 第二次提交、`completed` 重放、重放优先于空车、并发共享 session）**必须**写成共享 session：
>
> ```java
> MockHttpSession shared = new MockHttpSession();
> shared.setAttribute("user", new AuthenticatedCustomer("viewer@example.com"));
> shared.setAttribute("cart", cart);            // 同一 cart 实例
> mockMvc.perform(post("/api/boot/payment").session(shared).param(...)) ...;
> mockMvc.perform(post("/api/boot/payment").session(shared).param(...))
>         .andExpect(request().sessionAttribute("checkoutState", "completed"));
> ```
>
> **`.sessionAttr(...)` 不得用于跨请求断言**（第二次请求会拿到空 session → token 不匹配 → 断言必然失败）；单请求用例（如既有 401 用例）可继续用 `.sessionAttr(...)`。
>
> **覆盖率陷阱（B1 补充）**：`PaymentControllerTest` 用 `@MockBean CartService` 时，未 stub `cartService.getCart(...)` 会由 Mockito 默认返回**空 List**，断言 `order.lines` 会“意外通过”（空数组）——**断言订单行前必须显式 stub `getCart` 返回预期行**。
>
> **必须覆盖的分支清单（新增代码需接近全覆盖才能守住 85%，B1）**：
> - `CheckoutController`：`GET /checkout/token` 200；`GET /confirmation` **200 与 204** 两条分支；
> - `CheckoutTokenService`：签发、校验失败（缺失/不匹配）、消费（成功后置 `completed`）、重放（`completed` 分支）；
> - `PaymentController`：token 缺失、token 不匹配、`completed` 重放、空车、`SUCCESS`、`INVALID_PAYMENT`、`CUSTOMER_NOT_FOUND`、default/异常路径；
> - `OperationResponse`：有 `order` / 无 `order` 两条序列化分支；
> - `OrderSummary`/`OrderLine`：全部 getter（经 JSON 断言覆盖）。
>
> 新增/修改代码估约 **200–250 行**：`L=200` 需覆盖 ≈79%、`L=250` 需 ≈83% 的新增行，未覆盖余量仅约 43 行 → **“每个类一条测试”是必要但不充分**。

1. `PaymentControllerTest`（改）
   - **跨请求断言必须共享同一 `MockHttpSession`（R2-B2，写法见上）**；断言 `order.lines` 前必须显式 stub `cartService.getCart(...)`（§7.1 覆盖率陷阱）。
   - 成功：断言 `$.status == "success"`、`$.message` 保持原文案（`"Order placed successfully."`）、`$.order.*`（`order_date` 为 `YYYY-MM-DD`、`total_amount` 为 scale 2、`lines[]` 各行的 `movie_id`/`movie_title`/`quantity`/`unit_price`/`line_total`）；断言传入 cart 被清空（既有断言保留）。
   - **重放（T1）**：同一 token 第二次提交 → 200 `success` + **同一 `order`**，且 `verify(paymentService, times(1)).placeOrder(...)`（B3：断言可观测行为，而不是断言内部实现）。
   - **新 token + 空 cart**：`verify(paymentService, never()).placeOrder(...)`，message 保持 `"Your shopping cart is empty."`（既有断言保留）。**顺序要求：重放判断必须先于空车判断**，测试需覆盖“completed + 空 cart”仍返回重放成功。
   - `INVALID_PAYMENT` / `CUSTOMER_NOT_FOUND`：HTTP 200 + fail message，cart 保留、`checkoutState` 保持 `pending`（既有断言保留）。
   - token 缺失/不匹配：200 `status:fail` + `"Your checkout session has expired. Please try again."`（O-1），`placeOrder` 未被调用。
   - 安全：失败响应体不含 `creditCardNumber` 值（防回显）。
2. `CheckoutControllerTest`：token 签发返回非空 token 且写入 session（`checkoutState=pending`）；`GET /confirmation` 有订单 → 200 + 订单 JSON、无订单 → **204**；无 session user → 经 `CustomerAuthenticationFilter` 401（复用 `CustomerAuthenticationFilterTest` 模式）；非法调用 → 400 `{"errorMessage":...}`（验证 B4 已接入 advice）。
   - **`CheckoutTokenServiceTest`（O-2 固定的小类，必须单测）**：签发返回非空 token 并置 `pending`；校验失败（缺失/不匹配）；成功后置 `completed` 并缓存订单；`completed` 重放返回同一缓存订单（不新增 `placeOrder`）。
3. `PaymentServiceTest`：若 `placeOrder` 签名/行为未变，保持既有断言作为回归护栏；若变化，同步更新。
4. 并发（**O-3 已裁定：主用确定性 2 线程单测**）：`PaymentControllerConcurrencyTest` 用 `CountDownLatch` 挂住第一个线程的 `placeOrder`（`thenAnswer` 内 await），再放行第二个线程；两线程**直接调用** `PaymentController`（或 `CheckoutTokenService`）方法并**共享同一 session/cart 实例**（**不经 MockMvc**）；断言 `verify(paymentService, times(1)).placeOrder(...)`，另一线程为重放 success 或安全 fail。**MockMvc 版并发测试列为可选**（`MockHttpSession.attributes` 是普通 `LinkedHashMap`，并发读写属数据竞争，易 flaky）；不稳定时按降级路径处理并在 worker 报告与本文档中如实记录限制；**不得为通过测试而弱化断言**。
5. 回归护栏：`CartControllerTest`、`CartRepositoryTest`、`CartServiceTest`、`PaymentRepositoryTest`、`PaymentServiceTest`、`FrontendApiMigrationTest`、`SpringBootOnlyArchitectureTest` 必须保持通过。
6. AC-2 的后端半边（N12 第二条 + **R2-B2 修订**）：落在 **`CartServiceTest`**——真实 `CartService` + `MockHttpSession` + stub `CartRepository`，`updateCart(session, movieId, "increase")` 后 `getCart(session)` 断言行/数量变化。**`@MockBean CartService` 的 `CartControllerTest` 不做 `POST`→`GET` 状态断言**（mock 无状态，无法体现变化）。
7. 命令（**必须实际运行并报告**）：`mvn --batch-mode verify`（与 CI 同命令，含覆盖率闸门）；可选先跑子集 `-Dtest=Payment*Test,Checkout*Test`。

### 7.2 前端（vitest + Testing Library，沿用 `MovieListPage.test.jsx` 模式）

- `CartPage.test.jsx`
  - AC-1：`GET /api/boot/cart` 用 `credentials` 发出；渲染数量/单价/行小计/总额（**整数分求和**，N2）；点击 `+`/`−`/Delete → `POST /api/boot/cart`，断言 body 为 `movieId=...&action=increase|decrease|delete` 且随后重新 `GET`。
  - 空态：空数组 → 空态文案 + 去 `/movies` 链接。
  - 错误：GET 500 → `role="alert"` + Try again（重试后再请求）。
  - AC-2（**N12 第一条**）：**每次 mount 都重新 `GET /api/boot/cart`（不缓存）**——mount→卸载→再 mount 断言两次请求均发出。
- `PaymentPage.test.jsx`
  - 表单字段带 `required`（4 个）与 expiration `type="date"`（B6/U-4）。
  - 提交时携带 4 字段 + `checkoutToken`，body 为 `URLSearchParams`；`credentials: 'include'`。
  - **R2-N3：token 未取得前提交按钮 `disabled`**（挂住 `GET /boot/checkout/token` 的 fetch，断言点击不会发出 `POST /boot/payment`）；`GET /boot/checkout/token` 失败（500/网络错误）→ 显示安全错误（`role="alert"`）、按钮仍 `disabled`，**不发出任何 `POST /boot/payment`**（防止陈旧 token 触发旧订单重放）。
  - 提交中重复点击 → 只发出一次 `POST /boot/payment`（AC-3/D-3 的前端半边）。
  - 失败：`status:"fail"` → `role="alert"` 显示服务端 message，**页面不出现卡号回显**。
  - 成功 → 导航到 `/confirmation`（用 `Routes` 断言目标页 + 传入 state）。
- `ConfirmationPage.test.jsx`
  - AC-3：给定 order 数据 → 渲染每行与总额、日期。
  - 无 `location.state` → `GET /boot/confirmation` 返回 **204** → 安全空态，无假数据。
  - `location.state` 缺失但端点返回订单 → 正常渲染（刷新场景的近似）；**R2-N2：前端只看 200/204，服务端即使 `checkoutState==pending` 也返回 200 + 上一笔成功订单，前端不得据此自行判断“无订单”**。
- `AddToCartButton.test.jsx`（或并入页面测试）
  - 点击 → `POST /api/boot/cart`，body `movieId=<id>&action=add`；成功用**内联状态文案**（U-7，不用 `alert`）；失败/401 呈现安全文案。
- `MovieListPage.test.jsx` / `MovieDetailPage.test.jsx`（改）
  - D-1：存在加购按钮；点击正确 POST。**现有 movie 测试的 fetch mock 是整页一个 `vi.fn().mockResolvedValue(...)`，加购点击会产生第二个请求，必须按 URL 分派**（N14）。
- `App.test.jsx`（改）
  - 三个路由渲染真实页面（不再是占位标题）；匿名 `/cart`（以及 `/checkout`、`/confirmation`）→ 登录页。
  - **必须扩展 `responseBody(url)` 按 URL 分派**（N13：第 62-67 行现状对未知 URL 返回电影行数据，不扩展会使 `/cart` 拿到错误形状的数据而失去断言意义）。
- 命令（**必须实际运行并报告；不在 CI，属手动验证门**，C-14）：`npm test`（`vitest run`）+ `npm run lint`。

### 7.3 测试覆盖与 AC 的对应

| AC | 后端测试 | 前端测试 |
| --- | --- | --- |
| AC-1 | `CartControllerTest`（既有契约） | `CartPage.test.jsx`（更新/渲染/整数分总额） |
| AC-2 | `CartServiceTest`（真实 service + `MockHttpSession` + stub `CartRepository`）：`updateCart`→`getCart` 行/数量变化（R2-B2） | `CartPage.test.jsx`（每次 mount 重新 GET，不缓存） |
| AC-3 | `PaymentControllerTest`（order + 重放）、`CheckoutControllerTest`（200/204） | `PaymentPage.test.jsx` + `ConfirmationPage.test.jsx` |
| AC-4 | `PaymentControllerTest`（fail 分支 + 无卡号回显） | `PaymentPage.test.jsx`（`role="alert"`、required/type=date、无回显） |
| AC-5 | §7.1 全部（含 filter 401、token 缺失/重放、覆盖率闸门） | §7.2 全部（cart→checkout→confirmation 流程） |

### 7.4 "integration tests" 在本项目中的可行定义（**U-8 已裁定接受此定义**）

仓库**没有** e2e 设施（无 Playwright/Cypress/Selenium；`frontend/package.json` 只有 vitest+jsdom；后端只有 surefire）。因此本计划的 integration 覆盖为：

1. **后端集成（真实 HTTP 层用 MockMvc）**：`@WebMvcTest` + `@MockBean` 现有模式，覆盖 controller 契约、
   `CustomerAuthenticationFilter` 的 401、session 语义、**token 校验/重放/重放优先于空车**。至少需覆盖（U-8）：
   ① 经 `CustomerAuthenticationFilter` 的 401 用例；② token 缺失/重放用例。
2. **前端流程集成（页面级，`fetch` 被 mock）**：cart → checkout → confirmation 的页面级流程测试（U-8 第 ③ 项），断言跨页 URL/导航与请求序列。
3. **不做**：真实浏览器 + 真实 MySQL + 真实 session cookie 的端到端测试（无设施；引入设施属范围外，需单独批准）。

> **手动门声明**：前端 `npm test`/`npm run lint` **不在 CI**，worker 报告必须给出实际命令与结果（C-14）。

> **未覆盖声明**：真实浏览器刷新保持 cart、真实 MySQL `CURDATE()` 的订单日期、真实 `JSESSIONID` 轮换、双标签页并发，在本计划中**没有**自动化覆盖，列为残余风险（§10 / U-6）。

## 8. 依赖

| 依赖 | 状态 | 证据/说明 |
| --- | --- | --- |
| 认证（登录/会话/`JSESSIONID`） | 已完成（#35/#36） | `CustomerAuthController`、`CustomerAuthenticationFilter`、`ProtectedRoute`、`AuthProvider` |
| 共享 API 处理（`apiRequest`、`ApiError`） | 已完成 | `frontend/src/api/apiClient.js` |
| movie list / detail 迁移（#38/#39） | 已完成 | `MovieListPage.jsx`、`MovieDetailPage.jsx`、`MovieTable.jsx` |
| 路由壳（#34） | 已完成 | `AppRoutes.jsx`、`routeDefinitions.js`、`AppShell.jsx` |
| 后端 cart/payment 接口 | 已存在 | `commerce/` 包 |
| 新增：checkout token 端点、`GET /confirmation` 端点、支付响应扩展、legacy `payment.js` 取 token | **依赖本 issue 自行实现** | D-3/D-6/D-8/D-7（B5 方案 a） |

issue 正文的 "Dependency" 行与上述一致（authentication、shared API handling、movie detail/list flows）。

## 9. 约束

| # | 约束 | 来源 |
| --- | --- | --- |
| C-1 | **session cart 语义不变**：cart 存在 session（`cart` 键，`Map<String,Integer>`），按 movie id 排序，未知 movie id 静默丢弃，`add`/`increase` 等价 | `CartService.java` |
| C-2 | 价格是 `movieId.hashCode()` 派生的伪价格（$5.00–$19.99），客户端与服务端必须**同源**（都来自 `GET /cart` 或服务端返回），不得在前端另算 | `CartService.priceFor`；`CartServiceTest` 有 legacy 价格范围断言 |
| C-3 | 无 Spring Security、**无 CSRF token**：任何新增 `POST` 仍依赖 cookie 认证；CSRF 为本 issue out-of-scope，记为残余风险 | `pom.xml`、`src/main` grep、D-4 |
| C-4 | **不得删除/改坏 legacy `src/main/webapp`** 结账页面；**唯一例外：本 issue 允许修改 `src/main/webapp/payment.js`** 以携带 checkout token（D-7/B5 方案 a）。其余 legacy 文件与 `FrontendApiMigrationTest` 的 `contains` 断言保持不变 | `FrontendApiMigrationTest.criticalPagesUseTheirSpringBootEndpoints`；B5 |
| C-5 | 架构守卫：不得引入 servlet API / 不得改变 pom 打包（`jar`+`repackage`） | `SpringBootOnlyArchitectureTest` |
| C-6 | **`routeDefinitions.js` 与 `AppRoutes.jsx`/`App.test.jsx` 必须同步改**（N6）：从 `customerProtectedRoutes` 删除 cart/checkout/confirmation 三条定义，在 `AppRoutes.jsx` 内新增显式路由；`it.each(routeDefinitions)`（`App.test.jsx` 第 85 行）仍由 employee 两条占位覆盖，**不会出现空数组** | `App.test.jsx` 第 85 行；`routeDefinitions.js` 第 3-22 行 |
| C-7 | react-router-dom **7**；页面用 `MemoryRouter` 测试，导航用 `navigate(..., {replace})` 避免重复提交 | `frontend/package.json`、`App.test.jsx` |
| C-8 | 金额：服务端 `CartItem.price`/`total_amount`/`unit_price`/`line_total` 均为 `BigDecimal`（scale 2），JSON 序列化为 number（7.50→7.5）；**前端一律以整数分求和（`Math.round(price*100)`）再格式化展示，不在前端做浮点累加**（N2 已裁定）；服务端是金额唯一权威来源（C-2），前端不得复算金额回传 | `CartItem.java`；`legacy shoppingCart.js:5-16,34` 的浮点累加属既有隐患，**不复制** |
| C-9 | 业务失败一律 HTTP 200 + `{"status":"fail","message":<英文>}`；参数校验错误才是 400 `{"errorMessage":...}`。**token 缺失/不匹配属业务失败，走 200 `status:fail`（U-3 裁定），不用 400** | `PaymentController`、`CommerceApiExceptionHandler`、§4.2 |
| C-10 | 前端表单 POST 用 `URLSearchParams` body（后端 `@ModelAttribute`/`@RequestParam` 接受 form 编码）；`apiRequest` 不设 Content-Type | `apiClient.js`、`customerAuthApi.js`、`CartController` |
| C-11 | 前端目录约定：`features/<name>/<Name>Page.jsx` + `<name>Api.js` + `<Name>Page.test.jsx`；状态走 `components/states/` | 既有 `features/movies`、`features/auth`、`features/stars` |
| C-12 | 不新增 npm 依赖、不引入 e2e 设施（除非单独批准） | `frontend/package.json`；§7.4 |
| C-13 | **后端覆盖率闸门【测试强制】**：CI 执行 `mvn --batch-mode verify`，JaCoCo `BUNDLE LINE COVEREDRATIO ≥ 0.85`。**每个新增/修改的后端类至少要有测试触及其代码行与分支**（DTO getter 经 JSON 序列化断言即可覆盖）；当前实测 873/984 ≈ 0.8872，未覆盖余量仅约 43 行，新增未覆盖代码很容易使 CI 变红 | `.github/workflows/ci.yml:36`；`pom.xml:111-171`；`target/site/jacoco/jacoco.csv`（B1） |
| C-14 | **CI 只跑后端 `mvn verify`**；前端 `npm test` / `npm run lint` **不在 CI**，属**手动验证门**——worker 报告必须给出实际命令与结果，不得声称未运行的门已通过 | `.github/workflows/ci.yml`（B1） |
| C-15 | **不引入 Spring Security / JWT**（D-10 路线 C）：网页端继续 session-based 认证；JWT 仅属未来 Spring AI agent 平台的独立 trust boundary，需另立 issue + 计划 + oracle 审查 | 用户已确认；`pom.xml` 无 security starter |
| C-16 | **session 存储对象必须 `Serializable`（R2-N1）**：任何写入 `HttpSession` 的新对象（本 issue 的 `checkoutOrder` → `OrderSummary`/`OrderLine`）都要 `implements Serializable`（建议 `serialVersionUID = 1L`），与 `AuthenticatedCustomer`/`EmployeeIdentity` 一致；否则内嵌 Tomcat 关闭时持久化 session（`SESSIONS.ser`）会产生 `NotSerializableException` 告警并丢 session | `AuthenticatedCustomer.java:3-6`、`EmployeeIdentity.java:3-6` |
| C-17 | **MockMvc 跨请求测试必须复用同一 `MockHttpSession`（R2-B2）**：`.sessionAttr(...)` 每次 `perform` 都会新建 session，只能用于单请求用例；跨请求 token/重放/空车断言必须 `.session(shared)`（写法见 §7.1） | spring-test `MockHttpServletRequestBuilder`；`PaymentControllerTest` 现有用法 |

## 10. 风险与回退方案

| # | 风险 | 影响 | 缓解 / 回退 |
| --- | --- | --- | --- |
| R-1 | **支付响应契约变更**（新增 `order`）破坏既有断言或 legacy 客户端 | 中 | 只增字段不改 `status`/`message`；legacy `payment.js` 只读 `status`，兼容；同步更新 `PaymentControllerTest`；`@JsonInclude(NON_NULL)` 保证失败响应不出现 `order` |
| R-2 | **token 失败 UX**：用户不知如何继续 | 中 | **O-1 定稿文案**（`"Your checkout session has expired. Please try again."`，200 `fail`，不回显 token）+ 恢复入口（重新取 token / 回 `/cart`）；`INVALID_PAYMENT` 保持 `pending` 可改卡重试；T1 重放语义消除"响应丢失后被报失败"（D-5） |
| R-3 | **刷新丢失订单数据** | **低（已由 D-6 消解）** | session-backed `GET /api/boot/confirmation`（204 → 安全空态）；导航 state 仅作免一次请求的加速路径 |
| R-4 | **legacy 与 React 双入口共存**：legacy `shoppingCart.html`/`payment.html` 仍存在（运行时无静态映射，但源码层会误导 + 测试钉死） | 低-中 | 本 issue 保留 legacy（C-4，仅改 `payment.js`）；记录"清理 legacy 需与 `FrontendApiMigrationTest` 一起改，属后续 issue/需批准" |
| R-5 | **并发/幂等残余（B3 修正后）**：现状与修订后均**不会重复插入 sales**；剩余的只是 token 仅单实例/单 session 有效，跨实例或跨 session（同顾客两台设备）仍可并发下单 | 中-低 | 单实例假设已核实（U-6）；"DB 唯一约束/orders 表"列为需单独批准的后续工作；如实记录为残余风险，**不宣称已彻底解决** |
| R-6 | **`sale_date` 与响应日期不一致**（`CURDATE()` vs `LocalDate.now()`；DB/JVM 时区差，午夜边界可能差一天） | 低（U-1 已裁定接受） | 不读回插入日期，保持 SQL 不变；若未来要求严格一致，再新增 `SELECT CURDATE()` 查询 |
| R-7 | **`routeDefinitions`/`AppRoutes`/`App.test` 联动疏漏** → 前端测试红 | 低 | C-6：三文件同一次改动；§5.2/§5.3 显式列出 |
| R-8 | **复用 `synchronized (cart)` 作 token 临界区**：U-3 已裁定可接受；但 `getOrCreateCart`（第 81-91 行）的"读-创建-写"未加锁，理论上并发首次创建可得不同 `Map` 实例而使锁失效 | 低 | 支付路径要求 cart 非空（已实例化），影响低，**接受并记录**；若 worker 发现该竞态实际可达，暂停并回主 agent |
| R-9 | **CSRF 残余风险**（无 token、cookie 认证的表单 POST） | 中（安全） | D-4 已批准 out-of-scope，但必须**显式记录为本 issue 未解决的安全残余风险**，并在 PR 描述中披露 |
| R-10 | **AC-4 "safe error" 边界**：已裁定（U-4）为"保留 `required` + `type=date` parity，**不**加格式校验" | 低 | 前端只做安全展示；若未来要加格式校验（属新增行为）需另立 issue |
| R-11 | **并发测试可能 flaky；`mvn verify` 全量较慢** | 低-中 | **O-3 配方**（确定性 2 线程 `CountDownLatch` + `thenAnswer`，**直接调用式**）；MockMvc 版可选；不稳定时降级为 token 服务并发单测或移除并发测试，并在报告/本文档如实记录；**不得弱化断言** |
| R-12 | **覆盖率闸门使 CI 变红（B1）**：新增 DTO/controller/service 未配测试，或耗尽约 43 行的余量 | 中-高 | C-13：每个新增/修改类至少一条测试；worker 提交前本地跑 `mvn --batch-mode verify` |
| R-13 | **`payment.js` 改动破坏 legacy 钉死测试** | 低 | 只新增 `api/boot/checkout/token` 调用，保留 `api/boot/cart`/`api/boot/payment` 字符串；改动后跑 `FrontendApiMigrationTest` |
| R-14 | **前端测试不在 CI**：`npm test`/`npm run lint` 变红不会被 CI 拦住 | 中 | C-14：worker 报告必须给出实际命令与结果；主 agent 在交付前复核 |
| R-15 | **订单行一致性边界（R2-N4 已裁定为已知限制）**：`cartService.getCart` 静默丢弃 movies 表中不存在的 movie id（`CartService.java:36-44`），而 `placeOrder` 用原始 `Map.copyOf(cart)` 插入 | 低（需 cart 内含不存在的 movie id 才触发） | 不改 `getCart`/`insertSales`（保持既有行为）；记录为**已知限制，不在本 issue 修**；若要修，另立 issue |
| R-16 | **`completed` 状态下旧 token 会重放旧订单**：用户下单后又加购但页面仍持旧 token → 返回旧订单 success，新 cart 不会被下单 | 低 | 正常流程下 `/checkout` 每次进入都重新 `GET /checkout/token`（置 `pending`）；`PaymentPage` 在取得本页 token 前禁止提交（R2-N3）；作为设计语义记录，**不新增额外端点**（U-9） |
| R-17 | **测试机制误用导致“伪绿”（R2-B2 / B1）**：`.sessionAttr` 每请求新建 session，`@MockBean CartService` 默认返回空 List | 中（断言失效但测试通过） | C-17 + §7.1 的强制写法；断言订单行前必须 stub `getCart`；reviewer 检查跨请求用例是否用 `.session(shared)` |

## 11. 原未决问题（U-1..U-9）的裁定结果

> 第一轮 oracle 审查提出的 U-1..U-9 已由主 agent 逐条裁定，完整依据见 §12。本节记录裁定结果与写入位置。

| # | 原问题 | 主 agent 裁定（已写入位置） |
| --- | --- | --- |
| U-1 | 订单日期来源 | `order_date` 用 JVM `LocalDate.now()`，**不改** `INSERT_SALE_SQL`（仍 `CURDATE()`）；午夜边界/时区差记为低风险（§4.1、§10 R-6） |
| U-2 | 金额表示、响应类型与字段名 | BigDecimal（scale 2）+ snake_case；**扩展 `OperationResponse`**（新增可空 `order`，不新增 `PaymentResponse`）；字段名 `order_date`/`total_amount`/`lines`（D-8、§4.1、C-8） |
| U-3 | token 临界区/并发测试/失效时机/缺 token 状态码 | 复用 `synchronized (cart)`，token 校验+重放+空车+摘要全在同一临界区，不新增锁对象；不在 cart 变更时失效 token（记边界）；缺 token → 200 `status:fail`；并发测试原定为 N5 配方，**第二轮已改定 O-3 配方**（确定性 2 线程直接调用式，MockMvc 版可选），不稳定则降级（§4.2、§7.1.4、C-9、R-8、R-11） |
| U-4 | AC-4 "safe error" 边界 | 保留四字段 `required` + expiration `type="date"`（既有行为 parity），**不加**格式校验；错误只安全展示服务端 `fail` 文案（§2、§5.2、§6.2、D-9、R-10） |
| U-5 | sessionStorage 键清理时机 | **由 D-6 直接消解**：改用 session-backed `GET /api/boot/confirmation`，不再有客户端订单存储；`LogoutButton.jsx` 不改（§4.3） |
| U-6 | 部署假设（单实例） | **单实例假设成立**：`compose.yaml` 仅 mysql（第 1-24 行），`pom.xml` 无 `spring-session`/Redis（§3.2、§10 R-5） |
| U-7 | 加购反馈方式 | 用**内联状态文案**（可测），不用 `alert`；记为"已批准的呈现差异"（D-9、§5.2） |
| U-8 | integration 定义 | **接受 §7.4 定义**（后端 MockMvc + 前端页面级 fetch mock），并要求至少覆盖：经 `CustomerAuthenticationFilter` 的 401、token 缺失/重放、cart→checkout→confirmation 前端流程；e2e 设施不在本 issue（引入需单独批准），未覆盖项留在 §7.4/§10 |
| U-9 | token 端点路径 / 是否需要"重新开始结账"端点 | `GET /api/boot/checkout/token` **路径不变**；**不需要**额外端点（重新 GET token 即可）（§6.1） |

### 11.1 原待复审点（O-1..O-5）——第二轮复审后**全部关闭**

| # | 待复审项 | 第二轮裁定结果（写入位置） |
| --- | --- | --- |
| O-1 | 失败/重放安全文案 | **已关闭（定稿）**：token 缺失/不匹配 → 200 `fail` + `"Your checkout session has expired. Please try again."`（不回显 token）；`completed` 重放不新增文案，返回原 `success` + `"Order placed successfully."` + 原 `order`；"已处理"候选已删除（§4.2、§6.1、§6.2、R-2） |
| O-2 | `CheckoutController` 是否同时承载 `GET /confirmation`；`CheckoutTokenService` 拆分粒度 | **已关闭（结构固定）**：`CheckoutController` **同时**承载 `GET /api/boot/checkout/token` 与 `GET /api/boot/confirmation`；token 签发/校验/重放逻辑放入**可单测的 `CheckoutTokenService`**；**worker 不得再改结构**，若有技术阻碍暂停询问主 agent（§5.1、§7.1.2） |
| O-3 | 并发测试在 MockMvc 下能否稳定 | **已关闭（主用方案固定）**：主用**确定性 2 线程单测**（`CountDownLatch` + 共享 session/cart，**直接调用** `PaymentController`/`CheckoutTokenService`，断言 `placeOrder` 恰一次）；MockMvc 版列为可选；不稳定时按降级路径处理并如实记录，**不得弱化断言**（§5.3、§7.1.4、R-11）。**实测结果已回填（2026-10-14）**：`PaymentControllerConcurrencyTest` 单独重跑 3/3 稳定通过；已把初版的 `Thread.sleep` 机会式重叠改为**有界轮询等待第二线程进入 `Thread.State.BLOCKED`（cart monitor）**，确定性重叠，无需降级且未弱化任何断言（详见 §13.1）。 |
| O-4 | `lines` 是否保留 `sale_lines` 别名 | **已关闭**：**删除** `sale_lines` 别名说明，字段名只用 `lines`；issue 原文用词只在 issue 跟踪文档保留（§4.1） |
| O-5 | 第一轮修订对第二轮是否完全闭合 | **已关闭**：第二轮提出 R2-B1/R2-B2 两项必须修订 + R2-N1..N5 建议，均已由主 agent 裁定并写入本次终稿；**第一轮 + 第二轮全部闭合**（§12.1） |

## 12. Oracle 建议与主 agent 决定

> **第一轮审查已完成**（`.pi/artifacts/oracle-issue-30-plan-review.md`），B1–B6 / N1–N14 / U-1..U-9 已逐条裁定；**第二轮复审（§12.1）后主 agent 已批准本终稿**。下表为第一轮裁定与写入位置；未列入的 oracle 观察不影响范围。

| 来源 | 建议（摘要） | 主 agent 裁定 | 已写入本文档 |
| --- | --- | --- | --- |
| B1 | CI 有 JaCoCo `BUNDLE LINE ≥ 0.85` 闸门（`mvn verify`），当前 873/984 ≈ 0.8872，余量约 43 行；新增后端类必须配测试；§5.3 补测试清单与降级预案 | **接受** | C-13/C-14（§9）、§7.1「必须项」、§5.3、R-12；架构文档 §7/§10 |
| B2 | 幂等语义选 T1（token 兼作 idempotency key；成功后重放同一成功响应），而非现稿的“第二次一律 fail” | **接受 T1**（用户/主 agent） | D-5、§4.2、§6.1/§6.2、§10 R-2 |
| B3 | §4.2 对现状的描述与代码不符：快照在锁内，**不会重复插入 sales**；真实缺陷是假成功且无测试；把 `cart.isEmpty()` 移入临界区 | **接受**（删除原“重复插入 sales”表述） | §1.2（PaymentController 并发现状）、§4.2、§10 R-5、§5.1 |
| B4 | 新 `CheckoutController` 不在 `CommerceApiExceptionHandler` 覆盖内；选方案 a（加入 `assignableTypes`） | **接受方案 a** | §5.1（“不改”已作废）、§7.1.2 |
| B5 | 强制 `checkoutToken` 与“不碰 legacy”冲突；选方案 a（同步改 `payment.js` 取 token） | **接受方案 a**（用户已选 a） | D-7、C-4、§5.4、§1.2 |
| B6 | 事实错误：legacy 四字段本有 `required`，expiration 是 `type="date"` | **接受** | §1.2、§2、§5.2、§6.2、U-4/R-10 |
| N1 | 改用 session-backed `GET /api/boot/confirmation`（200/204），不用 sessionStorage | **接受**（用户已选 a） | D-6、§4.3、§6.1、§5.2 |
| N2 | 前端用整数分求和（`Math.round(price*100)`）再格式化展示 | **接受** | C-8、§5.2、§7.2 |
| N3 | 字段/类型定稿；**扩展 `OperationResponse`**（可空 `order`）而非新增 `PaymentResponse` | **接受** | D-8、§4.1、§5.1 |
| N4 | 摘要用锁内 `cartService.getCart(session)` 同源组装，不在 controller 重算价格 | **接受** | §4.1、§5.1、§1.2 |
| N5 | 并发测试配方：`@WebMvcTest` + `CountDownLatch` + `thenAnswer`；不稳定则降级 | **接受** | §7.1.4、§5.3、R-11 |
| N6 | 从 `customerProtectedRoutes` 删除 cart/checkout/confirmation 三条，`AppRoutes` 内新增显式路由 | **接受** | C-6、§5.2、§7.2 |
| N7 | 修正计划/架构文档中的行号引用 | **接受**（已按实际源码重新核对） | §1.2（多处）、§6.2、§9 C-6/C-8、§11 U-6；架构文档 §3 |
| N8 | 失败响应可加机器可读 `code` | **接受“不加”** | D-8、§4.1 已否决候选 |
| N9 | `checkoutToken` 作为 `PaymentRequest` 新字段由 `@ModelAttribute` 绑定 | **接受** | §5.1、§4.2 |
| N10 | 架构文档补 B1 两条约束 | **接受** | 架构文档 §7/§10 |
| N11 | §5.3 补全测试文件清单与降级预案 | **接受** | §5.3 |
| N12 | AC-2 近似验证拆为两条（前端每次 mount 重新 GET；后端同 session POST→GET） | **接受** | §2 AC-2、§7.1.6、§7.2 |
| N13 | `App.test.jsx` 通用 fetch stub 必须按 URL 分派 | **接受** | §5.3、§7.2、§1.2 |
| N14 | `AddToCartButton` 抽成单一组件供两页共用 | **接受** | §5.2、§7.2、§3.1 |
| U-1..U-9 | 见 oracle 报告 §3 | **全部已裁定**，结果见本文 §11 | §11（含写入位置） |
| JWT | 路线 C：网页端继续 session 认证；JWT 仅保留给未来 Spring AI agent 平台，不属 #30/#31 | **用户已确认** | D-10、C-15、§3.2；架构文档 §10 |

> **第一轮审查已完成。** 复审判定若有新阻塞项，回到主 agent 裁定，docs-writer 不自行决定。**实现后的三路 reviewer 结论、接受/驳回项与终验结果见 §12.1 末尾的“审查与修复结果”小结，逐项落点见 §13.1。**

### 12.1 第二轮复审（`.pi/artifacts/oracle-issue-30-plan-review-round2.md`）—— 主 agent 全部接受，已写入终稿

> 第二轮结论：第一轮 B1–B6 全部闭合；新增 **R2-B1**、**R2-B2** 两项必须修订（均为实现层技术细节，**不改变已批准的范围 D-1..D-10、契约形状或安全边界，无需用户重新决策**）+ 5 条建议（R2-N1..N5）。主 agent 对以下 12 条裁定**全部接受**。

| 来源 | oracle 建议（摘要） | 主 agent 裁定 | 已写入本文档 |
| --- | --- | --- | --- |
| R2-B1 | Blocker（设计）：摘要在 `placeOrder` **之后**组装 → 摘要组装抛异常时 sales 已提交、cart 未清、`state` 仍 `pending`，同 token 重试会**重复下单**；改为**先组装摘要（含 DB I/O）→ 再 `placeOrder` → 成功后纯内存记账 + 清空** | **接受** | §4.1（关键时序 = “组装先于下单”）、§4.2（锁内 6 步 + 窗口消除说明）、§5.1（`PaymentController` 行） |
| R2-B2 | Blocker（测试机制）：T1 跨请求测试必须用**同一 `MockHttpSession`**（`.session(shared)`）；`.sessionAttr(...)` 每次 `perform` 新建 session；AC-2 后端半边应移到 `CartServiceTest` | **接受** | §2 AC-2、§5.3（`PaymentControllerTest`/`CartServiceTest` 行）、§7.1（强制写法块 + 第 1/6 项重写）、C-17、R-17 |
| R2-N1 | 新增 DTO（`OrderSummary`/`OrderLine`）写入 session 应 `implements Serializable` | **接受** | §4.1、§5.1（两个 DTO 行 + R2-N1 说明块）、C-16、架构文档 §9 |
| R2-N2 | 固定 `GET /api/boot/confirmation` 语义：存在 `checkoutOrder` 即 200（即使 `pending`）；无则 204；新单覆盖旧单；登出后消失 | **接受** | §4.3、§6.1、§6.2、§7.2 |
| R2-N3 | `PaymentPage` 必须取得 token 后才允许提交；token 获取失败显示安全错误且不得用陈旧 token 提交 | **接受** | §5.2（`PaymentPage` 行）、§6.2、§7.2 |
| R2-N4 | 记录已知边界：`getCart` 丢弃 movies 表中不存在的 movie id，而插入用原始 map → 展示行与插入行可能不一致（本 issue 不修） | **接受（仅记录）** | §4.1、§10 R-15 |
| R2-N5 | 行号微调：`apiClient.js` 的 `credentials: 'include'` 在第 53 行；`payment.js` status 分支 34-39；架构文档 `criticalPagesUseTheirSpringBootEndpoints` 方法体 24-35、`routeDefinitions.js` 的 `customerProtectedRoutes` 3-22 | **接受** | §1.2（两处）、架构文档 §1/§2；其余 oracle 本轮确认为正确的行号未动 |
| O-1 | 文案定稿：token 缺失/不匹配 → 200 `fail` + expired 文案（不回显 token）；`completed` 重放不加文案；删除“已处理”候选 | **接受** | §4.2（文案段 + 时序表）、§6.1、§6.2、R-2、§11.1 |
| O-2 | 结构固定：`CheckoutController` 承载两个 GET；token/状态逻辑放可单测的 `CheckoutTokenService` | **接受** | §5.1、§7.1.2、§11.1 |
| O-3 | 并发测试主用确定性 2 线程单测（共享 session/cart、直接调用 controller/service），MockMvc 版可选；不得弱化断言 | **接受** | §5.3、§7.1.4、R-11、§11.1（实测结果已于 2026-10-14 回填） |
| O-4 | 删除 `sale_lines` 别名说明 | **接受** | §4.1 |
| 覆盖率测试陷阱 | `@MockBean CartService` 未 stub `getCart` 默认返回空 List；必须覆盖的分支清单；~200-250 行新增需接近全覆盖才能守住 85% | **接受** | §7.1（覆盖率陷阱 + 分支清单）、C-13、R-12/R-17 |

**闭合结论：第一轮（B1–B6 / N1–N14 / U-1..U-9）+ 第二轮（R2-B1 / R2-B2 / R2-N1..N5 / O-1..O-5）全部闭合。** 主 agent 已据此批准本文档（§13）。仍开放的是 O-3 的**实测**结果（需 worker 运行并发测试后回填）与实现期间若出现技术阻碍需回主 agent 裁定。

**审查与修复结果（实现后回填，2026-10-14）**——本节只记结论，不重复 reviewer 全文；逐项落点见 §13.1。

- **三路只读 reviewer（正确性与边界 / 测试覆盖 / 必要性复杂度）均无 P0/P1 Blocker**；相关数字由主 agent 独立复核。
- **主 agent 接受并已修复**：P2 token 恢复入口（`PaymentPage` 的 "Start checkout again" 重取 cart+token）、legacy token 改为页面加载时获取并复用、删除死代码 `OperationResponse.success(String)`、抽取共享 `money.js`+测试、CSS 两处副作用修复（`<td>` 上的 `display:flex`、`.movie-table td:last-child` 误指向）、并发测试确定性改造、补计划 §4.2 强制用例（token 重签 / 新 token 二次下单 / 旧 token 重放）、补 AC-5 前端跨页流程测试（`checkoutFlow.test.jsx`）、断言与匹配器加固、去重复断言、失败消息常量化。
- **驳回并记录理由**：`toCents` 的 NaN 防御（服务端契约保证 JSON number，当前不可达）；逐页 `credentials` 断言与 `/confirmation` 重复 401 用例（`apiClient` 与 `CustomerAuthenticationFilter` 已集中覆盖）。
- **主 agent 终验（亲自运行）**：`mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS、`Tests run: 120, Failures: 0, Errors: 0, Skipped: 0`、`All coverage checks have been met`；`npm test` → 24 files / 116 tests passed；`npm run lint` → exit 0 无 warning；`PaymentControllerConcurrencyTest` 单独重跑 3/3 稳定。

## 13. 状态

- 当前状态：**实施完成，已通过主 agent 终验（2026-10-14）——已提交（`d6b3e07` 实现提交、`eeb935f` 文档提交）并推送；PR [#40](https://github.com/PNZ-z/ai-powered-Fabflix/pull/40)（标题 “Migrate cart, payment, and confirmation flows to React”；base `main` ← head `codex/issue-30-cart-payment-confirmation`；GitHub 已识别 `Closes #30`）——已于 2026-10-16 由主 agent 合并：`state=MERGED`、`mergedAt=2026-10-16T01:32:59Z`、merge commit `91547499d09e5a16419871843cb86936261cf594`（短 SHA `9154749`）；Issue #30 已自动关闭（`CLOSED`）、远端分支已删除、`main` 上该 merge commit 的 CI（`Build and test (Java 11)`）SUCCESS**。本次变更已全部提交并随 PR #40 合入 `main`（合并前位于分支 `codex/issue-30-cart-payment-confirmation`）。**历史快照（合并前的 #30 工作区记录）**：当时 `git status --short` 仅剩进入该任务前已存在的 `frontend/package-lock.json` 修改与未跟踪的 `.idea/`、`.pi/`、`AGENTS.md`，无 staged 文件；其中 `.pi/` 与 `AGENTS.md` 现已由本次发布状态更新分支的 `.gitignore` 规则忽略（见工作流日志 §4.2）。
- **发布状态记录（2026-10-16 回填）**：合并与关闭的事实、纳入合并的提交清单、以及本状态更新分支的 `.gitignore` 附带改动，均记录在工作流日志 `docs/logs/2026-10-14-issue-30-migrate-cart-payment-confirmation-flows.md` **§4「合并与关闭」**；本次更新**未改动**任何实测数字、残余风险与 AC 证据。
- 流程回顾：§12（第一轮裁定）+ §12.1（第二轮裁定）全部闭合 → 主 agent 批准 → worker 实现 → 三路 reviewer → worker 修复 → 主 agent 终验（结果小结见 §12.1，逐项落点与实测数字见 §13.1）。
- **§11.1 的 O-1..O-5 已全部关闭**，其中 O-3 的实测结果已回填（§11.1）。
- **与批准范围的偏离**：仅 §13.1 列出的 8 项（含 3 个主 agent 批准的新增文件与 1 处死代码删除），**未改变已批准契约、范围或安全边界**；无未批准的偏离。
- **未完成项**：提交、推送、PR 创建**均已完成（2026-10-14）**；**PR #40 已合并且 Issue #30 已关闭（2026-10-16）**——合并后 `main` 的 CI（`Build and test (Java 11)`，merge commit `9154749`）SUCCESS，PR 链接与合并/关闭信息已回填至本文档、issue 跟踪文档与工作流日志 §4。**PR #40 审查后的两批后续修复（提交 `6b637a4`、`048debf`）均已推送并纳入本次合并**，详细见 §13.1。
- **文档口径**：§1–§12 保持批准时的计划表述；**实现结果与实测证据集中在 §13.1**；逐文件变更、遗留风险与知识点见工作流日志 `docs/logs/2026-10-14-issue-30-migrate-cart-payment-confirmation-flows.md` 与 issue 跟踪文档 `docs/issues/issue-30-migrate-cart-payment-confirmation-flows.md`。

### 13.1 实现结果与计划差异

**实际文件清单（与 §5 对照；最终以 `git status` / diff 为准）**

- 后端新增（`src/main/java/com/fabflix/boot/commerce/`）：`CheckoutController.java`、`CheckoutTokenService.java`、`OrderSummary.java`、`OrderLine.java`——与 §5.1 一致。
- 后端修改：`PaymentController.java`（T1 六步时序）、`PaymentRequest.java`（`checkoutToken`）、`OperationResponse.java`（可空 `order`；**并删除无调用方的 `success(String)` 重载**）、`CommerceApiExceptionHandler.java`（`assignableTypes` 加入 `CheckoutController`）。
- 后端测试新增：`CheckoutControllerTest`、`CheckoutTokenServiceTest`、`PaymentControllerConcurrencyTest`；修改：`PaymentControllerTest`、`CartServiceTest`（§5.3 允许 `CheckoutControllerTest` / `CheckoutTokenTest` 二选一，实际取前者并另建 `CheckoutTokenServiceTest`）。
- 前端新增：`features/cart/{cartApi.js, CartPage.jsx, CartPage.test.jsx, AddToCartButton.jsx, AddToCartButton.test.jsx, money.js, money.test.js}`、`features/checkout/{paymentApi.js, PaymentPage.jsx, PaymentPage.test.jsx, ConfirmationPage.jsx, ConfirmationPage.test.jsx, checkoutFlow.test.jsx}`。
- 前端修改：`app/{AppRoutes.jsx, routeDefinitions.js}`、`App.test.jsx`、`features/movies/{MovieTable.jsx, MovieDetailPage.jsx, MovieListPage.test.jsx, MovieDetailPage.test.jsx}`、`index.css`。
- legacy：`src/main/webapp/payment.js`（唯一被改的 legacy 文件，D-7）。
- **未改动**：`CartService` / `PaymentService` / `PaymentRepository` / `PaymentResult`、`frontend/src/api/apiClient.js`、`features/auth/**`、`pom.xml`、`createtable.sql`、`stored-procedure.sql`、`vite.config.js`、`package.json`。
- **工作区无关项（不属本次变更）**：`frontend/package-lock.json` 的既有修改，以及未跟踪的 `.idea/`、`.pi/`、`AGENTS.md`。（该句为 2026-10-14 的实现时点记录；其中 `.pi/`、`AGENTS.md` 已于 2026-10-16 由发布状态更新分支的 `.gitignore` 规则忽略，见工作流日志 §4.2。）

**关键实现位置**

- T1 幂等：`PaymentController.pay` 在 `synchronized (cart)` 内按序执行 token 校验 → `completedOrder` 重放（**先于空车判断**）→ 空车判断 → 组装摘要（含 `cartService.getCart(session)` 的 DB I/O）→ `placeOrder` → 成功后 `completeOrder(...)` + `cart.clear()`；支付失败保持 `pending` / 保留 token / 不清 cart；token 缺失或不匹配 → 200 `{"status":"fail","message":"Your checkout session has expired. Please try again."}`。
- `GET /api/boot/checkout/token`：锁内签发并置 `pending`，不清 cart（`CheckoutController.issueToken` + `CheckoutTokenService.issueToken`）。
- `GET /api/boot/confirmation`：`recentOrder`（不论 state）有则 200、无则 204（R2-N2 语义）。
- 契约：`order_date` = `LocalDate.now()`；`total_amount`/`unit_price`/`line_total` 为 `BigDecimal` scale 2；JSON snake_case；两个 DTO `implements Serializable`。
- 前端：`PaymentPage` 先 GET cart、非空才 GET token；提交失败显示 "Start checkout again" 调 `retry()` 重取 cart+token；金额一律经共享 `features/cart/money.js`（整数分）；确认页优先导航 state、回退 `GET /boot/confirmation`、204 安全空态；`AddToCartButton` 单组件被 `MovieTable` 与 `MovieDetailPage` 共用。
- 路由：`routeDefinitions.js` 删除 cart/checkout/confirmation 三条占位（`customerProtectedRoutes` 现为 `[]`，`routeDefinitions` 仍含 employee 两条，`it.each` 不空转）；`AppRoutes.jsx` 新增三条显式路由；`App.test.jsx` 的 `responseBody(url)` 按 URL 分派。

**与计划的差异（均已获主 agent 批准，未改变契约、范围或安全边界）**

| # | 差异 | 批准与理由 |
| --- | --- | --- |
| 1 | **新增清单外文件** `features/cart/money.js`、`money.test.js`、`features/checkout/checkoutFlow.test.jsx` | 主 agent 批准的**小幅新增**：`money.js` + 测试消除三处重复的整数分 helper（reviewer 接受项）；`checkoutFlow.test.jsx` 补 AC-5 的前端跨页 cart→checkout→confirmation 流程（U-8 第 ③ 项）。 |
| 2 | **删除** `OperationResponse.success(String)` 单参重载 | 改为 `success(String, OrderSummary)` 后该重载无任何调用方（main+test 已 grep）；属死代码清理，不影响外部契约，删除后 `OperationResponse` 覆盖率为 0 missed / 11 covered。 |
| 3 | `index.css` 两处副作用修复 | ① `.cart-table__actions` 不再在 `<td>` 上用 `display:flex`，改为内层 `span` + `white-space: nowrap`；② 删除 `.movie-table td:last-child` 的误指向，改为按显式类名（`movie-table__rating` / `movie-table__actions`）。 |
| 4 | 并发测试改为**有界轮询等待第二线程 `BLOCKED` 于 cart monitor** | 替换初版的 `Thread.sleep(50)` 机会式重叠，得到确定性重叠；未弱化断言（O-3 要求）。 |
| 5 | `PaymentPage` 改为顺序加载（先 cart、非空才取 token）并新增 "Start checkout again" 恢复入口 | 修复 reviewer 接受的 P2：空车不发 token 请求；token 过期（多标签页 / StrictMode 双执行轮换）后可自行恢复，无需整页刷新。 |
| 6 | legacy `payment.js` 的 token **在页面加载时获取一次并复用**（非每次提交前取） | 修复 reviewer 接受项：避免每次提交轮换 token 而破坏重放语义；`api/boot/cart`、`api/boot/payment` 字符串保留，`FrontendApiMigrationTest` 保持绿。 |
| 7 | 未创建 §5.3 标注为“可选”的 `cartApi.test.js` | 参数构造经 `CartPage.test.jsx` / `checkoutFlow.test.jsx` 覆盖。 |
| 8 | 抽取失败消息常量（`INVALID_PAYMENT_MESSAGE` / `CUSTOMER_NOT_FOUND_MESSAGE`） | 与既有 `ORDER_PLACED_MESSAGE` 风格统一（reviewer 接受项）。 |

**审查后后续修复（PR #40 审查通过之后 → 第一批 `6b637a4`、第二批 `048debf`，2026-10-14）**

PR #40 审查通过**之后**（该 PR 已于 2026-10-16 合并为 `9154749`），用户又提出两批后续修复，均已并入同一分支 `codex/issue-30-cart-payment-confirmation`：

- **第一批**落在提交 **`6b637a4`**（`feat: improve movie list layout, sorting controls, and caching`）——3 组修复（表格宽度回归、排序控件重做、movie list 前端缓存）。**状态：已提交、已推送并随 PR #40 合并（`9154749`，2026-10-16）**。
- **第二批**落在提交 **`048debf`**（`feat: add cart badge and add-to-cart toast`）——用户随后**直接提出**的 3 组 UI 修复（加购成功 toast、导航栏 Cart 件数徽标、结算页间距）。**状态：已提交、已推送并随 PR #40 合并（`9154749`，2026-10-16）**。

推送前 `git status -sb` → `codex/issue-30-cart-payment-confirmation...origin/codex/issue-30-cart-payment-confirmation [ahead 1]`；`git rev-parse HEAD` = `048debf9c1e08919c4b566d266bf0ade530dbc16`（第二批提交；第一批 `6b637a4` = `6b637a452a0b03229032af29ddb9b91999e9c24d` 为其父提交）。**2026-10-16 回填：两批提交均已推送，并随 PR #40 合并入 `main`（merge commit `9154749`）；合并状态记录见工作流日志 §4。**

| # | 修复组 | 关键变更 | 涉及文件 |
| --- | --- | --- | --- |
| F-1 | 表格宽度回归修复 | `.page-content > * { min-width: 0; max-width: 100% }` 阻止 grid 子项被表格 min-content 撑破；`.movie-table-wrap { max-width: 100% }`；Title / Director / Genres / Stars 改 `white-space: normal`（**Year / Rating / Actions 保持 nowrap**）；`.cart-table` / `.confirmation-table` 首列可换行；cart / checkout / confirmation 页 `width: min(760px, 100%)` | `index.css`；`MovieTable.jsx`（补显式列类名，不再用位置选择器） |
| F-2 | 排序控件重做 | 字段选择由下拉框改为**始终可见 radio pills（Title / Rating）**；两行 `Sort by` / `then`（全页仅一个 "then"）；方向仍为 Ascending/Descending `select`；`Apply` 在右侧上部；`Results per page` 降为 **12px 弱化**小控件（排序区 15px）；`.movie-list-controls` 改 `width: fit-content; margin-inline: auto` 整体居中（不再左右贴边）；≤760px 单列不溢出 | `MovieListPage.jsx`；`index.css` |
| F-3 | movie list 前端缓存（方案 A） | 新增进程内 SWR 缓存：键 = 规范化后端请求路径（`movieListRequestPath`）、TTL `60_000`；新鲜命中直接渲染**不发请求**；过期先渲染旧数据并后台静默 revalidate（**失败保留旧数据**）；同 key in-flight 去重（顺带消除 StrictMode 双请求）；错误态 `Try again` 清条目并绕过缓存；滚动位置存 `sessionStorage`（前缀 `fabflix.movieListScroll:`，与 `fabflix.lastMovieQuery` 区分）并在首次成功渲染后恢复；无新依赖、未改 `apiClient.js`、未改后端契约 | `movieListCache.js`（新增）；`movieListApi.js`；`MovieListPage.jsx`；`MovieListPage.test.jsx`；`movieListCache.test.js`（新增）；`movieListApi.test.js`；`App.test.jsx` |
| F-4 | 加购成功 toast + 导航栏 Cart 件数徽标（**第二批，提交 `048debf`**） | ① 加购成功后由**右下角固定小弹窗**（`Added "<title>" to your cart.`）替代原来的按钮内联成功文案：**2 秒自动消失**、可手动关闭（`aria-label="Close notification"`）、底部 `Don't show again` 复选框（勾选立即关闭并持久化 `localStorage['fabflix.hideAddToCartToast']='true'`，此后不再弹但加购与计数照常）；同一时刻只保留一个 toast（重复加购更新文案并重置计时）；失败/401 仍为内联 `role="alert"`、**不弹 toast**。② `AppShell` 拆出 `AppShellLayout` 并由 `CartStatusProvider` 包住（header + `<Outlet/>`），Cart 链接右上角渲染红底圆形徽标：数字 = `GET /boot/cart` 各行 **quantity 之和**，`0` 不渲染、`1–9` 显示数字、**`≥10` 显示 `...`**（真实件数放在链接 `aria-label`，如 `Cart, 11 items in your cart`）；加购成功与 `CartPage` 的 `+`/`−`/`Delete` 后刷新，登出/401 清零，其它刷新失败保留上次值；计数与 `sessionKey` 绑定以免跨会话陈旧值。未新增端点、未改 `cartApi.js` / `apiClient.js` / 后端契约 | `CartStatusProvider.jsx`（新增）；`cartStatusContext.js`（新增）；`CartBadge.jsx`（新增）；`AppShell.jsx`；`AppShell.test.jsx`（新增）；`AddToCartButton.jsx`；`AddToCartButton.test.jsx`；`CartPage.jsx`；`CartPage.test.jsx`；`checkoutFlow.test.jsx`；`MovieListPage.test.jsx`；`MovieDetailPage.test.jsx`；`index.css` |
| F-5 | 结算页间距修复（**第二批，提交 `048debf`**） | `.cart-page__checkout { margin-top: 16px }`（`Total: $X` 与 `Proceed to payment` 之间，**主要修复**）；一致性补齐 `.checkout-page__status { margin-top: 12px }`、`.checkout-page__back, .confirmation-page__back { margin-top: 24px }`（对齐既有 `.movie-list-page__back`）。仅 CSS，未改字号/布局/JSX/测试 | `index.css` |

- **第一批实测（主 agent 亲自运行，2026-10-14；docs-writer 事后独立复跑前端）**：`npm test` → **25 files / 142 tests passed**（本次前端改动前为 24 files / 116 tests）；`npm run lint` → **exit 0**；`npm run build` → **成功**。后端**未重跑** `mvn --batch-mode verify`（`6b637a4` 仅含前端文件，后端零改动）——上文「测试与覆盖率实测」的 120 tests / 947 of 1057 = 0.8959 仍为最近一次后端全量验证结果，**未改动**。
- **未改变**原契约、范围与安全边界：URL 参数与后端 API 契约不变、`apiClient.js` 未改、无新依赖、无 schema/legacy 改动；**AC-1..AC-5 状态不变**（仍为「已完成」，证据同 §2 映射与 §13.1 上文）。
- **残余/边界**：缓存为**进程内内存**（刷新即失效）；管理员新增影片需等 TTL（60s）或显式 `retry`；revalidate 失败静默；滚动恢复依赖 `sessionStorage`。逐条见工作流日志 `docs/logs/2026-10-14-issue-30-migrate-cart-payment-confirmation-flows.md` §3.5。
- **推送状态**：**已推送并随 PR #40 合并**（提交 `6b637a4`，分支 `codex/issue-30-cart-payment-confirmation`；当时本地 ahead 1 → 已于 2026-10-16 合并入 `main`，merge commit `9154749`；发布状态唯一权威记录见工作流日志 §4.1）。

**第二批后续修复（提交 `048debf`，2026-10-14）——实测与边界**

- **实测（主 agent 亲自运行，2026-10-14）**：`npm test` → **26 files / 155 tests passed**（本批改动前为 25 files / 142 tests）；`npm run lint` → **exit 0**；`npm run build` → **成功**。后端**未重跑** `mvn --batch-mode verify`（`048debf` 仅含前端文件，后端零改动）——「测试与覆盖率实测」表中 120 tests / 947 of 1057 = 0.8959 仍为最近一次后端全量验证结果，**未改动**。
- **新增测试**：`AppShell.test.jsx`（新增，9 条，含最小有状态 `/boot/cart` 假服务）与 `AddToCartButton.test.jsx`（3 → 7 条）；既有断言**未删除或弱化**，仅页面测试的 render helper 增加 `AuthContext.Provider`(anonymous) + `CartStatusProvider` 包裹（anonymous 下 provider 不发取数请求，既有请求次数/序列断言不变）。
- **未改变**原契约、范围与安全边界：未新增端点/字段、`cartApi.js` 与 `apiClient.js` 未改、无新依赖、无 schema/legacy 改动；**AC-1..AC-5 状态不变**（仍为「已完成」，证据同 §2 映射）。
- **残余/边界**：徽标计的是**件数之和**（`Σ quantity`，非行数）；`≥10` 视觉上只显示 `...`，精确值仅在 `aria-label`；`Don't show again` 是**单向开关**（当前无「重新开启提示」入口）；每次成功加购与每次 `CartPage` 变更会**多一次 `GET /boot/cart`**（徽标刷新，未本地推算）；徽标为内存态、不跨标签页同步；**jsdom 无布局**，toast 位置/2 秒消失观感与窄屏通栏需人工在浏览器确认。逐条见工作流日志 `docs/logs/2026-10-14-issue-30-migrate-cart-payment-confirmation-flows.md` §3.7–§3.10。
- **推送状态**：**已推送并随 PR #40 合并**（提交 `048debf`，分支 `codex/issue-30-cart-payment-confirmation`；当时本地 ahead 1 = 该提交 → 已于 2026-10-16 合并入 `main`，merge commit `9154749`；发布状态唯一权威记录见工作流日志 §4.1）。

**测试与覆盖率实测（主 agent 亲自运行，2026-10-14）**

| 命令 | 结果 |
| --- | --- |
| `mvn --batch-mode --no-transfer-progress verify` | **BUILD SUCCESS**；`Tests run: 120, Failures: 0, Errors: 0, Skipped: 0`；`All coverage checks have been met` |
| JaCoCo BUNDLE 行覆盖 | **947 covered / 110 missed / 1057 total = 0.8959**（≥0.85 闸门；修复前 947/1058 = 0.8951；批准时基线 873/984 ≈ 0.8872） |
| `mvn --batch-mode -Dtest='PaymentControllerConcurrencyTest' test` | 单独重跑 **3/3** 稳定通过 |
| `npm test`（`frontend/`） | **24 files / 116 tests passed** |
| `npm run lint`（`oxlint src`） | **exit 0，无 warning** |

> 上表为 #30 实现终验（`d6b3e07` / `eeb935f`）时点的后端结果；**PR #40 审查后修复提交 `6b637a4`（第一批）的前端实测（25 files / 142 tests）见上方 F-1..F-3 小节，提交 `048debf`（第二批）的前端实测（26 files / 155 tests）见上方 F-4/F-5 小节**。

**残余风险（如实记录，未解决）**：无真实浏览器 / 真实 MySQL e2e（U-8/§7.4）；单 JVM / 同 session 幂等假设（AD-2、R-5）；CSRF 保持现状（D-4、R-9）；`CartService.getCart` 丢弃 movies 表中不存在的 movie id（R-15）；`order_date`（JVM `LocalDate.now()`）与 `sales.sale_date`（`CURDATE()`）在午夜/时区边界可能差一天（R-6）；`PaymentController` 的 `switch default: throw` 属既有模式，1 行未覆盖。逐条说明与后续事项见工作流日志 `docs/logs/2026-10-14-issue-30-migrate-cart-payment-confirmation-flows.md`。

# 工作流日志 — Issue #30 Migrate cart, payment, and confirmation flows

> 记录一次完整工作流（计划 → oracle 审查 → 实现 → 三路审查 → 修复 → 终验 → 文档）的实际结果。
> 本文档只记录**已发生的事实与已运行的验证**；未运行的检查在「未验证项」中明确标出。

## 元数据

| 项目 | 值 |
| --- | --- |
| 完成日期 | 2026-10-14 |
| GitHub issue | #30 — https://github.com/PNZ-z/ai-powered-Fabflix/issues/30（远端 issue，未新建） |
| 分支 | `codex/issue-30-cart-payment-confirmation` |
| 提交 / 推送 | **未提交、未推送**（`git status --short` 全部改动仍为未提交状态；`git diff --cached --name-only` 为空） |
| PR | **未创建（待主 agent 授权）**；链接由主 agent 回传后补入本日志与 issue 文档 |
| 计划文档 | `docs/plans/issue-30-migrate-cart-payment-confirmation-flows.md`（§12/§12.1 裁定，§13.1 实现结果） |
| Issue 跟踪文档 | `docs/issues/issue-30-migrate-cart-payment-confirmation-flows.md` |
| 架构约束 | `docs/architecture/frontend-migration-contracts.md` |
| 关联内部产物 | worker 初版报告 `.pi/artifacts/worker-issue-30-implementation-report.1b62ef0e.md`；worker 修复报告 `.pi/artifacts/worker-issue-30-implementation-report.1b62ef0e.625f54b7.md`；oracle 审查报告 `.pi/artifacts/oracle-issue-30-plan-review.md`、`.pi/artifacts/oracle-issue-30-plan-review-round2.md` |
| 文档维护者 | docs-writer（未改源码/测试/配置；未提交、未推送、未创建 GitHub issue/PR） |

### 验证命令与实际结果

| 命令 | 执行者 | 结果 |
| --- | --- | --- |
| `mvn --batch-mode --no-transfer-progress verify`（仓库根） | 主 agent（终验） | **passed** — BUILD SUCCESS；`Tests run: 120, Failures: 0, Errors: 0, Skipped: 0`；`jacoco:check` → `All coverage checks have been met` |
| JaCoCo BUNDLE 行覆盖（`target/site/jacoco/jacoco.csv`） | 主 agent 终验 + docs-writer 按同一产物独立统计复核 | **947 covered / 110 missed / 1057 total = 0.8959**（闸门 ≥0.85；修复前 947/1058 = 0.8951；批准时基线 873/984 ≈ 0.8872） |
| `mvn --batch-mode -Dtest='PaymentControllerConcurrencyTest' test`（单独重跑） | 主 agent 终验 | **passed** — 3/3 稳定（`Tests run: 1, Failures: 0, Errors: 0`） |
| `npm test`（`frontend/`，即 `vitest run`） | 主 agent 终验；docs-writer 独立复跑 | **passed** — `Test Files 24 passed (24)` / `Tests 116 passed (116)` |
| `npm run lint`（`frontend/`，即 `oxlint src`） | 主 agent 终验；docs-writer 独立复跑 | **passed** — exit 0，无 warning |
| 后端 surefire 报告汇总（`target/surefire-reports/*.txt`，39 个测试类） | docs-writer 按既有产物统计复核 | `total=120 failures=0 errors=0 skipped=0`（与 `verify` 结果一致） |

> 说明：docs-writer **未**重跑后端全量 `mvn --batch-mode verify`（耗时长且不改源码）；上表后端数字引自主 agent 终验，并用仓库内同一次运行的产物（`jacoco.csv`、surefire 报告）独立核对一致。前端 `npm test`、`npm run lint` 由 docs-writer 实际复跑，结果与主 agent 报告一致。

---

## 1. 修改了哪些文件及潜在问题

### 1.1 后端契约与幂等（`src/main/java/com/fabflix/boot/commerce/`）

| 文件 | 变更与目的 |
| --- | --- |
| `CheckoutTokenService.java`（新增） | 集中管理三个 session 属性：`checkoutToken`（`UUID.randomUUID()` 不透明串，兼作 idempotency key）、`checkoutState`（`pending`/`completed`）、`checkoutOrder`（成功订单摘要）。方法：`issueToken`（签发并置 `pending`）、`matches`（校验缺失/空/不匹配）、`completedOrder`（**仅 `completed` 时**返回缓存订单，用于重放）、`completeOrder`（纯内存记账）、`recentOrder`（不论状态返回最近成功订单，供确认端点）。目的：把幂等语义收敛到一个可单测的小类（计划 O-2）。 |
| `CheckoutController.java`（新增） | 承载两个 GET：`/api/boot/checkout/token`（在 `synchronized (cartService.getOrCreateCart(session))` 内签发新 token 并置 `pending`，**不清 cart**）与 `/api/boot/confirmation`（`recentOrder` 有则 200 + 订单 JSON、无则 `204 No Content`）。目的：满足 D-3（token）与 D-6（会话内订单回读）。 |
| `PaymentController.java`（改） | 在**同一个** `synchronized (cart)` 临界区内实现 T1 六步时序：① token 校验（缺失/不匹配 → 200 `fail` + `"Your checkout session has expired. Please try again."`）→ ② `completedOrder` 重放（**先于空车判断**）→ ③ 空车判断（由锁外移入锁内）→ ④ 组装摘要（含 `cartService.getCart(session)` 的 **DB I/O**）→ ⑤ `placeOrder` → ⑥ 成功后 `completeOrder(...)` + `cart.clear()`（纯内存）。失败（`INVALID_PAYMENT`/`CUSTOMER_NOT_FOUND`）保持 `pending`、保留 token、不清 cart。目的：消除“假成功”并把隐式并发保证变成显式可测契约；“组装先于下单”消除“sales 已提交但记账失败 → 同 token 重试 → 重复下单”的窗口。 |
| `OrderSummary.java`、`OrderLine.java`（新增） | 订单摘要/订单行 DTO：`order_date`（String `YYYY-MM-DD`）、`total_amount`、`lines[]`（`movie_id`/`movie_title`/`quantity`/`unit_price`/`line_total`），snake_case 经 `@JsonProperty`；`BigDecimal` scale 2；两者 `implements Serializable`（`serialVersionUID = 1L`）以便写入 session。 |
| `PaymentRequest.java`（改） | 新增 `checkoutToken` 字段（由 `@ModelAttribute` 绑定表单参数）。 |
| `OperationResponse.java`（改） | 新增可空 `order` 字段与 `success(String, OrderSummary)`；`@JsonInclude(NON_NULL)` 保证失败响应不出现 `order`。**并删除无调用方的 `success(String)` 单参重载**（死代码，删除后该类 0 missed / 11 covered）。 |
| `CommerceApiExceptionHandler.java`（改） | `assignableTypes` 加入 `CheckoutController.class`（1 行），使新 controller 的 `IllegalArgumentException` 仍返回 400 `{"errorMessage":...}`。 |

**未改动（重要）**：`CartService`、`PaymentService`、`PaymentRepository`、`PaymentResult`、`CartController`、`CartItem`、`pom.xml`、`createtable.sql`、`stored-procedure.sql` —— 保证既有行为与 SQL（含 `INSERT_SALE_SQL` 的 `CURDATE()`）不变。

### 1.2 legacy（仅 1 个文件）

| 文件 | 变更与目的 |
| --- | --- |
| `src/main/webapp/payment.js`（改） | 页面加载时 `GET api/boot/checkout/token` **一次**并缓存在模块级 `checkoutToken`，提交时把它与四个表单字段一起 POST；token 为空时显示安全文案且不提交。目的：满足 D-7/B5 方案 a（服务端强制 token 后保持 legacy 可用）；保留 `api/boot/cart`、`api/boot/payment` 字符串，使 `FrontendApiMigrationTest` 的 `contains` 断言继续通过。**除该文件外，`src/main/webapp` 无任何改动。** |

### 1.3 前端页面、路由与金额工具（`frontend/src/`）

| 文件 / 文件组 | 变更与目的 |
| --- | --- |
| `features/cart/cartApi.js`（新增） | `fetchCart()`（`GET /boot/cart`）、`updateCartItem(movieId, action)`（`POST /boot/cart`，`URLSearchParams` body）。 |
| `features/cart/money.js`（新增，**清单外经批准**） | `toCents = Math.round(Number(amount) * 100)`、`formatCents`；三页共用，避免各自重复定义浮点/整数分换算。 |
| `features/cart/CartPage.jsx`（新增） | 表格（标题/数量/单价/行小计）+ 整数分总额 + `+`/`−`/Delete（POST 成功后重新 GET）；空态/加载/错误重试；**每次 mount 重新 GET（不缓存）**；"Proceed to payment" → `/checkout`。 |
| `features/cart/AddToCartButton.jsx`（新增） | 单一组件，被 `MovieTable` 与 `MovieDetailPage` 共用；POST `action=add`；内联 `role="status"`/`role="alert"` 反馈（不用 `alert`）；401 提示重新登录。 |
| `features/checkout/paymentApi.js`（新增） | `fetchCheckoutToken()`、`submitPayment(formValues, checkoutToken)`、`fetchConfirmation()`（204 → `null`）。 |
| `features/checkout/PaymentPage.jsx`（新增） | 顺序加载：先 `GET cart`，**空车不发 token 请求**；非空才 `GET token`；`tokenStatus !== 'success'` 时提交按钮 `disabled`；4 字段 `required` + expiration `type="date"`；提交中禁用防连点；失败 `role="alert"` 展示服务端 message 且不回显卡号；失败/过期时提供 "Start checkout again" → `retry()`（重取 cart+token）；成功 `navigate('/confirmation', {replace:true, state:{order}})`。 |
| `features/checkout/ConfirmationPage.jsx`（新增） | 优先 `location.state?.order`（免一次请求），否则 `GET /boot/confirmation`；204 → 安全空态（`EmptyState`，不显示假订单）；展示日期、行、总额。 |
| `app/routeDefinitions.js`（改） | 删除 cart/checkout/confirmation 三条占位定义（`customerProtectedRoutes` 现为 `[]`）；导出与聚合 (`customerRoutes`/`routeDefinitions`) 保持不变，`it.each(routeDefinitions)` 仍由 employee 两条覆盖。 |
| `app/AppRoutes.jsx`（改） | 在 `ProtectedRoute audience="customer"` 下新增 `cart`/`checkout`/`confirmation` 三条显式路由，替代对这三条的占位渲染。 |
| `features/movies/MovieTable.jsx`、`MovieDetailPage.jsx`（改） | 新增 Actions 列 / 详情页嵌入同一个 `AddToCartButton`（D-1）。 |
| `index.css`（改） | 追加 cart 表操作列、总额、错误文案、加购内联状态、checkout 表单的最小样式；并修复两处副作用：`.cart-table__actions` 不再在 `<td>` 上用 `display:flex`（改内层 `span` + nowrap）、删除 `.movie-table td:last-child` 的误指向（改按显式类名 `movie-table__rating` / `movie-table__actions`）。 |
| **未改动** | `api/apiClient.js`、`features/auth/**`、`vite.config.js`、`package.json`（未新增依赖）。 |

### 1.4 测试

| 文件 / 文件组 | 变更与目的 |
| --- | --- |
| `PaymentControllerTest.java`（改） | 成功含订单数据并清空 cart；**同 token 重放**同一订单（`verify(placeOrder, times(1))`）且重放先于空车；token 缺失/不匹配 → 200 fail + expired 文案且不调 `placeOrder`；新 token + 空车短路；失败保留 cart 与 `pending`；失败响应不含卡号；失败后改卡重试可成功；token 重签后二次下单、旧 token 重放（补计划 §4.2 强制用例）。跨请求用例统一 `.session(sharedMockHttpSession)`。 |
| `CheckoutControllerTest.java`（新增） | token 签发 200 + 写入 session 且 `state=pending`；`/confirmation` 有订单 200（即使 `pending`）/ 无订单 204；匿名 401（经 `CustomerAuthenticationFilter`）；`IllegalArgumentException` → 400 advice（验证 B4 接入）。 |
| `CheckoutTokenServiceTest.java`（新增） | 签发非空 + 置 `pending`；校验失败（缺失/空/不匹配）；成功后置 `completed` 并缓存；`completed` 重放返回同一实例；未完成时无订单；重签回到 `pending`。 |
| `PaymentControllerConcurrencyTest.java`（新增） | 确定性 2 线程**直接调用** `PaymentController.pay`（不经 MockMvc），共享同一 `MockHttpSession`/cart 实例；用 `CountDownLatch` 挂住第一个 `placeOrder`，**有界轮询等待第二线程进入 `Thread.State.BLOCKED`（cart monitor）** 后放行；断言 `placeOrder` 恰一次、两响应对同一 order 实例、cart 清空一次。 |
| `CartServiceTest.java`（改） | 新增 AC-2 后端半边（真实 `CartService` + `MockHttpSession` + stub `CartRepository`：`updateCart` → `getCart` 反映数量变化/删除）。 |
| `frontend/src/features/cart/{CartPage,AddToCartButton,money}.test.*`（新增） | 行/总额（整数分）、POST URL/body 与随后重新 GET、空态、错误重试、每次 mount 重新 GET；加购成功/失败/401 文案；整数分换算与格式化边界。 |
| `frontend/src/features/checkout/{PaymentPage,ConfirmationPage,checkoutFlow}.test.jsx`（新增） | token 未取得前禁用且不发 POST、token 失败不发 POST、空车不发 token、重复提交只发一次、失败安全展示无卡号回显、成功导航；确认页 state/204/错误重试；**跨页流程**（真实三页 + `MemoryRouter`，请求序列 `cart→cart→token→payment`）。 |
| `App.test.jsx`、`features/movies/{MovieListPage,MovieDetailPage}.test.jsx`（改） | 三路由真实页面断言 + 三条路由匿名重定向参数化断言；`responseBody(url)` 改为按 URL 分派（cart/token/confirmation）；movie 页面 fetch mock 按 URL 分派以支持加购请求。 |

### 1.5 遗留风险、边界限制、未验证项与后续事项

**残余风险（已在计划 §10/§13.1 与本日志如实记录，非“已解决”）**

1. **无真实浏览器 / 真实 MySQL 端到端验证**：无 Playwright/Cypress/Selenium 设施（引入需单独批准）。前线覆盖为后端 MockMvc + 前端页面级 `fetch` mock。
2. **幂等保证的适用边界**：token 只对**单 JVM、单 session**有效（架构文档 AD-2）；跨实例或同顾客跨设备（不同 session）并发下单未被覆盖，也无 DB 唯一约束（无 schema 变更授权）。
3. **CSRF 保持现状**：`pom.xml` 无 Spring Security、无 CSRF token；cookie 认证的写操作继承该安全残余风险（D-4 明确排除）。
4. **订单行一致性边界（R-15）**：`CartService.getCart` 静默丢弃 movies 表中不存在的 movie id，而 `placeOrder` 用原始 `Map.copyOf(cart)` 插入 → 极端情况下确认页展示行与实际插入的 sales 行可能不一致。本 issue 未修改（保持既有行为）。
5. **日期边界（R-6）**：`order_date` 用 JVM `LocalDate.now()`，`sales.sale_date` 用 SQL `CURDATE()`；午夜/时区差可能相差一天。已裁定接受。
6. **覆盖率死角**：`PaymentController` 的 `switch default: throw`（`PaymentResult` 仅三值）1 行未覆盖，属既有模式。
7. **`completed` 状态下旧 token 会重放旧订单**（R-16）：用户下单后又加购但页面仍持旧 token 时返回旧订单；已通过 `/checkout` 每次进入都重取 token + “取 token 前禁止提交” + "Start checkout again" 恢复入口缓解，但该语义本身是设计选择。

**未验证项（明确未做）**

- 真实浏览器刷新后 cart 保持、真实 `JSESSIONID` 轮换、双标签页并发下单：**无自动化覆盖**（U-8/§7.4 声明的未覆盖项）。
- CSRF、多实例部署下的幂等：**未验证**（超出批准范围）。
- 本日志作者未重跑后端全量 `mvn verify`（引用主 agent 终验 + 同次运行的产物复核）。

**后续事项**

- 提交、推送与 PR 创建：**待主 agent 授权**；PR 链接与远端 issue 勾选状态由主 agent 回传后更新 issue 文档与本日志元数据。
- 如未来要求“展示行 = 实际插入 sales 行”，需另立 issue 修改 `CartService.getCart` 的丢弃语义（R-15）。
- 如需严格日期一致性，可在插入后读回 `CURDATE()`（R-6）。
- legacy 清理（`shoppingCart.js`/`payment.js`/`confirmation.html` 等）必须与 `FrontendApiMigrationTest` 一并修改，且需单独批准。

**没有发现其他已知遗留问题。** 具体地：`src/main/webapp` 仅 `payment.js` 被改且相关钉死测试仍绿；`pom.xml` / SQL / `package.json` / `apiClient.js` / `features/auth/**` 未被改动；工作区的 `frontend/package-lock.json` 既有修改与未跟踪的 `.idea/`、`.pi/`、`AGENTS.md` 属进入本任务前已存在，**不属本次变更**；未发现源码与批准计划之间未获批准的偏离（全部 8 项差异见计划 §13.1，均由主 agent 批准）。

---

## 2. 新增知识点 / 重点学习对象

### 2.1 分布式/接口设计概念：幂等键与重放语义（T1）

- **含义**：服务端签发一次性 `checkoutToken` 兼作 **idempotency key**；成功后同一 token 再次提交**重放同一成功响应**（返回缓存的 `order`），而不是报错或再次下单；失败则保持 `pending` 允许改卡重试；新 token 会把状态重置为 `pending`。这样“响应丢失后客户端重试”不会被误报为失败。
- **对应位置**：`src/main/java/com/fabflix/boot/commerce/CheckoutTokenService.java`（`issueToken`/`matches`/`completedOrder`/`completeOrder`/`recentOrder`）、`PaymentController.pay`（临界区内六步顺序）、`src/test/java/.../PaymentControllerTest.java`（重放断言）、计划 §4.2 的时序表。
- **为何值得学习**：这是把“重复提交防护”从 UI 禁用按钮升级为**服务端可断言契约**的典型做法；也揭示了关键设计点——**重放判断必须先于空车判断**（否则成功后 cart 已清空，重放会被空车短路掉），以及“副作用（DB 写）之前完成所有可能失败的前置工作”这一消除重复下单窗口的通用模式。

### 2.2 Java / Spring：`BigDecimal` 的 scale 与 `RoundingMode`

- **含义**：金额统一用 `BigDecimal`，并显式设定 scale 2（`BigDecimal.ZERO.setScale(2)`、`setScale(2, RoundingMode.HALF_UP)`）；`CartItem.price` 等序列化为 JSON number（`7.50` → `7.5`），因此“小数位数”不能靠 JSON 表现，必须在服务端定好精度与舍入。
- **对应位置**：`PaymentController.summarize(...)`、`OrderSummary`/`OrderLine`、`CartItem.java`。
- **为何值得学习**：`BigDecimal.equals` 会比较 scale（`7.5 != 7.50`），而 JSON 序列化会丢掉末尾 0；同时 `multiply` 后必须显式 `setScale(..., RoundingMode)` 才能得到确定的两位小数。前端因此被约束为**整数分求和**（见 2.6），服务端保持金额权威。

### 2.3 Jakarta/Java 会话与序列化：session 存储对象必须 `Serializable`

- **含义**：写入 `HttpSession` 的对象要实现 `Serializable`（并建议 `serialVersionUID = 1L`），否则内嵌 Tomcat 的 `StandardManager` 在应用关闭/重启持久化 session（`SESSIONS.ser`）时会抛 `NotSerializableException` 告警并导致该 session 丢失。
- **对应位置**：`OrderSummary.java`、`OrderLine.java`（本 issue 新增）；既有参照 `AuthenticatedCustomer.java`、`EmployeeIdentity.java`；架构文档 §9（C-16/R2-N1）。
- **为何值得学习**：这是“看起来能跑但不健壮”的典型陷阱——session 属性只在容器关闭/重启路径才会暴露问题；把 DTO 放进 session 缓存（`checkoutOrder`）时必须一并遵守项目既有约定。

### 2.4 Spring 测试：`MockHttpSession` 跨请求共享与 `.sessionAttr(...)` 陷阱

- **含义**：`MockHttpServletRequestBuilder.sessionAttr(name, value)` 会把属性写到**该请求自己新建的** `MockHttpSession` 上，因此每次 `mockMvc.perform(...)` 都是一个新 session；要做跨请求序列断言（签发 token → 提交 → 重放），必须自己构造一个 `MockHttpSession` 并对每个请求 `.session(shared)`。
- **对应位置**：`src/test/java/com/fabflix/boot/commerce/PaymentControllerTest.java`（`sessionWithCart(...)` + `.session(session)`）、`CheckoutControllerTest.java`、`CheckoutTokenServiceTest.java`；架构文档 §7；计划 C-17/R-17。
- **为何值得学习**：这是**“测试伪绿”**的隐蔽来源——用 `.sessionAttr` 写跨请求用例时，第二次请求会因 token 不匹配而失败（或断言失去意义）；同理 `@MockBean CartService` 未 stub `getCart` 时 Mockito 默认返回空 List，会让 `order.lines` 断言“空通过”。真正的修法是断言前显式 stub、并共享 session 实例。

### 2.5 JUnit / 并发：用 `Thread.State.BLOCKED` 有界轮询做确定性并发测试

- **含义**：不要用 `Thread.sleep(n)` 赌两个线程会重叠。做法是：在第一个线程持有 `synchronized (cart)`（其 `placeOrder` 内部用 `CountDownLatch` 挂住）时启动第二个线程，然后**有界轮询第二个线程的 `getState() == Thread.State.BLOCKED`**（唯一 monitor 就是 session cart），确认它确实阻塞在 cart 监视器上后再放行第一个线程；超时则抛 `AssertionError` 并打印状态。断言仍是 `verify(paymentService, times(1)).placeOrder(...)`。
- **对应位置**：`src/test/java/com/fabflix/boot/commerce/PaymentControllerConcurrencyTest.java`（`awaitBlockedOnCartMonitor(...)`，第 98/124/127 行附近）。
- **为何值得学习**：`BLOCKED` 是 JVM 层面“等待进入 monitor”的可观测状态，用它替代时间猜测能消除 flaky；同时这也是“**断言可观测行为而非内部实现**”与“不得为让测试通过而弱化断言”的实例。

### 2.6 前端/JS：整数分金额运算（避免浮点漂移）

- **含义**：服务端下发的 JSON number 直接累加会出现 `0.1 + 0.2` 类浮点误差；做法是先把每个金额 `Math.round(Number(amount) * 100)` 转成整数分再求和，最后 `(cents / 100).toFixed(2)` 格式化展示；服务端始终是金额唯一权威来源。
- **对应位置**：`frontend/src/features/cart/money.js`（`toCents`/`formatCents`）、`CartPage.jsx`/`PaymentPage.jsx`/`ConfirmationPage.jsx`（统一 import）、`money.test.js`；legacy `shoppingCart.js` 的浮点累加是**反例**。
- **为何值得学习**：这是金额计算的通用铁律；把它抽成共享模块（而不是每页各写一份）让行为一致且可单测，也顺带删除了三份重复实现。

### 2.7 React / React 19：StrictMode 双执行 effect 与 token 轮换

- **含义**：入口 `frontend/src/main.jsx` 使用 `<StrictMode>`，开发模式下 effect 会“挂载 → 清理 → 再挂载”，**一次性副作用会发两次请求**。对于“每次 GET 都会轮换服务端 token”的端点，这会让服务端与页面持有的 token 可能不一致（旧 token 在 `completed` 下重放旧订单、在 `pending` 下被拒为 expired）。
- **对应位置**：`frontend/src/main.jsx`；`frontend/src/features/checkout/PaymentPage.jsx`（`active` 标志防卸载后置状态；先 GET cart、非空才取 token；token 未取得前提交 `disabled`；失败/过期用 "Start checkout again" → `retry()` 重取 cart+token）；架构文档 §4 的 StrictMode 条款。
- **为何值得学习**：StrictMode 双执行是 React 18/19 的常见踩坑点；正确姿势不是“用 ref 屏蔽第二次”，而是让副作用**幂等**或提供**显式恢复入口**，并区分“可重复的读”与“有副作用的写/轮换”。

### 2.8 HTTP/响应契约：204 语义与 `@JsonInclude(NON_NULL)`

- **含义**：无内容的成功响应用 `204 No Content`（`ResponseEntity.noContent()`），前端 `apiClient` 把 204 解析为 `null`，因此确认页用“`null` → 安全空态”而不是假订单；`@JsonInclude(NON_NULL)` 让可空字段（`OperationResponse.order`）在失败响应中**整个消失**，从而做到“只增字段、不破坏既有失败响应形状”。
- **对应位置**：`CheckoutController.confirmation(...)`、`OperationResponse.java`、`frontend/src/api/apiClient.js`、`ConfirmationPage.jsx`；架构文档 §5/§6。
- **为何值得学习**：区分“空结果”与“错误”是 API 设计常见问题；前端按 `status` 字段而非 HTTP 码区分业务失败的约定（架构文档 §6）也在此体现。

### 2.9 构建/CI：JaCoCo 85% 行覆盖率闸门与“新增类必须配测试”

- **含义**：CI 唯一构建步骤是 `mvn --batch-mode --no-transfer-progress verify`，其中 JaCoCo 强制 `BUNDLE / LINE / COVEREDRATIO ≥ 0.85`；未覆盖余量有限（批准时约 43 行），因此**每个新增/修改的后端类都必须有测试触及行与分支**（DTO getter 用一次 JSON 序列化断言即可覆盖）。前端 `npm test`/`npm run lint` **不在 CI**，属手动验证门。
- **对应位置**：`.github/workflows/ci.yml`、`pom.xml`（JaCoCo 插件）、`target/site/jacoco/jacoco.csv`；计划 C-13/C-14；架构文档 §7。
- **为何值得学习**：本地 `mvn test` 是绿的、CI 却红，常因为闸门只在 `verify` 阶段生效；同时理解“覆盖率闸门把新增死代码也变成负担”（本 issue 删除无调用方的 `success(String)` 正是把 1 missed 归零）。

### 2.10 Java 语法/工具：`@RestControllerAdvice(assignableTypes = ...)` 的作用域

- **含义**：异常处理器默认只覆盖其扫描到的 controller；新建 `CheckoutController` 若不加入 `assignableTypes`，其抛出的 `IllegalArgumentException` 就不会被转成 400 `{"errorMessage":...}`，契约与既有端点不一致。
- **对应位置**：`CommerceApiExceptionHandler.java`、`CheckoutControllerTest.mapsInvalidCartRequestsToBadRequest()`。
- **为何值得学习**：这是“新增 controller 时容易漏掉的横切配置”，也正是 oracle 审查发现的 B4 项。

### 2.11 前端表单编码：`URLSearchParams` 与 `@ModelAttribute` 的对应

- **含义**：`apiClient` 不自动设置 `Content-Type`；表单 POST 用 `URLSearchParams` body（`application/x-www-form-urlencoded`），后端用 `@ModelAttribute`/`@RequestParam` 绑定。
- **对应位置**：`frontend/src/features/checkout/paymentApi.js`（`submitPayment`，含 `checkoutToken`）、`features/cart/cartApi.js`、`CartController`/`PaymentController`、`PaymentRequest.java`。
- **为何值得学习**：显式理解“谁负责设置 Content-Type”，避免 JSON 序列化 body 打到表单绑定端点导致 400/参数为空。

**本次没有新增知识点的情况：不适用——以上各条均为本次变更实际引入或显著涉及的内容。**

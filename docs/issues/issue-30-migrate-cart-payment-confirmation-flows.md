# Issue #30 — Migrate cart, payment, and confirmation flows（Issue 跟踪文档）

> **状态：已在 GitHub 存在，无需新建。** 本文档记录远端 issue 的编号/URL/原文与本地批准范围；本文档不是"待发布草稿"，
> `github-operator` **不需**为本 issue 创建新 issue。docs-writer 不修改远端 issue 内容。
>
> **本地实现状态（2026-10-14）**：分支 `codex/issue-30-cart-payment-confirmation` 上实现已完成并通过**主 agent 终验**（`mvn --batch-mode --no-transfer-progress verify` → 120 tests / JaCoCo BUNDLE 行覆盖 947/1057 = 0.8959；`npm test` 24 files / 116 tests；`npm run lint` exit 0）；**已提交（`d6b3e07` 实现提交、`eeb935f` 文档提交）并推送，已创建 PR [#40](https://github.com/PNZ-z/ai-powered-Fabflix/pull/40)（OPEN，尚未合并）**。AC-1..AC-5 状态见 §6，实测证据见 §7 与计划 §13.1。**审查后后续修复（UI 与缓存，提交 `6b637a4`，前端 `npm test` 25 files / 142 tests）已提交、待主 agent 推送**，见 §7 与 §8。

## 1. 远端元数据

| 项目 | 值 |
| --- | --- |
| 编号 | 30 |
| URL | https://github.com/PNZ-z/ai-powered-Fabflix/issues/30 |
| 标题（远端原文） | Migrate cart, payment, and confirmation flows |
| Milestone | Phase 2 — React Frontend Migration (#2) |
| 获取方式 | 主 agent 用 `gh` 获取的远端原文（`.pi` 会话输入 1） |
| 本地记录文档 | 本文件 |
| 关联计划 | `docs/plans/issue-30-migrate-cart-payment-confirmation-flows.md` |
| 远端状态核对时间 | 2026-10-14（本地文档时间；远端状态未由 docs-writer 直接查询 → **未确认**） |

> docs-writer 无 GitHub 访问授权，未直接读取远端。以上元数据来自主 agent 提供的远端原文；如与远端当前状态不符，以远端为准并由主 agent 更正。

## 2. 远端 issue 正文原文（逐字保留，供对照）

```markdown
## Goal
Rebuild the complete customer checkout journey in React without changing business behavior.

## Scope
- Build cart viewing, quantity update, removal, and empty-cart states
- Migrate payment form validation and submission
- Migrate order confirmation and sale-line display
- Preserve server-side session cart behavior
- Prevent accidental duplicate submissions

## Acceptance criteria
- [ ] Cart quantities and totals update correctly
- [ ] Cart contents persist across navigation and refresh
- [ ] Valid payment completes and displays confirmation data
- [ ] Invalid card/customer input displays a safe error
- [ ] Critical cart and checkout paths have integration tests

## Dependency
Depends on authentication, shared API handling, and movie detail/list flows.
```

## 3. 用户已批准的范围决定（主 agent 传达，视为批准范围内）

| 编号 | 已批准决定 | 对应 issue 正文 |
| --- | --- | --- |
| D-1 | **Add to cart 入口纳入 #30**：#28/#29 迁移的 React movie list 与 movie detail 页需新增 "Add to cart" 操作，否则 React 购物车无法被填充、验收条件无法端到端成立。 | 隐含于 scope 第 1 条 "Build cart viewing…"；issue 未显式提及加购入口 |
| D-2 | **Confirmation 展示服务端订单数据**（issue 正文 "sale-line display" 按此实现）：支付成功后后端返回订单数据（订单行、金额、日期等），前端确认页展示。**允许扩展支付响应契约。** | scope 第 3 条 "Migrate order confirmation and sale-line display"；AC 第 3 条 |
| D-3 | **完整幂等/防重复提交方案**：需要 submit token / idempotency key 之类的新 API 契约；**仅前端禁用按钮不够。** | scope 第 5 条 "Prevent accidental duplicate submissions" |
| D-4 | **CSRF 保持现状**：明确列为 out-of-scope，并在风险中记录为残余风险（当前无 Spring Security、无 CSRF token）。 | issue 未提及 CSRF |

> **2026-10-14 追加（第一轮 oracle 审查后主 agent 裁定）**：本 issue 新增 D-5..D-10——D-5 幂等语义采用 T1（token 兼作 idempotency key，成功后重放同一成功响应）；D-6 确认页刷新改用 session-backed `GET /api/boot/confirmation`（200/204），不再用 sessionStorage；D-7 允许修改 legacy `payment.js` **单个文件**；D-8 支付响应契约细节（扩展 `OperationResponse` + snake_case 字段 + 不加 `code`）；D-9 前端表单 parity（`required` + `type="date"`，不加格式校验）与加购内联反馈；D-10 JWT 路线 C（网页端继续 session 认证）。完整内容与逐条裁定见计划文档 §1.3 与 §12。

> **2026-10-14 追加（第二轮 oracle 复审后主 agent 裁定）**：计划终稿已按 R2-B1/R2-B2 + R2-N1..N5 + O-1..O-5 修订并被主 agent **批准**。对本 issue 范围有影响的两点：① R2-N2 固定 `GET /api/boot/confirmation` 语义（存在“最近一次成功订单”即 200，**即使 `checkoutState==pending`**；无缓存订单 204）；② R2-B1 固定下单顺序为“先组装订单摘要（含 DB I/O）→ 再 `placeOrder` → 成功后纯内存记账”（消除重复下单窗口）。**D-1..D-10 的范围未变**；详见计划文档 §4.2/§4.3/§12.1/§13。

## 4. 范围（依据 issue + 已批准决定）

### 4.1 In scope

- 购物车：查看、数量增减、删除行、空 cart 状态、总额。
- 支付：表单（`firstName`/`lastName`/`creditCardNumber`/`expirationDate`）校验与提交、错误展示。
- 确认：订单/销售行展示（按 D-2，数据来自服务端）。
- 保持服务端 session cart 行为。
- 防意外重复提交（按 D-3，服务端契约级防护 + 前端提交中态）。
- React movie list / movie detail 加购入口（D-1）。
- 关键 cart/checkout 路径的集成测试（定义见计划 §7.4）。

### 4.2 非目标（Non-goals）

- CSRF / Spring Security / JWT 引入（D-4、D-10 路线 C）。
- 删除或改动 legacy `src/main/webapp` 结账页面（被 `FrontendApiMigrationTest` 钉死；本 issue 保留）。**已批准例外：允许修改 `src/main/webapp/payment.js` 一个文件**（携带 checkout token，D-7/B5 方案 a，见计划 §5.4）；其余 legacy 文件仍不得删改。
- `orders` 表 / DB schema 变更 / sales 表加列 / DB 唯一约束。
- 多实例（多 JVM）部署下的分布式幂等。
- 浏览器 e2e 测试设施（无此设施；§7.4 为替代定义）。
- 真实定价/税/金额体系、i18n。
- employee/admin（#31）相关改动。

## 5. 依赖

| 依赖（issue 正文：authentication、shared API handling、movie detail/list flows） | 仓库现状 | 证据 |
| --- | --- | --- |
| authentication | 已完成（#35/#36） | `CustomerAuthController`、`CustomerAuthenticationFilter`、`frontend/src/auth/ProtectedRoute.jsx`、`AuthProvider.jsx` |
| shared API handling | 已完成 | `frontend/src/api/apiClient.js`（`apiRequest`、`ApiError`、`credentials: 'include'`） |
| movie detail / list flows | 已完成（#38/#39） | `frontend/src/features/movies/{MovieListPage,MovieDetailPage,MovieTable}.jsx` |

本 issue 自身新增的依赖：checkout token 端点（D-3）、`GET /api/boot/confirmation`（D-6）与支付响应订单数据扩展（D-2/D-8），均由本 issue 实现。

## 6. 验收条件与跟踪

| # | 验收条件 | 承载方案 | 状态 |
| --- | --- | --- | --- |
| AC-1 | Cart quantities and totals update correctly | `CartPage` + `cartApi` | **已完成**。证据：`frontend/src/features/cart/CartPage.test.jsx`（行/数量/单价/行小计与**整数分总额**；+ / − / Delete 的 POST URL/body 及随后重新 GET）——`npm test` 24 files / 116 tests passed；后端 200 `status`/`message` 契约不变（`CartControllerTest` 回归护栏在 `mvn --batch-mode --no-transfer-progress verify` 中全绿，共 120 tests） |
| AC-2 | Cart contents persist across navigation and refresh | 服务端 session cart 复用（后端半边在 `CartServiceTest`，R2-B2） | **已完成（按批准的近似验证定义）**。证据：`CartServiceTest`（真实 `CartService` + `MockHttpSession` + stub `CartRepository`，`updateCart`→`getCart` 反映数量变化/删除）；`CartPage.test.jsx` 断言每次 mount 重新 `GET /boot/cart`（不缓存）。**真实浏览器刷新无自动化覆盖**（无 e2e 设施，见 §4.2 非目标、计划 §7.4/§10 与工作流日志的残余风险） |
| AC-3 | Valid payment completes and displays confirmation data | 支付响应携带订单数据（D-2/D-8）+ session-backed `GET /api/boot/confirmation`（D-6，R2-N2 语义）+ `ConfirmationPage` | **已完成**。证据：后端 `PaymentControllerTest`（成功响应含 `order.*` 全字段；同 token 重放返回同一订单且 `verify(placeOrder, times(1))`）、`CheckoutControllerTest`（`/confirmation` 有订单 200 / 无订单 204）；前端 `PaymentPage.test.jsx`（成功导航携 state）、`ConfirmationPage.test.jsx`（渲染行/总额/日期、204 安全空态）、`checkoutFlow.test.jsx`（cart→checkout→confirmation 跨页流程，请求序列 `cart→cart→token→payment`） |
| AC-4 | Invalid card/customer input displays a safe error | 安全展示服务端 fail message；保留 `required` + `type="date"` parity，不加格式校验（U-4 已裁定） | **已完成**。证据：`PaymentControllerTest`（`INVALID_PAYMENT`/`CUSTOMER_NOT_FOUND` → 200 fail、cart 与 `pending` 保留、失败响应**不含卡号**）；`PaymentPage.test.jsx`（4 字段 `required` + expiration `type="date"`、`role="alert"` 展示服务端 message、无卡号回显、token 未取得前不发 POST） |
| AC-5 | Critical cart and checkout paths have integration tests | 计划 §7.4 的 integration 定义（U-8 已裁定接受） | **已完成**。证据：① 经 `CustomerAuthenticationFilter` 的 401（`CheckoutControllerTest.rejectsAnonymousRequests`）；② token 缺失/不匹配与重放（`PaymentControllerTest`，含「重放先于空车」）；③ 前端跨页流程（`checkoutFlow.test.jsx`）。闸门：`mvn --batch-mode --no-transfer-progress verify` → `Tests run: 120, Failures: 0` + `All coverage checks have been met`（947/1057 = 0.8959）。**未覆盖**：真实浏览器 + 真实 MySQL + 真实 session cookie 的 e2e（无设施，引入需单独批准） |

## 7. 关联记录

| 类型 | 位置 | 说明 |
| --- | --- | --- |
| 计划 / 方案 | `docs/plans/issue-30-migrate-cart-payment-confirmation-flows.md` | **状态：实施完成，已通过主 agent 终验（2026-10-14）；已提交并推送（`d6b3e07`、`eeb935f`），PR #40 已创建（OPEN，尚未合并）**；§13.1 为实际文件清单、与计划的差异、实测数字与残余风险；§12.1 末尾为审查与修复结果小结 |
| Oracle 第一轮审查报告 | `.pi/artifacts/oracle-issue-30-plan-review.md` | B1–B6 / N1–N14 / U-1..U-9；主 agent 裁定与写入位置见计划 §12/§11 |
| Oracle 第二轮复审报告 | `.pi/artifacts/oracle-issue-30-plan-review-round2.md` | R2-B1 / R2-B2 / R2-N1..N5 / O-1..O-5；主 agent 全部接受，裁定与写入位置见计划 **§12.1**；**第一轮+第二轮全部闭合**（O-3 实测结果已于 2026-10-14 回填） |
| 架构约束 | `docs/architecture/frontend-migration-contracts.md` | 跨 issue 的前端迁移约定与钉死约束；已补 CI 覆盖率闸门、`payment.js` 例外、`responseBody` 分派规则、AD-1/AD-2 决策（§7/§10）、session 存储对象需 `Serializable`（§9）、`@WebMvcTest` 跨请求 session 陷阱（§7），以及本次从 #30 实现提取的金额整数分工具与 React StrictMode effect 约定（§4）；并补入 PR #40 审查后修复提取的「列表类 GET 的进程内缓存（TTL + SWR + in-flight 去重）」约定（§4） |
| 工作流日志 | `docs/logs/2026-10-14-issue-30-migrate-cart-payment-confirmation-flows.md` | 已创建：实际变更逐项说明、遗留风险、终验命令与覆盖率数字、新增知识点；**§3 为 PR #40 审查后的后续修复**（UI 布局 / 排序控件 / movie list 缓存）的文件、验证与残余边界，含 §3.6 新增知识点 |
| 实现报告（内部证据） | `.pi/artifacts/worker-issue-30-implementation-report.1b62ef0e.md`、`.pi/artifacts/worker-issue-30-implementation-report.1b62ef0e.625f54b7.md` | worker 初版与修复报告（**内部产物，未纳入版本控制的交付内容**；本日志引用其测试命令与结果） |
| PR | **#40** — https://github.com/PNZ-z/ai-powered-Fabflix/pull/40 | **已创建**（由 `github-operator` 创建，主 agent 回传）。标题 “Migrate cart, payment, and confirmation flows to React”；base `main` ← head `codex/issue-30-cart-payment-confirmation`；状态 **OPEN、非 draft、mergeable**；GitHub 已识别 `Closes #30`。**尚未合并**——合并前以 PR 的 CI 结果与 review 为准。docs-writer 无 GitHub 访问授权，本行来自主 agent 回传，未由 docs-writer 直接查询远端。**PR #40 审查后的后续修复（UI 布局 + 排序控件 + movie list 前端缓存，提交 `6b637a4`）已提交、待主 agent 推送**（详见工作流日志 §3 与计划 §13.1）；该修复**未改变**已批准契约/范围/安全边界 |

## 8. 备注

- 本 issue 远端正文**没有**提到 CSRF、加购入口、订单数据来源或幂等实现方式；后三者由主 agent 与用户确认后写入 §3，属对 issue 编码范围的澄清，非 issue 原文。
- 本文件不发布到 GitHub；远端 issue 内容不由 docs-writer 修改。
- **2026-10-14**：计划已按主 agent 对 oracle 第一轮审查（B1–B6/N1–N14/U-1..U-9）的裁定修订完毕，**当时仍未批准**（需 oracle 第二轮复审与主 agent 明确批准后才能进入实现）。**测试状态：本 issue 尚未开始实现，未运行任何测试。**
- **2026-10-14（第二轮）**：oracle 第二轮复审（R2-B1/R2-B2 两项 Blocker + R2-N1..N5 + O-1..O-5）已由主 agent **全部接受**并写入计划终稿；计划文档 §13 状态为 **已批准（主 agent，2026-10-14）— 可交 worker 实现**，§12.1 记录逐条裁定与写入位置。**本 issue 的 AC 状态因此更新为“计划已批准，待实现”。测试状态：仍未开始实现，未运行任何测试**（计划中的覆盖率数字 873/984 来自仓库内既有 `verify` 产物，不是本轮新跑）。**（本条为当时的记录；后续已实现并通过终验，见下条与 §6/§7。）**
- docs-writer 无 GitHub 访问授权，未直接读取远端 issue；§1 元数据来自主 agent 用 `gh` 获取的原文（已由主 agent 核实）。
- **2026-10-14（实现完成）**：实现已完成并通过主 agent 终验，**提交（`d6b3e07` 实现提交、`eeb935f` 文档提交）并推送至 `codex/issue-30-cart-payment-confirmation`，并已创建 PR #40（OPEN，尚未合并）**。AC-1..AC-5 更新为“已完成”并附验证证据（§6）；实际命令与结果：`mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS、`Tests run: 120, Failures: 0, Errors: 0, Skipped: 0`、JaCoCo BUNDLE 行覆盖 947/110/1057 = 0.8959、`All coverage checks have been met`；`npm test` → 24 files / 116 tests passed；`npm run lint` → exit 0，无 warning；`PaymentControllerConcurrencyTest` 单独重跑 3/3 稳定。**实测数字由主 agent 亲自运行并回传**；docs-writer 未重跑后端全量 `verify`（本地 `target/site/jacoco/jacoco.csv` 与 surefire 报告产物与上述数字一致，前端 `npm test`/`npm run lint` 由 docs-writer 独立复跑确认一致）。
- **提交 / 推送 / PR 创建：已完成（2026-10-14，主 agent 与 `github-operator` 执行）**——实现提交 `d6b3e07`、文档提交 `eeb935f` 已推送；PR **#40** 已创建（见 §7）。**仍未合并**：合并前以 PR 的 CI 结果与 review 为准。远端 issue 的勾选状态由主 agent 回传后更新本文档（docs-writer 不修改远端 issue）。
- **2026-10-14（PR #40 审查后的后续修复）**：PR #40 审查通过**之后**用户提出 3 组后续修复——① 表格宽度回归修复；② 排序控件重做（radio pills + 两行 `Sort by`/`then`）；③ movie list 前端缓存（方案 A）。三者落在提交 **`6b637a4`**（`feat: improve movie list layout, sorting controls, and caching`），并入同一分支 `codex/issue-30-cart-payment-confirmation`。**状态：已提交、待主 agent 推送**（`git status -sb` 显示分支相对 `origin` **ahead 1**；HEAD = `6b637a452a0b03229032af29ddb9b91999e9c24d`）——**未推送、未合并**，推送/PR 更新后回填。实测：`npm test` → **25 files / 142 tests passed**（修复前 24 files / 116 tests）、`npm run lint` → exit 0、`npm run build` → 成功；后端零改动（`6b637a4` 仅含前端文件），**未重跑** `mvn --batch-mode verify`（§6/§7 中 120 tests / 947 of 1057 = 0.8959 的后端数字仍为最近一次全量验证结果，未被本提交影响）。**AC-1..AC-5 状态不变**（仍为“已完成”，证据见 §6）；契约、范围与安全边界未变（无新依赖、`apiClient.js` 未改、无 schema/legacy 改动）。逐项变更、验证与残余边界见计划文档 §13.1 的「审查后后续修复」小节与工作流日志 §3。

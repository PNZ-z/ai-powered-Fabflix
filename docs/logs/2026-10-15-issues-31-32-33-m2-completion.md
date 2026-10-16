# 工作流日志 — Issue #31 / #32 / #33（M2 收尾：员工迁移、响应式与 a11y、测试/CI、生产构建与 legacy 退役）

> 一次工作流覆盖三个 issue（`#31 → #32 → #33`），单分支、单 PR 计划。本日志按 AGENTS.md 的要求分两大节：
> **一、修改了哪些文件及潜在问题**；**二、新增知识点 / 重点学习对象**。元数据如下（不替代这两节）。

## 元数据

| 项目 | 值 |
| --- | --- |
| 完成日期 | 2026-10-15（主 agent 终验） |
| 分支 | `codex/issue-31-32-33-m2-completion`（工作区已提交、已推送；HEAD `fb65eda`） |
| 提交 | `092147a`（实现：员工仪表盘、质量门、生产构建、legacy 退役）＋ `fb65eda`（文档：计划/审查/工作流日志） |
| PR | **#42 — https://github.com/PNZ-z/ai-powered-Fabflix/pull/42**（`Complete the React migration: employee dashboard, quality gates, and production build`；base `main` ← head `codex/issue-31-32-33-m2-completion`；**OPEN，非 draft，MERGEABLE，未合并**；`closingIssuesReferences` = #31/#32/#33） |
| CI 首跑 | **head `fb65eda`：两个 job 均 SUCCESS** —— `Build and test (Java 11)`、`Frontend (lint, test, coverage, build, e2e)`（后者是新 `frontend` job 的首次运行） |
| Issue | [#31](https://github.com/PNZ-z/ai-powered-Fabflix/issues/31) / [#32](https://github.com/PNZ-z/ai-powered-Fabflix/issues/32) / [#33](https://github.com/PNZ-z/ai-powered-Fabflix/issues/33)（均**已存在于远端**，本工作流不修改远端 issue） |
| 计划文档 | `docs/plans/issue-31-migrate-employee-dashboard.md`、`…issue-32-…md`、`…issue-33-…md`（均含实施终验回填节） |
| 跟踪文档 | `docs/issues/issue-31-…md`、`issue-32-…md`、`issue-33-…md` |
| 全局约束 | `docs/architecture/frontend-migration-contracts.md`（本轮更新 §1/§2/§3/§6/§7/§8/§9 并新增 §11/§12） |
| 实现者 | 单个 `worker`（按批准计划实施；三路只读 reviewer 审查后由同一 worker 修复并复验） |
| 审查 | 三路并行只读审查（正确性/边界、测试覆盖、必要性与复杂度）：**均无 Blocker**；接受项已修复并复验 |

### 实际运行的验证命令与结果（除 CI 行外均为**主 agent 亲跑**）

| 命令 | 结果 |
| --- | --- |
| `mvn --batch-mode --no-transfer-progress verify` | **BUILD SUCCESS**；`Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`；`All coverage checks have been met.`；JaCoCo BUNDLE LINE **968 covered / 110 missed / 1078 total = 0.8980**（闸门 0.85） |
| JaCoCo 新增/改动类（行） | `SpaFallbackConfig` 0 missed / 13 covered；`SpaFallbackConfig$SpaResourceResolver` 0/5；`EmployeeController` 0/13；`CustomerAuthController` 0/24；`SearchController` 0/13 |
| `npm test`（`vitest run`） | **30 files / 195 passed** |
| `npm run test:coverage` | **Lines 92.35% (846/916)**（阈值 88）、Statements 90.05%、Branches 82.16%、Functions 91.17% |
| `npm run lint`（`oxlint src`） | 干净（exit 0，无 warning） |
| `npm run build` | 成功 |
| `npm run test:e2e`（Playwright chromium） | **12 passed**（360 / 1440 两档 viewport） |
| Node 22 证据（`node:22-slim` 容器，v22.23.3；同一命令**连续两次一致**） | lint 0；**195 tests / 30 files**；Lines **92.35%**；build 成功；容器内 `npx playwright install --with-deps chromium && npm run test:e2e` → **12 passed** |
| `mvn -Pfrontend -DskipTests clean package` | 成功；`target/fablix.jar` 含 **6 个** `BOOT-INF/classes/static` 条目（`index.html`、`favicon.svg`、`assets/index-*.css`、`assets/index-*.js` + 2 个目录条目） |
| dist 缺失守卫（临时移走 `frontend/dist` 后 `mvn -Pfrontend -DskipTests package`） | **BUILD FAILURE**（enforcer `require-frontend-dist`，报错含 `frontend/dist/index.html is missing`） |
| 生产 compose smoke（`docker compose build app` + `up -d --no-deps app`） | health **200**；`/` 含 `id="root"`；**10 条深链全部 200 `text/html`**；未认证 `/api/boot/cart` → **401 JSON**；`/API/boot/cart` → **404 JSON**；`/assets/missing.js` → **404**；顾客登录（`local` profile、dev 种子账号）→ session/customer 200 + cart `[]`；员工登录 → dashboard session 200 + metadata `status:success`；随后 `docker compose rm -sf app`（mysql 容器与数据卷未动） |
| **CI 首跑**（PR #42，head `fb65eda`）—— 非 docs-writer 执行 | **`Build and test (Java 11)` → SUCCESS**；**`Frontend (lint, test, coverage, build, e2e)` → SUCCESS**（该新 job 的第一次运行，因此 M2 checklist 中此前待验证的 CI 项与 AC-32-5/AC-33-5 的 CI 部分均已闭合）。来源：主 agent 核验的 GitHub CI 运行结果（docs-writer **未**直接访问 GitHub，**未**触发任何 CI 运行） |

> docs-writer 的**只读**复核（未运行任何测试/构建）：`git status`/`git diff`、`target/surefire-reports/*`（聚合 127/0/0/0）、`target/site/jacoco/jacoco.csv`（LINE 968/110/1078）、`unzip -l target/fablix.jar`、`git ls-files`、`grep`、配置与测试源码内容。

---

## 一、修改了哪些文件及潜在问题

### 1. Issue #31 — 员工迁移（`docs/plans/issue-31-…md` §13）

**新增（`frontend/src/features/employee/`）**

| 文件 | 目的 |
| --- | --- |
| `employeeApi.js` | 端点常量 + `dashboardRequestPath(action)` + `loginEmployee`/`logoutEmployee`/`fetchMetadata`/`addStar`/`addMovie`；表单用 `URLSearchParams` body（后端 `@RequestParam`） |
| `EmployeeLoginPage.jsx` | 员工登录表单（`Employee login`、`username`/`password` 均 `required`、无 reCAPTCHA）；成功后 `await refreshEmployee()` 再跳转，支持 `state.from` 回跳 |
| `EmployeeDashboardPage.jsx` | metadata 区块（行内 loading/空态/错误+重试）+ add-star/add-movie 表单 + 页内登出；401 清会话回登录页 |
| `employeeApi.test.js`(6) / `EmployeeLoginPage.test.jsx`(8) / `EmployeeDashboardPage.test.jsx`(16) / `employeeFlow.test.jsx`(1) | 请求形状（含 `action=` 参数）、页面状态、401/网络失败、`200 + status:"fail"`、登出成功/失败、axe |

**修改**

| 文件 | 目的 |
| --- | --- |
| `src/main/java/com/fabflix/boot/admin/EmployeeController.java` | 新增 `GET /api/boot/dashboard/logout`：`removeAttribute("employee")` + 204（D-31-1） |
| `src/test/java/com/fabflix/boot/admin/EmployeeControllerTest.java` | 新增 2 条 logout 用例（204 + `employee` 移除 + `user` 保留；未登录经 filter → 401） |
| `frontend/src/app/AppRoutes.jsx` | 员工两条路由改为真实页面；删除 `placeholderRoute` |
| `frontend/src/App.test.jsx` | 删除 `it.each(routeDefinitions)`；`responseBody(url)` 增加 4 条员工分派（最具体优先）；真实员工页/重定向断言 + 2 条 shell axe 断言 |
| `frontend/src/index.css` | 员工页样式 + 480px 断点必要规则；删除死 CSS `.route-placeholder`（保留 `__eyebrow`） |

**删除**：`frontend/src/app/routeDefinitions.js`、`frontend/src/features/placeholders/RoutePlaceholderPage.jsx`（占位机制最后两个消费者已消失，Q-3 裁定）。

**与计划文本的 4 处已裁定差异**（详情见计划 §13.2）：① add-star/add-movie 的 legacy 必填字段**恢复 `required`**（oracle 原先「legacy 无 required」判断有误，legacy `dashboard.html` 实有 `required`；其余「不做格式校验」口径不变）；② metadata 区块改为**行内非标题状态**（`<p role="status">`/`<p role="alert">`+Try again/普通 `<p>`），不复用 `components/states/`（避免同页第二个 `<h1>`）；③ `routeDefinitions.js` **整文件删除**而非留空模块；④ 见 #33 的 pom/`resourceChain(false)` 条（同批裁定）。

**前端用例数变化（计划要求报告）**：155 tests / 26 files（#30 基线）→ **195 tests / 30 files**；差值 = 删除 2 条占位参数化用例 + 新增员工 4 个文件 31 个用例 + `App.test.jsx` 新增 4 条。

### 2. Issue #32 — 响应式 / a11y / 测试 / CI（`docs/plans/issue-32-…md` §13）

| 文件 | 目的 |
| --- | --- |
| `frontend/src/index.css` | 全局 `:focus-visible`；新增 `@media (max-width: 480px)`（仅必要规则）；**评审修复**：暗色块内 `.form-error`/`.logout-control [role='alert']`/`.autocomplete [role='alert']`（`#f87171`）与 `.form-success`（`#4ade80`）对比度；删除 480px 冲突规则 |
| `frontend/src/features/cart/CartStatusProvider.jsx` | **评审修复（计划外）**：toast 的 `role="status"` 从内部 `<p>` 移到容器 `<div class="cart-toast">`，使整条 toast 成为一个 live region，避免触发 `region`（landmark）规则 |
| `frontend/src/test/axe.js` | `axe-core` 直连 + 自建 helper：`expectNoA11yViolations`（页面级，禁 `region`）/`expectNoShellA11yViolations`（shell 级，启 `region`）；两者均禁 `color-contrast` |
| `HomePage`/`LoginPage`/`MovieListPage`/`MovieDetailPage`/`CartPage`/`PaymentPage`/`ConfirmationPage` 的测试 | 各 +1 条 axe 断言（共 9 个页面 + `App.test.jsx` 2 条 shell 断言） |
| `frontend/e2e/{login,browsing,cart,employee}.spec.js` + `e2e/support/api.js` | Playwright smoke：**4 个 spec 文件 / 6 个用例**，API 全 `page.route` mock（契约保真：customer `displayName: null`、`/boot/confirmation` **204**、详情页 rating **字符串**、未 mock 路径 404 JSON）＋ `expectNoHorizontalOverflow` |
| `frontend/playwright.config.js` | 仅 chromium；`chromium-360`/`chromium-1440`；`webServer: npm run build && npm run preview -- --host 127.0.0.1 --port 4173 --strictPort`；CI `retries: 1` |
| `frontend/vitest.config.js` | `test.exclude` 加入 `**/e2e/**` 并**重述 vitest 默认排除项**；`coverage`（v8、`include: src/**`、exclude 三项、`thresholds.lines = 88`） |
| `frontend/package.json` / `package-lock.json` | 新增 3 个 devDependencies（`@playwright/test`、`@vitest/coverage-v8`、`axe-core`）+ `test:coverage`/`test:e2e` + `engines.node >= 22.12.0`；lock 重新生成（`@vitest/coverage-v8` peer 使 vitest 5.0.3 → 5.0.5，已接受） |
| `frontend/.gitignore` | 新增 `test-results`、`playwright-report`、`coverage` |
| `.github/workflows/ci.yml` | 新增独立 `frontend` job（Node 22 + npm 缓存 + `working-directory: frontend`；`npm ci → lint → test → test:coverage → build → playwright install → test:e2e`）；后端 job 未改 |

**与计划的差异**：单 axe helper → **双 helper**（`region` 按渲染树区分）；覆盖率阈值定稿 **lines 88**（实测 92.35%，低 4.35pp，符合「基线下方 2–5pp 且 ≥75%」）；另显式设 `coverage.include`（使未被测试导入的 `src/App.jsx` 计入分母，行覆盖 0%）；`RecaptchaField.jsx` 核查后**未改**；`.oxlintrc.json` 未加 a11y 规则。

### 3. Issue #33 — 生产构建与 legacy 退役（`docs/plans/issue-33-…md` §7/§13）

| 文件 | 目的 |
| --- | --- |
| `pom.xml` | 新增 `frontend` profile：enforcer `require-frontend-dist`（`validate` 相位，`<fail>true</fail>`）+ resources 插件（显式 `3.3.1`）`copy-frontend-dist`（`process-resources` → `target/classes/static`）；默认构建行为不变 |
| `src/main/java/com/fabflix/boot/web/SpaFallbackConfig.java` | SPA fallback：`/**` → `classpath:/static/` + 自定义 `SpaResourceResolver`；纯函数 `shouldFallback(path)`（排除 `api/**`、`error/**`、含 `.` 的路径、空/null；大小写归一） |
| `src/main/java/com/fabflix/boot/auth/CustomerAuthController.java` | 非 JSON 登出 302 落点 `/login.html` → **`/login`**（D-33-7） |
| `src/main/java/com/fabflix/boot/search/SearchController.java` | jump-back 302 落点 `/movieList.html?<q>` → **`/movies?mode=fulltext&<q>`**（D-33-7；缺失 `mode=fulltext` 会丢 `query`） |
| `src/main/webapp/**`（**21 个跟踪文件**） | **已删除并已随提交 `092147a` 进入 git**（`git ls-files src/main/webapp` = 0）：`META-INF/MANIFEST.MF`、`styles.css` + 19 个 legacy `.html`/`.js` |
| `src/test/java/com/fabflix/boot/web/SpaFallbackTest.java` | 5 个用例：真值表（含 `API`/`Error` 大小写变体）、真实资源/入口文档、缺失资源与 `api` 路径 → null、入口文档缺失 → null、**注册层断言**（`/**` handler 用 SPA resolver 且 location 为 `classpath:/static/`） |
| `src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java` | 重写为**退役守卫**（目录不存在 + 若存在则无 `.html`/`.js`）；删除 `criticalPagesUseTheirSpringBootEndpoints` 与 `Files.walk` 缺失目录的旧写法 |
| `CustomerAuthControllerTest.java` / `SearchControllerTest.java` | 断言更新为新落点 |
| `Dockerfile` | 三阶段（`node:22-alpine` → `maven:3.9-eclipse-temurin-11` + `dependency:go-offline` 缓存层 → `eclipse-temurin:11-jre`）；**评审修复：非 root 运行（uid/gid 1001 的 `fabflix`）** |
| `.dockerignore` | 排除 `node_modules`/`target`/`.git`/`.idea`/`.pi`/`docs`/`frontend/dist` 及 Playwright/coverage 产物 |
| `compose.yaml` | 新增 `app` 服务（`build: .`、`depends_on: mysql(service_healthy)`、`8081:8081`、容器内 `FABFLIX_DB_URL=jdbc:mysql://mysql:3306/...`、`SPRING_PROFILES_ACTIVE=local`）；`mysql` 服务未改 |
| `README.md` / `frontend/README.md` / `fabflix.http` | 改写 legacy 表述为「已退役」；新增 Docker Compose、生产构建（`npm ci && npm run build` + `mvn -Pfrontend clean package`）、Node 22、`test:coverage`/`test:e2e`；jump-back 注释更新 |

### 潜在问题与残余风险（跨三个 issue，与 `docs/plans/…§13.5` 一致）

1. ~~**提交/推送/PR/CI 均未发生**~~ → **已闭合（2026-10-15）**：已提交 `092147a`/`fb65eda`、已推送、**PR #42 已创建（OPEN，未合并）**；**CI 首跑（head `fb65eda`）两个 job 均 SUCCESS**，M2-BE-6 与 AC-32-5/AC-33-5 的 CI 证据已回填。**仍待处理**：PR 合并与 issue 关闭（需用户/主 agent 决定）——在此之前**不得**写「已合并/已关闭」。
2. ~~**legacy 删除尚未反映到 git 索引**~~ → **已闭合（2026-10-15）**：21 个文件已随提交 `092147a` 移除，`git ls-files src/main/webapp` = **0**（M2-DOC-2 已为真）。
3. **手工颜色对比度**：jsdom 无布局/无颜色计算 → axe 的 `color-contrast` 全局禁用；新增暗色色值按 ≥4.5:1 选择，**仍需人工在浏览器确认**。
4. **axe 规则边界**：`region` 按渲染树区分（页面级禁用 / shell 级启用）；其余依赖布局的规则未专门处理 → 「违规集合为空」不等于真实浏览器审计结论。
5. **e2e 全为 mock**：`page.route` 保真度已尽量贴近契约，但不能替代真实后端/MySQL 往返；真实存储过程（`add_movie`）往返未自动化。
6. **已知测试缺口**：`AdminRepository` 的 metadata 遍历与 `add_movie` 结果集解析**仍无直接测试**（计划 Q-6 裁定不补，记录为已知缺口）。
7. **`frontend/src/App.jsx` 前端行覆盖 0%**：因 `coverage.include: src/**` 计入分母（不排除以抬分），拖低整体数字但仍高于阈值。
8. **M2-BE-5 未完全验证**：未单独执行「干净 checkout 的 `npm ci` + `mvn -Pfrontend clean package`」单次端到端复现（各步骤分别验证过）；**已由 PR #42 CI 首跑间接补强**（`frontend` job 在干净 checkout 上跑通 `npm ci → … → npm run build`、后端 job 跑通 `mvn verify`，两 job 均 SUCCESS）——但该条本身仍记为**未完全验证**。
9. **仓库卫生（已随提交改善）**：提交后 `git status --short --untracked-files=all` **仅剩 6 项**未跟踪的 `.idea/*`（本机 IDE 文件，**非本次变更**）→ 是否纳管（或加入 `.gitignore`）仍需主 agent 决定；`frontend/dist`/`coverage`/`test-results`/`playwright-report` 均已被 gitignore。
10. **未验证的显式边界**：`RecaptchaField` 在真实浏览器 + 真实 key 下的渲染（#32 未改、未验证）；「同一浏览器同时持有顾客与员工身份后员工登出」仅有后端单测断言（`user` 保留），无 e2e 覆盖。
11. **依赖抬升**：`@vitest/coverage-v8` 的 peer 要求使 vitest 5.0.3 → 5.0.5（已接受，Node 24 与 Node 22 下复跑通过）。

**未发现其他已知遗留问题**（已按上述条目逐条记录；无“零风险”声明）。

### 后续事项

1. ~~主 agent 授权后：暂存/提交/推送 → `github-operator` 创建 PR（`Closes #31`/`#32`/`#33`）→ 回填 PR 链接、CI 首跑结果~~ → **已完成（2026-10-15）**：提交 `092147a`/`fb65eda`、已推送、PR **#42** 已创建（OPEN，未合并，`closingIssuesReferences` = #31/#32/#33）；CI 首跑两个 job 均 SUCCESS，计划/issue 文档已回填（#31/#32 计划 §13、#33 计划 §7.1/§13.4）。
   - **仍待主 agent/用户决定**：PR 合并与 issue 关闭；`.idea/*` 是否纳管。
2. ~~提交后复核 `git ls-files src/main/webapp` 为空、`git ls-files frontend/dist` 为空~~ → **已复核（2026-10-15，docs-writer 只读）**：两者均为 0 行。
3. **M2-BE-5（干净 checkout 的 `npm ci` + `mvn -Pfrontend clean package` 单次端到端复现）仍未单独执行**：各步骤已分别验证，CI 的 `npm ci` + build 作为间接补强。
4. 人工浏览器检查：暗色模式对比度、360px 窄屏观感、toast 2 秒消失、键盘焦点可见性。
5. 若后续需要：补 `AdminRepositoryTest`（metadata/存储过程）、真实 MySQL 的员工存储过程往返测试、e2e 真实后端 smoke（均属新范围，需另立计划）。

---

## 二、新增知识点 / 重点学习对象

### Spring / Java

- **Spring MVC 资源处理与 SPA fallback 的 resolver 链 + `@Order`【重要】**
  - 含义：Spring MVC 的静态资源由 `ResourceHttpRequestHandler` + `ResourceResolver` 链处理：先按 `resourcePath` 在 `location`（这里是 `classpath:/static/`）下查真实文件，全部 resolver 返回 null 才 404。自定义 `PathResourceResolver` 覆写 `getResource` 即可在“文件不存在”时返回 `index.html`，实现 SPA 深链。`@Order(Ordered.LOWEST_PRECEDENCE)` 让本配置注册的 `/**` handler **覆盖** Boot 的默认资源 handler（`@Order(0)`），代价是丢掉了 Boot 的其它默认静态位置（`META-INF/resources/`、`resources/`、`public/`）——本项目不在那里放资源所以可接受。
  - 位置：`src/main/java/com/fabflix/boot/web/SpaFallbackConfig.java`（`resourceChain(false).addResolver(...)`）、`src/test/java/com/fabflix/boot/web/SpaFallbackTest.java`
  - 学习价值：这是“前端路由 + 后端兜底”的通用模式（Vue/React/Angular 同构）；同时演示了「顺序元数据 + resolver 链」这一 Spring 扩展点，以及**判定逻辑抽成纯函数**（`shouldFallback`）以便不启动容器就能测。
- **`resourceChain(false)` 的语义（容易误解）**
  - 含义：`ResourceHandlerRegistration.resourceChain(false)` 表示**不启用**带缓存的资源链，但 `addResolver(...)` 仍会触发调用链注册；`resourceChain(true)` 会启用 `CachingResourceResolver`/`CachingResourceTransformer` 等**无界缓存**，长跑进程有内存增长风险。写 fallback 时通常只需 resolver 而不要缓存。
  - 位置：`SpaFallbackConfig.addResourceHandlers`
  - 学习价值：避免把“开启 chain”与“注册 resolver”当成同一件事；评审正是靠这一点发现了缓存风险。
- **`MockHttpSession` 与 standalone MockMvc 的接线【常见坑】**
  - 含义：`MockMvcBuilders.standaloneSetup(controller)` **不会注册任何 filter**，也不加载 Spring 上下文：要验证「未认证 → 401」必须 `.addFilters(new EmployeeAuthenticationFilter())`；要验证「一次请求内移除属性但保留另一个」，需把两个属性写进**同一个** `MockHttpSession` 并 `.session(shared)`（`.sessionAttr(...)` 每次 `perform` 都会新建 session，无法跨请求/无法观察同一次请求后的属性）。
  - 位置：`src/test/java/com/fabflix/boot/admin/EmployeeControllerTest.java`（`employeeMvc()`）；对比 `SpaFallbackTest` 用 `InspectableRegistry` 暴露 `ResourceHandlerRegistry.getHandlerMapping()` 做注册层断言。
  - 学习价值：这是后端“最小化测试基座”（全仓库无 `@SpringBootTest`）下的核心技巧；理解它才能写出有意义的鉴权断言，否则会出现“401 用例得到 200/404”的伪测。
- **JaCoCo BUNDLE 行覆盖率闸门与“行数预算”思维**
  - 含义：`verify` 阶段强制 `BUNDLE / LINE / COVEREDRATIO ≥ 0.85`；每次新增后端代码都会同时增加“未覆盖行”，因此新类必须自带测试。本轮实测 968 covered / 110 missed / 1078 total = 0.8980，5 个新增/改动类均 **0 行未覆盖**。
  - 位置：`pom.xml`（jacoco 插件）、`target/site/jacoco/jacoco.csv`（列序：`LINE_MISSED`/`LINE_COVERED` 在第 8/9 列——读报告时容易错列）
  - 学习价值：把覆盖率当作“预算”而非“分数”，能提前判断一个 PR 会不会让 CI 变红。

### 构建 / 部署

- **`maven-enforcer-plugin` 守卫构建前置条件（fail-fast）**
  - 含义：`requireFilesExist` 规则可在**更早的相位**（本轮选 `validate`）检查 `frontend/dist/index.html` 是否存在，缺失即 `BUILD FAILURE`。这修复了 `maven-resources-plugin:copy-resources` 对不存在的目录“只告警不失败”的静默成功问题——否则会打出**不含 UI 的 jar**。
  - 位置：`pom.xml` 的 `frontend` profile
  - 学习价值：构建系统的“负向验证”（故意制造缺失、确认失败）与正向验证同等重要；守卫必须绑定在**消费者之前**的相位。
- **本项目无 `<parent>` ⇒ 插件版本必须显式声明**
  - 含义：dependency BOM（`spring-boot-dependencies`）只管理**依赖**版本，不管理**插件**版本；没有 `<parent>` 时 `maven-resources-plugin` 若不写 `<version>` 会产生 `plugin.version is missing` 警告并依赖 Maven 的隐式绑定行为（跨版本可能变）。
  - 位置：`pom.xml`（`<version>3.3.1</version>` + 注释）
  - 学习价值：区分“依赖版本管理”与“插件版本管理”，是排查构建可复现性问题的常见盲点。
- **Docker 多阶段构建 + 非 root 运行 + 依赖缓存层**
  - 含义：stage 1（`node:22-alpine`）构建前端并 `test -f dist/index.html`；stage 2（`maven:3.9-eclipse-temurin-11`）先 `COPY pom.xml` + `dependency:go-offline` 形成可缓存层，再复制源码与 `dist` 打包；stage 3（`eclipse-temurin:11-jre`）只复制 jar，创建 uid/gid 1001 用户并 `USER fabflix`（最小攻击面 + 兼容只读挂载/受限集群策略）。
  - 位置：`Dockerfile`、`.dockerignore`、`compose.yaml` 的 `app`
  - 学习价值：多阶段是“构建环境与运行环境分离”的标准做法；`.dockerignore` 直接决定构建上下文大小（排除 `node_modules`/`target`/`.git`/`.pi` 可显著加速）。
- **Compose 网络内的服务名寻址 + profile 差异**
  - 含义：容器内数据库必须用 `mysql:3306`（服务名）而不是宿主机的 `localhost:3307`；`SPRING_PROFILES_ACTIVE=local` 才会激活 `LocalRecaptchaVerifier`，从而让顾客登录在演示栈中可用（生产必须配 `RECAPTCHA_SECRET_KEY`）。
  - 位置：`compose.yaml`、`application-local.properties`、`auth/LocalRecaptchaVerifier`/`GoogleRecaptchaVerifier`
  - 学习价值：把“环境变量/激活 profile”与“代码里的 `@Profile` 分支”连起来看，能解释容器内登录 403 这类现象。

### 前端 / 测试

- **Vite 构建期变量内联（构建期 vs 运行期）**
  - 含义：`VITE_*` 变量在 `vite build` 时被静态替换进产物；运行期注入环境变量**无效**。此类变量必须区分：`VITE_RECAPTCHA_SITE_KEY`（**构建期**）vs `FABFLIX_DB_*`/`SPRING_PROFILES_ACTIVE`（**运行期**，Spring 读取）。
  - 位置：`README.md` 生产部署段；`frontend/.env.example`
  - 学习价值：这是“配置为什么没生效”的头号原因；也是 Docker 多阶段构建必须在前端 stage 传入构建参数的原因。
- **Playwright `webServer` + `page.route` + 多 viewport project**
  - 含义：`webServer.command` 让测试自带构建与预览服务器（`npm run preview -- --host 127.0.0.1 --port 4173 --strictPort`，显式绑定 IPv4 避免 `localhost` 解析到 `::1`），`url` 作为就绪探针；`page.route('**/api/**', …)` 提供有状态假后端，做到**无 MySQL/无网络波动**的确定性；`projects` 用同一浏览器 + 两个 viewport（360/1440）把同一用例跑两遍（6 用例 × 2 = 12）。
  - 位置：`frontend/playwright.config.js`、`frontend/e2e/**`、`frontend/e2e/support/api.js`
  - 学习价值：e2e 的可维护性依赖“mock 保真”（如 `displayName: null`、204 空体、rating 字符串）而非“能跑通”；viewport 作为 project 是覆盖响应式需求的最低成本做法。
- **vitest 的 `exclude` 会替换默认值 ⇒ 必须重述默认排除项**
  - 含义：vitest 默认 `include` 会匹配 `e2e/*.spec.js`；一旦显式设置 `test.exclude`，**默认排除项不会自动保留**，需把 `node_modules`/`dist`/`.idea`/各工具 `*.config.*` 等一并列出，否则可能意外收集无关文件。本轮把 `**/e2e/**` 加入 exclude，使 `npm test` 不再尝试执行 Playwright 用例（否则 `import { test } from '@playwright/test'` 直接失败）。
  - 位置：`frontend/vitest.config.js`
  - 学习价值：两套测试运行器共存时的边界管理；「B1 修不修都会红」类问题的典型形态。
- **vitest v8 coverage 的阈值与 `include` 口径**
  - 含义：`coverage.thresholds.lines = 88` 会让未达标的 `npm run test:coverage` 失败（CI 阻断）；`include: ['src/**/*.{js,jsx}']` 表示**未被任何测试导入的文件也计入分母**（如 `src/App.jsx` 0%），这是防止“只测好测的模块”来抬分的关键口径；`exclude` 只放入口/测试设施。
  - 位置：`frontend/vitest.config.js`
  - 学习价值：理解阈值、分母口径、exclude 白名单三者如何共同决定闸门强度（exclude 越宽，闸门越虚）。
- **axe-core 直连与 jsdom 的规则边界**
  - 含义：`axe.run(container, { rules })` 可直接在 vitest 里用；jsdom 无布局、无颜色计算，`color-contrast` 必须禁用/人工确认；`region`（landmark）规则在“页面组件单独渲染、没有 `AppShell` 的 `<main>`”时会误报，因此 helper 分两个变体按渲染树启用/禁用该规则。
  - 位置：`frontend/src/test/axe.js`、各页面测试、`App.test.jsx`
  - 学习价值：把“自动化能证明什么”与“只能人工确认什么”写进测试注释与文档，是 a11y 工作中最容易被过度宣称的部分。
- **员工/顾客 session 属性语义（`removeAttribute` vs `invalidate`）**
  - 含义：同一 `JSESSIONID` 可同时持有 `user`（顾客）与 `employee`（员工）两个属性；员工登出只 `removeAttribute("employee")`，顾客登出才 `invalidate()` 整个 session。前端也不能复用一个“登出”实现，否则会误杀对方会话。
  - 位置：`EmployeeController.logout`、`CustomerAuthController.logout`、`EmployeeControllerTest.logoutRemovesEmployeeButKeepsCustomerSession`
  - 学习价值：多身份共存的会话设计要点；也是“行为变更/新增端点必须写成测试断言”的典型案例。

### 测试术语 / 工程习惯

- **负向验证（negative verification）**：移走 `frontend/dist` 确认构建失败、断言 `/API/boot/cart` 不被 fallback 吞成 HTML、断言 `200 + status:"fail"` 仍算失败——这些都在验证“系统在错误输入下的行为”，比只验证 happy path 更能防止回归。
- **证据分层**：单测（判定逻辑/注册层）→ 组件测试（页面状态）→ e2e（浏览器行为，mock API）→ 生产 smoke（真实 jar/容器/MySQL）。本轮文档明确写出“哪一层能证明什么、不能证明什么”，避免把单测当作生产验证。

---

> **本日志口径**：所有“通过/实测”数字均来自主 agent 终验、**PR #42 的 CI 首跑结果（主 agent 核验）**与 docs-writer 的只读产物复核；docs-writer **未**运行测试/构建、**未**触发 CI。当前状态：已提交（`092147a`/`fb65eda`）、已推送、**PR #42 OPEN（未合并）**；CI 首跑两 job 均 SUCCESS。PR 合并/关闭后（若发生）需再次更新本日志与相关计划/issue 文档。

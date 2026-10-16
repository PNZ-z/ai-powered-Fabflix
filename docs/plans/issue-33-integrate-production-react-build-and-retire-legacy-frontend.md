# Issue #33 方案 / 计划 — Integrate production React build and retire the legacy frontend

> **实现状态：实施完成，已通过主 agent 终验（2026-10-15）；已提交（`092147a` 实现 + `fb65eda` 文档）、已推送、PR #42 已创建（OPEN，未合并）。**
> 分支 `codex/issue-31-32-33-m2-completion`（HEAD `fb65eda`）；legacy 删除已随实现提交 `092147a` 进入 git（`git ls-files src/main/webapp` = **0**，§7 的 M2-DOC-2 已为真）；**CI 首跑（head `fb65eda`）两个 job 均 SUCCESS**（见 §7.1 M2-BE-6 与 §13.4）。
> 本文档 §1–§11（及 §12 的批准时记录）保留为**批准时的计划文本**（事实性内容未追改）；实现结果、与计划的逐项差异、**M2 checklist（§7）的逐条证据回填**与残余风险见 **§7** 与 **§13**。
>
> **文档状态（批准时）：已批准（主 agent，2026-10-15）— 已交 worker 实现。**
>
> 依据 oracle 第一轮只读审查报告 `.pi/artifacts/oracle-issues-31-32-33-plans.md`（#33 章节），**三个 Blocker 已全部闭合**：
> **B3**（容器内顾客登录的 reCAPTCHA/profile 策略）→ 裁定 **方案 (a)：`compose.yaml` 的 `app` 显式 `SPRING_PROFILES_ACTIVE=local`**，仅用于本地/演示完整栈 smoke，README 必须写明生产需配 `RECAPTCHA_SECRET_KEY` + **构建期** `VITE_RECAPTCHA_SITE_KEY`；
> **B4**（`-Pfrontend` 在 `frontend/dist` 缺失时静默成功）→ `-Pfrontend` 下 **dist 缺失必须构建失败**（enforcer `requireFilesExist` 或 antrun fail）+ Docker/本地统一 `mvn -Pfrontend clean package`；
> **B5**（两处仍在生效的 legacy 重定向）→ **主 agent 明确批准行为变更**：`/login.html` → `/login`，`/movieList.html?<query>` → `/movies?mode=fulltext&<query>`（`lastMovieQuery` 只由 fulltext 写入，**必须带 `mode=fulltext`**），同步改 `CustomerAuthControllerTest`、`SearchControllerTest`、`fabflix.http`。
> 建议 N-33-1..N-33-10 与 Q-1..Q-9 已全部裁定并写入本文档（逐行回填见 §11；§10 已转为「已裁定问题」表）。
> **本批修订不改变 #33 的 AC-33-1..AC-33-6**；**主 agent 已在本机用 `docker manifest inspect` 验证三个 Docker 镜像可拉取**（oracle 所在环境因无网络未能验证）。
> **批准状态不代表已实现**：M2 checklist（§7）的生产 smoke 仍需**实际执行并回填证据**；若 worker 环境拉不到镜像/无网络，**必须如实标注“未执行”**（D-33-4）。文件:行号若与 oracle 报告不一致，**以 oracle 报告（其已复核）为准**并在相关节注明。

## 元数据

| 项目 | 值 |
| --- | --- |
| GitHub issue | #33 — https://github.com/PNZ-z/ai-powered-Fabflix/issues/33 |
| 标题（远端原文） | Integrate production React build and retire the legacy frontend |
| Milestone | Phase 2 — React Frontend Migration（与 #31/#32 共用） |
| 来源 | 用户任务 + GitHub issue #33 远端正文（主 agent 用 `gh` 获取）+ scout 报告 `.pi/artifacts/scout-issues-31-32-33.md` + docs-writer 亲自核对的源码 |
| 关联 issue 跟踪文档 | `docs/issues/issue-33-integrate-production-react-build-and-retire-legacy-frontend.md` |
| 关联全局约束 | `docs/architecture/frontend-migration-contracts.md`（§2 legacy 保留策略、§8 架构守卫） |
| 实现顺序 | **#31 → #32 → #33**；#33 是 M2 最后一个 issue，依赖全部功能迁移与质量 issue（issue 正文 "Dependency"）；**#31 完成前不得删 legacy** |
| 分支 / PR 计划 | 分支 `codex/issue-31-32-33-m2-completion`，单个 PR（`Closes #31`/`#32`/`#33`） |
| 创建日期 | 2026-10-15 |
| 文档状态 | **已批准（主 agent，2026-10-15）— 可交 worker 实现**；B3/B4/B5 已闭合，N-33-1..N-33-10 与 Q-1..Q-9 已全部裁定（§10/§11） |
| 验收范围 | **本批修订不改变 #33 的 AC-33-1..AC-33-6**；两处重定向的行为变更已由主 agent 明确批准（§1.3） |

## 1. 目标与背景

### 1.1 目标

让 React 成为**唯一受支持的生产前端**并安全收尾 M2（issue 正文 "Goal"）：把构建产物接入 Spring Boot 与 Docker、
配置不拦 API 的 SPA 路由 fallback、用生产构建验证顾客与员工全部流程、删除被取代的 legacy HTML/JavaScript、
更新 README / 架构说明 / 本地与生产启动说明。

### 1.2 背景：现状事实（带文件:行号证据）

> docs-writer 亲自复核（`main` @ `f6163f3`）；与 scout 不一致处已注明。

**Spring Boot 静态资源与服务**

- 全仓库**没有** `WebMvcConfigurer` / `addResourceHandlers` / `ResourceHandlerRegistry` / `forward:`（已 grep `src/main/java`：**0 命中**）。
- **没有** `src/main/resources/static/` 或 `public/` 目录（`find src/main -type d -name static`：0）。`src/main/resources` 只有 `application.properties`、`application-local.properties`。
- `pom.xml:10` `<packaging>jar</packaging>`；`pom.xml:12` `<finalName>fablix</finalName>`；`spring-boot-maven-plugin` 的 `repackage`（`pom.xml:104-120`，mainClass = `com.fabflix.boot.FabflixApplication`）。
  - **`maven-resources-plugin` 静默成功事实（oracle B4 复核，本计划已按此修定）**：`copy-resources` 对**不存在**的 `<directory>` 只告警并继续（`skipNonExistingResourceDirectory` 语义）→ 忘记 `npm run build` 时 `mvn -Pfrontend package` **仍会成功**并产出**不含 UI 的 jar**，与 AC-33-1 的证据要求相矛盾。修见 §1.3（B4）/§4.1/§5.1。
- **`pom.xml` 中 `<profiles>` 数量 = 0**（docs-writer 复核）→ 尚无 `frontend` profile。
- `src/main/resources/application.properties:1` `server.port=${FABFLIX_BOOT_PORT:8081}`；第 3-5 行 `spring.datasource.url/username/password` 默认指向 `jdbc:mysql://localhost:3307/moviedb`、用户 `fabflix`。
- `HealthController.java:10-16`：`GET /api/boot/health` → `{"status":"UP","application":"fabflix-spring-boot"}`。
- 权限现状：`GET /api/boot/health` 对两个 filter 都公开；其余 `/api/boot/**` 受 `CustomerAuthenticationFilter` 保护（豁免 `login`/`employee-login`/`dashboard*`/`health`，`CustomerAuthenticationFilter.java:18-25`）；`/api/boot/dashboard*` 由 `EmployeeAuthenticationFilter` 保护（`EmployeeAuthenticationFilter.java:16-19`）。
- 当前 React 只在开发期通过 Vite 同源代理访问后端（`frontend/vite.config.js:12-19`，target 默认 `http://localhost:8081`）。
- **容器内顾客登录的 reCAPTCHA 事实（oracle B3 复核，本计划已按此修定）**：`LocalRecaptchaVerifier` 带 `@Profile("local")`（`auth/LocalRecaptchaVerifier.java:7-8`），`GoogleRecaptchaVerifier` 带 `@Profile("!local")`（`auth/GoogleRecaptchaVerifier.java:12`）且读 `fabflix.recaptcha.secret-key`（默认空串，`application.properties:10`）；**未启用 `local` 且未注入密钥时，顾客登录返回 403 `Recaptcha Not Passed`** → 直接决定 compose `app` 的 profile/环境变量（B3 → §1.3/§4.4/§7.3）。

**前端构建**

- `frontend/vite.config.js`：`appType: 'spa'`（第 10 行）；**未设置** `base`、`build.outDir`、`build`（已 grep：0 命中）→ 默认 `base='/'`、`outDir='dist'`。
- `frontend/package.json:8` `build = vite build`；无 `engines`（Node 版本未固定 — #32 D-32-3 会处理）。
- `frontend/.gitignore:11` 忽略 `dist`；`git ls-files frontend/dist` = **0**（**未提交、被忽略**）。磁盘上存在 `frontend/dist/`（scout C2）。
- `frontend/index.html:1-12` 为 Vite 入口（`<div id="root">` + `/src/main.jsx`）。

**Docker**

- `compose.yaml`：**只有 `mysql` 服务**（第 1-24 行）：`mysql:8.4`、`container_name: fabflix-mysql`、`MYSQL_ROOT_PASSWORD`/`MYSQL_DATABASE=moviedb`/`MYSQL_USER=${FABFLIX_DB_USER}`/`MYSQL_PASSWORD=${FABFLIX_DB_PASSWORD}`（第 6-10 行）、端口 `3307:3306`（第 12 行）、初始化脚本挂载 4 个 `.sql`（第 15-18 行）、`healthcheck` 用 `mysqladmin ping`（第 19-24 行）、volume `fabflix-mysql-data`（第 26-27 行）。
- **没有** `Dockerfile` / `.dockerignore`（docs-writer 复核：`find` 结果 0）。

**legacy 退役范围**

- `src/main/webapp/**`：`git ls-files src/main/webapp` = **21 个**跟踪文件（docs-writer 实测）：
  `META-INF/MANIFEST.MF`、`styles.css`、`_dashboard.html`、`_dashboard.js`、`confirmation.html`、`dashboard.html`、`dashboard.js`、`login.html`、`login.js`、`main.html`、`main.js`、`movieList.html`、`movieList.js`、`payment.html`、`payment.js`、`shoppingCart.html`、`shoppingCart.js`、`single-movie.html`、`single-movie.js`、`single-star.html`、`single-star.js`。
  - **数量修正**：scout C4 写「20 文件」但同一清单实际列出 21 项；主 agent 的任务说明也写「20 个 git 跟踪文件」。**实际为 21**（含 `styles.css` 与 `META-INF/MANIFEST.MF`）。
- **钉死 legacy 的测试** `src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java`：
  - 第 13 行 `WEBAPP = Path.of("src","main","webapp")`；`frontendNoLongerCallsLegacyServletApiPaths`（第 15-22 行）用 `Files.walk(WEBAPP)` → **目录不存在会抛 `IOException` → 测试报错**。
  - `criticalPagesUseTheirSpringBootEndpoints`（第 24-35 行）**要求 9 个 legacy 文件仍存在**且含指定字符串（`login.js`、`main.js`、`movieList.js`、`single-movie.js`、`single-star.js`、`shoppingCart.js`、`payment.js`、`_dashboard.js`、`dashboard.js`）→ **删除这些文件必然失败**。
- `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java`：只检查 `src/main/java/**/*.java`（第 12-38 行：不得含 `jakarta.servlet`/`@WebServlet`/`extends HttpServlet`）与 `pom.xml`（`jar` + `repackage`、无 `jakarta.servlet-api`）→ **不影响 webapp 删除**，预期保持通过。
- **⚠️ docs-writer 新发现的 legacy 引用（scout 未列）**：
  - `src/main/java/com/fabflix/boot/auth/CustomerAuthController.java:59-69`：顾客登出在 `Accept` **不含** `application/json` 时 `response.sendRedirect("/login.html")`（第 68 行）→ 删除 webapp 后指向**不存在的文件**。钉死测试：`src/test/java/com/fabflix/boot/auth/CustomerAuthControllerTest.java:97`（`.andExpect(redirectedUrl("/login.html"))`）。
  - `src/main/java/com/fabflix/boot/search/SearchController.java:42-48`：`GET /api/boot/jump-back` 返回 **302 → `/movieList.html?<query>`**（第 47 行）→ 同样指向 legacy 文件。钉死测试：`src/test/java/com/fabflix/boot/search/SearchControllerTest.java:55,60`。
- 文档中的 legacy 引用（scout C4，docs-writer 复核行号）：`README.md:15`（架构图 "legacy HTML + JavaScript retained"）、`README.md:67`（`http://localhost:8081/login.html`）、`README.md:99`（"The existing HTML/JavaScript frontend remains the supported application…"）、`frontend/README.md:3`、`frontend/README.md:56`、`docs/architecture/frontend-migration-contracts.md` §2/§8。
- 仓库**无** `.jsp`。

**SPA 路由清单与深链要求**

- `frontend/src/app/AppRoutes.jsx`（docs-writer 已读）：`index → /home` 重定向（第 36 行）、`login`（第 37 行）、顾客受保护（第 39-48 行）：`home`、`movies`、`movies/:movieId`、`stars/:starId`、`cart`、`checkout`、`confirmation`；员工（第 49-52 行，**#31 会改为真实页面**）：`employee/login`、`employee/dashboard`；`playground`（第 53 行）；`*` → `NotFoundPage`（第 54 行）。
- 生产下这些**非 `/api` 前缀**的深链必须由 Spring Boot 返回 `index.html`，且 `/api/**` 必须**不被拦截**（scout C5 / `frontend/README.md:71-74`）。

**生产验证手段**

- `fabflix.http`（仓库根）：顾客/员工登录、metadata、add-star、add-movie、cart/payment 请求示例（员工部分约第 50-92 行），可作手动 smoke。
- **无** `scripts/` 目录、**无** smoke 脚本。
- **「M2 regression checklist」在本仓库无既有定义**（scout C6 已 grep 确认）→ 本计划在 §7 给出草案（D-33-4 允许）。

### 1.3 已批准决定（用户/主 agent 已传达；写入本文档作为**已批准范围**）

| 编号 | 已批准决定 |
| --- | --- |
| **D-33-1** | 生产构建走 **Docker 多阶段**（**不提交 `frontend/dist`**）：前端 `node:22` 阶段构建 → Maven 阶段在 **`-Pfrontend` profile** 下用 **resources 插件**把 `frontend/dist` 复制进 `target/classes/static` → 运行阶段用 JRE 镜像跑 `target/fablix.jar`。`compose.yaml` 新增 **`app` 服务**（`depends_on: mysql healthy`、端口 **8081**、env 注入 `FABFLIX_DB_*`）。本地流程写入 README（`cd frontend && npm ci && npm run build` 后 `mvn -Pfrontend package`）。 |
| **D-33-2** | **SPA fallback**：真实静态资源优先；**`/api/**` 一律不被 fallback 拦截**；其余非文件路径（`/home`、`/movies/...`、`/cart`、`/employee/...` 等）返回 `index.html`；需有针对性测试（在「不依赖 `@SpringBootTest`」的现有约定下，可对 resolver/配置类做隔离单测）。 |
| **D-33-3** | **删除整个 `src/main/webapp/**`**（用户已批准这一破坏性动作；实际 **21** 个 git 跟踪文件，见 §1.2 数量修正）；`FrontendApiMigrationTest` 替换为「legacy 已退役」守卫测试（例如断言目录不存在 / 无 `.html` / 无 `.js`）；同步更新 `README.md`、`frontend/README.md`、`docs/architecture/frontend-migration-contracts.md` 中所有 legacy 引用；`SpringBootOnlyArchitectureTest` 保持通过。 |
| **D-33-4** | **M2 regression checklist 作为文档产出**（写在 #33 计划内或独立 `docs/` 文档）：条目需**可执行、可记录证据**——后端 `mvn verify`、前端 lint/test/coverage/build、e2e、以及生产形态 smoke（`docker compose build app` + health + index.html + 关键 API/页面）；**实际执行结果由 worker 回填；无法执行的项如实标注**。 |
| **D-33-5（新增，B3 裁定 = 方案 a）** | **`compose.yaml` 的 `app` 服务显式设 `SPRING_PROFILES_ACTIVE=local`**（仅用于本地/演示完整栈 smoke）；`README.md` 必须写明**生产部署需配置 `RECAPTCHA_SECRET_KEY`**，且前端 `VITE_RECAPTCHA_SITE_KEY` 是**构建期**变量（Vite 构建期内联，**运行期注入无效**）。M2-PROD-7 按此重写。 |
| **D-33-6（新增，B4 裁定）** | **`-Pfrontend` 在 `frontend/dist/index.html` 缺失时必须构建失败**（`maven-enforcer-plugin` 的 `requireFilesExist` 或 `maven-antrun-plugin` 的 `<fail>`，绑定在 `process-resources` 之前）；Docker 与本地文档统一 `mvn -Pfrontend clean package`；Dockerfile 前端阶段可加 `test -f dist/index.html`。 |
| **D-33-7（新增，B5 裁定：行为变更，主 agent 已批准）** | `CustomerAuthController.java:68` 的非 JSON 登出重定向 **`/login.html` → `/login`**；`SearchController.java:47` 的 jump-back **`/movieList.html?<query>` → `/movies?mode=fulltext&<query>`**（**必须带 `mode=fulltext`**：`lastMovieQuery` 只由 `fullTextSearch` 写入，缺此参数时 React 列表页落到 browse 模式并丢弃 `query`）；302 语义不变、仅换落点；同步改 `CustomerAuthControllerTest.java:97`、`SearchControllerTest.java:55,60`、`fabflix.http:45`。 |
| **顺序与交付** | 一个分支、一个 PR（`Closes #31`/`#32`/`#33`）；顺序 #31 → #32 → #33；每阶段跑相应测试；后端新增代码必须配测试（JaCoCo 85% 闸门）。 |

> **批准状态（2026-10-15 更新）**：D-33-1..D-33-4 为原已批准范围；**D-33-5/D-33-6/D-33-7 为本轮根据 oracle B3/B4/B5 新增的裁定**（其中 **D-33-7 为行为变更，已由主 agent 明确批准**；D-33-5 的 `local` profile 仅用于本地/演示 smoke；D-33-6 的失败守卫不改变默认构建行为）；N-33-1..N-33-10 与 Q-1..Q-9 已全部裁定（§10/§11）。**本批修订不改变 AC-33-1..AC-33-6**。

## 2. 验收条件逐条映射（issue 正文原文 → 本计划如何满足）

| # | issue 验收条件（原文） | 计划承载（提案） | 验证方式（计划） |
| --- | --- | --- | --- |
| AC-33-1 | Spring Boot serves the production React application | `-Pfrontend` profile 下 resources 插件把 `frontend/dist` 复制到 `target/classes/static`（D-33-1）；**dist 缺失时构建必须失败（D-33-6/B4）**；`mvn -Pfrontend clean package` 产出的 `target/fablix.jar` 内含静态资源 | 构建产物检查（jar 内含 `BOOT-INF/classes/static/index.html`、`assets/*`）+ 生产 smoke（`docker compose build app` + `curl /` 返回 `index.html`） |
| AC-33-2 | Docker Compose builds and starts the complete stack | `compose.yaml` 新增 `app` 服务（`build: .`、`depends_on: mysql healthy`、**`ports 8081:8081`**、`FABFLIX_DB_*`/`FABFLIX_BOOT_PORT` 注入、**`SPRING_PROFILES_ACTIVE=local`（D-33-5/B3）**）；新增 `Dockerfile`（多阶段、锁 tag）+ `.dockerignore`（D-33-1） | `docker compose build app` + `docker compose up -d` + `curl /api/boot/health` + `curl /`（生产 smoke，§7） |
| AC-33-3 | Direct navigation works for every React route | SPA fallback（D-33-2）：真实静态资源优先；`/api/**` 不拦；**判定排除 `api/`、`error`、带扩展名路径、真实存在资源（N-33-1）**；其余非文件路径返回 `index.html` | 对 **纯函数 `shouldFallback(path)` 的真值表单测** + 一条「未映射 `/api/boot/xxx` 不得返回 HTML」断言（不依赖 `@SpringBootTest`）+ 生产 smoke 逐个 `curl` 深链断言返回 HTML（§4.2/§6） |
| AC-33-4 | No active route references legacy frontend files | 删除 `src/main/webapp/**`（D-33-3）+ **两处重定向按 D-33-7/B5 改指向 SPA 路由** + 文档引用清理（行级清单见 §5.4） | 新守卫测试（目录不存在 **且** 若存在则无 `.html`/`.js`，N-33-3）+ `mvn verify` 全绿 + `grep` 源码/配置/文档无遗留引用 + `mvn verify` 含 #32 的前端步骤 |
| AC-33-5 | Full backend and frontend CI passes after legacy removal | `FrontendApiMigrationTest` 重写为退役守卫；`SpringBootOnlyArchitectureTest` 保持通过；**默认 `mvn verify` 不得依赖 `frontend/dist`/Node（C-33-10）** | **PR 上 CI 全绿**（直接证据） + 本地 `mvn --batch-mode verify` + #32 的前端命令（lint/test/coverage/build/e2e） |
| AC-33-6 | M2 regression checklist is complete | §7 的 checklist（可执行 + 证据列）：**已补三项证据**（PR CI 全绿、`git ls-files frontend/dist` 为空、干净 checkout 的 `npm ci` + `mvn -Pfrontend clean package` 可复现，N-33-7）；**生产 smoke 计划为实际执行并回填**，无法执行的项如实标注（D-33-4） | §7 表格的证据列 |

## 3. 范围（In scope）与 Non-goals（Out of scope）

### 3.1 In scope（已批准）

1. `pom.xml`：新增 `frontend` profile（resources 插件把 `frontend/dist` 复制到 `target/classes/static`）；**默认构建行为不变**（无 profile 时不复制、不依赖 `frontend/dist`）。
2. SPA fallback 实现（新增配置类/resolver，D-33-2）+ 针对性单测。
3. `Dockerfile`（多阶段：Node 22 构建 → Maven 构建/打包 → JRE 运行）+ `.dockerignore`。
4. `compose.yaml` 新增 `app` 服务（D-33-1）。
5. 删除 `src/main/webapp/**`（**21** 个跟踪文件，D-33-3）+ 重写 `FrontendApiMigrationTest`。
6. **两处 Java legacy 重定向按已批准行为变更处置**（D-33-7/B5）：`CustomerAuthController.java:68` → `/login`、`SearchController.java:47` → `/movies?mode=fulltext&<query>`；同步改 `CustomerAuthControllerTest.java:97`、`SearchControllerTest.java:55,60`、`fabflix.http:45`，并补覆盖。
7. 文档：`README.md`、`frontend/README.md`、`docs/architecture/frontend-migration-contracts.md`（legacy 引用 + 测试设施/CI 事实）；本计划 + issue 跟踪 + 实施日志。
8. M2 regression checklist（§7）+ 生产 smoke 的实际执行与结果回填。

### 3.2 Non-goals（明确排除）

| 排除项 | 理由 / 证据 |
| --- | --- |
| **提交 `frontend/dist` 到仓库** | D-33-1 已批准「不提交 `frontend/dist`」；`frontend/.gitignore:11` 已忽略，`git ls-files frontend/dist` = 0。 |
| **修改业务 API 契约 / DB schema / 存储过程** | 与 issue 目标无关；如需改属范围扩大。 |
| **改动顾客/员工业务流程的行为语义**（除 **已批准的 D-33-7 两处重定向落点变更**外） | 本 issue 只做生产接入与 legacy 退役；D-33-7 的 302 语义不变、仅换落点（已批准）。 |
| **多实例部署、Spring Session、外部 CDN/反向代理（nginx）** | 架构文档 §10 AD-2（单 JVM）；未批准。 |
| **Kubernetes / CI 部署流水线 / 镜像发布到 registry** | 未批准；issue 只要求 `docker compose`。 |
| **Playwright/覆盖率阈值/CI 前端步骤本身** | 属 **#32**；本 issue 只要求"删除 legacy 后 CI 仍全绿"。 |
| **员工/顾客功能补做** | 属 #31（及更早 issue）。 |
| **修改 JaCoCo 85% 闸门或 excludes 白名单** | 未批准。 |
| **引入 Spring Security / CSRF / JWT** | 架构文档 §10 AD-1。 |
| **保留 `src/main/webapp` 作为"参考副本"（如移到 docs/）** | D-33-3 已批准**删除整个目录**；保留副本需重新批准（§10 Q-4）。 |

## 4. 关键契约与实现要点

### 4.1 静态资源打包（D-33-1 + B4/D-33-6）

```
[本地]  cd frontend && npm ci && npm run build      → frontend/dist/{index.html, assets/*}
[本地]  mvn -Pfrontend clean package                → target/classes/static/{index.html, assets/*}
                                                     → target/fablix.jar（含 BOOT-INF/classes/static/**）
        （dist 缺失时 **必须构建失败**（D-33-6）：enforcer requireFilesExist / antrun <fail>，绑定在 process-resources 之前）
[Docker 多阶段]（镜像 tag 已锁，N-33-5）
  stage 1 (前端)   node:22-alpine   → npm ci && npm run build → /build/dist（结束加 test -f dist/index.html）
  stage 2 (构建)   maven:3.9-eclipse-temurin-11 → 先 COPY pom.xml + dependency:go-offline（缓存层）→ COPY 源码/dist → mvn -Pfrontend -DskipTests clean package
  stage 3 (运行)   eclipse-temurin:11-jre → COPY target/fablix.jar → ENTRYPOINT java -jar fablix.jar
[compose]  app 服务：build: .、depends_on mysql(service_healthy)、ports 8081:8081、env FABFLIX_DB_URL/USER/PASSWORD、FABFLIX_BOOT_PORT、SPRING_PROFILES_ACTIVE=local（B3/D-33-5）
```

**已定稿的实现细节（Q-2/N-33-5 裁定）**：镜像 tag 锁定为 **`node:22-alpine`、`maven:3.9-eclipse-temurin-11`、`eclipse-temurin:11-jre`**（不用 `latest`，与 Java 11 对齐）；Maven 阶段先 `COPY pom.xml` + `dependency:go-offline` 并缓存依赖；`.dockerignore` 至少含 `node_modules`、`frontend/node_modules`、`target`、`.git`、`.idea`、`.pi`、`docs`；Docker 内 `-DskipTests`（测试在 CI/本地 `verify` 跑）；`clean` 同时清掉 `target/classes/static` 的陈旧 hash 资产。

### 4.2 SPA fallback（D-33-2）

| 请求 | 期望 | 备注 |
| --- | --- | --- |
| `/` | `index.html`（`AppRoutes` 的 `index → /home` 重定向在浏览器内完成） | Spring Boot 静态资源默认欢迎页 |
| `/assets/*.js`、`/assets/*.css`、`/favicon.ico`（真实存在的文件） | 该文件（200，正确 Content-Type） | **真实静态资源优先** |
| `/home`、`/movies`、`/movies/:id`、`/stars/:id`、`/cart`、`/checkout`、`/confirmation`、`/login`、`/employee/login`、`/employee/dashboard`、`/playground` | `index.html`（200 + `text/html`） | 深链/刷新可用（AC-33-3） |
| `/api/boot/**`（含 `GET /api/boot/health`、未认证的 `/api/boot/cart`） | **绝不被 fallback 拦截**：仍走原有 filter/controller 语义（401 JSON / 200 JSON） | D-33-2 的硬性要求 |
| `/api/boot/不存在的端点` | **不得**变成 `index.html`（应为 404 JSON 或 Spring 默认错误响应） | 需专项断言 |
| `/error` | **不得**被 fallback 吞成 `index.html`（避免干扰 BasicErrorController） | N-33-1：`shouldFallback` 排除 `error` |
| `/assets/missing.js`、缺失的 `/favicon.svg`（带扩展名但不存存在） | **404**（**不得**以 `text/html` 返回缺失资源，否则产生 MIME/缓存问题） | N-33-1：`shouldFallback` 排除**带扩展名的路径** |
| 未知非 API 路径（如 `/missing-page`） | `index.html` → 前端渲染 `NotFoundPage` | `AppRoutes.jsx:54` |

- **实现形态（Q-3/N-33-2 已裁定）**：`WebMvcConfigurer.addResourceHandlers("/**")` + **自定义 `PathResourceResolver`**（“真实资源优先；缺失且判定为 SPA 路径 → `classpath:/static/index.html`”）；判定抽成**纯函数 `shouldFallback(path)`** 做真值表单测。
  - **实施定稿补充（详见 §13.2 第 4 条）**：配置类带 `@Order(Ordered.LOWEST_PRECEDENCE)`；资源链用 `.resourceChain(false).addResolver(new SpaResourceResolver())`（不启用无界资源缓存）；`shouldFallback` 对 `api`/`error` 前缀做**大小写归一**比较。
  - **真值表（N-33-1，写死）**：`api/**`、`error`、**含 `.` 扩展名的路径**、**真实存在的资源** → **不 fallback**；其余非 `/api` 路径 → fallback 到 `index.html`。
  - **依赖注册顺序（需写入代码注释/测试）**：资源 handler 注册顺序天然晚于 `@RequestMapping`，**controller 优先**；并加一条“**未映射 `/api/boot/xxx` 不得返回 HTML**”的断言。
- **约束**：项目用 `javax.servlet`（`SpringBootOnlyArchitectureTest` 禁止 `jakarta.servlet`）；Spring Boot 版本 2.7.18（`pom.xml:22`）。
- **测试形态（不依赖 `@SpringBootTest`，C-33-3）**：`shouldFallback(path)` 的**纯函数真值表单测**（上表四类）；另可选 `MockMvcBuilders.standaloneSetup` + 真实 `ResourceHttpRequestHandler` 的隔离集成测试。
- **可测性分工（N-33-8）**：纯函数真值表只能覆盖**判定逻辑**；“resource handler 注册生效”与“jar 内静态资源可被服务”**只有生产 smoke 能证明**（§6/§7）——不得声称单测已证明 AC-33-1/AC-33-3 的全部。

### 4.3 legacy 退役（D-33-3 + D-33-7）

| 项 | 计划 |
| --- | --- |
| 删除 | `src/main/webapp/**`（21 个跟踪文件，一次 `git rm -r src/main/webapp`） |
| 守卫测试重写（**N-33-3 已裁定**） | `FrontendApiMigrationTest`：**双保险** = ① `assertFalse(Files.exists(WEBAPP))`；② **若存在**则遍历断言无 `.html`/`.js`；**删除** `criticalPagesUseTheirSpringBootEndpoints`（`FrontendApiMigrationTest.java:24-35`，其断言对象已消失）；**不得**保留 `Files.walk` 缺失目录的旧写法（`FrontendApiMigrationTest.java:16-21` 会招 `NoSuchFileException`）；新测试仍必须在**无网络/无 `@SpringBootTest` 下运行** |
| 保持通过 | `SpringBootOnlyArchitectureTest`（不受影响） |
| **两处重定向（D-33-7，行为变更已批准）** | ① `CustomerAuthController.java:68`：`sendRedirect("/login.html")` → **`/login`**；② `SearchController.java:47`：302 `/movieList.html?" + query` → **`/movies?mode=fulltext&" + query`**（**必须带 `mode=fulltext`**）；302 语义不变；同步改 `CustomerAuthControllerTest.java:97`（`redirectedUrl("/login")`）与 `SearchControllerTest.java:55,60`（`Location: /movies?mode=fulltext&...`），并更新 `fabflix.http:45` |
| 不保留副本 | **Q-4 已裁定：不保留**（git 历史可回溯）；如需保留须重新批准 |

### 4.4 生产 smoke（D-33-4 + B3/D-33-5）

> **镜像可拉取性**：oracle 所在环境 `docker manifest inspect` 对三个镜像全部失败（无网络）；**主 agent 已在本机用 `docker manifest inspect` 验证 `node:22-alpine`、`maven:3.9-eclipse-temurin-11`、`eclipse-temurin:11-jre` 均可拉取**。旧稿“镜像可用性未证实”已解除；但 worker 仍需在实际环境验证 `docker compose build app`，失败时如实标注“未执行”。

用 `docker compose` 起完整栈后（或本地 `java -jar`），至少验证：
① `GET /api/boot/health` → `{"status":"UP",...}`；
② `GET /` → `index.html`（含 `<div id="root">`）；
③ 每个 SPA 深链 → 200 + HTML（AC-33-3）；
④ 未认证 `GET /api/boot/cart` → 401 JSON（证明 `/api/**` 未被 fallback 吞掉）；
⑤ 顾客与员工关键 API 用 `fabflix.http` 的手动请求（或等效 curl）走通（登录 → session → metadata / cart）。

- **B3/D-33-5 的直接影响**：compose 的 `app` 启用 `SPRING_PROFILES_ACTIVE=local` → 顾客登录走 `LocalRecaptchaVerifier`，**才可能让 ⑤（顾客登录 → cart）在容器内跑通**；若仍失败（例如本地演示绕过被禁止），则如实标注“未执行/失败”并回主 agent，**不得**用“服务已启动”代替全流程证据。
- **Vite 变量陷阱（写入 README 与计划）**：`VITE_RECAPTCHA_SITE_KEY` 在 **构建期**被 Vite 内联 → **运行期注入无效**；生产部署必须构建前设好。
- **CI 不跑完整 compose**（Q-8 已裁定）：生产 smoke 作为本地/发布前验证并记录（§7）。

## 5. 拟修改 / 新增文件清单（已批准；最终以 worker 实际 diff 为准）

> 本清单已随 §10/§11 的裁定定稿（主 agent 2026-10-15 批准）；删除项与行为变更项均为**明确批准**。

### 5.1 构建与部署

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `pom.xml` | 改 | 新增 `<profiles><profile><id>frontend</id>…</profile></profiles>`：`maven-resources-plugin` 把 `frontend/dist` 复制到 `${project.build.outputDirectory}/static`；**并加 dist 缺失的失败守卫（D-33-6/B4）**：`maven-enforcer-plugin` 的 `requireFilesExist`（`${project.basedir}/frontend/dist/index.html`）或 `maven-antrun-plugin` `<fail>`，绑定在 `process-resources` **之前**；**默认无 profile 时行为不变**（`SpringBootOnlyArchitectureTest` 只校验 `jar`+`repackage`+无 `jakarta.servlet-api`，不受影响） |
| `Dockerfile` | 新 | 多阶段：**`node:22-alpine`** 前端构建（结束 `test -f dist/index.html`）→ **`maven:3.9-eclipse-temurin-11`**（先 `COPY pom.xml` + `dependency:go-offline` 缓存层）→ **`eclipse-temurin:11-jre`** 运行阶段（D-33-1 + N-33-5） |
| `.dockerignore` | 新 | 至少排除 `node_modules`、`frontend/node_modules`、`target`、`.git`、`.idea`、`.pi`、`docs`（缩短构建上下文，N-33-5） |
| `compose.yaml` | 改 | 新增 `app` 服务：`build: .`、`depends_on: { mysql: { condition: service_healthy } }`、**`ports: ["8081:8081"]`**、env `FABFLIX_DB_URL=jdbc:mysql://mysql:3306/moviedb?useUnicode=true&characterEncoding=UTF-8`（容器内**不能**用 `localhost:3307`）/`FABFLIX_DB_USER`/`FABFLIX_DB_PASSWORD`（从 `.env`）/`FABFLIX_BOOT_PORT=8081`、**`SPRING_PROFILES_ACTIVE=local`（B3/D-33-5）**；**不改** `mysql` 服务现状 |
| `frontend/vite.config.js` | **不改（Q-5 已裁定）** | 保持 `base: '/'`（根路径部署，匹配 compose）；将来子路径部署需重新裁定 |

### 5.2 应用代码

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `src/main/java/com/fabflix/boot/web/SpaFallbackConfig.java`（路径/名称仍为**提案**） | 新 | SPA fallback 配置（D-33-2）；实现形态已定（Q-3/N-33-2：`WebMvcConfigurer.addResourceHandlers("/**")` + 自定义 `PathResourceResolver`）并**抽成纯函数 `shouldFallback(path)`** 供单测 |
| `src/main/java/com/fabflix/boot/auth/CustomerAuthController.java` | 改（**D-33-7 已批准**） | `/login.html` → `/login`（第 68 行）；302 语义不变 |
| `src/main/java/com/fabflix/boot/search/SearchController.java` | 改（**D-33-7 已批准**） | `/movieList.html?<query>` → `/movies?mode=fulltext&<query>`（第 47 行），**必须带 `mode=fulltext`** |
| `src/main/webapp/**` | **删（21 文件）** | D-33-3 已批准的破坏性动作 |
| `其他 src/main/java/**` | 不改 | 除上述两处重定向外无 legacy 引用（已 grep `.html`：仅这两处） |

### 5.3 测试

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java` | 重写 | 改为 legacy 退役守卫（§4.3，**双保险**，N-33-3） |
| `src/test/java/com/fabflix/boot/SpaFallbackTest.java`（路径**提案**） | 新 | fallback 判定真值表单测（`api/**`、`error`、带扩展名路径、真实文件、SPA 路径、未知路径）；+ **“未映射 `/api/boot/xxx` 不得返回 HTML”**断言；不依赖 `@SpringBootTest` |
| `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java` | **不改** | 必须继续通过 |
| `src/test/java/com/fabflix/boot/auth/CustomerAuthControllerTest.java` | 改（**D-33-7 已批准**） | 第 97 行 `redirectedUrl("/login.html")` → `redirectedUrl("/login")` |
| `src/test/java/com/fabflix/boot/search/SearchControllerTest.java` | 改（**D-33-7 已批准**） | 第 55、60 行的 `Location` 改为 `/movies?mode=fulltext&...` |
| 前端 | 不改（属 #32） | e2e 生产形态验证由 smoke 承担（§4.4） |

**命令（必须由 worker 实际运行并报告）**：
`mvn --batch-mode --no-transfer-progress verify`（默认 profile，与 CI 同命令）；`mvn -Pfrontend --batch-mode -DskipTests clean package`（生产打包；**clean 为必须**，D-33-6）；dist 缺失时的**失败守卫验证**（删除/改名 `frontend/dist` 后构建应失败）；
`docker compose build app` + `docker compose up -d` + 生产 smoke（§4.4，**计划为实际执行并回填**）；#32 引入的前端命令（`npm ci`/`lint`/`test`/`test:coverage`/`build`/`test:e2e`）。

### 5.4 文档

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `README.md` | 改 | 删除/改写 legacy 引用（**行级清单（N-33-4）**：`:3`、`:15`、`:67`、`:99`）——其中 `:67` 的 `http://localhost:8081/login.html` 与 `:99` 的“remains the supported application”必须改写为 React 为唯一前端；补生产启动（`docker compose build app`、`docker compose up`）、本地流程（`npm ci && npm run build` + `mvn -Pfrontend clean package`）、Node 22 要求；**并写明生产需 `RECAPTCHA_SECRET_KEY` + 构建期 `VITE_RECAPTCHA_SITE_KEY`（B3/D-33-5）** |
| `frontend/README.md` | 改 | **行级清单（N-33-4）**：`:3`、`:56`（另复核 `:57-67`）的 legacy 语句；补 `test:coverage`/`test:e2e`（#32）与生产构建说明 |
| `docs/architecture/frontend-migration-contracts.md` | 改 | §1 迁移进度（M2 完成）、§2 legacy 保留策略（改为“已退役”）、§7 测试设施/CI 事实（**归 #32 更新**）、§8 架构守卫（如新增 fallback 类需登记） |
| `docs/plans/issue-33-...md`（本文件） | — | 计划 + M2 regression checklist（§7） |
| `docs/issues/issue-33-...md` | 新 | issue 跟踪（含"已批准删除整个 legacy 目录"的记录） |
| `docs/logs/...`（实施后） | 新 | 工作流日志（本任务阶段不创建） |

## 6. 验收与测试计划

| AC | 后端/构建证据 | 命令/操作 |
| --- | --- | --- |
| AC-33-1 | `mvn -Pfrontend clean package` 产物含 `BOOT-INF/classes/static/index.html`；**dist 缺失时构建失败（D-33-6）**；`java -jar target/fablix.jar` 后 `GET /` 返回 HTML | 构建产物检查 + `curl` |
| AC-33-2 | `docker compose build app` 成功；`docker compose up -d` 后 `mysql` healthy、`app` 起来；`GET /api/boot/health` = UP | `docker compose` 命令 |
| AC-33-3 | `SpaFallbackTest`（真值表）+ 深链 `curl` 全部返回 `text/html` | `mvn verify` + `curl` 深链清单（§4.1/§4.2） |
| AC-33-4 | 新守卫测试（双保险）；全仓库 `grep` legacy 引用为空（源码/配置/文档）；两处 Java 重定向按 D-33-7 改指向 | `mvn verify` + `grep` + `git ls-files src/main/webapp` 为空 |
| AC-33-5 | `mvn --batch-mode verify`（含 JaCoCo 85% 闸门）全绿；#32 的前端命令全绿；**PR 上 CI 全绿**（直接证据） | 本地命令 + CI |
| AC-33-6 | §7 checklist 全部条目有证据或明确标注"无法执行/未执行" | §7 表格 |

**未纳入自动化（如实声明）**：`docker compose` 生产 smoke 需要本机 Docker 与网络（拉取镜像）；**主 agent 已用 `docker manifest inspect` 验证三个镜像可拉取**，worker 应在有网环境实际执行；若执行环境不具备，**必须如实标注“未执行”**，不得声称通过（D-33-4 明确要求）。
**证据分工（N-33-8）**：`shouldFallback` 真值表只证明**判定逻辑**；“resource handler 注册”“jar 内静态资源可被服务”“深链返回 HTML”**由生产 smoke 证明**；“两处重定向落点”由 `CustomerAuthControllerTest`/`SearchControllerTest` 证明。不得把其中一层当作另一层的证据。
**JaCoCo 额度提醒（共享）**：当前后端 947/1057 = 0.89593，`≥0.85` 允许**新增未覆盖行 ≤ ~57 行**；#31 的 `logout` 与 #33 的 fallback 配置/判定类共享该额度 → 新增/修改的后端类**每个至少一条测试**（C-33-4）。

## 7. M2 Regression Checklist（已定稿，D-33-4；**证据列已由主 agent 终验回填，2026-10-15**）

> 定位：M2 收尾的可执行清单。**§7 各“结果”列已由主 agent 终验（2026-10-15）逐条回填，并已随 PR #42（head `fb65eda`）的 CI 首跑回填 M2-BE-6**；未执行/部分验证的条目已明确标注（不得当作已通过）。
> 记录方式：每条填「命令/操作 + 实际输出摘要（如 `Tests run: N`、HTTP 状态、文件存在性）+ 执行日期」；标注来源为「主 agent 实测」或「docs-writer 只读复核」。
> **Docker 镜像可拉取性已由主 agent 在本机用 `docker manifest inspect` 验证通过**（`node:22-alpine`/`maven:3.9-eclipse-temurin-11`/`eclipse-temurin:11-jre`）→ **7.3 的生产 smoke 已实际执行并回填**。

### 7.1 后端与构建

| # | 检查项（可执行） | 预期 | 结果（**主 agent 终验后已回填，2026-10-15**） |
| --- | --- | --- | --- |
| M2-BE-1 | `mvn --batch-mode --no-transfer-progress verify`（默认 profile） | BUILD SUCCESS；测试全绿；`All coverage checks have been met`（BUNDLE LINE ≥ 0.85） | ✅ **通过**：BUILD SUCCESS；`Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`；`All coverage checks have been met.`；BUNDLE LINE **968 covered / 110 missed / 1078 total = 0.8980**（闸门 0.85）。新增/改动类均为 **0 行未覆盖**：`SpaFallbackConfig` 13/13、`SpaFallbackConfig$SpaResourceResolver` 5/5、`EmployeeController` 13/13、`CustomerAuthController` 24/24、`SearchController` 13/13（主 agent 实测；docs-writer 由 `target/surefire-reports/*` 与 `target/site/jacoco/jacoco.csv` 只读复核一致） |
| M2-BE-2 | `mvn -Pfrontend --batch-mode -DskipTests clean package` 后检查 `target/fablix.jar` 内 `BOOT-INF/classes/static/index.html` 与 `assets/**` 存在 | 存在 | ✅ **通过**：jar 内 **6 个** `BOOT-INF/classes/static` 条目 = `static/`、`static/assets/`、`assets/index-BINlLyG3.css`、`assets/index-CbFV0THv.js`、`favicon.svg`、`index.html`（前两项为目录条目）（主 agent 实测；docs-writer 只读 `unzip -l target/fablix.jar` 复核一致） |
| M2-BE-2b（**N-33-7 新增**） | **dist 缺失时的失败守卫**：暂移 `frontend/dist` 后跑 `mvn -Pfrontend -DskipTests package` | **构建失败**（D-33-6/B4；不得静默成功） | ✅ **通过（负向验证）**：临时移走 `frontend/dist` → **BUILD FAILURE**，失败来自 enforcer `require-frontend-dist`，报错信息含 `frontend/dist/index.html is missing`（主 agent 实测） |
| M2-BE-3 | 删除 legacy 后重跑 `mvn --batch-mode verify` | 全绿（含重写后的守卫测试） | ✅ **通过**：21 个 legacy 文件已删除（已随提交 `092147a` 进入 git），`mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS，**127 tests** 全绿（含重写后的 `FrontendApiMigrationTest` 退役守卫与 `SpaFallbackTest`）（主 agent 实测） |
| M2-BE-4 | `grep -rn "webapp\|\.html" src/main/java src/main/resources pom.xml compose.yaml Dockerfile` | 无 legacy 文件引用（仅留已批准的 D-33-7 新落点） | ✅ **通过**（docs-writer 只读 grep）：`src/main/java`、`src/main/resources`、`pom.xml`、`compose.yaml`、`Dockerfile`、`fabflix.http`、`.github/workflows/ci.yml` 均无 `login.html`/`movieList.html`/`webapp`；两处重定向已用新落点（`/login`、`/movies?mode=fulltext&`）。**备注**：命中项只出现在文档（如本计划的历史段落与架构文档的历史说明）中 |
| M2-BE-5（**N-33-7 新增**） | **干净 checkout 的可复现性**：`npm ci` + `mvn -Pfrontend clean package` | 从干净树可重现成功构建（无隐式前置步骤） | ⚠️ **部分验证**：Node 22 容器（依赖安装 → `npm run build`）与 `mvn -Pfrontend -DskipTests clean package`（含 dist 存在/缺失两态）分别跑通；但**未单独执行**「干净 checkout 单次端到端 `npm ci` + `mvn -Pfrontend clean package`」的专门复现 → 记为**未完全验证**，PR CI 从干净 checkout 跑 `npm ci` 与构建后可作为间接补强（M2-BE-6） |
| M2-BE-6（**N-33-7 新增**） | **PR 上 CI 全绿**（后端 + #32 的独立 `frontend` job） | 两个 job 均 SUCCESS | ✅ **通过（已回填，2026-10-15）**：PR **#42**（head `fb65eda`）上 `Build and test (Java 11)` → **SUCCESS**；`Frontend (lint, test, coverage, build, e2e)` → **SUCCESS**（新 `frontend` job 的首次运行）。来源：主 agent 核验的 GitHub CI 运行结果（docs-writer 未直接访问 GitHub）。**PR 仍为 OPEN、未合并**（见 §13.4） |
| M2-BE-7（**N-33-7 新增**） | `git ls-files frontend/dist` | **为空**（dist 未提交，C-33-1） | ✅ **通过**：`git ls-files frontend/dist` → 0 行（`frontend/.gitignore` 忽略 `dist`）（docs-writer 只读执行） |

### 7.2 前端（命令由 #32 引入）

| # | 检查项 | 预期 | 结果（主 agent 终验后已回填，2026-10-15） |
| --- | --- | --- | --- |
| M2-FE-1 | `cd frontend && npm ci` | 成功（Node 22） | ⚠️ **间接受验证**：Node 22 容器（`node:22-slim`，v22.23.3）内依赖安装成功并连续两次跑通 lint/test/coverage/build；**未单独留存该步骤的输出摘要**（CI 的 `npm ci` 步骤见 M2-BE-6） |
| M2-FE-2 | `npm run lint` | exit 0，无 warning | ✅ **通过**：本地 Node 24 干净；Node 22 容器连续两次 **0** issue |
| M2-FE-3 | `npm test` | 全绿 | ✅ **通过**：**30 files / 195 passed**（本地 Node 24；Node 22 容器连续两次结果一致） |
| M2-FE-4 | `npm run test:coverage` | 达到 #32 裁定的阈值（≥75% 且 ≥基线-5pp） | ✅ **通过**：**Lines 92.35% (846/916)**，阈值 `lines: 88`（低 4.35pp，位于 2–5pp 区间且 ≥75%）；Statements 90.05%、Branches 82.16%、Functions 91.17%（本轮未在容器内重跑）；**阈值仅对 lines 生效** |
| M2-FE-5 | `npm run build` | 生成 `frontend/dist/` | ✅ **通过**：本地与 Node 22 容器均 build 成功，产出 `frontend/dist/index.html` + `assets/*`（由 `-Pfrontend` 打包消费） |
| M2-FE-6 | `npm run test:e2e`（Playwright，chromium，mock API） | 四条 smoke 全绿 | ✅ **通过**：**12 passed**（4 个 spec 文件 / 6 个用例 × 2 个 viewport 360·1440），本地与 Node 22 容器（容器内先 `npx playwright install --with-deps chromium`）均一致 |

### 7.3 生产形态 smoke（需要 Docker）

| # | 检查项 | 预期 | 结果（主 agent 终验后已回填，2026-10-15） |
| --- | --- | --- | --- |
| M2-PROD-1 | `docker compose build app` | 构建成功（三个镜像已由主 agent 验证可拉取） | ✅ **通过**：`docker compose build app` 成功（提交版 `compose.yaml`：`8081:8081`）（主 agent 亲自执行） |
| M2-PROD-2 | `docker compose up -d` → `docker compose ps` | `mysql` healthy、`app` 运行 | ✅ **通过（命令略有差异）**：实际用 `docker compose up -d --no-deps app`（mysql 容器已在运行，未受本次 smoke 影响）→ `app` 启动正常；另：运行前，主 agent 已获用户许可停掉遗留的 dev server |
| M2-PROD-3 | `curl -i http://localhost:8081/api/boot/health` | 200 `{"status":"UP",...}` | ✅ **通过**：health → **200** |
| M2-PROD-4 | `curl -i http://localhost:8081/` | 200 `text/html`（含 `id="root"`） | ✅ **通过**：`/` 返回含 `id="root"` 的 HTML |
| M2-PROD-5 | 逐个深链 `curl -i`：`/home`、`/movies`、`/movies/<id>`、`/cart`、`/checkout`、`/confirmation`、`/login`、`/employee/login`、`/employee/dashboard`、`/playground` | 全部 200 `text/html` | ✅ **通过**：10 条深链（含 `/movies/tt123` 形式）**全部 200 `text/html`**（AC-33-3 的浏览器级补强证据） |
| M2-PROD-6 | `curl -i http://localhost:8081/api/boot/cart`（未认证） | **401 JSON**（证明 `/api/**` 未被 fallback 吞掉） | ✅ **通过（含两项额外断言）**：未认证 `/api/boot/cart` → **401 JSON**；**`/API/boot/cart`（大小写变体）→ 404 `application/json`**（大小写归一修复生效，未被 fallback 吞成 HTML）；`/assets/missing.js` → **404** |
| M2-PROD-7 | 用 `fabflix.http`（或等效 curl）走通顾客：登录 → session → cart；员工：登录 → `dashboard/session` → `action=metadata` | 与契约一致。**按 B3/D-33-5 重写**：`app` 启用 `SPRING_PROFILES_ACTIVE=local` → 顾客登录走 `LocalRecaptchaVerifier`，**否则会得到 403 `Recaptcha Not Passed`**；README 必须写明生产需 `RECAPTCHA_SECRET_KEY` + **构建期** `VITE_RECAPTCHA_SITE_KEY` | ✅ **通过**：顾客登录（`local` profile，dev 种子账号）→ session/customer **200** + cart **`[]`**；员工登录 → dashboard session **200** + metadata `status: success`（用等效 curl 而非 `.http` 文件执行） |
| M2-PROD-8 | `docker compose down`（保留 volume） | 干净退出 | ✅ **通过（更保守的命令）**：实际执行 `docker compose rm -sf app` —— **`mysql` 容器与 `fabflix-mysql-data` 数据卷均未动**；未执行 `down`（避免误停/误删数据） |

### 7.4 文档与仓库卫生

| # | 检查项 | 预期 | 结果（终验后已回填，2026-10-15） |
| --- | --- | --- | --- |
| M2-DOC-1 | `grep -rn "login.html\|movieList.html\|webapp\|legacy" README.md frontend/README.md docs/architecture/frontend-migration-contracts.md` | 仅剩说明"已退役"的表述，无"仍然可用"的误导 | ✅ **通过**（docs-writer 只读 grep）：`README.md`/`frontend/README.md` 仅剩「已退役（has been retired）」表述，无「仍然可用」措辞；架构文档 §2 已改写为「已退役（本 issue）」并把 #30 时代的保留策略标为历史（同时新增 §11 SPA fallback 与 §7 测试设施/CI 更新） |
| M2-DOC-2 | `git ls-files src/main/webapp` | 空（目录已删除） | ✅ **通过（已回填，2026-10-15）**：docs-writer 本轮只读执行 `git ls-files src/main/webapp` → **0 行**；21 个文件已随实现提交 **`092147a`** 从 git 移除（`git show --stat 092147a -- src/main/webapp` = 21 files changed, 2057 deletions） |
| M2-DOC-4（**N-33-7/N-33-4**） | 核验 `README.md:3/:15/:67/:99` 与 `frontend/README.md:3/:56/:57-67` 已改写，无遗留 `login.html`/`movieList.html` 旧路径 | 无遗留引用 | ✅ **通过**：`README.md:3`（React 为唯一前端）、`:15`（架构图改为「React + Vite single-page app」）、原 `:67` 的 `/login.html` 段落改写为「Docker Compose 全栈 / 本地开发 / 生产构建」三节、原 `:99` 改写为「React is the only supported frontend…」；`frontend/README.md:3` 与生产构建段落已改写；两文件中已无 `login.html`/`movieList.html` |
| M2-DOC-5（**N-33-10**） | 核验 `fabflix.http` 的 jump-back/legacy 示例已更新（不再请求 `/movieList.html`） | 示例与新落点一致 | ✅ **通过**：`fabflix.http` 的 jump-back 注释更新为「应返回 302，Location 指向 `/movies?mode=fulltext&...`」 |
| M2-DOC-3 | `git status --short` | 无未预期文件（`frontend/dist` 仍被忽略） | ✅ **通过（已回填，2026-10-15）**：提交（`092147a`/`fb65eda`）后 `git status --short --untracked-files=all` **仅 6 项**，均为未跟踪的 `.idea/*`（本机 IDE 文件，**非本次变更**）；`frontend/dist`/`coverage`/`test-results`/`playwright-report` 均按 `.gitignore` 忽略。`.idea/*` 是否纳管仍待主 agent 决定 |

## 8. 依赖

| 依赖 | 状态 | 证据/说明 |
| --- | --- | --- |
| 所有 React 流程已迁移（顾客 + 员工） | **#31 完成 + 更早 issue** | issue 正文 "Final M2 issue; depends on all feature migration and quality issues" |
| 前端测试/覆盖率/CI/e2e 设施 | **#32 完成** | AC-33-5 要求"删除 legacy 后前后端 CI 全绿" |
| Docker（本机与 CI 环境） | 需可用；**镜像可拉取性已由主 agent 用 `docker manifest inspect` 验证** | 现状仓库无 Dockerfile；生产 smoke 依赖 Docker（worker 仍需实际验证 `docker compose build app`） |
| Node 22 LTS | #32 固定（CI/Docker/engines） | D-32-3 |
| `frontend/dist` 构建能力 | 已具备（`npm run build`） | `frontend/package.json:8`；`dist` 已被忽略 |

## 9. 约束

| # | 约束 | 来源 |
| --- | --- | --- |
| C-33-1 | **不提交 `frontend/dist`**（构建期生成） | D-33-1；`frontend/.gitignore:11` |
| C-33-2 | **`/api/**` 绝不被 SPA fallback 拦截**；真实静态资源优先 | D-33-2 |
| C-33-3 | **无 `@SpringBootTest`**（全仓库 0 命中）；fallback 测试用隔离单测 | scout A4；docs-writer 复核 |
| C-33-4 | **JaCoCo 行覆盖率 ≥0.85（BUNDLE/LINE）**：新增 `SpaFallbackConfig` 等后端类必须配测试 | `pom.xml:130-171`；`.github/workflows/ci.yml:36` |
| C-33-5 | **`javax.servlet`（非 `jakarta.servlet`）**；`pom.xml` 保持 `jar` + `repackage` + 无 `jakarta.servlet-api` | `SpringBootOnlyArchitectureTest.java:24-31` |
| C-33-6 | Spring Boot **2.7.18**（`pom.xml:22`）→ fallback 实现已定：**`WebMvcConfigurer#addResourceHandlers` + 自定义 `PathResourceResolver`**（真实资源优先；缺失且判定为 SPA 路径 → `classpath:/static/index.html`），判定**抽成纯函数 `shouldFallback(path)`** | `pom.xml`；Q-3/N-33-2 |
| C-33-7 | **mysql 服务现状不改**（端口 `3307:3306`、初始化脚本、healthcheck）；`app` 服务容器内必须用 `mysql:3306` 而非 `localhost:3307` | `compose.yaml:1-27`；`application.properties:3` |
| C-33-8 | 删除 legacy 是**已批准的破坏性动作**，但必须与 `FrontendApiMigrationTest` 同步（双保险守卫），且删除后不得留下悬空引用（两处重定向按 D-33-7 改指向） | D-33-3/D-33-7 |
| C-33-9 | 文档中的 legacy 引用必须同步清理（README / frontend README / 架构文档） | D-33-3 |
| C-33-10 | **CI 的后端步骤不得弱化**；默认 `mvn verify` 不得因为 `frontend` profile 而依赖 `frontend/dist` | `.github/workflows/ci.yml:36`；D-33-1 |
| C-33-11 | 单实例部署假设（AD-2）；不引入 Spring Session / 反向代理 | 架构文档 §10 AD-2 |
| C-33-12 | **`-Pfrontend` 在 `frontend/dist/index.html` 缺失时必须构建失败**（enforcer `requireFilesExist` 或 antrun fail，绑定在 `process-resources` 之前）；Docker/本地统一 `mvn -Pfrontend clean package` | D-33-6/oracle B4 |
| C-33-13 | **compose `app` 显式 `SPRING_PROFILES_ACTIVE=local`（仅本地/演示）**；README 必须写明生产需 `RECAPTCHA_SECRET_KEY` + **构建期** `VITE_RECAPTCHA_SITE_KEY`（运行期注入无效） | D-33-5/oracle B3；`application-local.properties` |
| C-33-14 | **jump-back 302 目标必须带 `mode=fulltext`**（`/movies?mode=fulltext&<query>`）——否则 React 列表页落到 browse 模式并丢弃 `query`；两处重定向的 302 语义不变 | D-33-7/oracle B5；`SearchController.java:37` |
| C-33-15 | **fallback 排除项写入代码并单测**：`api/**`、`error`、带 `.` 扩展名的路径、真实存在的资源；另断言“未映射 `/api/boot/xxx` 不得返回 HTML” | N-33-1/N-33-2；D-33-2 |
| C-33-16 | **Docker 镜像固定 tag**：`node:22-alpine`、`maven:3.9-eclipse-temurin-11`、`eclipse-temurin:11-jre`；`.dockerignore` 含 `node_modules`/`frontend/node_modules`/`target`/`.git`/`.idea`/`.pi`/`docs`；Maven 依赖缓存层（先 `COPY pom.xml` + `dependency:go-offline`） | N-33-5 |

## 10. 已裁定问题（原未决问题 Q-1..Q-9；主 agent，2026-10-15）

> **本节全部问题已裁定**（依据 oracle 报告 §3 与 §5，含新增的 B3/B4/B5 三行）；逐条回填见 §11。

| # | 问题 | 主 agent 裁定（2026-10-15，全部接受 oracle 建议） | 实施要点（已写入节号） |
| --- | --- | --- | --- |
| **Q-1（B5）** | **两处 legacy 重定向如何处置**（`CustomerAuthController.java:68`、`SearchController.java:47`） | **纳入 #33；改指向 SPA 路由**：`/login.html` → **`/login`**；`/movieList.html?<query>` → **`/movies?mode=fulltext&<query>`**（**必须带 `mode=fulltext`**）；同步改 `CustomerAuthControllerTest.java:97`、`SearchControllerTest.java:55,60`、`fabflix.http:45`。**行为变更，已由主 agent 明确批准**（302 语义不变、仅换落点） | §1.3 D-33-7/§2（AC-33-4）/§4.3/§5.2/§5.3/§9 C-33-14 |
| Q-2 | **Maven/Docker 镜像与插件细节** | **锁 tag**：`node:22-alpine`、`maven:3.9-eclipse-temurin-11`、`eclipse-temurin:11-jre`；Maven 先 `COPY pom.xml` + `dependency:go-offline` 缓存层；Docker 内 `-DskipTests`；`.dockerignore` 含 `node_modules`/`frontend/node_modules`/`target`/`.git`/`.idea`/`.pi`/`docs` | §4.1/§5.1/§9 C-33-16 |
| Q-3 | **SPA fallback 实现形态** | **`WebMvcConfigurer.addResourceHandlers("/**")` + 自定义 `PathResourceResolver`**；判定**抽成纯函数 `shouldFallback(path)`** 做真值表单测；保留默认构建不依赖 dist/Node；并加“未映射 `/api/boot/xxx` 不得返回 HTML”断言 | §4.2/§5.2/§5.3/§9 C-33-15 |
| Q-4 | 是否保留 legacy 副本 | **不保留**（git 历史可回溯）；如需保留须重新批准 | §4.3 |
| Q-5 | `vite.config.js` 是否设 `base`/`build.outDir` | **不改**（保持 `base: '/'`，根路径部署匹配 compose） | §5.1 |
| Q-6 | M2 checklist 放本计划内还是独立文档 | **放本计划 §7**（已可执行）；后续如需复用再抽独立文档 | §7 |
| Q-7 | 新守卫测试的断言形态 | **双保险**：`assertFalse(Files.exists(WEBAPP))` **且**（若存在）遍历断言无 `.html`/`.js`；**删除** `criticalPagesUseTheirSpringBootEndpoints`；**不得**保留 `Files.walk` 缺失目录的旧写法 | §4.3/§5.3 |
| Q-8 | 生产 smoke 是否在 CI 中执行 | **CI 不跑完整 compose**（#32 已定义 CI 门）；生产 smoke 作为本地/发布前验证并记录证据 | §4.4/§7.3 |
| Q-9 | `app` 服务是否显式注入 `FABFLIX_DB_URL` | **显式注入容器内 URL** `jdbc:mysql://mysql:3306/moviedb?useUnicode=true&characterEncoding=UTF-8`（避免 docker 网络中误用 localhost） | §5.1 |
| **B3（新增裁定 D-33-5）** | 容器内顾客登录的 reCAPTCHA/profile 策略 | **方案 (a)：`app` 显式 `SPRING_PROFILES_ACTIVE=local`**（仅本地/演示 smoke）；README 必须写明生产需 `RECAPTCHA_SECRET_KEY` + **构建期** `VITE_RECAPTCHA_SITE_KEY`（运行期注入无效）；M2-PROD-7 按此重写 | §1.2/§1.3 D-33-5/§4.4/§7.3/§9 C-33-13 |
| **B4（新增裁定 D-33-6）** | dist 缺失时构建静默成功 | **必须构建失败**（enforcer `requireFilesExist` 或 antrun fail，绑定在 `process-resources` 之前）；统一 `mvn -Pfrontend clean package`；Dockerfile 加 `test -f dist/index.html` | §1.2/§1.3 D-33-6/§4.1/§5.1/§7.1 M2-BE-2b/§9 C-33-12 |

## 11. Oracle 建议与主 agent 决定（已回填）

> 依据：oracle 只读审查报告 `.pi/artifacts/oracle-issues-31-32-33-plans.md`（#33 章节，含 B3/B4/B5 与 N-33-1..N-33-10）+ 主 agent 裁定（2026-10-15）。
> oracle 结论：「#33：**需修订后可批准**——B3（reCAPTCHA/profile）、B4（dist 缺失静默成功）、B5（Q-1 重定向 + `mode=fulltext`）必须先闭合；B3/B5 需要用户或主 agent 明确裁定」→ 三项已全部闭合并回填。

| 来源 | oracle 建议（摘要） | 主 agent 裁定 | 已写入本文档 |
| --- | --- | --- | --- |
| **B3** | 容器内顾客登录会因 reCAPTCHA 失败（计划完全未提），M2-PROD-7 会红；选项 (a) `local` profile 演示 or (b) 密钥 + 构建期 `VITE_RECAPTCHA_SITE_KEY` | **接受方案 (a)** | §1.2/§1.3 D-33-5/§4.4/§7.3 M2-PROD-7/§9 C-33-13/§10 |
| **B4** | `-Pfrontend` 复制在 dist 缺失时静默成功，打出不含 UI 的 jar | **接受（必须）** | §1.2/§1.3 D-33-6/§4.1/§5.1/§5.3 命令段/§7.1 M2-BE-2b/§9 C-33-12 |
| **B5** | Q-1 必须裁定，且 jump-back 目标需要 `mode=fulltext`（`lastMovieQuery` 只由 fulltext 写入） | **接受：`/login` 与 `/movies?mode=fulltext&<query>`，同步改两个测试与 `fabflix.http`（行为变更，已批准）** | §1.3 D-33-7/§2 AC-33-4/§4.3/§5.2/§5.3/§7.4 M2-DOC-5/§9 C-33-14/§10 Q-1 |
| N-33-1 | fallback 排除 `api/`、`error`、带扩展名路径与真实存在的资源（真值表） | **接受** | §4.2/§9 C-33-15 |
| N-33-2 | fallback 用 `WebMvcConfigurer` + 自定义 `PathResourceResolver`；判定抽纯函数 `shouldFallback(path)`；保留默认构建不依赖 dist/Node；加“未映射 `/api/boot/xxx` 不得返回 HTML”断言 | **接受** | §4.2/§5.2/§5.3/§9 C-33-15 |
| N-33-3 | 守卫测试双保险（目录不存在 + 若存在则无 `.html`/`.js`）；删除 `criticalPagesUseTheirSpringBootEndpoints` | **接受** | §4.3/§5.3 |
| N-33-4 | 文档行级清单（README `:3/:15/:67/:99`、`frontend/README.md :3/:56/:57-67`、架构文档 §2/§7/§8） | **接受** | §5.4/§7.4 M2-DOC-1/M2-DOC-4 |
| N-33-5 | Docker 锁 tag（`node:22-alpine`/`maven:3.9-eclipse-temurin-11`/`eclipse-temurin:11-jre`）+ Maven 依赖缓存层 + `.dockerignore`（含 node_modules/target/.git/.idea/.pi/docs） | **接受** | §4.1/§5.1/§9 C-33-16/§10 Q-2 |
| N-33-6 | compose `app`（`build: .`、`depends_on: mysql: service_healthy`、`ports 8081:8081`、容器内 `FABFLIX_DB_URL`（**不是 FDFLIX**）、env 注入）；不改 `mysql` 服务 | **接受**（注明：旧稿曾误写 `FDFLIX_DB_URL`，实为 **`FABFLIX_DB_URL`**） | §4.1/§5.1/§9 C-33-7/§10 Q-9 |
| N-33-7 | M2 checklist 补三项证据（PR 上 CI 全绿、`git ls-files frontend/dist` 为空、干净 checkout 的 `npm ci` + `mvn -Pfrontend clean package` 可复现） | **接受** | §2 AC-33-6/§7.1 M2-BE-5/6/7 |
| N-33-8 | 明确单测与生产 smoke 的证据分工 | **接受** | §4.2/§6 |
| N-33-9 | `vite.config.js` 保持 `base: '/'` 不改 | **接受** | §5.1/§10 Q-5 |
| N-33-10 | 更新 `fabflix.http` 中 jump-back/legacy 示例 | **接受** | §4.3/§5.3 命令段/§7.4 M2-DOC-5 |
| Q-1..Q-9 | 见 oracle 报告 §5 裁定建议表 | **全部接受** | §10（已转为「已裁定问题」表） |
| *镜像可拉取性* | oracle 在无网络环境无法验证三个镜像；若实施环境同样受限，M2-PROD 只能标注“未执行” | **主 agent 在本机 `docker manifest inspect` 验证三个镜像 OK** → M2-PROD smoke **计划为实际执行并回填**（若 worker 环境失败则如实标注） | §4.4/§7 引导语 |
| *本批修订* | 不改变 #33 的验收范围 | **接受：AC-33-1..AC-33-6 范围不变** | §2、§12 |

## 12. 状态（批准时记录 → 已由 §7/§13 更新为实施完成）

- **文档状态（批准时）：已批准（主 agent，2026-10-15）— 可交 worker 实现。** B3/B4/B5 已闭合（D-33-5/D-33-6/D-33-7），N-33-1..N-33-10 与 Q-1..Q-9 已全部裁定（§10/§11）；**本批修订未改变 AC-33-1..AC-33-6**。
- **批准后的破坏性动作（已授权，已实施）**：删除 **21 个** legacy 跟踪文件（`src/main/webapp/**`，D-33-3）+ **两处重定向行为变更**（`/login.html`→`/login`、`/movieList.html?<query>`→`/movies?mode=fulltext&<query>`，D-33-7）；已逐项落地（§13.1）。**当前状态：已随提交 `092147a` 进入 git（`git ls-files src/main/webapp` = 0）**。
- **测试/构建状态（批准时）：未运行任何测试，未运行任何构建/Docker 命令。** §7 的 M2 checklist 当时**全部为“未执行”**——现已由主 agent 终验逐条回填（含生产 smoke）。
- **实现前状态（批准时）**：无 `Dockerfile`/`.dockerignore`/`app` 服务/`frontend` profile/SPA fallback/dist 失败守卫；`src/main/webapp` 21 个文件仍在；`FrontendApiMigrationTest` 未改；两处 Java legacy 重定向仍在。**现已全部完成**（§13.1）。
- **批准时「仍由 worker 确认」的点**：镜像 tag、`-Pfrontend` 体积/资源 hash、失败守卫插件选择与绑定相位、fallback 与默认静态资源处理的交互、jump-back 新落点、`local` profile 下顾客登录——均已在 §13 回填。
- **下一步（现已完成）**：worker 已按本文档实现 #33（顺序 #31 → #32 → #33）→ 三个 reviewer 并行审查（无 Blocker）→ 同一 worker 修复并复验 → docs-writer 依实际 diff/构建/测试输出回填 §7 并建立 §13 与工作流日志。
- **只写范围**：本文档由 docs-writer 维护；docs-writer **未**删除/修改任何源码或 legacy 文件、**未**运行构建/Docker（§7/§13 的通过项来自主 agent 终验实测与本轮只读复核），**未**创建或修改 GitHub issue/PR，**未**提交或推送。

## 13. 实现结果与计划差异（主 agent 终验后回填，2026-10-15）

> **状态：实施完成，已通过主 agent 终验（2026-10-15）；已提交（`092147a`/`fb65eda`）、已推送、PR #42 已创建（OPEN，未合并）；CI 首跑（head `fb65eda`）两个 job 均 SUCCESS（§13.4）。**
> 证据来源：主 agent 亲跑的终验命令与输出（本地 Node 24 + Node 22 容器 + Docker 生产 smoke）＋本轮 docs-writer 的**只读**复核（`git status`/`git diff`、`unzip -l target/fablix.jar`、`target/surefire-reports/*`、`target/site/jacoco/jacoco.csv`、`git ls-files`、`grep`）。M2 checklist 的逐条结果见 **§7**。

### 13.1 实际改动文件（相对 §5 计划清单）

**构建 / 部署**

| 路径 | 状态 | 说明 |
| --- | --- | --- |
| `pom.xml` | 改（与计划一致，含 §13.2 第 4 条的定稿） | 新增 `frontend` profile：`maven-enforcer-plugin` 3.5.0 `require-frontend-dist`（**`validate` 相位**，`<fail>true</fail>`）+ `maven-resources-plugin` **3.3.1** `copy-frontend-dist`（`process-resources`，输出 `${project.build.outputDirectory}/static`，`filtering=false`）；**默认 profile 行为不变** |
| `Dockerfile` | 新（与计划一致 + 1 处评审修复） | 三阶段：`node:22-alpine`（`npm ci` → `npm run build && test -f dist/index.html`）→ `maven:3.9-eclipse-temurin-11`（`COPY pom.xml` + `mvn -Pfrontend dependency:go-offline` 缓存层 → `COPY src` + `COPY --from=frontend …/dist` → `mvn -B -Pfrontend -DskipTests clean package`）→ `eclipse-temurin:11-jre`；**评审修复：运行阶段创建 `fabflix` 用户/组（uid/gid 1001）并 `USER fabflix`（非 root）** |
| `.dockerignore` | 新（与计划一致 + 评审补强） | 另排除 `frontend/test-results`、`frontend/playwright-report`、`frontend/coverage`（#32 产物） |
| `compose.yaml` | 改（与计划一致） | 新增 `app` 服务：`build: .`、`container_name: fabflix-app`、`restart: unless-stopped`、`depends_on: mysql(service_healthy)`、`ports: ["8081:8081"]`、`FABFLIX_DB_URL=jdbc:mysql://mysql:3306/moviedb?useUnicode=true&characterEncoding=UTF-8`、`FABFLIX_DB_USER/PASSWORD`（来自 `.env`）、`FABFLIX_BOOT_PORT=8081`、**`SPRING_PROFILES_ACTIVE: local`**；`mysql` 服务未改 |
| `frontend/vite.config.js` | **未改**（与 Q-5 裁定一致） | 保持 `base: '/'` |

**应用代码 / 测试**

| 路径 | 状态 | 说明 |
| --- | --- | --- |
| `src/main/java/com/fabflix/boot/web/SpaFallbackConfig.java` | 新（路径与计划提案一致） | `@Configuration @Order(Ordered.LOWEST_PRECEDENCE) implements WebMvcConfigurer`；`/**` → `classpath:/static/` + `.resourceChain(false).addResolver(new SpaResourceResolver())`；`static boolean shouldFallback(String path)`（`api/**`、`error`/`error/**`、**含 `.` 的路径**、空/null 不 fallback，**大小写归一**）；`SpaResourceResolver extends PathResourceResolver`（真实资源优先，缺失且判定为 SPA 路径 → `index.html`） |
| `src/main/java/com/fabflix/boot/auth/CustomerAuthController.java` | 改（D-33-7） | `sendRedirect("/login.html")` → **`sendRedirect("/login")`**（302 语义不变） |
| `src/main/java/com/fabflix/boot/search/SearchController.java` | 改（D-33-7） | 302 `Location` → **`/movies?mode=fulltext&<query>`**（保留 `title=&pageSize=10&pageNumber=0` 默认查询串） |
| `src/main/webapp/**`（21 个跟踪文件） | **删除（已随提交 `092147a` 进入 git）** | `META-INF/MANIFEST.MF`、`styles.css` 及 19 个 `.html`/`.js` legacy 文件，共 21 个；`git ls-files src/main/webapp` 现为 0 行（见 §7 M2-DOC-2） |
| `src/test/java/com/fabflix/boot/web/SpaFallbackTest.java` | 新 | 5 个用例：真值表（含大小写变体）、真实资源/入口文档、缺失资源与 `api` 路径返回 null、`index.html` 缺失时返回 null、**注册层断言**（`/**` handler 使用 `SpaResourceResolver` 且 location 为 `classpath:/static/`） |
| `src/test/java/com/fabflix/boot/FrontendApiMigrationTest.java` | 重写（N-33-3 双保险） | `legacyWebappDirectoryIsRemoved()`（断言目录不存在）+ `noLegacyFrontendFilesRemain()`（若存在则遍历断言无 `.html`/`.js`）；**删除** `criticalPagesUseTheirSpringBootEndpoints` 与 `Files.walk` 缺失目录的旧写法 |
| `src/test/java/com/fabflix/boot/auth/CustomerAuthControllerTest.java` | 改 | 第 97 行断言 → `redirectedUrl("/login")` |
| `src/test/java/com/fabflix/boot/search/SearchControllerTest.java` | 改 | 两条 `Location` 断言 → `/movies?mode=fulltext&…` |
| `SpringBootOnlyArchitectureTest.java` | **未改** | 仍通过（`mvn verify` 全绿） |

**文档 / 仓库卫生（同批）**：`README.md`、`frontend/README.md`、`fabflix.http`（jump-back 注释）、`docs/architecture/frontend-migration-contracts.md`（§1/§2/§3/§7/§9/§11）、三个计划与三个 issue 文档、工作流日志。

### 13.2 与计划文本的差异（实现后确认）

| # | 计划原文 | 实际实现 | 裁定与理由 |
| --- | --- | --- | --- |
| 1 | §5.1：两处重定向按 D-33-7 改指向 | 完全按计划执行 | 无差异；`CustomerAuthControllerTest`/`SearchControllerTest`/`fabflix.http` 同步更新 |
| 2 | §4.3：守卫测试「双保险」 | 完全按计划执行（目录不存在 + 若存在则无 `.html`/`.js`） | 无差异 |
| 3 | §4.2/Q-3：`WebMvcConfigurer.addResourceHandlers("/**")` + 自定义 `PathResourceResolver`；判定抽纯函数 | 同计划，**补充实现细节**：`@Order(Ordered.LOWEST_PRECEDENCE)`（覆盖 Boot 默认资源 handler）、`shouldFallback` 大小写归一（`API`/`Error` 也不返回 HTML）、`returnsNothingWhenTheEntryDocumentIsMissing` 用例（默认无 dist 构建下 SPA 路径解析为 null 而非抛异常） | 大小写归一是**评审发现**（生产 smoke 中 `/API/boot/cart` 曾被 fallback 吞掉）；`resourceChain(false)` 见第 4 条 |
| 4 | §4.1/§5.1：dist 缺失守卫的插件/绑定相位「enforcer `requireFilesExist` 或 antrun `<fail>`，绑定在 `process-resources` 之前」；未规定 resources 插件版本 | **实际选择**：`maven-enforcer-plugin` `require-frontend-dist`，相位 **`validate`**（在 `process-resources` 之前，满足意图）；`maven-resources-plugin` 版本固定 **3.3.1**；`SpaFallbackConfig` 的资源链用 **`resourceChain(false)`** | ① 本 `pom.xml` **无 `<parent>`**，BOM 只管理依赖不管理插件 → 显式 `<version>` 是消除 `plugin.version is missing` 警告、锁定 `copy-resources` 行为所必需（删除会引入警告并依赖 Maven 隐式绑定）；② `validate` 比 `process-resources` 更早，失败更早暴露；③ `resourceChain(false)` 在**不启用无界资源缓存**的前提下仍需 `.addResolver(...)` 注册（避免长跑进程内存无界增长，同时保留 SPA 解析） |
| 5 | §4.4/§7.3 M2-PROD-8：`docker compose down`（保留 volume） | 实际执行 **`docker compose rm -sf app`**，mysql 容器与 `fabflix-mysql-data` 卷未动 | 比计划**更保守**：避免 `down` 影响数据库容器/卷；效果等价（app 干净退出） |
| 6 | §4.1/§5.1：Dockerfile 运行阶段「JRE 镜像跑 `target/fablix.jar`」 | 额外创建 uid/gid 1001 的 `fabflix` 用户并 `USER fabflix` | **评审修复**（非计划项）：容器以非 root 运行，属安全加固；不改变启动命令 |
| 7 | §5.3：`SpaFallbackTest` 覆盖真值表 + 「未映射 `/api/boot/xxx` 不得返回 HTML」 | 另新增**注册层断言**（`wiresTheCatchAllPatternToTheSpaResolver`，用 `InspectableRegistry` 暴露 `ResourceHandlerRegistry.getHandlerMapping()`）与「入口文档缺失 → null」用例 | 评审要求补强「resolver 真的被注册」这一层（N-33-8 的证据分工：注册层可单测，jar/服务行为仍需 smoke） |

### 13.3 终验证据（AC-33-1..AC-33-6）

| AC | 证据 | 结果 |
| --- | --- | --- |
| AC-33-1 | `mvn -Pfrontend -DskipTests clean package`；jar 内静态资源；默认 `mvn verify` 不依赖 Node/dist | ✅ jar 内 6 个 `BOOT-INF/classes/static` 条目；dist 缺失时 **BUILD FAILURE**（enforcer）；默认 `mvn --batch-mode verify` → BUILD SUCCESS（127 tests） |
| AC-33-2 | `docker compose build app` + `up -d --no-deps app` + health | ✅ 构建成功；health **200**；smoke 后 `docker compose rm -sf app`（mysql 与卷未动） |
| AC-33-3 | `SpaFallbackTest` 真值表/注册层 + 生产 smoke 深链 | ✅ 单测全绿（含到后端 127 内）；**10 条深链全部 200 `text/html`**；`/assets/missing.js` 404 |
| AC-33-4 | 退役守卫测试 + grep 无遗留引用 + 两处重定向新落点 + `fabflix.http` | ✅ `FrontendApiMigrationTest` 重写后全绿；`src/main/java`/`pom.xml`/`compose.yaml`/`Dockerfile`/`.http` 无 legacy 路径；两处重定向已改并同步测试 |
| AC-33-5 | `mvn --batch-mode verify` + #32 前端命令 | ✅ **已完成**：本地全部通过（后端 127 / 前端 195 / e2e 12 / lint 干净 / build 成功）；**PR #42（head `fb65eda`）上 CI 两个 job 均 SUCCESS**（`Build and test (Java 11)`、`Frontend (lint, test, coverage, build, e2e)`）→ 该 AC 的「CI 全绿」直接证据**已回填**（§13.4）；**PR 仍为 OPEN、未合并** |
| AC-33-6 | §7 的 M2 checklist | ✅ **已逐条回填**；原「非完全通过」的 3 条已有两条随 PR/提交闭合：**M2-BE-6 → ✅ SUCCESS（PR #42 两 job）**、**M2-DOC-2 → ✅ `git ls-files src/main/webapp` = 0**（提交 `092147a`）；**仅 M2-BE-5 仍为部分验证**（干净 checkout 端到端未单独复现，CI 的 `npm ci` + build 作为间接补强）；M2-DOC-3 已更新为「提交后仅剩未跟踪 `.idea/*`」 |

### 13.4 PR、CI 首跑与提交后状态（原「待 PR 后回填 / 未验证项」，**已回填 2026-10-15**）

| # | 项 | 状态 |
| --- | --- | --- |
| 1 | **PR 链接与 CI 首跑（含 #32 新增的 `frontend` job）** | ✅ **已回填**：提交 `092147a`（实现）+ `fb65eda`（文档）已推送；PR **#42**（HEAD `fb65eda`）；**CI 首跑两个 job 均 SUCCESS**（`Build and test (Java 11)`、`Frontend (lint, test, coverage, build, e2e)`）→ M2-BE-6 与 AC-33-5 的 CI 直接证据已闭合（来源：主 agent 核验的 GitHub CI 结果）。**PR 仍为 OPEN、未合并** |
| 2 | M2-BE-5「干净 checkout 的 `npm ci` + `mvn -Pfrontend clean package` 单次端到端复现」 | **仍未单独执行**（各步骤已分别验证）→ 记为未完全验证；PR CI 的 `npm ci` + `npm run build`（干净 checkout）作为间接补强（M2-BE-6） |
| 3 | M2-DOC-2「`git ls-files src/main/webapp` 为空」 | ✅ **已成立**：docs-writer 本轮只读复核 `git ls-files src/main/webapp` → 0 行（21 文件随 `092147a` 移除） |
| 4 | `.idea/*`（6 个未跟踪本机文件） | **仍未处理**：提交后 `git status --short --untracked-files=all` 仅剩这 6 项，非本次变更，待主 agent 决定是否排除（M2-DOC-3） |
| 5 | 真实浏览器 + 真实 MySQL 的存储过程往返（`add_movie`） | **未自动化**（e2e 全 mock；生产 smoke 只到 metadata）；见 #31 §13.5 |
| 6 | 手工颜色对比度确认 | jsdom 无法验证（#32 §13.5）；需人工在浏览器确认 |

### 13.5 回退方案（实现后复核，与计划 §9 一致）

- 无 schema/数据迁移；`compose.yaml` 仅新增服务、`mysql` 未改；**生产 smoke 未触碰数据卷**。
- 两处重定向为已批准的行为变更，回退即恢复 legacy 落点（但 legacy 文件已删，故回退需一并恢复守卫测试）。
- `frontend` profile 为附加 profile，默认构建不受影响 → 仅需去掉 profile 即可回到「不打包前端」状态。

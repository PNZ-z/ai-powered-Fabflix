# Issue #33 — Integrate production React build and retire the legacy frontend（Issue 跟踪文档）

> **状态：已在 GitHub 存在，无需新建。** 本文档记录远端 issue 的编号/URL/标题/正文原文与本地计划范围；
> 它**不是**发布草稿，`github-operator` **不需**为本 issue 创建新 issue。docs-writer 不修改远端 issue 内容。
>
> **本地计划状态（2026-10-15）：`docs/plans/issue-33-integrate-production-react-build-and-retire-legacy-frontend.md` = 已批准（主 agent，2026-10-15，依据 oracle 报告）→ 实现完成，已通过主 agent 终验（2026-10-15）。**
> **实现状态：已完成并已合并（分支 `codex/issue-31-32-33-m2-completion`，HEAD `fb65eda`；已提交 `092147a`/`fb65eda`、已推送、**PR #42 已于 2026-10-16 合并**（merge commit `d618b64`）；**issue #33 已关闭**（GitHub 依 PR 正文的 `Closes #33` 自动关闭）；合并后远端分支已删除，`main`（`d618b64`）CI 双 job SUCCESS）。** 破坏性动作已实际执行并已进入 git（见 §3.5）。
> **验证状态：`mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS / 127 tests / JaCoCo LINE 968/110/1078 = 0.8980；`mvn -Pfrontend -DskipTests clean package` 产物含 `static/index.html`；dist 缺失 → BUILD FAILURE（enforcer）；`docker compose build app` + `up -d --no-deps app` → health 200、**10 条深链全部 200 text/html**、未认证 `/api/boot/cart` 401 JSON、`/API/boot/cart` 404 JSON、`/assets/missing.js` 404；顾客/员工登录链路走通；M2 checklist 逐条结果见计划 §7。** AC 逐条状态与证据见 §6。
>
> ⛔ **本 issue 含用户/主 agent 已批准的破坏性动作**：删除整个 `src/main/webapp/**`（21 个跟踪文件，见 §3.2）；**并包含两处已批准的行为变更**：顾客非 JSON 登出重定向 `/login.html`→`/login`、jump-back `/movieList.html?<query>`→`/movies?mode=fulltext&<query>`（见 §3.3/§3.4）。
> ✅ **两处 legacy 重定向已裁定并闭合**（oracle B5，主 agent 明确批准）；**容器内 reCAPTCHA 策略（B3）与 dist 缺失失败守卫（B4）亦已裁定**（见 §3.4）。

## 1. 远端元数据

| 项目 | 值 |
| --- | --- |
| 编号 | 33 |
| URL | https://github.com/PNZ-z/ai-powered-Fabflix/issues/33 |
| 标题（远端原文） | Integrate production React build and retire the legacy frontend |
| Milestone | Phase 2 — React Frontend Migration（与 #31/#32 共用；由主 agent 提供） |
| 获取方式 | 主 agent 用 `gh` 获取的远端原文，并在任务说明中逐字提供（docs-writer 无 GitHub 访问授权，**未**直接查询远端） |
| 远端状态核对 | **已关闭（`CLOSED`）**——2026-10-16 由 GitHub 依 PR #42 正文的 `Closes #33` 自动关闭（主 agent 核验）；docs-writer 未直接查询远端，仍以远端为准 |
| 本地记录文档 | 本文件 |
| 关联计划 | `docs/plans/issue-33-integrate-production-react-build-and-retire-legacy-frontend.md`（**已批准（主 agent，2026-10-15）→ 实施完成，已通过主 agent 终验（2026-10-15）**；M2 checklist 已逐条回填，实现结果/差异见其 **§7** 与 **§13**） |
| 关联全局约束 | `docs/architecture/frontend-migration-contracts.md`（§2 legacy 保留策略、§8 架构守卫） |
| 实现顺序 | **最后一个 M2 issue**；依赖全部功能迁移与质量 issue（#31/#32）；**#31 完成前不得删 legacy** |

> ⚠️ 以上元数据来自主 agent 提供的远端原文；docs-writer **未**直接读取远端。如与远端当前状态不符，以远端为准并由主 agent 更正。

## 2. 远端 issue 正文原文（逐字保留，供对照）

```markdown
## Goal
Make React the only supported production frontend and finish M2 safely.

## Scope
- Integrate built React assets with Spring Boot and Docker
- Configure SPA route fallback without intercepting API routes
- Verify all customer and employee flows against the production build
- Remove superseded legacy HTML and JavaScript files
- Update README, architecture notes, and local/production startup instructions

## Acceptance criteria
- [ ] Spring Boot serves the production React application
- [ ] Docker Compose builds and starts the complete stack
- [ ] Direct navigation works for every React route
- [ ] No active route references legacy frontend files
- [ ] Full backend and frontend CI passes after legacy removal
- [ ] M2 regression checklist is complete

## Dependency
Final M2 issue; depends on all feature migration and quality issues.
```

## 3. 用户/主 agent 已批准的范围决定（视为批准范围内）

### 3.1 已批准决定

| 编号 | 已批准决定 | 对应 issue 正文 |
| --- | --- | --- |
| D-33-1 | 生产构建走 **Docker 多阶段**（**不提交 `frontend/dist`**）：前端 `node:22` 构建 → Maven 阶段在 **`-Pfrontend` profile** 下用 resources 插件把 `frontend/dist` 复制进 `target/classes/static` → 运行阶段用 JRE 镜像跑 `target/fablix.jar`；`compose.yaml` 新增 `app` 服务（`depends_on: mysql healthy`、端口 **8081**、env 注入 `FABFLIX_DB_*`）；本地流程写入 README（`cd frontend && npm ci && npm run build` 后 `mvn -Pfrontend package`）。 | scope 第 1 条；AC 第 1、2 条 |
| D-33-2 | **SPA fallback**：真实静态资源优先；**`/api/**` 一律不被 fallback 拦截**；其余非文件路径（`/home`、`/movies/...`、`/cart`、`/employee/...` 等）返回 `index.html`；需有针对性的测试（在「不依赖 `@SpringBootTest`」的现有约定下，可对 resolver/配置类做隔离单测）。 | scope 第 2 条；AC 第 3 条 |
| D-33-3 | **删除整个 `src/main/webapp/**`**（**用户已批准该破坏性动作**；实际为 **21 个 git 跟踪文件**，非 20——见 §3.2）；`FrontendApiMigrationTest` 替换为「legacy 已退役」守卫测试（如断言目录不存在/无 `.html`/`.js`）；同步更新 `README.md`、`frontend/README.md`、`docs/architecture/frontend-migration-contracts.md` 中所有 legacy 引用；`SpringBootOnlyArchitectureTest` 保持通过。 | scope 第 4 条；AC 第 4 条 |
| D-33-4 | **M2 regression checklist 作为文档产出**（#33 计划内或独立 `docs/` 文档）；条目需**可执行、可记录证据**：后端 `mvn verify`、前端 lint/test/coverage/build、e2e、生产形态 smoke（`docker compose build app` + health + index.html + 关键 API/页面）；**实际执行结果由 worker 回填；无法执行的项如实标注**。 | AC 第 6 条 |
| 顺序与交付 | 一个分支 `codex/issue-31-32-33-m2-completion`、一个 PR（`Closes #31`/`#32`/`#33`）；顺序 #31 → #32 → #33；每阶段跑相应测试；后端新增代码必须配测试（JaCoCo 85% 闸门）。 | issue 正文未规定；由主 agent 传达 |

### 3.2 破坏性动作记录（已批准）

| 项 | 记录 |
| --- | --- |
| 动作 | **删除整个 `src/main/webapp/` 目录**（`git rm -r src/main/webapp`） |
| 批准状态 | **用户已批准**（经主 agent 传达，D-33-3） |
| 实际文件数 | **21 个 git 跟踪文件**（docs-writer 用 `git ls-files src/main/webapp \| wc -l` 实测）：`META-INF/MANIFEST.MF`、`styles.css`、`_dashboard.html`、`_dashboard.js`、`confirmation.html`、`dashboard.html`、`dashboard.js`、`login.html`、`login.js`、`main.html`、`main.js`、`movieList.html`、`movieList.js`、`payment.html`、`payment.js`、`shoppingCart.html`、`shoppingCart.js`、`single-movie.html`、`single-movie.js`、`single-star.html`、`single-star.js` |
| 数量修正 | scout C4 与主 agent 任务说明写「20 个文件」，但 scout 同一清单实际列出 21 项；**实测为 21**。本文件以实测为准，已提请主 agent 注意 |
| 可回退性 | git 历史可完整回溯（删除是可回退的版本控制操作）；**但**删除后运行时的 legacy 页面立即不可用（当前它们已非运行时资源：无静态资源映射、`jar` 打包 —— scout C1/架构文档 §2） |
| 前置条件 | ① #31 已完成（legacy 员工页是 #31 的行为基准）→ **已满足**；② 与 `FrontendApiMigrationTest` 同步重写（双保险守卫）→ **已完成**；③ 两处重定向按 **D-33-7（已批准行为变更）** 改指向 → **已完成**；④ `-Pfrontend` 加 dist 缺失失败守卫（D-33-6）→ **已完成** |
| 不做 | 不保留副本（不移动到 `docs/`）；如需保留须重新批准（Q-4 已裁定） |

### 3.3 ⚠️ docs-writer 新发现（scout 未覆盖）——**已裁定并闭合**

| # | 事实 | 证据 | 影响 |
| --- | --- | --- | --- |
| N-33-1 | 顾客登出在 `Accept` 不含 `application/json` 时 **302 → `/login.html`** | `src/main/java/com/fabflix/boot/auth/CustomerAuthController.java:68`；钉死测试 `src/test/java/com/fabflix/boot/auth/CustomerAuthControllerTest.java:97`（`redirectedUrl("/login.html")`） | 删除 webapp 后该重定向指向**不存在的文件**，与 AC-33-4「No active route references legacy frontend files」冲突 |
| N-33-2 | `GET /api/boot/jump-back` **302 → `/movieList.html?<query>`** | `src/main/java/com/fabflix/boot/search/SearchController.java:47`；钉死测试 `src/test/java/com/fabflix/boot/search/SearchControllerTest.java:55,60` | 同上；且这是**仍在生效的 API 路由**引用 legacy 文件 |

> **处置方式已裁定（D-33-7，主 agent 2026-10-15 明确批准行为变更）**：`/login.html` → **`/login`**；`/movieList.html?<query>` → **`/movies?mode=fulltext&<query>`**（**必须带 `mode=fulltext`**：`lastMovieQuery` 只由 `fullTextSearch` 写入，缺此参数时 React 列表页落到 browse 模式并**丢弃 `query`**）；302 语义不变、仅换落点；同步改 `CustomerAuthControllerTest.java:97`、`SearchControllerTest.java:55,60`、`fabflix.http:45`。

### 3.4 本轮新增裁定与批准记录（主 agent，2026-10-15）

| 编号 | 裁定 | 对应 AC |
| --- | --- | --- |
| **D-33-5（B3）** | compose `app` 显式 **`SPRING_PROFILES_ACTIVE=local`**（仅本地/演示完整栈 smoke）；README 必须写明生产需 `RECAPTCHA_SECRET_KEY` + **构建期** `VITE_RECAPTCHA_SITE_KEY`（运行期注入无效）；M2-PROD-7 按此重写 | AC-33-2/AC-33-6 |
| **D-33-6（B4）** | **`-Pfrontend` 在 `frontend/dist/index.html` 缺失时必须构建失败**（enforcer `requireFilesExist` / antrun `<fail>`，绑定在 `process-resources` 之前）；Docker/本地统一 `mvn -Pfrontend clean package`；Dockerfile 前端阶段可加 `test -f dist/index.html` | AC-33-1 |
| **D-33-7（B5：行为变更，已批准）** | ① `CustomerAuthController.java:68`：`/login.html` → **`/login`**；② `SearchController.java:47`：`/movieList.html?<query>` → **`/movies?mode=fulltext&<query>`**；302 语义不变；同步改 `CustomerAuthControllerTest.java:97`、`SearchControllerTest.java:55,60`、`fabflix.http:45` | AC-33-4 |

> **破坏性/行为变更清单一览（交付报告必须逐项列出）**：① 删除 21 个 legacy 跟踪文件；② 顾客非 JSON 登出重定向落点变更；③ jump-back 302 落点变更（含新增 `mode=fulltext`）。三项均属**已批准范围**。
> **行号不一致处置**：若本文档或计划中的行号与 oracle 报告不符，**以 oracle 报告（已复核）为准**。

### 3.5 破坏性动作与行为变更：实际执行记录（2026-10-15）

| # | 项 | 执行结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 删除 `src/main/webapp/**`（**21** 个跟踪文件） | ✅ **已删除并已随实现提交 `092147a` 进入 git**；docs-writer 只读复核 `git ls-files src/main/webapp` = **0 行**，`git show --stat 092147a -- src/main/webapp` = 21 files changed / 2057 deletions → M2-DOC-2 已为真 | 只读复核 `git ls-files`/`git show --stat`；计划 §7 M2-DOC-2 |
| 2 | 两处重定向行为变更（D-33-7） | ✅ `CustomerAuthController.java`：`/login.html` → **`/login`**；`SearchController.java`：`/movieList.html?<query>` → **`/movies?mode=fulltext&<query>`**（302 语义不变）；对应测试与 `fabflix.http` 同步更新 | 只读复核 `git diff`；`mvn verify` 127 全绿（主 agent 实测） |
| 3 | `FrontendApiMigrationTest` 替换为退役守卫 | ✅ 重写为双保险：`legacyWebappDirectoryIsRemoved()`（断言目录不存在）+ `noLegacyFrontendFilesRemain()`（若存在则无 `.html`/`.js`）；删除 `criticalPagesUseTheirSpringBootEndpoints` 与 `Files.walk` 缺失目录的旧写法 | 只读复核 `git diff`；`mvn verify` 全绿（主 agent 实测） |
| 4 | dist 缺失失败守卫（B4/D-33-6） | ✅ `maven-enforcer-plugin` `require-frontend-dist`（`validate` 相位）；临时移走 `frontend/dist` → **BUILD FAILURE**（报错含 `frontend/dist/index.html is missing`） | 主 agent 实测；计划 §7 M2-BE-2b |
| 5 | Docker 运行时非 root（评审修复） | ✅ 运行阶段创建 uid/gid 1001 的 `fabflix` 用户并 `USER fabflix` | 只读复核 `Dockerfile` |

## 4. 范围（依据 issue + 已批准决定）

### 4.1 In scope

- Spring Boot 接入生产 React 产物（`-Pfrontend` profile + resources 插件；不提交 `dist`）。
- SPA 路由 fallback（不拦 `/api/**`）+ 针对性测试。
- 用生产构建验证顾客与员工全部流程（生产 smoke）。
- **删除被取代的 legacy HTML/JavaScript 文件**（整个 `src/main/webapp/`，21 文件；已批准）。
- Docker 多阶段构建 + `compose.yaml` 的 `app` 服务。
- README / `frontend/README.md` / 架构说明 / 本地与生产启动说明更新。
- M2 regression checklist（可执行 + 证据）。

### 4.2 非目标（Non-goals）

- 提交 `frontend/dist` 到仓库（D-33-1 已批准「不提交」；`frontend/.gitignore` 已忽略）。
- 修改业务 API 契约 / DB schema / 存储过程。
- 除 N-33-1/N-33-2 的处置外的业务行为语义变更。
- 多实例部署 / Spring Session / 反向代理（nginx 等）。
- Kubernetes / 镜像发布到 registry / CI 部署流水线。
- Playwright 设施、覆盖率阈值、CI 前端步骤本身（→ #32）。
- 顾客/员工功能补做（→ #31 及更早 issue）。
- 修改 JaCoCo 85% 闸门或 excludes。
- 引入 Spring Security / CSRF / JWT（架构文档 §10 AD-1）。

## 5. 依赖

| 依赖（issue 正文：depends on all feature migration and quality issues） | 现状/证据 |
| --- | --- |
| 全部 React 流程已迁移（顾客 + 员工） | 顾客侧已完成（#30 等）；**员工侧依赖 #31** |
| 前端测试/覆盖率/CI/e2e 设施 | **依赖 #32**（AC-33-5 要求"删除 legacy 后前后端 CI 全绿"） |
| 构建能力 | 已有：`frontend/package.json` 的 `build`；`frontend/vite.config.js`（`appType:'spa'`，默认 `base='/'`/`outDir='dist'`） |
| 静态资源接入 | **无**（无 `WebMvcConfigurer`/`static` 目录/`pom.xml` profile，均已核实） |
| Docker | **无 `Dockerfile`/`.dockerignore`**；`compose.yaml` 仅有 `mysql` 服务 |
| 生产验证 | 仅有 `fabflix.http` 手工请求；**无** smoke 脚本、**无** M2 checklist 既有定义（scout C6） |

## 6. 验收条件与跟踪（**状态：全部已完成（主 agent 终验 2026-10-15）；AC-33-5 的 CI 证据已随 PR #42 首跑回填**）

| # | 验收条件（远端原文） | 承载方案（计划） | 状态与证据 |
| --- | --- | --- | --- |
| AC-33-1 | Spring Boot serves the production React application | `-Pfrontend` profile + resources 插件（D-33-1）+ dist 失败守卫（D-33-6/B4） | ✅ **已完成**：`mvn -Pfrontend -DskipTests clean package` 成功；jar 内 6 个 `BOOT-INF/classes/static` 条目（含 `index.html`、`favicon.svg`、`assets/index-*.{css,js}`）；**dist 缺失 → BUILD FAILURE**（enforcer）；默认 `mvn verify` 不依赖 Node/dist |
| AC-33-2 | Docker Compose builds and starts the complete stack | 多阶段 `Dockerfile` + `.dockerignore` + `app` 服务（D-33-1） | ✅ **已完成**：`docker compose build app` 成功；`up -d --no-deps app` 后 health **200**；随后 `docker compose rm -sf app`（**mysql 与数据卷未动**） |
| AC-33-3 | Direct navigation works for every React route | SPA fallback（D-33-2）+ 深链验证（含 `/api/**` 不被拦截） | ✅ **已完成**：`SpaFallbackTest` 5 个用例（真值表含大小写变体、真实资源/入口文档、缺失资源与 `api` 路径 → null、入口缺失 → null、注册层断言）；**生产 smoke：10 条深链全部 200 text/html**；未认证 `/api/boot/cart` → **401 JSON**；`/API/boot/cart` → **404 JSON**；`/assets/missing.js` → **404** |
| AC-33-4 | No active route references legacy frontend files | 删除 webapp（D-33-3）+ **两处重定向按 D-33-7 改指向** + 文档行级清单清理 | ✅ **已完成**：21 个 legacy 文件已删除并随提交 `092147a` 进入 git（`git ls-files src/main/webapp` = 0）；两处重定向已改并同步测试与 `fabflix.http`；源码/配置/构建文件无 legacy 路径（只读 grep）；README/frontend README 已改写 |
| AC-33-5 | Full backend and frontend CI passes after legacy removal | `FrontendApiMigrationTest` 重写 + `SpringBootOnlyArchitectureTest` 保持通过 + CI 前后端 | ✅ **已完成**：本地 `mvn --batch-mode verify`（127 tests、JaCoCo LINE 0.8980）、`SpringBootOnlyArchitectureTest` 保持通过、#32 前端命令全绿；**PR #42（head `fb65eda`）上 CI 两个 job 均 SUCCESS**（`Build and test (Java 11)`、`Frontend (lint, test, coverage, build, e2e)`）→ 该 AC 的直接证据已闭合（来源：主 agent 核验的 GitHub CI 结果）；**PR 已于 2026-10-16 合并（`d618b64`），合并后 `main` 上双 job 仍 SUCCESS** |
| AC-33-6 | M2 regression checklist is complete | 计划 §7 的 checklist（已逐条回填证据） | ✅ **已完成（原 3 条非完全通过已闭合 2 条）**：**M2-BE-6 → ✅ SUCCESS**（PR #42 两 job）；**M2-DOC-2 → ✅**（`git ls-files src/main/webapp` = 0）；M2-DOC-3 → ✅（提交后仅剩未跟踪 `.idea/*`，待主 agent 决定是否排除）；**M2-BE-5 仍为部分验证**（干净 checkout 端到端未单独复现，CI 的 `npm ci` + build 作为间接补强） |

> **证据要求（已满足）**：AC 对应的实际命令与结果已填入上表（均为**主 agent 亲跑**）；原标注为「无法执行」的 CI 项（M2-BE-6）已随 **PR #42（head `fb65eda`）的 CI 首跑**回填为 SUCCESS（来源：主 agent 核验的 GitHub CI 结果）。docs-writer 只读复核了 `git status`/`git diff`/`git ls-files`/`unzip -l`/surefire/jacoco 产物与源码。
> **事实（已更新，2026-10-16）**：改动已提交（`092147a`/`fb65eda`）、已推送，**PR #42 已于 2026-10-16 合并**（merge commit `d618b64`）；**CI 首跑（head `fb65eda`）两个 job 均 SUCCESS，合并后 `main`（`d618b64`）上双 job 亦 SUCCESS**；**issue #33 已由 GitHub 依 PR 正文的 `Closes #33` 自动关闭**（`CLOSED`）；远端分支已删除；docker 生产 smoke 已实际执行（非 CI，在本地）；里程碑 Phase 2（#2）已关闭（10/10）。
> **已回填**：PR 链接/提交号、CI 首跑（后端 job + #32 的 `frontend` job）、`git ls-files src/main/webapp` = 0（提交 `092147a` 后成立）。

## 7. 关联记录

| 类型 | 位置 | 说明 |
| --- | --- | --- |
| 计划 / 方案 | `docs/plans/issue-33-integrate-production-react-build-and-retire-legacy-frontend.md` | **已批准（主 agent，2026-10-15）→ 实施完成，已通过主 agent 终验（2026-10-15）**；B3/B4/B5 已闭合，N-33-1..N-33-10 与 Q-1..Q-9 已全部裁定；**M2 checklist 见其 §7（已逐条回填）**，实现结果/差异见其 §13 |
| scout 调查报告 | `.pi/artifacts/scout-issues-31-32-33.md` | §C（Spring Boot 静态资源、前端构建、Docker、legacy 退役范围、SPA 路由清单、生产验证手段）；**未覆盖 §3.3 的两处 Java 重定向** |
| 架构约束 | `docs/architecture/frontend-migration-contracts.md` | §1 迁移进度（M2 已收尾）、§2 **legacy 已退役**、§8 架构守卫、§9 会话/认证、§10 AD-2、**§11 SPA fallback 与前端测试设施/Node 22 CI（新增）** |
| 工作流日志 | `docs/logs/2026-10-15-issues-31-32-33-m2-completion.md` | **已创建**（三个 issue 共用；含生产 smoke 与 M2 checklist 结果摘要） |
| 远端 issue | #33（见 §1） | **不由 docs-writer 修改**；该 issue 已由 GitHub 依 PR #42 的 `Closes #33` 自动关闭（`CLOSED`，2026-10-16）；PR #42 合并与 CI 结果已回填（本 §7 的 PR 行与计划 §7.1/§13.4） |
| PR | **#42** — https://github.com/PNZ-z/ai-powered-Fabflix/pull/42 | **已合并（`MERGED`，2026-10-16；merge commit `d618b64`，`mergedAt` = 2026-10-16T04:57:00Z）**：分支 `codex/issue-31-32-33-m2-completion`（HEAD `fb65eda`）→ base `main`；`closingIssuesReferences` = #31/#32/#33；**CI 首跑与合并后 `main` 上两 job 均 SUCCESS**；远端分支已删除 |

## 8. 备注

- 本文件**不发布到 GitHub**；远端 issue 内容不由 docs-writer 修改。**删除 legacy 的实际执行不在本阶段**：本文件只是记录"已批准该破坏性动作"这一事实；在 oracle 审查 + 主 agent 批准计划之前不得执行。
- **2026-10-15（创建时）**：本文件与关联计划由 docs-writer 首次创建；**当时的计划状态 = 待 oracle 审查 / 未批准**；实现未开始、未运行任何测试/构建/Docker 命令。**当前状态见下一行的本轮批准记录。**
- **2026-10-15（本轮）**：oracle 只读审查完成（报告 `.pi/artifacts/oracle-issues-31-32-33-plans.md`；结论：#33 需修订后可批准，B3/B4/B5 必须先闭合）→ **B3/B4/B5 已按裁定写入计划（B5 为行为变更，主 agent 明确批准）；主 agent 批准计划并全部接受 N-33-1..N-33-10/Q-1..Q-9**。
- **2026-10-15（实施终验）**：worker 完成 #33 实现并由同一 worker 处理三路只读审查意见（无 Blocker）后复验；**主 agent 亲跑终验**：后端 `mvn verify` 127 tests / LINE 0.8980；`-Pfrontend` 打包与 dist 缺失失败守卫；**生产 compose smoke**（health 200 / 10 条深链 200 text/html / `/API/boot/cart` 404 JSON 等）；前端 195 tests / 92.35% / e2e 12 passed。详见计划 §7（checklist）与 §13。
- **2026-10-15（提交 / PR / CI 首跑）**：实现提交 `092147a`（含 21 个 legacy 文件的删除）、文档提交 `fb65eda` 已推送；**PR #42 已创建**；**CI 首跑（head `fb65eda`）两个 job 均 SUCCESS**（`Build and test (Java 11)`、`Frontend (lint, test, coverage, build, e2e)`）→ M2-BE-6 与 AC-33-5 已闭合；`git ls-files src/main/webapp` = 0（M2-DOC-2 已为真）。来源：主 agent 核验的 GitHub CI 结果（docs-writer 未直接访问 GitHub）。详见计划 §7.1 与 §13.4。
- **2026-10-16（合并 / issue 关闭 / 里程碑关闭）**：**PR #42 已合并**（merge commit `d618b64`；`mergedAt` = 2026-10-16T04:57:00Z）；**issue #33 由 GitHub 依 PR 正文的 `Closes #33` 自动关闭**（`CLOSED`）；远端分支 `codex/issue-31-32-33-m2-completion` 已删除（origin 仅剩 `main`）；本地 `main` 已同步到 `d618b64`；**合并后 `main` 上 CI 双 job（Java 11 + Frontend）SUCCESS**；**里程碑 `Phase 2 — React Frontend Migration`（#2）已关闭**（`open_issues` = 0、`closed_issues` = 10、`closed_at` = 2026-10-16T05:00:27Z）。来源：主 agent 核验的 GitHub 结果（docs-writer 未直接访问 GitHub）。
- **镜像可拉取性**：主 agent 已在本机用 `docker manifest inspect` 验证 `node:22-alpine`、`maven:3.9-eclipse-temurin-11`、`eclipse-temurin:11-jre` 均可拉取；**生产 smoke 已实际执行**（未标注“未执行”）。
- **§3.3 的两处 legacy 重定向已闭合并已实施（D-33-7）**：`CustomerAuthController.java:68` → `/login`、`SearchController.java:47` → `/movies?mode=fulltext&<query>`，两个测试与 `fabflix.http` 已同步，`mvn verify` 全绿。
- docs-writer 无 GitHub 访问授权；§1 元数据来自主 agent 用 `gh` 获取的原文。§3.2/§3.3 的数量与文件:行号均为 docs-writer 通过 `git ls-files` / `grep` / 读源码亲自核实。

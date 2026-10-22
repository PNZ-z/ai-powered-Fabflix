# 工作流日志 / 证据记录 — Issue #51（S6：全量回归 + 生产 smoke 证明迁移）

> **本文件的定位：证据记录（evidence record）**，即 issue #51 Scope 最后一条要求的那份「单一日志」—— 每一项都列出**确切命令 / 结果摘要 / executed 或 not executed（含原因）**。
> **S6 是纯取证步骤**：不改源码、测试、`pom.xml`、CI、`Dockerfile`、`compose.yaml`、`frontend/**` 或架构文档；本步唯一的文件产出就是本文件（及对 checklist / steps / issue 文档的状态回填）。
> **不声称本步已合并**：分支尚未提交/推送，**无 PR**（见元数据）。

## 元数据

| 项目 | 值 |
| --- | --- |
| 日期（主机时区） | **2026-10-21**（主机 `date +%F` = `2026-10-21`，PDT，约 22:45）。**容器 / MySQL 为 UTC，故容器内观测到的日期为 2026-10-22** —— 见 §0.5 时区口径 |
| GitHub issue | **[#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)** `[Step 6/7] Prove the migration with full regression and production smoke evidence` —— state **OPEN**（创建于 2026-10-19；正文含 `Part of #44`） |
| 上游 epic | [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（保持 OPEN）；拆分总览 `docs/plans/issue-44-migration-steps.md`，checklist `docs/plans/issue-44-migration-checklist.md` |
| 分支 | **`codex/issue-51-regression-proof`**（**从已合并 S5 的 `main` = `ff57e79` 创建**；`ff57e79` = `Merge pull request #58 from PNZ-z/codex/issue-50-mockito-annotation`） |
| 基准 commit | **`ff57e79`**（S5 合并后的最终形态：Boot 3.5.15 + `release=17` + Jakarta + enforcer JDK 21 + 两阶段 JDK 21 镜像 + `@MockitoBean`） |
| 提交 / 推送 / PR | **未提交、未推送、无 PR**。本轮 `docs-writer` 只读核验时的仓库状态：`HEAD = ff57e79`、`git status --porcelain` 仅有开工前既存的未跟踪 `?? .idea/`（不属本次改动，不得带入提交） |
| 改动规模 | **源码/配置/测试：0 行**。PR 的 diff 预期只含 `docs/`（本日志 + checklist/steps/issue 文档回填） |
| 执行者 | 全部命令与 smoke 由**主 agent 亲自执行**；本文件由 `docs-writer` 依主 agent 回报 + 只读复核（`grep`/`git` 核对 `Dockerfile`、`pom.xml`、`vite.config.js`、merge 历史）撰写 |
| 证据来源 | 主 agent 的命令输出与 HTTP 观测记录（本文件逐条转述）；`docs-writer` 只读复核（2026-10-21）：`git log --merges`、`git status`、`grep -n "^FROM" Dockerfile`、`grep -c "<exclude>" pom.xml`、`grep -n requireJavaVersion pom.xml`、`sed -n '175,195p' pom.xml`、`frontend/vitest.config.js` |
| 关联 PR（**S5**，非本 issue，本步回填） | **[#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)** —— **MERGED**，merge commit **`ff57e79`**；CI **双 job SUCCESS**；[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50) 已 **CLOSED**；S5 的 **AC-1..AC-5 全部 ✅**（见 `docs/issues/issue-50-mockito-annotation-cleanup.md`） |
| 结论 | **迁移后的最终形态在单元测试、前端门禁、e2e、HTTP 语义、认证/会话、JDBC 日期与容器路径上均与记录一致；无失败项。** 另有 5 类**未执行**项已显式登记（§0.7），不得读作「全部验证通过」 |

---

## 0. 逐项证据表（Scope → 命令 → 结果 → executed / not executed）

> 判据口径：**executed** = 命令/请求真的跑了且结果已记录；**not executed** = 未跑并写明原因。**不存在"隐含通过"**；未执行项在 §0.7 汇总。

### 0.1 后端门禁（AC-1 / AC-2 / AC-9）

| # | 项目 | 命令 | 结果摘要 | 状态 |
| --- | --- | --- | --- | --- |
| B-1 | 全量后端门禁 | `mvn --batch-mode --no-transfer-progress verify`（`JAVA_HOME` = JDK 21） | **BUILD SUCCESS**；`Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`；`All coverage checks have been met.` | **executed ✅** |
| B-2 | JaCoCo 数字 | 同上（`target/site/jacoco/jacoco.csv`） | **988 covered / 1078 total = 0.9165**，门槛 **0.90** → 通过（与收紧后基线逐位相同：134 / 0.9165） | **executed ✅** |
| B-3 | `excludes` 未被扩 | `grep -c "<exclude>" pom.xml` | **8**（与迁移前基线一致；本步未动 `pom.xml`） | **executed ✅**（本轮 `docs-writer` 只读复核同为 8） |
| B-4 | 测试数量未下降 | 对比 checklist「2026-10-20 覆盖率门槛收紧」表 | **134 = 134**（不低于基线；S1 的 127 是收紧前历史快照，不作对照基线） | **executed ✅** |
| B-5 | 已知覆盖缺口（非本次回归） | `target/site/jacoco/jacoco.csv` | `admin/AdminRepository` **0 covered / 39 missed**；`auth/GoogleRecaptchaVerifier` **0 covered / 17 missed** —— 均为**既有缺口**，本步不改测试、不扩 `excludes` | **executed（只读核对）✅** |

### 0.2 前端门禁（AC-1 的前端侧 / #44 AC-5）

| # | 项目 | 命令（`frontend/`） | 结果摘要 | 状态 |
| --- | --- | --- | --- | --- |
| F-1 | 依赖安装 | `npm ci` | 成功（无 `--force`、未改 lockfile） | **executed ✅** |
| F-2 | lint | `npm run lint` | 干净（无 error / 无 warning 输出） | **executed ✅** |
| F-3 | 单测 | `npm test` | **30 files / 195 tests** 全绿（= 基线 195 / 30） | **executed ✅** |
| F-4 | 覆盖率 | `npm run test:coverage` | **Lines 92.35%（846/916）** ≥ 门槛 **90**（`frontend/vitest.config.js` 的 `coverage.thresholds.lines = 90`，本轮只读复核确认） | **executed ✅** |
| F-5 | 生产构建 | `npm run build` | 成功（`dist/` 产出，供 `-Pfrontend` 与容器镜像使用） | **executed ✅** |
| F-6 | 端到端 | `npm run test:e2e`（Playwright，6 用例 × 2 viewport） | **12 passed**（= 基线 12） | **executed ✅** |
| F-7 | 前端零行为改动 | `git diff fff314f -- frontend/src` | **0 行**（相对迁移前基线 commit `fff314f` 无任何 src 改动） | **executed ✅** |

### 0.3 生产 smoke —— HTTP 语义（AC-3 / #44 AC-6 的 smoke 部分）

**环境**：**提交版 `compose.yaml`** 的 `app` 服务（`docker compose build app` + `docker compose up -d --no-deps app`），端口 **8081**；容器内 **JDK 21**、运行用户 **非 root（uid 1001）**；MySQL 为既有 compose 服务（数据卷保留）。

| # | 项目 | 命令 / 请求 | 结果摘要 | 状态 |
| --- | --- | --- | --- | --- |
| P-1 | 健康检查 | `GET /api/boot/health` | **200** | **executed ✅** |
| P-2 | SPA 宿主页 | `GET /` | 200，正文含 **`id="root"`** | **executed ✅** |
| P-3 | 深链 `/home` | `GET /home` | **200 `text/html`** | **executed ✅** |
| P-4 | 深链 `/movies` | `GET /movies` | **200 `text/html`** | **executed ✅** |
| P-5 | 深链 `/movies/tt9000001`（真实 movie id） | `GET /movies/tt9000001` | **200 `text/html`** | **executed ✅** |
| P-6 | 深链 `/cart` | `GET /cart` | **200 `text/html`** | **executed ✅** |
| P-7 | 深链 `/checkout` | `GET /checkout` | **200 `text/html`** | **executed ✅** |
| P-8 | 深链 `/confirmation` | `GET /confirmation` | **200 `text/html`** | **executed ✅** |
| P-9 | 深链 `/login` | `GET /login` | **200 `text/html`** | **executed ✅** |
| P-10 | 深链 `/employee/login` | `GET /employee/login` | **200 `text/html`** | **executed ✅** |
| P-11 | 深链 `/employee/dashboard` | `GET /employee/dashboard` | **200 `text/html`** | **executed ✅** |
| P-12 | 深链 `/playground` | `GET /playground` | **200 `text/html`** | **executed ✅** |
| P-13 | 未认证 API → 401 | `GET /api/boot/cart`（无 cookie） | **401**，body **`{"errorMessage":"Authentication required."}`** | **executed ✅** |
| P-14 | 大小写变体 → 404（非 401） | `GET /API/boot/cart` | **404**，`application/json`（证明未认证判定只作用于精确路径，大小写变体落到「无映射」而非「未认证」） | **executed ✅** |
| P-15 | 静态资源 404 | `GET /assets/missing.js` | **404**（未被 SPA fallback 吞掉） | **executed ✅** |
| P-16 | SPA fallback | `GET /no-such-page` | 返回 **`index.html`**（含 `id="root"`），即未知前端路由交还 React Router | **executed ✅** |
| P-17 | 容器清理 | `docker compose rm -sf app` | 已执行；**MySQL 容器与数据卷保留** | **executed ✅** |

> 10 条深链（P-3..P-12）与 issue 正文列出的路径**逐条对应**（正文写 `/movies/:id`，此处用真实 id `tt9000001` 实例化）。

### 0.4 认证 / 会话语义（AC-4 / #44 AC-7）

| # | 项目 | 方法 | 结果摘要 | 状态 |
| --- | --- | --- | --- | --- |
| A-1 | 会话固定防护：`changeSessionId()` 生效 | 连续两次顾客登录，比对响应 `Set-Cookie` 中的 `JSESSIONID` | **JSESSIONID 发生变化**（第二次登录后不再是登录前的值）→ `changeSessionId()` 在 Boot 3 + Tomcat 10 下按预期工作 | **executed ✅（smoke 观测）** |
| A-2 | 登录后会话端点 | `GET /api/boot/session/customer`、`GET /api/boot/cart` | 均 **200** | **executed ✅** |
| A-3 | 未认证 401 body 形状 | 见 P-13 | `{"errorMessage":"Authentication required."}`（单一 `errorMessage` 字段，无 Spring 默认 `timestamp/status/path` 结构） | **executed ✅** |
| A-4 | 员工登录 + 会话 | `POST` 员工登录 → `GET /api/boot/dashboard/session` | **200** | **executed ✅** |
| A-5 | 员工功能端点 | `GET /api/boot/dashboard?action=metadata` | **200** | **executed ✅** |
| A-6 | 员工登出返回 204 | `GET /api/boot/dashboard/logout` | **204** | **executed ✅** |
| A-7 | 员工登出**只清员工态、保留顾客 `user`** | 登出后再查顾客会话 | 顾客 session **仍 200**，`user` 属性**保留** | **executed ✅（smoke 观测 + 既有单测证据）** |
| A-8 | 单测侧的会话证据 | `mvn verify`（见 B-1） | 134 用例中含登录/登出/401 形状相关断言（本步未新增用例） | **executed ✅** |

### 0.5 JDBC 语义（#44 AC-6 的 JDBC 子句；驱动 9.7.0 → **条件分支已触发**）

**时区 / 日期口径（重要，避免误读为「日期漂移」）**：

- **主机（macOS，PDT）**：`date +%F` = **2026-10-21**（约 22:45 PDT）；
- **容器 JVM 与 MySQL 服务均为 UTC** → 容器内「今天」= **2026-10-22**；
- 因此本次观测到 **`order_date` = `sales.sale_date` = 2026-10-22**，即**同一时区（UTC）下两条独立路径（应用响应 / 数据库 `CURDATE()`）一致** —— 这是**一致性证据**，**不是**时区偏移或 `CURDATE()` 漂移；
- **跨时区 / 跨日边界仍未构造**（既有边界，S2 日志已记录并顺延；本次同样不覆盖）。**不得**把「主机日期 10-21 / 库内日期 10-22」的差异解读为缺陷，也**不得**把本次观测解读为跨日边界已验证。

| # | 项目 | 方法 | 结果摘要 | 状态 |
| --- | --- | --- | --- | --- |
| J-1 | 顾客下单链路 | 加购 → `checkout` token → 支付 | **支付成功**（HTTP 层成功响应，非错误分支） | **executed ✅** |
| J-2 | 响应中的日期 | 支付响应体 | `order_date` = **2026-10-22**（容器 UTC） | **executed ✅** |
| J-3 | 库内日期（第二数据源） | 查 `sales` 最新行 | `sales.id` = **13567**、`sale_date` = **2026-10-22** | **executed ✅** |
| J-4 | 双源一致性 | 比对 J-2 / J-3 | `order_date` **==** `sale_date` = 2026-10-22 → 无时区偏移、无 `CURDATE()` 跨日漂移 | **executed ✅** |
| J-5 | 无新增致命 SSL / 连接告警 | 容器日志：`docker compose logs app \| grep -Ei "ssl\|fatal\|severe\|exception"` | **匹配数 0** | **executed ✅** |
| J-6 | 驱动版本前提 | `pom.xml` 显式 pin | `mysql-connector-j` **9.7.0**（S2 定版，S3/S5 未改）→ AC-6 的「驱动升到 9.x」条件分支**已触发**，不得以「驱动未变」结案 | **executed（只读核对）✅** |
| J-7 | 跨时区 / 跨日边界 | —— | **未构造**（需人为改时区或跨零点采样；S2 已列为既有边界） | **not executed（既有边界，非本步回归）** |
| J-8 | 连接池语义 / 真实并发 | —— | **未构造并发负载**（见 §0.7） | **not executed** |

> **数据性质提示**：`sales` 的测试行 **13562–13567** 属 dev 数据（S2/S3/S4 期间的 smoke 与本次 `13567` 连续），**不是**生产数据；本文件的日期一致性结论只针对这条 dev 记录。

### 0.6 工具链 / 制品一致性（AC-5 / #44 AC-11）

| # | 项目 | 命令 | 结果摘要 | 状态 |
| --- | --- | --- | --- | --- |
| T-1 | Maven 运行时 JDK | `mvn -version` | **Java 21.0.12.1**（`JAVA_HOME=/opt/homebrew/opt/openjdk@21`） | **executed ✅** |
| T-2 | 字节码版本 | `javap -verbose -cp target/classes com.fabflix.boot.FablixApplication \| grep 'major version'` | **major version: 61**（= Java 17 目标，`maven.compiler.release=17`） | **executed ✅** |
| T-3 | 镜像三阶段均为 JDK 21 / Node 22 | `grep -n "^FROM" Dockerfile` | `node:22-alpine AS frontend` · `maven:3.9-eclipse-temurin-21 AS build` · `eclipse-temurin:21-jre AS runtime`（本轮只读复核逐行确认） | **executed ✅** |
| T-4 | 容器内实际 JDK | 容器内 `java -version` | **21**（与 T-3 的 runtime stage 一致；运行用户非 root `1001`） | **executed ✅** |
| T-5 | enforcer 的 JDK 21 规则生效 | `grep -c requireJavaVersion pom.xml` + `sed -n '90,112p' pom.xml` | 主 `<build>` 的 `maven-enforcer-plugin:3.5.0` 执行 `require-jdk-21`（`validate` 阶段、`requireJavaVersion [21,22)`、`fail=true`）**存在且生效**（S4 已用 JDK 27 → `validate` BUILD FAILURE 反证）。**口径澄清**：`grep -c requireJavaVersion pom.xml` = **2 行**是**同一个规则元素的开/闭标签**（**1 处规则**），并非主 build + profile 各一处；`frontend` profile 里另有 `require-frontend-dist`（`requireFilesExist`，校验 `frontend/dist/index.html`），与本规则不同 | **executed ✅（含口径更正）** |
| T-6 | JaCoCo `excludes` 与基线一致 | 见 B-3 | **8** | **executed ✅** |
| T-7 | CI 的 JDK 21（由 S3 落地） | `.github/workflows/ci.yml` | job `Build and test (Java 21)` + 前端 job；PR #58 的 CI **双 job SUCCESS**（S5 回填） | **executed ✅（CI 结果由主 agent 核验）** |

### 0.7 未执行清单（汇总；**不得**读作通过）

| 项目 | 状态 | 原因 |
| --- | --- | --- |
| **真实浏览器多标签 / 并发** | **not executed** | jsdom 无真实渲染；Playwright 只跑既定 6 用例 × 2 viewport，未构造多标签/并发场景（既有边界，契约文档 §7） |
| **视觉 / 对比度回归** | **not executed** | 无布局引擎（jsdom），视觉背离无法自动化；沿用既有手工边界 |
| **真实 MySQL 并发 / 连接池语义** | **not executed** | 本次 smoke 为**顺序**单用户请求；未构造并发负载，HikariCP 6.3.3 的池淘汰/超时/并发语义**未被观测**（S3 已登记的顺延项） |
| **生产 reCAPTCHA 路径** | **not executed** | `GoogleRecaptchaVerifier` 仍 **0/17** 覆盖（`@Profile("!local")`），smoke 未配置真实 reCAPTCHA 凭据、未触达 Google 校验端点；另有 Boot 3 弃用（`RestTemplateBuilder.setConnectTimeout` / `setReadTimeout`，整份构建日志中剩余 **2** 条弃用即来自此类） |
| **员工 `?action=add_movie` 真实存储过程往返** | **not executed** | 本次 smoke 只触达 `session` / `metadata`；`AdminRepository`（0 covered / 39 missed）的 `add_movie` 解析与存储过程未被真实驱动 |
| **跨时区 / 跨日边界** | **not executed** | 见 §0.5 口径说明（需人为改 TZ 或跨零点采样；S2 已记录为既有边界） |

---

## 1. 修改了哪些文件及潜在问题

### 1.1 本步实际改动的文件（**仅 `docs/`**）

| 文件 | 变更目的 |
| --- | --- |
| `docs/logs/2026-10-21-issue-51-regression-proof.md` | **本文件（新增）** —— issue #51 Scope 要求的单一证据记录：逐项命令 / 结果 / executed 或 not executed，覆盖后端、前端、生产 smoke、认证会话、JDBC、工具链/制品 |
| `docs/plans/issue-44-migration-checklist.md` | S6 行状态 → **已执行（2026-10-21），待 PR 合并**；补证据路径；在 S7 段前注明「S6 证据已产出，S7 只做文档收尾」；S5 行改为**已完成（PR #58 已合并 @ `ff57e79`，CI 双 job SUCCESS）** |
| `docs/plans/issue-44-migration-steps.md` | §3 步骤总表 S5 / S6 行与 §4-S5、§4-S6 的 DoD 逐条回填（含本证据记录路径、未执行项） |
| `docs/issues/issue-51-regression-proof.md` | 状态 → **已执行（2026-10-21），待提交 / PR**；AC-1..AC-7 逐条回填（AC-6 指向本证据记录、AC-7 说明 PR 仅含 `docs/`）；`## GitHub issue body` 原文**未改** |
| `docs/issues/issue-50-mockito-annotation-cleanup.md` | S5 回填：**已合并**（PR #58 / merge `ff57e79`）、CI 双 job SUCCESS、#50 CLOSED、AC-1..AC-5 ✅ |
| `docs/issues/issue-44-boot3-java17-jakarta-migration.md` | 进展块补「S5 已合并（PR #58 @ `ff57e79`）」与「S6 证据已产出、待 PR」；`## GitHub issue body` 原文**未改** |

- **零改动（刻意不做）**：`src/main/**`、`src/test/**`、`pom.xml`、`.github/workflows/ci.yml`、`Dockerfile`、`compose.yaml`、`frontend/**`、`docs/architecture/frontend-migration-contracts.md`（架构文档归 **S7**）、历史日志（`docs/logs/2026-10-*`）。
- **提交纪律**：工作区既存的未跟踪 `?? .idea/` **不属本次改动**，不得带入提交。

### 1.2 潜在问题、遗留风险与后续事项

1. **未执行项是真实缺口，不是措辞问题**（§0.7）：并发/池语义、生产 reCAPTCHA、`AdminRepository` 存储过程往返、跨时区边界。**S7 关闭 #44 时必须显式引用本清单**，不得写「全部验证通过」。
2. **「零弃用」不成立**：构建日志仍有 **2** 条弃用警告（`GoogleRecaptchaVerifier` 的 `setConnectTimeout` / `setReadTimeout`，Boot 3 已弃用该 API）。S5 的成果只能表述为「`MockBean` 相关弃用 **11 → 0**」。
3. **覆盖缺口原样保留**：`AdminRepository` 0/39、`GoogleRecaptchaVerifier` 0/17 —— 本步**不新增测试**（非目标），改造它们属独立后续 issue。
4. **日期证据的可复读性**：`sales` 的 13562–13567 是 **dev 数据**；跨日/跨时区仍未验证（§0.5）。若未来要证明时区正确性，需构造受控跨 TZ 采样，而不是复用本记录。
5. **S6 尚未合并**：本文件所在分支未提交/未推送、**无 PR**；PR 的 CI 结果与 #51 的远端关闭都**尚未发生**。在 PR 合并前，checklist/issue 文档中一律写「已执行（2026-10-21），待 PR 合并」。
6. **T-5 的口径更正**：本步核对发现「enforcer `requireJavaVersion` 在 `pom.xml` 出现 2 次（主 build + profile）」的说法**不成立** —— 实际是**1 个规则元素**（`grep -c` 的 2 是开/闭标签），profile 里是另一条 `require-frontend-dist`。该更正在本文件与 checklist 中体现；结论「enforcer 规则生效」不受影响。
7. **无已知功能缺陷**：本次 smoke 未发现任何迁移后行为差异；若后续审查认为某项「未执行」必须补证，需回主 agent 决定是否重开取证（本步不改源码）。

---

## 2. 新增知识点 / 重点学习对象

### 2.1 生产 smoke 的「取证设计」而非「点一遍」（Spring Boot 3 / Docker）

- **含义**：S6 的价值不在「跑通」，而在**每一次观测都对应一条可复核的判据**：状态码 + `Content-Type` + 响应体特征（`id="root"`、`{"errorMessage":…}`、`index.html`）。这样 Boot 3（Tomcat 10.1 / Jackson 2.21 / HikariCP 6.3.3）的**行为差异**才会以「判据不符」的形式暴露，而不是被「页面能打开」掩盖。
- **对应位置**：§0.3 的 P-1..P-16；命令面 = `curl -s -o /dev/null -w '%{http_code} %{content_type}'`（状态 + 类型同时取）＋ `curl -s | grep -q 'id="root"'`（内容判据）。
- **学习价值**：迁移类工作的验收是**真值表比对**，不是「能用就行」。**负向判据**尤其重要：`/API/boot/cart` → 404（而非 401）证明未认证过滤器只精确匹配 `/api/boot/…` 路径；`/assets/missing.js` → 404（而非 `index.html`）证明 SPA fallback 没吞静态资源请求。

### 2.2 会话固定攻击防护的「可观测」验证：`changeSessionId()`

- **含义**：登录成功后 Spring Security/自研过滤器调用 `HttpServletRequest#changeSessionId()`（Servlet 3.1+），**保留会话数据但更换会话 ID**，防止攻击者预置的 JSESSIONID 在登录后被沿用。
- **观测方法**：连续两次登录，抓 `Set-Cookie` 里的 `JSESSIONID` 值比较 —— 变化即证明换了 ID；只看「登录成功」无法证明该保护生效。
- **对应位置**：§0.4 的 A-1；单测侧由 134 用例中的登录用例兜底。
- **学习价值**：**安全语义要用「ID 变了、数据还在」两个观测点同时证明**，缺一不可（只换 ID 但丢 `user` 属性 = 登录态丢失；不换 ID = 会话固定漏洞仍在）。

### 2.3 员工登出的「部分登出」语义：204 + 顾客 `user` 仍保留

- **含义**：`GET /api/boot/dashboard/logout` 返回 **204**（无响应体，REST 语义），但**只清员工态**；同一浏览器里的顾客 `user` 属性**仍存在**。这是本项目的既有行为，不是 Bug。
- **验证方式**：登出 → 立刻请求顾客端点，仍 **200**（若误清整个 session 会是 401）。
- **对应位置**：§0.4 的 A-6 / A-7。
- **学习价值**：「登出」的语义是**按角色边界**定义的，迁移时最容易在会话实现细节上被悄悄改变；用「204 状态 + 另一角色仍可用」两个判据才能锁死它。

### 2.4 日期一致性要走「双源核对」＋时区口径

- **含义**：应用写库时用 SQL `CURDATE()`（`PaymentRepository`），响应里的 `order_date` 由应用侧产生 —— 两条独立来源。核对 `order_date == sales.sale_date` 才能排除「时区偏移」和「`CURDATE()` 与 JVM 日期不一致」。
- **关键坑**：**主机与容器的时区不同**（主机 PDT = 10-21，容器/MySQL UTC = 10-22）。同一时刻两个「今天」不同，**不等于**漂移；报告必须写清「谁在哪个时区观测到了什么」，否则很容易被后人误读成缺陷。
- **对应位置**：§0.5 的时区口径块 + J-1..J-4。
- **学习价值**：时间类证据的**可复读性**取决于口径（观测方时区、数据源、样本 id）。写「2026-10-22 一致」而不写时区，等于把证据变成噪声。

### 2.5 容器日志作为「无告警」证据：`grep -Ei "ssl|fatal|severe|exception"` = 0

- **含义**：驱动/JDK/容器升级最典型的隐性回归是**启动期 SSL/握手告警**和**运行期异常**，它们通常不影响 HTTP 200，但会在日志里留痕。`grep` 计数 = 0 才可作为「无新增告警面」的证据。
- **对应位置**：§0.5 的 J-5。
- **学习价值**：**「没有匹配」也是证据，但必须给出模式**。`ssl|fatal|severe|exception` 同时覆盖 TLS 协商、JUL/Logback 的 `SEVERE/FATAL` 级别与异常栈；只写「日志正常」无法复核。

### 2.6 Maven enforcer 的「规则元素 vs 文本出现次数」

- **含义**：`grep -c requireJavaVersion pom.xml` = 2 统计的是**开/闭标签两行**，实际规则元素只有 **1 个**（`require-jdk-21`，`validate` 阶段、`[21,22)`、`fail=true`）；`frontend` profile 内的 `require-frontend-dist` 是**另一条**规则（`requireFilesExist`）。
- **对应位置**：§0.6 的 T-5（含口径更正）、`pom.xml` L91–L111 与 L200–L224。
- **学习价值**：**用 grep 计数当证据时必须区分「标签行数」与「语义实体数」**；否则会得出「规则重复出现」的错误结论。这也是本步唯一一处对既有记录的更正。

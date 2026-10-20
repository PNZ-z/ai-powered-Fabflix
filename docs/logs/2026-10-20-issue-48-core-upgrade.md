# 工作流日志 / 实施记录 — Issue #48（S3：Spring Boot 3.5 + `release=17` + Jakarta 核心升级）

> 本文件是 **S3（[#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)）的实施、验证与记录**，并按 `AGENTS.md` §8 承载本工作流日志的两大节：
> **一、修改了哪些文件及潜在问题**（§一）；**二、新增知识点 / 重点学习对象**（§二）。
>
> **口径声明**：§0 与 §1.3 中标注「worker / 主 agent」的结果由**单一 `worker` 与主 agent 亲自运行并核验**（原始报告 `.pi/artifacts/worker-issue-48-core-upgrade.md`、补强报告 `.pi/artifacts/worker-issue-48-core-upgrade.04d1a650.md`、审查 `.pi/artifacts/reviewer-s3-necessity-retry.md`）；`docs-writer` **没有**运行 `mvn` / npm / Docker / 应用 smoke / 浏览器 / CI，只做了**只读复核**（`git diff`/`git status`、`grep` 计数、`target/site/jacoco/jacoco.csv` 汇总、`target/surefire-reports/*.xml` 汇总、`target/classes` 字节码头、`target/fablix.jar` 内 `BOOT-INF/lib` 清单、`CustomerAuthenticationFilter` 源码）与文档写入，**未执行任何 GitHub 写操作**。

## 元数据

| 项目 | 值 |
| --- | --- |
| 记录日期 | **2026-10-20**（实施、验证与记录同日，本机时区 America/Los_Angeles / PDT） |
| Issue | **#48** — https://github.com/PNZ-z/ai-powered-Fabflix/issues/48（`[Step 3/7] Upgrade to Spring Boot 3.5 and migrate to the Jakarta namespace`，milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)，labels 空，state **OPEN**） |
| 上游 epic | [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（epic，**OPEN**）；前置 [#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)（基线）、[#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)（驱动就绪） |
| 分支 | **`codex/issue-48-core-boot3-jakarta`**（基于 `main` @ `55f8d79` = `Merge pull request #55 from PNZ-z/codex/issue-47-dependency-readiness`） |
| Commit SHA | **无本步提交**（工作区改动未暂存；`git diff --cached` 为空） |
| 提交 / 推送 / PR | ~~**未提交、未推送、无 PR**。远端 `main` 未受影响；**#48 保持 OPEN**；checklist 中 S3 记为「已实施（2026-10-20），待 PR 合并」~~ → **合并回填（2026-10-20，主 agent 核验）**：PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) **已合并**（merge commit **`70fb78b`**），CI 双 job（`Build and test (Java 21)` + `Frontend (lint, test, coverage, build, e2e)`）**SUCCESS**，**#48 已 CLOSED**；checklist 中 S3 现记为「已完成（PR #56 已合并 @ `70fb78b`）」。本文件 §0/§1 的「无 PR」表述与本行第 11 项属**合并前状态**，保留为历史记录 |
| 树状态（本轮结束） | `M` 18 个文件（`git diff --stat` = **18 files changed, 85 insertions(+), 38 deletions(-)**）＋本轮新增/更新的 `docs/` 文件；另有**开工前既存**的未跟踪 `?? .idea/`（IDE 目录，非本步产物）与 `.pi/artifacts/`（不在 `docs/`，不入提交） |
| 构建工具链 | Apache Maven **3.10.0**；`JAVA_HOME=/opt/homebrew/opt/openjdk@21` → **Java 21.0.12.1**（Homebrew） |
| 目标运行形态 | Spring Boot **3.5.15** / `maven.compiler.release=17`（class file **major 61**）/ Jakarta Servlet 命名空间 / 容器镜像 JDK **21**（build + runtime） |
| 数据库环境 | 容器 `fabflix-mysql`（`mysql:8.4`，`0.0.0.0:3307->3306`，`Up 7 days (healthy)`）；**未执行 `docker compose down`**（开工前即运行，保持原状） |
| 关联计划 / checklist | `docs/plans/issue-44-migration-checklist.md`（状态总表、§S3、§S4 预期值）、`docs/plans/issue-44-migration-steps.md`（§3 总表 S3 行、§4-S3 DoD） |
| 关联 issue 文档 | `docs/issues/issue-48-core-boot3-jakarta.md`（#48 的规范来源与 AC 回填）、`docs/issues/issue-44-boot3-java17-jakarta-migration.md`（epic / AC-6） |
| 证据来源 | worker 报告（`.pi/artifacts/worker-issue-48-core-upgrade.md`）、补强报告（`.pi/artifacts/worker-issue-48-core-upgrade.04d1a650.md`）、必要性审查（`.pi/artifacts/reviewer-s3-necessity-retry.md`）、**主 agent 终验与容器 smoke**、docs-writer 只读复核（§0 的 E11） |

---

## 0. 验证命令与结果汇总（证据索引）

> 「执行者」列区分 **worker / 主 agent**（实际运行）与 **docs-writer**（只读复核）。全部 Maven 命令均带 `JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=…/bin:$PATH`。

| # | 验证项 | 关键命令（节选） | 结果 | 执行者 |
| --- | --- | --- | --- | --- |
| **E1** | 后端门禁 | `mvn --batch-mode --no-transfer-progress clean verify` | **BUILD SUCCESS**；`Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`；**`All coverage checks have been met.`**；JaCoCo **988 covered / 90 missed / 1078 = 0.9165**（门槛 0.90） | worker（终验）；主 agent **复跑通过**；docs-writer 复核 csv 与 44 个 surefire XML = `134/0/0/0` |
| **E2** | 守卫测试 | `mvn … test -Dtest=SpringBootOnlyArchitectureTest` | **BUILD SUCCESS**；`Tests run: 2`（**仍 2 个 `@Test`**）；白名单**集合相等**（恰好 11 路径）通过；`@WebServlet` / `extends HttpServlet` / `jakarta.servlet.annotation.WebServlet` 禁令生效 | worker（含 2 行补强后复跑） |
| **E3** | Jakarta 边界（双判据） | `grep -rl "jakarta.servlet" src/main/java \| wc -l`；`grep -rn "javax.servlet" src/main/java`；`grep -rl "javax\.\(sql\|xml\)" src/main/java` | `jakarta.servlet` = **11**（路径与守卫白名单逐条一致）；`javax.servlet` = **0**；`javax.sql`/`javax.xml` = **4**（`AdminRepository` + 3 个 XML parser，**未动**） | worker；docs-writer 复核 11/0/4 |
| **E4** | 打包形态（正向） | `cd frontend && npm ci && npm run build`；`mvn -B -Pfrontend -DskipTests clean package`；`unzip -l target/fablix.jar` | 均 exit 0；jar 含 **`BOOT-INF/classes/static/index.html`**（463 B）与 `static/assets/...`（`index-BINlLyG3.css` 11.49 kB、`index-CbFV0THv.js` 309.13 kB） | worker；主 agent 复跑 |
| **E5** | 打包形态（负向，dist 守卫） | `mv frontend/dist /tmp/dist.bak && mvn -Pfrontend -DskipTests package; echo "exit=$?"` | **exit=1 / BUILD FAILURE**：`enforcer:3.5.0:enforce (require-frontend-dist)` → `frontend/dist/index.html is missing…`；随后已恢复 `frontend/dist` | worker |
| **E5b** | 默认 `verify` 不依赖 dist | 移走 `frontend/dist` 后 `mvn verify`（**无** `-Pfrontend`） | **BUILD SUCCESS**，`Tests run: 134`，覆盖率仍 988/1078 = 0.9165 → 后端门禁不依赖 Node/`dist` | worker |
| **E6** | 工具链 + 字节码 | `mvn -version`；`javap -verbose -cp target/classes com.fabflix.boot.FabflixApplication \| grep 'major version'` | `Java version: 21.0.12.1`（`/opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home`）；**`major version: 61`** | worker；主 agent 复核；docs-writer 用 `xxd -l 16 target/classes/…/FabflixApplication.class` → `cafe babe 0000 003d`（**0x3d = 61**）复核 |
| **E7** | 容器构建 + 运行（B-1） | `docker compose build app`；`docker compose up -d --no-deps app`；`docker exec fabflix-app java -version` / `id fabflix` | build exit 0（`maven:3.9-eclipse-temurin-21@sha256:99e61ab…`、`eclipse-temurin:21-jre@sha256:2271187…`）；容器内 `JAVA_VERSION=jdk-21.0.12.1+1`、`openjdk version "21.0.12.1"`；`uid=1001 gid=1001`（非 root 保持）；**health 200**；随后 `docker compose rm -sf app` | worker（初轮）+ **主 agent（提交版 Dockerfile 亲自复跑）** |
| **E8** | 容器 smoke（HTTP 语义与业务链路） | `curl` 逐项（8081） | `/` 200 `text/html` 含 `id="root"`；**10 条深链全部 200 `text/html`**（`/home` `/movies` `/movies/tt123` `/cart` `/checkout` `/confirmation` `/login` `/employee/login` `/employee/dashboard` `/playground`）；未认证 `/api/boot/cart` → **401 JSON**；`/API/boot/cart` → **404 JSON**；`/assets/missing.js` → **404**；顾客登录 → 加购 → token → **支付成功**（响应 `order_date=2026-10-20`，库内 `sales.id=13566`、`sale_date=2026-10-20`）；员工登录 → `/dashboard/session` 200 → `?action=metadata` 200；容器日志 `grep -iE "warn\|error\|deprecat"` **无输出** | **主 agent（提交版镜像）** |
| **E9** | 依赖解析（不下降/未被 BOM 覆盖） | `mvn dependency:list`（test scope） | `spring-boot:3.5.15`；`com.mysql:mysql-connector-j:9.7.0`（**S2 显式 pin 仍生效**）；Tomcat **10.1.55**、Jackson **2.21.4**、HikariCP **6.3.3**、Logback **1.5.34**；`junit-jupiter:5.11.4`（聚合 pin）vs `junit-jupiter-engine:5.12.2`（BOM）；`mockito-core:5.23.0` vs `mockito-junit-jupiter:5.17.0`（BOM）；`byte-buddy{,-agent}:1.17.7` | worker；主 agent；docs-writer 用 `unzip -l target/fablix.jar \| grep BOOT-INF/lib` 交叉核对 6 项（`spring-boot-3.5.15`/`mysql-connector-j-9.7.0`/`tomcat-embed-core-10.1.55`/`jackson-databind-2.21.4`/`HikariCP-6.3.3`/`logback-classic-1.5.34`）一致 |
| **E10** | 域门禁不变 | `grep -c "<exclude>" pom.xml`；`grep -n "<minimum>" pom.xml` | **8**（未扩）；`<minimum>0.90</minimum>`（未降） | docs-writer 复核 |
| **E11** | docs-writer 只读复核 | `git status --short` / `git diff`（逐文件、逐行过滤）/ `git diff --cached` | 18 文件全部落在批准边界内；`src/main/java` 的 diff **只有** `javax.servlet.* → jakarta.servlet.*` import 行（`HttpSession` 11、`HttpServletRequest` 5、`HttpServletResponse` 3、`ServletException` 2、`FilterChain` 2）；`src/test/java` 的 3 个文件各 1 行 import；守卫测试为唯一含逻辑改动的测试文件；**无 staged 文件** | docs-writer |

**未证明 / 未执行（不得视为已通过）**：① **PR CI 双 job**（尚无 PR，AC-6 未取证）；② HikariCP 4→6 的**连接池语义与并发**（未做池指标/并发观测）；③ 生产 **reCAPTCHA** 路径（无测试、无 Boot 3 运行证据）；④ 静态深链的**尾斜杠**场景（归 S6）；⑤ 驱动 `DATE`→Java 的读取映射、跨日/TZ 边界、TLS 实际协商状态（S2 已顺延 S6，本步未触及）。

---

## 一、修改了哪些文件及潜在问题

### 1.1 代码/配置改动（**18 个文件**，`git diff --stat` = 85 insertions / 38 deletions）

| # | 文件（组） | 变更 | 目的 |
| --- | --- | --- | --- |
| 1 | `pom.xml` | `maven.compiler.source/target=11`（2 行）→ **`<maven.compiler.release>17</maven.compiler.release>`**（`:13`）；`spring-boot.version` `2.7.18` → **`3.5.15`**（`:22`） | 唯一一次原子升级的构建面。BOM import（`:30`）与 `spring-boot-maven-plugin`（`:97`）都引用 `${spring-boot.version}`，实测执行日志为 `spring-boot:3.5.15:repackage` |
| 2–12 | **11 个 `src/main/java`**（commerce 5：`CartService`/`CartController`/`CheckoutController`/`CheckoutTokenService`/`PaymentController`；auth 2：`CustomerAuthController`/`CustomerAuthenticationFilter`；admin 2：`EmployeeController`/`EmployeeAuthenticationFilter`；search 1：`SearchController`；session 1：`SessionController`） | **只改 import**：`javax.servlet.*` → `jakarta.servlet.*` | Spring Boot 3 的 `spring-boot-starter-web` 只提供 Jakarta API，`javax.servlet` 无法编译；**无业务逻辑改动**（docs-writer 逐行过滤复核：diff 中除 import 外无其他行） |
| 13 | `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java` | 重写守卫：新增 `JAKARTA_SERVLET_ALLOW_LIST`（**恰好 11 条相对路径**）+ `assertEquals(new TreeSet<>(…), filesContaining("jakarta.servlet"))` **集合相等**；移除旧的反向禁令 `assertFalse(source.contains("jakarta.servlet"))`；保留 `@WebServlet` / `extends HttpServlet`；新增 `jakarta.servlet.annotation.WebServlet` 导入禁令；保留 pom 的 `jar` / `repackage` / 无 `jakarta.servlet-api` 断言 | 把「禁止 Jakarta Servlet」反转成**受控白名单**：新增/缺失文件都会失败（`contains` 会随白名单悄悄变大而仍绿） |
| 13b | 同上（**主 agent 终验后补强，2 行**） | `assertFalse(source.contains("javax.servlet"), …)`（`src/main/java` 全量扫描）；`assertFalse(pom.contains("javax.servlet-api"))` | 三路 review 只接受这一项代码补强：**被淘汰的 `javax.servlet` 不得从源码或 pom 回归**。补强后**仍是 2 个 `@Test` 方法**，`Tests run` 仍 134 |
| 14–16 | **3 个 `src/test/java`**：`commerce/CartServiceTest`（`:9`）、`web/SpaFallbackTest`（`:15`）、`admin/EmployeeAuthenticationFilterTest`（`:4`） | **只改 import**（`javax.servlet.*` → `jakarta.servlet.*`） | **编译必需**：Boot 3 的 `spring-test` 只有 `jakarta.servlet` 类型（`MockHttpSession`/`MockServletContext`/`FilterChain`），保留 `javax` 声明会导致 `testCompile` 直接失败（worker 首次 `verify` 实测 10 个编译错误）。**不在 AC-2(a) 的 `src/main/java` 边界内**，已在 issue 文档与本表逐条登记 |
| 17 | `.github/workflows/ci.yml` | job 名 `Build and test (Java 11)` → **`(Java 21)`**（`:20`）；step 名 `Set up Java 11` → **`Set up Java 21`**（`:28`）；`java-version: "11"` → **`"21"`**（`:32`） | Spring Boot 3.5 需要 JDK 17+；**不随本 PR 改动则 PR 自身 CI 与合并后的 `main` 立即红**。其余步骤/缓存/前端 job 未动 |
| 18 | `Dockerfile` | build `maven:3.9-eclipse-temurin-11` → **`…-21`**（`:12`）；runtime `eclipse-temurin:11-jre` → **`eclipse-temurin:21-jre`**（`:21`） | JDK 11 的 `javac` 不能 `--release 17`，JRE 11 不能加载 class file 61；**不改则 `docker compose build app`（文档化生产路径）立即损坏而 CI 仍绿**（oracle B-1）。`uid/gid 1001` 非 root 块、`EXPOSE 8081`、`ENTRYPOINT`、frontend 阶段与 `test -f dist/index.html` 守卫**均未动** |

**明确未改动**（逐项确认）：`src/main/java` 的 `javax.sql`/`javax.xml` 4 个文件（`AdminRepository.java`、`ActorXMLParser.java`、`CastXMLParser.java`、`MainXMLParser.java`——这些包是 JDK 自带的，不属于 Jakarta EE 9 改名范围）；`README.md`；`compose.yaml`；`frontend/src/**`、`frontend/e2e/**`；`database/**`；JaCoCo `<exclude>`（仍 **8** 项）与 `<minimum>0.90</minimum>`；`@MockBean` 注解（11 处，S5）；`maven-enforcer-plugin` 主 `<build>` 规则（S4）；`mysql-connector-j` 显式 `9.7.0`（S2 的裁决，未被 Boot 3.5.15 BOM 覆盖）；其他依赖/插件版本（`junit.version=5.11.4`、`mockito.version=5.23.0`、`byte-buddy=1.17.7`、`jacoco=0.8.15`、`surefire=3.5.2`）。

### 1.2 文档改动（本次 docs-writer 写入）

| 文件 | 变更类型 | 目的 |
| --- | --- | --- |
| `docs/logs/2026-10-20-issue-48-core-upgrade.md` | **新增**（本文件） | S3 的实施/验证记录：元数据、证据索引、行为差异与未证明项、残余风险、两大节总结 |
| `docs/plans/issue-44-migration-checklist.md` | **更新** | 状态总表 S3 行 → 「已实施（2026-10-20），待 PR 合并」＋分支/证据；§S3 补「实际执行结果」块（版本/134/0.9165/major 61/11 文件/白名单/CI+Docker/容器 smoke/依赖解析/已知项）；§S4 预期值注明「Boot 3 已就绪，S4 只加 enforcer + README + 取证」；顶部「本文件不表示任何步骤已完成」按事实同步 |
| `docs/plans/issue-44-migration-steps.md` | **更新** | 顶部「实施进展」补 S3；§3 总表 S3 行补状态与证据；§4-S3 追加「实际结果」逐条回填 DoD 1–9 |
| `docs/issues/issue-48-core-boot3-jakarta.md` | **更新** | 状态行与 Internal tracking 状态 → 「已实施（2026-10-20），待提交/PR」；新增 **AC-1..AC-9 逐条回填**（AC-6 的 PR CI 部分**明确待 PR**）；记录主 agent 对守卫的 2 行补强与 3 个测试 import 的必要性；记录 `FablixApplication` 笔误；**`## GitHub issue body` 围栏原文未改** |
| `docs/issues/issue-44-boot3-java17-jakarta-migration.md` | **更新（Internal tracking + 顶部进展行）** | AC-6 的 JDBC 子句保持「**已触发**」，补记 **S3 的 Boot 3 支付 smoke 证据**（`sales.id=13566`，`order_date` = `sale_date` = 2026-10-20）；顶部进展与「当前跟踪状态」改为「S3 已实施、S4–S7 未开工」；**已发布的 `## GitHub issue body` 围栏文本未改** |

**本轮刻意未改**：`docs/architecture/frontend-migration-contracts.md` 的 §8 / §11 / §12（守卫语义、镜像两阶段、CI JDK 的**契约文档同步属 S7 / #52**）；`docs/logs/2026-10-20-issue-46-baseline.md` 与 `2026-10-20-issue-47-dependency-readiness.md`（历史日志不追改）；`docs/plans/ai-option-a-boot3-migration.md`（已批准计划的迁移前状态描述属历史陈述）。

### 1.3 潜在问题、行为差异与残余风险

> 以下每一条都是**显式记录**的边界；任何一条都不构成「已验证通过」。**结论限定**：本步只证明「Boot 3.5.15 + `release=17` + Jakarta 在 JDK 21 上编译、134 个既有用例全绿、覆盖率与门槛不变、jar 形态与 dist 守卫不变、JDK 21 两阶段镜像可构建并完成一次真实顾客结算与员工 metadata 路径」。

1. **`/api/boot/health/`（尾斜杠）→ 401 不是 Boot 3 行为差异（归因已修正）** —— （worker 报告 §3.1 的原始归因错误，经主 agent 在**升级前基点树** `55f8d79` 实测否证：2.7.18 上同样 401）。真实原因在源码里：`CustomerAuthenticationFilter.shouldNotFilter`（`src/main/java/com/fabflix/boot/auth/CustomerAuthenticationFilter.java:17-26`）用**精确字符串比较** `path.equals("/api/boot/health")` 放行，`/api/boot/health/` 不等于该字符串 → 落进认证分支返回 401；**Servlet filter 在 handler mapping 之前执行**，所以与 Boot ≥3.2 移除尾斜杠匹配的默认策略无关。**结论：既有行为，S3 未改变、不修**（修它需要改过滤器匹配逻辑，属「HTTP 状态码/语义保持不变」之外的新行为，且该 URL 不在文档化路径内、前端不产生尾斜杠请求）。**静态深链的尾斜杠场景留 S6 取证**（未测量，不声明差异）。
2. **Boot 3 新引入的编译期弃用：`GoogleRecaptchaVerifier` 的 `RestTemplateBuilder.setConnectTimeout` / `setReadTimeout`**（`src/main/java/com/fabflix/boot/auth/GoogleRecaptchaVerifier.java:27-28`）。该类带 `@Profile("!local")`，覆盖 **0/17 行**（既有缺口，见 checklist 的「已知覆盖缺口」表），**本次未运行生产 reCAPTCHA**；因此这是一条**编译期**弃用警告，**没有任何运行期证据**。**未在本步修复**（不在「升级必需」范围，且改动该 API 需要替换为 `RestTemplate`/`ClientHttpRequestFactory` 的新构造方式——具体替代 API 本步**未核实**，不写成已定方案）。**顺延为可选后续修复，S6 记为风险**。
3. **`@MockBean` 11 条弃用警告**（Boot 3.4+ `marked for removal`）：**预期之内**，属 **S5（#50）**，本步未改一行注解。
4. **连接池 / 并发语义未专门验证**。本步 `mvn verify` 是 MockMvc（不连真实数据库），容器 smoke 只有十余次串行请求。因此「未观察到差异」**只限已测范围**：HTTP 状态码与 JSON 形状、启动日志无 warn/error/deprecat。**HikariCP 4.0.3 → 6.3.3 的池语义、并发与失败重试未取证 → 顺延 S6**（issue Risk notes 原本就把这一项交给 S6）。
5. **生产 reCAPTCHA 路径无测试、无 Boot 3 运行证据**（同第 2 条）：local profile 走 `LocalRecaptchaVerifier`，`GoogleRecaptchaVerifier` 从未实例化 → **顺延 S6**。
6. **显式 pin 与 BOM 管理的版本错配**（按 issue「keep the explicit pins unless a real conflict appears」保留不动）：`junit-jupiter:5.11.4`（聚合）vs **`junit-jupiter-engine:5.12.2`**（BOM 实际解析）；`mockito-core:5.23.0` vs **`mockito-junit-jupiter:5.17.0`**（BOM）。**且 `pom.xml:18-19` 的 `junit-jupiter.version` / `junit-platform.version` 属性当前对解析结果无影响**（全仓库无引用；实测 engine 5.12.2、platform 1.12.2 —— 属**升级前既有技术债**，不是本步引入）。`mockito-junit-jupiter` 的错配目前是**惰性**的：仓库未使用 `MockitoExtension`（只用 `@MockBean`）。**本步不改**（属未裁定项 D-A5 与既有债），登记为后续清理候选。
7. **javap 的「类找不到」不是失败**：本机 `/usr/bin/javap` 属于旧 JDK，读 class file 61 会报 `class not found: com.fabflix.boot.FablixApplication`。**可靠判据**：`JAVA_HOME` 指向 JDK 21 的 `javap`（worker/主 agent 已得到 `major version: 61`），或直接读字节码头 `xxd -l 16 target/classes/com/fabflix/boot/FabflixApplication.class` → `cafe babe 0000 003d`（**0x3d = 61**，docs-writer 独立复核）。**不要**因为旧 javap 报错就怀疑字节码级别。
8. **`FablixApplication` 是 issue 文本的笔误**：issue #48 的 AC-5 与 checklist 的验证命令写 `com.fabflix.boot.FablixApplication`，**实际类名为 `com.fabflix.boot.FabflixApplication`**（`src/main/java/com/fabflix/boot/FabflixApplication.java`，`pom.xml` 的 `mainClass` 同）。因此那条验收命令**照抄会得到「类找不到」**，必须用实际类名（或改用 `xxd` 判据）。已在 issue 文档与 checklist 中登记，**不改远端正文**（笔误不影响验收实质，远端编辑需主 agent 授权）。
9. **`target/fablix.jar` 的形态取决于最后一次构建命令**：E4 的「jar 内含 `static/index.html`」只有在执行过 `mvn -Pfrontend … package` 后成立；**默认** `mvn clean verify`（无 `-Pfrontend`）构建出的 jar **不含** `static/`。写文档时工作区的 `target/fablix.jar` 正是后者（`unzip -l … \| grep -c "BOOT-INF/classes/static/"` = **0**），**这不表示 E4 失败**，只表示当前 `target/` 不是那次打包的产物 —— 任何人复核 AC-3 都必须重跑 `-Pfrontend` 的打包命令。worker 的 E5b 恰好证明了「默认 `verify` 不依赖 dist」。
10. **dev 库又被写入 1 行测试数据**：容器 smoke 的支付成功写入 `sales.id=**13566**`（`customer_id=900001`，`sale_date=2026-10-20`）。连同 S2 的 4 行（13562–13565），dev 库现有 5 行本迁移产生的测试数据。清理 SQL：`DELETE FROM sales WHERE id IN (13562,13563,13564,13565,13566);`。`database/dev-seed.sql` **只在数据卷首次初始化**时执行，重跑容器不会清掉。**未执行 `docker compose down`**（MySQL 开工前即 `Up 7 days (healthy)`，保持原状；`down -v` 属破坏性操作，须主 agent 批准）。
11. **AC-6 未取证（PR CI 不存在）**：issue #48 的 AC-6 是「**PR 的 CI 双 job 绿且后端 job 名反映新 JDK**」。本步只完成了「文件已改（job 名 `(Java 21)`、step 名、`java-version: "21"`）」这一可审查的部分；**CI 是否真跑绿必须在 PR 出现后由主 agent 回填**。**不得**在此之前声称 AC-6 通过。（容器路径是 **AC-9**，已由主 agent 的 smoke 取证。）
12. **本步交付未闭环**：18 个文件的改动与本文档**均未提交、未推送、无 PR、未合并**；#48 仍 **OPEN**，远端 `main` 未变。任何「S3 已完成 / `main` 上已是 Boot 3」的说法在合并前都**不成立** —— 准确表述是「已实施（本地工作区，分支 `codex/issue-48-core-boot3-jakarta`），待 PR 合并」。
13. **提交时必须只 add 预期文件**：工作区另有**开工前既存**的未跟踪 `?? .idea/`（以及 `.pi/artifacts/`，不在 `docs/`）。提交范围应严格是这 18 个文件 + 本步 `docs/` 下的 5 个文件（本日志 + checklist + steps + issue-48 + issue-44），避免把 IDE 目录或 agent 产物带入。
14. **回退点仍是「单 PR 整体 revert」**：Jakarta 的 11 个 import 与守卫测试重写**必须成对回退**（只回退一侧会得到 `mvn verify` 恒红）；**BOM 升版、CI 的 JDK、`Dockerfile` 两行 `FROM` 三者也不得拆开**（任一单独回退都会让 CI 或生产路径损坏）。`release=17` 与这三者同属编译/运行前提。
15. **未发现其他已知遗留问题**：除上面 1–14 条外，本步没有观察到其他需要后续处理的问题；`javax.sql`/`javax.xml` 4 个文件未被误改，`excludes` 与阈值未被动，`compose.yaml` 与前端源码零改动（`git diff --stat` 可验证）。

---

## 二、新增知识点 / 重点学习对象

### Spring Boot 3 / Jakarta 迁移

- **Jakarta EE 9 的改名只覆盖 `javax.servlet` 这一支，`javax.sql` / `javax.xml` 属于 JDK**
  - 含义：`javax.servlet.*`（Servlet API）随 Jakarta EE 9 整体迁移到 `jakarta.*`，因此必须改；而 `javax.sql.DataSource`、`javax.xml.parsers.*` 是 **Java SE 自带的包**（从未属于 Jakarta EE），必须保留原样。判断「哪个 import 要改」不能靠 `javax` 前缀，要靠包归属。
  - 位置：改动的 11 个文件（`commerce` 5 / `auth` 2 / `admin` 2 / `search` 1 / `session` 1）vs 未动的 `admin/AdminRepository.java`（`javax.sql`）与 3 个 `*XMLParser.java`（`javax.xml`）
  - 学习价值：迁移事故的典型来源就是「无差别 `sed s/javax/jakarta/`」，它会立刻把 XML parser 与 DataSource 打坏，且错误信息（`package jakarta.xml.parsers does not exist`）会误导成「依赖缺失」。
- **为什么这一步是「编译层面原子」的**
  - 含义：Boot 3 的 `spring-boot-starter-web` 只带 `jakarta.servlet-api`，此时 11 个 `javax.servlet` 源文件直接编译失败；而仓库的守卫测试原本**禁止** `jakarta.servlet` 出现，不反转守卫则 `mvn verify` 永远红。两头夹住 ⇒ BOM 升版、import 改写、守卫反转（以及 `ci.yml` 的 JDK、`Dockerfile` 的两行 `FROM`）**只能同一个改动里完成**。
  - 位置：`pom.xml:30`（BOM import）、`SpringBootOnlyArchitectureTest.java`、`ci.yml:20/28/32`、`Dockerfile:12/21`
  - 学习价值：识别「不可再切的原子步骤」是拆分迁移的关键判断；对应的回退纪律是「成对/成组回退」，否则会留下一个恒红的中间态。
- **测试源码也会被命名空间变更连带打穿**
  - 含义：`spring-test` 的 `MockHttpSession`、`MockServletContext`、`FilterChain` 在 Boot 3 下实现的是 `jakarta` 类型；测试里写 `javax.servlet.http.HttpSession` 变量会 `testCompile` 失败（本次实测 10 个编译错误）。这类文件**不在 issue 的「11 个 `src/main/java`」清单里**，必须在 PR 里单独列出并说明「编译必需、无断言改动」。
  - 位置：`CartServiceTest.java:9`、`SpaFallbackTest.java:15`、`EmployeeAuthenticationFilterTest.java:4`
  - 学习价值：验收条件若只约束「生产代码文件数」，测试文件就很容易成为未登记的口子；照实登记（而不是悄悄改掉）才能让 reviewer 判断「这是必需还是顺手」。

### Maven / 构建

- **`maven.compiler.release` 取代 `source`/`target` 是语义升级，不只是写法变短**
  - 含义：`source`/`target` 只告诉编译器「语法按 N、字节码按 N」，**不校验**你用的 API 是否真的存在于目标 JDK；`--release N` 会同时用**目标版本 API 签名**编译（等同 `-source N -target N -bootclasspath <JDK N>`），因此能在 JDK 21 上编译出**保证能在 17 上跑**的字节码，并顺带屏蔽 JDK 18+ 的新 API。这就是「用 JDK 21 构建、`release=17` 出 class file 61」能自洽的原因。
  - 位置：`pom.xml:13`
  - 学习价值：`release` 是「工具链 JDK ≠ 目标 JDK」场景的正确表达；也解释了为什么还要保留 JDK 21 的工具链要求（enforcer 在 S4 才加）。
- **显式 `<version>` 只覆盖版本号，不解除 BOM 的其他约束**
  - 含义：`mysql-connector-j` 写了 `<version>9.7.0</version>` 仍是 **managed** 依赖——BOM `dependencyManagement` 条目里的 `<exclusions>`、`scope` 继续生效（S2 已记录 BOM 对它有 `protobuf-java` 的 exclusion）。本次这个 pin 与 Boot 3.5.15 BOM 的 `<mysql.version>` **恰好同为 9.7.0**，所以既满足 P1-8（保留显式版本）又没有版本漂移风险。
  - 位置：`pom.xml:58`（显式 9.7.0）；证据 E9（`dependency:list` 与 jar 内 `mysql-connector-j-9.7.0.jar`）
  - 学习价值：判断「升级 BOM 会不会偷偷换掉我的驱动」要同时看**显式 pin** 与 **BOM 的 management 值**；两者相等时 `pin` 的价值是「防未来 BOM 漂移」，不是「当前版本不同」。
- **聚合 artifact 的 pin 管不到 engine**
  - 含义：`junit-jupiter` 是**聚合**包（只有 `api`+`params` 之类的依赖声明），测试真正跑的是 `junit-jupiter-engine`；pom 里 pin 了聚合 `5.11.4`，engine 却来自 BOM 的 `5.12.2`。同理 `mockito-core`（显式 5.23.0）与 `mockito-junit-jupiter`（BOM 5.17.0）不是同一个 artifact。而 `pom.xml:18-19` 两个属性**根本没有被引用**，所以它们对解析结果毫无影响。
  - 位置：`pom.xml:15-19/69/75`；证据 E9
  - 学习价值：「我 pin 过了 ⇒ 版本受控」是常见的错误自信；正确做法是看 `dependency:list` 的**实际解析结果**，而不是 pom 里的属性值。
- **`import` 型 BOM 用自己的属性解析自己管理的版本**
  - 含义：`scope=import` 的 BOM 在被解析时会以**它自身 pom 的属性**求值，父/子项目里对同名属性（如 `junit-jupiter.version`）的重定义**不能**改写 BOM 管理的版本。要覆盖就必须在自己 `<dependencyManagement>` 里显式声明该 artifact 的 `<version>`。
  - 位置：`pom.xml:27-35`（BOM import）、`pom.xml:18-19`（无效属性）
  - 学习价值：这是「改了属性但版本没变」这类灵异现象的标准解释。

### 测试与守卫设计

- **白名单要用「集合相等」而不是「包含」**
  - 含义：`assertTrue(actual.containsAll(expected))` 或「每个文件都合法」只能证明「没有遗漏」，不能证明「没有新增」；反过来 `assertEquals(expectedSet, actualSet)` 能同时锁住**新增与删除**。因此新增一个会用到 Servlet API 的类时，守卫会失败并强制开发者在同一改动里说明理由 —— 这正是 issue 里 "not contains — a growing allow-list must fail" 的意思。
  - 位置：`SpringBootOnlyArchitectureTest.java`（`JAKARTA_SERVLET_ALLOW_LIST` + `assertEquals(new TreeSet<>(...), filesContaining("jakarta.servlet"))`）
  - 学习价值：架构守卫的价值来自**双向约束**；单向白名单会随项目演化悄悄退化成「什么都允许」。
- **路径归一化：跨平台断言必须把分隔符统一**
  - 含义：`Path.relativize()` 在 Windows 上给出 `\` 分隔的字符串，直接与 `/` 分隔的期望集合比较会**恒失败**（CI 是 ubuntu，本地可能不是）。做法：`relativize(...).toString().replace(File.separatorChar, '/')`。
  - 位置：`SpringBootOnlyArchitectureTest.java` 的 `relativePath(...)`
  - 学习价值：守卫测试本身要能在任意平台稳定通过，否则它会被当作「环境问题」被 Ignore 掉。
- **「不新增测试方法」也是一种约束**
  - 含义：用例数被当作不下降判据（≥134）时，**新增**一个 `@Test` 方法会让数字上升而**掩盖**删除旧用例；本次把白名单断言**并入**原有守卫方法（重命名）以保持 2 个方法与 134 总数不变，属于对「不改测试语义」的尊重。
  - 位置：`onlineApplicationConfinesJakartaServletApiToTheReviewedAllowList()`
  - 学习价值：用例数是弱信号，必须同时看「哪些用例被改名/合并/删除」；反过来，正因为弱，才不该借升级顺手加/减。
- **否定型断言也要成对加**：本次补强的 2 行（源码禁 `javax.servlet`、pom 禁 `javax.servlet-api`）说明：把旧的「禁 A」反转成「限定 B」时，**A 的禁令不应该消失**（否则一次回退式改动会静默回归而守卫仍绿）。

### Spring Boot 3 行为差异与归因方法

- **「升级后观察到 X」≠「升级导致 X」**
  - 含义：worker 观察到 `/api/boot/health/` 401 就归因为「Boot 3 移除了尾斜杠匹配」，但主 agent 在**升级前基点树**（`55f8d79`）上实测同样 401 ⇒ 该现象与升级无关。真实原因是 `CustomerAuthenticationFilter.shouldNotFilter` 的**精确字符串比较**，而 **Servlet filter 在 handler mapping 之前**运行，尾斜杠匹配策略根本没机会参与。
  - 位置：`CustomerAuthenticationFilter.java:17-26`；对照证据：升级前基点树实测 401
  - 学习价值：迁移记录里最危险的不是「漏记差异」，而是**记错归因**——它会把一个既有行为固化成「Boot 3 的已知问题」，后续修复方向全错。方法：**先测旧版本**（A/B），再决定它是不是差异；只看新版本无法归因。
- **Boot ≥3.2 的尾斜杠匹配默认值确实变了（这条是真差异，但本次没被触发）**
  - 含义：`PathPatternParser` 成为默认路径匹配实现后，`/path` 不再自动匹配 `/path/`（旧 `AntPathMatcher` 会）。要恢复需显式配置（如 `setUseTrailingSlashMatch(true)` 一类选项）。本次 10 条深链都是无尾斜杠形式，所以前端与 smoke 都不受影响；**静态深链的尾斜杠场景未测量**，留 S6。
  - 位置：`SpaFallbackTest`（SPA 回退真值表，无尾斜杠用例）
  - 学习价值：知道「有这个差异」和「这次到底有没有被触发」是两件事；文档里要分开写，否则读者会以为已经验证。
- **Error 响应形态**：未认证 `/api/boot/cart` → **401 + `{"errorMessage":"Authentication required."}`**（应用自己写 body，不走 Boot 错误处理）；`/API/boot/cart`、`/assets/missing.js` → **404 + Boot 默认错误体**（`timestamp/status/error/path`）。两类 404/401 的来源不同：前者是 filter，后者是 `BasicErrorController`。判断「错误响应有没有变」就要分别对这两条路径取样。
- **`RestTemplateBuilder.setConnectTimeout/setReadTimeout` 在 Boot 3 被弃用**（`GoogleRecaptchaVerifier:27-28`）：这是**编译期**信号，不是运行期差异；该类 `@Profile("!local")` 且 0/17 覆盖，因此本步既没有运行证据也没有修复。**替代 API 本步未核实**，只登记为后续项 —— 这类「编译器告诉你的迁移债」应当显式入账，而不是被「BUILD SUCCESS」掩盖。

### 容器与字节码取证

- **class file major version 是「这个 jar 能跑在哪个 JVM」最硬的证据**
  - 含义：`.class` 前 8 字节 `ca fe ba be` + minor + major；`0x3d = 61` = Java 17，`55` = 11，`65` = 21。JVM 拒绝加载 major 高于自身的 class。因此「`release=17` 生效」= 字节码头是 61（本次 `xxd` 与 JDK 21 的 `javap` 双证据）。
  - 位置：`target/classes/com/fabflix/boot/FabflixApplication.class`
  - 学习价值：比读 pom/日志更可靠；也提醒「工具版本要对」——旧 `javap` 会直接报类找不到，别误判成构建失败（见 §1.3 第 7 条）。
- **多阶段镜像里 JDK 版本有两处，必须同时改**
  - 含义：build stage 的 `maven:3.9-eclipse-temurin-21` 决定**能不能编译**（`--release 17` 需要 JDK ≥17，`javac` 版本也要能出 61），runtime stage 的 `eclipse-temurin:21-jre` 决定**能不能加载**（JRE 11 不能加载 major 61）。只改其一 → 要么构建失败，要么启动 `UnsupportedClassVersionError`，而 **CI 不构建 Docker，所以 CI 仍是绿的**——这是本次把两行 `FROM` 并进同一 PR 的全部理由。
  - 位置：`Dockerfile:12`、`Dockerfile:21`；证据 E7（容器内 `java -version` = 21.0.12.1）
  - 学习价值：「CI 绿」的覆盖边界要心里有数：Docker 路径只能靠 `docker compose build app` 自己证明。
- **非 root 与 JDK 升级互不影响，但要用命令复核而不是「看起来没动」**
  - 含义：`groupadd/useradd --gid/--uid 1001` + `USER fabflix` 与 `FROM` 行无关；容器 smoke 用 `id fabflix` → `uid=1001 gid=1001` 来证；`EXPOSE 8081`、`ENTRYPOINT` 也在 diff 之外。升级类改动最容易被「顺手」碰到的正是这些安全/运维细节。
  - 位置：`Dockerfile` 的 runtime stage；证据 E7
  - 学习价值：把「未改动」也变成一条**可执行断言**（`docker exec … id fabflix`），比在 PR 里写「未改动」可信。

---

## 结论（一句话）

**结论（2026-10-20 合并回填）**：S3 **已完成** —— PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 已合并（merge `70fb78b`），**#48 已 CLOSED**，**PR CI 双 job SUCCESS**（AC-6 已取证，本文件 §1.5 第 11 项的「未取证」属合并前记录）。实施与核验内容：S3 已在分支 `codex/issue-48-core-boot3-jakarta` 上实施并核验 —— `spring-boot.version` 2.7.18 → **3.5.15**、`maven.compiler.release=17`、11 个 `src/main/java` + 3 个测试文件的 `javax.servlet` → `jakarta.servlet`（另有 3 个测试 import 为编译必需）、守卫测试改为「白名单集合相等（恰好 11 路径）+ `javax.servlet`/`javax.servlet-api` 禁令」、`ci.yml` 三处 JDK 21、`Dockerfile` 两行 `FROM` → JDK 21；JDK 21 下 `mvn clean verify` BUILD SUCCESS、**134 用例全绿**、JaCoCo **988/1078 = 0.9165**、字节码 **major 61**、jar 形态与 dist 守卫不变、容器镜像 JDK **21** 且非 root **1001**、10 条深链 + 401/404 语义 + 顾客支付（`sales.id=13566`）与员工 metadata 全绿。但：**PR CI（AC-6）尚不存在**，连接池/并发、生产 reCAPTCHA、静态尾斜杠、驱动 DATE 读取映射归 **S6**，`@MockBean` 11 条警告归 **S5**，`/api/boot/health/` 的 401 已更正为**既有行为（非 Boot 3 差异）**；改动**未提交、未推送、无 PR**，#48 仍 OPEN。

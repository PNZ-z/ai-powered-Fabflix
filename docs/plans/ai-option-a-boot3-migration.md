# Option A — Spring Boot 3 / Java 17 现代化迁移（AI 前置）

> **文档状态：✅ 已批准（用户，2026-10-19）** —— 定为**本路线图的首个工作流**（Option A 迁移；**GitHub issue 已立项 = [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)，milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**，2026-10-19 创建）。（批准前状态：已按 oracle 审查修订 / 待用户审批）
> 用户已批准：**D1 接受 Boot 2.7.18 → 3.x + `release=17`（工具链 JDK 21）+ jakarta 迁移**，并作为 **AI 的硬前置（Option A）**；**D-时间 = 每周 >10 小时**，路线 **A → B → C**（C 可裁剪）。**D-A3/D-A4/D-A5/D-A7 未逐条裁定**（暂按本文件建议，实施前需确认，见 §9）。
> 批准是**方案方向批准**，不等于已获准立即改码：实施仍须由 **主 agent 指派 `worker`**，且**至今仍未改 `pom.xml`/源码/CI/Dockerfile、未安装任何依赖**。**issue 已于 2026-10-19 创建（#44）**，但**迁移实施仍全部未开始**。
> **立项记录（2026-10-19）**：GitHub issue 已由 `github-operator` 按获批草稿创建 = **[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) `Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace`**（milestone **[#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**，labels 空，state=OPEN，created `2026-10-19T07:06:27Z`）；**本地跟踪文档 = `docs/issues/issue-44-boot3-java17-jakarta-migration.md`**（2026-10-19 由发布草稿 `docs/issues/proposed-option-a-boot3-migration.md` 改名；远端正文与本文件的验收条件逐字一致，`Internal tracking` 分区未发布）。**立项≠实施**：本计划的代码/配置改动仍未开始（D-A6、总览 D6/§12.3-A-1）。
> **修订记录（2026-10-18，oracle 只读审查后；主 agent 裁定「全部接受」）**：**P1-3** 新增 **blocking 前置 = 本地固定 JDK 21**（`JAVA_HOME`/toolchain，写入 README，验收命令 `mvn -version`），并把版本表述统一为"**编译 `release=17`，工具链/CI/镜像统一 JDK 21**"（消除 17/21 混用；本地 Maven 现跑 **JDK 27，不受 Boot 3.5 支持**）；**P2-8** 补 `mysql-connector-j` 升 9.x 的 JDBC 行为差异（时区/`CURDATE()`/SSL 警告）由 AC-A-6 smoke 覆盖；**P2-9** 守卫白名单方案补"**白名单集合断言 == 11 个文件**"；**P2-10** 新增"**允许的等价修改清单**"避免 review 争议（§4.7）；**X-6** 本文件内 CI/镜像版本表述已统一。标注 `javax.servlet` = **11** 个文件（另 4 个保持 `javax.*`）——与总览及 B1 一致。审查报告：`.pi/artifacts/oracle-ai-roadmap-review.md`。
> 本文**不声称**任何迁移已完成、任何测试已运行。凡"估算"均为估计值。
>
> 命名说明：项目约定为 `docs/plans/request-YYYY-MM-DD-<slug>.md`（无 GitHub 编号时）；本文件按任务指定使用 `ai-option-a-*.md` 临时命名，issue 创建后应重命名为 `issue-<编号>-<slug>.md`（**触发条件已于 2026-10-19 到：Option A issue = #44；本次未执行本文件的重命名，待主 agent 指示**）。**长期归属建议**（oracle P2-4）：本文件属**一次性版本方案**，留在 `docs/plans/`；其产生的**跨 issue 持续有效约束**（Boot 3/Spring AI 版本前提、守卫测试与 Java/JDK 约定）在批准实施后沉淀到 `docs/architecture/ai-integration-contracts.md`。

## 元数据

| 项目 | 值 |
| --- | --- |
| 文档类型 | 实施方案（版本 A：现代化迁移，AI 的硬前置）——**已批准（2026-10-19）并定为首个工作流** |
| 创建日期 | 2026-10-18 |
| 工作区基线 | `main` @ `76e3d64` |
| 事实来源 | `.pi/artifacts/scout-ai-roadmap.md`（§A1–A5、§D16–D18）；`pom.xml`；`.github/workflows/ci.yml`；`Dockerfile`；`compose.yaml`；`docs/architecture/frontend-migration-contracts.md`；**本文档另有 docs-writer 自查证据**（见 §2.2 的 grep 结论） |
| 上级文档 | `docs/plans/ai-roadmap-overview.md` |
| 关联 GitHub issue | **[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)** `Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace`；milestone **[#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**（state=open）；labels 空；state=OPEN；created `2026-10-19T07:06:27Z`。**跟踪文档**：`docs/issues/issue-44-boot3-java17-jakarta-migration.md`（正文引用本文件的验收条件）。**立项≠实施**：本计划的改动全部未开始 |
| 依赖 | 无（本方案自身即 Option B/C 的前置） |
| 预估工作量 | **1.5–3 周**（估算，主变量 = jakarta 机械改动之外的行为差异排查） |
| 状态 | **✅ 已批准（用户，2026-10-19）；定为路线图首个工作流（D1 + D-时间 >10h/周）**；D-A1/D-A2 已随 D1 获批，**D-A3..D-A5/D-A7 未逐条裁定（暂按建议，实施前确认）**；D-A6 仅立项方向获授权（**已执行：issue [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) 于 2026-10-19 创建，milestone #3；跟踪文档 = `docs/issues/issue-44-boot3-java17-jakarta-migration.md`；提交/推送仍由主 agent 按常规流程**） |
| 本方案是否让 AI 可用 | **否**。A 完成后**仍然没有**任何 AI 功能；它只解锁 Spring AI 的依赖前提。A 的独立价值是现代化（EOL 修复 + Java 17 + Boot 3 + 简历） |

---

## 总结

Spring AI 1.x 实测基线是 **Spring Boot 3.5.15**（`spring-ai-starter-model-openai:1.1.8` 的 pom 显式依赖，scout `curl` 证据），而本项目是 **Spring Boot 2.7.18（EOL）+ Java 11 + `javax.servlet`**，且架构守卫测试**主动禁止** `jakarta.servlet`（源码断言 + `pom.xml` 断言双保险）。因此"装个 Spring AI 依赖"不可行，必须做一次大版本迁移：Boot 2.7→3.x、**Java 11 → 编译 `release=17`（工具链 JDK 21）**、`javax.servlet`→`jakarta.servlet`（**11 个文件**），并同步升级 CI 的 Java 版本（11 → 21）与 Docker 运行镜像（现为 `eclipse-temurin:11-jre` → JDK 21 系）。**做什么**：一次性、**不改变业务行为**的技术迁移，验收就是"现有 127 个后端测试 + 195 个前端测试 + 12 次 e2e 全绿、JaCoCo 85% 闸门不变、生产 compose smoke 的 HTTP 语义不变"。**工作量**：约 1.5–3 周（估算）；jakarta 包名替换本身是机械的（11 个文件），真正的不确定性来自 **Boot 3 行为差异**（trailing-slash 匹配、`@MockBean` 弃用、Tomcat 10、依赖版本对齐）与 **JDK 版本矩阵**（本地 Maven 现跑 **JDK 27 —— 不受 Spring Boot 3.5 支持**，必须固定 **JDK 21**；CI 由 Java 11 → **21**；统一表述为"编译 `release=17`，工具链/CI/镜像统一 JDK 21"）。**风险**：中等（改动广但每处小、有测试兜底、可整体回退）。**适合谁**：所有想做 Spring AI 的人（**没有例外**）；也适合只想要"现代化"简历分、暂不做 AI 的人。**明确一句**：A 是**所有 Spring AI 方案的前置**，但**单独也有现代化价值**。

---

## 1. 目标与背景

### 1.1 目标

1. 把项目升级到 **Spring Boot 3.x（建议 3.5.x，与 Spring AI 1.1.x 基线一致）**；**编译目标 `release=17`，工具链/CI/镜像统一 JDK 21**；Servlet API 全面切到 `jakarta.servlet`（**11 个文件必须迁移**）。
2. **不改变任何业务行为**：URL、HTTP 状态码、JSON 形状、认证/会话语义、SPA fallback 真值表、错误文案全部保持。
3. 保持**所有质量闸门**：后端 JaCoCo `BUNDLE/LINE ≥ 0.85`、前端 `lines ≥ 88`、Playwright e2e。
4. 同步更新 **CI（Java 版本 → 21）**、**Dockerfile（构建与运行镜像 → JDK 21 系）**、**本地 JDK（固定 21 + 写入 README）**、**架构守卫测试**与**架构文档**。
5. 为 Option B/C 扫清依赖与运行环境前提。

### 1.2 背景（已证实事实）

| 事实 | 证据 |
| --- | --- |
| `<spring-boot.version>2.7.18</spring-boot.version>`，通过 `spring-boot-dependencies` BOM import，**无 `<parent>`** | `pom.xml:23`、`pom.xml:26-33` |
| `maven.compiler.source/target = 11` | `pom.xml:14-15` |
| 依赖：`spring-boot-starter-web`、`-jdbc`、`mysql-connector-j:8.3.0`、`jasypt:1.9.3`；测试 `junit-jupiter:5.11.4`、`mockito-core:5.23.0`、`byte-buddy:1.17.7`、`spring-boot-starter-test` | `pom.xml:37-81` |
| 插件：`maven-surefire-plugin:3.5.2`、`spring-boot-maven-plugin:${spring-boot.version}`（`repackage` + `mainClass=com.fabflix.boot.FabflixApplication`）、`jacoco-maven-plugin:0.8.15`（`excludes` + `verify` 阶段 `check` 0.85） | `pom.xml:88-171` |
| `frontend` profile：enforcer `require-frontend-dist`（`validate`）+ resources `copy-frontend-dist`（`process-resources`） | `pom.xml:186-266` |
| 守卫测试：源码不得含 `jakarta.servlet`、`@WebServlet`、`extends HttpServlet`；`pom.xml` 必须 `jar` + `repackage`，且**不得含 `jakarta.servlet-api`** | `SpringBootOnlyArchitectureTest.java:20-42` |
| CI 后端 job：name `Build and test (Java 11)`、temurin **11**、`mvn --batch-mode --no-transfer-progress verify`、`actions/checkout@v7`、`setup-java@v5`、`upload-artifact@v7` | `.github/workflows/ci.yml` |
| Dockerfile：`node:22-alpine` → `maven:3.9-eclipse-temurin-11` → **`eclipse-temurin:11-jre`**（非 root uid/gid 1001） | `Dockerfile` |
| Spring AI 1.x 需 Boot 3.x + Java 17+ + jakarta；1.1.8 基线 Boot 3.5.15 | scout §A5 |
| 本机 Maven 运行在 **JDK 27**（Homebrew，**不受 Spring Boot 3.5 支持**），`/usr/bin/java` = 17.0.12 | scout §A4；**P1-3** |
| 覆盖率现状：后端 LINE 968/1078 = 0.8980、`Tests run: 127`；前端 lines 92.35%（846/916）、195 用例、e2e 12 次执行 | 契约文档 §7；scout §A3 |

### 1.3 迁移的"命名空间成本"到底有多大（本文的澄清）

scout §A2 表述为"`javax.` 用量 15 个文件"。**docs-writer 本轮自查（`grep -rn "^import javax\\." src/main/java`）显示需要区分两类**：

| 类别 | 包 | 文件数 | 迁移后 | 证据（本轮 grep） |
| --- | --- | --- | --- | --- |
| **Jakarta EE 重命名（必须改）** | `javax.servlet.*` | **11** | → `jakarta.servlet.*` | `CartService`、`CartController`、`CheckoutController`、`CheckoutTokenService`、`PaymentController`、`CustomerAuthController`、`CustomerAuthenticationFilter`、`EmployeeController`、`EmployeeAuthenticationFilter`、`SearchController`、**`SessionController`** |
| **不重命名（保持 `javax.*`）** | `javax.sql.DataSource` | 1（`admin/AdminRepository.java:9`） | **不变** | `javax.sql` 不属于 Jakarta EE 9 重命名范围 |
| **不重命名（保持 `javax.*`）** | `javax.xml.parsers.*` | 3（`ActorXMLParser`、`CastXMLParser`、`MainXMLParser`） | **不变** | Jakarta EE 只改 `javax.servlet` 等 EE 包，**不改 JDK 自带的 `javax.xml`** |

**结论**：`javax.` 总计 15 个文件（与 scout 一致），但**真正必须改的是 11 个 `javax.servlet` 文件**（scout §A2 的"集中列表"漏了 `SessionController.java`）；其余 4 个**故意保持 `javax.*`**。这直接决定工作量与守卫测试的写法（守卫不能一刀切禁 `jakarta.servlet`，也不能一刀切禁 `javax.`）。

---

## 2. 范围与非目标

### 2.1 In scope

1. `pom.xml`：`spring-boot.version` → 3.5.x；`maven.compiler.release` → 17；插件/依赖版本对齐。
2. **11 个文件**的 `javax.servlet` → `jakarta.servlet`（import 与类型引用；`Filter#doFilter`、`HttpServletRequest/Response`、`HttpSession`、`FilterChain`、`ServletException`）。
3. `SpringBootOnlyArchitectureTest` 改写（§4.2）。
4. `.github/workflows/ci.yml`：Java 版本 + job 名称 `Build and test (Java 11)` → 新版本（**该 name 是文档与日志中被引用的字符串，改后需同步文档**）。
5. `Dockerfile`：`maven:3.9-eclipse-temurin-11` → **`maven:3.9-eclipse-temurin-21`**；`eclipse-temurin:11-jre` → **`eclipse-temurin:21-jre`**；保持非 root uid/gid 1001（与 CI/本地 **JDK 21** 一致，X-6）。
6. 依赖兼容性核查（§4.3）：`mysql-connector-j`、`jasypt`、`mockito-core`、`byte-buddy`、`surefire`、`JaCoCo`、`spring-boot-maven-plugin`、`maven-enforcer-plugin`、`maven-resources-plugin`。
7. `docs/architecture/frontend-migration-contracts.md` 更新：§8 守卫描述、Java/Boot 版本、任何被改名影响的表述（**由主 agent 批准后**）。
8. 迁移后的**回归验证**（§6）：后端、前端、e2e、生产 smoke。
9. 本文档与工作流日志。

### 2.2 Non-goals（明确排除）

| 排除项 | 理由 |
| --- | --- |
| 引入 `spring-ai-*` 任何依赖 | 属 Option B（A 只解锁前提） |
| 引入 Spring Security / JWT | 违反 **AD-1**（契约文档 §10） |
| 任何新功能 / 行为变更 / 重构 | A 的红线是"零行为变更" |
| 引入向量库 / 语料 / 推荐 | 属 Option B/C |
| 多实例 / Spring Session / Redis | 违反 **AD-2** |
| CSRF 引入 | 现状无 CSRF（契约文档 §9）；变更需单独批准 |
| 前端构建链升级（Vite/React/vitest/Playwright） | 非必需（Node 22 与 Boot 版本无关）；避免扩大范围 |
| `webapp` legacy 恢复 | 已有退役守卫禁止（契约文档 §2） |
| 数据库 schema / 数据迁移 | 与本迁移无关 |
| 提交/推送/建 issue（**本迁移的执行包**） | 本任务禁止；且创建 issue 需用户授权（**issue #44 已由主 agent / `github-operator` 于 2026-10-19 创建**，故本行仅约束 worker 自行提交/推送/建 issue） |

---

## 3. 依赖与前置

> **Blocking 前置（P1-3）**：迁移前必须先把**本地 JDK 固定为 21**（`JAVA_HOME` + Maven toolchain，写入 README）。理由：本地 Maven 现跑 **JDK 27**，而 **Spring Boot 3.5 不支持 JDK 27**（Boot 3.5 早于 JDK 27；其测试/字节码增强链对未知 class file 版本会直接失败）。不固定则"本地 vs CI"不可比，A4（Boot 3 行为差异，最大不确定性）会被环境噪声掩盖。

| 前置 | 说明 | 状态 |
| --- | --- | --- |
| **本地固定 JDK 21（blocking，P1-3）** | `JAVA_HOME`/toolchain 指向 JDK 21，写入 README；验收命令含 `mvn -version` | **待安装/待确认（U-A1）** |
| 用户批准（本迁移触碰构建、CI、运行镜像） | 属"中等影响、可回退"变更，但改 CI 与镜像 | **✅ 已批准（用户，2026-10-19，D1）** |
| 决策：目标 Boot 版本（3.5.x 建议）与 JDK 策略（**`release=17` + 工具链/CI/镜像 JDK 21**） | 见 §9-D-A1/D-A2 | **✅ 已批准**：D-A1 = Boot **3.x**（具体小版本按建议取 **3.5.x**，与 Spring AI 1.1.x 基线一致）；D-A2 = **`release=17` + 本地/CI/镜像统一 JDK 21** |
| 依赖兼容性预核查结论 | §4.3 | **待实施** |
| 备份/回退点 | 迁移失败可整体 revert（单分支单 PR） | 可满足 |
| 测试基线（迁移前后对比用） | 现有 127 后端 / 195 前端 / 12 e2e / 覆盖率数字已记录（契约文档 §7） | 已具备 |

---

## 4. 方案设计（草案）

### 4.1 `pom.xml` 变更（草案）

| 项 | 现状 | 目标（草案） | 备注 |
| --- | --- | --- | --- |
| `spring-boot.version` | `2.7.18` | **`3.5.x`**（与 Spring AI 1.1.x 基线对齐） | 具体补丁版本实施时定 |
| `maven.compiler.source/target` | `11`/`11` | 改用 **`maven.compiler.release=17`** 单属性 | `release` 更严格（防止误用新 API）；**工具链 JDK 21**（`release=17` 只约束字节码/API 级别，不限制 JDK 版本） |
| `spring-boot-maven-plugin` | `${spring-boot.version}` | 随 Boot 3.5.x | 版本属性已绑定，自动跟随 |
| `mockito-core` / `byte-buddy` / `byte-buddy-agent` 显式 pin | `5.23.0` / `1.17.7` | **评估是否交回 BOM 管理** | 显式 pin 是为旧 JDK 兼容，Boot 3.5 BOM 已管理较新版本 |
| `junit-jupiter` / `junit-platform` 显式 pin | `5.11.4` / `1.11.4` | 同上评估 | |
| `mysql-connector-j` | 显式 `8.3.0` | 评估是否跟随 BOM（Boot 3.5 管理 9.x）或保持 | **兼容性核查项**；若升 9.x 必须由 AC-A-6 smoke 覆盖 JDBC 行为差异（**时区处理、`CURDATE()` 返回类型/时区、SSL 连接警告**，见 §4.3 与 P2-8） |
| `jasypt:1.9.3` | 保持 | **保持但需核查** | 见 §4.3；用于 `StrongPasswordEncryptor`（`JasyptPasswordVerifier`），**不涉及 servlet** |
| `jacoco-maven-plugin` | `0.8.15` | **保持**（支持 Java 17/21） | 具体支持范围实施时确认 |
| `excludes` 列表 | 8 项（离线工具） | **不变** | 不得借迁移扩 exclude |
| `frontend` profile（enforcer/resources） | 现状 | **不变** | 迁移不得破坏 `require-frontend-dist` 与 dist 复制（契约文档 §11） |

### 4.2 架构守卫测试改写（**必须先改**）

现状（`SpringBootOnlyArchitectureTest.java:20-42`）：

```java
assertFalse(source.contains("jakarta.servlet"));   // ← 迁移后必须删/反转
assertFalse(source.contains("@WebServlet"));
assertFalse(source.contains("extends HttpServlet"));
assertTrue(pom.contains("<packaging>jar</packaging>"));
assertTrue(pom.contains("<goal>repackage</goal>"));
assertFalse(pom.contains("jakarta.servlet-api"));  // ← 语义需重新确认
```

**改写原则（草案）**：

1. **保留**对 `@WebServlet` / `extends HttpServlet` 的禁止（仍然表达"不得回到 servlet 编程模型"的原始意图）。
2. **反转** `jakarta.servlet` 断言：从"禁止"改为"**受控允许**"。
   - 可选做法（二选一，实施时定）：
     - (a) **白名单**：仅允许 `jakarta.servlet` 出现在已知的 controller/filter 集合中（**11 个文件**），任何**新增**文件引入 `jakarta.servlet` 需显式更新白名单 → 保留"架构是显式声明的"这一价值；
      - **并补一条断言（P2-9）**：白名单集合本身必须被断言为 **恰好 11 个文件**（集合相等，而不是"包含"），否则白名单会逐渐膨胀、守卫失效。
     - (b) **仅保留"不得出现 legacy servlet 标记"**（`@WebServlet`/`extends HttpServlet`/`*.jsp`），不再断言命名空间 → 最简、但丢失一部分守卫力。
   - **docs-writer 建议 (a)**：白名单与既有仓库风格（用测试钉死架构决策）一致，且能防止"悄悄再加一个 servlet 类"。
3. `pom.xml` 的 `jakarta.servlet-api` 断言：Spring Boot 3 的 `spring-boot-starter-web` **传递**引入 `jakarta.servlet-api`，但 `pom.xml` 文本中**不会**出现该字面量（只要不显式声明依赖）→ **该断言可保留**；实施时须确认（避免误报红灯）。
4. 仓库中**没有任何 `.jsp`**（scout §A2/契约文档 §8）——该断言保持不变。
5. **`FrontendApiMigrationTest`（legacy 退役守卫）与 `SpaFallbackTest` 不受影响**（§8 契约文档已确认 `SpaFallbackConfig` 不依赖 servlet API；**迁移后需重新确认**）。

### 4.3 依赖兼容性核查清单（实施前第一步）

| 依赖 | 关注点 | 已知/待查 |
| --- | --- | --- |
| `mysql-connector-j:8.3.0` | Boot 3.5 传输驱动 API；驱动 8.3.0 对 MySQL 8.4 的兼容；**若跟随 BOM 升 9.x 的 JDBC 行为差异（时区/`CURDATE()`/SSL 警告）** | 驱动侧一般向前兼容；**待实测 JDBC 集成**（U-A2）；升 9.x 的行为差异必须由 **AC-A-6 生产 smoke** 覆盖（P2-8） |
| `jasypt:1.9.3` | 2014 年版本；是否使用 `javax.crypto`（JDK 自带，OK）；是否依赖已删 API | 仅 `org.jasypt.util.password.*` 被使用（`JasyptPasswordVerifier`、`UpdateSecurePassword`、`UpdateEmployeeSecurePassword`、`CreateLocalCustomer`）→ **风险低，但需实测** |
| `mockito-core:5.23.0` + `byte-buddy:1.17.7` | JDK 17/21 下的 inline mock maker / 类文件版本 | 5.x/1.17.x 应支持 17–21；**待实测** |
| `spring-boot-starter-test` | Boot 3 的 JUnit 5.11 与现有显式 pin 的一致性 | **待实测** |
| `maven-surefire-plugin:3.5.2` | JDK 17/21 运行 | 应可用；**待实测** |
| `jacoco-maven-plugin:0.8.15` | 对 JDK 17/21 class 文件支持 | 应可用；**待实测**（若失败再升版本，但会触碰闸门） |
| `spring-boot-maven-plugin` 3.5.x | `repackage` 行为、`mainClass` 配置 | 不变；**待实测**（jar 内容/静态资源仍须在 `BOOT-INF/classes/static`） |
| `maven-enforcer-plugin:3.5.0` + `require-frontend-dist` | 与 Boot 3 无关 | 不变 |
| `maven-resources-plugin:3.3.1` | 与 Boot 3 无关 | 不变 |
| `@MockBean`（**9 处**） | **Boot 3.4 起已 deprecated**（替代为 Spring Framework 6.2 的 `@MockitoBean`） | **待实施确认**；建议本次迁移**先不动**（避免扩大范围），把弃用警告记录为后续任务；或作为可选清理项由主 agent 决定 |
| `MockMvcBuilders.standaloneSetup` | admin 包写法 | Spring Framework 6 保持；**待实测** |

### 4.4 运行环境变更（CI 与 Docker）

| 位置 | 现状 | 目标（草案） |
| --- | --- | --- |
| `.github/workflows/ci.yml` 后端 job | `name: Build and test (Java 11)`、`java-version: "11"` | **`java-version: "21"`（LTS）**；**job name 必须同步改**（日志/文档引用该字符串） |
| 本地开发（**blocking**） | Maven 跑在 **JDK 27**（Homebrew，**不受 Boot 3.5 支持**），`/usr/bin/java` = 17 | **固定 JDK 21**（`JAVA_HOME` + Maven toolchain）；写入 README；验收命令 `mvn -version` 必须显示 21 |
| `Dockerfile` build stage | `maven:3.9-eclipse-temurin-11` | `maven:3.9-eclipse-temurin-21`（与 CI 同 JDK） |
| `Dockerfile` runtime stage | `eclipse-temurin:11-jre` | **`eclipse-temurin:21-jre`**，保持非 root uid/gid 1001 |
| `compose.yaml` | `app` 服务仅传 env/端口 | **无需改动**（除非镜像标签写在 compose 内 —— 现状是 `build: .`，无镜像标签 → 无需改） |

**版本选择权衡（已按 P1-3/X-6 统一表述）**：

- **统一口径（推荐）**：**编译 `release=17`**（保证与 Spring AI 1.x 的 Java 17+ 基线一致，且防止误用新 API）+ **本地工具链 / CI / Docker 镜像全部 JDK 21（LTS）**。三者同版本可消除"本地绿、CI 红"。
- **为何不把本地留在 JDK 27**：**Boot 3.5 不支持 JDK 27**（Boot 3.5 早于 JDK 27）；保留 27 会让 A4（Boot 3 行为差异）被环境噪声污染。
- **退化方案**：若 JDK 21 下出现工具链问题（`byte-buddy`/JaCoCo/依赖链），退化到 **全 JDK 17**（同时保留 `release=17`），并在文档中记录。
- **本机 `/usr/bin/java` 的 17.0.12**：可作为降级备选，但它不是 Maven 实际使用的 JDK（见上表）；**验收以 `mvn -version` 输出为准**。**最终由主 agent/用户决定。**

### 4.5 执行顺序（草案，降低返工）

0. **固定本地 JDK 21**（blocking，P1-3）：`JAVA_HOME` + toolchain + 写入 README；`mvn -version` 验证 → 1. **先改守卫测试**（否则迁移后必然红灯）→ 2. 依赖兼容性预核查（§4.3，可先在一个临时分支验证编译）→ 3. `pom.xml` 版本升级 + `release=17` → 4. 11 个文件 jakarta 替换（机械、可批量）→ 5. 编译 + 跑后端测试 → 6. 修 Boot 3 行为差异 → 7. CI/Dockerfile 更新（JDK 21）→ 8. 前端与 e2e 回归（应无改动，只做验证）→ 9. 生产 compose smoke（含 JDBC 行为差异检查）→ 10. 文档更新（契约文档 + 计划 + 日志）。

### 4.6 被否决的候选

| 候选 | 否决理由 |
| --- | --- |
| 升到 Spring Boot 4 / Spring AI 2.0.x | 跨度更大、生态资料少（scout 标注未逐条核实）；非必要 |
| 保留 Boot 2.7 但只升 Java 17 | 无收益（Spring AI 仍不可用）；且 Spring Boot 2.7 对 Java 17 支持有限 |
| 用 `spring-boot-starter-web` 之外的方式（如 WebFlux）| 会重写整个 web 层，违反"零行为变更" |
| 顺手用 Spring Security 替换自写 filter | 违反 AD-1；属独立大决策 |
| 迁移时同时引入 Spring AI 依赖 | **混两次大变更会失去定位能力**；B 应独立立项 |
| 迁移时同步把所有 `@MockBean` 迁到 `@MockitoBean` | 可选但会扩大 diff；建议拆到后续清理 issue（若本次必须做，只能按 §4.7 的**允许的等价修改清单**执行） |

### 4.7 允许的等价修改清单（P2-10）

迁移中"**零行为变更**"的本意是 **行为/契约不变**，而下列**纯 API/注解重命名**允许（且仅允许）在本次迁移内发生，避免 review 时对 diff 范围反复争论：

| # | 原写法 | 允许改为 | 依据/备注 |
| --- | --- | --- | --- |
| 1 | `@MockBean`（**9 处**） | `@MockitoBean` | Boot 3.4 起 `@MockBean` 已 deprecated，替代品为 Spring Framework 6.2 的 `@MockitoBean`（语义等价）；**建议本次不动**（D-A4），但若要动则按本表 |
| 2 | `javax.servlet.*` import/类型引用（**11 个文件**） | `jakarta.servlet.*` | Jakarta EE 9 重命名；**4 个 `javax.sql`/`javax.xml` 文件必须保持原样** |
| 3 | `maven.compiler.source/target = 11` | `maven.compiler.release = 17` | 属性形式变更，编译产物级别语义等价 |
| 4 | Mockito/ByteBuddy/JUnit 显式 pin 版本 | 交回 Boot 3.5 BOM 管理（**可选**） | 仅版本号变动；若出问题则回退到保持 pin（D-A5） |
| 5 | JaCoCo / surefire / spring-boot-maven-plugin 版本号 | 随 BOM/基线更新 | 不得改阈值与 `excludes`（AC-A-2/AC-A-9） |

**不允许**（属行为变更，必须另立 issue/批准）：URL 与状态码、JSON 形状与文案、认证/会话语义、filter 放行/拦截白名单、SPA fallback 真值表、数据库 schema |

---

## 5. 里程碑与工作量（**估算**）

| 里程碑 | 内容 | 估算 |
| --- | --- | --- |
| **A0** 预核查 | **固定本地 JDK 21**（blocking）+ 依赖兼容性核查（编译级 spike）；确认目标 Boot 版本；备份基线测试数字 | 1–2 天 |
| **A1** 守卫测试改写 | 白名单化 `jakarta.servlet` 断言 + **断言白名单集合 == 11 个文件**（P2-9）；保留 legacy servlet 禁令；更新测试 | 0.5–1 天 |
| **A2** 构建升级 | `pom.xml`（Boot 3.5.x、`release=17`、版本对齐）+ 编译通过 | 1–2 天 |
| **A3** jakarta 迁移 | 11 个文件的 import/类型替换 + 编译 + 单测修复 | 1–3 天 |
| **A4** Boot 3 行为差异 | trailing-slash、错误响应、`@MockBean` 弃用、Tomcat 10 行为、SPA fallback 复核 | 2–5 天（**最大不确定性**） |
| **A5** CI/Docker/JDK | workflow Java 版本 → **21** + job name；Dockerfile 两个 stage → **21**；本地 JDK 固定 21（A0 已做则此处仅验收） | 1 天 |
| **A6** 全量回归 | `mvn verify` + 前端 lint/test/coverage/build/e2e + 生产 compose smoke（§6） | 1–2 天 |
| **A7** 文档 | 契约文档 §8/§9/§12 更新、计划回填、工作流日志 | 0.5–1 天 |
| **合计** | | **1.5–3 周**（估算） |

**估算假设**：单开发者 + 标准工作流；仓库现有测试覆盖良好（"改动被测试兜底"）。**未含**因依赖不兼容导致的大范围替换（如 jasypt 必须换库）。

---

## 6. 验收与评测（"零行为变更"的可验证定义）

### 6.1 验收条件（草案）

| # | 验收条件 | 验证方式 | 通过判据 |
| --- | --- | --- | --- |
| AC-A-1 | 后端测试全绿且数量不下降 | `mvn --batch-mode --no-transfer-progress verify` | BUILD SUCCESS；`Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`（迁移后**不得减少**；增加必须解释） |
| AC-A-2 | 后端覆盖率闸门不变 | 同上（JaCoCo `verify` 阶段 check） | `All coverage checks have been met.`；LINE 比率 ≥ 0.85（基线 0.8980） |
| AC-A-3 | 构建产物形态不变 | `mvn -Pfrontend -DskipTests clean package` + `unzip -l target/fablix.jar` | 成功；jar 内含 `BOOT-INF/classes/static/index.html` 等条目（契约文档 §11） |
| AC-A-4 | dist 缺失守卫仍生效 | 临时移走 `frontend/dist` 后 `mvn -Pfrontend -DskipTests package` | **BUILD FAILURE**（enforcer `require-frontend-dist`） |
| AC-A-5 | 前端不变且全绿 | `npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e` | 干净；195 用例通过；Lines ≥ 88（基线 92.35%）；12 次 e2e 通过 |
| AC-A-6 | HTTP 语义不变 + **JDBC 行为差异**（若驱动升 9.x） | 生产 compose smoke（`docker compose build app` + `up -d --no-deps app`） | health **200**；`/` 含 `id="root"`；10 条深链 **200 text/html**；未认证 `/api/boot/cart` → **401 JSON**；`/API/boot/cart` → **404 JSON**；`/assets/missing.js` → **404**；顾客登录→session/cart 200；员工登录→dashboard 元数据 `status:success`；**JDBC 回归（P2-8）**：下单/销售的日期与 `sales.sale_date` 一致（无时区偏移/CURDATE 偏差）、无新增 SSL 连接警告致命化 |
| AC-A-7 | 认证/会话语义不变 | 同上 smoke + 后端测试 | `changeSessionId()`、401 body 形状、员工登出 204 + 保留 `user` 属性 |
| AC-A-8 | 守卫测试与文档一致 | 读测试文件 + 契约文档 §8 | 白名单/禁令描述与实际代码一致 |
| AC-A-9 | 无新增 JaCoCo exclude | 读 `pom.xml` | `excludes` 列表与迁移前一致（8 项） |
| AC-A-10 | CI 双 job 全绿（Java 版本已升级） | 推送后 CI（**需用户授权提交/推送**） | `Build and test (Java 21)` 与 `Frontend (...)` 均 SUCCESS |
| AC-A-11 | **本地/CI/镜像 JDK 一致（blocking 前置）** | `mvn -version` + 读 `.github/workflows/ci.yml` + `Dockerfile` | 本地 `mvn -version` 显示 **JDK 21**；CI `java-version: "21"`；`Dockerfile` 两个 stage 均为 JDK 21 系（P1-3/X-6） |

> **注意**：AC-A-6 中的"10 条深链"与"12 次 e2e"等数字来自 M2 终验记录（`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`）；迁移**必须复现**这些结果（数量可增不可减）。

### 6.2 明确"不验证/无法自动验证"的部分

- 真实浏览器刷新、双标签页、真实 MySQL 并发：沿用既有边界（不在本迁移引入）。
- 对比度/视觉回归：jsdom 无布局（契约文档 §7），只能人工确认。
- Boot 3 的**未被测试覆盖的行为差异**：靠 AC-A-1/A-6 + 人工审阅 diff 兜底。

---

## 7. 风险与回退

| 风险 | 概率 | 影响 | 缓解 |
| --- | --- | --- | --- |
| **依赖不兼容**（jasypt 1.9.3 / byte-buddy / JaCoCo / mysql-connector） | 中 | 阻塞迁移 | A0 预核查先做；不兼容则逐个评估替换（jasypt 可换 `spring-security-crypto`，但**属范围外，需批准**） |
| **Boot 3 行为差异**（trailing-slash 匹配、错误响应、Tomcat 10、`@MockBean` 弃用） | 中高 | 测试红灯/隐性行为变更 | 以 AC-A-1/A-6 为判据；差异必须**显式记录并获批**，不得静默接受 |
| **守卫测试遗漏更新**（迁移后 CI 红灯） | 高（必然） | 阻塞 | 执行顺序把 A1 放最前 |
| **Jakarta 替换不完整**（漏文件/漏类型引用） | 中 | 编译失败 | 编译器会直接报错；11 个文件清单已列明（§1.3） |
| **JDK 矩阵不一致**（本地 27 / CI 21 / 镜像 21） | 中高 | "本地绿、CI 红"；A4（Boot 3 行为差异）无法定位 | **已升级为 blocking 前置（P1-3）**：本地 `JAVA_HOME`/toolchain 固定 **JDK 21**、CI 与镜像同版本、`release=17` 约束 API 使用；以 AC-A-11 验收 |
| **覆盖率余量被吞**（迁移引入新分支/类） | 低中 | CI 红 | 迁移不新增类（理想）；若新增（如配置类）必须配测试；**不得扩 exclude** |
| **前端回归**（理论上不该发生） | 低 | 返工 | 前端不参与构建链；AC-A-5 作为回归证明 |
| **Docker 镜像拉取/构建变慢** | 低 | 时间 | 仅换基础镜像标签，层缓存策略不变 |
| **`mysql:8.4` 与驱动版本组合** | 低 | 连接失败 | A6 smoke 覆盖 |

### 7.1 回退方案

1. **整体 revert**：迁移在**单分支单 PR** 内完成；失败即 `git revert`/关 PR，`main` 不受影响（前提：不合并中间态）。
2. **分阶段回退点**：`pom.xml` 升级（A2）与 jakarta 替换（A3）之间是一个可保留的中间态（此时仍 Boot 2.7、仍 `javax`），可先验证依赖兼容性再继续。
3. **镜像与 CI 回退**：CI/镜像改动独立、可单独 revert。
4. **禁止的动作**：不得通过放宽闸门（降覆盖率阈值、扩 excludes、跳过 e2e）来"让 CI 变绿"——若确实必须，需暂停并请求主 agent/用户决策。

---

## 8. 简历价值

| 要点 | 可展示证据 |
| --- | --- |
| "把 EOL 的 Spring Boot 2.7 / Java 11 应用迁移到 Boot 3.5（**`release=17` + JDK 21 工具链**），零行为变更" | 迁移 PR diff（pom / **11 个文件** / CI / Dockerfile / 本地 JDK 固定）、**迁移前后相同的测试与覆盖率数字**、smoke 结果 |
| "Jakarta 命名空间迁移 + 架构守卫测试重构（白名单化）" | 守卫测试前后代码 + 测试断言清单 |
| "统一 CI/本地/容器 JDK 版本，消除环境漂移" | CI 配置、Dockerfile、`release` 属性 |
| "在迁移中保持质量门禁不放水（覆盖率/闸门不变）" | `pom.xml` excludes 未变、阈值未变、CI 记录 |

**结论**：A 单独即是一条合格的简历条目（"现代化/EOL 修复"是招聘常见硬要求），且是 B/C 的必要条件。

---

## 9. 用户决策（oracle 审查已完成；**用户已裁定，2026-10-19**）

| # | 事项 | 选项 | docs-writer 建议 | **用户决定（2026-10-19）** |
| --- | --- | --- | --- | --- |
| **D-A1** | 目标 Boot 版本 | 3.5.x / 其他 | **3.5.x**（与 Spring AI 1.1.x 基线一致） | **已批准 Boot 3.x**（随总览 D1）；具体小版本仍按建议取 **3.5.x**（未逐条讨论小版本） |
| **D-A2** | Java/JDK 版本策略（**X-6 统一口径**） | 全 17 / **`release=17` + 本地·CI·镜像 JDK 21** / 全 21 | **`release=17` + 本地/CI/镜像统一 JDK 21** | **已批准（按建议）**：`release=17` + 工具链/本地/CI/镜像统一 **JDK 21**；**本地必须离开 JDK 27**（仍在 §3 blocking / U-A1） |
| **D-A3** | 守卫测试改写方式 | 白名单 (a) / 仅保留 legacy 禁令 (b) | **(a) 白名单** | **未逐条裁定**（本次批准未涉及）→ 暂按建议 (a)，**实施前需确认（未确认）** |
| **D-A4** | 是否在本次迁移一并清理 `@MockBean` 弃用 | 一并 / 拆后续 | **拆后续** | **未逐条裁定** → 暂按建议"拆后续"，**实施前需确认（未确认）** |
| **D-A5** | 依赖显式 pin 是否交回 BOM | 交回 / 保持 | 先保持、仅改必需项 | **未逐条裁定** → 暂按建议保持，**实施前需确认（未确认）** |
| **D-A6** | 是否授权提交/推送/建 issue | 是/否 | 需用户明确授权（本任务禁止） | **已执行立项**：**D6 授权方向 = 先建 Option A 迁移 issue**，已于 2026-10-19 创建 = **[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)**（milestone #3，由主 agent / `github-operator` 执行；范围限定只建 A，B/C 后续再立）；**提交/推送仍由主 agent 按常规流程**，`worker` 不得自行提交 |
| **D-A7** | 是否接受 §4.7 的**允许的等价修改清单**（P2-10） | 接受 / 逐条讨论 | **接受** | **未逐条裁定** → 暂按建议接受，**实施前需确认（未确认）** |

**oracle 应重点检查**：§1.3 的 `javax.` 分类是否与代码一致（11 vs 15 的区分）；§4.2 守卫白名单设计是否足够（是否会被绕过）；§4.3 兼容性核查清单是否遗漏依赖；§6 验收条件是否足以证明"零行为变更"；§7.1 回退方案是否可行。

**未确认事项**：

| # | 未确认项 |
| --- | --- |
| U-A1 | 本地 **JDK 21** 是否已安装（当前 Maven 跑 JDK 27，**不受 Boot 3.5 支持**）；JDK 21 下以 `release=17` 编译/跑测试是否可行（**未实测**） |
| U-A2 | 各依赖在 Boot 3.5.x + Java 17/21 下的实际兼容性（**未实测**） |
| U-A3 | Spring Boot 3.5.x 中 `@MockBean` 究竟只是 deprecated 还是已不可用（**未在本仓库实测**） |
| U-A4 | Boot 3 相对 2.7 的**其他未预见行为差异**（只能通过回归发现） |
| U-A5 | CI 上 `Build and test (Java 11)` job 名被哪些文档/日志引用（需检索后同步更新） |
| U-A6 | JaCoCo 0.8.15 对 Java 21 class 文件的支持范围 |

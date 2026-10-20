# 工作流日志 / 实施记录 — Issue #47（S2：Validate non-Spring dependency readiness）

> 本文件是 **S2（[#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)）的实施与验证记录**，并按 `AGENTS.md` §8 承载本工作流日志的两大节：
> **一、修改了哪些文件及潜在问题**（§一）；**二、新增知识点 / 重点学习对象**（§二）。
>
> **口径声明**：下表所有测量/冒烟结果由**单一 `worker` 与主 agent 亲自运行并核验**（原始报告 `.pi/artifacts/worker-issue-47-dependency-readiness.md`）；`docs-writer` **未**运行 `mvn` / npm / Docker / 应用 smoke / 浏览器 / CI，只做了**只读复核**（`git diff`、`target/fablix.jar` 内容与 SHA-256、`target/site/jacoco/jacoco.csv` 汇总、`target/surefire-reports/*.xml` 汇总、`~/.m2` 内 BOM 与驱动的 pom/字节码）与文档写入，**未执行任何 GitHub 写操作**。

## 元数据

| 项目 | 值 |
| --- | --- |
| 记录日期 | **2026-10-20**（实施、验证与记录同日，本机时区 America/Los_Angeles / PDT） |
| Issue | **#47** — https://github.com/PNZ-z/ai-powered-Fabflix/issues/47（`[Step 2/7] Validate non-Spring dependency readiness on the Boot 2.7 baseline`，milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)，labels 空，state **OPEN**） |
| 上游 epic | [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（epic，**OPEN**）；相邻步骤 [#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)、[#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)–[#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52) |
| 分支 | **`codex/issue-47-dependency-readiness`（尚未创建 / 尚未提交）** —— 本轮结束时工作区仍在 **`main`**，改动未暂存 |
| Commit SHA | **无本步提交**。基线 commit = **`9eea6645a4f14004e994848f0daf8741f2909c34`**（短 `9eea664`，`Merge pull request #54 from PNZ-z/codex/coverage-threshold-90`） |
| 提交 / 推送 / PR | **未提交、未推送、无 PR**。远端 `main` 未受影响；**#47 保持 OPEN**，checklist 中 S2 记为「已实施（2026-10-20），**待 PR 合并**」 |
| 树状态（本轮结束） | `M pom.xml`（1 行）＋ 本步新增/更新的 `docs/` 文件；另有本任务开始前即存在的未跟踪 `?? .idea/`（IDE 目录）与 `.pi/artifacts/`（不在 `docs/`，不入提交） |
| 唯一代码改动 | `pom.xml`：`com.mysql:mysql-connector-j` **8.3.0 → 9.7.0**（`git diff --stat` = `1 file changed, 1 insertion(+), 1 deletion(-)`） |
| 构建工具链 | Apache Maven **3.10.0**；`JAVA_HOME=/opt/homebrew/opt/openjdk@21` → **Java 21.0.12.1**（vendor Homebrew）；locale `zh_US_#Hans`；timezone `America/Los_Angeles` |
| 运行时（附加验证） | **Java 11.0.31**（`/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home`），用于 9.7.0 的 Java 11 运行时 smoke |
| 数据库环境 | 容器 `fabflix-mysql`（镜像 `mysql:8.4`，`0.0.0.0:3307->3306`，`Up 7 days (healthy)`，`SELECT VERSION()` = **8.4.11**）；**未执行 `docker compose down`**（本步开工前已运行 7 天，保持原状） |
| 关联计划 / checklist | `docs/plans/issue-44-migration-checklist.md`（§S2、§S3、状态总表）、`docs/plans/issue-44-migration-steps.md`（§3 总表 S2 行、§4-S2 DoD） |
| 关联 issue 文档 | `docs/issues/issue-47-dependency-readiness.md`（#47 的规范来源与跟踪记录）、`docs/issues/issue-44-boot3-java17-jakarta-migration.md`（epic / AC-6） |
| 证据来源 | worker 报告 `.pi/artifacts/worker-issue-47-dependency-readiness.md`（主 agent 核验）；主 agent 的 Java 11 重跑与 A/B 复核；docs-writer 的只读复核（见 §0 的 E10） |

---

## 0. 验证命令与结果汇总（证据索引）

> 「执行者」列区分 **worker / 主 agent**（实际运行）与 **docs-writer**（只读复核）。

| # | 验证项 | 关键命令（节选） | 结果 | 执行者 |
| --- | --- | --- | --- | --- |
| **E1** | 驱动 9.7.0 定版证据 | `curl -o /dev/null -w "%{http_code}" .../mysql-connector-j/9.7.0/mysql-connector-j-9.7.0.jar`；`unzip -p ... com/mysql/cj/jdbc/Driver.class \| xxd -l 16` | Central **200**（jar 与 pom）；`0353648eaa1c91e0f4020c959abf756bc866ffd583df22ae6b6f6e0cbd43eb44`；`Driver.class` = `cafe babe 0000 0034` → **class file major 52（Java 8）**；全 jar 1066 个 `.class` 直方图 `{49: 2, 52: 1064}`，**无 > 52**；jar 内无 `module-info.class`（单 release，非 MR-jar） | worker；docs-writer 复核 major 52 |
| **E2** | 门禁（JDK 21） | `JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=…/bin:$PATH mvn --batch-mode --no-transfer-progress verify` | **BUILD SUCCESS**；`Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`；`All coverage checks have been met.`；JaCoCo **988 covered / 90 missed / 1078 = 0.9165**（门槛 0.90） | worker 跑两次（改版后 + A/B 还原后）结果一致；docs-writer 复核 csv 与 surefire XML |
| **E3** | 运行期确实用 9.7.0 | `unzip -l target/fablix.jar \| grep mysql-connector` | `BOOT-INF/lib/mysql-connector-j-9.7.0.jar`（2607237 B）；jar 内该文件 SHA-256 = 与 Central 下载件一致（`0353648e…`）；`target/fablix.jar` 整体 SHA-256 = `a78305ce7baf5e36edbb2a09311e68f94b60b0b0f6e71346265bd631c0c21d77` | worker；docs-writer 复核 |
| **E4** | 真实 MySQL smoke（9.7.0, JDK 21, 8081） | `java -jar target/fablix.jar --spring.profiles.active=local`；`curl` 登录 → 加购 `tt9000001` → `/checkout/token` → 支付（`Dev/Customer/DEV-CARD-0001/2035-12-31`） | `/api/boot/health` 200；login 200 `success`；cart 200；token 200；payment 200 `Order placed successfully.`，`order.order_date=2026-10-20`，`total_amount=8.98`；库内 `sales` 新行 **`id=13562, customer_id=900001, movie_id=tt9000001, sale_date=2026-10-20`** | worker |
| **E5** | 员工路径 + 元数据读取 | `POST /api/boot/employee-login`（参数名是 `username`，不是 `email`）→ `GET /api/boot/dashboard/session` → `GET /api/boot/dashboard?action=metadata` | 200 / 200 `authenticated:true, role:employee` / 200 `{"status":"success","tables":[…10 张表…]}`，含 `sales`，列元数据 `id INT(10)`, `customer_id INT(10)`, `movie_id VARCHAR(10)`, **`sale_date DATE(10)`** | worker |
| **E6** | 日志告警面 | `grep -niE "ssl\|warn\|severe\|fatal\|exception\|error" /tmp/fablix-smoke.log` | 31 行日志中**仅 1 条命中**，且是**故意**用 `email=` 触发的一次应用层鉴权 WARN（`EmployeeController: Employee login rejected because credentials were invalid`）；**无任何 SSL/连接类告警**；HikariPool 启动两行 INFO 正常 | worker |
| **E7** | A/B 对照（8.3.0） | `cp pom.xml /tmp/…` → `sed` 改回 8.3.0 → `mvn package -DskipTests` → 跑同一路径（`FABLIX_BOOT_PORT=18084`）→ **还原 `pom.xml`** | 同样 `success`、`order_date=2026-10-20`、库内新行 **`id=13563`**、`sale_date=2026-10-20`；metadata 200 / 10 表 / `DATE(10)` 相同；`grep` 告警 **无匹配** → 与 9.7.0 **在日志警告面、日期语义、metadata 输出三个维度一致** | worker |
| **E8** | Java 11 运行时 smoke | 用 **JDK 21 构建**的 9.7.0 jar 在 `jdk-11.jdk` 上启动（worker: 18085；主 agent 重跑: **18086**） | `Starting FabflixApplication using Java 11.0.31`；health 200；登录/加购/token/支付 `success`；`order_date=2026-10-20`；库内新行 **`id=13564`（18085）/ `id=13565`（18086）**，`sale_date=2026-10-20`；日志无 SSL/WARN/ERROR；进程已停止 | worker + **主 agent 重跑闭合** |
| **E9** | jasypt 1.9.3 on JDK 21 | `JAVA_HOME=/opt/homebrew/opt/openjdk@21 … mvn --batch-mode --no-transfer-progress -Dtest=JasyptPasswordVerifierTest test` | `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`；**BUILD SUCCESS**；`grep -E "IllegalAccessError\|InaccessibleObjectException\|Unable to make\|--add-opens"` **无匹配**；**未替换 jasypt、未加 `--add-opens`** | worker |
| **E10** | docs-writer 只读复核 | `git diff`、`unzip -l/-p target/fablix.jar`、`shasum -a 256`、`jacoco.csv` 汇总、`target/surefire-reports/*.xml` 汇总、`grep -n "mysql.version\|protobuf"` 于 `~/.m2` 内的 `spring-boot-dependencies-2.7.18.pom` 与 `mysql-connector-j-9.7.0.pom`、`grep -rn "sales\|CURDATE\|LocalDate.now" src/main/java` | `pom.xml` 差异=1 行；jar 内仅 `mysql-connector-j-9.7.0.jar`（**无 protobuf-java、无 oci-java-sdk**）；`988/90/1078`；44 个 surefire XML 合计 `tests=134, failures=0, errors=0`；`.m2` 仅有 `spring-boot-dependencies/2.7.18`（`mysql.version=8.0.33`，L1606-1614 对 `mysql-connector-j` **排除 `com.google.protobuf:protobuf-java`**）；`src/main` 中 `sales` 只出现在 `PaymentRepository:19` 的 `INSERT … CURDATE()`，`LocalDate.now()` 只在 `PaymentController:104` | docs-writer |

**未执行（不得视为已通过）**：Java 11 上的完整 `mvn verify`（本步门槛是 JDK 21 下的 verify）；真实 MySQL 并发/连接池上限；TLS 实际协商状态（未查 `Ssl_version`）；`docker compose build app` 容器路径（属 S3/S6）。

---

## 一、修改了哪些文件及潜在问题

### 1.1 代码改动（**只有一处**，`pom.xml`）

| 文件 | 变更 | 目的 |
| --- | --- | --- |
| `pom.xml` | `com.mysql:mysql-connector-j` **`8.3.0` → `9.7.0`**（1 行） | 在**仍为 Boot 2.7.18 / Java 11** 的树上把 MySQL 驱动定版到 **Boot 3.5.15 BOM 的 `mysql.version`**，使 S3（#48）**不再动驱动**；并把「JDBC 行为差异」从「Boot 3 行为差异」中分离出来（拆分的核心收益）。 |

**未改动**（逐项确认）：`src/main/**`、`src/test/**`、`.github/workflows/ci.yml`、`Dockerfile`、`compose.yaml`、`README.md`、`database/**`、JaCoCo `<exclude>`（仍 8 项）、覆盖率阈值（仍 0.90）、`jasypt`（仍 1.9.3）、其他任何依赖版本。

### 1.2 文档改动（本次 docs-writer 写入）

| 文件 | 变更类型 | 目的 |
| --- | --- | --- |
| `docs/logs/2026-10-20-issue-47-dependency-readiness.md` | **新增**（本文件） | S2 的实施/验证记录：元数据、证据索引、逐条边界与残余风险、两大节总结 |
| `docs/plans/issue-44-migration-checklist.md` | **更新** | 状态总表 S2 行 → 「已实施（2026-10-20），待 PR 合并」＋分支/证据路径；§S2 补「实际执行结果」块；§S3 预期值注明「驱动已定版 9.7.0，保留显式 pin 并用 `dependency:tree` 复核」；顶部「本文件不表示任何步骤已完成」的表述按事实同步 |
| `docs/plans/issue-44-migration-steps.md` | **更新** | §3 总表 S2 行补状态与证据；顶部状态说明补「S1 已测量 / S2 已实施（均未提交、未建 PR）」 |
| `docs/issues/issue-47-dependency-readiness.md` | **更新** | 状态 → 「已实施（2026-10-20），待提交/PR」；**AC-1..AC-5 逐条回填**（含 AC-1 的条件分支**已触发**）；保留原 `## Scope` / `## Acceptance criteria` 结构与正文逐字性 |
| `docs/issues/issue-44-boot3-java17-jakarta-migration.md` | **更新（Internal tracking + 顶部状态行）** | 记录 **AC-6 的 JDBC 条件子句已触发**（S2 把驱动升到 9.7.0），证据指向 #47 记录；**其余 AC 与已发布的 `## GitHub issue body` 围栏文本未改** |

**grep 确认无需改动**：`docs/architecture/frontend-migration-contracts.md` 与 `README.md` **均未提及** `mysql-connector-j` / 驱动版本（`grep -rn "mysql-connector-j\|mysql\.version" docs/ README.md`：命中全部在 `docs/plans/` 与 `docs/issues/` 的历史计划/issue 文档，无契约文档或 README 命中）。契约文档 §11 的容器约定仍是 `temurin-11`，那属 **S3（#48）** 的两阶段镜像改动与 **S7（#52）** 的文档同步范围，**本步不改**。历史计划/路线图里「`mysql-connector-j:8.3.0`」是**迁移前状态的描述**（`docs/plans/ai-option-a-boot3-migration.md:51/132/168`、`docs/plans/ai-roadmap-overview.md:75`），属已批准计划的历史陈述，本步**不改**，由 S7 收尾时统一处理。

### 1.3 潜在问题、边界与残余风险

> 以下每一条都是**显式记录**的边界，不得在后续步骤中被当作「已验证通过」。**结论限定**：本步只证明「在 Boot 2.7.18 基线上把驱动换成 9.7.0，三个观察维度（日志告警面、日期语义、metadata 输出）与 8.3.0 一致，且在 Java 11 / Java 21 运行时都能完成一次真实结算写入」。

1. **驱动 `DATE` → Java 的读取映射未被应用读取路径锻炼**。`src/main` 里 `sales` 只出现在 `PaymentRepository:19` 的 `INSERT INTO sales (customer_id, movie_id, sale_date) VALUES (?, ?, CURDATE())`，**没有** `SELECT … FROM sales` 的读取路径（docs-writer 只读复核确认）。因此本次「回读」是用 `docker compose exec … mysql` 的 CLI 完成的，**不能**据此宣称「驱动把 `DATE` 映射成 `LocalDate`/`java.sql.Date` 的行为已验证」。→ 读取映射的验证归 S6（真实 `add_movie`/销售读取路径若被引入）或未来新增读取功能的 issue。
2. **双日期源，跨日/时区边界未构造**。响应里的 `order.order_date` 来自应用侧 `LocalDate.now()`（`PaymentController:104`），库侧 `sales.sale_date` 来自 `CURDATE()`（`PaymentRepository:19`）——**两条独立取值路径**。本次同日一致（宿主 PDT 与 MySQL SYSTEM/UTC 恰为同一日期 2026-10-20），**跨日边界（本地 00:00 前后）未构造**；8.3.0 与 9.7.0 行为相同 → 属**既有语义**，**不是驱动回归**。
3. **连接池 / 并发未验证**。Hikari 连接池上限、并发下单/库存竞争、死锁与重试均未取证（smoke 是单线程串行脚本）。→ **顺延 S6**（沿用 `docs/logs/2026-10-20-issue-46-baseline.md` 的「非自动化覆盖清单」第 3 项）。
4. **TLS 实际状态未验证**。`.env` 连接串无 `sslMode` 参数 → 驱动默认 `PREFERRED`；本次只证明「日志里没有新增 SSL/连接告警」，**未查** `SHOW SESSION STATUS LIKE 'Ssl_version'` 之类的实际协商结果，也未比较 8.3.0 与 9.7.0 在该状态上的差异。→ **顺延 S6 / 部署类 issue**（若生产要求强制 TLS）。
5. **dev 库被写入 4 行测试数据**。`sales` 新增 **`id=13562`（9.7.0 @8081）、`13563`（8.3.0 A/B @18084）、`13564`（Java 11 @18085）、`13565`（Java 11 @18086，主 agent 重跑）**，`customer_id=900001`，`sale_date=2026-10-20`。保留或清理均可；清理 SQL：`DELETE FROM sales WHERE id IN (13562,13563,13564,13565);`。注意 `database/dev-seed.sql` **只在数据卷首次初始化时**才被自动执行，重跑容器**不会**自动清掉这些行。
6. **未执行 `docker compose down`**（按「保持原状」处理）：MySQL 容器在本步开工前已 `Up 7 days (healthy)`，结束时仍 healthy，数据卷 `fabflix-mysql-data` 未动。若后续步骤需要干净环境，须由主 agent 决定（`down -v` 会丢数据，属破坏性操作）。
7. **「S1 基线 = 134 / 0.9165」是引用错误（已在文档中改正）**。worker 报告把 134/0.9165 称为「S1 基线」不准确：S1 快照是 **127 tests / 968-110-1078 = 0.8980**（当时门槛 **0.85**，见 `docs/logs/2026-10-20-issue-46-baseline.md`，该历史记录**不追改**）；**134 / 0.9165 是 2026-10-20 门槛收紧（0.85→0.90、88→90）后的新比较基线**（checklist 的「2026-10-20 覆盖率门槛收紧」表）。本步实际对齐的是**后者**，结论不受影响，但引用必须写对。
8. **Boot 3.5.15 BOM 只经 `curl` 核对，未在本地解析**。`~/.m2/repository/org/springframework/boot/spring-boot-dependencies/` 下**只有 `2.7.18`**（docs-writer 复核），因此 `9.7.0 = <mysql.version>` 的证据来自 worker 的 Maven Central 下载与行号核对（3.5.15 pom L150 等），**不是**本地 BOM 解析结果。→ **S3（#48）必须以 `mvn dependency:tree`（或 `dependency:list`）复核 `mysql-connector-j` 实际解析到的行**，并在 PR 里贴出该行。
9. **驱动「POM 声明的传递依赖」与「实际解析到的依赖」要分清**。9.7.0 自身 pom **声明** `com.google.protobuf:protobuf-java:4.31.1`（非 optional）与 `com.alibaba:oci-java-sdk-common:3.66.0`（`<optional>true</optional>`），但 Boot BOM 的 managed 条目对 `mysql-connector-j` **带 exclusion**（`spring-boot-dependencies-2.7.18.pom:1606-1614` 排除 `protobuf-java`；3.5.15 同样排除），optional 依赖不解析 → **实际依赖集合相对 8.3.0 无任何新增条目**（`target/fablix.jar` 的 34 个 `BOOT-INF/lib/` 中既无 `protobuf-java` 也无 `oci-java-sdk`，docs-writer 复核）。这既**不违反**「不动其他依赖」的约束，也说明「升级驱动带来了新依赖」这个说法是错的。
10. **`9.7.0` 不是 Maven Central 上的最新版**（`maven-metadata.xml` 的 `<release>` 为 `26.10.0`）。选它的**唯一理由**是「它是目标 Boot 3.5.15 BOM 管理的版本」，以兑现「S3 不再动驱动」。若 S3 最终不跟随该 BOM 版本号，**必须重新定版并记录**。
11. **jasypt 只验证了 password-verifier 路径**（与本步 AC 一致）：`JasyptPasswordVerifier` → `org.jasypt.util.password.StrongPasswordEncryptor` 在 JDK 21 下无 `IllegalAccessError` / `InaccessibleObjectException`、无需 `--add-opens`。项目未使用 `org.jasypt.encryption.*` 等反射路径，因此那些路径**未覆盖**；**未替换 jasypt**（替换属范围变化，须回主 agent 裁定）。
12. **Java 11 只做了运行时 smoke**：用 JDK 21 构建的 9.7.0 jar 在 Java 11.0.31 上完成一次真实结算写入并成功，但**未在 Java 11 上跑测试套件**。这与「Java 11 是当前部署目标、JDK 21 是构建工具链」的事实一致，但「Java 11 上全绿」**不可声称**。
13. **本步交付未闭环**：`pom.xml` 改动与本文档**均未提交/未推送、无 PR、未合并**；#47 仍 **OPEN**，远端 `main` 未变。因此任何「S2 已完成」「驱动已升级到 9.7.0（对仓库而言）」的说法在合并前都**不成立**——准确表述是「已实施（本地工作区），待 PR 合并」。
14. **提交时必须只 add 预期文件**：工作区另有本任务前即存在的未跟踪 `?? .idea/`（以及 `.pi/artifacts/`，不在 `docs/`）。提交范围应严格是 `pom.xml` + 本步 `docs/` 下的 5 个文件，避免把 IDE 目录或 agent 产物带入。
15. **未发现其他已知遗留问题**：本轮代码改动只有 1 行依赖版本，无 Java 源码/测试/CI/Docker 行为变化；回退方式 = `git checkout -- pom.xml`（或 revert 1 行）即可，**不改变行为**。

---

## 二、新增知识点 / 重点学习对象

### Java / 构建与依赖管理

- **「显式版本 pin」只覆盖*版本*，不覆盖*managed 条目的 exclusions / scope***
  - 含义：`pom.xml` 给 `mysql-connector-j` 写了 `<version>9.7.0</version>`，但该依赖**仍然是被 BOM 管理（managed）的依赖**——因此 BOM 中该 `dependencyManagement` 条目里的 `<exclusions>`（这里排除 `com.google.protobuf:protobuf-java`）与 `scope` 依然生效。所以「显式 pin」= **改版本号**，≠「脱离 BOM 的所有约束」。
  - 位置：`pom.xml:57-61`（显式版本）；`~/.m2/.../spring-boot-dependencies-2.7.18.pom:1606-1614`（managed 条目 + exclusion）；`mysql-connector-j-9.7.0.pom:64`（自身声明的 `protobuf-java:4.31.1`）
  - 学习价值：判断「升一个依赖会不会顺带引入新 jar」时，必须同时看 **依赖自身 pom**、**BOM managed 条目的 exclusions**、**optional 标记** 三处；只看依赖自身 pom 会得出错误结论（本步任务书起初就是这么误判的）。
- **跨版本「提前定版」：用目标 BOM 的版本值定当前依赖**
  - 含义：当前生效 BOM 是 Boot **2.7.18**（`<mysql.version>8.0.33</mysql.version>`，`spring-boot-dependencies-2.7.18.pom:167`），目标是 Boot **3.5.15**（`<mysql.version>9.7.0</mysql.version>`）。S2 故意在 2.7 的树上把驱动定到**目标 BOM 的版本**，使 S3 升级 BOM 时驱动「已经在目标状态」，JDBC 差异也就在 S3 之前被单独验证过。
  - 位置：`pom.xml:23`（`spring-boot.version=2.7.18`）、`pom.xml:57-61`；证据 E1
  - 学习价值：迁移工程里「把能提前对齐的东西提前对齐」是降低单次 PR 风险的一般手法；代价是**中间态偏离当前 BOM**，必须把理由写在文档里（本步 AC-1 就是要求这件事）。
- **用 class file major version 证明「这个 jar 能在 Java N 上加载」**
  - 含义：`.class` 文件头 8 字节 `ca fe ba be` + minor + major；`0x34 = 52` → **Java 8**，`55 = 11`、`61 = 17`、`65 = 21`。JVM 拒绝加载 major 高于自身支持版本的 class。因此「驱动 9.7.0 的 class 全是 52 → 在 Java 8/11/17/21 上都能加载」是**字节码级证据**，比读发行说明更硬。
  - 位置：`unzip -p mysql-connector-j-9.7.0.jar com/mysql/cj/jdbc/Driver.class | xxd -l 16`；对照 S3 的验收 `javap -verbose … | grep 'major version'` → 61
  - 学习价值：这也是 #44 AC-11 取证手法的同源技巧（`javap`/`xxd` 读 major version）；顺带要学会看**是否 MR-jar**（jar 内有无 `module-info.class`）——单 release 才能用「一个字节码版本覆盖所有 JDK」的推论。
- **`.m2` 里没有的 BOM，不能假装它解析过**
  - 含义：本地只有 `spring-boot-dependencies/2.7.18`，所以「3.5.15 BOM 的 `mysql.version`」这一事实只能靠**从 Maven Central 下载 pom 并核对行号**（HTTP 200）来取证，并明确标注「未在本地解析」。S3 拿到真 BOM 后必须用 `dependency:tree` 复核。
  - 位置：`~/.m2/repository/org/springframework/boot/spring-boot-dependencies/`（只有 `2.7.18`）；§1.3 第 8 条
  - 学习价值：**证据等级**意识——「curl 到的 pom 文本」＜「本地解析出的依赖树」；写进文档时要标明是哪一级。

### JDBC / 数据库

- **`CURDATE()`（数据库时钟）与 `LocalDate.now()`（JVM 时钟）是两条独立日期源**
  - 含义：`sales.sale_date` 由 **MySQL 服务端**的 `CURDATE()` 决定；API 响应里的 `order.order_date` 由 **JVM 本地时区**的 `LocalDate.now()` 决定。二者只在「JVM 时区日期 == MySQL 会话时区日期」时相等；跨日窗口（例如 PDT 17:00 = UTC 次日 00:00 之后）会出现「库里写 D、响应显示 D+1」。
  - 位置：`src/main/java/com/fabflix/boot/commerce/PaymentRepository.java:19`（`INSERT … CURDATE()`）、`PaymentController.java:104`（`LocalDate.now()`）
  - 学习价值：这是「时区 bug 报告」最常见的误判源——先分清是**驱动/连接时区换算**问题，还是**两个时钟**问题。本步的 A/B 证明 8.3.0 与 9.7.0 在这一点上**行为相同**，因此它属于既有语义，不该记成驱动回归。
- **`DatabaseMetaData` 是「驱动能力」的另一条观测面**
  - 含义：员工 dashboard 的 `action=metadata` 走 `DatabaseMetaData` 遍历表与列类型，输出 `sales` 的 `sale_date DATE(10)`。这条路径**真实经过驱动**，因此它比单测更能反映驱动差异；但同时它**不经过**业务 SQL 的 result-set 映射。
  - 位置：`/api/boot/dashboard?action=metadata`（E5）；证据里 `sale_date DATE(10)` 在 8.3.0 与 9.7.0 下一致
  - 学习价值：判断「某个驱动行为到底有没有被验证」要问「它经过哪条代码路径」——metadata 路径 ≠ `SELECT` 映射路径（§1.3 第 1 条就是这条的诚实边界）。
- **MySQL Connector/J 9.x 默认 `sslMode=PREFERRED`**
  - 含义：连接串不写 `sslMode` 时，驱动会**尝试** TLS、失败则回落到明文；因此「没有 SSL 告警」只说明没有失败到产生告警，**不说明**连接实际加密。
  - 位置：`.env` 的 `FABFLIX_DB_URL`（无 `sslMode`）；§1.3 第 4 条
  - 学习价值：安全相关的「未告警」≠「已加密」；要证明 TLS 须查会话状态（`Ssl_version`）或显式设 `sslMode=REQUIRED`。

### 测试与验证方法

- **A/B 对照：用「旧版本构同一个 jar」制造对照基线，再还原**
  - 含义：仓库里**不存在** 8.3.0 的真库 smoke 记录（S1 已明记该 smoke「未执行」），因此「没有*新增*告警」无从比较。做法：临时把 `pom.xml` 改回 8.3.0 → `mvn package -DskipTests`（3.1 s）→ 把 jar 拷到 `/tmp` → **立即还原** `pom.xml` → 用同一路径、同一环境跑一遍。这样「新/不新」才有对照。
  - 位置：E7；`git diff` 最终仍只有 9.7.0 那一行（还原可验证）
  - 学习价值：**对照实验的设计纪律**——(a) 只改一个变量；(b) 对照产物放 `/tmp` 不入库；(c) 立刻还原并**用 `git diff` 自证**；(d) 结论必须限定在比较过的维度上（本步限定为「日志警告面、日期语义、metadata 输出」三项，不推广成「行为完全等价」）。
- **用 `cookie jar` 跑真实 HTTP 业务路径，比 MockMvc 更接近生产**
  - 含义：`curl -c/-b /tmp/jar.txt` 保存并回放 session cookie，即可串起「登录 → 加购 → checkout token → 支付 → confirmation」这条依赖服务端 session 的链路——这是 MockMvc 单测（无 `@SpringBootTest`、无真实 DB）覆盖不到的部分。
  - 位置：E4/E5/E8 的 curl 序列
  - 学习价值：低成本「半真实」e2e；也是发现「参数名踩坑」的地方（`/api/boot/employee-login` 的参数是 `username` 而非 `email`，用错只得到 `200 {"status":"fail"}` —— **HTTP 200 掩盖业务失败**，也是应当警惕的既有 API 语义）。
- **独立复核覆盖率与用例数：`jacoco.csv` 加总 + surefire XML 加总**
  - 含义：`All coverage checks have been met.` 只是插件的结论，复核要自己算：把 `target/site/jacoco/jacoco.csv` 各行的 `LINE_COVERED` / `LINE_MISSED` 求和 → `988/90/1078 = 0.9165`；把 `target/surefire-reports/*.xml` 的 `tests=` 属性求和 → `134`（44 个 XML 文件）。二者与报告一致才算取证成功。
  - 位置：`target/site/jacoco/jacoco.csv`、`target/surefire-reports/`；E10
  - 学习价值：reviewer 视角的「不只看结论行，还要重算」；也解释了为什么「换驱动不会改变覆盖率」——驱动不是被插桩的项目类，分母不变。
- **复现环境必须记录到「端口 / 进程 / 容器状态」粒度**
  - 含义：本步在同一台机器上先后用了 8081（正式 smoke）、18084（8.3.0 A/B）、18085/18086（Java 11），每个进程都 `pkill` 并 `lsof -iTCP:<port>` 确认无残留；MySQL 容器状态与 `SELECT VERSION()` 一并记录。否则「两次结果不同」无法排除端口/进程串扰。
  - 位置：E4/E7/E8；元数据表的「运行时」「数据库环境」行
  - 学习价值：「环境也是证据的一部分」，迁移类工作尤其如此（S1 的日志已把 commit/Maven/JDK/Node/tz 全部固定）。

### 运行时兼容

- **2014 年的 jasypt 1.9.3 能在 JDK 21 上用，但「能」需要被测量**
  - 含义：JDK 9+ 的强封装会让老旧反射库抛 `InaccessibleObjectException` / `IllegalAccessError`。实测 `JasyptPasswordVerifierTest`（走 `StrongPasswordEncryptor`）在 JDK 21 上 1 个用例通过、**无需 `--add-opens`**。但这只覆盖**密码校验**这一条路径。
  - 位置：`src/main/java/.../auth/JasyptPasswordVerifier`（`org.jasypt.util.password.*`）；E9
  - 学习价值：JDK 升级的「隐性成本清单」里，老依赖的反射访问和「只覆盖部分 API」是两个独立的坑；先测被真正使用的路径，再把未覆盖路径写成显式风险，而不是声称「jasypt 兼容 JDK 21」。

---

## 结论（一句话）

**在 Boot 2.7.18 / Java 11 基线上，把 `mysql-connector-j` 从 8.3.0 定版到 9.7.0（= 目标 Boot 3.5.15 BOM 的 `mysql.version`）已完成实施与验证：JDK 21 下 `mvn verify` 134 用例全绿、覆盖率 988/1078 = 0.9165；真实 MySQL 结算写入与员工 metadata 路径在 9.7.0、8.3.0（A/B）与 Java 11 运行时三种条件下日期语义与告警面一致；jasypt 在 JDK 21 上通过。** 但：读取映射、跨日边界、连接池/并发、TLS 状态**未验证**（顺延 S6）；改动**未提交、未推送、无 PR**，#47 仍 OPEN。

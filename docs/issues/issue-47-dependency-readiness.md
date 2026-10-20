# Step 2/7 — 非 Spring 依赖就绪（JDBC 驱动 / jasypt）（GitHub issue #47 记录）

> **文档状态：🟢 已在 GitHub 存在（[#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)），无需新建** —— 2026-10-19 由 `github-operator` 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**，仅 GitHub 在末尾追加一个换行；`## Internal tracking` 分区**未发布**）。~~**本文件不表示该步骤已实施**：Scope / Acceptance criteria 全部未执行，无源码/配置/测试改动、无 PR。~~ → **该句已于 2026-10-20 失效**（见下一行）。
> **S2 进度（2026-10-20）**：**已实施（本地工作区），待提交 / PR / 合并** —— `pom.xml` 把 `com.mysql:mysql-connector-j` **8.3.0 → 9.7.0**（1 行，其余零改动）；JDK 21 下 `mvn verify` **134 tests / 0 failures / 0 errors**、JaCoCo **988/1078 = 0.9165**；真实 MySQL smoke（9.7.0、8.3.0 A/B、Java 11 运行时）通过；jasypt 1.9.3 在 JDK 21 上通过。**AC-1..AC-5 已逐条回填**（见下方 `## Internal tracking` → 「实施结果与 AC 回填」）。**本次改动未提交、未推送、无 PR**，#47 **保持 OPEN**；分支 `codex/issue-47-dependency-readiness` **尚未创建**。完整证据：`docs/logs/2026-10-20-issue-47-dependency-readiness.md`。
> **远端元数据（主 agent 核验，2026-10-19）**：编号 **#47** · 标题 `[Step 2/7] Validate non-Spring dependency readiness on the Boot 2.7 baseline` · milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) · labels **空** · state **OPEN** · 正文含显式 `Part of #44` · 上游 epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**）。
> `github-operator` 只发布 `## GitHub issue body` 分区（四反引号围栏只是本地容器，发布时去掉围栏行本身）；`## Internal tracking` **不上 GitHub**。
> 拆分总览：`docs/plans/issue-44-migration-steps.md`（**已批准并已创建子 issue，2026-10-19**）。
> **本轮修订（oracle P1-2 / P2-7）**：正文加显式 `Part of #44`；明确“若结论为保持 8.3.0，则 #44 AC-6 的 JDBC 分支不触发”。
> ~~本文件**不声称**任何验证已运行；`mysql-connector-j` 9.x 与 Boot 2.7.18 / Java 11 的兼容性、jasypt 1.9.3 在 JDK 21 下的行为均**未在本仓库实测**。~~ → **该句为 2026-10-19 起草时的状态，已不适用**：三项均已于 **2026-10-20 实测**（驱动升级至 9.7.0、Boot 2.7.18 + Java 11/21 双运行时 smoke、jasypt on JDK 21），结果见下方「实施结果与 AC 回填」。

## GitHub issue title

```
[Step 2/7] Validate non-Spring dependency readiness on the Boot 2.7 baseline
```

## GitHub issue body

````markdown
## Goal

Decide and validate the **non-Spring dependency changes** that the Boot 3.x migration needs — above all the MySQL JDBC driver version — **while the application is still on Spring Boot 2.7.18 / Java 11**.

**Why a separate step:** Spring Boot 3.5 manages a newer `mysql-connector-j` (9.x) than the version currently pinned (8.3.0). If the driver upgrade is folded into the Boot 3 upgrade, a JDBC behaviour change (time zone handling, `CURDATE()`/date mapping, SSL warning severity) becomes indistinguishable from a Spring Boot behaviour change. Doing it here first gives the JDBC delta its own green finish line.

**This issue covers only this step.** Spring Boot stays at 2.7.18, Java stays at 11, no `javax` → `jakarta` change, no CI/Docker change.

## Background

**Part of #44.**

This is **step 2 of 7** in the Spring Boot 3.x / Java 17 / Jakarta migration tracked by #44 (`docs/plans/ai-option-a-boot3-migration.md`, split in `docs/plans/issue-44-migration-steps.md`).

Current state:

- `pom.xml` pins `com.mysql:mysql-connector-j` to **8.3.0** explicitly (it does not follow the BOM for this artifact).
- `pom.xml` pins `org.jasypt:jasypt` to **1.9.3** (a 2014-era release). Only `org.jasypt.util.password.*` is used (online password verification and the offline password-initialisation utilities).
- The tests are MockMvc-based (no `@SpringBootTest`), so **`mvn verify` alone does not exercise a real MySQL connection** — JDBC behaviour needs a real database.

## Scope

- [ ] Decide the target `mysql-connector-j` version and write the decision down with its reason. Default: take the version managed by the **Boot 3.5 BOM** (9.x) so that step 3 does not have to touch the driver again. Alternative, allowed conclusion: **keep 8.3.0** if the upgrade cannot be validated on the Boot 2.7 / Java 11 baseline; in that case record the decision and treat the upgrade as a separate follow-up issue (it is *not* folded into step 3).
- [ ] Verify the chosen version's minimum runtime JDK requirement against the current build (Java 11 today, JDK 21 toolchain after step 4) and record the evidence.
- [ ] Run `mvn --batch-mode --no-transfer-progress verify` and confirm the test count, failures/errors and JaCoCo gate match step 1's baseline.
- [ ] Run a **real-database smoke** against Compose MySQL (`docker compose up -d mysql`, application started against it with the local profile): exercise the customer checkout path and the employee sales path so that a row is written and read back. Confirm that
      (a) the stored and displayed dates agree with `sales.sale_date` (no time-zone offset),
      (b) no `CURDATE()`/date-mapping drift appears,
      (c) no **new fatal** SSL/connection warning is introduced (a non-fatal warning is acceptable, but it must be recorded verbatim).
- [ ] Check `jasypt:1.9.3` under **JDK 21**: run the password-verification tests with `JAVA_HOME` pointing at JDK 21 and confirm there is no `IllegalAccessError` / `InaccessibleObjectException` and no reliance on removed JDK APIs. If the Boot 2.7 test harness cannot run on JDK 21 at all, **defer this check to step 3 and say so** — do not change the harness to force it.
- [ ] Record every finding in the step's plan document / completion log, including "no difference observed" as an explicit statement rather than silence.

## Non-goals

- No Spring Boot version change (stays 2.7.18) and no Java/JDK target change.
- No `javax` → `jakarta` change and no architecture-guard-test change (step 3).
- No other dependency upgrades: not Mockito, ByteBuddy, JUnit, JaCoCo, surefire, the Maven plugins, Jackson/HikariCP/Logback/Tomcat (those arrive transitively with Boot 3 in step 3).
- **No replacement of `jasypt`.** If the password-verification implementation turns out to be unusable on the target JDK, that is a **scope change** and must go back to the parent agent for a ruling; it is not decided inside this issue.
- No test additions or test rewrites, no behaviour change, no URL/status/JSON change.
- No CI, Dockerfile or README change.

## Dependency

None — this step can run **in parallel** with step 1 and must be finished **before** step 3.

## Acceptance criteria

- [ ] **AC-1** The `mysql-connector-j` decision is recorded: the chosen version, why, and the compatibility evidence used (BOM version listing and/or the version's documented JDK requirement). **If the recorded conclusion is “keep 8.3.0”, state explicitly that #44 AC-6's JDBC clause ("if the JDBC driver is upgraded to 9.x") is therefore not triggered** — it is a conditional branch, not an unmet criterion.
- [ ] **AC-2** `mvn --batch-mode --no-transfer-progress verify` passes with the same test count as step 1's baseline (`Tests run`, `Failures: 0`, `Errors: 0`) and `All coverage checks have been met.`.
- [ ] **AC-3** The real-database smoke result is recorded with the concrete evidence: date values written and read back, the corresponding `sales.sale_date` value, and any connection/SSL warning text. If the environment (Docker, port, database) is unavailable, the record says **not executed** and why — never "passed".
- [ ] **AC-4** The jasypt-on-JDK-21 check is recorded as passed, failed, or deferred-to-step-3, with the command and output summary.
- [ ] **AC-5** `git diff` for the pull request touches only `pom.xml` (the dependency version) plus documentation; no Java source, no tests, no CI, no Dockerfile.

## Risk notes

- **Driver 9.x on Boot 2.7.18 / Java 11 is unverified in this repository.** The merge is expected to work (the driver is not managed by Boot at runtime), but the documented minimum JDK of the chosen 9.x release must be checked before merging.
- **JDBC behaviour deltas** around time zones, date/timestamp mapping, `CURDATE()` return handling and SSL warning severity are the specific reason this step exists; they are covered only by the real-database smoke, not by the unit tests.
- **Environment dependence**: the real-database smoke needs Docker and a free database port. If unavailable, the honest outcome is "not executed", which reduces the value of this step — in that case say so and let step 6 cover the JDBC checks in the final state.
- **`jasypt:1.9.3` on JDK 21** may hit reflective-access restrictions (2014-era library). If it does, that is a **blocking finding for the migration** and a scope question, not something to patch silently here.

## Rollback

Revert the single dependency version line (and the documentation). If the smoke exposes a behaviour difference that cannot be accepted, the compliant rollback is **keep 8.3.0 and record the decision** — that is a valid, non-blocking outcome for step 3.
````

## Internal tracking

> 本分区**不发布到 GitHub**。

| 项目 | 值 |
| --- | --- |
| 步骤 | **S2 / 7**（拆分总览 §3、§4-S2；§6 无专属裁定项） |
| GitHub issue 编号 | **#47** —— https://github.com/PNZ-z/ai-powered-Fabflix/issues/47 |
| 远端标题 | `[Step 2/7] Validate non-Spring dependency readiness on the Boot 2.7 baseline` |
| 远端 state | **OPEN**（2026-10-19 创建；`Part of #44` 已在正文） |
| 来源 | `docs/plans/ai-option-a-boot3-migration.md` §4.3（依赖兼容性核查清单）、§6.1 AC-A-6、P2-8（驱动 9.x 的时区/`CURDATE()`/SSL 差异）；#44 正文 `Scope → Dependency compatibility check`；`docs/plans/issue-44-migration-steps.md` §1.2（拆分理由：把 JDBC 差异与 Boot 3 差异分离） |
| 关联 | 上游 epic：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**；正文 `## Steps` 列出 #46–#52）；正文含显式 `Part of #44`（P1-2），**已随创建发布** |
| 依赖关系 | 无前置；**S3 依赖本步**（驱动定版后 S3 不再动驱动） |
| milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) |
| labels | **空** |
| 状态 | **🟢 已在 GitHub 存在（#47），无需新建** —— `github-operator` 按批准草稿创建（正文逐字一致，除末尾换行）；`## Internal tracking` 未发布。**2026-10-20：已实施（本地工作区），待提交 / PR** —— 代码改动 = `pom.xml` 1 行（驱动 8.3.0 → 9.7.0）；**未提交、未推送、无 PR**，分支 `codex/issue-47-dependency-readiness` 尚未创建；#47 保持 **OPEN** |
| 估算 | 1 天 |
| 负责人角色 | docs-writer → oracle → 主 agent 批准 → 单一 `worker`（改 1 行版本 + 跑验证）→ 3 reviewer → `worker` 修复 → docs-writer 回填 → 主 agent 提交 → `github-operator` 建 PR |
| 事实来源（可核验） | 计划文档：`com.mysql:mysql-connector-j` 曾显式 pin `8.3.0`、`org.jasypt:jasypt:1.9.3`；jasypt 仅用 `org.jasypt.util.password.*`（计划 §4.1/§4.3）；测试无 `@SpringBootTest`（契约文档 §7）。**2026-10-20 实测已取代「未实测」的旧表述**：仓库 `pom.xml` 当前为 `9.7.0`；`9.7.0` = 目标 Boot **3.5.15** BOM 的 `mysql.version`（Maven Central 下载 pom 核对，本地 `.m2` 仅有 `2.7.18`）；`Driver.class` 的 class file major = **52**（Java 8）；`target/fablix.jar` 内为 `mysql-connector-j-9.7.0.jar` |
| 未确认 | **已闭合（2026-10-20 实施）**：① 9.x 与 Boot 2.7.18 / Java 11 的兼容性（U-A2）→ **已验证**：JDK 21 `mvn verify` 全绿 + Java 11.0.31 运行时 smoke 完成真实结算写入；② 所选 9.x 的最低 JDK → **Java 8**（字节码 major 52 + 上游 `build.xml`/`README`，`dev.mysql.com` 抓取 403）；③ 时区/`CURDATE()`/SSL 是否出现差异 → **三个观察维度与 8.3.0 一致**（A/B 对照）；④ Docker/3307 → **可用**（`mysql:8.4`，8.4.11，healthy）；⑤ Boot 2.7 能否在 JDK 21 上跑 → **能**（S1 已证，本步复跑仍全绿）。**仍未确认（顺延）**：驱动 `DATE`→Java 的**读取**映射（`src/main` 无 `SELECT … FROM sales`）、跨日/TZ 边界、连接池/并发、TLS 实际协商状态 → **S6** |
| 待裁定 | **已闭合（2026-10-19）**：oracle 确认“保持 8.3.0 + 另立后续 issue”是**合规结论**，与 #44 AC-6 的“if the JDBC driver is upgraded to 9.x”条件表述一致（P2-7：AC-1 已补“条件不触发”的写法）。**是否创建本 issue → 已创建 #47（2026-10-19）**。**2026-10-20 定版结果为「升级到 9.7.0」→ 备选路径（保持 8.3.0）未采用，AC-1 的条件分支按「已触发」记录**；无需新的用户裁定（升级方向是 #47 正文 Scope 第 1 条的默认结论）。 |

### 实施结果与 AC 回填（2026-10-20）

> 本小节**不发布到 GitHub**；发布用的 `## Scope` / `## Acceptance criteria` 文本**逐字未改**（复选框仍为 `- [ ]`，远端不做写操作）。完整证据见 `docs/logs/2026-10-20-issue-47-dependency-readiness.md`。

| AC | 结果 | 证据 |
| --- | --- | --- |
| **AC-1** 驱动定版决策 | ✅ **选定 `mysql-connector-j` 9.7.0**。理由：9.7.0 = **目标 Boot 3.5.15 BOM 的 `mysql.version`**（S3 不再动驱动；P1-8 要求 S3 保留本步的显式 pin）。兼容性证据：Maven Central jar/pom **200**、SHA-256 `0353648e…`；`com/mysql/cj/jdbc/Driver.class` = `cafe babe 0000 0034` → **major 52（Java 8）**，全 jar 1066 个 `.class` 无 > 52；jar 内无 `module-info.class`；上游 `build.xml`/`README` 的 “Java 8 required”。**条件分支已触发**：#44 AC-6 的 JDBC 子句（“if the JDBC driver is upgraded to 9.x”）**已生效**，证据由本步承担（日志 E4–E7），终态复核归 S6 |
| **AC-2** `mvn verify` 与基线一致 | ✅ JDK 21：**`Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`** + `All coverage checks have been met.`；JaCoCo **988 covered / 90 missed / 1078 = 0.9165**（门槛 0.90）。**对照的是 2026-10-20 门槛收紧后的基线**（134 / 0.9165）；S1 快照 127 / 0.8980（门槛 0.85）仅作历史记录 | 日志 E2/E10 |
| **AC-3** 真实数据库 smoke | ✅ **已执行**（非“未执行”）：9.7.0 + JDK 21 + 8081 完成「顾客登录 → 加购 `tt9000001` → checkout token → 支付」→ `success`，响应 `order_date=2026-10-20`，库内 `sales` 新行 `id=13562, sale_date=2026-10-20`；员工登录 → dashboard session → `metadata` 200（10 表含 `sales`，`sale_date DATE(10)`）。告警原文：唯一 WARN 是**故意**用 `email=` 触发的一次鉴权失败；**无 SSL/连接告警**。附加：**8.3.0 A/B**（`id=13563`）与 **Java 11.0.31 运行时**（`id=13564`/`13565`）同日同语义 | 日志 E4–E8 |
| **AC-4** jasypt on JDK 21 | ✅ **通过**（未顺延、未替换 jasypt）：`JAVA_HOME=…openjdk@21 mvn -Dtest=JasyptPasswordVerifierTest test` → 1 test / **BUILD SUCCESS**；无 `IllegalAccessError` / `InaccessibleObjectException`；无需 `--add-opens`。仅覆盖 password-verifier 路径 | 日志 E9 |
| **AC-5** diff 范围 | ✅ `git diff` = `pom.xml` **1 行**（8.3.0 → 9.7.0）；另有本步 `docs/` 文档。无 `src/**`、无测试、无 CI、无 `Dockerfile`、无其他依赖版本、无 JaCoCo `exclude`/阈值改动。**但改动尚未提交/推送/PR** | 日志 §1.1/§1.2 |

**本步未闭环的动作**（待主 agent / `github-operator`）：① 创建分支 `codex/issue-47-dependency-readiness` 并提交（`pom.xml` + 5 个 `docs/` 文件）；② 推送；③ 建 PR（PR 说明需含 9.7.0 定版理由、AC 回填与未验证边界）；④ 合并后由主 agent 更新本文件状态与 checklist 为「已完成（PR #n 已合并）」；⑤ #47 的关闭口径由主 agent 决定（本文件**不自行标记完成**）。
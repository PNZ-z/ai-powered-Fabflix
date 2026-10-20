# Step 2/7 — 非 Spring 依赖就绪（JDBC 驱动 / jasypt）（GitHub issue #47 记录）

> **文档状态：🟢 已在 GitHub 存在（[#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)），无需新建** —— 2026-10-19 由 `github-operator` 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**，仅 GitHub 在末尾追加一个换行；`## Internal tracking` 分区**未发布**）。**本文件不表示该步骤已实施**：Scope / Acceptance criteria 全部未执行，无源码/配置/测试改动、无 PR。
> **远端元数据（主 agent 核验，2026-10-19）**：编号 **#47** · 标题 `[Step 2/7] Validate non-Spring dependency readiness on the Boot 2.7 baseline` · milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) · labels **空** · state **OPEN** · 正文含显式 `Part of #44` · 上游 epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**）。
> `github-operator` 只发布 `## GitHub issue body` 分区（四反引号围栏只是本地容器，发布时去掉围栏行本身）；`## Internal tracking` **不上 GitHub**。
> 拆分总览：`docs/plans/issue-44-migration-steps.md`（**已批准并已创建子 issue，2026-10-19**）。
> **本轮修订（oracle P1-2 / P2-7）**：正文加显式 `Part of #44`；明确“若结论为保持 8.3.0，则 #44 AC-6 的 JDBC 分支不触发”。
> 本文件**不声称**任何验证已运行；`mysql-connector-j` 9.x 与 Boot 2.7.18 / Java 11 的兼容性、jasypt 1.9.3 在 JDK 21 下的行为均**未在本仓库实测**。

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
| 状态 | **🟢 已在 GitHub 存在（#47），无需新建** —— `github-operator` 按批准草稿创建（正文逐字一致，除末尾换行）；`## Internal tracking` 未发布。**实现未开始** |
| 估算 | 1 天 |
| 负责人角色 | docs-writer → oracle → 主 agent 批准 → 单一 `worker`（改 1 行版本 + 跑验证）→ 3 reviewer → `worker` 修复 → docs-writer 回填 → 主 agent 提交 → `github-operator` 建 PR |
| 事实来源（可核验） | `pom.xml`：`com.mysql:mysql-connector-j` 显式 `8.3.0`、`org.jasypt:jasypt:1.9.3`（本轮只读核对）；Boot 3.5 BOM 管理 9.x 来自计划 §4.3 与 #44 正文（**未在本仓库实测**）；jasypt 仅用 `org.jasypt.util.password.*`（计划 §4.1/§4.3）；测试无 `@SpringBootTest`（契约文档 §7） |
| 未确认 | ① 9.x 与 **Boot 2.7.18 / Java 11** 的兼容性（U-A2）；② 所选 9.x 版本的最低 JDK 要求；③ 时区/`CURDATE()`/SSL 警告是否真的出现差异（**必须实测**）；④ 本地 Docker 与 3307 端口可用性；⑤ Boot 2.7 是否能在 JDK 21 上跑（决定 jasypt 检查是否需要顺延到 S3） |
| 待裁定 | **已闭合（2026-10-19）**：oracle 确认“保持 8.3.0 + 另立后续 issue”是**合规结论**，与 #44 AC-6 的“if the JDBC driver is upgraded to 9.x”条件表述一致（P2-7：AC-1 已补“条件不触发”的写法）。**是否创建本 issue → 已创建 #47（2026-10-19）**。 |
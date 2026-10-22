# Issue #44 拆分总览 — Option A 迁移的 7 个可独立实现/验证的步骤

> **文档状态：🟢 已批准并已创建子 issue（2026-10-19）** —— 7 个子 issue **[#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)–[#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52)** 已由 `github-operator` 按批准草稿创建（milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)，labels 空，正文含 `Part of #44`），#44 **已转为 epic 并保持 OPEN**。~~**但本文件仍不表示任何步骤已实施**：无源码/配置/测试改动、无 PR，S1–S7 的 Scope / AC 全部未执行。~~
> **实施进展（2026-10-20 回填；**下表取代“均未提交/未推送/未建 PR”的旧说法**）**：**S1 基线已测量并已合并**（PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)，merge `9696848`；证据 `docs/logs/2026-10-20-issue-46-baseline.md`；其后门槛已收紧为 0.90/90，新比较基线 = 后端 134 / 0.9165）；**S2 已实施并已合并**（PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)，merge `55f8d79`；`pom.xml` 一行：`mysql-connector-j` 8.3.0 → **9.7.0**；证据 `docs/logs/2026-10-20-issue-47-dependency-readiness.md`）；**S3 已实施并已合并**（PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56)，merge `70fb78b`；#48 已 **CLOSED**；分支 `codex/issue-48-core-boot3-jakarta`，18 文件：`spring-boot.version` **3.5.15** + `maven.compiler.release=17` + 11 个 `src/main/java` 与 3 个测试文件的 `javax.servlet`→`jakarta.servlet` + 守卫白名单集合相等 + `ci.yml` JDK 21 + `Dockerfile` 两行 `FROM`；JDK 21 下 **134 用例 / 0.9165 / major 61**，容器 smoke 全绿；**PR CI 双 job SUCCESS**；证据 `docs/logs/2026-10-20-issue-48-core-upgrade.md`）；**S4 已实施并已合并**（PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)，merge `9a7705c`；#49 已 **CLOSED**；CI 双 job **SUCCESS**；分支 `codex/issue-49-jdk21-toolchain`，基线 `70fb78b`；主 `<build>` 新增 enforcer `require-jdk-21` + `README.md` 新增 `## Java toolchain`；JDK 21 `validate` 通过、JDK 27 `validate` 按预期 BUILD FAILURE、`verify` **134 / 0.9165 / major 61**、容器 health 200；证据 `docs/logs/2026-10-20-issue-49-jdk21-toolchain.md`）；**S5 已实施并已合并**（PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)，merge `ff57e79`；#50 已 **CLOSED**；CI 双 job **SUCCESS**；分支 `codex/issue-50-mockito-annotation`，基线 `9a7705c`；9 文件 / **11 处** `@MockBean`→`@MockitoBean` + 9 import，`+20 −20`；JDK 21 `verify` **134 / 0.9165**；clean 构建 `MockBean` 弃用 **11 → 0**（整份日志弃用 **13 → 2**）；证据 `docs/logs/2026-10-20-issue-50-mockito-annotation-cleanup.md`）；**S6 已合并**（PR [#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59)，merge `b4a6d2a`；#51 已 **CLOSED**；CI 双 job SUCCESS；纯证据步骤，无源码/配置/测试改动；后端 **134 / 0.9165**、前端 **195 / 92.35% / 12 e2e**、生产 smoke 全绿；证据 `docs/logs/2026-10-21-issue-51-regression-proof.md`）；**S7 已实施（2026-10-21），待 PR 合并**（分支 `codex/issue-52-close-out`，基线 `b4a6d2a`；**仅 `docs/`**；日志 `docs/logs/2026-10-21-issue-52-close-out.md`）；#44 AC-6 的 JDBC 子句因 S2 升到 9.7.0 而**已触发**（证据归 S2，终态复核归 S6）；S3 的 **AC-6（PR 双 job CI）已取证 ✅**（见 §4-S3 第 6 条）。
> **创建与回填结果（2026-10-19，事实记录）**：① 7 份草稿已改名为 `docs/issues/issue-46-baseline.md` … `issue-52-close-out.md`，正文与远端**逐字一致**（仅 GitHub 追加一个尾部换行），`Internal tracking` 未发布；② #44 正文已追加 `## Steps`（7 个带链接的 checkbox + “每步一个 PR”）与 `## Step → acceptance mapping`，Non-goals 的 `@MockBean` 句与 Rollback 的 "single PR" 句**各改 1 行**，`Scope` / `AC-1..AC-11` 未改，标题/milestone/labels/state 未变；③ **JDK 21 已安装**：`/opt/homebrew/opt/openjdk@21`（`java version 21.0.12.1`），Maven 在该 `JAVA_HOME` 下**编译现有 Boot 2.7 代码成功**（仅编译，**未跑 `mvn verify`**，也未跑 npm/Docker/CI）。
> 本文只是**粒度细化提案**：把已批准的 Option A 迁移（`docs/plans/ai-option-a-boot3-migration.md`，2026-10-19 批准；issue [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)，milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)）拆成 7 个可独立实现、独立验证的小步骤。**它不改变已批准的范围、验收条件或决策**（见 §10），也**不表示任何步骤已实施**：7 个子 issue 已创建（#46–#52）、#44 已转 epic，但**无源码/配置/测试改动、无 PR**，S1–S7 全部未执行。**（2026-10-20 更新：S1、S2、S3 已分别随 PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)（merge `9696848`）、PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)（merge `55f8d79`）、PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56)（merge `70fb78b`，#48 CLOSED）合并；S4 已随 PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)（merge `9a7705c`，#49 CLOSED，CI 双 job SUCCESS）合并；S5 已随 PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)（merge `ff57e79`，#50 CLOSED，CI 双 job SUCCESS）合并；S6 已随 PR [#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59)（merge `b4a6d2a`，#51 CLOSED，CI 双 job SUCCESS）合并；S7 已实施（2026-10-21，仅 `docs/`，待 PR 合并）。见上文「实施进展」。）**
> **审批门禁**：① oracle 只读审查本文 + 7 份草稿 → ✅ **已完成（2026-10-19，`.pi/artifacts/oracle-issue-44-split-review.md`）**；② 主 agent 裁定建议 → ✅ **已完成，裁定「全部接受」**（B-1、P1-1..P1-8、P2-1..P2-8、C-1..C-4）；③ 用户授权创建 7 个 issue + 授权把 #44 转为 epic → ✅ **已完成（2026-10-19）**；④ `github-operator` 按草稿创建 / 编辑远端 → ✅ **已完成（2026-10-19，产出 #46–#52；#44 已转 epic）**。**但 7 个子 issue 的存在不等于实施许可**：每个 step 仍需各自走 oracle 预审 → 主 agent 批准 → 单一 worker → 3 reviewer 的流程；**任何未经主 agent 批准的实施、提交或远端变更仍然禁止**。**
> 起草日期：**2026-10-19**（仓库基线 `main` @ `70fcddb`，当前分支 `codex/ai-roadmap-planning`，对应已开出的 PR [#45](https://github.com/PNZ-z/ai-powered-Fabflix/pull/45)）。所有数字均**引用既有记录**（`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`、`docs/architecture/frontend-migration-contracts.md`）或本文件的**只读 grep 复核**；**本轮未运行 `mvn` / npm / Docker / CI**。
>
> **修订记录（2026-10-19，按 oracle 只读审查；主 agent 裁定「全部接受」）**
>
> - **B-1（步骤边界调整）**：`Dockerfile` 的两行 `FROM`（build `maven:3.9-eclipse-temurin-11` → `…-21`；runtime `eclipse-temurin:11-jre` → `eclipse-temurin:21-jre`）**并入 S3**。理由：S3 的 `release=17` 在 JDK 11 的 `javac` 上直接失败，且 JDK 11 JRE 无法加载 class file 61 → S3 合并后 `docker compose build app`（文档化的生产路径）立即损坏，而 CI 不构建 Docker 因此仍显示绿。**S4 相应缩窄**：只保留 enforcer `requireJavaVersion [21,22)`、README 的 JDK 21 说明，以及“两阶段镜像确为 JDK 21 + `docker compose build app` 成功”的**验证/取证**；标题改为 `[Step 4/7] Enforce JDK 21 in the build and document/verify it`。
> - **P1（全部采纳）**：**P1-1** epic Rollback 的 "single PR" → "**one pull request per step**"，并在 epic `## Steps` 写明每步一个 PR；**P1-2** 7 份子 issue 的**正文**（不是 Internal tracking）加显式 `Part of #44`；**P1-3** S3 所有命令必须在 `JAVA_HOME` 指向 **JDK 21** 的环境下运行（enforcer/README 在 S4 才到位），AC 证据写明所用 `JAVA_HOME`；**P1-4** S3 的“恰好 11 文件”AC 拆为 (a) 含 `jakarta.servlet` 的文件恰好 11 个 +(b) 任何**额外** `src/main/java` 改动逐条列在 PR 说明并说明升级必要性、不得改业务逻辑；**P1-5** S1 **不得安装 JDK**（若缺 JDK 21 → 记为 steps 3–4 的 blocking gap；安装需主 agent 批准）；**P1-6** S7 显式纳入契约文档 **§11**（容器两阶段镜像 = JDK 21）；**P1-7 / C-2** S5 作为 **epic 子 issue**，epic Non-goals 的 `@MockBean` 句改为澄清式表述；**P1-8** S3 保留 S2 决定的 `mysql-connector-j` 显式版本（不得被 Boot 3.5 BOM 覆盖）。
> - **P2-1..P2-8（全部采纳）**：S6 列出 10 条深链具体路径；S5 给弃用警告 grep 模式；S4 AC-2 用本机 JDK 27 取证“错误 JDK → BUILD FAILURE”；S1 AC-4 补跨 JDK 对比规则；§3 加“每步自有计划文档与完成日志”；S7 日志用 **S7 自己的 issue 编号**；§5.3 的 AC-6 行注明条件分支；S3 正文把 `jakarta.servlet-api` 断言写为“expected to stay absent”。
> - **C-1/C-2/C-3/C-4 均采纳 (a)**：`ci.yml` 的 `java-version: "21"` + job/step 改名并入 **S3**（S4 不再改 CI）；S5 为 epic 子 issue；S3 只跑 `npm ci && npm run build`（全量前端/e2e 归 S6）；epic 工期改为“**工作量 ≈ 1.5 周 / 日历 ≈ 5–7.5 周（含 7 轮审查）**”。
> - **未变**：§1.1 的原子性论证、§2 的顺序硬约束（除 S3/S4 边界外）、§5.3/§5.4 的验收映射、§7 的估算表均保持；本次修订**未运行**任何 `mvn` / npm / Docker / CI。
> - **创建结果（2026-10-19，本次回填新增）**：子 issue 编号 **S1→#46、S2→#47、S3→#48、S4→#49、S5→#50、S6→#51、S7→#52**（均 **OPEN**、milestone #3、labels 空）；#44 转为 **epic（OPEN）**。证据：`.pi/artifacts/github-operator-issue-44-steps.md`、`docs/issues/issue-44-boot3-java17-jakarta-migration.md`。本次回填**仍未运行**任何 `mvn` / npm / Docker / CI。

## 元数据

| 项目 | 值 |
| --- | --- |
| 文档类型 | 拆分/跟踪总览（issue #44 epic 的步骤规划）——**已批准并已创建子 issue（2026-10-19）** |
| 事实来源 | 已批准计划 `docs/plans/ai-option-a-boot3-migration.md`（§1.3/§4.3/§5/§6/§7.1）；路线图 `docs/plans/ai-roadmap-overview.md`（§6 M3、§12.3）；`docs/issues/issue-44-boot3-java17-jakarta-migration.md`（已发布的 Scope / Non-goals / AC-1..AC-11）；`docs/architecture/frontend-migration-contracts.md`（§7/§8/§9/§11/§12）；`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`（基线数字）；仓库只读核对：`pom.xml`、`.github/workflows/ci.yml`、`Dockerfile`、`src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java`、`grep`（2026-10-19） |
| 关联 GitHub issue | **[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) —— epic，state OPEN**（milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)，labels 空；正文 `## Steps` 列出 7 个子 issue）。**子 issue（均已创建；#46–#51 已随 PR #53/#55/#56/#57/#58/#59 合并并 CLOSED；仅 #52 仍 OPEN（S7 在 PR 中）；milestone #3、labels 空）**：S1 [#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)、S2 [#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)、S3 [#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)、S4 [#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49)、S5 [#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)、S6 [#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)、S7 [#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52) |
| 关联子 issue 文档 | `docs/issues/issue-46-baseline.md`、`issue-47-dependency-readiness.md`、`issue-48-core-boot3-jakarta.md`、`issue-49-jdk21-toolchain.md`、`issue-50-mockito-annotation-cleanup.md`、`issue-51-regression-proof.md`、`issue-52-close-out.md`（原名 `proposed-step-1..7-*.md`，issue 创建后按真实编号改名；每份均记录远端编号/URL/state 与“正文逐字一致”） |
| 估算 | 工作量合计 **≈ 6.5–7.5 人日**；日历口径（含每步 oracle 预审 + 3 路 reviewer + 修复 + PR/CI 循环）**≈ 5–7.5 周**（每周 >10 小时）。**C-4 已裁定 (a)**：epic 正文改写为“**工作量 ≈ 1.5 周 / 日历 ≈ 5–7.5 周（含 7 轮审查）**”，与已批准计划的“1.5–3 周”同口径。**注（2026-10-19 核对）**：远端 #44 正文的 `Estimate` 行**仍为“~1.5–3 weeks”，该改写未落远端**（不在 §5.1 的 6 项内），已记入 `docs/issues/issue-44-boot3-java17-jakarta-migration.md` 的“剩余待确认”第 9 项 |
| 状态 | **🟢 已批准并已创建子 issue（2026-10-19）**：B-1 + P1-1..P1-8 + P2-1..P2-8 + C-1..C-4 已落入本文件与 7 份 issue 文档；7 个子 issue（#46–#52）与 #44 的 epic 转换**均已执行并核验**。**剩余待办**：① ~~PR [#45](https://github.com/PNZ-z/ai-powered-Fabflix/pull/45) 的合并时机~~ → **已合并**（`main` 上 `fff314f88455216122912f07c26706f70dd63b62` = `Merge pull request #45`，见 `docs/logs/2026-10-20-issue-46-baseline.md` 元数据）；当前 `main` 基线为 `9eea6645…` = `Merge PR #54`（覆盖率门槛 0.90）；② ~~S1（#46）开工~~ → **S1 已合并**（PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)，merge `9696848`）；③ **S2（#47）已合并**（PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)，merge `55f8d79`）、**S3（#48）已合并**（PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56)，merge `70fb78b`，#48 CLOSED，CI 双 job SUCCESS），**S4（#49）已合并**（PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)，merge `9a7705c`，#49 CLOSED，CI 双 job SUCCESS；`pom.xml` enforcer + `README.md`），**S5（#50）已合并**（PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)，merge `ff57e79`，#50 CLOSED，CI 双 job SUCCESS），**S6（#51）已合并**（PR [#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59)，merge `b4a6d2a`，#51 CLOSED，CI 双 job SUCCESS）；**S7（#52）已实施（2026-10-21），待 PR 合并**（分支 `codex/issue-52-close-out`，仅 `docs/`）；④ **C-4 的 epic `Estimate` 行改写未落远端**（待主 agent 决定是否补改）。**已完成**：⑤ 安装 JDK 21；⑥ 创建子 issue；⑦ #44 转 epic |

---

## 1. 为什么这样拆

### 1.1 核心升级在编译层面是**原子**的

三个事实共同把"Boot 3 + Java 17 + Jakarta"锁成一个不可再分的最小单位：

1. **Boot 3 无法编译 `javax.servlet`**：`spring-boot-starter-web` 3.x 只提供 `jakarta.servlet-api`，`javax.servlet` 编译期即失败 → `pom.xml` 的 BOM 升级与 11 个文件的 import 替换**必须在同一个可编译提交里**。
2. **架构守卫测试主动禁止 `jakarta.servlet`**：`SpringBootOnlyArchitectureTest.java:39` 断言 `src/main/java` 中不得出现 `jakarta.servlet`，`pom.xml` 断言不得出现 `jakarta.servlet-api` → 守卫不先反转，迁移后 `mvn verify` **必然**红灯。守卫反转与 import 替换也是原子的（守卫若先合并而代码未改，main 立刻红）。
3. **字节码级别与工具链**：`maven.compiler.release=17` 只有在 Boot 3 的 BOM/插件对齐后才有意义（`spring-boot-maven-plugin` 版本由 `${spring-boot.version}` 属性驱动，自动跟随）。

因此 **S3 是"单一目的、不可再切"的核心步骤**（单一目的 = "让项目在 Boot 3.5 上编译并跑通现有测试"，不是"顺便清理"）。

### 1.2 可拆出去的四类工作

| 类别 | 为什么能独立 | 落在哪一步 |
| --- | --- | --- |
| **迁移前基线**（只读测量 + 记录） | 不改代码，只在 Boot 2.7 现状上复跑；它的价值是给 S6 的对比提供**当天**数字（#44 正文 Scope 的第 1 条就是这个） | **S1** |
| **非 Spring 依赖就绪**（驱动/jasypt） | 可以在 **Boot 2.7 + Java 11** 上单独升级并单独验证，**把 JDBC 行为差异从 Boot 3 行为差异里分离出来**（这是拆分最大的可调试性收益：出问题能二分定位） | **S2** |
| **环境固定 + 清理**（JDK 21 的机器强制、README、`@MockBean` 弃用） | 不改变编译单元，可在核心升级后逐个 PR 验证；**`Dockerfile` 的两行 `FROM` 不在此列**——它与 `release=17` 同属“必须能构建/能运行”的编译前提，已按 B-1 并入 S3 | **S4**（enforcer + README + 镜像取证）、**S5**（注解） |
| **回归证据 + 文档收尾** | 只产生证据与文档，不改行为 | **S6**、**S7** |

### 1.3 拆分的收益与代价（诚实列出）

**收益**

- 每次 PR 的 diff 单一目的、可 review：S3 的 diff 只含 `pom.xml` + 11 个 import 文件 + 守卫测试 + CI 的 JDK 版本 + `Dockerfile` 的两行 `FROM`（见 §6-C-1、B-1），审查者不必同时判断 “JDBC 变了吗 / 注解变了吗”。
- **可二分定位**：`mvn verify` 或 smoke 红时，先问"是驱动（S2）还是 Boot 3（S3）"——这个问题被结构性地回答了。
- 回退粒度：`git revert` 单步即可，不必整体回退 1.5–3 周的工作。
- 每步都有**独立的绿色终点**：7 个检查点，而不是"全部做完才知道行不行"。

**代价**

- 7 个 PR = 7 次 CI（后端 ≤15 min、前端 ≤20 min）+ 7 轮 3 路 reviewer 审查 → 日历成本上升（§7）。
- 如果拆分点选错，会出现“**中间态 main 红**”：本提案通过把 `ci.yml` 的 JDK 升版**和 `Dockerfile` 的两阶段镜像**一并并入 S3 来消除这一风险（§6-C-1、B-1）。只搬 CI 不搬镜像时，S3 合并后 `docker compose build app` 会失败，而 CI 因为不构建 Docker 仍显示绿——这是 oracle B-1 指出的 blocker。
- 文档/跟踪开销：7 份 issue + 7 份计划 + 1 份 epic 记录。

---

## 2. 依赖图与执行顺序

```text
S1 基线 (0.5d, docs/只读复跑)  ─┐
                                ├─→ S3 核心升级 (2–3d, Boot 3.5 + release 17 + jakarta + 守卫 + CI JDK 21 + Dockerfile 镜像)
S2 依赖就绪 (1d, pom 单点)     ─┘        │
                                        ├─→ S4 JDK 21 强制与取证 (1d: enforcer + README + 镜像验证)
                                        ├─→ S5 @MockBean 清理 (0.5d)
                                        │        │
                                        └────────┴─→ S6 回归与生产验证 (1d)
                                                          └─→ S7 收尾（文档/状态/日志）(0.5d)
```

- **S1 ∥ S2** 可并行（互不依赖），但两者都必须在 S3 开始前完成。
- **S4 ∥ S5** 可并行（都只依赖 S3，且互不触碰同一文件：S4 = `pom.xml` 主 `<build>` + `README.md`（以及只读核对镜像/跑 `docker compose build app`）；S5 = 9 个测试文件）。
- **S6 依赖 S3 + S4 + S5**（它要证明的是"最终形态"的全量门禁）。
- **S7 依赖 S6**（文档必须记录真实证据）。

**顺序硬约束（不可调换）**

| 约束 | 理由 |
| --- | --- |
| S1 在 S3 前 | S6/S3 的"数量不下降"判据需要**当天**的基线数字；引用旧记录（2026-10-15）只能作参考 |
| S2 在 S3 前 | 否则驱动升级与 Boot 3 升级混在一个 diff，JDBC 差异无法归因 |
| S3 的 CI 改动与 **`Dockerfile` 镜像改动**随 S3 合并（§6-C-1、B-1） | Boot 3.5 与 `release=17` 在 JDK 11 上无法构建（`javac --release 17` 不支持，JRE 11 也无法加载 class file 61）；若只搬 CI 不搬镜像，S3 合并后生产路径（`docker compose build app`）损坏而 CI 仍绿 |
| S4 在 S3 后 | `requireJavaVersion [21,22)` 在 JDK 11 的 CI 上会让构建失败；它只应在 CI 已是 21 之后生效 |
| S6 在 S3/S4/S5 后 | 生产 smoke 必须对"最终形态"（Boot 3 + JDK 21 镜像 + 无弃用注解）取证 |

---

## 3. 步骤总表

> "负责人角色"按 `AGENTS.md` 的工作流：`docs-writer`（计划/草稿/日志）→ `oracle`（只读风险关卡）→ 主 agent 裁定/批准 → **单一 `worker`** 实现 → 3 路 `reviewer` 并行只读审查 → 同一 `worker` 修复 + 跑测试 → `docs-writer` 回填 + 日志 → 主 agent 提交推送 → `github-operator` 建 PR。

| # | 标题（issue 前缀） | 目标（一句话） | 主要改动面 | 依赖 | 估算 | 验证要点 |
| --- | --- | --- | --- | --- | --- | --- |
| **S1**（[#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)） | `[Step 1/7] Record the pre-migration baseline and start the migration checklist` | 在**未改动**的 Boot 2.7 现状上复跑并记录迁移前基线，建 checklist 与回退点 | 仅 `docs/`（1 份基线记录 + 1 份 checklist）；**不改源码/pom/CI/Dockerfile** | 无 | **0.5 天** | 后端 `mvn verify`、前端 5 条命令、e2e 各跑一次并逐字记录输出。**状态（2026-10-20）：基线已测量**（JDK 27 与 JDK 21 均 `127` 用例 / `0.8980`；前端 `195` tests / `92.35%` / `12` e2e），见 `docs/logs/2026-10-20-issue-46-baseline.md` 与 `docs/plans/issue-44-migration-checklist.md`；**已合并（PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)，merge `9696848`）**。⚠️ **该组数字是 2026-10-20 门槛收紧（0.85/88 → 0.90/90）前的快照，S3/S6 不再以它作对照基线**，改用 checklist 的「2026-10-20 覆盖率门槛收紧（0.90 / 90）与新的比较基线」（后端 `134` / `0.9165`、前端 `195` / `92.35%`） |
| **S2**（[#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)） | `[Step 2/7] Validate non-Spring dependency readiness on the Boot 2.7 baseline` | 在 Boot 2.7 + Java 11 上单独定版并验证 `mysql-connector-j`（及 jasypt 的 JDK 21 检查），把 JDBC 差异与 Boot 3 差异分离 | `pom.xml`（1 个依赖版本）；可能 1 处配置文件 | 无（可与 S1 并行） | **1 天** | `mvn verify` 全绿 + 真 MySQL compose smoke 的日期/时区语义。**状态（2026-10-20）：已实施** —— 驱动 **8.3.0 → 9.7.0**（= 目标 Boot 3.5.15 BOM 的 `mysql.version`）；JDK 21 `mvn verify` **134 用例 / 0 failures / 0.9165**；真库 smoke（含 8.3.0 A/B 与 Java 11 运行时）通过；jasypt on JDK 21 通过。**已合并（PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)，merge `55f8d79`）**。证据：`docs/logs/2026-10-20-issue-47-dependency-readiness.md`、`docs/issues/issue-47-dependency-readiness.md`（AC-1..AC-5 回填）；**未验证项（读取映射/跨日/并发/TLS）顺延 S6** |
| **S3**（[#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)） | `[Step 3/7] Upgrade to Spring Boot 3.5 and migrate to the Jakarta namespace` | 唯一一次原子升级：BOM 3.5.x + `release=17` + 11 文件 jakarta + 守卫白名单 + CI JDK 21 + **两阶段镜像 JDK 21** | `pom.xml`、11 个 `src/main/java` 文件、`SpringBootOnlyArchitectureTest.java`、`.github/workflows/ci.yml`、**`Dockerfile`（两行 `FROM`，B-1）** | S1、S2 | **2–3 天** | `mvn verify`（**≥134 用例 / LINE ≥ 0.9165 不下降**（收紧后基线），JDK 21 环境下）、守卫集合相等断言 == 11、jar 形态、dist 缺失仍失败、`major version: 61`、PR CI 双 job 绿、**`docker compose build app` 成功 + 容器 health 200**。**状态（2026-10-20）：已完成（PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 已合并 @ `70fb78b`，#48 CLOSED）** —— 实测 Boot **3.5.15** / `release=17`（major **61**）/ **134 用例** / JaCoCo **0.9165** / `jakarta.servlet` 恰 **11** 文件 / 容器 smoke 全绿；**PR CI 双 job SUCCESS**（`Build and test (Java 21)` + `Frontend (…)`）；证据 `docs/logs/2026-10-20-issue-48-core-upgrade.md` |
| **S4**（[#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49)） | `[Step 4/7] Enforce JDK 21 in the build and document/verify it` | 用机器可执行的方式强制 JDK 21 并归档证据：enforcer `requireJavaVersion [21,22)` + README；**只验证不改镜像**（Dockerfile 已在 S3） | `pom.xml`（主 `<build>` 新增 enforcer 执行）、`README.md` | S3 | **1 天** | 错 JDK → BUILD FAILURE（可用本机 JDK 27 取证）；JDK 21 → 通过；两 stage 镜像为 21 + `docker compose build app` 成功（证据）；`-Pfrontend` 打包与 dist 守卫不回退。**状态（2026-10-20）：已完成（PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57) 已合并 @ `9a7705c`，#49 CLOSED）** —— 分支 `codex/issue-49-jdk21-toolchain`（基线 `70fb78b`），`pom.xml` +23 / `README.md` +17；JDK 21 `validate` PASS、JDK 27 `validate` **BUILD FAILURE**（预期）、`verify` **134 用例 / 0.9165 / major 61**、`-Pfrontend` 打包成功且 dist 移走后仍失败、容器 health **200** + JDK **21.0.12.1** + 非 root **1001**；**PR CI 双 job SUCCESS**（`Build and test (Java 21)` + `Frontend (…)`，run `39504525702`）；证据 `docs/logs/2026-10-20-issue-49-jdk21-toolchain.md` |
| **S5**（[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)） | `[Step 5/7] Replace deprecated @MockBean with @MockitoBean in the tests` | 消除 Boot 3.4+ 的 `@MockBean` 弃用警告（9 文件 / 11 处） | 9 个测试文件（仅 import + 注解） | S3 | **0.5 天** | `mvn verify` 全绿且用例数不变；弃用警告计数归零；无任何测试语义改动。**状态（2026-10-21）：已完成 —— PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) 已合并（merge `ff57e79`），CI 双 job SUCCESS，#50 已 CLOSED，AC-1..AC-5 全部 ✅** —— 分支 `codex/issue-50-mockito-annotation`（基线 `9a7705c`）；`@MockBean` **0** / `@MockitoBean` **11 处 / 9 文件**（+20 −20，仅 import + 注解）；JDK 21 `verify` **134 用例 / 0 failures / 0.9165**；`MockBean` 弃用（**clean** 构建）**11 → 0**（增量 `verify` 因 `Nothing to compile` 恒为 0，不可作 before 证据；整份日志弃用总数 **13 → 2**，剩 2 条属 `GoogleRecaptchaVerifier` → S6）；证据 `docs/logs/2026-10-20-issue-50-mockito-annotation-cleanup.md` |
| **S6**（[#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)） | `[Step 6/7] Prove the migration with full regression and production smoke evidence` | 对最终形态跑全量门禁 + HTTP 语义 + 顾客/员工流程 + 提交版 compose smoke，产出可核验证据 | 仅 `docs/`（证据记录）；**不改源码** | S3、S4、S5 | **1 天** | #44 的 AC-1..AC-7/AC-9/AC-11 逐条取证；环境不足则**显式标注未执行**。**状态（2026-10-21）：已执行，待 PR 合并** —— 分支 `codex/issue-51-regression-proof`（基线 `ff57e79`）；**无源码改动、PR diff 仅 `docs/`**；后端 134 / 0.9165 / `excludes` 8、前端 195 / 92.35% / 12 e2e、生产 smoke 全绿（10 深链 200 + 401/404/SPA fallback + `changeSessionId` + logout 204 + 日期双源一致）；证据 `docs/logs/2026-10-21-issue-51-regression-proof.md` |
| **S7**（[#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52)） | `[Step 7/7] Close out the migration: architecture docs, roadmap status and workflow log` | 收尾：架构约定 **§8/§12/§11**（+§7 陈旧数字）与 README、路线图状态、里程碑与 #44 关闭口径、工作流日志 | `docs/architecture/frontend-migration-contracts.md`、`README.md`、`docs/plans/ai-roadmap-overview.md`、`docs/logs/`、#44 记录 | S6 | **0.5 天** | 文档与实现的守卫/版本描述一致（含 **§11** 容器两阶段 = JDK 21）；路线图与里程碑状态与实际一致；日志含两类总结 |

> **S3 状态（2026-10-20 回填）：已完成 —— PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 已合并（merge `70fb78b`），#48 已 CLOSED，PR CI 双 job SUCCESS。** 分支 `codex/issue-48-core-boot3-jakarta`（基点 `55f8d79`），`git diff --stat` = **18 files / +85 −38**。实测：`spring-boot.version` **3.5.15**、`maven.compiler.release=17`（**major 61**）、`jakarta.servlet` 在 `src/main/java` **恰 11 文件**（`javax.servlet` = 0，`javax.sql`/`javax.xml` 4 文件未动）、`mvn clean verify` **134 用例 / 0 failures / 0 errors / `All coverage checks have been met.` / 988-90-1078 = 0.9165**（`excludes` 仍 8、阈值仍 0.90）、jar 含 `static/index.html`、移走 dist 后 `-Pfrontend package` BUILD FAILURE（默认 `verify` 仍 SUCCESS）、`ci.yml` job/step 名与 `java-version` 均为 21、`Dockerfile` 两行 `FROM` 为 JDK 21 且非 root 1001 保持、容器 health 200 + 10 条深链 200 + 顾客支付（`sales.id=13566`）与员工 metadata 全绿。**已取证**：PR 双 job CI（`Build and test (Java 21)` + `Frontend (lint, test, coverage, build, e2e)` 均 SUCCESS）。**仍顺延 S6**：HikariCP 池语义/并发、生产 reCAPTCHA、静态尾斜杠。证据 `docs/logs/2026-10-20-issue-48-core-upgrade.md`；AC 回填 `docs/issues/issue-48-core-boot3-jakarta.md`。

**合计工作量**：0.5 + 1 + (2–3) + 1 + 0.5 + 1 + 0.5 = **6.5–7.5 人日**（S1∥S2 可压缩 0.5–1 天）。

> **环境更新（2026-10-19，主 agent 确认）**：**JDK 21 已安装**于 `/opt/homebrew/opt/openjdk@21`（`java version 21.0.12.1`），且 Maven 在该 `JAVA_HOME` 下**编译现有 Boot 2.7 代码成功**（仅编译，**未跑 `mvn verify`**）。因此 S1 的“额外在 JDK 21 上复跑”从“可能不可用”转为**可执行**；按 S1 的跨 JDK 对比规则（P2-4），**建议 S1 基线在 JDK 21 下复跑**，使 S3/S6 的“数量不下降”与后续步骤在**同一 JDK** 上对比、避免 cross-JDK caveat；Boot 2.7.18 能否在 JDK 21 上跑完 `mvn verify` 仍**未实测**。

> **覆盖率门槛更新（2026-10-20，经用户批准）**：后端 JaCoCo `BUNDLE/LINE COVEREDRATIO` **0.85 → 0.90**、前端 `thresholds.lines` **88 → 90**，属**独立 PR**（分支 `codex/coverage-threshold-90`）的**迁移前置变更**，**不属于 S1–S7 任一步的范围**，也不改变各步的 Scope/AC，只把“不下降”的对照基准换为：后端 **134 tests / 988-90-1078 = 0.9165**、前端 **195 tests / Lines 92.35%**（详见 `docs/plans/issue-44-migration-checklist.md` 的「2026-10-20 覆盖率门槛收紧（0.90 / 90）与新的比较基线」与 `docs/logs/2026-10-20-coverage-threshold-90.md`）。S1 记录的 127 / 0.8980 降为历史快照，**不再作为 S3/S6 的对照基线**。收紧后后端只剩 **N ≤ 19 行**新增未覆盖代码的余量 → S3/S5 等步骤新增生产代码**必须配测试**。

> **每个 step 都是完整工作流**：按 `AGENTS.md` §2/§8，每个 step 建立**自己的** `docs/plans/issue-<n>-<slug>.md` 与完成日志 `docs/logs/YYYY-MM-DD-issue-<n>-<slug>.md`（可按需精简，但必须有）；`<n>` 用**该 step 自己的 issue 编号**，epic #44 在文档/日志中引用而非替代（P2-5/P2-6）。

---

## 4. 每步的"完成定义（DoD）"与"回退点"

### S1 — 基线与跟踪

- **DoD**：① 在**未改动**的工作区（`git status` 干净，只有无关的未被跟踪文件）记录 6 条命令的输出与关键数字；② 明确标注**运行时的 JDK 版本**（`mvn -version` 原文）——这是后续所有“不下降”判据的前提；③ 记录当前 HEAD commit；④ 建 checklist（S1–S7 复选 + 每步验证命令）；⑤ 声明 UI/视觉与真实浏览器刷新**不在自动化范围**（沿用契约文档 §7 边界）；⑥ 明确记录“若缺 JDK 21 → steps 3–4 的 **blocking gap**”，并给出**跨 JDK 对比规则**（P2-4）。
- **产出文件**：`docs/logs/YYYY-MM-DD-issue-<S1 编号>-baseline.md`（**用 S1 自己的 issue 编号**，P2-6）+ checklist（建议并入同一文件，避免新增一次性文档）。
- **回退点**：S1 只增文档 → 回退 = 删除文件，**不影响任何构建**。
- **JDK 21 不在此步安装（P1-5）**：若本机没有 JDK 21，**记录为 steps 3–4 的 blocking gap**；**安装 JDK 需主 agent 批准**，本步**不得安装**。
- **特殊风险（U-1）**：本机 Maven 现跑 **JDK 27**，而目标工具链是 **JDK 21**。基线与终验若在不同 JDK 上测，“数量变化”可能混入环境噪声 → S1 必须①记录 JDK；②**额外**在 JDK 21 上复跑一次 `mvn verify`（若 Boot 2.7 在 JDK 21 上可运行；需主 agent 已批准安装则先得批准）；③若 Boot 2.7 在 JDK 21 上**无法**运行（Spring Framework 5.3 的 ASM/CGLIB 对 class file 65 的支持未在本仓库实测），**如实记录“JDK 21 上的 Boot 2.7 基线不可得”**。
- **跨 JDK 对比规则（P2-4）**：若 JDK 21 基线不可得，S3/S6 的“不下降”必须**(a)** 与 S1 的同一 JDK（JDK 27）对比，并**记录 cross-JDK caveat**；或 **(b)** 在 JDK 21 上重新取一份基线并同时保留 S1 的 JDK 27 数字；**不得**用不同 JDK 的两个数字直接相减而不标注。**不得**因此修改源码或跳过验证。

### S2 — 非 Spring 依赖就绪

- **DoD**：① `mysql-connector-j` 的目标版本**定版并记录理由**（默认建议：取 Boot 3.5 BOM 管理的 9.x，使 S3 无需再动驱动；备选：若 9.x 在 Boot 2.7 基线上有阻塞问题，则保持 8.3.0 并把升级留给后续独立 issue，记录决策）；② 在 Boot 2.7 上 `mvn verify` 全绿、用例数与 S1 基线一致；③ **真 MySQL smoke**：`docker compose up -d mysql` 后跑一次读/写路径（下单或销售记录），确认**无时区偏移 / `CURDATE()` 偏差 / 新增致命 SSL 警告**（这是 #44 AC-6 的 JDBC 部分在"只有驱动变了"的条件下单独取证）；④ jasypt 在 JDK 21 下的检查：跑 `JasyptPasswordVerifier` 相关测试，确认无 `IllegalAccessError` / `InaccessibleObjectException`；**若 Boot 2.7 无法在 JDK 21 上跑**，把该检查**显式顺延到 S3** 并记录。
- **非目标**：不改 Boot 版本、不改 Java 版本、不改任何 Java 源码、不改 CI/Dockerfile、不引入新依赖、不替换 jasypt（替换 = 范围变化 → 回主 agent 裁定）。
- **回退点**：单个依赖版本号 → revert 一行；若驱动 9.x 在 smoke 中暴露行为差异，**保持 8.3.0 并记录**即为合规回退（不阻塞 S3）。
- **风险**：`mysql-connector-j` 9.x 与 Boot 2.7 的组合**未在本仓库实测**（计划 §4.3 / U-A2）；9.x 的最低 JDK 要求需实施时核实（本机 JDK 27 跑 Boot 2.7 基线意味着 JDK 11 目标下需确认驱动可用性）。若任一不成立 → 选备选方案（保持 8.3.0）。**实施结果（2026-10-20）：两项风险均未实现** —— 9.7.0 在 Boot 2.7.18 上 `mvn verify` 全绿（JDK 21），且在 **Java 11.0.31 运行时**完成真实结算写入；最低 JDK = **Java 8**（`Driver.class` major 52 + 上游 build 脚本/README）。因此**未走备选方案**。
- **与 #44 AC-6 的条件分支（P2-7）**：AC-6 的 JDBC 部分写作“**If the JDBC driver is upgraded to 9.x**”；若 S2 结论为“**保持 8.3.0**”，则该分支**不触发**，S6 只需记录“驱动未变，无 JDBC 差异可验”即可（不得因此把 AC-6 当作未完成）。**实际结论（2026-10-20）：S2 把驱动升到 9.7.0 → 该分支已触发**；其证据（真库 smoke 的日期一致性、日志告警面、员工 metadata）由 S2 承担，**终态（Boot 3.5 + JDK 21 镜像）的复核归 S6**。未验证项（驱动 `DATE`→Java 读取映射、跨日/TZ 边界、连接池/并发、TLS 实际状态）已在 S2 日志 §1.3 显式列出并顺延 S6。

### S3 — 核心升级（最大但有界）

- **DoD（一次性全部满足）**：
  1. `mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS，`Tests run` **≥ 134**（2026-10-20 收紧门槛后的基线；收紧前快照为 127）、`Failures/Errors = 0`，`All coverage checks have been met.`（JaCoCo `BUNDLE/LINE ≥ 0.90`，基线 **0.9165**），JaCoCo `excludes` **仍是 8 项**。
  2. `SpringBootOnlyArchitectureTest` 改为**白名单 + 集合相等 == 11 条路径**（`assertFalse(source.contains("jakarta.servlet"))` 不再存在；保留 `@WebServlet` / `extends HttpServlet` 禁令、`jar` + `repackage` 断言、`jakarta.servlet-api` 字面量不存在的断言、`jakarta.servlet.annotation.WebServlet` 禁令）。
  3. **Jakarta 迁移边界（P1-4）**：(a) 含 `jakarta.servlet` 的 `src/main/java` 文件**恰好 11 个**（守卫白名单集合相等，且 `git diff` 中这 11 个都在）；(b) 任何**额外**的 `src/main/java` 改动必须**逐条**列在 PR 说明并说明升级必要性，且**不得借机改动业务逻辑**。
  4. 打包形态不变：`cd frontend && npm ci && npm run build` → `mvn -Pfrontend -DskipTests clean package` → `unzip -l target/fablix.jar` 含 `BOOT-INF/classes/static/index.html`;移走 `frontend/dist` 后 `package` → BUILD FAILURE(`require-frontend-dist` 仍生效)。
  5. `javap -verbose -cp target/classes com.fabflix.boot.FablixApplication | grep 'major version'` → **61**；`mvn -version` 显示 **JDK 21**。**所有命令必须在 `JAVA_HOME` 指向 JDK 21 的环境下运行**（enforcer 与 README 说明在 S4 才到位，P1-3），AC 证据写明所用 `JAVA_HOME` 路径（本机默认 JDK 27 会直接导致本项不符）。
  6. PR 的 CI **两个 job 均绿**（后端 job 名已随 JDK 版本改名）。
  7. **容器内可构建/运行（B-1）**：`Dockerfile` 两阶段改为 JDK 21（build `maven:3.9-eclipse-temurin-21`、runtime `eclipse-temurin:21-jre`，保留 uid/gid 1001 与非 root `USER`、`EXPOSE 8081`），`docker compose build app` 成功，`docker compose up -d --no-deps app` 后 health 端点 **200**；Docker 不可用时**显式记“未执行 + 原因”**（不得假定通过）。
  8. **驱动版本不被 BOM 覆盖（P1-8）**：保留 S2 决定的 `mysql-connector-j` 显式版本（若 S2 定为 8.3.0 则继续显式 8.3.0；若定为 9.x 则显式 9.x），并记录到 PR 说明。
  9. 任何 **Boot 3 行为差异**(trailing-slash、错误响应、Tomcat 10、Jackson/Hikari/Logback 传递升级)**显式记录**在 PR 说明与 S3 计划文档中;**不得静默接受**。
- **非目标**：不改 `@MockBean`；不改 `compose.yaml` 等其他容器资产（`Dockerfile` **只**改两行 `FROM`，不加新层/不改 uid/gid 与非 root 块）；不加 enforcer `requireJavaVersion`；不改 README；不改前端代码；不改覆盖率阈值与 `excludes`；不做行为重构；**不顺手把依赖显式 pin 交回 BOM**（D-A5 未裁定 → 默认保持）；**不得移除/忽略 S2 已定的 `mysql-connector-j` 显式版本**（P1-8）。
- **回退点**：单 PR 整体 revert（回到 Boot 2.7 + javax，main 仍绿，因为 S1/S2 未改变行为）。**禁止**通过降阈值/扩 `excludes`/跳过 e2e 让 CI 变绿。
- **风险**：Boot 3 行为差异（计划 §7 评为中高，A4 是最大不确定性）；显式 pin 的 `mockito`/`byte-buddy`/`junit-jupiter` 在 Boot 3.5 BOM 下可能冲突（默认保持现行 pin，只有实测冲突才动，且记录）；**镜像风险（B-1）**：`maven:3.9-eclipse-temurin-21` / `eclipse-temurin:21-jre` 需要可拉取，首次构建可能因网络失败（环境问题，如实记录，不得改成“Docker 不支持”）；**容器 smoke 依赖 Docker 与 8081 端口**，不可用时记“未执行”。**开放问题 U-S3-1**：Boot 2.7 → 3.5 是否会因 HikariCP 4→6 / Jackson 2.13→2.19 改变 JSON 形状（AC-6 smoke 覆盖，但 S3 阶段可能只能靠单测 + 记录）。

**实际结果（2026-10-20，DoD 1–9 逐条回填；证据 `docs/logs/2026-10-20-issue-48-core-upgrade.md`）**

1. ✅ `mvn --batch-mode --no-transfer-progress clean verify`（JDK 21）→ **BUILD SUCCESS**；`Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`；`All coverage checks have been met.`；JaCoCo **988 covered / 90 missed / 1078 = 0.9165**（**分母未变** → 本步未增删被统计的生产代码）；`excludes` 仍 **8** 项。
2. ✅ 守卫改为白名单 + **集合相等（恰好 11 路径）**；旧 `assertFalse(source.contains("jakarta.servlet"))` 已移除；保留 `@WebServlet` / `extends HttpServlet` 禁令、`jar` + `repackage`、无 `jakarta.servlet-api` 字面量断言；新增 `jakarta.servlet.annotation.WebServlet` 禁令。**主 agent 终验后又补强 2 行**（源码禁 `javax.servlet`、pom 禁 `javax.servlet-api`），补强后仍 **2 个 `@Test`**、用例总数仍 134。
3. ✅ (a) `grep -rl jakarta.servlet src/main/java` = **恰 11 个**，与守卫白名单逐条一致，`git diff` 中 11 个文件都在；(b) **额外的 `src/main/java` 改动 = 0**，因此无业务逻辑改动可列；**另有 3 个 `src/test/java` 文件做了纯 import 替换**（`CartServiceTest:9`、`SpaFallbackTest:15`、`EmployeeAuthenticationFilterTest:4`），原因是 Boot 3 的 `spring-test` 只提供 `jakarta` 类型（不作此改动 `testCompile` 直接失败，worker 实测 10 个编译错误），**不在 AC-2(a) 的 `src/main/java` 边界内**。
4. ✅ `cd frontend && npm ci && npm run build` 成功；`mvn -Pfrontend -DskipTests clean package` 成功且 jar 含 `BOOT-INF/classes/static/index.html`（463 B）+ `static/assets/…`；移走 `frontend/dist` 后同一命令 **BUILD FAILURE**（`enforcer:3.5.0:enforce (require-frontend-dist)`）；另测：dist 缺失时**默认** `mvn verify`（无 `-Pfrontend`）仍 **BUILD SUCCESS** → 后端门禁不依赖 dist。
5. ✅ `major version: 61`（`javap`；并以字节头 `cafe babe 0000 003d` 独立复核）；全部命令在 `JAVA_HOME=/opt/homebrew/opt/openjdk@21`（`Java 21.0.12.1`）下运行。**注意**：本条 DoD 与 issue 正文写的类名 `FablixApplication` 是**笔误**，实际类为 `com.fabflix.boot.FabflixApplication`（照抄原文会得到「类找不到」）。
6. ✅ **已取证（2026-10-20 回填）**：PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 已合并（merge `70fb78b`），CI **双 job 均 SUCCESS** —— 后端 `Build and test (Java 21)`（job 名已随 JDK 升版改名）+ 前端 `Frontend (lint, test, coverage, build, e2e)`；#48 已 **CLOSED**。`ci.yml` 的 job 名 `Build and test (Java 21)`、step 名 `Set up Java 21`、`java-version: "21"` 与合并后的 `main` 一致（本轮只读复核）。
7. ✅ `Dockerfile` 两阶段 = `maven:3.9-eclipse-temurin-21` / `eclipse-temurin:21-jre`，`uid/gid 1001` 非 root、`EXPOSE 8081`、`ENTRYPOINT` 未动；`docker compose build app` 成功；容器内 `openjdk version "21.0.12.1"`、`id fabflix` = `uid=1001 gid=1001`；health **200**；10 条深链全 200 `text/html`、未认证 `/api/boot/cart` 401 JSON、`/API/boot/cart` 与 `/assets/missing.js` 404、顾客登录→加购→token→**支付成功**（`sales.id=13566`，`order_date` = `sale_date` = 2026-10-20）、员工登录→dashboard session 200→metadata 200。
8. ✅ `mysql-connector-j` 显式 **9.7.0** 保留且实际解析为该版本（`dependency:list` + jar 内 `mysql-connector-j-9.7.0.jar`）；**Boot 3.5.15 BOM 的 `mysql.version` 同为 9.7.0**，因此不存在被 BOM 改版的风险。
9. ✅ 行为差异已**全部记录**在 `docs/logs/2026-10-20-issue-48-core-upgrade.md` §1.3 并将在 PR 说明中重述。要点：**`/api/boot/health/` 的 401 不是 Boot 3 差异**（`CustomerAuthenticationFilter` 精确匹配 + filter 先于 MVC；**升级前基点树 `55f8d79` 实测同样 401**，worker 原始归因已更正）；Boot 3 的实际影响是 **Tomcat 10.1.55 / Jackson 2.21.4 / HikariCP 6.3.3 / Logback 1.5.34** 的 BOM 传递升级（未观察到 JSON 形状与状态码差异，「未观察」只限已测范围）；**新增弃用**：`GoogleRecaptchaVerifier` 的 `RestTemplateBuilder.setConnectTimeout/setReadTimeout`（`@Profile("!local")`、0/17 覆盖、本步未运行生产 reCAPTCHA）→ S6 风险/可选修复；`@MockBean` 11 条弃用警告 → S5；池语义/并发、静态尾斜杠、驱动 DATE 读取映射 → S6。

**未走备选方案 / 未偏离批准范围**：无新增依赖、无新增插件、无 enforcer 主 `<build>` 规则（S4）、无 README 改动（S4）、无 `compose.yaml` 改动、无前端源码改动、`excludes` 与阈值未动、`mysql-connector-j` pin 未动。

### S4 — JDK 21 强制（机器可执行）与镜像取证

- **DoD**：① `pom.xml` **主 `<build>`** 中新增 `maven-enforcer-plugin`（与既有 `require-frontend-dist` 同一插件坐标）的 `requireJavaVersion [21,22)` 规则，`fail=true` + 可读 message；**注意现状**：`maven-enforcer-plugin` 目前**只存在于 `frontend` profile 内**（`pom.xml:178-186` profile、`:181` 插件），因此本步必须把它加入主 `<build>`（不是“复制一份到 profile”），并确认 profile 内的 `require-frontend-dist` 执行仍生效。② `README.md` 写明 JDK 21 要求 + `JAVA_HOME` 说明 + 构建/运行命令。③ **镜像只验证不改（B-1）**：确认 `Dockerfile` 两阶段已为 JDK 21（build `maven:3.9-eclipse-temurin-21`、runtime `eclipse-temurin:21-jre`，保留非 root uid/gid 1001）——这是 **S3 的产物**，本步只取证并在 PR 里给出 `docker compose build app` 成功的证据；若发现镜像仍为 JDK 11 → **停下回主 agent**（说明 S3 未完成），不在本步补改。④ 证据：JDK 21 下 build 通过；**错误 JDK 下 BUILD FAILURE（可用本机 JDK 27 直接取证，P2-3）**；`mvn -Pfrontend -DskipTests package` 与“移走 dist 仍失败”两项不回退。
- **非目标**：**不改 `Dockerfile`**（已在 S3，B-1）；不改 `ci.yml`（已在 S3）；不改业务代码；不改 `compose.yaml`；不升 Spring Boot 补丁版本。
- **回退点**：revert 本 PR 一步（enforcer 与 README 相互独立，可只回退 enforcer）。**注意**：`requireJavaVersion` 会让 JDK 27 的本地构建**直接失败**——这是**预期行为**（计划 P1-3），README 必须写清，否则会被当成 bug。
- **风险**：若 enforcer 版本/规则与既有 `require-frontend-dist` 合并方式冲突（同一 plugin 的两个 execution），可能使 `-Pfrontend` 打包异常 → DoD ④ 显式覆盖此项；若 S3 的镜像未合并（S3 被回退），本步的镜像取证必然失败——应作为**依赖未满足**上报，而不是在本步重改 Dockerfile。

**实际结果（2026-10-20，DoD ①–④ 逐条回填；证据 `docs/logs/2026-10-20-issue-49-jdk21-toolchain.md`）**

1. ✅ `pom.xml` 主 `<build>` 新增 `maven-enforcer-plugin:3.5.0` execution **`require-jdk-21`**（`phase=validate`、`fail=true`、`requireJavaVersion [21,22)`，消息指回 README）；profile 内 `require-frontend-dist` **原样保留**，`-Pfrontend` 下**两条 execution 都执行**（实测日志逐条可见），未出现合并冲突。
2. ✅ `README.md` 新增 `## Java toolchain`：**构建（所有 `mvn` 命令）需 JDK 21** + `JAVA_HOME` 指向 JDK 21、自检 `mvn -version` 报 **`21.x`** + `export JAVA_HOME=/path/to/jdk-21`、**IDE 内嵌 Maven 也需设 21**、`release=17` → major 61 → **jar 可在 Java 17+ 运行**、CI 与 `Dockerfile` **both JDK stages** 为 21、Compose 在镜像内运行而不依赖宿主 JDK。
3. ✅ **镜像只验证不改（B-1 成立）**：`Dockerfile` 仍为 `maven:3.9-eclipse-temurin-21` / `eclipse-temurin:21-jre`，`uid/gid 1001` 非 root、`EXPOSE 8081`、`ENTRYPOINT` 未动；`docker compose build app` 成功 → health **200**、容器内 `java -version` = **21.0.12.1**、`id` = **uid/gid 1001**；随后 `rm -sf app`，**未 `down` mysql**。`Dockerfile` / `ci.yml` 零改动。
4. ✅ JDK 21 `mvn validate` → `Rule 0 … RequireJavaVersion passed` + **BUILD SUCCESS**；**JDK 27**（`/opt/homebrew/opt/openjdk`）同命令 → **BUILD FAILURE**（预期，消息含 “pins the build … verified on 21 only … see README”）；`mvn -Pfrontend -DskipTests clean package` 成功且 jar 含 **6 个 `static` 条目**，移走 `frontend/dist` 后同命令 **BUILD FAILURE**（`require-frontend-dist`）随后恢复；`excludes`/阈值/`mysql-connector-j` pin/`release=17` 均未动。
5. ✅ 门禁不回归：JDK 21 `mvn verify` → BUILD SUCCESS，**134 用例 / 0 failures / 0 errors / 0 skipped**、`All coverage checks have been met.`、JaCoCo **988/1078 = 0.9165**、字节码 **major 61**。
6. 三路 review **无 Blocker**；采纳 **5 项文案**并已修正（enforcer 对 22–24 的歧义、README 首句过宽、`21` → `21.x`、补 `JAVA_HOME`/IDE 提示、`both stages` → `both JDK stages`）。

**PR 与 CI（2026-10-20 回填，已闭合）**：PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)《Enforce JDK 21 for the build and document it》**已合并**（merge commit `9a7705ca2bb9f5b4dc0fb1a20a4413a0c46aa0b6` = 短 SHA `9a7705c`），CI run `39504525702` 两个 job 均 **SUCCESS** —— `Build and test (Java 21)`（51s）+ `Frontend (lint, test, coverage, build, e2e)`（2m38s）；远端 **#49 已 CLOSED**（本轮 `gh pr view/checks`、`gh issue view 49` 只读核验）。

**未取证 / 未偏离**：~~本步的 PR 与其 CI（尚无 PR）~~ → **已闭合（见上）**；JDK 17 未单独复跑（规则语义已由 JDK 27 取证）；前端 lint/test/e2e 本步未跑（由 PR CI 的前端 job 覆盖）。**未偏离批准范围**：无源码/测试改动、无 `Dockerfile`/`ci.yml`/`compose.yaml` 改动、无阈值/excludes/pin 改动；后续事项（`frontend/README.md` JDK 21 说明 → S7；`SpringBootOnlyArchitectureTest` 可加 enforcer 守卫 → S6/S7；enforcer 版本字面量两处重复 → 可选清理）已在日志登记。

### S5 — `@MockBean` 清理

- **DoD**：9 个测试文件、11 处 `@MockBean` → `@MockitoBean`（import 由 `org.springframework.boot.test.mock.mockito.MockBean` 改为 `org.springframework.test.context.bean.override.mockito.MockitoBean`）；`mvn verify` 全绿且用例数/覆盖率不变；迁移前后的**弃用警告计数**对比记录（`mvn verify 2>&1 | grep -ci deprecat`，只作证据，不作闸门）；`git diff` 只含 import + 注解行。
- **非目标**：不改任何断言/测试逻辑/被测代码；不引入新测试依赖。
- **回退点**：单 PR revert。
- **⚠️ 边界已裁定（C-2 = (a)，P1-7）**：#44 正文 Non-goals 原本写死“本 issue 不做 `@MockBean`→`@MockitoBean`”。主 agent 采纳 oracle 建议 **(a)**：S5 作为 **epic 子 issue**（正文含 `Part of #44`），并同步把 epic Non-goals 改为澄清句："The core upgrade step (S3) does not migrate `@MockBean`; step 5 (S5) does it as a follow-up step."（属对已批准契约的澄清，不缩减范围；**远端编辑已于 2026-10-19 生效**，见 §5.1）。

**实际结果（2026-10-20，DoD 逐条回填；证据 `docs/logs/2026-10-20-issue-50-mockito-annotation-cleanup.md`，AC 回填 `docs/issues/issue-50-mockito-annotation-cleanup.md`）**

1. ✅ **9 文件 / 11 处** `@MockBean` → `@MockitoBean`，import 统一为 `org.springframework.test.context.bean.override.mockito.MockitoBean`：`grep -rn "@MockBean" src/test/java` = **0**，`@MockitoBean` = **11 处 / 9 文件**（计数与预期完全一致，无需调整）；`@SpyBean`/`@MockitoSpyBean` = **0**。
2. ✅ `mvn --batch-mode --no-transfer-progress verify`（JDK 21）→ **BUILD SUCCESS**；**134 tests / 0 failures / 0 errors / 0 skipped**；`All coverage checks have been met.`；JaCoCo **988 / 90 / 1078 = 0.9165**（与改动前逐位相同，阈值 0.90、`excludes` 8 未动）。
3. ✅ **弃用计数（口径已纠正）**：DoD 写的 `mvn verify 2>&1 | grep -ci deprecat` 在**增量构建**下 before/after **都是 0**（日志含 `Nothing to compile - all classes are up to date.`，javac 未重编译 → 无警告），**不能**充当 before 证据；改以 **`mvn clean verify`** 取证：**before = 11**（恰为 11 个 `@MockBean` 站点）、**after = 0**；整份日志弃用总数 **13 → 2**（差值 11），剩余 2 条是 `GoogleRecaptchaVerifier` 的 `RestTemplateBuilder.setConnectTimeout`/`setReadTimeout`（S3 已登记、属 **S6**）。**AC-1 才是闸门**，计数仅作证据。
4. ✅ `git diff -U0` 逐 hunk 仅 import（9 组）与注解（11 处）行；无测试体/断言/fixture/`.session(...)` 变化；无 `@SuppressWarnings`、无编译参数改动。注意 7 个文件的 import 是**位移**（不在同一行位置），故 `+20/−20` 不等同「同一位置改 20 行」。
5. ✅ **语义等价**：11 个 mock 目标 = 10 个 `@Service` 类 + 1 个接口（`RecaptchaVerifier`），**全部不在 `@WebMvcTest` 切片内** → 两注解在此路径等价；**无任何用例**因切换失败，故**未触发**「保留 `@MockBean` 并上报」的回退口径。
6. ✅ **context cache key 风险已按 spring-test 6.2.19 字节码核实**（不是转述）：`@MockitoBean` 的 override 通过 `BeanOverrideContextCustomizer` **参与** cache key，粒度为 **handler 形状**（bean 类型、字段名、`BeanOverrideStrategy`、qualifier 注解；不含声明测试类）→ mock 形状相同的测试类可共享 context、mock 集不同则不共享；本仓库 10 个 `@WebMvcTest` 各自钉住不同 controller（另有 4 个 `standaloneSetup` 无 context 缓存），**当前无共享**。结论已写入 `docs/architecture/frontend-migration-contracts.md` **§7**。
7. ✅ **PR 与 CI（2026-10-21 闭合）**：PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) **已合并**（merge `ff57e79`），CI 双 job **SUCCESS**，[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50) 已 **CLOSED**，AC-1..AC-5 全部 ✅。**仍成立的两点**：「构建零弃用」**不成立**（clean 构建仍剩 **2** 条范围外弃用 `GoogleRecaptchaVerifier.setConnectTimeout/setReadTimeout`，已在 S6 证据记录登记）；`@MockitoSpyBean` / `@MockitoBean(name=…)` 路径未使用、未验证。**未偏离批准范围**：无 `src/main/**`、无 `pom.xml`、无 `ci.yml`/`Dockerfile`/`compose.yaml`、无 `frontend/**`、无阈值/`excludes`/pin/`release` 改动。

### S6 — 回归与生产验证

- **DoD**：逐条对 #44 的 AC 取证，**每条都写"命令 + 输出摘要 + 是否执行"**：
  - AC-1/AC-2：`mvn --batch-mode --no-transfer-progress verify`（数字与 S1 基线对比；若 S1 的 JDK 21 基线不可得，按 **S1 的跨 JDK 对比规则**标注 cross-JDK caveat，P2-4）；
  - AC-5：`npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e`（195 用例 / Lines ≥90（2026-10-20 收紧后；收紧前为 ≥88）/ 12 次 e2e）；
  - AC-6：`docker compose build app` + `docker compose up -d --no-deps app`（8081）→ health 200；`/` 含 `id="root"`；**10 条深链**均 200 `text/html`（P2-1）：`/home`、`/movies`、`/movies/:id`、`/cart`、`/checkout`、`/confirmation`、`/login`、`/employee/login`、`/employee/dashboard`、`/playground`；未认证 `/api/boot/cart` → 401 JSON；`/API/boot/cart` → 404 JSON;`/assets/missing.js` → 404;顾客登录 → session/cart 200;员工登录 → dashboard `status:success`;若驱动为 9.x 另验日期/`CURDATE()`/SSL 警告;
  - AC-7：`changeSessionId()`、401 body 形状、员工登出 204 且保留 `user`；
  - AC-9：`excludes` 仍 8 项；AC-11：`mvn -version` + 字节码 61 + 两个 Docker stage 为 21（CI 与 enforcer 由 S3/S4 取证）。
  - **环境不可用的部分（Docker/端口 8081/真实 MySQL）必须写"未执行 + 原因"，不得声称通过。**
- **非目标**：不改源码（若发现缺陷 → **停下，回主 agent**，由 S3/S4/S5 的 owner 修，不在 S6 里顺手改）。
- **回退点**：只增证据文档 → 回退 = 删除。
- **风险**：生产 smoke 需要 Docker + 8081 端口空闲（计划 AC-6 已注明）；若不可用，S6 只能给"未执行"，而 **#44 不得因此关闭**（这一点直接影响 S7）。

**实际结果（2026-10-21，DoD 逐条回填；证据 `docs/logs/2026-10-21-issue-51-regression-proof.md`，AC 回填 `docs/issues/issue-51-regression-proof.md`）**

> 执行基线：**`ff57e79`**（已合并 S5 的 `main`）；分支 `codex/issue-51-regression-proof`；**未提交 / 未推送 / 无 PR**；**无源码/配置/测试改动**，PR 的 diff **只含 `docs/`**。

1. ✅ **AC-1 / AC-2（后端）**：`mvn --batch-mode --no-transfer-progress verify`（JDK 21）→ **BUILD SUCCESS**；**`Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`**（= 收紧后基线 134，不低于）；`All coverage checks have been met.`；JaCoCo **988 / 1078 = 0.9165**（门槛 **0.90**，与基线逐位相同）。对照基准取 checklist「2026-10-20 覆盖率门槛收紧」表，**不**再对照 S1 的 127 / 0.8980；本次两侧同 JDK 21 → **无 cross-JDK caveat**（P2-4 不触发）。
2. ✅ **AC-5（前端五条命令 + e2e）**：`npm ci` → `npm run lint`（干净）→ `npm test` **30 files / 195 tests** → `npm run test:coverage` **Lines 92.35%（846/916，门槛 90）** → `npm run build` 成功 → `npm run test:e2e` **12 passed**（均 = 基线）。前端零改动：`git diff fff314f -- frontend/src` = **0 行**。
3. ✅ **AC-6（生产 compose smoke）**：提交版 `compose.yaml` + `docker compose build app` / `up -d --no-deps app`（**8081**，容器内 **JDK 21**、非 root **1001**）→ health **200**；`/` 含 **`id="root"`**；**10 条深链全部 200 `text/html`**：`/home`、`/movies`、`/movies/tt9000001`（真实 id）、`/cart`、`/checkout`、`/confirmation`、`/login`、`/employee/login`、`/employee/dashboard`、`/playground`；未认证 `/api/boot/cart` → **401** + `{"errorMessage":"Authentication required."}`；`/API/boot/cart` → **404** `application/json`；`/assets/missing.js` → **404**；不存在的 SPA 路由 `/no-such-page` → `index.html`（含 `id="root"`）；顾客登录 → session/customer/cart **200**，加购 → checkout token → **支付成功**；员工登录 → dashboard session **200** → metadata **200**。收尾 `docker compose rm -sf app`，MySQL 与数据卷保留。
4. ✅ **AC-6 的 JDBC 子句（条件分支已触发，驱动 9.7.0）**：支付响应 `order_date` = **2026-10-22**，库内 `sales.id=**13567**`、`sale_date` = **2026-10-22** → **双源一致，无时区偏移、无 `CURDATE()` 漂移**；容器日志 `ssl|fatal|severe|exception` 匹配 **0**。**时区口径**：主机 **2026-10-21 PDT**、容器 JVM 与 MySQL **UTC 2026-10-22**（同一时区两条路径一致，**不是**漂移）。**跨时区 / 跨日边界仍未构造**（既有边界，S2 已记录，本次同样不覆盖）；`sales` 行 13562–13567 为 **dev 数据**。
5. ✅ **AC-7（认证 / 会话语义）**：顾客连续两次登录 → **`JSESSIONID` 变化**（`changeSessionId()` 在 Boot 3 + Tomcat 10 下生效）；401 body 形状为单一 `errorMessage` 字段（见第 3 条）；员工 `GET /api/boot/dashboard/logout` → **204**，且登出后顾客 session 仍 **200**、`user` 属性**保留**（单测 + smoke 双证）。
6. ✅ **AC-9 / AC-11（工具链与制品）**：`<exclude>` = **8**（与基线一致，本步未动 `pom.xml`）；`mvn -version` → **Java 21.0.12.1**；字节码 **major version: 61**；`Dockerfile` 三阶段 = `node:22-alpine` / `maven:3.9-eclipse-temurin-21` / `eclipse-temurin:21-jre`；enforcer `require-jdk-21`（`requireJavaVersion [21,22)`、`validate`、`fail=true`）生效。**口径更正**：`grep -c requireJavaVersion pom.xml` = 2 是**开/闭标签两行**（= 1 个规则元素），**不是**「主 build + profile 各一处」；`frontend` profile 内为另一条 `require-frontend-dist`。
7. **未执行（显式登记，均"未执行 + 原因"，不得读作通过）**：① 真实浏览器多标签 / 并发（无相应场景，既有边界）；② 视觉 / 对比度（jsdom 无布局）；③ 真实 MySQL 并发 / 连接池语义（本次为顺序单用户请求）；④ 生产 reCAPTCHA（`GoogleRecaptchaVerifier` 0/17、`@Profile("!local")`、无真实凭据；且 Boot 3 弃用 `setConnectTimeout/setReadTimeout` —— 这 2 条弃用是构建日志里剩余的全部弃用，「零弃用」不成立）；⑤ 员工 `?action=add_movie` 存储过程真实往返（smoke 只触达 session/metadata；`AdminRepository` 仍 0/39）；⑥ 跨时区 / 跨日边界（见第 4 条）。
8. ✅ **无缺陷、无偏离**：本次未发现迁移后行为差异，因此**未**触发「发现缺陷 → 停下回主 agent」的路径；未改源码/测试/配置/CI/`Dockerfile`/`compose.yaml`/`frontend/**`，未新增测试、未降阈值、未扩 `excludes`、未跳过 e2e。**未合并**：无 PR，#51 仍 OPEN，PR 的 CI 结果与 #51 关闭均未发生。**→ 已闭合（后续动作，2026-10-21）**：PR [#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59) 已合并（merge `b4a6d2a`），CI 双 job **SUCCESS**，[#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51) 已 **CLOSED**。

### S7 — 收尾

> **前置状态（2026-10-21）**：**S6 证据已产出**（`docs/logs/2026-10-21-issue-51-regression-proof.md`），**S7 只做文档收尾**，不重跑 S6 的命令；#44 关闭时必须引用 S6 证据记录 §0.7 的「未执行清单」，不得写「迁移完成，全部验证通过」。

- **DoD**：① `docs/architecture/frontend-migration-contracts.md` **§8**（守卫从“禁止 `jakarta.servlet`”改为“11 文件白名单 + 集合相等断言”）、**§12**（CI/镜像 JDK 21 约定）、**§11**（容器两阶段镜像 = JDK 21，P1-6 **必须显式列入，不依赖 AC-3 的 drift 检查**）、以及 **§7 的陈旧覆盖率余量**（现写 "~106 行" → 正确表述“现有 110 行未覆盖，最多还能新增 ≈60 行未覆盖”）；② `README.md` 与实现一致的构建/JDK 说明（若 S4 已写完，本步只核对）；③ `docs/plans/ai-roadmap-overview.md` §12.3 与 M3 状态回填；④ #44（epic）关闭或按 §5.1 的口径处理，milestone #3 状态核对（**GitHub 写操作由主 agent / `github-operator` 执行**）；⑤ 工作流日志 `docs/logs/YYYY-MM-DD-issue-<S7 的 issue 编号>-close-out.md`（**用 S7 自己的编号，不用 #44；epic 在日志中引用**，P2-6），含**“修改了哪些文件及潜在问题”**与**“新增知识点 / 重点学习对象”**两大分区。
- **非目标**：不改任何源码/测试/配置；不改历史日志与已关闭 issue 的计划/issue 文档（#44 正文已写死）。
- **回退点**：文档回退 = revert。
- **风险**：若 S6 有"未执行"项，#44 的关闭必须**显式记录该缺口**，不得写"迁移完成，全部验证通过"。

**实际结果（2026-10-21，DoD 逐条回填；证据 `docs/logs/2026-10-21-issue-52-close-out.md`，AC 回填 `docs/issues/issue-52-close-out.md`）**

> 执行基线：**`b4a6d2a`**（已合并 S6 的 `main`）；分支 `codex/issue-52-close-out`；**未提交 / 未推送 / 无 PR**；**无源码/配置/测试改动**，PR 的 diff = `docs/` **7 个文件** + `worker` 的 `README.md`/`frontend/README.md` 4 处文本。

1. ✅ **契约文档**：§8 重写为「`jakarta.servlet` = **11 路径 allow-list + 集合相等**，`javax.servlet`/`@WebServlet`/`extends HttpServlet` 仍禁；`pom.xml` 断言含 `jar`+`repackage` 且**禁 `jakarta.servlet-api` 与 `javax.servlet-api` 两个字面量**」；§11 两个 JDK 阶段 → **21**（非 root 1001、`mysql:3306`、`SPRING_PROFILES_ACTIVE=local`、SPA fallback 约定不变）；§12 新增「本地/CI/镜像统一 JDK 21、`release=17`、major 61」；§7 复核后确认**无** "~106 行" 陈旧数字（口径更正为 **90 行未覆盖 / 最多再新增 ≈19 行**，**不是** DoD ① 里旧的「110 行 / ≈60 行」——那是 **0.85 门槛时代** 的写法，已于 2026-10-20 门槛收紧时失效）。
2. ✅ **README**：S4 已写入的 `## Java toolchain`（JDK 21 + `JAVA_HOME` + enforcer `[21,22)` + `release=17`/major 61/CI+镜像 JDK 21）与实现一致；**另发现 4 处需对齐的表述，由 `worker` 落地**（`docs-writer` 不编辑 `docs/` 之外的文件）：`README.md:3` 写明 **`Spring Boot 3.5`**/**`React 19`**；`README.md:159` 补前端 **90% lines** 覆盖率闸门；`frontend/README.md:51` 阈值措辞改 **90% `lines`**；`frontend/README.md:75` 新增 `mvn -Pfrontend` 需 **JDK 21** + `JAVA_HOME`（= checklist S4 遗留项闭合）。复核：`grep -n "Java 11\|temurin-11\|Boot 2\|2.7.18\|JDK 11" README.md frontend/README.md` → **无命中**。依据 issue Scope：「step 4 already wrote the JDK paragraph — this step checks it against the code and fixes drift only」。
3. ✅ **路线图**：`docs/plans/ai-roadmap-overview.md` 顶部新增「执行状态（2026-10-21）」行，并回填元数据表的「状态/执行状态/GitHub」行、§2.6、§6（M3）、§12.3（A-3/A-4）；**S1–S6 已合并（PR #53–#59 中 6 个）、S7 收尾中、B/B-API/C 未开始**；历史证据与已批准决策**未改写**。
4. ✅ **#44 / milestone 口径**：#44 记录补「S1–S6 已合并、S7 在 PR」，**未声称 #44 已关闭**；**未做任何 GitHub 写操作**（epic #44、milestone #3、#52 均仍 OPEN）。
5. ✅ **工作流日志**：**`docs/logs/2026-10-21-issue-52-close-out.md`**，用 **S7 自己的编号（#52）**，含「修改了哪些文件及潜在问题」与「新增知识点 / 重点学习对象」两大分区。
6. ✅ **机械核对**（均可重跑）：`grep -rl "jakarta.servlet" src/main/java \| wc -l` = **11**；白名单 `Set.of(...)` 条目 = **11**；`grep -n "servlet" pom.xml` = **0**；`grep -n "^FROM" Dockerfile` → `node:22-alpine` / `maven:3.9-eclipse-temurin-21` / `eclipse-temurin:21-jre`；`grep -n "java-version" .github/workflows/ci.yml` → `"21"`（job name `Build and test (Java 21)`）；`grep -n "maven.compiler.release" pom.xml` → **17**。
7. **未执行（显式登记）**：**未重跑** S6 的任何命令（本步不重验）；**未运行** `mvn` / npm / Docker / CI（因此**不得**声称本轮构建绿）；**未**创建/编辑/关闭任何远端 issue 或 PR。
8. ✅ **无范围偏离**：未改源码/测试/配置/CI/`Dockerfile`/`compose.yaml`/`frontend/src`；未降阈值、未扩 `excludes`。改动 = `docs/` **7 个文件** + `worker` 的 `README.md`/`frontend/README.md` 4 处文本（在 AC-6 允许范围内）。**未合并**：#52 仍 OPEN，PR 与 CI 结果尚未发生。

---

## 5. 与 #44 的关系：推荐把它转为 epic/tracking issue

### 5.1 推荐方案（A）：#44 = epic，#44 正文追加步骤清单，子 issue 用 `Part of #44` 关联

**理由**

1. **#44 已经是完整的范围与验收契约**（Scope 12 条、Non-goals 11 条、AC-1..AC-11）。关闭它再建 7 个小 issue，等于把这份已批准契约拆散重写 → 失去"一处可读的总验收"，且容易在重写中漂移（计划/评审成本也更高）。
2. **milestone #3 的进度才有意义**：7 个子 issue 关闭 = 里程碑可验证地推进；单一 epic 无法反映进度。
3. **保留审计链**：立项（2026-10-19）→ 拆分（本文件）→ 7 步实施 → 关闭，每条 GitHub 记录都指向同一个 epic。
4. **"立项 ≠ 实施"的口径不用重写**：epic 正文可原样保留 Scope/AC，只加"步骤清单 + 每步映射"。

**需要的远端改动（✅ 已于 2026-10-19 执行完成；采纳 oracle 方案 A）**：由 `github-operator` 在用户批准后执行以下 **6 项**（docs-writer 不改远端）：

1. **追加 `## Steps`**：7 个 checkbox（每项 `- [ ]` + 子 issue 链接；**创建后已回填为真实编号 #46–#52**），并**写明“每个 step 一个独立 PR”**；
2. **追加 `## Step → acceptance mapping`**（即 §5.3 的表）；
3. **修改 Rollback 段的 "single PR" 表述（P1-1）** → "delivered as **one pull request per step**; each step is independently revertible; the Jakarta rewrite and guard-test rewrite are reverted together"；
4. **修改 Non-goals 的 `@MockBean` 句（P1-7 / C-2）** → "The core upgrade step (S3) does not migrate `@MockBean`; step 5 (S5) does it as a follow-up step."；
5. **保留原 Scope / AC-1..AC-11 不动**（作为总契约，不改数字与验收）；
6. **不关闭 #44**：epic 保持 open，待 S7 完成后由主 agent 关闭（或按 milestone 口径保持 open 至 milestone 关闭）。

**实际执行结果（2026-10-19，`github-operator`；证据 `.pi/artifacts/github-operator-issue-44-steps.md`，主 agent 已核验）**：① ✅ `## Steps` 已追加（1 句“one PR per step / 每步可独立回退” + 7 个 checkbox 链接 **#46–#52**，链接逐一核对）；② ✅ `## Step → acceptance mapping` 已追加（AC-1..AC-11 → 主责/复核步骤/证据形式，含 AC-6 条件分支）；③ ✅ Rollback 段已改为 “**delivered as one pull request per step** … **Each step is independently revertible; the Jakarta rewrite and guard-test rewrite are reverted together.**”（**仅此 1 行变化**，原质量门禁句保留）；④ ✅ Non-goals 的 `@MockBean` 句已改为澄清句（**仅此 1 行变化**）；⑤ ✅ `## Goal` / `## Background` / `## Scope` / `## Acceptance criteria` / `## Dependency` / `## Risk notes` **逐字未改**，AC-1..AC-11 全部保留；⑥ ✅ #44 **保持 OPEN**（编辑后核验：`44 | OPEN | milestone=3 | labels=0`），标题/milestone/labels/state 均未变。**上述只是立项/拆分层面的远端变更，S1–S7 一律未开工。**

**子 issue 统一规范**：标题带 `[Step N/7]`；**正文**含 `Part of #44`（不是只写在 Internal tracking）；milestone **#3**；labels **空**；`Internal tracking` 分区**不发布**；每份正文**只描述本步范围**（非目标与依赖写死）。

### 5.2 备选方案（B）：关闭 #44，由 7 个小 issue 取代

**代价**：#44 的正文字段落与 AC-1..AC-11 **必须重新分配**到 7 个 issue；已批准的 Scope/Non-goals 变成 7 处副本，后续修订要改 7 处；评论区与立项记录断链。**只在用户明确要求"一个 issue 只装一周内的工作"时才选 B。**

### 5.3 #44 验收条件 → 步骤映射（推荐方案 A 下写进 epic 正文）

| #44 AC | 子 issue（主责 / 复核） | 主责步骤 | 复核步骤 | 证据形式 |
| --- | --- | --- | --- | --- |
| AC-1 `mvn verify` 用例数不下降 | #48 / #51 | S3 | S6 | 命令输出 + 与 S1 基线对比 |
| AC-2 覆盖率闸门**不放松**（门槛已于 2026-10-20 由用户批准收紧为 **0.90**，前端 **90**；本 epic 以收紧后门槛验收） | #48 / #51 | S3 | S6 | `All coverage checks have been met.` |
| AC-3 jar 形态（`BOOT-INF/classes/static/index.html`） | #48 / — | S3 | — | `unzip -l` 输出 |
| AC-4 dist 缺失 → BUILD FAILURE | #48 / #49 | S3 | S4（回退检查） | 两次构建结果对比 |
| AC-5 前端全绿且不变 | #51 / — | S6 | — | 前端 5 条命令输出 + `git diff` 证明前端零改动 |
| AC-6 生产 compose smoke + JDBC 语义 | #51 / #47 | S6 | S2（仅 JDBC 部分） | HTTP 状态/响应体 + 日期一致性。**条件分支（P2-7）**：若 S2 结论为保持 8.3.0，则 AC-6 的 JDBC 分支不触发。**实际（2026-10-20）：S2 已升到 9.7.0 → 分支已触发**，JDBC 部分的证据由 S2 承担（`docs/logs/2026-10-20-issue-47-dependency-readiness.md` E4–E7），终态复核归 S6 |
| AC-7 认证/会话语义 | #51 / #48 | S6 | S3（单测证据） | smoke + `mvn verify` 用例 |
| AC-8 守卫测试与文档一致 | #48 + #52 / — | S3（代码）+ S7（文档） | — | 测试文件 + 契约文档 §8 |
| AC-9 无新增 JaCoCo exclude | #48 / #51 | S3 | S6 | `pom.xml` 8 项对照 |
| AC-10 CI 双 job 绿 | #48 / #49, #51 | S3（后端 job 切 JDK 21） | S4、S6 | PR 的 CI 运行 |
| AC-11 JDK 一致性 + 字节码 61 | #48 + #49 / #51 | S3（字节码/`mvn -version`/`Dockerfile` 两阶段）+ S4（enforcer/README/镜像取证） | S6 | `javap` + `mvn -version` + 两个 stage 标签 |

### 5.4 #44 Scope → 步骤映射

| #44 Scope 条目 | 步骤 |
| --- | --- |
| Baseline（first step） | **S1** |
| Toolchain（`release=17`、JDK 21 + README + enforcer `requireJavaVersion`） | `release=17` → **S3**；JDK 21 强制/enforcer/README → **S4**；**两阶段镜像 → S3（B-1）** |
| Build（BOM 3.5.x、`release` 属性、插件依赖对齐） | **S3** |
| Jakarta migration（11 文件） | **S3** |
| Architecture guard test（白名单 + 集合相等 == 11） | **S3** |
| Dependency compatibility check（驱动/jasypt/BOM 传递升级） | **S2**（驱动/jasypt）+ **S3**（BOM 传递升级的实测）+ **S6**（smoke 取证） |
| CI（`java-version`/job 名/step 名） | **S3**（见 §6-C-1） |
| Docker（两 stage → JDK 21） | **S3**（两行 `FROM`，B-1；随 `release=17` 合并才能保证生产路径不损坏）+ **S4**（镜像取证：两 stage 确为 21 + `docker compose build app` 成功） |
| Docs（live documents only：契约 §8/§12 + 陈旧覆盖率数字；README） | README → **S4**；契约文档 → **S7** |
| Verification（后端/前端/e2e/生产 smoke） | **S3**（后端/打包）+ **S6**（全量 + smoke） |
| （#44 Non-goals 的 `@MockBean`） | **S5** —— **C-2 已裁定 (a)**：作为 epic 子 issue（正文含 `Part of #44`），epic Non-goals 以**澄清句**修订（“core step 不做、step 5 做”，见 §5.1 第 4 项） |

---

## 6. 原待裁定事项 —— 裁定结果（2026-10-19，主 agent：全部接受 oracle 建议）

> oracle 报告：`.pi/artifacts/oracle-issue-44-split-review.md`（§1 Blocker、§2 P1、§3 P2、§4 C-1..C-4、§5 epic 处理、§7 决策项）。下表是**已裁定并已落入**本文与 7 份草稿的状态；**未获用户批准前不得据此创建/编辑任何 GitHub issue**。

| # | 事项 | 裁定 | 落点 |
| --- | --- | --- | --- |
| **B-1** | S3 合并后 Docker 生产路径损坏（`docker compose build app` 失败，而 CI 仍绿） | 采纳：`Dockerfile` 两行 `FROM` 并入 **S3**；**S4 缩窄**为 enforcer + README + 镜像验证/取证 | 本文 §2/§3/§4-S3/§4-S4/§5.4/§8；S3、S4 草稿 |
| **C-1** | `ci.yml` 的 JDK 升版放哪一步 | **(a)**：`java-version: "21"` + job/step 改名并入 **S3**；**S4 不改 CI** | 本文 §3/§4-S3/§5.4；S3、S4 草稿 |
| **C-2 / P1-7** | S5（`@MockBean`→`@MockitoBean`）与 #44 Non-goals 的冲突 | **(a)**：S5 作为 **epic 子 issue**（正文 `Part of #44`）；epic Non-goals 改为澄清句（“core step 不做、step 5 做”），属澄清不缩减范围 | 本文 §4-S5/§5.1/§5.4；S5 草稿；#44 跟踪记录 |
| **C-3** | S3 是否必须跑全量前端套件 | **(a)**：S3 只跑 `npm ci && npm run build`（`-Pfrontend` 所需）+ 前端零改动证据，全量/e2e 归 **S6** | 本文 §4-S3；S3 草稿 |
| **C-4** | 工期口径与 #44 正文的 "~1.5–3 weeks" 不一致 | **(a)**：epic 正文改为“**工作量 ≈ 1.5 周 / 日历 ≈ 5–7.5 周（含 7 轮审查）**” | 本文 §7；#44 跟踪记录 |

**无需裁定但必须知会**：

- **S4 的实现细节发现**：`maven-enforcer-plugin` **目前只在 `frontend` profile 内**（`pom.xml:178-186`），所以 `requireJavaVersion` 不能"就地加一条规则"，必须移/加到主 `<build>`；否则只在 `-Pfrontend` 时生效（等于没有）。已写入 S4 草稿的 Scope。
- **S5 的合并顺序（已按 (a) 确定）**：S5 依赖 S3，且**必须在 S6 之前合并**，否则 S6 的"最终形态"证据里仍含弃用警告。
- **S1 的 JDK 21 安装（P1-5）**：**安装 JDK 需主 agent 批准**（超出当前“安装 Ollama/模型”的授权范围）；S1 只记录 blocking gap，不得自行安装。

---

## 7. 工期估算

| 口径 | 数值 | 说明 |
| --- | --- | --- |
| **工作量（人日）** | **6.5–7.5**（S1 0.5 ∥ S2 1 → 关键路径 ≈ 6.5） | 不含审查往返；含每步跑验证命令的时间 |
| **日历（含审查/修复/PR/CI）** | **≈ 5–7.5 周** @ 每周 >10 小时 | 建议值：S1+S2 并行 ≈ 1–1.5 周；S3–S7 ≈ 4–6 周。**7 轮 ×（oracle 预审 + 3 reviewer + 修复 + CI）是主要成本** |
| 与已批准计划对照 | 计划 §5 给 "**1.5–3 周**"（A0–A7） | 口径不同（纯工作量 vs 含审查日历）。**C-4 已裁定 (a)**：epic 正文改写为“工作量 ≈ 1.5 周 / 日历 ≈ 5–7.5 周（含 7 轮审查）”，两口径同时写明，避免被读成违约。**注**：远端 `Estimate` 行仍为“~1.5–3 weeks”，该改写**未落远端**（见元数据表“估算”行与 #44 文档“剩余待确认”第 9 项） |

**估算假设**：单开发者；现有测试兜底（后端 134（门槛收紧后；收紧前 127）/ 前端 195 / e2e 12）；**未含** jasypt 必须替换、Boot 3 行为差异导致大范围重写、本地 Docker 不可用导致 S6 顺延等情形。

---

## 8. 全局风险与回退

| 风险 | 概率 | 影响 | 缓解 |
| --- | --- | --- | --- |
| S3 的 Boot 3 行为差异超出预期（trailing-slash/错误响应/Tomcat 10/Jackson/Hikari/Logback） | 中高 | S3 超 3 天，或被迫做行为变更 | 差异**必须显式记录并获批**；S3 的 PR 允许"记录为后续 issue"而不放宽闸门；必要时回主 agent 决定是否另立 issue |
| ~~中间态 main 变红（S3 与 CI 不同步）~~ / **中间态 Docker 损坏** | ~~中~~ **已消除** | ~~破坏 main 常绿~~ | **B-1 + C-1**：`Dockerfile` 两行 `FROM` 与 `ci.yml` 的 JDK 升版**均并入 S3** → S3 合并后构建/CI/容器路径同步；残余风险仅剩“镜像拉取失败”（环境问题） |
| JDBC 差异被误归因为 Boot 3 | 中 | 排错时间翻倍 | **S2 单独验证驱动**（拆分的核心收益）；**实际（2026-10-20）S2 定 9.7.0 → AC-6 的 JDBC 分支已触发**，其证据由 S2 承担、S6 复核 |
| 本地 JDK 21 未安装 / Boot 2.7 在 JDK 21 上不可运行 | 中 | S1 的 JDK 21 基线不可得 | S1 如实标注"跨 JDK 对比"并记录对比规则（P2-4），不伪造基线；**安装 JDK 需主 agent 批准（P1-5）** |
| Docker/8081 不可用（S3 容器 smoke、S6 的 AC-6） | 中 | S3 DoD 7 与 S6 的 AC-6 无法取证 | 标注"未执行 + 原因"；**#44 不得因此关闭** |
| 守卫白名单被绕过（新增文件悄然引入 `jakarta.servlet`） | 低 | 架构约束退化 | DoD 要求**集合相等**断言 + 10 个已知文件的 diff 证据；额外改动必须在 PR 里逐条申报（P1-4） |
| 7 个 PR 的审查疲劳导致后期步骤审查质量下降 | 中 | 缺陷漏检 | 每步都有可执行 AC；S6 做整体复核 |
| 拆分本身改变范围（如 S2 变成"升级所有依赖"） | 低 | 违反已批准范围 | 每份草稿写死"**仅此步骤范围**"+ 非目标；越界 → 回主 agent |

**整体回退**：每一步都是独立 PR、无破坏性操作；任一后续步骤失败，前置步骤的成果仍可保留（S1/S2 不改行为；S3 失败整体 revert 即回到 Boot 2.7 的绿色 main）。

---

## 9. oracle 审查结论（已完成 2026-10-19）——原「应重点检查」清单已逐条回答

审查报告：`.pi/artifacts/oracle-issue-44-split-review.md`。结果：**修完 B-1 + P1 后可交用户批准创建**（现已全部修完）。

1. **§1.1 的原子性论证** → ✅ 成立（oracle §6 “确认无误”）；**但 oracle 发现同类原子性问题在 Docker 侧** → **B-1**（已修：镜像并入 S3）。
2. **§6-C-1（`ci.yml` 并入 S3）** → ✅ 采纳 (a)；其他会合并后变红的点：**`Dockerfile`**（B-1，已并入 S3）。
3. **§2 顺序** → ✅ 成立；C-2 裁定为 (a) 后，S5 作为 epic 子 issue 仍必须在 S6 之前合并。
4. **S3 非目标是否会膨胀** → oracle 反过来指出**非目标过严**（“恰好 11 文件”会让合法升级修复失败）→ **P1-4** 已拆为 (a)+(b)；同时补 **P1-8**（驱动 pin 不被 BOM 覆盖）。
5. **§5.3/§5.4 映射是否覆盖全部 Scope/AC** → ✅ 覆盖；调**§5.3 AC-6 加条件分支（P2-7）**、**§5.4 Docker → S3 + S4 取证**。
6. **§6-C-2：S5 是否会削弱已批准 Non-goals** → 采纳 **(a)** + epic 澄清句（P1-7），并补 **P1-2**（正文 `Part of #44`）。
7. **S1 的“JDK 21 上跑 Boot 2.7”假设** → 保留为“尽力而为 + 如实记录”；新增 **P1-5**（不得安装 JDK）与 **P2-4**（跨 JDK 对比规则）。

---

## 10. 本拆分**不改变**任何已批准的范围与决策

以下仅供对照，**均未被本文件修改**：

- 迁移范围 = #44 正文的 Scope（Boot 3.5.x + `release=17` + JDK 21 工具链/CI/镜像 + 11 文件 jakarta + 守卫白名单 + 依赖核查 + CI/Docker/文档 + 回归验证）；
- **零行为变更**红线：URL、状态码、JSON 形状与文案、认证/会话语义、SPA fallback 真值表、数据库 schema 全部不变；
- 质量闸门：后端 JaCoCo `BUNDLE/LINE ≥ 0.90`（**2026-10-20 收紧后**；基线 **0.9165 / 134 tests**；收紧前快照 0.85 / 0.8980 / 127 tests）、前端 `lines ≥ 90`（基线 92.35%；收紧前为 88）、e2e 12 次；**不得扩 `excludes`（8 项）、不得降阈值、不得跳过 e2e**；
- 已批准决策：D1/D-A1（Boot 3.x 取 3.5.x）、D-A2（`release=17` + 本地/CI/镜像 JDK 21）；**D-A3/D-A4/D-A5/D-A7 仍未逐条裁定**（本拆分按"计划建议"落位：白名单 (a)、`@MockBean` 拆出（C-2 已裁定为 epic 子 issue）、pin 先保持、允许的等价修改清单）；
- `docs/architecture/frontend-migration-contracts.md` §10 的 **AD-1**（不引入 Spring Security/JWT 到网页端）、**AD-2**（单 JVM）；
- 后续 AI 路线（Option B/C：本地 LLM、RAG、agent 平台）**不受本拆分影响**，只是被 S3 解锁依赖前提——本文件不新增任何 AI 范围。

---

## 11. 交付物与下一步

**本轮产出（均已发布/定稿）**

| 文件 | 内容 | 状态 |
| --- | --- | --- |
| `docs/plans/issue-44-migration-steps.md` | 本文件（拆分总览） | **已批准，子 issue 已创建（2026-10-19）** |
| `docs/issues/issue-46-baseline.md` | S1 = **[#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)**（P1-5 / P2-4 / `Part of #44`） | **已创建（OPEN）** |
| `docs/issues/issue-47-dependency-readiness.md` | S2 = **[#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)**（`Part of #44`） | **已创建（OPEN）** |
| `docs/issues/issue-48-core-boot3-jakarta.md` | S3 = **[#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)**（B-1 / P1-3 / P1-4 / P1-8 / P2-8 / C-1 / C-3 / `Part of #44`） | **已创建（OPEN）** |
| `docs/issues/issue-49-jdk21-toolchain.md` | S4 = **[#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49)**（缩窄为 enforcer + README + 镜像取证；P2-3 / `Part of #44`） | **已创建（OPEN）** |
| `docs/issues/issue-50-mockito-annotation-cleanup.md` | S5 = **[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)**（C-2 = (a)；P2-2 / `Part of #44`） | **已创建（OPEN）** |
| `docs/issues/issue-51-regression-proof.md` | S6 = **[#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)**（P2-1 的 10 条深链 / `Part of #44`） | **已创建（OPEN）** |
| `docs/issues/issue-52-close-out.md` | S7 = **[#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52)**（P1-6 的 §11 / P2-6 的日志编号 / `Part of #44`） | **已创建（OPEN）** |
| `docs/issues/issue-44-boot3-java17-jakarta-migration.md` | #44 = **epic（OPEN）**：已记录 7 个子 issue、epic 正文的 2 处修改与 2 个新增区块 | **已回填（2026-10-19）** |
| `docs/plans/issue-44-migration-checklist.md` | **7 步迁移 checklist**（每步：一句话目标 / 精确验证命令 / 回退点 / 完成分支-commit + 状态列；顶部链接 #44 与 #46–#52） | **新增（2026-10-20）**：S1 = 基线已测量（见 `docs/logs/2026-10-20-issue-46-baseline.md`），S2–S7 = 未开始。**更新（2026-10-20 晚些：S2 实施后）**：S2 = 已实施（待 PR 合并），并补 S2 实际结果表与 S3 的驱动复核要求。**更新（2026-10-20 收尾）**：S1/S2/S3/S4 = 已完成（PR #53/#55/#56/#57，merge `9696848`/`55f8d79`/`70fb78b`/`9a7705c`；#48、#49 已 CLOSED），S5 = 已实施（2026-10-20，待 PR 合并）+ 实际结果表，S6 前置状态已注明（最终形态代码已就绪，但 S5 未合并前不得在 `main` 上取证「无弃用注解」） |

> 上表的 7 份 issue 文档原名均为 `docs/issues/proposed-step-*.md`，issue 创建后按真实编号改名（文件未跟踪 → 直接 `mv`，无内容丢失）；除顶部元数据与 `Internal tracking` 外，`## GitHub issue body` 与远端**逐字一致**。

**下一步（按顺序，缺一不可）**

1. ✅ `oracle` 只读审查本文件 + 7 份草稿 → 已完成（结论：修完 B-1 + P1 后可交用户批准）；
2. ✅ 主 agent 裁定 **B-1、P1-1..P1-8、P2-1..P2-8、C-1..C-4**（全部接受）→ 已完成；
3. ✅ docs-writer 按裁定更新本文件与 7 份草稿（不改远端）→ 已完成（本轮）；
4. ✅ **用户授权**：① 创建 7 个 issue → 已授权；② 编辑 #44 为 epic → 已授权；③ 安装 JDK 21 → **已安装**（`/opt/homebrew/opt/openjdk@21`，2026-10-19）；④ PR [#45](https://github.com/PNZ-z/ai-powered-Fabflix/pull/45) 是否先合并 → **仍待定**（未合并前这些文档不在 `main`）；
5. ✅ `github-operator` 已按草稿创建 issue（只用 `## GitHub issue body` 分区，**保留 `Part of #44`**），真实编号 **#46–#52**（主 agent 已回传）；
6. ✅ docs-writer 已把草稿改名为 `docs/issues/issue-46-baseline.md` … `issue-52-close-out.md`，并回填 #44 的**本地记录**（`## Steps` 与 `## Step → acceptance mapping` 已在创建时由 operator 写入真实链接）；
7. 🔵 **下一步（2026-10-20 回填）**：~~S1（#46）开工~~ → **S1 已合并**（PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53) @ `9696848`）、**S2 已合并**（PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55) @ `55f8d79`）、**S3 已合并**（PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) @ `70fb78b`，#48 CLOSED，CI 双 job SUCCESS）。**S4 已合并**（PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57) @ `9a7705c`，#49 CLOSED，CI 双 job SUCCESS）；**S5（#50）已合并**（PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) @ `ff57e79`，#50 CLOSED，CI 双 job SUCCESS）；**S6（#51）已合并**（PR [#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59) @ `b4a6d2a`，#51 CLOSED）→ **当前下一步 = S7（#52）的提交、推送、PR 与合并**（**仅 `docs/`**，分支 `codex/issue-52-close-out`）；**S7 已实施（2026-10-21）、待 PR 合并**（#52 仍 OPEN）。 原始流程（供参考）：**S1（#46）开工**——先建 `docs/plans/issue-46-baseline.md`（若需要）→ oracle → 单一 worker 跑基线命令 → 3 reviewer → 日志；之后 S2…S7 各自走完整工作流（每步计划 `docs/plans/issue-<编号>-<slug>.md`、日志 `docs/logs/YYYY-MM-DD-issue-<编号>-<slug>.md`）。~~S1–S7 目前全部未开始。~~ → **2026-10-21 实际：S1/S2/S3/S4/S5/S6 已合并（PR #53/#55/#56/#57/#58/#59）；S7 已实施、待 PR（仅 `docs/`）。**

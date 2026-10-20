# 工作流日志 / 实施记录 — Issue #49（S4：用机器可执行的方式强制 JDK 21 + README + 镜像取证）

> 本文件是 **S4（[#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49)）的实施、验证与记录**，并按 `AGENTS.md` §8 承载本工作流日志的两大节：
> **一、修改了哪些文件及潜在问题**（§一）；**二、新增知识点 / 重点学习对象**（§二）。
>
> **口径声明**：§0 的 `mvn` / Docker / 容器结果由**主 agent 亲自运行并核验**（另与单一 `worker` 的报告一致，`.pi/artifacts/worker-issue-49-jdk21.18e55600.cf65fa7e.md`）；`docs-writer` **没有**运行 `mvn` / npm / Docker / 应用 smoke / CI，只做了**只读复核**（`git status`/`git diff`/`git diff --stat`、`README.md` 与 `pom.xml` 新增段全文、`Dockerfile` 的 `FROM`/`USER`/`EXPOSE`/`ENTRYPOINT`、`.github/workflows/ci.yml` 的 job/step 名与 `java-version`、`git log --merges`、两个 JDK 的 `java -version`）与文档写入，**未执行任何 GitHub 写操作**。

## 元数据

| 项目 | 值 |
| --- | --- |
| 记录日期 | **2026-10-20**（实施、验证与记录同日，本机时区 America/Los_Angeles / PDT） |
| Issue | **#49** — https://github.com/PNZ-z/ai-powered-Fabflix/issues/49（`[Step 4/7] Enforce JDK 21 in the build and document/verify it`，milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)，labels 空，state **OPEN**） |
| 上游 epic | [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（epic，**OPEN**）；前置 S3 [#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)（**已随 PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 合并**，merge commit `70fb78b`） |
| 分支 | **`codex/issue-49-jdk21-toolchain`**（基于 `70fb78b` = `Merge pull request #56 from PNZ-z/codex/issue-48-core-boot3-jakarta`） |
| Commit SHA | **无本步提交**（`git diff --cached` 为空；改动未暂存） |
| 提交 / 推送 / PR | **未提交、未推送、无 PR**。远端 `main` 未受影响；**#49 仍 OPEN**；checklist 中 S4 记为「已实施（2026-10-20），待 PR 合并」 |
| 树状态（本轮结束） | `M README.md`（+17）、`M pom.xml`（+23）→ `git diff --stat` = **2 files changed, 40 insertions(+), 0 deletions(-)**；另有**开工前既存**的未跟踪 `?? .idea/`（IDE 目录，非本步产物）与 `.pi/artifacts/`（不在 `docs/`，不入提交） |
| 构建工具链 | **JDK 21** = `/opt/homebrew/opt/openjdk@21`（`openjdk version "21.0.12.1"`）；**JDK 27** = `/opt/homebrew/opt/openjdk`（`openjdk version "27"`，本机 Maven 默认） |
| 目标形态 | 构建 / `mvn` 运行门禁 = **JDK 21**；编译目标不变 = `maven.compiler.release=17`（class file **major 61**）；容器镜像 = **JDK 21 两阶段**（S3 已交付，本步**只验证不改**） |
| 数据库环境 | 容器 `fabflix-mysql` 保持运行；本步容器验证后仅 `docker compose rm -sf app`，**未执行 `docker compose down`**（不触碰 MySQL 容器与数据卷） |
| 关联计划 / checklist | `docs/plans/issue-44-migration-checklist.md`（状态总表、§S4、§S5 前注）、`docs/plans/issue-44-migration-steps.md`（§3 总表 S4 行、§4-S4 DoD） |
| 关联 issue 文档 | `docs/issues/issue-49-jdk21-toolchain.md`（#49 的规范来源与 AC 回填）、`docs/issues/issue-48-core-boot3-jakarta.md`（S3 / AC-6 回填）、`docs/issues/issue-44-boot3-java17-jakarta-migration.md`（epic 进展块） |
| 证据来源 | 主 agent 终验（`mvn validate` / `mvn verify` / `-Pfrontend package` / `docker compose build app` + `up`）、worker 报告 `.pi/artifacts/worker-issue-49-jdk21.18e55600.cf65fa7e.md`、三路 reviewer 结论（主 agent 转述）、docs-writer 只读复核（§0 的 E8） |

---

## 0. 验证命令与结果汇总（证据索引）

> 「执行者」列区分 **主 agent / worker**（实际运行）与 **docs-writer**（只读复核）。

| # | 验证项 | 关键命令（节选） | 结果 | 执行者 |
| --- | --- | --- | --- | --- |
| **E1** | AC-1：JDK 21 下 `validate` 通过且规则确实执行 | `JAVA_HOME=/opt/homebrew/opt/openjdk@21 … mvn --batch-mode --no-transfer-progress validate` | `enforcer:3.5.0:enforce (require-jdk-21)` → **`Rule 0: … RequireJavaVersion passed`**；**BUILD SUCCESS** | 主 agent（worker 报告一致） |
| **E2** | AC-2：范围外 JDK → BUILD FAILURE + 可读消息 | `JAVA_HOME=/opt/homebrew/opt/openjdk … mvn --batch-mode --no-transfer-progress validate`（JDK **27**） | **BUILD FAILURE**，`Rule 0: … RequireJavaVersion failed with message:` → `Fabflix pins the build and Maven-run to JDK 21 (bytecode release=17; Boot 3.5 runs on 17-24, but this project is verified on 21 only); set JAVA_HOME to a JDK 21 installation - see README`（**预期行为，不是 bug**） | 主 agent |
| **E3** | AC-6（门禁不回归）：JDK 21 `verify` | `JAVA_HOME=/opt/homebrew/opt/openjdk@21 … mvn --batch-mode --no-transfer-progress verify` | **BUILD SUCCESS**；**134 tests / 0 failures / 0 errors / 0 skipped**；`All coverage checks have been met.`；JaCoCo **988 covered / 90 missed / 1078 = 0.9165**（门槛仍 0.90，未改） | 主 agent（worker 同参数两次结果一致） |
| **E4** | AC-6（字节码不变） | `javap -verbose -cp target/classes com.fabflix.boot.FablixApplication \| grep 'major version'` | **`major version: 61`**（`release=17` 未变） | 主 agent（worker 报告一致） |
| **E5** | AC-3（正向）：`-Pfrontend` 打包不回退 | `mvn -Pfrontend -DskipTests clean package`（`frontend/dist` 存在） | **BUILD SUCCESS**；jar 内含 **6 个 `static` 条目**（含 `BOOT-INF/classes/static/index.html`）；日志中 **两个 execution 都执行**：`enforce (require-jdk-21)` → passed，`enforce (require-frontend-dist)` → passed | 主 agent |
| **E6** | AC-3（负向）：dist 守卫仍生效 | 移走 `frontend/dist` 后重跑 `mvn -Pfrontend -DskipTests clean package`，随后恢复 | **BUILD FAILURE**（`enforcer:3.5.0:enforce (require-frontend-dist)`，消息为 `frontend/dist/index.html is missing …`）；**已恢复 `frontend/dist`** | 主 agent |
| **E7** | AC-4：容器只验证不改（S3 的产物） | `docker compose build app` → `docker compose up` → health → `docker exec … java -version` / `id` → `docker compose rm -sf app` | 构建成功；容器启动后 health **200**（`{"application":"fabflix-spring-boot","status":"UP"}`）；容器内 `openjdk version "21.0.12.1"`；运行用户 **uid/gid 1001（非 root）**；随后仅 `rm -sf app`，**未 `down` mysql** | 主 agent |
| **E8** | docs-writer 只读复核 | `git status --short`；`git diff --stat`；`sed -n` 读 `README.md` 新增段与 `pom.xml` 新增段全文；`grep -n "^FROM\|USER\|EXPOSE\|ENTRYPOINT" Dockerfile`；`grep -n "name:" .github/workflows/ci.yml`；`git log --merges --oneline -8`；两个 JDK 的 `java -version` | `git diff` 仅 `README.md`/`pom.xml` 两文件（40 插入 0 删除）；`pom.xml` 只有主 `<build>` 新增一个 `require-jdk-21` execution（其余插件/依赖/阈值/`excludes` 未动）；`README.md` 只在 `## Current architecture` 与 `## Local database` 之间新增 `## Java toolchain`，既有章节**原文未改**；`Dockerfile` 两阶段仍为 `maven:3.9-eclipse-temurin-21` / `eclipse-temurin:21-jre` + `USER fabflix`/`EXPOSE 8081`/`ENTRYPOINT`（**未改**）；`ci.yml` 仍为 job `Build and test (Java 21)`、step `Set up Java 21`、`java-version: "21"` + 前端 job `Frontend (lint, test, coverage, build, e2e)`（**未改**）；`git log` 确认 PR #56 的 merge commit = `70fb78b` | docs-writer |

**未执行 / 未取证（不得视为已通过）**：① **本步的提交、推送、PR 与 PR CI**（尚无 PR）；② 未在 **JDK 17** 下单独复跑（AC-2 只要求「范围外 JDK 失败」，已用 JDK 27 取证；JDK 17 被 `[21,22)` 拒绝是规则语义，无需实测）；③ 前端 `lint`/`test`/`coverage`/`e2e`（本步不触前端）；④ 容器内的完整业务 smoke（S3 已做；本步只验证「镜像 JDK 21 + health 200 + 非 root」）；⑤ `docs/architecture/frontend-migration-contracts.md` 的 §8/§11/§12 文档同步（S7）。

---

## 一、修改了哪些文件及潜在问题

### 1.1 `pom.xml`（+23 行；主 `<build><plugins>` 首位新增一个 plugin 块）

| 项目 | 值 |
| --- | --- |
| 插件坐标 | `org.apache.maven.plugins:maven-enforcer-plugin` **3.5.0**（与 `frontend` profile 内既有声明**同一坐标/同一版本**） |
| execution `id` | **`require-jdk-21`** |
| 绑定 | `phase=validate`，`goal=enforce` |
| 规则 | `<requireJavaVersion><version>[21,22)</version>` |
| 失败行为 | `<fail>true</fail>`（规则不满足即构建失败，不降级为 WARN） |
| 消息（最终版） | `Fabflix pins the build and Maven-run to JDK 21 (bytecode release=17; Boot 3.5 runs on 17-24, but this project is verified on 21 only); set JAVA_HOME to a JDK 21 installation - see README` |

**关键点**：`maven-enforcer-plugin` 原本**只存在于 `frontend` profile 内**（`require-frontend-dist`）。本步把插件**声明提升到主 `<build>`**，profile 里那条 execution **原样保留**。Maven 按 `<id>` 合并同一 plugin 的 execution 列表 —— 于是主构建跑 `require-jdk-21`，`-Pfrontend` 构建跑 **两条**（E5/E6 的日志逐条印证），没有互相覆盖或跳过。

**未改动（逐项确认）**：`spring-boot.version`、`maven.compiler.release`（仍 `17`）、`mysql-connector-j`（仍显式 `9.7.0`）、JaCoCo 阈值（仍 `0.90`）、`<exclude>`（仍 8 项）、其他插件与精确 pin、`frontend` profile 的 `require-frontend-dist` 配置、`compose.yaml`、`Dockerfile`、`.github/workflows/ci.yml`、`src/main/**`、`src/test/**`。

### 1.2 `README.md`（+17 行；在 `## Current architecture` 与 `## Local database` 之间新增 `## Java toolchain`）

新增段落的内容（按 review 采纳后的**最终措辞**）：

1. **构建边界**：`Building the backend — every mvn command, including mvn spring-boot:run — requires JDK 21`；`JAVA_HOME` 必须指向 JDK 21（Maven 使用 `JAVA_HOME` 选定的 JDK）；`pom.xml` 的 `requireJavaVersion [21,22)` 在 `validate` 阶段对任何其他版本失败；错误消息指回 README。
2. **自检命令**：`mvn -version   # must report "Java version: 21.x"` + `export JAVA_HOME=/path/to/jdk-21`（JDK 21 安装目录）。
3. **IDE 提示**：`IDE-embedded Maven uses the IDE's project JDK, so set that to 21 as well.`（enforcer 管不到 IDE 内嵌 Maven 用的 JDK，只能提示）。
4. **构建 vs 运行**：编译器仍 `release=17` → 字节码 class file major 61 → **打包后的 jar 可在 Java 17+ 运行**；**「JDK 21 是构建工具链，不是语言级别」**；CI 也在 Java 21 构建；`Dockerfile` **both JDK stages** 为 JDK 21 → 本地 / CI / 镜像同一个 JDK。
5. **宿主 vs 镜像**：Compose 命令在镜像内构建与运行，**不依赖宿主 JDK**；`mvn` 命令在宿主运行，**依赖**。

> 与旧稿的区别（review 采纳项）：首句由 "Building and running the backend" 收窄为 "Building the backend — every `mvn` command"（`java -jar` 运行 17+ 即可，写「running 需要 21」过宽）；`mvn -version` 断言由 `"Java version: 21"` 改为 `"21.x"`；补 `export JAVA_HOME=…` 与 IDE 内嵌 Maven 提示；`both stages` → `both JDK stages`（`Dockerfile` 实际有三个 stage，其中一个是 `node:22-alpine`）。

### 1.3 未改动的文件（本步刻意不做）

| 文件 | 为何不改 |
| --- | --- |
| `Dockerfile` | 两行 `FROM` 与其非 root 块属 **S3（B-1）**，已合并；本步**只验证取证**（E7） |
| `.github/workflows/ci.yml` | JDK 21 切换 + job/step 改名属 **S3（C-1）**，已合并 |
| `docs/architecture/frontend-migration-contracts.md` | §11/§8 等契约描述属 **S7（#52）** |
| `frontend/README.md` | 其中 `mvn -Pfrontend` 说明未提 JDK 21 → 记入 **S7 docs 清单**（见 §1.5-6） |
| `src/main/**`、`src/test/**` | 本步零源码/测试改动（`git status` 可证） |

### 1.4 三路 review 结果与文案修正

- **无 Blocker**（正确性与边界 / 测试覆盖 / 必要性与复杂度 三路一致）。
- **接受并已修正的文案项**（worker 已落到工作区，即当前 `git diff`）：
  1. enforcer 消息对 **JDK 22–24** 有歧义（旧稿 "Spring Boot 3.5 supports 17-24" 会被读成「22–24 也被支持」）→ 改为 **「pin 21；Boot 3.5 能跑 17-24，但本项目只在 21 上验证」**；
  2. README 首句 "Building and running" 过宽 → 收窄为「构建（所有 `mvn` 命令）」；
  3. `mvn -version` 断言 `"Java version: 21"` → **`"21.x"`**（补丁号会变）；
  4. 补 `export JAVA_HOME=/path/to/jdk-21` 与 IDE 内嵌 Maven 提示；
  5. `both stages` → **`both JDK stages`**。
- **登记到后续、本步不改代码**（主 agent 裁定，记入 §1.5）。
- **证据口径提醒**：worker 原始报告（`.pi/artifacts/worker-issue-49-jdk21.18e55600.cf65fa7e.md`）里出现的消息文本、`README.md` 首句、`mvn -version` 断言均为**修订前**版本；上述 5 项修正后的**最终文本以当前 `git diff`（及本日志 §1.1 / §1.2 / §0 E2）为准**。主 agent 终验使用的是修正后的最终文本。

### 1.5 潜在问题、遗留风险与后续事项

1. **未提交 / 未推送 / 无 PR / 未合并**：`#49` 仍 **OPEN**，准确表述是「已实施（本地工作区，分支 `codex/issue-49-jdk21-toolchain`），待 PR 合并」。**不得**说 S4 已完成或 `main` 上已有该门禁。
2. **JDK 22+ 被刻意拒绝**（`[21,22)` 的右开区间）：Spring Boot 3.5 支持 17–24，但本项目只验证 21；**放宽区间属于改变支持矩阵的独立决策**，不在本步。
3. **JDK 27（本机默认）现在会直接失败**：这正是本步的目的（P1-3）。任何未设 `JAVA_HOME` 的 `mvn` 命令（含 `mvn spring-boot:run`）都会在 `validate` 阶段失败 —— **预期行为，不是 bug**；README 已写清，失败消息也指回 README。
4. **IDE 内嵌 Maven 是规则覆盖不到的角落**：enforcer 只看「Maven 进程实际用的 JDK」，IDE 用工程 JDK 时可能绕过终端里的 `JAVA_HOME`。README 只能提示，无法机器强制（残余风险：本机 IDE 若设为 17/27，IDE 内构建失败而终端成功，或反之）。
5. **`require-frontend-dist` 与 `require-jdk-21` 的合并语义**依赖 Maven「按 execution id 合并」这一行为；E5/E6 已实测两条都执行。**若将来有人在 profile 里复用同一个 `id`**，会静默覆盖其中一条 —— 这是维护时需要知道的坑（无自动化守卫，见 6）。
6. **知识/文档欠账（均登记，不在本步）**：
   - `frontend/README.md` 的 `mvn -Pfrontend` 说明需提 JDK 21 → **S7 docs 清单**；
   - `SpringBootOnlyArchitectureTest` 可加一条「pom 主 `<build>` 含 `requireJavaVersion [21,22)`」的守卫断言 → **S6/S7 评估**（防止有人静默删除门禁）；
   - `maven-enforcer-plugin` 的版本字面量现在在**主 build 与 profile 两处重复**（`3.5.0`）→ 可选收敛为 property（纯清理，不阻塞）；
   - `docs/architecture/frontend-migration-contracts.md` §11/§8 的旧 JDK 11 / `jakarta` 禁令描述 → **S7**。
7. **容器取证依赖 S3 不被回退**：本步**没有**改 `Dockerfile`；若 S3 被整体 revert，本步的「镜像 JDK 21」证据随之失效（应作为「依赖未满足」上报，而不是在 S4 里补改镜像）。
8. **PR 前若再改文案**：`pom.xml` 的 `<message>` 与 `README.md` 的文字都不是编译/测试输入（消息只在规则失败时输出，README 不进 jar）。若在提交/PR 前还需微调这两段，**只需重跑 `mvn validate`（JDK 21 与 JDK 27 各一次）确认“通过/失败 + 消息”仍准确**，不必重跑 `verify`。

---

## 二、新增知识点 / 重点学习对象

### 2.1 Maven / 构建

**（1）同一插件在主 `<build>` 与 profile 中的 execution 合并**

- 含义：`<plugin>` 在 POM 里**按 `groupId:artifactId` 聚合**，其 `<executions>` **按 `<id>` 逐条合并**。主 `<build>` 声明 `maven-enforcer-plugin` + `require-jdk-21`，profile 里声明同坐标 + `require-frontend-dist`，激活 profile 时得到**两条都生效**的规则集，而不是「后者覆盖前者」。
- 位置：`pom.xml` 主 `<build><plugins>`（新增）与 `<profiles><profile><id>frontend</id>`（原有）。
- 学习价值：这是「往既有插件上追加行为而不破坏原行为」的标准做法；反过来，**两条 execution 用同一个 `<id>` 会静默覆盖** —— 命名要与语义绑定（本例用 `require-jdk-21` / `require-frontend-dist`）。

**（2）`<fail>true</fail>` 与 execution 绑定的「早期失败」位置**

- 含义：把规则绑到 `validate`（Maven 生命周期最早的业务阶段）而不是 `verify`，使错误 JDK 在**任何编译/测试开始前**就失败（1.4 s vs 整轮构建）。`fail=true` 让规则违例升级为构建失败而不是 WARN。
- 位置：`pom.xml` 的 `require-jdk-21` execution。
- 学习价值：「门禁放在最早能判定的阶段」能显著降低误用成本；也解释了为什么本步的 AC-2 用 `mvn validate` 就能取证。

**（3）`requireJavaVersion` 的区间语义 `[21,22)`**

- 含义：Enforcer 的版本区间是**数学区间表示**：`[` `]` 为闭区间、`(` `)` 为开区间。`[21,22)` = 「≥ 21 且 < 22」，因此 **21.x 全部通过，22.0 起全部拒绝**；它比较的是**运行 Maven 的那个 JVM 的版本**（即 `JAVA_HOME` 所选 JDK），不是 `maven.compiler.release`。
- 位置：`pom.xml` 的 `<version>[21,22)</version>`。
- 学习价值：这是「pin 一个主版本」最简洁的表达；若写 `21`（无区间）语义会变成「任意 ≥21」，若写 `[21,)` 会放进 22/27 —— 区间写法直接决定支持矩阵。

**（4）`maven.compiler.release=17`（编译目标）与 JDK 21（构建工具链）是两件事**

- 含义：`--release 17` 让 `javac`（可以是 JDK 21/27）只暴露 Java 17 的 API 与语言级别，产出 **class file major 61**；运行只需 **Java 17+**。而「构建需要 JDK 21」是**团队/CI/镜像的统一约定**（同一个 JDK 版本避免环境漂移），不是语言要求。
- 位置：`pom.xml` 的 `maven.compiler.release`（未改）＋ 新增的 enforcer 门禁；`README.md` 的 `JDK 21 is the build toolchain, not the language level`。
- 学习价值：把「构建工具链」与「目标字节码/运行环境」分开表述，能避免「README 说需要 21 → 用户以为 jar 不能在 17 上跑」这类误读，也解释了为什么两条都要写进文档。

**（5）可操作的失败消息设计**

- 含义：规则消息写清 **三件事**：① 被 pin 的版本（JDK 21）；② 为什么是这个版本（`release=17`；Boot 3.5 能跑 17–24 但项目只验证 21）；③ **怎么修**（`set JAVA_HOME to a JDK 21 installation - see README`）。并且注意 enforcer 的 `<message>` **只在规则失败时输出**（通过时日志只有 `Rule 0: … passed`），所以消息必须自带修复指引。
- 位置：`pom.xml` 的 `<message>`；`README.md` 的 `## Java toolchain`。
- 学习价值：门禁的价值取决于**失败时用户能否自助修复**；「将输出指向文档 + 文档写清命令」形成的闭环，比一条 `JDK 21 required` 有效得多。

### 2.2 容器 / 环境

**（6）容器内构建的 JDK 与宿主 `JAVA_HOME` 无关**

- 含义：`docker compose build app` 在 `maven:3.9-eclipse-temurin-21` 阶段里跑 Maven，用的是**镜像内的 JDK 21**，宿主的 `JAVA_HOME`（本例 JDK 27）完全不参与；因此新加的 enforcer 规则在容器里**自动通过**（E7 的构建日志里能看到 `require-jdk-21 … passed`），反过来宿主 `mvn` 会被同一规则拒绝。
- 位置：`Dockerfile:12`（`maven:3.9-eclipse-temurin-21 AS build`）与 `pom.xml` 的新 execution；对照 `Dockerfile:21`（`eclipse-temurin:21-jre AS runtime`）。
- 学习价值：这解释了「为什么本地默认 JDK 27 构建失败、`docker compose build app` 却成功」，也是「容器化 = 环境一致性的最后一道保险」在本仓库的具体体现；同时提醒：**CI 不构建 Docker**，容器里的门禁只能由自己跑 `docker compose build app` 证明。

**（7）容器验证的最小取证集**

- 含义：只验证不改的前提下，够用的取证是**四个可执行断言**：构建成功 → 启动后 health **200** → 容器内 `java -version` = **21.0.12.1** → `id` = **uid/gid 1001**（非 root）；最后 `docker compose rm -sf app` 清理并**不动 MySQL 容器/数据卷**。
- 位置：本日志 §0 E7；与 S3 日志的容器 smoke 互为补充（S3 证明业务流，本步只证明「镜像里的 JDK 仍是 21」）。
- 学习价值：把「未改动」也变成一条**可执行断言**（`docker exec … java -version` / `id`），比在 PR 里写「镜像没动」可信；清理范围（只删 app、不 `down` mysql）同样是可审计的约定。

---

## 结论

S4 已在分支 `codex/issue-49-jdk21-toolchain` 上实施并核验：`pom.xml` 主 `<build>` 新增 `maven-enforcer-plugin:3.5.0` 的 `require-jdk-21`（`validate` / `fail=true` / `requireJavaVersion [21,22)` / 指向 README 的可操作消息），`README.md` 新增 `## Java toolchain`（构建 vs 运行、`JAVA_HOME`、`mvn -version` 自检、IDE 提示、CI/镜像同 JDK 21、宿主 vs 镜像边界）。JDK 21 下 `validate` → **passed / BUILD SUCCESS**；JDK 27 → **BUILD FAILURE + 正确消息**（预期）；`verify` → **134 用例 / 0.9165 / major 61**；`-Pfrontend` 打包成功且两个 execution 都执行、dist 移走后仍 **BUILD FAILURE**；容器 health **200**、容器内 JDK **21.0.12.1**、非 root **1001**（S3 产物，本步只验证）。三路 review **无 Blocker**，采纳的 5 项文案已修正。**未提交、未推送、无 PR**，#49 仍 OPEN；`[21,22)` 拒绝 22+、IDE 内嵌 Maven、`frontend/README.md` 与契约文档同步等已登记为后续（S6/S7）。

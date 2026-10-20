# 工作流日志 / 迁移前基线记录 — Issue #46（S1：Record the pre-migration baseline and start the migration checklist）

> 本文件是 **S1（[#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)）的迁移前基线记录**，同时按 `AGENTS.md` 的要求承载本工作流的日志两大节：
> **一、修改了哪些文件及潜在问题**；**二、新增知识点 / 重点学习对象**（见文末）。
> **基线部分（元数据 / 测量记录 / 对比规则 / 非自动化覆盖）是本记录的核心交付物**：S2–S7 的所有“数量不下降”“覆盖率不下降”“e2e 不减少”判据都以本文件的数字为基准，**不得**改用更早的记录（如 2026-10-15 的日志）或事后重跑的数字。
>
> **口径声明**：下表所有测量数字均由**主 agent 在 2026-10-20 亲自运行并核验**；`docs-writer` **未**运行 `mvn` / npm / Docker / CI，只做了**只读**复核（`pom.xml`、测试文件数、`git status`）与文档写入，**未**执行任何 GitHub 写操作。

## 元数据

| 项目 | 值 |
| --- | --- |
| 记录日期 | **2026-10-20**（测量与记录同日） |
| Issue | **#46** — https://github.com/PNZ-z/ai-powered-Fabflix/issues/46（`[Step 1/7] Record the pre-migration baseline and start the migration checklist`，state **OPEN**，milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)） |
| 上游 epic | [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（epic，**OPEN**）；同期兄弟步骤 [#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)–[#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52) |
| 分支 | `codex/issue-46-baseline`（基于 `main`） |
| Commit SHA（测量基准） | **`fff314f88455216122912f07c26706f70dd63b62`**（短 `fff314f`）= `main` 上 PR [#45](https://github.com/PNZ-z/ai-powered-Fabflix/pull/45) 的合并提交（`Merge pull request #45 from PNZ-z/codex/ai-roadmap-planning`） |
| 提交 / 推送状态 | **未提交、未推送**：本轮只新增 `docs/` 文件，**无 commit、无 push、无 PR**；S1 **未合并**，远端 `main` 未受影响 |
| 树清洁度 | 测量前 `git status --short` → 仅 `?? .idea/`；**全部测量结束后仍仅 `?? .idea/`**（无 `pom.xml` / `src/**` / `frontend/**` / `.github/**` / `Dockerfile` 改动） |
| 后端工具链 | Apache Maven **3.10.0**；默认 `JAVA_HOME` = **JDK 27**（Homebrew vendor，runtime `/opt/homebrew/Cellar/openjdk/27/...`）；locale `zh_US_#Hans`；timezone `America/Los_Angeles` |
| JDK 21（复跑用） | Homebrew **keg-only** `/opt/homebrew/opt/openjdk@21`，`java -version` = **21.0.12.1**（主 agent 于 **2026-10-19** 经用户授权安装，**在 S1 之前**完成，见 §一 第 6 条） |
| 前端工具链 | Node **v24.16.0**；npm **11.13.0**；Playwright **1.64.1**（`@playwright/test`）；命令均在 `frontend/` 下执行 |
| 项目版本（只读复核） | `pom.xml`：`spring-boot.version=2.7.18`、`maven.compiler.source/target=11`、`jacoco.version=0.8.15`、`maven.surefire.version=3.5.2`；JaCoCo `<exclude>` **恰好 8 项**；`src/test/java` 共 **40** 个 `*Test.java`（与“40 个测试类”一致） |
| 关联计划 / checklist | 拆分总览 `docs/plans/issue-44-migration-steps.md`（§3 S1 行、§4-S1）；**新增** 7 步 checklist `docs/plans/issue-44-migration-checklist.md`（每步含目标 / 验证命令 / 回退点 / 完成分支 + 状态列） |
| 关联 issue 文档 | `docs/issues/issue-46-baseline.md`（#46 的规范来源与跟踪记录） |
| 证据来源 | 主 agent 于 2026-10-20 亲跑的终端输出（下文逐条转录）；`docs-writer` 只读复核（`git status`、`git log`、`pom.xml` 片段、`find src/test/java -name '*Test.java' \| wc -l` = 40、`grep -c '<exclude>' pom.xml` = 8） |

---

# 基线测量记录（S1 交付物）

## 0. 测量前的树状态与基准提交

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| 工作区状态（测量前） | `git status --short` | `?? .idea/`（**唯一**条目；`.idea/` 为 IDE 目录，未跟踪且与迁移无关） |
| 基准提交 | `git rev-parse HEAD` | `fff314f88455216122912f07c26706f70dd63b62` |
| 提交主题 | `git log --oneline -1` | `fff314f Merge pull request #45 from PNZ-z/codex/ai-roadmap-planning` |
| 分支 | `git branch --show-current` | `codex/issue-46-baseline` |
| 工作区状态（**全部测量结束后**） | `git status --short` | `?? .idea/`（**仍只有这一条** → 证明测量过程零源码/构建改动，满足 #46 AC-3） |

## 1. 工具链实际版本（`mvn -version` 原文要点）

| 项 | 值 |
| --- | --- |
| Apache Maven | **3.10.0** |
| Java version | **27**（vendor: Homebrew） |
| Java home / runtime | `/opt/homebrew/Cellar/openjdk/27/...` |
| Default locale | `zh_US_#Hans` |
| Platform encoding | `UTF-8` |
| OS name | macOS（`/usr/libexec/java_home` 与 `/Library/Java/JavaVirtualMachines` 存在；Homebrew 前缀为 `/opt/homebrew`） |

> 这是 **P2-4 / U-1 要求的“运行时 JDK 必须逐字记录”**：本地 Maven 默认跑 JDK 27，而迁移目标工具链是 **JDK 21**。

## 2. 后端基线 —— **默认 JDK 27**

```bash
mvn --batch-mode --no-transfer-progress verify
```

| 指标 | 结果 |
| --- | --- |
| 构建结果 | **BUILD SUCCESS** |
| 耗时 | 约 **8.871s**（`Total time`） |
| 用例 | `Tests run: 127, Failures: 0, Errors: 0, Skipped: 0` |
| 测试类数 | **40**（与 `src/test/java` 的 40 个 `*Test.java` 一致） |
| JaCoCo 闸门 | `All coverage checks have been met.` |
| JaCoCo `BUNDLE / LINE` | **968 covered / 110 missed / 1078 total = 0.8980**（闸门 `≥ 0.85`） |

## 3. JDK 21 可用性核查与**同 JDK 复跑**

### 3.1 可用性核查（含一个真实陷阱）

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| macOS 注册的 JDK | `/usr/libexec/java_home -v 21` | 输出 `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home` —— **系统未注册 21**（`/Library/Java/JavaVirtualMachines` 只有 11 / 17），故该命令**回落**到 17，**不能**作为“有无 JDK 21”的判据 |
| Homebrew keg-only JDK 21 | `/opt/homebrew/opt/openjdk@21/bin/java -version` | **21.0.12.1** → **JDK 21 可用** |
| 安装来源 | 主 agent 记录 | 2026-10-19 经**用户授权**由主 agent 安装（**不属于 S1 的动作**，S1 本身未安装任何 JDK；见 §一 第 6 条） |

### 3.2 JDK 21 复跑（**关键：与 JDK 27 数字完全一致**）

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify
```

| 指标 | 结果 |
| --- | --- |
| 构建结果 | **BUILD SUCCESS** |
| 用例 | `Tests run: 127, Failures: 0, Errors: 0, Skipped: 0` |
| JaCoCo `BUNDLE / LINE` | **968 covered / 110 missed / 1078 total = 0.8980** |

**结论**：**Boot 2.7.18 可以在 JDK 21 上完整跑完 `mvn verify` 且全绿**，且 127 用例 / 0.8980 与 JDK 27 完全一致。→ **JDK 21 基线可获得，S3/S6 无需 cross-JDK caveat**（但本文件同时保留两套原始数字，以便任何需要时对照）。

## 4. 前端基线（`frontend/`，Node v24.16.0 / npm 11.13.0 / Playwright 1.64.1）

| # | 命令 | 结果 |
| --- | --- | --- |
| 0 | `npm ci` | 成功（可选依赖安装无报错） |
| 1 | `npm run lint`（oxlint） | **无输出**（exit 0，零 warning） |
| 2 | `npm test`（vitest run） | **30 files / 195 tests passed** |
| 3 | `npm run test:coverage` | **Lines 92.35% (846/916)**；Statements **90.05%**；Branches **82.16%**；Functions **91.17%**（阈值 `lines ≥ 88` 通过） |
| 4 | `npm run build`（vite build） | 成功 |
| 5 | `npm run test:e2e`（Playwright chromium，360/1440 两档 viewport） | **12 passed**（6 用例 × 2 viewport） |

> 与 2026-10-15 记录（`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`）对照：后端的 127 / 0.8980 与前端 195（30 files）/ 92.35% / 12 e2e **数值一致**；差异仅在环境版本（Maven 3.10.0、JDK 27、Node 24.16.0）。**以本文件为 S2–S7 的唯一对比基准。**

## 5. 本轮**未执行**的检查（不得在任何后续步骤中假定通过）

| 项 | 状态 | 原因 |
| --- | --- | --- |
| `mvn -Pfrontend -DskipTests clean package` | **未执行** | 属 S3（AC-3/AC-4 的 jar 形态与 dist 守卫），S1 范围只要求后端 `verify` 与前端 5 条命令 |
| `docker compose build app` / `docker compose up -d --no-deps app` 生产 smoke | **未执行** | 属 S3/S6 |
| 真实 MySQL 读写 smoke（日期/`CURDATE()`/时区/SSL 警告） | **未执行** | 属 S2/S6 |
| CI（GitHub Actions 双 job） | **未执行** | 本轮无 PR |
| 真实浏览器刷新 / 视觉与对比度人工检查 | **未执行（自动化不可覆盖）** | 见下节 |

---

# 对比规则（S2–S7 必须遵守）

1. **唯一基准**：本文件的数字（后端 `127 / 0 / 0 / 0`、`968 / 110 / 1078 = 0.8980`；前端 `30 files / 195 tests`、`Lines 92.35% (846/916)`、`12 passed` e2e）是**迁移前基线**。任何后续步骤都**不得**改用 2026-10-15 的数字或“迁移开始后重跑”的数字来声称“不下降”。
2. **JDK 选择**：JDK 21 与 JDK 27 的基线**完全一致**（127 / 0.8980），因此 **S3 与 S6 默认在 JDK 21（`JAVA_HOME=/opt/homebrew/opt/openjdk@21`）下复跑并与本文件对比**，无需 `cross-JDK caveat`。
3. **记录两套数字的义务**：本文件同时保留 JDK 27 与 JDK 21 两套原始结果；若某步骤因环境原因只能用 JDK 27 取证，**必须**在证据中写明所用 `JAVA_HOME`，并说明“本步骤结果对应基线中的哪一套”（二者相同，故不构成偏离）。
4. **允许的合法差异**：依赖/缓存/机器负载导致的**耗时**差异不算回归；**用例数下降、覆盖率下降、e2e 数量下降、lint 出现告警、build 失败**才算。
5. **禁止的手段**：不得通过降低覆盖率阈值、扩大 JaCoCo `<exclude>`（现为 **8** 项）、跳过 e2e、跳过 lint 让后续门禁“变绿”。
6. **陈旧基线失效条件**：若在 S3 之前有人改动 `pom.xml`（S2 的驱动定版除外）或测试数量，则本基线**失效**，须由主 agent 决定是否重取（不得静默沿用）。

---

# 非自动化覆盖清单（自动化**不能**证明的部分）

以下项**必须**由人工或在后续步骤中显式标注“未执行”，**不得**写成“已验证通过”：

| # | 项目 | 为什么自动化覆盖不到 | 归属步骤 |
| --- | --- | --- | --- |
| 1 | **真实浏览器刷新后的 cart 持久化**（session/cookie 语义跨页面重载、`sessionStorage`/服务端 session 组合） | 组件测试与 e2e 均用 `page.route` mock API，刷新语义与真实 session 重建未被断言 | S6（或显式标注未执行） |
| 2 | **视觉 / 对比度检查**（深色块内错误/成功文案、响应式断点观感） | jsdom 无布局与颜色计算，`color-contrast` 规则在单测中被禁用；只有 axe 无法量化的观感需人工 | S6（人工） |
| 3 | **真实 MySQL 并发**（连接池上限、并发下单/库存竞争、死锁与重试） | 单测与 e2e 均 mock/单连接；`docker compose` smoke 是串行脚本 | S2 / S6 |
| 4 | **`add_movie` 存储过程真实往返**（员工创建影片 → 存储过程写库 → 前端可查） | 员工流程 e2e 的 API 全部被 mock；`add_movie` 的 SP 语义需真实 MySQL | S6 |
| 5 | **后端 `verify` 之外的打包/容器路径**（jar 内 `static/index.html`、两阶段镜像、health 200） | S1 范围只跑 `verify`；属 S3/S6 证据 | S3 / S6 |

---

## 一、修改了哪些文件及潜在问题

### 本次实际变更（**仅 `docs/`，零源码改动**）

| 文件 | 变更类型 | 目的 |
| --- | --- | --- |
| `docs/logs/2026-10-20-issue-46-baseline.md` | **新增**（本文件） | S1 的基线记录（元数据、逐条测量原始数字、对比规则、非自动化覆盖清单）+ AGENTS 要求的两大节总结 |
| `docs/plans/issue-44-migration-checklist.md` | **新增** | 7 步迁移 checklist：每步含 (a) 一句话目标、(b) 精确验证命令、(c) 回退点、(d) 完成分支/commit + 状态列；顶部链接 epic #44 与 #46–#52 |
| `docs/plans/issue-44-migration-steps.md` | **更新（2 处）** | ① §3 总表的 **S1 行**补状态“基线已测量（2026-10-20）”并指向本记录与 checklist；② §11 交付物表新增 checklist 行（其余内容未改） |
| `docs/issues/issue-46-baseline.md` | **更新（状态块）** | 补记“S1 基线已测量并记录（2026-10-20），待提交/PR”；**不**声称 issue 完成或已合并 |

**未改动**（显式确认）：`pom.xml`、`src/**`、`frontend/**`、`.github/workflows/ci.yml`、`Dockerfile`、`compose.yaml`、`AGENTS.md`、`.pi/agents/**`、任何历史日志（`docs/logs/2026-10-14-*`、`2026-10-15-*` 未触碰）。

### 潜在问题与残余风险

1. **基线会随环境漂移**：本记录的 127 / 0.8980 / 195 / 92.35% / 12 是 **2026-10-20 + Maven 3.10.0 + JDK 27/JDK 21 + Node 24.16.0** 的快照。后续若某人升级 Maven/Node/JDK 补丁或依赖解析结果，重跑数字可能合法变化——**对比必须以本文件为基准并记录环境**，不得因“今天跑出 128 个用例”就宣布改进或回退。
2. **存在两套后端数字**（JDK 27 与 JDK 21）：目前二者完全一致（127 / 0.8980），但**两套都保留**意味着后续若出现不一致，需要先查环境而不是查代码。已在“对比规则”第 3 条写明记录义务。
3. **JDK 21 是 Homebrew keg-only 安装，不在 macOS 注册列表中**：`/usr/libexec/java_home -v 21` 会**错误地**返回 JDK 17。任何脚本若用 `java_home` 判断“JDK 21 存在性”会得出错误结论 → checklist 中所有 JDK 21 命令一律写**显式 `JAVA_HOME` / `PATH` 前缀**，不依赖 `java_home`。
4. **`?? .idea/` 未被跟踪**：测量期间工作区并非“绝对干净”（有一个未跟踪的 IDE 目录）。这不影响测量有效性（`.idea/` 不参与构建），但提交时必须**只 add `docs/` 下的文件**，避免把 `.idea/` 带入提交。
5. **S1 尚未完成其交付闭环**：本记录只是“测量 + 文档”，**提交、推送、PR、CI 绿、合并**均未发生；因此 #46 的正确状态是“**基线已测量，待提交/PR**”，**不是** closed。checklist 中 S1 行写“基线已测量（待 PR 合并后标记完成）”正是为此。
6. **JDK 21 的安装发生在 S1 之前，且经用户授权**（2026-10-19，主 agent 记录）：拆分总览 **P1-5** 规定“S1 不安装 JDK、安装需主 agent 批准”。事实是：安装在 **S1 开工前一天**由主 agent 在**用户已授权**的前提下完成，**S1 本身未安装任何 JDK**。此偏离已在本文与 checklist 中显式记录，供 reviewer 核对是否与 P1-5 的意图（“S1 不得擅自扩权安装”）一致。
7. **未执行项的风险**：见上文“本轮未执行的检查”与“非自动化覆盖清单”——`-Pfrontend` 打包、Docker smoke、真实 MySQL、CI 全部**未在 S1 取证**，S3/S6 不得引用 S1 声称这些已通过。
8. **未发现其他已知遗留问题**：本轮为纯文档新增/更新，无构建、运行时或 CI 行为变化；回退方式 = 删除/还原本 PR 的 4 个 `docs/` 文件。

---

## 二、新增知识点 / 重点学习对象

### Java / 构建工具链

- **Boot 2.7.18 + Spring Framework 5.3 可以在 JDK 21 上编译并跑完全部测试**
  - 含义：许多资料认为 Boot 2.x 不支持 JDK 21（ASM/CGLIB/byte-buddy 对 class file 65 的支持是常见断点）。本仓库实测：在 `JAVA_HOME=/opt/homebrew/opt/openjdk@21` 下 `mvn verify` **BUILD SUCCESS**、127 用例全绿、覆盖率与 JDK 27 完全一致（0.8980）。可能原因：`maven.compiler.source/target=11`（只编译到字节码 55）+ `surefire 3.5.2`、`mockito 5.23.0`、`byte-buddy 1.17.7` 均为较新版本，具备 JDK 21 支持。
  - 位置：`pom.xml`（`maven.compiler.source/target=11`、`maven.surefire.version=3.5.2`、`mockito.version`、`byte-buddy.version`）；证据见本文 §3.2
  - 学习价值：把“能否在 JDK X 上运行”从**信念**变成**测量**；同时说明“编译目标版本（`source/target`）”与“运行 JDK”是两个独立维度——这正是 S3 要把 `source/target` 换成 `release=17` 的原因（`release` 会同时约束 API 可见性）。
- **`JAVA_HOME` / `PATH` 前缀覆盖 Maven 的工具链选择**
  - 含义：`mvn` 启动脚本优先使用 `JAVA_HOME` 指定的 JDK，而不是 `java` 在 `PATH` 中的顺序；一次性的 `VAR=... cmd` 前缀写法（`JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH mvn ...`）只在**本条命令**生效，不改 shell 环境——这是“同一台机器两套 JDK 取证”的安全做法。
  - 位置：本文 §3.2 的命令；S3/S4 的 DoD 均要求“AC 证据写明所用 `JAVA_HOME`”
  - 学习价值：Maven 工具链（toolchains.xml）之外的“临时 JDK 切换”最轻量手段；也是避免误用默认 JDK 27 导致“字节码不是 61 / enforcer 误报”的关键操作纪律。
- **macOS `/usr/libexec/java_home -v N` **不是**“是否安装了 JDK N”的判据**
  - 含义：`java_home` 只认识 `/Library/Java/JavaVirtualMachines`（及少数已注册位置）下的 JVM；Homebrew 的 `openjdk@21` 是 **keg-only**、不注册到系统，因此 `java_home -v 21` **静默回落**到已注册的最高版本（本机 → JDK 17），**不报错**。真实判据是直接执行 `/opt/homebrew/opt/openjdk@21/bin/java -version`。
  - 位置：本文 §3.1；`docs/issues/issue-46-baseline.md` 正文 Scope 里给出的“`/usr/libexec/java_home -v 21` 或等效检查”
  - 学习价值：一个“命令成功但结论错误”的经典陷阱——**退出码 0 不代表语义正确**；写自动化检查时必须验证输出内容而非仅看 exit code。
- **JaCoCo 的覆盖率行数来自 `surefire` 的执行数据汇总，`BUNDLE` 是“整个模块”口径**
  - 含义：`jacoco-maven-plugin` 的 `prepare-agent` 把 agent 挂到 surefire fork 的 JVM 上，`report` 阶段汇总 `target/jacoco.exec` 生成 `target/site/jacoco/jacoco.csv/.xml`；`BUNDLE / LINE` 一行 = **全模块所有类**的 covered/missed 合计，而不是各测试类的平均。本仓库的闸门配置为 `BUNDLE / LINE / COVEREDRATIO ≥ 0.85`，实测 968/1078 = **0.8980**。
  - 位置：`pom.xml`（jacoco 插件 + `excludes` 8 项）、`target/site/jacoco/jacoco.csv`（`LINE_MISSED` / `LINE_COVERED` 列）
  - 学习价值：理解“覆盖率是加总而非平均”才能预判一个 PR 会不会把闸门压破（新增 N 行无测试代码 ≈ 分母 +N）；也解释了为什么删代码可能**提高**覆盖率。

### 前端 / 测试

- **`npm ci` 与 `npm install` 的差异在基线场景下很关键**
  - 含义：`npm ci` 严格按 `package-lock.json` 安装、删除已有 `node_modules`、**不修改** lock 文件，因此基线可复现；`npm install` 可能升级满足 semver 范围的补丁版本并改写 lock，让“基线”随日期漂移。
  - 位置：`frontend/package-lock.json`、`frontend/package.json`（`"vitest": "^5.0.3"`、`"@playwright/test": "^1.64.1"`、`"oxlint": "^1.81.0"`）
  - 学习价值：基线/CI 都必须用 `npm ci`；否则 195 个用例、92.35% 行覆盖都不再是可比较数字。
- **Playwright 的“6 用例 → 12 次执行”是 `projects`（viewport）造成的**
  - 含义：`npm run test:e2e` 输出的是**执行次数**（test × project），不是用例数。本仓库配置两个 chromium project（360 / 1440），故 6 个用例 → **12 passed**；“12 不下降”的判据必须理解为“12 次执行”，若有人新增 project 或改 viewport，数字会变。
  - 位置：`frontend/playwright.config.js`；`docs/architecture/frontend-migration-contracts.md`（e2e 口径）
  - 学习价值：报告“e2e 数量”时区分“用例数/执行次数”，避免后续步骤用错误的基数宣称“未减少”。

### 工程习惯

- **“先记录，再迁移”的基线纪律**：基线的价值不在于数字本身，而在于**同一环境下可比**。本记录同时固定了 commit SHA（`fff314f`）、Maven/JDK/Node/npm/Playwright 版本、locale/timezone，以及“未执行清单”——少了任何一项，后续的“不下降”都可以被质疑为环境噪声。
- **负向证据（“未执行”也要写）**：本文件与 checklist 都显式列出**未运行**的检查（打包、Docker、真实 MySQL、CI）。在迁移类工作中，“漏写未执行”比“写错一个数字”更危险，因为它会被下游当成已验证。

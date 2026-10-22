# 工作流日志 — Issue #52（S7 / 7：收尾 —— 架构约定、README 核对、路线图状态与工作流日志）

> **本文件的定位**：Step 7/7（收尾）的工作流日志，按 `AGENTS.md` §8 的要求分成**两大分区** —— §1「修改了哪些文件及潜在问题」与 §2「新增知识点 / 重点学习对象」。
> **S7 是纯文档步骤**：不改源码、测试、`pom.xml`、`ci.yml`、`Dockerfile`、`compose.yaml`、`frontend/**`。`README.md` 与 `frontend/README.md` 的措辞对齐由 **`worker`** 完成（`docs-writer` 不编辑 `docs/` 之外的文件）；本文件负责 `docs/` 下的文档与跟踪记录，并**如实记录** README 的实际 diff（见 §1.1 第 8 行）。
> **不声称本步已合并**：~~分支尚未提交/推送，**无 PR**；epic #44、milestone #3、issue #52 均仍 **OPEN**（见元数据）。~~ → **2026-10-21 状态回填**：**S7 已随 PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) 合并（merge `c1067f7`），#52 已 CLOSED；epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) 已由主 agent 关闭（CLOSED / reason=completed，完成评论 [`issuecomment-6334171353`](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353)）；当前 open issues = 0；milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) 保持 OPEN（`open_issues=0` / `closed_issues=8`）。**

## 元数据

| 项目 | 值 |
| --- | --- |
| 日期（主机时区） | **2026-10-21**（`date +%F` = `2026-10-21`，PDT） |
| GitHub issue | **[#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52)** `[Step 7/7] Close out the migration: architecture docs, roadmap status and workflow log` —— state **CLOSED（2026-10-21 回填：随 PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) @ `c1067f7` 合并后关闭）**（创建于 2026-10-19；正文含 `Part of #44`） |
| 上游 epic | [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**2026-10-21 回填：已 CLOSED / reason=completed**；完成评论 [`issuecomment-6334171353`](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353)，`closedAt` = `2026-10-22T06:17:55Z` UTC；远端 `## Steps` 7/7 勾选）；milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)（**仍 OPEN**，`open_issues=0` / `closed_issues=8`，供后续 B/B-API/C） |
| 拆分与清单 | `docs/plans/issue-44-migration-steps.md`（§4-S7）、`docs/plans/issue-44-migration-checklist.md`（§S7） |
| 分支 | **`codex/issue-52-close-out`** |
| 基线 commit | **`b4a6d2a`** = `Merge pull request #59 from PNZ-z/codex/issue-51-regression-proof`（= 已合并 S6 的 `main`） |
| 提交 / 推送 / PR | **2026-10-21 回填（已发生）**：[PR #60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) `Close out the Option A migration` 已 **MERGED**（`mergedAt` = `2026-10-22T06:14:50Z`），merge commit = **`c1067f7`**（= `main` 最新 merge）。**S7 工作期内的历史事实**：分支 `codex/issue-52-close-out` 提交前 `git status --short` 仅有未跟踪 `?? .idea/`（不属本次改动，不得带入提交）；工作区 = **8 个已跟踪文件**（`docs/` 6 个 + `README.md` + `frontend/README.md`）+ **1 个未跟踪日志**（`docs/logs/2026-10-21-issue-52-close-out.md`） |
| 改动规模 | **源码/配置/测试/CI/`Dockerfile`：0 行**；改动 = `docs/` 下 **7 个文件**（6 个已跟踪 + 1 个新建日志，见 §1.1 第 1–7 行）+ **`README.md` / `frontend/README.md` 的 4 处文本（`worker`，第 8 行）** |
| 执行角色 | 本步的核对命令与 diff 判断由 `docs-writer` 在只读范围（`git`/`grep`/`awk`/`sed`）内完成；实际构建/测试/容器命令**本步未运行**（引用 S6 证据） |
| S1–S6 合并事实（本轮引用） | S1 [#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)→PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)（`9696848`）· S2 [#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)→PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)（`55f8d79`）· S3 [#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)→PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56)（`70fb78b`）· S4 [#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49)→PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)（`9a7705c`）· S5 [#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)→PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)（`ff57e79`）· S6 [#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)→PR [#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59)（`b4a6d2a`）；**#46–#51 均已 CLOSED，`main` CI 均 SUCCESS**（取自主 agent 核验结论 + `git log --merges` 只读复核） |
| 引用的验证证据 | **`docs/logs/2026-10-21-issue-51-regression-proof.md`**（S6：后端 134 / 988-90-1078 = 0.9165 @ 0.90；前端 30 files / 195 tests / Lines 92.35% / 12 e2e；生产 smoke：10 深链 200、401/404、SPA fallback、`changeSessionId`、员工 logout 204 且保留 `customer` session、`order_date` = `sales.sale_date`、容器 JDK 21 / 非 root 1001、无 SSL/致命告警；**§0.7 未执行清单**） |
| 结论 | 文档与实现一致（可机器核对）；README 漂移由 `worker` 修正 4 处文本（**含 `README.md` 首段事实、前端 90% 闸门说明**；`docs-writer` 未编辑这两个文件）；**S7 已随 PR #60 合并（`c1067f7`）**，**epic #44 已 CLOSED（2026-10-21，reason=completed）**，**open issues = 0**，**milestone #3 保持 OPEN** 供后续 B / B-API / C；**Option A 迁移至此完成**。残余/未覆盖项见 §1.3 |

---

## 1. 修改了哪些文件及潜在问题

### 1.1 实际改动（逐文件组）

> 第 1–7 行是 `docs-writer` 编辑的（均在 `docs/` 内）；**第 8 行是 `worker` 编辑的**（`README.md` / `frontend/README.md`，在 `docs/` 之外）：本文件**只记录其 diff 事实**，不代替它做验收。源码、测试、`pom.xml`、`ci.yml`、`Dockerfile`、`compose.yaml` **均未改**。

| # | 文件 | 改了什么（目的） |
| --- | --- | --- |
| 1 | `docs/architecture/frontend-migration-contracts.md` | **§8 重写（语义反转）**：删除「源码中不得出现 `jakarta.servlet`」的过期表述，改为 **11 个路径的 allow-list + 集合相等**（列出全部 11 个相对路径、按包分组 2/2/5/1/1、说明"新增/删减文件都必须同步改白名单"），并写清**仍然禁止**的写法（`javax.servlet`、`jakarta.servlet.annotation.WebServlet`、`@WebServlet`、`extends HttpServlet`）与 `pom.xml` 断言（`jar` + `repackage` + **禁 `jakarta.servlet-api` 与 `javax.servlet-api` 两个字面量**）。**§11 容器约定**：两个 JDK 阶段 `temurin-11` → **21**（附"JDK 11 无法编译 `release=17`、无法加载 major 61"的原因与历史注记），非 root 1001 / `mysql:3306` / `SPRING_PROFILES_ACTIVE=local` / SPA fallback 约定**原样保留**。**§12** 新增「Java 版本约定」小节（本地/CI/镜像统一 JDK 21、`release=17`、major 61、enforcer `[21,22)`、CI job `Build and test (Java 21)`）。**§7** 增加 2026-10-21 复核条目（确认**无** "~106 行"，口径 = 90 行未覆盖 / 最多再新增 ≈19 行）。文首新增「本轮（2026-10-21，#52）更新摘要」。 |
| 2 | `docs/plans/ai-roadmap-overview.md` | 顶部新增「**执行状态（2026-10-21）**」行（Option A **已实施完成**：S1–S6 合并 + PR 编号/merge SHA 逐一列出，S7 收尾中，**B/B-API/C 未开始**，epic #44 与 milestone #3 仍 OPEN）；元数据表新增「执行状态」行并更新「状态 / GitHub」两行；§2.6 现状与结论、§6 里程碑注与 **M3 行 DoD 回填**、§12.3 的 A-3/A-4 行同步。**历史证据与已批准决策未改写**（见 §1.3 第 4 条）。**2026-10-21 关闭回填**：执行状态行/元数据「执行状态」行改为「**Option A 已完成（epic #44 CLOSED，2026-10-21）**；下一步 Option B/B-API 未开始；milestone #3 保持 open」 |
| 3 | `docs/plans/issue-44-migration-checklist.md` | 顶部事实段与状态总表：**S6 → 已完成（PR #59 @ `b4a6d2a`，#51 CLOSED）**、**S7 → 待 PR**；S7 小节 **(d) 填分支 `codex/issue-52-close-out` 与基线 `b4a6d2a`**，并补「实际结果」表（§8/§11/§12/§7 的复核命令与结果、路线图、**README 漂移由 `worker` 修正的 4 处**、日志、未执行项）。**2026-10-21 关闭回填**：S7 → **已完成（PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) @ `c1067f7`，#52 CLOSED）**，顶部新增「S1–S7 全部完成 / epic #44 CLOSED / open issues = 0 / milestone #3 open」的总状态行，S7 的 (d) 与「实际结果」表同步为已合并 |
| 4 | `docs/plans/issue-44-migration-steps.md` | 顶部「实施进展 / 更新说明」、元数据表（子 issue 状态、剩余待办）、§4-S6 第 8 条（追加 PR #59 合并的闭合注记）、**§4-S7 新增「实际结果」8 条回填**（含 6 条机械核对命令、未执行项、无范围偏离）。 |
| 5 | `docs/issues/issue-52-close-out.md` | 顶部状态行由「未实施」改为 **已实施（2026-10-21），待提交/PR**（含分支、基线、diff = `docs/` + 两份 README 的 4 处文本）；Internal tracking 的「状态 / 依赖关系 / 事实来源 / 未确认」4 行回填；文末新增 **「实施结果与 AC 回填（2026-10-21）」**（执行状态表 + 核对命令 + **AC-1..AC-7 逐条状态** + 「仍开放」3 条）。**`## GitHub issue body` 原文逐字未改。** |
| 6 | `docs/issues/issue-44-boot3-java17-jakarta-migration.md` | S7 工作期：进展块与状态行补 **S6 已合并（PR #59 @ `b4a6d2a`，#51 CLOSED）**、**S7 待 PR**；子 issue 状态改为「#46–#51 已合并并 CLOSED，仅 #52 仍 OPEN」。**2026-10-21 关闭回填：S7 已随 PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) @ `c1067f7` 合并、epic #44 已 CLOSED（reason=completed，完成评论 `issuecomment-6334171353`，`## Steps` 7/7 勾选），并新增「epic 关闭记录」表；`## GitHub issue body` 原文本区未改**（与远端 `- [x]` 的差异已显式登记） |
| 7 | `docs/logs/2026-10-21-issue-52-close-out.md` | **本文件**：S7 的元数据 + 两大分区（AGENTS §8 / issue Scope 要求），用 **S7 自己的 issue 编号（#52）**，不占用 epic #44。 |
| (8) | `README.md`、`frontend/README.md` —— **`worker` 编辑（非 `docs-writer`）** | 4 处文本对齐（`git diff README.md frontend/README.md` 可核）：① `README.md:3` 首段把交付事实写清 —— **`Spring Boot 3.5`** + **`React 19`** + Vite（原文只写“Spring Boot”“React”）；② `README.md:159` 新增一句：**前端由 CI 的 `frontend` job 单独把关**（`npm run test:coverage` 低于 `frontend/vitest.config.js` 的 **90% lines** 阈值即失败）；③ `frontend/README.md:51` 把覆盖率阈值措辞改为 **90% `lines` threshold**；④ `frontend/README.md:75` 新增「`mvn -Pfrontend` 是普通 Maven 构建，需求 **JDK 21** + `JAVA_HOME`」并链回根 `README.md#java-toolchain`（**即 checklist S4 遗留项的闭合**）。**`docs-writer` 未编辑这两个文件**（写入边界），本条仅记录事实。 |

**核对开关（本轮 `docs-writer` 实际运行、可重跑）**

| 命令 | 结果 | 对照的文档断言 |
| --- | --- | --- |
| `git log --oneline -5` / `git branch --show-current` | `b4a6d2a Merge pull request #59 …`；分支 `codex/issue-52-close-out` | 基线 = S6 合并后的 `main` |
| `grep -rl "jakarta.servlet" src/main/java \| wc -l` | **11** | §8 的「11 个文件」 |
| `awk '/JAKARTA_SERVLET_ALLOW_LIST = Set.of\(/,/\);/' … \| grep -c '\.java"'` | **11** | §8 的「11 条白名单、集合相等」 |
| `grep -n "servlet" pom.xml` | **0 匹配**（`jakarta.servlet-api` 与 `javax.servlet-api` 都不出现） | §8 的 `pom.xml` 断言 |
| `grep -n "^FROM" Dockerfile` | `node:22-alpine` / `maven:3.9-eclipse-temurin-21` / `eclipse-temurin:21-jre` | §11 的「两个 JDK 阶段 = 21」 |
| `grep -n "java-version\|name: Build and test" .github/workflows/ci.yml` | `name: Build and test (Java 21)`、`java-version: "21"` | §12 的 CI JDK 21 |
| `grep -n "maven.compiler.release" pom.xml` | `17`（`pom.xml:13`） | §12 的 `release=17` → major 61 |
| `grep -n "Java 11\|temurin-11\|Boot 2\|2.7.18\|JDK 11" README.md frontend/README.md` | **无命中** | AC-3：worker 修改后的两份 README 里**没有被写回旧事实**（旧事实只存在于历史日志/计划文档） |
| `grep -n "106" docs/architecture/frontend-migration-contracts.md` | **无命中** | §7：无 "~106 行" 陈旧句子 |
| `grep -n "JDK 21\|JAVA_HOME\|90%" frontend/README.md` | `frontend/README.md:51`（90% `lines` 阈值）、`:75`（`mvn -Pfrontend` 需 JDK 21 + `JAVA_HOME`） | checklist S4 遗留的「`frontend/README.md` 需提 JDK 21」**已闭合** |

### 1.2 明确的边界（本步**没有**做的事）

- **未运行**任何 `mvn`、npm、Docker、CI 命令。本步不重验 S6 的结论，只引用其证据；因此**不得**把本日志读作「构建/测试/容器在本轮再次通过」。
- **未修改**（且按范围不得修改）：任何 `src/**`、`frontend/**`（含 `frontend/README.md` 中的代码目录）、`pom.xml`、`.github/workflows/ci.yml`、`Dockerfile`、`compose.yaml`、`.pi/agents/**`、`AGENTS.md`；**`README.md` 与 `frontend/README.md` 的文本改动由 `worker` 完成，不属 `docs-writer` 的写入范围**。
- **未做 GitHub 写操作**：`docs-writer` 在 S7 工作期未创建/编辑/关闭 issue、未改 milestone、未建 PR；**关闭与合并由主 agent / `github-operator` 执行**（2026-10-21 回填结果：PR #60 MERGED @ `c1067f7`、#52 CLOSED、#44 CLOSED/completed、open issues = 0、milestone #3 仍 OPEN）。
- **未改历史记录**：`docs/logs/2026-10-14-*`、`2026-10-15-*`、`2026-10-20-*`、`2026-10-21-issue-51-*` 等历史日志，以及已关闭 issue #30–#33 的计划/issue 文档，**逐字未动**（即便其中仍含 `Build and test (Java 11)` 或收紧前的 `0.85 / 88` 数字）。
- **未改写路线图的历史证据与已批准决策**：`docs/plans/ai-roadmap-overview.md` §2 的现状事实表、§4 的版本对比与估算、§11 的决策表、§13 的未确认事项均保持原样（其中仍会出现 Java 11 / `temurin-11` —— 那是**迁移前的实测基线**，属于历史证据）。

### 1.3 仍存在的风险、边界限制、未验证项与后续事项

1. **S6 的未执行项原样保留（不因 S7 完成而消失）** —— 真实浏览器多标签/并发、视觉/对比度（jsdom 无布局）、真实 MySQL 并发与连接池语义、生产 reCAPTCHA（`GoogleRecaptchaVerifier` 覆盖 0/17，且 Boot 3 下 `RestTemplateBuilder.setConnectTimeout/setReadTimeout` 弃用仍未消除 → 「构建零弃用」不成立）、员工 `?action=add_movie` 存储过程真实往返（`AdminRepository` 覆盖 **0/39**）、跨时区/跨日边界（主机 2026-10-21 PDT vs 容器/MySQL UTC 2026-10-22 的日期一致性**只在同一时区口径下**被证明）。**关闭 #44 时必须引用该清单，不得写「全部验证通过」。**
2. **文档断言的可验证性边界**：§8/§12 的结论可被 `grep`/`awk` 机械复核；但「jar 内静态资源可被服务」「深链返回 HTML」仍只有**生产 smoke** 能证明（§11 的「边界（不得声称）」一节保留原口径）。本步**没有**重跑 smoke。
3. **S7 自身已合并（2026-10-21 回填）**：PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) **MERGED**（merge `c1067f7`，`mergedAt` = `2026-10-22T06:14:50Z`），`main` CI **SUCCESS**（主 agent 核验），**#52 已 CLOSED**。**epic #44 已随之关闭**（CLOSED / reason=completed，完成评论 [`issuecomment-6334171353`](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353)，远端 `## Steps` 7/7 勾选）；**当前 open issues = 0**；**milestone #3 保持 OPEN**（`open_issues=0` / `closed_issues=8`）→ **Option A 迁移完成**。全文其余风险（第 1、2、4–8 条）**不因关闭而消失**。

4. **有意不改的历史/方案文档**（后续若要统一口径需主 agent 决策，不属本步范围）：`docs/plans/ai-option-a-boot3-migration.md` 仍按**迁移前**状态描述 CI `Build and test (Java 11)`、`temurin-11` 镜像与「守卫禁止 `jakarta.servlet`」（它是**已批准方案的历史记录**）；`docs/plans/issue-44-migration-steps.md` 的 S7 DoD ① 仍写着 0.85 时代的「110 行未覆盖 / ≈60 行」旧口径（本轮已在「实际结果」里更正为 **90 / ≈19**，但**未回改 DoD 原文**，以免重写已批准计划）。
5. **远端 #44 正文的门槛数字仍未同步**（远端写 0.85 / 88，本地 body 为 0.90 / 90）：历史遗留项，需主 agent 授权 `github-operator` 编辑远端；本步不做 GitHub 写操作。
6. **dev 数据副作用**：S2–S6 的 smoke 在 dev 库 `sales` 留下 **13562–13567** 行（`database/dev-seed.sql` 只在数据卷首次初始化时执行），未清理；本次文档明确记录为"dev 数据"，避免被误读为夹具。
7. **路线图后续项仍未立项（已知缺口）**：`B / B-API / C` 三个方向均**仍无 issue**（open issues = 0；milestone #3 为后续保留而保持 open）；下一里程碑 = **M4.1（Option B-① 电影目录语义检索）**，其前置（chat 与 embedding 模型可用、中文 embedding 实测）仍未开始。**已知覆盖缺口（不因 #44 关闭而消失）**：`AdminRepository` **0/39**、`GoogleRecaptchaVerifier` **0/17**，真实浏览器多标签/并发/视觉一致性/连接池语义**未自动化**。
8. **README 的核对边界**：`docs-writer` 只做**静态核对**（`grep`）并交由 `worker` 落地修改（本步实际改了 4 处，见 §1.1 第 8 行）；本轮**没有**实跑 `mvn -version` / `mvn verify` / `mvn -Pfrontend clean package` 去逐条复验 README 的命令文字 —— 该复验由 S6 承担（其证据记录 §0.6 已记载 `mvn -version` = Java 21.0.12.1 与 major 61）。**且 README 的命令层改动本身不会在 `docs`-only PR 的 CI 里被执行。**

> **已知遗留问题结论（明确写出，不留空）**：`docs-writer` 负责的 7 个文件均通过机械核对，**未发现需回退的文档缺陷**；README 的 4 处改动由 `worker` 执行，本文件只能确认它们与 `pom.xml`/`vitest.config.js`/`Dockerfile` 的**静态**一致性（`grep` 级），**不构成构建级验收**。**上文 1–8 项属于既有残余风险与未验证项，逐条保留，不视为"零风险"。**

---

## 2. 新增知识点 / 重点学习对象

> 只记录本次真正新增或显著涉及的**文档/工程核对**知识点；通用的 Spring/React 常识不重复罗列。

### 2.1 工程技术（迁移收尾的"文档对齐"方法）

| 知识点 | 含义 | 位置 | 为什么值得学 |
| --- | --- | --- | --- |
| **收尾 ≠ 写新内容，而是"让文档可被机器证伪"** | 把每条结论改写成能被一条命令验证的形式（计数、集合、镜像标签、locale job 名），文档评审才能从"读起来对"变成"跑一下就知道" | `docs/architecture/frontend-migration-contracts.md` §8/§11/§12 与 §1.1 的核对表 | 迁移类 PR 的最大风险是**文档悄悄失真**；可证伪的表述让失真在 review 阶段暴露 |
| **allow-list 的"集合相等"为何强于"包含"** | `assertEquals(expectedSet, foundSet)` 同时抓两个方向：**新增文件滥用 API**（白名单外）与**文件迁移后遗留项**（白名单内多余）；`containsAll` 只抓前者 | `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java`（`onlineApplicationConfinesJakartaServletApiToTheReviewedAllowList`） | 架构守卫的本质是"变化必须显式化"；集合相等把"无意扩散"变成编译期之外的硬失败 |
| **工具链一致性（本地 / CI / 镜像）与"必须先证明的中间量"** | `release=17` 是**语言级别**，JDK 21 是**构建工具链**，两者不能混说；字节码 major 61 是"确实产出了 Java 17 字节码"的唯一硬证据 | `pom.xml:13`（release）+ enforcer `requireJavaVersion [21,22)`；`ci.yml:20,32`；`Dockerfile:12,21`；契约文档 §12 | 迁移中"本地绿、CI 红"的根源往往是三处版本漂移；用 enforcer + 文档写死同一口径，才能让差异可归因 |
| **镜像阶段升级的连锁条件** | JDK 11 的 `javac` 无法编译 `release=17`，JDK 11 JRE 也无法加载 major 61 —— 所以镜像升 21 必须与 `release=17` **同批**交付，而不能留到"后续清理" | `Dockerfile`（S3 的两行 `FROM`）；契约文档 §11 的"历史（已失效）"注记 | 学会从"产物能否被运行"倒推交付边界，避免出现"CI 绿但 `docker compose build` 坏掉"的假绿 |
| **路线图/跟踪文档的"状态回填"与"历史保留"的边界** | 只更新**状态/进度**（合并 PR、SHA、AC 证据指向），不改**已批准决策**与**当时的事实快照**；配以显式 `~~删除线~~ → 更新` 或"2026-10-21 更新"前缀 | `docs/plans/ai-roadmap-overview.md` 顶部执行状态行、§6 M3 行、§12.3 A-3/A-4；`docs/issues/issue-44-*.md` 进展块 | 项目要求"历史日志不追改"，但活文档必须反映现状；用"追加而非覆盖"的写法同时满足两者，也让后来的读者能还原时间线 |
| **证据链闭合（evidence chain）的三要素** | ① 命令 + 输出摘要；② executed / not executed（含原因）；③ 指向唯一 log 文件。S7 只**引用** S6 的命令结果，不复制、不重跑、不改写 | `docs/logs/2026-10-21-issue-51-regression-proof.md`（S6）→ 本文件（S7）→ `docs/issues/issue-52-close-out.md` 的 AC 回填 | 关闭 epic 时最容易被"总结性文字"掩盖的就是"到底跑没跑"；把证据固定在日志 + AC 表，关闭动作才可被审计 |

### 2.2 工具与语法

| 知识点 | 含义 | 位置 | 为什么值得学 |
| --- | --- | --- | --- |
| `awk '/START/,/END/' file` 区间抽取 | 按"起始行到结束行"切出源码片段，再交给 `grep -c` 计数，可避免把整文件里其它 `.java` 字符串算进来 | 本日志 §1.1 的「白名单条目 = 11」核对命令 | 校验"集合条数"这类断言时，比手数或用 `grep -c` 扫全文件可靠 |
| `grep -rl … \| wc -l` 与 `grep -n … \|\| echo "0 matches"` | 前者数**文件**（不是匹配行），后者在"期望无匹配"时把退出码 1 变成可读输出，避免误判为命令失败 | 同上 | 守卫类断言天然有"必须为空"的检查（如 `pom.xml` 无 servlet 字面量），需要处理 `grep` 的退出码语义 |
| Markdown 表格内的 `\|` 转义 | 即使管道符在行内代码（`` ` ``）里，GFM 表格仍会把它当单元格分隔符，必须写成 `\|` | 本文件 §1.1 与 checklist 的实际结果表 | 文档里的命令一旦被表格切断，读者复制后命令就是错的 |
| `git log --merges` / merge commit 作为"合并事实"的**唯一**可信来源 | PR 是否合并看 `main` 上的 `Merge pull request #N` 提交（而不是 PR 页面文字或口头结论） | 基线 `b4a6d2a`；S1–S6 的 6 个 merge SHA | 文档写"已合并"必须有 SHA 可查；这也是"不虚构状态"的底线 |

### 2.3 概念

| 知识点 | 含义 | 位置 | 为什么值得学 |
| --- | --- | --- | --- |
| **epic / 子 issue / milestone 的三层状态** | 子 issue CLOSED ≠ epic 关闭 ≠ milestone 完成；三者由不同动作触发，任何一层都不能替另一层作结论 | `docs/issues/issue-44-*.md`（epic，**2026-10-21 已 CLOSED**）、#46–#52（**全部 CLOSED**）、milestone #3（**仍 open**，open_issues=0 / closed_issues=8） | 收尾文档最容易出现"顺手宣称关闭"的过度声称；把三层状态分开写是审计需要 —— **本次也正是这样发生的：epic 与子 issue 均已关闭，而 milestone 仍 open** |
| **"未执行清单"是一种可继承的资产** | S6 把 6 类未执行项显式登记，S7 原样继承，**关闭 epic 时已实际引用**——缺口不会因为"步骤完成"而蒸发 | S6 证据 §0.7 → 本日志 §1.3 第 1 条 → #52 的 AC-7 → #44 关闭记录 · `docs/issues/issue-44-*.md` 顶部状态行与「epic 关闭记录」表 | 与"区分已运行/未运行/无法运行"的项目规则直接对应；也是防止"假完成"的唯一机制 |
| **文档侧的 AC 也要有"证据形式"** | AC 写"可对照 `ci.yml`/`Dockerfile`"时，验收动作就是**运行一条 `grep`**；否则 AC 无法被判定 | `docs/issues/issue-52-close-out.md`「逐条 AC 状态」表 | 把验收条件改写成"可执行检查"，是把文档工作纳入工程流程的关键一步 |

---

## 3. 后续动作（S7 之后 / 2026-10-21 回填）

1. ~~**主 agent**：提交并推送 `docs/` 下 **7 个文件** 加 `worker` 的 **`README.md` / `frontend/README.md`** 改动（不含 `?? .idea/`），随后由 `github-operator` 创建 S7 的 PR~~ → ✅ **已完成**：PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) 已 **MERGED**（`c1067f7`）。
2. ~~**PR 出现后**：核验 CI 双 job 结果与 PR diff，再回填「已合并/CI 绿」~~ → ✅ **已完成**：`main` CI **SUCCESS**（主 agent 核验），PR diff 与预期一致（`docs/` + 两份 README，无源码/配置）。
3. ~~**#52 关闭 → epic #44 关闭 → milestone #3 状态**：由主 agent 决定并执行；关闭 #44 时必须引用 S6 §0.7 的未执行清单~~ → ✅ **已完成**：#52、#44 均已 **CLOSED**（#44 = completed，完成评论 `issuecomment-6334171353`，**已引用** §0.7 未执行清单/已知缺口）；**milestone #3 仍 OPEN**（open_issues=0 / closed_issues=8），留给 B / B-API / C。
4. **路线图下一项（仍未开始）**：M4.1（Option B-①）需另行立项（用户授权 → issue → 计划 → oracle）；**当前仓库 open issues = 0**，milestone #3 保留供后续使用。

# AI-powered Fabflix 路线图总览与评估（候选方案对比）

> **文档状态：✅ 已批准（用户，2026-10-19）。**
> 用户**整体批准**本路线图（5 个版本 + 1 份本地部署专项），并在 §11 逐条裁定决策（决定见该表「用户决定（2026-10-19）」列）。**实施按 A → B → C 推进**（C 可裁剪；B 用本地默认 + 外部 API 兜底）；**首个工作流 = Option A 迁移**（已批准；**GitHub issue 已完成立项：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)，milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**，2026-10-19 创建，见 §12）。批准前的状态为「已按 oracle 审查修订 / 待用户审批」。
> **执行状态（2026-10-21 关闭回填；不改写下述历史证据与已批准决策）**：**Option A 已完成（epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) CLOSED / reason=completed，2026-10-21）** —— 拆分的 7 个步骤（[#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)–[#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52)）**全部完成并已合并**：PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)（S1 基线，`9696848`）、[#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)（S2 驱动 9.7.0，`55f8d79`）、[#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56)（S3 Boot 3.5.15 + Jakarta，`70fb78b`）、[#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)（S4 JDK 21 工具链，`9a7705c`）、[#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)（S5 `@MockitoBean`，`ff57e79`）、[#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59)（S6 回归与生产 smoke 证据，`b4a6d2a`）、[#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60)（S7 收尾，merge **`c1067f7`** = `main` 最新 merge）；**#46–#52 全部 CLOSED**；epic #44 完成评论 = [`issuecomment-6334171353`](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353)，远端 `## Steps` **7/7 勾选**；`main` CI **SUCCESS**；**当前 open issues = 0**。**下一步 = Option B / B-API（Spring AI + RAG）—— 尚未开始，仍无对应 issue**；**milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) 保持 open**（`open_issues=0` / `closed_issues=8`）供 B/B-API/C 使用。**最终形态**：Boot **3.5.15** / `release=17`（major 61）/ JDK 21 / Jakarta（11 文件 allow-list）；后端 **134 tests / 0.9165 @ 0.90**、前端 **195 / 92.35% @ 90 / 12 e2e**、生产容器 smoke 全绿；**已知覆盖缺口**：`AdminRepository` **0/39**、`GoogleRecaptchaVerifier` **0/17**，真实浏览器多标签/并发/视觉对比度/连接池语义**未自动化**。
> 批准的是**方案方向与决策**，不等于任何代码已改、依赖已装、issue 已建：具体实施仍须按 `docs/plans/` 计划走 `worker` 实现 + 三路 review，且 **D7/D8 等数值项已由用户于 2026-10-19 确认默认值**（见 §11 与 §12.1；原「待用户给数」条目移入 §12.2 保留历史说明）。
> **修订记录(2026-10-18,oracle 只读审查后;主 agent 裁定「全部接受」)**:事实口径修正 **B1**(`javax.servlet` = **11 个文件必须迁移**;宽口径 `javax.` = 15)、**B2**(`@SpringBootTest` 全仓库 **0 处使用**)、**B3**（覆盖率口径 → 不放宽闸门时**最多还能新增 ≈60 行未覆盖代码**；**该数字已于 2026-10-20 随门槛收紧（0.85→0.90）更新为 ≈19 行，见 §2.5**）、**B4**(语料 = **11 个 md / 38,755 词**;含本路线图 6 份文档则 17 个 md / 54,696 词);论证补强 **P1-1**(多语言 embedding = 头号技术风险)、**P1-2**(电影目录语义检索升为等价交付物 **B-1**)、**P1-3**(本地固定 **JDK 21** 作 blocking 前置)、**P1-4**(macOS 容器无 Apple GPU/Metal 直通);并按 **P2-1..P2-22** 逐条写入;跨文档不一致按 oracle **X-1..X-8** 表收敛。审查报告:`.pi/artifacts/oracle-ai-roadmap-review.md`。
> **批准记录（2026-10-19，用户经主 agent 转达）**：用户**整体批准** 6 份文档；**D-时间** = 每周可投入 **>10 小时** → 目标路线 **A → B → C**；**D1** 接受 Boot 3 + `release=17`/JDK 21 + jakarta（Option A 为硬前置）；**D2 / D-API 优先 / D7** 按建议（本地原生 Ollama 默认 + 外部 API 作**显式开关**兜底）；**D3 / D-embedding / D-语料构成** 按建议（语料 = 既有 `docs/` 11 md + `README.md` + 电影结构化卡片，**不含** `ai-*.md`；必须用多语言 embedding 并先实测中→中/中→英）；**D4 / D5** 按建议（`SimpleVectorStore`；首个交付 = B-① 电影目录语义检索 + B-② 文档 RAG）；**D6** 按建议（先建 Option A 迁移 issue，由主 agent/`github-operator` 执行）→ **已于 2026-10-19 执行：issue [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) + milestone #3**；**D-L1** 授权实施阶段安装 Ollama 并下载模型（本次仍不安装）；**D9** 接受模型磁盘占用；**P-2** 批准修改 `README.md` roadmap 第 3 项措辞（由 `worker` 执行）；**审计日志字段（P2-17）** 默认**不记录**用户问题原文（仅长度/哈希）。**D7/D8/D10/D-AI 端点登录/D-语料指纹 的默认值已于 2026-10-19 由用户确认**（详见下方「决策默认值回填」与 §11、§12.1）。
> **决策默认值回填（2026-10-19，用户经主 agent 转达；docs-writer 同步写入 6 份文档）**：**D7** 外部 API 上限 = **默认关闭**；启用时默认**每日 $2 且 ≤200k tokens**（可调）。**D8** 质量下限 = 目录语义检索 **top-5 命中率 ≥80%**、RAG **引用正确率 ≥95%**、NL→JSON **schema 合规率 ≥95%**、**p95 延迟先实测后定**（首版以「可演示」为准）。**D10** 夜间评测 job = **要**（**非阻塞**，M4.2 后接入）。**D-AI 端点登录** = **要求登录**（沿用 `CustomerAuthenticationFilter` 对 `/api/boot/**` 的默认保护，**零行为变更**）。**D-语料指纹** = **要**（记录**文档数/块数/构建时间/哈希**；响应或日志携带指纹，保证评测可复现）。
> **确认不消除风险（重申）**：这些是**已确认的默认值/阈值**，不是**已达标的实测结果**；D8 各项仍须在实施时给出实测证据，**p95 延迟目标仍待实测后再定**，D7 的 $2/日 与 200k tokens 仍**可调**，语料指纹的具体字段仍需实施时落地。**本次回填未删除任何风险项与未实测项**（见 §13）。
> 本文**不声称**任何 AI 功能已实现，也**不声称**任何测试/基准已运行、任何软件已安装。凡"估算/待实测"字样均为估计值，需实测后回填。
>
> 本文档命名说明：项目约定为 `docs/plans/request-YYYY-MM-DD-<slug>.md`（无 GitHub 编号时）。本路线图文档由任务指定使用 `ai-*.md` 命名，**属临时命名**；待对应 GitHub issue 创建后应重命名为 `issue-<编号>-<slug>.md`（**触发条件已于 2026-10-19 到：Option A issue = #44；本次未执行本文件重命名**）。6 份文档的**长期归属建议**见 §12-P-7（一次性版本方案留 `docs/plans/`；跨 issue 持续有效的约束在实施后沉淀到 `docs/architecture/ai-integration-contracts.md`）。

## 元数据

| 项目 | 值 |
| --- | --- |
| 文档类型 | 路线图总览与方案评估（多版本对比 + 推荐路线） |
| 创建日期 | 2026-10-18（本机 `date`） |
| 工作区基线 | `main` @ `76e3d64`（`git log --oneline -3`：`76e3d64` Merge PR #43、`f60c58c`、`d618b64`）；`git status` 仅 `.idea/` 未跟踪 |
| 主要事实来源 | `.pi/artifacts/scout-ai-roadmap.md`（只读调查，含文件:行号与命令证据）；`README.md`；`docs/architecture/frontend-migration-contracts.md`；`pom.xml`；`compose.yaml`；`Dockerfile`；`.github/workflows/ci.yml` |
| 关联候选文档 | `docs/plans/ai-option-0-local-llm-spike.md`、`ai-option-a-boot3-migration.md`、`ai-option-b-spring-ai-rag.md`、`ai-option-c-agent-platform.md`、`ai-local-llm-deployment.md` |
| 用户诉求 | 把 Fabflix 打造成 AI-powered 全栈 webapp（Spring AI + RAG + 本地 LLM）；避免外部 token 成本；提升简历价值 |
| 状态 | **✅ 已批准（用户，2026-10-19）**；实施按 **A → B → C** 推进（C 可裁剪，B 本地默认 + 外部 API 兜底）；**首个工作流 = Option A 迁移（已完成：epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) CLOSED，2026-10-21；milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) 保持 open）；跟踪文档 = `docs/issues/issue-44-boot3-java17-jakarta-migration.md`。~~注意：立项≠实施，迁移尚未开始~~ → **2026-10-21：Option A 已完成（S1–S7 全部合并，epic #44 CLOSED，open issues = 0）**，见下文「执行状态」行；**D7/D8/D10/D-AI 端点登录/D-语料指纹 的默认值已于 2026-10-19 确认**（D7 = 默认关闭、启用时 $2/日 且 ≤200k tokens；D8 = 目录 top-5 ≥80% / 引用正确率 ≥95% / NL→JSON 合规率 ≥95% / p95 先实测后定；D10 = 要（非阻塞夜间 job）；AI 端点 = 需登录；语料指纹 = 要）——见 §11、§12.1–§12.2 |
| 执行状态 | **（2026-10-21 新增；同日关闭回填）Option A（Boot 3 现代化迁移）= 已完成** —— S1–S7 均已合并（PR **#53**、**#55**、**#56**、**#57**、**#58**、**#59**、**#60**；merge `9696848` / `55f8d79` / `70fb78b` / `9a7705c` / `ff57e79` / `b4a6d2a` / **`c1067f7`**；#46–#52 均已 **CLOSED**，`main` CI 均 SUCCESS）；**epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) 已 CLOSED（reason=completed，2026-10-21；完成评论 [`issuecomment-6334171353`](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353)）**；**当前 open issues = 0**。**迁移不交付任何 AI 能力**，只解锁 Spring AI 的前置。**下一步 = Option B / B-API（Spring AI + RAG）—— 未开始、仍无 issue**；**milestone #3 保持 open**（open_issues=0 / closed_issues=8） |
| GitHub | **Option A 已立项并已实施**：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) `Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace`；milestone **[#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**（state=open）；**labels 空、state=OPEN、created `2026-10-19T07:06:27Z`**。**roadmap 第 3/4 项（Spring AI / agent 工作流）仍无对应 issue**（见 §2.6）。本地跟踪文档：`docs/issues/issue-44-boot3-java17-jakarta-migration.md`（2026-10-19 由 `proposed-option-a-boot3-migration.md` 改名，含远端元数据与发布记录，2026-10-21 新增「epic 关闭记录」）。~~立项不代表迁移已实施：无代码/配置/测试改动、无 PR~~ → **2026-10-21 最终更新：迁移已完成** —— **#46–#52 已随 PR #53/#55/#56/#57/#58/#59/#60 全部合并并 CLOSED**；**epic #44 已 CLOSED（reason=completed，`## Steps` 7/7 勾选）**；**open issues = 0**；**milestone #3 仍 open**（留给 B/B-API/C） |

---

## 总结

本条路线图要回答一个问题：在当前这套 **Spring Boot 2.7.18 + Java 11 + React 19** 的 Fabflix 上，如何以最低返工成本做出「Spring AI + RAG + 本地 LLM」的 AI 能力，并让产出对简历有价值。结论是**必须先把 AI 与迁移解耦**：Spring AI 1.x（实测基线 `spring-ai-bom:1.1.8` 依赖 `spring-boot-starter 3.5.15`）要求 **Boot 3.x + Java 17+ + `jakarta.*`**，而本项目是 **Boot 2.7.18（已 EOL）+ Java 11 + 11 个 `javax.servlet` 文件必须迁移**（commerce 5、auth 2、admin 2、search 1、session 1；另有 4 个文件使用 JDK 自带的 `javax.sql`/`javax.xml`，**不迁移**），且架构守卫测试**主动禁止** `jakarta.servlet`——所以「装个 Spring AI 依赖」不可行，前置是一次大版本迁移（**编译 `release=17`，本地/CI/镜像统一 JDK 21**；本机 Maven 现跑 JDK 27，不受 Boot 3.5 支持）。硬件是第二道硬约束：本机 **Apple M2 / 8 GiB 内存 / 22 GiB 可用磁盘**，Docker Desktop 只分到 **3.826 GiB**（配额**可调大**，但会挤压宿主 8 GiB、带来换页风险），且 **macOS 上 Docker 容器无法访问 Apple GPU（无 Metal 直通）→ 容器内 Ollama 只能 CPU 推理、比原生慢一个量级 → 容器内跑 LLM 不可行**；**即便调大内存，也应在原生进程跑 Ollama**。原生最多跑 **~3B–8B 4-bit GGUF**（估）。第三道约束是语料：电影表**没有剧情简介**，只有 `title/year/director/genres/stars/rating` → 首版 RAG 做**项目/文档知识问答**（语料 = 既有 **11 个 md / 38,755 词**；若计入本路线图 6 份文档则 17 个 md / 54,696 词，**已定不纳入**）；**电影剧情问答**需外部语料与合规处理（属 Option C），但**电影目录本身可做语义检索**（**B-①**：结构化卡片向量化，`21087 × 768 维 × 4B ≈ 64.8 MB`、1024 维 ≈ 86 MB → **内存不是约束**，真正成本是一次性 embedding 时间，**待实测**）。第四道约束是**检索质量**：语料与提问都是中文，**embedding 的语言能力是本方案头号技术风险**，必须选多语言 embedding 并先做中→中/中→英实测。为此本文给出 **5 个版本（含 B-API 变体）+ 1 份本地部署专项**：Option 0（零迁移 spike，1–2 周）、Option A（Boot 3 迁移，1.5–3 周，AI 前置且自带现代化价值）、Option B（**推荐**：Spring AI + 本地 Ollama + `SimpleVectorStore`，**B-① 电影目录语义检索 + B-② 文档 RAG**，3–5 周）、**Option B-API**（A + Spring AI + 外部 OpenAI 兼容端点，3–4 周）、Option C（agent 平台，6–10 周）。所有工作量均为**估算**，需在危险点先做实测再收敛。以下每节都区分**已证实事实**（带证据）与**估算/未确认项**。

---

## 1. 目标与用户诉求

### 1.1 用户目标（原文转述）

1. 把 Fabflix 从"传统 Java web + 前端迁移完成"推进为 **AI-powered 全栈 webapp**；
2. 技术路线明确为 **Spring AI + RAG + 本地 LLM**；
3. **尽量避免外部 LLM API 的 token 成本**（本地推理优先）；
4. 产出需具备**简历/作品集价值**。

### 1.2 与仓库既有 roadmap 的对应关系

`README.md:5-10` 的 roadmap 原文：

1. **Foundation**（已做）
2. **Spring Boot**（已做）
3. **Spring AI:** add evaluated natural-language search, recommendations, and retrieval-augmented movie Q&A.
4. **Agent workflows:** expose narrowly scoped tools with authorization, human approval for writes, budgets, audit logs, and end-to-end observability.

`README.md:3` 亦声明应用"being modernized into a Spring Boot and Spring AI agent platform"。**本路线图即 roadmap 第 3/4 项的规划**；第 3 项的"evaluated"（有评测）与第 4 项的 authorization/human approval/budgets/audit/observability 是本路线图的硬性设计输入，不是可选装饰。

### 1.3 本路线图要交付的"完成定义"（**已批准，2026-10-19**）

- 有一个**可演示**的自然语言入口（NL 搜索或问答），带**评测数据**（命中率/正确性/延迟）；
- 有一条**明确标注**的本地推理路径（模型、量化、内存上限、实测 tokens/s）；
- 所有 AI 新增代码**不破坏**既有质量闸门（后端 JaCoCo **0.90**（2026-10-20 由 0.85 收紧）、前端 lines **90**（同日由 88 收紧）、Playwright e2e 在 CI）；
- 有一份可写进简历的**架构图 + 数字**（评测、闸门、本地部署）。

---

## 2. 现状盘点（证据）

> 本节事实除注明外均来自 `.pi/artifacts/scout-ai-roadmap.md`（该报告以只读方式执行 `grep`/`find`/`curl`/只读 SQL/`docker info` 等命令取证，含文件:行号）。**本节不再二次验证**，引用时保留 scout 的原始证据位置。

### 2.1 技术栈与构建（`pom.xml`、CI、Dockerfile）

| 项 | 现状 | 证据 |
| --- | --- | --- |
| Spring Boot | **2.7.18**（EOL，OSS 支持 2023-11 结束）；BOM import、**无 `<parent>`** | `pom.xml:23`、`pom.xml:26-33` |
| Java | **11**（`maven.compiler.source/target = 11`） | `pom.xml:14-15` |
| Servlet API | **`javax.servlet`：11 个文件必须迁移**（commerce 5 / auth 2 / admin 2 / search 1 / session 1）；另有 **4 个文件使用 JDK 自带的 `javax.sql`（1）/`javax.xml.parsers`（3），不迁移**；`jakarta.*` 用量 **0** | 本轮复核：`grep -rl "javax\.servlet" src/main/java` = **11**；`grep -rl "javax\." src/main/java` = **15**（scout §A2 为宽口径 `javax.`，已订正，见 B1） |
| 依赖 | `spring-boot-starter-web`、`-jdbc`、`mysql-connector-j:8.3.0`、`jasypt:1.9.3`；测试 `junit-jupiter:5.11.4`、`mockito:5.23.0`、`byte-buddy:1.17.7` | `pom.xml:37-81` |
| **无** `spring-boot-starter-security` / **无** `spring-ai-*` / **无**任何 vector store 依赖 | 事实 | `pom.xml:37-81`；契约文档 §9 |
| 打包 | `jar` + `spring-boot-maven-plugin` `repackage`（`mainClass=com.fabflix.boot.FabflixApplication`，`finalName=fablix`） | `pom.xml:7-11`、`86`、`88-107` |
| `frontend` profile | enforcer `require-frontend-dist`（`validate`）+ resources 复制 `frontend/dist` → `target/classes/static` | `pom.xml:186-266` |
| CI 后端 job | `name: Build and test (Java 11)`、`actions/setup-java@v5` + temurin **`java-version: "11"`**、`timeout-minutes: 15`、步骤 `mvn --batch-mode --no-transfer-progress verify` | `.github/workflows/ci.yml`（scout §A4） |
| CI 前端 job | Node **22**、`npm ci → lint → test → test:coverage → build → playwright install → test:e2e`、`timeout-minutes: 20` | `.github/workflows/ci.yml`（scout §A4） |
| 运行镜像 | 三阶段：`node:22-alpine` → `maven:3.9-eclipse-temurin-11` → **`eclipse-temurin:11-jre`**（非 root uid/gid 1001） | `Dockerfile`（scout §D18） |
| 本地 `mvn` 运行 JDK | **JDK 27**（Homebrew）跑 Maven 3.10.0；`/usr/bin/java` 为 **17.0.12** | scout §A4 |

**关键推论**：CI、运行镜像、编译目标**三处都绑定 Java 11**，迁移必须同步改这三处 + `pom.xml`。

### 2.2 架构守卫与文档约定（迁移的"路障"）

- `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java:31-42`：遍历 `src/main/java` 断言源码**不含 `jakarta.servlet`**、不含 `@WebServlet`、不含 `extends HttpServlet`。
- 同文件 `:20-26`：断言 `pom.xml` 含 `jar` + `repackage`，且**不含 `jakarta.servlet-api`**。
- **含义（scout 结论）**：Boot 3 迁移必然让 controller/filter 引入 `jakarta.servlet.http.*`，**该守卫测试必须先改**，且 `pom.xml` 不得出现字面量 `jakarta.servlet-api`（`spring-boot-starter-web` 传递引入不算显式依赖字符串，实施时需确认守卫写法）。
- 文档侧同一约定：`docs/architecture/frontend-migration-contracts.md` §8。
- 另一条守卫：`FrontendApiMigrationTest` 已重写为**退役守卫**（`src/main/webapp` 必须不存在，§2 契约文档）——**与 AI 无关，但说明本仓库"用测试钉死架构决策"的既有风格**；新增 AI 相关跨 issue 约定应沿用该风格（写入 `docs/architecture/`）。

### 2.3 数据与语料（RAG 的现实边界）

- Schema（`createtable.sql:1-53`，`moviedb`）：`movies(id,title,year,director)`、`stars(id,name,birth_year)`、`genres`、`stars_in_movies`、`genres_in_movies`、`ratings(movie_id,rating,vote_count)`、`customers`、`credit_cards`、`sales`、`employees`。
- 行数（scout 只读 SQL，容器 `fabflix-mysql`）：`movies` **21087**、`stars` **76259**、`stars_in_movies` **124958**、`ratings` 8005、`genres` 61、`sales` 13556、`customers` 454、`employees` 2。
- **`movies` 没有剧情简介/描述字段**（`createtable.sql:5-10`）→ **电影语义问答缺语料**。
- 现有搜索：全文索引**仅 title 列**（`CreateFullTextIndex.java:24`，离线工具、被 JaCoCo exclude）；`FullTextQueryBuilder.java:1-22` 把输入分词后拼 `+token*` 做 **BOOLEAN MODE 前缀匹配**；`SearchService` autocomplete 需 ≥3 字符、支持 title/rating 排序。**无自然语言、无语义检索**。
- **可用语料**：`docs/` 既有 **11 个 md / 38,755 词**（**不含**本路线图 6 份 `ai-*.md`；这 6 份合计 15,941 词，全部 17 个 md = **54,696 词**）、`README.md` 891 词、`frontend/README.md` 541 词；scout 评估为"结构良好、事实密集的 markdown，适合项目问答 RAG"。
- **语料构成已定（2026-10-19，用户）**：采用 §11 **D-语料构成 (a)** —— 首版语料 = 既有 `docs/` **11 个 md**（**不含**本路线图 6 份 `ai-*.md`）+ `README.md` + **电影结构化卡片**；把 `ai-*.md` 纳入等于把"规划/批准文档"自身喂给 RAG、可能污染问答权威性（此保留为**风险说明**，不再作为待决项）。语料变更仍需**增量重建 + 语料指纹**（§11 **D-语料指纹，2026-10-19 已确认：要**）。

### 2.4 前端（AI 功能的挂载点）

- React **19.2.8**、react-router-dom **7.18.4**、Vite **8.3.0**、vitest 5、Playwright 1.64、`engines.node >= 22.12.0`（`frontend/package.json`）。
- `frontend/src/api/apiClient.js`：`API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'`，统一 `Accept: application/json` + `credentials: 'include'`（`JSESSIONID`）；失败抛 `ApiError`。**新增 AI 端点只需 `apiRequest('/boot/...')`**。
- 路由：公共 `login`/`employee/login`/`playground`；顾客受保护 `home`/`movies`/`movies/:movieId`/`stars/:starId`/`cart`/`checkout`/`confirmation`；员工 `employee/dashboard`；`*` → `NotFoundPage`（`AppRoutes.jsx`）。
- **`/playground` 是现成的公共学习页**（`frontend/src/features/playground/PlaygroundPage.jsx:1-90`，当前为静态演示数据）→ **AI 试验页的最低成本挂载点**。
- 首页搜索框在 `HomePage.jsx:1-115`、自动补全在 `AutocompleteSearch.jsx:1-160`（300ms debounce + `/boot/autocomplete`）。
- **Vite 环境变量构建期内联**（契约文档 §9/#33 D-33-5）：任何 AI 开关若走 `VITE_*`，必须在构建前设定。

### 2.5 测试与质量闸门（AI 代码会被闸门计入）

- 后端：JaCoCo `BUNDLE / LINE / COVEREDRATIO ≥ 0.90`（`pom.xml:158-163`，`verify` 阶段；**门槛于 2026-10-20 经用户批准由 0.85 收紧**，独立 PR，属迁移前置变更）；当前 **LINE 988/1078 = 0.9165**（**未覆盖 90 行**）、`Tests run: 134`（2026-10-20 实测）；excludes 仅限离线工具（`pom.xml:122-131`）。
  - **覆盖率预算口径（已按 B3 订正，并又于 2026-10-20 按新门槛 0.90 重算）**：在**不放宽闸门**的前提下，**最多还能新增 ≈19 行未覆盖代码**（`988/(1078+N) ≥ 0.90 ⇒ N ≤ 19`；等价地“闸门允许未覆盖 ≈108 行，现有 90 行”）。**历史口径（均失效，保留仅为对照）**：B3 的“≈60 行”（旧 0.85 门槛：`968/(1078+N) ≥ 0.85 ⇒ N ≤ 60.8`）；更早写的“余量约 52 行”是把 `968 − 1078×0.85` 误当成“还能新增多少未覆盖行”，**口径错误，已删除**。该口径用于推算“新增 AI 类必须配测试”的紧迫程度。
- 前端：`vitest.config.js` `thresholds.lines = 90`（**2026-10-20 由 88 收紧**），当前 **Lines 92.35%（846/916）**（距门槛 2.35pp）、30 文件 / 195 用例；Playwright **12 passed**（360/1440 两 viewport，API 全 `page.route` mock）。
- 测试模式：全仓库 **0 处使用 `@SpringBootTest`**（唯一命中是 `src/test/java/com/fabflix/boot/web/SpaFallbackTest.java:32` 的**注释**："…asserted without starting a Spring context (no @SpringBootTest anywhere)."）→ **契约文档 §7 的表述正确**，原 U-8 未确认项**已删除**；10 个 `@WebMvcTest`、9 处 `@MockBean`、40 个测试文件；admin 包用 `MockMvcBuilders.standaloneSetup`。
  - **含义**：本仓库**没有 Spring Context 测试基座**，AI 集成测试只能走切片测试（`@WebMvcTest`）或自建独立上下文；这是 Option B §6.3 测试分层的事实前提。
- **政策**：扩 `excludes` 与"excludes 仅限离线工具"冲突（`README.md` "Tests and coverage" 节）；**AI 新增代码默认计入闸门**。

### 2.6 GitHub 现状（**2026-10-19：Option A 已立项；2026-10-21：Option A 已完成（epic #44 CLOSED，open issues = 0），第 3/4 项仍无 issue**）

- `gh issue list --state all`（**2026-10-18 调查时点**）：全部 issue **已 CLOSED**（#10–#16、#24–#33）；`--state open` → **无任何 open issue**。**2026-10-19 更新（只读核验 `gh issue view 44`）**：现有 **1 个 open issue = [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)**（Option A 迁移立项，`Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace`，labels 空，created `2026-10-19T07:06:27Z`）。**2026-10-21 关闭回填（只读核验 `gh issue view 44` / `gh issue list --state open`）**：**#44 已 CLOSED（reason=completed，`closedAt` = `2026-10-22T06:17:55Z`）**，**`--state open` 再次为空（open issues = 0）**；milestone #3 `open_issues=0` / `closed_issues=8`。
- `gh api .../milestones`（**2026-10-18 调查时点**）→ **`[]`**（`Phase 2 — React Frontend Migration` 已随 #31–#33 关闭，2026-10-16）。**2026-10-19 更新**：现有 milestone **[#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**（state=open，created `2026-10-19T07:05:43Z`，由主 agent 创建），#44 已挂载。
- **结论（已更新，2026-10-21 关闭回填）**：roadmap **第 3/4 项（Spring AI / agent 工作流）仍无对应 issue**（规划新 issue 需用户授权）；**首个工作流 Option A 已全部完成并关闭**：**[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) CLOSED / reason=completed（2026-10-21）+ milestone #3 保持 open**，属 §11-D6 / §12.3-A-1 的**已完成项**。**当前 open issues = 0**；milestone #3 `open_issues=0` / `closed_issues=8`。本地跟踪文档：`docs/issues/issue-44-boot3-java17-jakarta-migration.md`（原 `docs/issues/proposed-option-a-boot3-migration.md`，草稿已按批准内容发布）。~~**立项≠实施**：迁移范围（Scope/Acceptance criteria）全部未执行。~~ → **2026-10-21 更新（只读核验 `git log --merges` 与主 agent 回报的 PR 状态）：迁移已实施** —— 子 issue **#46–#51 已随 PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)/[#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)/[#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56)/[#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)/[#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)/[#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59) 合并并 **CLOSED**（`main` 上 CI 均 SUCCESS）**；S7（#52）为文档收尾、**在 PR 中**；**epic #44 与 milestone #3 仍 OPEN**（关闭由主 agent 在 S7 合并后执行）。**B / B-API / C 仍未创建 issue。** **〔2026-10-21 最终状态：S7 已随 PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) 合并（`c1067f7`），epic #44 已 CLOSED / completed，#46–#52 全部 CLOSED，open issues = 0，milestone #3 仍 open。〕**

### 2.7 认证与安全现状（AI 功能的边界输入）

- 认证：session + 两个 filter —— `CustomerAuthenticationFilter` 保护全部 `/api/boot/**`（放行 `login`/`employee-login`/`dashboard*`/`health`）、`EmployeeAuthenticationFilter` 保护 `/api/boot/dashboard*`；**无 Spring Security、无 CSRF token、无 CORS/SameSite 配置**；登录 `changeSessionId()` 轮换（契约文档 §6/§9）。
- **AD-1（已批准跨 issue 决策）**：网页端**继续 session-based 认证**，**JWT 不作为网页端认证机制**，仅保留给**未来 Spring AI agent 平台的独立 trust boundary**；任何 JWT 工作必须**另立 issue + 计划 + oracle 审查**（`docs/architecture/frontend-migration-contracts.md` §10）。
- **AD-2（已批准）**：**单 JVM 实例**部署假设（`compose.yaml` 现为 **`mysql` + `app` 两个服务**，无 `spring-session`/Redis，session 为 JVM 内存）。依赖 session 内存语义的设计只在该假设下成立。
- 响应约定：业务失败 **HTTP 200 + `{status:"fail",message}`**；校验失败 400 `{errorMessage}`；未认证 401 `{errorMessage}`（`EmployeeAuthenticationFilter` 的 401 为手写 `{status:"fail",...}` 形状）。
- session 属性对象必须 `Serializable`（契约文档 §9）。

---

## 3. 硬约束与差距分析

### C-1 Spring AI 的版本前置（最关键）

- 实测（scout `curl`）：`spring-ai-starter-model-openai:1.1.8` 的 pom 显式依赖 **`spring-boot-starter 3.5.15`**；`spring-ai-bom` 现有版本线 1.0.x（1.0.0–1.0.9）、**1.1.x（1.1.0–1.1.8，1.x 最新 GA）**、2.0.x（2.0.0–2.0.1）、2.1.0-M1。
- 结论：**Spring AI 1.x 需要 Spring Boot 3.x + Java 17+ + jakarta EE 9+**；2.0.x GA 预期对应 **Spring Boot 4.0.x**（scout 标注"未从 pom 逐条核实"）。
- **差距 = 一次大版本迁移**（Boot 2.7→3.x、**Java 11 → 编译 `release=17`（工具链 JDK 21）**、`javax.servlet`→`jakarta.servlet`），涉及 **11 个 `javax.servlet` 源码文件**（另外 4 个使用 JDK 自带 `javax.sql`/`javax.xml` 的文件**不迁移**）+ 架构守卫测试 + CI + Dockerfile/基础镜像 + 依赖兼容性核查。
- **工具链前置（P1-3）**：统一表述为"**编译 `release=17`，工具链/CI/镜像统一 JDK 21**"；本机 Maven 现跑 **JDK 27**，**不受 Spring Boot 3.5 支持**，因此"本地固定 JDK 21"是 Option A 的 **blocking 前置**（详见 `ai-option-a-boot3-migration.md` §3/§4.4）。
- **不迁移就没有 Spring AI**（除非走 Option 0 的手写 HTTP 调用）。

### C-2 硬件约束（scout §C11–C13）

| 事实 | 值 | 影响 |
| --- | --- | --- |
| 机型 / 芯片 | `Mac14,7` / **Apple M2** | Metal 可加速，统一内存 |
| 内存 | **8 GiB** | 4-bit 下最多 **~3B–8B**（8B 接近上限、上下文受限；14B+ 不现实），**估算** |
| 磁盘 | **22 GiB 可用**（228 GiB 总） | 模型下载 + Docker 镜像 + jar/构建产物的现实上限 |
| Docker Desktop | **3.826 GiB** 内存 / 8 CPU | 内存配额**用户可调大**，但会挤压宿主 8 GiB（换页风险）；且 **macOS 上容器拿不到 Apple GPU（无 Metal 直通）→ 容器内 Ollama 只能 CPU 推理、比原生慢一个量级** → **容器内跑 LLM 不可行**（7B-4bit ≈ 4.5–5GB 权重+KV）；**结论：即便内存可调，也应在原生进程跑 Ollama**（P1-4） |
| 已装推理软件 | **无**（`which ollama llama-cli vllm` 全空；`~/.ollama` 不存在） | 需安装 + 拉模型（本任务**不执行**） |

### C-3 向量库约束

- **MySQL 8.4 无原生 vector 类型/函数**（`VECTOR` 为 9.0+）；现有 `mysql:8.4` 容器**不能**当向量库。
- Spring AI 1.1.8 提供 `pgvector`/`mariadb`/`chroma`/`qdrant`/`redis`/`elasticsearch`/`milvus` → 均**需新容器**，与 **Docker 3.8 GiB** 冲突。
- **`SimpleVectorStore`（纯内存、零外部依赖）** 与 **AD-2 单 JVM** 假设一致 → **首版首选**；代价：重启即失（需重建索引或做持久化设计）。

### C-4 语料约束

- `movies` 无文本描述 → 电影语义问答**缺语料**；真实电影 Q&A 需外部简介/评论导入（**数据获取与合规问题**）。
- `docs/` 既有 **11 个 md / 38,755 词**（若含本路线图 6 份 `ai-*.md` 则 17 个 md / 54,696 词）+ README 是**现成、优质、可引用**的 markdown → 首版做**项目/开发知识问答 RAG**（B-②）。
- **电影目录可做语义检索（B-①）**：把 `title/year/director/genres/stars/rating` 拼成"结构化卡片"再向量化，可回答"**有没有类似《X》的电影**""1990 年代的科幻片"这类**目录问题**，**无需外部语料、无版权处理**。**内存不是约束**：`21087 × 768 维 × 4B ≈ 64.8 MB`（1024 维 ≈ 86 MB）；真正成本是**一次性 embedding 时间**（M2 上数分钟～数十分钟，**待实测**）与"结构化字段 → 卡片文本"的构造规则。
- **头号技术风险 = embedding 的语言能力（P1-1）**：语料与提问都是中文，必须选**多语言 embedding**（`bge-m3`、`multilingual-e5-*`、`qwen3-embedding` 等候选，**以 Ollama 实际可用性与维度为准，实施时核对**），并先做**中→中 / 中→英检索实测**；否则 AC-B-6 检索命中率会系统性偏低，甚至被误判为"8 GiB 模型不行"。

### C-5 质量闸门约束

- 后端 **0.90**（2026-10-20 收紧；**不放宽闸门时最多还能新增 ≈19 行未覆盖代码**）、前端 lines **90**（同日收紧，余量 2.35pp）、Playwright e2e 均**在 CI 阻塞**。
- AI 代码**非确定性**：不能直接放进 e2e 做真实模型调用；需 mock/stub + 可选真实调用通道（见 §7）。
- 扩 JaCoCo excludes 与既有政策冲突 → 新增 AI 类**必须写测试**。

### C-6 架构与安全约束（AD-1 / AD-2）

- AD-1：网页端不引入 JWT/Spring Security；agent 平台的 JWT/授权是**独立 trust boundary**，须另立 issue + oracle 审查。
- AD-2：单 JVM；任何"进程内状态"（`SimpleVectorStore`、对话记忆）只在单实例成立。
- 当前无 CSRF 保护；新增 AI 写端点（若 Option C）继承该残余风险。

### C-7 与用户目标的张力

| 张力 | 说明 |
| --- | --- |
| 本地推理 vs 质量 | 8 GiB 只能跑 3B–8B 4-bit，NL 搜索/RAG 生成质量**明显低于**云端大模型（估算，需实测） |
| "避免外部 token" vs 简历价值 | 纯本地是把双刃剑：加分项是"离线、零边际成本、隐私"，减分项是"没有云端模型能力" |
| 电影 Q&A 承诺 vs 无语料 | README roadmap 第 3 项写的是 "movie Q&A"：**电影"目录"语义检索可做**（结构化卡片、无语料上限、无版权问题 → B-①），**电影"剧情"问答需要外部语料**（获取与版权 → Option C）→ 需显式修正 README 措辞与语料范围（**已获用户批准，P-2**，见 §12 P-2；由 `worker` 执行） |
| 大迁移 vs 快速见效 | Option A 本身没有 AI 产出（只有现代化价值）；想快速见到 AI 就选 Option 0，但返工 |

---

## 4. 五个版本（含 B-API 变体）的对比

### 4.0 一览表

| 维度 | **Option 0** 本地 LLM spike | **Option A** Boot 3 迁移 | **Option B** Spring AI + 本地 RAG（推荐） | **Option B-API** A + Spring AI + 外部 API | **Option C** Agent 平台 |
| --- | --- | --- | --- | --- | --- |
| 一句话 | 不迁移，用 Java 11 `HttpClient` 手写调用本地 Ollama 的最小 AI 入口 | 只做现代化迁移，为 Spring AI 铺路 | 在 A 之上用 Spring AI 做**电影目录语义检索（B-①）**+ **文档 RAG 问答（B-②）** | **与 B 同一套 Spring AI 代码**，但 chat/embedding 指向**外部 OpenAI 兼容端点**（本地模型可选） | 在 B 之上做向量库容器 + 语料扩展 + 推荐 + 带审批的 agent tools |
| 目标 | 1–2 周内拿到本地模型的**实测数据**（质量/时延） | `README` roadmap 之外的**技术债清偿** + AI 前置 | 交付 README roadmap 第 3 项的**可演示 + 有评测**版本（语料全部来自仓库/数据库） | 同上，但用外部 API 换**质量与速度**，绕开 8 GiB 硬件上限 | 交付 README roadmap 第 4 项（tools/authorization/approval/budgets/audit/observability） |
| 前置 | 安装 Ollama + 拉模型（需用户授权） | 无（自身即前置）；但**本地固定 JDK 21** 为 blocking（P1-3） | **Option A 完成**（硬前置）+ chat 与 embedding 模型就绪 | **Option A 完成** + API key + token 成本政策（D7 = 默认关闭；启用时 $2/日 且 ≤200k tokens） | **Option B 完成** + 向量库容器 + 语料决策 + AD-1 边界 issue |
| 新增组件 | Java 11 `HttpClient`；1 个后端端点；1 个前端入口；**无 Spring AI、无向量库、无评测体系** | Boot 3.5.x（建议）、`release=17`、JDK 21 工具链、jakarta 命名空间；CI/镜像/JDK 升级；守卫测试改写 | `spring-ai-bom` + Ollama chat starter + **多语言 embedding 模型** + `SimpleVectorStore` + RAG Advisor/ETL | 同 B，但用 `spring-ai-starter-model-openai` 指向外部端点（`base-url`/`api-key`）；本地模型仅作可演练项 | pgvector **或** qdrant 容器（二选一）、电影语料导入、内容特征 + LLM 重排、agent tools + 审批流、多轮对话、审计/预算/配额、CI 评测分层 |
| 交付物 | spike 报告（实测 tokens/s、时延、样例质量）+ 可演示端点（报告落地为 `docs/` 内文件） | 迁移后的绿色 CI（127 后端 + 195 前端 + e2e）、覆盖率闸门不变、生产 smoke 通过 | **B-① 电影目录语义检索**（"有没有类似《X》的电影"；无语料上限、无版权问题）+ **B-② 文档 RAG 问答**（带引用来源）+（条件性 **B-③ NL→结构化条件搜索**）+ golden set 评测报告 + 架构图 | 同 B（质量更高、延迟更低），但**运行依赖外部 API 与网络**，无离线能力 | 完整 agent 平台 + 审计/预算/观测 + 评测集进 CI（分层）+ 简历级架构 |
| 工作量（**估算**） | **1–2 周** | **1.5–3 周**（jakarta 机械改动 + 行为差异排查是主变量） | **3–5 周**（含评测集与前端） | **3–4 周**（省去本地模型适配与调优，但要加 API 接入与成本控制） | **6–10 周**（可裁剪，见 Option C 文档"优先 30%"与"最可能翻倍的变量项"） |
| 主要风险 | 结论可能证明"8 GiB 做不了"；手写 HTTP 代码会被 Spring AI 替换（返工） | jakarta 迁移遗漏、Boot 3 行为差异、**本地 JDK 27 不受 Boot 3.5 支持**、依赖不兼容（jasypt 等） | 小模型输出质量与中文能力、**embedding 中文检索能力（头号风险）**、非确定性测试、`SimpleVectorStore` 重启失、prompt injection | **token 成本不可控**、数据出境/隐私、供应商模型版本变更导致行为漂移 | 复杂度/成本/安全 + 与 3.8 GiB Docker 内存冲突 + 语料合规 |
| 简历价值 | **低**（说明"验证过本地推理"，但无框架、无评测） | **中**（Boot 3/Jakarta/Java 17 现代化是常见硬要求） | **高**（Spring AI + RAG + 评测 + 本地推理是差异化组合） | **高**（同上 + API 工程化与成本治理），但少了"本地推理"这个差异化点 | **最高**（agent 平台 + 授权/审批/审计/观测是稀有能力） |
| 成本维度 | 一次性模型下载（磁盘）+ 电费/时延；无 token 计费 | 迁移工时；无运行成本 | 同 0；可选外部 API 兜底（token 计费） | **按 token 计费**（必须有预算硬上限）；无硬件门槛 | 本地向量库容器内存占用；若用云 GPU 则显著成本 |
| 是否可直接进 CI | 可（mock） | 是（现有闸门不变） | 可（mock + 夜间可选真实调用） | 可（mock + 夜间可选真实调用） | 可（分层） |

> **B-API 存在的意义**：它回答了"**是否必须买大内存机器 / 云 GPU**"——**不必须**。想要高质量 AI 而不想被 8 GiB 卡住，只需把生成/embedding 侧换成外部 API（代价是 token 成本与数据出境）；反之，**只有"必须在离线/隐私环境下跑高质量模型"这一诉求才会真正把"买机器/云 GPU"变成必需**。详见 §4.3 末的 B-API 变体与 `ai-local-llm-deployment.md` §6。

### 4.1 Option 0 摘要

**做什么**：不升级 Boot、不引 Spring AI；用 Java 11 自带 `HttpClient`（或 `RestTemplate`）调用本地 Ollama 的 **OpenAI 兼容** `/v1/chat/completions`，做一个最小"检索 → 拼 prompt → 生成 → 返回"端点 + 前端入口（`/playground` 或首页搜索旁）。

**为什么**：在**不承担迁移成本**的前提下，最快拿到**本地模型在本机的真实施工数据**（tokens/s、时延、中文/英文指令跟随、幻觉率），并为后续选型（模型、量化、上下文长度）提供实测依据。

**局限（必须写进结论）**：无 Spring AI（等于绕开用户的明确技术路线）、无持久向量库、无评测体系、`HttpClient` 手写代码在 Option B 时大概率被替换（返工）；简历价值低。

**何时该放弃**：若 1 周内实测显示 8 GiB 上的输出质量无法支撑演示（例：NL→查询转换准确率低于可接受阈值），则应转向"外部 API 兜底"或"缩小到检索式功能（不做生成）"，而不是继续在这条路上投入。

详见 `docs/plans/ai-option-0-local-llm-spike.md`。

### 4.2 Option A 摘要

**做什么**：Boot 2.7.18 → **3.5.x**（与 Spring AI 1.1.x 基线一致）、**编译 `release=17`**、**本地固定 JDK 21**（工具链，blocking 前置）、`javax.servlet` → `jakarta.servlet`（**11 个文件必须迁移**；另有 4 个使用 JDK 自带 `javax.sql`/`javax.xml` 的文件**不迁移**）、改写 `SpringBootOnlyArchitectureTest`、CI Java 版本 + `Dockerfile` 运行镜像升级、依赖兼容性核查（mysql-connector-j 升 9.x 的 JDBC 行为差异、jasypt、Mockito/ByteBuddy、surefire、JaCoCo）。

**为什么**：这是**所有 Spring AI 方案的硬前置**（C-1）；即使不做 AI，**Boot 3 + Java 17** 本身也是明确的现代化收益（EOL 修复、简历价值）。

**约束**：**不改业务行为**；验收 = 现有 127 后端测试 + 195 前端测试 + 12 e2e 全绿、覆盖率闸门不变、生产 smoke 通过（HTTP 语义、SPA fallback、认证 401 形状都不变）。

详见 `docs/plans/ai-option-a-boot3-migration.md`。

### 4.3 Option B 摘要（推荐）

**做什么**：在 A 之上引入 `spring-ai-bom` + **Ollama chat starter**（默认 `http://localhost:11434`）+ **多语言 embedding 模型**（候选 `bge-m3`/`multilingual-e5-*`/`qwen3-embedding`，**以 Ollama 实际可用性与维度为准**）+ **`SimpleVectorStore`**（内存，零容器）+ Spring AI RAG（Advisor / ETL pipeline），交付**两个等价交付物**：

- **B-① 电影目录语义检索**：把 `title/year/director/genres/stars/rating` 拼成"结构化卡片"→ embedding → 语义 top-k → 复用现有电影列表/详情 UI；可回答"**有没有类似《X》的电影**"这类**目录问题**，**无需外部语料、无版权处理**，效果上限受结构化字段本身限制（scout §B10）；
- **B-② 文档知识 RAG**：语料 = `docs/`（既有 **11 个 md / 38,755 词**，**已定不含**本路线图 `ai-*.md`，见 D-语料构成）+ `README`，回答**必须带引用来源**；
-（条件性 **B-③ 自然语言搜索**：NL → 结构化查询/排序 + 严格白名单校验；是否做取决于 B0 spike 实测的小模型 JSON 合规率，不达标则由 B-① 的语义检索承担同一直入口。）

**为什么**：这是**成本/风险/简历价值的最优交点**——用上了用户点名的 Spring AI 与 RAG，零新增容器（不撞 3.8 GiB 内存墙），且两个交付物的语料都**现成或可自建**，能力可演示、可评测、可写进简历。

**关键取舍（已按 P1-2 修订）**：① 电影**目录**语义检索（B-①）从"备选"**升为等价交付物**——它直接对应用户原话"把网站相关的内容和知识点存入 RAG"，且**无语料上限、无版权问题**；② 电影**剧情**问答仍需外部语料，属 Option C；③ README 措辞需区分"**电影目录语义检索（可做）**"与"**电影剧情问答（需外部语料）**"（**已批准，P-2**，见 §12-P-2）；④ `SimpleVectorStore` 重启即失（首版可接受，迁移路径指向 pgvector）。

**风险**：3B–8B 模型的输出质量与中文能力（**待实测**）、**embedding 的中文检索能力（头号风险，P1-1）**、非确定性测试策略、prompt injection（RAG 语料注入 → 必须限定语料来源与输出格式）。

#### 变体：Option B-API（外部 API 优先）

**是什么**：与 Option B **同一套 Spring AI 代码与端点契约**，只把 chat/embedding 的 `base-url`/`api-key` 指向**外部 OpenAI 兼容 API**（本地 Ollama 仍可作为可演练项）；工作量 **3–4 周**（估算）。**代价**：按 token 计费（D7 已确认：默认关闭；启用时每日 $2 且 ≤200k tokens）、数据出境/隐私、供应商模型版本变更导致行为漂移；**收益**：质量与速度最优，**不依赖 8 GiB 硬件**，也不需要 embedding 中文实测就能达标。**何时选它**：D-时间 很紧、或用户对质量要求高、或"先看到效果再考虑本地"。**何时不选**：若目标就是"零 token 成本 + 离线/隐私"（那仍应选本地 B）。

详见 `docs/plans/ai-option-b-spring-ai-rag.md`。

### 4.4 Option C 摘要

**做什么**：在 B 之上加 **向量库容器**（pgvector 或 qdrant 二选一，含与 3.8 GiB Docker 内存的取舍）、**电影语料扩展**（外部简介导入，涉及获取与合规）、**推荐系统**（协同过滤 + LLM 重排）、**agent tools**（查电影 / 查订单 / 购物车操作 / 员工写操作**需人工审批**）、多轮对话、**审计日志**、**预算/配额**、评测集进 CI（分层：确定性单测 + 夜间真实调用）。

**为什么**：这是唯一完整覆盖 roadmap 第 4 项（authorization、human approval for writes、budgets、audit logs、observability）和用户"AI-powered 全栈"叙事的版本，简历价值最高。

**风险**：复杂度、成本、安全面（写操作 + 审批流 + 审计）；Docker 内存限制可能强制"向量库跑在宿主机/远端"的折中；语料导入合规需用户先决策。**工作量 6–10 周偏乐观**：合规（C-2）、审批流 + UI（C-5）、评测扩展（C-10）是**最可能翻倍的变量项**（详见 Option C 文档 §5.3）。

详见 `docs/plans/ai-option-c-agent-platform.md`。

### 4.5 本地部署专项（贯穿 A/B/C）

`docs/plans/ai-local-llm-deployment.md` 是本路线图的**硬件与部署事实基线**：Ollama / llama.cpp / LM Studio / vLLM 对比、模型与量化矩阵（含磁盘/下载成本）、与 Spring AI 的对接配置、性能预期与**测量方法**（不编造数字）、本地 vs 外部 API 决策树、部署形态（本机原生推荐 / 局域网大内存机器 / 云 GPU / Docker 的边界）。**所有选项都共用这份文档**。

---

## 5. 推荐路线

> **用户已选（2026-10-19）：每周可投入 >10 小时 → 目标路线 A → B → C**（**C 可裁剪**；**B 用本地默认 + 外部 API 兜底**）。下文按该约束排序；Option 0 不属于目标路线，仅作可选降风险手段。

### 5.1 推荐（按序）

```
[可选] Option 0 spike（1–2 周，只做实测与报告）
        ↓
Option A   Boot 3 迁移（1.5–3 周，硬前置，独立价值；blocking = 本地固定 JDK 21）
        ↓
Option B   Spring AI + 本地 Ollama + SimpleVectorStore
           B-① 电影目录语义检索 + B-② 文档 RAG（+ 条件性 B-③ NL→结构化搜索）
           （3–5 周）★首个 AI 交付
        ↓
Option C   Agent 平台（6–10 周，可裁剪，分阶段）

（分叉）若不想受 8 GiB 硬件限制 / 想要最高质量：Option A → **Option B-API**（3–4 周，token 计费）
        后续可平滑切回本地：同一套 Spring AI 代码只改 `base-url`/`api-key`
```

理由：

1. **Option A 无法绕开**（C-1），越早做越省；把 A 单独作为一次"现代化 issue"还有独立简历价值，不需要等 AI。**已于 2026-10-19 获用户批准（D1），并定为首个工作流**（**立项已完成**：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) + milestone #3，2026-10-19）。
2. **Option B 是"性价比高点"**：满足用户点名的 Spring AI + RAG、零新增容器、两个交付物（B-① 电影目录语义检索 / B-② 文档 RAG）的语料**现成或可自建**、可评测；**若本地硬件成为阻碍，B-API 是同一代码的可插拔替换**。
3. **Option 0 只在"想先验风险"时做**：如果用户更在意"先看效果再投入迁移"，可以先 spike；但必须接受手写 HTTP 代码的返工，且 spike 结论（模型/量化/上下文）正好是 B 的输入。
4. **Option C 放在 B 之后**：它的价值来自 B 已经跑通的模型与检索底座；在 B 之前做会同时承担"迁移 + 框架 + agent + 语料"四个未知。**用户已批准此行序（2026-10-19）**：C 排在 B 之后且**可裁剪**（优先 §5.2 的 30%），推荐系统也放入 C。

### 5.2 不推荐的做法（及理由）

| 不推荐 | 理由（证据） |
| --- | --- |
| **在 Boot 2.7.18 上硬引 `spring-ai-bom`** | Spring AI 1.x 依赖 Boot 3.5.15 基线（scout `curl`）；2.7 上不可行 |
| **直接在 Docker 容器里跑 Ollama/LLM** | Docker Desktop 仅 **3.826 GiB**（`docker info`）——配额**可调大**，但会挤压宿主 **8 GiB**（换页风险）；更关键的是 **macOS 上容器拿不到 Apple GPU（无 Metal 直通），容器内 Ollama 只能 CPU 推理、比原生慢一个量级** → 即便内存够，也应**在原生进程跑 Ollama**（P1-4） |
| **首版上 pgvector/qdrant/Redis 容器** | 与 3.8 GiB 内存冲突（C-2/C-3）；且无必要（`SimpleVectorStore` 足够） |
| **首版就做"电影剧情问答"** | `movies` **无剧情简介字段**（`createtable.sql:5-10`），需外部语料 + 合规决策；但**电影目录语义检索（B-①）是可做的**（结构化卡片 + 向量化，无语料上限、无版权问题） |
| **直接跳到 Spring AI 2.0.x / Boot 4** | 2.0.x 预期 Boot 4 基线（scout 标注未逐条核实），跨度更大、生态资料更少 |
| **把 AI 代码加入 JaCoCo excludes** | 与既有限定"excludes 仅限离线工具"的政策冲突（`README.md` 覆盖率节） |
| **在网页端引入 JWT/Spring Security 来"给 AI 加权限"** | 违反 **AD-1**（已批准跨 issue 决策，契约文档 §10）；JWT 属 agent 平台独立 trust boundary，须另立 issue |
| **把真实模型调用塞进 Playwright e2e / 每次 PR 的 CI** | e2e 目前 API 全 `page.route` mock（契约文档 §7）；真实调用非确定性 + 慢，会破坏 CI 稳定性 |
| **用外部 API 但又声称"零成本/本地推理"** | 与用户目标冲突；外部 API 只能作为**显式开关的兜底**，不能在文档里含糊 |

### 5.3 决策耦合提醒

- 若用户**拒绝** Option A（迁移），可行空间只剩 Option 0（+ 后续手写 HTTP 的持续维护），本路线图的 B/B-API/C **全部作废**。
- 若用户**只要简历亮点、不要 8 GiB 折腾**，可选 **Option B-API**（A + Spring AI + 外部 OpenAI 兼容端点，见 §4.3 变体）；代价是 token 成本（D7 已确认：默认关闭；启用时每日 $2 且 ≤200k tokens）与数据出境。**这直接回答"是否必须买大内存机器/云 GPU"：不必须**（见 §4.0 表下注）。
- 若用户**有更大内存的机器**（局域网 32/64 GiB 或云 GPU），C-2 的模型上限与 C-3 的容器限制都会放宽 → Option C 的可行性显著提升，且**换 32/64 GiB 机器后容器化才可能可行**（`ai-local-llm-deployment.md` §7）。

---

## 6. 里程碑切分与"完成定义"

> 编号接续既有 M1（Foundation）/ M2（React 前端迁移，里程碑 `Phase 2` 已关闭）。**M3（Option A）已完成 = issue [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) CLOSED（reason=completed，2026-10-21，`## Steps` 7/7）+ milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) 保持 open 供 M4+（2026-10-19 立项）；S1–S7 全部合并（PR #53/#55/#56/#57/#58/#59/#60）**。M4–M6 仍为草案命名，不代表已创建的 GitHub milestone。**
> **用户已定（2026-10-19）**：按 **A → B → C** 推进，**C 可裁剪**；**首个工作流 = M3（Option A）**；**M5.x 必须等到 M4 完成后**才能开始；**M4.1–M4.3 的数值阈值已由 D8（2026-10-19 确认）给出**；**Nightly job 已由 D10 确认添加（非阻塞，M4.2 后接入）**。

| 里程碑 | 内容 | 前置 | 验收标准（草案） | 完成定义（DoD） |
| --- | --- | --- | --- | --- |
| **M3**（Option A） | Boot 2.7.18→3.5.x、`release=17`、**本地固定 JDK 21**、jakarta 迁移（11 个文件）、守卫测试改写、CI/镜像/JDK 升级、依赖兼容性核查 | 无（但**本地 JDK 21 固定**为 blocking 前置） | `mvn --batch-mode verify` 全绿（`Tests run` 不下降）、前端 `lint/test/coverage/build/e2e` 全绿、JaCoCo `BUNDLE/LINE ≥ 0.90` 不变（2026-10-20 收紧后的门槛）、生产 compose smoke 通过（health/深链/401/404 语义不变）、`mvn -version` 显示 JDK 21 | 无行为变更的迁移 PR 合并；`docs/architecture/` 更新守卫与 Java 版本约定。**【2026-10-21 回填】已达成**：后端 **134 tests / JaCoCo 988-90-1078 = 0.9165（门槛 0.90）**、前端 **30 files / 195 tests / Lines 92.35%（门槛 90）**、**12 次 e2e**、生产 compose smoke 全绿（证据 `docs/logs/2026-10-21-issue-51-regression-proof.md`）；`docs/architecture/frontend-migration-contracts.md` 的 **§8（allow-list）/§11（JDK 21 镜像）/§12（JDK 21）** 已由 **S7（#52）** 同步 —— **S7 已随 PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) 合并（`c1067f7`），M3 已全部关账（epic #44 CLOSED，2026-10-21）**；**未执行项见 S6 证据 §0.7（不得读作全部验证通过）** |
| **M4.1**（Option B-①） | **电影目录语义检索**：结构化卡片构造（title/year/director/genres/stars/rating）→ embedding → 语义 top-k → 复用现有电影列表/详情 UI | M3 + chat 与 embedding 模型可用（**embedding 中文能力先实测**） | 端点契约测试（mock embedding/chat）通过；前端页面测试 + 覆盖率不影响闸门；golden set 跑出目录问题的**检索命中率**；embedding 构建**耗时实测**记录 | 可演示（"有没有类似《X》的电影"）+ 有评测数字 + embedding 构建耗时/内存实测记录 |
| **M4.2**（Option B-②） | 文档 RAG 问答（`docs/` 11 md / 38,755 词 + README 语料、带引用来源） | M4.1（共用管道） | 引用来源正确性抽检通过；回答**必带来源**；`SimpleVectorStore` 重建流程可复现 + **语料指纹**记录 | 可演示 + 评测报告（命中率/引用正确性/延迟）+ 语料更新流程文档化 |
| **M4.3**（Option B-③，条件性） | NL 搜索（NL → 结构化条件 + 严格白名单校验 + 映射现有查询） | M4.1/B0 实测确认小模型 JSON 合规率可接受 | 格式合规率与条件命中率达标（**阈值 = D8 已确认：NL→JSON schema 合规率 ≥95%**）；不达标则**由 M4.1 的语义检索承担同一直入口**并明确记录 | 可演示，或如实记录"退化为语义检索" |
| **M5.x**（Option C 分阶段） | C-1 向量库容器（或替代持久化）；C-2 语料扩展；C-3 推荐；C-4 agent tools + 审批；C-5 审计/预算/观测；C-6 评测进 CI | M4 完成 + 用户对语料与安全边界批准 | 每个 C-x 单独验收；写操作必须有审批流与审计记录（**引用 #30 的 T1 幂等/重放语义，不复用实现**）；预算/配额有硬上限 | 每个子项独立 issue/计划/oracle 审查；AD-1 边界 issue 先立 |
| **Nightly**（贯穿） | "真实模型调用"评测 job（不进 PR 阻塞路径） | **M4.2 后接入（D10 已确认「要」）** | 夜间 job 产出评测报告（**失败不阻塞 PR**） | 有历史趋势数据；**CI 分钟预算仍待实施时确认**（§13-U-9） |

**时间预期（按用户所选"每周 >10 小时"，**均为估值，未实测**）**：M3 ≈ **2–3 周**（含本地 JDK 21 固定、依赖兼容性核查；主变量 = Boot 3 行为差异）；M4.1 ≈ **1 周**；M4.2 ≈ **1 周**；M4.3（条件性）≈ **0.5–1 周**；即 **A + B 合计 ≈ 4–5 周**；M5.x ≈ **6–10 周且可裁剪**（最可能翻倍的变量项见 Option C §5.3）；Nightly job 贯穿。以上为按"每周 >10 小时"的**线性估算**，非承诺；实际以实施时实测与 issue 为准。

**M4.1–M4.3 的"验收 = 有数字"** 是本路线图的核心立场：README roadmap 第 3 项明确写着 "**evaluated**"，没有 golden set 与数字的 AI 功能只能算 demo。

---

## 7. 评测与质量策略

### 7.1 分层评测

| 层 | 内容 | 运行位置 | 是否阻塞 |
| --- | --- | --- | --- |
| L1 单元/契约 | 纯函数（prompt 构造、响应解析、检索结果排序、引用拼装）；chat client 用 mock | `mvn verify` / `npm test` | **阻塞**（沿用现有闸门） |
| L2 契约/集成 | 端点 + filter + 错误/超时/降级语义；`@WebMvcTest` + mock chat client | CI 后端 job | **阻塞** |
| L3 前端页面/流程 | NL 搜索与问答页的状态机、空态、错误态、`role="alert"` 安全展示 | vitest | **阻塞** |
| L4 e2e | Playwright：**API 全 mock**（沿用契约文档 §7 模式），验证 UI 流程与无障碍 | CI 前端 job | **阻塞**（不调用真实模型） |
| L5 评测（golden set） | 真实模型调用：检索命中率、引用正确性、NL→查询转换正确率、延迟、失败率 | **夜间 job / 手工脚本** | **不阻塞 PR** |
| L6 人工抽检 | LLM-as-judge 输出 + 人工评分（相关性/有用性/幻觉） | 里程碑评审 | 不作为 CI 门禁 |

### 7.2 Golden set / 回归集设计（草案）

- **规模**：**20–50 条**（估算，首个版本 20 条足够发现方向性错误；50 条可做趋势对比）。
- **NL 搜索用例（B-③）**：自然语言 → 期望的结构化条件（如"1990s 的科幻片"→ `genre=Sci-Fi, year∈[1990,1999]`）；判据 = 结构化条件是否覆盖期望字段（精确/部分命中）。
- **电影目录检索用例（B-①）**：问题（如"有没有类似《The Matrix》的电影？""1990 年代的科幻片"）→ 期望返回集合的特征（genre/年代/与种子片的相似度）；判据 = ① 命中率（是否出现在 top-k） ② 合理性（人工抽检：推荐结果是否在 genre/年代上成立）。**不需要外部语料，评测集可以自建。**
- **RAG 问答用例（B-②）**：问题（如"覆盖率闸门是多少？""jump-back 为什么要带 mode=fulltext？"）→ 期望**来源文件/章节** + 关键事实；判据 = ① 检索是否命中正确文档 ② 回答是否包含关键事实 ③ 引用来源是否真实存在（**引用正确性**）。
- **回归集**：把已修复的错误（幻觉、漏引用、错误页码）固化为用例，防止退化。
- **版本化**：golden set 以 `docs/` 内或专用目录的**可读数据文件**形式管理（具体位置由实施计划决定，本路线图不预设路径）。

### 7.3 RAG 指标（定义 + **已确认阈值**）

> **阈值（2026-10-19，用户确认的 D8 默认值）**：目录语义检索 **top-5 命中率 ≥80%**；RAG **引用正确率 ≥95%**；NL→JSON **schema 合规率 ≥95%**；**p95 延迟仍待实测后再定**（首版以「可演示」为准）。
> **注意**：阈值确认 ≠ 已达标；下表仍为**指标定义**，实施时须给出**实测证据**（尤其 p95 与引用正确性抽样）。

| 指标 | 定义 | 采集方式 |
| --- | --- | --- |
| 检索命中率（recall@k） | 期望文档出现在 top-k 的比例 | 离线脚本对比 golden set |
| 引用正确性 | 回答引用的来源片段是否**确实**支持该回答 | 人工/LLM-as-judge（**需抽样**） |
| 回答事实符合率 | 回答中事实性声明与语料一致的比例 | 人工抽检（20% 抽样，估算） |
| 拒绝率/安全率 | 语料外问题是否被明确拒绝而非编造 | golden set 中的"应拒绝"用例 |
| 延迟 P50/P95 | 端到端（检索 + 生成）耗时 | 应用侧埋点（见 §7.5） |
| 失败率 | 超时/连接失败/解析失败占比 | 埋点 + 日志 |

### 7.4 非确定性在 CI 中的处理（硬要求）

1. **PR 路径永不依赖真实模型**：chat/embedding 一律 mock（L1–L4）。
2. **真实调用只在夜间 job 或手工脚本**（L5），失败**不阻塞** PR，但要有历史趋势与告警。
3. **确定性化手段**：固定 prompt 模板、`temperature=0`（如模型支持）、固定 top-k、固定 seed（如支持）——**均需实测确认模型是否支持**（未确认项）。
4. **e2e 契约保真**：mock 响应的**形状必须与真实 Spring AI 响应一致**（沿用契约文档 §7 的 `page.route` 惯例与"最具体优先"分派规则），否则测试是伪绿。

### 7.5 可观测性（roadmap 第 4 项的 observability）

- 指标：请求延迟（P50/P95）、失败率、超时率、**token 估算**（本地推理无法直接计费，但可统计输入/输出 token 数用于容量规划）、每次请求检索文档数、模型名/版本。
- 实现层次（由轻到重，Option B 先做轻量）：日志结构化字段 → 简单计数器/计时器 → Micrometer/Actuator（若引入需评估闸门与依赖影响）。
- 审计（Option C）：谁（customer/employee 身份）、何时、调用哪个 tool、参数摘要、结果、是否需审批、审批人——**审计内容不得记录密码/卡号**。

---

## 8. 安全与合规

| 风险 | 说明 | 缓解（草案） |
| --- | --- | --- |
| **Prompt injection（用户输入）** | 用户输入可能试图覆盖系统指令，诱导越权/泄露 | 系统提示与用户输入**结构化分离**；限制工具白名单；输出后置过滤；对"要求执行写操作"的输入强制走审批 |
| **RAG 语料注入** | 语料若可被外部内容污染（如导入的电影简介含指令），检索片段可能携带指令 | 首版语料**仅限仓库内受控 markdown**；外部语料（Option C）必须清洗 + 显式标记不可信；不得把检索片段当系统指令 |
| **输出过滤** | 模型可能输出未授权数据（其他顾客信息）或不安全内容 | 工具/检索层做**权限过滤**（只检索调用者可见数据）；输出不回显敏感字段（沿用 #30「不回显卡号」先例） |
| **权限 / 授权** | AI 端点默认落在 `CustomerAuthenticationFilter` 保护范围内（`/api/boot/**`）；员工写操作（C）需明确归属 | **AD-1**：网页端仍是 session；**JWT/Spring Security 属 agent 平台独立 trust boundary → 另立 issue + oracle 审查** |
| **写操作的人工审批（roadmap 第 4 项）** | agent 不得直接执行写操作 | 所有写 tool 走"提议 → 人工批准 → 执行"三段式，审批记录进审计；见 Option C 文档 |
| **预算 / 配额** | 本地推理无 token 计费，但有**时间/内存/并发**预算；外部 API 有 token 成本 | 配额按请求数/token 估算/并发上限设**硬上限**；超限返回明确错误（符合 `200 + {status:"fail"}` 或 429 语义需定稿） |
| **隐私** | 本地推理天然隐私友好；外部 API 会把 prompt（含语料片段）送到第三方 | 本地优先（默认）；外部兜底必须**显式开关**并在文档说明数据流向 |
| **CSRF 残余风险** | 现状无 CSRF 保护（契约文档 §9）；新增 AI 写端点（C）继承 | 列为残余风险；是否引入 CSRF 需单独决策（不在本路线图批准范围） |
| **语料合规（C）** | 外部电影简介/评论的版权与使用条款 | 引入前需**用户决策 + 明确来源与许可**；不得默认抓取 |
| **模型许可（本地模型，P2-22）** | 候选模型的许可**各不相同**：Llama 系列（Meta Llama Community License，含使用/分发限制）、Gemma（Google Gemma Terms of Use）、Qwen（多为 Apache-2.0，以模型页为准）、Mistral 7B（Apache-2.0）、Phi（MIT）等 | **逐模型核对许可**（以模型卡/官方页面为准，**实施时逐条确认**）；只使用许可明确允许预期用途的模型；模型权重**不入仓库**；评测/演示报告记录模型来源与许可 |

---

## 9. 成本模型（维度对比，**不报价**）

| 维度 | 本地推理（本机原生） | 外部 API |
| --- | --- | --- |
| 计费方式 | 一次性硬件投入 + 电费 + 首次模型下载（磁盘）；**无按次计费** | **按 token**（输入/输出单价不同；随请求量与上下文长度线性增长） |
| 边际成本 | ≈ 0（直到需要更大机器） | 每请求递增 |
| 时延 | 8 GiB M2 上 3B–8B 4-bit 生成速度**明显低于云端**（推测，须实测） | 网络往返 + 队列，通常更快 |
| 吞吐/并发 | 受本机内存/CPU/GPU 限制，单并发（估算） | 受配额与预算限制，可水平扩展 |
| 上下文长度 | 受内存限制（8B 4-bit 时上下文要小） | 受模型上限与价格影响 |
| Embedding 成本 | 本地 embedding 无按次成本（候选多语言模型见 `ai-local-llm-deployment.md` §4.1；维度/体积待实测，磁盘约 +0.3–1 GB） | 按 token 计费（通常便宜但仍随语料量增长） |
| 前置投入 | 安装 + 模型下载（**磁盘 22 GiB 是硬约束**） | API key + 网络 + 隐私评审 |
| 隐私/离线 | 数据不出本机；可离线 | 数据出本机；需网络 |
| 可复现性 | 模型/量化固定后较好 | 供应商模型版本变更可能改变行为 |
| 隐藏成本 | 失败重试的时间成本；磁盘清理；机器发热/降频 | 失控调用（runaway loop）的成本风险 → 需预算上限 |

**结论（草案）**：默认本地（满足"避免 token 成本"与隐私），把外部 API 设计成**可插拔兜底**（同一接口，配置切换），并在 Option B 的配置与测试策略中预留该切换点。

---

## 10. 简历价值分析

| 能力 | 简历要点（示例措辞） | 可展示证据 | 属于哪个版本 |
| --- | --- | --- | --- |
| Boot 3 + Java 17 现代化 | "把 EOL 的 Spring Boot 2.7/Java 11 应用迁移到 Boot 3.x/Java 17，含 Jakarta 命名空间迁移与守卫测试改造" | 迁移 PR diff、绿色 CI、命令行证据 | **A** |
| Spring AI 集成 | "使用 Spring AI 构建 LLM 应用层（chat/embedding/RAG Advisor）" | 依赖与配置、端点契约、架构图 | **B / B-API** |
| 电影目录语义检索（B-①） | "为 21,087 部电影构建结构化卡片向量索引，支持语义化"类似电影"检索；无外部语料/版权依赖" | 卡片构造规则 + 构建耗时实测 + 检索评测数字 | **B-①** |
| RAG + 引用 | "为项目文档构建 RAG 问答，回答带可核验来源；检索命中率/引用正确性有评测数据" | golden set + 评测报告 + 指标数字 | **B-②** |
| 多语言检索风险识别 | "在中文语料 RAG 中识别并实测 embedding 语言能力风险（中→中/中→英）" | 选型对比表 + 实测命中率 | **B-②** |
| 本地 LLM 部署 | "在 8 GiB Apple Silicon 上部署 4-bit 量化本地模型，实测 tokens/s 与内存占用，零 token 成本" | 部署文档 + **实测**数字（安装后回填） | **B**（+专项文档） |
| 评测与质量门禁 | "把非确定性 AI 功能纳入 CI：mock 分层 + 夜间真实调用评测；不破坏既有覆盖率/CI 闸门" | CI 配置、覆盖率数字、评测 job 历史 | **B/C** |
| Agent tools + 授权/审批 | "设计带授权、写操作人工审批、预算与审计的 agent 工具层" | 工具清单、审批流设计、审计样例 | **C** |
| 可观测性 | "为 LLM 路径加延迟/失败率/token 估算指标" | 仪表板/日志格式、指标定义 | **B/C** |
| 向量库与持久化 | "对比并落地向量存储（内存 → pgvector/qdrant），含容量与内存取舍" | 对比表 + 迁移方案 | **C** |
| 推荐系统 | "协同过滤 + LLM 重排的电影推荐" | 算法说明 + 评测（如离线指标） | **C** |
| 数据/合规意识 | "为 RAG 语料做来源与许可评估" | 语料决策记录 | **C** |

---

## 11. 用户决策清单与裁定结果（2026-10-19 已回填）

> 对应 scout 报告"需要用户 / 主 agent 决策的点"(6 条)+ oracle 补充的 5 项**会改变方案选择**的决策(§8 决策清单补全建议),逐条给出选项与本路线图的**建议**。**用户已于 2026-10-19 逐项裁定，结果在“用户决定（2026-10-19）”列**；表中建议列保留为当时依据，不做事后改写。**凡未逐条裁定的项已在该列显式标出。**
> **第二轮回填（2026-10-19，用户经主 agent 转达；docs-writer 写入 6 份文档）**：原显式标为「未逐条裁定 / 待用户给数」的 **D-AI 端点登录**、**D-语料指纹** 以及次要问题 **D7 / D8 / D10**，已由用户给出**确认的默认值**（见下表与本节末）；这些条目已不再是待决项，但**其实测/实施细节仍为未验证项**。

| # | 决策点 | 选项 | 本路线图建议 | **用户决定（2026-10-19）** | 影响 |
| --- | --- | --- | --- | --- | --- |
| **D-时间** | **每周可投入小时数 / 目标期限**（选择 0/A/B/B-API/C 的**第一约束**） | 需用户给数（小时/周）+ 目标日期 | 映射示例：**每周 ≤5 小时 → Option A + B-API**；每周 5–10 小时 → A + B；每周 ≥10 小时且想拿最大简历分 → A + B + C（可裁剪） | **每周可投入 >10 小时 → 目标路线 A → B → C**（**C 可裁剪**；**B 用本地默认 + 外部 API 兜底**）；本次**未给出目标日期**（如需要应补） | 决定整条路线能否按"周"推进；决定先走本地还是外部 API |
| **D1** | 是否接受 Boot 2.7.18→3.x + **Java 11 → `release=17`（工具链 JDK 21）** + jakarta 大迁移（AI 的硬前置） | (a) 接受，先做 Option A；(b) 不接受，只做 Option 0；(c) 先 spike 再定 | **(a)** 接受 | **接受（a）**：Option A 作为硬前置，并定为**首个工作流**（**立项已完成：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)**，2026-10-19） | 决定 B/B-API/C 是否可行 |
| **D2** | 模型运行位置 | (a) 本机原生 Ollama；(b) 外部 API；(c) 本机 + 外部兜底；(d) 更大内存机器/云 GPU | **(c)**：默认本地，外部 API 作**显式开关**兜底 | **按建议（c）**：**本地原生 Ollama 默认；外部 API 作为显式开关兜底**；（d）本次未授权/未提供 | 决定质量上限与成本模型 |
| **D3** | RAG 语料范围 | (a) 仅项目/代码知识（`docs/` 11 md + README）+ **电影目录结构化卡片**（B-①）；(b) 加电影**剧情**语料（需外部简介导入 + 合规） | **(a)** 首版 | **按建议（a）**；电影剧情语料仍留 Option C | 决定 B 的交付物与 README 措辞修改 |
| **D4** | 向量存储 | (a) `SimpleVectorStore`（内存）；(b) 新增向量库容器 | **(a)** 首版 | **按建议（a）**：首版 `SimpleVectorStore`（内存）；向量库容器留 Option C | 决定 compose 复杂度与重启语义 |
| **D5** | 首个 AI 交付范围 | (a) 只做 NL 搜索；(b) NL 搜索 + 文档 RAG + **电影目录语义检索**（不含推荐）；(c) 全做 | **(b)** | **按建议（b）**：首个交付 = **B-① 电影目录语义检索 + B-② 文档知识 RAG（带引用）**；推荐系统放 Option C | 决定 M4 规模与工作量 |
| **D6** | 是否现在创建 GitHub issue/milestone，并授权 | (a) 建 1 个 Option A 迁移 issue（先）；(b) 建 A + B；(c) 先不建 | **(a)** 或 **(b)** | **按建议（a）**：先建 **Option A 迁移 issue**，由**主 agent / `github-operator`** 执行；**创建与否以主 agent 最终确认为准**（未确认：是否同时建 B 的 issue）→ **✅ 已于 2026-10-19 执行：只建了 Option A 一个 issue = [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)）；B/B-API/C 的 issue 后续再立** | 决定工作流起点 |
| **D-embedding** | 是否接受"**必须用多语言 embedding 并先实测中→中/中→英**" | (a) 接受多语言 + 实测；(b) 单语；(c) 外部 API embedding | **(a)** | **按建议（a）**：必须用**多语言 embedding**并先实测中→中/中→英；候选 `bge-m3` / `multilingual-e5-*` / `qwen3-embedding`，**以 Ollama 实际可用性为准** | 决定检索质量与语料策略 |
| **D-语料构成** | 语料 = `docs/`（**是否含本路线图 6 份 `ai-*.md`**）+ `README.md` + **电影结构化卡片**？ | (a) 既有 `docs/` 11 md/38,755 词 + README + 电影卡片（**不含** `ai-*.md`）；(b) 含 `ai-*.md`；(c) 仅 `docs/architecture/` + README | **(a)** | **按建议（a）**：首版语料 = 既有 `docs/` **11 个 md（不含 `ai-*.md`）** + `README.md` + **电影结构化卡片** | 决定语料规模/chunk 数/embedding 时间/重建成本 |
| **D-API 优先** | 是否先用**外部 API** 完成 B(质量/速度最优),本地模型只作可选演练? | (a) 本地优先;(b) 外部 API 优先;(c) 本地默认 + 外部兜底 | **(c)** 默认 | **按建议（c）**：本地默认 + 外部 API 兜底（同 D2）；**外部 API 启用时按 D7 已确认的硬上限**（默认关闭；启用时每日 $2 且 ≤200k tokens，可调） | 是否绕开 8 GiB 硬件上限;决定成本模型 |
| **D-AI 端点登录** | AI 端点(RAG/搜索)是否要求登录? | (a) 需登录(零行为变更);(b) 允许匿名只读 | **(a) 需登录（零行为变更）** | **✅ 已确认（a）（2026-10-19）**：AI 端点**要求登录**，沿用 `CustomerAuthenticationFilter` 对 `/api/boot/**` 的默认保护，**零行为变更**；匿名只读**未批准**（若需匿名只读须单独批准，属行为变更） | 决定可演示性与展示方式 |
| **D-语料指纹** | 语料变更后是否需要**增量重建 + 语料指纹**? | (a) 需要;(b) 不需要 | **(a)** | **✅ 已确认（a）需要（2026-10-19）**：记录**文档数/块数/构建时间/哈希**；**响应或日志携带指纹**，保证评测可复现（具体字段与位置实施时落地） | 决定可复现性与索引重建成本 |

**另需用户明确的次要问题（含"需用户给数"的项）—— 已回填用户决定**：

- **D7（token 成本政策）**：**已确认（用户，2026-10-19）** —— 外部 API 仅作**显式开关**兜底（D2/D-API 优先）且**默认关闭**；**启用时默认每日上限 $2 且 ≤200k tokens（可调）**。**仍属默认值**：预算数值本身仍可由用户后续调整；**启用外部 API 的开销与硬上限落地方式（计数/拒绝行为）仍需实施时验证**。
- **D8（演示质量下限）**：**已确认（用户，2026-10-19；原为建议默认值）**：① 目录语义检索 **top-5 命中率 ≥80%**；② RAG **引用正确率 ≥95%**；③ NL→JSON **schema 合规率 ≥95%**；④ **p95 端到端延迟目标先实测后定**（首版以"可演示"为准）。**仍为未实测项**：以上比例为验收基准，但**达成与否必须由实施时的实测证据判定**；p95 在实测前**没有数值目标**。
- **D9（磁盘占用）**：**接受** —— chat 模型约 1–2 GB + embedding 模型约 0.3–1 GB；**实施前需清理磁盘**（当前 22 GiB 可用）。
- **D10（夜间评测 job）**：**已确认：要（用户，2026-10-19）** —— 非阻塞评测 job，**M4.2 后接入**；**CI 分钟预算与是否保留历史趋势仍为未确认项**（§13-U-9）。
- **P-2（README 措辞）**：**已批准** —— 修改 `README.md` roadmap 第 3 项，区分**电影目录语义检索（可做）**与**电影剧情问答（需外部语料）**并写明语料范围；由 `worker` 执行。**证据（docs-writer 只读核对，2026-10-19）**：工作区 `git status` 显示 `README.md` 已修改（`M`，**未提交**），改动与批准方向一致（roadmap 第 3 项拆为 NL 搜索 / 目录语义检索 / 文档 RAG（语料 = `docs/` 11 md + README）/ 剧情问答 out of scope）；**主 agent 已回传 `worker` 完成实施，提交随 §12.3-A-3 的 PR 一并处理**。
- **D-L1（安装授权）**：**已授权** —— 允许在**实施阶段**安装 Ollama 并下载模型（含磁盘清理方案，当前 22 GiB 可用）；**本次仍不安装**。
- **审计日志字段（P2-17，Option C）**：**已决定** —— 默认**不记录**用户问题原文（仅长度/哈希）；**保留期待定**；字段白名单见 Option C §4.6。
- **D6 补充**：**先建 Option A 迁移 issue**（由主 agent / `github-operator` 执行）→ ✅ **已完成（2026-10-19，[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)）**；**范围限定 = 只建 Option A**，B/C 后续再立；worker 仍不得自行建 issue。

> **口径提醒（2026-10-19 更新）**：D7/D8 的数值**已经用户确认**，可作为实施与评测基准引用（并可被 issue 草稿引用）；但**确认 ≠ 已实测达标**：D8 各项仍须在实施时给出实测证据（**p95 仍无目标值**），D7 的 $2/日 与 200k tokens 仍为**可调默认**，语料指纹的具体字段未定。其余决策项（D-A3/D-A4/D-A5/D-A7、D-B5、D-C*、D-L6/D-L7 等）**仍未逐条裁定**。

#### 决策顺序（已按此顺序完成裁定，2026-10-19）

```
D-时间（每周可投入多少小时 / 目标期限）  → 用户答：>10 小时/周（A → B → C）
   → D1（是否接受 Boot 3 迁移）        ← 已接受（否则 B/B-API/C 全部作废）
   → D-API 优先 / D2 / D7（本地 vs 外部 API、是否兜底、成本政策） ← 已定本地默认+外部兑底；**D7 数值 = 默认关闭；启用时 $2/日 且 ≤200k tokens（2026-10-19 确认）**
   → D-语料构成 / D-embedding（语料范围与检索质量基础设施）      ← 已定 (a)+(a)
   → D4（向量存储）/ D5（首个 AI 交付范围）                      ← 已定 (a)/(b)
   → D6（是否立项建 issue/milestone）                           ← ✅ 已建 Option A issue（[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)，2026-10-19）
```

**为什么是这顺序**：D-时间 是第一约束（决定选 0/A/B/B-API/C 哪个）；D1 是硬门槛；D-API/本地决定项目会不会被 8 GiB 硬件卡死；语料与 embedding 决定 B 的验收能否达标；D4/D5 决定实现形态；**立项放最后**，避免为未定型的方案建 issue。

**第二轮补充裁定（2026-10-19，用户经主 agent 转达）**：在第一轮路线级裁定之后，用户进一步确认了 5 项**默认值级**决策（不改变 A → B → C 路线）：**D7**（默认关闭；启用时 $2/日 且 ≤200k tokens）、**D8**（top-5 ≥80% / 引用正确率 ≥95% / NL→JSON ≥95% / p95 先实测后定）、**D10**（夜间评测 job = 要，非阻塞）、**D-AI 端点登录**（需登录）、**D-语料指纹**（要）。

---

## 12. 已批准事项与待办

### 12.1 已批准（用户，2026-10-19）

| # | 事项 | 批准内容 |
| --- | --- | --- |
| P-1 | 本路线图 6 份文档的正式批准 | **已批准**：用户整体批准路线图（5 个版本 + 本地部署专项）及 §11 逐条决策；实施按 **A → B → C**（C 可裁剪；B 本地默认 + 外部 API 兜底）；首个工作流 = **Option A 迁移** |
| P-2 | **修改 `README.md` roadmap 第 3 项措辞**：区分 **电影目录语义检索（可做，B-①）** 与 **电影剧情问答（需外部语料与合规，属 Option C）**，并写清语料范围 | **已批准**（`README.md:5-10`）；由 `worker` 执行；**✅ `worker` 已完成，改动仍为未提交状态，待随 PR 提交**（主 agent 回传「worker 已完成」；docs-writer 只读核对：工作区 `README.md` 为 `M`，diff 与批准方向一致）→ 提交见 §12.3-A-3 |
| P-4' | **D8 阈值的建议默认值**（见 §11 次要问题） | **已由用户于 2026-10-19 确认**（同 **P-9**）：目录 top-5 命中率 ≥80% / RAG 引用正确率 ≥95% / NL→JSON schema 合规率 ≥95% / **p95 延迟先实测后定**；**仍为待实测达标项** |
| P-8 | **D7 外部 API 上限（默认值）** | **已确认（用户，2026-10-19）**：外部 API **默认关闭**；启用时默认**每日 $2 且 ≤200k tokens**（**可调**）；具体计数/拒绝实现待实施时验证 |
| P-9 | **D8 质量下限** | **已确认**：目录语义检索 **top-5 命中率 ≥80%**；RAG **引用正确率 ≥95%**；NL→JSON **schema 合规率 ≥95%**；**p95 延迟先实测后定**（首版以「可演示」为准）；**阈值确认 ≠ 已达标** |
| P-10 | **D10 夜间评测 job** | **已确认「要」**：**非阻塞**，**M4.2 后接入**；**CI 分钟预算/历史趋势保留仍待定**（§13-U-9） |
| P-11 | **D-AI 端点登录** | **已确认「要求登录」**：沿用 `CustomerAuthenticationFilter` 对 `/api/boot/**` 的默认保护，**零行为变更**；匿名只读未批准 |
| P-12 | **D-语料指纹** | **已确认「要」**：记录**文档数/块数/构建时间/哈希**；**响应或日志携带指纹**，保证评测可复现（字段待实施时落地） |
| P-3 | 创建 GitHub issue（Option A / B，见 D6） | **已执行（Option A）**：**#44** 已于 2026-10-19 创建（milestone #3 `Phase 3 — AI-powered platform`；labels 空、state=OPEN〔已于 2026-10-21 关闭为 CLOSED / completed〕）→ 见 §12.3-A-1（已完成）与 `docs/issues/issue-44-boot3-java17-jakarta-migration.md`；**Option B/B-API/C 的 issue 仍未创建（当前 open issues = 0）** |
| P-5 | 把 AI 相关跨 issue 约定沉淀到 `docs/architecture/ai-integration-contracts.md` | **待批准（批准时机 = Option A/B 实施后）** —— 未因本次整体批准而提前生效（仍建议实施后由主 agent 批准再新增） |
| P-7 | **6 份文档的长期归属（P2-4，oracle 建议）** | 不变：① 一次性版本方案（本 6 份）留在 `docs/plans/`，issue 创建后按 `issue-<编号>-<slug>.md` 重命名；② 跨 issue 持续有效的约束在 **Option A/B 实施后**沉淀到 `docs/architecture/ai-integration-contracts.md` |
| P-6 | 文档命名正式化 | 现有 `ai-*.md` 为**临时命名**；Option A issue **已于 2026-10-19 创建（#44）**，重命名为 `docs/plans/issue-<编号>-<slug>.md` 的**触发条件已到**——**本次未执行重命名**（主 agent 未下达该指示；`ai-*` 下 6 份文档路径在全文与其他文档中已被大量引用，改名需单独一轮一致改动）|

### 12.2 原「待用户给数」项（2026-10-19 已确认默认值；保留历史说明）

> **历史说明**：下列 5 项原列于本节标题为「待用户给数」的表中，**2026-10-19 用户经主 agent 转达确认了它们的默认值**，现已作为已确认项移入 **§12.1（P-8..P-12）**。本表保留原条目、原建议值与确认结果，用于追溯「确认前状态」；**本节不再表示任何待办**。

| # | 原待给数项 | **2026-10-19 确认结果** | 原建议默认值（历史记录） |
| --- | --- | --- | --- |
| D7 | 外部 API 的**每日 token/金额硬上限** | **已确认**：默认关闭；启用时默认每日 **$2** 且 **≤200k tokens**（可调） | 同左（原为「建议默认，待确认」） |
| D8 | AI 演示**质量下限** | **已确认**：目录 top-5 ≥80%、RAG 引用正确率 ≥95%、NL→JSON 合规率 ≥95%、**p95 先实测后定** | 同左 |
| D10 | 夜间评测 job 是否需要（CI 分钟数） | **已确认：要**（**非阻塞**，M4.2 后接入）；分钟预算仍待定 | 原为「未裁定」 |
| D-AI 端点登录 | AI 端点是否允许匿名只读 | **已确认：要求登录**（零行为变更） | 原为「未逐条裁定；暂按 (a) 需登录」 |
| D-语料指纹 | 是否需要语料指纹/增量重建 | **已确认：要**（文档数/块数/构建时间/哈希） | 原为「未逐条裁定；暂按 (a) 需要」 |

> **保留的未确认项**：D10 的 **CI 分钟预算/历史趋势**（§13-U-9）、D7 的 **预算数值是否需再收紧**、D8 的 **p95 目标值（待实测）**、语料指纹**具体字段**——均**未因本条确认而消除**。

### 12.3 待主 agent / `github-operator` / `worker` 执行的动作

| # | 动作 | 负责人 | 状态 |
| --- | --- | --- | --- |
| A-1 | **D6 立项**：按发布草稿创建 **Option A 迁移 issue**（草稿 → oracle 风险关卡 → 主 agent 批准 → `github-operator` 创建） | 主 agent / `github-operator` | **✅ 已完成（2026-10-19）**：**#44** `Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace` = https://github.com/PNZ-z/ai-powered-Fabflix/issues/44 ；milestone **#3 `Phase 3 — AI-powered platform`**（state=open）、labels 空、state=OPEN、created `2026-10-19T07:06:27Z`。跟踪文档：`docs/issues/issue-44-boot3-java17-jakarta-migration.md`（原草稿 `proposed-option-a-boot3-migration.md` 已改名，记录远端链接与发布记录）。**立项≠实施**：迁移本身全部未开始（**2026-10-21 更新**：迁移已由 S1–S7 实施并合并、**epic #44 已 CLOSED**，见顶部「执行状态」与 §6 的 M3 行；本行只记录 A-1 的立项动作） |
| A-2 | **P-2 README 回填**：改 `README.md` roadmap 第 3 项措辞（区分目录检索/剧情问答 + 语料范围） | `worker`（执行）→ 主 agent 回传 | **✅ worker 已完成，待随 PR 提交**：工作区 `README.md` 为**未提交**改动（与批准方向一致，docs-writer 只读核对：roadmap 第 3 项已区分 NL 搜索/目录语义检索/文档 RAG（语料 = `docs/` 11 md + README）/剧情问答 out of scope）；**提交与 PR 由主 agent / `github-operator` 在 A-3 一并处理** |
| A-3 | **提交/推送/PR**：本 6 份文档、`README.md` 回填、#44 跟踪文档及后续实施分支的 Git 操作 | 主 agent（提交推送）+ `github-operator`（PR） | ~~**待执行**~~；本次 docs-writer 任务**只改 `docs/`，不提交、不推送、不建/改 GitHub 内容**。**【2026-10-21 回填】→ **已全部完成**：该动作按“每步一个 PR”执行 —— Option A 的 7 个实施分支已随 PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)、[#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)、[#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56)、[#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)、[#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)、[#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59)、[#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) 全部 MERGED；`main` 最新 merge = **`c1067f7`**（PR #60）。本次 docs-writer 关闭回填任务**只改 `docs/`**，不提交、不推送、不做 GitHub 写操作。 |
| A-4 | 把本次批准与立项记录写入文档后的**提交与推送** | 主 agent | ~~**待执行**；**仍未提交/未推送**：6 份 `ai-*.md`、`README.md` 回填、`docs/issues/issue-44-boot3-java17-jakarta-migration.md`（前 6 份与 issue 文档在 `git status` 中仍为未跟踪文件）~~ → **已完成**：上述文档随 PR [#45](https://github.com/PNZ-z/ai-powered-Fabflix/pull/45) 合并（`main` @ `fff314f88455216122912f07c26706f70dd63b62` = `Merge pull request #45`；见 `docs/logs/2026-10-20-issue-46-baseline.md` 元数据） |
| A-5 | 实施阶段安装 Ollama + 下载模型（D-L1 已授权，含磁盘清理） | 主 agent 指派 | **未开始**（本次不安装） |

---

## 13. 未确认事项 / 已知不确定

| # | 未确认项 | 影响 | 如何确认 |
| --- | --- | --- | --- |
| U-1 | Spring AI 1.0.x 与 2.0.x **精确**对应的 Boot 小版本（仅 1.1.8 = Boot 3.5.15 实测） | 选版本线 | 实施时 `curl` 核对 pom 或依赖树 |
| U-2 | 本机 **JDK 27**（Homebrew Maven）**不受 Spring Boot 3.5 支持**（Boot 3.5 早于 JDK 27；其测试/字节码增强链对未知 class file 版本会直接失败）→ 迁移期"本地 vs CI"不可比 | 本地开发体验；A4（Boot 3 行为差异）能否被定位 | **已升级为 Option A 的 blocking 前置（P1-3）**：本地固定 **JDK 21**（`JAVA_HOME`/toolchain），写入 README，验收命令含 `mvn -version`；CI/镜像同版本（见 `ai-option-a-boot3-migration.md` §3/§4.4） |
| U-3 | 8 GiB 上各模型的**实测** tokens/s、内存占用、上下文可用长度 | Option B/C 选型 | 部署专项文档中的测量方法 |
| U-4 | 外部 embedding/LLM 的 token 单价 | 成本模型 | 用户决策 D2/D7 后再评估 |
| U-5 | `SimpleVectorStore` 的**索引重建成本**（文档变更后重新 embed 全量 `docs/` 的耗时） | 语料更新流程 | 实测（embedding 本地） |
| U-6 | Boot 3 迁移后哪些依赖不兼容（jasypt 1.9.3、byte-buddy、surefire、JaCoCo 0.8.15 等） | Option A 工作量 | 依赖兼容性核查（Option A 文档 §设计 列为第一步） |
| U-7 | `README.md` 的 "evaluated" 是否对用户意味着**必须**有 golden set 进 CI | M4/M5 验收 | **部分已答复**：D8 阈值已确认、D10 已确认加**非阻塞**夜间 job；但「**阻塞式 CI 闸门**是否必须」**仍待确认**（当前方案：PR 路径不调真实模型，评测不进 PR 阻塞路径） |
| U-8 | 本分支工作区有未跟踪 `.idea/`（`git status`） | 提交卫生 | 主 agent 决定是否加入 `.gitignore`（**不在本文档范围**） |
| U-9 | **夜间评测 job 的 CI 分钟预算与历史趋势保留方式**（D10 只确认了"要加"，未给数字） | 是否可长期保留 nightly 结果 | 实测首轮耗时后回填；必要时由用户定上限 |
| U-10 | **D7 预算的计数/拒绝实现**（$2 与 200k tokens 如何计量、超限行为）与**D8 阈值能否达标**（含 p95 目标值） | B/B-API 的可演示性与成本安全 | 实施时实测；p95 在实测前**无目标值** |
| U-11 | **语料指纹的具体字段与暴露位置**（响应头/响应体/日志） | 评测可复现性的可验证性 | 实施 M4.1/M4.2 时定稿并文档化 |

> **已删除**的原 U-8（`@SpringBootTest` 计数矛盾）：经复核为 **B2**，结论是**全仓库 0 处使用**（唯一命中为 `SpaFallbackTest.java:32` 的注释），契约文档 §7 表述正确。

> **批准不消除风险（2026-10-19 重申，2026-10-19 第二轮回填后仍然有效）**：上表 U-1..U-11 以及全文的估算/未实测项（含硬件上限、embedding 中文能力、模型质量、成本、闸门余量、**D8 阈值的达标情况与 p95 目标值、D7 预算实现、语料指纹字段**）**因本次批准与默认值确认而全部保留**；批准只确定**路线、实现方向与验收基准数值**，不把任何未实测项转为已证实事实，也不意味着 AI 功能已实现、依赖已安装、测试/基准已运行。

---

## 附录 A：证据索引（供 oracle 核对）

| 结论 | 证据 |
| --- | --- |
| Boot 2.7.18 / Java 11 | `pom.xml:23`、`pom.xml:14-15` |
| **`javax.servlet` = 11 个文件必须迁移**（commerce 5 / auth 2 / admin 2 / search 1 / session 1）；**宽口径 `javax.` = 15 个文件**（另 4 个是 JDK 自带：`javax.sql.DataSource` 1 个 + `javax.xml.parsers` 3 个，**不迁移**）；`jakarta.*` = **0** | 本轮复核：`grep -rl "javax\.servlet" src/main/java` = **11**；`grep -rl "javax\." src/main/java` = **15**；`grep -rl "jakarta\." src/main/java` = **0**（B1；scout §A2 为宽口径） |
| 守卫测试禁止 `jakarta.servlet` | `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java:20-42` |
| Spring AI 1.1.8 → Boot 3.5.15 | scout §A5（`curl repo1.maven.org` 的 starter pom） |
| CI Java 11 / Node 22 | `.github/workflows/ci.yml`（scout §A4） |
| 运行镜像 Java 11 | `Dockerfile`（scout §D18） |
| 8 GiB / 22 GiB / Mac14,7 | scout §C11（`sysctl`、`df -h`） |
| Docker Desktop 3.826 GiB | scout §C11（`docker info`） |
| 未安装推理软件 | scout §C12（`which`、`ls /Applications`、`~/.ollama`） |
| MySQL 8.4 无 vector | scout §C14 |
| `SimpleVectorStore` 存在 | scout §C14 |
| movies 无语料 / 21087 行 | `createtable.sql:5-10`；scout §B7 只读 SQL 计数 |
| `docs/` 语料：既有 **11 个 md / 38,755 词**；6 份 `ai-*.md` = **15,941 词**；含二者的 **17 个 md = 54,696 词** | 本轮复核：`find docs -name '*.md' -not -name 'ai-*.md' \| wc -w` = 38,755（11 个文件）；`cat docs/plans/ai-*.md \| wc -w` = 15,941；全部 17 个 md = 54,696（B4；scout §B9 为宽口径 `docs/`） |
| `@SpringBootTest` **全仓库 0 处使用**（唯一命中为 `SpaFallbackTest.java:32` 的**注释**） | 本轮复核 `grep -rn "@SpringBootTest" src/test/java`（B2）；契约文档 §7 |
| JaCoCo **0.90**（2026-10-20 收紧） / **988/1078 = 0.9165（未覆盖 90 行）** / 134 tests；**不放宽闸门时最多还能新增 ≈19 行未覆盖代码**（`988/(1078+N) ≥ 0.90 ⇒ N ≤ 19`）。旧口径 0.85 / 968/1078 = 0.8980 / 127 tests / ≈60 行为收紧前快照 | `pom.xml:158-159`（阈值）、`pom.xml:163`（check）；契约文档 §7；**B3（口径订正）**；**2026-10-20 门槛收紧**（`docs/logs/2026-10-20-coverage-threshold-90.md`） |
| 前端 thresholds.lines=**90**（2026-10-20 收紧；旧值 88） / 92.35%（846/916）/ 195 tests / 12 e2e | `frontend/vitest.config.js`；契约文档 §7/§12 |
| 无 GitHub issue/milestone（**2026-10-18 调查时点**） | scout §D20（`gh issue list`、`gh api .../milestones`）；**2026-10-19 已变更为：open issue #44 + milestone #3**（见 §2.6） |
| `compose.yaml` = `mysql` + `app` 两个服务 | `compose.yaml:2,26`；`ai-option-c-agent-platform.md` §1.2（P2-1） |
| AD-1 / AD-2 | `docs/architecture/frontend-migration-contracts.md` §10 |
| 错误/响应约定 | 契约文档 §6 |
| `apiClient` 契约与 AI 挂载点 | 契约文档 §5；scout §A6 |
| `/playground` 现成公共页 | scout §A6（`PlaygroundPage.jsx:1-90`） |

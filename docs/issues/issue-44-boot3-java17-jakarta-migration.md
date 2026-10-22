# Option A — Spring Boot 3.x / Java 17 / Jakarta 迁移（GitHub issue **#44** 跟踪记录）

> **✅ 状态：已关闭（CLOSED / reason=completed，2026-10-21）** —— epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) 已由主 agent **关闭**：完成评论 = [#44 (issuecomment-6334171353)](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353)（`2026-10-22T06:17:51Z` UTC），`closedAt` = **`2026-10-22T06:17:55Z`** UTC（本机 `date +%F` = **2026-10-21** PDT）。远端正文 **`## Steps` 的 7/7 checkbox 已全部勾选**（#46–#52；只读核验 `gh issue view 44`：`- [x]` = 7、其余 `Scope`/`Acceptance criteria` 的 `- [ ]` = 21 **按原样保留、未改动**）。**S1–S7 全部完成并已合并**：S7 收尾 = PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60)，merge **`c1067f7`**（`2026-10-22T06:14:50Z`），**#52 已 CLOSED**；`main` 最新 merge = `c1067f7`，main CI **SUCCESS**（主 agent 核验）；**当前 open issues = 0**（`gh issue list --state open` 返回空数组）；milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) **保持 OPEN**（`open_issues=0` / `closed_issues=8`），留给后续 **B / B-API / C**。**最终形态**：Spring Boot **3.5.15**、`maven.compiler.release=17`（字节码 major **61**）、构建工具链 **JDK 21**、Jakarta（**11 文件** allow-list）；后端 **134 tests / 988÷1078 = 0.9165 @ 0.90**、前端 **195 tests / 92.35% @ 90 / 12 e2e**、生产容器 smoke 全绿。**已知覆盖缺口（不因关闭而消失）**：`AdminRepository` **0/39**、`GoogleRecaptchaVerifier` **0/17**，真实浏览器多标签/并发/视觉对比度/连接池语义**未自动化**（S6 证据 §0.7）。**⚠️ 本文件 `## GitHub issue body` 分区仍保留立项时的 `- [ ]` 文本（按任务要求不改原 body 区），与远端的 `- [x]` 已不一致，差异见 `## Internal tracking` → 「epic 关闭记录」**。下方为 2026-10-19 立项与 2026-10-20/21 实施期的历史记录。
> **🟢 〔历史，2026-10-19 立项时〕状态：epic（已创建 7 个子 issue，保持 OPEN）** —— 2026-10-19 用户授权后，`github-operator` ① 创建子 issue **#46–#52**（milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)，labels 空，正文含 `Part of #44`，均 **OPEN**）；② 按下方 **6 项清单**编辑本 issue 正文，把 #44 转为 **epic / tracking issue**（**state 仍为 OPEN**；标题/milestone/labels 均未变）。本 issue 因此**只承载范围与总验收契约**。
> **〔历史，2026-10-19〕「立项 ≠ 完成」**：本 issue 只表示迁移工作已立项并已拆分；Boot 3.x / Java 17 / Jakarta 的**代码与配置改动、测试与验证全部尚未开始**（无源码/配置/测试改动，无 PR），**AC-1..AC-11 一条都未取证**。**（该表述已于 2026-10-21 失效：S1–S7 全部完成并合并，epic 已 CLOSED，见本节第一行。）**
> **实施进展（2026-10-20，本节部分取代上一句）**：**S1（#46）基线已测量并已合并**（PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)，merge `9696848`）、**S2（#47）已实施并已合并**（PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)，merge `55f8d79`）—— `pom.xml` 的 `com.mysql:mysql-connector-j` **8.3.0 → 9.7.0**（1 行）另加 `docs/`；**S3（#48）已实施**（分支 `codex/issue-48-core-boot3-jakarta`，18 文件：Boot **3.5.15** + `maven.compiler.release=17` + Jakarta + 守卫白名单 + `ci.yml` JDK 21 + `Dockerfile` 两行 `FROM`；JDK 21 下 **134 用例 / 0.9165 / major 61**、容器 smoke 全绿；证据 `docs/logs/2026-10-20-issue-48-core-upgrade.md`），**已随 PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 合并（merge `70fb78b`；**#48 已 CLOSED**；CI 双 job SUCCESS）**；**S4（#49）已实施并已合并**（PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)，merge `9a7705c`；**#49 已 CLOSED**；CI 双 job SUCCESS）—— 分支 `codex/issue-49-jdk21-toolchain`，`pom.xml` 主 `<build>` 新增 `maven-enforcer-plugin:3.5.0` 的 `require-jdk-21`（`validate` / `fail=true` / `requireJavaVersion [21,22)`）+ `README.md` 新增 `## Java toolchain`；证据 `docs/logs/2026-10-20-issue-49-jdk21-toolchain.md`；**S5（#50）已实施并已合并**（PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)，merge `ff57e79`；#50 已 **CLOSED**；CI 双 job **SUCCESS**）—— 分支 `codex/issue-50-mockito-annotation`（基线 `9a7705c`），9 个测试文件 / **11 处** `@MockBean`→`@MockitoBean` + 9 个 import（`+20 −20`），JDK 21 `verify` **134 用例 / 0.9165**，clean 构建 `MockBean` 弃用 **11 → 0**（整份日志弃用 **13 → 2**，剩 2 条属 S6）；证据 `docs/logs/2026-10-20-issue-50-mockito-annotation-cleanup.md`；**S6（#51）已执行（2026-10-21），待 PR 合并**（纯证据步骤，无源码/配置/测试改动）—— 基线 `ff57e79`，后端 **134 / 0.9165 / `excludes` 8**、前端 **195 / 92.35% / 12 e2e**、生产 smoke 全绿（10 深链 200 + 401/404/SPA fallback + `changeSessionId` + 员工 logout 204 且保留 `user` + 日期双源一致）；证据 `docs/logs/2026-10-21-issue-51-regression-proof.md`；**S6 已合并**（2026-10-21 回填：PR [#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59)，merge `b4a6d2a`；#51 已 **CLOSED**；CI 双 job SUCCESS）；**S7（#52）已合并（2026-10-21 回填：PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60)，merge `c1067f7`；#52 已 **CLOSED**；仅 `docs/`；日志 `docs/logs/2026-10-21-issue-52-close-out.md`）**。因此上面「全部尚未开始 / AC-1..AC-11 一条都未取证」在 **S3 已合并、S4 已实施**之后**已不再成立**：AC-1/AC-2/AC-3/AC-4/AC-9/AC-10 与 AC-11 的工具链（字节码 + 两阶段镜像）部分已有 S3 证据（AC-10 的 CI 双 job 由 PR #56 取证），AC-11 的 enforcer/README 部分由 **S4（已合并，PR #57）** 承担；**AC-5/AC-6/AC-7 已由 S6（已合并，PR #59）取证**，**AC-8 的文档侧已由 S7 完成（PR #60 已合并）**；**AC-6 的 JDBC 条件子句已因驱动升至 9.x 而触发**，其证据由 S2 承担（见下方「AC-6 的 JDBC 条件子句：已触发」）；AC-6 的生产 compose smoke **已由 S6（PR #59 已合并）取证**，但 S6 证据 §0.7 的未执行项（并发/池语义、生产 reCAPTCHA、`AdminRepository` 存储过程往返、跨时区跨日、真实浏览器多标签、视觉对比度）**仍开放**。
> **子 issue（#46–#52，milestone #3、labels 空、正文含 `Part of #44`；**#46–#52 均已随 PR #53/#55/#56/#57/#58/#59/#60 合并并 CLOSED（2026-10-21 回填：S7 #52 随 PR #60 合并 @ `c1067f7` 并 CLOSED）**）**：S1 [#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46) 基线 · S2 [#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47) 依赖就绪 · S3 [#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48) 核心升级 · S4 [#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49) JDK 21 强制 · S5 [#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50) `@MockitoBean` · S6 [#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51) 回归与 smoke · S7 [#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52) 收尾。拆分方案与依赖图见 **`docs/plans/issue-44-migration-steps.md`**（**已批准并已创建子 issue**），子 issue 文档为 `docs/issues/issue-46-baseline.md` … `issue-52-close-out.md`（原名 `proposed-step-*.md`）。
> **epic 编辑不改变已批准的范围与验收**：`## Goal` / `## Background` / `## Scope` / `## Acceptance criteria` / `## Dependency` / `## Risk notes` **逐字未改**，`AC-1..AC-11` 全部保留；子 issue 的创建也不表示任何步骤已实施。
> **已完成的关卡**：① oracle 只读审查（`.pi/artifacts/oracle-issue-44-split-review.md`）→ ✅；② 主 agent 裁定 **B-1、P1-1..P1-8、P2-1..P2-8、C-1..C-4 全部接受** → ✅，已落入拆分总览与 7 份子 issue 文档；③ 用户授权创建子 issue + 编辑本 issue → ✅（2026-10-19）；④ `github-operator` 创建 **#46–#52** 并编辑本 issue 为 epic → ✅（2026-10-19，主 agent 已核验）；⑤ docs-writer 回填本地文档 → ✅（本轮）。
> **授权状态与结果**：（A）创建 7 个子 issue → ✅ **已执行（#46–#52）**；（B）按下表编辑本 issue 正文 → ✅ **已执行**（结果为“2 处单行修改 + 2 个新增区块”，见下方“实际结果”）。下方所有已批准内容仍然有效。
>
> **已执行的 #44 正文 6 项清单（2026-10-19 由 `github-operator` 完成；docs-writer 未改远端）**
>
> | # | 编辑动作 | 具体内容 |
> | --- | --- | --- |
> | 1 | 追加 `## Steps` | 7 个 checkbox（`- [ ]` + 子 issue 链接；**已回填为真实编号 #46–#52**）；**写明“每个 step 一个独立 PR”** |
> | 2 | 追加 `## Step → acceptance mapping` | 拆分总览 §5.3 的表（AC-1..AC-11 → S1..S7） |
> | 3 | 修改 `Rollback` 段（P1-1） | "single PR" → "**one pull request per step**; each step is independently revertible; the Jakarta rewrite and guard-test rewrite are reverted together" |
> | 4 | 修改 `Non-goals` 的 `@MockBean` 句（P1-7 / C-2） | "The core upgrade step (S3) does not migrate `@MockBean`; step 5 (S5) does it as a follow-up step." |
> | 5 | 保留原 Scope / AC-1..AC-11 | **不改**数字与验收，作为总契约 |
> | 6 | **不关闭 #44** | epic 保持 open；待 S7 完成后由主 agent 关闭（或按 milestone 口径保持 open 至 milestone 关闭） |
>
> **子 issue 统一规范**：标题 `[Step N/7]`；**正文**含 `Part of #44`（不是只写在 Internal tracking）；milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)；labels **空**；`Internal tracking` **不发布**；每份正文只描述本步范围。
>
> **实际结果（2026-10-19，`github-operator` 执行；主 agent 核验；证据 `.pi/artifacts/github-operator-issue-44-steps.md`）**
>
> - **新增区块 1：`## Steps`** —— 1 句“七个 step 各自一个独立 PR（one PR per step）/ 每步可独立回退”，后接 **7 个 `- [ ]` checkbox**，每项 `#<n> — [Step N/7] …`，链接逐一核对指向 **#46–#52**。
> - **新增区块 2：`## Step → acceptance mapping`** —— 拆分总览 §5.3 的映射表（英文表述）：AC-1..AC-11 → 主责步骤 / 复核步骤 / 证据形式，含 AC-6 的条件分支（“if S2 concludes keep 8.3.0, the JDBC clause of AC-6 is not triggered”）。
> - **正文内 2 处单行修改**：① `Non-goals` 的 `@MockBean` 句 → “**The core upgrade step (S3) does not migrate `@MockBean`; step 5 (S5) does it as a follow-up step.**”（原句其余文字保留）；② `Rollback` 段 → “**delivered as one pull request per step** … **Each step is independently revertible; the Jakarta rewrite and guard-test rewrite are reverted together.**”（末尾质量门禁句保留）。
> - **未改动**：`## Goal`、`## Background`、`## Scope`（12 条）、`## Acceptance criteria`（AC-1..AC-11）、`## Dependency`、`## Risk notes` 逐字保持；标题、milestone #3、labels（空）、state（OPEN）均未变（编辑后核验：`44 | OPEN | milestone=3 | labels=0`）。
> - **本文件 `## GitHub issue body` 分区的处理**：2026-10-19 按 `github-operator` 已推送到远端的正文（经其与 `gh issue view 44` 比对，仅差 GitHub 追加的尾部换行）**同步为 epic 版本**，因此本文正文**重新与远端逐字一致**（仅差末尾 1 个换行）。**⚠️ 2026-10-20 例外**：本轮把覆盖率门槛口径改为 **0.90 / 90**（用户已批准，独立 PR），而**远端正文仍是 0.85 / 88**（docs-writer 不做 GitHub 写操作）→ **本文件 body 与远端暂时不一致**，待主 agent 授权 `github-operator` 编辑后恢复；详见 `## Internal tracking` → 「剩余待确认」第 10 项。

> **状态：✅ 已关闭（CLOSED / reason=completed，2026-10-21；完成评论见 [`#44` issuecomment-6334171353](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353)）** —— 〔历史：🟢 epic（已创建 7 个子 issue #46–#52，保持 OPEN）〕已在 GitHub 存在（[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)），无需新建。远端 issue 已由 `github-operator` 于 **2026-10-19** 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**；`## Internal tracking` 分区**未发布**），并同日按上述 6 项清单改为 epic（**state 仍为 OPEN**）。**注意「立项 ≠ 完成」**：本 issue 只表示**迁移工作已立项并已拆分**；Boot 3.x / Java 17 / Jakarta 的**代码与配置改动、测试与验证全部尚未开始**（无源码/配置/测试改动，无 PR），AC-1..AC-11 均未取证。**（2026-10-21 更新：**S1–S6 均已合并**（PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53) @ `9696848`、[#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55) @ `55f8d79`、[#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) @ `70fb78b`、[#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57) @ `9a7705c`、[#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) @ `ff57e79`、[#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59) @ `b4a6d2a`；**#46–#51 已 CLOSED**；PR #56–#59 的 CI 双 job 已核验为 SUCCESS，PR #53/#55 的 PR CI 本轮未单独核验）；**S7（#52）已合并（PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) @ `c1067f7`，#52 CLOSED）**；**epic #44 已 CLOSED（2026-10-21，reason=completed）**，**milestone #3 仍 OPEN**（`open_issues=0` / `closed_issues=8`）**）**
> **修订记录（2026-10-19，oracle 只读审查 `.pi/artifacts/oracle-option-a-issue-draft.md` 后；主 agent 裁定「全部接受」）**：闭合 **B-1**（`AC-10` 去掉流程语；`Scope → Docs` 只列**活文档**并声明历史记录不追改）、**B-2**（**写死** `@MockBean` 处置 = 本 issue 不迁移、列入 Non-goals）、**B-3**（补可复现的 JDK 21 固定机制：`README.md` + `JAVA_HOME` + 采纳 enforcer `requireJavaVersion [21,22)`；`AC-11` 增字节码级证据 major version 61；CI step 名一并列为改动点）；采纳 **P1-1..P1-5** 与 **P2-1..P2-9**；回填 **Y-1/Y-3/Y-4**；**Y-2**（milestone 名）当时标为「建议、待用户确认」，**现已确认并创建为 milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**（原「待确认」表述已失效）。
> 本文件是 GitHub issue **#44** 的**规范来源与本地跟踪记录**（由发布草稿 `docs/issues/proposed-option-a-boot3-migration.md` 改名而来）。`github-operator` 只发布下方 `## GitHub issue body` 分区的内容（**`## Internal tracking` 分区不上 GitHub**）；正文包裹在四反引号 `markdown` 围栏内仅是**本地容器**，发布时已去掉围栏行本身，其余内容逐字使用。
> **发布前置门禁：✅ 已全部通过（2026-10-19）** —— oracle 风险关卡（✅，B/P1/P2 已按裁定闭合）→ 主 agent 明确批准（✅）→ 用户授权创建（✅）→ `github-operator` 创建（✅，产出 [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)）。本行保留为过程记录。
> **与拆分提案的关系（口径提示；C-2 已裁定 = (a)）**：本 issue 正文的 `Non-goals` 写死“本 issue 不做 `@MockBean` → `@MockitoBean`”，而拆分把该项列为 **Step 5**。主 agent 采纳 oracle 建议 **(a)**：S5 作为本 issue 的**子 issue**，并把正文该句**改为澄清句**（见上表第 4 项）——属对已批准契约的**澄清**，不减范围、不改行为。**该远端编辑已于 2026-10-19 生效；本文 `## GitHub issue body` 已同步为 epic 版本（与远端仅差末尾换行）**。
> 事实、工作量与验收条件均引用**已批准**的 `docs/plans/ai-option-a-boot3-migration.md`（方案，2026-10-19 批准）与 `docs/plans/ai-roadmap-overview.md`（路线图，§11-D1/D6、§12.3-A-1）。本文件**不声称**任何迁移已完成、任何命令已运行。
> 与 `README.md` roadmap 的一致性：本 issue 属于 roadmap 中 **"Spring Boot"** 现代化范畴的延续（EOL 修复 + Boot 3 + Java 17 + Jakarta），**在 roadmap 中不对应任何 AI 功能项**——第 3 项 "Spring AI"、第 4 项 "Agent workflows" **均不在本 issue 范围内**；迁移完成后**仍然没有**任何 AI 功能。

## GitHub issue title

```
Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace
```

## GitHub issue body

````markdown
## Goal

Upgrade the application from the end-of-life **Spring Boot 2.7.18 / Java 11** baseline to **Spring Boot 3.5.x** with **`release=17`** and the **Jakarta** servlet namespace, so that Spring AI 1.x can be added later without a second migration. This is a behavior-preserving technical migration: URLs, HTTP status codes, JSON shapes, authentication/session semantics and the SPA fallback truth table must not change.

This issue continues the roadmap's **Spring Boot** modernization work (removing the EOL baseline, Boot 3, Java 17, Jakarta). It does not correspond to the roadmap's Spring AI or agent-workflow items: **no AI capability is delivered here**. It is the hard prerequisite for the planned Spring AI / RAG work.

## Background

- `pom.xml` imports the `spring-boot-dependencies` BOM with `spring-boot.version = 2.7.18` and sets the compiler source/target to 11. Spring Boot 2.7 is EOL (OSS support ended 2023-11).
- Spring AI 1.x requires Spring Boot 3.x, Java 17+ and `jakarta.*`. Measured baseline: `spring-ai-starter-model-openai:1.1.8` depends on `spring-boot-starter 3.5.15`. Adding Spring AI to the current stack is therefore not possible; the migration is the prerequisite.
- The architecture guard test `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java` currently **forbids** `jakarta.servlet` in `src/main/java` and forbids `jakarta.servlet-api` in `pom.xml`, so it must be rewritten as part of this migration (see Scope).
- **11 source files** import `javax.servlet` and must move to `jakarta.servlet` (commerce 5, auth 2, admin 2, search 1, session 1):
  - `src/main/java/com/fabflix/boot/commerce/CartService.java`
  - `src/main/java/com/fabflix/boot/commerce/CartController.java`
  - `src/main/java/com/fabflix/boot/commerce/CheckoutController.java`
  - `src/main/java/com/fabflix/boot/commerce/CheckoutTokenService.java`
  - `src/main/java/com/fabflix/boot/commerce/PaymentController.java`
  - `src/main/java/com/fabflix/boot/auth/CustomerAuthController.java`
  - `src/main/java/com/fabflix/boot/auth/CustomerAuthenticationFilter.java`
  - `src/main/java/com/fabflix/boot/admin/EmployeeController.java`
  - `src/main/java/com/fabflix/boot/admin/EmployeeAuthenticationFilter.java`
  - `src/main/java/com/fabflix/boot/search/SearchController.java`
  - `src/main/java/com/fabflix/boot/session/SessionController.java`
- **4 other files must stay on `javax.*`** because those packages are JDK-provided and are not part of the Jakarta EE 9 rename: `javax.sql.DataSource` (1 file: `admin/AdminRepository.java`) and `javax.xml.parsers.*` (3 files: `ActorXMLParser`, `CastXMLParser`, `MainXMLParser`).
- Local Maven currently runs on **JDK 27** (Homebrew). **Spring Boot 3.5.x officially supports Java 17–24**; JDK 27 is outside that supported range, and its ByteBuddy/Mockito chain fails at build/test time on an unrecognized class file version. Fixing the local JDK is a blocking prerequisite of this issue (see Scope and Acceptance criteria).
- Baseline quality gates that must not regress: backend JaCoCo `BUNDLE/LINE >= 0.90` (the threshold was tightened from 0.85 to 0.90 on 2026-10-20 by user approval, in a separate PR; the gate now stands at 988/1078 = 0.9165 with `Tests run: 134`), frontend `lines >= 90` (tightened from 88 on the same date; recorded 92.35%, 195 tests), Playwright e2e (12 executions). The pre-tightening snapshot (968/1078 = 0.8980, 127 tests, `lines >= 88`) is kept only as history in `docs/logs/2026-10-20-issue-46-baseline.md`; the approved plan §6.1 and these tightened numbers are the comparison baseline for this issue.

## Scope

- [ ] **Baseline (first step)**: re-run and record the **pre-migration baseline** — `mvn --batch-mode --no-transfer-progress verify`, and in `frontend/`: `npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e` — and attach the recorded output to the pull request. This fresh baseline is what AC-1/AC-5 are compared against (reference numbers above come from the approved plan, not from a run today).
- [ ] **Toolchain**: compile with `release=17` while running the build on **JDK 21** locally, in CI and in the container image (one JDK version everywhere to remove "green locally, red in CI" drift). The JDK 21 requirement is fixed by two mechanisms: (a) `README.md` states the JDK 21 requirement and that `JAVA_HOME` must point at JDK 21; (b) a **`maven-enforcer-plugin` rule `requireJavaVersion [21,22)`** is added to the same plugin (and the same `validate`-phase execution pattern) as the existing `require-frontend-dist` rule, so a wrong JDK fails the build instead of silently skewing results.
- [ ] **Build**: `spring-boot.version` 2.7.18 → **3.5.x** (patch version decided during implementation); replace `maven.compiler.source/target = 11` with **`maven.compiler.release = 17`**; align plugin/dependency versions with the Boot 3.5 BOM.
- [ ] **Jakarta migration**: `javax.servlet` → `jakarta.servlet` in the **11 files listed above**; the 4 JDK-`javax.*` files stay unchanged; compiler errors are the completeness check.
- [ ] **Architecture guard test**: rewrite `SpringBootOnlyArchitectureTest` — keep the bans on `@WebServlet` / `extends HttpServlet`, and replace the `jakarta.servlet` ban with a **controlled allow-list** implemented as: collect the relative paths of the files under `src/main/java` whose content contains `jakarta.servlet`, and assert **set equality with the expected set of exactly 11 paths** (not "contains", which would let the allow-list silently grow). Add an assertion that **no `jakarta.servlet.annotation.WebServlet` import exists**. Keep the `pom.xml` assertion on `jar` + `repackage`. **The `pom.xml` `jakarta.servlet-api` literal assertion stays in place**: the literal is absent from `pom.xml` today and stays absent, because Spring Boot 3 supplies the API transitively through `spring-boot-starter-web` as long as no explicit dependency is declared.
- [ ] **Dependency compatibility check** (first implementation step): `mysql-connector-j` (currently pinned 8.3.0; Boot 3.5 manages 9.x — if upgraded, JDBC behavior deltas around time zones, `CURDATE()` and SSL connection warnings must be covered by the production smoke test), `jasypt:1.9.3` (**verification item**: 2014-era release — check reflective access on JDK 21 such as `IllegalAccessError` / `InaccessibleObjectException` and any reliance on removed JDK APIs; only `org.jasypt.util.password.*` is used today. **If the password-verification implementation has to be replaced, that is a scope change and must go back to the main agent for a ruling**), `mockito-core` / `byte-buddy` / `byte-buddy-agent`, `spring-boot-starter-test`, `maven-surefire-plugin:3.5.2`, `jacoco-maven-plugin:0.8.15`, `spring-boot-maven-plugin`, `maven-enforcer-plugin`, `maven-resources-plugin`, and `MockMvcBuilders.standaloneSetup` usage in the admin package. Also review the **BOM-managed transitive upgrades** that Boot 3.5 brings in: **HikariCP (4.0.3 → 6.x)**, **Jackson (2.13.5 → 2.19.x)**, **Logback/SLF4J (1.2.12 → 1.5.x)** and **Tomcat (9.0.83 → 10.1.x)**. Connection-pool behaviour and JSON serialization are covered by the AC-6 smoke test; any behavioural difference must be recorded, not silently accepted.
- [ ] **CI**: `.github/workflows/ci.yml` backend job — `java-version: "11"` → **`"21"`** (`ci.yml:32`), rename the **job** `Build and test (Java 11)` → the new JDK version (`ci.yml:20`), **and** rename the **step** `Set up Java 11` → `Set up Java 21` (`ci.yml:28`).
- [ ] **Docker**: `Dockerfile` build stage `maven:3.9-eclipse-temurin-11` → `maven:3.9-eclipse-temurin-21`, runtime stage `eclipse-temurin:11-jre` → `eclipse-temurin:21-jre`, keeping the non-root uid/gid 1001 setup.
- [ ] **Docs (live documents only)**: update `README.md` (local JDK 21 requirement and `JAVA_HOME`, build/run instructions) and `docs/architecture/frontend-migration-contracts.md` — **§8** (the guard-test description: the `jakarta.servlet` ban becomes the 11-file allow-list) and **§12** (container/version conventions: JDK 21 images, CI JDK). **Do not change the coverage thresholds in this issue** — they were already tightened to 0.90 / 90 on 2026-10-20 (separate PR); only keep the documentation consistent with them, and record any newly discovered uncovered class as a known gap instead of silently absorbing it. **Historical workflow logs and closed-issue plan/issue documents are records of past runs and are not rewritten**: the files that still contain `Build and test (Java 11)` or the pre-tightening `85% / 88` figures for historical reasons (`docs/logs/2026-*`, `docs/plans/issue-30..33-*.md`, `docs/issues/issue-30..33-*.md`, `docs/issues/issue-46-baseline.md`) stay as they are.
- [ ] **Verification**: backend, frontend, e2e and production compose smoke (see Acceptance criteria).

## Non-goals

- No Spring AI / `spring-ai-*` dependency (that is a separate, later issue).
- **The core upgrade step (S3) does not migrate `@MockBean`; step 5 (S5) does it as a follow-up step.** `@MockBean` appears in **9 test files (11 annotations)** and has been deprecated since Boot 3.4 (replacement: Spring Framework 6.2's `@MockitoBean`, semantically equivalent). The resulting deprecation warnings are **acceptable and will be recorded** in the completion log; a follow-up issue may address them. This issue must not change those annotations.
- No rewriting of historical workflow logs or closed-issue plan/issue documents (see Scope → Docs).
- No Spring Security, JWT, CSRF or authentication-model change.
- No business-behavior change, new feature or refactor; no URL, status code, JSON shape or error-message change.
- No database schema or data migration.
- No vector store, corpus, recommendation or agent functionality.
- No multi-instance / Spring Session / Redis work.
- No frontend build-chain upgrade (Vite/React/vitest/Playwright are unrelated to the Boot version).
- No restoration of the retired `src/main/webapp` legacy frontend.
- No relaxation of quality gates: the coverage thresholds are the **tightened 0.90 (backend) / 90 (frontend)** values approved on 2026-10-20, the JaCoCo `excludes` list stays unchanged, and no test/e2e/lint step may be skipped. This issue may neither lower nor raise those thresholds.

## Acceptance criteria

- [ ] **AC-1** `mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS; backend test count does not decrease against the pre-migration baseline recorded in Scope step 0 (`Tests run: 127, Failures: 0, Errors: 0, Skipped: 0` is the recorded reference; increases must be explained).
- [ ] **AC-2** No relaxation of the backend coverage gate: **the gate was tightened from 0.85 to 0.90 on 2026-10-20 by user approval (separate PR, before this migration starts)**, and this epic is accepted against the **tightened** threshold — the JaCoCo `verify` check must pass with `BUNDLE/LINE >= 0.90` and print `All coverage checks have been met.` (post-tightening reference: 988/1078 = 0.9165; margin for newly added uncovered lines is only ~19 lines, so new production code needs tests; no `exclude` may be added). **No quality gate may be relaxed anywhere in this epic.**
- [ ] **AC-3** With `frontend/dist` built first (`cd frontend && npm ci && npm run build`), `mvn -Pfrontend -DskipTests clean package` succeeds and `unzip -l target/fablix.jar` contains `BOOT-INF/classes/static/index.html` (packaging shape unchanged).
- [ ] **AC-4** With `frontend/dist` built as in AC-3 and then moved aside, `mvn -Pfrontend -DskipTests package` → BUILD FAILURE from the `require-frontend-dist` enforcer rule (the missing-`dist` guard still fails the build).
- [ ] **AC-5** Frontend unchanged and green: `npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e` → 195 tests pass, Lines >= 90 (**the threshold was tightened from 88 to 90 on 2026-10-20 by user approval; recorded 92.35%**), 12 Playwright executions pass — compared against the fresh pre-migration baseline from Scope step 0.
- [ ] **AC-6** Production compose smoke (`docker compose build app` + `docker compose up -d --no-deps app`) reproduces the existing HTTP semantics: health endpoint 200; `/` contains `id="root"`; the 10 recorded deep links return 200 `text/html`; unauthenticated `/api/boot/cart` → 401 JSON; `/API/boot/cart` → 404 JSON; `/assets/missing.js` → 404; customer login → session/cart 200; employee login → dashboard with `status:success`. If the JDBC driver is upgraded to 9.x, also verify order/sale dates match `sales.sale_date` (no timezone offset / `CURDATE()` drift) and that SSL connection warnings do not become fatal. **This smoke test needs Docker locally and port 8081 free; if the environment is not available, record it explicitly as not executed instead of claiming it passed.**
- [ ] **AC-7** Authentication/session semantics unchanged: `changeSessionId()` on login, 401 body shape, employee logout 204 keeping the `user` attribute.
- [ ] **AC-8** Guard test and documentation agree (`docs/architecture/frontend-migration-contracts.md` §8 describes the allow-list that the code asserts; the allow-list is asserted as **exactly 11 paths**).
- [ ] **AC-9** No new JaCoCo `exclude` entries: the `excludes` list is identical to the pre-migration list.
- [ ] **AC-10** CI passes on both jobs after the change (evidence: the pull request's CI run).
- [ ] **AC-11** JDK consistency and bytecode level: `mvn -version` reports **JDK 21**; the new `maven-enforcer-plugin` `requireJavaVersion [21,22)` rule accepts JDK 21 and rejects JDKs outside that range (evidence: the enforcement output of the JDK 21 run, plus a wrong-JDK run showing BUILD FAILURE if one is performed and recorded); `.github/workflows/ci.yml` sets `java-version: "21"`; both `Dockerfile` stages use a JDK 21 image; and the produced bytecode is Java 17 — **class file major version 61** (e.g. `javap -verbose -cp target/classes com.fabflix.boot.FablixApplication | grep 'major version'` → `major version: 61`).

## Dependency

None. This issue is itself the prerequisite for the planned Spring AI work (Boot 3.x + Java 17+ + Jakarta).

## Risk notes

- **Boot 3 behavior differences** (trailing-slash matching, error-response handling, Tomcat 10) are the main uncertainty; any difference must be recorded explicitly, not silently accepted. `@MockBean` deprecation warnings are expected and accepted (see Non-goals).
- **JDBC driver upgrade** (8.3.0 → 9.x if the version is taken from the BOM) can change time-zone / date-function behavior; covered by the production smoke test in AC-6.
- **BOM-managed transitive upgrades** (HikariCP 4.0.3 → 6.x, Jackson 2.13.5 → 2.19.x, Logback/SLF4J 1.2.12 → 1.5.x, Tomcat 9.0.83 → 10.1.x) can change connection-pool and JSON serialization behaviour in ways unit tests do not see; the AC-6 smoke test covers them, and any difference must be written down.
- **`jasypt:1.9.3` on JDK 21 is a verification item**: reflective access (`IllegalAccessError` / `InaccessibleObjectException`) and any use of removed APIs must be checked. Replacing the password-verification implementation is a **scope change** and requires a main-agent ruling.
- **Local JDK 27 is outside Spring Boot 3.5's supported range (Java 17–24)** and would mask environment problems; the README declaration plus the enforcer `requireJavaVersion [21,22)` rule make a wrong JDK a hard build failure instead of a silent skew.
- **Coverage budget**: the gate is now **0.90** (tightened on 2026-10-20 by user approval, from 0.85) and leaves room for at most **~19 additional uncovered lines** (`988/(1078+N) >= 0.90 ⇒ N <= 19`), so any newly introduced class needs a test and no `exclude` may be added. `docs/architecture/frontend-migration-contracts.md` §7 has been updated to the 0.90 threshold and the current 988/1078 = 0.9165 figure.
- **Guard test must be updated first**, otherwise the migration cannot pass CI.
- **Estimate**: ~1.5–3 weeks for a single developer (estimate, not a commitment); the main variable is hunting down Boot 3 behavioral differences, not the mechanical Jakarta rename.

## Rollback

The migration is **delivered as one pull request per step** with no destructive operations: failure can be reverted wholesale (`git revert` / close the PR) without affecting `main`. **Each step is independently revertible; the Jakarta rewrite and guard-test rewrite are reverted together.** Relaxing any quality gate to make CI green is not an acceptable mitigation and requires explicit approval.

## Steps

Each of the seven steps is delivered as **its own pull request** (**one PR per step**); every step is independently verifiable and independently revertible. The steps are tracked as sub-issues:

- [ ] #46 — [Step 1/7] Record the pre-migration baseline and start the migration checklist
- [ ] #47 — [Step 2/7] Validate non-Spring dependency readiness on the Boot 2.7 baseline
- [ ] #48 — [Step 3/7] Upgrade to Spring Boot 3.5 and migrate to the Jakarta namespace
- [ ] #49 — [Step 4/7] Enforce JDK 21 in the build and document/verify it
- [ ] #50 — [Step 5/7] Replace deprecated @MockBean with @MockitoBean in the tests
- [ ] #51 — [Step 6/7] Prove the migration with full regression and production smoke evidence
- [ ] #52 — [Step 7/7] Close out the migration: architecture docs, roadmap status and workflow log

## Step → acceptance mapping

| #44 AC | Primary step | Re-checked by | Evidence form |
| --- | --- | --- | --- |
| AC-1 `mvn verify` test count does not decrease | S3 | S6 | command output + comparison against the S1 baseline |
| AC-2 coverage gate 0.90 (tightened on 2026-10-20) | S3 | S6 | `All coverage checks have been met.` |
| AC-3 jar shape (`BOOT-INF/classes/static/index.html`) | S3 | — | `unzip -l` output |
| AC-4 missing `dist` → BUILD FAILURE | S3 | S4 (rollback check) | comparison of two build runs |
| AC-5 frontend green and unchanged | S6 | — | the five frontend commands' output + `git diff` proving zero frontend change |
| AC-6 production compose smoke + JDBC semantics | S6 | S2 (JDBC part only) | HTTP status/body + date consistency. **Conditional branch**: if S2 concludes "keep 8.3.0", the JDBC clause of AC-6 is not triggered |
| AC-7 authentication/session semantics | S6 | S3 (unit-test evidence) | smoke + `mvn verify` test cases |
| AC-8 guard test and documentation agree | S3 (code) + S7 (docs) | — | test file + contracts document §8 |
| AC-9 no new JaCoCo excludes | S3 | S6 | `pom.xml` comparison of the 8 entries |
| AC-10 both CI jobs green | S3 (backend job → JDK 21) | S4, S6 | the PR's CI run |
| AC-11 JDK consistency + bytecode 61 | S3 (bytecode / `mvn -version` / both `Dockerfile` stages) + S4 (enforcer/README/image evidence) | S6 | `javap` + `mvn -version` + both stage tags |
````

## Internal tracking

> 本分区**不发布到 GitHub**；`github-operator` 只使用上方 `## GitHub issue body`（**已发布：远端正文与该分区逐字一致，本分区未外泄**；**但 2026-10-20 起正文的门禁数字（0.90 / 90）尚未同步到远端**，见「剩余待确认」第 10 项）。

### epic 关闭记录（2026-10-21）

| 项目 | 值 |
| --- | --- |
| 远端 state / reason | **CLOSED** / **COMPLETED**（只读核验 `gh issue view 44 --json state,stateReason,closedAt`） |
| closedAt（远端 UTC） | **`2026-10-22T06:17:55Z`**（本机 `date +%F` = **2026-10-21**，PDT） |
| 完成评论 | **https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353**（`created_at` = `2026-10-22T06:17:51Z`） |
| `## Steps` checkbox | **7/7 已勾选**（`- [x]` = 7：#46–#52，逐行只读核对） |
| 其余 checkbox | `Scope` + `Acceptance criteria` 的 **21 个 `- [ ]` 逐字未改**（只读核验 `grep -c '^- \[ \]'` = 21） |
| 子 issue | **#46–#52 全部 CLOSED / COMPLETED**（逐个 `gh issue view <n> --json state,stateReason` 核验） |
| 对应 PR | **#53 / #55 / #56 / #57 / #58 / #59 / #60 全部 MERGED**；S7 = PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60)，merge **`c1067f7`**（`2026-10-22T06:14:50Z`，标题 `Close out the Option A migration`） |
| `main` 位置 | 最新 merge = **`c1067f7`**（`git log --merges --oneline -1`）；main CI **SUCCESS**（主 agent 核验） |
| open issues | **0**（`gh issue list --state open --json number` → `[]`） |
| milestone #3 | **仍 OPEN**：`open_issues=0` / `closed_issues=8`（留给后续 **Option B / B-API / C**） |
| 本地 body 与远端的差异 | 远端 `## Steps` 已勾选为 `- [x]`，而本文件 `## GitHub issue body` 区**仍为立项时的 `- [ ]` 文本**（本轮任务明确要求**不改原 body 区**）；该差异已在文件顶部状态行与本表中显式登记，不视为“已同步” |
| 未做 | 本轮 docs-writer **未做任何 GitHub 写操作**（未关闭/编辑 issue、未改 milestone、未建 PR），关闭动作与 CI 核验均由主 agent 执行；本行仅记录其结果 |

### 远端 issue 元数据（2026-10-19 只读核验：`gh issue view 44`、`gh api repos/PNZ-z/ai-powered-Fabflix/milestones`，docs-writer/主 agent）

| 项目 | 值 |
| --- | --- |
| 编号 | **#44** |
| URL | https://github.com/PNZ-z/ai-powered-Fabflix/issues/44 |
| 标题（远端） | `Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace` |
| milestone | **[#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**（number 3，**state=open**，created `2026-10-19T07:05:43Z`，由主 agent 创建；#44 已挂该 milestone） |
| labels | **无（空数组）** |
| state | **CLOSED（reason=completed）**；`closedAt` = `2026-10-22T06:17:55Z`（本机 2026-10-21）；关闭前为 OPEN（2026-10-19 → 2026-10-21） |
| 创建时间（createdAt） | **`2026-10-19T07:06:27Z`** |
| 正文 | 与本文件 `## GitHub issue body` **逐字一致**（主 agent 核验）；`## Internal tracking` 分区**未发布**。**2026-10-19 epic 编辑后**：远端正文 = 原正文 + `## Steps` + `## Step → acceptance mapping` + 2 处单行修改；本文件 body 分区已同步为该版本（与远端仅差 GitHub 追加的尾部换行）。**2026-10-20 起不再逐字一致**：本地 body 已把覆盖率门槛改为 0.90 / 90，远端仍为 0.85 / 88（只读核对 `gh issue view 44`；见「剩余待确认」第 10 项）。**2026-10-21 起又多一处差异**：远端 `## Steps` 已勾选为 `- [x]`（7/7），本文件 `## GitHub issue body` 区**仍为 `- [ ]`**（本轮按任务要求**不改原 body 区**）；已登记于上方「epic 关闭记录」 |
| **当前跟踪状态** | **✅ 已关闭（CLOSED / reason=completed，2026-10-21）**：S1–S7 全部完成并合并（PR #53/#55/#56/#57/#58/#59/#60），epic 由主 agent 关闭（完成评论 [`issuecomment-6334171353`](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353)；远端 `## Steps` 7/7 勾选）；**#46–#52 全部 CLOSED**；**当前 open issues = 0**；**milestone #3 仍 OPEN**（`open_issues=0` / `closed_issues=8`，供后续 B/B-API/C）。**实现进度（2026-10-20 起，以下为历史回填）**：**S1/S2/S3 均已合并**（PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53) @ `9696848`、PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55) @ `55f8d79`、PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) @ `70fb78b`；**#48 已 CLOSED**；PR #56 的 CI 双 job **SUCCESS**）；**S4（#49）已实施并已合并**（PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57) @ `9a7705c`；**#49 已 CLOSED**；CI 双 job **SUCCESS**）；**S5（#50）已合并**（PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) @ `ff57e79`；#50 已 **CLOSED**；CI 双 job **SUCCESS**）；**S6（#51）已合并（2026-10-21 回填：PR [#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59) @ `b4a6d2a`，#51 已 CLOSED）**（证据 `docs/logs/2026-10-21-issue-51-regression-proof.md`）；**S7（#52）已合并（2026-10-21 回填：PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60)，merge `c1067f7`；仅 `docs/`；日志 `docs/logs/2026-10-21-issue-52-close-out.md`）**——**#52 已 CLOSED**；**epic #44 已 CLOSED（2026-10-21，reason=completed）**，**milestone #3 仍 OPEN（`open_issues=0` / `closed_issues=8`）**。本 issue 的验收（AC-1..AC-11）**尚未整体验收**：**AC-6 的 JDBC 条件子句已触发**（证据由 S2 提供，见下方专节）；S3 已对 **AC-1 / AC-2 / AC-3 / AC-4 / AC-9 / AC-10（CI 双 job）/ AC-11（字节码 + 两阶段镜像）** 提供证据，S4 补齐 AC-11 的 enforcer/README 部分（**已合并，PR #57**），S6 补齐 **AC-5 / AC-6 / AC-7 / AC-9 / AC-11 的终态复核**（**已合并，PR #59**；未执行项见其证据记录 §0.7）；**AC-8 的文档侧由 S7 完成（PR #60 已合并）**（详见 `docs/issues/issue-48-core-boot3-jakarta.md` 与 `docs/issues/issue-49-jdk21-toolchain.md` 的回填分区） |

### 本地跟踪表

| 项目 | 值 |
| --- | --- |
| 文档状态 | **✅ 已关闭（CLOSED / reason=completed，2026-10-21）**：完成评论 [`#44` issuecomment-6334171353](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44#issuecomment-6334171353)，远端 `## Steps` 7/7 勾选，#46–#52 全部 CLOSED，open issues = 0，milestone #3 保持 OPEN。〔历史：🟢 epic（已创建 7 个子 issue #46–#52，保持 OPEN）〕；本 issue 于 2026-10-19 由 `github-operator` 按获批草稿创建，同日转为 epic；本文件已从 `docs/issues/proposed-option-a-boot3-migration.md` 改名为 `docs/issues/issue-44-boot3-java17-jakarta-migration.md`（**slug 说明**：选用 `boot3-java17-jakarta-migration`，含编号 **44**，与远端标题的 Boot 3.x / Java 17 / Jakarta 三项对应，并与方案文档 `docs/plans/ai-option-a-boot3-migration.md` 的命名区分开）|
| oracle 审查报告 | `.pi/artifacts/oracle-option-a-issue-draft.md`（只读审查，2026-10-19）：必须修 **B-1..B-3 + P1-1..P1-5**；建议修 **P2-1..P2-9**；待裁定 **Y-1..Y-4**。**裁定结果：全部接受**——B/P1/P2 已全部落入本稿；**Y-1/Y-3/Y-4 已由主 agent 裁定并回填**；**Y-2（milestone 名）仍为建议、待用户确认**（→ **2026-10-19 已闭合：milestone #3 已创建**，见下表） |
| 来源 | ① `docs/plans/ai-roadmap-overview.md`（**已批准**路线图，2026-10-19）：Option A 为路线图首个工作流（§5.1、§11-D1）、立项授权方向（§11-D6）、§12.3-A-1；② `docs/plans/ai-option-a-boot3-migration.md`（**已批准**实施计划，2026-10-19） |
| 关联计划 | `docs/plans/ai-option-a-boot3-migration.md`；上级文档 `docs/plans/ai-roadmap-overview.md` |
| 范围（Y-3 已裁定） | **本次只创建 Option A 一个 issue**。Option B / B-API / C 的 issue **后续再立**；`docs/plans/ai-option-b-spring-ai-rag.md` 已是批准方案，但**本次不立 issue** |
| GitHub 标题（已发布） | `Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace`（**#44 已按此标题创建**；与 `README.md` roadmap 的 **"Spring Boot"** 现代化范畴一致；**不**写成 Spring AI 功能） |
| milestone（已创建） | **#3 `Phase 3 — AI-powered platform`**（**Y-2 已确认并由主 agent 创建**，state=open，`2026-10-19T07:05:43Z`；原先「仅为建议名、待用户确认、未确认前不写入远端」的限定**已随创建失效**）。仓库此前的 `Phase 2 — React Frontend Migration` 已随 #31–#33 关闭 |
| labels（已发布） | **空**（P2-4 已裁定）：仓库既有 issue（#28–#33）均无 label，保持与现状一致；**#44 远端 labels 实测为空数组** |
| 事实来源（可核验） | `pom.xml`（`spring-boot.version` 2.7.18、`maven.compiler.source/target` = 11、`maven-enforcer-plugin:3.5.0` 已有 `require-frontend-dist` 执行）；`.github/workflows/ci.yml`（job name `Build and test (Java 11)` @ `:20`、step `Set up Java 11` @ `:28`、`java-version: "11"` @ `:32`）；`Dockerfile`（`maven:3.9-eclipse-temurin-11`、`eclipse-temurin:11-jre`、uid/gid 1001）；`SpringBootOnlyArchitectureTest.java`；`grep -rl "javax\.servlet" src/main/java` = **11**、宽口径 `javax.` = 15、`grep -rl "@MockBean" src/test/java` = **9 个文件 / 11 处标注**（docs-writer 只读复核，2026-10-19）。**B-1 证据**：`grep -rln "Build and test (Java 11)" docs README.md .github` = **14 个文件**，其中 10 个是**历史记录**（`docs/logs/2026-10-14-issue-30-*.md`、`docs/logs/2026-10-15-issues-31-32-33-*.md`、`docs/plans/issue-30/31/32/33-*.md`、`docs/issues/issue-30/31/32/33-*.md`），需改的是**活文档**（`README.md` 不含该字面量、契约文档也不含该字面量 → 契约文档要改的是 §8/§12 的守卫与容器/版本约定） |
| 未运行/未验证 | docs-writer **未运行** `mvn`、npm、Docker、CI；**立项后仅做只读 GitHub 核对**（`gh issue view 44` / `gh api .../milestones`，2026-10-19，用于回填上方元数据表），**未做任何 GitHub 写操作**。正文中的 127/195/12 用例数、覆盖率 0.8980 / 92.35%、10 条深链、HikariCP/Jackson/Logback/Tomcat 的版本号均**来自 oracle 报告与已批准计划文档的引用**，需实施时复跑/复核确认 |
| 工作量 | **1.5–3 周（估算）**，引用计划 §5 里程碑表（A0–A7）。**拆分后口径（C-4 已裁定 (a)）**：本文件与拆分总览记录“工作量 ≈ 1.5 周 / 日历 ≈ 5–7.5 周（含 7 轮审查）”；**但远端 #44 正文的 `Risk notes → Estimate` 行仍为 “~1.5–3 weeks”，未按 C-4 改写**（见下方“剩余待确认”第 9 项） |

### 已裁定（2026-10-19，主 agent；oracle 建议 → 裁定）

| oracle 意见 | 裁定 | 落地位置 |
| --- | --- | --- |
| **B-1** AC-10 流程语 + Docs 范围会诱导改写历史记录 | 接受：`AC-10` 改为可验证表述；`Scope → Docs` 只列活文档并显式声明历史 logs/已关闭 issue 文档不追改 | 正文 `AC-10`、`Scope → Docs`、`Non-goals` |
| **B-2 / Y-1 / D-A4** `@MockBean` 处置不能留待实施时决定 | 接受：**本 issue 不迁移** `@MockBean`→`@MockitoBean`；弃用警告可接受并记录；后续 issue 处理 | 正文 `Non-goals`（写死）、`Risk notes`；Scope 中原「等价修改清单」条目已删除 |
| **B-3 / Y-4** JDK 21 固定机制与证据 | 接受：`README.md` 声明 JDK 21 + `JAVA_HOME`；**采纳** `maven-enforcer-plugin` `requireJavaVersion [21,22)`（与既有 `require-frontend-dist` 同插件）；`AC-11` 增**字节码级证据**（class major version **61**）；CI step 名 `Set up Java 11` 一并列入改动点 | 正文 `Scope → Toolchain`、`Scope → CI`、`AC-11`、`Risk notes` |
| **P1-5** `jasypt:1.9.3` 风险需具体化为验证项 | 接受（含「若需替换实现则属范围变化，须回主 agent 裁定」） | 正文 `Scope → Dependency compatibility check`、`Risk notes` |
| **P2-3** `jakarta.servlet-api` 字面量断言 | 接受：写死为「**保持成立**」（`pom.xml` 无该字面量；Boot 3 由 starter-web 传递提供） | 正文 `Scope → Architecture guard test` |
| **P2-4** labels | 接受：留空 | 本表「labels（已发布）」行 |
| **P2-7** Rollback 表述 | 接受：单 PR 交付；若按步骤拆分 commit 则每步可独立回退 | 正文 `Rollback` |
| **U-A5** CI job name 引用同步范围 | 已闭合：只改活文档；含字面量的历史 logs 与已关闭 issue 的 plans/issues 不追改 | 正文 `Scope → Docs` |
| **Y-3 / D6** 是否同时建 Option B 的 issue | 接受：**只建 Option A 一个 issue**；B/B-API/C 后续再立 | 本表「范围」行 |

### 剩余待用户/主 agent 确认

| # | 待确认项 | 说明 |
| --- | --- | --- |
| 1 | ~~**milestone 名称与是否新建**（Y-2）~~ | **已闭合（2026-10-19）**：milestone **#3 `Phase 3 — AI-powered platform`** 已由主 agent 创建（state=open），#44 已挂载；「建议名/待用户确认」的限制解除 |
| 2 | **目标 Boot 补丁版本** | 计划 D-A1 = Boot 3.x；建议取 **3.5.x**（与 Spring AI 1.1.x 基线一致），具体补丁版本仍标记为「实施时定」 |
| 3 | **D-A5 依赖显式 pin** | `mockito-core`/`byte-buddy`/`junit-jupiter` 等显式 pin 是否交回 BOM 管理：计划建议「先保持、仅改必需项」，**未逐条裁定** |
| 4 | **是否需要 oracle 复审修订稿** | **已闭合**：#44 已由 `github-operator` 按本文件 `## GitHub issue body` 创建（正文逐字一致，主 agent 核验），发布前**未再引入新的架构/风险选择**；本轮不再需要复审。**注意**：`@MockBean` 明确不迁移、`requireJavaVersion [21,22)` 等收窄口径**仅写入本 issue**，**尚未回填** `docs/plans/ai-option-a-boot3-migration.md`（见「内部备注」末条） |
| 5 | ~~**是否创建 7 个子 issue**（拆分提案）~~ | **已闭合（2026-10-19）**：`github-operator` 已创建 **#46–#52**（milestone #3、labels 空、`Part of #44`、均 OPEN）。 |
| 6 | ~~**是否编辑 #44 为 epic**（上述 6 项清单）~~ | **已闭合（2026-10-19）**：已执行并核验；#44 **保持 OPEN**（标题/milestone/labels/state 未变）。 |
| 7 | ~~**是否授权安装 JDK 21**~~ | **已闭合（2026-10-19）**：JDK 21 **已安装**于 `/opt/homebrew/opt/openjdk@21`（`java version 21.0.12.1`，主 agent 确认）；Maven 在该 `JAVA_HOME` 下**编译现有 Boot 2.7 代码成功**（仅编译，未跑 `mvn verify`）。 |
| 8 | **PR [#45](https://github.com/PNZ-z/ai-powered-Fabflix/pull/45) 是否先合并** | 本文件与拆分文档均在该分支（`codex/ai-roadmap-planning`，state=OPEN）；未合并前这些修订不在 `main` |
| 9 | **C-4 的 epic 正文改写未落远端**（**新发现，未确认是否有意**） | 拆分总览 §7 / 元数据要求把 epic 正文的工期口径改为“工作量 ≈ 1.5 周 / 日历 ≈ 5–7.5 周（含 7 轮审查）”，但 `github-operator` 的 6 项清单（§5.1）**不包含**这一项：2026-10-19 核对远端正文的 `Estimate` 行仍为“~1.5–3 weeks”。**本轮 docs-writer 未修改远端**；如需补改，需主 agent 授权 `github-operator` 单独编辑。 |
| 10 | **远端 #44 正文尚未同步“门槛已收紧为 0.90 / 90”的口径**（**2026-10-20 新发现，待授权**） | 2026-10-20 经**用户批准**，后端 JaCoCo 门槛 0.85 → **0.90**、前端 `thresholds.lines` 88 → **90**（分支 `codex/coverage-threshold-90`，独立 PR，未合并）。本轮已把本文件 `## GitHub issue body` 的 **Background 的门禁句、AC-2、AC-5、Scope→Docs、Non-goals、Risk notes（覆盖率预算）、`## Step → acceptance mapping` 的 AC-2 行**改为收紧后的口径；但 **docs-writer 不做 GitHub 写操作**，所以**远端正文仍写着 `>= 0.85` / `lines >= 88`**（2026-10-20 只读核对 `gh issue view 44` 确认）。**结论：本文件 body 与远端暂时不一致**，需主 agent 授权 `github-operator` 编辑 #44 后才能恢复“逐字一致”。 |

### AC-6 的 JDBC 条件子句：**已触发**（2026-10-20）

> 本小节**不发布到 GitHub**。

- **AC-6 的末句是条件式**（`## Acceptance criteria` → AC-6）：“**If the JDBC driver is upgraded to 9.x**, also verify order/sale dates match `sales.sale_date` (no timezone offset / `CURDATE()` drift) and that SSL connection warnings do not become fatal.”
- **触发结论**：S2（[#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)）已于 **2026-10-20** 把 `com.mysql:mysql-connector-j` 从 `8.3.0` 升到 **`9.7.0`**（= 目标 Boot 3.5.15 BOM 的 `mysql.version`）→ **该条件子句已生效**，不再是“未触发的条件分支”。因此 S6 **不得**用“驱动未变，无 JDBC 差异可验”结案。
- **S2 已提供的证据**（真 MySQL，9.7.0 / JDK 21 / 8081）：顾客结算写入 `sales.sale_date = 2026-10-20` 与响应 `order.order_date = 2026-10-20` 一致（无时区偏移）；员工 `metadata` 200（10 表含 `sales`，`sale_date DATE(10)`）；应用日志**无**新增 SSL/连接告警（唯一 WARN 是故意的鉴权失败）。附加对照：**8.3.0 A/B**（`sales.id=13563`）与 **Java 11.0.31 运行时**（`id=13564/13565`）在“日志警告面 / 日期语义 / metadata 输出”三维度一致。
- **S3 的 Boot 3 支付 smoke 证据（2026-10-20，补充，不改变「已触发」结论）**：在 **Boot 3.5.15 / JDK 21 / 提交版 `Dockerfile` 镜像（`eclipse-temurin:21-jre`，非 root 1001）** 下，主 agent 亲自跑通顾客登录 → 加购 → checkout token → **支付成功**：响应 `order_date=2026-10-20`，库内 `sales.id=**13566**`、`sale_date=2026-10-20`（**无时区偏移、无 `CURDATE()` 漂移**），容器日志无 SSL/连接告警；员工登录 → `/api/boot/dashboard/session` 200 → `?action=metadata` 200。即：AC-6 的 JDBC 子句在**迁移后的最终态**上又取证了一次（`mysql-connector-j 9.7.0` 显式 pin 在 Boot 3.5.15 BOM 下仍生效，且 BOM 的 `mysql.version` 同为 9.7.0）。**注意**：这仍不等于 AC-6 完成 —— AC-6 还包含「生产 compose smoke 的完整 10 条深链 + 401/404 语义」（S3 已顺带跑到，但终态复核归 **S6**）与本节下方的未验证项。证据：`docs/logs/2026-10-20-issue-48-core-upgrade.md`（E8）。
- **仍缺（S6 必须补）**：驱动 `DATE`→Java 的**读取**映射（`src/main` **无** `SELECT … FROM sales`，唯一 `sales` 用法是 `PaymentRepository:19` 的 `INSERT … CURDATE()`）、跨日/TZ 边界、连接池/并发、TLS 实际协商状态（无 `sslMode`，默认 `PREFERRED`，未查 `Ssl_version`）。S2 已将这些显式记为“未验证/顺延 S6”。
- 证据：`docs/logs/2026-10-20-issue-47-dependency-readiness.md`（E4–E8、§1.3 第 1–4 条）、`docs/issues/issue-47-dependency-readiness.md`（AC-1/AC-3 回填）。
- **本文件的 `## GitHub issue body` 围栏文本未改**：AC-6 仍是条件式表述，**仍然正确**；把“已触发”写在本地跟踪区，是为了不制造**第三处**「本地与远端不一致」（已有：门槛 0.90/90、以及本条若写入正文）。若需把“已触发”写进远端正文，须主 agent 授权 `github-operator` 编辑，**建议与「剩余待确认」第 10 项的 0.90/90 同步一并处理**。
- **对其他 AC 的影响**：无。AC-1..AC-5、AC-7..AC-11 的文字与责任步骤**不变**（AC-6 的“生产 compose smoke”部分仍归 S6）。

### 发布前置（go/no-go）——**已全部完成**

1. oracle 只读审查本草稿 → ✅ **已完成**（2026-10-19；B/P1/P2 已按裁定闭合）；
2. 主 agent 复核修订稿并**明确标记可发布** → ✅ **已完成**（主 agent 裁定「全部接受」并批准发布）；
3. 用户授权创建 issue（含 milestone 名，Y-2）→ ✅ **已完成**（2026-10-19；milestone 定为 #3 `Phase 3 — AI-powered platform`）；
4. `github-operator` 仅按上方 `## GitHub issue body` 创建 issue → ✅ **已完成**（2026-10-19T07:06:27Z，产出 **#44**，URL 见上方元数据表）；
5. docs-writer 收到编号后将其改名为 `docs/issues/issue-<编号>-<slug>.md` 并记录远端链接与状态 → ✅ **已完成**（本文件 = `docs/issues/issue-44-boot3-java17-jakarta-migration.md`）。

### 内部备注

- **2026-10-20 覆盖率门槛收紧（0.90 / 90）对本 issue 的影响**：门槛收紧是**独立 PR（分支 `codex/coverage-threshold-90`，未提交/未推送）**、**迁移前的变更**，**不属于** #44 或 #46–#52 任一步的范围；本 epic 的验收**改用收紧后的门槛**（后端 `BUNDLE/LINE >= 0.90`，实测 988/1078 = 0.9165 / `Tests run: 134`；前端 `lines >= 90`，实测 92.35% / 195 tests），并保留“**不得放松任何质量门**”的原有约束。收紧后**后端仅剩 N ≤ 19 行**新增未覆盖代码的余量。证据：`docs/logs/2026-10-20-coverage-threshold-90.md`；比较基线：`docs/plans/issue-44-migration-checklist.md` 的「2026-10-20 覆盖率门槛收紧（0.90 / 90）与新的比较基线」。**远端未同步**（见“剩余待确认”第 10 项）。
- **拆分提案（2026-10-19）**：`docs/plans/issue-44-migration-steps.md` + 7 份子 issue 文档（现为 `docs/issues/issue-46-baseline.md` … `issue-52-close-out.md`）已按 oracle 审查（`.pi/artifacts/oracle-issue-44-split-review.md`）修订，主 agent 裁定 **B-1、P1-1..P1-8、P2-1..P2-8、C-1..C-4 全部接受**。**2026-10-19 用户授权后**：子 issue 已创建、本 issue 已转 epic；**本文件 body 分区已同步为 epic 版本**（与远端仅差末尾换行）。
- **epic 转换的记录位置**：远端变更由 `github-operator` 执行并留有证据 `.pi/artifacts/github-operator-issue-44-steps.md`；本地回填见本文件顶部“实际结果”与 `docs/plans/issue-44-migration-steps.md` §5.1。**docs-writer 本轮未做任何 GitHub 写操作**，也未修改任何历史日志。
- 本 issue **不包含任何代码改动**；issue 本身已于 **2026-10-19** 创建（**#44**），对应总览 §12.3-A-1 的**已完成项**。~~**立项不等于实施**：Scope/Acceptance criteria 全部未执行。~~ → **2026-10-20 更新**：Scope 的 `Dependency compatibility check` 一项已由 **#47** 执行（驱动定版 9.7.0，PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55) 已合并），**AC-6 的 JDBC 子句已触发**；`Build` / `Jakarta migration` / `Architecture guard test` / `CI` / `Docker` 已由 **S3（#48，PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 已合并、#48 CLOSED）**落地，`Toolchain` 的 enforcer/README 部分由 **S4（#49，PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57) 已合并、#49 CLOSED）** 完成，`@MockBean` 弃用已由 **S5（#50，PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) 已合并、#50 CLOSED）** 清理（clean 构建 11 → 0）；**全量回归与生产 smoke 已由 S6（#51，PR [#59](https://github.com/PNZ-z/ai-powered-Fabflix/pull/59) 已合并 @ `b4a6d2a`、#51 CLOSED）取证**（含 AC-1/AC-2/AC-5/AC-6/AC-7/AC-9/AC-11；未执行项已显式登记）；**活文档同步（契约文档 §8/§12/§11/§7、路线图与里程碑状态、#44 关闭口径）已由 S7（#52）于 2026-10-21 完成并随 PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) 合并（`c1067f7`，#52 CLOSED）**；**README 的 4 处文本对齐由 `worker` 完成**（`README.md` 首段 `Spring Boot 3.5`/`React 19`、前端 90% 闸门；`frontend/README.md` 阈值措辞与 `mvn -Pfrontend` 的 JDK 21 说明），详见 `docs/logs/2026-10-21-issue-52-close-out.md` §1.1。**本 issue 正文（`## GitHub issue body`）未改**，因此远端 Scope 勾选状态不变。
- **epic 已关闭（2026-10-21）**：S7 随 PR [#60](https://github.com/PNZ-z/ai-powered-Fabflix/pull/60) 合并（`c1067f7`）后，由主 agent 关闭 #44（reason=completed，完成评论 `issuecomment-6334171353`）。~~**后续待执行（不在本次文档任务范围）**：迁移实施的 worker 指派、`docs/logs/` 工作流日志（完成后建立）、以及总览 §12.3 的提交/推送/PR（A-3/A-4）~~ → **已全部完成**（S1–S7 各自的日志见 `docs/logs/`，总览 §12.3 的 A-3/A-4 已回填）。~~**本 issue 关闭前不应把任何 Scope 条目标记为已完成**~~ → **实际处理：远端 `Scope` / `AC-1..AC-11` 的 21 个 checkbox 至今未被勾选**（关闭时只勾选了 `## Steps` 的 7 项）；验收证据已存在于各步日志与子 issue 回填中，但**远端正文未逐条打勾**——这是关闭后的已知文档缺口（如需补齐，须主 agent 授权 `github-operator` 编辑远端正文）。
- 计划文档 `ai-option-a-boot3-migration.md` 中 `@MockBean` 记作「**9 处**」（按**文件**计），本轮 grep 实测为 **9 个文件 / 11 处标注**；草稿正文采用实测口径，**与计划的差异已在 §事实来源 记录，不视为计划错误**。
- 计划文档 §1.2 把 `maven.compiler.source/target` 记为 `pom.xml:14-15`，实测为 `pom.xml` 顶部 properties 第 13–14 行；草稿正文**不使用该行号**，避免引入未经确认的引用。
- **P2-1 的落点说明**：契约文档 `docs/architecture/frontend-migration-contracts.md` 的覆盖率余量口径（现写 "~106 行" → 正确为「现有 110 行未覆盖；最多还能新增 ≈60 行未覆盖」）**由本 issue 的 worker 在 `Docs` 步骤中订正**（本轮 docs-writer 未改动该活文档，以免出现「issue 声称要做、实际已做完」的口径不一致）。
- **口径差异提示（请主 agent 决定）**：本轮新增/收窄的内容（`requireJavaVersion [21,22)`、`@MockBean` 明确不迁移、CI step 名、Docs 范围限定为活文档、回退粒度表述）**尚未回填到 `docs/plans/ai-option-a-boot3-migration.md`**；按 AGENTS §3「接受的调整由 docs-writer 更新到方案文档」，如需回填请主 agent 指示（本轮任务范围限定为修订本发布草稿 + 回填 #44 立项记录）。
- **重命名说明（2026-10-19）**：本文件原路径 `docs/issues/proposed-option-a-boot3-migration.md`（未跟踪文件）→ `docs/issues/issue-44-boot3-java17-jakarta-migration.md`；仓库内对该旧路径的引用已同步更新（总览 §2.6/§12.3-A-1/A-4、Option A 方案 §文档头）。**历史日志（`docs/logs/**`）不改**。

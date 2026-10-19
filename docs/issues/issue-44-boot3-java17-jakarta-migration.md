# Option A — Spring Boot 3.x / Java 17 / Jakarta 迁移（GitHub issue **#44** 跟踪记录）

> **状态：🟢 已在 GitHub 存在（[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)），无需新建。** 远端 issue 已由 `github-operator` 于 **2026-10-19** 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**；`## Internal tracking` 分区**未发布**）。**注意「立项 ≠ 完成」**：本 issue 只表示**迁移工作已立项**；Boot 3.x / Java 17 / Jakarta 的**代码与配置改动、测试与验证全部尚未开始**（无源码/配置/测试改动，无 PR）。
> **修订记录（2026-10-19，oracle 只读审查 `.pi/artifacts/oracle-option-a-issue-draft.md` 后；主 agent 裁定「全部接受」）**：闭合 **B-1**（`AC-10` 去掉流程语；`Scope → Docs` 只列**活文档**并声明历史记录不追改）、**B-2**（**写死** `@MockBean` 处置 = 本 issue 不迁移、列入 Non-goals）、**B-3**（补可复现的 JDK 21 固定机制：`README.md` + `JAVA_HOME` + 采纳 enforcer `requireJavaVersion [21,22)`；`AC-11` 增字节码级证据 major version 61；CI step 名一并列为改动点）；采纳 **P1-1..P1-5** 与 **P2-1..P2-9**；回填 **Y-1/Y-3/Y-4**；**Y-2**（milestone 名）当时标为「建议、待用户确认」，**现已确认并创建为 milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**（原「待确认」表述已失效）。
> 本文件是 GitHub issue **#44** 的**规范来源与本地跟踪记录**（由发布草稿 `docs/issues/proposed-option-a-boot3-migration.md` 改名而来）。`github-operator` 只发布下方 `## GitHub issue body` 分区的内容（**`## Internal tracking` 分区不上 GitHub**）；正文包裹在四反引号 `markdown` 围栏内仅是**本地容器**，发布时已去掉围栏行本身，其余内容逐字使用。
> **发布前置门禁：✅ 已全部通过（2026-10-19）** —— oracle 风险关卡（✅，B/P1/P2 已按裁定闭合）→ 主 agent 明确批准（✅）→ 用户授权创建（✅）→ `github-operator` 创建（✅，产出 [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)）。本行保留为过程记录。
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
- Baseline quality gates that must not regress: backend JaCoCo `BUNDLE/LINE >= 0.85` (recorded baseline 968/1078 = 0.8980, `Tests run: 127`), frontend `lines >= 88` (recorded 92.35%, 195 tests), Playwright e2e (12 executions). These numbers are recorded in the approved plan §6.1 and are the comparison baseline for this issue.

## Scope

- [ ] **Baseline (first step)**: re-run and record the **pre-migration baseline** — `mvn --batch-mode --no-transfer-progress verify`, and in `frontend/`: `npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e` — and attach the recorded output to the pull request. This fresh baseline is what AC-1/AC-5 are compared against (reference numbers above come from the approved plan, not from a run today).
- [ ] **Toolchain**: compile with `release=17` while running the build on **JDK 21** locally, in CI and in the container image (one JDK version everywhere to remove "green locally, red in CI" drift). The JDK 21 requirement is fixed by two mechanisms: (a) `README.md` states the JDK 21 requirement and that `JAVA_HOME` must point at JDK 21; (b) a **`maven-enforcer-plugin` rule `requireJavaVersion [21,22)`** is added to the same plugin (and the same `validate`-phase execution pattern) as the existing `require-frontend-dist` rule, so a wrong JDK fails the build instead of silently skewing results.
- [ ] **Build**: `spring-boot.version` 2.7.18 → **3.5.x** (patch version decided during implementation); replace `maven.compiler.source/target = 11` with **`maven.compiler.release = 17`**; align plugin/dependency versions with the Boot 3.5 BOM.
- [ ] **Jakarta migration**: `javax.servlet` → `jakarta.servlet` in the **11 files listed above**; the 4 JDK-`javax.*` files stay unchanged; compiler errors are the completeness check.
- [ ] **Architecture guard test**: rewrite `SpringBootOnlyArchitectureTest` — keep the bans on `@WebServlet` / `extends HttpServlet`, and replace the `jakarta.servlet` ban with a **controlled allow-list** implemented as: collect the relative paths of the files under `src/main/java` whose content contains `jakarta.servlet`, and assert **set equality with the expected set of exactly 11 paths** (not "contains", which would let the allow-list silently grow). Add an assertion that **no `jakarta.servlet.annotation.WebServlet` import exists**. Keep the `pom.xml` assertion on `jar` + `repackage`. **The `pom.xml` `jakarta.servlet-api` literal assertion stays in place**: the literal is absent from `pom.xml` today and stays absent, because Spring Boot 3 supplies the API transitively through `spring-boot-starter-web` as long as no explicit dependency is declared.
- [ ] **Dependency compatibility check** (first implementation step): `mysql-connector-j` (currently pinned 8.3.0; Boot 3.5 manages 9.x — if upgraded, JDBC behavior deltas around time zones, `CURDATE()` and SSL connection warnings must be covered by the production smoke test), `jasypt:1.9.3` (**verification item**: 2014-era release — check reflective access on JDK 21 such as `IllegalAccessError` / `InaccessibleObjectException` and any reliance on removed JDK APIs; only `org.jasypt.util.password.*` is used today. **If the password-verification implementation has to be replaced, that is a scope change and must go back to the main agent for a ruling**), `mockito-core` / `byte-buddy` / `byte-buddy-agent`, `spring-boot-starter-test`, `maven-surefire-plugin:3.5.2`, `jacoco-maven-plugin:0.8.15`, `spring-boot-maven-plugin`, `maven-enforcer-plugin`, `maven-resources-plugin`, and `MockMvcBuilders.standaloneSetup` usage in the admin package. Also review the **BOM-managed transitive upgrades** that Boot 3.5 brings in: **HikariCP (4.0.3 → 6.x)**, **Jackson (2.13.5 → 2.19.x)**, **Logback/SLF4J (1.2.12 → 1.5.x)** and **Tomcat (9.0.83 → 10.1.x)**. Connection-pool behaviour and JSON serialization are covered by the AC-6 smoke test; any behavioural difference must be recorded, not silently accepted.
- [ ] **CI**: `.github/workflows/ci.yml` backend job — `java-version: "11"` → **`"21"`** (`ci.yml:32`), rename the **job** `Build and test (Java 11)` → the new JDK version (`ci.yml:20`), **and** rename the **step** `Set up Java 11` → `Set up Java 21` (`ci.yml:28`).
- [ ] **Docker**: `Dockerfile` build stage `maven:3.9-eclipse-temurin-11` → `maven:3.9-eclipse-temurin-21`, runtime stage `eclipse-temurin:11-jre` → `eclipse-temurin:21-jre`, keeping the non-root uid/gid 1001 setup.
- [ ] **Docs (live documents only)**: update `README.md` (local JDK 21 requirement and `JAVA_HOME`, build/run instructions) and `docs/architecture/frontend-migration-contracts.md` — **§8** (the guard-test description: the `jakarta.servlet` ban becomes the 11-file allow-list) and **§12** (container/version conventions: JDK 21 images, CI JDK); also correct that document's stale coverage-margin figure, which currently reads "~106 lines remaining" — the correct statement is **110 lines are currently uncovered, with room for at most ≈60 additional uncovered lines**. **Historical workflow logs and closed-issue plan/issue documents are records of past runs and are not rewritten**: the files that still contain `Build and test (Java 11)` for historical reasons (`docs/logs/2026-*`, `docs/plans/issue-30..33-*.md`, `docs/issues/issue-30..33-*.md`) stay as they are.
- [ ] **Verification**: backend, frontend, e2e and production compose smoke (see Acceptance criteria).

## Non-goals

- No Spring AI / `spring-ai-*` dependency (that is a separate, later issue).
- **No `@MockBean` → `@MockitoBean` migration in this issue.** `@MockBean` appears in **9 test files (11 annotations)** and has been deprecated since Boot 3.4 (replacement: Spring Framework 6.2's `@MockitoBean`, semantically equivalent). The resulting deprecation warnings are **acceptable and will be recorded** in the completion log; a follow-up issue may address them. This issue must not change those annotations.
- No rewriting of historical workflow logs or closed-issue plan/issue documents (see Scope → Docs).
- No Spring Security, JWT, CSRF or authentication-model change.
- No business-behavior change, new feature or refactor; no URL, status code, JSON shape or error-message change.
- No database schema or data migration.
- No vector store, corpus, recommendation or agent functionality.
- No multi-instance / Spring Session / Redis work.
- No frontend build-chain upgrade (Vite/React/vitest/Playwright are unrelated to the Boot version).
- No restoration of the retired `src/main/webapp` legacy frontend.
- No relaxation of quality gates: coverage thresholds and the JaCoCo `excludes` list stay unchanged.

## Acceptance criteria

- [ ] **AC-1** `mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS; backend test count does not decrease against the pre-migration baseline recorded in Scope step 0 (`Tests run: 127, Failures: 0, Errors: 0, Skipped: 0` is the recorded reference; increases must be explained).
- [ ] **AC-2** Backend coverage gate unchanged: JaCoCo `verify` check passes with `BUNDLE/LINE >= 0.85` (recorded baseline 0.8980 against the fresh pre-migration re-run) and `All coverage checks have been met.`
- [ ] **AC-3** With `frontend/dist` built first (`cd frontend && npm ci && npm run build`), `mvn -Pfrontend -DskipTests clean package` succeeds and `unzip -l target/fablix.jar` contains `BOOT-INF/classes/static/index.html` (packaging shape unchanged).
- [ ] **AC-4** With `frontend/dist` built as in AC-3 and then moved aside, `mvn -Pfrontend -DskipTests package` → BUILD FAILURE from the `require-frontend-dist` enforcer rule (the missing-`dist` guard still fails the build).
- [ ] **AC-5** Frontend unchanged and green: `npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e` → 195 tests pass, Lines >= 88 (recorded 92.35%), 12 Playwright executions pass — compared against the fresh pre-migration baseline from Scope step 0.
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
- **Coverage budget**: without relaxing the gate there is room for at most **~60 additional uncovered lines** (110 lines are currently uncovered; the ≈161-line allowance at the 85% threshold minus those 110), so any newly introduced class needs a test. `docs/architecture/frontend-migration-contracts.md` still states a stale "~106 lines" figure; Scope → Docs corrects it in the same PR.
- **Guard test must be updated first**, otherwise the migration cannot pass CI.
- **Estimate**: ~1.5–3 weeks for a single developer (estimate, not a commitment); the main variable is hunting down Boot 3 behavioral differences, not the mechanical Jakarta rename.

## Rollback

The migration is delivered as a single PR with no destructive operations: failure can be reverted wholesale (`git revert` / close the PR) without affecting `main`. If the migration is split into commits by step, each step is independently revertible. Relaxing any quality gate to make CI green is not an acceptable mitigation and requires explicit approval.
````

## Internal tracking

> 本分区**不发布到 GitHub**；`github-operator` 只使用上方 `## GitHub issue body`（**已发布：远端正文与该分区逐字一致，本分区未外泄**）。

### 远端 issue 元数据（2026-10-19 只读核验：`gh issue view 44`、`gh api repos/PNZ-z/ai-powered-Fabflix/milestones`，docs-writer/主 agent）

| 项目 | 值 |
| --- | --- |
| 编号 | **#44** |
| URL | https://github.com/PNZ-z/ai-powered-Fabflix/issues/44 |
| 标题（远端） | `Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace` |
| milestone | **[#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)**（number 3，**state=open**，created `2026-10-19T07:05:43Z`，由主 agent 创建；#44 已挂该 milestone） |
| labels | **无（空数组）** |
| state | **OPEN** |
| 创建时间（createdAt） | **`2026-10-19T07:06:27Z`** |
| 正文 | 与本文件 `## GitHub issue body` **逐字一致**（主 agent 核验）；`## Internal tracking` 分区**未发布** |
| **当前跟踪状态** | **已立项（issue 已创建）**，实现**未开始**：无代码/配置/测试改动、无 PR；本 issue 的 Scope 与 Acceptance criteria 均**未执行** |

### 本地跟踪表

| 项目 | 值 |
| --- | --- |
| 文档状态 | **🟢 已在 GitHub 存在（#44），无需新建**（2026-10-19 由 `github-operator` 按获批草稿创建）；本文件已从 `docs/issues/proposed-option-a-boot3-migration.md` 改名为 `docs/issues/issue-44-boot3-java17-jakarta-migration.md`（**slug 说明**：选用 `boot3-java17-jakarta-migration`，含编号 **44**，与远端标题的 Boot 3.x / Java 17 / Jakarta 三项对应，并与方案文档 `docs/plans/ai-option-a-boot3-migration.md` 的命名区分开）|
| oracle 审查报告 | `.pi/artifacts/oracle-option-a-issue-draft.md`（只读审查，2026-10-19）：必须修 **B-1..B-3 + P1-1..P1-5**；建议修 **P2-1..P2-9**；待裁定 **Y-1..Y-4**。**裁定结果：全部接受**——B/P1/P2 已全部落入本稿；**Y-1/Y-3/Y-4 已由主 agent 裁定并回填**；**Y-2（milestone 名）仍为建议、待用户确认**（→ **2026-10-19 已闭合：milestone #3 已创建**，见下表） |
| 来源 | ① `docs/plans/ai-roadmap-overview.md`（**已批准**路线图，2026-10-19）：Option A 为路线图首个工作流（§5.1、§11-D1）、立项授权方向（§11-D6）、§12.3-A-1；② `docs/plans/ai-option-a-boot3-migration.md`（**已批准**实施计划，2026-10-19） |
| 关联计划 | `docs/plans/ai-option-a-boot3-migration.md`；上级文档 `docs/plans/ai-roadmap-overview.md` |
| 范围（Y-3 已裁定） | **本次只创建 Option A 一个 issue**。Option B / B-API / C 的 issue **后续再立**；`docs/plans/ai-option-b-spring-ai-rag.md` 已是批准方案，但**本次不立 issue** |
| GitHub 标题（已发布） | `Modernize to Spring Boot 3.x, Java 17, and the Jakarta namespace`（**#44 已按此标题创建**；与 `README.md` roadmap 的 **"Spring Boot"** 现代化范畴一致；**不**写成 Spring AI 功能） |
| milestone（已创建） | **#3 `Phase 3 — AI-powered platform`**（**Y-2 已确认并由主 agent 创建**，state=open，`2026-10-19T07:05:43Z`；原先「仅为建议名、待用户确认、未确认前不写入远端」的限定**已随创建失效**）。仓库此前的 `Phase 2 — React Frontend Migration` 已随 #31–#33 关闭 |
| labels（已发布） | **空**（P2-4 已裁定）：仓库既有 issue（#28–#33）均无 label，保持与现状一致；**#44 远端 labels 实测为空数组** |
| 事实来源（可核验） | `pom.xml`（`spring-boot.version` 2.7.18、`maven.compiler.source/target` = 11、`maven-enforcer-plugin:3.5.0` 已有 `require-frontend-dist` 执行）；`.github/workflows/ci.yml`（job name `Build and test (Java 11)` @ `:20`、step `Set up Java 11` @ `:28`、`java-version: "11"` @ `:32`）；`Dockerfile`（`maven:3.9-eclipse-temurin-11`、`eclipse-temurin:11-jre`、uid/gid 1001）；`SpringBootOnlyArchitectureTest.java`；`grep -rl "javax\.servlet" src/main/java` = **11**、宽口径 `javax.` = 15、`grep -rl "@MockBean" src/test/java` = **9 个文件 / 11 处标注**（docs-writer 只读复核，2026-10-19）。**B-1 证据**：`grep -rln "Build and test (Java 11)" docs README.md .github` = **14 个文件**，其中 10 个是**历史记录**（`docs/logs/2026-10-14-issue-30-*.md`、`docs/logs/2026-10-15-issues-31-32-33-*.md`、`docs/plans/issue-30/31/32/33-*.md`、`docs/issues/issue-30/31/32/33-*.md`），需改的是**活文档**（`README.md` 不含该字面量、契约文档也不含该字面量 → 契约文档要改的是 §8/§12 的守卫与容器/版本约定） |
| 未运行/未验证 | docs-writer **未运行** `mvn`、npm、Docker、CI；**立项后仅做只读 GitHub 核对**（`gh issue view 44` / `gh api .../milestones`，2026-10-19，用于回填上方元数据表），**未做任何 GitHub 写操作**。正文中的 127/195/12 用例数、覆盖率 0.8980 / 92.35%、10 条深链、HikariCP/Jackson/Logback/Tomcat 的版本号均**来自 oracle 报告与已批准计划文档的引用**，需实施时复跑/复核确认 |
| 工作量 | **1.5–3 周（估算）**，引用计划 §5 里程碑表（A0–A7） |

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

### 发布前置（go/no-go）——**已全部完成**

1. oracle 只读审查本草稿 → ✅ **已完成**（2026-10-19；B/P1/P2 已按裁定闭合）；
2. 主 agent 复核修订稿并**明确标记可发布** → ✅ **已完成**（主 agent 裁定「全部接受」并批准发布）；
3. 用户授权创建 issue（含 milestone 名，Y-2）→ ✅ **已完成**（2026-10-19；milestone 定为 #3 `Phase 3 — AI-powered platform`）；
4. `github-operator` 仅按上方 `## GitHub issue body` 创建 issue → ✅ **已完成**（2026-10-19T07:06:27Z，产出 **#44**，URL 见上方元数据表）；
5. docs-writer 收到编号后将其改名为 `docs/issues/issue-<编号>-<slug>.md` 并记录远端链接与状态 → ✅ **已完成**（本文件 = `docs/issues/issue-44-boot3-java17-jakarta-migration.md`）。

### 内部备注

- 本 issue **不包含任何代码改动**；issue 本身已于 **2026-10-19** 创建（**#44**），对应总览 §12.3-A-1 的**已完成项**。**立项不等于实施**：Scope/Acceptance criteria 全部未执行。
- **后续待执行（不在本次文档任务范围）**：迁移实施的 worker 指派、`docs/logs/` 工作流日志（完成后建立）、以及总览 §12.3 的提交/推送/PR（A-3/A-4）。**本 issue 关闭前不应把任何 Scope 条目标记为已完成**。
- 计划文档 `ai-option-a-boot3-migration.md` 中 `@MockBean` 记作「**9 处**」（按**文件**计），本轮 grep 实测为 **9 个文件 / 11 处标注**；草稿正文采用实测口径，**与计划的差异已在 §事实来源 记录，不视为计划错误**。
- 计划文档 §1.2 把 `maven.compiler.source/target` 记为 `pom.xml:14-15`，实测为 `pom.xml` 顶部 properties 第 13–14 行；草稿正文**不使用该行号**，避免引入未经确认的引用。
- **P2-1 的落点说明**：契约文档 `docs/architecture/frontend-migration-contracts.md` 的覆盖率余量口径（现写 "~106 行" → 正确为「现有 110 行未覆盖；最多还能新增 ≈60 行未覆盖」）**由本 issue 的 worker 在 `Docs` 步骤中订正**（本轮 docs-writer 未改动该活文档，以免出现「issue 声称要做、实际已做完」的口径不一致）。
- **口径差异提示（请主 agent 决定）**：本轮新增/收窄的内容（`requireJavaVersion [21,22)`、`@MockBean` 明确不迁移、CI step 名、Docs 范围限定为活文档、回退粒度表述）**尚未回填到 `docs/plans/ai-option-a-boot3-migration.md`**；按 AGENTS §3「接受的调整由 docs-writer 更新到方案文档」，如需回填请主 agent 指示（本轮任务范围限定为修订本发布草稿 + 回填 #44 立项记录）。
- **重命名说明（2026-10-19）**：本文件原路径 `docs/issues/proposed-option-a-boot3-migration.md`（未跟踪文件）→ `docs/issues/issue-44-boot3-java17-jakarta-migration.md`；仓库内对该旧路径的引用已同步更新（总览 §2.6/§12.3-A-1/A-4、Option A 方案 §文档头）。**历史日志（`docs/logs/**`）不改**。

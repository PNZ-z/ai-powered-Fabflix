# Step 3/7 — 核心升级：Spring Boot 3.5 + `release=17` + Jakarta（GitHub issue #48 记录）

> **文档状态：🟢 已在 GitHub 存在（[#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)），无需新建** —— 2026-10-19 由 `github-operator` 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**，仅 GitHub 在末尾追加一个换行；`## Internal tracking` 分区**未发布**）。~~**本文件不表示该步骤已实施**：Scope / Acceptance criteria 全部未执行，无源码/配置/测试改动、无 PR。~~ → **2026-10-20 更新：本步已实施**（分支 `codex/issue-48-core-boot3-jakarta`，18 文件；JDK 21 下 `mvn clean verify` 134 用例全绿、JaCoCo 0.9165、字节码 major 61、容器 smoke 全绿），**未提交 / 未推送 / 无 PR / 未合并**；AC 逐条状态见文末「实施结果与 AC 回填（2026-10-20）」。
> **远端元数据（主 agent 核验，2026-10-19）**：编号 **#48** · 标题 `[Step 3/7] Upgrade to Spring Boot 3.5 and migrate to the Jakarta namespace` · milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) · labels **空** · state **OPEN** · 正文含显式 `Part of #44` · 上游 epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**）。
> `github-operator` 只发布 `## GitHub issue body` 分区（四反引号围栏只是本地容器，发布时去掉围栏行本身）；`## Internal tracking` **不上 GitHub**。
> 拆分总览：`docs/plans/issue-44-migration-steps.md`。**本步骤是 7 步中唯一不可再切的核心**（编译层面原子：Boot 3 无法编译 `javax.servlet`，守卫测试又主动禁止 `jakarta.servlet`）。
> **已裁定（2026-10-19，oracle 建议 + 主 agent）：** **B-1** `Dockerfile` 两行 `FROM` 并入本步（否则合并后 `docker compose build app` 损坏而 CI 仍绿）；**C-1 (a)** `ci.yml` 的 JDK 升版在本步；**C-3 (a)** 本步只跑 `npm ci && npm run build`；**P1-3** 所有命令需 `JAVA_HOME`=JDK 21；**P1-4** “恰好 11 文件”拆为两道判据；**P1-8** 保留 S2 的 `mysql-connector-j` 显式版本；**P2-8** `jakarta.servlet-api` 断言写为“expected to stay absent”；**P1-2** 正文加 `Part of #44`。
> ~~本文件**不声称**任何验证已运行；用例数/覆盖率均引用既有记录（2026-10-15）并标注待 S1 复跑确认。~~ → **2026-10-20**：上文（2026-10-19 稿）的引用口径已被实测取代 —— 收紧后基线为 **134 用例 / 988-90-1078 = 0.9165**（门槛 0.90），本步实测与之一致；所有命令与结果见 `docs/logs/2026-10-20-issue-48-core-upgrade.md`。

## GitHub issue title

```
[Step 3/7] Upgrade to Spring Boot 3.5 and migrate to the Jakarta namespace
```

## GitHub issue body

````markdown
## Goal

Perform the **one atomic upgrade** that takes the application from Spring Boot 2.7.18 / Java 11 / `javax.servlet` to **Spring Boot 3.5.x** with **`maven.compiler.release=17`** and the **Jakarta** servlet namespace, while keeping behaviour identical.

This must be a single change: Spring Boot 3's `spring-boot-starter-web` ships only `jakarta.servlet-api`, so the BOM upgrade and the 11 import rewrites cannot compile apart; and the architecture guard test currently **forbids** `jakarta.servlet` in `src/main/java`, so it must be inverted in the same change or `mvn verify` can never be green.

**This issue covers only this step.** It deliberately does not fix `@MockBean` deprecations (step 5), does not add the JDK enforcement rule or the README instructions (step 4), and does not upgrade the JDBC driver (step 2 — its pinned version is kept). It **does** update the two `Dockerfile` `FROM` lines and the CI JDK, because `release=17` and class file 61 cannot be built or run on JDK 11.

## Background

**Part of #44.**

This is **step 3 of 7** in the migration tracked by #44, planned in `docs/plans/ai-option-a-boot3-migration.md` and split in `docs/plans/issue-44-migration-steps.md`.

- `pom.xml` imports the `spring-boot-dependencies` BOM at `spring-boot.version = 2.7.18` and sets `maven.compiler.source`/`target` to `11`. Spring Boot 2.7 is end-of-life.
- **11 files** under `src/main/java` import `javax.servlet` and must move to `jakarta.servlet`:
  - `com/fabflix/boot/commerce/CartService.java`, `CartController.java`, `CheckoutController.java`, `CheckoutTokenService.java`, `PaymentController.java`
  - `com/fabflix/boot/auth/CustomerAuthController.java`, `CustomerAuthenticationFilter.java`
  - `com/fabflix/boot/admin/EmployeeController.java`, `EmployeeAuthenticationFilter.java`
  - `com/fabflix/boot/search/SearchController.java`
  - `com/fabflix/boot/session/SessionController.java`
- **4 files must stay on `javax.*`** because those packages are JDK-provided and are not part of the Jakarta EE 9 rename: `javax.sql.DataSource` (`com/fabflix/boot/admin/AdminRepository.java`) and `javax.xml.parsers.*` (`ActorXMLParser.java`, `CastXMLParser.java`, `MainXMLParser.java`).
- `src/test/java/com/fabflix/boot/SpringBootOnlyArchitectureTest.java` asserts `src/main/java` contains no `jakarta.servlet`, and that `pom.xml` keeps `jar` + `repackage` and contains no `jakarta.servlet-api` literal.
- `.github/workflows/ci.yml` builds the backend with `java-version: "11"`. **Spring Boot 3.5 requires Java 17+**, so the CI JDK bump must land in the same change as the BOM bump: otherwise the pull request's own CI job — and `main` after merge — fails immediately.
- `Dockerfile` uses `maven:3.9-eclipse-temurin-11` for the build stage and `eclipse-temurin:11-jre` for the runtime stage. **Both `FROM` lines must change in this same step**: a JDK 11 `javac` cannot compile with `--release 17`, and a JRE 11 cannot load class file 61, so leaving the images behind would break the documented production path (`docker compose build app`) while CI — which does not build Docker — would still look green.
- Quality gates that must not regress: `Tests run: 134` and the JaCoCo `BUNDLE/LINE >= 0.90` gate with `988/1078 = 0.9165` behind it (both measured on 2026-10-20 **after** the coverage threshold was tightened from 0.85 to 0.90 by user approval in a separate PR — the pre-tightening snapshot was `Tests run: 127` / `968/1078 = 0.8980` against 0.85), plus the 8-entry `excludes` list.

## Scope

- [ ] **Toolchain precondition**: run **every** command in this step with `JAVA_HOME` pointing at **JDK 21** (local Maven currently defaults to JDK 27, which is outside the supported range). The `maven-enforcer-plugin` rule and the README instructions **only arrive in step 4**, so nothing catches a wrong `JAVA_HOME` here — record the `JAVA_HOME` path in the evidence.
- [ ] **Build**: `spring-boot.version` `2.7.18` → **`3.5.x`** (patch version chosen during implementation and recorded); replace `maven.compiler.source`/`maven.compiler.target` (`11`) with **`maven.compiler.release=17`**; align only the plugin/dependency versions that the upgrade actually requires. **Keep the explicit `mockito-core` / `byte-buddy` / `byte-buddy-agent` / `junit-jupiter` / `junit-platform` pins unless a real conflict appears**; if one must change, change the minimum and record it. **Keep the explicit `mysql-connector-j` version decided in step 2 (`8.3.0` or the 9.x that step 2 ruled) — do not let the Boot 3.5 BOM override it.**
- [ ] **Jakarta migration**: `javax.servlet` → `jakarta.servlet` in **the 11 files listed above** (imports and type references only). The 4 `javax.sql`/`javax.xml` files stay untouched. **(a)** afterwards exactly **11** files under `src/main/java` contain `jakarta.servlet`, and the diff contains all 11; **(b)** any **additional** `src/main/java` file that the upgrade forces you to touch must be listed one by one in the pull request description with the reason it is required — and **no business logic may change**.
- [ ] **Architecture guard test**: rewrite `SpringBootOnlyArchitectureTest`
      - keep the bans on `@WebServlet` and `extends HttpServlet`;
      - replace the `jakarta.servlet` ban with a **controlled allow-list**: collect the relative paths of `src/main/java` files whose content contains `jakarta.servlet` and assert **set equality with the expected set of exactly 11 paths** (not "contains" — a growing allow-list must fail);
      - add an assertion that no `jakarta.servlet.annotation.WebServlet` import exists;
      - keep the `pom.xml` assertions (`<packaging>jar</packaging>`, `<goal>repackage</goal>`, and **no `jakarta.servlet-api` literal** — that literal is **expected to stay absent**: `spring-boot-starter-web` supplies the API transitively, so no explicit dependency is declared).
- [ ] **CI**: `.github/workflows/ci.yml` backend job — `java-version: "11"` → **`"21"`**, rename the **job** `Build and test (Java 11)` and the **step** `Set up Java 11` to the new JDK version. Nothing else in the workflow changes.
- [ ] **Container images (same change)**: `Dockerfile` build stage `maven:3.9-eclipse-temurin-11` → `maven:3.9-eclipse-temurin-21`, runtime stage `eclipse-temurin:11-jre` → `eclipse-temurin:21-jre`; keep the `uid/gid 1001` non-root block, `EXPOSE 8081` and the `ENTRYPOINT` unchanged. Then **prove the container path works**: `docker compose build app` succeeds and the started container's health endpoint returns 200 (if Docker is unavailable, record "not executed" with the reason — never assume).
- [ ] **Boot 3 behaviour differences**: run the suite, fix only what the upgrade requires, and **write every behavioural difference you had to handle into the pull request description and the step's plan document** (trailing-slash matching, error responses, Tomcat 10, the BOM-managed Jackson / HikariCP / Logback upgrades). A difference must never be silently accepted.
- [ ] **Evidence**: `mvn --batch-mode --no-transfer-progress verify` output (with the `JAVA_HOME` used); the guard test run showing **set equality with 11 paths**; `git diff --stat` plus the list of any additional `src/main/java` file with its justification; the jar-shape check; the missing-`dist` negative check; the bytecode level check; `docker compose build app` + container health result; the `mysql-connector-j` version actually resolved.

## Non-goals

- **No `@MockBean` → `@MockitoBean` change** (step 5). Deprecation warnings are expected here and are recorded, not fixed.
- **No `maven-enforcer-plugin` `requireJavaVersion` rule and no `README.md` change** (step 4). **The `Dockerfile`'s two `FROM` lines are in scope here** (see Scope); everything else about the container setup is out: no new layers, no change to `uid/gid 1001`, `EXPOSE`, `ENTRYPOINT` or `compose.yaml`.
- **No JDBC driver change** (step 2 already decided it); no other dependency upgrades beyond what the Boot 3.5 BOM/upgrade requires.
- No source refactor, no new feature, no behaviour change: URLs, HTTP status codes, JSON shapes, error messages, authentication/session semantics, the SPA fallback truth table and the database schema stay identical.
- No frontend source change (the frontend does not take part in the Boot build chain). `npm ci && npm run build` is run only because `-Pfrontend` packaging consumes `frontend/dist`.
- **The coverage threshold was already tightened to 0.90 (backend JaCoCo) / 90 (frontend `lines`) before the migration; this step must neither lower nor bypass it.** No new JaCoCo `exclude`, no skipped tests, no `compose.yaml` change. The tightening leaves room for only **~19 additional uncovered lines** (`988/(1078+N) >= 0.90 ⇒ N <= 19`), so any production code this step has to touch needs a test.
- No moving the explicit dependency pins to BOM management unless required to compile/test (that decision is still open upstream).

## Dependency

**Requires step 1** (fresh pre-migration baseline to compare against) and **step 2** (JDBC driver already decided and validated) to be merged first.

## Acceptance criteria

- [ ] **AC-1** `mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS; `Tests run` **not lower than step 1's baseline** (recorded reference: `127`, `Failures: 0`, `Errors: 0`, `Skipped: 0`); `All coverage checks have been met.`; the JaCoCo `excludes` list still has the **same 8 entries** as before the change.
- [ ] **AC-2** Jakarta boundary (two separate criteria): **(a)** exactly **11** files under `src/main/java` contain `jakarta.servlet` — the guard test asserts **set equality with the expected 11 paths** (not containment), the guard test passes, and `git diff` shows those 11 files; **(b)** any **additional** `src/main/java` file changed for the upgrade is listed **one by one** in the pull request description with the reason it is required, and **no business logic was changed** by those edits.
- [ ] **AC-3** Packaging shape unchanged: after `cd frontend && npm ci && npm run build`, `mvn -Pfrontend -DskipTests clean package` succeeds and `unzip -l target/fablix.jar` contains `BOOT-INF/classes/static/index.html`.
- [ ] **AC-4** Missing-`frontend/dist` guard still fails the build: with `frontend/dist` moved aside, `mvn -Pfrontend -DskipTests package` → **BUILD FAILURE** from `require-frontend-dist`.
- [ ] **AC-5** Bytecode and toolchain: every command was run with `JAVA_HOME` pointing at **JDK 21** — `mvn -version` reports **JDK 21** and the evidence records the `JAVA_HOME` path used — and `javap -verbose -cp target/classes com.fabflix.boot.FablixApplication | grep 'major version'` reports **61**.
- [ ] **AC-6** The pull request's CI is green on **both** jobs, and the backend job's name reflects the new JDK version.
- [ ] **AC-7** Every Boot 3 behaviour difference encountered is documented in the pull request description (with the handling), and **no quality gate was relaxed** to reach green (no lowering of the 0.90 / 90 thresholds, no new `exclude`, no skipped test, no disabled assertion).
- [ ] **AC-8** This step did not touch: `src/test` `@MockBean` annotations, `README.md`, `compose.yaml`, `frontend/src/**`, the JaCoCo `excludes` list, or any dependency unrelated to the upgrade; the explicit `mysql-connector-j` version is still the one step 2 decided.
- [ ] **AC-9** Container path: `Dockerfile` has `maven:3.9-eclipse-temurin-21` (build) and `eclipse-temurin:21-jre` (runtime) with the `uid/gid 1001` non-root setup intact; `docker compose build app` succeeds; `docker compose up -d --no-deps app` serves the health endpoint with **200** (or: recorded as **not executed** with the concrete reason).

## Risk notes

- **Boot 3 behaviour differences are the main uncertainty** (trailing-slash matching, error-response handling, Tomcat 10 networking/session behaviour). They are the reason this step is estimated larger than the others; anything that cannot be handled without a behaviour change must go back to the parent agent instead of being "fixed" quietly.
- **BOM-managed transitive upgrades** arrive with this step (recorded references: HikariCP 4.0.3 → 6.x, Jackson 2.13.5 → 2.19.x, Logback/SLF4J 1.2.12 → 1.5.x, Tomcat 9.0.83 → 10.1.x). Connection-pool and JSON-serialization differences may not be visible to unit tests; they are recorded here and proven in step 6's production smoke.
- **Explicit pins versus the Boot 3.5 BOM** (`mockito-core`, `byte-buddy`, `junit-jupiter`) can conflict on JDK 21. The default is to keep them; if one must move, change the minimum and record the reason. The same applies to the **`mysql-connector-j` pin from step 2**: it must stay explicit, otherwise the Boot 3.5 BOM silently switches the driver and step 2's evidence no longer describes what runs.
- **Container risk (B-1)**: `maven:3.9-eclipse-temurin-21` and `eclipse-temurin:21-jre` must be pullable; a registry/network failure is an environment problem (record it), not a reason to keep JDK 11 images. The container smoke also needs Docker and port 8081 free.
- **Guard-test residual risk**: if the `jakarta.servlet-api` literal assertion turns out to be violated by the BOM, do **not** delete the assertion silently — record the finding and raise it with the parent agent.
- **`mvn verify` does not exercise a real database** (the suite is MockMvc-based), so a green backend job does not prove JDBC behaviour; that is step 2's and step 6's job.
- **Merging this step without the CI JDK bump would leave `main` red.** The CI change is therefore part of this issue's scope.
- **Merging this step without the `Dockerfile` change would leave the documented production build broken while CI stays green.** The two `FROM` lines are therefore part of this issue's scope too.

## Rollback

Single pull request; reverting it returns the tree to Spring Boot 2.7.18 + `javax` **and the JDK 11 images**, with steps 1 and 2 intact and `main` green. If the change is delivered as several commits, each commit is independently revertible, but the jakarta rewrite and the guard-test rewrite must be reverted together (they only compile/work as a pair), and **the BOM bump, the CI JDK bump and the two `Dockerfile` `FROM` lines must not be split apart** — any one of them alone leaves CI or the container path broken.
````

## Internal tracking

> 本分区**不发布到 GitHub**。

| 项目 | 值 |
| --- | --- |
| 步骤 | **S3 / 7**（拆分总览 §3、§4-S3、§1.1 原子性论证） |
| GitHub issue 编号 | **#48** —— https://github.com/PNZ-z/ai-powered-Fabflix/issues/48 |
| 远端标题 | `[Step 3/7] Upgrade to Spring Boot 3.5 and migrate to the Jakarta namespace` |
| 远端 state | **OPEN**（2026-10-19 创建；`Part of #44` 已在正文） |
| 来源 | `docs/plans/ai-option-a-boot3-migration.md` §2.1、§4.1、§4.2、§4.5、§6.1、§7；#44 正文 `Scope`（Build / Jakarta migration / Architecture guard test / CI / 部分 Verification）；`docs/plans/issue-44-migration-steps.md` |
| 关联 | 上游 epic：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**；正文 `## Steps` 列出 #46–#52）；正文含显式 `Part of #44`（P1-2），**已随创建发布**；承担 #44 的 **AC-1/AC-2/AC-3/AC-4/AC-8/AC-9/AC-10（后端 job）/AC-11（字节码 + 镜像部分）** |
| 依赖关系 | **前置 S1（基线）、S2（驱动定版）**；**后继 S4、S5**（都只依赖本步）；S6 依赖本步 |
| milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) |
| labels | **空** |
| 状态 | **🟢 已在 GitHub 存在（#48），无需新建** —— `github-operator` 按批准草稿创建（正文逐字一致，除末尾换行）；`## Internal tracking` 未发布。**实现：已实施（2026-10-20），待提交 / PR** —— 分支 `codex/issue-48-core-boot3-jakarta`（基点 `55f8d79`），18 文件；**未提交 / 未推送 / 无 PR / 未合并**，远端 #48 仍 **OPEN**；AC-1..AC-5 / AC-7..AC-9 已取证，**AC-6（PR 双 job CI）因尚无 PR 未取证** |
| 估算 | **2–3 天**（最大的一步，但单一目的） |
| 负责人角色 | docs-writer 写 `docs/plans/issue-<编号>-*.md` → oracle（重点审原子性与非目标）→ 主 agent 批准 → **单一 `worker`** → 3 reviewer（正确性/Boot 3 行为差异、测试覆盖与守卫断言、必要性与 diff 最小化）→ `worker` 修复 → docs-writer 回填 → 主 agent 提交 → `github-operator` 建 PR |
| 事实来源（可核验） | **本轮只读复核（2026-10-19）**：`grep -rl "javax\.servlet" src/main/java` = **exactly 11**（路径与正文逐条一致）；`grep -rln "javax\.\(sql\|xml\)"` = 4（`AdminRepository.java` + 3 个 XML parser）；`SpringBootOnlyArchitectureTest.java:39` 的 `assertFalse(source.contains("jakarta.servlet"))`；`pom.xml` `spring-boot.version=2.7.18`、`maven.compiler.source/target=11`、JaCoCo `excludes` = **8 项**；`ci.yml` job name `Build and test (Java 11)` / step `Set up Java 11` / `java-version: "11"`。BOM 传递升级版本号（HikariCP/Jackson/Logback/Tomcat）来自 #44 正文与 oracle 报告，**未在本仓库实测** |
| 未确认 | ① 具体 Boot 3.5 补丁版本（D-A1 只批到 3.5.x）；② 显式 pin 是否与 Boot 3.5 BOM 冲突（U-A2/D-A5 未裁定）；③ Boot 3 的实际行为差异范围（U-A4）；④ 127 用例数与 0.8980 是 2026-10-15 记录，**须由 S1 复跑确认**；⑤ 本机是否已安装 JDK 21（若否，本步无法满足 AC-5，需主 agent 批准安装）→ **已闭合（2026-10-19）**：JDK 21 已安装于 `/opt/homebrew/opt/openjdk@21`（`java version 21.0.12.1`），且 Maven 在该 `JAVA_HOME` 下**编译现有 Boot 2.7 代码成功**（仅编译，**未跑 `mvn verify`**）→ AC-5 的 `JAVA_HOME` 前置可满足，但本步**未执行**。**已闭合**：`jakarta.servlet-api` 字面量断言**预期保持成立**（starter-web 传递提供，`pom.xml` 无字面量，P2-8） |
| 待裁定 / 可调整点 | **已全部闭合（2026-10-19）**：**C-1 = (a)**（CI 三处改动在本步）、**C-3 = (a)**（只跑 `npm ci && npm run build`）、**B-1**（`Dockerfile` 两行 `FROM` 在本步）、**P1-3/P1-4/P1-8/P2-8** 已落入正文。**是否创建本 issue → 已创建 #48（2026-10-19）**。若主 agent 日后改变 C-1/C-3 裁定，需同步修订 Scope/AC（本稿不再预留分支）。 |

---

## 实施结果与 AC 回填（2026-10-20）

> 本分区**不发布到 GitHub**；上方 `## GitHub issue body` 围栏原文**未改**（AC 的措辞保持发布时的样子，逐条状态记录在此）。

| 项目 | 值 |
| --- | --- |
| 实施状态 | **已实施（2026-10-20），待提交 / PR** |
| 分支 | **`codex/issue-48-core-boot3-jakarta`**（基于 `main` @ `55f8d79` = `Merge pull request #55`） |
| 提交 / 推送 / PR | **未提交、未推送、无 PR**（`git diff --cached` 为空）；远端 #48 仍 **OPEN** |
| 改动规模 | `git diff --stat` = **18 files changed, 85 insertions(+), 38 deletions(-)** |
| 工具链 | `JAVA_HOME=/opt/homebrew/opt/openjdk@21` → **Java 21.0.12.1** |
| 证据 | **`docs/logs/2026-10-20-issue-48-core-upgrade.md`**（E1–E11 证据索引、§1.3 行为差异与残余风险、§二 新增知识点） |

### 逐条 AC 状态

| AC | 状态 | 证据 / 说明 |
| --- | --- | --- |
| **AC-1** `mvn verify`（`Tests run` 不低于基线；覆盖率达标；`excludes` 仍 8 项） | ✅ **满足** | `mvn --batch-mode --no-transfer-progress clean verify`（JDK 21）→ BUILD SUCCESS；**134 tests / 0 failures / 0 errors / 0 skipped**（收紧后基线 = 134）；`All coverage checks have been met.`；JaCoCo **988/1078 = 0.9165**；`grep -c "<exclude>"` = **8**；`<minimum>0.90</minimum>` 未变 |
| **AC-2** Jakarta 边界（(a) 恰 11 文件 + 守卫集合相等；(b) 额外改动逐条列出、无业务逻辑改动） | ✅ **满足** | (a) `grep -rl jakarta.servlet src/main/java` = **恰 11**（admin 2 / auth 2 / commerce 5 / search 1 / session 1），路径与守卫白名单逐条一致，`git diff` 中 11 个都在；(b) **`src/main/java` 的额外改动 = 0**（无业务逻辑改动）。**另有 3 个 `src/test/java` 的纯 import 替换必须单列**：`commerce/CartServiceTest.java:9`、`web/SpaFallbackTest.java:15`、`admin/EmployeeAuthenticationFilterTest.java:4` —— Boot 3 的 `spring-test` 只提供 `jakarta` 类型，不改则 `testCompile` 直接失败（实测 10 个编译错误）；**这 3 个文件不在 AC-2(a) 的 `src/main/java` 边界内**，且无断言/逻辑改动 |
| **AC-3** 打包形态（jar 内含 `BOOT-INF/classes/static/index.html`） | ✅ **满足** | `cd frontend && npm ci && npm run build` 成功 → `mvn -Pfrontend -DskipTests clean package` 成功 → jar 含 `BOOT-INF/classes/static/index.html`（463 B）与 `static/assets/…` |
| **AC-4** dist 缺失时 `-Pfrontend package` 必须失败 | ✅ **满足** | 移走 `frontend/dist` 后 → **BUILD FAILURE**（`enforcer:3.5.0:enforce (require-frontend-dist)`，`frontend/dist/index.html is missing`），随后已恢复；附加事实：dist 缺失时**默认** `mvn verify`（无 `-Pfrontend`）仍 BUILD SUCCESS → 后端门禁不依赖 dist |
| **AC-5** 字节码 61 + 工具链 JDK 21（证据记录 `JAVA_HOME`） | ✅ **满足** | `javap -verbose … com.fabflix.boot.FabflixApplication` → **`major version: 61`**（另以字节头 `cafe babe 0000 003d` 独立复核）；`mvn -version` → `Java version: 21.0.12.1`，仓库为 `/opt/homebrew/opt/openjdk@21`。**注意原文类名 `FablixApplication` 是笔误**，实际类为 **`com.fabflix.boot.FabflixApplication`**（照抄原文会得到「类找不到」，`pom.xml` 的 `mainClass` 亦为实际类名） |
| **AC-6** PR 的 CI 双 job 绿 + 后端 job 名反映新 JDK | ⬜ **未取证（待 PR）** | `ci.yml` 的可审查部分已完成：job 名 `Build and test (Java 21)`、step 名 `Set up Java 21`、`java-version: "21"`（其余步骤/缓存/前端 job 未动）。**但 PR 尚不存在，CI 未运行** —— 因此本 AC **不得**标记通过，须在 PR 出现后由主 agent 回填实际 CI 结果 |
| **AC-7** 记录所有 Boot 3 行为差异，且未放松任何质量门 | ✅ **满足** | 已逐条记入 `docs/logs/2026-10-20-issue-48-core-upgrade.md` §1.3，要点见下方「行为差异」。质量门未放松：阈值仍 0.90/90、`excludes` 仍 8、无跳过/删除用例（134 不变）、无禁用断言 |
| **AC-8** 未触碰 `@MockBean` / `README.md` / `compose.yaml` / `frontend/src/**` / `excludes` / 无关依赖；`mysql-connector-j` 仍为 S2 定的版本 | ✅ **满足** | `git diff --stat` 18 文件逐一核对：无 `README.md`、无 `compose.yaml`、无 `frontend/**`、无 `database/**`；`src/test` 无 `@MockBean` 改动（11 处弃用警告原样保留，归 S5）；`mysql-connector-j` 仍显式 **9.7.0** 且实际解析为 9.7.0（Boot 3.5.15 BOM 的 `mysql.version` 亦为 9.7.0，无漂移风险） |
| **AC-9** 容器路径（镜像 JDK 21 + 非 root + `docker compose build app` + health 200） | ✅ **满足** | `Dockerfile`：build `maven:3.9-eclipse-temurin-21`、runtime `eclipse-temurin:21-jre`，`uid/gid 1001` 非 root 块、`EXPOSE 8081`、`ENTRYPOINT` 未动；`docker compose build app` 成功；容器内 `openjdk version "21.0.12.1"`、`id fabflix` = `uid=1001 gid=1001`；`/api/boot/health` **200**（另完成 10 条深链、401/404 语义、顾客支付 `sales.id=13566`、员工 metadata 的 smoke） |

### 主 agent 对守卫的补强（终验后，2 行）

三路 review 只接受**一项代码补强**，主 agent 已采纳并复跑：在 `SpringBootOnlyArchitectureTest` 中加入
`assertFalse(source.contains("javax.servlet"), …)`（`src/main/java` 全量扫描）与 `assertFalse(pom.contains("javax.servlet-api"))`。
语义：守卫从「禁止 `jakarta.servlet`」反转为「受控白名单」后，**被淘汰的 `javax.servlet` 仍必须被禁止**，否则一次回退式改动会静默回归而守卫仍绿。
补强后**仍为 2 个 `@Test` 方法**，`Tests run` 仍 **134**，白名单集合相等断言未改。证据：`.pi/artifacts/worker-issue-48-core-upgrade.04d1a650.md`。

### 行为差异（AC-7 落地摘要）

1. **`/api/boot/health/`（尾斜杠）→ 401 不是 Boot 3 差异**：主 agent 在**升级前基点树**（`55f8d79`）实测同样 401。成因是 `CustomerAuthenticationFilter.shouldNotFilter` 的**精确字符串比较** `path.equals("/api/boot/health")`，且 **Servlet filter 先于 MVC handler mapping** 执行 → 与 Boot ≥3.2 的尾斜杠匹配默认值无关。**结论：既有行为，本步未改变、不修**；静态深链的尾斜杠场景**未测量**，留 S6。
2. **BOM 传递升级**：Tomcat 9.0 → **10.1.55**、Jackson 2.13 → **2.21.4**、HikariCP 4.0.3 → **6.3.3**、Logback 1.2 → **1.5.34**。已测范围内（HTTP 状态码、JSON 形状、启动日志无 warn/error/deprecat）**未观察到差异**；**连接池语义与并发未专门验证 → S6**（不得宣称「池行为无差异」）。
3. **Boot 3 新增编译期弃用**：`GoogleRecaptchaVerifier` 的 `RestTemplateBuilder.setConnectTimeout` / `setReadTimeout`（该类 `@Profile("!local")`、**0/17 覆盖**、本步未运行生产 reCAPTCHA）。**未修复**（超出「升级必需」范围，替代 API 未核实）→ **S6 风险 / 可选后续修复**。
4. **`@MockBean` 11 条弃用警告**（BOM 3.4+ `marked for removal`）→ **S5（#50）**，本步未改。
5. **显式 pin 与 BOM 的版本错配**（按 issue「keep the explicit pins unless a real conflict appears」保持不动）：`junit-jupiter 5.11.4`（聚合）vs `junit-jupiter-engine 5.12.2`（BOM）；`mockito-core 5.23.0` vs `mockito-junit-jupiter 5.17.0`（BOM；无 `MockitoExtension` → 惰性）。另：`pom.xml` 的 `junit-jupiter.version` / `junit-platform.version` 属性**对解析结果无影响**（既有无引用属性，属升级前技术债，本步未改）。
6. **测试数据副作用**：容器 smoke 的支付成功写入 dev 库 `sales.id=**13566**`（连同 S2 的 13562–13565）。清理 SQL：`DELETE FROM sales WHERE id IN (13562,13563,13564,13565,13566);`

### 仍待完成（不在本步范围 / 依赖后续动作）

1. **提交、推送与 PR**：由主 agent 执行（本步 18 文件 + `docs/` 下 5 个文件）；注意工作区有开工前既存的未跟踪 `?? .idea/`，不应带入提交。
2. **AC-6 回填**：PR 出现后核对 CI 双 job 与后端 job 名，再由 docs-writer/主 agent 回填。
3. **PR 说明**须收录本节的差异清单（AC-7 要求「写入 PR 说明与计划文档」）。
4. **S4 / S5 / S6 / S7**：enforcer + README（S4）、`@MockBean` → `@MockitoBean`（S5）、全量回归与生产 smoke（S6）、契约文档 §8/§11/§12 与路线图同步（S7）。

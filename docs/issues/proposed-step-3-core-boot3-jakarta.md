# Step 3/7 — 核心升级：Spring Boot 3.5 + `release=17` + Jakarta（GitHub issue 草稿）

> **文档状态：🟠 已按 oracle 审查修订 / 待用户批准创建（未批准，未发布）**
> `github-operator` 只发布 `## GitHub issue body` 分区（四反引号围栏只是本地容器，发布时去掉围栏行本身）；`## Internal tracking` **不上 GitHub**。
> 拆分总览：`docs/plans/issue-44-migration-steps.md`。**本步骤是 7 步中唯一不可再切的核心**（编译层面原子：Boot 3 无法编译 `javax.servlet`，守卫测试又主动禁止 `jakarta.servlet`）。
> **已裁定（2026-10-19，oracle 建议 + 主 agent）：** **B-1** `Dockerfile` 两行 `FROM` 并入本步（否则合并后 `docker compose build app` 损坏而 CI 仍绿）；**C-1 (a)** `ci.yml` 的 JDK 升版在本步；**C-3 (a)** 本步只跑 `npm ci && npm run build`；**P1-3** 所有命令需 `JAVA_HOME`=JDK 21；**P1-4** “恰好 11 文件”拆为两道判据；**P1-8** 保留 S2 的 `mysql-connector-j` 显式版本；**P2-8** `jakarta.servlet-api` 断言写为“expected to stay absent”；**P1-2** 正文加 `Part of #44`。
> 本文件**不声称**任何验证已运行；用例数/覆盖率均引用既有记录（2026-10-15）并标注待 S1 复跑确认。

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
- Quality gates that must not regress: `Tests run: 127` (recorded 2026-10-15), the JaCoCo `BUNDLE/LINE >= 0.85` gate with `968/1078 = 0.8980` behind it, and the 8-entry `excludes` list.

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
- No coverage-threshold change, no new JaCoCo `exclude`, no skipped tests, no `compose.yaml` change.
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
- [ ] **AC-7** Every Boot 3 behaviour difference encountered is documented in the pull request description (with the handling), and **no quality gate was relaxed** to reach green (no threshold change, no new `exclude`, no skipped test, no disabled assertion).
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
| 来源 | `docs/plans/ai-option-a-boot3-migration.md` §2.1、§4.1、§4.2、§4.5、§6.1、§7；#44 正文 `Scope`（Build / Jakarta migration / Architecture guard test / CI / 部分 Verification）；`docs/plans/issue-44-migration-steps.md` |
| 关联 | 上游 epic：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**拟转 epic，待用户批准**）；**正文已含显式 `Part of #44`（P1-2）**；承担 #44 的 **AC-1/AC-2/AC-3/AC-4/AC-8/AC-9/AC-10（后端 job）/AC-11（字节码 + 镜像部分）** |
| 依赖关系 | **前置 S1（基线）、S2（驱动定版）**；**后继 S4、S5**（都只依赖本步）；S6 依赖本步 |
| milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) |
| labels | **空** |
| 状态 | **🟠 已按 oracle 审查修订 / 待用户批准创建** |
| 估算 | **2–3 天**（最大的一步，但单一目的） |
| 负责人角色 | docs-writer 写 `docs/plans/issue-<编号>-*.md` → oracle（重点审原子性与非目标）→ 主 agent 批准 → **单一 `worker`** → 3 reviewer（正确性/Boot 3 行为差异、测试覆盖与守卫断言、必要性与 diff 最小化）→ `worker` 修复 → docs-writer 回填 → 主 agent 提交 → `github-operator` 建 PR |
| 事实来源（可核验） | **本轮只读复核（2026-10-19）**：`grep -rl "javax\.servlet" src/main/java` = **exactly 11**（路径与正文逐条一致）；`grep -rln "javax\.\(sql\|xml\)"` = 4（`AdminRepository.java` + 3 个 XML parser）；`SpringBootOnlyArchitectureTest.java:39` 的 `assertFalse(source.contains("jakarta.servlet"))`；`pom.xml` `spring-boot.version=2.7.18`、`maven.compiler.source/target=11`、JaCoCo `excludes` = **8 项**；`ci.yml` job name `Build and test (Java 11)` / step `Set up Java 11` / `java-version: "11"`。BOM 传递升级版本号（HikariCP/Jackson/Logback/Tomcat）来自 #44 正文与 oracle 报告，**未在本仓库实测** |
| 未确认 | ① 具体 Boot 3.5 补丁版本（D-A1 只批到 3.5.x）；② 显式 pin 是否与 Boot 3.5 BOM 冲突（U-A2/D-A5 未裁定）；③ Boot 3 的实际行为差异范围（U-A4）；④ 127 用例数与 0.8980 是 2026-10-15 记录，**须由 S1 复跑确认**；⑤ 本机是否已安装 JDK 21（若否，本步无法满足 AC-5，需主 agent 批准安装）。**已闭合**：`jakarta.servlet-api` 字面量断言**预期保持成立**（starter-web 传递提供，`pom.xml` 无字面量，P2-8） |
| 待裁定 / 可调整点 | **已全部闭合（2026-10-19）**：**C-1 = (a)**（CI 三处改动在本步）、**C-3 = (a)**（只跑 `npm ci && npm run build`）、**B-1**（`Dockerfile` 两行 `FROM` 在本步）、**P1-3/P1-4/P1-8/P2-8** 已落入正文。**仍待用户批准**：是否创建本 issue。若主 agent 日后改变 C-1/C-3 裁定，需同步修订 Scope/AC（本稿不再预留分支）。 |

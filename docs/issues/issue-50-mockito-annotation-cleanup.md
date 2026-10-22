# Step 5/7 — 测试注解清理：`@MockBean` → `@MockitoBean`（GitHub issue #50 记录）

> **文档状态：🟢 已在 GitHub 存在（[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)），无需新建** —— 2026-10-19 由 `github-operator` 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**，仅 GitHub 在末尾追加一个换行；`## Internal tracking` 分区**未发布**）。~~**本文件不表示该步骤已实施**：Scope / Acceptance criteria 全部未执行，无源码/配置/测试改动、无 PR。~~ → **2026-10-20 更新：本步已实施**（分支 `codex/issue-50-mockito-annotation`，基于 S4 合并后的 `main` = `9a7705c`；9 个测试文件 / 11 处 `@MockBean`→`@MockitoBean` + 9 个 import，`git diff --stat` = **20 insertions / 20 deletions**；JDK 21 `mvn verify` → **134 用例 / 0.9165 / 0.90 门禁通过**；clean 构建的 `MockBean` 弃用警告 **11 → 0**），**未提交 / 未推送 / 无 PR**（`origin/main..HEAD` 为空）→ **待提交 / PR**；AC-1..AC-5 逐条状态见文末「实施结果与 AC 回填（2026-10-20）」；工作流日志 `docs/logs/2026-10-20-issue-50-mockito-annotation-cleanup.md`。→ **2026-10-21 更新：本步已合并** —— PR **[#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)** 已合并（merge commit **`ff57e79`**），CI **双 job SUCCESS**，[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50) 已 **CLOSED**；AC-1..AC-5 **全部 ✅**。
> **远端元数据（主 agent 核验，2026-10-19）**：编号 **#50** · 标题 `[Step 5/7] Replace deprecated @MockBean with @MockitoBean in the tests` · milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) · labels **空** · state **OPEN** · 正文含显式 `Part of #44` · 上游 epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**）。
> `github-operator` 只发布 `## GitHub issue body` 分区（四反引号围栏只是本地容器，发布时去掉围栏行本身）；`## Internal tracking` **不上 GitHub**。
> 拆分总览：`docs/plans/issue-44-migration-steps.md`。
> **已裁定（C-2 = (a)，2026-10-19）：本步作为 epic 子 issue**（正文含 `Part of #44`）；epic #44 正文的 Non-goals 将以**澄清句**修订："The core upgrade step (S3) does not migrate `@MockBean`; step 5 (S5) does it as a follow-up step."（**已执行**：远端 #44 的 Non-goals 已于 2026-10-19 改为该澄清句，见 `docs/issues/issue-44-boot3-java17-jakarta-migration.md`）。
> **本轮修订（oracle P1-2 / P2-2）**：正文加显式 `Part of #44`；弃用警告证据给出**明确 grep 模式**（`MockBean` 专属，避免混入其他 Boot 3 弃用警告）。
> 本文件**不声称**任何验证已运行；文件数与出现次数来自本轮只读 grep（9 文件 / 11 处），用例数与覆盖率引用既有记录并待 S3/S6 复跑确认。

## GitHub issue title

```
[Step 5/7] Replace deprecated @MockBean with @MockitoBean in the tests
```

## GitHub issue body

````markdown
## Goal

Replace the deprecated `@MockBean` annotation with its supported replacement **`@MockitoBean`** in the backend test suite, so that the migration no longer produces deprecation warnings from the test framework.

`@MockBean` has been deprecated since Spring Boot 3.4 (the replacement is Spring Framework 6.2's `@MockitoBean`, semantically equivalent). The annotation still works, so this is a **cleanup step, not a fix**: the goal is a warning-free, future-proof test suite with byte-for-byte identical test behaviour.

**This issue covers only this step.** Only the annotation and its import change — no test logic, no assertion, no production code, no build configuration.

## Background

**Part of #44.**

This is **step 5 of 7** in the Spring Boot 3.x migration sequence documented in `docs/plans/issue-44-migration-steps.md` (plan: `docs/plans/ai-option-a-boot3-migration.md`). After the Spring Boot 3.5 upgrade (step 3) the suite reports deprecation warnings caused by `@MockBean`; the migration deliberately left them in place so the upgrade itself stayed focused.

Current usage — **9 test files, 11 annotation occurrences** (verified by grep on the tree before the Spring Boot upgrade):

- `src/test/java/com/fabflix/boot/auth/CustomerAuthControllerTest.java`
- `src/test/java/com/fabflix/boot/commerce/CartControllerTest.java`
- `src/test/java/com/fabflix/boot/commerce/CheckoutControllerTest.java`
- `src/test/java/com/fabflix/boot/commerce/PaymentControllerTest.java`
- `src/test/java/com/fabflix/boot/main/MainControllerTest.java`
- `src/test/java/com/fabflix/boot/movie/MovieDetailControllerTest.java`
- `src/test/java/com/fabflix/boot/movie/MovieListControllerTest.java`
- `src/test/java/com/fabflix/boot/search/SearchControllerTest.java`
- `src/test/java/com/fabflix/boot/star/StarDetailControllerTest.java`

The suite is `@WebMvcTest`-based plus `MockMvcBuilders.standaloneSetup` in the admin package (no `@SpringBootTest`), and it is covered by the JaCoCo `BUNDLE/LINE >= 0.90` gate (tightened from 0.85 on 2026-10-20 before the migration; this step must neither lower nor bypass it).

## Scope

- [ ] Replace `@MockBean` with `@MockitoBean` in the 9 files above (**11 occurrences**), including the import change: `org.springframework.boot.test.mock.mockito.MockBean` → `org.springframework.test.context.bean.override.mockito.MockitoBean`.
- [ ] If the annotation count differs from 11 at implementation time (files added or removed in the meantime), adjust the list and record the final count — do not silently leave a file behind (`grep -rn "@MockBean" src/test/java` must return nothing).
- [ ] Record the deprecation-warning evidence **before and after** with an explicit command, e.g. `mvn --batch-mode --no-transfer-progress verify 2>&1 | grep -ci "MockBean.*deprecat"` plus the first matching line verbatim. Keep the pattern **`MockBean`-specific** so unrelated Boot 3 deprecation warnings are not mixed in. This is evidence, not a build gate.
- [ ] Run `mvn --batch-mode --no-transfer-progress verify` and confirm the test count and coverage are unchanged.

## Non-goals

- **No test-logic change**: no assertion, no setup, no `.session(...)`/`.sessionAttr(...)` rewrite, no new test case, no deleted test case.
- No production source change, no `pom.xml` change, no dependency addition (`@MockitoBean` comes with the existing Spring test/context artifacts).
- No `MockMvcBuilders.standaloneSetup` → `@WebMvcTest` refactor (it is a deliberate existing pattern, and the admin tests rely on explicit `addFilters(...)`).
- No suppression of deprecation warnings via compiler flags, `@SuppressWarnings`, or `-nowarn`: the point is to remove the deprecated usage, not to silence it.
- No other Spring Boot 3 deprecation cleanups (they belong to their own follow-up if they exist).
- No CI/Dockerfile/README changes.

## Dependency

**Requires step 3** (Spring Boot 3.5) to be merged: `@MockitoBean` lives in Spring Framework 6.2, which only arrives with the Boot 3.x upgrade. It should be merged **before** the final regression step so that the tested artifact is the final one.

## Acceptance criteria

- [ ] **AC-1** `grep -rn "@MockBean" src/test/java` returns **no match**, and `grep -rn "@MockitoBean" src/test/java` returns **11** occurrences across **9** files (or the recorded adjusted count, with the reason).
- [ ] **AC-2** `mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS with the **same** test count and the same coverage result as the run before the change; `All coverage checks have been met.`.
- [ ] **AC-3** `git diff -U0` on the changed test files shows **only** import and annotation lines; no test body, assertion or fixture line is modified.
- [ ] **AC-4** The before/after deprecation-warning counts are recorded in the pull request description **with the exact command used** (e.g. `grep -ci "MockBean.*deprecat"`); AC-1 ("no `@MockBean` left") remains the real gate — the warning count is evidence only.
- [ ] **AC-5** No other file category is touched: no `src/main/**`, no `pom.xml`, no CI, no Dockerfile, no frontend.

## Risk notes

- **`@MockitoBean` has slightly different defaults than `@MockBean`** (it belongs to Spring Framework's bean-override support). Any behavioural difference must surface as a test failure; if a test fails, the honest response is to stop and report it as a finding — not to add local configuration that re-creates the old semantics silently.
- **`@WebMvcTest` + `@MockitoBean` interplay** is the main compatibility question; it is covered by running the full suite, but only for the code paths the suite exercises.
- **Warning counts are environment-sensitive** (Maven/compiler versions). They are recorded as evidence, not asserted as a gate.
- **The step is cosmetic**: if it turns out a test cannot be migrated without a logic change, the correct outcome is to exclude that file, record why, and raise it with the parent agent — because a behaviour-preserving cleanup is the entire justification for this issue.

## Rollback

Single pull request, mechanically revertible: reverting restores `@MockBean` and the previous imports, with deprecation warnings returning (accepted before this step). No production behaviour is involved.
````

## Internal tracking

> 本分区**不发布到 GitHub**。

| 项目 | 值 |
| --- | --- |
| 步骤 | **S5 / 7**（拆分总览 §3、§4-S5、**§6-C-2**） |
| GitHub issue 编号 | **#50** —— https://github.com/PNZ-z/ai-powered-Fabflix/issues/50 |
| 远端标题 | `[Step 5/7] Replace deprecated @MockBean with @MockitoBean in the tests` |
| 远端 state | **OPEN**（2026-10-19 创建；`Part of #44` 已在正文） |
| 来源 | `docs/plans/ai-option-a-boot3-migration.md` §4.3（`@MockBean` 行）、§4.6 末行（"建议拆到后续清理 issue"）、§4.7 第 1 条（允许的等价修改）；#44 正文 `Non-goals`（**明确排除本项**）；oracle B-2/Y-1 裁定（"本 issue 不迁移，后续 issue 处理"） |
| 关联 | 上游 epic：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**；正文 `## Steps` 列出 #46–#52）；正文含显式 `Part of #44`（P1-2），**已随创建发布**；**C-2 已裁定 (a) = epic 子 issue**（epic Non-goals 的澄清句**已在远端生效**，见文件头） |
| 依赖关系 | **前置 S3**；须在 **S6 之前**合并（否则 S6 的"最终形态"证据仍含弃用警告） |
| milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)（C-2 = (a) 后与其余子 issue 一致） |
| labels | **空** |
| 状态 | **🟢 已在 GitHub 存在（#50），无需新建** —— `github-operator` 按批准草稿创建（正文逐字一致，除末尾换行）；`## Internal tracking` 未发布。**实现：已合并（2026-10-21，PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) → merge `ff57e79`）** —— 分支 `codex/issue-50-mockito-annotation`（基点 `9a7705c`）；9 文件 / 11 处注解 + 9 import；CI 双 job **SUCCESS**；#50 已 **CLOSED**；AC-1..AC-5 均为 ✅（见文末回填） |
| 估算 | 0.5 天 |
| 负责人角色 | docs-writer → oracle → 主 agent 批准（含 C-2 裁定）→ 单一 `worker` → 3 reviewer（重点：diff 是否真的只含注解/import、用例数与覆盖率是否不变）→ `worker` 修复 → docs-writer 回填 → 主 agent 提交 → `github-operator` 建 PR |
| 事实来源（可核验，本轮只读 grep 2026-10-19） | `grep -rln "@MockBean" src/test/java` = **9 个文件**、`grep -rn "@MockBean" src/test/java \| wc -l` = **11 处**；文件清单与正文逐条一致；测试模式（`@WebMvcTest` + `standaloneSetup`、无 `@SpringBootTest`）见契约文档 §7；JaCoCo 闸门 **0.90**（2026-10-20 收紧）见 `pom.xml` |
| 未确认 | ① ~~`@MockitoBean` 在本仓库的 `@WebMvcTest` 用例下是否**完全**等价~~ → **已闭合（2026-10-20 实测）**：11 个 mock 目标（10 个 `@Service` 类 + `RecaptchaVerifier` 接口）均在 `@WebMvcTest` 切片之外，切换后 134 用例全绿、无一条失败；② ~~迁移前后的弃用警告计数（需实测）~~ → **已闭合**：clean 构建 **11 → 0**（整份日志弃用总数 **13 → 2**，见文末 AC-4）；③ ~~9 文件 / 11 处是否在 S3 之后发生变化~~ → **已闭合**：S3 未改这 9 个文件，实施时计数仍为 **9 / 11**；④ **（新）** context cache key 的实际语义 → **已按 spring-test 6.2.19 字节码核实**：参与 key，粒度为 handler 形状（类型/字段名/strategy/qualifier），已写入架构文档 §7 |
| 待裁定 | **已闭合（2026-10-19）**：**C-2 = (a)** —— 作为 epic 子 issue，正文含 `Part of #44`；epic Non-goals 改为澄清句（不缩减范围）。**是否创建本 issue → 已创建 #50（2026-10-19）**。**回退口径（已保留但未触发）**：若 `@MockitoBean` 在某个用例上无法等价替换，允许「该文件保留 `@MockBean` 并上报」——**实际 11 处全部成功替换，未触发该回退**。 |

---

## 实施结果与 AC 回填（2026-10-20）

> 本分区**不发布到 GitHub**；上方 `## GitHub issue body` 围栏原文**逐字未改**（AC 措辞保持发布时的样子，逐条状态记录在此）。

| 项目 | 值 |
| --- | --- |
| 实施状态 | **已合并（2026-10-21）**：PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) → merge commit `ff57e79`；CI 双 job **SUCCESS**；#50 已 **CLOSED**。实施完成于 2026-10-20 |
| 分支 | **`codex/issue-50-mockito-annotation`**（基于 `9a7705c` = `Merge pull request #57 from PNZ-z/codex/issue-49-jdk21-toolchain`，即 S4 合并后的 `main`） |
| 提交 / 推送 / PR | **已提交、已推送、已合并**：PR **[#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58)**，merge commit **`ff57e79`**（= `Merge pull request #58 from PNZ-z/codex/issue-50-mockito-annotation`）；CI **双 job SUCCESS**；[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50) 已 **CLOSED**（主 agent 核验） |
| 改动规模 | `git diff --stat` = **9 files changed, 20 insertions(+), 20 deletions(-)**（20 = 11 处注解 + 9 个 import）；`git diff --cached --stat` 空 |
| 工具链 | 全部 `mvn` 命令在 `JAVA_HOME=/opt/homebrew/opt/openjdk@21`（**Java 21.0.12.1**）下运行（S4 的 enforcer 已生效：非 21 的 `JAVA_HOME` 会在 `validate` 阶段失败） |
| 证据 | **`docs/logs/2026-10-20-issue-50-mockito-annotation-cleanup.md`**（§1.2 弃用计数口径、§1.3 context cache key、§1.5 遗留项、§2 新增知识点）；worker 报告 `.pi/artifacts/worker-issue-50-mockito.md` |

### 逐条 AC 状态

| AC | 状态 | 证据 / 说明 |
| --- | --- | --- |
| **AC-1** `grep -rn "@MockBean" src/test/java` 无命中；`@MockitoBean` = 11 处 / 9 文件 | ✅ **满足** | 本轮只读复核：`grep -rn "@MockBean" src/test/java` = **0 行**；`grep -rn "@MockitoBean" src/test/java` = **11 处 / 9 文件**（文件清单与正文 9 条逐条一致：auth 2 处、commerce 4 处、main 1、movie 2、search 1、star 1）；`@SpyBean`/`@MockitoSpyBean` = **0**。**计数无需调整**（S3 未改这 9 个文件） |
| **AC-2** `mvn verify` → BUILD SUCCESS，用例数与覆盖率与改动前一致；`All coverage checks have been met.` | ✅ **满足** | 主 agent 终验（JDK 21）：**BUILD SUCCESS**；`Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`（改动前同为 134）；JaCoCo **988 covered / 90 missed / 1078 = 0.9165**（与改动前逐位相同）；`All coverage checks have been met.`（阈值仍 **0.90**、`excludes` 仍 **8**，本步未动 `pom.xml`） |
| **AC-3** `git diff -U0` 只显示 import 与注解行 | ✅ **满足** | 逐 hunk 核对：全部 hunk 头要么是 import 的删/增（**9 组**），要么是 `@MockBean` → `@MockitoBean`（**11 处**，其中 `SearchControllerTest` 为单行声明 `@MockitoBean SearchService searchService;`）；**无**测试方法体、断言、fixture、`.session(...)`/`.sessionAttr(...)`、`@WebMvcTest`/`@Import`/`standaloneSetup` 结构变化；无 `@SuppressWarnings`、无编译参数改动。注意 7 个文件的 import 是**位移**（不在同一行位置），因此 `+20/−20` 不等于「同一位置改 20 行」 |
| **AC-4** 前后弃用计数与**确切命令**记录在 PR 说明 | ✅ **满足（口径已澄清，必须随 PR 发布）** | ① 正文规定的 `mvn … verify 2>&1 \| grep -ci "MockBean.*deprecat"` 在**增量构建**下 before/after 都是 **0**（日志含 `Nothing to compile - all classes are up to date.`，javac 未重编译 → 无任何警告），**不能**用来证明 before=11；② 权威 before = **`mvn clean verify`**（同一未改动树）→ `grep -ci "MockBean.*deprecat"` = **11**，首条为 `…CustomerAuthControllerTest.java:[30,6] org.springframework.boot.test.mock.mockito.MockBean … has been deprecated and marked for removal`；③ after（clean）= **0**；④ 交叉校验：整份日志的弃用警告总数 **13 → 2**，差值恰为 **11**，剩余 2 条是 **范围外**的 `GoogleRecaptchaVerifier` `setConnectTimeout`/`setReadTimeout`（S3 已登记、归 **S6**）。**AC-1 仍是真正的闸门**，计数只是证据。**PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) 已合并（merge `ff57e79`）** → 该口径已随 PR 交付（本表为权威版本；PR 描述正文是否逐字包含该计数**本轮未单独核验**） |
| **AC-5** 不触碰其他文件类别 | ✅ **满足** | `git status --short` 只有 `M` 的 **9 个 `src/test/java/**/*Test.java`** + 开工前既存的未跟踪 `?? .idea/`（不属本次改动、不得带入提交）。**零改动**：`src/main/**`、`pom.xml`、`.github/workflows/ci.yml`、`Dockerfile`、`compose.yaml`、`frontend/**`、JaCoCo 阈值/`excludes`、`mysql-connector-j` pin、`maven.compiler.release` |

### 语义与风险（已闭合）

- **等价性**：11 个 mock 目标 = **10 个 `@Service` 类**（`CustomerAuthService`、`CartService`×3、`PaymentService`、`GenreService`、`MovieDetailService`、`MovieListService`、`SearchService`、`StarDetailService`）+ **1 个接口** `RecaptchaVerifier`（实现为 `@Component` + `@Profile("local"|"!local")`）。**全部不在 `@WebMvcTest` 切片内** → 两种注解在此路径上等价；134 用例全绿、**无一条**因注解切换而失败，因此**未触发**「保留 `@MockBean` 并上报」的回退口径。
- **context cache key（按库实现核实）**：`@MockitoBean` 的 override 通过 `BeanOverrideContextCustomizer` **参与** Spring Test 的 context cache key，粒度为 **handler 形状**（bean 类型、字段名、`BeanOverrideStrategy`、qualifier 注解；**不含声明测试类**）。因此 mock 形状相同的测试类可共享 context，mock 集不同则不共享。**本仓库当前无共享**：10 个 `@WebMvcTest` 类各自钉住不同 controller，另有 4 个 `standaloneSetup` 文件无 context 缓存。该结论已写入 `docs/architecture/frontend-migration-contracts.md` §7（**S7 无需再改 §7 的测试模式描述**）。

### 范围边界（未扩大）

**未走备选方案**：没有保留任何一处 `@MockBean`、没有加 `@SuppressWarnings`/`-nowarn`、没有改写断言、没有把 `standaloneSetup` 改成 `@WebMvcTest`、没有顺手修 `GoogleRecaptchaVerifier` 的 2 条弃用（属 S6）。**未新增依赖**（`@MockitoBean` 随既有 `spring-test` 6.2 提供）。

### 仍待完成（依赖后续动作）

1. ~~**提交、推送与 PR**：由主 agent 执行（9 个测试文件 + 本轮 `docs/`）；工作区既存的未跟踪 `?? .idea/` 不应带入提交。~~ → ✅ **已完成（2026-10-21）**：PR [#58](https://github.com/PNZ-z/ai-powered-Fabflix/pull/58) 已合并（merge `ff57e79`），`?? .idea/` 未带入。
2. ~~**PR 的 CI**：PR 出现后由主 agent 核验并回填（预期与 S4 相同：后端 `Build and test (Java 21)` + 前端 job 双绿）。~~ → ✅ **已完成**：CI **双 job SUCCESS**。
3. **登记给 S6（本步不改代码）**：`GoogleRecaptchaVerifier` 的 `setConnectTimeout`/`setReadTimeout` 弃用（构建仍有 **2** 条弃用警告，「零弃用」当前**不成立**）；`@MockitoSpyBean`/`@MockitoBean(name=…)` 路径未在本仓库使用，故未验证。→ **S6 已于 2026-10-21 执行并如实登记为「未执行 / 风险」**（`docs/logs/2026-10-21-issue-51-regression-proof.md` §0.7 与 §1.2 第 2 条）：生产 reCAPTCHA 路径未执行、剩余 2 条弃用仍在；S6 的结论是「`MockBean` 相关弃用 **11 → 0**」，而非「零弃用」。

# Step 5/7 — 测试注解清理：`@MockBean` → `@MockitoBean`（GitHub issue #50 记录）

> **文档状态：🟢 已在 GitHub 存在（[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)），无需新建** —— 2026-10-19 由 `github-operator` 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**，仅 GitHub 在末尾追加一个换行；`## Internal tracking` 分区**未发布**）。**本文件不表示该步骤已实施**：Scope / Acceptance criteria 全部未执行，无源码/配置/测试改动、无 PR。
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

The suite is `@WebMvcTest`-based plus `MockMvcBuilders.standaloneSetup` in the admin package (no `@SpringBootTest`), and it is covered by the JaCoCo `BUNDLE/LINE >= 0.85` gate.

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
| 状态 | **🟢 已在 GitHub 存在（#50），无需新建** —— `github-operator` 按批准草稿创建（正文逐字一致，除末尾换行）；`## Internal tracking` 未发布。**实现未开始**（C-2 已裁定，无剩余阻塞） |
| 估算 | 0.5 天 |
| 负责人角色 | docs-writer → oracle → 主 agent 批准（含 C-2 裁定）→ 单一 `worker` → 3 reviewer（重点：diff 是否真的只含注解/import、用例数与覆盖率是否不变）→ `worker` 修复 → docs-writer 回填 → 主 agent 提交 → `github-operator` 建 PR |
| 事实来源（可核验，本轮只读 grep 2026-10-19） | `grep -rln "@MockBean" src/test/java` = **9 个文件**、`grep -rn "@MockBean" src/test/java \| wc -l` = **11 处**；文件清单与正文逐条一致；测试模式（`@WebMvcTest` + `standaloneSetup`、无 `@SpringBootTest`）见契约文档 §7；JaCoCo 0.85 闸门见 `pom.xml` |
| 未确认 | ① `@MockitoBean` 在本仓库的 `@WebMvcTest` 用例下是否**完全**等价（U-A3：`@MockBean` 在 Boot 3.5 是 deprecated 还是已不可用 —— 计划标注"未在本仓库实测"）；② 迁移前后的弃用警告计数（需实测）；③ 9 文件/11 处是否在 S3 之后发生变化（S3 不改测试，预期不变，但需实施时复查） |
| 待裁定 | **已闭合（2026-10-19）**：**C-2 = (a)** —— 作为 epic 子 issue，正文含 `Part of #44`；epic Non-goals 改为澄清句（不缩减范围）。**是否创建本 issue → 已创建 #50（2026-10-19）**。另：若 `@MockitoBean` 在某个用例上无法等价替换，草稿的 Risk notes 允许“该文件保留 `@MockBean` 并上报”——该回退口径保持不变（需主 agent 在实施时确认）。 |

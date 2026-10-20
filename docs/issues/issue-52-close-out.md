# Step 7/7 — 收尾：架构约定、README、路线图状态与工作流日志（GitHub issue #52 记录）

> **文档状态：🟢 已在 GitHub 存在（[#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52)），无需新建** —— 2026-10-19 由 `github-operator` 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**，仅 GitHub 在末尾追加一个换行；`## Internal tracking` 分区**未发布**）。**本文件不表示该步骤已实施、也不表示迁移已完成**：Scope / Acceptance criteria 全部未执行，无源码/配置/测试改动、无 PR。
> **远端元数据（主 agent 核验，2026-10-19）**：编号 **#52** · 标题 `[Step 7/7] Close out the migration: architecture docs, roadmap status and workflow log` · milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) · labels **空** · state **OPEN** · 正文含显式 `Part of #44` · 上游 epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**）。
> `github-operator` 只发布 `## GitHub issue body` 分区（四反引号围栏只是本地容器，发布时去掉围栏行本身）；`## Internal tracking` **不上 GitHub**。
> 拆分总览：`docs/plans/issue-44-migration-steps.md`。本步骤**不改任何源码/测试/配置**，只让文档与最终实现一致，并建立本次工作流的日志。
> **本轮修订（oracle P1-6 / P2-6 / P1-2）**：契约文档 **§11**（容器两阶段镜像 = JDK 21）**显式列入 Scope/AC**，不再只依赖 AC-3 的 drift 检查；工作流日志编号明确用 **S7 自己的 issue 编号**（epic 在日志中引用）；正文加显式 `Part of #44`。
> 本文件**不声称**任何验证已运行、任何迁移已完成。

## GitHub issue title

```
[Step 7/7] Close out the migration: architecture docs, roadmap status and workflow log
```

## GitHub issue body

````markdown
## Goal

Make the written record match the delivered implementation: update the live architecture conventions and the roadmap status, then close the migration with a workflow log that states what changed, what was verified and what was not.

**This issue covers only this step.** It is documentation and tracking only — no production code, tests, `pom.xml`, CI or `Dockerfile` change. Documentation must describe what the code **actually** does (verified by reading the merged code), not what the plan intended.

## Background

**Part of #44.**

This is **step 7 of 7** — the close-out of the Spring Boot 3.x / Java 17 / Jakarta migration tracked by #44 (`docs/plans/ai-option-a-boot3-migration.md`, split in `docs/plans/issue-44-migration-steps.md`). By the time this step runs, steps 1–6 have delivered the baseline, the validated JDBC driver, the Boot 3.5 / `release=17` / Jakarta upgrade, the JDK 21 enforcement and images, the `@MockitoBean` cleanup, and the full regression evidence.

Four places are known to be stale after the migration:

1. `docs/architecture/frontend-migration-contracts.md` **§8** describes the architecture guard test as *forbidding* `jakarta.servlet` in `src/main/java`. After the migration the guard is a **controlled allow-list of exactly 11 file paths** (set equality), while the `@WebServlet` / `extends HttpServlet` bans and the `pom.xml` assertions (`jar`, `repackage`, no `jakarta.servlet-api` literal) remain.
2. The same document's **§12** states the container/CI versions (`maven:3.9-eclipse-temurin-11`, `eclipse-temurin:11-jre`, CI Java 11). After the migration: both Docker stages and CI use **JDK 21**, with `release=17` as the bytecode level.
3. The same document's **§11** (production static resources and SPA fallback conventions) also names the JDK 11 container images. After the migration the two-stage image is **JDK 21** in both stages; this section is **explicitly in scope** here, not merely "drift found by AC-3".
4. The same document's **§7** previously carried a stale coverage-margin sentence ("~106 lines remaining"). It was already updated on 2026-10-20 together with the threshold tightening, so the current statement is: **90 lines are currently uncovered and the 0.90 gate allows at most ≈19 additional uncovered lines** (`988/(1078+N) >= 0.90 ⇒ N <= 19`); the backend gate is **0.90** and the frontend `lines` threshold is **90**. This step **re-checks** that wording against the delivered tree (and recomputes the margin if the test suite changed) — it must not lower or bypass the tightened thresholds.

## Scope

- [ ] Update `docs/architecture/frontend-migration-contracts.md`: **§8** (guard = 11-path allow-list with set equality; what is still banned), **§12** (CI/Docker JDK 21, `release=17`, bytecode major version 61), **§11** (two-stage image = JDK 21, SPA fallback conventions unchanged), **§7** (coverage margin re-checked against the tightened 0.90 gate as above). Keep the document's existing structure and labelling conventions.
- [ ] Verify/extend `README.md` so that the JDK 21 requirement, `JAVA_HOME`, build/run commands and the mirror/compose instructions match the delivered build (step 4 already wrote the JDK paragraph — this step checks it against the code and fixes drift only).
- [ ] Update `docs/plans/ai-roadmap-overview.md`: the execution-status section for Option A (the `§12.3` action table and the M3 milestone row) so it reflects what was actually delivered, and keep earlier entries as history rather than rewriting them.
- [ ] Update the #44 tracking record in `docs/issues/`: record the delivered scope, any approved deviation, the residual risks, and the links to the step issues. **Do not edit GitHub directly** — remote issue/milestone edits are performed by the parent agent / GitHub operator after approval.
- [ ] Write the workflow log at `docs/logs/YYYY-MM-DD-issue-<number>-<slug>.md`, **where `<number>` is this step's own issue number (not #44 — the epic is referenced inside the log)**, with (in this exact two-part structure):
      1. **which files changed and what problems remain** — per file or per file group, the actual change and its purpose, followed by residual risks, boundary limits, unverified items and follow-ups; if nothing remains, state "no known residual issues" explicitly;
      2. **new knowledge / key learning points** — grouped by technology/concept, each with what it means, where it lives (file/location) and why it is worth learning; if nothing new was introduced, say so explicitly.
- [ ] Verify documentation/code agreement mechanically where possible: the allow-list size asserted in `SpringBootOnlyArchitectureTest` equals the count documented in §8 (11), and the JDK versions documented in §12 equal the ones in `ci.yml` and the `Dockerfile`.
- [ ] State explicitly what the migration does **not** deliver (no AI capability; the Spring AI prerequisite is only unlocked) so the roadmap's later items are not misread as done.

## Non-goals

- **No source, test, build, CI or container change.** If documentation and code disagree, report the disagreement — do not "fix" the code here.
- No rewriting of historical records: past workflow logs (`docs/logs/2026-*`) and the closed-issue plan/issue documents are history and stay as they are, even where they still mention `Build and test (Java 11)`.
- No GitHub write operations: creating/editing/closing issues, changing milestones, commenting and opening pull requests are the parent agent's / GitHub operator's job.
- No new architecture decision: this step documents decisions already approved; if a new cross-issue convention would be needed, raise it instead of writing it.
- No coverage-threshold lowering or bypassing (the gates are the threshold values approved on 2026-10-20: backend JaCoCo 0.90, frontend `lines` 90) and no `excludes` change; no `@MockBean` leftovers cleanup (that was step 5); no rename of the `ai-*.md` plan files (separately decided).

## Dependency

**Requires step 6** (the regression evidence) — the log must quote real, executed results; and requires steps 3–5 merged so the documented guard/versions are the real ones.

## Acceptance criteria

- [ ] **AC-1** `docs/architecture/frontend-migration-contracts.md` §8 describes the 11-path allow-list with set equality, §12 states JDK 21 for CI and both image stages plus `release=17`, **§11 states the two-stage image is JDK 21** (and keeps the SPA-fallback conventions), and §7 no longer contains the stale "~106 lines" figure. A reader can verify each statement against `SpringBootOnlyArchitectureTest`, `ci.yml` and the `Dockerfile`.
- [ ] **AC-2** The allow-list count in the documentation equals the count asserted in the guard test equals the number of `src/main/java` files containing `jakarta.servlet` (11), each verified by a recorded command.
- [ ] **AC-3** `README.md` and the delivered build agree (JDK 21 requirement, `JAVA_HOME`, commands); any drift found is fixed in this step and recorded.
- [ ] **AC-4** `docs/plans/ai-roadmap-overview.md` reflects the actual execution status of Option A; earlier history is preserved, not rewritten.
- [ ] **AC-5** The workflow log exists with the two mandatory sections, is named with **this step's own issue number**, contains the real command results from step 6 (with "not executed" items called out) and references the epic #44, and states "no known residual issues" or lists them.
- [ ] **AC-6** The pull request diff contains only `docs/` and `README.md` changes; `git diff --stat` shows no source, test, `pom.xml`, `ci.yml` or `Dockerfile` change.
- [ ] **AC-7** The step's own completion note states what remains open (e.g. an unexecuted production smoke, `@MockBean`-related follow-up, the roadmap's next milestone) rather than claiming a clean finish.

## Risk notes

- **Documentation drift is the main risk**: §8/§12 describe the guard and the toolchain, and a wrong description silently weakens the architecture constraint. AC-1/AC-2 therefore require cross-checking against the code, not against the plan.
- **Over-claiming is a failure mode**: if step 6 recorded "not executed" items, the log must repeat them; closing the migration with an implied "all verified" would contradict the project rule of distinguishing executed / not executed / not runnable.
- **Scope creep risk**: it is tempting to "while we are here" fix unrelated stale paragraphs. Only the three named places plus drift found by AC-3 are in scope; anything else is recorded as a follow-up.
- **Remote/GitHub state**: milestone and epic state changes are not made by the implementing agent, so the log must not state them as done before the parent agent confirms.

## Rollback

Documentation only: reverting the pull request removes the updates but leaves the delivered migration and its evidence intact; the stale descriptions would return, so a revert should be paired with a follow-up fix rather than left in place.
````

## Internal tracking

> 本分区**不发布到 GitHub**。

| 项目 | 值 |
| --- | --- |
| 步骤 | **S7 / 7**（拆分总览 §3、§4-S7） |
| GitHub issue 编号 | **#52** —— https://github.com/PNZ-z/ai-powered-Fabflix/issues/52 |
| 远端标题 | `[Step 7/7] Close out the migration: architecture docs, roadmap status and workflow log` |
| 远端 state | **OPEN**（2026-10-19 创建；`Part of #44` 已在正文） |
| 来源 | #44 正文 `Scope → Docs (live documents only)`（§8/§12 + 陈旧覆盖率数字）；`docs/plans/ai-option-a-boot3-migration.md` §2.1-7、§5-A7；`AGENTS.md` §8（日志两大分区）；`docs/architecture/frontend-migration-contracts.md` §7/§8/§12（本轮只读核对：§7 确有 "~106 行" 表述、§8 确有"不得出现 `jakarta.servlet`"、§12 确有 `maven:3.9-eclipse-temurin-11` / `eclipse-temurin:11-jre`） |
| 关联 | 上游 epic：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**；正文 `## Steps` 列出 #46–#52）；正文含显式 `Part of #44`（P1-2），**已随创建发布**；本步承担 #44 的 **AC-8 的文档侧**，并在关闭时汇总 AC-1..AC-11 的证据 |
| 依赖关系 | **前置 S6**（以及 S3/S4/S5 已合并） |
| milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)（milestone 的实际关闭由主 agent / `github-operator` 决定，本步只核对状态并记录） |
| labels | **空** |
| 状态 | **🟢 已在 GitHub 存在（#52），无需新建** —— `github-operator` 按批准草稿创建（正文逐字一致，除末尾换行）；`## Internal tracking` 未发布。**实现未开始**（S7 的前置 S1–S6 全部未开始） |
| 估算 | 0.5 天 |
| 负责人角色 | docs-writer → oracle → 主 agent 批准 → 单一 `worker`（只改文档并跑核对命令）→ 3 reviewer（文档与代码一致性、是否改写历史记录、是否过度声称）→ `worker` 修复 → docs-writer 定稿 → 主 agent 提交/推送 → `github-operator` 建 PR；**epic/milestone 的关闭由主 agent 决定并执行** |
| 事实来源（可核验，本轮只读） | 契约文档 §7（"~106 行" 陈旧表述）、§8（`jakarta.servlet` 禁令原文）、§12（`maven:3.9-eclipse-temurin-11` / `eclipse-temurin:11-jre` / CI Java 11）；`SpringBootOnlyArchitectureTest.java`（当前禁令 + `pom.xml` 三条断言）；`README.md`（无 JDK 版本要求章节，S4 补）；路线图 `docs/plans/ai-roadmap-overview.md` §12.3 / M3 行 |
| 未确认 | ① S4 写入 README 后还剩哪些漂移（需实施时读 diff 判断）；② 路线图 §12.3 的行号/编号在实施前是否变化；③ S6 是否留下“未执行”项（决定日志与关闭口径）。**已闭合**：**P1-6** 契约文档 **§11** 已**显式列入 Scope/AC-1**（不再依赖 AC-3 的 drift 检查）；**P2-6** 日志编号用 **S7 自己的 issue 编号** |
| 待裁定 | **已全部闭合（2026-10-19）**：**P1-6** 纳入 §11；**P2-6** 日志编号口径已明确。**是否创建本 issue → 已创建 #52（2026-10-19）**。**#44 的关闭方式与时机**仍由主 agent 裁定（本稿按“epic 保持 open 到 S7 完成、由主 agent 关闭”记录）。 |

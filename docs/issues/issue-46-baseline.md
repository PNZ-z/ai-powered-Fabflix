# Step 1/7 — 迁移前基线与跟踪 checklist（GitHub issue #46 记录）

> **文档状态：🟢 已在 GitHub 存在（[#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)），无需新建** —— 2026-10-19 由 `github-operator` 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**，仅 GitHub 在末尾追加一个换行；`## Internal tracking` 分区**未发布**）。**本文件不表示该步骤已实施**：正文的 Scope / Acceptance criteria 全部未执行，无源码/配置/测试改动、无 PR。
> **S1 进度（2026-10-20）**：基线**已测量并记录**（`docs/logs/2026-10-20-issue-46-baseline.md`；checklist `docs/plans/issue-44-migration-checklist.md`），树内**仍无源码/pom/CI/Dockerfile 改动**；但记录**未提交、未推送、无 PR**，因此 #46 **仍未完成**，状态保持 OPEN。
> **远端元数据（主 agent 核验，2026-10-19）**：编号 **#46** · 标题 `[Step 1/7] Record the pre-migration baseline and start the migration checklist` · milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) · labels **空** · state **OPEN** · 正文含显式 `Part of #44` · 上游 epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**）。
> 本文件是 GitHub issue 的**规范来源与本地跟踪记录**（原名 `docs/issues/proposed-step-1-baseline.md`，issue 创建后改名）。`github-operator` 只发布 `## GitHub issue body` 分区（四反引号围栏只是本地容器，发布时去掉围栏行本身）；`## Internal tracking` **不上 GitHub**。
> 拆分总览：`docs/plans/issue-44-migration-steps.md`（**已批准并已创建子 issue，2026-10-19**）。
> **本轮修订（oracle P1-5 / P2-4 / P1-2）**：删去“安装 JDK 是允许动作”（→ 安装需主 agent 批准）；AC-4 补跨 JDK 对比规则；正文加显式 `Part of #44`。
> 本文件**不声称**任何验证已运行：所有数字均来自既有记录（`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`）与本轮只读 grep，**本轮未运行 `mvn` / npm / Docker / CI**。

## GitHub issue title

```
[Step 1/7] Record the pre-migration baseline and start the migration checklist
```

## GitHub issue body

````markdown
## Goal

Record a **fresh pre-migration baseline** on the current, unmodified **Spring Boot 2.7.18 / Java 11** tree, so every later step can be compared against numbers taken today rather than against values quoted from an older run. Also start the migration checklist that tracks the seven steps and their rollback points.

**This issue covers only this step.** No production source, `pom.xml`, CI or Dockerfile change is allowed here: the whole value of the step is that the tree is untouched, so the numbers are a true reference.

## Background

**Part of #44.**

This is **step 1 of 7** in the Spring Boot 3.x / Java 17 / Jakarta migration tracked by #44. The migration is planned in `docs/plans/ai-option-a-boot3-migration.md` and split into seven independently verifiable steps in `docs/plans/issue-44-migration-steps.md`.

The migration must not regress the current quality gates:

- backend: `Tests run: 127`, JaCoCo `BUNDLE/LINE` 968/1078 = **0.8980** (gate 0.85);
- frontend: **195** tests in 30 files, `lines` **92.35%** (846/916), gate 88;
- end-to-end: **12** Playwright executions (6 cases × 2 viewports).

Those numbers were recorded on 2026-10-15 (`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`). They are **not** today's numbers: dependency caches, Node/Maven versions and the local JDK can all shift the result. Later steps' acceptance criteria ("count must not decrease") are only meaningful against a baseline taken immediately before the migration.

## Scope

- [ ] Confirm the tree is clean before measuring (`git status --short`, `git rev-parse HEAD`) and record the commit SHA in the baseline record.
- [ ] Record the JDK actually used by Maven (`mvn -version`, full output). It is expected to be **JDK 27** today; the migration target toolchain is **JDK 21**.
- [ ] Check whether **JDK 21** is available locally (`/usr/libexec/java_home -v 21` or an equivalent check) and record the result. **If JDK 21 is absent, record it as a blocking gap for steps 3–4. Installing a JDK requires main-agent approval — do not install it inside this step.**
- [ ] Run and record verbatim: `mvn --batch-mode --no-transfer-progress verify` — capture `Tests run`, the JaCoCo line ratio, and the presence of `All coverage checks have been met.`.
- [ ] Run and record verbatim in `frontend/`: `npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e` — capture test count, coverage lines %, and Playwright pass count.
- [ ] **If JDK 21 is available**, additionally attempt `mvn --batch-mode --no-transfer-progress verify` with `JAVA_HOME` pointing at JDK 21 and record the outcome **honestly**, including "Boot 2.7.18 cannot run on JDK 21" if that is what happens. Do not modify any source file to make it pass.
- [ ] Create the migration checklist document: the seven steps with (a) their one-line goal, (b) the exact verification commands, (c) their rollback point, (d) the commit/branch where each was completed (filled in as the steps land).
- [ ] Note explicitly in the record which checks the automation cannot cover (real browser refresh, visual/contrast checks, real-MySQL concurrency) so no later step claims them.

## Non-goals

- **No change to `pom.xml`, `src/**`, `frontend/**`, `.github/workflows/ci.yml` or `Dockerfile`.** This step is measurement plus documentation only.
- No dependency upgrade (that is step 2), no Spring Boot upgrade (step 3), no JDK/Docker/CI change (steps 3 and 4), no test annotation cleanup (step 5).
- No new test cases and no test edits: the number recorded must be the number the current suite produces.
- No GitHub issue creation, no commit/push performed by the worker: the delivery workflow is handled by the parent agent and the GitHub operator.

## Dependency

None. This step can run in parallel with step 2. It must be finished **before** step 3, because step 3 and step 6 compare against this baseline.

## Acceptance criteria

- [ ] **AC-1** A baseline record exists in `docs/` containing the commit SHA, `mvn -version` output, and the raw result of `mvn --batch-mode --no-transfer-progress verify` (test count, failures, errors, skipped, JaCoCo line covered/missed/total and ratio).
- [ ] **AC-2** The same record contains the raw results of the five frontend commands with the Playwright pass count and the coverage `Lines` percentage.
- [ ] **AC-3** `git status --short` after the step shows **no modification** to `pom.xml`, `src/`, `frontend/` or `.github/`; the diff of the pull request contains only files under `docs/`.
- [ ] **AC-4** The record states the JDK used for the measurement and, if a JDK 21 attempt was made, its outcome (pass/fail plus the first error line when it failed). If JDK 21 is missing, the record says so explicitly **(blocking gap for steps 3–4; installing a JDK is not part of this step)** and records the **comparison rule**: if a JDK 21 baseline is not obtainable, steps 3 and 6 must compare against step 1's numbers **on the same JDK**, or state an explicit **cross-JDK caveat** — never silently subtract numbers taken on different JDKs.
- [ ] **AC-5** The migration checklist lists all seven steps with verification commands and rollback points, and states the current status of each (expected: step 1 in progress, steps 2–7 not started).
- [ ] **AC-6** The record lists the known non-automated areas (browser refresh, visual/contrast, real-MySQL concurrency) as explicitly out of automated coverage.

## Risk notes

- **The baseline may not be reproducible on JDK 21.** Boot 2.7.18 predates JDK 21; if `mvn verify` fails there, the only honest option is to record the failure and keep the JDK 27 baseline, flagging later comparisons as "cross-JDK". Do not work around it by changing code in this step.
- **Numbers may legitimately differ from the 2026-10-15 record** (Maven/Node patch versions, dependency resolution, machine load). Today's number is authoritative for comparisons; a difference is not a defect.
- **The E2E run needs a browser download** (`npx playwright install --with-deps chromium`) and takes several minutes; if the environment cannot install it, record "not executed" instead of claiming a pass.
- **A stale baseline is worse than no baseline**: if step 1 is measured, then step 2 changes `pom.xml`, step 3 must compare against step 1's record, not a re-run made after step 2.

## Rollback

Documentation only: reverting the pull request removes the record and nothing else. No build, runtime or CI behaviour is affected.
````

## Internal tracking

> 本分区**不发布到 GitHub**。

| 项目 | 值 |
| --- | --- |
| 步骤 | **S1 / 7**（拆分总览 §3、§4-S1） |
| GitHub issue 编号 | **#46** —— https://github.com/PNZ-z/ai-powered-Fabflix/issues/46 |
| 远端标题 | `[Step 1/7] Record the pre-migration baseline and start the migration checklist` |
| 远端 state | **OPEN**（2026-10-19 创建；`Part of #44` 已在正文） |
| 来源 | `docs/plans/issue-44-migration-steps.md`（拆分提案，**待审查**）；`docs/plans/ai-option-a-boot3-migration.md` §3/§5-A0/§6.1；#44 正文 Scope 第 1 条（"Baseline (first step)"） |
| 关联 | 上游 epic：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**；正文 `## Steps` 列出 #46–#52）；正文含显式 `Part of #44`（P1-2），**已随创建发布** |
| 依赖关系 | 无前置；S3/S6 依赖本步的基线数字 |
| milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)（已存在，state=open） |
| labels | **空**（与 #44 及既有 #28–#33 一致） |
| 状态 | **🟢 已在 GitHub 存在（#46），无需新建** —— `github-operator` 按本文件批准草稿创建（正文逐字一致，除末尾换行）；`## Internal tracking` 未发布。**实现未开始**：Scope / AC-1..AC-6 全部未执行。**S1 基线已测量并记录（2026-10-20）**，**待提交/PR**（未提交、未推送、无 PR；#46 **未完成**，保持 OPEN）。证据：`docs/logs/2026-10-20-issue-46-baseline.md`、`docs/plans/issue-44-migration-checklist.md` |
| 估算 | 0.5 天 |
| 负责人角色 | docs-writer 写计划 → oracle 只读审查 → 主 agent 批准 → 单一 `worker` 执行（只跑命令 + 写记录）→ 3 reviewer（正确性/测试证据/必要性）→ `worker` 修复 → docs-writer 回填 → 主 agent 提交 → `github-operator` 建 PR |
| 事实来源（可核验） | 基线数字：`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md:38-45`（127 tests、0.8980、195 tests/30 files、92.35%、12 e2e）。JDK 现状：计划 §3（本地 Maven 跑 JDK 27）+ U-A1（JDK 21 是否已安装**未确认**）。命令：计划 §6.1 AC-A-1/AC-A-5、契约文档 §12 |
| 未确认 | **已全部闭合（2026-10-20 基线测量）**：① 本地 JDK 21 → **已安装**（`/opt/homebrew/opt/openjdk@21`，`21.0.12.1`，2026-10-19 经用户授权）；② Boot 2.7.18 能否在 JDK 21 上运行 → **能，`mvn verify` BUILD SUCCESS / 127 用例 / 0.8980，与 JDK 27 完全一致**；③ 浏览器可用于 e2e → **已证实**（Playwright `12 passed`）；Docker 仍未在 S1 取证（属 S3/S6）；④ 基线记录最终路径 → **`docs/logs/2026-10-20-issue-46-baseline.md`**，checklist 独立为 **`docs/plans/issue-44-migration-checklist.md`**（未并入同一文件，属文档结构选择，不改变范围） |
| 待裁定 | **已全部闭合**：① **P1-5**（2026-10-19，主 agent 裁定全部接受）删除“安装 JDK 是允许动作”→ 改为“缺 JDK 21 记为 steps 3–4 的 blocking gap；**安装 JDK 需主 agent 批准**，本步不得安装”；② **P2-4** AC-4 补“跨 JDK 对比规则”；③ **P1-2** 正文加 `Part of #44`；④ **是否创建本 issue → 已创建 #46（2026-10-19）**。**U-A1 已闭合（主 agent 确认，2026-10-19）**：JDK 21 **已安装**于 `/opt/homebrew/opt/openjdk@21`（`java version 21.0.12.1`），且 Maven 在该 `JAVA_HOME` 下**编译现有 Boot 2.7 代码成功**（仅编译，未跑 `mvn verify`）→ 本步的“额外在 JDK 21 上复跑”一条已于 **2026-10-20 执行**（`JAVA_HOME=/opt/homebrew/opt/openjdk@21` → BUILD SUCCESS / 127 用例 / 0.8980，与 JDK 27 一致），见 `docs/logs/2026-10-20-issue-46-baseline.md` §3.2。 |

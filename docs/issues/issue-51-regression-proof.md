# Step 6/7 — 回归与生产验证（GitHub issue #51 记录）

> **文档状态：🟢 已在 GitHub 存在（[#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)），无需新建** —— 2026-10-19 由 `github-operator` 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**，仅 GitHub 在末尾追加一个换行；`## Internal tracking` 分区**未发布**）。**本文件不表示该步骤已实施**：Scope / Acceptance criteria 全部未执行，无源码/配置/测试改动、无 PR。
> **远端元数据（主 agent 核验，2026-10-19）**：编号 **#51** · 标题 `[Step 6/7] Prove the migration with full regression and production smoke evidence` · milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) · labels **空** · state **OPEN** · 正文含显式 `Part of #44` · 上游 epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**）。
> `github-operator` 只发布 `## GitHub issue body` 分区（四反引号围栏只是本地容器，发布时去掉围栏行本身）；`## Internal tracking` **不上 GitHub**。
> 拆分总览：`docs/plans/issue-44-migration-steps.md`。
> **本轮修订（oracle P2-1 / P1-2）**：正文列出 **10 条深链的具体路径**（不再只写数量）；正文加显式 `Part of #44`。
> 本文件**不声称**任何验证已运行；正文中的 127 / 195 / 92.35% / 12 次 e2e / 10 条深链均为**既有记录引用**（`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`、`docs/architecture/frontend-migration-contracts.md`），真实数字以 Step 1 的当日基线为准。

## GitHub issue title

```
[Step 6/7] Prove the migration with full regression and production smoke evidence
```

## GitHub issue body

````markdown
## Goal

Produce the **evidence package** that shows the migration changed nothing observable: full quality gates on the final tree, plus an HTTP-level production smoke test of the packaged application.

Every later claim ("migration complete, behaviour unchanged") depends on this step's records. Therefore every item below must be recorded as **executed with its output**, or as **not executed with the reason** — a missing item is a recorded gap, never a pass.

**This issue covers only this step.** It does not change source, tests, `pom.xml`, CI, the `Dockerfile` or documentation of architecture conventions: if a check fails, **stop and report** — the fix belongs to the step that owns that change (3, 4 or 5), not here.

## Background

**Part of #44.**

This is **step 6 of 7** in the migration tracked by #44 (`docs/plans/ai-option-a-boot3-migration.md`, split in `docs/plans/issue-44-migration-steps.md`). Steps 1–5 deliver: a recorded pre-migration baseline, the validated JDBC driver, the Spring Boot 3.5 / `release=17` / Jakarta upgrade, the JDK 21 enforcement and images, and the `@MockitoBean` cleanup. This step proves the *combination*.

Reference values recorded on 2026-10-20 **after** the coverage thresholds were tightened (user approval, separate pre-migration PR):

- backend `mvn verify`: `Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`; JaCoCo line coverage `988/1078 = 0.9165` against the **0.90** gate;
- frontend: 195 tests in 30 files, `lines` 92.35% (846/916) against the **90** threshold;
- the pre-tightening snapshot (recorded 2026-10-15: `127` tests, `968/1078 = 0.8980` against 0.85; `lines >= 88`) is **history only** and is no longer the comparison baseline — the thresholds were already tightened before the migration started, and this step must neither lower nor bypass them;
- end-to-end: 12 Playwright executions (6 cases × 2 viewports);
- production smoke: health 200, `/` contains `id="root"`, the 10 recorded deep links (`/home`, `/movies`, `/movies/:id`, `/cart`, `/checkout`, `/confirmation`, `/login`, `/employee/login`, `/employee/dashboard`, `/playground`) return 200 `text/html`, unauthenticated `/api/boot/cart` → 401 JSON, `/API/boot/cart` → 404 JSON, `/assets/missing.js` → 404, customer login → session/cart 200, employee login → dashboard `status:success`.

## Scope

- [ ] **Backend gate**: `mvn --batch-mode --no-transfer-progress verify` — record BUILD result, `Tests run`/failures/errors/skipped, the JaCoCo covered/missed/total line numbers and the ratio, and the fact that `All coverage checks have been met.` appeared.
- [ ] **Frontend gate** (in `frontend/`): `npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e` — record the test count, the coverage `Lines` value, the Playwright pass count, and confirm `frontend/src` has no diff versus the pre-migration baseline commit.
- [ ] **Production smoke** (`docker compose build app` then `docker compose up -d --no-deps app`, port 8081) — record per item: health endpoint status; `/` containing `id="root"`; **each of these 10 deep links** returning 200 `text/html` — `/home`, `/movies`, `/movies/:id` (a real movie id), `/cart`, `/checkout`, `/confirmation`, `/login`, `/employee/login`, `/employee/dashboard`, `/playground`; unauthenticated `/api/boot/cart` returning 401 with the JSON error shape; `/API/boot/cart` (case variant) returning 404 JSON; `/assets/missing.js` returning 404; a missing SPA route returning `index.html`; customer login → session/cart 200; employee login → dashboard with `status:success`.
- [ ] **Authentication / session semantics**: `changeSessionId()` on customer login; the 401 body shape; employee logout returning 204 while keeping the `user` attribute (test evidence plus smoke evidence where possible).
- [ ] **JDBC behaviour** (only if the driver was upgraded to 9.x in step 2; if step 2 ruled “keep 8.3.0”, record that the clause is **not triggered** rather than quietly skipping it): order/sale dates agree with `sales.sale_date` (no time-zone offset, no `CURDATE()` drift) and no new **fatal** SSL/connection warning.
- [ ] **Toolchain/artifact consistency**: `mvn -version` reports JDK 21; the produced bytecode is class file major version **61**; both `Dockerfile` stages use a JDK 21 image; the `maven-enforcer-plugin` rule is active; the JaCoCo `excludes` list still has the same entries as the pre-migration baseline.
- [ ] **Write a single evidence record** in `docs/` that lists every item with: the exact command, the result summary, and `executed` / `not executed (<reason>)`.

## Non-goals

- **No source, test, `pom.xml`, CI, `Dockerfile` or architecture-document change.** A failing check goes back to the parent agent.
- No new test cases to raise coverage; no threshold lowering or bypassing (the gates are the tightened **0.90 / 90** values approved on 2026-10-20); no new JaCoCo `exclude`; no skipped or quarantined test.
- No new functional/HTTP contract: this step compares against the recorded truth table, it does not define a new one.
- No visual/contrast regression claim (jsdom has no layout — that stays a manual, out-of-automation area).
- No real-browser multi-tab / concurrency testing (existing boundary).
- No documentation close-out (step 7), no GitHub operations (parent agent / GitHub operator).

## Dependency

**Requires steps 3, 4 and 5 to be merged** — this step must test the final artifact, not an intermediate state.

## Acceptance criteria

- [ ] **AC-1** Backend: `mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS, `Tests run` not lower than step 1's baseline, `All coverage checks have been met.`, and the `excludes` list identical to the baseline.
- [ ] **AC-2** Frontend: lint clean, unit tests ≥ the baseline count, `Lines` ≥ **90** (baseline 92.35%; threshold tightened from 88 on 2026-10-20), coverage build successful, Playwright ≥ 12 passing executions; and `git diff <baseline-sha> -- frontend/src` is empty.
- [ ] **AC-3** Production smoke: every HTTP expectation listed in Scope is recorded with the observed status code (and response-body indicator where relevant), on the packaged application served from port 8081.
- [ ] **AC-4** Auth/session: `changeSessionId()` on login, the 401 JSON body shape, and employee logout 204 with the `user` attribute retained are each evidenced (test output and/or smoke transcript).
- [ ] **AC-5** Toolchain/artifact: `mvn -version` = JDK 21, bytecode `major version: 61`, both Docker stages on a JDK 21 image, enforcer rule active.
- [ ] **AC-6** The evidence record exists in `docs/`, lists every Scope item, and marks each one **executed** (with output summary) or **not executed** (with the concrete reason: no Docker, port 8081 busy, no browser, etc.).
- [ ] **AC-7** The pull request's diff contains **only** files under `docs/` (no code, config, test, CI or frontend change).

## Risk notes

- **Environment dependence is the main risk**: the production smoke needs Docker, a free port 8081 and a MySQL container; the Playwright run needs a browser download. If any is unavailable the honest outcome is **not executed**, and the parent agent must decide whether the migration can be declared complete with that gap. Never report an unexecuted check as passing.
- **Unit tests do not see** connection-pool behaviour (HikariCP upgrade), JSON serialization details (Jackson upgrade) or Tomcat 10 container behaviour; the smoke is the only place they are observed, which makes this step's completeness matter more than its effort.
- **"Not lower than baseline" needs the step 1 baseline**: if step 1 was skipped or measured on a different JDK, record the comparison caveat explicitly instead of assuming.
- **Smoke flakiness**: a failing deep link or 401 shape may indicate a real Boot 3 behaviour difference — record the exact request/response and stop; do not "adjust the test".
- **Time-zone checks are date-sensitive**: record the concrete timestamps and the database values used, so the evidence can be re-read later.

## Rollback

This step only adds documentation. If it uncovers a defect, the rollback decision belongs to the owning step (3/4/5) and to the parent agent; nothing in this step changes runtime behaviour.
````

## Internal tracking

> 本分区**不发布到 GitHub**。

| 项目 | 值 |
| --- | --- |
| 步骤 | **S6 / 7**（拆分总览 §3、§4-S6） |
| GitHub issue 编号 | **#51** —— https://github.com/PNZ-z/ai-powered-Fabflix/issues/51 |
| 远端标题 | `[Step 6/7] Prove the migration with full regression and production smoke evidence` |
| 远端 state | **OPEN**（2026-10-19 创建；`Part of #44` 已在正文） |
| 来源 | `docs/plans/ai-option-a-boot3-migration.md` §6.1（AC-A-1..AC-A-11）、§6.2（明确不自动验证的部分）；#44 正文 `Acceptance criteria` AC-1..AC-11、`Scope → Verification`；`docs/architecture/frontend-migration-contracts.md` §7/§11（深链与 SPA 语义、命令与判据）；`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`（参考数字） |
| 关联 | 上游 epic：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**；正文 `## Steps` 列出 #46–#52）；正文含显式 `Part of #44`（P1-2），**已随创建发布**；承担 #44 的 **AC-1/AC-2/AC-5/AC-6/AC-7/AC-9 的复核**与 **AC-3/AC-4/AC-8/AC-10/AC-11 的最终确认**(拆分总览 §5.3) |
| 依赖关系 | **前置 S3 + S4 + S5**（必须在最终形态上取证）；**后继 S7** |
| milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) |
| labels | **空** |
| 状态 | **🟢 已在 GitHub 存在（#51），无需新建** —— `github-operator` 按批准草稿创建（正文逐字一致，除末尾换行）；`## Internal tracking` 未发布。**实现未开始**（S6 的前置 S3/S4/S5 也均未开始） |
| 估算 | 1 天 |
| 负责人角色 | docs-writer → oracle（重点：AC 是否覆盖 #44 全部验收、是否把"未执行"留成模糊表述）→ 主 agent 批准 → 单一 `worker`（只跑命令 + 写证据，不改代码）→ 3 reviewer（证据完整性、AC 覆盖、必要性）→ `worker` 补证 → docs-writer 回填 → 主 agent 提交 → `github-operator` 建 PR |
| 事实来源（可核验） | 参考数字：`docs/logs/2026-10-15-issues-31-32-33-m2-completion.md:38-45`（127 / 0.8980 / 195 / 92.35% / 12）、契约文档 §11（深链/401/404/SPA fallback 真值表）、计划 §6.1（AC-A-6 含 10 条深链与 JDBC 检查）；`Dockerfile` 现为两阶段 JDK 11（**S3** 改为 JDK 21，B-1）；`compose.yaml` 的 `app` 服务端口 8081（契约文档 §11） |
| 未确认 | ① 本机 Docker 与 8081 端口可用性；② MySQL 容器 `mysql:8.4` 与驱动组合；③ 当日基线数字（依赖 S1）；④ e2e 是否需要重新下载浏览器。**已闭合（P2-1）**：10 条深链的具体路径已写入正文（`/home`、`/movies`、`/movies/:id`、`/cart`、`/checkout`、`/confirmation`、`/login`、`/employee/login`、`/employee/dashboard`、`/playground`） |
| 待裁定 | **是否创建本 issue → 已创建 #51（2026-10-19）**。若 S6 有“未执行”项，#44 是否仍可关闭 —— 建议主 agent 在 S7 前明确口径（草稿的 `Risk notes` 与 S7 的 DoD 都要求“不得写成全部通过”）；**此口径仍待主 agent 裁定** |

# Step 4/7 — 用机器可执行的方式强制 JDK 21 并归档镜像证据（enforcer + README）（GitHub issue #49 记录）

> **文档状态：🟢 已在 GitHub 存在（[#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49)），无需新建** —— 2026-10-19 由 `github-operator` 按本文件获批的 `## GitHub issue body` 创建（**正文逐字一致**，仅 GitHub 在末尾追加一个换行；`## Internal tracking` 分区**未发布**）。**本文件不表示该步骤已实施**：Scope / Acceptance criteria 全部未执行，无源码/配置/测试改动、无 PR。
> **远端元数据（主 agent 核验，2026-10-19）**：编号 **#49** · 标题 `[Step 4/7] Enforce JDK 21 in the build and document/verify it` · milestone [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) · labels **空** · state **OPEN** · 正文含显式 `Part of #44` · 上游 epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**）。
> `github-operator` 只发布 `## GitHub issue body` 分区（四反引号围栏只是本地容器，发布时去掉围栏行本身）；`## Internal tracking` **不上 GitHub**。
> 拆分总览：`docs/plans/issue-44-migration-steps.md`。**已裁定 —— C-1 = (a)**：`ci.yml` 的 JDK 升版已在 **Step 3** 完成；**B-1**：`Dockerfile` 两行 `FROM` 也已在 **Step 3** 完成。本步只负责**构建期强制（enforcer）+ README + 对镜像/容器的验证取证**，**不再改镜像**。
> **本轮修订（oracle B-1 / P2-3 / P1-2）**：标题改为 `[Step 4/7] Enforce JDK 21 in the build and document/verify it`；删除“本步才改镜像”的表述；AC-2 明写可用**本机 JDK 27** 取证“错误 JDK → BUILD FAILURE”（删去“可能未执行”的犹豫表述）；正文加 `Part of #44`。
> 本文件**不声称**任何验证已运行。

## GitHub issue title

```
[Step 4/7] Enforce JDK 21 in the build and document/verify it
```

## GitHub issue body

````markdown
## Goal

Make **"the build runs on JDK 21"** a machine-enforced fact rather than a convention, document it, and **record the evidence that the container images already run JDK 21**:

1. a `maven-enforcer-plugin` rule `requireJavaVersion [21,22)` so a wrong JDK fails the build instead of silently producing different results;
2. `README.md` stating the JDK 21 requirement, that `JAVA_HOME` must point at JDK 21, and the build/run commands;
3. **verification only** of the two `Dockerfile` stages (JDK 21 in both — delivered by step 3), including a successful `docker compose build app` and a container health response, recorded as evidence.

**Why a separate step:** after step 3 the build compiles on JDK 21 but nothing prevents a developer or a runner from using another JDK. The recorded problem was that local Maven ran on **JDK 27**, which is outside the supported range for Spring Boot 3.5 — an environment skew that would mask real behaviour differences. This step closes that hole.

**This issue covers only this step.** No application source, no test annotation cleanup (step 5), no CI workflow change (done in step 3), no `Dockerfile` change (done in step 3), no Spring Boot version change.

## Background

**Part of #44.**

This is **step 4 of 7** in the migration tracked by #44 (`docs/plans/ai-option-a-boot3-migration.md`, split in `docs/plans/issue-44-migration-steps.md`). Approved decisions: compile with `release=17` while running the build on **JDK 21** locally, in CI and in the container image (one JDK version everywhere).

Current state to be aware of:

- After **step 3** the `Dockerfile` already uses JDK 21 in both stages (`maven:3.9-eclipse-temurin-21`, `eclipse-temurin:21-jre`) and still runs as unprivileged `uid/gid 1001`. **This step does not change those lines** — it verifies them and records the container build/health evidence. If they are still JDK 11, step 3 is incomplete: stop and report, do not edit the `Dockerfile` here.
- `maven-enforcer-plugin` **currently exists only inside the `frontend` profile** (`pom.xml`, execution `require-frontend-dist`). A rule added only there would apply only when `-Pfrontend` is used — i.e. not to the normal build. The new rule therefore belongs in the **main `<build>`**, and the profile's existing execution must keep working.
- CI already uses Java 21 after step 3, so this rule is consistent with the pipeline rather than fighting it.

## Scope

- [ ] **Build enforcement**: add `maven-enforcer-plugin` (same version already used by the profile) to the **main `<build>`** with an execution bound to the `validate` phase, `fail=true`, and a `requireJavaVersion` rule with the range **`[21,22)`** plus a message that names the requirement and points at `README.md`.
- [ ] Confirm the existing `frontend` profile execution `require-frontend-dist` still triggers as before (same plugin coordinates, merged executions).
- [ ] **Container verification (no change here)**: confirm both `Dockerfile` stages are JDK 21 and keep the `groupadd`/`useradd` `uid/gid 1001` block, `EXPOSE 8081` and `ENTRYPOINT`; then record the evidence that the container path still works: `docker compose build app` succeeds and the started container answers its health endpoint with 200 (or record "not executed" with the reason).
- [ ] **Documentation**: `README.md` states that **JDK 21** is required to build/run, that `JAVA_HOME` must point at a JDK 21 installation, and how to check it (`mvn -version` must report 21); keep the existing build/run/Compose instructions correct.
- [ ] **Evidence**: (a) `mvn --batch-mode --no-transfer-progress verify` passes on JDK 21 and the enforcer rule reports success; (b) a build with a **different JDK** (e.g. `JAVA_HOME` pointing at 17 or 27) → **BUILD FAILURE** from the new rule, with the message captured; (c) `docker compose build app` succeeds (or is recorded as not executed); (d) the jar-shape and missing-`dist` checks still behave as before.

## Non-goals

- **No `.github/workflows/ci.yml` change** — the CI JDK bump and job/step rename belong to step 3.
- **No `Dockerfile` change** — the two `FROM` lines were delivered in step 3 (B-1). If they are still JDK 11, stop and report instead of fixing them here.
- No change to `src/main/**` or `src/test/**` (no `@MockBean` work here — step 5).
- No `compose.yaml` change (the `app` service builds with `build: .`, no image tag to bump).
- No Spring Boot / dependency version change; no Java target change (`release=17` stays).
- No new build profiles, no `toolchains.xml` requirement (the requirement is expressed through the enforcer rule and `JAVA_HOME`, not through a per-machine toolchain file).
- No CI minutes/tooling changes, **no coverage-gate lowering or bypassing** (the gate was already tightened to backend 0.90 / frontend 90 on 2026-10-20, before the migration; this step must leave it as-is).

## Dependency

**Requires step 3** (the project must already build on Spring Boot 3.5 / JDK 21). Adding the enforcer rule before step 3 would fail the JDK 11 CI job.

## Acceptance criteria

- [ ] **AC-1** `pom.xml`'s main `<build>` contains a `maven-enforcer-plugin` execution with `requireJavaVersion [21,22)` bound to `validate`; running `mvn --batch-mode --no-transfer-progress validate` on **JDK 21** succeeds and shows the rule being enforced.
- [ ] **AC-2** Running the same command with a JDK outside `[21,22)` → **BUILD FAILURE** quoting the rule's message. The local Maven default **is JDK 27**, so this case is directly reproducible: point `JAVA_HOME` at that JDK 27 installation (the one `mvn -version` reports today), capture the failure output, then point it back at JDK 21.
- [ ] **AC-3** The `frontend` profile's `require-frontend-dist` still fails the build when `frontend/dist/index.html` is missing, and `mvn -Pfrontend -DskipTests clean package` still succeeds when it is present.
- [ ] **AC-4** **Verified, not changed** (step 3 delivered the images): `Dockerfile` has `maven:3.9-eclipse-temurin-21` and `eclipse-temurin:21-jre`, keeps `uid/gid 1001` and the unprivileged `USER`, `docker compose build app` succeeds, and the started container returns **200** on its health endpoint (or the Docker part is recorded as not executed with the reason).
- [ ] **AC-5** `README.md` documents the JDK 21 requirement, the `JAVA_HOME` requirement, and the `mvn -version` check.
- [ ] **AC-6** `mvn --batch-mode --no-transfer-progress verify` is still green with the same test count and coverage result as after step 3, and the produced bytecode is still **class file major version 61**.

## Risk notes

- **This step intentionally breaks JDK 27/17 local builds.** That is the point of the rule, but it will surprise anyone whose `JAVA_HOME` is not JDK 21; the README change is part of the acceptance criteria for that reason, and the failure message must be actionable.
- **Enforcer merging**: `maven-enforcer-plugin` is declared in the `frontend` profile today. Adding the same plugin to the main build merges the executions; a misconfiguration can make `-Pfrontend` builds fail or skip the dist guard, which AC-3 covers explicitly.
- **Image availability**: the JDK 21 images are already used by step 3, so a pull here normally hits a warm cache; if a pull fails it is an environment issue, not a code issue — record it as such and do not edit the `Dockerfile`.
- **Step 3 dependency**: if step 3 is reverted or incomplete (images still JDK 11), the container verification in AC-4 fails. Report it as an unmet dependency; do not repair step 3's work in this pull request.
- **`[21,22)` deliberately rejects JDK 22+.** If a future JDK needs support, widening the range is a separate decision (it changes the supported matrix), not a drive-by edit.
- **No behaviour risk**: this step changes enforcement, images and docs only; application behaviour must be untouched (proven by the unchanged test count).

## Rollback

Revert the single pull request: the enforcer rule and the README paragraph are independent, so either can be reverted alone. The `Dockerfile` `FROM` lines belong to step 3 and are **not** part of this revert. Reverting must not leave the project unable to build on JDK 21 (`release=17` from step 3 is unaffected either way).
````

## Internal tracking

> 本分区**不发布到 GitHub**。

| 项目 | 值 |
| --- | --- |
| 步骤 | **S4 / 7**（拆分总览 §3、§4-S4、§6-C-1） |
| GitHub issue 编号 | **#49** —— https://github.com/PNZ-z/ai-powered-Fabflix/issues/49 |
| 远端标题 | `[Step 4/7] Enforce JDK 21 in the build and document/verify it` |
| 远端 state | **OPEN**（2026-10-19 创建；`Part of #44` 已在正文） |
| 来源 | `docs/plans/ai-option-a-boot3-migration.md` §3（blocking 前置 P1-3）、§4.4、§4.5-0、§6.1 AC-A-11；#44 正文 `Scope → Toolchain`、`Scope → Docker`、`Scope → Docs (README)`、`AC-11`；oracle B-3/Y-4 裁定（`requireJavaVersion [21,22)` + README + `JAVA_HOME`） |
| 关联 | 上游 epic：[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（**已转为 epic，保持 OPEN**；正文 `## Steps` 列出 #46–#52）；正文含显式 `Part of #44`（P1-2），**已随创建发布**；承担 #44 的 **AC-11 的 enforcer/README 部分 + 镜像取证**、与 S3 共同承担 AC-4 的回归检查 |
| 依赖关系 | **前置 S3**（必须已完成 CI 的 JDK 21 切换**与 `Dockerfile` 两阶段 JDK 21**）；**后继 S6** |
| milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) |
| labels | **空** |
| 状态 | **🟢 已在 GitHub 存在（#49），无需新建** —— `github-operator` 按批准草稿创建（正文逐字一致，除末尾换行）；`## Internal tracking` 未发布。**实现未开始**（前置 S3 也未开始） |
| 估算 | 1 天 |
| 负责人角色 | docs-writer → oracle（重点审 enforcer 与 profile 执行的合并、以及"错误 JDK 失败"的可验证性）→ 主 agent 批准 → 单一 `worker` → 3 reviewer → `worker` 修复 → docs-writer 回填 → 主 agent 提交 → `github-operator` 建 PR |
| 事实来源（可核验，本轮只读） | `pom.xml:178-186`（`<profile><id>frontend</id>`）+ `:181` 起的 `maven-enforcer-plugin:3.5.0`（**仅存在于 profile 内**）及其 `require-frontend-dist` 执行；`pom.xml:87` 起的主 `<build>`（当前无 enforcer）；`Dockerfile`（**修订后**：`Dockerfile` 的两个 `FROM` 由 **S3** 改为 JDK 21，本步只取证）；`ci.yml`（JDK 由 S3 改） |
| 未确认 | ① `requireJavaVersion` 与该 profile 执行合并后是否完全无副作用（待实测）；② README 现有章节的最小改动位置（实施时定）；③ 本地能否 `docker compose build app`（Docker/网络）。**已闭合**：本机 Maven 默认跑 **JDK 27**（在 `[21,22)` 之外），因此 AC-2 的“错误 JDK → BUILD FAILURE”**可直接取证**（P2-3）；另（主 agent 确认，2026-10-19）**JDK 21 已安装**于 `/opt/homebrew/opt/openjdk@21`（`java version 21.0.12.1`），所以 AC-1/AC-3 的“JDK 21 下通过”也可本机取证 |
| 待裁定 | **已全部闭合（2026-10-19）**：**B-1**（镜像并入 S3）、**C-1 = (a)**（CI 在 S3）、**P2-3**（AC-2 用 JDK 27 取证）已落入正文。**是否创建本 issue → 已创建 #49（2026-10-19）**。 |

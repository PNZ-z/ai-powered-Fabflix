# B1 — Spring AI 接入本地 Ollama（依赖 + 配置 + 可达性健康端点）（issue #66）

> **文档状态：🟢 issue #66 已创建（2026-10-24），本文件为规范来源。** 下面 `## GitHub issue body` 分区是**已逐字发布**到 GitHub 的正文；`## Internal tracking` 分区**未发布**（只存在于本仓库）。
> **远端已核实（2026-10-24，只读操作）**：`gh issue view 66` → **OPEN**、milestone **#3 `Phase 3 — AI-powered platform`**、**labels 空**、title 与下方 `## GitHub issue title` 一致；并以脚本比对了**已发布正文与本文档 `## GitHub issue body` 分区逐字相同**（14271 字符，零差异）。文档更名为 `docs/issues/issue-66-b1-spring-ai-integration.md`，编号不再未知。
> **2026-10-24 修订**：基线更新为 `main` @ `df24b67`（PR #65 已合并，已修复前端 coverage 时序）；按 oracle 条件 C1–C5 与主 agent 对 P-B1-1..10 的裁定改写。
> **2026-10-24 实现回填**：B1 **已实现并在本地验证通过**（`mvn verify` 152 tests / 0.9208 / 新增产码 N=59,U=0；M1–M4 已执行），三路只读审查通过、F1–F6 修复落地。
> **2026-10-24 PR/CI 回填**：commit **`e446aab`** 已推送，**PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67)** 已创建（`Closes #66`），**CI 双 job SUCCESS**（`Build and test (Java 21)`、`Frontend (lint, test, coverage, build, e2e)`），`mergeStateStatus=CLEAN`；**合并待执行（未合并、无 merge commit）**，GitHub 远端 #66 仍 **OPEN**、**checkbox 未勾选**（本文件**未**修改远端，也未修改下文 `## GitHub issue body` 发布分区——它仍与远端正文逐字一致）。
> 本文件**不声称** B1 已交付：**未合并、无 merge commit**；`AC-B-12` **仍未闭环**（B1 不闭环它）。详见 Internal tracking 的「实现与验证状态」「PR / CI / 合并」行与文末「实现结果回填」小节。

## GitHub issue title

```
[B1] Integrate Spring AI with the local Ollama endpoint
```

## GitHub issue body

````markdown
## Goal

Wire Spring AI into the application on the already-migrated Boot 3.5.15 / JDK 21 baseline: add the `spring-ai-bom` and the Ollama starter, expose the Ollama connection settings through environment variables (no hard-coded addresses), and add a single login-protected health endpoint that tells an operator whether the configured Ollama endpoint is enabled and reachable.

**This issue delivers integration and observability only — no AI feature.** There is no chat call, no embedding call, no RAG pipeline, no vector store, no natural-language search, no recommendation, no prompt, no UI work. The point is to make "Spring AI is connected, and we can tell whether the local model endpoint answers" a verifiable fact before any feature is built on top of it.

## Background

The `Phase 3 — AI-powered platform` milestone started with the Boot 3 / Java 17 / Jakarta migration (epic #44, closed) because Spring AI 1.x requires Boot 3.x and Java 17+. The local-model spike (B0, evidence in `docs/logs/2026-10-24-b0-local-llm-spike.md`) then established the facts this issue builds on:

- Ollama runs on a **LAN machine, not on this host**, at an address supplied out of band; it is reachable and has `qwen2.5:7b` (chat, 100 % GPU) and `bge-m3` (embedding, 1024 dimensions) installed.
- Chat hot throughput was **56–70 tok/s** (derived from completion tokens ÷ end-to-end time) and the model made a factual error on a list-style question — so retrieval and source citation will be needed later, not model memory.
- Embedding cold start was **7.64 s** versus **0.071–0.129 s** hot: **a health check must not trigger a model load**, or it becomes a 7-second request.
- The endpoint has **no authentication**, so the address must never be echoed back in an HTTP response.
- On macOS, a client without the "Local Network" permission fails with `No route to host` while `curl` on the same host works — "cannot reach" must therefore be a normal, representable state, not a 500 or a startup failure.

The B0 log also lists what it deliberately did **not** cover (vector-store coordinates, 21,087-card embedding cost, document chunk counts, a second embedding candidate, cross-language retrieval, NL→JSON compliance). None of that is in scope here; this issue covers the integration seam only.

Spring AI **1.1.8** is the chosen version: the BOM is published on Maven Central and it manages the Ollama starter, whose own dependency on `spring-boot-starter` is 3.5.15 — the same Boot version this project already pins.

## Scope

- [ ] `pom.xml`: import `org.springframework.ai:spring-ai-bom:1.1.8` with `<type>pom</type>` / `<scope>import</scope>` in `<dependencyManagement>` (after `spring-boot-dependencies`), and add `spring-ai-starter-model-ollama` **without an explicit version** (managed by the BOM).
- [ ] Configuration in `src/main/resources/application.properties`, every value environment-driven with a working default and no real host in the repository:
      - Ollama base URL from `FABFLIX_AI_OLLAMA_BASE_URL`, default `http://localhost:11434`, bound to `spring.ai.ollama.base-url`;
      - chat model from `FABFLIX_AI_CHAT_MODEL`, default `qwen2.5:7b`;
      - embedding model from `FABFLIX_AI_EMBEDDING_MODEL`, default `bge-m3`;
      - feature flag `fabflix.ai.enabled` from `FABFLIX_AI_ENABLED`, **default `false`**.
- [ ] `.env.example`: add the four variables above as placeholders (local defaults only — no real LAN address, no token, no secret).
- [ ] New endpoint `GET /api/boot/ai/health`, protected by the existing `CustomerAuthenticationFilter` (that is, login required, **with no change to the filter itself**), returning HTTP 200 in every case with:
      - `enabled` (boolean), `chatModel` (string), `embeddingModel` (string), `reachable` (boolean), `status` (`disabled` | `ok` | `unreachable`);
      - when `enabled` is `false`: **no outbound network call at all**, `reachable=false`, `status=disabled`;
      - when `enabled` is `true`: exactly one lightweight reachability probe against the Ollama HTTP endpoint — a single `GET /api/version` — with a **2-second whole-request budget** (one overall deadline, not 2s connect + 2s request), and **no** chat/embedding/generate request (those load models);
      - probe failure (connection refused, timeout, non-2xx, malformed base URL) → `reachable=false`, `status=unreachable`, **still HTTP 200** — never a 5xx and never a leaked stack trace;
      - the response must not echo the base URL, host or port.
- [ ] Tests for every new production class: an anonymous request returns 401; `enabled=false` returns `disabled` **and provably does not probe**; `enabled=true` with a reachable stub returns `ok`; `enabled=true` with an unreachable stub returns `unreachable`; probe unit tests cover 2xx / non-2xx / timeout / connection refused / malformed URL against a loopback stub, with no real network or Ollama dependency.
- [ ] Document the endpoint contract, the configuration keys and the residual risks in `docs/plans/` and in the workflow log.

## Non-goals

- **No AI capability**: no chat completion, no embedding call at runtime, no RAG, no vector store, no document ingestion, no natural-language search, no recommendation, no prompts, no frontend page.
- **No `spring-ai-vector-store` / `SimpleVectorStore` dependency** and no corpus or index work (that belongs to the follow-up feature milestones).
- **No change to the authentication boundary**: `CustomerAuthenticationFilter` is untouched; anonymous access to AI endpoints is not approved.
- **No change to CI, `Dockerfile` or `compose.yaml`**: the build must stay independent of the LAN machine and of Ollama.
- **No coverage-threshold change** and **no new JaCoCo `excludes`** (backend line gate stays 0.90).
- **No cloud/external-API fallback provider** in this issue.
- **`README.md` is not required**: documenting the four variables in `README.md` is optional follow-up, not part of the acceptance criteria; the authoritative placeholder list is `.env.example`.
- **No editing of closed issues, historical logs or the roadmap's execution status.**

**This issue does not close `AC-B-12`.** The unfinished B0 evaluation items (a second multilingual embedding candidate, a Chinese→English retrieval case, the 21,087-card embedding cost, the `docs/` chunk count, the p95 latency target and the NL→JSON compliance rate) stay **out of scope here**; closing `AC-B-12` is a **prerequisite for the follow-up feature milestone**, not part of this integration issue. Its acceptance criteria must not be read as evidence that any of those items is done.

## Dependency

- Requires the completed Boot 3 migration (Boot 3.5.15, `release=17`, JDK 21, Jakarta) — epic #44, closed.
- Requires the B0 spike facts above; the model and endpoint are already provisioned by the user.
- Blocks the feature milestones that will actually call the models (catalogue semantic search, document RAG).

## Acceptance criteria

- [ ] **AC-1** `mvn --batch-mode --no-transfer-progress verify` passes on JDK 21 with the new dependencies; `spring-ai-bom` is imported (not a plain dependency) and `spring-ai-starter-model-ollama` declares **no version of its own**; the resolved tree contains `spring-ai-ollama:1.1.8` and `spring-boot-starter:3.5.15`, with no second Spring Boot version.
- [ ] **AC-2** No real LAN address or secret exists in `pom.xml`, `src/` or `.env.example` (a `grep` for the LAN address returns nothing); all four settings are `${ENV:default}` placeholders; `.env.example` documents them.
- [ ] **AC-3** With no AI environment variables set, `GET /api/boot/ai/health` (authenticated) returns `enabled=false`, `status=disabled`, `reachable=false`, and **no outbound request is made** (asserted in a test, not by inspection).
- [ ] **AC-4** With `enabled=true` and a reachable endpoint, the response is `status=ok`, `reachable=true`; the probe makes **exactly one** request and that request is **`GET /api/version`** — it never hits `/api/tags`, `/api/ps`, `/api/chat`, `/api/generate`, `/api/embed` or any `/v1/**` path (asserted by recording every requested path in a test).
- [ ] **AC-5** With `enabled=true` and an unreachable endpoint (refused, timeout, non-2xx, malformed URL), the response is HTTP **200** with `reachable=false`, `status=unreachable`; no exception escapes and no 5xx is returned; the probe respects a **single whole-request deadline** (a slow handler must not cost two stacked timeouts) rather than hanging.
- [ ] **AC-6** An anonymous request to `GET /api/boot/ai/health` returns `401` with `{"errorMessage":"Authentication required."}`, and `CustomerAuthenticationFilter` is unchanged in the diff.
- [ ] **AC-7** The response body contains exactly the documented fields, uses the documented `status` values, and contains no base URL, host, port or token.
- [ ] **AC-8** Coverage does not regress: `mvn verify` is green with JaCoCo `BUNDLE / LINE / COVEREDRATIO ≥ 0.90`, `Tests run` is not lower than before, and `excludes` is unchanged. With `N` new production lines of which `U` are uncovered, the gate requires `U ≤ 0.1N + 17.8` (from the 988/1078 baseline); the practical target is close to 100 % line coverage of the new code, and the exact numbers are recomputed from `target/site/jacoco/jacoco.csv` during implementation.
- [ ] **AC-9** CI is unchanged and independent of the LAN machine: `ci.yml` has no diff and the whole suite passes on a host with no Ollama running (the only network used by tests is a loopback stub).
- [ ] **AC-10** The application starts normally with **no Ollama available** and no AI environment variables, **and performs no outbound AI connection during startup**; with `FABFLIX_AI_ENABLED=true` pointing at nothing, the health endpoint reports `unreachable` and the application keeps running.
- [ ] **AC-11** Recording is honest: the workflow log separates what was verified from what was not (in particular the optional manual check against the real LAN endpoint, which needs the macOS "Local Network" permission for the JVM), and states explicitly that no AI feature exists yet. **This is a documentation/process item and a verification note — it is not a merge gate.**

## Risk notes

- **Startup behaviour of the starter is not self-evident.** The Ollama starter's initialisation defaults are `pull-model-strategy=NEVER`, a 5-minute timeout and no retries; the initialisation hook is a no-op under `NEVER` and the Ollama API client's constructor does not issue a request, so the current expectation is "no outbound traffic at startup". That is code-level evidence, not an end-to-end measurement, so AC-10 still has to prove it on a real start. If it is ever falsified, do **not** simply set `spring.ai.model.chat=none`: `ChatClientAutoConfiguration` requires a `ChatModel` bean and the application would then fail to start for a different reason. The fallback is one of, in priority order: (1) `spring.ai.model.chat=none` **plus** excluding `org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration` (and `ChatMemoryAutoConfiguration` if needed); (2) introducing the starter only under an `ai` profile; (3) removing the starter entirely — the health endpoint keeps working, because the probe does not depend on Spring AI.
- **A health check must not become a slow request.** Calling chat or embedding to "prove" reachability would load a model (measured cold start: 7.64 s) and can evict the other model from VRAM. The probe must stay on `GET /api/version` with a **single whole-request deadline** and no retries — a per-phase timeout would silently double the worst case.
- **Silent over-claiming is the most likely failure.** A green build does not mean the LAN endpoint works from the JVM: on macOS, a JVM without the "Local Network" permission looks exactly like "Ollama is down". That check is manual, may be unavailable during implementation, and must be recorded as verified or not verified — never implied. The same applies to scope: this issue does not close `AC-B-12`.
- **The endpoint is unauthenticated on the LAN.** Any response field that echoes the address widens the exposure; the address therefore stays in configuration and in DEBUG logs only.
- **`status` is a health-endpoint-family field, not a business result.** `/api/boot/health` already answers `{"status":"UP"}`, so a `disabled` / `ok` / `unreachable` value on this endpoint is the same kind of field rather than an exception to the business `success` / `fail` convention. `unreachable` is not a business failure, hence HTTP 200 — and the field must not be renamed or reinterpreted.
- **`reachable` is not `ready`.** `reachable=true` / `status=ok` only proves that `GET /api/version` answered 2xx. It does **not** prove that `qwen2.5:7b` or `bge-m3` is installed, loaded, resident in VRAM, or that the embedding dimension is 1024. Model-readiness semantics belong to the later observability milestone, so this endpoint must never be presented — in code comments, logs or docs — as "the model is ready to serve".
- **Coverage headroom is finite, not a fixed number of lines.** The backend gate is 0.90 against a 988/1078 baseline: with `N` new production lines of which `U` are uncovered, the gate requires `U ≤ 0.1N + 17.8`. Rather than planning around a specific number, every new class needs tests and every probe failure branch and service branch must be exercised, aiming at close to 100 % line coverage of the new code.
- **Reported vs effective model name.** The health endpoint reports the configured model names. Reading them back from the same property key that Spring AI binds (`…chat.options.model`) is sufficient here, because that key and the shorter `.model` form resolve to the same value in Spring AI 1.1.8; the remaining proof that Spring AI really uses the value arrives with the first real model call in the follow-up milestone.

## Rollback

Single-branch, single-pull-request change with no data migration and no external state: reverting the pull request removes the dependency, the endpoint and the configuration. Independently, leaving `FABFLIX_AI_ENABLED=false` keeps the application on the "AI off" path, and the probe deliberately does not depend on Spring AI APIs, so removing the starter does not require touching the health endpoint's implementation.
````

## Internal tracking

> 本分区**不发布到 GitHub**。

| 项目 | 值 |
| --- | --- |
| 状态 | **🟢 已实现 / PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67) 已创建、CI 双绿 / 合并待执行**（2026-10-24）。流程：oracle 只读审查 ✅（有条件，C1–C5 已落实）→ 主 agent 批准 ✅ → 用户授权 ✅ → `github-operator` 创建 #66 ✅ → `worker` 实现 ✅ → 三路只读审查（无 P0/P1）✅ → 接受的意见修复（F1/F2/F3/F6）✅ → `docs-writer` 文档回填 ✅ → 提交 `e446aab` + 推送 + PR #67 ✅ → CI 双 job SUCCESS ✅ → **合并待执行**。**未合并、无 merge commit；#66 仍 OPEN（合并后由 `Closes #66` 自动关闭），远端 checkbox 未勾选** |
| **PR / CI / 合并（2026-10-24 回填）** | **PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67)** `feat: integrate Spring AI with the local Ollama endpoint`：base `main`（`df24b67`）← head `codex/b1-spring-ai-integration`（commit **`e446aab`**，已推送）；正文 `Closes #66`。**CI 双 job SUCCESS**：`Build and test (Java 21)`、`Frontend (lint, test, coverage, build, e2e)`；`mergeStateStatus=CLEAN`、`mergeable=MERGEABLE`。**合并待执行**（用户已授权，主 agent 执行）；本行**不得**改写成“已合并/已关闭”（上述事实由主 agent 转达，`docs-writer` 未做 GitHub 写操作、未重跑 CI） |
| GitHub issue 编号 / URL | **[#66](https://github.com/PNZ-z/ai-powered-Fabflix/issues/66)**（OPEN；GitHub API `createdAt` = `2026-10-25T02:01:50Z`，主 agent 告知的本地创建日期为 **2026-10-24**）。本文件已更名为 `docs/issues/issue-66-b1-spring-ai-integration.md` |
| 建议标题 | `[B1] Integrate Spring AI with the local Ollama endpoint`（与上方 `## GitHub issue title` 一致） |
| milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)（保持 open） |
| labels | **空**（`gh issue view 66` 核实创建时无 labels；主 agent 如需 `docs`/`backend` 等需另行批准，docs-writer 不做 GitHub 写操作） |
| 来源 | 用户确认开始 B1（主 agent 转达）；`docs/plans/ai-option-b-spring-ai-rag.md` §2.1 第 1/7 项 + §4.1（P2-11 配置真值来源）+ §4.4（`/api/boot/ai/*` 端点需登录，D-AI 端点登录 = (a)）；`docs/plans/ai-roadmap-overview.md` §5.1/§6（Option A → Option B）；`docs/logs/2026-10-24-b0-local-llm-spike.md` §3.2 第 **1（仅坐标核实）/2/3/4** 项 |
| **B0 §3.2 第 5–8 项 = Deferred（不属于本 issue）** | **#5**（第二 embedding 候选 + 中→英，闭环 `AC-B-12`、keep-alive 定性）；**#6**（21,087 卡片 embedding 耗时/内存、`docs/` 切分块数）；**#7**（p95 目标值）；**#8**（NL→JSON 合规率）。**`AC-B-12` 仍未满足**，它是 **B-①/B-② 的准入前置**，需在 B-① 之前**单独另建 issue** 完成；**本工作流不创建 B1.5 issue**。正文已在 Non-goals 之后用英文写明这一点 |
| 关联计划 | **`docs/plans/b1-spring-ai-integration.md`**（§2.3 Deferred from B0、§3.5.1 回退三选一、§6.4 覆盖率算术、§9.1 已裁定的 P-B1-1..10、§9.2 U 项状态、§10.4 oracle 核实事实、**§11 实现结果**、§12 状态表） |
| **实现与验证状态（2026-10-24）** | **`mvn --batch-mode --no-transfer-progress verify`（JDK 21）= Tests run 152 / Failures 0 / Errors 0**（基线 134 → +18）；**JaCoCo 90 missed / 1047 covered / 1137 total = 0.9208（闸门 0.90）**；新增产码 **N=59 / U=0**；`excludes` 仍 **8**；新增 3 个测试类共 **18 用例**（controller 5 / service 7 / probe 6），连续 5 次稳定。该结果由 `worker` 报告并由**主 agent 独立复跑核实**；`docs-writer` 未自行重跑 `mvn` |
| **手工验证（M1–M4）** | **M1**：无 AI env 启动 **0.992 s**，`/api/boot/health` = UP，进程对 11434 的 TCP 连接数 = **0**（`lsof -nP -a -p <pid> -iTCP`，socket 级证据）；**M2**：`FABFLIX_AI_ENABLED=`（空值）启动 **0.941 s**、登录后 `disabled` / **4 ms** / 零探测；**M3**：`enabled=true` + `localhost` 无 Ollama → **200 `unreachable` / 16.9 ms**；**M4**：`enabled=true` + 局域网端点 → **200 `ok` / 34 ms**（probe 29 ms），JVM 具备 macOS「本地网络」权限，采样到 `HttpClient` keep-alive 的 `ESTABLISHED`（**非泄漏**） |
| **审查修复（F1–F6，无 P0/P1）** | **F1** 请求加 `.timeout(timeout)`（与 `orTimeout` **同值**，超时真正终止 exchange）；**F2**/P2-1 删掉 4 处 `@Value` 重复默认值（**单一配置真值**，缺键 fail-fast）；**F3** `FABFLIX_AI_ENABLED=` **空值曾导致启动失败**（已实测复现）→ 改 `String` + **仅 `true`（忽略大小写）启用**、空/其他 = 关闭；**F6** Javadoc 写明 disabled 时 `reachable=false` = **未探测**；另：测试 P3 加下界（`100 ≤ elapsed < 1000`）与 600ms stub sleep、P1 断言 `GET`；**P6（中断）保留**；**§6.1 超时注入点接受**（方法参数，2s 常量在 service） |
| **仍未验证（不得读作已完成）** | ① 真实端点抓包证明“只请求 `/api/version`”（仅 loopback stub 证明）；② keep-alive 连接生命周期；③ **U-B1-5** starter 启动/内存开销（未与基线对比）；④ **U-B1-6** compose 透传；⑤ **模型就绪**（`reachable ≠ ready`）；⑥ chat/embedding 推理（B1 不做）；⑦ **`AC-B-12` 仍未闭环**。**⑧ PR CI 已不再属未验证项**：PR #67 双 job SUCCESS；但 CI 绿**不**外推第 ①–⑦ 项 |
| **本文件发布分区保护** | 下方 `## GitHub issue body` **未改动**（保持与远端 #66 正文逐字一致）；**未勾选**远端任何 checkbox（任务未交付）；本文只改头部说明、Internal tracking 与本条以下的新增小节 |
| **不写 `Part of #44`** | #44 已 **CLOSED（reason=completed，2026-10-21）**，且其范围只覆盖 Option A 迁移；B1 属 milestone **`Phase 3 — AI-powered platform`** 的后续工作。正文以 milestone 名称 + B0 日志仓库路径表达归属，**不建立与 #44 的父子关系** |
| 事实来源（可核验） | **Spring AI 1.1.8 工件**：`docs-writer` 本轮 `curl` 复核 `spring-ai-bom` / `spring-ai-starter-model-ollama` / `spring-ai-vector-store` 的 `1.1.8` 路径均 **HTTP 200**（repo1.maven.org）；BOM 含 `spring-ai-starter-model-ollama`（`${project.version}`），**不含** `org.springframework.boot` 工件；starter 依赖 `spring-boot-starter:3.5.15`，与 `pom.xml` 的 `spring-boot.version=3.5.15` 一致。配置前缀/模型键/`pull-model-strategy=NEVER` 默认值由 **1.1.8 字节码常量**读出（`javap`，计划 §10.2）。**oracle 追加核实（主 agent 转达，计划 §10.4）**：`spring-ai-ollama:1.1.8` 传递 `spring-webflux:6.2.19`/`jackson-databind:2.21.4` 与 Boot 3.5.15 管理版本一致；`OllamaChatProperties.getModel()` 委托 `options.getModel()`；`OllamaApi` 无 `version()`；`ChatClientAutoConfiguration` 需要 `ChatModel`；`initializeModel` 在 `NEVER` 下 no-op；`/api/boot/ai/health` 受 `CustomerAuthenticationFilter` 保护。**B0 数字**（56–70 tok/s、1024 维、7.64 s 冷启动、`size_vram=size`）来自主 agent 实测并经 B0 日志记录，本文只引用不重测。**窗口环境**：`main` @ **`df24b67`**（PR #65 已合并；主 agent 告知其修复前端 coverage 时序），`git status --short` = `?? .idea/` + 两份未跟踪的 B1 草稿 |
| 覆盖率口径（**已修正**） | 0.90 闸门来自 `docs/architecture/frontend-migration-contracts.md` §7 + `docs/logs/2026-10-20-coverage-threshold-90.md`（988 covered / 1078 total）。**旧稿 `N ≤ 19` 是错误推导**（把“新增全部行”当成“新增未覆盖行”），已删除；正式口径：设新增产码 `N` 行、其中未覆盖 `U` 行，须 **`U ≤ 0.1N + 17.8`**（计划 §6.4、正文 AC-8）。**本轮未运行 `mvn`**，实施时必须用 `target/site/jacoco/jacoco.csv` 重算 → **实施后已重算：1137 total / 1047 covered / 90 missed = 0.9208，N=59/U=0**（见「实现与验证状态」行） |
| **oracle 条件落实（C1–C5）** | **C1 已完成**（main 绿，基线 `df24b67`）；**C2 已落实**（B1 只做接入、不闭环 `AC-B-12`，正文与计划 §2.3 双写；不建 B1.5 issue）；**C3 已落实**（删除 “≤19 行” 推导，改 `U ≤ 0.1N + 17.8`，见正文 AC-8 与 risk note）；**C4 已落实**（回退三选一 + 优先级，见正文 risk note；并纠正“单写 `spring.ai.model.chat=none` 会启动失败”）；**C5 已落实**（`status` 改为 health 端点家族字段措辞、`reachable ≠ ready` 写进正文 risk notes） |
| **主 agent 已裁定的 P 项** | **P-B1-1 A**（JDK `HttpClient`）、**P-B1-2 A**（捕获全部 `RuntimeException` → `unreachable`）、**P-B1-3 A**（保留 `status` 三态、按 C5 改写措辞、不改 `state`）、**P-B1-4 A**（2s 常量 + **方法参数注入**，不加运维键）、**P-B1-5 B**（**不加** `ApplicationContextRunner`，删掉 `AiPropertiesBindingTest`）、**P-B1-6 A**（`GET /api/version`，不暴露模型清单）、**P-B1-7 A**（保持自动配置开启，回退按 C4）、**P-B1-8 B**（不更新 README）、**P-B1-9 B**（不回填 `ai-option-b` §4.1）、**P-B1-10 A**（B1 用 `/api/boot/ai/health`，`/ai/status` 留 M4.2） |
| 发布前置（**已完成**） | ① oracle 只读审查 **✅**（有条件，C1–C5 已落实）；② 主 agent 明确批准/标记可发布 **✅**（2026-10-24）；③ 用户授权创建 **✅**；④ `github-operator` 按草稿创建 **✅**（→ #66）。发布结果：正文与草稿逐字一致，`Internal tracking` 未上 GitHub |
| 待确认 | labels 是否补充（当前为空，创建时未添加）；是否把 B1 拆成子任务（**实际产出 14 个文件：产码 5 + 测试 3 + 配置 3 + 文档 3，不拆 issue**）；B0 #5–#8 的后续 issue 何时立项（**记录为 B-① 之前单独做，不在本工作流创建**）；**PR 已创建（#67，CI 双绿）**，合并时点由主 agent 执行（用户已授权）；合并后需回填 `docs/plans/ai-roadmap-overview.md` 并确认 #66 自动关闭与 checkbox 状态 |

### 实现结果回填（2026-10-24）

> 本小节位于 `Internal tracking` 之内（**不发布到 GitHub**）。上方 `## GitHub issue body` 分区**逐字未改**，因此与远端 #66 仍一致；**远端 checkbox 未勾选**（任务未合并）。

**已完成（本地验证，可复跑）**

| 项 | 结果 |
| --- | --- |
| 构建/测试 | `mvn --batch-mode --no-transfer-progress verify`（JDK 21）→ **Tests run: 152, Failures: 0, Errors: 0**（基线 134） |
| 覆盖率 | **90 missed / 1047 covered / 1137 total = 0.9208** ≥ 0.90；新增产码 **N=59 / U=0**；`excludes` 仍 **8** |
| 新增测试 | 3 个类共 **18 用例**（controller 5 / service 7 / probe 6），连续 5 次稳定 |
| 变更面 | `pom.xml`（BOM 1.1.8 + starter 无版本）、`application.properties`（4 项 `${ENV:default}`）、`.env.example`（4 占位）、`src/main/java/com/fabflix/boot/ai/`（5 类）、`src/test/java/com/fabflix/boot/ai/`（3 类） |
| 认证边界 | `CustomerAuthenticationFilter.java` **零 diff**；匿名 → 401 |
| 手工验证 | M1（0.992 s、socket 级零出站）、M2（空值启用位→ `disabled`/4 ms）、M3（`unreachable`/16.9 ms）、M4（`ok`/34 ms）——详见计划 §11.3 |

**仍未验证 / 未发生**（禁止写成已完成）：真实端点抓包、keep-alive 生命周期、**U-B1-5** starter 启动/内存开销、**U-B1-6** compose 透传、模型就绪（`reachable ≠ ready`）、chat/embedding 推理、**`AC-B-12`**。**PR CI 已不在此列**（PR #67 双 job SUCCESS）；**合并仍未发生**。

**流程状态（2026-10-24 PR/CI 回填）**：分支 `codex/b1-spring-ai-integration`（基线 `df24b67`）@ **`e446aab`**，已推送；**PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67)** 已创建、**CI 双 job SUCCESS**、`mergeStateStatus=CLEAN`；**未合并（无 merge commit）**，**#66 仍 OPEN**。下一步：主 agent 合并 PR #67（用户已授权）→ 合并后 #66 自动关闭；回填 `docs/plans/ai-roadmap-overview.md` 执行状态与 checkbox 记录。

**范围偏离（已获批/已记录）**：① probe **接口 + 实现拆为 2 个文件**（产码由 4 变 5，总数 13 → 14）；② 测试类名 `OllamaReachabilityProbeTest` → **`HttpOllamaReachabilityProbeTest`**；③ `.enabled` 绑定类由计划隐含的 boolean 改为 **String（仅 `true` 启用）**（F3，已在 `String` 层面保持外部契约不变）；④ 测试 P3 改为 `600ms sleep + 100≤elapsed<1000` 区间断言。均为**收紧/澄清**，未扩大 AC、未改响应契约、未改认证边界。

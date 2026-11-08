# B1 — Spring AI 接入本地 Ollama（依赖 + 配置 + 可达性健康端点）

> **文档状态：🟢 已批准 / 已实现 / 已合并（PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67)，merge commit `aea0477`，`mergedAt` 2026-11-08T23:16:16Z）** —— 2026-10-24 批准与实现，2026-11-08 合并，均由主 agent 执行。批准部分：`oracle` 只读审查给出条件 **C1–C5** 与 **P-B1-1..10** 建议，**主 agent 已全部接受并裁定**（§9.1）。实现部分：唯一 `worker` 已完成实现，三路只读审查（正确性/边界、测试覆盖、必要性/复杂度）**均通过、无 P0/P1**，接受的修复 **F1–F6** 已落地，`mvn --batch-mode --no-transfer-progress verify`（JDK 21）**Tests run: 152, Failures: 0, Errors: 0**，JaCoCo `BUNDLE/LINE = 0.9208`（见 §11）。交付部分：commit **`e446aab`** 经 **PR #67 已 MERGED**（merge commit **`aea0477`**，2026-11-08T23:16:16Z，head 分支已删除），`main` @ `aea0477` 的 CI **双 job SUCCESS**（`Build and test (Java 21)`、`Frontend (lint, test, coverage, build, e2e)`），issue **#66 已 CLOSED**（`closedAt` 2026-11-08T23:16:17Z，由 PR 正文 `Closes #66` 自动关闭）（§12）。
> **本文件不声称远端 checkbox 已勾选**：PR #67 已合并、`main` CI SUCCESS、#66 已 CLOSED 均为**可核事实**；但 GitHub 远端 issue #66 的 acceptance checkbox **是否已勾选未核实**（本步不做任何 GitHub 写操作，也不替 `github-operator` 判断）。`AC-B-12` 仍不闭环（§2.3），**B1 任何验收条件都不是它的完成证据**。
> **本地验证 vs 未验证的分界**：§11 只记录**已实际运行**的检查（`mvn verify`、5 次稳定性重复、M1–M4 手工启动）；**真实端点抓包证明"只请求 `/api/version`"、keep-alive 生命周期、starter 启动/内存开销（U-B1-5）、compose 透传（U-B1-6）、chat/embedding 推理均未验证**（§9.2、§11.4）。
> **B1 不实现任何 AI 功能**：不做自然语言搜索、不做 RAG、不做推荐、不建向量库、不做前端、不写提示词、不调用 chat/embedding 推理。B1 只做**接入与可观测**：依赖引入、配置暴露（环境变量驱动）、一个**受登录保护的可达性健康端点**、以及相应测试。
> **B0 的前置已成立**（不是本文件的实测）：`docs/logs/2026-10-24-b0-local-llm-spike.md`（局域网 PC 上的 Ollama：`qwen2.5:7b` 100% GPU、56–70 tok/s；`bge-m3` 1024 维；中文小样本检索 3/3；已知 7B 幻觉与 macOS「本地网络」权限问题）。**B0 未覆盖项仍在**（该日志 §0.7/§3.2），本文件把其中与"接入"相关的部分作为依赖说明，不给 B0 补测。

## 元数据

| 项目 | 值 |
| --- | --- |
| 文档类型 | 实施方案（B1 里程碑：Spring AI 接入，不含 AI 功能） |
| 状态 | **🟢 已批准 / 已实现 / 已合并（PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67)，merge commit `aea0477`）** —— 批准：2026-10-24 主 agent（oracle C1–C5 落实，P-B1-1..10 裁定见 §9.1）；实现：`worker` 完成 + 三路审查通过 + F1–F6 修复落地（§11）；交付：commit **`e446aab`** → **PR #67 已 MERGED**（merge commit **`aea0477`**，`mergedAt` 2026-11-08T23:16:16Z，head 分支已删除），`main` @ `aea0477` CI **双 job SUCCESS**，**issue #66 已 CLOSED**（2026-11-08T23:16:17Z）（§12） |
| 创建日期 | **2026-10-24**；**修订 2026-10-24**（落实 oracle 条件与主 agent 裁定）；**实现结果回填 2026-10-24**（§11、§12 与超时/文件计数/测试细节的实测修正） |
| 工作区基线（合并后） | `main` @ **`aea0477`**（`Merge pull request #67 from PNZ-z/codex/b1-spring-ai-integration`，2026-11-08T23:16:16Z）。实施分支 `codex/b1-spring-ai-integration`（基线 `main` @ `df24b67`）@ **`e446aab`** 已随 PR #67 合并，**head 分支已删除**；该 PR 相对 `main` 的改动 = `pom.xml`、`src/main/resources/application.properties`、`.env.example`、`src/main/java/com/fabflix/boot/ai/`（5 类）、`src/test/java/com/fabflix/boot/ai/`（3 类）、`docs/`（3 文件）。合并后 `git status --short` 仅剩 `?? .idea/`（本地 IDE，不属交付内容） |
| GitHub issue | **[#66](https://github.com/PNZ-z/ai-powered-Fabflix/issues/66)** `[B1] Integrate Spring AI with the local Ollama endpoint` —— **CLOSED**（`closedAt` 2026-11-08T23:16:17Z，由 PR #67 的 `Closes #66` 自动关闭；milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3) 保持 open，无 labels，2026-10-24 创建）。**远端 checkbox 是否勾选未核实**（本步无 GitHub 写操作）。规范记录 = `docs/issues/issue-66-b1-spring-ai-integration.md`（改写前的发布草稿已逐字发布，正文零差异） |
| Milestone | [#3 `Phase 3 — AI-powered platform`](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)（保持 open；B1 不关闭 milestone） |
| 上级路线 | `docs/plans/ai-roadmap-overview.md`（Option A → **Option B** → Option C）；`docs/plans/ai-option-b-spring-ai-rag.md`（已批准方案，B0 → **B1** → B-①/B-②/B-③） |
| 前置 | **Option A 已完成**（epic [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) CLOSED，Boot 3.5.15 / `release=17` / JDK 21 / jakarta）；**B0 已实测通过**（`docs/logs/2026-10-24-b0-local-llm-spike.md`）；**用户已确认开始 B1**（主 agent 转达） |
| 估算 | **0.5–1 天**（估算口径：1 个 controller + 1 个 service + 1 个 probe 接口 + 1 个 probe 实现 + 1 个 record = **产码 5 个类** + **3 个测试类** + 配置与文档；不含真实模型调用，也不含已删除的绑定测试）。**实际产出 = 上述 5 + 3 + 3 配置 + 3 文档 = 14 个文件**（§4.1） |
| 实现与验证证据 | `git diff`（`pom.xml`、`application.properties`、`.env.example`）+ `src/main/java/com/fabflix/boot/ai/`（5 个类）+ `src/test/java/com/fabflix/boot/ai/`（3 个类，18 个用例）+ 主 agent 独立复跑的 `mvn verify`（152 tests / 0.9208 / 新增产码 N=59, U=0）+ M1–M4 手工启动记录（§11） |
| 关键事实来源 | **实现与测试证据**：实际 `git diff` + `src/main/java/com/fabflix/boot/ai/` 与 `src/test/java/com/fabflix/boot/ai/` 源码 + 主 agent 独立复跑的 `mvn verify` 输出（§11）。**设计与依赖事实**：**oracle 只读核实**的 Spring AI 1.1.8 / Boot 3.5.15 事实（§10.4）；本文件早期**只读核对**的 Maven Central 工件（§10.1–§10.3）；`docs/logs/2026-10-24-b0-local-llm-spike.md`；`docs/architecture/frontend-migration-contracts.md` §6/§7/§9；`CustomerAuthenticationFilter.java`；`HealthController.java` |

---

## 1. 目标与背景

### 1.1 目标

1. **把 Spring AI 接进构建**：`spring-ai-bom` **1.1.8**（`<scope>import</scope>`）+ `spring-ai-starter-model-ollama`（版本由 BOM 管理，**不写死**），在 Boot 3.5.15 / JDK 21 上可编译、可通过既有 `mvn verify` 闸门。
2. **把配置暴露成环境变量**：Ollama `base-url`、chat 模型、embedding 模型、AI 功能开关**全部由环境变量驱动并带默认值**；**仓库内不得出现真实 PC IP**（B0 的 `192.168.87.48` 只出现在 `docs/` 的历史记录里）。
3. **给运维/演示一个可核对的真实信号**：`GET /api/boot/ai/health`（受登录保护）返回"是否启用、配置的模型名、端点是否可达"，**默认关闭时零网络探测**，开启时**只做轻量可达性探测**（2 秒上界），失败**安全降级为 200 + JSON**，绝不 500。
4. **用测试把以上钉死**：新增生产类必须有测试（覆盖率闸门 0.90 不变），且 **CI 不依赖 PC/Ollama**。
5. **为 B-① / B-② 留好接缝**：端点形状、错误语义、配置真值来源（`spring.ai.*` 连接参数 + `fabflix.ai.*` 开关）与已批准方案 §4.1（P2-11）一致，后续里程碑只往上加，不改已发布契约。

### 1.2 背景（为什么现在做、为什么只做这些）

- **Option A 是硬前置，已完成**：Spring AI 1.x 需要 Boot 3.x + Java 17+ + jakarta（已批准方案 §1.2、`pom.xml` 的 `spring-boot.version=3.5.15` / `maven.compiler.release=17` / enforcer `[21,22)`）。B1 是这个前置解锁后的**第一小步**。
- **B0 已经把"能不能用"证明了，但没有一行 Spring Boot 侧代码**：B0 全部是对 Ollama 端点的**外部 HTTP 调用**（主 agent 执行）；Spring AI 从未加入 `pom.xml`。所以"模型可用"≠"应用里接上了"。B1 的价值就是**把接入这件事本身变成可验证的**。
- **B0 的现场问题决定了 B1 的形状**：端点**不是 `localhost`**（是局域网 PC），因此必须有**显式 `base-url` 配置**；**没有鉴权**（B0 日志 §1.3 第 4 条），因此**不能把地址回显到响应里**；**冷启动 7.64 s / 换入换出**（§0.4）意味着**不能靠"调用一次 chat/embedding"判断可达**——那会把健康检查变成一次模型加载；**macOS「本地网络」权限**（§0.6）意味着**"连不上"必须能被正确表达**，而不是伪装成 500 或启动失败。
- **不做功能是刻意的**：B-①/B-② 需要的 vecstore 依赖、卡片模板、语料切分、golden set、评测脚本都不在 B1；混进来会让"接入是否成功"无法单独判定。

---

## 2. 范围

### 2.1 In scope（本 issue 只做这些）

| # | 内容 | 备注 |
| --- | --- | --- |
| S1 | `pom.xml`：`<dependencyManagement>` 内 import `org.springframework.ai:spring-ai-bom:1.1.8`（`<type>pom</type>`、`<scope>import</scope>`）；`<dependencies>` 加 `spring-ai-starter-model-ollama`（**无 `<version>`**） | 版本以属性表达（候选，见 §3.3） |
| S2 | `src/main/resources/application.properties`：4 组配置（见 §4.2），**全部 `${ENV:default}` 形式** | 默认值 = B0 选定模型；默认地址 = `http://localhost:11434` |
| S3 | `GET /api/boot/ai/health`（新增 controller，落在现有受保护前缀内，**无需改 filter**） | 契约见 §5 |
| S4 | 可达性探测组件（接口 + JDK `HttpClient` 实现，可注入、可 mock）；**超时的真值 = `AiHealthService.PROBE_TIMEOUT`（2s 常量），经 `isReachable(Duration)` 的**方法参数**传入**；实现类**不持有第二个超时**，只将收到的值用于 `HttpRequest.timeout(...)` 与总体 `orTimeout(...)`；测试传短值；**不新增运维配置键** — P-B1-4 = A） | 实现见 §3.1-A；实际形状见 §11.1 |
| S5 | 测试：controller 切片（401 / disabled / ok / unreachable）+ 探测组件单测（200 / 非 200 / 超时 / 连接拒绝 / 非法 URL）+ service 单测（`enabled` 分支与 probe 交互）。**不加** `ApplicationContextRunner` 绑定测试（P-B1-5 = B，理由与 oracle 证据见 §3.6） | 见 §6 |
| S6 | `.env.example`：新增 4 个变量占位（**不含真实 IP/密钥**） | 见 §4.3 |
| S7 | 文档：本计划、`docs/issues/proposed-*.md` 草稿、工作流日志 | 日志由 docs-writer 在实施后写 |

### 2.2 Out of scope / Non-goals（明确不做）

| 排除项 | 理由 |
| --- | --- |
| **任何 chat / embedding 推理调用**（含"顺便调一次验证模型能回答"） | 会触发模型加载（冷 7.64 s）与显存换入换出；B1 只做接入，不验证输出质量 |
| **RAG / 向量库 / `SimpleVectorStore` 依赖引入** | 属 B-①/B-②（M4.1/M4.2）；B1 只**记录**本轮核实的坐标（§10.3），**不写入 `pom.xml`**；**也不回填** `docs/plans/ai-option-b-spring-ai-rag.md` §4.1（**P-B1-9 = B**：该回填留给 B-① 立项时） |
| **NL 搜索 / 推荐 / 提示词 / 前端 / 新页面** | B-③ 仍为条件性；前端属 B3 |
| **外部 API 兜底（OpenAI 等）** | 已批准为"显式开关"，但属后续；B1 不引入第二个 starter |
| **修改 `CustomerAuthenticationFilter`**（含把 `/api/boot/ai/**` 加进 `shouldNotFilter`） | D-AI 端点登录 = (a) 需登录（已批准，零行为变更）；匿名只读**未批准** |
| **改 `ci.yml` / `Dockerfile` / `compose.yaml`** | B1 不需要；CI 不得依赖 PC/Ollama；compose 是否透传新变量待后续决定（§9-U-B1-6） |
| **改 `docs/architecture/frontend-migration-contracts.md`**（含 §6 响应约定） | D-B7：AI 架构约定文档"实施后再立、仍待批准"；本端点的 `status` 是 **health 端点家族的状态字段**（与既有 `/api/boot/health` 的 `status:"UP"` 同族），**不是**业务 `success/fail` 契约的"例外"（oracle C5；P-B1-3 = A：不改 `state`、不现在就入契约文档） |
| **改 `README.md` / `frontend/README.md`** | **P-B1-8 = B：B1 不更新 README**（`worker` 也不改；`docs-writer` 本就只写 `docs/`）；`.env.example` 即权威占位表 |
| **更新 `docs/plans/ai-roadmap-overview.md` 的执行状态** | 在 B1 **实现阶段**不改（当时 PR 未合并）；**2026-11-08 合并后已由 `docs-writer` 回填**（B1 已完成 / PR #67 / `aea0477` / #66 CLOSED），见 §12 补充 4 |
| **扩大 JaCoCo `excludes`** | 与既有政策冲突（契约文档 §7：excludes 仅限离线工具，仍 8 项） |

### 2.3 Deferred from B0（**不属于 B1**；`AC-B-12` 仍未满足）

> 来源：`docs/logs/2026-10-24-b0-local-llm-spike.md` §3.2 的 B1 待办清单。oracle 条件 **C2** + 主 agent 裁定把该清单**切分**：B1 只做**接入**（B0 #2/#3/#4，加 #1 的坐标核实），**不**闭已批准方案里的 `AC-B-12` 及其他评测项。

| B0 §3.2 项 | 内容 | B1 是否做 | 归属 |
| --- | --- | --- | --- |
| #5 | 补测**第二个 embedding 候选** + **中→英**跨语言用例，闭环 `AC-B-12`；并为"查询 embedding 2.3–2.5 s"定性（是否换入换出）+ 定 keep-alive 策略 | **否** | **`AC-B-12` 仍未满足**；作为 **B-①/B-② 的准入前置**，在 **B-① 之前单独做**，届时**另建 issue**（**本工作流不创建 B1.5 issue**，只在此记录为后续工作） |
| #6 | **21,087 卡片**的一次性 embedding 耗时/内存；`docs/`（11 md）**切分块数**（语料指纹三要素之一） | **否** | 同上（评测/语料规模类，属 B-① 的语料与索引准备） |
| #7 | **p95 延迟**目标值"实测后再定"（需在有/无模型换入换出两种条件下各测一轮） | **否** | 同上（D8 未闭环项，依赖 #5/#6 的结论） |
| #8 | `qwen2.5:7b` 的 **NL→JSON 合规率**（≥95% 才立项 B-③） | **否** | B-③ 立项前的评测项，与 B1 无关 |
| #1 | `SimpleVectorStore` 在 Spring AI 1.1.x 的确切 artifact/类名/BOM 坐标 | **部分**：**只做坐标核实**（§10.3：`org.springframework.ai:spring-ai-vector-store:1.1.8`）；**§4.1 回填延后**（P-B1-9 = B） | 回填动作留给 B-① 立项时 |
| #2 / #3 / #4 | 引入 BOM + starter；先排除 macOS「本地网络」权限；用 `spring.ai.*` 配 chat/embedding 且开关默认 `false` | **是**（B1 的全部实现范围） | 本计划 §2.1 S1–S7 |
| #9 | （原待主 agent 决策）是否把 B0 结论回填 `ai-local-llm-deployment.md` / `ai-option-0-local-llm-spike.md` | **否**（且不在 B1 边界内） | 主 agent 另行决策 |

**明确结论（不得被读成已完成）**：

- B1 **不闭环 `AC-B-12`**，也**不**闭环 D8 的 p95/引用正确率；B1 只把"Spring AI 接上了、端点可达性可被观测"变成事实。
- 上表 #5–#8 **未被任何 B1 验收条件覆盖**；B1 任何 AC 不得被引用为这些项的完成证据。
- B-① / B-② 的**准入前置** = 先完成 #5（含 `AC-B-12`）；否则"模型/语料够用"这个前提仍是未验证的假设。

### 2.4 验收条件（AC-B1-1 .. AC-B1-10 为可机械验证项；AC-B1-11 为流程/记录项，不作合并门禁）

> 与 issue 草稿的 AC 一一对应；每条都给出**可复跑的判据**，不依赖“看起来对”。

| # | 验收条件 | 判据 |
| --- | --- | --- |
| **AC-B1-1** | 构建通过且依赖形状正确 | `mvn --batch-mode --no-transfer-progress verify` 在 JDK 21 下成功；`pom.xml` 中 `spring-ai-bom` 以 `<type>pom</type>/<scope>import</scope>` 出现在 `<dependencyManagement>`，`spring-ai-starter-model-ollama` **无自带 `<version>`**；`mvn dependency:tree` 含 `spring-ai-ollama:1.1.8` 与 `spring-boot-starter:3.5.15`，**无第二个 Boot 版本** |
| **AC-B1-2** | 配置全部环境变量驱动、无硬编码 | `grep -rn "192\.168" pom.xml src/ .env.example` **无命中**；`application.properties` 的 4 个 AI 项均为 `${ENV:default}`；`.env.example` 含这 4 个变量（值仅为 `localhost`/模型名/`false`） |
| **AC-B1-3** | 默认关闭时**零探测** | 未设任何 `FABFLIX_AI_*` 时，已登录请求返回 `200 {enabled:false, status:"disabled", reachable:false}`；**测试断言** probe 从未被调用（`verify(probe, never())`），而不是靠人工观察 |
| **AC-B1-4** | 可达时报告 `ok`，且**只碰一个只读端点** | loopback stub 返回 200 → `status="ok"`, `reachable=true`；stub 记录的请求**恰为 1 个**且路径**只能是** `/api/version`（即 `/api/version` 是**唯一**请求）；并显式断言**未**请求 `/api/tags`、`/api/ps`、`/api/chat`、`/api/generate`、`/api/embed`、`/v1/**` 中任何一个 |
| **AC-B1-5** | 不可达时**安全降级**、不挂死 | 连接拒绝 / 超时 / 非 2xx / 非法 URL 四种情形均返回 **HTTP 200** + `reachable:false` + `status:"unreachable"`；无 5xx、无栈泄漏；超时预算是**整体 deadline**（从发起探测到返回 ≤ 配置值，默认 2s；**不是** connect 2s + request 2s ⇒ 最坏 ~4s，见 §6.2 P3），单测用注入的短超时断言返回时机 |
| **AC-B1-6** | 认证边界零变更 | 匿名请求 → `401` + `{"errorMessage":"Authentication required."}`；`git diff` 中**无** `CustomerAuthenticationFilter.java` |
| **AC-B1-7** | 响应契约与最小暴露面 | 响应键集恰为 `enabled/chatModel/embeddingModel/reachable/status`；`status ∈ {disabled, ok, unreachable}`；**不含** `baseUrl`/`host`/`port`/token |
| **AC-B1-8** | 覆盖率不倒退 | `mvn verify` 绿且 JaCoCo `BUNDLE/LINE ≥ 0.90`；`Tests run` ≥ 既有基线；`excludes` 仍 8 项（未扩）；采用 §6.4 的算术：设新增产码 `N` 行、其中未覆盖 `U` 行，则必须满足 **`U ≤ 0.1N + 17.8`**（由 `(988 + N − U) / (1078 + N) ≥ 0.90` 展开得到）；实施时用 `target/site/jacoco/jacoco.csv` 重算，**不预设固定容差行数** |
| **AC-B1-9** | CI 不依赖 PC/Ollama | `ci.yml` 零 diff；测试仅用 loopback stub 或 mock；在**没有 Ollama** 的机器上 `mvn verify` 可通过（以 PR 的 CI 运行或本地无 Ollama 环境为证据） |
| **AC-B1-10** | 无 Ollama 也能启动，且**启动期无任何出站 AI 连接** | 无 Ollama + 默认配置下应用正常启动（日志无 AI 异常，`/api/boot/health` 仍 `UP`）；`FABFLIX_AI_ENABLED=true` 指向不可达地址时应用不崩、端点返回 `unreachable`（§6.5 M1–M3 记录命令与输出）。"启动期零出站"已有**代码级证据**（§10.4：`initializeModel` 在 `NEVER` 下 no-op、`OllamaApi` 构造不发请求）；**端到端**仍由 M1 的实际启动记录承担 |

> **AC-B1-11（对应 issue 草稿的 AC-11，等同项）：诚实记录 = 流程/记录项，不作合并门禁。** 它不走代码断言，而是 verification note：工作流日志必须区分"已验证 / 未验证"（特别是面向真实局域网端点的手工检查 M4，需 JVM 获得 macOS"本地网络"权限），并明写"尚无任何 AI 功能"。主 agent 在合并前核对日志即可，**不得因为该项而阻塞 CI 或把已跑的测试写成未跑（反之亦然）**。

---

## 3. 候选方案与取舍

### 3.1 可达性探测的实现方式（**核心候选**）

目标：判断"Ollama 进程在这个地址上是否应答"，**不加载模型**、**有确定的时间上界**、**可单测**。

| 候选 | 做法 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- | --- |
| **A. JDK `java.net.http.HttpClient`**（推荐） | `HttpClient.newBuilder().build()`（**不设 connectTimeout**，避免与整体预算叠加）+ `HttpRequest.newBuilder(uri).timeout(t).GET()`，只请求 **`GET {baseUrl}/api/version`**；**整体上界**用 `sendAsync(...).orTimeout(t)` 包住，使“一次探测”真的 ≤ t（**实测落地**：`HttpRequest.timeout(t)` 与 `orTimeout(t)` 用**同一个**方法参数，前者负责在预算耗尽时**终止 exchange**——审查 F1，见 §11.2） | 零新增依赖（JDK 17+ 自带）；超时可精确控制且有**单一整体 deadline**；测试可用 `com.sun.net.httpserver.HttpServer` 起本地 stub（JDK 自带）；不引入 Spring AI 的 bean 依赖 | 需要自己写 3 个 timeout/异常分支；`HttpClient` 需要关闭/复用（可持有单实例，`HttpClient` 是线程安全的） | **已采纳并已实现（P-B1-1 = A）** |
| B. Spring `RestClient` | `RestClient.builder()` + `ClientHttpRequestFactory` 设 connect/read timeout | 与 Spring 生态一致；已有 `spring-web` | 默认 `SimpleClientHttpRequestFactory` 的超时是"无限/不明确"，必须显式配置；把一个"进程级探活"耦合到 MVC 客户端栈上收益不大 | 备选 |
| C. 复用 Spring AI 的 `OllamaApi` / `OllamaChatModel` bean | 调 starter 提供的 API bean | 复用配置、少一个地址来源 | 把 B1 的健康端点与 Spring AI 内部 API（1.1.8 的非稳定内部面）绑定；且 `OllamaApi` 的失败语义是异常，需要再包一层；一旦要在 `enabled=false` 时"完全禁用"自动配置就会连带端点不可用 | **不推荐** |
| D. TCP 连通性探测（`Socket.connect`） | 只测端口 | 最简单、最快 | 无法区分"端口被别的进程占用"与"Ollama 应答"；也测不到 HTTP 层是否健康 | 不采用（可作为排障手段） |

**共同约束（三个候选都要遵守）**：

- **不调用** `/api/chat`、`/api/generate`、`/api/embed`、`/v1/chat/completions`、`/v1/embeddings`（会加载/换入模型）。
- **不读** `/api/ps`（B0 用它取证 GPU，属诊断，不是探活必需）。
- **不复用 B0 的 OpenAI 兼容路径**（`/v1/models` 也可用，但原生 `/api/version` 更轻且与本 starter 的语义一致）。
- 探测地址**从同一份配置派生**（`spring.ai.ollama.base-url`），**不新增第二个地址真值**（P2-11）。
- 任何异常（连接拒绝 / 解析失败 / 超时 / 非 2xx）都归一为 `reachable=false`，**只记录一行不含敏感信息的日志**。

### 3.2 健康端点的响应字段

| 问题 | 候选 | 建议 |
| --- | --- | --- |
| 字段集 | (a) `{enabled, chatModel, embeddingModel, reachable, status}`（主 agent 给定）；(b) 再加 `baseUrlHost`/`baseUrl`；(c) 再加 `latencyMs` | **(a)**；(b) **不采纳**：B0 已记录端点**无鉴权**、位于内网，回显地址等于把内网拓扑写进 HTTP 响应/浏览器/前端日志；`host:port` 同理（只写 DEBUG 日志）。`latencyMs` 属可观测（B5），B1 不加（避免"多一个数值就多一个被误读的契约"） |
| `status` 三态语义 | (a) `disabled` / `ok` / `unreachable`（主 agent 给定）；(b) 复用 `{status:"success"|"fail"}` 包装 + 其他字段表达可达性 | **(a)（P-B1-3 = A）**。语义澄清（oracle C5）：这个 `status` 是 **health 端点家族的状态字段**，与仓库既有的 `/api/boot/health` 返回 `status:"UP"` 同类；它**不是**业务 `success/fail` 契约的“例外”——该契约约束的是业务操作，本端点根本不是业务操作。因此 `unreachable` 不表示业务失败，HTTP 仍为 **200**。另外：**`reachable=true`/`status=ok` 只证明 `GET /api/version` 返回 2xx**，**不证明** `qwen2.5:7b`/`bge-m3` 已安装/已加载/维度正确 —— **reachable ≠ ready**，模型就绪语义留给 B5/M4.x（见 §5.2、§8.1） |
| `enabled=false` 时其它字段 | (a) 仍返回配置的模型名 + `reachable=null`/`false`；(b) 只返回 `{enabled:false,status:"disabled"}` | **(a) 但 `reachable=false`**（不用 `null`，避免三态布尔）：`enabled=false` 时**不做探测**，`reachable=false` + `status=disabled`，`chatModel`/`embeddingModel` 仍回显（它们只是配置字符串，无敏感性）→ 便于运维确认"配置读到了、只是关着" |

### 3.3 版本管理：写死 vs 属性 vs 完全继承

| 候选 | 做法 | 取舍 |
| --- | --- | --- |
| A. `<version>1.1.8</version>` 直接写在 `<dependency>` 上 | 最直白 | **违反 B1 要求**（版本由 BOM 管理，不写死 starter 版本） |
| **B. 属性 + BOM**（推荐） | `<properties><spring-ai.version>1.1.8</spring-ai.version></properties>`；BOM 用 `${spring-ai.version}`；starter 无 `<version>` | 升级改一处；与既有 `spring-boot.version` / `jacoco.version` 的风格一致 |
| C. BOM 版本也写死 1.1.8（无属性） | 少一行 | 与项目既有"版本进 properties"的写法不一致 |

### 3.4 两个 BOM 的顺序（Spring Boot vs Spring AI）

- **已核实事实（§10.2）**：`spring-ai-bom:1.1.8` 的 178 条 `dependencyManagement` 条目中**不含 `spring-boot-starter`/任何 `org.springframework.boot` 工件**；反过来 `spring-boot-dependencies:3.5.15` 也不管理 `org.springframework.ai:*`。→ **两者工件集合不重叠，导入顺序在版本解析上没有影响。**
- **仍建议固定顺序并在文档写明**：`spring-boot-dependencies` 在前，`spring-ai-bom` 在后。理由：Maven 对多个 `import` 的依赖管理是"第一个声明的匹配项获胜"，把 Boot 放前可以保证**未来 Spring AI BOM 若开始管理 Boot 工件也只影响 AI 自身**，方向与"Boot 是平台基线"一致。
- **已核实事实**：`spring-ai-starter-model-ollama:1.1.8` 自带依赖 `spring-boot-starter:3.5.15`，**与项目 `spring-boot.version=3.5.15` 完全一致** → 不会引入第二个 Boot 版本（AC 里用 `dependency:tree` 断言）。

### 3.5 `enabled=false` 时是否同时禁用 Spring AI 自动配置

| 候选 | 做法 | 取舍 |
| --- | --- | --- |
| **A. 保持自动配置开启，只在业务层判 `fabflix.ai.enabled`**（推荐） | starter 在 classpath → `OllamaChatModel`/`OllamaEmbeddingModel` bean 仍会创建；B1 **不使用**它们 | 简单、零魔法；**前提 = bean 创建不触发网络**。已核实 `spring.ai.ollama.init.pull-model-strategy` 的**默认值是 `NEVER`**（`OllamaInitializationProperties` 构造器字节码：`PullModelStrategy.NEVER`、`timeout=5m`、`maxRetries=0`）→ 默认**不会拉模型**。**且已有代码级证据（oracle §10.4）**：初始化路径 `initializeModel` 在 `NEVER` 下**直接 no-op**，`OllamaApi` 构造器**不发任何请求** → 启动期无出站。端到端仍由 AC-B1-10/M1 记录 | **已裁定（P-B1-7 = A）：首版用 A** |
| B. 用 `spring.ai.model.chat=none` / `spring.ai.model.embedding=none` 关闭 | 已核实这两个键由 `@ConditionalOnProperty` 控制 `OllamaChatAutoConfiguration`/`OllamaEmbeddingAutoConfiguration` | ✅ 比 A 更彻底（连 bean 都不建），**但单靠 `spring.ai.model.chat=none` 会启动失败**：`ChatClientAutoConfiguration` 需要 `ChatModel` bean（oracle 核实）→ 必须**同时排除**该自动配置（必要时还有 `ChatMemoryAutoConfiguration`）。并且这两个 key 是静态属性，无法随 `FABFLIX_AI_ENABLED` 动态联动；会让“启用”变成“改两处” | **仅作回退选项**（见下方 §3.5.1） |
| C. 用 Spring profile 隔离 | `@Profile("ai")` | 与现有单 profile（`local`）习惯冲突；B1 不需要 | **仅作回退选项**（见下方 §3.5.1） |

#### 3.5.1 回退方案（仅当 A 实测失败时启用，三选一，按优先级）

> 条件：AC-B1-10/M1 实测发现**启动期有出站网络或启动失败**（与代码级证据矛盾）。这时按以下**优先级**选一个，不要自己发明第四种：

1. **①（推荐）`spring.ai.model.chat=none` + 排除 `org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration`**（必要时同时排除 `…chat.memory.autoconfigure.ChatMemoryAutoConfiguration`）。理由：这是 "关掉模型" 的最小完备集——只写 `none` 会因 `ChatClientAutoConfiguration` 要 `ChatModel` 而启动失败，排除不干净的回退等于把故障从“网络”换成“启动”。
2. **②** 用 profile 隔离：只在 `ai` profile 下引入 starter（依赖侧隔离，代价是构建/启动双路径，且与现有单 profile 习惯冲突）。
3. **③** 直接移除 starter（最彻底）。可行——**health 端点仍可用**，因为 probe 用 JDK `HttpClient`、**不依赖 Spring AI**（这正是 P-B1-1 选 A 而非 C 的额外收益）。

> **首版仍用 §3.5-A**（自动配置保持开启），回退仅在实测证伪时启用；回退一旦启用，必须回到 oracle/主 agent 关卡确认，而不是静默切换。

### 3.6 配置绑定是否需要新测试模式（**已裁定：B —— 不加 `ApplicationContextRunner`**）

- 问题本身仍然成立：健康端点会回显“Spring AI **实际生效**的 chat/embedding 模型名”；若键名写错，端点就会**报一个没在用的模型名**（伪信号）。
- 已核实（§10.2）1.1.8 里**两种键形状都存在**：`OllamaChatProperties` 既有 `getModel()/setModel()`（→ `spring.ai.ollama.chat.model`）也有 `getOptions()`（→ `spring.ai.ollama.chat.options.model`）；`OllamaEmbeddingProperties` 同样。
- **oracle 的代码级证据（决定性的）**：`OllamaChatProperties.getModel()` **委托**给 `options.getModel()`（embedding 属性同理）——**`.options.model` 与 `.model` 同源**，写任一都能被同一 getter 读到。因此专门加一个 `ApplicationContextRunner` 测试去“证伪键名”**收益极低**，却要引入仓库尚无的测试模式（新依赖面 + 新失败面）。
- **主 agent 裁定：P-B1-5 = B（不加绑定测试）**。采用的替代做法：
  1. 端点回显的模型名从**同一个键**读取（`@Value`/`Environment` 读 `spring.ai.ollama.chat.options.model` / `spring.ai.ollama.embedding.options.model`，与 `application.properties` 的占位符**字面一致**）；
  2. **service 单测**覆盖“注入值原样透出”（§6.3 S5），保该字段不静默变空；
  3. “Spring AI 真的在用它”由**M4.1 首次真实调用**验证（已写入 B-① 的前置，不归 B1 的 AC）。
- **因此**：`src/test/java/com/fabflix/boot/ai/AiPropertiesBindingTest.java` **从拟改文件清单中删除**（不再存在“可选文件”），§6 测试计划与 issue 草稿同步更新。

---

## 4. 拟修改文件与配置

### 4.1 拟修改/新增文件清单

| # | 文件 | 类型 | 内容 | 是否新文件 |
| --- | --- | --- | --- | --- |
| 1 | `pom.xml` | 修改 | `<properties>` 加 `spring-ai.version`；`<dependencyManagement>` 加 `spring-ai-bom` import（Boot 之后）；`<dependencies>` 加 `spring-ai-starter-model-ollama`（无版本） | 否 |
| 2 | `src/main/resources/application.properties` | 修改 | 4 组 AI 配置（见 §4.2），全部 `${ENV:default}` | 否 |
| 3 | `.env.example` | 修改 | 4 个变量占位（见 §4.3） | 否 |
| 4 | `src/main/java/com/fabflix/boot/ai/AiHealthController.java` | 新增 | `GET /api/boot/ai/health` | **是** |
| 5 | `src/main/java/com/fabflix/boot/ai/AiHealthService.java`（若拆出 `AiHealthStatus`，由 worker 在获批范围内收敛；**不得改响应契约**） | 新增 | 组装 `enabled/模型名/reachable/status`，`enabled=false` 时**不调用** probe；捕获全部 `RuntimeException` → `unreachable`（P-B1-2 = A） | **是** |
| 6 | `src/main/java/com/fabflix/boot/ai/OllamaReachabilityProbe.java` | 新增（**接口**） | 契约：`boolean isReachable(Duration timeout)`（整体 deadline 语义记在 Javadoc） | **是** |
| 7 | `src/main/java/com/fabflix/boot/ai/HttpOllamaReachabilityProbe.java` | 新增（**实现**） | JDK `HttpClient`，`GET {baseUrl}/api/version`；`HttpRequest.timeout(t)` + `sendAsync(...).orTimeout(t)` 使用**同一个**方法参数 | **是** |
| 8 | `src/main/java/com/fabflix/boot/ai/AiHealthResponse.java` | 新增（record） | 响应体 5 字段 | **是** |
| 9 | `src/test/java/com/fabflix/boot/ai/AiHealthControllerTest.java` | 新增 | `@WebMvcTest` 切片（见 §6.1） | **是** |
| 10 | `src/test/java/com/fabflix/boot/ai/AiHealthServiceTest.java` | 新增 | 纯单测（`enabled` 分支 + probe 交互） | **是** |
| 11 | `src/test/java/com/fabflix/boot/ai/HttpOllamaReachabilityProbeTest.java` | 新增 | `com.sun.net.httpserver.HttpServer` stub（见 §6.2） | **是** |
| 12 | `docs/plans/b1-spring-ai-integration.md` | 新增 | 本文件 | **是** |
| 13 | `docs/issues/issue-66-b1-spring-ai-integration.md` | 新增（原 `proposed-*.md` 已改名） | issue #66 规范记录（含已发布正文 + Internal tracking） | **是** |
| 14 | `docs/logs/2026-10-24-issue-66-b1-spring-ai-integration.md` | 新增（实施后） | 工作流日志（两大分区） | **是** |

**共计 14 个文件**（以实际 diff 为准）= **产码 5（#4–#8）** + **测试 3（#9–#11）** + **构建/配置 3（#1–#3）** + **文档 3（#12–#14：计划 1 + issue 记录 1 + 日志 1）**。作废的早期计数：旧稿写“共计 13 个 = 产码 4”，那是**接口/实现拆成两个文件之前**、且日志文件名还写作 `<编号>` 占位时的预估；实际落地为 **5 个产码类**（`OllamaReachabilityProbe` 接口 + `HttpOllamaReachabilityProbe` 实现），日志文件名固定为 `issue-66`。**已删除**原 `AiPropertiesBindingTest.java`（**P-B1-5 = B**，理由 §3.6）：不再有“可选文件”，worker 不得自行补上。

**明确的"不改"清单**：`CustomerAuthenticationFilter.java`、`HealthController.java`、`EmployeeAuthenticationFilter.java`、`ci.yml`、`Dockerfile`、`compose.yaml`、`docs/architecture/*`、`docs/plans/ai-roadmap-overview.md`、`README.md`、`frontend/**`、任何既有测试。

### 4.2 配置（`application.properties`，草案）

```properties
# —— AI：连接/模型参数，真值来源 = Spring AI 自己的前缀（P2-11）——
spring.ai.ollama.base-url=${FABFLIX_AI_OLLAMA_BASE_URL:http://localhost:11434}
spring.ai.ollama.chat.options.model=${FABFLIX_AI_CHAT_MODEL:qwen2.5:7b}
spring.ai.ollama.embedding.options.model=${FABFLIX_AI_EMBEDDING_MODEL:bge-m3}

# —— AI：本项目自有开关（fabflix.ai.* 只承载开关/语料/阈值，不重复连接参数）——
fabflix.ai.enabled=${FABFLIX_AI_ENABLED:false}
```

**说明与约束**：

1. `spring.ai.ollama.base-url` 的默认值 `http://localhost:11434` **与 Spring AI 1.1.8 的默认值一致**（`OllamaConnectionProperties.baseUrl`），因此默认行为不改变任何东西；局域网 PC 地址只能经 `FABFLIX_AI_OLLAMA_BASE_URL` 注入（B0 的 `192.168.87.48` **不进仓库**）。
2. 键名来源：`spring.ai.ollama`（`OllamaConnectionProperties.CONFIG_PREFIX`）、`spring.ai.ollama.chat`/`spring.ai.ollama.embedding`（`OllamaChatProperties`/`OllamaEmbeddingProperties` 的 `CONFIG_PREFIX`）+ `options.model`（`OllamaChatOptions`/`OllamaEmbeddingOptions` 的 `model` 字段）——**从 1.1.8 字节码常量读出，非猜测**（§10.2）。**两键同源已由 oracle 核实**（§10.4：`getModel()` 委托 `options.getModel()`），因此**不再加绑定测试**（§3.6，P-B1-5 = B）；**真实生效性**留给 M4.1 首次真实调用。
3. **超时不在 properties 里新增自有键**（**P-B1-4 = A**：探测超时的唯一真值是 `AiHealthService.PROBE_TIMEOUT = Duration.ofSeconds(2)`，**经 `isReachable(Duration)` 方法参数**传给探测实现，测试传短值；**不加** `fabflix.ai.health.timeout` 运维键）。若将来 B5 需要可运维调整，另行获批后再加。
4. **不设置** `spring.ai.ollama.init.pull-model-strategy`（默认 `NEVER` 已满足“不自动拉模型”）；若回退到 §3.5.1 的 **①**，则需**同时**加 `spring.ai.model.chat=none`/`spring.ai.model.embedding=none` **与**排除 `ChatClientAutoConfiguration`（必要时 `ChatMemoryAutoConfiguration`）——**属回退路径，不在首版**。
5. `application-local.properties` **不改**（AI 配置与 profile 无关）。

### 4.3 `.env.example`（草案）

```dotenv
# —— AI（可选；全部关闭时留空即可）——
# Ollama 端点。默认 localhost；局域网/远程端点请在此填入（不要提交真实地址到仓库）
FABFLIX_AI_OLLAMA_BASE_URL=http://localhost:11434
# chat / embedding 模型名（需已在 Ollama 侧 pull 完成）
FABFLIX_AI_CHAT_MODEL=qwen2.5:7b
FABFLIX_AI_EMBEDDING_MODEL=bge-m3
# AI 功能总开关（默认 false：端点返回 disabled，且不做任何网络探测）
FABFLIX_AI_ENABLED=false
```

> `.env.example` 只放**本机默认值**（`localhost`），**不含**真实 IP、token 或密钥。`.env` 已被 `.gitignore` 忽略（`.env` / `.env.*` / `!.env.example`），因此真实地址不会进仓库。

---

## 5. 数据 / API 契约（草案）

### 5.1 端点

| 项 | 值 |
| --- | --- |
| 方法/路径 | **`GET /api/boot/ai/health`** |
| 认证 | **需要登录**（`/api/boot/` 前缀 + 不在 `CustomerAuthenticationFilter.shouldNotFilter` 白名单 → 现状自动保护，**零 filter 改动**） |
| 匿名请求 | **401** + `{"errorMessage":"Authentication required."}`（filter 生成，内容与其既有 401 完全一致） |
| 请求参数 | 无 |
| 副作用 | **无状态变更**；`enabled=true` 时**只有一次**出站 GET（`{baseUrl}/api/version`，**整体 deadline 2s**）；`enabled=false` 时**零出站** |
| 缓存 | 无（每次请求重新探测；不引入缓存，避免"缓存掩盖故障"） |

### 5.2 响应

`200 application/json`（**所有** `enabled` 与可达性组合都是 200；B1 不定义 4xx/5xx 的 AI 错误态）：

```jsonc
// enabled=false（默认）：不探测
{ "enabled": false, "chatModel": "qwen2.5:7b", "embeddingModel": "bge-m3",
  "reachable": false, "status": "disabled" }

// enabled=true 且 /api/version 应答 2xx
{ "enabled": true, "chatModel": "qwen2.5:7b", "embeddingModel": "bge-m3",
  "reachable": true, "status": "ok" }

// enabled=true 但连接拒绝 / 超时 / 非 2xx / 解析失败
{ "enabled": true, "chatModel": "qwen2.5:7b", "embeddingModel": "bge-m3",
  "reachable": false, "status": "unreachable" }
```

| 字段 | 类型 | 语义 | 约束 |
| --- | --- | --- | --- |
| `enabled` | boolean | `fabflix.ai.enabled` | 默认 `false` |
| `chatModel` | string | 配置的 chat 模型名 | 原样回显配置值（**不**做可用性判断） |
| `embeddingModel` | string | 配置的 embedding 模型名 | 同上 |
| `reachable` | boolean | 配置的 Ollama **HTTP 端点**是否在 2s 内对 `GET /api/version` 返回 2xx | `enabled=false` 时恒为 `false`（**未探测**，语义 = "未知/未启用"，由 `status` 区分）。**注意：`reachable=true` 只代表"端口上的 HTTP 服务在应答"，不代表模型就绪**（见下方 reachable ≠ ready） |
| `status` | string enum | `disabled` \| `ok` \| `unreachable` | 三态，**health 端点家族的状态字段**（与 `/api/boot/health` 的 `status:"UP"` 同族），**不是**业务 `success/fail` 契约的例外（oracle C5 / P-B1-3 = A） |

**必须连带读的两个限制（oracle C5）**：

1. **`status` 不是业务结果的 `success/fail`**：它是 health 端点家族的状态字段（`/api/boot/health` 用 `status:"UP"`），描述的是“本端点的探针状态”；因此 `unreachable` **不**表示业务操作失败，HTTP 仍为 **200**。
2. **`reachable=true` / `status=ok` 只证明 `GET /api/version` 返回 2xx** —— 它**不证明** `qwen2.5:7b` / `bge-m3` 已安装、已加载、显存驻留、或 embedding 维度（1024）正确。**reachable ≠ ready**：模型就绪语义留给 **B5/M4.x**（"模型未加载"与"模型不可用"的区分已在 B0 日志 §0.4 列为设计需求）。不要把本端点的 `ok` 当作“RAG 可以开工”的依据。

**明确不返回**：`baseUrl`、`host`、`port`、token、模型大小、GPU/`size_vram`、延迟数值、任何 Ollama 原始响应体。（延迟与 GPU 属诊断/可观测，B5 或手工排障。）

### 5.3 错误与降级语义

| 情形 | 行为 |
| --- | --- |
| `enabled=false` | 200 `status=disabled`；**不创建连接、不发请求**（测试用 `verify(never())` 断言） |
| `enabled=true` + 连接拒绝（`ConnectException`） | 200 `status=unreachable`、`reachable=false`；`WARN`/`DEBUG` 日志一行（错误类别 + 耗时；本机日志可含 base-url，**响应不得回显**） |
| `enabled=true` + 超时（`HttpTimeoutException`） | 同上（`status=unreachable`） |
| `enabled=true` + 非 2xx（如 404/500） | 同上（记日志含状态码） |
| `enabled=true` + 非法 URL（配置错误） | **不抛 500**：捕获 `IllegalArgumentException`/`URISyntaxException` → `status=unreachable` + `WARN` 日志（配置错误可被运维看见，但用户侧安全降级） |
| 应用启动时 Ollama 缺失 | **不影响启动**（AC-B1-10）：已无启动期网络依赖（`pull-model-strategy=NEVER` 默认；oracle 代码级证据：`initializeModel` 在 `NEVER` 下 no-op、`OllamaApi` 构造不发请求）；端到端仍由 M1 记录 |

---

## 6. 测试计划

### 6.1 `AiHealthControllerTest`（`@WebMvcTest(AiHealthController.class)`，`@MockitoBean AiHealthService`）

> 模式沿用仓库既有做法（契约文档 §7）：`@WebMvcTest` + `@MockitoBean`（`org.springframework.test.context.bean.override.mockito.MockitoBean`），表单/会话通过 `.param(...)`/`.sessionAttr(...)`。**`@WebMvcTest` 切片会注册 `Filter` bean**，因此 `CustomerAuthenticationFilter` 生效，匿名请求自然得到 401（既有测试同样依赖这一点，例如 `MovieListControllerTest` 必须显式 `sessionAttr`）。

> **分工**：controller 切片只验证**契约形状 + 认证**（service 用 `@MockitoBean` 喂入三种 canned 结果）；**"`enabled=false` 时不探测"** 属于 `AiHealthService` 的职责，在 `AiHealthServiceTest` 里用 `verify(probe, never())` 断言（§6.3）。这样每个断言都落在真正负责该行为的类上，不会被 mock 掩盖。

| # | 用例 | 断言 |
| --- | --- | --- |
| T1 | 匿名请求（**真实生效的 `CustomerAuthenticationFilter`**） | `401`；`$.errorMessage == "Authentication required."`；service **未被调用** |
| T2 | 已登录 + service 返回 `disabled` 组合 | `200`；`enabled=false`、`status=disabled`、`reachable=false`、`chatModel`/`embeddingModel` 原样透出 |
| T3 | 已登录 + service 返回 `ok` 组合 | `200`；`status=ok`、`reachable=true`、`enabled=true` |
| T4 | 已登录 + service 返回 `unreachable` 组合 | `200`；`status=unreachable`、`reachable=false`；**不是 4xx/5xx** |
| T5 | 已登录 + 任意组合 | 响应 JSON 的**键集恰好**为 `enabled/chatModel/embeddingModel/reachable/status`（无 `baseUrl`/`host`/`port`/`token`） |

> 默认值 `enabled=false` 的**真实来源**是 `application.properties` 的 `${FABFLIX_AI_ENABLED:false}`，不在 controller 切片里断言（那里 service 被 mock）。§3.6 已裁定**不加** `ApplicationContextRunner` 绑定测试（**P-B1-5 = B**），因此“默认开关为 false”的验证由 **`AiHealthServiceTest` 的注入分支 + §6.5 M2 手工启动记录** 共同承担，controller 切片不承担该项。

### 6.2 `HttpOllamaReachabilityProbeTest`（真 HTTP，仅本地 loopback）

用 JDK 自带 `com.sun.net.httpserver.HttpServer` 在 **127.0.0.1:0（随机端口）** 起 stub，**不依赖外网/PC/Ollama**：

| # | 用例 | 断言 |
| --- | --- | --- |
| P1 | stub 返回 `200` + `{"version":"x"}` | `true`；stub 记录的**请求数恰为 1**、**路径 = `/api/version`**、**方法 = `GET`**；并断言 stub **从未**收到 `/api/tags`、`/api/ps`、`/api/chat`、`/api/generate`、`/api/embed`、`/v1/**`（handler 记录 `getRequestURI()` 与 `getRequestMethod()` 两个列表，逐个断言不存在） |
| P2 | stub 返回 `500` | `false`（**非 2xx 即不可达**；不解析 body，避免对 Ollama 版本差异敏感） |
| P3 | stub handler 睡眠 > 超时（**实测实现：`Thread.sleep(600)`**） | `false`，且**整体耗时**在超时上下界之间（注入的短超时 = **150ms**；断言 **`100 ≤ elapsed < 1000` ms**，以证明是**整体 deadline** 而非 connect/read 各一份，否则最坏会到 ~2× 超时）；不给测试套件加 2s。**实测**：probe 在约 **153 ms** 返回，**test case 约 0.775 s**（包含 600 ms stub 的 teardown 等待，属预期开销） |
| P4 | 关闭端口（`new ServerSocket(0)` 取端口后关闭） | `false`（连接拒绝分支，不抛异常） |
| P5 | 非法 base-url（如 `ht!tp://x` / `""` / `null`） | `false` + 不抛异常（配置错误的安全降级） |
| P6 | 当前线程已置中断位时探测 | `false` 且**中断标志被保留**（`Thread.currentThread().isInterrupted()` 仍为 `true`）；**保留**（审查时确认保留，不合并进 P3） |

> 成本与稳定性：loopback + 随机端口，无固定端口冲突；超时用例用短超时（≤200ms）；**实测该测试类每个用例 ~0.79–0.82 s（含 P3/P6 的 600 ms stub teardown），整类 < 6 s**。**不写"重试"逻辑**（B1 不需要；重试会把 2s 上界变成 N×2s）。

### 6.3 `AiHealthServiceTest`（纯单测，不起 context）

| # | 用例 | 断言 |
| --- | --- | --- |
| S1 | `enabled=false` | **`verify(probe, never())`**（关键：默认关闭时零探测）；`status=disabled`、`reachable=false` |
| S1b | **空值开关** `enabled=""`（F3 新增） | 同 S1：视为 `disabled` + `probe never`（空值不得造成启动/请求失败） |
| S1c | **大小写宽松** `enabled="TRUE"`（F3 新增） | 视为启用（仅 `true` 忽略大小写；其他任何值 = 关闭） |
| S2 | `enabled=true` + probe `true` | `status=ok`、`reachable=true`；probe **被调用一次** |
| S3 | `enabled=true` + probe `false` | `status=unreachable`、`reachable=false` |
| S4 | `enabled=true` + probe 抛未预期 `RuntimeException` | **P-B1-2 = A**：service 捕获全部 `RuntimeException` → `status=unreachable`、`reachable=false`，**不冒泡、不出 500** |
| S5 | 配置值原样透出 | `chatModel`/`embeddingModel` 与注入值一致（不做可用性判断） |

> **实际用例数 = 7**（S1、S1b、S1c、S2、S3、S4、S5）；S1b/S1c 是审查修复 **F3** 后新增的空值与大小写用例。另外 S2 断言 **`verify(probe).isReachable(Duration.ofSeconds(2))`**——即超时真值来自 `AiHealthService.PROBE_TIMEOUT`，不是探测实现自带常量。

> §3.6 已裁定**不加**配置绑定测试（P-B1-5 = B）：oracle 已核实 `getModel()` 委托 `options.getModel()`（同源），再加 `ApplicationContextRunner` 价值极低。模型名“不是空/不被静默改写”由 **S5** 保证；“Spring AI 真的在读它”由 M4.1 首次真实调用验证（不属 B1）。

### 6.4 覆盖率与闸门影响

| 项 | 数值/口径 | 说明 |
| --- | --- | --- |
| 后端闸门 | `BUNDLE / LINE / COVEREDRATIO ≥ **0.90**`（`pom.xml`，2026-10-20 由 0.85 收紧） | **不得降低、不得扩 `excludes`**（仍 8 项） |
| 基线（文档记载；**实施前口径**） | **988 covered / 90 missed / 1078 total = 0.9165**、`Tests run: 134`（`docs/logs/2026-10-20-coverage-threshold-90.md`、契约文档 §7） | 实际实施时基线已变，已用 `jacoco.csv` 重算（见下方“实际结果”行）；本行仅保留历史推导所用口径 |
| 余量（**重新推导，C3**） | 设新增产码 **`N`** 行、其中**未覆盖** **`U`** 行，则 `(988 + N − U) / (1078 + N) ≥ 0.90` ⟺ **`U ≤ 0.1N + 17.8`** | 新增产码**估算** 60–90 行（controller + service + probe + record）→ 对应允许未覆盖约 **24–27 行**（即约 **10%**）。这只是**闸门上限**，**不是目标**：B1 的目标是**新增产码接近 100% 行覆盖**（每个 probe 失败分支、每个 service 分支都有测试，§5.3 一行一条）。实施时以 `target/site/jacoco/jacoco.csv` 重算 |
| 实际结果（**2026-10-24，已运行**） | **1137 total / 1047 covered / 90 missed = 0.9208**；`Tests run: 152`（基线 134 → +18）；新增产码 **N = 59 / U = 0**；`excludes` 仍 **8 项** | 详见 §11.3；口径与上式一致（`U = 0` 使闸门条件远远成立） |
| 修正说明 | 旧稿写的“新增产码未覆盖行 ≤ 19”是**错误推导**（把“新增全部行”当成“新增未覆盖行”，且把 `988` 当成固定分子） | 已删除；以本行公式为唯一口径（AC-B1-8、issue 风险条目同步修正） |
| 前端 | 无前端改动 → 前端闸门（lines 90）不受 B1 影响 | 不新增页面/组件。**main @ `df24b67` 已包含 PR #65 的前端测试确定性修复**（`MovieListPage.test.jsx` 滚动断言）；即前端 coverage 时序问题已由上流 PR 处理，**不是** B1 的责任范围 |
| CI | `mvn --batch-mode --no-transfer-progress verify`（Java 21）**零改动** | 测试**不发起任何真实出站**（唯一"网络"是 loopback stub）；CI runner 上没有 Ollama 也能通过 |

### 6.5 启动验证（手工，需在实施阶段记录命令与输出）

| # | 场景 | 期望 |
| --- | --- | --- |
| M1 | 未设任何 `FABFLIX_AI_*`、本机无 Ollama → `mvn spring-boot:run` | **正常启动**；日志无 AI 相关异常（即**启动期无任何出站 AI 连接/重试报错**；代码级已证 §10.4，本条提供**端到端**证据）；`GET /api/boot/health` 仍 `UP` |
| M2 | 登录后 `GET /api/boot/ai/health` | `200` + `enabled=false`/`status=disabled`（无出站请求；可用日志/抓包佐证） |
| M3 | `FABFLIX_AI_ENABLED=true` + 未设 base-url（`localhost:11434` 无 Ollama） | `200` + `status=unreachable`，响应时间 2 秒级（记录实测毫秒），**应用不崩** |
| M4 | `FABFLIX_AI_ENABLED=true` + `FABFLIX_AI_OLLAMA_BASE_URL=http://<局域网PC>:11434` | `200` + `status=ok`（**前置：macOS「本地网络」权限已授予运行 JVM 的终端/IDE**，否则会表现为 unreachable —— B0 日志 §0.6） |

> M4 依赖局域网 PC 与真实 Ollama，**属"能演示"的手工验证，不进 CI**；若实施时不可达，**必须记录为未验证**，不得写成已通过。
>
> **实际结果（2026-10-24）**：M1–M4 **均已执行并通过**，实测数字、命令与口径（包括 M1 的 `lsof` socket 级取证、M2 的 F3 空值回归、M4 的 keep-alive 采样）见 **§11.3**。注意：M2 在原计划里是“登录后看 disabled”，实际测试时用的是 **`FABFLIX_AI_ENABLED=` 空值**，兼验证 F3 修复（§11.2）。

---

## 7. 依赖与前置

| 前置 | 状态 | 说明 |
| --- | --- | --- |
| Option A 完成（Boot 3.5.15 / `release=17` / JDK 21 / jakarta） | **✅ 已完成**（epic #44 CLOSED，2026-10-21） | Spring AI 1.x 的硬前置 |
| B0 实测通过（端点可达、模型就绪） | **✅ 已完成（范围受限）** | `docs/logs/2026-10-24-b0-local-llm-spike.md`；**7B 幻觉 / 冷启动 / 无鉴权仍为已知风险** |
| Spring AI 1.1.8 工件可用 | **✅ 本轮只读核实**（§10.1：Maven Central 200） | 由主 agent 提供，docs-writer 复核了 URL 可下载性 |
| 本机 JDK 21 + Maven | 假设成立（`pom.xml` enforcer `[21,22)`） | 实施时 `mvn -version` 记录 |
| MySQL（`mvn verify` 是否需要 DB） | **既有测试不依赖 DB**（surefire + `@WebMvcTest`/standalone，全仓库无 `@SpringBootTest`） | 实施时若 `mvn verify` 因环境失败，按既有方式排查，**不得**为解决环境问题改测试结构 |
| macOS「本地网络」权限 | **B1 首跑前置**（M4 必需；M1–M3 不需要） | 未授权会伪装成"连接超时/不可达"（B0 日志 §0.6） |
| 用户授权创建 GitHub issue | **✅ 已完成**（issue #66 已由 `github-operator` 创建） | 未经授权不创建；本轮只回填文档，无 GitHub 写操作 |

---

## 8. 约束与风险

### 8.1 硬约束

1. **覆盖率闸门 0.90 不变**：新增产码必须有测试；未覆盖新增行 `U` 必须满足 **`U ≤ 0.1N + 17.8`**（`N` = 新增产码行数；口径见 §6.4，实施时用 `jacoco.csv` 重算）；**目标新增产码接近 100% 行覆盖**；**不得扩 `excludes`**。
2. **CI 不依赖 PC / 不依赖 Ollama / 不改 `ci.yml`**：所有测试只打 loopback stub 或 mock。
3. **认证边界零变更**：不改 `CustomerAuthenticationFilter`；端点必须 401（匿名）。
4. **配置真值来源单一（P2-11）**：连接/模型参数只在 `spring.ai.*`；`fabflix.ai.*` 只承载开关（以及将来的语料/阈值）。
5. **仓库内不出现真实内网地址/密钥**：`192.168.87.48` 只允许出现在 `docs/logs/` 的历史记录中；`pom.xml`/`src/`/`.env.example` 一律默认 `localhost`。
6. **不做 chat/embedding 调用**（含"顺手验证一下"）：B1 的任何代码路径都不得请求 `/api/chat`、`/api/generate`、`/api/embed`、`/v1/*`。
7. **响应不回显地址**：默认不回显 `base-url`/host/port（§3.2）。
8. **JSON 命名**：新端点字段 camelCase（契约文档 §6：“新增字段命名需与所属端点既有风格一致”；`/api/boot/health` 用平铺小写键，`movie-list` 用 snake_case，AI 端点用 camelCase 与该端点自身一致，属新增风格 → 记录在案）。
9. **`status` 的语义不得被写成“业务契约例外”**（C5）：它是 **health 端点家族字段**（同族：`/api/boot/health` 的 `status:"UP"`）。worker 不得改名为 `state`、**不得**在 B1 里动 `docs/architecture/`（留待主 agent 决定是否入契约文档）。
10. **`reachable ≠ ready`**（C5）：`reachable=true`/`status=ok` 只证 `GET /api/version` 2xx；**不得**在响应字段、日志文案、文档或提交信息里暗示“模型已就绪/已加载/已安装”（docstring 与 README 同理）；模型就绪语义留 **B5/M4.x**。

### 8.2 风险与回退

| 风险 | 概率 | 影响 | 缓解 | 回退 |
| --- | --- | --- | --- | --- |
| **加 starter 后启动期行为变化**（bean 初始化时探测/拉模型/失败导致启动失败） | 低（oracle 代码级证据已否定“默认会探测”；**M1 已提供端到端证据**） | 既有功能启动受影响（**最严重的一类**） | 已核实默认 `pull-model-strategy=NEVER`；`initializeModel` 在 `NEVER` 下 no-op、`OllamaApi` 构造不发请求（§10.4）；**M1 实测：0.992 s 启动、无 AI env 时进程对 11434 的 TCP 连接数 = 0**（§11.3） | **三选一，按优先级（§3.5.1）**：① `spring.ai.model.chat=none` **且排除** `ChatClientAutoConfiguration`（必要时 `ChatMemoryAutoConfiguration`）；② 用 profile 只在 `ai` profile 引入 starter；③ 移除 starter（health 端点仍可用）。**不得只写 `spring.ai.model.chat=none`** |
| **健康端点变成“慢端点”**（探测挂起拖住线程） | 中 | 影响演示体验 | **单一整体 deadline**（`sendAsync` + `orTimeout` + `HttpRequest.timeout`，默认 2s，而非 connect/read 各 2s ⇒ 最坏 ~4s）；不用重试；不做缓存。**M3 实测 16.9 ms（拒绝）/ M4 实测 34 ms（成功）** | 缩短默认超时（如 1s）或改为异步探测（后者超出 B1 范围 → 记为后续） |
| **`status` 被误读为业务 `success/fail`**（C5 已改措辞） | 中 | 读代码/写前端的人误判 | 明确说明它是 **health 端点家族的状态字段**（同族：`/api/boot/health` 的 `status:"UP"`），**不是**契约例外；`unreachable` 永不等于 `fail`，HTTP 恒 200（§3.2、§5.2、issue 风险条目） | 不改名 `state`（P-B1-3 = A）；若将来要入契约文档，由主 agent 另行决定 |
| **`reachable=true` 被误读为“模型就绪”**（C5） | 中 | 让 B-① 误以为前置已满足（尤其 `AC-B-12` 仍未闭环） | 文档/日志/字段注解一律写 **reachable ≠ ready**；只证明 `GET /api/version` 2xx；模型就绪留 B5/M4.x | 不新增就绪字段（不做“模型清单/加载状态探测”，P-B1-6 = A 已排除） |
| **模型名键名不生效**（`.model` vs `.options.model`） | 低（**已降级**） | 端点报告的模型名不是真正在用的 → 伪信号 | **oracle 已证 `getModel()` 委托 `options.getModel()`（同源）**；B1 用同一键读回显值 + service 单测 S5 保不静默变空（P-B1-5 = B） | 若 M4.1 首次真实调用发现不一致，再评估；B1 不再加 `ApplicationContextRunner` |
| **覆盖率余量不足**（新增 60–90 行产码） | 中 | CI 红 | 每个 catch 分支都有测试；record/DTO 无逻辑；口径 = `U ≤ 0.1N + 17.8`（§6.4），**不承诺固定 19 行** | 不得扩 `excludes`；缩小产码面积（合并类、减少分支） |
| **范围蔓延：把 B1 当成“完成 AI 前置”**（C2） | 中 | 把 `AC-B-12`/p95/语料规模误认为已完成 | §2.3 已把 B0 #5–#8 显式列为 **Deferred、不属于 B1**；issue body 加等价英文声明 | 不现在就建 B1.5 issue；在 B-① 前单独做并另建 issue |
| **macOS 本地网络权限误判** | 中高（首跑） | 把"M4 不可达"误记为"端点坏了" | 按 B0 日志 §0.6 先授权；用 `curl` 对照。**M4 实测已确认 JVM 具备权限**（`ok`，34 ms） | 记录为"环境未就绪"，不作结论 |
| **空值开关导致启动失败**（**实测已发生并修复**） | 已发生 | `FABFLIX_AI_ENABLED=` 空值曾使启动失败（布尔绑定） | **F3**：改为 `String` + 仅 `true`（忽略大小写）启用；空值/其他 = 关闭；S1b 单测 + M2 实测（0.941 s 启动、`disabled`、4 ms） | 保留空值回归测试（S1b），防复发 |
| **局域网端点无鉴权**（环境既有） | 确认 | 响应若回显地址会加剧暴露 | 不回显地址；文档记录边界 | 不因 B1 改变安全边界；加鉴权属部署专项 |
| **未授权就给 issue 发布** | 低 | 流程违规 | 本稿明确"待授权" | 不发布 |

### 8.3 回退方案

1. **功能级**：`FABFLIX_AI_ENABLED=false`（默认）→ 端点不出网、行为=无 AI。
2. **代码级**：B1 是单分支单 PR；`git revert` 即回到"无 Spring AI 依赖"的现状（无数据迁移、无外部状态）。
3. **依赖级**：删掉 `spring-ai-bom` import + starter 依赖即可（无任何源码对 Spring AI 的强依赖——probe 用的是 JDK `HttpClient`，**这一点是选 A 而非 C 的额外收益**）。

---

## 9. 决策点与未确认事项（裁定已完成）

### 9.1 决策点（P 项）—— **已裁定**（oracle 建议 → 主 agent 全部接受）

> 与初稿的区别：**不再有“待 oracle”。** 下表是最终裁定，worker 按此实现；若实施中发现与裁定矛盾的证据，**停下来回报主 agent**，不得自行改选。

| # | 决策点 | **裁定** | 依据 / 后续含义 |
| --- | --- | --- | --- |
| **P-B1-1** | 探测实现 | **A = JDK `HttpClient`** | 零新依赖、整体 deadline 可控、可 revert；**health 端点不依赖 Spring AI** → 移除 starter 也不影响它（§3.5.1 选项 ③ 的前提） |
| **P-B1-2** | probe 抛未预期异常时 | **A = service 捕获全部 `RuntimeException` → `unreachable`** | “永不 500”是 B1 的安全目标（避免栈泄漏）；对应 §6.3 S4 断言 |
| **P-B1-3** | `status` 三态的措辞 | **A = 保留 `status` 三态，不改 `state`**；按 **C5 改写措辞**：它是 **health 端点家族字段**（同族 `/api/boot/health` 的 `status:"UP"`），**不是**业务 `success/fail` 的例外 | 不动 `docs/architecture/`（D-B7）；`state` 不改名（避免契约漂移） |
| **P-B1-4** | 探测超时是否可配 | **A = 2s 代码常量**（`AiHealthService.PROBE_TIMEOUT`）**+ 方法参数传入**（`isReachable(Duration)`，测试传短值）；**不加** `fabflix.ai.health.timeout` 运维键；**探测实现不持有第二个超时**（唯一的 2s 常量在 service） | 少一个未测配置面；B5 需要时再加。**实际落地**（§11.2 F1）：同一个 `timeout` 参数同时用于 `HttpRequest.timeout(...)`（终止 exchange）与 `orTimeout(...)`（限时返回），两者同值 |
| **P-B1-5** | 是否加 `ApplicationContextRunner` 绑定测试 | **B = 不加**，并**删除** `AiPropertiesBindingTest.java`（§3.6 已改写） | oracle 已证 `getModel()` 委托 `options.getModel()` → `.options.model` 与 `.model` **同源**，绑定测试收益极低；改用 `@Value`/`Environment` 读同一键 + service 单测 S5 |
| **P-B1-6** | 探测目标端点 | **A = `GET /api/version`**；**不暴露模型清单** | 最轻且不动 GPU；**写明 reachable ≠ ready**（§5.2）：模型存在/加载属 B5/M4.x |
| **P-B1-7** | `enabled=false` 时是否禁自动配置 | **A = 保持自动配置开启**（首版），**回退按 C4 三选一**（§3.5.1：① `none` + 排除 `ChatClientAutoConfiguration`；② profile；③ 移除 starter） | 保留 oracle 代码级证据：`initializeModel` 在 `NEVER` 下 no-op、`OllamaApi` 构造不发请求 |
| **P-B1-8** | `README.md` 是否在 B1 更新 | **B = B1 不更新 README** | `.env.example` 即权威占位表；不进 AC |
| **P-B1-9** | 是否现在回填 `ai-option-b-spring-ai-rag.md` §4.1 | **B = B1 不回填** | 坐标已记录在**本计划 §10.3**，回填留给 B-① 立项时 |
| **P-B1-10** | 端点命名 | **A = B1 用 `/api/boot/ai/health`**；`/api/boot/ai/status` **留给 M4.2** | 后者将来回业务形 `success/fail`（含语料指纹），与 B1 的 health 端点语义不同——**不要合并两个端点** |

### 9.2 未确认事项（U 项，不得由本文件推出结论）

| # | 未确认项 | **状态** |
| --- | --- | --- |
| U-B1-1 | 加入 starter 后**启动期是否发生任何网络访问** | **✅ 已解决（代码级 + 端到端）**：oracle 核实 `initializeModel` 在 `pull-model-strategy=NEVER` 下 no-op、`OllamaApi` 构造器不发请求；**M1 端到端补充**：无 AI env 启动 0.992 s，进程对 11434 的 TCP 连接数 = **0**（`lsof -a` socket 级） |
| U-B1-2 | `.options.model` vs `.model` 哪个是 1.1.8 实际生效键 | **✅ 已解决**：`OllamaChatProperties.getModel()` **委托** `options.getModel()`（embedding 同理）→ 两键同源，`.options.model` 有效；实现统一用 `.options.model` 读回显值 |
| U-B1-3 | 局域网端点从 JVM 访问是否需要 macOS 授权 | **✅ 已解决（本机环境）**：**M4 实测** `enabled=true` + `http://192.168.87.48:11434` → **200 `ok` / 34 ms（probe 29 ms）** → 运行 JVM 的终端**已具备** macOS「本地网络」权限。**不能外推**为“任何环境都已授权” |
| U-B1-4 | 实施时覆盖率基线是否仍为 988/1078 | **✅ 已解决**：实施后实测 **1137 total / 1047 covered / 90 missed = 0.9208**，新增产码 **N=59, U=0**（§11.3）；旧基线 988/1078 不再用于推导 |
| U-B1-5 | starter 引入后对**启动时间/内存**的实际影响 | **❌ 仍开放（未测）**（仅知启动 0.99 s 可接受，**未与基线对比**、内存/依赖体积未量；不阻塞 AC，但必须记入日志） |
| U-B1-6 | `compose.yaml` 是否需要透传 4 个新变量 | **❌ 仍开放**（本次未改 compose；容器内 `localhost:11434` 无意义，需显式 `host.docker.internal` 或宿主机 IP —— 属部署决策） |
| U-B1-7 | `HealthController`（`/api/boot/health`）是否需要增加 AI 汇总字段 | **❌ 仍开放**（本次未改；避免把“应用健康”与“AI 可达”耦合） |

---

## 10. 本轮只读核实记录（可复跑）

> 以下 §10.1–§10.3 均为 `docs-writer` **本轮实际执行**的只读网络/文件读取（不是 `mvn`，也不是对 Ollama 的调用）；**未运行** `mvn` / `npm` / `docker` / CI / 任何 Ollama 请求。**§10.4 是 `oracle` 的只读核实结果**（由主 agent 转达，**不是** docs-writer 重跑）；本文件已就其中两条做了本地核对（见 §10.4 末行）。

### 10.1 Maven Central 可达性（HTTP 200 = 工件存在）

| 命令（摘要） | 结果 |
| --- | --- |
| `curl -o /dev/null -w '%{http_code}' .../org/springframework/ai/spring-ai-bom/1.1.8/spring-ai-bom-1.1.8.pom` | **200** |
| 同上 `spring-ai-starter-model-ollama/1.1.8/...pom` | **200** |
| 同上 `spring-ai-vector-store/1.1.8/...jar` | **200** |

### 10.2 BOM / starter / 属性键（下到本地临时目录 `javap`/`grep`，未写入仓库）

| 事实 | 证据 |
| --- | --- |
| `spring-ai-bom:1.1.8` 的 `dependencyManagement` 共 **178** 条 `dependency`，**含** `spring-ai-starter-model-ollama`（`${project.version}`）与 `spring-ai-vector-store` | `grep -n "starter-model-ollama\|spring-ai-vector-store" spring-ai-bom-1.1.8.pom` |
| BOM **不含**任何 `org.springframework.boot` 工件 | `grep -n "spring-boot" spring-ai-bom-1.1.8.pom` → 仅 `spring-ai-spring-boot-*`（AI 自己的集成件） |
| `spring-ai-starter-model-ollama:1.1.8` 依赖 = `spring-boot-starter:**3.5.15**`、`spring-ai-autoconfigure-model-ollama`、`spring-ai-ollama`、`spring-ai-autoconfigure-model-chat-client`、`spring-ai-autoconfigure-model-chat-memory` | starter 的 pom `<dependencies>` |
| 配置前缀（**字节码常量**）：`spring.ai.ollama`（`OllamaConnectionProperties`）、`spring.ai.ollama.chat`（`OllamaChatProperties`）、`spring.ai.ollama.embedding`（`OllamaEmbeddingProperties`）、`spring.ai.ollama.init`（`OllamaInitializationProperties`） | `javap -p -constants` 输出 `CONFIG_PREFIX` |
| 模型键两种形状并存：`getModel()/setModel()` **与** `getOptions()`（→ `options.model`） | `javap -p` 两个 Properties 类 |
| 初始化默认值：`pullModelStrategy = PullModelStrategy.NEVER`、`timeout = 5m`、`maxRetries = 0` | `javap -c OllamaInitializationProperties` 构造器 |
| 自动配置开关键：`spring.ai.model.chat`、`spring.ai.model.embedding`（`@ConditionalOnProperty`） | `javap -v` 两个 AutoConfiguration 的常量池 |
| 同一 starter 同时提供 chat 与 embedding 自动配置（`OllamaChatAutoConfiguration` / `OllamaEmbeddingAutoConfiguration` 都在 `spring-ai-autoconfigure-model-ollama` 里） | jar 内类清单 |

### 10.3 顺带核实（**B1 不引入，仅记录**）

| 事实 | 证据 |
| --- | --- |
| `org.springframework.ai.vectorstore.SimpleVectorStore` 位于 `org.springframework.ai:spring-ai-vector-store:1.1.8`（BOM 管理） | `unzip -l spring-ai-vector-store-1.1.8.jar \| grep SimpleVectorStore` |

> 这条**闭合了 B0 日志 §3.2 第 1 项的一半**（artifact/类名；但 B1 不引入该依赖、不写入 `pom.xml`）。**已裁定 P-B1-9 = B：B1 不回填** `ai-option-b-spring-ai-rag.md` §4.1 —— 坐标就留在本节，回填留给 B-① 立项时（避免在已批准方案文档里做未经批准的编辑）。

### 10.4 oracle 只读核实事实（**由 oracle 提供、主 agent 转达；本文件未重跑**）

> 用途：这些事实支撑了 C4/C5 与 P-B1-1/5/6/7 的裁定。它们在实施时**仍需被实证**（尤其“启动期无出站”的**端到端**部分）。

| 事实 | 影响哪个裁定 |
| --- | --- |
| `spring-ai-ollama:1.1.8` 传递依赖 `spring-webflux:6.2.19`、`jackson-databind:2.21.4`，与 Boot 3.5.15 已管理版本**一致** | 不会因导入 starter 产生第二个 Web/Jackson 版本（支持 AC-B1-1 的“无第二个 Boot 版本”族断言） |
| `OllamaChatProperties.getModel()` **委托** `options.getModel()` | **P-B1-5 = B**：`.options.model` 与 `.model` 同源，绑定测试收益极低 |
| `OllamaApi` **无 `version()` 方法** | 印证 **P-B1-1 = A**（不用 Spring AI 内部 API 做探活），且监视器只能自建 HTTP 请求 `/api/version` |
| `ChatClientAutoConfiguration` **需要 `ChatModel` bean** | **C4**：单用 `spring.ai.model.chat=none` 会因缺 `ChatModel` 而**启动失败** → 回退必须同时排除该自动配置 |
| `initializeModel` 在 `PullModelStrategy.NEVER` 下 **no-op**；`OllamaApi` 构造**不发请求** | **P-B1-7 = A** + **AC-B1-10** 的代码级证据（启动期无出站） |
| `/api/boot/ai/health` 确实受 `CustomerAuthenticationFilter` 保护：`shouldNotFilter` 白名单为 `/api/boot/login`、`/api/boot/employee-login`、`/api/boot/dashboard*`、`/api/boot/health`，**不含**它 | AC-B1-6 / §5.1（**零 filter 改动**就能得 401）；**本文件已复核源码确认**（`src/main/java/com/fabflix/boot/auth/CustomerAuthenticationFilter.java`） |

> 本文件复核的另一条：`/api/boot/health` 的现有响应为 `{"status":"UP","application":"fabflix-spring-boot"}`（`src/main/java/com/fabflix/boot/HealthController.java`）—— **支撑 C5 的“health 端点家族字段”措辞**（`status` 不是业务 `success/fail`）。

---

## 11. 实现结果（2026-10-24）

> 本节的数字与命令输出来自 `worker` 报告 + **主 agent 独立复跑核实**；`docs-writer` **未自行重跑** `mvn`（只读 `git diff`/`git status` 与 `src/` 源码核对形状）。本节不含交付结论：提交/推送/PR/CI/合并见 §12（**PR #67 已 MERGED，merge commit `aea0477`**）。

### 11.1 实际落地形状（与 §4.1 的对照）

| 项 | 实际 | 与计划的差异 |
| --- | --- | --- |
| `pom.xml` | `<properties>` 加 `spring-ai.version=1.1.8`；`<dependencyManagement>` 在 `spring-boot-dependencies` **之后** import `spring-ai-bom`（`<type>pom</type>`/`<scope>import</scope>`）；`<dependencies>` 加 `spring-ai-starter-model-ollama`（**无 `<version>`**） | 与 §3.3-B / §3.4 一致 |
| `application.properties` | 4 项 `${ENV:default}`：`spring.ai.ollama.base-url`、`spring.ai.ollama.chat.options.model`、`spring.ai.ollama.embedding.options.model`、`fabflix.ai.enabled` | 与 §4.2 一致（未新增超时运维键） |
| `.env.example` | 4 个占位（`localhost` + 模型名 + `false`），无真实 IP/密钥 | 与 §4.3 一致 |
| 产码 | `com.fabflix.boot.ai` **5 个类**：`AiHealthController`(22) / `AiHealthService`(60) / `OllamaReachabilityProbe`(16, 接口) / `HttpOllamaReachabilityProbe`(89, 实现) / `AiHealthResponse`(23, record) | **接口/实现拆为 2 个文件**（计划 §4.1 原先把它们写成 1 行）；文件计数见 §4.1 的 14 个 |
| 测试 | `AiHealthControllerTest`(5 用例) / `AiHealthServiceTest`(7) / `HttpOllamaReachabilityProbeTest`(6) = **18 个用例** | 类名从计划的 `OllamaReachabilityProbeTest` 改为 `HttpOllamaReachabilityProbeTest`（与实现类同名）；service 用例 5 → 7（F3 新增空值与大小写） |
| 超时 | `AiHealthService.PROBE_TIMEOUT = Duration.ofSeconds(2)`，**经 `isReachable(Duration)` 方法参数**传入；实现类**不持有第二个超时** | S4 / P-B1-4 的措辞已按实际修正（§2.1、§4.2、§9.1） |
| 认证边界 | `CustomerAuthenticationFilter` **零 diff**；匿名请求 401 `{"errorMessage":"Authentication required."}` 由既有 filter 产生 | 与 AC-B1-6 一致 |

### 11.2 三路审查与修复（无 P0/P1）

三路只读审查（正确性与边界 / 测试覆盖 / 必要性与复杂度）**均通过，未发现 P0/P1**。采纳并已落地的修复：

| 项 | 问题 | 修复 |
| --- | --- | --- |
| **F1** | `orTimeout` 只让 Future 超时返回，**不取消**底层 exchange（连接/读写可能继续挂着） | 请求上加 `.timeout(timeout)`，**与 `orTimeout` 同值**——超过预算时真正终止 exchange；两者共用一个方法参数，不产生第二个超时 |
| **F2** / P2-1 | `@Value` 上重复写了 4 处默认值，形成第二份配置真值 | 删除 4 处重复默认值 → **单一配置真值**（默认值只在 `application.properties`）；缺键时 **fail-fast**（而不是静默地拿代码里的副本） |
| **F3** | `fabflix.ai.enabled` 用 `boolean` 绑定时，`FABFLIX_AI_ENABLED=`（**空值**）会**启动失败**（已实测复现） | 改为 `String` + **只有 `true`（忽略大小写）启用**；空值/其他=关闭；补 2 个单测（S1b 空值、S1c `TRUE`） |
| **F6** | Javadoc 未说清 `disabled` 时 `reachable=false` 的含义 | 在 `AiHealthResponse` Javadoc 写明：disabled 时 `reachable=false` = **未探测**（不是"探测后不可达"），由 `status=disabled` 区分 |
| 测试 P3 | 上界 `500ms` 太紧（当时实现把 stub sleep 设得短）、且没证明"至少等到 deadline" | stub sleep **600ms**、断言 **`100 ≤ elapsed < 1000` ms**；实测 probe 约 **153ms** 返回（deadline 150ms），test case 约 **0.775s**（含 stub teardown） |
| 测试 P1 | 只断言路径，不断言方法 | 增加**方法 = `GET`** 断言（handler 同时记录 `getRequestMethod()`） |
| **P6（保留）** | 中断分支测试 | **保留**（确认中断标志不被吞掉），不合并进 P3 |
| **§6.1 超时注入点（接受）** | 审查提出超时是否该在 probe 内部常量 | **保持**：注入点 = **方法参数**，**2s 常量留在 service**；实现类不持有第二个超时（与 P-B1-4 一致） |

### 11.3 验证证据（已运行）

| 检查 | 结果 | 来源 |
| --- | --- | --- |
| `mvn --batch-mode --no-transfer-progress verify`（JDK 21） | **BUILD SUCCESS**；**Tests run: 152, Failures: 0, Errors: 0, Skipped: 0** | worker 报告 + **主 agent 独立复跑** |
| JaCoCo（`target/site/jacoco/jacoco.csv`） | **90 missed / 1047 covered / 1137 total = 0.9208**（闸门 0.90） | 同上 |
| 新增产码覆盖 | **N = 59 行，U = 0 行** → 闸门要求 `U ≤ 0.1·59 + 17.8 = 23.7`，**余量充足** | 同上 |
| `excludes` | **仍 8 项**（未扩） | 同上 |
| `Tests run` 基线 | **134 → 152**（+18，全部为新增类） | 同上 |
| 稳定性 | 新增测试**连续 5 次**运行结果一致；probe 测试类约 **0.79–0.82 s** | worker 报告 |

**手工启动验证（M1–M4，已实际执行）**

| # | 场景 | 结果 |
| --- | --- | --- |
| M1 | 无任何 `FABFLIX_AI_*`、本机无 Ollama | 启动 **0.992 s**；`/api/boot/health` = **UP**；应用进程 `lsof -nP -a -p <pid> -iTCP \| grep -c 11434` = **0**（**socket 级**证据：无到 11434 的出站连接；启动日志无 AI 异常是**弱证据**，故以 socket 为准） |
| M2 | 空 `FABFLIX_AI_ENABLED=`（F3 回归） | 启动 **0.941 s**；登录后端点返回 `disabled`，耗时 **4 ms**，**零探测** |
| M3 | `enabled=true` + 默认 `localhost` 无 Ollama | **200** + `unreachable`，**16.9 ms**（连接拒绝远快于 2s 预算） |
| M4 | `enabled=true` + `http://192.168.87.48:11434`（局域网 PC） | **200** + `ok`，**34 ms**（probe 29 ms）→ **JVM 已具备 macOS「本地网络」权限**；采样到 `HttpClient` keep-alive 的 `ESTABLISHED` 连接（**非泄漏**） |

### 11.4 未验证事项（不得读作已完成）

1. **"只请求 `/api/version`"仅在 loopback stub 上被证明**；**未对真实端点抓包**验证。
2. **keep-alive 连接的生命周期未测**（M4 只采样到连接存在，没测何时回收）。
3. **U-B1-5：starter 引入后的启动时间/内存开销未测**（starter 的启动耗时与包体积仍未量化）。
4. **U-B1-6：`compose.yaml` 是否透传 4 个新变量未做**（容器内 `localhost` 无意义）。
5. **模型就绪性未验**：`reachable=true` 仍 **≠ ready**（不证明 `qwen2.5:7b`/`bge-m3` 已安装/已加载/维度 1024）。
6. **chat/embedding 推理未做**（B1 范围外）。
7. **`AC-B-12` 仍未闭环**（§2.3）。
8. **CI**：**PR #67 的 CI 双 job 已 SUCCESS**（`Build and test (Java 21)`、`Frontend (lint, test, coverage, build, e2e)`）；**合并后 `main` @ `aea0477` 的 CI 运行同样 SUCCESS**（同一组 job，run 已完成）—— 均为主 agent 转达的 GitHub 元数据（`docs-writer` 未触发/未重跑 CI）。但 CI 绿**不**代表 §11.4 第 1–7 项（真实端点抓包、keep-alive、U-B1-5/U-B1-6、模型就绪、推理、`AC-B-12`）已验。

### 11.5 验收条件对照（本地口径；**非 CI/合并结论**）

| AC | 本地状态 | 证据 / 缺口 |
| --- | --- | --- |
| AC-B1-1 | ⚠️ 部分 | `mvn verify` 绿 + pom 形状可核（BOM import 在 Boot 之后、starter 无版本）；`mvn dependency:tree` 的逐行输出**本轮未回传**，"无第二个 Boot 版本" 仅有 oracle 的依赖事实佐证 |
| AC-B1-2 | ✅ | `git diff` 显示 4 项均 `${ENV:default}`；`.env.example` 仅 `localhost`/模型名/`false` |
| AC-B1-3 | ✅ | `AiHealthServiceTest` S1 `verify(probe, never())` + M2 实测 `disabled`、零探测 |
| AC-B1-4 | ✅（loopback） | P1：请求数=1、路径=`/api/version`、方法=`GET`，且六个禁用路径逐个断言不存在；**真实端点未抓包** |
| AC-B1-5 | ✅ | P2/P3/P4/P5 + 控制器 T4：四种失败均 200 `unreachable`；P3 证明整体 deadline |
| AC-B1-6 | ✅ | T1 → 401 + `Authentication required.`；`CustomerAuthenticationFilter.java` 零 diff |
| AC-B1-7 | ✅ | T5：`$.*` 计数 = 5 且 `baseUrl/host/port/token` 均不存在 |
| AC-B1-8 | ✅ | 0.9208 ≥ 0.90；152 ≥ 134；`excludes` 8；N=59/U=0 |
| AC-B1-9 | ✅ | `ci.yml` 零 diff、本地无 Ollama 环境 `mvn verify` 通过；**PR #67 的 CI**：`Build and test (Java 21)` = SUCCESS、`Frontend (lint, test, coverage, build, e2e)` = SUCCESS（主 agent 转达的 PR 元数据；`docs-writer` 未重跑） |
| AC-B1-10 | ✅（本地） | M1（0.992 s、socket 级零出站）+ M3（`unreachable` 且不崩） |
| AC-B1-11 | ✅ | 本文件 §11.3/§11.4 + 工作流日志的两大分区（已验证/未验证分开写） |

---

## 12. 状态与后续流程

| 阶段 | 负责人 | 状态 |
| --- | --- | --- |
| 本计划草稿（候选方案）+ issue 草稿 | `docs-writer` | **✅ 已产出（2026-10-24，初稿）** |
| 风险/方案审查（C1–C5 + P-B1-1..10） | `oracle`（只读） | **✅ 已完成** |
| 裁定建议（全部接受 oracle） | 主 agent | **✅ 已裁定**（见 §9.1） |
| 按裁定修订本计划 + issue 草稿 | `docs-writer` | **✅ 已完成**（本修订，基线 `df24b67`） |
| **批准本文件作为 worker 实施依据** | 主 agent | **✅ 已批准（2026-10-24）** |
| 创建 GitHub issue | 主 agent → `github-operator` | **✅ 已完成（2026-10-24）**—— [#66](https://github.com/PNZ-z/ai-powered-Fabflix/issues/66)，milestone #3，无 labels；正文与草稿逐字一致 |
| 实现（唯一 `worker`） | `worker` | **✅ 已完成（2026-10-24）**——产码 5 个类 + 3 个测试类（18 用例）+ 3 处配置；详见 §11.1 |
| 三路只读审查 → 修复 → 测试 | 3 × `reviewer` → `worker` | **✅ 已完成**——三路均通过（无 P0/P1）；采纳并修复 **F1/F2/F3/F6** + 两处测试强化，P6 与 §6.1 超时注入点保留（§11.2） |
| 文档回填 + 工作流日志 | `docs-writer` | **✅ 本次（本步）**——本文件 §11、issue 记录 Internal tracking、`docs/logs/2026-10-24-issue-66-b1-spring-ai-integration.md` |
| 提交 / PR | 主 agent + `github-operator` | **✅ 已完成（2026-10-24）**——commit **`e446aab`** 已推送 `origin/codex/b1-spring-ai-integration`；**PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67)** `feat: integrate Spring AI with the local Ollama endpoint`（base `main` @ `df24b67`），正文 `Closes #66` |
| CI 运行（PR #67） | GitHub Actions | **✅ 双 job SUCCESS**——`Build and test (Java 21)`、`Frontend (lint, test, coverage, build, e2e)`；`mergeStateStatus=CLEAN`、`mergeable=MERGEABLE`（主 agent 转达；不得写成 `docs-writer` 重跑） |
| **合并 PR #67** | 主 agent | **✅ 已完成（`mergedAt` 2026-11-08T23:16:16Z）**——**PR #67 = MERGED**，merge commit **`aea0477`**（`Merge pull request #67 from PNZ-z/codex/b1-spring-ai-integration`）；**head 分支 `codex/b1-spring-ai-integration` 已删除**；`main` @ `aea0477`（§12 补充 4） |
| **合并后 `main` CI** | GitHub Actions | **✅ SUCCESS**——`main` @ `aea0477` 的 `Build and test (Java 21)` 与 `Frontend (lint, test, coverage, build, e2e)` 均 SUCCESS，run 已完成（主 agent 转达；`docs-writer` 未触发/未重跑） |
| **关闭 issue #66** | GitHub（`Closes #66` 自动关闭） | **✅ 已 CLOSED**——`closedAt` 2026-11-08T23:16:17Z（合并后 1 秒，由 PR #67 正文 `Closes #66` 自动触发）；**远端 acceptance checkbox 是否勾选未核实**（本步无 GitHub 写操作） |

**当前状态与剩余动作（明确写出）**：

1. **实现已完成并在本地验证通过**（152 tests / 0.9208 / N=59,U=0），三路审查无 P0/P1，接受的修复已落地（§11.2）。
2. **已合并**：commit `e446aab` 经 **PR #67 MERGED**（merge commit **`aea0477`**，`mergedAt` 2026-11-08T23:16:16Z，head 分支已删除），`main` @ `aea0477` 的 **CI SUCCESS**；**#66 已 CLOSED**（2026-11-08T23:16:17Z，自动关闭）。B1 交付链路（提交 → PR → CI → 合并 → issue 关闭）**全部完成**。
3. **`AC-B-12` 仍未闭环**（§2.3）—— 这是 **B-① 的准入前置**，不是 B1 的缺陷，也**不在**本次范围：embedding 第二候选 / 中→英 / 21k 卡片 / `docs/` 切分 / p95 / NL→JSON 仍未做，**须在 B-① 之前作为单独步骤（B1.5）另建 issue 完成**。
4. **roadmap 已在合并后回填**（`docs/plans/ai-roadmap-overview.md`：B1 已完成 / PR #67 / `aea0477` / #66 CLOSED；`ai-option-b-spring-ai-rag.md`：B0/B1 完成、**B-① 未开始**、`AC-B-12` 仍未满足）；**milestone #3 保持 open**；**B-①/B-② 未开始**，不声称已进入。
5. **仍未勾选、也未核实的**：#66 远端 acceptance checkbox 状态（本步不做 GitHub 写操作）；**真实端点抓包、keep-alive 生命周期、U-B1-5/U-B1-6** 仍为未验证项（§11.4）。

**本次实现结果回填（2026-10-24）明确不做的事（再次声明）**：不创建/编辑 GitHub issue 或 PR（也不勾选任何远端 checkbox）；**不创建 B1.5 issue**（仅记录为后续）；不更新 `docs/plans/ai-roadmap-overview.md` 的执行状态（待 PR 合并后）；不修改 B0 日志、`ai-option-b` 方案、`docs/architecture/*`；`docs/` 下**只动三个文件**（本计划、issue 记录、新建工作流日志）；不编辑任何源码/测试/配置；不提交、不推送。

> **补充（2026-10-24，仅元数据回填）**：issue #66 创建后，`docs-writer` 只做了两件事——将 `docs/issues/proposed-b1-spring-ai-integration.md` 改名为 `docs/issues/issue-66-b1-spring-ai-integration.md` 并回填编号/URL/发布前置状态，以及在本文件回填 issue 编号与 §12 的 issue 创建状态。该次回填**不改范围、不改验收条件、不新增/修改源码或测试**。
>
> **补充 2（2026-10-24，实现结果回填）**：本文件新增 **§11 实现结果** 并把原 §11 改为 **§12 状态与后续流程**；同步修正 §2.1 S4 / §4.2 第 3 条 / §9.1 P-B1-4 的超时措辞（**方法参数注入**）、§4.1 的文件计数（**产码 5 / 总计 14**）、§6.2 P3 与 P1 的实测细节、§6.3 的 7 个用例与 §6.4 的实际覆盖率。**未修改任何源码/测试/配置，未提交/推送，未创建 PR**；`## GitHub issue body` 发布分区仍与远端 #66 逐字一致（未勾选远端 checkbox）。
>
> **补充 3（2026-10-24，PR/CI 元数据回填）**：本文件只回填**交付元数据**，**不改范围/验收条件/设计方案**：头部状态与元数据表（状态、工作区基线）、§2.2 表中“不更新 roadmap”行、§11.4 第 8 条（CI）、§11.5 中 AC-B1-9、§12 状态表（提交/PR、CI、合并）与 §12 的“当前状态与剩余动作”第 2/4 条。**事实**：commit `e446aab`、已推送 `origin/codex/b1-spring-ai-integration`、**PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67)**（base `main` @ `df24b67`，正文 `Closes #66`）、两个 CI job 均 SUCCESS、`mergeStateStatus=CLEAN`；**未合并（无 merge commit）、#66 仍 OPEN**。**本步不改 `docs/plans/ai-roadmap-overview.md`（待合并后回填）、不改源码/测试/配置、不勾选远端 checkbox、不提交/推送。**

> **补充 4（2026-11-08，合并后最终回填）**：本文件已把“合并待执行 / 未合并 / 无 merge commit”的旧状态**全部改写为已合并事实**，范围不变、验收条件不变、设计方案不变。**可核事实**：**PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67) = MERGED，`mergedAt` = 2026-11-08T23:16:16Z，merge commit `aea0477`**（`main` 上的 `Merge pull request #67 from PNZ-z/codex/b1-spring-ai-integration`），**head 分支 `codex/b1-spring-ai-integration` 已删除**；**`main` @ `aea0477` 的 CI SUCCESS**（`Build and test (Java 21)` + `Frontend (lint, test, coverage, build, e2e)`，run 已完成）；**issue [#66](https://github.com/PNZ-z/ai-powered-Fabflix/issues/66) = CLOSED**（`closedAt` 2026-11-08T23:16:17Z，由 `Closes #66` 自动关闭）；合并内容 = B1 实现 + 测试 + 计划/issue/日志（commit `e446aab`、`3bc9402`）。本次回填的落点：头部两条状态说明、元数据表（状态 / 工作区基线 / GitHub issue）、§2.2 的 roadmap 行、§11 开头、§11.4 第 8 条、§12 状态表（提交·PR、PR CI、合并、`main` CI、#66）与 §12 的“当前状态与剩余动作”。**不声称**远端 issue #66 的 acceptance checkbox 已勾选（**未核实**；本步无 GitHub 写操作）。**仍未闭环**：`AC-B-12`（第二 embedding 候选、中→英、21k 卡片、`docs/` 切分、p95、NL→JSON）—— 须在 **B-① 之前作为单独步骤（B1.5）另建 issue** 完成；**U-B1-5 / U-B1-6 / 真实端点抓包 / keep-alive 生命周期** 仍为未验证项（§11.4）。
>
> **补充 5（2026-11-08，合并后路线图回填）**：合并完成后，`docs-writer` 同步回填了两份上级/方案文档的**执行状态**（不改其已批准方案内容）：`docs/plans/ai-roadmap-overview.md`（**B1 已完成 = PR #67 / `aea0477`；#66 CLOSED**；**下一步 = B-① 前先完成 `AC-B-12` 的 embedding 评测/中→英（B1.5，另建 issue）**；milestone #3 保持 open；**不声称 B-①/B-② 已开始**）与 `docs/plans/ai-option-b-spring-ai-rag.md`（**B0/B1 已完成、B-① 未开始、`AC-B-12` 仍未满足**）。本步历史记录（补充 1–3）中“未提交/未合并/不改 roadmap”等表述是**当时状态的如实记录**，**现状以上述补充 4/5 为准**。本步**只改 `docs/`**：不改源码/配置/测试，不改 B0 日志与历史日志，不提交/不推送，不创建/编辑 GitHub issue 或 PR（**包括不创建 B1.5 issue**）。

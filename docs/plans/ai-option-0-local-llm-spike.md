# Option 0 — 本地 LLM 快速验证（零迁移 spike）

> **文档状态：✅ 已批准（用户，2026-10-19）** —— 但**未纳入目标路线 A → B → C**（用户选择直接 A → B → C）；本方案保留为**可选的降风险手段**，是否先做 spike 需另行确认。（批准前状态：已按 oracle 审查修订 / 待用户审批）
> 相关批准结论：**D-L1 已授权**在**实施阶段**安装 Ollama 并下载模型（**本次不安装**）；**D9 已接受**模型磁盘占用（实施前需清理磁盘）；**D8 质量下限已于 2026-10-19 由用户确认默认值**（目录 top-5 ≥80% / RAG 引用正确率 ≥95% / NL→JSON schema 合规率 ≥95% / **p95 先实测后定**）——**阈值确认 ≠ 已达标，仍待实测**；**D7 外部 API 上限亦已确认**（默认关闭；启用时 $2/日 且 ≤200k tokens）。
> 批准是方向批准：**本次未安装任何软件、未写任何源码、未建 issue**。
> **修订记录（2026-10-18，oracle 只读审查后；主 agent 裁定「全部接受」）**：**P2-5** 超时/错误路径测试改用 **JDK 自带的 `com.sun.net.httpserver.HttpServer`**，**删除原 O0-4（MockWebServer 依赖）决策项**；**P2-6** tokens/s 测量补**兜底法**（应用侧计时 + 固定输出长度）；**P2-7** spike 报告明确**落地为 `docs/` 内文件**；**P1-4** 补"macOS 容器无 Apple GPU（无 Metal 直通）"这一更强理由。事实口径与总览一致：`javax.servlet`=**11** 个文件、`@SpringBootTest`=**0** 处、覆盖率最多可新增 **≈60 行**未覆盖代码、语料 = **11 个 md / 38,755 词**。审查报告：`.pi/artifacts/oracle-ai-roadmap-review.md`。
> 本文**不声称**任何功能已实现、任何基准已运行。所有性能数字均为**估算/待实测**。
>
> 命名说明：对应 `docs/plans/request-YYYY-MM-DD-<slug>.md` 约定；本文件按任务指定使用 `ai-option-0-*.md` 临时命名，issue 创建后应重命名为 `issue-<编号>-<slug>.md`。**长期归属建议**：本文件属一次性版本方案，留在 `docs/plans/`；而它产生的 **spike 报告**（实测数字与选型结论）落地到 `docs/` 内（见 §2.1 第 7 项）。

## 元数据

| 项目 | 值 |
| --- | --- |
| 文档类型 | 实施方案（版本 0：快速验证）——**已批准（2026-10-19），但未纳入目标路线 A → B → C**（保留为可选降风险手段） |
| 创建日期 | 2026-10-18 |
| 工作区基线 | `main` @ `76e3d64` |
| 事实来源 | `.pi/artifacts/scout-ai-roadmap.md`（§A1/§A2/§A4/§C11–C15/§D16/§D18）；`docs/plans/ai-roadmap-overview.md`；`docs/plans/ai-local-llm-deployment.md` |
| 上级文档 | `docs/plans/ai-roadmap-overview.md`（总览与推荐路线） |
| 前置 | 无代码前置；仅需本机安装推理软件 + 拉取模型（**本任务不执行**） |
| 预估工作量 | **1–2 周**（估算，含环境安装与实测） |
| 状态 | **✅ 已批准（用户，2026-10-19），但未纳入目标路线 A → B → C**（保留为可选降风险手段）；本次**未安装、未写源码、未建 issue** |

---

## 总结

Option 0 是在**不升级 Spring Boot、不引入 Spring AI、不改任何既有架构约定**的前提下，最快拿到"本地大模型在这台机器上到底能做到什么"的**实测结论**的一条路：用 Java 11 自带的 `java.net.http.HttpClient`（或 `RestTemplate`）直接调用本地 Ollama 的 **OpenAI 兼容** `POST /v1/chat/completions`，在后端加一个最小的"检索 → 拼 prompt → 生成 → 返回"端点，并在前端（复用现成公共页 `/playground`，或首页搜索旁加"AI 搜索"）加一个入口。**为什么做**：它把风险最高、最不可预测的部分（8 GiB 内存上的模型质量与时延）提前到 1–2 周内验证，为 Option A/B/C 的选型提供真实数据（tokens/s、内存占用、可用上下文、中英文指令跟随、结构化输出成功率）。**主要风险**：结论可能是"8 GiB 做不了令人满意的 NL 搜索"；且这条路径**绕开了用户点名的 Spring AI**，手写的 HTTP 调用代码在 Option B 时大概率被替换（返工）。**工作量**：约 1–2 周（估算，其中环境/模型下载占相当比例）。**适合谁**：想先看效果再决定是否投入大迁移的用户；不适合"只要最终架构分"的用户（那种情况直接 Option A→B）。

---

## 1. 目标与背景

### 1.1 目标

1. **跑通链路**：检索（先用现有 MySQL 全文检索或内存中的简单过滤）→ 拼 prompt → 调用本地模型生成 → 返回 JSON → 前端展示。
2. **拿到实测数据**（本方案的核心产出，见 §6）：
   - 本机可加载的最大模型规模与量化；
   - 实测 tokens/s（prompt 处理 / 生成）；
   - 首 token 延迟与端到端延迟；
   - 在 8 GiB 下的**可用上下文长度**；
   - NL → 结构化查询（JSON）的输出**格式合规率**；
   - 简单问答的**幻觉率**（人工抽样）。
3. **为选型提供依据**：模型/量化/上下文/超时/降级策略的推荐值。

### 1.2 背景（已证实事实）

| 事实 | 证据 |
| --- | --- |
| 本机 **Apple M2 / 8 GiB / 22 GiB 可用磁盘**，`Mac14,7` | scout §C11 |
| Docker Desktop 仅 **3.826 GiB** 内存（配额可调大，但挤压宿主 8 GiB）；且 **macOS 上容器拿不到 Apple GPU（无 Metal 直通）→ 容器内 Ollama 只能 CPU 推理** → 容器内跑 LLM 不可行 | scout §C11（`docker info`）；P1-4 |
| 本机**未安装** Ollama / llama.cpp / LM Studio / vLLM；`~/.ollama` 不存在 | scout §C12 |
| Ollama 默认服务 `http://localhost:11434`，提供 **OpenAI 兼容** `POST /v1/chat/completions`（及 `/v1/embeddings`） | scout §C13 |
| 4-bit GGUF 大致权重：3B≈2GB、7B/8B≈4.5–5GB、13B≈8GB（**估算**） | scout §C13 |
| 项目为 **Java 11 + Boot 2.7.18 + `javax.servlet`**，且守卫测试**禁止** `jakarta.servlet` | `pom.xml:14-15`、`pom.xml:23`；`SpringBootOnlyArchitectureTest.java:20-42` |
| AI 新增后端代码**默认计入 JaCoCo 85% 闸门**（扩 exclude 与政策冲突） | `pom.xml:163`；`README.md` 覆盖率节 |
| 前端 lines 88 阈值、e2e 在 CI，API 全 mock | `frontend/vitest.config.js`；契约文档 §7 |
| `/playground` 是现成公共页（静态演示数据），是最低成本的 AI 挂载点 | scout §A6 |
| 后端→前端响应约定：业务失败 = HTTP 200 + `{status:"fail",message}`；校验 400 `{errorMessage}`；未认证 401 `{errorMessage}` | 契约文档 §6 |

**关键推论**：因为不能升 Boot/不能引 Spring AI，本方案只能走**裸 HTTP 客户端**；这意味着实现必须**自己**处理：超时、重试、超时后的降级、响应解析、prompt 模板、错误映射。这些正是 Spring AI 会替你做的事情——也就是这条路的"技术债代价"。

---

## 2. 范围与非目标

### 2.1 In scope（候选）

1. 本机推理环境安装与模型拉取（**执行时机由用户决定；本规划文档不执行**）。
2. 后端一个最小 AI 端点（草案，名字待定）：`GET /api/boot/ai/health`（探测本地模型可用性/延迟）与 `POST /api/boot/ai/ask`（question → answer）。
3. 使用 Java 11 `HttpClient` 调用 `http://localhost:11434/v1/chat/completions`；**base-url、模型名、超时用配置项**（环境变量 + Spring 配置），默认关闭（feature flag）。
4. **检索层最小实现**：Option 0 不引入向量库。两个可选做法：
   - (a) 用现有 MySQL 全文检索（`MATCH(title) AGAINST(...)`，`MovieListRepository.java:119`）产出候选，再由模型组织答案；
   - (b) 直接把少量结构化电影行（`title/year/director/genres/stars/rating`）拼进 prompt（"结构化卡片"思路，scout §B10 承认这是 movies 表可提供的全部文本）——**该思路就是 Option B 的 B-①（电影目录语义检索）的简化版**。
5. 前端入口（二选一，取成本最低者）：
   - (a) 在既有 `/playground` 公共页加一个"问 Fabflix"区块；
   - (b) 首页搜索框旁加"AI 搜索"切换。
   两者都只调用新端点，复用 `apiClient.apiRequest`（契约文档 §5）。
6. **最小降级语义**：模型不可用/超时 → 明确错误（符合既有 `{status:"fail"}` 或 `{errorMessage}` 约定之一，需在实施前定稿）；**绝不静默返回空结果**。
7. **Spike 报告**（**落地为 `docs/` 内文件**，本次唯一交付物不能只留在本地）：建议路径 `docs/logs/YYYY-MM-DD-request-<slug>-local-llm-spike.md`（若后续挂到 GitHub issue 下，则改为 `docs/logs/YYYY-MM-DD-issue-<编号>-<slug>.md`）；内容 = §6 的全部实测数据 + 选型建议 + 测量命令。

### 2.2 Non-goals（明确排除）

| 排除项 | 理由 |
| --- | --- |
| 升级 Spring Boot / Java / jakarta | 本方案的定义就是"零迁移" |
| 引入 `spring-ai-*` 任何依赖 | 不可行（Spring AI 1.x 需 Boot 3.x，scout §A5） |
| 向量数据库 / `SimpleVectorStore` | 属于 Option B |
| 电影语料导入（剧情简介等） | 涉及获取与合规，属 Option C 决策 |
| 推荐系统 | 属 Option C |
| Agent tools / 审批 / 审计 / 预算 | 属 Option C（roadmap 第 4 项） |
| 评测体系 / golden set / 夜间 CI job | 属 Option B（本方案只做**手工/脚本**的粗略统计，**不建 CI 闸门**） |
| 引入 Spring Security / JWT | 违反 AD-1（契约文档 §10） |
| 修改 `README.md` | 本任务（docs-writer）不改；P-2 已获用户批准（2026-10-19），由 `worker` 执行 |
| 生产部署（compose 加 ollama 服务） | 不可行（Docker 3.826 GiB，且 **macOS 容器无 GPU/Metal 直通 → 只能 CPU 推理**，scout §C11） |

---

## 3. 依赖与前置

| 前置 | 说明 | 是否已满足 |
| --- | --- | --- |
| 本机安装 Ollama（或 llama.cpp） | 见 `docs/plans/ai-local-llm-deployment.md` §推荐 | **否**（scout §C12：未安装） |
| 拉取一个 4-bit 量化模型 | 建议先 3B 级（磁盘/内存更安全），再试 7B/8B | **否** |
| 磁盘空间 | 当前 **22 GiB 可用**；模型下载 + 缓存需占用（估算 2–6 GB/模型） | 需用户确认（D9） |
| 后端可运行（`mvn spring-boot:run -Dspring-boot.run.profiles=local` + Compose MySQL） | 现有能力 | 是 |
| 配置项载体 | 现有 `application*.properties`/环境变量机制（`FABFLIX_*` 先例） | 是 |
| 网络（可选） | 模型下载需网络；推理本身离线 | 是 |

**无代码前置**——这是本方案相对 A/B/C 的唯一优势。

---

## 4. 方案设计（草案）

### 4.1 组件与数据流

```
Browser (React)
  └─ /playground 或首页"AI 搜索"
        │  apiClient.apiRequest('/boot/ai/ask', { method:'POST', body })
        ▼
Spring MVC Controller  (Boot 2.7 / Java 11 / javax.servlet)   ← 新建，受 CustomerAuthenticationFilter 保护
        │  1) 检索候选（MySQL 全文 或 结构化卡片）
        │  2) prompt 模板拼装（系统提示 + 候选数据 + 用户问题）
        ▼
AiClient (java.net.http.HttpClient, Java 11)
        │  POST http://localhost:11434/v1/chat/completions
        ▼
Ollama (本机原生进程, Metal 加速)
        ▲  JSON: choices[0].message.content
        │
      解析 + 安全过滤 → { status:"success", answer, sources:[...] }
```

### 4.2 后端设计要点

| 项 | 草案 | 依据 |
| --- | --- | --- |
| 端点 | `GET /api/boot/ai/health`、`POST /api/boot/ai/ask` | 需落在 `/api/boot/**` 内以获得现有认证保护（契约文档 §6） |
| 入参 | `question`（form 参数，与现有 `@ModelAttribute`/`@RequestParam` 风格一致） | 契约文档 §5（`apiClient` 不自动设 `Content-Type`，表单用 `URLSearchParams`） |
| 出参 | 成功：HTTP 200 `{status:"success", answer, sources:[...]}`；失败：HTTP 200 `{status:"fail", message}`（业务失败）或 400 `{errorMessage}`（参数非法） | 契约文档 §6 |
| 超时 | 连接超时 + 请求超时必须**显式设置**（`HttpClient.connectTimeout` / `HttpRequest.timeout`）；具体值**待实测**（3B 与 8B 差异大） | 需实测，不预设数字 |
| 配置项 | `fabflix.ai.enabled`（默认 false）、`fabflix.ai.base-url`（默认 `http://localhost:11434`）、`fabflix.ai.model`、`fabflix.ai.timeout-seconds` | 与 `FABFLIX_BOOT_PORT` 等既有外部化配置风格一致 |
| 关闭时行为 | 端点返回明确的"AI 未启用"失败，**不影响任何既有功能** | 安全性/可回退 |
| 并发 | 单实例假设（AD-2）；同一时刻限制并发请求数（本地模型单实例，估算为 1） | scout §C13；契约文档 §10 |
| 日志 | 记录模型名、耗时、prompt 长度、生成长度（**不记录敏感用户数据**） | §7 可观测性 |
| 异常映射 | 连接失败/超时 → 明确的 message；解析失败 → 明确 message；**不回显底层堆栈** | 契约文档 §6 安全展示惯例 |

### 4.3 Prompt 设计要点（草案）

- **系统提示**：限定角色（Fabflix 助手）、限定可用事实（只准用提供的候选数据）、限定输出格式（纯文本答案 + 明确的"无依据则说不知道"）。
- **结构化输出尝试**（用于 NL 搜索方向）：要求模型输出 **JSON**（如 `{"genre":"Sci-Fi","yearFrom":1990,"yearTo":1999}`），并在后端**严格校验**（白名单字段、类型、范围）；解析失败即视为失败（不猜）。**格式合规率是本 spike 的核心测量项之一**（小模型在 JSON 输出上常失败——这正是需要测的原因）。
- **防注入**：候选数据明确标记为"数据"，与系统指令分区；用户输入不得改变系统角色（详见总览 §8）。
- prompt 模板**集中在一个类/资源文件中**（便于后续被 Option B 的 Spring AI 模板替换，也便于单测）。

### 4.4 前端设计要点（草案）

- 复用 `apiClient.apiRequest`（契约文档 §5），页面挂 `/playground`（公共路由已存在，scout §A6）或首页。
- 状态机沿用既有页面模式：`loading/success/error` + `retry`（契约文档 §4）。
- 错误展示：`role="alert"`、不回显敏感内容（沿用 #30 安全展示先例）。
- **新增页面必须**：在 `AppRoutes.jsx` 注册、（如进入 `App.test.jsx` 渲染路径）扩展 `responseBody(url)` 按 URL 分派（契约文档 §3 的 N13 规则）、页面级测试 + 覆盖率（阈值 88）。
- **不引入任何新前端依赖**（避免锁文件变动与闸门风险）。

### 4.5 被否决的候选（保留以免重复讨论）

| 候选 | 否决理由 |
| --- | --- |
| 用 `RestTemplate` + 自写 DTO | 与 `HttpClient` 等价；`HttpClient` 是 JDK 自带、无额外依赖，但需手写 JSON 解析（可用 Jackson，已是 Boot web 传递依赖）——实施时定稿 |
| 直接在 Docker 里跑 Ollama | 内存仅 3.826 GiB（**虽可调大，但会挤压宿主 8 GiB**），且 **macOS 容器无法访问 Apple GPU（无 Metal 直通）→ 容器内只能 CPU 推理、比原生慢一个量级**（scout §C11；P1-4） |
| 引入 Spring AI 但保持 Boot 2.7 | 依赖基线不允许（scout §A5） |
| 首版就上向量库 | 属 Option B/C；本方案要求零依赖 |
| 前端直连 `localhost:11434` | 暴露模型端点给浏览器、绕过认证与审计；且 CORS 未配置（契约文档 §9）→ **必须经后端代理** |

---

## 5. 里程碑与工作量（**估算**）

| 里程碑 | 内容 | 估算 |
| --- | --- | --- |
| **S0** 环境 | 安装 Ollama、拉 3B 模型、`curl` 验证 OpenAI 兼容端点、记录磁盘占用 | 0.5–2 天（含下载等待） |
| **S1** 后端最小链路 | 配置 + `HttpClient` 客户端 + 1 个端点 + 超时/错误映射 + 单测（覆盖率闸门内） | 2–3 天 |
| **S2** 检索/prompt | 候选产出（MySQL 全文或结构化卡片）+ prompt 模板 + 严格输出校验 | 1–2 天 |
| **S3** 前端入口 | `/playground` 或首页入口 + 状态机 + 测试 | 1–2 天 |
| **S4** 实测与报告 | §6 的测量矩阵（3B 与 7B/8B 各一轮）+ spike 报告 | 2–3 天 |
| **S5** 结论与建议 | 选型建议（模型/量化/上下文/超时）+ 是否进入 Option A/B 的建议 | 0.5 天 |
| **合计** | | **1–2 周**（估算） |

**人力假设**：1 名开发者 + 主 agent 工作流（scout/oracle/worker/reviewer）。工作量**未含**模型下载等待与磁盘清理时间。

---

## 6. 验收与评测（spike 的报告模板）

> 本方案的"验收"不是功能完备，而是**拿到可信数据**。以下均为**待实测**，报告必须逐项填写并注明测量方法。

| 测量项 | 测量方法（草案） | 结果 |
| --- | --- | --- |
| 模型可加载性（3B / 7B / 8B 4-bit） | `ollama run <model> "hi"` 是否成功；`ollama ps` 内存占用 | 待实测 |
| 生成速度（tokens/s） | 首选：固定 prompt、固定输出长度上限，读取运行时返回的生成统计（如 Ollama 的 eval 计数/时长字段，**字段名实施时确认**）；**兜底法（P2-6）**：若字段名/语义无法确认，则由**应用侧计时**（`System.nanoTime()` 包住整次请求）+ **固定输出长度**（要求模型生成固定字数）反推 tokens/s，并**同时记录两种口径** | 待实测 |
| 首 token 延迟 | 计时首个流式 chunk（若用流式）或非流式总时长 | 待实测 |
| 端到端延迟（经 Spring 端点） | 应用侧计时日志；P50/P95 各 N≥20 次 | 待实测 |
| 可用上下文长度 | 逐步增大 prompt 直至失败/OOM，记录最大可用值 | 待实测 |
| JSON 输出合规率 | 20 条 NL 查询 → 校验 JSON 是否可解析且字段合法 | 待实测 |
| NL→条件正确率（粗略） | 人工判定 20 条中条件是否合理 | 待实测 |
| 幻觉率（粗略） | 20 条问题中"无依据却给出确定答案"的比例 | 待实测 |
| 内存余量 | 推理时 `vm_stat`/活动监视器读数 | 待实测 |
| 磁盘占用 | 模型目录 `du -sh` | 待实测 |

**功能验收（草案）**：

1. `GET /api/boot/ai/health` 在模型可用时返回成功，在模型不可用时返回明确失败（可测）。
2. `POST /api/boot/ai/ask` 在模型可用时返回答案 JSON；超时/连接失败时返回约定形状的错误且**不崩溃**。
3. `fabflix.ai.enabled=false` 时端点返回"未启用"，且既有 127 后端测试 / 195 前端测试 / JaCoCo 85% / 前端 88% 闸门**不受影响**。
4. 前端入口在 loading/error/empty 三态下都有可测表现（含 axe 断言）。
5. spike 报告逐项填满（§6 表 + §7 决策建议）。

---

## 7. 风险与回退

| 风险 | 可能性 | 影响 | 缓解 / 回退 |
| --- | --- | --- | --- |
| **实测证明 8 GiB 上质量不可接受** | 中高（估算） | 方案失去意义 | 这是 spike 的**正常结论**；回退到"外部 API 兜底"或"缩小到检索式功能"，并把结论写入报告 |
| 模型下载占用磁盘过多（22 GiB 上限） | 中 | 无空间 | 只用 3B 起步；测完删除不再使用的模型；**不**同时保留多个大模型 |
| 实时推理导致本机发烫/降频，延迟不可接受 | 中 | 体验差 | 记录实测；给出"仅演示用"的定位；限制并发为 1 |
| 手写 HTTP 代码被 Option B 替换（**返工**） | 高（确定） | 1–2 周实现可能被丢弃 | 把 prompt 模板/解析/校验写成**可复用纯函数**（Option B 迁移时保留），端点契约与错误语义**提前与 B 对齐** |
| 非确定性输出导致测试 flaky | 中 | CI 不稳 | 后端测试**全部 mock** 模型；真实调用只在手工脚本；不把真实调用放进 CI |
| prompt injection / 越权 | 中 | 安全 | §4.3 的分区 + 严格输出校验；端点受现有 filter 保护；只读（无写操作） |
| Java 11 `HttpClient` 的超时/连接池陷阱 | 低中 | 请求挂死 | 显式设置 `connectTimeout` 与 `HttpRequest.timeout`；单测覆盖超时/连接拒绝/畸形 JSON 路径——用 **JDK 自带的 `com.sun.net.httpserver.HttpServer`** 起本地 stub（**不引入新测试依赖**，见 §9） |
| 覆盖率闸门被新增类打破 | 中 | CI 红 | 新增类必须有单测（**JDK 自带 `com.sun.net.httpserver.HttpServer`** 起的本地 stub + 纯函数测试）；**不**扩 JaCoCo excludes |
| 违反守卫测试（若不小心引入 jakarta） | 低 | CI 红 | 本方案完全不碰 servlet 命名空间；**不新增 servlet** |

### 7.1 何时应该放弃这条路

满足以下**任一**条件，即应停止 Option 0 并转入"Option A + 外部 API 兜底"或纯检索方案，并在 spike 报告中明确记录原因：

1. 3B/7B/8B 4-bit 中**没有**任何模型能在 8 GiB 内稳定运行（频繁 OOM 或被系统换页拖垮）；
2. 端到端延迟（抖音级交互体验所需）**远超**可接受阈值（**p95 目标值仍待实测后定**，见总览 D8：已确认「先实测后定」，即**当前无固定阈值**）；
3. JSON/结构化输出的合规率**过低**以至无法可靠地支撑 NL→查询转换（**已确认阈值（2026-10-19，总览 D8）= schema 合规率 ≥95%**）；
4. 人工抽检显示幻觉率在使用场景下不可接受，且无法通过 prompt/校验缓解；
5. 磁盘空间不足以容纳所需模型。

---

## 8. 简历价值

| 要点 | 可展示证据 |
| --- | --- |
| "在 8 GiB Apple Silicon 上评估本地 4-bit LLM 的可行边界" | spike 报告中的**实测** tokens/s、内存、上下文上限 |
| "用 JDK `HttpClient` 直接对接 OpenAI 兼容端点，无第三方 SDK" | 代码 + 契约测试 |
| "把非确定性 LLM 调用隔离在 CI 之外" | mock 测试 + 手工脚本 |
| 局限 | 无框架、无向量检索、无评测体系 → **简历价值有限**，应作为 Option A/B 的铺垫而非终点 |

---

## 9. 用户决策（oracle 审查已完成；**用户已裁定，2026-10-19**）

| # | 事项 | 建议 | **用户决定（2026-10-19）** |
| --- | --- | --- | --- |
| O0-1 | 是否值得做（vs 直接 Option A） | 建议：**仅在"用户希望先看效果"或"对 8 GiB 可行性没底"时做** | **未采纳为目标路线**：用户已选 **A → B → C**（每周 >10h）；Option 0 **未逐条否决**，作为**可选降风险手段**保留，先做与否待主 agent/用户确认 |
| O0-2 | 前端入口选 `/playground` 还是首页搜索 | 建议：**`/playground`** | **未裁定**（因 O0 未入路线） |
| O0-3 | 模型默认选择 | 建议：**3B 起步** | **未裁定**（总览 D-L3/L2 也未逐条裁定）；暂按建议，**以实测为准** |
| ~~O0-4~~ | ~~是否允许为超时测试引入测试依赖~~ | **已按 oracle P2-5 关闭** | 不变：**无需新增依赖**（JDK 自带 `HttpServer`） |
| O0-5 | 是否允许安装 Ollama + 下载模型（**本任务不执行**） | 需用户明确授权 + 磁盘确认 | **✅ 已授权（仅限实施阶段，2026-10-19）**（同部署专项 D-L1）；**本次仍不安装** |
| O0-6 | 质量下限阈值 | 需用户给数（总览 D8） | **✅ 已确认（用户，2026-10-19）**（总览 §11 D8）：目录 top-5 ≥80% / RAG 引用正确率 ≥95% / NL→JSON schema 合规率 ≥95% / **p95 先实测后定**；**阈值未因本次确认而变为实测结果**（本方案仍只做手工/脚本粗略统计） |

**未确认事项**：Ollama 响应中用于计算 tokens/s 的**确切字段名与语义**（若无法确认则用 §6 的兜底测量法）；本机可加载的最大模型（实测）；`HttpClient` 在流式响应下的实现细节；**spike 报告的最终存放路径**（建议 `docs/logs/`）；均标注为**待实测/待实施时确认**。

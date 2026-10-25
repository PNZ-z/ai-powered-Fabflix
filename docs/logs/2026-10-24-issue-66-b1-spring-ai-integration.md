# 工作流日志 — Issue #66（B1：Spring AI 接入本地 Ollama —— 依赖 + 配置 + 可达性健康端点）

> **本文件的定位**：B1（issue [#66](https://github.com/PNZ-z/ai-powered-Fabflix/issues/66)）**实现完成、三路审查修复完成、本地验证完成**之后的工作流日志，按 `AGENTS.md` §8 分成**两大分区** —— §1「修改了哪些文件及潜在问题」与 §2「新增知识点 / 重点学习对象」。
> **不声称已交付**：分支 `codex/b1-spring-ai-integration`（基线 `main` @ `df24b67`）**未提交、未推送、无 PR、无 merge commit**；GitHub issue #66 仍 **OPEN**，远端 checkbox 未勾选；**没有 CI 运行结果**（无 PR 即无 CI）。本日志不声称 PR/CI/合并已完成。
> **本日志不重跑 `mvn`**：152 tests / 0.9208 等数字来自 `worker` 报告 + **主 agent 独立复跑核实**；`docs-writer` 只做只读核对（`git diff`/`git status`/`grep`/`wc`/源码阅读）。凡未运行的检查都在 §1.3 / §2 明确标为未验证。
> **B1 不含任何 AI 功能**：没有 chat 调用、没有 embedding 调用、没有 RAG、没有向量库、没有前端。只做接入与可观测。

## 元数据

| 项目 | 值 |
| --- | --- |
| 日期（日志写入） | **2026-10-24** |
| GitHub issue | **[#66](https://github.com/PNZ-z/ai-powered-Fabflix/issues/66)** `[B1] Integrate Spring AI with the local Ollama endpoint` —— **OPEN**（milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)，无 labels）。**未关闭、checkbox 未勾选** |
| 分支 | **`codex/b1-spring-ai-integration`** |
| 基线 commit | **`df24b67`** = `Merge pull request #65 from PNZ-z/codex/fix-movielist-scroll-test`（= 当前 `main`） |
| 提交 / 推送 / PR | **均未发生**：`git status --short` 显示改动为未暂存/未跟踪（`M pom.xml`、`M src/main/resources/application.properties`、`M .env.example`、`?? src/main/java/com/fabflix/boot/ai/`、`?? src/test/java/com/fabflix/boot/ai/`、`?? docs/plans/b1-spring-ai-integration.md`、`?? docs/issues/issue-66-b1-spring-ai-integration.md`、`?? docs/logs/2026-10-24-issue-66-b1-spring-ai-integration.md`、`?? .idea/`）；**无 commit、无 push、无 PR、无 merge commit** |
| 关联计划 | `docs/plans/b1-spring-ai-integration.md`（**§11 实现结果**、§12 状态表为本次回填） |
| 关联 issue 记录 | `docs/issues/issue-66-b1-spring-ai-integration.md`（Internal tracking 已回填；**`## GitHub issue body` 分区逐字未改**，仍 14271 字符、与远端一致） |
| 变更规模（实际） | **14 个文件**（以实际为准）：产码 **5** + 测试 **3** + 构建/配置 **3** + 文档 **3**（本日志 + 计划 + issue 记录）。产码/测试**行数**：产码 210 行、测试 343 行（`wc -l`） |
| 验证证据来源 | `worker` 报告 + **主 agent 独立复跑**：`mvn --batch-mode --no-transfer-progress verify`（JDK 21）→ 152 tests / 0 failures / 0 errors；JaCoCo 90 missed / 1047 covered / 1137 total = **0.9208**；新增产码 **N=59, U=0**；`excludes` 仍 8；手工 M1–M4。**`docs-writer` 未重跑任何构建/测试** |
| 结论 | 实现与已批准计划一致（差异均已获批/记录，见 §1.2）；本地闸门全绿；**仍未交付（无 PR/CI/合并）**；残余风险与未验证项见 §1.3 |

---

## 1. 修改了哪些文件及潜在问题

### 1.1 实际改动（逐文件）

> 第 1–11 行是 `worker` 的实现改动（`docs/` 之外，`docs-writer` **未编辑**，只记录事实）；第 12–14 行是本次 `docs-writer` 的文档改动（均在 `docs/` 内）。

| # | 文件 | 改了什么（目的） |
| --- | --- | --- |
| 1 | `pom.xml` | `<properties>` 加 **`spring-ai.version=1.1.8`**；`<dependencyManagement>` 在 **`spring-boot-dependencies` 之后** import `spring-ai-bom`（`<type>pom</type>` / `<scope>import</scope>`）；`<dependencies>` 加 **`spring-ai-starter-model-ollama`（无 `<version>`）**。目的：版本单一来源（升一处）+ 由 BOM 管 starter 版本 + Boot 优先级在前 |
| 2 | `src/main/resources/application.properties` | 新增 **4 项 `${ENV:default}`**：`spring.ai.ollama.base-url`（`FABFLIX_AI_OLLAMA_BASE_URL`，默认 `http://localhost:11434`）、`spring.ai.ollama.chat.options.model`（`FABFLIX_AI_CHAT_MODEL`，默认 `qwen2.5:7b`）、`spring.ai.ollama.embedding.options.model`（`FABFLIX_AI_EMBEDDING_MODEL`，默认 `bge-m3`）、`fabflix.ai.enabled`（`FABFLIX_AI_ENABLED`，默认 `false`）。目的：配置真值单一（连接参数只在 `spring.ai.*`，开关只在 `fabflix.ai.*`）；仓库内无真实内网地址 |
| 3 | `.env.example` | 4 个占位（`localhost` + 两个模型名 + `false`），无真实 IP/密钥。目的：权威占位表（README 按 P-B1-8 = B 不更新） |
| 4 | `src/main/java/com/fabflix/boot/ai/AiHealthController.java`（22 行） | `GET /api/boot/ai/health`，委托 `AiHealthService`。落在 `/api/boot/` 前缀且不在 filter 白名单 → **零 filter 改动**即受登录保护 |
| 5 | `src/main/java/com/fabflix/boot/ai/AiHealthService.java`（60 行） | 组装 `enabled/chatModel/embeddingModel/reachable/status`；`enabled=false` 时**不调用 probe**；`RuntimeException` 全捕获 → `unreachable`（**永不 5xx**）。**`PROBE_TIMEOUT = Duration.ofSeconds(2)` 是唯一超时真值，经 `isReachable(Duration)` 方法参数传入**；开关用 `String` + **仅 `true`（忽略大小写）启用**（F3）；4 处 `@Value` **不写默认值**（F2，缺失即 fail-fast） |
| 6 | `src/main/java/com/fabflix/boot/ai/OllamaReachabilityProbe.java`（16 行） | 探测接口：`boolean isReachable(Duration timeout)`，Javadoc 说明"整体 deadline、绝不抛异常" |
| 7 | `src/main/java/com/fabflix/boot/ai/HttpOllamaReachabilityProbe.java`（89 行） | JDK `HttpClient` 实现：只 `GET {baseUrl}/api/version`；`HttpRequest.timeout(t)`（**终止 exchange**，F1）+ `sendAsync(...).orTimeout(t)`（**限时返回**）共用同一参数；`HttpClient` **不设 connectTimeout**（避免与整体预算叠成 2×）；超时/拒绝/非 2xx/非法 URL 全部 → `false`；中断分支恢复中断位；日志只记异常类型不记 URL（避免回显端点） |
| 8 | `src/main/java/com/fabflix/boot/ai/AiHealthResponse.java`（23 行） | `record`，5 字段；Javadoc 写明 `status` 是 **health 端点家族字段**（同族 `/api/boot/health` 的 `status:"UP"`）、`reachable ≠ ready`、禁用时 `reachable=false` = **未探测**（F6）、不回显 base URL |
| 9 | `src/test/java/com/fabflix/boot/ai/AiHealthControllerTest.java`（97 行，**5 用例**） | `@WebMvcTest(AiHealthController.class)` + `@MockitoBean AiHealthService`：T1 匿名 401（真实 `CustomerAuthenticationFilter` 生效）+ `verifyNoInteractions(service)`；T2 disabled；T3 ok；T4 unreachable 仍 200；T5 **键集恰好 5 个**且无 `baseUrl/host/port/token` |
| 10 | `src/test/java/com/fabflix/boot/ai/AiHealthServiceTest.java`（98 行，**7 用例**） | 纯单测：S1 disabled（`verify(probe, never())`）；**S1b 空值开关**（F3 回归）；**S1c `TRUE` 忽略大小写**；S2 ok 且 `verify(probe).isReachable(Duration.ofSeconds(2))`；S3 probe false；S4 probe 抛异常；S5 模型名原样透出 |
| 11 | `src/test/java/com/fabflix/boot/ai/HttpOllamaReachabilityProbeTest.java`（148 行，**6 用例**） | `com.sun.net.httpserver.HttpServer` 在 `127.0.0.1:0` 起 stub（记录 path + method）：P1 只有 1 个请求、`/api/version`、**方法 `GET`**、六个禁用路径逐个断言不存在；P2 500 → false；P3 `sleep(600)` + 150ms deadline → 断言 **`100 ≤ elapsed < 1000`**；P4 关闭端口；P5 非法/空/`null` URL；P6 中断位保留。**无外网、无 Ollama、无固定端口** |
| 12 | `docs/plans/b1-spring-ai-integration.md` | **本次回填**：状态改「🟢 已批准 / 已实现（本地验证通过；PR 待创建）」+ 明确"未提交/未推送/无 PR/未合并"；修正 **§2.1 S4** 与 **§9.1 P-B1-4** 的超时措辞（`AiHealthService.PROBE_TIMEOUT` 2s → **方法参数**传入，探测类不持有第二个超时）；修正 **§4.1** 文件计数（**产码 5 / 总 14**，新增 `HttpOllamaReachabilityProbe` 行、测试类改名）；**§6.2 P3/P1** 改实测（600ms sleep、`100 ≤ elapsed < 1000`、probe ~153ms、test case ~0.775s）+ 新增 P6；§6.3 补 S1b/S1c（7 用例）；§6.4 补实测覆盖率；§8.2 风险表补 M1/M3/M4/F3 证据；§9.2 更新 U 项状态；**新增 §11「实现结果（2026-10-24）」**（152 tests、0.9208、N=59/U=0、M1–M4、F1–F6、AC 对照）；原 §11 顺延为 **§12 状态与后续流程**（worker ✅ / 三路审查 ✅ / 修复 ✅ / 文档 ✅ / 提交·PR·CI·合并 ⏳） |
| 13 | `docs/issues/issue-66-b1-spring-ai-integration.md` | **本次回填**（只动头部说明 + `Internal tracking` + 文末新小节）：状态改「实现完成，PR 待创建 / 未合并」；新增 4 行（实现与验证状态、M1–M4、审查修复 F1–F6、仍未验证项）；新增「实现结果回填（2026-10-24）」小节（含范围偏离记录）。**`## GitHub issue body` 分区逐字未改**（复算 = **14271 字符**，与发布时一致）；**未勾选远端 checkbox** |
| 14 | `docs/logs/2026-10-24-issue-66-b1-spring-ai-integration.md` | **本文件**：两大分区（AGENTS §8 要求），元数据含"未提交/无 PR/无 CI"的明确声明 |

**`docs-writer` 本次实际运行的只读核对命令**（可复跑）：`git status --short` / `git branch --show-current` / `git log --oneline -3` / `git diff pom.xml src/main/resources/application.properties .env.example` / `wc -l src/main/java/com/fabflix/boot/ai/*.java src/test/java/com/fabflix/boot/ai/*.java` / 逐文件 `cat` 阅读 8 个新类 / `grep -n` 校对计划与 issue 文档的过时措辞 / 用 `python3` 复算 `## GitHub issue body` 分区字符数（14271）。

### 1.2 与已批准计划的偏离（均已获批或已记录，未扩大范围）

| 偏离 | 说明 | 裁定/记录 |
| --- | --- | --- |
| probe **接口 + 实现拆为 2 个文件** | 计划 §4.1 原先把 `OllamaReachabilityProbe` 写成 1 行（接口/实现合一）；实际是 `OllamaReachabilityProbe`（接口）+ `HttpOllamaReachabilityProbe`（实现）→ 产码 4 → **5**，文件总数 13 → **14** | 属**在获批范围内的收敛**（举证：可 mock 的接口 + 可替换实现），已回填 §4.1 |
| 测试类名 `OllamaReachabilityProbeTest` → **`HttpOllamaReachabilityProbeTest`** | 与实现类同名 | 已回填 §4.1/§6.2 |
| `.enabled` 绑定类型 boolean → **String（仅 `true` 启用）** | 审查 F3：`FABFLIX_AI_ENABLED=` 空值会让 boolean 绑定**启动失败**（已实测复现）；改 String 后空值/其他 = 关闭 | 外部契约不变（响应 `enabled` 仍是 boolean）；有 S1b 回归测试 + M2 实测 |
| 测试 P3 断言由 `< 500ms` 改为 **`100 ≤ elapsed < 1000`**，stub `sleep(600)` | 原上界过紧、且缺"至少等到 deadline"的下界；下界能证明不是提前放弃 | 属**测试强化**（F 类修复），无生产行为变化 |
| P6（中断）**保留** | 审查曾建议合并进 P3 | **主 agent 裁定保留** |

### 1.3 仍存在的风险、边界限制、未验证项与后续事项

> 以下均为**实施后仍然存在**的项；没有"已修复即消失"的假设。

1. **真实端点上"只请求 `/api/version`"未被证明**：AC-B1-4 只在 **loopback stub** 上断言（请求数 = 1、路径 = `/api/version`、方法 = `GET`、六个禁用路径不存在）。**未对局域网端点抓包**，因此"Spring AI 的 starter 在运行期不会私下请求别的 Ollama 路径"只有代码级依据（B1 不使用 `OllamaChatModel`/`OllamaEmbeddingModel`）。
2. **keep-alive 生命周期未测**：M4 采样到 `HttpClient` 的 `ESTABLISHED` 连接，判定为连接复用**非泄漏**，但没有测何时回收、长时间空转是否累积连接。`HttpClient` 由 `HttpOllamaReachabilityProbe` 单实例持有（线程安全），未实现显式关闭。
3. **U-B1-5（starter 启动/内存开销）仍未量化**：只知道无 AI env 启动 **0.992 s** 可接受，**没有与基线启动时间对比**，也未量依赖体积/堆占用。
4. **U-B1-6（compose 透传）未做**：`compose.yaml` 未改。容器内 `localhost:11434` 指向容器自身，实际部署需要 `host.docker.internal` 或宿主机 IP —— 属部署决策，未在 B1 处理。
5. **2s 整体 deadline 的代价与无缓存**：`enabled=true` 且端点慢/半通时，请求可能占用一个请求线程最长 2s；**无缓存**（每次请求都重新探测，刻意避免"缓存掩盖故障"）。演示可用，不适合高频轮询。
6. **`reachable ≠ ready`**：`status=ok` 只证明 `GET /api/version` 返回 2xx；**不证明** `qwen2.5:7b`/`bge-m3` 已安装、已加载、显存驻留或 embedding 维度 = 1024。模型就绪语义留给 B5/M4.x。
7. **局域网端点无鉴权**（环境既有）：响应刻意不回显 `baseUrl`/`host`/`port`，日志也只记异常类型；但端点本身仍无认证，加鉴权属部署专项，B1 不改变安全边界。
8. **macOS「本地网络」权限是环境事实，不是代码保证**：M4 实测本机 JVM **已获授权**（`ok` / 34 ms）；换机器/换终端仍可能表现为 `unreachable`。文档不得据此声称"任何环境都能连上"。
9. **依赖体积与传递依赖**：starter 带来 `spring-ai-ollama`、WebFlux、Jackson 等传递依赖（与 Boot 3.5.15 管理版本一致），**依赖树逐行输出本轮未回传**，"无第二个 Boot 版本"目前由 oracle 的依赖事实 + `mvn verify` 成功支撑（AC-B1-1 标为 ⚠️ 部分）。
10. **配置 fail-fast 的代价（F2 副作用）**：4 处 `@Value` 默认值删除后，`spring.ai.ollama.chat.options.model` 等键若被整体删除，应用会**启动失败**而不是静默取代码默认值。这是刻意的（单一真值来源），但"删键"这种误操作现在会表现为启动失败，需在排障文档里说明。
11. **P3/P6 的计时敏感测试**：断言 `100 ≤ elapsed < 1000` 依赖 stub `sleep(600)` 与 150ms deadline；在极端负载/慢 CI 上仍可能抖动（本次连续 5 次稳定，probe 类每例 ~0.79–0.82 s，其中 600ms 是 stub teardown 的预期开销）。
12. **`AC-B-12` 仍未闭环**：B1 **不**闭环它（第二 embedding 候选、中→英跨语言用例、21,087 卡片 embedding 成本、`docs/` 切分块数、p95 目标、NL→JSON 合规率全部在范围外）。它是 **B-① / B-② 的准入前置**，需在 B-① 之前单独另建 issue 完成。
13. **流程侧未完成项**：**无 PR、无 CI 结果、未合并、#66 仍 OPEN 且远端 checkbox 未勾选**；`docs/plans/ai-roadmap-overview.md` 的执行状态**未回填**（刻意留给 PR 合并后）。
14. **文档断言的可复核性**：计划 §11.5 的 AC 对照是**本地口径**；其中 AC-B1-1（`dependency:tree` 细节）与 AC-B1-9（PR CI）标为 **⚠️ 部分**，不得在 PR 描述里写成"已全部满足"。

**未发现其他已知遗留问题**：本次改动无数据迁移、无 schema 变更、无破坏性接口变更；`CustomerAuthenticationFilter`、`ci.yml`、`Dockerfile`、`compose.yaml`、`frontend/**`、`README.md`、`docs/architecture/**`、B0 日志与历史日志**均零改动**（`git status` 可核）。

---

## 2. 新增知识点 / 重点学习对象

> **"本次没有新增知识点"不适用** —— 本次确实引入了以下新知识面（Maven 依赖管理、Spring 配置绑定、JDK HTTP 客户端语义、覆盖率算术、测试切片与计时断言、运维取证）。

### 2.1 构建与依赖管理（Maven）

- **BOM（`<dependencyManagement>` + `<type>pom</type>` + `<scope>import</scope>`）与"starter 不写版本"** —— `pom.xml`。
  含义：BOM 只提供版本决策表，不引入依赖；`import` 把它的 `dependencyManagement` 合入本项目，于是 `spring-ai-starter-model-ollama` 可以**省略 `<version>`**。
  学习价值：这是"版本单一来源"的机制基础；升级只需改 `spring-ai.version` 属性。B1 还验证了 **两个 BOM 的工件集合不重叠**（Boot 不管理 `org.springframework.ai:*`，Spring AI BOM 不管理 `org.springframework.boot:*`），所以导入顺序在解析上无影响，但**约定 Boot 在前**以保持"平台基线优先"。
- **BOM 对齐（Boot 3.5.15 ↔ Spring AI 1.1.8）** —— `pom.xml`。
  `spring-ai-starter-model-ollama:1.1.8` 自身依赖 `spring-boot-starter:3.5.15`，与项目 `spring-boot.version` 完全一致 → 不会引入第二个 Boot 版本。学习价值：接新生态库时，**先核对它钉的 Boot 版本是否与项目一致**，这比事后排查"莫名其妙的自动配置失败"便宜得多。

### 2.2 Spring：配置绑定与宽松解析

- **`@Value` 的默认值 = 第二份配置真值** —— `AiHealthService`（F2 修复）。
  含义：`@Value("${k:default}")` 里的 default 会让"配置文件漏了这个键"变成静默成功；删掉后缺键即 fail-fast。
  学习价值：默认值应只存在于**一个地方**（此项目 = `application.properties` 的 `${ENV:default}`），代码里再抄一份等于断言两份真值永远一致。
- **boolean 绑定的空值陷阱 + 宽松解析** —— `AiHealthService`（F3 修复）、`AiHealthServiceTest` S1b/S1c。
  含义：环境变量 `FABFLIX_AI_ENABLED=`（存在但为空）经 `${ENV:default}` 解析成**空字符串**；绑定到 `boolean` 时 Spring 无法转换 → **启动失败**（本次已实测复现）。改为 `String` 后在代码里判定 `"true".equalsIgnoreCase(value.trim())`，空值/其他值一律视为关闭。
  学习价值：环境变量驱动开关时，"未设置"和"设为空"是两种输入；用布尔类型会让第二种变成生产事故。宽松解析 + 明确语义（只有 `true` 才开）比"猜用户意图"安全。

### 2.3 Spring AI：模型键与"可达 ≠ 就绪"

- **`.options.model` 与 `.model` 同源** —— `application.properties`、`AiHealthService` 的 `@Value`。
  `OllamaChatProperties.getModel()` **委托** `options.getModel()`（embedding 同理），因此两种键形状解析到同一值。学习价值：端点回显模型名时"读哪个键"不是风格问题，而是"是否与真正生效值同源"的问题；确认同源后就不必为它加 `ApplicationContextRunner` 绑定测试（P-B1-5 = B）。
- **Ollama `/api/version` 是最轻的探活端点；`reachable ≠ ready`** —— `HttpOllamaReachabilityProbe`、`AiHealthResponse`。
  含义：`/api/chat`、`/api/generate`、`/api/embed` 会**加载模型**（B0 实测冷启动 7.64 s）、`/api/ps`、`/api/tags` 会**暴露清单**；只有 `/api/version` 既不动 GPU 也不泄露库存。但 `/api/version` 2xx 只说明"HTTP 服务在应答"，不说明模型已装/已加载。
  学习价值：健康检查的语义边界要写死在字段注释和文档里，否则下游里程碑会把 `ok` 当成"模型可服务"。

### 2.4 Java：JDK `HttpClient` 的超时语义（本次最值得记住的一条）

- **`orTimeout` 不取消请求，`HttpRequest.timeout` 才终止 exchange** —— `HttpOllamaReachabilityProbe`（F1 修复）。
  含义：`sendAsync(...).orTimeout(t)` 只让**等待方**在 t 后收到 `TimeoutException`，底层 HTTP exchange（connect/读写）仍可能继续跑；`HttpRequest.newBuilder(uri).timeout(t)` 才会在预算耗尽时**终止该 exchange**。两者用**同一个** `timeout` 值 ⇒ 对外＝单一整体 deadline，对内＝超时后不留后台连接。
  学习价值：只写 `orTimeout` 会出现"端点看起来 2s 超时了，但连接还在"的隐蔽资源问题；只写 `HttpRequest.timeout` 则拿不到确定的返回时机。**两者同值组合**才是"整体 deadline + 真取消"。另外 `HttpClient` **不设 connectTimeout** 也是刻意的：避免 connect 与 request 各付一份预算（最坏 2×）。
- **`InterruptedException` 要恢复中断位** —— 同文件。
  学习价值：捕获后 `Thread.currentThread().interrupt()`，否则线程池的中断信号被吞掉；P6 专门断言这一点。

### 2.5 测试：覆盖率算术、切片与计时断言

- **`U ≤ 0.1N + 17.8` 的来源** —— 计划 §6.4 / issue AC-8。
  含义：基线 `988 covered / 1078 total`，闸门 0.90；新增产码 `N` 行、其中未覆盖 `U` 行时要求 `(988 + N − U) / (1078 + N) ≥ 0.90`，展开即 `U ≤ 0.1N + 17.8`。
  学习价值：**"新增产码可以有多少行不覆盖"不是拍脑袋的数字**，而是由分母增长决定的；本次实测 `N=59, U=0`（1137 total / 1047 covered / 90 missed = 0.9208），余量远大于闸门要求。也提醒：不能靠扩 `excludes` 绕闸门（仍 8 项）。
- **`@WebMvcTest` 会注册 `Filter` bean** —— `AiHealthControllerTest`。
  含义：切片不只加载 controller，`CustomerAuthenticationFilter` 也在内，所以匿名请求能自然地断言 **401**（本测试反过来说明"零 filter 改动即得认证边界"）。同时 `@MockitoBean` 替换 service，`verifyNoInteractions` 可证"401 时根本没进业务层"。
- **计时敏感测试的写法** —— `HttpOllamaReachabilityProbeTest` P3/P6。
  含义：只断言 `< 上界` 不能排除"提前放弃"，所以加**下界**（`≥ 100ms` 相对 150ms deadline）；stub 必须比 deadline 睡得久（`sleep(600)`）才能稳定触发超时；代价是 **test case 时间被 teardown 拉长**（~0.775 s，其中 600 ms 是预期开销）。
  学习价值：超时类断言的判据是"**区间**"，不是单边界。

### 2.6 端点语义：health 端点家族字段

- **`status` 属于 health 端点家族，不是业务 `success/fail`** —— `AiHealthResponse`、`/api/boot/health`。
  含义：既有 `/api/boot/health` 返回 `{"status":"UP"}`；新端点沿用同一"族"语义（`disabled|ok|unreachable`），因此 `unreachable` **不是**业务失败，HTTP 恒 **200**。
  学习价值：区分"传输层/健康语义"与"业务结果语义"，可以避免把 5xx 当成唯一错误表达方式，也避免前端把健康探针当业务失败处理。

### 2.7 运维取证：`lsof` 的 AND 语义

- **`lsof -nP -a -p <pid> -iTCP` 里的 `-a` 是"必须同时满足"** —— M1 取证。
  含义：`lsof -p <pid> -iTCP` 默认是**或**关系（列出该进程的所有文件 + 所有 TCP），不加 `-a` 会把无关行也算进来，从而把"到底有没有连 11434"变成一个假的 0/非 0 结论。加 `-a` 后是 `(pid AND TCP)`，再 `grep -c 11434` 得到的 0 才是"该进程没有到 11434 的连接"。
  学习价值：**"日志里没报错"是弱证据，"socket 层没有连接"才是强证据**——这正是 AC-B1-10「启动期无出站」的取证方法。

---

## 3. 结论与下一步

- **实现与已批准计划一致**：范围未扩大（无 chat/embedding 调用、无 RAG、无向量库、无前端、无 filter/CI/Docker/compose 改动），5 处差异均已获批或已记录（§1.2）。
- **本地闸门全绿**：152 tests / 0 failures / 0 errors、JaCoCo **0.9208** ≥ 0.90、新增产码 N=59/U=0、`excludes` 仍 8、M1–M4 手工验证通过。
- **仍未交付**：分支未提交、未推送、**无 PR、无 CI、未合并**；#66 仍 OPEN。
- **下一步（不在本步执行）**：主 agent 提交并推送 → `github-operator` 按 issue 文档 + 本日志开 PR（PR 描述需保留 §1.3 的未验证项，特别是"真实端点未抓包""U-B1-5/U-B1-6 未测""`AC-B-12` 未闭环"）→ CI 通过后再决定合并；路线图执行状态在**合并后**回填。

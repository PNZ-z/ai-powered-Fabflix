# Option B — Spring AI + 本地 Ollama + RAG（**推荐方案**）

> **文档状态：✅ 已批准（用户，2026-10-19）。**（批准前状态：已按 oracle 审查修订 / 待用户审批）
> 用户已批准与本方案相关的：**D3 语料范围 (a)**、**D4 `SimpleVectorStore`（内存）**、**D5 首个交付 = B-① 电影目录语义检索 + B-② 文档 RAG**、**D-embedding 多语言 embedding + 先实测中→中/中→英**、**D-语料构成 (a)**（既有 `docs/` 11 md + `README.md` + 电影结构化卡片，**不含** `ai-*.md`）、**D2/D-API 优先 (c)**（本地原生 Ollama 默认 + 外部 API 显式开关兜底）、**D-B9/P-2 README 措辞**；**阈值与夜间 job 已于 2026-10-19 确认**：**D8** = 目录 top-5 命中率 ≥80% / RAG 引用正确率 ≥95% / NL→JSON schema 合规率 ≥95% / **p95 先实测后定**；**D10** = 加**非阻塞**夜间 job（**M4.2 后接入**）；**D7** = 外部 API 默认关闭、启用时 $2/日 且 ≤200k tokens；**D-AI 端点登录** = 需登录（零行为变更）；**D-语料指纹** = 要。**这些阈值未因确认而变为已达标结果。**
> 批准是**方向批准**，不等于已开始实现：本方案的实施仍须待 **Option A 完成**（硬前置）且由主 agent 指派 `worker`；**当前未加依赖、未写源码、未装模型、未建 issue**（指本方案自身；**Option A 的 issue 已于 2026-10-19 创建 = [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)**，B/B-API/C 的 issue 仍后续再立）。**〔2026-11-08 回填：本句为 2026-10-19 的当时状态，现已部分过时 —— Option A 已完成，B1（Spring AI 接入）也已于 2026-11-08 合并（PR #67 / `aea0477`，issue #66 CLOSED）：本方案已加 `spring-ai-bom` + Ollama starter 依赖与一个可达性健康端点，但仍**无任何 AI 功能**；B-①/B-②/B-③ 仍未建 issue、未开始。〕**
> **修订记录（2026-10-18，oracle 只读审查后；主 agent 裁定「全部接受」）**：**P1-1** embedding 从 `nomic-embed-text` 改为**多语言候选**（`bge-m3` / `multilingual-e5-*` / `qwen3-embedding` 等，**以 Ollama 实际可用性与维度为准**），并明确"**embedding 的语言能力是本方案头号技术风险，必须先做中→中/中→英检索实测**"；**P1-2** 把"电影结构化卡片语义检索"从"备选"**升为等价交付物 B-①**（修正算术：`21087 × 768 维 × 4B ≈ 64.8 MB`，内存**不是**约束，成本是一次性 embedding 时间），文档 RAG 为 B-②，NL→结构化条件搜索为条件性 B-③；**P2-11** 配置真值来源定稿；**P2-12** AI 端点登录决策上浮到总览决策清单（D-AI 端点登录）；**P2-13** 风险表补"Ollama 未启动/启动顺序"；**P2-14** B0 spike 第一件事改为确认 `SimpleVectorStore` 在 1.1.x 的确切 artifact/类名并回填；**B4** 语料口径改为 **11 个 md / 38,755 词**（含本路线图 6 份则 17 md/54,696 词）并新增语料构成决策。审查报告：`.pi/artifacts/oracle-ai-roadmap-review.md`。
> **执行状态（2026-10-24 回填；不改写下述已批准决策与历史修订记录）**：**B0 spike（事实核对 + 本地模型/embedding 实测）已完成并通过方向验证** —— 实测在**局域网 PC 的 Ollama 端点 `http://192.168.87.48:11434`**（**不是本机 Mac 8 GiB**；`qwen2.5:7b` 的 `/api/ps` 显示 `size_vram`=`size`=4.76 GB → **100% GPU**）上完成：chat=`qwen2.5:7b` 热请求 **56–70 tok/s**（含 1 次事实性错误）、embedding=`bge-m3` **1024 维**（热请求 0.071–0.129 s、冷启动 7.64 s）、小样本中文检索 **3/3** 命中 top-1（**4 chunk / 3 query，非质量基准**）。**结论：继续 Option B（本地优先）—— 默认 chat = `qwen2.5:7b`、embedding = `bge-m3`、向量库先用 `SimpleVectorStore`。（后续：B1 已完成并合并，见上方「执行状态 2026-11-08」；当时记录的“未开始、未立项”为当时事实。）** 证据：**`docs/logs/2026-10-24-b0-local-llm-spike.md`**。**注意：B0 的完成范围不含** `SimpleVectorStore` 坐标核实（P2-14）、21087 条卡片构建耗时、`docs/` 切分块数、第二个多语言候选与中→英用例、NL→JSON 合规率 —— 上述各项**仍未完成**（该日志 §0.7/§3.2），**`AC-B-12` 因此仍未满足**。**本项目仍无任何 AI 功能、AI 端点调用或评测脚本**（B1 之后仍成立）；**注意：[2026-11-08 回填] 本句原写作“无任何 Spring AI 依赖”，已因 B1（PR #67 / `aea0477`）合并而不再成立 —— `spring-ai-bom` 与 Ollama starter 已入构建**。
> **执行状态（2026-11-08 回填；不改写下述已批准决策与历史修订记录）**：**B1（Spring AI 接入）= 已完成并已合并** —— issue [#66](https://github.com/PNZ-z/ai-powered-Fabflix/issues/66) → PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67) **MERGED**（`mergedAt` 2026-11-08T23:16:16Z，merge commit **`aea0477`**），`main` @ `aea0477` CI **SUCCESS**，**#66 已 CLOSED**（2026-11-08T23:16:17Z）。B1 交付 `spring-ai-bom` 1.1.8 + Ollama starter、4 项 env 配置、`GET /api/boot/ai/health`（登录保护 + 可达性探测）与 18 个新测试，**但不包含任何 AI 功能**。**B-①/B-②/B-③ 与 B3/B4 均未开始；`AC-B-12` 仍未满足（B1 不闭环）** —— 第二 embedding 候选、中→英、21k 卡片、`docs/` 切分、p95、NL→JSON 均未做，需作为 **B-① 前的独立步骤（B1.5）另建 issue** 完成。详细证据：`docs/plans/b1-spring-ai-integration.md`、`docs/logs/2026-10-24-issue-66-b1-spring-ai-integration.md`。
> 本文**不声称**任何 AI 功能已实现、任何评测已运行。所有精度/延迟/内存数字均为**估算或待实测**（**2026-10-24 例外：B0 实测数字见上方执行状态与 `docs/logs/2026-10-24-b0-local-llm-spike.md`**）。
>
> 命名说明：项目约定为 `docs/plans/request-YYYY-MM-DD-<slug>.md`；本文件按任务指定使用 `ai-option-b-*.md` 临时命名，issue 创建后应重命名为 `issue-<编号>-<slug>.md`。**长期归属建议**（oracle P2-4）：本文件属一次性版本方案，留在 `docs/plans/`；其产生的**跨 issue 持续有效约束**（AI 端点契约与 `enabled` 开关约定、评测分层与非确定性 CI 边界、配置真值来源）在实施后沉淀到 `docs/architecture/ai-integration-contracts.md`（见总览 §12 P-7）。

## 元数据

| 项目 | 值 |
| --- | --- |
| 文档类型 | 实施方案（版本 B：推荐——首个 AI 交付）——**已批准（2026-10-19）**；实施排在 Option A 之后 |
| 创建日期 | 2026-10-18 |
| 工作区基线 | `main` @ `76e3d64` |
| 事实来源 | `.pi/artifacts/scout-ai-roadmap.md`（§A5/§A6/§B7–B10/§C11–C15/§D16–D19）；`docs/architecture/frontend-migration-contracts.md`；`docs/plans/ai-option-a-boot3-migration.md`；`docs/plans/ai-local-llm-deployment.md` |
| 上级文档 | `docs/plans/ai-roadmap-overview.md` |
| **硬前置** | **Option A 完成**（Boot 3.5.x + 编译 `release=17` + JDK 21 工具链 + jakarta），否则 Spring AI 1.x 依赖无法引入 |
| 其他前置 | 本地推理软件 + 模型（见部署专项）；`docs/` 语料（现成） |
| 预估工作量 | **3–5 周**（估算，含评测集与前端） |
| 状态 | **✅ 已批准（用户，2026-10-19；见下表“用户决定”列）**；实施排在 **Option A 完成之后**；**D-B8（阈值）与 D-B5（夜间 job）已于 2026-10-19 确认**（D8 阈值 = 目录 top-5 ≥80% / 引用正确率 ≥95% / NL→JSON ≥95% / p95 先实测后定；D10 = 加非阻塞夜间 job，M4.2 后接入；D-B11 语料指纹 = 要）；**阈值仍需实测验证** |
| 对应 roadmap | `README.md:5-10` 第 3 项（natural-language search / recommendations / retrieval-augmented Q&A）中的**前两项变体**；推荐系统见 Option C |
| **执行状态（2026-11-08 回填）** | **B0 = 已完成（实测通过，范围受限）；B1（Spring AI 接入）= 已完成并已合并**（PR [#67](https://github.com/PNZ-z/ai-powered-Fabflix/pull/67) MERGED，merge commit **`aea0477`**；#66 **CLOSED**；`main` CI SUCCESS；B1 **不含 AI 功能**）；**B-①/B-②/B-③/B3/B4 等全部未开始**；**`AC-B-12` 仍未满足**（第二 embedding 候选、中→英、21k 卡片、`docs/` 切分、p95、NL→JSON 未做）→ 需 **B-① 前以 B1.5 单独完成**。B0 的实测结论：继续本地优先，chat=`qwen2.5:7b` / embedding=`bge-m3`（1024 维）/ 向量库 `SimpleVectorStore`；实测环境 = **局域网 PC + Ollama（无鉴权）**，非本机 8 GiB。证据：`docs/logs/2026-10-24-b0-local-llm-spike.md`、`docs/plans/b1-spring-ai-integration.md` |

---

## 总结

Option B 是在 Option A 之上引入 **Spring AI 1.1.x**（`spring-ai-bom`）、用 **本地 Ollama** 做 chat 与 embedding（**多语言 embedding 候选**见 §4.1，P1-1）、用 **`SimpleVectorStore`（纯内存、零新增容器）** 做向量检索，交付**两个等价交付物 + 一个条件性交付物**：① **B-① 电影目录语义检索**（结构化卡片 → 向量化 → 语义 top-k，回答"有没有类似《X》的电影"这类**目录问题**，**无需外部语料、无版权处理**）；② **B-② 项目/文档知识问答 RAG**（语料 = 仓库内既有 **11 个 md / 38,755 词** + README，回答**必须带引用来源**）；③ **B-③ 自然语言搜索**（自由文本 → 结构化查询条件/排序，依赖小模型 JSON 合规率，属**条件性**）。选它是"成本 × 风险 × 简历价值"的最优交点：用上了用户点名的 Spring AI 与 RAG，**不新增任何容器**（避开 Docker Desktop 仅 3.826 GiB 的内存墙；且 macOS 容器无 GPU 直通，P1-4），两个交付物的语料**现成或可自建**，且能力可演示、可**评测**（README roadmap 第 3 项原文就是 "**evaluated**"）。为什么**不**做"电影剧情问答"：`movies` 表只有 `id/title/year/director`（加 genres/stars/rating），**没有剧情简介**，做真正的电影剧情 Q&A 需要外部语料与合规决策（属 Option C）；但**电影目录本身很适合做语义检索（B-①）**——它直接对应用户原话"把网站相关的内容和知识点存入 RAG"。主要风险：**embedding 的语言能力（头号风险，语料与提问均为中文）**、8 GiB 上 3B–8B 模型的**输出质量与中文能力**（待实测）、**非确定性测试**如何不污染 CI（方案：PR 全 mock + 夜间真实调用）、`SimpleVectorStore` **重启即失**（首版可接受，迁移路径指向 pgvector）、以及 **prompt injection / 语料注入**。回退方案已内建：模型侧可用外部 API 作**可插拔兜底**（即总览的 Option B-API），存储侧有 `SimpleVectorStore → pgvector` 的迁移路径，且 AI 开关默认关闭、不影响既有功能。工作量约 3–5 周（估算），其中评测集与前端占相当比例。

---

## 1. 目标与背景

### 1.1 目标

1. 交付 **B-① 电影目录语义检索**：把每部电影的结构化字段（`title/year/director/genres/stars/rating`）拼成"卡片文本"→ embedding → 语义 top-k；能回答"**有没有类似《X》的电影**""1990 年代的科幻片"这类**目录问题**，结果复用现有 movie list 页面展示。**无需外部语料、无版权处理**。
2. 交付 **B-② 项目/文档知识问答 RAG**：基于既有 `docs/`（**11 个 md / 38,755 词**）+ README 语料回答问题，**回答必须携带来源（文件名/章节）**，无法回答时**明确说不知道**。
3. 交付 **B-③ 自然语言搜索（条件性）**：自由文本 → 结构化查询（genre/year 范围/director/star 等）或排序意图；是否做取决于 B0 spike 实测的小模型 JSON 合规率（不达标则由 B-① 的语义检索承担同一直入口）。
4. 建立**评测**：golden set（目录检索 + 文档问答 + NL 搜索，共 20–50 条）+ 指标（检索命中、引用正确性、事实符合率、延迟、失败率），有可复现的脚本与报告。
5. 建立**非确定性测试策略**：PR 路径 100% mock；真实调用仅在夜间/手工通道。
6. 明确**降级与回退**：模型不可用、超时、embedding 失败、语料缺失时的用户可见行为。
7. 明确**embedding 选型与实测**：多语言 embedding 候选 + 中→中/中→英检索实测（**头号风险，P1-1**），并记录语料指纹。

### 1.2 背景（已证实事实）

| 事实 | 证据 |
| --- | --- |
| **前置**：Spring AI 1.x 需 Boot 3.x + Java 17+ + jakarta（1.1.8 基线 Boot 3.5.15）；本方案工具链统一 **JDK 21** | scout §A5 |
| Spring AI 有**专用 Ollama starter** `spring-ai-starter-model-ollama:1.1.8`，默认 `http://localhost:11434` | scout §C15 |
| OpenAI starter 亦可 `base-url=http://localhost:11434/v1`（Ollama 提供 OpenAI 兼容 `/v1/chat/completions`、`/v1/embeddings`） | scout §C13/§C15 |
| `SimpleVectorStore`（纯内存、零外部服务）在 Spring AI 中存在；pgvector/chroma/qdrant/redis/mariadb 等均**需新容器** | scout §C14 |
| **MySQL 8.4 无原生 vector 类型/函数** | scout §C14 |
| Docker Desktop 仅 **3.826 GiB** 内存 → 容器方案冲突 | scout §C11 |
| `docs/` 语料：**11 个 md / 38,755 词**（不含本路线图 6 份 `ai-*.md`；含则 **17 个 md / 54,696 词**）、`README.md` 891 词、`frontend/README.md` 541 词；Spring AI 提供 markdown document reader | 本轮复核（`find docs -name '*.md' -not -name 'ai-*.md' \| wc -w` = 38,755）；scout §B9 为宽口径 `docs/`（B4） |
| `movies` 字段只有 `id/title/year/director`（+ genres/stars/rating），**无剧情简介**；`movies` 约 **21087** 行 | `createtable.sql:5-10`；scout §B7/§B10 |
| 现有搜索：全文索引**仅 title 列**、BOOLEAN MODE 前缀匹配、autocomplete ≥3 字符 | `CreateFullTextIndex.java:24`、`FullTextQueryBuilder.java:1-22`、`SearchService` |
| 响应约定：业务失败 200 + `{status:"fail",message}`；400 `{errorMessage}`；401 `{errorMessage}` | 契约文档 §6 |
| `apiClient` 契约：`apiRequest(path)`、`credentials: 'include'`、204 → `null` | 契约文档 §5 |
| 覆盖率闸门：后端 JaCoCo LINE ≥ **0.90**（2026-10-20 由 0.85 收紧；现 **0.9165**，`Tests run: 134`）；前端 lines ≥ **90**（同日由 88 收紧；现 92.35%，195 用例） | `pom.xml:163`；契约文档 §7 |
| **AI 新增代码默认计入闸门**；扩 exclude 与既有政策冲突 | `README.md` 覆盖率节 |
| e2e 现状：Playwright，API 全 `page.route` mock，12 次执行 | 契约文档 §7 |
| `/playground` 是现成公共页，是最低成本挂载点 | scout §A6 |
| AD-1：网页端 session 认证，JWT 仅属 agent 平台独立 trust boundary | 契约文档 §10 |
| AD-2：单 JVM 实例 | 契约文档 §10 |

### 1.3 语料结论（本方案的关键取舍，已按 B4/P1-2 修订）

- `docs/`（**11 个 md / 38,755 词**，若含本路线图 6 份 `ai-*.md` 则 17 个 md / 54,696 词）+ README 是**受控、结构良好、事实密集**的 markdown → 适合"**项目知识问答（B-②）**"，且引用来源天然可核验（能指到文件/章节）。**`ai-*.md` 已定不纳入语料（用户批准，2026-10-19，总览 D-语料构成 (a)）**：纳入等于把"规划/批准文档自身"喂给 RAG，可能污染问答权威性（保留为风险说明）。
- 电影**剧情**无语料 → 若坚持"电影剧情语义问答"，必须引入外部简介（获取方式、版权、清洗），属 Option C 且需用户决策（总览 D3）。
- **电影"目录"语义检索（B-①）不需要任何外部语料**：语料是 `movies`/`genres_in_movies`/`stars_in_movies`/`ratings` 里已有的结构化字段 → 仍是"把网站内容存入 RAG"，但**无语料上限、无版权风险**，且直接可评测（"相似电影"有人类可判断的期望）。
- **embedding 的语言能力决定 B-② 的成败（P1-1）**：语料与提问都是中文，**必须用多语言 embedding 并先做中→中/中→英实测**，否则检索命中率（AC-B-6）会系统性偏低。
- **推论**：本方案的交付物都是**可评测的**（目录检索有期望集合；文档问答有期望来源），这是 "evaluated" 的落地方式。

---

## 2. 范围与非目标

### 2.1 In scope

1. `spring-ai-bom` import + `spring-ai-starter-model-ollama`（chat + embedding）+ `SimpleVectorStore`（**确切 artifact/类名由 B0 spike 第一件事核实并回填，P2-14**）。
2. **B-① 电影目录检索管道**：结构化卡片构造规则（字段→文本模板）→ 批量 embedding（**一次性构建，耗时实测**）→ `SimpleVectorStore` → 语义 top-k → 复用现有电影列表 UI；**支持"相似电影"与"条件式自然语言查询"两种入口**。
3. **B-② 文档 RAG 管道（ETL）**：markdown 读取 → 切分 → embedding → `SimpleVectorStore` 装载（启动时或按需）+ 检索。
4. **RAG 问答端点**：检索 top-k → 拼 prompt → 生成 → **返回答案 + 来源列表**。
5. **B-③ 自然语言搜索端点**（条件性）：NL → 结构化条件/排序（严格校验）→ 复用现有 movie list 查询路径。
6. **前端**：一个 AI 页面（优先复用 `/playground`）+ 首页/列表页的入口（可选）+ "相似电影"区块。
7. **配置与开关**：`base-url`、模型名（chat/embedding）、超时、top-k、`enabled` 开关；默认关闭或"仅本地可用时开启"（**配置真值来源见 §4.1 定稿**）。
8. **降级**：模型不可用/超时/解析失败 → 明确失败语义 + 前端可测错误态；**不影响**既有非 AI 路径。
9. **embedding 选型实测**（P1-1）：多语言候选对比 + 中→中/中→英检索命中率 + 维度/体积/构建耗时。
10. **测试与评测**：mock 化的单测/契约/页面测试/e2e；golden set 脚本 + 指标报告；夜间真实调用通道（可选）。
11. **可观测性**：延迟、失败率、检索文档数、token 估算的日志/指标。
12. 文档：本计划、`docs/architecture/` 的 AI 约定（**D-B7：仍待批准，批准时机 = 实施后**）、工作流日志。

### 2.2 Non-goals（明确排除）

| 排除项 | 理由 |
| --- | --- |
| 推荐系统 | 需协同过滤 + 离线指标，属 Option C（总览 D5 建议） |
| 电影**剧情**语料导入 / 电影剧情语义问答 | 无语料 + 合规决策（总览 D3）；**但电影目录语义检索（B-①）是 in scope** |
| 向量库容器（pgvector/qdrant/redis） | 与 Docker 3.826 GiB 冲突；`SimpleVectorStore` 足够（总览 D4） |
| Agent tools / 写操作 / 审批 / 预算 | 属 Option C |
| JWT / Spring Security | 违反 AD-1 |
| 多实例 / Spring Session | 违反 AD-2 |
| 生产部署 Ollama 容器 | 不可行（Docker 内存） |
| 改 `README.md` roadmap 措辞 | 需批准（P-2） |
| 引入前端新依赖（如 AI SDK） | 复用 `apiClient` 即可 |
| 扩 JaCoCo/vitest exclude | 与政策冲突 |
| 云端 API 作为**默认**路径 | 与"避免 token 成本"目标冲突；仅作**显式开关**兜底 |

---

## 3. 依赖与前置

| 前置 | 说明 | 状态 |
| --- | --- | --- |
| **Option A 完成**（Boot 3.5.x / `release=17` / JDK 21 / jakarta） | 硬前置（scout §A5） | **未开始**（方案已于 2026-10-19 获批准并定为首个工作流，且**已立项 = issue [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44) / milestone #3 `Phase 3 — AI-powered platform`**；**立项≠完成**，迁移实施未开始） |
| 本地推理软件 + 至少一个 chat 模型 + **一个多语言 embedding 模型** | 见 `ai-local-llm-deployment.md` | **✅ 已就绪（2026-10-24 回填）**：局域网 PC 的 Ollama `http://192.168.87.48:11434`（**非本机 Mac**；未设鉴权），已装 **`qwen2.5:7b`（4.68 GB，100% GPU）** + **`bge-m3`（1.16 GB，1024 维）**；**注意：这是外部局域网端点，不是 `localhost`** → 配置须显式 `base-url`，且 Mac 侧需「本地网络」权限（B0 日志 §0.6） |
| **embedding 语言能力实测**（中→中 / 中→英） | **头号技术风险**（P1-1）；失败则 AC-B-6 难以达标 | **部分完成（2026-10-24）**：`bge-m3` 在 **4 chunk / 3 query** 的中→中检索中 **3/3** 命中 top-1（0.6121 / 0.6990 / 0.8445）→ **方向可行**；**但仅 1 个候选、无中→英用例、样本极小** → **`AC-B-12` 仍未满足**，风险**未消除**（B0 日志 §0.5/§0.7） |
| 磁盘 | 22 GiB 可用；模型 + 缓存（估算 2–6 GB chat + 0.3–1 GB embedding） | **✅ 已接受（D9，2026-10-19）**；**实施前需清理磁盘** |
| 用户批准本方案与 D3（语料范围） | — | **✅ 已批准（2026-10-19）**：D3 (a)、D4 (a)、D5 (b)、D-embedding (a)、D-语料构成 (a) |
| 评测质量下限（golden set 阈值） | 用户给数（D8） | **✅ 已确认（用户，2026-10-19）**：目录 top-5 ≥80% / RAG 引用正确率 ≥95% / NL→JSON schema 合规率 ≥95% / **p95 延迟先实测后定**（**阈值≠已达标**） |
| CI 是否可以加"夜间 job" | 需确认（D10） | **✅ 已确认：加（非阻塞）**，**M4.2 后接入**；**CI 分钟预算仍待实施时确认**（U-B7） |
| `docs/` 语料可用 | 现成 | 已具备 |
| 响应/错误契约 | 契约文档 §6 | 已具备 |

---

## 4. 方案设计（草案）

### 4.1 依赖与配置（草案，artifact 名实施时核对）

| 组件 | 候选 artifact / 类（**实施时核对确切名称**） | 说明 |
| --- | --- | --- |
| BOM | `org.springframework.ai:spring-ai-bom:1.1.x`（import） | 统一版本 |
| Chat | `spring-ai-starter-model-ollama`（scout 已核实 1.1.8 存在） | 默认 `http://localhost:11434` |
| Embedding | 同一 Ollama starter 提供 embedding 模型；**候选（多语言，P1-1）**：`bge-m3`、`multilingual-e5-*`、`qwen3-embedding` 等（**以 Ollama 实际可用性与维度为准，实施时核对**）；亦可用 OpenAI 兼容 `/v1/embeddings` | **头号风险**：中文语料必须用多语言 embedding；**可用性/维度/检索质量待实测**（B0 要跑中→中/中→英对比） |
| 兜底（可选） | `spring-ai-starter-model-openai` + `base-url` 指向 Ollama `/v1`，或指向真实云端 | 同一抽象层切换 |
| 向量存储 | `SimpleVectorStore`（`spring-ai-vector-store` 基础 jar 内） | **零容器**；进程内、重启即失 |
| RAG | Spring AI 的 RAG 支持（Advisor / `QuestionAnswerAdvisor` / ETL pipeline 类，**1.1.x 确切类名实施时核对**） | 避免手写检索拼装 |
| Markdown 读取 | `spring-ai-markdown-document-reader`（scout §A5 的 bom 清单中出现） | 语料加载 |
| 切分 | `TokenTextSplitter`（或等价类） | 分块策略见 §4.3 |

**配置项（草案）**：

```properties
# —— 连接/模型参数：真值来源 = Spring AI 自己的前缀（P2-11 定稿）——
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=<待实测选定的 chat 模型>
spring.ai.ollama.chat.options.timeout=<待实测>
spring.ai.ollama.embedding.options.model=<待实测选定的多语言 embedding 候选>

# —— 项目自有开关/语料/阈值：真值来源 = fabflix.ai.*（不重复上面任何连接参数）——
fabflix.ai.enabled=false            # 关闭时所有 AI 端点返回明确的"未启用"失败，不影响既有功能
fabflix.ai.retrieval.top-k=4
fabflix.ai.corpus.paths=README.md,docs/**   # 语料构成见总览 D-语料构成（**已定不含 ai-*.md**，2026-10-19）
```

> **配置真值来源（P2-11 已定稿）**：**连接/模型参数一律用 Spring AI 的 `spring.ai.*`**（含 `base-url`、模型名、超时、`options.*`）；**本项目自定义 `fabflix.ai.*` 只承载「开关 / 语料 / 阈值」**（`enabled`、`corpus.paths`、`retrieval.top-k`、评测阈值），**不得出现第二份真值**。上面 `spring.ai.ollama.*` 的**子键名以安装后实测/官方文档为准（本文不猜键名）**。

### 4.2 数据流

> 下面按 **B-② 文档 RAG → B-③ 条件式搜索 → B-① 目录语义检索** 顺序给出（B-① 是本次新增的等价交付物，放在最后因为它是新增内容）；**B-① 与 B-② 共用同一套 embedding/向量库/错误处理管道**，实现上应当先抽公共管道再做两条业务流。

**B-② 文档知识 RAG 问答**

```
启动时（或首次请求时）：
  docs/**.md + README.md
     │ MarkdownDocumentReader（按文件产生 Document，携带 metadata: 文件路径/标题）
     ▼ TokenTextSplitter（chunk size / overlap 待实测）
  chunks ──► EmbeddingModel（Ollama 多语言 embedding，候选见 §4.1）
     └──► SimpleVectorStore.add(documents)

运行时：
  用户问题
     │ EmbeddingModel → 查询向量
     ▼ VectorStore.similaritySearch(topK) → 命中 chunks（含文件路径 metadata）
     │ 拼装 prompt（系统指令 + 命中片段 + 问题）
     ▼ ChatModel（本地 Ollama）
  答案 + sources[]（来自 metadata，去重）
     ▼
  HTTP 200 { status:"success", answer, sources:[{title, path, snippet}] }
```

**B-③ 自然语言搜索（条件性）**

```
用户输入："1990 年代的科幻片，按评分排序"
     │ ChatModel（低 temperature）+ 严格 JSON 输出指令
     ▼ 解析 JSON（白名单字段：genre, yearFrom, yearTo, director, star, sortBy, order）
     │ 校验失败 → 200 { status:"fail", message:"无法理解该查询" }（不猜）
     ▼ 映射到现有 MovieListRepository 查询参数（复用既有路径）
  电影列表（复用现有列表 UI）
```

**B-① 电影目录语义检索（等价交付物，已按 P1-2 从"备选"升级）**

```
构建时（一次性）：
  movies(21087) + genres_in_movies + stars_in_movies + ratings
     │ 卡片文本模板（确定性、可单测）
     │   "{title} ({year}) | director: {director} | genres: {genres} | stars: {stars} | rating: {rating}"
     ▼ 批量 embedding（多语言模型；分批 + 失败重试；记录耗时/内存）
  cards ──► SimpleVectorStore（内存索引）

运行时：
  用户输入（"有没有类似《The Matrix》的电影？" / 自由文本条件）
     │ ① 若命中片名 → 取该片向量做 similaritySearch（"找相似"）
     │    否则  → 对查询文本 embedding 后 similaritySearch
     ▼ top-k 电影 → 复用现有 movie list / detail UI（附字段级"为何相似"说明）
```

**为何它是等价交付物**：

- 直接对应用户原话"**把这个网站相关的内容和知识点存入 RAG**"——"网站内容"最自然的落点就是**电影目录**（结构化卡片）；
- **不需要外部语料、无版权/合规问题**（与电影剧情问答相反）；
- **内存不是约束（P1-2 修正算术）**：`21087 × 768 维 × 4B ≈ 64.8 MB`（1024 维 ≈ 86 MB）；
- **真正的成本**是**一次性 embedding 时间**：M2 上数分钟～数十分钟（**待实测**），以及"结构化字段 → 卡片文本"的构造规则设计；
- 局限（如实记录）：卡片只有结构化字段，**无法回答剧情类问题**（"这部电影讲了什么"）；效果上限受字段本身限制（scout §B10）。
- 与 B-② 的关系：**共用同一套 embedding/向量库/错误处理管道**，因此两者一起做的增量成本很低。

**README 措辞建议（区分能力边界；P-2 已获用户批准，2026-10-19，由 `worker` 执行）**：

- "**电影目录语义检索（可做）**"：基于结构化字段的"相似电影/条件检索"，无需外部数据；
- "**电影剧情问答（需外部语料）**"：需外部简介/评论 + 许可，属 Option C。

### 4.3 关键设计决策（草案）

| 决策点 | 选项 | 建议 | 理由 |
| --- | --- | --- | --- |
| 索引构建时机 | 启动时 / 首次请求时 / 手工触发 | **懒加载 + 内存缓存**（首次请求构建，记录耗时） | 避免拖慢启动；`SimpleVectorStore` 本来重启即失 |
| 切分策略 | 按 heading / 固定 token / 混合 | **按 markdown 结构 + token 上限**（具体参数待实测） | `docs/` 是结构化 md，按章节切分利于引用正确性 |
| 语料范围（B-②） | 仅 `docs/architecture/` / 全部 `docs/` + README / + `ai-*.md` / + 源码注释 | **全部 `docs/`（11 md）+ README，暂不含 `ai-*.md`**（是否纳入由总览 **D-语料构成** 拍板）；源码注释仅 108 行，收益低 | scout §B9；B4 |
| 目录卡片语料（B-①） | 只 `title` / `title+year+genres` / 全字段（含 stars/rating） | **全字段卡片**（信息量大、规则简单，且模板是**确定性纯函数**易于测试） | 可评测（相似电影有人类可判断的期望） |
| **Embedding 模型**（P1-1） | 单语（如 `nomic-embed-text`）/ **多语言**（`bge-m3`、`multilingual-e5-*`、`qwen3-embedding` 等） | **多语言候选 + 先实测**（中→中/中→英命中率、维度、体积） | 中文语料 + 中文提问：单语模型会让 AC-B-6 系统性偏低 |
| 引用粒度 | 文件 / 文件+章节 / 片段文本 | **文件 + 章节标题 + 片段摘要** | 可核验（引用正确性可评测） |
| top-k | 3 / 4 / 8 | **待实测**（内存与延迟权衡） | 8 GiB 上上下文有限 |
| 无答案行为 | 编造 / 明确拒绝 / 返回最相近 | **明确拒绝**（"语料中没有依据"） | 可评测（安全率/幻觉率） |
| 流式输出 | 支持 / 不支持 | **首版不支持**（非流式，简化前端与测试） | 降低复杂度；流式可后续加 |
| 会话记忆 | 有 / 无 | **首版无**（单轮问答） | 多轮属 Option C |
| 模型不可用时 | 500 / 明确失败 / 回退外部 API | **明确失败**（`{status:"fail"}`）+ 可选外部兜底开关 | 契约一致性；不静默降级 |
| 端点命名 | `/api/boot/ai/ask`、`/api/boot/ai/search` | 建议如此（落在受保护前缀内） | 契约文档 §6 |
| 是否暴露"语料版本" | 是/否 | **是**（响应带语料指纹/构建时间）—— 已确认（总览 **D-语料指纹 = 要**，2026-10-19：文档数/块数/构建时间/哈希） | 评测与问题定位 |

### 4.4 API 设计（草案）

| 端点 | 方法 | 入参 | 成功响应 | 失败响应 |
| --- | --- | --- | --- | --- |
| `/api/boot/ai/status` | GET | — | 200 `{status:"success", enabled, chatModel, embeddingModel, corpus:{documents, chunks, builtAt}}` | 200 `{status:"fail", message}` |
| `/api/boot/ai/ask` | POST | `question`（form） | 200 `{status:"success", answer, sources:[{path, title, snippet}]}` | 200 `{status:"fail", message}`（模型不可用/超时/无依据）；400 `{errorMessage}`（参数非法） |
| `/api/boot/ai/search` | POST | `query`（form）+ `mode`（可选） | 200 `{status:"success", criteria:{...}, movies:[...]}`（**B-③ 条件式**）**或** `{status:"success", movies:[...]}`（**B-① 语义检索**） | 200 `{status:"fail", message}`（无法理解/无结果）；400 `{errorMessage}` |
| `/api/boot/ai/similar`（**B-①**） | POST | `movieId` **或** `query`（form） | 200 `{status:"success", movies:[{id,title,year,genres,rating,score}]}` | 200 `{status:"fail", message}`（电影不存在/索引未构建）；400 `{errorMessage}` |

**约定一致性**（契约文档 §6）：业务失败 `200 + status:"fail"`；参数非法 `400 + errorMessage`；未认证 `401 + errorMessage`（由现有 filter 提供）；**`/api/boot/**` 默认受 `CustomerAuthenticationFilter` 保护** → AI 端点因此需要登录。**该决策已上浮（P2-12）**：总览 §11 的 **D-AI 端点登录**——(a) 需登录（零行为变更）/(b) 允许匿名只读（需改 `shouldNotFilter`，属**行为变更**，须单独批准）。本文件建议 **(a)**。**用户已于 2026-10-19 确认 (a) 需登录**（零行为变更，沿用现有 filter 默认保护）；匿名只读**未批准**。

**超时/错误映射（草案）**：区分"模型未运行（连接拒绝）"、"超时"、"模型返回不可解析内容"、"无检索结果"四类，返回不同 message；前端只展示 message，不展示堆栈。

### 4.5 前端设计（草案）

| 项 | 方案 |
| --- | --- |
| 页面 | 优先扩展现有 **`/playground`**（公共路由，scout §A6）：新增"问项目文档"区块（输入框 + 答案 + 来源列表）**与"找相似电影"区块（B-①）**；可选新增 `/ai-search`（需注册路由 + 更新参数化重定向断言清单，若受保护） |
| 相似电影入口（B-①） | movie detail 页新增"相似电影"区块（复用现有卡片组件）——最低成本、最自然的演示路径 |
| API 模块 | `frontend/src/features/ai/aiApi.js`：端点常量 + 薄封装 + 导出纯函数（沿用 `movieListApi.js` 约定，契约文档 §4） |
| 页面状态 | 沿用 `loading/success/error` + retry；444/401 处理沿用 `ApiError.isUnauthorized` |
| 无障碍 | 答案区 `role="status"`；错误 `role="alert"`；来源列表用语义化列表；**页面级 axe 断言**（契约文档 §7） |
| 安全展示 | **不回显 prompt 内部结构**；来源 path 相对化（不泄露服务器绝对路径） |
| 测试联动 | 若页面进入 `App.test.jsx` 渲染路径 → 必须扩展 `responseBody(url)` 按 URL 分派（契约文档 §3 N13，**最具体优先**） |
| 覆盖率 | 新页面必须满足 lines **90** 阈值（计入分母；2026-10-20 由 88 收紧） |
| e2e | 新增 spec 时 API 全 `page.route` mock（**不调用真实模型**）；须同步 `vitest.config.js` 的 `exclude` 与 `playwright.config.js` 的 `testDir`（契约文档 §7 陷阱） |

### 4.6 被否决的候选

| 候选 | 否决理由 |
| --- | --- |
| pgvector/qdrant/Redis 容器 | 与 Docker 3.826 GiB 冲突（scout §C11/C14） |
| 用 MySQL 存向量 | MySQL 8.4 无 vector 类型（scout §C14） |
| 前端直连 Ollama / 直连云端 API | 绕过认证与审计、CORS 未配置 |
| 首版做多轮对话与 tools | 属 Option C |
| 首版做流式输出 | 前端与测试复杂度上升，收益有限 |
| 用 LLM 直接生成 SQL | 注入与越权风险高；改为**严格白名单字段的结构化条件**映射现有查询 |
| 把真实模型调用写进 PR 的 CI 路径 | 非确定性 + 慢 → 破坏 CI 稳定性（契约文档 §7 精神） |

---

## 5. 里程碑与工作量（**估算**）

| 里程碑 | 内容 | 依赖 | 估算 |
| --- | --- | --- | --- |
| **B0** 事实核对与索引 spike（**P2-14**） | **第一件事**：确认 `SimpleVectorStore` 在 Spring AI **1.1.x 的确切 artifact/类名与 BOM 坐标**，并回填本文 §4.1;随后：多语言 embedding 对比（**中→中/中→英检索**）+ 电影卡片构建（耗时/内存实测）+ 文档切分 + 检索 | A 完成 + 模型就绪 | 3–5 天 · **状态（2026-10-24）：✅ 已完成（范围受限）** —— 已实测端点可达性、模型清单与 GPU 占比、chat 速度/质量（3 条）、`bge-m3` 维度与冷/热延迟、4 chunk×3 query 的中文检索（3/3 top-1）；**结论：继续本地优先（`qwen2.5:7b` + `bge-m3` + `SimpleVectorStore`），下一步 B1**（**[2026-11-08 回填] B1 已完成并合并**，见顶部执行状态）。**未覆盖**：`SimpleVectorStore` 1.1.x 坐标（P2-14）、21087 卡片 embedding 耗时、`docs/` 切分块数、第二候选与中→英、NL→JSON 合规率 → **B1 也未覆盖（B1 只做接入与可观测）**，改由 **B-① 前的 B1.5 步骤（`AC-B-12` 评测，另建 issue）** 完成（证据：`docs/logs/2026-10-24-b0-local-llm-spike.md`） |
| **B-①** 电影目录语义检索后端 | 卡片文本模板（纯函数）+ 批量 embedding 索引 + `/ai/similar` + 单测（mock embedding） | B0 | 3–5 天 |
| **B-②** 文档 RAG 问答后端 | 管道封装 + `/ai/ask` + 来源提取 + 错误/超时/降级 + 单测（mock chat/embedding） | B0 | 4–6 天 |
| **B-③** NL 搜索后端（条件性） | NL→JSON 条件 + 严格校验 + 映射现有查询 + 单测；**仅当 B0 实测 JSON 合规率可接受时做** | B0 | 3–5 天 |
| **B3** 前端 | AI 页面/区块（问答 + **相似电影**）+ API 模块 + 状态机 + a11y + 测试 + 覆盖率 | B-①/B-② 契约冻结 | 4–6 天 |
| **B4** 评测 | golden set（目录检索 + 文档问答 + NL 搜索，共 20–50 条）+ 评测脚本 + 指标报告 + 回归集 | B-①/B-② | 4–6 天 |
| **B5** 观测与配置 | 日志/指标字段、开关、模型不可用行为完善 | B1 | 1–2 天 |
| **B6** 夜间真实调用通道（可选） | CI/nightly job（非阻塞） | B4 | 1–2 天 |
| **B7** 文档 | 计划回填、`docs/architecture/` AI 约定（D-B7：仍待批准，批准时机 = 实施后）、工作流日志 | 全部 | 1–2 天 |
| **合计** | | | **3–5 周**（估算） |

**估算假设**：Option A 已完成；本地模型已就绪且质量可用；单开发者 + 标准工作流。**最大不确定项（两个，已按 P1-1/P1-2 修订）**：① **embedding 的中文检索能力**（若候选模型均不达标 → 需换模型、换外部 API embedding，或把 B-② 降级）；② 小模型输出质量（若 JSON 合规率不足 → **取消 B-③**，由 B-① 的语义检索承担同一入口）。

---

## 6. 验收与评测

### 6.1 验收条件（**方案已批准；阈值已于 2026-10-19 由用户确认（D8）**，仍须实测验证）

| # | 验收条件 | 验证方式 | 判据 |
| --- | --- | --- | --- |
| AC-B-1 | 既有功能零回归 | `mvn verify` + 前端全套 + e2e | 后端 `Tests run` 不下降（≥134）、覆盖 ≥**0.90**、前端 lines ≥**90**、e2e 全绿（门槛均于 2026-10-20 收紧，**不得降低或绕过**） |
| AC-B-2 | AI 开关关闭时零影响 | 配置 `fabflix.ai.enabled=false` | 既有测试全绿；AI 端点返回明确"未启用"失败 |
| AC-B-3 | 问答返回**可核验来源** | 契约测试 + 人工抽检 | 每条源于语料的回答至少 1 个真实存在的来源 path |
| AC-B-4 | 无语料依据时明确拒绝 | golden set 的"应拒绝"用例 | 不编造（判据：不得出现语料外事实断言） |
| AC-B-5 | **B-③** NL 搜索条件正确率达标（**条件性**：仅当 B0 实测 JSON 合规率可接受时纳入验收） | golden set | **阈值 = D8 已确认（2026-10-19）：NL→JSON schema 合规率 ≥95%**；实测未达标则按计划降级并记录 |
| AC-B-6 | 检索命中率达标（**B-② 文档**与 **B-① 目录**分开统计） | 评测脚本（recall@k） | **阈值 = D8 已确认（2026-10-19）：B-① 目录检索 top-5 命中率 ≥80%；B-② 引用正确率 ≥95%；延迟 p95 先实测后定（当前无目标值）**；B-①/B-② 的阈值**分开设** |
| AC-B-7 | 模型不可用/超时行为可测 | 契约测试（mock 抛异常/超时） | 返回约定形状失败；前端显示 `role="alert"`；不崩溃 |
| AC-B-8 | 真实调用不在 PR CI 路径 | 检查 CI 配置 + 测试代码 | PR job 中无真实模型调用；夜间 job 可失败不阻塞 |
| AC-B-9 | 观测字段可用 | 日志/指标抽样 | 至少包含：耗时、top-k 命中数、token 估算、模型名、失败原因分类 |
| AC-B-10 | 可演示 | 手工演示（**本任务不执行**） | 在 `/playground` 或 AI 页面完成一次文档问答 + 一次相似电影检索 |
| AC-B-11 | **B-① 电影目录语义检索可用** | 契约测试 + golden set（相似电影）+ 手工抽检 | 对种子片返回 top-k 相似片，genre/年代合理性抽检通过；构建耗时/内存**有实测记录**；索引构建可复现（语料指纹） |
| AC-B-12 | **embedding 语言能力有实测证据**（头号风险，P1-1） | B0 对比脚本 + 报告 | **至少 2 个多语言候选**完成中→中/中→英检索对比；命中率与维度/体积/构建耗时记录在案。**（2026-10-24：仍未满足）** B0 只测 **1 个候选（`bge-m3`，1024 维）**、只有**中→中**、样本 4 chunk / 3 query，**无中→英**、无构建耗时 → 需补第二个候选与跨语言用例后才可判定（证据：`docs/logs/2026-10-24-b0-local-llm-spike.md` §0.5/§0.7）。**〔2026-11-08 回填：B1 未补此项（B1 只做接入与可观测，**不闭环 `AC-B-12`**）→ 改为在 **B-① 之前以 B1.5 单独步骤（另建 issue）** 完成。〕** |

### 6.2 评测（golden set）设计（草案）

| 集合 | 规模 | 内容 | 指标 |
| --- | --- | --- | --- |
| `catalog-similar`（**B-①**） | 15–25 条 | 种子片 → 期望相似集合特征（genre/年代/子类型） | 目录检索命中率（期望片是否在 top-k）、合理性抽检 |
| `nl-search`（**B-③**） | 20–30 条 | NL → 期望结构（genre/year 范围/sort） | 条件字段命中率、JSON 合规率 |
| `doc-qa`（**B-②**） | 20–30 条 | 问题 → 期望来源文件/章节 + 关键事实 | 检索命中率（recall@k）、引用正确性、事实符合率 |
| `should-refuse` | 5–10 条 | 语料外问题（如"电影 X 的剧情是什么？"） | 拒绝率（不得编造） |
| `regression` | 随修复增长 | 已发现的错误模式 | 不得回退 |

**测量方法**（不编造数值）：

- 检索命中率 = 期望来源出现在 top-k 的比例（脚本自动计算）。
- 引用正确性 = 人工/LLM-as-judge 判定"引用的片段是否支持回答"（**抽样人工复核**）。
- 事实符合率 = 抽样人工比对语料（**抽样比例建议 20%，估算**）。
- 延迟 = 应用侧计时（P50/P95，样本 ≥20）。
- **报告必须注明**：模型名/量化、上下文长度、top-k、语料指纹、测试日期。

### 6.3 测试策略（非确定性的处理）

| 层 | 做法 | 位置 |
| --- | --- | --- |
| 单测 | chat/embedding 全部 mock（`ChatClient`/`EmbeddingModel` 接口注入）；纯函数（prompt 构造、来源提取、条件校验）可确定性测试 | `mvn verify`（**阻塞**） |
| 契约/集成 | `@WebMvcTest` + mock AI bean；错误/超时/降级路径 | 后端 CI（**阻塞**） |
| 前端页面 | `fetch` mock（沿用既有模式）；三态 + a11y | vitest（**阻塞**） |
| e2e | Playwright `page.route` mock AI 端点（**响应形状必须与真实一致**） | 前端 CI（**阻塞**） |
| 真实评测 | golden set 脚本调用真实本地模型 | 夜间/手工（**不阻塞**） |

**覆盖率预算**：

- 后端新增类（controller/service/prompt/解析）**必须有测试**；不扩 exclude。
- 前端新页面/组件计入 lines **90** 阈值（2026-10-20 由 88 收紧）。
- **风险**：AI 分支多（超时/解析失败/无结果）→ 分支覆盖虽无阈值但行覆盖需全绿；建议用**接口注入**让所有分支可 mock 触达。

### 6.4 可观测性

| 指标 | 采集点 | 用途 |
| --- | --- | --- |
| 延迟（P50/P95） | 端点计时 | 体验与容量 |
| 失败率/失败分类 | 异常分类计数 | 稳定性 |
| top-k 命中数与最高相似度 | 检索层 | 调参与评测 |
| token 估算（输入/输出） | prompt/回答长度估算 | 容量与"若换外部 API 的成本"估算 |
| 语料指纹（文档数/块数/构建时间） | 索引层 | 复现评测 |
| 模型名/版本 | 配置 | 结果可比性 |

**不采集**：用户敏感数据（密码、卡号）、完整 prompt 原文（可只记录长度与哈希）。

---

## 7. 风险与回退

| 风险 | 概率 | 影响 | 缓解 / 回退 |
| --- | --- | --- | --- |
| **8 GiB 上模型质量不足**（NL→JSON 合规率低、中文差） | 中高 | **B-③ 不可用**（B-①/B-② 仍可交付） | B0 先实测；退化路径：**取消 B-③**、由 B-① 语义检索承担同一入口 / 缩小到**检索式**功能 / **外部 API 兜底**（需批准 token 成本）。**2026-10-24 B0 实测（局域网 PC、100% GPU）**：概念题可用（RAG 释义合理、56–70 tok/s），但**事实清单题出错**（把 2009 年的《阿凡达》列为 1990 年代）→ 幻觉风险**已实测复现**；**NL→JSON 合规率仍未测**（`AC-B-5`/B-③ 仍条件性）。证据：`docs/logs/2026-10-24-b0-local-llm-spike.md` §0.3 |
| **延迟过高**（8B 4-bit 在本机） | 中 | 体验差 | 首选更小模型；限制 top-k 与输出长度；非流式 + 明确 loading 态；给"演示用"定位 |
| **内存/磁盘压力** | 中 | 本机不可用 | 只保留必要模型；限制上下文；磁盘 22 GiB 硬约束 |
| **`SimpleVectorStore` 重启即失** | 确认 | 首次请求需重建索引 | 懒加载 + 记录构建耗时；迁移路径 → pgvector（见 §7.2） |
| **语料泄露敏感信息** | 低 | 安全 | 语料仅限仓库内受控文档（已是 C 决策的一部分）；不索引 `.env`/凭据；实施时显式白名单路径 |
| **prompt injection（用户输入）** | 中 | 越权/误导 | 系统指令与用户输入分区；输出后置校验；工具白名单（本方案无写操作） |
| **RAG 语料注入**（外部内容若引入） | 低（本方案语料受控） | 安全 | 首版语料仅仓库内文件；**明确不引入外部语料** |
| **非确定性导致 CI flaky** | 中 | CI 不稳 | PR 全 mock；真实调用仅夜间；e2e mock 形状保真 |
| **覆盖率闸门被打破** | 中 | CI 红 | 新增类必须配测试；用接口注入保证可 mock |
| **引用错误（幻觉引用）** | 中 | 可信度 | 来源必须来自检索 metadata（**不让模型自由生成路径**）；引用正确性进评测 |
| **`@MockBean`/上下文加载问题**（Boot 3） | 低中 | 测试不稳 | 与 Option A 的测试策略一致；必要时用切片测试 |
| **embedding 中文检索能力不足（头号风险，P1-1）** | 中高 | AC-B-6/AC-B-3 系统性不达标，甚至被误判为"8 GiB 模型不行" | B0 **先做中→中/中→英对比**（**至少 2 个**多语言候选）；备选：换模型 / 换外部 API embedding / 把 B-② 降级为纯关键词检索；**必须把实测数字写进报告**。**2026-10-24 B0 部分证据**：`bge-m3`（1024 维）在中→中 4 chunk / 3 query 上 3/3 命中 top-1（0.6121–0.8445）→ **未发现方向性缺陷**，但样本太小、无中→英、无第二候选，**风险仍为中高**（`AC-B-12` 未满足） |
| **Ollama 未启动 / 启动顺序（P2-13）** | 高（演示常见） | 首次请求失败、演示"卡住"；若索引在启动时构建还会拖慢应用启动 | 端点返回明确"模型不可用"并给操作提示；索引**懒加载**而非启动时同步构建（§4.3）；前端可测错误态 + 重试；部署专项 §8 前置清单给出"**先启 Ollama 再启应用**"的检查步骤 |
| **生产环境无模型 → 端点报错** | 高（预期） | 用户困惑 | 端点/前端**明确显示"AI 未启用/本地模型不可用"**；默认开关关闭；不影响其他功能 |
| **局域网端点无鉴权**（**2026-10-24 B0 实测环境新识别**） | 确认（当前环境） | 任何能进入该局域网的主机即可调用（耗 GPU / 读模型）；语料与 prompt 走**明文 HTTP** | 仅限**受控家用/实验局域网**；跨网或共享网络前必须先加鉴权（反向代理 + token）与 TLS；端点地址只走配置（`spring.ai.ollama.base-url`），不在前端/`README` 硬编码；**不将敏感语料送入不可控网络** |
| **冷启动 / 模型换入换出**（**2026-10-24 B0 实测新识别**） | 确认 | embedding 冷 7.64 s vs 热 0.071–0.129 s（**约 100×**）；chat 与 embedding 交替调用会让查询侧回落到 2.3–2.5 s → 直接污染 p95 | 索引/首次请求**懒加载**并记录构建耗时；`/api/boot/ai/status` 暴露"模型已加载/未加载"；p95 **仅在"已加载"条件下统计**，且**先实测后定目标值**（D8 未闭环）；必要时调 keep-alive/常驻策略（**待实测确认**） |
| **macOS「本地网络」权限拦截**（**2026-10-24 B0 实测新识别**） | 中高（首次运行） | 表现为 `No route to host` / 超时，易被误判为"模型未启动"或"配置写错" | B1 前置检查：为运行 Maven/JVM 的终端或 IDE 在**系统设置 → 隐私与安全性 → 本地网络**授权；排障时用"换客户端对照"（B0 中 `curl` 通、Python `urllib` 不通）；错误日志区分"连接不可达"与"模型未就绪" |
| **与 AD-2 单实例假设耦合**（内存向量库 + 会话） | 确认 | 扩展受限 | 记录为约束；多实例前必须换持久向量库 |

### 7.1 回退方案

1. **功能级回退**：`fabflix.ai.enabled=false` 即回到"无 AI"状态（代码保留、行为消失）。
2. **实现级回退**：**取消 B-③**（NL→结构化），由 **B-① 语义检索**承担同一直入口，或退化为纯检索式（不生成）。
3. **模型级替换**：chat 侧换成外部 API（显式配置 + 用户批准），接口不变。
4. **版本回退**：整个 Option B 在单分支单 PR；未合并即 `revert`。
5. **禁止**：不得为了"让 AI 看起来能用"而静默吞掉失败（必须显式失败 + 可观测）。

### 7.2 迁移路径（后续能力）

| 现在 | 后续（Option C） | 触发条件 |
| --- | --- | --- |
| `SimpleVectorStore`（内存） | pgvector / qdrant（容器或宿主机服务） | 需要持久化/多实例/更大语料，且 Docker 内存问题解决 |
| 语料 = `docs/`+README | + 电影简介等外部语料 | 用户批准来源与合规 |
| 单轮问答 | 多轮对话 + 会话记忆 | Option C |
| 只读 | agent tools + 写操作审批 | Option C（+ AD-1 边界 issue） |
| 无成本计量 | 预算/配额 | Option C |

---

## 8. 简历价值

| 要点 | 可展示证据 |
| --- | --- |
| "用 Spring AI 构建 RAG 问答：ETL（markdown 读取→切分→embedding→向量库）+ 检索 + 生成 + 引用来源" | 架构图、代码结构、管道配置 |
| "为 21,087 部电影构建结构化卡片向量索引，支持"相似电影"语义检索"（**B-①**） | 卡片构造规则 + **实测构建耗时** + 目录检索评测数字 |
| "在中文语料 RAG 中识别并实测 embedding 语言能力风险（中→中/中→英）" | 候选词型对比表 + 实测命中率 |
| "回答必须带可核验来源，且无依据时明确拒绝" | golden set 的"应拒绝"用例 + 评测报告 |
| "为 AI 功能建立评测体系（golden set 20–50 条、检索命中率、引用正确性、延迟 P50/P95）" | 评测脚本 + 报告 + 版本化数据集 |
| "把非确定性 AI 纳入 CI：PR 全 mock 分层 + 夜间真实调用，不破坏既有覆盖率/CI 门禁" | CI 配置 + 覆盖率数字 + 夜间 job 历史 |
| "本地 LLM 部署：8 GiB Apple Silicon 上运行 4-bit 量化模型，零 token 成本" | 部署文档 + **实测** tokens/s（部署后回填） |
| "自然语言搜索：LLM 输出结构化查询 + 严格白名单校验（不让模型直接生成 SQL）" | 校验代码 + 评测数字 |
| "可观测性：延迟/失败率/检索命中/token 估算" | 指标定义 + 日志样例 |

---

## 9. 用户决策（oracle 审查已完成；**用户已裁定，2026-10-19**）

| # | 事项 | 选项 | docs-writer 建议 | **用户决定（2026-10-19）** |
| --- | --- | --- | --- | --- |
| **D-B1** | 首个 AI 交付范围 | (a) 只 NL 搜索 / (b) **B-① 电影目录语义检索 + B-② 文档 RAG**（+ 条件性 B-③）/ (c) 含推荐 | **(b)**；推荐 → Option C | **按建议（b）已批准**（总览 D5）：首个交付 = **B-① + B-②（带引用）**；推荐 → Option C |
| **D-B2** | 检索实现路径 | **B-① 语义检索** / B-③ 结构化条件 / 两者都做 | **两者都做** | **未逐条裁定** → 暂按建议（B-① 硬交付，B-③ 仍条件性），**实施前需确认（未确认）** |
| ~~D-B3~~ | ~~AI 端点是否需要登录~~ | — | **已上浮为总览 §11 的 D-AI 端点登录（P2-12）** | **✅ 已确认（用户，2026-10-19）= (a) 需登录**（零行为变更，沿用 `CustomerAuthenticationFilter` 默认保护；匿名只读未批准） |
| **D-B4** | 外部 API 兜底是否允许 | 允许/不允许 | 允许作为**显式开关**；默认本地 | **已批准（允许，按建议）**：外部 API = **显式开关兜底**；**D7 已于 2026-10-19 确认**：默认关闭；启用时 $2/日 + ≤200k tokens（可调） |
| **D-B5** | 是否加夜间真实调用 job | 加/不加 | **加** | **✅ 已确认：加（非阻塞）（总览 D10，2026-10-19）**；**M4.2 后接入**；**CI 分钟预算仍待定**（U-B7） |
| **D-B6** | 语料范围（B-②） | 全部 `docs/`+README / 仅 `docs/architecture/` / + `ai-*.md` / + 源码 | **全部 `docs/`（11 md）+ README，暂不含 `ai-*.md`** | **已批准（按建议）**：即总览 **D-语料构成 (a)** |
| **D-B7** | 是否新增 `docs/architecture/ai-integration-contracts.md` | 新增/暂不 | **实施后新增** | **仍待批准（批准时机 = 实施后）**；未因整体批准而提前生效（总览 P-5） |
| **D-B8** | 演示质量下限 | 需用户给数 | 无此数字则 AC-B-5/AC-B-6 无法判定 | **✅ 已确认（用户，2026-10-19）**：目录 top-5 命中率 ≥80%、RAG 引用正确率 ≥95%、NL→JSON schema 合规率 ≥95%；**p95 延迟先实测后定（当前无目标值）**（总览 §11 D8）；**阈值确认 ≠ 已达标，仍需实测** |
| **D-B9** | 是否修改 README roadmap 措辞（movie Q&A → 区分“目录语义检索（可做）”与“剧情问答（需外部语料）”） | 改/不改 | **改** | **已批准（P-2）**；由 `worker` 执行，**执行中/已完成以主 agent 回传为准** |
| **D-B10** | embedding 选型与实测（P1-1） | 单语 / **多语言候选** | **多语言 + 先实测** | **已批准（按建议）**：多语言 embedding + **先实测中→中/中→英**；候选以 Ollama 实际可用性为准 |
| **D-B11** | 是否要求语料指纹与增量重建 | 需要/不需要 | **需要** | **✅ 已确认（用户，2026-10-19）= 需要**（总览 **D-语料指纹**）：记录**文档数/块数/构建时间/哈希**；响应或日志携带指纹；具体字段/位置待实施时定（U-B8） |

**oracle 应重点检查**：① 语料范围与"电影问答"期望的落差是否被诚实处理；② 评测指标定义是否可复现、是否会被"看起来通过"的伪指标污染；③ 非确定性测试分层是否真的不阻塞 PR；④ 引用来源是否可能被模型伪造（本方案要求来源来自 metadata）；⑤ 与 AD-2/AD-1 的耦合是否被明确记录；⑥ 覆盖率与 e2e 契约（mock 形状保真）是否被覆盖；⑦ 超时/降级语义是否与既有 `{status,message}` 约定一致。

**未确认事项**：

| # | 未确认项 |
| --- | --- |
| U-B1 | ~~Spring AI 1.1.x 中 RAG 相关类的确切名称与包~~ → **P2-14 已把它定为 B0 的第一件事**：确认 **`SimpleVectorStore` 在 1.1.x 的确切 artifact/类名与 BOM 坐标**，并**回填本文 §4.1** |
| U-B2 | `SimpleVectorStore` 的持久化选项与索引重建**实测**耗时 |
| U-B3 | **多语言 embedding 候选**（`bge-m3` / `multilingual-e5-*` / `qwen3-embedding`）在 Ollama 的可用性、维度、下载体积与**中文检索命中率**（**头号风险**，P1-1）→ **2026-10-24 部分解决**：`bge-m3` 可用、**1024 维**、1.16 GB；中→中 3/3 top-1（0.6121/0.6990/0.8445，4 chunk）。**仍开放**：第二候选对比、中→英、命中率/构建耗时 |
| U-B4 | 8 GiB 上 chat 模型的实际 tokens/s 与 top-k 对延迟的影响（**待实测**）。**2026-10-24 部分解决（口径不同）**：实测环境**不是本机 8 GiB**，而是**局域网 PC + 100% GPU**；`qwen2.5:7b` 热请求 **56–70 tok/s**（推算口径 = completion tokens ÷ 端到端耗时）。**仍开放**：top-k 对延迟的影响、并发>1、`bge-m3` 是否全量驻留，以及本机 8 GiB 能否承载 |
| U-B5 | 21087 条电影"卡片"embedding 的**耗时**（B-① 可行性；**内存已由算术排除**：768 维 ≈ 64.8 MB、1024 维 ≈ 86 MB） |
| U-B6 | `docs/` 语料切分后总块数与 embedding 总耗时（**待实测**） |
| U-B7 | 夜间 job 的 CI 分钟数预算（**D-B5/D10 已确认「加」，但未给分钟数**） |
| U-B8 | **语料指纹的具体字段与暴露位置**（响应头/响应体/日志）与增量重建触发方式（总览 D-语料指纹已定「要」） |
| U-B9 | **D8 阈值能否在 8 GiB 本地方案下达标**（p95 尚无目标值；目录 top-5 ≥80% / 引用正确率 ≥95% 仍待实测） |

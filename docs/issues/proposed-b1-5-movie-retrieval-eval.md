# [SUPERSEDED] B1.5 单 issue 草稿 — 不发布

> **状态：⬜ SUPERSEDED（2026-11-09）— 本文件不是可发布的 issue 正文，`github-operator` 不得据此创建 issue。**
>
> | 元数据 | 值 |
> | --- | --- |
> | 文件类型 | 历史草稿（已被拆分取代，仅用于追溯） |
> | `last_updated` | **2026-11-09**（改为 superseded 说明） |
> | 取代者 | `docs/issues/proposed-b1-5a-movie-card-embeddings.md`（A 路线）、`docs/issues/proposed-b1-5b-tool-routing.md`（B 路线） |
> | 计划侧取代者 | `docs/plans/b1-5a-movie-card-embeddings.md`、`docs/plans/b1-5b-tool-routing.md`；总纲 = `docs/plans/b1-5-movie-retrieval-eval.md`（umbrella / 决策备忘录） |
> | GitHub issue | **不存在，且不再创建本版本**（编号不得虚构） |

---

## 1. 为什么被取代

用户 2026-11-09 批准 **B-9（拆分）**：原草案把 A（电影卡片 embedding）与 B（结构化工具/路由）两条路线的全部执行细节、验收条件与风险写在一个 issue 里。问题：

1. **一个 PR 无法同时交付两条独立评测** → 验收无法分别闭环，reviewer 无法判断哪一条通过了；
2. **原草案有 4 处已被后续事实/决定修正的表述**（标题"MCP-style"、"replaces AC-B-12"、AC-2 的"第二个模型待批准"、"1990 年代必然空结果"），继续维护会同时存在两套口径；
3. **`AC-B-12` 已按 D-7 改写为两条路线各自的验收**，一个 issue 装不下两种验收对象。

## 2. 原草案中已被取代的具体内容（不要沿用）

| 原内容 | 现在的口径 |
| --- | --- |
| 标题含 "MCP-style tools" | 改为 "structured tools / tool calling"；B1.5 **不实现 MCP**（D-6） |
| "This issue replaces `AC-B-12`" | **不声称关闭旧 `AC-B-12`**；按 D-7 改写为两条路线各自的验收（umbrella §5） |
| AC-2 "至少两个多语言 embedding 候选（第二个待批准）" | 第二个模型 **已批准并已安装**（`qwen3-embedding:0.6b`，实测 1024 维）；A 路线的 AC 见 B1.5a |
| "数据集 9,052 或 21,087，Docker 未运行无法核实" | 数据源 **已定**：Docker `fabflix-mysql`(3307) **21,087** 部 / 196–2024 / 61 行 56 distinct 类型 / 13,082 无评分（D-3） |
| "本地覆盖 2001–2019（1990–1999 = 0 行），golden set 不得含 1990s" | **已推翻**：21k 上 1990s 有 **2,370** 部（"1990 年代科幻片"实测 55 部可命中）；**"1990 年代 + 评分门槛"仍必然为空**（该区间无评分行） |
| "本地 23 个英文类型名" | 当前快照 **56 个 distinct 名称 / 61 行**，且含重复行与 `Sci-Fi`/`Science Fiction` 变体 → enum 必须由 DB 派生 |
| 30 条金标准（10/10/10） | **60 条**（20/20/20）+ **15 条工具用例**（B-4），A/B 共用 |
| 单一 issue 的 B15-1…B15-10 验收表 | 拆为 `B15a-*`（B1.5a §10）与 `B15b-*`（B1.5b §9） |
| 无机器可读 schema 契约 | `docs/ai/tool-schema.b1-5.json`（含 `schema_hash`）+ `docs/ai/golden-set.schema.json`（B-5） |
| 无 fingerprint 算法 | `docs/ai/README.md` §2 固定了 `template_hash` / `db_snapshot_hash` / `schema_hash`（B-6） |
| "MCP 端点在 `/mcp`、`/sse` 匿名可达"作为必测项 | 本期**不发布 MCP 端点** → AC-9 写 **N/A**；若引入另立鉴权 issue（D-6/B-8） |
| "第 15/16 行 README" | README 实际需改的是**第 13/14 行**，精确替换文本见 `docs/plans/b1-5a-movie-card-embeddings.md` §9 |

## 3. 保留了什么（仍有效的前提）

以下前提在拆分后仍然有效，已写入两份新草稿与 umbrella：

- 用户侧只做**电影**检索/推荐；开发文档不是用户语料（D-1/D-5）。
- B1.5 只做**评测与选型**，不交付用户功能、不动 `src/main`/`pom.xml`/CI。
- 纯 LLM 路由 **58%（7/12）不可交付**（D-9）；交付形态 = 确定性路由 + 受控填充。
- 真实模型调用**不进 PR CI**（`AC-B-8`）。
- 端点无鉴权、仅家用局域网；评测脚本不得依赖 CI 或 PC 常开。

## 4. Internal tracking

> 本分区不发布到 GitHub（本文件整体都不发布）。

| 项 | 值 |
| --- | --- |
| 来源 | 用户 2026-11-08 新方向 + 2026-11-09 的 9 项批准决定 |
| 被取代时间 | 2026-11-09（oracle 阻塞项 B-9 落地） |
| 取代者 | `docs/issues/proposed-b1-5a-movie-card-embeddings.md`、`docs/issues/proposed-b1-5b-tool-routing.md` |
| GitHub issue | **未创建、不会创建本版本** |
| 处理建议 | 保留本文件作为追溯记录；B1.5a/b 两个 issue 创建后，可考虑在本文件顶部追加真实编号链接，**不要**删除本文件 |

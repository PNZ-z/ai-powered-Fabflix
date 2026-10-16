# Issue #32 方案 / 计划 — Add responsive styling, accessibility, frontend tests, and CI

> **实现状态：实施完成，已通过主 agent 终验（2026-10-15）；已提交（`092147a` 实现 + `fb65eda` 文档）、已推送、**PR #42 已于 2026-10-16 合并**（merge commit **`d618b64`**），**issue #32 已关闭**（GitHub 依 PR 正文的 `Closes #32` 自动关闭）。**
> 分支 `codex/issue-31-32-33-m2-completion`（HEAD `fb65eda`）→ PR **#42**；**CI 首跑（head `fb65eda`）两个 job 均 SUCCESS**——其中 `Frontend (lint, test, coverage, build, e2e)` 是该新 job 的**第一次运行**，故本 issue 的 CI 证据已闭合（见 §13.4）；**合并后远端分支已删除、`main`（`d618b64`）上 CI 双 job 亦 SUCCESS**；里程碑 **Phase 2 — React Frontend Migration（#2）已关闭**（10/10）。
> 本文档 §1–§11（及 §12 的批准时记录）保留为**批准时的计划文本**（事实性内容未追改）；实现结果、与计划的逐项差异、终验证据（含实际覆盖率数字与阈值）与残余风险见 **§13**。
>
> **文档状态（批准时）：已批准（主 agent，2026-10-15）— 已交 worker 实现。**
>
> 依据 oracle 第一轮只读审查报告 `.pi/artifacts/oracle-issues-31-32-33-plans.md`（#32 章节），**B1（vitest 会收集 `e2e/**`）与 B2（a11y 依赖定稿）已闭合**，
> 建议 N-32-1..N-32-12 与未决问题 Q-1..Q-10 **已全部裁定并写入本文档**（逐行回填见 §11；§10 已转为「已裁定问题」表）。
> 主 agent 裁定依据：oracle 报告「#32：需修订后可批准——B1（不修就必红）与 B2（已批准决定要求写明）必须先闭合」——二者现已按报告的最小修复写入计划。
> **本批修订不改变 #32 的 AC-32-1..AC-32-5**；新增依赖已获批：**`@playwright/test`、`@vitest/coverage-v8`、`axe-core`**（3 个 devDependencies，+ Playwright 浏览器下载）；a11y 采用 **`axe-core` 直连 + 自建 helper，不引入 `vitest-axe`**。
> **批准状态不代表已实现**：覆盖率**基线数字**、CI 实际运行结果仍需 worker 实测后回填（§12）。文件:行号若与 oracle 报告不一致，**以 oracle 报告（其已复核）为准**并在相关节注明。

## 元数据

| 项目 | 值 |
| --- | --- |
| GitHub issue | #32 — https://github.com/PNZ-z/ai-powered-Fabflix/issues/32 |
| 标题（远端原文） | Add responsive styling, accessibility, frontend tests, and CI |
| Milestone | Phase 2 — React Frontend Migration（与 #31/#33 共用；由主 agent 提供） |
| 来源 | 用户任务 + GitHub issue #32 远端正文（主 agent 用 `gh` 获取）+ scout 报告 `.pi/artifacts/scout-issues-31-32-33.md` + docs-writer 亲自核对的源码 |
| 关联 issue 跟踪文档 | `docs/issues/issue-32-...`（见 §5.5） |
| 关联全局约束 | `docs/architecture/frontend-migration-contracts.md`（§7 测试设施与 CI 闸门） |
| 实现顺序 | **#31 → #32 → #33**（用户已批准）；#32 可与 #31 部分并行，但「前端测试覆盖 + CI 跑前端」必须在 #31 新页面并入后才完整（scout「风险与依赖顺序」） |
| 分支 / PR 计划 | 分支 `codex/issue-31-32-33-m2-completion`，单个 PR（`Closes #31`/`#32`/`#33`） |
| 创建日期 | 2026-10-15 |
| 文档状态 | **已批准（主 agent，2026-10-15）— 可交 worker 实现**；B1/B2 已闭合，N-32-1..N-32-12 与 Q-1..Q-10 已全部裁定（§10/§11） |
| 验收范围 | **本批修订不改变 #32 的 AC-32-1..AC-32-5**；新增 devDependencies 已明确批准：`@playwright/test`、`@vitest/coverage-v8`、`axe-core` |

## 1. 目标与背景

### 1.1 目标

让迁移后的 React 应用具备**生产级用户体验**与**自动化质量闸门**（issue 正文 "Goal"）：一致的响应式样式与可复用 design token、
语义化标记 / label / 焦点态 / 键盘交互、带 API mock 的单元/组件测试、关键流程的浏览器级测试，以及
GitHub Actions 中的 lint / 测试 / 覆盖率 / 生产构建。

### 1.2 背景：现状事实（带文件:行号证据）

> docs-writer 亲自复核（`main` @ `f6163f3`）；与 scout 不一致处已注明。

**样式体系**

- `frontend/src/index.css`（**848 行**，单文件）：唯一 token 来源是 `:root`（第 1-16 行：`--text`/`--text-h`/`--bg`/`--border`/`--accent`/`--accent-bg`/`--accent-border`/`--shadow`/`--sans`/`--heading`/`--mono`）；暗色 token 在 `@media (prefers-color-scheme: dark)`（第 33-48 行）。
- 现有断点（docs-writer 已 grep）：`@media (max-width: 1024px)` 出现在第 28、80、90 行；`@media (max-width: 760px)` 出现在第 688、828 行。**没有**更窄的移动断点（如 480px），也**没有**设 `base`/`outDir` 之外的构建层设计 token 文件。
- **无设计 token 独立文件/构建层** → D-32-4 已批准「继续以 `index.css` 的 `:root` 为单一来源，不新建设计系统」。

**无障碍现状**

- 已有：表单 `htmlFor`（`HomePage`、`AutocompleteSearch`、`LoginPage`、`PaymentPage`、`MovieListPage`、`MovieSearch`、`MovieSort`，scout B2 逐行列出）；`role="alert"`/`role="status"` 状态播报；`AutocompleteSearch.jsx:109-135` 完整 combobox 语义；`MovieListPage.jsx:58` 排序 `role="radiogroup"`；cart 操作按钮 `aria-label`；`CartBadge` 视觉层 `aria-hidden` + Cart 链接真实 `aria-label`（架构文档 §4）。
- **缺口（scout B2，docs-writer 已复核）**：`frontend/src/index.css` 中**唯一**的焦点相关规则是 `.movie-sort-pill:focus-within`（第 527 行）；**没有全局 `:focus-visible`** → 按钮/链接/输入框的键盘焦点态依赖浏览器默认。
- 无自动化 a11y 断言：`frontend/.oxlintrc.json`（docs-writer 已读）只有 3 条规则（`react/rules-of-hooks`、`react/only-export-components`）；无 `eslint-plugin-jsx-a11y`、无 jest-axe/vitest-axe。
- **a11y 依赖选型事实（oracle B2 复核，本轮采用）**：`npm view vitest-axe version peerDependencies` → **`version = '0.1.0'`、`peerDependencies = { vitest: '>=0.16.0' }`**（单版本、明显久未更新，且对 React 19 无 peer 声明）→ **不采用 `vitest-axe`**；改用 **`axe-core` 直连 + 自建 helper**（`frontend/src/test/axe.js`）。参考版本：`@vitest/coverage-v8` 与 `vitest ^5.0.3` 对齐（可用 `5.0.x`）、`@playwright/test` `1.64.1`（均由 oracle 用 `npm view` 实测）。

**前端测试设施**

- `frontend/vitest.config.js`（docs-writer 已读）：`environment: 'jsdom'`、`setupFiles: './src/test/setup.js'`（第 7-8 行）；**无 coverage 配置**，**未覆写 `exclude`**。
  - **B1 事实（oracle 实测，本计划已按此修定）**：本仓库 vitest 为 **5.0.3**，其默认 `include = ['**/*.{test,spec}.?(c|m)[jt]s?(x)']`，而当前 `vitest.config.js` **没有**覆写 `include`/`exclude` → 放在 `frontend/e2e/*.spec.js` 的 Playwright 用例**会被 `vitest run` 收集并执行**，`import { test } from '@playwright/test'` 在 vitest 下会直接失败 → `npm test` 必红。修见 §4.3/§5.4（Q-5）。
- `frontend/package.json`（docs-writer 已读）：scripts = `dev`/`build`/`lint`(`oxlint src`)/`preview`/`test`(`vitest run`)/`test:watch`；devDependencies 有 `@testing-library/*`、`jsdom`、`oxlint`、`vite@^8.3.0`、`vitest@^5.0.3`；**无 `@vitest/coverage-*`、无 Playwright/Cypress**（已 grep：0 命中）；**无 `engines` 字段**。
- 现有前端测试文件：**26 个**（`find frontend/src -name '*.test.js*' | wc -l`，docs-writer 复核）。
- 测试模式（架构文档 §7）：`MemoryRouter` + `vi.stubGlobal('fetch', ...)` 返回 `new Response(JSON.stringify(...))`；`src/test/setup.js` 做 jest-dom + `afterEach(cleanup)`。

**CI**

- `.github/workflows/ci.yml`（docs-writer 已读）：单一 job `build-and-test`，name `Build and test (Java 11)`；步骤 = checkout（第 26 行）、`actions/setup-java@v5`（第 29-33 行，temurin 11 + maven 缓存）、**唯一构建步骤** `mvn --batch-mode --no-transfer-progress verify`（第 36 行）、JaCoCo summary（第 38-72 行）、上传 surefire/JaCoCo 产物（第 74-89 行）。
  - **无 Node 步骤、无 npm**（scout B4 已 grep；docs-writer 复核 ci.yml 中确无）。
  - `timeout-minutes: 15`（第 22 行）、`concurrency` 取消在跑（第 14-16 行）。
- `pom.xml`：`<profile>` 数量 = **0**（docs-writer 复核）→ 尚无 `frontend` profile（#33 D-33-1 才会加）。
- JaCoCo 闸门：`BUNDLE / LINE / COVEREDRATIO ≥ 0.85`，`verify` 阶段（`pom.xml:130-171`）。

### 1.3 已批准决定（用户/主 agent 已传达；写入本文档作为**已批准范围**）

| 编号 | 已批准决定 |
| --- | --- |
| **D-32-1** | 引入 **Playwright（仅 chromium）** 做关键浏览器级测试：login / browsing / cart / employee 四条 smoke；API 用 `page.route` mock，**不依赖 MySQL**；CI 新增步骤 `npx playwright install --with-deps chromium` + `npm run test:e2e`；新增相应 devDependencies。 |
| **D-32-2** | 引入 **`@vitest/coverage-v8`**，新增 `test:coverage` 脚本；**先测量实际基线**，阈值设为**基线下方 2–5 个百分点**且**行覆盖 ≥75%**，CI 阻断（lint / unit tests / coverage / production build / e2e）。 |
| **D-32-3** | **Node 22 LTS** 固定用于 CI 与 Docker（`actions/setup-node`、Dockerfile；可在 `frontend/package.json` 加 `engines`）。 |
| **D-32-4** | 响应式与无障碍按 scout B1/B2 缺口补齐：design tokens 继续以 `index.css` `:root` 为**单一来源**（不新建设计系统）；增加**全局 `:focus-visible`** 焦点样式；关键页面（home/login/movies/cart/checkout/confirmation/employee）在 **360–1440px** 检查与修复；引入 **`vitest-axe`（或等价、无浏览器依赖的方案）** 做关键页面的自动化 a11y 检查——**若引入该依赖需在计划中写明并已获批准**。→ **2026-10-15 裁定：采用 `axe-core` 直连 + 自建 helper（不引入 `vitest-axe`，§10 Q-1）** |
| **顺序与交付** | 一个分支 `codex/issue-31-32-33-m2-completion`、一个 PR（`Closes #31`/`#32`/`#33`）；实现顺序 #31 → #32 → #33；每阶段都要跑相应测试；后端若有新增代码必须配测试（JaCoCo 85% 闸门）。 |

> **批准状态（2026-10-15 更新）**：D-32-1..D-32-4 与「顺序与交付」均为已批准范围；D-32-4 中「vitest-axe **或等价方案**」的**具体选型已由主 agent 裁定为 `axe-core` 直连 + 自建 helper（不引入 `vitest-axe`）**（§10 Q-1，依据 oracle B2）；覆盖率阈值的**具体数字**仍需 worker 实测基线后回填（§6.2/§10 Q-10），但规则（基线下方 2–5pp 且行覆盖 ≥75%）已固定。

## 2. 验收条件逐条映射（issue 正文原文 → 本计划如何满足）

| # | issue 验收条件（原文） | 计划承载（提案） | 验证方式（计划） |
| --- | --- | --- | --- |
| AC-32-1 | Core pages work at desktop and mobile widths | 以 `index.css` `:root` token 为唯一来源，补齐 360–1440px 的响应式规则（断点沿用/扩展既有 1024px、760px，**新增 480px 仅加必要规则**）；重点：宽表格横向滚动/列宽、排序控件、表单、toast/徽标、员工页 | **Playwright** 在 **360 与 1440** 两种 viewport 下跑 smoke（Q-3 已裁定：仅两档）；可用可执行断句（如 `document.documentElement.scrollWidth <= window.innerWidth`、关键控件可见）；jsdom 无法验证布局 → 人工在浏览器确认 + 记录（§6.4） |
| AC-32-2 | Forms and navigation are keyboard usable | 全局 `:focus-visible` 样式（D-32-4）；确认既有 label/`role="alert"`/combobox 语义不回退；对新增/修改的交互补键盘可达性（Tab 顺序、Enter/Space） | 组件测试断言 label/role/aria；Playwright smoke 用 `keyboard` 走 login 与导航（断言焦点与提交）；a11y 自动检查（§6.3） |
| AC-32-3 | Automated accessibility checks cover key pages | 用 **`axe-core` 直连 + 自建 helper `frontend/src/test/axe.js`（`expectNoA11yViolations(container)`）**（Q-1 已裁定）；**页面清单与关键断言已固定**（N-32-12）：`HomePage`、`LoginPage`、`MovieListPage`（含排序 `role="radiogroup"`）、`MovieDetailPage`、`CartPage`、`PaymentPage`、`ConfirmationPage`、员工登录页与仪表盘页（#31）——每页至少断言：单一 `h1`、表单控件有可访问名、交互元素有 role、图片/图标可访问 | 前端组件测试中的 axe 断言；`npm test` 必须全绿；**jsdom 下无布局/无颜色计算 → `color-contrast` 等规则不适用，需逐页列出实际生效/跳过的规则集**（§6.3，不得声称覆盖对比度） |
| AC-32-4 | Frontend test and coverage commands run deterministically | 新增 `test:coverage`（`@vitest/coverage-v8`）+ `test:e2e`（Playwright）脚本；固定 Node 22（D-32-3；`engines: { "node": ">=22.12.0" }`）；Playwright 固定 chromium 且 mock API（无 MySQL/无网络波动）；覆盖率阈值 = 实测基线下方 2–5 个百分点且 ≥75%（D-32-2） | **确定性证据 = 干净环境连续两次同一个命令的用例数与覆盖率数字一致**（Q-9，写入交付报告）；命令：`npm test` / `npm run test:coverage` / `npm run test:e2e` / `npm run lint` / `npm run build`；**以 Node 22 的结果为准** |
| AC-32-5 | CI blocks regressions in linting, tests, coverage, or build | `.github/workflows/ci.yml` **新增独立 `frontend` job**（Q-2 已裁定：`timeout-minutes: 20`、`actions/setup-node@v4` + `node-version: 22` + npm 缓存指 `frontend/package-lock.json`、`working-directory: frontend`）：`npm ci` → `lint` → `test` → `test:coverage` → `build` → `playwright install --with-deps chromium` → `test:e2e`（D-32-1/D-32-2） | CI 配置本身；本地先复现全部命令（§6.5）；CI 运行结果由 worker/主 agent 回填 |

## 3. 范围（In scope）与 Non-goals（Out of scope）

### 3.1 In scope（提案）

1. 响应式样式：`frontend/src/index.css`（token 复用 + 断点补齐 + 关键页面 360–1440px 修复）+ 因布局所需的 JSX class/结构微调。
2. 无障碍：全局 `:focus-visible`；关键页面/组件的语义、label、aria 属性补齐（只补缺口，不回退既有语义）；无人为依赖的对比度检查声明边界。
3. 前端测试：新增/补强的组件测试（含 API mock）+ 关键页面的 axe 断言（**axe-core 直连 + `frontend/src/test/axe.js` helper**，Q-1）。
4. Playwright（仅 chromium）：login / browsing / cart / employee 四条 smoke，API 由 `page.route` mock。
5. 覆盖率工具：`@vitest/coverage-v8` + `test:coverage` + 阈值（基线下方 2–5pp，且行覆盖 ≥75%）。
6. CI：**独立 `frontend` job**（lint/test/coverage/build/e2e，阻塞回归，`timeout-minutes: 20`）；Node 22 固定。
7. `frontend/package.json` 的 scripts/devDependencies/`engines` 更新（**新增依赖仅 `@playwright/test`、`@vitest/coverage-v8`、`axe-core`**）；`.github/workflows/ci.yml` 更新；**本 issue 重新生成并提交 `frontend/package-lock.json`**（Q-3/N-32-3）。
8. 必要文档：`frontend/README.md`（命令表）、本 issue 的计划 + issue 跟踪；**架构文档 §7（测试设施/CI 事实）在本 issue 完成时更新**（Q-7 已裁定）。

### 3.2 Non-goals（明确排除）

| 排除项 | 理由 / 证据 |
| --- | --- |
| **新建设计系统 / CSS-in-JS / Tailwind / 组件库** | D-32-4 已批准「继续以 `index.css` `:root` 为单一来源」；引入框架属未批准的范围扩大。 |
| 引入 **`vitest-axe` 以外的 a11y 依赖**（未批准）；`eslint-plugin-jsx-a11y` / oxlint a11y 规则集 | **Q-1 已裁定：用 `axe-core` 直连，不引入 `vitest-axe`（`axe-core` 已获批）；Q-4 已裁定：不加 oxlint a11y 规则** |
| **跨浏览器 e2e（firefox/webkit）** | D-32-1 已批准「仅 chromium」。 |
| **e2e 直连 MySQL / 真实后端** | D-32-1 已批准「API 用 `page.route` mock，不依赖 MySQL」。 |
| **前端引入真实浏览器视觉回归（截图对比服务）** | 未批准；`timeout-minutes: 15` 与成本约束。 |
| **后端 API、契约、DB schema 的任何改动** | 属 #31/#33；本 issue 只做前端质量与 CI。 |
| **生产构建接入 Spring Boot / SPA fallback / Docker** | 属 **#33**。 |
| **删除 legacy `src/main/webapp/**`** | 属 **#33**（已批准的破坏性动作）。 |
| **修改 JaCoCo 后端 85% 闸门或其 excludes** | 未批准；仍须满足（任何后端改动都要配测试）。 |
| **i18n / 多语言文案** | 后端与前端文案硬编码英文；未批准。 |
| **提高/降低后端覆盖率阈值** | 未批准。 |

## 4. 现状缺口与计划动作（逐一对应 scout B1/B2/B3/B4/B5）

### 4.1 样式缺口（B1）→ 提案

| 缺口 | 现状证据 | 提案动作 |
| --- | --- | --- |
| 断点只有 1024/760 | `index.css:28,80,90`（1024）、`688,828`（760） | 保留既有断点语义；**新增 `@media (max-width: 480px)`（Q-3 已裁定）但只加必要规则**，逐条说明动机；不得重做交互 |
| 宽表格在窄屏只横向滚动 | `.movie-table-wrap { overflow-x: auto }`（scout B1，约第 571-573 行） | 保持横向滚动作为兜底（不改列结构）；确认 360px 下不撑破 `grid` 容器（#30 已加 `min-width:0` 修复，见 #30 计划 §13.1 F-1） |
| 排序控件、toast、详情页返回链接的窄屏表现 | scout B1（`.movie-list-controls` 760 变 column、`.cart-toast` 760 通栏） | 逐页在 **360/1440**（AC 要求的 mobile/desktop 两档）与中宽下检查并修复；不重做交互（Q-3） |
| 无全局焦点样式 | `index.css` 仅第 527 行 `:focus-within` | 新增全局 `:focus-visible`（用 `--accent`/`--accent-border` token，暗色下也可见） |

### 4.2 无障碍缺口（B2）→ 提案

- 补 `:focus-visible`（上述）。
- 关键页面逐一核查：home / login / movies（含 detail）/ cart / checkout / confirmation / employee（#31 新增）——label、`role`、`aria-*`、heading 层级（每页一个 `h1`）。
- `RecaptchaField.jsx` 的可访问名：scout 标为**未确认**（未逐行读）——**Q-6 已裁定：先实际核查，再决定；若补则只做最小 aria，且不得破坏 `VITE_RECAPTCHA_SITE_KEY` 为空时的渲染分支**（顾客登录测试依赖该分支）；若不宜改，需在交付报告中记录。
- **边界**：jsdom 无法计算真实颜色对比度与布局；axe 在 jsdom 下会跳过/不可靠地判定 color-contrast → 必须在测试与交付报告中声明（§6.4）。

### 4.3 测试与覆盖率（B3/B5）→ 提案

- 新增 `@vitest/coverage-v8`（与 `vitest@5` 匹配的 `5.0.x`）+ `test:coverage`。
- `vitest.config.js` 增加 `coverage` 段：`provider: 'v8'`、`reporter`、`include`/`exclude`、`thresholds`（D-32-2）；**`exclude` 只列 `src/main.jsx`、`src/test/**`、`**/*.test.*`**（Q-8 已裁定）。
- **B1 修（Q-5 已裁定）**：`vitest.config.js` 显式设置 `test.exclude`，**加入 `**/e2e/**`**，并在覆写时**保留 vitest 默认排除项**（`**/node_modules/**`、`**/dist/**`、`**/cypress/**`、`**/.{idea,git,cache,output,temp}/**`、`**/{karma,rollup,webpack,vite,vitest,jest,ava,babel,nyc,cypress,tsup,build,eslint,prettier}.config.*`）；**验证证据：加入 e2e 目录后 `npm test` 用例数不变（保持 155）与 26 files**。
  - 等价可接受方案（二选一，写入交付报告）：把 Playwright 用例改名（如 `e2e/*.e2e.js`）并在 `playwright.config.js` 设 `testMatch`。**两种方案不得同时采用时互相矛盾**。
- **阈值数字待实测基线**（docs-writer **未**运行 coverage；"未确认"）→ worker 先测量再定，且必须满足「基线 -2..-5pp 且 ≥75%」；**若基线 <75% → 回主 agent（Q-10），不得先降阈值**。

### 4.4 CI（B4）→ 提案

- 前端步骤（**Q-2 已裁定：独立 `frontend` job**，不再追加到后端 job）：`timeout-minutes: 20`；`actions/setup-node@v4` + `node-version: 22` + `cache: npm`（`cache-dependency-path: frontend/package-lock.json`）+ `defaults.run.working-directory: frontend`；步骤顺序 = `npm ci` → `npm run lint` → `npm test` → `npm run test:coverage` → `npm run build` → `npx playwright install --with-deps chromium` → `npm run test:e2e`。
  - 现有后端 job 的 `timeout-minutes: 15`（`ci.yml:22`）是**按 job** 计的，**不**把 Playwright 安装塞进后端 job；后端 job 保持原样（#33 会再加 `-Pfrontend` 相关验证）。
- 后端 `mvn verify` 步骤不变（C-32-5）。

## 5. 拟修改 / 新增文件清单（已批准；最终以 worker 实际 diff 为准）

> 本清单已随 §10/§11 的裁定定稿（主 agent 2026-10-15 批准）。

### 5.1 前端源码（样式与 a11y）

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `frontend/src/index.css` | 改 | 断点补齐（1024/760 + **新增 480 但只加必要规则**，Q-3）、全局 `:focus-visible`、关键页面 360–1440px 修复；**只复用既有 `:root` token** |
| `frontend/src/features/**/*.jsx`（home/login/movies/cart/checkout/confirmation/employee、`components/**`） | 改（最小） | 仅补 a11y 缺口与布局所需的 class/结构；**不得重写既有语义**（如 combobox、radiogroup、radix 类 aria） |
| `frontend/src/features/auth/RecaptchaField.jsx` | 改（可能） | 可访问名/提示（scout 标为未确认）；**Q-6 已裁定：先核查，若补只做最小 aria 且不破坏空 `VITE_RECAPTCHA_SITE_KEY` 分支**；若不宜改，需在交付报告中记录 |

### 5.2 前端测试

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `frontend/src/test/axe.js` | 新（**Q-1 已裁定**） | **`axe-core` 直连 + 自建 helper**，导出 `expectNoA11yViolations(container)`（内部 `axe.run` 后 `expect(violations).toEqual([])`）；**不引入 `vitest-axe`**；参考 #30 抽取 `money.js` 的先例。**jsdom 规则限制与回退路径写入注释与报告** |
| 关键页面测试（`HomePage.test.jsx`、`LoginPage.test.jsx`、`MovieListPage.test.jsx`、`MovieDetailPage.test.jsx`、`CartPage.test.jsx`、`PaymentPage.test.jsx`、`ConfirmationPage.test.jsx`、员工登录页/仪表盘测试（#31 产出）） | 改/新增 | 增加 axe 断言 + 键盘可达性断言（**页面清单与关键断言见 §2 AC-32-3 / N-32-12**） |
| 既有 26 个测试文件 | 保持/最小改 | a11y 属性变更可能影响查询（如 `getByLabelText`）；**不得弱化既有断言** |

### 5.3 e2e（Playwright）

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `frontend/playwright.config.js` | 新 | **Q-6/N-32-6 已定稿**：仅 chromium project；`testDir: './e2e'`；`use: { baseURL: 'http://127.0.0.1:4173' }`；`webServer: { command: 'npm run build && npm run preview -- --port 4173 --strictPort', url: 'http://127.0.0.1:4173', reuseExistingServer: !process.env.CI }`；`reporter`；`projects` 仅 **chromium + 两个 viewport（360/1440）** |
| `frontend/e2e/login.spec.js` | 新 | 顾客登录（含失败路径）smoke；API `page.route` mock |
| `frontend/e2e/browsing.spec.js` | 新 | home → movie list → movie detail 深链/导航 smoke |
| `frontend/e2e/cart.spec.js` | 新 | 加购 → cart → checkout → confirmation smoke；API mock（含 `checkout/token`、`payment`） |
| `frontend/e2e/employee.spec.js` | 新 | 员工登录 → dashboard（metadata/add-star/add-movie/登出）smoke；依赖 #31 已合并的页面 |
| `frontend/e2e/support/*.js` | 新 | 共享 mock 路由与 helpers（避免四条 spec 重复，参照 #30 抽 `money.js` 的先例） |
| `frontend/.gitignore` | 改（**Q-5 已裁定：加**） | 忽略 Playwright 产物 `test-results/`、`playwright-report/`（当前 `.gitignore` 只忽略 `dist`/`node_modules` 等） |

### 5.4 工具链 / CI / 配置

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `frontend/package.json` | 改 | scripts：`test:coverage`、`test:e2e`；devDependencies（**已批准，仅 3 个**）：`@playwright/test`、`@vitest/coverage-v8`、`axe-core`；`engines: { "node": ">=22.12.0" }`（N-32-2：不写 `^22`，否则本机 Node 24 会告警/在 `engine-strict` 下失败） |
| `frontend/vitest.config.js` | 改 | ① `coverage` 段（provider `v8`/reporter/include/exclude/thresholds）；② **`test.exclude` 显式加入 `**/e2e/**` 并保留 vitest 默认排除项（B1/Q-5）** |
| `frontend/playwright.config.js` | 新 | 见 §5.3 |
| `.github/workflows/ci.yml` | 改 | **独立 `frontend` job**（Q-2）：`timeout-minutes: 20`、Node 22、npm 缓存指 `frontend/package-lock.json`、`working-directory: frontend`；后端 job 不变 |
| `frontend/.oxlintrc.json` | **不改（Q-4 已裁定）** | **不加** a11y 相关 lint 规则（oxlint 的 a11y 规则集与存量告警量未验证）；a11y 门交给 axe helper。**N-32-11**：`oxlint src` **不覆盖 `e2e/`** —— 计划明确 e2e **不纳入 lint**（不得声称 e2e 已被 lint 覆盖） |
| `frontend/package-lock.json` | 改 | **本 issue 重新生成并提交**（Q-3/N-32-3）：3 个新 devDependencies 必然改 lock；**本 issue 的 lock 改动属本 issue 变更**（覆盖 #30 的排除口径）。工作区已有未提交修改（npm 版本导致的 `peer` 标记规范化）——由主 agent 确定是否合入同一提交（§9 R-32-1） |

### 5.5 文档

| 路径 | 类型 | 目的 |
| --- | --- | --- |
| `docs/plans/issue-32-add-responsive-styling-accessibility-frontend-tests-and-ci.md` | 本文件 | 计划 |
| `docs/issues/issue-32-...` | 新 | issue 跟踪（见 §5.5 的实际文件名 `docs/issues/issue-32-add-responsive-styling-accessibility-frontend-tests-and-ci.md`） |
| `frontend/README.md` | 改 | 命令表（新增 `test:coverage`/`test:e2e`）、Node 22 要求、Playwright 说明 |
| `README.md` | 改（可能） | 若 CI/前端命令变化需要在根 README 体现（主要归 #33） |
| `docs/architecture/frontend-migration-contracts.md` | 改（**Q-7 已裁定：归 #32**） | §7 的两条事实（「**没有** e2e 设施」、「CI 不跑前端」）**由本 issue 完成时更新**（否则文档与代码矛盾）；#33 只补 legacy/生产相关内容（§1/§2/§8） |
| `docs/logs/...` | 实施后 | 工作流日志（本阶段不创建） |

**测试/验证命令（必须由 worker 实际运行并报告）**：`cd frontend && npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npx playwright install --with-deps chromium && npm run test:e2e`；后端 `mvn --batch-mode verify`（若本 issue 触及后端——预期不动）。

**确定性与基线证据（N-32-5/Q-9，必填）**：① 先跑 `npx vitest run --coverage --coverage.reporter=text-summary` 记录**基线**（当前未知）；② 阈值按基线 -2..-5pp 且 ≥75% 设定；③ **连续两次运行** `npm test` 与 `npm run test:coverage`，报告两次的**用例数/文件数/覆盖率数字**并断言一致；④ 实际生效/跳过的 axe 规则集逐页列出。

## 6. 验收与测试计划

### 6.1 单元/组件测试（vitest + RTL）

- 保持既有 26 个测试文件全绿（**不得弱化**）。
- 新增/补强：关键页面的状态与键盘交互；a11y 断言（§6.3）。
- 页面级测试注意：使用 `AppShell` 的测试需要 `CartStatusProvider` 包裹（架构文档 §4 测试陷阱）；使用 movie list 缓存的测试需要 `clearMovieListCache()`（架构文档 §4）。

### 6.2 覆盖率

| 项 | 计划 |
| --- | --- |
| 工具 | `@vitest/coverage-v8`（provider v8） |
| 命令 | `npm run test:coverage` |
| 基线 | **先在干净环境实测**（docs-writer 未运行 → 当前**未知**；worker 必填） |
| 阈值 | **基线下方 2–5 个百分点，且行覆盖 ≥75%**（D-32-2）；具体数字由 worker 实测后写入本文件与代码配置，并由主 agent 复核 |
| 排除（**Q-8 已裁定，完整清单**） | **仅** `src/main.jsx`、`src/test/**`、`**/*.test.*`；不得再扩大（防人为抬分，R-32-10）；完整 exclude 列表必须写进计划修订与交付报告 |
| 阻塞 | CI 必须因阈值不满足而失败（AC-32-5） |

> **风险提示**：`MovieListPage.jsx`、`PaymentPage.jsx`、`CartPage.jsx`、`AutocompleteSearch.jsx` 等大 JSX 文件会拖低阈值（scout B5）；**若实测基线 <75% → 先补测试；若仍不能达 75%，必须回主 agent（Q-10），不得先降阈值**（D-32-2 要求 ≥75%）。

### 6.3 无障碍自动检查

- 方案（**Q-1 已裁定**）：**`axe-core` 直连 + 自建 helper `frontend/src/test/axe.js` 的 `expectNoA11yViolations(container)`**；**不引入 `vitest-axe`**（依据 oracle：`vitest-axe@0.1.0` 单版本、peer 仅声明 `vitest >=0.16.0`、对 React 19 无声明；自建 helper 避开 wrapper 的 peer/维护风险）。
- **回退路径**：若 `axe-core` 在 jsdom 下出现无法解释的失败，可对该用例按**最小范围**禁用具体规则，并**在代码注释与交付报告中记录规则名 + 理由**（**不得静默 disable**）；仍不引入 `vitest-axe`（如需变更依赖选型，回主 agent）。
- 覆盖（**N-32-12 已定稿**）：`HomePage`、`LoginPage`、`MovieListPage`（含 `role="radiogroup"` 排序控件）、`MovieDetailPage`、`CartPage`、`PaymentPage`、`ConfirmationPage`、员工登录页与仪表盘页（#31）**逐页**渲染后 axe 断言（不可写「关键页」这类不可判定描述）。
- **jsdom 规则限制（如实声明）**：jsdom 无布局/无真实颜色计算 → **`color-contrast` 等依赖布局/颜色的规则不可用**；worker 必须**逐页列出实际生效/跳过的规则集**；对比度列入人工检查清单（§6.4），**不得声称自动覆盖对比度**。

### 6.4 浏览器级（Playwright）与手工检查

- 四条 smoke（D-32-1）：login / browsing / cart / employee；**仅 chromium**；API 全部 `page.route` mock。
- viewport（**Q-3 已裁定：仅两档**）：**360（移动）与 1440（桌面）**，用于 AC-32-1 的可执行证据——如 `document.documentElement.scrollWidth <= window.innerWidth` 断言、关键控件可见（#30 的表格/排序控件修复很敏感，R-32-5 的兜底必须落成 e2e 断言）。
- 键盘：至少一条用 `keyboard.press('Tab'/'Enter')` 走完 login 与主导航（AC-32-2 的可执行证据）。
- **手工项（如实声明，不自动化）**：颜色对比度、缩放手感、toast 2 秒消失观感、窄屏徽标视觉偏移（#30 已记录为需人工确认）。

### 6.5 AC → 测试/命令 映射

| AC | 证据（哪一层） | 命令 | 不可自动化的残余 |
| --- | --- | --- | --- |
| AC-32-1 | Playwright（viewport/keyboard，仅 chromium）+ 人工确认记录 | `npm run test:e2e`；人工检查表 | 真实字体、缩放手感、视觉美观 |
| AC-32-2 | 组件测试（label/role/aria）+ Playwright 键盘路径 | `npm test`；`npm run test:e2e` | 真实屏幕阅读器行为 |
| AC-32-3 | 关键页面 axe 断言（`axe-core` helper） | `npm test` | **颜色对比度**（jsdom 不可用）→ 人工 |
| AC-32-4 | 固定 Node 22 + 仅 chromium + mock API + **连续两次结果一致（用例数/覆盖率）** | `npm ci`/`npm test`/`npm run test:coverage`/`npm run test:e2e` 各两次 | 本机 Node 24 与 CI Node 22 的细微差异（以 22 为准） |
| AC-32-5 | CI workflow 中 lint→test→coverage→build→e2e 全部阻塞（独立 `frontend` job） | CI 运行结果（本任务阶段**未运行**） | — |

> 本表可直接复用到 #33 的 M2 checklist（证据链分层）；#33 只需加 legacy/生产相关条目。

## 7. 依赖

| 依赖 | 状态 | 证据/说明 |
| --- | --- | --- |
| 已有 React 页面（home/login/movies/cart/checkout/confirmation） | 已完成（#28/#29/#30/#34-#39） | `frontend/src/features/**` |
| 员工页面（employee） | **依赖 #31** | 本 issue 的 employee smoke 与 a11y 覆盖必须在 #31 合并后进行 |
| 前端 API mock 能力 | 已具备（组件测试） | `vi.stubGlobal('fetch', ...)`（架构文档 §7） |
| Node 22 LTS | 需在 CI/Docker/`engines` 固定（D-32-3） | `frontend/package.json` 现无 `engines`；**N-32-2 已定稿：`engines: { "node": ">=22.12.0" }`（CI/Docker 用 22，本地允许 ≥22.12，本机实为 v24.16.0）** |
| Playwright 浏览器二进制 | 需 CI 步骤 `npx playwright install --with-deps chromium`（D-32-1） | 现状无任何 e2e 设施；**镜像/浏览器可拉取性由 worker 在有网环境验证** |
| a11y 工具 `axe-core` | **本 issue 新增（Q-1 已批准）** | 现状：`frontend/package.json` 无 axe 依赖；`vitest-axe` **不采用**（oracle B2） |
| 覆盖率工具 `@vitest/coverage-v8` | **本 issue 新增（D-32-2 已批准）** | 与 `vitest ^5.0.3` 对齐（`5.0.x`） |

## 8. 约束

| # | 约束 | 来源 |
| --- | --- | --- |
| C-32-1 | **不新建设计系统**：design token 只在 `frontend/src/index.css` `:root`（+暗色块） | D-32-4；`index.css:1-16,33-48` |
| C-32-2 | 仅 **chromium**；e2e **不依赖 MySQL/真实后端**（`page.route` mock） | D-32-1 |
| C-32-3 | **Node 22 LTS** 固定（CI `setup-node`、Dockerfile(#33)）；`engines: { "node": ">=22.12.0" }`（不写 `^22`，避免本机 Node 24 告警） | D-32-3；N-32-2 |
| C-32-4 | 覆盖率阈值 = 实测基线 -2..-5pp **且** 行覆盖 ≥75%；CI 阻断；**exclude 仅 `src/main.jsx`、`src/test/**`、`**/*.test.*`**；**基线 <75% 必须回主 agent，不得先降阈值** | D-32-2；Q-8/Q-10 |
| C-32-5 | CI 现有后端步骤（`mvn --batch-mode --no-transfer-progress verify`）**不得被弱化**；JaCoCo 85% 闸门继续生效 | `.github/workflows/ci.yml:36`；`pom.xml:130-171` |
| C-32-6 | **前端不使用 `@SpringBootTest`/后端测试基座**；前端测试沿用 vitest+jsdom+RTL 既有模式 | 架构文档 §7；scout A4 |
| C-32-7 | 既有 a11y 语义（combobox/radiogroup/`role="alert"`/`aria-label`）**不得回退**；改动需有测试保护 | scout B2 已列既有属性清单 |
| C-32-8 | `oxlint src` 保持 exit 0 无 warning；**Q-4 已裁定：不加 a11y lint 规则**；**N-32-11：`oxlint src` 不覆盖 `e2e/`** → e2e 不纳入 lint，文档/报告不得声称已 lint | `frontend/package.json:9`；Q-4/N-32-11 |
| C-32-9 | 工作目录：所有前端命令在 `frontend/` 下执行（CI 需设 `working-directory`）；`npm ci` 依赖 `frontend/package-lock.json` | 仓库结构；`frontend/package-lock.json` 已存在 |
| C-32-10 | 不触碰后端源码/`pom.xml`/legacy；不引入 Spring Security/JWT | 架构文档 §10 AD-1；#31/#33 边界 |
| C-32-11 | **jsdom 无布局/无颜色渲染**：布局与对比度结论必须来自浏览器或人工，不得由 jsdom 断言"推断" | scout B1/B2；`vitest.config.js` |
| C-32-12 | CI 总时长：**Q-2 已裁定 —— 新建独立 `frontend` job 并设 `timeout-minutes: 20`**；后端 job 的 `timeout-minutes: 15`（`ci.yml:22`）**不改**；两 job 相互独立（无需 `needs`） | `.github/workflows/ci.yml:22`；Q-2 |
| C-32-13 | **`npm test` 必须不收集 `frontend/e2e/**`**（B1）：`vitest.config.js` 的 `test.exclude` 显式加入 `**/e2e/**` 并保留 vitest 默认排除项；**验证证据 = 用例数保持 155** | oracle B1；Q-5 |
| C-32-14 | 本批新增依赖**仅 3 个**：`@playwright/test`、`@vitest/coverage-v8`、`axe-core`；**不得引入 `vitest-axe`**（已裁定不采用）；**本 issue 重新生成并提交 `frontend/package-lock.json`** | Q-1/Q-3；主 agent 批准 |

## 9. 风险与回退方案

| # | 风险 | 影响 | 缓解 / 回退 |
| --- | --- | --- | --- |
| R-32-1 | **工作区已有 `frontend/package-lock.json` 未提交修改**（docs-writer 复核 `git status`：` M frontend/package-lock.json`）→ 可能与本 issue 的依赖安装混在一起 | 中（提交内容不清晰） | **已裁定（Q-3/N-32-3）**：本 issue **必然重生成并提交 lock**（3 个新 devDependencies）；**本批 lock 改动属本 issue 变更**（覆盖 #30 的排除口径）；工作区既存的 `peer` 标记规范化修改由主 agent 决定是否合入；worker 必须报告 lock 差异来源 |
| R-32-2 | **Playwright 下载浏览器在 CI 上耗时/易失败** | 中 | 仅 chromium + `--with-deps`；`cache: npm`；**Q-2 已裁定：独立 `frontend` job + `timeout-minutes: 20`** |
| R-32-3 | **覆盖率阈值定得过高 → 长期红**；定得过低 → 闸门无意义 | 中 | D-32-2 的量化规则（基线 -2..-5pp 且 ≥75%）+ 主 agent 复核具体数字；实测数字写入文档 |
| R-32-4 | **jsdom 下的 axe 误报/漏报**（尤其 color-contrast、landmark 嵌套） | 中 | 只把可靠规则作为闸门；不可靠项明确声明 + 手工清单；必要时按页面禁用个别规则并记录理由（**不得静默 disable**） |
| R-32-5 | **响应式修复回归既有布局**（#30 的表格宽度/排序控件修复很敏感） | 中-高 | 改动集中在 `index.css` 且以 token 为单位；`npm run build` + 组件测试 + Playwright viewport smoke 兜底；必要时回退单条 CSS 规则 |
| R-32-6 | **a11y 属性变更破坏既有测试查询**（`getByLabelText`/`getByRole` 名称变化） | 中 | 改属性时同步改测试，但**不得弱化断言**；提交前跑全量 `npm test` |
| R-32-7 | **CI 步骤顺序/job 结构与后端步骤冲突**（例如前端 build 需要后端 profile（#33）） | 中 | #33 会引入 `-Pfrontend` profile；本 issue 的 CI 改动**不得**依赖 #33 的 profile；两 issue 的 CI 改动需在同一 PR 内协同（顺序 #32 → #33） |
| R-32-11 | **B1 未修 → `npm test` 必红**（vitest 收集 `e2e/*.spec.js`） | **高（阻断 AC-32-4/AC-32-5）** | **已闭合**：C-32-13 的 `test.exclude: ['**/e2e/**', ...默认项]`（或等价改名方案）；worker 必验「用例数仍为 155」 |
| R-32-12 | **a11y helper 自建（不用 `vitest-axe`）时，`axe.run` 在 jsdom 下的 async/清理细节可能不稳** | 低-中 | helper 内部 `await axe.run(container)` 后用 `expect(violations).toEqual([])`；不确定的规则逐页记录并禁用理由；**不得静默 disable**；必要时回主 agent 评估方案 |
| R-32-8 | **Playwright 与 Vite 8 / Node 22 的版本兼容性未验证** | 低-中 | worker 先本地跑通再改 CI；版本号写入 `package.json` 并由主 agent 复核 |
| R-32-9 | `RecaptchaField` 可访问名未确认（scout 标为未知） | 低 | **Q-6 已裁定**：先实际核查再最小补 aria（不破坏空 key 分支）；若无法确认则记录为已知限制 |
| R-32-10 | 覆盖率 exclude 列表若过宽 → 人为抬分（闸门失真） | 中 | 只排除入口/工具类文件，并在文档与交付报告中列出完整 exclude 清单 |

**回退方案**：本 issue 的改动以前端配置/样式/测试为主，可用 `git revert` 单点回退；**Playwright/coverage 依赖可整体移除**（删除 devDependency + scripts + CI 步骤）而不影响运行时功能。**无破坏性操作**（不删 legacy、不改 schema）。

## 10. 已裁定问题（原未决问题 Q-1..Q-10；主 agent，2026-10-15）

> **本节全部问题已裁定**（依据 oracle 报告 §2.1/§2.2/§5 与主 agent 裁定）；逐条回填见 §11。

| # | 问题 | 主 agent 裁定（2026-10-15，全部接受 oracle 建议） | 实施要点（已写入节号） |
| --- | --- | --- | --- |
| Q-1 | **a11y 依赖选型**（D-32-4 要求写明并批准） | **`axe-core` 直连 + 自建 helper `frontend/src/test/axe.js`（`expectNoA11yViolations(container)`）；不引入 `vitest-axe`**（依据：`vitest-axe@0.1.0` peer 仅 `vitest >=0.16.0`、久未维护、对 React 19 无声明）；配**回退路径**（按最小范围禁用具体规则 + 记录理由，不得静默 disable） | §1.2/§5.2/§5.4/§6.3 |
| Q-2 | **CI 结构** | **独立 `frontend` job**：`timeout-minutes: 20`、`actions/setup-node@v4`、`node-version: 22`、npm 缓存指 `frontend/package-lock.json`、`working-directory: frontend`；步骤顺序 `npm ci → lint → test → test:coverage → build → playwright install --with-deps chromium → test:e2e`；后端 job 与 timeout 不变 | §2（AC-32-5）/§4.4/§5.4/§8 C-32-12 |
| Q-3 | 是否新增 **480px 断点**与 viewport 集合 | **新增 480px 但只加必要规则并逐条说明动机**；**Playwright 仅 360/1440 两档**（不再用 768） | §4.1/§5.1/§6.4 |
| Q-4 | 是否给 `.oxlintrc.json` 加 a11y lint 规则 | **不加**（oxlint 的 a11y 规则集与存量告警量未验证；a11y 门交给 axe helper，避免一次性清理存量告警） | §3.2/§5.4/§8 C-32-8 |
| Q-5 | Playwright 产物是否写入 `.gitignore` | **写**：`test-results/`、`playwright-report/`；**并加 vitest `test.exclude: ['**/e2e/**', ...]`（B1 必须做）**，覆写时保留 vitest 默认排除项；验证证据 = `npm test` 用例数仍为 155 | §4.3/§5.3/§5.4/§8 C-32-13 |
| Q-6 | `RecaptchaField.jsx` 可访问名是否补齐 | **先核查再决定**；若补只做**最小 aria**，**不得破坏 `VITE_RECAPTCHA_SITE_KEY` 为空时的渲染分支**（顾客登录测试依赖） | §4.2/§5.1 |
| Q-7 | 架构文档 §7 事实更新归属 | **归 #32**（完成时更新「无 e2e 设施」「CI 不跑前端」两条事实）；#33 只补 legacy/生产；**实施前不得提前改写为“已支持”** | §5.5 |
| Q-8 | 覆盖率 `include`/`exclude` 清单 | **exclude 仅** `src/main.jsx`、`src/test/**`、`**/*.test.*`；完整清单写入报告 | §6.2/§8 C-32-4 |
| Q-9 | AC-32-4 的 “deterministically” 如何证明 | **干净环境连续两次**同一命令（`npm test`/`test:coverage`/`test:e2e`）的**用例数与覆盖率数字一致**，报告给出两次结果 | §2（AC-32-4）/§6.5/§5.4 命令段 |
| Q-10 | 实测基线 **<75%** 如何处置 | **先补测试把基线抬到 ≥75%**；仍不达标则**回主 agent 重新裁定阈值，不得先降阈值** | §6.2/C-32-4 |

## 11. Oracle 建议与主 agent 决定（已回填）

> 依据：oracle 只读审查报告 `.pi/artifacts/oracle-issues-31-32-33-plans.md`（#32 章节，含 B1/B2 与 N-32-1..N-32-12）+ 主 agent 裁定（2026-10-15）。
> oracle 结论：「#32：**需修订后可批准** —— B1（vitest 收集 e2e）与 B2（a11y 依赖定稿）必须先闭合；B1 属“不修就必红”，B2 属“已批准决定要求写明”」→ 二者已按报告的最小修复写入本文档。

| 来源 | oracle 建议（摘要） | 主 agent 裁定 | 已写入本文档 |
| --- | --- | --- | --- |
| **B1** | `vitest.config.js` 显式 `test.exclude` 加入 `**/e2e/**`（覆写时保留 vitest 默认排除项），**以 `npm test` 用例数仍为 155 作为验证证据**；或等价改名方案（二选一） | **接受（必须）** | §1.2、§4.3、§5.4、§8 C-32-13、§10 Q-5、§9 R-32-11 |
| **B2** | a11y 用 **`axe-core` 直连 + 自建 helper**，**不引入 `vitest-axe`**；写明 jsdom 规则限制与回退路径 | **接受（必须）** | §1.2/§1.3 备注/§5.2/§6.3/§10 Q-1、§9 R-32-12 |
| N-32-1 | 独立 `frontend` job（timeout 20、`setup-node@v4`、node 22、npm 缓存指 `frontend/package-lock.json`、`working-directory: frontend`）；步骤顺序与现有一致 | **接受** | §2（AC-32-5）/§4.4/§5.4/§8 C-32-12 |
| N-32-2 | `engines: { node: ">=22.12.0" }`（CI/Docker 22，本地允许 ≥22.12）；AC-32-4 证据以 Node 22 为准 | **接受** | §2（AC-32-4）/§5.4/§7/§8 C-32-3 |
| N-32-3 | **本 issue 重新生成并提交 `frontend/package-lock.json`**（覆盖 #30 的排除口径，明确 lock 改动属本 issue） | **接受** | §3.1/§5.4/§8 C-32-14/§9 R-32-1/§10（→ 归入 Q-3 旁项） |
| N-32-4 | 覆盖率先测基线再定阈值（行 ≥75%，基线下方 2–5 点；基线 <75% 回主 agent，**不得先降阈值**）；exclude 仅 `src/main.jsx`、`src/test/**`、`**/*.test.*` | **接受** | §6.2/§10 Q-8/Q-10/§8 C-32-4 |
| N-32-5 | determinism 证据落成“连续两次命令用例数/覆盖率一致” | **接受** | §2（AC-32-4）/§6.5/§5.4 命令段 |
| N-32-6 | Playwright 配置：`testDir: './e2e'`、`baseURL http://127.0.0.1:4173`、`webServer: npm run build && npm run preview -- --port 4173 --strictPort`、仅 chromium + 360/1440 viewport；产物 `test-results/`、`playwright-report/` 加 `.gitignore` | **接受** | §5.3/§6.4/§10 Q-5 |
| N-32-7 | 480px 断点仅加必要规则；360/1440 断言用 `document.documentElement.scrollWidth <= innerWidth` 之类可执行断言 | **接受** | §4.1/§2（AC-32-1）/§6.4 |
| N-32-8 | **不加** oxlint a11y 规则 | **接受** | §3.2/§5.4/§8 C-32-8/§10 Q-4 |
| N-32-9 | `RecaptchaField` 先核查再最小补 aria（不破坏空 key 分支） | **接受** | §4.2/§5.1/§10 Q-6 |
| N-32-10 | 架构文档 §7 两条事实（无 e2e 设施、CI 不跑前端）由 #32 更新 | **接受** | §5.5/§10 Q-7 |
| N-32-11 | 说明 `oxlint src` 是否覆盖 `e2e/`（不覆盖） | **接受** | §5.4/§8 C-32-8 |
| N-32-12 | AC-32-3 需列出具体页面清单与关键 role/label 断言（不用笼统“关键页”） | **接受** | §2（AC-32-3）/§5.2/§6.3 |
| Q-1..Q-10 | 见 oracle 报告 §5 裁定建议表（含 a11y 选型、CI 结构、断点、oxlint、产物忽略+vitest exclude、RecaptchaField、文档归属、coverage exclude、determinism、基线 <75% 预案） | **全部接受** | §10（已转为「已裁定问题」表） |
| *新增依赖* | oracle 建议的依赖与版本：`@playwright/test` 1.64.1、`@vitest/coverage-v8` 5.0.x、`axe-core`（不用 `vitest-axe@0.1.0`） | **接受：共 3 个 devDependencies（+ Playwright 浏览器下载）** | §1.3 备注/§3.1/§5.4/§7/§8 C-32-14 |
| *本批修订* | 不改变 #32 的验收范围 | **接受：AC-32-1..AC-32-5 范围不变** | §2、§12 |

## 12. 状态（批准时记录 → 已由 §13 更新为实施完成）

- **文档状态（批准时）：已批准（主 agent，2026-10-15）— 可交 worker 实现。** B1/B2 已闭合（§8 C-32-13/§6.3），N-32-1..N-32-12 与 Q-1..Q-10 已全部裁定（§10/§11）；**本批修订未改变 AC-32-1..AC-32-5**。
- **测试状态（批准时）：未运行任何测试，也未运行覆盖率命令。** 前端**实际覆盖率基线为未知** → D-32-2 的**具体阈值数字**已在实施后确定并回填（**lines 阈值 = 88**，见 §13.4；实测行覆盖 92.35%）。
- **实现前状态（批准时）**：无 Playwright 配置、无 e2e 目录、无 `@vitest/coverage-v8`/`axe-core`、无 `test:coverage`/`test:e2e` 脚本、CI 无 Node 步骤（均已核实）——**现已全部落地**（§13.1）。
- **批准时「仍由 worker 确认」的点**：已全部在 §13 回填（覆盖率数字/阈值、`test.exclude` 方式、CI job 结构、Node 22 与 Playwright 兼容性、axe 规则集），或记为残余项（§13.5）。
- **下一步（现已完成）**：worker 已按本文档实现 #32（顺序 #31 → #32 → #33）→ 三个 reviewer 并行审查（无 Blocker）→ 同一 worker 修复并复验 → docs-writer 依实际 diff 与测试输出建立 §13 与工作流日志 `docs/logs/2026-10-15-issues-31-32-33-m2-completion.md`。
- **只写范围**：本文档由 docs-writer 维护；docs-writer **未**修改任何源码/配置/CI，**未**安装依赖，**未**运行测试/覆盖率/构建（§13 数字来自主 agent 终验实测与本轮只读复核），**未**创建或修改 GitHub issue/PR，**未**提交或推送。

## 13. 实现结果与计划差异（主 agent 终验后回填，2026-10-15）

> **状态：实施完成，已通过主 agent 终验（2026-10-15）；已提交（`092147a`/`fb65eda`）、已推送、**PR #42 已于 2026-10-16 合并（merge commit `d618b64`）**、**issue #32 已关闭**；CI 首跑（head `fb65eda`）两个 job 均 SUCCESS，合并后 `main`（`d618b64`）双 job 亦 SUCCESS；里程碑 Phase 2 已关闭（§13.4）。**
> 证据来源：主 agent 亲跑的终验命令与输出（标注「主 agent 实测」）、Node 22 容器内的复跑证据、主 agent 核验的 PR #42 合并结果与 CI 运行结果，以及本轮 docs-writer 的**只读**复核（`git status`/`git diff`、配置文件与测试源码内容）。docs-writer **未**运行任何 npm 命令。

### 13.1 实际改动文件（相对 §5 计划清单）

**前端源码 / 样式**

| 路径 | 状态 | 说明 |
| --- | --- | --- |
| `frontend/src/index.css` | 改（与计划一致，含 1 处评审修复） | 全局 `:focus-visible`（`outline: 2px solid var(--accent)`）；**新增 `@media (max-width: 480px)`** 仅含必要规则（header/footer padding、`.site-footer` gap、metadata 卡片与表格 padding）；**评审修复**：暗色块内新增 `.form-error`/`.logout-control [role='alert']`/`.autocomplete [role='alert']` 的 `#f87171` 与 `.form-success` 的 `#4ade80`（≥4.5:1）；删除死 CSS `.route-placeholder`/`.route-placeholder__id` 与 480px 冲突规则 |
| `frontend/src/features/cart/CartStatusProvider.jsx` | 改（**计划外，评审修复**） | toast 的 `role="status"` 从内部 `<p>` **移到容器** `<div className="cart-toast">`：`role="status"` 是 landmark（`region` 规则），包住整条 toast 后不再触发「内容不在 landmark 内」告警 |
| `frontend/src/features/**/**.test.jsx`（Home/Login/MovieList/MovieDetail/Cart/Payment/Confirmation） | 改（与计划一致） | 各 +1 条 axe 断言（`expectNoA11yViolations`），另有键盘/语义断言保持 |
| `frontend/src/App.test.jsx` | 改 | +2 条 **shell 级** axe 断言（`expectNoShellA11yViolations`，`region` 规则启用） |
| `frontend/src/test/axe.js` | 新（与计划一致） | `axe-core` 直连 + 自建 helper（**不引入 `vitest-axe`**） |
| `frontend/src/features/auth/RecaptchaField.jsx` | **未改** | 实际核查后未发现需补的可访问名缺口（Q-6 的「先核查再决定」结论：不改） |

**测试 / e2e / 工具链**

| 路径 | 状态 | 说明 |
| --- | --- | --- |
| `frontend/e2e/login.spec.js` | 新 | 2 个用例：键盘登录（`keyboard.type`/`Tab`/`Enter`）成功；失败凭据文案 |
| `frontend/e2e/browsing.spec.js` | 新 | 2 个用例：home → movie detail；**键盘**导航打开列表 |
| `frontend/e2e/cart.spec.js` | 新 | 1 个用例：加购 → cart → checkout → confirmation |
| `frontend/e2e/employee.spec.js` | 新 | 1 个用例：员工登录 → metadata → add-star → 登出 |
| `frontend/e2e/support/api.js` | 新 | 有状态 `page.route` mock（**契约保真**：customer session `displayName: null`、`/boot/confirmation` 无订单时 **204**、详情页 `movie_rating` 为**字符串**、未 mock 路径返回 404 JSON）＋ `expectNoHorizontalOverflow(page)` 断言 |
| `frontend/playwright.config.js` | 新 | 仅 chromium，两个 project（`chromium-360` / `chromium-1440`）；`baseURL http://127.0.0.1:4173`；`webServer.command = npm run build && npm run preview -- --host 127.0.0.1 --port 4173 --strictPort`；CI 下 `retries: 1`、`forbidOnly`、html reporter；`trace: 'on-first-retry'`；`timeout: 120_000` |
| `frontend/vitest.config.js` | 改 | ① `test.exclude` 显式列入 `**/e2e/**` 并**重述 vitest 默认排除项**（B1/Q-5 方案 1）；② `coverage`：`provider 'v8'`、`include: ['src/**/*.{js,jsx}']`、`exclude: ['src/main.jsx','src/test/**','**/*.test.*']`、`thresholds: { lines: 88 }` |
| `frontend/package.json` | 改 | scripts `test:coverage`/`test:e2e`；`engines.node >= 22.12.0`；新增 3 个 devDependencies：`@playwright/test ^1.64.1`、`@vitest/coverage-v8 ^5.0.5`、`axe-core ^4.14.0` |
| `frontend/package-lock.json` | 改 | 本 issue 重新生成并提交（3 个新依赖） |
| `frontend/.gitignore` | 改 | 新增 `test-results`、`playwright-report`、`coverage` |
| `.github/workflows/ci.yml` | 改 | 新增独立 `frontend` job（`timeout-minutes: 20`、`actions/setup-node@v4` + Node 22 + npm 缓存指 `frontend/package-lock.json`、`working-directory: frontend`），步骤 `npm ci → lint → test → test:coverage → build → npx playwright install --with-deps chromium → test:e2e`；后端 job 与其 timeout 未改 |
| `frontend/.oxlintrc.json` | **未改**（与 Q-4 裁定一致） | 未加 a11y lint 规则；`oxlint src` 仍不覆盖 `e2e/` |
| `frontend/vite.config.js` | **未改**（#33 Q-5 裁定） | 保持 `base: '/'` |
| `frontend/README.md`、`README.md` | 改 | 命令表（`test:coverage`/`test:e2e`）、Node 22 要求、`dist` 不提交与 `-Pfrontend` 打包说明（与 #33 同批编辑） |

### 13.2 与计划文本的差异（实现后确认）

| # | 计划原文 | 实际实现 | 说明 |
| --- | --- | --- | --- |
| 1 | §5.2/§6.3：单个 helper `expectNoA11yViolations(container)` | 实际为**两个导出**：`expectNoA11yViolations`（`region` 规则**禁用**——页面组件单独渲染时没有 `AppShell` 的 `<main>`）与 `expectNoShellA11yViolations`（`region` **启用**——用于包含自身 landmark 的 `App`/shell 渲染树） | 若只保留单 helper，要么全量断言 `region` 导致所有页面级测试误报，要么全局禁用导致 landmark 覆盖丢失；计划 §6.3 已允许「按最小范围禁用具体规则 + 记录理由」，此处按测试树类型显式区分并写入代码注释 |
| 2 | §6.2/D-32-2：阈值 = 基线下方 2–5pp **且** 行覆盖 ≥75%（具体数字由 worker 实测后定） | **阈值定稿为 `lines: 88`**；实测行覆盖 **92.35%**（846/916） | 满足规则：92.35 − 88 = **4.35pp**，位于 2–5pp 区间且远高于 75% 下限。**阈值只对 lines 生效**（statements/branches/functions 未设阈值），计划未要求 |
| 3 | §6.2/Q-8：只需定义 `exclude`（`src/main.jsx`、`src/test/**`、`**/*.test.*`） | 另显式定义 `include: ['src/**/*.{js,jsx}']` | 显式 `include` 使**未被测试导入的源文件也计入分母**（现例：`frontend/src/App.jsx` 行覆盖 **0%**，计入 916 行分母）。这是**有意**选择：不缩小分母以抬分（R-32-10 口径） |
| 4 | §5.4 命令段：「B1 验证证据 = `npm test` 用例数**仍为 155**」 | 实测 **195 tests / 30 files** | 155 是 #31 实施前的基线；B1 的实质判据是「vitest **不收集** `e2e/**`」——若收集，`import { test } from '@playwright/test'` 会直接失败，`npm test` 不可能全绿（实测全绿 195）。**不得**再用「用例数 155」作为 B1 判据 |
| 5 | §5.3/§6.4：四条 smoke；计划未给用例总数 | 实际 **4 个 spec 文件、6 个 `test` 用例**（login 2 / browsing 2 / cart 1 / employee 1）× **2 个 viewport = 12 次执行** | 主 agent 报告中的「12 passed」与此计数一致；任务说明里「5 条 spec」为笔误（第 5 个文件是 `e2e/support/api.js`，是共享 mock，不是 spec） |
| 6 | §5.4：`@vitest/coverage-v8` 与 `vitest ^5.0.3` 对齐（`5.0.x`） | `@vitest/coverage-v8 ^5.0.5` 使 **vitest 5.0.3 → 5.0.5** | peer 要求导致的版本抬升，**已接受**；升级后 `npm test`/覆盖率/构建在 Node 24 与 Node 22 下均复跑通过（§13.3） |
| 7 | §5.1：`index.css` 新增 480px 断点 | 已按「只加必要规则」实现，且**修正了一处 480px 与既有规则的冲突** | 评审发现的规则冲突已按最小改动修正（见 §13.1 表） |

### 13.3 终验证据（AC-32-1..AC-32-5）

| AC | 证据 | 结果 | 来源 |
| --- | --- | --- | --- |
| AC-32-1 | Playwright chromium 两档 viewport（360/1440）+ `expectNoHorizontalOverflow`；480px 断点规则；生产 smoke 的 10 条深链 200 text/html（#33） | `npm run test:e2e` → **12 passed**（本地 Node 24 与 Node 22 容器一致） | 主 agent 实测 |
| AC-32-2 | 全局 `:focus-visible`；组件测试的 label/role 断言；e2e 键盘路径（login `keyboard.type/Tab/Enter`、browsing `Tab`+`Enter` 打开列表） | 前端 **195 passed**；e2e 12 passed（含 2 条键盘用例） | 主 agent 实测 |
| AC-32-3 | `frontend/src/test/axe.js` + 9 个页面的 axe 断言（Home/Login/MovieList/MovieDetail/Cart/Payment/Confirmation/员工登录/员工仪表盘）＋ `App.test.jsx` 的 2 条 shell 断言 | 前端 195 passed（axe 违规集合必须为空） | 主 agent 实测 |
| AC-32-4 | Node 22（`engines >= 22.12.0`；CI `node-version: 22`）+ 仅 chromium + 全 API mock；**连续两次同一命令用例数/覆盖率一致** | 本地 Node 24：`npm test` **195 passed / 30 files**、`npm run test:coverage` **Lines 92.35% (846/916)**、Statements 90.05%、Branches 82.16%、Functions 91.17%、`npm run lint` 干净、`npm run build` 成功；**Node 22 容器（`node:22-slim`，v22.23.3）连续两次结果一致**：lint 0、195 tests / 30 files、Lines 92.35%、build 成功 | 主 agent 实测（本地 + 容器） |
| AC-32-5 | `.github/workflows/ci.yml` 新增独立 `frontend` job，步骤按 lint→test→coverage→build→e2e 全部阻塞回归；后端 job 保持 `mvn verify` | 配置已落地（docs-writer 复核 diff）；**CI 首跑（head `fb65eda`）已发生且两 job 均 SUCCESS**：`Frontend (lint, test, coverage, build, e2e)` **SUCCESS**（新 job 首次运行）、`Build and test (Java 11)` **SUCCESS**；**合并后 `main`（`d618b64`）上双 job 仍 SUCCESS（闸门已生效）** | 主 agent 核验的 GitHub CI 运行结果（docs-writer 未直接访问 GitHub） |

> **后端回归**（本 issue 未改后端，仍作为整体闸门复核）：`mvn --batch-mode --no-transfer-progress verify` → BUILD SUCCESS，`Tests run: 127`，`All coverage checks have been met.`，BUNDLE LINE 968/110/1078 = **0.8980**（闸门 0.85）。

### 13.4 PR、CI 首跑与合并结果（原「待 PR 后回填」项，**已回填 2026-10-15；合并结果 2026-10-16 回填**）

| 项 | 状态 |
| --- | --- |
| **PR 链接 / 提交号 / 合并** | ✅ **已回填**：实现提交 `092147a`、文档提交 `fb65eda`；分支 `codex/issue-31-32-33-m2-completion`（HEAD `fb65eda`）已推送；PR **#42** — https://github.com/PNZ-z/ai-powered-Fabflix/pull/42（标题 `Complete the React migration: employee dashboard, quality gates, and production build`；base `main`）；**已于 2026-10-16 合并**（merge commit **`d618b64`**；`mergedAt` = 2026-10-16T04:57:00Z） |
| **CI 首跑结果（后端 job + `frontend` job）** | ✅ **已回填**：head `fb65eda` 上 `Build and test (Java 11)` → **SUCCESS**；`Frontend (lint, test, coverage, build, e2e)` → **SUCCESS**（**该 job 自加入 ci.yml 后的第一次运行**）。**合并后 `main`（`d618b64`）上双 job 亦 SUCCESS**（即本 issue 新增的前端闸门在默认分支上生效）。来源：主 agent 核验的 GitHub CI 结果（docs-writer 未直接访问 GitHub） |
| **合并后的后续状态** | ✅ **已回填**：远端分支 `codex/issue-31-32-33-m2-completion` **已删除**（origin 仅剩 `main`）；**issue #32 已关闭**（GitHub 依 `Closes #32` 自动关闭）；**里程碑 `Phase 2 — React Frontend Migration`（#2）已关闭**（`open_issues` = 0、`closed_issues` = 10、`closed_at` = 2026-10-16T05:00:27Z） |
| Playwright 浏览器下载耗时 / job 实际时长 | **仍未观测**（本次未回填具体时长数据；`timeout-minutes: 20` 为计划值） |

### 13.5 残余风险、未验证项与后续事项

| # | 项 | 状态 |
| --- | --- | --- |
| 1 | **颜色对比度** | jsdom 无布局/无颜色计算 → `color-contrast` 在 helper 中**全局禁用**；新增暗色色值按 ≥4.5:1 选择，**仍需人工在浏览器确认**（不得声称已自动覆盖对比度） |
| 2 | **axe 在 jsdom 下的规则边界** | `region` 规则按渲染树区分（页面级禁用 / shell 级启用）；其余依赖布局的规则未被专门处理 → 覆盖强度以「违规集合为空」为准，不等价于真实浏览器审计 |
| 3 | **e2e 全部 mock API** | 无真实后端/MySQL 参与，故不能证明真实契约（`page.route` 用「契约保真」的 mock 降低偏差风险，但仍非真实往返）；真实存储过程往返见 #31 §13.5 |
| 4 | **`frontend/src/App.jsx` 行覆盖 0%** | 计入覆盖率分母（`include: src/**`）；不禁用/排除以抬分 |
| 5 | `RecaptchaField.jsx` | 核查后未改（Q-6 结论：无必要）；未在真实浏览器验证 reCAPTCHA 渲染（依赖外部 key，属 #30 遗留的人工地带） |
| 6 | Nightly/跨浏览器/视觉回归 | 计划 Non-goal（仅 chromium、无截图对比）；本 issue 不引入 |

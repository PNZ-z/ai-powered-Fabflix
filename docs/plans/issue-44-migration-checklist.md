# Issue #44 迁移 checklist（S1–S7：目标 / 验证命令 / 回退点 / 完成分支）

> **用途**：把 Option A 迁移（Boot 2.7.18 + Java 11 + `javax` → **Boot 3.5.x + Java 17 + `jakarta` + JDK 21 工具链**）的 7 个步骤做成**可逐条打勾的执行清单**。每一步都写明 (a) 一句话目标、(b) 精确验证命令、(c) 回退点、(d) 完成分支/commit 与状态。
> **上位文档**（本 checklist 不改变其任何已批准范围与验收）：
> - epic：**[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)**（`[Epic] Upgrade to Spring Boot 3 / Java 17 / Jakarta`，milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)，**OPEN**）
> - 子 issue：**S1 [#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)** · **S2 [#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)** · **S3 [#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)** · **S4 [#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49)** · **S5 [#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)** · **S6 [#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)** · **S7 [#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52)**
> - 拆分总览：`docs/plans/issue-44-migration-steps.md`（§2 依赖与顺序、§3 步骤总表、§4 各步 DoD/回退点）
> - 已批准计划：`docs/plans/ai-option-a-boot3-migration.md`；约束：`docs/architecture/frontend-migration-contracts.md`
> - **迁移前基线（所有“不下降”判据的唯一基准）**：`docs/logs/2026-10-20-issue-46-baseline.md`
>
> **状态图例**：`未开始` / `基线已测量（待 PR 合并后标记完成）` / `已实施（待 PR 合并）` / `实施中` / `已完成（PR #n 已合并 @ <sha>）` / `阻塞（原因）`。
> **本文件不表示整轮迁移已完成**：**2026-10-20 事实** —— **S1 已合并**（PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53)，merge `9696848`）、**S2 已合并**（PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55)，merge `55f8d79`）、**S3 已合并**（PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56)，merge `70fb78b`；#48 已 CLOSED；18 文件：`pom.xml` 两处 + 11 个 `src/main/java` import + 守卫测试 + 3 个测试 import + `ci.yml` 3 行 + `Dockerfile` 2 行）、**S4 已合并**（PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)，merge `9a7705c`；#49 已 CLOSED；CI 双 job SUCCESS；`pom.xml` enforcer + `README.md`）；**S5 已实施（2026-10-20），待 PR 合并**（`@MockBean`→`@MockitoBean`，9 文件 / 11 处 + 9 import）；**S6–S7 的 Scope / AC 仍未执行**。每一步仍需各自走 docs-writer 计划 → oracle 只读审查 → 主 agent 批准 → 单一 worker → 3 路 reviewer → 修复与复验 → 日志的完整流程。**合并事实取自 `git log --merges` 与 `gh pr view/checks`（本轮只读复核）；S1/S2 的 PR CI 结果本轮未单独核验**，S3 与 S4 的 PR CI 双 job 结果已核验后回填（见 §S3、§S4）。

## 状态总表

| 步骤 | Issue | 状态 | 完成分支 / commit | 证据 / 记录 |
| --- | --- | --- | --- | --- |
| **S1** 迁移前基线 + checklist | [#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46) | **已完成（PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53) 已合并 @ `9696848`）** | `codex/issue-46-baseline`（基于 `main` @ `fff314f88455216122912f07c26706f70dd63b62`）；merge commit `9696848` | `docs/logs/2026-10-20-issue-46-baseline.md` + 本文件 |
| **S2** 非 Spring 依赖就绪 | [#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47) | **已完成（PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55) 已合并 @ `55f8d79`）** | `codex/issue-47-dependency-readiness`（基于 `main` @ `9eea664`）；merge commit `55f8d79` | `docs/logs/2026-10-20-issue-47-dependency-readiness.md`（AC-1..AC-5 回填见 `docs/issues/issue-47-dependency-readiness.md`） |
| **S3** 核心 Boot 3 + Jakarta 升级 | [#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48) | **已完成（PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 已合并 @ `70fb78b`）**；#48 已 **CLOSED** | `codex/issue-48-core-boot3-jakarta`（基于 `main` @ `55f8d79`）；merge commit `70fb78b` | `docs/logs/2026-10-20-issue-48-core-upgrade.md`（AC-1..AC-9 回填见 `docs/issues/issue-48-core-boot3-jakarta.md`，**AC-6 已 ✅**）；行为差异与未证明项见该日志 §1.3 |
| **S4** JDK 21 强制与镜像取证 | [#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49) | **已完成（PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57) 已合并 @ `9a7705c`）**；#49 已 **CLOSED** | `codex/issue-49-jdk21-toolchain`（基于 `70fb78b`）；merge commit `9a7705ca2bb9f5b4dc0fb1a20a4413a0c46aa0b6` | `docs/logs/2026-10-20-issue-49-jdk21-toolchain.md`（AC-1..AC-6 回填见 `docs/issues/issue-49-jdk21-toolchain.md`）；**PR CI 双 job SUCCESS**（run `39504525702`） |
| **S5** `@MockBean` → `@MockitoBean` | [#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50) | **已实施（2026-10-20），待 PR 合并** | `codex/issue-50-mockito-annotation`（基于 `9a7705c`；**未提交/未推送/无 PR**，commit SHA 待提交后回填） | `docs/logs/2026-10-20-issue-50-mockito-annotation-cleanup.md`（AC-1..AC-5 回填见 `docs/issues/issue-50-mockito-annotation-cleanup.md`） |
| **S6** 回归与生产验证 | [#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51) | **未开始** | `（待填：codex/issue-51-…）` | `（待填）` |
| **S7** 收尾（文档/状态/日志） | [#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52) | **未开始** | `（待填：codex/issue-52-…）` | `（待填）` |

> **执行顺序硬约束**（`issue-44-migration-steps.md` §2）：S1 ∥ S2 → S3 → (S4 ∥ S5) → S6 → S7。每步**一个独立 PR**，可单独 revert。

## 通用环境前缀（S3 / S4 / S6 的所有 Maven 命令**必须**带）

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
# 或只对单条命令生效（不改当前 shell）：
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify
```

> **不要**用 `/usr/libexec/java_home -v 21` 判定 JDK 21 是否存在：Homebrew 的 `openjdk@21` 是 keg-only 且未注册，该命令会**静默返回 JDK 17**（见 `docs/logs/2026-10-20-issue-46-baseline.md` §3.1）。真实判据：`/opt/homebrew/opt/openjdk@21/bin/java -version`。

---

## 2026-10-20 覆盖率门槛收紧（0.90 / 90）与新的比较基线

> **本节是 S2–S7 的唯一比较基线。** 2026-10-20 **经用户批准**，后端 JaCoCo 门槛 `BUNDLE/LINE COVEREDRATIO` 由 **0.85 → 0.90**、前端 `vitest` `thresholds.lines` 由 **88 → 90**（独立 PR，分支 `codex/coverage-threshold-90`，**迁移前置变更**，不属于 S1–S7 任一步的范围）。

| 门禁 | 收紧后门槛 | 收紧后实测（2026-10-20） | 收紧前快照（S1 `docs/logs/2026-10-20-issue-46-baseline.md`） |
| --- | --- | --- | --- |
| 后端 JaCoCo `BUNDLE / LINE` | **≥ 0.90** | **134 tests / 0 failures**；**988 covered / 90 missed / 1078 total = 0.9165** | 127 tests；968 / 110 / 1078 = 0.8980 |
| 前端 `thresholds.lines` | **≥ 90** | **195 tests / 30 files**；**Lines 92.35%（846/916）** | 195 tests / 30 files；Lines 92.35% |
| Playwright e2e | 不涉及门槛变更 | 12 passed | 12 passed |

- **S3/S6 的“数量与覆盖率不下降”一律对比本表右侧的收紧后数字**（后端 ≥134 用例、LINE ≥ 0.9165；前端 195 用例、Lines ≥ 90）。S1 记录的 **127 / 0.8980 仍然保留为收紧前的历史快照**，但它**不再作为 S3/S6 的对照基线**（S1 基线记录自身 §对比规则 第 6 条的失效条件已被本次 `pom.xml` 与测试数量变更触发，故由本表接管）。
- **后端余量只有 N ≤ 19 行**：`988/(1078+N) ≥ 0.90 ⇒ N ≤ 19`。因此**后续任何新增生产代码都必须同时配测试**，且**不得扩 `excludes`（仍为 8 项）、不得降阈值**。
- **前端余量 2.35pp**（92.35 − 90）：新增页面/组件计入分母，`coverage.include`/`exclude` 不得为抬分而扩大（契约文档 §7）。
- **已知覆盖缺口（本次不处理，不视为回归）**：

| 类 | 行覆盖（`target/site/jacoco/jacoco.csv`，2026-10-20） | 说明 |
| --- | --- | --- |
| `admin/AdminRepository` | **0 covered / 39 missed** | 既有缺口，原记录见 `docs/plans/issue-31-migrate-employee-dashboard.md:68`（无 `AdminRepositoryTest`，metadata 遍历与 `add_movie` 解析未覆盖）；保留原记录，本次不改 |
| `auth/GoogleRecaptchaVerifier` | **0 covered / 17 missed** | **本次补登记**：类上有 `@Profile("!local")`，测试**从未实例化**它（`CustomerAuthControllerTest` 注入的是 `RecaptchaVerifier` 接口的 mock，`LocalRecaptchaVerifierTest` 只测 local 实现），因此它那 17 行（含构造器、超时/空 token 早返回、`RestTemplate` 调用与响应解析）计入分母但永远无人触达 |
| `movie/MovieListRequest` | 38 covered / 14 missed | 既有缺口 |
| `movie/MovieSummaryStar` | 6 covered / 6 missed | 既有缺口 |
| `search/SearchService` | 26 covered / 4 missed | 既有缺口 |
| `movie/MovieListQueryFactory` | 45 covered / 3 missed | 既有缺口 |

> 补充证据：`docs/logs/2026-10-20-coverage-threshold-90.md`（本轮实测与复核记录）。收紧门槛时新增/修改的 5 个后端测试文件：`AdminApiExceptionHandlerTest`、`EmployeeRepositoryTest`、`GenreServiceTest`、`AutocompleteRepositoryTest` 与 `CartControllerTest`（后者新增 1 个用例，驱动 `CommerceApiExceptionHandler` 的 **500 分支**）。

---

## S1 — 记录迁移前基线并建立 checklist（[#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)）

**(a) 目标**：在**完全未改动**的 Boot 2.7.18 树上复跑后端/前端/e2e 门禁并逐字记录，作为 S3/S6 “数量与覆盖率不下降”的唯一基准；同时建立本 checklist 与各步回退点。

**(b) 验证命令**（主 agent 已于 2026-10-20 全部执行，结果见基线记录）

```bash
git status --short && git rev-parse HEAD && git branch --show-current
mvn -version
/opt/homebrew/opt/openjdk@21/bin/java -version
mvn --batch-mode --no-transfer-progress verify
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify
cd frontend && npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e
cd .. && git status --short   # 必须仍只有与源码无关的未跟踪条目（本机为 `?? .idea/`）
```

**已记录的基线数字**（`docs/logs/2026-10-20-issue-46-baseline.md`）：后端 **127 / 0 / 0 / 0**、JaCoCo LINE **968 / 110 / 1078 = 0.8980**（JDK 27 与 JDK 21 **一致**）；前端 **30 files / 195 tests**、**Lines 92.35% (846/916)**、e2e **12 passed**。

> **⚠️ 该基线已被 2026-10-20 的门槛收紧取代**：这些数字是**收紧前**（门槛 0.85 / 88）的快照，仍作为历史记录保留在本节，但 **S2–S7 的对照基准改为上方「2026-10-20 覆盖率门槛收紧（0.90 / 90）与新的比较基线」表**（后端 134 / 0.9165、前端 195 / 92.35%）。依据：基线记录自身的对比规则第 6 条 —— 在 S3 前改动 `pom.xml`（本次把 JaCoCo 阈值改为 0.90）或测试数量（127 → 134），基线即失效，须由主 agent 决定是否重取。

**(c) 回退点**：纯文档 → 删除本 PR 的 4 个 `docs/` 文件即回退，**不影响任何构建/运行/CI**。

**(d) 完成分支 / commit**：分支 `codex/issue-46-baseline`（基于 `main` @ `fff314f`）；**已随 PR [#53](https://github.com/PNZ-z/ai-powered-Fabflix/pull/53) 合并，merge commit `9696848`**；证据 = `docs/logs/2026-10-20-issue-46-baseline.md` + 本文件。

---

## S2 — 在 Boot 2.7 基线上验证非 Spring 依赖就绪（[#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)）

**(a) 目标**：在**仍为 Boot 2.7** 的树上把 `mysql-connector-j` 定版并验证（含 jasypt 在 JDK 21 下的检查），把“JDBC 行为差异”从“Boot 3 行为差异”里分离出来。

**(b) 验证命令**

```bash
# 1) 定版记录（必须写明理由与最终版本）
grep -n "mysql-connector-j" -A3 pom.xml

# 2) 后端全绿且用例数不下降（与 S1 基线同 JDK 对比）
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify   # 期望 Tests run ≥ 134 / LINE ≥ 0.9165（收紧后基线；127 / 0.8980 为收紧前快照）

# 3) jasypt 在 JDK 21 下可解密（无 IllegalAccessError / InaccessibleObjectException）
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress test -Dtest='*Jasypt*'

# 4) 真 MySQL smoke：读/写路径的日期与时区语义
docker compose up -d mysql
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT CURDATE(), NOW();"
# 通过 app 跑一次下单或员工销售记录路径，确认无时区偏移、CURDATE() 偏差、新增致命 SSL 警告
docker compose down        # 保留数据卷；如需彻底清理再 -v（会丢数据，须主 agent 批准）
```

**(c) 回退点**：单个依赖版本号 → revert 一行即可。若 9.x 在 Boot 2.7 基线上暴露行为差异，**保持 `8.3.0` 并记录**即为合规回退（不阻塞 S3；此时 AC-6 的 JDBC 分支按 P2-7 不触发）。**本步未采用此回退**（见下方实际结果）。

**(d) 完成分支 / commit**：`codex/issue-47-dependency-readiness`；基线 commit `9eea6645a4f14004e994848f0daf8741f2909c34`（`Merge PR #54`）；**已随 PR [#55](https://github.com/PNZ-z/ai-powered-Fabflix/pull/55) 合并，merge commit `55f8d79`**。

**实际执行结果（2026-10-20，worker 执行 + 主 agent 核验；证据 `docs/logs/2026-10-20-issue-47-dependency-readiness.md`）**

| 项 | 结果 |
| --- | --- |
| 定版 | **`com.mysql:mysql-connector-j` 8.3.0 → 9.7.0**（1 行）。理由：9.7.0 = **目标 Boot 3.5.15 BOM 的 `mysql.version`**（使 S3 不再动驱动，P1-8）。备选方案（保持 8.3.0）**未采用** |
| 兼容性证据 | Maven Central jar/pom **200**（SHA-256 `0353648e…`）；`Driver.class` = **major 52（Java 8）**，全 jar 1066 个 `.class` 无 > 52，无 `module-info.class`；上游 `build.xml`/`README` “Java 8 required”。**真实 Java 11.0.31 运行时 smoke 通过**（另经主 agent 在 18086 重跑闭合） |
| 门禁（JDK 21） | `mvn --batch-mode --no-transfer-progress verify` → **BUILD SUCCESS**；**134 tests / 0 failures / 0 errors / 0 skipped**；JaCoCo **988/1078 = 0.9165**（门槛 0.90） |
| 真库 smoke | ✅ **已执行**：9.7.0（8081）下单 → `sales.id=13562, sale_date=2026-10-20` == 响应 `order_date=2026-10-20`；员工 metadata 200（10 表含 `sales`，`sale_date DATE(10)`）；日志无 SSL/连接告警。**A/B 对照 8.3.0**（18084，`id=13563`）三维度一致 |
| jasypt on JDK 21 | ✅ 通过：`-Dtest=JasyptPasswordVerifierTest` → 1 test / BUILD SUCCESS，无 `IllegalAccessError` / `InaccessibleObjectException`；**未替换 jasypt** |
| diff 范围 | `git diff` = `pom.xml` 1 行 + 本步 `docs/`；无 `src/**`、无测试、无 CI、无 `Dockerfile`、无其他依赖 |
| **未验证（顺延 S6）** | 驱动 `DATE`→Java 的**读取**映射（`src/main` **无** `SELECT … FROM sales`，唯一 `sales` 用法是 `PaymentRepository:19` 的 `INSERT … CURDATE()`）；跨日/TZ 边界；连接池/并发；TLS 实际协商状态（无 `sslMode`，默认 `PREFERRED`） |
| 环境副作用 | dev 库 `sales` 新增 4 行：**`13562`（9.7.0）/ `13563`（8.3.0）/ `13564`（Java 11 @18085）/ `13565`（Java 11 @18086）**，`customer_id=900001`。清理：`DELETE FROM sales WHERE id IN (13562,13563,13564,13565);`（`database/dev-seed.sql` 只在数据卷首次初始化时执行）。**未执行 `docker compose down`**（MySQL 原已 `Up 7 days (healthy)`，保持原状） |
| 引用更正 | worker 报告把 134 / 0.9165 称为“S1 基线”属**引用错误**：S1 快照为 **127 / 0.8980**（门槛 0.85）；**134 / 0.9165 是 2026-10-20 门槛收紧后的新比较基线**（见上方门槛表）。本步实际对齐的是后者 |

> **对 S3 的直接影响**：驱动已定版 `9.7.0`，S3 **必须保留该显式 `<version>`**（不得被 Boot 3.5 BOM 覆盖），并在 PR 里用 `mvn dependency:tree` 贴出 `mysql-connector-j` 实际解析行作为复核证据（本步只能在 Maven Central 的 pom 文本层面核对 3.5.15 BOM —— 本地 `.m2` 仅有 `2.7.18`）。

---

## S3 — 一次性原子升级：Boot 3.5 + `release=17` + Jakarta + 守卫 + CI JDK 21 + 两阶段镜像（[#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)）

**(a) 目标**：让项目在 **Boot 3.5.x / `maven.compiler.release=17` / `jakarta.*`** 上编译并跑通现有 **134** 个用例（门槛收紧后的基线；收紧前为 127），且**同一 PR** 内同步守卫测试、CI JDK 版本与 `Dockerfile` 两阶段镜像（否则生产路径损坏而 CI 仍绿）。

**(b) 验证命令**

```bash
# 1) 后端全绿、用例与覆盖率不下降（JDK 21 环境）
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify
#   期望 Tests run ≥ 134, Failures: 0, Errors: 0, Skipped: 0；LINE ≥ 0.9165（988/1078）；All coverage checks have been met.

# 2) Jakarta 迁移边界：含 jakarta.servlet 的 src/main/java 文件恰好 11 个
grep -rl "jakarta.servlet" src/main/java | sort            # 逐行核对，与守卫白名单一致
grep -rl "jakarta.servlet" src/main/java | wc -l           # 期望 11
grep -rn "jakarta.servlet" src/test/java                   # 守卫测试中只允许出现禁令/白名单语境
grep -c "<exclude>" pom.xml                                # 期望仍为 8（不得扩 exclude）

# 3) 守卫测试改为白名单 + 集合相等（不再是“禁止 jakarta.servlet”）
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress test -Dtest=SpringBootOnlyArchitectureTest

# 4) 字节码与工具链
javap -verbose -cp target/classes com.fabflix.boot.FablixApplication | grep 'major version'   # 期望 61
mvn -version                                                                                  # 期望 Java version: 21

# 5) 打包形态不变（正向 + 负向验证）
cd frontend && npm ci && npm run build && cd ..
mvn -Pfrontend -DskipTests clean package
unzip -l target/fablix.jar | grep 'BOOT-INF/classes/static/index.html'   # 必须存在
mv frontend/dist /tmp/dist.bak && mvn -Pfrontend -DskipTests package; echo "exit=$?"   # 期望 BUILD FAILURE（require-frontend-dist）
mv /tmp/dist.bak frontend/dist

# 6) 容器可构建/可运行（B-1）
docker compose build app
docker compose up -d --no-deps app
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8081/api/boot/health   # 期望 200
docker compose rm -sf app
```

**AC 证据必须写明所用 `JAVA_HOME`**；Docker 不可用时**显式记“未执行 + 原因”**，不得假定通过。

**(c) 回退点**：**单 PR 整体 revert**（回到 Boot 2.7 + `javax`，`main` 仍绿，因为 S1/S2 未改变行为）。**禁止**用降阈值、扩 `excludes`、跳过 e2e 让 CI 变绿。

**(d) 完成分支 / commit**：`codex/issue-48-core-boot3-jakarta`（基于 `main` @ `55f8d79` = `Merge pull request #55`）；**已随 PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 合并，merge commit `70fb78b`**（#48 已 **CLOSED**；CI 双 job **SUCCESS**，见上方 CI 行）。证据 = `docs/logs/2026-10-20-issue-48-core-upgrade.md`。

**实际执行结果（2026-10-20，worker 执行 + 主 agent 终验与容器 smoke；证据 `docs/logs/2026-10-20-issue-48-core-upgrade.md`）**

| 项 | 结果 |
| --- | --- |
| 版本 / 工具链 | `spring-boot.version` **2.7.18 → 3.5.15**；`maven.compiler.source/target=11` → **`maven.compiler.release=17`**；全部命令在 `JAVA_HOME=/opt/homebrew/opt/openjdk@21`（**Java 21.0.12.1**）下运行 |
| 门禁 | `mvn --batch-mode --no-transfer-progress clean verify` → **BUILD SUCCESS**；**134 tests / 0 failures / 0 errors / 0 skipped**；`All coverage checks have been met.`；JaCoCo **988/1078 = 0.9165**（门槛 0.90，**分母未变**）；`<exclude>` 仍 **8** 项 |
| Jakarta 边界 | `grep -rl jakarta.servlet src/main/java` 得到 **恰好 11 个文件**（路径与守卫白名单逐条一致）；`grep -rn javax.servlet src/main/java` **0 命中**；`javax.sql` / `javax.xml` 的 **4 个文件未动**（`AdminRepository` + 3 个 XML parser） |
| 守卫 | 白名单**集合相等**（恰好 11 路径）通过；保留 `@WebServlet` / `extends HttpServlet` / `jakarta.servlet.annotation.WebServlet` 禁令；**主 agent 终验后补强 2 行**（源码禁 `javax.servlet`、pom 禁 `javax.servlet-api`），仍为 **2 个 `@Test`**、用例总数仍 134 |
| 字节码 | `major version: 61`（另以字节头 `cafe babe 0000 003d` 复核）。**注意**：本 checklist 与 issue #48 原文写的类名 `FablixApplication` 是**笔误**，实际类为 `com.fabflix.boot.FabflixApplication`；照抄原文会得到「类找不到」 |
| 打包 | `cd frontend && npm ci && npm run build` 成功；`mvn -Pfrontend -DskipTests clean package` → jar 含 `BOOT-INF/classes/static/index.html` + assets；移走 `frontend/dist` 后**同一命令 BUILD FAILURE**（`require-frontend-dist`）；默认 `mvn verify`（**无** `-Pfrontend`）在 dist 缺失时仍 **BUILD SUCCESS**（后端门禁不依赖 dist） |
| CI | ✅ **PR [#56](https://github.com/PNZ-z/ai-powered-Fabflix/pull/56) 已合并（merge `70fb78b`），S3 的 AC-6 已取证**：CI 双 job 均 **SUCCESS** —— 后端 `Build and test (Java 21)` + 前端 `Frontend (lint, test, coverage, build, e2e)`（结果由主 agent 核验，2026-10-20）；`ci.yml` 3 行改动为 job 名 `Build and test (Java 21)`、step 名 `Set up Java 21`、`java-version: "21"`；#48 已 **CLOSED** |
| 镜像 / 容器 | `Dockerfile`：build `maven:3.9-eclipse-temurin-21`、runtime `eclipse-temurin:21-jre`；`uid/gid 1001` 非 root、`EXPOSE`、`ENTRYPOINT` 未动。`docker compose build app` 成功；容器内 **JDK 21**（`openjdk version "21.0.12.1"`）、`id fabflix` = `uid=1001 gid=1001`；health **200** |
| 容器 smoke（提交版镜像，主 agent 亲自跑） | `/` 含 `id="root"`；**10 条深链全 200 `text/html`**；未认证 `/api/boot/cart` → **401 JSON**；`/API/boot/cart` → **404 JSON**；`/assets/missing.js` → 404；顾客登录 → 加购 → token → **支付成功**（`order_date=2026-10-20`，`sales.id=13566`、`sale_date=2026-10-20`）；员工登录 → `/dashboard/session` 200 → `?action=metadata` 200；容器日志无 warn/error/deprecat |
| 依赖解析 | `mysql-connector-j` **9.7.0**（显式 pin 生效，未被 BOM 覆盖；且 Boot 3.5.15 BOM 的值同为 9.7.0，**无漂移风险**）；Tomcat **10.1.55**、Jackson **2.21.4**、HikariCP **6.3.3**、Logback **1.5.34**；`junit-jupiter 5.11.4`（聚合）vs `junit-jupiter-engine 5.12.2`（BOM）、`mockito-core 5.23.0` vs `mockito-junit-jupiter 5.17.0`（BOM；无 `MockitoExtension` 使用 → 惰性） |
| **已知项（不得视为已验证）** | ① `@MockBean` 11 条弃用警告 → **S5**；② HikariCP 4→6 的**池语义/并发**未专门验证（「未观察到差异」只限 HTTP 状态码/JSON 形状/启动日志）→ **S6**；③ 生产 `GoogleRecaptchaVerifier`（`@Profile("!local")`，0/17 覆盖）无测试/Boot 3 运行证据，且有 Boot 3 新弃用（`RestTemplateBuilder.setConnectTimeout` / `setReadTimeout`）→ **S6 风险 / 可选后续修复**；④ 静态深链**尾斜杠**未测量 → **S6**；⑤ `/api/boot/health/` 的 **401 是既有行为**（`CustomerAuthenticationFilter` 精确匹配 + filter 先于 MVC；升级前基点树 `55f8d79` 实测同样 401），**不是 Boot 3 差异**；⑥ dev 库新增 `sales.id=13566`（连 S2 共 13562–13566） |

> **给 S4 的输入**：Boot 3 + `release=17` + Jakarta + CI JDK 21 + 两阶段镜像**均已就绪且已核验**（上表）。S4 因此**只需**加 enforcer `requireJavaVersion [21,22)`、写 README 的 JDK 21 / `JAVA_HOME` 说明，并对镜像与 `docker compose build app` **取证**（**不**再用 `mv frontend/dist` 验证 dist 守卫，那是 S3 已完成的回退检查，S4 只需确认不回退）。**不要**再改 `Dockerfile` / `ci.yml`；若发现镜像仍为 JDK 11 → 停下回主 agent（说明 S3 未完成）。注意：S4 生效后本机默认 JDK 27 的构建会**按预期失败**（P1-3）。

---

## S4 — 用机器可执行的方式强制 JDK 21，并归档镜像证据（[#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49)）

**(a) 目标**：在主 `<build>` 中加入 enforcer `requireJavaVersion [21,22)`（`maven-enforcer-plugin` 目前只存在于 `frontend` profile，`pom.xml:178-186`）+ README 说明，并只**取证**（不改）S3 已改好的两阶段 JDK 21 镜像。

**(b) 验证命令**

```bash
# 1) 正确 JDK → 通过
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress validate     # 期望 BUILD SUCCESS

# 2) 错误 JDK（本机默认 JDK 27）→ BUILD FAILURE（预期行为，不是 bug）
mvn --batch-mode --no-transfer-progress validate; echo "exit=$?"   # 期望非 0，报 requireJavaVersion

# 3) profile 内既有的 require-frontend-dist 未被破坏
cd frontend && npm ci && npm run build && cd ..
mvn -Pfrontend -DskipTests package                      # 期望成功
mv frontend/dist /tmp/dist.bak && mvn -Pfrontend -DskipTests package; echo "exit=$?"   # 期望失败
mv /tmp/dist.bak frontend/dist

# 4) README 记录 JDK 21 要求与 JAVA_HOME 说明
grep -n "21" README.md | grep -i "jdk\|java_home"

# 5) 镜像证据（S3 的产物，本步只验证）
grep -n "^FROM" Dockerfile                              # 期望 maven:3.9-eclipse-temurin-21 / eclipse-temurin:21-jre
docker compose build app
```

**(c) 回退点**：revert 本 PR（enforcer 与 README 相互独立，可只回退 enforcer）。注意 `requireJavaVersion` 会让 JDK 27 本地构建**直接失败**——这是计划内行为（P1-3），README 必须写清。

**(d) 完成分支 / commit**：`codex/issue-49-jdk21-toolchain`（基于 `70fb78b` = `Merge pull request #56`）；**已随 PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57) 合并，merge commit `9a7705ca2bb9f5b4dc0fb1a20a4413a0c46aa0b6`**（#49 已 **CLOSED**）；证据 = `docs/logs/2026-10-20-issue-49-jdk21-toolchain.md`（AC-1..AC-6 回填见 `docs/issues/issue-49-jdk21-toolchain.md`）。

**实际执行结果（2026-10-20，主 agent 终验 + worker 执行；证据 `docs/logs/2026-10-20-issue-49-jdk21-toolchain.md`）**

| 项 | 结果 |
| --- | --- |
| 工具链 / diff | 全部 `mvn` 命令在 `JAVA_HOME=/opt/homebrew/opt/openjdk@21`（**Java 21.0.12.1**）下运行；`git diff --stat` = **2 files changed, +40 −0**（`pom.xml` +23、`README.md` +17）；无 `src/**`、无 `Dockerfile`、无 `ci.yml`、无 `compose.yaml` 改动 |
| 门禁实现 | 主 `<build>` 新增 `maven-enforcer-plugin:3.5.0` execution **`require-jdk-21`**（`validate` / `fail=true` / `requireJavaVersion [21,22)` / 消息指回 README）；`frontend` profile 的 `require-frontend-dist` **原样保留**，`-Pfrontend` 下**两条 execution 都执行**（日志逐条可见） |
| 正向 / 负向 | JDK 21 `mvn validate` → `Rule 0: … RequireJavaVersion passed` + **BUILD SUCCESS**；**JDK 27**（`/opt/homebrew/opt/openjdk`，本机 Maven 默认）同命令 → **BUILD FAILURE**，消息 = `Fabflix pins the build and Maven-run to JDK 21 (bytecode release=17; Boot 3.5 runs on 17-24, but this project is verified on 21 only); set JAVA_HOME to a JDK 21 installation - see README`（**预期行为**，P1-3） |
| 门禁不回归 | JDK 21 `mvn verify` → **BUILD SUCCESS**；**134 tests / 0 failures / 0 errors / 0 skipped**；`All coverage checks have been met.`；JaCoCo **988/1078 = 0.9165**（阈值仍 0.90、`excludes` 仍 8）；字节码 **major 61**（`release=17` 未变） |
| 打包 / dist 守卫 | `mvn -Pfrontend -DskipTests clean package` 成功且 jar 含 **6 个 `static` 条目**（含 `BOOT-INF/classes/static/index.html`）；移走 `frontend/dist` 后同命令 **BUILD FAILURE**（`require-frontend-dist`：`frontend/dist/index.html is missing …`），**随后已恢复** |
| 容器（**只验证未改**） | `Dockerfile` 两阶段仍为 `maven:3.9-eclipse-temurin-21` / `eclipse-temurin:21-jre`（S3 产物，**零改动**）；`docker compose build app` 成功 → `up` 后 health **200**；容器内 `java -version` = **21.0.12.1**；运行用户 **uid/gid 1001**（非 root）；`docker compose rm -sf app`，**未 `down` mysql** |
| README | 新增 `## Java toolchain`：**构建（所有 `mvn` 命令，含 `spring-boot:run`）需 JDK 21** 且 `JAVA_HOME` 指向 JDK 21；自检 `mvn -version` 须报 **`21.x`** + `export JAVA_HOME=/path/to/jdk-21`；**IDE 内嵌 Maven 需同样设为 21**；`release=17` → major 61 → **打包后的 jar 可在 Java 17+ 运行**（「JDK 21 是构建工具链，不是语言级别」）；CI 与 `Dockerfile` **both JDK stages** 为 21；Compose 在镜像内运行、**不依赖宿主 JDK** |
| 三路 review | **无 Blocker**；采纳并已修正 **5 项文案**：enforcer 消息对 JDK 22–24 的歧义、README 首句「building and running」过宽、`"Java version: 21"` → `21.x`、补 `export JAVA_HOME` 与 IDE 提示、`both stages` → `both JDK stages` |
| **未取证 / 登记后续** | ~~**本步的 PR 与其 CI**（尚无 PR，故不得写「已合并」）~~ → ✅ **已闭合**：PR [#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57) 已合并（merge `9a7705c`），CI 双 job **SUCCESS**（`Build and test (Java 21)` 51s + `Frontend (…)` 2m38s，run `39504525702`），#49 已 **CLOSED**；JDK 17 未单独复跑（规则语义已由 JDK 27 取证）；前端 lint/test/e2e（本步不涉及）。**仍开放（登记到后续，本步不改代码）**：`frontend/README.md` 的 `mvn -Pfrontend` 说明需提 JDK 21（**S7 docs 清单**）；`SpringBootOnlyArchitectureTest` 可加 enforcer 规则守卫（**S6/S7 评估**）；enforcer 版本字面量在主 build 与 profile **两处重复**（可选清理）；`docs/architecture/frontend-migration-contracts.md` §11/§8 的旧 JDK 11/`jakarta` 禁令描述（**S7**） |

> **下一步 S5（[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)）**：S4 的 **JDK 21 门禁已生效** —— 本地 `mvn`、CI（Java 21）与容器镜像（两阶段 JDK 21）现在是**同一个 JDK 21**，任何非 21 的 `JAVA_HOME` 都会在 `validate` 阶段立刻失败。S5 只改 9 个测试文件的 `@MockBean` → `@MockitoBean`（+ import），不触碰 `pom.xml` 的 enforcer 规则与 `README.md`；所有 S5 命令同样必须在 `JAVA_HOME=/opt/homebrew/opt/openjdk@21` 下运行（否则会先被 S4 的门禁拦下）。

---

## S5 — 把 `@MockBean` 换成 `@MockitoBean`（[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)）

**(a) 目标**：消除 Boot 3.4+ 的 `@MockBean` 弃用警告（9 个测试文件 / 11 处），**不改任何测试语义**。

**(b) 验证命令**

```bash
# 1) 迁移前计数（记录，用于对照）
grep -rn "@MockBean" src/test/java | wc -l              # 期望 11
grep -rl "@MockBean" src/test/java | wc -l              # 期望 9

# 2) 迁移后 → 0，并确认新注解数
grep -rn "@MockBean" src/test/java | wc -l              # 期望 0
grep -rn "@MockitoBean" src/test/java | wc -l           # 期望 11

# 3) 全绿且用例/覆盖率不下降
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify        # 期望 Tests run ≥ 134 / LINE ≥ 0.9165（收紧后基线）

# 4) 弃用警告计数（证据，非闸门）
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify 2>&1 | grep -ci deprecat

# 5) diff 只含 import + 注解行
git diff --stat && git diff -- src/test/java | grep -E "^[+-]" | grep -vE "^[+-]{3}" | head -40
```

**(c) 回退点**：单 PR revert（import 与注解成对回退，无运行时影响）。

**(d) 完成分支 / commit**：`codex/issue-50-mockito-annotation`（基于 `9a7705c` = `Merge pull request #57`）；**未提交、未推送、无 PR**；commit SHA 待提交后回填。证据 = `docs/logs/2026-10-20-issue-50-mockito-annotation-cleanup.md`（AC-1..AC-5 回填见 `docs/issues/issue-50-mockito-annotation-cleanup.md`）。

**实际执行结果（2026-10-20，worker 执行 + 主 agent 终验；证据 `docs/logs/2026-10-20-issue-50-mockito-annotation-cleanup.md`）**

| 项 | 结果 |
| --- | --- |
| 计数 | `grep -rn "@MockBean" src/test/java` = **0**；`@MockitoBean` = **11 处 / 9 文件**；`@SpyBean`/`@MockitoSpyBean` = **0**（计数无需调整） |
| diff 范围 | `git diff --stat` = **9 files changed, +20 −20**（20 = 11 处注解 + 9 个 import）；逐 hunk 仅 import/注解，**无**测试体/断言/fixture/会话写法变化；**未改动** `src/main/**`、`pom.xml`、`ci.yml`、`Dockerfile`、`compose.yaml`、`frontend/**`、JaCoCo 阈值/`excludes` |
| 门禁（JDK 21） | `mvn --batch-mode --no-transfer-progress verify` → **BUILD SUCCESS**；**134 tests / 0 failures / 0 errors / 0 skipped**（与改动前一致）；`All coverage checks have been met.`；JaCoCo **988 / 90 / 1078 = 0.9165**（阈值 0.90） |
| 弃用警告（**口径关键**） | 规定的 `grep -ci "MockBean.*deprecat"` 在**增量**构建下 before/after **都是 0**（`Nothing to compile` → 无 javac 警告，**不能**用作 before 证据）；改用 **`mvn clean verify`**：before = **11**（恰对应 11 个 `@MockBean` 站点），after = **0**；整份日志弃用总数 **13 → 2**（差值 11），剩 2 条为 `GoogleRecaptchaVerifier` 的 `setConnectTimeout`/`setReadTimeout`（S3 已登记，归 **S6**） |
| 等价性 | 11 个 mock 目标 = **10 个 `@Service` 类** + **1 个接口**（`RecaptchaVerifier`），**全部不在 `@WebMvcTest` 切片内** → 两注解在此路径等价；**无一条**用例因注解切换失败，故**未触发**「保留 `@MockBean` 并上报」的回退 |
| context cache key | 已按 **spring-test 6.2.19 字节码**核实：`@MockitoBean` override 通过 `BeanOverrideContextCustomizer` **参与** cache key，粒度为 **handler 形状**（类型/字段名/strategy/qualifier，不含测试类）→ mock 形状相同可共享 context、mock 集不同不共享；**本仓库当前无共享**（10 个 `@WebMvcTest` 各自钉不同 controller，4 个 `standaloneSetup` 无 context 缓存）；已写入契约文档 **§7** |
| 三路 review | **无代码 Blocker**；P1「S5 分支夹带 S4」经主 agent 核实**不成立**（`git log origin/main..HEAD` 为空）；其余为记录项 |
| **未取证 / 登记后续** | **本步的 PR 与其 CI**（尚无 PR，故不得写「已合并」）；「构建零弃用」**当前不成立**（仍剩 2 条范围外弃用 → **S6**）；`@MockitoSpyBean` / `@MockitoBean(name=…)` 路径未使用也未验证 |

---

> **进入 S6 前的前置状态（2026-10-20 回填）**：S5 已实施，**最终形态代码层面已就绪** —— 分支 `codex/issue-50-mockito-annotation` 上 `@MockBean` = **0**、`@MockitoBean` = 11/9 文件，JDK 21 `verify` **134 / 0.9165**。**但 S5 尚未合并** → S6 不得在 `main`（= `9a7705c`，仍含 11 处 `@MockBean`）上取证「无弃用注解」；须等 PR 合并后基于新 `main` 跑全量门禁。另：S6 取证时构建仍会有 **2 条**范围外的弃用警告（`GoogleRecaptchaVerifier`），「零弃用」的描述需限定为「`MockBean` 相关弃用为 0」。

## S6 — 用全量回归 + 生产 smoke 证明迁移（[#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)）

**(a) 目标**：对**最终形态**（Boot 3 + JDK 21 镜像 + 无弃用注解）逐条取证 #44 的 AC-1..AC-7 / AC-9 / AC-11，产出可核验证据；环境不足则**显式标注未执行**。

**(b) 验证命令**

```bash
# AC-1 / AC-2 / AC-9：后端全绿 + 覆盖率闸门 + excludes 不被扩
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify        # ≥134 用例、LINE ≥ 0.9165、All coverage checks have been met.
grep -c "<exclude>" pom.xml                             # 期望 8

# AC-5：前端 5 条命令全绿且数字不下降
cd frontend && npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e
cd ..   # 期望 195 tests / 30 files、Lines 92.35%（≥90）、12 passed
git diff --stat -- frontend/src frontend/e2e            # 期望无输出（前端零行为改动）

# AC-3 / AC-11：jar 形态、字节码、工具链
cd frontend && npm run build && cd ..
mvn -Pfrontend -DskipTests clean package
unzip -l target/fablix.jar | grep 'BOOT-INF/classes/static/index.html'
javap -verbose -cp target/classes com.fabflix.boot.FablixApplication | grep 'major version'   # 61
mvn -version                                                                                  # Java version: 21
grep -n "^FROM" Dockerfile

# AC-6 / AC-7：生产 compose smoke
docker compose build app && docker compose up -d --no-deps app
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8081/api/boot/health                # 200
curl -s http://127.0.0.1:8081/ | grep -q 'id="root"' && echo "root ok"
for p in /home /movies /movies/1 /cart /checkout /confirmation /login /employee/login /employee/dashboard /playground; do
  printf '%s ' "$p"; curl -s -o /dev/null -w '%{http_code} %{content_type}\n' "http://127.0.0.1:8081$p"
done   # 期望 10 条均 200 text/html
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8081/api/boot/cart        # 未认证 → 401 JSON
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8081/API/boot/cart        # → 404 JSON
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8081/assets/missing.js    # → 404
docker compose rm -sf app
```

**未执行项**（真实浏览器刷新、视觉/对比度、真实 MySQL 并发、`add_movie` 存储过程真实往返）见 `docs/logs/2026-10-20-issue-46-baseline.md` 的“非自动化覆盖清单”，**必须写“未执行 + 原因”**。

**(c) 回退点**：只增证据文档 → 删除即回退。**若发现缺陷：停下回主 agent**，由对应步骤（S3/S4/S5）的 owner 修，**不在 S6 顺手改源码**。

**(d) 完成分支 / commit**：`（待填）`

---

## S7 — 收尾：架构文档、路线图状态与工作流日志（[#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52)）

**(a) 目标**：把契约文档、README、路线图与 #44/milestone 状态对齐到**实现的真实状态**，并写 S7 自己的工作流日志（用 #52 的编号，不用 #44）。

**(b) 验证命令**

```bash
# 契约文档 §8（守卫语义）、§11（容器两阶段 = JDK 21）、§12（CI JDK 21）、§7（覆盖率余量）
grep -n "jakarta.servlet" docs/architecture/frontend-migration-contracts.md
grep -n "JDK 21\|21-jre\|temurin-21" docs/architecture/frontend-migration-contracts.md
grep -n "0.9165\|≥ 0.90\|0.90" docs/architecture/frontend-migration-contracts.md

# README 与实现一致
grep -n "JDK 21\|java_home\|JAVA_HOME" README.md

# 路线图状态回填
grep -n "Spring Boot 3\|M3" docs/plans/ai-roadmap-overview.md | head -20

# 日志存在且含两大节标题
ls docs/logs/ | grep issue-52
grep -n "修改了哪些文件\|新增知识点" docs/logs/*issue-52*.md
```

**(c) 回退点**：文档回退 = revert 本 PR。

**(d) 完成分支 / commit**：`（待填）`

---

## 维护约定

1. 每完成一步，由**主 agent** 确认后更新本文件的三处：① 状态总表的“状态”列；② 该步的 (d) 完成分支/commit；③ 其证据路径。**docs-writer 不自行标记“已完成”**，也不在 PR 合并前写“已合并”。
2. 状态词只用图例中的五种，不写模糊表述（如“基本完成”）。
3. 任何步骤的范围变化、验收放宽或回退到备选方案，必须回到主 agent 裁定并在 `docs/plans/issue-44-migration-steps.md` §10 记录，**不得只改本 checklist**。
4. 本 checklist 的验证命令与 `issue-44-migration-steps.md` §4 的 DoD 一一对应；两者冲突时**以 §4 + 子 issue 正文的 AC 为准**，并修正本文件。
5. 覆盖率门槛与基线以「2026-10-20 覆盖率门槛收紧（0.90 / 90）与新的比较基线」一节为准；**任何后续步骤不得降低阈值、不得扩 `excludes`、不得跳过 e2e**。若有人再次改动 `pom.xml` 的 JaCoCo `check` 或测试数量，该表失效，须由主 agent 决定是否重取基线（不得静默沿用）。

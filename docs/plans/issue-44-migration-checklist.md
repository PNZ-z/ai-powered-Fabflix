# Issue #44 迁移 checklist（S1–S7：目标 / 验证命令 / 回退点 / 完成分支）

> **用途**：把 Option A 迁移（Boot 2.7.18 + Java 11 + `javax` → **Boot 3.5.x + Java 17 + `jakarta` + JDK 21 工具链**）的 7 个步骤做成**可逐条打勾的执行清单**。每一步都写明 (a) 一句话目标、(b) 精确验证命令、(c) 回退点、(d) 完成分支/commit 与状态。
> **上位文档**（本 checklist 不改变其任何已批准范围与验收）：
> - epic：**[#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)**（`[Epic] Upgrade to Spring Boot 3 / Java 17 / Jakarta`，milestone [#3](https://github.com/PNZ-z/ai-powered-Fabflix/milestone/3)，**OPEN**）
> - 子 issue：**S1 [#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46)** · **S2 [#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)** · **S3 [#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)** · **S4 [#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49)** · **S5 [#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)** · **S6 [#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)** · **S7 [#52](https://github.com/PNZ-z/ai-powered-Fabflix/issues/52)**
> - 拆分总览：`docs/plans/issue-44-migration-steps.md`（§2 依赖与顺序、§3 步骤总表、§4 各步 DoD/回退点）
> - 已批准计划：`docs/plans/ai-option-a-boot3-migration.md`；约束：`docs/architecture/frontend-migration-contracts.md`
> - **迁移前基线（所有“不下降”判据的唯一基准）**：`docs/logs/2026-10-20-issue-46-baseline.md`
>
> **状态图例**：`未开始` / `基线已测量（待 PR 合并后标记完成）` / `实施中` / `已完成（PR #n 已合并 @ <sha>）` / `阻塞（原因）`。
> **本文件不表示任何步骤已完成**：除 S1 的基线测量外，S2–S7 的 Scope / AC **全部未执行**；每一步仍需各自走 docs-writer 计划 → oracle 只读审查 → 主 agent 批准 → 单一 worker → 3 路 reviewer → 修复与复验 → 日志的完整流程。

## 状态总表

| 步骤 | Issue | 状态 | 完成分支 / commit | 证据 / 记录 |
| --- | --- | --- | --- | --- |
| **S1** 迁移前基线 + checklist | [#46](https://github.com/PNZ-z/ai-powered-Fabflix/issues/46) | **基线已测量（待 PR 合并后标记完成）** | `codex/issue-46-baseline`（基于 `main` @ `fff314f88455216122912f07c26706f70dd63b62`）；**未提交/未推送** | `docs/logs/2026-10-20-issue-46-baseline.md` + 本文件 |
| **S2** 非 Spring 依赖就绪 | [#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47) | **未开始** | `（待填：codex/issue-47-…）` | `（待填）` |
| **S3** 核心 Boot 3 + Jakarta 升级 | [#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48) | **未开始** | `（待填：codex/issue-48-…）` | `（待填）` |
| **S4** JDK 21 强制与镜像取证 | [#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49) | **未开始** | `（待填：codex/issue-49-…）` | `（待填）` |
| **S5** `@MockBean` → `@MockitoBean` | [#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50) | **未开始** | `（待填：codex/issue-50-…）` | `（待填）` |
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

**(c) 回退点**：纯文档 → 删除本 PR 的 4 个 `docs/` 文件即回退，**不影响任何构建/运行/CI**。

**(d) 完成分支 / commit**：分支 `codex/issue-46-baseline`（基于 `main` @ `fff314f`）；**当前未提交/未推送**，commit SHA 待提交后回填；证据 = `docs/logs/2026-10-20-issue-46-baseline.md` + 本文件。

---

## S2 — 在 Boot 2.7 基线上验证非 Spring 依赖就绪（[#47](https://github.com/PNZ-z/ai-powered-Fabflix/issues/47)）

**(a) 目标**：在**仍为 Boot 2.7** 的树上把 `mysql-connector-j` 定版并验证（含 jasypt 在 JDK 21 下的检查），把“JDBC 行为差异”从“Boot 3 行为差异”里分离出来。

**(b) 验证命令**

```bash
# 1) 定版记录（必须写明理由与最终版本）
grep -n "mysql-connector-j" -A3 pom.xml

# 2) 后端全绿且用例数不下降（与 S1 基线同 JDK 对比）
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify   # 期望 Tests run: 127 / 0.8980

# 3) jasypt 在 JDK 21 下可解密（无 IllegalAccessError / InaccessibleObjectException）
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress test -Dtest='*Jasypt*'

# 4) 真 MySQL smoke：读/写路径的日期与时区语义
docker compose up -d mysql
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT CURDATE(), NOW();"
# 通过 app 跑一次下单或员工销售记录路径，确认无时区偏移、CURDATE() 偏差、新增致命 SSL 警告
docker compose down        # 保留数据卷；如需彻底清理再 -v（会丢数据，须主 agent 批准）
```

**(c) 回退点**：单个依赖版本号 → revert 一行即可。若 9.x 在 Boot 2.7 基线上暴露行为差异，**保持 `8.3.0` 并记录**即为合规回退（不阻塞 S3；此时 AC-6 的 JDBC 分支按 P2-7 不触发）。

**(d) 完成分支 / commit**：`（待填）`

---

## S3 — 一次性原子升级：Boot 3.5 + `release=17` + Jakarta + 守卫 + CI JDK 21 + 两阶段镜像（[#48](https://github.com/PNZ-z/ai-powered-Fabflix/issues/48)）

**(a) 目标**：让项目在 **Boot 3.5.x / `maven.compiler.release=17` / `jakarta.*`** 上编译并跑通现有 127 个用例，且**同一 PR** 内同步守卫测试、CI JDK 版本与 `Dockerfile` 两阶段镜像（否则生产路径损坏而 CI 仍绿）。

**(b) 验证命令**

```bash
# 1) 后端全绿、用例与覆盖率不下降（JDK 21 环境）
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify
#   期望 Tests run: 127, Failures: 0, Errors: 0, Skipped: 0；968/1078 = 0.8980；All coverage checks have been met.

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

**(d) 完成分支 / commit**：`（待填）`

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

**(d) 完成分支 / commit**：`（待填）`

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
  mvn --batch-mode --no-transfer-progress verify        # 期望 127 / 0.8980

# 4) 弃用警告计数（证据，非闸门）
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify 2>&1 | grep -ci deprecat

# 5) diff 只含 import + 注解行
git diff --stat && git diff -- src/test/java | grep -E "^[+-]" | grep -vE "^[+-]{3}" | head -40
```

**(c) 回退点**：单 PR revert（import 与注解成对回退，无运行时影响）。

**(d) 完成分支 / commit**：`（待填）`

---

## S6 — 用全量回归 + 生产 smoke 证明迁移（[#51](https://github.com/PNZ-z/ai-powered-Fabflix/issues/51)）

**(a) 目标**：对**最终形态**（Boot 3 + JDK 21 镜像 + 无弃用注解）逐条取证 #44 的 AC-1..AC-7 / AC-9 / AC-11，产出可核验证据；环境不足则**显式标注未执行**。

**(b) 验证命令**

```bash
# AC-1 / AC-2 / AC-9：后端全绿 + 覆盖率闸门 + excludes 不被扩
JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
  mvn --batch-mode --no-transfer-progress verify        # ≥127 用例、0.8980、All coverage checks have been met.
grep -c "<exclude>" pom.xml                             # 期望 8

# AC-5：前端 5 条命令全绿且数字不下降
cd frontend && npm ci && npm run lint && npm test && npm run test:coverage && npm run build && npm run test:e2e
cd ..   # 期望 195 tests / 30 files、Lines 92.35%（≥88）、12 passed
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
grep -n "0.8980\|110" docs/architecture/frontend-migration-contracts.md

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

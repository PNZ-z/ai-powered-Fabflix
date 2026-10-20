# 工作流日志 — issue #50（S5）：`@MockBean` → `@MockitoBean`（测试注解清理）

## 元数据

| 项目 | 值 |
| --- | --- |
| 日期 | **2026-10-20**（实施与终验同日；本日志由 `docs-writer` 于同日回填） |
| GitHub issue | **[#50](https://github.com/PNZ-z/ai-powered-Fabflix/issues/50)** `[Step 5/7] Replace deprecated @MockBean with @MockitoBean in the tests` —— state **OPEN**（创建于 2026-10-19；正文含 `Part of #44`） |
| 上游 epic | [#44](https://github.com/PNZ-z/ai-powered-Fabflix/issues/44)（保持 OPEN）；拆分总览 `docs/plans/issue-44-migration-steps.md`，checklist `docs/plans/issue-44-migration-checklist.md` |
| 分支 | **`codex/issue-50-mockito-annotation`**（基点 **`9a7705c`** = `Merge pull request #57 from PNZ-z/codex/issue-49-jdk21-toolchain`，即 S4 合并后的 `main`） |
| 提交 / 推送 / PR | **未提交、未推送、无 PR**（`git diff --cached --stat` 为空；`origin/main..HEAD` 为空 → 分支上**不含**任何 S4 或更早的提交）；远端 #50 仍 **OPEN** |
| 改动规模 | `git diff --stat` = **9 files changed, 20 insertions(+), 20 deletions(-)**（20 = **11 处注解** + **9 个 import**）；全部位于 `src/test/java` |
| 工具链 | 所有 `mvn` 命令在 `JAVA_HOME=/opt/homebrew/opt/openjdk@21`（**Java 21.0.12.1**）下运行；非 21 的 `JAVA_HOME` 会被 S4 的 enforcer 在 `validate` 阶段直接拦下 |
| 终验门禁（主 agent，JDK 21） | `mvn --batch-mode --no-transfer-progress verify` → **BUILD SUCCESS**；**`Tests run: 134, Failures: 0, Errors: 0, Skipped: 0`**；`All coverage checks have been met.`；JaCoCo **988 covered / 90 missed / 1078 total = 0.9165**（门槛 **0.90**，`excludes` 仍 8）；最终日志 `grep -ci "MockBean.*deprecat"` = **0** |
| 证据来源 | worker 报告 `.pi/artifacts/worker-issue-50-mockito.md`（实施 + before/after 日志路径）；主 agent 终验；三路只读 review 结论（主 agent 转述）；本轮 `docs-writer` 只读复核：`git status/diff/diff --numstat`、`grep -rn "@MockBean"/"@MockitoBean"`、逐文件 `git diff -U4`、mock 目标类的 `@Service`/`@Profile` 注解、`gh issue view 49/50`、`gh pr view 57`、`gh pr checks 57` |
| 关联 PR（**S4**，非本 issue） | **[#57](https://github.com/PNZ-z/ai-powered-Fabflix/pull/57)** `Enforce JDK 21 for the build and document it` —— **MERGED**，merge commit **`9a7705c`**；CI 双 job **SUCCESS**（run `39504525702`）；[#49](https://github.com/PNZ-z/ai-powered-Fabflix/issues/49) 已 **CLOSED**（本轮 `gh` 只读核验） |

> **本步是「清理」而非「修复」**：`@MockBean` 在 Boot 3.4+ 被标记弃用但仍可用；本步只替换注解与 import，**不改任何测试语义**，因此用例数与覆盖率必须**逐位不变**（实测不变）。

---

## 1. 修改了哪些文件及潜在问题

### 1.1 改动清单（9 个文件，逐文件核对）

每个文件只有两类行变化：**import 一行** + **`@MockBean` → `@MockitoBean`**（2 个文件各有 2 处注解）。`git diff -U0` 逐 hunk 核对：**无测试方法体、断言、fixture、`.session(...)`/`.sessionAttr(...)`、`@WebMvcTest`/`@Import`/`standaloneSetup` 结构变化**；无 `@SuppressWarnings`、无编译参数变化。

| 文件 | 注解处数 | import 处理 | 被 mock 的类型 |
| --- | --- | --- | --- |
| `src/test/java/com/fabflix/boot/auth/CustomerAuthControllerTest.java` | 2（L30、L33） | 移到字母序位置（L10，原 L8） | `CustomerAuthService`（`@Service`）、`RecaptchaVerifier`（**接口**） |
| `src/test/java/com/fabflix/boot/commerce/CartControllerTest.java` | 1（L29） | 移位置（L11，原 L10） | `CartService`（`@Service`） |
| `src/test/java/com/fabflix/boot/commerce/CheckoutControllerTest.java` | 1（L30） | 移位置（L13，原 L11） | `CartService` |
| `src/test/java/com/fabflix/boot/commerce/PaymentControllerTest.java` | 2（L38、L41） | 移位置（L15，原 L13） | `CartService`、`PaymentService`（`@Service`） |
| `src/test/java/com/fabflix/boot/main/MainControllerTest.java` | 1（L23） | **原位替换**（L15；该槽位原本已满足字母序） | `GenreService`（`@Service`） |
| `src/test/java/com/fabflix/boot/movie/MovieDetailControllerTest.java` | 1（L26） | 移位置（L11，原 L10） | `MovieDetailService`（`@Service`） |
| `src/test/java/com/fabflix/boot/movie/MovieListControllerTest.java` | 1（L26） | **原位替换**（L11） | `MovieListService`（`@Service`） |
| `src/test/java/com/fabflix/boot/search/SearchControllerTest.java` | 1（L25，单行声明 `@MockitoBean SearchService searchService;`） | 移位置（L11，原 L9） | `SearchService`（`@Service`） |
| `src/test/java/com/fabflix/boot/star/StarDetailControllerTest.java` | 1（L26） | 移位置（L11，原 L10） | `StarDetailService`（`@Service`） |

- **计数复核（本轮只读 grep）**：`grep -rn "@MockBean" src/test/java` = **0**；`grep -rn "@MockitoBean" src/test/java` = **11 处 / 9 文件**；`@SpyBean`/`@MockitoSpyBean` = **0**（本仓库没有需要处理的 spy 场景）。
- **import 去向统一**：`org.springframework.boot.test.mock.mockito.MockBean` → `org.springframework.test.context.bean.override.mockito.MockitoBean`（新 import 落在 `org.springframework.mock.web.MockHttpSession` / `org.springframework.context.annotation.Import` 之后、`org.springframework.test.web.servlet.MockMvc` 之前）。
- **mock 目标的性质（供等价性判断）**：11 个目标 = **10 个 `@Service` 类** + **1 个接口**（`RecaptchaVerifier`；其实现 `LocalRecaptchaVerifier` 为 `@Component @Profile("local")`、`GoogleRecaptchaVerifier` 为 `@Component @Profile("!local")`）。这些类型**都不在 `@WebMvcTest` 切片内**（切片只装配 MVC 层组件），因此两种注解在此路径上都是「按类型注册一个 mock 并替换/补位」，行为等价；实测 134 用例全绿、无一条因注解切换而失败。

### 1.2 弃用证据的准确口径（**必须随 PR 说明一起发布**）

这是本步唯一容易写错的地方，reviewer 已指出，主 agent 已核验：

| 采集方式 | before | after | 能否作为「before=11」的证据 |
| --- | --- | --- | --- |
| issue 正文规定的 `mvn verify 2>&1 \| grep -ci "MockBean.*deprecat"`（**增量构建**） | **0** | **0** | ❌ **不能**：两次日志都含 `Nothing to compile - all classes are up to date.`（`target/` 已存在），javac 没有重新编译 → 不产生任何警告，整个日志 `grep -ci deprecat` 都是 0 |
| **`mvn clean verify`**（强制重编译，同一未改动树） | **11** | **0** | ✅ **权威 before 证据**：11 条警告恰好对应 11 个 `@MockBean` 站点 |
| 整份日志的弃用警告总数（clean） | **13** | **2** | ✅ 差值恰为 **11**，与上表一致 |

- before 首条命中（逐字，clean 日志）：`[WARNING] .../auth/CustomerAuthControllerTest.java:[30,6] org.springframework.boot.test.mock.mockito.MockBean in org.springframework.boot.test.mock.mockito has been deprecated and marked for removal`。
- **after 剩余 2 条范围外弃用**（不是本 step 的回归）：`src/main/java/com/fabflix/boot/auth/GoogleRecaptchaVerifier.java:[27,17] setConnectTimeout(...)`、`[28,17] setReadTimeout(...)`（`org.springframework.boot.web.client.RestTemplateBuilder`）。该类是 `@Profile("!local")` 生产实现、0/17 行覆盖（见 `docs/plans/issue-44-migration-checklist.md` 的已知覆盖缺口表），**S3 已登记、属 S6 评估范围**，本 issue 的 Non-goals 明确不处理。
- **结论口径**：AC-4 的「计数证据」以 **clean 构建** 为准（11 → 0）；增量 `verify` 的 0/0 只是「规定的命令照跑」的记录，**不得**用它论证 before。真正的闸门仍是 AC-1（`grep -rn "@MockBean" src/test/java` 无命中）。

### 1.3 语义与上下文缓存风险（本步唯一的技术判断点）

- **等价性**：`@MockitoBean` 是 Spring Framework 6.2 的 bean override 注解，替代 Boot 3.4 起弃用的 `@MockBean`；在「mock 的 bean 不存在于切片内 / 需要按类型替换」这两种情形下语义相同。本步 11 个目标全部属于前者，故等价；若将来出现**需要 `spy` 既有 bean** 或**按 `name` 精确替换**的场景，必须改用 `@MockitoSpyBean` / 显式 `name=`，不能照抄本步做法。
- **context cache key（已按库实现核实，非猜测）**：`@MockitoBean` 的 override 通过 `BeanOverrideContextCustomizer` **参与** Spring Test 的 context cache key，但粒度是**handler 形状**——`BeanOverrideHandler.equals/hashCode` 只比较 handler 类、bean 类型、`beanName`、`contextName`、`BeanOverrideStrategy`、字段**名**与 qualifier 注解（**不含声明测试类**）；`MergedContextConfiguration.equals` 显式比较 `contextCustomizers` 集合（spring-test **6.2.19** 字节码：`BeanOverrideContextCustomizerFactory` / `BeanOverrideContextCustomizer` / `BeanOverrideHandler` / `MergedContextConfiguration`）。
  - 推论①：两个测试类的 mock 形状**完全相同** → 可共享同一 context；mock 集**不同**（类型或字段名不同）→ cache key 不同，**不会**错误复用。
  - 推论②：**本仓库当前无共享**——10 个 `@WebMvcTest` 类（9 个含 mock + `HealthControllerTest`）各自 `@WebMvcTest(<不同 Controller>.class)`，controller 本身即进入 cache key；另有 4 个 `standaloneSetup` 文件（`admin/**` 3 个 + `session/SessionControllerTest`）根本不建 context 缓存。
  - **契约提示（已写入架构文档 §7）**：将来若为**同一 controller** 增加第二套切片测试，只要 mock 的类型/字段名/strategy/qualifier 与本类完全一致，就会共享同一缓存 context（这不是 bug，但意味着「第二套切片的 mock 集」不会自动获得独立上下文）。
- **结论**：本步无 `@MockBean` 特有的 cache-key 行为变化需要补偿；不需要加 `@DirtiesContext`（加了反而会掩盖问题并拖慢测试）。

### 1.4 范围边界（未扩大）

**未改动**：`src/main/**`（生产代码零改动）、`pom.xml`（无新依赖——`@MockitoBean` 随既有 `spring-test` 提供）、`.github/workflows/ci.yml`、`Dockerfile`、`compose.yaml`、`frontend/**`、JaCoCo 阈值（0.90）与 `excludes`（8 项）、`mysql-connector-j` pin（9.7.0）、`maven.compiler.release=17`。**没有**：新增/删除测试用例、改写断言、`standaloneSetup` → `@WebMvcTest` 重构、以 `@SuppressWarnings` / 编译参数压制警告、顺手修 `GoogleRecaptchaVerifier` 的 2 条弃用。

### 1.5 仍存在的风险 / 未验证项 / 后续事项

| 类别 | 内容 |
| --- | --- |
| 未验证（本步范围外，需等 PR） | **本步的 PR 与其 CI**：目前**无 PR、未提交、未推送**，因此**不得**声称「已合并」或「CI 绿」；PR 出现后由主 agent 核验并回填 |
| 已知残余（非本步回归） | 弃用警告总数仍为 **2**（`GoogleRecaptchaVerifier` 的 `setConnectTimeout`/`setReadTimeout`）→ 归 **S6** 评估/可选修复。因此「整个构建零弃用警告」**当前不成立**，只能说「`MockBean` 相关弃用为 0」 |
| 边界限制 | 11 个 mock 目标里有 1 个是**接口**（`RecaptchaVerifier`），故本 step 只证明了「按类型 override 接口/服务 bean」这一条路径；`@MockitoSpyBean`、`@MockitoBean(name=…)`、`MockitoBean` 的 `reset` 语义**未在本仓库使用，也未验证** |
| 证据方法限制 | 增量 `verify` 无法产出弃用警告证据（1.2）；今后任何人复核本 step，**必须用 `mvn clean verify`** 采集 before/after，否则会得到 0/0 的假绿 |
| 后续事项 | ① 提交/推送/建 PR（主 agent + `github-operator`），PR 说明必须带上 1.2 的 before/after 口径与 1.3 的 cache-key 说明；② S6 复核「最终形态」的全量门禁时，`MockBean` 计数应为 0（本步产物）；③ S7 收尾时架构文档 §7 的测试模式描述已在本轮同步（`@MockitoBean` + cache-key 提示），无需再改 |

**未发现其他已知遗留问题**：除此之外没有已知缺陷、没有未记录的偏离、没有为通过门禁而做的临时改动。工作区仅剩开工前既存的未跟踪目录 `?? .idea/`（不属于本次改动，**不得**带入提交）。

---

## 2. 新增知识点 / 重点学习对象

### 2.1 Java / Spring Test（本次最核心）

1. **`@MockitoBean` 与 `@MockBean` 的关系**
   - 含义：`@MockBean` 是 Spring Boot 自 2.2 起的「替换/补位一个 mock bean」注解；Boot **3.4** 起被标记 `@Deprecated(forRemoval = true)`，官方替代品是 Spring Framework **6.2** 的 `@MockitoBean`（配套 `@MockitoSpyBean` 替代 `@SpyBean`）。两者都作用于**测试类的字段**，由 test context 在建立/刷新 `ApplicationContext` 时把 mock 注册进 `BeanFactory`。
   - 位置：9 个 `src/test/java/com/fabflix/boot/**/*ControllerTest.java`；import 由 `org.springframework.boot.test.mock.mockito.MockBean` 变为 `org.springframework.test.context.bean.override.mockito.MockitoBean`。
   - 为什么值得学：这是 **Boot 3.x 迁移里最常见的「能编译但一直刷警告」项**；理解它是「同一框架能力的归属迁移」（从 Boot 的 test 支持搬到 Framework 的 `spring-test` bean override 体系），就能预期：① 不需加新依赖；② 行为等价但**作用域语义由 Framework 定义**，长期应该按 Framework 的 bean override 文档来写新测试。

2. **bean override 与 Spring Test 的 context cache 机制**
   - 含义：Spring Test 用 `MergedContextConfiguration` 做 key 缓存 `ApplicationContext`；key 包含配置文件、`@ContextConfiguration`、profiles、propertySource、**`contextCustomizers`** 等。测试注解（`@MockBean`/`@MockitoBean`）通过 `ContextCustomizerFactory` 产出 customizer 参与 key，因此「mock 集变化」是否触发新 context，取决于该 customizer 的 `equals/hashCode`。
   - 本次结论：`BeanOverrideHandler.equals` 比较 **类型 + 字段名 + strategy + qualifier**（不含测试类），所以「mock 形状相同 → 共享上下文；mock 集不同 → 不共享」。对照 `@MockBean` 的 `MockitoContextCustomizer`（比较 `MockDefinition` 集合）思路一致，但比较字段不同。
   - 为什么值得学：**这是「测试慢」和「测试互相污染」的根因级知识**。多加一个 `@MockBean`/`@MockitoBean`、改个字段名、或给 mock 起 `name=`，都可能让缓存 context 数量翻倍（构建时间）或意外共享（状态泄漏）。理解 key 的构成，比记住「@MockBean 会重启 context」这类口口相传的结论可靠得多。

3. **如何证明「弃用警告消失了」（证据方法学）**
   - 含义：javac 的弃用警告**只在真正编译时产生**。Maven 的增量构建在 `target/` 已是最新时会输出 `Nothing to compile - all classes are up to date.`，此时日志里**任何** `grep -c deprecat` 都必然是 0 —— 用这种日志做 before/after 对照会得到「0 → 0」的假证据（既不证明之前有 11 条，也不证明之后为 0）。
   - 正确做法：**before 与 after 都用 `mvn clean verify` 重编译**，并用**范围明确的正则**（本 step 是 `MockBean.*deprecat`）避免把无关弃用（如 `RestTemplateBuilder.setConnectTimeout`）混进来；同时记录「整份日志的弃用总数」（本例 13 → 2）作为交叉校验。
   - 为什么值得学：迁移类任务里「证明某事消失」比「证明某事仍工作」更容易造假；掌握「必须强制重编译 + 正则要窄 + 用总数交叉校验」这套方法，能防止把假绿写进 PR 和发布记录。

### 2.2 Java 语法 / 代码风格

4. **import 的字母序与「机械替换」的相互作用**
   - 含义：仓库沿用 IDE 风格的**字典序 import 分组**（`java.*` → 第三方 `org.*` → `static`）。旧 import `org.springframework.boot.test.mock.mockito.MockBean` 排在 `org.springframework.boot.*` 区段；新 import `org.springframework.test.context.bean.override.mockito.MockitoBean` 属于 `org.springframework.test.*` 区段。因此**不能无脑原位替换**：9 个文件中有 7 个需要把 import 移到 `org.springframework.test.web.servlet.MockMvc` 之前（其中 4 个还要越过 `org.springframework.context.annotation.Import` / `org.springframework.mock.web.MockHttpSession`），只有 2 个（`MainControllerTest`、`MovieListControllerTest`）原位替换后仍满足字母序。
   - 位置：9 个测试文件的 import 区；diff 表现为「1 行删除 + 1 行新增」（不在同一位置），所以 `git diff --stat` 仍是 `20 insertions / 20 deletions` 而看不出位移。
   - 为什么值得学：**做「纯机械替换」时要理解 diff 的读法**：`+20/−20` 不等于「同一位置改了 20 行」，要配合 `git diff -U0` 看 hunk 头（`@@ -8 +7,0 @@` 与 `@@ -10,0 +10 @@` 是两处独立位移）。这直接影响 review 时对「是否只改了 import 和注解」的判断，也是 review 里 P1「分支夹带 S4」误判的成因——**判断依据应是 `git log origin/main..HEAD` 与 `git diff --cached`，而不是 diff 行数**。

> 本次没有新增前端（React/vitest）、SQL、Docker 或构建插件方面的知识点：本 step 只触及 `src/test/java` 的注解与 import，`pom.xml`、容器资产与前端零改动。

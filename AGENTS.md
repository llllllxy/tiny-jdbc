# tiny-jdbc 项目开发指引

> 本文件是 tiny-jdbc 仓库的**项目级**指令，优先级高于用户全局 `~/.dsh/AGENTS.md`。
> 通用编码原则（最小改动、不过度封装、不机械拆方法、不擅自重构）见全局文件，
> 这里只写**本项目专属**约定，避免重复。

---

## 一、项目定位

- 基于 Spring `JdbcTemplate` 的轻量 ORM 框架，不引入 MyBatis 之类的独立执行引擎。
- 版本号**单点维护**：根 `pom.xml` 的 `<revision>` 属性，所有模块继承；改版本只改这一处。
- 开源协议 Apache-2.0，仓库内容对公众可见。

## 二、模块职责

| 模块 | 职责 | 注意 |
|---|---|---|
| `tiny-jdbc-core` | ORM 主体：SQL 组装、条件构造器、DAO 支撑、分页方言、主键生成、拦截器、自动填充 | 对外发布的核心包，**API 兼容性红线在此** |
| `tiny-jdbc-boot-starter` | Spring Boot 自动配置与配置属性绑定 | 只做装配，业务逻辑一律放 core |
| `tiny-jdbc-codegen` | 基于 FreeMarker 的实体 / DAO 代码生成器 | 模板位于 `src/main/resources/templates/` |

依赖方向**单向**：`starter → core`、`codegen → core`；core 不得反向依赖其它模块。

## 三、技术栈与兼容性红线

- **Java 8**（`maven.compiler.source/target=8`）：禁用 `record`、`var`、`List.of`、`instanceof` 模式匹配等 9+ 语法。
- **Spring Framework 5.3.x / Spring Boot 2.7.x**：新增 API 不得抬高依赖基线。
- **测试用 JUnit 4**（`org.junit.Test`），不要引入 JUnit 5。
- **不新增第三方依赖**：先确认项目已有工具类能否满足（`util` 包下有大量自研工具）。
- 2.0.x 版本内**不得破坏公共 API**：不改已有方法签名与语义，不删除已发布的公开类型；确需破坏性变更时，必须先与维护者确认并写入 `CHANGELOG.md` 的「破坏性变更」。

## 四、关键类定位

| 关注点 | 入口 |
|---|---|
| SQL 组装（只组装不执行） | `support/SqlAssembler`，方法统一 `build*` 前缀 |
| 执行与主键回写、自动填充 | `support/AbstractSqlSupport` |
| 条件构造器 | `criteria/Criteria`（状态与参数）→ `AbstractCriteria`（字符串版）/ `AbstractLambdaCriteria`（Lambda 版）→ `query`、`update` 子包下的具体实现 |
| 标识符安全校验 | `util/SqlIdentifierUtils` |
| 分页 | `page/IPageHandle` + 各方言实现 + `page/PageHandleFactory` |
| 数据库类型识别 | `util/DbType`、`util/DbTypeUtils` |

## 五、构建与测试

```bash
# 单个模块全量测试
mvn -pl tiny-jdbc-core test
mvn -pl tiny-jdbc-codegen test
mvn -pl tiny-jdbc-boot-starter test

# 定点运行某个测试类（PowerShell 下 -Dtest 需加引号）
mvn -pl tiny-jdbc-core "-Dtest=CriteriaAndOrVerifyMain" test
```

### 测试命名与收录规则

- core 的测试类统一命名为 `XxxVerifyMain`，放在 `org.tinycloud.jdbc.verify` 包下。
- `tiny-jdbc-core/pom.xml` 的 surefire `includes` 已配置收录 `**/*VerifyMain.java`、`**/*Test.java`、`**/*Tests.java`、`**/*TestCase.java`，新增测试沿用这些命名即可被自动执行。
- codegen 使用常规 `XxxTest` 命名。

### 重要限制：core 测试不连接数据库

core 的 `*VerifyMain` 是**内存 SQL 组装 / 行为验证**，只断言生成的 SQL 文本、参数列表与内存逻辑，**不会真正执行数据库语句**。因此以下几类改动**无法由现有单测证明**：

- 自增主键回写（依赖驱动返回的 `GeneratedKeyHolder`）；
- 各数据库分页方言的真实执行结果；
- 驱动相关的类型转换与 `ResultSet` 取值差异。

对这类改动，必须如实说明「已通过 SQL 组装断言，但未在真实数据库上验证」，**不得声称已验证**。

### 验证要求

- 改哪个模块，至少跑该模块测试。
- 涉及公共支撑（`criteria`、`SqlAssembler`、分页、主键生成、拦截器）时，必须跑 **core 全量**。

## 六、SQL 与安全约定

1. **标识符必须校验**：表名、列名、别名走 `SqlIdentifierUtils`，不要自行拼接未校验的标识符。标识符无法用 `?` 参数化，只能白名单校验。
2. **值一律参数化**：用 `?` 占位符，不做字符串拼接。
3. **不受限原 SQL 必须显式授权**：需要拼接原始片段时用 `RawSql` 包裹表达信任，不允许绕过校验。
4. **方言分页按顶层 SELECT 定位**：新增或修改分页实现时，用 `page/PageSqlUtils.findTopLevelSelect`，禁止固定下标 `insert(6, ...)` 或非锚定 `replaceFirst("(?i)select", ...)`——必须正确处理前导空白、注释、字符串与 CTE 子查询。
5. **count SQL 规则**：忽略 `ORDER BY` 与 `last()` 尾片段；存在 `GROUP BY` / `HAVING` 时必须用 `SELECT COUNT(*) FROM (SELECT 1 ... ) 别名` 子查询统计分组数，不能直接在末尾追加 `HAVING`。
6. **参数顺序固定**：更新 SET 值 → WHERE → HAVING。查询 / 分页 / count 用 `getConditionParameters()`，更新用 `getParameters()`，不要混用。
7. **注意数据库差异**：改动 SQL 前确认目标数据库语法（MySQL、Oracle、DB2、PostgreSQL、SQL Server、GBase8s、Informix、GaussDB、Trino 等），不要假设各方言一致。

## 七、criteria 连接符状态机（高发错误区）

`Criteria.nextIsOr` 表示「下一个条件用 OR 连接」，由 `getConditionPrefix()` 消费后自动复位。历史上这里出过两类静默错误（SQL 能执行但结果集错误），改动时务必遵守：

1. 任何新增条件方法**必须使用 `this.getConditionPrefix()`**，不得硬编码 `" AND "` / `" OR "`。
2. 条件被跳过时（`whether=false`、空嵌套块）**必须清理待消费状态**，否则 OR 会泄漏到后续不相关条件。
3. **字符串版与 Lambda 版必须同步修改**，两处方法一一对应，不要只改一侧。
4. 修改后至少覆盖这些场景：`.or().and(...)` 的连接符、`whether=false` 后续条件的连接符、空嵌套块、以及参数顺序（见 `CriteriaAndOrVerifyMain`）。

## 八、代码生成器约定

1. **模板与输出统一 UTF-8**：写文件必须用 `Files.newBufferedWriter(path, StandardCharsets.UTF_8)`，禁止使用无 charset 的 `FileWriter`（Windows 非 UTF-8 环境下中文会乱码）。
2. **类型映射单一来源**：数据库类型 → Java 类型只在 `util/TypeUtils` 定义。注意 `FLOAT` / `DOUBLE` → `Double`，`REAL` → `Float`。
3. 模板变量变更需同步 `entity.ftl` 与 `dao.ftl`，并在 `CodeGeneratorTest` 补断言。

## 九、自动配置约定

1. Starter 中提供的默认 Bean 必须带 `@ConditionalOnMissingBean`，允许使用者覆盖，否则用户自定义同名 Bean 会导致启动失败。
2. 新增配置项写入 `TinyJdbcProperties`，并提供默认值以保持向后兼容。

## 十、文档维护

1. `CHANGELOG.md`：按版本记录变更，区分「新增特性 / 稳定性修复 / 破坏性变更」等维度。
2. `AUDIT_REPORT.md`：审计条目格式为 `文件@行号 | 严重级别 | 问题 | 修复建议`。修完后把该条目标注为 `【已修复】` 并附验证结果，**不要删除历史条目**。
3. `README.md`、`SQL.md`：面向使用者的用法说明，公共 API 变更时同步更新。

## 十一、提交规范

- **中文提交信息**，格式 `type(scope): 摘要`，type 取 `feat` / `fix` / `refactor` / `docs` / `test` / `chore`。
- 正文分条列具体改动点，末尾注明验证结果（例如「core 全量测试通过」）。
- **一次提交只做一件事**：功能修复与重构不要混在同一次提交里。

## 十二、交付前自检

- [ ] 编译通过，无未使用的 import
- [ ] 改动的模块测试通过；公共支撑改动已跑 core 全量
- [ ] criteria 的字符串版与 Lambda 版已同步
- [ ] 未新增第三方依赖，未使用 Java 9+ 语法
- [ ] 未破坏公共 API 与既有业务行为
- [ ] 无法用单测覆盖的部分（真实数据库行为）已在说明中如实标注
- [ ] 未改动与本次任务无关的代码

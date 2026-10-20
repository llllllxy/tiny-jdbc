# 更新日志

本文件依据 Git 标签、版本发布提交及对应代码差异整理。每个版本均给出可回溯的发布锚点；标记为“升级必读”的版本包含已确认的 API 或包路径迁移。

## 2.0.3（未发布）

> 升级路径：2.0.2 → 2.0.3 ｜ 主题：SQL 构造 DSL 安全加固（AUDIT_REPORT.md 第三章 15 条）

### 类型概览

| 维度 | 内容 |
|------|------|
| 破坏性变更 | 操作符白名单、`FuncBuilder` 字符串参数按列引用校验、`FuncExpr.of(String)` 收紧、`eq(col, null)` 归一为 `IS NULL`、非 CROSS JOIN 强制 ON、**移除 `IdUtils`**、**`selectByIds`/`deleteByIds` 入参改为 `Collection<? extends ID>`** |
| 安全加固 | 收敛为三个校验闸门（操作符白名单 / 标识符校验 / 原始片段信任模型），同时关闭审计未点名的约 40 个同类注入点 |
| 稳定性修复 | 空条件组、UNION 自引用、IN 集合快照、`GROUP_CONCAT` 分隔符、`limit`/`offset` 负数、`IdGeneratorInterface` 默认方法空上下文 NPE |
| 新增 API | `SqlOperatorUtils`、`RawSql` 版本的操作符重载、`FuncBuilder.raw(RawSql)`、`FuncExpr.of(RawSql)`、`SqlIdentifierUtils.checkFunctionName` |
| 测试 | 新增 `SqlDslSecurityVerifyMain`（23 项）、`IdGeneratorVerifyMain` 补充 7 项（空上下文与生成器入口回归）、新增 `ByIdsCollectionVerifyMain`（4 项）；core 全量 213 项通过 |

### ⚠️ 破坏性变更（升级必读）

本章节的共同主题是：**字符串参数不再被默认为「受信 SQL」**。此前多个入口把调用方传入的字符串原样拼进 SQL，这既是注入面，也让「列名」与「自由表达式」两种语义混在一起。现在二者被显式区分：

| 语义 | 用法 |
|------|------|
| 列引用（受校验） | 直接传 `String`，如 `col("u.name")`、`and("a.id", "=", 1)` |
| 自由表达式（不受限） | 用 `RawSql.wrap("...")` 显式授权，如 `FuncBuilder.raw(RawSql.wrap("price * qty"))` |

- **操作符白名单（`SimpleCondition` / `SubQueryCondition` / `Join.on|and` / `ConditionGroup.and` / `SQL.on`）**：操作符不再原样拼接，只接受白名单内的运算符。比较类白名单：`=`、`<>`、`!=`、`>`、`<`、`>=`、`<=`、`<=>`、`LIKE`、`NOT LIKE`、`REGEXP`、`NOT REGEXP`、`RLIKE`、`NOT RLIKE`、`SOUNDS LIKE`；子查询类：`IN`、`NOT IN`、`EXISTS`、`NOT EXISTS` 及上述比较符。输入会先规范化（去首尾空白、折叠内部空白、`Locale.ROOT` 转大写）后再渲染，因此**生成的 SQL 中操作符一律为大写**（`and("a", "like", …)` 现在输出 `a LIKE ?` 而非 `a like ?`；`"not  like"` 这类多空格写法会被修正为 `NOT LIKE`，此前会拼出非法 SQL）。白名单外的方言运算符需改用新增的 `RawSql` 重载：`and("a.flag", RawSql.wrap("&"), 1)`。
- **⚠️ 新增 `RawSql` 重载带来的编译期歧义**：由于 `and/on` 同时存在 `(String, String, Object)` 与 `(String, RawSql, Object)`，把**字面量 `null`** 作为操作符传入（`and("a", null, 1)`）会因两个重载都无法确定而编译失败。这种写法本身无意义（此前会拼出 `a null ?`），但升级时若遇到该编译错误，请显式转型：`and("a", (String) null, 1)`，或改用 `isNull` / `isNotNull`。
- **`FuncBuilder` 字符串参数按列引用校验**：`col` / `count` / `sum` / `avg` / `max` / `min` / `upper` / `dateFormat` / `distinct` / `concat` … 等约 40 个方法的 `String` 参数此前直接原样拼接，现一律按列引用白名单校验。`count("a + b")` 这类自由表达式会抛 `TinyJdbcException`，需改为 `FuncBuilder.count(FuncBuilder.raw(RawSql.wrap("a + b")))`。
- **`caseWhen` / `_if` 等「表达式位」不再接受裸字符串**：`FuncBuilder.caseWhen("status = 1", ...)`、`FuncBuilder._if("1=1", ...)` 这类写法（`String` 曾被文档描述为「裸 SQL」）现会报错，需改为 `RawSql.wrap("status = 1")`，或传入 `FuncExpr` / `FuncBuilder.col(...)`。这是本次最容易踩到的一条。
- **`FuncExpr.of(String)` 弃用并收紧**：只接受列引用，自由表达式改用新增的 `FuncExpr.of(RawSql)`。方法已标 `@Deprecated`，计划在 2.1.0 移除。
- **`eq(col, null)` / `notEq(col, null)` 归一为 `IS NULL` / `IS NOT NULL`**：此前生成 `= ?` 并绑定 `null`（三值逻辑恒不匹配），现直接产出 `IS NULL` / `IS NOT NULL` 且**不再产生参数**，SQL 文本与参数个数都会变化。通用入口 `and(col, "=", null)`、`and(col, "!=", null)` 同样归一；其它运算符（如 `> null`）无意义，直接抛异常。
- **非 `CROSS JOIN` 必须至少有一个 ON 条件**：`leftJoin("u", "u")` 而不调 `on(...)` 会在渲染期报错（此前生成 `LEFT JOIN u u` 这种非法 SQL），需要「无 ON」请改用 `crossJoin`。
- **`limit` / `offset` 拒绝负数**（此前生成 `LIMIT -1` 非法 SQL）；`limit(0)` 仍然合法。
- **`GROUP_CONCAT` 分隔符输出变化**：`groupConcat(field, separator)` 由原样拼接 `GROUP_CONCAT(name, SEPARATOR ,)` 改为 `GROUP_CONCAT(name SEPARATOR ',')`。这里一并修掉两个问题：分隔符未加引号（不可执行），以及 `SEPARATOR` 前多了一个逗号——按 MySQL/MariaDB 语法 `GROUP_CONCAT([DISTINCT] expr [,expr ...] [ORDER BY ...] [SEPARATOR str_val])`，`SEPARATOR` 前**没有**逗号（逗号表示「按行拼接多个表达式」，是另一种用法）。SQL 文本变化，但旧文本本身无法执行。
- **UNION 自引用 / 重复 / 跨管道合并改为报错**：`a.union(a)`、同一片段重复入队、把已属于另一条管道的片段合并进来，此前会导致渲染死循环或交叉引用，现直接抛异常。
- **`FuncExpr.getParameters()` 返回只读视图**：此前返回内部可变 `List`，调用方可借此改动表达式状态；现在修改会抛 `UnsupportedOperationException`。
- **`InCondition` 构造时对集合做快照**：构建后调用方再增删原集合不再影响已生成的条件（此前会造成占位符数量与参数列表不一致）。
- **⚠️ `selectByIds` / `deleteByIds` 入参由 `List<ID>` 改为 `Collection<? extends ID>`**：`IObjectSupport` 的两个方法与 `AbstractSqlSupport` 的对应实现同步修改，调用方可直接传入 `List`、`Set` 或任意 `Collection` 实现，无需再手工转换。
  - **为什么用 `? extends ID` 而不是 `Collection<ID>`**：泛型是不变型，`Collection<ID>` 在主键声明为 `Serializable`（代码生成器无法推断具体类型时的常见结果）时**仍然无法接受 `Set<Long>`**（`Collection<Long>` 不是 `Collection<Serializable>`）。只有 `Collection<? extends ID>` 才能同时接受 `Set<Long>` 与 `Set<Serializable>`。若仅把 `List` 换成 `Collection` 而不加通配符，等于付出了破坏兼容的代价却没解决实际问题。
  - **迁移影响**：
    - 直接调用方（`dao.selectByIds(list)` 等）：**源码兼容**，`List<ID>` 实参可自动匹配 `Collection<? extends ID>`，无需改动。
    - 已编译的调用方：**二进制不兼容**，方法描述符变化会导致升级后 `NoSuchMethodError`，需重新编译。
    - 直接实现 `IObjectSupport` 的第三方类：**源码不兼容**，必须把 `List<ID>` 改为 `Collection<? extends ID>`，否则编译失败。项目内唯一实现是 `AbstractSqlSupport`，已同步修改。
    - 覆写了 `selectByIds(List<ID>)` / `deleteByIds(List<ID>)` 的自定义 DAO：原 `@Override` 失效，会退化为重载而非覆写，**可能静默调用父类实现**，升级时必须检查。
  - **行为变化**：`Set` 等无序集合的迭代顺序与插入顺序不同，生成的 SQL 参数顺序会随之变化（`IN` 语义不依赖顺序，结果集不变）。若日志比对或语句缓存依赖 SQL 文本，需注意差异。
  - 可变参数重载 `selectByIds(ID...)` / `deleteByIds(ID...)` 保持不变，行为与签名均未调整。
- **⚠️ 移除 `IdUtils`（`2.0.3` 立即移除）**：`org.tinycloud.jdbc.id.IdUtils` 整体删除，**未经过弃用过渡期**。移除原因有两条：一是它与 `TinyJdbcRuntime` 各自持有一份 `SnowflakeId` 实例，而节点标识（workerId / datacenterId）由「MAC + 进程号」推导，同进程内两份实例必然得到相同节点标识，**会生成重复 ID**；二是它的 `objectId()` / `simpleUUID()` / `randomUUID()` 只是 JDK 与 `ObjectId` 的一行封装，无独立价值。替代写法见下方「替代方案」。
  - **替代方案**：
    - `IdUtils.nextId()` / `nextLongId()` / `getInstance()` → 注入 `TinyJdbcRuntime`，调用 `getSnowflakeId().nextId()`（与框架内部共用同一实例，也顺带消除了重复 ID 风险）。
    - `IdUtils.objectId()` → `ObjectId.nextId()`（`public static`，返回 24 位 `String`）。
    - `IdUtils.simpleUUID()` → `UUID.randomUUID().toString().replace("-", "")`（32 位）。
    - `IdUtils.randomUUID()` → `UUID.randomUUID().toString()`（36 位）。
  - **非 Spring 场景**：`TinyJdbcRuntime` 构造器需要 7～9 个参数，不宜由工具类调用方自行创建。若确实需要在无 Spring 环境下获取雪花 ID，请自行持有 `new SnowflakeId(workerId, datacenterId)` 并显式指定节点标识，不要依赖自动推导，否则与其他实例存在撞号风险。
  - README 已新增「手工获取各类 ID」章节（第 9 章），给出全部替代示例。

### 安全加固

- **闸门①：操作符白名单（新增 `util/SqlOperatorUtils`）**：所有操作符最终都汇入 `SimpleCondition` / `SubQueryCondition` 两个构造器，白名单在这两处统一强制，因此 `Join.on|and`（4 个方法）、`ConditionGroup.and|andIfAbsent`（6 个方法）、`SQL.on` 一次性全覆盖。`Join` 的 ON 条件已重构为直接复用 `SimpleCondition`，与 WHERE 条件共享同一套操作符与空值语义。
- **闸门②：标识符校验**：`SQL.table(table, alias)` 补上 `checkAlias`（此前只校验表名）；`FuncExpr.as(alias)` 由「仅判空」改为 `checkAlias`；`SubQueryCondition` 补齐子查询非空、操作符白名单，以及「`EXISTS`/`NOT EXISTS` 不得带列、`IN` 必须带列」的组合校验。
- **闸门③：原始片段信任模型**：`FuncExpr` 内部拆分为两条路径——`raw(String)` 按列引用校验（覆盖全部调用方字符串），新增包内可见的 `fragment(String)` 承载框架自建片段（`NOW()`、`DISTINCT col`、`INTERVAL n DAY`、整型字面量等），审计未点名的约 40 个 `FuncBuilder` 参数位由此一并收口。函数名经新增的 `SqlIdentifierUtils.checkFunctionName` 校验，拒绝 `"COUNT) OR (1=1"` 之类输入。
- **受信出口显式化**：新增 `FuncBuilder.raw(RawSql)` 与 `FuncExpr.of(RawSql)`；`RawSql` 版本的操作符重载使「确需白名单外运算符」有明确、可搜索的表达方式，而不是放宽白名单。

### 稳定性修复

- **空条件组不再渲染成 `()`**：`and` / `or` / `group` 传入空 `Consumer` 时跳过该嵌套块（不再产生非法 `()` 或悬空连接符），`GroupElement.toSql()` 与 `ConditionGroup.toSql()` 同时做防御性跳过。
- **UNION 管道完整性**：拒绝自引用、重复入队与跨管道合并。
- **`InCondition` 集合快照**：修复构建后外部改动集合导致占位符与参数不一致。
- **`GROUP_CONCAT` 分隔符按值转义**：分隔符被单引号包裹并双写转义 `'` / `\`，注入尝试无法逃逸字面量。
- **`IdGeneratorInterface` 默认方法空上下文 NPE（2.0.2 引入的回归）**：`IdGeneratorInterface` 新增 `nextId(IdContext)` 默认方法后，调用方写 `nextId(null)` 时 `IdContext` 比 `Object` 更具体，重载解析会选中该默认方法并触发 `context.getObj()` 的 NPE。该缺陷曾导致 `IdUtils` 的 `nextId()` / `nextLongId()` / `simpleUUID()` / `objectId()` 全部失效。修复内容：默认方法对 `null` context 降级为空实体，`SequenceGenerator.nextId(IdContext)` 补上 `null` 上下文校验（缺失时抛 `TinyJdbcException` 而非 NPE）。`IdUtils` 本身已在本次版本移除，不再涉及。
  - **版本订正**：该缺陷由 `9b794ce` 引入，`9b794ce` 已包含在 `v2.0.2` 标签内，因此**实际受影响版本为 2.0.2**，而非此前记录的「2.0.3 待发布」。2.0.3 为修复版本。

### 升级检查清单

- [ ] 全局搜索 `.and(`、`.on(`、`Join` 相关调用，确认自定义操作符都在白名单内；否则改用 `RawSql.wrap(...)` 重载。
- [ ] 全局搜索 `FuncBuilder.col(` / `count(` / `sum(` 等，确认字符串参数是列引用；自由表达式改用 `FuncBuilder.raw(RawSql.wrap(...))`。
- [ ] 全局搜索 `caseWhen(` / `_if(`，字符串条件改用 `RawSql.wrap(...)` 或 `FuncFactory`/`FuncExpr` 表达式。
- [ ] 全局搜索 `FuncExpr.of(`，改列为 `FuncExpr.of(RawSql.wrap(...))`（自由表达式）或 `FuncBuilder.col(...)`（列引用）。
- [ ] 复核依赖 `eq(col, null)` 生成 `= ?` 或依赖其参数个数的代码（如手工拼装参数数组、参数下标断言）。
- [ ] 复核无 ON 的 `leftJoin` / `rightJoin` / `innerJoin` 调用，改用 `crossJoin` 或补上 ON。
- [ ] 若断言了 `GROUP_CONCAT(... SEPARATOR x)` 的 SQL 文本，需按新的字面量形式更新。
- [ ] **全局搜索 `selectByIds(` / `deleteByIds(`**，确认是否有自定义 DAO 覆写了 `List<ID>` 版本；若存在需改为 `Collection<? extends ID>`，否则覆写会失效。
- [ ] **全局搜索 `IdUtils`**，按「移除 `IdUtils`」章节的替代方案逐个替换：雪花 ID 改为注入 `TinyJdbcRuntime`，`objectId()` 改为 `ObjectId.nextId()`，`simpleUUID()` / `randomUUID()` 改为 JDK `UUID`。

### 兼容性说明

| 用户侧能力 | 兼容性 |
|------------|--------|
| 基于 `BaseDao` 的常规 CRUD 调用 | ✅ 完全兼容 |
| `criteria` 条件构造器 | ✅ 完全兼容 |
| 分页 / 主键生成 / 代码生成器 | ✅ 完全兼容 |
| `SQL` 构造器：表名、列名、别名、排序、分组、插入、更新 | ✅ 兼容（校验更严，合法标识符不受影响） |
| `FuncBuilder` 传列名的既有用法 | ✅ 兼容 |
| 自定义操作符（白名单内） | ✅ 兼容 |
| 自定义操作符（白名单外） | ⚠️ 需改用 `RawSql` 重载 |
| `FuncBuilder` 传自由表达式的用法 | ⚠️ 需改用 `FuncBuilder.raw(RawSql.wrap(...))` |
| `caseWhen` / `_if` 传裸字符串条件 | ⚠️ 需改用 `RawSql.wrap(...)` |
| `FuncExpr.of(String)` 传自由表达式 | ⚠️ 需改用 `FuncExpr.of(RawSql)`；方法已弃用 |
| `eq(col, null)` 依赖 `= ?` 语义或参数个数 | ⚠️ 行为变更，现为 `IS NULL` 且无参数 |
| 无 ON 的非 CROSS JOIN | ❌ 不再允许，改用 `crossJoin` |
| `groupConcat` 分隔符的 SQL 文本 | ⚠️ 文本变化（`GROUP_CONCAT(name SEPARATOR ',')`），旧文本无法执行 |
| 直接调用 `IdUtils.*` | ❌ 类已移除，见「移除 `IdUtils`」章节的替代方案 |
| 调用 `selectByIds` / `deleteByIds`（传 List） | ✅ 源码兼容；⚠️ 需重新编译（二进制不兼容） |
| 调用 `selectByIds` / `deleteByIds`（传 Set 等集合） | ✅ 新增能力 |
| 直接实现 `IObjectSupport` 的第三方类 | ❌ 需把 `List<ID>` 改为 `Collection<? extends ID>` |
| 覆写 `selectByIds(List<ID>)` 的自定义 DAO | ⚠️ `@Override` 失效，可能静默调用父类实现 |
| `IdGeneratorInterface` 自定义实现（仅实现 `nextId(Object)`） | ✅ 兼容，默认方法会正确降级 |

---

## 2.0.2

> 发布日期：2026-09-08 ｜ 发布提交：`ab518531` ｜ 升级路径：2.0.1 → 2.0.2

### 类型概览

| 维度 | 内容 |
|------|------|
| 新增特性 | 内置主键生成器 `NanoId` / `ULID`；按 `@Column` 的结果映射器 `TableRowMapper`；批量插入模式 `BatchMode` |
| 架构重构 | 主键生成策略模型化（`IdGeneratorInterface` + `IdGeneratorRouter`）；生成器类下沉到 `id.generator` 子包；`SqlGenerator` 重命名为 `SqlAssembler`，方法统一 `build*` 前缀 |
| 稳定性修复 | 雪花 ID 回退与解析稳健化、DB2 分页写法、SQL 参数渲染与主键校验等多处修复 |
| 结果类型映射 | `TableRowMapper` 改按目标属性类型精确取值，修复 `BLOB/CLOB`、Oracle 特殊类型、`primitive` 空值、枚举及 `java.time` 转换；新增「列 → 字段」映射 |
| 代码生成器 | 按列精度精确映射 Java 类型；`IdType` 默认按主键自增自动推断；实体类注释模板修复 |
| 破坏性变更 | `SqlGenerator` 重命名为 `SqlAssembler`（直接调用方需修正用名）；`nanoId` / `ulid` / `uuid` 主键字段类型要求 `String` |
| 条件构造器 | `criteria` 补齐 `GROUP BY` / `HAVING`、聚合列表达式 `selectExpr`、`EXISTS` 子查询、`apply` 原始片段、同名 `in`/`notIn` 可变参数、`setField` 字段赋值 |

### 新增特性

- **NanoId 主键生成器（`bdeadef`）**：新增 `IdType.NANO_ID` 内置主键生成策略，生成面向 URL 的紧凑随机字符串。
- **ULID 主键生成器（`0559d53`）**：新增 `IdType.ULID` 内置主键生成策略，生成有序、可排序的 26 位 ULID 字符串。
- **按 `@Column` 的结果映射器（`362bf3f`）**：新增 `TableRowMapper`，识别 `@Column.value()` 做「列名 → 属性名」精确映射，修复自定义 `@Column` 查询结果映射为 `null` 的问题；`AbstractSqlSupport` 的 `select` / 分页查询默认改用该映射器（赋值仍复用 Spring `BeanWrapper`，类型转换能力与 `BeanPropertyRowMapper` 一致）。
- **`TableRowMapper` 按目标类型精确取值（`b3feadd`）**：结果映射改按目标属性类型经 Spring `JdbcUtils.getResultSetValue` 精确取值，替代原先的 `rs.getObject` 裸取值，修复 `BLOB/CLOB`、Oracle 特殊类型、`primitive` 空值、枚举及 `java.time` 等场景的转换失败问题；`TableRowMapper` 注入 `DefaultConversionService`，类型处理能力与 `BeanPropertyRowMapper` 对齐。随同新增 `TableParserUtils.resolveColumnToFieldMap` / `TableInfo.getColumnToFieldMap`，暴露「列名 → 字段」映射。
- **`FuncBuilder` 补齐 Lambda 重载（`d81c19a`）**：`FuncBuilder` 支持 `TypeFunction`（方法引用）形式的函数参数，用法更贴近流畅 API。

### 安全性加固

- **标识符安全边界（`待发布`）**：新增 `SqlIdentifierUtils` 白名单校验器，对 SQL 构建器 / 条件构造器中的表名、列引用、别名、裸列名做默认严格校验，拒绝含空白、分号、引号、注释、括号、前导数字等非法标识符，阻断标识符拼接注入。`last(String)` 也默认做尾部片段安全校验（拒绝分号 / 引号 / 注释符）；新增受信任原始 SQL 标记 `RawSql`（`SQL.raw(...)` / `last(RawSql)`）作为显式授权出口，仅应传入可信常量。Lambda 列（`TypeFunction`，经实体元数据解析）与 `FuncBuilder` 表达式参数保持透明、不做校验。

- **原始 SQL 片段加固（`待发布`）**：新增 `SqlIdentifierUtils.checkTailClause()`，在字符级校验之上拒绝 `SELECT` / `UNION` / `INSERT` 等语句级关键字与顶层 `OR` / `AND`，`last(String)` 与 `apply(String)` 已接入，避免 `last("LIMIT 1 UNION SELECT ...")`、`apply("1=1 OR 1=1")` 改变查询语义；`UPDATE` 刻意放行以兼容 `FOR UPDATE`。含子查询的 `exists` / `having` / `selectExpr` 保持原字符级校验，不受限的原始 SQL 仍请使用 `RawSql` 显式授权。

### 批量写入优化

- **批量插入模式（`待发布`）**：新增 `BatchMode`（`JDBC_BATCH` / `MULTI_VALUE`）。`JDBC_BATCH` 为默认值，沿用 `JdbcTemplate.batchUpdate`；`MULTI_VALUE` 把集合切块后生成单条多值 `INSERT ... VALUES (...),(...)`，显著减少网络往返与解析开销。可通过 `tiny-jdbc.batch-insert-mode` / `tiny-jdbc.batch-insert-size` 配置，或调用 `batchInsert(collection, ignoreNulls, mode)` 显式指定。
- **`rewriteBatchedStatements` 定位**：该参数属于 MySQL JDBC URL 配置，框架无法代码内强制。`JDBC_BATCH` 模式下建议连接串携带 `rewriteBatchedStatements=true` 才真正合并往返；`MULTI_VALUE` 模式不依赖该参数，天然减少往返。
- **主键回写限制（`待发布`）**：批量写入下自增主键<b>不回写</b>到实体（多行 INSERT 的生成键无法可靠按行映射）；单条 `insert(entity)` 仍回写。非自增主键（如 `ASSIGN_ID` / `NANO_ID`）在批量前逐实体的 `IdGeneratorRouter` 生成并回写，再打包参数。
- **列一致性约束**：`MULTI_VALUE` 要求集合内所有实体的可写列集一致。`ignoreNulls=true` 时以首个实体的非空列集为准，后续实体列集不一致将抛出明确异常；`ignoreNulls=false` 时 null 字段以 SQL `NULL` 占位保留。
- **多值模式返回值语义（`待发布`）**：`MULTI_VALUE` 下 `batchInsert` 返回的是<b>语句级</b>影响行数，同一多值 `INSERT` 内各元素数值相同，不可对数组求和，也不能据此判断单行是否插入成功；需要真实逐行影响行数请使用 `JDBC_BATCH` 模式。

### 底层 API 调整（重命名）

- **`SqlGenerator` 重命名为 `SqlAssembler`（`待发布`）**：SQL 组装器类名更贴合「把实体 / 条件构造器 / 主键列表翻译成可执行 SQL」的职责；所有构建方法统一为 `build*` 前缀（如 `insertSql` → `buildInsertSql`、`selectByIdsSql` → `buildSelectByIdsSql`），与已有 `buildBatchInsert` 命名保持一致。直接调用旧 `SqlGenerator.*` 或按旧名 import 的项目需同步修改。
- **类注释重写（`待发布`）**：`SqlAssembler` 类级注释按「职责边界 / 输入分类 / 元数据规则 / 批量插入」四块重写，准确描述其实际作用（只组装不执行、收敛 `@Table` / `@Column` / `@Id` 规则）。


### 主键生成架构重构

- **主键生成策略模型化（`fbe96e0`）**：主键生成逻辑从 `SqlGenerator` 内联分支抽离为 `IdGeneratorInterface` + `IdGeneratorRouter`，按 `IdType` 统一分发生成器并完成类型校验、类型转换与实体字段回写。
- **生成器类下沉（`9b62283`）**：`ObjectIdGenerator` / `SequenceGenerator` / `SnowflakeIdGenerator` / `UuidGenerator` 等实现类移至 `id.generator` 子包。
- **雪花 ID 稳健化（`ac2ce74` / `e3aec67`）**：雪花 ID 回退改为随机化，修复时间戳反解与中断标志；`getWorkerId` 解析更稳健并修正注释。

### 稳定性修复

- **DB2 分页写法（`f056c01` / `163ff7c`）**：DB2 分页改用 `ROWNUMBER() OVER()` 写法，与主流分页插件保持一致；修正 DB2 分页函数名，并对未知数据库类型显式报错。
- **日志 SQL 参数渲染（`9b794ce`）**：修正日志中 SQL 参数渲染，忽略字符串与注释内的占位符，避免误替换。
- **主键与字段校验（`775edc8` / `ca3aa8b`）**：`insertSql` 增加多 `@Id` 校验；`selectSql` 全字段 `exist=false` 时给出明确错误。
- **自动填充列名解析（`250aac8`）**：修正 `strictUpdateFill` 注释，并支持按 `@Column` 解析更新列名。
- **COUNT 查询条件提取（`待发布`）**：`selectCount` 不再复用 `whereSql()` 的排序与 `last()` 尾片段，改为仅取条件部分（`whereConditions()`）；避免生成 `SELECT COUNT(*) ... ORDER BY ...` 或 `FOR UPDATE` 等，在 PostgreSQL 等数据库上非法 / 语义错误的聚合查询。

- **criteria 健壮性（`待发布`）**：`in` / `notIn` 可变参数收到 `(Object[]) null` 时不再抛 NPE，改为 `whether=true` 抛明确 `TinyJdbcException`、`whether=false` 直接跳过且不求值；嵌套 `and/or` 若使用 `selectExpr` / `groupBy` / `having` / `orderBy` / `last` 立即抛出 `TinyJdbcException`，避免 HAVING 参数进入参数列表却不被渲染造成的占位符与参数错位。

### 工具类健壮性

- **`SqlIdentifierUtils.checkTailSql` 括号配对**：新增括号深度统计校验，`(` / `)` 不配对直接抛 `TinyJdbcException`，堵住 `exists("subq) OR (1=1")` 可用不配对括号 + `OR` 绕过的注入路径。`last` / `apply` 保持关键字级加固 + 括号配对双重防护；`exists` / `selectExpr` / `having` 受括号配对保护，不含关键字级拦截。`RawSql` 不受限授权机制不变。
- **`getPrimitiveDefaultValue` 类型修正**：`int/long/float/double` 统一返回 `Integer 0` 的缺陷已修复，现分别返回 `0`、`0L`、`0F`、`0D`。
- **`ignoreConvertError` 异常拦截补齐**：`parse*`、`BigDecimal`、`BigInteger` 与 Boolean 解析分支统一改为 try-catch，异常时按 `ignoreConvertError` 返回 `null`，与"忽略转换错误"声明一致。
- **`TableNameParser` 空输入保护**：null SQL 抛 `TinyJdbcException`；空 tokens 在 `accept()` 中直接返回；`processFromToken` 增加 index 越界检查。
- **`TableParserUtils.clearCache()`**：新增公共 `clearCache()` 清理入口，与 `ReflectUtils` 对齐；两者均无自动淘汰（无 `ClassValue`/弱键），热部署时需手动调用。
- **`Locale.ROOT` 替换**：`TableInfo.getFieldByColumn`、`TableRowMapper.mapRow`、`TableParserUtils.buildColumnToField`、`TableNameParser` 内三处 `toLowerCase()` 改为 `toLowerCase(Locale.ROOT)`，修复土耳其等 locale 下 `I/İ` 映射错位。
- **重复列名检测**：`TableParserUtils` 构建列映射时检测 `columnToField` 已存在同列名映射，抛 `TinyJdbcException("Duplicate column '...'")`，替代原先的静默覆盖。
- **`ArrayUtils.mergeMultipleArrays` 引用跳过修复**：改为按索引跳过首次出现，同一实例重复出现时正常复制。
### 条件构造器增强（criteria）

- **`groupBy` / `having`（`待发布`）**：`QueryCriteria` / `LambdaQueryCriteria` 支持 `GROUP BY` 与 `HAVING`，拼接顺序为 `WHERE ... GROUP BY ... HAVING ... ORDER BY`；`having(String, Object...)` 参数化绑定，默认做尾部片段安全校验。
- **`selectExpr`（`待发布`）**：支持把受信任的聚合 / 函数表达式作为查询列（如 `count(*) as cnt`、`sum(amount)`、`date(create_time)`），配合 `groupBy` 实现聚合统计；走 `SqlIdentifierUtils.checkTailSql` 安全校验。
- **`exists` / `notExists`（`待发布`）**：支持 `EXISTS` / `NOT EXISTS` 子查询条件（如 `exists("select 1 from t_order o where o.user_id = t.id")`），子查询以小括号包裹，可带 `?` 参数。
- **`apply`（`待发布`）**：支持追加受信任的原始 SQL 条件片段（如 `apply("date(create_time) = ?", v)`、`apply("a = b")`），兜底字段间比较 / 函数条件。
- **`in` / `notIn` 可变参数（`待发布`）**：新增 `Object...` 可变参数重载，与既有 `Collection` 版同名共存（对齐 MyBatis-Plus）；注意传裸 `null` 会因重载歧义编译失败，空值请传 `Collections.emptyList()`（空集合仍走“运行时应抛 `TinyJdbcException`”校验）。
- **`setField`（`待发布`）**：`UpdateCriteria` / `LambdaUpdateCriteria` 支持字段对字段赋值（`setField("create_time", "update_time")` → `create_time = update_time`），取值以列引用写入 SQL、不入参数列表。

### 代码生成器增强

- **Java 类型映射精确化（`a50cc43`）**：`TypeUtils.getJavaType` 新增按列大小与小数位精确分派 —— `TINYINT`→`Byte`、`SMALLINT`→`Short`、`DECIMAL`/`NUMERIC` 按 `decimalDigits` 与 `columnSize` 映射为 `BigDecimal` / `Integer` / `Long`、`DATE`/`TIME`/`TIMESTAMP`/`TIMESTAMP_WITH_TIMEZONE` 分别映射为 `LocalDate` / `LocalTime` / `LocalDateTime` / `OffsetDateTime`；主键与普通列均传入列大小参与判定。
- **主键策略自动推断（`a50cc43`）**：`StrategyConfig` 的 `idType` 默认由 `INPUT` 改为 `null`，代码生成器根据主键列是否自增自动推断（自增 → `AUTO_INCREMENT`，否则 → `INPUT`），显式设置则固定使用该策略；相关注释一并补充。
- **实体类注释模板修复（`e51249e`）**：修复 `entity.ftl` 中 `@author` / `@date` 被 `tableComment?has_content` 包裹、表无注释时这两行随之消失的问题；现调整为 `@author` / `@date` 始终输出，表注释作为可选部分。
- **代码生成器单元测试（`5a3f92a` / `e51249e`）**：新增基于内存 `TableMeta` 的生成测试与实体注释模板回归用例 —— 覆盖默认 / 显式 / 自动推断 `IdType`，并分别校验表有无注释时 `@author` / `@date` 均正确输出。

### 依赖维护

- **Maven 插件版本更新（`28500d3`）**：更新构建插件版本，保持构建稳定性。

### 升级检查清单

- [ ] 如使用 `@Id(idType = IdType.NANO_ID)` / `IdType.ULID`，确认主键字段类型为 `String`（`NANO_ID` / `ULID` / `UUID` 均要求 `String`）。
- [ ] 如直接引用 `IdUtils` 或具体生成器实现（`ObjectIdGenerator` / `SequenceGenerator` 等）为公开 API，请核对包路径迁移至 `id.generator`。
- [ ] 回归验证自定义 `@Column` 实体类的查询与分页结果映射，确认映射行为符合预期。
- [ ] 如使用代码生成器：核对新生成的 `java.time` / `Byte` / `Short` 类型与主键策略（自增 → `AUTO_INCREMENT`，非自增 → `INPUT`）是否符合预期；类型映射有变化，旧项目可能需调整实体字段类型。
- [ ] 如用 criteria 的 `groupBy` / `having` 聚合，确认用 `selectExpr` 选中了聚合列（如 `count(*) as cnt`）；仅 `GROUP BY` 而 `SELECT` 含非分组列，在 `ONLY_FULL_GROUP_BY` / PostgreSQL 等数据库会报错。

---

## 2.0.1

> 发布日期：2026-08-27 ｜ 发布提交：`720b68f` ｜ 升级路径：2.0.0 → 2.0.1

### 类型概览

| 维度 | 内容 |
|------|------|
| 重大重构 | SQL 构造器重构，支持像写 SQL 一样组合复杂语句 |
| 破坏性变更 | `Expression` 移除，函数能力并入 `FuncBuilder` / `FuncExpr` |
| 稳定性优化 | 泛型解析支持 CGLIB 代理、反射工具精简 |
| 测试 | SQL 构造器测试集中维护，95 项全绿 |

### ⚠️ 破坏性变更（升级必读）

- **`Expression` 类移除（`d2f7f03`）**：旧 SQL 函数表达式类已删除，`count` / `sum` / `avg` / `max` / `min` / `coalesce` / `ifNull` / `caseWhen` 等能力合并到 `FuncBuilder` / `FuncExpr`。直接使用 `Expression.of(String)` 的代码需迁移为 `FuncExpr.of(String)`。

### SQL 构造器重构

- **组合式复杂语句（`d2f7f03`）**：`SQL` 构造器全面升级，支持像写 SQL 一样组合复杂语句；新增主表别名、多行插入、函数嵌套、`caseWhen` 三参数等能力，并补齐 Lambda 重载（`insert` / `join` / `onDuplicateKeyUpdate` / 自定义操作符）。
- **JOIN 增强（`d2f7f03`）**：`JOIN` 表名支持直接传入实体类；`ConditionGroup` 新增 `xxxIfAbsent` 判空跳过与块级开关。
- **UNION 修复（`d2f7f03`）**：修复 UNION 的 `ORDER BY` / `LIMIT` / 行锁应用到整体的问题。
- **文档重写（`d2f7f03`）**：重写 `SQL.md` 并补充用法速查。

### 泛型与反射优化

- **泛型类型解析支持 CGLIB 代理（`b3656a8`）**：移除过时的 `ParameterizedType` 反射方式，集成 Spring `GenericTypeResolver`；`@Transactional` / `@Async` 等切面代理场景下不再出现 `ClassCastException`。
- **反射工具精简（`eb37e99`）**：移除自定义 `ClassUtils`，改用 Spring 框架实现；`ReflectUtils.getFields` 增加防御性拷贝选项，避免缓存污染。

### 测试整理

- **SQL 构造器测试用例合并（`480ecff`）**：分散的验证类合并为 `SQLBuilderVerifyMain`，按能力模块分区维护，全量 95 项测试通过。

### 升级检查清单

- [ ] 全局搜索 `Expression`，迁移至 `FuncBuilder` / `FuncExpr`。
- [ ] 在 `@Transactional` / `@Async` 等切面代理场景回归验证泛型 DAO 的类型解析。
- [ ] 回归验证 `SQL` 构造器的既有用法（JOIN、UNION、子查询等）。

### 兼容性说明

| 用户侧能力 | 兼容性 |
|------------|--------|
| 基于 `BaseDao` 的常规 CRUD 调用 | ✅ 完全兼容 |
| `SQL` 构造器既有用法 | ✅ 兼容，并新增多项能力 |
| 泛型 DAO（含切面代理场景） | ✅ 修复 `ClassCastException` |
| `Expression.of(String)` | ❌ 已移除，改用 `FuncExpr.of(String)` |

---

## 2.0.0

> 发布日期：2026-08-11 ｜ 发布提交：待发布 ｜ 升级路径：1.9.9 → 2.0.0

### 类型概览

| 维度 | 内容 |
|------|------|
| 架构升级 | `GlobalConfig` 移除；Starter 绑定属性，core 持有不可变运行时上下文 |
| 破坏性变更 | 静态配置 API 删除、`close-conn` 删除、部分底层扩展 API 参数调整 |
| 事务一致性 | 数据库类型识别改用 Spring 事务连接管理 |

### ⚠️ 破坏性变更（升级必读）

- **移除 `GlobalConfig`**：不再提供 `GlobalConfig.setConfig()` / `GlobalConfig.getConfig()` 等静态 API。框架内部统一通过 `TinyJdbcRuntime` 获取运行时配置、ID 生成器和自动填充处理器，避免多 Spring 上下文、集成测试之间共享可变状态。
- **`TinyJdbcProperties` 保持在 Starter 模块**：继续使用 `org.tinycloud.jdbc.TinyJdbcProperties` 进行 Spring Boot 配置绑定；core 仅保留不依赖 Spring Boot 的 `TinyJdbcRuntime`，用户无需修改属性类 import。
- **移除 `tiny-jdbc.close-conn`**：数据库类型识别使用 `DataSourceUtils.getConnection()` / `releaseConnection()` 管理连接；不再允许通过配置关闭或绕过连接释放，以保证事务绑定连接不会被框架提前关闭。
- **底层扩展 API 调整**：直接调用 `SqlAssembler.buildInsertSql(...)`、直接创建 `JdbcTemplateHelper`，或自定义继承 `AbstractSqlSupport` 的项目，需传入 / 提供 `TinyJdbcRuntime`。

### 连接释放与连接池兼容性

- **统一使用 Spring 事务感知连接管理**：数据库类型识别从直接 `DataSource#getConnection()` 改为 `DataSourceUtils.getConnection()` / `releaseConnection()`。
- **事务场景不会提前关闭连接**：当前线程已绑定事务连接时，`releaseConnection()` 不会在识别数据库类型后将其归还，仍由 Spring 在事务完成时统一处理。
- **非事务场景会归还连接池**：对于 Druid、HikariCP、DBCP 等连接池，释放逻辑会关闭逻辑连接并归还池中，通常不会直接断开数据库物理连接。
- **为什么移除 `close-conn`**：设为 `false` 会使非事务连接长期处于借出状态并可能耗尽连接池；设为 `true` 后由框架直接关闭又无法正确区分事务绑定连接。2.0.0 固定采用 Spring 的事务感知释放策略。

| 场景 | 2.0.0 行为 |
|------|------------|
| `@Transactional` 方法内 | 保留线程绑定连接，事务结束时由 Spring 释放 |
| 非事务方法内 | 释放逻辑连接并归还连接池 |
| Druid 等连接池 | 归还连接池，不等同于立即关闭数据库物理连接 |

### 升级检查清单

- [ ] 全局搜索 `GlobalConfig`，删除静态配置初始化和访问代码。
- [ ] 如直接使用 `TinyJdbcProperties`，确认仍从 `org.tinycloud.jdbc.TinyJdbcProperties` 导入。
- [ ] 删除 `tiny-jdbc.close-conn` 配置项；不要以关闭开关替代 Spring 的 `DataSourceUtils` 连接释放。
- [ ] 如直接使用 `SqlAssembler.buildInsertSql(...)`，补充 `TinyJdbcRuntime` 参数。
- [ ] 如自定义继承 `AbstractSqlSupport`，实现 `getTinyJdbcRuntime()`。
- [ ] 如直接 `new JdbcTemplateHelper(...)`，在构造参数中传入 `TinyJdbcRuntime`。
- [ ] 在事务方法及多数据源场景回归验证分页与数据库类型自动识别。

### 兼容性说明

| 用户侧能力 | 兼容性 |
|------------|--------|
| `tiny-jdbc.*` 配置（不含 `close-conn`） | ✅ 完全兼容 |
| 继承 `BaseDao` 的常规 CRUD 调用 | ✅ 完全兼容，无需新增构造器 |
| `GlobalConfig` 静态 API | ❌ 已移除 |
| `org.tinycloud.jdbc.TinyJdbcProperties` | ✅ 包路径保持不变 |
| 自定义 `AbstractSqlSupport` | ⚠️ 需实现运行时上下文方法 |

---
## 1.9.9

> 发布日期：2026-08-05 ｜ 发布提交：`ac2753c` ｜ 升级路径：1.9.8 → 1.9.9

### 类型概览

| 维度 | 内容 |
|------|------|
| 新增特性 | 2 项（`Collection` 集合入参、SQL 统计结果日志） |
| 兼容性 | ✅ 现有 `List` 调用完全兼容 |
| 文档 | Maven 坐标和使用说明同步至 `1.9.9` |

### 关键变更

- **`IN` / `NOT IN` 参数放宽为 `Collection`（`1b56ee6`）**：调用方不再局限于 `List`，可直接传入 `Set` 等 `Collection` 实现；原有 `List` 写法无需调整。
- **SQL 拦截器执行链重构并支持结果统计日志（`47df74d`）**：SQL 统计能力可记录执行结果，便于排查数据访问行为。

### 升级检查清单

- [ ] 如项目自定义 SQL 拦截器，验证拦截顺序和日志输出是否符合预期。
- [ ] 如需使用集合去重语义，可将 `in` / `notIn` 参数由 `List` 替换为 `Set`；非必需。

### 兼容性说明

| 用户侧能力 | 兼容性 |
|------------|--------|
| `in` / `notIn` 传入 `List` | ✅ 完全兼容 |
| `in` / `notIn` 传入其他 `Collection` | ✅ 新增支持 |
| 自定义 SQL 拦截器 | ⚠️ 建议回归验证 |

---

## 1.9.8

> 发布日期：2026-06-08 ｜ 发布提交：`73c28e7` ｜ 升级路径：1.9.7 → 1.9.8

### 类型概览

| 维度 | 内容 |
|------|------|
| Bug 修复 | 4 项（代码生成、模板、`updateById`、多主键） |
| 稳定性优化 | 3 项（Class 缓存、集合空值保护、JDBC URL 解析） |
| 破坏性变更 | 无 |

### Bug 修复

- **代码生成器字段注释修复（`6dc4474`）**：字段注释改为读取列注释，避免所有字段误写为表注释。
- **代码生成模板修复（`0ff1b9d`）**：修正模板内容错误。
- **`updateById` 空更新字段保护（`2053759`）**：没有可更新字段时不再抛出底层字符串截取异常。
- **多 `@Id` 保护（`41be64d`）**：框架不支持复合主键时明确抛出异常，避免生成错误 SQL。

### 架构与健壮性优化

- **Class 缓存改用 `ClassValue`（`773faf6`）**：降低 Class 元数据缓存的生命周期管理风险。
- **`in` / `notIn` 空集合保护（`c4b6d47`）**：避免空集合或 `null` 进入 SQL 构建过程。
- **JDBC URL 解析重构（`6c8c935`）**：提升数据库类型识别的健壮性。

### 升级检查清单

- [ ] 实体类只能保留一个 `@Id`；复合主键场景需在业务侧自行处理。
- [ ] 执行代码生成后，检查生成字段注释是否符合数据库列注释。

---

## 1.9.7

> 发布日期：2026-05-09 ｜ 发布提交：`9399f8d` ｜ 升级路径：1.9.6 → 1.9.7

### 类型概览

| 维度 | 内容 |
|------|------|
| 新增模块 | 1 项（`tiny-jdbc-codegen`） |
| 新增能力 | 3 项（SQL 构造器升级、自动填充、批量填充） |
| 破坏性变更 | 1 项（移除部分歧义 `update` 重载） |

### 新增特性

- **新增代码生成器模块（`8cb228d`）**：提供实体、DAO 等代码生成基础能力。
- **代码生成器支持作者配置（`21e7a09`）**：生成文件可输出作者信息。
- **实体字段自动填充（`9b9b548` / `360c666`）**：引入 `MetaObjectHandler`，插入和批量插入可执行自动填充。
- **SQL 构造器升级（`e67423d`）**：完善构造器能力及配套使用文档。

### ⚠️ 破坏性变更（升级必读）

- **移除部分 `update` 重载（`f0733c6`）**：为减少“实体更新”和“条件更新”之间的歧义，删除了部分易误用的重载方法。

### 升级检查清单

- [ ] 全局搜索 `update(`，通过编译检查定位已删除重载，并迁移到保留的明确更新方法。
- [ ] 如启用自动填充，实现并配置 `MetaObjectHandler`，同时验证批量插入字段值。
- [ ] 如使用代码生成器，新增 `tiny-jdbc-codegen` 依赖并核对模板生成结果。

---

## 1.9.6

> 发布日期：2025-12-14 ｜ 发布提交：`e8d5672` ｜ 升级路径：1.9.5 → 1.9.6

### 类型概览

| 维度 | 内容 |
|------|------|
| Bug 修复 | 1 项（配置错误） |
| 破坏性变更 | 无 |

### 关键变更

- **配置修复（`e8d5672`）**：修正 `1.9.5` 发布后的配置错误。

---

## 1.9.5

> 发布日期：2025-12-14 ｜ 发布提交：`253e457` ｜ 升级路径：1.9.4 → 1.9.5

### 类型概览

| 维度 | 内容 |
|------|------|
| 新增特性 | 2 项（拦截器机制、统计拦截器） |
| 默认行为 | 统计拦截器默认关闭 |
| 破坏性变更 | 无 |

### 关键变更

- **拦截器机制（`4e9e036`）**：为 SQL 执行流程提供扩展入口。
- **统计拦截器（`a7e999c`）**：新增 SQL 统计能力，默认不启用，避免影响现有运行行为。

---

## 1.9.4

> 发布日期：2025-12-04 ｜ 发布提交：`295a691` ｜ 升级路径：1.9.3 → 1.9.4

### 类型概览

| 维度 | 内容 |
|------|------|
| 新增特性 | 2 项（`@Column.exist`、`batchInsert` 重载） |
| 稳定性优化 | 3 项（自动配置、Snowflake、构造器） |
| 破坏性变更 | 无 |

### 关键变更

- **`@Column.exist`（`fb1ba8e`）**：可标记实体字段不参与表字段映射。
- **批量插入重载（`ae57915`）**：扩展 `batchInsert` 调用方式，并补充使用说明。
- **Snowflake 初始化降级（`99bed2c`）**：初始化失败时增加降级处理；同时优化容器环境中的 `workerId` 获取（`e564868`）。
- **自动配置与构造器优化（`0da3058` / `72ce880`）**：改善启动和构造器使用体验。

---

## 1.9.3

> 发布日期：2025-11-06 ｜ 发布提交：`9f52db0` ｜ 升级路径：1.9.2 → 1.9.3

### 关键变更

- **JDK 8 缓存优化（`1c731c7`）**：新增 `ConcurrentHashMapUtils`，规避 JDK 8 `ConcurrentHashMap.computeIfAbsent` 的性能问题。
- **依赖管理调整（`daa7fb3`）**：更新 POM 依赖组织方式。

---

## 1.9.2

> 发布日期：2025-11-02 ｜ 发布提交：`e95f73e` ｜ 升级路径：1.9.1 → 1.9.2

### 关键变更

- **Lambda 元数据与缓存优化（`3fa5247` / `1cde0dd`）**：改善极端场景下的 Lambda 属性解析，并优化缓存写入。

---

## 1.9.1

> 发布日期：2025-10-30 ｜ 发布提交：`3f59f96` ｜ 升级路径：1.9.0 → 1.9.1

### 关键变更

- **SQL 值转换优化（`34fd474`）**：`SqlGenerator` 使用 `ConvertUtils` 处理数据转换。
- **JDBC URL 解析优化（`9d379df`）**：提升 URL 获取逻辑的健壮性。
- **移除 `TinyJdbcVersion`（`4781cf7`）**：内部版本方案调整；如业务代码直接依赖该类，需先完成编译验证。

---

## 1.9.0

> 发布日期：2025-10-20 ｜ 发布提交：`60f1938` ｜ 升级路径：1.8.9 → 1.9.0

### 关键变更

- **分页插件支持多数据源（`f110ff0` / `b606136` / `f06c63b`）**：根据运行时数据源处理分页方言。
- **`Parser` 泛型化（`c5efdf0`）**：改善创建实例时的类型检查体验。

---

## 1.8.9

> 发布日期：2025-08-10 ｜ 发布提交：`767e04d` ｜ 升级路径：1.8.8 → 1.8.9

### 关键变更

- **华为云 GaussDB 适配（`b93b384`）**：新增数据库方言支持。
- **默认构造方法（`a44159d`）**：改善部分对象创建场景。

---

## 1.8.8

> 发布日期：2025-07-18 ｜ 发布提交：`4e90570` ｜ 升级路径：1.8.7 → 1.8.8

### 类型概览

| 维度 | 内容 |
|------|------|
| 安全性优化 | 1 项（分页参数改用占位符） |
| 破坏性变更 | 无 |

### 关键变更

- **分页参数化重构（`29b975a`）**：分页 SQL 由字符串拼接改为占位符传参，降低 SQL 注入风险。

---

## 1.8.7

> 发布日期：2025-07-14 ｜ 发布提交：`c8584b3` ｜ 升级路径：1.8.6 → 1.8.7

### 类型概览

| 维度 | 内容 |
|------|------|
| 新增特性 | 1 项（`selectSingleColumn`） |
| Bug 修复 | 1 项（批量插入 SQL 为空） |
| 破坏性变更 | 1 项（`selectForObject` 更名） |

### ⚠️ 破坏性变更（升级必读）

| 旧 API | 新 API |
|---------|--------|
| `selectForObject(String, Class, Object...)` | `selectOneObject(String, Class, Object...)` |
| `selectForObject(SQL, Class)` | `selectOneObject(SQL, Class)` |

### 关键变更

- **新增单列查询（`c5a2912`）**：`BaseDao` 增加 `selectSingleColumn`。
- **方法名称标准化（`c5a2912`）**：`selectForObject` 统一更名为 `selectOneObject`，强调仅返回单条结果。
- **批量插入修复（`4eee6e5`）**：修复 `batchInsert` 生成 SQL 为空的问题。

### 升级检查清单

- [ ] 全局搜索 `selectForObject` 并替换为 `selectOneObject`。
- [ ] 为单行单列查询优先使用 `selectSingleColumn`。

---

## 1.8.6

> 发布日期：2025-07-07 ｜ 发布提交：`a796240` ｜ 升级路径：1.8.5 → 1.8.6

### 关键变更

- **条件化 `set`（`dec9f03`）**：更新构造器的 `set` 方法支持 `whether`，可按条件决定是否生成更新字段。

---

## 1.8.5

> 发布日期：2025-06-30 ｜ 发布提交：`c46e6b2` ｜ 升级路径：1.8.4 → 1.8.5

### 关键变更

- **排序与方言扩展（`5a4c427` / `4b0df9f`）**：新增 `orderByDesc` 并补充数据库方言。
- **Snowflake 配置类型调整（`684465d`）**：`DatacenterAndWorkerProvider` 属性调整为 `Long`。

---

## 1.8.4

> 发布日期：2025-06-17 ｜ 发布提交：`b878bd3` ｜ 升级路径：1.8.3 → 1.8.4

### 关键变更

- **条件化排序（`f537e6f`）**：`orderBy` 重载支持 `whether`。
- **空页性能优化（`7dc7ae4`）**：总记录数为 `0` 时不再执行分页查询。

---

## 1.8.3

> 发布日期：2025-06-11 ｜ 发布提交：`3cfb687` ｜ 升级路径：1.8.2 → 1.8.3

### 关键变更

- **新增 `selectAll`（`fa34a24`）**：提供全量实体查询快捷方法。
- **条件参数修复（`8755471`）**：修复 `UpdateCriteria` 参数顺序错误。
- **SQL 构造器整理（`3a356d0`）**：优化内容与文档；相关集合参数调整为 `List`（`0f565f2`）。

---

## 1.8.2

> 发布日期：2025-05-30 ｜ 发布提交：`11cb7ec` ｜ 升级路径：1.8.1 → 1.8.2

### 类型概览

| 维度 | 内容 |
|------|------|
| 新增特性 | SQL 构造器、缓存清理、`NamedParameterJdbcTemplate` 包装 |
| Bug 修复 | 首个 `and()` / `or()` 条件异常 |
| 破坏性变更 | `GlobalConfigUtils` 删除、`SqlGenerator` / `SqlProvider` 包路径调整 |

### 新增特性

- **SQL 构造器初版（`b7de9ca` / `548a049` / `5347dc0`）**：支持 `notIn`、`notLike`、`notBetweenAnd`、`groupBy`、`having` 等能力。
- **JDBC 包装与缓存控制（`321def2` / `dcce6f0`）**：新增 `NamedParameterJdbcTemplate` 包装和 `clearCache`。
- **`BaseDao` 接入 SQL 构造器（`a01a9ea`）**：可从 DAO 使用 SQL 构造器能力。

### ⚠️ 破坏性变更（升级必读）

| 变更 | 升级动作 |
|------|----------|
| 删除 `GlobalConfigUtils`（`505a502`） | 将静态调用迁移至 `GlobalConfig` 对应方法 |
| `SqlGenerator` 包路径迁移（`6bd1768`） | `org.tinycloud.jdbc.sql.SqlGenerator` → `org.tinycloud.jdbc.support.SqlAssembler` |
| `SqlProvider` 包路径迁移（`6bd1768`） | `org.tinycloud.jdbc.sql.SqlProvider` → `org.tinycloud.jdbc.support.SqlProvider` |

### 升级检查清单

- [ ] 全局搜索 `GlobalConfigUtils`，替换为 `GlobalConfig`。
- [ ] 更新 `SqlGenerator`、`SqlProvider` 的 import。
- [ ] 对包含首个 `and()` / `or()` 的查询条件执行回归测试。

---

## 1.8.1

> 发布日期：2024-12-18 ｜ 发布提交：`0988a71` ｜ 升级路径：1.8.0 → 1.8.1

### 关键变更

- **SQL 尾部拼接（`cf45736`）**：构造器新增 `last`。
- **Snowflake 配置优化（`83205e8`）**：新增 `DatacenterAndWorkerProvider`。

---

## 1.8.0

> 发布日期：2024-10-17 ｜ 发布提交：`e5a3155` ｜ 升级路径：1.7.9 → 1.8.0

### 关键变更

- **条件构造器 `whether`（`98be02d`）**：布尔值控制条件是否加入最终 SQL。

---

## 1.7.9

> 发布日期：2024-10-14 ｜ 发布提交：`6d6e3f7` ｜ 升级路径：1.7.8 → 1.7.9

### 关键变更

- **业务扩展工具（`7b2bc7b`）**：新增 `JdbcTemplateHelper`。
- **ID 泛型约束（`93f8cc2`）**：ID 类型必须实现 `Serializable`。

---

## 1.7.8

> 发布日期：2024-08-23 ｜ 发布提交：`b15fa34` ｜ 升级路径：1.7.7 → 1.7.8

### 关键变更

- **SEQUENCE 修复（`14e2c84`）**：修复主键策略判断错误。
- **更新构造器整理（`3f69e8f`）**：完善 `setIncrement`、`setDecrement`。

---

## 1.7.7

> 发布日期：2024-07-03 ｜ 发布提交：`07e28ee` ｜ 升级路径：1.7.6 → 1.7.7

### 关键变更

- **雪花配置重命名（`db66b8a`）**：`SequenceConfigInterface` 更名为 `SnowflakeConfigInterface`。
- **日志与雪花算法优化（`db66b8a` / `e4551b4`）**。

---

## 1.7.6

> 发布日期：2024-06-06 ｜ 发布提交：`6deffe0` ｜ 升级路径：1.7.5 → 1.7.6

### 关键变更

- **自定义雪花节点配置（`8511fd8` / `bc53c83`）**：支持配置 `workerId`、`datacenterId`。
- **分页与批量插入优化（`4860d73` / `a43a0b3`）**。

---

## 1.7.5

> 发布日期：2024-04-22 ｜ 发布提交：`6ae28e8` ｜ 升级路径：1.7.4 → 1.7.5

### 关键变更

- **P6Spy 监控（`dbe7242`）**：新增按需加载的 SQL 监控能力。
- **Release 展示（`b868734`）**：增加发布信息展示。

---

## 1.7.4

> 发布日期：2024-04-21 ｜ 发布提交：`3d55142` ｜ 升级路径：1.7.3 → 1.7.4

### 类型概览

| 维度 | 内容 |
|------|------|
| 新增特性 | 独立 `@Id` 注解、数据库序列主键 |
| 破坏性变更 | `@Column` 不再承担主键标记和策略配置 |

### ⚠️ 破坏性变更（升级必读）

| 旧写法 | 新写法 |
|--------|--------|
| `@Column(value = "id", primaryKey = true, idType = IdType.ASSIGN_ID)` | `@Id(idType = IdType.ASSIGN_ID)` 与 `@Column("id")` 分开声明 |

### 关键变更

- **主键注解拆分（`99855b4`）**：新增 `@Id`，从 `@Column` 移除 `primaryKey`、`idType` 属性。
- **序列主键支持（`01e55e0`）**：`@Id` 可配置数据库序列 SQL。

### 升级检查清单

- [ ] 全局搜索 `primaryKey =` 与 `idType =`，从 `@Column` 迁移至 `@Id`。
- [ ] 使用序列主键时，确认 `@Id.value` 中的序列 SQL 与目标数据库兼容。

---

## 1.7.3

> 发布日期：2024-04-17 ｜ 发布提交：`06d7bf9` ｜ 升级路径：1.7.2 → 1.7.3

### 关键变更

- **分页扩展（`6814300` / `0dd000f` / `3628a64`）**：新增 `paginateMap` 并补充数据库分页适配。
- **更新表达式（`ab31d3d`）**：`LambdaUpdateCriteria`、`UpdateCriteria` 支持字段自增、自减。

---

## 1.7.2

> 发布日期：2024-04-16 ｜ 发布提交：`be27986` ｜ 升级路径：1.7.0 → 1.7.2

### 关键变更

- **多模块重构（`f91eca0`）**：项目拆分为 Boot Starter 与 Core 等模块。
- **配置与启动能力（`889dcf3`）**：支持 `DbType` 显式配置或从 JDBC URL 自动解析，支持 Banner 和动态版本号。
- **工具与安全（`da8b45f` / `13f39f7`）**：新增 `truncate` 并补充安全说明。

---

## 1.7.0

> 发布日期：2024-04-15 ｜ 发布提交：`0ac7e55` ｜ 升级路径：1.6.3 → 1.7.0

### 关键变更

- **Criteria 类型校验（`78c6096`）**：强化编译期错误发现能力。
- **快捷创建（`78c6096`）**：新增 `Criterias` 工具类。

---

## 1.6.3

> 发布日期：2024-04-14 ｜ 发布提交：`e34e012` ｜ 升级路径：1.6.2 → 1.6.3

### 关键变更

- **SQL 构造器重构（`f75ff9c`）**：增加缓存，减少反射调用。
- **主键约束（`8e412a4`）**：多主键场景仅取第一个注解，不支持复合主键。

---

## 1.6.2

> 发布日期：2024-04-11 ｜ 发布提交：`a67c22d` ｜ 升级路径：1.6.1 → 1.6.2

### 关键变更

- **插入主键回填重载（`800a835`）**：`insertReturnAutoIncrement` 增加调用方式。
- **逻辑组合优化（`1109d10`）**：改善 `and`、`or` 使用体验。

---

## 1.6.1

> 发布日期：2024-04-07 ｜ 发布提交：`955c64b` ｜ 升级路径：1.6.0 → 1.6.1

### 关键变更

- **工具扩展（`c44fc13` / `f57b026` / `68b02fe`）**：新增表名解析和便捷创建、输出方法。
- **JdbcTemplate 统一（`4e8fef6`）**：统一使用 `JdbcTemplate`；后续提交清理了未成熟的插件实现（`a797888`）。

---

## 1.6.0

> 发布日期：2024-04-05 ｜ 发布提交：`a39a83e` ｜ 升级路径：1.5.3 → 1.6.0

### 类型概览

| 维度 | 内容 |
|------|------|
| 重大重构 | 条件构造器拆分为查询、更新两个方向 |
| 新增特性 | 更新构造器 `set` |
| 升级风险 | 直接依赖旧 `LambdaCriteria` 类型的代码需编译验证 |

### 关键变更

- **构造器职责拆分（`82c310b`）**：新增 `QueryCriteria`、`LambdaQueryCriteria`、`UpdateCriteria`、`LambdaUpdateCriteria`。
- **自定义更新字段（`82c310b`）**：更新构造器新增 `set`，可构造更灵活的更新语句。
- **反射性能优化（`362d19a` / `79f76bc`）**：优化方法引用属性解析和 `SqlGenerator`。

### 升级检查清单

- [ ] 查询场景使用 `QueryCriteria` / `LambdaQueryCriteria`，更新场景使用 `UpdateCriteria` / `LambdaUpdateCriteria`。
- [ ] 如直接引用 `LambdaCriteria`，执行编译并按新类型层级调整。

---

## 1.5.3

> 发布日期：2024-04-01 ｜ 发布提交：`ba2d8a2` ｜ 升级路径：1.5.2 → 1.5.3

### ⚠️ 破坏性变更（升级必读）

- **异常类更名（`95ac5a2`）**：`JdbcException` 重命名为 `TinyJdbcException`。请更新 import、捕获逻辑和自定义异常处理代码。

### 关键变更

- **反射缓存优化（`041c6bf`）**：提高 `getFields`、`getMethods` 缓存性能，避免父子类同名字段重复。

---

## 1.5.2

> 发布日期：2024-04-01 ｜ 发布提交：`22bfa15` ｜ 升级路径：1.5.1 → 1.5.2

### 类型概览

| 维度 | 内容 |
|------|------|
| API 扩展 | 批量 ID 查询与删除支持数组 |
| 行为变化 | `selectOne` 多结果时抛出异常 |
| 数据类型变化 | `Page` 字段升级为 `Long` |

### 关键变更

- **数组入参（`5411b42`）**：`deleteByIds`、`selectByIds` 支持数组。
- **结果唯一性约束（`5c8074e`）**：`selectOne` 查询到多条记录时抛出异常，不再静默返回。
- **分页类型升级（`372f356`）**：`Page` 全部字段调整为 `Long`。

### 升级检查清单

- [ ] 确保调用 `selectOne` 的 SQL 具备唯一性；多结果场景改用列表查询。
- [ ] 如直接使用 `Page` 字段或构造参数，检查 `Integer` 到 `Long` 的类型适配。

---

## 1.5.1

> 发布日期：2024-03-29 ｜ 发布提交：`3c3305b` ｜ 升级路径：1.5.0 → 1.5.1

### 关键变更

- **查询字段选择（`b263387` / `94876ea`）**：构造器支持 `select` 指定查询字段。
- **映射行为调整（`da17ce0`）**：查询 SQL 不再生成 `AS` 别名，由 `BeanPropertyRowMapper` 进行驼峰映射；实体属性需遵循驼峰命名。

---

## 1.5.0

> 发布日期：2024-03-05 ｜ 发布提交：`970489b` ｜ 升级路径：1.4.9 → 1.5.0

### 关键变更

- **自定义 ID 生成器（`970489b`）**：支持在内置策略之外扩展主键生成方式。

---

## 1.4.9

> 发布日期：2023-11-22 ｜ 发布提交：`4ed2c96` ｜ 升级路径：1.4.8 → 1.4.9

### 关键变更

- **Spring Boot 3 适配（`4ed2c96`）**：提升 Spring Boot 基线版本并兼容 Spring Boot 3。

---

## 1.4.8

> 发布日期：2023-11-20 ｜ 发布提交：`0c00657` ｜ 升级路径：1.4.7 → 1.4.8

### 关键变更

- **Maven 中央仓库发布（`2e8224b`）**：组件发布方式和 GroupId 调整。
- **Criteria 参数化（`8e4637c`）**：条件构造器统一使用占位符，降低 SQL 注入风险。

---

## 1.4.7

> 发布日期：2023-11-12 ｜ 发布提交：`a8a78ed` ｜ 升级路径：1.4.6 → 1.4.7

### 关键变更

- **主键策略枚举化（`a8a78ed`）**：`@Column` 主键策略配置改用 `IdType`，减少配置冲突。

---

## 1.4.6

> 发布日期：2023-10-23 ｜ 发布提交：`2b048a5` ｜ 升级路径：1.4.5 → 1.4.6

### 关键变更

- **分页方言扩展（`ad024b0`）**：新增多种数据库分页方言。
- **分页修复（`89eddc7`）**：修复已知分页问题。

---

## 1.4.5

> 发布日期：2023-10-17 ｜ 发布提交：`ce349b6` ｜ 升级路径：1.4.4 → 1.4.5

### 关键变更

- **`Page` 直传分页（`7c34efb`）**：`paginate` 支持直接传入 `Page` 对象。

---

## 1.4.4

> 发布日期：2023-09-03 ｜ 发布提交：`f22dcc6` ｜ 升级路径：1.4.3 → 1.4.4

### 关键变更

- **字符串判断修复（`d9a35d4`）**：修复字符串相等判断问题，并简化空值判断（`4dd426b`）。

---

## 1.4.3

> 发布日期：2023-08-27 ｜ 发布提交：`c84924b` ｜ 升级路径：1.4.2 → 1.4.3

### 安全性变更

- **全表操作保护（`ebd771c`）**：`update`、`delete` 增加更严格校验，禁止无条件全表更新和删除。

---

## 1.4.2

> 发布日期：2023-08-16 ｜ 发布提交：`171948d` ｜ 升级路径：1.4.1 → 1.4.2

### 关键变更

- **主键策略扩展（`9753f68`）**：新增 MongoDB `ObjectId`。
- **雪花算法升级（`af9598f`）**：切换到新的 `Sequence` 实现。

---

## 1.4.1

> 发布日期：2023-08-10 ｜ 发布提交：`e319e58` ｜ 升级路径：1.4.0 → 1.4.1

### 关键变更

- **批量主键查询（`e319e58`）**：新增 `selectByIds`。

---

## 1.4.0

> 发布日期：2023-08-10 ｜ 发布提交：`a234783` ｜ 升级路径：1.3.0 → 1.4.0

### 关键变更

- **计数与存在性查询（`9181731`）**：新增 `selectCount`、`exists`。
- **参数替换修复（`1e98bf0`）**：由 `replace` 改为 `replaceFirst`，避免极端场景误替换。

---

## 1.3.0

> 发布日期：2023-08-05 ｜ 发布提交：`c022453` ｜ 升级路径：1.2.0 → 1.3.0

### 关键变更

- **Criteria 基类抽取（`69ac989`）**：抽取 `AbstractCriteria` 公共逻辑并增强条件构造器。

---

## 1.2.0

> 发布日期：2023-08-04 ｜ 发布提交：`5c42672` ｜ 升级路径：1.1.0 → 1.2.0

### 关键变更

- **条件扩展（`9b57d99` / `ffa9b44`）**：新增 `notLike`、`notBetweenAnd`。
- **分页方言（`df9f2be` / `0d978cd` / `5df148d`）**：新增 PostgreSQL、SQLite、H2。

---

## 1.1.0

> 发布日期：2023-08-02 ｜ 发布提交：`44a593c` ｜ 升级路径：1.0.0 → 1.1.0

### 关键变更

- **泛型化 DAO（`a1ac128`）**：大部分接口无需显式传递实体 `Class`。
- **条件构造器（`a112543` / `e4f6ae7`）**：新增并接入条件构造器，支持按条件删除（`44a593c`）。
- **工具与批量操作（`eeb2746` / `687df48`）**：新增 `replaceSqlParams`、`deleteByIds`。

---

## 1.0.0

> 发布日期：2023-07-30 ｜ 发布提交：`6c4c5fe` ｜ 升级路径：首次发布

### 类型概览

| 维度 | 内容 |
|------|------|
| 基础能力 | 实体 CRUD、分页、反射工具、主键策略 |
| API 调整 | `update` 更名为 `updateById` |

### 关键变更

- **实体 CRUD（`999d7b3`）**：提供基于实体的基础增删改查。
- **分页与主键（`ebbe56b` / `af0bd6e` / `aaf0418`）**：新增分页查询、ID 工具和多种主键策略。
- **方法命名（`5883155`）**：`update` 调整为 `updateById`，明确按主键更新语义。

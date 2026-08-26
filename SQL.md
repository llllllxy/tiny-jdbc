# SQL 构建器使用指南

`SQL` 用于以链式方式构建参数化的 `SELECT`、`INSERT`、`UPDATE` 和 `DELETE` 语句。构建完成后可调用 `toSql()` 获取 SQL，调用 `getParameters()` 获取与 `?` 占位符顺序一致的参数；也可以直接交给 `BaseDao` / `JdbcTemplateHelper` / `AbstractSqlSupport` 执行。

> 值参数会使用 `?` 绑定；表名、列名以及 `FuncBuilder.col(...)` / `FuncExpr.of(...)` 中的字符串属于 SQL 结构，必须来自受信任的代码，不能直接使用前端输入。

> **关于 SQL 函数表达式**：推荐使用 `FuncBuilder` / `FuncExpr`（见 [4.2 函数表达式](#42-函数表达式funcbuilder)）。旧的 `Expression` 类**已移除**，其能力已合并到 `FuncBuilder`（`count/sum/avg/max/min/coalesce/ifNull/caseWhen` 等），新代码请直接使用 `FuncBuilder` / `FuncExpr`。

> **本指南导航**（按需跳读）：
> - **第一次用**：先读 [1. 快速开始](#1-快速开始) + 上面三个核心约定，再扫一遍 [2. 创建构建器与选择操作](#2-创建构建器与选择操作)。
> - **想快速组条件**：直接看 [3. WHERE 与 HAVING 条件](#3-where-与-having-条件) 的条件速查表。
> - **玩 SQL 函数 / 表达式**：[4.2 函数表达式](#42-函数表达式funcbuilder) + 文末[附录：FuncBuilder 可用函数](#附录funcbuilder-可用函数)。
> - **要 JOIN / 子查询 / UNION**：[5. JOIN](#5-join)、[6. 子查询](#6-子查询)、[7. UNION / UNION ALL](#7-union--union-all)。
> - **想一次看明白全链路**：跳到 [12.3 端到端示例](#123-端到端示例)。
> - **进阶 / 复杂场景**：[9. 复杂 UPDATE](#9-复杂-update)、[10. 执行与参数顺序](#10-执行与参数顺序)、[11. 使用约束与建议](#11-使用约束与建议)。

---

## 1. 快速开始

```java
SQL<User> query = SQL.table(User.class)
        .select(User::getId, User::getName)
        .where(w -> w.eq(User::getStatus, "ACTIVE")
                .ge(User::getAge, 18))
        .orderBy(User::getId).desc()
        .limit(20);

String sql = query.toSql();
List<Object> parameters = query.getParameters();

// SQL: SELECT id, name FROM t_user WHERE status = ? AND age >= ? ORDER BY id DESC LIMIT 20
// parameters: [ACTIVE, 18]

List<User> users = userDao.select(query);
```

> **这套 API 的三个核心约定**（先理解它，后面就顺了）：
>
> 1. **值是参数、结构是原样**。数据 / 数字 / 字符串常量 → 交给 `?` 预编译绑定（值参数）；表名、列名、函数名、裸 SQL 片段 → **原样拼进 SQL，必须来自受信任代码**。这条贯穿所有方法。
> 2. **条件写进回调**。`where(...)` / `having(...)` 接收一个 `Consumer<ConditionGroup>`，在 `w -> w.eq(...).ge(...)` 里组织条件，而不是把 `=` / `>` 直接挂在链上。条件多、要 `OR` 分组、要嵌套时更清晰。
> 3. **列和值要分清**。列名 / 表达式用裸 `String`（如 `"age"`）或 `FuncBuilder.col(...)`；字符串常量用 `lit(...)`，数字直接传值（如 `w.eq("age", 18)`、`lit(", ")`）。"写进 SQL 的列"和"拼成参数的值"从类型上就分开，不再手写引号。

#### 参数类型速查表（一个方法参数"该传什么"）

| 参数位置 | 你可以传 | 含义 / 渲染 |
| --- | --- | --- |
| 列名 | 裸 `String`（如 `"name"`） | 原样拼为列名 |
| 列名 | `TypeFunction`（如 `User::getName`） | 解析为列名（兼容 `@Column` 改名） |
| 列 / 表达式 | `FuncBuilder.col(...)` | 包成 `FuncExpr`，当作"列" |
| 字符串常量 | `lit("...")` | 参数化 `?` |
| 数字 / 布尔 | 直接传 `18` / `true` | 参数化 `?` |
| 裸 SQL 片段 | `FuncBuilder.raw(...)` / `FuncExpr.of(...)` | 原样拼（可含函数/操作符） |
| 子查询 | `SQL<?>` | 生成 `(SELECT ...)`，参数按序并入 |

---

## 2. 创建构建器与选择操作

### 2.1 指定表

```java
SQL<?> byTableName = SQL.table("t_user");
SQL<User> byEntity = SQL.table(User.class); // User 必须标注 @Table
```

表可携带别名，生成 `FROM table alias` / `UPDATE table alias` / `DELETE FROM table alias`。`from("tb_user", "u")` 这类别名主表在 tiny-jdbc 里用 `SQL.table(table, alias)` 表达：

```java
SQL<?> withAlias = SQL.table("t_user", "u").select("u.id");          // FROM t_user u
SQL<User> entityAlias = SQL.table(User.class, "u");                  // FROM t_user u
SQL<?> upd = SQL.table("t_user", "u").update().set("u.email", "a");  // UPDATE t_user u SET ...
SQL<?> del = SQL.table("t_user", "u").delete().where(w -> w.eq("u.id", 1)); // DELETE FROM t_user u WHERE ...
```

### 2.2 SELECT

未指定查询列时会生成 `SELECT *`；也可以显式调用 `select()` 表示查询全部列。`select` 支持字符串列名、实体方法引用（Lambda）、`FuncExpr`，以及 `selectDistinct` 去重。

```java
SQL.table("t_user").select();
SQL.table("t_user").select("id", "name", "created_at");

SQL<User> query = SQL.table(User.class)
        .select(User::getId, User::getName);

// 使用 SQL 函数表达式作为查询字段（见 4.2）
SQL<?> agg = SQL.table("t_user")
        .select(FuncBuilder.count("id").as("total"),
                FuncBuilder.max("age").as("maxAge"));

// 去重
SQL<?> distinct = SQL.table("t_user").selectDistinct("age");
// SELECT DISTINCT age FROM t_user
```

> `select` 一次调用内会固定操作类型；若要混合"普通列 + 函数"，请把普通列用 `FuncBuilder.col(...)` 包成 `FuncExpr` 后一并传给 `select(FuncExpr...)`，例如 `select(FuncBuilder.col("age"), FuncBuilder.max("age").as("maxAge"))`。

### 2.3 INSERT

`insert(...)` / `insertInto(...)` 中的列与 `values(...)` 中的值必须一一对应，数量不一致会抛出异常。

```java
SQL<?> insert = SQL.table("t_user")
        .insert("name", "age", "status")
        .values("张三", 18, "ACTIVE");

// INSERT INTO t_user (name, age, status) VALUES (?, ?, ?)
// [张三, 18, ACTIVE]
```

`values(...)` 可多次调用，实现**多行插入**（每次调用对应一行，值数量须与列一致，参数按行序追加）：

```java
SQL<?> batch = SQL.table("t_user")
        .insert("name", "age")
        .values("张三", 18)
        .values("李四", 22);

// INSERT INTO t_user (name, age) VALUES (?, ?), (?, ?)
// [张三, 18, 李四, 22]
```

`insertIgnoreInto(...)` / `replaceInto(...)` 同样支持多行。

列也可以用**实体方法引用（Lambda）** 指定，三个插入方法（`insertInto` / `insertIgnoreInto` / `replaceInto`，以及 `insert` 本身）都有对应的 `TypeFunction` 重载：

```java
SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class)
        .insertInto(VerifyChildEntity::getChildName, VerifyChildEntity::isActive)
        .values("张三", true);
// INSERT INTO t_verify_child (child_name_col, active_flag) VALUES (?, ?)

SQL<VerifyChildEntity> ignore = SQL.table(VerifyChildEntity.class)
        .insertIgnoreInto(VerifyChildEntity::getChildName)
        .values("李四");
// INSERT IGNORE INTO t_verify_child (child_name_col) VALUES (?)
```

插入还支持 `INSERT IGNORE`、`REPLACE`，以及 MySQL 专有的 `ON DUPLICATE KEY UPDATE`（见 [2.3.1](#231-插入变体与-insert-ignore--replace--on-duplicate-key-update)）。

### 2.4 UPDATE 与 DELETE

为避免误更新或误删除，`UPDATE` 和 `DELETE` 都必须包含 `where(...)` 条件。

```java
SQL<?> update = SQL.table("t_user")
        .update()
        .set("status", "INACTIVE")
        .set("updated_by", "system")
        .where(w -> w.eq("id", 1L));

SQL<?> delete = SQL.table("t_user")
        .delete()
        .where(w -> w.eq("id", 1L));
```

`UPDATE` 还支持连接更新、字段引用赋值与子查询赋值（见 [9 复杂 UPDATE](#9-复杂-update)）。

#### 2.3.1 插入变体与 INSERT IGNORE / REPLACE + ON DUPLICATE KEY UPDATE

```java
// INSERT IGNORE（存在唯一键冲突时忽略）
SQL<?> ignore = SQL.table("t_user")
        .insertIgnoreInto("id", "name")
        .values(5, "x")
        .onDuplicateKeyUpdate("name", "y");
// INSERT IGNORE INTO t_user (id, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = ?
// [5, x, y]

// REPLACE（存在唯一键冲突时先删后插）
SQL<?> replace = SQL.table("t_user")
        .replaceInto("id", "name")
        .values(5, "x")
        .onDuplicateKeyUpdateValues("name");
// REPLACE INTO t_user (id, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name)
// [5, x]
```

- `onDuplicateKeyUpdate("col", value)`：右侧为值，`?` 参数化。
- `onDuplicateKeyUpdateValues("col", ...)`：右侧为 `VALUES(col)`（引用本次待插入的值），无参数。
- 这两个方法也都支持**实体方法引用（Lambda）**：`onDuplicateKeyUpdate(VerifyChildEntity::getChildName, value)`、`onDuplicateKeyUpdateValues(VerifyChildEntity::getChildName)`。

---

## 3. WHERE 与 HAVING 条件

`where(...)` 和 `having(...)` 的回调参数都是 `ConditionGroup`。未显式指定时，多个条件默认以 `AND` 连接。

> **为什么用内嵌式回调，而不是把 `=` / `>` 直接挂在链上？** 因为条件往往是一组逻辑：多个条件、`OR` 分组、括号嵌套、Lambda 列引用、子查询、列到列比较。把它们收敛进 `w -> ...` 这个**封闭作用域**里，一眼可见这组条件都属于当前查询，嵌套与分组也更直观；而长链式在条件多时容易和 `from` / `groupBy` / `orderBy` 混在一起。这正是 `where(Consumer<ConditionGroup>)` 风格的价值。

| 方法 | 生成的 SQL | 示例 |
| --- | --- | --- |
| `eq` / `notEq` | `= ?` / `<> ?` | `w.eq("status", "ACTIVE")` |
| `gt` / `ge` / `lt` / `le` | 比较条件 | `w.ge("age", 18)` |
| `like` / `notLike` | `LIKE '%值%'` | `w.like("name", "张")` |
| `leftLike` / `notLeftLike` | `LIKE '%值'` | `w.leftLike("code", "A")` |
| `rightLike` / `notRightLike` | `LIKE '值%'` | `w.rightLike("code", "A")` |
| `in` / `notIn` | `IN (?, ...)` / `NOT IN (?, ...)` | `w.in("id", ids)` |
| `betweenAnd` / `notBetweenAnd` | `BETWEEN ? AND ?` | `w.betweenAnd("age", 18, 60)` |
| `isNull` / `isNotNull` | `IS NULL` / `IS NOT NULL` | `w.isNull("deleted_at")` |
| `in` / `notIn`（子查询） | `IN (SELECT ...)` | `w.in("id", subSql)` |
| `exists` / `notExists` | `EXISTS (SELECT ...)` | `w.exists(subSql)` |
| `and("col", opt, value)` | 自定义操作符 | `w.and("a.id", "=", 1)` / `w.and(User::getAge, ">", 18)` |
| `or()` | 下一个条件改为 `OR` 连接 | `w.eq("a",1).or().eq("b",2)` |
| `and(c)` / `or(c)` / `group(c)` | 括号分组 | `w.group(g -> g.eq("a",1).or().eq("b",2))` |

`in/notIn` 接收任意非空 `Collection`，包括 `List`、`Set` 和队列。若需稳定的参数输出顺序，使用 `List` 或 `LinkedHashSet`。

```java
Collection<Long> ids = new LinkedHashSet<>(Arrays.asList(1L, 2L, 3L));

SQL<?> query = SQL.table("t_user")
        .select("id", "name")
        .where(w -> w.in("id", ids)
                .notIn("status", Collections.singleton("DELETED"))
                .betweenAnd("age", 18, 60));
```

#### 条件判断：`xxxIfAbsent`（值为空自动跳过）

适用于搜索表单等"值可能为空"的场景。当值为 `null` / 空字符串 / 空集合 / 空 `Map` 时，该条件不生成。

```java
// keyword 为空时自动跳过 LIKE，只保留 status 条件
SQL<?> query = SQL.table("t_user")
        .select("id", "name")
        .where(w -> w.eq("status", 1)
                .likeIfAbsent("name", keyword));
// keyword=null  → WHERE status = ?
// keyword="张"  → WHERE status = ? AND name LIKE ?
```

`eqIfAbsent / notEqIfAbsent / gtIfAbsent / geIfAbsent / ltIfAbsent / leIfAbsent`、`likeIfAbsent / notLikeIfAbsent / leftLikeIfAbsent / notLeftLikeIfAbsent / rightLikeIfAbsent / notRightLikeIfAbsent`、`inIfAbsent / notInIfAbsent`、`betweenAndIfAbsent / notBetweenAndIfAbsent`、`andIfAbsent(col, opt, value)` 都可用（**String + Lambda** 两套）。每个还提供带 `Predicate` 的重载，可自定义判断：

```java
// 仅当 keyword 含字母时才加 LIKE
.where(w -> w.likeIfAbsent("name", keyword, v -> v != null && ((String) v).matches(".*[A-Za-z].*")));
```

整组条件也可按开关启用/禁用：`and(boolean, sub)` / `or(boolean, sub)`（及 `BooleanSupplier` 版本），`false` 时整组跳过：

```java
// flag=true 时才加 status 条件
.where(w -> w.and(flag, g -> g.eq("status", 1)).or(flag2, g -> g.gt("age", 18)));
```

---

### 3.1 列到列比较（FieldReference）

把条件/赋值右侧当作**字段引用**（而非值）时，使用 `FieldReference`。它不会参数化，会原样输出列名。

```java
SQL<?> query = SQL.table("t_score")
        .select("id")
        .where(w -> w.eq("student_id", new FieldReference("student_code"))
                .gt("avg_score", new FieldReference("max_score")));
// WHERE student_id = student_code AND avg_score > max_score

SQL<?> update = SQL.table("t_score")
        .update()
        .set("max_score", new FieldReference("score"))
        .where(w -> w.eq("student_id", 7));
// UPDATE t_score SET max_score = score WHERE student_id = ?
```

### 3.2 OR 与分组

`or()` 只影响下一个条件；`and(consumer)`、`or(consumer)` 与 `group(consumer)` 用于添加括号分组。

```java
SQL<?> query = SQL.table("t_user")
        .select()
        .where(w -> w.group(g -> g.eq("status", "ACTIVE")
                                .or().eq("status", "PENDING"))
                .and(g -> g.ge("age", 18)
                           .lt("age", 60))
                .or(g -> g.eq("role", "ADMIN")));

// SELECT * FROM t_user
// WHERE (status = ? OR status = ?) AND (age >= ? AND age < ?) OR (role = ?)
```

### 3.3 Lambda 条件

字符串列名与实体方法引用可以混用；方法引用会按实体字段映射转换为列名。

```java
SQL<User> query = SQL.table(User.class)
        .select(User::getId, User::getName)
        .where(w -> w.eq(User::getStatus, "ACTIVE")
                .in(User::getId, Arrays.asList(1L, 2L)));
```

### 3.4 条件子查询

子查询条件用于 `IN / NOT IN / EXISTS / NOT EXISTS`。`SQL<?> sub` 是另一个构建器，其参数会按顺序并入外层。

```java
SQL<?> in = SQL.table("tb_user")
        .select()
        .where(w -> w.in("id",
                SQL.table("tb_order").select("user_id").where(c2 -> c2.eq("status", "PAID"))));
// SELECT * FROM tb_user WHERE id IN (SELECT user_id FROM tb_order WHERE status = ?)
// [PAID]

SQL<?> exists = SQL.table("tb_user")
        .select()
        .where(w -> w.eq("is_active", 1)
                .exists(SQL.table("tb_role").select("id").where(c2 -> c2.eq("status", "ENABLED"))));
// SELECT * FROM tb_user WHERE is_active = ? AND EXISTS (SELECT id FROM tb_role WHERE status = ?)
// [1, ENABLED]

SQL<?> notIn = SQL.table("tb_user")
        .delete()
        .where(w -> w.notIn("id",
                SQL.table("tb_order").select("user_id").where(c2 -> c2.eq("status", "PAID"))));
// DELETE FROM tb_user WHERE id NOT IN (SELECT user_id FROM tb_order WHERE status = ?)
```

---

## 4. 分组、表达式、排序与分页

### 4.1 GROUP BY 与 HAVING

```java
SQL<?> report = SQL.table("t_order")
        .select(FuncBuilder.col("user_id"),
                FuncBuilder.count("*").as("total"),
                FuncBuilder.sum("amount").as("total_amount"))
        .where(w -> w.eq("status", "PAID"))
        .groupBy("user_id")
        .having(w -> w.gt("total_amount", 1000));

// SELECT user_id, COUNT(*) AS total, SUM(amount) AS total_amount
// FROM t_order WHERE status = ? GROUP BY user_id HAVING total_amount > ?
```

`group-by` 也支持实体方法引用：`groupBy(User::getAge)`、`orderBy(User::getId)`。

### 4.2 函数表达式（FuncBuilder）

`FuncBuilder` 用于构建 SQL 函数表达式，返回不可变的 `FuncExpr`，支持嵌套组合与链式别名 `.as(alias)`。

**参数约定**（与 `FuncBuilder` 各工厂方法一致）：

- `String` → **裸 SQL**（列名 / 表达式），原样拼接，**不加引号**；
- `TypeFunction`（如 `User::getAge`）→ 解析为对应列名；
- `FuncExpr` → 已组合的表达式（`col()` / `lit()` / 其它函数的结果）；
- `lit(Object)` / 值参数 → **值字面量**，参数化（`?` + 绑定参数）。

```java
import static org.tinycloud.jdbc.sql.FuncBuilder.*;

// 聚合 + 别名：SELECT COUNT(id) AS total, MAX(age) AS maxAge FROM t_user
SQL.table("t_user")
        .select(count("id").as("total"), max("age").as("maxAge"));

// Lambda 列引用 + lit() 字面量（混用时用 col() 包成 FuncExpr）：CONCAT(real_name, ?, name) AS display
SQL.table("t_user")
        .select(concat(col(User::getRealName), lit(", "), col(User::getName)).as("display"));
// SELECT CONCAT(real_name, ?, name) AS display FROM t_user
// [, ]

// String 为裸列名，值字面量参数化：IFNULL(remark, ?)
SQL.table("t_order")
        .select(ifNull("remark", "no remark").as("remark"));
// SELECT IFNULL(remark, ?) AS remark FROM t_order
// [no remark]

// 日期函数
SQL.table("t_user")
        .select(dateFormat("create_time", "%Y-%m-%d").as("day"),
                year("create_time").as("current_year"));
// SELECT DATE_FORMAT(create_time, ?) AS day, YEAR(create_time) AS current_year FROM t_user
// [%Y-%m-%d]
```

> `String` 的列名 / 表达式参数按设计原样拼接，其内容由调用方负责；不要把外部输入直接当列名传进去。值字面量请用 `lit(...)` 参数化。

#### 函数嵌套

大多数函数（日期 / 字符 / 数值）都提供 `FuncExpr` 重载，可自由嵌套，例如 `YEAR(NOW())`、`TRIM(LOWER(name))`：

```java
// YEAR(NOW())
SQL.table("t").select(FuncBuilder.year(FuncBuilder.now()).as("y"));

// TRIM(LOWER(name))
SQL.table("t").select(FuncBuilder.trim(FuncBuilder.lower(FuncBuilder.col("name"))).as("n"));
```

当某个函数没有便捷方法时，可用通用入口 `FuncBuilder.func(name, args...)` 组合任意函数：

```java
// COALESCE(a, ?)  → 参数 [x]
SQL.table("t").select(FuncBuilder.func("COALESCE", FuncBuilder.col("a"), FuncBuilder.lit("x")).as("c"));
```

完整函数清单见文末[附录](#附录funcbuilder-可用函数)。

### 4.3 CASE WHEN

`FuncBuilder.caseWhen()` 返回一个构建器，`when(condition, result)` / `otherwise(result)` 均遵循同样的参数约定（条件/结果用 `String` 裸表达式或 `lit(...)` 值字面量），最后 `build().as(alias)`。只有一个条件的简单场景可用 `caseWhen(condition, then, otherwise)` 三参快捷版：

```java
// CASE WHEN age < 18 THEN ? ELSE ? END
SQL.table("t_user")
        .select(FuncBuilder.caseWhen("age < 18", FuncBuilder.lit("未成年"), FuncBuilder.lit("成年")).as("age_group"));
```

多条件仍用构建器（条件用 `String`、其它字面量）：

```java
SQL<?> sql = SQL.table("t_user")
        .select(FuncBuilder.col("id"), FuncBuilder.col("name"),
                FuncBuilder.caseWhen()
                        .when("age < 18", FuncBuilder.lit("未成年"))
                        .when("age >= 18 AND age < 60", FuncBuilder.lit("成年"))
                        .otherwise(FuncBuilder.lit("老年"))
                        .build().as("age_group"))
        .limit(10);

// SELECT id, name,
//   CASE WHEN age < 18 THEN ? WHEN age >= 18 AND age < 60 THEN ? ELSE ? END AS age_group
// FROM t_user LIMIT 10
// [未成年, 成年, 老年]
```

### 4.4 ORDER BY

`orderBy(...)` 默认升序；调用紧随其后的 `desc()` 可将最后一个排序字段设为降序。当前 API 没有 `asc()` 方法。

```java
SQL<?> query = SQL.table("t_user")
        .select()
        .orderBy("created_at").desc()
        .orderBy("id"); // ASC
```

### 4.5 LIMIT 与 OFFSET

```java
SQL<?> query = SQL.table("t_user")
        .select()
        .limit(20)
        .offset(40);
```

该构建器输出 `LIMIT ... OFFSET ...`。涉及不同数据库方言的业务分页，优先使用 `BaseDao.paginate(...)`。

---

## 5. JOIN

`SELECT` 与 `UPDATE` 支持 `LEFT / RIGHT / INNER / CROSS` 连接。

### 5.1 基本 JOIN（`.on(...)`）

```java
SQL<?> query = SQL.table("tb_user")
        .select("name", "r.role_name")
        .leftJoin("tb_role", "r")
        .on("tb_user.role_id", "r.id")
        .where(w -> w.eq("is_active", 1));
// SELECT name, r.role_name FROM tb_user
// LEFT JOIN tb_role r ON tb_user.role_id = r.id
// WHERE is_active = ?
```

`on(field1, field2)` 表示两个字段相等（右侧为字段引用，不参数化）；`on(field1, opt, value)` 表示带操作符、右侧为值（参数化）。

连接条件也支持**实体方法引用（Lambda）**，`on` / `and` / `andIfAbsent` / `onIfAbsent` 都提供 `TypeFunction` 重载；并且 **JOIN 的表名可直接传实体类**（`leftJoin(Role.class, "r", ...)`），表名由 `@Table` 注解解析，`LEFT / RIGHT / INNER / CROSS` 四种连接都有相同的 `Class` 重载：

```java
SQL<Role> sq = SQL.table(Role.class, "r")
        .select("r.name")
        .leftJoin(Token.class, "t", on -> on
                .on(Role::getName, Token::getTk)                 // 列到列（Lambda）
                .andIfAbsent(Token::getStatus, "=", status));    // 值为空则跳过
// SELECT r.name FROM t_role r LEFT JOIN t_token t ON name = tk [AND status = ?]
```

```java
// RIGHT JOIN / INNER JOIN / CROSS JOIN
SQL.table("tb_user").select("*").rightJoin("tb_role", "r").on("tb_user.role_id", "r.id");
SQL.table("tb_user").select("*").innerJoin("tb_role", "r").on("tb_user.role_id", "r.id");
SQL.table("a").select("*").crossJoin("b", "bb");
// SELECT * FROM a CROSS JOIN b bb
```

### 5.2 回调式 JOIN（`.leftJoin(table, alias, on -> on.on().and())`）

```java
// SELECT u.name, r.role_name
// FROM tb_user u INNER JOIN tb_role r ON u.role_id = r.id AND r.status = ?
// WHERE u.is_active = ?
SQL<?> query = SQL.table("tb_user")
        .select("u.name", "r.role_name")
        .innerJoin("tb_role", "r", on -> on
                .on("u.role_id", "r.id")
                .and("r.status", "=", 1))
        .where(w -> w.eq("u.is_active", 1));
```

回调里 `on...` 的 `on(f1, f2)` / `and(f1, opt, value)` 与链式 JOIN 一致：`and(f1, f2)` 是两个字段相等，`and(f1, opt, value)` 是右侧为值。

`andIfAbsent(f1, opt, value)`（以及首个条件的 `onIfAbsent`）在 **value 为 `null` 或空字符串时跳过该条件**，适合动态拼接可选条件：

```java
// 当 deptType 为 null 时不生成 d.type 条件
SQL<?> query = SQL.table("tb_user", "u")   // 主表用别名
        .select("u.name", "r.role_name", "d.dept_name")
        .innerJoin("tb_role", "r", on -> on
                .on("u.role_id", "r.id")
                .and("r.status", "=", 1))
        .leftJoin("tb_department", "d", on -> on
                .on("u.dept_id", "d.id")
                .andIfAbsent("d.type", "=", deptType))
        .where(w -> w.eq("u.is_active", 1));
// SELECT u.name, r.role_name, d.dept_name
// FROM tb_user u INNER JOIN tb_role r ON u.role_id = r.id AND r.status = ?
// LEFT JOIN tb_department d ON u.dept_id = d.id   (d.type 条件被跳过)
// WHERE u.is_active = ?
```

---

## 6. 子查询

### 6.1 派生表 `from(SQL, alias)`

把另一个 `SQL` 作为 `FROM` 的子查询（派生表）。

```java
SQL<?> query = SQL.table("d")
        .select("a.*")
        .from(SQL.table("tb_user").select("u.id").where(c -> c.gt("age", 18)), "a");
// SELECT a.* FROM (SELECT u.id FROM tb_user WHERE age > ?) a
// [18]
```

### 6.2 条件子查询

见 [3.4 条件子查询](#34-条件子查询)（`in/notIn/exists/notExists`）。

### 6.3 嵌套子查询

`from(SQL...)` 可无限嵌套。

```java
SQL<?> query = SQL.table("t")
        .select("a.*")
        .from(SQL.table("tb_user")
                .select("b.*")
                .from(SQL.table("tb_order").select("c.*").where(c -> c.eq("status", "PAID")), "c")
                .where(c -> c.gt("age", 18)), "b")
        .where(c -> c.eq("platform", "APP"));
```

---

## 7. UNION / UNION ALL

`union(SQL)` / `unionAll(SQL)` 拼接后续查询片段（参数按片段顺序并入）。

```java
SQL<?> query = SQL.table("a").select("id").where(c -> c.eq("x", 1))
        .unionAll(SQL.table("b").select("id").where(c -> c.eq("y", 2)));
// SELECT id FROM a WHERE x = ? UNION ALL SELECT id FROM b WHERE y = ?
// [1, 2]
```

支持多段拼接：

```java
SQL<?> query = SQL.table("a").select("id").where(c -> c.eq("x", 1))
        .unionAll(SQL.table("b").select("id").where(c -> c.eq("y", 2)))
        .unionAll(SQL.table("c").select("id").where(c -> c.eq("z", 3)));
// SELECT id FROM a WHERE x = ? UNION ALL SELECT id FROM b WHERE y = ? UNION ALL SELECT id FROM c WHERE z = ?
// [1, 2, 3]
```

---

## 8. DISTINCT 与行锁

```java
// DISTINCT
SQL<?> distinct = SQL.table("t_user").selectDistinct("age");
// SELECT DISTINCT age FROM t_user

// 行锁
SQL<?> lock = SQL.table("t_user").select("id").where(c -> c.eq("id", 99)).forUpdate();
// SELECT id FROM t_user WHERE id = ? FOR UPDATE

SQL<?> shareMode = SQL.table("t_user").select("id").lockInShareMode();
// SELECT id FROM t_user LOCK IN SHARE MODE
```

---

## 9. 复杂 UPDATE

`UPDATE` 支持连接更新、字段引用赋值（右侧为列）以及子查询赋值。

### 9.1 基础更新

```java
SQL<?> update = SQL.table("t_user")
        .update()
        .set("name", "李四")
        .set("age", 30)
        .where(w -> w.eq("id", 1));
// UPDATE t_user SET name = ?, age = ? WHERE id = ?
// [李四, 30, 1]
```

### 9.2 连接更新 + 字段引用赋值

```java
// UPDATE tb_user INNER JOIN tb_role r ON tb_user.role_id = r.id
// SET name = r.role_name WHERE tb_user.role_id = ?
SQL<?> update = SQL.table("tb_user")
        .update()
        .set("name", new FieldReference("r.role_name"))
        .innerJoin("tb_role", "r")
        .on("tb_user.role_id", "r.id")
        .where(w -> w.eq("tb_user.role_id", 3));
```

### 9.3 子查询赋值

```java
// UPDATE tb_user SET email = (SELECT email FROM tb_user_info WHERE user_id = ?) WHERE id = ?
SQL<?> update = SQL.table("tb_user")
        .update()
        .set("email", SQL.table("tb_user_info").select("email").where(c -> c.eq("user_id", 9)))
        .where(w -> w.eq("id", 9));
```

---

## 10. 执行与参数顺序

```java
SQL<User> query = SQL.table(User.class)
        .select(User::getId, User::getName)
        .where(w -> w.eq(User::getStatus, "ACTIVE"));

String sql = query.toSql();
List<Object> parameters = query.getParameters();
List<User> users = userDao.select(query);
```

`getParameters()` 的顺序始终与 `toSql()` 中的 `?` 占位符一致。子查询、JOIN 的 ON 条件、UNION 各片段、函数参数都会按其在 SQL 中的位置自动并入。不要把参数直接拼接到 SQL 文本中。

---

## 11. 使用约束与建议

- 一个构建器只能选择一种操作：`select`、`insert`、`update` 或 `delete`。
- `where(...)` 只能调用一次；需要多个条件时，在同一个回调中组合。
- `having(...)` 只能调用一次，且仅适用于 `SELECT`。
- `UPDATE` 至少需要一个 `set(...)`，并且必须有 `where(...)`；`DELETE` 必须有 `where(...)`。
- `IN/NOT IN` 不接受 `null` 或空集合；子查询条件要求子查询非空。
- `SELECT` 的 `JOIN` 只在 `SELECT` / `UPDATE` 中合法，`DELETE` 不支持 `JOIN`。
- 表名、列名、排序字段和原始表达式文本（`FuncBuilder.col(...)` / `FuncExpr.of(...)` / `FuncBuilder.raw(...)` 中的字符串）不属于值参数，不要直接信任外部输入。
- 聚合函数：使用 `FuncBuilder` 的静态方法创建聚合表达式，用 `.as("alias")` 设置结果列名；`count` 默认别名为小写 `total`。
- 旧 `Expression` 类**已移除**，其能力（`count/sum/avg/max/min/coalesce/ifNull/caseWhen`）已合并到 `FuncBuilder`，请使用 `FuncBuilder` / `FuncExpr`。

---

## 12. 部分完整示例

### 12.1 SELECT 语句

```java
// 简单条件
SQL<?> sql1 = SQL.table("user")
        .select("id", "name")
        .where(w -> w.leftLike("name", "张")
                .ge("age", 20)
                .in("status", Arrays.asList("ACTIVE", "PENDING"))
                .betweenAnd("create_time", "2023-01-01", "2023-02-01"));
// SELECT id, name FROM user WHERE name LIKE ? AND age >= ? AND status IN (?, ?) AND create_time BETWEEN ? AND ?
// [%张, 20, ACTIVE, PENDING, 2023-01-01, 2023-02-01]
```

```java
// 聚合 + 函数 + 别名 + 分组排序
SQL<?> report = SQL.table("user")
        .select(FuncBuilder.col("name"),
                FuncBuilder.count("*").as("total"),
                FuncBuilder.avg("age").as("avg_age"))
        .where(w -> w.eq("status", "active"))
        .groupBy("name")
        .orderBy("total").desc();
// SELECT name, COUNT(*) AS total, AVG(age) AS avg_age
// FROM user WHERE status = ? GROUP BY name ORDER BY total DESC
// [active]
```

### 12.2 INSERT / UPDATE / DELETE

```java
// INSERT
SQL<?> insertSql = SQL.table("user")
        .insert("id", "name", "age")
        .values(1, "张三", 25);
// INSERT INTO user (id, name, age) VALUES (?, ?, ?)
// [1, 张三, 25]

// UPDATE
SQL<?> updateSql = SQL.table("user")
        .update()
        .set("name", "李四")
        .set("age", 30)
        .where(w -> w.eq("id", 1));
// UPDATE user SET name = ?, age = ? WHERE id = ?
// [李四, 30, 1]

// DELETE（含 OR 分组）
SQL<?> deleteSql = SQL.table("user")
        .delete()
        .where(w -> w.eq("id", 1).or(g -> g.eq("name", "测试")));
// DELETE FROM user WHERE id = ? OR (name = ?)
// [1, 测试]
```

### 12.3 端到端：列表查询（别名 + JOIN + 函数 + 分组 + 分页）

把前面各章串成一个完整业务场景：查"启用状态"的用户，带出角色名，按姓名分组统计并降序，分页取前 50。

```java
// 组装
SQL<?> page = SQL.table("tb_user", "u")                          // 主表别名
        .select("u.name",
                FuncBuilder.groupConcat("r.role_name").as("roles"),
                FuncBuilder.count("u.id").as("cnt"))
        .leftJoin("tb_role", "r", on -> on.on("u.role_id", "r.id"))   // 回调式 JOIN
        .where(w -> w.eq("u.is_active", 1)                      // 多条件
                .like("u.name", keyword)
                .ge("u.age", 18))
        .groupBy("u.name")                                      // 分组
        .orderBy("cnt").desc()                                  // 排序
        .limit(50);                                             // 分页

// 交付执行层（BaseDao / JdbcTemplateHelper 均可）
String sql = page.toSql();
List<Object> params = page.getParameters();
List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

// 生成的 SQL 与参数（示意）：
// SELECT u.name, GROUP_CONCAT(r.role_name) AS roles, COUNT(u.id) AS cnt
// FROM tb_user u
// LEFT JOIN tb_role r ON u.role_id = r.id
// WHERE u.is_active = ? AND u.name LIKE ? AND u.age >= ?
// GROUP BY u.name ORDER BY cnt DESC LIMIT 50
// params = [1, "%张%", 18]
```

要点回顾：
- `SQL.table("tb_user", "u")` 给主表起别名，`u.xxx` 才能在查询里引用。
- `leftJoin(table, alias, on -> on.on(...))` 回调式连接条件。
- `where(w -> ...)` 里用 `eq` / `like` / `ge` 组织非等值条件，都是参数化。
- `groupConcat(...).as("roles")`、`count(...).as("cnt")` 用 `FuncBuilder` 生成函数并起别名。
- 最终 `page.toSql()` / `page.getParameters()` 拿到符合 `?` 顺序的 SQL 与参数，交给执行层。

---

## 附录：FuncBuilder 可用函数

> 多数函数都提供 `FuncExpr` 重载，可嵌套（如 `year(now())`）。任意函数也可用通用入口 `func(name, args...)` 组合。

- **聚合**：`count`、`countDistinct`、`sum`、`avg`、`max`、`min`、`groupConcat`、`groupConcatDistinct`
- **字符串**：`concat`、`concat_ws`、`length`、`charLength`、`substring`、`upper`、`lower`、`ltrim`、`rtrim`、`trim`、`left`、`right`、`locate`、`instr`、`replace`、`findInSet`、`position`、`elt`、`insert`
- **数值**：`abs`、`ceil`、`floor`、`round`、`mod`、`truncate`、`rand`
- **日期**：`now`、`curdate`、`curtime`、`dateFormat`、`format`、`year`、`month`、`day`、`hour`、`minute`、`second`、`week`、`weekday`、`dayname`、`monthname`、`date`、`dateAdd`、`dateSub`、`strToDate`、`unixTimeStamp`、`fromUnixTime`
- **条件**：`ifNull`、`coalesce`、`nullIf`、`caseWhen`（构建器 + `caseWhen(cond, then, otherwise)` 三参版）、`_if`
- **JSON**：`jsonExtract`、`jsonUnquote`、`jsonContains`、`jsonSet`、`jsonRemove`、`jsonObject`、`jsonArray`
- **其它**：`col`（列引用）、`lit`（值字面量）、`of`（`FuncExpr.of`，裸 SQL 表达式）、`distinct`、`func`（通用函数调用）

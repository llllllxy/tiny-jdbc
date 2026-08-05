# SQL 构建器使用指南

`SQL` 用于以链式方式构建参数化的 `SELECT`、`INSERT`、`UPDATE` 和 `DELETE` 语句。构建完成后可调用 `toSql()` 获取 SQL，调用 `getParameters()` 获取与 `?` 占位符顺序一致的参数；也可以直接交给 `BaseDao` 执行。

> 值参数会使用 `?` 绑定；表名、列名以及 `Expression.of(String)` 中的字符串属于 SQL 结构，必须来自受信任的代码，不能直接使用前端输入。

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

## 2. 创建构建器与选择操作

### 2.1 指定表

```java
SQL<?> byTableName = SQL.table("t_user");
SQL<User> byEntity = SQL.table(User.class); // User 必须标注 @Table
```

### 2.2 SELECT

未指定查询列时会生成 `SELECT *`；也可以显式调用 `select()` 表示查询全部列。

```java
SQL.table("t_user").select();
SQL.table("t_user").select("id", "name", "created_at");

SQL<User> query = SQL.table(User.class)
        .select(User::getId, User::getName);
```

### 2.3 INSERT

`insert(...)` 中的列与 `values(...)` 中的值必须一一对应，数量不一致会抛出异常。

```java
SQL<?> insert = SQL.table("t_user")
        .insert("name", "age", "status")
        .values("张三", 18, "ACTIVE");

// INSERT INTO t_user (name, age, status) VALUES (?, ?, ?)
// [张三, 18, ACTIVE]
```

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

## 3. WHERE 与 HAVING 条件

`where(...)` 和 `having(...)` 的回调参数都是 `ConditionGroup`。未显式指定时，多个条件默认以 `AND` 连接。

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

`in/notIn` 接收任意非空 `Collection`，包括 `List`、`Set` 和队列。若需稳定的参数输出顺序，使用 `List` 或 `LinkedHashSet`。

```java
Collection<Long> ids = new LinkedHashSet<>(Arrays.asList(1L, 2L, 3L));

SQL<?> query = SQL.table("t_user")
        .select("id", "name")
        .where(w -> w.in("id", ids)
                .notIn("status", Collections.singleton("DELETED"))
                .betweenAnd("age", 18, 60));
```

### 3.1 OR 与分组

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

### 3.2 Lambda 条件

字符串列名与实体方法引用可以混用；方法引用会按实体字段映射转换为列名。

```java
SQL<User> query = SQL.table(User.class)
        .select(User::getId, User::getName)
        .where(w -> w.eq(User::getStatus, "ACTIVE")
                .in(User::getId, Arrays.asList(1L, 2L)));
```

## 4. 分组、表达式、排序与分页

### 4.1 GROUP BY 与 HAVING

`Expression` 用于聚合函数、别名和数据库表达式。表达式文本不会被参数化，动态内容必须自行校验。

```java
SQL<?> report = SQL.table("t_order")
        .select(
                Expression.of("user_id"),
                Expression.count("*").as("total"),
                Expression.sum("amount").as("total_amount"))
        .where(w -> w.eq("status", "PAID"))
        .groupBy("user_id")
        .having(w -> w.gt("total_amount", 1000));

// SELECT user_id, COUNT(*) AS total, SUM(amount) AS total_amount
// FROM t_order WHERE status = ? GROUP BY user_id HAVING total_amount > ?
```

常用表达式包括 `Expression.count`、`sum`、`avg`、`min`、`max`、`coalesce`、`ifNull` 和 `caseWhen`。需要控制输出列名时使用 `as("alias")`。

### 4.2 ORDER BY

`orderBy(...)` 默认升序；调用紧随其后的 `desc()` 可将最后一个排序字段设为降序。当前 API 没有 `asc()` 方法。

```java
SQL<?> query = SQL.table("t_user")
        .select()
        .orderBy("created_at").desc()
        .orderBy("id"); // ASC
```

### 4.3 LIMIT 与 OFFSET

```java
SQL<?> query = SQL.table("t_user")
        .select()
        .limit(20)
        .offset(40);
```

该构建器输出 `LIMIT ... OFFSET ...`。涉及不同数据库方言的业务分页，优先使用 `BaseDao.paginate(...)`。

## 5. 执行与参数顺序

```java
SQL<User> query = SQL.table(User.class)
        .select(User::getId, User::getName)
        .where(w -> w.eq(User::getStatus, "ACTIVE"));

String sql = query.toSql();
List<Object> parameters = query.getParameters();
List<User> users = userDao.select(query);
```

`getParameters()` 的顺序始终与 `toSql()` 中的 `?` 占位符一致。不要把参数直接拼接到 SQL 文本中。

## 6. 使用约束与建议

- 一个构建器只能选择一种操作：`select`、`insert`、`update` 或 `delete`。
- `where(...)` 只能调用一次；需要多个条件时，在同一个回调中组合。
- `having(...)` 只能调用一次，且仅适用于 `SELECT`。
- `UPDATE` 至少需要一个 `set(...)`，并且必须有 `where(...)`；`DELETE` 必须有 `where(...)`。
- `IN/NOT IN` 不接受 `null` 或空集合。
- 表名、列名、排序字段和原始 `Expression` 文本不属于值参数；不要直接信任外部输入。
- 聚合函数：使用Expression类的静态方法创建聚合表达式时，默认使用字段名作为结果别名，可使用as()方法进行设置别名；count函数的结果默认别名为total小写；caseWhen函数必须使用as()方法指定别名。


## 7. 部分完整示例
### 7.1 SELECT 语句

```java
// 示例：简单条件
SQL<?> sql1 = SQL.table("user")
        .select("id", "name")
        .where(i -> i.leftLike("name", "张")
                .ge("age", 20)
                .in("status", Arrays.asList("ACTIVE", "PENDING"))
                .betweenAnd("create_time", "2023-01-01", "2023-02-01"));
// SQL: SELECT id, name FROM user WHERE name LIKE ? AND age >= ? AND status IN (?, ?) AND create_time BETWEEN ? AND ?
// Parameters: [%张, 20, ACTIVE, PENDING, 2023-01-01, 2023-02-01]
```

```java
// 示例： 简单条件(使用OR)
SQL<User> selectSql7 = SQL.table(User.class)
                .select(User::getId, User::getName)
                .where(i -> i.eq(User::getAge, 25)
                        .or().eq(User::getAge, 30));
                
// SQL: SELECT id, username FROM users WHERE age = ? OR age = ? 
// Parameters: [25, 30]
```

```java
// 示例：嵌套 AND/OR 条件
SQL<?> sql1 = SQL.table("user")
        .select("id", "birthday")
        .where(i -> i.and(j -> j.eq("name", "李白").eq("status", "alive"))
                .or(j -> j.eq("name", "杜甫").eq("status", "alive")))
        .orderBy("updated_at").desc()
        .orderBy("id");
// SQL: SELECT id, birthday FROM user WHERE (name = ? AND status = ?) OR (name = ? AND status = ?) ORDER BY updated_at DESC, id ASC
// Parameters: [李白, alive, 杜甫, alive]
```

```java
// 示例：多层嵌套条件
SQL<?> sql2 = SQL.table("article").select()
        .where(i -> i.and(j -> j.eq("category", "java").like("title", "spring"))
                .or(j -> j.eq("author", "张三").and(k -> k.lt("views", 1000).ge("comments", 5))))
        .limit(20);

// SQL: SELECT * FROM article WHERE (category = ? AND title LIKE ?) OR (author = ? AND (views < ? AND comments >= ?)) LIMIT 20
// Parameters: [java, %spring%, 张三, 1000, 5]
```

```java
// 示例：混合条件
SQL<?> sql3 = SQL.table("product")
        .select("id", "name", "price")
        .where(i -> i.eq("status", "active")
                .and(j -> j.gt("price", 100).or(k -> k.like("name", "pro")))
                .and(j -> j.in("category", Arrays.asList("electronics", "books"))))
        .orderBy("price").desc();
// SELECT id, name, price FROM product WHERE status = ? AND (price > ? OR (name LIKE ?)) AND (category IN (?, ?)) ORDER BY price DESC
// Parameters: [active, 100, %pro%, electronics, books]
```

```java
// 示例：复杂 OR 分组
SQL<?> sql4 = SQL.table("order").select("id", "order_no", "amount", "create_time")
        .where(i -> i.or(j -> j.eq("status", "paid").eq("amount", 1000))
                .or(j -> j.eq("status", "pending").gt("amount", 5000))
                .or(j -> j.eq("status", "cancelled").le("create_time", "2023-01-01")))
        .orderBy("create_time").desc();
// SQL: SELECT id, order_no, amount, create_time FROM order WHERE (status = ? AND amount = ?) OR (status = ? AND amount > ?) OR (status = ? AND create_time <= ?) ORDER BY create_time DESC
// Parameters: [paid, 1000, pending, 5000, cancelled, 2023-01-01]
```

```java
// 示例：复杂嵌套 with like
SQL<?> sql6 = SQL.table("order").select("id", "order_no", "amount", "create_time")
        .where(i -> i.and(j -> j.eq("status", "paid").leftLike("order_no", "ORD2023"))
                .or(j -> j.rightLike("customer_name", "先生").gt("amount", 1000)));
// SELECT id, order_no, amount, create_time FROM order WHERE (status = ? AND order_no LIKE ?) OR (customer_name LIKE ? AND amount > ?)
// [paid, %ORD2023, 先生%, 1000]
```

```java
// 示例：使用orderBy
SQL<?> selectSql = SQL.table("user")
                .select("id", "name")
                .where(i -> i.leftLike("name", "张")
                        .and().ge("age", 20)
                        .and().in("status", Arrays.asList("ACTIVE", "PENDING")))
                .orderBy("id").desc()
                .orderBy("age");
// SQL: SELECT id, name FROM user WHERE name LIKE ? AND age >= ? AND status IN (?, ?) ORDER BY id DESC, age ASC
// Parameters: [张%, 20, ACTIVE, PENDING]
```

```java
// 示例：使用实体类方法引用
 SQL<User> selectSql6 = SQL.table(User.class)
                .select(User::getId, User::getName)
                .where(i -> i.eq(User::getAge, 25)
                        .or(j -> j.like(User::getName, "张")))
                .orderBy(User::getId)
                .desc();
// SQL: SELECT id, username FROM users WHERE age = ? OR (username LIKE ?) ORDER BY id DESC
// Parameters: [25, %张%]
```

```java
// 示例： 使用group()方法添加括号
SQL<?> selectSql8 = SQL.table("user")
        .select("*")
        .where(i -> i.group(j -> j.eq("age", 25).or().eq("age", 30))
                .and().like("name", "张"));
// SQL: SELECT * FROM user WHERE (age = ? OR age = ?) AND name LIKE ?
// Parameters: [25, 30, %张%]
```

```java
// 示例：嵌套括号
SQL<?> complexSql9 = SQL.table("user")
        .select("*")
        .where(i -> i.group(j -> j.eq("status", "ACTIVE")
                        .and().group(k -> k.gt("age", 18).and().lt("age", 60)))
                .or().eq("role", "ADMIN"));
// SQL: SELECT * FROM user WHERE (status = ? AND (age > ? AND age < ?)) OR role = ?
// Parameters: [ACTIVE, 18, 60, ADMIN]
```

```java
// 示例：使用表达式配合GROUP BY、聚合函数、AS使用
SQL<?> complexSql10 = SQL.table("user")
                // 传入多个列名和表达式（顺序任意）
                .select(Expression.of("id"),
                        Expression.of("birthday"),
                        Expression.of(User::getEmail),
                        Expression.max("age").as("maxAge"), // 带别名的聚合表达式
                        Expression.count("*").as("total") // 带别名的聚合表达式
                )
                .where(i -> i.eq("status", "active"))
                .groupBy("name", "status") // GROUP BY多个列
                .orderBy("maxAge").desc();
// SQL: SELECT id, birthday, email, MAX(age) AS maxAge, COUNT(*) AS total FROM user WHERE status = ? GROUP BY name, status ORDER BY maxAge DESC
// Parameters: [active]
```

```java
// 示例：使用GROUP BY 和 HAVING子句
SQL<?> sql = SQL.table("user")
                // 传入多个列名和表达式（顺序任意）
                .select(Expression.of("id"),
                        Expression.of("birthday"),
                        Expression.of(User::getEmail),
                        Expression.max("age").as("maxAge"), // 带别名的聚合表达式
                        Expression.count("*").as("total") // 带别名的聚合表达式
                )
                .where(i -> i.eq("status", "active"))
                .groupBy("name", "status") // GROUP BY多个列
                .having(i -> i.gt("maxAge", 25))
                .orderBy("maxAge").desc();
// SQL: SELECT id, birthday, email, MAX(age) AS maxAge, COUNT(*) AS total FROM user WHERE status = ? GROUP BY name, status HAVING maxAge > ? ORDER BY maxAge DESC
// Parameters: [active, 25]
```

```java
// 示例：使用GROUP BY 和 HAVING子句（在HAVING子句子句中使用表达式）
SQL<?> sql = SQL.table("user")
                // 传入多个列名和表达式（顺序任意）
                .select(Expression.of("id"),
                        Expression.of("birthday"),
                        Expression.of(User::getEmail),
                        Expression.max("age").as("maxAge"), // 带别名的聚合表达式
                        Expression.count("*").as("total") // 带别名的聚合表达式
                )
                .where(i -> i.eq("status", "active"))
                .groupBy("name", "status") // GROUP BY多个列
                .having(i -> i.gt(Expression.max("age").toString(), 25))
                .orderBy("maxAge").desc();
// SQL: SELECT id, birthday, email, MAX(age) AS maxAge, COUNT(*) AS total FROM user WHERE status = ? GROUP BY name, status HAVING MAX(age) > ? ORDER BY maxAge DESC
// Parameters: [active, 25]
```

```java
// 示例：使用Expression表达式进行CASE WHEN操作
Expression caseWhenExpr = Expression.caseWhen()
        .when("age < 18", "'未成年'")
        .when("age >= 18 AND age < 60", "'成年'")
        .otherwise("'老年'")
        .build();
SQL<?> sql = SQL.table("user")
        .select(Expression.of("id"), Expression.of("name"), caseWhenExpr.as("age_group"))
        .limit(10);

// SQL: SELECT id, name, CASE WHEN age < 18 THEN '未成年' WHEN age >= 18 AND age < 60 THEN '成年' ELSE '老年' END AS age_group FROM user LIMIT 10
// Parameters: []
```

```java
// 示例：使用Expression表达式进行COALESCE函数操作
SQL<?> sql = SQL.table("user")
        .select(Expression.of("id"), Expression.coalesce("name", "'未知'"))
        .limit(10);

// SQL: SELECT id, COALESCE(name, '未知') AS name FROM user LIMIT 10
// Parameters: []
```

```java
// 示例：使用Expression表达式进行IFNULL函数操作
Expression ifNullExpr = Expression.ifNull("email", "'no-email@example.com'");
SQL<?> sql = SQL.table("user")
        .select(Expression.of("id"), ifNullExpr.as("user_email"))
        .limit(10);

// SQL: SELECT id, IFNULL(email, 'no-email@example.com') AS user_email FROM user LIMIT 10
// Parameters: []
```

### 7.2 INSERT 语句
```java
 SQL<?> insertSql = SQL.table("user")
        .insert("id", "name", "age")
        .values(1, "张三", 25);
// SQL: INSERT INTO user(id, name, age) VALUES (?, ?, ?)
// Parameters: [1, 张三, 25]
```

### 7.3 UPDATE 语句
```java
 SQL<?> updateSql = SQL.table("user")
        .update()
        .set("name", "李四")
        .set("age", 30)
        .where(i -> i.eq("id", 1));
// UPDATE user SET name = ?, age = ? WHERE id = ?
// Parameters: [李四, 30, 1]
```

###  7.4 DELETE 语句
```java
// 测试用例 3：DELETE
SQL<?> deleteSql = SQL.table("user")
        .delete()
        .where(i -> i.eq("id", 1).or(j -> j.eq("name", "测试")));
// DELETE FROM user WHERE id = ? OR (name = ?)
// Parameters: [1, 测试]
```

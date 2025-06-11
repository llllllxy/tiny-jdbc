# SQL 构建器使用文档

## 1. 基本使用

### 1.1 初始化表

```java
// 使用表名初始化
SQL sql = SQL.table("user");

// 使用实体类初始化（需配合@Table注解）
SQL sql = SQL.table(User.class);
```

### 1.2 选择操作类型

**SELECT**
```java
// 使用普通字符串
sql.select("id", "name", "age");

// 使用实体类方法引用
sql.select(User::getId, User::getName);

// 使用Expression表达式，Expression支持各方方式混用（在使用聚合函数或者混用时，必须使用表达式形式）
sql.select(
    Expression.of("id"),  
    Expression.of(User::getEmail),
    Expression.max("age").as("maxAge"),
    Expression.count("*").as("total")
);
```

**INSERT**
```java
sql.insert("id", "name", "age").values(1, "张三", 25);
```

**UPDATE**
```java
sql.update()
   .set("name", "李四")
   .set("age", 30)
   .where(i -> i.eq("id", 1));
```

**DELETE**
```java
sql.delete()
   .where(i -> i.eq("id", 1));
```

## 2. 条件构建

### 2.1 基本条件
```java
.where(i -> i
    .eq("name", "张三")      // =
    .notEq("status", "deleted") // !=
    .gt("age", 18)          // >
    .lt("age", 60)          // <
    .ge("score", 90)        // >=
    .le("score", 100)       // <=
    .like("title", "Java")  // LIKE '%Java%'
    .notLike("title", "Java")  // NOT LIKE '%Java%'
    .leftLike("code", "P")  // 左匹配（后缀匹配） LIKE '%P'
    .notLeftLike("code", "P") // 左匹配（后缀匹配） NOT LIKE '%P'
    .rightLike("email", "@") // 右匹配（前导匹配）LIKE '@%'
    .notRightLike("email", "@") // 右匹配（前导匹配）NOT LIKE '@%'
    .in("category", Arrays.asList("book", "electronics")) // IN ('book', 'electronics')
    .notIn("category", Arrays.asList("book", "electronics")) // NOT IN ('book', 'electronics')
    .isNull("deleted_at")   // IS NULL 
    .isNotNull("created_at") // IS NOT NULL
    .betweenAnd("create_time", "2023-01-01", "2023-02-01") // BETWEEN '2023-01-01' AND '2023-02-01'
    .notBetweenAnd("create_time", "2023-01-01", "2023-02-01") // NOT BETWEEN '2023-01-01' AND '2023-02-01'
)
```

### 2.2 逻辑组合
```java
// AND条件（默认连接方式）
.where(i -> i
    .eq("status", "active")
    .eq("type", "vip")
)

// 显式AND连接
.where(i -> i.and().eq("status", "active")
             .and().eq("type", "vip")
)

// OR条件
.where(i -> i
    .eq("status", "active")
    .or().eq("status", "pending")
)
```

### 2.3 括号优先级
```java
// 简单括号
.where(i -> i
    .group(j -> j.eq("age", 25).or().eq("age", 30))
    .and().like("name", "张")
)

// 嵌套括号
.where(i -> i
    .group(j -> j
        .eq("status", "ACTIVE")
        .and().group(k -> k.gt("age", 18).and().lt("age", 60))
    )
    .or().eq("role", "ADMIN")
)
```

## 3. 高级查询
### 3.1 聚合函数与 GROUP BY
```java
sql.select(
    "name",
    Expression.count("*").as("total"),
    Expression.sum("amount").as("totalAmount"),
    Expression.avg("score").as("avgScore"),
    Expression.max("age").as("maxAge"),
    Expression.min("age").as("minAge")
)
.groupBy("name")
.having(i -> i.gt("totalAmount", 1000));
```

### 3.2 排序与分页
```java
// 排序
sql.orderBy("created_at").desc()  // 降序
   .orderBy("id").asc();          // 升序（默认）

// 分页
sql.limit(10)  // 每页10条
   .offset(20); // 偏移量20（第3页）
```

## 4. 使用实体类方法引用
### 4.1 实体类定义
```java
@Table("user")
class User {
    @Column("id")
    private Long id;

    @Column("username")
    private String name;

    @Column("age")
    private Integer age;

    @Column("email")
    private String email;

    // getters and setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }
}
```

### 4.2 在查询中使用
```java
SQL sql = SQL.table(User.class)
    .select(User::getId, User::getName)
    .where(i -> i
        .eq(User::getAge, 25)
        .or().like(User::getName, "张三")
    )
    .orderBy(User::getId).desc()
    .limit(10);
```

## 5. 生成 SQL 与参数
```java
// 生成SQL语句
String sqlString = sql.toSql();

// 获取参数列表
List<Object> parameters = sql.getParameters();

// 示例输出
System.out.println("SQL: " + sqlString);
System.out.println("Parameters: " + parameters);
```

## 6. 完整示例
### 6.1 SELECT 语句

```java
// 示例：简单条件
SQL sql1 = SQL.table("user")
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
SQL selectSql7 = SQL.table(User.class)
                .select(User::getId, User::getName)
                .where(i -> i.eq(User::getAge, 25)
                        .or().eq(User::getAge, 30));
                
// SQL: SELECT id, username FROM users WHERE age = ? OR age = ? 
// Parameters: [25, 30]
```

```java
// 示例：嵌套 AND/OR 条件
SQL sql1 = SQL.table("user")
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
SQL sql2 = SQL.table("article").select()
        .where(i -> i.and(j -> j.eq("category", "java").like("title", "spring"))
                .or(j -> j.eq("author", "张三").and(k -> k.lt("views", 1000).ge("comments", 5))))
        .limit(20);

// SQL: SELECT * FROM article WHERE (category = ? AND title LIKE ?) OR (author = ? AND (views < ? AND comments >= ?)) LIMIT 20
// Parameters: [java, %spring%, 张三, 1000, 5]
```

```java
// 示例：混合条件
        SQL sql3 = SQL.table("product")
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
        SQL sql4 = SQL.table("order").select("id", "order_no", "amount", "create_time")
                .where(i -> i.or(j -> j.eq("status", "paid").eq("amount", 1000))
                        .or(j -> j.eq("status", "pending").gt("amount", 5000))
                        .or(j -> j.eq("status", "cancelled").le("create_time", "2023-01-01")))
                .orderBy("create_time").desc();
// SQL: SELECT id, order_no, amount, create_time FROM order WHERE (status = ? AND amount = ?) OR (status = ? AND amount > ?) OR (status = ? AND create_time <= ?) ORDER BY create_time DESC
// Parameters: [paid, 1000, pending, 5000, cancelled, 2023-01-01]
```

```java
// 示例：复杂嵌套 with like
SQL sql6 = SQL.table("order").select("id", "order_no", "amount", "create_time")
        .where(i -> i.and(j -> j.eq("status", "paid").leftLike("order_no", "ORD2023"))
                .or(j -> j.rightLike("customer_name", "先生").gt("amount", 1000)));
// SELECT id, order_no, amount, create_time FROM order WHERE (status = ? AND order_no LIKE ?) OR (customer_name LIKE ? AND amount > ?)
// [paid, %ORD2023, 先生%, 1000]
```

```java
// 示例：使用orderBy
SQL selectSql = SQL.table("user")
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
 SQL selectSql6 = SQL.table(User.class)
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
SQL selectSql8 = SQL.table("user")
        .select("*")
        .where(i -> i.group(j -> j.eq("age", 25).or().eq("age", 30))
                .and().like("name", "张"));
// SQL: SELECT * FROM user WHERE (age = ? OR age = ?) AND name LIKE ?
// Parameters: [25, 30, %张%]
```

```java
// 示例：嵌套括号
SQL complexSql9 = SQL.table("user")
        .select("*")
        .where(i -> i.group(j -> j.eq("status", "ACTIVE")
                        .and().group(k -> k.gt("age", 18).and().lt("age", 60)))
                .or().eq("role", "ADMIN"));
// SQL: SELECT * FROM user WHERE (status = ? AND (age > ? AND age < ?)) OR role = ?
// Parameters: [ACTIVE, 18, 60, ADMIN]
```

```java
// 示例：使用表达式配合GROUP BY、聚合函数、AS使用
SQL complexSql10 = SQL.table("user")
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
SQL sql = SQL.table("user")
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
SQL sql = SQL.table("user")
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


### 6.2 INSERT 语句
```java
 SQL insertSql = SQL.table("user")
        .insert("id", "name", "age")
        .values(1, "张三", 25);
// SQL: INSERT INTO user(id, name, age) VALUES (?, ?, ?)
// Parameters: [1, 张三, 25]
```

### 6.3 UPDATE 语句
```java
 SQL updateSql = SQL.table("user")
        .update()
        .set("name", "李四")
        .set("age", 30)
        .where(i -> i.eq("id", 1));
// UPDATE user SET name = ?, age = ? WHERE id = ?
// Parameters: [李四, 30, 1]
```

###  6.4 DELETE 语句
```java
// 测试用例 3：DELETE
SQL deleteSql = SQL.table("user")
        .delete()
        .where(i -> i.eq("id", 1).or(j -> j.eq("name", "测试")));
// DELETE FROM user WHERE id = ? OR (name = ?)
// Parameters: [1, 测试]
```

## 7. 注意事项

- 方法链顺序：操作需按逻辑顺序调用，如select()后接where()，update()后接set()。
- 参数安全：所有值参数会自动转为占位符?，防止 SQL 注入。
- Lambda 表达式：使用实体类方法引用时，确保方法存在且符合 Java Bean 规范。
- NULL 值处理：isNull()和isNotNull()用于判断 NULL 值，无需传入参数。
- 聚合函数：使用Expression类的静态方法创建聚合表达式，必须使用as()设置别名。
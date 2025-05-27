package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.sql.enums.ClauseState;
import org.tinycloud.jdbc.sql.enums.Operation;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQL {
    private final String table;
    private Operation operation;
    private final List<String> selectFields = new ArrayList<>();
    private final Map<String, Object> insertValues = new LinkedHashMap<>();
    private final Map<String, Object> updateValues = new LinkedHashMap<>();
    private final ConditionGroup whereCondition = new ConditionGroup();
    private final List<OrderBy> orderByClauses = new ArrayList<>();
    private final List<String> groupByColumns = new ArrayList<>();
    private final ConditionGroup havingCondition = new ConditionGroup();
    private Integer limit;
    private Integer offset;

    // 记录各子句的调用状态
    private volatile ClauseState whereState = ClauseState.NOT_CALLED;
    private volatile ClauseState havingState = ClauseState.NOT_CALLED;

    private SQL(String table) {
        this.table = table;
    }

    public static SQL table(String table) {
        return new SQL(table);
    }

    public static SQL table(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new IllegalArgumentException("类 " + entityClass.getName() + " 缺少 @Table 注解");
        }
        return new SQL(tableAnnotation.value());
    }


    // ------------------------ SELECT ------------------------

    public SQL select() {
        validateOperation(Operation.SELECT);
        this.selectFields.add("*");
        return this;
    }

    public SQL select(String... columns) {
        validateOperation(Operation.SELECT);
        this.selectFields.addAll(Arrays.asList(columns));
        return this;
    }

    @SafeVarargs
    public final <T> SQL select(TypeFunction<T, ?>... fields) {
        validateOperation(Operation.SELECT);
        for (TypeFunction<T, ?> field : fields) {
            String columnName = LambdaUtils.getLambdaColumnName(field);
            this.selectFields.add(columnName);
        }
        return this;
    }

    public SQL select(Expression... expressions) {
        validateOperation(Operation.SELECT);
        for (Expression expr : expressions) {
            selectFields.add(expr.toString());
        }
        return this;
    }

    public <T> SQL orderBy(TypeFunction<T, ?> field) {
        if (operation != Operation.SELECT) {
            throw new IllegalStateException("ORDER BY 只能用于 SELECT 语句");
        }
        String column = LambdaUtils.getLambdaColumnName(field);
        this.orderByClauses.add(new OrderBy(column, false));
        return this;
    }

    public SQL orderBy(String column) {
        if (operation != Operation.SELECT) {
            throw new IllegalStateException("ORDER BY 只能用于 SELECT 语句");
        }
        this.orderByClauses.add(new OrderBy(column, false));
        return this;
    }

    public SQL groupBy(String... columns) {
        if (operation != Operation.SELECT) {
            throw new IllegalStateException("GROUP BY 只能用于 SELECT 语句");
        }
        this.groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    @SafeVarargs
    public final <T> SQL groupBy(TypeFunction<T, ?>... fields) {
        if (operation != Operation.SELECT) {
            throw new IllegalStateException("GROUP BY 只能用于 SELECT 语句");
        }
        for (TypeFunction<T, ?> field : fields) {
            String column = LambdaUtils.getLambdaColumnName(field);
            this.groupByColumns.add(column);
        }
        return this;
    }

    public SQL having(Consumer<ConditionGroup> conditions) {
        if (operation != Operation.SELECT) {
            throw new IllegalStateException("HAVING 子句只能用于 SELECT 语句");
        }
        if (havingState == ClauseState.CALLED) {
            throw new IllegalStateException("HAVING 子句已被调用，不能重复调用");
        }
        havingState = ClauseState.CALLED;
        conditions.accept(havingCondition);
        return this;
    }

    public SQL desc() {
        if (!orderByClauses.isEmpty()) {
            OrderBy lastOrder = orderByClauses.get(orderByClauses.size() - 1);
            orderByClauses.set(orderByClauses.size() - 1, new OrderBy(lastOrder.column, true));
        }
        return this;
    }

    public SQL limit(int limit) {
        if (operation != Operation.SELECT) {
            throw new IllegalStateException("LIMIT 只能用于 SELECT 语句");
        }
        this.limit = limit;
        return this;
    }

    public SQL offset(int offset) {
        if (operation != Operation.SELECT) {
            throw new IllegalStateException("OFFSET 只能用于 SELECT 语句");
        }
        this.offset = offset;
        return this;
    }

    // ------------------------ INSERT ------------------------
    public SQL insert(String... columns) {
        validateOperation(Operation.INSERT);
        for (String column : columns) {
            insertValues.put(column, null);
        }
        return this;
    }

    @SafeVarargs
    public final <T> SQL insert(TypeFunction<T, ?>... fields) {
        validateOperation(Operation.INSERT);
        for (TypeFunction<T, ?> field : fields) {
            String columnName = LambdaUtils.getLambdaColumnName(field);
            this.insertValues.put(columnName, null);// 占位
        }
        return this;
    }

    public SQL values(Object... values) {
        if (operation != Operation.INSERT) {
            throw new IllegalStateException("values() 方法只能在 insert() 之后调用");
        }
        if (insertValues.isEmpty()) {
            throw new IllegalStateException("请先调用 insert() 方法指定列");
        }
        if (values.length != insertValues.size()) {
            throw new IllegalArgumentException("值的数量与列的数量不匹配");
        }
        int i = 0;
        for (String column : insertValues.keySet()) {
            insertValues.put(column, values[i++]);
        }
        return this;
    }

    // ------------------------ UPDATE ------------------------
    public SQL update() {
        validateOperation(Operation.UPDATE);
        return this;
    }

    public SQL set(String column, Object value) {
        if (operation != Operation.UPDATE) {
            throw new IllegalStateException("set() 方法只能在 update() 之后调用");
        }
        updateValues.put(column, value);
        return this;
    }

    public <T> SQL set(TypeFunction<T, ?> field, Object value) {
        if (operation != Operation.UPDATE) {
            throw new IllegalStateException("set() 方法只能在 update() 之后调用");
        }
        String column = LambdaUtils.getLambdaColumnName(field);
        updateValues.put(column, value);
        return this;
    }

    // ------------------------ DELETE ------------------------
    public SQL delete() {
        validateOperation(Operation.DELETE);
        return this;
    }

    // ------------------------ 通用方法 ------------------------
    public SQL where(Consumer<ConditionGroup> conditions) {
        if (operation == null) {
            throw new IllegalStateException("请先调用 select()、update() 或 delete() 方法");
        }
        if (operation == Operation.INSERT) {
            throw new IllegalStateException("INSERT 语句不能使用 WHERE 子句");
        }
        if (whereState == ClauseState.CALLED) {
            throw new IllegalStateException("WHERE 子句已被调用，不能重复调用");
        }
        whereState = ClauseState.CALLED;
        conditions.accept(whereCondition);
        return this;
    }

    public String toSql() {
        if (operation == null) {
            throw new IllegalStateException("请先调用 select()、insert()、update() 或 delete() 方法");
        }
        switch (operation) {
            case SELECT:
                return buildSelectSql();
            case INSERT:
                return buildInsertSql();
            case UPDATE:
                return buildUpdateSql();
            case DELETE:
                return buildDeleteSql();
            default:
                throw new IllegalArgumentException("不支持的操作类型: " + operation);
        }
    }

    public List<Object> getParameters() {
        switch (operation) {
            case SELECT:
            case DELETE:
                return Stream.concat(whereCondition.getParameters().stream(), havingCondition.getParameters().stream()).collect(Collectors.toList());
            case INSERT:
                return new ArrayList<>(insertValues.values());
            case UPDATE:
                return Stream.concat(updateValues.values().stream(), whereCondition.getParameters().stream()).collect(Collectors.toList());
            default:
                throw new IllegalArgumentException("不支持的操作类型: " + operation);
        }
    }

    // ------------------------ 私有方法 ------------------------
    private void validateOperation(Operation newOperation) {
        if (operation != null) {
            throw new IllegalStateException("不能同时使用 " + operation + " 和 " + newOperation + " 操作");
        }
        this.operation = newOperation;
    }

    private String buildSelectSql() {
        StringBuilder sql = new StringBuilder();
        if (selectFields.isEmpty()) {
            sql.append("SELECT *");
        } else {
            sql.append("SELECT ").append(String.join(", ", selectFields));
        }
        sql.append(" FROM ").append(table);
        if (!whereCondition.isEmpty()) {
            sql.append(" WHERE ").append(whereCondition.toSql());
        }
        // 添加GROUP BY子句
        if (!groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", groupByColumns));
        }
        // 添加 HAVING 子句
        if (!havingCondition.isEmpty()) {
            sql.append(" HAVING ").append(havingCondition.toSql());
        }
        // 添加ORDER BY子句
        if (!orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ");
            StringJoiner orderJoiner = new StringJoiner(", ");
            for (OrderBy order : orderByClauses) {
                orderJoiner.add(order.column + (order.isDesc ? " DESC" : " ASC"));
            }
            sql.append(orderJoiner);
        }
        // 添加LIMIT和OFFSET子句
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }
        return sql.toString();
    }

    private String buildInsertSql() {
        if (insertValues.isEmpty()) {
            throw new IllegalStateException("INSERT 语句需要指定列和值");
        }
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table);

        // 构建列名
        StringJoiner columnsJoiner = new StringJoiner(", ", "(", ")");
        insertValues.keySet().forEach(columnsJoiner::add);
        sql.append(columnsJoiner);

        // 构建占位符
        StringJoiner valuesJoiner = new StringJoiner(", ", " VALUES (", ")");
        insertValues.keySet().forEach(col -> valuesJoiner.add("?"));
        sql.append(valuesJoiner);

        return sql.toString();
    }

    private String buildUpdateSql() {
        if (updateValues.isEmpty()) {
            throw new IllegalStateException("UPDATE 语句需要至少一个 SET 子句");
        }
        StringBuilder sql = new StringBuilder("UPDATE ").append(table).append(" SET ");

        // 构建 SET 子句
        StringJoiner setJoiner = new StringJoiner(", ");
        updateValues.forEach((col, val) -> setJoiner.add(col + " = ?"));
        sql.append(setJoiner);

        // 构建 WHERE 子句
        if (!whereCondition.isEmpty()) {
            sql.append(" WHERE ").append(whereCondition.toSql());
        } else {
            throw new IllegalStateException("UPDATE 语句需要 WHERE 子句");
        }

        return sql.toString();
    }

    private String buildDeleteSql() {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(table);
        if (!whereCondition.isEmpty()) {
            sql.append(" WHERE ").append(whereCondition.toSql());
        } else {
            throw new IllegalStateException("DELETE 语句需要 WHERE 子句");
        }

        return sql.toString();
    }


    // 测试用例
    public static void main(String[] args) {
        // 测试用例 1：嵌套 AND/OR 条件
        SQL sql1 = SQL.table("user")
                .select("id", "birthday")
                .where(i -> i.and(j -> j.eq("name", "李白").eq("status", "alive"))
                        .or(j -> j.eq("name", "杜甫").eq("status", "alive")))
                .orderBy("updated_at").desc()
                .orderBy("id");

        printTestResult(sql1);

        // 测试用例 2：多层嵌套条件
        SQL sql2 = SQL.table("article").select()
                .where(i -> i.and(j -> j.eq("category", "java").like("title", "spring"))
                        .or(j -> j.eq("author", "张三").and(k -> k.lt("views", 1000).ge("comments", 5))))
                .limit(20);

        printTestResult(sql2);

        // 测试用例 3：混合条件
        SQL sql3 = SQL.table("product")
                .select("id", "name", "price")
                .where(i -> i.eq("status", "active")
                        .and(j -> j.gt("price", 100).or(k -> k.like("name", "pro")))
                        .and(j -> j.in("category", Arrays.asList("electronics", "books"))))
                .orderBy("price").desc()
                .limit(10).offset(5);

        printTestResult(sql3);

        // 测试用例 4：复杂 OR 分组
        SQL sql4 = SQL.table("order").select("id", "order_no", "amount", "create_time")
                .where(i -> i.or(j -> j.eq("status", "paid").eq("amount", 1000))
                        .or(j -> j.eq("status", "pending").gt("amount", 5000))
                        .or(j -> j.eq("status", "cancelled").le("create_time", "2023-01-01")))
                .orderBy("create_time").desc();

        printTestResult(sql4);


        // 测试用例 3：混合 like
        SQL sql5 = SQL.table("article").select("title", "spring", "books")
                .where(i -> i.like("title", "spring")
                        .and().leftLike("author", "张")
                        .and().rightLike("category", "java"));

        printTestResult(sql5);

        // 测试用例 4：复杂嵌套 with like
        SQL sql6 = SQL.table("order")//.select("id", "order_no", "amount", "create_time")
                .where(i -> i.and(j -> j.eq("status", "paid").leftLike("order_no", "ORD2023"))
                        .or(j -> j.rightLike("customer_name", "先生").gt("amount", 1000)));

        printTestResult(sql6);
    }

    private static void printTestResult(SQL sql) {
        try {
            System.out.println("SQL: " + sql.toSql());
            System.out.println("Parameters: " + sql.getParameters());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println("------------------------");
    }
}


// 示例实体类
@Table("users")
class User {
    private Long id;

    @Column("username")
    private String name;

    private Integer age;

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
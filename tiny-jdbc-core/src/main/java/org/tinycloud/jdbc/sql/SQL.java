package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

public class SQL {
    private final String table;
    private Operation operation;
    private final List<String> selectFields = new ArrayList<>();
    private final Map<String, Object> insertValues = new LinkedHashMap<>();
    private final Map<String, Object> updateValues = new LinkedHashMap<>();
    private final ConditionGroup whereCondition = new ConditionGroup();
    private final List<OrderBy> orderByClauses = new ArrayList<>();
    private Integer limit;
    private Integer offset;

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


    public <T, R> String getColumnName(TypeFunction<T, R> field) {
        return LambdaUtils.getLambdaColumnName(field);
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
    public final <T, R> SQL select(TypeFunction<T, R>... fields) {
        validateOperation(Operation.SELECT);
        for (TypeFunction<T, ?> field : fields) {
            String columnName = getColumnName(field);
            this.selectFields.add(columnName);
        }
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
    public final <T, R> SQL insert(TypeFunction<T, R>... fields) {
        validateOperation(Operation.INSERT);
        for (TypeFunction<T, ?> field : fields) {
            String columnName = getColumnName(field);
            this.insertValues.put(columnName, null);// 占位
        }
        return this;
    }

    public SQL values(Object... values) {
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

    public <T, R> SQL set(TypeFunction<T, R> field, Object value) {
        if (operation != Operation.UPDATE) {
            throw new IllegalStateException("set() 方法只能在 update() 之后调用");
        }
        String column = getColumnName(field);
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
        if (operation == Operation.INSERT) {
            throw new IllegalStateException("INSERT 语句不能使用 WHERE 子句");
        }
        conditions.accept(whereCondition);
        return this;
    }

    public <T, R> SQL orderBy(TypeFunction<T, R> field) {
        if (operation != Operation.SELECT) {
            throw new IllegalStateException("ORDER BY 只能用于 SELECT 语句");
        }
        String column = getColumnName(field);
        orderByClauses.add(new OrderBy(column, false));
        return this;
    }

    public SQL orderBy(String column) {
        if (operation != Operation.SELECT) {
            throw new IllegalStateException("ORDER BY 只能用于 SELECT 语句");
        }
        orderByClauses.add(new OrderBy(column, false));
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
                return whereCondition.getParameters();
            case INSERT:
                return new ArrayList<>(insertValues.values());
            case UPDATE:
                List<Object> params = new ArrayList<>(updateValues.values());
                params.addAll(whereCondition.getParameters());
                return params;
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
        if (!orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ");
            StringJoiner orderJoiner = new StringJoiner(", ");
            for (OrderBy order : orderByClauses) {
                orderJoiner.add(order.column + (order.isDesc ? " DESC" : " ASC"));
            }
            sql.append(orderJoiner);
        }
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
        // 测试用例 1：INSERT
        SQL insertSql = SQL.table("user")
                .insert("id", "name", "age")
                .values(1, "张三", 25);

        printTestResult(insertSql);

        // 测试用例 2：UPDATE
        SQL updateSql = SQL.table("user")
                .update()
                .set("name", "李四")
                .set("age", 30)
                .where(i -> i.eq("id", 1));

        printTestResult(updateSql);

        // 测试用例 3：DELETE
        SQL deleteSql = SQL.table("user")
                .delete()
                .where(i -> i.eq("id", 1).or(j -> j.eq("name", "测试")));

        printTestResult(deleteSql);

        // 测试用例 4：SELECT with WHERE
        SQL selectSql = SQL.table("user")
                .select("id", "name")
                .where(i -> i.leftLike("name", "张")
                        .and().ge("age", 20)
                        .and().in("status", Arrays.asList("ACTIVE", "PENDING")))
                .orderBy("age").desc()
                .limit(10);

        printTestResult(selectSql);

        // 使用类和方法引用的示例
        SQL selectSql6 = SQL.table(User.class)
                .select(User::getId, User::getName)
                .where(i -> i.eq(User::getAge, 25)
                        .or(j -> j.like(User::getName, "张")))
                .orderBy(User::getId)
                .desc()
                .limit(10);
        printTestResult(selectSql6);

        // 使用类和方法引用的示例
        SQL selectSql7 = SQL.table(User.class)
                .select(User::getId, User::getName)
                .where(i -> i.eq(User::getAge, 25)
                        .or().eq(User::getAge, 30))
                .orderBy(User::getId)
                .desc().orderBy(User::getAge);
        printTestResult(selectSql7);


        // 示例1：使用group()方法添加括号
        SQL selectSql8 = SQL.table("user")
                .select("*")
                .where(i -> i.group(j -> j.eq("age", 25).or().eq("age", 30))
                        .and().like("name", "张"));
        printTestResult(selectSql8);


        // 示例2：嵌套括号
        SQL complexSql9 = SQL.table("user")
                .select("*")
                .where(i -> i.group(j -> j.eq("status", "ACTIVE")
                                .and().group(k -> k.gt("age", 18).and().lt("age", 60)))
                        .or().eq("role", "ADMIN"));
        printTestResult(complexSql9);
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
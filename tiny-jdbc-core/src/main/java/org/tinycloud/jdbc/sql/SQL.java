package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.sql.enums.ClauseState;
import org.tinycloud.jdbc.sql.enums.Operation;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.*;
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
        this.validateOperation(Operation.SELECT);
        this.selectFields.add("*");
        return this;
    }

    public SQL select(String... columns) {
        this.validateOperation(Operation.SELECT);
        this.selectFields.addAll(Arrays.asList(columns));
        return this;
    }

    @SafeVarargs
    public final <T> SQL select(TypeFunction<T, ?>... fields) {
        this.validateOperation(Operation.SELECT);
        for (TypeFunction<T, ?> field : fields) {
            String columnName = LambdaUtils.getLambdaColumnName(field);
            this.selectFields.add(columnName);
        }
        return this;
    }

    public SQL select(Expression... expressions) {
        this.validateOperation(Operation.SELECT);
        for (Expression expr : expressions) {
            this.selectFields.add(expr.toString());
        }
        return this;
    }

    public <T> SQL orderBy(TypeFunction<T, ?> field) {
        if (this.operation != Operation.SELECT) {
            throw new IllegalStateException("ORDER BY 只能用于 SELECT 语句");
        }
        String column = LambdaUtils.getLambdaColumnName(field);
        this.orderByClauses.add(new OrderBy(column, false));
        return this;
    }

    public SQL orderBy(String column) {
        if (this.operation != Operation.SELECT) {
            throw new IllegalStateException("ORDER BY 只能用于 SELECT 语句");
        }
        this.orderByClauses.add(new OrderBy(column, false));
        return this;
    }

    public SQL groupBy(String... columns) {
        if (this.operation != Operation.SELECT) {
            throw new IllegalStateException("GROUP BY 只能用于 SELECT 语句");
        }
        this.groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    @SafeVarargs
    public final <T> SQL groupBy(TypeFunction<T, ?>... fields) {
        if (this.operation != Operation.SELECT) {
            throw new IllegalStateException("GROUP BY 只能用于 SELECT 语句");
        }
        for (TypeFunction<T, ?> field : fields) {
            String column = LambdaUtils.getLambdaColumnName(field);
            this.groupByColumns.add(column);
        }
        return this;
    }

    public SQL having(Consumer<ConditionGroup> conditions) {
        if (this.operation != Operation.SELECT) {
            throw new IllegalStateException("HAVING 子句只能用于 SELECT 语句");
        }
        if (this.havingState == ClauseState.CALLED) {
            throw new IllegalStateException("HAVING 子句已被调用，不能重复调用");
        }
        this.havingState = ClauseState.CALLED;
        conditions.accept(this.havingCondition);
        return this;
    }

    public SQL desc() {
        if (!this.orderByClauses.isEmpty()) {
            OrderBy lastOrder = this.orderByClauses.get(this.orderByClauses.size() - 1);
            this.orderByClauses.set(this.orderByClauses.size() - 1, new OrderBy(lastOrder.getColumn(), true));
        }
        return this;
    }

    public SQL limit(int limit) {
        if (this.operation != Operation.SELECT) {
            throw new IllegalStateException("LIMIT 只能用于 SELECT 语句");
        }
        this.limit = limit;
        return this;
    }

    public SQL offset(int offset) {
        if (this.operation != Operation.SELECT) {
            throw new IllegalStateException("OFFSET 只能用于 SELECT 语句");
        }
        this.offset = offset;
        return this;
    }

    // ------------------------ INSERT ------------------------
    public SQL insert(String... columns) {
        this.validateOperation(Operation.INSERT);
        for (String column : columns) {
            this.insertValues.put(column, null);
        }
        return this;
    }

    @SafeVarargs
    public final <T> SQL insert(TypeFunction<T, ?>... fields) {
        this.validateOperation(Operation.INSERT);
        for (TypeFunction<T, ?> field : fields) {
            String columnName = LambdaUtils.getLambdaColumnName(field);
            this.insertValues.put(columnName, null);// 占位
        }
        return this;
    }

    public SQL values(Object... values) {
        if (this.operation != Operation.INSERT) {
            throw new IllegalStateException("values() 方法只能在 insert() 之后调用");
        }
        if (this.insertValues.isEmpty()) {
            throw new IllegalStateException("请先调用 insert() 方法指定列");
        }
        if (values.length != this.insertValues.size()) {
            throw new IllegalArgumentException("值的数量与列的数量不匹配");
        }
        int i = 0;
        for (String column : this.insertValues.keySet()) {
            this.insertValues.put(column, values[i++]);
        }
        return this;
    }

    // ------------------------ UPDATE ------------------------
    public SQL update() {
        this.validateOperation(Operation.UPDATE);
        return this;
    }

    public SQL set(String column, Object value) {
        if (this.operation != Operation.UPDATE) {
            throw new IllegalStateException("set() 方法只能在 update() 之后调用");
        }
        this.updateValues.put(column, value);
        return this;
    }

    public <T> SQL set(TypeFunction<T, ?> field, Object value) {
        if (this.operation != Operation.UPDATE) {
            throw new IllegalStateException("set() 方法只能在 update() 之后调用");
        }
        String column = LambdaUtils.getLambdaColumnName(field);
        this.updateValues.put(column, value);
        return this;
    }

    // ------------------------ DELETE ------------------------
    public SQL delete() {
        this.validateOperation(Operation.DELETE);
        return this;
    }

    // ------------------------ 通用方法 ------------------------
    public SQL where(Consumer<ConditionGroup> conditions) {
        if (this.operation == null) {
            throw new IllegalStateException("请先调用 select()、update() 或 delete() 方法");
        }
        if (this.operation == Operation.INSERT) {
            throw new IllegalStateException("INSERT 语句不能使用 WHERE 子句");
        }
        if (this.whereState == ClauseState.CALLED) {
            throw new IllegalStateException("WHERE 子句已被调用，不能重复调用");
        }
        this.whereState = ClauseState.CALLED;
        conditions.accept(this.whereCondition);
        return this;
    }

    public String toSql() {
        if (this.operation == null) {
            throw new IllegalStateException("请先调用 select()、insert()、update() 或 delete() 方法");
        }
        switch (this.operation) {
            case SELECT:
                return this.buildSelectSql();
            case INSERT:
                return this.buildInsertSql();
            case UPDATE:
                return this.buildUpdateSql();
            case DELETE:
                return this.buildDeleteSql();
            default:
                throw new IllegalArgumentException("不支持的操作类型: " + this.operation);
        }
    }

    public List<Object> getParameters() {
        switch (this.operation) {
            case SELECT:
            case DELETE:
                return Stream.concat(this.whereCondition.getParameters().stream(), this.havingCondition.getParameters().stream()).collect(Collectors.toList());
            case INSERT:
                return new ArrayList<>(this.insertValues.values());
            case UPDATE:
                return Stream.concat(this.updateValues.values().stream(), this.whereCondition.getParameters().stream()).collect(Collectors.toList());
            default:
                throw new IllegalArgumentException("不支持的操作类型: " + this.operation);
        }
    }

    // ------------------------ 私有方法 ------------------------
    private void validateOperation(Operation newOperation) {
        if (this.operation != null) {
            throw new IllegalStateException("不能同时使用 " + this.operation + " 和 " + newOperation + " 操作");
        }
        this.operation = newOperation;
    }

    private String buildSelectSql() {
        StringBuilder sql = new StringBuilder();
        if (this.selectFields.isEmpty()) {
            sql.append("SELECT *");
        } else {
            sql.append("SELECT ").append(String.join(", ", this.selectFields));
        }
        sql.append(" FROM ").append(this.table);
        if (!this.whereCondition.isEmpty()) {
            sql.append(" WHERE ").append(this.whereCondition.toSql());
        }
        // 添加GROUP BY子句
        if (!this.groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", this.groupByColumns));
        }
        // 添加 HAVING 子句
        if (!this.havingCondition.isEmpty()) {
            sql.append(" HAVING ").append(this.havingCondition.toSql());
        }
        // 添加ORDER BY子句
        if (!this.orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ");
            StringJoiner orderJoiner = new StringJoiner(", ");
            for (OrderBy order : this.orderByClauses) {
                orderJoiner.add(order.getColumn() + (order.isDesc() ? " DESC" : " ASC"));
            }
            sql.append(orderJoiner);
        }
        // 添加LIMIT和OFFSET子句
        if (this.limit != null) {
            sql.append(" LIMIT ").append(this.limit);
        }
        if (this.offset != null) {
            sql.append(" OFFSET ").append(this.offset);
        }
        return sql.toString();
    }

    private String buildInsertSql() {
        if (this.insertValues.isEmpty()) {
            throw new IllegalStateException("INSERT 语句需要指定列和值");
        }
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(this.table);

        // 构建列名
        StringJoiner columnsJoiner = new StringJoiner(", ", "(", ")");
        this.insertValues.keySet().forEach(columnsJoiner::add);
        sql.append(columnsJoiner);

        // 构建占位符
        StringJoiner valuesJoiner = new StringJoiner(", ", " VALUES (", ")");
        this.insertValues.keySet().forEach(col -> valuesJoiner.add("?"));
        sql.append(valuesJoiner);
        return sql.toString();
    }

    private String buildUpdateSql() {
        if (this.updateValues.isEmpty()) {
            throw new IllegalStateException("UPDATE 语句需要至少一个 SET 子句");
        }
        StringBuilder sql = new StringBuilder("UPDATE ").append(this.table).append(" SET ");

        // 构建 SET 子句
        StringJoiner setJoiner = new StringJoiner(", ");
        this.updateValues.forEach((col, val) -> setJoiner.add(col + " = ?"));
        sql.append(setJoiner);

        // 构建 WHERE 子句
        if (!this.whereCondition.isEmpty()) {
            sql.append(" WHERE ").append(this.whereCondition.toSql());
        } else {
            throw new IllegalStateException("UPDATE 语句需要 WHERE 子句");
        }
        return sql.toString();
    }

    private String buildDeleteSql() {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(this.table);
        if (!this.whereCondition.isEmpty()) {
            sql.append(" WHERE ").append(this.whereCondition.toSql());
        } else {
            throw new IllegalStateException("DELETE 语句需要 WHERE 子句");
        }
        return sql.toString();
    }
}



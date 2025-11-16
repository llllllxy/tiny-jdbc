package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.sql.enums.ClauseState;
import org.tinycloud.jdbc.sql.enums.Operation;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *     SQL语句构建器
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:00
 */
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

    /**
     * 创建一个SQL对象，指定表名
     *
     * @param table 表名
     * @return SQL对象
     */
    public static SQL table(String table) {
        return new SQL(table);
    }

    /**
     * 根据实体类创建一个 SQL 对象。
     * 该方法会检查传入的实体类是否包含 @Table 注解，若包含则使用注解中的表名创建 SQL 对象；
     * 若不包含，则抛出 TinyJdbcException 异常。
     *
     * @param entityClass 实体类的 Class 对象，该类应包含 @Table 注解以指定对应的数据库表名
     * @return 一个基于指定表名创建的 SQL 对象
     * @throws TinyJdbcException 当传入的实体类缺少 @Table 注解时抛出此异常
     */
    public static SQL table(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new TinyJdbcException("Class " + entityClass.getName() + " is missing the @Table annotation.");
        }
        return new SQL(tableAnnotation.value());
    }


    // ------------------------ SELECT ------------------------

    /**
     * 构建一个 SELECT * 语句
     *
     * @return 当前 SQL 对象实例，用于支持链式调用。
     */
    public SQL select() {
        this.validateOperation(Operation.SELECT);
        this.selectFields.add("*");
        return this;
    }

    /**
     * 构建一个 SELECT 语句，指定要查询的列，支持链式调用
     *
     * @param columns 要查询的列名，可变参数形式，可传入一个或多个列名
     * @return 当前 SQL 对象实例，用于支持链式调用。
     */
    public SQL select(String... columns) {
        this.validateOperation(Operation.SELECT);
        this.selectFields.addAll(Arrays.asList(columns));
        return this;
    }

    /**
     * 构建一个 SELECT 语句，使用 Lambda 表达式指定要查询的列，支持链式调用。
     * 该方法会将 Lambda 表达式转换为对应的数据库列名，并添加到查询字段列表中。
     *
     * @param <T>    实体类的类型
     * @param fields 一个或多个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @return 当前 SQL 对象实例，用于支持链式调用。
     */
    @SafeVarargs
    public final <T> SQL select(TypeFunction<T, ?>... fields) {
        this.validateOperation(Operation.SELECT);
        for (TypeFunction<T, ?> field : fields) {
            String columnName = LambdaUtils.getLambdaColumnName(field);
            this.selectFields.add(columnName);
        }
        return this;
    }

    /**
     * 构建一个 SELECT 语句，使用 Expression 对象指定要查询的表达式，支持链式调用。
     * 该方法会将传入的 Expression 对象转换为字符串形式，并添加到查询字段列表中。
     *
     * @param expressions 一个或多个 Expression 对象，用于表示 SQL 查询中的表达式
     * @return 当前 SQL 对象实例，用于支持链式调用。
     */
    public SQL select(Expression... expressions) {
        this.validateOperation(Operation.SELECT);
        for (Expression expr : expressions) {
            this.selectFields.add(expr.toString());
        }
        return this;
    }

    /**
     * 为 SELECT 语句添加 ORDER BY 子句，使用 Lambda 表达式指定排序字段，支持链式调用。
     * 该方法会将 Lambda 表达式转换为对应的数据库列名，并将其添加到排序规则列表中，默认按升序排序。
     *
     * @param <T>   实体类的类型
     * @param field 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 如果当前操作不是 SELECT 操作，抛出此异常
     */
    public <T> SQL orderBy(TypeFunction<T, ?> field) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The ORDER BY clause can only be used in SELECT statements.");
        }
        String column = LambdaUtils.getLambdaColumnName(field);
        this.orderByClauses.add(new OrderBy(column, false));
        return this;
    }

    /**
     * 为 SELECT 语句添加 ORDER BY 子句，指定排序字段，支持链式调用。
     * 该方法会将传入的列名添加到排序规则列表中，默认按升序排序。
     *
     * @param column 要进行排序的数据库列名
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 如果当前操作不是 SELECT 操作，抛出此异常
     */
    public SQL orderBy(String column) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The ORDER BY clause can only be used in SELECT statements.");

        }
        this.orderByClauses.add(new OrderBy(column, false));
        return this;
    }

    /**
     * 为 SELECT 语句添加 GROUP BY 子句，指定分组字段，支持链式调用。
     * 该方法会将传入的列名添加到分组字段列表中。
     * 若当前操作不是 SELECT 操作，会抛出 TinyJdbcException 异常。
     *
     * @param columns 要进行分组的数据库列名，可变参数形式，可传入一个或多个列名
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 如果当前操作不是 SELECT 操作，抛出此异常
     */
    public SQL groupBy(String... columns) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The GROUP BY clause can only be used in SELECT statements.");
        }
        this.groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    /**
     * 为 SELECT 语句添加 GROUP BY 子句，使用 Lambda 表达式指定分组字段，支持链式调用。
     * 该方法会将 Lambda 表达式转换为对应的数据库列名，并将这些列名添加到分组字段列表中。
     * 若当前操作不是 SELECT 操作，会抛出 TinyJdbcException 异常。
     *
     * @param <T>    实体类的类型
     * @param fields 一个或多个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 如果当前操作不是 SELECT 操作，抛出此异常
     */
    @SafeVarargs
    public final <T> SQL groupBy(TypeFunction<T, ?>... fields) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The GROUP BY clause can only be used in SELECT statements.");
        }
        for (TypeFunction<T, ?> field : fields) {
            String column = LambdaUtils.getLambdaColumnName(field);
            this.groupByColumns.add(column);
        }
        return this;
    }

    /**
     * 为 SELECT 语句添加 HAVING 子句，支持链式调用。
     * HAVING 子句通常与 GROUP BY 子句一起使用，用于过滤分组后的结果。
     * 该方法会验证当前操作是否为 SELECT 操作，并且 HAVING 子句是否已经被调用过。
     *
     * @param conditions 一个 Consumer 函数式接口，用于配置 HAVING 子句的条件，接收 ConditionGroup 对象作为参数
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 当当前操作不是 SELECT 操作时，抛出此异常，提示 HAVING 子句仅能用于 SELECT 语句；
     *                           当 HAVING 子句已经被调用过时，抛出此异常，提示不能重复调用 HAVING 子句。
     */
    public SQL having(Consumer<ConditionGroup> conditions) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The HAVING clause can only be used in SELECT statements.");
        }
        if (this.havingState == ClauseState.CALLED) {
            throw new TinyJdbcException("The HAVING clause has already been called and cannot be called again.");
        }
        this.havingState = ClauseState.CALLED;
        conditions.accept(this.havingCondition);
        return this;
    }

    /**
     * 将最后一个添加的 ORDER BY 子句设置为降序排序，支持链式调用。
     * 该方法会检查排序规则列表是否为空，如果不为空，则将列表中最后一个排序规则设置为降序。
     *
     * @return 当前 SQL 对象实例，用于支持链式调用。
     */
    public SQL desc() {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The DESC clause can only be used in SELECT statements.");
        }
        if (!this.orderByClauses.isEmpty()) {
            OrderBy lastOrder = this.orderByClauses.get(this.orderByClauses.size() - 1);
            this.orderByClauses.set(this.orderByClauses.size() - 1, new OrderBy(lastOrder.getColumn(), true));
        }
        return this;
    }

    /**
     * 为 SELECT 语句添加 LIMIT 子句，用于限制查询结果的行数，支持链式调用。
     * 该方法会验证当前操作是否为 SELECT 操作，若不是则抛出异常；
     * 若为 SELECT 操作，则设置 LIMIT 子句的限制行数。
     *
     * @param limit 要限制的查询结果的最大行数
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 当当前操作不是 SELECT 操作时，抛出此异常，提示 LIMIT 子句仅能用于 SELECT 语句。
     */
    public SQL limit(int limit) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The LIMIT clause can only be used in SELECT statements.");
        }
        this.limit = limit;
        return this;
    }

    /**
     * 为 SELECT 语句添加 OFFSET 子句，用于指定查询结果的起始偏移量，支持链式调用。
     * OFFSET 子句通常与 LIMIT 子句配合使用，用于实现分页查询。
     * 该方法会验证当前操作是否为 SELECT 操作，若不是则抛出异常；
     * 若为 SELECT 操作，则设置 OFFSET 子句的起始偏移量。
     *
     * @param offset 要跳过的查询结果的行数，即查询结果的起始偏移量
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 当当前操作不是 SELECT 操作时，抛出此异常，提示 OFFSET 子句仅能用于 SELECT 语句。
     */
    public SQL offset(int offset) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The OFFSET clause can only be used in SELECT statements.");
        }
        this.offset = offset;
        return this;
    }

    // ------------------------ INSERT ------------------------

    /**
     * 开始构建一个 INSERT SQL 语句，指定要插入的列，支持链式调用。
     * 该方法会将传入的列名添加到待插入的列集合中，
     * 同时为每个列名对应的值设置为 null 作为占位。
     * 调用此方法后，可继续链式调用 values 方法设置要插入的值。
     *
     * @param columns 要插入的数据库列名，可变参数形式，可传入一个或多个列名
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 当已经设置了其他操作类型，却尝试同时使用 INSERT 操作类型时抛出此异常
     */
    public SQL insert(String... columns) {
        this.validateOperation(Operation.INSERT);
        for (String column : columns) {
            this.insertValues.put(column, null);
        }
        return this;
    }

    /**
     * 开始构建一个 INSERT SQL 语句，使用 Lambda 表达式指定要插入的列，支持链式调用。
     * 该方法会将 Lambda 表达式转换为对应的数据库列名，并将这些列名添加到待插入的列集合中，
     * 同时为每个列名对应的值设置为 null 作为占位。
     * 调用此方法后，可继续链式调用 values 方法设置要插入的值。
     *
     * @param <T>    实体类的类型
     * @param fields 一个或多个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 当已经设置了其他操作类型，却尝试同时使用 INSERT 操作类型时抛出此异常
     */
    @SafeVarargs
    public final <T> SQL insert(TypeFunction<T, ?>... fields) {
        this.validateOperation(Operation.INSERT);
        for (TypeFunction<T, ?> field : fields) {
            String columnName = LambdaUtils.getLambdaColumnName(field);
            this.insertValues.put(columnName, null);// 占位
        }
        return this;
    }

    /**
     * 为 INSERT 语句设置要插入的值，支持链式调用。
     * 该方法会将传入的值按顺序与之前通过 insert() 方法指定的列一一对应。
     * 调用此方法前，必须先调用 insert() 方法指定要插入的列，且传入值的数量必须与列的数量一致。
     *
     * @param values 要插入的值，可变参数形式，可传入一个或多个值
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 当当前操作不是 INSERT 操作，或者未调用 insert() 方法指定列时抛出此异常。当传入值的数量与指定列的数量不匹配时抛出此异常。
     */
    public SQL values(Object... values) {
        if (this.operation != Operation.INSERT) {
            throw new TinyJdbcException("The values() method can only be called after insert().");
        }
        if (this.insertValues.isEmpty()) {
            throw new TinyJdbcException("Please call the insert() method first to specify the columns.");

        }
        if (values.length != this.insertValues.size()) {
            throw new TinyJdbcException("The number of values does not match the number of columns.");
        }
        int i = 0;
        for (String column : this.insertValues.keySet()) {
            this.insertValues.put(column, values[i++]);
        }
        return this;
    }

    // ------------------------ UPDATE ------------------------

    /**
     * 开始构建一个 UPDATE SQL 语句，支持链式调用。
     * 该方法会调用 validateOperation 方法验证并设置当前的 SQL 操作类型为 UPDATE，
     * 确保不会同时使用多种不同类型的 SQL 操作（如 SELECT、INSERT、UPDATE、DELETE）。
     * 调用此方法后，可继续链式调用 set 方法设置要更新的列和值，以及 where 方法添加更新条件。
     *
     * @return 当前 SQL 对象实例，用于支持链式调用后续的方法，如添加 SET 子句、WHERE 子句等。
     * @throws TinyJdbcException 当已经设置了其他操作类型，却尝试同时使用 UPDATE 操作类型时抛出此异常
     */
    public SQL update() {
        this.validateOperation(Operation.UPDATE);
        return this;
    }

    /**
     * 为 UPDATE 语句设置要更新的列及其对应的值，支持链式调用。
     * 该方法直接指定要更新的列名，将列名和对应的值添加到待更新的值映射中。
     *
     * @param column 要更新的数据库列名
     * @param value  要更新到数据库中的值
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 当当前操作不是 UPDATE 操作时，抛出此异常，提示只能在调用 update() 方法后调用此方法。
     */
    public SQL set(String column, Object value) {
        if (this.operation != Operation.UPDATE) {
            throw new TinyJdbcException("The set() method can only be called after update().");
        }
        this.updateValues.put(column, value);
        return this;
    }

    /**
     * 为 UPDATE 语句设置要更新的列及其对应的值，支持链式调用。
     * 该方法使用 Lambda 表达式指定要更新的列，会将 Lambda 表达式转换为对应的数据库列名，
     * 并将列名和对应的值添加到待更新的值映射中。
     *
     * @param <T>   实体类的类型
     * @param field 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param value 要更新到数据库中的值
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 当当前操作不是 UPDATE 操作时，抛出此异常，提示只能在调用 update() 方法后调用此方法。
     */
    public <T> SQL set(TypeFunction<T, ?> field, Object value) {
        if (this.operation != Operation.UPDATE) {
            throw new TinyJdbcException("The set() method can only be called after update().");
        }
        String column = LambdaUtils.getLambdaColumnName(field);
        this.updateValues.put(column, value);
        return this;
    }

    // ------------------------ DELETE ------------------------

    /**
     * 开始构建一个 DELETE SQL 语句，支持链式调用。
     * 该方法会调用 validateOperation 方法验证并设置当前的 SQL 操作类型为 DELETE，
     * 确保不会同时使用多种不同类型的 SQL 操作（如 SELECT、INSERT、UPDATE、DELETE）。
     *
     * @return 当前 SQL 对象实例，用于支持链式调用后续的方法，如添加 WHERE 子句等。
     * @throws TinyJdbcException 当已经设置了其他操作类型，却尝试同时使用 DELETE 操作类型时抛出此异常
     */
    public SQL delete() {
        this.validateOperation(Operation.DELETE);
        return this;
    }

    // ------------------------ 通用方法 ------------------------

    /**
     * 为 SQL 语句添加 WHERE 子句，支持链式调用。
     * 该方法接收一个 Consumer 函数式接口，用于配置 WHERE 子句的条件。
     * 在添加 WHERE 子句前，会进行一系列验证，确保操作类型支持 WHERE 子句且 WHERE 子句未被重复调用。
     *
     * @param conditions 一个 Consumer 函数式接口，用于配置 WHERE 子句的条件，接收 ConditionGroup 对象作为参数
     * @return 当前 SQL 对象实例，用于支持链式调用。
     * @throws TinyJdbcException 当未调用 select(), update(), 或 delete() 方法指定操作类型时抛出此异常；
     *                           当操作类型为 INSERT 时，抛出此异常，因为 INSERT 语句不支持 WHERE 子句；
     *                           当 WHERE 子句已经被调用过时，抛出此异常。
     */
    public SQL where(Consumer<ConditionGroup> conditions) {
        if (this.operation == null) {
            throw new TinyJdbcException("Please call the select(), update(), or delete() method first.");
        }
        if (this.operation == Operation.INSERT) {
            throw new TinyJdbcException("The WHERE clause cannot be used in an INSERT statement.");
        }
        if (this.whereState == ClauseState.CALLED) {
            throw new TinyJdbcException("The WHERE clause has already been called and cannot be called again.");
        }
        this.whereState = ClauseState.CALLED;
        conditions.accept(this.whereCondition);
        return this;
    }

    /**
     * 根据当前设置的操作类型构建并返回对应的 SQL 语句。
     * 该方法会检查是否已经调用了 select(), insert(), update(), 或 delete() 方法来指定操作类型，
     * 若未指定则抛出异常。根据不同的操作类型，调用相应的私有构建方法生成 SQL 语句。
     *
     * @return 生成的 SQL 语句字符串
     * @throws TinyJdbcException 当未调用 select(), insert(), update(), 或 delete() 方法指定操作类型时抛出此异常
     */
    public String toSql() {
        if (this.operation == null) {
            throw new TinyJdbcException("Please call the select(), insert(), update(), or delete() method first.");
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
                throw new TinyJdbcException("Unsupported operation type: " + this.operation);
        }
    }

    /**
     * 获取当前 SQL 操作对应的参数列表。
     * 根据不同的 SQL 操作类型（SELECT、INSERT、UPDATE、DELETE），从不同的数据源收集参数。
     *
     * @return 包含当前 SQL 操作所需参数的列表
     * @throws TinyJdbcException 当操作类型不支持时抛出此异常
     */
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
                throw new TinyJdbcException("Unsupported operation type: " + this.operation);
        }
    }

    // ------------------------ 私有方法 ------------------------

    /**
     * 验证并设置当前的 SQL 操作类型。
     * 该方法用于确保在构建 SQL 语句时，不会同时使用多种不同类型的操作（如 SELECT、INSERT、UPDATE、DELETE）。
     * 如果已经设置了操作类型，再次调用此方法传入不同的操作类型时，会抛出 TinyJdbcException 异常。
     *
     * @param newOperation 新的 SQL 操作类型，由 Operation 枚举定义
     * @throws TinyJdbcException 当已经设置了操作类型，却尝试同时使用另一种操作类型时抛出此异常
     */
    private void validateOperation(Operation newOperation) {
        if (this.operation != null) {
            throw new TinyJdbcException("Cannot use " + this.operation + " and " + newOperation + " operations simultaneously.");
        }
        this.operation = newOperation;
    }

    /**
     * 构建 SELECT SQL 语句。
     * 该方法会根据已设置的查询字段、条件、分组、排序、分页等信息，
     * 构建一个完整的 SELECT SQL 语句。
     *
     * @return 生成的 SELECT SQL 语句字符串
     */
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
        // 添加 GROUP BY 子句
        if (!this.groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", this.groupByColumns));
        }
        // 添加 HAVING 子句
        if (!this.havingCondition.isEmpty()) {
            sql.append(" HAVING ").append(this.havingCondition.toSql());
        }
        // 添加 ORDER BY 子句
        if (!this.orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ");
            StringJoiner orderJoiner = new StringJoiner(", ");
            for (OrderBy order : this.orderByClauses) {
                orderJoiner.add(order.getColumn() + (order.isDesc() ? " DESC" : " ASC"));
            }
            sql.append(orderJoiner);
        }
        // 添加 LIMIT 和 OFFSET 子句
        if (this.limit != null) {
            sql.append(" LIMIT ").append(this.limit);
        }
        if (this.offset != null) {
            sql.append(" OFFSET ").append(this.offset);
        }
        return sql.toString();
    }

    /**
     * 构建 INSERT SQL 语句。
     * 该方法会验证是否指定了插入的列和对应的值，若未指定则抛出异常。
     * 随后会构建包含列名和占位符的 INSERT SQL 语句。
     *
     * @return 生成的 INSERT SQL 语句字符串
     * @throws TinyJdbcException 当没有指定插入的列和值时抛出此异常
     */
    private String buildInsertSql() {
        if (this.insertValues.isEmpty()) {
            throw new TinyJdbcException("The INSERT statement requires columns and values to be specified.");
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

    /**
     * 构建 UPDATE SQL 语句。
     * 该方法会验证是否设置了至少一个 SET 子句，以及是否包含 WHERE 子句，
     * 确保不会执行无限制的更新操作。
     *
     * @return 生成的 UPDATE SQL 语句字符串
     * @throws TinyJdbcException 当没有设置 SET 子句或 WHERE 子句时抛出此异常
     */
    private String buildUpdateSql() {
        if (this.updateValues.isEmpty()) {
            throw new TinyJdbcException("The UPDATE statement requires at least one SET clause.");
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
            throw new TinyJdbcException("The UPDATE statement requires a WHERE clause.");
        }
        return sql.toString();
    }

    /**
     * 构建 DELETE SQL 语句。
     * 该方法会生成一条用于从指定表中删除数据的 SQL 语句，
     * 为避免误删全量数据，要求必须包含 WHERE 子句。
     *
     * @return 生成的 DELETE SQL 语句字符串
     * @throws TinyJdbcException 当没有设置 WHERE 子句时抛出此异常
     */
    private String buildDeleteSql() {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(this.table);
        if (!this.whereCondition.isEmpty()) {
            sql.append(" WHERE ").append(this.whereCondition.toSql());
        } else {
            throw new TinyJdbcException("The DELETE statement requires a WHERE clause.");
        }
        return sql.toString();
    }
}

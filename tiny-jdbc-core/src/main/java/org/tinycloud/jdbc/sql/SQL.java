package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.sql.enums.ClauseState;
import org.tinycloud.jdbc.sql.enums.Operation;
import org.tinycloud.jdbc.sql.enums.SqlJoinType;
import org.tinycloud.jdbc.util.LambdaUtils;
import org.tinycloud.jdbc.util.SqlIdentifierUtils;

import java.util.*;
import java.util.function.Consumer;

/**
 * <p>
 * SQL语句构建器
 * </p>
 * <p>
 * 支持 SELECT / INSERT / UPDATE / DELETE / REPLACE 等操作，以及子查询（from/in/exists）、
 * 真实 JOIN、SQL 函数（{@link FuncBuilder}）、别名、distinct、行锁等能力。
 * 最终通过 {@link #toSql()} 与 {@link #getParameters()} 交付给执行层。
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:00
 */
public class SQL<T> {
    private final String table;
    private String tableAlias;
    private Operation operation;
    private final List<SelectItem> selectItems = new ArrayList<>();
    private final List<String> insertColumns = new ArrayList<>();
    private final List<List<Object>> insertRows = new ArrayList<>();
    private final Map<String, Object> updateValues = new LinkedHashMap<>();
    private final ConditionGroup<T> whereCondition = new ConditionGroup<>();
    private final List<OrderBy> orderByClauses = new ArrayList<>();
    private final List<String> groupByColumns = new ArrayList<>();
    private final ConditionGroup<T> havingCondition = new ConditionGroup<>();
    private Integer limit;
    private Integer offset;

    // 子查询 FROM（派生表）
    private SQL<?> subQueryFrom;
    private String fromAsAlias;

    // JOIN
    private final List<Join> joins = new ArrayList<>();

    // select distinct
    private boolean distinct;

    // 行锁 / 其它
    private String lockClause;

    // INSERT 模式与 ON DUPLICATE KEY UPDATE
    private String insertMode = "INSERT";
    private final Map<String, Object> onDupUpdate = new LinkedHashMap<>();
    private final List<String> onDupUpdateValues = new ArrayList<>();

    // UNION / UNION ALL 管道（为 null 表示独立 SQL；共享同一列表则属于同一管道）
    private List<SQL<?>> unionParts;
    private String unionType;

    // 记录各子句的调用状态
    private volatile ClauseState whereState = ClauseState.NOT_CALLED;
    private volatile ClauseState havingState = ClauseState.NOT_CALLED;

    private SQL(String table) {
        SqlIdentifierUtils.checkTableName(table);
        this.table = table;
    }

    private SQL(String table, String alias) {
        SqlIdentifierUtils.checkTableName(table);
        this.table = table;
        this.tableAlias = alias;
    }

    public static <T> SQL<T> table(String table) {
        return new SQL<>(table);
    }

    public static <T> SQL<T> table(String table, String alias) {
        return new SQL<>(table, alias);
    }

    public static <T> SQL<T> table(Class<T> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new TinyJdbcException("Class " + entityClass.getName() + " is missing the @Table annotation.");
        }
        return new SQL<>(tableAnnotation.value());
    }

    public static <T> SQL<T> table(Class<T> entityClass, String alias) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new TinyJdbcException("Class " + entityClass.getName() + " is missing the @Table annotation.");
        }
        return new SQL<>(tableAnnotation.value(), alias);
    }

    /**
     * 构造一段受信任的原始 SQL 标记。
     * <p>
     * 例如 {@code SQL.raw("FOR UPDATE")}。请务必仅用受信、常量内容包裹；
     * 不要在未经 {@link RawSql#wrap(String)} 显式授权时传入不受控输入。
     * </p>
     *
     * @param sql 原始 SQL 片段
     * @return 受信 SQL 标记
     */
    public static RawSql raw(String sql) {
        return RawSql.wrap(sql);
    }

    // ------------------------ SELECT ------------------------

    public SQL<T> select() {
        this.validateOperation(Operation.SELECT);
        this.selectItems.add(new SelectItem("*", Collections.emptyList()));
        return this;
    }

    public SQL<T> select(String... columns) {
        this.validateOperation(Operation.SELECT);
        for (String column : columns) {
            SqlIdentifierUtils.checkColumnRef(column);
            this.selectItems.add(new SelectItem(column, Collections.emptyList()));
        }
        return this;
    }

    @SafeVarargs
    public final SQL<T> select(TypeFunction<T, ?>... fields) {
        this.validateOperation(Operation.SELECT);
        for (TypeFunction<T, ?> field : fields) {
            this.selectItems.add(new SelectItem(LambdaUtils.getLambdaColumnName(field), Collections.emptyList()));
        }
        return this;
    }

    /**
     * 使用 SQL 函数表达式作为查询字段（支持嵌套与别名）。
     */
    public SQL<T> select(FuncExpr... expressions) {
        this.validateOperation(Operation.SELECT);
        for (FuncExpr expr : expressions) {
            this.selectItems.add(new SelectItem(expr.toSql(), expr.getParameters()));
        }
        return this;
    }

    public SQL<T> selectDistinct(Object... columns) {
        this.validateOperation(Operation.SELECT);
        this.distinct = true;
        if (columns == null || columns.length == 0) {
            this.selectItems.add(new SelectItem("*", Collections.emptyList()));
        } else {
            for (Object col : columns) {
                String column = String.valueOf(col);
                SqlIdentifierUtils.checkColumnRef(column);
                this.selectItems.add(new SelectItem(column, Collections.emptyList()));
            }
        }
        return this;
    }

    @SafeVarargs
    public final SQL<T> selectDistinct(TypeFunction<T, ?>... fields) {
        this.validateOperation(Operation.SELECT);
        this.distinct = true;
        for (TypeFunction<T, ?> field : fields) {
            this.selectItems.add(new SelectItem(LambdaUtils.getLambdaColumnName(field), Collections.emptyList()));
        }
        return this;
    }

    public SQL<T> selectDistinct(FuncExpr... expressions) {
        this.validateOperation(Operation.SELECT);
        this.distinct = true;
        for (FuncExpr expr : expressions) {
            this.selectItems.add(new SelectItem(expr.toSql(), expr.getParameters()));
        }
        return this;
    }

    // ------------------------ 子查询 FROM ------------------------

    public SQL<T> from(SQL<?> subQuery) {
        return from(subQuery, null);
    }

    public SQL<T> from(SQL<?> subQuery, String alias) {
        if (this.operation != null && this.operation != Operation.SELECT) {
            throw new TinyJdbcException("FROM sub-query can only be used in SELECT statements.");
        }
        if (subQuery == null) {
            throw new TinyJdbcException("Sub-query cannot be null.");
        }
        this.operation = Operation.SELECT;
        this.subQueryFrom = subQuery;
        if (alias != null && !alias.trim().isEmpty()) {
            SqlIdentifierUtils.checkAlias(alias);
        }
        this.fromAsAlias = alias;
        return this;
    }

    // ------------------------ JOIN ------------------------

    public SQL<T> leftJoin(String table, String alias) {
        return join(SqlJoinType.LEFT, table, alias);
    }

    public SQL<T> leftJoin(String table, String alias, Consumer<Join> joinConsumer) {
        return join(SqlJoinType.LEFT, table, alias, joinConsumer);
    }

    public SQL<T> rightJoin(String table, String alias) {
        return join(SqlJoinType.RIGHT, table, alias);
    }

    public SQL<T> rightJoin(String table, String alias, Consumer<Join> joinConsumer) {
        return join(SqlJoinType.RIGHT, table, alias, joinConsumer);
    }

    public SQL<T> innerJoin(String table, String alias) {
        return join(SqlJoinType.INNER, table, alias);
    }

    public SQL<T> innerJoin(String table, String alias, Consumer<Join> joinConsumer) {
        return join(SqlJoinType.INNER, table, alias, joinConsumer);
    }

    public SQL<T> crossJoin(String table, String alias) {
        return join(SqlJoinType.CROSS, table, alias);
    }

    // ------ JOIN 表名支持实体类（通过 @Table 解析），ON 条件可用 Lambda ------

    public SQL<T> leftJoin(Class<?> entityClass, String alias) {
        return join(SqlJoinType.LEFT, resolveTable(entityClass), alias);
    }

    public SQL<T> leftJoin(Class<?> entityClass, String alias, Consumer<Join> joinConsumer) {
        return join(SqlJoinType.LEFT, resolveTable(entityClass), alias, joinConsumer);
    }

    public SQL<T> rightJoin(Class<?> entityClass, String alias) {
        return join(SqlJoinType.RIGHT, resolveTable(entityClass), alias);
    }

    public SQL<T> rightJoin(Class<?> entityClass, String alias, Consumer<Join> joinConsumer) {
        return join(SqlJoinType.RIGHT, resolveTable(entityClass), alias, joinConsumer);
    }

    public SQL<T> innerJoin(Class<?> entityClass, String alias) {
        return join(SqlJoinType.INNER, resolveTable(entityClass), alias);
    }

    public SQL<T> innerJoin(Class<?> entityClass, String alias, Consumer<Join> joinConsumer) {
        return join(SqlJoinType.INNER, resolveTable(entityClass), alias, joinConsumer);
    }

    public SQL<T> crossJoin(Class<?> entityClass, String alias) {
        return join(SqlJoinType.CROSS, resolveTable(entityClass), alias);
    }

    private static String resolveTable(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new TinyJdbcException("Class " + entityClass.getName() + " is missing the @Table annotation.");
        }
        return tableAnnotation.value();
    }

    private SQL<T> join(SqlJoinType type, String table, String alias) {
        this.ensureSelectOperation();
        this.joins.add(new Join(table, alias, type));
        return this;
    }

    private SQL<T> join(SqlJoinType type, String table, String alias, Consumer<Join> joinConsumer) {
        this.ensureSelectOperation();
        Join join = new Join(table, alias, type);
        joinConsumer.accept(join);
        this.joins.add(join);
        return this;
    }

    private void ensureSelectOperation() {
        if (this.operation == null) {
            this.operation = Operation.SELECT;
        } else if (this.operation != Operation.SELECT && this.operation != Operation.UPDATE) {
            throw new TinyJdbcException("JOIN can only be used in SELECT or UPDATE statements.");
        }
    }

    /**
     * 为最后一个 JOIN 添加列到列连接条件。
     */
    public SQL<T> on(String field1, String field2) {
        lastJoin().on(field1, field2);
        return this;
    }

    /**
     * 为最后一个 JOIN 添加带操作符的 ON 条件。
     */
    public SQL<T> on(String field1, String opt, Object value) {
        lastJoin().on(field1, opt, value);
        return this;
    }

    private Join lastJoin() {
        if (this.joins.isEmpty()) {
            throw new TinyJdbcException("No JOIN to add ON condition to.");
        }
        return this.joins.get(this.joins.size() - 1);
    }

    public SQL<T> forUpdate() {
        this.lockClause = "FOR UPDATE";
        return this;
    }

    public SQL<T> lockInShareMode() {
        this.lockClause = "LOCK IN SHARE MODE";
        return this;
    }

    // ------------------------ 排序 / 分组 / 分页 ------------------------

    public SQL<T> orderBy(TypeFunction<T, ?> field) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The ORDER BY clause can only be used in SELECT statements.");
        }
        String column = LambdaUtils.getLambdaColumnName(field);
        this.orderByClauses.add(new OrderBy(column, false));
        return this;
    }

    public SQL<T> orderBy(String column) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The ORDER BY clause can only be used in SELECT statements.");
        }
        SqlIdentifierUtils.checkColumnRef(column);
        this.orderByClauses.add(new OrderBy(column, false));
        return this;
    }

    public SQL<T> groupBy(String... columns) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The GROUP BY clause can only be used in SELECT statements.");
        }
        for (String column : columns) {
            SqlIdentifierUtils.checkColumnRef(column);
        }
        this.groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    @SafeVarargs
    public final SQL<T> groupBy(TypeFunction<T, ?>... fields) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The GROUP BY clause can only be used in SELECT statements.");
        }
        for (TypeFunction<T, ?> field : fields) {
            String column = LambdaUtils.getLambdaColumnName(field);
            this.groupByColumns.add(column);
        }
        return this;
    }

    public SQL<T> having(Consumer<ConditionGroup<T>> conditions) {
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

    public SQL<T> desc() {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The DESC clause can only be used in SELECT statements.");
        }
        if (!this.orderByClauses.isEmpty()) {
            OrderBy lastOrder = this.orderByClauses.get(this.orderByClauses.size() - 1);
            this.orderByClauses.set(this.orderByClauses.size() - 1, new OrderBy(lastOrder.getColumn(), true));
        }
        return this;
    }

    public SQL<T> limit(int limit) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The LIMIT clause can only be used in SELECT statements.");
        }
        this.limit = limit;
        return this;
    }

    public SQL<T> offset(int offset) {
        if (this.operation != Operation.SELECT) {
            throw new TinyJdbcException("The OFFSET clause can only be used in SELECT statements.");
        }
        this.offset = offset;
        return this;
    }

    // ------------------------ INSERT ------------------------

    public SQL<T> insert(String... columns) {
        this.validateOperation(Operation.INSERT);
        insertMode = "INSERT";
        for (String column : columns) {
            SqlIdentifierUtils.checkColumnName(column);
        }
        Collections.addAll(this.insertColumns, columns);
        return this;
    }

    public SQL<T> insertInto(String... columns) {
        return this.insert(columns);
    }

    public SQL<T> insertIgnoreInto(String... columns) {
        this.validateOperation(Operation.INSERT);
        insertMode = "INSERT IGNORE";
        for (String column : columns) {
            SqlIdentifierUtils.checkColumnName(column);
        }
        Collections.addAll(this.insertColumns, columns);
        return this;
    }

    public SQL<T> replaceInto(String... columns) {
        this.validateOperation(Operation.INSERT);
        insertMode = "REPLACE";
        for (String column : columns) {
            SqlIdentifierUtils.checkColumnName(column);
        }
        Collections.addAll(this.insertColumns, columns);
        return this;
    }

    @SafeVarargs
    public final SQL<T> insert(TypeFunction<T, ?>... fields) {
        this.validateOperation(Operation.INSERT);
        for (TypeFunction<T, ?> field : fields) {
            String columnName = LambdaUtils.getLambdaColumnName(field);
            this.insertColumns.add(columnName);
        }
        return this;
    }

    @SafeVarargs
    public final SQL<T> insertInto(TypeFunction<T, ?>... fields) {
        return this.insert(fields);
    }

    @SafeVarargs
    public final SQL<T> insertIgnoreInto(TypeFunction<T, ?>... fields) {
        this.validateOperation(Operation.INSERT);
        insertMode = "INSERT IGNORE";
        for (TypeFunction<T, ?> field : fields) {
            this.insertColumns.add(LambdaUtils.getLambdaColumnName(field));
        }
        return this;
    }

    @SafeVarargs
    public final SQL<T> replaceInto(TypeFunction<T, ?>... fields) {
        this.validateOperation(Operation.INSERT);
        insertMode = "REPLACE";
        for (TypeFunction<T, ?> field : fields) {
            this.insertColumns.add(LambdaUtils.getLambdaColumnName(field));
        }
        return this;
    }

    /**
     * MySQL 专有：INSERT ... ON DUPLICATE KEY UPDATE col = ?（右侧为值，参数化）。
     */
    public SQL<T> onDuplicateKeyUpdate(String column, Object value) {
        this.requireInsertOperation("onDuplicateKeyUpdate");
        SqlIdentifierUtils.checkColumnName(column);
        this.onDupUpdate.put(column, value);
        return this;
    }

    /**
     * MySQL 专有：INSERT ... ON DUPLICATE KEY UPDATE col = ?（Lambda 列，右侧为值，参数化）。
     */
    public SQL<T> onDuplicateKeyUpdate(TypeFunction<T, ?> field, Object value) {
        this.requireInsertOperation("onDuplicateKeyUpdate");
        this.onDupUpdate.put(LambdaUtils.getLambdaColumnName(field), value);
        return this;
    }

    /**
     * MySQL 专有：INSERT ... ON DUPLICATE KEY UPDATE col = VALUES(col)（无参数）。
     */
    public SQL<T> onDuplicateKeyUpdateValues(String... columns) {
        this.requireInsertOperation("onDuplicateKeyUpdateValues");
        for (String column : columns) {
            SqlIdentifierUtils.checkColumnName(column);
        }
        Collections.addAll(this.onDupUpdateValues, columns);
        return this;
    }

    /**
     * MySQL 专有：INSERT ... ON DUPLICATE KEY UPDATE col = VALUES(col)（Lambda 列，无参数）。
     */
    @SafeVarargs
    public final SQL<T> onDuplicateKeyUpdateValues(TypeFunction<T, ?>... fields) {
        this.requireInsertOperation("onDuplicateKeyUpdateValues");
        for (TypeFunction<T, ?> field : fields) {
            this.onDupUpdateValues.add(LambdaUtils.getLambdaColumnName(field));
        }
        return this;
    }

    private void requireInsertOperation(String method) {
        if (this.operation != Operation.INSERT) {
            throw new TinyJdbcException("The " + method + "() method can only be called after insert().");
        }
    }

    /**
     * 添加一行值。可多次调用以实现多行插入；每次调用值的数量必须与列的数量一致。
     */
    public SQL<T> values(Object... rowValues) {
        if (this.operation != Operation.INSERT) {
            throw new TinyJdbcException("The values() method can only be called after insert().");
        }
        if (this.insertColumns.isEmpty()) {
            throw new TinyJdbcException("Please call the insert() method first to specify the columns.");
        }
        if (rowValues.length != this.insertColumns.size()) {
            throw new TinyJdbcException("The number of values does not match the number of columns.");
        }
        this.insertRows.add(Arrays.asList(rowValues));
        return this;
    }

    // ------------------------ UPDATE ------------------------

    public SQL<T> update() {
        this.validateOperation(Operation.UPDATE);
        return this;
    }

    public SQL<T> set(String column, Object value) {
        if (this.operation != Operation.UPDATE) {
            throw new TinyJdbcException("The set() method can only be called after update().");
        }
        SqlIdentifierUtils.checkColumnRef(column);
        this.updateValues.put(column, value);
        return this;
    }

    public SQL<T> set(TypeFunction<T, ?> field, Object value) {
        if (this.operation != Operation.UPDATE) {
            throw new TinyJdbcException("The set() method can only be called after update().");
        }
        String column = LambdaUtils.getLambdaColumnName(field);
        this.updateValues.put(column, value);
        return this;
    }

    /**
     * 子查询赋值：UPDATE ... SET col = (SELECT ...)。
     */
    public SQL<T> set(String column, SQL<?> subQuery) {
        if (this.operation != Operation.UPDATE) {
            throw new TinyJdbcException("The set() method can only be called after update().");
        }
        SqlIdentifierUtils.checkColumnRef(column);
        this.updateValues.put(column, subQuery);
        return this;
    }

    /**
     * 子查询赋值：UPDATE ... SET col = (SELECT ...)。
     */
    public SQL<T> set(TypeFunction<T, ?> field, SQL<?> subQuery) {
        if (this.operation != Operation.UPDATE) {
            throw new TinyJdbcException("The set() method can only be called after update().");
        }
        String column = LambdaUtils.getLambdaColumnName(field);
        this.updateValues.put(column, subQuery);
        return this;
    }

    // ------------------------ DELETE ------------------------

    public SQL<T> delete() {
        this.validateOperation(Operation.DELETE);
        return this;
    }

    // ------------------------ 条件（子查询等） ------------------------

    public SQL<T> where(Consumer<ConditionGroup<T>> conditions) {
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

    public SQL<T> in(String column, SQL<?> subQuery) {
        this.ensureWhereAvailable();
        this.whereCondition.in(column, subQuery);
        return this;
    }

    public SQL<T> notIn(String column, SQL<?> subQuery) {
        this.ensureWhereAvailable();
        this.whereCondition.notIn(column, subQuery);
        return this;
    }

    public SQL<T> exists(SQL<?> subQuery) {
        this.ensureWhereAvailable();
        this.whereCondition.exists(subQuery);
        return this;
    }

    public SQL<T> notExists(SQL<?> subQuery) {
        this.ensureWhereAvailable();
        this.whereCondition.notExists(subQuery);
        return this;
    }

    private void ensureWhereAvailable() {
        if (this.operation == null) {
            this.operation = Operation.SELECT;
        }
        if (this.operation == Operation.INSERT) {
            throw new TinyJdbcException("Sub-query condition cannot be used in an INSERT statement.");
        }
    }

    // ------------------------ UNION ------------------------

    /**
     * UNION 拼接一个后续 SELECT 片段。同管道所有片段共享 unionParts，渲染时按序拼接。
     * 返回 this，便于继续链式调用。
     */
    public SQL<T> union(SQL<?> next) {
        return mergeUnion("UNION", next);
    }

    /**
     * UNION ALL 拼接一个后续 SELECT 片段。
     */
    public SQL<T> unionAll(SQL<?> next) {
        return mergeUnion("UNION ALL", next);
    }

    private SQL<T> mergeUnion(String type, SQL<?> next) {
        if (next == null) {
            throw new TinyJdbcException("union()/unionAll() requires a non-null SQL segment.");
        }
        List<SQL<?>> pipeline;
        if (this.unionParts == null) {
            pipeline = new ArrayList<>();
            pipeline.add(this);
            this.unionParts = pipeline;
        } else {
            pipeline = this.unionParts;
        }
        next.unionType = type;
        next.unionParts = pipeline;
        pipeline.add(next);
        return this;
    }

    // ------------------------ 渲染 ------------------------

    public String toSql() {
        return render().sql();
    }

    public List<Object> getParameters() {
        return render().parameters();
    }

    private RenderedSql render() {
        // UNION 管道：遍历所有片段，按序拼接；普通（无 union）SQL 仅渲染自身
        List<SQL<?>> pipeline = (this.unionParts == null) ? Collections.<SQL<?>>singletonList(this) : this.unionParts;
        boolean isUnion = pipeline.size() > 1;
        RenderedSql r = new RenderedSql();
        for (int i = 0; i < pipeline.size(); i++) {
            SQL<?> segment = pipeline.get(i);
            if (i > 0) {
                r.appendRaw(" ").appendRaw(segment.unionType).appendRaw(" ");
            }
            // union 时各片段不输出整体子句（ORDER BY / LIMIT / OFFSET / 行锁），由主查询统一追加
            RenderedSql single = segment.renderSingle(!isUnion);
            r.appendRaw(single.sql());
            r.params.addAll(single.parameters());
        }
        if (isUnion) {
            // 排序 / 分页 / 行锁作用于整个 UNION 结果
            appendTailClause(r);
        }
        return r;
    }

    private RenderedSql renderSingle() {
        return renderSingle(true);
    }

    private RenderedSql renderSingle(boolean includeTail) {
        if (this.operation == null) {
            throw new TinyJdbcException("Please call the select(), insert(), update(), or delete() method first.");
        }
        switch (this.operation) {
            case SELECT:
                return renderSelect(includeTail);
            case INSERT:
                return renderInsert();
            case UPDATE:
                return renderUpdate();
            case DELETE:
                return renderDelete();
            default:
                throw new TinyJdbcException("Unsupported operation type: " + this.operation);
        }
    }

    private RenderedSql renderSelect(boolean includeTail) {
        RenderedSql r = new RenderedSql();
        r.appendRaw("SELECT ");
        if (this.distinct) {
            r.appendRaw("DISTINCT ");
        }
        if (this.selectItems.isEmpty()) {
            r.appendRaw("*");
        } else {
            for (int i = 0; i < this.selectItems.size(); i++) {
                if (i > 0) {
                    r.appendRaw(", ");
                }
                SelectItem item = this.selectItems.get(i);
                r.appendRaw(item.sql);
                r.params.addAll(item.params);
            }
        }
        // FROM
        r.appendRaw(" FROM ");
        if (this.subQueryFrom != null) {
            r.appendRaw("(");
            r.appendRaw(this.subQueryFrom.toSql());
            r.appendRaw(")");
            if (this.fromAsAlias != null && !this.fromAsAlias.trim().isEmpty()) {
                r.appendRaw(" ").appendRaw(this.fromAsAlias);
            } else {
                r.appendRaw(" ").appendRaw(this.subQueryFromTableAlias());
            }
            r.params.addAll(this.subQueryFrom.getParameters());
        } else {
            r.appendRaw(this.table);
            if (this.tableAlias != null && !this.tableAlias.trim().isEmpty()) {
                r.appendRaw(" ").appendRaw(this.tableAlias);
            }
        }
        // JOIN
        for (Join join : this.joins) {
            r.appendRaw(join.toSql());
            r.params.addAll(join.getParameters());
        }
        // WHERE
        if (!this.whereCondition.isEmpty()) {
            r.appendRaw(" WHERE ");
            r.appendRaw(this.whereCondition.toSql());
            r.params.addAll(this.whereCondition.getParameters());
        }
        // GROUP BY
        if (!this.groupByColumns.isEmpty()) {
            r.appendRaw(" GROUP BY ").appendRaw(String.join(", ", this.groupByColumns));
        }
        // HAVING
        if (!this.havingCondition.isEmpty()) {
            r.appendRaw(" HAVING ");
            r.appendRaw(this.havingCondition.toSql());
            r.params.addAll(this.havingCondition.getParameters());
        }
        if (includeTail) {
            appendTailClause(r);
        }
        return r;
    }

    /**
     * 追加"整体子句"（ORDER BY / LIMIT / OFFSET / 行锁）。普通 SELECT 在末尾输出；
     * UNION 时由主查询在所有片段拼接完成后统一调用，作用于整个 UNION 结果。
     */
    private void appendTailClause(RenderedSql r) {
        // ORDER BY
        if (!this.orderByClauses.isEmpty()) {
            r.appendRaw(" ORDER BY ");
            StringJoiner orderJoiner = new StringJoiner(", ");
            for (OrderBy order : this.orderByClauses) {
                orderJoiner.add(order.getColumn() + (order.isDesc() ? " DESC" : " ASC"));
            }
            r.appendRaw(orderJoiner.toString());
        }
        // LIMIT / OFFSET
        if (this.limit != null) {
            r.appendRaw(" LIMIT ").appendRaw(String.valueOf(this.limit));
        }
        if (this.offset != null) {
            r.appendRaw(" OFFSET ").appendRaw(String.valueOf(this.offset));
        }
        // 行锁
        if (this.lockClause != null) {
            r.appendRaw(" ").appendRaw(this.lockClause);
        }
    }

    private String subQueryFromTableAlias() {
        return fromAsAlias != null ? fromAsAlias : "t";
    }

    private RenderedSql renderInsert() {
        if (this.insertColumns.isEmpty() || this.insertRows.isEmpty()) {
            throw new TinyJdbcException("The INSERT statement requires columns and values to be specified.");
        }
        RenderedSql r = new RenderedSql();
        r.appendRaw(this.insertMode + " INTO " + this.table + " (");
        r.appendRaw(String.join(", ", this.insertColumns));
        r.appendRaw(") VALUES ");
        for (int i = 0; i < this.insertRows.size(); i++) {
            if (i > 0) {
                r.appendRaw(", ");
            }
            List<Object> row = this.insertRows.get(i);
            r.appendRaw("(");
            for (int j = 0; j < row.size(); j++) {
                if (j > 0) {
                    r.appendRaw(", ");
                }
                r.appendRaw("?");
            }
            r.appendRaw(")");
            r.params.addAll(row);
        }
        // ON DUPLICATE KEY UPDATE
        renderOnDuplicateKey(r);
        return r;
    }

    private void renderOnDuplicateKey(RenderedSql r) {
        if (this.onDupUpdate.isEmpty() && this.onDupUpdateValues.isEmpty()) {
            return;
        }
        r.appendRaw(" ON DUPLICATE KEY UPDATE ");
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, Object> entry : this.onDupUpdate.entrySet()) {
            joiner.add(entry.getKey() + " = ?");
            r.params.add(entry.getValue());
        }
        for (String column : this.onDupUpdateValues) {
            joiner.add(column + " = VALUES(" + column + ")");
        }
        r.appendRaw(joiner.toString());
    }

    @SuppressWarnings("unchecked")
    private RenderedSql renderUpdate() {
        if (this.updateValues.isEmpty()) {
            throw new TinyJdbcException("The UPDATE statement requires at least one SET clause.");
        }
        RenderedSql r = new RenderedSql();
        r.appendRaw("UPDATE " + this.table);
        if (this.tableAlias != null && !this.tableAlias.trim().isEmpty()) {
            r.appendRaw(" ").appendRaw(this.tableAlias);
        }
        // JOIN（连接更新）
        for (Join join : this.joins) {
            r.appendRaw(join.toSql());
            r.params.addAll(join.getParameters());
        }
        r.appendRaw(" SET ");
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, Object> entry : this.updateValues.entrySet()) {
            String col = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof FieldReference) {
                joiner.add(col + " = " + ((FieldReference) val).getColumn());
            } else if (val instanceof SQL<?>) {
                SQL<?> sub = (SQL<?>) val;
                joiner.add(col + " = (" + sub.toSql() + ")");
                r.params.addAll(sub.getParameters());
            } else {
                joiner.add(col + " = ?");
                r.params.add(val);
            }
        }
        r.appendRaw(joiner.toString());
        if (this.whereCondition.isEmpty()) {
            throw new TinyJdbcException("The UPDATE statement requires a WHERE clause.");
        }
        r.appendRaw(" WHERE ").appendRaw(this.whereCondition.toSql());
        r.params.addAll(this.whereCondition.getParameters());
        return r;
    }

    private RenderedSql renderDelete() {
        RenderedSql r = new RenderedSql();
        r.appendRaw("DELETE FROM " + this.table);
        if (this.tableAlias != null && !this.tableAlias.trim().isEmpty()) {
            r.appendRaw(" ").appendRaw(this.tableAlias);
        }
        if (!this.whereCondition.isEmpty()) {
            r.appendRaw(" WHERE ").appendRaw(this.whereCondition.toSql());
            r.params.addAll(this.whereCondition.getParameters());
        } else {
            throw new TinyJdbcException("The DELETE statement requires a WHERE clause.");
        }
        return r;
    }

    // ------------------------ 私有方法 ------------------------

    private void validateOperation(Operation newOperation) {
        if (this.operation != null) {
            throw new TinyJdbcException("Cannot use " + this.operation + " and " + newOperation + " operations simultaneously.");
        }
        this.operation = newOperation;
    }

    // ------------------------ 内部结构 ------------------------

    /**
     * SELECT 字段：承载字段 SQL 及其携带的参数（函数表达式值参数）。
     */
    private static class SelectItem {
        final String sql;
        final List<Object> params;

        SelectItem(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }

    /**
     * 渲染产物：SQL 字符串 + 按 ? 顺序收集的参数。
     */
    private static class RenderedSql {
        final StringBuilder sb = new StringBuilder();
        final List<Object> params = new ArrayList<>();

        RenderedSql appendRaw(String sql) {
            sb.append(sql);
            return this;
        }

        RenderedSql append(String sql, List<Object> sqlParams) {
            sb.append(sql);
            if (sqlParams != null) {
                params.addAll(sqlParams);
            }
            return this;
        }

        String sql() {
            return sb.toString();
        }

        List<Object> parameters() {
            return params;
        }
    }
}

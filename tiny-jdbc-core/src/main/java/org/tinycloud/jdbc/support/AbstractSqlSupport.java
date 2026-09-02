package org.tinycloud.jdbc.support;

import org.springframework.core.GenericTypeResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.ClassUtils;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.fill.FillMetaObject;
import org.tinycloud.jdbc.fill.MetaObjectHandler;
import org.tinycloud.jdbc.interceptor.*;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.page.PageCheck;
import org.tinycloud.jdbc.page.PageHandleResult;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.util.ArrayUtils;
import org.tinycloud.jdbc.util.CollectionUtils;
import org.tinycloud.jdbc.util.TableRowMapper;
import org.tinycloud.jdbc.util.tuple.Pair;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * jdbc抽象类，给出默认的支持
 *
 * @author liuxingyu01
 * @since 2022-03-11-16:49
 **/
public abstract class AbstractSqlSupport<T, ID extends Serializable> implements ISqlSupport<T, ID>, IObjectSupport<T, ID> {

    protected abstract JdbcTemplate getJdbcTemplate();

    protected abstract IPageHandle getPageHandle();

    protected abstract List<SqlInterceptor> getSqlInterceptors();

    protected abstract TinyJdbcRuntime getTinyJdbcRuntime();

    protected abstract NamedParameterJdbcTemplate getNamedParameterJdbcTemplate();

    /**
     * 泛型
     */
    private final Class<T> entityClass;

    /**
     * bean转换器
     */
    private final RowMapper<T> rowMapper;

    @SuppressWarnings("unchecked")
    public AbstractSqlSupport() {
        // 先剥离 CGLIB 代理层：若 DAO 被 @Transactional/@Async/切面等代理，
        // getClass() 会是 XxxDao$$EnhancerBySpringCGLIB 子类，其 getGenericSuperclass()
        // 不再是 ParameterizedType，直接用旧写法会 ClassCastException。
        Class<?> userClass = ClassUtils.getUserClass(getClass());
        // 沿继承链解析 AbstractSqlSupport 的泛型实参（T, ID），取第一个（T）作为实体类型
        Class<?>[] resolvedArgs = GenericTypeResolver.resolveTypeArguments(userClass, AbstractSqlSupport.class);
        if (resolvedArgs == null || resolvedArgs.length == 0 || resolvedArgs[0] == null) {
            throw new TinyJdbcException("Cannot resolve entity class from " + userClass.getName()
                    + ", please specify the generic type of the DAO explicitly");
        }
        Class<?> resolved = resolvedArgs[0];
        entityClass = (Class<T>) resolved;
        rowMapper = TableRowMapper.newInstance(entityClass);
    }

    // ======================== 抽离的私有工具方法（加do前缀） ========================

    /**
     * 私有工具方法：通过统一执行器执行 SQL 请求。
     *
     * @param request   SQL 请求
     * @param execution 最终数据库执行器
     * @param <R>       执行结果类型
     * @return SQL 执行结果
     */
    private <R> R doSqlExecute(SqlRequest<R> request, SqlExecution<R> execution) {
        return new SqlExecutor(this.getSqlInterceptors()).execute(request, execution);
    }

    /**
     * 私有工具方法：执行查询，返回指定类型的列表
     */
    private <F> List<F> doQuery(String sql, RowMapper<F> rowMapper, Object... params) {
        JdbcTemplate jdbcTemplate = this.getJdbcTemplate();
        SqlRequest<List<F>> request = new SqlRequest<>(sql, params, SqlType.QUERY);
        return this.doSqlExecute(request, invocation -> jdbcTemplate.query(invocation.getSql(), rowMapper, invocation.getArgs()));
    }

    /**
     * 私有工具方法：执行查询，返回Map列表
     */
    private List<Map<String, Object>> doQueryForList(String sql, Object... params) {
        JdbcTemplate jdbcTemplate = this.getJdbcTemplate();
        SqlRequest<List<Map<String, Object>>> request = new SqlRequest<>(sql, params, SqlType.QUERY);
        return this.doSqlExecute(request, invocation -> jdbcTemplate.queryForList(invocation.getSql(), invocation.getArgs()));
    }

    /**
     * 私有工具方法：执行查询，返回单个对象
     */
    private <F> F doQueryForObject(String sql, Class<F> clazz, Object... params) {
        JdbcTemplate jdbcTemplate = this.getJdbcTemplate();
        SqlRequest<F> request = new SqlRequest<>(sql, params, SqlType.QUERY);
        return this.doSqlExecute(request, invocation -> jdbcTemplate.queryForObject(invocation.getSql(), clazz, invocation.getArgs()));
    }

    /**
     * 私有工具方法：执行增删改操作
     */
    private int doUpdate(String sql, Object... params) {
        JdbcTemplate jdbcTemplate = this.getJdbcTemplate();
        SqlRequest<Integer> request = new SqlRequest<>(sql, params, SqlType.UPDATE);
        return this.doSqlExecute(request, invocation -> jdbcTemplate.update(invocation.getSql(), invocation.getArgs()));
    }

    /**
     * 私有工具方法：执行增删改操作，返回自增主键值
     */
    private Pair<Integer, Long> doUpdateReturnAutoIncrement(String sql, Object... params) {
        JdbcTemplate jdbcTemplate = this.getJdbcTemplate();
        SqlRequest<Pair<Integer, Long>> request = new SqlRequest<>(sql, params, SqlType.UPDATE);
        return this.doSqlExecute(request, invocation -> {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int affectedRows = jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(invocation.getSql(), PreparedStatement.RETURN_GENERATED_KEYS);
                Object[] invocationArgs = invocation.getArgs();
                if (ArrayUtils.isNotEmpty(invocationArgs)) {
                    for (int i = 0; i < invocationArgs.length; i++) {
                        ps.setObject(i + 1, invocationArgs[i]);
                    }
                }
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() == null) {
                throw new TinyJdbcException("please check whether it is an autoincrement primary key");
            }
            return new Pair<>(affectedRows, keyHolder.getKey().longValue());
        });
    }


    /**
     * 私有工具方法：执行 DDL 语句（CREATE / ALTER / DROP / TRUNCATE 等）
     */
    private void doExecute(String sql) {
        JdbcTemplate jdbcTemplate = this.getJdbcTemplate();
        SqlRequest<Void> request = new SqlRequest<>(sql, null, SqlType.EXECUTE);
        this.doSqlExecute(request, invocation -> {
            jdbcTemplate.execute(invocation.getSql());
            return null;
        });
    }

    /**
     * 私有工具方法：执行新增前自动填充
     */
    private void doInsertFill(T entity) {
        if (entity == null) {
            return;
        }
        MetaObjectHandler metaObjectHandler = this.getTinyJdbcRuntime().getMetaObjectHandler();
        if (metaObjectHandler == null) {
            return;
        }
        metaObjectHandler.insertFill(new FillMetaObject(entity));
    }

    /**
     * 私有工具方法：执行更新前自动填充
     */
    private void doUpdateFill(T entity) {
        if (entity == null) {
            return;
        }
        MetaObjectHandler metaObjectHandler = this.getTinyJdbcRuntime().getMetaObjectHandler();
        if (metaObjectHandler == null) {
            return;
        }
        metaObjectHandler.updateFill(new FillMetaObject(entity));
    }

    /**
     * 私有工具方法：仅条件构造器更新前自动填充（字符串字段版）
     */
    private void doUpdateCriteriaFill(UpdateCriteria<T> criteria) {
        if (criteria == null) {
            return;
        }
        MetaObjectHandler metaObjectHandler = this.getTinyJdbcRuntime().getMetaObjectHandler();
        if (metaObjectHandler == null) {
            return;
        }
        metaObjectHandler.updateCriteriaFill(criteria, entityClass);
    }

    /**
     * 私有工具方法：仅条件构造器更新前自动填充（Lambda 字段版）
     */
    private void doUpdateLambdaCriteriaFill(LambdaUpdateCriteria<T> criteria) {
        if (criteria == null) {
            return;
        }
        MetaObjectHandler metaObjectHandler = this.getTinyJdbcRuntime().getMetaObjectHandler();
        if (metaObjectHandler == null) {
            return;
        }
        metaObjectHandler.updateLambdaCriteriaFill(criteria, entityClass);
    }


    // ======================== ISqlSupport（纯sql实现）实现开始 ========================

    @Override
    public List<T> select(String sql, Object... params) {
        // 调用加do前缀的方法
        return this.doQuery(sql, rowMapper, params);
    }

    @Override
    public <F> List<F> select(String sql, Class<F> clazz, Object... params) {
        return this.doQuery(sql, new TableRowMapper<>(clazz), params);
    }

    @Override
    public List<Map<String, Object>> selectMap(String sql, Object... params) {
        return this.doQueryForList(sql, params);
    }

    @Override
    public <F> List<F> selectSingleColumn(String sql, Class<F> clazz, Object... params) {
        return this.doQuery(sql, new SingleColumnRowMapper<>(clazz), params);
    }

    @Override
    public <F> F selectOneObject(String sql, Class<F> clazz, Object... params) {
        return this.doQueryForObject(sql, clazz, params);
    }

    @Override
    public Page<T> paginate(String sql, Page<T> page, final Object... params) {
        PageCheck.check(page);
        PageHandleResult handleResult = getPageHandle().handle(sql, page.getPageNum(), page.getPageSize());
        // 查询总共数量
        Long count = this.doQueryForObject(handleResult.getCountSql(), Long.class, params);
        List<T> records;
        if (count != null && count > 0L) {
            records = this.doQuery(handleResult.getPageSql(), rowMapper, ArrayUtils.mergeArrays(params, handleResult.getParameters()));
        } else {
            records = new ArrayList<>();
        }
        page.setRecords(records);
        page.setTotal(count);
        return page;
    }

    @Override
    public <F> Page<F> paginate(String sql, Class<F> clazz, Page<F> page, final Object... params) {
        PageCheck.check(page);
        PageHandleResult handleResult = getPageHandle().handle(sql, page.getPageNum(), page.getPageSize());
        // 查询总共数量
        Long count = this.doQueryForObject(handleResult.getCountSql(), Long.class, params);
        List<F> records;
        if (count != null && count > 0L) {
            records = this.doQuery(handleResult.getPageSql(), new TableRowMapper<>(clazz), ArrayUtils.mergeArrays(params, handleResult.getParameters()));
        } else {
            records = new ArrayList<>();
        }
        page.setRecords(records);
        page.setTotal(count);
        return page;
    }

    @Override
    public Page<Map<String, Object>> paginateMap(String sql, Page<Map<String, Object>> page, Object... params) {
        PageCheck.check(page);
        PageHandleResult handleResult = getPageHandle().handle(sql, page.getPageNum(), page.getPageSize());
        // 查询总共数量
        Long count = this.doQueryForObject(handleResult.getCountSql(), Long.class, params);
        List<Map<String, Object>> records;
        if (count != null && count > 0L) {
            records = this.doQueryForList(handleResult.getPageSql(), ArrayUtils.mergeArrays(params, handleResult.getParameters()));
        } else {
            records = new ArrayList<>();
        }
        page.setRecords(records);
        page.setTotal(count);
        return page;
    }

    @Override
    public int update(String sql, final Object... params) {
        return this.doUpdate(sql, params);
    }

    @Override
    public void execute(String sql) {
        this.doExecute(sql);
    }

    // ======================== ISqlSupport（SQL构造器实现）实现开始 ========================

    @Override
    public int update(SQL<T> sql) {
        return this.update(sql.toSql(), sql.getParameters().toArray());
    }

    @Override
    public List<T> select(SQL<T> sql) {
        return this.select(sql.toSql(), sql.getParameters().toArray());
    }

    @Override
    public <F> List<F> select(SQL<T> sql, Class<F> clazz) {
        return this.select(sql.toSql(), clazz, sql.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(SQL<T> sql, Page<T> page) {
        return this.paginate(sql.toSql(), page, sql.getParameters().toArray());
    }

    @Override
    public <F> Page<F> paginate(SQL<T> sql, Class<F> clazz, Page<F> page) {
        return this.paginate(sql.toSql(), clazz, page, sql.getParameters().toArray());
    }

    @Override
    public <F> F selectOneObject(SQL<T> sql, Class<F> clazz) {
        return this.selectOneObject(sql.toSql(), clazz, sql.getParameters().toArray());
    }

    // ======================== IObjectSupport实现开始 ========================

    @Override
    public T selectById(ID id) {
        if (id == null) {
            throw new TinyJdbcException("selectById id cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectByIdSql(id, entityClass);
        return this.selectOne(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> selectByIds(List<ID> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new TinyJdbcException("selectByIds ids cannot be null or empty");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectByIdsSql(entityClass, new ArrayList<>(ids));
        return this.select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("select entity cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectSql(entity);
        return this.select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(QueryCriteria<T> criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("select criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectCriteriaSql(criteria, entityClass);
        return this.select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(LambdaQueryCriteria<T> lambdaCriteria) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("select lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectLambdaCriteriaSql(lambdaCriteria, entityClass);
        return this.select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(T entity, Page<T> page) {
        if (entity == null) {
            throw new TinyJdbcException("paginate entity cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectSql(entity);
        return this.paginate(sqlProvider.getSql(), page, sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(QueryCriteria<T> criteria, Page<T> page) {
        if (criteria == null) {
            throw new TinyJdbcException("paginate criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectCriteriaSql(criteria, entityClass);
        return this.paginate(sqlProvider.getSql(), page, sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(LambdaQueryCriteria<T> lambdaCriteria, Page<T> page) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("paginate lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectLambdaCriteriaSql(lambdaCriteria, entityClass);
        return this.paginate(sqlProvider.getSql(), page, sqlProvider.getParameters().toArray());
    }

    @Override
    public Long selectCount(QueryCriteria<T> criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectCountCriteriaSql(criteria, entityClass);
        return selectOneObject(sqlProvider.getSql(), Long.class, sqlProvider.getParameters().toArray());
    }

    @Override
    public Long selectCount(LambdaQueryCriteria<T> lambdaCriteria) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildSelectCountLambdaCriteriaSql(lambdaCriteria, entityClass);
        return selectOneObject(sqlProvider.getSql(), Long.class, sqlProvider.getParameters().toArray());
    }

    @Override
    public int insert(T entity) {
        return insert(entity, true);
    }

    @Override
    public int insert(T entity, boolean ignoreNulls) {
        if (entity == null) {
            throw new TinyJdbcException("insert entity cannot be null");
        }
        this.doInsertFill(entity);
        SqlProvider sqlProvider = SqlAssembler.buildInsertSql(entity, ignoreNulls, this.getJdbcTemplate(), this.getTinyJdbcRuntime());
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("insert parameters cannot be null");
        }
        if (sqlProvider.getAutoIncrementPrimaryKeyField() != null) {
            Pair<Integer, Long> pair = this.doUpdateReturnAutoIncrement(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
            // 反射设置自增主键值
            try {
                sqlProvider.getAutoIncrementPrimaryKeyField().set(entity, pair.getRight());
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new TinyJdbcException("inject auto increment primary key failed", e);
            }
            return pair.getLeft();
        }
        return this.insert(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int updateById(T entity) {
        return updateById(entity, true);
    }

    @Override
    public int updateById(T entity, boolean ignoreNulls) {
        if (entity == null) {
            throw new TinyJdbcException("update entity cannot be null");
        }
        this.doUpdateFill(entity);
        SqlProvider sqlProvider = SqlAssembler.buildUpdateByIdSql(entity, ignoreNulls);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("update parameters cannot be null");
        }
        return update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int update(UpdateCriteria<T> criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("criteria cannot be null");
        }
        this.doUpdateCriteriaFill(criteria);
        SqlProvider sqlProvider = SqlAssembler.buildUpdateByCriteriaSql(criteria, entityClass);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("update parameters cannot be null");
        }
        return update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int update(LambdaUpdateCriteria<T> lambdaCriteria) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("lambdaCriteria cannot be null");
        }
        this.doUpdateLambdaCriteriaFill(lambdaCriteria);
        SqlProvider sqlProvider = SqlAssembler.buildUpdateByLambdaCriteriaSql(lambdaCriteria, entityClass);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("update parameters cannot be null");
        }
        return update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int delete(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("delete entity cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildDeleteSql(entity);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("delete parameters cannot be null");
        }
        return delete(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int delete(LambdaUpdateCriteria<T> criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("delete lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildDeleteLambdaCriteriaSql(criteria, entityClass);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("delete parameters cannot be null");
        }
        return delete(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int delete(UpdateCriteria<T> criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("delete criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildDeleteCriteriaSql(criteria, entityClass);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("delete parameters cannot be null");
        }
        return delete(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int deleteById(ID id) {
        if (id == null) {
            throw new TinyJdbcException("deleteById id cannot be null");
        }
        SqlProvider sqlProvider = SqlAssembler.buildDeleteByIdSql(id, entityClass);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("deleteById parameters cannot be null");
        }
        return delete(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int deleteByIds(List<ID> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new TinyJdbcException("deleteByIds ids cannot be null or empty");
        }
        SqlProvider sqlProvider = SqlAssembler.buildDeleteByIdsSql(entityClass, new ArrayList<>(ids));
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("deleteById parameters cannot be null");
        }
        return delete(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int[] batchInsert(Collection<T> collection, boolean ignoreNulls) {
        return this.batchInsert(collection, ignoreNulls, this.getTinyJdbcRuntime().getBatchMode());
    }

    /**
     * 批量插入，显式指定执行模式。
     *
     * @param collection  待插入实体集合
     * @param ignoreNulls 是否忽略 null 字段
     * @param mode        执行模式
     * @return 每个元素表示对应实体的受影响行数
     */
    public int[] batchInsert(Collection<T> collection, boolean ignoreNulls, BatchMode mode) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new TinyJdbcException("batchInsert collection cannot be null or empty");
        }
        BatchMode effective = mode == null ? this.getTinyJdbcRuntime().getBatchMode() : mode;
        if (effective == BatchMode.MULTI_VALUE) {
            return this.doBatchInsertMultiValue(collection, ignoreNulls);
        } else {
            return this.doBatchInsertJdbcBatch(collection, ignoreNulls);
        }
    }

    /**
     * JDBC 批量插入：逐实体生成 SQL，收集参数后交给 {@code JdbcTemplate.batchUpdate}。
     *
     * <p>要求所有实体生成完全一致的 SQL，否则抛出异常。自增主键不写回实体。
     * 若 JDBC URL 未配置 {@code rewriteBatchedStatements=true}，底层实际仍是逐条往返。</p>
     */
    private int[] doBatchInsertJdbcBatch(Collection<T> collection, boolean ignoreNulls) {
        List<Object[]> batchArgs = new ArrayList<>(collection.size());
        String sql = null;
        for (T t : collection) {
            this.doInsertFill(t);
            SqlProvider sqlProvider = SqlAssembler.buildInsertSql(t, ignoreNulls, this.getJdbcTemplate(), this.getTinyJdbcRuntime());
            if (sql == null || sql.isEmpty()) {
                sql = sqlProvider.getSql();
            } else if (!sql.equals(sqlProvider.getSql())) {
                throw new TinyJdbcException("batchInsert requires the same SQL for all entities, first sql: "
                        + sql + ", current sql: " + sqlProvider.getSql());
            }
            batchArgs.add(sqlProvider.getParameters().toArray());
        }
        SqlRequest<int[]> request = SqlRequest.batch(sql, batchArgs);
        return this.doSqlExecute(request, invocation -> this.getJdbcTemplate().batchUpdate(invocation.getSql(), invocation.getBatchArgs()));
    }

    /**
     * 多值批量插入：把集合切块，每块生成单条 {@code INSERT ... VALUES (...),(...)}。
     *
     * <p>注意：批量模式下自增主键不回写实体（无法可靠按行映射）；返回数组的每个元素为该行所在
     * 语句的受影响行数按行分摊（正常插入时为 1）。</p>
     */
    private int[] doBatchInsertMultiValue(Collection<T> collection, boolean ignoreNulls) {
        for (T t : collection) {
            this.doInsertFill(t);
        }
        BatchInsertSql batch = SqlAssembler.buildBatchInsert(collection, ignoreNulls, this.getJdbcTemplate(), this.getTinyJdbcRuntime());
        int batchSize = Math.max(1, this.getTinyJdbcRuntime().getBatchInsertSize());
        List<Object[]> rows = batch.getRows();
        int[] result = new int[rows.size()];
        String columnClause = String.join(", ", batch.getColumns());
        int index = 0;
        while (index < rows.size()) {
            int end = Math.min(index + batchSize, rows.size());
            StringBuilder sb = new StringBuilder("INSERT INTO ").append(batch.getTableName())
                    .append(" (").append(columnClause).append(") VALUES ");
            List<Object> params = new ArrayList<>();
            for (int i = index; i < end; i++) {
                Object[] row = rows.get(i);
                if (i > index) {
                    sb.append(", ");
                }
                sb.append("(");
                for (int j = 0; j < row.length; j++) {
                    if (j > 0) {
                        sb.append(", ");
                    }
                    sb.append("?");
                }
                sb.append(")");
                Collections.addAll(params, row);
            }
            String stmt = sb.toString();
            SqlRequest<Integer> request = new SqlRequest<>(stmt, params.toArray(), SqlType.UPDATE);
            int affected = this.doSqlExecute(request, invocation -> this.getJdbcTemplate().update(invocation.getSql(), invocation.getArgs()));
            int chunkRows = end - index;
            int perRow = chunkRows == 0 ? 0 : affected / chunkRows;
            for (int k = index; k < end; k++) {
                result[k] = perRow;
            }
            index = end;
        }
        return result;
    }

    @Override
    public void truncate() {
        SqlProvider sqlProvider = SqlAssembler.buildTruncateSql(entityClass);
        this.execute(sqlProvider.getSql());
    }
}

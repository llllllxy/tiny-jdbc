package org.tinycloud.jdbc.support;

import org.springframework.core.GenericTypeResolver;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.util.ClassUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.fill.FillMetaObject;
import org.tinycloud.jdbc.fill.MetaObjectHandler;
import org.tinycloud.jdbc.interceptor.SqlExecution;
import org.tinycloud.jdbc.interceptor.SqlExecutor;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;
import org.tinycloud.jdbc.interceptor.SqlRequest;
import org.tinycloud.jdbc.interceptor.SqlType;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.page.PageCheck;
import org.tinycloud.jdbc.page.PageHandleResult;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.util.ArrayUtils;
import org.tinycloud.jdbc.util.CollectionUtils;
import org.tinycloud.jdbc.util.tuple.Pair;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        rowMapper = BeanPropertyRowMapper.newInstance(entityClass);
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
        return this.doQuery(sql, new BeanPropertyRowMapper<>(clazz), params);
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
            records = this.doQuery(handleResult.getPageSql(), new BeanPropertyRowMapper<>(clazz), ArrayUtils.mergeArrays(params, handleResult.getParameters()));
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
        SqlProvider sqlProvider = SqlGenerator.selectByIdSql(id, entityClass);
        return this.selectOne(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> selectByIds(List<ID> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new TinyJdbcException("selectByIds ids cannot be null or empty");
        }
        SqlProvider sqlProvider = SqlGenerator.selectByIdsSql(entityClass, new ArrayList<>(ids));
        return this.select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("select entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectSql(entity);
        return this.select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(QueryCriteria<T> criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("select criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCriteriaSql(criteria, entityClass);
        return this.select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(LambdaQueryCriteria<T> lambdaCriteria) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("select lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectLambdaCriteriaSql(lambdaCriteria, entityClass);
        return this.select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(T entity, Page<T> page) {
        if (entity == null) {
            throw new TinyJdbcException("paginate entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectSql(entity);
        return this.paginate(sqlProvider.getSql(), page, sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(QueryCriteria<T> criteria, Page<T> page) {
        if (criteria == null) {
            throw new TinyJdbcException("paginate criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCriteriaSql(criteria, entityClass);
        return this.paginate(sqlProvider.getSql(), page, sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(LambdaQueryCriteria<T> lambdaCriteria, Page<T> page) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("paginate lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectLambdaCriteriaSql(lambdaCriteria, entityClass);
        return this.paginate(sqlProvider.getSql(), page, sqlProvider.getParameters().toArray());
    }

    @Override
    public Long selectCount(QueryCriteria<T> criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCountCriteriaSql(criteria, entityClass);
        return selectOneObject(sqlProvider.getSql(), Long.class, sqlProvider.getParameters().toArray());
    }

    @Override
    public Long selectCount(LambdaQueryCriteria<T> lambdaCriteria) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCountLambdaCriteriaSql(lambdaCriteria, entityClass);
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
        SqlProvider sqlProvider = SqlGenerator.insertSql(entity, ignoreNulls, this.getJdbcTemplate(), this.getTinyJdbcRuntime());
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
        SqlProvider sqlProvider = SqlGenerator.updateByIdSql(entity, ignoreNulls);
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
        SqlProvider sqlProvider = SqlGenerator.updateByCriteriaSql(criteria, entityClass);
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
        SqlProvider sqlProvider = SqlGenerator.updateByLambdaCriteriaSql(lambdaCriteria, entityClass);
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
        SqlProvider sqlProvider = SqlGenerator.deleteSql(entity);
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
        SqlProvider sqlProvider = SqlGenerator.deleteLambdaCriteriaSql(criteria, entityClass);
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
        SqlProvider sqlProvider = SqlGenerator.deleteCriteriaSql(criteria, entityClass);
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
        SqlProvider sqlProvider = SqlGenerator.deleteByIdSql(id, entityClass);
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
        SqlProvider sqlProvider = SqlGenerator.deleteByIdsSql(entityClass, new ArrayList<>(ids));
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("deleteById parameters cannot be null");
        }
        return delete(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int[] batchInsert(Collection<T> collection, boolean ignoreNulls) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new TinyJdbcException("batchInsert collection cannot be null or empty");
        }
        List<Object[]> batchArgs = new ArrayList<>(collection.size());
        String sql = null;
        for (T t : collection) {
            this.doInsertFill(t);
            SqlProvider sqlProvider = SqlGenerator.insertSql(t, ignoreNulls, this.getJdbcTemplate(), this.getTinyJdbcRuntime());
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

    @Override
    public void truncate() {
        SqlProvider sqlProvider = SqlGenerator.truncateSql(entityClass);
        this.execute(sqlProvider.getSql());
    }
}

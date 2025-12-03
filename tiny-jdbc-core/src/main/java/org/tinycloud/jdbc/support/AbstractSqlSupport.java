package org.tinycloud.jdbc.support;

import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.page.PageCheck;
import org.tinycloud.jdbc.page.PageHandleResult;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.util.ArrayUtils;
import org.tinycloud.jdbc.util.CollectionUtils;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        entityClass = (Class<T>) type.getActualTypeArguments()[0];
        rowMapper = BeanPropertyRowMapper.newInstance(entityClass);
    }

    // ---------------------------------ISqlSupport（纯sql实现）实现开始---------------------------------

    @Override
    public List<T> select(String sql, Object... params) {
        return getJdbcTemplate().query(sql, rowMapper, params);
    }

    @Override
    public <F> List<F> select(String sql, Class<F> clazz, Object... params) {
        return getJdbcTemplate().query(sql, new BeanPropertyRowMapper<>(clazz), params);
    }

    @Override
    public List<Map<String, Object>> selectMap(String sql, Object... params) {
        return getJdbcTemplate().queryForList(sql, params);
    }

    @Override
    public <F> List<F> selectSingleColumn(String sql, Class<F> clazz, Object... params) {
        return getJdbcTemplate().query(sql, new SingleColumnRowMapper<>(clazz), params);
    }

    @Override
    public <F> F selectOneObject(String sql, Class<F> clazz, Object... params) {
        return getJdbcTemplate().queryForObject(sql, clazz, params);
    }

    @Override
    public Page<T> paginate(String sql, Page<T> page, final Object... params) {
        PageCheck.check(page);
        PageHandleResult handleResult = getPageHandle().handle(sql, page.getPageNum(), page.getPageSize());
        // 查询总共数量
        Long count = getJdbcTemplate().queryForObject(handleResult.getCountSql(), Long.class, params);
        List<T> records;
        if (count != null && count > 0L) {
            records = getJdbcTemplate().query(handleResult.getPageSql(), rowMapper, ArrayUtils.mergeArrays(params, handleResult.getParameters()));
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
        Long count = getJdbcTemplate().queryForObject(handleResult.getCountSql(), Long.class, params);
        List<F> records;
        if (count != null && count > 0L) {
            records = getJdbcTemplate().query(handleResult.getPageSql(), new BeanPropertyRowMapper<>(clazz), ArrayUtils.mergeArrays(params, handleResult.getParameters()));
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
        Long count = getJdbcTemplate().queryForObject(handleResult.getCountSql(), Long.class, params);
        List<Map<String, Object>> records;
        if (count != null && count > 0L) {
            records = getJdbcTemplate().queryForList(handleResult.getPageSql(), ArrayUtils.mergeArrays(params, handleResult.getParameters()));
        } else {
            records = new ArrayList<>();
        }
        page.setRecords(records);
        page.setTotal(count);
        return page;
    }

    @Override
    public int execute(String sql, final Object... params) {
        return getJdbcTemplate().update(sql, params);
    }

    // ---------------------------------ISqlSupport（SQL构造器实现）实现开始---------------------------------

    @Override
    public int execute(SQL sql) {
        return execute(sql.toSql(), sql.getParameters().toArray());
    }

    @Override
    public List<T> select(SQL sql) {
        return select(sql.toSql(), sql.getParameters().toArray());
    }

    @Override
    public <F> List<F> select(SQL sql, Class<F> clazz) {
        return select(sql.toSql(), clazz, sql.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(SQL sql, Page<T> page) {
        return paginate(sql.toSql(), page, sql.getParameters().toArray());
    }

    @Override
    public <F> Page<F> paginate(SQL sql, Class<F> clazz, Page<F> page) {
        return paginate(sql.toSql(), clazz, page, sql.getParameters().toArray());
    }

    @Override
    public <F> F selectOneObject(SQL sql, Class<F> clazz) {
        return selectOneObject(sql.toSql(), clazz, sql.getParameters().toArray());
    }

    // ---------------------------------IObjectSupport实现开始---------------------------------
    @Override
    public T selectById(ID id) {
        if (id == null) {
            throw new TinyJdbcException("selectById id cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectByIdSql(id, entityClass);
        return selectOne(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> selectByIds(List<ID> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new TinyJdbcException("selectByIds ids cannot be null or empty");
        }
        SqlProvider sqlProvider = SqlGenerator.selectByIdsSql(entityClass, new ArrayList<>(ids));
        return select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("select entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectSql(entity);
        return select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(QueryCriteria<T> criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("select criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCriteriaSql(criteria, entityClass);
        return select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(LambdaQueryCriteria<T> lambdaCriteria) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("select lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectLambdaCriteriaSql(lambdaCriteria, entityClass);
        return select(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(T entity, Page<T> page) {
        if (entity == null) {
            throw new TinyJdbcException("paginate entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectSql(entity);
        return paginate(sqlProvider.getSql(), page, sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(QueryCriteria<T> criteria, Page<T> page) {
        if (criteria == null) {
            throw new TinyJdbcException("paginate criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCriteriaSql(criteria, entityClass);
        return paginate(sqlProvider.getSql(), page, sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(LambdaQueryCriteria<T> lambdaCriteria, Page<T> page) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("paginate lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectLambdaCriteriaSql(lambdaCriteria, entityClass);
        return paginate(sqlProvider.getSql(), page, sqlProvider.getParameters().toArray());
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
        SqlProvider sqlProvider = SqlGenerator.insertSql(entity, ignoreNulls, getJdbcTemplate());
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("insert parameters cannot be null");
        }
        return insert(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public Long insertReturnAutoIncrement(T entity) {
        return insertReturnAutoIncrement(entity, true);
    }

    @Override
    public Long insertReturnAutoIncrement(T entity, boolean ignoreNulls) {
        if (entity == null) {
            throw new TinyJdbcException("insertReturnAutoIncrement entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.insertSql(entity, ignoreNulls, getJdbcTemplate());
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("insertReturnAutoIncrement parameters cannot be null");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        final Object[] params = sqlProvider.getParameters().toArray();
        getJdbcTemplate().update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(sqlProvider.getSql(), PreparedStatement.RETURN_GENERATED_KEYS);
                if (ArrayUtils.isNotEmpty(params)) {
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);
                    }
                }
                return ps;
            }
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            return keyHolder.getKey().longValue();
        } else {
            throw new TinyJdbcException("please check whether it is an autoincrement primary key");
        }
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
        SqlProvider sqlProvider = SqlGenerator.updateByIdSql(entity, ignoreNulls);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("update parameters cannot be null");
        }
        return update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int update(T entity, UpdateCriteria<T> criteria) {
        return update(entity, true, criteria);
    }

    @Override
    public int update(T entity, LambdaUpdateCriteria<T> lambdaCriteria) {
        return update(entity, true, lambdaCriteria);
    }

    @Override
    public int update(T entity, boolean ignoreNulls, UpdateCriteria<T> criteria) {
        if (entity == null) {
            throw new TinyJdbcException("update entity cannot be null");
        }
        if (criteria == null) {
            throw new TinyJdbcException("criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.updateByEntityAndCriteriaSql(entity, ignoreNulls, criteria);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("update parameters cannot be null");
        }
        return update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int update(T entity, boolean ignoreNulls, LambdaUpdateCriteria<T> criteria) {
        if (entity == null) {
            throw new TinyJdbcException("update entity cannot be null");
        }
        if (criteria == null) {
            throw new TinyJdbcException("criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.updateByEntityAndLambdaCriteriaSql(entity, ignoreNulls, criteria);
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
            SqlProvider sqlProvider = SqlGenerator.insertSql(t, ignoreNulls, getJdbcTemplate());
            if (sql == null || sql.isEmpty()) {
                sql = sqlProvider.getSql();
            }
            batchArgs.add(sqlProvider.getParameters().toArray());
        }
        return getJdbcTemplate().batchUpdate(sql, batchArgs);
    }

    @Override
    public int truncate() {
        SqlProvider sqlProvider = SqlGenerator.truncateSql(entityClass);
        return execute(sqlProvider.getSql());
    }
}

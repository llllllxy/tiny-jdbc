package org.tinycloud.jdbc.support;

import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.sql.SqlGenerator;
import org.tinycloud.jdbc.sql.SqlProvider;
import org.tinycloud.jdbc.util.DataAccessUtils;
import org.tinycloud.jdbc.util.StrUtils;

import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


/**
 * jdbc抽象类，给出默认的支持
 *
 * @author liuxingyu01
 * @since 2022-03-11-16:49
 **/
public abstract class AbstractSqlSupport<T, ID> implements ISqlSupport<T, ID>, IObjectSupport<T, ID> {

    protected abstract JdbcTemplate getJdbcTemplate();

    protected abstract IPageHandle getPageHandle();

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

    /**
     * 执行查询sql
     *
     * @param sql    要执行的SQL
     * @param params 要绑定到查询的参数 ，可以不传
     * @return 查询结果
     */
    @Override
    public List<T> select(String sql, Object... params) {
        List<T> resultList;
        if (!ObjectUtils.isEmpty(params)) {
            resultList = getJdbcTemplate().query(sql, rowMapper, params);
        } else {
            // BeanPropertyRowMapper是自动映射实体类的
            resultList = getJdbcTemplate().query(sql, rowMapper);
        }
        return resultList;
    }

    /**
     * 执行查询sql，自定义返回类型clazz
     *
     * @param sql    要执行的SQL
     * @param clazz  实体类类型
     * @param params 要绑定到查询的参数 ，可以不传
     * @param <F>    泛型
     * @return 查询结果
     */
    @Override
    public <F> List<F> select(String sql, Class<F> clazz, Object... params) {
        List<F> resultList;
        if (!ObjectUtils.isEmpty(params)) {
            resultList = getJdbcTemplate().query(sql, new BeanPropertyRowMapper<>(clazz), params);
        } else {
            // BeanPropertyRowMapper是自动映射实体类的
            resultList = getJdbcTemplate().query(sql, new BeanPropertyRowMapper<>(clazz));
        }
        return resultList;
    }

    /**
     * 执行查询sql，（固定返回List<Map<String, Object>>）
     *
     * @param sql    要执行的sql
     * @param params 要绑定到查询的参数
     * @return Map<String, Object>
     */
    @Override
    public List<Map<String, Object>> selectMap(String sql, Object... params) {
        return getJdbcTemplate().queryForList(sql, params);
    }

    /**
     * 执行查询sql，有查询条件，结果返回第一条（固定返回Map<String, Object>）
     *
     * @param sql    要执行的sql
     * @param params 要绑定到查询的参数
     * @return Map<String, Object>
     */
    @Override
    public Map<String, Object> selectOneMap(String sql, final Object... params) {
        List<Map<String, Object>> resultList = getJdbcTemplate().queryForList(sql, params);
        return DataAccessUtils.singleResult(resultList);
    }

    /**
     * 查询一个值（常用于查count等）
     *
     * @param sql    要执行的SQL查询
     * @param clazz  实体类类型
     * @param params 要绑定到查询的参数
     * @param <F>    泛型
     * @return F
     */
    @Override
    public <F> F selectOneColumn(String sql, Class<F> clazz, Object... params) {
        F result;
        if (ObjectUtils.isEmpty(params)) {
            result = getJdbcTemplate().queryForObject(sql, clazz);
        } else {
            result = getJdbcTemplate().queryForObject(sql, clazz, params);
        }
        return result;
    }

    /**
     * 分页查询（带参数）
     *
     * @param sql    要执行的SQL
     * @param page   分页参数
     * @param params ？参数
     * @return Page<T>
     */
    @Override
    public Page<T> paginate(String sql, Page<T> page, final Object... params) {
        if (page == null || page.getPageNum() == null || page.getPageSize() == null) {
            throw new TinyJdbcException("paginate page cannot be null");
        }
        if (page.getPageNum() <= 0) {
            throw new TinyJdbcException("pageNum must be greater than 0");
        }
        if (page.getPageSize() <= 0) {
            throw new TinyJdbcException("pageSize must be greater than 0");
        }
        String selectSql = getPageHandle().handlerPagingSQL(sql, page.getPageNum(), page.getPageSize());
        String countSql = getPageHandle().handlerCountSQL(sql);
        // 查询数据列表
        List<T> list = getJdbcTemplate().query(selectSql, rowMapper, params);
        // 查询总共数量
        long totalSize = getJdbcTemplate().queryForObject(countSql, Long.class, params);
        page.setRecords(list);
        page.setTotal(totalSize);
        return page;
    }

    /**
     * 分页查询（带参数）
     *
     * @param sql    要执行的SQL
     * @param clazz  实体类型
     * @param page   分页参数
     * @param params ？参数
     * @return Page<F>
     */
    @Override
    public <F> Page<F> paginate(String sql, Class<F> clazz, Page<F> page, final Object... params) {
        if (page == null || page.getPageNum() == null || page.getPageSize() == null) {
            throw new TinyJdbcException("paginate page cannot be null");
        }
        if (page.getPageNum() <= 0) {
            throw new TinyJdbcException("pageNum must be greater than 0");
        }
        if (page.getPageSize() <= 0) {
            throw new TinyJdbcException("pageSize must be greater than 0");
        }
        String selectSql = getPageHandle().handlerPagingSQL(sql, page.getPageNum(), page.getPageSize());
        String countSql = getPageHandle().handlerCountSQL(sql);
        // 查询数据列表
        List<F> list = getJdbcTemplate().query(selectSql, new BeanPropertyRowMapper<>(clazz), params);
        // 查询总共数量
        long totalSize = getJdbcTemplate().queryForObject(countSql, Long.class, params);
        page.setRecords(list);
        page.setTotal(totalSize);
        return page;
    }


    /**
     * 执行删除，插入，更新操作
     *
     * @param sql    要执行的SQL
     * @param params 要绑定到SQL的参数
     * @return 成功的条数
     */
    @Override
    public int execute(String sql, final Object... params) {
        int num = 0;
        if (ObjectUtils.isEmpty(params)) {
            num = getJdbcTemplate().update(sql);
        } else {
            num = getJdbcTemplate().update(sql, params);
        }
        return num;
    }

    @Override
    public int insert(String sql, final Object... params) {
        int num = 0;
        if (ObjectUtils.isEmpty(params)) {
            num = getJdbcTemplate().update(sql);
        } else {
            num = getJdbcTemplate().update(sql, params);
        }
        return num;
    }

    @Override
    public int update(String sql, final Object... params) {
        int num = 0;
        if (ObjectUtils.isEmpty(params)) {
            num = getJdbcTemplate().update(sql);
        } else {
            num = getJdbcTemplate().update(sql, params);
        }
        return num;
    }

    @Override
    public int delete(String sql, final Object... params) {
        int num = 0;
        if (ObjectUtils.isEmpty(params)) {
            num = getJdbcTemplate().update(sql);
        } else {
            num = getJdbcTemplate().update(sql, params);
        }
        return num;
    }

    // ---------------------------------ISqlSupport结束---------------------------------


    // ---------------------------------IObjectSupport开始---------------------------------
    @Override
    public T selectById(ID id) {
        if (id == null) {
            throw new TinyJdbcException("selectById id cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectByIdSql(id, entityClass);
        List<T> list = getJdbcTemplate().query(sqlProvider.getSql(), rowMapper, sqlProvider.getParameters().toArray());
        if (!CollectionUtils.isEmpty(list)) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<T> selectByIds(List<ID> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new TinyJdbcException("selectByIds ids cannot be null or empty");
        }
        SqlProvider sqlProvider = SqlGenerator.selectByIdsSql(entityClass, (List<Object>) ids);
        return getJdbcTemplate().query(sqlProvider.getSql(), rowMapper, sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("select entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectSql(entity);
        return getJdbcTemplate().query(sqlProvider.getSql(), rowMapper, sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(QueryCriteria criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("select criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCriteriaSql(criteria, entityClass);
        return getJdbcTemplate().query(sqlProvider.getSql(), rowMapper, sqlProvider.getParameters().toArray());
    }

    @Override
    public List<T> select(LambdaQueryCriteria lambdaCriteria) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("select lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectLambdaCriteriaSql(lambdaCriteria, entityClass);
        return getJdbcTemplate().query(sqlProvider.getSql(), rowMapper, sqlProvider.getParameters().toArray());
    }

    @Override
    public Page<T> paginate(T entity, Page<T> page) {
        if (entity == null) {
            throw new TinyJdbcException("paginate entity cannot be null");
        }
        if (page == null || page.getPageNum() == null || page.getPageSize() == null) {
            throw new TinyJdbcException("paginate page cannot be null");
        }
        if (page.getPageNum() <= 0) {
            throw new TinyJdbcException("当前页数必须大于1");
        }
        if (page.getPageSize() <= 0) {
            throw new TinyJdbcException("每页大小必须大于1");
        }
        SqlProvider sqlProvider = SqlGenerator.selectSql(entity);
        String selectSql = getPageHandle().handlerPagingSQL(sqlProvider.getSql(), page.getPageNum(), page.getPageSize());
        String countSql = getPageHandle().handlerCountSQL(sqlProvider.getSql());
        // 查询数据列表
        List<T> resultList = getJdbcTemplate().query(selectSql, rowMapper, sqlProvider.getParameters().toArray());
        // 查询总共数量
        long totalSize = getJdbcTemplate().queryForObject(countSql, Long.class, sqlProvider.getParameters().toArray());
        page.setRecords(resultList);
        page.setTotal(totalSize);
        return page;
    }

    @Override
    public Page<T> paginate(QueryCriteria criteria, Page<T> page) {
        if (criteria == null) {
            throw new TinyJdbcException("paginate criteria cannot be null");
        }
        if (page == null || page.getPageNum() == null || page.getPageSize() == null) {
            throw new TinyJdbcException("paginate page cannot be null");
        }
        if (page.getPageNum() <= 0) {
            throw new TinyJdbcException("pageNum must be greater than 0");
        }
        if (page.getPageSize() <= 0) {
            throw new TinyJdbcException("pageSize must be greater than 0");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCriteriaSql(criteria, entityClass);
        String selectSql = getPageHandle().handlerPagingSQL(sqlProvider.getSql(), page.getPageNum(), page.getPageSize());
        String countSql = getPageHandle().handlerCountSQL(sqlProvider.getSql());
        // 查询数据列表
        List<T> resultList = getJdbcTemplate().query(selectSql, rowMapper, sqlProvider.getParameters().toArray());
        // 查询总共数量
        long totalSize = getJdbcTemplate().queryForObject(countSql, Long.class, sqlProvider.getParameters().toArray());
        page.setRecords(resultList);
        page.setTotal(totalSize);
        return page;
    }

    @Override
    public Page<T> paginate(LambdaQueryCriteria lambdaCriteria, Page<T> page) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("paginate lambdaCriteria cannot be null");
        }
        if (page == null || page.getPageNum() == null || page.getPageSize() == null) {
            throw new TinyJdbcException("paginate page cannot be null");
        }
        if (page.getPageNum() <= 0) {
            throw new TinyJdbcException("pageNum must be greater than 0");
        }
        if (page.getPageSize() <= 0) {
            throw new TinyJdbcException("pageSize must be greater than 0");
        }
        SqlProvider sqlProvider = SqlGenerator.selectLambdaCriteriaSql(lambdaCriteria, entityClass);
        String selectSql = getPageHandle().handlerPagingSQL(sqlProvider.getSql(), page.getPageNum(), page.getPageSize());
        String countSql = getPageHandle().handlerCountSQL(sqlProvider.getSql());
        // 查询数据列表
        List<T> resultList = getJdbcTemplate().query(selectSql, rowMapper, sqlProvider.getParameters().toArray());
        // 查询总共数量
        long totalSize = getJdbcTemplate().queryForObject(countSql, Long.class, sqlProvider.getParameters().toArray());
        page.setRecords(resultList);
        page.setTotal(totalSize);
        return page;
    }

    @Override
    public Long selectCount(QueryCriteria criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCountCriteriaSql(criteria, entityClass);
        return getJdbcTemplate().queryForObject(sqlProvider.getSql(), Long.class, sqlProvider.getParameters().toArray());
    }

    @Override
    public Long selectCount(LambdaQueryCriteria lambdaCriteria) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectCountLambdaCriteriaSql(lambdaCriteria, entityClass);
        return getJdbcTemplate().queryForObject(sqlProvider.getSql(), Long.class, sqlProvider.getParameters().toArray());
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
        SqlProvider sqlProvider = SqlGenerator.insertSql(entity, ignoreNulls);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("insert parameters cannot be null");
        }
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public Long insertReturnAutoIncrement(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("insertReturnAutoIncrement entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.insertSql(entity, true);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("insertReturnAutoIncrement parameters cannot be null");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        final Object[] params = sqlProvider.getParameters().toArray();

        getJdbcTemplate().update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(sqlProvider.getSql(),
                        PreparedStatement.RETURN_GENERATED_KEYS);
                if (!ObjectUtils.isEmpty(params)) {
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
            throw new TinyJdbcException("insertReturnAutoIncrement please check whether it is an autoincrement primary key");
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
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int update(T entity, UpdateCriteria criteria) {
        return update(entity, true, criteria);
    }

    @Override
    public int update(T entity, LambdaUpdateCriteria lambdaCriteria) {
        return update(entity, true, lambdaCriteria);
    }

    @Override
    public int update(T entity, boolean ignoreNulls, UpdateCriteria criteria) {
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
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int update(T entity, boolean ignoreNulls, LambdaUpdateCriteria criteria) {
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
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int update(UpdateCriteria criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.updateByCriteriaSql(criteria, entityClass);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("update parameters cannot be null");
        }
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int update(LambdaUpdateCriteria lambdaCriteria) {
        if (lambdaCriteria == null) {
            throw new TinyJdbcException("lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.updateByLambdaCriteriaSql(lambdaCriteria, entityClass);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("update parameters cannot be null");
        }
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
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
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int delete(LambdaUpdateCriteria criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("delete lambdaCriteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.deleteLambdaCriteriaSql(criteria, entityClass);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("deleteById parameters cannot be null");
        }
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int delete(UpdateCriteria criteria) {
        if (criteria == null) {
            throw new TinyJdbcException("delete criteria cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.deleteCriteriaSql(criteria, entityClass);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("deleteById parameters cannot be null");
        }
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
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
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public int deleteByIds(List<ID> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new TinyJdbcException("deleteByIds ids cannot be null or empty");
        }
        SqlProvider sqlProvider = SqlGenerator.deleteByIdsSql(entityClass, (List<Object>) ids);
        if (CollectionUtils.isEmpty(sqlProvider.getParameters())) {
            throw new TinyJdbcException("deleteById parameters cannot be null");
        }
        return getJdbcTemplate().update(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }


    @Override
    public int[] batchInsert(Collection<T> collection) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new TinyJdbcException("batchInsert collection cannot be null or empty");
        }
        int[] row = null;
        List<Object[]> batchArgs = new ArrayList<>();
        String sql = "";
        for (T t : collection) {
            SqlProvider sqlProvider = SqlGenerator.insertSql(t, true);
            if (StrUtils.isEmpty(sql)) {
                sql = sqlProvider.getSql();
            }
            batchArgs.add(sqlProvider.getParameters().toArray());
        }
        if (CollectionUtils.isEmpty(batchArgs)) {
            throw new TinyJdbcException("batchInsert batchArgs cannot be null");
        }
        row = getJdbcTemplate().batchUpdate(sql, batchArgs);
        return row;
    }
}

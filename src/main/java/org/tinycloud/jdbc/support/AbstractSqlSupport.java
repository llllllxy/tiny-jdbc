package org.tinycloud.jdbc.support;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;
import org.tinycloud.jdbc.exception.JdbcException;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.sql.SqlGenerator;
import org.tinycloud.jdbc.sql.SqlProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


/**
 * @author liuxingyu01
 * @date 2022-03-11-16:49
 * @description jdbc抽象类，给出默认的支持
 **/
public abstract class AbstractSqlSupport implements ISqlSupport, IObjectSupport {

    protected abstract JdbcTemplate getJdbcTemplate();

    protected abstract IPageHandle getPageHandle();


    /**
     * 执行查询sql，有查询条件
     *
     * @param sql    要执行的SQL
     * @param classz 实体类
     * @param params 要绑定到查询的参数 ，可以不传
     * @param <T>    泛型
     * @return 查询结果
     */
    @Override
    public <T> List<T> select(String sql, Class<T> classz, Object... params) {
        List<T> resultList = null;
        if (params != null && params.length > 0) {
            resultList = getJdbcTemplate().query(sql, params, new BeanPropertyRowMapper<T>(classz));
        } else {
            // BeanPropertyRowMapper是自动映射实体类的
            resultList = getJdbcTemplate().query(sql, new BeanPropertyRowMapper<T>(classz));
        }
        return resultList;
    }


    /**
     * 执行查询sql，有查询条件，（固定返回List<Map<String, Object>>）
     *
     * @param sql    要执行的sql
     * @param params 要绑定到查询的参数
     * @return Map<String, Object>
     */
    public List<Map<String, Object>> select(String sql, Object... params) {
        return getJdbcTemplate().queryForList(sql, params);
    }


    /**
     * 执行查询sql，无查询条件，（固定返回List<Map<String, Object>>）
     *
     * @param sql 要执行的sql
     * @return Map<String, Object>
     */
    public List<Map<String, Object>> select(String sql) {
        return getJdbcTemplate().queryForList(sql);
    }


    /**
     * 执行查询sql，有查询条件，结果返回第一条
     *
     * @param sql    要执行的SQL
     * @param classz 实体类
     * @param params 要绑定到查询的参数
     * @param <T>    泛型
     * @return 查询结果
     */
    @Override
    public <T> T selectOne(String sql, Class<T> classz, final Object... params) {
        List<T> resultList = this.select(sql, classz, params);
        if (resultList != null && !resultList.isEmpty()) {
            return resultList.get(0);
        }
        return null;
    }

    /**
     * 执行查询sql，无查询条件，结果返回第一条（固定返回Map<String, Object>）
     *
     * @param sql 要执行的sql
     * @return Map<String, Object>
     */
    @Override
    public Map<String, Object> selectOne(String sql) {
        List<Map<String, Object>> resultList = getJdbcTemplate().queryForList(sql);
        if (resultList != null && !resultList.isEmpty()) {
            return resultList.get(0);
        }
        return null;
    }


    /**
     * 执行查询sql，有查询条件，结果返回第一条（固定返回Map<String, Object>）
     *
     * @param sql    要执行的sql
     * @param params 要绑定到查询的参数
     * @return Map<String, Object>
     */
    @Override
    public Map<String, Object> selectOne(String sql, final Object... params) {
        List<Map<String, Object>> resultList = getJdbcTemplate().queryForList(sql, params);
        if (resultList != null && !resultList.isEmpty()) {
            return resultList.get(0);
        }
        return null;
    }


    /**
     * 查询一个值（经常用于查count）
     *
     * @param sql    要执行的SQL查询
     * @param clazz  实体类
     * @param params 要绑定到查询的参数
     * @param <T>    泛型
     * @return T
     */
    @Override
    public <T> T selectOneColumn(String sql, Class<T> clazz, Object... params) {
        T result;
        if (params == null || params.length == 0) {
            result = getJdbcTemplate().queryForObject(sql, clazz);
        } else {
            result = getJdbcTemplate().queryForObject(sql, params, clazz);
        }
        return result;
    }

    /**
     * 分页查询
     *
     * @param sql   要执行的SQL查询
     * @param clazz 实体类
     * @param <T>   泛型
     * @return T
     */
    @Override
    public <T> Page<T> paginate(String sql, Class<T> clazz, Integer pageNumber, Integer pageSize) {
        if (pageNumber <= 0) {
            throw new JdbcException("当前页数必须大于1");
        }
        if (pageSize <= 0) {
            throw new JdbcException("每页大小必须大于1");
        }
        String selectSql = getPageHandle().handlerPagingSQL(sql, pageNumber, pageSize);
        String countSql = getPageHandle().handlerCountSQL(sql);
        // 查询数据列表
        List<T> resultList = getJdbcTemplate().query(selectSql, new BeanPropertyRowMapper<T>(clazz));
        // 查询总共数量
        int totalSize = getJdbcTemplate().queryForObject(countSql, Integer.class);

        Page<T> bean = new Page<>(pageNumber, pageSize);
        bean.setRecords(resultList);
        bean.setTotal(totalSize);
        return bean;
    }

    /**
     * 分页查询（带参数）
     *
     * @param sql        要执行的SQL
     * @param clazz      实体类
     * @param pageNumber 当前页
     * @param pageSize   页大小
     * @param params     ？参数
     * @param <T>        泛型
     * @return Page<T>
     */
    public <T> Page<T> paginate(String sql, Class<T> clazz, Integer pageNumber, Integer pageSize, final Object... params) {
        if (pageNumber <= 0) {
            throw new JdbcException("当前页数必须大于1");
        }
        if (pageSize <= 0) {
            throw new JdbcException("每页大小必须大于1");
        }
        String selectSql = getPageHandle().handlerPagingSQL(sql, pageNumber, pageSize);
        String countSql = getPageHandle().handlerCountSQL(sql);
        // 查询数据列表
        List<T> resultList = getJdbcTemplate().query(selectSql, params, new BeanPropertyRowMapper<T>(clazz));
        // 查询总共数量
        int totalSize = getJdbcTemplate().queryForObject(countSql, params, Integer.class);

        Page<T> bean = new Page<>(pageNumber, pageSize);
        bean.setRecords(resultList);
        bean.setTotal(totalSize);
        return bean;
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
        if (params == null || params.length == 0) {
            num = getJdbcTemplate().update(sql);
        } else {
            num = getJdbcTemplate().update(sql, new PreparedStatementSetter() {
                public void setValues(PreparedStatement ps) throws SQLException {
                    for (int i = 0; i < params.length; i++)
                        ps.setObject(i + 1, params[i]);
                }
            });
        }
        return num;
    }

    @Override
    public int insert(String sql, final Object... params) {
        int num = 0;
        if (params == null || params.length == 0) {
            num = getJdbcTemplate().update(sql);
        } else {
            num = getJdbcTemplate().update(sql, params);
        }
        return num;
    }

    @Override
    public int update(String sql, final Object... params) {
        int num = 0;
        if (params == null || params.length == 0) {
            num = getJdbcTemplate().update(sql);
        } else {
            num = getJdbcTemplate().update(sql, params);
        }
        return num;
    }

    @Override
    public int delete(String sql, final Object... params) {
        int num = 0;
        if (params == null || params.length == 0) {
            num = getJdbcTemplate().update(sql);
        } else {
            num = getJdbcTemplate().update(sql, params);
        }
        return num;
    }


    /**
     * 使用 in 进行批量操作，比如批量启用，批量禁用，批量删除等 -- 更灵活的就需要自己写了
     *
     * @param sql    示例： update s_url_map set del_flag = '1' where id in (:idList)
     * @param idList 一般为 List<String> 或 List<Integer>
     * @return 执行的结果条数
     */
    @Override
    public int batchOpera(String sql, List<Object> idList) {
        if (idList == null || idList.size() == 0) {
            throw new JdbcException("batchOpera idList cannot be null or empty");
        }
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate());
        Map<String, Object> param = new HashMap<>();
        param.put("idList", idList);
        return namedJdbcTemplate.update(sql, param);
    }
    // ---------------------------------ISqlSupport结束---------------------------------


    // ---------------------------------IObjectSupport开始---------------------------------
    @Override
    public <T> T selectById(Object id, Class<T> clazz) {
        if (id == null) {
            throw new JdbcException("selectById id cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectByIdSql(id, clazz);
        List<T> list = getJdbcTemplate().query(sqlProvider.getSql(), sqlProvider.getParameters().toArray(),
                new BeanPropertyRowMapper<T>((Class<T>) clazz));
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T selectOne(T entity) {
        if (entity == null) {
            throw new JdbcException("selectOne entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectSql(entity);
        List<T> list = getJdbcTemplate().query(sqlProvider.getSql(), sqlProvider.getParameters().toArray(),
                new BeanPropertyRowMapper<T>((Class<T>) entity.getClass()));
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> select(T entity) {
        if (entity == null) {
            throw new JdbcException("select entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.selectSql(entity);
        return getJdbcTemplate().query(sqlProvider.getSql(), sqlProvider.getParameters().toArray(),
                new BeanPropertyRowMapper<T>((Class<T>) entity.getClass()));
    }

    @Override
    public <T> int insert(T entity) {
        return insert(entity, true);
    }

    @Override
    public <T> int insert(T entity, boolean ignoreNulls) {
        if (entity == null) {
            throw new JdbcException("insert entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.insertSql(entity, ignoreNulls);
        if (sqlProvider.getParameters() == null || sqlProvider.getParameters().isEmpty()) {
            throw new JdbcException("insert parameters cannot be null");
        }
        return execute(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public <T> Long insertReturnAutoIncrement(T entity) {
        if (entity == null) {
            throw new JdbcException("insert entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.insertSql(entity, true);
        if (sqlProvider.getParameters() == null || sqlProvider.getParameters().isEmpty()) {
            throw new JdbcException("insert parameters cannot be null");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        final Object[] params = sqlProvider.getParameters().toArray();
        if (params == null || params.length == 0) {
            getJdbcTemplate().update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement(sqlProvider.getSql(),
                            PreparedStatement.RETURN_GENERATED_KEYS);
                    return ps;
                }
            }, keyHolder);

        } else {
            getJdbcTemplate().update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement(sqlProvider.getSql(),
                            PreparedStatement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < params.length; i++)
                        ps.setObject(i + 1, params[i]);
                    return ps;
                }
            }, keyHolder);
        }
        return keyHolder.getKey().longValue();
    }


    @Override
    public <T> int update(T entity) {
        return update(entity, true);
    }

    @Override
    public <T> int update(T entity, boolean ignoreNulls) {
        if (entity == null) {
            throw new JdbcException("update entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.updateSql(entity, ignoreNulls);
        if (sqlProvider.getParameters() == null || sqlProvider.getParameters().isEmpty()) {
            throw new JdbcException("update parameters cannot be null");
        }

        return execute(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public <T> int delete(T entity) {
        if (entity == null) {
            throw new JdbcException("delete entity cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.deleteSql(entity);
        if (sqlProvider.getParameters() == null || sqlProvider.getParameters().isEmpty()) {
            throw new JdbcException("delete parameters cannot be null");
        }
        return execute(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public <T> int deleteById(Object id, Class<T> clazz) {
        if (id == null) {
            throw new JdbcException("deleteById id cannot be null");
        }
        SqlProvider sqlProvider = SqlGenerator.deleteByIdSql(id, clazz);
        if (sqlProvider.getParameters() == null || sqlProvider.getParameters().isEmpty()) {
            throw new JdbcException("deleteById parameters cannot be null");
        }
        return execute(sqlProvider.getSql(), sqlProvider.getParameters().toArray());
    }

    @Override
    public <T> int[] batchUpdate(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            throw new JdbcException("updateBatch collection cannot be null");
        }
        int row[] = null;
        List<Object[]> batchArgs = new ArrayList<Object[]>();
        String sql = "";
        for (T t : collection) {
            SqlProvider sqlProvider = SqlGenerator.updateSql(t, true);
            if (StringUtils.isEmpty(sql)) {
                sql = sqlProvider.getSql();
            }
            batchArgs.add(sqlProvider.getParameters().toArray());
        }
        if (batchArgs == null || batchArgs.isEmpty()) {
            throw new JdbcException("batchUpdate batchArgs cannot be null");
        }
        row = getJdbcTemplate().batchUpdate(sql, batchArgs);
        return row;
    }


    @Override
    public <T> int[] batchInsert(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            throw new JdbcException("batchInsert collection cannot be null");
        }
        int row[] = null;
        List<Object[]> batchArgs = new ArrayList<Object[]>();
        String sql = "";
        for (T t : collection) {
            SqlProvider sqlProvider = SqlGenerator.insertSql(t, true);
            if (StringUtils.isEmpty(sql)) {
                sql = sqlProvider.getSql();
            }
            batchArgs.add(sqlProvider.getParameters().toArray());
        }
        if (batchArgs == null || batchArgs.isEmpty()) {
            throw new JdbcException("batchInsert batchArgs cannot be null");
        }
        row = getJdbcTemplate().batchUpdate(sql, batchArgs);
        return row;
    }


    @Override
    public <T> int[] batchDelete(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            throw new JdbcException("batchDelete collection cannot be null");
        }
        int row[] = null;
        List<Object[]> batchArgs = new ArrayList<Object[]>();
        String sql = "";
        for (T t : collection) {
            SqlProvider sqlProvider = SqlGenerator.deleteSql(t);
            if (StringUtils.isEmpty(sql)) {
                sql = sqlProvider.getSql();
            }
            batchArgs.add(sqlProvider.getParameters().toArray());
        }
        if (batchArgs == null || batchArgs.isEmpty()) {
            throw new JdbcException("batchDelete batchArgs cannot be null");
        }
        row = getJdbcTemplate().batchUpdate(sql, batchArgs);
        return row;
    }

}

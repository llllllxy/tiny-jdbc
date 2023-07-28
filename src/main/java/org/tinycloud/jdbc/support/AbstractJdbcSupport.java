package org.tinycloud.jdbc.support;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.tinycloud.jdbc.exception.JdbcException;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.Page;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author liuxingyu01
 * @date 2022-03-11-16:49
 * @description jdbc抽象类，给出默认的支持
 **/
public abstract class AbstractJdbcSupport implements IJdbcSupport {

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

}

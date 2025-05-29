package org.tinycloud.jdbc;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.page.PageCheck;
import org.tinycloud.jdbc.page.PageHandleResult;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.util.DataAccessUtils;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * JdbcTemplate增强工具类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-10-14 10:44
 */
public class JdbcTemplateHelper {

    private final JdbcTemplate jdbcTemplate;

    private final IPageHandle pageHandle;

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    private final IPageHandle getPageHandle() {
        return pageHandle;
    }

    public JdbcTemplateHelper(JdbcTemplate jdbcTemplate, IPageHandle pageHandle) {
        this.jdbcTemplate = jdbcTemplate;
        this.pageHandle = pageHandle;
    }

    public <F> List<F> select(String sql, Class<F> clazz, Object... params) {
        return getJdbcTemplate().query(sql, new BeanPropertyRowMapper<>(clazz), params);
    }

    public <F> F selectOne(String sql, Class<F> clazz, Object... params) {
        List<F> resultList = select(sql, clazz, params);
        return DataAccessUtils.singleResult(resultList);
    }

    public List<Map<String, Object>> selectMap(String sql, Object... params) {
        return getJdbcTemplate().queryForList(sql, params);
    }

    public <F> F selectForObject(String sql, Class<F> clazz, Object... params) {
        return getJdbcTemplate().queryForObject(sql, clazz, params);
    }

    public <F> Page<F> paginate(String sql, Class<F> clazz, Page<F> page, final Object... params) {
        PageCheck.check(page);
        PageHandleResult handleResult = getPageHandle().handle(sql, page.getPageNum(), page.getPageSize());
        // 查询数据列表
        List<F> list = getJdbcTemplate().query(handleResult.getPageSql(), new BeanPropertyRowMapper<>(clazz), params);
        // 查询总共数量
        Long count = getJdbcTemplate().queryForObject(handleResult.getCountSql(), Long.class, params);
        page.setRecords(list);
        page.setTotal(count);
        return page;
    }

    public Page<Map<String, Object>> paginateMap(String sql, Page<Map<String, Object>> page, Object... params) {
        PageCheck.check(page);
        PageHandleResult handleResult = getPageHandle().handle(sql, page.getPageNum(), page.getPageSize());
        // 查询数据列表
        List<Map<String, Object>> list = getJdbcTemplate().queryForList(handleResult.getPageSql(), params);
        // 查询总共数量
        Long count = getJdbcTemplate().queryForObject(handleResult.getCountSql(), Long.class, params);
        page.setRecords(list);
        page.setTotal(count);
        return page;
    }

    public int execute(String sql, final Object... params) {
        return getJdbcTemplate().update(sql, params);
    }

    public int insert(String sql, final Object... params) {
        return execute(sql, params);
    }

    public int update(String sql, final Object... params) {
        return execute(sql, params);
    }

    public int delete(String sql, final Object... params) {
        return execute(sql, params);
    }

    public int execute(SQL sql) {
        return execute(sql.toSql(), sql.getParameters().toArray());
    }

    public int insert(SQL sql) {
        return insert(sql.toSql(), sql.getParameters().toArray());
    }

    public int update(SQL sql) {
        return update(sql.toSql(), sql.getParameters().toArray());
    }

    public int delete(SQL sql) {
        return delete(sql.toSql(), sql.getParameters().toArray());
    }

    public <F> List<F> select(SQL sql, Class<F> clazz) {
        return select(sql.toSql(), clazz, sql.getParameters().toArray());
    }

    public <F> Page<F> paginate(SQL sql, Class<F> clazz, Page<F> page) {
        return paginate(sql.toSql(), clazz, page, sql.getParameters().toArray());
    }

    public <F> F selectForObject(SQL sql, Class<F> clazz) {
        return selectForObject(sql.toSql(), clazz, sql.getParameters().toArray());
    }

    public <F> F selectOne(SQL sql, Class<F> clazz) {
        List<F> resultList = select(sql, clazz);
        return DataAccessUtils.singleResult(resultList);
    }
}

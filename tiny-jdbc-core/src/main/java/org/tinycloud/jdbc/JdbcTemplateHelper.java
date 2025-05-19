package org.tinycloud.jdbc;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.ObjectUtils;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.page.PageHandleResult;

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
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        return namedJdbcTemplate;
    }


    private final IPageHandle getPageHandle() {
        return pageHandle;
    }

    public JdbcTemplateHelper(JdbcTemplate jdbcTemplate, IPageHandle pageHandle) {
        this.jdbcTemplate = jdbcTemplate;
        this.pageHandle = pageHandle;
    }

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

    public List<Map<String, Object>> selectMap(String sql, Object... params) {
        return getJdbcTemplate().queryForList(sql, params);
    }

    public <F> F selectOneColumn(String sql, Class<F> clazz, Object... params) {
        F result;
        if (ObjectUtils.isEmpty(params)) {
            result = getJdbcTemplate().queryForObject(sql, clazz);
        } else {
            result = getJdbcTemplate().queryForObject(sql, clazz, params);
        }
        return result;
    }

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
        if (page == null || page.getPageNum() == null || page.getPageSize() == null) {
            throw new TinyJdbcException("paginate page cannot be null");
        }
        if (page.getPageNum() <= 0) {
            throw new TinyJdbcException("pageNum must be greater than 0");
        }
        if (page.getPageSize() <= 0) {
            throw new TinyJdbcException("pageSize must be greater than 0");
        }
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
        int num = 0;
        if (ObjectUtils.isEmpty(params)) {
            num = getJdbcTemplate().update(sql);
        } else {
            num = getJdbcTemplate().update(sql, params);
        }
        return num;
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
}

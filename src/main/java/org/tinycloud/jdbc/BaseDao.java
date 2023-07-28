package org.tinycloud.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.support.AbstractSqlSupport;

public class BaseDao extends AbstractSqlSupport {

    /**
     * JdbcTemplate
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 分页处理
     */
    protected final IPageHandle pageHandle;

    public BaseDao(JdbcTemplate jdbcTemplate, IPageHandle pageHandle) {
        this.jdbcTemplate = jdbcTemplate;
        this.pageHandle = pageHandle;
    }

    @Override
    protected JdbcTemplate getJdbcTemplate() {
        return this.jdbcTemplate;
    }

    @Override
    protected IPageHandle getPageHandle() {
        return this.pageHandle;
    }
}

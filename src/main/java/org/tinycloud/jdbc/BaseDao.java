package org.tinycloud.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.support.AbstractSqlSupport;

public class BaseDao<T> extends AbstractSqlSupport<T> {

    /**
     * JdbcTemplate
     */
    @Autowired
    private  JdbcTemplate jdbcTemplate;

    /**
     * 分页处理
     */
    @Autowired
    private IPageHandle pageHandle;

    @Override
    protected JdbcTemplate getJdbcTemplate() {
        return this.jdbcTemplate;
    }

    @Override
    protected IPageHandle getPageHandle() {
        return this.pageHandle;
    }
}

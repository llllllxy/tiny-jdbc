package org.tinycloud.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.PageHandleFactory;
import org.tinycloud.jdbc.support.AbstractSqlSupport;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * BaseDao 基础类，DAO 层继承该类即可获得增强的 CRUD 功能。
 * </p>
 *
 * <p>为保持用户继承 DAO 时无需声明构造器的兼容性，运行时上下文在该适配层注入；
 * 具体执行组件均通过 {@link TinyJdbcRuntime} 获取配置和扩展 SPI，不再读取静态全局状态。</p>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author liuxingyu01
 */
public class BaseDao<T, ID extends Serializable> extends AbstractSqlSupport<T, ID> {

    /**
     * Spring JDBC 模板。
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 默认分页处理器。
     */
    @Autowired
    private IPageHandle pageHandle;

    /**
     * TinyJDBC 运行时上下文。
     */
    @Autowired
    private TinyJdbcRuntime tinyJdbcRuntime;

    /**
     * SQL 拦截器列表。
     */
    @Autowired(required = false)
    private List<SqlInterceptor> sqlInterceptors;

    /**
     * 获取 JdbcTemplate 实例。
     *
     * @return 当前类中注入的 JdbcTemplate 实例
     */
    @Override
    protected JdbcTemplate getJdbcTemplate() {
        return this.jdbcTemplate;
    }

    /**
     * 获取分页处理器实例。
     *
     * @return 当前数据源对应的分页处理器
     */
    @Override
    protected IPageHandle getPageHandle() {
        return !this.tinyJdbcRuntime.isOpenRuntimeDbType() && this.pageHandle != null
                ? this.pageHandle
                : PageHandleFactory.getDynamicPageHandle(this.getJdbcTemplate(), this.tinyJdbcRuntime);
    }

    /**
     * 获取 TinyJDBC 运行时上下文。
     *
     * @return 当前应用上下文的 TinyJDBC 运行时上下文
     */
    @Override
    protected TinyJdbcRuntime getTinyJdbcRuntime() {
        return this.tinyJdbcRuntime;
    }

    /**
     * 获取 SQL 拦截器列表。
     *
     * @return 当前类中注入的 SQLInterceptor 实例列表
     */
    @Override
    protected List<SqlInterceptor> getSqlInterceptors() {
        return this.sqlInterceptors;
    }

    /**
     * 获取 NamedParameterJdbcTemplate 实例。
     *
     * @return 基于当前 JdbcTemplate 实例创建的 NamedParameterJdbcTemplate 实例
     */
    @Override
    protected NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(this.getJdbcTemplate());
    }
}

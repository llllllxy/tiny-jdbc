package org.tinycloud.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.support.AbstractSqlSupport;

import java.io.Serializable;

/**
 * <p>
 * baseDao基础类，dao层继承该接口即可获得增强的CURD功能
 * </p>
 * <p>
 * 原理：
 * Spring的AutowiredAnnotationBeanPostProcessor.AutowiredFieldElement.inject会对一个类的本身的字段和其所有父类的字段进行遍历，
 * 凡是含有@Autowired的字段都会被注入。
 * </p>
 *
 * @param <T>
 * @param <ID>
 */
public class BaseDao<T, ID extends Serializable> extends AbstractSqlSupport<T, ID> {

    /**
     * JdbcTemplate
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分页处理
     */
    @Autowired
    private IPageHandle pageHandle;

    /**
     * 获取 JdbcTemplate 实例
     *
     * @return 当前类中注入的 JdbcTemplate 实例
     */
    @Override
    protected JdbcTemplate getJdbcTemplate() {
        return this.jdbcTemplate;
    }

    /**
     * 获取分页处理器实例
     *
     * @return 当前类中注入的 IPageHandle 实例
     */
    @Override
    protected IPageHandle getPageHandle() {
        return this.pageHandle;
    }


    /**
     * 获取 NamedParameterJdbcTemplate 实例
     * NamedParameterJdbcTemplate 是 JdbcTemplate 的扩展，支持使用命名参数。
     *
     * @return 基于当前 JdbcTemplate 实例创建的 NamedParameterJdbcTemplate 实例
     */
    @Override
    protected NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(getJdbcTemplate());
    }
}

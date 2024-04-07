package org.tinycloud.jdbc.plugins;

import org.tinycloud.jdbc.sql.SqlProvider;

/**
 * <p>
 * sql拦截器插件接口声明
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-06 09:27
 */
public interface SQLInterceptor {

    /**
     * 在真正构建sql和参数之前执行，该方法会影响最终的sql和参数
     *
     * @param sqlType     sql类型
     * @param sqlProvider sql内容，如若修改，需要重新set进去（引用传递）
     */
    void before(SQLType sqlType, SqlProvider sqlProvider);

    /**
     * 在真正执行sql之后执行
     * <br/>
     * 分页方法只会返回分页的结果，不会返回count(XXX)的结果
     *
     * @param sqlProvider sql内容
     * @param result      执行结果
     */
    void after(SqlProvider sqlProvider, Object result);
}

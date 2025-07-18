package org.tinycloud.jdbc.page;

/**
 * <p>
 * 分页处理器-接口声明
 * </p>
 *
 * @author liuxingyu01
 * @since 2022-05-10 8:31
 **/
public interface IPageHandle {
    /**
     * 将传入的SQL做分页处理
     *
     * @param sql      oldSql 原SQL
     * @param pageNo   pageNo 页码
     * @param pageSize pageSize 每页数量
     * @return 带有分页逻辑的新 SQL 语句。
     */
    PagingSQLProvider handlerPagingSQL(String sql, long pageNo, long pageSize);

    /**
     * 将传入的 SQL 做 COUNT 处理
     * 根据传入的原 SQL 语句，生成用于统计符合条件记录总数的 COUNT SQL 语句。
     *
     * @param sql oldSql 原 SQL 语句，即未添加 COUNT 逻辑的原始查询语句。
     * @return 用于统计记录总数的 COUNT SQL 语句。
     */
    String handlerCountSQL(String sql);

    /**
     * 综合处理分页和 COUNT 查询
     * 调用 handlerPagingSQL 方法生成带有分页逻辑的 SQL 语句，
     * 调用 handlerCountSQL 方法生成用于统计记录总数的 COUNT SQL 语句，
     * 并将这两个 SQL 语句封装到 PageHandleResult 对象中返回。
     *
     * @param sql      oldSql 原 SQL 语句，即未添加分页和 COUNT 逻辑的原始查询语句。
     * @param pageNo   pageNo 第几页，用于计算查询的起始位置，从 1 开始计数。
     * @param pageSize pageSize 每页数量，即每页要查询的记录数。
     * @return 封装了分页 SQL 语句和 COUNT SQL 语句的 PageHandleResult 对象。
     */
    default PageHandleResult handle(String sql, long pageNo, long pageSize) {
        PagingSQLProvider pageSQl = this.handlerPagingSQL(sql, pageNo, pageSize);
        String countSql = this.handlerCountSQL(sql);
        return PageHandleResult.create(pageSQl, countSql);
    }
}

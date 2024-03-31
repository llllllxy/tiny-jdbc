package org.tinycloud.jdbc.page;

/**
 * @author liuxingyu01
 * @date 2022-05-10 8:31
 * @description
 **/
public interface IPageHandle {
    /**
     * 将传入的SQL做分页处理
     *
     * @param sql oldSql 原SQL
     * @param pageNo pageNo 第几页，用来计算first 这个值由（pageNo-1）*pageSize
     * @param pageSize pageSize 每页数量
     * */
    String handlerPagingSQL(String sql, long pageNo, long pageSize);


    /**
     * 将传入的SQL做COUNT处理
     *
     * @param sql oldSql 原SQL
     * */
    String handlerCountSQL(String sql);
}

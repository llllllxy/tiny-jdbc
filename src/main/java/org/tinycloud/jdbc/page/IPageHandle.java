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
    String handlerPagingSQL(String sql, int pageNo, int pageSize);


    /**
     * 将传入的SQL做COUNT处理
     *
     * @param sql oldSql 原SQL
     * */
    String handlerCountSQL(String sql);


    /**
     * 根据传入的SQL，返回Page对象
     * @param oldSQL 原SQL
     * @param pageNo 第几页，用来计算first 这个值由（pageNo-1）*pageSize
     * @param pageSize pageSize 每页数量
     * @return Page
     */
    Page getPage(String oldSQL, int pageNo, int pageSize);
}

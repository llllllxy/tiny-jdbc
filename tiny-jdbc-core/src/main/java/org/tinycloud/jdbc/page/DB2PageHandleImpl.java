package org.tinycloud.jdbc.page;


/**
 * 分页查询适配器-DB2
 * @author liuxingyu01
 * @since  2022-05-14 16:23
 **/
public class DB2PageHandleImpl implements IPageHandle {

    /**
     * 分页查询适配
     *
     * @param oldSQL   需要改造为分页查询的SQL
     * @param pageNo   pageNo 第几页，用来计算first 这个值由（pageNo-1）*pageSize
     * @param pageSize pageSize 每页数量
     * @return 处理过后的sql
     */
    @Override
    public PagingSQLProvider handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        long pageStart = (pageNo - 1) * pageSize + 1;
        long pageEnd = pageStart + pageSize - 1;
        StringBuilder sql = new StringBuilder("SELECT * FROM ( SELECT B.*, ROWNUMBER() OVER() AS RN FROM ( ");
        sql.append(oldSQL);
        sql.append(" ) AS B ) AS A WHERE A.RN BETWEEN ").append("?").append(" AND ")
                .append("?");
        return PagingSQLProvider.create(sql.toString(), pageStart, pageEnd);
    }


    @Override
    public String handlerCountSQL(String oldSQL) {
        StringBuilder newSql = new StringBuilder();
        newSql.append("SELECT COUNT(*) FROM ( ");
        newSql.append(oldSQL);
        newSql.append(" ) TEMP");
        return newSql.toString();
    }
}

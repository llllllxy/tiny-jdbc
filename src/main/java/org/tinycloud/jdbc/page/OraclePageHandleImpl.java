package org.tinycloud.jdbc.page;


/**
 * 分页查询适配器-ORACLE
 *
 * @author liuxingyu01
 * @since 2022-05-14 21:54
 **/
public class OraclePageHandleImpl implements IPageHandle {

    /**
     * 分页查询适配
     *
     * @param oldSQL   需要改造为分页查询的SQL
     * @param pageNo   pageNo 第几页，用来计算first 这个值由（pageNo-1）*pageSize
     * @param pageSize pageSize 每页数量
     * @return
     */
    @Override
    public String handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ( SELECT TMP_TB.*, ROWNUM ROW_ID FROM ( ");
        sql.append(oldSQL);
        long pageStart = (pageNo - 1) * pageSize + 1;
        long pageEnd = pageNo * pageSize;
        sql.append(" ) TMP_TB WHERE ROWNUM <=  ").append(pageEnd).append(" ) WHERE ROW_ID >= ")
                .append(pageStart);
        return sql.toString();
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

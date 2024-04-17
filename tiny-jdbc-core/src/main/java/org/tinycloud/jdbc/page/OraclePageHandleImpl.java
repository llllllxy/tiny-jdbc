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
     * @return 处理过后的sql
     */
    @Override
    public String handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        long pageStart = (pageNo - 1L) * pageSize;
        long pageEnd = pageNo * pageSize;
        StringBuilder sql = new StringBuilder("SELECT * FROM ( SELECT TMP_TB.*, ROWNUM ROW_ID FROM ( ");
        sql.append(oldSQL);
        sql.append(" ) TMP_TB WHERE ROWNUM <=  ")
                .append(pageEnd)
                .append(" ) WHERE ROW_ID > ")
                .append(pageStart);
        return sql.toString();
    }

    @Override
    public String handlerCountSQL(String oldSQL) {
        return "SELECT COUNT(*) FROM ( " + oldSQL + " ) TEMP";
    }
}

package org.tinycloud.jdbc.page;

/**
 * 分页查询适配器-sqlite
 *
 * @author liuxingyu01
 * @since 2023-08-03
 **/
public class SqlitePageHandleImpl implements IPageHandle {

    /**
     * 分页查询适配
     *
     * @param oldSQL   需要改造为分页查询的SQL
     * @param pageNo   pageNo 第几页，用来计算offset，这个值由（pageNo-1）*pageSize
     * @param pageSize pageSize 每页数量
     * @return 处理过后的sql
     */
    @Override
    public String handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        StringBuilder sql = new StringBuilder(oldSQL);

        long offset = (pageNo - 1) * pageSize;
        long limit = pageSize;
        if (limit > 0) {
            sql.append("\n LIMIT ").append(limit);
        }
        if (offset > 0) {
            sql.append("\n OFFSET ").append(offset);
        }
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

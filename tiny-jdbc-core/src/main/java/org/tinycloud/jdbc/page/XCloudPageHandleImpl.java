package org.tinycloud.jdbc.page;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-17 12:54
 */
public class XCloudPageHandleImpl implements IPageHandle {
    @Override
    public String handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        StringBuilder sql = new StringBuilder();
        long offset = (pageNo - 1) * pageSize;
        long limit = pageSize;

        sql.append(" LIMIT ");
        if (offset != 0) {
            sql.append(" ( ").append(offset + 1).append(",").append(offset + limit).append(" ) ");
        } else {
            sql.append(limit);
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

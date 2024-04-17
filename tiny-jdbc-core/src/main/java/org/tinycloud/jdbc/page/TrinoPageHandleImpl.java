package org.tinycloud.jdbc.page;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-17 15:42
 */
public class TrinoPageHandleImpl implements IPageHandle {
    @Override
    public String handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        StringBuilder sql = new StringBuilder(oldSQL);
        long offset = (pageNo - 1L) * pageSize;
        long limit = pageSize;
        if (offset != 0L) {
            sql.append(" OFFSET ").append(limit).append(" LIMIT ").append(offset);
        } else {
            sql.append(" LIMIT ").append(limit);
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

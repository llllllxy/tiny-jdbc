package org.tinycloud.jdbc.page;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-17 12:47
 */
public class Oracle12cPageHandleImpl implements IPageHandle {

    @Override
    public String handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        StringBuilder sql = new StringBuilder();
        long offset = (pageNo - 1L) * pageSize;
        long limit = pageSize;

        sql.append(oldSQL);
        sql.append(" OFFSET ");
        sql.append(offset);
        sql.append(" ROWS FETCH NEXT ");
        sql.append(limit);
        sql.append(" ROWS ONLY");
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

package org.tinycloud.jdbc.page;

/**
 * <p>
 *     行云数据库分页处理实现类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-17 12:54
 */
public class XCloudPageHandleImpl implements IPageHandle {
    @Override
    public PagingSQLProvider handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        StringBuilder sql = new StringBuilder(oldSQL);
        long offset = (pageNo - 1L) * pageSize;
        long limit = pageSize;

        sql.append(" LIMIT ");
        if (offset != 0L) {
            sql.append(" ( ").append("?").append(",").append("?").append(" ) ");
            return PagingSQLProvider.create(sql.toString(), offset + 1L, offset + limit);
        } else {
            sql.append("?");
            return PagingSQLProvider.create(sql.toString(), limit);
        }
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

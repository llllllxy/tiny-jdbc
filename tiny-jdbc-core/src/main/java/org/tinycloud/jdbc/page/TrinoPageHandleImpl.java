package org.tinycloud.jdbc.page;

/**
 * <p>
 *     Trino数据库分页处理实现类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-17 15:42
 */
public class TrinoPageHandleImpl implements IPageHandle {
    @Override
    public PagingSQLProvider handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        StringBuilder sql = new StringBuilder(oldSQL);
        long offset = (pageNo - 1L) * pageSize;
        long limit = pageSize;
        if (offset != 0L) {
            sql.append(" OFFSET ").append("?").append(" LIMIT ").append("?");
            return PagingSQLProvider.create(sql.toString(), offset, limit);
        } else {
            sql.append(" LIMIT ").append("?");
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

package org.tinycloud.jdbc.page;

/**
 * <p>
 * 分页查询适配器-华为云GaussDB数据库
 * 分页语法格式: SELECT query_list FROM table_name [ LIMIT { [offset,] count | ALL } ]
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-08-04 10:16
 */
public class GaussDBPageHandleImpl implements IPageHandle {
    @Override
    public PagingSQLProvider handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        StringBuilder sql = new StringBuilder(oldSQL);
        long offset = (pageNo - 1) * pageSize;
        long limit = pageSize;
        if (offset != 0L) {
            sql.append(" LIMIT ").append("?").append(",").append("?");
            return PagingSQLProvider.create(sql.toString(), offset, limit);
        } else {
            sql.append(" LIMIT ").append("?");
            return PagingSQLProvider.create(sql.toString(), limit);
        }
    }

    @Override
    public String handlerCountSQL(String oldSQL) {
        return "SELECT COUNT(*) FROM ( " + oldSQL + " ) TEMP";
    }
}

package org.tinycloud.jdbc.page;

/**
 * <p>
 * 分页查询适配器-GBase8s
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-17 11:45
 */
public class GBase8sPageHandleImpl implements IPageHandle {
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
        long offset = (pageNo - 1) * pageSize;
        long limit = pageSize;

        StringBuilder sql = (new StringBuilder(oldSQL)).insert(6, " SKIP " + offset + " FIRST " + limit);
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

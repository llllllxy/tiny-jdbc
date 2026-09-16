package org.tinycloud.jdbc.page;

/**
 * <p>
 * 分页查询适配器-InforMix
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-17 11:48
 */
public class InforMixPageHandleImpl implements IPageHandle {
    /**
     * 分页查询适配
     *
     * @param oldSQL   需要改造为分页查询的SQL
     * @param pageNo   pageNo 第几页，用来计算offset，这个值由（pageNo-1）*pageSize
     * @param pageSize pageSize 每页数量
     * @return 处理过后的sql
     */
    @Override
    public PagingSQLProvider handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        long offset = PageCheck.offset(pageNo, pageSize);
        long limit = pageSize;
        // Informix 的分页语法紧跟顶层 SELECT，无法使用后置参数。
        int selectIndex = PageSqlUtils.findTopLevelSelect(oldSQL);
        String sql = new StringBuilder(oldSQL)
                .insert(selectIndex + "SELECT".length(), " SKIP " + offset + " FIRST " + limit)
                .toString();
        return PagingSQLProvider.create(sql);
    }

    @Override
    public String handlerCountSQL(String oldSQL) {
        return "SELECT COUNT(*) FROM ( " +
                oldSQL +
                " ) TEMP";
    }
}

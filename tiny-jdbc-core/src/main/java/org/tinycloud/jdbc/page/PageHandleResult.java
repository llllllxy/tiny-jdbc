package org.tinycloud.jdbc.page;


/**
 * <p>
 * 分页处理结果实体类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-05-29 16:10
 */
public class PageHandleResult {

    private PagingSQLProvider pageSqlProvider;

    private String countSql;

    public PagingSQLProvider getPageSqlProvider() {
        return pageSqlProvider;
    }

    public void setPageSqlProvider(PagingSQLProvider pageSqlProvider) {
        this.pageSqlProvider = pageSqlProvider;
    }

    public String getCountSql() {
        return countSql;
    }

    public void setCountSql(String countSql) {
        this.countSql = countSql;
    }

    public String getPageSql() {
        return pageSqlProvider.getSql();
    }

    public Object[] getParameters() {
        return pageSqlProvider.getParameters();
    }

    public static PageHandleResult create(PagingSQLProvider pageSqlProvider, String countSql) {
        PageHandleResult result = new PageHandleResult();
        result.setPageSqlProvider(pageSqlProvider);
        result.setCountSql(countSql);
        return result;
    }

    @Override
    public String toString() {
        return "PageHandleResult{" +
                "pageSql='" + pageSqlProvider.getSql() + '\'' +
                ", parameters='" + pageSqlProvider.getParameters() + '\'' +
                ", countSql='" + countSql + '\'' +
                '}';
    }
}

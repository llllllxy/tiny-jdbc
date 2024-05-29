package org.tinycloud.jdbc.page;


/**
 * <p>
 *     分页处理结果实体类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-05-29 16:10
 */
public class PageHandleResult {

    private String pageSql;

    private String countSql;

    public String getPageSql() {
        return pageSql;
    }

    public void setPageSql(String pageSql) {
        this.pageSql = pageSql;
    }

    public String getCountSql() {
        return countSql;
    }

    public void setCountSql(String countSql) {
        this.countSql = countSql;
    }

    public static PageHandleResult create(String pageSql, String countSql) {
        PageHandleResult result = new PageHandleResult();
        result.setPageSql(pageSql);
        result.setCountSql(countSql);
        return result;
    }

    @Override
    public String toString() {
        return "PageHandleResult{" +
                "pageSql='" + pageSql + '\'' +
                ", countSql='" + countSql + '\'' +
                '}';
    }
}

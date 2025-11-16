package org.tinycloud.jdbc.sql;

/**
 * <p>
 * 排序规则
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:00
 */
public class OrderBy {
    private String column;
    private boolean isDesc;

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public boolean isDesc() {
        return isDesc;
    }

    public void setDesc(boolean desc) {
        isDesc = desc;
    }

    /**
     * 构造函数，用于创建一个 OrderBy 对象。
     *
     * @param column 排序所依据的数据库列名
     * @param isDesc 排序方式，true 为降序，false 为升序
     */
    public OrderBy(String column, boolean isDesc) {
        this.column = column;
        this.isDesc = isDesc;
    }
}

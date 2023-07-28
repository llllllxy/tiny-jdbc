package org.tinycloud.jdbc.page;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author liuxingyu01
 * @date 2022-05-10 9:17
 * @description 分页对象，支持offset-limit模式
 **/
public class OffsetPage<T> implements Serializable {
    private static final long serialVersionUID = -1L;


    /**
     * 偏移位置（offset = (pageNo - 1) * pageSize;）
     */
    private Integer offset;

    /**
     * 偏移量（等价于pageSize）
     */
    private Integer limit;

    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 分页后的数据
     */
    private Collection<T> records;

    public OffsetPage() {
    }

    public OffsetPage(Integer offset, Integer limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public OffsetPage(Collection<T> records, int total, Integer offset, Integer limit) {
        this.records = (records == null ? new ArrayList<T>(0) : records);
        this.total = total;
        this.offset = offset;
        this.limit = limit;
        this.pages = (total + limit - 1) / limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public Collection<T> getRecords() {
        return records;
    }

    public void setRecords(Collection<T> records) {
        this.records = records;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
        this.pages = (total + limit - 1) / limit;
    }

    @Override
    public String toString() {
        return "Page {offset=" + offset + ", limit=" + limit + ", total=" + total + ", pages=" + pages
                + ", records=" + records + "}";
    }
}

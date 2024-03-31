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
    private Long offset;

    /**
     * 偏移量（等价于pageSize）
     */
    private Long limit;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 分页后的数据
     */
    private Collection<T> records;

    public OffsetPage() {
    }

    public OffsetPage(Long offset, Long limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public OffsetPage(Collection<T> records, Long total, Long offset, Long limit) {
        this.records = (records == null ? new ArrayList<T>(0) : records);
        this.total = total;
        this.offset = offset;
        this.limit = limit;
        this.pages = (total + limit - 1) / limit;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public void setPages(Long pages) {
        this.pages = pages;
    }

    public Collection<T> getRecords() {
        return records;
    }

    public void setRecords(Collection<T> records) {
        this.records = records;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
        this.pages = (total + limit - 1) / limit;
    }

    @Override
    public String toString() {
        return "Page {offset=" + offset + ", limit=" + limit + ", total=" + total + ", pages=" + pages
                + ", records=" + records + "}";
    }
}

package org.tinycloud.jdbc.page;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 分页对象，支持pageNum-pageSize模式
 * @author liuxingyu01
 * @since  2022-05-10 9:17
 **/
public class Page<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 当前页（pageNo = offset / limit + 1;）
     */
    private Long pageNum;

    /**
     * 分页大小（等价于limit）
     */
    private Long pageSize;

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

    public Page() {

    }

    public Page(Long pageNum, Long pageSize) {
        this.pageSize = pageSize;
        this.pageNum = pageNum;
    }

    public Page(Collection<T> records, Long total, Long pageNum, Long pageSize) {
        this.records = (records == null ? new ArrayList<T>(0) : records);
        this.total = total;
        this.pageSize = pageSize;
        this.pageNum = pageNum;
        this.pages = (total + pageSize - 1) / pageSize;
    }

    public Long getPageNum() {
        return pageNum;
    }

    public void setPageNum(Long pageNum) {
        this.pageNum = pageNum;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public Long getPages() {
        return pages;
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
        this.pages = (total + pageSize - 1) / pageSize;
    }

    @Override
    public String toString() {
        return "Page {pageNum=" + pageNum + ", pageSize=" + pageSize + ", total=" + total + ", pages=" + pages
                + ", records=" + records + "}";
    }

    public static <T> Page<T> of(Long pageNum, Long pageSize) {
        return new Page<>(pageNum, pageSize);
    }
}

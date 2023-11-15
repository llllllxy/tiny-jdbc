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
    private Integer pageNum;

    /**
     * 分页大小（等价于limit）
     */
    private Integer pageSize;

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

    public Page() {

    }

    public Page(Integer pageNum, Integer pageSize) {
        this.pageSize = pageSize;
        this.pageNum = pageNum;
    }

    public Page(Collection<T> records, int total, Integer pageNum, Integer pageSize) {
        this.records = (records == null ? new ArrayList<T>(0) : records);
        this.total = total;
        this.pageSize = pageSize;
        this.pageNum = pageNum;
        this.pages = (total + pageSize - 1) / pageSize;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPages() {
        return pages;
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
        this.pages = (total + pageSize - 1) / pageSize;
    }

    @Override
    public String toString() {
        return "Page {pageNum=" + pageNum + ", pageSize=" + pageSize + ", total=" + total + ", pages=" + pages
                + ", records=" + records + "}";
    }

}

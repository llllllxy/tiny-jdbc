package org.tinycloud.jdbc.page;

import org.tinycloud.jdbc.exception.TinyJdbcException;

/**
 * <p>
 * 分页参数检查工具类，用于验证分页对象的有效性。
 * </p>
 * <p>
 * 除基础的范围校验外，还提供 {@link #offset(long, long)}、{@link #pageEnd(long, long)}、
 * {@link #pages(Long, Long)} 等溢出安全的分页计算，避免 {@code (pageNo - 1) * pageSize} 在
 * 超大页码/页大小时发生 long 溢出，以及 {@code total / pageSize} 在 {@code pageSize} 为 0
 * 或 {@code total} 为 null 时出现除零 / 空指针。
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-28 14:43
 */
public class PageCheck {

    private PageCheck() {
    }

    /**
     * 检查分页对象的有效性。
     * 该方法会验证分页对象是否为 null，以及页码和每页数量是否为有效的正数。
     * 如果分页对象、页码或每页数量不符合要求，将抛出 TinyJdbcException 异常。
     *
     * @param page 要检查的分页对象，泛型类型。
     * @throws TinyJdbcException 当分页对象、页码或每页数量不满足要求时抛出该异常。
     */
    public static void check(Page<?> page) {
        if (page == null || page.getPageNum() == null || page.getPageSize() == null) {
            throw new TinyJdbcException("paginate page cannot be null");
        }
        check(page.getPageNum(), page.getPageSize());
    }

    /**
     * 检查分页页码与页大小的有效性（首页从 1 开始）。
     *
     * @param pageNo   页码，从 1 开始
     * @param pageSize 每页数量
     * @throws TinyJdbcException 当页码或页大小不大于 0 时抛出。
     */
    public static void check(long pageNo, long pageSize) {
        if (pageNo <= 0L) {
            throw new TinyJdbcException("pageNum must be greater than 0");
        }
        if (pageSize <= 0L) {
            throw new TinyJdbcException("pageSize must be greater than 0");
        }
    }

    /**
     * 计算分页偏移量 {@code offset = (pageNo - 1) * pageSize}，并对入参做范围校验与溢出保护。
     * <p>
     * 与直接乘法不同，当 {@code (pageNo - 1) * pageSize} 超出 {@code long} 范围时会抛出
     * {@link TinyJdbcException}，而不是静默得到错误的负数 offset。
     * </p>
     *
     * @param pageNo   页码，从 1 开始
     * @param pageSize 每页数量
     * @return 偏移量
     * @throws TinyJdbcException 页码/页大小非法，或计算结果溢出时抛出。
     */
    public static long offset(long pageNo, long pageSize) {
        check(pageNo, pageSize);
        try {
            return Math.multiplyExact(pageNo - 1L, pageSize);
        } catch (ArithmeticException e) {
            throw new TinyJdbcException("pagination offset overflow: pageNo=" + pageNo + ", pageSize=" + pageSize);
        }
    }

    /**
     * 计算分页结束行号 {@code pageNo * pageSize}，并对入参做范围校验与溢出保护。
     *
     * @param pageNo   页码，从 1 开始
     * @param pageSize 每页数量
     * @return 结束行号
     * @throws TinyJdbcException 页码/页大小非法，或计算结果溢出时抛出。
     */
    public static long pageEnd(long pageNo, long pageSize) {
        check(pageNo, pageSize);
        try {
            return Math.multiplyExact(pageNo, pageSize);
        } catch (ArithmeticException e) {
            throw new TinyJdbcException("pagination pageEnd overflow: pageNo=" + pageNo + ", pageSize=" + pageSize);
        }
    }

    /**
     * 计算总页数 {@code ceil(total / pageSize)}，并对入参做空值、除零与溢出保护。
     * <p>
     * 使用 {@code total / pageSize + (total % pageSize == 0 ? 0 : 1)} 计算，
     * 避免 {@code (total + pageSize - 1) / pageSize} 在 {@code total} 接近
     * {@code Long.MAX_VALUE} 时溢出。
     * </p>
     *
     * @param total    总记录数，可为 null（此时总页数返回 null，不抛异常）
     * @param pageSize 每页数量
     * @return 总页数；当 {@code total} 或 {@code pageSize} 为 null 时返回 null；{@code total} 小于等于 0 时返回 0。
     * @throws TinyJdbcException 当 {@code pageSize} 不大于 0 时抛出。
     */
    public static Long pages(Long total, Long pageSize) {
        if (total == null || pageSize == null) {
            return null;
        }
        if (total <= 0L) {
            return 0L;
        }
        if (pageSize <= 0L) {
            throw new TinyJdbcException("pageSize must be greater than 0");
        }
        long div = total / pageSize;
        return div + (total % pageSize == 0L ? 0L : 1L);
    }
}

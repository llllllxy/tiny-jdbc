package org.tinycloud.jdbc.page;

import org.tinycloud.jdbc.exception.TinyJdbcException;

/**
 * <p>
 * 分页参数检查工具类，用于验证分页对象的有效性
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-28 14:43
 */
public class PageCheck {

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
        if (page.getPageNum() <= 0) {
            throw new TinyJdbcException("pageNum must be greater than 0");
        }
        if (page.getPageSize() <= 0) {
            throw new TinyJdbcException("pageSize must be greater than 0");
        }
    }
}

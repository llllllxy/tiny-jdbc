package org.tinycloud.jdbc.page;

import org.tinycloud.jdbc.exception.TinyJdbcException;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-28 14:43
 */
public class PageCheck {
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

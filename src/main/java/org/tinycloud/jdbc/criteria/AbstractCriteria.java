package org.tinycloud.jdbc.criteria;

import org.springframework.format.datetime.DateFormatter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 条件构造器抽象类，抽象了一些公共的方法
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public abstract class AbstractCriteria {
    private static final String datetimePattern = "yyyy-MM-dd HH:mm:ss";

    private static final String timestampPattern = "yyyy-MM-dd HH:mm:ss:SSS";

    protected String formatValue(Object value) {
        if (value instanceof String) {
            return "'" + value + "'";
        } else if (value instanceof java.util.Date) {
            DateFormatter dateFormatter = new DateFormatter(datetimePattern);
            String datetime = dateFormatter.print((java.util.Date) value, Locale.getDefault());
            return "'" + datetime + "'";
        } else if (value instanceof java.time.LocalDateTime) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datetimePattern);
            String datetime = ((LocalDateTime) value).format(formatter);
            return "'" + datetime + "'";
        } else {
            return String.valueOf(value);
        }
    }
}

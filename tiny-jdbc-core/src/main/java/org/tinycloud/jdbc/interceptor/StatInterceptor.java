package org.tinycloud.jdbc.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.util.SqlUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 统计拦截器，用于统计SQL执行耗时
 * 1. 统计SQL执行耗时
 * 2. 打印SQL执行开始时间、结束时间、耗时、原始SQL、完整SQL
 * 3. 打印SQL执行结果
 * 4. 开启后会有一定的性能影响，建议在开发环境开启，生产环境关闭
 *
 * @author liuxingyu01
 * @since 2025-12-10 14:20
 */
public class StatInterceptor implements SqlInterceptor {
    private static final Logger log = LoggerFactory.getLogger(StatInterceptor.class);

    @Override
    public void before(SqlInvocation invocation, JdbcTemplate jdbcTemplate) {
        log.info("执行SQL开始时间：{}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        log.info("原始SQL：{}", invocation.getSql());
        log.info("原始SQL参数：{}", Arrays.toString(invocation.getArgs()));
        log.info("完整SQL：{}", SqlUtils.replaceSqlParams(invocation.getSql(), invocation.getArgs()));
        invocation.putMetadata("startTime", LocalDateTime.now());
    }

    @Override
    public Object after(Object result, SqlInvocation invocation, JdbcTemplate jdbcTemplate) {
        log.info("执行SQL结束时间：{}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        LocalDateTime startTime = (LocalDateTime) invocation.getMetadata("startTime");
        log.info("执行SQL耗时：{}毫秒", Duration.between(startTime, LocalDateTime.now()).toMillis());
        return result;
    }
}

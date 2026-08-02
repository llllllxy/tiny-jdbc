package org.tinycloud.jdbc.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinycloud.jdbc.util.SqlUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 统计拦截器，用于统计SQL执行耗时
 * 1. 统计SQL执行耗时
 * 2. 打印原始SQL、参数、完整SQL和耗时
 * 3. 按配置打印SQL执行结果
 * 4. 开启后会有一定的性能影响，建议在开发环境开启，生产环境关闭
 *
 * @author liuxingyu01
 * @since 2025-12-10 14:20
 */
public class StatInterceptor implements SqlInterceptor {
    private static final Logger log = LoggerFactory.getLogger(StatInterceptor.class);
    private final boolean printResult;

    /**
     * 创建默认不打印执行结果的 SQL 统计拦截器。
     */
    public StatInterceptor() {
        this(false);
    }

    /**
     * 创建 SQL 统计拦截器。
     *
     * @param printResult 是否打印 SQL 执行结果
     */
    public StatInterceptor(boolean printResult) {
        this.printResult = printResult;
    }

    /**
     * 记录 SQL 执行信息，并按配置打印成功执行结果。
     *
     * @param request 当前 SQL 请求
     * @param chain   后续 SQL 拦截器链
     * @param <R>     执行结果类型
     * @return SQL 执行结果
     */
    @Override
    public <R> R intercept(SqlRequest<R> request, SqlInterceptorChain<R> chain) {
        long startTime = System.nanoTime();
        log.info("原始SQL：{}", request.getSql());
        log.info("原始SQL参数：{}", Arrays.toString(request.getArgs()));
        log.info("完整SQL：{}", SqlUtils.replaceSqlParams(request.getSql(), request.getArgs()));
        try {
            R result = chain.proceed(request);
            if (this.printResult) {
                log.info("执行SQL结果：{}", result);
            }
            return result;
        } catch (RuntimeException e) {
            log.error("SQL执行异常：{}", request.getSql(), e);
            throw e;
        } finally {
            log.info("执行SQL耗时：{}毫秒", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
        }
    }
}

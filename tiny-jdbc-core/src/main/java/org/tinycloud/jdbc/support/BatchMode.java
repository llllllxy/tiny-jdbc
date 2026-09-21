package org.tinycloud.jdbc.support;

/**
 * 批量插入执行模式。
 *
 * <p>控制 {@code batchInsert} 的底层执行方式：</p>
 * <ul>
 *   <li>{@link #JDBC_BATCH}：使用 {@code JdbcTemplate.batchUpdate} 逐条 {@code addBatch}，
 *       依赖 JDBC URL 的 {@code rewriteBatchedStatements=true} 才能真正合并往返；</li>
 *   <li>{@link #MULTI_VALUE}：把多条记录拼成<b>单条多值</b> {@code INSERT ... VALUES (...),(...)}，
 *       天然减少网络往返，不依赖 JDBC URL 参数。</li>
 * </ul>
 *
 * <p>两者对 {@code batchInsert} 返回值的影响不同：{@link #JDBC_BATCH} 可返回真实逐行影响行数；
 * {@link #MULTI_VALUE} 只有语句级影响行数，数组内同一语句的各元素数值相同，不可求和。</p>
 *
 * @author liuxingyu01
 * @since 2026-09-01
 */
public enum BatchMode {

    /**
     * JDBC 批量（{@code batchUpdate}），每行一次 {@code addBatch}。
     * 默认值，保持与既有行为一致。
     */
    JDBC_BATCH,

    /**
     * 多值批量（单条 SQL 含多个 VALUES 元组）。每语句行数受
     * {@code TinyJdbcRuntime#getBatchInsertSize()} 限制以控制占位符 / 包大小。
     *
     * <p>因单条语句无法按行归属影响行数，{@code batchInsert} 返回的数组元素为语句级影响行数，
     * 不是逐行结果。</p>
     */
    MULTI_VALUE
}

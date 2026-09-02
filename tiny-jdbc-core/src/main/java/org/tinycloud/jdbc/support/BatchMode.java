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
     */
    MULTI_VALUE
}

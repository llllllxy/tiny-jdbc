package org.tinycloud.jdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.tinycloud.jdbc.support.BatchMode;
import org.tinycloud.jdbc.util.DbType;

/**
 * TinyJDBC 配置属性，映射 {@code tiny-jdbc.*} 配置项。
 *
 * <p>该类仅负责 Starter 模块的 Spring Boot 配置绑定，
 * 绑定后的值由自动配置转换为 core 模块的 {@code TinyJdbcRuntime}。</p>
 *
 * @author liuxingyu01
 */
@ConfigurationProperties(prefix = "tiny-jdbc")
public class TinyJdbcProperties {

    /**
     * 是否打印 Banner。
     */
    private Boolean banner = true;

    /**
     * 默认数据库类型。
     */
    private DbType dbType;

    /**
     * 是否按运行时数据源动态识别数据库类型。
     */
    private Boolean openRuntimeDbType = false;

    /**
     * 是否开启 SQL 统计。
     */
    private Boolean sqlStatEnabled = false;

    /**
     * 是否打印 SQL 执行结果。
     */
    private Boolean sqlStatResultEnabled = false;

    /**
     * 批量插入模式。
     */
    private BatchMode batchInsertMode = BatchMode.JDBC_BATCH;

    /**
     * 多值批量插入每语句最大行数。
     */
    private int batchInsertSize = 1000;

    /**
     * 获取是否打印 Banner。
     *
     * @return true 表示打印 Banner
     */
    public Boolean getBanner() {
        return this.banner;
    }

    /**
     * 设置是否打印 Banner。
     *
     * @param banner true 表示打印 Banner
     */
    public void setBanner(Boolean banner) {
        this.banner = banner;
    }

    /**
     * 获取默认数据库类型。
     *
     * @return 默认数据库类型，未配置时返回 null
     */
    public DbType getDbType() {
        return this.dbType;
    }

    /**
     * 设置默认数据库类型。
     *
     * @param dbType 默认数据库类型
     */
    public void setDbType(DbType dbType) {
        this.dbType = dbType;
    }

    /**
     * 获取是否按运行时数据源动态识别数据库类型。
     *
     * @return true 表示按运行时数据源识别
     */
    public Boolean getOpenRuntimeDbType() {
        return this.openRuntimeDbType;
    }

    /**
     * 设置是否按运行时数据源动态识别数据库类型。
     *
     * @param openRuntimeDbType true 表示按运行时数据源识别
     */
    public void setOpenRuntimeDbType(Boolean openRuntimeDbType) {
        this.openRuntimeDbType = openRuntimeDbType;
    }

    /**
     * 获取是否开启 SQL 统计。
     *
     * @return true 表示开启 SQL 统计
     */
    public Boolean getSqlStatEnabled() {
        return this.sqlStatEnabled;
    }

    /**
     * 设置是否开启 SQL 统计。
     *
     * @param sqlStatEnabled true 表示开启 SQL 统计
     */
    public void setSqlStatEnabled(Boolean sqlStatEnabled) {
        this.sqlStatEnabled = sqlStatEnabled;
    }

    /**
     * 获取是否打印 SQL 执行结果。
     *
     * @return true 表示打印 SQL 执行结果
     */
    public Boolean getSqlStatResultEnabled() {
        return this.sqlStatResultEnabled;
    }

    /**
     * 设置是否打印 SQL 执行结果。
     *
     * @param sqlStatResultEnabled true 表示打印 SQL 执行结果
     */
    public void setSqlStatResultEnabled(Boolean sqlStatResultEnabled) {
        this.sqlStatResultEnabled = sqlStatResultEnabled;
    }

    /**
     * 获取批量插入模式。
     *
     * @return 批量插入模式
     */
    public BatchMode getBatchInsertMode() {
        return this.batchInsertMode;
    }

    /**
     * 设置批量插入模式。
     *
     * @param batchInsertMode 批量插入模式
     */
    public void setBatchInsertMode(BatchMode batchInsertMode) {
        this.batchInsertMode = batchInsertMode;
    }

    /**
     * 获取多值批量插入每语句最大行数。
     *
     * @return 每语句最大行数
     */
    public int getBatchInsertSize() {
        return this.batchInsertSize;
    }

    /**
     * 设置多值批量插入每语句最大行数。
     *
     * @param batchInsertSize 每语句最大行数
     */
    public void setBatchInsertSize(int batchInsertSize) {
        this.batchInsertSize = batchInsertSize;
    }
}

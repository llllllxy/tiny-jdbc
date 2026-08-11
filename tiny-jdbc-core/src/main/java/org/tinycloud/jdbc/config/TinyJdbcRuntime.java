package org.tinycloud.jdbc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinycloud.jdbc.fill.MetaObjectHandler;
import org.tinycloud.jdbc.id.DatacenterAndWorkerProvider;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.SnowflakeConfigInterface;
import org.tinycloud.jdbc.id.SnowflakeId;
import org.tinycloud.jdbc.util.DbType;
import org.tinycloud.jdbc.util.LocalHostUtils;

import java.util.Objects;

/**
 * TinyJDBC 不可变运行时上下文。
 *
 * <p>该类由 Starter 在应用上下文启动时通过构造参数创建，替代原有静态全局配置，
 * 从而保证不同 Spring ApplicationContext 之间的配置和扩展 SPI 相互隔离。</p>
 *
 * @author liuxingyu01
 */
public final class TinyJdbcRuntime {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TinyJdbcRuntime.class);

    /**
     * 是否打印 Banner。
     */
    private final boolean banner;

    /**
     * 当前组件版本。
     */
    private final String version;

    /**
     * 默认数据库类型。
     */
    private final DbType dbType;

    /**
     * 是否按运行时数据源动态识别数据库类型。
     */
    private final boolean openRuntimeDbType;

    /**
     * 自定义 ID 生成器。
     */
    private final IdGeneratorInterface idGeneratorInterface;

    /**
     * 实体字段自动填充处理器。
     */
    private final MetaObjectHandler metaObjectHandler;

    /**
     * 当前应用上下文独占的雪花 ID 生成器。
     */
    private final SnowflakeId snowflakeId;

    /**
     * 创建 TinyJDBC 运行时上下文。
     *
     * <p>该构造器只接收已经解析的运行时值，避免 core 模块依赖 Spring Boot 的属性绑定模型。</p>
     *
     * @param banner                       是否打印 Banner
     * @param version                      当前组件版本
     * @param dbType                       默认数据库类型，可为 null
     * @param openRuntimeDbType            是否按运行时数据源动态识别数据库类型
     * @param idGeneratorInterface         自定义 ID 生成器，可为 null
     * @param snowflakeConfigInterface     雪花 ID 配置，可为 null
     * @param metaObjectHandler            自动填充处理器，可为 null
     */
    public TinyJdbcRuntime(boolean banner,
                           String version,
                           DbType dbType,
                           boolean openRuntimeDbType,
                           IdGeneratorInterface idGeneratorInterface,
                           SnowflakeConfigInterface snowflakeConfigInterface,
                           MetaObjectHandler metaObjectHandler) {
        this.banner = banner;
        this.version = version;
        this.dbType = dbType;
        this.openRuntimeDbType = openRuntimeDbType;
        this.idGeneratorInterface = idGeneratorInterface;
        this.metaObjectHandler = metaObjectHandler;
        this.snowflakeId = this.createSnowflakeId(snowflakeConfigInterface);
    }

    /**
     * 获取是否打印 Banner。
     *
     * @return true 表示打印 Banner
     */
    public boolean isBanner() {
        return this.banner;
    }

    /**
     * 获取当前组件版本。
     *
     * @return 当前组件版本
     */
    public String getVersion() {
        return this.version;
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
     * 获取是否按运行时数据源动态识别数据库类型。
     *
     * @return true 表示按运行时数据源识别
     */
    public boolean isOpenRuntimeDbType() {
        return this.openRuntimeDbType;
    }

    /**
     * 获取自定义 ID 生成器。
     *
     * @return 自定义 ID 生成器，未配置时返回 null
     */
    public IdGeneratorInterface getIdGeneratorInterface() {
        return this.idGeneratorInterface;
    }

    /**
     * 获取自动填充处理器。
     *
     * @return 自动填充处理器，未配置时返回 null
     */
    public MetaObjectHandler getMetaObjectHandler() {
        return this.metaObjectHandler;
    }

    /**
     * 获取当前应用上下文的雪花 ID 生成器。
     *
     * @return 雪花 ID 生成器
     */
    public SnowflakeId getSnowflakeId() {
        return this.snowflakeId;
    }

    /**
     * 打印组件 Banner。
     */
    public void printBanner() {
        String bannerText = "  _______ _                    _     _ _          \n" +
                " |__   __(_)                  | |   | | |         \n" +
                "    | |   _ _ __  _   _       | | __| | |__   ___ \n" +
                "    | |  | | '_ \\| | | |  _   | |/ _` | '_ \\ / __|\n" +
                "    | |  | | | | | |_| | | |__| | (_| | |_) | (__ \n" +
                "    |_|  |_|_| |_|\\__, |  \\____/ \\__,_|_.__/ \\___|\n" +
                "                   __/ |                          \n" +
                "                  |___/                           \n" + this.version;
        System.out.println(bannerText);
    }

    /**
     * 根据显式配置或本机信息创建雪花 ID 生成器。
     *
     * @param snowflakeConfigInterface 雪花 ID 配置，可为 null
     * @return 雪花 ID 生成器
     */
    private SnowflakeId createSnowflakeId(SnowflakeConfigInterface snowflakeConfigInterface) {
        if (snowflakeConfigInterface != null) {
            DatacenterAndWorkerProvider provider = snowflakeConfigInterface.getDatacenterIdAndWorkerId();
            if (provider != null && provider.getDatacenterId() != null && provider.getWorkerId() != null) {
                return new SnowflakeId(provider.getWorkerId(), provider.getDatacenterId());
            }
        }
        try {
            return new SnowflakeId(LocalHostUtils.getInetAddress());
        } catch (Exception e) {
            LOGGER.warn("Unable to obtain correct IP address information, the fixed workerId and datacenterId will be used to generate Snowflake IDs.");
            return new SnowflakeId(1L, 1L);
        }
    }
}

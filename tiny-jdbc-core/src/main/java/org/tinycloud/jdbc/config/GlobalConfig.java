package org.tinycloud.jdbc.config;

import org.tinycloud.jdbc.fill.MetaObjectHandler;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.SnowflakeConfigInterface;
import org.tinycloud.jdbc.util.DbType;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 全局配置文件
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-2024/4/15 23:33
 */
public class GlobalConfig implements Serializable {
    /**
     * 是否开启 LOGO 打印
     */
    private boolean banner = true;

    /**
     * 版本号
     */
    private String version;

    /**
     * 默认分页器适配类型
     */
    private DbType dbType;

    /**
     * 是否使用运行时数据库类型，默认值为 false，设置为 true 时，会在运行时根据多数据源自动识别对应的分页处理器
     */
    private Boolean openRuntimeDbType;

    /**
     * 默认值为 true，当使用运行时动态数据源或没有设置 dbType 属性自动获取数据库类型时，会自动获取一个数据库连接。
     * 通过该属性设置是否关闭获取的这个连接。
     */
    private Boolean closeConn;

    /**
     * 主键生成器
     */
    private IdGeneratorInterface idGeneratorInterface;

    /**
     * 雪花算法 workerId 和 datacenterId 配置
     */
    private SnowflakeConfigInterface snowflakeConfigInterface;

    /**
     * 实体字段自动填充处理器
     */
    private MetaObjectHandler metaObjectHandler;

    public boolean isBanner() {
        return banner;
    }

    public void setBanner(boolean banner) {
        this.banner = banner;
    }

    public IdGeneratorInterface getIdGeneratorInterface() {
        return idGeneratorInterface;
    }

    public void setIdGeneratorInterface(IdGeneratorInterface idGeneratorInterface) {
        this.idGeneratorInterface = idGeneratorInterface;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public DbType getDbType() {
        return dbType;
    }

    public void setDbType(DbType dbType) {
        this.dbType = dbType;
    }

    public Boolean getOpenRuntimeDbType() {
        return openRuntimeDbType;
    }

    public void setOpenRuntimeDbType(Boolean openRuntimeDbType) {
        this.openRuntimeDbType = openRuntimeDbType;
    }

    public Boolean getCloseConn() {
        return closeConn;
    }

    public void setCloseConn(Boolean closeConn) {
        this.closeConn = closeConn;
    }

    public SnowflakeConfigInterface getSnowflakeConfigInterface() {
        return snowflakeConfigInterface;
    }

    public void setSnowflakeConfigInterface(SnowflakeConfigInterface snowflakeConfigInterface) {
        this.snowflakeConfigInterface = snowflakeConfigInterface;
    }

    public MetaObjectHandler getMetaObjectHandler() {
        return metaObjectHandler;
    }

    public void setMetaObjectHandler(MetaObjectHandler metaObjectHandler) {
        this.metaObjectHandler = metaObjectHandler;
    }

    /**
     * 缓存全局配置信息
     */
    private static final Map<String, GlobalConfig> GLOBAL_CONFIG = new ConcurrentHashMap<>(1);
    private static final String GLOBAL_CONFIG_KEY = "global_config_key";

    /**
     * 配置全局设置
     *
     * @param globalConfig 全局配置
     */
    public static void setConfig(GlobalConfig globalConfig) {
        GLOBAL_CONFIG.putIfAbsent(GLOBAL_CONFIG_KEY, globalConfig);
        if (globalConfig.isBanner()) {
            printBanner();
        }
    }

    /**
     * 获取全局配置
     */
    public static GlobalConfig getConfig() {
        return GLOBAL_CONFIG.get(GLOBAL_CONFIG_KEY);
    }

    /**
     * 输出 banner
     */
    public static void printBanner() {
        String banner = "  _______ _                    _     _ _          \n" +
                " |__   __(_)                  | |   | | |         \n" +
                "    | |   _ _ __  _   _       | | __| | |__   ___ \n" +
                "    | |  | | '_ \\| | | |  _   | |/ _` | '_ \\ / __|\n" +
                "    | |  | | | | | |_| | | |__| | (_| | |_) | (__ \n" +
                "    |_|  |_|_| |_|\\__, |  \\____/ \\__,_|_.__/ \\___|\n" +
                "                   __/ |                          \n" +
                "                  |___/                           " + "\n" +
                getConfig().getVersion();
        System.out.println(banner);
    }

}

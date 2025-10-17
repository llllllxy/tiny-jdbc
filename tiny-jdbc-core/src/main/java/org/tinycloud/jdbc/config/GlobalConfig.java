package org.tinycloud.jdbc.config;

import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.SnowflakeConfigInterface;
import org.tinycloud.jdbc.util.DbType;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 *     全局配置文件
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
     * 默认分页器适配类型（用于兜底）
     */
    private DbType dbType;

    /**
     * 是否使用运行时数据库类型，默认值为 false，设置为 true 时，会在运行时根据多数据源自动识别对应的分页处理器
     */
    private Boolean runtimeDbType;

    /**
     * 默认值为 true，当使用运行时动态数据源或没有设置 helperDialect 属性自动获取数据库类型时，会自动获取一个数据库连接， 通过该属性来设置是否关闭获取的这个连接，默认true关闭，设置为 false 后，不会关闭获取的连接，这个参数的设置要根据自己选择的数据源来决定。
     */
    private Boolean closeConn;

    /**
     * 数据源类型，允许配置为 hikari,druid,tomcat-jdbc,c3p0,dbcp,beecp,default，默认为 default
     */
    private String datasourceType;

    /**
     * 主键生成器
     */
    private IdGeneratorInterface idGeneratorInterface;

    /**
     * 雪花算法 workerId 和 datacenterId 配置
     */
    private SnowflakeConfigInterface snowflakeConfigInterface;

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

    public Boolean getRuntimeDbType() {
        return runtimeDbType;
    }

    public void setRuntimeDbType(Boolean runtimeDbType) {
        this.runtimeDbType = runtimeDbType;
    }

    public Boolean getCloseConn() {
        return closeConn;
    }

    public void setCloseConn(Boolean closeConn) {
        this.closeConn = closeConn;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public SnowflakeConfigInterface getSnowflakeConfigInterface() {
        return snowflakeConfigInterface;
    }

    public void setSnowflakeConfigInterface(SnowflakeConfigInterface snowflakeConfigInterface) {
        this.snowflakeConfigInterface = snowflakeConfigInterface;
    }


    /* -----------------------静态方法和变量开始-------------------------------------- */
    /**
     * 缓存全局配置信息
     */
    private static final Map<String, GlobalConfig> GLOBAL_CONFIG = new ConcurrentHashMap<>(1);
    private static final String GLOBAL_CONFIG_KEY = "global_config_key";

    /**
     * <p>
     *  配置全局设置
     * <p/>
     *
     * @param globalConfig 全局配置
     */
    public static void setConfig(GlobalConfig globalConfig) {
        // 设置全局设置
        GLOBAL_CONFIG.putIfAbsent(GLOBAL_CONFIG_KEY, globalConfig);
        if (globalConfig.isBanner()) {
            printBanner();
        }
    }

    /**
     * 获取MybatisGlobalConfig (统一所有入口)
     */
    public static GlobalConfig getConfig() {
        return GLOBAL_CONFIG.get(GLOBAL_CONFIG_KEY);
    }

    /**
     * <p>
     *  输出banner
     * <p/>
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

package org.tinycloud.jdbc.config;

import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.SnowflakeConfigInterface;

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

    private String version;

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

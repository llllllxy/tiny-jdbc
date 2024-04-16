package org.tinycloud.jdbc.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-2024/4/15 23:39
 */
public class GlobalConfigUtils {
    /**
     * 缓存全局信息
     */
    private static final Map<String, GlobalConfig> GLOBAL_CONFIG = new ConcurrentHashMap<>(1);

    private static final String GLOBAL_CONFIG_KEY = "global_config_key";

    /**
     * <p>
     * 设置全局设置
     * <p/>
     *
     * @param globalConfig 全局配置
     */
    public static void setGlobalConfig(GlobalConfig globalConfig) {
        // 设置全局设置
        GLOBAL_CONFIG.putIfAbsent(GLOBAL_CONFIG_KEY, globalConfig);
        if (globalConfig.isBanner()) {
            printBanner();
        }
    }

    /**
     * 获取MybatisGlobalConfig (统一所有入口)
     */
    public static GlobalConfig getGlobalConfig() {
        return GLOBAL_CONFIG.get(GLOBAL_CONFIG_KEY);
    }

    /**
     * <p>
     * 输出banner
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
                getGlobalConfig().getVersion();

        System.out.println(banner);
    }
}

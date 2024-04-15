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
    }

    /**
     * 获取MybatisGlobalConfig (统一所有入口)
     */
    public static GlobalConfig getGlobalConfig() {
        return GLOBAL_CONFIG.get(GLOBAL_CONFIG_KEY);
    }
}

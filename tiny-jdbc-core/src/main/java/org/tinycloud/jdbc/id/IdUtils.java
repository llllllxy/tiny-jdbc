package org.tinycloud.jdbc.id;

import org.tinycloud.jdbc.config.GlobalConfig;
import org.tinycloud.jdbc.util.LocalHostUtils;

import java.util.UUID;

/**
 * <p>
 * 唯一ID工具-IdUtil
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-07-26 15:11:53
 */
public class IdUtils {

    /**
     * 私有构造函数，防止外部实例化
     */
    private IdUtils() {
    }

    /**
     * 静态内部类实现懒汉式单例
     * 特点：1. 懒加载（仅在首次调用时初始化） 2. 线程安全（JVM保证类加载过程线程安全）
     */
    private static class InstanceHolder {
        private static final SnowflakeId INSTANCE;

        static {
            // 根据配置初始化雪花ID生成器单例
            SnowflakeConfigInterface snowflakeConfigInterface = GlobalConfig.getConfig().getSnowflakeConfigInterface();
            if (snowflakeConfigInterface != null) {
                DatacenterAndWorkerProvider provider = snowflakeConfigInterface.getDatacenterIdAndWorkerId();
                if (provider != null && provider.getDatacenterId() != null && provider.getWorkerId() != null) {
                    INSTANCE = new SnowflakeId(provider.getWorkerId(), provider.getDatacenterId());
                } else {
                    INSTANCE = new SnowflakeId(LocalHostUtils.getInetAddress());
                }
            } else {
                INSTANCE = new SnowflakeId(LocalHostUtils.getInetAddress());
            }
        }
    }

    /**
     * 获取单例（静态内部类实现，线程安全）
     *
     * @return SnowflakeId单例对象
     */
    public static SnowflakeId getInstance() {
        return InstanceHolder.INSTANCE;
    }


    /**
     * 生成雪花id
     *
     * @return String
     */
    public static String nextId() {
        return String.valueOf(getInstance().nextId());
    }

    /**
     * 生成雪花id
     *
     * @return long
     */
    public static long nextLongId() {
        return getInstance().nextId();
    }

    /**
     * 生成的UUID是带-的字符串，类似于：a5c8a5e8-df2b-4706-bea4-08d0939410e3
     *
     * @return String
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成的是不带-的字符串，类似于：a5c8a5e8df2b4706bea408d0939410e3
     *
     * @return String
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 生成MongoDB中的ObjectId，长度24，类似于：a5c8a5edf2b4706bd09390e3
     *
     * @return String
     */
    public static String objectId() {
        return ObjectId.nextId();
    }
}
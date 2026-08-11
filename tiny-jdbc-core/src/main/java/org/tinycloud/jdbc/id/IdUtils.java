package org.tinycloud.jdbc.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(IdUtils.class);

    /**
     * 私有构造函数，防止外部实例化
     */
    private IdUtils() {
    }

    /**
     * 静态内部类实现懒汉式单例。
     * 该实例仅服务于直接调用 IdUtils 的非 Spring 场景；框架内部使用 TinyJdbcRuntime 中的应用上下文实例。
     */
    private static class InstanceHolder {
        private static final SnowflakeId INSTANCE;

        static {
            INSTANCE = createSnowflakeIdByLocalHost();
        }
    }

    /**
     * 创建雪花ID生成器实例（根据本地主机IP地址）
     *
     * @return SnowflakeId实例
     */
    private static SnowflakeId createSnowflakeIdByLocalHost() {
        try {
            return new SnowflakeId(LocalHostUtils.getInetAddress());
        } catch (Exception e) {
            logger.warn("Unable to obtain correct IP address information, the machine ID and serial number of the fixed machine will be used to generate the primary key.");
            return new SnowflakeId(1L, 1L);
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
     * 生成雪花id，长度19，类似于：1932382813993381897
     *
     * @return String
     */
    public static String nextId() {
        return String.valueOf(getInstance().nextId());
    }

    /**
     * 生成雪花id，长度19，类似于：1932382813993381897
     *
     * @return long
     */
    public static long nextLongId() {
        return getInstance().nextId();
    }

    /**
     * 生成的UUID是带-的字符串，长度36，类似于：a5c8a5e8-df2b-4706-bea4-08d0939410e3
     *
     * @return String
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成的是不带-的字符串，长度32，类似于：a5c8a5e8df2b4706bea408d0939410e3
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

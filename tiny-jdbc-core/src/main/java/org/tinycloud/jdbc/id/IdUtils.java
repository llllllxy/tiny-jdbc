package org.tinycloud.jdbc.id;

import org.tinycloud.jdbc.config.GlobalConfigUtils;
import org.tinycloud.jdbc.util.LocalHostUtils;
import org.tinycloud.jdbc.util.Pair;

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

    // 私有化示例要加上volatile，防止jvm重排序，导致空指针
    private static volatile SnowflakeId snowflakeIdObj = null;

    /**
     * 获取单例（懒汉式单例，有线程安全问题，所以加锁）
     *
     * @return Sequence单例对象
     */
    public static SnowflakeId getInstance() {
        if (snowflakeIdObj == null) {
            synchronized (IdUtils.class) {
                if (snowflakeIdObj == null) {
                    SnowflakeConfigInterface snowflakeConfigInterface = GlobalConfigUtils.getGlobalConfig().getSnowflakeConfigInterface();
                    if (snowflakeConfigInterface != null) {
                        Pair<Long, Long> datacenterIdAndWorkerId = snowflakeConfigInterface.getDatacenterIdAndWorkerId();
                        snowflakeIdObj = new SnowflakeId(datacenterIdAndWorkerId.getLeft(), datacenterIdAndWorkerId.getRight());
                    } else {
                        snowflakeIdObj = new SnowflakeId(LocalHostUtils.getInetAddress());
                    }
                }
            }
        }
        return snowflakeIdObj;
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

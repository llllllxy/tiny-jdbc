package org.tinycloud.jdbc.id;

import java.util.UUID;

/**
 * <p>
 *  唯一ID工具-IdUtil
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-07-26 15:11:53
 */
public class IdUtils {
    /**
     * 生成雪花id
     * @return String
     */
    public static String nextId() {
        return Snowflake.nextId();
    }

    /**
     * 生成雪花id
     * @return long
     */
    public static long nextLongId() {
        return Snowflake.nextLongId();
    }

    /**
     * 生成的UUID是带-的字符串，类似于：a5c8a5e8-df2b-4706-bea4-08d0939410e3
     * @return String
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成的是不带-的字符串，类似于：a5c8a5e8df2b4706bea408d0939410e3
     * @return String
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}

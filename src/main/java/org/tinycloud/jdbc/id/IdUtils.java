package org.tinycloud.jdbc.id;

import org.tinycloud.jdbc.util.LocalHostUtils;

import java.util.HashSet;
import java.util.Set;
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
    private static volatile Sequence sequenceObj = null;
    private static final Object lock = new Object();

    /**
     * 获取单例（懒汉式单例，有线程安全问题，所以加锁）
     *
     * @return Sequence单例对象
     */
    public static Sequence getInstance() {
        if (sequenceObj == null) {
            synchronized (lock) {
                if (sequenceObj == null) {
                    sequenceObj = new Sequence(LocalHostUtils.getInetAddress());
                }
            }
        }
        return sequenceObj;
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



    public static void main(String[] args) {

        // ID是否重复验证测试
        Set<String> set = new HashSet<>();
        try {
            for (int i = 0; i < 10000; i++) {
                String id = String.valueOf(nextId());
                if (set.contains(id)) {
                    throw new Exception(id + " exists");
                }
                set.add(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

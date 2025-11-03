package org.tinycloud.jdbc.util;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 用来过渡下Jdk1.8下ConcurrentHashMap的性能bug的工具类
 * <br/>
 * <a href="https://bugs.openjdk.java.net/browse/JDK-8161372">JDK-8161372</a>
 * <br/>
 * A temporary workaround for Java 8 ConcurrentHashMap#computeIfAbsent specific performance issue: JDK-8161372.
 *
 * @author liuxingyu01
 * @since 2025-11-03 10:55
 */
public class ConcurrentHashMapUtils {

    private static boolean isJdk8;

    static {
        // Java 8
        // Java 9+: 9,11,17
        try {
            isJdk8 = System.getProperty("java.version").startsWith("1.8.");
        } catch (Exception ignore) {
            isJdk8 = true;
        }
    }


    /**
     * 规避 JDK 8 中 ConcurrentHashMap.computeIfAbsent 的锁竞争问题（JDK-8161372）
     * <p>
     * 注意事项：
     * 1. 当 key 不存在时，mappingFunction 可能被多线程重复执行，需确保其为幂等操作（重复执行无副作用）
     * 2. mappingFunction 不可返回 null，否则会抛出 NullPointerException
     * 3. 仅适用于 JDK 8，JDK 9+ 会直接调用原生 computeIfAbsent 方法
     *
     * @param concurrentHashMap ConcurrentHashMap，非ConcurrentHashMap别调用
     * @param key               key
     * @param mappingFunction   function
     * @param <K>               k
     * @param <V>               v
     * @return V
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-8161372">https://bugs.openjdk.java.net/browse/JDK-8161372</a>
     */
    public static <K, V> V computeIfAbsent(Map<K, V> concurrentHashMap, K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        if (isJdk8) {
            V v = concurrentHashMap.get(key);
            if (null == v) {
                // issue#11986 lock bug
                // v = map.computeIfAbsent(key, func);

                // this bug fix methods maybe cause `func.apply` multiple calls.
                v = mappingFunction.apply(key);
                if (null == v) {
                    return null;
                }
                final V res = concurrentHashMap.putIfAbsent(key, v);
                if (null != res) {
                    // if pre value present, means other thread put value already, and putIfAbsent not effect
                    // return exist value
                    return res;
                }
                // if pre value is null, means putIfAbsent effected, return current value
            }
            return v;
        } else {
            return concurrentHashMap.computeIfAbsent(key, mappingFunction);
        }
    }

}

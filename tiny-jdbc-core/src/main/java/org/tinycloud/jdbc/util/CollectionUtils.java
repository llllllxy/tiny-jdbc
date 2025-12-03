package org.tinycloud.jdbc.util;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Collection工具类
 *
 * @author liuxingyu01
 * @since 2025-12-03 10:55
 */
public class CollectionUtils {

    /**
     * 校验集合是否为空
     *
     * @param coll 入参
     * @return boolean
     */
    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    /**
     * 校验集合是否不为空
     *
     * @param coll 入参
     * @return boolean
     */
    public static boolean isNotEmpty(Collection<?> coll) {
        return !isEmpty(coll);
    }

    /**
     * 将数组转换为List(不可变)
     *
     * @param t 入参
     * @return List
     */
    @SafeVarargs
    public static <T> List<T> toList(T... t) {
        if (t != null) {
            return Arrays.asList(t);
        }
        return Collections.emptyList();
    }

    /**
     * 切割集合为多个集合
     *
     * @param entityList 数据集合
     * @param batchSize  每批集合的大小
     * @param <T>        数据类型
     * @return 切割后的多个集合
     */
    public static <T> List<List<T>> split(Collection<T> entityList, int batchSize) {
        if (isEmpty(entityList)) {
            return Collections.emptyList();
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must not be less than one");
        }
        final Iterator<T> iterator = entityList.iterator();
        final List<List<T>> results = new ArrayList<>(entityList.size() / batchSize);
        while (iterator.hasNext()) {
            final List<T> list = IntStream.range(0, batchSize).filter(x -> iterator.hasNext())
                    .mapToObj(i -> iterator.next()).collect(Collectors.toList());
            if (!list.isEmpty()) {
                results.add(list);
            }
        }
        return results;
    }

    /**
     * 判断Map是否为空
     *
     * @param map 入参
     * @return boolean
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    /**
     * 判断Map是否不为空
     *
     * @param map 入参
     * @return boolean
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 创建默认HashMap
     *
     * @param <K> K
     * @param <V> V
     * @return HashMap
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * 创建默认ArrayList
     *
     * @param <E> 列表元素类型
     * @return ArrayList实例
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    /**
     * 从元素数组创建ArrayList
     *
     * @param <E>      列表元素类型
     * @param elements 元素数组
     * @return ArrayList实例
     */
    @SafeVarargs
    public static <E> ArrayList<E> newArrayList(E... elements) {
        if (elements == null || elements.length == 0) {
            return new ArrayList<>();
        }
        ArrayList<E> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return list;
    }

    /**
     * 快速构建 Map<String, Object>（键为String，值为任意类型）
     * 用法：CollectionUtils.asMap("id", 2, "Dave", true) → {id=2, Dave=true}
     *
     * @param keyValues 键值对数组（必须是偶数个元素，奇数个会抛异常）
     * @return 构建后的不可变Map（若需可变，可修改返回值为new HashMap<>(...)）
     * @throws IllegalArgumentException 键值对数组为null、长度为奇数、键非String类型时抛出
     */
    public static Map<String, Object> asMap(Object... keyValues) {
        // 1. 空值校验
        if (keyValues == null) {
            throw new IllegalArgumentException("键值对数组不能为null");
        }

        // 2. 校验数组长度为偶数（键值成对）
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("键值对数组长度必须为偶数（格式：key1, value1, key2, value2...），当前长度：" + keyValues.length);
        }

        // 3. 遍历数组，构建键值对（i为键索引，i+1为值索引）
        Map<String, Object> map = new HashMap<>(keyValues.length / 2); // 初始容量优化
        for (int i = 0; i < keyValues.length; i += 2) {
            Object keyObj = keyValues[i];
            Object value = keyValues[i + 1];

            // 4. 校验键必须是String类型（符合Map<String, Object>的键类型）
            if (!(keyObj instanceof String)) {
                throw new IllegalArgumentException("第" + (i / 2 + 1) + "个键必须是String类型，当前类型：" + (keyObj == null ? "null" : keyObj.getClass().getName()));
            }
            String key = (String) keyObj;

            // 5. 放入Map（允许值为null，若需禁止null值，可加 if (value == null) throw ...）
            map.put(key, value);
        }

        // 6. 返回不可变Map（避免外部修改，若需可变，直接返回map即可）
        return Collections.unmodifiableMap(map);
    }

    /**
     * 快速构建可变 Map<String, Object>（键为String，值为任意类型）
     * 用法：CollectionUtils.asMutableMap("id", 2, "Dave", true) → {id=2, Dave=true}
     *
     * @param keyValues 键值对数组（必须是偶数个元素，奇数个会抛异常）
     * @return 构建后的可变HashMap（若需不可变，可修改返回值为Collections.unmodifiableMap(map)）
     * @throws IllegalArgumentException 键值对数组为null、长度为奇数、键非String类型时抛出
     */
    public static Map<String, Object> asMutableMap(Object... keyValues) {
        Map<String, Object> map = asMap(keyValues);
        return new HashMap<>(map); // 转为可变HashMap
    }

    // 可选重载：构建指定泛型的Map（如Map<String, String>）

    /**
     * 快速构建指定泛型的Map（如Map<String, String>）
     * 用法：CollectionUtils.asTypedMap(String.class, "id", "2", "Dave", "true") → {id=2, Dave=true}
     *
     * @param valueType 期望的值类型（如String.class）
     * @param keyValues 键值对数组（必须是偶数个元素，奇数个会抛异常）
     * @return 构建后的不可变Map（若需可变，可修改返回值为new HashMap<>(...)）
     * @throws IllegalArgumentException 键值对数组为null、长度为奇数、键非String类型、值类型不匹配时抛出
     */
    public static <V> Map<String, V> asTypedMap(Class<V> valueType, Object... keyValues) {
        Map<String, Object> rawMap = asMap(keyValues);
        // 类型转换并校验值类型
        return rawMap.entrySet().stream()
                .collect(HashMap::new, (map, entry) -> {
                    Object value = entry.getValue();
                    if (value == null) {
                        map.put(entry.getKey(), null);
                    } else {
                        if (!valueType.isInstance(value)) {
                            throw new ClassCastException("键[" + entry.getKey() + "]的值类型为" + value.getClass().getName() + "，期望类型：" + valueType.getName());
                        }
                        map.put(entry.getKey(), valueType.cast(value));
                    }
                }, HashMap::putAll);
    }
}

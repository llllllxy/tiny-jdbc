package org.tinycloud.jdbc.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 数组工具类，提供数组合并、数组与List互转等常用操作
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-07-18 11:50
 */
public class ArrayUtils {

    /**
     * 合并两个数组
     *
     * @param first  第一个数组
     * @param second 第二个数组
     * @param <T>    数组元素类型
     * @return 合并后的新数组
     */
    public static <T> T[] mergeArrays(T[] first, T[] second) {
        // 校验输入数组
        if (first == null) {
            return second == null ? null : Arrays.copyOf(second, second.length);
        }
        if (second == null) {
            return Arrays.copyOf(first, first.length);
        }
        // 创建新数组并复制元素
        T[] merged = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    /**
     * 将数组转换为ArrayList
     *
     * @param array 输入数组
     * @param <T>   元素类型
     * @return 包含数组元素的ArrayList，若输入为null则返回空列表
     */
    public static <T> ArrayList<T> arrayToArrayList(T[] array) {
        if (array == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(array));
    }

    /**
     * 将ArrayList转换为数组
     *
     * @param list  输入列表
     * @param clazz 元素类型的Class对象
     * @param <T>   元素类型
     * @return 包含列表元素的数组，若列表为null则返回空数组
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayListToArray(List<T> list, Class<T> clazz) {
        if (list == null || list.isEmpty()) {
            return (T[]) java.lang.reflect.Array.newInstance(clazz, 0);
        }
        return list.toArray((T[]) java.lang.reflect.Array.newInstance(clazz, list.size()));
    }

    /**
     * 合并多个数组
     *
     * @param arrays 数组列表
     * @param <T>    元素类型
     * @return 合并后的新数组
     */
    @SafeVarargs
    public static <T> T[] mergeMultipleArrays(T[]... arrays) {
        if (arrays == null || arrays.length == 0) {
            return null;
        }

        // 计算总长度并找到第一个非null数组
        int totalLength = 0;
        T[] firstNonEmptyArray = null;

        for (T[] array : arrays) {
            if (array != null) {
                totalLength += array.length;
                if (firstNonEmptyArray == null) {
                    firstNonEmptyArray = array;
                }
            }
        }

        // 所有数组都为null或空数组的情况
        if (firstNonEmptyArray == null) {
            return null;
        }

        // 初始化结果数组
        T[] result = Arrays.copyOf(firstNonEmptyArray, totalLength);
        int currentPosition = firstNonEmptyArray.length;

        // 复制后续数组元素
        for (int i = 0; i < arrays.length; i++) {
            T[] array = arrays[i];
            // 跳过第一个非空数组（已作为初始数组）和null数组
            if (array == firstNonEmptyArray || array == null) {
                continue;
            }
            System.arraycopy(array, 0, result, currentPosition, array.length);
            currentPosition += array.length;
        }

        return result;
    }

    /**
     * 判断数组是否为空
     *
     * @param array 数组
     * @return 若数组为null或长度为0则返回true，否则返回false
     */
    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 判断数组是否不为空
     *
     * @param array 数组
     * @return 若数组不为null且长度大于0则返回true，否则返回false
     */
    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }
}

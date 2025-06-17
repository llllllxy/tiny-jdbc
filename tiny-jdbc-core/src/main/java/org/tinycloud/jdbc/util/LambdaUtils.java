package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.criteria.TypeFunction;

import java.io.Serializable;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Lambda 解析工具类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-02 10:55
 */
public class LambdaUtils {

    // 缓存 Lambda 到 字段名 的映射
    public static final Map<String, String> LAMBDA_TO_FIELD_CACHE = new ConcurrentHashMap<>();

    // 缓存 字段名 到 Lambda 的映射
    private static final Map<String, Serializable> FIELD_TO_LAMBDA_CACHE = new ConcurrentHashMap<>();


    /**
     * 方法引用获取属性名
     *
     * @param getter 函数式接口，如 UploadFile::getFileId
     * @return String 列名称
     */
    public static String getLambdaColumnName(Serializable getter) {
        SerializedLambda serializedLambda = resolve(getter);
        String implClass = serializedLambda.getImplClass();
        String methodName = serializedLambda.getImplMethodName();
        String fieldName = PropertyNamer.methodToProperty(methodName);
        // 已有缓存的话，直接返回
        String lambdaCacheKey = implClass + "." + fieldName;
        if (LAMBDA_TO_FIELD_CACHE.containsKey(lambdaCacheKey)) {
            return LAMBDA_TO_FIELD_CACHE.get(lambdaCacheKey);
        }
        try {
            // 通过字段名获取字段
            Field field = Class.forName(implClass.replace("/", ".")).getDeclaredField(fieldName);
            // 获取字段上的注解
            Column annotation = field.getAnnotation(Column.class);
            String sqlField;
            if (annotation == null || StrUtils.isEmpty(annotation.value())) {
                sqlField = StrUtils.humpToLine(fieldName);
            } else {
                sqlField = annotation.value();
            }
            LAMBDA_TO_FIELD_CACHE.put(lambdaCacheKey, sqlField);
            return sqlField;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to infer property name from method '" + methodName + "': " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Lambda implementation class not found: " + implClass.replace("/", "."), e);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found in class '" + implClass.replace("/", ".") + "'", e);
        } catch (SecurityException e) {
            throw new RuntimeException("Security manager blocked reflection access: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while getting lambda column name: " + e.getMessage(), e);
        }
    }

    /**
     * 解析方法引用，获取SerializedLambda
     *
     * @param fn 方法引用，如 UploadFile::getFileId
     * @return SerializedLambda
     */
    private static SerializedLambda resolve(Serializable fn) {
        try {
            Method method = fn.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda) method.invoke(fn);
            return serializedLambda;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("An exception occurred while obtaining SerializedLambda!", e);
        }
    }


    /**
     * 通过字段名获取对应的 Lambda Getter 方法引用
     *
     * @param clazz 类
     * @param prop  字段名
     * @param <T>   类的类型
     * @return TypeFunction 函数式接口
     */
    public static <T> TypeFunction<T, ?> getLambdaGetter(Class<T> clazz, String prop) {
        try {
            // 构建缓存键
            String cacheKey = clazz.getName() + "." + prop;
            // 从缓存中获取
            if (FIELD_TO_LAMBDA_CACHE.containsKey(cacheKey)) {
                return (TypeFunction<T, ?>) FIELD_TO_LAMBDA_CACHE.get(cacheKey);
            }
            // 反射获取 Getter 方法
            String methodName = PropertyNamer.propertyToMethod("get", prop);
            Method readMethod = clazz.getMethod(methodName);
            // 拿到方法句柄
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            final MethodHandle methodHandle = lookup.unreflect(readMethod);
            // 创建动态调用链
            CallSite callSite = LambdaMetafactory.altMetafactory(
                    lookup,
                    "apply",
                    MethodType.methodType(TypeFunction.class),
                    MethodType.methodType(Object.class, Object.class),
                    methodHandle,
                    MethodType.methodType(readMethod.getReturnType(), clazz),
                    LambdaMetafactory.FLAG_SERIALIZABLE
            );
            TypeFunction<T, ?> function = (TypeFunction<T, ?>) callSite.getTarget().invokeExact();
            // 放入缓存
            FIELD_TO_LAMBDA_CACHE.put(cacheKey, (Serializable) function);
            return function;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field '" + prop + "' does not exist in class " + clazz.getName(), e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " does not define a public getter method for field '" + prop + "'", e);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to generate lambda expression", e);
        }
    }

}

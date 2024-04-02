package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.annotation.Column;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
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

    public static final Map<String, String> LAMBDA_FIELD_CACHE = new ConcurrentHashMap<>();


    /**
     * 方法引用获取属性名
     *
     * @param getter 函数式接口，如 UploadFile::getFileId
     * @return String 列名称
     */
    public static String getLambdaColumnName(Serializable getter) {
        try {
            SerializedLambda serializedLambda = resolve(getter);
            String implClass = serializedLambda.getImplClass();
            String methodName = serializedLambda.getImplMethodName();
            String fieldName = PropertyNamer.methodToProperty(methodName);

            // 已有缓存的话，直接返回
            String lambdaCacheKey = implClass + "." + fieldName;
            if (LAMBDA_FIELD_CACHE.containsKey(lambdaCacheKey)) {
                return LAMBDA_FIELD_CACHE.get(lambdaCacheKey);
            }

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

            LAMBDA_FIELD_CACHE.put(lambdaCacheKey, sqlField);
            return sqlField;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e.getCause());
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
}

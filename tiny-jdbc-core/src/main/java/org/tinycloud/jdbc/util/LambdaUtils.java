package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.criteria.TypeFunction;

import java.io.Serializable;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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

    /**
     * 缓存实体类字段到数据库列名的映射。
     */
    private static final ClassValue<Map<String, String>> LAMBDA_TO_FIELD_CACHE = new ClassValue<Map<String, String>>() {
        /**
         * 为每个实体类创建独立的字段列名缓存。
         *
         * @param type 实体类
         * @return 当前实体类对应的字段列名缓存
         */
        @Override
        protected Map<String, String> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    /**
     * 缓存实体类字段到 Lambda Getter 的映射。
     */
    private static final ClassValue<Map<String, TypeFunction<?, ?>>> FIELD_TO_LAMBDA_CACHE = new ClassValue<Map<String, TypeFunction<?, ?>>>() {
        /**
         * 为每个实体类创建独立的字段 Lambda 缓存。
         *
         * @param type 实体类
         * @return 当前实体类对应的字段 Lambda 缓存
         */
        @Override
        protected Map<String, TypeFunction<?, ?>> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    /**
     * 方法引用获取属性名。
     *
     * @param getter 函数式接口，如 UploadFile::getFileId
     * @param <T>    实体类型
     * @return String 列名称
     */
    public static <T> String getLambdaColumnName(TypeFunction<T, ?> getter) {
        SerializedLambda serializedLambda = resolve(getter);
        final String methodName = serializedLambda.getImplMethodName();
        final String fieldName = PropertyNamer.methodToProperty(methodName);
        String instantiatedMethodType = serializedLambda.getInstantiatedMethodType();
        int start = instantiatedMethodType.indexOf('L');
        int end = instantiatedMethodType.indexOf(';', start);
        if (start < 0 || end < 0 || start >= end) {
            throw new IllegalArgumentException("Cannot resolve instantiated class from lambda method type: " + instantiatedMethodType);
        }
        final String className = instantiatedMethodType.substring(start + 1, end).replace("/", ".");
        final Class<?> entityClass = ClassUtils.getUserClass(ClassUtils.toClassConfident(className, getter.getClass().getClassLoader()));
        Map<String, String> columnNameCache = LAMBDA_TO_FIELD_CACHE.get(entityClass);
        String cachedColumnName = columnNameCache.get(fieldName);
        if (cachedColumnName != null) {
            return cachedColumnName;
        }
        return ConcurrentHashMapUtils.computeIfAbsent(columnNameCache, fieldName, key -> {
            try {
                Field field = ReflectUtils.getAccessibleField(entityClass, fieldName);
                Column annotation = field.getAnnotation(Column.class);
                if (annotation != null && !annotation.exist()) {
                    throw new IllegalArgumentException("Field '" + fieldName + "' marked with @Column(exist=false), which is not allowed to be used in a lambda expression.");
                }
                if (annotation == null || StrUtils.isEmpty(annotation.value())) {
                    return StrUtils.camelToUnderline(fieldName);
                }
                return annotation.value();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to infer property name from method '" + methodName + "': " + e.getMessage(), e);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Field '" + fieldName + "' not found in class '" + entityClass.getName() + "'", e);
            } catch (SecurityException e) {
                throw new RuntimeException("Security manager blocked reflection access: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error while getting lambda column name: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 解析方法引用，获取 SerializedLambda。
     *
     * @param fn 方法引用，如 UploadFile::getFileId
     * @return SerializedLambda
     */
    private static SerializedLambda resolve(Serializable fn) {
        try {
            Method method = fn.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            return (SerializedLambda) method.invoke(fn);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("An exception occurred while obtaining SerializedLambda!", e);
        }
    }

    /**
     * 通过字段名获取对应的 Lambda Getter 方法引用。
     *
     * @param clazz 类
     * @param prop  字段名
     * @param <T>   类的类型
     * @return TypeFunction 函数式接口
     */
    @SuppressWarnings("unchecked")
    public static <T> TypeFunction<T, ?> getLambdaGetter(Class<T> clazz, String prop) {
        final Class<T> userClass = (Class<T>) ClassUtils.getUserClass(clazz);
        Map<String, TypeFunction<?, ?>> fieldCache = FIELD_TO_LAMBDA_CACHE.get(userClass);
        TypeFunction<?, ?> cachedLambdaGetter = fieldCache.get(prop);
        if (cachedLambdaGetter != null) {
            return (TypeFunction<T, ?>) cachedLambdaGetter;
        }
        return (TypeFunction<T, ?>) ConcurrentHashMapUtils.computeIfAbsent(fieldCache, prop, key -> {
            try {
                String methodName = PropertyNamer.propertyToMethod("get", prop);
                Method readMethod;
                try {
                    readMethod = userClass.getMethod(methodName);
                } catch (NoSuchMethodException e) {
                    Field field = ReflectUtils.getAccessibleField(userClass, prop);
                    if (ClassUtils.isBoolean(field.getType())) {
                        readMethod = userClass.getMethod(PropertyNamer.propertyToMethod("is", prop));
                    } else {
                        throw e;
                    }
                }

                MethodHandles.Lookup lookup = MethodHandles.lookup();
                final MethodHandle methodHandle = lookup.unreflect(readMethod);
                CallSite callSite = LambdaMetafactory.altMetafactory(
                        lookup,
                        "apply",
                        MethodType.methodType(TypeFunction.class),
                        MethodType.methodType(Object.class, Object.class),
                        methodHandle,
                        MethodType.methodType(readMethod.getReturnType(), userClass),
                        LambdaMetafactory.FLAG_SERIALIZABLE
                );
                return (TypeFunction<T, ?>) callSite.getTarget().invokeExact();
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Class " + userClass.getName() + " does not define a public getter method for field '" + prop + "'", e);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Field '" + prop + "' does not exist in class " + userClass.getName(), e);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to generate lambda expression", e);
            }
        });
    }

}

package org.tinycloud.jdbc.util;

import org.springframework.util.ClassUtils;
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

    /**
     * 缓存实体类字段到 Lambda Getter 的映射。
     *
     * <p><b>已知隐患（内存泄漏）：</b>{@link ClassValue} 的 key（实体类 Class）虽是弱引用，
     * 但 value（{@code Map<String, TypeFunction<?, ?>>}）是强引用；value 中的 lambda 实例
     * 会通过类解析强引用实体类，形成 {@code ClassValue → Map → lambda → 实体类} 的强引用链，
     * 导致实体类及其 ClassLoader 无法被卸载。在热部署、动态 classloader 等场景下，每次类加载器
     * 重载都会泄漏一批类，最终可能导致 Metaspace OOM。</p>
     *
     * <p>当前保持此实现（单 ClassLoader 长期运行无影响）。如后续需要修复，应改为弱值方案，
     * 例如用 {@code ConcurrentReferenceHashMap<Class<?>, Map<String, TypeFunction<?, ?>>>}
     * （key、value 均使用 {@code ReferenceType.WEAK}）替换外层 {@link ClassValue}，
     * 或使用 {@code WeakReference} 包装 value。如{@code ClassValue<Map<String, WeakReference<TypeFunction<?, ?>>>>}
     * </p>
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
        final Class<?> entityClass = ClassUtils.getUserClass(ClassUtils.resolveClassName(className, getter.getClass().getClassLoader()));
        try {
            // 统一走 TableInfo：@Column.value() 优先，否则驼峰转下划线；字段存在性 / exist=false 均由其校验
            TableInfo tableInfo = TableParserUtils.getTableInfo(entityClass);
            if (tableInfo.getField(fieldName) == null) {
                throw new IllegalArgumentException("Field '" + fieldName + "' not found in class '" + entityClass.getName() + "'");
            }
            if (!tableInfo.isPersistentField(fieldName)) {
                throw new IllegalArgumentException("Field '" + fieldName + "' marked with @Column(exist=false), which is not allowed to be used in a lambda expression.");
            }
            return tableInfo.getColumn(fieldName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to infer property name from method '" + methodName + "': " + e.getMessage(), e);
        }
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
                    if (field.getType() == boolean.class || Boolean.class == field.getType()) {
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

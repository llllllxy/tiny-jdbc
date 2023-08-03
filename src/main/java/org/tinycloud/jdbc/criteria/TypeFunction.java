package org.tinycloud.jdbc.criteria;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.util.SqlUtils;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

@FunctionalInterface
public interface TypeFunction<T, R> extends Serializable, Function<T, R> {
    /**
     * 获取列名称
     *
     * @param lambda lambda表达式
     * @return String 列名称
     */
    static String getLambdaColumnName(Serializable lambda) {
        try {
            Method method = lambda.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda) method.invoke(lambda);
            String implClass = serializedLambda.getImplClass();
            String getter = serializedLambda.getImplMethodName();
            String fieldName = Introspector.decapitalize(getter.replace("get", ""));

            String cacheKey = implClass + fieldName;
            if (LambdaCriteria.cache.containsKey(cacheKey)) {
                return LambdaCriteria.cache.get(cacheKey);
            }

            // 通过字段名获取字段
            Field field = Class.forName(implClass.replace("/", ".")).getDeclaredField(fieldName);
            Column annotation = field.getAnnotation(Column.class);

            String sqlField;
            if (annotation == null || annotation.value().isEmpty()) {
                sqlField = SqlUtils.humpToLine(fieldName);
            } else {
                sqlField = annotation.value();
            }

            LambdaCriteria.cache.put(cacheKey, sqlField);
            return sqlField;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

}

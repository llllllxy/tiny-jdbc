package org.tinycloud.jdbc.criteria;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.util.StrUtils;

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
     * @param getter 函数式接口，如 UploadFile::getFileId
     * @return String 列名称
     */
    static String getLambdaColumnName(Serializable getter) {
        try {
            Method method = getter.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda) method.invoke(getter);

            String implClass = serializedLambda.getImplClass();
            String methodName = serializedLambda.getImplMethodName();
            String fieldName = Introspector.decapitalize(methodName.replaceFirst("get", ""));

            String lambdaCacheKey = implClass + fieldName;
            if (LambdaCriteria.LAMBDA_CACHE.containsKey(lambdaCacheKey)) {
                return LambdaCriteria.LAMBDA_CACHE.get(lambdaCacheKey);
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

            LambdaCriteria.LAMBDA_CACHE.put(lambdaCacheKey, sqlField);
            return sqlField;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

}

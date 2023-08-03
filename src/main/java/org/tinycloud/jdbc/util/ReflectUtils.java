package org.tinycloud.jdbc.util;

import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.JdbcException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * java反射工具类
 *
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
public class ReflectUtils {

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];
    private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

    private static final Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentReferenceHashMap<>(256);
    private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentReferenceHashMap<>(256);

    /**
     * 校验Entity对象的合法性
     *
     * @param entity 实体类对象
     * @param <T>    泛型
     */
    public static <T> void validateTargetClass(T entity) {
        if (entity == null) {
            throw new JdbcException("SqlGenerator entity cannot be null");
        }
        Class<?> clazz = entity.getClass();
        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new JdbcException("SqlGenerator " + clazz + "no @Table defined");
        }
        String table = tableAnnotation.value();
        if (StringUtils.isEmpty(table)) {
            throw new JdbcException("SqlGenerator " + clazz + "@Table value cannot be null");
        }
        Field[] fields = getFields(clazz);
        if (fields == null || fields.length == 0) {
            throw new JdbcException("SqlGenerator " + clazz + " no field defined");
        }
    }


    /**
     * 判断数据是否为null
     *
     * @param clazz      类对象
     * @param filedValue 属性值
     * @return true or false
     */
    public static boolean typeValueIsNotNull(Class<?> clazz, Object filedValue) {
        // String
        if (clazz == java.lang.String.class) {
            if (filedValue == null || filedValue == "") {
                return false;
            }
            // Number
        } else if (clazz == java.lang.Integer.class || clazz == java.lang.Double.class || clazz == java.lang.Float.class
                || clazz == java.lang.Long.class || clazz == java.lang.Short.class
                || clazz == java.math.BigDecimal.class) {
            if (null == filedValue) {
                return false;
            }
            // Date
        } else if (clazz == java.util.Date.class || clazz == java.sql.Timestamp.class || clazz == java.sql.Date.class) {
            if (null == filedValue) {
                return false;
            }
            // Boolean
        } else if (clazz == java.lang.Boolean.class) {
            if (null == filedValue) {
                return false;
            }
            // Byte
        } else if (clazz == java.lang.Byte.class) {
            if (null == filedValue) {
                return false;
            }
        }
        return true;
    }


    /**
     * 创建类实例
     *
     * @param clazz 类对象
     * @return 对象示例
     */
    public static Object createInstance(Class<?> clazz) {
        Object o;
        try {
            o = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("load class " + clazz.getCanonicalName() + " fail");
        }
        return o;
    }


    /**
     * 根据类对象获取其属性列表
     *
     * @param clazz 类对象
     * @return 字段数组
     */
    public static Field[] getFields(Class<?> clazz) {
        Field[] result = declaredFieldsCache.get(clazz);
        if (result != null) {
            return result;
        }
        List<Field> list = new ArrayList<>();
        // 遍历类及其父类的所有字段并获取属性名称
        while (clazz != null && !"java.lang.Object".equals(clazz.getName())) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                list.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        result = list.toArray(new Field[0]);
        declaredFieldsCache.put(clazz, result.length == 0 ? EMPTY_FIELD_ARRAY : result);
        return result;
    }


    /**
     * 根据类对象获取其方法列表
     *
     * @param clazz 类对象
     * @return 字段数组
     */
    public static Method[] getMethods(Class<?> clazz) {
        Method[] result = declaredMethodsCache.get(clazz);
        if (result != null) {
            return result;
        }
        result = clazz.getDeclaredMethods();
        declaredMethodsCache.put(clazz, result.length == 0 ? EMPTY_METHOD_ARRAY : result);
        return result;
    }


    /**
     * 通过set注入属性值
     *
     * @param o          对象
     * @param fieldName  对象属性名
     * @param fieldValue 对象属性值
     */
    public static void invokeSetter(Object o, String fieldName, Object fieldValue) {
        Method[] declaredMethods = getMethods(o.getClass());
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.getName().equalsIgnoreCase("set" + StringUtils.capitalize(fieldName))) {
                try {
                    declaredMethod.invoke(o, fieldValue);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    throw new RuntimeException("set field value fail : " + fieldName);
                }
            }
        }
    }


    /**
     * 通过get获取属性值
     *
     * @param o         对象
     * @param fieldName 对象属性名
     */
    public static Object invokeGetter(Object o, String fieldName) {
        Object object = o;
        Method[] declaredMethods = getMethods(o.getClass());
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.getName().equalsIgnoreCase("get" + StringUtils.capitalize(fieldName))) {
                try {
                    object = declaredMethod.invoke(o);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    throw new RuntimeException("get field value fail : " + fieldName);
                }
            }
        }
        return object;
    }


    /**
     * 根据名称和参数类型获取一个方法。如果未找到该方法，则返回null。
     *
     * @param clazz          方法所属的类
     * @param methodName     方法的名称
     * @param parameterTypes 方法接受的参数类型
     */
    protected static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            if (clazz == null) {
                return null;
            } else {
                return clazz.getMethod(methodName, parameterTypes);
            }
        } catch (SecurityException e) {
            throw new RuntimeException("Security exception looking for method " + clazz.getName() + "." + methodName + ". ");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method not found " + clazz.getName() + "." + methodName + "." + methodName + ". ");
        }
    }

    /**
     * 根据对象和属性名称获取一个属性。如果未找到，则返回null
     * 循环向上转型, 获取对象的DeclaredField, 并强制设置为可访问.
     *
     * @param obj       对象
     * @param fieldName 属性名称
     */
    public static Field getAccessibleField(final Object obj, final String fieldName) {
        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                makeAccessible(field);
                return field;
            } catch (NoSuchFieldException e) {
                // Field不在当前类定义,继续向上转型
                continue;// new add
            }
        }
        return null;
    }

    /**
     * 直接读取对象属性值, 无视private/protected修饰符, 不经过setter函数
     *
     * @param obj       对象
     * @param fieldName 对象属性名
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        Field field = getAccessibleField(obj, fieldName);
        if (field == null) {
            throw new RuntimeException("there are no field named " + fieldName + " in class " + obj.getClass().getName());
        }
        Object result = null;
        try {
            result = field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("get field value fail : " + fieldName);
        }
        return result;
    }

    /**
     * 直接设置对象属性值, 无视private/protected修饰符, 不经过setter函数
     *
     * @param obj        对象
     * @param fieldName  对象属性名
     * @param fieldValue 对象属性值
     */
    public static void setFieldValue(Object obj, String fieldName, Object fieldValue) {
        Field field = getAccessibleField(obj, fieldName);
        if (field == null) {
            throw new RuntimeException("there are no field named " + fieldName + " in class " + obj.getClass().getName());
        }
        try {
            field.set(obj, fieldValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("set field value fail : " + fieldName);
        }
    }


    /**
     * 改变private/protected的成员变量为public，尽量不调用实际改动的语句，避免JDK的SecurityManager抱怨。
     */
    public static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers()) || Modifier
                .isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }


    /**
     * 改变private/protected的方法为public，尽量不调用实际改动的语句，避免JDK的SecurityManager抱怨。
     */
    public static void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
                && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }
}

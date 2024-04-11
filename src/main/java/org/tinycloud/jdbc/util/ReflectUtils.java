package org.tinycloud.jdbc.util;

import org.springframework.util.ConcurrentReferenceHashMap;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * java反射工具类
 *
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
public class ReflectUtils {

    private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];
    private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

    /**
     * Cache for {@link Class#getDeclaredMethods()} plus equivalent default methods
     */
    private static final Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentReferenceHashMap<>(256);

    /**
     * Cache for {@link Class#getDeclaredFields()}, allowing for fast iteration.
     */
    private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentReferenceHashMap<>(256);

    /**
     * 校验Entity对象的合法性
     *
     * @param entity 实体类对象
     * @param <T>    泛型
     */
    public static <T> Triple<Class<?>, Field[], String> resolveByEntity(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("SqlGenerator entity cannot be null");
        }
        Class<?> clazz = entity.getClass();
        return resolveByClass(clazz);
    }

    /**
     * 校验clazz的合法性
     *
     * @param clazz 实体类对象类型
     */
    public static Triple<Class<?>, Field[], String> resolveByClass(Class<?> clazz) {
        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new TinyJdbcException("SqlGenerator " + clazz + "no @Table defined");
        }
        String tableName = tableAnnotation.value();
        if (StrUtils.isEmpty(tableName)) {
            throw new TinyJdbcException("SqlGenerator " + clazz + "@Table value cannot be null");
        }
        Field[] fields = getFields(clazz);
        if (fields == null || fields.length == 0) {
            throw new TinyJdbcException("SqlGenerator " + clazz + " no field defined");
        }
        return Triple.of(clazz, fields, tableName);
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
        // 先获取本类的所有字段
        Field[] fields = getDeclaredFields(clazz);

        // 再遍历其父类的所有字段
        List<Field> superFieldList = new ArrayList<>();
        for (Class<?> superClazz = clazz.getSuperclass(); superClazz != null && Object.class != superClazz; superClazz = superClazz.getSuperclass()) {
            Field[] declaredFields = getDeclaredFields(superClazz);
            Collections.addAll(superFieldList, declaredFields);
            superClazz = superClazz.getSuperclass();
        }

        /* 排除重载属性 */
        Map<String, Field> fieldMap = excludeOverrideSuperField(fields, superFieldList);

        /* 去除和父类相同名字的属性 */
        Field[] result = fieldMap.values().stream()
                /* 过滤静态属性 */
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                /* 过滤 transient关键字修饰的属性 */
                .filter(f -> !Modifier.isTransient(f.getModifiers()))
                .toArray(Field[]::new);
        return result;
    }

    /**
     * 获取本类的所有字段
     *
     * @param clazz 类对象
     * @return 类的字段数组
     */
    private static Field[] getDeclaredFields(Class<?> clazz) {
        Field[] result = declaredFieldsCache.get(clazz);
        if (result == null) {
            try {
                result = clazz.getDeclaredFields();
                declaredFieldsCache.put(clazz, result.length == 0 ? EMPTY_FIELD_ARRAY : result);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() + "] from ClassLoader [" + clazz.getClassLoader() + "]", e);
            }
        }
        return result;
    }

    /**
     * <p>
     * 排序后。重置覆盖掉父类同名属性
     * </p>
     *
     * @param fields         子类属性
     * @param superFieldList 父类属性
     * @return 重置后的所有字段组成的Map
     */
    private static Map<String, Field> excludeOverrideSuperField(Field[] fields, List<Field> superFieldList) {
        // 子类属性
        Map<String, Field> fieldMap = Stream.of(fields).collect(Collectors.toMap(Field::getName, Function.identity(),
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                },
                LinkedHashMap::new));
        superFieldList.stream().filter(field -> !fieldMap.containsKey(field.getName()))
                .forEach(f -> fieldMap.put(f.getName(), f));
        return fieldMap;
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
            if (declaredMethod.getName().equalsIgnoreCase("set" + StrUtils.capitalize(fieldName))) {
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
            if (declaredMethod.getName().equalsIgnoreCase("get" + StrUtils.capitalize(fieldName))) {
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
    public static Field getAccessibleField(Object obj, String fieldName) {
        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                makeAccessible(field);
                return field;
            } catch (NoSuchFieldException ignored) {

            }
        }
        return null;
    }

    /**
     * 根据对象和属性名称获取一个属性。如果未找到，则返回null
     * 循环向上转型, 获取对象的DeclaredField, 并强制设置为可访问.
     *
     * @param clazz     对象类型
     * @param fieldName 属性名称
     */
    public static Field getAccessibleField(Class<?> clazz, String fieldName) {
        for (Class<?> superClass = clazz; superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                makeAccessible(field);
                return field;
            } catch (NoSuchFieldException ignored) {

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

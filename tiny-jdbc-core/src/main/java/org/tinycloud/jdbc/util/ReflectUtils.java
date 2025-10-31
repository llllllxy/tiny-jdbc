package org.tinycloud.jdbc.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];
    private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

    /**
     * Cache for {@link Class#getDeclaredMethods()} plus equivalent default methods
     */
    private static final Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentHashMap<>(128);

    /**
     * Cache for {@link Class#getDeclaredFields()}, allowing for fast iteration.
     */
    private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentHashMap<>(128);

    /**
     * 缓存清除方法
     */
    public static void clearCache() {
        declaredMethodsCache.clear();
        declaredFieldsCache.clear();
    }


    /**
     * 根据类对象获取其属性列表(包括祖宗类)
     *
     * @param clazz 类对象
     * @return 字段数组
     */
    public static Field[] getFields(Class<?> clazz) {
        Field[] result = declaredFieldsCache.get(clazz);
        if (result != null) {
            return result;
        }
        /* 先获取本类的所有字段 */
        Field[] fields = clazz.getDeclaredFields();

        /*  再遍历其父类的所有字段 */
        List<Field> superFieldList = new ArrayList<>();
        Class<?> superClazz = clazz.getSuperclass();
        while (Object.class != superClazz && superClazz != null) {
            Field[] superClassFields = getDeclaredFields(superClazz);
            Collections.addAll(superFieldList, superClassFields);
            superClazz = superClazz.getSuperclass();
        }

        /* 去除和父类相同名字的属性 */
        Map<String, Field> fieldMap = excludeOverrideSuperField(fields, superFieldList);

        /* 去除静态属性和transient关键字修饰的属性 */
        result = fieldMap.values().stream()
                /* 过滤静态属性 */
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                /* 过滤 transient 关键字修饰的属性 */
                .filter(f -> !Modifier.isTransient(f.getModifiers()))
                .toArray(Field[]::new);

        declaredFieldsCache.put(clazz, result.length == 0 ? EMPTY_FIELD_ARRAY : result);
        return result;
    }

    /**
     * 获取本类的所有字段
     *
     * @param clazz 类对象
     * @return 类的字段数组
     */
    private static Field[] getDeclaredFields(Class<?> clazz) {
        try {
            return clazz.getDeclaredFields();
        } catch (Throwable ex) {
            throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() + "] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
        }
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
    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return getDeclaredMethods(clazz, true);
    }

    /**
     * 根据类对象获取其方法列表
     *
     * @param clazz 类对象
     * @return 字段数组
     */
    public static Method[] getDeclaredMethods(Class<?> clazz, boolean defensive) {
        Method[] result = declaredMethodsCache.get(clazz);
        if (result == null) {
            try {
                Method[] declaredMethods = clazz.getDeclaredMethods();
                // 获取接口类里的默认方法
                List<Method> defaultMethods = findDefaultMethodsOnInterfaces(clazz);
                if (defaultMethods != null) {
                    result = new Method[declaredMethods.length + defaultMethods.size()];
                    System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
                    int index = declaredMethods.length;
                    for (Method defaultMethod : defaultMethods) {
                        result[index] = defaultMethod;
                        index++;
                    }
                } else {
                    result = declaredMethods;
                }
                declaredMethodsCache.put(clazz, result.length == 0 ? EMPTY_METHOD_ARRAY : result);
            } catch (Throwable ex) {
                throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() + "] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
            }
        }
        return (result.length == 0 || !defensive) ? result : result.clone();
    }

    /**
     * 查找接口中的默认方法（Default Methods）， Java 8 中引入了接口的默认方法
     *
     * @param clazz 类对象
     * @return 默认方法列表
     */
    private static List<Method> findDefaultMethodsOnInterfaces(Class<?> clazz) {
        List<Method> result = null;
        for (Class<?> ifc : clazz.getInterfaces()) {
            for (Method method : ifc.getMethods()) {
                if (method.isDefault()) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    result.add(method);
                }
            }
        }
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
        Method[] declaredMethods = getDeclaredMethods(o.getClass());
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.getName().equalsIgnoreCase("set" + StrUtils.capitalize(fieldName))) {
                try {
                    declaredMethod.invoke(o, fieldValue);
                } catch (IllegalAccessException | InvocationTargetException e) {
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
        Method[] declaredMethods = getDeclaredMethods(o.getClass());
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.getName().equalsIgnoreCase("get" + StrUtils.capitalize(fieldName))) {
                try {
                    object = declaredMethod.invoke(o);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("get field value fail : " + fieldName);
                }
            }
        }
        return object;
    }


    /**
     * 根据名称获取一个方法。如果未找到该方法，则返回null。
     *
     * @param clazz      方法所属的类
     * @param methodName 方法的名称
     */
    public static Method getMethod(Class<?> clazz, String methodName) {
        return getMethod(clazz, methodName, EMPTY_CLASS_ARRAY);
    }

    /**
     * 根据名称和参数类型获取一个方法。如果未找到该方法，则返回null。
     *
     * @param clazz          方法所属的类
     * @param methodName     方法的名称
     * @param parameterTypes 方法接受的参数类型
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Class<?> searchType = clazz;
        while (searchType != null) {
            Method[] methods = (searchType.isInterface() ? searchType.getMethods() : getDeclaredMethods(searchType, false));
            for (Method method : methods) {
                if (methodName.equals(method.getName()) && (parameterTypes == null || hasSameParams(method, parameterTypes))) {
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * 判断方法的参数类型列表是否相同
     *
     * @param method     方法
     * @param paramTypes 参数类型列表
     * @return true or false
     */
    private static boolean hasSameParams(Method method, Class<?>[] paramTypes) {
        return (paramTypes.length == method.getParameterCount() &&
                Arrays.equals(paramTypes, method.getParameterTypes()));
    }

    /**
     * 根据对象和属性名称获取一个属性。如果未找到，则返回null
     * 循环向上转型, 获取对象的DeclaredField, 并强制设置为可访问.
     *
     * @param obj       对象
     * @param fieldName 属性名称
     */
    public static Field getAccessibleField(Object obj, String fieldName) throws NoSuchFieldException {
        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                makeAccessible(field);
                return field;
            } catch (NoSuchFieldException ignored) {

            }
        }
        throw new NoSuchFieldException("there are no field named " + fieldName + " in class " + obj.getClass().getName());
    }

    /**
     * 根据对象和属性名称获取一个属性。如果未找到，则返回null
     * 循环向上转型, 获取对象的DeclaredField, 并强制设置为可访问.
     *
     * @param clazz     对象类型
     * @param fieldName 属性名称
     */
    public static Field getAccessibleField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        for (Class<?> superClass = clazz; superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                makeAccessible(field);
                return field;
            } catch (NoSuchFieldException ignored) {

            }
        }
        throw new NoSuchFieldException("there are no field named " + fieldName + " in class " + clazz.getName());
    }

    /**
     * 直接读取对象属性值, 无视private/protected修饰符, 不经过setter函数
     *
     * @param obj       对象
     * @param fieldName 对象属性名
     */
    public static Object getFieldValue(Object obj, String fieldName) throws NoSuchFieldException {
        Field field = getAccessibleField(obj, fieldName);
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
    public static void setFieldValue(Object obj, String fieldName, Object fieldValue) throws NoSuchFieldException {
        Field field = getAccessibleField(obj, fieldName);
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
     * 判断是否为public static final类型
     */
    public static boolean isPublicStaticFinal(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
    }
}

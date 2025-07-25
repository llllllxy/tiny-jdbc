package org.tinycloud.jdbc.util;

import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * <p>
 * 类型转换工具类
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-20 11:30
 */
public class ConvertUtils {
    private ConvertUtils() {
    }

    /**
     * 转换对象类型
     *
     * @param value       待转换对象
     * @param targetClass 目标类型
     * @return 转换后的对象
     */
    @SuppressWarnings("rawtypes")
    public static Object convert(Object value, Class targetClass) {
        return convert(value, targetClass, false);
    }

    /**
     * 转换对象类型
     *
     * @param value              待转换对象
     * @param targetClass        目标类型
     * @param ignoreConvertError 是否忽略转换错误
     * @return 转换后的对象
     */
    @SuppressWarnings({"rawtypes"})
    public static Object convert(Object value, Class targetClass, boolean ignoreConvertError) {
        if (value == null && targetClass.isPrimitive()) {
            return getPrimitiveDefaultValue(targetClass);
        }
        if (value == null || (targetClass != String.class && value.getClass() == String.class && !StringUtils.hasText((String) value))) {
            return null;
        }
        if (value.getClass().isAssignableFrom(targetClass)) {
            return value;
        }
        if (targetClass == Serializable.class && value instanceof Serializable) {
            return value;
        }
        if (targetClass == String.class) {
            return value.toString();
        } else if (targetClass == Integer.class || targetClass == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } else if (targetClass == Long.class || targetClass == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } else if (targetClass == Double.class || targetClass == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } else if (targetClass == Float.class || targetClass == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.parseFloat(value.toString());
        } else if (targetClass == Boolean.class || targetClass == boolean.class) {
            String v = value.toString().toLowerCase();
            if ("1".equals(v) || "true".equalsIgnoreCase(v)) {
                return Boolean.TRUE;
            } else if ("0".equals(v) || "false".equalsIgnoreCase(v)) {
                return Boolean.FALSE;
            } else {
                throw new RuntimeException("Can not parse to boolean type of value: \"" + value + "\"");
            }
        } else if (targetClass == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(value.toString());
        } else if (targetClass == java.math.BigInteger.class) {
            return new java.math.BigInteger(value.toString());
        } else if (targetClass == byte[].class) {
            return value.toString().getBytes();
        } else if (targetClass == Short.class || targetClass == short.class) {
            if (value instanceof Number) {
                return ((Number) value).shortValue();
            }
            return Short.parseShort(value.toString());
        }
        if (ignoreConvertError) {
            return null;
        } else {
            throw new IllegalArgumentException("Can not convert \"" + value + "\" to type\"" + targetClass.getName() + "\".");
        }
    }

    /**
     * 获取原始类型的默认值
     * Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Void.TYPE
     *
     * @param paraClass 原始类型
     * @return 默认值
     */
    public static Object getPrimitiveDefaultValue(Class<?> paraClass) {
        if (paraClass == int.class || paraClass == long.class || paraClass == float.class || paraClass == double.class) {
            return 0;
        } else if (paraClass == boolean.class) {
            return Boolean.FALSE;
        } else if (paraClass == short.class) {
            return (short) 0;
        } else if (paraClass == byte.class) {
            return (byte) 0;
        } else if (paraClass == char.class) {
            return '\u0000';
        } else {
            throw new IllegalArgumentException("Can not get primitive default value for type: " + paraClass);
        }
    }

    /**
     * 转换原始类型为包装类型
     *
     * @param paraClass 原始类型
     * @return 包装类型
     */
    public static Class<?> primitiveToBoxed(Class<?> paraClass) {
        if (paraClass == Integer.TYPE) {
            return Integer.class;
        } else if (paraClass == Long.TYPE) {
            return Long.class;
        } else if (paraClass == Double.TYPE) {
            return Double.class;
        } else if (paraClass == Float.TYPE) {
            return Float.class;
        } else if (paraClass == Boolean.TYPE) {
            return Boolean.class;
        } else if (paraClass == Short.TYPE) {
            return Short.class;
        } else if (paraClass == Byte.TYPE) {
            return Byte.class;
        } else if (paraClass == Character.TYPE) {
            return Character.class;
        } else {
            throw new IllegalArgumentException("Can not convert primitive class for type: " + paraClass);
        }
    }

    /**
     * 转换对象为整数类型
     *
     * @param i 待转换对象
     * @return 整数类型
     */
    public static Integer toInt(Object i) {
        if (i instanceof Integer) {
            return (Integer) i;
        } else if (i instanceof Number) {
            return ((Number) i).intValue();
        }
        return i != null ? Integer.parseInt(i.toString()) : null;
    }

    /**
     * 转换对象为长整数类型
     *
     * @param l 待转换对象
     * @return 长整数类型
     */
    public static Long toLong(Object l) {
        if (l instanceof Long) {
            return (Long) l;
        } else if (l instanceof Number) {
            return ((Number) l).longValue();
        }
        return l != null ? Long.parseLong(l.toString()) : null;
    }

    /**
     * 转换对象为双精度浮点数类型
     *
     * @param d 待转换对象
     * @return 双精度浮点数类型
     */
    public static Double toDouble(Object d) {
        if (d instanceof Double) {
            return (Double) d;
        } else if (d instanceof Number) {
            return ((Number) d).doubleValue();
        }

        return d != null ? Double.parseDouble(d.toString()) : null;
    }

    /**
     * 转换对象为双精度浮点数类型
     *
     * @param b 待转换对象
     * @return 双精度浮点数类型
     */
    public static BigDecimal toBigDecimal(Object b) {
        if (b instanceof BigDecimal) {
            return (BigDecimal) b;
        } else if (b != null) {
            return new BigDecimal(b.toString());
        } else {
            return null;
        }
    }

    /**
     * 转换对象为大整数类型
     *
     * @param b 待转换对象
     * @return 大整数类型
     */
    public static BigInteger toBigInteger(Object b) {
        if (b instanceof BigInteger) {
            return (BigInteger) b;
        }
        // 数据类型 id(19 number)在 Oracle Jdbc 下对应的是 BigDecimal,
        // 但是在 MySql 下对应的是 BigInteger，这会导致在 MySql 下生成的代码无法在 Oracle 数据库中使用
        if (b instanceof BigDecimal) {
            return ((BigDecimal) b).toBigInteger();
        } else if (b instanceof Number) {
            return BigInteger.valueOf(((Number) b).longValue());
        } else if (b instanceof String) {
            return new BigInteger((String) b);
        }
        return (BigInteger) b;
    }

    /**
     * 转换对象为单精度浮点数类型
     *
     * @param f 待转换对象
     * @return 单精度浮点数类型
     */
    public static Float toFloat(Object f) {
        if (f instanceof Float) {
            return (Float) f;
        } else if (f instanceof Number) {
            return ((Number) f).floatValue();
        }
        return f != null ? Float.parseFloat(f.toString()) : null;
    }

    /**
     * 转换对象为短整数类型
     *
     * @param s 待转换对象
     * @return 短整数类型
     */
    public static Short toShort(Object s) {
        if (s instanceof Short) {
            return (Short) s;
        } else if (s instanceof Number) {
            return ((Number) s).shortValue();
        }
        return s != null ? Short.parseShort(s.toString()) : null;
    }

    /**
     * 转换对象为字节类型
     *
     * @param b 待转换对象
     * @return 字节类型
     */
    public static Byte toByte(Object b) {
        if (b instanceof Byte) {
            return (Byte) b;
        } else if (b instanceof Number) {
            return ((Number) b).byteValue();
        }
        return b != null ? Byte.parseByte(b.toString()) : null;
    }

    /**
     * 转换对象为布尔类型
     *
     * @param b 待转换对象
     * @return 布尔类型
     */
    public static Boolean toBoolean(Object b) {
        if (b instanceof Boolean) {
            return (Boolean) b;
        } else if (b == null) {
            return null;
        }

        // 支持 Number 之下的整数类型
        if (b instanceof Number) {
            int n = ((Number) b).intValue();
            if (n == 1) {
                return Boolean.TRUE;
            } else if (n == 0) {
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Can not support convert: \"" + b + "\" to boolean.");
        }

        // 支持 String
        if (b instanceof String) {
            String s = b.toString();
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                return Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                return Boolean.FALSE;
            }
        }
        return (Boolean) b;
    }
}

package org.tinycloud.jdbc.codegen.util;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 *  {@link TypeUtils} 数据库类型到 Java 类型映射的单元测试（无需连接数据库）
 * </p>
 */
public class TypeUtilsTest {

    // ===== 三元方法：日期/时间类型 =====
    @Test
    public void testDateTimeMappings() {
        assertEquals("java.time.LocalDate", TypeUtils.getJavaType(Types.DATE, 10, 0));
        assertEquals("java.time.LocalTime", TypeUtils.getJavaType(Types.TIME, 8, 0));
        assertEquals("java.time.LocalDateTime", TypeUtils.getJavaType(Types.TIMESTAMP, 19, 0));
        assertEquals("java.time.OffsetDateTime", TypeUtils.getJavaType(Types.TIMESTAMP_WITH_TIMEZONE, 29, 6));
    }

    // ===== 整数类型 =====
    @Test
    public void testIntegerMappings() {
        assertEquals("Byte", TypeUtils.getJavaType(Types.TINYINT, 3, 0));
        assertEquals("Short", TypeUtils.getJavaType(Types.SMALLINT, 5, 0));
        assertEquals("Integer", TypeUtils.getJavaType(Types.INTEGER, 10, 0));
        assertEquals("Long", TypeUtils.getJavaType(Types.BIGINT, 20, 0));
    }

    // ===== 布尔/浮点类型 =====
    @Test
    public void testBooleanAndFloatMappings() {
        assertEquals("Boolean", TypeUtils.getJavaType(Types.BOOLEAN, 1, 0));
        assertEquals("Boolean", TypeUtils.getJavaType(Types.BIT, 1, 0));
        assertEquals("Float", TypeUtils.getJavaType(Types.FLOAT, 12, 2));
        assertEquals("Float", TypeUtils.getJavaType(Types.REAL, 12, 2));
        assertEquals("Double", TypeUtils.getJavaType(Types.DOUBLE, 22, 2));
    }

    // ===== DECIMAL / NUMERIC 精度推导 =====
    @Test
    public void testDecimalMappings() {
        // decimalDigits > 0，一律 BigDecimal
        assertEquals("java.math.BigDecimal", TypeUtils.getJavaType(Types.DECIMAL, 10, 2));
        assertEquals("java.math.BigDecimal", TypeUtils.getJavaType(Types.NUMERIC, 12, 4));

        // decimalDigits == 0 且 columnSize 1..9 -> Integer
        assertEquals("Integer", TypeUtils.getJavaType(Types.DECIMAL, 5, 0));
        assertEquals("Integer", TypeUtils.getJavaType(Types.NUMERIC, 9, 0));

        // decimalDigits == 0 且 columnSize 10..18 -> Long
        assertEquals("Long", TypeUtils.getJavaType(Types.DECIMAL, 10, 0));
        assertEquals("Long", TypeUtils.getJavaType(Types.NUMERIC, 18, 0));

        // decimalDigits == 0 且 columnSize <= 0 或 > 18 -> BigDecimal
        assertEquals("java.math.BigDecimal", TypeUtils.getJavaType(Types.DECIMAL, 0, 0));
        assertEquals("java.math.BigDecimal", TypeUtils.getJavaType(Types.DECIMAL, -1, 0));
        assertEquals("java.math.BigDecimal", TypeUtils.getJavaType(Types.DECIMAL, 19, 0));
        assertEquals("java.math.BigDecimal", TypeUtils.getJavaType(Types.NUMERIC, 40, 0));
    }

    // ===== 字符串/二进制类型 =====
    @Test
    public void testStringAndBinaryMappings() {
        assertEquals("String", TypeUtils.getJavaType(Types.VARCHAR, 64, 0));
        assertEquals("String", TypeUtils.getJavaType(Types.CHAR, 1, 0));
        assertEquals("String", TypeUtils.getJavaType(Types.LONGVARCHAR, 1000, 0));
        assertEquals("String", TypeUtils.getJavaType(Types.NVARCHAR, 64, 0));
        assertEquals("String", TypeUtils.getJavaType(Types.NCHAR, 1, 0));
        assertEquals("String", TypeUtils.getJavaType(Types.CLOB, 1000, 0));
        assertEquals("String", TypeUtils.getJavaType(Types.NCLOB, 1000, 0));
        assertEquals("byte[]", TypeUtils.getJavaType(Types.BLOB, 1000, 0));
        assertEquals("byte[]", TypeUtils.getJavaType(Types.VARBINARY, 1000, 0));
        assertEquals("byte[]", TypeUtils.getJavaType(Types.LONGVARBINARY, 1000, 0));
    }

    // ===== 无法识别的类型回退 =====
    @Test
    public void testUnknownTypeFallsBackToString() {
        assertEquals("String", TypeUtils.getJavaType(Types.OTHER, 0, 0));
        assertEquals("String", TypeUtils.getJavaType(Types.NULL, 0, 0));
    }

    // ===== 两参便捷方法委托给三参方法 =====
    @Test
    public void testTwoArgDelegatesToThreeArg() {
        // 日期时间类型也遵循新映射
        assertEquals("java.time.LocalDate", TypeUtils.getJavaType(Types.DATE, 0));
        assertEquals("java.time.LocalTime", TypeUtils.getJavaType(Types.TIME, 0));
        assertEquals("java.time.LocalDateTime", TypeUtils.getJavaType(Types.TIMESTAMP, 0));
        assertEquals("java.time.OffsetDateTime", TypeUtils.getJavaType(Types.TIMESTAMP_WITH_TIMEZONE, 0));

        // 整数映射
        assertEquals("Byte", TypeUtils.getJavaType(Types.TINYINT, 0));
        assertEquals("Short", TypeUtils.getJavaType(Types.SMALLINT, 0));
        assertEquals("Integer", TypeUtils.getJavaType(Types.INTEGER, 0));
        assertEquals("Long", TypeUtils.getJavaType(Types.BIGINT, 0));

        // decimalDigits > 0 -> BigDecimal
        assertEquals("java.math.BigDecimal", TypeUtils.getJavaType(Types.DECIMAL, 2));
        // columnSize 未知（0）且 decimalDigits == 0 -> BigDecimal
        assertEquals("java.math.BigDecimal", TypeUtils.getJavaType(Types.DECIMAL, 0));
    }
}

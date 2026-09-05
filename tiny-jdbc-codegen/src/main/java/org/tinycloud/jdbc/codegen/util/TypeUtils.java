package org.tinycloud.jdbc.codegen.util;

import java.sql.Types;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * <p>
 *  类型工具类， 用于处理数据库类型和Java类型之间的转换
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-03-21 11:22
 */
public class TypeUtils {
    private static final Set<String> JAVA_KEYWORDS = new HashSet<>();

    static {
        String[] keywords = {
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
                "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
                "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "package", "private", "protected", "public",
                "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
                "throw", "throws", "transient", "try", "void", "volatile", "while"
        };
        Collections.addAll(JAVA_KEYWORDS, keywords);
    }

    public static String getCamelCase(String name) {
        String lowerCamel = getLowerCamelCase(name);
        if (lowerCamel.isEmpty()) {
            return lowerCamel;
        }
        return Character.toUpperCase(lowerCamel.charAt(0)) + lowerCamel.substring(1);
    }

    public static String getLowerCamelCase(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
        }

        String fieldName = sb.toString();
        if (JAVA_KEYWORDS.contains(fieldName)) {
            fieldName = fieldName + "Field";
        }
        return fieldName;
    }

    /**
     * 根据 JDBC 类型和精度生成 Java 类型（两参便捷方法，仅保留小数位）。
     * <p>委托给 {@link #getJavaType(int, int, int)}，此时 {@code columnSize} 视为未知（传 0）。</p>
     *
     * @param sqlType       数据库列类型（{@link java.sql.Types}）
     * @param decimalDigits 小数位数
     * @return 对应的 Java 类型（可能带包名）
     */
    public static String getJavaType(int sqlType, int decimalDigits) {
        return getJavaType(sqlType, 0, decimalDigits);
    }

    /**
     * 根据 JDBC 类型、列大小和小数位数生成 Java 类型。
     * <p>映射规则如下：</p>
     * <ul>
     *     <li>DATE → {@code java.time.LocalDate}</li>
     *     <li>TIME → {@code java.time.LocalTime}</li>
     *     <li>TIMESTAMP → {@code java.time.LocalDateTime}</li>
     *     <li>TIMESTAMP_WITH_TIMEZONE → {@code java.time.OffsetDateTime}</li>
     *     <li>TINYINT → {@code Byte}，SMALLINT → {@code Short}，INTEGER → {@code Integer}，BIGINT → {@code Long}</li>
     *     <li>DECIMAL/NUMERIC：{@code decimalDigits > 0} 为 {@code BigDecimal}；
     *         {@code decimalDigits == 0} 且列大小 1..9 为 {@code Integer}、10..18 为 {@code Long}、
     *         其余（≤0 或 >18）为 {@code BigDecimal}</li>
     * </ul>
     *
     * @param sqlType       数据库列类型（{@link java.sql.Types}）
     * @param columnSize    列大小（宽度）
     * @param decimalDigits 小数位数
     * @return 对应的 Java 类型（可能带包名）
     */
    public static String getJavaType(int sqlType, int columnSize, int decimalDigits) {
        switch (sqlType) {
            case Types.BIT:
            case Types.BOOLEAN:
                return "Boolean";

            case Types.TINYINT:
                return "Byte";

            case Types.SMALLINT:
                return "Short";

            case Types.INTEGER:
                return "Integer";

            case Types.BIGINT:
                return "Long";

            case Types.FLOAT:
            case Types.REAL:
                return "Float";

            case Types.DOUBLE:
                return "Double";

            case Types.NUMERIC:
            case Types.DECIMAL:
                if (decimalDigits > 0) {
                    return "java.math.BigDecimal";
                }
                if (decimalDigits == 0) {
                    if (columnSize >= 1 && columnSize <= 9) {
                        return "Integer";
                    }
                    if (columnSize >= 10 && columnSize <= 18) {
                        return "Long";
                    }
                }
                return "java.math.BigDecimal";

            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                return "String";

            case Types.DATE:
                return "java.time.LocalDate";

            case Types.TIME:
                return "java.time.LocalTime";

            case Types.TIMESTAMP:
                return "java.time.LocalDateTime";

            case Types.TIMESTAMP_WITH_TIMEZONE:
                return "java.time.OffsetDateTime";

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return "byte[]";

            case Types.CLOB:
            case Types.NCLOB:
                return "String";

            default:
                return "String";
        }
    }

    public static String getSimpleJavaType(String fullJavaType) {
        if (fullJavaType == null || fullJavaType.isEmpty()) {
            return fullJavaType;
        }
        int lastDot = fullJavaType.lastIndexOf('.');
        if (lastDot == -1) {
            return fullJavaType;
        }
        return fullJavaType.substring(lastDot + 1);
    }

    public static boolean needImport(String javaType) {
        if (javaType == null || javaType.isEmpty()) {
            return false;
        }
        return javaType.contains(".") && !javaType.endsWith("[]");
    }
}
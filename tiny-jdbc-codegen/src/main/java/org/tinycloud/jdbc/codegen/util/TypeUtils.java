package org.tinycloud.jdbc.codegen.util;

import java.sql.Types;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    public static String getJavaType(int sqlType, int decimalDigits) {
        switch (sqlType) {
            case Types.BIT:
            case Types.BOOLEAN:
                return "Boolean";

            case Types.TINYINT:
                return "Integer";

            case Types.SMALLINT:
                return "Integer";

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
                return decimalDigits > 0 ? "java.math.BigDecimal" : "Long";

            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                return "String";

            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return "java.util.Date";

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
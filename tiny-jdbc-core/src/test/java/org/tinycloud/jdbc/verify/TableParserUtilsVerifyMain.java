package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.TableParserUtils;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TableParserUtils.resolveColumnName 与 resolveColumnToPropertyMap 的测试。
 */
public class TableParserUtilsVerifyMain {

    public static class Demo {
        @Column(value = "CUSTOM_COL")
        private String customField;

        private String plainField;

        @Column(value = "IGNORED", exist = false)
        private String ignoredField;
    }

    // 验证：@Column.value() 优先
    @Test
    public void testResolveColumnNameFromAnnotation() {
        assertEquals("CUSTOM_COL", TableParserUtils.resolveColumnName(Demo.class, "customField"));
    }

    // 验证：无 @Column 时驼峰转下划线
    @Test
    public void testResolveColumnNameDefaultUnderline() {
        assertEquals("plain_field", TableParserUtils.resolveColumnName(Demo.class, "plainField"));
    }

    // 验证：字段不存在时报错
    @Test
    public void testResolveColumnNameMissingFieldThrows() {
        try {
            TableParserUtils.resolveColumnName(Demo.class, "notExist");
            fail("expected TinyJdbcException for missing field");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("no field named"));
        }
    }

    // 验证：列名→属性名映射（@Column 优先、默认下划线、exist=false 跳过）
    @Test
    public void testResolveColumnToPropertyMap() {
        Map<String, String> map = TableParserUtils.resolveColumnToPropertyMap(Demo.class);
        assertEquals("customField", map.get("custom_col"));
        assertEquals("plainField", map.get("plain_field"));
        assertFalse(map.containsKey("ignored"));
        assertFalse(map.containsKey("ignored_field"));
    }
}

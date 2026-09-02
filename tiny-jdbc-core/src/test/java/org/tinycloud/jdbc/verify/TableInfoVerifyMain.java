package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.TableInfo;
import org.tinycloud.jdbc.util.TableParserUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TableInfo 统一列名/映射的边界测试：表名与主键列、双向映射、exist=false 兼容、多 @Id 统一抛错、无 @Table 报错。
 */
public class TableInfoVerifyMain {

    @Table("t_demo")
    public static class Demo {
        @Id
        private Long id;

        @Column("custom_col")
        private String custom;

        private String userName;

        @Column(value = "ignored", exist = false)
        private String ignored;
    }

    public static class NoTableDemo {
        private Long id;
    }

    @Table("t_multi_id")
    public static class MultiIdDemo {
        @Id
        private Long idA;

        @Id(idType = IdType.INPUT)
        private Long idB;
    }

    // 表名 / 主键列 / 有效列列表
    @Test
    public void testTableInfoBasic() {
        TableInfo info = TableParserUtils.getTableInfo(Demo.class);
        assertEquals("t_demo", info.getTableName());
        assertEquals("id", info.getPrimaryKeyColumn());
        List<String> columns = info.getColumns();
        assertTrue(columns.contains("id"));
        assertTrue(columns.contains("custom_col"));
        assertTrue(columns.contains("user_name"));
        assertFalse(columns.contains("ignored"));
        // 不含 exist=false 的有效列数应为 3
        assertEquals(3, columns.size());
    }

    // 双向映射：字段名→列名；列名(忽略大小写)→字段
    @Test
    public void testColumnToPropertyMapAndFieldByColumn() {
        TableInfo info = TableParserUtils.getTableInfo(Demo.class);
        assertEquals("id", info.getColumn("id"));
        assertEquals("custom_col", info.getColumn("custom"));
        assertEquals("user_name", info.getColumn("userName"));

        // 列名忽略大小写 -> 字段
        Field custom = info.getFieldByColumn("CUSTOM_COL");
        assertEquals("custom", custom.getName());
        assertNull(info.getFieldByColumn("not_exists"));

        // 结果映射：列名(小写) -> 属性名，跳过 exist=false
        Map<String, String> map = TableParserUtils.resolveColumnToPropertyMap(Demo.class);
        assertEquals("custom", map.get("custom_col"));
        assertEquals("userName", map.get("user_name"));
        assertFalse(map.containsKey("ignored"));
    }

    // 按字段名的暴露：getField 返回全部字段（含 exist=false），isPersistentField 仅对有效字段为 true
    @Test
    public void testGetFieldAndIsPersistentField() {
        TableInfo info = TableParserUtils.getTableInfo(Demo.class);

        // getField 覆盖全部字段（含 exist=false）
        assertEquals("id", info.getField("id").getName());
        assertEquals("custom", info.getField("custom").getName());
        assertEquals("userName", info.getField("userName").getName());
        // exist=false 字段仍可被 getField 命中（用于 Lambda 校验）
        assertEquals("ignored", info.getField("ignored").getName());
        assertNull(info.getField("not_exists"));
        assertNull(info.getField(null));

        // isPersistentField 仅对有效字段为 true
        assertTrue(info.isPersistentField("id"));
        assertTrue(info.isPersistentField("custom"));
        assertTrue(info.isPersistentField("userName"));
        assertFalse(info.isPersistentField("ignored"));
        assertFalse(info.isPersistentField("not_exists"));
        assertFalse(info.isPersistentField(null));
    }

    // resolveColumnName 对 exist=false 字段仍返回列名（兼容原语义，不因 exist=false 抛错）
    @Test
    public void testResolveColumnNameIgnoresExist() {
        assertEquals("ignored", TableParserUtils.resolveColumnName(Demo.class, "ignored"));
        assertEquals("user_name", TableParserUtils.resolveColumnName(Demo.class, "userName"));
    }

    // 多 @Id 统一在 TableInfo 构建时抛错（不再依赖 SqlAssembler 各处校验）
    @Test
    public void testMultipleIdThrows() {
        try {
            TableParserUtils.getTableInfo(MultiIdDemo.class);
            fail("expected TinyJdbcException for multiple @Id");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("Only one @Id"));
        }
    }

    // 无 @Table：getTableName 抛错，但列名解析（不依赖 @Table）仍可用
    @Test
    public void testNoTable() {
        try {
            TableParserUtils.getTableName(NoTableDemo.class);
            fail("expected TinyJdbcException for missing @Table");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("no @Table defined"));
        }
        // 列名解析不依赖 @Table
        assertEquals("id", TableParserUtils.resolveColumnName(NoTableDemo.class, "id"));
    }
}

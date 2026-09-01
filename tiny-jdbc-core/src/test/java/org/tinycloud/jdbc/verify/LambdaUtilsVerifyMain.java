package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.util.LambdaUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * LambdaUtils.getLambdaColumnName 统一走 TableInfo 的测试：
 * 正常字段返回列名；{@code @Column(exist=false)} 字段抛错；找不到字段（getter 无对应属性）抛错。
 */
public class LambdaUtilsVerifyMain {

    @Table("t_lambda_demo")
    public static class LambdaDemo {
        @Id
        private Long id;

        @Column("custom_col")
        private String custom;

        private String userName;

        @Column(value = "ignored", exist = false)
        private String ignored;

        public Long getId() {
            return id;
        }

        public String getCustom() {
            return custom;
        }

        public String getUserName() {
            return userName;
        }

        public String getIgnored() {
            return ignored;
        }

        /**
         * 无对应字段的 getter：用于「字段不存在」的解析报错场景。
         */
        public String getComputed() {
            return "computed";
        }
    }

    // 正常字段：@Column.value() 优先，否则驼峰转下划线
    @Test
    public void testGetLambdaColumnName() {
        assertEquals("id", LambdaUtils.getLambdaColumnName(LambdaDemo::getId));
        assertEquals("custom_col", LambdaUtils.getLambdaColumnName(LambdaDemo::getCustom));
        assertEquals("user_name", LambdaUtils.getLambdaColumnName(LambdaDemo::getUserName));
    }

    // @Column(exist=false) 字段不允许用于 Lambda 表达式，抛等价描述异常
    @Test
    public void testGetLambdaColumnNameExistFalseThrows() {
        try {
            LambdaUtils.getLambdaColumnName(LambdaDemo::getIgnored);
            fail("expected IllegalArgumentException for @Column(exist=false) field");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("exist=false"));
        }
    }

    // getter 无对应字段：抛描述字段不存在的异常
    @Test
    public void testGetLambdaColumnNameMissingFieldThrows() {
        try {
            LambdaUtils.getLambdaColumnName(LambdaDemo::getComputed);
            fail("expected IllegalArgumentException for missing field");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not found in class"));
        }
    }
}

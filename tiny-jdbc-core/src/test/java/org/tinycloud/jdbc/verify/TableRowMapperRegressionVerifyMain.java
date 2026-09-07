package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.util.TableRowMapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * <p>
 * {@link TableRowMapper} 回归测试：验证 {@code mapRow} 通过 Spring {@link org.springframework.jdbc.support.JdbcUtils#getResultSetValue}
 * 按目标属性类型精确取值，能把 JDBC 原始值正确映射到对应类型字段。
 * </p>
 * <p>
 * 覆盖：{@code Timestamp → LocalDateTime}（依赖 {@code DefaultConversionService}）、
 * {@code byte[] → byte[]}（BLOB 值）、{@code String → String}。用 JDK 动态代理模拟
 * {@link ResultSet}/{@link ResultSetMetaData}，无需真实数据库。
 * </p>
 */
public class TableRowMapperRegressionVerifyMain {

    public static class Demo {
        private LocalDateTime createTime;
        private byte[] photo;
        private String name;

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public byte[] getPhoto() {
            return photo;
        }

        public void setPhoto(byte[] photo) {
            this.photo = photo;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void testMapRowByTargetType() throws Exception {
        byte[] photoBytes = new byte[]{1, 2, 3};
        ResultSet rs = mockResultSet(
                new String[]{"create_time", "photo", "name"},
                new Object[]{new Timestamp(System.currentTimeMillis()), photoBytes, "hello"});

        Demo demo = new TableRowMapper<>(Demo.class).mapRow(rs, 0);

        assertNotNull("应能把 Timestamp 转换为 LocalDateTime（依赖注入的 ConversionService）",
                demo.getCreateTime());
        assertNotNull("byte[] 字段（BLOB 列）应能映射", demo.getPhoto());
        assertEquals("byte[] 内容应完整", 3, demo.getPhoto().length);
        assertEquals("字符串字段应正常映射", "hello", demo.getName());
    }

    /** 用 JDK 动态代理构造一个只包含指定列与值的 ResultSet（覆盖 TableRowMapper 用到的 getMetaData/getObject/getBytes/getString/wasNull）。 */
    private static ResultSet mockResultSet(String[] columns, Object[] values) {
        InvocationHandler metaHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("getColumnCount".equals(name)) {
                return columns.length;
            }
            if ("getColumnLabel".equals(name) || "getColumnName".equals(name)) {
                return columns[((Integer) args[0]) - 1];
            }
            return defaultReturn(method);
        };
        ResultSetMetaData metaData = (ResultSetMetaData) Proxy.newProxyInstance(
                TableRowMapperRegressionVerifyMain.class.getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class}, metaHandler);

        InvocationHandler rsHandler = (proxy, method, args) -> {
            String name = method.getName();
            if ("getMetaData".equals(name)) {
                return metaData;
            }
            if ("wasNull".equals(name)) {
                return false;
            }
            // getObject（单参/两参）、getBytes、getString 等都带 index 参数，统一返回对应列值
            if (name.startsWith("get") && args != null && args.length > 0 && args[0] instanceof Integer) {
                int index = ((Integer) args[0]) - 1;
                return values[index];
            }
            return defaultReturn(method);
        };
        return (ResultSet) Proxy.newProxyInstance(
                TableRowMapperRegressionVerifyMain.class.getClassLoader(),
                new Class<?>[]{ResultSet.class}, rsHandler);
    }

    private static Object defaultReturn(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        }
        if (returnType == int.class || returnType == Integer.class) {
            return 0;
        }
        return null;
    }
}

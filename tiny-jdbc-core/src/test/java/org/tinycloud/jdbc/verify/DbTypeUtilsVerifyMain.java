package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.tinycloud.jdbc.util.DbType;
import org.tinycloud.jdbc.util.DbTypeUtils;

import java.sql.Connection;

import static org.junit.Assert.assertEquals;

/**
 * DbTypeUtils 的 JDBC URL 获取与类型解析回归验证。
 */
public class DbTypeUtilsVerifyMain {

    @Test
    public void testAll() {
        verifyFallsBackWhenReflectiveUrlIsNull();
        verifyParsesDbTypeFromUrl();
        System.out.println("DbTypeUtilsVerifyMain passed.");
    }

    /**
     * 反射调用 {@code getUrl()} 返回 null 时，应继续尝试后续候选方法，而不是直接返回 null。
     */
    private static void verifyFallsBackWhenReflectiveUrlIsNull() {
        NullUrlDataSource dataSource = new NullUrlDataSource();
        assertEquals("should fall back to getJdbcUrl when getUrl returns null",
                "jdbc:postgresql://127.0.0.1:5432/verify", DbTypeUtils.getJdbcUrl(dataSource));
        assertEquals("dbType should be resolved from the fallback url",
                DbType.POSTGRE_SQL, DbTypeUtils.getDbType(dataSource));
    }

    /**
     * URL 解析：主流数据库前缀应映射到对应 DbType。
     */
    private static void verifyParsesDbTypeFromUrl() {
        assertEquals(DbType.MYSQL, DbTypeUtils.parseDbType("jdbc:mysql://127.0.0.1:3306/t"));
        assertEquals(DbType.ORACLE, DbTypeUtils.parseDbType("jdbc:oracle:thin:@127.0.0.1:1521:orcl"));
        assertEquals(DbType.DB2, DbTypeUtils.parseDbType("jdbc:db2://127.0.0.1:50000/t"));
        assertEquals(DbType.POSTGRE_SQL, DbTypeUtils.parseDbType("jdbc:postgresql://127.0.0.1:5432/t"));
    }

    /**
     * {@code getUrl()} 返回 null、{@code getJdbcUrl()} 返回有效 URL 的数据源。
     * 必须为 public 静态类，否则反射调用其 public 方法会抛 IllegalAccessException。
     */
    public static class NullUrlDataSource extends AbstractDataSource {

        public String getUrl() {
            return null;
        }

        public String getJdbcUrl() {
            return "jdbc:postgresql://127.0.0.1:5432/verify";
        }

        @Override
        public Connection getConnection() {
            throw new UnsupportedOperationException("no real connection in this verification");
        }

        @Override
        public Connection getConnection(String username, String password) {
            throw new UnsupportedOperationException("no real connection in this verification");
        }
    }
}

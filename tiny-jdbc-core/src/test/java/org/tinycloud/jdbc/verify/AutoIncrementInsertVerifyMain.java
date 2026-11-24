package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.support.AbstractSqlSupport;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 验证自增主键插入：参数走 Spring 绑定（null → setNull）、生成键回写实体。
 */
public class AutoIncrementInsertVerifyMain {

    @Test
    public void testNullParameterUsesSpringBindingAndKeyIsWrittenBack() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TestSupport support = new TestSupport(jdbc);
        AutoIncEntity entity = new AutoIncEntity();
        entity.setName(null);
        entity.setAge(18);

        support.insert(entity, false);

        assertTrue(jdbc.calls.contains("setNull@1"), "null parameter should be bound via setNull");
        assertFalse(jdbc.calls.contains("setObject@1=null"), "null parameter must not use setObject");
        assertEquals(Long.valueOf(42L), entity.getId(), "generated key should be written back");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }

    /** 只记录 PreparedStatement 绑定动作、填充生成键，不真正访问数据库。 */
    private static class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<String> calls = new ArrayList<String>();
        private Map<String, Object> keyRow = Collections.<String, Object>singletonMap("id", 42L);
        private final ParameterMetaData parameterMetaData = (ParameterMetaData) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ParameterMetaData.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if ("getParameterType".equals(method.getName())) {
                            return Types.VARCHAR;
                        }
                        return null;
                    }
                });
        private final Connection connection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{Connection.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if ("prepareStatement".equals(method.getName())) {
                            return statementProxy();
                        }
                        return null;
                    }
                });

        private PreparedStatement statementProxy() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                            if ("getParameterMetaData".equals(method.getName())) {
                                return parameterMetaData;
                            }
                            if ("setNull".equals(method.getName())) {
                                calls.add("setNull@" + args[0]);
                            } else if ("setObject".equals(method.getName())) {
                                calls.add("setObject@" + args[0] + "=" + args[1]);
                            }
                            return null;
                        }
                    });
        }

        @Override
        public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) {
            try {
                psc.createPreparedStatement(this.connection);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            generatedKeyHolder.getKeyList().add(this.keyRow);
            return 1;
        }
    }

    private static class TestSupport extends AbstractSqlSupport<AutoIncEntity, Long> {
        private final JdbcTemplate jdbcTemplate;
        private final TinyJdbcRuntime runtime =
                new TinyJdbcRuntime(false, "verify", null, false, null, null, null);

        private TestSupport(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        protected JdbcTemplate getJdbcTemplate() {
            return this.jdbcTemplate;
        }

        @Override
        protected IPageHandle getPageHandle() {
            return null;
        }

        @Override
        protected TinyJdbcRuntime getTinyJdbcRuntime() {
            return this.runtime;
        }

        @Override
        protected List<SqlInterceptor> getSqlInterceptors() {
            return Collections.emptyList();
        }

        @Override
        protected NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
            return null;
        }
    }

    @Table("t_auto_inc")
    public static class AutoIncEntity implements Serializable {
        @Id(idType = IdType.AUTO_INCREMENT)
        @Column("id")
        private Long id;
        @Column("name")
        private String name;
        @Column("age")
        private Integer age;

        public Long getId() { return this.id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return this.name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return this.age; }
        public void setAge(Integer age) { this.age = age; }
    }

}

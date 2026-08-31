package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.id.IdContext;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.IdGeneratorRouter;
import org.tinycloud.jdbc.util.DbType;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 主键生成路由的测试：内置策略分发、CUSTOM 统一、SEQUENCE、AUTO_INCREMENT/INPUT 特殊分支与类型校验。
 */
public class IdGeneratorVerifyMain {

    private TinyJdbcRuntime newRuntime(IdGeneratorInterface custom) {
        return new TinyJdbcRuntime(true, "test", DbType.MYSQL, false, custom, null, null);
    }

    private IdGeneratorRouter router(IdGeneratorInterface custom) {
        return new IdGeneratorRouter(newRuntime(custom));
    }

    @Test
    public void testUuidViaRouter() {
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), String.class, IdType.UUID);
        Object id = router(null).generate(ctx);
        assertTrue(id instanceof String);
        assertEquals(32, ((String) id).length());
        assertEquals(id, demo.getId());
    }

    @Test
    public void testObjectIdViaRouter() {
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), String.class, IdType.OBJECT_ID);
        Object id = router(null).generate(ctx);
        assertTrue(id instanceof String);
        assertEquals(24, ((String) id).length());
        assertEquals(id, demo.getId());
    }

    @Test
    public void testNanoIdViaRouter() {
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), String.class, IdType.NANO_ID);
        Object id = router(null).generate(ctx);
        assertTrue(id instanceof String);
        assertEquals(21, ((String) id).length());
        assertTrue(((String) id).matches("^[0-9A-Za-z_-]{21}$"));
        assertEquals(id, demo.getId());
    }

    @Test
    public void testUlidViaRouter() {
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), String.class, IdType.ULID);
        Object id = router(null).generate(ctx);
        assertTrue(id instanceof String);
        String s = (String) id;
        assertEquals(26, s.length());
        // Crockford's Base32 大写字符集（0-9、A-H、J、K、M、N、P-T、V-Z，不含 I/L/O/U），首字符必须 0-7
        assertTrue(s.matches("^[0-7][0-9A-HJKMNP-TV-Z]{25}$"));
        assertEquals(id, demo.getId());
    }

    @Test
    public void testAssignIdLongViaRouter() {
        LongDemo demo = new LongDemo();
        IdContext ctx = ctx(demo, field(LongDemo.class, "id"), Long.class, IdType.ASSIGN_ID);
        Object id = router(null).generate(ctx);
        assertTrue(id instanceof Long);
        assertTrue(((Long) id) > 0);
        assertEquals(id, demo.getId());
    }

    @Test
    public void testAssignIdStringViaRouter() {
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), String.class, IdType.ASSIGN_ID);
        Object id = router(null).generate(ctx);
        assertTrue(id instanceof String);
        assertTrue(Long.parseLong((String) id) > 0);
        assertEquals(id, demo.getId());
    }

    @Test
    public void testSequenceViaRouter() {
        JdbcTemplate fake = new JdbcTemplate() {
            @Override
            public <T> T queryForObject(String sql, Class<T> requiredType) {
                return (T) Long.valueOf(5000L);
            }
        };
        LongDemo demo = new LongDemo();
        IdContext ctx = IdContext.builder()
                .obj(demo).field(field(LongDemo.class, "id")).fieldType(Long.class).fieldName("id")
                .idType(IdType.SEQUENCE).sequenceSql("SELECT NEXT VALUE FOR seq_demo").jdbcTemplate(fake).build();
        Object id = router(null).generate(ctx);
        assertTrue(id instanceof Long);
        assertEquals(5000L, ((Long) id).longValue());
        assertEquals(5000L, demo.getId().longValue());
    }

    @Test
    public void testCustomViaRouter() {
        // CUSTOM 使用用户注册的生成器，nextId(Object) 旧签名依然可用（default nextId(IdContext) 委托）
        IdGeneratorInterface custom = entity -> "custom-id";
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), String.class, IdType.CUSTOM);
        Object id = router(custom).generate(ctx);
        assertEquals("custom-id", id);
        assertEquals("custom-id", demo.getId());
    }

    @Test
    public void testCustomWithoutBeanThrows() {
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), String.class, IdType.CUSTOM);
        try {
            router(null).generate(ctx);
            fail("expected TinyJdbcException for CUSTOM without bean");
        } catch (TinyJdbcException e) {
            // ok
        }
    }

    @Test
    public void testCustomNullResultThrows() {
        // CUSTOM 生成器返回 null 时应报错，避免插入空主键
        IdGeneratorInterface custom = entity -> null;
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), String.class, IdType.CUSTOM);
        try {
            router(custom).generate(ctx);
            fail("expected TinyJdbcException for null custom id");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("returned null"));
        }
    }

    @Test
    public void testAutoIncrementReturnsNull() {
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), Long.class, IdType.AUTO_INCREMENT);
        assertNull(router(null).generate(ctx));
    }

    @Test
    public void testInputMustBeSetByCaller() {
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(Demo.class, "id"), String.class, IdType.INPUT);
        try {
            router(null).generate(ctx);
            fail("expected TinyJdbcException for null INPUT primary key");
        } catch (TinyJdbcException e) {
            // ok
        }
    }

    @Test
    public void testAssignIdWrongTypeThrows() {
        class IntDemo {
            private Integer id;
        }
        Demo demo = new Demo();
        IdContext ctx = ctx(demo, field(IntDemo.class, "id"), Integer.class, IdType.ASSIGN_ID);
        try {
            router(null).generate(ctx);
            fail("expected TinyJdbcException for assignId on non String/Long field");
        } catch (TinyJdbcException e) {
            // ok
        }
    }

    private IdContext ctx(Object obj, Field field, Class<?> fieldType, IdType idType) {
        return IdContext.builder()
                .obj(obj).field(field).fieldType(fieldType).fieldName(field.getName())
                .idType(idType).build();
    }

    /**
     * 反射获取字段并置为可访问，模拟框架内 resolveFields 的行为。
     */
    private Field field(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class Demo {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class LongDemo {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}

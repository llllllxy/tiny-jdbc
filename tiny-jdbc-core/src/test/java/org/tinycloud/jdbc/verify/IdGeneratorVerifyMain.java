package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.id.IdContext;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.IdGeneratorRouter;
import org.tinycloud.jdbc.id.ObjectId;
import org.tinycloud.jdbc.id.SnowflakeId;
import org.tinycloud.jdbc.id.generator.NanoIdGenerator;
import org.tinycloud.jdbc.id.generator.ObjectIdGenerator;
import org.tinycloud.jdbc.id.generator.SequenceGenerator;
import org.tinycloud.jdbc.id.generator.SnowflakeIdGenerator;
import org.tinycloud.jdbc.id.generator.UlidGenerator;
import org.tinycloud.jdbc.id.generator.UuidGenerator;
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

    @Test
    public void testDefaultNextIdToleratesNullContext() {
        // 回归：默认方法收到 null context 时应降级为空实体，而不是在 context.getObj() 处抛 NPE。
        // 该缺陷曾导致 IdUtils 的雪花 / UUID / ObjectId 方法全部失效（2.0.2 回归）。
        IdGeneratorInterface generator = entity -> "ok";
        assertEquals("ok", generator.nextId((IdContext) null));
        assertEquals("ok", generator.nextId((Object) null));
    }

    @Test
    public void testDefaultNextIdPassesEntity() {
        // 默认方法收到非空 context 时，应把实体原样透传给旧签名 nextId(Object)
        IdGeneratorInterface generator = entity -> entity;
        Object entity = new Object();
        assertTrue(generator.nextId(IdContext.builder().obj(entity).build()) == entity);
    }

    @Test
    public void testSequenceGeneratorRejectsNullContext() {
        // 回归：SequenceGenerator 覆盖了 nextId(IdContext)，不经过默认方法的兜底，
        // 必须在 context 为空时抛业务异常而不是 NPE
        try {
            new SequenceGenerator().nextId((IdContext) null);
            fail("expected TinyJdbcException for null IdContext");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("IdContext is required"));
        }
    }

    @Test
    public void testSequenceGeneratorRejectsNullJdbcTemplate() {
        IdContext ctx = IdContext.builder().sequenceSql("SELECT NEXT VALUE FOR seq_demo").build();
        try {
            new SequenceGenerator().nextId(ctx);
            fail("expected TinyJdbcException for null JdbcTemplate");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("JdbcTemplate is required"));
        }
    }

    @Test
    public void testBuiltinGeneratorsWithoutContext() {
        // 验证内置生成器均可通过旧签名 nextId(Object) 正常生成（IdUtils 移除后的推荐用法）
        assertTrue(((String) new UuidGenerator().nextId((Object) null)).matches("^[0-9a-f]{32}$"));
        assertTrue(((String) new ObjectIdGenerator().nextId((Object) null)).matches("^[0-9a-f]{24}$"));
        assertEquals(21, ((String) new NanoIdGenerator().nextId((Object) null)).length());
        assertEquals(26, ((String) new UlidGenerator().nextId((Object) null)).length());
        // 雪花生成器需要 SnowflakeId 实例，验证其忽略实体、正常产出正数
        SnowflakeId snowflakeId = new SnowflakeId(1L, 1L);
        Object id = new SnowflakeIdGenerator(snowflakeId).nextId((Object) null);
        assertTrue(((Long) id) > 0L);
    }

    @Test
    public void testObjectIdStaticEntry() {
        // IdUtils.objectId() 移除后的替代入口
        String objectId = ObjectId.nextId();
        assertEquals(24, objectId.length());
        assertTrue(objectId.matches("^[0-9a-f]{24}$"));
    }

    @Test
    public void testRuntimeSnowflakeSharedInstance() {
        // 验证 TinyJdbcRuntime 暴露的 SnowflakeId 是同一实例，是手工获取雪花 ID 的正确入口
        TinyJdbcRuntime runtime = newRuntime(null);
        assertTrue(runtime.getSnowflakeId() == runtime.getSnowflakeId());
        long first = runtime.getSnowflakeId().nextId();
        long second = runtime.getSnowflakeId().nextId();
        assertTrue("snowflake id should increase", second > first);
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

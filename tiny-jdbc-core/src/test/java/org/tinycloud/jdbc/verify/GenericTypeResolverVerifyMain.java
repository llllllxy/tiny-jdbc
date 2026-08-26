package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.support.AbstractSqlSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * 验证 {@link AbstractSqlSupport} 泛型解析在「正常继承」与「CGLIB 代理」两种场景下都能正确解析实体类型。
 *
 * <p>旧实现 {@code (ParameterizedType) getClass().getGenericSuperclass()} 在代理子类上会抛
 * ClassCastException；新实现先经 {@code ClassUtils.getUserClass} 剥离代理层，再用
 * {@code GenericTypeResolver} 解析，两种情况都应返回 {@link VerifyDemoEntity}。</p>
 */
public class GenericTypeResolverVerifyMain {

    static class DemoDao extends AbstractSqlSupport<VerifyDemoEntity, Long> {
        @Override
        protected JdbcTemplate getJdbcTemplate() { return null; }

        @Override
        protected IPageHandle getPageHandle() { return null; }

        @Override
        protected List<SqlInterceptor> getSqlInterceptors() { return Collections.emptyList(); }

        @Override
        protected TinyJdbcRuntime getTinyJdbcRuntime() { return null; }

        @Override
        protected NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() { return null; }
    }

    /** 模拟 CGLIB 代理生成的增强子类（类名特征与 Spring 一致） */
    static class DemoDao$$EnhancerBySpringCGLIB$$1 extends DemoDao {
    }

    @Test public void testAll() throws Exception {
        Field entityClassField = AbstractSqlSupport.class.getDeclaredField("entityClass");
        entityClassField.setAccessible(true);

        // 1. 正常继承场景
        DemoDao dao = new DemoDao();
        Class<?> normal = (Class<?>) entityClassField.get(dao);
        System.out.println("normal  entityClass = " + normal.getName());
        assertEquals(VerifyDemoEntity.class, normal, "normal case");

        // 2. 模拟 CGLIB 代理场景（旧实现在此处抛 ClassCastException）
        DemoDao$$EnhancerBySpringCGLIB$$1 proxy = new DemoDao$$EnhancerBySpringCGLIB$$1();
        Class<?> proxied = (Class<?>) entityClassField.get(proxy);
        System.out.println("proxy   entityClass = " + proxied.getName());
        assertEquals(VerifyDemoEntity.class, proxied, "proxy case");

        System.out.println("GenericTypeResolverVerifyMain passed.");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }
}

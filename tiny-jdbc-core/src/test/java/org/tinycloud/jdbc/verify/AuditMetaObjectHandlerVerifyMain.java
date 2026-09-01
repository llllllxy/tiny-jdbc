package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.fill.AuditMetaObjectHandler;
import org.tinycloud.jdbc.fill.FillMetaObject;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 使用 main 方法快速验证 AuditMetaObjectHandler 的自动填充行为。
 */
public class AuditMetaObjectHandlerVerifyMain {

    @Test public void testAll() {
        AuditMetaObjectHandler handler = new AuditMetaObjectHandler() {
            @Override
            protected Long currentUserId() {
                return 9527L;
            }
        };

        verifyInsertAndUpdateFill(handler);
        verifyCriteriaFill(handler);
        verifyLambdaCriteriaFill(handler);
        verifyExistFalseNotFilled(handler);

        System.out.println("AuditMetaObjectHandlerVerifyMain passed.");
    }

    private static void verifyInsertAndUpdateFill(AuditMetaObjectHandler handler) {
        VerifyDemoEntity entity = new VerifyDemoEntity();
        FillMetaObject metaObject = new FillMetaObject(entity);

        handler.insertFill(metaObject);
        assertEquals(9527L, entity.getCreateUserId(), "createUserId fill mismatch");
        assertNotNull(entity.getCreateTime(), "createTime should be filled");
        assertEquals(9527L, entity.getUpdateUserId(), "updateUserId fill mismatch");
        assertNotNull(entity.getUpdateTime(), "updateTime should be filled");

        entity.setUpdateUserId(1L);
        LocalDateTime oldUpdateTime = entity.getUpdateTime();
        handler.updateFill(metaObject);
        assertEquals(9527L, entity.getUpdateUserId(), "updateFill should override updateUserId");
        assertTrue(entity.getUpdateTime().isAfter(oldUpdateTime) || entity.getUpdateTime().isEqual(oldUpdateTime), "updateFill should refresh updateTime");
    }

    private static void verifyCriteriaFill(AuditMetaObjectHandler handler) {
        UpdateCriteria<VerifyDemoEntity> criteria = new UpdateCriteria<>();
        criteria.set("update_user_id", 111L);
        criteria.eq("id", 100L);

        handler.updateCriteriaFill(criteria, VerifyDemoEntity.class);

        List<Object> params = criteria.getParameters();

        System.out.println("verifyCriteriaFill updateSql =  " + criteria.updateSql());
        System.out.println("verifyCriteriaFill whereSql =  " + criteria.whereSql());
        System.out.println("verifyCriteriaFill params =  " + params);
    }

    private static void verifyLambdaCriteriaFill(AuditMetaObjectHandler handler) {
        LambdaUpdateCriteria<VerifyDemoEntity> criteria = new LambdaUpdateCriteria<>();
        criteria.set(VerifyDemoEntity::getUpdateUserId, 111L);
        criteria.eq(VerifyDemoEntity::getId, 200L);

        handler.updateLambdaCriteriaFill(criteria, VerifyDemoEntity.class);

        List<Object> params = criteria.getParameters();
        System.out.println("verifyLambdaCriteriaFill updateSql =  " + criteria.updateSql());
        System.out.println("verifyLambdaCriteriaFill whereSql =  " + criteria.whereSql());
        System.out.println("verifyLambdaCriteriaFill params =  " + params);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }

    /**
     * 验证 {@code @Column(exist=false)} 的“审计字段”不会被自动填充写入更新列，
     * 避免向非持久化字段生成无效的 UPDATE 列。
     */
    private static void verifyExistFalseNotFilled(AuditMetaObjectHandler handler) {
        UpdateCriteria<IgnoreAuditEntity> criteria = new UpdateCriteria<>();
        criteria.eq("id", 1L);
        handler.updateCriteriaFill(criteria, IgnoreAuditEntity.class);
        String updateSql = criteria.updateSql();
        // 该实体 updateUserId 标记为 exist=false，不应生成 update_user_id 更新列
        assertTrue(!updateSql.contains("update_user_id"),
                "exist=false field should not be filled into update columns, but got: " + updateSql);

        LambdaUpdateCriteria<IgnoreAuditEntity> lambdaCriteria = new LambdaUpdateCriteria<>();
        lambdaCriteria.eq(IgnoreAuditEntity::getId, 1L);
        handler.updateLambdaCriteriaFill(lambdaCriteria, IgnoreAuditEntity.class);
        String lambdaUpdateSql = lambdaCriteria.updateSql();
        assertTrue(!lambdaUpdateSql.contains("update_user_id"),
                "exist=false field should not be filled into lambda update columns, but got: " + lambdaUpdateSql);

        // 实体级填充同理：exist=false 字段不应被写入
        IgnoreAuditEntity entity = new IgnoreAuditEntity();
        FillMetaObject metaObject = new FillMetaObject(entity);
        handler.insertFill(metaObject);
        assertNull(entity.getUpdateUserId(), "insertFill should not fill exist=false field");
    }

    private static void assertNull(Object value, String message) {
        if (value != null) {
            throw new IllegalStateException(message + " expected=null, actual=" + value);
        }
    }

    /**
     * 包含 {@code @Column(exist=false)} 审计字段的测试实体：updateUserId 不映射到表列，
     * 用于验证自动填充会跳过非持久化字段。
     */
    public static class IgnoreAuditEntity {
        @Column("id")
        private Long id;

        @Column(value = "update_user_id", exist = false)
        private Long updateUserId;

        @Column("update_time")
        private LocalDateTime updateTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUpdateUserId() {
            return updateUserId;
        }

        public void setUpdateUserId(Long updateUserId) {
            this.updateUserId = updateUserId;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }
    }
}

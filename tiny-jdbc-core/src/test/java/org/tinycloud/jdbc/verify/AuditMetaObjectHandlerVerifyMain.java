package org.tinycloud.jdbc.verify;
import org.junit.Test;

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
}

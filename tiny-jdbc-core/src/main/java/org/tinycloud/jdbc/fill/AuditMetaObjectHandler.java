package org.tinycloud.jdbc.fill;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.util.LambdaUtils;
import org.tinycloud.jdbc.util.ReflectUtils;

import java.time.LocalDateTime;

/**
 * <p>
 * 审计字段自动填充示例
 * </p>
 * <p>
 * 使用说明：
 * 1. 将本类复制到你的业务项目中（可保留类名，也可自定义）。
 * 2. 注册为 Spring Bean（例如加上 @Component 或在 @Configuration 中通过 @Bean 返回）。
 * 3. 按你的登录上下文实现 {@link #currentUserId()} 方法。
 * </p>
 * <p>
 * 说明：
 * - 本类演示了实体新增/更新、criteria 更新、lambdaCriteria 更新四种场景。
 * - 为保证通用性，示例中先判断字段是否存在，再执行填充，避免无审计字段实体报错。
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-04-18
 */
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_USER_ID_FIELD = "createUserId";
    private static final String CREATE_TIME_FIELD = "createTime";
    private static final String UPDATE_USER_ID_FIELD = "updateUserId";
    private static final String UPDATE_TIME_FIELD = "updateTime";

    @Override
    public void insertFill(FillMetaObject metaObject) {
        Object entity = metaObject.getEntity();
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        if (ReflectUtils.hasField(entity.getClass(), CREATE_USER_ID_FIELD)) {
            metaObject.strictInsertFill(CREATE_USER_ID_FIELD, userId);
        }
        if (ReflectUtils.hasField(entity.getClass(), CREATE_TIME_FIELD)) {
            metaObject.strictInsertFill(CREATE_TIME_FIELD, now);
        }
        if (ReflectUtils.hasField(entity.getClass(), UPDATE_USER_ID_FIELD)) {
            metaObject.strictInsertFill(UPDATE_USER_ID_FIELD, userId);
        }
        if (ReflectUtils.hasField(entity.getClass(), UPDATE_TIME_FIELD)) {
            metaObject.strictInsertFill(UPDATE_TIME_FIELD, now);
        }
    }

    @Override
    public void updateFill(FillMetaObject metaObject) {
        Object entity = metaObject.getEntity();
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        if (ReflectUtils.hasField(entity.getClass(), UPDATE_USER_ID_FIELD)) {
            metaObject.strictUpdateFill(UPDATE_USER_ID_FIELD, userId);
        }
        if (ReflectUtils.hasField(entity.getClass(), UPDATE_TIME_FIELD)) {
            metaObject.strictUpdateFill(UPDATE_TIME_FIELD, now);
        }
    }

    @Override
    public <T> void updateCriteriaFill(UpdateCriteria<T> criteria, Class<T> entityClass) {
        if (ReflectUtils.hasField(entityClass, UPDATE_USER_ID_FIELD)) {
            if (!criteria.hasUpdateColumn("update_user_id")) {
                criteria.set("update_user_id", currentUserId());
            }
        }
        if (ReflectUtils.hasField(entityClass, UPDATE_TIME_FIELD)) {
            if (!criteria.hasUpdateColumn("update_time")) {
                criteria.set("update_time", LocalDateTime.now());
            }
        }
    }

    @Override
    public <T> void updateLambdaCriteriaFill(LambdaUpdateCriteria<T> criteria, Class<T> entityClass) {
        if (ReflectUtils.hasField(entityClass, UPDATE_USER_ID_FIELD)) {
            TypeFunction<T, ?> field = LambdaUtils.getLambdaGetter(entityClass, UPDATE_USER_ID_FIELD);
            if (!criteria.hasUpdateColumn(field)) {
                criteria.set(field, currentUserId());
            }
        }
        if (ReflectUtils.hasField(entityClass, UPDATE_TIME_FIELD)) {
            TypeFunction<T, ?> field = LambdaUtils.getLambdaGetter(entityClass, UPDATE_TIME_FIELD);
            if (!criteria.hasUpdateColumn(field)) {
                criteria.set(field, LocalDateTime.now());
            }
        }
    }

    /**
     * 获取当前登录用户ID（示例方法）
     * <p>
     * 请在业务项目中替换为你自己的上下文取值逻辑。
     * </p>
     */
    protected Long currentUserId() {
        return 0L;
    }

}

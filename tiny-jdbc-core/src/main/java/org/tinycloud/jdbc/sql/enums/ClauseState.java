package org.tinycloud.jdbc.sql.enums;

/**
 * <p>
 *      统一状态管理
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-27 16:19
 */
public enum ClauseState {
    NOT_CALLED,      // 未调用
    CALLED,         // 已调用
    LOCKED          // 已锁定（禁止重复调用）
}

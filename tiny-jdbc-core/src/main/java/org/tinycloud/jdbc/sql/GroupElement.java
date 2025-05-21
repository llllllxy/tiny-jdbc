package org.tinycloud.jdbc.sql;

import java.util.List;

/**
 * <p>
 *     内部类：条件组元素
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:04
 */
public class GroupElement implements ConditionElement {
    private final JoinType joinType;
    private final ConditionGroup group;

    public GroupElement(JoinType joinType, ConditionGroup group) {
        this.joinType = joinType;
        this.group = group;
    }

    @Override
    public String toSql() {
        return "(" + group.toSql() + ")";
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public List<Object> getParameters() {
        return group.getParameters();
    }
}

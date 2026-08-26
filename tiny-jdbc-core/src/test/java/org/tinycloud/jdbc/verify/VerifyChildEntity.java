package org.tinycloud.jdbc.verify;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Table;

/**
 * 用于验证子类字段、父类字段和 boolean getter。
 */
@Table("t_verify_child")
public class VerifyChildEntity extends VerifyParentEntity {
    @Column("child_name_col")
    private String childName;

    @Column("active_flag")
    private boolean active;

    /**
     * 获取子类名称。
     *
     * @return 子类名称
     */
    public String getChildName() {
        return childName;
    }

    /**
     * 设置子类名称。
     *
     * @param childName 子类名称
     */
    public void setChildName(String childName) {
        this.childName = childName;
    }

    /**
     * 判断是否启用。
     *
     * @return true 表示启用
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 设置启用状态。
     *
     * @param active 启用状态
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}

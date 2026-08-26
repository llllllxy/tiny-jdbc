package org.tinycloud.jdbc.verify;

import org.tinycloud.jdbc.annotation.Column;

/**
 * 用于验证父类字段也可以通过子类 Lambda 解析。
 */
public class VerifyParentEntity {
    @Column("parent_code_col")
    private String parentCode;

    /**
     * 获取父类编码。
     *
     * @return 父类编码
     */
    public String getParentCode() {
        return parentCode;
    }

    /**
     * 设置父类编码。
     *
     * @param parentCode 父类编码
     */
    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }
}

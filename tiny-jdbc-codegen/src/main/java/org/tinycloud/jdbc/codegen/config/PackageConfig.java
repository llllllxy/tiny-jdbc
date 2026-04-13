package org.tinycloud.jdbc.codegen.config;


/**
 * <p>
 *  包结构配置信息类，封装代码生成器的包结构信息
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-03-21 11:22
 */
public class PackageConfig {
    /**父包名*/
    private final String parent;
    /**实体类包名*/
    private final String entity;
    /**数据访问层包名*/
    private final String dao;

    private PackageConfig(Builder builder) {
        this.parent = builder.parent;
        this.entity = builder.entity;
        this.dao = builder.dao;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ===== Getter =====
    public String getParent() {
        return parent;
    }

    public String getEntity() {
        return entity;
    }

    public String getDao() {
        return dao;
    }

    public String getEntityPackage() {
        return parent + "." + entity;
    }

    public String getDaoPackage() {
        return parent + "." + dao;
    }

    // ===== Builder =====
    public static class Builder {
        private String parent;
        private String entity = "entity"; // 默认值
        private String dao = "dao";       // 默认值

        public Builder parent(String parent) {
            this.parent = parent;
            return this;
        }

        public Builder entity(String entity) {
            this.entity = entity;
            return this;
        }

        public Builder dao(String dao) {
            this.dao = dao;
            return this;
        }

        public PackageConfig build() {
            // 核心校验
            if (parent == null || parent.isEmpty()) {
                throw new IllegalArgumentException("parent 不能为空");
            }

            // 防御性处理（可选，但推荐）
            if (entity == null || entity.isEmpty()) {
                entity = "entity";
            }
            if (dao == null || dao.isEmpty()) {
                dao = "dao";
            }

            return new PackageConfig(this);
        }
    }
}
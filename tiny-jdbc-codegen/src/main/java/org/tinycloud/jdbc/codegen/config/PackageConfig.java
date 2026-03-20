package org.tinycloud.jdbc.codegen.config;

public class PackageConfig {

    private final String parent;
    private final String entity;
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
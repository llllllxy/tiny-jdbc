package org.tinycloud.jdbc.codegen.config;

import org.tinycloud.jdbc.annotation.IdType;

import java.util.Arrays;


/**
 * <p>
 *  生成策略配置信息类，封装代码生成器的生成策略信息
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-03-21 11:22
 */
public class StrategyConfig {

    /**包含的数据库表名数组*/
    private final String[] includeTables;
    /**是否使用实际列名*/
    private final boolean useActualColumnNames;
    /**是否启用Lombok注解*/
    private final boolean enableLombok;

    /**
     * 主键策略（可选）
     */
    private final IdType idType;

    private StrategyConfig(Builder builder) {
        this.includeTables = builder.includeTables;
        this.useActualColumnNames = builder.useActualColumnNames;
        this.enableLombok = builder.enableLombok;
        this.idType = builder.idType;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ===== Getter =====
    public String[] getIncludeTables() {
        return includeTables;
    }

    public boolean isUseActualColumnNames() {
        return useActualColumnNames;
    }

    public boolean isEnableLombok() {
        return enableLombok;
    }

    public IdType getIdType() {
        return idType;
    }

    // ===== Builder =====
    public static class Builder {
        private String[] includeTables;
        private boolean useActualColumnNames = false;
        private boolean enableLombok = true;
        private IdType idType = IdType.INPUT;

        // 支持可变参数（更优雅）
        public Builder includeTables(String... tables) {
            this.includeTables = tables;
            return this;
        }

        public Builder useActualColumnNames(boolean useActualColumnNames) {
            this.useActualColumnNames = useActualColumnNames;
            return this;
        }

        public Builder enableLombok(boolean enableLombok) {
            this.enableLombok = enableLombok;
            return this;
        }

        public Builder idType(IdType idType) {
            this.idType = idType;
            return this;
        }

        public StrategyConfig build() {
            // 校验
            if (includeTables == null || includeTables.length == 0) {
                throw new IllegalArgumentException("includeTables 不能为空，至少指定一张表");
            }
            // 防御性拷贝（避免外部修改数组）
            this.includeTables = Arrays.copyOf(includeTables, includeTables.length);

            return new StrategyConfig(this);
        }
    }
}
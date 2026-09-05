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
     * 主键策略（可选）。
     * <p>当该值为 {@code null} 时，代码生成器会根据主键列是否自增自动推断：
     * 自增主键生成 {@code AUTO_INCREMENT}，否则生成 {@code INPUT}。</p>
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
        /**
         * 主键策略，默认 {@code null}。
         * <p>当为 {@code null} 时，代码生成器根据主键列是否自增自动推断
         * （自增 → AUTO_INCREMENT，非自增 → INPUT）。显式设置则固定使用该策略生成。</p>
         */
        private IdType idType = null;

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

        /**
         * 设置主键策略。不设置（保持 {@code null}）时由生成器根据主键列是否自增自动推断。
         *
         * @param idType 主键策略
         * @return 当前 Builder 实例
         */
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
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
    /**排除的数据库表名数组（优先级高于 includeTables）*/
    private final String[] excludeTables;
    /**生成类名时从表名剥离的前缀*/
    private final String[] tablePrefixes;
    /**生成类名时从表名剥离的后缀*/
    private final String[] tableSuffixes;
    /**生成实体时跳过的列名（大小写不敏感）*/
    private final String[] ignoreColumns;
    /**生成实体时跳过的列前缀*/
    private final String[] ignoreColumnPrefixes;
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
        this.excludeTables = builder.excludeTables;
        this.tablePrefixes = builder.tablePrefixes;
        this.tableSuffixes = builder.tableSuffixes;
        this.ignoreColumns = builder.ignoreColumns;
        this.ignoreColumnPrefixes = builder.ignoreColumnPrefixes;
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

    public String[] getExcludeTables() {
        return excludeTables;
    }

    public String[] getTablePrefixes() {
        return tablePrefixes;
    }

    public String[] getTableSuffixes() {
        return tableSuffixes;
    }

    public String[] getIgnoreColumns() {
        return ignoreColumns;
    }

    public String[] getIgnoreColumnPrefixes() {
        return ignoreColumnPrefixes;
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
        private String[] excludeTables;
        private String[] tablePrefixes;
        private String[] tableSuffixes;
        private String[] ignoreColumns;
        private String[] ignoreColumnPrefixes;
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

        public Builder excludeTables(String... tables) {
            this.excludeTables = tables;
            return this;
        }

        /** 生成类名时剥离的表前缀，可配多个（按配置顺序匹配第一个）。 */
        public Builder tablePrefix(String... prefixes) {
            this.tablePrefixes = prefixes;
            return this;
        }

        /** 生成类名时剥离的表后缀，可配多个（按配置顺序匹配第一个）。 */
        public Builder tableSuffix(String... suffixes) {
            this.tableSuffixes = suffixes;
            return this;
        }

        /** 生成实体时忽略的列名（大小写不敏感）。 */
        public Builder ignoreColumn(String... columns) {
            this.ignoreColumns = columns;
            return this;
        }

        /** 生成实体时忽略的列前缀。 */
        public Builder ignoreColumnPrefix(String... prefixes) {
            this.ignoreColumnPrefixes = prefixes;
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
            this.excludeTables = copyOf(excludeTables);
            this.tablePrefixes = copyOf(tablePrefixes);
            this.tableSuffixes = copyOf(tableSuffixes);
            this.ignoreColumns = copyOf(ignoreColumns);
            this.ignoreColumnPrefixes = copyOf(ignoreColumnPrefixes);

            return new StrategyConfig(this);
        }

        private static String[] copyOf(String[] source) {
            return source == null ? null : Arrays.copyOf(source, source.length);
        }
    }
}
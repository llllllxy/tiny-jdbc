package org.tinycloud.jdbc.codegen.config;

import java.io.File;

/**
 * <p>
 *  代码生成器的核心配置类，封装代码生成所需的各种配置信息，采用建造者模式构建
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-03-21 11:22
 */
public class CodegenConfig {
    /** 数据源配置信息 */
    private final DataSourceConfig dataSourceConfig;
    /** 包结构配置信息 */
    private final PackageConfig packageConfig;
    /** 生成策略配置信息 */
    private final StrategyConfig strategyConfig;
    /** 输出目录配置信息 */
    private final String outputDir;

    private CodegenConfig(Builder builder) {
        this.dataSourceConfig = builder.dataSourceConfig;
        this.packageConfig = builder.packageConfig;
        this.strategyConfig = builder.strategyConfig;
        this.outputDir = builder.outputDir;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ===== Getter =====
    public DataSourceConfig getDataSourceConfig() {
        return dataSourceConfig;
    }

    public PackageConfig getPackageConfig() {
        return packageConfig;
    }

    public StrategyConfig getStrategyConfig() {
        return strategyConfig;
    }

    public String getOutputDir() {
        return outputDir;
    }

    /**
     * 获取绝对输出路径
     */
    public String getAbsoluteOutputDir() {
        String dir = (outputDir == null || outputDir.isEmpty())
                ? "generated"
                : outputDir;

        File file = new File(dir);
        if (file.isAbsolute()) {
            return file.getAbsolutePath();
        }
        return System.getProperty("user.dir") + File.separator + dir;
    }


    /**
     * CodegenConfig的建造者类，用于链式构建配置实例
     */
    public static class Builder {
        private DataSourceConfig dataSourceConfig;
        private PackageConfig packageConfig;
        private StrategyConfig strategyConfig;
        private String outputDir;

        public Builder dataSourceConfig(DataSourceConfig dataSourceConfig) {
            this.dataSourceConfig = dataSourceConfig;
            return this;
        }

        public Builder packageConfig(PackageConfig packageConfig) {
            this.packageConfig = packageConfig;
            return this;
        }

        public Builder strategyConfig(StrategyConfig strategyConfig) {
            this.strategyConfig = strategyConfig;
            return this;
        }

        public Builder outputDir(String outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        /**
         * 构建CodegenConfig实例，会先校验必要配置的有效性
         * @return 构建好的CodegenConfig实例
         * @throws IllegalArgumentException 当必要配置为空时抛出
         */
        public CodegenConfig build() {
            // ===== 核心校验（必须有）=====
            if (dataSourceConfig == null) {
                throw new IllegalArgumentException("dataSourceConfig 不能为空");
            }
            if (packageConfig == null) {
                throw new IllegalArgumentException("packageConfig 不能为空");
            }
            if (strategyConfig == null) {
                throw new IllegalArgumentException("strategyConfig 不能为空");
            }
            // outputDir 可选，给默认值
            if (outputDir == null || outputDir.isEmpty()) {
                outputDir = "generated";
            }
            return new CodegenConfig(this);
        }
    }
}
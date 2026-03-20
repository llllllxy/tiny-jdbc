package org.tinycloud.jdbc.codegen.config;

import java.io.File;

public class CodegenConfig {

    private final DataSourceConfig dataSourceConfig;
    private final PackageConfig packageConfig;
    private final StrategyConfig strategyConfig;
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

    // ===== Builder =====
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
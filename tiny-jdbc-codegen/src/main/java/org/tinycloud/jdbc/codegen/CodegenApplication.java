package org.tinycloud.jdbc.codegen;

import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.codegen.config.CodegenConfig;
import org.tinycloud.jdbc.codegen.config.DataSourceConfig;
import org.tinycloud.jdbc.codegen.config.PackageConfig;
import org.tinycloud.jdbc.codegen.config.StrategyConfig;


/**
 * <p>
 * 代码生成器示例，用于根据数据库表结构自对应的Java代码
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-03-21 11:22
 */
public class CodegenApplication {
    public static void main(String[] args) throws Exception {
        DataSourceConfig dataSourceConfig = DataSourceConfig.builder()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url("jdbc:mysql://localhost:3306/xxl_job?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useInformationSchema=true")
                .username("root")
                .password("123654")
                .build();

        PackageConfig packageConfig = PackageConfig.builder()
                .parent("com.example")
                .entity("entity")
                .dao("dao")
                .build();

        StrategyConfig strategyConfig = StrategyConfig.builder()
                .includeTables(new String[]{"xxl_job_user", "xxl_job_info"})
                .useActualColumnNames(false)
                .enableLombok(true)
                .idType(IdType.ASSIGN_ID)   // 指定主键策略
                .build();

        CodegenConfig config = CodegenConfig.builder()
                .dataSourceConfig(dataSourceConfig)
                .packageConfig(packageConfig)
                .strategyConfig(strategyConfig)
                .outputDir("generated")
                .author("XXXX")
                .build();

        CodeGenerator generator = new CodeGenerator(config);
        generator.generate();
    }
}
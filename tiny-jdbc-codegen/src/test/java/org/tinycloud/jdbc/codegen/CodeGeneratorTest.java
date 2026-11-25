package org.tinycloud.jdbc.codegen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.codegen.config.CodegenConfig;
import org.tinycloud.jdbc.codegen.config.DataSourceConfig;
import org.tinycloud.jdbc.codegen.config.PackageConfig;
import org.tinycloud.jdbc.codegen.config.StrategyConfig;
import org.tinycloud.jdbc.codegen.meta.ColumnMeta;
import org.tinycloud.jdbc.codegen.meta.TableMeta;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 *  基于内存构造的 {@link TableMeta}/{@link ColumnMeta}，在临时输出目录中生成实体与 DAO，
 * 校验默认/显式 {@link IdType} 输出及类型映射（无需连接数据库）。
 * </p>
 */
public class CodeGeneratorTest {

    private Path tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("codegen-test");
    }

    @After
    public void tearDown() {
        deleteRecursively(tempDir.toFile());
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private CodeGenerator createGenerator(IdType idType) throws Exception {
        return createGenerator(idType, true);
    }

    private CodeGenerator createGenerator(IdType idType, boolean enableLombok) throws Exception {
        StrategyConfig.Builder strategyBuilder = StrategyConfig.builder().includeTables("test_table")
                .enableLombok(enableLombok);
        if (idType != null) {
            strategyBuilder.idType(idType);
        }

        CodegenConfig config = CodegenConfig.builder()
                .dataSourceConfig(DataSourceConfig.builder()
                        .url("jdbc:mysql://127.0.0.1:3306/test")
                        .username("root")
                        .password("root")
                        .build())
                .packageConfig(PackageConfig.builder().parent("com.example").entity("entity").dao("dao").build())
                .strategyConfig(strategyBuilder.build())
                .outputDir(tempDir.toString())
                .author("tester")
                .build();
        return new CodeGenerator(config);
    }

    private TableMeta buildTable(boolean idAutoIncrement) {
        TableMeta table = new TableMeta();
        table.setTableName("test_table");
        table.setRemarks("测试表");

        List<ColumnMeta> columns = new ArrayList<>();

        ColumnMeta id = new ColumnMeta();
        id.setColumnName("id");
        id.setDataType(Types.BIGINT);
        id.setColumnSize(20);
        id.setDecimalDigits(0);
        id.setPrimaryKey(true);
        id.setAutoIncrement(idAutoIncrement);
        id.setNullable(false);
        id.setRemarks("主键");
        columns.add(id);

        ColumnMeta name = new ColumnMeta();
        name.setColumnName("name");
        name.setDataType(Types.VARCHAR);
        name.setColumnSize(64);
        name.setDecimalDigits(0);
        name.setNullable(true);
        name.setRemarks("名称");
        columns.add(name);

        ColumnMeta createTime = new ColumnMeta();
        createTime.setColumnName("create_time");
        createTime.setDataType(Types.DATE);
        createTime.setColumnSize(10);
        createTime.setDecimalDigits(0);
        createTime.setNullable(false);
        createTime.setRemarks("创建时间");
        columns.add(createTime);

        table.setColumns(columns);
        return table;
    }

    private void invokeGenerate(CodeGenerator generator, TableMeta table, boolean entity, boolean dao) throws Exception {
        if (entity) {
            Method entityMethod = CodeGenerator.class.getDeclaredMethod("generateEntity", TableMeta.class);
            entityMethod.setAccessible(true);
            entityMethod.invoke(generator, table);
        }
        if (dao) {
            Method daoMethod = CodeGenerator.class.getDeclaredMethod("generateDao", TableMeta.class);
            daoMethod.setAccessible(true);
            daoMethod.invoke(generator, table);
        }
    }

    private String readFile(String relativePath) throws Exception {
        File file = new File(tempDir.toFile(), relativePath);
        assertTrue("生成文件缺失: " + relativePath, file.exists());
        return new String(Files.readAllBytes(file.toPath()), "UTF-8");
    }

    // ===== UTF-8 输出：中文表/列注释按 UTF-8 原始字节写入 =====
    @Test
    public void testGeneratedSourceUsesUtf8() throws Exception {
        CodeGenerator generator = createGenerator(null);
        invokeGenerate(generator, buildTable(true), true, false);

        Path entityPath = tempDir.resolve("com/example/entity/TestTable.java");
        byte[] bytes = Files.readAllBytes(entityPath);
        String source = new String(bytes, StandardCharsets.UTF_8);
        assertTrue("UTF-8 解码后应保留中文表注释", source.contains("测试表"));
        assertTrue("UTF-8 解码后应保留中文列注释", source.contains("主键"));
        assertTrue("生成内容必须是 UTF-8 编码", contains(bytes, "测试表".getBytes(StandardCharsets.UTF_8)));
    }

    private boolean contains(byte[] source, byte[] target) {
        for (int i = 0; i <= source.length - target.length; i++) {
            int j = 0;
            while (j < target.length && source[i + j] == target[j]) {
                j++;
            }
            if (j == target.length) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testEntityDefaultIdTypeAutoIncrement() throws Exception {
        CodeGenerator generator = createGenerator(null);
        invokeGenerate(generator, buildTable(true), true, true);

        String entity = readFile("com/example/entity/TestTable.java");
        assertTrue("应包含自增主键注解", entity.contains("@Id(idType = IdType.AUTO_INCREMENT)"));

        // 类型映射：BIGINT -> Long，DATE -> LocalDate（带 import）
        assertTrue("BIGINT 应映射为 Long", entity.contains("private Long id;"));
        assertTrue("date 列应导入 java.time.LocalDate", entity.contains("import java.time.LocalDate;"));
        assertTrue("DATE 应映射为 LocalDate", entity.contains("private LocalDate createTime;"));

        // DAO 主键泛型应使用简单类型 Long
        String dao = readFile("com/example/dao/TestTableDao.java");
        assertTrue("DAO 主键泛型应为 Long", dao.contains("extends BaseDao<TestTable, Long>"));
    }

    @Test
    public void testDaoImportsPrimaryKeyType() throws Exception {
        TableMeta table = buildTable(true);
        table.getColumns().get(0).setDataType(Types.DATE);
        CodeGenerator generator = createGenerator(null);
        invokeGenerate(generator, table, false, true);

        String dao = readFile("com/example/dao/TestTableDao.java");
        assertTrue("DAO 应导入主键类型 LocalDate", dao.contains("import java.time.LocalDate;"));
        assertTrue("DAO 主键泛型应为 LocalDate", dao.contains("extends BaseDao<TestTable, LocalDate>"));
    }

    @Test
    public void testEntityWithoutLombokGeneratesAccessors() throws Exception {
        CodeGenerator generator = createGenerator(null, false);
        invokeGenerate(generator, buildTable(true), true, false);

        String entity = readFile("com/example/entity/TestTable.java");
        assertFalse("关闭 Lombok 时不应输出 @Data", entity.contains("@Data"));
        assertTrue("应生成 getter", entity.contains("public Long getId()"));
        assertTrue("应生成 setter", entity.contains("public void setId(Long id)"));
        assertTrue("应生成日期字段 getter", entity.contains("public LocalDate getCreateTime()"));
    }

    // ===== 默认 IdType（未设置）：非自增主键 -> INPUT =====
    @Test
    public void testEntityDefaultIdTypeInput() throws Exception {
        CodeGenerator generator = createGenerator(null);
        invokeGenerate(generator, buildTable(false), true, false);

        String entity = readFile("com/example/entity/TestTable.java");
        assertTrue("非自增主键应生成 INPUT", entity.contains("@Id(idType = IdType.INPUT)"));
    }

    // ===== 显式 IdType：固定使用用户配置 =====
    @Test
    public void testEntityExplicitIdType() throws Exception {
        CodeGenerator generator = createGenerator(IdType.ASSIGN_ID);
        invokeGenerate(generator, buildTable(true), true, false);

        String entity = readFile("com/example/entity/TestTable.java");
        assertTrue("应使用显式配置的 ASSIGN_ID", entity.contains("@Id(idType = IdType.ASSIGN_ID)"));
        assertFalse("显式配置后不应再自动推断为 AUTO_INCREMENT", entity.contains("AUTO_INCREMENT"));
    }

    // ===== 类注释：空表注释时 @author/@date 仍必须输出 =====
    @Test
    public void testEntityCommentEmpty_authorDateAlways() throws Exception {
        CodeGenerator generator = createGenerator(null);

        TableMeta table = buildTable(false);
        table.setRemarks(null);
        invokeGenerate(generator, table, true, false);

        String entity = readFile("com/example/entity/TestTable.java");
        assertTrue("空表注释时也应输出 @author", entity.contains("@author tester"));
        assertTrue("空表注释时也应输出 @date", entity.contains("@date"));
        assertTrue("空表注释时应保留类注释块", entity.contains("/**") && entity.contains("*/"));
        assertFalse("空表注释时不应出现表注释文本", entity.contains("测试表"));
    }

    // ===== 表过滤：excludeTables 优先于 includeTables =====
    @Test
    public void testExcludeTablesTakesPrecedence() throws Exception {
        StrategyConfig strategy = StrategyConfig.builder()
                .includeTables("test_table", "excluded_table")
                .excludeTables("excluded_table")
                .build();
        CodegenConfig config = CodegenConfig.builder()
                .dataSourceConfig(DataSourceConfig.builder().url("jdbc:mysql://x").username("u").password("p").build())
                .packageConfig(PackageConfig.builder().parent("com.example").entity("entity").dao("dao").build())
                .strategyConfig(strategy).outputDir(tempDir.toString()).build();
        CodeGenerator generator = new CodeGenerator(config);

        java.lang.reflect.Method filter = CodeGenerator.class.getDeclaredMethod("filterTables", List.class);
        filter.setAccessible(true);
        TableMeta keep = new TableMeta();
        keep.setTableName("test_table");
        TableMeta drop = new TableMeta();
        drop.setTableName("excluded_table");
        TableMeta other = new TableMeta();
        other.setTableName("other_table");

        @SuppressWarnings("unchecked")
        List<TableMeta> result = (List<TableMeta>) filter.invoke(generator,
                java.util.Arrays.asList(keep, drop, other));
        assertEqualsInt("只应保留 test_table", 1, result.size());
        assertTrue("保留 test_table", "test_table".equals(result.get(0).getTableName()));
    }

    // ===== 表名前缀/后缀剥离 =====
    @Test
    public void testTablePrefixAndSuffixStripped() throws Exception {
        StrategyConfig strategy = StrategyConfig.builder()
                .includeTables("t_xxl_job_info_log")
                .tablePrefix("t_").tableSuffix("_log")
                .build();
        CodegenConfig config = CodegenConfig.builder()
                .dataSourceConfig(DataSourceConfig.builder().url("jdbc:mysql://x").username("u").password("p").build())
                .packageConfig(PackageConfig.builder().parent("com.example").entity("entity").dao("dao").build())
                .strategyConfig(strategy).outputDir(tempDir.toString()).build();
        CodeGenerator generator = new CodeGenerator(config);

        java.lang.reflect.Method resolve = CodeGenerator.class.getDeclaredMethod("resolveClassName", String.class);
        resolve.setAccessible(true);
        Object className = resolve.invoke(generator, "t_xxl_job_info_log");
        assertTrue("前后缀剥离后类名应为 XxlJobInfo", "XxlJobInfo".equals(className));
    }

    // ===== 列忽略：精确列名 + 前缀 =====
    @Test
    public void testIgnoreColumnsAndPrefixes() throws Exception {
        StrategyConfig strategy = StrategyConfig.builder()
                .includeTables("test_table")
                .ignoreColumn("name").ignoreColumnPrefix("create_")
                .build();
        CodegenConfig config = CodegenConfig.builder()
                .dataSourceConfig(DataSourceConfig.builder().url("jdbc:mysql://x").username("u").password("p").build())
                .packageConfig(PackageConfig.builder().parent("com.example").entity("entity").dao("dao").build())
                .strategyConfig(strategy).outputDir(tempDir.toString()).build();
        CodeGenerator generator = new CodeGenerator(config);
        invokeGenerate(generator, buildTable(true), true, false);

        String entity = readFile("com/example/entity/TestTable.java");
        assertFalse("name 列应被忽略", entity.contains("private String name;"));
        assertFalse("create_time 列应被前缀忽略", entity.contains("private LocalDate createTime;"));
        assertTrue("主键应保留", entity.contains("private Long id;"));
    }

    // ===== fileOverride=false：已存在文件跳过且不覆盖 =====
    @Test
    public void testFileOverrideFalseSkipsExistingFile() throws Exception {
        CodeGenerator generator = createGenerator(null);
        invokeGenerate(generator, buildTable(true), true, false);
        File entityFile = tempDir.resolve("com/example/entity/TestTable.java").toFile();
        Files.write(entityFile.toPath(), "// user edited\n".getBytes(StandardCharsets.UTF_8));

        invokeGenerate(generator, buildTable(true), true, false);
        String content = new String(Files.readAllBytes(entityFile.toPath()), "UTF-8");
        assertTrue("默认不覆盖用户手改文件", content.startsWith("// user edited"));
        assertFalse("不应产生 .bak 文件", new File(entityFile.getAbsolutePath() + ".bak").exists());
    }

    // ===== fileOverride=true：备份原文件为 .java.bak 后覆盖 =====
    @Test
    public void testFileOverrideTrueBacksUpThenOverwrites() throws Exception {
        StrategyConfig strategy = StrategyConfig.builder().includeTables("test_table").build();
        CodegenConfig config = CodegenConfig.builder()
                .dataSourceConfig(DataSourceConfig.builder().url("jdbc:mysql://x").username("u").password("p").build())
                .packageConfig(PackageConfig.builder().parent("com.example").entity("entity").dao("dao").build())
                .strategyConfig(strategy).outputDir(tempDir.toString()).author("tester")
                .fileOverride(true).build();
        CodeGenerator generator = new CodeGenerator(config);

        invokeGenerate(generator, buildTable(true), true, false);
        File entityFile = tempDir.resolve("com/example/entity/TestTable.java").toFile();
        Files.write(entityFile.toPath(), "// user edited\n".getBytes(StandardCharsets.UTF_8));

        invokeGenerate(generator, buildTable(true), true, false);
        String content = new String(Files.readAllBytes(entityFile.toPath()), "UTF-8");
        assertTrue("应覆盖为生成内容", content.contains("@Table(\"test_table\")"));
        assertTrue("应保留 .bak 备份", new File(entityFile.getAbsolutePath() + ".bak").exists());
        String backup = new String(Files.readAllBytes(
                new File(entityFile.getAbsolutePath() + ".bak").toPath()), "UTF-8");
        assertTrue("备份应是用户手改内容", backup.startsWith("// user edited"));
    }

    private static void assertEqualsInt(String message, int expected, int actual) {
        assertTrue(message + " expected=" + expected + " actual=" + actual, expected == actual);
    }

    // ===== 类注释：有表注释时 @author/@date 仍必须输出，且包含表注释 =====
    @Test
    public void testEntityCommentPresent_authorDateAlways() throws Exception {
        CodeGenerator generator = createGenerator(null);

        TableMeta table = buildTable(true);
        table.setRemarks("业务表");
        invokeGenerate(generator, table, true, false);

        String entity = readFile("com/example/entity/TestTable.java");
        assertTrue("有表注释时也应输出 @author", entity.contains("@author tester"));
        assertTrue("有表注释时也应输出 @date", entity.contains("@date"));
        assertTrue("有表注释时应包含表注释文本", entity.contains("业务表"));
    }
}

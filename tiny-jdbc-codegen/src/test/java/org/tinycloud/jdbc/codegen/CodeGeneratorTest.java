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
        StrategyConfig.Builder strategyBuilder = StrategyConfig.builder().includeTables("test_table");
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

    // ===== 默认 IdType（未设置）：自增主键 -> AUTO_INCREMENT =====
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
}

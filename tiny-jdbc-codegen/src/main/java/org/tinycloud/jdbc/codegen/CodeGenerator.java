package org.tinycloud.jdbc.codegen;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.tinycloud.jdbc.codegen.config.CodegenConfig;
import org.tinycloud.jdbc.codegen.meta.ColumnMeta;
import org.tinycloud.jdbc.codegen.meta.DatabaseMeta;
import org.tinycloud.jdbc.codegen.meta.TableMeta;
import org.tinycloud.jdbc.codegen.util.TypeUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.*;


/**
 * <p>
 *  FreeMarker 实现的代码生成器核心代码
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-03-21 11:22
 */
public class CodeGenerator {

    private final CodegenConfig config;
    private final Configuration cfg;

    public CodeGenerator(CodegenConfig config) throws Exception {
        this.config = config;
        this.cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
        // 强制 FreeMarker 在输出 boolean 时使用 "true"/"false"
        cfg.setSetting("boolean_format", "c");
    }

    /**
     * 主生成方法
     */
    public void generate() throws Exception {
        DatabaseMeta databaseMeta = new DatabaseMeta(config.getDataSourceConfig());
        List<TableMeta> allTables = databaseMeta.getTables();
        List<TableMeta> filteredTables = filterTables(allTables);

        for (TableMeta table : filteredTables) {
            validateTable(table);
            generateEntity(table);
            generateDao(table);
        }

        System.out.println("代码生成完成！");
    }

    /**
     * 过滤需要生成的表
     */
    private List<TableMeta> filterTables(List<TableMeta> tables) {
        String[] includeTables = config.getStrategyConfig().getIncludeTables();
        if (includeTables == null || includeTables.length == 0) {
            return tables;
        }

        Set<String> includeSet = new LinkedHashSet<>();
        for (String table : includeTables) {
            includeSet.add(table.toLowerCase());
        }

        List<TableMeta> filtered = new ArrayList<>();
        for (TableMeta table : tables) {
            if (includeSet.contains(table.getTableName().toLowerCase())) {
                filtered.add(table);
            }
        }
        return filtered;
    }

    /**
     * 暂不支持联合主键
     */
    private void validateTable(TableMeta table) {
        List<ColumnMeta> primaryKeys = table.getPrimaryKeys();
        if (primaryKeys.size() > 1) {
            throw new IllegalStateException("暂不支持联合主键，表名: " + table.getTableName());
        }
    }

    /**
     * 生成实体类
     */
    private void generateEntity(TableMeta table) throws Exception {
        String className = TypeUtils.getCamelCase(table.getTableName());
        String packageName = config.getPackageConfig().getEntityPackage();
        String outputDir = buildOutputDir(packageName);

        File dir = new File(outputDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("创建目录失败: " + outputDir);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("packageName", packageName);
        model.put("className", className);
        model.put("tableName", table.getTableName());
        model.put("tableComment", safeString(table.getRemarks()));
        model.put("enableLombok", config.getStrategyConfig().isEnableLombok());
        model.put("createDate", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        List<ColumnInfo> columns = new ArrayList<>();
        Set<String> importTypes = new LinkedHashSet<>();

        for (ColumnMeta column : table.getColumns()) {
            ColumnInfo columnInfo = new ColumnInfo();
            columnInfo.setColumnName(column.getColumnName());

            String fullJavaType = TypeUtils.getJavaType(column.getDataType(), column.getDecimalDigits());
            columnInfo.setJavaType(TypeUtils.getSimpleJavaType(fullJavaType));

            String fieldName;
            if (config.getStrategyConfig().isUseActualColumnNames()) {
                fieldName = column.getColumnName();
            } else {
                fieldName = TypeUtils.getLowerCamelCase(column.getColumnName());
            }
            columnInfo.setFieldName(fieldName);

            columnInfo.setPrimaryKey(column.isPrimaryKey());
            columnInfo.setAutoIncrement(column.isAutoIncrement());
            columnInfo.setNullable(column.isNullable());
            columnInfo.setComment(safeString(column.getRemarks()));
            if (column.isPrimaryKey()) {
                // 优先使用用户配置
                if (config.getStrategyConfig().getIdType() != null) {
                    columnInfo.setIdType(config.getStrategyConfig().getIdType().name());
                } else {
                    columnInfo.setIdType(column.isAutoIncrement() ? "AUTO_INCREMENT" : "INPUT");
                }
            }

            if (TypeUtils.needImport(fullJavaType)) {
                importTypes.add(fullJavaType);
            }

            columns.add(columnInfo);
        }

        model.put("imports", importTypes);
        model.put("columns", columns);

        Template template = cfg.getTemplate("entity.ftl");
        File file = new File(outputDir, className + ".java");
        try (Writer writer = new FileWriter(file)) {
            template.process(model, writer);
        }

        System.out.println("生成实体类: " + file.getAbsolutePath());
    }

    /**
     * 生成 DAO 类
     */
    private void generateDao(TableMeta table) throws Exception {
        String entityClassName = TypeUtils.getCamelCase(table.getTableName());
        String daoClassName = entityClassName + "Dao";
        String packageName = config.getPackageConfig().getDaoPackage();
        String entityPackageName = config.getPackageConfig().getEntityPackage();
        String outputDir = buildOutputDir(packageName);

        File dir = new File(outputDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("创建目录失败: " + outputDir);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("packageName", packageName);
        model.put("className", daoClassName);
        model.put("entityClassName", entityClassName);
        model.put("entityPackageName", entityPackageName);
        model.put("tableComment", safeString(table.getRemarks()));
        model.put("createDate", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        String idType = "Void";
        List<ColumnMeta> primaryKeys = table.getPrimaryKeys();
        if (!primaryKeys.isEmpty()) {
            ColumnMeta pk = primaryKeys.get(0);
            String fullJavaType = TypeUtils.getJavaType(pk.getDataType(), pk.getDecimalDigits());
            idType = TypeUtils.getSimpleJavaType(fullJavaType);
        }
        model.put("idType", idType);

        Template template = cfg.getTemplate("dao.ftl");
        File file = new File(outputDir, daoClassName + ".java");
        try (Writer writer = new FileWriter(file)) {
            template.process(model, writer);
        }

        System.out.println("生成DAO类: " + file.getAbsolutePath());
    }

    /**
     * 根据包名计算输出目录
     */
    private String buildOutputDir(String packageName) {
        return new File(config.getAbsoluteOutputDir(),
                packageName.replace('.', File.separatorChar)).getAbsolutePath();
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 内部类，用于模板列信息
     */
    public static class ColumnInfo {
        private String columnName;
        private String javaType;
        private String fieldName;
        private boolean primaryKey;
        private boolean autoIncrement;
        private boolean nullable;
        private String idType;
        private String comment;

        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getJavaType() { return javaType; }
        public void setJavaType(String javaType) { this.javaType = javaType; }
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public boolean isPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }
        public boolean isAutoIncrement() { return autoIncrement; }
        public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        public String getIdType() { return idType; }
        public void setIdType(String idType) { this.idType = idType; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
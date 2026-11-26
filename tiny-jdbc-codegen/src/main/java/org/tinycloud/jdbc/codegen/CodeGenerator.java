package org.tinycloud.jdbc.codegen;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.tinycloud.jdbc.codegen.config.CodegenConfig;
import org.tinycloud.jdbc.codegen.meta.ColumnMeta;
import org.tinycloud.jdbc.codegen.meta.DatabaseMeta;
import org.tinycloud.jdbc.codegen.meta.TableMeta;
import org.tinycloud.jdbc.codegen.util.TypeUtils;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * <p>
 * FreeMarker 实现的代码生成器核心代码
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
        Set<String> excludeSet = toLowerCaseSet(config.getStrategyConfig().getExcludeTables());
        for (TableMeta table : tables) {
            String lowerName = table.getTableName().toLowerCase();
            // excludeTables 优先级最高
            if (excludeSet.contains(lowerName)) {
                continue;
            }
            if (includeSet.isEmpty() || includeSet.contains(lowerName)) {
                filtered.add(table);
            }
        }
        return filtered;
    }

    private static Set<String> toLowerCaseSet(String[] values) {
        Set<String> set = new java.util.HashSet<String>();
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    set.add(value.toLowerCase());
                }
            }
        }
        return set;
    }

    /** 剥离配置的表前缀/后缀后再转驼峰类名。 */
    private String resolveClassName(String tableName) {
        String name = tableName;
        String[] prefixes = config.getStrategyConfig().getTablePrefixes();
        if (prefixes != null) {
            for (String prefix : prefixes) {
                if (prefix != null && !prefix.isEmpty() && name.toLowerCase().startsWith(prefix.toLowerCase())) {
                    name = name.substring(prefix.length());
                    break;
                }
            }
        }
        String[] suffixes = config.getStrategyConfig().getTableSuffixes();
        if (suffixes != null) {
            for (String suffix : suffixes) {
                if (suffix != null && !suffix.isEmpty() && name.toLowerCase().endsWith(suffix.toLowerCase())) {
                    name = name.substring(0, name.length() - suffix.length());
                    break;
                }
            }
        }
        return TypeUtils.getCamelCase(name);
    }

    /**
     * 识别 AuditMetaObjectHandler 自动填充字段：
     * create_user_id / create_time 仅 INSERT 填充，update_user_id / update_time 在 INSERT 与 UPDATE 都填充。
     * 按列名转驼峰后匹配，不受 useActualColumnNames 影响。
     *
     * @return 空串表示非填充字段；否则为 "INSERT" 或 "INSERT / UPDATE"
     */
    private static String resolveFillStage(String columnName) {
        String property = TypeUtils.getLowerCamelCase(columnName);
        if ("createUserId".equals(property) || "createTime".equals(property)) {
            return "INSERT";
        }
        if ("updateUserId".equals(property) || "updateTime".equals(property)) {
            return "INSERT / UPDATE";
        }
        return "";
    }

    /** 列是否应被忽略（大小写不敏感，支持前缀匹配）。 */
    private boolean isColumnIgnored(String columnName) {
        String lower = columnName.toLowerCase();
        String[] columns = config.getStrategyConfig().getIgnoreColumns();
        if (columns != null) {
            for (String ignored : columns) {
                if (ignored != null && lower.equals(ignored.toLowerCase())) {
                    return true;
                }
            }
        }
        String[] prefixes = config.getStrategyConfig().getIgnoreColumnPrefixes();
        if (prefixes != null) {
            for (String prefix : prefixes) {
                if (prefix != null && !prefix.isEmpty() && lower.startsWith(prefix.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 暂不支持联合主键
     */
    private void validateTable(TableMeta table) {
        List<ColumnMeta> primaryKeys = table.getPrimaryKeys();
        if (primaryKeys.isEmpty()) {
            throw new IllegalStateException("暂不支持无主键表，表名: " + table.getTableName());
        }
        if (primaryKeys.size() > 1) {
            throw new IllegalStateException("暂不支持联合主键，表名: " + table.getTableName());
        }
    }

    /**
     * 生成实体类
     */
    private void generateEntity(TableMeta table) throws Exception {
        String className = String.format(config.getStrategyConfig().getFormatEntityName(),
                this.resolveClassName(table.getTableName()));
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
        model.put("tableComment", table.getRemarks() == null ? "" : table.getRemarks());
        model.put("enableLombok", config.getStrategyConfig().isEnableLombok());
        model.put("createDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        model.put("author", config.getAuthor());

        List<ColumnInfo> columns = new ArrayList<>();
        Set<String> importTypes = new LinkedHashSet<>();

        for (ColumnMeta column : table.getColumns()) {
            if (this.isColumnIgnored(column.getColumnName())) {
                continue;
            }
            ColumnInfo columnInfo = new ColumnInfo();
            columnInfo.setColumnName(column.getColumnName());

            String fullJavaType = TypeUtils.getJavaType(column.getDataType(), column.getColumnSize(), column.getDecimalDigits());
            columnInfo.setJavaType(TypeUtils.getSimpleJavaType(fullJavaType));

            String fieldName;
            if (config.getStrategyConfig().isUseActualColumnNames()) {
                fieldName = column.getColumnName();
            } else {
                fieldName = TypeUtils.getLowerCamelCase(column.getColumnName());
            }
            columnInfo.setFieldName(fieldName);

            columnInfo.setFillStage(this.resolveFillStage(column.getColumnName()));
            columnInfo.setPrimaryKey(column.isPrimaryKey());
            columnInfo.setAutoIncrement(column.isAutoIncrement());
            columnInfo.setNullable(column.isNullable());
            columnInfo.setComment(column.getRemarks() == null ? "" : column.getRemarks());
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
        if (!this.writeTemplate(template, model, file)) {
            System.out.println("跳过已存在文件（如需覆盖请设置 fileOverride=true）: " + file.getAbsolutePath());
            return;
        }
        System.out.println("生成实体类: " + file.getAbsolutePath());
    }

    /**
     * 生成 DAO 类
     */
    private void generateDao(TableMeta table) throws Exception {
        String entityClassName = String.format(config.getStrategyConfig().getFormatEntityName(),
                this.resolveClassName(table.getTableName()));
        String daoClassName = String.format(config.getStrategyConfig().getFormatDaoName(), entityClassName);
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
        model.put("tableComment", table.getRemarks() == null ? "" : table.getRemarks());
        model.put("createDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        model.put("author", config.getAuthor());

        String idType = "Void";
        Set<String> imports = new LinkedHashSet<>();
        List<ColumnMeta> primaryKeys = table.getPrimaryKeys();
        if (!primaryKeys.isEmpty()) {
            ColumnMeta pk = primaryKeys.get(0);
            String fullJavaType = TypeUtils.getJavaType(pk.getDataType(), pk.getColumnSize(), pk.getDecimalDigits());
            idType = TypeUtils.getSimpleJavaType(fullJavaType);
            if (TypeUtils.needImport(fullJavaType)) {
                imports.add(fullJavaType);
            }
        }
        model.put("idType", idType);
        model.put("imports", imports);

        Template template = cfg.getTemplate("dao.ftl");
        File file = new File(outputDir, daoClassName + ".java");
        if (!this.writeTemplate(template, model, file)) {
            System.out.println("跳过已存在文件（如需覆盖请设置 fileOverride=true）: " + file.getAbsolutePath());
            return;
        }
        System.out.println("生成DAO类: " + file.getAbsolutePath());
    }

    /**
     * 将模板渲染写入目标文件。
     *
     * @return true 表示已写入；false 表示文件已存在且未开启 fileOverride（已跳过）
     */
    private boolean writeTemplate(Template template, Map<String, Object> model, File file) throws Exception {
        if (file.exists()) {
            if (!config.isFileOverride()) {
                return false;
            }
            File backup = new File(file.getAbsolutePath() + ".bak");
            Files.deleteIfExists(backup.toPath());
            Files.move(file.toPath(), backup.toPath());
        }
        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("创建目录失败: " + dir.getAbsolutePath());
        }
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            template.process(model, writer);
        }
        return true;
    }

    /**
     * 根据包名计算输出目录
     */
    private String buildOutputDir(String packageName) {
        return new File(config.getAbsoluteOutputDir(),
                packageName.replace('.', File.separatorChar)).getAbsolutePath();
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
        private String fillStage;

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getJavaType() {
            return javaType;
        }

        public void setJavaType(String javaType) {
            this.javaType = javaType;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
        }

        public boolean isAutoIncrement() {
            return autoIncrement;
        }

        public void setAutoIncrement(boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }

        public String getIdType() {
            return idType;
        }

        public void setIdType(String idType) {
            this.idType = idType;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getFillStage() {
            return fillStage;
        }

        public void setFillStage(String fillStage) {
            this.fillStage = fillStage;
        }
    }
}

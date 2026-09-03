package org.tinycloud.jdbc.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.id.IdContext;
import org.tinycloud.jdbc.id.IdGeneratorRouter;
import org.tinycloud.jdbc.util.ReflectUtils;
import org.tinycloud.jdbc.util.StrUtils;
import org.tinycloud.jdbc.util.TableInfo;
import org.tinycloud.jdbc.util.TableParserUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * SQL 组装器：把「领域模型 / 条件构造器 / 主键列表」翻译成可执行的 SQL 及其绑定参数。
 *
 * <p><b>职责边界</b>：本类<b>只负责组装 SQL</b>，不负责执行。真正执行 SQL
 * （连接获取、拦截器链、结果映射）由 {@link AbstractSqlSupport} / {@code SqlExecutor} 承担；
 * 调用方拿到的只是 {@link SqlProvider}（sql + parameters）或 {@link BatchInsertSql}，仅作为
 * 待执行 / 待提交的 SQL 载体。</p>
 *
 * <h3>输入</h3>
 * <ul>
 *   <li><b>实体对象</b> → 单行 {@code INSERT} / {@code UPDATE}（byId）/ {@code DELETE} / {@code SELECT}；</li>
 *   <li><b>条件构造器</b>（{@link QueryCriteria} / {@link LambdaQueryCriteria} /
 *       {@link UpdateCriteria} / {@link LambdaUpdateCriteria}）→ {@code SELECT} / {@code COUNT} /
 *       {@code UPDATE} / {@code DELETE}；</li>
 *   <li><b>主键或主键集合</b> → {@code SELECT ... BY id(s)} / {@code DELETE ... BY id(s)}；</li>
 *   <li><b>实体集合</b> → 多值批量 {@code INSERT}（返回 {@link BatchInsertSql}）。</li>
 * </ul>
 *
 * <h3>统一收敛的元数据规则</h3>
 * <p>所有方法均基于 {@link TableParserUtils} 缓存的 {@link TableInfo} 元数据解析：
 * {@code @Table} 决定表名、{@code @Column}（含 {@code exist=false}）决定属性↔列映射、
 * {@code @Id} + {@code IdType} 决定主键策略。因此「属性 ↔ 列 ↔ 生成 SQL」只有一套规则、一处实现。
 * 具体包括：</p>
 * <ul>
 *   <li>跳过 {@code @Column(exist=false)} 字段；</li>
 *   <li>自增主键（{@code IdType.AUTO_INCREMENT}）从插入列集剔除，交由数据库生成，不写回实体；</li>
 *   <li>其余主键策略经 {@link IdGeneratorRouter} 生成并<b>回写</b>实体字段；</li>
 *   <li>{@code ignoreNulls} 为 {@code true} 时跳过 {@code null} 字段；</li>
 *   <li>按主键操作（byId / byIds）要求实体声明唯一的 {@code @Id}，缺失时明确报错。</li>
 * </ul>
 *
 * <h3>批量插入</h3>
 * <p>{@link #buildBatchInsert} 生成<b>稳定列集 + 行参数</b>载体：列集取自集合内首个实体，
 * 其余实体列集必须与其一致（多值 INSERT 要求所有行共享同一列），否则抛出异常；自增主键始终剔除。
 * 载体的行参数供 {@code MULTI_VALUE} 多值 INSERT 或进一步切块使用。</p>
 *
 * <p><b>注意</b>：本类全部为<b>静态无状态</b>方法；部分方法（插入 / 批量）因序列型主键生成，
 * 需要传入 {@link JdbcTemplate} 与 {@link TinyJdbcRuntime}。</p>
 *
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
public class SqlAssembler {

    /**
     * 构建插入SQL
     *
     * @param object 入参
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider buildInsertSql(Object object, boolean ignoreNulls, JdbcTemplate jdbcTemplate, TinyJdbcRuntime tinyJdbcRuntime) {
        Field[] fields = TableParserUtils.resolveFields(object);
        String tableName = TableParserUtils.getTableName(object);
        TableInfo tableInfo = TableParserUtils.getTableInfo(object.getClass());

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        SqlProvider sqlProvider = new SqlProvider();

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Class<?> fieldType = field.getType();
            String fieldName = field.getName();
            Column columnAnnotation = field.getAnnotation(Column.class);
            Id idAnnotation = field.getAnnotation(Id.class);
            String column;
            if (columnAnnotation != null && !columnAnnotation.exist()) {
                continue;
            }
            column = tableInfo.getColumn(fieldName);
            Object fieldValue;
            try {
                fieldValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            // 如果是主键列
            if (idAnnotation != null) {
                // 处理主键生成/赋值，返回最终的主键值（可能是自动生成的）
                fieldValue = processPrimaryKey(field, fieldValue, fieldName, fieldType, idAnnotation, object, jdbcTemplate, tinyJdbcRuntime);
                // 为自增主键时，返回 null，此时跳过该字段（无需加入 SQL）
                if (fieldValue == null) {
                    // 自增主键：跳过列/值，但保存 Field 到 SqlProvider，后续处理时需要使用
                    sqlProvider.setAutoIncrementPrimaryKeyField(field);
                    continue;
                }
            }

            // 判断是否忽略null
            if (ignoreNulls && Objects.isNull(fieldValue)) {
                continue;
            }
            columns.append(column).append(",");
            values.append("?").append(",");
            parameters.add(fieldValue);
        }
        if (columns.length() == 0) {
            throw new TinyJdbcException("No valid columns to insert! All fields are marked as exist=false or ignored.");
        }

        String tableColumns = columns.subSequence(0, columns.length() - 1).toString();
        String tableValues = values.subSequence(0, values.length() - 1).toString();
        sql.append("INSERT INTO ").append(tableName);
        sql.append(" (").append(tableColumns).append(")");
        sql.append(" VALUES (").append(tableValues).append(")");

        sqlProvider.setSql(sql.toString());
        sqlProvider.setParameters(parameters);
        return sqlProvider;
    }

    /**
     * 构建批量插入载体：一份稳定列集 + 一组行参数，供多值 INSERT 或批量 upsert 复用。
     *
     * <p>列集取自集合内<b>首个</b>实体；其余实体必须与其列集完全一致，否则抛出异常
     * （多值 INSERT 要求所有行共享同一列集）。自增主键始终从列集剔除（由数据库生成）。</p>
     *
     * @param objects           待插入实体集合，不能为空
     * @param ignoreNulls       是否忽略 null 字段
     * @param jdbcTemplate      JdbcTemplate（用于序列类主键生成）
     * @param tinyJdbcRuntime   运行时上下文（用于主键生成分派）
     * @return 批量插入载体
     */
    public static BatchInsertSql buildBatchInsert(Collection<?> objects, boolean ignoreNulls,
                                                  JdbcTemplate jdbcTemplate, TinyJdbcRuntime tinyJdbcRuntime) {
        if (objects == null || objects.isEmpty()) {
            throw new TinyJdbcException("buildBatchInsert collection cannot be null or empty");
        }
        List<?> list = new ArrayList<>(objects);
        // 校验所有实体类型一致且非 null：表名、主键等元数据取自首元素，若后续元素来自不同实体类，
        // 即使列集恰好相同也可能把值写入错误的表（静默数据错写），因此先做整体校验。
        Class<?> entityClass = null;
        for (int i = 0; i < list.size(); i++) {
            Object entity = list.get(i);
            if (entity == null) {
                throw new TinyJdbcException("batchInsert entity at index " + i + " cannot be null");
            }
            if (entityClass == null) {
                entityClass = entity.getClass();
            } else if (entity.getClass() != entityClass) {
                throw new TinyJdbcException("batchInsert requires all entities to have the same type, expected "
                        + entityClass.getName() + ", but index " + i + " is " + entity.getClass().getName());
            }
        }
        TableInfo tableInfo = TableParserUtils.getTableInfo(entityClass);
        String tableName = tableInfo.getTableName();

        List<ColumnItem> firstItems = resolveInsertColumns(list.get(0), ignoreNulls, jdbcTemplate, tinyJdbcRuntime);
        if (firstItems.isEmpty()) {
            throw new TinyJdbcException("No valid columns to insert! All fields are marked as exist=false or ignored.");
        }
        List<String> columns = firstItems.stream().map(ColumnItem::getColumn).collect(Collectors.toList());
        List<Object[]> rows = new ArrayList<>();
        rows.add(firstItems.stream().map(ColumnItem::getValue).toArray());

        for (int i = 1; i < list.size(); i++) {
            Object obj = list.get(i);
            List<ColumnItem> items = resolveInsertColumns(obj, ignoreNulls, jdbcTemplate, tinyJdbcRuntime);
            if (!sameColumns(columns, items)) {
                throw new TinyJdbcException("batchInsert requires the same columns for all entities, first columns: "
                        + columns + ", current columns: "
                        + items.stream().map(ColumnItem::getColumn).collect(Collectors.toList()));
            }
            rows.add(items.stream().map(ColumnItem::getValue).toArray());
        }

        String pkColumn = tableInfo.getPrimaryKeyColumn();
        boolean autoIncrement = pkColumn != null && !columns.contains(pkColumn);
        return new BatchInsertSql(tableName, columns, rows, pkColumn, autoIncrement);
    }

    /**
     * 单实体插入列解析：返回需要写入的列及其取值（含主键处理）。
     *
     * <p>遵循与 {@link #buildInsertSql} 一致的规则：跳过 {@code @Column(exist=false)}；
     * 自增主键返回 null 时跳过该列；{@code ignoreNulls} 为 true 时跳过 null 值列。</p>
     */
    private static List<ColumnItem> resolveInsertColumns(Object object, boolean ignoreNulls,
                                                         JdbcTemplate jdbcTemplate, TinyJdbcRuntime tinyJdbcRuntime) {
        TableInfo tableInfo = TableParserUtils.getTableInfo(object.getClass());
        Field[] fields = TableParserUtils.resolveFields(object);
        List<ColumnItem> items = new ArrayList<>();
        for (Field field : fields) {
            if (!tableInfo.isPersistentField(field.getName())) {
                continue;
            }
            ReflectUtils.makeAccessible(field);
            Id idAnnotation = field.getAnnotation(Id.class);
            String column = tableInfo.getColumn(field.getName());
            Object value = getFieldValue(field, object);
            if (idAnnotation != null) {
                value = processPrimaryKey(field, value, field.getName(), field.getType(), idAnnotation, object, jdbcTemplate, tinyJdbcRuntime);
                if (value == null) {
                    // 自增主键：由数据库生成，剔除该列
                    continue;
                }
            }
            if (ignoreNulls && value == null) {
                continue;
            }
            items.add(new ColumnItem(column, value));
        }
        return items;
    }

    private static Object getFieldValue(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new TinyJdbcException("get field value failed: " + field.getName(), e);
        }
    }

    private static boolean sameColumns(List<String> columns, List<ColumnItem> items) {
        if (columns.size() != items.size()) {
            return false;
        }
        for (int i = 0; i < columns.size(); i++) {
            if (!columns.get(i).equals(items.get(i).getColumn())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 单实体插入列项：列名 + 取值。
     */
    private static final class ColumnItem {
        private final String column;
        private final Object value;

        ColumnItem(String column, Object value) {
            this.column = column;
            this.value = value;
        }

        String getColumn() {
            return column;
        }

        Object getValue() {
            return value;
        }
    }

    /**
     * 抽取的私有方法：处理主键字段的生成、赋值逻辑
     *
     * @param field        主键字段
     * @param fieldValue   原始字段值（可能为 null）
     * @param fieldName    字段名（用于异常提示）
     * @param fieldType    字段类型（用于校验）
     * @param idAnnotation Id注解（包含主键策略等信息）
     * @param object       实体对象（用于将生成的主键值塞回）
     * @param jdbcTemplate JdbcTemplate（用于序列查询）
     * @return 最终的主键值（自增主键返回 null，需跳过）
     */
    private static Object processPrimaryKey(Field field, Object fieldValue, String fieldName, Class<?> fieldType,
                                            Id idAnnotation, Object object, JdbcTemplate jdbcTemplate, TinyJdbcRuntime tinyJdbcRuntime) {
        // 只有用户没有自己设置主键值时，才需要走自动生成的策略
        if (Objects.isNull(fieldValue)) {
            IdType idType = idAnnotation.idType();
            if (idType == IdType.AUTO_INCREMENT) {
                // 自增主键：返回 null，外层逻辑会跳过该字段
                return null;
            }
            // 其它策略统一交给 IdGeneratorRouter 按 IdType 分发到对应生成器实现，并回写实体字段
            IdContext context = IdContext.builder()
                    .obj(object)
                    .field(field)
                    .fieldType(fieldType)
                    .fieldName(fieldName)
                    .idType(idType)
                    .sequenceSql(idAnnotation.value())
                    .jdbcTemplate(jdbcTemplate)
                    .build();
            return new IdGeneratorRouter(tinyJdbcRuntime).generate(context);
        }
        // 用户已设置主键值：原样返回
        return fieldValue;
    }

    /**
     * 构建更新SQL
     *
     * @param object 入参
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider buildUpdateByIdSql(Object object, boolean ignoreNulls) {
        Field[] fields = TableParserUtils.resolveFields(object);
        String tableName = TableParserUtils.getTableName(object);
        TableInfo tableInfo = TableParserUtils.getTableInfo(object.getClass());

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();
        Object whereValues = null;
        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            Id idAnnotation = field.getAnnotation(Id.class);
            String column;
            if (columnAnnotation != null && !columnAnnotation.exist()) {
                continue;
            }
            column = tableInfo.getColumn(field.getName());
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            if (idAnnotation != null) {
                // 多 @Id 校验已由 TableInfo 统一保证（仅一个主键），此处直接定位主键列
                whereColumns.append(column);
                whereValues = filedValue;
                continue;
            }
            // 是否忽略null
            if (ignoreNulls && filedValue == null) {
                continue;
            }
            columns.append(column).append("=?,");
            parameters.add(filedValue);
        }
        if (whereValues == null) {
            throw new TinyJdbcException("SqlAssembler buildUpdateByIdSql primaryKeyId can not null!");
        }
        if (columns.length() == 0) {
            throw new TinyJdbcException("SqlAssembler buildUpdateByIdSql updateColumns can not null!");
        }
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        sql.append("UPDATE ")
                .append(tableName)
                .append(" SET ")
                .append(tableColumn)
                .append(" WHERE ")
                .append(whereColumns)
                .append("=?");

        parameters.add(whereValues);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建更新SQL
     *
     * @param clazz    实体对象类型
     * @param criteria 条件构造器
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider buildUpdateByCriteriaSql(UpdateCriteria<T> criteria, Class<?> clazz) {
        String whereSql = criteria.whereSql();
        String updateSql = criteria.updateSql();
        if (StrUtils.isEmpty(whereSql) || !whereSql.contains("WHERE")) {
            throw new TinyJdbcException("The parameter criteria can not null or empty!");
        }
        if (StrUtils.isEmpty(updateSql)) {
            throw new TinyJdbcException("The parameter criteria can not null or empty!");
        }
        String tableName = TableParserUtils.getTableName(clazz);
        String sql = "UPDATE " + tableName + " SET " + updateSql + whereSql;
        SqlProvider so = new SqlProvider();
        so.setSql(sql);
        so.setParameters(criteria.getParameters());
        return so;
    }

    /**
     * 构建更新SQL
     *
     * @param clazz    实体对象类型
     * @param criteria 条件构造器
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider buildUpdateByLambdaCriteriaSql(LambdaUpdateCriteria<T> criteria, Class<?> clazz) {
        String whereSql = criteria.whereSql();
        String updateSql = criteria.updateSql();
        if (StrUtils.isEmpty(whereSql) || !whereSql.contains("WHERE")) {
            throw new TinyJdbcException("The parameter criteria can not null or empty!");
        }
        if (StrUtils.isEmpty(updateSql)) {
            throw new TinyJdbcException("The parameter criteria can not null or empty!");
        }
        String tableName = TableParserUtils.getTableName(clazz);
        String sql = "UPDATE " + tableName + " SET " + updateSql + whereSql;
        SqlProvider so = new SqlProvider();
        so.setSql(sql);
        so.setParameters(criteria.getParameters());
        return so;
    }

    /**
     * 构建删除SQL
     *
     * @param object 入参
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider buildDeleteSql(Object object) {
        Field[] fields = TableParserUtils.resolveFields(object);
        String tableName = TableParserUtils.getTableName(object);
        TableInfo tableInfo = TableParserUtils.getTableInfo(object.getClass());

        StringBuilder sql = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            String column;
            if (columnAnnotation != null && !columnAnnotation.exist()) {
                continue;
            }
            column = tableInfo.getColumn(field.getName());
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            if (filedValue == null) {
                continue;
            }
            whereColumns.append("AND ").append(column).append("=? ");
            parameters.add(filedValue);
        }
        if (StrUtils.isEmpty(whereColumns.toString())) {
            throw new TinyJdbcException("SqlAssembler buildDeleteSql whereColumns can not null!");
        }
        sql.append("DELETE FROM ");
        sql.append(tableName);
        sql.append(" WHERE ");
        sql.append(whereColumns.toString().replaceFirst("AND", ""));

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建删除SQL（根据条件构造器删除）
     *
     * @param criteria 条件构造器
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider buildDeleteCriteriaSql(UpdateCriteria<T> criteria, Class<?> clazz) {
        String criteriaSql = criteria.whereSql();
        if (StrUtils.isEmpty(criteriaSql) || !criteriaSql.contains("WHERE")) {
            throw new TinyJdbcException("The parameter criteria can not null or empty!");
        }
        List<Object> parameters = criteria.getParameters();
        String tableName = TableParserUtils.getTableName(clazz);

        SqlProvider so = new SqlProvider();
        so.setSql("DELETE FROM " + tableName + criteriaSql);
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建删除SQL（根据条件构造器删除）
     *
     * @param criteria 条件构造器Lambda
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider buildDeleteLambdaCriteriaSql(LambdaUpdateCriteria<T> criteria, Class<?> clazz) {
        String criteriaSql = criteria.whereSql();
        if (StrUtils.isEmpty(criteriaSql) || !criteriaSql.contains("WHERE")) {
            throw new TinyJdbcException("The parameter criteria can not null or empty!");
        }
        List<Object> parameters = criteria.getParameters();
        String tableName = TableParserUtils.getTableName(clazz);

        SqlProvider so = new SqlProvider();
        so.setSql("DELETE FROM " + tableName + criteriaSql);
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建TRUNCATE SQL
     *
     * @param clazz Entity类型
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider buildTruncateSql(Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        SqlProvider so = new SqlProvider();
        so.setSql("TRUNCATE TABLE " + tableName);
        return so;
    }

    /**
     * 构建查询SQL
     *
     * @param object 入参Entity，查询参数也是从这个类里获取
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider buildSelectSql(Object object) {
        String tableName = TableParserUtils.getTableName(object);
        TableInfo tableInfo = TableParserUtils.getTableInfo(object.getClass());
        Field[] fields = TableParserUtils.resolveFields(object);

        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();

        List<Object> parameters = new ArrayList<>();
        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            String column;
            if (columnAnnotation != null && !columnAnnotation.exist()) {
                continue;
            }
            column = tableInfo.getColumn(field.getName());
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            if (filedValue != null) {
                whereColumns.append("AND ").append(column).append("=? ");
                parameters.add(filedValue);
            }
            columns.append(column).append(",");
        }
        // 全部字段都被 exist=false 或忽略时，无法构建可查询列
        if (columns.length() == 0) {
            throw new TinyJdbcException("No valid columns to select! All fields are marked as exist=false or ignored.");
        }
        // 截去columns的最后一个字符
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableName);
        if (StrUtils.isNotEmpty(whereColumns.toString())) {
            sql.append(" WHERE ").append(whereColumns.toString().replaceFirst("AND", ""));
        }
        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建查询SQL（根据id查询）
     *
     * @param id    入参
     * @param clazz 实体类Entity.class
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider buildSelectByIdSql(Object id, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        TableInfo tableInfo = TableParserUtils.getTableInfo(clazz);
        String primaryKeyColumn = requirePrimaryKeyColumn(tableInfo);
        String tableColumn = String.join(",", tableInfo.getColumns());
        List<Object> parameters = new ArrayList<>();
        parameters.add(id);
        SqlProvider so = new SqlProvider();
        so.setSql("SELECT " + tableColumn + " FROM " + tableName + " WHERE " + primaryKeyColumn + "=?");
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建查询SQL（根据id列表查询）
     *
     * @param clazz 实体类Entity.class
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider buildSelectByIdsSql(Class<?> clazz, List<Object> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new TinyJdbcException("selectByIds ids cannot be null or empty");
        }
        String tableName = TableParserUtils.getTableName(clazz);
        TableInfo tableInfo = TableParserUtils.getTableInfo(clazz);
        String primaryKeyColumn = requirePrimaryKeyColumn(tableInfo);
        String tableColumn = String.join(",", tableInfo.getColumns());

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableName)
                .append(" WHERE ").append(primaryKeyColumn).append(" IN ");
        // 构建 IN 查询的 SQL 语句
        String placeholders = IntStream.range(0, ids.size()).mapToObj(i -> "?").collect(Collectors.joining(",", "(", ")"));
        sql.append(placeholders);

        // 防御性拷贝：避免调用方后续修改原列表导致占位符与参数数量不匹配
        List<Object> parameters = new ArrayList<>(ids);
        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建删除SQL（根据id删除）
     *
     * @param id 入参
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider buildDeleteByIdSql(Object id, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        TableInfo tableInfo = TableParserUtils.getTableInfo(clazz);
        String primaryKeyColumn = requirePrimaryKeyColumn(tableInfo);

        List<Object> parameters = new ArrayList<>();
        parameters.add(id);
        SqlProvider so = new SqlProvider();
        so.setSql("DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + "=?");
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建删除SQL（根据id批量删除）
     *
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider buildDeleteByIdsSql(Class<?> clazz, List<Object> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new TinyJdbcException("deleteByIds ids cannot be null or empty");
        }
        String tableName = TableParserUtils.getTableName(clazz);
        TableInfo tableInfo = TableParserUtils.getTableInfo(clazz);
        String primaryKeyColumn = requirePrimaryKeyColumn(tableInfo);
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tableName).append(" WHERE ").append(primaryKeyColumn).append(" IN ");
        // 构建 IN 查询的 SQL 语句
        String placeholders = IntStream.range(0, ids.size()).mapToObj(i -> "?").collect(Collectors.joining(",", "(", ")"));
        sql.append(placeholders);
        // 防御性拷贝：避免调用方后续修改原列表导致占位符与参数数量不匹配
        List<Object> parameters = new ArrayList<>(ids);
        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建查询SQL（根据条件构造器查询）
     *
     * @param criteria 条件构造器
     * @param clazz    实体类Entity.class
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider buildSelectCriteriaSql(QueryCriteria<T> criteria, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        String tableColumn = criteria.selectSql();
        if (StrUtils.isEmpty(tableColumn)) {
            TableInfo tableInfo = TableParserUtils.getTableInfo(clazz);
            requirePrimaryKeyColumn(tableInfo);
            tableColumn = String.join(",", tableInfo.getColumns());
        }
        String whereSql = criteria.whereSql();
        List<Object> parameters = criteria.getParameters();

        SqlProvider so = new SqlProvider();
        so.setSql("SELECT " + tableColumn + " FROM " + tableName + whereSql);
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建查询SQL（根据条件构造器查询）
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @param clazz          实体类Entity.class
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider buildSelectLambdaCriteriaSql(LambdaQueryCriteria<T> lambdaCriteria, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        String tableColumn = lambdaCriteria.selectSql();
        if (StrUtils.isEmpty(tableColumn)) {
            TableInfo tableInfo = TableParserUtils.getTableInfo(clazz);
            requirePrimaryKeyColumn(tableInfo);
            tableColumn = String.join(",", tableInfo.getColumns());
        }

        String whereSql = lambdaCriteria.whereSql();
        List<Object> parameters = lambdaCriteria.getParameters();

        SqlProvider so = new SqlProvider();
        so.setSql("SELECT " + tableColumn + " FROM " + tableName + whereSql);
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建查询数量SQL（根据条件构造器）
     *
     * @param criteria 条件构造器
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider buildSelectCountCriteriaSql(QueryCriteria<T> criteria, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        SqlProvider so = new SqlProvider();
        so.setSql("SELECT COUNT(*) FROM " + tableName + criteria.whereConditions());
        so.setParameters(criteria.getParameters());
        return so;
    }

    /**
     * 构建查询数量SQL（根据条件构造器lambda）
     *
     * @param lambdaCriteria 条件构造器lambda
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider buildSelectCountLambdaCriteriaSql(LambdaQueryCriteria<T> lambdaCriteria, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        SqlProvider so = new SqlProvider();
        so.setSql("SELECT COUNT(*) FROM " + tableName + lambdaCriteria.whereConditions());
        so.setParameters(lambdaCriteria.getParameters());
        return so;
    }

    /**
     * 读取实体主键列，若实体未声明 {@code @Id} 则抛出明确异常（与历史 getTableColumn 行为一致），
     * 避免将 null 主键列拼进 SQL 导致 "WHERE null=?"。
     *
     * @param tableInfo 表结构元信息
     * @return 主键列名
     */
    private static String requirePrimaryKeyColumn(TableInfo tableInfo) {
        String primaryKeyColumn = tableInfo.getPrimaryKeyColumn();
        if (StrUtils.isEmpty(primaryKeyColumn)) {
            throw new TinyJdbcException("Please correctly set the primary key attribute column!");
        }
        return primaryKeyColumn;
    }
}

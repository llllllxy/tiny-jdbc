package org.tinycloud.jdbc.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ObjectUtils;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.config.GlobalConfig;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.IdUtils;
import org.tinycloud.jdbc.util.ConvertUtils;
import org.tinycloud.jdbc.util.ReflectUtils;
import org.tinycloud.jdbc.util.StrUtils;
import org.tinycloud.jdbc.util.TableParserUtils;
import org.tinycloud.jdbc.util.tuple.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * sql生成器，通过传入的对象，将对象转为要执行的SQL，要绑定到SQL的参数
 *
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
public class SqlGenerator {

    /**
     * 构建插入SQL
     *
     * @param object 入参
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider insertSql(Object object, boolean ignoreNulls, JdbcTemplate jdbcTemplate) {
        Field[] fields = TableParserUtils.resolveFields(object);
        String tableName = TableParserUtils.getTableName(object);

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

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
            if (columnAnnotation != null && StrUtils.isNotEmpty(columnAnnotation.value())) {
                column = columnAnnotation.value();
            } else {
                column = StrUtils.camelToUnderline(fieldName);
            }
            Object fieldValue;
            try {
                fieldValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            // 如果是主键列
            if (idAnnotation != null) {
                // 处理主键生成/赋值，返回最终的主键值（可能是自动生成的）
                fieldValue = processPrimaryKey(field, fieldValue, fieldName, fieldType, idAnnotation, object, jdbcTemplate);
                // 为自增主键时，返回 null，此时跳过该字段（无需加入 SQL）
                if (fieldValue == null) {
                    continue;
                }
            }

            // 判断是否忽略null
            if (ignoreNulls && ObjectUtils.isEmpty(fieldValue)) {
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

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
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
                                            Id idAnnotation, Object object, JdbcTemplate jdbcTemplate) {
        // 只有用户没有自己设置主键值时，才需要走自动生成的策略
        if (ObjectUtils.isEmpty(fieldValue)) {
            IdType idType = idAnnotation.idType();
            if (idType == IdType.AUTO_INCREMENT) {
                // 自增主键：返回 null，外层逻辑会跳过该字段
                return null;
            }
            // 如果是其他主键策略，设置完主键后，塞回到实体类里，这样可以方便插入后获取主键值
            else if (idType == IdType.OBJECT_ID) {
                if (fieldType != String.class) {
                    throw new TinyJdbcException("The type of " + fieldName + " field  must be String when objectId!");
                }
                fieldValue = IdUtils.objectId();
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new TinyJdbcException("inject field value fail : " + fieldName + " field type must be String when objectId!", e);
                }
            } else if (idType == IdType.ASSIGN_ID) {
                if (fieldType != String.class && fieldType != Long.class) {
                    throw new TinyJdbcException("The type of " + fieldName + ", field  must be String or Long when assignId!");
                }
                fieldValue = (fieldType == String.class) ? IdUtils.nextId() : IdUtils.nextLongId();
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new TinyJdbcException("inject field value fail : " + fieldName + ", field type must be String or Long when assignId!", e);
                }
            } else if (idType == IdType.UUID) {
                if (fieldType != String.class) {
                    throw new TinyJdbcException("The type of " + fieldName + " field must be String when uuid!");
                }
                fieldValue = IdUtils.simpleUUID();
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new TinyJdbcException("inject field value fail : " + fieldName + ", field type must be String when uuid!", e);
                }
            } else if (idType == IdType.SEQUENCE) {
                if (!Number.class.isAssignableFrom(fieldType)) {
                    throw new TinyJdbcException("The type of " + fieldName + " field must be assignable from Number when sequence!");
                }
                String sequenceSql = idAnnotation.value();
                // 执行查询操作，并获取序列的下一个值
                fieldValue = jdbcTemplate.queryForObject(sequenceSql, fieldType);
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new TinyJdbcException("inject field value fail : " + fieldName + ", field type must be assignable from Number when sequence!", e);
                }
            } else if (idType == IdType.CUSTOM) {
                IdGeneratorInterface idGeneratorInterface = GlobalConfig.getConfig().getIdGeneratorInterface();
                Object id = idGeneratorInterface.nextId(object);
                try {
                    fieldValue = ConvertUtils.convert(id, fieldType);
                } catch (Exception e) {
                    throw new TinyJdbcException("The fieldType of " + fieldName + " is not supported! Please check if the ID type matches the primary key type.", e);
                }
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new TinyJdbcException("inject field value fail : " + field.getName() + ", please verify if the return data type of idGeneratorInterface.nextId() method matches the data type of the primary key!", e);
                }
            } else {
                throw new TinyJdbcException("Unknown idType: " + idType + "!");
            }
        }
        // 返回最终的主键值（非自增场景）
        return fieldValue;
    }


    /**
     * 构建更新SQL
     *
     * @param object 入参
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider updateByIdSql(Object object, boolean ignoreNulls) {
        Field[] fields = TableParserUtils.resolveFields(object);
        String tableName = TableParserUtils.getTableName(object);

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();
        Object whereValues = new Object();
        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            Id idAnnotation = field.getAnnotation(Id.class);
            String column;
            if (columnAnnotation != null && !columnAnnotation.exist()) {
                continue;
            }
            if (columnAnnotation != null && StrUtils.isNotEmpty(columnAnnotation.value())) {
                column = columnAnnotation.value();
            } else {
                column = StrUtils.camelToUnderline(field.getName());
            }
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            if (idAnnotation != null) {
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
            throw new TinyJdbcException("SqlGenerator updateByIdSql primaryKeyId can not null!");
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
     * @param object      实体对象
     * @param ignoreNulls 是否忽略null
     * @param criteria    条件构造器
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider updateByEntityAndCriteriaSql(Object object, boolean ignoreNulls, UpdateCriteria<T> criteria) {
        String criteriaSql = criteria.whereSql();
        if (StrUtils.isEmpty(criteriaSql) || !criteriaSql.contains("WHERE")) {
            throw new TinyJdbcException("SqlGenerator updateByCriteriaSql criteria can not null or empty!");
        }
        Field[] fields = TableParserUtils.resolveFields(object);
        String tableName = TableParserUtils.getTableName(object);

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        StringBuilder columns = new StringBuilder();

        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            String column;
            if (columnAnnotation != null && !columnAnnotation.exist()) {
                continue;
            }
            if (columnAnnotation == null || StrUtils.isEmpty(columnAnnotation.value())) {
                column = StrUtils.camelToUnderline(field.getName());
            } else {
                column = columnAnnotation.value();
            }
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            // 是否忽略null
            if (ignoreNulls && filedValue == null) {
                continue;
            }
            columns.append(column).append("=?,");
            parameters.add(filedValue);
        }
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        sql.append("UPDATE ").append(tableName).append(" SET ").append(tableColumn);
        sql.append(criteriaSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        parameters.addAll(criteria.getParameters());
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建更新SQL
     *
     * @param object      实体对象
     * @param ignoreNulls 是否忽略null
     * @param criteria    条件构造器Lambda
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider updateByEntityAndLambdaCriteriaSql(Object object, boolean ignoreNulls, LambdaUpdateCriteria<T> criteria) {
        String criteriaSql = criteria.whereSql();
        if (StrUtils.isEmpty(criteriaSql) || !criteriaSql.contains("WHERE")) {
            throw new TinyJdbcException("SqlGenerator updateByLambdaCriteriaSql criteria can not null or empty!");
        }
        Field[] fields = TableParserUtils.resolveFields(object);
        String tableName = TableParserUtils.getTableName(object);

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        StringBuilder columns = new StringBuilder();

        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            String column;
            if (columnAnnotation != null && !columnAnnotation.exist()) {
                continue;
            }
            if (columnAnnotation == null || StrUtils.isEmpty(columnAnnotation.value())) {
                column = StrUtils.camelToUnderline(field.getName());
            } else {
                column = columnAnnotation.value();
            }
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            // 是否忽略null
            if (ignoreNulls && filedValue == null) {
                continue;
            }
            columns.append(column).append("=?,");
            parameters.add(filedValue);
        }

        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        sql.append("UPDATE ").append(tableName).append(" SET ").append(tableColumn);
        sql.append(criteriaSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        parameters.addAll(criteria.getParameters());
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
    public static <T> SqlProvider updateByCriteriaSql(UpdateCriteria<T> criteria, Class<?> clazz) {
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
    public static <T> SqlProvider updateByLambdaCriteriaSql(LambdaUpdateCriteria<T> criteria, Class<?> clazz) {
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
    public static SqlProvider deleteSql(Object object) {
        Field[] fields = TableParserUtils.resolveFields(object);
        String tableName = TableParserUtils.getTableName(object);

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
            if (columnAnnotation == null || StrUtils.isEmpty(columnAnnotation.value())) {
                column = StrUtils.camelToUnderline(field.getName());
            } else {
                column = columnAnnotation.value();
            }
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
            throw new TinyJdbcException("SqlGenerator deleteSql whereColumns can not null!");
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
    public static <T> SqlProvider deleteCriteriaSql(UpdateCriteria<T> criteria, Class<?> clazz) {
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
    public static <T> SqlProvider deleteLambdaCriteriaSql(LambdaUpdateCriteria<T> criteria, Class<?> clazz) {
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
    public static SqlProvider truncateSql(Class<?> clazz) {
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
    public static SqlProvider selectSql(Object object) {
        String tableName = TableParserUtils.getTableName(object);
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
            if (columnAnnotation == null || StrUtils.isEmpty(columnAnnotation.value())) {
                column = StrUtils.camelToUnderline(field.getName());
            } else {
                column = columnAnnotation.value();
            }
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
    public static SqlProvider selectByIdSql(Object id, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        Pair<List<String>, String> pair = TableParserUtils.getTableColumn(clazz);
        String primaryKeyColumn = pair.getRight();
        List<String> columnList = pair.getLeft();
        String tableColumn = String.join(",", columnList);
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
    public static SqlProvider selectByIdsSql(Class<?> clazz, List<Object> ids) {
        String tableName = TableParserUtils.getTableName(clazz);
        Pair<List<String>, String> pair = TableParserUtils.getTableColumn(clazz);
        String primaryKeyColumn = pair.getRight();
        List<String> columnList = pair.getLeft();
        String tableColumn = String.join(",", columnList);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableName)
                .append(" WHERE ").append(primaryKeyColumn).append(" IN ");
        // 构建 IN 查询的 SQL 语句
        String placeholders = IntStream.range(0, ids.size()).mapToObj(i -> "?").collect(Collectors.joining(",", "(", ")"));
        sql.append(placeholders);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(ids);
        return so;
    }


    /**
     * 构建删除SQL（根据id删除）
     *
     * @param id 入参
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider deleteByIdSql(Object id, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        Pair<List<String>, String> pair = TableParserUtils.getTableColumn(clazz);
        String primaryKeyColumn = pair.getRight();

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
    public static SqlProvider deleteByIdsSql(Class<?> clazz, List<Object> ids) {
        String tableName = TableParserUtils.getTableName(clazz);
        Pair<List<String>, String> pair = TableParserUtils.getTableColumn(clazz);
        String primaryKeyColumn = pair.getRight();
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tableName).append(" WHERE ").append(primaryKeyColumn).append(" IN ");
        // 构建 IN 查询的 SQL 语句
        String placeholders = IntStream.range(0, ids.size()).mapToObj(i -> "?").collect(Collectors.joining(",", "(", ")"));
        sql.append(placeholders);
        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(ids);
        return so;
    }

    /**
     * 构建查询SQL（根据条件构造器查询）
     *
     * @param criteria 条件构造器
     * @param clazz    实体类Entity.class
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider selectCriteriaSql(QueryCriteria<T> criteria, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        String tableColumn = criteria.selectSql();
        if (StrUtils.isEmpty(tableColumn)) {
            Pair<List<String>, String> pair = TableParserUtils.getTableColumn(clazz);
            List<String> columnList = pair.getLeft();
            tableColumn = String.join(",", columnList);
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
    public static <T> SqlProvider selectLambdaCriteriaSql(LambdaQueryCriteria<T> lambdaCriteria, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        String tableColumn = lambdaCriteria.selectSql();
        if (StrUtils.isEmpty(tableColumn)) {
            Pair<List<String>, String> pair = TableParserUtils.getTableColumn(clazz);
            List<String> columnList = pair.getLeft();
            tableColumn = String.join(",", columnList);
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
    public static <T> SqlProvider selectCountCriteriaSql(QueryCriteria<T> criteria, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        SqlProvider so = new SqlProvider();
        so.setSql("SELECT COUNT(*) FROM " + tableName + criteria.whereSql());
        so.setParameters(criteria.getParameters());
        return so;
    }


    /**
     * 构建查询数量SQL（根据条件构造器lambda）
     *
     * @param lambdaCriteria 条件构造器lambda
     * @return 组装完毕的SqlProvider
     */
    public static <T> SqlProvider selectCountLambdaCriteriaSql(LambdaQueryCriteria<T> lambdaCriteria, Class<?> clazz) {
        String tableName = TableParserUtils.getTableName(clazz);
        SqlProvider so = new SqlProvider();
        so.setSql("SELECT COUNT(*) FROM " + tableName + lambdaCriteria.whereSql());
        so.setParameters(lambdaCriteria.getParameters());
        return so;
    }
}

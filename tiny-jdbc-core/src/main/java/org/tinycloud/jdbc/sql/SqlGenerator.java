package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.config.GlobalConfigUtils;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.IdUtils;
import org.tinycloud.jdbc.util.Pair;
import org.tinycloud.jdbc.util.ReflectUtils;
import org.tinycloud.jdbc.util.StrUtils;
import org.tinycloud.jdbc.util.TableParserUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


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
    public static SqlProvider insertSql(Object object, boolean ignoreNulls) {
        Field[] fields = TableParserUtils.resolveFields(object);
        String tableName = TableParserUtils.getTableName(object);

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            boolean primaryKey = columnAnnotation.primaryKey();
            if (primaryKey) {
                IdType idType = columnAnnotation.idType();
                if (idType == IdType.AUTO_INCREMENT) {
                    // 自增主键直接跳过，无需处理
                    continue;
                }
                // 如果是其他主键策略，设置完主键后，塞回到实体类里，这样可以方便插入后获取主键值
                if (idType == IdType.OBJECT_ID) {
                    Object fieldValue = IdUtils.objectId();
                    try {
                        field.set(object, fieldValue);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new TinyJdbcException("inject field value fail : " + field.getName() + ", field type must be String when objectId!", e);
                    }
                }
                if (idType == IdType.ASSIGN_ID) {
                    Class<?> type = field.getType();
                    Object fieldValue = (type == String.class) ? IdUtils.nextId() : IdUtils.nextLongId();
                    try {
                        field.set(object, fieldValue);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new TinyJdbcException("inject field value fail : " + field.getName() + ", field type must be String or Long when assignId!", e);
                    }
                }
                if (idType == IdType.UUID) {
                    Object fieldValue = IdUtils.simpleUUID();
                    try {
                        field.set(object, fieldValue);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new TinyJdbcException("inject field value fail : " + field.getName() + ", field type must be String when uuid!", e);
                    }
                }
                if (idType == IdType.CUSTOM) {
                    IdGeneratorInterface idGeneratorInterface = GlobalConfigUtils.getGlobalConfig().getIdGeneratorInterface();
                    Class<?> keyType = field.getType();
                    Object fieldValue;
                    Object id = idGeneratorInterface.nextId(object);
                    if (keyType == id.getClass()) {
                        fieldValue = id;
                    } else if (Integer.class == keyType) {
                        fieldValue = Integer.parseInt(id.toString());
                    } else if (Long.class == keyType) {
                        fieldValue = Long.parseLong(id.toString());
                    } else if (BigDecimal.class == keyType) {
                        fieldValue = new BigDecimal(id.toString());
                    } else if (BigInteger.class == keyType) {
                        fieldValue = new BigInteger(id.toString());
                    } else {
                        fieldValue = id;
                    }
                    try {
                        field.set(object, fieldValue);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new TinyJdbcException("inject field value fail : " + field.getName() + ", please verify if the return data type of idGeneratorInterface.nextId() method matches the data type of the primary key!", e);
                    }
                }
            }
            Object fieldValue = null;
            try {
                fieldValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            // 是否忽略null
            if (ignoreNulls && fieldValue == null) {
                continue;
            }

            columns.append(column).append(",");
            values.append("?").append(",");
            parameters.add(fieldValue);
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
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StrUtils.isEmpty(column)) {
                continue;
            }
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            boolean primaryKey = columnAnnotation.primaryKey();
            if (primaryKey) {
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
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StrUtils.isEmpty(column)) {
                continue;
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
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StrUtils.isEmpty(column)) {
                continue;
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
            throw new TinyJdbcException("SqlGenerator updateByCriteriaSql criteria can not null or empty!");
        }
        if (StrUtils.isEmpty(updateSql)) {
            throw new TinyJdbcException("SqlGenerator updateByCriteriaSql criteria can not null or empty!");
        }

        String tableName = TableParserUtils.getTableName(clazz);
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ").append(updateSql);
        sql.append(whereSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
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
            throw new TinyJdbcException("SqlGenerator updateByCriteriaSql criteria can not null or empty!");
        }
        if (StrUtils.isEmpty(updateSql)) {
            throw new TinyJdbcException("SqlGenerator updateByCriteriaSql criteria can not null or empty!");
        }

        String tableName = TableParserUtils.getTableName(clazz);
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ").append(updateSql);
        sql.append(whereSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
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
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StrUtils.isEmpty(column)) {
                continue;
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
            throw new TinyJdbcException("SqlGenerator deleteCriteriaSql criteria can not null or empty!");
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
            throw new TinyJdbcException("SqlGenerator deleteLambdaCriteriaSql criteria can not null or empty!");
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
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StrUtils.isEmpty(column)) {
                continue;
            }
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new TinyJdbcException("get field value failed: " + field.getName(), e);
            }
            if (filedValue != null) {
                whereColumns.append("AND ")
                        .append(column)
                        .append("=? ");
                parameters.add(filedValue);
            }
            columns.append(column)
                    .append(",");
        }
        // 截去columns的最后一个字符
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append(tableColumn)
                .append(" FROM ")
                .append(tableName);
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
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableName)
                .append(" WHERE ")
                .append(primaryKeyColumn)
                .append("=?");
        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
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
                .append(" WHERE ")
                .append(primaryKeyColumn)
                .append(" IN ");
        // 构建 IN 查询的 SQL 语句
        StringJoiner placeholders = new StringJoiner(",", "(", ")");
        for (int i = 0; i < ids.size(); i++) {
            placeholders.add("?");
        }
        sql.append(placeholders.toString());

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
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ")
                .append(tableName)
                .append(" WHERE ")
                .append(primaryKeyColumn)
                .append("=?");

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
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
        sql.append("DELETE FROM ")
                .append(tableName)
                .append(" WHERE ")
                .append(primaryKeyColumn)
                .append(" IN ");
        // 构建 IN 查询的 SQL 语句
        StringJoiner placeholders = new StringJoiner(",", "(", ")");
        for (int i = 0; i < ids.size(); i++) {
            placeholders.add("?");
        }
        sql.append(placeholders.toString());

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

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableName)
                .append(whereSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
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
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableName).append(whereSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
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
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(tableName).append(criteria.whereSql());
        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
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
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(tableName).append(lambdaCriteria.whereSql());
        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(lambdaCriteria.getParameters());
        return so;
    }
}

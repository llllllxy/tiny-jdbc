package org.tinycloud.jdbc.sql;

import org.springframework.util.StringUtils;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.criteria.Criteria;
import org.tinycloud.jdbc.criteria.LambdaCriteria;
import org.tinycloud.jdbc.exception.JdbcException;
import org.tinycloud.jdbc.id.IdUtils;
import org.tinycloud.jdbc.util.ReflectUtils;
import org.tinycloud.jdbc.util.Triple;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


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
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Field[] fields = triple.getSecond();
        Table tableAnnotation = triple.getThird();

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            boolean primaryKey = columnAnnotation.primaryKey();
            boolean autoIncrement = columnAnnotation.autoIncrement();
            // 如果是自增主键，那么直接跳过，无需后面的逻辑了
            if (primaryKey && autoIncrement) {
                continue;
            }

            boolean assignId = columnAnnotation.assignId();
            boolean uuid = columnAnnotation.uuid();
            boolean objectId = columnAnnotation.objectId();

            // 如果是其他主键策略，设置完主键后，塞回到实体类里，这样可以方便插入后获取主键值
            /* 雪花ID */
            if (primaryKey && assignId) {
                Class<?> type = field.getType();
                Object fieldValue = (type == java.lang.String.class) ? IdUtils.nextId() : IdUtils.nextLongId();
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException("inject field value fail : " + field.getName() + ", field type must be String or Long when assignId!");
                }
            }
            /* UUID */
            if (primaryKey && uuid) {
                Object fieldValue = IdUtils.simpleUUID();
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException("inject field value fail : " + field.getName() + ", field type must be String when uuid!");
                }
            }
            /* objectId */
            if (primaryKey && objectId) {
                Object fieldValue = IdUtils.objectId();
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException("inject field value fail : " + field.getName() + ", field type must be String when objectId!");
                }
            }

            Object fieldValue = null;
            try {
                fieldValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
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
        sql.append("INSERT INTO ").append(tableAnnotation.value());
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
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Field[] fields = triple.getSecond();
        Table tableAnnotation = triple.getThird();

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
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
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
            throw new JdbcException("SqlGenerator updateByIdSql primaryKeyId can not null!");
        }
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        sql.append("UPDATE ").append(tableAnnotation.value()).append(" SET ").append(tableColumn);
        sql.append(" WHERE ");
        sql.append(whereColumns);
        sql.append("=?");

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
    public static SqlProvider updateByCriteriaSql(Object object, boolean ignoreNulls, Criteria criteria) {
        String criteriaSql = criteria.generateSql();
        if (StringUtils.isEmpty(criteriaSql) || !criteriaSql.contains("WHERE")) {
            throw new JdbcException("SqlGenerator updateByCriteriaSql criteria can not null or empty!");
        }

        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Field[] fields = triple.getSecond();
        Table tableAnnotation = triple.getThird();

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
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
            // 是否忽略null
            if (ignoreNulls && filedValue == null) {
                continue;
            }
            columns.append(column).append("=?,");
            parameters.add(filedValue);
        }

        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        sql.append("UPDATE ").append(tableAnnotation.value()).append(" SET ").append(tableColumn);
        sql.append(criteriaSql);

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
     * @param criteria    条件构造器Lambda
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider updateByLambdaCriteriaSql(Object object, boolean ignoreNulls, LambdaCriteria criteria) {
        String criteriaSql = criteria.generateSql();
        if (StringUtils.isEmpty(criteriaSql) || !criteriaSql.contains("WHERE")) {
            throw new JdbcException("SqlGenerator updateByLambdaCriteriaSql criteria can not null or empty!");
        }

        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Field[] fields = triple.getSecond();
        Table tableAnnotation = triple.getThird();

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
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
            // 是否忽略null
            if (ignoreNulls && filedValue == null) {
                continue;
            }
            columns.append(column).append("=?,");
            parameters.add(filedValue);
        }

        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        sql.append("UPDATE ").append(tableAnnotation.value()).append(" SET ").append(tableColumn);
        sql.append(criteriaSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
    }

    /**
     * 构建删除SQL
     *
     * @param object 入参
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider deleteSql(Object object) {
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Field[] fields = triple.getSecond();
        Table tableAnnotation = triple.getThird();

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
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
            if (filedValue == null) {
                continue;
            }
            whereColumns.append("AND ").append(column).append("=? ");
            parameters.add(filedValue);
        }
        if (StringUtils.isEmpty(whereColumns.toString())) {
            throw new JdbcException("SqlGenerator deleteSql whereColumns can not null!");
        }
        sql.append("DELETE FROM ");
        sql.append(tableAnnotation.value());
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
    public static SqlProvider deleteCriteriaSql(Criteria criteria, Class<?> clazz) {
        String criteriaSql = criteria.generateSql();
        if (StringUtils.isEmpty(criteriaSql) || !criteriaSql.contains("WHERE")) {
            throw new JdbcException("SqlGenerator deleteCriteriaSql criteria can not null or empty!");
        }

        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Table tableAnnotation = triple.getThird();

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tableAnnotation.value()).append(criteriaSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        return so;
    }

    /**
     * 构建删除SQL（根据条件构造器删除）
     *
     * @param criteria 条件构造器Lambda
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider deleteLambdaCriteriaSql(LambdaCriteria criteria, Class<?> clazz) {
        String criteriaSql = criteria.generateSql();
        if (StringUtils.isEmpty(criteriaSql) || !criteriaSql.contains("WHERE")) {
            throw new JdbcException("SqlGenerator deleteLambdaCriteriaSql criteria can not null or empty!");
        }

        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Table tableAnnotation = triple.getThird();

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tableAnnotation.value()).append(criteriaSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        return so;
    }

    /**
     * 构建查询SQL
     *
     * @param object 入参Entity，查询参数也是从这个类里获取
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider selectSql(Object object) {
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Field[] fields = triple.getSecond();
        Table tableAnnotation = triple.getThird();

        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();
        String primaryKeyColumn = "";

        List<Object> parameters = new ArrayList<>();
        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            boolean primaryKey = columnAnnotation.primaryKey();
            if (primaryKey) {
                primaryKeyColumn = column;
            }

            Object filedValue = null;
            try {
                filedValue = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }

            Class<?> type = field.getType();
            if (ReflectUtils.typeValueIsNotNull(type, filedValue)) {
                whereColumns.append("AND ")
                        .append(column)
                        .append("=? ");
                parameters.add(filedValue);
            }
            columns.append(column)
                    .append(" AS ")
                    .append(field.getName())
                    .append(",");
        }
        // 截去columns的最后一个字符
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append(tableColumn)
                .append(" FROM ")
                .append(tableAnnotation.value());
        if (!StringUtils.isEmpty(whereColumns.toString())) {
            sql.append(" WHERE ").append(whereColumns.toString().replaceFirst("AND", ""));
        }
        if (!StringUtils.isEmpty(primaryKeyColumn)) {
            sql.append(" ORDER BY ").append(primaryKeyColumn).append(" DESC");
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
        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);

        Table tableAnnotation = triple.getThird();
        Field[] fields = ReflectUtils.getFields(clazz);
        List<Object> parameters = new ArrayList<>();
        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();

        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String fieldName = field.getName();
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            boolean primaryKey = columnAnnotation.primaryKey();
            if (primaryKey) {
                whereColumns.append(column);
            }
            columns.append(column)
                    .append(" AS ")
                    .append(fieldName)
                    .append(",");
        }

        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        parameters.add(id);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableAnnotation.value())
                .append(" WHERE ")
                .append(whereColumns)
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
    public static SqlProvider selectByIdsSql(Class<?> clazz) {
        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);

        Table tableAnnotation = triple.getThird();
        Field[] fields = ReflectUtils.getFields(clazz);
        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();

        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String fieldName = field.getName();
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            boolean primaryKey = columnAnnotation.primaryKey();
            if (primaryKey) {
                whereColumns.append(column);
            }
            columns.append(column)
                    .append(" AS ")
                    .append(fieldName)
                    .append(",");
        }
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableAnnotation.value())
                .append(" WHERE ")
                .append(whereColumns)
                .append(" IN (:idList)");

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        return so;
    }


    /**
     * 构建删除SQL（根据id删除）
     *
     * @param id 入参
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider deleteByIdSql(Object id, Class<?> clazz) {
        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Table tableAnnotation = triple.getThird();
        Field[] fields = triple.getSecond();

        List<Object> parameters = new ArrayList<>();
        StringBuilder whereColumns = new StringBuilder();

        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            boolean primaryKey = columnAnnotation.primaryKey();
            if (primaryKey) {
                whereColumns.append(column);
            }
        }
        parameters.add(id);

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ")
                .append(tableAnnotation.value())
                .append(" WHERE ")
                .append(whereColumns)
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
    public static SqlProvider deleteByIdsSql(Class<?> clazz) {
        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Field[] fields = triple.getSecond();
        Table tableAnnotation = triple.getThird();

        StringBuilder whereColumns = new StringBuilder();

        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            boolean primaryKey = columnAnnotation.primaryKey();
            if (primaryKey) {
                whereColumns.append(column);
                break;
            }
        }
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ")
                .append(tableAnnotation.value())
                .append(" WHERE ")
                .append(whereColumns)
                .append(" IN (:idList)");
        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        return so;
    }

    /**
     * 构建查询SQL（根据条件构造器查询）
     *
     * @param criteria 条件构造器
     * @param clazz    实体类Entity.class
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider selectCriteriaSql(Criteria criteria, Class<?> clazz) {
        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Field[] fields = triple.getSecond();
        Table tableAnnotation = triple.getThird();

        StringBuilder columns = new StringBuilder();

        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String fieldName = field.getName();
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            columns.append(column)
                    .append(" AS ")
                    .append(fieldName)
                    .append(",");
        }
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        String criteriaSql = criteria.generateSql();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableAnnotation.value())
                .append(criteriaSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        return so;
    }

    /**
     * 构建查询SQL（根据条件构造器查询）
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @param clazz          实体类Entity.class
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider selectLambdaCriteriaSql(LambdaCriteria lambdaCriteria, Class<?> clazz) {
        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Field[] fields = triple.getSecond();
        Table tableAnnotation = triple.getThird();

        StringBuilder columns = new StringBuilder();

        for (Field field : fields) {
            ReflectUtils.makeAccessible(field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String fieldName = field.getName();
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            columns.append(column)
                    .append(" AS ")
                    .append(fieldName)
                    .append(",");
        }
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        String criteriaSql = lambdaCriteria.generateSql();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableColumn).append(" FROM ").append(tableAnnotation.value())
                .append(criteriaSql);

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        return so;
    }


    /**
     * 构建查询数量SQL（根据条件构造器）
     *
     * @param criteria 条件构造器
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider selectCountCriteriaSql(Criteria criteria, Class<?> clazz) {
        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Table tableAnnotation = triple.getThird();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(tableAnnotation.value()).append(criteria.generateSql());

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        return so;
    }


    /**
     * 构建查询数量SQL（根据条件构造器lambda）
     *
     * @param lambdaCriteria 条件构造器lambda
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider selectCountLambdaCriteriaSql(LambdaCriteria lambdaCriteria, Class<?> clazz) {
        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        Triple<Class<?>, Field[], Table> triple = ReflectUtils.validateTargetClass(object);
        Table tableAnnotation = triple.getThird();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(tableAnnotation.value()).append(lambdaCriteria.generateSql());

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        return so;
    }
}

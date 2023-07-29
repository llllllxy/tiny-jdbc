package org.tinycloud.jdbc.sql;

import org.springframework.util.StringUtils;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.JdbcException;
import org.tinycloud.jdbc.id.IdUtils;

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
        ReflectUtils.validateTargetClass(object);
        Class<?> clazz = object.getClass();
        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        Field[] fields = ReflectUtils.getFields(clazz);
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (Field field : fields) {
            field.setAccessible(true);
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
            boolean assignUuid = columnAnnotation.assignUuid();
            // 如果是其他主键策略，设置完主键后，塞回到实体类里，这样可以方便插入后获取主键值
            if (primaryKey && assignId) {
                Class<?> type = field.getType();
                Object fieldValue;
                if (type == java.lang.String.class) {
                    fieldValue = IdUtils.nextId();
                } else {
                    fieldValue = IdUtils.nextLongId();
                }
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException("inject field value fail : " + field.getName() + ", field type must be String or Long when assignId!");
                }
            }
            if (primaryKey && assignUuid) {
                Object fieldValue = IdUtils.simpleUUID();
                try {
                    field.set(object, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException("inject field value fail : " + field.getName() + ", field type must be String when assignUuid!");
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
        sql.append("insert into ").append(tableAnnotation.value());
        sql.append(" (").append(tableColumns).append(")");
        sql.append(" values (").append(tableValues).append(")");

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
    public static SqlProvider updateSql(Object object, boolean ignoreNulls) {
        ReflectUtils.validateTargetClass(object);
        Class<?> clazz = object.getClass();
        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        Field[] fields = ReflectUtils.getFields(clazz);
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();
        StringBuilder whereValues = new StringBuilder();
        for (Field field : fields) {
            field.setAccessible(true);
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
                whereValues.append(filedValue);
                continue;
            }
            // 是否忽略null
            if (ignoreNulls && filedValue == null) {
                continue;
            }
            columns.append(column).append("=?,");
            parameters.add(filedValue);
        }

        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        sql.append("update ").append(tableAnnotation.value()).append(" set ").append(tableColumn);
        sql.append(" where ");
        sql.append(whereColumns);
        sql.append("=?");

        parameters.add(whereValues.toString());

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
        ReflectUtils.validateTargetClass(object);
        Class<?> classz = object.getClass();
        Table tableAnnotation = (Table) classz.getAnnotation(Table.class);
        Field[] fields = ReflectUtils.getFields(classz);
        StringBuilder sql = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        for (Field field : fields) {
            field.setAccessible(true);
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
            whereColumns.append("and ").append(column).append("=? ");
            parameters.add(filedValue);
        }
        if (StringUtils.isEmpty(whereColumns.toString())) {
            throw new JdbcException("deleteSql方法不能传入空对象，会导致删除全表！");
        }
        sql.append("delete from ");
        sql.append(tableAnnotation.value());
        sql.append(" where ");
        sql.append(whereColumns.toString().replaceFirst("and", ""));

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
    }


    /**
     * 构建查询SQL
     *
     * @param object 入参Entity，查询参数也是从这个类里获取
     * @return 组装完毕的SqlProvider
     */
    public static SqlProvider selectSql(Object object) {
        ReflectUtils.validateTargetClass(object);
        Class<?> clazz = object.getClass();
        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        Field[] fields = ReflectUtils.getFields(clazz);
        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();
        String primaryKeyColumn = "";

        List<Object> parameters = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
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
                whereColumns.append("and ")
                        .append(column)
                        .append("=? ");
                parameters.add(filedValue);
            }
            columns.append(column)
                    .append(" as ")
                    .append(field.getName())
                    .append(",");
        }
        // 截去columns的最后一个字符
        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();

        StringBuilder sql = new StringBuilder();
        sql.append("select ")
                .append(tableColumn)
                .append(" from ")
                .append(tableAnnotation.value());
        if (!StringUtils.isEmpty(whereColumns.toString())) {
            sql.append(" where ").append(whereColumns.toString().replaceFirst("and", ""));
        }
        if (!StringUtils.isEmpty(primaryKeyColumn)) {
            sql.append(" order by ").append(primaryKeyColumn).append(" desc");
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
        ReflectUtils.validateTargetClass(object);

        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        Field[] fields = ReflectUtils.getFields(clazz);
        List<Object> parameters = new ArrayList<>();
        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();

        for (Field field : fields) {
            field.setAccessible(true);
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
                    .append(" as ")
                    .append(fieldName)
                    .append(",");
        }

        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        parameters.add(String.valueOf(id));

        StringBuilder sql = new StringBuilder();
        sql.append("select ").append(tableColumn).append(" from ").append(tableAnnotation.value())
                .append(" where ")
                .append(whereColumns)
                .append("=?");

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
    public static SqlProvider deleteByIdSql(Object id, Class<?> clazz) {
        Object object = ReflectUtils.createInstance(clazz);
        // 对象检验
        ReflectUtils.validateTargetClass(object);

        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        Field[] fields = ReflectUtils.getFields(clazz);
        List<Object> parameters = new ArrayList<>();
        StringBuilder whereColumns = new StringBuilder();

        for (Field field : fields) {
            field.setAccessible(true);
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
        parameters.add(String.valueOf(id));

        StringBuilder sql = new StringBuilder();
        sql.append("delete from ")
                .append(tableAnnotation.value())
                .append(" where ")
                .append(whereColumns)
                .append("=?");

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
    }

}

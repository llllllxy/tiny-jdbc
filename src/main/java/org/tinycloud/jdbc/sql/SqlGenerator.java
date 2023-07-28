package org.tinycloud.jdbc.sql;

import org.springframework.util.StringUtils;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Table;

import java.lang.reflect.Field;
import java.math.BigDecimal;
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
        SqlUtils.validateTargetClass(object);
        Class<?> clazz = object.getClass();
        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        Field[] fields = SqlUtils.getFields(clazz);
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Column columnAnnotation = fields[i].getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();

            // 数据库自增列为false, 代码插入
            boolean primaryKey = columnAnnotation.primaryKey();
            boolean autoIncrement = columnAnnotation.autoIncrement();
            if (primaryKey && autoIncrement) {
                continue;
            }
            Object filedValue = null;
            try {
                filedValue = fields[i].get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
            // 是否忽略null
            if (ignoreNulls && filedValue == null) {
                continue;
            }

            columns.append(column);
            columns.append(",");
            values.append("?");
            values.append(",");
            parameters.add(filedValue);
        }
        String tableColumns = columns.subSequence(0, columns.length() - 1).toString();
        String tableValues = values.subSequence(0, values.length() - 1).toString();
        sql.append("insert into " + tableAnnotation.value() + " ");
        sql.append(" (" + tableColumns + ") ");
        sql.append(" values(" + tableValues + ")");

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
        SqlUtils.validateTargetClass(object);
        Class<?> classz = object.getClass();
        Table tableAnnotation = (Table) classz.getAnnotation(Table.class);
        Field[] fields = SqlUtils.getFields(classz);
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<Object>();

        StringBuilder columns = new StringBuilder();

        StringBuilder whereColumns = new StringBuilder();
        StringBuilder whereValues = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Column columnAnnotation = fields[i].getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            Object filedValue = null;
            try {
                filedValue = fields[i].get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
            // 是否忽略null
            if (ignoreNulls && filedValue == null) {
                continue;
            }

            boolean primaryKey = columnAnnotation.primaryKey();
            if (primaryKey) {
                whereColumns.append(column);
                whereValues.append(filedValue);
                continue;
            }
            columns.append(column + "=?");
            columns.append(",");
            parameters.add(filedValue);
        }

        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        sql.append("update " + tableAnnotation.value() + " set " + tableColumn + "");
        sql.append(" ");
        sql.append("where");
        sql.append(" ");
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
        SqlUtils.validateTargetClass(object);
        Class<?> classz = object.getClass();
        Table tableAnnotation = (Table) classz.getAnnotation(Table.class);
        Field[] fields = SqlUtils.getFields(classz);
        StringBuilder sql = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();
        List<Object> parameters = new ArrayList<Object>();

        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Column columnAnnotation = fields[i].getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            Object filedValue = null;
            try {
                filedValue = fields[i].get(object);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (filedValue == null) {
                continue;
            }
            whereColumns.append(" and " + column + "=? ");
            parameters.add(filedValue);
        }
        String tableWhere = whereColumns.subSequence(0, whereColumns.length() - 1).toString();

        sql.append("delete from");
        sql.append(" ");
        sql.append(tableAnnotation.value());
        sql.append(" ");
        sql.append("where 1=1 ");
        sql.append(" ");
        sql.append(tableWhere);

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
        SqlUtils.validateTargetClass(object);
        Class<?> clazz = object.getClass();
        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        Field[] fields = SqlUtils.getFields(clazz);
        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();

        List<Object> parameters = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Column columnAnnotation = fields[i].getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }

            Object filedValue = null;
            try {
                filedValue = fields[i].get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }

            Class<?> type = fields[i].getType();
            if (SqlUtils.typeValueIsNotNull(type, filedValue)) {
                whereColumns.append("and");
                whereColumns.append(" ");
                whereColumns.append(column);
                whereColumns.append("=");
                whereColumns.append("?");
                whereColumns.append(" ");
                parameters.add(filedValue);
            }
            columns.append(column + " " + "as" + " " + fields[i].getName());
            columns.append(",");
        }


        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();

        StringBuilder sql = new StringBuilder();
        sql.append("select " + tableColumn + " from " + tableAnnotation.value());
        sql.append(" ");
        sql.append("where 1=1");
        sql.append(" ");
        sql.append(whereColumns);

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
        Object object = SqlUtils.createInstance(clazz);
        // 对象检验
        SqlUtils.validateTargetClass(object);

        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        Field[] fields = SqlUtils.getFields(clazz);
        List<Object> parameters = new ArrayList<>();
        StringBuilder columns = new StringBuilder();
        StringBuilder whereColumns = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Column columnAnnotation = fields[i].getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String fieldName = fields[i].getName();
            String column = columnAnnotation.value();
            if (StringUtils.isEmpty(column)) {
                continue;
            }
            boolean primaryKey = columnAnnotation.primaryKey();
            if (primaryKey) {
                whereColumns.append(column);
            }
            columns.append(column);
            columns.append(" ");
            columns.append("as");
            columns.append(" ");
            columns.append(fieldName);
            columns.append(",");
        }

        String tableColumn = columns.subSequence(0, columns.length() - 1).toString();
        parameters.add(String.valueOf(id));

        StringBuilder sql = new StringBuilder();
        sql.append("select ").append(tableColumn).append(" from ").append(tableAnnotation.value());
        sql.append(" ");
        sql.append("where");
        sql.append(" ");
        sql.append(whereColumns);
        sql.append("=?");

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
        Object object = SqlUtils.createInstance(clazz);
        // 对象检验
        SqlUtils.validateTargetClass(object);

        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        Field[] fields = SqlUtils.getFields(clazz);
        List<Object> parameters = new ArrayList<>();
        StringBuilder whereColumns = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Column columnAnnotation = fields[i].getAnnotation(Column.class);
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
        sql.append("delete from");
        sql.append(" ");
        sql.append(tableAnnotation.value());
        sql.append(" ");
        sql.append("where");
        sql.append(" ");
        sql.append(whereColumns);
        sql.append("=?");

        SqlProvider so = new SqlProvider();
        so.setSql(sql.toString());
        so.setParameters(parameters);
        return so;
    }
}

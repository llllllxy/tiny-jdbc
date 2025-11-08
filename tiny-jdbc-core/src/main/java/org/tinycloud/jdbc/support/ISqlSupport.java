package org.tinycloud.jdbc.support;

import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.util.DataAccessUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * sql操作接口，传入要执行的SQL和要绑定到SQL的参数，操纵数据库，执行增、删、改、查操作
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-07-19
 **/
public interface ISqlSupport<T, ID extends Serializable> {

    /**
     * 使用提供的SQL语句和提供的参数，执行增删改
     *
     * @param sql    要执行的SQL
     * @param params 要绑定到SQL的参数
     * @return int 受影响的行数
     */
    int execute(String sql, Object... params);

    /**
     * 使用提供的SQL语句和提供的参数，执行删
     *
     * @param sql    要执行的SQL
     * @param params 要绑定到SQL的参数
     * @return int 受影响的行数
     */
    default int delete(String sql, final Object... params) {
        return this.execute(sql, params);
    }

    /**
     * 使用提供的SQL语句和提供的参数，执行改
     *
     * @param sql    要执行的SQL
     * @param params 要绑定到SQL的参数
     * @return int 受影响的行数
     */
    default int update(String sql, final Object... params) {
        return this.execute(sql, params);
    }

    /**
     * 使用提供的SQL语句和提供的参数，执行增
     *
     * @param sql    要执行的SQL
     * @param params 要绑定到SQL的参数
     * @return int 受影响的行数
     */
    default int insert(String sql, final Object... params) {
        return this.execute(sql, params);
    }

    /**
     * 查询给定的SQL和参数列表，返回实例列表
     *
     * @param sql    要执行的SQL查询
     * @param params 要绑定到查询的参数
     * @return List<T> 实例列表
     */
    List<T> select(String sql, Object... params);

    /**
     * 查询给定的SQL和参数列表，返回实体列表
     *
     * @param sql    要执行的SQL查询
     * @param params 要绑定到查询的参数
     * @param clazz  实体类
     * @return List<F> 实例列表
     */
    <F> List<F> select(String sql, Class<F> clazz, Object... params);

    /**
     * 查询给定的SQL和参数列表，结果返回第一条
     *
     * @param sql    要执行的SQL查询
     * @param params 要绑定到查询的参数
     * @return T 实例
     */
    default T selectOne(String sql, Object... params) {
        List<T> resultList = this.select(sql, params);
        return DataAccessUtils.singleResult(resultList);
    }

    /**
     * 查询给定的SQL和参数列表，结果返回第一条
     *
     * @param sql    要执行的SQL查询
     * @param params 要绑定到查询的参数
     * @param clazz  实体类
     * @return F 实例
     */
    default <F> F selectOne(String sql, Class<F> clazz, Object... params) {
        List<F> resultList = this.select(sql, clazz, params);
        return DataAccessUtils.singleResult(resultList);
    }

    /**
     * 执行查询sql，有查询条件，（固定返回List<Map<String, Object>>）
     *
     * @param sql    要执行的sql
     * @param params 要绑定到查询的参数
     * @return Map<String, Object>
     */
    List<Map<String, Object>> selectMap(String sql, Object... params);

    /**
     * 执行给定的 SQL 查询，返回指定类型的单列结果列表。
     * 该方法适用于查询预期返回多行单列数据的场景，例如只查询用户表中的所有用户名。
     *
     * @param sql    要执行的sql
     * @param clazz  结果集中单列数据的类型，例如 String.class、Integer.class 等
     * @param params 要绑定到查询的参数
     * @param <F>    单列数据的泛型类型。
     * @return 包含指定类型单列数据的列表，如果没有结果则返回空列表。
     */
    <F> List<F> selectSingleColumn(String sql, Class<F> clazz, Object... params);

    /**
     * 执行查询sql，有查询条件，结果返回第一条（固定返回Map<String, Object>）
     *
     * @param sql    要执行的sql
     * @param params 要绑定到查询的参数
     * @return Map<String, Object>
     */
    default Map<String, Object> selectOneMap(String sql, Object... params) {
        List<Map<String, Object>> resultList = this.selectMap(sql, params);
        return DataAccessUtils.singleResult(resultList);
    }

    /**
     * 执行 SQL 查询，返回单个结果对象。
     * 它适用于查询预期返回单行单列数据的场景，例如获取单个数值、字符串或其他类型的标量值
     *
     * @param sql    要执行的sql（必须返回单行单列），例如 "SELECT COUNT(*) FROM users"。
     * @param clazz  结果集中单行单列数据的类型，例如 String.class、Integer.class 等
     * @param params 要绑定到 SQL 查询语句中的参数，用于替换 SQL 中的占位符（?）。
     * @param <F>    结果对象的泛型类型，由 clazz 参数指定。
     * @return 单个结果对象，类型为 clazz，若查询结果为空，抛出 EmptyResultDataAccessException，若结果超过一行，抛出 IncorrectResultSizeDataAccessException
     */
    <F> F selectOneObject(String sql, Class<F> clazz, Object... params);

    /**
     * 分页查询
     *
     * @param sql 要执行的SQL查询
     * @return T
     */
    Page<T> paginate(String sql, Page<T> page, Object... params);

    /**
     * 分页查询
     *
     * @param sql 要执行的SQL查询
     * @return T
     */
    Page<Map<String, Object>> paginateMap(String sql, Page<Map<String, Object>> page, Object... params);

    /**
     * 分页查询（带参数）
     *
     * @param sql    要执行的SQL
     * @param clazz  实体类型
     * @param page   分页参数
     * @param params ？参数
     * @return Page<F>
     */
    <F> Page<F> paginate(String sql, Class<F> clazz, Page<F> page, final Object... params);

    /**
     * 使用提供的SQL对象，执行增删改操作
     *
     * @param sql 要执行的SQL对象，封装了SQL语句和参数
     * @return int 受影响的行数
     */
    int execute(SQL sql);

    /**
     * 使用提供的SQL对象，执行删除操作
     *
     * @param sql 要执行的SQL对象，封装了SQL语句和参数
     * @return int 受影响的行数
     */
    default int delete(SQL sql) {
        return this.execute(sql);
    }

    /**
     * 使用提供的SQL对象，执行更新操作
     *
     * @param sql 要执行的SQL对象，封装了SQL语句和参数
     * @return int 受影响的行数
     */
    default int update(SQL sql) {
        return this.execute(sql);
    }

    /**
     * 使用提供的SQL对象，执行插入操作
     *
     * @param sql 要执行的SQL对象，封装了SQL语句和参数
     * @return int 受影响的行数
     */
    default int insert(SQL sql) {
        return this.execute(sql);
    }

    /**
     * 使用提供的SQL对象，执行查询操作
     *
     * @param sql 要执行的SQL对象，封装了SQL语句和参数
     * @return List<T> 包含查询结果的列表
     */
    List<T> select(SQL sql);

    /**
     * 使用提供的SQL对象，执行查询操作，返回第一条结果
     *
     * @param sql 要执行的SQL对象，封装了SQL语句和参数
     * @return T 包含查询结果的对象
     */
    default T selectOne(SQL sql) {
        List<T> resultList = this.select(sql);
        return DataAccessUtils.singleResult(resultList);
    }

    /**
     * 使用提供的SQL对象，执行查询操作，返回指定类型的结果列表
     *
     * @param sql   要执行的SQL对象，封装了SQL语句和参数
     * @param clazz 结果集中数据的类型
     * @param <F>   结果对象的泛型类型，由 clazz 参数指定
     * @return 包含指定类型结果数据的列表，如果没有结果则返回空列表
     */
    <F> List<F> select(SQL sql, Class<F> clazz);

    /**
     * 使用提供的SQL对象，执行查询操作，返回指定类型的结果列表，返回第一条结果
     *
     * @param sql   要执行的SQL对象，封装了SQL语句和参数
     * @param clazz 结果集中数据的类型
     * @param <F>   结果对象的泛型类型，由 clazz 参数指定
     * @return 包含指定类型结果数据的对象，如果没有结果则返回 null
     */
    default <F> F selectOne(SQL sql, Class<F> clazz) {
        List<F> resultList = this.select(sql, clazz);
        return DataAccessUtils.singleResult(resultList);
    }

    /**
     * 使用提供的SQL对象，执行分页查询操作
     *
     * @param sql  要执行的SQL对象，封装了SQL语句和参数
     * @param page 分页参数
     * @return Page<T> 包含分页查询结果的对象
     */
    Page<T> paginate(SQL sql, Page<T> page);

    /**
     * 使用提供的SQL对象，执行分页查询操作，返回指定类型的结果列表
     *
     * @param sql   要执行的SQL对象，封装了SQL语句和参数
     * @param clazz 结果集中数据的类型
     * @param page  分页参数
     * @param <F>   结果对象的泛型类型，由 clazz 参数指定
     * @return Page<F> 包含分页查询结果的对象
     */
    <F> Page<F> paginate(SQL sql, Class<F> clazz, Page<F> page);

    /**
     * 使用提供的SQL对象，执行分页查询操作，返回指定类型的结果对象
     *
     * @param sql   要执行的SQL对象，封装了SQL语句和参数
     * @param clazz 结果集中数据的类型
     * @param <F>   结果对象的泛型类型，由 clazz 参数指定
     * @return F 包含分页查询结果的对象，如果没有结果则返回 null
     */
    <F> F selectOneObject(SQL sql, Class<F> clazz);
}

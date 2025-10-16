package org.tinycloud.jdbc;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.tinycloud.jdbc.page.*;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.util.ArrayUtils;
import org.tinycloud.jdbc.util.DataAccessUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * JdbcTemplate增强工具类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-10-14 10:44
 */
public class JdbcTemplateHelper {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取 JdbcTemplate 实例。
     *
     * @return 当前类中持有的 JdbcTemplate 实例
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * 获取 NamedParameterJdbcTemplate 实例。
     * NamedParameterJdbcTemplate 是 JdbcTemplate 的扩展，支持使用命名参数。
     *
     * @return 基于当前 JdbcTemplate 实例创建的 NamedParameterJdbcTemplate 实例
     */
    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }


    /**
     * 构造函数，用于初始化 JdbcTemplateHelper 实例。
     *
     * @param jdbcTemplate Spring 提供的 JdbcTemplate 实例
     */
    public JdbcTemplateHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 执行 SQL 查询语句，返回指定类型的对象列表。
     *
     * @param sql    要执行的 SQL 查询语句
     * @param clazz  结果对象的类型
     * @param params SQL 语句中的参数
     * @param <F>    结果对象的泛型类型
     * @return 包含查询结果的对象列表
     */
    public <F> List<F> select(String sql, Class<F> clazz, Object... params) {
        return getJdbcTemplate().query(sql, new BeanPropertyRowMapper<>(clazz), params);
    }

    /**
     * 执行 SQL 查询语句，返回指定类型的单个对象，如果查询结果为空或超过一条记录，将抛出异常。
     *
     * @param sql    要执行的 SQL 查询语句
     * @param clazz  结果对象的类型
     * @param params SQL 语句中的参数
     * @param <F>    结果对象的泛型类型
     */
    public <F> F selectOne(String sql, Class<F> clazz, Object... params) {
        List<F> resultList = select(sql, clazz, params);
        return DataAccessUtils.singleResult(resultList);
    }

    /**
     * 执行 SQL 查询语句，返回 Map 列表，每个 Map 表示查询结果中的一行记录。
     *
     * @param sql    要执行的 SQL 查询语句
     * @param params SQL 语句中的参数
     * @return 包含查询结果的 Map 列表
     */
    public List<Map<String, Object>> selectMap(String sql, Object... params) {
        return getJdbcTemplate().queryForList(sql, params);
    }

    /**
     * 执行 SQL 查询语句，返回指定类型的单个对象。
     *
     * @param sql    要执行的 SQL 查询语句
     * @param clazz  结果对象的类型
     * @param params SQL 语句中的参数
     * @param <F>    结果对象的泛型类型
     * @return 查询结果中的单个对象
     */
    public <F> F selectForObject(String sql, Class<F> clazz, Object... params) {
        return getJdbcTemplate().queryForObject(sql, clazz, params);
    }

    /**
     * 执行分页查询，返回指定类型的对象列表。
     *
     * @param sql    要执行的 SQL 查询语句
     * @param clazz  结果对象的类型
     * @param page   分页对象，用于指定页码和每页数量
     * @param params SQL 语句中的参数
     * @param <F>    结果对象的泛型类型
     * @return 包含分页信息和查询结果的分页对象
     */
    public <F> Page<F> paginate(String sql, Class<F> clazz, Page<F> page, final Object... params) {
        PageCheck.check(page);
        PageHandleResult handleResult = PageHandleFactory.getPageHandle(getJdbcTemplate()).handle(sql, page.getPageNum(), page.getPageSize());
        // 查询总共数量
        Long count = getJdbcTemplate().queryForObject(handleResult.getCountSql(), Long.class, params);
        List<F> records;
        if (count != null && count > 0L) {
            records = getJdbcTemplate().query(handleResult.getPageSql(), new BeanPropertyRowMapper<>(clazz), ArrayUtils.mergeArrays(params, handleResult.getParameters()));
        } else {
            records = new ArrayList<>();
        }
        page.setRecords(records);
        page.setTotal(count);
        return page;
    }

    /**
     * 执行分页查询，返回 Map 类型的对象列表，每个 Map 表示查询结果中的一行记录。
     *
     * @param sql    要执行的 SQL 查询语句
     * @param page   分页对象，用于指定页码和每页数量
     * @param params SQL 语句中的参数
     * @return 包含分页信息和查询结果的分页对象，结果为 Map 列表
     */
    public Page<Map<String, Object>> paginateMap(String sql, Page<Map<String, Object>> page, Object... params) {
        PageCheck.check(page);
        PageHandleResult handleResult = PageHandleFactory.getPageHandle(getJdbcTemplate()).handle(sql, page.getPageNum(), page.getPageSize());
        // 查询总共数量
        Long count = getJdbcTemplate().queryForObject(handleResult.getCountSql(), Long.class, params);
        List<Map<String, Object>> records;
        if (count != null && count > 0L) {
            records = getJdbcTemplate().queryForList(handleResult.getPageSql(), ArrayUtils.mergeArrays(params, handleResult.getParameters()));
        } else {
            records = new ArrayList<>();
        }
        page.setRecords(records);
        page.setTotal(count);
        return page;
    }

    /**
     * 执行 SQL 语句，通常用于执行 INSERT、UPDATE、DELETE 等更新操作。
     * 该方法借助 Spring 的 JdbcTemplate 来执行 SQL 语句，并返回受影响的行数。
     *
     * @param sql    要执行的 SQL 语句
     * @param params SQL 语句中的参数，可变参数类型
     * @return 执行 SQL 语句后受影响的行数
     */
    public int execute(String sql, final Object... params) {
        return getJdbcTemplate().update(sql, params);
    }

    /**
     * 执行 INSERT 语句，向数据库中插入数据。
     * 该方法调用 execute 方法来实际执行 SQL 插入操作，并返回受影响的行数。
     *
     * @param sql    要执行的 INSERT SQL 语句
     * @param params SQL 语句中的参数，可变参数类型
     * @return 执行插入操作后受影响的行数
     */
    public int insert(String sql, final Object... params) {
        return execute(sql, params);
    }

    /**
     * 执行 UPDATE 语句，更新数据库中的数据。
     * 该方法调用 execute 方法来实际执行 SQL 更新操作，并返回受影响的行数。
     *
     * @param sql    要执行的 UPDATE SQL 语句
     * @param params SQL 语句中的参数，可变参数类型
     * @return 执行更新操作后受影响的行数
     */
    public int update(String sql, final Object... params) {
        return execute(sql, params);
    }

    /**
     * 执行 DELETE 语句，从数据库中删除符合条件的数据。
     * 该方法调用 execute 方法来实际执行 SQL 删除操作，并返回受影响的行数。
     *
     * @param sql    要执行的 DELETE SQL 语句
     * @param params SQL 语句中的参数，可变参数类型
     * @return 执行删除操作后受影响的行数
     */
    public int delete(String sql, final Object... params) {
        return execute(sql, params);
    }

    /**
     * 执行自定义 SQL 对象封装的 SQL 语句，通常用于执行 INSERT、UPDATE、DELETE 等更新操作。
     * 该方法会将自定义 SQL 对象转换为标准 SQL 语句和参数数组，再调用本类的另一个 execute 方法执行，并返回受影响的行数。
     *
     * @param sql 自定义 SQL 对象，封装了 SQL 语句和对应的参数
     * @return 执行 SQL 语句后受影响的行数
     */
    public int execute(SQL sql) {
        return execute(sql.toSql(), sql.getParameters().toArray());
    }

    /**
     * 执行自定义 SQL 对象封装的 INSERT 语句，向数据库中插入数据。
     * 该方法会将自定义 SQL 对象转换为标准 SQL 语句和参数数组，
     * 再调用本类的另一个 insert 方法执行插入操作，并返回受影响的行数。
     *
     * @param sql 自定义 SQL 对象，封装了 INSERT SQL 语句和对应的参数
     * @return 执行插入操作后受影响的行数
     */
    public int insert(SQL sql) {
        return insert(sql.toSql(), sql.getParameters().toArray());
    }

    /**
     * 执行自定义 SQL 对象封装的 UPDATE 语句，更新数据库中的数据。
     * 该方法会将自定义 SQL 对象转换为标准 SQL 语句和参数数组，
     * 再调用本类的另一个 update 方法执行更新操作，并返回受影响的行数。
     *
     * @param sql 自定义 SQL 对象，封装了 UPDATE SQL 语句和对应的参数
     * @return 执行更新操作后受影响的行数
     */
    public int update(SQL sql) {
        return update(sql.toSql(), sql.getParameters().toArray());
    }

    /**
     * 执行自定义 SQL 对象封装的 DELETE 语句，从数据库中删除符合条件的数据。
     * 该方法会将自定义 SQL 对象转换为标准 SQL 语句和参数数组，
     * 再调用本类的另一个 delete 方法执行删除操作，并返回受影响的行数。
     *
     * @param sql 自定义 SQL 对象，封装了 DELETE SQL 语句和对应的参数
     * @return 执行删除操作后受影响的行数
     */
    public int delete(SQL sql) {
        return delete(sql.toSql(), sql.getParameters().toArray());
    }

    /**
     * 执行自定义 SQL 对象封装的查询语句，返回指定类型的对象列表。
     * 该方法会将自定义 SQL 对象转换为标准 SQL 语句和参数数组，
     * 再调用本类的另一个 select 方法执行查询操作，并返回查询结果列表。
     *
     * @param sql   自定义 SQL 对象，封装了查询 SQL 语句和对应的参数
     * @param clazz 结果对象的类型
     * @param <F>   结果对象的泛型类型
     * @return 包含查询结果的对象列表
     */
    public <F> List<F> select(SQL sql, Class<F> clazz) {
        return select(sql.toSql(), clazz, sql.getParameters().toArray());
    }

    /**
     * 执行自定义 SQL 对象封装的分页查询，返回指定类型的对象分页结果。
     * 该方法会将自定义 SQL 对象转换为标准 SQL 语句和参数数组，
     * 再调用本类的另一个 paginate 方法执行分页查询操作，并返回包含分页信息和查询结果的分页对象。
     *
     * @param sql   自定义 SQL 对象，封装了查询 SQL 语句和对应的参数
     * @param clazz 结果对象的类型
     * @param page  分页对象，用于指定页码和每页数量
     * @param <F>   结果对象的泛型类型
     * @return 包含分页信息和查询结果的分页对象
     */
    public <F> Page<F> paginate(SQL sql, Class<F> clazz, Page<F> page) {
        return paginate(sql.toSql(), clazz, page, sql.getParameters().toArray());
    }

    /**
     * 执行自定义 SQL 对象封装的查询语句，返回指定类型的单个对象。
     * 该方法会将自定义 SQL 对象转换为标准 SQL 语句和参数数组，
     * 再调用本类的另一个 selectForObject 方法执行查询操作，并返回查询结果中的单个对象。
     *
     * @param sql   自定义 SQL 对象，封装了查询 SQL 语句和对应的参数
     * @param clazz 结果对象的类型
     * @param <F>   结果对象的泛型类型
     * @return 查询结果中的单个对象
     */
    public <F> F selectForObject(SQL sql, Class<F> clazz) {
        return selectForObject(sql.toSql(), clazz, sql.getParameters().toArray());
    }

    /**
     * 执行自定义 SQL 对象封装的查询语句，返回指定类型的单个对象。
     * 若查询结果为空，返回 {@code null}；若查询结果超过一条记录，可能会抛出异常，具体取决于 {@link DataAccessUtils#singleResult} 方法的实现。
     *
     * @param sql   自定义 SQL 对象，封装了查询 SQL 语句和对应的参数
     * @param clazz 结果对象的类型
     * @param <F>   结果对象的泛型类型
     * @return 查询结果中的单个对象，若结果为空则返回 {@code null}
     */
    public <F> F selectOne(SQL sql, Class<F> clazz) {
        List<F> resultList = select(sql, clazz);
        return DataAccessUtils.singleResult(resultList);
    }
}

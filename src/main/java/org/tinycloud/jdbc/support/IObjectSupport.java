package org.tinycloud.jdbc.support;

import org.springframework.util.ObjectUtils;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.util.DataAccessUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 对象操作接口，传入要执行的实例，操纵数据库，执行增、删、改、查操作，
 * 前提是传入的实例中用@Table指定了数据库表，用@Column指定了表字段
 * 对象操作只支持对单表的增、删、改、查。多表查询和存储过程等请使用sql操作接口或JdbcTemplate原生接口
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
public interface IObjectSupport<T, ID> {
    /**
     * 持久化插入给定的实例（默认忽略null值）
     *
     * @param entity 实例
     * @return int 受影响的行数
     */
    int insert(T entity);

    /**
     * 持久化插入给定的实例
     *
     * @param entity      实例
     * @param ignoreNulls 是否忽略null值，true忽略，false不忽略
     * @return int 受影响的行数
     */
    int insert(T entity, boolean ignoreNulls);

    /**
     * 持久化插入给定的实例，并且返回自增主键（默认忽略null值）
     *
     * @param entity 实例
     * @return Integer 返回主键
     */
    Long insertReturnAutoIncrement(T entity);

    /**
     * 持久化插入给定的实例，并且返回自增主键
     *
     * @param entity      实例
     * @param ignoreNulls 是否忽略null值，true忽略，false不忽略
     * @return Integer 返回主键
     */
    Long insertReturnAutoIncrement(T entity, boolean ignoreNulls);

    /**
     * 持久化更新给定的实例（默认忽略null值），根据主键值更新
     *
     * @param entity 实例
     * @return int 受影响的行数
     */
    int updateById(T entity);

    /**
     * 持久化更新给定的实例，根据主键值更新
     *
     * @param entity      实例
     * @param ignoreNulls 是否忽略null值，true忽略，false不忽略
     * @return int 受影响的行数
     */
    int updateById(T entity, boolean ignoreNulls);

    /**
     * 持久化更新给定的实例（默认忽略null值）
     *
     * @param criteria 条件构造器
     * @return int 受影响的行数
     */
    int update(T entity, boolean ignoreNulls, UpdateCriteria<T> criteria);

    /**
     * 持久化更新给定的实例（默认忽略null值）
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return int 受影响的行数
     */
    int update(T entity, boolean ignoreNulls, LambdaUpdateCriteria<T> lambdaCriteria);

    /**
     * 持久化更新给定的实例（默认忽略null值）
     *
     * @param criteria 条件构造器
     * @return int 受影响的行数
     */
    int update(T entity, UpdateCriteria<T> criteria);

    /**
     * 持久化更新给定的实例（默认忽略null值）
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return int 受影响的行数
     */
    int update(T entity, LambdaUpdateCriteria<T> lambdaCriteria);

    /**
     * 持久化更新给定的实例（完全使用构造器）
     *
     * @param criteria 条件构造器
     * @return int 受影响的行数
     */
    int update(UpdateCriteria<T> criteria);

    /**
     * 持久化更新给定的实例（完全使用构造器）
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return int 受影响的行数
     */
    int update(LambdaUpdateCriteria<T> lambdaCriteria);

    /**
     * 持久化删除给定的实例
     *
     * @param entity 实例
     * @return int 受影响的行数
     */
    int delete(T entity);

    /**
     * 持久化删除给定的实例
     *
     * @param criteria 条件构造器
     * @return int 受影响的行数
     */
    int delete(UpdateCriteria<T> criteria);

    /**
     * 持久化删除给定的实例
     *
     * @param criteria 条件构造器(lambda版)
     * @return int 受影响的行数
     */
    int delete(LambdaUpdateCriteria<T> criteria);

    /**
     * 根据ID进行删除
     *
     * @param id 主键id
     * @return T 对象
     */
    int deleteById(ID id);

    /**
     * 根据ID列表进行批量删除
     *
     * @param ids 主键id列表
     * @return int 删除数量
     */
    int deleteByIds(List<ID> ids);

    /**
     * 根据ID列表进行批量删除
     *
     * @param id 主键id列表
     * @return int 删除数量
     */
    @SuppressWarnings("unchecked")
    default int deleteByIds(ID... id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new TinyJdbcException("deleteByIds ids cannot be null or empty");
        }
        List<ID> ids = Arrays.asList(id);
        return this.deleteByIds(ids);
    }

    /**
     * 清空表
     * truncate table操作的返回值是0
     *
     * @return 0
     */
    int truncate();

    /**
     * 批量持久化插入给定的实例
     *
     * @param collection 实例集合
     * @return 一个数组，其中包含受批处理中每个更新影响的行数
     */
    int[] batchInsert(Collection<T> collection);

    /**
     * 查询给定的id，返回一个实例
     *
     * @param id 主键id
     * @return T 对象
     */
    T selectById(ID id);

    /**
     * 根据ID列表进行批量查询
     *
     * @param ids 主键id列表
     * @return List<T> 实例列表
     */
    List<T> selectByIds(List<ID> ids);

    /**
     * 根据ID列表进行批量查询
     *
     * @param id 主键id列表
     * @return List<T> 实例列表
     */
    @SuppressWarnings("unchecked")
    default List<T> selectByIds(ID... id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new TinyJdbcException("selectByIds ids cannot be null or empty");
        }
        List<ID> ids = Arrays.asList(id);
        return this.selectByIds(ids);
    }

    /**
     * 查询给定的实例，返回实例列表
     *
     * @param entity 实例
     * @return List<T> 实例列表
     */
    List<T> select(T entity);

    /**
     * 查询给定的实例，返回实例列表
     *
     * @param criteria 条件构造器
     * @return List<T> 实例列表
     */
    List<T> select(QueryCriteria<T> criteria);

    /**
     * 查询给定的实例，返回实例列表
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return List<T> 实例列表
     */
    List<T> select(LambdaQueryCriteria<T> lambdaCriteria);

    /**
     * 分页查询给定的实例，返回实例列表
     *
     * @param entity 实例
     * @param page   分页参数
     * @return List<T> 实例列表
     */
    Page<T> paginate(T entity, Page<T> page);

    /**
     * 分页查询给定的实例，返回实例列表
     *
     * @param criteria 条件构造器
     * @param page     分页参数
     * @return List<T> 实例列表
     */
    Page<T> paginate(QueryCriteria<T> criteria, Page<T> page);

    /**
     * 分页查询给定的实例，返回实例列表
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @param page           分页参数
     * @return List<T> 实例列表
     */
    Page<T> paginate(LambdaQueryCriteria<T> lambdaCriteria, Page<T> page);

    /**
     * 查询给定的实例，返回一个实例
     *
     * @param entity 实例
     * @return T 实例
     */
    default T selectOne(T entity) {
        List<T> list = this.select(entity);
        return DataAccessUtils.singleResult(list);
    }

    /**
     * 查询给定的实例，返回一个实例
     *
     * @param criteria 条件构造器
     * @return T 实例
     */
    default T selectOne(QueryCriteria<T> criteria) {
        List<T> list = this.select(criteria);
        return DataAccessUtils.singleResult(list);
    }

    /**
     * 查询给定的实例，返回一个实例
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return T 实例
     */
    default T selectOne(LambdaQueryCriteria<T> lambdaCriteria) {
        List<T> list = this.select(lambdaCriteria);
        return DataAccessUtils.singleResult(list);
    }

    /**
     * 根据 criteria 条件，查询总记录数
     *
     * @param criteria 条件构造器
     * @return 总记录数
     */
    Long selectCount(QueryCriteria<T> criteria);

    /**
     * 根据 criteria 条件，查询总记录数
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return 总记录数
     */
    Long selectCount(LambdaQueryCriteria<T> lambdaCriteria);

    /**
     * 查询记录是否存在
     *
     * @param criteria 条件构造器
     * @return true存在，false不存在
     */
    default boolean exists(QueryCriteria<T> criteria) {
        Long count = this.selectCount(criteria);
        return null != count && count > 0L;
    }

    /**
     * 查询记录是否存在
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return true存在，false不存在
     */
    default boolean exists(LambdaQueryCriteria<T> lambdaCriteria) {
        Long count = this.selectCount(lambdaCriteria);
        return null != count && count > 0L;
    }
}

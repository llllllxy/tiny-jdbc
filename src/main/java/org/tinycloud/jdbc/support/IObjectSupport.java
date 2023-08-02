package org.tinycloud.jdbc.support;

import org.tinycloud.jdbc.criteria.Criteria;
import org.tinycloud.jdbc.criteria.LambdaCriteria;
import org.tinycloud.jdbc.page.Page;

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
     * 持久化插入给定的实例，并且返回自增主键
     *
     * @param entity 实例
     * @return Integer 返回主键
     */
    Long insertReturnAutoIncrement(T entity);

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
    int update(T entity, boolean ignoreNulls, Criteria criteria);

    /**
     * 持久化更新给定的实例（默认忽略null值）
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return int 受影响的行数
     */
    int update(T entity, boolean ignoreNulls, LambdaCriteria lambdaCriteria);

    /**
     * 持久化更新给定的实例（默认忽略null值）
     *
     * @param criteria 条件构造器
     * @return int 受影响的行数
     */
    int update(T entity, Criteria criteria);

    /**
     * 持久化更新给定的实例（默认忽略null值）
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return int 受影响的行数
     */
    int update(T entity, LambdaCriteria lambdaCriteria);

    /**
     * 持久化删除给定的实例
     *
     * @param entity 实例
     * @return int 受影响的行数
     */
    int delete(T entity);

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
     * @return T 对象
     */
    int deleteByIds(List<ID> ids);

    /**
     * 批量持久化更新给定的实例
     *
     * @param collection 实例集合
     * @return 一个数组，其中包含受批处理中每个更新影响的行数
     */
    int[] batchUpdate(Collection<T> collection);

    /**
     * 批量持久化插入给定的实例
     *
     * @param collection 实例集合
     * @return 一个数组，其中包含受批处理中每个更新影响的行数
     */
    int[] batchInsert(Collection<T> collection);

    /**
     * 批量持久化删除给定的实例
     *
     * @param collection 实例集合
     * @return 一个数组，其中包含受批处理中每个更新影响的行数
     */
    int[] batchDelete(Collection<T> collection);

    /**
     * 查询给定的id，返回一个实例
     *
     * @param id 主键id
     * @return T 对象
     */
    T selectById(ID id);

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
    List<T> select(Criteria criteria);

    /**
     * 查询给定的实例，返回实例列表
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return List<T> 实例列表
     */
    List<T> select(LambdaCriteria lambdaCriteria);

    /**
     * 分页查询给定的实例，返回实例列表
     *
     * @param entity     实例
     * @param pageNumber 页码
     * @param pageSize   页大小
     * @return List<T> 实例列表
     */
    Page<T> paginate(T entity, Integer pageNumber, Integer pageSize);

    /**
     * 分页查询给定的实例，返回实例列表
     *
     * @param criteria   条件构造器
     * @param pageNumber 页码
     * @param pageSize   页大小
     * @return List<T> 实例列表
     */
    Page<T> paginate(Criteria criteria, Integer pageNumber, Integer pageSize);

    /**
     * 分页查询给定的实例，返回实例列表
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @param pageNumber     页码
     * @param pageSize       页大小
     * @return List<T> 实例列表
     */
    Page<T> paginate(LambdaCriteria lambdaCriteria, Integer pageNumber, Integer pageSize);

    /**
     * 查询给定的实例，返回一个实例
     *
     * @param entity 实例
     * @return T 实例
     */
    T selectOne(T entity);

    /**
     * 查询给定的实例，返回一个实例
     *
     * @param criteria 条件构造器
     * @return T 实例
     */
    T selectOne(Criteria criteria);

    /**
     * 查询给定的实例，返回一个实例
     *
     * @param lambdaCriteria 条件构造器(lambda版)
     * @return T 实例
     */
    T selectOne(LambdaCriteria lambdaCriteria);
}

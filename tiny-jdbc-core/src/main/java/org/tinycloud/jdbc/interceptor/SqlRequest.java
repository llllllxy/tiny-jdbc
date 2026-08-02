package org.tinycloud.jdbc.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 执行请求，封装可被拦截器改写的 SQL 和参数。
 *
 * @param <R> 执行结果类型
 */
public class SqlRequest<R> {
    private String sql;
    private Object[] args;
    private List<Object[]> batchArgs;
    private final SqlType sqlType;
    private final Map<String, Object> attributes = new HashMap<>(0);

    /**
     * 创建普通 SQL 执行请求。
     *
     * @param sql     SQL 语句
     * @param args    SQL 参数
     * @param sqlType SQL 类型
     */
    public SqlRequest(String sql, Object[] args, SqlType sqlType) {
        this.sql = sql;
        this.args = copyArgs(args);
        this.sqlType = sqlType;
    }

    /**
     * 创建批量 SQL 执行请求。
     *
     * @param sql       SQL 语句
     * @param batchArgs 批量参数
     * @param <R>       执行结果类型
     * @return 批量 SQL 请求
     */
    public static <R> SqlRequest<R> batch(String sql, List<Object[]> batchArgs) {
        SqlRequest<R> request = new SqlRequest<>(sql, null, SqlType.BATCH);
        request.setBatchStatement(sql, batchArgs);
        return request;
    }

    /**
     * 获取当前 SQL 语句。
     *
     * @return 当前 SQL 语句
     */
    public String getSql() {
        return this.sql;
    }

    /**
     * 获取当前普通 SQL 参数的副本。
     *
     * @return SQL 参数副本
     */
    public Object[] getArgs() {
        return copyArgs(this.args);
    }

    /**
     * 原子替换普通 SQL 语句及其参数。
     *
     * @param sql  替换后的 SQL 语句
     * @param args 替换后的 SQL 参数
     */
    public void setStatement(String sql, Object[] args) {
        this.sql = sql;
        this.args = copyArgs(args);
    }

    /**
     * 获取批量 SQL 参数的深拷贝。
     *
     * @return 批量 SQL 参数副本
     */
    public List<Object[]> getBatchArgs() {
        List<Object[]> copiedBatchArgs = new ArrayList<>(this.batchArgs == null ? 0 : this.batchArgs.size());
        if (this.batchArgs != null) {
            for (Object[] batchArg : this.batchArgs) {
                copiedBatchArgs.add(copyArgs(batchArg));
            }
        }
        return copiedBatchArgs;
    }

    /**
     * 原子替换批量 SQL 语句及其参数。
     *
     * @param sql       替换后的 SQL 语句
     * @param batchArgs 替换后的批量 SQL 参数
     */
    public void setBatchStatement(String sql, List<Object[]> batchArgs) {
        this.sql = sql;
        this.batchArgs = new ArrayList<>(batchArgs == null ? 0 : batchArgs.size());
        if (batchArgs != null) {
            for (Object[] batchArg : batchArgs) {
                this.batchArgs.add(copyArgs(batchArg));
            }
        }
    }

    /**
     * 获取 SQL 类型。
     *
     * @return SQL 类型
     */
    public SqlType getSqlType() {
        return this.sqlType;
    }

    /**
     * 设置请求上下文属性。
     *
     * @param key   属性键
     * @param value 属性值
     */
    public void putAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    /**
     * 获取请求上下文属性。
     *
     * @param key 属性键
     * @return 属性值，不存在时返回 null
     */
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * 复制 SQL 参数，避免调用方在执行期间修改参数数组。
     *
     * @param args 原始 SQL 参数
     * @return SQL 参数副本
     */
    private static Object[] copyArgs(Object[] args) {
        return args == null ? new Object[0] : Arrays.copyOf(args, args.length);
    }
}

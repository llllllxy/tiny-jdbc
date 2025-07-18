package org.tinycloud.jdbc.page;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 分页查询适配器-InforMix
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-17 11:48
 */
public class InforMixPageHandleImpl implements IPageHandle {
    /**
     * 分页查询适配
     *
     * @param oldSQL   需要改造为分页查询的SQL
     * @param pageNo   pageNo 第几页，用来计算offset，这个值由（pageNo-1）*pageSize
     * @param pageSize pageSize 每页数量
     * @return 处理过后的sql
     */
    @Override
    public PagingSQLProvider handlerPagingSQL(String oldSQL, long pageNo, long pageSize) {
        long offset = (pageNo - 1L) * pageSize;
        long limit = pageSize;
        StringBuilder ret = new StringBuilder();
        // 这个sql的分页的是紧跟着SELECT的（SELECT SKIP ? FIRST ? * FROM user WHERE age > 18），所以暂时拼接，无法参数后置
        ret.append(String.format("select skip %s first %s ", offset + "", limit + ""));
        ret.append(oldSQL.replaceFirst("(?i)select", ""));
        return PagingSQLProvider.create(ret.toString());
    }

    @Override
    public String handlerCountSQL(String oldSQL) {
        return "SELECT COUNT(*) FROM ( " +
                oldSQL +
                " ) TEMP";
    }
}

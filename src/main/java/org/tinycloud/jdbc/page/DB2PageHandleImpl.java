package org.tinycloud.jdbc.page;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * @author liuxingyu01
 * @date 2022-05-14 16:23
 * @description 分页查询适配器-DB2
 **/
public class DB2PageHandleImpl implements IPageHandle {

    private final JdbcTemplate jdbcTemplate;

    public DB2PageHandleImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    /**
     * 分页查询适配
     * @param oldSQL 需要改造为分页查询的SQL
     * @param pageNo pageNo 第几页，用来计算first 这个值由（pageNo-1）*pageSize
     * @param pageSize pageSize 每页数量
     * @return
     */
    @Override
    public String handlerPagingSQL(String oldSQL, int pageNo, int pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ( SELECT B.*, ROWNUMBER() OVER() AS RN FROM ( ");
        if (pageSize > 0) {
            sql.append(oldSQL);
            int pageStart = (pageNo - 1) * pageSize + 1;
            int pageEnd = pageStart + pageSize -1;
            sql.append(" ) AS B ) AS A WHERE A.RN BETWEEN ").append(pageStart).append(" AND ")
                    .append(pageEnd);
        }
        return sql.toString();
    }


    @Override
    public String handlerCountSQL(String oldSQL) {
        StringBuilder newSql = new StringBuilder();
        newSql.append("SELECT COUNT(*) FROM ( ");
        newSql.append(oldSQL);
        newSql.append(" ) TEMP");
        return newSql.toString();
    }


    @Override
    public Page<Map<String, Object>> getPage(String oldSQL, int pageNo, int pageSize) {
        if (pageNo <= 0) {
            throw new RuntimeException("当前页数必须大于1");
        }
        if (pageSize <= 0) {
            throw new RuntimeException("每页大小必须大于1");
        }
        // 数据列表
        List<Map<String, Object>> list = jdbcTemplate.queryForList(this.handlerPagingSQL(oldSQL, pageNo, pageSize));
        // 总共数量
        int totalSize = jdbcTemplate.queryForObject(handlerCountSQL(oldSQL), Integer.class);

        Page<Map<String, Object>> bean = new Page<>(pageNo, pageSize);
        bean.setRecords(list);
        bean.setTotal(totalSize);
        return bean;
    }
}

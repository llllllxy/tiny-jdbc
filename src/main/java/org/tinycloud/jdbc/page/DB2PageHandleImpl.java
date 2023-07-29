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
}

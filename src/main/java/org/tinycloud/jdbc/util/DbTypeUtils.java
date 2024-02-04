package org.tinycloud.jdbc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.tinycloud.jdbc.exception.JdbcException;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class DbTypeUtils {
    private static final Logger logger = LoggerFactory.getLogger(DbTypeUtils.class);

    private DbTypeUtils() {
    }


    /**
     * 获取当前配置的 DbType
     */
    public static DbType getDbType(DataSource dataSource) {
        String jdbcUrl = getJdbcUrl(dataSource);
        if (!StringUtils.isEmpty(jdbcUrl)) {
            return parseDbType(jdbcUrl);
        }
        throw new IllegalStateException("Can not get dataSource jdbcUrl: " + dataSource.getClass().getName());
    }

    /**
     * 通过数据源中获取 jdbc 的 url 配置
     * 符合 HikariCP, druid, c3p0, DBCP, BEECP 数据源框架 以及 MyBatis UnpooledDataSource 的获取规则
     *
     * @return jdbc url 配置
     */
    public static String getJdbcUrl(DataSource dataSource) {
        String[] methodNames = new String[]{"getUrl", "getJdbcUrl"};
        for (String methodName : methodNames) {
            try {
                Method method = dataSource.getClass().getMethod(methodName);
                return (String) method.invoke(dataSource);
            } catch (Exception e) {
                //ignore
            }
        }

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            return connection.getMetaData().getURL();
        } catch (Exception e) {
            throw new JdbcException("Can not get the dataSource jdbcUrl!");
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) { //ignore
                }
            }
        }
    }


    /**
     * 参考 druid  和 MyBatis-plus 的 JdbcUtils
     *
     * @param jdbcUrl jdbcURL
     * @return 返回数据库类型
     */
    public static DbType parseDbType(String jdbcUrl) {
        if (StringUtils.isEmpty(jdbcUrl)) {
            throw new IllegalStateException("The jdbcUrl is null, cannot parse DialectEnum!");
        }
        jdbcUrl = jdbcUrl.toLowerCase();
        if (jdbcUrl.contains(":mysql:") || jdbcUrl.contains(":cobar:")) {
            return DbType.MYSQL;
        } else if (jdbcUrl.contains(":mariadb:")) {
            return DbType.MARIADB;
        } else if (jdbcUrl.contains(":oracle:")) {
            return DbType.ORACLE;
        } else if (jdbcUrl.contains(":sqlserver2012:")) {
            return DbType.SQLSERVER;
        } else if (jdbcUrl.contains(":sqlserver:") || jdbcUrl.contains(":microsoft:")) {
            return DbType.SQLSERVER_2005;
        } else if (jdbcUrl.contains(":postgresql:")) {
            return DbType.POSTGRE_SQL;
        } else if (jdbcUrl.contains(":hsqldb:")) {
            return DbType.HSQL;
        } else if (jdbcUrl.contains(":db2:")) {
            return DbType.DB2;
        } else if (jdbcUrl.contains(":sqlite:")) {
            return DbType.SQLITE;
        } else if (jdbcUrl.contains(":h2:")) {
            return DbType.H2;
        } else if (isMatchedRegex(":dm\\d*:", jdbcUrl)) {
            return DbType.DM;
        } else if (jdbcUrl.contains(":xugu:")) {
            return DbType.XUGU;
        } else if (isMatchedRegex(":kingbase\\d*:", jdbcUrl)) {
            return DbType.KINGBASE_ES;
        } else if (jdbcUrl.contains(":phoenix:")) {
            return DbType.PHOENIX;
        } else if (jdbcUrl.contains(":zenith:")) {
            return DbType.GAUSS;
        } else if (jdbcUrl.contains(":gbase:")) {
            return DbType.GBASE;
        } else if (jdbcUrl.contains(":gbasedbt-sqli:") || jdbcUrl.contains(":informix-sqli:")) {
            return DbType.GBASE_8S;
        } else if (jdbcUrl.contains(":ch:") || jdbcUrl.contains(":clickhouse:")) {
            return DbType.CLICK_HOUSE;
        } else if (jdbcUrl.contains(":oscar:")) {
            return DbType.OSCAR;
        } else if (jdbcUrl.contains(":sybase:")) {
            return DbType.SYBASE;
        } else if (jdbcUrl.contains(":oceanbase:")) {
            return DbType.OCEAN_BASE;
        } else if (jdbcUrl.contains(":highgo:")) {
            return DbType.HIGH_GO;
        } else if (jdbcUrl.contains(":cubrid:")) {
            return DbType.CUBRID;
        } else if (jdbcUrl.contains(":goldilocks:")) {
            return DbType.GOLDILOCKS;
        } else if (jdbcUrl.contains(":csiidb:")) {
            return DbType.CSIIDB;
        } else if (jdbcUrl.contains(":sap:")) {
            return DbType.SAP_HANA;
        } else if (jdbcUrl.contains(":impala:")) {
            return DbType.IMPALA;
        } else if (jdbcUrl.contains(":vertica:")) {
            return DbType.VERTICA;
        } else if (jdbcUrl.contains(":xcloud:")) {
            return DbType.XCloud;
        } else if (jdbcUrl.contains(":firebirdsql:")) {
            return DbType.FIREBIRD;
        } else if (jdbcUrl.contains(":redshift:")) {
            return DbType.REDSHIFT;
        } else if (jdbcUrl.contains(":opengauss:")) {
            return DbType.OPENGAUSS;
        } else if (jdbcUrl.contains(":taos:") || jdbcUrl.contains(":taos-rs:")) {
            return DbType.TDENGINE;
        } else if (jdbcUrl.contains(":informix")) {
            return DbType.INFORMIX;
        } else if (jdbcUrl.contains(":sinodb")) {
            return DbType.SINODB;
        } else if (jdbcUrl.contains(":uxdb:")) {
            return DbType.UXDB;
        } else if (jdbcUrl.contains(":greenplum:")) {
            return DbType.GREENPLUM;
        } else {
            logger.warn("The jdbcUrl " + jdbcUrl + ", cannot parse DialectEnum or the database is not supported!");
            return DbType.OTHER;
        }
    }

    /**
     * 正则匹配，验证成功返回 true，验证失败返回 false
     */
    public static boolean isMatchedRegex(String regex, String jdbcUrl) {
        if (null == jdbcUrl) {
            return false;
        }
        return Pattern.compile(regex).matcher(jdbcUrl).find();
    }
}

package org.tinycloud.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.page.DB2PageHandleImpl;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.MysqlPageHandleImpl;
import org.tinycloud.jdbc.page.OraclePageHandleImpl;
import org.tinycloud.jdbc.util.DbType;
import org.tinycloud.jdbc.util.DbTypeUtils;

import javax.sql.DataSource;

@Configuration
public class TinyJdbcAutoConfiguration {
    final static Logger logger = LoggerFactory.getLogger(TinyJdbcAutoConfiguration.class);


    @ConditionalOnMissingBean(IPageHandle.class)
    @Bean
    public IPageHandle pageHandle(@Autowired DataSource dataSource) {
        DbType dbType = DbTypeUtils.getDbType(dataSource);
        if (logger.isInfoEnabled()) {
            logger.info("TinyJdbcAutoConfiguration pageHandle dbType={}", dbType.getName());
        }
        IPageHandle pageHandle;
        if (dbType == DbType.MYSQL) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.DB2) {
            pageHandle = new DB2PageHandleImpl();
        } else if (dbType == DbType.ORACLE) {
            pageHandle = new OraclePageHandleImpl();
        } else {
            pageHandle = new MysqlPageHandleImpl();
        }
        if (logger.isInfoEnabled()) {
            logger.info("TinyJdbcAutoConfiguration pageHandle is running!");
        }
        return pageHandle;
    }

    @Bean
    public BaseDao baseDao(@Autowired JdbcTemplate jdbcTemplate, @Autowired IPageHandle pageHandle) {
        if (jdbcTemplate == null) {
            logger.error("TinyJdbcAutoConfiguration: Bean jdbcTemplate is null");
        }
        if (pageHandle == null) {
            logger.error("TinyJdbcAutoConfiguration: Bean pageHandle Not Defined");
        }
        return new BaseDao(jdbcTemplate, pageHandle);
    }
}

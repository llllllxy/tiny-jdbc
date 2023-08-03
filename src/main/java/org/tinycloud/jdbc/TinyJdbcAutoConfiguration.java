package org.tinycloud.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tinycloud.jdbc.page.*;
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
        } else if (dbType == DbType.POSTGRE_SQL) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else {
            pageHandle = new MysqlPageHandleImpl();
        }
        if (logger.isInfoEnabled()) {
            logger.info("TinyJdbcAutoConfiguration pageHandle is running!");
        }
        return pageHandle;
    }
}

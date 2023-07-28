package org.tinycloud.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.page.DB2PageHandleImpl;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.MysqlPageHandleImpl;
import org.tinycloud.jdbc.page.OraclePageHandleImpl;

@Configuration
@EnableConfigurationProperties(TinyJdbcProperties.class)
public class TinyJdbcAutoConfiguration {
    final static Logger logger = LoggerFactory.getLogger(TinyJdbcAutoConfiguration.class);

    @Autowired
    private TinyJdbcProperties tinyJdbcProperties;


    @ConditionalOnMissingBean(IPageHandle.class)
    @ConditionalOnProperty(name = "tiny-jdbc.db-type", havingValue = "mysql")
    @Bean
    public IPageHandle mysqlPageHandle(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            logger.error("TinyJdbcAutoConfiguration: Bean jdbcTemplate is null");
        }
        logger.info("mysqlPageHandle is running");
        return new MysqlPageHandleImpl(jdbcTemplate);
    }


    @ConditionalOnMissingBean(IPageHandle.class)
    @ConditionalOnProperty(name = "tiny-jdbc.db-type", havingValue = "oracle")
    @Bean
    public IPageHandle oraclePageHandle(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            logger.error("TinyJdbcAutoConfiguration: Bean jdbcTemplate is null");
        }
        logger.info("oraclePageHandle is running");
        return new OraclePageHandleImpl(jdbcTemplate);
    }


    @ConditionalOnMissingBean(IPageHandle.class)
    @ConditionalOnProperty(name = "tiny-jdbc.db-type", havingValue = "db2")
    @Bean
    public IPageHandle db2PageHandle(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            logger.error("TinyJdbcAutoConfiguration: Bean jdbcTemplate is null");
        }
        logger.info("db2PageHandle is running");
        return new DB2PageHandleImpl(jdbcTemplate);
    }

    @Bean
    public BaseDao baseDao(JdbcTemplate jdbcTemplate, IPageHandle pageHandle) {
        if (jdbcTemplate == null) {
            logger.error("TinyJdbcAutoConfiguration: Bean jdbcTemplate is null");
        }
        if (pageHandle == null) {
            logger.error("TinyJdbcAutoConfiguration: Bean pageHandle Not Defined");
        }
        return new BaseDao(jdbcTemplate, pageHandle);
    }
}

package org.tinycloud.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tinycloud.jdbc.config.GlobalConfig;
import org.tinycloud.jdbc.config.GlobalConfigUtils;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.page.*;
import org.tinycloud.jdbc.util.DbType;
import org.tinycloud.jdbc.util.DbTypeUtils;

import javax.sql.DataSource;
import java.util.function.Consumer;

@Configuration
public class TinyJdbcAutoConfiguration implements ApplicationContextAware {
    final static Logger logger = LoggerFactory.getLogger(TinyJdbcAutoConfiguration.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @ConditionalOnMissingBean(IPageHandle.class)
    @Bean
    public IPageHandle pageHandle(@Autowired DataSource dataSource) {
        DbType dbType = DbTypeUtils.getDbType(dataSource);
        if (logger.isInfoEnabled()) {
            logger.info("TinyJdbc dbType: {}", dbType.getName());
        }
        IPageHandle pageHandle;
        if (dbType == DbType.MYSQL) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.MARIADB) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.CLICK_HOUSE) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.GBASE) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.OSCAR) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.OCEAN_BASE) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.DERBY) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.CUBRID) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.GOLDILOCKS) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.CSIIDB) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.SAP_HANA) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.OTHER) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType == DbType.DB2) {
            pageHandle = new DB2PageHandleImpl();
        } else if (dbType == DbType.ORACLE) {
            pageHandle = new OraclePageHandleImpl();
        } else if (dbType == DbType.POSTGRE_SQL) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.SQLITE) {
            pageHandle = new SqlitePageHandleImpl();
        } else if (dbType == DbType.GREENPLUM) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.OPENGAUSS) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.KINGBASE_ES) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.HSQL) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.PHOENIX) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.HIGH_GO) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.IMPALA) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.VERTICA) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.REDSHIFT) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.TDENGINE) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.UXDB) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType == DbType.H2) {
            pageHandle = new H2PageHandleImpl();
        } else {
            pageHandle = new MysqlPageHandleImpl();
        }

        GlobalConfig globalConfig = new GlobalConfig();
        /* 获取自定义的ID生成器 */
        this.getBeanThen(IdGeneratorInterface.class, globalConfig::setIdGeneratorInterface);
        GlobalConfigUtils.setGlobalConfig(globalConfig);

        if (logger.isInfoEnabled()) {
            logger.info("TinyJdbc started successfully, version: 1.7.0!");
        }
        return pageHandle;
    }

    /**
     * 根据Class<T>获取Bean
     *
     * @param clazz    Class
     * @param <T>      泛型
     * @param consumer 操作
     */
    public <T> void getBeanThen(Class<T> clazz, Consumer<T> consumer) {
        if (this.applicationContext.getBeanNamesForType(clazz, false, false).length > 0) {
            consumer.accept(this.applicationContext.getBean(clazz));
        }
    }
}

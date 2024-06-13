package org.tinycloud.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tinycloud.jdbc.config.GlobalConfig;
import org.tinycloud.jdbc.config.GlobalConfigUtils;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.SnowflakeConfigInterface;
import org.tinycloud.jdbc.page.*;
import org.tinycloud.jdbc.util.DbType;
import org.tinycloud.jdbc.util.DbTypeUtils;
import org.tinycloud.jdbc.util.TinyJdbcVersion;

import javax.sql.DataSource;
import java.util.function.Consumer;

@Configuration
@EnableConfigurationProperties(TinyJdbcProperties.class)
public class TinyJdbcAutoConfiguration implements ApplicationContextAware {
    final static Logger logger = LoggerFactory.getLogger(TinyJdbcAutoConfiguration.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @ConditionalOnMissingBean(IPageHandle.class)
    @Bean
    public IPageHandle pageHandle(@Autowired DataSource dataSource,
                                  @Autowired TinyJdbcProperties tinyJdbcProperties) {
        DbType dbType = tinyJdbcProperties.getDbType();
        if (dbType == null) {
            dbType = DbTypeUtils.getDbType(dataSource);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Tiny-Jdbc dbType: {}", dbType.getName());
        }
        IPageHandle pageHandle;
        if (dbType.mysqlFamilyType()) {
            pageHandle = new MysqlPageHandleImpl();
        } else if (dbType.oracleFamilyType()) {
            pageHandle = new OraclePageHandleImpl();
        } else if (dbType.postgresqlFamilyType()) {
            pageHandle = new PostgreSqlPageHandleImpl();
        } else if (dbType.oracle12cFamilyType()) {
            pageHandle = new Oracle12cPageHandleImpl();
        } else if (dbType.gBase8sFamilyType()) {
            pageHandle = new GBase8sPageHandleImpl();
        } else if (dbType == DbType.DB2) {
            pageHandle = new DB2PageHandleImpl();
        } else if (dbType == DbType.INFORMIX) {
            pageHandle = new InforMixPageHandleImpl();
        } else if (dbType == DbType.XCLOUD) {
            pageHandle = new XCloudPageHandleImpl();
        } else if (dbType == DbType.TRINO || dbType == DbType.PRESTO) {
            pageHandle = new TrinoPageHandleImpl();
        } else {
            logger.warn("{} database not supported, default to PostgreSql Implements", dbType.getName());
            pageHandle = new PostgreSqlPageHandleImpl();
        }

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(tinyJdbcProperties.getBanner());
        String version = TinyJdbcVersion.getVersion();
        globalConfig.setVersion(version);
        /* 获取自定义的（ID生成器） */
        this.getBeanThen(IdGeneratorInterface.class, globalConfig::setIdGeneratorInterface);
        /* 获取自定义的（雪花算法 workerId 和 datacenterId 配置） */
        this.getBeanThen(SnowflakeConfigInterface.class, globalConfig::setSnowflakeConfigInterface);
        GlobalConfigUtils.setGlobalConfig(globalConfig);

        if (logger.isInfoEnabled()) {
            logger.info("Tiny-Jdbc started successfully, version: {}!", version);
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

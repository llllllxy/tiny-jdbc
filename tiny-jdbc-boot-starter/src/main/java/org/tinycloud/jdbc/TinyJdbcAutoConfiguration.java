package org.tinycloud.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.config.GlobalConfig;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.SnowflakeConfigInterface;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;
import org.tinycloud.jdbc.interceptor.StatInterceptor;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.PageHandleFactory;
import org.tinycloud.jdbc.util.DbType;
import org.tinycloud.jdbc.util.DbTypeUtils;
import org.tinycloud.jdbc.util.TinyJdbcVersion;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.function.Consumer;

@ConditionalOnClass({DataSource.class, JdbcTemplate.class})
@ConditionalOnSingleCandidate(DataSource.class)
@AutoConfigureAfter({JdbcTemplateAutoConfiguration.class})
@Configuration
@EnableConfigurationProperties(TinyJdbcProperties.class)
public class TinyJdbcAutoConfiguration implements ApplicationContextAware, InitializingBean {
    final static Logger logger = LoggerFactory.getLogger(TinyJdbcAutoConfiguration.class);

    private ApplicationContext applicationContext;

    @Autowired
    private TinyJdbcProperties tinyJdbcProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 避免重复初始化
        if (Objects.nonNull(GlobalConfig.getConfig())) {
            return;
        }
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(tinyJdbcProperties.getBanner());
        String version = TinyJdbcVersion.getVersion();
        globalConfig.setVersion(version);
        globalConfig.setDbType(tinyJdbcProperties.getDbType());
        globalConfig.setOpenRuntimeDbType(Objects.isNull(tinyJdbcProperties.getOpenRuntimeDbType()) ? Boolean.FALSE : tinyJdbcProperties.getOpenRuntimeDbType());
        globalConfig.setCloseConn(Objects.isNull(tinyJdbcProperties.getCloseConn()) ? Boolean.TRUE : tinyJdbcProperties.getCloseConn());
        globalConfig.setDatasourceType(tinyJdbcProperties.getDatasourceType());
        /* 获取自定义的（ID生成器） */
        this.getBeanThen(IdGeneratorInterface.class, globalConfig::setIdGeneratorInterface);
        /* 获取自定义的（雪花算法 workerId 和 datacenterId 配置） */
        this.getBeanThen(SnowflakeConfigInterface.class, globalConfig::setSnowflakeConfigInterface);
        GlobalConfig.setConfig(globalConfig);

        if (logger.isInfoEnabled()) {
            logger.info("Tiny-Jdbc started successfully, version: {}.", version);
        }
    }

    @ConditionalOnMissingBean(IPageHandle.class)
    @Bean
    public IPageHandle pageHandle(@Autowired JdbcTemplate jdbcTemplate) {
        DbType dbType = tinyJdbcProperties.getDbType();
        if (dbType == null) {
            dbType = DbTypeUtils.getDbType(jdbcTemplate.getDataSource());
        }
        if (logger.isInfoEnabled()) {
            logger.info("Tiny-Jdbc create bean IPageHandle, dbType: {}.", dbType.getName());
        }
        return PageHandleFactory.createPageHandleByDbType(dbType);
    }


    @ConditionalOnBean({IPageHandle.class, JdbcTemplate.class})
    @Bean
    public JdbcTemplateHelper jdbcTemplateHelper(@Autowired IPageHandle pageHandle,
                                                 @Autowired JdbcTemplate jdbcTemplate) {
        if (logger.isInfoEnabled()) {
            logger.info("Tiny-Jdbc create bean JdbcTemplateHelper.");
        }
        return new JdbcTemplateHelper(jdbcTemplate, pageHandle);
    }

    @ConditionalOnProperty(name = "tiny-jdbc.sql-stat-enabled", havingValue = "true", matchIfMissing = false)
    @Bean
    public SqlInterceptor statInterceptor() {
        if (logger.isInfoEnabled()) {
            logger.info("Tiny-Jdbc create bean StatInterceptor.");
        }
        return new StatInterceptor();
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
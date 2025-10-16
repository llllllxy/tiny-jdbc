package org.tinycloud.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.config.GlobalConfig;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.SnowflakeConfigInterface;
import org.tinycloud.jdbc.util.TinyJdbcVersion;

import java.util.function.Consumer;

@Configuration
@EnableConfigurationProperties(TinyJdbcProperties.class)
public class TinyJdbcAutoConfiguration implements ApplicationContextAware, InitializingBean {
    final static Logger logger = LoggerFactory.getLogger(TinyJdbcAutoConfiguration.class);

    private ApplicationContext applicationContext;

    @Autowired
    private TinyJdbcProperties tinyJdbcProperties; // 注入配置属性

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    // 新增：容器初始化完成后执行全局配置初始化
    @Override
    public void afterPropertiesSet() throws Exception {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(tinyJdbcProperties.getBanner());
        globalConfig.setDbType(tinyJdbcProperties.getDbType());
        String version = TinyJdbcVersion.getVersion();
        globalConfig.setVersion(version);
        /* 获取自定义的（ID生成器） */
        this.getBeanThen(IdGeneratorInterface.class, globalConfig::setIdGeneratorInterface);
        /* 获取自定义的（雪花算法 workerId 和 datacenterId 配置） */
        this.getBeanThen(SnowflakeConfigInterface.class, globalConfig::setSnowflakeConfigInterface);
        GlobalConfig.setConfig(globalConfig);
        if (logger.isInfoEnabled()) {
            logger.info("Tiny-Jdbc started successfully, version: {}!", version);
        }
    }


    @ConditionalOnBean({JdbcTemplate.class})
    @Bean
    public JdbcTemplateHelper jdbcTemplateHelper(@Autowired JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateHelper(jdbcTemplate);
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

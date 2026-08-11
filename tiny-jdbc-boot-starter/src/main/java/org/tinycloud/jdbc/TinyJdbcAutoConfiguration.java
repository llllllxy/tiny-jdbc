package org.tinycloud.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.fill.MetaObjectHandler;
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

/**
 * TinyJDBC 自动配置。
 *
 * <p>Starter 仅负责启用配置绑定并通过构造参数装配运行时 Bean，
 * core 模块不再依赖静态全局配置。</p>
 *
 * @author liuxingyu01
 */
@ConditionalOnClass({DataSource.class, JdbcTemplate.class})
@ConditionalOnSingleCandidate(DataSource.class)
@AutoConfigureAfter({JdbcTemplateAutoConfiguration.class})
@Configuration
@EnableConfigurationProperties(TinyJdbcProperties.class)
public class TinyJdbcAutoConfiguration {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TinyJdbcAutoConfiguration.class);

    /**
     * 创建 TinyJDBC 运行时上下文。
     *
     * @param properties                       TinyJDBC 配置属性
     * @param idGeneratorProvider               自定义 ID 生成器提供者
     * @param snowflakeConfigProvider           雪花 ID 配置提供者
     * @param metaObjectHandlerProvider         自动填充处理器提供者
     * @return 当前应用上下文独占的运行时上下文
     */
    @Bean
    @ConditionalOnMissingBean
    public TinyJdbcRuntime tinyJdbcRuntime(TinyJdbcProperties properties,
                                           ObjectProvider<IdGeneratorInterface> idGeneratorProvider,
                                           ObjectProvider<SnowflakeConfigInterface> snowflakeConfigProvider,
                                           ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider) {
        TinyJdbcRuntime runtime = new TinyJdbcRuntime(Boolean.TRUE.equals(properties.getBanner()),
                TinyJdbcVersion.getVersion(),
                properties.getDbType(),
                Boolean.TRUE.equals(properties.getOpenRuntimeDbType()),
                idGeneratorProvider.getIfAvailable(),
                snowflakeConfigProvider.getIfAvailable(),
                metaObjectHandlerProvider.getIfAvailable());
        if (runtime.isBanner()) {
            runtime.printBanner();
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Tiny-Jdbc started successfully, version: {}.", runtime.getVersion());
        }
        return runtime;
    }

    /**
     * 创建默认分页处理器。
     *
     * @param jdbcTemplate Spring JDBC 模板
     * @param runtime      TinyJDBC 运行时上下文
     * @return 默认分页处理器
     */
    @ConditionalOnMissingBean(IPageHandle.class)
    @Bean
    public IPageHandle pageHandle(JdbcTemplate jdbcTemplate, TinyJdbcRuntime runtime) {
        DbType dbType = runtime.getDbType();
        if (dbType == null) {
            dbType = DbTypeUtils.getDbType(jdbcTemplate.getDataSource());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Tiny-Jdbc create bean IPageHandle, dbType: {}.", dbType.getName());
        }
        return PageHandleFactory.createPageHandleByDbType(dbType);
    }

    /**
     * 创建 JDBC 模板增强工具。
     *
     * @param pageHandle   默认分页处理器
     * @param jdbcTemplate Spring JDBC 模板
     * @param runtime      TinyJDBC 运行时上下文
     * @return JDBC 模板增强工具
     */
    @ConditionalOnBean({IPageHandle.class, JdbcTemplate.class, TinyJdbcRuntime.class})
    @Bean
    public JdbcTemplateHelper jdbcTemplateHelper(IPageHandle pageHandle,
                                                 JdbcTemplate jdbcTemplate,
                                                 TinyJdbcRuntime runtime) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Tiny-Jdbc create bean JdbcTemplateHelper.");
        }
        return new JdbcTemplateHelper(jdbcTemplate, pageHandle, runtime);
    }

    /**
     * 创建 SQL 统计拦截器。
     *
     * @param properties TinyJDBC 配置属性
     * @return SQL 统计拦截器
     */
    @ConditionalOnProperty(name = "tiny-jdbc.sql-stat-enabled", havingValue = "true", matchIfMissing = false)
    @Bean
    public SqlInterceptor statInterceptor(TinyJdbcProperties properties) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Tiny-Jdbc create bean StatInterceptor.");
        }
        return new StatInterceptor(Boolean.TRUE.equals(properties.getSqlStatResultEnabled()));
    }
}

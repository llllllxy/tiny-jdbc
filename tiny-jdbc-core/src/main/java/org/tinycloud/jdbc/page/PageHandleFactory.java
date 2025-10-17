package org.tinycloud.jdbc.page;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.config.GlobalConfig;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.DbType;
import org.tinycloud.jdbc.util.DbTypeUtils;


/**
 * <p>
 *     分页处理器获取工厂类
 * </p>
 *
 * @author liuxingyu01
 * @since 2025/10/16 22:30
 */
public class PageHandleFactory {
    private static final Logger logger = LoggerFactory.getLogger(PageHandleFactory.class);

    /**
     * 根据数据源动态获取对应的分页处理器
     */
    public static IPageHandle getDynamicPageHandle(JdbcTemplate jdbcTemplate) {
        DbType dbType = null;
        try {
            dbType = DbTypeUtils.getDbType(jdbcTemplate.getDataSource());
        } catch (Exception e) {
            // 自动识别失败，使用配置的默认dbType
            dbType = GlobalConfig.getConfig().getDbType();
        }
        // 2. 若自动识别失败且配置未指定，则抛出异常或使用默认实现
        if (dbType == null) {
            throw new TinyJdbcException("Could not identify the database type. Please specify tiny-jdbc.db-type in the configuration.");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Tiny-Jdbc dynamic dbType: {}", dbType.getName());
        }
        return createPageHandleByDbType(dbType);
    }

    /**
     * 根据数据库类型创建对应的分页处理器
     * @param dbType 数据库类型
     * @return 分页处理器IPageHandle
     */
    public static IPageHandle createPageHandleByDbType(DbType dbType) {
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
        } else if (dbType == DbType.GAUSS_DB) {
            pageHandle = new GaussDBPageHandleImpl();
        } else {
            logger.warn("{} database not supported, default to PostgreSQL Implements", dbType.getName());
            pageHandle = new PostgreSqlPageHandleImpl();
        }
        return pageHandle;
    }
}

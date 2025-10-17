package org.tinycloud.jdbc.page.urlparser;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析器注册与调度器(核心逻辑)
 */
public class JdbcUrlResolver {
    // 存储所有已注册的解析器（排除初始化失败的），启动时写，运行时只读，所以线程安全
    private static final List<JdbcUrlParser> PARSERS = new ArrayList<>();

    // 静态代码块：尝试注册所有解析器，忽略因连接池不存在导致的异常
    static {
        // 注册 Hikari 解析器（若类路径不存在 Hikari，实例化会抛异常，直接忽略）
        try {
            PARSERS.add(new HikariJdbcUrlParser());
        } catch (Throwable ignore) {
        }

        // 注册 Druid 解析器
        try {
            PARSERS.add(new DruidJdbcUrlParser());
        } catch (Throwable ignore) {
        }

        // 注册 Tomcat-JDBC 解析器
        try {
            PARSERS.add(new TomcatJdbcUrlParser());
        } catch (Throwable ignore) {
        }

        // 注册 C3P0 解析器
        try {
            PARSERS.add(new C3p0JdbcUrlParser());
        } catch (Throwable ignore) {
        }

        // 注册 DBCP2 解析器
        try {
            PARSERS.add(new Dbcp2JdbcUrlParser());
        } catch (Throwable ignore) {
        }

        // 最后添加兜底解析器（确保始终存在）
        PARSERS.add(new FallbackJdbcUrlParser());
    }

    /**
     * 对外提供的统一方法：获取 jdbcUrl
     */
    public static String getJdbcUrl(DataSource dataSource) {
        if (dataSource == null) {
            throw new TinyJdbcException("DataSource cannot be null!");
        }
        // 遍历所有解析器，找到第一个支持当前数据源的解析器
        for (JdbcUrlParser parser : PARSERS) {
            if (parser.supports(dataSource)) {
                return parser.getJdbcUrl(dataSource);
            }
        }
        // 理论上不会走到这里，因为兜底解析器始终返回 true
        throw new TinyJdbcException("No suitable parser found for DataSource: " + dataSource.getClass().getName());
    }
}

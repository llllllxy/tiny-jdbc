package org.tinycloud.jdbc.page.urlparser;

import org.tinycloud.jdbc.config.GlobalConfig;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.StrUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析器注册与调度器(核心逻辑)
 */
public class JdbcUrlResolver {
    // 存储所有已注册的解析器（排除初始化失败的），启动时写，运行时只读，所以线程安全
    private static final List<JdbcUrlParser<?>> PARSERS = new ArrayList<>();

    // 存储所有已注册的解析器Class，用于配置datasourceType了时的反射对象创建
    private static final Map<String, Class<? extends JdbcUrlParser<?>>> PARSER_MAP = new LinkedHashMap<>();
    // 定义静态缓存Map（key: datasourceType，value: JdbcUrlParser实例）
    private static final ConcurrentHashMap<String, JdbcUrlParser<?>> PARSER_INSTANCE_CACHE = new ConcurrentHashMap<>();


    // 静态代码块
    static {
        // 尝试注册所有解析器，忽略因连接池不存在导致的异常
        try {
            PARSERS.add(new DruidJdbcUrlParser());
        } catch (Throwable ignore) {
        }
        try {
            PARSERS.add(new HikariJdbcUrlParser());
        } catch (Throwable ignore) {
        }
        try {
            PARSERS.add(new BeecpJdbcUrlParser());
        } catch (Throwable ignore) {
        }
        try {
            PARSERS.add(new TomcatJdbcUrlParser());
        } catch (Throwable ignore) {
        }
        try {
            PARSERS.add(new C3p0JdbcUrlParser());
        } catch (Throwable ignore) {
        }
        try {
            PARSERS.add(new Dbcp2JdbcUrlParser());
        } catch (Throwable ignore) {
        }
        // 最后添加兜底解析器（确保始终存在）
        PARSERS.add(new FallbackJdbcUrlParser());


        PARSER_MAP.put("hikari", HikariJdbcUrlParser.class);
        PARSER_MAP.put("druid", DruidJdbcUrlParser.class);
        PARSER_MAP.put("tomcat-jdbc", TomcatJdbcUrlParser.class);
        PARSER_MAP.put("c3p0", C3p0JdbcUrlParser.class);
        PARSER_MAP.put("dbcp2", Dbcp2JdbcUrlParser.class);
        PARSER_MAP.put("beecp", BeecpJdbcUrlParser.class);
        PARSER_MAP.put("default", FallbackJdbcUrlParser.class);
    }

    /**
     * 根据给定的数据源对象获取其对应的 JDBC URL。
     * 1、若指定了数据源类型datasourceType，则直接获取该类型的解析器
     * 2、若未指定具体数据源类型，则遍历所有已知解析器，查找第一个支持当前数据源的解析器
     *
     * @param dataSource 数据源对象，不能为 null
     * @return 返回该数据源对应的 JDBC URL 字符串
     * @throws TinyJdbcException 当数据源为 null、找不到合适的解析器或解析过程中发生错误时抛出异常
     */
    public static String getJdbcUrl(DataSource dataSource) {
        if (dataSource == null) {
            throw new TinyJdbcException("DataSource cannot be null!");
        }
        String datasourceType = GlobalConfig.getConfig().getDatasourceType();
        if (StrUtils.isNotEmpty(datasourceType)) {
            try {
                // 先从缓存获取，若不存在则创建并缓存
                JdbcUrlParser<?> parser = PARSER_INSTANCE_CACHE.computeIfAbsent(datasourceType, type -> {
                    try {
                        Class<? extends JdbcUrlParser<?>> parserClass;
                        if (PARSER_MAP.containsKey(type)) {
                            parserClass = PARSER_MAP.get(type);
                        } else {
                            parserClass = (Class<JdbcUrlParser<?>>) Class.forName(type);
                        }
                        return parserClass.newInstance(); // 创建实例
                    } catch (Exception e) {
                        // 包装为运行时异常抛出（computeIfAbsent不允许受检异常）
                        throw new RuntimeException(e);
                    }
                });
                return parser.resolveJdbcUrl(dataSource);
            } catch (RuntimeException e) {
                // 还原原始异常信息
                Throwable cause = e.getCause();
                if (cause instanceof ClassNotFoundException) {
                    throw new TinyJdbcException("Make sure that the datasource implementation class (" + datasourceType + ") exists!", cause);
                } else if (cause instanceof Exception) {
                    throw new TinyJdbcException(datasourceType + " must provide a no-arg constructor", cause);
                } else {
                    throw new TinyJdbcException("Failed to get parser for " + datasourceType, e);
                }
            }
        } else {
            // 遍历所有解析器，找到第一个支持当前数据源的解析器
            for (JdbcUrlParser<?> parser : PARSERS) {
                return parser.resolveJdbcUrl(dataSource);
            }
            // 理论上不会走到这里，因为兜底解析器始终返回 true
            throw new TinyJdbcException("No suitable parser found for DataSource: " + dataSource.getClass().getName());
        }
    }
}

package org.tinycloud.jdbc.p6spy;

import com.p6spy.engine.common.P6Util;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

/**
 * <p>
 * 自定义p6spy日志打印格式
 * 该类实现了 MessageFormattingStrategy 接口，用于自定义 p6spy 框架的 SQL 日志输出格式。
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-2024/4/21 23:10
 */
public class P6SpyLogger implements MessageFormattingStrategy {

    /**
     * 格式化 SQL 执行信息为自定义的日志格式。
     *
     * @param connectionId 数据库连接的唯一标识符
     * @param now          当前时间的字符串表示
     * @param elapsed      SQL 执行所花费的时间，单位为毫秒
     * @param category     SQL 操作的类别，例如 "statement"、"resultset" 等
     * @param prepared     预编译的 SQL 语句
     * @param sql          最终执行的 SQL 语句
     * @param url          数据库连接的 URL
     * @return 格式化后的日志信息字符串
     */
    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        StringBuilder sb = new StringBuilder();
        sb.append("==========================================================================================================\n")
                .append("连接id：").append(connectionId).append("\n")
                .append("当前时间：").append(now).append("\n")
                .append("类别：").append(category).append("\n")
                .append("花费时间：").append(elapsed).append("\n")
                .append("url：").append(url).append("\n")
                .append("预编译sql：").append(P6Util.singleLine(prepared)).append("\n")
                .append("最终执行sql：").append(P6Util.singleLine(sql)).append("\n")
                .append("==========================================================================================================\n");
        return sb.toString();
    }
}

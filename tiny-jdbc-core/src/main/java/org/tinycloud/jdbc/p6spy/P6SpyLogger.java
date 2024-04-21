package org.tinycloud.jdbc.p6spy;

import com.p6spy.engine.common.P6Util;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

/**
 * <p>
 * 自定义p6spy日志打印格式
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-2024/4/21 23:10
 */
public class P6SpyLogger implements MessageFormattingStrategy {
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

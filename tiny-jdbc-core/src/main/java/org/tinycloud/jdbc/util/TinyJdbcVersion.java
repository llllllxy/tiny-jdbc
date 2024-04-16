package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * <p>
 * 获取版本号
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-16 11:11
 */
public class TinyJdbcVersion {
    private TinyJdbcVersion() {
    }

    public static String getVersion() {
        String appVersion = "";
        Properties properties = new Properties();
        try (InputStream stream = TinyJdbcVersion.class.getClassLoader().getResourceAsStream("core.properties")) {
            properties.load(stream);
            if (!properties.isEmpty()) {
                appVersion = properties.getProperty("core.version");
            }
        } catch (IOException e) {
            throw new TinyJdbcException("getVersion failed: ", e);
        }
        return appVersion;
    }

}

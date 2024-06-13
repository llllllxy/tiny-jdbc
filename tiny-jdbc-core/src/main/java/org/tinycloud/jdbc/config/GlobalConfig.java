package org.tinycloud.jdbc.config;

import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.SnowflakeConfigInterface;

import java.io.Serializable;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-2024/4/15 23:33
 */
public class GlobalConfig implements Serializable {
    /**
     * 是否开启 LOGO 打印
     */
    private boolean banner = true;

    private String version;

    /**
     * 主键生成器
     */
    private IdGeneratorInterface idGeneratorInterface;

    /**
     * 雪花算法 workerId 和 datacenterId 配置
     */
    private SnowflakeConfigInterface snowflakeConfigInterface;

    public boolean isBanner() {
        return banner;
    }

    public void setBanner(boolean banner) {
        this.banner = banner;
    }

    public IdGeneratorInterface getIdGeneratorInterface() {
        return idGeneratorInterface;
    }

    public void setIdGeneratorInterface(IdGeneratorInterface idGeneratorInterface) {
        this.idGeneratorInterface = idGeneratorInterface;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public SnowflakeConfigInterface getSnowflakeConfigInterface() {
        return snowflakeConfigInterface;
    }

    public void setSnowflakeConfigInterface(SnowflakeConfigInterface snowflakeConfigInterface) {
        this.snowflakeConfigInterface = snowflakeConfigInterface;
    }
}

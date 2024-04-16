package org.tinycloud.jdbc.config;

import org.tinycloud.jdbc.id.IdGeneratorInterface;

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
}

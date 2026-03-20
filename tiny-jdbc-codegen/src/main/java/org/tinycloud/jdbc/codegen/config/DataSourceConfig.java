package org.tinycloud.jdbc.codegen.config;

public class DataSourceConfig {

    private final String url;
    private final String username;
    private final String password;
    private final String driverClassName;

    // 私有构造方法，只能通过 Builder 创建
    private DataSourceConfig(Builder builder) {
        this.url = builder.url;
        this.username = builder.username;
        this.password = builder.password;
        this.driverClassName = builder.driverClassName;
    }

    // 对外入口
    public static Builder builder() {
        return new Builder();
    }

    // ===== Getter =====
    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    // ===== Builder 内部类 =====
    public static class Builder {
        private String url;
        private String username;
        private String password;
        private String driverClassName;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        // 自动根据 URL 推断 driver
        private void resolveDriver() {
            if (this.driverClassName != null) {
                return;
            }
            if (url == null) {
                return;
            }
            if (url.contains("mysql")) {
                this.driverClassName = "com.mysql.cj.jdbc.Driver";
            } else if (url.contains("postgresql")) {
                this.driverClassName = "org.postgresql.Driver";
            } else if (url.contains("oracle")) {
                this.driverClassName = "oracle.jdbc.OracleDriver";
            } else if (url.contains("sqlserver")) {
                this.driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            }
        }

        // 核心 build 方法
        public DataSourceConfig build() {
            // 基础校验
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("url 不能为空");
            }
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("username 不能为空");
            }

            // 自动补 driver
            resolveDriver();

            if (driverClassName == null || driverClassName.isEmpty()) {
                throw new IllegalArgumentException("无法根据 url 识别 driverClassName，请手动指定");
            }

            return new DataSourceConfig(this);
        }
    }
}
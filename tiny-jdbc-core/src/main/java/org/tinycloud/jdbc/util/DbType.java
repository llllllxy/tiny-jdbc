package org.tinycloud.jdbc.util;

/**
 * 数据库类型枚举
 *
 * @author liuxingyu01
 * @since 2023-07-31-16:52
 **/
public enum DbType {
    /**
     * MYSQL
     */
    MYSQL("mysql", "MySql 数据库"),
    /**
     * MARIADB
     */
    MARIADB("mariadb", "MariaDB 数据库"),
    /**
     * ORACLE
     */
    ORACLE("oracle", "Oracle11g 及以下数据库"),
    /**
     * oracle12c
     */
    ORACLE_12C("oracle12c", "Oracle12c 及以上数据库"),
    /**
     * DB2
     */
    DB2("db2", "DB2 数据库"),
    /**
     * H2
     */
    H2("h2", "H2 数据库"),
    /**
     * HSQL
     */
    HSQL("hsql", "HSQL 数据库"),
    /**
     * SQLITE
     */
    SQLITE("sqlite", "SQLite 数据库"),
    /**
     * POSTGRE
     */
    POSTGRE_SQL("postgresql", "PostgreSQL 数据库"),
    /**
     * SQLSERVER
     */
    SQLSERVER("sqlserver", "SQLServer 数据库"),
    /**
     * SqlServer 2005 数据库
     */
    SQLSERVER_2005("sqlserver_2005", "SQLServer 数据库"),
    /**
     * DM
     */
    DM("dm", "达梦数据库"),
    /**
     * xugu
     */
    XUGU("xugu", "虚谷数据库"),
    /**
     * Kingbase
     */
    KINGBASE_ES("kingbasees", "人大金仓数据库"),
    /**
     * Phoenix
     */
    PHOENIX("phoenix", "Phoenix HBase 数据库"),
    /**
     * Gauss（低版本为 zenith，为贡献者提供，非标准官方驱动）
     */
    GAUSS("gauss", "Gauss 数据库"),
    /**
     * 华为云GaussDB数据库
     *
     */
    GAUSS_DB("gaussDB", "GaussDB 数据库"),
    /**
     * ClickHouse
     */
    CLICK_HOUSE("clickhouse", "clickhouse 数据库"),
    /**
     * GBase
     */
    GBASE("gbase", "南大通用(华库)数据库"),
    /**
     * GBase-8s
     */
    GBASE_8S("gbase-8s", "南大通用数据库 GBase 8s"),
    /**
     * GBase8sPG
     */
    GBASE8S_PG("gbase8s-pg", "南大通用数据库 GBase 8s兼容pg"),
    /**
     * GBase8s
     */
    @Deprecated
    GBASE_INFORMIX("gbase 8s", "南大通用数据库 GBase 8s"),
    /**
     * gbasedbt
     */
    @Deprecated
    GBASEDBT("gbasedbt", "南大通用数据库"),
    /**
     * GBase8c
     */
    GBASE_8C("gbase-8c", "南大通用数据库 GBase 8c"),
    /**
     * Oscar
     */
    OSCAR("oscar", "神通数据库"),
    /**
     * Sybase
     */
    SYBASE("sybase", "Sybase ASE 数据库"),
    /**
     * OceanBase
     */
    OCEAN_BASE("oceanbase", "OceanBase 数据库"),
    /**
     * Firebird
     */
    FIREBIRD("Firebird", "Firebird 数据库"),
    /**
     * derby
     */
    DERBY("derby", "Derby 数据库"),
    /**
     * HighGo
     */
    HIGH_GO("highgo", "瀚高数据库"),
    /**
     * CUBRID
     */
    CUBRID("cubrid", "CUBRID 数据库"),
    /**
     * SUNDB
     */
    SUNDB("sundb", "SUNDB 数据库"),
    /**
     * GOLDILOCKS
     */
    GOLDILOCKS("goldilocks", "GOLDILOCKS 数据库"),
    /**
     * CSIIDB
     */
    CSIIDB("csiidb", "CSIIDB 数据库"),
    /**
     * SAP_HANA
     */
    SAP_HANA("hana", "SAP_HANA 数据库"),
    /**
     * Impala
     */
    IMPALA("impala", "impala 数据库"),
    /**
     * Vertica
     */
    VERTICA("vertica", "vertica数据库"),
    /**
     * 东方国信 xcloud
     */
    XCLOUD("xcloud", "行云数据库"),
    /**
     * redshift
     */
    REDSHIFT("redshift", "亚马逊 redshift 数据库"),
    /**
     * openGauss
     */
    OPENGAUSS("openGauss", "华为 openGauss 数据库"),
    /**
     * TDengine
     */
    TDENGINE("TDengine", "TDengine 数据库"),
    /**
     * Informix
     */
    INFORMIX("informix", "Informix 数据库"),
    /**
     * sinodb
     */
    SINODB("sinodb", "SinoDB 数据库"),
    /**
     * uxdb
     */
    UXDB("uxdb", "优炫数据库"),
    /**
     * greenplum
     */
    GREENPLUM("greenplum", "greenplum 数据库"),
    /**
     * trino
     */
    TRINO("trino", "trino数据库"),
    /**
     * lealone
     */
    LEALONE("lealone", "Lealone数据库"),
    /**
     * trino
     */
    PRESTO("Presto", "Presto数据库"),
    /**
     * goldendb
     */
    GOLDENDB("goldendb", "GoldenDB数据库"),
    /**
     * yasdb
     */
    YASDB("yasdb", "崖山数据库"),
    /**
     * vastbase
     */
    VASTBASE("vastbase", "Vastbase数据库"),
    /**
     * duckdb
     */
    DUCKDB("duckdb", "duckdb数据库"),
    /**
     * hive2
     */
    HIVE2("hive2", "Hadoop数据仓库"),
    /**
     * UNKNOWN DB
     */
    OTHER("other", "其他数据库");

    /**
     * 数据库名称
     */
    private final String name;

    /**
     * 描述
     */
    private final String remarks;


    public String getName() {
        return name;
    }

    public String getRemarks() {
        return remarks;
    }

    DbType(String name, String remarks) {
        this.name = name;
        this.remarks = remarks;
    }

    /**
     * 获取数据库类型
     *
     * @param dbType 数据库类型字符串
     */
    public static DbType getDbType(String dbType) {
        for (DbType type : DbType.values()) {
            if (type.name.equalsIgnoreCase(dbType)) {
                return type;
            }
        }
        return OTHER;
    }

    public boolean mysqlFamilyType() {
        return this == DbType.MYSQL
                || this == DbType.MARIADB
                || this == DbType.GBASE
                || this == DbType.OSCAR
                || this == DbType.XUGU
                || this == DbType.CLICK_HOUSE
                || this == DbType.OCEAN_BASE
                || this == DbType.CUBRID
                || this == DbType.GOLDILOCKS
                || this == DbType.CSIIDB
                || this == DbType.SUNDB
                || this == DbType.GOLDENDB
                || this == DbType.YASDB;
    }

    public boolean oracleFamilyType() {
        return this == DbType.ORACLE
                || this == DbType.DM
                || this == DbType.GAUSS;
    }

    public boolean oracle12cFamilyType() {
        return this == DbType.ORACLE_12C
                || this == DbType.FIREBIRD
                || this == DbType.SQLSERVER
                || this == DbType.DERBY;
    }

    public boolean gBase8sFamilyType() {
        return this == DbType.GBASE_8S
                || this == DbType.GBASEDBT
                || this == DbType.GBASE_INFORMIX
                || this == DbType.SINODB;
    }

    public boolean postgresqlFamilyType() {
        return this == DbType.POSTGRE_SQL
                || this == DbType.H2
                || this == DbType.LEALONE
                || this == DbType.SQLITE
                || this == DbType.HSQL
                || this == DbType.KINGBASE_ES
                || this == DbType.PHOENIX
                || this == DbType.SAP_HANA
                || this == DbType.IMPALA
                || this == DbType.HIGH_GO
                || this == DbType.VERTICA
                || this == DbType.REDSHIFT
                || this == DbType.OPENGAUSS
                || this == DbType.TDENGINE
                || this == DbType.UXDB
                || this == DbType.GBASE8S_PG
                || this == DbType.GREENPLUM
                || this == DbType.GBASE_8C
                || this == DbType.DUCKDB
                || this == DbType.VASTBASE;
    }
}

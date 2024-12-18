package org.tinycloud.jdbc.id;

/**
 * <p>
 *     自定义数据中心和机器ID 提供者载体
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-12-18 18:10
 */
public class DatacenterAndWorkerProvider {
    private Integer datacenterId;

    private Integer workerId;

    public Integer getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(Integer datacenterId) {
        this.datacenterId = datacenterId;
    }

    public Integer getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Integer workerId) {
        this.workerId = workerId;
    }
}

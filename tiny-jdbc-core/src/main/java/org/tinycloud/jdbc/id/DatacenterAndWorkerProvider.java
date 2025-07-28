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

    public DatacenterAndWorkerProvider() {
    }

    public DatacenterAndWorkerProvider(Long datacenterId, Long workerId) {
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    private Long datacenterId;

    private Long workerId;

    public Long getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(Long datacenterId) {
        this.datacenterId = datacenterId;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }
}

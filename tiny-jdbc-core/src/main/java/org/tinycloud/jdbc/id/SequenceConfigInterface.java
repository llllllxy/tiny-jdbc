package org.tinycloud.jdbc.id;

import org.tinycloud.jdbc.util.Pair;

/**
 * <p>
 * 雪花ID自定义workerId和datacenterId
 * 避免使用网络计算的导致的重复
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-05-30 11:46
 */
public interface SequenceConfigInterface {

    /**
     * 获取datacenterId和workerId
     * Pair对象左边为datacenterId，右边为workerId
     *
     * @return datacenterId和workerId
     */
    Pair<Long, Long> getDatacenterIdAndWorkerId();
}

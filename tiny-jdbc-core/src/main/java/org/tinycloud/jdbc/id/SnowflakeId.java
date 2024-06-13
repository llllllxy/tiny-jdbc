package org.tinycloud.jdbc.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.ThreadLocalRandom;


/**
 * 雪花算法ID
 * <ul>
 *      <li>最高 1 位固定值 0，因为生成的 ID 是正整数；
 *      <li>接下来 41 位存储毫秒级时间戳，2 ^ 41 / ( 1000 * 60 * 60 * 24 * 365) = 69，大概可以使用 69 年；
 *      <li>再接下 10 位存储机器码，包括 5 位 dataCenterId 和 5 位 workerId，最多可以部署 2 ^ 10 = 1024 台机器；
 *      <li>最后 12 位存储序列号，同一毫秒时间戳时，通过这个递增的序列号来区分，即对于同一台机器而言，同一毫秒时间戳下，可以生成 2 ^ 12 = 4096 个不重复 ID。
 * </ul>
 * <br/>
 * 优化自开源项目：https://gitee.com/yu120/sequence
 *
 * @author liuxingyu01
 * @version 2023-07-26 15:11:53
 */
public class SnowflakeId {
    private static final Logger logger = LoggerFactory.getLogger(SnowflakeId.class);

    /*
     * 时间起始标记点，作为基准，一般取系统的最近时间（一旦确定不能变动）
     */
    private static final long TWEPOCH = 1288834974657L;
    /*
     * 机器标识位数（5bit）
     */
    private static final long WORKER_ID_BITS = 5L;
    /*
     * 数据中心标识位数（5bit）
     */
    private static final long DATACENTER_ID_BITS = 5L;
    /*
     * 工作机器ID最大值 31（0-31）
     */
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);
    /*
     * 数据中心ID最大值 31（0-31）
     */
    private static final long MAX_DATACENTER_ID = -1L ^ (-1L << DATACENTER_ID_BITS);
    /*
     * 毫秒内自增位
     */
    private static final long SEQUENCE_BITS = 12L;
    /*
     * 机器ID偏左移12位
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    /*
     * 时间戳左移动位
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);

    /*
     * 可容忍的时间偏移量
     */
    private static final long OFFSET_PERIOD = 5L;

    /*
     * 机器 ID 部分
     */
    private final long workerId;
    /*
     * 数据标识 ID 部分
     */
    private final long datacenterId;
    /*
     * 并发控制
     */
    private long sequence = 0L;
    /*
     * 上次生产 ID 时间戳
     */
    private long lastTimestamp = -1L;
    /*
     * IP 地址
     */
    private InetAddress inetAddress;

    /**
     * 有参构造器
     *
     * @param inetAddress InetAddress对象
     */
    public SnowflakeId(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
        this.datacenterId = getDatacenterId(MAX_DATACENTER_ID);
        this.workerId = getWorkerId(datacenterId, MAX_WORKER_ID);
        this.printLog();
    }

    /**
     * 有参构造器
     *
     * @param workerId     工作机器 ID
     * @param datacenterId 序列号
     */
    public SnowflakeId(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("Datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.printLog();
    }

    /**
     * 获取 maxWorkerId
     */
    protected long getWorkerId(long datacenterId, long maxWorkerId) {
        StringBuilder mpId = new StringBuilder();
        mpId.append(datacenterId);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (name != null && !name.isEmpty()) {
            // GET jvmPid
            mpId.append(name.split("@")[0]);
        }
        // MAC + PID 的 hashcode 获取16个低位
        return (mpId.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    /**
     * 数据标识id部分
     */
    protected long getDatacenterId(long maxDatacenterId) {
        long id = 0L;
        try {
            if (null == this.inetAddress) {
                this.inetAddress = InetAddress.getLocalHost();
            }
            NetworkInterface network = NetworkInterface.getByInetAddress(this.inetAddress);
            if (null == network) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long) mac[mac.length - 2]) | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            }
        } catch (Exception e) {
            logger.warn("getDatacenterId error: " + e.getMessage());
        }
        return id;
    }

    /**
     * 获取下一个 ID
     *
     * @return 下一个 ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        // 闰秒
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= OFFSET_PERIOD) {
                try {
                    // 休眠双倍差值后重新获取，再次校验
                    wait(offset << 1L);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
            }
        }

        if (lastTimestamp == timestamp) {
            // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 同一毫秒的序列数已经达到最大
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒内，序列号置为 1 - 2 随机数
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }

        lastTimestamp = timestamp;

        // 时间戳部分 | 数据中心部分 | 机器标识部分 | 序列号部分
        return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 反解id的时间戳部分
     */
    public static long parseIdTimestamp(long id) {
        return (id >> 22) + TWEPOCH;
    }

    private void printLog() {
        if (logger.isInfoEnabled()) {
            logger.info("Initialization SnowflakeId network:" + inetAddress.getHostName() + "/" + inetAddress.getHostAddress());
            logger.info("Initialization SnowflakeId datacenterId:" + this.datacenterId + " workerId:" + this.workerId);
        }
    }
}

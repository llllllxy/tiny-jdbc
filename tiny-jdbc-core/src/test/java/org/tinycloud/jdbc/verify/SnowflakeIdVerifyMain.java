package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.id.SnowflakeId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * SnowflakeId 的测试：parseIdTimestamp 无符号右移、节点 ID 边界校验。
 */
public class SnowflakeIdVerifyMain {

    // 验证：parseIdTimestamp 使用无符号右移，符号位被占（负 ID）时仍能正确反解时间戳
    @Test
    public void testParseIdTimestampWithNegativeId() {
        // TWEPOCH 与 SnowflakeId 一致
        final long TWEPOCH = 1288834974657L;
        // 超出 41 位时间范围，左移 22 位后占用符号位，ID 变为负数
        long ts = TWEPOCH + (1L << 41);
        long id = ((ts - TWEPOCH) << 22) | 1L;
        assertTrue("id should be negative for out-of-range timestamp", id < 0);
        assertEquals(ts, SnowflakeId.parseIdTimestamp(id));
    }

    // 验证：workerId 超出范围时，构造器显式抛出 IllegalArgumentException
    @Test
    public void testWorkerIdOutOfRangeThrows() {
        try {
            new SnowflakeId(32L, 1L);
            fail("expected IllegalArgumentException for workerId 32");
        } catch (IllegalArgumentException e) {
            // 符合预期
        }
    }
}

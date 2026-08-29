package org.tinycloud.jdbc.id.generator;

import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.SnowflakeId;

/**
 * <p>
 *     雪花 ID 生成器（服务于 {@link org.tinycloud.jdbc.annotation.IdType#ASSIGN_ID}）。
 * </p>
 * <p>
 *     包装一个 {@link SnowflakeId} 实例，生成 19 位长整型雪花 ID。
 * </p>
 *
 * @author liuxingyu01
 */
public class SnowflakeIdGenerator implements IdGeneratorInterface {

    /**
     * 底层雪花算法生成器。
     */
    private final SnowflakeId snowflakeId;

    public SnowflakeIdGenerator(SnowflakeId snowflakeId) {
        this.snowflakeId = snowflakeId;
    }

    @Override
    public Object nextId(Object entity) {
        // 雪花 ID 生成不依赖实体信息，忽略 entity
        return this.snowflakeId.nextId();
    }
}

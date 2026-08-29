package org.tinycloud.jdbc.id.generator;

import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.NanoId;

/**
 * <p>
 *     NanoId 生成器（服务于 {@link org.tinycloud.jdbc.annotation.IdType#NANO_ID}）。
 * </p>
 * <p>
 *     生成 21 位 url-safe 的 NanoId 字符串（基于 {@link NanoId}）。
 * </p>
 *
 * @author liuxingyu01
 */
public class NanoIdGenerator implements IdGeneratorInterface {

    @Override
    public Object nextId(Object entity) {
        // NanoId 生成不依赖实体信息，忽略 entity
        return NanoId.INSTANCE.randomNanoId();
    }
}

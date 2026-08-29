package org.tinycloud.jdbc.id;

import java.util.UUID;

/**
 * <p>
 *     UUID 生成器（服务于 {@link org.tinycloud.jdbc.annotation.IdType#UUID}）。
 * </p>
 * <p>
 *     生成 32 位无连字符的 UUID 字符串。
 * </p>
 *
 * @author liuxingyu01
 */
public class UuidGenerator implements IdGeneratorInterface {

    @Override
    public Object nextId(Object entity) {
        // 去掉连字符，生成 32 位字符串
        return UUID.randomUUID().toString().replace("-", "");
    }
}

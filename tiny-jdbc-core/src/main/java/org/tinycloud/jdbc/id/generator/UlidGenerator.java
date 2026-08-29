package org.tinycloud.jdbc.id.generator;

import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.id.UlidCreator;

/**
 * <p>
 *     ULID 生成器（服务于 {@link org.tinycloud.jdbc.annotation.IdType#ULID}）。
 * </p>
 * <p>
 *     生成 26 位、按字典序可排序的 ULID 字符串（基于 {@link UlidCreator#getMonotonicUlid()}，
 *     同一毫秒内单调递增，对主键索引友好）。
 * </p>
 *
 * @author liuxingyu01
 */
public class UlidGenerator implements IdGeneratorInterface {

    @Override
    public Object nextId(Object entity) {
        // 单调 ULID 生成由 UlidCreator 全局单例 + 内部锁保证线程安全，忽略 entity
        return UlidCreator.getMonotonicUlid().toString();
    }
}

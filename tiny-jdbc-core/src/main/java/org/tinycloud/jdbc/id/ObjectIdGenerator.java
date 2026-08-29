package org.tinycloud.jdbc.id;

/**
 * <p>
 *     ObjectId 生成器（服务于 {@link org.tinycloud.jdbc.annotation.IdType#OBJECT_ID}）。
 * </p>
 * <p>
 *     生成 24 位十六进制 MongoDB 风格 ObjectId 字符串。
 * </p>
 *
 * @author liuxingyu01
 */
public class ObjectIdGenerator implements IdGeneratorInterface {

    @Override
    public Object nextId(Object entity) {
        // ObjectId 生成不依赖实体信息，忽略 entity
        return ObjectId.nextId();
    }
}

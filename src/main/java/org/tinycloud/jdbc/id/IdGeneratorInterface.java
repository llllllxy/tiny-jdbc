package org.tinycloud.jdbc.id;

/**
 * <p>
 * 自定义ID生成器-接口
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-03-05 15:00
 */
public interface IdGeneratorInterface {

    Object nextId(Object entity);
}

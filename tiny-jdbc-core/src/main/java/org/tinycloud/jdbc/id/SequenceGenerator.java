package org.tinycloud.jdbc.id;

import org.tinycloud.jdbc.exception.TinyJdbcException;

/**
 * <p>
 *     序列生成器（服务于 {@link org.tinycloud.jdbc.annotation.IdType#SEQUENCE}）。
 * </p>
 * <p>
 *     通过 {@link IdContext} 携带的 {@code sequenceSql} 与 {@code jdbcTemplate} 执行数据库序列查询，
 *     得到下一个序列值。与前几个生成器不同，它需要实体/字段上下文（序列脚本、字段类型、JdbcTemplate），
 *     因此覆盖 {@link IdGeneratorInterface#nextId(IdContext)}。
 * </p>
 *
 * @author liuxingyu01
 */
public class SequenceGenerator implements IdGeneratorInterface {

    @Override
    public Object nextId(Object entity) {
        throw new UnsupportedOperationException("SequenceGenerator requires IdContext (sequenceSql & jdbcTemplate).");
    }

    @Override
    public Object nextId(IdContext context) {
        if (context.getJdbcTemplate() == null) {
            throw new TinyJdbcException("JdbcTemplate is required for SEQUENCE id generation.");
        }
        return context.getJdbcTemplate().queryForObject(context.getSequenceSql(), context.getFieldType());
    }
}

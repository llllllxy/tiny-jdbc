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

    /**
     * 生成下一个主键 ID。
     *
     * @param entity 实体对象（生成器可按需忽略）
     * @return 生成的主键值
     */
    Object nextId(Object entity);

    /**
     * 生成下一个主键 ID（带完整上下文）。
     * <p>
     *     默认实现委托给 {@link #nextId(Object)}，以兼容仅实现旧签名 {@code nextId(Object)} 的自定义生成器。
     *     需要额外上下文（如 {@link org.tinycloud.jdbc.annotation.IdType#SEQUENCE} 的序列脚本与 {@code JdbcTemplate}）
     *     的实现可覆盖此方法。
     * </p>
     * <p>
     *     注意：{@code context} 允许为 {@code null}，此时按空实体委托给 {@link #nextId(Object)}。
     *     实现类内部若需要调用本接口且不关心上下文，必须写成 {@code nextId((Object) null)} 等显式转型形式，
     *     不要直接写 {@code nextId(null)}——{@code IdContext} 比 {@code Object} 更具体，{@code null} 字面量会
     *     优先选中本方法，从而造成无限递归。
     * </p>
     *
     * @param context 主键生成上下文，可为 {@code null}
     * @return 生成的主键值
     */
    default Object nextId(IdContext context) {
        // context 为空表示调用方不提供上下文，按空实体委托给旧签名方法
        Object entity = context == null ? null : context.getObj();
        return nextId(entity);
    }
}

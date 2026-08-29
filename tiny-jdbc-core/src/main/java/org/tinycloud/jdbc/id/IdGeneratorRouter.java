package org.tinycloud.jdbc.id;

import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.ConvertUtils;

import java.util.EnumMap;
import java.util.Map;

/**
 * <p>
 *     主键生成路由：按 {@link IdType} 选择对应的 {@link IdGeneratorInterface} 实现来生成主键，
 *     并统一完成类型校验、类型转换与回写实体字段。
 * </p>
 * <p>
 *     <ul>
 *         <li>内置策略（OBJECT_ID / UUID / ASSIGN_ID / SEQUENCE）由框架提供实现类；</li>
 *         <li>{@link IdType#CUSTOM} 复用用户通过 {@link TinyJdbcRuntime} 注册的 {@link IdGeneratorInterface}；</li>
 *         <li>{@link IdType#AUTO_INCREMENT} 返回 null（跳过该列）；{@link IdType#INPUT} 要求调用方预赋值。</li>
 *     </ul>
 * </p>
 *
 * @author liuxingyu01
 */
public class IdGeneratorRouter {

    /**
     * 内置生成器映射。
     */
    private final Map<IdType, IdGeneratorInterface> builtin = new EnumMap<>(IdType.class);

    /**
     * 运行时上下文（提供自定义生成器 / 雪花 ID）。
     */
    private final TinyJdbcRuntime tinyJdbcRuntime;

    public IdGeneratorRouter(TinyJdbcRuntime tinyJdbcRuntime) {
        this.tinyJdbcRuntime = tinyJdbcRuntime;
        this.builtin.put(IdType.OBJECT_ID, new ObjectIdGenerator());
        this.builtin.put(IdType.UUID, new UuidGenerator());
        this.builtin.put(IdType.ASSIGN_ID, new SnowflakeIdGenerator(tinyJdbcRuntime.getSnowflakeId()));
        this.builtin.put(IdType.SEQUENCE, new SequenceGenerator());
    }

    /**
     * 根据上下文的策略生成主键值并回写实体字段，返回最终主键值。
     *
     * @param context 主键生成上下文
     * @return 最终主键值；{@link IdType#AUTO_INCREMENT} 时返回 null
     */
    public Object generate(IdContext context) {
        IdType idType = context.getIdType();
        if (idType == null || idType == IdType.AUTO_INCREMENT) {
            // 自增主键：跳过该列
            return null;
        }
        if (idType == IdType.INPUT) {
            throw new TinyJdbcException("INPUT primary key [" + context.getFieldName() + "] must be set by the caller before insert.");
        }

        IdGeneratorInterface generator = resolveGenerator(idType);
        Object id = generator.nextId(context);
        Object converted = convert(context, id);
        setField(context, converted);
        return converted;
    }

    /**
     * 根据策略解析生成器。CUSTOM 取用户的生成器，其余取内置实现。
     */
    private IdGeneratorInterface resolveGenerator(IdType idType) {
        if (idType == IdType.CUSTOM) {
            IdGeneratorInterface custom = tinyJdbcRuntime.getIdGeneratorInterface();
            if (custom == null) {
                throw new TinyJdbcException("IdType.CUSTOM requires an IdGeneratorInterface bean.");
            }
            return custom;
        }
        IdGeneratorInterface generator = builtin.get(idType);
        if (generator == null) {
            throw new TinyJdbcException("No id generator for idType: " + idType + "!");
        }
        return generator;
    }

    /**
     * 按策略校验目标类型并把生成的 id 转换到字段类型。
     */
    private Object convert(IdContext context, Object id) {
        Class<?> fieldType = context.getFieldType();
        IdType idType = context.getIdType();

        if (idType == IdType.OBJECT_ID) {
            if (fieldType != String.class) {
                throw new TinyJdbcException("The type of " + context.getFieldName() + " field must be String when objectId!");
            }
            return id;
        }
        if (idType == IdType.UUID) {
            if (fieldType != String.class) {
                throw new TinyJdbcException("The type of " + context.getFieldName() + " field must be String when uuid!");
            }
            return id;
        }
        if (idType == IdType.ASSIGN_ID) {
            if (fieldType != String.class && fieldType != Long.class) {
                throw new TinyJdbcException("The type of " + context.getFieldName() + ", field must be String or Long when assignId!");
            }
            return (fieldType == String.class) ? String.valueOf(id) : id;
        }
        if (idType == IdType.SEQUENCE) {
            if (!Number.class.isAssignableFrom(fieldType)) {
                throw new TinyJdbcException("The type of " + context.getFieldName() + " field must be assignable from Number when sequence!");
            }
            return id;
        }
        // CUSTOM：允许任意可转换类型
        try {
            return ConvertUtils.convert(id, fieldType);
        } catch (Exception e) {
            throw new TinyJdbcException("The fieldType of " + context.getFieldName() + " is not supported! Please check if the ID type matches the primary key type.", e);
        }
    }

    /**
     * 把生成的主键值回写到实体字段。
     */
    private void setField(IdContext context, Object converted) {
        try {
            context.getField().set(context.getObj(), converted);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new TinyJdbcException("inject field value fail : " + context.getFieldName() + ", please verify if the ID type matches the primary key type!", e);
        }
    }
}

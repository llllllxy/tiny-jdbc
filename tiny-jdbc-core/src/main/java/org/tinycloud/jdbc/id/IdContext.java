package org.tinycloud.jdbc.id;

import org.springframework.jdbc.core.JdbcTemplate;
import org.tinycloud.jdbc.annotation.IdType;

import java.lang.reflect.Field;

/**
 * <p>
 *     主键生成上下文，携带生成某个主键所需的全部信息。
 * </p>
 * <p>
 *     内置生成器（雪花 / ObjectId / UUID / SEQUENCE）与用户自定义 {@link IdGeneratorInterface}
 *     均通过 {@link IdGeneratorInterface#nextId(IdContext)} 拿到该上下文，从而获取实体、字段类型、
 *     序列脚本等额外信息（例如 {@link IdType#SEQUENCE} 需要 {@code sequenceSql} 与 {@code jdbcTemplate}）。
 * </p>
 *
 * @author liuxingyu01
 */
public class IdContext {

    /**
     * 实体对象。
     */
    private final Object obj;

    /**
     * 主键字段。
     */
    private final Field field;

    /**
     * 主键字段类型。
     */
    private final Class<?> fieldType;

    /**
     * 主键字段名（用于异常提示）。
     */
    private final String fieldName;

    /**
     * 主键策略。
     */
    private final IdType idType;

    /**
     * 序列脚本（{@link IdType#SEQUENCE} 使用）。
     */
    private final String sequenceSql;

    /**
     * JdbcTemplate（{@link IdType#SEQUENCE} 使用）。
     */
    private final JdbcTemplate jdbcTemplate;

    private IdContext(Builder builder) {
        this.obj = builder.obj;
        this.field = builder.field;
        this.fieldType = builder.fieldType;
        this.fieldName = builder.fieldName;
        this.idType = builder.idType;
        this.sequenceSql = builder.sequenceSql;
        this.jdbcTemplate = builder.jdbcTemplate;
    }

    public Object getObj() {
        return obj;
    }

    public Field getField() {
        return field;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public IdType getIdType() {
        return idType;
    }

    public String getSequenceSql() {
        return sequenceSql;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Object obj;
        private Field field;
        private Class<?> fieldType;
        private String fieldName;
        private IdType idType;
        private String sequenceSql;
        private JdbcTemplate jdbcTemplate;

        public Builder obj(Object obj) {
            this.obj = obj;
            return this;
        }

        public Builder field(Field field) {
            this.field = field;
            return this;
        }

        public Builder fieldType(Class<?> fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public Builder idType(IdType idType) {
            this.idType = idType;
            return this;
        }

        public Builder sequenceSql(String sequenceSql) {
            this.sequenceSql = sequenceSql;
            return this;
        }

        public Builder jdbcTemplate(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
            return this;
        }

        public IdContext build() {
            return new IdContext(this);
        }
    }
}

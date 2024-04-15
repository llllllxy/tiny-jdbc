package org.tinycloud.jdbc.criteria;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 支持序列化的 Function
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
@FunctionalInterface
public interface TypeFunction<T, R> extends Serializable, Function<T, R> {
}

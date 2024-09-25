package org.tinycloud.jdbc.util.tuple;

import java.io.Serializable;

/**
 * <p>
 * 包装两个对象关系
 * </p>
 *
 * @param <L> 第二个属性类型
 * @param <R> 第三个属性类型
 * @author liuxingyu01
 * @since 2024-03-29 10:14
 */
public class Pair<L, R> implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 第一个属性 (一般为key)
     */
    private L left;
    /**
     * 第二个属性 (一般为value)
     */
    private R right;

    public final L getLeft() {
        return left;
    }

    public final L getKey() {
        return getLeft();
    }

    public final void setLeft(L left) {
        this.left = left;
    }

    public final R getRight() {
        return right;
    }

    public final R getValue() {
        return getRight();
    }

    public final void setRight(R right) {
        this.right = right;
    }

    public Pair() {
    }

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    @Override
    public String toString() {
        return "(" + left + ", " + right + ")";
    }
}

package org.tinycloud.jdbc.util.tuple;


import java.io.Serializable;

/**
 * 包装三个对象关系
 *
 * @param <F> 第一个属性类型
 * @param <S> 第二个属性类型
 * @param <T> 第三个属性类型
 */
public class Triple<F, S, T> implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 第一个属性
     */
    private F first;
    /**
     * 第二个属性
     */
    private S second;
    /**
     * 第三个属性
     */
    private T third;

    public F getFirst() {
        return first;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public S getSecond() {
        return second;
    }

    public void setSecond(S second) {
        this.second = second;
    }

    public T getThird() {
        return third;
    }

    public void setThird(T third) {
        this.third = third;
    }

    public Triple() {
    }

    public Triple(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public static <F, S, T> Triple<F, S, T> of(F first, S second, T third) {
        return new Triple<>(first, second, third);
    }

    @Override
    public String toString() {
        return "Triple{" +
                "first=" + first +
                ", second=" + second +
                ", third=" + third +
                '}';
    }
}

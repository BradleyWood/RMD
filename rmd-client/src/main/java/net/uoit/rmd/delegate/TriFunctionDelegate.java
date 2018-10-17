package net.uoit.rmd.delegate;

import java.io.Serializable;

/**
 * Represents an operation that accepts three inputs
 * and produces a result.
 *
 * @param <T> The type of the first parameter
 * @param <U> The type of the second parameter
 * @param <V> The type of the third parameter
 * @param <R> The type of the result
 */
@FunctionalInterface
public interface TriFunctionDelegate<T, U, V, R> extends Serializable {

    /**
     * Performs the operation and returns a result
     *
     * @param t The first parameter
     * @param u The second parameter
     * @param v The third parameter
     * @return The result
     */
    R invoke(T t, U u, V v);

}

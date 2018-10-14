package net.uoit.rmd.delegate;

import java.io.Serializable;

/**
 * Represents an operation that accepts two input arguments
 * and produces a result
 *
 * @param <T> The type of the first argument
 * @param <U> The type of the second argument
 * @param <R> The return type
 */
@FunctionalInterface
public interface BiFunctionDelegate<T, U, R> extends Serializable {

    /**
     * Invokes the operation
     *
     * @param t The first input argument
     * @param u The second input argument
     * @return The result
     */
    R invoke(T t, U u);

}

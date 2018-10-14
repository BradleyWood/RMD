package net.uoit.rmd.delegate;

import java.io.Serializable;

/**
 * Represents an operation that accepts one input argument
 * and produces a result.
 *
 * @param <T> The type of input argument
 * @param <R> The type of the result
 */
@FunctionalInterface
public interface FunctionDelegate<T, R> extends Serializable {

    /**
     * Invokes the operation
     *
     * @param t The input argument
     * @return The result of the operation
     */
    R invoke(T t);

}

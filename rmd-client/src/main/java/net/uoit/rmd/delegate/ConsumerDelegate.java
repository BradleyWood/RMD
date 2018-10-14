package net.uoit.rmd.delegate;

import java.io.Serializable;

/**
 * Represents an operation that accepts a single argument as input
 *
 * @param <T> The type of the input argument
 */
@FunctionalInterface
public interface ConsumerDelegate<T> extends Serializable {

    /**
     * Performs the operation on the input argument
     *
     * @param t The input argument
     */
    void invoke(T t);

}

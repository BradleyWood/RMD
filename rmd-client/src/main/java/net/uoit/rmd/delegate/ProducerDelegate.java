package net.uoit.rmd.delegate;

import java.io.Serializable;

/**
 * Represents an operation that accepts no input and produces a result
 *
 * @param <T> The type of the result
 */
@FunctionalInterface
public interface ProducerDelegate<T> extends Serializable {

    /**
     * Performs the operation produces the result
     *
     * @return The result produced by the operation
     */
    T invoke();

}

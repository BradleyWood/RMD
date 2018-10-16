package net.uoit.rmd.delegate;

/**
 * Represents a callback for delegate methods that were invoked asynchronously
 *
 * @param <R> The type of the result
 */
@FunctionalInterface
public interface Callback<R> {

    /**
     * Invoked in completion of a delegate method
     *
     * @param result The result of the delegate
     */
    void accept(R result);

}

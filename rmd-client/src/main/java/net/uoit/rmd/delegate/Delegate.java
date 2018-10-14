package net.uoit.rmd.delegate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that the target method is a delegate and is intended to
 * be executed remotely.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Delegate {

    /**
     * Optionally specifies the timeout for the delegate.
     * A timeout less than 0 is considered to have no timeout.
     *
     * @return The timeout in ms
     */
    int timeout() default -1;
}

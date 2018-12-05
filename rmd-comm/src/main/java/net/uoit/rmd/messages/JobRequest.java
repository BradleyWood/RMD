package net.uoit.rmd.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data class JobRequest extends Request {

    /**
     * The class file containing the method to execute
     */
    private final String className;
    /**
     * The index of the method
     */
    private final int methodIdx;
    /**
     * The serialized function arguments
     */
    private final byte[] arguments;
}

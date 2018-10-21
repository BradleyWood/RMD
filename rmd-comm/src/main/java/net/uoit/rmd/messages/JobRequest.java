package net.uoit.rmd.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data class JobRequest extends Request {

    private final String className;
    private final int methodIdx;
    private final byte[] arguments;
}

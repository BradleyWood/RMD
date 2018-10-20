package net.uoit.rmd.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data class JobRequest extends Request {

    private final int methodHash;
    private final byte[] arguments;
}

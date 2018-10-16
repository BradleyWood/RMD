package net.uoit.rmd.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data class JobResponse extends Response {

    private Object result = null;
    private Exception exception = null;

    public JobResponse(final Exception exception) {
        super(false, exception.getMessage());
        this.exception = exception;
    }

    public JobResponse(final Object result) {
        super(true, "Success");
        this.result = result;
    }
}

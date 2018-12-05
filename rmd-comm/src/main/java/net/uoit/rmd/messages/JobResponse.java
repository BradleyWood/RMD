package net.uoit.rmd.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data class JobResponse extends Response {

    /**
     * The result produced by the job
     */
    private Object result = null;
    /**
     * The exception thrown by a job.
     * If null, the job did not throw an exception.
     */
    private Throwable exception = null;

    public JobResponse(final Throwable exception) {
        super(false, exception.getMessage());
        this.exception = exception;
    }

    public JobResponse(final Object result) {
        super(true, "Success");
        this.result = result;
    }
}

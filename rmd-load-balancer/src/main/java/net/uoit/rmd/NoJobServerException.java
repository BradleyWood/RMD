package net.uoit.rmd;

/**
 * Thrown to indicate that RMD could not connect
 * to any job server
 */
public class NoJobServerException extends Throwable {

    public NoJobServerException() {
    }

    public NoJobServerException(final String message) {
        super(message);
    }
}

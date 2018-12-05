package net.uoit.rmd.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the response to a request
 */
@EqualsAndHashCode(callSuper = true)
public @Data class Response extends Message {

    private static final long serialVersionUID = 9155941886539270093L;

    /**
     * True of the request was successful
     */
    private final boolean success;
    /**
     * The response message, if applicable
     */
    private final String message;

}

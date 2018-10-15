package net.uoit.rmd.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data class Response extends Message {

    private static final long serialVersionUID = 9155941886539270093L;

    private final boolean success;
    private final String message;

}

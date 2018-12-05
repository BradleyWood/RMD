package net.uoit.rmd.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Represents a request to the server to migrate some code
 */
@EqualsAndHashCode(callSuper = true)
public @Data class MigrationRequest extends Request {

    private static final long serialVersionUID = 3321621871263793742L;

    /**
     * The map of class files to migrate (key=class name, value=class file instructions)
     */
    private final Map<String, byte[]> classMap;

}

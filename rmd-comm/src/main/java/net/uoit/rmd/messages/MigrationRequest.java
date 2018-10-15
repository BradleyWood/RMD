package net.uoit.rmd.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
public @Data class MigrationRequest extends Request {

    private static final long serialVersionUID = 3321621871263793742L;

    private final Map<String, byte[]> classMap;

}

package net.uoit.rmd.delegate;

import lombok.Data;

public @Data class DelegateInfo {

    private final Class definingClass;
    private final String name;
    private final String signature;

}

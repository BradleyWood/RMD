package net.uoit.rmd;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public @Data class RmdConfig implements Serializable {

    public static final String RETRY_STRATEGY = "RETRY";
    public static final String RUN_LOCAL_STRATEGY = "RUN_LOCALLY";
    public static final String THROW_ERROR_STRATEGY = "THROW_EXCEPTION";

    private final Set<String> hosts;
    private final String errorStrategy;
    private final int port;

    public void addHost(final String host) {
        hosts.add(host);
    }

    public Set<String> getHosts() {
        return new HashSet<>(hosts);
    }

    public static final RmdConfig DEFAULT = new RmdConfig(new HashSet<>(), "RETRY", 6050);
}

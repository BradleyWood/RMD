package net.uoit.rmd;

import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public @Data class RmdConfig implements Serializable {

    public static final String RETRY_STRATEGY = "RETRY";
    public static final String RUN_LOCAL_STRATEGY = "RUN_LOCALLY";
    public static final String THROW_ERROR_STRATEGY = "THROW_EXCEPTION";
    public static final int DEFAULT_JOB_SERVER_PORT = 5050;
    public static final int DEFAULT_SOCKET_TIMEOUT = 2000;

    private final Set<String> hosts;
    private final String errorStrategy;
    private final int port;
    private final int socketTimeout;

    /**
     * Add a host to the configuration
     *
     * @param host The host to add
     */
    public void addHost(final String host) {
        verifyHost(host);

        synchronized (hosts) {
            hosts.add(host);
        }
    }

    /**
     *
     * @return The connection timeout for the sockets
     */
    public int getSocketTimeout() {
        if (socketTimeout <= 0)
            return DEFAULT_SOCKET_TIMEOUT;

        return socketTimeout;
    }

    /**
     *
     * @param host
     */
    private void verifyHost(final @NonNull String host) {
        final String[] addressPortSplit = host.split(":");

        if (addressPortSplit.length == 2) {
            try {
                Integer.parseInt(addressPortSplit[1]);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Malformed host, port is not an integer: " + host);
            }
        } else if (addressPortSplit.length > 2) {
            throw new IllegalArgumentException("Malformed host: " + host);
        }
    }

    /**
     * Verifies the grammar of the specified hosts
     */
    public void verify() {
        synchronized (hosts) {
            for (final String host : hosts) {
                verifyHost(host);
            }
        }
    }

    /**
     * Remove a host from the configuration
     *
     * @param host The host to remove
     */
    public void removeHost(final String host) {
        synchronized (hosts) {
            hosts.remove(host);
        }
    }

    /**
     *
     * @return The list of hosts defined in the configuration
     */
    public Set<String> getHosts() {
        synchronized (hosts) {
            return new HashSet<>(hosts);
        }
    }

    public static final RmdConfig DEFAULT = new RmdConfig(new HashSet<>(), "RETRY", 6050, 2000);
}

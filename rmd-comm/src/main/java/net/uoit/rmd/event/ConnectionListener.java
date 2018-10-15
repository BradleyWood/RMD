package net.uoit.rmd.event;

import net.uoit.rmd.Connection;

public interface ConnectionListener {

    /**
     * Invoked to notify listeners that a client has connected
     *
     * @param connection The client that has connected
     */
    void connected(final Connection connection);

    /**
     * Invoked to notify listeners that a client has disconnected
     *
     * @param connection The client that has been disconnected
     */
    void disconnected(final Connection connection);

}

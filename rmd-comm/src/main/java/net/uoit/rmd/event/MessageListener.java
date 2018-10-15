package net.uoit.rmd.event;

import net.uoit.rmd.Connection;
import net.uoit.rmd.messages.Message;
import net.uoit.rmd.messages.Request;

public interface MessageListener {

    /**
     * Invoked to indicate a message has been received and it does not
     * require a response
     *
     * @param connection The connection
     * @param message The message
     */
    void messageReceived(final Connection connection, final Message message);

    /**
     * Invoked to indicate that a request has been received
     *
     * @param connection The connection
     * @param request The request
     * @param rId The request Id
     * @return The response
     */
    void requestReceived(final Connection connection, final Request request, final int rId);

}

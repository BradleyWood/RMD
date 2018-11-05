package net.uoit.rmd;

import lombok.Data;
import lombok.NonNull;
import net.uoit.rmd.event.ConnectionListener;
import net.uoit.rmd.event.MessageListener;
import net.uoit.rmd.messages.Message;
import net.uoit.rmd.messages.Request;
import net.uoit.rmd.messages.Response;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
import java.net.Socket;
import java.util.*;

public @Data class Connection implements Runnable {

    private static final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    private final List<ConnectionListener> connectionListeners = new LinkedList<>();
    private final Map<Integer, Response> responses = Collections.synchronizedMap(new HashMap<>());
    private MessageListener messageListener;

    private static int counter = 0;

    private final DataOutputStream dos;
    private final DataInputStream dis;
    private final Socket socket;

    private IOException exception;
    private boolean isRunning;

    public Connection(final Socket socket) throws IOException {
        this.socket = socket;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
        socket.setTcpNoDelay(true);
    }

    public <T extends Response> T send(final Request request) throws IOException {
        final int req = counter++;

        sendMessage(request, req);

        while (!responses.containsKey(req)) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }

        return (T) responses.remove(req);
    }

    public void send(final Message message) throws IOException {
        sendMessage(message, -1);
    }

    public void send(final Response response, final int rId) throws IOException {
        sendMessage(response, rId);
    }

    private void sendMessage(final @NonNull Message obj, final int rId) throws IOException {
        final byte[] message = conf.asByteArray(obj);

        synchronized (dos) {
            dos.writeInt(message.length);
            dos.writeInt(rId);
            dos.write(message);
        }
    }

    /**
     * Adds a packet listener the Client.
     *
     * @param messageListener The packet listener
     */
    public void setMessageListener(final MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    /**
     * Adds a connection listener
     *
     * @param listener The connection listener to add
     */
    public void addConnectionListener(final @NonNull ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    /**
     * Removes a connection listener
     *
     * @param listener The listener to remove
     */
    public void removeConnectionListener(final @NonNull ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    /**
     * Closes the socket and stops listening for packets
     *
     * @throws IOException if the socket is already closed
     */
    public void destroy() throws IOException {
        isRunning = false;
        socket.close();
    }

    @Override
    public void run() {
        isRunning = true;

        connectionListeners.forEach(l -> l.connected(this));

        while (isRunning) {
            try {
                final int messageLength = dis.readInt();
                final int rId = dis.readInt();
                final byte[] bytes = new byte[messageLength];

                dis.readFully(bytes);

                final Message obj = (Message) conf.asObject(bytes);

                if (obj instanceof Response) {
                    responses.put(rId, (Response) obj);

                    synchronized (this) {
                        notifyAll();
                    }

                    continue;
                }

                final boolean isRequest = obj instanceof Request && rId >= 0;

                try {
                    if (isRequest && messageListener != null) {
                        messageListener.requestReceived(this, (Request) obj, rId);
                    } else if (messageListener != null) {
                        messageListener.messageReceived(this, obj);
                    }
                } catch (final Throwable e) {
                    if (isRequest) {
                        send(new Response(false, e.getMessage()), rId);
                    }
                }
            } catch (IOException e) {
                isRunning = false;
                exception = e;
            } catch (Throwable e) {
                System.err.println("Internal Error");
            }
        }

        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        synchronized (this) {
            notifyAll();
        }

        connectionListeners.forEach(l -> l.disconnected(this));
    }
}

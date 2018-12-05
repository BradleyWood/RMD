package net.uoit.rmd;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import net.uoit.rmd.event.ConnectionListener;
import net.uoit.rmd.event.MessageListener;
import net.uoit.rmd.messages.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EqualsAndHashCode(callSuper = false)
public @Data class RMDServer extends Thread {

    private final List<Connection> connectionList = new LinkedList<>();
    private final Map<Connection, Client> clientConnectionMap = Collections.synchronizedMap(new HashMap<>());
    private final List<ConnectionListener> connectionListeners = Collections.synchronizedList(new LinkedList<>());
    private final ExecutorService executorService;
    private MessageListener messageListener;

    private final ServerSocket socket;

    public RMDServer(final ServerSocket socket) {
        this.socket = socket;
        executorService = Executors.newCachedThreadPool();

        addConnectionListener(cl);
        addMessageListener(ml);
    }

    public void addMessageListener(final @NonNull MessageListener messageListener) {
        this.messageListener = messageListener;
        connectionList.forEach(c -> c.setMessageListener(messageListener));
    }

    public void addConnectionListener(final @NonNull ConnectionListener listener) {
        connectionListeners.add(listener);
        connectionList.forEach(c -> c.addConnectionListener(listener));
    }

    public void removeConnectionListener(final @NonNull ConnectionListener listener) {
        connectionListeners.remove(listener);
        connectionList.forEach(c -> c.removeConnectionListener(listener));
    }

    @Override
    public void run() {
        while (true) {
            try {
                final Socket clientSocket = socket.accept();
                final Connection connection = new Connection(clientSocket);

                connectionList.add(connection);
                connectionListeners.forEach(connection::addConnectionListener);

                connection.setMessageListener(messageListener);

                new Thread(connection).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final ConnectionListener cl = new ConnectionListener() {
        @Override
        public void connected(Connection connection) {
            clientConnectionMap.put(connection, new Client());
        }

        @Override
        public void disconnected(Connection connection) {
            clientConnectionMap.remove(connection);
        }
    };

    private final MessageListener ml = new MessageListener() {
        @Override
        public void requestReceived(final Connection connection, final Request request, final int rId) {
            executorService.submit(() -> {
//                System.out.println(request);
                final Client client = clientConnectionMap.get(connection);

                Response response = new Response(false, "The request has no handler");

                if (request instanceof MigrationRequest) {
                    response = client.handleMigrationRequest((MigrationRequest) request);
                } else if (request instanceof JobRequest) {
                    response = client.handleJobRequest((JobRequest) request);
                }

                try {
                    connection.send(response, rId);
                } catch (IOException e) {
                    System.err.println("TODO; Cache response due to disconnect");
                }
            });
        }

        @Override
        public void messageReceived(final Connection connection, final Message message) {

        }
    };
}

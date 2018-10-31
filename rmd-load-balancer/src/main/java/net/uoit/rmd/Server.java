package net.uoit.rmd;

import lombok.Data;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public @Data class Server implements Runnable {

    private final RmdConfig config;
    private final LoadBalancer balancer;
    private ServerSocket serverSocket;
    private Thread thread;

    public void init() throws IOException {
        serverSocket = new ServerSocket(config.getPort());
        thread = new Thread(this);
        thread.start();
    }

    public void stop() throws IOException {
        serverSocket.close();
        thread.interrupt();
    }

    @Override
    public void run() {
        while (true) {
            try {
                final Socket socket = serverSocket.accept();
                final Connection connection = new Connection(socket);

//                connection.addConnectionListener(balancer);
                connection.setMessageListener(balancer);

                new Thread(connection).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

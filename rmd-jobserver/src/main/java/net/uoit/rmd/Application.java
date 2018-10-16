package net.uoit.rmd;

import java.io.IOException;
import java.net.ServerSocket;

public class Application {

    public static void main(String[] args) throws IOException {
        final RMDServer server = new RMDServer(new ServerSocket(5050));
        server.start();

        System.err.println("Server started on port: " + server.getSocket().getLocalPort());
    }
}

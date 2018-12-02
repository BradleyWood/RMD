package net.uoit.rmd;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;
import net.uoit.rmd.balancing.BalanceStrategy;
import net.uoit.rmd.balancing.RoundRobinStrategy;
import net.uoit.rmd.event.ConnectionListener;
import net.uoit.rmd.event.MessageListener;
import net.uoit.rmd.messages.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LoadBalancer implements Runnable, ConnectionListener, MessageListener {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, byte[]> classMap = new HashMap<>();
    private final List<JobServer> jobServers;
    private final BalanceStrategy balanceStrategy;
    private final RmdConfig config;
    private Thread thread;

    public LoadBalancer() {
        this(RmdConfig.DEFAULT);
    }

    public LoadBalancer(final RmdConfig config) {
        this(new ArrayList<>(), config);
    }

    public LoadBalancer(final ArrayList<JobServer> jobServers, final RmdConfig config) {
        this(jobServers, new RoundRobinStrategy(), config);
    }

    public LoadBalancer(final @NonNull List<JobServer> jobServers, final @NonNull BalanceStrategy balanceStrategy,
                        final @NonNull RmdConfig config) {
        this.jobServers = jobServers;
        this.balanceStrategy = balanceStrategy;
        this.config = config;
    }

    @SneakyThrows
    @Synchronized
    public void init() {
        if (config == null || config.getHosts().isEmpty()) {
            throw new NoJobServerException();
        }

        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void addClassDefs(final Map<String, byte[]> classMap) {
        this.classMap.putAll(classMap);
    }

    public boolean migrate(final Map<String, byte[]> classMap) {
        if (jobServers.isEmpty() && RmdConfig.THROW_ERROR_STRATEGY.equals(config.getErrorStrategy()))
            throw new NoJobServerException("no server to migrate to");

        if (jobServers.isEmpty() && RmdConfig.RUN_LOCAL_STRATEGY.equals(config.getErrorStrategy()))
            return false;

        while (jobServers.isEmpty()) {
            synchronized (jobServers) {
                try {
                    jobServers.wait(100);
                } catch (InterruptedException e) {
                }
            }
        }

        synchronized (jobServers) {
            for (final JobServer jobServer : jobServers) {
                jobServer.migrate(classMap);
            }
        }

        return true;
    }

    public JobResponse submit(final JobRequest jobRequest) {
        while (true) {
            try {
                final JobServer server = balanceStrategy.next(jobServers);
                migrate(classMap);
                return server.submit(jobRequest);
            } catch (IOException ignored) {
                ignored.printStackTrace();
            } catch (NoJobServerException e) {
                return new JobResponse(e);
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            final Set<String> hosts = config.getHosts();

            synchronized (jobServers) {
                for (final JobServer jobServer : jobServers) {
                    hosts.remove(jobServer.getHost());
                }
            }

            final LinkedList<Future> tasks = new LinkedList<>();

            for (final String host : hosts) {
                tasks.add(executorService.submit(() -> {
                    System.out.println("Trying: " + host);
                    try {
                        String address = host;
                        int port = RmdConfig.DEFAULT_JOB_SERVER_PORT;

                        String[] addressPortSplit = host.split(":");

                        if (addressPortSplit.length == 2) {
                            address = addressPortSplit[0];
                            port = Integer.parseInt(addressPortSplit[1]);
                        }


                        final InetSocketAddress endPoint = new InetSocketAddress(address, port);
                        final Socket socket = new Socket();

                        socket.connect(endPoint, config.getSocketTimeout());

                        final Connection connection = new Connection(socket);
                        connection.addConnectionListener(this);

                        final JobServer jobServer = new JobServer(connection, host);

                        synchronized (jobServers) {
                            jobServers.add(jobServer);
                            jobServers.notifyAll();
                        }

                        new Thread(connection).start();
                        System.out.println("Success: " + host);
                    } catch (IOException e) {
                        System.out.println("Failed: " + host);
                    }
                }));
            }

            while (!tasks.isEmpty()) {
                try {
                    tasks.removeFirst().get();
                } catch (InterruptedException | ExecutionException e) {
                }
            }

            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public void connected(final Connection connection) {
        System.out.println("Connected to " + connection.getSocket().getInetAddress());
    }

    @Override
    public void disconnected(final Connection connection) {
        System.out.println("Disconnected from " + connection.getSocket().getInetAddress());

        synchronized (jobServers) {
            jobServers.removeIf(p -> p.getConnection() == connection);
        }

        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void messageReceived(final Connection connection, final Message message) {

    }

    @Override
    public void requestReceived(final Connection connection, final Request request, final int rId) {
        try {
            if (request instanceof MigrationRequest) {
                classMap.putAll(((MigrationRequest) request).getClassMap());

                final Response response = new Response(true, "Ok");

                connection.send(response, rId);
            } else if (request instanceof JobRequest) {
                JobResponse response;

                try {
                    response = submit((JobRequest) request);
                } catch (NoJobServerException e) {
                    response = new JobResponse(e);
                }

                connection.send(response, rId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

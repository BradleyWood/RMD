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
import java.net.Socket;
import java.util.*;

public class LoadBalancer implements Runnable, ConnectionListener, MessageListener {

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
        this(jobServers, new RoundRobinStrategy(jobServers), config);
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

    public void migrate(final Map<String, byte[]> classMap) {
        synchronized (jobServers) {
            for (final JobServer jobServer : jobServers) {
                jobServer.migrate(classMap);
            }
        }
    }

    public JobResponse submit(final JobRequest jobRequest) {
        while (true) {
            try {
                final JobServer server = balanceStrategy.next();
                migrate(classMap);
                return server.submit(jobRequest);
            } catch (NoJobServerException e) {
                return new JobResponse(e);
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while (true) {

            synchronized (jobServers) {
                final Set<String> hosts = config.getHosts();

                for (final JobServer jobServer : jobServers) {
                    hosts.remove(jobServer.getHost());
                }

                boolean error = false;

                for (final String host : hosts) {
                    try {
                        final Connection connection = new Connection(new Socket(host, 5050));
                        connection.addConnectionListener(this);

                        final JobServer jobServer = new JobServer(connection, host);
                        jobServers.add(jobServer);
                        System.out.println("Add job server");

                        new Thread(connection).start();
                    } catch (IOException e) {
                        error = true;
                    }
                }

                if (error) {
                    continue;
                }
            }

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public void connected(final Connection connection) {
        System.out.println("Connected");
    }

    @Override
    public void disconnected(final Connection connection) {
        System.out.println("Disconnected");
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
        System.out.println(request);
        try {
            if (request instanceof MigrationRequest) {
                classMap.putAll(((MigrationRequest) request).getClassMap());

                final Response response = new Response(true, "Ok");

                connection.send(response, rId);
                System.out.println("Migration Response: " + response);
            } else if (request instanceof JobRequest) {
                System.out.println("Submit job");
                final JobResponse response = submit((JobRequest) request);

                System.out.println("Job Response: " + response);
                connection.send(response, rId);
            } else {
                System.err.println("wtfff?");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

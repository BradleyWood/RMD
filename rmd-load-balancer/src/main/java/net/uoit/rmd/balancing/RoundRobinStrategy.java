package net.uoit.rmd.balancing;

import lombok.NonNull;
import net.uoit.rmd.JobServer;
import net.uoit.rmd.NoJobServerException;

import java.util.List;

public class RoundRobinStrategy implements BalanceStrategy {

    private final List<JobServer> jobServers;
    private int idx = 0;

    public RoundRobinStrategy(final @NonNull List<JobServer> jobServers) {
        this.jobServers = jobServers;
    }

    @Override
    public JobServer next() throws NoJobServerException {
        synchronized (jobServers) {
            if (jobServers.isEmpty())
                throw new NoJobServerException();

            idx++;

            if (idx >= jobServers.size()) {
                idx = 0;
            }

            return jobServers.get(idx);
        }
    }
}

package net.uoit.rmd.balancing;

import lombok.NonNull;
import net.uoit.rmd.JobServer;
import net.uoit.rmd.NoJobServerException;

import java.util.List;

/**
 * Implements the round robin scheduling strategy.
 */
public class RoundRobinStrategy implements BalanceStrategy {

    private int idx = 0;

    @Override
    public JobServer next(final @NonNull List<JobServer> jobServers) throws NoJobServerException {
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

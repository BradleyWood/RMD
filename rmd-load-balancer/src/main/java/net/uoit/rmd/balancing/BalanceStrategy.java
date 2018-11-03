package net.uoit.rmd.balancing;

import net.uoit.rmd.JobServer;
import net.uoit.rmd.NoJobServerException;

import java.util.List;

public interface BalanceStrategy {

    /**
     * Gets the next job server
     *
     * @param jobServers The list to choose from
     * @return The job-server to handle the request
     * @throws NoJobServerException if no job servers are available
     */
    JobServer next(List<JobServer> jobServers);

}

package net.uoit.rmd.balancing;

import net.uoit.rmd.JobServer;
import net.uoit.rmd.NoJobServerException;

public interface BalanceStrategy {

    /**
     * Gets the next job server
     *
     * @return The job-server to handle the request
     * @throws NoJobServerException if no job servers are available
     */
    JobServer next() throws NoJobServerException;

}

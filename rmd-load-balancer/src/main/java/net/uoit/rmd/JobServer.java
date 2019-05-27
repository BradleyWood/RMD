package net.uoit.rmd;

import lombok.Data;
import lombok.Synchronized;
import net.uoit.rmd.messages.JobRequest;
import net.uoit.rmd.messages.JobResponse;
import net.uoit.rmd.messages.MigrationRequest;
import net.uoit.rmd.messages.Response;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public @Data class JobServer {

    private final Set<String> classes = new HashSet<>();
    private final Connection connection;
    private final String host;

    public JobResponse submit(final JobRequest jobRequest) throws IOException {
        return connection.send(jobRequest);
    }

    @Synchronized
    public void migrate(final Map<String, byte[]> classMap) {
        try {
            for (final String aClass : classes) {
                classMap.remove(aClass);
            }

            if (classMap.isEmpty())
                return;

            final MigrationRequest mr = new MigrationRequest(classMap);
            final Response response = connection.send(mr);

            if (response.isSuccess()) {
                classes.addAll(classMap.keySet());
            } else {
                System.out.println("Migration Failed");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

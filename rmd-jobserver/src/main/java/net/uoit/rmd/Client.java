package net.uoit.rmd;

import net.uoit.rmd.messages.JobRequest;
import net.uoit.rmd.messages.JobResponse;
import net.uoit.rmd.messages.MigrationRequest;
import net.uoit.rmd.messages.Response;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Client {

    private final Map<Integer, Method> methodMap = new HashMap<>();

    public Response handleMigrationRequest(final MigrationRequest request) {
        final Map<String, byte[]> classes = request.getClassMap();
        final RemoteClassLoader classLoader = new RemoteClassLoader();

        Response response;

        try {
            for (final Map.Entry<String, byte[]> set : classes.entrySet()) {
                classLoader.addClass(set.getKey(), set.getValue());
            }

            for (final Map.Entry<String, byte[]> set : classes.entrySet()) {
                final Class cl = classLoader.loadClass(set.getKey());

                for (final Method method : cl.getDeclaredMethods()) {
                    method.setAccessible(true);
                    methodMap.put(method.hashCode(), method);
                }
            }

            response = new Response(true, "Success");
        } catch (Throwable e) {
            response = new Response(false, "Class loading error");
        }

        return response;
    }

    public JobResponse handleJobRequest(final JobRequest jobRequest) {
        final Method method = methodMap.get(jobRequest.getMethodHash());
        JobResponse response;

        if (method == null) {
            response = new JobResponse(new NoSuchMethodException("Method hash: " + jobRequest.getMethodHash()));
        } else {
            try {
                final Object result = method.invoke(null, jobRequest.getArguments());

                response = new JobResponse(result);
            } catch (IllegalAccessException | InvocationTargetException e) {
                response = new JobResponse(e);
            }
        }

        return response;
    }
}
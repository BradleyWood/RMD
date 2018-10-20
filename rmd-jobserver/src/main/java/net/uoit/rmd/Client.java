package net.uoit.rmd;

import net.uoit.rmd.messages.JobRequest;
import net.uoit.rmd.messages.JobResponse;
import net.uoit.rmd.messages.MigrationRequest;
import net.uoit.rmd.messages.Response;
import org.nustaq.serialization.FSTConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Client {

    private final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    private final RemoteClassLoader classLoader = new RemoteClassLoader();
    private final Map<Integer, Method> methodMap = new HashMap<>();

    public Response handleMigrationRequest(final MigrationRequest request) {
        final Map<String, byte[]> classes = request.getClassMap();
        conf.setClassLoader(classLoader);

        System.out.println(request);

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
                Object[] args = (Object[]) conf.asObject(jobRequest.getArguments());
                final Object instance = Modifier.isStatic(method.getModifiers()) ? null : args[0];

                if (instance != null)
                    args = Arrays.copyOfRange(args, 1, args.length);

                final Object result = method.invoke(instance, args);

                response = new JobResponse(result);
            } catch (IllegalAccessException | InvocationTargetException e) {
                response = new JobResponse(e);
                e.printStackTrace();
            }
        }

        return response;
    }
}

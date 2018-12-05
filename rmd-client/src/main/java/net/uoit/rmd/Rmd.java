package net.uoit.rmd;

import lombok.Synchronized;
import net.uoit.rmd.asm.DependencyManager;
import net.uoit.rmd.delegate.*;
import net.uoit.rmd.messages.JobRequest;
import net.uoit.rmd.messages.JobResponse;
import org.nustaq.kson.Kson;
import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SuppressWarnings("unchecked")
public class Rmd {

    private static final Map<Object, Serializable> delegateMap = Collections.synchronizedMap(new HashMap<>());
    private static final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    private static final Set<String> classes = Collections.synchronizedSet(new HashSet<>());
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final List<Future> asyncJobs = Collections.synchronizedList(new LinkedList<>());
    private static RmdConfig config = RmdConfig.DEFAULT;
    private static LoadBalancer balancer;

    @Synchronized
    private static void init() {
        if (balancer != null)
            return;

        Kson kson = new Kson().map("config", RmdConfig.class);

        try {
            final File configFile = new File("config.kson");

            if (configFile.exists()) {
                config = (RmdConfig) kson.readObject(new File("config.kson"));
            } else {
                final InputStream in = Rmd.class.getResourceAsStream("/config.kson");
                if (in != null) {
                    config = (RmdConfig) kson.readObject(in, "UTF-8", null);
                } else {
                    System.err.println("WARNING: No configuration found.");
                    config = RmdConfig.DEFAULT;
                }
            }
        } catch (Exception e) {
            sneakyThrow(e);
        }

        config.verify();

        balancer = new LoadBalancer(config);
        balancer.init();
    }

    /**
     * Waits for all asynchronous jobs to complete execution.
     *
     * @throws ExecutionException If an async job throws an exception
     * @throws InterruptedException If the current thread is interrupted
     */
    public static void waitForAsyncJobs() throws ExecutionException, InterruptedException {
        while (!asyncJobs.isEmpty()) {
            asyncJobs.remove(0).get();
        }
    }

    /**
     * Add a host to configuration.
     *
     * @param host the host address
     */
    public static void addHost(final String host) {
        if (balancer != null)
            init();

        config.addHost(host);
        balancer.notifyAll();
    }

    /**
     * Remove a host from the configuration
     *
     * @param host the host address
     */
    public static void removeHost(final String host) {
        if (balancer != null)
            init();

        config.removeHost(host);
        balancer.notifyAll();
    }

    @Synchronized
    private static void mapClass(final DelegateInfo info) throws IOException {
        if (!classes.contains(info.getDefiningClass().getName())) {
            final Map<String, byte[]> deps = DependencyManager.getDependencies(info.getDefiningClass());

            classes.addAll(deps.keySet());
            classes.add(info.getDefiningClass().getName());
            balancer.addClassDefs(deps);
        }
    }

    private static <E extends Throwable> void sneakyThrow(final Throwable e) throws E {
        throw (E) e;
    }

    private static Object invokeDelegate(final DelegateInfo info, final Object[] array) {
        if (balancer == null) {
            init();
        }

        Throwable toThrow;

        while (true) {
            try {
                mapClass(info);

                final byte[] args = conf.asByteArray(array);

                final JobRequest request = new JobRequest(info.getDefiningClass().getName(), info.getIdx(), args);
                final JobResponse response = balancer.submit(request);

                final Throwable exception = response.getException();

                if (exception instanceof InvocationTargetException) {
                    toThrow = ((InvocationTargetException) exception).getTargetException();
                    break;
                } else if (exception instanceof NoJobServerException) {
                    if (RmdConfig.RUN_LOCAL_STRATEGY.equals(config.getErrorStrategy())) {
                        return invokeLocally(info, array);
                    } else if (RmdConfig.RETRY_STRATEGY.equals(config.getErrorStrategy())) {
                        continue;
                    }
                }

                if (exception != null) {
                    toThrow = exception;
                    break;
                }

                return response.getResult();
            } catch (IOException ignored) {
            } catch (NoJobServerException | IllegalAccessException e) {
                toThrow = e;
                break;
            } catch (InvocationTargetException e) {
                toThrow = e.getTargetException();
                break;
            }
        }

        sneakyThrow(toThrow);
        return null;
    }

    private static Object invokeLocally(final DelegateInfo delegateInfo, final Object[] arguments)
            throws InvocationTargetException, IllegalAccessException {
        final Method method = delegateInfo.getDefiningClass().getMethods()[delegateInfo.getIdx()];
        method.setAccessible(true);

        Object[] args = arguments;
        final Object instance = Modifier.isStatic(method.getModifiers()) ? null : args[0];

        if (instance != null)
            args = Arrays.copyOfRange(args, 1, args.length);

        return method.invoke(instance, args);
    }

    /**
     * Produces a wrapper interface that can be invoked to execute
     * jobs on a job server.
     *
     * @param delegate The delegate info containing the callsite information
     * @return The job delegate interface
     */
    public static FunctionDelegate asDelegate(final DelegateInfo delegate) {
        final Serializable producer = delegateMap.get(delegate);

        if (producer instanceof FunctionDelegate)
            return (FunctionDelegate) producer;

        final FunctionDelegate producerD = (i) -> invokeDelegate(delegate, new Object[]{i});

        delegateMap.put(delegate, producerD);

        return producerD;
    }

    /**
     * Produces a wrapper interface that represents a function that can be
     * invoked on a job server to produce a result. This function does not accept any input.
     *
     * @param delegate Represents a job that has no parameters and produces a result
     * @param <R> The return type of the job
     * @return The job delegate interface
     */
    public static <R> ProducerDelegate<R> asDelegate(final ProducerDelegate<R> delegate) {
        final Serializable producer = delegateMap.get(delegate);

        if (producer instanceof ProducerDelegate)
            return (ProducerDelegate<R>) producer;

        final DelegateInfo info = getDelegateInfo(delegate);
        final ProducerDelegate<R> producerD = () -> (R) invokeDelegate(info, new Object[0]);

        delegateMap.put(delegate, producerD);

        return producerD;
    }

    /**
     * Produces a wrapper interface that represents a function that can be invoked on
     * a job server to consume one input variable. This function does not produce
     * a result.
     *
     * @param delegate A job the consumes one input parameter
     * @param <T> The type of the input
     * @return the job delegate interface
     */
    public static <T> ConsumerDelegate<T> asDelegate(final ConsumerDelegate<T> delegate) {
        final Serializable consumer = delegateMap.get(delegate);

        if (consumer instanceof ConsumerDelegate)
            return (ConsumerDelegate<T>) consumer;

        final DelegateInfo info = getDelegateInfo(delegate);
        final ConsumerDelegate<T> consumerD = (t) -> invokeDelegate(info, new Object[]{t});

        delegateMap.put(delegate, consumerD);

        return consumerD;
    }

    /**
     * Produces a wrapper interface that represents a function that can be invoked
     * on a job server to produce a result. This function accepts one input parameter.
     *
     * @param delegate A job that represents a function that accepts one input and produces a result
     * @param <T> The type of the input parameter
     * @param <R> The type of the return value
     * @return the job delegate interface
     */
    public static <T, R> FunctionDelegate<T, R> asDelegate(final FunctionDelegate<T, R> delegate) {
        final Serializable fun = delegateMap.get(delegate);

        if (fun instanceof FunctionDelegate)
            return (FunctionDelegate<T, R>) fun;

        final DelegateInfo info = getDelegateInfo(delegate);
        FunctionDelegate<T, R> funD = (t) -> (R) invokeDelegate(info, new Object[]{t});

        delegateMap.put(delegate, funD);

        return funD;
    }

    /**
     * Produces a wrapper interface that represents a function that can be invoked
     * on a job server to produce a result. This function accepts two input parameters
     * and produces a result.
     *
     * @param delegate A job that represents a function that accepts two inputs and produces a result
     * @param <T> The type of the first input parameter
     * @param <U> The type of the second input parameter
     * @param <R> The type of the return value
     * @return the job delegate interface
     */
    public static <T, U, R> BiFunctionDelegate<T, U, R> asDelegate(final BiFunctionDelegate<T, U, R> delegate) {
        final Serializable fun = delegateMap.get(delegate);

        if (fun instanceof BiFunctionDelegate)
            return (BiFunctionDelegate<T, U, R>) fun;

        final DelegateInfo info = getDelegateInfo(delegate);
        final BiFunctionDelegate<T, U, R> biFunctionDelegate = (t, u) -> (R) invokeDelegate(info, new Object[]{t, u});

        delegateMap.put(delegate, biFunctionDelegate);

        return biFunctionDelegate;
    }

    /**
     * Produces a wrapper interface that represents a function that can be invoked
     * on a job server to produce a result. This function accepts three input parameters
     * and produces a result.
     *
     * @param delegate A job the represents a function that accepts three inputs and produces a result
     * @param <T> The type of the first parameter
     * @param <U> The type of the second parameter
     * @param <V> The type of the third parameter
     * @param <R> The type of the return value
     * @return the job delegate interface
     */
    public static <T, U, V, R> TriFunctionDelegate<T, U, V, R> asDelegate(final TriFunctionDelegate<T, U, V, R> delegate) {
        final Serializable fun = delegateMap.get(delegate);

        if (fun instanceof TriFunctionDelegate)
            return (TriFunctionDelegate<T, U, V, R>) fun;

        final DelegateInfo info = getDelegateInfo(delegate);
        final TriFunctionDelegate<T, U, V, R> triFunctionDelegate = (t, u, v) -> (R) invokeDelegate(info, new Object[]{t, u, v});

        delegateMap.put(delegate, triFunctionDelegate);

        return triFunctionDelegate;
    }

    /**
     * Synchronously executes the specified delegate function on a job server and produces a result.
     * Exceptions thrown by the job are uncaught.
     *
     * @param delegate A job the represents a function that accepts three inputs and produces a result
     * @param t The first parameter
     * @param u The second parameter
     * @param v The third parameter
     * @param <T> The type of the first input parameter
     * @param <U> The type of the second input parameter
     * @param <V> The type of the third input parameter
     * @param <R> The return type of the job
     * @return The result produced by the job function
     */
    public static <T, U, V, R> R delegate(final TriFunctionDelegate<T, U, V, R> delegate, final T t, final U u, final V v) {
        return asDelegate(delegate).invoke(t, u, v);
    }

    /**
     * Synchronously executes the specified delegate function on a job server and produces a result.
     * Exceptions thrown by the job are uncaught.
     *
     * @param delegate A job that represents a function that accepts two inputs and produces a result
     * @param t The first parameter
     * @param u The second parameter
     * @param <T> The type of the first parameter
     * @param <U> The type of the second parameter
     * @param <R> The type of the return value
     * @return The result produced by the job function
     */
    public static <T, U, R> R delegate(final BiFunctionDelegate<T, U, R> delegate, final T t, final U u) {
        return asDelegate(delegate).invoke(t, u);
    }

    /**
     * Synchronously executes the specified delegate function on a job server and produces a result.
     * Exceptions thrown by the job are uncaught.
     *
     * @param delegate A job that represents a function that accepts one input and produces a result.
     * @param t The input parameter
     * @param <T> The type of the input parameter
     * @param <R> The type of the return value
     * @return The result produced by the job function
     */
    public static <T, R> R delegate(final FunctionDelegate<T, R> delegate, final T t) {
        return asDelegate(delegate).invoke(t);
    }

    /**
     * Synchronously executes the specified delegate function on a job server and consumes an input value.
     * Exceptions thrown by the job are uncaught.
     *
     *
     * @param delegate The job that represents a function that consumes a value
     * @param t The input parameter
     * @param <T> The type of the input parameter
     */
    public static <T> void delegate(final ConsumerDelegate<T> delegate, final T t) {
        asDelegate(delegate).invoke(t);
    }

    /**
     *  Synchronously executes the specified delegate function on a job server and produces a result.
     *  Exceptions thrown by the job are uncaught.
     *
     * @param delegate A job that represents a function that produces a value
     * @param <R> The type of the return value
     * @return The result produced
     */
    public static <R> R delegate(final ProducerDelegate<R> delegate) {
        return asDelegate(delegate).invoke();
    }

    /**
     * Asynchronously executes the specified delegate function on a job server.
     *
     * The specified callback function is invoked upon completion of the job.
     *
     * @param delegate A job that represents a function that produces a value
     * @param callback The function to be called after the job completes
     * @param <R> The type of the return value
     */
    public static <R> void delegate(final ProducerDelegate<R> delegate, final Callback<R> callback) {
        asyncJobs.add(executorService.submit(() -> {
            final R result = delegate(delegate);

            if (callback != null)
                callback.accept(result);
        }));
    }

    /**
     * Asynchronously executes the specified delegate function on a job server with
     * the given input parameters.
     *
     * The specified callback function is invoked upon completion of the job.
     *
     * @param delegate A job that represents a function that accepts one input parameter and produces a result
     * @param t The input parameter
     * @param callback The function to be called after the job completes
     * @param <T> The type of the input parameter
     * @param <R> The type of the job's result value
     */
    public static <T, R> void delegate(final FunctionDelegate<T, R> delegate, final T t, final Callback<R> callback) {
        asyncJobs.add(executorService.submit(() -> {
            final R result = delegate(delegate, t);

            if (callback != null)
                callback.accept(result);
        }));
    }

    /**
     * Asynchronously executes the specified delegate function on a job server with
     * the given input parameters.
     *
     * The specified callback function is invoked upon completion of the job.
     *
     *
     * @param delegate A job that represents a function that accepts two input parameters and produces a result
     * @param t The first input parameter
     * @param u The second input parameter
     * @param callback The function to be called after the job completes
     * @param <T> The type of the first input parameter
     * @param <U> The type of the second input parameter
     * @param <R> The type of the job's result value
     */
    public static <T, U, R> void delegate(final BiFunctionDelegate<T, U, R> delegate, final T t, final U u,
                                          final Callback<R> callback) {
        asyncJobs.add(executorService.submit(() -> {
            final R result = delegate(delegate, t, u);

            if (callback != null)
                callback.accept(result);
        }));
    }

    /**
     * Asynchronously executes the specified delegate function on a job server with
     * the given input parameters.
     *
     * The specified callback function is invoked upon completion of the job.
     *
     * @param delegate A job that represents a function that accepts three input parameters and produces a result
     * @param t The first input parameter
     * @param u The second input parameter
     * @param v The third input parameter
     * @param callback The function to be called after the job completes
     * @param <T> The type of the first input parameter
     * @param <U> The type of the second input parameter
     * @param <V> The type of the third input parameter
     * @param <R> The type of the job's result value
     */
    public static <T, U, V, R> void delegate(final TriFunctionDelegate<T, U, V, R> delegate, final T t, final U u,
                                             final V v, final Callback<R> callback) {
        asyncJobs.add(executorService.submit(() -> {
            final R result = delegate(delegate, t, u, v);

            if (callback != null)
                callback.accept(result);
        }));
    }

    private static DelegateInfo getDelegateInfo(final Serializable methodReference) {
        final SerializedLambda serializedLambda = getLambda(methodReference);

        if (serializedLambda == null)
            throw new RuntimeException("Failed to serialize lambda expression");

        try {
            final Class cl = Class.forName(serializedLambda.getImplClass().replace("/", "."));
            final String name = serializedLambda.getImplMethodName();

            return new DelegateInfo(cl, name, serializedLambda.getImplMethodSignature());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to serialize lambda expression");
        }
    }

    private static SerializedLambda getLambda(final Serializable lambda) {
        Class<?> clazz = lambda.getClass();

        while (clazz != null) {
            try {
                final Method m = clazz.getDeclaredMethod("writeReplace");
                m.setAccessible(true);

                final Object serializedLambda = m.invoke(lambda);

                if (serializedLambda instanceof SerializedLambda) {
                    return (SerializedLambda) serializedLambda;
                }

                return null;
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException | InvocationTargetException e) {
                return null;
            }

            clazz = clazz.getSuperclass();
        }

        return null;
    }
}

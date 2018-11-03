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

    private static void init() {
        Kson kson = new Kson().map("config", RmdConfig.class);

        try {
            final File configFile = new File("config.kson");

            if (configFile.exists()) {
                config = (RmdConfig) kson.readObject(new File("config.kson"));
            } else {
                final InputStream in = Object.class.getResourceAsStream("config.kson");
                if (in != null) {
                    config = (RmdConfig) kson.readObject(in, "UTF-8", null);
                }
            }
        } catch (Exception e) {
            sneakyThrow(e);
        }

        config.verify();

        balancer = new LoadBalancer(config);
        balancer.init();
    }

    public static void waitForAsyncJobs() throws ExecutionException, InterruptedException {
        while (!asyncJobs.isEmpty()) {
            asyncJobs.remove(0).get();
        }
    }

    public static void addHost(final String host) {
        if (balancer != null)
            init();

        config.addHost(host);
        balancer.notifyAll();
    }

    public static void removeHost(final String host) {
        if (balancer != null)
            init();

        config.removeHost(host);
        balancer.notifyAll();
    }

    @Synchronized
    private static boolean migrate(final DelegateInfo info) throws IOException {
        if (!classes.contains(info.getDefiningClass().getName())) {
            final Map<String, byte[]> deps = DependencyManager.getDependencies(info.getDefiningClass());

            for (final String aClass : classes) {
                deps.remove(aClass);
            }

            classes.addAll(deps.keySet());
            classes.add(info.getDefiningClass().getName());

            if (deps.isEmpty())
                return true;

            return balancer.migrate(deps);
        }

        return true;
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
                if (!migrate(info) && RmdConfig.RUN_LOCAL_STRATEGY.equals(config.getErrorStrategy())) {
                    return invokeLocally(info, array);
                }

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

    public static FunctionDelegate asDelegate(final DelegateInfo delegate) {
        final Serializable producer = delegateMap.get(delegate);

        if (producer instanceof FunctionDelegate)
            return (FunctionDelegate) producer;

        final FunctionDelegate producerD = (i) -> invokeDelegate(delegate, new Object[]{i});

        delegateMap.put(delegate, producerD);

        return producerD;
    }

    public static <R> ProducerDelegate<R> asDelegate(final ProducerDelegate<R> delegate) {
        final Serializable producer = delegateMap.get(delegate);

        if (producer instanceof ProducerDelegate)
            return (ProducerDelegate<R>) producer;

        final DelegateInfo info = getDelegateInfo(delegate);
        final ProducerDelegate<R> producerD = () -> (R) invokeDelegate(info, new Object[0]);

        delegateMap.put(delegate, producerD);

        return producerD;
    }

    public static <T> ConsumerDelegate<T> asDelegate(final ConsumerDelegate<T> delegate) {
        final Serializable consumer = delegateMap.get(delegate);

        if (consumer instanceof ConsumerDelegate)
            return (ConsumerDelegate<T>) consumer;

        final DelegateInfo info = getDelegateInfo(delegate);
        final ConsumerDelegate<T> consumerD = (t) -> invokeDelegate(info, new Object[]{t});

        delegateMap.put(delegate, consumerD);

        return consumerD;
    }

    public static <T, R> FunctionDelegate<T, R> asDelegate(final FunctionDelegate<T, R> delegate) {
        final Serializable fun = delegateMap.get(delegate);

        if (fun instanceof FunctionDelegate)
            return (FunctionDelegate<T, R>) fun;

        final DelegateInfo info = getDelegateInfo(delegate);
        FunctionDelegate<T, R> funD = (t) -> (R) invokeDelegate(info, new Object[]{t});

        delegateMap.put(delegate, funD);

        return funD;
    }

    public static <T, U, R> BiFunctionDelegate<T, U, R> asDelegate(final BiFunctionDelegate<T, U, R> delegate) {
        final Serializable fun = delegateMap.get(delegate);

        if (fun instanceof BiFunctionDelegate)
            return (BiFunctionDelegate<T, U, R>) fun;

        final DelegateInfo info = getDelegateInfo(delegate);
        final BiFunctionDelegate<T, U, R> biFunctionDelegate = (t, u) -> (R) invokeDelegate(info, new Object[]{t, u});

        delegateMap.put(delegate, biFunctionDelegate);

        return biFunctionDelegate;
    }

    public static <T, U, V, R> TriFunctionDelegate<T, U, V, R> asDelegate(final TriFunctionDelegate<T, U, V, R> delegate) {
        final Serializable fun = delegateMap.get(delegate);

        if (fun instanceof TriFunctionDelegate)
            return (TriFunctionDelegate<T, U, V, R>) fun;

        final DelegateInfo info = getDelegateInfo(delegate);
        final TriFunctionDelegate<T, U, V, R> triFunctionDelegate = (t, u, v) -> (R) invokeDelegate(info, new Object[]{t, u, v});

        delegateMap.put(delegate, triFunctionDelegate);

        return triFunctionDelegate;
    }

    public static <T, U, V, R> R delegate(final TriFunctionDelegate<T, U, V, R> delegate, final T t, final U u, final V v) {
        return asDelegate(delegate).invoke(t, u, v);
    }

    public static <T, U, R> R delegate(final BiFunctionDelegate<T, U, R> delegate, final T t, final U u) {
        return asDelegate(delegate).invoke(t, u);
    }

    public static <T, R> R delegate(final FunctionDelegate<T, R> delegate, final T t) {
        return asDelegate(delegate).invoke(t);
    }

    public static <T> void delegate(final ConsumerDelegate<T> delegate, final T t) {
        asDelegate(delegate).invoke(t);
    }

    public static <R> R delegate(final ProducerDelegate<R> delegate) {
        return asDelegate(delegate).invoke();
    }

    public static <R> void delegate(final ProducerDelegate<R> delegate, final Callback<R> callback) {
        asyncJobs.add(executorService.submit(() -> {
            final R result = delegate(delegate);

            if (callback != null)
                callback.accept(result);
        }));
    }

    public static <T, R> void delegate(final FunctionDelegate<T, R> delegate, final T t, final Callback<R> callback) {
        asyncJobs.add(executorService.submit(() -> {
            final R result = delegate(delegate, t);

            if (callback != null)
                callback.accept(result);
        }));
    }

    public static <T, U, R> void delegate(final BiFunctionDelegate<T, U, R> delegate, final T t, final U u,
                                          final Callback<R> callback) {
        asyncJobs.add(executorService.submit(() -> {
            final R result = delegate(delegate, t, u);

            if (callback != null)
                callback.accept(result);
        }));
    }

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

package net.uoit.rmd;

import lombok.Synchronized;
import net.uoit.rmd.asm.DependencyManager;
import net.uoit.rmd.delegate.*;
import net.uoit.rmd.messages.JobRequest;
import net.uoit.rmd.messages.JobResponse;
import net.uoit.rmd.messages.MigrationRequest;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unchecked")
public class Rmd {

    private static final Map<Object, Serializable> delegateMap = Collections.synchronizedMap(new HashMap<>());
    private static final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    private static final Set<String> classes = Collections.synchronizedSet(new HashSet<>());
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5050;
    private static Connection connection;

    @Synchronized
    private static void connect() throws IOException {
        if (connection == null || connection.getSocket().isClosed()) {
            connection = new Connection(new Socket(SERVER_ADDRESS, SERVER_PORT));
            new Thread(connection).start();
        }
    }

    @Synchronized
    private static void migrate(final DelegateInfo info) throws IOException {
        if (!classes.contains(info.getDefiningClass().getName())) {
            final Map<String, byte[]> deps = DependencyManager.getDependencies(info.getDefiningClass());

            for (final String aClass : classes) {
                deps.remove(aClass);
            }

            classes.addAll(deps.keySet());
            classes.add(info.getDefiningClass().getName());

            if (deps.isEmpty())
                return;

            final MigrationRequest mr = new MigrationRequest(deps);
            connection.send(mr);
        }
    }

    private static <E extends Throwable> void sneakyThrow(final Throwable e) throws E {
        throw (E) e;
    }

    private static Object invokeDelegate(final DelegateInfo info, final Object[] array) {
        Throwable toThrow;

        while (true) {
            try {

                // todo; revise disconnect protocol
                // avoid infinite reconnect
                // invoke locally after some time...

                connect();
                migrate(info);

                final byte[] args = conf.asByteArray(array);
                final JobResponse response = connection.send(new JobRequest(info.getMethodHash(), args));
                final Throwable exception = response.getException();

                if (exception instanceof InvocationTargetException) {
                    toThrow = ((InvocationTargetException) exception).getTargetException();
                    break;
                } else if (exception != null) {
                    toThrow = exception;
                    break;
                }

                return response.getResult();
            } catch (IOException ignored) {
            }
        }

        sneakyThrow(toThrow);
        return null;
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
        executorService.submit(() -> {
            final R result = delegate(delegate);

            if (callback != null)
                callback.accept(result);
        });
    }

    public static <T, R> void delegate(final FunctionDelegate<T, R> delegate, final T t, final Callback<R> callback) {
        executorService.submit(() -> {
            final R result = delegate(delegate, t);

            if (callback != null)
                callback.accept(result);
        });
    }

    public static <T, U, R> void delegate(final BiFunctionDelegate<T, U, R> delegate, final T t, final U u,
                                          final Callback<R> callback) {
        executorService.submit(() -> {
            final R result = delegate(delegate, t, u);

            if (callback != null)
                callback.accept(result);
        });
    }

    public static <T, U, V, R> void delegate(final TriFunctionDelegate<T, U, V, R> delegate, final T t, final U u,
                                             final V v, final Callback<R> callback) {
        executorService.submit(() -> {
            final R result = delegate(delegate, t, u, v);

            if (callback != null)
                callback.accept(result);
        });
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

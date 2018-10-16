package net.uoit.rmd;

import net.uoit.rmd.asm.DependencyManager;
import net.uoit.rmd.delegate.*;
import net.uoit.rmd.messages.JobRequest;
import net.uoit.rmd.messages.JobResponse;
import net.uoit.rmd.messages.MigrationRequest;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.*;

public class Rmd {

    private static final Map<Serializable, DelegateInfo> delegateMap = Collections.synchronizedMap(new HashMap<>());
    private static final Set<Class> remoteClasses = Collections.synchronizedSet(new HashSet<>());
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5050;
    private static Connection connection;

    private static void connect() throws IOException {
        if (connection == null || connection.getSocket().isClosed()) {
            connection = new Connection(new Socket(SERVER_ADDRESS, SERVER_PORT));
            new Thread(connection).start();
        }
    }

    private static <E extends Throwable> void sneakyThrow(final Throwable e) throws E {
        throw (E) e;
    }

    private static void migrate(final DelegateInfo info) throws IOException {
        if (!remoteClasses.contains(info.getDefiningClass())) {
            final MigrationRequest mr = new MigrationRequest(DependencyManager.getDependencies(info.getDefiningClass()));
            connection.send(mr);
            remoteClasses.add(info.getDefiningClass());
        }
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

                final JobResponse response = connection.send(new JobRequest(info.getMethodHash(), array));
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

    public static <R> ProducerDelegate<R> asDelegate(final ProducerDelegate<R> delegate) {
        final DelegateInfo info = getDelegateInfo(delegate);

        return () -> (R) invokeDelegate(info, new Object[0]);
    }

    public static <T> ConsumerDelegate<T> asDelegate(final ConsumerDelegate<T> delegate) {
        final DelegateInfo info = getDelegateInfo(delegate);
        return (t) -> invokeDelegate(info, new Object[] {t});
    }

    public static <T, R> FunctionDelegate<T, R> asDelegate(final FunctionDelegate<T, R> delegate) {
        final DelegateInfo info = getDelegateInfo(delegate);

        return (t) -> (R) invokeDelegate(info, new Object[]{t});
    }

    public static <T, U, R> BiFunctionDelegate<T, U, R> asDelegate(final BiFunctionDelegate<T, U, R> delegate) {
        final DelegateInfo info = getDelegateInfo(delegate);

        return (t, u) -> (R) invokeDelegate(info, new Object[]{t, u});
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

    private static DelegateInfo getDelegateInfo(final Serializable methodReference) {
        final DelegateInfo info = delegateMap.get(methodReference);

        if (info != null)
            return info;

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

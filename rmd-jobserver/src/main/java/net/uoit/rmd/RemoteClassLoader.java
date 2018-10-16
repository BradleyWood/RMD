package net.uoit.rmd;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RemoteClassLoader extends ClassLoader {

    private final Map<String, byte[]> classes = new HashMap<>();

    public RemoteClassLoader(final ClassLoader parent, final Map<String, byte[]> classes) {
        super(parent);
        this.classes.putAll(classes);
    }

    public RemoteClassLoader() {
        this(ClassLoader.getSystemClassLoader(), Collections.emptyMap());
    }

    public void addClass(final String name, final byte[] def) {
        classes.put(name, def);
    }

    @Override
    protected Class<?> findClass(final String name) {
        final byte[] classBytes = classes.remove(name);

        if (classBytes != null) {
            return defineClass(name, classBytes, 0, classBytes.length);
        }

        return null;
    }
}

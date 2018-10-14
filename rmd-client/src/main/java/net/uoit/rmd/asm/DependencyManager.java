package net.uoit.rmd.asm;

import org.objectweb.asm.ClassReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DependencyManager {

    public static Map<String, byte[]> getDependencies(final Class clazz) throws IOException {
        return getAllDependencies(new HashSet<>(), clazz.getClassLoader(), clazz.getName());
    }

    public static Map<String, byte[]> getDependencies(final String clazz) throws IOException {
        return getAllDependencies(new HashSet<>(), ClassLoader.getSystemClassLoader(), clazz);
    }

    private static Map<String, byte[]> getAllDependencies(final Set<String> mappedClasses, final ClassLoader loader,
                                                          final String className) throws IOException {
        final Map<String, byte[]> map = new HashMap<>();

        if (mappedClasses.contains(className))
            return map;

        final byte[] bytes = getBytes(loader, className);
        final Set<String> dependencies = getDependencies(bytes);

        mappedClasses.add(className);

        if (dependencies.contains(className))
            map.put(className, bytes);

        for (String dependency : dependencies) {
            map.putAll(getAllDependencies(mappedClasses, loader, dependency));
        }

        return map;
    }

    private static byte[] getBytes(final ClassLoader loader, final String className) throws IOException {
        final InputStream in = loader.getResourceAsStream(className.replace(".", File.separator) + ".class");
        return readFully(in);
    }

    private static Set<String> getDependencies(final byte[] bytes) {
        final ClassReader cr = new ClassReader(bytes);

        final ClassDependencyVisitor dcv = new ClassDependencyVisitor();
        cr.accept(dcv, ClassReader.SKIP_DEBUG);

        return dcv.getDependencies();
    }

    private static byte[] readFully(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int len;
        final byte[] buf = new byte[1024];

        while ((len = inputStream.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }

        return baos.toByteArray();
    }
}

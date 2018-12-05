package net.uoit.rmd.asm;

import org.objectweb.asm.ClassReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static net.uoit.rmd.asm.DependencySet.PACKAGE_FILTER;

public class DependencyManager {

    /**
     * Traverses the dependency graph of the specified class file and detects
     * any dependencies that are not part of the Java or Kotlin runtime environments.
     *
     * @param clazz The class to check
     * @return A map of dependencies (key=class name, value=class file)
     * @throws IOException If there are errors reading the class files.
     */
    public static Map<String, byte[]> getDependencies(final Class clazz) throws IOException {
        return getAllDependencies(new HashSet<>(), clazz.getName());
    }

    /**
     * Traverses the dependency graph of the specified class file and detects
     * any dependencies that are not part of the Java or Kotlin runtime environments.
     *
     * @param clazz The fully qualified class name
     * @return A map of dependencies (key=class name, value=class file)
     * @throws IOException If there are errors reading the class files.
     */
    public static Map<String, byte[]> getDependencies(final String clazz) throws IOException {
        return getAllDependencies(new HashSet<>(), clazz);
    }

    private static Map<String, byte[]> getAllDependencies(final Set<String> mappedClasses, final String className)
            throws IOException {
        final Map<String, byte[]> map = new HashMap<>();

        if (mappedClasses.contains(className))
            return map;

        final byte[] bytes = getBytes(className);
        final Set<String> dependencies = getDependencies(bytes);

        mappedClasses.add(className);

        for (final String dependency : dependencies) {
            map.putAll(getAllDependencies(mappedClasses, dependency));
        }

        for (final String s : PACKAGE_FILTER) {
            if (className.startsWith(s))
                return map;
        }

        map.put(className, bytes);

        return map;
    }

    private static byte[] getBytes(final String className) throws IOException {
        final InputStream in = Object.class.getResourceAsStream("/" + className.replace('.', '/') + ".class");
        return readFully(in);
    }

    private static Set<String> getDependencies(final byte[] bytes) {
        final ClassReader cr = new ClassReader(bytes);

        final ClassDependencyVisitor dcv = new ClassDependencyVisitor();
        cr.accept(dcv, ClassReader.SKIP_DEBUG);

        return new HashSet<>(dcv.getDependencies());
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

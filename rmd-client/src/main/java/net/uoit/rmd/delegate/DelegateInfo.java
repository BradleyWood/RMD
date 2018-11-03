package net.uoit.rmd.delegate;

import lombok.Data;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Represents callsite information for a delegate function
 */
public @Data class DelegateInfo {

    private final Class definingClass;
    private final String name;
    private final String signature;
    private int idx;

    public DelegateInfo(final Class clazz, final String name, final String signature) {
        this.definingClass = clazz;
        this.name = name;
        this.signature = signature;

        getMethodIdx();
    }

    public DelegateInfo(final Method method) {
        definingClass = method.getDeclaringClass();
        name = method.getName();
        signature = Type.getMethodDescriptor(method);

        int i = 0;

        for (final Method m : definingClass.getMethods()) {
            if (method == m) {
                idx = i;
                break;
            }
            i++;
        }
    }

    private void getMethodIdx() {
        boolean found = false;
        int i = 0;

        final Method[] methods = definingClass.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::toString));

        for (final Method declaredMethod : methods) {
            if (signature.equals(Type.getMethodDescriptor(declaredMethod))){
                idx = i;
                found = true;
                break;
            }

            i++;
        }

        if (!found) {
            throw new RuntimeException("No such delegate: " + name + " " + signature);
        }
    }
}

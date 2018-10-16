package net.uoit.rmd.delegate;

import jdk.internal.org.objectweb.asm.Type;
import lombok.Data;

import java.lang.reflect.Method;

/**
 * Represents callsite information for a delegate function
 */
public @Data class DelegateInfo {

    private final Class definingClass;
    private final String name;
    private final String signature;
    private Integer hash;

    public DelegateInfo(final Class clazz, final String name, final String signature) {
        this.definingClass = clazz;
        this.name = name;
        this.signature = signature;
    }

    public DelegateInfo(final Method method) {
        definingClass = method.getDeclaringClass();
        name = method.getName();
        signature = Type.getMethodDescriptor(method);
        hash = method.hashCode();
    }

    /**
     * The hash of the callsite
     *
     * @return The hash used for remote invocation
     */
    public int getMethodHash() {
        if (hash != null)
            return hash;

        for (final Method declaredMethod : definingClass.getDeclaredMethods()) {
            if (signature.equals(Type.getMethodDescriptor(declaredMethod))){
                hash = declaredMethod.hashCode();
                break;
            }
        }

        if (hash == null) {
            throw new RuntimeException("No such delegate: " + name + " " + signature);
        }

        return hash;
    }
}

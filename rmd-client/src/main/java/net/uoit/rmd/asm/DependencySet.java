package net.uoit.rmd.asm;

import lombok.Getter;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a set of dependencies that class
 * may have
 */
class DependencySet {

    private final @Getter Set<String> dependencies = new HashSet<>();

    private final List<String> FILTER = Arrays.asList(
            "java.",
            "javax.",
            "net.uoit.rmd."
    );

    public void addDesc(final String desc) {
        if (!desc.contains("L")) // primitive
            return;

        Type type = Type.getType(desc);

        if (desc.contains("["))
            type = type.getElementType();

        addInternalName(type.getInternalName());
    }

    public void addInternalName(final String internalName) {
        final String name = internalName.replace("/", ".");

        for (final String s : FILTER) {
            if (name.startsWith(s))
                return;
        }

        dependencies.add(name);
    }

    public void addMethod(final String methodDesc) {
        final Type type = Type.getMethodType(methodDesc);
        final String retType = type.getReturnType().getDescriptor();

        addDesc(retType);

        final Type[] argumentTypes = type.getArgumentTypes();

        for (final Type argumentType : argumentTypes) {
            addDesc(argumentType.getDescriptor());
        }
    }

}

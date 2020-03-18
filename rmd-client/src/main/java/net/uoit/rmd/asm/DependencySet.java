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

    /**
     * A list of packages to filter out because they should not be migrated
     * as they already exists on the server side.
     */
    public static final List<String> PACKAGE_FILTER = Arrays.asList(
            "java.",
            "javax.",
            "net.uoit.rmd.",
            "kotlin."
    );

    /**
     * Add a dependency to the set based on the type descriptor
     *
     * @param desc The type descriptor
     */
    public void addDesc(final String desc) {
        if (desc == null || !desc.contains("L")) // primitive
            return;

        Type type = Type.getType(desc);

        if (desc.contains("["))
            type = type.getElementType();

        addInternalName(type.getInternalName());
    }

    /**
     * Add dependency by its internal name
     *
     * @param internalName The internal name of the class
     */
    public void addInternalName(final String internalName) {
        if (internalName == null)
            return;

        String name = internalName;

        if (internalName.contains("("))
            throw new RuntimeException(internalName);

        if (internalName.contains("[")) {
            final Type elementType = Type.getType(internalName).getElementType();

            if (isPrimitive(elementType))
                return;

            name = elementType.getInternalName();
        }

        name = name.replace("/", ".");

        for (final String s : PACKAGE_FILTER) {
            if (name.startsWith(s) && !name.endsWith("NonDeterministic"))
                return;
        }

        dependencies.add(name);
    }

    /**
     * Adds a method to the dependency set
     *
     * @param methodDesc The method descriptor
     */
    public void addMethod(final String methodDesc) {
        final Type type = Type.getMethodType(methodDesc);
        final String retType = type.getReturnType().getDescriptor();

        addDesc(retType);

        final Type[] argumentTypes = type.getArgumentTypes();

        for (final Type argumentType : argumentTypes) {
            addDesc(argumentType.getDescriptor());
        }
    }

    private boolean isPrimitive(final Type type) {
        return Type.VOID_TYPE.equals(type) || Type.BYTE_TYPE.equals(type) || Type.SHORT_TYPE.equals(type) ||
                Type.INT_TYPE.equals(type) || Type.LONG_TYPE.equals(type) || Type.FLOAT_TYPE.equals(type) ||
                Type.DOUBLE_TYPE.equals(type) || Type.CHAR_TYPE.equals(type) || Type.BOOLEAN_TYPE.equals(type);

    }
}

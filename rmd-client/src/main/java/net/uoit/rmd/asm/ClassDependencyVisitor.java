package net.uoit.rmd.asm;

import org.objectweb.asm.*;

import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM5;

public class ClassDependencyVisitor extends ClassVisitor {

    private static final DependencySet dependencies = new DependencySet();

    public ClassDependencyVisitor() {
        super(ASM5);
    }

    public Set<String> getDependencies() {
        return dependencies.getDependencies();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        dependencies.addInternalName(superName);

        if (interfaces != null) {
            for (final String anInterface : interfaces) {
                dependencies.addInternalName(anInterface);
            }
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        dependencies.addInternalName(owner);
        dependencies.addMethod(desc);

        super.visitOuterClass(owner, name, desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        dependencies.addDesc(desc);

        final AnnotationVisitor av = super.visitAnnotation(desc, visible);

        return new AnnotationDependencyVisitor(av, dependencies);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        dependencies.addDesc(desc);

        final AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);

        return new AnnotationDependencyVisitor(av, dependencies);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        dependencies.addInternalName(outerName);

        if (outerName != null && innerName != null)
            dependencies.addInternalName(outerName + "$" + innerName);

        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        dependencies.addDesc(desc);

        final FieldVisitor fv = super.visitField(access, name, desc, signature, value);

        return new FieldDependencyVisitor(fv, dependencies);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (exceptions != null) {
            for (final String exception : exceptions) {
                dependencies.addInternalName(exception);
            }
        }

        dependencies.addMethod(desc);

        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        return new MethodDependencyVisitor(mv, dependencies);
    }
}

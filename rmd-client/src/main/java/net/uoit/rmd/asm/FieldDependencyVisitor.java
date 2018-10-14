package net.uoit.rmd.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

import static org.objectweb.asm.Opcodes.ASM5;

public class FieldDependencyVisitor extends FieldVisitor {

    private final DependencySet dependencySet;

    public FieldDependencyVisitor(final FieldVisitor fv, final DependencySet dependencySet) {
        super(ASM5, fv);
        this.dependencySet = dependencySet;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        dependencySet.addDesc(desc);

        final AnnotationVisitor av = super.visitAnnotation(desc, visible);

        return new AnnotationDependencyVisitor(av, dependencySet);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        dependencySet.addDesc(desc);

        final AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);

        return new AnnotationDependencyVisitor(av, dependencySet);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}

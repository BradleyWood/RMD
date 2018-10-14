package net.uoit.rmd.asm;

import org.objectweb.asm.AnnotationVisitor;

import static org.objectweb.asm.Opcodes.ASM5;

public class AnnotationDependencyVisitor extends AnnotationVisitor {

    private final DependencySet dependencySet;

    public AnnotationDependencyVisitor(final AnnotationVisitor av, final DependencySet dependencySet) {
        super(ASM5, av);
        this.dependencySet = dependencySet;
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        super.visitEnum(name, desc, value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        dependencySet.addDesc(desc);

        final AnnotationVisitor av = super.visitAnnotation(name, desc);

        return new AnnotationDependencyVisitor(av, dependencySet);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        final AnnotationVisitor av = super.visitArray(name);

        return new AnnotationDependencyVisitor(av, dependencySet);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}

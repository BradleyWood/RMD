package net.uoit.rmd.asm;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.ASM5;

public class MethodDependencyVisitor extends MethodVisitor {

    private final DependencySet dependencySet;

    public MethodDependencyVisitor(final MethodVisitor mv, final DependencySet dependencySet) {
        super(ASM5, mv);
        this.dependencySet = dependencySet;
    }

    @Override
    public void visitParameter(String name, int access) {
        super.visitParameter(name, access);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return new AnnotationDependencyVisitor(super.visitAnnotationDefault(), dependencySet);
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
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        dependencySet.addDesc(desc);

        final AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);

        return new AnnotationDependencyVisitor(av, dependencySet);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        super.visitFrame(type, nLocal, local, nStack, stack);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        dependencySet.addInternalName(type);
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        dependencySet.addInternalName(owner);
        dependencySet.addDesc(desc);

        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        dependencySet.addInternalName(owner);
        dependencySet.addMethod(desc);

        super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        dependencySet.addInternalName(owner);
        dependencySet.addMethod(desc);

        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        dependencySet.addMethod(desc);
        dependencySet.addDesc(bsm.getOwner());
        dependencySet.addMethod(bsm.getDesc());

        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        dependencySet.addDesc(desc);

        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        dependencySet.addDesc(desc);

        final AnnotationVisitor av = super.visitInsnAnnotation(typeRef, typePath, desc, visible);

        return new AnnotationDependencyVisitor(av, dependencySet);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        dependencySet.addInternalName(type);

        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        dependencySet.addDesc(desc);

        final AnnotationVisitor av = super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);

        return new AnnotationDependencyVisitor(av, dependencySet);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        dependencySet.addDesc(desc);

        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
        dependencySet.addDesc(desc);

        final AnnotationVisitor av = super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);

        return new AnnotationDependencyVisitor(av, dependencySet);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}

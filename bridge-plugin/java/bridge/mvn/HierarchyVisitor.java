package bridge.mvn;

import bridge.asm.HierarchyScanner;
import bridge.asm.TypeMap;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

final class HierarchyVisitor extends HierarchyScanner {
    private static final String[] EMPTY = new String[0];
    private final ArrayList<String> synthetic = new ArrayList<>();
    private boolean adopting;
    private String signature;

    HierarchyVisitor(TypeMap types) {
        super(types);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (!visible) {
            if (desc.equals("Lbridge/Adopt;")) {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    LinkedList<String> implemented;
                    String extended;
                    String signature;
                    boolean clean;

                    @Override
                    public void visit(String name, Object value) {
                        if (name.equals("clean")) {
                            clean = (boolean) value;
                        } else if (name.equals("parent")) {
                            extended = ((Type) value).getInternalName();
                        } else {
                            signature = value.toString();
                        }
                    }

                    @Override
                    public AnnotationVisitor visitArray(String name) {
                        if (implemented == null) implemented = new LinkedList<>();
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            public void visit(String name, Object value) {
                                implemented.add(((Type) value).getInternalName());
                            }
                        };
                    }

                    @Override
                    public void visitEnd() {
                        int i;
                        if (implemented != null && implemented.size() != 0) {
                            if (!clean && HierarchyVisitor.super.implemented != null && (i = HierarchyVisitor.super.implemented.length) != 0) {
                                String[] array = HierarchyVisitor.super.implemented = Arrays.copyOf(HierarchyVisitor.super.implemented, i + implemented.size(), String[].class);
                                for (String element : implemented) array[i++] = element;
                            } else {
                                HierarchyVisitor.super.implemented = implemented.toArray(new String[0]);
                            }
                        } else if (clean) {
                            HierarchyVisitor.super.implemented = EMPTY;
                        }
                        if (extended != null) {
                            HierarchyVisitor.super.extended = extended;
                        } else if (clean) {
                            HierarchyVisitor.super.extended = "java/lang/Object";
                        }
                        HierarchyVisitor.this.signature = signature;
                        HierarchyVisitor.this.adopting = true;
                    }
                };
            } else if (desc.equals("Lbridge/Synthetic;")) {
                synthetic.add("");
                return null;
            }
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return new FieldVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (desc.equals("Lbridge/Synthetic;")) {
                    synthetic.add(name);
                    return null;
                }
                return super.visitAnnotation(desc, visible);
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (desc.equals("Lbridge/Synthetic;")) {
                    synthetic.add(name + descriptor);
                    return null;
                }
                return super.visitAnnotation(desc, visible);
            }
        };
    }

    @Override
    public void visitEnd() {
        AdjustedType type;
        types.add(type = new AdjustedType(super.type));
        super.visitEnd();
        type.synthetic = Collections.unmodifiableCollection(synthetic);
        type.adopting = adopting;
        type.signature = signature;
    }
}

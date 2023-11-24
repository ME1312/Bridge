package bridge.mvn;

import bridge.asm.KnownType;
import bridge.asm.TypeMap;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static bridge.asm.Types.*;
import static org.objectweb.asm.Opcodes.*;

final class BridgeVisitor extends ClassVisitor {
    private static final int ACC_VALID = ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC | ACC_TRANSIENT | ACC_VARARGS;
    private Map<BridgeAnnotation.Data, Field> staticFields;
    private Map<BridgeAnnotation.Data, Field> fields;
    private boolean isInterface, clinit, init;
    final TypeMap types;

    KnownType type;
    AdjustmentData adjust;
    HashMap<Integer, Boolean> forks = new HashMap<>();
    String adopt, name, src = "Unknown Source";
    int version, bridges, invocations, adjustments;

    BridgeVisitor(ClassVisitor parent, TypeMap types) {
        super(ASM9, parent);
        this.types = types;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String extended, String[] implemented) {
        KnownType type = this.type = types.load(Type.getObjectType(this.name = name));
        AdjustmentData adjust = this.adjust = (AdjustmentData) type.data();
        if (adjust != null) {
            if (adjust.adopting) {
                KnownType[] interfaces = type.interfaces();
                extended = adopt = type.supertype().type.getInternalName();
                if (implemented.length != interfaces.length) implemented = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; ++i) implemented[i] = interfaces[i].type.getInternalName();
                signature = adjust.signature;
            }
            if (adjust.synthetic.contains("")) {
                access |= ACC_SYNTHETIC;
            }
        }
        this.isInterface = (access & ACC_INTERFACE) != 0;
        this.forks.put(this.version = version - 44, Boolean.FALSE);
        super.visit(version, access, name, signature, extended, implemented);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (!visible && (descriptor.equals("Lbridge/Adopt;") || descriptor.equals("Lbridge/Synthetic;"))) {
            ++adjustments;
            return null;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(this.src = source, debug);
    }

    private abstract class Field extends FieldVisitor {
        final KnownType returns;
        final String name, desc;
        final int access;

        private Field(int access, String name, String descriptor, String signature, Object value) {
            super(ASM9, BridgeVisitor.super.visitField(access, name, descriptor, signature, value));
            this.access = access;
            this.name = name;
            this.desc = descriptor;
            this.returns = types.load(Type.getType(descriptor));
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (adjust != null && adjust.synthetic.contains(name)) access |= ACC_SYNTHETIC;
        return new Field(access, name, descriptor, signature, value) {
            private ArrayList<BridgeAnnotation.Data> bridges;

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (!visible) {
                    if (desc.equals("Lbridge/Bridges;") || desc.equals("Lbridge/Bridge;")) {
                        if (bridges == null) bridges = new ArrayList<>();
                        return new BridgeAnnotation(access | ACC_TRANSIENT | ACC_SYNTHETIC, name, descriptor, returns.type, data -> bridges.add(data));
                    } else if (desc.equals("Lbridge/Synthetic;")) {
                        ++adjustments;
                        return null;
                    }
                }
                return super.visitAnnotation(desc, visible);
            }

            private Map<BridgeAnnotation.Data, Field> initStatic() {
                if (clinit) throw new IllegalStateException("Cannot @Bridge static fields after <clinit>: " + this.name.replace('/', '.') + '.' + name + '(' + src + ')');
                if (staticFields == null) staticFields = new HashMap<>();
                return staticFields;
            }

            private Map<BridgeAnnotation.Data, Field> initVirtual() {
                if (init) throw new IllegalStateException("Cannot @Bridge instance fields after <init>: " + this.name.replace('/', '.') + '.' + name + '(' + src + ')');
                if (fields == null) fields = new HashMap<>();
                return fields;
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
                final int ACC_VALID = ((access & ACC_STATIC) != 0)? BridgeVisitor.ACC_VALID : BridgeVisitor.ACC_VALID & ~ACC_STATIC;
                if (bridges != null) {
                    boolean safe, constant = (access & ACC_FINAL) != 0;
                    for (BridgeAnnotation.Data data : bridges) {
                        safe = constant && returns.type.equals(data.returns);
                        FieldVisitor fv = cv.visitField(data.access &= ACC_VALID, data.name, data.desc, data.sig, (safe)? value : null);
                        data.annotate(fv::visitAnnotation);
                        fv.visitEnd();
                        if (!safe || value == null) {
                            (((data.access & ACC_STATIC) != 0)? initStatic() : initVirtual()).put(data, this);
                        }
                    }
                    BridgeVisitor.this.bridges += bridges.size();
                }
            }
        };
    }

    public MethodVisitor visitMethod(int $, String name, String descriptor, String signature, String[] exceptions) {
        final int access = (adjust != null && adjust.synthetic.contains(name + descriptor))? $ | ACC_SYNTHETIC : $;
        return new InvocationVisitor(this, access, name, descriptor, new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            private final KnownType returns = types.load(Type.getReturnType(descriptor));
            private ArrayList<BridgeAnnotation.Data> bridges;
            private final boolean amend = name.equals("<init>");
            private final boolean adopt = BridgeVisitor.this.adopt != null;
            private boolean safe = true;
            private int occ = 0;

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (!visible) {
                    if (desc.equals("Lbridge/Bridges;") || desc.equals("Lbridge/Bridge;")) {
                        if (bridges == null) bridges = new ArrayList<>();
                        return new BridgeAnnotation(access | ACC_SYNTHETIC, name, descriptor, returns.type, data -> bridges.add(data));
                    } else if (desc.equals("Lbridge/Synthetic;")) {
                        ++adjustments;
                        return null;
                    }
                }
                return super.visitAnnotation(desc, visible);
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                if (opcode == NEW) ++occ;
                super.visitTypeInsn(opcode, type);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                if (opcode == INVOKESPECIAL && name.equals("<init>")) {
                    if (occ > 0) {
                        --occ;
                    } else if (amend) {
                        if (BridgeVisitor.this.name.equals(owner)) {
                            safe = false;
                        } else if (adopt) {
                            super.visitMethodInsn(opcode, BridgeVisitor.this.adopt, name, desc, isInterface);
                            return;
                        }
                    }
                }
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            }

            @Override
            public void visitInsn(int opcode) {
                if (opcode == RETURN) {
                    Map<BridgeAnnotation.Data, Field> map;
                    if (amend) {
                        init = true;
                        map = fields;
                    } else if (name.equals("<clinit>")) {
                        clinit = true;
                        map = staticFields;
                    } else {
                        map = null;
                    }

                    if (map != null) {
                        BridgeAnnotation.Data data; Field field;
                        for (Entry<BridgeAnnotation.Data, Field> entry : map.entrySet()) {
                            data = entry.getKey();
                            if (safe || (data.access & ACC_FINAL) == 0) {
                                field = entry.getValue();
                                super.visitLabel(new Label());
                                int get = GETSTATIC, set = PUTSTATIC;
                                if ((data.access & ACC_STATIC) == 0) {
                                    super.visitVarInsn(ALOAD, 0);
                                    set = PUTFIELD;
                                    if ((field.access & ACC_STATIC) == 0) {
                                        super.visitInsn(DUP);
                                        get = GETFIELD;
                                    }
                                }
                                super.visitFieldInsn(get, BridgeVisitor.this.name, field.name, field.desc);
                                cast(mv, field.returns, types.load(data.returns));
                                super.visitFieldInsn(set, BridgeVisitor.this.name, data.name, data.desc);
                            }
                        }
                    }
                }
                super.visitInsn(opcode);
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
                if (bridges != null) {
                    final int ACC_VALID = ((access & ACC_STATIC) != 0)? BridgeVisitor.ACC_VALID : BridgeVisitor.ACC_VALID & ~ACC_STATIC;
                    final int ACC_COMPATIBLE = (access & ACC_SYNCHRONIZED) | ((amend)? 0 : ACC_BRIDGE);
                    final KnownType[] METHOD = types.load(Type.getArgumentTypes(descriptor));
                    for (BridgeAnnotation.Data data : bridges) {
                        MethodVisitor mv = cv.visitMethod(data.access = (data.access & ACC_VALID) | ACC_COMPATIBLE, data.name, data.desc, data.sig, exceptions);
                        data.annotate(mv::visitAnnotation);
                        mv.visitCode();
                        mv.visitLabel(new Label());
                        KnownType[] params = types.load(Type.getArgumentTypes(data.desc));

                        int size = 0;
                        int invoke = INVOKESTATIC;
                        if ((data.access & ACC_STATIC) == 0) {
                            if ((access & ACC_STATIC) == 0) {
                                mv.visitVarInsn(ALOAD, 0);
                                invoke = INVOKESPECIAL;
                            }
                            size = 1;
                        }

                        // bridge -> method
                        int fromIndex = data.fromIndex;
                        final int length = Math.min(Math.min(METHOD.length, params.length), fromIndex + data.length);
                        size += size(params, 0, Math.min(fromIndex, params.length));

                        int toIndex = 0;
                        while (toIndex < data.toIndex) {
                            cast(mv, VOID_TYPE, METHOD[toIndex++].type);
                        }
                        for (KnownType from; fromIndex < length; ++fromIndex, ++toIndex) {
                            mv.visitVarInsn((from = params[fromIndex]).type.getOpcode(ILOAD), size);
                            size += size(from);
                            cast(mv, from, METHOD[toIndex]);
                        }
                        while (toIndex < METHOD.length) {
                            cast(mv, VOID_TYPE, METHOD[toIndex++].type);
                        }

                        // method -> bridge
                        mv.visitMethodInsn(invoke, BridgeVisitor.this.name, name, descriptor, isInterface);
                        cast(mv, this.returns, types.load(data.returns));
                        mv.visitInsn(data.returns.getOpcode(IRETURN));
                        mv.visitMaxs(0, 0);
                        mv.visitEnd();
                    }
                    BridgeVisitor.this.bridges += bridges.size();
                }
            }
        });
    }

    private void visitSpecial(int access, String name) {
        MethodVisitor mv = this.visitMethod(access, name, "()V", null, null);
        mv.visitCode();
        mv.visitLabel(new Label());
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @Override
    public void visitEnd() {
        if (!init && fields != null) this.visitSpecial(ACC_PUBLIC, "<init>");
        if (!clinit && staticFields != null) this.visitSpecial(ACC_STATIC, "<clinit>");
        super.visitEnd();
    }
}

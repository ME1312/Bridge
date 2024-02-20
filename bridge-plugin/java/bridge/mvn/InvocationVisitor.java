package bridge.mvn;

import bridge.asm.ArrayType;
import bridge.asm.KnownType;
import bridge.asm.QueuedVisitor;
import bridge.asm.TypeMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static bridge.asm.Types.*;
import static org.objectweb.asm.Opcodes.*;

// The function of this class is to insert invocation visitors between itself and its parent when necessary.
// Workflows that usage of this class often create include:
//
// <input> -> Analyzer/Listener (this) -> BridgeVisitor -> <output>
// <input> -> Analyzer/Listener (this) -> Possible Invocation -> BridgeVisitor -> <output>
// <input> -> Analyzer/Listener (this) -> Possible Invocation B (inner) -> Possible Invocation A (outer) -> BridgeVisitor -> <output>
final class InvocationVisitor extends AnalyzerAdapter {
    private final TypeMap types;
    private final BridgeVisitor caller;
    private final String method;
    private Map<Object, Map.Entry<Label, Integer>> labels;
    private int line;

    InvocationVisitor(BridgeVisitor caller, int access, String name, String descriptor, MethodVisitor parent) {
        super(ASM9, caller.name, access, name, descriptor, parent);
        this.types = caller.types;
        this.caller = caller;
        this.method = name;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(this.line = line, start);
    }

    private RuntimeException exception(String message) {
        StringBuilder str = new StringBuilder(message).append(": at ")
                .append(caller.name.replace('/', '.')).append('.').append(method)
                .append('(').append(caller.src);
        if (line != 0) str.append(':').append(line);
        return new IllegalStateException(str.append(')').toString());
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        if (opcode == GETSTATIC && "bridge/Invocation".equals(owner) && "LANGUAGE_LEVEL".equals(name)) {
            mv = new MethodVisitor(ASM9, mv) {
                private int version;

                @Override
                public void visitIntInsn(int opcode, int operand) {
                    if (opcode == BIPUSH || opcode == SIPUSH) {
                        version = operand;
                    }
                    super.visitIntInsn(opcode, operand);
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof Integer) {
                        version = (int) value;
                    }
                    super.visitLdcInsn(value);
                }

                @Override
                public void visitJumpInsn(int opcode, Label label) {
                    int target = this.version;
                    super.visitJumpInsn(opcode, label);
                    switch (opcode) {
                        case IFEQ:
                        case IFNE:
                        case IFGT:
                        case IFLE:
                        case IFLT:
                        case IFGE:
                            target = 0;
                        case IF_ICMPEQ: // != target
                        case IF_ICMPNE: // == target
                            fork(target);
                        case IF_ICMPGT: // <= target
                        case IF_ICMPLE: // >  target
                            ++target;
                        case IF_ICMPLT: // >= target
                        case IF_ICMPGE: // <  target
                            fork(target);
                            InvocationVisitor.this.mv = mv;
                    }
                }

                private void fork(int target) {
                    if (caller.forks.putIfAbsent(target, Boolean.TRUE) == null) {
                        if (target < 9) throw exception("Multi-release jar files are not supported at language level " + target);
                        if (target < caller.version) throw exception("Class version " + (caller.version + 44) + ".0 is not supported at language level " + target);
                    }
                }

                @Override
                public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
                public void visitMaxs(int maxStack, int maxLocals) {}
                public void visitEnd() {
                    throw exception("Method ended despite incomplete fork operation");
                }
            };
        }
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor, boolean isInterface) {
        if ((opcodeAndSource & ~SOURCE_MASK) != INVOKESTATIC || !"bridge/Unchecked".equals(owner) || !"check".equals(name) || !"()V".equals(descriptor)) {
            super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (labels != null) for (Map.Entry<Object, Map.Entry<Label, Integer>> label : labels.entrySet()) {
            if (label.getValue().getValue() != null) {
                Object id = label.getKey();
                line = label.getValue().getValue();
                throw exception("Attempted jump to undefined label [" + ((id instanceof CharSequence)? '"'+id.toString()+'"' : id.toString()) + ']');
            }
        }

        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == NEW) {
            final boolean jump;
            if ((jump = "bridge/Jump".equals(type)) || "bridge/Label".equals(type)) mv = new MethodVisitor(ASM9, mv) {
                private Object id;
                private int one;
                private int state = 0x00;

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    if (state == 0x00 && opcode == NEW) {
                        state = 0x01;
                        one = stack.size() + 2;
                    } else {
                        super.visitTypeInsn(opcode, type);
                    }
                }

                @Override
                public void visitInsn(int opcode) {
                    if (state == 0x01 && opcode == DUP) {
                        state = 0x02;
                        ++one;
                    } else if (state <= 0x02 && (ICONST_M1 <= opcode && opcode <= ICONST_5) && id == null) {
                        id = opcode - ICONST_0;
                    } else if (state == 0x03 && (opcode == POP || opcode == ATHROW || opcode == ARETURN)) {
                        exit();
                    } else {
                        super.visitInsn(opcode);
                    }
                }

                @Override
                public void visitIntInsn(int opcode, int operand) {
                    if (state <= 0x02 && (opcode == BIPUSH || opcode == SIPUSH) && id == null) {
                        id = operand;
                    } else {
                        super.visitIntInsn(opcode, operand);
                    }
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (state <= 0x02 && id == null) {
                        id = value;
                    } else {
                        super.visitLdcInsn(value);
                    }
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (state <= 0x02 && stack.size() == one && descriptor.endsWith(")V")) {
                        if (id == null) throw exception("No label name provided");
                        if (labels == null) labels = new HashMap<>();
                        Map.Entry<Label, Integer> label = labels.computeIfAbsent(id, k -> new Map.Entry<Label, Integer>() {
                            private final Label label = new Label();
                            private Integer line = InvocationVisitor.this.line;

                            @Override
                            public Label getKey() {
                                return label;
                            }

                            @Override
                            public Integer getValue() {
                                return line;
                            }

                            @Override
                            public Integer setValue(Integer value) {
                                return line = value;
                            }
                        });
                        if (jump) {
                            mv.visitJumpInsn(GOTO, label.getKey());
                        } else {
                            if (label.getValue() == null) throw exception("Attempted redefinition of existing label [" + ((id instanceof CharSequence)? '"'+ id.toString()+'"' : id.toString()) + ']');
                            mv.visitLabel(label.getKey());
                            label.setValue(null);
                        }
                        if (state == 0x02) {
                            state = 0x03;
                        } else exit();
                    } else {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                }

                private void exit() {
                    InvocationVisitor.this.mv = mv;
                    if (jump) ++caller.invocations;
                    state = 0x04;
                }

                @Override
                public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
                public void visitMaxs(int maxStack, int maxLocals) {}
                public void visitEnd() {
                    throw exception("Method ended despite incomplete label operation [0x0" + state + ']');
                }
            };
            else if ("bridge/Unchecked".equals(type)) mv = new MethodVisitor(ASM9, mv) {
                private int one;
                private Boolean state;

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    if (state == null && opcode == NEW) {
                        state = Boolean.FALSE;
                        one = stack.size() + 2;
                    } else {
                        super.visitTypeInsn(opcode, type);
                    }
                }

                @Override
                public void visitInsn(int opcode) {
                    if (state != Boolean.TRUE && opcode == DUP) {
                        state = Boolean.TRUE;
                        ++one;
                    } else {
                        super.visitInsn(opcode);
                    }
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (stack.size() == one && descriptor.endsWith(")V")) {
                        if (state == Boolean.TRUE) stack.set(one - 3, stack.get(one - 1));
                        InvocationVisitor.this.mv = mv;
                        ++caller.adjustments;
                    } else {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                }

                @Override
                public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
                public void visitMaxs(int maxStack, int maxLocals) {}
                public void visitEnd() {
                    throw exception("Method ended despite incomplete throwable operation [" + Objects.toString(state).toUpperCase(Locale.ROOT) + ']');
                }
            };
            else if ("bridge/Invocation".equals(type)) mv = new QueuedVisitor(ASM9, mv) {
                private int ldi, zero, virtual;
                private Object ldc;
                private Type primitive;
                private String name;
                private KnownType owner, returns, casted = types.load(OBJECT_TYPE);
                private final LinkedList<Type> params = new LinkedList<>();
                private Supplier<KnownType> defaults = () -> casted;
                private int state = 0x00;

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    if (state == 0x00 && opcode == NEW) {
                        state = 0x01;
                        zero = stack.size() + 1;
                        return;
                    } else if (state == 0x06 && opcode == CHECKCAST) {
                        casted = types.loadClass(type);
                        return;
                    }
                    super.visitTypeInsn(opcode, type);
                }

                @Override
                public void visitInsn(int opcode) {
                    if (state == 0x01 && opcode == DUP) {
                        state = 0x02;
                        ++zero;
                        return;
                    } else if (state == 0x03 && opcode == ACONST_NULL) {
                        {
                            throw exception("Illegal null invocation constant");
                        }
                    } else if (state <= 0x05 && opcode == ACONST_NULL) {
                        if (ldc == null && stack.size() == zero) {
                            ldi = ops.size();
                            ldc = new Object() {
                                @Override
                                public String toString() {
                                    throw exception("Illegal null invocation constant");
                                }
                            };
                        } else {
                            primitive = VOID_TYPE;
                        }
                    } else if (state == 0x06 && (opcode == POP || opcode == POP2)) {
                        casted = types.load(VOID_TYPE);
                        queue(() -> {});
                        return;
                    }
                    super.visitInsn(opcode);
                }

                public void queue(Runnable operation) {
                    if (state <= 0x05) {
                        primitive = null;
                        super.queue(operation);
                    } else if (state == 0x06) {
                        state = 0x07; // this is a hack that temporarily hides the internal top known stack value ...
                        final Object hack = (stack.size() == 0)? null : stack.remove(stack.size() - 1);
                        if (returns == null) returns = defaults.get();
                        for (Runnable op : ops) op.run();
                        cast(mv, returns, casted);        // ... fixing input and type detection for nested invocations
                        if (hack != null) stack.add((hack.equals("java/lang/Object"))? box(returns.type).getInternalName() : hack);
                        InvocationVisitor.this.mv = mv;
                        ++caller.invocations;
                        operation.run();
                    } else {
                        throw new IllegalStateException(Integer.toHexString(state));
                    }
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    Type primitive;
                    if (state <= 0x05 && opcode == GETSTATIC && ldc == null && stack.size() == zero && name.equals("TYPE") && (primitive = get(sort(Type.getObjectType(owner)))) != null) {
                        ldi = ops.size();
                        ldc = primitive;
                    }
                    super.visitFieldInsn(opcode, owner, name, descriptor);
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (state <= 0x05 && ldc == null && stack.size() == zero) {
                        ldi = ops.size();
                        ldc = value;

                    } else if (state == 0x03) {
                        state = 0x04;
                        name = Objects.toString(value);
                        return;
                    }
                    super.visitLdcInsn(value);
                }

                private KnownType consume() {
                    final Object ldc;
                    if ((ldc = this.ldc) == null) throw exception("Illegal dynamic invocation constant");
                    this.ops.remove(ldi);
                    this.ldc = null;
                    if (ldc instanceof KnownType) {
                        return (KnownType) ldc;
                    } else if (ldc instanceof Type) {
                        return types.load((Type) ldc);
                    } else {
                        final String str;
                        if ((str = ldc.toString().replace('.', '/')).length() != 0) {
                            return types.loadClass(str);
                        } else {
                            throw exception("Illegal empty invocation constant");
                        }
                    }
                }

                private KnownType stack(int sort, int index) {
                    return types.load(get(sort, () -> Type.getObjectType(stack.get(zero + index).toString())));
                }

                private boolean special(String desc) {
                    final KnownType owner;
                    if (!(owner = this.owner).isArray()) {
                        if (!owner.isInterface()) {
                            return (owner.equals(caller.type) && (
                                    caller.adjust == null || (caller.adjust.access.getOrDefault(this.name + desc, ACC_PRIVATE) & ACC_PRIVATE) != 0)
                            ) || (!caller.type.isInterface() && owner.equals(caller.type.supertype()));
                        }
                        final KnownType[] interfaces = caller.type.interfaces();
                        for (int i = 0, length = interfaces.length; i != length; ++i) {
                            if (owner.equals(interfaces[i])) return true;
                        }
                    }
                    return false;
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (state <= 0x02) {
                        int params = (descriptor.length() >= 22)? 2 : 1;
                        if (stack.size() == params + zero && descriptor.endsWith(")V")) {
                            if (descriptor.equals("(Ljava/lang/Object;)V")) {
                                this.owner = stack(OBJECT_SORT, 0);
                                virtual = ops.size();
                                ldc = null;
                                ldi = 0;
                            } else {
                                if (this.ldc == null) throw exception("Attempted invocation on unknown type");
                                if ((this.owner = consume()).isPrimitive()) {
                                    throw exception("Attempted invocation on primitive type");
                                }
                                if (params == 2) {
                                    KnownType type = stack(OBJECT_SORT, 1);
                                    virtual = (ldi = ops.size()) + 1;
                                    queue(() -> cast(mv, type, this.owner));
                                }
                            }
                            --zero;
                            state = 0x03;
                            return;
                        }
                    } else if (state <= 0x04) {
                        if (this.ldc != null) {
                            if (this.name == null) {
                                this.ops.remove(ldi);
                                this.name = ldc.toString();
                                this.ldc = null;
                            } else {
                                this.returns = consume();
                            }
                        }

                        switch (name) {
                            case "ofMethod":
                                if (this.name == null || this.name.length() == 0) {
                                    throw exception("No method name provided");
                                }
                                break;
                            case "ofField":
                                if (this.name == null || this.name.length() == 0)
                                    throw exception("No field name provided");
                                if (returns != null && returns.type.getSort() == VOID_SORT) {
                                    throw exception("Attempted access of void field");
                                }
                                break;
                            case "ofConstructor":
                                if (virtual != 0)
                                    throw exception("Attempted reinvocation of constructor");
                                if (!(returns = this.owner).isArray()) queue(() -> {
                                    mv.visitTypeInsn(NEW, returns.type.getInternalName());
                                    if (casted.type.getSort() == VOID_SORT) returns = casted;
                                    else mv.visitInsn(DUP);
                                });
                                this.name = null;
                                break;
                            case "ofInstanceOf":
                                if (virtual == 0)
                                    throw exception("Attempted instanceof without instance");
                                if (ldi != 0) {
                                    this.ops.remove(ldi);
                                }
                                queue(() -> mv.visitTypeInsn(INSTANCEOF, this.owner.type.getInternalName()));
                                casted = returns = types.load(BOOLEAN_TYPE);
                                state = 0x06;
                                return;
                            case "ofClassLiteral":
                                if (virtual != 0) queue(() ->
                                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
                                );else queue(() -> {
                                    if (casted.type.getSort() == VOID_SORT) throw exception("Invocation does not result in a statement");
                                    mv.visitLdcInsn(this.owner.type);
                                });
                                casted = returns = types.load(CLASS_TYPE);
                                state = 0x06;
                                return;
                            default:
                                throw exception("Unknown invocation type [." + name + "()]");
                        }
                        state = 0x05;
                        return;
                    } else if (state == 0x05) {
                        Type[] params = Type.getArgumentTypes(descriptor);

                        if (stack.size() == size(params) + zero) {
                            if (owner.equals("bridge/Invocation$Executor")) {
                                if (name.equals("with")) {
                                    if (params.length == 1) {
                                        ldc = null;
                                        this.params.add(stack(params[0].getSort(), 0).type);
                                    } else if (primitive == VOID_TYPE) {
                                        this.params.add(consume().type);
                                    } else {
                                        final KnownType param, arg;
                                        if ((param = consume()).type == VOID_TYPE) {
                                            throw exception("Attempted use of void argument");
                                        }
                                        final Type primitive;
                                        if ((primitive = this.primitive) != null) {
                                            this.ops.removeLast();
                                            this.primitive = null;
                                            arg = types.load(primitive);
                                        } else {
                                            arg = stack(OBJECT_SORT, 1);
                                        }
                                        this.params.add(param.type);
                                        queue(() -> cast(mv, arg, param));
                                    }

                                    // Additional checks required for safe array initialization
                                    if (this.name == null && this.owner.isArray()) {
                                        if (((ArrayType) this.owner).depth < this.params.size()) {
                                            throw exception("Array depth exceeds type declaration");
                                        } else {
                                            final Type type;
                                            if ((type = this.params.getLast()).getSort() != INT_SORT) {
                                                queue(() -> cast(mv, type, INT_TYPE));
                                            }
                                        }
                                    }
                                } else if (name.equals("check")) {
                                    if (ldc != null) {
                                        this.ops.remove(ldi);
                                        this.ldc = null;
                                    }
                                } else if (name.equals("invoke")) {
                                    if (this.name != null) {
                                        final String desc = Type.getMethodDescriptor(returns.type, this.params.toArray(new Type[0]));
                                        final int INVOKE;
                                        if (virtual == 0) {
                                            INVOKE = INVOKESTATIC; // static method
                                        } else if (special(desc)) {
                                            INVOKE = INVOKESPECIAL; // supertype/private method
                                        } else if (this.owner.isInterface()) {
                                            INVOKE = INVOKEINTERFACE; // interface method
                                        } else {
                                            INVOKE = INVOKEVIRTUAL; // instance method
                                        }

                                        queue(() -> mv.visitMethodInsn(INVOKE, this.owner.type.getInternalName(), this.name, desc, this.owner.isInterface()));
                                    } else if (!this.owner.isArray()) { // constructor
                                        queue(() -> mv.visitMethodInsn(INVOKESPECIAL, this.owner.type.getInternalName(), "<init>", Type.getMethodDescriptor(VOID_TYPE, this.params.toArray(new Type[0])), false));
                                    } else {
                                        queue(() -> { // array constructor
                                            if (casted.type.getSort() == VOID_SORT) throw exception("Invocation does not result in a statement");
                                            final int depth;
                                            if ((depth = this.params.size()) == 0) {
                                                throw exception("Attempted array creation with undefined length");
                                            } else if (depth == 1) {
                                                final Type type;
                                                switch ((type = ((ArrayType) this.owner).element.type).getSort()) {
                                                    case BOOLEAN_SORT:
                                                        mv.visitIntInsn(NEWARRAY, T_BOOLEAN);
                                                        break;
                                                    case CHAR_SORT:
                                                        mv.visitIntInsn(NEWARRAY, T_CHAR);
                                                        break;
                                                    case BYTE_SORT:
                                                        mv.visitIntInsn(NEWARRAY, T_BYTE);
                                                        break;
                                                    case SHORT_SORT:
                                                        mv.visitIntInsn(NEWARRAY, T_SHORT);
                                                        break;
                                                    case INT_SORT:
                                                        mv.visitIntInsn(NEWARRAY, T_INT);
                                                        break;
                                                    case FLOAT_SORT:
                                                        mv.visitIntInsn(NEWARRAY, T_FLOAT);
                                                        break;
                                                    case LONG_SORT:
                                                        mv.visitIntInsn(NEWARRAY, T_LONG);
                                                        break;
                                                    case DOUBLE_SORT:
                                                        mv.visitIntInsn(NEWARRAY, T_DOUBLE);
                                                        break;
                                                    default:
                                                        mv.visitTypeInsn(ANEWARRAY, type.getInternalName());
                                                }
                                            } else {
                                                mv.visitMultiANewArrayInsn(this.owner.type.getInternalName(), depth);
                                            }
                                        });
                                    }
                                    state = 0x06;
                                } else {
                                    throw exception("Unknown invoke operation [." + name + "()]");
                                }
                                return;
                            } else if (owner.equals("bridge/Invocation$Accessor")) {
                                if (params.length == 0) {
                                    if (name.equals("get")) {
                                        queue(() -> {
                                            if (casted.type.getSort() == VOID_SORT) throw exception("Invocation does not result in a statement");
                                            if (!this.owner.isArray() || virtual == 0 || !this.name.equals("length")) {
                                                mv.visitFieldInsn((virtual == 0)? GETSTATIC : GETFIELD, this.owner.type.getInternalName(), this.name, returns.type.getDescriptor());
                                            } else {
                                                mv.visitInsn(ARRAYLENGTH);
                                                returns = types.load(INT_TYPE);
                                            }
                                        });
                                    } else {
                                        throw exception("Unknown read operation [." + name + "()]");
                                    }
                                } else if (params.length == 1) {
                                    final KnownType arg = stack(params[0].getSort(), 0);
                                    defaults = () -> (casted.type.getSort() == VOID_SORT)? arg : casted;

                                    final int index = ops.size();
                                    queue(() -> cast(mv, arg, returns));
                                    queue(() -> mv.visitFieldInsn((virtual == 0)? PUTSTATIC : PUTFIELD, this.owner.type.getInternalName(), this.name, returns.type.getDescriptor()));

                                    if (name.equals("getAndSet")) {
                                        ops.add(virtual, () -> {
                                            final int size;
                                            if ((size = size(casted)) != VOID_SORT) {
                                                if (virtual == 0) {
                                                    mv.visitFieldInsn(GETSTATIC, this.owner.type.getInternalName(), this.name, returns.type.getDescriptor());
                                                } else {
                                                    mv.visitInsn(DUP);
                                                    mv.visitFieldInsn(GETFIELD, this.owner.type.getInternalName(), this.name, returns.type.getDescriptor());
                                                    if (size(returns) != 1) { // use switch(pair()) instead if new data categories are added
                                                        if (size != 1) {
                                                            mv.visitInsn(DUP2_X1);
                                                            mv.visitInsn(POP2);
                                                            return;
                                                        }
                                                        cast(mv, returns, casted);
                                                        casted = returns;
                                                    }
                                                    mv.visitInsn(SWAP);
                                                }
                                            } else {// disable casting
                                                casted = returns;
                                            }
                                        });
                                    } else {
                                        Consumer<KnownType> dup = type -> {
                                            if (casted.type.getSort() != VOID_SORT) {
                                                if (virtual == 0) {
                                                    mv.visitInsn((size(type) == 2)? DUP2 : DUP);
                                                } else {
                                                    mv.visitInsn((size(type) == 2)? DUP2_X1 : DUP_X1);
                                                }
                                            } else {// disable casting
                                                casted = type;
                                            }
                                        };

                                        if (name.equals("set")) {
                                            casted = arg;
                                            defaults = () -> arg;
                                            ops.add(() -> returns = arg);
                                            ops.add(index, () -> dup.accept(arg));
                                        } else if (name.equals("setAndGet")) {
                                            ops.add(index + 1, () -> dup.accept(returns));
                                        } else {
                                            throw exception("Unknown write operation [." + name + "()]");
                                        }
                                    }
                                } else {
                                    throw exception("Too many arguments for field access");
                                }
                                ldc = null;
                                state = 0x06;
                                return;
                            }
                        } else if (name.equals("valueOf") && params.length == 1 && params[0].getSort() < ARRAY_SORT && sort(Type.getObjectType(owner)) < ARRAY_SORT) {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            primitive = params[0];
                            return;
                        }
                    } else if (state == 0x06) {
                        final Type primitive;
                        if (name.endsWith("Value") && (primitive = Type.getReturnType(descriptor)).getSort() < ARRAY_SORT && sort(Type.getObjectType(owner)) < ARRAY_SORT) {
                            casted = types.load(primitive);
                            return;
                        }
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }

                @Override
                public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
                public void visitMaxs(int maxStack, int maxLocals) {}
                public void visitEnd() {
                    throw exception("Method ended despite incomplete invocation [0x0" + state + ']');
                }
            };
        }
        super.visitTypeInsn(opcode, type);
    }
}

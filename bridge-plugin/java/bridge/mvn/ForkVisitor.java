package bridge.mvn;

import bridge.asm.QueuedVisitor;
import org.objectweb.asm.*;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public final class ForkVisitor extends ClassVisitor {
    static final byte NO_LINE_NUMBERS    = 0b00001;
    static final byte NO_MODULE_VERSIONS = 0b00010;
    static final byte NO_SOURCE_NAMES    = 0b00100;
    static final byte NO_SOURCE_EXT      = 0b01000;
    static final byte NO_NAMED_LOCALS    = 0b10000;

    private final BridgeVisitor caller;
    private final boolean names, lines;
    private final int version, flags;

    ForkVisitor(ClassVisitor parent, BridgeVisitor caller, int version, int flags) {
        super(ASM9, parent);
        this.caller = caller;
        this.version = version;
        this.flags = flags;
        this.names = (flags & NO_NAMED_LOCALS) == 0;
        this.lines = (flags & NO_LINE_NUMBERS) == 0;
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        return super.visitModule(name, access, ((flags & NO_MODULE_VERSIONS) == 0)? version : null);
    }

    @Override
    public void visitSource(String source, String debug) {
        final int flags;
        if (((flags = this.flags) & NO_SOURCE_NAMES) == 0 || (flags & NO_SOURCE_EXT) == 0) {
            super.visitSource(((flags & NO_SOURCE_NAMES) == 0)? source : null, ((flags & NO_SOURCE_EXT) == 0)? debug : null);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new Fork(name, new Clean(super.visitMethod(access, name, descriptor, signature, exceptions)));
    }

    private final class Fork extends MethodVisitor {
        private final String method;
        private int line;

        private Fork(String name, MethodVisitor parent) {
            super(ASM9, parent);
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
            return new IllegalStateException(str.append(") [target:").append(version).append(']').toString());
        }

        @Override
        public void visitEnd() {
            try {
                super.visitEnd();
            } catch (RuntimeException thrown) {
                Throwable root = thrown;
                for (Throwable cause = root.getCause(); cause != null; cause = cause.getCause()) {
                    root = cause;
                }
                root.initCause(root = exception("Failed recompiling method"));
                root.setStackTrace(new StackTraceElement[0]);
                throw thrown;
            }
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode == GETSTATIC && "bridge/Invocation".equals(owner) && name.equals("LANGUAGE_LEVEL")) {
                mv = new MethodVisitor(ASM9, mv) {
                    private int version;

                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        if (opcode == BIPUSH || opcode == SIPUSH) {
                            version = operand;
                        } else {
                            super.visitIntInsn(opcode, operand);
                        }
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Integer) {
                            version = (int) value;
                        } else {
                            super.visitLdcInsn(value);
                        }
                    }

                    @Override
                    public void visitJumpInsn(int opcode, Label label) {
                        switch (opcode) {
                            case IF_ICMPEQ:
                                if (ForkVisitor.this.version == version) super.visitJumpInsn(GOTO, label);
                                break;
                            case IF_ICMPNE:
                                if (ForkVisitor.this.version != version) super.visitJumpInsn(GOTO, label);
                                break;
                            case IF_ICMPGT:
                                if (ForkVisitor.this.version >  version) super.visitJumpInsn(GOTO, label);
                                break;
                            case IF_ICMPLE:
                                if (ForkVisitor.this.version <= version) super.visitJumpInsn(GOTO, label);
                                break;
                            case IF_ICMPLT:
                                if (ForkVisitor.this.version <  version) super.visitJumpInsn(GOTO, label);
                                break;
                            case IF_ICMPGE:
                                if (ForkVisitor.this.version >= version) super.visitJumpInsn(GOTO, label);
                                break;
                            default:
                                super.visitJumpInsn(opcode, label);
                                return;
                        }
                        Fork.this.mv = mv;
                    }

                    @Override
                    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
                    public void visitMaxs(int maxStack, int maxLocals) {}
                    public void visitEnd() {
                        throw exception("Method ended despite incomplete fork operation");
                    }
                };
            } else {
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }
        }
    }

    // --- general code cleanup ---
    // > rewrites branches
    // > merges labels
    // > removes unreachable bytecode
    // > removes user-specified debugging elements
    private final class Clean extends QueuedVisitor {
        private final HashMap<Label, LinkedHashMap<Runnable, Block>> handlers = new HashMap<>();
        private final LinkedList<Block> blocks = new LinkedList<>();
        private Block block;
        private Runnable line;

        private final class Block {
            private LinkedList<Label> labels;
            private LinkedList<Block> branches;
            private LinkedList<Runnable> handlers, ops, attrs;
            private Block merge, next;
            private boolean used;
            private int fop;

            private Block dereference() {
                Block block = this;
                while (block.merge != null) block = block.merge;
                return block;
            }

            private void merge(Block next) {
                if (next.labels != null) {
                    if (labels == null) {
                        labels = next.labels;
                    } else {
                        labels.addAll(next.labels);
                    }
                }
                if (next.branches != null) {
                    if (branches == null) {
                        branches = next.branches;
                    } else {
                        branches.addAll(next.branches);
                    }
                }
                if (next.attrs != null) {
                    if (attrs == null) {
                        attrs = next.attrs;
                    } else {
                        attrs.addAll(next.attrs);
                    }
                }
                if (next.ops != null) ops = next.ops;
                this.fop = next.fop;
                this.next = next.next;
                next.merge = this;
                used = next.used;
            }

            private boolean merged(Block next) {
                for (Block block = this;;) {
                    if ((block = block.dereference()) == next) return true;
                    if (!block.ops.isEmpty()) return false;
                    if ((block = block.next) == null) return true;
                }
            }
        }

        private Clean(MethodVisitor parent) {
            super(ASM9, parent);
            blocks.add(this.block = new Block());
            block.ops = ops;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            final Block block;
            blocks.add(this.block = block = new Block());
            block.ops = ops = new LinkedList<>();
            block.used = true;
            super.visitMaxs(maxStack, maxLocals);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (lines) this.line = () -> super.visitLineNumber(line, start);
        }

        @Override
        public void visitOperation(int opcode, Runnable op) {
            final Runnable line;
            if ((line = this.line) != null) {
                this.line = null;
                line.run();
            }
            super.visitOperation(opcode, op);
        }

        @Override
        public void visitParameter(String name, int access) {
            if (names) super.visitParameter(name, access);
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            if (names) {
                final Block block = (end.info != null)? (Block) end.info : new Block();
                if (block.attrs == null) block.attrs = new LinkedList<>();
                block.attrs.add(() -> {
                    if (!((Block) start.info).merged(block.dereference())) {
                        mv.visitLocalVariable(name, descriptor, signature, start, end, index);
                    }
                });
                end.info = block;
            }
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            final Block sb = (start.info != null)? (Block) start.info : new Block();
            if (sb.handlers == null) sb.handlers = new LinkedList<>();
            sb.handlers.add(() -> {
                final Block hb = (handler.info != null)? (Block) handler.info : new Block();
                handlers.computeIfAbsent(end, k -> new LinkedHashMap<>()).put(() -> {
                    if (!sb.merged(((Block) end.info).dereference())) {
                        mv.visitTryCatchBlock(start, end, handler, type);
                    }
                }, hb);
                handler.info = hb;
            });
            start.info = sb;
        }

        @Override
        public void visitLabel(Label label) {
            LinkedHashMap<Runnable, Block> handlers = this.handlers.remove(label);
            Block block = this.block;
            if (label.info != null) {
                final Block info = (Block) label.info;
                if (info.handlers != null) for (Runnable handler : info.handlers) {
                    handler.run();
                }
                if (!ops.isEmpty() || block.fop != 0) {
                    blocks.add(this.block = block = block.next = info);
                    block.ops = ops = new LinkedList<>();
                }
                else block.merge(info);
            } else {
                if (!ops.isEmpty() || block.fop != 0) {
                    blocks.add(this.block = block = block.next = new Block());
                    block.ops = ops = new LinkedList<>();
                }
            }
            if (!this.handlers.isEmpty()) {
                if (block.branches == null) block.branches = new LinkedList<>();
                for (Iterator<LinkedHashMap<Runnable, Block>> it = this.handlers.values().iterator(); it.hasNext();) {
                    block.branches.addAll(it.next().values());
                }
            }
            if (handlers != null) {
                if (block.attrs == null) block.attrs = new LinkedList<>();
                block.attrs.addAll(handlers.keySet());
            }
            if (block.labels == null) block.labels = new LinkedList<>();
            block.labels.add(label);
            label.info = block;
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode >= IRETURN && opcode <= RETURN || opcode == ATHROW) {
                block.fop = opcode;
                blocks.add(this.block = new Block());
                block.ops = ops = new LinkedList<>();
                return;
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            if (opcode == GOTO) {
                label.info = block.next = (label.info != null)? (Block) label.info : new Block();
                blocks.add(this.block = new Block());
                block.ops = ops = new LinkedList<>();
                return;
            }
            super.visitJumpInsn(opcode, label);
            branch(label);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            super.visitTableSwitchInsn(min, max, dflt, labels);
            for (Label label : labels) branch(label);
            branch(dflt);
            blocks.add(this.block = new Block());
            block.ops = ops = new LinkedList<>();
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            super.visitLookupSwitchInsn(dflt, keys, labels);
            for (Label label : labels) branch(label);
            branch(dflt);
            blocks.add(this.block = new Block());
            block.ops = ops = new LinkedList<>();
        }

        private void branch(Label label) {
            final Block block, info;
            if ((block = this.block).branches == null) block.branches = new LinkedList<>();
            block.branches.add(info = (label.info != null)? (Block) label.info : new Block());
            label.info = info;
        }

        @Override
        public void visitEnd() {
            LinkedList<Block> branches = new LinkedList<>();
            for (Block block = blocks.getFirst();;) {
                if (!block.used) {
                    block.used = true;
                    if (block.branches != null && !block.ops.isEmpty()) {
                        branches.addAll(block.branches);
                    }
                    if ((block = block.next) != null) {
                        block = block.dereference();
                        continue;
                    }
                }
                if (branches.isEmpty()) break;
                block = branches.removeFirst().dereference();
            }

            Block next = null;
            for (Iterator<Block> it = blocks.iterator(); it.hasNext();) {
                Block block = it.next();
                while (!block.used) {
                    block.merge(it.next());
                }
                if (next != null && (next = next.dereference()) != block) {
                    mv.visitJumpInsn(GOTO, next.labels.getFirst());
                }
                if (block.labels != null) for (Label label : block.labels) {
                    mv.visitLabel(label);
                }
                if (block.attrs != null) for (Runnable attr : block.attrs) {
                    attr.run();
                }
                for (Runnable op : block.ops) {
                    op.run();
                }
                if (block.fop != 0) {
                    mv.visitInsn(block.fop);
                }
                next = block.next;
            }
            mv.visitEnd();
        }
    }
}

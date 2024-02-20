package bridge.mvn;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.ASM9;

final class BridgeAnnotation extends AnnotationVisitor {
    private static final int MAX_ARITY = 255;
    private Map<String, Repeater> annotations;
    private final int access;
    private int $access, fromIndex, toIndex, length = MAX_ARITY;
    private final Consumer<Data> action;
    private final String name, desc;
    private String $name, $desc, sig;
    private final Type returns;
    private Type $returns;


    BridgeAnnotation(int access, String name, String descriptor, Type returns, Consumer<Data> action) {
        super(ASM9);
        this.action = action;
        this.access = $access = access;
        this.name = $name = name;
        this.desc = $desc = descriptor;
        this.returns = $returns = returns;
    }

    @Override
    public void visit(String name, Object value) {
        if ("access".equals(name)) {
            int access = (int) value;
            if (access < 0) {
                this.$access &= access;
            } else {
                this.$access = access;
            }
        } else if ("name".equals(name)) {
            $name = value.toString();
        } else if ("fromIndex".equals(name)) {
            if ((fromIndex = Math.min((int) value, MAX_ARITY)) < 0) fromIndex = 0;
        } else if ("toIndex".equals(name)) {
            if ((toIndex = Math.min((int) value, MAX_ARITY)) < 0) toIndex = 0;
        } else if ("length".equals(name)) {
            if ((length = Math.min((int) value, MAX_ARITY)) < 0) length = 0;
        } else if ("signature".equals(name)) {
            sig = value.toString();
        } else if ("returns".equals(name)) {
            String desc = $desc, returns = value.toString();
            $returns = Type.getType(returns);
            $desc = new StringBuilder(desc).replace(desc.indexOf(')') + 1, desc.length(), returns).toString();
        }
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        if ("params".equals(name)) {
            return new AnnotationVisitor(ASM9) {
                private final StringBuilder desc = new StringBuilder().append('(');

                @Override
                public void visit(String name, Object descriptor) {
                    desc.append(descriptor);
                }

                @Override
                public void visitEnd() {
                    $desc = desc.append(')').append($returns.getDescriptor()).toString();
                }
            };
        } else {
            return new AnnotationVisitor(ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    return new BridgeAnnotation(access, BridgeAnnotation.this.name, desc, returns, action);
                }
            };
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        if (annotations == null) annotations = new LinkedHashMap<>();
        return annotations.put(desc, new Repeater(new LinkedList<>()));
    }

    private final static class Repeater extends AnnotationVisitor {
        private final List<Runnable> queue;

        private Repeater(List<Runnable> queue) {
            super(ASM9);
            this.queue = queue;
        }

        private void visit(AnnotationVisitor av) {
            super.av = av;
        }

        @Override
        public void visit(String name, Object value) {
            queue.add(() -> super.visit(name, value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            queue.add(() -> super.visitEnum(name, descriptor, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            Repeater av = new Repeater(queue);
            queue.add(() -> av.av = super.visitAnnotation(name, descriptor));
            return av;
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            Repeater av = new Repeater(queue);
            queue.add(() -> av.av = super.visitArray(name));
            return av;
        }

        @Override
        public void visitEnd() {
            queue.add(super::visitEnd);
        }
    }

    @Override
    public void visitEnd() {
        if (!$name.equals(name) || !$desc.equals(desc)) {
            action.accept(new Data(annotations, $access, $name, $desc, sig, $returns, fromIndex, toIndex, length));
        }
    }

    static final class Data {
        private final Map<String, Repeater> annotations;
        int access;
        final String name, desc, sign;
        final int fromIndex, toIndex, length;
        final Type returns;

        private Data(Map<String, Repeater> annotations, int access, String name, String descriptor, String signature, Type returns, int fromIndex, int toIndex, int length) {
            this.annotations = (annotations == null)? Collections.emptyMap() : Collections.unmodifiableMap(annotations);
            this.access = access;
            this.name = name;
            this.desc = descriptor;
            this.sign = signature;
            this.returns = returns;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
            this.length = length;
        }

        void annotate(BiFunction<String, Boolean, AnnotationVisitor> code) {
            for (Map.Entry<String, Repeater> entry : annotations.entrySet()) {
                Repeater annotation = entry.getValue();
                annotation.visit(code.apply(entry.getKey(), Boolean.TRUE));
                for (Runnable op : annotation.queue) op.run();
            }
        }
    }
}

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
    private final String name, desc;
    private String $name, $desc, sign;
    private final Type returns;
    private Type $returns;
    private String[] ex;
    private final Consumer<Data> action;


    BridgeAnnotation(int access, String name, String descriptor, String[] exceptions, Type returns, Consumer<Data> action) {
        super(ASM9);
        this.access = $access = access;
        this.name = $name = name;
        this.desc = $desc = descriptor;
        this.ex = exceptions;
        this.returns = $returns = returns;
        this.action = action;
    }

    @Override
    public void visit(String name, Object value) {
        if ("access".equals(name)) {
            int access = (int) value;
            $access = (access < 0)? $access & access : access;
        } else if ("name".equals(name)) {
            $name = value.toString();
        } else if ("fromIndex".equals(name)) {
            if ((fromIndex = Math.min((int) value, MAX_ARITY)) < 0) fromIndex = 0;
        } else if ("toIndex".equals(name)) {
            if ((toIndex = Math.min((int) value, MAX_ARITY)) < 0) toIndex = 0;
        } else if ("length".equals(name)) {
            if ((length = Math.min((int) value, MAX_ARITY)) < 0) length = 0;
        } else if ("signature".equals(name)) {
            sign = value.toString();
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
        } else if ("exceptions".equals(name)) {
            return new AnnotationVisitor(ASM9) {
                private final ArrayList<String> list = new ArrayList<>();

                @Override
                public void visit(String name, Object descriptor) {
                    list.add(((Type) descriptor).getInternalName());
                }

                @Override
                public void visitEnd() {
                    ex = list.toArray(new String[0]);
                }
            };
        } else {
            return new AnnotationVisitor(ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    return new BridgeAnnotation(access, BridgeAnnotation.this.name, desc, ex, returns, action);
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
            action.accept(new Data($access, $name, $desc, fromIndex, toIndex, length, ex, sign, $returns, annotations));
        }
    }

    static final class Data {
        int access;
        final String name, desc, sign;
        final int fromIndex, toIndex, length;
        final String[] ex;
        final Type returns;
        private final Map<String, Repeater> annotations;

        private Data(int access, String name, String descriptor, int fromIndex, int toIndex, int length, String[] exceptions, String signature, Type returns, Map<String, Repeater> annotations) {
            this.access = access;
            this.name = name;
            this.desc = descriptor;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
            this.length = length;
            this.ex = exceptions;
            this.sign = signature;
            this.returns = returns;
            this.annotations = (annotations == null)? Collections.emptyMap() : Collections.unmodifiableMap(annotations);
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

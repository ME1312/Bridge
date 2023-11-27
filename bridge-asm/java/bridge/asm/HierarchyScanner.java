package bridge.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public class HierarchyScanner extends ClassVisitor {
    protected final TypeMap types;
    protected int access;
    protected Type type;
    private KnownType compiled;
    protected String extended;
    protected String[] implemented;
    protected Object data;

    public HierarchyScanner(TypeMap types) {
        super(Opcodes.ASM9);
        this.types = types;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String extended, String[] implemented) {
        this.access = access;
        this.type = Type.getObjectType(name);
        this.extended = extended;
        this.implemented = implemented;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        compile().data = data;
    }

    protected final KnownType compile() {
        KnownType type;
        if ((type = compiled) == null) {
            if (this.type == null) throw new IllegalStateException("Called to compile() before visit()");
            type = compiled = types.map.computeIfAbsent(this.type, KnownType::new);
            type.isInterface = (access & ACC_INTERFACE) != 0;
            type.extended = (extended == null)? types.get(Object.class) : types.load(Type.getObjectType(extended));
            type.data = data;

            if (implemented != null && implemented.length != 0) {
                KnownType[] types = new KnownType[implemented.length];
                for (int i = 0; i < implemented.length; ++i) {
                    types[i] = this.types.load(Type.getObjectType(implemented[i]));
                }
                type.implemented = types;
            } else {
                type.implemented = KnownType.EMPTY;
            }
        }
        return type;
    }
}

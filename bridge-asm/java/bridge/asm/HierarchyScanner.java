package bridge.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public class HierarchyScanner extends ClassVisitor {
    protected final TypeMap types;
    private KnownType type;
    protected Object adjust;
    protected String extended;
    protected String[] implemented;

    public HierarchyScanner(TypeMap types) {
        super(Opcodes.ASM9);
        this.types = types;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String extended, String[] implemented) {
        (this.type = types.map.computeIfAbsent(Type.getObjectType(name), KnownType::new)).isInterface = (access & ACC_INTERFACE) != 0;
        this.extended = extended;
        this.implemented = implemented;
    }

    @Override
    public void visitEnd() {
        KnownType type;
        (type = this.type).adjust = adjust;
        type.extended = (extended == null)? types.get(Object.class) : types.load(Type.getObjectType(extended));

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
}

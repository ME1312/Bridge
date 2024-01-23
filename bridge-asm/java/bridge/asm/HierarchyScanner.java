package bridge.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Objects;

public class HierarchyScanner extends ClassVisitor {
    private KnownType compiled;
    protected final TypeMap types;
    protected int access;
    protected String name, extended;
    protected String[] implemented;
    protected Object data;

    public HierarchyScanner(TypeMap types) {
        this(Opcodes.ASM9, null, types);
    }

    public HierarchyScanner(ClassVisitor parent, TypeMap types) {
        this(Opcodes.ASM9, parent, types);
    }

    protected HierarchyScanner(int api, TypeMap types) {
        this(api, null, types);
    }

    protected HierarchyScanner(int api, ClassVisitor parent, TypeMap types) {
        super(api, parent);
        this.types = Objects.requireNonNull(types);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String extended, String[] implemented) {
        super.visit(
                version,
                this.access = access,
                this.name = name,
                signature,
                this.extended = extended,
                this.implemented = implemented
        );
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        compile().data = data;
    }

    protected final KnownType compile() {
        KnownType type;
        if ((type = compiled) == null) {
            if (this.name == null) throw new IllegalStateException("Called to compile() before visit()");
            type = compiled = types.map.computeIfAbsent(Type.getObjectType(name), KnownType::new);
            type.extended = (extended == null)? types.get(Object.class) : types.loadClass(extended);
            type.implemented = types.loadClass(implemented);
            type.access = access;
            type.data = data;
        }
        return type;
    }
}

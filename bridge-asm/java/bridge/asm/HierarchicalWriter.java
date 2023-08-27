package bridge.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

public class HierarchicalWriter extends ClassWriter {
    private final TypeMap types;

    public HierarchicalWriter(TypeMap types, int flags) {
        super(flags);
        this.types = types;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        KnownType a = types.load(Type.getObjectType(type1));
        KnownType b = types.load(Type.getObjectType(type2));

        if (b.implemented(a)) {
            return type1;
        } else if (a.implemented(b)) {
            return type2;
        } else if (!a.isInterface() && !b.isInterface()) {
            do {
                a = a.supertype();
            } while (!b.extended(a));
            return a.type.getInternalName();
        } else {
            return "java/lang/Object";
        }
    }
}

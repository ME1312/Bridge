package bridge.asm;

import org.objectweb.asm.Type;

import java.util.*;

public class KnownType {
    protected static final KnownType[] EMPTY = new KnownType[0];
    private Object search;
    public final Type type;
    protected KnownType extended;
    protected KnownType[] implemented;
    protected boolean isInterface;

    protected KnownType(Type anonymous) {
        this.type = Objects.requireNonNull(anonymous);
    }

    KnownType(TypeMap types, Type type, Class<?> loaded) {
        this.type = Objects.requireNonNull(type);
        this.isInterface = loaded.isInterface();
        types.map.put(type, this);

        final Class<?> extended = loaded.getSuperclass();
        if (extended != null) {
            this.extended = types.get(extended);
        } else if (loaded != Object.class && type.getSort() == Type.OBJECT) {
            this.extended = types.get(Object.class);
        }

        final Class<?>[] interfaces = loaded.getInterfaces();
        if (interfaces.length != 0) {
            final KnownType[] implemented = new KnownType[interfaces.length];
            for (int i = 0; i < interfaces.length; ++i) {
                implemented[i] = types.get(interfaces[i]);
            }
            this.implemented = implemented;
        } else {
            this.implemented = EMPTY;
        }
    }

    public int hashCode() {
        return type.hashCode();
    }

    public boolean equals(Object type) {
        if (this == type) {
            return true;
        } else if (type instanceof Type) {
            return type.equals(this.type);
        } else if (type instanceof KnownType) {
            return ((KnownType) type).type.equals(this.type);
        } else if (type instanceof Class) {
            return Type.getType((Class<?>) type).equals(this.type);
        } else {
            return false;
        }
    }

    public KnownType supertype() {
        return extended;
    }

    public boolean extended(KnownType type) {
        final KnownType extended;
        return type != null && (
            type == this || type.type.equals(this.type) || ((extended = this.extended) != null && extended.extended(type))
        );
    }

    public KnownType[] interfaces() {
        return implemented.clone();
    }

    public boolean implemented(KnownType type) {
        return type != null && implemented(type, new Object());
    }

    private boolean implemented(KnownType type, Object search) {
        final KnownType extended;
        if (type == this || type.type.equals(this.type)) return true;
        if ((extended = this.extended) != null && extended.search != search) {
            if (extended.implemented(type, search)) return true;
            extended.search = search;
        }
        for (KnownType implemented : this.implemented) {
            if (implemented.search != search) {
                if (implemented.implemented(type, search)) return true;
                implemented.search = search;
            }
        }
        return false;
    }

    public boolean isPrimitive() {
        return type.getSort() < Type.ARRAY;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isArray() {
        return false;
    }

    private static final int INDENT = 4;
    private StringBuilder newline(StringBuilder builder, int indent) {
        builder.append('\n');
        while (indent != 0) {
            builder.append(' ');
            --indent;
        }
        return builder;
    }

    private void toString(StringBuilder builder, int indent) {
        builder.append(type.getClassName()).append(" 0x").append(Integer.toHexString(super.hashCode()).toUpperCase(Locale.ROOT)).append(" {");
        indent += INDENT;

        KnownType extended = this.extended;
        if (extended != null) {
            extended.toString(newline(builder, indent).append("extends "), indent);
        }

        KnownType[] implemented = this.implemented;
        if (implemented.length != 0) {
            newline(builder, indent).append("implements {");
            implemented[0].toString(newline(builder, indent += INDENT), indent);
            for (int i = 1; i < implemented.length; ++i) {
                implemented[i].toString(newline(builder.append(','), indent), indent);
            }
            newline(builder, indent -= INDENT).append('}');
        }

        if (this instanceof ArrayType) {
            ((ArrayType) this).element.toString(newline(builder, indent).append("return "), indent);
        }

        if (builder.charAt(builder.length() - 1) != '{')
            newline(builder, indent - INDENT);
        builder.append('}');
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder, 0);
        return builder.toString();
    }
}

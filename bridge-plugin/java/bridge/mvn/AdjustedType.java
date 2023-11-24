package bridge.mvn;

import bridge.asm.KnownType;
import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.Collections;

final class AdjustedType extends KnownType {
    Collection<String> synthetic;
    boolean adopting;
    String signature;

    AdjustedType(Type type) {
        super(type);
    }
}

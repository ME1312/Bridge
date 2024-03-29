package bridge.objects;

import bridge.Adopt;
import bridge.Synthetic;
import bridge.Unchecked;

@Adopt(clean = true)
@Synthetic
class Jester extends Super implements Adoptable {
    Jester(CharSequence cs) {
        super(cs);
    }

    @Override
    public Void v_method() {
        return null;
    }

    static void sneak() {
        throw new Unchecked(new NonException());
    }
}

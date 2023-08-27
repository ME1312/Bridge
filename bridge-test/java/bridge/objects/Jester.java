package bridge.objects;

import bridge.Adopt;
import bridge.Synthetic;

@Adopt(clean = true)
@Synthetic
class Jester extends Super implements Adoptable {
    Jester(CharSequence cs) {
        super(cs);
    }
}

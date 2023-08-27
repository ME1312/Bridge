package bridge.objects;

import bridge.Adopt;
import bridge.Bridge;
import bridge.Synthetic;

@Adopt(parent = Super.class, interfaces = Adoptable.class)
class Dummy extends Jester {
    static Super  s_obj;
    static short  sp_x1;
    static double sp_x2;

    Super  v_obj;
    short  vp_x1;
    double vp_x2;

    Dummy(String value) {
        super(value);
    }

    @Synthetic
    @Bridge(params = {})
    @Bridge(params = {boolean.class})
    @Bridge(params = {boolean.class, long.class})
    Dummy(final boolean THROW, final long VALUE, final CharSequence SEQ) throws NonException {
        super(SEQ);
        if (THROW) throw new NonException();
    }

    @Synthetic
    @Bridge(params = {})
    @Bridge(params = {}, returns = void.class)
    @Bridge(params = {boolean.class})
    @Bridge(params = {boolean.class}, returns = void.class)
    @Bridge(params = {boolean.class, long.class}, returns = void.class)
    @Bridge(params = {boolean.class, long.class, CharSequence.class})
    @Bridge(params = {boolean.class, long.class, CharSequence.class}, returns = void.class)
    @Bridge(params = {long.class}, fromIndex = 1, returns = void.class)
    long v_method(final boolean THROW, final long VALUE) throws NonException {
        if (THROW) throw new NonException();
        return VALUE;
    }

    @Synthetic
    @Bridge(params = {})
    @Bridge(params = {}, returns = void.class)
    @Bridge(params = {boolean.class})
    @Bridge(params = {boolean.class}, returns = void.class)
    @Bridge(params = {boolean.class, long.class}, returns = void.class)
    @Bridge(params = {boolean.class, long.class, CharSequence.class})
    @Bridge(params = {boolean.class, long.class, CharSequence.class}, returns = void.class)
    @Bridge(params = {long.class}, fromIndex = 1, returns = void.class)
    static long s_method(final boolean THROW, final long VALUE) throws NonException {
        if (THROW) throw new NonException();
        return VALUE;
    }
}

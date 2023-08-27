package bridge.objects;

import bridge.Invocation;
import bridge.Jump;
import bridge.Label;
import bridge.Unchecked;

@SuppressWarnings("UnusedReturnValue")
class Invocations {

    private static final Object alternate = new Dummy(null);
    private static final Dummy instance = new Dummy(null);
    private static final Super generic = Dummy.s_obj = instance.v_obj = instance;
    private static final short label = 420;

    // jump & label test
    @SuppressWarnings({"ConstantConditions", "ThrowableNotThrown"})
    static void JUMP() {
        int i = -1;
        new Label(label);
        if (++i == 5) throw new Jump("exit");
        new Jump(label);
        new Label("exit");
        assert i == 5;
    }

    // static class literals, test 1
    static void SL01() {
        assert new Invocation(Dummy.class).ofClassLiteral() == Dummy.class;
    }

    // static class literals, test 2
    static void SL02() {
        assert new Invocation("bridge.objects.Dummy").ofClassLiteral() == Dummy.class;
    }



    // static fields, object get
    static void SF01() {
        assert new Invocation(Dummy.class).ofField(Super.class, "s_obj").get() == generic;
    }

    // static fields, object get-and-set, use
    static void SF02() {
        assert new Invocation(Dummy.class).ofField(Super.class, "s_obj").getAndSet(alternate) == generic;
    }

    // static fields, object get-and-set, store
    static Super SF03() {
        Super value = new Invocation(Dummy.class).ofField("s_obj").getAndSet(generic);
        if (value != alternate) {
            assert false;
        }
        return value;
    }

    // static fields, object get-and-set, store-and-use
    static Super SF04() {
        Super value;
        if ((value = new Invocation(Dummy.class).ofField("s_obj").getAndSet(alternate)) != generic) {
            assert false;
        }
        return value;
    }

    // static fields, object set
    static void SF05() {
        assert new Invocation(Dummy.class).ofField("s_obj").set(generic) == generic;
    }

    // static fields, object set, void
    static void SF06() {
        new Invocation(Dummy.class).ofField("bridge.objects.Super", "s_obj").set(alternate);
    }

    // static fields, object set-and-get
    static void SF07() {
        assert new Invocation(Dummy.class).ofField(Super.class, "s_obj").setAndGet(generic) == generic;
    }


    // static fields, category 1 primitive get
    static void SF11() {
        assert (short) new Invocation(Dummy.class).ofField("sp_x1").get() == 0;
    }

    // static fields, category 1 primitive get-and-set, 1-to-1
    static void SF12() {
        int constant = 1;
        assert (short) new Invocation(Dummy.class).ofField("sp_x1").getAndSet(constant) == 0;
    }

    // static fields, category 1 primitive get-and-set, 1-to-2
    static void SF13() {
        int constant = 0;
        assert (long) new Invocation(Dummy.class).ofField(short.class, "sp_x1").getAndSet(constant) == 1;
    }

    // static fields, category 1 primitive get-and-set, 1-to-reference
    static void SF14() {
        int constant = 1;
        assert ((Boolean) new Invocation(Dummy.class).ofField(Short.TYPE, "sp_x1").getAndSet(constant)).compareTo(Boolean.FALSE) == 0;
    }

    // static fields, category 1 primitive set
    static void SF15() {
        int constant = 0;
        assert new Invocation(Dummy.class).ofField(short.class, "sp_x1").set(constant) == constant;
    }

    // static fields, category 1 primitive set, void
    static void SF16() {
        int constant = 1;
        new Invocation(Dummy.class).ofField(Short.TYPE, "sp_x1").set(constant);
    }

    // static fields, category 1 primitive set-and-get
    static void SF17() {
        int constant = 0;
        assert (short) new Invocation(Dummy.class).ofField("sp_x1").setAndGet(constant) == constant;
    }


    // static fields, category 2 primitive get
    static void SF21() {
        assert (double) new Invocation(Dummy.class).ofField("sp_x2").get() == 0;
    }

    // static fields, category 2 primitive get-and-set, 2-to-2
    static void SF22() {
        float constant = 1;
        assert (double) new Invocation(Dummy.class).ofField("sp_x2").getAndSet(constant) == 0;
    }

    // static fields, category 2 primitive get-and-set, 2-to-1
    static void SF23() {
        float constant = 0;
        assert (int) new Invocation(Dummy.class).ofField(double.class, "sp_x2").getAndSet(constant) == 1;
    }

    // static fields, category 2 primitive get-and-set, 2-to-reference
    static void SF24() {
        float constant = 1;
        assert ((Boolean) new Invocation(Dummy.class).ofField(Double.TYPE, "sp_x2").getAndSet(constant)).compareTo(Boolean.FALSE) == 0;
    }

    // static fields, category 2 primitive set
    static void SF25() {
        float constant = 0;
        assert new Invocation(Dummy.class).ofField(double.class, "sp_x2").set(constant) == constant;
    }

    // static fields, category 2 primitive set, void
    static void SF26() {
        float constant = 1;
        new Invocation(Dummy.class).ofField(Double.TYPE, "sp_x2").set(constant);
    }

    // static fields, category 2 primitive set-and-get
    static void SF27() {
        float constant = 0;
        assert (double) new Invocation(Dummy.class).ofField("sp_x2").setAndGet(constant) == constant;
    }



    // object instantiation, test 1
    static void SC01() {
        assert new Invocation(Dummy.class).ofConstructor().invoke() != null;
    }

    // object instantiation, test 2
    static void SC02() {
        boolean const1 = false;
        assert new Invocation(Dummy.class).ofConstructor()
                .with((boolean) const1)
                .invoke() != null;
    }

    // object instantiation, test 3
    static void SC03() {
        boolean const1 = false;
        long const2 = 64;
        assert new Invocation(Dummy.class).ofConstructor()
                .with((boolean) const1)
                .with((long) const2)
                .invoke() != null;
    }

    // object instantiation, test 4
    static void SC04() {
        boolean const1 = false;
        int const2 = 64;
        String const3 = "";
        assert new Invocation(Dummy.class).ofConstructor()
                .with((boolean) const1)
                .with((long) const2)
                .with(CharSequence.class, const3)
                .invoke() != null;
    }

    // object instantiation, test 5
    static void SC05() {
        boolean const1 = false;
        int const2 = 64;
        String const3 = "";
        try {
            new Invocation(Dummy.class).ofConstructor()
                    .with((boolean) const1)
                    .with((long) const2)
                    .with(CharSequence.class, const3)
                    .check(NonException.class)
                    .invoke();
        } catch (NonException e) {
            assert false;
        }
    }

    // object instantiation, test 6
    static void SC06() {
        boolean const1 = true;
        int const2 = 64;
        String const3 = "";
        try {
            new Invocation(Dummy.class).ofConstructor()
                    .with((boolean) const1)
                    .with((long) const2)
                    .with(CharSequence.class, const3)
                    .<NonException>check()
                    .invoke();
        } catch (NonException e) {
            return;
        }
        assert false;
    }


    // array instantiation, test 1
    static void SC11() {
        assert ((int[]) new Invocation(int[].class).ofConstructor().with(false).invoke()).length == 0;
    }

    // array instantiation, test 2
    static void SC12() {
        assert ((Dummy[]) new Invocation(Dummy[].class).ofConstructor().with((char) 1).invoke()).length == 1;
    }

    // array instantiation, test 3
    static void SC13() {
        assert ((short[][]) new Invocation(short[][].class).ofConstructor().with((byte) 3).invoke())[2] == null;
    }

    // array instantiation, test 4
    static void SC14() {
        assert ((Dummy[][]) new Invocation(Dummy[][].class).ofConstructor().with((short) 1).invoke())[0] == null;
    }

    // array instantiation, test 5
    static void SC15() {
        assert ((byte[][]) new Invocation(byte[][].class).ofConstructor().with((int) 1).with((float) 2).invoke())[0].length == 2;
    }

    // array instantiation, test 6
    static void SC16() {
        assert ((Dummy[][]) new Invocation(Dummy[][].class).ofConstructor().with((long) 2).with((double) 4).invoke())[1].length == 4;
    }



    // instance class literals, test 1
    static void VL01() {
        assert new Invocation(instance).ofClassLiteral() == Dummy.class;
    }

    // instance class literals, test 2
    static void VL02() {
        assert new Invocation(generic).ofClassLiteral() == Dummy.class;
    }



    // instance type checking, test 1
    static void VC01() {
        assert new Invocation(instance).ofInstanceOf();
    }

    // instance type checking, test 2
    static void VC02() {
        assert new Invocation(Super.class, instance).ofInstanceOf();
    }

    // instance type checking, test 3
    static void VC03() {
        assert !new Invocation(Jester.class, instance).ofInstanceOf();
    }

    // instance type checking, test 4
    static void VC04() {
        assert !new Invocation(Invocations.class, null).ofInstanceOf();
    }

    // instance type checking, test 5
    static void VC05() {
        assert new Invocation(Object[].class, new Dummy[0][]).ofInstanceOf();
    }

    // instance type checking, test 6
    static void VC06() {
        assert !new Invocation(Super[].class, new Dummy[0][]).ofInstanceOf();
    }



    // instance fields, object get
    static void VF01() {
        assert new Invocation(instance).ofField(Super.class, "v_obj").get() == generic;
    }

    // instance fields, object get-and-set, use
    static void VF02() {
        assert new Invocation(instance).ofField(Super.class, "v_obj").getAndSet(alternate) == generic;
    }

    // instance fields, object get-and-set, store
    static Super VF03() {
        Super value = new Invocation(instance).ofField("v_obj").getAndSet(generic);
        if (value != alternate) {
            assert false;
        }
        return value;
    }

    // instance fields, object get-and-set, store-and-use
    static Super VF04() {
        Super value;
        if ((value = new Invocation(instance).ofField("v_obj").getAndSet(alternate)) != generic) {
            assert false;
        }
        return value;
    }

    // instance fields, object set
    static void VF05() {
        assert new Invocation(instance).ofField("v_obj").set(generic) == generic;
    }

    // instance fields, object set, void
    static void VF06() {
        new Invocation(instance).ofField("bridge.objects.Super", "v_obj").set(alternate);
    }

    // instance fields, object set-and-get
    static void VF07() {
        assert new Invocation(instance).ofField(Super.class, "v_obj").setAndGet(generic) == generic;
    }


    // instance fields, category 1 primitive get
    static void VF11() {
        assert (short) new Invocation(instance).ofField("vp_x1").get() == 0;
    }

    // instance fields, category 1 primitive get-and-set, 1-to-1
    static void VF12() {
        int constant = 1;
        assert (short) new Invocation(instance).ofField("vp_x1").getAndSet(constant) == 0;
    }

    // instance fields, category 1 primitive get-and-set, 1-to-2
    static void VF13() {
        int constant = 0;
        assert (long) new Invocation(instance).ofField(short.class, "vp_x1").getAndSet(constant) == 1;
    }

    // instance fields, category 1 primitive get-and-set, 1-to-reference
    static void VF14() {
        int constant = 1;
        assert ((Boolean) new Invocation(instance).ofField(Short.TYPE, "vp_x1").getAndSet(constant)).compareTo(Boolean.FALSE) == 0;
    }

    // instance fields, category 1 primitive set
    static void VF15() {
        int constant = 0;
        assert new Invocation(instance).ofField(short.class, "vp_x1").set(constant) == constant;
    }

    // instance fields, category 1 primitive set, void
    static void VF16() {
        int constant = 1;
        new Invocation(instance).ofField(Short.TYPE, "vp_x1").set(constant);
    }

    // instance fields, category 1 primitive set-and-get
    static void VF17() {
        int constant = 0;
        assert (short) new Invocation(instance).ofField("vp_x1").setAndGet(constant) == constant;
    }


    // instance fields, category 2 primitive get
    static void VF21() {
        assert (double) new Invocation(instance).ofField("vp_x2").get() == 0;
    }

    // instance fields, category 2 primitive get-and-set, 2-to-2
    static void VF22() {
        float constant = 1;
        assert (double) new Invocation(instance).ofField("vp_x2").getAndSet(constant) == 0;
    }

    // instance fields, category 2 primitive get-and-set, 2-to-1
    static void VF23() {
        float constant = 0;
        assert (int) new Invocation(instance).ofField(double.class, "vp_x2").getAndSet(constant) == 1;
    }

    // instance fields, category 2 primitive get-and-set, 2-to-reference
    static void VF24() {
        float constant = 1;
        assert ((Boolean) new Invocation(instance).ofField(Double.TYPE, "vp_x2").getAndSet(constant)).compareTo(Boolean.FALSE) == 0;
    }

    // instance fields, category 2 primitive set
    static void VF25() {
        float constant = 0;
        assert new Invocation(instance).ofField(double.class, "vp_x2").set(constant) == constant;
    }

    // instance fields, category 2 primitive set, void
    static void VF26() {
        float constant = 1;
        new Invocation(instance).ofField(Double.TYPE, "vp_x2").set(constant);
    }

    // instance fields, category 2 primitive set-and-get
    static void VF27() {
        float constant = 0;
        assert (double) new Invocation(instance).ofField("vp_x2").setAndGet(constant) == constant;
    }
}

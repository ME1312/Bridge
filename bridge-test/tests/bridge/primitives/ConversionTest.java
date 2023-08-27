package bridge.primitives;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ConversionTest {

    public void test() throws Throwable {
        for (Object test : new Object[] {
                new VoidBridge(),
                new BooleanBridge(),
                new CharBridge(),
                new ByteBridge(),
                new ShortBridge(),
                new IntBridge(),
                new FloatBridge(),
                new LongBridge(),
                new DoubleBridge(),
                new BoxBridge(),
                new UnknownBridge()
        }) {
            Class<?> clazz = test.getClass();
            for (Field field : clazz.getDeclaredFields()) if ((field.getModifiers() & Modifier.PRIVATE) == 0) {
                try {
                    assert field.get(null) != null;
                } catch (AssertionError | IllegalAccessException e) {
                    throw new IllegalStateException(field.toString(), e);
                }
            }
            for (Method method : clazz.getDeclaredMethods()) if ((method.getModifiers() & Modifier.PRIVATE) == 0) {
                try {
                    if (clazz == VoidBridge.class || method.getReturnType() == Void.class || method.getReturnType() == void.class) {
                        method.invoke(test);
                    } else {
                        assert method.invoke(test) != null;
                    }
                } catch (AssertionError | IllegalAccessException e) {
                    throw new IllegalStateException(method.toString(), e);
                }
            }
        }
    }
}

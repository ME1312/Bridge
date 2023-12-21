package bridge;

/**
 * A class for interacting with unchecked exceptions
 * @implNote The constructor for this class does not actually construct anything &ndash; so, the imaginary object must be immediately thrown for your code to compile correctly.
 */
public final class Unchecked extends RuntimeException {

    /**
     * Allows any type of exception to become unchecked
     *
     * @param e Exception
     */
    public Unchecked(Throwable e) { Invocation.$(); }

    /**
     * Brings an (unchecked) exception into scope
     *
     * @param <E> Exception type
     */
    public static <E extends Throwable> void check() throws E { Invocation.$(); }
}

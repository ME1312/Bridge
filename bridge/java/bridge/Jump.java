package bridge;

/**
 * A class for teleporting to persistent labels
 * @implNote The constructor for this class does not actually construct anything &ndash; so, the imaginary object must be immediately thrown for your code to compile correctly.
 */
public final class Jump extends RuntimeException {

    /**
     * Teleports to a persistent label in this method
     *
     * @param id Label ID constant &ndash; no dynamic values or mathematics is permitted here.
     */
    public Jump(int id) { Invocation.$(); }

    /**
     * Teleports to a persistent label in this method
     *
     * @param name Label name constant &ndash; no dynamic values or string manipulation is permitted here.
     */
    public Jump(String name) { Invocation.$(name); }
}

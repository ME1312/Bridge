package bridge;

/**
 * A class for creating labels with persistent identifiers
 * @see Jump Teleport to a persistent label in the same method with <code>Jump</code>
 * @implNote The constructor for this class does not actually construct anything &ndash; so, the imaginary object must be immediately disposed of for your code to compile correctly.
 */
public final class Label {

    /**
     * Creates a persistent label at this location
     *
     * @param id Label ID constant &ndash; no dynamic values or mathematics is permitted here.
     */
    public Label(int id) {}

    /**
     * Creates a persistent label at this location
     *
     * @param name Label name constant &ndash; no dynamic values or string manipulation is permitted here.
     */
    public Label(String name) {}
}

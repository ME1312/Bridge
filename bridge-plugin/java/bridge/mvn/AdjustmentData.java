package bridge.mvn;

import java.util.ArrayList;

final class AdjustmentData {
    final ArrayList<String> synthetic = new ArrayList<>();
    String signature;
    boolean adopting;

    @Override
    public String toString() {
        return (
                "adopting = " + adopting + '\n' +
                "synthetic = " + synthetic.size() + '\n' +
                "signature = " + ((signature == null)? "null" : '"' + signature + '"')
        );
    }
}

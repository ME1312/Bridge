package bridge.mvn;

import java.util.HashMap;

final class AdjustmentData {
    final HashMap<String, Integer> access = new HashMap<>();
    boolean adopted;
    String signature;

    @Override
    public String toString() {
        return (
                "adopted = " + adopted + '\n' +
                "members = " + access.size() + '\n' +
                "signature = " + ((signature == null)? "null" : '"' + signature + '"')
        );
    }
}

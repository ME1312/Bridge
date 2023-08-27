package bridge.mvn;

import java.util.ArrayList;

final class TypeAdjustment {
    final ArrayList<String> synthetic = new ArrayList<>();
    String signature;
    boolean adopt;

    @Override
    public String toString() {
        return (
                "adopt = " + adopt + '\n' +
                "synthetic = " + synthetic.size() + '\n' +
                "signature = " + ((signature == null)? "null" : '"' + signature + '"')
        );
    }
}

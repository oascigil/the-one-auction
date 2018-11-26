//name:   VEDAuctionResult.java
package applications;
import core.DTNHost;

import java.util.HashMap;

public class VEDAuctionResult {
	public final int numberOfIterations;
    public final HashMap<DTNHost, DTNHost> X;//Assignment
    public final HashMap<DTNHost,Double> p;
    public VEDAuctionResult() {
        numberOfIterations = 0;
        X  = new HashMap();
        p  = new HashMap();
    }

    public VEDAuctionResult(int numberOfIterations,HashMap<DTNHost, DTNHost> X,HashMap<DTNHost,Double> p) {
        this.numberOfIterations = numberOfIterations;
        this.X  = X;
        this.p  = p;
    }
}

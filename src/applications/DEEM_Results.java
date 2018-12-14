//name:   DEEM_Results.java
package applications;
import core.DTNHost;

import java.util.HashMap;
import java.util.ArrayList;

public class DEEM_Results{
	public final int numberOfIterations;
    public final HashMap<DTNHost,DTNHost> userDeviceAssociation;
    public HashMap<DTNHost, DTNHost> previousUserDeviceAssociation;
    public final HashMap<DTNHost,DTNHost> deviceUserAssociation;
    public final HashMap<DTNHost,Integer> deviceLLAExecution;//Assignment
    public final HashMap<DTNHost,Double> p;
    public HashMap<DTNHost,Double> QoSGainPerUser;
    public HashMap<DTNHost,Double> QoSPerUser;
    public HashMap<DTNHost, Integer> userLLAAssociation;
    public HashMap<DTNHost, ArrayList<Integer>> deviceLLAsAssociation;
    public DEEM_Results() {
        numberOfIterations     = 0;
        userDeviceAssociation  = new HashMap();
        deviceUserAssociation  = new HashMap();
        deviceLLAExecution     = new HashMap();
        p                      = new HashMap();
        QoSGainPerUser         = new HashMap();
        QoSPerUser             = new HashMap();
    }

    public DEEM_Results(int numberOfIterations,HashMap<DTNHost,DTNHost> userDeviceAssociation,HashMap<DTNHost, DTNHost> deviceUserAssociation, HashMap<DTNHost,Integer> deviceLLAExecution,HashMap<DTNHost,Double> p,HashMap<DTNHost,Double> QoSGainPerUser,HashMap<DTNHost,Double> QoSPerUser) {
        this.numberOfIterations     = numberOfIterations;
        this.userDeviceAssociation  = new HashMap(userDeviceAssociation);
        this.deviceUserAssociation  = new HashMap(deviceUserAssociation);
        this.deviceLLAExecution     = new HashMap(deviceLLAExecution);
        this.p                      = new HashMap(p);
        this.QoSGainPerUser         = new HashMap(QoSGainPerUser);
        this.QoSPerUser             = new HashMap(QoSPerUser);
    }
}

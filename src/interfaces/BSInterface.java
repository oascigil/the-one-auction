package interfaces;

import java.util.Collection;
import core.Connection;
import core.NetworkInterface;
import core.Settings;
import core.VBRConnection;
import core.DTNHost;

public class BSInterface extends DistanceCapacityInterface {

	/**
	 * Reads the interface settings from the Settings file
	 */
    public BSInterface(Settings s) {
        super(s);
    }

	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
    public BSInterface(BSInterface ni ) {
        super(ni);

    }
	
    public NetworkInterface replicate()	{
		return new BSInterface(this);
	}
	
    /**
	 * Updates the state of current connections (i.e. tears down connections
	 * that are out of range and creates new ones).
	 */
	public void update() {
		if (optimizer == null) {
			return; /* nothing to do */
		}

		// First break the old ones
		optimizer.updateLocation(this);
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherInterface)) {
				disconnect(con,anotherInterface);
				connections.remove(i);
			}
			else {
				i++;
			}
		}
		// Then find new possible connections to Stationary nodes 
		Collection<NetworkInterface> interfaces =
			optimizer.getNearInterfaces(this);
        double min_distance = Double.POSITIVE_INFINITY;
        NetworkInterface closestInterface = null;
        DTNHost fromHost = this.getHost();
		for (NetworkInterface i : interfaces) {
            DTNHost toHost = i.getHost();
            if (toHost.is_stationary) {
                double dist = fromHost.getLocation().distance(toHost.getLocation());
                if (dist < min_distance) {
                    min_distance = dist;
                    closestInterface = i;
                }
            }
		}
        if (closestInterface != null)
    	    connect(closestInterface);

		/* update all connections */
		for (Connection con : getConnections()) {
			con.update();
		}
	}
	
    /**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "BSInterface " + super.toString();
	}
}


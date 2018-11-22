	package interfaces;

import java.util.Collection;
import core.Connection;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;
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
                DTNHost fromHost = this.getHost();
                DTNHost toHost = anotherInterface.getHost();
                DTNHost mobileHost = null;
				System.out.println(SimClock.getTime()+" Connection down from "+fromHost+" to "+toHost);

                if (!fromHost.isStationary) 
                    mobileHost = fromHost;
                else if (!toHost.isStationary) 
                    mobileHost = toHost;
                
                assert mobileHost != null : "Invalid BSInterface connection between two stationary or mobile hosts detected";

                Object retValue = DTNHost.attachmentPoints.remove(mobileHost);
                assert retValue != null : "Connection failed to be inserted into attachmentPoints mapping";
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
            if ((!fromHost.isStationary && toHost.isStationary) || (fromHost.isStationary && !toHost.isStationary)) {
                double dist = fromHost.getLocation().distance(toHost.getLocation());
                if (dist < min_distance) {
                    min_distance = dist;
                    closestInterface = i;
                }
            }
		}
        if (closestInterface != null) {
    	    connect(closestInterface);
            DTNHost toHost = closestInterface.getHost();
            DTNHost mobileHost = null;
            DTNHost stationaryHost = null;
            if (!fromHost.isStationary && toHost.isStationary) {
                mobileHost = fromHost;
                stationaryHost = toHost;
            }
            else if (!toHost.isStationary && fromHost.isStationary) {
                mobileHost = toHost;
                stationaryHost = fromHost;
            }

			//System.out.println(SimClock.getTime()+" Connection up from "+fromHost+" to "+toHost);
            DTNHost.attachmentPoints.put(mobileHost, stationaryHost);

        }

		/* update all connections */
		for (Connection con : getConnections()) {
			con.update();
		}
	}
	
	/**
	 * Tries to connect this host to another host. The other host must be
	 * active and within range of this host for the connection to succeed.
	 * @param anotherInterface The interface to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		if (isScanning()
				&& anotherInterface.getHost().isRadioActive()
				&& isWithinRange(anotherInterface)
				&& !isConnected(anotherInterface)
				&& (this != anotherInterface)) {
			
			Connection con = new VBRConnection(this.host, this,
					anotherInterface.getHost(), anotherInterface);
			connect(con,anotherInterface);
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


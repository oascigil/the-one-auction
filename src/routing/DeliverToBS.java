package routing;

import core.Settings;
import core.SimClock;
import core.Connection;
import core.DTNHost;
import core.Message;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
/** 
* Router that will deliver messages to the closest connected Stationary node, if there the To address is empty.
* Otherwise, route message to the intended destination.
*/
public class DeliverToBS extends ActiveRouter {

    public static HashMap<DTNHost, DTNHost> attachmentPoint;
    public DeliverToBS (Settings s) {
        super(s);
    }

    public DeliverToBS(DeliverToBS r) {
        super(r);
    }

    @Override
    public void update() {
    	super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
    }
	
	/*@Override
	protected void transferDone(Connection con) {
		Message m = con.getMessage();
		System.out.println(SimClock.getTime()+" Transfer done "+getHost()+" received "+m.getId()+" "+m.getProperty("type")+" from "+m.getFrom());
		this.deleteMessage(m.getId(), false); // delete from buffer
	}*/
    
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
		System.out.println(SimClock.getTime()+" DelivertoBS Transfer done "+getHost()+" from "+from+" received "+m.getId()+" "+m.getProperty("type")+" from "+m.getFrom());
		from.getRouter().deleteMessage(m.getId(),false);
		return m;
	}
	
    @Override
	public DeliverToBS replicate() {
		return new DeliverToBS(this);
	}
}


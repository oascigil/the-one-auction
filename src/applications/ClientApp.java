package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import java.util.Random;

/**
 * Simple Client application to request tasks to be executed by Server Applications.
*/

public class ClientApp extends Application {

    /** Task execution request msg size  */
    public static final String REQUEST_MSG_SIZE_S = "taskReqMsgSize";
    /** Request Sending Frequency (send one every x secs) */
    public static final String REQUEST_FREQUENCY_S = "taskReqFreq";
    /** Number of Services */
    public static final String NROF_SERVICES_S = "nrofServices";
	
    // Private vars
    private int reqMsgSize;
    private double reqSendingFreq;
    private double lastReqSentTime;
    private int nrofServices;
    private Random rng;

    public ClientApp(Settings s) {
        if (s.contains(REQUEST_MSG_SIZE_S)) {
            this.reqMsgSize = s.getInt(REQUEST_MSG_SIZE_S);
        }
        else {
            this.reqMsgSize = 1;
        }

        if (s.contains(REQUEST_MSG_SIZE_S)) {
            this.reqSendingFreq = s.getDouble(REQUEST_MSG_SIZE_S);
        }
        else {
            this.reqSendingFreq = 10;
        }
        if (s.contains(NROF_SERVICES_S)) {
            this.nrofServices = s.getInt(NROF_SERVICES_S);
        }
        else {
            this.nrofServices = 10;
        }

        this.lastReqSentTime = 0.0;
        this.rng = new Random(this.nrofServices);
    }
	
    /**
	 * Copy-constructor
	 *
	 * @param a
	 */
     public ClientApp(ClientApp a) {
        super(a);

     }
	
    @Override
	public Application replicate() {
		return new ClientApp(this);
	}
	
    /**
	 * Handles an incoming message. If the message is a ServerResponse message, then report.
	 *
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
    public Message handle(Message msg, DTNHost host) {
		String type = (String)msg.getProperty("type");
        DTNHost serverHost = (DTNHost) msg.getProperty("auctionResult");
		if (type==null) return msg; 

		if (msg.getTo()==host && type.equalsIgnoreCase("clientAuctionResponse")) {
			String id = "TaskRequest" + SimClock.getIntTime() + "-" + serverHost.getAddress();
            Message m = new Message(host, serverHost, id, 1);
            m.addProperty("type", "clientRequest");
			host.createNewMessage(m);
			super.sendEventToListeners("GotAuctionResult", null, host);
			super.sendEventToListeners("SentClientRequest", null, host);
        }
		if (msg.getTo()==host && type.equalsIgnoreCase("execResponse")) {

        }

        return null;
    }
	/**
	 * Send request messages to the Server Applications. 
	 *
	 * @param host to which the application instance is attached
	 */
	@Override
	public void update(DTNHost host) {
        double currTime = SimClock.getTime();
        if ((this.lastReqSentTime == 0.0) || (this.lastReqSentTime - currTime > this.reqSendingFreq)) {
            int service = rng.nextInt(this.nrofServices);
            Message m = new Message(host, null, "client" + host.getName(), 1);
            m.addProperty("type", "clientAuctionRequest");
            m.addProperty("serviceType", service);
			host.createNewMessage(m);
			super.sendEventToListeners("SentClientAuctionRequest", null, host);
            this.lastReqSentTime = currTime;
        }
    }
}

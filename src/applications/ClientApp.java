package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import java.util.Random;
import java.util.List;

/**
 * Simple Client application to request tasks to be executed by Server Applications.
*/

public class ClientApp extends Application {

    /** Task execution request msg size  */
    public static final String REQUEST_MSG_SIZE_S = "taskReqMsgSize";
    /** Request Sending Frequency (send one every x secs) */
    public static final String REQUEST_FREQUENCY_S = "taskReqFreq";
	
	/** Application ID */
	public static final String APP_ID = "ucl.ClientApp";

    // Private vars
    private int reqMsgSize;
    private int lastRequestedService=0;
    private double reqSendingFreq;
    private double lastReqSentTime;
    private Random rng;
    private int requestId = 1;

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

        this.lastReqSentTime = 0.0;
        this.rng = new Random(Application.nrofServices);
		super.setAppID(APP_ID);
    }
	
    /**
	 * Copy-constructor
	 *
	 * @param a
	 */
     public ClientApp(ClientApp a) {
        super(a);
        this.rng = a.rng;
        this.reqMsgSize = a.reqMsgSize;
        this.reqSendingFreq = a.reqSendingFreq;
        this.lastReqSentTime = a.lastReqSentTime;
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
		System.out.println(SimClock.getTime()+ " Client app "+host+" received "+msg.getId()+" "+msg.getProperty("type")+" "+msg.getTo());
		String type = (String)msg.getProperty("type");
        DTNHost serverHost = (DTNHost) msg.getProperty("auctionResult");
		if (type==null) return msg; 

		if (msg.getTo()==host && type.equalsIgnoreCase("clientAuctionResponse")) {
			String id = "TaskRequest" + SimClock.getIntTime() + "-" + serverHost.getAddress();
            Message m = new Message(host, serverHost, id, 1);
            m.addProperty("type", "clientRequest");
            m.addProperty("serviceType", this.lastRequestedService);
            m.setAppID(ServerApp.APP_ID);
			host.createNewMessage(m);
            System.out.println(SimClock.getTime()+" Client app "+host+" sent message "+m.getId()+" to "+m.getTo());
			super.sendEventToListeners("GotAuctionResult", null, host);
			super.sendEventToListeners("SentClientRequest", null, host);
        }
		if (msg.getTo()==host && type.equalsIgnoreCase("execResponse")) {
        
        }
        host.getMessageCollection().remove(msg);

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
        //Send a request to the auctionApp periodically for a random service type
        // TODO implement popularity distributions for services (Dennis has Zipf dist. code)
        if ((this.lastReqSentTime == 0.0) || (this.lastReqSentTime - currTime > this.reqSendingFreq)) {
            this.lastRequestedService = this.rng.nextInt(Application.nrofServices);
            //System.out.println("Destlist for service "+this.lastRequestedService);
            List<DTNHost> destList = DTNHost.auctioneers.get(this.lastRequestedService);
            //assertions are not enabled by default: use -ea flag
            assert (destList != null ) : "Tried to use a service with no auctioneers: " + this.lastRequestedService;
            //System.out.println("Destlist: " + DTNHost.auctioneers);
            //System.out.println("Service: " + this.lastRequestedService + " is empty");
            //System.out.println("Destlist: " + destList);
            //System.out.println("auctioneers: " + DTNHost.auctioneers);
            //TODO pick the closest one
            DTNHost dest = destList.get(0);
            Message m = new Message(host, dest, "clientAuctionRequest" + host.getName()+"-"+requestId, 1);
            requestId++;
            m.addProperty("type", "clientAuctionRequest");
            m.addProperty("serviceType", lastRequestedService);
            m.addProperty("location", host.getLocation());
            m.setAppID(AuctionApplication.APP_ID);
			host.createNewMessage(m);
            System.out.println(currTime+" Client app "+host+" sent message "+m.getId()+" to "+m.getTo());
			super.sendEventToListeners("SentClientAuctionRequest", null, host);
            this.lastReqSentTime = currTime;
        }
    }
}

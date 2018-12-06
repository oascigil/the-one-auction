package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import core.Quartet;
import java.util.Random;
import java.util.List;
import java.util.HashMap;

/**
 * Simple Client application to request tasks to be executed by Server Applications.
*/

public class ClientApp extends Application {

    /** Task execution request msg size  */
    public static final String REQUEST_MSG_SIZE_S = "taskReqMsgSize";
    /** Request Sending Frequency (send one every x secs) */
    public static final String REQUEST_FREQUENCY_S = "taskReqFreq";
	/** Ping generation interval */
	public static final String PING_INTERVAL = "interval";
	/** Auction Request Timeout */
    public static final Double REQUEST_TIMEOUT = 2.0;
	/** Application ID */
	public static final String APP_ID = "ucl.ClientApp";
    /** Device that the client is currently assigned to */
    public DTNHost server;
    /** time of sent for ping */
    HashMap <Integer, Double> timeSent; 

    // Private vars
    private int     reqMsgSize;
    private int     lastRequestedService=0;
    private double  reqSendingFreq;
    private double  lastReqSentTime;
    /** Is the client currently assigned to a server */
    private boolean isAssigned=false;
    /** time at which the client is assigned to a server by the auction*/
    private double  assignmentTime=0.0;
    private Random  rng;
    private int     requestId = 1;
    private int     taskId = 1;
    private double  pingInterval;
	private int     pingSize=1;
    private int     sequenceNumber = 0;
	private double	lastPing = 0;
    private Double  qos=0.0;
    private boolean debug = false;


    public ClientApp(Settings s) {
        if (s.contains(REQUEST_MSG_SIZE_S)) {
            this.reqMsgSize = s.getInt(REQUEST_MSG_SIZE_S);
        }
        else {
            this.reqMsgSize = 1;
        }

        if (s.contains(REQUEST_FREQUENCY_S)) {
            this.reqSendingFreq = s.getDouble(REQUEST_FREQUENCY_S);
        }
        else {
            this.reqSendingFreq = 2.0;
        }
		if (s.contains(PING_INTERVAL)){
			this.pingInterval = s.getDouble(PING_INTERVAL);
		}
        else {
            this.pingInterval = 2.0;
        }

        this.lastReqSentTime = -1*ClientApp.REQUEST_TIMEOUT - 1;
        this.rng = new Random(Application.nrofServices);
        this.server = null;
        this.timeSent = new HashMap();
        this.isAssigned = false;
        this.assignmentTime = 0.0;
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
		this.lastPing = a.lastPing;
        this.pingInterval = a.pingInterval;
        this.reqMsgSize = a.reqMsgSize;
        this.reqSendingFreq = a.reqSendingFreq;
        this.lastReqSentTime = a.lastReqSentTime;
        this.server = a.server;
        this.sequenceNumber = a.sequenceNumber;
        this.timeSent = new HashMap();
        this.qos = a.qos;
        this.isAssigned = false;
        this.assignmentTime = 0.0;
        this.debug = a.debug;
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
        double currTime = SimClock.getTime();
        if (this.debug) 
	    	System.out.println(currTime + " Client app "+host+" received "+msg.getId()+" "+msg.getProperty("type")+" "+msg.getTo());
		String type = (String)msg.getProperty("type");
		if (type==null) return msg; 

		if (msg.getTo()==host && type.equalsIgnoreCase("clientAuctionResponse")) {
            DTNHost serverHost = (DTNHost) msg.getProperty("auctionResult");
            this.server = serverHost;
            if (serverHost == null) {
                if (this.debug)
                    System.out.println(currTime + " Client app " + host + " assigned to Cloud");    
            }
            else {
                this.isAssigned = true;
                this.assignmentTime = currTime;
                this.qos = (Double) msg.getProperty("QoS")/1000.0;
			    String id = "TaskRequest"+serverHost.getAddress()+"-"+host+"-"+taskId;
    			taskId++;
                Message m = new Message(host, serverHost, id, 1);
                m.addProperty("type", "clientRequest");
                m.addProperty("serviceType", this.lastRequestedService);
                m.setAppID(ServerApp.APP_ID);
	    		host.createNewMessage(m);
                if (this.debug)
                    System.out.println(currTime + " Client app "+host+" sent message "+m.getId()+" to "+m.getTo());
			    //super.sendEventToListeners("GotAuctionResult", null, host);
    			//super.sendEventToListeners("SentClientRequest", null, host);
            }
        }
		if (msg.getTo()==host && type.equalsIgnoreCase("execResponse")) {
            this.server = null;
            this.isAssigned = false;
        }
		// Received a pong reply
		if (msg.getTo()==host && type.equalsIgnoreCase("pong")) {
			// Send event to listeners
            Integer seqNo = (Integer) msg.getProperty("seqNo");
            Double time = this.timeSent.get(seqNo);
            Double elapsed = currTime - time;
            Double difference = elapsed - this.qos;
            if (this.debug)
                System.out.println(currTime + " Client app " + host + " RTT difference " + difference + " from " + this.qos + " measured: " + elapsed + " to server" + msg.getFrom() + "\n"); 
            Quartet aQuartet = new Quartet(difference, host, msg.getFrom(), this.lastRequestedService);
			super.sendEventToListeners("SampleRTT", aQuartet, host);
            this.timeSent.remove(seqNo);
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

        if ( (currTime - this.lastPing > this.pingInterval) && (this.server != null) ) {
            Message m = new Message(host, this.server, "ping" + SimClock.getIntTime() + "-" + host.getAddress(), this.pingSize);
			m.addProperty("type", "ping");
            m.addProperty("seqNo", this.sequenceNumber);
			m.setAppID(ServerApp.APP_ID);
            host.createNewMessage(m);
            this.lastPing = currTime;
			//super.sendEventToListeners("SentPing", msg, host);
            this.timeSent.put(this.sequenceNumber, currTime);
            if (this.timeSent.getOrDefault(this.sequenceNumber, null) != null) {
			    //super.sendEventToListeners("SampleRTT", new Quartet(Double.POSITIVE_INFINITY, host, this.server, this.lastRequestedService), host);
            }
            this.sequenceNumber += 1;
        }
        //In case execution response is delayed
        if(this.isAssigned && ((currTime - this.assignmentTime) > Application.execTimes.get(0))) {
            this.isAssigned = false;
        }
        //Send a request to the auctionApp periodically for a random service type
        if (!this.isAssigned && ( (currTime - this.lastReqSentTime) > ClientApp.REQUEST_TIMEOUT ) ) {
            Double randNumber = this.rng.nextDouble();
            if (randNumber < (1.0/(1000.0*Application.execTimes.get(0)))) {
                this.lastRequestedService = this.rng.nextInt(Application.nrofServices);
                List<DTNHost> destList = DTNHost.auctioneers.get(this.lastRequestedService);
                DTNHost dest = destList.get(0);
                Message m = new Message(host, dest, "clientAuctionRequest" + host.getName()+"-"+requestId, 1);
                requestId++;
                m.addProperty("type", "clientAuctionRequest");
                m.addProperty("serviceType", lastRequestedService);
                m.addProperty("location", host.getLocation());
                m.setAppID(AuctionApplication.APP_ID);
			    host.createNewMessage(m);
                if (this.debug) 
                    System.out.println(currTime+" Client app "+host+" sent message "+m.getId()+" to "+m.getTo());
			    //super.sendEventToListeners("SentClientAuctionRequest", null, host);
                this.lastReqSentTime = currTime;
            }
        }
    }
}

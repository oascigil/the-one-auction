package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
/**
 * Simple Server Application to run tasks for Client Applications.
*/

public class ServerAppEdge extends Application {
    /** Service types for the application  */
    public static final String SERVICE_TYPE_S = "serviceTypes";
    /** Request Sending Frequency (send one every x secs) */
    public static final String REQUEST_FREQUENCY_S = "taskReqFreq";
    
    public static final String APP_NAME = "name";

    public static final String NUM_VMS = "vms";

	/** Auction Request Timeout */
    public static final Double REQUEST_TIMEOUT = 3.0;
    //private vars
    /** Completion time of the current task being executed */
    private double completionTime;
    /** Flag to indicate if the application is currently busy executing */
    //private boolean isBusy;
    /** The services that this application provides to ClientApps */
    private ArrayList<Integer> services;
    /** Size of the response message */
    private int respMsgSize;
	/** Application ID */
	public String appId;
    /** Have we sent auction request */
	/** Application ID */
	public static final String APP_ID = "ucl.ServerApp";
    /** Device that the client is currently assigned to */
    public DTNHost client;
    // private vars
    //private Random  rng;
    //private double  reqSendingFreq;
    //private double remainingEngagementTime;
	private int requestId=1;
	private int		pongSize=1;
    private boolean debug = false;
    private boolean isAuctionRegistrationComplete;
    private double lastOfferSentTime;
    private int vms=4;
	/**
	 * Creates a new server application with the given settings.
	 *
	 * @param s	Settings to use for initializing the application.
	 */
	public ServerAppEdge(Settings s) {
        if (s.contains(SERVICE_TYPE_S)) {
            int[] array = s.getCsvInts(SERVICE_TYPE_S);
            this.services = new ArrayList<Integer>();
            for(int d : array ) this.services.add(d);
        }
        if(s.contains(APP_NAME)) {
        	this.appId = s.getSetting(APP_NAME);
        //	System.out.println("Name "+this.appId+" "+s.getSetting(APP_NAME));
        }
        if(s.contains(NUM_VMS)) {
        	this.vms = s.getInt(NUM_VMS);
        //	System.out.println("Name "+this.appId+" "+s.getSetting(APP_NAME));
        }
        /*
        if (s.contains(REQUEST_FREQUENCY_S)) {
            this.reqSendingFreq = s.getDouble(REQUEST_FREQUENCY_S);
        }
        else {
            this.reqSendingFreq = 2.0;
        }*/
        this.completionTime = 0.0; 
        this.respMsgSize = 1;
        //this.appId = "ServerApp" + this.services;
        this.client = null;
        this.lastOfferSentTime = -1*ServerAppEdge.REQUEST_TIMEOUT;
        this.isAuctionRegistrationComplete = false;
        //this.rng = new Random();
		super.setAppID(APP_ID);
    }
	/**
	 * Copy-constructor
	 *
	 * @param a
	 */
     public ServerAppEdge(ServerAppEdge a) {
        super(a);
        this.services = a.services;
        this.respMsgSize = a.respMsgSize;
        this.appId = a.appId;
        this.pongSize = a.pongSize;
        this.debug = a.debug;
        this.client = a.client;
        this.lastOfferSentTime = a.lastOfferSentTime;
        this.isAuctionRegistrationComplete = false;
        this.vms = a.vms;
        //this.reqSendingFreq = a.reqSendingFreq;
    }
	
    @Override
	public Application replicate() {
		return new ServerAppEdge(this);
	}

	/**
	 * Handles an incoming message. If the message is a request message, then start executing the task.
	 *
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
	@Override
	public Message handle(Message msg, DTNHost host) {
        if (this.debug) 
    		System.out.println(SimClock.getTime()+" Server appedge "+host+" received "+msg.getId()+" "+msg.getProperty("type")+" "+msg.getTo());
		String type = (String)msg.getProperty("type");
		if (type==null) return msg; 

        /* Get the task execution request
		if (msg.getTo()==host && type.equalsIgnoreCase("clientRequest")) {
            int service = (int) msg.getProperty("serviceType");
            this.completionTime = SimClock.getTime() + Application.execTimes.get(service);
            this.isBusy = true;
            this.client = msg.getFrom();
        }*/
        // Get the auction result
        if (msg.getTo() == host && type.equalsIgnoreCase("serverAuctionResponse")) {
            this.isAuctionRegistrationComplete = true;
            DTNHost clientHost = (DTNHost) msg.getProperty("auctionResult");
            if(clientHost == null) { //serverApp was not assigned to any client
                if (this.debug)
                    System.out.println("ServerAppedge: " + host + " received null Auction Response");
            }
        }
		// Respond with pong if we're the recipient
		if (msg.getTo()==host && type.equalsIgnoreCase("ping")) {
			String id = "pong" + SimClock.getIntTime() + "-" +
				host.getAddress();
            Integer sequenceNumber = (Integer) msg.getProperty("seqNo");
			Message m = new Message(host, msg.getFrom(), id, this.pongSize);
            m.addProperty("seqNo", sequenceNumber);
			m.addProperty("type", "pong");
			m.setAppID(ClientApp.APP_ID);
			host.createNewMessage(m);
		}
        host.getMessageCollection().remove(msg);

        return null;
    }

	/**
	 * Send a response message back to the Client app when execution is complete. 
	 *
	 * @param host to which the application instance is attached
	 */
	@Override
	public void update(DTNHost host) {
        double time = SimClock.getTime();
        /*
        if (time >= this.completionTime && this.isBusy) {
            //Send a response back to the requestor
			Message m = new Message(host, reqMsg.getFrom(), reqMsg.getId()+"Response", this.respMsgSize);
            m.addProperty("type", "execResponse");
            m.setAppID(this.appId);
			host.createNewMessage(m);
            m.setAppID(ClientApp.APP_ID);
            if (this.debug) 
                System.out.println(SimClock.getTime()+" Server app "+host+" sent message "+m.getId()+" to "+m.getTo());
			//super.sendEventToListeners("SentExecResponse", null, host);
            this.isBusy = false;
        }*/

        // Register once with the Auction App 
        if (this.isAuctionRegistrationComplete == false && ( (time-this.lastOfferSentTime) >= ServerAppEdge.REQUEST_TIMEOUT) ) {
            this.lastOfferSentTime = time;
            //List<DTNHost> destList = DTNHost.auctioneers.get(this.services.get(0));
            //DTNHost dest = destList.get(0);
            Message m = new Message(host, host, this.appId+"Request"+ host.getName()+"-"+requestId, 1);
            //requestId++;
            m.addProperty("type", "serverAuctionRequest");
            m.addProperty("serviceType", this.services);
            m.addProperty("location", host.getLocation());
            m.addProperty("vm", this.vms);
            m.setAppID(AuctionApplication.APP_ID);
	        host.createNewMessage(m);
            if (this.debug)
	            System.out.println(SimClock.getTime()+" Server appedge "+this.appId+" "+host+" sent message "+m.getId()+" to "+m.getTo());
		    //super.sendEventToListeners("SentServerAuctionRequest", null, host);
        }
    }
}

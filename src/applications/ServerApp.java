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
/**
 * Simple Server Application to run tasks for Client Applications.
*/

public class ServerApp extends Application {
    /** Service types for the application  */
    public static final String SERVICE_TYPE_S = "serviceTypes";

    //private vars
    /** Completion time of the current task being executed */
    private double completionTime;
    /** Flag to indicate if the application is currently busy executing */
    private boolean isBusy;
    /** The services that this application provides to ClientApps */
    private ArrayList<Integer> services;
    /** Request message that is currently being executed */
    private Message reqMsg;
    /** Size of the response message */
    private int respMsgSize;
	/** Application ID */
	public String appId;
    /** Have we sent auction request */
    public boolean isServerAuctionRequestSent;
	/** Application ID */
	public static final String APP_ID = "ucl.ServerApp";
	private int requestId=1;
	/**
	 * Creates a new server application with the given settings.
	 *
	 * @param s	Settings to use for initializing the application.
	 */
	public ServerApp(Settings s) {
        if (s.contains(SERVICE_TYPE_S)) {
            int[] array = s.getCsvInts(SERVICE_TYPE_S);
            this.services = new ArrayList<Integer>();
            for(int d : array ) this.services.add(d);
        }
        this.isBusy = false;
        this.completionTime = 0.0; 
        this.reqMsg = null;
        this.respMsgSize = 1;
        this.appId = "ServerApp" + this.services;
        this.isServerAuctionRequestSent = false;
		super.setAppID(APP_ID);
    }
	/**
	 * Copy-constructor
	 *
	 * @param a
	 */
     public ServerApp(ServerApp a) {
        super(a);
        this.isBusy = a.isBusy;
        this.services = a.services;
        this.reqMsg = a.reqMsg;
        this.respMsgSize = a.respMsgSize;
        this.appId = a.appId;
     }
	
    @Override
	public Application replicate() {
		return new ServerApp(this);
	}

	/**
	 * Handles an incoming message. If the message is a request message, then start executing the task.
	 *
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
	@Override
	public Message handle(Message msg, DTNHost host) {
		System.out.println(SimClock.getTime()+" Server app "+host+" received "+msg.getId()+" "+msg.getProperty("type")+" "+msg.getTo());
		String type = (String)msg.getProperty("type");
		if (type==null) return msg; 

        //Start execution if we are the recipient
		if (msg.getTo()==host && type.equalsIgnoreCase("clientRequest")) {
            int service = (int) msg.getProperty("serviceType");
            this.completionTime = SimClock.getTime() + Application.execTimes.get(service);
            this.isBusy = true;
            this.isServerAuctionRequestSent = false;
            reqMsg = msg;
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
        if (time >= this.completionTime && this.isBusy) {
            //Send a response back to the requestor
			Message m = new Message(host, reqMsg.getFrom(), reqMsg.getId()+"Response", this.respMsgSize);
            m.addProperty("type", "execResponse");
            m.setAppID(this.appId);
			host.createNewMessage(m);
            m.setAppID(ClientApp.APP_ID);
            System.out.println(SimClock.getTime()+" Server app "+host+" sent message "+m.getId()+" to "+m.getTo());
			super.sendEventToListeners("SentExecResponse", null, host);
            this.isBusy = false;
        }
        if (!this.isBusy && !this.isServerAuctionRequestSent) {
            this.isServerAuctionRequestSent = true;
            double currTime = SimClock.getTime();
            //Send an offer for each auctioneer
            for (int s : this.services) {
                List<DTNHost> destList = DTNHost.auctioneers.get(s);
                
                assert (destList != null ) : "Tried to use a service with no auctioneers: " + s;
                //System.out.println("Auction server "+destList.get(0));
                //TODO pick the closest one
                DTNHost dest = destList.get(0);
                Message m = new Message(host, dest, "serverAuctionRequest" + host.getName()+"-"+requestId, 1);
                requestId++;
                m.addProperty("type", "serverAuctionRequest");
                m.addProperty("serviceType", s);
                m.addProperty("location", host.getLocation());
                m.setAppID(AuctionApplication.APP_ID);
	    		host.createNewMessage(m);
	            System.out.println(SimClock.getTime()+" Server app "+host+" sent message "+m.getId()+" to "+m.getTo());
		    	super.sendEventToListeners("SentServerAuctionRequest", null, host);
            }
        }
    }
}

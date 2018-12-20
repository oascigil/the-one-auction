package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import core.Coord;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuctionApplicationEdge extends Application {
    /** Auction period length */
    public static final String AUCTION_PERIOD_S = "auctionPeriod";
    /** Service types for the Auction  */
    public static final String SERVICE_TYPE_S = "serviceTypes";
    /** Migration overheads   */
    public static final String MIGRATION_OVERHEAD_S = "migrationOverheads";
    //public static final double speedOfLight = 180000;
    /** List of client request messages received during the current auctionPeriod */
    public ArrayList<Message> clientRequests;
    /** Mapping of client host to the last request message received during the current auctionPeriod */
    public HashMap<DTNHost, Message> clientHostToMessage;
    /** Mapping of server host to the last request message received during the current auctionPeriod */
    public HashMap<DTNHost, Message> serverHostToMessage;
    /** List of server request messages received during the current auctionPeriod */
    public ArrayList<Message> serverRequests;
    /** Device prices from the last (previous) invocation of the auction */
    public HashMap<DTNHost, Double> previousPrices;

    //Private vars
    /** Minimum QoS Requirement of Services  */
    public HashMap<Integer, Double> q_minPerLLA; // = new HashMap();
    /** Upper-bound on the QoS Requirement of Services  */
    public HashMap<Integer, Double> q_maxPerLLA; // = new HashMap();
    /** Client/Server Application that this auction belongs to */
    public int [] services;
    /** Last time that an auction was executed */
    public double lastAuctionTime;
    /** Size of the message sent to auction */
    public int auctionMsgSize;
    /** Frequency of auctions */
    public double auctionPeriod;
    

    // Static vars
	/** Application ID */
	public static final String APP_ID = "ucl.AuctionApplication";
    /** Debug flag */
    private boolean debug = true;
	/**
	 * Creates a new auction application with the given settings.
	 *
	 * @param s	Settings to use for initializing the application.
	 */
    public AuctionApplicationEdge(Settings s) {
        if (s.contains(SERVICE_TYPE_S)) {
            this.services = s.getCsvInts(SERVICE_TYPE_S);
        }
        else {
            System.out.println("Warning: Failed to set the service types in the AuctionApp.");
        }
        //this.LLAmigrationOverhead = new HashMap();
        if (s.contains(MIGRATION_OVERHEAD_S)) {
            int n_services = this.services.length;
            double [] overheads = s.getCsvDoubles(MIGRATION_OVERHEAD_S);
            if (overheads.length != this.services.length) {
                System.out.println("Warning: number of migration overheads do not match service population size.");
            }
            /*for (int i = 0; i < n_services; i++) {
                this.LLAmigrationOverhead.put(i, overheads[i]);
            }*/    
        }
        else {
            System.out.println("Warning: Failed to set the migration overheads.");
        }
        if (s.contains(AUCTION_PERIOD_S)) {
            this.auctionPeriod = s.getDouble(AUCTION_PERIOD_S);
        }
        else {
            this.auctionPeriod = 5.0; //5 seconds 
        }
        System.out.println("New Service types "+ this.services);
        this.clientRequests = new ArrayList<Message>();
        this.serverRequests = new ArrayList<Message>();
        this.clientHostToMessage = new HashMap<DTNHost, Message>();
        this.serverHostToMessage = new HashMap<DTNHost, Message>();
        this.auctionMsgSize = 10; //TODO read this from Settings
        this.lastAuctionTime = 0.0;
        this.q_minPerLLA = new HashMap<Integer, Double>();
        this.q_maxPerLLA = new HashMap<Integer, Double>();
        /*this.userCompletionTime = new HashMap();
        this.LLAs_Users_Association = null;
        this.user_LLA_Association = null;
        this.LLAs_Devices_Association = null;
        this.device_LLAs_Association = null;
        this.previousUserDeviceAssociation = null;*/
        this.previousPrices = null;
        //this.prices = null;
		super.setAppID(APP_ID);
    }
	
	/**
	 * Copy-constructor
	 *
	 * @param a
	 */
    public AuctionApplicationEdge(AuctionApplicationEdge a) {
		super(a);
        this.auctionPeriod = a.auctionPeriod;
        this.lastAuctionTime = a.lastAuctionTime;
        this.clientRequests = a.clientRequests;
        this.serverRequests = a.serverRequests;
        this.clientHostToMessage = new HashMap<DTNHost, Message>();
        this.serverHostToMessage = new HashMap<DTNHost, Message>();
        this.services = a.services;
        this.q_minPerLLA = a.q_minPerLLA;
        this.q_maxPerLLA = a.q_maxPerLLA;
        /*this.prices = a.prices;
        this.LLAmigrationOverhead = a.LLAmigrationOverhead;
        this.userCompletionTime = new HashMap();
        this.LLAs_Users_Association = null;
        this.user_LLA_Association = null;
        this.LLAs_Devices_Association = null;
        this.device_LLAs_Association = null;
        this.previousUserDeviceAssociation = null;*/
        this.previousPrices = null;
        this.debug = a.debug;
    }
	
    @Override
	public Application replicate() {
		return new AuctionApplicationEdge(this);
	}
	
    /**
	 * Handles incoming client and server request messages - it buffers them for the auction process.
	 *
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
	@Override
	public Message handle(Message msg, DTNHost host) {
		String type = (String)msg.getProperty("type");
        if (this.debug)
	    	System.out.println(SimClock.getTime()+" Auction app "+host+" received "+msg.getId()+" "+msg.getProperty("type")+" from "+msg.getFrom());
		//System.out.println("Auction app received "+msg.getId()+" "+type);
		if (type==null) return msg; // Not a ping/pong message

        if (type.equalsIgnoreCase("clientAuctionRequest")) {
            if (this.debug)
        	    System.out.println(SimClock.getTime()+" New client request "+clientRequests.size());
            Message clientMsg = msg.replicate();
            clientHostToMessage.put(clientMsg.getFrom(), clientMsg);
            clientRequests.add(clientMsg);
            //super.sendEventToListeners("ReceivedClientAuctionRequest", (Object) clientMsg, host);
        }
        if (type.equalsIgnoreCase("serverAuctionRequest")) {
            if (this.debug)
        	    System.out.println(SimClock.getTime()+" New server offer "+ this.serverRequests.size());
            if(this.serverHostToMessage.getOrDefault(msg.getFrom(), null) == null) {
                Message serverMsg = msg.replicate();
                this.serverHostToMessage.put(serverMsg.getFrom(), serverMsg);
                this.serverRequests.add(msg.replicate());    
                //super.sendEventToListeners("ReceivedServerAuctionRequest", (Object) serverMsg, host);       
            }
        }
        host.getMessageCollection().remove(msg);
        return null;
    }

	/**
	 * Pairs clients and servers through an auction
	 *
	 * @param host to which the application instance is attached
	 */
	@Override
	public void update(DTNHost host) {
        double currTime = SimClock.getTime();
        if (currTime - this.lastAuctionTime >= this.auctionPeriod) {
            if (this.debug)
                System.out.println("Executing auction at time: " + currTime + "Auction Period: " + this.auctionPeriod);
            execute_auction(host);
            this.lastAuctionTime = currTime;
        }
    }

    public void execute_auction(DTNHost host) {
        //int len = Math.min(clientRequests.size(), serverRequests.size());
		//System.out.println("Execute action "+clientRequests.size()+" "+serverRequests.size()+" "+len);
        double currTime = SimClock.getTime();
        this.lastAuctionTime = currTime;
        
        
        HashMap<DTNHost,String> userDeviceAssociation = new HashMap <DTNHost,String>();
        HashMap<String,DTNHost> deviceUserAssociation = new HashMap <String,DTNHost>();

        HashMap<DTNHost, Double> userCompletionTime = new HashMap <DTNHost,Double>();

        int i = 0;
        for (Message msg : clientRequests)
        {
            Double completionTime = userCompletionTime.getOrDefault(msg.getFrom(), null);
            if(completionTime != null && completionTime > currTime) {
                /** skip this message: user already part of auction  */
                continue;
            }
            int serviceType = (int) msg.getProperty("serviceType");
            completionTime = (Double) msg.getProperty("completionTime");
            if (completionTime == null) {
                completionTime = currTime + Application.execTimes.get(serviceType);
            }
            userCompletionTime.put(msg.getFrom(), completionTime);
        	if(i>=serverRequests.size()) { 
        		userDeviceAssociation.put(msg.getFrom(), null);
	        	//deviceUserAssociation.put(serverRequests.get(i).getId(), msg.getFrom());
        	} else {
	        	userDeviceAssociation.put(msg.getFrom(), serverRequests.get(i).getId());
	        	deviceUserAssociation.put(serverRequests.get(i).getId(), msg.getFrom());
        	}
        	i++;
        	
        }


        /**Send the auction results back to the clients (null if they are assigned to the cloud) */
        for (Map.Entry<DTNHost, String> entry : userDeviceAssociation.entrySet()) {
           
        	DTNHost client = entry.getKey();
        	String hostId = entry.getValue();
            DTNHost server = host;
            // Send a response back to a client
            Message clientMsg = this.clientHostToMessage.get(client);
            String msgId = new String("ClientAuctionResponse_" + client.getName());
            Message m = new Message(host, clientMsg.getFrom(), msgId, this.auctionMsgSize);
            //HashMap<DTNHost, Double> clientDistances = user_device_Latency.get(client);
            //Double latency = clientDistances.get(server);
            m.addProperty("type", "clientAuctionResponse");
            if(hostId==null)m.addProperty("auctionResult", null);
            else m.addProperty("auctionResult", server);
            m.addProperty("QoS", client.getLocalLatency());
            m.addProperty("completionTime", userCompletionTime.get(client)); 
            m.setAppID(ClientApp.APP_ID);
            host.createNewMessage(m);
            if (this.debug)
                System.out.println(SimClock.getTime()+" Execute auction from "+host+" to "+ client+" with result "+server+" "+ msgId+" "+host.getMessageCollection().size());
            super.sendEventToListeners("SentClientAuctionResponse", null, host);
        }
        /** Send the auction results back to the servers */
        for (Map.Entry<String, DTNHost> entry : deviceUserAssociation.entrySet()) {
            DTNHost server = host;
            DTNHost client = entry.getValue();
            // Send a response back to a server
            Message serverMsg = this.serverHostToMessage.get(server);
            String msgId = new String("serverAuctionResponse_" + server.getName());
            Message m = new Message(host, serverMsg.getFrom(), msgId, this.auctionMsgSize);
            m.addProperty("type", "serverAuctionResponse");
            m.addProperty("auctionResult", client);
            m.setAppID(ServerApp.APP_ID);
            host.createNewMessage(m);
            if (this.debug)
                System.out.println(SimClock.getTime()+" Execute auction from "+host+" to "+ server+" with result "+client+" "+ msgId+" "+host.getMessageCollection().size());
            super.sendEventToListeners("SentServerAuctionResponse", null, host);
        }

        //this.clientHostToMessage.clear();
        //this.serverHostToMessage.clear();
        this.clientRequests.clear();
        this.serverRequests.clear();
    }

    public int [] getServiceTypes() {
        return this.services;
    }
}

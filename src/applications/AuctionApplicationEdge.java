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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    /** Debug flag */
    private boolean debug = true;
    /** VM completion times */
    public HashMap<Integer, Double> vmCompletionTime;
    /** number of VMS */
    private int numOfVMs=0;
    /** VM user association */
    public HashMap<Integer, DTNHost> vmUserAssociation;
    /** free VMs */
    public Set<Integer> freeVMsSet=null; 
    /** user VM association */
    public HashMap<DTNHost,Integer> userVMAssociation;
    /** Users whose request arrived after last auction */
    public ArrayList<DTNHost> newUserRequests;

    // Static vars
    /** user engagement completion times */
    public static HashMap<DTNHost, Double> userCompletionTime;
    /** user LLA Associations */
    public static HashMap<DTNHost, Integer> userLLAAssociation;
    /** global user device associations */
    //public static HashMap<DTNHost, DTNHost> globalUserDeviceAssociation;
	/** Application ID */
	public static final String APP_ID = "ucl.AuctionApplication";

    static {
        //globalUserDeviceAssociation = new HashMap<DTNHost, DTNHost>();
        userCompletionTime = new HashMap<DTNHost, Double>();
        userLLAAssociation = new HashMap<DTNHost, Integer>();
    }

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
        this.vmUserAssociation = new HashMap<Integer, DTNHost>();
        this.userVMAssociation = new HashMap<DTNHost, Integer> ();
        this.auctionMsgSize = 10; //TODO read this from Settings
        this.lastAuctionTime = 0.0;
        this.q_minPerLLA = new HashMap<Integer, Double>();
        this.q_maxPerLLA = new HashMap<Integer, Double>();
        /*
        this.LLAs_Users_Association = null;
        this.user_LLA_Association = null;
        this.LLAs_Devices_Association = null;
        this.device_LLAs_Association = null; */
        //this.previousUserDeviceAssociation = null;
        //this.previousPrices = null;
        //this.prices = null;
        this.vmCompletionTime = new HashMap<Integer, Double>();
        this.numOfVMs = 0;
        this.newUserRequests = new ArrayList<DTNHost>();
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
        this.vmCompletionTime = new HashMap<Integer, Double>();
        this.vmUserAssociation = new HashMap<Integer, DTNHost>();
        this.userVMAssociation = new HashMap<DTNHost, Integer> ();
        this.previousPrices = null;
        this.newUserRequests = new ArrayList<DTNHost>();
        this.numOfVMs = 0;
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
        	
            if(this.serverHostToMessage.getOrDefault(msg.getFrom(), null) == null) {
                Message serverMsg = msg.replicate();
                this.serverHostToMessage.put(serverMsg.getFrom(), serverMsg);
                this.serverRequests.add(msg.replicate());    
                this.numOfVMs = (int) msg.getProperty("vm");
                this.freeVMsSet = new HashSet<Integer>();
                for (Integer indx = 0; indx < this.numOfVMs; indx++) {          
                    this.freeVMsSet.add(indx);
                }
                //for(int i = 0;i<(int) msg.getProperty("vm");i++)
                //super.sendEventToListeners("ReceivedServerAuctionRequest", (Object) serverMsg, host);       
            }
            if (this.debug)
        	    System.out.println(host+" "+SimClock.getTime()+" New server offer "+ this.serverRequests.size());
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
		System.out.println("Execute action "+clientRequests.size()+" "+serverRequests.size());
        double currTime = SimClock.getTime();
        this.lastAuctionTime = currTime;

        /** no VMs to allocate */
        if (this.numOfVMs == 0) {
            return;
        }
        ArrayList<Integer> entriesToRemove = new ArrayList<Integer>();
        for (Map.Entry<Integer, Double> entry : this.vmCompletionTime.entrySet() ) {
            Integer vm = entry.getKey();
            Double completionTime = entry.getValue();
            if (currTime >= completionTime)
            {
                entriesToRemove.add(vm);
                DTNHost user = this.vmUserAssociation.get(vm);
                AuctionApplicationEdge.userCompletionTime.remove(user);
                this.vmUserAssociation.remove(vm);
                this.userVMAssociation.remove(user);
                this.freeVMsSet.add(vm);
            }
        }
        
        /** Remove expired user requests */
        for(Integer vm : entriesToRemove) {
            this.vmCompletionTime.remove(vm);
        }

        if(this.vmCompletionTime.size() == this.numOfVMs) {
            /** there are no VMs available */
            //XXX call reporter
            return;
        }
        else if (this.vmCompletionTime.size() > this.numOfVMs) {
            System.out.println("Warning: Number of VMS: " + this.numOfVMs + " is less than occupied VMs: " + this.vmCompletionTime.size());
        }
        
        for (Message msg : clientRequests)
        {
            Double completionTime = AuctionApplicationEdge.userCompletionTime.getOrDefault(msg.getFrom(), null);
            DTNHost user = msg.getFrom();
            int serviceType = (int) msg.getProperty("serviceType");
            if (completionTime == null) {
                completionTime = currTime + Application.execTimes.get(serviceType);
                this.newUserRequests.add(user);
            }
            else if(completionTime > currTime) {
                /** Delayed requests that are already expired*/
                continue;
            }
            userLLAAssociation.put(user, serviceType);

        	if(this.vmCompletionTime.size() < this.numOfVMs) { 
	        	//deviceUserAssociation.put(serverRequests.get(i).getId(), msg.getFrom());
                AuctionApplicationEdge.userCompletionTime.put(user, completionTime);
                Integer vm = this.freeVMsSet.iterator().next(); 
                this.userVMAssociation.put(user, vm);
                this.vmCompletionTime.put(vm, completionTime);
                this.vmUserAssociation.put(vm, user);
                this.freeVMsSet.remove(vm);
        	} 
            else {
                this.userVMAssociation.put(user, null);
        	}
        }

        super.sendEventToListeners("EdgeAuctionExecutionComplete", userVMAssociation, host);

        /**Send the auction results back to the clients (null if they are assigned to the cloud) */
        for (Map.Entry<DTNHost, Integer> entry : this.userVMAssociation.entrySet()) {
        	DTNHost client = entry.getKey();
        	Integer hostId = entry.getValue();
            DTNHost server = host;
            // Send a response back to a client
            Message clientMsg = this.clientHostToMessage.get(client);
            String msgId = new String("ClientAuctionResponse_" + client.getName());
            Message m = new Message(host, clientMsg.getFrom(), msgId, this.auctionMsgSize);
            //HashMap<DTNHost, Double> clientDistances = user_device_Latency.get(client);
            //Double latency = clientDistances.get(server);
            m.addProperty("type", "clientAuctionResponse");
            if(hostId==null)
                m.addProperty("auctionResult", null);
            else 
                m.addProperty("auctionResult", server);
            m.addProperty("QoS", client.getLocalLatency());
            m.addProperty("completionTime", userCompletionTime.get(client)); 
            m.setAppID(ClientApp.APP_ID);
            host.createNewMessage(m);
            if (this.debug)
                System.out.println(SimClock.getTime()+" Execute auction from "+host+" to "+ client+" with result "+server+" "+ msgId+" "+host.getMessageCollection().size());
            //super.sendEventToListeners("SentClientAuctionResponse", null, host);
        }
        /** Send the auction results back to the servers */
        for (Map.Entry<Integer, DTNHost> entry : vmUserAssociation.entrySet()) {
            DTNHost server = host;
            DTNHost client = entry.getValue();
            //System.out.println(host+" "+SimClock.getTime()+" "+serverHostToMessage.size());;
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
            //super.sendEventToListeners("SentServerAuctionResponse", null, host);
        }

        //this.clientHostToMessage.clear();
        //this.serverHostToMessage.clear();
        this.clientRequests.clear();
        this.serverRequests.clear();
        this.newUserRequests.clear();
    }

    public int [] getServiceTypes() {
        return this.services;
    }
}

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

public class AuctionApplication extends Application {
    /** Service types for the Auction  */
    public static final String SERVICE_TYPE_S = "serviceTypes";
    /** Service types for the Auction  */
    public static final double speedOfLight = 180000;
    /** List of client request messages received during the current auctionPeriod */
    public ArrayList<Message> clientRequests;
    /** Mapping of client host to the last request message received during the current auctionPeriod */
    public HashMap<DTNHost, Message> clientHostToMessage;
    /** List of server request messages received during the current auctionPeriod */
    public ArrayList<Message> serverRequests;

    //Private vars
    /** Minimum QoS Requirement of Services  */
    private HashMap<Integer, Double> q_minPerLLA; // = new HashMap();
    /** Upper-bound on the QoS Requirement of Services  */
    private HashMap<Integer, Double> q_maxPerLLA; // = new HashMap();
    /** Client/Server Application that this auction belongs to */
    private int [] services;
    /** Last time that an auction was executed */
    private double lastAuctionTime;
    /** Size of the message sent to auction */
    private int auctionMsgSize;

    // Static vars
	/** Application ID */
	public static final String APP_ID = "ucl.AuctionApplication";

	/**
	 * Creates a new auction application with the given settings.
	 *
	 * @param s	Settings to use for initializing the application.
	 */
    public AuctionApplication(Settings s) {
        if (s.contains(SERVICE_TYPE_S)) {
            this.services = s.getCsvInts(SERVICE_TYPE_S);
        }
        System.out.println("New Service types "+ this.services);
        this.clientRequests = new ArrayList<Message>();
        this.serverRequests = new ArrayList<Message>();
        this.clientHostToMessage = new HashMap<DTNHost, Message>();
        this.auctionMsgSize = 10; //TODO read this from Settings
        this.lastAuctionTime = 0.0;
        this.q_minPerLLA = new HashMap<Integer, Double>();
        this.q_maxPerLLA = new HashMap<Integer, Double>();
		super.setAppID(APP_ID);
    }
	
	/**
	 * Copy-constructor
	 *
	 * @param a
	 */
    public AuctionApplication(AuctionApplication a) {
		super(a);
        this.auctionPeriod = a.auctionPeriod;
        this.lastAuctionTime = a.lastAuctionTime;
        this.clientRequests = a.clientRequests;
        this.serverRequests = a.serverRequests;
        this.clientHostToMessage = a.clientHostToMessage;
        this.services = a.services;
        this.q_minPerLLA = a.q_minPerLLA;
        this.q_maxPerLLA = a.q_maxPerLLA;
    }
	
    @Override
	public Application replicate() {
		return new AuctionApplication(this);
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
		System.out.println(SimClock.getTime()+" Auction app "+host+" received "+msg.getId()+" "+msg.getProperty("type")+" from "+msg.getFrom());
		//System.out.println("Auction app received "+msg.getId()+" "+type);
		if (type==null) return msg; // Not a ping/pong message

        if (type.equalsIgnoreCase("clientAuctionRequest")) {
            Message clientMsg = msg.replicate();
            clientHostToMessage.put(clientMsg.getFrom(), clientMsg);
            clientRequests.add(clientMsg);
        }
        if (type.equalsIgnoreCase("serverAuctionRequest")) {
            serverRequests.add(msg.replicate());
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
        if (currTime - this.lastAuctionTime > this.auctionPeriod) {
            execute_auction(host);
            this.lastAuctionTime = currTime;
        }
    }

    private void execute_auction(DTNHost host) {
        //int len = Math.min(clientRequests.size(), serverRequests.size());
		//System.out.println("Execute action "+clientRequests.size()+" "+serverRequests.size()+" "+len);
        double currTime = SimClock.getTime();
        this.lastAuctionTime = currTime;
        HashMap<Integer, ArrayList<DTNHost>>  LLAs_Users_Association = new HashMap();
        HashMap<DTNHost, Integer>  user_LLA_Association = new HashMap();
        HashMap<Integer, ArrayList<DTNHost>> LLAs_Devices_Association = new HashMap();
        HashMap<DTNHost, ArrayList<Integer>> device_LLAs_Association = new HashMap();

        assert (Application.nrofServices == Application.minQoS.size()) : "Discrepancy between nrofServices and minQoS size"; 
        boolean controlMessageFlag = false, controlAuctionMessageFlag = false;

        for(int indx : this.services)
        {
            double minQoS = Application.minQoS.get(indx);
            q_minPerLLA.put(indx, minQoS);
            q_maxPerLLA.put(indx, 100.0);
        }

        for(int indx= 0; indx < clientRequests.size();indx++)
        {
            Message clientMsg = clientRequests.get(indx);
            DTNHost clientHost = clientMsg.getFrom();
            int serviceType = (int) clientMsg.getProperty("serviceType");
            ArrayList<DTNHost> l = LLAs_Users_Association.get(serviceType);
            if (l == null) {
                l = new ArrayList<DTNHost>();
                l.add(clientHost);
                LLAs_Users_Association.put(serviceType, l);
            }
            else {
                l.add(clientHost);
            }
            user_LLA_Association.put(clientHost, serviceType);
        }

        for(int i=0;i<serverRequests.size();i++)
        {
            Message serverMsg = serverRequests.get(i);
            DTNHost serverHost = serverMsg.getFrom();
            int serviceType = (int) serverMsg.getProperty("serviceType");
            ArrayList<DTNHost> devicesList = LLAs_Devices_Association.get(serviceType);
            if (devicesList == null) {
                devicesList = new ArrayList<DTNHost>();
                devicesList.add(serverHost);
                LLAs_Devices_Association.put(serviceType, devicesList);
            }
            else {
                devicesList.add(serverHost);
            }
            ArrayList<Integer> deviceServices = device_LLAs_Association.get(serverHost);
            if (deviceServices == null) {
                deviceServices = new ArrayList<Integer>();
                deviceServices.add(serviceType);
                device_LLAs_Association.put(serverHost, deviceServices);
            }
            else {
                deviceServices.add(serviceType);
            }
        }

        HashMap<DTNHost, HashMap<DTNHost, Double>> user_device_Latency = new HashMap();
        
        for(int indx= 0; indx < clientRequests.size();indx++) { 
            Message clientMsg = clientRequests.get(indx);
            DTNHost clientHost = clientMsg.getFrom();
            Coord clientCoord = clientHost.getLocation();
            HashMap<DTNHost, Double> clientDistances = new HashMap();
            user_device_Latency.put(clientHost, clientDistances);
            for(int i=0;i<serverRequests.size();i++) {
                Message serverMsg = serverRequests.get(i);
                DTNHost serverHost = serverMsg.getFrom();
                Coord serverCoord = serverHost.getLocation();
                double dist = clientCoord.distance(serverCoord);
                double latency = dist/this.speedOfLight;
                clientDistances.put(serverHost, latency);
            }
        }
        DEEM mechanism  = new DEEM(q_minPerLLA, q_maxPerLLA, LLAs_Users_Association, user_LLA_Association, LLAs_Devices_Association, device_LLAs_Association, user_device_Latency);
    	mechanism.createMarkets(controlMessageFlag);
    	DEEM_Results results = mechanism.executeMechanism(controlMessageFlag,controlAuctionMessageFlag);
        //Send the messages back to the clients
        for (Map.Entry<DTNHost, DTNHost> entry : results.userDeviceAssociation.entrySet()) {
            DTNHost client = entry.getKey();
            DTNHost server = entry.getValue();
            Message clientMsg = this.clientHostToMessage.get(server);
            String msgId = new String("AuctionResponse_" + client.getName());
            Message m = new Message(host, client, msgId, this.auctionMsgSize);
            m.addProperty("type", "clientAuctionResponse");
            m.addProperty("auctionResult", server);
            m.setAppID(AuctionApplication.APP_ID);
            host.createNewMessage(m);
            System.out.println(SimClock.getTime()+" Execute auction from "+host+" to "+ client+" with result "+server+" "+ msgId);
            super.sendEventToListeners("SentClientAuctionResponse", null, host);
        }
        this.clientHostToMessage.clear();
        this.clientRequests.clear();
        this.serverRequests.clear();
    }

    public int [] getServiceTypes() {
        return this.services;
    }
}

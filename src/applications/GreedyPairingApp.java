package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import core.Coord;
import core.Quartet;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.HashSet;

public class GreedyPairingApp extends AuctionApplication {
	/** Application ID */
	public static final String APP_ID = "ucl.GreedyPairingApp";

    private boolean debug=false;

	/**
	 * Creates a new auction application with the given settings.
	 *
	 * @param s	Settings to use for initializing the application.
	 */
    public GreedyPairingApp(Settings s) {
        super(s);
    }
	
	/**
	 * Copy-constructor
	 *
	 * @param a
	 */
    public GreedyPairingApp(GreedyPairingApp a) {
		super((AuctionApplication) a);
    }
	
    @Override
	public Application replicate() {
		return new GreedyPairingApp(this);
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
            super.sendEventToListeners("ReceivedClientAuctionRequest", (Object) clientMsg, host);
        }
        if (type.equalsIgnoreCase("serverAuctionRequest")) {
            if (this.debug)
        	    System.out.println(SimClock.getTime()+" New server offer "+serverRequests.size());
            if(serverHostToMessage.getOrDefault(msg.getFrom(), null) == null) {
                Message serverMsg = msg.replicate();
                serverHostToMessage.put(serverMsg.getFrom(), serverMsg);
                serverRequests.add(msg.replicate());
                super.sendEventToListeners("ReceivedServerAuctionRequest", (Object) serverMsg, host);
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
        if (currTime - this.lastAuctionTime > this.auctionPeriod) {
            if (this.debug)
                System.out.println("Executing greedy pairing at time: " + currTime + "Period: " + this.auctionPeriod);
            execute_auction(host);
            this.lastAuctionTime = currTime;
        }
    }

	@Override
    public void execute_auction(DTNHost host) {
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
            ArrayList <Integer> serviceTypes = (ArrayList<Integer>) serverMsg.getProperty("serviceType");
            for(Integer serviceType : serviceTypes) { 
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
        }

        HashMap<DTNHost, HashMap<DTNHost, Double>> user_device_Latency = new HashMap();
        
        for(int indx= 0; indx < clientRequests.size();indx++) { 
            Message clientMsg = clientRequests.get(indx);
            DTNHost clientHost = clientMsg.getFrom();
            Coord clientCoord = clientHost.getLocation();
            HashMap<DTNHost, Double> clientDistances = new HashMap();
            user_device_Latency.put(clientHost, clientDistances);
            DTNHost apclient = DTNHost.attachmentPoints.get(clientHost);
            for(int i=0;i<serverRequests.size();i++) {
                Message serverMsg = serverRequests.get(i);
                DTNHost serverHost = serverMsg.getFrom();
                DTNHost apserver = DTNHost.attachmentPoints.get(serverHost);
                double latency = serverHost.getLocalLatency() + clientHost.getLocalLatency();  
                if(apserver!=apclient)
                	latency +=(Integer)(DTNHost.apLatencies.get(apclient.toString()+"to"+apserver.toString())).intValue();

                clientDistances.put(serverHost, latency);
            }
        }
        
        HashMap<DTNHost, ArrayList<Double>> deviceValuations = new HashMap();
        DEEM mechanism  = new DEEM(q_minPerLLA, q_maxPerLLA, LLAs_Users_Association, user_LLA_Association, LLAs_Devices_Association, device_LLAs_Association, user_device_Latency);
    	mechanism.createMarkets(controlMessageFlag);
        ArrayList<Quartet> allValuations = new ArrayList<Quartet>();

        for (Map.Entry<Integer ,HashMap<DTNHost, HashMap<DTNHost, Double>>> outerEntry : mechanism.allLLAvMatrix.entrySet()) {
            Integer service = outerEntry.getKey();
            for (Map.Entry<DTNHost, HashMap<DTNHost, Double>> middleEntry : outerEntry.getValue().entrySet()) {
                DTNHost user = middleEntry.getKey();
                for (Map.Entry<DTNHost, Double> innerEntry : middleEntry.getValue().entrySet()) {
                    DTNHost device = innerEntry.getKey();
                    Double val = innerEntry.getValue();
                    Quartet aQuartet = new Quartet(val, user, device, service);
                    allValuations.add(aQuartet);
                    ArrayList<Double> valArray = deviceValuations.getOrDefault(device, null);
                    if (valArray == null) {
                        valArray = new ArrayList<Double>();
                        deviceValuations.put(device, valArray);
                    }
                    valArray.add(val);
                }
            }
        }

        Collections.sort(allValuations); //larger to smaller in terms of valuation
        //System.out.println("Sorted Valuations: " + allValuations);
        
        HashSet<DTNHost> userSet = new HashSet(user_LLA_Association.keySet());
        HashSet<DTNHost> deviceSet = new HashSet(device_LLAs_Association.keySet());
        DEEM_Results results = new DEEM_Results();
        results.userLLAAssociation = new HashMap(user_LLA_Association);

        for (Quartet aQuartet : allValuations) {
            if (userSet.contains(aQuartet.user) && deviceSet.contains(aQuartet.device)) {
                results.userDeviceAssociation.put(aQuartet.user, aQuartet.device);
                userSet.remove(aQuartet.user);
                deviceSet.remove(aQuartet.device);
                ArrayList<Double> valArray = deviceValuations.get(aQuartet.device);
                //Collections.sort(valArray).reverse();
                Collections.sort(valArray, Collections.reverseOrder()); 
                int indx = valArray.indexOf(aQuartet.valuation);
                double price = valArray.get(indx);
                double nextLowerPrice = price;
                //get the next lower price 
                indx++;
                while(indx < valArray.size()-1) {
                    nextLowerPrice = valArray.get(indx);
                    if (nextLowerPrice != price)
                        break;
                    indx++;
                }
                if (nextLowerPrice == price)
                    results.p.put(aQuartet.device, 0.0);
                else
                    results.p.put(aQuartet.device, nextLowerPrice);
                /*
                double price=0;
                if (indx < valArray.size()-1) {
                    price = valArray.get(indx+1);   
                }
                results.p.put(aQuartet.device, price);
                */
            }
        }
        results.QoSGainPerUser = mechanism.QoSGainPerUser(results.userDeviceAssociation);
        results.QoSPerUser = mechanism.QoSPerUser(results.userDeviceAssociation);

        //Send the auction results back to the clients (null if they are assigned to the cloud)
        for (Map.Entry<DTNHost, DTNHost> entry : results.userDeviceAssociation.entrySet()) {
            DTNHost client = entry.getKey();
            DTNHost server = entry.getValue();
            // Send a response back to a client
            Message clientMsg = this.clientHostToMessage.get(client);
            String msgId = new String("ClientAuctionResponse_" + client.getName());
            Message m = new Message(host, clientMsg.getFrom(), msgId, this.auctionMsgSize);
            m.addProperty("type", "clientAuctionResponse");
            m.addProperty("auctionResult", server);
            HashMap<DTNHost, Double> clientDistances = user_device_Latency.get(client);
            Double latency = clientDistances.get(server);
            m.addProperty("QoS", latency);
            m.setAppID(ClientApp.APP_ID);
            host.createNewMessage(m);
            if (this.debug)
                System.out.println(SimClock.getTime()+" Execute greedyPairing from "+host+" to "+ client+" with result "+server+" "+ msgId+" "+host.getMessageCollection().size());
            //super.sendEventToListeners("SentClientAuctionResponse", null, host);
        }
        //Notify the unassigned clients 
        for(DTNHost client : userSet) {
            DTNHost server = null;
            // Send a response back to a client
            Message clientMsg = this.clientHostToMessage.get(client);
            String msgId = new String("ClientAuctionResponse_" + client.getName());
            Message m = new Message(host, clientMsg.getFrom(), msgId, this.auctionMsgSize);
            m.addProperty("type", "clientAuctionResponse");
            m.addProperty("auctionResult", server);
            m.addProperty("QoS", 0);
            m.setAppID(ClientApp.APP_ID);
            host.createNewMessage(m);
            results.userDeviceAssociation.put(client, null);
            if (this.debug)
                System.out.println(SimClock.getTime()+" Execute greedyPairing from "+host+" to "+ client+" with result "+server+" "+ msgId+" "+host.getMessageCollection().size());
            //super.sendEventToListeners("SentClientAuctionResponse", null, host);
        }
        super.sendEventToListeners("AuctionExecutionComplete", results, host);
        // Notify also the unassigned servers
        for(DTNHost server : deviceSet) {
            DTNHost client = null;
            // Send a response back to a server
            Message serverMsg = this.serverHostToMessage.get(server);
            String msgId = new String("serverAuctionResponse_" + server.getName());
            Message m = new Message(host, serverMsg.getFrom(), msgId, this.auctionMsgSize);
            m.addProperty("type", "serverAuctionResponse");
            m.addProperty("auctionResult", client);
            m.setAppID(ServerApp.APP_ID);
            host.createNewMessage(m);
            if (this.debug)
                System.out.println(SimClock.getTime()+" Execute greedyPairing from "+host+" to "+ server+" with result "+client+" "+ msgId+" "+host.getMessageCollection().size());
            //super.sendEventToListeners("SentServerAuctionResponse", null, host);
        }

        this.clientHostToMessage.clear();
        this.serverHostToMessage.clear();
        this.clientRequests.clear();
        this.serverRequests.clear();
    }

}

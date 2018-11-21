package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import static java.lang.Math.min;
import java.util.ArrayList;

public class AuctionApplication extends Application {
    /** Auction period length */
    public static final String AUCTION_PERIOD_S = "auctionPeriod";
    /** Auction service type (i.e., for application-specific auction) */
    public static final String SERVICE_TYPE_S = "serviceType";
    /** Last time that an auction was executed */
    private double lastAuctionTime;
    /** Period duration (i.e., every AuctionPeriod sec., repeat the auction) */
    private double auctionPeriod;
    /** Client/Server Application that this auction belongs to */
    private int serviceType;
    /** List of client request messages received during the current auctionPeriod */
    ArrayList<Message> clientRequests;
    /** List of server request messages received during the current auctionPeriod */
    ArrayList<Message> serverRequests;

	/** Application ID */
	public static final String APP_ID = "ucl.AuctionApplication";
    //private vars
    private int auctionMsgSize;

	/**
	 * Creates a new auction application with the given settings.
	 *
	 * @param s	Settings to use for initializing the application.
	 */
    public AuctionApplication(Settings s) {
        if (s.contains(AUCTION_PERIOD_S)) {
            this.auctionPeriod = s.getDouble(AUCTION_PERIOD_S);
        }
        if (s.contains(SERVICE_TYPE_S)) {
            this.serviceType = s.getInt(SERVICE_TYPE_S);
        }
        System.out.println("New Service type "+serviceType);
        this.lastAuctionTime = 0.0;
        this.clientRequests = new ArrayList<Message>();
        this.serverRequests = new ArrayList<Message>();
        this.auctionMsgSize = 10; //TODO read this from Settings
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
        this.serviceType  = a.serviceType;
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
            clientRequests.add(msg.replicate());
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
        }
    }

    private void execute_auction(DTNHost host) {
        //int len = Math.min(clientRequests.size(), serverRequests.size());
		//System.out.println("Execute action "+clientRequests.size()+" "+serverRequests.size()+" "+len);
        double currTime = SimClock.getTime();
        
        for(int indx= 0; indx < clientRequests.size();indx++)
        {
            Message clientMsg = clientRequests.get(indx);
            for(int i=0;i<serverRequests.size();i++)
            {
            	if(clientMsg.getFrom()!=serverRequests.get(i).getFrom())
            	{
            		Message serverMsg = serverRequests.get(i);
                    //Send an Auction response to the clientApp
                    Message m = new Message(host, clientMsg.getFrom(), clientMsg.getId(), this.auctionMsgSize);
                    m.addProperty("type", "clientAuctionResponse");
                    m.addProperty("auctionResult", serverMsg.getFrom());
                    m.setAppID(ClientApp.APP_ID);
        			host.createNewMessage(m);
        			System.out.println(SimClock.getTime()+" Execute auction from "+host+" to "+ clientMsg.getFrom()+" with result "+serverMsg.getFrom()+" "+clientMsg.getId());
        			super.sendEventToListeners("SentClientAuctionResponse", null, host);
        			serverRequests.remove(i);
        			break;
            	}
            }
         
        }
        clientRequests.clear();

        this.lastAuctionTime = currTime;
        this.clientRequests.clear();
        this.serverRequests.clear();
    }

    public int getServiceType() {
        return this.serviceType;
    }
}

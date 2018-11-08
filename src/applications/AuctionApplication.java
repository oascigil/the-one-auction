package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;

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
        this.lastAuctionTime = 0.0;
    }
	
	/**
	 * Copy-constructor
	 *
	 * @param a
	 */
    public AuctionApplication(AuctionApplication a) {
		super(a);
        this.lastAuctionTime = a.lastAuctionTime;
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
		if (type==null) return msg; // Not a ping/pong message

        return null;
    }
	/**
	 * Pairs clients and servers through an auction
	 *
	 * @param host to which the application instance is attached
	 */
	@Override
	public void update(DTNHost host) {

    }
}

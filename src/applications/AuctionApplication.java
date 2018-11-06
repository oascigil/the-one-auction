package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;

public class AuctionApplication extends Application {

    private double lastAuction = 0.0;

	/**
	 * Creates a new auction application with the given settings.
	 *
	 * @param s	Settings to use for initializing the application.
	 */
    public AuctionApplication(Settings s) {


    }
	
	/**
	 * Copy-constructor
	 *
	 * @param a
	 */
    public AuctionApplication(AuctionApplication a) {
		super(a);

    }
	
    /**
	 * Handles an incoming message. If the message is a request message 
	 * then it buffers the message
	 *
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
	@Override
	public Message handle(Message msg, DTNHost host) {

    }
}

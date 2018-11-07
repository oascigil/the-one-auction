package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;

/**
 * Simple Client application to request tasks to be executed by Server Applications.
*/

public class ClientApplication extends Application {


    public ClientApplication(Settings s) {


    }
	
    /**
	 * Handles an incoming message. If the message is a ServerResponse message, then report.
	 *
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
    public Message handle(Message msg, DTNHost host) {
		String type = (String)msg.getProperty("type");
		if (type==null) return msg; 

		if (msg.getTo()==host && type.equalsIgnoreCase("serverResponse")) {
            //report the completion time, etc.

        }
    }
	/**
	 * Send request messages to the Server Applications. 
	 *
	 * @param host to which the application instance is attached
	 */
	@Override
	public void update(DTNHost host) {


    }

}

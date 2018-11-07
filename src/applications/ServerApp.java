package applications;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;

/**
 * Simple Server Application to run tasks for Client Applications.
*/

public class ServerApplication extends Application {
    /** Execution time for a request */
    public static final String EXECUTION_TIME_S = "execTime";
    /** Service type for the application */
    public static final String SERVICE_TYPE_S = "serviceType";
    /** Execution time of the application service  */
    private double execTime;
    /** Completion time of the current task being executed */
    private double completionTime;
    /** Flag to indicate if the application is currently busy executing */
    private boolean isBusy;
    /** Type of the service that application provides */
    private int serviceType;
    /** Request message that is currently being executed */
    private Message reqMsg;
    /** Size of the response message */
    private int respMsgSize;
	/** Application ID */
	public String appId;

	/**
	 * Creates a new server application with the given settings.
	 *
	 * @param s	Settings to use for initializing the application.
	 */
	public ServerApplication(Settings s) {
        if (s.contains(EXECUTION_TIME_S)) {
            this.execTime = s.getDouble(EXECUTION_TIME_S);
        }
        if (s.contains(SERVICE_TYPE_S) {
            this.serviceType = s.getInt(SERVICE_TYPE_S);
        }
        this.isBusy = false;
        this.completionTime = SimClock.getTime();
        this.reqMsg = null;
        this.respMsgSize = 1;
        this.appId = "ServerApp" + this.serviceType;
    }

	/**
	 * Handles an incoming message. If the message is a request message, then start executing the task.
	 *
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
	@Override
	public Message handle(Message msg, DTNHost host) {
		String type = (String)msg.getProperty("type");
		if (type==null) return msg; 

        //Start execution if we are the recipient
		if (msg.getTo()==host && type.equalsIgnoreCase("clientRequest")) {
            this.completionTime = SimClock.getTime() + this.execTime;
            this.isBusy = true;
        }

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
			Message m = new Message(host, reqmsg.getFrom(), reqmsg.getId(), this.respMsgSize);
            m.addProperty("type", "serverResponse");
            m.setAppID = this.appId;
			host.createNewMessage(m);
			super.sendEventToListeners("SentResponse", null, host);
            this.isBusy = false;
        }
    }
}

/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import util.Tuple;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class APRouter extends ActiveRouter {

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public APRouter(Settings s) {
		super(s);
		//TODO: read&use epidemic router specific settings (if any)
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected APRouter(APRouter r) {
		super(r);
		//TODO: copy epidemic settings here (if any)
	}
	
	/*@Override
	protected void transferDone(Connection con) {
		Message m = con.getMessage();
		System.out.println(SimClock.getTime()+" APRouter Transfer done "+getHost()+" received "+m.getId()+" "+m.getProperty("type")+" from "+m.getFrom());
		this.deleteMessage(m.getId(), false); // delete from buffer
	}*/
	
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
		System.out.println(SimClock.getTime()+" APRouter Transfer done "+getHost()+" from "+from+" received "+m.getId()+" "+m.getProperty("type")+" from "+m.getFrom());
		from.getRouter().deleteMessage(m.getId(),false);
		return m;
	}
	
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		// then try any/all message to any/all connection
		//this.tryAllMessagesToAllConnections();
		this.tryMessagestoAps();
	}
	
	/**
	 * Tries to send all messages that this router is carrying to all
	 * connections this node has. Messages are ordered using the
	 * {@link MessageRouter#sortByQueueMode(List)}. See
	 * {@link #tryMessagesToConnections(List, List)} for sending details.
	 * @return The connections that started a transfer or null if no connection
	 * accepted a message.
	 */
	protected Connection tryMessagestoAps(){
		List<Connection> connections = getApConnected();

		if (connections.size() == 0) {
			return null;
		}

		@SuppressWarnings(value = "unchecked")
		Tuple<Message, Connection> t =
			tryMessagesForConnected(sortByQueueMode(getMessagesForApConnected()));

		if (t != null) {
			return t.getValue(); // started transfer
		}


		return null;
	}
	
	/**
	 * Returns a list of connections this host currently has with other hosts.
	 * @return a list of connections this host currently has with other hosts
	 */
	protected List<Connection> getApConnected() {
		List<Connection> connections = new ArrayList<Connection>();
		for(Connection con :  getHost().getConnections())
		{
			if(con.getOtherNode(getHost()).isStationary)
				connections.add(con);
		}
		
		return connections;
	}
	
	/**
	 * Returns a list of message-connections tuples of the messages whose
	 * recipient is some host that we're connected to at the moment.
	 * @return a list of message-connections tuples
	 */
	protected List<Tuple<Message, Connection>> getMessagesForApConnected() {
		if (getNrofMessages() == 0 || getConnections().size() == 0) {
			/* no messages -> empty list */
			return new ArrayList<Tuple<Message, Connection>>(0);
		}

		List<Tuple<Message, Connection>> forTuples =
			new ArrayList<Tuple<Message, Connection>>();
		for (Message m : getMessageCollection()) {
			for (Connection con : getConnections()) {
				DTNHost to = con.getOtherNode(getHost());
				DTNHost msgTo = getHost().attachmentPoints.get(m.getTo());
				if(msgTo==to&&to.isStationary) {
					//System.out.println(getHost()+" APRouter send message "+m.getId()+" type "+m.getProperty("type")+" "+m.getTo());
					forTuples.add(new Tuple<Message, Connection>(m,con));
				}
			}
		}

		return forTuples;
	}


	@Override
	public APRouter replicate() {
		return new APRouter(this);
	}

}

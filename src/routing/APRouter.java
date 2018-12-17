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
		//System.out.println(SimClock.getTime()+" APRouter Transfer done "+getHost()+" from "+from+" received "+m.getId()+" "+m.getProperty("type")+" from "+m.getFrom());
		from.getRouter().deleteMessage(m.getId(),false);
		return m;
	}
	
	@Override
	public int receiveMessage(Message m, DTNHost from) {
		/*int recvCheck = checkReceiving(m, from);
		if (recvCheck != RCV_OK) {
			return recvCheck;
		}*/
		System.out.println(SimClock.getTime() +" APRouter Message received at host "+this.getHost()+" with id "+m.getId());

		// seems OK, start receiving the message
		return super.receiveMessage(m, from);
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
	
	protected Connection exchangeDeliverableMessages() {
		List<Connection> connections = getConnections();

		if (connections.size() == 0) {
			return null;
		}

		@SuppressWarnings(value = "unchecked")
		Tuple<Message, Connection> t =
			tryMessagesForConnected(sortByQueueMode(getMessagesForConnected()));

		if (t != null) {
			return t.getValue(); // started transfer
		}

		// didn't start transfer to any node -> ask messages from connected
		for (Connection con : connections) {
			if (con.getOtherNode(getHost()).requestDeliverableMessages(con)) {
				return con;
			}
		}

		return null;
	}
	
	protected List<Tuple<Message, Connection>> getMessagesForConnected() {
		if (getNrofMessages() == 0 || getConnections().size() == 0) {
			/* no messages -> empty list */
			return new ArrayList<Tuple<Message, Connection>>(0);
		}

		List<Tuple<Message, Connection>> forTuples =
			new ArrayList<Tuple<Message, Connection>>();
		for (Message m : getMessageCollection()) {
			for (Connection con : getConnections()) {
				DTNHost to = con.getOtherNode(getHost());
				if (m.getTo() == to) {
					//System.out.println(SimClock.getTime()+ " send message "+m.getId()+" type "+m.getProperty("type")+" "+m.getTo()+" "+m.getReceiveTime());
					if(m.getReceiveTime()<SimClock.getTime())
						forTuples.add(new Tuple<Message, Connection>(m,con));
				}
			}
		}

		return forTuples;
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
	 * Tries to send messages for the connections that are mentioned
	 * in the Tuples in the order they are in the list until one of
	 * the connections starts transferring or all tuples have been tried.
	 * @param tuples The tuples to try
	 * @return The tuple whose connection accepted the message or null if
	 * none of the connections accepted the message that was meant for them.
	 */
	protected Tuple<Message, Connection> tryMessagesForConnected(
			List<Tuple<Message, Connection>> tuples) {
		if (tuples.size() == 0) {
			return null;
		}

		for (Tuple<Message, Connection> t : tuples) {
			Message m = t.getKey();
			Connection con = t.getValue();
			startTransfer(m, con);
			/*if (startTransfer(m, con) == RCV_OK) {
				return t;
			}*/
			if(con.isTransferring())con.finalizeTransfer();
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

			DTNHost msgTo = getHost().attachmentPoints.get(m.getTo());
			if(msgTo!=null) {
				for (Connection con : getConnections()) {
					DTNHost to = con.getOtherNode(getHost());
					if (msgTo==to&&to.isStationary) {
					//if(msgTo==to&&to.isStationary||m.getTo()==getHost()&&m.getTo().isStationary) {
						//System.out.println(getHost()+" APRouter send message "+m.getId()+" type "+m.getProperty("type")+" "+m.getTo());
						forTuples.add(new Tuple<Message, Connection>(m,con));
					}
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

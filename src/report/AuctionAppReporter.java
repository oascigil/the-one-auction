package report;

import applications.AuctionApplication;
import core.Application;
import core.ApplicationListener; 
import core.DTNHost;
import core.Message;
import applications.DEEM_Results;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

public class AuctionAppReporter extends Report implements ApplicationListener {

    /** Map auction run to average price */
    HashMap<Integer, Double> priceTime;
    /** Map auction run to average QoS  */
    HashMap<Integer, Double> qosTime;  
    /** Map auction run to average QoS Gain */
    HashMap<Integer, Double> qosGainTime;  
    /** Map service type to count of requests at the clients */
    HashMap<Integer, Integer> clientServicePopularity; 
    /** Map service type to count of requests at the servers */
    HashMap<Integer, Integer> serverServicePopularity; 
    /** Map auction run to number of server requests */
    HashMap<Integer, Integer> serverRequests; 
    /** Map auction run count to number of client requests */
    HashMap<Integer, Integer> clientRequests; 

    /** The number of times the auction process have run so far */
    private int auctionRun=0;

    public AuctionAppReporter() {
        super();
        this.priceTime = new HashMap();
        this.qosTime = new HashMap();
        this.qosGainTime = new HashMap();
        this.clientRequests = new HashMap();
        this.serverRequests = new HashMap();
        this.clientServicePopularity = new HashMap();
        this.serverServicePopularity = new HashMap();

        this.auctionRun = 0;
        clientRequests.put(0,0);
        serverRequests.put(0,0);
    }

    private void clientAuctionRequest(Message m) {
        Integer service = (Integer) m.getProperty("serviceType");
        Integer numReqs = clientRequests.getOrDefault(auctionRun, null);
        if (numReqs == null) {
            this.clientRequests.put(service, 1);
        }
        else {
            this.clientRequests.put(service, numReqs+1);
        }
        numReqs = clientServicePopularity.getOrDefault(service, null);
        if (numReqs == null) {
            this.clientServicePopularity.put(service, 1);
        }
        else {
            this.clientServicePopularity.put(service, numReqs+1);
        }
    }

    private void serverAuctionRequest(Message m) {
        Integer service = (Integer) m.getProperty("serviceType");
        Integer numReqs = serverRequests.getOrDefault(auctionRun, null);
        if (numReqs == null) {
            this.serverRequests.put(service, 1);
        }
        else {
            this.serverRequests.put(service, numReqs+1);
        }
        numReqs = serverServicePopularity.getOrDefault(service, null);
        if (numReqs == null) {
            this.serverServicePopularity.put(service, 1);
        }
        else {
            this.serverServicePopularity.put(service, numReqs+1);
        }
    }

    private void auctionExecutionComplete(DEEM_Results results) {
        this.auctionRun += 1;
        double totalPrice = 0;
        for (Map.Entry<DTNHost, Double> entry : results.p.entrySet()) {
            Double price = entry.getValue();
            totalPrice += price;
        }
        double averagePrice = totalPrice/results.p.keySet().size();
        this.priceTime.put(this.auctionRun, averagePrice);

        double totalQosGain = 0;
        double totalQos = 0;
        for (Map.Entry<DTNHost, DTNHost> entry : results.userDeviceAssociation.entrySet()) {
            DTNHost user = entry.getKey();
            DTNHost device = entry.getValue();
            totalQosGain += results.QoSGainPerUser.get(user);
            totalQos += results.QoSPerUser.get(user);
        }
        double averageQos = totalQos/results.userDeviceAssociation.keySet().size();
        double averageQosGain = totalQosGain/results.userDeviceAssociation.keySet().size();
        this.qosTime.put(this.auctionRun, averageQos);
        this.qosGainTime.put(this.auctionRun, averageQosGain);
        //move to the next iteration
    }
	
    public void gotEvent(String event, Object params, Application app, DTNHost host) {
        if(!(app instanceof AuctionApplication)) {
            return;
        }
        if(event.equalsIgnoreCase("ReceivedClientAuctionRequest")) {
            Message m = (Message) params; 
            clientAuctionRequest(m);
        }
        if(event.equalsIgnoreCase("ReceivedServerAuctionRequest")) {
            Message m = (Message) params; 
            serverAuctionRequest(m);
        }
        if(event.equalsIgnoreCase("AuctionExecutionComplete")) {
            DEEM_Results results = (DEEM_Results) params;
            auctionExecutionComplete(results);
        }
    }
       
	@Override
	public void done() {
		write("AuctionAppliation stats for scenario " + getScenarioName() +
				"\nsim_time: " + format(getSimTime()));
		String statsText = "AveragePricePerExecution: " + this.priceTime +
            "\nAverageQoSPerExecution: " + this.qosTime + 
            "\nAverageQoSGainPerExecution: " + this.qosGainTime + 
            "\nClientServicePopularity: " + this.clientServicePopularity + 
            "\nServerServicePopularity: " + this.serverServicePopularity;

            write(statsText);
            super.done();
    }
}

package report;

import applications.AuctionApplication;
import applications.ClientApp;
import core.Application;
import core.ApplicationListener; 
import core.DTNHost;
import core.Message;
import core.Quartet;
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
    /** Map user, device pair to QoS deviation from its value at Auction time */
    HashMap<Quartet, ArrayList<Double>> qosDeviation; 
    /** Map auction run to number of user-device pairs assigned */
    HashMap<Integer, Integer> assignedPairs; 
    /** Map auction run to number of migrations by the users */
    HashMap<Integer, Integer> migrationTime;

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
        this.assignedPairs = new HashMap();
        this.migrationTime = new HashMap();
        this.auctionRun = 0;
        this.qosDeviation = new HashMap();
        clientRequests.put(0,0);
        serverRequests.put(0,0);
    }

    private void clientAuctionRequest(Message m) {
        Integer service = (Integer) m.getProperty("serviceType");
        Integer numReqs = clientRequests.getOrDefault(auctionRun, null);
        if (numReqs == null) {
            this.clientRequests.put(this.auctionRun, 1);
        }
        else {
            this.clientRequests.put(this.auctionRun, numReqs+1);
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
        //ArrayList<Integer> serviceTypes = (ArrayList<Integer>) m.getProperty("serviceType");
        Integer numReqs = serverRequests.getOrDefault(this.auctionRun, null);
        if (numReqs == null) {
            this.serverRequests.put(this.auctionRun, 1);
        }
        else {
            this.serverRequests.put(this.auctionRun, numReqs+1);
        }
        /*
        numReqs = serverServicePopularity.getOrDefault(service, null);
        if (numReqs == null) {
            this.serverServicePopularity.put(service, 1);
        }
        else {
            this.serverServicePopularity.put(service, numReqs+1);
        }*/
    }

    private void auctionExecutionComplete(DEEM_Results results) {
        double totalPrice=0;
        Integer numPairs=0, numAllPairs=0, numMigrations=0;
        System.out.println("\n\nAuction Execution Complete:");
        //for (Map.Entry<DTNHost, Double> entry : results.p.entrySet()) {
        for (Map.Entry<DTNHost, DTNHost> entry : results.userDeviceAssociation.entrySet()) {
            DTNHost user = entry.getKey();
            DTNHost device = entry.getValue();
            Double price;
            if (user == null)
                continue;
            if (device != null)
                price = results.p.get(device);
            else
                price = 0.0;
            //if (price == 0.0) 
            //    continue;
            numPairs += 1;
            totalPrice += price;
            if( (results.previousUserDeviceAssociation != null) && (results.previousUserDeviceAssociation.containsKey(user) == true)) {
                DTNHost previousAssociation = results.previousUserDeviceAssociation.get(user);
                DTNHost currentAssociation = device;
                if (previousAssociation != currentAssociation) {
                    numMigrations += 1;
                }
            }
        }
        double averagePrice = 0.0;
        if(numPairs > 0)
            averagePrice = totalPrice/numPairs;

        this.priceTime.put(this.auctionRun, averagePrice);

        double totalQosGain = 0;
        double totalQos = 0;
        numPairs = 0;
        numAllPairs = 0;
        for (Map.Entry<DTNHost, DTNHost> entry : results.userDeviceAssociation.entrySet()) {
            DTNHost user = entry.getKey();
            DTNHost device = entry.getValue();
            if (user == null) {
                System.out.println("User NULL: " + user + " Device: " + device);
                continue;
            }
            if (user != null && device != null)
                numPairs += 1;
            if(device != null) {
                totalQosGain += results.QoSGainPerUser.get(user);
                totalQos += results.QoSPerUser.get(user);
                System.out.println("User: " + user + " Device: " + device + " QoS: " + results.QoSPerUser.get(user) + " QoS_Gain: " + results.QoSGainPerUser.get(user) + " Price: " + results.p.get(device) + " service: " + results.userLLAAssociation.get(user));
            }
            else {
                totalQos += Application.minQoS.get(results.userLLAAssociation.get(user));
                System.out.println("User: " + user + " Device: Cloud" + " QoS:  " + Application.minQoS.get(results.userLLAAssociation.get(user)) + " QoS_Gain: 0" + " Price: 0");
            }
            numAllPairs += 1;
        }
        double averageQos=0.0, averageQosGain=0.0;
        if (numAllPairs > 0) {
            averageQos = totalQos/numAllPairs;
            averageQosGain = totalQosGain/numAllPairs;
        }
        this.assignedPairs.put(this.auctionRun, numPairs);
        this.qosTime.put(this.auctionRun, averageQos);
        this.qosGainTime.put(this.auctionRun, averageQosGain);
        this.migrationTime.put(this.auctionRun, numMigrations);
        
        //move to the next iteration
        this.auctionRun += 1;
        this.clientRequests.put(this.auctionRun, 0);
        this.serverRequests.put(this.auctionRun, 0);
    }

    private void sampleRttMeasurement(Quartet q) {
        Double dev = q.valuation;
        ArrayList<Double> deviations = this.qosDeviation.getOrDefault(q, null);
        // System.out.println("Received an RTT measurement for: " + " client: " + q.user + " server: " + q.device + " pingResult: " + q.valuation);
        if (deviations == null) {
            deviations = new ArrayList<Double>();
            this.qosDeviation.put(q, deviations);
        }
        deviations.add(dev);
    }
	
    public void gotEvent(String event, Object params, Application app, DTNHost host) {
        if(!(app instanceof AuctionApplication) && !(app instanceof ClientApp)) {
            System.out.println("Warning: received an event report from an application other than auction or client");
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
        if(event.equalsIgnoreCase("SampleRTT")) {
            Quartet q = (Quartet) params;
            sampleRttMeasurement(q);
        }

    }

    private static double calculateAverage(ArrayList <Double> values) {
        Double sum = 0.0;
        if(!values.isEmpty()) {
            for (Double v : values) {
                sum += v;
            }
            return sum / values.size();
        }
        
        return sum;
    }
       
	@Override
	public void done() {
		write("AuctionAppliation stats for scenario " + getScenarioName() +
				"\nsim_time: " + format(getSimTime()));

        write("AveragePricePerAuctionExecution:");
        
        for (Map.Entry<Integer, Double> entry : this.priceTime.entrySet()) {
            Integer iter = entry.getKey();
            Double price = entry.getValue();
            String stats = iter + "\t" + price;
            write(stats);
        }

        write("\n\nAverageQoSPerExecution:");
        for (Map.Entry<Integer, Double> entry : this.qosTime.entrySet()) {
            Integer iter = entry.getKey();
            Double qos = entry.getValue();
            String stats = iter + "\t" + qos;
            write(stats);
        }

        write("\n\nAverageQoSGainPerExecution:");
        for (Map.Entry<Integer, Double> entry : this.qosGainTime.entrySet()) {
            Integer iter = entry.getKey();
            Double qos = entry.getValue();
            String stats = iter + "\t" + qos;
            write(stats);
        }

        write("\n\nClientRequestCount\tServerRequestCount\tNumPairsAssignedWithAuction:");
        for (Map.Entry<Integer, Integer> entry : this.clientRequests.entrySet()) {
            Integer iter = entry.getKey();
            Integer clientCount = entry.getValue();
            Integer serverCount = this.serverRequests.get(iter);
            String stats = iter + "\t" + clientCount + "\t" + serverCount + "\t" + this.assignedPairs.get(iter);
            write(stats);
        }
        write("\n\nNumber of migrations per auction run:");
        for (Map.Entry<Integer, Integer> entry : this.migrationTime.entrySet()) {
            Integer iter = entry.getKey();
            Integer migrationCount = entry.getValue();
            String stats = iter + "\t" + migrationCount;
            write(stats);
        }
        

        /*
        write("\n\nQosDeviation:\n");
        for (Map.Entry<Quartet, ArrayList<Double>> entry : this.qosDeviation.entrySet()) {
            Quartet q = entry.getKey();
            ArrayList<Double> devList = entry.getValue();
            double average = AuctionAppReporter.calculateAverage(devList);
            String stats = q.user.getName() + "\t" + q.device.getName() + "\t" + average;
            write(stats);
        }*/
        super.done();
    }
}

/*name: DEEM.java
input:  items, users, user valuations
output: VCG prices and VCG assignment
author: Argyrios
date:   8th of November 2018, 07:04
Notes: - update, values a auction execution, update auction
*/
package applications;
import core.SimClock;
import core.DTNHost;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

class DEEM{
	//Class Data Structures-----------------------------
	//LLA related data structures
	HashMap<Integer, Double> q_minPerLLA;
	HashMap<Integer, Double> q_maxPerLLA;
	HashMap<DTNHost,Integer>  user_LLA_Association;
	HashMap<Integer,ArrayList<DTNHost>>  LLAs_Users_Association;
	HashMap<DTNHost, ArrayList<Integer>>  device_LLAs_Association;
	HashMap<Integer, ArrayList<DTNHost>>  LLAs_Devices_Association;
	HashMap<DTNHost, HashMap<DTNHost, Double>> user_device_Latency;
	//Auction related data structures
	HashMap<DTNHost, Double> p;//device prices
	HashMap<DTNHost, Double> r;//device reserved prices
	HashMap<DTNHost, DTNHost> userDeviceAssociation;
	HashMap<DTNHost, DTNHost> deviceUserAssociation;
	HashMap<DTNHost, Integer> deviceLLAExecution;
    // vMatrix: valuation of users to LLA instances for a specific LLA
	public HashMap<Integer ,HashMap<DTNHost, HashMap<DTNHost, Double>>> allLLAvMatrix; //<LLA_ID,HashMap<user_id, HashMap<LLA_instances_device, Valuation>>>
	ArrayList<VEDAuction> markets;
	//Setting data structures
	static double minimumLatency  = 0.0;//in ms
	static double maximumLatency  = 100.0;//in ms
	static double dp              = 1.0;
	static Object defaulLocationIdentifier  = null; //"veryVeryDistantCloud";
	//static String debuggingDevice  = "f10";
	//-----------------------------Class Data Structures
	//Contructors---------------------------------------
	//Default constructor----------
	public DEEM(){
		q_minPerLLA = new HashMap();
		q_maxPerLLA = new HashMap();
		LLAs_Users_Association  = new HashMap();
		user_LLA_Association = new HashMap();
		LLAs_Devices_Association  = new HashMap();
		device_LLAs_Association = new HashMap();
		user_device_Latency = new HashMap();
		p  = new HashMap();
		r  = new HashMap();
		userDeviceAssociation = new HashMap();
        deviceUserAssociation = new HashMap();
		deviceLLAExecution    = new HashMap();
		allLLAvMatrix         = new HashMap();
		markets             = new ArrayList<VEDAuction>();
	}
	//Constructor------------------
	public DEEM(HashMap<Integer,Double> q_minPerLLA,HashMap<Integer,Double> q_maxPerLLA,HashMap<Integer,ArrayList<DTNHost>>  LLAs_Users_Association,HashMap<DTNHost,Integer> user_LLA_Association,HashMap<Integer,ArrayList<DTNHost>>  LLAs_Devices_Association,HashMap<DTNHost,ArrayList<Integer>> device_LLAs_Association, HashMap<DTNHost, HashMap<DTNHost, Double>> user_device_Latency) {
		this.q_minPerLLA = new HashMap(q_minPerLLA);
		this.q_maxPerLLA = new HashMap(q_maxPerLLA);
		//this.LLAs_Users_Association  = new HashMap(LLAs_Users_Association);
		this.LLAs_Users_Association  = LLAs_Users_Association; //new HashMap(LLAs_Users_Association);
		this.user_LLA_Association = new HashMap(user_LLA_Association);
		//this.LLAs_Devices_Association  = new HashMap(LLAs_Devices_Association);
		this.LLAs_Devices_Association  = LLAs_Devices_Association; //new HashMap(LLAs_Devices_Association);
		this.device_LLAs_Association = new HashMap(device_LLAs_Association);
		this.user_device_Latency = new HashMap(user_device_Latency);
		p  = new HashMap();
		r  = new HashMap();
        this.deviceUserAssociation = new HashMap();
		for(Integer LLA_ID: this.LLAs_Devices_Association.keySet()){
			for (DTNHost device_ID:this.LLAs_Devices_Association.get(LLA_ID)){
				p.put(device_ID,0.0);
				r.put(device_ID,0.0);
                deviceUserAssociation.put(device_ID, null);
			}
		}
		userDeviceAssociation = new HashMap();
		deviceLLAExecution    = new HashMap();
		allLLAvMatrix         = new HashMap();
		markets               = new ArrayList<VEDAuction>();
	}
	//---------------------------------------Contructor with p
	public DEEM(HashMap<Integer,Double> q_minPerLLA,HashMap<Integer,Double> q_maxPerLLA,HashMap<Integer,ArrayList<DTNHost>>  LLAs_Users_Association,HashMap<DTNHost,Integer> user_LLA_Association,HashMap<Integer,ArrayList<DTNHost>>  LLAs_Devices_Association,HashMap<DTNHost,ArrayList<Integer>> device_LLAs_Association, HashMap<DTNHost, HashMap<DTNHost, Double>> user_device_Latency,HashMap<DTNHost, Double> p ) {
		this.q_minPerLLA = new HashMap(q_minPerLLA);
		this.q_maxPerLLA = new HashMap(q_maxPerLLA);
		this.LLAs_Users_Association  = LLAs_Users_Association; //new HashMap(LLAs_Users_Association);
		this.user_LLA_Association = new HashMap(user_LLA_Association);
		this.LLAs_Devices_Association  = LLAs_Devices_Association; //new HashMap(LLAs_Devices_Association);
		this.device_LLAs_Association = new HashMap(device_LLAs_Association);
		this.user_device_Latency = new HashMap(user_device_Latency);
		//p  = new HashMap();
        this.p = p;
		r  = new HashMap();
        this.deviceUserAssociation = new HashMap();
		for(Integer LLA_ID: this.LLAs_Devices_Association.keySet()){
			for (DTNHost device_ID:this.LLAs_Devices_Association.get(LLA_ID)){
                if (this.p.getOrDefault(device_ID, null) == null) {
				    this.p.put(device_ID,0.0);
                }
				r.put(device_ID,0.0);
                deviceUserAssociation.put(device_ID, null);
			}
		}
		userDeviceAssociation = new HashMap();
		deviceLLAExecution    = new HashMap();
		allLLAvMatrix         = new HashMap();
		markets               = new ArrayList<VEDAuction>();
    }
	//Functions-----------------------------------------
	public void createMarkets(boolean controlMessageFlag, HashMap<DTNHost, Double> previousPrices, HashMap<Integer, Double> LLAmigrationOverhead, HashMap<DTNHost, Double> userCompletionTime, HashMap<DTNHost, DTNHost> previousUserDeviceAssociation) {
		//create valuations
		HashMap<DTNHost, HashMap<DTNHost, Double>> vMatrix; //HashMap<user_id, HashMap<LLA_instances_device, Valuation>>
		HashMap<DTNHost, Double> vMatrixForThisUser;
		VEDAuction newAppMarket;
        double QoSGainValuation,userDeviceQoSGainValuation,migrationParallelPrice,migrationOverhead;
		Integer marketID;
		DTNHost[] B;//bidders array, auxiliary data structure for coding clarity
		DTNHost[] I;//items array, auxiliary data structure for coding clarity
		HashMap<DTNHost,Double> market_prices           = new HashMap();
		HashMap<DTNHost,Double> market_reserved_prices  = new HashMap();
        DTNHost userDevice_ID;
        Double currTime = SimClock.getTime();
        //main mechanism execution
		for(Integer LLA_ID: LLAs_Users_Association.keySet()) {//for each market
			vMatrix  = new HashMap();//valuation matrix for this LLA
			if(controlMessageFlag) System.out.println(LLA_ID);
            //System.out.println("LLA_ID: " + LLA_ID + " Users: " + LLAs_Users_Association.get(LLA_ID));
			for(DTNHost user_ID:LLAs_Users_Association.get(LLA_ID)) {
                userDevice_ID = null;
                boolean isAssigned = false;
                if (previousUserDeviceAssociation != null) {
                    if(previousUserDeviceAssociation.containsKey(user_ID)) {
                        isAssigned = true;
                    }
                    userDevice_ID = previousUserDeviceAssociation.getOrDefault(user_ID, null);
                    if (controlMessageFlag) System.out.println("Previous device association for user: " + user_ID + " is " + userDevice_ID);
                }
                //if (userDevice_ID != null) {
                if (isAssigned) {
                    userDeviceQoSGainValuation = user_Device_QoSGain(LLA_ID, user_ID, userDevice_ID);
                    migrationOverhead          = LLAmigrationOverhead.get(LLA_ID);
                    migrationParallelPrice = 0.0;
                    if(userDevice_ID != null) {
                        migrationParallelPrice     = previousPrices.get(userDevice_ID);
                    	p.put(userDevice_ID, migrationParallelPrice);
                    }
                }
                else {
                    userDeviceQoSGainValuation = user_Device_QoSGain(LLA_ID, user_ID, userDevice_ID);
                    migrationParallelPrice     = 0.0;
                    migrationOverhead          = 0.0;
                }
				vMatrixForThisUser  = new HashMap();//for this user
                ArrayList<DTNHost> devicesList = LLAs_Devices_Association.getOrDefault(LLA_ID, null);
                if (devicesList != null) {
				    for (DTNHost device_ID:devicesList){
                        //if (device_ID == null) XXX  
                        //    continue;
                        double userRemainingTime = userCompletionTime.get(user_ID) - currTime;
                        assert (userRemainingTime > 0): "Service remaining time must be positive";
                        if(userRemainingTime <= 0) {
                            System.out.println("Warning: service remaining time is <= 0");
                        }
					    QoSGainValuation  = user_Device_QoSGain(LLA_ID,user_ID,device_ID);
                        if (device_ID != userDevice_ID) {
                            QoSGainValuation    = Math.floor((QoSGainValuation*(userRemainingTime-migrationOverhead)+(userDeviceQoSGainValuation-migrationParallelPrice)*migrationOverhead)/userRemainingTime);
                            //if (QoSGainValuation < 0) {
                            //    QoSGainValuation = 0.0;
                            //}
                        }
					    //p.put(device_ID,0.0);
					    vMatrixForThisUser.put(device_ID,QoSGainValuation);
				    }
                }
				if(controlMessageFlag) System.out.println("\tuser_ID: "+user_ID+", QoS gains valuation: "+vMatrixForThisUser.toString());
				vMatrix.put(user_ID,vMatrixForThisUser);
			}
			//create market
			B  = LLAs_Users_Association.get(LLA_ID).toArray(new DTNHost[LLAs_Users_Association.get(LLA_ID).size()]);
            ArrayList<DTNHost> devicesList = LLAs_Devices_Association.getOrDefault(LLA_ID, null);
            if (devicesList != null) {
    			I  = LLAs_Devices_Association.get(LLA_ID).toArray(new DTNHost[devicesList.size()]);
            }
            else {
                I = null;
            }
			//if(controlMessageFlag) System.out.println("\tLLA: "+LLA_ID+", LLA-Users for this App: "+Arrays.toString(B));
			//if(controlMessageFlag) System.out.println("\tLLA: "+LLA_ID+", LLA-Devices for this App: "+Arrays.toString(I));
			marketID  = LLA_ID;
			market_prices = pricesForThisMarket(LLA_ID);
			market_reserved_prices = reservedPricesForThisMarket(LLA_ID);

			newAppMarket  = new VEDAuction(B,I,vMatrix,market_prices,market_reserved_prices,dp,defaulLocationIdentifier,marketID);
			markets.add(newAppMarket);
			allLLAvMatrix.put(LLA_ID,vMatrix);
		}
        for(Integer LLA_ID: LLAs_Users_Association.keySet()){//create initial prices
            for (DTNHost device_ID:LLAs_Devices_Association.get(LLA_ID)){
                if (p.getOrDefault(device_ID, null)==null){
                    p.put(device_ID,0.0);
                }
            }
        }
	}

	//auxiliary functions----------
	private double user_Device_QoSGain(Integer LLA_ID, DTNHost user_ID, DTNHost device_ID) {
		double term1  = q_minPerLLA.get(LLA_ID)/q_maxPerLLA.get(LLA_ID);
		double term2  = 1.0 - term1;
        Double latency = 100.0; 
        HashMap<DTNHost, Double> devicesMapping = user_device_Latency.getOrDefault(user_ID, null);
        if (devicesMapping == null) {
            System.out.println("Error1: user_Device_QoSGain() unable to find latency for user: " + user_ID + " device: " + device_ID);
            devicesMapping.get(device_ID);
            return Math.floor(latency);
        }
        if (device_ID == null) { 
            /** device is cloud so return 0 gain */
            return 0.0;
        }
        latency = devicesMapping.getOrDefault(device_ID, null);
        if (latency == null) {
            System.out.println("Error2: user_Device_QoSGain() unable to find latency for user: " + user_ID + " device: " + device_ID);
            return Math.floor(latency);
        }
		//Double latency  = user_device_Latency.get(user_ID).get(device_ID);
		if (latency>maximumLatency){//if actual latency exceed the maximum predicted return 0 gain
			return 0.0;
		}
		double term3  = 1.0-(latency-minimumLatency)/maximumLatency;
		double power  = 0.9;//the convexity power equals 0.9 instead of 0.2
		return Math.floor((term1+term2*Math.pow(term3,1.0/power))*q_maxPerLLA.get(LLA_ID)-q_minPerLLA.get(LLA_ID));
		//return Math.round((term1+term2*Math.pow(term3,1.0/power))*q_maxPerLLA.get(LLA_ID)-q_minPerLLA.get(LLA_ID));
	}
	private HashMap<DTNHost,Double> pricesForThisMarket(Integer LLA_ID){
        
        ArrayList<DTNHost> devicesList = LLAs_Devices_Association.getOrDefault(LLA_ID, null);
        if (devicesList != null) {
		    HashMap<DTNHost,Double> market_prices  = new HashMap();
		    for (DTNHost device_ID:devicesList){
			    market_prices.put(device_ID,p.get(device_ID));
		    }
		    return market_prices;
        }
        return null;
	}
	private HashMap<DTNHost,Double> reservedPricesForThisMarket(Integer LLA_ID) {
        ArrayList<DTNHost> devicesList = LLAs_Devices_Association.getOrDefault(LLA_ID, null);
        if (devicesList != null) {
		    HashMap<DTNHost,Double> market_reserved_prices  = new HashMap();
		    for (DTNHost device_ID:devicesList){
			    if (deviceLLAExecution.get(device_ID)==LLA_ID){//if this is the most profitable market of the device, leave the reserved price as it is
				    market_reserved_prices.put(device_ID,r.get(device_ID));
			    }
			    else {//otherwise try to achieve in another market a slightly higher price
				    market_reserved_prices.put(device_ID,r.get(device_ID)+dp);
			    }
		    }
		    return market_reserved_prices;
        }
        return null;
	}
	//execute mechanism------------
	public DEEM_Results executeMechanism(boolean controlMessageFlag,boolean controlAuctionMessageFlag){
		VEDAuctionResult auctionResult;
		HashMap<DTNHost,DTNHost> X_market;//Assignment
		HashMap<DTNHost,Double> p_market;//p: price array for each item
		HashMap<DTNHost,Double> QoSGainPerUser;
		HashMap<DTNHost,Double> QoSPerUser;
		ArrayList<DTNHost> setOfDevicesAssignedInTheMarket;
		DEEM_Results deem_Results  = new DEEM_Results();
		Integer LLA_ID;
		int numberOfIterations=1;
		boolean nextMarketExecutionIteration;
        //initialise reserved prices
        for (DTNHost device_ID:device_LLAs_Association.keySet()){
        	r.put(device_ID,0.0);
        	//r.put(device_ID, device_ID.getRouter().energy.getEnergy());
        	p.put(device_ID,0.0);
        	device_LLAs_Association.put(device_ID,null);
        	deviceUserAssociation.put(device_ID,null);
        	deviceLLAExecution.put(device_ID,null);
        }
		if (controlMessageFlag){
    		System.out.println("------------------Devices Update------------------");
    		System.out.println("Prices: "+p.toString());
    		System.out.println("Reserved Prices: "+r.toString());
    		System.out.println("Device-LLA associations: "+deviceLLAExecution.toString());
    		System.out.println("User-Device associations: "+userDeviceAssociation.toString());
    		System.out.println("--------------------------------------------------");
		}
		do {
			nextMarketExecutionIteration  = false;
			for (int marketIndex=0; marketIndex<markets.size(); marketIndex++){//for each market
				//Execute the auction mechanism---------------------
				LLA_ID  = markets.get(marketIndex).returnMarketID();
				if (controlMessageFlag) System.out.println("market to be Executed: "+LLA_ID+", iteration: "+String.valueOf(numberOfIterations));
				markets.get(marketIndex).updateReservedPrices(reservedPricesForThisMarket(LLA_ID));
				auctionResult  = markets.get(marketIndex).auctionExecution(controlAuctionMessageFlag);
				//Update user-device assignments--------------------
				X_market        = auctionResult.X;//assignments for this market
				setOfDevicesAssignedInTheMarket  = new ArrayList<DTNHost>();
				for (DTNHost userID:X_market.keySet()){
					userDeviceAssociation.put(userID,X_market.get(userID));
                    if(X_market.get(userID) != null) {
                        deviceUserAssociation.put(X_market.get(userID), userID);
                    }
					setOfDevicesAssignedInTheMarket.add(X_market.get(userID));
				}
				//Update device-LLA association only if price is higher, also update the reserved price accordingly
				p_market        = auctionResult.p;//prices of devices from this market
                if (p_market != null) {
    				for (DTNHost device_ID:p_market.keySet()){
    					//if (device_ID.toString().trim().equals(debuggingDevice)){
    						//System.out.println("device's: "+device_ID+", price so far: "+String.valueOf(p.get(device_ID))+", price in this market: "+String.valueOf(p_market.get(device_ID)));
    						//System.out.println(nextMarketExecutionIteration);
    					//}
	    				if (p_market.get(device_ID)>p.get(device_ID)){//if the price of this device is higher than the one achieved in other markets and the item is assigned to a user
		    				if (!setOfDevicesAssignedInTheMarket.contains(device_ID)){//if the device is not assigned
			    				if (controlMessageFlag) System.out.println("device's: "+device_ID+" has not been assigned");
			    				continue;//go to next
				    		}
					    	p.put(device_ID,p_market.get(device_ID));//update price
    						r.put(device_ID,p_market.get(device_ID));//update reserved price
	    					deviceLLAExecution.put(device_ID,LLA_ID);//assign the device to this LLA
		    				nextMarketExecutionIteration  = true;//re-execute the mechanism for each market
			    			if (controlMessageFlag) System.out.println("device's: "+device_ID+", price increased to: "+String.valueOf(p.get(device_ID)));
				    	}
				    	//if (device_ID.toString().trim().equals(debuggingDevice)){
    					//	System.out.println(nextMarketExecutionIteration);
    					//}
				    }
                }
				if (controlMessageFlag){
    				System.out.println("------------------Devices Update------------------");
    				System.out.println("Prices: "+p.toString());
    				System.out.println("Reserved Prices: "+r.toString());
    				System.out.println("Device-LLA associations: "+deviceLLAExecution.toString());
    				System.out.println("User-Device associations: "+userDeviceAssociation.toString());
    				System.out.println("--------------------------------------------------");
				}
			}
			numberOfIterations  += 1;
		} while(nextMarketExecutionIteration);
		//Estimate the QoS and QoS gains of current assignment
		QoSGainPerUser  = QoSGainPerUser(userDeviceAssociation);
		QoSPerUser      = QoSPerUser(userDeviceAssociation);
		if (controlMessageFlag){
			System.out.println("QoS gain per user: "+QoSGainPerUser.toString());
			System.out.println("QoS per user: "+QoSPerUser.toString());
		}
		deem_Results    = new DEEM_Results(numberOfIterations,userDeviceAssociation, deviceUserAssociation, deviceLLAExecution,p,QoSGainPerUser,QoSPerUser);
		return deem_Results;
	}

	//Estimate Users QoS Gain
	public HashMap<DTNHost,Double> QoSGainPerUser(HashMap<DTNHost,DTNHost> userDeviceAssociation){
		HashMap<DTNHost,Double> QoSGainPerUser = new HashMap();
		DTNHost deviceAssignedToTheUser;
		Integer LLA_ID;
		for (DTNHost userID:user_LLA_Association.keySet()){
			deviceAssignedToTheUser  = userDeviceAssociation.get(userID);
			LLA_ID                   = user_LLA_Association.get(userID);
			if (deviceAssignedToTheUser==(DTNHost) defaulLocationIdentifier) QoSGainPerUser.put(userID,0.0);//no gain
			else{
				QoSGainPerUser.put(userID,user_Device_QoSGain(LLA_ID,userID,deviceAssignedToTheUser));
			}
		}
		return QoSGainPerUser;
	}
	public HashMap<DTNHost,Double> QoSPerUser(HashMap<DTNHost, DTNHost> userDeviceAssociation){
		HashMap<DTNHost,Double> QoSPerUser = new HashMap();
		DTNHost deviceAssignedToTheUser;
		Integer LLA_ID;
		for (DTNHost userID:user_LLA_Association.keySet()){
			deviceAssignedToTheUser  = userDeviceAssociation.get(userID);
			LLA_ID                   = user_LLA_Association.get(userID);
			if (deviceAssignedToTheUser== (DTNHost) defaulLocationIdentifier) QoSPerUser.put(userID,q_minPerLLA.get(LLA_ID));//minimum gain
			else{
				QoSPerUser.put(userID,user_Device_QoSGain(LLA_ID,userID,deviceAssignedToTheUser)+q_minPerLLA.get(LLA_ID));
			}
		}
		return QoSPerUser;
	}
	//-----------------------------------------Functions
}

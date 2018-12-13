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
	public DEEM(HashMap<Integer,Double> q_minPerLLA,
                HashMap<Integer,Double> q_maxPerLLA,
                HashMap<Integer,ArrayList<DTNHost>>  LLAs_Users_Association,
                HashMap<DTNHost,Integer> user_LLA_Association,
                HashMap<Integer,ArrayList<DTNHost>>  LLAs_Devices_Association,
                HashMap<DTNHost,ArrayList<Integer>> device_LLAs_Association, 
                HashMap<DTNHost, HashMap<DTNHost, Double>> user_device_Latency) {
		this.q_minPerLLA = new HashMap(q_minPerLLA);
		this.q_maxPerLLA = new HashMap(q_maxPerLLA);
		this.LLAs_Users_Association  = new HashMap(LLAs_Users_Association);
		this.user_LLA_Association = new HashMap(user_LLA_Association);
		this.LLAs_Devices_Association  = new HashMap(LLAs_Devices_Association);
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
	public DEEM(HashMap<Integer,Double> q_minPerLLA,
                HashMap<Integer,Double> q_maxPerLLA,
                HashMap<Integer,ArrayList<DTNHost>>  LLAs_Users_Association,
                HashMap<DTNHost,Integer> user_LLA_Association,
                HashMap<Integer,ArrayList<DTNHost>>  LLAs_Devices_Association,
                HashMap<DTNHost,ArrayList<Integer>> device_LLAs_Association, 
                HashMap<DTNHost, HashMap<DTNHost, Double>> user_device_Latency,
                HashMap<DTNHost, Double> p ) {
		this.q_minPerLLA = new HashMap(q_minPerLLA);
		this.q_maxPerLLA = new HashMap(q_maxPerLLA);
		this.LLAs_Users_Association  = new HashMap(LLAs_Users_Association);
		this.user_LLA_Association = new HashMap(user_LLA_Association);
		this.LLAs_Devices_Association  = new HashMap(LLAs_Devices_Association);
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
	//
	
	public void createMarkets(boolean controlMessageFlag, HashMap<DTNHost, Double> previousPrices, HashMap<Integer, Double> LLAmigrationOverhead, HashMap<DTNHost, Double> userCompletionTime) {
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
		for(Integer LLA_ID: LLAs_Users_Association.keySet()){//for each market
			vMatrix  = new HashMap();//valuation matrix for this LLA
			if(controlMessageFlag) System.out.println(LLA_ID);
			for(DTNHost user_ID:LLAs_Users_Association.get(LLA_ID)){
                userDevice_ID = userDeviceAssociation.getOrDefault(user_ID, null);
                if (userDevice_ID != null) {
                    userDeviceQoSGainValuation = user_Device_QoSGain(LLA_ID, user_ID, userDevice_ID);
                    migrationParallelPrice     = previousPrices.get(userDevice_ID);
                    migrationOverhead          = LLAmigrationOverhead.get(LLA_ID);
                    p.put(userDevice_ID, migrationParallelPrice);
                }
                else {
                    userDeviceQoSGainValuation = 0.0;
                    migrationParallelPrice     = 0.0;
                    migrationOverhead          = 0.0;
                }
				vMatrixForThisUser  = new HashMap();//for this user
                ArrayList<DTNHost> devicesList = LLAs_Devices_Association.getOrDefault(LLA_ID, null);
                if (devicesList != null) {
				    for (DTNHost device_ID:devicesList){
                        if (device_ID == null) 
                            continue;
                        double userRemainingTime = userCompletionTime.get(user_ID) - currTime;
					    QoSGainValuation  = user_Device_QoSGain(LLA_ID,user_ID,device_ID);
                        if (device_ID != userDevice_ID) {
                            QoSGainValuation    = (QoSGainValuation*(userRemainingTime-migrationOverhead)+(userDeviceQoSGainValuation-migrationParallelPrice)*migrationOverhead)/userRemainingTime;
                        }
                        else {
                            QoSGainValuation = QoSGainValuation*userRemainingTime;
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
	private double user_Device_QoSGain(Integer LLA_ID, DTNHost user_ID, DTNHost device_ID){
		double term1  = q_minPerLLA.get(LLA_ID)/q_maxPerLLA.get(LLA_ID);
		double term2  = 1.0 - term1;
		double latency  = user_device_Latency.get(user_ID).get(device_ID);
		if (latency>maximumLatency){//if actual latency exceed the maximum predicted return 0 gain
			return 0.0;
		}
		double term3  = 1.0-(latency-minimumLatency)/maximumLatency;
		double power  = 0.9;//the convexity power equals 0.9 instead of 0.2
		return Math.round((term1+term2*Math.pow(term3,1.0/power))*q_maxPerLLA.get(LLA_ID)-q_minPerLLA.get(LLA_ID));
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
		do{
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
	    				if (p_market.get(device_ID)>p.get(device_ID)){//if the price of this device is higher than the one achieved in other markets and the item is assigned to a user
		    				if (!setOfDevicesAssignedInTheMarket.contains(device_ID)){//if the device is not assigned
			    				continue;//go to next
				    		}
					    	p.put(device_ID,p_market.get(device_ID));//update price
    						r.put(device_ID,p_market.get(device_ID));//update reserved price
	    					deviceLLAExecution.put(device_ID,LLA_ID);//assign the device to this LLA
		    				nextMarketExecutionIteration  = true;//re-execute the mechanism for each market
			    			if (controlMessageFlag) System.out.println("device's: "+device_ID+", price increased to: "+String.valueOf(p.get(device_ID)));
				    	}
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
		}while(nextMarketExecutionIteration);
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
	/*Main----------------------------------------------
    public static void main(String[] args){
    	//LLA QoS------------------------------------------
    	//Minimum QoS for each LLA
    	HashMap<Integer,Double> q_minPerLLA = new HashMap();
    	q_minPerLLA.put("LLA_A",0.0);
    	q_minPerLLA.put("LLA_B",50.0);
    	//Maximum QoS for each LLA
    	HashMap<Integer,Double> q_maxPerLLA = new HashMap();
    	q_maxPerLLA.put("LLA_A",100.0);
    	q_maxPerLLA.put("LLA_B",100.0);
    	//Users per LLAs-----------------------------------
    	HashMap<Integer ,ArrayList<DTNHost>>  LLAs_Users_Association = new HashMap();
    	//LLA_A
    	ArrayList<DTNHost> LLA_A_users  = new ArrayList<DTNHost>();
    	LLA_A_users.add("User1");
    	LLA_A_users.add("User2");
    	LLA_A_users.add("User3");
    	LLAs_Users_Association.put("LLA_A",LLA_A_users);
    	//LLA_B
    	ArrayList<DTNHost> LLA_B_users  = new ArrayList<DTNHost>();
    	LLA_B_users.add("User4");
    	LLA_B_users.add("User5");
    	LLA_B_users.add("User6");
    	LLAs_Users_Association.put("LLA_B",LLA_B_users);
    	//Users-LLAs associations--------------------------
    	HashMap<DTNHost,Integer>  user_LLA_Association = new HashMap();
    	user_LLA_Association.put("User1","LLA_A");
    	user_LLA_Association.put("User2","LLA_A");
    	user_LLA_Association.put("User3","LLA_A");
    	user_LLA_Association.put("User4","LLA_B");
    	user_LLA_Association.put("User5","LLA_B");
    	user_LLA_Association.put("User6","LLA_B");
    	//Devices per LLAs---------------------------------
    	HashMap<Integer,ArrayList<DTNHost>>  LLAs_Devices_Association = new HashMap();
    	//LLA_A
    	ArrayList<DTNHost> LLA_A_Devices  = new ArrayList<DTNHost>();
    	LLA_A_Devices.add("Dev1");
    	LLA_A_Devices.add("Dev2");
    	LLA_A_Devices.add("Dev3");
    	LLA_A_Devices.add("Dev4");
    	LLAs_Devices_Association.put("LLA_A",LLA_A_Devices);
    	//LLA_B
    	ArrayList<DTNHost> LLA_B_Devices  = new ArrayList<DTNHost>();
    	LLA_B_Devices.add("Dev1");
    	LLA_B_Devices.add("Dev2");
    	LLA_B_Devices.add("Dev3");
    	LLA_B_Devices.add("Dev5");
    	LLAs_Devices_Association.put("LLA_B",LLA_B_Devices);
    	//Devices' association to LLAs---------------------
    	HashMap<DTNHost,ArrayList<Integer>>  device_LLAs_Association = new HashMap();
    	//Device 1
    	ArrayList<Integer> dev1Apps  = new ArrayList<Integer>();
    	dev1Apps.add("LLA_A");
    	dev1Apps.add("LLA_B");
    	device_LLAs_Association.put("Dev1",dev1Apps);
    	//Device 2
    	ArrayList<String> dev2Apps  = new ArrayList<String>();
    	dev2Apps.add("LLA_A");
    	dev2Apps.add("LLA_B");
    	device_LLAs_Association.put("Dev2",dev2Apps);
    	//Device 3
    	ArrayList<String> dev3Apps  = new ArrayList<String>();
    	dev3Apps.add("LLA_A");
    	dev3Apps.add("LLA_B");
    	device_LLAs_Association.put("Dev3",dev3Apps);
    	//Device 4
    	ArrayList<String> dev4Apps  = new ArrayList<String>();
    	dev4Apps.add("LLA_A");
    	device_LLAs_Association.put("Dev4",dev4Apps);
    	//Device 5
    	ArrayList<String> dev5Apps  = new ArrayList<String>();
    	dev5Apps.add("LLA_B");
    	device_LLAs_Association.put("Dev5",dev5Apps);
    	//Users-devices latency association----------------
    	HashMap<String, HashMap<String, Double>> user_device_Latency = new HashMap();
    	//User 1---- the aim is to allocated in Dev1
    	HashMap<String, Double> user1DevicesLatencies  = new HashMap();
    	user1DevicesLatencies.put("Dev1",10.0);
    	user1DevicesLatencies.put("Dev2",20.0);
    	user1DevicesLatencies.put("Dev3",30.0);
    	user1DevicesLatencies.put("Dev4",40.0);
    	user_device_Latency.put("User1",user1DevicesLatencies);
    	//User 2---- the aim is to allocated in Dev4
    	HashMap<String, Double> user2DevicesLatencies  = new HashMap();
    	user2DevicesLatencies.put("Dev1",70.0);
    	user2DevicesLatencies.put("Dev2",60.0);
    	user2DevicesLatencies.put("Dev3",50.0);
    	user2DevicesLatencies.put("Dev4",40.0);
    	user_device_Latency.put("User2",user2DevicesLatencies);
    	//User 3---- the aim is to allocated to the cloud
    	HashMap<String, Double> user3DevicesLatencies  = new HashMap();
    	user3DevicesLatencies.put("Dev1",90.0);
    	user3DevicesLatencies.put("Dev2",91.0);
    	user3DevicesLatencies.put("Dev3",92.0);
    	user3DevicesLatencies.put("Dev4",93.0);
    	user_device_Latency.put("User3",user3DevicesLatencies);
    	//User 4---- the aim is to allocated in Dev5
    	HashMap<String, Double> user4DevicesLatencies  = new HashMap();
    	user4DevicesLatencies.put("Dev1",10.0);
    	user4DevicesLatencies.put("Dev2",20.0);
    	user4DevicesLatencies.put("Dev3",5.0);
    	user4DevicesLatencies.put("Dev5",15.0);
    	user_device_Latency.put("User4",user4DevicesLatencies);
    	//User 5---- the aim is to allocated in Dev2
    	HashMap<String, Double> user5DevicesLatencies  = new HashMap();
    	user5DevicesLatencies.put("Dev1",15.0);
    	user5DevicesLatencies.put("Dev2",5.0);
    	user5DevicesLatencies.put("Dev3",20.0);
    	user5DevicesLatencies.put("Dev5",40.0);
    	user_device_Latency.put("User5",user5DevicesLatencies);
    	//User 6---- the aim is to allocated in Dev3
    	HashMap<String, Double> user6DevicesLatencies  = new HashMap();
    	user6DevicesLatencies.put("Dev1",15.0);
    	user6DevicesLatencies.put("Dev2",10.0);
    	user6DevicesLatencies.put("Dev3",20.0);
    	user6DevicesLatencies.put("Dev5",40.0);
    	user_device_Latency.put("User6",user6DevicesLatencies);
    	//execute mechanism--------------------------------
    	boolean controlMessageFlag = true;
    	boolean controlAuctionMessageFlag  = true;
    	DEEM mechanism  = new DEEM(q_minPerLLA,q_maxPerLLA,LLAs_Users_Association,user_LLA_Association,LLAs_Devices_Association,device_LLAs_Association,user_device_Latency);
    	mechanism.createMarkets(controlMessageFlag);
    	mechanism.executeMechanism(controlMessageFlag,controlAuctionMessageFlag);
    	//mechanism.executeMechanism(controlMessageFlag);
    }
	//----------------------------------------------Main
    */
}

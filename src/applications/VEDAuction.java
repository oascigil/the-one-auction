/*name:   VEDAuction.java
input:  items, users, user valuations
output: VEDAuctionResult object
author: Argyrios
date:   2nd of November 2018, 12:04
modified: 15th of November 2018, 6:00
*/
package applications;
import core.DTNHost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;


class VEDAuction{
	//Class Data Structures-----------------------------
	DTNHost B[];//B: set of bidders. integer IDs
	DTNHost I[];//I: set of items, integer IDs
	HashMap<DTNHost, HashMap<DTNHost, Double>> vMatrix;//vMatrix: user valuation
	HashMap<DTNHost,Double> p;//p: price array for each item
	HashMap<DTNHost, Double> r;//r: reserved price array for each item
	double dp;//dp: price increment in price adjustment
	Object defaulLocationIdentifier;//defaultLocationIdentifier: -1
	Integer marketID;
	//-----------------------------Class Data Structures
	//Contructors---------------------------------------
	//Default constructor----------
    public VEDAuction(){
   		B  = new DTNHost[0];
		I  = new DTNHost[0];
		vMatrix = new HashMap();
		p  = new HashMap();
		r  = new HashMap();
		dp = 0.0;
		defaulLocationIdentifier =  null;
		marketID                 = null;
    }
	//Constructor------------------
    public VEDAuction(DTNHost[] B, DTNHost[] I,HashMap<DTNHost, HashMap<DTNHost, Double>> vMatrix,HashMap<DTNHost,Double> p,HashMap<DTNHost, Double> r, double dp, Object defaulLocationIdentifier, Integer marketID){ 
   		this.B = B.clone();
        if (I != null) {
		    this.I = I.clone();
        }
        else {
            this.I = null;
        }
		this.vMatrix = new HashMap(vMatrix);
        if (p!=null) {
		    this.p = new HashMap(p);
        }
        else {
            this.p = null;
        }
        if (r!=null) {
		    this.r = new HashMap(r);
        }
        else {
            this.r = null;
        }
        if (I != null)
        {
		    for (DTNHost itemID:I){
			    if (this.p.get(itemID)<this.r.get(itemID))this.p.put(itemID,this.r.get(itemID));//the initial price is greater or equal to reserved price
            }
		}
		this.dp   = dp;
		this.defaulLocationIdentifier = defaulLocationIdentifier;
		this.marketID                 = marketID;
    }
	//---------------------------------------Contructors
	//Get/Set methods-----------------------------------
	public void updateMarketParticipants(DTNHost[] B, DTNHost[] I, HashMap<DTNHost, HashMap<DTNHost, Double>> vMatrix, HashMap<DTNHost,Double> p,HashMap<DTNHost, Double> r){
   		this.B = B.clone();
		this.I = I.clone();
		this.vMatrix = new HashMap(vMatrix);
		this.p = new HashMap(p);
		this.r = new HashMap(r);
	}
	public Integer returnMarketID(){//market ID equivelant to LLA ID
		return marketID;
	}
	public void updateReservedPrices(HashMap<DTNHost, Double> r){//market ID equivelant to LLA ID
		this.r=r;
        if (r != null) {
            
    		for (DTNHost itemID:r.keySet()){
	    		if (p.get(itemID)<r.get(itemID)){
		    		p.put(itemID,r.get(itemID));
			    }
		    }
        }
	}
	//-----------------------------------Get/Set methods
	//Functions-----------------------------------------
	//Auction Execution Function
	public VEDAuctionResult auctionExecution(boolean controlMessageFlag){
    	//Initialisation---------
    	//DS
    	VEDAuctionResult auctionResult  = new VEDAuctionResult();
    	int numberOfIterations  = 0;
    	HashMap<DTNHost,ArrayList<DTNHost>> D  = new HashMap();//demand correspondence initialisation
    	HashMap<DTNHost, DTNHost>            X  = new HashMap();//Assignment
    	ArrayList<DTNHost>   E  = new ArrayList<DTNHost>();//Excess demand data structure's initialisation
    	ArrayList<DTNHost>   S  = new ArrayList<DTNHost>();//Excess supply data structure's initialisation
    	boolean errorDetector  = false;
    	//VED iterations---------
    	do{
    		numberOfIterations+=1;//increase counter
    		//Step 1. Find demand correspondence
    		findTheDemandCorrespondence(D,controlMessageFlag);
    		//Step 2.a: identify the set of items in excess demand with maximum cardinality
    		ford_Fulkerson(D,X,E,controlMessageFlag);
    		//Step 2.b: Apply E-increase policy to items in excess demand (until elimination)
    		if (controlMessageFlag && p!=null)System.out.println("\titem prices BEFORE E-INCREASE: "+p.toString());
    		for (DTNHost itemID:E) p.put(itemID,p.get(itemID)+dp);
    		if (controlMessageFlag && p!=null){
    			System.out.println("\tExcess demand set E: "+E.toString());
    			System.out.println("\titem prices AFTER E-INCREASE: "+p.toString());
    		}
    		if ((E.size()>0)&&(errorDetector)) return auctionResult; //return auctionResults;
    		if (E.size()>0) continue;//go to the next iteration until you eliminate set in excess demand E
    		errorDetector  = true;
    		//Step 3.a: Identify the set of items in of excess supply with maximum cardinality
    		excessSupplyIdentification(D,X,S,controlMessageFlag);
    		//Step 3.b: Apply S-decrease policy to items in excess supply (until elimination)
    		if (controlMessageFlag && p!=null)System.out.println("\titem prices BEFORE S-DECREASE: "+p.toString());
    		for (DTNHost itemID:S) p.put(itemID,p.get(itemID)-dp);
    		if (controlMessageFlag && p!=null){
    			System.out.println("\tExcess Supply set S: "+S.toString());
    			System.out.println("\titem prices AFTER S-DECREASE: "+p.toString());
    		}
    		if(S.size()==0){//if VCG equilibrium has been achieved
    			auctionResult  = new VEDAuctionResult(numberOfIterations,X,p);
    			if (controlMessageFlag && p!=null){
    				System.out.println("\t==================================================");
    				System.out.println("\tIterations#: "+numberOfIterations);
    				System.out.println("\tVCG Assignments: "+X.toString());
    				System.out.println("\tVCG prices: "+p.toString());
    				System.out.println("\t==================================================");
    			}
			return auctionResult;    			
    		}
    	}while(true);
	}
    //Demand Correspondence Function Identification
	private void findTheDemandCorrespondence(HashMap<DTNHost,ArrayList<DTNHost>> D,boolean controlMessageFlag){
		//local variables
		double maxNetVal,netVal;
		//main iteration
		if (controlMessageFlag) System.out.println("\t\tDemand Correspondence------------");
		for (DTNHost bidderID: B){//for each bidder
			ArrayList<DTNHost> bidderDemandCorrespondence = new ArrayList<DTNHost>();
			maxNetVal  = 0.0;
			bidderDemandCorrespondence.add((DTNHost) defaulLocationIdentifier);
            if (I != null) {
			    for (DTNHost itemID: I){//for each item
				    if (p.get(itemID)>=r.get(itemID)){//if item available
					    netVal  = vMatrix.get(bidderID).get(itemID)-p.get(itemID);//estimate bidder's net valuation for this item
					    if (netVal>maxNetVal){//clear temporal data structure of bidder's demand correspondence
						    bidderDemandCorrespondence.clear();
    						maxNetVal  = netVal;
	    				}
		    			if (netVal==maxNetVal){//if this item brings the maximum net valuation, include it in the temporal data structure
			    			bidderDemandCorrespondence.add(itemID);
				    	}
    				}
	    		}
            }
			D.put(bidderID,bidderDemandCorrespondence);//update demand correspondence
			if (controlMessageFlag) System.out.println("\t\tUser "+bidderID+"'s D: "+D.get(bidderID)+", max net val: "+String.valueOf(maxNetVal));
		}
		if (controlMessageFlag) System.out.println("\t\t------------Demand Correspondence");
	}
	//Excess Demand Set of Maximum Cardinality Identification
	private void ford_Fulkerson(HashMap<DTNHost,ArrayList<DTNHost>> D,
                                HashMap<DTNHost,DTNHost> X,
                                ArrayList<DTNHost> E,
                                boolean controlMessageFlag){
		E.clear();
		HashMap<DTNHost,DTNHost> X_temp = new HashMap<DTNHost,DTNHost>();
		if (controlMessageFlag)System.out.println("\t\tExcess Demand Search-------------");
		if (controlMessageFlag)System.out.println("\t\t\tIteration------------------------");
		while(augmenting_paths_bfs(D,E,X_temp,controlMessageFlag)){
			if (controlMessageFlag){
				System.out.println("\t\t\t\tAssignment: "+Arrays.asList(X_temp));
				System.out.println("\t\t\t------------------------Iteration");
				System.out.println("\t\t\tIteration------------------------");
			}
		}
		if (controlMessageFlag)System.out.println("\t\t-------------Excess Demand Search");
		//Assignments update
		for (DTNHost bidderID: B){
			DTNHost AssigneItemID = X_temp.get(bidderID);
			if (AssigneItemID==null){
				X.put(bidderID, (DTNHost) defaulLocationIdentifier);
			}
			else{
				X.put(bidderID,AssigneItemID);
			}
		}
	}
	private boolean augmenting_paths_bfs(HashMap<DTNHost,ArrayList<DTNHost>> D,ArrayList<DTNHost> E,HashMap<DTNHost, DTNHost> X_temp,boolean controlMessageFlag){
		//Given current assignment return the augmented path of assignments
		//data structures:
		ArrayList<DTNHost> setOfBidders  = new ArrayList<DTNHost>();
		ArrayList<DTNHost> setOfBidders_soFar  = new ArrayList<DTNHost>();
		ArrayList<DTNHost> setOfItems  = new ArrayList<DTNHost>();
		ArrayList<DTNHost> setOfItems_soFar  = new ArrayList<DTNHost>();
		HashMap<DTNHost, DTNHost> BidderLabels = new HashMap<DTNHost,DTNHost>();
		HashMap<DTNHost,DTNHost> ItemLabels   = new HashMap<DTNHost,DTNHost>();
		//Step 0: initial set of bidders, bidders that are not included in the current assignment X
		for (DTNHost bidderID: B){
			if (!X_temp.containsKey(bidderID)) {//if bidder does not exists in the temporal assignment
				setOfBidders.add(bidderID);
				setOfBidders_soFar.add(bidderID);
				BidderLabels.put(bidderID, (DTNHost) defaulLocationIdentifier);//create path
        	}
		}
		if (controlMessageFlag)	System.out.println("\t\t\tStep 0: Initial Bidders "+setOfBidders.toString());
		//main iteration of augmenting paths
		while (true){
			//Step 1: Define the set of Items
			setOfItems.clear();
			for (DTNHost bidderID:setOfBidders){
				if (!X_temp.containsKey(bidderID)){
					for(DTNHost itemID:D.get(bidderID)){
						if (!setOfItems_soFar.contains(itemID)){//if item has not been included so far
							setOfItems.add(itemID);
							setOfItems_soFar.add(itemID);
							ItemLabels.put(itemID,bidderID);
						}						
					}
				}
				else{
					for(DTNHost itemID:D.get(bidderID)){
						if ((X_temp.get(bidderID)!=itemID)&&(!setOfItems_soFar.contains(itemID))){
							setOfItems.add(itemID);
							setOfItems_soFar.add(itemID);
							ItemLabels.put(itemID,bidderID);						
						}
					}
				}
			}
			if (controlMessageFlag)	System.out.println("\t\t\t\tStep 1: Items for this iteration "+setOfItems.toString());
			if (controlMessageFlag)	System.out.println("\t\t\t\t\tSet of Items so far "+setOfItems_soFar.toString());
			//Step 2: augmented path found conditions
			for (DTNHost itemID:setOfItems){//for each item in the set of items
				if ((itemID!= (DTNHost) defaulLocationIdentifier)&&(!X_temp.containsValue(itemID))){//if the item is unassigned
					if (controlMessageFlag)	System.out.println("\t\t\t\tStep 2 condition 2: Unassigned ItemID "+itemID+" in the set exit ");
					updateTemporalAssignment(itemID,X_temp,BidderLabels,ItemLabels);
					return true;
				}
			}
			if (setOfItems.contains((DTNHost) defaulLocationIdentifier)){
				//update assignment
				if (controlMessageFlag)	System.out.println("\t\t\t\tStep 2 condition 1: Null Item in the set exit ");
				updateTemporalAssignment((DTNHost) defaulLocationIdentifier,X_temp,BidderLabels,ItemLabels);
				return true;
			}

			//Step 3: maximum cardinality assignment found
			if (setOfItems.size()==0){
				for (DTNHost excessDemandItemID:setOfItems_soFar){
					if (!E.contains(excessDemandItemID)){
						E.add(excessDemandItemID);
					}
				}
				if (controlMessageFlag)	System.out.println("\t\t\t\tStep 3: No item left exit condition");
				return false;
			}
			//Step 4: Define the set of bidders: include bidders that belong to assignment and have not been investigated during bfs porcess
			setOfBidders.clear();
			for (DTNHost bidderID:B){
				if ((X_temp.containsKey(bidderID))&&(!setOfBidders_soFar.contains(bidderID))&&(setOfItems.contains(X_temp.get(bidderID)))){
					setOfBidders.add(bidderID);
					setOfBidders_soFar.add(bidderID);
					BidderLabels.put(bidderID,X_temp.get(bidderID));
				}
			}
			if (controlMessageFlag)	System.out.println("\t\t\t\tStep 4: Bidders for next iteration "+setOfBidders.toString());
			//Step 5: Go to the next iteration
		}
	}
	private void updateTemporalAssignment(DTNHost destItemID,HashMap<DTNHost,DTNHost> X_temp,HashMap<DTNHost,DTNHost> BidderLabels,HashMap<DTNHost,DTNHost> ItemLabels){
		//initialisation
		DTNHost bidderID;
		DTNHost itemID    = destItemID;
		do{
			bidderID = ItemLabels.get(itemID);
			X_temp.put(bidderID,itemID);
			itemID   = BidderLabels.get(bidderID);
		}while(itemID!= (DTNHost) defaulLocationIdentifier);
	}
	//Excess Supply Set of Maximum Cardinality Identification
	private void excessSupplyIdentification(HashMap<DTNHost,ArrayList<DTNHost>> D,HashMap<DTNHost, DTNHost> X, ArrayList<DTNHost> S,boolean controlMessageFlag){
		S.clear();
		//positive excess demand set is identical to the universally allocated items with positive prices
		ArrayList<DTNHost> U  = FindUnivAllocItems(D,X,controlMessageFlag);//universally allocated items with positive price
		//excess supply
        if (I != null) {
    		for(DTNHost itemID:I){//for each item
	    		if ((p.get(itemID)>r.get(itemID))&&(!U.contains(itemID))){//if the item has a higher price than its reserved one and is not included in U
		    		S.add(itemID);
			    }
		    }
        }
	}
	private ArrayList<DTNHost> FindUnivAllocItems(HashMap<DTNHost,ArrayList<DTNHost>> D,HashMap<DTNHost,DTNHost> X,boolean controlMessageFlag){
		ArrayList<DTNHost> U = new ArrayList<DTNHost>();//Universally allocated items with positive prices
		ArrayList<DTNHost> T = new ArrayList<DTNHost>();//Bidders requesting universally allocated items
		ArrayList<DTNHost> W = new ArrayList<DTNHost>();//Provisionally allocated items
		boolean wSubsetOfU;
		if (controlMessageFlag)System.out.println("\t\t\tUn. Allocated Items Search-------");
		//Step 0: Initialise universally allocated items with items in the D of a user that is assigned the null item
		for (DTNHost bidderID:B){
			for (DTNHost itemID:D.get(bidderID)){
				if ((itemID!= (DTNHost) defaulLocationIdentifier)&&(itemID!=X.get(bidderID))&&(p.get(itemID)>r.get(itemID))&&(!U.contains(itemID))) U.add(itemID);
			} 
		}

		if (controlMessageFlag)	System.out.println("\t\t\tStep 0: Initial Positive Excess Demand Set "+U.toString());
		do{
			//Step 1: Find bidders allocated to items in E_positive
			T.clear();
			for (DTNHost bidderID:B){
				if (U.contains(X.get(bidderID))) T.add(bidderID);
			}
			if (controlMessageFlag)	System.out.println("\t\t\t\tStep 1: Bidders requesting items in U "+T.toString());
			//Step 2: Find set of provisionally allocated items with positive price demanded by bidders in T
			W.clear();
			for (DTNHost bidderID:T){
				for (DTNHost itemID:D.get(bidderID)){
					if (itemID== (DTNHost) defaulLocationIdentifier) continue;
					if ((p.get(itemID)>r.get(itemID))&&(!W.contains(itemID))){
						W.add(itemID);
					}
				}
			}
			if (controlMessageFlag)	System.out.println("\t\t\t\tStep 2: Provisionally allocated items with positive price "+W.toString());
			//Step 3: Termination conditions W subset of U, otherwise U=U union W
			wSubsetOfU =  true;
			for(DTNHost itemID: W){
				if (!U.contains(itemID)){
					wSubsetOfU = false;
					U.add(itemID);
				}
			}
			if (controlMessageFlag)	System.out.println("\t\t\t\tStep 3: Updated items U: "+U.toString());
			if (wSubsetOfU){
				if (controlMessageFlag)System.out.println("\t\t\t-------Un. Allocated Items Search");
				return U;
			}
		}while(true);
	}
	//-----------------------------------------Functions
}

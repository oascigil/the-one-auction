# -*- coding: utf-8 -*-
import numpy as np
import sys
import random
import re 

def RepresentsInt(s):
    try: 
        int(s)
        return True
    except ValueError:
        return False


# Usage ./addPoints -st locations.txt default_settings.txt num_services
num_services = 0
if(len(sys.argv) >=3):
    filename = sys.argv[3]
    if (len(sys.argv) >= 4):
        nrofServices = int(sys.argv[4])
else:
    filename = "Manchester_test"
with open((filename), 'r') as F:
    data = F.readlines()
    #print(data)
    for d in data:
        if "nrofHostGroups" in d:
            groups = d
            print(groups)
            groups = groups.split()
            groups = groups[2]
            print(groups)  
            
    nrofGroups = 0
    print "This is the name of the script: ", sys.argv[0]
    print "Number of arguments: ", len(sys.argv)
    print "The arguments are: " , str(sys.argv)
    if(len(sys.argv) >= 2):
        type = sys.argv[1]
        if(type == '-st'):
            print("Stationary setting chosen")
            stat_filename = sys.argv[2]
            
            print("Stationary Points file: " + stat_filename)
            lines =[]
            with open(stat_filename,'r') as SF:
                lines = SF.readlines()
            num = len(lines)
            #add error checking for number of lines later
            pos =[[0 for x in range(2)] for y in range((int)(num))]
            count = 0
            for li in lines:
                li = li.split()
                pos[count][0] = li[0]
                pos[count][1] = li[1]
                count = count + 1
            print("Set of points to add: ")
            print(pos)
            newfile = filename + "_st_" + str(num) + ".txt"
            print("Writing to: " + newfile)
            nf = open(newfile,'w')
            s = set()
            for d in data:
                nf.write(d)
                if "Group" in d and "#" not in d: 
                    x = re.split(r'(\Group+|\.+|]+)', d)
                    if len(x) >= 2 and RepresentsInt(x[2]):
                        group_number = int(x[2])
                        if group_number not in s:
                            s.add(group_number)  
                            nf.write("Group" + str(group_number) + ".nrofApplications = 2\n")
                            nf.write("Group" + str(group_number) + ".application1 = clientApp" + str(group_number) + "\n")
                            nf.write("Group" + str(group_number) + ".application2 = serverApp" + str(group_number) + "\n")
                            nrofGroups += 1
            nf.write("\n")
            # Application Settings
            auctionPeriod = 10.0
            exec_time_low = 30.0
            exec_time_high = 30.0
            # Service Settings
            nrofServices = 3
            auction_apps = []
            server_apps = []
            services = range(nrofServices)
            # Client app settings
            taskFreq = 10.0
            taskReqMsgSz = 100
            
            nf.write("# service setttings\n")
            nf.write("Scenario.nrofServices = " + str(nrofServices) + "\n")
            for i in range(nrofServices):
                nf.write("Service" + str(i) + ".executionTime = " + str(random.uniform(exec_time_low, exec_time_high)) + "\n")
            nf.write("\n")
            
            nf.write("# auction app setttings\n")
            for i in range(nrofServices):
                nf.write("auctionApp" + repr(i+1) + ".type = AuctionApplication\n")
                nf.write("auctionApp" + repr(i+1) + ".auctionPeriod = " + repr(auctionPeriod) + "\n")
                nf.write("auctionApp" + repr(i+1) + ".serviceType = " + str(i) + "\n\n")
                auction_apps.append("auctionApp" + str(i+1))
            
            nf.write("# Server apps")
            for i in range(nrofGroups):
                nf.write("serverApp" + repr(i+1) + ".type = ServerApp\n")
                random_subset = [services[j] for j in sorted(random.sample(xrange(len(services)), random.randint(1, len(services))))]
                subset = ','.join(map(str, random_subset)) 
                nf.write("serverApp" + repr(i+1) + ".serviceTypes = " + subset + "\n") 
                server_apps.append("serverApp" + str(i+1))
                nf.write("\n")

            nf.write("# Client apps")
            nf.write("\n")
            for i in range(nrofGroups):
                nf.write("clientApp" + str(i+1) + ".type = ClientApp\n")
                nf.write("clientApp" + str(i+1) + ".taskReqFreq = " + str(taskFreq) + "\n")
                nf.write("clientApp" + str(i+1) + ".taskReqMsgSize = " + str(taskReqMsgSz) + "\n")
                nf.write("\n")

            gn = (int)(groups) 
            for p in pos:
                #print(p[0])
                gn = gn + 1 
                group = "Group" + (str)(gn) + "."
                nf.write(group + "groupID = a\n")
                nf.write(group + "nrofHosts = 1\n")
                nf.write(group + "movementModel = StationaryMovement\n")
                nf.write(group + "nodeLocation = " + repr(int(float((p[0])))) + "," + repr(int(float((p[1])))) + "\n")
                nf.write(group + "nrofInterfaces = 2\n")
                nf.write(group + "interface1 = wifiInterface\n")
                nf.write(group + "interface2 = backhaul\n")
                nf.write(group + "router = APRouter\n")
                if len(auction_apps) > 0:
                    nf.write(group + "nrofApplications = 1\n")
                    auctionApp = random.choice(auction_apps)
                    auction_apps.remove(auctionApp)
                    nf.write(group + "application1 = " + auctionApp + "\n")
                else:
                    nf.write(group + "nrofApplications = 0\n")
                    
                nf.write("\n")              
                
        elif(type == '-so'):
            print("Source")
        elif(type == '-c'):
            print("Client")
        elif(type == '-t'):
            print("Tourist")
        else:
            print("Sorry Wrong use of arguments")


    


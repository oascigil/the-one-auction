import numpy as np
import sys

# Usage: ./addPoints -st locations_file settings_file

if(len(sys.argv) >=3):
	filename = sys.argv[3]
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
			for d in data:
				nf.write(d)
			nf.write("\n")
			gn = (int)(groups)
			for p in pos:
				#print(p[0])
				gn = gn + 1 
				group = "Group" + (str)(gn) + "."
				nf.write(group + "groupID = s\n")
				nf.write(group + "nrofHosts = 1\n")
				nf.write(group + "movementModel = StationaryMovement\n")
				nf.write(group + "nodeLocation = " + p[0] + "," + p[1] + "\n")
				nf.write("\n")				
				
		elif(type == '-so'):
			print("Source")
		elif(type == '-c'):
			print("Client")
		elif(type == '-t'):
			print("Tourist")
		else:
			print("Sorry Wrong use of arguments")


	


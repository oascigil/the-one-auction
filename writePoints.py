#Scipt to be used on the commandline to write a file of x and y point locations
#Options are 
# 1) random points along roads defined by wkt file
# 2) equally spaced points in a rectangle defined by given sizex and sizey
# 3) randomly placed points along a given sizex sizey

# Usage ./writePoints -wkt wkt_file distance out_file

import numpy as np
import sys
import random
import math

def check_distance_with_existing(x_new, y_new, points, distance):
    
    for x, y in points:
        if dist(x,y, float(x_new), float(y_new)) < distance/2:
            return False 

    return True

def dist(x1, y1, x2, y2):
    x_delta = float(x1)-float(x2)
    y_delta = float(y1)-float(y2)

    d = math.sqrt(math.pow(x_delta,2) + math.pow(y_delta,2))

    return d

def generate_points(coordinates, distance):
    points = []
    indx  = 0
    x1 = 0.0
    y1 = 0.0
    for x,y in coordinates:
        if indx == 0:
            points.append([x,y])
        indx += 1
        if len(coordinates) > indx:
            x1 = float(coordinates[indx][0])
            y1 = float(coordinates[indx][1])
        else:
            break
        x = float(x)
        y = float(y)

        d = dist(x, y, x1, y1)
        x_delta = abs(x-x1)
        y_delta = abs(y-y1)
        
        if x_delta == 0:
            m = 1
        else:
            m = y_delta/x_delta

        alpha = math.atan2(y_delta, x_delta) # in radians
        if distance < d:
            x_betw = x
            y_betw = y
            total_distance = distance
            while total_distance < d:
                if x1 > x:
                    x_betw += math.cos(alpha)*distance
                else:
                    x_betw -= math.cos(alpha)*distance
                if y1 > y:
                    y_betw += math.sin(alpha)*distance
                else:
                    y_betw -= math.sin(alpha)*distance

                if check_distance_with_existing(x_betw, y_betw, points, distance):
                    points.append([x_betw, y_betw])
                total_distance += distance
        else:
            if check_distance_with_existing(x1, y1, points, distance):
                points.append([x1, y1])

    return points
            
def parseWKT(filename):
    pos =[]
    with open(filename, 'r') as f:
        data = f.readlines()
        for d in data:
            if("LINESTRING" in d):
                d = d.lstrip("LINESTRING (")
                d = d.rstrip("\n")
                d = d.rstrip(")")
                d = d.strip('\'')
                #print(d)
                li = d.split(',')
                #print(li)
                for i in range(0,(len(li)-1)):
                    p = li[i].split()
                    #print(p)
                    pos.append(p)
                    #print("x coord: " + p[0])
                    #print("y coord: " + p[1])
    return pos

if(len(sys.argv) >= 2):
        type = sys.argv[1]
        if(type == '-wkt'):
            filename = sys.argv[2]
            print("wkt file setting chosen")
            if(len(sys.argv)==3):
                print("Sorry need to enter how many points")
            else:
                pos = parseWKT(filename)
                distance = int(sys.argv[3])
                print "User entered a distance of: " + repr(distance)
                points = generate_points(pos, distance)
                print "Points = " + repr(points)
                with open(sys.argv[4],'w') as newfile:
                    for x,y in points:
                        newfile.write(str(x) + " " + str(y) + "\n")
                newfile.close() 
                
        elif(type == '-rect'):
            print("rectangle setting chosen")
            sizex = int(sys.argv[2])
            sizey = int(sys.argv[3])
            num = int(sys.argv[4])
            newfile = sys.argv[5]

            if(sizex == sizey):
                #square
                stepx = sizex % (num/2)
            
            
        elif(type == '-rand'):
            print("random setting chosen")
            sizex = int(sys.argv[2])
            sizey = int(sys.argv[3])
            num = int(sys.argv[4])
            newfile = sys.argv[5]
            with open(newfile,'w') as n:
                for x in range(num):
                    rintx = random.randint(0,sizex)
                    rinty = random.randint(0,sizey)
                    n.write(str(rintx) + " " + str(rinty) + "\n")
            
else:
    print("Sorry wrong use of command line arguments")


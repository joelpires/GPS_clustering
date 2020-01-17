#Definition of inputs and outputs
#==================================
##[my scripts]=group
##points=multiple vector
##network=multiple vector
##output=output vector


from PyQt4.QtCore import *
from PyQt4.QtGui import *

from qgis.core import *
from qgis.gui import *
from qgis.networkanalysis import *

from processing.tools.vector import VectorWriter
import numpy as np
from math import sin, cos, sqrt, atan2, radians
import matplotlib.pyplot as plt
import glob, os, sys
from operator import itemgetter

#TODO: make relative paths
path_to_trips = "C:/Users/Joel Pires/Desktop/new/csv/"
path_to_busStops = "C:/Users/Joel Pires/Desktop/new/busStops/"

listLines = network.split(';')
listTrips = points.split(';')

tripNames = []
linesNames = []

for trip in listTrips:
    aux1 = trip.split("?")
    aux2 = aux1[0].split(".")
    aux3 = aux2[0].split("/")
    aux4 = aux3[-1]
    tripNames.append(aux4)

h= 0
for line in listLines:
    h +=1
    aux1 = line.split(".")
    aux2 = aux1[0].split("/")
    aux3 = aux2[-1]
    inesNames.append(aux3)


#===================================================================================
#RUN THE PRINCIPAL ALGORITHM

thresholdStopsMin = 10       #distance in meters considered acceptable
thresholdStopsMax = 200     #distance in meters considered significant
thresholdMatchscore = 10    #distance in meters -> the threshold for the average error deviation
thresholdLinhasPerto = 1000 #distance in meters to consider lines close to a trip
R = 6373.0  #Earth radius

def main():
    for j in range(0, len(listTrips)):  #DISCLAIMER: THE ALGORITHM ASSUMES THAT THE BusStops ARE ALREADY TIME SEQUENTIATED

        nearBusLines = []
        closestInitialBusStop = ""
        initialBusLine = -1
        initialBusStop = -1
        closestFinalBusStop = ""
        finalBusLine = -1
        finalBusStop = -1

        point_layer = processing.getObject(listTrips[j])

        for i in range(0, len(listLines)):

            network_layer = processing.getObject(listLines[i])

            #retrieve the busStops
            busStopsFilename  = path_to_busStops + linesNames[i] ".shp"
            stops_layer = processing.getObject(busStopsFilename)

            # prepare graph
            vl = network_layer
            director = QgsLineVectorLayerDirector( vl, -1, '', '', '', 3 )
            properter = QgsDistanceArcProperter()
            director.addProperter( properter )
            crs = vl.crs()
            builder = QgsGraphBuilder( crs )

            #  ========== prepare busStop points
            features = processing.features(stops_layer)
            stops_count = stops_layer.featureCount()
            busStops = []

            for f in features:
                busStops.append((f.attributes()[0], f.attributes()[1], f.attributes()[2]))

            #  ========== prepare TRIP points
            features = processing.features(point_layer)
            point_count = point_layer.featureCount()

            points = []
            auxPoints = []

            for f in features:
                auxPoints.append((f.geometry().asPoint()[0], f.geometry().asPoint()[1], f.attributes()[2]))
                points.append(f.geometry().asPoint())

            newPoints = sorted(auxPoints, key=itemgetter(2))    #newPoints[0] indicate the beginning of the trip; newPoints[-1] indicate the end of the trip;
            tiedPoints = director.makeGraph( builder, points )
            graph = builder.graph()

            convert = [0]*point_count
            # l - guarda os indices antigos; o - guarda os novos indices sorted
            for l in range(0, busStops):
                for o in range(0, point_count):
                    if(auxPoints[l][2] == newPoints[o][2]):
                        convert[o] = l
                        break

            #  ========== Vamos determinar qual a paragem mais próxima
            initialStop = ""
            bestDistanceInit = 100000000
            finalStop = ""
            bestDistanceFinal = 100000000

            for s in range(0,stops_count):

                distanceToInit = calculateDistance(busStops[s], newPoints[0])
                distanceToFinal = calculateDistance(busStops[s], newPoints[-1])

                if(distanceToInit < bestDistanceInit):
                    initialStop = s
                    bestDistanceInit = distanceToInit

                if(distanceToFinal < bestDistanceFinal):
                    finalStop = s
                    bestDistanceFinal = distanceToFinal

            #  ========== Atualizar paragem mais próxima de todas as linhas em relacao ao inicio e fim da trip-j
            if(bestDistanceInit < closestInitialBusStop):
                initialBusLine = i
                initialBusStop = s
                closestInitialBusStop = bestDistanceInit

            if(bestDistanceFinal < closestFinalBusStop):
                finalBusLine = i
                finalBusStop = s
                closestFinalBusStop = bestDistanceFinal


            if (initialStop <= thresholdStopsMax and finalStop <= thresholdStopsMax):
                #  ========== fazer um troço temporário
                stretchStops = []
                inside = 0
                for s in range(0,stops_count):
                    if (busStops[s][0] == initialStop[0] and busStops[s][1] == initialStop[1] ):
                        stretchStops.append(s)
                        inside = 1
                    if (inside == 1):
                        stretchStops.append(s)
                    if (busStops[s][0] == finalStop[0] and busStops[s][1] == finalStop[1] ):
                        stretchStops.append(s)
                        inside = 0

                stretch = map-match(stretchStops, network_layer)






def calculateDistance(point1, point2):
    lat1 = radians(point1[0])
    lon1 = radians(point1[1])
    lat2 = radians(point2[0])
    lon2 = radians(point2[1])

    dlon = lon2 - lon1
    dlat = lat2 - lat1

    a = sin(dlat / 2)**2 + cos(lat1) * cos(lat2) * sin(dlon / 2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return R * c




def mapMatch():



main()

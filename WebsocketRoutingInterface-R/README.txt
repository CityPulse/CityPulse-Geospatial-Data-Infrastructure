Important Hint:  do not put any spaces in the commands that you send to the system.


V2.0 Major API Update, all old API calls are hereby deprecated/deactivated

The intgrated cost functions define the goal of the pathfinder. The following goals are available:
distance, time, pollution, combined

Current status of the cost function is:
  if if (goal == “distance"){
      cost = "distanceMulti * length"
  }else if (goal == "time"){   
    cost = "timeMulti * maxspeed_forward * length"
    }
  } else if (goal == "pollution"){
    cost = "pollutionMulti * length"
  } else if (goal == "combined"){
    cost = "distanceMulti * timeMulti * pollutionMulti * maxspeed_forward * length"
  }

To calculate the time, the algorithm uses maximum road speed. To get an assumption please take into account a multiplicator like*0.75 or let google decide how long it will really take (also taking ttraffic lights and crossings into account)

# getCityRoute(srcXlon,srcYlat,trgXlon,trgxlat,costMode,optional:numberOfRoutes)
- Parameters: 
  costMode = distance, time, pollution, combined
- Action:	Find path with minimum cost between two nodes AND resets the cost multiplicator for alternate routes automatically
- Returns:  CSV (Semicolon separated) with header containing the following columns (if you need other format, just ask):
	geometry: linestring or multilinestring
  length_m: length(meters) 
  time_s:   estimated time(seconds) - Has do be adapted, currently on top speed
  total_cost: Total cost of this path
examples: getCityRoute(10.1580607288361,56.1477407419276,10.1165896654129,56.2257947825602,distance)
example:getCityRoute(10.1580607288361,56.1477407419276,10.1165896654129,56.2257947825602,distance,30)
sweden: getCityRoute(17.092095, 58.883049, 17.102095, 58.983049, distance, 1)


# resetAllMultiplicators(optional:costMode) 
- Parameters: 
  costMode = distance, time, pollution, combined
             if not defined or 'combined' is used, all multiplicators are reset to 1.0
- Action: Resets cost multiplicators to 1.0
- Returns: number of updated multiplicators
- examples: resetAllMultiplicators(combined)
            resetAllMultiplicators(pollution)
            resetAllMultiplicators(distance)
            resetAllMultiplicators(time)

#updateCostCircular(xLon,xLat,costMode,radius_m,costMultiplicator)
- Action: updates cost multiplicators
- Parameters:
  xLon: Longitude in WGS84
  xLat: Latitude in WGS84
  costMode: distance, time, pollution (combined would not make any sense here)
  radius_m: radius surrounding coordinate in meters (all edges that touch the circle are affected)
  costMultiplicator: double value, which defines the cost multiplication (dependent on the costMode during route selection)
- Returns:   number of updated multiplicators
example:  updateCostCircular(10.158,56.21,pollution,400,5.0)
          updateCostCircular(10.156,56.20,distance,250,4.0)
          updateCostCircular(10.155,56.18,time,100,3.0)




# V1.0
The routing is based on OSM data of Aarhus.
Currently it utilises a simple Dijkstra algorithm where the length of an edge is used as the baisc cost of the edge. I'm currently looking into using a more sophisticated model, which also utilises the  type of street/maxspeed etc. But it would not change the basic interface approach. 

To specify areas that should be avoided it is possible to specify a cost-muliplicator for the edges of the graph. This multiplicator can be set either by the osm-id, or a bounding box. If you need other types for setting the cost-multiplicator, please make a feature request. 

Currently we have a "single user" version where every edge can only have one cost-multiplicator but for future use we can handle it applpication-wide or user-based.

The Routing System is reachable via websocket so you can easily implement it in your component or use a simple browser plugin to check it out. It is running on:
ws://pcsd-118.et.hs-osnabrueck.de:7686

We can start with the command (please ignore the Hash# in front of the commands)


Internally used: 
# resetAllOsmIdCost() 
- Action:	Reset all cost multiplicators to 1.0
- Returns: 	number of updated multiplicators

To operate quickly on a graph we need to get the startpoint end endpoint nodeIDs in our graph. 
# getNearestId(x-coordinate,y-coordinate)
- Action: 	Search for a node id near the coordinate
- Returns: 	node id

To alter the cost-multiplicator you can use:
# updateOsmIdCost(osm_id, cost_multiplicator) 
- Action:	Update cost-multiplicator for one specific osm-id
- Returns:	Count of updated multiplicators (edge based)

or:
# updateCostArea(con,min_x,min_y,max_x,max_y,costMultiplicator)
- Action:	Updates cost multplicator for an area intersecting this bounding box
- Returns: 	Number of updated multiplicators


EXAMPLE

If want to find a route between  10.1580607288361,56.1477407419276 and 10.1165896654129,56.2257947825602:
Get the node ids:
# getNearestNodeId(10.1580607288361,56.1477407419276)
- Returns: 38735
# getNearestNodeId(10.1165896654129,56.2257947825602)
- Returns: 12260

# getRoutingWayWeighted(38735,12260)
- Returns: large csv table describing the way

Result used osm_id 77009584 wich should be avoided. So we can block it:
# updateOsmIdCost(77009584,90.0)
- Returns: 7 (number of edges affected)

# getRoutingWayWeighted(38735,12260)
- Returns: large csv table without ousing osm_id 7709584

## old help() TEXT
# resetAllOsmIdCost() - Reset all cost multiplicators to 1.0, Returns: number of updated multiplicators\n",
                        #"getNearestId(x-coordinate,y-coordinate)\n",
                        #"updateOsmIdCost(osm_id, cost_multiplicator) Returns: number of updated multiplicators\n",
                        #"updateCostArea(con,min_x,min_y,max_x,max_y,costMultiplicator) Returns: number of updated multiplicators\n",
                        #"getRoutingWay(source_node,target_node), returns: gid,length,maxspeed_forward,osm_id,the_geom,source,target,node,edge_id,fullcost,osm_id,citycostmulti (see readme)\n",
                        #"getRoutingWayWeighted(source_node,target_node), returns: length,maxspeed_forward,osm_id,the_geom,source,target,node,edge_id,fullcost,citycostmulti (see readme)\n")
                        

 

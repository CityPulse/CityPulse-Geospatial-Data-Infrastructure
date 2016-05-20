# RoutingLib

getNearestNodeId <- function(con,x,y){
  #print(x)
  sql = paste("select id from ways_vertices_pgr order by st_distance(the_geom, st_setsrid(ST_GeomFromText('Point(", x," ",y, ")'), 4326)) limit 1", sep="")
  #print(sql)
  response = dbSendQuery(con,sql)
  #print(response)
  way=dbFetch(response,n=-1)
  #print(way)
  return (way$id[1]);
}

goal<-c("time", "distance", "pollution", "combined")

getRoutingWayGeneric <- function(con,trg, src, goal){
  if (goal == "distance"){
    cost = "cityCostMulti * distanceMulti * length"
  }else  if (goal == "time"){ 
    cost = "cityCostMulti * timeMulti * maxspeed_forward * length"
  } else if (goal == "pollution"){
    cost = "cityCostMulti * pollutionMulti * length"
  } else if (goal == "combined"){
    cost = "cityCostMulti * distanceMulti * timeMulti * pollutionMulti * maxspeed_forward * length"
  } else {
    return(NULL)
  }
#     sql = paste("SELECT  st_astext(ST_LineMerge(st_union(st_simplify(the_geom,100)))) as geometry, 
#               st_length(ST_LineMerge(st_union(the_geom)),true)::int as length_m,  
#               sum(st_length(the_geom,true) / (maxspeed_forward /3.6)*0.75)::int as time_s,  
#               sum(route.cost) as total_cost,
#               count(osm_id)::int as distinct_osm_ids, 
#               json_agg(edge_id)::text as edges,
#               json_agg(cityCostMulti)::text as city_cost_multi 
#               FROM ways JOIN (SELECT seq, node AS node, edge AS edge_id, cost 
#               FROM pgr_dijkstra('SELECT gid AS id, source::integer, target::integer, 
#               (",cost,")::double precision AS cost FROM ways'
#               ,",src,",",trg,", false)   ) AS route ON ways.gid = route.edge_id", sep="")
sql = paste("SELECT  st_astext(ST_LineMerge(st_union(st_simplify(the_geom,100)))) as geometry, 
              st_length(ST_LineMerge(st_union(the_geom)),true)::int as length_m,  
              sum(st_length(the_geom,true) / (maxspeed_forward /3.6)*0.75)::int as time_s,  
              sum(route.cost) as total_cost,
              count(osm_id)::int as distinct_osm_ids, 
              json_agg(edge_id)::text as edges,
              json_agg(cityCostMulti)::text as city_cost_multi 
              FROM ways JOIN (SELECT seq, node AS node, edge AS edge_id, cost 
              FROM pgr_dijkstra('SELECT gid AS id, source::integer, target::integer, 
              (",cost,")::double precision AS cost FROM ways as w,
      (SELECT ST_Expand(ST_Extent(the_geom),0.1) as box  FROM ways as l1    WHERE l1.source =",src," OR l1.target = ",trg,") as box
              WHERE w.the_geom && box.box'
              ,",src,",",trg,", false)   ) AS route ON ways.gid = route.edge_id", sep="")
    if(debug){print(sql)}
    response = dbSendQuery(con,sql)
    way=dbFetch(response,n=-1)
    if(debugPlot){
      geo = readWKT(way$geometry[1])
      plot(geo, col=currentColor, lwd=2,lty=6, add=TRUE)
    }
    return (way);
}

#getRoutingWay <- function(con,trg, src){ #output may be interesting in the future
#  sql = paste("SELECT osm_id, length, maxspeed_forward,  ST_asText(the_geom) as the_geom, source, target, node, edge_id, (cityCostMulti*length) as fullcost , cityCostMulti FROM ways JOIN (SELECT seq, id1 AS node, id2 AS edge_id, cost FROM pgr_dijkstra('SELECT gid AS id, source::integer, target::integer, length::double precision AS cost FROM ways',",src,",",trg,", false, false)   ) AS route ON ways.gid = route.edge_id", sep="")
#  response = dbSendQuery(con,sql)
#  way=dbFetch(response,n=-1)
#  return (way);
#}


updateEdges <- function(con, updateRows){
  sql= "UPDATE ways as d SET gid=s.gid, citycostmulti=s.citycostmulti FROM (VALUES "
  edgeCount=nrow(updateRows)
  first=TRUE
  for(j in 1:edgeCount){  
    #sql <- paste(sql, paste("(",updateRows$edge_id[j],",", updateRows$citycostmulti[j]*(1+sqrt((1/abs((edgeCount/2)-j)))/10),")"),sep=ifelse(first,"",", ")) 
    sql <- paste(sql, paste("(",updateRows$edge_id[j],",", updateRows$citycostmulti[j]*1.1,")"),sep=ifelse(first,"",", ")) 
    #if (updateRows$edge_id[j]==6937) {print(paste (updateRows$citycostmulti[j], updateRows$citycostmulti[j]*(1+sqrt((1/abs((edgeCount/2)-j)))/10)))}
    first=FALSE;
  }
  sql <- paste(sql, ")AS s(gid, citycostmulti) WHERE s.gid=d.gid")
  update = dbSendQuery(con,sql)
  rs= dbGetInfo(update,n=-1)
  return (rs)
}

#internl plot function of the whole map
plotFullStreetView <- function (con, xmin, ymin, xmax, ymax){
  request = paste("select st_astext(the_geom) as the_geom, pollutionMulti as pollution  from ways where the_geom && ST_MakeEnvelope (",xmin," , ",ymin," , ",xmax,", ",ymax,", 4326)", sep="")  
  result = dbSendQuery(con,request)
  way= dbFetch(result,n=-1)
  for (i in 1:length(way$the_geom)){
    geo = readWKT(way$the_geom[i])
    plot(geo, col="lightgray", add=TRUE)
#    if (way$pollution[i]>1.0){
#      plot(geo, col="red", lty=2,  add=TRUE)
#    }
  }
}

#internal plot function of area affecting the goal
plotMultiplicatorView <- function (con, xmin, ymin, xmax, ymax, goal){
  #xmin=10.1580607288361
  #ymin=56.1477407419276
  #xmax=10.1165896654129
  #ymax=56.2257947825602
  if (goal == "distance"){
    multi = c("distanceMulti")
  }else  if (goal == "time"){ 
    multi = c("timeMulti")
  } else if (goal == "pollution"){
    multi = c("pollutionMulti")
  } else if (goal == "combined"){
    multi = c("timemulti","distancemulti","pollutionmulti")
  } else {
    return(multi)
  }
  print(goal)
  request = paste("select st_astext(the_geom) as the_geom, pollutionMulti as pollution  from ways where (the_geom && ST_MakeEnvelope (",xmin," , ",ymin," , ",xmax,", ",ymax,", 4326)) and ", paste (multi, " > 1.0", collapse=" and "), sep="")  
  result = dbSendQuery(con,request)
  way= dbFetch(result,n=-1)
  print(length(way))  
  if(length(way)>0){
    for (i in 1:length(way$the_geom)){
      geo = readWKT(way$the_geom[i])
      #if (way$pollution[i]>1.0){
        plot(geo, col="red", lty=2,  add=TRUE)
      #} else{
      #  print('WrongSelect > 1.0')
      #}
      }
  }
}

updateCircular <- function(con, x, y, goal, radius_m, multivalue){
  if (goal == "time"){ 
    multi = "timemulti"
  }else  if (goal == "distance"){
    multi = "distancemulti"
  } else if (goal == "pollution"){
    multi = "pollutionmulti"
  } else  {
    return(NULL)
  }
  if(debugPlot){
    #x=10.13733
    #y=56.18677
    #radius_m=400
    #pollution_multi=50.0
    #multi= XXX
    plotRequest = paste("SELECT st_astext(st_buffer(ST_GeogFromText('SRID=4326;POINT(",x," ",y,")'), ",radius_m,")::geometry) as the_geom")
    response = dbSendQuery(con,plotRequest)
    result = dbFetch(response,n=-1)
    geo = readWKT(result$the_geom[1])
    plot(geo, add=TRUE)
  }
  request = paste("UPDATE ways SET ",multi," = ",multivalue," where the_geom && st_buffer(ST_GeogFromText('SRID=4326;POINT(",x," ",y,")'), ",radius_m,")")
  update = dbSendQuery(con,request)
  rs= dbGetInfo(update,n=-1)
  return (rs)
}

updateOsmIdCost <- function(con, osm_id, cost_multi){
  request = paste("UPDATE ways SET cityCostMulti = ",cost_multi," where osm_id  = ", osm_id , sep="")
  update = dbSendQuery(con,request)
  rs= dbGetInfo(update,n=-1)
  return (rs)
}
resetAllOsmIdCost <- function(con){
  request = paste("UPDATE ways SET cityCostMulti = 1.0" , sep="")
  reset = dbSendQuery(con,request)
  rs= dbGetInfo(reset,n=-1)
  return (rs)
}

resetAllMultiplicators <- function(con, goal){
  if (goal == "time"){ 
    multi = c("timemulti")
  }else  if (goal == "distance"){
    multi = c("distancemulti")
  } else if (goal == "pollution"){
    multi = c("pollutionmulti")
  } else  if (goal == "combined"){
    multi = c("timemulti","distancemulti","pollutionmulti")
  } else {
    multi = c("timemulti","distancemulti","pollutionmulti")
  }
  request = paste("UPDATE ways SET ",paste (multi, " = 1.0", collapse=", "), sep="")
  #print(request)  
  #request = paste("UPDATE ways SET ",multi," = 1.0" , sep="")
  reset = dbSendQuery(con,request)
  rs= dbGetInfo(reset,n=-1)
  return (rs)
}


#internal

updateCostArea <- function(con, xmin, ymin, xmax, ymax, cost_multi){
  request = paste("UPDATE ways SET cityCostMulti = ",cost_multi," where the_geom && ST_MakeEnvelope (",xmin," , ",ymin," , ",xmax,", ",ymax,", 4326)", sep="")  
  update = dbSendQuery(con,request)
  rs= dbGetInfo(update,n=-1)
  return (rs)
}




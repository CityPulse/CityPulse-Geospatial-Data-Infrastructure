library("websockets")
library(RPostgreSQL)## if the package is not already installed, use install.packages('RPostgreSQL') and install.packages(DBI)  
library(rgeos)# if the package is not already installed, use install.packages('rgeos')
library(DBI)
library(jsonlite)
library(RColorBrewer)
source(file="RoutingLib_sweden.R")
debug=TRUE
separator <- ";"
dbport=5432
drv <- dbDriver("PostgreSQL")

if (exists('con')){
  dbDisconnect(con)
}
con <- dbConnect(PostgreSQL(), user= "XXX", password="XXX", dbname="cp_sweden_stockholm", port=dbport, host="localhost")
debugPlot <<- FALSE;
count = 0
if(exists("server")){
  websocket_close(server)
}
server = create_server(port = 7686L,is.binary = FALSE)

f = function(WS) {
  websocket_write(paste("Registred to CityPulse Routing Websocket (",WS,")! \nAsk for help with: help()"), WS)
}
set_callback("established", f, server)

g = function(DATA, WS, ...) {
  receivedString <<- rawToChar(DATA)
  print(receivedString)
  receivedArray=strsplit(receivedString,"\\(" )
  receivedCommand=receivedArray[[1]][1]
  receivedArguments= strsplit(strsplit(receivedArray[[1]][2],"\\)" )[[1]][1],",")[[1]]#trim
  receivedArguments
  length(receivedArguments)
  if(receivedCommand=="resetAllOsmIdCost"){
    result <- resetAllOsmIdCost(con)
    websocket_write(as.character(result$rowsAffected), WS)
  }else if(receivedCommand=="resetAllMultiplicators"){
    if(length(receivedArguments)==0){
      result <- resetAllMultiplicators(con)$rowsAffected
    } else if (length(receivedArguments)==1){
      result <- resetAllMultiplicators(con,receivedArguments[1])$rowsAffected
    } else {
      result <- paste("Used command", receivedCommand, "with wrong number of arguments! See help()")
    }
    websocket_write(as.character(result), WS)
  }else if(receivedCommand=="getNearestNodeId"){
    if(length(receivedArguments)==2){
      result <- getNearestNodeId(con, receivedArguments[1],receivedArguments[2])
    } else {
      result <- paste("Used command", receivedCommand, "with wrong number of arguments! See help()")
    }
    websocket_write(as.character(result), WS)
  }else if(receivedCommand=="getCityRoute"){
    if((length(receivedArguments)>=5) && (length(receivedArguments)<=6)){
      srcX<-receivedArguments[1]
      srcY<-receivedArguments[2]
      trgX<-receivedArguments[3]
      trgY<-receivedArguments[4] 
      goal <- receivedArguments[5] 
      if (length(receivedArguments)==6){
        numOfIterations <- receivedArguments[6]
      }else  if(length(receivedArguments)==5){
        numOfIterations <- 1
      } 
      #srcX<-'10.1580607288361'
      #srcY<-'56.1477407419276'
      #trgX<-'10.1165896654129'
      #trgY<-'56.2257947825602'
      #goal <- "pollution"      
      fromId <- getNearestNodeId(con,srcX,srcY)
      toId <- getNearestNodeId(con,trgX,trgY)      
      if(debugPlot){        
        #paint
        n_src_geo <- readWKT(paste("POINT(",srcX," ",srcY,")"))
        n_trg_geo <- readWKT(paste("POINT(",trgX," ",trgY,")"))
        srcX<-as.numeric(srcX)
        srcY<-as.numeric(srcY)
        trgX<-as.numeric(trgX)
        trgY<-as.numeric(trgY)
        xdelta<-(max(srcX+trgX)-min(srcX+trgX))/4
        ydelta<-(max(srcY+trgY)-min(srcY+trgY))/4
        plotxlim=(c(min(srcX,trgX)-xdelta,max(srcX,trgX)+xdelta))
        plotylim=(c(min(srcY,trgY)-ydelta,max(srcY,trgY)+ydelta))
        #print(plotxlim)
        #print(plotylim)
        #plot(n_src_geo,col='green', pch='S',xlim=c(10.1,10.2),ylim=c( 56.15, 56.25))
        xmin<-min(srcX,trgX)
        ymin<-max(srcX,trgX)
        xmax<-min(srcY,trgY)
        ymax<-max(srcY,trgY)
        #plot(n_src_geo,col='black', pch='S',xlim=c(min(srcX,trgX)-xdelta,max(srcX,trgX)+xdelta),ylim=c(min(srcY,trgY)-ydelta,max(srcY,trgY)+ydelta))
        #plot(n_trg_geo,col='black', add=TRUE,pch='E')
        #plotFullStreetView(con, xmin, ymin, xmax, ymax)
        #colors <- brewer.pal(10, "PuBuGn")#RdYlBu
        colors <- brewer.pal(n = 8, name = 'Dark2')  
        pal <- colorRampPalette(colors)
      }       
      for(i in 1:numOfIterations){
        if(debugPlot){        
          currentColor <<- pal(numOfIterations)[i]
          #print(currentColor)
        }
        way <- getRoutingWayGeneric(con,fromId, toId, goal)#getRoutingWayWeightedSimple(con,fromId,toId)
        res=way[c('geometry', 'length_m', 'time_s', 'total_cost')]
        result <- paste(colnames(res),collapse=separator)
        result <- paste(result,paste(res,collapse=separator),collapse="\n", sep="\n")
        edge_id <- fromJSON(toString(way['edges']))
        citycostmulti <- fromJSON(toString(way['city_cost_multi']))     
        if (numOfIterations>1){
          updateRows = data.frame(edge_id, citycostmulti)
          updateRows
          updateEdges(con,updateRows)
        }
        #plot(linesegment,col=pal(numOfIterations)[i],add=TRUE)
        websocket_write(result, WS)
      }  
      if(debugPlot){                
        plotMultiplicatorView(con, xmin, ymin, xmax, ymax, goal)
      }
      if (numOfIterations>1){
        resetAllOsmIdCost(con)
      }
      #plot(n_src_select_geo,col='green',add=TRUE,pch='S')
      #plot(n_trg_select_geo,col='red',add=TRUE,pch='E')     
    }else {websocket_write(paste("Used command", receivedCommand, "with wrong number of arguments! See help()"), WS)}
  }else if(receivedCommand=="updateOsmIdCost"){ #INTERNAL DEPRECATED
    if(length(receivedArguments)==2){
      updateCost<-updateOsmIdCost(con,receivedArguments[1],receivedArguments[2])
      websocket_write(as.character(updateCost$rowsAffected), WS)
    }else {websocket_write(paste("Used command", receivedCommand, "with wrong number of arguments! See help()"), WS)}
  }else if(receivedCommand=="updateCostArea"){ #INTERNAL DEPRECATED
    if(length(receivedArguments)==5){
      updateCost<-  updateCostArea(con,receivedArguments[1],receivedArguments[2],receivedArguments[3],receivedArguments[4],receivedArguments[5])
      websocket_write(as.character(updateCost$rowsAffected), WS)
    }else {websocket_write(paste("Used command", receivedCommand, "with wrong number of arguments! See help()"), WS)}
  }else if(receivedCommand=="updateCostCircular"){
    if(length(receivedArguments)==5){
      xLat <- receivedArguments[1]
      yLon <- receivedArguments[2]
      goal <- receivedArguments[3]
      radius_m <- receivedArguments[4]
      pollutionCost <- receivedArguments[5]
      updatedCost <-  updateCircular(con,xLat,yLon,goal, radius_m,pollutionCost)
      result <- updatedCost$rowsAffected
      websocket_write(as.character(result), WS)
    }else {websocket_write(paste("Used command", receivedCommand, "with wrong number of arguments! See help()"), WS)}
  }else if(receivedCommand=="help"){
    helpstring <- paste("help() - get this help\n",
                        " PLEASE HAVE A LOOK AT the README.txt in the Repository")
    websocket_write(helpstring, WS)
  }else{
    websocket_write(paste("We cant understand the command: ",receivedCommand,"Ask for help with: help()"), WS)
  }
  # websocket_write(paste("Shouldn't reach here, please send command to d.kuemper at hs-osnabrueck.de CurrentCount=",count, " We recieved:",receivedString), WS)
  count<<-count+1
}
setCallback("receive", g, server)

while(TRUE)
{
  service(server)
}


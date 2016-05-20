# GDI - Geospatial Data Infrastructure

The GDI component is used by a number of other CityPulse components to tackle geo-spatial tasks. For example, the Composite Monitoring uses the GDI to find nearby sensors or the Decision Support uses the GDI to get a set of routes across the city, which follow non-functional user requirements.


## Requirements and Dependencies
The GDI uses OpenStreetMap Data (OSM) and is implemented in R and Java. Therefore it depends on the following components:

- For Websocket Routing Interface: R >= V3.2.3 (required libraries: websockets, RPostgreSQL,rgeos,DBI,jsonlite)
- For Java GDI Implementation: Java 8
- PostgreSQL version >= V9.3
- OpenStreetMap data 
- osm2pgsql >= V0.82.0
- osm2pgrouting >= V2.1.0

## Installation
Before using the GDI, an PostgreSQL instance must be set up. The following lists the necessary steps:

- Install PostgreSQL (http://www.postgresql.org/)
- Install PostGIS extension for Postgresql (http://postgis.net/)
- Install pgRouting (http://pgrouting.org/)
- Download Openstreetmap(OSM) dataset from required  (e.g. from http://download.geofabrik.de/)
- Create and import database 
- At shell level:
	```
	user@server:~$: createdb cp_sweden
	user@server:~$: psql cp_sweden
	```
- At Database level:
	```
	cp_sweden=# CREATE EXTENSION postgis; 
	cp_sweden=# CREATE EXTENSION hstore; 
	cp_sweden=# CREATE EXTENSION pgrouting;
	cp_sweden=# \q 
	```
- Optional (at shell level) cut down area smaller area by bounding box:
	```
	osmosis   --read-xml sweden-latest.osm --tee 1 --bounding-box left=17.2410 top=60.2997 bottom=58.5328 right=20.0253 --write-xml stockholm.osm
	```
- At shell level  (example Import of OSM LAyer and Routing process for sweden data):
	```
	user@server:~$: osm2pgsql -C2000 -d cp_sweden -k -l --slim --flat-nodes flat-nodes.bin --number-processes 8  sweden-latest.osm.pbf
	user@server:~$: osm2pgrouting --file ./sweden-latest.osm --conf /usr/share/osm2pgrouting/mapconfig.xml --dbname cp_sweden -p5432  --user XXX --passwd XXX 
	```
- At shell level create some CityPulse specific tables and indizes (sql file in the repository)
	```
	user@server:~$: psql cp_sweden -f CityPulseGDI_Public/res/Initialise-CP-specials.sql
	```

## Running the component
After the installation of the GDI, 
	- the websocket routing interface can be started with:
		```
		user@server:~$: R -f websocketRoutingInterface.R
		```
		- Websocket usage parameters are defined in the README file.
	- The GDI Java implementation can be found at https://github.com/CityPulse/GDI/tree/master/CityPulseGDI_Public
		- It provides a full Javadoc Documentation
		- Example class for first experiments: eu.citypulse.uaso.gdiclient.CpClientExampleStockholm

## Link
The code of the Resource Management can be found here: https://github.com/CityPulse/GDI.git

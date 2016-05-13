# GDI - Geospatial Data Infrastructure

The GDI component is used by a number of other CityPulse components to tackle geo-spatial tasks. For example, the Composite Monitoring uses the GDI to find nearby sensors or the Decision Support uses the GDI to get a set of routes across the city, which follow non-functional user requirements.


## Requirements
?

## Dependencies
The GDI uses OpenStreetMap (OSM) and is implemented in R. Therefore it depends on the following components:

- R
- PostgreSQL version >= 9.3
- OpenStreetMap

## Installation
Before using the GDI, an OSM instance must be set up. The following lists the necessary steps:

- a
- b
- c

## Running the component
After the installation of the GDI, it can be started with the following command:

	R -f websocketRoutingInterface.R

## Link
The code of the Resource Management can be found here: https://github.com/CityPulse/GDI.git
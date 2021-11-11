#buildung the jar
matsim-vulkaneifel-1.0-SNAPSHOT.jar:
	mvn clean package
	echo "Done"

#create transit-schedule
scenario/open-vulkaneifel-scenario/input/vulkaneifel-pt-schedule.xml.gz: scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare transit-from-gtfs\
		./scenario/open-vulkaneifel-scenario/prepare/pt-gtfs.zip\
		./scenario/open-vulkaneifel-scenario/prepare/pt-regio-2021-10-31.zip\
        --network ./scenario/open-vulkaneifel-scenario/input/vulkaneifel-network.xml.gz\
        --name vulkaneifel\
        --date 2021-11-04\
        --target-crs EPSG:25832\
        --shp ./scenario/open-vulkaneifel-scenario/prepare/rheinland-pfalz.shp.zip\
        --output ./scenario/open-vulkaneifel-scenario/input

#create network from osm.pbf
scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz: matsim-vulkaneifel-1.0-SNAPSHOT.jar
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare network\
 	--output ./scenario/open-vulkaneifel-scenario/input/vulkaneifel-network.xml.gz\
 	--osmnetwork ./scenario/open-vulkaneifel-scenario/prepare/rheinland-pfalz-latest.osm.pbf\

#create population
scenario/open-vulkaneifel-scenario/input/vulkaneifel-plans.xml.gz:
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare trajectory-to-plans\
	--name vulkaneifel-plans\
	--sample-size 0.01\
	--attribbutes ./scenario/open-vulkaneifel-scenario/prepare/personAttributes.xml\
	--population ./scenario/open-vulkaneifel-scenario/prepare/population.xml.gz\
	--output ./scenario/open-vulkaneifel-scenario/input\
	--crs EPSG:25832

#clean population
scenario/open-vulkaneifel-scenario/input/vulkaneifel-plans.xml.gz:

prepare: scenario/open-vulkaneifel-scenario/input/vulkaneifel-pt-schedule.xml.gz
	echo "Done"
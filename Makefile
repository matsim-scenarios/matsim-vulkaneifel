matsim-vulkaneifel-1.0-SNAPSHOT.jar:
	mvn clean package
	echo "Done"

scenario/open-vulkaneifel-scenario/input/vulkaneifel-pt-schedule.xml.gz: scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare transit-from-gtfs
	INPUT ./scenario/open-vulkaneifel-scenario/prepare/pt-gtfs.zip\
        --network ./scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz\
        --name vulkaneifel\
        --date 2021-11-04\
        --target-crs EPSG:25832

scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz: matsim-vulkaneifel-1.0-SNAPSHOT.jar
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare network\
 	--output ./scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz\
 	--osmnetwork /scenario/open-vulkaneifel-scenario/prepare/rheinland-pfalz-latest.osm.pbf\

prepare: scenario/open-vulkaneifel-scenario/input/vulkaneifel-pt-schedule.xml.gz
	echo "Done"
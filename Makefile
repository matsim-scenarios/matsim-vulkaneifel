matsim-vulkaneifel-1.0-SNAPSHOT.jar:
	mvn clean package

scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz: matsim-vulkaneifel-1.0-SNAPSHOT.jar
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare network\
 	--output scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz\
 	--osmnetwork /mnt/c/Users/ACER/Desktop/Uni/Bachelorarbeit/MATSim/Erstellung-Vulkaneifel/rheinland-pfalz-latest.osm.pbf

prepare: scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz
	echo hello
WORKING_DIRECTORY := C:/Users/ACER/Desktop/Uni/Bachelorarbeit/MATSim/Erstellung-Vulkaneifel => ://
VERSION := matsim-vulkaneifel-v1.0
OUTPUT_FOLDER := C:/Users/ACER/Desktop/Uni/Bachelorarbeit/MATSim/input => ://
JAR := matsim-vulkaneifel-0.0.1-SNAPSHOT.jar

NETWORK := $(OUTPUT_FOLDER)/$(VERSION).network.xml.gz => ://
SCHEDULE := $(OUTPUT_FOLDER)/$(VERSION).transit-schedule.xml.gz

# build the application
$(JAR):
	java --version
	mvn package

# create network and pt
$(NETWORK):	$(JAR)
	java -Xmx5G -jar $(JAR)	prepare network --osmnetwork $(WORKING_DIRECTORY)/rheinland-pfalz-latest.osm.pbf

$(SCHEDULE): $(NETWORK)
	java -Xmx5G -jar $(JAR) prepare transit-from-gtfs --network $(NETWORK)
	--date "2021-10-19" --name $(VERSION) --shp $(WORKING_DIRECTORY)/dilutionArea.shp

network: $(NETWORK)
schedule: $(SCHEDULE)

prepare: network
	@echo "Done"
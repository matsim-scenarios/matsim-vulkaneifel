CRS := EPSG:25832\
OUTPUT_FOLDER := ./scenario/open-vulkaneifel-scenario/input\
SHP := ./scenario/open-vulkaneifel-scenario/prepare/shape/rlp.shp\

#buildung the jar
matsim-vulkaneifel-1.0-SNAPSHOT.jar:
	mvn clean package
	echo "Done"

#create network from osm.pbf
scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz: matsim-vulkaneifel-1.0-SNAPSHOT.jar
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare network\
 	--output ./scenario/open-vulkaneifel-scenario/input/vulkaneifel-network.xml.gz\
 	--osmnetwork ./scenario/open-vulkaneifel-scenario/prepare/rheinland-pfalz-latest.osm.pbf\

#create transit-schedule
scenario/open-vulkaneifel-scenario/input/vulkaneifel-pt-train-schedule.xml.gz: scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-network.xml.gz
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare transit-from-gtfs\
		./scenario/open-vulkaneifel-scenario/prepare/regio-s-train-gtfs-2021-11-14.zip\
        --network ./scenario/open-vulkaneifel-scenario/input/vulkaneifel-network.xml.gz\
        --name vulkaneifel\
        --date 2021-11-21 2021-11-22\
        --target-crs EPSG:25832\
        --shp ./scenario/open-vulkaneifel-scenario/prepare/shape/rlp.shp\
        --output ./scenario/open-vulkaneifel-scenario/input

scenario/open-vulkaneifel-scenario/input/vulkaneifel-pt-bus-schedule.xml.gz: scenario/open-vulkaneifel-scenario/input/vulkaneifel-pt-train-schedule.xml.gz
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare transit-from-gtfs\
		./scenario/open-vulkaneifel-scenario/prepare/regio-s-train-gtfs-2021-11-14.zip\
		./scenario/open-vulkaneifel-scenario/prepare/bus-tram-subway-gtfs-2021-11-14t.zip\
		--network ./scenario/open-vulkaneifel-scenario/input/vulkaneifel-network.xml.gz\
		--name vulkaneifel\
		--date 2021-11-21 2021-11-22\
		--target-crs EPSG:25832\
		--shp ./scenario/open-vulkaneifel-scenario/prepare/shape/rlp.shp\
		--output ./scenario/open-vulkaneifel-scenario/input

#population stuff
scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-plans.xml.gz: #scenario/open-vulkaneifel-scenario/input/vulkaneifel-pt-schedule.xml.gz
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare trajectory-to-plans	\
	--name tmp	--sample-size 0.25	\
	--attributes ./scenario/open-vulkaneifel-scenario/prepare/personAttributes.xml.gz	\
	--population ./scenario/open-vulkaneifel-scenario/prepare/population.xml.gz	\
	--output ./scenario/open-vulkaneifel-scenario/prepare/	\
	--target-crs EPSG:25832\

#population filter
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare filter-population	\
	./scenario/open-vulkaneifel-scenario/prepare/tmp-25pct.plans.xml.gz	\
    --shp ./scenario/open-vulkaneifel-scenario/prepare/shape/rlp.shp	\
    --input-crs EPSG:25832	\
    --target-crs EPSG:25832	\
    --output ./scenario/open-vulkaneifel-scenario/prepare/tmp-25pct.plans.xml.gz	\

#grid2coordinates
	java -Xmx10G -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare resolve-grid-coords	\
	./scenario/open-vulkaneifel-scenario/prepare/tmp-25pct.plans.xml.gz	\
	--grid-resolution 300	\
	--input-crs EPSG:25832	\
	--landuse ./scenario/open-vulkaneifel-scenario/prepare/landuse.shp	\
	--network ./scenario/open-vulkaneifel-scenario/input/vulkaneifel-network-with-pt.xml.gz	\
	--output ./scenario/open-vulkaneifel-scenario/input/vulkaneifel-25pct-plans.xml.gz\

#downsampling
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare downsample-population	\
	./scenario/open-vulkaneifel-scenario/input/vulkaneifel-25pct-plans.xml.gz	\
	--sample-size 0.25	\
	--samples 0.1 0.01\

prepare: scenario/open-vulkaneifel-scenario/prepare/vulkaneifel-plans.xml.gz
	echo "Done, Have fun with your Vulkaneifel-Scenario ;)"
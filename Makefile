
N := vulkaneifel
V := v1.1
CRS := EPSG:25832
DILUTION_AREA := input/dilutionArea/dilutionArea.shp
JAR := matsim-vulkaneifel-1.1-SNAPSHOT.jar

REGIONS := baden-wuerttemberg bayern brandenburg bremen hamburg hessen mecklenburg-vorpommern niedersachsen nordrhein-westfalen\
	rheinland-pfalz saarland sachsen sachsen-anhalt schleswig-holstein thueringen

SHP_FILES := $(patsubst %, input/shp/%-210101-free.shp.zip, $(REGIONS))

# Required files
input/network.osm.pbf:
	curl https://download.geofabrik.de/europe/germany-230101.osm.pbf -o $@\

input/dilutionArea/dilutionArea.shp:
	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/dilutionArea.shp -o $@\

	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/dilutionArea.shx -o input/dilutionArea/dilutionArea.shx\

	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/dilutionArea.prj -o input/dilutionArea/dilutionArea.prj\

input/temp/population.xml.gz:
	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/population.xml.gz -o $@\

	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/personAttributes.xml.gz -o input/temp/personAttributes.xml.gz\

input/temp/german_freight.25pct.plans.xml.gz:
	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/v1/german-wide-freight-25pct.xml.gz -o input/temp/german_freight.25pct.plans.xml.gz\

	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/v1/german-primary-road.network.xml.gz -o input/temp/germany-europe-network.xml.gz\

${SHP_FILES} :
	curl https://download.geofabrik.de/europe/germany/$(@:input/shp/%=%) -o $@\

input/landuse/landuse.shp: ${SHP_FILES}
	java -Djava.io.tmpdir=${TMPDIR} -Xmx20G -jar $(JAR) prepare create-landuse-shp\
		$^\
		--target-crs EPSG:25832\
		--output $@\

#create network from osm.pbf
input/$N-$V-network.xml.gz: input/network.osm.pbf input/dilutionArea/dilutionArea.shp
	java -Xmx48G -jar $(JAR) prepare network\
		--output $@\
		--osmnetwork input/network.osm.pbf\
		--veryDetailedArea input/dilutionArea/dilutionArea.shp\
		
#create transit schedule
input/$N-$V-transitSchedule.xml.gz: input/$N-$V-network.xml.gz
 #create big bus schedule as template for regional train
	java -Djava.io.tmpdir=${TMPDIR} -Xmx48G -jar $(JAR) prepare transit-from-gtfs\
			../gtfs/bus-tram-subway-gtfs-2021-11-14t.zip\
			--network input/$N-$V-network.xml.gz\
			--name $N-$V-template\
			--date "2021-11-24"\
			--target-crs EPSG:25832\
			--output input/temp\

#create train line
	java -jar $(JAR) prepare create-train-line	\
		--network input/temp/$N-$V-network-with-pt.xml.gz	\
		--schedule input/temp/$N-$V-template-transitSchedule.xml.gz	\
		--vehicles input/temp/$N-$V-template-transitVehicles.xml.gz	\
		--shp $(DILUTION_AREA)\
		--shp-crs EPSG:25832\
		--target-crs EPSG:25832\
		--name $N-$V\
		--output input/temp\

	java -Djava.io.tmpdir=${TMPDIR} -Xmx48G -jar $(JAR) prepare transit-from-gtfs\
    		../gtfs/regio-s-train-gtfs-2021-11-14.zip\
    		--network input/$N-$V-network.xml.gz\
    		--name $N-$V-train\
    		--date "2021-11-24"\
    		--target-crs EPSG:25832\
    		--output input/temp\

#create bus schedule for Rheinland-Pfalz only
	java -Djava.io.tmpdir=${TMPDIR} -Xmx48G -jar $(JAR) prepare transit-from-gtfs\
			../gtfs/bus-tram-subway-gtfs-2021-11-14t.zip\
			--network input/$N-$V-network.xml.gz\
			--name $N-$V-bus\
			--date "2021-11-24"\
			--target-crs EPSG:25832\
			--shp input/shp/rheinland-pfalz-210101-free.shp.zip\
			--output input/temp\

#remove sev line from small schedule
	java -jar $(JAR) prepare remove-bus-line\
		--schedule input/temp/$N-$V-bus-transitSchedule.xml.gz\
		--name $N-$V-bus\
		--output input/temp\
		--lineId SEV---1747\

#merge regional train line into complete schedule
	java -jar $(JAR) prepare merge-transit-schedules\
		input/temp/$N-$V-train-transitSchedule.xml.gz\
		input/temp/$N-$V-bus-without-SEV-transitSchedule.xml.gz\
		input/temp/$N-$V-transitSchedule-only-regional-train.xml.gz\
		--vehicles input/temp/$N-$V-train-transitVehicles.xml.gz\
		--vehicles input/temp/$N-$V-bus-transitVehicles.xml.gz\
		--vehicles input/temp/$N-$V-transitVehicles-only-regional-train.xml.gz\
		--network input/$N-$V-network.xml.gz\
		--name $N-$V\
		--output input\

input/freight-trips.xml.gz: input/$N-$V-network.xml.gz input/temp/german_freight.25pct.plans.xml.gz input/dilutionArea.shp
	java -jar $(JAR) prepare extract-freight-trips input/temp/german_freight.25pct.plans.xml.gz\
		 --network input/temp/germany-europe-network.xml.gz\
		 --input-crs EPSG:5677\
		 --target-crs $(CRS)\
		 --shp $(DILUTION_AREA)\
		 --output $@

input/$N-$V-25pct.plans.xml.gz: input/landuse/landuse.shp input/temp/population.xml.gz input/freight-trips.xml.gz  input/$N-$V-transitSchedule.xml.gz
	java -jar $(JAR) prepare trajectory-to-plans\
    	--name $N-$V	--sample-size 0.25\
    	--attributes input/temp/personAttributes.xml.gz\
    	--population input/temp/population.xml.gz\
    	--output input/\
    	--target-crs $(CRS)\

	java -Xmx10G -jar $(JAR) prepare resolve-grid-coords\
    	$@\
    	--grid-resolution 300\
    	--input-crs $(CRS)\
    	--landuse input/landuse/landuse.shp\
    	--output $@\

	java -jar $(JAR) prepare generate-short-distance-trips\
		--population $@\
		--input-crs $(CRS)\
		--shp $(DILUTION_AREA)\
		--shp-crs $(CRS)\
		--num-trips 6000\
		--range 1000\
		--output $@\

	java -jar $(JAR) prepare adjust-activity-to-link-distances\
		$@\
		--shp $(DILUTION_AREA)\
		--scale 1.15\
		--input-crs $(CRS)\
		--network input/$N-$V-network.xml.gz\
		--output $@\

	 java -jar $(JAR) prepare fix-subtour-modes\
 		--coord-dist 100\
 		--input $@\
 		--output $@\

	java -jar $(JAR) prepare population\
		$@\
		--output $@\

	java -jar $(JAR) prepare extract-home-coordinates $@\
		--csv input/$N-$V-homes.csv

	java -jar $(JAR) prepare merge-populations\
		$@\
		input/freight-trips.xml.gz\
		 --output $@\

	java -jar $(JAR) prepare downsample-population $@\
	 	--sample-size 0.25\
        --samples 0.01\

prepare: input/$N-$V-25pct.plans.xml.gz
SHP := NRW-Sued-Rlp-Saar.shp
S := vulkaneifel
V := v1.1
CRS := EPSG:25832
DILUTION_AREA := https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/dilutionArea.shp
JAR := matsim-vulkaneifel-1.1-SNAPSHOT.jar

REGIONS := baden-wuerttemberg bayern brandenburg bremen hamburg hessen mecklenburg-vorpommern niedersachsen nordrhein-westfalen\
	rheinland-pfalz saarland sachsen sachsen-anhalt schleswig-holstein thueringen

SHP_FILES := $(patsubst %, input/shp/%-210101-free.shp.zip, $(REGIONS))

# Required files
input/network.osm.pbf:
	curl https://download.geofabrik.de/europe/germany-230101.osm.pbf -o input/network.osm.pbf

${SHP_FILES} :
	curl https://download.geofabrik.de/europe/germany/$(@:input/shp/%=%) -o $@\

input/landuse/landuse.shp: ${SHP_FILES}
	mkdir -p input/landuse\
	java -Xmx20G -jar $(JAR) prepare create-landuse-shp\
		$^\
		--target-crs EPSG:25832\
		--output $@\

#create network from osm.pbf
input/$(S)-$(V)-network.xml.gz: input/network.osm.pbf
	java -Xmx48G -jar $(JAR) prepare network\
		--output $@\
		--osmnetwork input/network.osm.pbf\
		--veryDetailedArea input/dilutionArea/dilutionArea.shp\
		
#create transit schedule
input/$(S)-$(V)-transitSchedule.xml.gz: input/temp/$(S)-$(V)-network.xml.gz
	java -Djava.io.tmpdir=${TMPDIR} -Xmx48G -jar $(JAR) prepare transit-from-gtfs\
    		../gtfs/regio-s-train-gtfs-2021-11-14.zip\
    		../gtfs/bus-tram-subway-gtfs-2021-11-14t.zip\
    		--network input/temp/$(S)-$(V)-network.xml.gz\
    		--name $(S)-$(V)\
    		--date "2021-11-24"\
    		--target-crs EPSG:25832\
    		--shp input/shp/$(SHP)\
    		--output input/temp\

#create train line
	java -jar $(JAR) prepare create-train-line	\
		--network input/temp/$(S)-$(V)-network-with-pt.xml.gz	\
		--schedule input/temp/$(S)-$(V)-transitSchedule.xml.gz	\
		--vehicles input/temp/$(S)-$(V)-transitVehicles.xml.gz	\
		--shp input/dilutionArea/dilutionArea.shp\
		--shp-crs EPSG:25832\
        --target-crs EPSG:25832\
		--name $(S)-$(V)\
		--output input/temp\

#remove sev line from small schedule
	java -jar $(JAR) prepare remove-bus-line\
		--schedule input/temp/$(S)-$(V)-transitSchedule.xml.gz\
		--name $(S)-$(V)\
		--output input/temp\
		--lineId SEV---1747\

#merge regional train line into complete schedule
	java -jar $(JAR) prepare merge-transit-schedules\
		input/temp/$(S)-$(V)-transitSchedule.xml.gz\
		input/temp/$(S)-$(V)-transitSchedule-only-regional-train.xml.gz\
		--vehicles input/temp/$(S)-$(V)-transitVehicles.xml.gz\
		--vehicles input/temp/$(S)-$(V)-transitVehicles-only-regional-train.xml.gz\
		--network input/temp/$(S)-$(V)-network.xml.gz\
		--name $(S)-$(V)\
		--output input\

input/freight-trips.xml.gz: input/$(S)-$(V)-network.xml.gz
	java -jar $(JAR) prepare extract-freight-trips ../shared-svn/projects/german-wide-freight/v1.2/german-wide-freight-25pct.xml.gz\
		 --network ../shared-svn/projects/german-wide-freight/original-data/german-primary-road.network.xml.gz\
		 --input-crs EPSG:5677\
		 --target-crs $(CRS)\
		 --shp $(DILUTION_AREA)\
		 --output $@

input/$(S)-$(V)-25pct-plans.xml.gz: input/landuse/landuse.shp input/$(S)-$(V)-transitSchedule.xml.gz
	java -jar $(JAR) prepare trajectory-to-plans\
    	--name $(S)-$(V)	--sample-size 0.25\
    	--attributes https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/personAttributes.xml.gz\
    	--population https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/population.xml.gz\
    	--output input/temp\
    	--target-crs EPSG:25832\

#grid2coordinates
	java -Xmx10G -jar $(JAR) prepare resolve-grid-coords\
    	$@\
    	--grid-resolution 300\
    	--input-crs EPSG:25832\
    	--landuse landuse/landuse.shp\
    	--network C:/Users/ACER/IdeaProjects/matsim-vulkaneifel/scenario/open-vulkaneifel-scenario/vulkaneifel-v1.0-25pct/vulkaneifel-v1.0-network-with-pt.xml.gz\
    	--output $@\

#clean population
	java -jar matsim-vulkaneifel-1.0-SNAPSHOT.jar prepare clean-population	\
    	--plans $@\
    	--remove-routes\
    	--remove-unselected-plans\
    	--remove-activity-location\
    	--trips-to-legs\
    	--output $@\

	java -jar $(JAR) prepare generate-short-distance-trips\
 	 --population $@\
 	 --input-crs $(CRS)\
 	 --shp $(DILUTION_AREA)\
 	 --shp-crs EPSG:25832\
 	 --num-trips 67395

	java -jar $(JAR) prepare adjust-activity-to-link-distances\
	$@\
	 --shp $(DILUTION_AREA)\
	 --scale 1.15\
	 --input-crs $(CRS)\
	 --network input/$(S)-$(V)-network.xml.gz\
	 --output $@\

	java -jar $(JAR) prepare merge-populations\
	$@\
	input/freight-trips.xml.gz\
	input/file.xml\
     --output $@\

prepare: input/$(S)-$(V)-25pct-plans.xml.gz
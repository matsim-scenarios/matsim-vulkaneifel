N := vulkaneifel
V := v1.2
CRS := EPSG:25832
JAR := matsim-vulkaneifel-*.jar

svn := ../public-svn/matsim/scenarios/countries/de/vulkaneifel
germany := ../shared-svn/projects/matsim-germany
DILUTION_AREA := $(svn)/v1.0/input/snz-data/20210521_vulkaneifel/dilutionArea.shp


# Required files
input/network.osm.pbf:
	curl https://download.geofabrik.de/europe/germany-230101.osm.pbf -o $@\

input/shp/VG5000_GEM.shp.zip:
	curl https://daten.gdz.bkg.bund.de/produkte/vg/vg5000_0101/aktuell/vg5000_01-01.utm32s.shape.ebenen.zip -o input/shp/VG5000_GEM.shp.zip\

	unzip input/shp/VG5000_GEM.shp.zip -d input/shp/

input/temp/population.xml.gz:
	mkdir -p input/temp
	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/population.xml.gz -o $@\

	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/openVulkaneifel/input/snz-data/20210521_vulkaneifel/personAttributes.xml.gz -o input/temp/personAttributes.xml.gz\

input/temp/german_freight.25pct.plans.xml.gz:
	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz -o input/temp/german_freight.25pct.plans.xml.gz\

	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz -o input/temp/germany-europe-network.xml.gz\


#create network from osm.pbf
input/$N-$V-network.xml.gz: input/network.osm.pbf
	java -Xmx48G -jar $(JAR) prepare network\
		--output $@\
		--osmnetwork input/network.osm.pbf\
		--veryDetailedArea $(DILUTION_AREA)\
		--buffer 20000\
		
#create transit schedule
input/$N-$V-transitSchedule.xml.gz: input/$N-$V-network.xml.gz input/shp/VG5000_GEM.shp.zip
	java -Djava.io.tmpdir=${TMPDIR} -Xmx48G -jar $(JAR) prepare transit-from-gtfs\
			$(germany)/gtfs/bus-tram-subway-gtfs-2021-11-14t.zip\
			$(germany)/gtfs/regio-s-train-gtfs-2021-11-14.zip\
			--prefix bus_,short_\
			--shp input/bus-area/bus-area.shp \
			--shp input/shp/vg5000_01-01.utm32s.shape.ebenen/vg5000_ebenen_0101/VG5000_GEM.shp \
			--network input/$N-$V-network.xml.gz\
			--name $N-$V\
			--date "2021-11-24"\
			--target-crs EPSG:25832\
			--output input/temp\

#create train line
	java -jar $(JAR) prepare create-train-line	\
		--network input/temp/$N-$V-network-with-pt.xml.gz	\
		--schedule input/temp/$N-$V-transitSchedule.xml.gz	\
		--vehicles input/temp/$N-$V-transitVehicles.xml.gz	\
		--shp $(DILUTION_AREA)\
		--shp-crs EPSG:25832\
		--target-crs EPSG:25832\
		--sev-id bus_SEV---1747\
		--name $N-$V\
		--output input/temp\

#remove sev line from small schedule
	java -jar $(JAR) prepare remove-bus-line\
		--schedule input/temp/$N-$V-transitSchedule.xml.gz\
		--name $N-$V\
		--output input/temp\
		--lineId bus_SEV---1747\

#merge regional train line into complete schedule
	java -jar $(JAR) prepare merge-transit-schedules\
		input/temp/$N-$V-without-SEV-transitSchedule.xml.gz\
		input/temp/$N-$V-transitSchedule-only-regional-train.xml.gz\
		--vehicles input/temp/$N-$V-transitVehicles.xml.gz\
		--vehicles input/temp/$N-$V-transitVehicles-only-regional-train.xml.gz\
		--network input/$N-$V-network.xml.gz\
		--name $N-$V\
		--output input\

input/freight-trips.xml.gz: input/$N-$V-network.xml.gz input/temp/german_freight.25pct.plans.xml.gz
	java -jar $(JAR) prepare extract-freight-trips input/temp/german_freight.25pct.plans.xml.gz\
		 --network input/temp/germany-europe-network.xml.gz\
		 --input-crs $(CRS)\
		 --target-crs $(CRS)\
		 --shp $(DILUTION_AREA)\
		 --shp-crs EPSG:25832\
		 --output $@

input/plans-completeSmallScaleCommercialTraffic.xml.gz:
	java -jar $(JAR) prepare generate-small-scale-commercial-traffic\
	  input/commercialTraffic/config_demand.xml\
	 --sample 0.25\
	 --jspritIterations 10\
	 --creationOption createNewCarrierFile\
	 --landuseConfiguration useOSMBuildingsAndLanduse\
	 --smallScaleCommercialTrafficType completeSmallScaleCommercialTraffic\
	 --zoneShapeFileName $(svn)/v1.2/input/shp/buildings_vulkaneifel_25832.shp\
	 --buildingsShapeFileName $(svn)/v1.2/input/shp/leipzig_buildings_25832.shp\
	 --landuseShapeFileName $(svn)/v1.2/input/shp/landuse_vulkaneifel_25832.shp\
	 --shapeCRS "EPSG:25832"\
	 --resistanceFactor "0.005"\
	 --nameOutputPopulation $(notdir $@)\
	 --pathOutput output/commercialTraffic

	mv output/commercialTraffic/$(notdir $@) $@

input/$N-$V-25pct.plans-initial.xml.gz: input/temp/population.xml.gz input/freight-trips.xml.gz input/plans-completeSmallScaleCommercialTraffic.xml.gz input/$N-$V-transitSchedule.xml.gz
	java -jar $(JAR) prepare trajectory-to-plans\
    	--name $N-$V	--sample-size 0.25\
		--max-typical-duration 0\
    	--attributes input/temp/personAttributes.xml.gz\
    	--population input/temp/population.xml.gz\
    	--output input/\
    	--target-crs $(CRS)\

	java -Xmx10G -jar $(JAR) prepare resolve-grid-coords\
    	$@\
    	--grid-resolution 300\
    	--input-crs $(CRS)\
    	--landuse $(germany)/landuse/landuse.shp\
    	--output $@\

	java -jar $(JAR) prepare generate-short-distance-trips\
		--population $@\
		--input-crs $(CRS)\
		--shp $(DILUTION_AREA)\
		--shp-crs $(CRS)\
		--num-trips 6000\
		--range 1000\
		--output $@\

	java -jar $(JAR) prepare split-activity-types-duration\
		--input $@ --output $@

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
		input/plans-completeSmallScaleCommercialTraffic.xml.gz\
		 --output $@\

	java -jar $(JAR) prepare downsample-population $@\
	 	--sample-size 0.25\
        --samples 0.01\
        --samples 0.001\

prepare: input/$N-$V-25pct.plans-initial.xml.gz

pt: input/$N-$V-transitSchedule.xml.gz
N := vulkaneifel
V := v1.2
CRS := EPSG:25832
JAR := matsim-vulkaneifel-*.jar

svn := ../public-svn/matsim/scenarios/countries/de/vulkaneifel
germany := ../shared-svn/projects/matsim-germany

MEMORY ?= 20G
DILUTION_AREA := $(svn)/v1.0/input/snz-data/20210521_vulkaneifel/dilutionArea.shp
NETWORK := $(germany)/maps/germany-230101.osm.pbf

# Scenario creation tool
sc := java -Xmx$(MEMORY) -XX:+UseParallelGC -jar $(JAR)

input/shp/VG5000_GEM.shp.zip:
	mkdir -p input/shp
	curl https://daten.gdz.bkg.bund.de/produkte/vg/vg5000_0101/aktuell/vg5000_01-01.utm32s.shape.ebenen.zip -o input/shp/VG5000_GEM.shp.zip\

	unzip input/shp/VG5000_GEM.shp.zip -d input/shp/

input/temp/german_freight.25pct.plans.xml.gz:
	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz -o input/temp/german_freight.25pct.plans.xml.gz\

	curl https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz -o input/temp/germany-europe-network.xml.gz\


#create network from osm.pbf
input/$V/$N-$V-network.xml.gz:
	$(sc) prepare network\
		--output $@\
		--osmnetwork $(NETWORK)\
		--veryDetailedArea $(DILUTION_AREA)\
		--buffer 20000\
		
#create transit schedule
input/$V/$N-$V-transitSchedule.xml.gz: input/$V/$N-$V-network.xml.gz input/shp/VG5000_GEM.shp.zip
	mkdir -p input/temp

	java -Djava.io.tmpdir=${TMPDIR} -Xmx48G -jar $(JAR) prepare transit-from-gtfs\
			$(germany)/gtfs/bus-tram-subway-gtfs-2021-11-14t.zip\
			$(germany)/gtfs/regio-s-train-gtfs-2021-11-14.zip\
			--prefix bus_,short_\
			--shp input/bus-area/bus-area.shp \
			--shp input/shp/vg5000_01-01.utm32s.shape.ebenen/vg5000_ebenen_0101/VG5000_GEM.shp \
			--network $<\
			--name $N-$V\
			--date "2021-11-24"\
			--target-crs EPSG:25832\
			--output input/temp\

#create train line
	$(sc) prepare create-train-line	\
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
	$(sc) prepare remove-bus-line\
		--schedule input/temp/$N-$V-transitSchedule.xml.gz\
		--name $N-$V\
		--output input/temp\
		--lineId bus_SEV---1747\

#merge regional train line into complete schedule
	$(sc) prepare merge-transit-schedules\
		input/temp/$N-$V-without-SEV-transitSchedule.xml.gz\
		input/temp/$N-$V-transitSchedule-only-regional-train.xml.gz\
		--vehicles input/temp/$N-$V-transitVehicles.xml.gz\
		--vehicles input/temp/$N-$V-transitVehicles-only-regional-train.xml.gz\
		--network $<\
		--name $N-$V\
		--output input/$V\

input/plans-longHaulFreight.xml.gz: input/$V/$N-$V-network.xml.gz input/temp/german_freight.25pct.plans.xml.gz
	$(sc) prepare extract-freight-trips input/temp/german_freight.25pct.plans.xml.gz\
		 --network input/temp/germany-europe-network.xml.gz\
		 --input-crs $(CRS)\
		 --target-crs $(CRS)\
		 --shp $(DILUTION_AREA)\
		 --shp-crs EPSG:25832\
		 --output $@

input/plans-completeSmallScaleCommercialTraffic.xml.gz:
	$(sc) prepare generate-small-scale-commercial-traffic\
	  input/commercialTraffic/config_demand.xml\
	 --sample 0.25\
	 --jspritIterations 10\
	 --creationOption createNewCarrierFile\
	 --landuseConfiguration useOSMBuildingsAndLanduse\
	 --smallScaleCommercialTrafficType completeSmallScaleCommercialTraffic\
	 --zoneShapeFileName $(svn)/v1.2/input/shp/zones_vulkaneifel_commercialTraffic_25832.shp\
	 --buildingsShapeFileName $(svn)/v1.2/input/shp/buildings_vulkaneifel_25832.shp\
	 --landuseShapeFileName $(svn)/v1.2/input/shp/landuse_vulkaneifel_25832.shp\
	 --shapeCRS "EPSG:25832"\
	 --resistanceFactor "0.005"\
	 --nameOutputPopulation $(notdir $@)\
	 --pathOutput output/commercialTraffic

	mv output/commercialTraffic/$(notdir $@) $@

input/$V/$N-$V-25pct.plans-initial.xml.gz: input/plans-longHaulFreight.xml.gz input/plans-completeSmallScaleCommercialTraffic.xml.gz input/$V/$N-$V-transitSchedule.xml.gz
	$(sc) prepare trajectory-to-plans\
    	--name population --sample-size 0.25\
		--max-typical-duration 0\
    	--attributes $(svn)/v1.0/input/snz-data/20210521_vulkaneifel/personAttributes.xml.gz\
    	--population $(svn)/v1.0/input/snz-data/20210521_vulkaneifel/population.xml.gz\
    	--output input/temp\
    	--target-crs $(CRS)\

	$(sc) prepare resolve-grid-coords\
    	input/temp/population-25pct.plans.xml.gz\
    	--grid-resolution 300\
    	--input-crs $(CRS)\
    	--landuse $(germany)/landuse/landuse.shp\
    	--output input/temp/population-25pct.plans.xml.gz\

	$(sc) prepare generate-short-distance-trips\
		--population input/temp/population-25pct.plans.xml.gz\
		--input-crs $(CRS)\
		--shp $(DILUTION_AREA)\
		--shp-crs $(CRS)\
		--num-trips 6000\
		--range 1000\
		--output input/temp/population-25pct.plans.xml.gz\

	$(sc) prepare adjust-activity-to-link-distances\
		input/temp/population-25pct.plans.xml.gz\
		--shp $(DILUTION_AREA)\
		--scale 1.15\
		--input-crs $(CRS)\
		--shp-crs $(CRS)\
		--network input/$V/$N-$V-network.xml.gz\
		--output input/temp/population-25pct.plans.xml.gz\

	$(sc) prepare split-activity-types-duration\
		--input input/temp/population-25pct.plans.xml.gz --output input/temp/population-25pct.plans.xml.gz

	$(sc) prepare fix-subtour-modes\
 		--coord-dist 100\
 		--input input/temp/population-25pct.plans.xml.gz\
 		--output input/temp/population-25pct.plans.xml.gz\

	$(sc) prepare population\
		input/temp/population-25pct.plans.xml.gz --output input/temp/population-25pct.plans.xml.gz\

	$(sc) prepare merge-populations\
		input/temp/population-25pct.plans.xml.gz\
		input/plans-longHaulFreight.xml.gz\
		input/plans-completeSmallScaleCommercialTraffic.xml.gz\
		 --output $@\

	$(sc) prepare downsample-population $@\
	 	--sample-size 0.25\
        --samples 0.01\
        --samples 0.001\

prepare: input/$V/$N-$V-25pct.plans-initial.xml.gz input/$V/$N-$V-transitSchedule.xml.gz
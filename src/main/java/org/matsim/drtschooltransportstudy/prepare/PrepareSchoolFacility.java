package org.matsim.drtschooltransportstudy.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.*;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Creates school facilities from OSM landuse shape.
 * */
public final class PrepareSchoolFacility {

	static final String SCHOOL_NAME = "school name";
	private static Logger log = LogManager.getLogger(PrepareSchoolFacility.class);

	public static void main(String[] args) throws IOException {
		// Attention: There are 3 missing schools from the Shape Files. Some manual modification of the facility is required.
		Network network = NetworkUtils.readNetwork("/Users/luchengqi/Documents/MATSimScenarios/Vulkaneifel/drt-prebooking-study/network.xml.gz");
		Path schoolShp = Path.of("/Users/luchengqi/Documents/MATSimScenarios/Vulkaneifel/drt-prebooking-study/shp/landuse/vulkaneifel-amenity-school.shp");
		String serviceAreaShpPath = "/Users/luchengqi/Documents/MATSimScenarios/Vulkaneifel/drt-prebooking-study/shp/ServiceArea/vulkaneifel.shp";

		ShapefileDataStore ds = ShpOptions.openDataStore(schoolShp);
		ds.setCharset(StandardCharsets.UTF_8);
		List<SimpleFeature> schoolFeatures = ShapeFileReader.getSimpleFeatures(ds);

		List<SimpleFeature> serviceAreaFeatures = (List<SimpleFeature>) ShapeFileReader.getAllFeatures(serviceAreaShpPath);
		assert serviceAreaFeatures.size() == 1;
		Geometry serviceAreaGeometry = (Geometry) serviceAreaFeatures.get(0).getDefaultGeometry();

		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		for (SimpleFeature feature : schoolFeatures) {
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			Coord coord = MGC.point2Coord(geometry.getCentroid());
			if (!geometry.getCentroid().within(serviceAreaGeometry)) {
				continue;
			}

			// Create facility
			ActivityFacilitiesFactory facilitiesFactory = new ActivityFacilitiesFactoryImpl();

			String facilityIdString = feature.getAttribute("osm_way_id").toString();
			Link link = NetworkUtils.getNearestLink(network, coord);
			ActivityFacility facility = facilitiesFactory.createActivityFacility(Id.create(facilityIdString, ActivityFacility.class), coord, link.getId());
			String schoolName = feature.getAttribute("name").toString();
			if (schoolName.equals("")) {
				schoolName = "unknown";
			}
			// There is a typo in the OSM data
			schoolName = schoolName.replace("Grunschule", "Grundschule");
			facility.getAttributes().putAttribute(SCHOOL_NAME, schoolName);

			facilities.addActivityFacility(facility);
			log.info("School name =  {} Mapped linkId = {}", feature.getAttribute("name").toString(), link.getId());
		}

		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(facilities);
		facilitiesWriter.write("/Users/luchengqi/Documents/MATSimScenarios/Vulkaneifel/drt-prebooking-study/schools.facility.xml");
	}

	private PrepareSchoolFacility(){}

}

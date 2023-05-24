package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import picocli.CommandLine;

import java.util.Map;
import java.util.Set;

@CommandLine.Command(
		name="network",
		description = "Create matsim network from osm data",
		showDefaultValues = true
)

public class CreateNetwork implements MATSimAppCommand {

	@CommandLine.Option(names = "--osmnetwork", description = "path to osm data files", required = true)
	private String osmnetwork;

	@CommandLine.Option(names = "--output", description = "path to output file", required = true)
	private String output;

	@CommandLine.Option(names = "--veryDetailedArea", description = "path to shape that covers very detailed network", required = true)
	private String veryDetailedArea;

	@CommandLine.Option(names = "--buffer", description = "Buffer for semidetailed area in meter", defaultValue = "10000")
	private double buffer;

	private static final Logger log = LogManager.getLogger(CreateNetwork.class);
	private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:25832");

	private static final Set<String> carOnlyModes = Set.of(TransportMode.car, TransportMode.ride, "freight");
	private static final Set<String> notCarOnlyModes = Set.of(TransportMode.car, TransportMode.ride, "freight", TransportMode.bike);

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateNetwork()).execute(args));
	}

	@Override
	public Integer call() {

		Geometry veryDetailedAreaBox = getVeryDetailedAreaBox(veryDetailedArea);
		Geometry semiDetailedAreaBox = getSemiDetailedAreaBox(veryDetailedAreaBox);

		log.info("Start to parse network. This might not output anything for a while");
		var network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(transformation)
				.setIncludeLinkAtCoordWithHierarchy((coord, level) -> {
					if (level <= LinkProperties.LEVEL_PRIMARY) return true;
					if (level <= LinkProperties.LEVEL_SECONDARY) return semiDetailedAreaBox
							.covers(MGC.coord2Point(coord));
					return veryDetailedAreaBox
							.covers(MGC.coord2Point(coord));
				})
				.setAfterLinkCreated((link, tags, direction) -> setAllowedMode(link, tags))
				.build()
				.read(osmnetwork);

		log.info("Finished parsing network. Start Network simplifier.");
		new NetworkSimplifier().run(network);

		log.info("Finished simplifying network. Start Network cleaner.");
		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		cleaner.run(Set.of(TransportMode.car));
		cleaner.run(Set.of(TransportMode.bike));
		cleaner.run(Set.of(TransportMode.ride));
		cleaner.removeNodesWithoutLinks();

		log.info("Finished cleaning network. Write network");
		new NetworkWriter(network).write(this.output);

		log.info("Finished CreateNetwork. Exiting.");
		return 0;
	}

	private static Geometry getGeometries(String path){

		//dilutionArea needs to be a single shape file
		log.info("reading in very detailed area file from " + path);

		if (path == null) {

			log.info("Path to geometry has not been initialized. Returning null...");
			return null;
		}

		return ShapeFileReader.getAllFeatures(path).stream()
				.map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
				.findFirst()
				.get();
	}

	private Geometry getSemiDetailedAreaBox(Geometry veryDetailedAreaBox){

		return veryDetailedAreaBox.buffer(buffer);
	}

	private Geometry getVeryDetailedAreaBox(String pathToVeryDetailedArea) {
		Geometry dilutionArea = getGeometries(pathToVeryDetailedArea);
		return dilutionArea != null ? dilutionArea.getEnvelope() : null;
	}

	private void setAllowedMode(Link link, Map<String, String> tags) {
		if (isCarOnly(tags)) {
			link.setAllowedModes(carOnlyModes);
		} else {
			link.setAllowedModes(notCarOnlyModes);
		}
	}

	private boolean isCarOnly(Map<String, String> tags) {
		var highwayType = tags.get(OsmTags.HIGHWAY);
		return highwayType == null || highwayType.equals(OsmTags.MOTORWAY) || highwayType.equals(OsmTags.MOTORWAY_LINK) || highwayType.equals(OsmTags.TRUNK) || highwayType.equals(OsmTags.TRUNK_LINK);
	}
}

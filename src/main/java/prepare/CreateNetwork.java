package prepare;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.Geometries;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CommandLine.Command(
        name="network",
        description = "Create matsim network from osm data",
        showDefaultValues = true
)

public class CreateNetwork implements MATSimAppCommand {

    @CommandLine.Option(names = "--osmnetwork", description = "path to osm data files", required = true)
    private String osmnetwork;

    @CommandLine.Option(names = "--output", description = "path to osm data files", required = true)
    private String output;

    @CommandLine.Option(names = "--detailedArea", description = "path to shape that covers detailed network")
    private String detailedArea;

    @CommandLine.Option(names = "--veryDetailedArea", description = "path to shape that covers very detailed network", required = true)
    private String veryDetailedArea;

    private static final Logger log = LogManager.getLogger(CreateNetwork.class);
    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:25832");
    private static final Set<String> carRideBike = Set.of(TransportMode.car, TransportMode.ride, TransportMode.bike);
    private static final Set<String> carRide = Set.of(TransportMode.car, TransportMode.ride);

    public static void main(String[] args) {
        System.exit(new CommandLine(new CreateNetwork()).execute(args));
    }

    @Override
    public Integer call() {

        //load veryDetailedArea geometry
        log.info("reading in very detailed area file");
        var veryDetailedAreaGeometries = getGeometries(veryDetailedArea);

        log.info("done reading shape file");

        List<Geometry> detailedAreaGeometries = getGeometries(detailedArea);


        log.info("Start to parse network. This might not output anything for a while");
        var network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(transformation)
                .setIncludeLinkAtCoordWithHierarchy((coord, level) -> {
                    if(level == LinkProperties.LEVEL_MOTORWAY) return true;

                    if (isInDetailedArea(detailedAreaGeometries, coord, level)) return true;
                    return veryDetailedAreaGeometries.stream()
                            .anyMatch(geometry -> geometry.covers(MGC.coord2Point(coord)));
                })
                .setAfterLinkCreated((link, tags, direction) -> setAllowedMode(link, tags))
                .build()
                .read(osmnetwork);
/*
        Network output;

        if(coarsenetwork != null){

            var coarseLinkProperties = LinkProperties.createLinkProperties().entrySet().stream()
                    .filter(entry -> entry.getValue().getHierarchyLevel() <= LinkProperties.LEVEL_PRIMARY)
                    .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

            log.info("reading in coarse network");
            var germanyNetwork = new SupersonicOsmNetworkReader.Builder()
                    .setCoordinateTransformation(transformation)
                    .setLinkProperties(coarseLinkProperties)
                    .setIncludeLinkAtCoordWithHierarchy(((coord, level) -> level == LinkProperties.LEVEL_PRIMARY))
                    .setAfterLinkCreated((link, tags, direction) -> setAllowedMode(link, tags))
                    .build()
                    .read(coarsenetwork);

            log.info("done reading coarse network");

            log.info("merge networks");
            output = Streams.concat(germanyNetwork.getLinks().values().stream(), network.getLinks().values().stream())
                    .collect(NetworkUtils.getCollector(new NetworkConfigGroup()));
            log.info("just kidding");
        } else {

            output = network;
        }
 */
        log.info("Finished parsing network. Start Network cleaner.");
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.car));
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.ride));
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.bike));

        log.info("Finished cleaning network. Write network");
        new NetworkWriter(network).write(this.output);

        log.info("Finished CreateNetwork. Exiting.");
        return 0;
    }

    private static List<Geometry> getGeometries(String path){

        if(path == null) {

            log.info("Path to geometry has not been initialized. Returning null...");
            return null;
        }

        return ShapeFileReader.getAllFeatures(path).stream()
                .map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
                .collect(Collectors.toList());
    }

    private static boolean isInDetailedArea(List<Geometry> detailedArea, Coord coord, int level){

        if (detailedArea == null) return false;

        if(level == LinkProperties.LEVEL_PRIMARY){

            return detailedArea.stream()
                    .anyMatch(geometry -> geometry.covers(MGC.coord2Point(coord)));
        } else return false;
    }

    private void setAllowedMode(Link link, Map<String, String> tags) {

        if (isCarOnly(tags)) {
            link.setAllowedModes(carRide);
        } else {
            link.setAllowedModes(carRideBike);
        }
    }

    private boolean isCarOnly (Map<String, String> tags) {

        var highwayType = tags.get(OsmTags.HIGHWAY);
        return highwayType == null || highwayType.equals(OsmTags.MOTORWAY) || highwayType.equals(OsmTags.MOTORWAY_LINK) || highwayType.equals(OsmTags.TRUNK) || highwayType.equals(OsmTags.TRUNK_LINK);
    }

    private String downloadGermanyLatestOsmPbf(){

        String filename = "osm/network.osm.pbf";
        URL germanyOSM;
        int count = 0;

        while(true){

            try{
                log.info("Try to open URL");
                germanyOSM = new URL("http://download.geofabrik.de/europe/germany-latest.osm.pbf");
                FileUtils.copyURLToFile(germanyOSM, new File(filename));
                break;

            } catch (IOException e){

                e.printStackTrace();
                if(count++ > 4) return null;
            }
        }

        return filename;
    }
}

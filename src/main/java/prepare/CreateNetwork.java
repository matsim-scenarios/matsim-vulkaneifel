package prepare;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import picocli.CommandLine;

import java.util.Arrays;
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

    @CommandLine.Option(names = "--output", description = "path to output directory", required = true)
    private String output;

    @CommandLine.Option(names = "--detailedArea", description = "path to shape that covers detailed network e.g. nrw", required = true)
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
        var detailedAreaGeometries = getGeometries(detailedArea);

        var veryDetailedAreaBox = getVeryDetailedAreaBox(veryDetailedArea);

        log.info("done reading shape file");

        log.info("Start to parse network. This might not output anything for a while");
        var network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(transformation)
                .setIncludeLinkAtCoordWithHierarchy((coord, level) -> {
                    if(level == LinkProperties.LEVEL_MOTORWAY) return true;

                    if(level <= LinkProperties.LEVEL_SECONDARY) return detailedAreaGeometries.stream()
                            .anyMatch(geometry -> geometry.covers(MGC.coord2Point(coord)));

                    return veryDetailedAreaBox.covers(MGC.coord2Point(coord));

                })
                .setAfterLinkCreated((link, tags, direction) -> setAllowedMode(link, tags))
                .build()
                .read(osmnetwork);

        log.info("Finished parsing network. Start Network cleaner.");
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.car));
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.ride));
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.bike));

        log.info("Finished cleaning network. Write network");
        new NetworkWriter(network).write(this.output + "/vulkaneifel-network.xml.gz");

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

    private PreparedGeometry getVeryDetailedAreaBox(String pathToVeryDetailedArea) {

        List<Geometry> veryDetailedAreaGeometry = getGeometries(pathToVeryDetailedArea);

        Point center;
        double highestDistance;

        Point tempPoint = veryDetailedAreaGeometry.stream()
                .map(Geometry::getCentroid)
                .reduce(
                        (point, point2) -> new GeometryFactory().createPoint(new Coordinate(point.getX() + point2.getX(), point.getY() + point2.getY()))
                )
                .get();

        center = new GeometryFactory().createPoint( new Coordinate( tempPoint.getX() / veryDetailedAreaGeometry.size(),
                tempPoint.getY() / veryDetailedAreaGeometry.size() ));

        var maxDistanceFromCentroid = veryDetailedAreaGeometry.stream()
                .map(Geometry::getCoordinates)
                .flatMap(Arrays::stream)
                .reduce((coordinate, coordinate2) -> {

                    Coord coord1 = MGC.coordinate2Coord(coordinate);
                    Coord coord2 = MGC.coordinate2Coord(coordinate2);

                    return NetworkUtils.getEuclideanDistance(MGC.point2Coord(center), coord1) > NetworkUtils.getEuclideanDistance(MGC.point2Coord(center), coord2) ?
                            coordinate: coordinate2;

                }).get();

        highestDistance = NetworkUtils.getEuclideanDistance(MGC.point2Coord(center), MGC.coordinate2Coord(maxDistanceFromCentroid)) * 5;

        var left = center.getX() - highestDistance;
        var right = center.getX() + highestDistance;
        var top = center.getY() + highestDistance;
        var bottom = center.getY() - highestDistance;

        var geometry = new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(left, top), new Coordinate(right, top), new Coordinate(right, bottom), new Coordinate(left, bottom), new Coordinate(left, top)
        });

        return new PreparedGeometryFactory().create(geometry);
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
}

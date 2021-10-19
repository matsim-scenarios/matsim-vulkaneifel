package prepare;

import com.google.common.collect.Streams;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CommandLine.Command(
        name="network",
        description = "Create matsim network from osm data",
        showDefaultValues = true
)

public class CreateNetwork implements MATSimAppCommand {

    @CommandLine.Option(names = "--osmnetwork", description = "path to osm data files", defaultValue = "rheinland-pfalz-latest.osm.pbf")

    private String osmnetwork;

    private static final Logger log = LogManager.getLogger(CreateNetwork.class);
    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:25832");
    private static final Set<String> carRideBike = Set.of(TransportMode.car, TransportMode.ride, TransportMode.bike);
    private static final Set<String> carRide = Set.of(TransportMode.car, TransportMode.ride);

    private static final String FILE_DIRECTORY = "C:\\Users\\ACER\\Desktop\\Uni\\Bachelorarbeit\\MATSim\\" +
            "Erstellung-Vulkaneifel\\";
    private static final String RHEINLAND_PFALZ_OSMPBF =  FILE_DIRECTORY + "rheinland-pfalz-latest.osm.pbf";
    private static final String GERMANY_OSMPBF = "C:\\Users\\ACER\\Desktop\\Uni\\Bachelorarbeit\\MATSim\\" +
            "Erstellung-Vulkaneifel\\germany-latest.osm.pbf";
    private static final String DILUTIONSHAPEFILEPATH = FILE_DIRECTORY + "dilutionArea.shp";
    private static final String OUTPUTFILEPATH = FILE_DIRECTORY + "vulkaneifel-network.xml.gz";

    public static void main(String[] args) {
        System.exit(new CommandLine(new CreateNetwork()).execute(args));
    }

    @Override
    public Integer call() throws Exception {

        /*
        var coarseLinkProperties = LinkProperties.createLinkProperties().entrySet().stream()
                .filter(entry -> entry.getValue().getHierarchyLevel() <= LinkProperties.LEVEL_PRIMARY)
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        log.info("reading in coarse network");
        var germanyNetwork = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(transformation)
                .setLinkProperties(coarseLinkProperties)
                .setAfterLinkCreated((link, tags, direction) -> setAllowedMode(link, tags))
                .build()
                .read(GERMANY_OSMPBF);


         */


        log.info("done reading coarse network");

        log.info("Loading shape file for diluation area");
        var dilutionArea = getDilutionArea(DILUTIONSHAPEFILEPATH);
        var veryDetailedArea = getBox(dilutionArea.getCentroid(), 20000);

        log.info("Start to parse network. This might not output anything for a while");
        var network = new OsmBicycleReader.Builder()
                .setCoordinateTransformation(transformation)
                .setLinkProperties(new ConcurrentHashMap<>(LinkProperties.createLinkProperties()))
                .setIncludeLinkAtCoordWithHierarchy((coord, level) -> {
                    if (level <= LinkProperties.LEVEL_SECONDARY) return true;
                    return veryDetailedArea.covers(MGC.coord2Point(coord));
                })
                .setAfterLinkCreated((link, tags, direction) -> setAllowedMode(link, tags))
                .build()
                .read(RHEINLAND_PFALZ_OSMPBF);

        /*
        log.info("merge networks");
        var network = Streams.concat(germanyNetwork.getLinks().values().stream(), rheinlandPfalzNetwork.getLinks().values().stream())
               .collect(NetworkUtils.getCollector());
        log.info("just kidding");


         */

        log.info("Finished parsing network. Start Network cleaner.");
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.car));
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.ride));
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.bike));

        log.info("Finished cleaning network. Write network");
        new NetworkWriter(network).write(OUTPUTFILEPATH);

        log.info("Finished CreateNetwork. Exiting.");
        return 0;
    }

    private static Geometry getDilutionArea(String filepath){

        log.info("Loading shape file for diluation area");

        return (Geometry) ShapeFileReader.getAllFeatures(filepath).stream().
                findFirst().orElseThrow().getDefaultGeometry();
    }

    private PreparedGeometry getBox(Point center, double diameter) {

        var left = center.getX() - diameter / 2;
        var right = center.getX() + diameter / 2;
        var top = center.getY() + diameter / 2;
        var bottom = center.getY() - diameter / 2;

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

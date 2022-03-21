package prepare;

import com.conveyal.gtfs.model.Stop;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@CommandLine.Command(
        name="filter-population",
        description = "Filter population by shape file",
        showDefaultValues = true
)

public class PopulationFilter implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(PopulationFilter.class);

    @CommandLine.Parameters(paramLabel = "INPUT", arity = "1", description = "Path to population")
    private Path input;

    @CommandLine.Option(names = "--output", description = "name of output plans file", required = false)
    private String output;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions("EPSG:4326");

    public static void main(String[] args) {
        System.exit(new CommandLine(new PopulationFilter()).execute(args));
    }
    @Override
    public Integer call() {

        var populationPath = input;

        if(output == null) output = input.toString();

        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationPath.toString());

        Predicate<Activity> filter = (activity) -> true;
        if (shp.getShapeFile() != null) {
            // default input is set to lat lon
            ShpOptions.Index index = shp.createIndex(crs.getInputCRS(), "_");
            filter = (activity) -> !index.contains(activity.getCoord());
        }

        Predicate<Activity> finalFilter = filter;
        var personsToRemove = scenario.getPopulation().getPersons().values().parallelStream()
                .filter(person -> {
                    var activities = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                       return activities.stream().anyMatch(finalFilter);

                   // return activities.stream().anyMatch(activity -> !filterGeometry.covers(MGC.coord2Point(activity.getCoord())));
                })
                .collect(Collectors.toList());

        log.info("Filter " + personsToRemove.size() + " of " + scenario.getPopulation().getPersons().size() + " persons.");
        for (var person : personsToRemove) {
            scenario.getPopulation().removePerson(person.getId());
        }

        new PopulationWriter(scenario.getPopulation()).write(output);

        return 0;
    }

    private List<Geometry> getGeometries(String shapeFile){

        return ShapeFileReader.getAllFeatures(shapeFile).stream().map(feature -> (Geometry)feature.getDefaultGeometry())
                .collect(Collectors.toList());
    }

    private List<PreparedGeometry> getPreparedGeometries(String shapeFile){

        return ShapeFileReader.getAllFeatures(shapeFile).stream().map(feature -> (Geometry)feature.getDefaultGeometry())
                .map(geometry -> new PreparedGeometryFactory().create(geometry))
                .collect(Collectors.toList());
    }


    private boolean isInGeometry(List<PreparedGeometry> geometries, Coord coord){
        int counter = 0;

        for (var geometry: geometries){

            counter++;
            if (geometry.covers(MGC.coord2Point(coord)))  {

                log.info("Geometry #" + counter + " covered the activity");
                return false;
            }
        }

        return true;
    }

    private PreparedGeometry getPreparedGeometry(String shapeFile) {

        return ShapeFileReader.getAllFeatures(shapeFile).stream()
                .map(feature -> (Geometry)feature.getDefaultGeometry())
                .map(geometry -> new PreparedGeometryFactory().create(geometry))
                .findAny()
                .orElseThrow();
    }
}

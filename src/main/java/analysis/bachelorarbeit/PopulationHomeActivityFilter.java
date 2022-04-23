package analysis.bachelorarbeit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.util.stream.Collectors;

public class PopulationHomeActivityFilter {

    private static final String sampleSize = "25";

    private static final String workingDirectory = "./scenario/open-vulkaneifel-scenario/vulkaneifel-0.1-" + sampleSize + "pct/";
    private static final String pathToDilutionArea = workingDirectory + "dilutionArea/dilutionArea.shp";
    private static final String pathToPopulation = workingDirectory + "vulkaneifel-" + sampleSize + "pct-plans.xml.gz";

    private static final String pathToOutput = workingDirectory + sampleSize + "pct" + "-plans-filtered.xml.gz";

    private static final Logger logger = LogManager.getLogger(PopulationHomeActivityFilter.class);

    public static void main(String[] args) {

        logger.info("Start reading shape file from " + pathToDilutionArea);
        Geometry dilutionArea = (Geometry) ShapeFileReader.getAllFeatures(pathToDilutionArea).stream()
                .findFirst()
                .get()
                .getDefaultGeometry();
        logger.info("Done reading shape file from " + pathToDilutionArea);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(pathToPopulation);

        logger.info("Start filter population for home activities within dilution area");
        var filteredPersons = scenario.getPopulation().getPersons().values().stream()
                .filter(person -> {
                    var activities = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                    return activities.stream()
                            .filter(activity -> activity.getType().startsWith("home"))
                            .anyMatch(activity -> dilutionArea.covers(MGC.coord2Point(activity.getCoord())));
                })
                .collect(Collectors.toList());
        logger.info("Filtered " + (scenario.getPopulation().getPersons().values().size() - filteredPersons.size()) + " persons from population");
        logger.info("++++ Done ++++");

        Population filteredPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        for (Person person: filteredPersons) {

            filteredPopulation.addPerson(person);
        }

        logger.info("Writing new population to " + pathToPopulation);
        new PopulationWriter(filteredPopulation).write(pathToOutput);

        logger.info("Finished writing. Have a nice day!");
    }
}

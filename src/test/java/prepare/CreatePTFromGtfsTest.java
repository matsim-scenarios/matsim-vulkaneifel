package prepare;

import graphql.AssertException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CreatePTFromGtfsTest {

    private final Logger log = LogManager.getLogger(CreatePTFromGtfsTest.class);

    @Test
    public void testVehiclesInTransitSchedule(){

        Config config = ConfigUtils.createConfig();

        String prefix = "./scenario/open-vulkaneifel-scenario/nrw-sued-rlp-saar/";


        for (String path: List.of(
                "temp/vulkaneifel-train-transitSchedule.xml.gz",
                "temp/vulkaneifel-bus-transitSchedule.xml.gz",
                "temp/vulkaneifel-transitSchedule-only-regional-train.xml.gz"
        )) {
            log.info("+++++++++ Start testing " + path + " +++++++++");
            config.transit().setTransitScheduleFile(prefix + path);
            Scenario scenario = ScenarioUtils.loadScenario(config);

            log.info("+++++++++ Initializing Stream +++++++++");
            //get vehicle ids from schedule
            scenario.getTransitSchedule().getTransitLines().values().stream()
                    .map(TransitLine::getRoutes)
                    .flatMap(routeMap -> routeMap.values().stream())
                    .map(route -> route.getDepartures().values())
                    .flatMap(Collection::stream)
                    .map(Departure::getVehicleId)
                    .forEach(Assert::assertNotNull);
        }
    }

    @Test
    public void testIfEveryVehicleExistsInVehiclesFile(){

        Config config = ConfigUtils.createConfig();

        String prefix = "./scenario/open-vulkaneifel-scenario/nrw-sued-rlp-saar/";

        String pathToSchedule = prefix + "temp/vulkaneifel-bus-transitSchedule.xml.gz";
        String pathToVehicles = prefix + "temp/vulkaneifel-bus-transitVehicles.xml.gz";

        config.transit().setTransitScheduleFile(pathToSchedule);
        config.transit().setVehiclesFile(pathToVehicles);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        var vehicleIds = scenario.getTransitVehicles().getVehicles().values().stream()
                .map(Identifiable::getId)
                .collect(Collectors.toList());

        scenario.getTransitSchedule().getTransitLines().values().stream()
                .map(TransitLine::getRoutes)
                .flatMap(idTransitRouteMap -> idTransitRouteMap.values().stream())
                .map(transitRoute -> transitRoute.getDepartures().values())
                .flatMap(Collection::stream)
                .map(Departure::getVehicleId)
                .forEach(vehicleIdInSchedule -> assertTrue(vehicleIds.contains(vehicleIdInSchedule)));

    }
}
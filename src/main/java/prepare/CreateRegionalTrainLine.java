package prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import picocli.CommandLine;

import java.util.*;
import java.util.stream.Collectors;

public class CreateRegionalTrainLine implements MATSimAppCommand {

    @CommandLine.Option(names = "--vehicles", description = "input matsim transit vehicles")
    private String vehicles;

    @CommandLine.Option(names = "--schedule", description = "input matsim transit schedule")
    private String schedule;

    @CommandLine.Option(names = "--network", description = "path to base network file", required = true)
    private String network;

    @CommandLine.Option(names = "--name", description = "name of the scenario", required = true)
    private String name;

    @CommandLine.Option(names = "--output", description = "output file path")
    private String output;

    public static void main(String[] args) { System.exit(new CommandLine(new CreateRegionalTrainLine()).execute(args));}

    @Override
    public Integer call() throws Exception {

        /**
         *
         * script extracts and removes SEV busline form bus schedule and creates new schedule and vehicles,
         * which contains only the regional train
         */

        String sevID = "0_SEV---1747";

        Id<TransitLine> id = Id.create(sevID, TransitLine.class);

        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(schedule);

        var networkWithPt = NetworkUtils.readNetwork(network, config);

        var scenario = ScenarioUtils.loadScenario(config);

        var transitSchedule = scenario.getTransitSchedule();

        var busLine = transitSchedule.getTransitLines().get(id);

        var trainRoutes = createTrainRoutes(
                busLine.getRoutes().values(),
                networkWithPt
        );

        var factory = transitSchedule.getFactory();

        //creates empty transit schedule for the new train line
        var trainSchedule = factory.createTransitSchedule();

        var trainLine = factory.createTransitLine(Id.create("RB26---edit", TransitLine.class));
        trainLine.setName("RB26");

        var trainVehicle = scenario.getVehicles();

        for (var trainRoute: trainRoutes){

            //collect vehicleId by findFirst because there is only one departure per route
            var vehicleId = trainRoute
                    .getDepartures()
                    .values()
                    .stream()
                    .map(Departure::getVehicleId)
                    .findFirst()
                    .get();

            trainLine.addRoute(trainRoute);
            trainVehicle.addVehicle(
                    createTrainVehicle(vehicleId)
            );
        }

          /*TODO

        add Route CHECK!
        add Vehicles CHECK!
        add departures CHECK!
         */

        transitSchedule.removeTransitLine(busLine);
        trainSchedule.addTransitLine(trainLine);
        addFaciltiesFromTransitLine(trainSchedule, trainLine);

        new TransitScheduleWriter(transitSchedule).writeFile(output + name + "-transitSchedule-edit.xml.gz");
        new TransitScheduleWriter(trainSchedule).writeFile(output + name + "-transitSchedule-only-regional-train.xml.gz");
        new MatsimVehicleWriter(trainVehicle).writeFile(output + "/" + name + "-transitVehicles-only-regional-train.xml.gz");


        return 0;
    }

    private static void addFaciltiesFromTransitLine(TransitSchedule transitSchedule, TransitLine transitLine){

        var facilities = transitLine
                .getRoutes()
                .values()
                .stream()
                .findFirst()
                .get()
                .getStops()
                .stream()
                .map(TransitRouteStop::getStopFacility)
                .collect(Collectors.toList());

        for(var f: facilities) transitSchedule.addStopFacility(f);
    }

    private static Vehicle createTrainVehicle(Id<Vehicle> vehicleId){

        var vehicleFactory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getVehicles().getFactory();

        VehicleType reRbVehicleType = vehicleFactory.createVehicleType( Id.create( "RE_RB_veh_type", VehicleType.class ) );

        VehicleCapacity capacity = reRbVehicleType.getCapacity();
        capacity.setSeats( 500 );
        capacity.setStandingRoom( 600 );
        VehicleUtils.setDoorOperationMode(reRbVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
        VehicleUtils.setAccessTime(reRbVehicleType, 1.0 / 10.0); // 1s per boarding agent, distributed on 10 doors
        VehicleUtils.setEgressTime(reRbVehicleType, 1.0 / 10.0); // 1s per alighting agent, distributed on 10 doors
//            scenario.getTransitVehicles().addVehicleType( reRbVehicleType );

        return vehicleFactory.createVehicle(vehicleId, reRbVehicleType);
    }

    private static List<TransitRouteStop> createTransitRouteStop(Departure departure, List<TransitStopFacility> facilities, List<? extends Node> nodes){

        //note: facility.getLinkId gets the link id from the link TO the facility (depending on route direction)
        //assumes that links in list are in the right order
        double time = departure.getDepartureTime();
        double stopTime = 120;
        double totalTraveltime = 0;

        if(facilities.size() != nodes.size()) throw new RuntimeException("There are either more or less facilities than nodes...");

        List<TransitRouteStop> stops = new ArrayList<>();
        var transitScheduleFactory = ScenarioUtils
                .createScenario(ConfigUtils.createConfig())
                .getTransitSchedule()
                .getFactory();

        var networkFactory = ScenarioUtils
                .createScenario(ConfigUtils.createConfig())
                .getNetwork()
                .getFactory();

        List<Link> ptLinks = new ArrayList<>();

        for(int i = 0; i < nodes.size() - 1; i ++){

            if(i == 0){

                Id<Link> id = Id.createLinkId("pt_edit_start");

                Link link = networkFactory.createLink(id, nodes.get(i), nodes.get(i));
                link.setFreespeed(120/3.6);
                link.setNumberOfLanes(1);
                link.setAllowedModes(Set.of("pt"));
                link.setCapacity(1000000.0);
                link.setLength(50);

                facilities.get(i + 1).setLinkId(id);
                ptLinks.add(link);
            }

            //creates pt links

            Node fromNode = nodes.get(i);
            Node toNode = nodes.get(i + 1);

            Id<Link> id = Id.createLinkId("pt_edit_" + i);

            Link link = networkFactory.createLink(id, fromNode, toNode);
            link.setFreespeed(120/3.6);
            link.setNumberOfLanes(1);
            link.setAllowedModes(Set.of("pt"));
            link.setCapacity(1000000.0);
            link.setLength(
                    NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord()) * 1.2
                    //multiply distance with 1.2 because railway is not 100 percent straight :)
            );

            facilities.get(i + 1).setLinkId(id);
            ptLinks.add(link);


            totalTraveltime += link.getLength()/link.getFreespeed();

            //create stops

            var stop = transitScheduleFactory
                    .createTransitRouteStopBuilder(facilities.get(i + 1))
                    .arrivalOffset(departure.getDepartureTime() + totalTraveltime)
                    .departureOffset(totalTraveltime += stopTime)
                    .build();

            stops.add(stop);
        }


        return stops;
    }

    private static List<TransitRoute> createTrainRoutes(Collection<TransitRoute> busRoutes, Network network){

        var factory = ScenarioUtils
                .createScenario(ConfigUtils.createConfig())
                .getTransitSchedule()
                .getFactory();

        double lineFrequency = 3600.;

        var busRoute = busRoutes.stream().findFirst().get();

        List<Id<Link>> linkIds = busRoute.getRoute().getLinkIds();

        var facilities = busRoute
                .getStops()
                .stream()
                .map(TransitRouteStop::getStopFacility)
                .collect(Collectors.toList());

        var routeNodes = busRoute
                .getStops()
                .stream()
                .map(stop -> network.getNodes().get(Id.createNodeId("pt_" + stop.getStopFacility().getId())))
                .collect(Collectors.toList());

        List<TransitRoute> trainRoutes = new ArrayList<>();

        final NetworkRoute trainNetworkRoute = RouteUtils.createNetworkRoute(linkIds);

        for(int i = 4*3600; i < 22*3600; i += lineFrequency){

            //creates route every hour from 4 am to 10 pm
            //its a very nice Nullsymmetrie by the way

            Departure departure = factory.createDeparture(Id.create(i, Departure.class), i);
            departure.setVehicleId(Id.createVehicleId("CustomTrain--" + i));

            List<TransitRouteStop> trainRouteStops =  createTransitRouteStop(departure, facilities, routeNodes);

            TransitRouteImpl trainRoute = (TransitRouteImpl) factory.createTransitRoute(
                    Id.create("CustomTrain--" + i, TransitRoute.class),
                    trainNetworkRoute,
                    trainRouteStops,
                    "rail"
                    );

            //add departure to route
            trainRoute.addDeparture(departure);
            //add route to route collection
            trainRoutes.add(trainRoute);
        }

        return trainRoutes;
    }
}
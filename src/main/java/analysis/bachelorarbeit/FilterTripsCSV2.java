package analysis.bachelorarbeit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


public class FilterTripsCSV2 {

    private static final String sampleSize = "25pct";
    private static final String runId = "050";
    private static final String workingDirectory = "C:\\Users\\ACER\\IdeaProjects\\matsim-vulkaneifel\\scenario" +
            "\\open-vulkaneifel-scenario\\calibration\\";
    private static final String tripsFilepath = workingDirectory + sampleSize + "\\" + runId + ".output_trips.csv.gz";
    private static final String shapeFilePath = "C:\\Users\\ACER\\IdeaProjects\\matsim-vulkaneifel\\scenario\\open-vulkaneifel-scenario\\calibration\\dilutionArea\\dilutionArea.shp";
    private static final String populationFilePath = "C:\\Users\\ACER\\IdeaProjects\\matsim-vulkaneifel\\scenario" +
            "\\open-vulkaneifel-scenario\\vulkaneifel-0.1-" + sampleSize + "\\" + sampleSize + "-plans-filtered.xml.gz";

    private static final Logger logger = LogManager.getLogger(FilterTripsCSV2.class);

    private static int counter = 0;

    public static void main(String[] args) {

        List<Trip> tripsWithinZone = new ArrayList<>();

        List<Geometry> geometries = getGeometry(shapeFilePath);
        List<Trip> tripList = readFile(tripsFilepath);

        List<Id<Person>> personIds = getPersonInDilutionArea(populationFilePath);

        for (var trip: tripList){

            if (isTripInGeometries(trip, geometries) && containsId(personIds, trip.getPersonId())) tripsWithinZone.add(trip);

            if (counter++ % 100 == 0) logger.info("Actual trip number: " + counter + "\n" + (tripList.size() - counter) +
                    " trips left.");
        }

        printCSV(workingDirectory + sampleSize + "\\" + runId + ".trips_filtered.csv",tripsWithinZone);
    }

    public static void decompressGzip(Path source, Path target) {

        logger.info("Start decompress");
        try (GZIPInputStream gis = new GZIPInputStream(
                new FileInputStream(source.toFile()));
             FileOutputStream fos = new FileOutputStream(target.toFile())) {

            // copy GZIPInputStream to FileOutputStream
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

        } catch (IOException e){

            e.printStackTrace();
        }

    }

    private static List<Id<Person>> getPersonInDilutionArea(String pathToPopulation){

        logger.info("Start reading population from " + pathToPopulation);
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(pathToPopulation);

        return new ArrayList<>(scenario.getPopulation().getPersons().keySet());
    }

    private static boolean containsId(List<Id<Person>> personList, Id<Person> id){

        return personList.contains(id);
    }

    private static List<Trip> readFile(String filepath) {

        Path tripsUnzip = Path.of(workingDirectory + "trips_unziped.csv");
        decompressGzip(Path.of(filepath), tripsUnzip);

        File inputFile = tripsUnzip.toFile();
        List<Trip> trips = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new FileReader(inputFile))) {
            logger.info("++++++ Start reading from " + filepath + " ++++++");

            String[] header = in.readLine().split(";"); //read in the headline
            String line ;
            while ((line = in.readLine()) != null) {
                String[] trip_attributes = line.split(";");

                HashMap<String, String> currentTrip = new HashMap<>();

                for (int i = 0; i < header.length-2; i++){
                    //we create a hashmap, so we can later decide which attributes we want to have in the later csv
                    currentTrip.put(header[i],trip_attributes[i]);
                }

                trips.add(new Trip(currentTrip));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("++++++ Done reading from " + filepath + " ++++++");
        return trips;
    }

    private static void printCSV(String outputFilePath, List<Trip> tripsList) {

        if (tripsList.isEmpty()) {

            logger.info("++++++ Trip list appears to be empty ++++++");
            return;
        }

        StringBuilder header = new StringBuilder();

        //get the whole keySet as a String with ';' for the csv data

        for (String key: tripsList.get(0).getTripAttributes().keySet()){
            if (!key.contains("x") && !key.contains("y")) {
                header.append(key).append(";");
            }
        }

        PrintWriter pWriter;
        try {
            pWriter = new PrintWriter(
                    new BufferedWriter(new FileWriter(outputFilePath)));

            logger.info("++++++ Start printing filtered trips to csv ++++++");
            pWriter.println(header);

            for (var trip: tripsList){
                pWriter.println(trip.toString());
            }

            pWriter.close();
            logger.info("++++++ Done printing filtered trips to csv ++++++");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Geometry> getGeometry(String shapeFilePath){

        return ShapeFileReader.getAllFeatures(shapeFilePath).stream()
                .map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
                .collect(Collectors.toList());
    }

    private static boolean isInGeometry(List<Coord> coords, List<Geometry> geometries){
        if(coords.size() > 2) logger.info("++++++ WARNING: Coords list contains more than two coords, are you sure you " +
                "are analyzing trips? ++++++");

        return coords.stream().
                anyMatch(coord -> geometries.stream()
                        .anyMatch(geometry -> geometry.covers(MGC.coord2Point(coord)))
                );
    }

    private static boolean isTripInGeometries(Trip trip, List<Geometry> geometries){
        Map<String, String> tripAttributes = trip.getTripAttributes();

        var deptCoord = new Coord(Double.parseDouble(tripAttributes.get("start_x")),
                Double.parseDouble(tripAttributes.get("start_y")));
        var arrCoord = new Coord(Double.parseDouble(tripAttributes.get("end_x")),
                Double.parseDouble(tripAttributes.get("end_y")));

     return isInGeometry(List.of(deptCoord, arrCoord), geometries);
    }

    private static class Trip{
        private final Map<String,String> tripAttributes;
        private Id<Person> personId;

        public Trip(HashMap<String,String> tripAsHashMap){

            tripAttributes = new HashMap<>();

            tripAttributes.put("personID",tripAsHashMap.get("person"));
            tripAttributes.put("traveled_distance", tripAsHashMap.get("traveled_distance"));
            tripAttributes.put("mainMode",tripAsHashMap.get("longest_distance_mode"));
            tripAttributes.put("legList",tripAsHashMap.get("modes"));
            tripAttributes.put("traveltime", computeTimeInSeconds(tripAsHashMap.get("trav_time")));

            tripAttributes.put("start_x", tripAsHashMap.get("start_x"));
            tripAttributes.put("start_y", tripAsHashMap.get("start_y"));
            tripAttributes.put("end_x", tripAsHashMap.get("end_x"));
            tripAttributes.put("end_y", tripAsHashMap.get("end_y"));

            personId = Id.createPersonId(tripAsHashMap.get("person"));
        }

        private String computeTimeInSeconds(String travelTimeInOrginalFormat){

            String[] s = travelTimeInOrginalFormat.split(":");

            int timeInSeconds = 0;

            for (int i = 0; i < s.length; i++){
                timeInSeconds += Integer.parseInt(s[i])*60^(2-i);
            }

            return String.valueOf(timeInSeconds);
        }

        public Map<String, String> getTripAttributes() {
            return tripAttributes;
        }

        public Id<Person> getPersonId() {
            return personId;
        }

        @Override
        public String toString() {
            StringBuilder tripAsStringForCSV = new StringBuilder();

            //its important, that we iterate by the order of the keys, otherwise the attributes will be in the wrong column in the csv later

            for (String key: tripAttributes.keySet()){
                if (!key.contains("x") && !key.contains("y")) {

                    if (tripAttributes.get(key).length() == 0){
                        tripAsStringForCSV.append("NA"); continue;
                    }

                    tripAsStringForCSV.append(tripAttributes.get(key)).append(";");
                }
            }

            return tripAsStringForCSV.toString();
        }
    }
}

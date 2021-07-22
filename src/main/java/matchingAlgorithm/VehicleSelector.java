package matchingAlgorithm;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.DrtRequest;

import java.util.Collection;

public interface VehicleSelector {
	VehicleEntry selectVehicleEntryForRequest(DrtRequest request, Collection<VehicleEntry> vehicleEntries, double timeOfTheDay);
}

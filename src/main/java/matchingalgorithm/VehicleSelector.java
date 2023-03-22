package matchingalgorithm;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.DrtRequest;

import java.util.Collection;
/**
 * Interface to select drt vehicles for a certain request.
 **/
public interface VehicleSelector {
	/**
	 * Selects a drt vehicle for a certain request.
	 **/
	VehicleEntry selectVehicleEntryForRequest(DrtRequest request, Collection<VehicleEntry> vehicleEntries, double timeOfTheDay);
}

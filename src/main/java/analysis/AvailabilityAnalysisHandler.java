package analysis;

import org.matsim.contrib.drt.analysis.zonal.DrtZone;

import java.util.Map;

/**
 * Interface to get the availabilty rate of drt vehicles in a service area zone.
 */
public interface AvailabilityAnalysisHandler {
	Map<DrtZone, Double> getAllDayAvailabilityRate();

	Map<DrtZone, Double> getPeakHourAvailabilityRate();
}

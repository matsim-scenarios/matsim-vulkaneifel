package analysis;

import org.matsim.contrib.drt.analysis.zonal.DrtZone;

import java.util.Map;

public interface AvailabilityAnalysisHandler {
    Map<DrtZone, Double> getAllDayAvailabilityRate();

    Map<DrtZone, Double> getPeakHourAvailabilityRate();
}

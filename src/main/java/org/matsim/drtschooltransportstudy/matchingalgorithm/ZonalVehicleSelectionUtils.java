package org.matsim.drtschooltransportstudy.matchingalgorithm;

import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.*;

/**
 * Utils class to get available drt vehicles per zone.
 * @deprecated not in use, TODO: needs documentation why not
 */
@Deprecated
public final class ZonalVehicleSelectionUtils {

	private ZonalVehicleSelectionUtils() {
	}

	/**
	 * Returns a map with all available drt vehicles per service zone.
	 */
	public static Map<DrtZone, List<VehicleEntry>> groupDisposableVehicleEntriesPerZone(DrtZonalSystem zonalSystem,
																						Collection<VehicleEntry> vEntries, double time, double maxWaitTime) {
		Map<DrtZone, List<VehicleEntry>> disposableVehicleEntriesPerZone = new HashMap<>();
		for (VehicleEntry vEntry : vEntries) {
			DvrpVehicle vehicle = vEntry.vehicle;
			int finalTaskIndex = vehicle.getSchedule().getTaskCount() - 1;
			DrtStayTask finalStayTask = (DrtStayTask) vehicle.getSchedule().getTasks().get(finalTaskIndex);
			DrtZone zone = zonalSystem.getZoneForLinkId(finalStayTask.getLink().getId());
			if (finalStayTask.getBeginTime() < time + maxWaitTime) {
				disposableVehicleEntriesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(vEntry);
			}
		}
		return disposableVehicleEntriesPerZone;
	}
}

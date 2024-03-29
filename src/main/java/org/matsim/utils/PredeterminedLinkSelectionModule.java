package org.matsim.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

/**
 * Install module to use predetermined links.
 */
public class PredeterminedLinkSelectionModule extends AbstractDvrpModeModule {
	private static final Logger log = LogManager.getLogger(PredeterminedLinkSelectionModule.class);
	private final String pathToPredeterminedPoints = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\ZonalSystem\\RequestClusterTargetPoints.shp";

	public PredeterminedLinkSelectionModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
	}

	@Override
	public void install() {
		log.info("Now installing the Predetermined target link selection module. The path to the shapefile containing the target points is: ");
		log.info(pathToPredeterminedPoints);
		bindModal(DrtZoneTargetLinkSelector.class).toProvider(
				modalProvider(getter -> new PredeterminedPointTargetLinkSelector(getter.getModal(DrtZonalSystem.class),
						pathToPredeterminedPoints)))
				.asEagerSingleton();
	}

}

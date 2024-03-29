package org.matsim.analysis;

import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
/**
 * Module class to install availability handler.
 * */
public class ZonalAvailabilityModule extends AbstractDvrpModeModule {

	public ZonalAvailabilityModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
	}

	@Override
	public void install() {
		bindModal(ZonalAvailabilityHandler.class)
				.toProvider(modalProvider(getter -> new ZonalAvailabilityHandler(getter.getModal(DrtZonalSystem.class),
						getter.getModal(NeighbouringZoneIdentifier.class))))
				.asEagerSingleton();

		bindModal(NeighbouringZoneIdentifier.class).toProvider(
				modalProvider(getter -> new NeighbouringZoneIdentifier(getter.getModal(DrtZonalSystem.class))));

		bindModal(ZonalAvailabilityResultWriter.class)
				.toProvider(modalProvider(
						getter -> new ZonalAvailabilityResultWriter(getter.getModal(ZonalAvailabilityHandler.class))))
				.asEagerSingleton();

		addEventHandlerBinding().to(modalKey(ZonalAvailabilityHandler.class));
		addMobsimListenerBinding().to(modalKey(ZonalAvailabilityHandler.class));
		addControlerListenerBinding().to(modalKey(ZonalAvailabilityResultWriter.class));
	}
}

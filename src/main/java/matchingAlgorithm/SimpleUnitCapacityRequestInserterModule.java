package matchingAlgorithm;

import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import utils.VehicleAssignmentTools;

public class SimpleUnitCapacityRequestInserterModule extends AbstractDvrpModeQSimModule {

	private final DrtConfigGroup drtCfg;
	private final double maxEuclideanDistance;

	public SimpleUnitCapacityRequestInserterModule(DrtConfigGroup drtCfg, double maxEuclideanDistance) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.maxEuclideanDistance = maxEuclideanDistance;
	}

	@Override
	protected void configureQSim() {
		bindModal(UnplannedRequestInserter.class)
				.toProvider(modalProvider(getter -> new SimpleUnitCapacityRequestInserter(drtCfg,
						getter.getModal(Fleet.class), getter.get(EventsManager.class), getter.get(MobsimTimer.class),
						getter.getModal(DrtScheduleInquiry.class), getter.getModal(VehicleAssignmentTools.class),
						maxEuclideanDistance)))
				.asEagerSingleton();

	}
}
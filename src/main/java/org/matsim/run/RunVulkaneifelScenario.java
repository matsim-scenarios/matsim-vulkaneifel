package org.matsim.run;

import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.CheckPopulation;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.freight.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.prepare.*;
import picocli.CommandLine;

@CommandLine.Command(header = ":: Open Vulkaneifel Scenario ::", version = RunVulkaneifelScenario.VERSION)
@MATSimApplication.Prepare({
		CreateNetwork.class, CreateTransitScheduleFromGtfs.class, CreateRegionalTrainLine.class, RemoveBusLineFromSmallSchedule.class, ExtractHomeCoordinates.class,
		MergeTransitSchedules.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class, CleanPopulation.class, ResolveGridCoordinates.class, MergePopulations.class,
		DownSamplePopulation.class, ExtractRelevantFreightTrips.class, FixSubtourModes.class, AdjustActivityToLinkDistances.class, CreateLandUseShp.class, PreparePopulation.class
})
@MATSimApplication.Analysis({CheckPopulation.class})
public class RunVulkaneifelScenario extends MATSimApplication {

	public static final String VERSION = "1.1";

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(25, 1);

	public static void main(String[] args) {
		MATSimApplication.run(RunVulkaneifelScenario.class, args);
	}

	public RunVulkaneifelScenario() {
		super(String.format("input/vulkaneifel-v%s-25pct.config.xml", VERSION));
	}

	@Override
	protected Config prepareConfig(Config config) {

		SnzActivities.addScoringParams(config);

		config.controler().setOutputDirectory(sample.adjustName(config.controler().getOutputDirectory()));
		config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		config.controler().setRunId(sample.adjustName(config.controler().getRunId()));

		config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
		config.qsim().setStorageCapFactor(sample.getSize() / 100.0);

		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setUsePersonIdForMissingVehicleId(false);

		return config;
	}
}

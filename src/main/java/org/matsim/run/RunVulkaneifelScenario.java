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
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.prepare.*;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand;
import picocli.CommandLine;

@CommandLine.Command(header = ":: Open Vulkaneifel Scenario ::", version = RunVulkaneifelScenario.VERSION)
@MATSimApplication.Prepare({
		CreateNetwork.class, CreateTransitScheduleFromGtfs.class, CreateRegionalTrainLine.class, RemoveBusLineFromSmallSchedule.class, ExtractHomeCoordinates.class,
		MergeTransitSchedules.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class, CleanPopulation.class, ResolveGridCoordinates.class, MergePopulations.class,
		DownSamplePopulation.class, ExtractRelevantFreightTrips.class, FixSubtourModes.class, AdjustActivityToLinkDistances.class, CreateLandUseShp.class, PreparePopulation.class,
		SplitActivityTypesDuration.class, GenerateSmallScaleCommercialTrafficDemand.class
})
@MATSimApplication.Analysis({CheckPopulation.class})
public class RunVulkaneifelScenario extends MATSimApplication {

	public static final String VERSION = "1.2";

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(25, 1);

	public static void main(String[] args) {
		MATSimApplication.run(RunVulkaneifelScenario.class, args);
	}

	public RunVulkaneifelScenario() {
		super(String.format("input/v%s/vulkaneifel-v%s-25pct.config.xml", VERSION, VERSION));
	}

	@Override
	protected Config prepareConfig(Config config) {

		SnzActivities.addScoringParams(config);

		// Prepare commercial types
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("service").setTypicalDuration(3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_start").setTypicalDuration(3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_end").setTypicalDuration(3600));

		config.controller().setOutputDirectory(sample.adjustName(config.controller().getOutputDirectory()));
		config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		config.controller().setRunId(sample.adjustName(config.controller().getRunId()));

		config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
		config.qsim().setStorageCapFactor(sample.getSize() / 100.0);

		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setUsePersonIdForMissingVehicleId(false);

		return config;
	}

	@Override
	protected void prepareControler(Controler controler) {

		controler.addOverridingModule(new SimWrapperModule());

	}
}

package run;

import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.CheckPopulation;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.freight.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import picocli.CommandLine;
import prepare.*;

import java.util.List;

@CommandLine.Command(header = ":: Open Vulkaneifel Scenario ::", version = "1.1")
@MATSimApplication.Prepare({
		CreateNetwork.class, CreateTransitScheduleFromGtfs.class, CreateRegionalTrainLine.class, RemoveBusLineFromSmallSchedule.class, ExtractHomeCoordinates.class,
		MergeTransitSchedules.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class, CleanPopulation.class, ResolveGridCoordinates.class, MergePopulations.class,
		DownSamplePopulation.class, ExtractRelevantFreightTrips.class, FixSubtourModes.class, AdjustActivityToLinkDistances.class, CreateLandUseShp.class, PreparePopulation.class
})

@MATSimApplication.Analysis({CheckPopulation.class})

public class VulkaneifelApplication extends MATSimApplication {

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(25, 1);

	public static void main(String[] args) {
		MATSimApplication.run(VulkaneifelApplication.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {

		for (long ii = 600; ii <= 97200; ii += 600) {

			for (String act : List.of("business", "educ_higher", "educ_kiga", "educ_other", "educ_primary", "educ_secondary",
					"educ_tertiary", "errands", "home", "leasure", "shop_daily", "shop_other", "visit", "work", "other")) {
				config.planCalcScore()
						.addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act + "_" + ii).setTypicalDuration(ii));
			}

			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work_" + ii).setTypicalDuration(ii)
					.setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("business_" + ii).setTypicalDuration(ii)
					.setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("leisure_" + ii).setTypicalDuration(ii)
					.setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));
			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shopping_" + ii).setTypicalDuration(ii)
					.setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
		}

		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("freight_start").setTypicalDuration(60 * 15));
		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("freight_end").setTypicalDuration(60 * 15));

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

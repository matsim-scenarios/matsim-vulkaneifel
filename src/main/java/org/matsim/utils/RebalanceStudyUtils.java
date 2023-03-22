package org.matsim.utils;

import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams.RebalancingTargetCalculatorType;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams.ZonalDemandEstimatorType;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;
/**
 * Helpful functions for the drt rebalancing study.
 * */
public final class RebalanceStudyUtils {

	private RebalanceStudyUtils(){}

	/**
	* Prepares the rebalancing strategy.
	 * */
	public static void prepareAdaptiveRealTimeStrategy(RebalancingParams rebalancingParams) {
		MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
		minCostFlowRebalancingStrategyParams.rebalancingTargetCalculatorType =
				RebalancingTargetCalculatorType.EqualRebalancableVehicleDistribution;
		minCostFlowRebalancingStrategyParams
				.zonalDemandEstimatorType = ZonalDemandEstimatorType.PreviousIterationDemand;
		minCostFlowRebalancingStrategyParams.targetAlpha = 1;
		minCostFlowRebalancingStrategyParams.targetBeta = 0;
		minCostFlowRebalancingStrategyParams.demandEstimationPeriod = 108000;
		rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);
	}

	/**
	 * Prepares the rebalancing strategy.
	 * */
	public static void preparePlusOneStrategy(RebalancingParams rebalancingParams) {
		rebalancingParams.addParameterSet(new PlusOneRebalancingStrategyParams());
	}

	/**
	 * Prepares the rebalancing strategy.
	 * */
	public static void prepareFeedforwardStrategy(RebalancingParams rebalancingParams) {
		FeedforwardRebalancingStrategyParams feedforwardRebalancingStrategyParams = new FeedforwardRebalancingStrategyParams();
		feedforwardRebalancingStrategyParams.feedbackSwitch = true;
		feedforwardRebalancingStrategyParams.feedforwardSignalLead = 300;
		feedforwardRebalancingStrategyParams.minNumVehiclesPerZone = 1;
		rebalancingParams.addParameterSet(feedforwardRebalancingStrategyParams);
	}

	/**
	 * Prepares the rebalancing strategy.
	 * */
	public static void preparePureFeedforwardStrategy(RebalancingParams rebalancingParams) {
		FeedforwardRebalancingStrategyParams feedforwardRebalancingStrategyParams = new FeedforwardRebalancingStrategyParams();
		feedforwardRebalancingStrategyParams.feedbackSwitch = false;
		feedforwardRebalancingStrategyParams.feedforwardSignalLead = 300;
		rebalancingParams.addParameterSet(feedforwardRebalancingStrategyParams);
	}

	/**
	 * Prepares the rebalancing strategy.
	 * */
	public static void prepareMinCostFlowStrategy(RebalancingParams rebalancingParams) {
		MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
		minCostFlowRebalancingStrategyParams.rebalancingTargetCalculatorType =
				RebalancingTargetCalculatorType.EstimatedDemand;
		minCostFlowRebalancingStrategyParams
				.zonalDemandEstimatorType = ZonalDemandEstimatorType.PreviousIterationDemand;
		minCostFlowRebalancingStrategyParams.targetAlpha = 0.8;
		minCostFlowRebalancingStrategyParams.targetBeta = 0.3;
		minCostFlowRebalancingStrategyParams.demandEstimationPeriod = 1800;
		rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);
	}

	// As we may want to see the waiting time in different zones, we need to use a dummy rebalancing strategy
	// This is due to the current setup in the zonal system creation. No rebalnce --> no DRT zonal system
	/**
	 * Prepares the rebalancing strategy.
	 * */
	public static void prepareNoRebalance(RebalancingParams rebalancingParams) {
		FeedforwardRebalancingStrategyParams feedforwardRebalancingStrategyParams = new FeedforwardRebalancingStrategyParams();
		feedforwardRebalancingStrategyParams.feedbackSwitch = false;
		feedforwardRebalancingStrategyParams.feedforwardSignalStrength = 0;
		rebalancingParams.addParameterSet(feedforwardRebalancingStrategyParams);
	}
}

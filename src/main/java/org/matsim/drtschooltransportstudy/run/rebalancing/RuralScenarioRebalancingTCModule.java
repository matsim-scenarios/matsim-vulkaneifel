package org.matsim.drtschooltransportstudy.run.rebalancing;

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.ZonalDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

/**
 * Module for the specific rural rebalancing.
 */
public class RuralScenarioRebalancingTCModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;
	private final double lookAheadTime;

	public RuralScenarioRebalancingTCModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		// Default value: 300 seconds (5 minutes)
		this.lookAheadTime = 300;
	}

	public RuralScenarioRebalancingTCModule(DrtConfigGroup drtCfg, double lookAheadTime) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.lookAheadTime = lookAheadTime;
	}

	@Override
	protected void configureQSim() {
		RebalancingParams params = drtCfg.getRebalancingParams().orElseThrow();
		// Currently, this one only works with Min Cost Flow strategy
		assert params.getName().equals(MinCostFlowRebalancingStrategyParams.SET_NAME);
		MinCostFlowRebalancingStrategyParams strategyParams = (MinCostFlowRebalancingStrategyParams) params.getRebalancingStrategyParams();
		assert strategyParams.targetAlpha == 1;
		assert strategyParams.targetBeta == 0;

		bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
				getter -> new RuralScenarioRebalancingTargetCalculator(
						getter.getModal(ZonalDemandEstimator.class),
						strategyParams.demandEstimationPeriod, lookAheadTime))).asEagerSingleton();
	}
}

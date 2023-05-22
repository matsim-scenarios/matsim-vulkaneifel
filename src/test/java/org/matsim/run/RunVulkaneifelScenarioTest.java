package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.*;

public class RunVulkaneifelScenarioTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testVulkaneifelWith1PctSample() {

		String[] args = {
				"run",
				"--1pct",
				"--config=input/vulkaneifel-v1.1-25pct.config.xml",
				"--config:controler.lastIteration=1",
				"--config:controler.outputDirectory=" + utils.getOutputDirectory(),
				"--config:global.numberOfThreads=2",
				"--config:qsim.numberOfThreads=2"
		};

		MATSimApplication.run(RunVulkaneifelScenario.class, args);
	}
}

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
				"--config:controler.lastIteration=5",
				"--config:controler.outputDirectory=" + utils.getOutputDirectory()
		};

		MATSimApplication.run(RunVulkaneifelScenario.class, args);
	}
}
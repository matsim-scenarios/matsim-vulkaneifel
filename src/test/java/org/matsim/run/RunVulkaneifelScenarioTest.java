package org.matsim.run;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
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
				"--config=input/v1.2/vulkaneifel-v1.2-25pct.config.xml",
				"--config:plans.inputPlansFile=https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/v1.2/input/vulkaneifel-v1.2-1pct.plans-initial.xml.gz",
				"--config:controler.lastIteration=1",
				"--config:controler.outputDirectory=" + utils.getOutputDirectory(),
				"--config:global.numberOfThreads=2",
				"--config:qsim.numberOfThreads=2"
		};

		int ret = MATSimApplication.execute(RunVulkaneifelScenario.class, args);
		assertEquals(0, ret);
	}
}

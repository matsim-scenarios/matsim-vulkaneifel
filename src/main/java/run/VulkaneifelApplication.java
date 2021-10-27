package run;

import org.matsim.application.MATSimApplication;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import picocli.CommandLine;
import prepare.CreateNetwork;

@CommandLine.Command(header = ":: Open Vulkaneifel Scenario ::", version="1.0")
@MATSimApplication.Prepare({
        CreateNetwork.class, CreateTransitScheduleFromGtfs.class
})

public class VulkaneifelApplication extends MATSimApplication {

    public static void main(String[] args) {
        MATSimApplication.run(VulkaneifelApplication.class, args);
    }

}

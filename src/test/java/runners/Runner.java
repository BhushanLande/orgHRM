package runners;

import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
        features = "src/test/resources/cucumberFeatures",
        glue = {"stepDefinitions"},
        plugin = {"pretty"},
        tags = "@TC04"
//        tags = "@TC04 and @Skip"
//        tags = "@TC04 and @Run and not @Skip"
)
public class Runner {
}

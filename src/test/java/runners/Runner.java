package runners;

import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
        features = "src/test/java/resources/cucumberFeatures",
        glue = {"stepDefinitions"},
        plugin = {"pretty"},
        tags = "@TC01"
)
public class Runner {
}

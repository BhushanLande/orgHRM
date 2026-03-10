package stepDefinitions;

import io.cucumber.java.en.*;
import pageObjects.EchoAPIPage;

public class EchoAPIStepDefinitions {

    EchoAPIPage echoApiPage = new EchoAPIPage();

    @Given("User prepares echo API request")
    public void prepareRequest() {
    }

    @When("User sends POST request to echo API")
    public void sendRequest() {
        echoApiPage.sendRequest();
    }

    @Then("API response status should be {int}")
    public void validateStatus(int status) {
        echoApiPage.validateStatusCode(status);
    }
}
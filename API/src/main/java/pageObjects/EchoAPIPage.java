package pageObjects;

import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;

import static org.junit.Assert.assertEquals;

public class EchoAPIPage {

    private Response response;

    // Prepare request (optional)
    public void prepareRequest() {
        // No payload needed for GET
        // Base URI optional
    }

    // Send GET request
    public void sendRequest() {
        response = SerenityRest
                .given()
                .header("Accept", "application/json") // optional
                .when()
                .get("https://echo.hoppscotch.io") // simple GET
                .then()
                .extract()
                .response();

        // Print response for debugging
        System.out.println("Response: " + response.asString());
    }

    // Validate HTTP status code
    public void validateStatusCode(int expectedStatus) {
        int actualStatus = response.getStatusCode();
        System.out.println("Actual Status Code: " + actualStatus);
        assertEquals(expectedStatus, actualStatus);
    }
}
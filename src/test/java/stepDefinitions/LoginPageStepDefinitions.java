package stepDefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import pageObjects.InvokeColumns;
import pageObjects.LoginPage;

import java.io.IOException;
import java.util.List;

public class LoginPageStepDefinitions {

    LoginPage loginPage;
    InvokeColumns invokeColumns = new InvokeColumns();
    @Given("user launches Login page of demo site")
    public void user_launches_login_page_of_demo_site() {
        loginPage.open();
    }

    @Given("user launch orange HRM site and enter creds")
    public void user_launch_orange_hrm_site_and_enter_creds() throws InterruptedException, IOException {
        String filepath ="src/test/ExcelData/UserInputs.xlsx";
        List<String> usernames = invokeColumns.getColumnDataByTitle(filepath, "Creds", "Username");
        List<String> passwords = invokeColumns.getColumnDataByTitle(filepath, "Creds", "Password");
        loginPage.enterTheUsername(usernames.get(0));
        loginPage.enterThePassword(passwords.get(0));
        loginPage.clickOnTheLoginButton();
    }

    @Then("user wait for landing page to load")
    public void user_wait_for_landing_page_to_load() {

    }
    @Then("user search with {string}")
    public void user_search_with(String options) throws InterruptedException {
        loginPage.enterTheSearchOptions(options);
    }

}

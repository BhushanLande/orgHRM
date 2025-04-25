package stepDefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import pageObjects.LoginPage;

public class LoginPageStepDefinitions {

    LoginPage loginPage;

    @Given("user launches Login page of demo site")
    public void user_launches_login_page_of_demo_site() {
        loginPage.open();
    }

    @Given("user launch orange HRM site and enter creds")
    public void user_launch_orange_hrm_site_and_enter_creds() throws InterruptedException {
        loginPage.enterTheUsername("Admin");
        loginPage.enterThePassword("admin123");
        loginPage.clickOnTheLoginButton();
    }

    @Then("user wait for landing page to load")
    public void user_wait_for_landing_page_to_load() {

    }

}

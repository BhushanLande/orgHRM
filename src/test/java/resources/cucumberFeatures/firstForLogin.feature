@Cucumber
@Login

Feature: Login for demo site OrangeHRM

  @TC01
  Scenario: Verify the user is login by providing valid credentials
    Given user launches Login page of demo site
    Then user launch orange HRM site and enter creds
    Then user wait for landing page to load

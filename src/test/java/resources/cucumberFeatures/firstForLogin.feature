@Cucumber
@Login

Feature: Login for demo site OrangeHRM

  @TC01
  Scenario: Verify the user is login by providing valid credentials
    Given user launches Login page of demo site
    Then user launch orange HRM site and enter creds
    Then user wait for landing page to load

  @TC02 @Smoke
  Scenario: Verify the utest 2
    Given user launches Login page of demo site
    Then user launch orange HRM site and enter creds
    Then user wait for landing page to load

  @TC03 @Smoke
  Scenario: Verify the utest 3
    Given user launches Login page of demo site
    Then user launch orange HRM site and enter creds
    Then user wait for landing page to load

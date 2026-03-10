Feature: Echo API Validation

  @TC01
  Scenario: Validate echo API response
    Given User prepares echo API request
    When User sends POST request to echo API
    Then API response status should be 200

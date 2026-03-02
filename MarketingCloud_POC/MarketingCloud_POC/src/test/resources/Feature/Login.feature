Feature: SFMC Email Testing POC

  @Login
  Scenario Outline: Login SFMC, Trigger Email Template and validate the email content
    Given User login into the Salesforce
    And User selects the "Content Builder" option from "Salesforce Home" page
    And User navigates to "Automation" folder from "Local" on "Content Builder" page
    When User selects the <EmailName> email option from "Content Builder" page
    And User selects the "Preview and Test" option from "Content Builder" page
    And User selects the "Sample Row Data Extension" from "Subscriber Preview" page
    And User clicks on the "Test Send" button on "Preview and Test" page
    And User enters <ReceiverEmail> in the "Individuals" field on "Preview and Test" page
    And User clicks on the "Send Test" button on "Preview and Test" page
    And User clicks on the "Confirm and Send" button on "Preview and Test" page
    Then Verify the "Test send successfully sent." message on "Preview and Test" page
    When Get email from receiver's inbox for <EmailName>
    Then validate broken links from email

    Examples: 
      | EmailName     | ReceiverEmail                     |
      | "Sample Mail - Automation Test 1" | "sample@suleepqt.mailosaur.net"   |
    ##  | "Sample HTML" | "test_marketingcloud@yopmail.com" |
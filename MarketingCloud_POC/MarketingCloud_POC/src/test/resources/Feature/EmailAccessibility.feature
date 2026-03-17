Feature: Email accessibility validation
  As an automation engineer
  I want to validate accessibility of email HTML retrieved from Mailosaur
  So that emails meet WCAG thresholds

  @Accessibility
  Scenario Outline: Validate email accessibility
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
    And Load the latest email HTML into the browser
    Then validate email accessibility compliance
    
 Examples: 
      | EmailName     | ReceiverEmail                     |
      | "Sample Mail - Automation Test 1" | "sample@j2rzr3mt.mailosaur.net"  |
      
      
   @E2E
  Scenario Outline: E2E : Validate email accessibility and broken links
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
    And Load the latest email HTML into the browser
    Then validate broken links from email
    And validate email accessibility compliance
    Then validate all images in the email are loading
    And Generate and log the final validation summary report
    And report all validation failures
    
    
 Examples: 
      | EmailName     | ReceiverEmail                     |
     ## | "Spring Lookbook 2026" | "sample@brpuvaer.mailosaur.net"  |
      | "Sample Mail - Automation Test 1" | "sample@brpuvaer.mailosaur.net"  |
      ##| "Perfectly Imperfect Email For Testing" | "sample@suleepqt.mailosaur.net"  |
      
      
     @MailosaurUI 
Scenario Outline: E2E : Trigger SFMC Email and Validate via Mailosaur UI
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

    When User reads the "Email" body from "Mailosaur" page
    And Load the latest email HTML into the browser

    Then validate broken links from email
    And validate email accessibility compliance
    Then validate all images in the email are loading
    And Generate and log the final validation summary report
    And report all validation failures

Examples: 
      | EmailName  				| ReceiverEmail                    |
      | "Spring Lookbook 2026" | "sample@j2rzr3mt.mailosaur.net"  |

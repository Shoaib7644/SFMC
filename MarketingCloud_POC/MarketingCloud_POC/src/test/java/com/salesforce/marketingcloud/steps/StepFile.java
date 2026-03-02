package com.salesforce.marketingcloud.steps;

import com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter;
import com.microsoft.playwright.FrameLocator;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.exception.CustomException;
import com.salesforce.marketingcloud.pageobjects.ContentBuilderPage;
import com.salesforce.marketingcloud.utils.CommonUtils;
import com.salesforce.marketingcloud.utils.PlaywrightUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class StepFile {

	ContentBuilderPage contentBuilderPage = new ContentBuilderPage();
	
	@Given("User login into the Salesforce {string} Environment for {string} Contact")
	public void user_login_into_the(String value, String contactName) throws Throwable {
		CommonUtils.salesforcelogin();
		ExtentCucumberAdapter.addTestStepLog(Constant.result);
	}
	
	@Given("User login into the Salesforce")
	public void user_login_into_the_salesforce() {
		Constant.env = "QA";
	    CommonUtils.salesforcelogin();
	}
	
	@Given("User swtiches to {string} App")
	public void user_switches_to_app(String appName) throws Throwable {
		CommonUtils.switchToApp(appName);
		ExtentCucumberAdapter.addTestStepLog(Constant.result);
	}

	@Given("User switches to {string} user in {string}")
	public void user_switches_to_user_in(String userName, String page) throws Throwable {
		CommonUtils.switchToUserByAPI(userName);
		ExtentCucumberAdapter.addTestStepLog(Constant.result);
	}

	@When("User clicks on the {string} button on {string} page")
	public void user_clicks_on_the_something_button(String button, String page) throws Throwable {
		switch (button) {
		case "Test Send":
		case "Send Test":
		case "Confirm and Send":
	    	FrameLocator contentBuilderFrame = PlaywrightUtils.getFrame("Content Builder Frame", Constant.PAGE);
	    	PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(button, contentBuilderFrame));
	    	PlaywrightUtils.click(PlaywrightUtils.getElement(button, contentBuilderFrame));
			break;
		default:
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(button, Constant.PAGE));
			PlaywrightUtils.click(PlaywrightUtils.getElement(button, Constant.PAGE));
			ExtentCucumberAdapter.addTestStepLog("'" + button + "' Button clicked successfully");
			break;
		}
	}

	@Then("User enters {string} in the {string} field on {string} page")
	public void user_enters_in_the_field_on_Page(String value, String field, String page) throws Throwable {
		switch (field) {
		case "Individuals":
	    	FrameLocator contentBuilderFrame = PlaywrightUtils.getFrame("Content Builder Frame", Constant.PAGE);
	    	PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(field, contentBuilderFrame));
	    	PlaywrightUtils.setValueUsingKeyboard(value, PlaywrightUtils.getElement(field, contentBuilderFrame));
			break;
		default:
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(field, Constant.PAGE));
			PlaywrightUtils.setValueUsingKeyboard(value, PlaywrightUtils.getElement(field, Constant.PAGE));
			ExtentCucumberAdapter.addTestStepLog("'" + value + "' value entered in '" + field + "' field successfully");
			break;
		}
	}

	@Then("User selects {string} in the {string} dropdown on {string} page")
	public void user_selects_in_the_dropdown(String value, String field, String page) throws Throwable {
		switch (field) {
		default:
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(field, Constant.PAGE));
			PlaywrightUtils.selectByLabelFromDropdown(value, PlaywrightUtils.getElement(field, Constant.PAGE));
			break;
		}
	}

	@Given("User refreshes the browser")
	public void user_refreshes_the_browser() throws Throwable {
		PlaywrightUtils.refreshBrowser(Constant.PAGE);
	}
	
	@Then("Verify the {string} message on {string} page")
	public void verify_the_message(String element, String page) throws Throwable {
		switch (element) {
		case "Test send successfully sent.":
	    	FrameLocator contentBuilderFrame = PlaywrightUtils.getFrame("Content Builder Frame", Constant.PAGE);
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(element, contentBuilderFrame));
			break;
		default:
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(element, Constant.PAGE));
			break;
		}
		ExtentCucumberAdapter.addTestStepLog("'" + element + "' displayed successfully");
	}

	@Then("Verify {string} field value is {string} on {string} page")
	public void verify_field_value_is_on_page(String fieldName, String value, String page) throws Throwable {
		switch (fieldName+"_"+value) {
		default:
			PlaywrightUtils.waitForMoreSec(4);
			CommonUtils.verifyFieldValue(value, fieldName, Constant.PAGE);
			break;
		}
		ExtentCucumberAdapter.addTestStepLog(Constant.result);
	}

	@Then("User compares copied values {string} from {string} and {string} page")
	public void user_compares_the_addresses_from_and_page(String condition, String page1, String page2) throws Throwable {
		CommonUtils.compareMaps(condition);
	}

	@Then("User updates {string} field value to {string} on {string} page")
	public void user_updtaes_field_value_to_on_page(String fieldName, String value, String page) throws Throwable {
		switch (fieldName) {
		default:
			throw new CustomException("'" + fieldName + "' case is not defined in respective Stepdef");
		}
	}

	@When("User checks {string} checkbox on {string} page")
	public void user_checks_checkbox_on_page(String field, String page) throws Throwable {
		switch (field) {
		default:
			PlaywrightUtils.checkCheckbox(PlaywrightUtils.getElement(field, Constant.PAGE));
			break;
		}
	}

	@Then("User waits for element {string} to load")
	public void user_waits_for_element_to_load(String locator) throws Throwable{
		PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(locator,Constant.PAGE));
	}

	@Given("User logouts from {string} user in {string}")
	public void user_logouts_from_user_in(String userName, String page) throws Throwable {
		CommonUtils.userLogout(userName);
		ExtentCucumberAdapter.addTestStepLog("Successfully logout from '" + userName + "'");
	}

	@When("User updates the {string} field on {string} as {string}")
	public void user_updates_the_field_on_account(String field, String object, String value) throws Throwable {
		switch (object) {
		default:
			throw new CustomException("'" + object + "' case is not defined in respective Stepdef");
		}
	}

	@Then("Verify {string} is created on {string} page")
	public void verify_is_created(String value, String page) throws Throwable {
		switch (value) {
		default:
			throw new CustomException("'" + value + "' case is not defined in respective Stepdef");
		}
	}
	
	@Given("User selects the {string} option from {string} page")
	public void user_select_on_the_option_from_page(String option, String string2) {
	    switch (option) {
	    case "Content Builder":
 	    	PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(option, Constant.PAGE));
	    	PlaywrightUtils.waitForMoreSec(2);
	    	PlaywrightUtils.waitForMoreSec(4);
	    	if(PlaywrightUtils.getAllElements("Close Ad", Constant.PAGE).size() > 0)
	    		PlaywrightUtils.click(PlaywrightUtils.getElement("Close Ad", Constant.PAGE));
	    	PlaywrightUtils.hover(PlaywrightUtils.getElement(option, Constant.PAGE));
	    	PlaywrightUtils.click(PlaywrightUtils.getElement( option + " Option", Constant.PAGE));
	    	PlaywrightUtils.waitForMoreSec(2);
	    	PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement( option + " Label", PlaywrightUtils.getFrame("Content Builder Frame", Constant.PAGE)));
	    	break;
	    case "Preview and Test":
	    	FrameLocator contentBuilderFrame = PlaywrightUtils.getFrame("Content Builder Frame", Constant.PAGE);
	    	PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Open Preview and Test", contentBuilderFrame));
	    	PlaywrightUtils.click(PlaywrightUtils.getElement("Open Preview and Test", contentBuilderFrame));
	    	PlaywrightUtils.click(PlaywrightUtils.getElement("Preview and Test", contentBuilderFrame));
	    	break;
	    default:
			throw new CustomException("'" + option + "' case is not defined in respective Stepdef");
		}
	}
	
	@Given("User navigates to {string} folder from {string} on {string} page")
	public void user_navigates_to_folder_from_on_page(String folder, String contentSection, String string3) {
		contentBuilderPage.selectFolder(contentSection, folder);
	}
	
	@Given("User selects the {string} email option from {string} page")
	public void user_select_on_the_email_option_from_page(String email, String string2) {
	    contentBuilderPage.selectEmail(email);
	}
	
	@Given("User selects the {string} from {string} page")
	public void user_select_on_the_from_page(String option, String string2) {
		contentBuilderPage.selectSubscriberPreview();
	}
}

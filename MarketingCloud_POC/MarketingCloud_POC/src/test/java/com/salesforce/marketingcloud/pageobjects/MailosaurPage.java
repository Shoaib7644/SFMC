package com.salesforce.marketingcloud.pageobjects;

import com.microsoft.playwright.Locator;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.exception.CustomException;
import com.salesforce.marketingcloud.utils.PlaywrightUtils;

public class MailosaurPage {
	
	public void readEmailFromWeb() {
	    try {
	        PlaywrightUtils.navigateToURL("https://mailosaur.com/", Constant.PAGE);
	        
	        // Use getElement for static locators
	        PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Sign In", Constant.PAGE));
	        PlaywrightUtils.click(PlaywrightUtils.getElement("Sign In", Constant.PAGE));
	        
	        PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Email", Constant.PAGE));
	        PlaywrightUtils.setValue("clive.shoaib@gmail.com", PlaywrightUtils.getElement("Email", Constant.PAGE));
	        PlaywrightUtils.click(PlaywrightUtils.getElement("Continue", Constant.PAGE));
	        
	        PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Password", Constant.PAGE));
	        PlaywrightUtils.setValue("Clive#005", PlaywrightUtils.getElement("Password", Constant.PAGE));
	        PlaywrightUtils.click(PlaywrightUtils.getElement("Log In", Constant.PAGE));
	        
	        PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("SFMC Emails", Constant.PAGE));
	        PlaywrightUtils.click(PlaywrightUtils.getElement("SFMC Emails", Constant.PAGE));
	        
	        Locator recentEmail = PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, "Recent Email", Constant.emailSubject);
	        PlaywrightUtils.waitForAnElement(recentEmail);
	        PlaywrightUtils.click(recentEmail);
	        // ----------------------------------

	        PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Three Dots", Constant.PAGE));
	        PlaywrightUtils.click(PlaywrightUtils.getElement("Three Dots", Constant.PAGE));
	        
	        PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("See HTML", Constant.PAGE));
	        PlaywrightUtils.click(PlaywrightUtils.getElement("See HTML", Constant.PAGE));
	        
	        PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Email Body Div", Constant.PAGE));
	        PlaywrightUtils.click(PlaywrightUtils.getElement("Email Body Div", Constant.PAGE));
	        
	        // Use your existing keyPress util
	        PlaywrightUtils.keyPress("Control+A", Constant.PAGE);
	        PlaywrightUtils.keyPress("Control+C", Constant.PAGE);
	        
	        PlaywrightUtils.waitForSec();
	        
	        // Retrieve from clipboard
	        Constant.latestEmailHtml = (String) Constant.PAGE.evaluate("navigator.clipboard.readText()");
	        System.out.println("Captured HTML Length: " + Constant.latestEmailHtml.length());

	    } catch (Exception e) {
	        throw new CustomException("Failed to read email from Mailosaur UI: " + e.getMessage());
	    }
	}

}

package com.salesforce.marketingcloud.pageobjects;

import com.microsoft.playwright.FrameLocator;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.exception.CustomException;
import com.salesforce.marketingcloud.utils.PlaywrightUtils;

public class ContentBuilderPage {
	
	/**
	 * Method to Select Folder from Content Builder Page
	 * @param contentFolder - Content Folder
	 * @param folderStructure - Folder Hierarchy
	 */
	public void selectFolder(String contentFolder, String folderStructure) {
		try {
			FrameLocator contentBuilderFrame = PlaywrightUtils.getFrame("Content Builder Frame", Constant.PAGE);
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Content Folder", contentFolder));
	    	PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Content Folder", contentFolder));
	    	PlaywrightUtils.waitForSec();
	    	PlaywrightUtils.scrollMoreVertically(Constant.PAGE);
			if(folderStructure.contains("->")) {
				String[] folders = folderStructure.split("->");
				for(int i=0; i<folders.length-1; i++) {
					PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Dynamic Span Text", folders[i].trim()));
					PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Dynamic Span Text", folders[i].trim()));
					PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Open Folder Hierarchy Arrow", folders[i].trim()));
					PlaywrightUtils.waitForSec();
					PlaywrightUtils.scrollToElement(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Open Folder Hierarchy Arrow", folders[i].trim()));
					PlaywrightUtils.waitForSec();
					if(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Dynamic Span Text", folders[folders.length - 1].trim()).all().size() == 0)
						PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Open Folder Hierarchy Arrow", folders[i].trim()));
					PlaywrightUtils.waitForSec();
					PlaywrightUtils.waitForSec();
					if(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Dynamic Span Text", folders[folders.length - 1].trim()).all().size() == 0)
						PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Open Folder Hierarchy Arrow", folders[i].trim()));
				}
				PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Dynamic Span Text", folders[folders.length - 1].trim()));
				PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Dynamic Span Text", folders[folders.length - 1].trim()));
			}
			else {
				PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Dynamic Span Text", folderStructure));
				PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Dynamic Span Text", folderStructure));
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * @param email
	 */
	public void selectEmail(String email) {
		try {
			FrameLocator contentBuilderFrame = PlaywrightUtils.getFrame("Content Builder Frame", Constant.PAGE);
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Select Email", email));
			PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(contentBuilderFrame, "Select Email", email));
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to select Subscriber Preview
	 */
	public void selectSubscriberPreview() {
		try {
			FrameLocator contentBuilderFrame = PlaywrightUtils.getFrame("Content Builder Frame", Constant.PAGE);
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Subscriber Preview", contentBuilderFrame));
			PlaywrightUtils.waitForSec();
			if(PlaywrightUtils.getAllElements("Select Subscriber Message", contentBuilderFrame).size()>0) {
				PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Folder Structure", contentBuilderFrame));
				PlaywrightUtils.click(PlaywrightUtils.getElement("Folder Structure", contentBuilderFrame));
				PlaywrightUtils.click(PlaywrightUtils.getElement("Lists", contentBuilderFrame));
				PlaywrightUtils.click(PlaywrightUtils.getElement("Data Extensions", contentBuilderFrame));
				PlaywrightUtils.setValueUsingKeyboard("SampleRow", PlaywrightUtils.getElement("Search Data Extensions", contentBuilderFrame));
				PlaywrightUtils.keyPress("Enter", Constant.PAGE);
				PlaywrightUtils.waitForSec();
				PlaywrightUtils.click(PlaywrightUtils.getElement("Sample Row Option", contentBuilderFrame));
				PlaywrightUtils.click(PlaywrightUtils.getElement("Koa Dog Option", contentBuilderFrame));
				PlaywrightUtils.click(PlaywrightUtils.getElement("Select", contentBuilderFrame));
				PlaywrightUtils.waitForSec();
				PlaywrightUtils.waitForSec();
			}
			else {
				System.out.println("Subscriber Preview is already selected.");
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
}
package com.salesforce.marketingcloud.utils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Locator.ClickOptions;
import com.microsoft.playwright.Locator.DblclickOptions;
import com.microsoft.playwright.Locator.FocusOptions;
import com.microsoft.playwright.Locator.WaitForOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.exception.CustomException;

public class PlaywrightUtils {
	
	/**
	 * Method to find Element by Dynamic Xpath Locator using params
	 * @param frame - Frame on which you are looking for Element
	 * @param fieldName - Field Name having locator stored in locators.properties
	 * @param params - Dynamic Text to be used in locator xpath
	 * @return - Locator Object having Browser Element
	 */
	public static Locator getElementByDynamicXpath(FrameLocator frame, String fieldName, Object... params) {
		Locator locator = null;
		  int counter = 0;
		  String locString = Constant.locatorsMap.get(fieldName.replaceAll(" ", "_").toLowerCase());
		  if(locString.contains("||")) {
			  String[] locators = locString.split("\\|\\|");
			  for(int i=0; i<locators.length; i++) {
				  System.out.println("\nChecking locator "+ ++counter);
				  try {
					  String temp = String.format(locators[i], params);
					  locator = getTypeOfElement(temp, frame);
				  }
			  	  catch(Exception e){
			  		  if(i == locators.length - 1)
			  			  throw new CustomException("Element not found correct your all locators");
			  		  continue;
			  	  }
				  break;
			  }
		  }
		  else {
			  locString = String.format(locString, params);
			  locator = getTypeOfElement(locString, frame);
		  }
		  return locator;
	}
	
	/**
	 * Method to find Element by Dynamic Xpath Locator using params
	 * @param page - Page on which you are looking for Element
	 * @param fieldName - Field Name having locator stored in locators.properties
	 * @param params - Dynamic Text to be used in locator xpath
	 * @return - Locator Object having Browser Element
	 */
	public static Locator getElementByDynamicXpath(Page page, String fieldName, Object... params) {
		Locator locator = null;
		  int counter = 0;
		  String locString = Constant.locatorsMap.get(fieldName.replaceAll(" ", "_").toLowerCase());
		  if(locString.contains("||")) {
			  String[] locators = locString.split("\\|\\|");
			  for(int i=0; i<locators.length; i++) {
				  System.out.println("\nChecking locator "+ ++counter);
				  try {
					  String temp = String.format(locators[i], params);
					  locator = getTypeOfElement(temp, page);
				  }
			  	  catch(Exception e){
			  		  if(i == locators.length - 1)
			  			  throw new CustomException("Element not found correct your all locators");
			  		  continue;
			  	  }
				  break;
			  }
		  }
		  else {
			  locString = String.format(locString, params);
			  locator = getTypeOfElement(locString, page);
		  }
		  return locator;
	}
	
	/**
	 * Method to find Element by given Locator
	 * @param fieldName - Field Name having locator stored in locators.properties
	 * @param page - Page on which you are looking for Element
	 * @return - Locator Object having Browser Element
	 */
	public static Locator getElement(String fieldName, Page page) {
	  Locator locator = null;
	  int counter = 0;
	  String locString = Constant.locatorsMap.get(fieldName.replaceAll(" ", "_").toLowerCase());
	  if(locString.contains("||")) {
		  String[] locators = locString.split("\\|\\|");
		  for(int i=0; i<locators.length; i++) {
			  System.out.println("\nChecking locator "+ ++counter);
			  try {
				  locator = getTypeOfElement(locators[i], page);
			  }
		  	  catch(Exception e){
		  		  if(i == locators.length - 1)
		  			  throw new CustomException("Element not found correct your all locators");
		  		  continue;
		  	  }
			  break;
		  }
	  }
	  else {
			  locator = getTypeOfElement(locString, page);
	  }
	  return locator;
	}
	
	/**
	 *  Method to find Element based on different Locator
	 * @param locString - Locator Value
	 * @param page - Page on which you are looking for Element
	 * @return - Locator Object having Browser Element
	 */
	public static Locator getTypeOfElement(String locString, Page page) {
		if(locString.contains("*")) {
		  String type = locString.split("*")[0].toLowerCase();
		  String value = locString.split("*")[1];
		  switch (type) {
			  case "alttext":
				    return page.getByAltText(value);
			  case "label":
				    return page.getByLabel(value);
			  case "placeholder":
				    return page.getByPlaceholder(value);
			  case "testid":
				    return page.getByTestId(value);
			  case "text":
				    return page.getByText(value);
			  case "title":
				    return page.getByTitle(value);
			  default:
				  throw new CustomException("Locator Strategy misspelled or does not exist");
		  }
		}
		else
		  return page.locator(locString);
	}
	
	/**
	 * Method to find All Elements by given Locator
	 * @param fieldName - Field Name having locator stored in locators.properties
	 * @param page - Page on which you are looking for Element
	 * @return - Locator Object List having Browser Element
	 */
	public static List<Locator> getAllElements(String fieldName, Page page) {
	  String locString = Constant.locatorsMap.get(fieldName.replaceAll(" ", "_").toLowerCase());
	  return Constant.PAGE.locator(locString).all();
	}
	
	/**
	 * Method to find All Elements by given Locator
	 * @param fieldName - Field Name having locator stored in locators.properties
	 * @param page - Page on which you are looking for Element
	 * @return - Locator Object List having Browser Element
	 */
	public static List<Locator> getAllElements(String fieldName, FrameLocator frameLocator) {
	  String locString = Constant.locatorsMap.get(fieldName.replaceAll(" ", "_").toLowerCase());
	  return frameLocator.locator(locString).all();
	}
	
	/**
	 * Method to find Element by given Locator from a FrameLocator
	 * @param fieldName - Field Name having locator stored in locators.properties
	 * @param frameLocator - FrameLocator on which you are looking for Element
	 * @return - Locator Object having Browser Element
	 */
	public static Locator getElement(String fieldName, FrameLocator frameLocator) {
	  Locator locator = null;
	  int counter = 0;
	  String locString = Constant.locatorsMap.get(fieldName.replaceAll(" ", "_").toLowerCase());
	  if(locString.contains("||")) {
		  String[] locators = locString.split("\\|\\|");
		  for(int i=0; i<locators.length; i++) {
			  System.out.println("\nChecking locator "+ ++counter);
			  try {
				  locator = getTypeOfElement(locators[i], frameLocator);
			  }
		  	  catch(Exception e){
		  		  if(i == locators.length - 1)
		  			  throw new CustomException("Element not found correct your all locators");
		  		  continue;
		  	  }
			  break;
		  }
	  }
	  else {
			  locator = getTypeOfElement(locString, frameLocator);
	  }
	  return locator;
	}
	
	/**
	 *  Method to find Element based on different Locator from a FrameLocator
	 * @param locString - Locator Value
	 * @param frameLocator - FrameLocator on which you are looking for Element
	 * @return - Locator Object having Browser Element
	 */
	public static Locator getTypeOfElement(String locString, FrameLocator frameLocator) {
		if(locString.contains("*")) {
		  String type = locString.split("*")[0].toLowerCase();
		  String value = locString.split("*")[1];
		  switch (type) {
			  case "alttext":
				    return frameLocator.getByAltText(value);
			  case "label":
				    return frameLocator.getByLabel(value);
			  case "placeholder":
				    return frameLocator.getByPlaceholder(value);
			  case "testid":
				    return frameLocator.getByTestId(value);
			  case "text":
				    return frameLocator.getByText(value);
			  case "title":
				    return frameLocator.getByTitle(value);
			  default:
				  throw new CustomException("Locator Strategy misspelled or does not exist");
		  }
		}
		else
		  return frameLocator.locator(locString);
	}
	
	/**
	 * Method to find Element by given Locator from a Locator
	 * @param fieldName - Field Name having locator stored in locators.properties
	 * @param loc - Locator on which you are looking for Element
	 * @return - Locator Object having Browser Element
	 */
	public static Locator getElement(String fieldName, Locator loc) {
	  Locator locator = null;
	  int counter = 0;
	  String locString = Constant.locatorsMap.get(fieldName.replaceAll(" ", "_").toLowerCase());
	  if(locString.contains("||")) {
		  String[] locators = locString.split("\\|\\|");
		  for(int i=0; i<locators.length; i++) {
			  System.out.println("\nChecking locator "+ ++counter);
			  try {
				  locator = getTypeOfElement(locators[i], loc);
			  }
		  	  catch(Exception e){
		  		  if(i == locators.length - 1)
		  			  throw new CustomException("Element not found correct your all locators");
		  		  continue;
		  	  }
			  break;
		  }
	  }
	  else {
			  locator = getTypeOfElement(locString, loc);
	  }
	  return locator;
	}
	
	/**
	 *  Method to find Element based on different Locator from a Locator
	 * @param locString - Locator Value
	 * @param loc - Locator on which you are looking for Element
	 * @return - Locator Object having Browser Element
	 */
	public static Locator getTypeOfElement(String locString, Locator locator) {
		if(locString.contains("*")) {
		  String type = locString.split("*")[0].toLowerCase();
		  String value = locString.split("*")[1];
		  switch (type) {
			  case "alttext":
				    return locator.getByAltText(value);
			  case "label":
				    return locator.getByLabel(value);
			  case "placeholder":
				    return locator.getByPlaceholder(value);
			  case "testid":
				    return locator.getByTestId(value);
			  case "text":
				    return locator.getByText(value);
			  case "title":
				    return locator.getByTitle(value);
			  default:
				  throw new CustomException("Locator Strategy misspelled or does not exist");
		  }
		}
		else
		  return locator.locator(locString);
	}
	
	/**
	 * Method to Select Option from Dropdown by Value
	 * @param valueToSelect - Dropdown Option Value
	 * @param locator - Dropdown Locator
	 */
	public static void selectByValueFromDropdown(String valueToSelect, Locator locator) {
		try {
			SelectOption options = new SelectOption();
			options.value = valueToSelect;
			locator.selectOption(options);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}		
	}
	
	/**
	 * Method to Select Option from Dropdown by Index
	 * @param index - Dropdown Index
	 * @param locator - Dropdown Locator
	 */
	public static void selectByIndexFromDropdown(int index, Locator locator) {
		try {
			SelectOption options = new SelectOption();
			options.index = Integer.valueOf(index);
			locator.selectOption(options);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}		
	}
	
	/**
	 * Method to Select Option from Dropdown by Label Value
	 * @param labelToSelect - Dropdown Option Label
	 * @param locator - Dropdown Locator
	 */
	public static void selectByLabelFromDropdown(String labelToSelect, Locator locator) {
		try {
			SelectOption options = new SelectOption();
			options.label = labelToSelect;
			locator.selectOption(options);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}		
	}
	
	/**
	 * Method to Click on Element
	 * @param locator - Element Locator
	 */
	public static void click(Locator locator) {
		try {
			ClickOptions options = new ClickOptions();
			options.button = MouseButton.LEFT;
			locator.click();
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Doubel Click on Element
	 * @param locator - Element Locator
	 */
	public static void doubleClick(Locator locator) {
		try {
			DblclickOptions options = new DblclickOptions();
			options.button = MouseButton.LEFT;
			locator.dblclick(options);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Scroll Vertically
	 * @param page - Page on which Scroll needs to be performed
	 */
	public static void scrollVertically(Page page) {
		try {
			page.mouse().wheel(0, 250);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Scroll Horizontally
	 * @param page - Page on which Scroll needs to be performed
	 */
	public static void scrollHorizontally(Page page) {
		try {
			page.mouse().wheel(250, 0);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Scroll Element into View if needed
	 * @param locator - Element Locator which need to be scrolled
	 */
	public static void scrollToElement(Locator locator) {
		try {
			locator.scrollIntoViewIfNeeded();
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Scroll until Element displayed
	 * @param locator - Element Locator which needs to ne scrolled
	 * @param lastElementOfPage - Last Element Locator of the Page
	 */
	public static void scrollUntilElementDisplayed(Locator locator, Locator lastElementOfPage, Page page) {
		try {
			WaitForOptions options = new WaitForOptions();
			options.timeout = (double) 5000;
			options.state = WaitForSelectorState.VISIBLE;
			for(int i=0; i<10; i++) {
				if(lastElementOfPage.isVisible()){
					break;
				}
				try {
					locator.waitFor(options);
				}
				catch (Exception e) {
					scrollVertically(page);
					continue;
				}
			}
			locator.scrollIntoViewIfNeeded();
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Mouse Click on Element using Coordinates
	 * @param locator - Element Locator
	 */
	public static void clickUsingCoordinates(Locator locator) {
		try {
			BoundingBox box = locator.boundingBox();
			Constant.PAGE.mouse().click(box.x + box.width / 2, box.y + box.height / 2);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Mouse Hover on Element
	 * @param locator - Element Locator
	 */
	public static void hover(Locator locator) {
		try {
			locator.hover();
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to get FrameLocator from Page
	 * @param frameName - Frame Selector
	 * @param page - Page containing Frame
	 * @return - FrameLocator to Interact with Inside Elements
	 */
	public static FrameLocator getFrame(String frameName, Page page) {
		try {
			return page.frameLocator(Constant.locatorsMap.get(frameName.replaceAll(" ", "_").toLowerCase()));
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to get Child FrameLocator from Parent FrameLocator
	 * @param frameName - Child Frame Name/Selector
	 * @param frameLocator - Parent FrameLocator
	 * @return - FrameLocator to Interact with Inside Elements in Child Frame
	 */
	public static FrameLocator getFrame(String frameName, FrameLocator frameLocator) {
		try {
			return frameLocator.frameLocator(frameName);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Accept Alert
	 * @param page - Page on which Alert occurs
	 */
	public static void alertAccept(Page page) {
		try {
			page.onDialog(dialog -> {dialog.accept();});
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Check Checkbox
	 * @param locator - Checkbox Locator
	 */
	public static void checkCheckbox(Locator locator) {
		try {
			if(!locator.isChecked())
				locator.check();
			else
				throw new CustomException(locator.toString() +" Checkbox already checked");
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Uncheck Checkbox
	 * @param locator - Checkbox Locator
	 */
	public static void uncheckCheckbox(Locator locator) {
		try {
			if(locator.isChecked())
				locator.uncheck();
			else
				throw new CustomException(locator.toString() +" Checkbox already unchecked");
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Wait for URL to occur on Page
	 * @param url - URL/Some Part of URL/URL pattern
	 * @param page - Page on which URL should occur
	 */
	public static void waitForURL(String url, Page page) {
		try {
			page.waitForURL(url);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Refresh Browser
	 * @param page - Page on which Refresh needs to performed
	 */
	public static void refreshBrowser(Page page) {
		page.reload();
		PlaywrightUtils.waitForPageToLoad(page);
	}
	
	/**
	 * Method to Set Value on Element(Input Fields)
	 * @param value - Value
	 * @param locator - Element Locator
	 */
	public static void setValue(String value, Locator locator) {
		try {
			locator.fill(value);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Clear then Set Value on Element(Input Fields)
	 * @param value - Value
	 * @param locator - Element Locator
	 */
	public static void clearAndSetValue(String value, Locator locator) {
		try {
			locator.clear();
			locator.fill(value);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Set Value on Element(Input Fields) using Keyboard
	 * @param value - Value
	 * @param locator - Element Locator
	 */
	public static void setValueUsingKeyboard(String value, Locator locator) {
		try {
			locator.pressSequentially(value);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Clear then Set Value on Element(Input Fields) using Keyboard
	 * @param value - Value
	 * @param locator - Element Locator
	 */
	public static void clearAndSetValueUsingKeyboard(String value, Locator locator) {
		try {
			locator.clear();
			locator.pressSequentially(value);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Focus on Element
	 * @param locator - Element Locator
	 */
	public static void focusOnElement(Locator locator) {
		try {
			FocusOptions options =  new FocusOptions();
			options.timeout = (double) 120000;
			locator.focus();
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Blur an Element
	 * @param locator = Element Locator
	 */
	public static void blurElement(Locator locator) {
		try {
			locator.blur();
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to wait for Element is Visible
	 * @param locator - Element Locator
	 */
	public static void waitForAnElement(Locator locator) {
		try {
			WaitForOptions options = new WaitForOptions();
			options.timeout = (double) 240000;
			options.state = WaitForSelectorState.VISIBLE;
			locator.waitFor(options);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to wait for Element is Visible
	 * @param locator - Element Locator
	 */
	public static void waitForAnElementToDisappear(Locator locator) {
		try {
			WaitForOptions options = new WaitForOptions();
			options.timeout = (double) 300000;
			options.state = WaitForSelectorState.HIDDEN;
			locator.waitFor(options);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to wait for page to load
	 * @param page - Page requires wait
	 */
	public static void waitForPageToLoad(Page page) {
		try {
			page.waitForLoadState();
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Switch to Tab by given URL
	 * @param fieldName - Field Name for Click operation
	 * @param URL - Part of URL/URL of the Tab you need to switch
	 * @param page - Page on which Click will be performed
	 * @return - New Tab Page
	 */
	public static Page clickThenSwitchToTab(String fieldName, String URL, Page page) {
		Page newPage = Constant.BROWSERCONTEXT.waitForPage(() -> {
			PlaywrightUtils.click(PlaywrightUtils.getElement(fieldName, page));
		});
		newPage.waitForLoadState();
		return newPage;
	}
	
	/**
	 * Method to wait for more time(Integer value multiplied by 3 seconds)
	 * @param multiplier - Integer value
	 */
	public static void waitForMoreSec(int multiplier) {
		try {
			TimeUnit.MILLISECONDS.sleep(multiplier * 3000);
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method for Wait 
	 */
	public static void waitForSec() {
		try {
			TimeUnit.MILLISECONDS.sleep(3000);
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Get Text from Element
	 * @param locator - Element Locator
	 * @return = Locator Text
	 */
	public static String getText(Locator locator) {
		try {
			return locator.textContent();
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Press Keyboard Keys
	 * @param keyName - Name of the Key
	 * @param page - Page on which Key should be pressed
	 */
	public static void keyPress(String keyName, Page page) {
		try {
			page.keyboard().press(keyName);
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to get Current Url from Browser Page
	 * @param page - Page from which URL is required
	 * @return - Page URL
	 */
	public static String getCurrentURL(Page page) {
		try {
			return page.url();
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Navigate to URL
	 * @param url - URL
	 * @param page - Page in which you need to open new URL
	 */
	public static void navigateToURL(String url, Page page) {
		try {
			page.navigate(url);
			page.waitForLoadState();
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to Switch to Tab by given URL
	 * @param URL - Part of URL/URL of the Tab you need to switch
	 * @return - New Tab Page
	 */
	public static Page switchToNewTab(String URL) {
		Page newPage = Constant.BROWSERCONTEXT.newPage();
		newPage.navigate(URL);
		return newPage;
	}
	
	/**
	 * Method to close all other tabs except given tab URL
	 * @param URL - Part of URL/URL of the Tab you need to switch
	 * @return - New Tab Page
	 */
	public static void closeAllTabsExceptGivenURLTab(String URL) {
		List<Page> pages = Constant.BROWSERCONTEXT.pages();
		for(Page page: pages)
			if(!page.url().contains(URL)) {
				PlaywrightUtils.waitForSec();
				page.close();
			}
	}
	
	/**
	 * Method to Scroll more Vertically 
	 * @param page - Page on which Scroll needs to be performed
	 */
	public static void scrollMoreVertically(Page page) {
		try {
			page.mouse().wheel(0, 1000);
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
}

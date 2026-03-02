package com.salesforce.marketingcloud.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;

import com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Locator.WaitForOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.exception.CustomException;
import com.salesforce.marketingcloud.managers.FileReaderManager;

public class CommonUtils {

    /**
     * Method to switch to an App using App Launcher
     * @param appName - App Name 
     */
    public static void switchToApp(String appName) {
        try {
//        	PlaywrightUtils.waitForAnElement(CommonUtils.getHomePageLoadConfirmation(Constant.env));
        	PlaywrightUtils.waitForSec();
            if(!PlaywrightUtils.getText(PlaywrightUtils.getElement("App Name", Constant.PAGE)).equals(appName)) {
                PlaywrightUtils.click(PlaywrightUtils.getElement("Nine Dots", Constant.PAGE));
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Search Apps And Item", Constant.PAGE));
                PlaywrightUtils.setValue(appName, PlaywrightUtils.getElement("Search Apps And Item", Constant.PAGE));
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, "Get Searched App Locator", appName));
                PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, "Get Searched App Locator", appName));
                PlaywrightUtils.waitForMoreSec(2);
                Constant.result =  "Swithced successfulyy to '" + appName + "' App";
            }
            else {
            	Constant.result =  "Already on '" + appName + "' App. No need to switch.";
            }
        }catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Opening Salesforce Object in Browser by given Record Id
     * @param recordId - Record Id from Salesforce
     * @param page - Page on which you are navigating to URL
     */
    public static void getRecordInSalesforce(String recordId, Page page) {
        try {
        	if(Constant.baseURL == null)
        		Constant.baseURL = FileReaderManager.getInstance().getConfigReader().getPayloadURL();
            page.navigate(Constant.baseURL + "/" + recordId);
            PlaywrightUtils.waitForPageToLoad(page);
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }

    /**
     * Method for Switching to Salesforce User
     * @param - Salesforce User whom do you want to switch
     */
    public static void switchToUser(String user){
        try {
            PlaywrightUtils.click(PlaywrightUtils.getElement("Gear Icon", Constant.PAGE));
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Setup", Constant.PAGE));
            Constant.PAGE = PlaywrightUtils.clickThenSwitchToTab("Setup", "SetupOneHome", Constant.PAGE);
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Search Setup", Constant.PAGE));
            PlaywrightUtils.setValue(user, PlaywrightUtils.getElement("Search Setup", Constant.PAGE));
            PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            		"Get Div Tag Locator by Exact Text", user));
            FrameLocator scrollingFrame = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.click(PlaywrightUtils.getElement("Login User", scrollingFrame));
            PlaywrightUtils.waitForMoreSec(3);
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Switching to Salesforce User
     * @param - Salesforce User whom do you want to switch
     */
    public static void switchToUserByNormalSearch(String user){
        try {
            PlaywrightUtils.waitForPageToLoad(Constant.PAGE);
            PlaywrightUtils.waitForMoreSec(2);
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Search Box Salesforce", Constant.PAGE));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Search Box Salesforce", Constant.PAGE));
            PlaywrightUtils.waitForSec();
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Search Box Input Salesforce", Constant.PAGE));
            PlaywrightUtils.setValue(user, PlaywrightUtils.getElement("Search Box Input Salesforce", Constant.PAGE));
            PlaywrightUtils.keyPress("Enter", Constant.PAGE);
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, "Get Span Tag Locator by Exact Text", "People"));
            PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, "Get Span Tag Locator by Exact Text", "People"));
            PlaywrightUtils.waitForSec();
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, "Get Anchor Tag Locator by Exact Text", "People"));
            PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, "Get Anchor Tag Locator by Exact Text", "People"));
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            		"Get Div Tag Locator by Exact Text", user));
            PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            		"Get Div Tag Locator by Exact Text", user));
            FrameLocator scrollingFrame = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Login User", scrollingFrame));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Login User", scrollingFrame));
            PlaywrightUtils.waitForMoreSec(3);
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            		"Get User Logout button Locator", user));
            Constant.result = "Switched to '" + user + "' Successfully";
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Switching to Salesforce User
     * @param - Salesforce User whom do you want to switch
     */
    public static void switchToUserByAPI(String user){
        try {
            PlaywrightUtils.waitForPageToLoad(Constant.PAGE);
            PlaywrightUtils.waitForMoreSec(2);
            String Id = APIUtils.getSoqlResult(String.format("Select Id from User where Name = '%s'", user), "Id");
            CommonUtils.getRecordInSalesforce(Id, Constant.PAGE);
			PlaywrightUtils.waitForMoreSec(3);
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            		"Get Div Tag Locator by Exact Text", user));
            PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            		"Get Div Tag Locator by Exact Text", user));
            FrameLocator scrollingFrame = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Login User", scrollingFrame));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Login User", scrollingFrame));
            PlaywrightUtils.waitForMoreSec(3);
//            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
//    		"Get User Logout button Locator", user));
            PlaywrightUtils.waitForMoreSec(2);
            Constant.result = "Switched to '" + user + "' Successfully";
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }

    /**
     * Method for Salesforce Login
     */
    public static void salesforcelogin() {
        try {
            NavigateOptions options = new NavigateOptions();
            options.timeout = (double) 3000000;
            Constant.PAGE.navigate(FileReaderManager.getInstance().getConfigReader().getSfdcUrl(), options);
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Username Salesforce", Constant.PAGE));
            String Username = FileReaderManager.getInstance().getConfigReader().getSfdcUsername();
            String Password = FileReaderManager.getInstance().getConfigReader().getSfdcPassword();
            PlaywrightUtils.clearAndSetValue(Username, PlaywrightUtils.getElement("Username Salesforce", Constant.PAGE));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Next", Constant.PAGE));
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Password Salesforce", Constant.PAGE));
            PlaywrightUtils.clearAndSetValue(Password, PlaywrightUtils.getElement("Password Salesforce", Constant.PAGE));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Login Salesforce", Constant.PAGE));
//			PlaywrightUtils.waitForAnElement(CommonUtils.getHomePageLoadConfirmation(Constant.env));
            Constant.result = "Logged Successfully in '"+Constant.env+"' Environment";
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to add days in the given date then returns the new date in String
     * @param date - Date in which Days should be added
     * @param days - No. of Days to add
     * @return - New Date
     */
    public static String addDays(String date, int days) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdf.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        cal.add(Calendar.DAY_OF_MONTH, days);
        return sdf.format(cal.getTime());
    }
    
    /**
     * Method to add months in the given date then returns the new date in String
     * @param date - Date in which Months should be added
     * @param days - No. of Months to add
     * @return - New Date
     */
    public static String addMonths(String date, int months) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdf.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        cal.add(Calendar.MONTH, months);
        return sdf.format(cal.getTime());
    }
    
    /**
     * Method for adding key-value in given map
     * @param key - Key which should be added in map
     * @param value - Value which should be added in map
     * @param mapName = Map Name to add Key & Value
     */
    public static void addKeyValueInMap(String key, String value, String mapName) {
        if(Constant.comparisonMap.containsKey(mapName))
            Constant.comparisonMap.get(mapName).put(key, value);
        else {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(key, value);
            Constant.comparisonMap.put(mapName, map);
        }
    }
    
    /**
     * Method for adding key-value in given map
     * @param key - Key which should be added in map
     * @param value - Value which should be added in map
     * @param mapName = Map Name to add Key & Value
     * @param map - Map in which you want to add map of key value
     */
    public static void addKeyValueInGivenMap(String key, Object value, String mapName, Map<String ,Map<String, Object>> map) {
        if(map.containsKey(mapName))
            map.get(mapName).put(key, value);
        else {
            HashMap<String, Object> temp = new HashMap<String, Object>();
            temp.put(key, value);
            map.put(mapName, temp);
        }
    }
    
    /**
     * Method to Compare All Maps stored in ComparisonMap
     * @param condition - Matches or Unmatches
     */
    public static void compareMaps(String condition) {
        List<String> keys = new ArrayList<String>(Constant.comparisonMap.keySet());
        String mapString = keys.get(0);
        for(int i=1; i<keys.size(); i++) {
            if(condition.equals("mismatches"))
                Assert.assertNotSame(Constant.comparisonMap.get(mapString), Constant.comparisonMap.get(keys.get(i)));
            else
                Assert.assertEquals(Constant.comparisonMap.get(mapString), Constant.comparisonMap.get(keys.get(i)));
        }
    }
    
    /**
     * Method for Verifying Field Value
     * @param value - Expected Value
     * @param fieldName - Field Name having Actual Value
     * @param page - Page having the Field
     */
    public static void verifyFieldValue(String value, String fieldName, Page page) {
        try {
            if(value.equals("Approved")) {
                Constant.quoteStatus = value;
                PlaywrightUtils.navigateToURL(Constant.quoteURL, Constant.PAGE);
            }
            if(value.equals("Presented")) {
                PlaywrightUtils.waitForMoreSec(30);
                PlaywrightUtils.refreshBrowser(page);
            }
            if(value.equals("Order Placed")) {
                PlaywrightUtils.waitForMoreSec(15);
                PlaywrightUtils.refreshBrowser(page);
            }
            if(value.equals("In Review")) {
                PlaywrightUtils.waitForMoreSec(5);
                PlaywrightUtils.refreshBrowser(page);
            }
            if(value.equals("Order Hold")) {
                PlaywrightUtils.waitForMoreSec(40);
                PlaywrightUtils.refreshBrowser(page);
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Details", Constant.PAGE));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Details", Constant.PAGE));
                PlaywrightUtils.waitForMoreSec(2);
                PlaywrightUtils.refreshBrowser(page);
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Details", Constant.PAGE));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Details", Constant.PAGE));
                PlaywrightUtils.waitForMoreSec(2);
            }
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, "Get Field Value Locator", fieldName));
            Assert.assertEquals(value, PlaywrightUtils.getText(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, "Get Field Value Locator", fieldName)));
            Constant.result = "Value for '" + fieldName + "' field is '" + value + "' verified successfully";
            if(value.equals("Draft")) {
                Constant.quoteURL = Constant.PAGE.url();
                Constant.quoteNumber = PlaywrightUtils.getText(PlaywrightUtils.getElement("Quote Number Field", Constant.PAGE));
                ExtentCucumberAdapter.addTestStepLog("Quote Number: '"+ Constant.quoteNumber + "' created Successfully. ");
            }
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Updating Quantity and Term of a Subscription Product in Edit Quote page
     * @param productName - Name of Product
     * @param quantity - New Quantity
     * @param term - New Term
     */
    public static void updateSubsProductQuantityAndTerm(String quoteLineNumber, String productName, String quantity, String term) {
        try {   
            PlaywrightUtils.waitForSec();
            Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.waitForSec();
            List<Locator> list = PlaywrightUtils.getElement("Table Row", Constant.FRAME).all();
            Locator prod = list.get(Integer.parseInt(quoteLineNumber)-1);
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quantity", prod));
            PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Quantity", prod));
            PlaywrightUtils.clearAndSetValueUsingKeyboard(quantity, PlaywrightUtils.getElement("Quantity Field", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Term", prod));
            PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Term", prod));
            PlaywrightUtils.selectByLabelFromDropdown(term, PlaywrightUtils.getElement("Term Dropdown", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            Constant.cpqQuoteFields.get(quoteLineNumber).put("Quantity", quantity);
            Constant.cpqQuoteFields.get(quoteLineNumber).put("Term", term+".0");
            if((boolean) Constant.cpqQuoteFields.get(quoteLineNumber).get("IsTiered"))
                Constant.cpqQuoteFields.get(quoteLineNumber).put("UnitPrice", Constant.tierPricing.get(productName).get(term+".0"));
            Constant.resultList.add("'" + productName + "' product quantity updated to '" + quantity +"' and term updated to '" + term + "' successfully");
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Updating Quantity Term and Trade Discount of a Subscription Product in Edit Quote page
     * @param productName - Name of Product
     * @param quantity - New Quantity
     * @param term - New Term
     * @param tradeDiscount - TradeDiscount
     */
    public static void updateSubsProductQuantityTermAndTradeDiscount(String quoteLineNumber, String productName, String quantity, String term, String tradeDiscount) {
        try {   
            PlaywrightUtils.waitForSec();
            Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.waitForSec();
            List<Locator> list = PlaywrightUtils.getElement("Table Row", Constant.FRAME).all();
            Locator prod = list.get(Integer.parseInt(quoteLineNumber)-1);
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quantity", prod));
            PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Quantity", prod));
            PlaywrightUtils.clearAndSetValueUsingKeyboard(quantity, PlaywrightUtils.getElement("Quantity Field", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Term", prod));
            PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Term", prod));
            PlaywrightUtils.selectByLabelFromDropdown(term, PlaywrightUtils.getElement("Term Dropdown", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Trade Disc", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Trade Disc", prod));
            PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Trade Disc", prod));
            PlaywrightUtils.clearAndSetValueUsingKeyboard(tradeDiscount, PlaywrightUtils.getElement("Quantity Field", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            Constant.cpqQuoteFields.get(quoteLineNumber).put("Quantity", quantity);
            Constant.cpqQuoteFields.get(quoteLineNumber).put("Term", term+".0");
            Constant.cpqQuoteFields.get(quoteLineNumber).put("Trade Disc", tradeDiscount);
            if((boolean) Constant.cpqQuoteFields.get(quoteLineNumber).get("IsTiered"))
                Constant.cpqQuoteFields.get(quoteLineNumber).put("UnitPrice", Constant.tierPricing.get(productName).get(term+".0"));
            Constant.resultList.add("'" + productName + "' product quantity updated to '" + quantity +"' term updated to '" + term + "' Trade Discount updated to '" + tradeDiscount + "' successfully");
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Updating Quantity of a Non Subscription Product in Edit Quote page
     * @param productName - Name of Product
     * @param quantity - New Quantity
     * @param term - New Term
     */
    public static void updateNonSubsProductQuantity(String quoteLineNumber, String productName, String quantity) {
        try {
            PlaywrightUtils.waitForSec();
            Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.waitForSec();
            List<Locator> list = PlaywrightUtils.getElement("Table Row", Constant.FRAME).all();
            Locator prod = list.get(Integer.parseInt(quoteLineNumber)-1);
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quantity", prod));
            PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Quantity", prod));
            PlaywrightUtils.clearAndSetValueUsingKeyboard(quantity, PlaywrightUtils.getElement("Quantity Field", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            Constant.cpqQuoteFields.get(quoteLineNumber).put("Quantity", quantity);
            Constant.resultList.add("'" + productName + "' product quantity updated to '" + quantity +"' successfully");
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Updating Quantity and Trade Discount of a Non Subscription Product in Edit Quote page
     * @param productName - Name of Product
     * @param quantity - New Quantity
     * @param tradeDiscount - Trade Discount
     */
    public static void updateNonSubsProductQuantityAndTradeDiscount(String quoteLineNumber, String productName, String quantity, String tradeDiscount) {
        try {
            PlaywrightUtils.waitForSec();
            Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.waitForSec();
            List<Locator> list = PlaywrightUtils.getElement("Table Row", Constant.FRAME).all();
            Locator prod = list.get(Integer.parseInt(quoteLineNumber)-1);
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quantity", prod));
            PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Quantity", prod));
            PlaywrightUtils.clearAndSetValueUsingKeyboard(quantity, PlaywrightUtils.getElement("Quantity Field", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Trade Disc", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Trade Disc", prod));
            PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Trade Disc", prod));
            PlaywrightUtils.clearAndSetValueUsingKeyboard(tradeDiscount, PlaywrightUtils.getElement("Quantity Field", prod));
            PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            Constant.cpqQuoteFields.get(quoteLineNumber).put("Quantity", quantity);
            Constant.cpqQuoteFields.get(quoteLineNumber).put("Trade Disc", tradeDiscount);
            Constant.resultList.add("'" + productName + "' product quantity updated to '" + quantity +"' and Trade Discount updated to '" + tradeDiscount + "' successfully");
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to get SOQL Field Value from Constant
     * @param fieldName - Field Name you want extract from API Response
     * @return - Object having the value
     */
    public static Object getSOQLFieldValueFromAPIConstant(String fieldName) {
        if(Constant.recordsFromAPI.get(0).containsKey(fieldName))
            return Constant.recordsFromAPI.get(0).get(fieldName);
        else
            throw new CustomException(fieldName + " Field Value not returned from API");
    }
    
    /**
     * Method to Set Value on Multiple Element(Input Fields)
     * @param values - Values Array
     * @param fieldNames - Field Names Array
     */
    public static void setValueInMultipleFields(String[] values, String[] fieldNames) {
        try {
            if(values.length != fieldNames.length)
                throw new CustomException("Values and Fields count mismatch");
            for(int i=0; i<values.length; i++) {
                PlaywrightUtils.setValue(values[i].trim(), PlaywrightUtils.getElement(fieldNames[i].trim(), Constant.PAGE));
            }
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to Select Multiple Dropdowns
     * @param options - Option Array
     * @param fieldNames - Dropdown Names Array
     */
    public static void selectMultipleDropdowns(String[] options, String[] fieldNames) {
        try {
            if(options.length != fieldNames.length)
                throw new CustomException("Options and Fields count mismatch");
            for(int i=0; i<options.length; i++) {
                PlaywrightUtils.selectByLabelFromDropdown(options[i].trim(), PlaywrightUtils.getElement(fieldNames[i].trim(), Constant.PAGE));
            }
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Search and Select from Dropdown
     * @param value - Value to Select
     * @param field - Field Name
     */
    public static void searchAndSelect(String value, String field) {
        try {
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(field, Constant.PAGE));
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(field, Constant.PAGE));
            PlaywrightUtils.setValueUsingKeyboard(value, PlaywrightUtils.getElement(field, Constant.PAGE));
            PlaywrightUtils.waitForSec();
            PlaywrightUtils.keyPress("Backspace", Constant.PAGE);
            try {
            	PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            			"Get Account Name for Opportunity", value));  
                
            } catch (Exception e) {
                PlaywrightUtils.waitForSec();
                PlaywrightUtils.keyPress("Backspace", Constant.PAGE);
                PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            			"Get Account Name for Opportunity", value));
            }
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Search and Select Multiple Dropdown
     * @param values - Values to Select
     * @param fields - Field Names
     */
    public static void searchAndSelectMultipleDropdown(String[] values, String[] fields) {
        try {
            if(values.length != fields.length)
                throw new CustomException("Options and Fields count mismatch");
            for(int i=0; i<values.length; i++) {
                searchAndSelect(values[i].trim(), fields[i].trim());
            }
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }

    /**
     * Method to Select Multiple Products
     * @param products - Products to Select
     */
    public static void selectMultipleProducts() {
        try {
            String[] prodArr = Arrays.copyOf(CommonUtils.getProducts().toArray(), CommonUtils.getProducts().toArray().length, String[].class);
            Constant.productName = prodArr[0];
            Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            for(String product : prodArr) {
                PlaywrightUtils.waitForAnElement(Constant.FRAME.getByRole(AriaRole.TEXTBOX, new FrameLocator.GetByRoleOptions().setName("Search Products")));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(product.trim(), Constant.FRAME.getByRole(AriaRole.TEXTBOX, new FrameLocator.GetByRoleOptions().setName("Search Products")));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Search Button", PlaywrightUtils.getElement("Search Header", Constant.FRAME)));
//              PlaywrightUtils.click(Constant.FRAME.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("")));
                PlaywrightUtils.waitForSec();
                if(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", product.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())).all().size()>1)
                    PlaywrightUtils.click(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", product.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())).first());
                else
                    PlaywrightUtils.click(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", product.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())));
                if(prodArr[prodArr.length - 1].equals(product))
                    PlaywrightUtils.click(PlaywrightUtils.getElement("Select", PlaywrightUtils.getElement("Page Header", Constant.FRAME)));
                else    
                    PlaywrightUtils.click(PlaywrightUtils.getElement("Select & Add More", PlaywrightUtils.getElement("Page Header", Constant.FRAME)));
                PlaywrightUtils.waitForMoreSec(2);
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
                PlaywrightUtils.waitForSec();
                Constant.resultList.add("'"+ product +"' product selected successfully");
            }
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }       
    }
    
    /**
     * Method to round off double to 2 decimal places
     * @param number - input number
     * @return - round off number
     */
    public static double roundOffDoubleToTwoDecPlace(double number) {
        return Math.round(number * 100.0) / 100.0;
    }
    
    /**
     * Method to round off double to 2 decimal places
     * @param number - input number
     * @return - round off number
     */
    public static String roundOffDoubleToTwoDecPlaceToString(double number) {
        return String.valueOf(Math.round(number * 100.0) / 100.0);
    }
    
    /**
     * Method for Testing Connection with Salesforce through API
     */
    public static void testConnection() {
        
        int statusCode = APIUtils.getStatusCode();
        
        if(statusCode == 200) {
            System.out.println("Connection succesfull with Status Code " + statusCode);
        }else {
             throw new CustomException("Connection unsuccesfull with Status Code " + statusCode);
        }
    }
    
    /**
     * Method for Getting Field Value Salesforce through API by Given Table Fields and there Value
     */
    public static void getSOQLResultsFromAPIByGivenFieldsAndValuesInJSON(String[] fields, String tableName, String[] tableField, String[] tableValue) {
        
        if(tableField.length != tableValue.length)
            throw new CustomException("Table Fields and Table Values Count mismatch");
        else {
            String whereClause = "", fieldClause = "", temp = "";
            for(int i=0; i<tableField.length-1; i++) {
                temp = tableField[i] + " = '" + tableValue[i] + "' and ";
                whereClause += temp;
            }
            whereClause += tableField[tableField.length-1] + " = '" + tableValue[tableField.length-1] + "'";
            for(int j=0; j<fields.length-1; j++) {
                temp = fields[j] + ", ";
                fieldClause += temp;
            }
            fieldClause += fields[fields.length-1];
            
            APIUtils.getSoqlResultInJSON("SELECT "+ fieldClause +" from "+ tableName +" WHERE "+ whereClause);
        }
    }
    
    /**
     * Method for Verifying Field Value located under frame
     * @param value - Expected Value
     * @param fieldName - Field Name having Actual Value
     * @param page - Page having the Field
     * @param frame - FrameLocator
     */
    public static void verifyFieldValue(String value, String fieldName, String frame, Page page) {
        Constant.FRAME = PlaywrightUtils.getFrame(frame, Constant.PAGE);
        PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement(fieldName, Constant.FRAME));
        Assert.assertEquals(value, PlaywrightUtils.getText(PlaywrightUtils.getElement(fieldName, Constant.FRAME)));
    }
    
    
    /**
     * Method to generate Random Number of given Digits
     * @param digits - no. of digits
     * @return - Random Number as String
     */
    public static String generateRandomNumber(int digits) {
        if(digits ==0)
            throw new CustomException("0 digits of number cannot be generated");
        String number = "";
        String temp;
        Random random = new Random();   
        for(int i=0; i<digits; i++) {
            temp = String.valueOf(random.nextInt(10));
            if(i==0 && temp.equals("0"))
                temp = "1";
            number += temp;
        }
        return number;
    }
    
    /**
     * Method to logout from given user in salesforce
     * @param user - user name
     */
    public static void userLogout(String user) {
        try {
        	PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            		"Get User Logout button Locator", user));
            PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
            		"Get User Logout button Locator", user));
            PlaywrightUtils.waitForMoreSec(2);
            
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to get Products List from Products.json file
     * @return - List of Products
     */
    public static List<String> getProducts(){
        String name = "Name";
        List<String> prodList = new ArrayList<>(); 
        for(Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
            JSONObject productJSONObj = (JSONObject) productObj;
            prodList.add((String) productJSONObj.get(name));
        }
        return prodList;
    }
    
    /**
     * Method to get Quantity of given Product from Products.json
     * @param productName - Name of the Products
     * @return - quantity of the given product
     */
    public static String getProductQuantity(String quoteLineNumber){
        String name = "QuoteLineItemNumber";
        String quantity = "Quantity";
        for(Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
            JSONObject productJSONObj = (JSONObject) productObj;
            if(quoteLineNumber.equals((String) productJSONObj.get(name)))
                if(((String) productJSONObj.get(quantity)).matches("[0-9]+") || Integer.parseInt((String) productJSONObj.get(quantity)) > 0)
                    return(String) productJSONObj.get(quantity);
                else
                    throw new CustomException("You must define Quantity as a valid digit in Products.json for '" + quoteLineNumber +"' QuoteLine Item");
        }
        throw new CustomException("For '"+ quoteLineNumber +"' QuoteLine Item Quantity is not defined in Products.json file");
    }
    
    /**
     * Method to get Quantity of given Product from Products.json
     * @param productName - Name of the Products
     * @return - quantity of the given product
     */
    public static String getProductTerm(String quoteLineNumber){
        String name = "QuoteLineItemNumber";
        String term = "Term";
        for(Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
            JSONObject productJSONObj = (JSONObject) productObj;
            if(quoteLineNumber.equals((String) productJSONObj.get(name)))
                if(((String) productJSONObj.get(term)).matches("[0-9]+") || ((String) productJSONObj.get(term)).equals("NA"))
                    return(String) productJSONObj.get(term);
                else
                    throw new CustomException("You must define Term as a digit or 'NA' in Products.json for '" + quoteLineNumber +"' QuoteLine Item");
        }
        throw new CustomException("For '"+ quoteLineNumber +"' QuoteLine Item Term is not defined in Products.json file");
    }
    
    /**
     * Method to get Trade Discount of given Product from Products.json
     * @param productName - Name of the Products
     * @return - TradeDiscount of the given product
     */
    public static String getProductTradeDiscount(String quoteLineNumber){
        String name = "QuoteLineItemNumber";
        String tradeDiscount = "LineLevelTD";
        for(Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
            JSONObject productJSONObj = (JSONObject) productObj;
            if(quoteLineNumber.equals((String) productJSONObj.get(name)))
                if(((String) productJSONObj.get(tradeDiscount)).matches("[0-9]+") || ((String) productJSONObj.get(tradeDiscount)).equals("NA"))
                    return(String) productJSONObj.get(tradeDiscount);
                else
                    throw new CustomException("You must define LineLevelTD as a digit or 'NA' in Products.json for '" + quoteLineNumber +"' QuoteLine Item");
        }
        throw new CustomException("For '"+ quoteLineNumber +"' QuoteLine Item LineLevelTD is not defined in Products.json file");
    }

    /**
     * Method to update Quantity and Term(defined in Products.json file) in Edit Quote page
     */
    public static void updateQuantityAndTerm() {
        String quoteLineNumber;
        try {
            PlaywrightUtils.waitForMoreSec(2);
            for (Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
                org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
                quoteLineNumber = (String) productJSONObj.get("QuoteLineItemNumber");
                if((boolean) Constant.cpqQuoteFields.get(quoteLineNumber).get("IsSubscriptionProduct"))
                    updateSubsProductQuantityAndTerm(quoteLineNumber, (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Product"), (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Quantity"), ((String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Term")).replace(".0", ""));
                else
                    updateNonSubsProductQuantity(quoteLineNumber, (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Product"), (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Quantity"));
            }
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to update Quantity Term and Trade Discount(defined in Products.json file) in Edit Quote page
     */
    public static void updateQuantityTermAndTradeDiscount() {
        String quoteLineNumber;
        try {
            PlaywrightUtils.waitForMoreSec(2);
            for (Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
                org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
                quoteLineNumber = (String) productJSONObj.get("QuoteLineItemNumber");
                if((boolean) Constant.cpqQuoteFields.get(quoteLineNumber).get("IsSubscriptionProduct"))
                    updateSubsProductQuantityTermAndTradeDiscount(quoteLineNumber, (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Product"), (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Quantity"), ((String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Term")).replace(".0", ""), (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("LineLevelTD"));
                else
                    updateNonSubsProductQuantityAndTradeDiscount(quoteLineNumber, (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Product"), (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("Quantity"), (String)Constant.cpqQuoteFields.get(quoteLineNumber).get("LineLevelTD") );
            }
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to assert number values are same(0.05 or less difference will be ignored)
     * @param apiFields - API Fields
     * @param uiValues - UI Values
     */
    public static void assertEqualsForDecimalNumbersWithSlightDifference(String[] apiFields, String[] uiValues) {
        if (apiFields.length != uiValues.length)
            throw new CustomException("Array count mismatch");
        else {
            float firstNumber, secondNumber;
            for (int i = 0; i < apiFields.length; i++) {
                firstNumber = Float.parseFloat(String.valueOf(APIUtils.getSOQLFieldValueFromJSONConstant(apiFields[i])));
                secondNumber = Float.parseFloat(uiValues[i]);
                if(firstNumber > secondNumber ) {
                    if(!(firstNumber - secondNumber <= 0.051)) {
                        System.out.println(firstNumber - secondNumber);
                        throw new CustomException("Comparison fialure. Actual:"+firstNumber+ " and Expected:"+secondNumber);
                    }
                }else if(firstNumber < secondNumber ){
                    if(!(secondNumber - firstNumber <= 0.051)) {
                        System.out.println(secondNumber - firstNumber);
                        throw new CustomException("Comparison fialure. Actual:"+firstNumber+ " and Expected:"+secondNumber);
                    }
                }
            }
            String uiValueStr = " ", apiValueStr = " ";
            for(int j = 0; j < apiFields.length-1; j++ ) {
                uiValueStr += apiFields[j] + " - '" + uiValues[j] + "', ";
                apiValueStr += apiFields[j] + " - '" + String.valueOf(APIUtils.getSOQLFieldValueFromJSONConstant(apiFields[j])) + "', ";                
            }
            uiValueStr += apiFields[apiFields.length-1] + " - '" + uiValues[apiFields.length-1] + "'";
            apiValueStr += apiFields[apiFields.length-1] + " - '" + String.valueOf(APIUtils.getSOQLFieldValueFromJSONConstant(apiFields[apiFields.length-1])) + "'";
            Constant.actualResult = uiValueStr;
            Constant.expectedResult = apiValueStr;
        }
    }
    
    /**
     * Method to get difference b/w Dates(in Months)
     * @param earlier - Earlier Date in YYYY-MM-DD
     * @param later - Later Date in YYYY-MM-DD
     * @return - Difference in Months and Days(Months#Days)
     */
    public static String getDifferenceBetweenDates(String earlier, String later) {

        later = CommonUtils.addDays(later, 1);
        LocalDate earlierFullDate = LocalDate.parse(earlier);
        LocalDate laterFullDate = LocalDate.parse(later);
        Period d = Period.between(earlierFullDate, laterFullDate);
        if(d.getYears() <= 0 && d.getMonths() <= 0 && d.getDays() <= 0) 
            throw new CustomException("Invalid Dates(Required format: 'yyyy-MM-dd') '" + earlier + "' and '" + later + "'");
        int months = 0;
        if(d.getYears() > 0)
            months = d.getYears() * 12 + d.getMonths();
        else
            months = d.getMonths();
        return months+"#"+d.getDays();
    }
    
    /**
     * Method to update Quantity and Term(defined in Products.json file) in Edit Quote page
     */
    public static void updateEffectiveQuantEndDateAndStartDate() {
        try {
            PlaywrightUtils.waitForMoreSec(2);
            for (Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
                org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
                if(((String) productJSONObj.get("Action")).contains("Update"))
                    updateEffectiveQuantEndDateAndStartDate((String) productJSONObj.get("QuoteLineItemNumber"), (String) productJSONObj.get("EffectiveQuantity"), (String) productJSONObj.get("StartDate"), (String) productJSONObj.get("EndDate"));
            }   
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to update Quantity Term and Trade Discount(defined in Products.json file) in Edit Quote page
     */
    public static void updateEffectiveQuantEndDateStartDateAndTradeDiscount() {
        try {
            PlaywrightUtils.waitForMoreSec(2);
            for (Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
                org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
                if(((String) productJSONObj.get("Action")).contains("Update") && ((String) productJSONObj.get("Action")).contains("Renewal"))
                	updateEffectiveQuantEndDateStartDateAndTradeDiscount((String) productJSONObj.get("QuoteLineItemNumber"), (String) productJSONObj.get("EffectiveQuantity"), (String) productJSONObj.get("StartDate"), (String) productJSONObj.get("EndDate"),(String) productJSONObj.get("LineLevelTD"));
                if(((String) productJSONObj.get("Action")).contains("Update") && ((String) productJSONObj.get("Action")).contains("Amend"))
                	updateEffectiveQuantEndDateAndStartDate((String) productJSONObj.get("QuoteLineItemNumber"), (String) productJSONObj.get("EffectiveQuantity"), (String) productJSONObj.get("StartDate"), (String) productJSONObj.get("EndDate"));
            }   
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Updating Effective Quantity, Start Date and End Date of an Existing Quote Line in Edit Quote page
     * @param quoteLineNumber -  Quote Line Number
     * @param effectiveQuantity - Effective Quantity
     * @param startDate - New Start Date
     * @param endDate - New End Date
     */
    public static void updateEffectiveQuantEndDateAndStartDate(String quoteLineNumber, String effectiveQuantity, String startDate, String endDate) {
        try {   
            PlaywrightUtils.waitForSec();
            Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.waitForSec();
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            List<Locator> list = PlaywrightUtils.getElement("Table Row", Constant.FRAME).all();
            Locator prod = list.get(Integer.parseInt(quoteLineNumber)-1);
            if(!effectiveQuantity.equalsIgnoreCase("NA")) {
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Quantity", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quantity", prod));
                PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Quantity", prod));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(effectiveQuantity, PlaywrightUtils.getElement("Quantity Field", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
//              Constant.contractFields.get(quoteLineNumber).put("EffectiveQuantity", effectiveQuantity);

            }
            if(!startDate.equalsIgnoreCase("NA")) {
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Start Date", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Start Date", prod));
                PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Start Date", prod));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(startDate, PlaywrightUtils.getElement("Start Date Field", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
//              Constant.contractFields.get(quoteLineNumber).put("StartDate", startDate);
            }
            if(!endDate.equalsIgnoreCase("NA")) {
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("End Date", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("End Date", prod));
                PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("End Date", prod));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(endDate, PlaywrightUtils.getElement("End Date Field", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
//              Constant.contractFields.get(quoteLineNumber).put("EndDate", endDate);
            }
            Constant.resultList.add("Quote Line '" + quoteLineNumber + "' Effective Quantity '" + effectiveQuantity +"', Start Date '" + startDate + "', End Date '"+endDate+"'");
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Updating Effective Quantity, Start Date, End Date and Trade Discount of an Existing Quote Line in Edit Quote page
     * @param quoteLineNumber -  Quote Line Number
     * @param effectiveQuantity - Effective Quantity
     * @param startDate - New Start Date
     * @param endDate - New End Date
     * @param tradeDiscount - Trade discount value
     */
    public static void updateEffectiveQuantEndDateStartDateAndTradeDiscount(String quoteLineNumber, String effectiveQuantity, String startDate, String endDate, String tradeDiscount) {
        try {   
            PlaywrightUtils.waitForSec();
            Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.waitForSec();
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            List<Locator> list = PlaywrightUtils.getElement("Table Row", Constant.FRAME).all();
            Locator prod = list.get(Integer.parseInt(quoteLineNumber)-1);
            if(!effectiveQuantity.equalsIgnoreCase("NA")) {
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Quantity", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quantity", prod));
                PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Quantity", prod));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(effectiveQuantity, PlaywrightUtils.getElement("Quantity Field", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
//              Constant.contractFields.get(quoteLineNumber).put("EffectiveQuantity", effectiveQuantity);

            }
            if(!startDate.equalsIgnoreCase("NA")) {
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Start Date", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Start Date", prod));
                PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Start Date", prod));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(startDate, PlaywrightUtils.getElement("Start Date Field", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
//              Constant.contractFields.get(quoteLineNumber).put("StartDate", startDate);
            }
            if(!endDate.equalsIgnoreCase("NA")) {
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("End Date", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("End Date", prod));
                PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("End Date", prod));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(endDate, PlaywrightUtils.getElement("End Date Field", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
//              Constant.contractFields.get(quoteLineNumber).put("EndDate", endDate);
            }
            if(!tradeDiscount.equalsIgnoreCase("NA")) {
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Trade Disc", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Trade Disc", prod));
                PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Trade Disc", prod));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(tradeDiscount, PlaywrightUtils.getElement("Quantity Field", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            }
            Constant.resultList.add("Quote Line '" + quoteLineNumber + "' Effective Quantity '" + effectiveQuantity +"', Start Date '" + startDate + "', End Date '"+ endDate +"' and Trade Discount '"+ tradeDiscount + "'");
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method for Updating Trade Discount of an Existing Quote Line in Edit Quote page
     * @param quoteLineNumber -  Quote Line Number
     * @param tradeDiscount - Trade Discount vale
     */
    public static void updateTradeDiscountOnQuoteLine(String quoteLineNumber, String tradeDiscount) {
        try {   
            PlaywrightUtils.waitForSec();
            Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
            PlaywrightUtils.waitForSec();
            PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            List<Locator> list = PlaywrightUtils.getElement("Table Row", Constant.FRAME).all();
            Locator prod = list.get(Integer.parseInt(quoteLineNumber)-1);
            if(!tradeDiscount.equalsIgnoreCase("NA")) {
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Trade Disc", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Trade Disc", prod));
                PlaywrightUtils.doubleClick(PlaywrightUtils.getElement("Trade Disc", prod));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(tradeDiscount, PlaywrightUtils.getElement("Quantity Field", prod));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
            }
            Constant.resultList.add("Quote Line '" + quoteLineNumber + "' Trade Discount '" + tradeDiscount +"'");
        }catch(Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to add/update New Quote Line for Renew or Amend
     */
    public static void addNewQuoteLineForRenewOrAmend() {
        try {
            PlaywrightUtils.waitForMoreSec(2);
            addQuoteLines();
//          selectMultipleProducts();
            PlaywrightUtils.waitForSec();
            for (Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
                org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
                if(((String) productJSONObj.get("Action")).contains("Add"))
                    updateEffectiveQuantEndDateAndStartDate((String) productJSONObj.get("QuoteLineItemNumber"), (String) productJSONObj.get("EffectiveQuantity"), (String) productJSONObj.get("StartDate"), (String) productJSONObj.get("EndDate"));
            }   
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to add/update New Quote Line for Renew or Amend with Trade Discount
     */
    public static void addNewQuoteLineForRenewOrAmendWithTradeDisc() {
        try {
            PlaywrightUtils.waitForMoreSec(2);
            addQuoteLines();
//          selectMultipleProducts();
            PlaywrightUtils.waitForSec();
            for (Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
                org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
                if(((String) productJSONObj.get("Action")).contains("Add") && ((String) productJSONObj.get("Action")).contains("Renewal"))
                	updateEffectiveQuantEndDateStartDateAndTradeDiscount((String) productJSONObj.get("QuoteLineItemNumber"), (String) productJSONObj.get("EffectiveQuantity"), (String) productJSONObj.get("StartDate"), (String) productJSONObj.get("EndDate"), (String) productJSONObj.get("LineLevelTD"));
                if(((String) productJSONObj.get("Action")).contains("Add") && ((String) productJSONObj.get("Action")).contains("Amend"))
                	updateEffectiveQuantEndDateStartDateAndTradeDiscount((String) productJSONObj.get("QuoteLineItemNumber"), (String) productJSONObj.get("EffectiveQuantity"), (String) productJSONObj.get("StartDate"), (String) productJSONObj.get("EndDate"), (String) productJSONObj.get("LineLevelTD"));
            }   
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to Add New Quote Lines for Amend/Renewal Flow
     */
    public static void addQuoteLines() {
        try {
            String productName;
            List<String> products = new ArrayList<>();
            for (Object productObj : (JSONArray)Constant.productsJsonObj.get(Constant.flowName)) {
                org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
                if(((String) productJSONObj.get("Action")).contains("Add"))
                    products.add((String) productJSONObj.get("Product"));
            }
            for (int i=0; i<products.size(); i++) {
                productName = products.get(i);
                PlaywrightUtils.waitForAnElement(Constant.FRAME.getByRole(AriaRole.TEXTBOX, new FrameLocator.GetByRoleOptions().setName("Search Products")));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(productName.trim(), Constant.FRAME.getByRole(AriaRole.TEXTBOX, new FrameLocator.GetByRoleOptions().setName("Search Products")));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Search Button", PlaywrightUtils.getElement("Search Header", Constant.FRAME)));
//              PlaywrightUtils.click(Constant.FRAME.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("")));
                PlaywrightUtils.waitForSec();
//              PlaywrightUtils.click(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", productName.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())));
                if(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", productName.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())).all().size()>1)
                    PlaywrightUtils.click(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", productName.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())).first());
                else
                    PlaywrightUtils.click(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", productName.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())));
                if(products.size()-1 == i)
                    PlaywrightUtils.click(PlaywrightUtils.getElement("Select", PlaywrightUtils.getElement("Page Header", Constant.FRAME)));
                else    
                    PlaywrightUtils.click(PlaywrightUtils.getElement("Select & Add More", PlaywrightUtils.getElement("Page Header", Constant.FRAME)));
                PlaywrightUtils.waitForMoreSec(2);
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
                PlaywrightUtils.waitForSec();
                Constant.resultList.add("'"+ productName +"' product selected successfully");
            }
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
     * Method to Add New Quote Lines for Amend/Renewal Flow without flow
     */
    public static void addQuoteLines(String product) {
        try {
            
                String productName = product;
                PlaywrightUtils.waitForAnElement(Constant.FRAME.getByRole(AriaRole.TEXTBOX, new FrameLocator.GetByRoleOptions().setName("Search Products")));
                PlaywrightUtils.clearAndSetValueUsingKeyboard(productName.trim(), Constant.FRAME.getByRole(AriaRole.TEXTBOX, new FrameLocator.GetByRoleOptions().setName("Search Products")));
                PlaywrightUtils.click(PlaywrightUtils.getElement("Search Button", PlaywrightUtils.getElement("Search Header", Constant.FRAME)));
//              PlaywrightUtils.click(Constant.FRAME.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("")));
                PlaywrightUtils.waitForSec();
//              PlaywrightUtils.click(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", productName.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())));
                if(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", productName.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())).all().size()>1)
                    PlaywrightUtils.click(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", productName.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase())).first());
                else
                    PlaywrightUtils.click(Constant.FRAME.locator(String.format("sb-table-row[name='%s']", productName.trim())).locator(Constant.locatorsMap.get("Select Product".replaceAll(" ", "_").toLowerCase()))); 
                PlaywrightUtils.click(PlaywrightUtils.getElement("Select", PlaywrightUtils.getElement("Page Header", Constant.FRAME)));
                PlaywrightUtils.waitForMoreSec(2);
                PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Quote Number", Constant.FRAME));
                PlaywrightUtils.waitForSec();
                Constant.resultList.add("'"+ productName +"' product selected successfully");
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
    
    /**
	 * Method for Store Login
	 */
	public static void storelogin() {
		try {
			NavigateOptions options = new NavigateOptions();
			options.timeout = (double) 300000;
			Constant.PAGE.navigate(FileReaderManager.getInstance().getConfigReader().getStoreUrl(), options);
//			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Login", Constant.PAGE));
//			PlaywrightUtils.click(PlaywrightUtils.getElement("Login", Constant.PAGE));
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Username Store", Constant.PAGE));
			String Username = FileReaderManager.getInstance().getConfigReader().getStoreUsername();
			String Password = FileReaderManager.getInstance().getConfigReader().getStorePassword();
			PlaywrightUtils.clearAndSetValue(Username, PlaywrightUtils.getElement("Username Store", Constant.PAGE));
			PlaywrightUtils.clearAndSetValueUsingKeyboard(Password, PlaywrightUtils.getElement("Password Store", Constant.PAGE));
			PlaywrightUtils.click(PlaywrightUtils.getElement("Login Store", Constant.PAGE));
			Constant.result = "Logged in Successfully";
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to assert number values are same(0.05 or less difference will be ignored)
	 * @param apiFields - API Fields
	 * @param uiValues - UI Values
	 */
	public static void assertEqualsForDecimalNumbersWithSlightDifference(String first, String second) {
		try {
			double firstNumber = Double.parseDouble(first), secondNumber = Double.parseDouble(second);
			if(firstNumber > secondNumber ) {
					if(!(firstNumber - secondNumber <= 0.051)) {
						System.out.println(firstNumber - secondNumber);
						throw new CustomException("Comparison fialure. Actual:"+firstNumber+ " and Expected:"+secondNumber);
					}
			}else if(firstNumber < secondNumber ){
				if(!(secondNumber - firstNumber <= 0.051)) {
					System.out.println(secondNumber - firstNumber);
					throw new CustomException("Comparison fialure. Actual:"+firstNumber+ " and Expected:"+secondNumber);
				}
			}
		} catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method for Re-Initialize the CPQ Field and Calculation field values to 0
	 */
	public static void reInitializeTheCpqFieldValues() {
		try {
			Constant.comments = "";
			Constant.cpqRegularAmount = "0";
			Constant.cpqNetAmount = "0"; 
			Constant.cpqPartnerDiscount = "0";
			Constant.cpqPartnerDiscAmount = "0";
			Constant.cpqTotalDiscount = "0";
			Constant.cpqTotalDiscountAmount = "0";
			Constant.cpqMRR = "0";
			Constant.cpqARR = "0";
			Constant.cpqACV = "0"; Constant.cpqNetNewACV = "0";
			Constant.cpqTCVOneTime = "0";
			Constant.cpqTCVRecurring = "0"; 
			Constant.cpqTCV = "0";
			Constant.contractAccountId = "0";
			Constant.contractBillTo = "0";
			Constant.contractSoldTo = "0";
			Constant.contract = "0"; 
			Constant.contractEndUser = "0";
			Constant.contractDealId = "0";
			Constant.contractRenewalOpp = "0";
			Constant.contractCurrency ="0"; 
			Constant.contractPriceBookId = "0";
			Constant.contractStartDate = "0";
			Constant.contractEndDate = "0";
			Constant.contractShipTo = "0";
			Constant.contractTerm ="0";
			Constant.discount ="0";
			Constant.unitPrice ="0"; 
			Constant.subsTerm ="0";
			Constant.quoteNumber ="0"; 
			Constant.tradeDiscount ="0"; 
			Constant.poNumber ="0";
			Constant.orderId ="0"; 
			Constant.contractId ="0"; 
			Constant.flowName ="0"; 
			Constant.contractNumber ="0"; 
			Constant.renewContractNumber ="0";
			Constant.renewOrderNumber ="0";
			Constant.renewOrderId ="0";
			Constant.orderProductListTotal = "0";
			Constant.orderRequestPartnerDiscount = "0";
			Constant.orderRequestOrderTotalTD = "0";
			Constant.orderRequestAdditionalTD = "0";
			Constant.orderRequestOrderWeight = "0";
			Constant.partnerDiscounts.clear();
			Constant.orderProductList.clear();
			Constant.orderProductSalesAmountList.clear();
			Constant.orderProductQuantityList.clear();
			Constant.cpqQuoteFields.clear();
			Constant.contractFields.clear();
			Constant.tierPricing.clear();		
		} catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to get any Random number from Given Range
	 * @param lowerLimit - Lower Value
	 * @param upperLimit - Upper Value(This Number is included)
	 * @return - Generated Random Number
	 */
	public static int getRandomNumberFromGivenRange(int lowerLimit, int upperLimit) {
		Random random = new Random();
		return random.nextInt(upperLimit - lowerLimit + 1) + lowerLimit;
	}
	
	/**
	 * Method for searching Contact in Salesforce
	 * @param name - Contact Name
	 */
	public static void searchContactInSalesforce(String name) {
		try {
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Search Box Salesforce", Constant.PAGE));
			PlaywrightUtils.waitForSec();
			PlaywrightUtils.click(PlaywrightUtils.getElement("Search Box Salesforce", Constant.PAGE));
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Search Box Input Salesforce", Constant.PAGE));
			PlaywrightUtils.clearAndSetValueUsingKeyboard(name, PlaywrightUtils.getElement("Search Box Input Salesforce", Constant.PAGE));
			PlaywrightUtils.keyPress("Enter", Constant.PAGE);
			selectContactInSalesforce(name);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	/**
	 * Method for Selecting Contact
	 * @param name - Contact Name
	 */
	public static void selectContactInSalesforce(String name) {
		try {
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElement("Contact Option", Constant.PAGE));
			PlaywrightUtils.click(PlaywrightUtils.getElement("Contact Option", Constant.PAGE));			
			PlaywrightUtils.waitForSec();
			PlaywrightUtils.waitForAnElement(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
					"Get Entity by Name", name));
			PlaywrightUtils.click(PlaywrightUtils.getElementByDynamicXpath(Constant.PAGE, 
					"Get Entity by Name", name));
			Constant.result = "'" + name + "' Contact selected successfully";
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	/**
	 * Method to wait for an element to display with retry mechanism after some interval
	 * @param value - Expected Value
	 */
	public static void waitForElementToDisplayWithRetry(String fieldName) {
		try {
			boolean flag = false;
			String usefulFieldName = fieldName.toLowerCase().replaceAll(" ", "_");
			for(int i=0; i<20; i++) {
				WaitForOptions options = new WaitForOptions();
				options.timeout = (double) 120000;
				options.state = WaitForSelectorState.ATTACHED;
				PlaywrightUtils.getElement(usefulFieldName, Constant.PAGE).waitFor(options);
				if(PlaywrightUtils.getAllElements(usefulFieldName, Constant.PAGE).size() > 0) {
					flag = true;
					break;
				} 
				else {
					PlaywrightUtils.waitForMoreSec(5);
					PlaywrightUtils.refreshBrowser(Constant.PAGE);
				}
			}
			if(!flag)
				throw new CustomException("'" + fieldName +"' is not displayed after waiting for 5 mins");
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
}


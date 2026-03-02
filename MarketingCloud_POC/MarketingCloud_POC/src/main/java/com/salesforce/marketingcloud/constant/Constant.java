package com.salesforce.marketingcloud.constant;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.salesforce.marketingcloud.enums.FlowType;

public class Constant {
	
	private Constant() {}
	
	/** 
	 * Constant having Playwright which can be used globally
	 */
	public static Playwright PLAYWRIGHT;
	
	/** 
	 * Constant having BrowserContext which can be used globally
	 */
	public static BrowserContext BROWSERCONTEXT;
	
	/** 
	 * Constant having Browser which can be used globally
	 */
	public static Browser BROWSER;
	
	/** 
	 * Constant having Page which can be used globally
	 */
	public static Page PAGE;
	
	/** 
	 * Constant having FrameLocator which can be used globally
	 */
	public static FrameLocator FRAME;
	
	/** 
	 * Constant having Config properties path
	 */
	public static final String PROPERTYFILEPATH = "src/test/resources/TestData/testproperties.properties";
	
	/** 
	 * Constant having Spark HTML report path
	 */
	public static final String REPORTPATH = "test-output/HtmlReport/HtmlReport.html";
	
	/** 
	 * Constant having TestData excel path
	 */
	public static final String DATAFILEPATH = "src/test/resources/TestData/TestData.xlsx";
	
	/** 
	 * Constant having Products json path
	 */
	public static final String PRODUCTSJSONPATH = "src/test/resources/TestData/Products.json";
	
	/** 
	 * Constant having Products json path
	 */
	public static final String SFAuthFileJSONPath = "src/test/resources/TestData/SF_Auth.json";
	
	/** 
	 * Constant Map having Locators keys and values
	 */
	public static HashMap<String, String> locatorsMap = new HashMap<>();
	
	/** 
	 * Constant having Locators file path
	 */
	public static final String LOCATORSFILEPATH = "src/test/resources/Locators/locators.properties";
		
	/** 
	 * Constant having Base URL used after Trimming RecordID from current URL
	 */
	public static String baseURL;
	
	/** 
	 * Constant used to store runtime Records
	 */
	public static String orderRequest, endUserName, endUserId, recordId, accountName, currencyISOCode, 
		cartName, accountCountry, accountStreet, accountCity, accountPostalCode, accountState, 
		accountFullCountryName, accountFullStateName, accountRegion;
	
	/**
	 * Constant used to Store Values in Map of Maps for Comparison
	 */
	public static Map<String, Map<String, String>> comparisonMap = new HashMap<String, Map<String, String>>();
	
	/**
	 * Constant used to Store API Response
	 */
	public static List<Map<String, Object>> recordsFromAPI;
	
	/** 
	 * Constant used to store format used for pricing
	 */
	public static final DecimalFormat decfor = new DecimalFormat("0.00");
	
	/** 
	 * Constant used to store Record URLs
	 */
	public static String oppURL, quoteURL, quoteStatus, productRuleError;
	
	/** 
	 * Constant having expected field values in Map
	 */
	public static HashMap<String, String> expectedValuesMap = new HashMap<>();

	/** 
	 * Constant having expected and actual value to add in report
	 */
	public static String actualResult, expectedResult, result;
	
	/** 
	 * Constant having expected and actual value to add in report
	 */
	public static List<String> resultList = new ArrayList<>();
	
	/**
	 * Constant having API Values in JSON Object
	 */
	public static JSONObject recordsJSON;
	
	/**
	 * Constant having API Values in JSON Object
	 */
	public static JSONArray recordsJSONArray;
	
	/**
	 * Constants used to dynamic values
	 */
    public static String discount, unitPrice, subsTerm, quoteNumber, tradeDiscount, poNumber, cartId, orderId, orderName, contractId, flowName, contractNumber, renewContractNumber,
    				renewOrderNumber, renewOrderId, quoteId, salesOrderId, comments = "", renewalContractId, renewalContractNumber, renewalQuoteId, renewalOrderId;

	/**
	 * Constants used to store prices of Products based on tiers
	 */
	public static Map<String, Map<String, String>> tierPricing = new HashMap<String, Map<String, String>>();
	
	/**
	 * Constants used to store prices of Products based on tiers
	 */
	public static Map<String, Map<String, String>> oneDayPricing = new HashMap<String, Map<String, String>>();
	
	/**
	 * Constant used to Product Store Prices
	 */
	public static Map<String, Map<String, Object>> cpqQuoteFields = new HashMap<String, Map<String, Object>>();
	
	/**
	 * Constant used to Product Store Prices
	 */
	public static Map<String, Map<String, Object>> contractFields = new HashMap<String, Map<String, Object>>();
	
	/**
	 * Constant used to store CPQ Quote Field Values
	 */
	public static String cpqRegularAmount = "0", cpqNetAmount = "0", cpqPartnerDiscount = "0", 
			cpqPartnerDiscAmount = "0",	cpqTotalDiscount = "0", cpqTotalDiscountAmount = "0", cpqMRR = "0", 
			cpqARR = "0", cpqACV = "0", cpqNetNewACV = "0", cpqTCVOneTime = "0", cpqTCVRecurring = "0", 
			cpqTCV = "0";
	
	/**
	 * Constant used to store Environment
	 */
	public static String env;
	
	/**
	 * Constant having Products after Reading from Products.json
	 */
	public static org.json.simple.JSONObject productsJsonObj, uniqueRroductsJsonObj, duplicateRroductsJsonObj;
	
	/**
	 * List for storing Approver user present on Quote Record
	 */
	public static List<String> Quote_Approver_Id = new ArrayList<String>();
	
	/**
	 * List for storing the Names of the Approver users present on Quote Record
	 */
	public static List<String> Quote_Approver_Name = new ArrayList<String>();
	
	/**
	 * List for storing the Approver Names present on Approval Instance
	 */
	public static List<String> Approver_Instance = new ArrayList<String>();
	
	/**
	 * List for storing Approver user for Renewal Quote
	 */
	public static List<String> Renewal_Quote_Approver = new ArrayList<String>();
	
	/**
	 * Constant used to store dynamic values
	 */
	public static String accountId, contactId, contactName, productName, productId, caseId, orderRequestId, orderRequestNumber,
		orderNumber, orderProductListTotal = "0", orderRequestPartnerDiscount = "0", orderRequestOrderTotalTD = "0",
		orderRequestAdditionalTD = "0", orderRequestOrderWeight = "0", parsedDomainForOwnerEmail,
		parsedDomainForPrimaryEmail, contactEmail, contactPrimaryEmail, orderReferenceNumber, totalAmount = "0",
		totalQuantity = "0";

	/**
	 * Constant used to store Contract Fields
	 */
	public static String contractAccountId, contractBillTo, contractSoldTo, contract, contractEndUser,
						 contractDealId, contractRenewalOpp, contractCurrency, contractPriceBookId,
						 contractStartDate, contractEndDate, contractShipTo, contractTerm, defaultTerm;
	
	/**
	 * Constant used to store ECommerce UI Fields
	*/
	public static String ecommMSRPSubtotal = "0", ecommDiscountedSubtotal = "0", ecommTotal = "0",
			ecommParterDiscAmount = "0", ecommAdditionalDiscAmount = "0";
	
	/**
	 * Constant used to store Address Values
	*/
	public static String billToAddressName, billToAddress1, billToCity, billToCountry, billToPostalCode, 
		billToState, endUser, endUserAddress1, endUserCity, endUserCountry, endUserPostalCode, endUserState,
		shipToAddressName, shipToAddress1, shipToCity, shipToCountry, shipToPostalCode, shipToState;
	
	/**
	 * Constant used to store Order Product List
	*/
	public static List<String> orderProductList = new ArrayList<String>();
	
	/**
	 * Constant used to store Order Product Sales Amount in a List
	*/
	public static List<String> orderProductSalesAmountList = new ArrayList<String>();
	
	/**
	 * Constant used to store Order Product Quantity in a List
	*/
	public static List<String> orderProductQuantityList = new ArrayList<String>();
	
	/**
	 * Constant used to store Order Product Type in a List
	*/
	public static List<String> orderProductTypeList = new ArrayList<String>();
	
	/**
	 * Constant used to store Partner Discount for All the Product Entitlements based on Brand
	 */
	public static Map<String, Map<String, Object>> partnerDiscounts = new HashMap<String, Map<String, Object>>();
	
	/**
	 * Constant used to enable Write in Excel
	 */
	public static boolean writingInExcel = false;
	
	/**
	 * Constant used to store various statuses
	 */
	public static final String activeStatus = "Active", newStatus = "New", approvedStatus = "Approved", renewableSubscriptionType = "Renewable", activatedStatus = "Activated",
			closedStatus = "Closed", pendingDeleteStatus = "PendingDelete", amendStatus = "Amend", renewalStatus = "Renewal";
	
	/**
	 * Constant used to Store Product Prices
	 */
	public static Map<String, Map<String, Object>> productSpecs = new HashMap<String, Map<String, Object>>();
	
	/**
	 * Constant used to Store type of Flow
	 */
	public static FlowType flow;
	
	/**
	 * Latest email HTML fetched from Mailosaur
	 */
	public static String latestEmailHtml;
}

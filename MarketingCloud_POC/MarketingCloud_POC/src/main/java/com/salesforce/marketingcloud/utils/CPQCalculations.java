package com.salesforce.marketingcloud.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Locator;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.exception.CustomException;

public class CPQCalculations {

    private static final Logger LOG = LoggerFactory.getLogger(CPQCalculations.class);

    /**
     * Method to calculate Product price based on Term
     * 
     * @param products - Products
     */
	public static void calculateProductPrice(String flow) {
		String productId, disScheduleId = null, subsType, subsTerm = null, unitPrice, oneDayPrice = null,
				quoteLineNumber, product;
		boolean isSubsProduct, isTiered;
		for (Object productObj : (JSONArray) Constant.productsJsonObj.get(flow)) {
			org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
			quoteLineNumber = (String) productJSONObj.get("QuoteLineItemNumber");
			product = (String) productJSONObj.get("Name");
			CommonUtils.addKeyValueInGivenMap("Product", product, quoteLineNumber, Constant.cpqQuoteFields);
			APIUtils.getSoqlResultInJSON(String.format("SELECT Id, SBQQ__SubscriptionType__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
					product));
			productId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
			subsType = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionType__c");
			APIUtils.getSoqlResultInJSON(String.format(
					"SELECT Id, Pricebook2.Name, Name, UnitPrice FROM PricebookEntry WHERE ProductCode = '%s' and CurrencyIsoCode ='"
							+ Constant.currencyISOCode + "' and Pricebook2.Id != '01s400000006WFgAAM'",
					product));
			unitPrice = APIUtils.getSOQLFieldValueFromJSONConstant("UnitPrice");
			if (subsType.equalsIgnoreCase("One-time")) {
				isSubsProduct = false;
				CommonUtils.addKeyValueInGivenMap("IsSubscriptionProduct", isSubsProduct, quoteLineNumber,
						Constant.cpqQuoteFields);
			} else {
				isSubsProduct = true;
				CommonUtils.addKeyValueInGivenMap("IsSubscriptionProduct", isSubsProduct, quoteLineNumber,
						Constant.cpqQuoteFields);
				APIUtils.getSoqlResultInJSON(String.format(
						"SELECT Id, SBQQ__SubscriptionTerm__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
						product));
				subsTerm = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionTerm__c");
				CommonUtils.addKeyValueInGivenMap("Term", subsTerm, quoteLineNumber, Constant.cpqQuoteFields);
				oneDayPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
						(double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm)));
				CommonUtils.addKeyValueInGivenMap("OneDayPrice", oneDayPrice, quoteLineNumber, Constant.cpqQuoteFields);
			}
			try {
				APIUtils.getSoqlResultInJSON(String.format(
						"SELECT Id FROM SBQQ__DiscountSchedule__c WHERE SBQQ__Product__r.Id = '%s'", productId));
				disScheduleId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
				isTiered = true;
			} catch (Exception e) {
				isTiered = false;
			}
			if (!isTiered) {
				CommonUtils.addKeyValueInGivenMap("IsTiered", isTiered, quoteLineNumber, Constant.cpqQuoteFields);
				CommonUtils.addKeyValueInGivenMap("UnitPrice", unitPrice, quoteLineNumber, Constant.cpqQuoteFields);
			} else {
				CommonUtils.addKeyValueInGivenMap("IsTiered", isTiered, quoteLineNumber, Constant.cpqQuoteFields);
			}
			if (isTiered) {
				APIUtils.getSoqlResultInJSONForMultipleRecords(String.format(
						"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '%s'",
						disScheduleId));
				JSONObject json;
				for (int i = 0; i < Constant.recordsJSONArray.length(); i++) {
					json = Constant.recordsJSONArray.getJSONObject(i);
					double monthlyPrice = (double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm));
					double lowerBound = Double.parseDouble(
							APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json));
					String price = String.valueOf((monthlyPrice * lowerBound) - ((monthlyPrice * lowerBound
							* Double.parseDouble(
									APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__Discount__c", json)))
							/ 100));
					String oneDayPriceTermBased = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
							((monthlyPrice * lowerBound) - ((monthlyPrice * lowerBound
									* Double.parseDouble(APIUtils
											.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__Discount__c", json)))
									/ 100)) / lowerBound);
					if (Constant.tierPricing.containsKey(product))
						Constant.tierPricing.get(product).put(
								APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json),
								price);
					else {
						Map<String, String> map = new HashMap<String, String>();
						map.put(APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json),
								price);
						Constant.tierPricing.put(product, map);
					}
					if (Constant.oneDayPricing.containsKey(product))
						Constant.oneDayPricing.get(product).put(
								APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json),
								oneDayPriceTermBased);
					else {
						Map<String, String> map = new HashMap<String, String>();
						map.put(APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json),
								oneDayPriceTermBased);
						Constant.oneDayPricing.put(product, map);
					}
				}
				Constant.cpqQuoteFields.get(quoteLineNumber).put("DefaultPrice",
						Constant.tierPricing.get(product).get(subsTerm));
			}
			if (CommonUtils.getProductQuantity(quoteLineNumber).matches("[0-9]+"))
				Constant.cpqQuoteFields.get(quoteLineNumber).put("Quantity",
						CommonUtils.getProductQuantity(quoteLineNumber));
			if (CommonUtils.getProductTerm(quoteLineNumber).matches("[0-9]+"))
				Constant.cpqQuoteFields.get(quoteLineNumber).put("Term",
						CommonUtils.getProductTerm(quoteLineNumber) + ".0");
		}
	}
	
	/**
	 * Method to calculate Product price based on Term with Trade Discount
	 * @param products - Products
	 */
	public static void calculateProductPriceWithTradeDisc(String flow) {
		String productId, disScheduleId = null, subsType, subsTerm = null, unitPrice, oneDayPrice = null,
				quoteLineNumber, product;
		boolean isSubsProduct, isTiered;
		for (Object productObj : (JSONArray) Constant.productsJsonObj.get(flow)) {
			org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
			quoteLineNumber = (String) productJSONObj.get("QuoteLineItemNumber");
			product = (String) productJSONObj.get("Name");
			CommonUtils.addKeyValueInGivenMap("Product", product, quoteLineNumber, Constant.cpqQuoteFields);
			APIUtils.getSoqlResultInJSON(String.format("SELECT Id, SBQQ__SubscriptionType__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
					product));
			productId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
			subsType = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionType__c");
			APIUtils.getSoqlResultInJSON(String.format(
					"SELECT Id, Pricebook2.Name, Name, UnitPrice FROM PricebookEntry WHERE ProductCode = '%s' and CurrencyIsoCode ='"
							+ Constant.currencyISOCode + "' and Pricebook2.Id != '01s400000006WFgAAM'",
					product));
			unitPrice = APIUtils.getSOQLFieldValueFromJSONConstant("UnitPrice");
			if (subsType.equalsIgnoreCase("One-time")) {
				isSubsProduct = false;
				CommonUtils.addKeyValueInGivenMap("IsSubscriptionProduct", isSubsProduct, quoteLineNumber,
						Constant.cpqQuoteFields);
			} else {
				isSubsProduct = true;
				CommonUtils.addKeyValueInGivenMap("IsSubscriptionProduct", isSubsProduct, quoteLineNumber,
						Constant.cpqQuoteFields);
				APIUtils.getSoqlResultInJSON(String.format(
						"SELECT Id, SBQQ__SubscriptionTerm__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
						product));
				subsTerm = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionTerm__c");
				CommonUtils.addKeyValueInGivenMap("Term", subsTerm, quoteLineNumber, Constant.cpqQuoteFields);
				oneDayPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
						(double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm)));
				CommonUtils.addKeyValueInGivenMap("OneDayPrice", oneDayPrice, quoteLineNumber, Constant.cpqQuoteFields);
			}
			try {
				APIUtils.getSoqlResultInJSON(String.format(
						"SELECT Id FROM SBQQ__DiscountSchedule__c WHERE SBQQ__Product__r.Id = '%s'", productId));
				disScheduleId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
				isTiered = true;
			} catch (Exception e) {
				isTiered = false;
			}
			if (!isTiered) {
				CommonUtils.addKeyValueInGivenMap("IsTiered", isTiered, quoteLineNumber, Constant.cpqQuoteFields);
				CommonUtils.addKeyValueInGivenMap("UnitPrice", unitPrice, quoteLineNumber, Constant.cpqQuoteFields);
			} else {
				CommonUtils.addKeyValueInGivenMap("IsTiered", isTiered, quoteLineNumber, Constant.cpqQuoteFields);
			}
			if (isTiered) {
				APIUtils.getSoqlResultInJSONForMultipleRecords(String.format(
						"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '%s'",
						disScheduleId));
				JSONObject json;
				for (int i = 0; i < Constant.recordsJSONArray.length(); i++) {
					json = Constant.recordsJSONArray.getJSONObject(i);
					double monthlyPrice = (double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm));
					double lowerBound = Double.parseDouble(
							APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json));
					String price = String.valueOf((monthlyPrice * lowerBound) - ((monthlyPrice * lowerBound
							* Double.parseDouble(
									APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__Discount__c", json)))
							/ 100));
					String oneDayPriceTermBased = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
							((monthlyPrice * lowerBound) - ((monthlyPrice * lowerBound
									* Double.parseDouble(APIUtils
											.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__Discount__c", json)))
									/ 100)) / lowerBound);
					if (Constant.tierPricing.containsKey(product))
						Constant.tierPricing.get(product).put(
								APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json),
								price);
					else {
						Map<String, String> map = new HashMap<String, String>();
						map.put(APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json),
								price);
						Constant.tierPricing.put(product, map);
					}
					if (Constant.oneDayPricing.containsKey(product))
						Constant.oneDayPricing.get(product).put(
								APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json),
								oneDayPriceTermBased);
					else {
						Map<String, String> map = new HashMap<String, String>();
						map.put(APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json),
								oneDayPriceTermBased);
						Constant.oneDayPricing.put(product, map);
					}
				}
				Constant.cpqQuoteFields.get(quoteLineNumber).put("DefaultPrice",
						Constant.tierPricing.get(product).get(subsTerm));
			}
			if (CommonUtils.getProductQuantity(quoteLineNumber).matches("[0-9]+"))
				Constant.cpqQuoteFields.get(quoteLineNumber).put("Quantity",
						CommonUtils.getProductQuantity(quoteLineNumber));
			if (CommonUtils.getProductTerm(quoteLineNumber).matches("[0-9]+"))
				Constant.cpqQuoteFields.get(quoteLineNumber).put("Term",
						CommonUtils.getProductTerm(quoteLineNumber) + ".0");
			if (CommonUtils.getProductTradeDiscount(quoteLineNumber).matches("[0-9]+"))
				Constant.cpqQuoteFields.get(quoteLineNumber).put("LineLevelTD",
						CommonUtils.getProductTradeDiscount(quoteLineNumber));
		}
	}

	/**
	 * Method to calculate price after discount
	 * @param price    - actual price
	 * @param discount - discount
	 * @return - price after discount
	 */
	public static double caculatePriceAfterDiscount(double price, double discount) {
		return (price - ((price * discount) / 100));
	}

	/**
	 * Method to calculate discount
	 * @param price    - actual price
	 * @param discount - discount
	 * @return - discounted price
	 */
	public static double caculateDiscount(double price, double discount) {
		return (price * discount) / 100;
	}

	/**
	 * Method to calculate CPQ Quote Field Values
	 */
	public static void calculateCPQQuoteFieldValues() {
		Map<String, Object> temp = null;
		double term = 0, quantity = 0, unitPrice = 0, cpqRegularAmount = 0, cpqPartnerDiscAmount = 0, cpqNetAmount = 0,
				cpqMRR = 0, cpqARR = 0, cpqTCVRecurring = 0, cpqTCVOneTime = 0;
		String product, quoteLineNumber;
		double discount = Double.parseDouble(Constant.discount.replace("%", ""));
		for (Object productObj : (JSONArray) Constant.productsJsonObj.get(Constant.flowName)) {
			org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
			quoteLineNumber = (String) productJSONObj.get("QuoteLineItemNumber");
			temp = Constant.cpqQuoteFields.get(quoteLineNumber);
			product = (String) temp.get("Product");
			System.out.println(product);
			for (Entry<String, Object> entry1 : temp.entrySet())
				System.out.println(entry1.getKey() + "  " + entry1.getValue());
			if ((boolean) temp.get("IsSubscriptionProduct"))
				term = Double.parseDouble((String) temp.get("Term"));
			quantity = Double.parseDouble((String) temp.get("Quantity"));
			if ((boolean) temp.get("IsTiered")) {
				unitPrice = Double.parseDouble(Constant.tierPricing.get(product).get(temp.get("Term")));
				Constant.cpqQuoteFields.get(String.valueOf(quoteLineNumber)).put("RegularPrice", unitPrice);
			} else
				unitPrice = Double.parseDouble((String) temp.get("UnitPrice"));
			if ((boolean) temp.get("IsSubscriptionProduct")) {
				cpqRegularAmount = CommonUtils.roundOffDoubleToTwoDecPlace(unitPrice * quantity);
				cpqPartnerDiscAmount = CommonUtils
						.roundOffDoubleToTwoDecPlace(caculateDiscount(cpqRegularAmount, discount));
				cpqNetAmount = CommonUtils.roundOffDoubleToTwoDecPlace(cpqRegularAmount - cpqPartnerDiscAmount);
				cpqMRR = cpqNetAmount / term;
				cpqARR = CommonUtils.roundOffDoubleToTwoDecPlace(12 * cpqMRR);
				cpqTCVRecurring = cpqNetAmount;
			} else {
				cpqRegularAmount = CommonUtils.roundOffDoubleToTwoDecPlace(unitPrice * quantity);
				cpqPartnerDiscAmount = CommonUtils
						.roundOffDoubleToTwoDecPlace(caculateDiscount(cpqRegularAmount, discount));
				cpqNetAmount = CommonUtils.roundOffDoubleToTwoDecPlace(cpqRegularAmount - cpqPartnerDiscAmount);
				cpqTCVOneTime = cpqNetAmount;
			}
			Constant.cpqRegularAmount = String.valueOf(CommonUtils
					.roundOffDoubleToTwoDecPlace(Double.parseDouble(Constant.cpqRegularAmount) + cpqRegularAmount));
			Constant.cpqPartnerDiscAmount = String.valueOf(CommonUtils.roundOffDoubleToTwoDecPlace(
					Double.parseDouble(Constant.cpqPartnerDiscAmount) + cpqPartnerDiscAmount));
			Constant.cpqNetAmount = String.valueOf(
					CommonUtils.roundOffDoubleToTwoDecPlace(Double.parseDouble(Constant.cpqNetAmount) + cpqNetAmount));
			Constant.cpqTCVOneTime = String.valueOf(CommonUtils
					.roundOffDoubleToTwoDecPlace(Double.parseDouble(Constant.cpqTCVOneTime) + cpqTCVOneTime));
			Constant.cpqPartnerDiscount = String.valueOf(CommonUtils.roundOffDoubleToTwoDecPlace(discount));
			Constant.cpqMRR = String.valueOf(Double.parseDouble(Constant.cpqMRR) + cpqMRR);
			Constant.cpqARR = String
					.valueOf(CommonUtils.roundOffDoubleToTwoDecPlace(Double.parseDouble(Constant.cpqARR) + cpqARR));
			Constant.cpqTCVRecurring = String.valueOf(CommonUtils
					.roundOffDoubleToTwoDecPlace(Double.parseDouble(Constant.cpqTCVRecurring) + cpqTCVRecurring));
			cpqRegularAmount = 0;
			cpqPartnerDiscAmount = 0;
			cpqNetAmount = 0;
			cpqMRR = 0;
			cpqARR = 0;
			cpqTCVRecurring = 0;
			cpqTCVOneTime = 0;
		}
		Constant.cpqMRR = String.valueOf(CommonUtils.roundOffDoubleToTwoDecPlace(Double.parseDouble(Constant.cpqMRR)));
		Constant.cpqTotalDiscount = Constant.cpqPartnerDiscount;
		Constant.cpqTotalDiscountAmount = Constant.cpqPartnerDiscAmount;
		Constant.cpqACV = Constant.cpqARR;
		Constant.cpqNetNewACV = Constant.cpqARR;
		Constant.cpqTCV = Constant.cpqNetAmount;
		Constant.resultList.add("System calculated values");
		Constant.resultList.add("Regular Amount: " + Constant.cpqRegularAmount);
		Constant.resultList.add("Net Amount: " + Constant.cpqNetAmount);
		Constant.resultList.add("Total Discount: " + Constant.cpqTotalDiscount);
		Constant.resultList.add("Total Discount Amount: " + Constant.cpqTotalDiscountAmount);
		Constant.resultList.add("Parter Discoun: " + Constant.cpqPartnerDiscount);
		Constant.resultList.add("Parter Discount Amount: " + Constant.cpqPartnerDiscAmount);
		Constant.resultList.add("MRR: " + Constant.cpqMRR);
		Constant.resultList.add("ARR: " + Constant.cpqARR);
		Constant.resultList.add("ACV: " + Constant.cpqACV);
		Constant.resultList.add("Net New ACV: " + Constant.cpqNetNewACV);
		Constant.resultList.add("TCV(One-Time): " + Constant.cpqTCVOneTime);
		Constant.resultList.add("TCV(Recurring): " + Constant.cpqTCVRecurring);
		Constant.resultList.add("TCV: " + Constant.cpqTCV);
	}

	/**
	 * Method to Verify Product Prices done by CPQ
	 */
	public static void verifyProductPrices() {
		String product;
		Map<String, Object> tempMap = null;
		double netUnitPrice, netTotalPrice, unitPrice, quantity;
		double discount = Double.parseDouble(Constant.discount.replace("%", ""));
		String actual = "Actual :- ", expected = "Expected :- ", quoteLineNumber;
		Constant.resultList.add("Validating System calculated values with UI values");
		try {
			for (Object productObj : (JSONArray) Constant.productsJsonObj.get(Constant.flowName)) {
				org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
				quoteLineNumber = (String) productJSONObj.get("QuoteLineItemNumber");
				tempMap = Constant.cpqQuoteFields.get(quoteLineNumber);
				product = (String) tempMap.get("Product");
				unitPrice = Double.parseDouble((String) tempMap.get("UnitPrice"));
				quantity = Double.parseDouble((String) tempMap.get("Quantity"));
				netUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlace(caculatePriceAfterDiscount(unitPrice, discount));
				netTotalPrice = CommonUtils.roundOffDoubleToTwoDecPlace(netUnitPrice * quantity);
				Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
				List<Locator> list = PlaywrightUtils.getElement("Table Row", Constant.FRAME).all();
				Locator temp = list.get(Integer.parseInt(quoteLineNumber) - 1);
				Constant.resultList.add("Values for : '" + product + "' product");
				Assert.assertEquals((String) tempMap.get("Quantity") + ".00",
						PlaywrightUtils.getText(PlaywrightUtils.getElement("Quantity", temp)));
				expected += "Quantity :'" + (String) tempMap.get("Quantity") + ".00" + "' ";
				actual += "Quantity :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Quantity", temp)) + "' ";
				if ((boolean) tempMap.get("IsSubscriptionProduct") && (boolean) tempMap.get("IsTiered")) {
					Assert.assertEquals(((String) tempMap.get("Term")).replace(".0", ""),
							PlaywrightUtils.getText(PlaywrightUtils.getElement("Term", temp)));
					expected += "Term :'" + ((String) (tempMap.get("Term"))).replace(".0", "") + "' ";
					actual += "Term :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Term", temp)) + "' ";
//                       Assert.assertEquals(CommonUtils.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble((String) tempMap.get("DefaultPrice"))), PlaywrightUtils.getText(PlaywrightUtils.getElement("List Unit Price", temp)).replace(",", "").trim().split(" ")[1]);
//                         expected += "List Unit Price :'" + CommonUtils.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble((String) tempMap.get("DefaultPrice"))) + "' ";
//                         actual += "List Unit Price :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("List Unit Price", temp)).replace(",", "").trim().split(" ")[1] + "' ";
				} else if ((boolean) tempMap.get("IsSubscriptionProduct") && !(boolean) tempMap.get("IsTiered")) {
					Assert.assertEquals(((String) tempMap.get("Term")).replace(".0", ""),
							PlaywrightUtils.getText(PlaywrightUtils.getElement("Term", temp)));
					expected += "Term :'" + ((String) (tempMap.get("Term"))).replace(".0", "") + "' ";
					actual += "Term :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Term", temp)) + "' ";
//                       Assert.assertEquals(CommonUtils.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble((String) tempMap.get("UnitPrice"))), PlaywrightUtils.getText(PlaywrightUtils.getElement("List Unit Price", temp)).replace(",", "").trim().split(" ")[1]);
//                         expected += "List Unit Price :'" + CommonUtils.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble((String) tempMap.get("UnitPrice"))) + "' ";
//                         actual += "List Unit Price :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("List Unit Price", temp)).replace(",", "").trim().split(" ")[1] + "' ";
				} else {
//                       Assert.assertEquals(CommonUtils.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble((String) tempMap.get("UnitPrice"))), PlaywrightUtils.getText(PlaywrightUtils.getElement("List Unit Price", temp)).replace(",", "").trim().split(" ")[1]);
//                         expected += "List Unit Price :'" + CommonUtils.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble((String) tempMap.get("UnitPrice"))) + "' ";
//                         actual += "List Unit Price :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("List Unit Price", temp)).replace(",", "").trim().split(" ")[1] + "' ";
				}
				Assert.assertEquals(
						CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								Double.parseDouble((String) tempMap.get("UnitPrice"))),
						PlaywrightUtils.getText(PlaywrightUtils.getElement("Regular Unit Price", temp)).replace(",", "")
								.trim().split(" ")[1]);
				expected += "Regular Unit Price :'" + CommonUtils.roundOffDoubleToTwoDecPlaceToString(
						Double.parseDouble((String) tempMap.get("UnitPrice"))) + "' ";
				actual += "Regular Unit Price :'"
						+ PlaywrightUtils.getText(PlaywrightUtils.getElement("Regular Unit Price", temp))
								.replace(",", "").trim().split(" ")[1]
						+ "' ";
				if (!Constant.discount.equals("0.00%")) {
					Assert.assertEquals(
							CommonUtils.roundOffDoubleToTwoDecPlaceToString(
									Double.parseDouble((String) Constant.discount)) + "%",
							PlaywrightUtils.getText(PlaywrightUtils.getElement("Total Disc", temp)));
					expected += "Total Disc :'" + CommonUtils.roundOffDoubleToTwoDecPlaceToString(
							Double.parseDouble((String) Constant.discount)) + "%" + "' ";
					actual += "Total Disc :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Total Disc", temp))
							+ "' ";
					Assert.assertEquals(
							CommonUtils.roundOffDoubleToTwoDecPlaceToString(
									Double.parseDouble((String) Constant.discount)),
							PlaywrightUtils.getText(PlaywrightUtils.getElement("Partner Disc", temp)).replace("%", ""));
					expected += "Partner Disc :'" + CommonUtils
							.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble((String) Constant.discount)) + "' ";
					actual += "Partner Disc :'"
							+ PlaywrightUtils.getText(PlaywrightUtils.getElement("Partner Disc", temp)) + "' ";
				} else {
					Assert.assertEquals(Constant.discount + "%",
							PlaywrightUtils.getText(PlaywrightUtils.getElement("Total Disc", temp)));
					expected += "Total Disc :'" + Constant.discount + "%" + "' ";
					actual += "Total Disc :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Total Disc", temp))
							+ "' ";
					Assert.assertEquals(Constant.discount,
							PlaywrightUtils.getText(PlaywrightUtils.getElement("Partner Disc", temp)).replace("%", ""));
					expected += "Partner Disc :'" + CommonUtils
							.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble((String) Constant.discount)) + "' ";
					actual += "Partner Disc :'"
							+ PlaywrightUtils.getText(PlaywrightUtils.getElement("Partner Disc", temp)).replace("%", "")
							+ "' ";
                }
                // end of loop over products
            }
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
}

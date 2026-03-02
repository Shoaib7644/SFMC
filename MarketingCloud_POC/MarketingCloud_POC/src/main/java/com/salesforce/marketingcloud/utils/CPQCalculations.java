package com.salesforce.marketingcloud.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.junit.Assert;

import com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter;
import com.microsoft.playwright.Locator;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.exception.CustomException;

public class CPQCalculations {

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
					expected += "Partner Disc :'" + Constant.discount + "' ";
					actual += "Partner Disc :'"
							+ PlaywrightUtils.getText(PlaywrightUtils.getElement("Partner Disc", temp)).replace("%", "")
							+ "' ";
				}
				Assert.assertEquals(
						CommonUtils
								.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble(String.valueOf(netUnitPrice))),
						PlaywrightUtils.getText(PlaywrightUtils.getElement("Net Unit Price", temp)).replace(",", "")
								.trim().split(" ")[1]);
				expected += "Net Unit Price :'" + CommonUtils
						.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble(String.valueOf(netUnitPrice))) + "' ";
				actual += "Net Unit Price :'"
						+ PlaywrightUtils.getText(PlaywrightUtils.getElement("Net Unit Price", temp)).replace(",", "")
								.trim().split(" ")[1]
						+ "' ";
				Assert.assertEquals(
						CommonUtils
								.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble(String.valueOf(netTotalPrice))),
						PlaywrightUtils.getText(PlaywrightUtils.getElement("Net Total", temp)).replace(",", "").trim()
								.split(" ")[1]);
				expected += "Net Total :'" + CommonUtils
						.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble(String.valueOf(netTotalPrice))) + "' ";
				actual += "Net Total :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Net Total", temp))
						.replace(",", "").trim().split(" ")[1] + "' ";
				if ((boolean) tempMap.get("IsSubscriptionProduct") && (boolean) tempMap.get("IsTiered"))
					Constant.cpqQuoteFields.get(quoteLineNumber).put("APIListPrice",
							CommonUtils.roundOffDoubleToTwoDecPlaceToString(
									Double.parseDouble((String) tempMap.get("DefaultPrice"))));
				else
					Constant.cpqQuoteFields.get(quoteLineNumber).put("APIListPrice",
							CommonUtils.roundOffDoubleToTwoDecPlaceToString(
									Double.parseDouble((String) tempMap.get("UnitPrice"))));
				Constant.cpqQuoteFields.get(quoteLineNumber).put("APINetTotal", CommonUtils
						.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble(String.valueOf(netTotalPrice))));
				Constant.cpqQuoteFields.get(quoteLineNumber).put("APIUnitPrice", CommonUtils
						.roundOffDoubleToTwoDecPlaceToString(Double.parseDouble(String.valueOf(netUnitPrice))));
				Constant.resultList.add(expected);
				Constant.resultList.add(actual);
				expected = "Actual :- ";
				actual = "Expected :- ";
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	/**
	 * Method to decide Term based on given Months for a Product
	 * 
	 * @param product - Product Name
	 */
	public static String decideTerm(String product, String startDate, String endDate) {
		boolean isTiered;
		String disScheduleId = null, productId, subsTerm = null, unitPrice;
		List<Integer> tiers = new ArrayList<>();
		product = product.trim();
		APIUtils.getSoqlResultInJSON(String.format(
				"SELECT Id, SBQQ__SubscriptionType__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
				product));
		productId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
		APIUtils.getSoqlResultInJSON(String.format(
				"SELECT Id, Pricebook2.Name, Name, UnitPrice FROM PricebookEntry WHERE ProductCode = '%s' and CurrencyIsoCode ='"
						+ Constant.currencyISOCode + "' and Pricebook2.Id != '01s400000006WFgAAM'",
				product));
		unitPrice = APIUtils.getSOQLFieldValueFromJSONConstant("UnitPrice");
		try {
			APIUtils.getSoqlResultInJSON(String
					.format("SELECT Id FROM SBQQ__DiscountSchedule__c WHERE SBQQ__Product__r.Id = '%s'", productId));
			disScheduleId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
			isTiered = true;
		} catch (Exception e) {
			isTiered = false;
			subsTerm = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionTerm__c");
		}
		if (isTiered) {
			APIUtils.getSoqlResultInJSONForMultipleRecords(String.format(
					"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '%s'",
					disScheduleId));
			JSONObject json;
			for (int i = 0; i < Constant.recordsJSONArray.length(); i++) {
				json = Constant.recordsJSONArray.getJSONObject(i);
				double lowerBound = Double
						.parseDouble(APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json));
				tiers.add((int) lowerBound);
				double monthlyPrice = (double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm));
				String price = String.valueOf((monthlyPrice * lowerBound) - ((monthlyPrice * lowerBound
						* Double.parseDouble(
								APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__Discount__c", json)))
						/ 100));
				String oneDayPriceTermBased = CommonUtils
						.roundOffDoubleToTwoDecPlaceToString(((monthlyPrice * lowerBound) - ((monthlyPrice * lowerBound
								* Double.parseDouble(APIUtils
										.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__Discount__c", json)))
								/ 100)) / lowerBound);
				if (Constant.tierPricing.containsKey(product))
					Constant.tierPricing.get(product).put(
							APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json), price);
				else {
					Map<String, String> map = new HashMap<String, String>();
					map.put(APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json), price);
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
			Collections.sort(tiers);
			int diff = Integer.parseInt(CommonUtils.getDifferenceBetweenDates(startDate, endDate).split("#")[0]);
			double decidedTier = 0;
			for (int tier : tiers) {
				if (tier <= diff)
					decidedTier = tier;
			}
			subsTerm = String.valueOf(decidedTier);
		}
		return subsTerm;
	}

	/**
	 * Method to read whole Contract by given Contract Number
	 * 
	 * @param contractNumber - Contract Number
	 */
	public static void readContract(String contractNumber) {
		try {
			ExtentCucumberAdapter.addTestStepLog("--- Reading the Contract : '" + contractNumber + "'---");
			double discount = Double.parseDouble(Constant.discount.replace("%", ""));
			String product, startDate, endDate, listPrice, quantity, quoteLineNumber, totalPrice, licenseKey,
					quoteLineEndDate, quoteLineStartDate, subsTerm = null, productId, disScheduleId = null,
					unitPrice = null, term, prorateMultiplier = null, price, regularUnitPrice, subsType = null,
					proratePrice = null, regularTotal = null, netUnitPrice = null, netTotal = null;
			List<Integer> tiers;
			boolean isTiered;
			APIUtils.getSoqlResultInJSON(String.format(
					"Select AccountId, Bill_To__c, Sold_To__c, End_Customer_Account__c, Sales_Deal_Id__c, SBQQ__RenewalOpportunity__c, CurrencyIsoCode, StartDate, EndDate, Pricebook2Id, Ship_To_Address__c, ContractTerm from Contract where SM_RecordType__c = true and ContractNumber = '%s'",
					contractNumber));
			Constant.contractAccountId = APIUtils.getSOQLFieldValueFromJSONConstant("AccountId");
			Constant.contractBillTo = APIUtils.getSOQLFieldValueFromJSONConstant("Bill_To__c");
			Constant.contractSoldTo = APIUtils.getSOQLFieldValueFromJSONConstant("Sold_To__c");
			Constant.contractEndUser = APIUtils.getSOQLFieldValueFromJSONConstant("End_Customer_Account__c");
			Constant.contractDealId = APIUtils.getSOQLFieldValueFromJSONConstant("Sales_Deal_Id__c");
			Constant.contractRenewalOpp = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__RenewalOpportunity__c");
			Constant.contractCurrency = APIUtils.getSOQLFieldValueFromJSONConstant("CurrencyIsoCode");
			Constant.contractStartDate = APIUtils.getSOQLFieldValueFromJSONConstant("StartDate");
			Constant.contractEndDate = APIUtils.getSOQLFieldValueFromJSONConstant("EndDate");
			Constant.contractPriceBookId = APIUtils.getSOQLFieldValueFromJSONConstant("Pricebook2Id");
			Constant.contractShipTo = APIUtils.getSOQLFieldValueFromJSONConstant("Ship_To_Address__c");
			Constant.contractTerm = APIUtils.getSOQLFieldValueFromJSONConstant("ContractTerm");
			APIUtils.getSoqlResultInJSONForMultipleRecords(String.format(
					"Select Id, SBQQ__Product__r.Name, SBQQ__StartDate__c, SBQQ__EndDate__c, SBQQ__ListPrice__c, Subscription_Key__c, SBQQ__Quantity__c, License_Key__c, SBQQ__ProrateMultiplier__c from SBQQ__Subscription__c where SBQQ__Contract__r.ContractNumber = '%s'",
					contractNumber));
			quoteLineEndDate = CommonUtils.addMonths(Constant.contractEndDate, Integer.parseInt(Constant.contractTerm));
			JSONObject json;
			org.json.JSONArray array = Constant.recordsJSONArray;
			for (int i = 0; i < array.length(); i++) {
				quoteLineNumber = String.valueOf(i + 1);
				json = array.getJSONObject(i);
				tiers = new ArrayList<>();
				product = APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__Product__r.Name", json);
				try {
					APIUtils.getSoqlResultInJSON(String.format(
							"Select id, SBQQ__RenewalProduct__r.Name from Product2 where RecordType.Name='Material product' and IsActive = true and SM_Product__c = true and SBQQ__Component__c != true and Name = '%s'",
							product));
					product = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__RenewalProduct__r.Name");
				} catch (Exception e) {
				}
				startDate = APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__StartDate__c", json);
				endDate = APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__EndDate__c", json);
				quoteLineStartDate = CommonUtils.addDays(endDate, 1);
				listPrice = APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__ListPrice__c", json);
				quantity = APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__Quantity__c", json);
				licenseKey = APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("License_Key__c", json);
				CommonUtils.addKeyValueInGivenMap("Product", product, quoteLineNumber, Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("StartDate", startDate, quoteLineNumber, Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("EndDate", endDate, quoteLineNumber, Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("ListPrice", listPrice, quoteLineNumber, Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("Quantity", quantity, quoteLineNumber, Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("LicenseKey", licenseKey, quoteLineNumber, Constant.contractFields);
				totalPrice = String.valueOf((double) (Double.parseDouble(listPrice) * Double.parseDouble(quantity)));
				CommonUtils.addKeyValueInGivenMap("TotalPrice", totalPrice, quoteLineNumber, Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("QuoteLineStartDate", quoteLineStartDate, quoteLineNumber,
						Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("QuoteLineEndDate", quoteLineEndDate, quoteLineNumber,
						Constant.contractFields);
				APIUtils.getSoqlResultInJSON(String.format(
						"SELECT Id, SBQQ__SubscriptionType__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
						product));
				productId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
				subsType = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionType__c");
				APIUtils.getSoqlResultInJSON(String.format(
						"SELECT Id, Pricebook2.Name, Name, UnitPrice FROM PricebookEntry WHERE ProductCode = '%s' and CurrencyIsoCode ='"
								+ Constant.contractCurrency + "' and Pricebook2.Id != '01s400000006WFgAAM'",
						product));
				unitPrice = APIUtils.getSOQLFieldValueFromJSONConstant("UnitPrice");
				if (!subsType.equalsIgnoreCase("One-time")) {
					APIUtils.getSoqlResultInJSON(String.format(
							"SELECT Id, SBQQ__SubscriptionTerm__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
							product));
					subsTerm = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionTerm__c");
				}
				try {
					APIUtils.getSoqlResultInJSON(String.format(
							"SELECT Id FROM SBQQ__DiscountSchedule__c WHERE SBQQ__Product__r.Id = '%s'", productId));
					disScheduleId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
					CommonUtils.addKeyValueInGivenMap("DiscountScheduleId", disScheduleId, quoteLineNumber,
							Constant.contractFields);
					isTiered = true;
				} catch (Exception e) {
					isTiered = false;
				}
				if (isTiered) {
					APIUtils.getSoqlResultInJSONForMultipleRecords(String.format(
							"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '%s'",
							disScheduleId));
					JSONObject jSON;
					org.json.JSONArray tierArray = Constant.recordsJSONArray;
					for (int j = 0; j < tierArray.length(); j++) {
						jSON = tierArray.getJSONObject(j);
						double lowerBound = Double.parseDouble(
								APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", jSON));
						tiers.add((int) lowerBound);
					}
					Collections.sort(tiers);
					int diff = Integer.parseInt(
							CommonUtils.getDifferenceBetweenDates(quoteLineStartDate, quoteLineEndDate).split("#")[0]);
					double decidedTier = 0;
					for (int tier : tiers) {
						if (tier <= diff)
							decidedTier = tier;
					}
					term = String.valueOf(decidedTier);
					prorateMultiplier = calculateProRateMultiplier(quoteLineStartDate, quoteLineEndDate, subsTerm);
					CommonUtils.addKeyValueInGivenMap("ProRateMultiplier", prorateMultiplier, quoteLineNumber,
							Constant.contractFields);
					double monthlyPrice = (double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm));
					APIUtils.getSoqlResultInJSON(
							"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '"
									+ disScheduleId + "' and SBQQ__LowerBound__c = " + subsTerm);
					price = String
							.valueOf((monthlyPrice * Double.parseDouble(subsTerm))
									- ((monthlyPrice * Double.parseDouble(subsTerm)
											* Double.parseDouble(
													APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__Discount__c")))
											/ 100));
					APIUtils.getSoqlResultInJSON(
							"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '"
									+ disScheduleId + "' and SBQQ__LowerBound__c = " + term);
					proratePrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
							(double) (Double.parseDouble(prorateMultiplier) * Double.parseDouble(price)));
					regularUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
							(double) (Double.parseDouble(proratePrice) - ((Double.parseDouble(proratePrice) * Double
									.parseDouble(APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__Discount__c")))
									/ 100)));
				} else {
					regularUnitPrice = unitPrice;
				}
				regularTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
						(double) (Double.parseDouble(regularUnitPrice) * Double.parseDouble(quantity)));
				netUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
						(double) (caculatePriceAfterDiscount(Double.parseDouble(regularUnitPrice), discount)));
				netTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
						(double) (Double.parseDouble(netUnitPrice) * Double.parseDouble(quantity)));
				CommonUtils.addKeyValueInGivenMap("RegularUnitPrice", regularUnitPrice, quoteLineNumber,
						Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("RegularTotal", regularTotal, quoteLineNumber,
						Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("NetUnitPrice", netUnitPrice, quoteLineNumber,
						Constant.contractFields);
				CommonUtils.addKeyValueInGivenMap("NetTotal", netTotal, quoteLineNumber, Constant.contractFields);
				ExtentCucumberAdapter
						.addTestStepLog("Exisiting Quote Line(Default Pricing) : '" + quoteLineNumber + "'");
				ExtentCucumberAdapter.addTestStepLog("Quantity : " + quantity);
				ExtentCucumberAdapter.addTestStepLog("StartDate : " + startDate);
				ExtentCucumberAdapter.addTestStepLog("EndDate : " + endDate);
				ExtentCucumberAdapter.addTestStepLog("Quote Line Start Date(During Renewal) : " + quoteLineStartDate);
				ExtentCucumberAdapter.addTestStepLog("Quote Line End Date(During Renewal) : " + quoteLineEndDate);
				if (isTiered) {
					ExtentCucumberAdapter.addTestStepLog("Prorate Multiplier : " + prorateMultiplier);
					ExtentCucumberAdapter.addTestStepLog("Prorate Price : " + proratePrice);
				}
				ExtentCucumberAdapter.addTestStepLog("Regular Unit Price : " + regularUnitPrice);
				ExtentCucumberAdapter.addTestStepLog("Regular Total : " + regularTotal);
				ExtentCucumberAdapter.addTestStepLog("Net Unit Price : " + netUnitPrice);
				ExtentCucumberAdapter.addTestStepLog("Net Total : " + netTotal);
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	/**
	 * Method to update Field Values in Quote Lines using Subscription Key
	 */
	@SuppressWarnings("unchecked")
	public static void updateQuoteLineFieldsThroughAPI() {
		String subsKey, effectiveQuantity, quantity, quoteLineId;
		for (Object productObj : (JSONArray) Constant.productsJsonObj.get(Constant.flowName)) {
			org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
			subsKey = (String) productJSONObj.get("SubscriptionKey");
			effectiveQuantity = (String) productJSONObj.get("EffectiveQuantity");
			quantity = (String) Constant.contractFields.get(subsKey).get("Quantity");
			effectiveQuantity = String
					.valueOf(Integer.parseInt(effectiveQuantity) + (int) Double.parseDouble(quantity));
			quoteLineId = APIUtils.getSoqlResult("Select Id from SBQQ__QuoteLine__c where Subscription_Key__c = '"
					+ subsKey + "' and SBQQ__Quote__r.Name = '" + Constant.quoteNumber + "'", "Id");
			org.json.simple.JSONObject requestParamsJSON = new org.json.simple.JSONObject();
			HashMap<Object, Object> map = new HashMap<>();
			map.put("SBQQ__Quantity__c", effectiveQuantity);
			requestParamsJSON.putAll(map);
			APIUtils.updateFieldValueInSObject(requestParamsJSON, quoteLineId, "SBQQ__QuoteLine__c");
		}
	}

	/**
	 * Method to Calculate Prorate Multiplier
	 * 
	 * @param startDate - Start Date
	 * @param endDate   - End Date
	 * @param term      - Term
	 * @return - Prorate Multiplier(in String format)
	 */
	public static String calculateProRateMultiplier(String startDate, String endDate, String term) {
		String proRateMultiplier = null;
		try {
			String difference = CommonUtils.getDifferenceBetweenDates(startDate, endDate);
			int months = Integer.parseInt(difference.split("#")[0]), days = Integer.parseInt(difference.split("#")[1]);
			proRateMultiplier = String
					.valueOf((double) ((months + (days / (365.0 / 12.0))) / Double.parseDouble(term)));
			return proRateMultiplier;
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	/**
	 * Method to update Contract Map based on Products.json values
	 * 
	 * @param flowName - Flow Name
	 */
	public static void updateContractMap(String flowName) {
		try {
			ExtentCucumberAdapter.addTestStepLog("--- Calculation with Updated Values ---");
			double discount = Double.parseDouble(Constant.discount.replace("%", ""));
			String quoteLineNumber, startDate, endDate, quantity, term, prorateMultiplier = null, price,
					regularUnitPrice = null, unitPrice, product, subsTerm = null, productId, subsType,
					disScheduleId = null, proratePrice = null, regularTotal = null, netUnitPrice = null,
					netTotal = null;
			boolean isTiered, isSubsProduct;
			for (Object productObj : (JSONArray) Constant.productsJsonObj.get(Constant.flowName)) {
				org.json.simple.JSONObject productJSONObj = (org.json.simple.JSONObject) productObj;
				quoteLineNumber = (String) productJSONObj.get("QuoteLineItemNumber");
				startDate = (String) productJSONObj.get("StartDate");
				endDate = (String) productJSONObj.get("EndDate");
				quantity = (String) productJSONObj.get("EffectiveQuantity");
				product = (String) productJSONObj.get("Product");
				if (Constant.contractFields.containsKey(quoteLineNumber)
						&& ((String) productJSONObj.get("Action")).equals("RenewalUpdate")) {
					if (!quantity.equals("NA") || !startDate.equals("NA") || !endDate.equals("NA")) {
						ExtentCucumberAdapter.addTestStepLog("Existing Quote Line : '" + quoteLineNumber + "'");
					}
					if (!quantity.equals("NA")) {
						CommonUtils.addKeyValueInGivenMap("Quantity", quantity, quoteLineNumber,
								Constant.contractFields);
						regularTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString((double) (Double.parseDouble(
								(String) Constant.contractFields.get(quoteLineNumber).get("RegularUnitPrice"))
								* Double.parseDouble(quantity)));
						netTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString((double) (Double
								.parseDouble((String) Constant.contractFields.get(quoteLineNumber).get("NetUnitPrice"))
								* Double.parseDouble(quantity)));
						CommonUtils.addKeyValueInGivenMap("RegularTotal", regularTotal, quoteLineNumber,
								Constant.contractFields);
						CommonUtils.addKeyValueInGivenMap("NetTotal", netTotal, quoteLineNumber,
								Constant.contractFields);
						ExtentCucumberAdapter.addTestStepLog("Updated Quantity : " + quantity);
					}
					if (!startDate.equals("NA")) {
						APIUtils.getSoqlResultInJSON(String.format(
								"SELECT Id, Pricebook2.Name, Name, UnitPrice FROM PricebookEntry WHERE ProductCode = '%s' and CurrencyIsoCode ='"
										+ Constant.contractCurrency + "' and Pricebook2.Id != '01s400000006WFgAAM'",
								(String) Constant.contractFields.get(quoteLineNumber).get("Product")));
						unitPrice = APIUtils.getSOQLFieldValueFromJSONConstant("UnitPrice");
						APIUtils.getSoqlResultInJSON(String.format(
								"SELECT Id, SBQQ__SubscriptionTerm__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
								(String) Constant.contractFields.get(quoteLineNumber).get("Product")));
						subsTerm = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionTerm__c");
						CommonUtils.addKeyValueInGivenMap("QuoteLineStartDate", startDate, quoteLineNumber,
								Constant.contractFields);
						List<Integer> tiers = new ArrayList<>();
						APIUtils.getSoqlResultInJSONForMultipleRecords(String.format(
								"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '%s'",
								(String) Constant.contractFields.get(quoteLineNumber).get("DiscountScheduleId")));
						JSONObject jSON;
						org.json.JSONArray tierArray = Constant.recordsJSONArray;
						for (int j = 0; j < tierArray.length(); j++) {
							jSON = tierArray.getJSONObject(j);
							double lowerBound = Double.parseDouble(
									APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", jSON));
							tiers.add((int) lowerBound);
						}
						Collections.sort(tiers);
						int diff = Integer.parseInt(CommonUtils
								.getDifferenceBetweenDates(
										(String) Constant.contractFields.get(quoteLineNumber).get("QuoteLineStartDate"),
										(String) Constant.contractFields.get(quoteLineNumber).get("QuoteLineEndDate"))
								.split("#")[0]);
						double decidedTier = 0;
						for (int tier : tiers) {
							if (tier <= diff)
								decidedTier = tier;
						}
						term = String.valueOf(decidedTier);
						prorateMultiplier = calculateProRateMultiplier(
								(String) Constant.contractFields.get(quoteLineNumber).get("QuoteLineStartDate"),
								(String) Constant.contractFields.get(quoteLineNumber).get("QuoteLineEndDate"),
								subsTerm);
						CommonUtils.addKeyValueInGivenMap("ProRateMultiplier", prorateMultiplier, quoteLineNumber,
								Constant.contractFields);
						double monthlyPrice = (double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm));
						APIUtils.getSoqlResultInJSON(
								"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '"
										+ (String) Constant.contractFields.get(quoteLineNumber)
												.get("DiscountScheduleId")
										+ "' and SBQQ__LowerBound__c = " + subsTerm);
						price = String.valueOf((monthlyPrice * Double.parseDouble(subsTerm))
								- ((monthlyPrice * Double.parseDouble(subsTerm)
										* Double.parseDouble(
												APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__Discount__c")))
										/ 100));
						APIUtils.getSoqlResultInJSON(
								"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '"
										+ (String) Constant.contractFields.get(quoteLineNumber)
												.get("DiscountScheduleId")
										+ "' and SBQQ__LowerBound__c = " + term);
						proratePrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(prorateMultiplier) * Double.parseDouble(price)));
						regularUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(proratePrice) - ((Double.parseDouble(proratePrice) * Double
										.parseDouble(APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__Discount__c")))
										/ 100)));
						regularTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(regularUnitPrice) * Double.parseDouble(
										(String) Constant.contractFields.get(quoteLineNumber).get("Quantity"))));
						netUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (caculatePriceAfterDiscount(Double.parseDouble(regularUnitPrice), discount)));
						netTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(netUnitPrice) * Double.parseDouble(
										(String) Constant.contractFields.get(quoteLineNumber).get("Quantity"))));
						CommonUtils.addKeyValueInGivenMap("RegularUnitPrice", regularUnitPrice, quoteLineNumber,
								Constant.contractFields);
						CommonUtils.addKeyValueInGivenMap("RegularTotal", regularTotal, quoteLineNumber,
								Constant.contractFields);
						CommonUtils.addKeyValueInGivenMap("NetUnitPrice", netUnitPrice, quoteLineNumber,
								Constant.contractFields);
						CommonUtils.addKeyValueInGivenMap("NetTotal", netTotal, quoteLineNumber,
								Constant.contractFields);
						ExtentCucumberAdapter.addTestStepLog("Updated Quote Line Start Date : " + startDate);
					}
					if (!endDate.equals("NA")) {
						APIUtils.getSoqlResultInJSON(String.format(
								"SELECT Id, Pricebook2.Name, Name, UnitPrice FROM PricebookEntry WHERE ProductCode = '%s' and CurrencyIsoCode ='"
										+ Constant.contractCurrency + "' and Pricebook2.Id != '01s400000006WFgAAM'",
								(String) Constant.contractFields.get(quoteLineNumber).get("Product")));
						unitPrice = APIUtils.getSOQLFieldValueFromJSONConstant("UnitPrice");
						APIUtils.getSoqlResultInJSON(String.format(
								"SELECT Id, SBQQ__SubscriptionTerm__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
								(String) Constant.contractFields.get(quoteLineNumber).get("Product")));
						subsTerm = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionTerm__c");
						CommonUtils.addKeyValueInGivenMap("QuoteLineEndDate", endDate, quoteLineNumber,
								Constant.contractFields);
						List<Integer> tiers = new ArrayList<>();
						APIUtils.getSoqlResultInJSONForMultipleRecords(String.format(
								"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '%s'",
								(String) Constant.contractFields.get(quoteLineNumber).get("DiscountScheduleId")));
						JSONObject jSON;
						org.json.JSONArray tierArray = Constant.recordsJSONArray;
						for (int j = 0; j < tierArray.length(); j++) {
							jSON = tierArray.getJSONObject(j);
							double lowerBound = Double.parseDouble(
									APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", jSON));
							tiers.add((int) lowerBound);
						}
						Collections.sort(tiers);
						int diff = Integer.parseInt(CommonUtils
								.getDifferenceBetweenDates(
										(String) Constant.contractFields.get(quoteLineNumber).get("QuoteLineStartDate"),
										(String) Constant.contractFields.get(quoteLineNumber).get("QuoteLineEndDate"))
								.split("#")[0]);
						double decidedTier = 0;
						for (int tier : tiers) {
							if (tier <= diff)
								decidedTier = tier;
						}
						term = String.valueOf(decidedTier);
						prorateMultiplier = calculateProRateMultiplier(
								(String) Constant.contractFields.get(quoteLineNumber).get("QuoteLineStartDate"),
								(String) Constant.contractFields.get(quoteLineNumber).get("QuoteLineEndDate"),
								subsTerm);
						CommonUtils.addKeyValueInGivenMap("ProRateMultiplier", prorateMultiplier, quoteLineNumber,
								Constant.contractFields);
						double monthlyPrice = (double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm));
						APIUtils.getSoqlResultInJSON(
								"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '"
										+ (String) Constant.contractFields.get(quoteLineNumber)
												.get("DiscountScheduleId")
										+ "' and SBQQ__LowerBound__c = " + subsTerm);
						price = String.valueOf((monthlyPrice * Double.parseDouble(subsTerm))
								- ((monthlyPrice * Double.parseDouble(subsTerm)
										* Double.parseDouble(
												APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__Discount__c")))
										/ 100));
						APIUtils.getSoqlResultInJSON(
								"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '"
										+ (String) Constant.contractFields.get(quoteLineNumber)
												.get("DiscountScheduleId")
										+ "' and SBQQ__LowerBound__c = " + term);
						proratePrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(prorateMultiplier) * Double.parseDouble(price)));
						regularUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(proratePrice) - ((Double.parseDouble(proratePrice) * Double
										.parseDouble(APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__Discount__c")))
										/ 100)));
						regularTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(regularUnitPrice) * Double.parseDouble(
										(String) Constant.contractFields.get(quoteLineNumber).get("Quantity"))));
						netUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (caculatePriceAfterDiscount(Double.parseDouble(regularUnitPrice), discount)));
						netTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(netUnitPrice) * Double.parseDouble(
										(String) Constant.contractFields.get(quoteLineNumber).get("Quantity"))));
						CommonUtils.addKeyValueInGivenMap("RegularUnitPrice", regularUnitPrice, quoteLineNumber,
								Constant.contractFields);
						CommonUtils.addKeyValueInGivenMap("RegularTotal", regularTotal, quoteLineNumber,
								Constant.contractFields);
						CommonUtils.addKeyValueInGivenMap("NetUnitPrice", netUnitPrice, quoteLineNumber,
								Constant.contractFields);
						CommonUtils.addKeyValueInGivenMap("NetTotal", netTotal, quoteLineNumber,
								Constant.contractFields);
						ExtentCucumberAdapter.addTestStepLog("Updated Quote Line End Date : " + endDate);
					}
					if (!quantity.equals("NA") || !startDate.equals("NA") || !endDate.equals("NA"))
						if (!endDate.equals("NA")) {
							ExtentCucumberAdapter
									.addTestStepLog("Updated Quote Line Prorate Multiplier : " + prorateMultiplier);
							ExtentCucumberAdapter.addTestStepLog("Updated Quote Line Prorate Price : " + proratePrice);
							ExtentCucumberAdapter
									.addTestStepLog("Updated Quote Line Regular Unit Price : " + regularUnitPrice);
							ExtentCucumberAdapter.addTestStepLog("Updated Quote Line Net Unit Price : " + netUnitPrice);
							ExtentCucumberAdapter.addTestStepLog("Updated Regular Total : " + regularTotal);
							ExtentCucumberAdapter.addTestStepLog("Updated Net Total : " + netTotal);
						} else if (endDate.equals("NA") && !quantity.equals("NA")) {
							ExtentCucumberAdapter.addTestStepLog("Updated Regular Total : " + regularTotal);
							ExtentCucumberAdapter.addTestStepLog("Updated Net Total : " + netTotal);
						}
				} else {
					CommonUtils.addKeyValueInGivenMap("Product", product, quoteLineNumber, Constant.contractFields);
					CommonUtils.addKeyValueInGivenMap("QuoteLineStartDate", startDate, quoteLineNumber,
							Constant.contractFields);
					CommonUtils.addKeyValueInGivenMap("QuoteLineEndDate", endDate, quoteLineNumber,
							Constant.contractFields);
					CommonUtils.addKeyValueInGivenMap("Quantity", quantity, quoteLineNumber, Constant.contractFields);
					APIUtils.getSoqlResultInJSON(String.format(
							"SELECT Id, SBQQ__SubscriptionType__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
							product));
					productId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
					subsType = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionType__c");
					APIUtils.getSoqlResultInJSON(String.format(
							"SELECT Id, Pricebook2.Name, Name, UnitPrice FROM PricebookEntry WHERE ProductCode = '%s' and CurrencyIsoCode ='"
									+ Constant.contractCurrency + "' and Pricebook2.Id != '01s400000006WFgAAM'",
							product));
					unitPrice = APIUtils.getSOQLFieldValueFromJSONConstant("UnitPrice");
					if (subsType.equalsIgnoreCase("One-time")) {
						isSubsProduct = false;
						CommonUtils.addKeyValueInGivenMap("IsSubscriptionProduct", isSubsProduct, quoteLineNumber,
								Constant.contractFields);
						regularUnitPrice = unitPrice;
						regularTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(regularUnitPrice) * Double.parseDouble(quantity)));
						netUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (caculatePriceAfterDiscount(Double.parseDouble(regularUnitPrice), discount)));
						netTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(netUnitPrice) * Double.parseDouble(quantity)));
						CommonUtils.addKeyValueInGivenMap("RegularUnitPrice", unitPrice, quoteLineNumber,
								Constant.contractFields);
						ExtentCucumberAdapter.addTestStepLog("Newly Added Quote Line : '" + quoteLineNumber + "'");
						ExtentCucumberAdapter.addTestStepLog("Product Name : " + product);
						ExtentCucumberAdapter.addTestStepLog("Quantity : " + quantity);
					} else {
						APIUtils.getSoqlResultInJSON(String.format(
								"SELECT Id, SBQQ__SubscriptionTerm__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
								product));
						subsTerm = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionTerm__c");
						isSubsProduct = true;
					}
					try {
						APIUtils.getSoqlResultInJSON(String.format(
								"SELECT Id FROM SBQQ__DiscountSchedule__c WHERE SBQQ__Product__r.Id = '%s'",
								productId));
						disScheduleId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
						isTiered = true;
					} catch (Exception e) {
						isTiered = false;
						if (isSubsProduct) {
							prorateMultiplier = calculateProRateMultiplier(startDate, endDate, subsTerm);
							proratePrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
									(double) (Double.parseDouble(prorateMultiplier) * Double.parseDouble(unitPrice)));
							CommonUtils.addKeyValueInGivenMap("ProRateMultiplier", prorateMultiplier, quoteLineNumber,
									Constant.contractFields);
							regularUnitPrice = proratePrice;
							regularTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
									(double) (Double.parseDouble(regularUnitPrice) * Double.parseDouble(quantity)));
							netUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
									(double) (caculatePriceAfterDiscount(Double.parseDouble(regularUnitPrice),
											discount)));
							netTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
									(double) (Double.parseDouble(netUnitPrice) * Double.parseDouble(quantity)));
							CommonUtils.addKeyValueInGivenMap("RegularUnitPrice", regularUnitPrice, quoteLineNumber,
									Constant.contractFields);
							CommonUtils.addKeyValueInGivenMap("ProRatePrice", proratePrice, quoteLineNumber,
									Constant.contractFields);
							ExtentCucumberAdapter.addTestStepLog("Newly Added Quote Line : '" + quoteLineNumber + "'");
							ExtentCucumberAdapter.addTestStepLog("Product Name : " + product);
							ExtentCucumberAdapter.addTestStepLog("Quantity : " + quantity);
							ExtentCucumberAdapter.addTestStepLog("Quote Line Start Date : " + startDate);
							ExtentCucumberAdapter.addTestStepLog("Quote Line End Date : " + endDate);
							ExtentCucumberAdapter
									.addTestStepLog("Quote Line Prorate Multiplier : " + prorateMultiplier);
							ExtentCucumberAdapter.addTestStepLog("Quote Line Prorate Price : " + proratePrice);
						}
					}
					if (isTiered) {
						List<Integer> tiers = new ArrayList<>();
						APIUtils.getSoqlResultInJSONForMultipleRecords(String.format(
								"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '%s'",
								disScheduleId));
						JSONObject jSON;
						org.json.JSONArray tierArray = Constant.recordsJSONArray;
						for (int j = 0; j < tierArray.length(); j++) {
							jSON = tierArray.getJSONObject(j);
							double lowerBound = Double.parseDouble(
									APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", jSON));
							tiers.add((int) lowerBound);
						}
						Collections.sort(tiers);
						int diff = Integer
								.parseInt(CommonUtils.getDifferenceBetweenDates(startDate, endDate).split("#")[0]);
						double decidedTier = 0;
						for (int tier : tiers) {
							if (tier <= diff)
								decidedTier = tier;
						}
						term = String.valueOf(decidedTier);
						prorateMultiplier = calculateProRateMultiplier(startDate, endDate, subsTerm);
						CommonUtils.addKeyValueInGivenMap("ProRateMultiplier", prorateMultiplier, quoteLineNumber,
								Constant.contractFields);
						double monthlyPrice = (double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm));
						APIUtils.getSoqlResultInJSON(
								"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '"
										+ disScheduleId + "' and SBQQ__LowerBound__c = " + subsTerm);
						price = String.valueOf((monthlyPrice * Double.parseDouble(subsTerm))
								- ((monthlyPrice * Double.parseDouble(subsTerm)
										* Double.parseDouble(
												APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__Discount__c")))
										/ 100));
						APIUtils.getSoqlResultInJSON(
								"SELECT id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '"
										+ disScheduleId + "' and SBQQ__LowerBound__c = " + term);
						proratePrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(prorateMultiplier) * Double.parseDouble(price)));
						regularUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(proratePrice) - ((Double.parseDouble(proratePrice) * Double
										.parseDouble(APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__Discount__c")))
										/ 100)));
						regularTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(regularUnitPrice) * Double.parseDouble(quantity)));
						netUnitPrice = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (caculatePriceAfterDiscount(Double.parseDouble(regularUnitPrice), discount)));
						netTotal = CommonUtils.roundOffDoubleToTwoDecPlaceToString(
								(double) (Double.parseDouble(netUnitPrice) * Double.parseDouble(quantity)));
						CommonUtils.addKeyValueInGivenMap("ProRatePrice", proratePrice, quoteLineNumber,
								Constant.contractFields);
						CommonUtils.addKeyValueInGivenMap("RegularUnitPrice", regularUnitPrice, quoteLineNumber,
								Constant.contractFields);
						ExtentCucumberAdapter.addTestStepLog("Newly Added Quote Line : '" + quoteLineNumber + "'");
						ExtentCucumberAdapter.addTestStepLog("Product Name : " + product);
						ExtentCucumberAdapter.addTestStepLog("Quantity : " + quantity);
						ExtentCucumberAdapter.addTestStepLog("Quote Line Start Date : " + startDate);
						ExtentCucumberAdapter.addTestStepLog("Quote Line End Date : " + endDate);
						ExtentCucumberAdapter.addTestStepLog("Quote Line Prorate Multiplier : " + prorateMultiplier);
						ExtentCucumberAdapter.addTestStepLog("Quote Line Prorate Price : " + proratePrice);
					}
					CommonUtils.addKeyValueInGivenMap("RegularTotal", regularTotal, quoteLineNumber,
							Constant.contractFields);
					CommonUtils.addKeyValueInGivenMap("NetUnitPrice", netUnitPrice, quoteLineNumber,
							Constant.contractFields);
					CommonUtils.addKeyValueInGivenMap("NetTotal", netTotal, quoteLineNumber, Constant.contractFields);
					ExtentCucumberAdapter.addTestStepLog("Quote Line Regular Unit Price : " + regularUnitPrice);
					ExtentCucumberAdapter.addTestStepLog("Quote Line Regular Total : " + regularTotal);
					ExtentCucumberAdapter.addTestStepLog("Quote Line Net Unit Price : " + netUnitPrice);
					ExtentCucumberAdapter.addTestStepLog("Quote Line Net Total : " + netTotal);
				}
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	/**
      * Method to Verify Product Prices done by CPQ
      * @param flowName - Flow Name
      */
      public static void verifyProductPrices(String flowName) {
         Map<String, Object> tempMap = null;
         String netUnitPrice, netTotalPrice, regUnitPrice, regTotalPrice, quantity, startDate, endDate;
         String actual = "Actual :- ", expected = "Expected :- ", quoteLineNumber;
         Constant.resultList.add("Validating System calculated values with UI values");
         try {
           for (Map.Entry<String, Map<String, Object>> entry : Constant.contractFields.entrySet()) {
              quoteLineNumber = entry.getKey();
              tempMap = Constant.contractFields.get(quoteLineNumber);
              String regex = "\\d+\\.*\\d$";
              String reg = "[0-9]+";                          
              regUnitPrice = (String) tempMap.get("RegularUnitPrice");
              regTotalPrice = (String) tempMap.get("RegularTotal");
              quantity = (String) tempMap.get("Quantity");
              netUnitPrice = (String) tempMap.get("NetUnitPrice");
              netTotalPrice = (String) tempMap.get("NetTotal");
              if(regUnitPrice.matches(regex))
                regUnitPrice += "0";
              if(regTotalPrice.matches(regex))
                regTotalPrice += "0";
              if(netUnitPrice.matches(regex))
                netUnitPrice += "0";
              if(netTotalPrice.matches(regex))
                netTotalPrice += "0";
              if(quantity.matches(reg))
                quantity += ".00";
              else if(quantity.matches(regex))
                quantity += "0";
              startDate = (String) tempMap.get("QuoteLineStartDate");
              endDate = (String) tempMap.get("QuoteLineEndDate");
              Constant.FRAME = PlaywrightUtils.getFrame("Scrolling Frame", Constant.PAGE);
              List<Locator> list = PlaywrightUtils.getElement("Table Row", Constant.FRAME).all();
                                              Locator temp = list.get(Integer.parseInt(quoteLineNumber)-1);
                                              Constant.resultList.add("Values for Quote Line : '" + quoteLineNumber + "'");
              Assert.assertEquals(quantity, PlaywrightUtils.getText(PlaywrightUtils.getElement("Quantity", temp)));
              expected += "Quantity :'" + quantity + "' ";
              actual += "Quantity :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Quantity", temp)) + "' ";
              if(!tempMap.containsKey("IsSubscriptionProduct")){ 
                Assert.assertEquals(startDate, PlaywrightUtils.getText(PlaywrightUtils.getElement("Start Date", temp)));
                expected += "Start Date :'" + startDate + "' ";
                actual += "Start Date :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Start Date", temp)) + "' ";
                Assert.assertEquals(endDate, PlaywrightUtils.getText(PlaywrightUtils.getElement("End Date", temp)));
                expected += "End Date :'" + endDate + "' ";
                actual += "End Date :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("End Date", temp)) + "' ";
              }
              Assert.assertEquals(regUnitPrice, PlaywrightUtils.getText(PlaywrightUtils.getElement("Regular Unit Price", temp)).replace(",", "").trim().split(" ")[1]);
		      expected += "Regular Unit Price :'" + regUnitPrice + "' ";
		      actual += "Regular Unit Price :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Regular Unit Price", temp)).replace(",", "").trim().split(" ")[1] + "' ";
		      Assert.assertEquals(regTotalPrice, PlaywrightUtils.getText(PlaywrightUtils.getElement("Regular Total", temp)).replace(",", "").trim().split(" ")[1]);
		      expected += "Regular Total :'" + regTotalPrice + "' ";
		      actual += "Regular Total :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Regular Total", temp)).replace(",", "").trim().split(" ")[1] + "' ";
		      Assert.assertEquals(netUnitPrice, PlaywrightUtils.getText(PlaywrightUtils.getElement("Net Unit Price", temp)).replace(",", "").trim().split(" ")[1]);
		      expected += "Net Unit Price :'" + netUnitPrice + "' ";
		      actual += "Net Unit Price :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Net Unit Price", temp)).replace(",", "").trim().split(" ")[1] + "' ";
		      Assert.assertEquals(netTotalPrice, PlaywrightUtils.getText(PlaywrightUtils.getElement("Net Total", temp)).replace(",", "").trim().split(" ")[1]);
		      expected += "Net Total :'" + netTotalPrice + "' ";
		      actual += "Net Total :'" + PlaywrightUtils.getText(PlaywrightUtils.getElement("Net Total", temp)).replace(",", "").trim().split(" ")[1] + "' ";
              Constant.resultList.add(expected);
              Constant.resultList.add(actual);
              expected = "Actual :- "; actual = "Expected :- ";
           }
         }catch (Exception e) {
                       throw new CustomException(e.getMessage());
         }
      }
            
     /**
     * Method to calculate Unit Price of Ava Product(Subs/Non-subs Product both supported)
     * @param productName - Product Code
     * @return - Unit Price
     */
    public static String calculateUnitPriceForAvaProduct(String product) {
    	String unitPrice = null, subsType, subsTerm = null, disScheduleId = null, productId, decidedPrice = null;
    	boolean isTiered;
    	APIUtils.getSoqlResultInJSON(String.format("SELECT Id, SBQQ__SubscriptionType__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
				product));
    	productId = APIUtils.getSOQLFieldValueFromJSONConstant("Id");
		subsType = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionType__c");
    	APIUtils.getSoqlResultInJSON(String.format(
				"SELECT Id, Pricebook2.Name, Name, UnitPrice FROM PricebookEntry WHERE ProductCode = '%s' and CurrencyIsoCode ='"
						+ Constant.currencyISOCode + "' and Pricebook2.Id != '01s400000006WFgAAM'", product));
    	unitPrice = APIUtils.getSOQLFieldValueFromJSONConstant("UnitPrice");
		if (subsType.equalsIgnoreCase("One-time")) {
			decidedPrice = unitPrice;
		} else {
			APIUtils.getSoqlResultInJSON(String.format(
					"SELECT Id, SBQQ__SubscriptionTerm__c FROM Product2 WHERE ProductCode = '%s' and RecordType.Name = 'Material product' and IsActive = true",
					product));
			subsTerm = APIUtils.getSOQLFieldValueFromJSONConstant("SBQQ__SubscriptionTerm__c");
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
			decidedPrice = unitPrice;
		}
		if (isTiered) {
			APIUtils.getSoqlResultInJSONForMultipleRecords(String.format(
					"SELECT Id, SBQQ__Discount__c, SBQQ__LowerBound__c, SBQQ__UpperBound__c FROM SBQQ__DiscountTier__c WHERE SBQQ__Schedule__r.Id = '%s'",
					disScheduleId));
			JSONObject json;
			for (int i = 0; i < Constant.recordsJSONArray.length(); i++) {
				json = Constant.recordsJSONArray.getJSONObject(i);
				double monthlyPrice = (double) (Double.parseDouble(unitPrice) / Double.parseDouble(subsTerm));
				double lowerBound = Double.parseDouble(
				APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__LowerBound__c", json));
				decidedPrice = String.valueOf((monthlyPrice * lowerBound) - ((monthlyPrice * lowerBound
						* Double.parseDouble(
								APIUtils.getSOQLFieldValueFromJSONConstantByGiveJSON("SBQQ__Discount__c", json)))
						/ 100));
			}
		}
		return decidedPrice;
    }
    
    /**
     * Method to calculate Unit Price of Non Ava Product(Subs/Non-subs Product both supported)
     * @param productName - Product Code
     * @return - Unit Price
     */
    public static String calculateUnitPriceForNonAvaProduct(String product) {
    	String unitPrice = null;
    	if(Constant.currencyISOCode == null)
    		Constant.currencyISOCode = "USD";
    	APIUtils.getSoqlResultInJSON("SELECT Id, Price__c from Price_Lookup_Item__c where "
    			+ "Product__r.Name = '"+product+"' and Base_Price__r.Status__c = 'Published' and "
    					+ "CurrencyIsoCode = '"+Constant.currencyISOCode+"'");
    	unitPrice = APIUtils.getSOQLFieldValueFromJSONConstant("Price__c");
    	return unitPrice;
    }
}

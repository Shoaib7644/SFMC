package com.salesforce.marketingcloud.constant;

import com.salesforce.marketingcloud.exception.CustomException;

public class SOQLConstant {

	private SOQLConstant() {}
	
	/**
	 * Method to get Query by given Object Name
	 * @param objName - Object Name
	 * @param params - Param used in where clause
	 * @return - Query
	 */
	public static String getQuery(String objName, Object... params) {
		switch (objName) {
		case "Cart Id from Order":
			return String.format("SELECT Cart__c from Order where OrderReferenceNumber  = '%s'", params);
		case "Cart":
			return String.format("SELECT Cart_Type__c, Status, TotalProductCount, TotalProductAmount, TotalAmount, GrandTotalAmount, Id FROM WebCart where Id = '%s'", params);
		case "Cart Item":
			return String.format("SELECT Quantity, SalesPrice, TotalLineNetAmount, TotalAmount, Id FROM CartItem WHERE CartId = '%s' and Product2.Name = '%s'", params);
		case "Quote":
			return String.format("SELECT SBQQ__Status__c, SBQQ__StartDate__c, SBQQ__ExpirationDate__c, SBQQ__ListAmount__c, SBQQ__RegularAmount__c, SBQQ__CustomerAmount__c, SBQQ__NetAmount__c,  Id FROM SBQQ__Quote__c WHERE Cart__c = '%s'", params);
		case "Quote Line":
			return String.format("SELECT SBQQ__DefaultSubscriptionTerm__c, SBQQ__SubscriptionType__c, SBQQ__EffectiveQuantity__c, SBQQ__CustomerPrice__c, SBQQ__NetPrice__c, SBQQ__RegularPrice__c, SBQQ__ListTotal__c, SBQQ__CustomerTotal__c, SBQQ__NetTotal__c, Id FROM SBQQ__QuoteLine__c WHERE SBQQ__Quote__c = '%s' and SBQQ__Product__r.Name = '%s'", params);
		case "Order":
			return String.format("SELECT EffectiveDate, TotalAmount, Status, OrderNumber, Id FROM Order WHERE OrderReferenceNumber = '%s'", params);
		case "Renewal Order via Order":
			return String.format("SELECT EffectiveDate, TotalAmount, Status, OrderNumber, Id FROM Order WHERE Id = '%s'", params);
		case "Order Activation":
			return String.format("SELECT Status, OrderNumber, Id FROM Order WHERE OrderReferenceNumber = '%s'", params);
		case "Order Item":
			return String.format("SELECT SBQQ__Status__c, ServiceDate, EndDate, Quantity, UnitPrice, TotalPrice, Id FROM OrderItem WHERE OrderId = '%s' and Product2.Name = '%s'", params);
		case "Contract":
			return String.format("Select StartDate, EndDate, ContractTerm, Id FROM Contract WHERE SBQQ__Order__c = '%s'", params);
		case "Subscription":
			return String.format("Select SBQQ__StartDate__c, SBQQ__EndDate__c, SBQQ__Quantity__c, SBQQ__CustomerPrice__c, SBQQ__RegularPrice__c, SBQQ__NetPrice__c, Id FROM SBQQ__Subscription__c WHERE SBQQ__Contract__c = '%s' and SBQQ__Product__r.Name = '%s' order by CreatedDate desc limit 1", params);
		case "Subscription Quantity and Product":
			return String.format("Select SUM(SBQQ__Quantity__c), SBQQ__Product__r.Name from SBQQ__Subscription__c where SBQQ__Contract__c = '%s' group by SBQQ__Product__r.Name", params);
		case "Pricebook Entry":
			return String.format("SELECT UnitPrice FROM PricebookEntry where Product2.Name = '%s' and CurrencyIsoCode = 'USD' and Pricebook2.Name like 'InEight%%'", params);
		case "Contract Id":
			return String.format("Select ContractNumber, Id from Contract where SBQQ__Order__r.OrderReferenceNumber = '%s'", params);
		case "Order Id":
			return String.format("Select Id from Order where OrderReferenceNumber = '%s'", params);
		case "Contract Id, End Date from ECOM User Contact":
			return String.format("Select Id, EndDate, ContractNumber, SBQQ__RenewalTerm__c, StartDate, Provisioning_Admin_Email__c from Contract where SIB_Related_Contact__r.Name = '%s' and Status = 'Active Contract' and EndDate > TODAY order by CreatedDate desc limit 1", params);
		case "Stripe Token for Digital Wallet":
			return String.format("Select GatewayToken from DigitalWallet where NickName = '%s'", params);
		case "Subscription Status":
			return String.format("select SIB_Subscription_Status__c from SBQQ__Subscription__c where SBQQ__Contract__r.ContractNumber = '%s' and SBQQ__Product__r.Name = '%s'", params);
		case "All Active Contracts":
			return String.format("Select Id, ContractNumber from Contract where SIB_Related_Contact__r.Name = '%s' and Status = 'Active Contract'", params);
		case "All Subscription Details":
			return String.format("Select SBQQ__Product__r.Name, SBQQ__Contract__c, MIN(SBQQ__StartDate__c), MAX(SBQQ__EndDate__c),  SUM(SBQQ__Quantity__c) from SBQQ__Subscription__c where SBQQ__Contract__c in (Select Id from Contract where SIB_Related_Contact__r.Name = '%s' and Status = 'Active Contract' ) group by SBQQ__Product__r.Name, SBQQ__Contract__c", params);
		case "Renewal Opportunity":
			return String.format("Select Id, SBQQ__RenewalOpportunity__c from Contract where Id = '%s'", params);
		case "Renewal Quote":
			return String.format("Select Id from SBQQ__Quote__c where SBQQ__Opportunity2__c = '%s'", params);
		case "Renewal Order":
			return String.format("Select Id from Order where SBQQ__Quote__c = '%s'", params);
		case "Renewal Contract":
			return String.format("Select Id, ContractNumber from Contract where SBQQ__Order__c = '%s'", params);
		case "Cart Id from Renewal Order":
			return String.format("Select Cart__c from SBQQ__Quote__c where Id = '%s'", params);
		case "Orders in Order History DESC":
			return String.format("Select Id, OrderNumber, OrderedDate, TotalAmount from Order where Account.Name = 'Caban Inc- Automation Acc' and Provisioning_Admin_Email__c = '%s' order by OrderedDate desc", params);
		case "Orders in Order History AESC":
			return String.format("Select Id, OrderNumber, OrderedDate, TotalAmount from Order where Account.Name = 'Caban Inc- Automation Acc' and Provisioning_Admin_Email__c = '%s' order by OrderedDate", params);
		case "Seven Day Orders in Order History":
			return String.format("Select Id, OrderNumber, OrderedDate, TotalAmount from Order where Account.Name = 'Caban Inc- Automation Acc' and Provisioning_Admin_Email__c = '%s' and OrderedDate = LAST_N_DAYS:7 order by OrderedDate", params);
		default:
			throw new CustomException("Query not defined");
		}
	}
}

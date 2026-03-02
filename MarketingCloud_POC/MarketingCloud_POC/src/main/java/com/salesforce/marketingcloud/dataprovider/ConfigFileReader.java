package com.salesforce.marketingcloud.dataprovider;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.enums.EnvironmentType;
import com.salesforce.marketingcloud.enums.WebBrowserType;
import com.salesforce.marketingcloud.utils.AESUtils;


/*-----------------------------------------------------------------------------------------------
 * ----------------------------------------------------------------------------------------------
This Class is used the get the value from the properties file
-------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------*/

public class ConfigFileReader {

	private Properties properties;
	private final String propertyFilePath = Constant.PROPERTYFILEPATH;
	// private final String  reportConfigPath="src//test//resources//config//extent-config.xml";

	public ConfigFileReader() {
		BufferedReader reader;
		try {
			reader = new BufferedReader (new FileReader (propertyFilePath));
			properties = new Properties ( );
			try {
				properties.load (reader);
				reader.close ( );
			} catch (IOException e) {
				e.printStackTrace ( );
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace ( );
			throw new RuntimeException ("Configuration.properties not found at " + propertyFilePath);
		}
	}

	public long getImplicitlyWait() {
		String implicitlyWait = properties.getProperty ("implicitlyWait");
		if (implicitlyWait != null)
			return Long.parseLong (implicitlyWait);
		else
			throw new RuntimeException ("implicitlyWait not specified in the Configuration.properties file.");
	}

	public String getSfdcUrl() {
		String url = properties.getProperty ("SFDCURL_"+Constant.env);
		if (url != null) return url;
		else throw new RuntimeException ("url not specified in the Configuration.properties file.");
	}
	
	public String getSfdcUsername() {
		String Username = properties.getProperty ("SFDCUsername_"+Constant.env);
		if (Username != null) return Username;
		else throw new RuntimeException ("Username not specified in the Configuration.properties file.");
	}
	
	public String getSfdcPassword() {
		String Password = properties.getProperty ("SFDCPassword_"+Constant.env);
		if (Password != null) {
			Password = AESUtils.decrypt(Password);
			return Password;
		}
		else throw new RuntimeException ("Password not specified in the Configuration.properties file.");
	}
	
	public Boolean getBrowserWindowSize() { String windowSize =
			properties.getProperty("windowMaximize"); if(windowSize != null) return
					Boolean.valueOf(windowSize); return true; }

	public String getemailfrom() {
		String emailfrom = properties.getProperty ("emailfrom");
		if (emailfrom != null) return emailfrom;
		else throw new RuntimeException ("emailfrom not specified in the Configuration.properties file.");
	}
	public String getemailto() {
		String emailto = properties.getProperty ("emailto");
		if (emailto != null) return emailto;
		else throw new RuntimeException ("emailto not specified in the Configuration.properties file.");
	}
	public String getemailpwd() {
		String emailpwd = properties.getProperty ("emailpwd");
		if (emailpwd != null) return emailpwd;
		else throw new RuntimeException ("emailpwd not specified in the Configuration.properties file.");
	}
	public String getemailsmtp() {
		String emailsmtp = properties.getProperty ("emailsmtp");
		if (emailsmtp != null) return emailsmtp;
		else throw new RuntimeException ("emailsmtp not specified in the Configuration.properties file.");
	}
	
	public String getProductName() {
		String productName = properties.getProperty ("productName");
		if (productName != null) return productName;
		else throw new RuntimeException ("productName not specified in the Configuration.properties file.");
	}
	
	public String getProductSKU() {
		String productSKU = properties.getProperty ("SKUCode");
		if (productSKU != null) return productSKU;
		else throw new RuntimeException ("SKUCode not specified in the Configuration.properties file.");
	}
	
	public String getCategoryName() {
		String category = properties.getProperty ("Category");
		if (category != null) return category;
		else throw new RuntimeException ("Category not specified in the Configuration.properties file.");
	}
	
	public String getCardHolder() {
		String cardHolderName = properties.getProperty ("CardHolder");
		if (cardHolderName != null) return cardHolderName;
		else throw new RuntimeException ("CardHolder not specified in the Configuration.properties file.");
	}
	
	public String getCardNumber() {
		String cardNo = properties.getProperty ("CardNumber");
		if (cardNo != null) return cardNo;
		else throw new RuntimeException ("CardNumber not specified in the Configuration.properties file.");
	}
	
	public String getCardCVV() {
		String cvvNo = properties.getProperty ("CVV");
		if (cvvNo != null) return cvvNo;
		else throw new RuntimeException ("CVV not specified in the Configuration.properties file.");
	}
	
	public String getCardType() {
		String type = properties.getProperty ("CartType");
		if (type != null) return type;
		else throw new RuntimeException ("CartType not specified in the Configuration.properties file.");
	}
	
	public String getCardExpireDateMonth() {
		String month = properties.getProperty ("Expiration");
		if (month != null) return month;
		else throw new RuntimeException ("Expiration not specified in the Configuration.properties file.");
	}
	
	public String getSecretKey() {
		String secretKey = properties.getProperty ("SecretKey");
		if (secretKey != null) return secretKey;
		else throw new RuntimeException ("SecretKey not specified in the Configuration.properties file.");
	}

	public String getSalt() {
		String saltValue = properties.getProperty ("Salt");
		if (saltValue != null) return saltValue;
		else throw new RuntimeException ("Salt not specified in the Configuration.properties file.");
	}

	public String getPayloadURL() {
		String payloadURL = properties.getProperty ("PayloadURL_"+Constant.env);
		if (payloadURL != null) return payloadURL;
		else throw new RuntimeException ("PayloadURL_"+Constant.env+" not specified in the Configuration.properties file.");
	}
	
	public String getSecurityToken() {
		String securityToken = properties.getProperty ("SecurityToken_"+Constant.env);
		if (securityToken != null) return securityToken;
		else throw new RuntimeException ("SecurityToken_"+Constant.env+" not specified in the Configuration.properties file.");
	}
	
	public String getClientId() {
		String clientId = properties.getProperty ("ClientId_"+Constant.env);
		if (clientId != null) return clientId;
		else throw new RuntimeException ("ClientId_"+Constant.env+" not specified in the Configuration.properties file.");
	}
	
	public String getClientSecret() {
		String clientSecret = properties.getProperty ("ClientSecret_"+Constant.env);
		if (clientSecret != null) return clientSecret;
		else throw new RuntimeException ("ClientSecret_"+Constant.env+" not specified in the Configuration.properties file.");
	}
	
	public String getAIOAuthorizationToken() {
		String aioToken = properties.getProperty ("AIOToken");
		if (aioToken != null) return aioToken;
		else throw new RuntimeException ("AIOToken not specified in the Configuration.properties file.");
	}

	 public String getAIOAutomationOwnerID() {
		 String ownerId = properties.getProperty ("AIOAutomationOwnerID");
		 if (ownerId != null) return ownerId;
		 else throw new RuntimeException ("AIOAutomationOwnerID not specified in the Configuration.properties file.");
	 }
	 
	 public String getJiraProjectId() {
		 String projectId = properties.getProperty ("JiraProjectId");
		 if (projectId != null) return projectId;
		 else throw new RuntimeException ("Jira Project Id not specified in the Configuration.properties file.");
	 }
	 
	 public WebBrowserType getWebBrowser() {
			String browserName = properties.getProperty("browser");
			if(browserName == null || browserName.equals("chrome")) return WebBrowserType.CHROME;
			else if(browserName.equalsIgnoreCase("firefox")) return WebBrowserType.FIREFOX;
			else if(browserName.equals("chromium")) return WebBrowserType.CHROMIUM;
			else if(browserName.equals("edge")) return WebBrowserType.EDGE;
			else if(browserName.equals("webkit")) return WebBrowserType.WEBKIT;
			else throw new RuntimeException("Browser Name Key value in Configuration.properties is not matched : " + browserName);
	}
	 
	 public EnvironmentType getEnvironment() {
			String environmentName = properties.getProperty("environment");
			if(environmentName == null || environmentName.equalsIgnoreCase("web")) return EnvironmentType.WEB;
			else throw new RuntimeException("Environment Type Key value in Configuration.properties is not matched : " + environmentName);
	}
	
	public String getStoreUrl() {
		String url = properties.getProperty ("ECommerceStoreURL_"+Constant.env);
		if (url != null) return url;
		else throw new RuntimeException ("ECommerceStoreURL_"+Constant.env+" not specified in the Configuration.properties file.");
	}
	public String getStoreUsername() {
		String Username = properties.getProperty ("ECommerceStoreUsername_"+Constant.env);
		if (Username != null) return Username;
		else throw new RuntimeException ("ECommerceStoreUsername_"+Constant.env+" not specified in the Configuration.properties file.");
	}
	
	public String getStorePassword() {
		String Password = properties.getProperty ("ECommerceStorePassword_"+Constant.env);
		if (Password != null) {
			Password = AESUtils.decrypt(Password);
			return Password;
		}
		else throw new RuntimeException ("ECommerceStorePassword_"+Constant.env+" not specified in the Configuration.properties file.");
	}
	
	public String getPropertyByName(String propertyName) {
		String value = properties.getProperty (propertyName);
		if (value != null) {
			return value;
		}
		else throw new RuntimeException ("'"+propertyName+"' not specified in the Configuration.properties file.");
	}
	
	public String getEmailFrom() {
		String emailFrom = properties.getProperty ("EmailFrom");
		if (emailFrom != null) return emailFrom;
		else throw new RuntimeException ("'emailfrom' not specified in the config.properties file.");
	}
	
	public String getEmailTo() {
		String emailTo = properties.getProperty ("EmailTo");
		if (emailTo != null) return emailTo;
		else throw new RuntimeException ("'emailto' not specified in the config.properties file.");
	}
	
	public String getEmailCC() {
		String emailCC = properties.getProperty ("EmailCC");
		if (emailCC != null) return emailCC;
		else throw new RuntimeException ("'emailto' not specified in the config.properties file.");
	}
	
	public String getEmailPassword() {
		String emailPassword = properties.getProperty ("EmailPassword");
		if (emailPassword != null) return AESUtils.decrypt(emailPassword);
		else throw new RuntimeException ("'emailpwd' not specified in the config.properties file.");
	}
	
	public String getEmailSMTP() {
		String emailSMTP = properties.getProperty ("EmailSMTP");
		if (emailSMTP != null) return emailSMTP;
		else throw new RuntimeException ("'emailsmtp' not specified in the config.properties file.");
	}
	
	public String getEmailSubject() {
		String value = properties.getProperty ("Subject");
		if (value != null) return value;
		else throw new RuntimeException ("'Subject' not specified in the Configuration.properties file.");
	}

	public String getEmailInitial() {
		String value = properties.getProperty ("Initial");
		if (value != null) return value;
		else throw new RuntimeException ("'Initial' not specified in the Configuration.properties file.");
	}

	public String getEmailMessage() {
		String value = properties.getProperty ("Message");
		if (value != null) return value;
		else throw new RuntimeException ("'Message' not specified in the Configuration.properties file.");
	}

	public String getEmailRegards() {
		String value = properties.getProperty ("Regards");
		if (value != null) return value;
		else throw new RuntimeException ("'Regards' not specified in the Configuration.properties file.");
	}

	public String getEmailSender() {
		String value = properties.getProperty ("Sender");
		if (value != null) return value;
		else throw new RuntimeException ("'Sender' not specified in the Configuration.properties file.");
	}

	public String getEmailReportName() {
		String value = properties.getProperty ("ReportName");
		if (value != null) return value;
		else throw new RuntimeException ("'ReportName' not specified in the Configuration.properties file.");
	}
	
	public String getEmailSignatureLogo() {
		String value = properties.getProperty ("EmailSignatureLogo");
		if (value != null) return value;
		else throw new RuntimeException ("'ReportName' not specified in the Configuration.properties file.");
	}
	
	public String getPdfReportFilePath() {
		String value = properties.getProperty ("PdfReportFilePath");
		if (value != null) return value;
		else throw new RuntimeException ("'PdfReportFilePath' not specified in the Configuration.properties file.");
	}
}


package com.salesforce.marketingcloud.hooks;

import java.io.FileReader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.dataprovider.LocatorsFileReader;
import com.salesforce.marketingcloud.managers.PlaywrightManager;
import com.salesforce.marketingcloud.utils.CommonUtils;
import com.salesforce.marketingcloud.utils.ExcelUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class Hooks {

	
	@Before
	public void beforeScenario(Scenario scenario) throws Exception {
		
		LocatorsFileReader.readLocatorProperties();
		ExcelUtils.openStream();
		new PlaywrightManager();
		Constant.PAGE = PlaywrightManager.intializePlaywright();
		Constant.productsJsonObj = (JSONObject)new JSONParser()
				.parse(new FileReader(Constant.PRODUCTSJSONPATH));
		CommonUtils.reInitializeTheCpqFieldValues();
		Constant.writingInExcel = false;
	}
	
	@After
	public void afterScenario(Scenario scenario) throws Exception {

		try {
			String screenshotName = scenario.getName().replaceAll(" ", "_");
			if (scenario.isFailed()) {
				scenario.log("Test Case Failed");
				byte[] screenshot = Constant.PAGE.screenshot();
				scenario.attach(screenshot, "image/png", screenshotName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(Constant.writingInExcel)
			ExcelUtils.writeInExcelFileUsingStream();
		PlaywrightManager.closePlaywright();
	}
}

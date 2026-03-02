package com.salesforce.marketingcloud.hooks;

import java.io.ByteArrayInputStream; // Added for Allure
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
import io.qameta.allure.Allure; // Added for Allure

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
        
        // Optional: Add a label to Allure for better categorization
        Allure.parameter("Browser", "Chromium");
    }
    
    @After
    public void afterScenario(Scenario scenario) throws Exception {
        try {
            String screenshotName = scenario.getName().replaceAll(" ", "_");
            if (scenario.isFailed()) {
                scenario.log("Test Case Failed");
                
                // 1. Capture the screenshot buffer
                byte[] screenshot = Constant.PAGE.screenshot();
                
                // 2. Attach to standard Cucumber Report (your current logic)
                scenario.attach(screenshot, "image/png", screenshotName);
                
                // 3. Attach specifically to Allure Report
                Allure.addAttachment(screenshotName, new ByteArrayInputStream(screenshot));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(Constant.writingInExcel)
                ExcelUtils.writeInExcelFileUsingStream();
            
            // Ensure Playwright closes even if screenshot fails
            PlaywrightManager.closePlaywright();
        }
    }
}
package com.salesforce.marketingcloud.hooks;

import java.io.ByteArrayInputStream;
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
import io.qameta.allure.Allure;

public class Hooks {

    @Before
    public void beforeScenario(Scenario scenario) throws Exception {

        try {

            scenario.log("Initializing framework setup");

            LocatorsFileReader.readLocatorProperties();
            ExcelUtils.openStream();

            new PlaywrightManager();
            Constant.PAGE = PlaywrightManager.intializePlaywright();

            if (Constant.PAGE != null) {
                Constant.PAGE.setDefaultTimeout(60000);
            }

            Constant.productsJsonObj = (JSONObject) new JSONParser()
                    .parse(new FileReader(Constant.PRODUCTSJSONPATH));

            CommonUtils.reInitializeTheCpqFieldValues();
            Constant.writingInExcel = false;

            Allure.parameter("Browser", "Chromium");

        } catch (Exception e) {
            scenario.log("Framework initialization failed: " + e.getMessage());
            throw e;
        }
    }

    @After
    public void afterScenario(Scenario scenario) {

        try {

            String screenshotName =
                    scenario.getName().replaceAll(" ", "_");

            if (scenario.isFailed()) {

                scenario.log("Test Case Failed");

                if (Constant.PAGE != null) {

                    byte[] screenshot = Constant.PAGE.screenshot();

                    scenario.attach(
                            screenshot,
                            "image/png",
                            screenshotName
                    );

                    Allure.addAttachment(
                            screenshotName,
                            "image/png",
                            new ByteArrayInputStream(screenshot),
                            ".png"
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            try {
                if (Constant.writingInExcel) {
                    ExcelUtils.writeInExcelFileUsingStream();
                }
            } catch (Exception ignored) {}

            try {
                PlaywrightManager.closePlaywright();
            } catch (Exception ignored) {}
        }
    }
}
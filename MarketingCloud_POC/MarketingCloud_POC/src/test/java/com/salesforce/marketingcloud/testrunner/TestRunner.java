package com.salesforce.marketingcloud.testrunner;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
		features={"src/test/resources/Feature"},
		glue= {"com/salesforce/marketingcloud/steps","com/salesforce/marketingcloud/hooks"},
		tags= "@Accessibility",
		plugin={"pretty",
				"json:target/cucumber-reports/Cucumber.json",
				"html:test-output",
				"io.qameta.allure.cucumber6jvm.AllureCucumber6Jvm"
						},
		monochrome = true,
		dryRun = false
		)
public class TestRunner {}
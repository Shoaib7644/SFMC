package com.salesforce.marketingcloud.steps;

import com.salesforce.marketingcloud.context.ValidationContext;
import io.cucumber.java.en.Then;
import io.qameta.allure.Allure;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Final reporting step that aggregates soft assertion failures and fails the scenario if needed.
 */
public class ValidationReportingSteps {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationReportingSteps.class);

    @Then("report all validation failures")
    public void reportAllValidationFailures() {
        if (ValidationContext.hasErrors()) {
            String combined = ValidationContext.combinedMessage();
            LOG.error("Validation errors found:\n{}", combined);
            Allure.step("Validation errors found");
            Allure.addAttachment("All Validation Failures", "text/plain", combined, ".txt");
            // Clear after reporting to avoid leakage
            ValidationContext.clear();
            Assert.fail("Validation failures:\n" + combined);
        } else {
            Allure.step("No validation failures recorded");
            ValidationContext.clear();
        }
    }
}

package com.salesforce.marketingcloud.hooks;

import com.salesforce.marketingcloud.context.ValidationContext;
import io.cucumber.java.Before;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber hooks for validation context lifecycle.
 */
public class ValidationHooks {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationHooks.class);

    @Before
    public void beforeScenario() {
        // Clear context to avoid data leakage across scenarios
        ValidationContext.clear();
        Allure.step("ValidationContext cleared before scenario");
        LOG.debug("ValidationContext cleared for new scenario");
    }
}

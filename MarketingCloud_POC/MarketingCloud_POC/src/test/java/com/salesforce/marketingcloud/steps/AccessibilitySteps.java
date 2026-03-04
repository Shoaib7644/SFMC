package com.salesforce.marketingcloud.steps;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.model.accessibility.AccessibilitySummary;
import com.salesforce.marketingcloud.services.AccessibilityService;

import io.cucumber.java.en.Then;
import io.qameta.allure.Allure;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class AccessibilitySteps {

    private static final Logger LOG =
            LoggerFactory.getLogger(AccessibilitySteps.class);

    @Then("validate email accessibility compliance")
    public void validate_email_accessibility_compliance() {

        String html = Constant.latestEmailHtml;

        if (html == null || html.trim().isEmpty()) {
            String msg = "No email HTML found in scenario context (Constant.latestEmailHtml)";
            LOG.error(msg);
            // record as soft failure
            ValidationContext.addError(msg);
            Allure.step(msg);
            return;
        }

        Page page = Constant.PAGE;

        if (page == null) {
            String msg = "Playwright Page is not initialized (Constant.PAGE is null)";
            LOG.error(msg);
            ValidationContext.addError(msg);
            Allure.step(msg);
            return;
        }

        try {

            Allure.step("Loading email HTML into Playwright page");

            page.setContent(html);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            Instant start = Instant.now();

            Allure.step("Running accessibility scan using axe-core");

            AccessibilitySummary summary =
                    AccessibilityService.runAndSummarize(page);

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            String summaryText = String.format(
                    "Scan completed in %d ms | Total=%d, critical=%d, serious=%d, moderate=%d, minor=%d",
                    duration.toMillis(),
                    summary.getTotalViolations(),
                    summary.getCriticalCount(),
                    summary.getSeriousCount(),
                    summary.getModerateCount(),
                    summary.getMinorCount()
            );

            LOG.info(summaryText);

            // Add friendly Allure step and parameters for quick visibility in Allure
            try {
                Allure.step("Accessibility Scan Results");
                Allure.parameter("Critical", String.valueOf(summary.getCriticalCount()));
                Allure.parameter("Serious", String.valueOf(summary.getSeriousCount()));
                Allure.parameter("Moderate", String.valueOf(summary.getModerateCount()));
                Allure.parameter("Minor", String.valueOf(summary.getMinorCount()));
            } catch (Exception e) {
                LOG.warn("Failed to add Allure parameters", e);
            }

            Allure.addAttachment(
                    "Accessibility Summary",
                    "text/plain",
                    summaryText,
                    ".txt"
            );

            if (summary.getRawJson() != null) {
                Allure.addAttachment(
                        "Accessibility Violations JSON",
                        "application/json",
                        summary.getRawJson(),
                        ".json"
                );
            }

            // Only assert once using blocking count. Use soft assertion pattern: capture AssertionError instead of throwing
            try {
                Assert.assertEquals(
                        "Accessibility policy failed. " + summaryText,
                        0,
                        summary.getBlockingCount()
                );
                Allure.step("Accessibility policy passed - no critical/serious violations");
            } catch (AssertionError ae) {
                String msg = "Accessibility policy failed: " + ae.getMessage();
                LOG.error(msg);
                ValidationContext.addError(msg);
                Allure.step(msg);
                Allure.addAttachment("Accessibility Failure", "text/plain", msg, ".txt");
            }

        } catch (Exception e) {
            String msg = "Failed to execute accessibility validation: " + e.getMessage();
            LOG.error(msg, e);
            ValidationContext.addError(msg);
            Allure.step(msg);
            Allure.addAttachment("Accessibility Exception", "text/plain", msg + "\n" + e.toString(), ".txt");
        }
    }
}
package com.salesforce.marketingcloud.steps;

import java.util.List;
import java.util.stream.Collectors;

import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.model.EmailValidationResult;
import com.salesforce.marketingcloud.model.LinkResult;
import com.salesforce.marketingcloud.validator.SmartLinkValidator;
import com.salesforce.marketingcloud.utils.MailosaurUtils;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Assert;
import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;

public class MailSteps {

    private static final Logger LOG = LoggerFactory.getLogger(MailSteps.class);

    @When("Get email from receiver's inbox for {string}")
    public void get_email_from_receivers_inbox_for(String emailName) throws Exception {
        try {
            String html = MailosaurUtils.getLatestEmailHtmlForSubject(emailName, 120);
            if (html == null) {
                throw new AssertionError("Email with name '" + emailName + "' was not found within timeout");
            }
            Constant.latestEmailHtml = html;
            MailosaurUtils.openEmailInPlaywright(html);
        } catch (RuntimeException e) {
            throw new AssertionError("Email not received within timeout: " + e.getMessage());
        }
    }

    @When("Load the latest email HTML into the browser")
    public void load_latest_email_html_into_browser() {
        String html = Constant.latestEmailHtml;
        if (html == null || html.isEmpty()) {
            String msg = "No email HTML available to load into browser";
            LOG.error(msg);
            Allure.step(msg);
            throw new AssertionError(msg);
        }

        Page page = Constant.PAGE;
        try {
            Allure.step("Load latest email HTML into Playwright page");
            if (page == null) {
                // try to create a page from existing context or browser
                if (Constant.BROWSERCONTEXT != null) {
                    page = Constant.BROWSERCONTEXT.newPage();
                } else if (Constant.BROWSER != null) {
                    Constant.BROWSERCONTEXT = Constant.BROWSER.newContext();
                    page = Constant.BROWSERCONTEXT.newPage();
                } else {
                    String msg = "Playwright Browser/Context not initialized (Constant.BROWSER or Constant.BROWSERCONTEXT is null)";
                    LOG.error(msg);
                    Allure.step(msg);
                    throw new AssertionError(msg);
                }
                Constant.PAGE = page;
            }

            page.setContent(html);
            page.waitForLoadState();

            LOG.info("Loaded latest email HTML into browser (page)");
            Allure.step("Loaded latest email HTML into browser");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Exception e) {
            String msg = "Failed to load email HTML into browser: " + e.getMessage();
            LOG.error(msg, e);
            Allure.step(msg);
            throw new AssertionError(msg, e);
        }
    }

    @Then("validate broken links from email")
    public void validate_broken_links_from_email() throws Exception {
        String html = Constant.latestEmailHtml;
        if (html == null || html.isEmpty()) {
            throw new AssertionError("No email HTML available to validate links");
        }

        // extract links from html - reuse existing extractor
        List<String> links = extractLinksFromHtml(html);
        LOG.info("Found {} links in email", links.size());

        EmailValidationResult result = SmartLinkValidator.validateLinks(Constant.latestEmailHtml == null ? "" : "EmailFromMailosaur", links);

        // attach report first
        SmartLinkValidator.attachReportToAllure(result);

        if (result.getBrokenLinks() > 0) {
            LOG.error("Broken links found: {}", result.getOnlyBrokenLinks().stream().map(LinkResult::getUrl).collect(Collectors.joining(", ")));
            Assert.fail(result.generateSummary());
        } else {
            LOG.info("No broken links detected. Summary: {}", result.generateSummary());
        }
    }

    // copied extractor from earlier implementation
    private List<String> extractLinksFromHtml(String html) {
        java.util.List<String> links = new java.util.ArrayList<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("href=\\\"([^\\\"]+)\\\"", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(html);
        while (m.find()) {
            String url = m.group(1).trim();
            if (url.startsWith("mailto:")) continue;
            if (url.startsWith("#")) continue;
            if (url.startsWith("/") || !url.matches("^https?://.*")) continue;
            links.add(url);
        }
        return links;
    }
}
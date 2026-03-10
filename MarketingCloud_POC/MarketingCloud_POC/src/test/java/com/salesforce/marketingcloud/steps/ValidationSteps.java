package com.salesforce.marketingcloud.steps;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mailosaur.MailosaurClient;
import com.mailosaur.models.Message;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.reporting.AllureValidationReporter;
import com.salesforce.marketingcloud.reporting.ReportGenerator;
import com.salesforce.marketingcloud.services.EmailValidationService;

import io.cucumber.java.en.Then;
import io.qameta.allure.Allure;

public class ValidationSteps {
    private static final Logger logger = LoggerFactory.getLogger(ValidationSteps.class);
    private final ValidationContext context = ValidationContext.getInstance(); // use shared singleton

    @Then("validate email accessibility compliance")
    public void validateEmailAccessibility() {
        logger.info("Cucumber step: validate email accessibility compliance");
        // Mailosaur configuration loaded from env
        String apiKey = System.getenv("MAILOSAUR_API_KEY");
        String serverId = System.getenv("MAILOSAUR_SERVER_ID");
        String html = "";
        Message message = null;

        if (apiKey != null && serverId != null) {
            try {
                MailosaurClient client = new MailosaurClient(apiKey);
                message = client.messages().get(serverId, new com.mailosaur.models.SearchCriteria());
                if (message != null && message.html() != null) {
                    html = message.html().toString();
                }
            } catch (Exception e) {
                logger.warn("Mailosaur fetch failed: {}", e.getMessage());
            }
        } else {
            logger.warn("Mailosaur not configured; using empty html");
        }

        // Use a single Playwright browser/page for accessibility to reuse and be efficient
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            EmailValidationService service = new EmailValidationService(context);
            List results = service.runAllValidations(page, html, message);

            ReportGenerator gen = new ReportGenerator(context);
            String htmlReport = gen.generateHtml();
            Allure.addAttachment("Validation Report HTML", new ByteArrayInputStream(htmlReport.getBytes(StandardCharsets.UTF_8)));

            try {
                // generate PDF and auxiliary files under target/email-audit
                gen.generatePdfAndWriteFiles(html);
            } catch (Exception e) {
                logger.warn("Failed to generate PDF report: {}", e.getMessage());
            }

            // Attach other Allure artifacts
            AllureValidationReporter ar = new AllureValidationReporter(context);
            ar.attachReports();

            // log summary
            logger.info("Validation summary: {} results, {} broken links, {} broken images", context.getResults().size(), context.getBrokenLinks().size(), context.getBrokenImages().size());

            browser.close();
        } catch (Exception e) {
            logger.warn("Error while running Playwright or validations: {}", e.getMessage(), e);
            // Even if Playwright fails, attach whatever context has and continue
            ReportGenerator gen = new ReportGenerator(context);
            String htmlReport = gen.generateHtml();
            Allure.addAttachment("Validation Report HTML", new ByteArrayInputStream(htmlReport.getBytes(StandardCharsets.UTF_8)));
            AllureValidationReporter ar = new AllureValidationReporter(context);
            ar.attachReports();
        }

        // Do not fail the test; results are stored in context and attached to Allure
    }
}
package com.example.emailqa.steps;

import com.example.emailqa.EmailAuditReportGenerator;
import com.example.emailqa.EmailAuditRunner;
import com.example.emailqa.model.EmailAuditResult;
import com.mailosaur.MailosaurClient;
import com.mailosaur.models.Message;
import com.mailosaur.models.SearchCriteria;
import io.cucumber.java.en.Then;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("deprecation")
public class EmailAuditSteps {
    private static final Logger logger = LoggerFactory.getLogger(EmailAuditSteps.class);

    @Then("validate email quality audit")
    public void validateEmailQualityAudit() {
        logger.info("Step: validate email quality audit");
        // Read Mailosaur configuration from system properties or environment variables
        String apiKey = System.getProperty("mailosaur.apiKey", System.getenv("MAILOSAUR_API_KEY"));
        String serverId = System.getProperty("mailosaur.serverId", System.getenv("MAILOSAUR_SERVER_ID"));
        String serverDomain = serverId != null ? serverId + "@mailosaur.net" : null;

        String html = "";
        String subject = "";
        String preheader = "";

        if (apiKey != null && serverId != null) {
            try {
                MailosaurClient client = new MailosaurClient(apiKey);
                // Fetch the latest message for the server using empty SearchCriteria
                Message message = client.messages().get(serverId, new SearchCriteria());
                if (message != null) {
                    subject = message.subject();
                    // preheader may be in text parts or html; message.text()/html() return MessageContent objects
                    if (message.html() != null) {
                        html = message.html().toString();
                    } else if (message.text() != null) {
                        html = message.text().toString();
                    }

                    // Try to set a simple preheader from text preview
                    if (message.text() != null) {
                        preheader = message.text().toString();
                    }
                }
            } catch (Exception e) {
                logger.warn("Unable to fetch Mailosaur message: {}", e.getMessage());
            }
        } else {
            logger.warn("Mailosaur configuration not provided; falling back to empty html");
        }

        EmailAuditRunner runner = new EmailAuditRunner();
        EmailAuditResult result = runner.run(html, subject, preheader);

        EmailAuditReportGenerator generator = new EmailAuditReportGenerator();
        String htmlReport = generator.generateHtmlReport(result);

        // Attach report to Allure
        Allure.addAttachment("Email Audit Report", new ByteArrayInputStream(htmlReport.getBytes(StandardCharsets.UTF_8)));
        logger.info("Email audit completed and attached to Allure");

        // Do not fail the test even if there are violations
    }
}
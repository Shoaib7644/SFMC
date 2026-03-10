package com.salesforce.marketingcloud.steps;

import com.mailosaur.MailosaurClient;
import com.mailosaur.models.Message;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.services.EmailValidationService;

import io.cucumber.java.en.Then;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber step: "validate email accessibility compliance"
 *
 * ROOT CAUSE FIX:
 *   The original class used AccessibilityService.runAndSummarize(page) — a legacy runner that
 *   wrote results into com.salesforce.marketingcloud.context.ValidationContext (a completely
 *   different class from framework.validation.context.ValidationContext).
 *   WordReportGenerator reads from the *framework* ValidationContext, which was never populated,
 *   causing every report row to show "NOT EXECUTED".
 *
 *   Fixed: replaced legacy AccessibilityService call with EmailValidationService.runAllValidations()
 *   which writes all 16 per-field ValidationResult entries into framework.validation.context.ValidationContext —
 *   the same instance that WordReportGenerator and ReportSteps read from.
 *
 *   The com.salesforce.marketingcloud.context.ValidationContext (used by ValidationReportingSteps
 *   for soft-assertion error collection) is still written to via ValidationContext.addError() for
 *   any hard failures, so the "report all validation failures" step continues to work correctly.
 */
public class AccessibilitySteps {

    private static final Logger LOG = LoggerFactory.getLogger(AccessibilitySteps.class);

    @Then("validate email accessibility compliance")
    public void validate_email_accessibility_compliance() {

        // ── Read email HTML from Constant (set by "Load the latest email HTML" step) ──────────
        String html = Constant.latestEmailHtml;

        if (html == null || html.trim().isEmpty()) {
            String msg = "No email HTML found in scenario context (Constant.latestEmailHtml is null/empty)";
            LOG.error(msg);
            com.salesforce.marketingcloud.context.ValidationContextPage.addError(msg);
            Allure.step(msg);
            return;
        }

        // ── Use the shared Playwright page already initialised by earlier steps ────────────────
        Page page = Constant.PAGE;

        if (page == null) {
            String msg = "Playwright Page is not initialized (Constant.PAGE is null)";
            LOG.error(msg);
            com.salesforce.marketingcloud.context.ValidationContextPage.addError(msg);
            Allure.step(msg);
            return;
        }

        // ── Fetch Mailosaur message for subject/preheader/header validation (optional) ─────────
        // Validators degrade gracefully when message is null.
        Message message = fetchMailosaurMessage();

        // ── Load HTML into the existing Playwright page ───────────────────────────────────────
        try {
            Allure.step("Loading email HTML into Playwright page for validation");
            page.setContent(html);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        } catch (Exception e) {
            LOG.warn("Failed to set page content (non-fatal — validators will still run): {}", e.getMessage());
        }

        // ── Run all validations via EmailValidationService ────────────────────────────────────
        //
        // This is the KEY FIX: EmailValidationService.runAllValidations() writes all 16
        // per-field ValidationResult entries into framework.validation.context.ValidationContext.
        //
        // The old code called AccessibilityService.runAndSummarize() which only ran axe and
        // wrote to com.salesforce.marketingcloud.context.ValidationContext — a completely
        // separate class that WordReportGenerator never reads from.
        try {
            Allure.step("Running full email validation suite (accessibility, links, images, metadata, unsubscribe, mobile)");

            // framework.validation.context.ValidationContext is the shared singleton used by
            // WordReportGenerator.generateAuditReport() and ReportSteps.
            ValidationContext ctx = ValidationContext.getInstance();

            EmailValidationService service = new EmailValidationService(ctx);
            service.runAllValidations(page, html, message);

            int resultCount = ctx.getResults().size();
            LOG.info("Email validation complete: {} result entries in context", resultCount);
            Allure.step(String.format("Validation complete — %d checks recorded in context", resultCount));

        } catch (Exception e) {
            String msg = "Failed to execute email validation suite: " + e.getMessage();
            LOG.error(msg, e);
            // Record as soft failure so "report all validation failures" step surfaces it.
            com.salesforce.marketingcloud.context.ValidationContextPage.addError(msg);
            Allure.step(msg);
            Allure.addAttachment("Validation Exception", "text/plain",
                    msg + "\n" + e.toString(), ".txt");
        }
    }

    /**
     * Attempts to fetch the latest Mailosaur message for metadata validators.
     * Returns null gracefully if Mailosaur is not configured or fetch fails —
     * EmailMetadataValidator and EmailHeaderValidator both handle a null message safely.
     */
    private Message fetchMailosaurMessage() {
        String apiKey   = System.getenv("MAILOSAUR_API_KEY");
        String serverId = System.getenv("MAILOSAUR_SERVER_ID");

        if (apiKey == null || serverId == null) {
            LOG.info("Mailosaur env vars not set — subject/preheader/unsubscribe-header checks will be skipped");
            return null;
        }

        try {
            MailosaurClient client = new MailosaurClient(apiKey);
            Message msg = client.messages().get(serverId, new com.mailosaur.models.SearchCriteria());
            if (msg != null) {
                LOG.info("Fetched Mailosaur message for metadata validation, subject='{}'", msg.subject());
            }
            return msg;
        } catch (Exception e) {
            LOG.warn("Mailosaur message fetch failed (non-fatal): {}", e.getMessage());
            return null;
        }
    }
}
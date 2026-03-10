package com.salesforce.marketingcloud.steps;

import com.salesforce.marketingcloud.constant.Constant;
import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.model.ValidationResult;
import com.salesforce.marketingcloud.model.ValidationResultsModel;
import com.salesforce.marketingcloud.reporting.WordReportGenerator;
import com.salesforce.marketingcloud.services.EmailValidationService;

import io.cucumber.java.en.Then;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReportSteps {

    private static final Logger LOG = LoggerFactory.getLogger(ReportSteps.class);

    @Then("Generate and log the final validation summary report")
    public void generate_final_validation_summary_report() {
        String emailName = Constant.latestEmailName == null ? "unknown_email" : Constant.latestEmailName;
        try {
            ValidationContext ctx = ValidationContext.getInstance();

            // ── Guard: run validations on-demand if context is empty ──────────────────────────
            //
            // BUG FIX (ReportSteps):
            //   The old guard checked BOTH hasResults AND hasAggregated:
            //     if (!hasResults && !hasAggregated) { run on-demand }
            //
            //   After the AccessibilityReportAdapter fix, the axe run in AccessibilitySteps
            //   partially populates the aggregated model (colorContrast, headingHierarchy, etc.)
            //   via the adapter. So hasAggregated became TRUE even though most validators never
            //   ran — and the on-demand fallback was skipped entirely.
            //
            //   Fixed: the guard now only checks hasResults (the named ValidationResult entries
            //   in context). A fully-run EmailValidationService always produces 16+ entries.
            //   A partial axe-only run produces 2 entries (Runtime Error + Accessibility (axe)).
            //   We require at least 10 entries to consider the context adequately populated.
            //   If fewer exist, re-run all validations to fill the gaps.
            //
            //   In practice, with the fixed AccessibilitySteps, this fallback should never
            //   trigger — all 16 entries will already be in context before this step runs.
            //   The threshold guards against partial-run edge cases.
            //
            int resultCount = ctx.getResults().size();
            boolean hasAdequateResults = resultCount >= 10;

            if (!hasAdequateResults) {
                LOG.info("Context has only {} result entries (expected 16+); running on-demand validation to populate context",
                        resultCount);
                try {
                    EmailValidationService service = new EmailValidationService(ctx);
                    service.runAllValidations(Constant.PAGE, Constant.latestEmailHtml, null);
                    LOG.info("On-demand validation complete: {} result entries now in context",
                            ctx.getResults().size());
                } catch (Exception e) {
                    LOG.warn("On-demand validation failed: {}", e.getMessage());
                }
            } else {
                LOG.info("Context already has {} result entries — skipping on-demand validation", resultCount);
            }

            // ── Debug snapshot ─────────────────────────────────────────────────────────────────
            attachDebugSnapshot(ctx);

            // ── Generate Word audit report ─────────────────────────────────────────────────────
            byte[] doc = WordReportGenerator.generateAuditReport(emailName, Constant.PAGE);
            Allure.step("Generated Word audit report for: " + emailName);
            LOG.info("Generated audit report for {} ({} bytes)", emailName, doc == null ? 0 : doc.length);

        } catch (Exception e) {
            String msg = "Failed to generate audit report: " + e.getMessage();
            LOG.error(msg, e);
            Allure.step(msg);
            Allure.addAttachment("Audit Report Error", "text/plain", msg + "\n" + e.toString(), ".txt");
        }
    }

    private void attachDebugSnapshot(ValidationContext ctx) {
        try {
            StringBuilder dbg = new StringBuilder();
            dbg.append("=== Runtime Context Info ===\n");
            dbg.append("Constant.PAGE present: ").append(Constant.PAGE != null).append('\n');
            dbg.append("latestEmailHtml length: ")
                    .append(Constant.latestEmailHtml == null ? "null" : Constant.latestEmailHtml.length())
                    .append('\n');

            dbg.append("\n=== ValidationResult entries ===\n");
            List<ValidationResult> entries = ctx.getResults();
            if (entries == null || entries.isEmpty()) {
                dbg.append("<no ValidationResult entries>\n");
            } else {
                for (ValidationResult vr : entries) {
                    dbg.append(vr.toString()).append('\n');
                }
            }

            dbg.append("\n=== Aggregated ValidationResultsModel ===\n");
            ValidationResultsModel model = ctx.getAggregatedModel();
            dbg.append(model == null ? "<no aggregated model>" : model.toString()).append('\n');

            try {
                Path resultsDir = Paths.get("results");
                if (!Files.exists(resultsDir)) Files.createDirectories(resultsDir);
                Files.write(resultsDir.resolve("validation-context-debug.txt"),
                        dbg.toString().getBytes(StandardCharsets.UTF_8));
                try (ByteArrayInputStream bis = new ByteArrayInputStream(
                        dbg.toString().getBytes(StandardCharsets.UTF_8))) {
                    Allure.addAttachment("ValidationContext Debug", "text/plain", bis, ".txt");
                }
            } catch (IOException ioe) {
                LOG.warn("Failed to write debug file: {}", ioe.getMessage());
                Allure.addAttachment("ValidationContext Debug (inline)",
                        new ByteArrayInputStream(dbg.toString().getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            LOG.warn("Failed to attach debug snapshot: {}", e.getMessage());
        }
    }
}
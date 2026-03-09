package com.salesforce.marketingcloud.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salesforce.marketingcloud.model.accessibility.AccessibilitySummary;
import com.salesforce.marketingcloud.model.accessibility.AxeResult;
import com.salesforce.marketingcloud.model.accessibility.AxeViolation;
import com.salesforce.marketingcloud.model.accessibility.AxeNode;
import com.salesforce.marketingcloud.reporting.AccessibilityHtmlReportBuilder;
import com.salesforce.marketingcloud.utils.accessibility.AxeAccessibilityUtil;
import com.salesforce.marketingcloud.utils.accessibility.AccessibilityException;
import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.lang.reflect.Type;

// new imports for framework validation context and adapter
import framework.validation.context.ValidationContext;
import framework.validation.reporting.AccessibilityReportAdapter;
import framework.validation.model.ValidationResultsModel;

// Import TypeToken for safe Gson deserialization
import com.google.gson.reflect.TypeToken;

public final class AccessibilityService {

    private static final Logger LOG = LoggerFactory.getLogger(AccessibilityService.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AccessibilityService() {}

    public static AccessibilitySummary runAndSummarize(Page page) {
        return runAndSummarize(page, null);
    }

    public static AccessibilitySummary runAndSummarize(Page page, List<String> tags) {

        Objects.requireNonNull(page, "page must not be null");

        try {
            Allure.step("Execute axe accessibility scan");

            String rawJson = AxeAccessibilityUtil.runAxeAnalysis(page, tags);
            AxeResult result = GSON.fromJson(rawJson, AxeResult.class);

            int critical = 0, serious = 0, moderate = 0, minor = 0;
            int total = 0;

            if (result != null && result.getViolations() != null) {
                total = result.getViolations().size();

                for (AxeViolation v : result.getViolations()) {
                    String impact = v.getImpact() == null ? "" : v.getImpact().toLowerCase();

                    switch (impact) {
                        case "critical": critical++; break;
                        case "serious": serious++; break;
                        case "moderate": moderate++; break;
                        case "minor": minor++; break;
                        default: break;
                    }
                }
            }

            AccessibilitySummary summary = new AccessibilitySummary(
                    total,      // totalViolations
                    critical,   // criticalCount
                    serious,    // seriousCount
                    moderate,   // moderateCount
                    minor,      // minorCount
                    rawJson,    // rawJson
                    result      // axeResult
            );

            // Structured logging - human readable and enterprise friendly
            logStructuredAccessibilityReport(result, summary);

            // Populate framework ValidationContext so standalone AccessibilityService runs create structured results
            try {
                ValidationContext ctx = ValidationContext.getInstance();

                // set raw violations list (as generic objects) by parsing the rawJson into a Map and extracting "violations"
                List<Object> violationsRaw = new ArrayList<>();
                if (rawJson != null && !rawJson.isBlank()) {
                    // Use TypeToken to avoid unchecked cast warnings
                    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> root = GSON.fromJson(rawJson, mapType);
                    Object viol = root == null ? null : root.get("violations");
                    if (viol instanceof List) {
                        for (Object item : (List<?>) viol) {
                            violationsRaw.add(item);
                        }
                    }
                }
                ctx.setAxeViolations(violationsRaw);

                // set a simple impact summary map
                Map<String, Integer> summaryMap = new LinkedHashMap<>();
                summaryMap.put("critical", summary.getCriticalCount());
                summaryMap.put("serious", summary.getSeriousCount());
                summaryMap.put("moderate", summary.getModerateCount());
                summaryMap.put("minor", summary.getMinorCount());
                ctx.setAxeSummary(summaryMap);

                // use adapter to map axe violations into the aggregated ValidationResultsModel and also populate per-rule ValidationResult entries
                AccessibilityReportAdapter adapter = new AccessibilityReportAdapter(ctx);
                ValidationResultsModel agg = adapter.adapt();
                ctx.setAggregatedModel(agg);

                // overall accessibility pass/fail
                boolean passed = summary.getTotalViolations() == 0;
                ctx.addResult("Accessibility (axe)", passed, passed ? "No axe violations for mapped rules" : "Axe violations detected - check detailed items", passed ? "INFO" : "MAJOR");

            } catch (Exception e) {
                LOG.warn("Failed to populate framework ValidationContext from AccessibilityService: {}", e.getMessage());
            }

            // Attach executive summary HTML to Allure
            try {
                String executiveHtml = AccessibilityHtmlReportBuilder.buildExecutiveSummaryHtml(summary);
                Allure.addAttachment("Accessibility Executive Summary", "text/html", executiveHtml, ".html");
            } catch (Exception e) {
                LOG.warn("Failed to attach executive HTML summary", e);
            }

            // Add Allure parameters for quick CI visibility
            try {
                Allure.step("Accessibility Scan Results");
                Allure.parameter("Critical", String.valueOf(summary.getCriticalCount()));
                Allure.parameter("Serious", String.valueOf(summary.getSeriousCount()));
                Allure.parameter("Moderate", String.valueOf(summary.getModerateCount()));
                Allure.parameter("Minor", String.valueOf(summary.getMinorCount()));
            } catch (Exception e) {
                LOG.warn("Failed to add Allure parameters", e);
            }

            attachToAllure(summary);

            return summary;

        } catch (AccessibilityException ae) {
            LOG.error("Accessibility execution failed", ae);
            throw new RuntimeException("Accessibility execution failed", ae);
        }
    }

    /**
     * Logs a detailed, structured accessibility report.
     * Output includes totals and grouped violations by severity. For each violation we log
     * Rule ID, Description, affected element targets and failure summary (first node or all nodes).
     */
    private static void logStructuredAccessibilityReport(AxeResult result, AccessibilitySummary summary) {
        // Header with totals
        LOG.info("\n===== Accessibility Scan Report =====");
        LOG.info("Total Violations: {}", summary.getTotalViolations());
        LOG.info("Critical: {} | Serious: {} | Moderate: {} | Minor: {}",
                summary.getCriticalCount(), summary.getSeriousCount(), summary.getModerateCount(), summary.getMinorCount());

        if (result == null || result.getViolations() == null || result.getViolations().isEmpty()) {
            LOG.info("No violations found.");
            LOG.info("===== End Accessibility Scan Report =====\n");
            return;
        }

        // Group violations by impact while preserving order: critical, serious, moderate, minor, others
        Map<String, List<AxeViolation>> grouped = new LinkedHashMap<>();
        grouped.put("critical", new ArrayList<>());
        grouped.put("serious", new ArrayList<>());
        grouped.put("moderate", new ArrayList<>());
        grouped.put("minor", new ArrayList<>());
        grouped.put("other", new ArrayList<>());

        for (AxeViolation v : result.getViolations()) {
            String impact = v.getImpact() == null ? "other" : v.getImpact().toLowerCase();
            if (!grouped.containsKey(impact)) impact = "other";
            grouped.get(impact).add(v);
        }

        // For each severity, print grouped violations
        for (Map.Entry<String, List<AxeViolation>> entry : grouped.entrySet()) {
            String severity = entry.getKey();
            List<AxeViolation> violations = entry.getValue();
            if (violations.isEmpty()) continue;

            LOG.info("\n--- {} violations (count={}) ---", severity.toUpperCase(), violations.size());

            int idx = 1;
            for (AxeViolation v : violations) {
                LOG.info("[{}] Rule ID: {}", idx++, v.getId());
                LOG.info("     Description: {}", v.getDescription());
                // If nodes exist, list their targets and failureSummary
                if (v.getNodes() != null && !v.getNodes().isEmpty()) {
                    int nodeIdx = 1;
                    for (AxeNode n : v.getNodes()) {
                        String targets = n.getTarget() == null ? "[]" : n.getTarget().toString();
                        String failure = n.getFailureSummary() == null ? "" : n.getFailureSummary();
                        LOG.info("     Affected[{}]: {}", nodeIdx, targets);
                        if (!failure.isEmpty()) LOG.info("         Failure Summary: {}", failure);
                        nodeIdx++;
                    }
                } else {
                    LOG.info("     Affected: [no node information]");
                }
            }
        }

        LOG.info("===== End Accessibility Scan Report =====\n");
    }

    private static void attachToAllure(AccessibilitySummary summary) {
        if (summary == null) return;

        Allure.addAttachment(
                "axe-results.json",
                "application/json",
                summary.getRawJson(),
                ".json"
        );

        String severity = String.format(
                "critical:%d, serious:%d, moderate:%d, minor:%d",
                summary.getCriticalCount(),
                summary.getSeriousCount(),
                summary.getModerateCount(),
                summary.getMinorCount()
        );

        Allure.addAttachment(
                "accessibility-severity.txt",
                "text/plain",
                severity,
                ".txt"
        );

        try {
            byte[] bytes = summary.getRawJson() == null
                    ? new byte[0]
                    : summary.getRawJson().getBytes(StandardCharsets.UTF_8);

            Allure.addAttachment(
                    "axe-results-stream.json",
                    new ByteArrayInputStream(bytes)
            );
        } catch (Exception e) {
            LOG.warn("Failed to attach JSON stream", e);
        }
    }
}
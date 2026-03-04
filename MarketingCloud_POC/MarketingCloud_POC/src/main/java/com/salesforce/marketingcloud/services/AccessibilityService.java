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
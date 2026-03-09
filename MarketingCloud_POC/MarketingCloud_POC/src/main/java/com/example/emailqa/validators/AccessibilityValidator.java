package com.example.emailqa.validators;

import com.example.emailqa.model.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AccessibilityValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(AccessibilityValidator.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Starting Accessibility (Axe) validation");
        List<String> issues = new ArrayList<>();
        List<String> affected = new ArrayList<>();

        String axeScriptUrl = System.getProperty("axe.script.url", System.getenv("AXE_SCRIPT_URL"));
        if (axeScriptUrl == null || axeScriptUrl.isEmpty()) {
            axeScriptUrl = "https://cdnjs.cloudflare.com/ajax/libs/axe-core/4.8.4/axe.min.js";
        }

        String rawJson = null;
        try (Playwright pw = Playwright.create()) {
            var browser = pw.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setContent(emailHtml == null ? "" : emailHtml);

            try {
                // Fetch and inject axe script
                String fetchScript = "async url => { const res = await fetch(url); const txt = await res.text(); eval(txt); return true; }";
                page.evaluate(fetchScript, axeScriptUrl);

                // Run axe
                String axeRun = "async () => { return await axe.run(); }";
                Object result = page.evaluate(axeRun);
                rawJson = mapper.writeValueAsString(result);

                JsonNode root = mapper.readTree(rawJson);
                JsonNode violations = root.path("violations");
                if (violations.isArray()) {
                    Iterator<JsonNode> it = violations.elements();
                    while (it.hasNext()) {
                        JsonNode v = it.next();
                        String id = v.path("id").asText();
                        String description = v.path("description").asText();
                        JsonNode nodes = v.path("nodes");
                        List<String> nodeTargets = new ArrayList<>();
                        if (nodes.isArray()) {
                            for (JsonNode n : nodes) {
                                JsonNode target = n.path("target");
                                if (target.isArray() && target.size() > 0) {
                                    nodeTargets.add(target.get(0).asText());
                                }
                            }
                        }
                        String issue = String.format("%s: %s (targets=%s)", id, description, nodeTargets);
                        issues.add(issue);
                        affected.addAll(nodeTargets);
                    }
                }

                browser.close();
            } catch (Exception e) {
                logger.warn("Error running axe on page: {}", e.getMessage(), e);
                issues.add("Error running axe: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.warn("Error launching Playwright or running accessibility checks: {}", e.getMessage(), e);
            issues.add("Playwright/axe error: " + e.getMessage());
        }

        // Attach raw axe JSON if available
        if (rawJson != null) {
            Allure.addAttachment("axe.raw.json", new ByteArrayInputStream(rawJson.getBytes(StandardCharsets.UTF_8)));
        }

        boolean colorContrastFail = issues.stream().anyMatch(s -> s.contains("color-contrast") || s.contains("color_contrast") || s.contains("color contrast"));

        boolean passed = issues.isEmpty();
        String notes = passed ? "No accessibility violations detected by axe" : String.join("; ", issues);
        ValidationResult result = ValidationResult.builder("Accessibility (Axe)")
                .passed(passed)
                .severity(colorContrastFail ? "CRITICAL" : (passed ? "INFO" : "MAJOR"))
                .notes(notes)
                .issues(issues)
                .affectedElements(affected)
                .build();
        logger.info("Completed Accessibility validation: {}", result.isPassed() ? "PASS" : "FAIL");
        return result;
    }
}
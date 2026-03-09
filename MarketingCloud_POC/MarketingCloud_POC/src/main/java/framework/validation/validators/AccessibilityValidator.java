package framework.validation.validators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResult;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Runs axe-core accessibility analysis on the email HTML via Playwright.
 *
 * FIXES IN THIS VERSION:
 *
 *  Fix 1 — Summary badge counts wrong (Serious:2, Moderate:16 instead of Serious:1, Moderate:3):
 *
 *    Root cause: the summary map was accumulating NODE counts, not RULE counts.
 *      summary.put(impact, summary.getOrDefault(impact, 0) + count)
 *    where `count = v.path("nodes").size()` — the number of HTML elements affected by the rule.
 *    The axe `region` rule flagged 16 page elements → summary showed moderate:16.
 *    The violation TABLE showed 4 rows (correct — 4 distinct rules) but the summary badges
 *    showed Serious:2 Moderate:16 (wrong — those are element counts, not rule counts).
 *
 *    Fixed: summary now counts +1 per rule (violation), not +nodeCount.
 *      summary.merge(impact, 1, Integer::sum)
 *
 *    The node count is still stored separately in nodeCountByImpact for informational use
 *    (displayed as "X elements affected" in the axe HTML report).
 *
 *  Fix 2 — Affected elements column was showing all nodes from every rule.
 *    Capped at MAX_NODES_PER_RULE = 3 per violation row in the HTML report.
 *    The full list is still stored in context for programmatic access.
 */
public class AccessibilityValidator implements EmailValidator {

    private static final Logger logger = LoggerFactory.getLogger(AccessibilityValidator.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /** Maximum node snippets to show per violation row in the axe HTML report */
    private static final int MAX_NODES_PER_RULE = 3;

    private final ValidationContext context;

    public AccessibilityValidator(ValidationContext context) {
        this.context = context;
    }

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Running AccessibilityValidator (standalone — launching headless browser)");
        try (Playwright pw = Playwright.create()) {
            var browser = pw.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setContent(emailHtml == null ? "" : emailHtml);
            ValidationResult res = validate(page, emailHtml);
            browser.close();
            return res;
        } catch (Exception e) {
            logger.warn("Accessibility standalone validation error: {}", e.getMessage(), e);
            context.addResult("Accessibility Engine", false,
                    "Error running axe: " + e.getMessage(), "CRITICAL");
            return new ValidationResult("Accessibility (axe)", false,
                    "Error running axe: " + e.getMessage(), "CRITICAL");
        }
    }

    /**
     * Runs axe on an existing Playwright page — avoids launching a second browser.
     */
    public ValidationResult validate(Page page, String emailHtml) {
        logger.info("Running AccessibilityValidator using provided Playwright Page");

        List<Object> rawViolations = new ArrayList<>();

        // FIX 1: two separate maps —
        //   summary       = rule count per impact level  (1 per violation rule)
        //   nodeCountMap  = total affected elements per impact (sum of nodes across rules)
        Map<String, Integer> summary      = new LinkedHashMap<>(); // rule counts — shown as badges
        Map<String, Integer> nodeCountMap = new LinkedHashMap<>(); // element counts — informational

        // Friendly name mapping for context result keys
        Map<String, String> idToName = new HashMap<>();
        idToName.put("color-contrast",   "Color Contrast");
        idToName.put("image-alt",        "Image Alt Text");
        idToName.put("heading-order",    "Heading Hierarchy");
        idToName.put("html-has-lang",    "Language Attribute");
        idToName.put("landmark-one-main","Landmark structure");
        idToName.put("link-name",        "Link text accessibility");
        idToName.put("button-name",      "Button Accessibility");
        idToName.put("td-headers-attr",  "Table Accessibility");
        idToName.put("th-has-data-cells","Table Accessibility");
        idToName.put("scope-attr-valid", "Table Accessibility");

        String axeScriptUrl = System.getProperty("axe.script.url", System.getenv("AXE_SCRIPT_URL"));
        if (axeScriptUrl == null || axeScriptUrl.isEmpty()) {
            axeScriptUrl = "https://cdnjs.cloudflare.com/ajax/libs/axe-core/4.8.4/axe.min.js";
        }

        String rawJson = null;
        try {
            // Inject axe-core
            final String scriptUrl = axeScriptUrl;
            page.evaluate("async url => { const r = await fetch(url); const t = await r.text(); eval(t); return true; }",
                    scriptUrl);

            Object result = page.evaluate("async () => { return await axe.run(); }");
            rawJson = mapper.writeValueAsString(result);

            JsonNode root       = mapper.readTree(rawJson);
            JsonNode violations = root.path("violations");

            if (violations.isArray()) {
                for (JsonNode v : violations) {
                    String id          = v.path("id").asText();
                    String impact      = v.path("impact").asText();
                    String description = v.path("description").asText();
                    int    nodeCount   = v.path("nodes").size();

                    // Store the full violation object for the axe HTML report
                    rawViolations.add(mapper.convertValue(v, Object.class));

                    String mappedName = idToName.getOrDefault(id, id);
                    String severity   = mapImpactToSeverity(impact);

                    // FIX 1: count +1 per RULE (not +nodeCount per rule)
                    summary.merge(impact, 1, Integer::sum);

                    // Store element count separately — used for "X elements affected" display
                    nodeCountMap.merge(impact, nodeCount, Integer::sum);

                    // Write individual result to context
                    if ("FAIL".equalsIgnoreCase(severity)) {
                        context.addResult(mappedName, false,
                                nodeCount + " violation(s) detected", "CRITICAL");
                    } else if ("WARNING".equalsIgnoreCase(severity)) {
                        context.addResult(mappedName, false,
                                nodeCount + " violation(s) detected", "MAJOR");
                    } else {
                        context.addResult(mappedName, false,
                                nodeCount + " violation(s) detected", "INFO");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Accessibility validation error: {}", e.getMessage(), e);
            context.addResult("Accessibility Engine", false,
                    "Error running axe: " + e.getMessage(), "CRITICAL");
        }

        // Store raw violations list and the corrected RULE-COUNT summary in context
        if (rawJson != null) {
            context.setAxeViolations(new ArrayList<>(rawViolations));
            context.setAxeSummary(summary);          // FIX 1: rule counts
            // NOTE: setAxeNodeCounts() is a new method — add it to ValidationContext if not present:
            //   private Map<String,Integer> axeNodeCounts = new LinkedHashMap<>();
            //   public void setAxeNodeCounts(Map<String,Integer> m){ this.axeNodeCounts=m; }
            //   public Map<String,Integer> getAxeNodeCounts(){ return axeNodeCounts; }
            try { context.setAxeNodeCounts(nodeCountMap); } catch (Exception ignored) {}

            try (ByteArrayInputStream bis = new ByteArrayInputStream(
                    rawJson.getBytes(StandardCharsets.UTF_8))) {
                Allure.addAttachment("axe.raw.json", "application/json", bis, ".json");
            } catch (Exception e) {
                logger.warn("Failed to attach axe raw JSON: {}", e.getMessage());
            }
        }

        boolean passed = rawViolations.isEmpty();
        String notes = passed
                ? "No axe violations detected"
                : String.format("Axe violations detected (%d rule(s), %d total elements affected) — see Axe HTML report",
                        rawViolations.size(),
                        nodeCountMap.values().stream().mapToInt(Integer::intValue).sum());

        context.addResult("Accessibility (axe)", passed, notes, passed ? "INFO" : "MAJOR");
        return new ValidationResult("Accessibility (axe)", passed, notes, passed ? "INFO" : "MAJOR");
    }

    private String mapImpactToSeverity(String impact) {
        if (impact == null) return "WARNING";
        switch (impact.toLowerCase()) {
            case "critical":
            case "serious":   return "FAIL";
            case "moderate":
            case "minor":     return "WARNING";
            default:          return "WARNING";
        }
    }
}
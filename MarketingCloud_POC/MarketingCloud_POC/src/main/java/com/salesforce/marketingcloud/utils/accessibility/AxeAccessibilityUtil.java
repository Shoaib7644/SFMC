package com.salesforce.marketingcloud.utils.accessibility;

import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility to run Axe accessibility checks inside Playwright Page.
 * <p>
 * Usage:
 * String resultJson = AxeAccessibilityUtil.runAxeAnalysis(page, List.of("wcag2a","wcag2aa"));
 */
public final class AxeAccessibilityUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AxeAccessibilityUtil.class);
    private static final String AXE_SCRIPT_CLASSPATH = "/axe/axe.min.js"; // place axe.min.js under src/test/resources/axe/axe.min.js

    private AxeAccessibilityUtil() {
        // utility
    }

    /**
     * Injects axe script from classpath into the given Playwright Page and runs axe.run().
     * Returns the full axe results JSON string.
     *
     * @param page the Playwright Page instance (non-null)
     * @param tags optional list of axe tags to filter rules; if null or empty, default rules run
     * @return full axe results JSON string
     * @throws AccessibilityException when loading/injecting/executing axe fails
     */
    public static String runAxeAnalysis(Page page, List<String> tags) throws AccessibilityException {
        Objects.requireNonNull(page, "Page must not be null");

        try {
            // load axe script content
            String axeSource = loadAxeScriptFromResources();

            // inject axe script into page
            page.addScriptTag(new Page.AddScriptTagOptions().setContent(axeSource));

            // build the async JS to run axe
            String tagFilterJs = buildTagFilterJs(tags);

            String evalScript = "(async () => {" +
                    "  if (!window.axe) { console.error('axe not found on window'); return JSON.stringify({error: 'axe not injected'}); }" +
                    "  try {" +
                    "    const options = { runOnly: {} };" +
                    tagFilterJs +
                    "    const results = await window.axe.run(document, options);" +
                    "    return JSON.stringify(results);" +
                    "  } catch(e) { return JSON.stringify({error: e && e.message ? e.message : String(e) }); }" +
                    "})()";

            // evaluate on page and wait for the promise
            Object raw = page.evaluate(evalScript);
            if (raw == null) {
                throw new AccessibilityException("Axe evaluation returned null");
            }
            String json = raw.toString();

            // check for axe error payload
            if (json.contains("\"error\"")) {
                LOG.error("Axe returned an error payload: {}", json);
                throw new AccessibilityException("Axe returned an error: " + json);
            }

            return json;
        } catch (IOException ioe) {
            throw new AccessibilityException("Failed to load axe script from resources", ioe);
        } catch (RuntimeException re) {
            throw new AccessibilityException("Runtime error executing axe on page", re);
        }
    }

    private static String buildTagFilterJs(List<String> tags) {
        // Check if tags are null, empty, or contain only empty strings
        if (tags == null || tags.isEmpty() || tags.stream().allMatch(String::isEmpty)) {
            return "\n    // No tags provided - removing runOnly to run default rules\n" +
                   "    delete options.runOnly;\n";
        }

        // Build the array: ["wcag2a", "wcag2aa"]
        String values = tags.stream()
                .filter(s -> s != null && !s.trim().isEmpty()) // Filter out nulls/blanks
                .map(s -> "\"" + escapeJs(s) + "\"")
                .collect(Collectors.joining(","));

        // If after filtering we have no values, delete the runOnly option
        if (values.isEmpty()) {
            return "\n    delete options.runOnly;\n";
        }

        return "\n    options.runOnly = { type: 'tag', values: [" + values + "] };\n";
    }

    private static String escapeJs(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String loadAxeScriptFromResources() throws IOException, AccessibilityException {
        InputStream is = AxeAccessibilityUtil.class.getResourceAsStream(AXE_SCRIPT_CLASSPATH);
        if (is == null) {
            throw new AccessibilityException("axe.min.js not found on classpath at: " + AXE_SCRIPT_CLASSPATH);
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }
}

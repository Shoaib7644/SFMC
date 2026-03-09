package com.example.emailqa.validators;

import com.example.emailqa.model.ValidationResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class BrokenLinkValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(BrokenLinkValidator.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Starting BrokenLink validation");
        Document doc = Jsoup.parse(emailHtml == null ? "" : emailHtml);
        Elements anchors = doc.select("a[href]");
        List<String> issues = new ArrayList<>();
        List<String> affected = new ArrayList<>();

        if (anchors.isEmpty()) {
            logger.debug("No anchor tags found in email");
        }

        for (Element a : anchors) {
            String href = a.attr("href").trim();
            if (href.isEmpty()) continue;
            // Ignore mailto and tel and anchors
            if (href.startsWith("mailto:") || href.startsWith("tel:") || href.startsWith("#")) continue;
            affected.add(href);

            try {
                int code = checkUrlStatus(href);
                if (code >= 400) {
                    issues.add(String.format("Broken link (%d): %s", code, href));
                } else {
                    logger.debug("Link OK (%d): %s", code, href);
                }
            } catch (IOException e) {
                logger.warn("Error checking link {}: {}", href, e.getMessage());
                issues.add(String.format("Error checking link: %s (%s)", href, e.getMessage()));
            }
        }

        boolean passed = issues.isEmpty();
        String notes = passed ? "No broken links found" : String.join("; ", issues);
        ValidationResult result = ValidationResult.builder("Broken Links")
                .passed(passed)
                .severity(passed ? "INFO" : "CRITICAL")
                .notes(notes)
                .issues(issues)
                .affectedElements(affected)
                .build();
        logger.info("Completed BrokenLink validation: {}", result.isPassed() ? "PASS" : "FAIL");
        return result;
    }

    private int checkUrlStatus(String href) throws IOException {
        URL url = new URL(href);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            // Some servers block HEAD; try HEAD first, fallback to GET
            conn.setRequestMethod("HEAD");
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_BAD_METHOD || code == HttpURLConnection.HTTP_CLIENT_TIMEOUT) {
                // fallback
            }
            if (code >= 400 && conn.getRequestMethod().equals("HEAD")) {
                // Try GET as a fallback for servers that block HEAD
                conn.disconnect();
                HttpURLConnection conn2 = (HttpURLConnection) url.openConnection();
                try {
                    conn2.setConnectTimeout(CONNECT_TIMEOUT_MS);
                    conn2.setReadTimeout(READ_TIMEOUT_MS);
                    conn2.setInstanceFollowRedirects(true);
                    conn2.setRequestMethod("GET");
                    return conn2.getResponseCode();
                } finally {
                    conn2.disconnect();
                }
            }
            return code;
        } finally {
            conn.disconnect();
        }
    }
}
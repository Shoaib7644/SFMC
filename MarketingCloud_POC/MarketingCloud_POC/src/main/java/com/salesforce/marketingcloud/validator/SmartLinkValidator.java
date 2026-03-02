package com.salesforce.marketingcloud.validator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salesforce.marketingcloud.model.EmailValidationResult;
import com.salesforce.marketingcloud.model.LinkResult;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates links found inside email content and prepares a structured report.
 * Thread-safe and non-throwing within per-link processing.
 */
public final class SmartLinkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartLinkValidator.class);

    private SmartLinkValidator() {
        // utility
    }

    /**
     * Validate links using HTTP GET. Returns EmailValidationResult with per-link outcomes.
     *
     * @param emailSubject subject or name of the email (for reporting)
     * @param links list of absolute URLs to validate
     * @return EmailValidationResult summarizing results
     */
    public static EmailValidationResult validateLinks(String emailSubject, List<String> links) {
        List<LinkResult> results = new ArrayList<>();
        if (links == null || links.isEmpty()) {
            LOGGER.info("No links provided for validation for email={}", emailSubject);
            return new EmailValidationResult(emailSubject, results);
        }

        for (String link : links) {
            LinkResult lr = validateSingleLink(link);
            results.add(lr);
            LOGGER.debug("Validated link: {} -> {}", link, lr);
        }

        EmailValidationResult summary = new EmailValidationResult(emailSubject, results);
        LOGGER.info("Link validation complete for email='{}', totalLinks={}, brokenLinks={}", emailSubject, summary.getTotalLinks(), summary.getBrokenLinks());
        return summary;
    }

    private static LinkResult validateSingleLink(String link) {
        int status = -1;
        String error = null;
        boolean broken = false;

        HttpURLConnection conn = null;
        try {
            URL url = new URL(link);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; SFMC-LinkValidator/1.0)");
            conn.connect();
            status = conn.getResponseCode();
            if (status >= 400) {
                broken = true;
            }
        } catch (IOException e) {
            broken = true;
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            LOGGER.warn("Exception validating link {}: {}", link, error);
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }

        return new LinkResult(link, status, error, broken);
    }

    /**
     * Attach JSON and HTML summary into Allure report for the provided EmailValidationResult.
     */
    public static void attachReportToAllure(EmailValidationResult result) {
        if (result == null) return;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(result);
        Allure.addAttachment("Email Link Validation - JSON", "application/json", json, ".json");

        // build small HTML table
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"utf-8\"></head><body>");
        html.append("<h3>Email Link Validation Summary</h3>");
        html.append("<p><strong>Subject:</strong> ").append(escapeHtml(result.getEmailSubject())).append("</p>");
        html.append("<p><strong>Total Links:</strong> ").append(result.getTotalLinks()).append(" &nbsp;&nbsp; <strong>Broken Links:</strong> ").append(result.getBrokenLinks()).append("</p>");

        if (result.getBrokenLinks() > 0) {
            html.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">\n");
            html.append("<tr><th>URL</th><th>Status</th><th>Error</th></tr>");
            for (LinkResult lr : result.getOnlyBrokenLinks()) {
                html.append("<tr>");
                html.append("<td><a href=\"").append(escapeHtml(lr.getUrl())).append("\">\"").append(escapeHtml(lr.getUrl())).append("</a></td>");
                html.append("<td>").append(lr.getStatusCode() <= 0 ? "N/A" : lr.getStatusCode()).append("</td>");
                html.append("<td>").append(escapeHtml(lr.getErrorMessage())).append("</td>");
                html.append("</tr>");
            }
            html.append("</table>");
        } else {
            html.append("<p>No broken links detected.</p>");
        }

        html.append("</body></html>");
        Allure.addAttachment("Email Link Validation - HTML", "text/html", html.toString(), ".html");

        LOGGER.info("Allure attachments added for email='{}'", result.getEmailSubject());
    }

    private static String escapeHtml(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}

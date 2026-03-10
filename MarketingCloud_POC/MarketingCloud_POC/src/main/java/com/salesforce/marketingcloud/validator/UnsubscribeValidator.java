package com.salesforce.marketingcloud.validator;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.model.ValidationResult;

public class UnsubscribeValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(UnsubscribeValidator.class);
    private final ValidationContext context;

    public UnsubscribeValidator(ValidationContext context) {
        this.context = context;
    }

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Running UnsubscribeValidator");
        Document doc = Jsoup.parse(emailHtml == null ? "" : emailHtml);
        Elements anchors = doc.select("a[href]");
        boolean found = false;
        List<String> hints = new ArrayList<>();

        // 1) Check anchors for common unsubscribe patterns including mailto and subscription center
        for (Element a : anchors) {
            String href = a.attr("href").toLowerCase();
            String text = a.text().toLowerCase();

            // mailto unsubscribe (mailto:unsubscribe@... or mailto:...subject=unsubscribe)
            if (href.startsWith("mailto:") && (href.contains("unsubscribe") || text.contains("unsubscribe"))) {
                found = true;
                hints.add(href);
                continue;
            }

            // subscription center / profile center patterns used by some ESPs
            if (href.contains("subscription_center") || href.contains("subscriptioncenter") || href.contains("profile_center") || href.contains("profilecenter")) {
                found = true;
                hints.add(href);
                continue;
            }

            // generic unsubscribe/optout/link text checks
            if (href.contains("unsubscribe") || text.contains("unsubscribe") || href.contains("optout") || text.contains("optout") || href.contains("managepreferences") || text.contains("manage preferences") || href.contains("unsubscribe.aspx") || href.contains("/unsubscribe/")) {
                found = true;
                hints.add(href);
            }
        }

        // 2) Check raw html for tokens (SFMC tokens or list-unsubscribe headers embedded)
        if (!found) {
            String lower = (emailHtml == null) ? "" : emailHtml.toLowerCase();
            if (lower.contains("%unsubscribe%") || lower.contains("%%unsubscribe%%") || lower.contains("optout") || lower.contains("manage preferences") || lower.contains("subscription_center") || lower.contains("subscriptioncenter")) {
                found = true;
                hints.add("token or keyword present");
            }
        }

        // 3) Consult ValidationContext for List-Unsubscribe header result if present (some upstream validators may have recorded it)
        try {
            ValidationResult headerRes = context.getResultByName("List-Unsubscribe");
            if (headerRes == null) headerRes = context.getResultByName("List-Unsubscribe Header");
            if (headerRes == null) headerRes = context.getResultByName("List-Unsubscribe Header");
            if (headerRes != null) {
                if (headerRes.isPassed()) {
                    found = true;
                    hints.add("List-Unsubscribe header present");
                } else {
                    hints.add("List-Unsubscribe header absent or failed");
                }
            }
        } catch (Exception e) {
            logger.debug("No header info in context: {}", e.getMessage());
        }

        boolean passed = found;
        String notes = passed ? "Unsubscribe/opt-out link or token found" : "No unsubscribe mechanism detected in email";
        String evidence = String.join("; ", hints);
        context.addResult("Unsubscribe Link", passed, notes + (evidence.isEmpty() ? "" : ": " + evidence), passed ? "INFO" : "CRITICAL");
        return new ValidationResult("Unsubscribe Link", passed, notes + (evidence.isEmpty() ? "" : ": " + evidence), passed ? "INFO" : "CRITICAL");
    }
}
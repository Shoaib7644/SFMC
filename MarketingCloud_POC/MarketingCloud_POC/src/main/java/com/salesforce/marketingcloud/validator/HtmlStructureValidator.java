package com.salesforce.marketingcloud.validator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.model.ValidationResultsModel;

/**
 * Validates basic HTML structure such as language attribute and headings.
 */
public class HtmlStructureValidator {
    private static final Logger logger = LoggerFactory.getLogger(HtmlStructureValidator.class);
    private final ValidationContext context;

    public HtmlStructureValidator(ValidationContext context) {
        this.context = context;
    }

    public ValidationResultsModel.FieldResult validateLanguageAttribute(String html) {
        try {
            Document doc = Jsoup.parse(html == null ? "" : html);
            String lang = doc.selectFirst("html") != null ? doc.selectFirst("html").attr("lang") : null;
            if (lang != null && !lang.trim().isEmpty()) {
                return new ValidationResultsModel.FieldResult("PASS", "lang attribute present: " + lang, "html.lang=" + lang);
            } else {
                return new ValidationResultsModel.FieldResult("FAIL", "Missing html lang attribute", "");
            }
        } catch (Exception e) {
            logger.warn("Error validating language attribute: {}", e.getMessage());
            return new ValidationResultsModel.FieldResult("WARN", "Error extracting language attribute: " + e.getMessage(), "");
        }
    }
}

package com.example.emailqa.validators;

import com.example.emailqa.model.ValidationResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MobileResponsivenessValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(MobileResponsivenessValidator.class);

    private static final Pattern MEDIA_QUERY_PATTERN = Pattern.compile("@media\\s*\\([^)]+\\)", Pattern.CASE_INSENSITIVE);

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Starting MobileResponsiveness validation");
        List<String> issues = new ArrayList<>();
        List<String> affected = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(emailHtml == null ? "" : emailHtml);

            // Meta viewport
            Element viewport = doc.selectFirst("meta[name=viewport]");
            if (viewport == null) {
                logger.debug("meta viewport not found");
                issues.add("Missing meta viewport tag");
            } else {
                affected.add("meta[name=viewport]=" + viewport.attr("content"));
            }

            // Media queries
            StringBuilder stylesBuilder = new StringBuilder();
            for (Element styleEl : doc.select("style")) {
                stylesBuilder.append(styleEl.data()).append("\n");
            }
            String styles = stylesBuilder.toString();
            Matcher m = MEDIA_QUERY_PATTERN.matcher(styles);
            boolean mediaFound = m.find();
            if (!mediaFound) {
                logger.debug("No @media found in <style> tags");
                issues.add("No media queries found in <style> blocks");
            } else {
                affected.add("@media queries");
            }

            // Images - if there are none, this check should be informational
            Elements imgs = doc.select("img");
            boolean fluidImageFound = false;
            if (imgs.isEmpty()) {
                logger.debug("No images found in email; skipping fluid image requirement");
            }
            for (Element img : imgs) {
                String style = img.attr("style").toLowerCase();
                String widthAttr = img.attr("width").toLowerCase();
                String src = img.attr("src");
                String srcset = img.attr("srcset");

                if (style.contains("max-width:100%") || style.contains("width:100%") || "100%".equals(widthAttr) || (srcset != null && !srcset.isEmpty())) {
                    fluidImageFound = true;
                    affected.add("img:" + (src == null || src.isEmpty() ? "unknown-src" : src));
                    break;
                }
            }
            if (!fluidImageFound && !imgs.isEmpty()) {
                issues.add("No fluid images detected (expected max-width:100%, width:100% or responsive srcset)");
            }

            // Tables responsive - only fail if tables exist and none are responsive
            Elements tables = doc.select("table");
            boolean responsiveTable = false;
            if (tables.isEmpty()) {
                logger.debug("No tables found; skipping responsive table requirement");
            }
            for (Element t : tables) {
                String style = t.attr("style").toLowerCase();
                String widthAttr = t.attr("width").toLowerCase();
                if (style.contains("width:100%") || style.contains("max-width:100%") || "100%".equals(widthAttr)) {
                    responsiveTable = true;
                    affected.add("table:responsive");
                    break;
                }
            }
            if (!tables.isEmpty() && !responsiveTable) {
                issues.add("Tables present but not marked responsive (no width:100% or max-width:100% found)");
            }

        } catch (Exception e) {
            logger.error("Error while validating mobile responsiveness: {}", e.getMessage(), e);
            issues.add("Exception during mobile responsiveness validation: " + e.getMessage());
        }

        boolean passed = issues.isEmpty();
        String notes = passed ? "Mobile responsiveness checks passed" : String.join("; ", issues);
        ValidationResult result = ValidationResult.builder("Mobile Responsiveness")
                .passed(passed)
                .severity(passed ? "INFO" : "MAJOR")
                .notes(notes)
                .issues(issues)
                .affectedElements(affected)
                .build();
        logger.info("Completed MobileResponsiveness validation: {}", result.isPassed() ? "PASS" : "FAIL");
        return result;
    }
}
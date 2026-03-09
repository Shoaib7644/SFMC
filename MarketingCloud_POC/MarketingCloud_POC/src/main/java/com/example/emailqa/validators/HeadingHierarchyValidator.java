package com.example.emailqa.validators;

import com.example.emailqa.model.ValidationResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HeadingHierarchyValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(HeadingHierarchyValidator.class);

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Starting HeadingHierarchy validation");
        Document doc = Jsoup.parse(emailHtml == null ? "" : emailHtml);
        List<String> issues = new ArrayList<>();
        List<String> affected = new ArrayList<>();

        List<Element> headings = doc.select("h1, h2, h3, h4, h5, h6");
        if (headings.isEmpty()) {
            issues.add("No headings found");
        } else {
            int prevLevel = 0;
            for (Element h : headings) {
                String tag = h.tagName();
                int level = Integer.parseInt(tag.substring(1));
                affected.add(tag + ":" + (h.text().length() > 30 ? h.text().substring(0, 30) + "..." : h.text()));
                if (prevLevel == 0) {
                    prevLevel = level;
                    continue;
                }
                // Valid sequence: next level can be same or +1 or smaller (e.g., h2 -> h2 or h3 or h1)
                if (level - prevLevel > 1) {
                    issues.add(String.format("Heading jump detected: %s after %s", tag, "h" + prevLevel));
                }
                prevLevel = level;
            }
        }

        boolean passed = issues.isEmpty();
        String notes = passed ? "Heading hierarchy is sequential" : String.join("; ", issues);
        ValidationResult result = ValidationResult.builder("Heading Hierarchy")
                .passed(passed)
                .severity(passed ? "INFO" : "MAJOR")
                .notes(notes)
                .issues(issues)
                .affectedElements(affected)
                .build();
        logger.info("Completed HeadingHierarchy validation: {}", result.isPassed() ? "PASS" : "FAIL");
        return result;
    }
}

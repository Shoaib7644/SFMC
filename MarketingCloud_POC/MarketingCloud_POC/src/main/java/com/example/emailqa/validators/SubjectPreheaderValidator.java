package com.example.emailqa.validators;

import com.example.emailqa.model.ValidationResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SubjectPreheaderValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(SubjectPreheaderValidator.class);

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Starting SubjectPreheader validation");
        Document doc = Jsoup.parse(emailHtml == null ? "" : emailHtml);
        List<String> issues = new ArrayList<>();
        List<String> affected = new ArrayList<>();

        // Try to extract subject from title tag
        String subject = doc.title();
        if (subject == null || subject.trim().isEmpty()) {
            issues.add("Subject not found in <title> tag");
        } else {
            affected.add("<title>");
            if (subject.length() >= 60) {
                issues.add(String.format("Subject length is %d (>=60)", subject.length()));
            }
        }

        // Preheader: often in a <span class="preheader"> or meta[name=description]
        String preheader = null;
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) {
            preheader = metaDesc.attr("content");
            if (preheader != null && !preheader.isEmpty()) {
                affected.add("meta[name=description]");
            }
        }
        if (preheader == null || preheader.trim().isEmpty()) {
            Element pre = doc.selectFirst("span.preheader, div.preheader, p.preheader");
            if (pre != null) {
                preheader = pre.text();
                affected.add(pre.nodeName() + (pre.classNames().isEmpty() ? "" : ".preheader"));
            }
        }

        if (preheader == null || preheader.trim().isEmpty()) {
            issues.add("Preheader text not found");
        } else {
            if (preheader.length() >= 100) {
                issues.add(String.format("Preheader length is %d (>=100)", preheader.length()));
            }
            // Check duplication in body
            String bodyText = doc.body() == null ? "" : doc.body().text();
            if (bodyText != null && preheader != null && !preheader.isEmpty() && bodyText.contains(preheader)) {
                issues.add("Preheader duplicated in body");
            }
        }

        boolean passed = issues.isEmpty();
        String notes = passed ? "Subject and preheader checks passed" : String.join("; ", issues);
        ValidationResult result = ValidationResult.builder("Subject & Preheader")
                .passed(passed)
                .severity(passed ? "INFO" : "MAJOR")
                .notes(notes)
                .issues(issues)
                .affectedElements(affected)
                .build();

        logger.info("Completed SubjectPreheader validation: {}", result.isPassed() ? "PASS" : "FAIL");
        return result;
    }
}

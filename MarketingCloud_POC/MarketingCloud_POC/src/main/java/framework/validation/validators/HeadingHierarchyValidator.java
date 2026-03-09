package framework.validation.validators;

import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HeadingHierarchyValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(HeadingHierarchyValidator.class);
    private final ValidationContext context;

    public HeadingHierarchyValidator(ValidationContext context) {
        this.context = context;
    }

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Running HeadingHierarchyValidator");
        Document doc = Jsoup.parse(emailHtml == null ? "" : emailHtml);
        List<Element> headings = doc.select("h1, h2, h3, h4, h5, h6");
        List<String> issues = new ArrayList<>();

        int prev = 0;
        for (Element h : headings) {
            String tag = h.tagName();
            int lvl = Integer.parseInt(tag.substring(1));
            if (prev == 0) {
                prev = lvl;
            } else {
                if (lvl - prev > 1) {
                    issues.add(String.format("Heading jump: %s after h%d", tag, prev));
                }
                prev = lvl;
            }
        }

        boolean passed = issues.isEmpty();
        String notes = passed ? "Heading order OK" : String.join("; ", issues);
        // per requirements: warning if missing or incorrect
        context.addResult("Heading Hierarchy", passed, notes, passed ? "INFO" : "MAJOR");
        return new ValidationResult("Heading Hierarchy", passed, notes, passed ? "INFO" : "MAJOR");
    }
}
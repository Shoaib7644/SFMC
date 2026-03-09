package framework.validation.reporting;

import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResult;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class AllureValidationReporter {
    private static final Logger logger = LoggerFactory.getLogger(AllureValidationReporter.class);
    private final ValidationContext context;

    public AllureValidationReporter(ValidationContext context) {
        this.context = context;
    }

    public void attachReports() {
        logger.info("Attaching validation artifacts to Allure");

        // Accessibility summary
        String accSummary = String.format("Total axe violations: %d", context.getAxeViolations().size());
        Allure.addAttachment("accessibility-summary.txt", accSummary);

        // Broken links
        String brokenLinks = context.getBrokenLinks().stream().collect(Collectors.joining("\n"));
        Allure.addAttachment("broken-links.txt", brokenLinks);

        // Broken images
        String brokenImages = context.getBrokenImages().stream().collect(Collectors.joining("\n"));
        Allure.addAttachment("broken-images.txt", brokenImages);

        // Validation results JSON
        List<ValidationResult> results = context.getResults();
        String json = results.stream().map(r -> String.format("{\"item\":\"%s\",\"status\":\"%s\",\"notes\":\"%s\",\"severity\":\"%s\"}",
                escape(r.getItem()), escape(r.getStatus()), escape(r.getNotes()), escape(r.getSeverity()))).collect(Collectors.joining(",\n", "[", "]"));
        Allure.addAttachment("validation-results.json", json);
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}

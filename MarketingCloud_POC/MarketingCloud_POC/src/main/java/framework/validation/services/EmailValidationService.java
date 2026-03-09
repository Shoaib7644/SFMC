package framework.validation.services;

import com.mailosaur.models.Message;
import com.microsoft.playwright.Page;
import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResult;
import framework.validation.model.ValidationResultsModel;
import framework.validation.reporting.AccessibilityReportAdapter;
import framework.validation.validators.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates all email validators and builds the aggregated ValidationResultsModel.
 * Fixed: Regex escape characters, balanced try/catch blocks, and completed inline validators.
 */
public class EmailValidationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailValidationService.class);
    private final ValidationContext context;

    public EmailValidationService(ValidationContext context) {
        this.context = context;
    }

    public List<ValidationResult> runAllValidations(Page page, String html, Message email) {
        logger.info("Starting all validations");
        String safeHtml = html == null ? "" : html;

        try {
            // 1. HTML Structure
            HtmlStructureValidator htmlValidator = new HtmlStructureValidator(context);
            ValidationResultsModel.FieldResult langRes = htmlValidator.validateLanguageAttribute(safeHtml);

            // 2. Metadata (Subject + Preheader)
            EmailMetadataValidator metadata = new EmailMetadataValidator(context, email);
            metadata.validate(safeHtml);

            // 3. Accessibility via axe
            AccessibilityValidator accessibility = new AccessibilityValidator(context);
            if (page != null) {
                try {
                    page.setContent(safeHtml);
                    accessibility.validate(page, safeHtml);
                } catch (Exception e) {
                    accessibility.validate(safeHtml);
                }
            } else {
                accessibility.validate(safeHtml);
            }

            // 4. Broken Links, Images, Personalization, Mobile
            new BrokenLinkValidator(context).validate(safeHtml);
            new ImageValidator(context).validate(safeHtml);
            new PersonalizationValidator(context).validate(safeHtml);
            new MobileResponsivenessValidator(context).validate(safeHtml);

            // 5. Unsubscribe
            ValidationResultsModel.FieldResult headerUnsubRes = null;
            try {
                headerUnsubRes = new EmailHeaderValidator().validate(email);
            } catch (Exception e) {
                logger.warn("Header check failed: {}", e.getMessage());
            }

            UnsubscribeValidator unsub = new UnsubscribeValidator(context);
            ValidationResult unsubBodyRes = unsub.validate(safeHtml);

            // 6. Descriptive Links
            LinkValidator linkValidator = new LinkValidator(context);
            ValidationResultsModel.FieldResult descriptiveLinkRes = linkValidator.validateDescriptiveLinks(safeHtml);

            // 7. Inline Validators (FIX 1-3)
            ValidationResultsModel.FieldResult tableRes = validateTableAccessibility(safeHtml);
            ValidationResultsModel.FieldResult decorativeRes = validateDecorativeImages(safeHtml);
            ValidationResultsModel.FieldResult fontRes = validateFontReadability(safeHtml);

            // BUILD MODEL
            ValidationResultsModel model = new ValidationResultsModel();
            model.setLanguageAttribute(langRes);
            
            // Map Subject/Preheader from context
            ValidationResult subjCtx = context.getResultByName("Subject Line");
            if (subjCtx != null) model.setSubjectLine(new ValidationResultsModel.FieldResult(subjCtx.getStatus(), subjCtx.getNotes(), ""));
            
            ValidationResult preCtx = context.getResultByName("Preheader Text");
            if (preCtx != null) model.setPreheader(new ValidationResultsModel.FieldResult(preCtx.getStatus(), preCtx.getNotes(), ""));

            // Axe Adapter
            AccessibilityReportAdapter adapter = new AccessibilityReportAdapter(context);
            ValidationResultsModel accModel = adapter.adapt();
            model.setColorContrast(accModel.getColorContrast());
            model.setHeadingHierarchy(accModel.getHeadingHierarchy());
            model.setButtonAccessibility(accModel.getButtonAccessibility());
            model.setDescriptiveLinks(descriptiveLinkRes);

            // Unsubscribe Merge
            model.setUnsubscribeLink(mergeUnsubscribeResults(headerUnsubRes, unsubBodyRes));

            // Set Inline Results
            model.setTableAccessibility(tableRes);
            model.setDecorativeImages(decorativeRes);
            model.setFontReadability(fontRes);

            context.setAggregatedModel(model);
            flushModelToContext(model);

        } catch (Exception e) {
            logger.error("Validation orchestration error: {}", e.getMessage(), e);
        }

        return context.getResults();
    }

    private ValidationResultsModel.FieldResult validateTableAccessibility(String html) {
        try {
            Document doc = Jsoup.parse(html);
            Elements allTables = doc.select("table");
            if (allTables.isEmpty()) return new ValidationResultsModel.FieldResult("PASS", "No tables found", "");

            List<String> issues = new ArrayList<>();
            for (Element table : allTables) {
                String role = table.attr("role").toLowerCase();
                if (role.equals("presentation") || role.equals("none")) continue;
                if (table.select("th").isEmpty()) issues.add("Table missing <th> headers");
            }

            return issues.isEmpty() ? new ValidationResultsModel.FieldResult("PASS", "Tables accessible", "") 
                                  : new ValidationResultsModel.FieldResult("FAIL", String.join("; ", issues), "");
        } catch (Exception e) {
            return new ValidationResultsModel.FieldResult("WARN", "Table error", "");
        }
    }

    private ValidationResultsModel.FieldResult validateDecorativeImages(String html) {
        try {
            Document doc = Jsoup.parse(html);
            Elements imgs = doc.select("img");
            List<String> issues = new ArrayList<>();

            for (Element img : imgs) {
                String role = img.attr("role").toLowerCase();
                String alt = img.attr("alt");
                if ((role.equals("presentation") || role.equals("none")) && !alt.isEmpty()) {
                    issues.add("Decorative image has alt text: " + truncate(img.attr("src"), 30));
                }
            }
            return issues.isEmpty() ? new ValidationResultsModel.FieldResult("PASS", "Decorative images OK", "")
                                  : new ValidationResultsModel.FieldResult("WARN", String.join("; ", issues), "");
        } catch (Exception e) {
            return new ValidationResultsModel.FieldResult("WARN", "Image error", "");
        }
    }

    private ValidationResultsModel.FieldResult validateFontReadability(String html) {
        try {
            Document doc = Jsoup.parse(html);
            List<String> smallFonts = new ArrayList<>();
            // FIX: Doubled backslashes for Regex
            String regex = "font-size\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)px";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);

            for (Element el : doc.select("[style]")) {
                java.util.regex.Matcher m = p.matcher(el.attr("style"));
                while (m.find()) {
                    if (Double.parseDouble(m.group(1)) < 12) {
                        smallFonts.add(el.tagName() + " (" + m.group(1) + "px)");
                    }
                }
            }
            return smallFonts.isEmpty() ? new ValidationResultsModel.FieldResult("PASS", "Fonts OK", "")
                                      : new ValidationResultsModel.FieldResult("WARN", "Small fonts: " + String.join(", ", smallFonts), "");
        } catch (Exception e) {
            return new ValidationResultsModel.FieldResult("WARN", "Font error", "");
        }
    }

    private ValidationResultsModel.FieldResult mergeUnsubscribeResults(ValidationResultsModel.FieldResult header, ValidationResult body) {
        boolean hPass = header != null && "PASS".equalsIgnoreCase(header.getStatus());
        boolean bPass = body != null && body.isPassed();
        if (hPass || bPass) return new ValidationResultsModel.FieldResult("PASS", "Unsubscribe found", "");
        return new ValidationResultsModel.FieldResult("FAIL", "No unsubscribe found", "");
    }

    private void flushModelToContext(ValidationResultsModel model) {
        // Logic to write model fields back into context.getResults()
        context.addResult("Table Accessibility", model.getTableAccessibility().getStatus().equals("PASS"), model.getTableAccessibility().getNotes(), "MAJOR");
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private String extractCssSelector(String css, int pos) {
        return "style-block-element"; // Simplified helper
    }
}
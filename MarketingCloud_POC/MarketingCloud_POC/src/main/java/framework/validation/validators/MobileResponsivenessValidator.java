package framework.validation.validators;

import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResult;
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
    private final ValidationContext context;

    public MobileResponsivenessValidator(ValidationContext context) {
        this.context = context;
    }

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Running MobileResponsivenessValidator");
        List<String> issues = new ArrayList<>();
        List<String> affected = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(emailHtml == null ? "" : emailHtml);
            Element viewport = doc.selectFirst("meta[name=viewport]");
            if (viewport == null) {
                issues.add("Missing meta viewport tag");
            } else {
                affected.add("meta[name=viewport]=" + viewport.attr("content"));
            }

            StringBuilder stylesBuilder = new StringBuilder();
            for (Element styleEl : doc.select("style")) {
                stylesBuilder.append(styleEl.data()).append("\n");
            }
            String styles = stylesBuilder.toString();
            Matcher m = MEDIA_QUERY_PATTERN.matcher(styles);
            if (!m.find()) {
                issues.add("No media queries found in <style> blocks");
            } else {
                affected.add("@media queries");
            }

            Elements imgs = doc.select("img");
            boolean fluid = false;
            for (Element img : imgs) {
                String style = img.attr("style").toLowerCase();
                String widthAttr = img.attr("width").toLowerCase();
                String src = img.attr("src");
                String srcset = img.attr("srcset");
                if (style.contains("max-width:100%") || style.contains("width:100%") || "100%".equals(widthAttr) || (srcset != null && !srcset.isEmpty())) {
                    fluid = true;
                    affected.add("img:" + (src == null ? "unknown-src" : src));
                    break;
                }
            }
            if (!imgs.isEmpty() && !fluid) {
                issues.add("No fluid images detected");
            }

            Elements tables = doc.select("table");
            boolean responsiveTable = false;
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
                issues.add("Tables present but not responsive");
            }

        } catch (Exception e) {
            logger.error("Error in MobileResponsivenessValidator: {}", e.getMessage(), e);
            issues.add("Exception during mobile responsiveness validation: " + e.getMessage());
        }

        boolean passed = issues.isEmpty();
        String notes = passed ? "Mobile responsiveness detected" : (issues.isEmpty() ? "Mobile responsiveness detected" : String.join("; ", issues));
        if (!passed) {
            // per requirements: warning when responsive checks missing
            context.addResult("Mobile Responsiveness", false, "No responsive CSS detected" + (notes.isEmpty() ? "" : ": " + notes), "MAJOR");
            return new ValidationResult("Mobile Responsiveness", false, "No responsive CSS detected" + (notes.isEmpty() ? "" : ": " + notes), "MAJOR");
        } else {
            context.addResult("Mobile Responsiveness", true, notes, "INFO");
            return new ValidationResult("Mobile Responsiveness", true, notes, "INFO");
        }
    }
}
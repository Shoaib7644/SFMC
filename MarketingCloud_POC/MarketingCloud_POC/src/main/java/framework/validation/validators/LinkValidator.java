package framework.validation.validators;

import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResultsModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates links: descriptive text and HTTP status integration (broken links are handled elsewhere).
 */
public class LinkValidator {
    private static final Logger logger = LoggerFactory.getLogger(LinkValidator.class);
    private final ValidationContext context;

    public LinkValidator(ValidationContext context) {
        this.context = context;
    }

    public ValidationResultsModel.FieldResult validateDescriptiveLinks(String html) {
        try {
            Document doc = Jsoup.parse(html == null ? "" : html);
            Elements anchors = doc.select("a[href]");
            List<String> bad = new ArrayList<>();
            for (Element a : anchors) {
                String text = a.text() == null ? "" : a.text().toLowerCase().trim();
                if (text.isEmpty()) {
                    bad.add(a.attr("href") + " (empty text)");
                } else if (text.contains("click here") || text.contains("read more") || text.equals("link")) {
                    bad.add(a.attr("href") + " (" + a.text() + ")");
                }
            }

            if (!bad.isEmpty()) {
                return new ValidationResultsModel.FieldResult("FAIL", "Non-descriptive link text found", String.join("; ", bad));
            }
            return new ValidationResultsModel.FieldResult("PASS", "All links have descriptive text", "");
        } catch (Exception e) {
            logger.warn("Error validating descriptive links: {}", e.getMessage());
            return new ValidationResultsModel.FieldResult("WARN", "Error validating links: " + e.getMessage(), "");
        }
    }
}

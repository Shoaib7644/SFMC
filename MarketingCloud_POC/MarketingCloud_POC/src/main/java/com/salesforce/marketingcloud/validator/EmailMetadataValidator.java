package com.salesforce.marketingcloud.validator;

import com.mailosaur.models.Message;
import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.model.ValidationResult;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates email Subject Line and Preheader text.
 */
public class EmailMetadataValidator implements EmailValidator {

    private static final Logger logger = LoggerFactory.getLogger(EmailMetadataValidator.class);

    private static final int SUBJECT_MIN_LENGTH  = 10;
    private static final int SUBJECT_WARN_LENGTH = 78;

    private static final int PREHEADER_MIN_LENGTH  = 30;
    private static final int PREHEADER_MAX_LENGTH  = 150;

    private final ValidationContext context;
    private final Message message;

    public EmailMetadataValidator(ValidationContext context, Message message) {
        this.context = context;
        this.message = message;
    }

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Running EmailMetadataValidator");

        // FIX: Pass emailHtml to validateSubjectLine so it is in scope
        boolean subjPass = validateSubjectLine(emailHtml == null ? "" : emailHtml);
        boolean prePass  = validatePreheader(emailHtml == null ? "" : emailHtml);

        boolean overall = subjPass && prePass;
        
        // Final Reporting for the Word Audit Table Key: "Subject Line & Preheader"
        context.addResult("Subject Line & Preheader", overall, 
                "Subject and preheader validation complete", overall ? "INFO" : "MAJOR");

        return new ValidationResult(
                "Email Metadata", overall,
                "Subject and preheader validation complete",
                overall ? "INFO" : "MAJOR");
    }

    /**
     * FIX: Added String emailHtml parameter to resolve scope issue
     */
    private boolean validateSubjectLine(String emailHtml) {
        String subject    = null;
        String sourceDesc = "unknown";

        if (message != null) {
            try {
                String s = message.subject();
                if (s != null && !s.trim().isEmpty()) {
                    subject    = s.trim();
                    sourceDesc = "Mailosaur message.subject()";
                }
            } catch (Exception e) {
                logger.warn("Error reading message.subject(): {}", e.getMessage());
            }
        }

        if (subject == null) {
            try {
                Document doc = Jsoup.parse(emailHtml);
                Element metaSubject = doc.selectFirst("meta[name=subject]");
                if (metaSubject != null) {
                    String content = metaSubject.attr("content");
                    if (content != null && !content.trim().isEmpty()) {
                        subject    = content.trim();
                        sourceDesc = "<meta name=\"subject\">"; // FIX: Escaped quotes
                    }
                }
            } catch (Exception e) {
                logger.warn("Error reading meta subject tag: {}", e.getMessage());
            }
        }

        boolean pass;
        String notes;

        if (subject == null || subject.isEmpty()) {
            pass  = false;
            // FIX: Escaped quotes in the string below
            notes = "Subject is empty — not found in Mailosaur message or <meta name=\"subject\">";
            logger.info("Subject: FAIL — empty/null (source={})", sourceDesc);
        } else {
            int len = subject.length();
            if (len < SUBJECT_MIN_LENGTH) {
                pass  = false;
                notes = String.format("Subject too short: '%s' (%d chars, minimum %d)",
                        subject, len, SUBJECT_MIN_LENGTH);
            } else if (len > SUBJECT_WARN_LENGTH) {
                pass  = true;
                notes = String.format("Subject length %d chars (may be clipped >78): '%s'", len, subject);
            } else {
                pass  = true;
                notes = String.format("Subject OK (%d chars): '%s'", len, subject);
            }
            logger.info("Subject: {} — '{}' ({} chars, source={})",
                    pass ? "PASS" : "FAIL", subject, len, sourceDesc);
        }

        context.addResult("Subject Line", pass, notes, pass ? "INFO" : "MAJOR");
        return pass;
    }

    private boolean validatePreheader(String html) {
        Document doc;
        try {
            doc = Jsoup.parse(html);
        } catch (Exception e) {
            logger.warn("Failed to parse HTML for preheader detection: {}", e.getMessage());
            context.addResult("Preheader Text", false, "HTML parse error: " + e.getMessage(), "MAJOR");
            return false;
        }

        String preheaderText = null;
        String sourceDesc    = null;

        Element metaPreheader = doc.selectFirst("meta[name=preheader]");
        if (metaPreheader != null) {
            String content = metaPreheader.attr("content");
            if (content != null && !content.trim().isEmpty()) {
                preheaderText = content.trim();
                sourceDesc    = "<meta name=\"preheader\">";
            }
        }

        if (preheaderText == null) {
            for (Element el : doc.select("[style]")) {
                String style = el.attr("style").toLowerCase().replaceAll("\\s+", "");
                if (style.contains("display:none")) {
                    String text = el.text().trim();
                    text = text.replaceAll("[\u00AD\u200B\u200C\u200D\uFEFF\u00A0]+", "").trim();
                    if (!text.isEmpty() && text.length() >= 5) {
                        preheaderText = text;
                        sourceDesc = "hidden element (display:none)";
                        break;
                    }
                }
            }
        }

        if (preheaderText == null) {
            Element classEl = doc.selectFirst(".preheader, .preview-text, [class*=preheader]");
            if (classEl != null) {
                String text = classEl.text().trim();
                if (!text.isEmpty()) {
                    preheaderText = text;
                    sourceDesc    = "element with class 'preheader'";
                }
            }
        }

        boolean pass;
        String notes;

        if (preheaderText == null || preheaderText.isEmpty()) {
            pass  = false;
            notes = "Preheader missing — no <meta name=\"preheader\">, hidden div, or .preheader element found";
            logger.info("Preheader: FAIL — not found");
        } else {
            int len = preheaderText.length();
            if (len < PREHEADER_MIN_LENGTH) {
                pass  = true; 
                notes = String.format(
                        "Preheader present but short (%d chars, recommended ≥%d) [%s]: '%s'",
                        len, PREHEADER_MIN_LENGTH, sourceDesc, truncate(preheaderText, 80));
            } else if (len > PREHEADER_MAX_LENGTH) {
                pass  = true; 
                notes = String.format(
                        "Preheader present but long (%d chars, recommended ≤%d) [%s]: '%s…'",
                        len, PREHEADER_MAX_LENGTH, sourceDesc, truncate(preheaderText, 80));
            } else {
                pass  = true;
                notes = String.format(
                        "Preheader OK (%d chars) [%s]: '%s'",
                        len, sourceDesc, truncate(preheaderText, 80));
            }
            logger.info("Preheader: {} — {} chars (source={})", pass ? "PASS" : "FAIL", len, sourceDesc);
        }

        context.addResult("Preheader Text", pass, notes, pass ? "INFO" : "MAJOR");
        return pass;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
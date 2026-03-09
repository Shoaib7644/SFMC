package framework.validation.reporting;

import com.microsoft.playwright.Page;
import com.salesforce.marketingcloud.constant.Constant;
import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResult;
import framework.validation.model.ValidationResultsModel;
import io.qameta.allure.Allure;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the SFMC Email Audit Report DOCX.
 *
 * FIXES IN THIS VERSION:
 *
 *  Fix B (WARN vs FAIL in Status column) — The previous version derived the displayed status
 *  from ValidationResult.getStatus() which only returns "PASS" or "FAIL" (boolean-based).
 *  WARN items (headingHierarchy landmark issue, fontReadability, tableAccessibility,
 *  decorativeImages) appeared as "FAIL" in the report.
 *
 *  Fixed: the report now reads status directly from the aggregated model's FieldResult.getStatus()
 *  which correctly returns "PASS", "FAIL", or "WARN". The FieldResult is the authoritative
 *  source — it was set correctly by EmailValidationService.
 *
 *  Fix for "NOT EXECUTED" fallback — when a FieldResult is null (validator didn't run),
 *  the status shows "NOT EXECUTED" and notes shows "No validator ran for this item".
 *  When a FieldResult exists with status="WARN" and notes="Not evaluated", it shows
 *  "WARN" and the actual notes — correctly distinguishing "not run" from "run but inconclusive".
 *
 *  Fix for evidence contamination — FieldResult.evidence no longer contains severity strings
 *  (fixed in EmailValidationService). Notes column now shows clean notes only.
 */
public class WordReportGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(WordReportGenerator.class);

    // ── Report row definitions: report label → context result key(s) ─────────────────────────
    // Order matches the DOCX table rows exactly.
    private static final List<ReportRow> REPORT_ROWS = new ArrayList<>();

    static {
        REPORT_ROWS.add(new ReportRow(1,  "Language Attribute (html lang)",  "languageAttribute"));
        REPORT_ROWS.add(new ReportRow(2,  "Subject Line & Preheader",        "subjectAndPreheader")); // special-cased
        REPORT_ROWS.add(new ReportRow(3,  "Meaningful Alt Text",             "altText"));
        REPORT_ROWS.add(new ReportRow(4,  "Decorative Image Handling",       "decorativeImages"));
        REPORT_ROWS.add(new ReportRow(5,  "Heading Hierarchy",               "headingHierarchy"));
        REPORT_ROWS.add(new ReportRow(6,  "Color Contrast",                  "colorContrast"));
        REPORT_ROWS.add(new ReportRow(7,  "Descriptive Link Text",           "descriptiveLinks"));
        REPORT_ROWS.add(new ReportRow(8,  "Button/CTA Discernibility",       "buttonAccessibility"));
        REPORT_ROWS.add(new ReportRow(9,  "Font Size & Spacing",             "fontReadability"));
        REPORT_ROWS.add(new ReportRow(10, "HTTP Link Status",                "httpLinks"));
        REPORT_ROWS.add(new ReportRow(11, "Image Loading Status",            "imageLoading"));
        REPORT_ROWS.add(new ReportRow(12, "SFMC Personalization Strings",    "personalizationStrings"));
        REPORT_ROWS.add(new ReportRow(13, "Table Accessibility",             "tableAccessibility"));
        REPORT_ROWS.add(new ReportRow(14, "Mobile Responsiveness",           "mobileResponsiveness"));
        REPORT_ROWS.add(new ReportRow(15, "Unsubscribe Link",                "unsubscribeLink"));
    }

    private static class ReportRow {
        final int sno;
        final String label;
        final String modelKey;
        ReportRow(int sno, String label, String modelKey) {
            this.sno = sno; this.label = label; this.modelKey = modelKey;
        }
    }

    /**
     * Generates the audit report DOCX and attaches it to Allure.
     * Reads validation results from framework.validation.context.ValidationContext singleton.
     *
     * @param emailName  display name for the email being audited
     * @param page       Playwright page (used for screenshot; may be null)
     * @return           DOCX bytes, or null on failure
     */
    public static byte[] generateAuditReport(String emailName, Page page) {
        LOG.info("Generating SFMC Email Audit Report for: {}", emailName);
        try {
            ValidationContext ctx = ValidationContext.getInstance();
            ValidationResultsModel model = ctx.getAggregatedModel();

            if (model == null) {
                LOG.warn("Aggregated model is null — report will show NOT EXECUTED for all rows");
                model = new ValidationResultsModel();
            }

            byte[] docBytes = buildDocument(emailName, model, ctx, page);

            // Attach to Allure
            try (ByteArrayInputStream bis = new ByteArrayInputStream(docBytes)) {
                Allure.addAttachment(
                        "SFMC Email Audit Report - " + emailName + ".docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        bis, ".docx");
            }

            // Save to disk
            try {
                Path outDir = Paths.get("target", "audit-reports");
                Files.createDirectories(outDir);
                String safeName = emailName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
                Path outFile = outDir.resolve("AuditReport_" + safeName + ".docx");
                Files.write(outFile, docBytes);
                LOG.info("Saved audit report to {}", outFile.toAbsolutePath());
            } catch (Exception e) {
                LOG.warn("Failed to save report to disk: {}", e.getMessage());
            }

            return docBytes;

        } catch (Exception e) {
            LOG.error("Failed to generate audit report: {}", e.getMessage(), e);
            return null;
        }
    }

    // ── Document construction ─────────────────────────────────────────────────────────────────

    private static byte[] buildDocument(String emailName,
                                         ValidationResultsModel model,
                                         ValidationContext ctx,
                                         Page page) throws IOException {

        XWPFDocument doc = new XWPFDocument();

        // ── Title ──────────────────────────────────────────────────────────────────────────────
        XWPFParagraph titlePara = doc.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setFontFamily("Arial");
        titleRun.setText("SFMC Email Audit Report");
        titleRun.addBreak();

        // ── Meta ───────────────────────────────────────────────────────────────────────────────
        addMetaLine(doc, "Email: " + emailName);
        addMetaLine(doc, "Generated: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        doc.createParagraph(); // spacer

        // ── Validation Table ───────────────────────────────────────────────────────────────────
        XWPFParagraph sectionPara = doc.createParagraph();
        XWPFRun sectionRun = sectionPara.createRun();
        sectionRun.setBold(true);
        sectionRun.setFontSize(12);
        sectionRun.setFontFamily("Arial");
        sectionRun.setText("Validation Results");

        XWPFTable table = doc.createTable(1, 4);
        table.setWidth(9072);

        // Header row
        XWPFTableRow headerRow = table.getRow(0);
        setHeaderCell(headerRow.getCell(0), "S.No");
        setHeaderCell(headerRow.addNewTableCell(), "Items to Test");
        setHeaderCell(headerRow.addNewTableCell(), "Status");
        setHeaderCell(headerRow.addNewTableCell(), "Notes");

        // Data rows — read status DIRECTLY from FieldResult, not from ValidationResult.getStatus()
        for (ReportRow rowDef : REPORT_ROWS) {
            XWPFTableRow row = table.createRow();

            String status;
            String notes;

            if ("subjectAndPreheader".equals(rowDef.modelKey)) {
                // ── Special case: merge Subject + Preheader into one row ───────────────────────
                ValidationResultsModel.FieldResult subj = model.getSubjectLine();
                ValidationResultsModel.FieldResult pre  = model.getPreheader();

                if (subj == null && pre == null) {
                    status = "NOT EXECUTED";
                    notes  = "No Subject or Preheader validation executed";
                } else {
                    // Worst status wins: FAIL > WARN > PASS
                    String subjStatus = subj != null ? subj.getStatus() : "NOT EXECUTED";
                    String preStatus  = pre  != null ? pre.getStatus()  : "NOT EXECUTED";
                    status = worstStatus(subjStatus, preStatus);

                    String subjNotes = subj != null ? subj.getNotes() : "not executed";
                    String preNotes  = pre  != null ? pre.getNotes()  : "not executed";
                    notes = String.format("Subject: %s - %s; Preheader: %s - %s",
                            subjStatus, subjNotes, preStatus, preNotes);
                }
            } else {
                // ── Standard case: read FieldResult directly from model ───────────────────────
                // FIX B: getFieldResult() returns the FieldResult with status "PASS"/"FAIL"/"WARN"
                // The old code used vr.getStatus() which only returned "PASS" or "FAIL".
                ValidationResultsModel.FieldResult field = getFieldResult(model, rowDef.modelKey);

                if (field == null) {
                    status = "NOT EXECUTED";
                    notes  = "No validator ran for this item";
                } else {
                    // Use FieldResult.getStatus() directly — this is "PASS", "FAIL", or "WARN"
                    status = field.getStatus() == null ? "WARN" : field.getStatus().toUpperCase();
                    notes  = field.getNotes()  == null ? ""     : field.getNotes();
                }
            }

            setCellText(row.getCell(0), String.valueOf(rowDef.sno));
            setCellText(row.getCell(1), rowDef.label);
            setStatusCell(row.getCell(2), status);
            setCellText(row.getCell(3), notes);
        }

        // ── Screenshot placeholder ─────────────────────────────────────────────────────────────
        doc.createParagraph(); // spacer
        XWPFParagraph visualPara = doc.createParagraph();
        XWPFRun visualRun = visualPara.createRun();
        visualRun.setBold(true);
        visualRun.setFontFamily("Arial");
        visualRun.setText("Visual Audit");

        // Attempt screenshot via Playwright page
        if (page != null) {
            try {
                byte[] screenshot = page.screenshot(
                        new com.microsoft.playwright.Page.ScreenshotOptions().setFullPage(true));
                XWPFParagraph imgPara = doc.createParagraph();
                XWPFRun imgRun = imgPara.createRun();
                // Embed screenshot — approximate 6 inches wide
                int emuWidth  = 5486400; // ~6 inches in EMU (1 inch = 914400 EMU)
                int emuHeight = (int) (emuWidth * 1.4); // rough A4 aspect
                imgRun.addPicture(
                        new java.io.ByteArrayInputStream(screenshot),
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "screenshot.png",
                        emuWidth, emuHeight);
            } catch (Exception e) {
                LOG.warn("Failed to capture screenshot for report: {}", e.getMessage());
                XWPFParagraph noImg = doc.createParagraph();
                noImg.createRun().setText("[Screenshot unavailable: " + e.getMessage() + "]");
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.write(baos);
        doc.close();
        return baos.toByteArray();
    }

    // ── Model field accessor ──────────────────────────────────────────────────────────────────

    /**
     * Retrieves a FieldResult from the model by the modelKey string used in REPORT_ROWS.
     * This replaces the old resultMap lookup that used normalizeKey/aliases — since the
     * model is already fully populated by EmailValidationService, direct getter access
     * is simpler, faster, and has no key-mismatch risk.
     */
    private static ValidationResultsModel.FieldResult getFieldResult(
            ValidationResultsModel model, String key) {
        switch (key) {
            case "languageAttribute":       return model.getLanguageAttribute();
            case "altText":                 return model.getAltText();
            case "decorativeImages":        return model.getDecorativeImages();
            case "headingHierarchy":        return model.getHeadingHierarchy();
            case "colorContrast":           return model.getColorContrast();
            case "descriptiveLinks":        return model.getDescriptiveLinks();
            case "buttonAccessibility":     return model.getButtonAccessibility();
            case "fontReadability":         return model.getFontReadability();
            case "httpLinks":               return model.getHttpLinks();
            case "imageLoading":            return model.getImageLoading();
            case "personalizationStrings":  return model.getPersonalizationStrings();
            case "tableAccessibility":      return model.getTableAccessibility();
            case "mobileResponsiveness":    return model.getMobileResponsiveness();
            case "unsubscribeLink":         return model.getUnsubscribeLink();
            default:                        return null;
        }
    }

    // ── Status merging ────────────────────────────────────────────────────────────────────────

    /** Returns the most severe status between two: FAIL > WARN > PASS > NOT EXECUTED */
    private static String worstStatus(String a, String b) {
        if ("FAIL".equalsIgnoreCase(a) || "FAIL".equalsIgnoreCase(b)) return "FAIL";
        if ("WARN".equalsIgnoreCase(a) || "WARN".equalsIgnoreCase(b)) return "WARN";
        if ("PASS".equalsIgnoreCase(a) || "PASS".equalsIgnoreCase(b)) return "PASS";
        return "NOT EXECUTED";
    }

    // ── Cell styling helpers ──────────────────────────────────────────────────────────────────

    private static void addMetaLine(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setFontFamily("Arial");
        r.setFontSize(10);
        r.setText(text);
    }

    private static void setHeaderCell(XWPFTableCell cell, String text) {
        cell.setColor("2E4057"); // dark blue
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setBold(true);
        r.setColor("FFFFFF");
        r.setFontFamily("Arial");
        r.setFontSize(10);
        r.setText(text);
    }

    private static void setCellText(XWPFTableCell cell, String text) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r = p.getRuns().isEmpty() ? p.createRun() : p.getRuns().get(0);
        r.setFontFamily("Arial");
        r.setFontSize(9);
        r.setText(text == null ? "" : text);
    }

    /**
     * Sets the status cell text AND background colour based on status string.
     *
     * FIX B: now handles "WARN" as a distinct state — amber background.
     * Old code only handled PASS (green) and FAIL (red); WARN was shown as red FAIL.
     *
     * PASS         → green  (#C6EFCE)
     * FAIL         → red    (#FFC7CE)
     * WARN         → amber  (#FFEB9C)
     * NOT EXECUTED → grey   (#D9D9D9)
     */
    private static void setStatusCell(XWPFTableCell cell, String status) {
        String bgColor;
        String textColor = "000000";

        switch (status == null ? "" : status.toUpperCase()) {
            case "PASS":
                bgColor = "C6EFCE"; // green
                break;
            case "FAIL":
                bgColor = "FFC7CE"; // red
                break;
            case "WARN":
                bgColor = "FFEB9C"; // amber
                break;
            case "NOT EXECUTED":
            default:
                bgColor = "D9D9D9"; // grey
                break;
        }

        cell.setColor(bgColor);
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.getRuns().isEmpty() ? p.createRun() : p.getRuns().get(0);
        r.setFontFamily("Arial");
        r.setFontSize(9);
        r.setBold(!"PASS".equalsIgnoreCase(status));
        r.setColor(textColor);
        r.setText(status == null ? "NOT EXECUTED" : status.toUpperCase());
    }
}

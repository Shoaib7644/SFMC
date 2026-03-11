package com.salesforce.marketingcloud.reporting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.playwright.Page;
import com.salesforce.marketingcloud.constant.Constant;
import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResult;
import framework.validation.model.ValidationResultsModel;
import framework.validation.services.EmailValidationService;
import io.qameta.allure.Allure;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds an executive Word (DOCX) audit report for email validation using Apache POI.
 *
 * DOCUMENT STRUCTURE:
 *   Page 1+  — Cover title + Audit Checklist table (15 rows, PASS/WARN/FAIL colour coded)
 *   Page break
 *   Section 2 — Axe Accessibility Report (violation table, colour-coded by impact)
 *   Page break
 *   Section 3 — Link & Image Validation (HTTP link check + broken images)
 *   Page break
 *   Section 4 — Email Screenshot (full-page Playwright screenshot)
 *
 * ALSO ATTACHES to Allure:
 *   - Axe Accessibility Report.html   (standalone colour-coded HTML for browser viewing)
 *   - The DOCX itself
 */
public final class WordReportGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(WordReportGenerator.class);
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DISPLAY_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /** Max chars for a URL in the Notes column before truncation */
    private static final int NOTES_URL_MAX_LEN = 60;
    /** Max node snippets per axe violation row in the DOCX */
    private static final int MAX_NODES_IN_DOCX = 2;

    // Colour constants (hex, no leading #)
    private static final String COL_PASS     = "1A7A3C"; // green
    private static final String COL_FAIL     = "C00000"; // red
    private static final String COL_WARN     = "B97700"; // amber
    private static final String COL_GREY     = "767676"; // grey
    private static final String COL_CRITICAL = "C00000"; // same red
    private static final String COL_SERIOUS  = "E05A1A"; // orange-red
    private static final String COL_MODERATE = "B97700"; // amber
    private static final String COL_MINOR    = "555555"; // dark grey
    private static final String FILL_HEADER  = "2E4057"; // dark slate — table header bg
    private static final String FILL_SECTION = "F0F4F8"; // very light blue — section header bg
    private static final String FILL_D9      = "D9D9D9"; // checklist header grey

    private WordReportGenerator() {}

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // PUBLIC ENTRY POINT
    // ══════════════════════════════════════════════════════════════════════════════════════════

    public static byte[] generateAuditReport(String emailName, Page page) throws Exception {
        Objects.requireNonNull(emailName, "emailName must not be null");

        Path resultsDir = Paths.get("results");
        if (!Files.exists(resultsDir)) Files.createDirectories(resultsDir);

        String safeEmail = safeFilename(emailName);
        String fileName  = String.format("Audit_%s_%s.docx", safeEmail, TS.format(Instant.now()));
        Path   outFile   = resultsDir.resolve(fileName);

        List<ReportItem> items = buildValidationMatrix();
        ValidationContext ctx  = ValidationContext.getInstance();
        List<ValidationResult> results = ctx.getResults();

        writeDebugSnapshot(ctx, results, resultsDir);

        // On-demand validation fallback
        boolean hasAggregated = isModelPopulated(ctx.getAggregatedModel());
        Map<String, ValidationResult> resultMap = buildResultMap(results);
        if (resultMap.isEmpty() && !hasAggregated) {
            LOG.info("No structured results found — running on-demand validations");
            try {
                new EmailValidationService(ctx).runAllValidations(Constant.PAGE, Constant.latestEmailHtml, null);
                resultMap = buildResultMap(ctx.getResults());
            } catch (Exception e) {
                LOG.warn("On-demand validations failed: {}", e.getMessage());
            }
        }

        Map<String, List<String>> aliases = buildAliases();
        ValidationResultsModel agg = ctx.getAggregatedModel();

        // Resolve status + notes for each report row
        for (ReportItem ri : items) {
            if (ri.item.equalsIgnoreCase("Subject Line & Preheader")) {
                resolveSubjectPreheader(ri, agg, resultMap, aliases);
                continue;
            }
            ValidationResultsModel.FieldResult field = getFieldResult(agg, ri.item);
            if (field != null) {
                ri.status = field.getStatus() == null ? "WARN" : field.getStatus().toUpperCase();
                ri.notes  = field.getNotes()  == null ? ""     : field.getNotes();
            } else {
                ValidationResult match = lookupResult(resultMap, aliases, ri.item);
                if (match == null) {
                    ri.status = "NOT EXECUTED";
                    ri.notes  = "No validator ran for this item";
                } else {
                    ri.status = match.getStatus();
                    ri.notes  = match.getNotes();
                }
            }
            ri.notes = truncateNotesUrls(ri.notes);
        }

        // Capture screenshot before building doc (Playwright must be called before POI work)
        byte[] screenshot = captureScreenshot(page);

        // ── Build DOCX ────────────────────────────────────────────────────────────────────────
        byte[] docBytes;
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ── Section 1: Cover + Checklist ──────────────────────────────────────────────────
            buildCoverBlock(doc, emailName);
            buildChecklistTable(doc, items);

            // ── Section 2: Axe Accessibility Report ───────────────────────────────────────────
            addPageBreak(doc);
            buildAxeSection(doc, ctx);

            // ── Section 3: Link & Image Validation ────────────────────────────────────────────
            addPageBreak(doc);
            buildLinkSection(doc, ctx);

            // ── Section 4: Email Screenshot ───────────────────────────────────────────────────
            addPageBreak(doc);
            buildScreenshotSection(doc, screenshot);

            doc.write(out);
            docBytes = out.toByteArray();
        }

        Files.write(outFile, docBytes);

        // Attach DOCX to Allure
        try (ByteArrayInputStream bis = new ByteArrayInputStream(docBytes)) {
            Allure.addAttachment(
                    "Audit Report - " + safeEmail,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    bis, ".docx");
        } catch (Exception e) {
            LOG.warn("Failed to attach DOCX to Allure: {}", e.getMessage());
        }

        // Also attach standalone Axe HTML report to Allure (for browser viewing)
        attachAxeHtmlReport(ctx, emailName);

        LOG.info("Saved audit report → {}", outFile.toAbsolutePath());
        return docBytes;
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // SECTION BUILDERS
    // ══════════════════════════════════════════════════════════════════════════════════════════

    /** Cover title block: report title, email name, generated timestamp. */
    private static void buildCoverBlock(XWPFDocument doc, String emailName) {
        XWPFParagraph titlePara = doc.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = titlePara.createRun();
        r.setBold(true);
        r.setFontSize(18);
        r.setFontFamily("Arial");
        r.setText("SFMC Email Accessibility Audit Report");

        XWPFParagraph metaPara = doc.createParagraph();
        metaPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun m = metaPara.createRun();
        m.setFontSize(11);
        m.setFontFamily("Arial");
        m.setColor(COL_GREY);
        m.setText("Email: " + emailName);
        m.addBreak();
        m.setText("Generated: " + DISPLAY_TS.format(Instant.now()));
        m.addBreak();
        m.addBreak();
    }

    /** 15-row audit checklist table. */
    private static void buildChecklistTable(XWPFDocument doc, List<ReportItem> items) {
        // Section heading
        addSectionHeading(doc, "Audit Checklist", true);

        XWPFTable table = doc.createTable(items.size() + 1, 4);
        setTableWidth(table, 9360);

        // Column widths: S.No=500, Item=2800, Status=800, Notes=5260
        int[] colWidths = {500, 2800, 800, 5260};

        // Header row
        XWPFTableRow hdr = table.getRow(0);
        String[] hdrs = {"S.No", "Items to Test", "Status", "Notes"};
        for (int c = 0; c < 4; c++) {
            XWPFTableCell cell = hdr.getCell(c);
            setCellWidth(cell, colWidths[c]);
            setCellBg(cell, FILL_D9);
            setCellText(cell, hdrs[c], true, "000000", 10);
        }

        // Data rows
        for (int i = 0; i < items.size(); i++) {
            ReportItem ri = items.get(i);
            XWPFTableRow row = table.getRow(i + 1);

            setCellWidth(row.getCell(0), colWidths[0]);
            setCellWidth(row.getCell(1), colWidths[1]);
            setCellWidth(row.getCell(2), colWidths[2]);
            setCellWidth(row.getCell(3), colWidths[3]);

            setCellText(row.getCell(0), String.valueOf(ri.sno), false, "000000", 9);
            setCellText(row.getCell(1), ri.item,   false, "000000", 9);

            // Status cell — coloured bold text
            String statusColor = statusColor(ri.status);
            setCellText(row.getCell(2), ri.status == null ? "NOT EXECUTED" : ri.status,
                    true, statusColor, 9);

            setCellText(row.getCell(3), ri.notes == null ? "" : ri.notes, false, "000000", 9);
        }
    }

    // ── Axe Section ────────────────────────────────────────────────────────────────────────────

    /**
     * Builds the Axe Accessibility Report section in the DOCX.
     *
     * Layout:
     *   Heading: "Axe Accessibility Report"
     *   Summary line: "X rule(s) violated — Critical: N, Serious: N, Moderate: N, Minor: N"
     *   If violations: table with columns Rule ID | Impact | Description | WCAG Tags | Affected Elements
     *   If none:       "✓ No axe violations detected."
     */
    @SuppressWarnings("unchecked")
    private static void buildAxeSection(XWPFDocument doc, ValidationContext ctx) {
        addSectionHeading(doc, "Axe Accessibility Report", false);

        List<Object> violations = ctx.getAxeViolations();
        Map<String, Integer> summary = ctx.getAxeSummary();
        int total = violations == null ? 0 : violations.size();

        // Summary line
        XWPFParagraph sumPara = doc.createParagraph();
        XWPFRun sumRun = sumPara.createRun();
        sumRun.setFontFamily("Arial");
        sumRun.setFontSize(10);
        if (total == 0) {
            sumRun.setColor(COL_PASS);
            sumRun.setBold(true);
            sumRun.setText("✓ No axe violations detected.");
            return;
        }

        sumRun.setBold(false);
        sumRun.setColor(COL_GREY);
        sumRun.setText(buildAxeSummaryLine(total, summary));

        // Violations table: Rule ID | Impact | Description | WCAG Tags | Affected Elements
        XWPFTable table = doc.createTable(total + 1, 5);
        setTableWidth(table, 9360);
        int[] colWidths = {1200, 900, 2500, 1600, 3160};

        // Header row
        XWPFTableRow hdr = table.getRow(0);
        String[] hdrs = {"Rule ID", "Impact", "Description", "WCAG Tags", "Affected Elements"};
        for (int c = 0; c < 5; c++) {
            XWPFTableCell cell = hdr.getCell(c);
            setCellWidth(cell, colWidths[c]);
            setCellBg(cell, FILL_HEADER);
            setCellText(cell, hdrs[c], true, "FFFFFF", 9);
        }

        // Violation rows
        int rowIdx = 1;
        for (Object v : violations) {
            if (!(v instanceof Map)) { rowIdx++; continue; }
            Map<String, Object> vmap = (Map<String, Object>) v;

            String id     = str(vmap, "id");
            String impact = str(vmap, "impact");
            String desc   = str(vmap, "description");
            Object tagsO  = vmap.get("tags");
            Object nodesO = vmap.get("nodes");

            String impactColor = impactColor(impact);
            String tags  = buildTagsText(tagsO);
            String nodes = buildNodesText(nodesO, MAX_NODES_IN_DOCX);

            XWPFTableRow row = table.getRow(rowIdx++);
            setCellWidth(row.getCell(0), colWidths[0]);
            setCellWidth(row.getCell(1), colWidths[1]);
            setCellWidth(row.getCell(2), colWidths[2]);
            setCellWidth(row.getCell(3), colWidths[3]);
            setCellWidth(row.getCell(4), colWidths[4]);

            setCellText(row.getCell(0), id,                     true,  "000000", 9);
            setCellText(row.getCell(1), impact.toUpperCase(),   true,  impactColor, 9);
            setCellText(row.getCell(2), trunc(desc, 200),       false, "000000", 9);
            setCellText(row.getCell(3), tags,                   false, "444466", 9);
            setCellText(row.getCell(4), nodes,                  false, "333333", 8);
        }

        // Footnote
        XWPFParagraph note = doc.createParagraph();
        XWPFRun noteRun = note.createRun();
        noteRun.setFontFamily("Arial");
        noteRun.setFontSize(8);
        noteRun.setItalic(true);
        noteRun.setColor(COL_GREY);
        noteRun.setText("Impact levels: CRITICAL/SERIOUS = must fix (WCAG failure); MODERATE/MINOR = should fix (best practice)");
    }

    // ── Link & Image Section ───────────────────────────────────────────────────────────────────

    /**
     * Builds the Link & Image Validation section in the DOCX.
     *
     * Layout:
     *   Heading: "Link & Image Validation"
     *   Sub-section: HTTP Links
     *     → Summary: "X link(s) checked — N broken"
     *     → Table of broken links (if any)  OR  "✓ All links returned OK."
     *   Sub-section: Image Loading
     *     → Table of broken images (if any) OR  "✓ All images loaded successfully."
     *   Sub-section: Link Text Quality
     *     → Descriptive links notes from model
     */
    private static void buildLinkSection(XWPFDocument doc, ValidationContext ctx) {
        addSectionHeading(doc, "Link & Image Validation", false);

        ValidationResultsModel model = ctx.getAggregatedModel();

        // ── HTTP Links ────────────────────────────────────────────────────────────────────────
        addSubHeading(doc, "HTTP Link Status");

        List<String> brokenLinks = ctx.getBrokenLinks();
        if (brokenLinks == null || brokenLinks.isEmpty()) {
            addStatusLine(doc, "✓ All HTTP links returned OK status (tracking links skipped).", COL_PASS);
        } else {
            addStatusLine(doc, brokenLinks.size() + " broken link(s) detected:", COL_FAIL);
            XWPFTable lt = doc.createTable(brokenLinks.size() + 1, 2);
            setTableWidth(lt, 9360);
            int[] lCols = {1400, 7960};
            XWPFTableRow lh = lt.getRow(0);
            setCellWidth(lh.getCell(0), lCols[0]); setCellBg(lh.getCell(0), FILL_D9);
            setCellWidth(lh.getCell(1), lCols[1]); setCellBg(lh.getCell(1), FILL_D9);
            setCellText(lh.getCell(0), "#", true, "000000", 9);
            setCellText(lh.getCell(1), "URL / Error", true, "000000", 9);
            for (int i = 0; i < brokenLinks.size(); i++) {
                XWPFTableRow lr = lt.getRow(i + 1);
                setCellWidth(lr.getCell(0), lCols[0]);
                setCellWidth(lr.getCell(1), lCols[1]);
                setCellText(lr.getCell(0), String.valueOf(i + 1), false, "000000", 9);
                setCellText(lr.getCell(1), trunc(brokenLinks.get(i), 250), false, COL_FAIL, 9);
            }
        }

        doc.createParagraph(); // spacer

        // ── Image Loading ─────────────────────────────────────────────────────────────────────
        addSubHeading(doc, "Image Loading Status");

        List<String> brokenImages = ctx.getBrokenImages();
        if (brokenImages == null || brokenImages.isEmpty()) {
            addStatusLine(doc, "✓ All images loaded successfully.", COL_PASS);
        } else {
            addStatusLine(doc, brokenImages.size() + " broken image(s) detected:", COL_FAIL);
            XWPFTable it = doc.createTable(brokenImages.size() + 1, 3);
            setTableWidth(it, 9360);
            int[] iCols = {500, 5000, 3860};
            XWPFTableRow ih = it.getRow(0);
            for (int c = 0; c < 3; c++) {
                setCellWidth(ih.getCell(c), iCols[c]);
                setCellBg(ih.getCell(c), FILL_D9);
            }
            setCellText(ih.getCell(0), "#",     true, "000000", 9);
            setCellText(ih.getCell(1), "Image URL",   true, "000000", 9);
            setCellText(ih.getCell(2), "Alt Text / Error", true, "000000", 9);
            for (int i = 0; i < brokenImages.size(); i++) {
                XWPFTableRow ir = it.getRow(i + 1);
                for (int c = 0; c < 3; c++) setCellWidth(ir.getCell(c), iCols[c]);
                // brokenImages entries are: "url (error:msg)" or "url (404)"
                String entry = brokenImages.get(i);
                String url   = entry.replaceAll("\\s*\\(.*$", "").trim();
                String error = entry.contains("(") ? entry.replaceAll("^[^(]*\\(|\\)$", "") : "";
                setCellText(ir.getCell(0), String.valueOf(i + 1), false, "000000", 9);
                setCellText(ir.getCell(1), trunc(url, 120),        false, COL_FAIL,  9);
                setCellText(ir.getCell(2), trunc(error, 100),      false, COL_GREY,  9);
            }
        }

        doc.createParagraph(); // spacer

        // ── Descriptive Link Text ─────────────────────────────────────────────────────────────
        addSubHeading(doc, "Link Text Quality");

        ValidationResultsModel.FieldResult linkField =
                model != null ? model.getDescriptiveLinks() : null;
        if (linkField == null) {
            addStatusLine(doc, "Not evaluated.", COL_GREY);
        } else {
            String statusCol = statusColor(linkField.getStatus());
            addStatusLine(doc, linkField.getStatus() + ": " + truncateNotesUrls(linkField.getNotes()), statusCol);
            // If there's evidence (the vague link text + URL) show it
            if (linkField.getEvidence() != null && !linkField.getEvidence().isEmpty()) {
                // Parse "URL (Link text)" format written by LinkValidator
                String ev = linkField.getEvidence();
                // Split on "; " to get multiple entries
                String[] entries = ev.split(";\\s*");
                if (entries.length > 0) {
                    XWPFTable et = doc.createTable(Math.min(entries.length, 10) + 1, 2);
                    setTableWidth(et, 9360);
                    int[] eCols = {4680, 4680};
                    XWPFTableRow eh = et.getRow(0);
                    setCellWidth(eh.getCell(0), eCols[0]); setCellBg(eh.getCell(0), FILL_D9);
                    setCellWidth(eh.getCell(1), eCols[1]); setCellBg(eh.getCell(1), FILL_D9);
                    setCellText(eh.getCell(0), "URL", true, "000000", 9);
                    setCellText(eh.getCell(1), "Link Text (Issue)", true, "000000", 9);
                    int shown = 0;
                    for (String entry : entries) {
                        if (shown >= 10) break;
                        XWPFTableRow er = et.getRow(shown + 1);
                        // Entry format: "URL (link text)" — split on last " ("
                        int parenIdx = entry.lastIndexOf(" (");
                        String url   = parenIdx > 0 ? trunc(entry.substring(0, parenIdx), 100) : trunc(entry, 100);
                        String text  = parenIdx > 0 ? entry.substring(parenIdx + 2).replaceAll("\\)$", "") : "";
                        setCellWidth(er.getCell(0), eCols[0]);
                        setCellWidth(er.getCell(1), eCols[1]);
                        setCellText(er.getCell(0), url,  false, COL_GREY, 9);
                        setCellText(er.getCell(1), text, false, COL_WARN, 9);
                        shown++;
                    }
                }
            }
        }
    }

    // ── Screenshot Section ────────────────────────────────────────────────────────────────────

    /** Builds the email screenshot section. */
    private static void buildScreenshotSection(XWPFDocument doc, byte[] screenshot) {
        addSectionHeading(doc, "Email Screenshot", false);

        if (screenshot != null && screenshot.length > 0) {
            XWPFParagraph imgPara = doc.createParagraph();
            imgPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun imgRun = imgPara.createRun();
            try (ByteArrayInputStream in = new ByteArrayInputStream(screenshot)) {
                // Width: 5.5 inches (7920 EMU * 914400/7920 ≈ fits within margins)
                // Height: auto-scaled to full page width — cap at 9 inches
                imgRun.addPicture(in, XWPFDocument.PICTURE_TYPE_PNG, "email-screenshot.png",
                        Units.toEMU(520),   // ~5.4 inches wide — fits A4/Letter content area
                        Units.toEMU(780));  // ~8.1 inches tall (scales proportionally)
            } catch (Exception e) {
                LOG.warn("Failed to embed screenshot: {}", e.getMessage());
                imgRun.setText("Screenshot capture failed: " + e.getMessage());
            }
        } else {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun r = p.createRun();
            r.setFontFamily("Arial");
            r.setFontSize(10);
            r.setItalic(true);
            r.setColor(COL_GREY);
            r.setText("No screenshot available — Playwright page was not provided or screenshot failed.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // STANDALONE AXE HTML REPORT (attached to Allure separately from DOCX)
    // ══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Generates a self-contained HTML file of axe violations and attaches it to Allure.
     * This is separate from the DOCX embed — it's for browser-level review with hyperlinks
     * and full node snippets.
     */
    @SuppressWarnings("unchecked")
    private static void attachAxeHtmlReport(ValidationContext ctx, String emailName) {
        try {
            List<Object> violations = ctx.getAxeViolations();
            Map<String, Integer> summary = ctx.getAxeSummary();
            int total = violations == null ? 0 : violations.size();

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<title>Axe Report — ").append(esc(emailName)).append("</title>")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;margin:24px;color:#222;font-size:13px}")
                .append("h1{font-size:18px;margin-bottom:4px}")
                .append("h2{font-size:14px;border-bottom:2px solid #ddd;padding-bottom:4px;margin-top:24px}")
                .append(".meta{color:#666;font-size:12px;margin-bottom:16px}")
                .append(".summary{display:flex;gap:10px;margin:12px 0;flex-wrap:wrap;align-items:center}")
                .append(".badge{padding:4px 12px;border-radius:4px;font-weight:bold;color:#fff;font-size:12px}")
                .append(".bc{background:#c00000}.bs{background:#e05a1a}.bm{background:#b97700}.bn{background:#555}.bt{background:#2e4057}.bp{background:#1a7a3c}")
                .append(".note{font-size:11px;color:#777;font-style:italic;margin:0 0 12px}")
                .append("table{border-collapse:collapse;width:100%;margin-top:8px}")
                .append("th{background:#2e4057;color:#fff;padding:8px 10px;text-align:left;font-size:12px}")
                .append("td{padding:7px 10px;border-bottom:1px solid #eee;vertical-align:top;font-size:12px}")
                .append("tr:nth-child(even) td{background:#fafafa}")
                .append("tr:hover td{background:#f0f4f8}")
                .append(".ic{font-weight:bold;border-radius:3px;padding:2px 8px;font-size:11px;color:#fff}")
                .append(".critical{background:#c00000}.serious{background:#e05a1a}")
                .append(".moderate{background:#b97700}.minor{background:#767676}")
                .append(".node{font-family:'Courier New',monospace;font-size:10px;background:#f5f5f5;")
                .append("padding:3px 6px;border-radius:2px;margin:2px 0;display:block;word-break:break-all;")
                .append("border-left:3px solid #ccc}")
                .append(".tag{display:inline-block;background:#e8eaf6;color:#3949ab;border-radius:3px;")
                .append("padding:1px 5px;font-size:10px;margin:2px 2px 0 0}")
                .append("a{color:#1565c0}")
                .append(".more{color:#888;font-size:10px;font-style:italic;margin-top:3px}")
                .append("</style></head><body>");

            html.append("<h1>Axe Accessibility Report</h1>")
                .append("<p class=\"meta\">Email: <strong>").append(esc(emailName)).append("</strong>")
                .append(" &nbsp;|&nbsp; Generated: ").append(DISPLAY_TS.format(Instant.now()))
                .append("</p>");

            // Summary badges
            html.append("<div class=\"summary\">");
            if (total == 0) {
                html.append("<span class=\"badge bp\">✓ No Violations</span>");
            } else {
                html.append("<span class=\"badge bt\">").append(total).append(" rule(s) violated</span>");
                appendHtmlBadge(html, summary, "critical", "Critical", "bc");
                appendHtmlBadge(html, summary, "serious",  "Serious",  "bs");
                appendHtmlBadge(html, summary, "moderate", "Moderate", "bm");
                appendHtmlBadge(html, summary, "minor",    "Minor",    "bn");
            }
            html.append("</div>");

            if (total > 0) {
                html.append("<p class=\"note\">Counts reflect distinct rules violated. ")
                    .append("Each rule may affect multiple HTML elements — see Affected Elements column.</p>");

                html.append("<h2>Violations (").append(total).append(")</h2>")
                    .append("<table><tr>")
                    .append("<th>#</th><th>Rule ID</th><th>Impact</th>")
                    .append("<th>Description</th><th>Help</th><th>WCAG Tags</th><th>Affected Elements</th>")
                    .append("</tr>");

                int row = 1;
                for (Object v : violations) {
                    if (!(v instanceof Map)) continue;
                    Map<String, Object> vmap = (Map<String, Object>) v;
                    String id      = str(vmap, "id");
                    String impact  = str(vmap, "impact");
                    String desc    = str(vmap, "description");
                    String helpUrl = str(vmap, "helpUrl");
                    Object tagsO   = vmap.get("tags");
                    Object nodesO  = vmap.get("nodes");

                    String impactClass = impact.isEmpty() ? "minor" : impact.toLowerCase();

                    // Tags
                    StringBuilder tags = new StringBuilder();
                    if (tagsO instanceof List) {
                        for (Object t : (List<?>) tagsO)
                            tags.append("<span class=\"tag\">").append(esc(t.toString())).append("</span>");
                    }

                    // Nodes — cap at 3, show "… and N more"
                    StringBuilder nodes = new StringBuilder();
                    if (nodesO instanceof List) {
                        List<?> nodeList = (List<?>) nodesO;
                        int totalNodes = nodeList.size();
                        int shown = 0;
                        for (Object n : nodeList) {
                            if (shown >= 3) break;
                            if (!(n instanceof Map)) continue;
                            String nodeHtml = str((Map<String, Object>) n, "html");
                            if (!nodeHtml.isEmpty()) {
                                nodes.append("<span class=\"node\">").append(esc(trunc(nodeHtml, 150))).append("</span>");
                                shown++;
                            }
                        }
                        if (totalNodes > 3)
                            nodes.append("<span class=\"more\">… and ").append(totalNodes - 3).append(" more element(s)</span>");
                    }

                    html.append("<tr>")
                        .append("<td>").append(row++).append("</td>")
                        .append("<td><strong>").append(esc(id)).append("</strong></td>")
                        .append("<td><span class=\"ic ").append(impactClass).append("\">")
                            .append(impact.isEmpty() ? "–" : impact.toUpperCase()).append("</span></td>")
                        .append("<td>").append(esc(desc)).append("</td>")
                        .append("<td>");
                    if (!helpUrl.isEmpty())
                        html.append("<a href=\"").append(esc(helpUrl)).append("\" target=\"_blank\">Learn more</a>");
                    html.append("</td>")
                        .append("<td>").append(tags).append("</td>")
                        .append("<td>").append(nodes).append("</td>")
                        .append("</tr>");
                }
                html.append("</table>");
            } else {
                html.append("<p style=\"color:#1a7a3c;font-weight:bold\">✓ No axe violations detected.</p>");
            }

            html.append("</body></html>");
            byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
                Allure.addAttachment("Axe Accessibility Report.html", "text/html", bis, ".html");
            }
            LOG.info("Attached Axe HTML report ({} violations)", total);

        } catch (Exception e) {
            LOG.warn("Failed to generate Axe HTML report: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // POI HELPERS
    // ══════════════════════════════════════════════════════════════════════════════════════════

    private static void addPageBreak(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setPageBreak(true);
    }

    private static void addSectionHeading(XWPFDocument doc, String text, boolean isFirst) {
        if (!isFirst) {
            // Small spacer before section heading
            XWPFParagraph sp = doc.createParagraph();
            sp.createRun().setText("");
        }
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(isFirst ? 0 : 120);
        p.setSpacingAfter(80);
        // Coloured bottom border as section divider
        CTPPr ppr = p.getCTP().getPPr();
        if (ppr == null) ppr = p.getCTP().addNewPPr();
        CTPBdr bdr = ppr.addNewPBdr();
        CTBorder bot = bdr.addNewBottom();
        bot.setVal(STBorder.SINGLE);
        bot.setSz(BigInteger.valueOf(6));
        bot.setSpace(BigInteger.valueOf(1));
        bot.setColor("2E4057");

        XWPFRun r = p.createRun();
        r.setBold(true);
        r.setFontSize(13);
        r.setFontFamily("Arial");
        r.setColor("2E4057");
        r.setText(text);
    }

    private static void addSubHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(160);
        p.setSpacingAfter(60);
        XWPFRun r = p.createRun();
        r.setBold(true);
        r.setFontSize(11);
        r.setFontFamily("Arial");
        r.setColor("444444");
        r.setText(text);
    }

    private static void addStatusLine(XWPFDocument doc, String text, String color) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(60);
        XWPFRun r = p.createRun();
        r.setFontFamily("Arial");
        r.setFontSize(10);
        r.setColor(color);
        r.setText(text);
    }

    private static void setTableWidth(XWPFTable table, int dxa) {
        CTTblPr tpr = table.getCTTbl().getTblPr();
        if (tpr == null) tpr = table.getCTTbl().addNewTblPr();
        CTTblWidth w = tpr.isSetTblW() ? tpr.getTblW() : tpr.addNewTblW();
        w.setType(STTblWidth.DXA);
        w.setW(BigInteger.valueOf(dxa));
    }

    private static void setCellWidth(XWPFTableCell cell, int dxa) {
        CTTcPr pr = cell.getCTTc().getTcPr();
        if (pr == null) pr = cell.getCTTc().addNewTcPr();
        CTTblWidth w = pr.isSetTcW() ? pr.getTcW() : pr.addNewTcW();
        w.setType(STTblWidth.DXA);
        w.setW(BigInteger.valueOf(dxa));
    }

    private static void setCellBg(XWPFTableCell cell, String hexColor) {
        CTTcPr pr = cell.getCTTc().getTcPr();
        if (pr == null) pr = cell.getCTTc().addNewTcPr();
        CTShd shd = pr.isSetShd() ? pr.getShd() : pr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(hexColor);
    }

    private static void setCellText(XWPFTableCell cell, String text,
                                    boolean bold, String hexColor, int fontSize) {
        // Clear any existing paragraph content
        for (XWPFParagraph p : cell.getParagraphs()) {
            for (int i = p.getRuns().size() - 1; i >= 0; i--) p.removeRun(i);
        }
        XWPFParagraph p = cell.getParagraphs().isEmpty()
                ? cell.addParagraph() : cell.getParagraphs().get(0);
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setBold(bold);
        r.setFontFamily("Arial");
        r.setFontSize(fontSize);
        r.setColor(hexColor);
        r.setText(text == null ? "" : text);
    }

    private static void appendHtmlBadge(StringBuilder sb, Map<String, Integer> summary,
                                        String key, String label, String cls) {
        int count = summary != null ? summary.getOrDefault(key, 0) : 0;
        if (count > 0)
            sb.append("<span class=\"badge ").append(cls).append("\">")
              .append(label).append(": ").append(count).append("</span>");
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // DATA HELPERS
    // ══════════════════════════════════════════════════════════════════════════════════════════

    private static byte[] captureScreenshot(Page page) {
        if (page == null) return null;
        try {
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        } catch (Exception e) {
            LOG.warn("Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }

    private static String buildAxeSummaryLine(int total, Map<String, Integer> summary) {
        StringBuilder sb = new StringBuilder();
        sb.append(total).append(" rule(s) violated");
        if (summary != null && !summary.isEmpty()) {
            sb.append("  —  ");
            List<String> parts = new ArrayList<>();
            for (String level : new String[]{"critical","serious","moderate","minor"}) {
                int n = summary.getOrDefault(level, 0);
                if (n > 0) parts.add(capitalize(level) + ": " + n);
            }
            sb.append(String.join(",  ", parts));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String buildTagsText(Object tagsO) {
        if (!(tagsO instanceof List)) return "";
        List<?> tagList = (List<?>) tagsO;
        // Filter to WCAG-relevant tags only (wcag*, cat.*)
        return tagList.stream()
                .map(Object::toString)
                .filter(t -> t.startsWith("wcag") || t.startsWith("cat.") || t.startsWith("best-"))
                .limit(5)
                .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unchecked")
    private static String buildNodesText(Object nodesO, int maxNodes) {
        if (!(nodesO instanceof List)) return "";
        List<?> nodeList = (List<?>) nodesO;
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Object n : nodeList) {
            if (shown >= maxNodes) break;
            if (!(n instanceof Map)) continue;
            String nodeHtml = str((Map<String, Object>) n, "html");
            if (!nodeHtml.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(trunc(nodeHtml, 100));
                shown++;
            }
        }
        int total = nodeList.size();
        if (total > maxNodes) sb.append("\n… +").append(total - maxNodes).append(" more");
        return sb.toString();
    }

    private static void resolveSubjectPreheader(ReportItem ri, ValidationResultsModel agg,
                                                Map<String, ValidationResult> resultMap,
                                                Map<String, List<String>> aliases) {
        ValidationResultsModel.FieldResult subjF = agg != null ? agg.getSubjectLine() : null;
        ValidationResultsModel.FieldResult preF  = agg != null ? agg.getPreheader()   : null;
        if (subjF == null && preF == null) {
            ValidationResult subjR = lookupResult(resultMap, aliases, "Subject Line");
            ValidationResult preR  = lookupResult(resultMap, aliases, "Preheader Text");
            if (subjR == null && preR == null) {
                ri.status = "NOT EXECUTED";
                ri.notes  = "No Subject or Preheader validation executed";
            } else {
                boolean anyFail = (subjR != null && !subjR.isPassed()) || (preR != null && !preR.isPassed());
                boolean allPass = (subjR == null || subjR.isPassed()) && (preR == null || preR.isPassed());
                ri.status = anyFail ? "FAIL" : (allPass ? "PASS" : "WARN");
                StringBuilder sb = new StringBuilder();
                if (subjR != null) sb.append("Subject: ").append(subjR.getStatus()).append(" - ").append(subjR.getNotes());
                if (preR  != null) { if (sb.length() > 0) sb.append("; "); sb.append("Preheader: ").append(preR.getStatus()).append(" - ").append(preR.getNotes()); }
                ri.notes = sb.toString();
            }
        } else {
            String subjStatus = subjF != null ? subjF.getStatus() : "NOT EXECUTED";
            String preStatus  = preF  != null ? preF.getStatus()  : "NOT EXECUTED";
            ri.status = worstStatus(subjStatus, preStatus);
            StringBuilder sb = new StringBuilder();
            if (subjF != null) sb.append("Subject: ").append(subjStatus).append(" - ").append(subjF.getNotes());
            if (preF  != null) { if (sb.length() > 0) sb.append("; "); sb.append("Preheader: ").append(preStatus).append(" - ").append(preF.getNotes()); }
            ri.notes = sb.toString();
        }
        ri.notes = truncateNotesUrls(ri.notes);
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // STRING / COLOUR HELPERS
    // ══════════════════════════════════════════════════════════════════════════════════════════

    private static String statusColor(String status) {
        if (status == null) return COL_GREY;
        switch (status.toUpperCase()) {
            case "PASS":         return COL_PASS;
            case "FAIL":         return COL_FAIL;
            case "WARN":         return COL_WARN;
            case "NOT EXECUTED": return COL_GREY;
            default:             return COL_GREY;
        }
    }

    private static String impactColor(String impact) {
        if (impact == null) return COL_GREY;
        switch (impact.toLowerCase()) {
            case "critical": return COL_CRITICAL;
            case "serious":  return COL_SERIOUS;
            case "moderate": return COL_MODERATE;
            case "minor":    return COL_MINOR;
            default:         return COL_GREY;
        }
    }

    static String truncateNotesUrls(String notes) {
        if (notes == null || notes.isEmpty()) return notes;
        String[] tokens = notes.split("(?<=[ ;])|(?=[ ;])");
        StringBuilder result = new StringBuilder();
        for (String token : tokens) {
            String trimmed = token.trim();
            if ((trimmed.startsWith("http://") || trimmed.startsWith("https://"))
                    && trimmed.length() > NOTES_URL_MAX_LEN) {
                result.append(trimmed, 0, NOTES_URL_MAX_LEN).append("…");
            } else {
                result.append(token);
            }
        }
        return result.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v == null ? "" : v.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String worstStatus(String a, String b) {
        if ("FAIL".equalsIgnoreCase(a) || "FAIL".equalsIgnoreCase(b)) return "FAIL";
        if ("WARN".equalsIgnoreCase(a) || "WARN".equalsIgnoreCase(b)) return "WARN";
        if ("PASS".equalsIgnoreCase(a) || "PASS".equalsIgnoreCase(b)) return "PASS";
        return "NOT EXECUTED";
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // FIELD / RESULT LOOKUP
    // ══════════════════════════════════════════════════════════════════════════════════════════

    private static ValidationResultsModel.FieldResult getFieldResult(
            ValidationResultsModel model, String rowLabel) {
        if (model == null || rowLabel == null) return null;
        switch (rowLabel) {
            case "Language Attribute (html lang)": return model.getLanguageAttribute();
            case "Meaningful Alt Text":            return model.getAltText();
            case "Decorative Image Handling":      return model.getDecorativeImages();
            case "Heading Hierarchy":              return model.getHeadingHierarchy();
            case "Color Contrast":                 return model.getColorContrast();
            case "Descriptive Link Text":          return model.getDescriptiveLinks();
            case "Button/CTA Discernibility":      return model.getButtonAccessibility();
            case "Font Size & Spacing":            return model.getFontReadability();
            case "HTTP Link Status":               return model.getHttpLinks();
            case "Image Loading Status":           return model.getImageLoading();
            case "SFMC Personalization Strings":   return model.getPersonalizationStrings();
            case "Table Accessibility":            return model.getTableAccessibility();
            case "Mobile Responsiveness":          return model.getMobileResponsiveness();
            case "Unsubscribe Link":               return model.getUnsubscribeLink();
            default:                               return null;
        }
    }

    private static ValidationResult lookupResult(Map<String, ValidationResult> resultMap,
                                                  Map<String, List<String>> aliases,
                                                  String label) {
        if (label == null) return null;
        String key = normalizeKey(label);
        if (resultMap.containsKey(key)) return resultMap.get(key);
        List<String> a = aliases.get(label);
        if (a != null) for (String alias : a) {
            ValidationResult vr = resultMap.get(normalizeKey(alias));
            if (vr != null) return vr;
        }
        for (Map.Entry<String, ValidationResult> e : resultMap.entrySet()) {
            String rn = e.getKey();
            if (rn.contains(key) || key.contains(rn)) return e.getValue();
        }
        return null;
    }

    private static String normalizeKey(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]+", "").trim();
    }

    private static Map<String, ValidationResult> buildResultMap(List<ValidationResult> results) {
        Map<String, ValidationResult> map = new HashMap<>();
        if (results == null) return map;
        for (ValidationResult vr : results) {
            if (vr == null || vr.getItem() == null) continue;
            if ("Runtime Error".equalsIgnoreCase(vr.getItem())) continue;
            map.put(normalizeKey(vr.getItem()), vr);
        }
        return map;
    }

    private static boolean isModelPopulated(ValidationResultsModel m) {
        if (m == null) return false;
        return m.getLanguageAttribute() != null || m.getSubjectLine() != null
            || m.getPreheader() != null || m.getAltText() != null
            || m.getDecorativeImages() != null || m.getHeadingHierarchy() != null
            || m.getColorContrast() != null || m.getDescriptiveLinks() != null
            || m.getButtonAccessibility() != null || m.getFontReadability() != null
            || m.getHttpLinks() != null || m.getImageLoading() != null
            || m.getPersonalizationStrings() != null || m.getTableAccessibility() != null
            || m.getMobileResponsiveness() != null || m.getUnsubscribeLink() != null;
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // DEBUG SNAPSHOT
    // ══════════════════════════════════════════════════════════════════════════════════════════

    private static void writeDebugSnapshot(ValidationContext ctx,
                                           List<ValidationResult> results,
                                           Path resultsDir) {
        try {
            StringBuilder dbg = new StringBuilder();
            dbg.append("=== Runtime Context Info ===\n")
               .append("Constant.PAGE present: ").append(Constant.PAGE != null).append('\n')
               .append("latestEmailHtml length: ")
               .append(Constant.latestEmailHtml == null ? "null" : Constant.latestEmailHtml.length()).append('\n')
               .append("\n=== ValidationResult entries ===\n");
            if (results == null || results.isEmpty()) dbg.append("<no entries>\n");
            else for (ValidationResult vr : results) dbg.append(vr.toString()).append('\n');
            dbg.append("\n=== Aggregated ValidationResultsModel ===\n")
               .append(ctx.getAggregatedModel() == null ? "<null>" : ctx.getAggregatedModel().toString());

            Files.write(resultsDir.resolve("validation-context-debug.txt"),
                    dbg.toString().getBytes(StandardCharsets.UTF_8));
            try (ByteArrayInputStream bis = new ByteArrayInputStream(
                    dbg.toString().getBytes(StandardCharsets.UTF_8))) {
                Allure.addAttachment("ValidationContext Debug (report-start)", "text/plain", bis, ".txt");
            }

            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(new Object() {
                    final Object r = ctx.getResults();
                    final Object a = ctx.getAggregatedModel();
                });
                Files.write(resultsDir.resolve("validation-context-debug.json"),
                        json.getBytes(StandardCharsets.UTF_8));
                try (ByteArrayInputStream jbis = new ByteArrayInputStream(
                        json.getBytes(StandardCharsets.UTF_8))) {
                    Allure.addAttachment("ValidationContext JSON", "application/json", jbis, ".json");
                }
            } catch (Exception je) {
                LOG.warn("Failed to write JSON debug: {}", je.getMessage());
            }
        } catch (Exception e) {
            LOG.warn("Failed to write debug snapshot: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // STATIC DATA — VALIDATION MATRIX + ALIASES
    // ══════════════════════════════════════════════════════════════════════════════════════════

    private static Map<String, List<String>> buildAliases() {
        Map<String, List<String>> m = new HashMap<>();
        m.put("Language Attribute (html lang)", Arrays.asList("Language Attribute", "html-has-lang"));
        m.put("Subject Line & Preheader",        Arrays.asList("Subject Line", "Preheader Text", "Preheader"));
        m.put("Meaningful Alt Text",             Arrays.asList("Image Alt Text", "Image Alt Text Compliance"));
        m.put("Decorative Image Handling",       Arrays.asList("Decorative Images", "decorative"));
        m.put("Heading Hierarchy",               Arrays.asList("heading-order", "Heading Hierarchy"));
        m.put("Descriptive Link Text",           Arrays.asList("Descriptive Link Text", "Descriptive Links", "link-name"));
        m.put("Button/CTA Discernibility",       Arrays.asList("Button Accessibility", "button-name"));
        m.put("Font Size & Spacing",             Arrays.asList("Font Readability", "font-size"));
        m.put("HTTP Link Status",                Arrays.asList("HTTP Link Status", "Broken Link"));
        m.put("Image Loading Status",            Arrays.asList("Image Loading Status", "Image Loading"));
        m.put("SFMC Personalization Strings",    Arrays.asList("SFMC Personalization Strings", "Personalization"));
        m.put("Table Accessibility",             Arrays.asList("Table Accessibility", "table headers"));
        m.put("Mobile Responsiveness",           Arrays.asList("Mobile Responsiveness", "media query"));
        m.put("Unsubscribe Link",                Arrays.asList("Unsubscribe Link", "List-Unsubscribe"));
        return m;
    }

    private static List<ReportItem> buildValidationMatrix() {
        List<ReportItem> list = new ArrayList<>();
        list.add(new ReportItem(1,  "Language Attribute (html lang)", "Check for <html lang=\"en\">.",         Arrays.asList("lang=", "html-has-lang")));
        list.add(new ReportItem(2,  "Subject Line & Preheader",       "Subject and preheader present.",        Arrays.asList("subject", "preheader")));
        list.add(new ReportItem(3,  "Meaningful Alt Text",            "Images must have meaningful alt text.", Arrays.asList("alt", "image alt")));
        list.add(new ReportItem(4,  "Decorative Image Handling",      "Decorative images: empty alt.",         Arrays.asList("decorative", "presentation")));
        list.add(new ReportItem(5,  "Heading Hierarchy",              "H1..H6 in logical order.",              Arrays.asList("heading", "h1", "h2")));
        list.add(new ReportItem(6,  "Color Contrast",                 "WCAG 2.1 AA contrast.",                 Arrays.asList("contrast", "color-contrast")));
        list.add(new ReportItem(7,  "Descriptive Link Text",          "No 'click here' link text.",            Arrays.asList("link text", "click here")));
        list.add(new ReportItem(8,  "Button/CTA Discernibility",      "Buttons keyboard accessible.",          Arrays.asList("button", "cta")));
        list.add(new ReportItem(9,  "Font Size & Spacing",            "Readable font size and spacing.",       Arrays.asList("font-size", "line-height")));
        list.add(new ReportItem(10, "HTTP Link Status",               "All links return 200 OK.",              Arrays.asList("broken link", "404", "500")));
        list.add(new ReportItem(11, "Image Loading Status",           "All images load successfully.",         Arrays.asList("broken image", "image")));
        list.add(new ReportItem(12, "SFMC Personalization Strings",   "No raw %%name%% tokens.",               Arrays.asList("personalization", "%%")));
        list.add(new ReportItem(13, "Table Accessibility",            "Tables have headers.",                  Arrays.asList("table", "th")));
        list.add(new ReportItem(14, "Mobile Responsiveness",          "Responsive / mobile-friendly.",         Arrays.asList("mobile", "responsive")));
        list.add(new ReportItem(15, "Unsubscribe Link",               "Unsubscribe link present.",             Arrays.asList("unsubscribe", "opt-out")));
        return list;
    }

    private static String safeFilename(String s) {
        if (s == null) return "email";
        return s.replaceAll("[\\/:*?\"<>|\\s]+", "_").replaceAll("^_+|_+$", "");
    }

    private static final class ReportItem {
        final int sno; final String item; final String defaultNotes;
        final List<String> matchKeywords;
        String status; String notes;
        ReportItem(int sno, String item, String defaultNotes, List<String> matchKeywords) {
            this.sno = sno; this.item = item; this.defaultNotes = defaultNotes;
            this.matchKeywords = matchKeywords.stream().map(String::toLowerCase).collect(Collectors.toList());
            this.status = "PASS"; this.notes = defaultNotes;
        }
    }
}
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
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
 * FIXES IN THIS VERSION:
 *
 *  Fix B (WARN vs FAIL) — status read from FieldResult.getStatus() directly, not from
 *  boolean-based ValidationResult.getStatus(). Now correctly shows PASS/FAIL/WARN.
 *
 *  Fix C (Notes truncation) — long URLs in the Notes column are now truncated to 60 chars
 *  with "…" so the Word table remains readable. Full evidence is in the axe HTML report.
 *
 *  Fix D (axe HTML report) — after generating the DOCX, attaches a colour-coded HTML
 *  table of all axe violations to Allure as a downloadable .html file.
 */
public final class WordReportGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(WordReportGenerator.class);
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    /** Max length for a URL shown in the Notes column before it is truncated. */
    private static final int NOTES_URL_MAX_LEN = 60;

    private WordReportGenerator() {}

    public static byte[] generateAuditReport(String emailName, Page page) throws Exception {
        Objects.requireNonNull(emailName, "emailName must not be null");

        Path resultsDir = Paths.get("results");
        if (!Files.exists(resultsDir)) Files.createDirectories(resultsDir);

        String safeEmail = emailName.replaceAll("[\\/:*?\"<>|\\s]+", "_").replaceAll("^_+|_+$", "");
        String fileName = String.format("Audit_%s_%s.docx", safeEmail, TS.format(Instant.now()));
        Path outFile = resultsDir.resolve(fileName);

        List<ReportItem> items = buildValidationMatrix();
        ValidationContext ctx = ValidationContext.getInstance();
        List<ValidationResult> results = ctx.getResults();

        // ── Debug snapshot ─────────────────────────────────────────────────────────────────────
        writeDebugSnapshot(ctx, results, resultsDir);

        // ── On-demand validation fallback ──────────────────────────────────────────────────────
        boolean hasAggregated = isModelPopulated(ctx.getAggregatedModel());
        Map<String, ValidationResult> resultMap = buildResultMap(results);

        if (resultMap.isEmpty() && !hasAggregated) {
            LOG.info("No structured ValidationResult entries found; running validations on-demand");
            try {
                EmailValidationService svc = new EmailValidationService(ctx);
                svc.runAllValidations(Constant.PAGE, Constant.latestEmailHtml, null);
                resultMap = buildResultMap(ctx.getResults());
            } catch (Exception e) {
                LOG.warn("On-demand validations failed: {}", e.getMessage());
            }
        }

        Map<String, List<String>> aliases = buildAliases();
        ValidationResultsModel agg = ctx.getAggregatedModel();

        // ── Map report rows ────────────────────────────────────────────────────────────────────
        for (ReportItem ri : items) {

            if (ri.item.equalsIgnoreCase("Subject Line & Preheader")) {
                ValidationResultsModel.FieldResult subjF = agg != null ? agg.getSubjectLine() : null;
                ValidationResultsModel.FieldResult preF  = agg != null ? agg.getPreheader()   : null;

                if (subjF == null && preF == null) {
                    ValidationResult subjR = lookupResult(resultMap, aliases, "Subject Line");
                    ValidationResult preR  = lookupResult(resultMap, aliases, "Preheader Text");
                    if (subjR == null && preR == null) {
                        ri.status = "NOT EXECUTED"; ri.notes = "No Subject or Preheader validation executed";
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
                continue;
            }

            // FIX B: read status from FieldResult (3-state) not ValidationResult (boolean)
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

            // FIX C: truncate long URLs in notes so the Word table stays readable
            ri.notes = truncateNotesUrls(ri.notes);
        }

        // ── Build DOCX ─────────────────────────────────────────────────────────────────────────
        byte[] docBytes;
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Title
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("SFMC Email Audit Report");
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.addBreak();

            XWPFParagraph meta = doc.createParagraph();
            meta.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun metaRun = meta.createRun();
            metaRun.setText("Email: " + emailName);
            metaRun.addBreak();
            metaRun.setText("Generated: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault()).format(Instant.now()));
            metaRun.addBreak();

            // Table
            XWPFTable table = doc.createTable(items.size() + 1, 4);
            XWPFTableRow header = table.getRow(0);
            header.getCell(0).setText("S.No");
            header.getCell(1).setText("Items to Test");
            header.getCell(2).setText("Status");
            header.getCell(3).setText("Notes");
            for (int c = 0; c < 4; c++) header.getCell(c).getCTTc().addNewTcPr().addNewShd().setFill("D9D9D9");

            for (int i = 0; i < items.size(); i++) {
                XWPFTableRow row = table.getRow(i + 1);
                ReportItem ri = items.get(i);
                row.getCell(0).setText(String.valueOf(ri.sno));
                row.getCell(1).setText(ri.item);

                XWPFParagraph statusP = row.getCell(2).getParagraphs().get(0);
                XWPFRun statusRun = statusP.createRun();
                statusRun.setBold(true);
                // FIX B: WARN gets amber colour — not just PASS/FAIL
                switch (ri.status == null ? "" : ri.status.toUpperCase()) {
                    case "PASS":         statusRun.setColor("008000"); break; // green
                    case "FAIL":         statusRun.setColor("FF0000"); break; // red
                    case "WARN":         statusRun.setColor("B97700"); break; // amber
                    case "NOT EXECUTED": statusRun.setColor("888888"); break; // grey
                    default:             statusRun.setColor("888888"); break;
                }
                statusRun.setText(ri.status == null ? "NOT EXECUTED" : ri.status);
                row.getCell(3).setText(ri.notes == null ? "" : ri.notes);
            }

            // Visual Evidence
            XWPFParagraph visTitle = doc.createParagraph();
            visTitle.setSpacingBefore(200);
            XWPFRun visRun = visTitle.createRun();
            visRun.setBold(true);
            visRun.setText("Visual Audit");
            visRun.addBreak();

            byte[] screenshot = null;
            try {
                if (page != null) screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            } catch (Exception e) {
                LOG.warn("Failed to capture screenshot: {}", e.getMessage());
            }

            if (screenshot != null && screenshot.length > 0) {
                XWPFParagraph imgPara = doc.createParagraph();
                imgPara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun imgRun = imgPara.createRun();
                try (ByteArrayInputStream in = new ByteArrayInputStream(screenshot)) {
                    imgRun.addPicture(in, Document.PICTURE_TYPE_PNG, "screenshot.png",
                            Units.toEMU(600), Units.toEMU(800));
                } catch (Exception e) {
                    LOG.warn("Failed to embed screenshot: {}", e.getMessage());
                }
            } else {
                doc.createParagraph().createRun().setText("No screenshot available.");
            }

            doc.write(out);
            docBytes = out.toByteArray();
        }

        Files.write(outFile, docBytes);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(docBytes)) {
            Allure.addAttachment(
                    "Audit Report - " + safeFilename(emailName),
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    bis, ".docx");
        } catch (Exception e) {
            LOG.warn("Failed to attach audit report to Allure: {}", e.getMessage());
        }

        // FIX D: attach axe violations as a colour-coded HTML table
        attachAxeHtmlReport(ctx, emailName);

        LOG.info("Saved audit report to {}", outFile.toAbsolutePath());
        return docBytes;
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // FIX C — Notes URL truncation
    // ══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Replaces long URLs inside a notes string with a truncated form.
     * A "URL" is detected as any token starting with http:// or https://.
     * Only the URL itself is truncated — surrounding text (e.g. "(click here)") is kept.
     *
     * Example:
     *   "Non-descriptive link text found; https://cl.s12.exct.net/?qs=eyJk…very_long… (click here)"
     *   → "Non-descriptive link text found; https://cl.s12.exct.net/?qs=eyJk… (click here)"
     */
    static String truncateNotesUrls(String notes) {
        if (notes == null || notes.isEmpty()) return notes;
        // Split on spaces and semicolons, rebuild with truncated URLs
        StringBuilder result = new StringBuilder();
        // Process token by token (split keeping delimiters via regex lookahead/lookbehind)
        String[] tokens = notes.split("(?<=[ ;])|(?=[ ;])");
        for (String token : tokens) {
            String trimmed = token.trim();
            if ((trimmed.startsWith("http://") || trimmed.startsWith("https://"))
                    && trimmed.length() > NOTES_URL_MAX_LEN) {
                // Preserve any trailing non-URL suffix (e.g. space before "(click here)")
                result.append(trimmed, 0, NOTES_URL_MAX_LEN).append("…");
            } else {
                result.append(token);
            }
        }
        return result.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // FIX D — Axe HTML report attachment
    // ══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Generates a colour-coded HTML table of axe-core violations and attaches it to Allure.
     * Opens directly in any browser — no JSON parsing needed.
     *
     * Impact colours: critical=red, serious=orange-red, moderate=amber, minor=grey.
     */
    @SuppressWarnings("unchecked")
    private static void attachAxeHtmlReport(ValidationContext ctx, String emailName) {
        try {
            List<Object> violations = ctx.getAxeViolations();
            Map<String, Integer> summary = ctx.getAxeSummary();
            int total = violations == null ? 0 : violations.size();

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<title>Axe Report - ").append(esc(emailName)).append("</title>")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;margin:24px;color:#222;font-size:13px}")
                .append("h1{font-size:18px}h2{font-size:14px;border-bottom:2px solid #ddd;padding-bottom:4px;margin-top:20px}")
                .append(".summary{display:flex;gap:10px;margin:12px 0;flex-wrap:wrap}")
                .append(".badge{padding:5px 12px;border-radius:4px;font-weight:bold;color:#fff;font-size:12px}")
                .append(".bc{background:#c00}.bs{background:#e44}.bm{background:#d07000}.bn{background:#777}.bt{background:#333}.bp{background:#2e7d32}")
                .append("table{border-collapse:collapse;width:100%}th{background:#2E4057;color:#fff;padding:7px 10px;text-align:left;font-size:12px}")
                .append("td{padding:6px 10px;border-bottom:1px solid #eee;vertical-align:top;font-size:12px}")
                .append("tr:hover td{background:#f9f9f9}")
                .append(".ic{font-weight:bold;border-radius:3px;padding:2px 7px;font-size:11px}")
                .append(".critical{color:#fff;background:#c00}.serious{color:#fff;background:#e44}")
                .append(".moderate{color:#fff;background:#d07000}.minor{color:#555;background:#eee}")
                .append(".node{font-family:monospace;font-size:11px;background:#f5f5f5;padding:2px 5px;border-radius:2px;margin:2px 0;display:block;word-break:break-all}")
                .append(".tag{display:inline-block;background:#e8eaf6;color:#3949ab;border-radius:3px;padding:1px 5px;font-size:10px;margin:1px}")
                .append("a{color:#1565c0}")
                .append("</style></head><body>");

            html.append("<h1>Axe Accessibility Report</h1>")
                .append("<p style=\"color:#555\">Email: <strong>").append(esc(emailName)).append("</strong></p>");

            // Summary badges — counts are per RULE (not per affected element)
            html.append("<div class=\"summary\">");
            if (total == 0) {
                html.append("<span class=\"badge bp\">✓ No Violations</span>");
            } else {
                html.append("<span class=\"badge bt\">Total: ").append(total).append(" rule(s)</span>");
                appendBadge(html, summary, "critical", "Critical", "bc");
                appendBadge(html, summary, "serious",  "Serious",  "bs");
                appendBadge(html, summary, "moderate", "Moderate", "bm");
                appendBadge(html, summary, "minor",    "Minor",    "bn");
            }
            html.append("</div>");
            // Clarifying note so counts are not misread as element counts
            if (total > 0) {
                html.append("<p style=\"font-size:11px;color:#777;margin:0 0 12px 0\">")
                    .append("Counts show number of distinct accessibility rules violated. ")
                    .append("Each rule may affect multiple HTML elements — see the Affected Elements column for details.")
                    .append("</p>");
            }

            if (violations != null && !violations.isEmpty()) {
                html.append("<h2>Violations (").append(total).append(")</h2>")
                    .append("<table><tr><th>#</th><th>Rule ID</th><th>Impact</th>")
                    .append("<th>Description</th><th>Help</th><th>WCAG Tags</th><th>Affected Elements</th></tr>");

                int row = 1;
                for (Object v : violations) {
                    if (!(v instanceof Map)) continue;
                    Map<String, Object> vmap = (Map<String, Object>) v;
                    String id     = str(vmap, "id");
                    String impact = str(vmap, "impact");
                    String desc   = str(vmap, "description");
                    String help   = str(vmap, "helpUrl");
                    Object tagsO  = vmap.get("tags");
                    Object nodesO = vmap.get("nodes");

                    String impactClass = impact == null ? "minor" : impact.toLowerCase();

                    // Build tags
                    StringBuilder tags = new StringBuilder();
                    if (tagsO instanceof List) {
                        for (Object t : (List<?>) tagsO)
                            tags.append("<span class=\"tag\">").append(esc(t.toString())).append("</span>");
                    }

                    // Build affected nodes — cap at 3 to avoid flooding the report
                    // (e.g. the "region" rule can flag every div in an email = 16 elements)
                    StringBuilder nodes = new StringBuilder();
                    if (nodesO instanceof List) {
                        List<?> nodeList = (List<?>) nodesO;
                        int totalNodes = nodeList.size();
                        int shown = 0;
                        for (Object n : nodeList) {
                            if (shown >= 3) break;
                            if (!(n instanceof Map)) continue;
                            Map<String, Object> nmap = (Map<String, Object>) n;
                            String nodeHtml = str(nmap, "html");
                            if (!nodeHtml.isEmpty()) {
                                nodes.append("<span class=\"node\">").append(esc(trunc(nodeHtml, 120))).append("</span>");
                                shown++;
                            }
                        }
                        if (totalNodes > 3) {
                            nodes.append("<span style=\"color:#888;font-size:11px;font-style:italic\">")
                                 .append("… and ").append(totalNodes - 3).append(" more element(s)")
                                 .append("</span>");
                        }
                    }

                    html.append("<tr>")
                        .append("<td>").append(row++).append("</td>")
                        .append("<td><strong>").append(esc(id)).append("</strong></td>")
                        .append("<td><span class=\"ic ").append(impactClass).append("\">")
                        .append(impact == null ? "-" : impact.toUpperCase()).append("</span></td>")
                        .append("<td>").append(esc(desc)).append("</td>")
                        .append("<td>");
                    if (!help.isEmpty())
                        html.append("<a href=\"").append(esc(help)).append("\" target=\"_blank\">Learn more</a>");
                    html.append("</td>")
                        .append("<td>").append(tags).append("</td>")
                        .append("<td>").append(nodes).append("</td>")
                        .append("</tr>");
                }
                html.append("</table>");
            } else {
                html.append("<p style=\"color:#2e7d32;font-weight:bold\">✓ No axe violations detected.</p>");
            }

            html.append("</body></html>");
            byte[] htmlBytes = html.toString().getBytes(StandardCharsets.UTF_8);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(htmlBytes)) {
                Allure.addAttachment(
                        "Axe Accessibility Report.html",
                        "text/html",
                        bis,
                        ".html");
            }
            LOG.info("Attached axe HTML report ({} violations)", total);

        } catch (Exception e) {
            LOG.warn("Failed to generate axe HTML report: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────────────────

    private static void appendBadge(StringBuilder sb, Map<String, Integer> summary,
                                     String key, String label, String cls) {
        int count = summary != null ? summary.getOrDefault(key, 0) : 0;
        if (count > 0)
            sb.append("<span class=\"badge ").append(cls).append("\">")
              .append(label).append(": ").append(count).append("</span>");
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v == null ? "" : v.toString();
    }
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
    private static String trunc(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max) + "…");
    }
    private static String worstStatus(String a, String b) {
        if ("FAIL".equalsIgnoreCase(a) || "FAIL".equalsIgnoreCase(b)) return "FAIL";
        if ("WARN".equalsIgnoreCase(a) || "WARN".equalsIgnoreCase(b)) return "WARN";
        if ("PASS".equalsIgnoreCase(a) || "PASS".equalsIgnoreCase(b)) return "PASS";
        return "NOT EXECUTED";
    }

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

    private static Map<String, ValidationResult> buildResultMap(List<ValidationResult> results) {
        Map<String, ValidationResult> map = new HashMap<>();
        if (results == null) return map;
        for (ValidationResult vr : results) {
            if (vr == null || vr.getItem() == null) continue;
            // Skip the legacy "Runtime Error" key — it is not a named report row
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

    private static void writeDebugSnapshot(ValidationContext ctx,
                                            List<ValidationResult> results,
                                            Path resultsDir) {
        try {
            StringBuilder dbg = new StringBuilder();
            dbg.append("=== Early ValidationContext Snapshot ===\n")
               .append("Constant.PAGE present: ").append(Constant.PAGE != null).append('\n')
               .append("latestEmailHtml length: ")
               .append(Constant.latestEmailHtml == null ? "null" : Constant.latestEmailHtml.length()).append('\n')
               .append("--- ValidationResult entries ---\n");
            if (results == null || results.isEmpty()) dbg.append("<no entries>\n");
            else for (ValidationResult vr : results) dbg.append(vr.toString()).append('\n');
            dbg.append("\n--- Aggregated Model ---\n")
               .append(ctx.getAggregatedModel() == null ? "<null>" : ctx.getAggregatedModel().toString()).append('\n');

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
                    Allure.addAttachment("ValidationContext JSON (report-start)", "application/json", jbis, ".json");
                }
            } catch (Exception je) {
                LOG.warn("Failed to write JSON debug: {}", je.getMessage());
            }
        } catch (Exception e) {
            LOG.warn("Failed to write debug snapshot: {}", e.getMessage());
        }
    }

    private static ValidationResult lookupResult(Map<String, ValidationResult> resultMap,
                                                   Map<String, List<String>> aliases,
                                                   String label) {
        if (label == null) return null;
        String key = normalizeKey(label);
        if (resultMap.containsKey(key)) return resultMap.get(key);
        List<String> a = aliases.get(label);
        if (a != null) {
            for (String alias : a) {
                ValidationResult vr = resultMap.get(normalizeKey(alias));
                if (vr != null) return vr;
            }
        }
        for (Map.Entry<String, ValidationResult> e : resultMap.entrySet()) {
            String rn = e.getKey();
            if (rn.contains(key) || key.contains(rn)) return e.getValue();
        }
        return null;
    }

    private static String normalizeKey(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "").trim();
    }

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
package framework.validation.reporting;

import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResult;
import framework.validation.model.ValidationResultsModel;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);
    private final ValidationContext context;

    public ReportGenerator(ValidationContext context) {
        this.context = context;
    }

    public String generateHtml() {
        logger.info("Generating consolidated HTML report (aggregated model)");
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"utf-8\"><title>Email Validation Report</title>");
        sb.append("<style>body{font-family:Arial,Helvetica,sans-serif}table{border-collapse:collapse;width:100%}th,td{padding:8px;border:1px solid #ddd}th{background:#f4f4f4}.pass{background:#d4edda}.fail{background:#f8d7da}.warn{background:#fff3cd}</style>");
        sb.append("</head><body>");
        sb.append("<h1>Email Validation Report</h1>");

        ValidationResultsModel model = context.getAggregatedModel();
        sb.append("<h2>Validation Summary</h2>");
        sb.append("<table>");
        sb.append("<tr><th>Item</th><th>Status</th><th>Notes</th><th>Evidence</th></tr>");

        renderRow(sb, "Language Attribute", model.getLanguageAttribute());
        renderRow(sb, "Subject Line", model.getSubjectLine());
        renderRow(sb, "Preheader", model.getPreheader());
        renderRow(sb, "Alt Text", model.getAltText());
        renderRow(sb, "Decorative Images", model.getDecorativeImages());
        renderRow(sb, "Heading Hierarchy", model.getHeadingHierarchy());
        renderRow(sb, "Color Contrast", model.getColorContrast());
        renderRow(sb, "Descriptive Links", model.getDescriptiveLinks());
        renderRow(sb, "Button Accessibility", model.getButtonAccessibility());
        renderRow(sb, "Font Readability", model.getFontReadability());
        renderRow(sb, "HTTP Links", model.getHttpLinks());
        renderRow(sb, "Image Loading", model.getImageLoading());
        renderRow(sb, "Personalization Strings", model.getPersonalizationStrings());
        renderRow(sb, "Table Accessibility", model.getTableAccessibility());
        renderRow(sb, "Mobile Responsiveness", model.getMobileResponsiveness());
        renderRow(sb, "Unsubscribe Link", model.getUnsubscribeLink());

        sb.append("</table>");

        // Append legacy result list for debugging
        sb.append("<h2>Legacy Validator Results</h2>");
        List<ValidationResult> results = context.getResults();
        sb.append(String.format("<p>Total checks: %d</p>", results.size()));
        sb.append("<table>");
        sb.append("<tr><th>Item</th><th>Status</th><th>Severity</th><th>Notes</th></tr>");
        for (ValidationResult r : results) {
            String cls = r.isPassed() ? "pass" : "fail";
            sb.append(String.format("<tr class=\"%s\"><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>", cls, escape(r.getItem()), r.getStatus(), escape(r.getSeverity()), escape(r.getNotes())));
        }
        sb.append("</table>");

        // Broken links
        if (!context.getBrokenLinks().isEmpty()) {
            sb.append("<h2>Broken Links</h2>");
            sb.append("<ul>");
            for (String b : context.getBrokenLinks()) {
                sb.append(String.format("<li>%s</li>", escape(b)));
            }
            sb.append("</ul>");
        }

        // Axe violations raw
        if (!context.getAxeViolations().isEmpty()) {
            sb.append("<h2>Axe Violations (raw)</h2>");
            sb.append(String.format("<pre>%s</pre>", escape(context.getAxeViolations().toString())));
        }

        sb.append("</body></html>");
        String html = sb.toString();
        Allure.addAttachment("Email Validation Report (HTML)", new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
        return html;
    }

    private void renderRow(StringBuilder sb, String label, ValidationResultsModel.FieldResult field) {
        String status = "WARN";
        String notes = "Not evaluated";
        String evidence = "";
        if (field != null) {
            status = field.getStatus();
            notes = field.getNotes();
            evidence = field.getEvidence();
        }
        String cls = "warn";
        if ("PASS".equalsIgnoreCase(status)) cls = "pass";
        if ("FAIL".equalsIgnoreCase(status)) cls = "fail";

        sb.append(String.format("<tr class=\"%s\"><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>", cls, escape(label), escape(status), escape(notes), escape(evidence)));
    }

    /**
     * Generate a very basic single-page PDF and auxiliary files under target/email-audit.
     */
    public byte[] generatePdfAndWriteFiles(String emailHtml) throws IOException {
        logger.info("Generating PDF report (basic) and auxiliary files");
        Path targetDir = Paths.get("target", "email-audit");
        Files.createDirectories(targetDir);
        Path pdfPath = targetDir.resolve("email-audit-report.pdf");

        StringBuilder content = new StringBuilder();
        content.append("Email Validation Audit Report\n\n");
        content.append("Validation Summary:\n");

        ValidationResultsModel model = context.getAggregatedModel();
        appendField(content, "Language Attribute", model.getLanguageAttribute());
        appendField(content, "Subject Line", model.getSubjectLine());
        appendField(content, "Preheader", model.getPreheader());
        appendField(content, "Alt Text", model.getAltText());
        appendField(content, "Decorative Images", model.getDecorativeImages());
        appendField(content, "Heading Hierarchy", model.getHeadingHierarchy());
        appendField(content, "Color Contrast", model.getColorContrast());
        appendField(content, "Descriptive Links", model.getDescriptiveLinks());
        appendField(content, "Button Accessibility", model.getButtonAccessibility());
        appendField(content, "Font Readability", model.getFontReadability());
        appendField(content, "HTTP Links", model.getHttpLinks());
        appendField(content, "Image Loading", model.getImageLoading());
        appendField(content, "Personalization Strings", model.getPersonalizationStrings());
        appendField(content, "Table Accessibility", model.getTableAccessibility());
        appendField(content, "Mobile Responsiveness", model.getMobileResponsiveness());
        appendField(content, "Unsubscribe Link", model.getUnsubscribeLink());

        content.append("\nAccessibility Summary:\n");
        if (context.getAxeSummary().isEmpty()) {
            content.append("No accessibility summary available\n");
        } else {
            for (var e : context.getAxeSummary().entrySet()) {
                content.append(String.format("%s : %d\n", e.getKey(), e.getValue()));
            }
        }

        if (!context.getBrokenLinks().isEmpty()) {
            content.append("\nBroken Links:\n");
            for (String b : context.getBrokenLinks()) content.append("- ").append(b).append("\n");
        }

        content.append("\nRecommendations:\n");
        content.append("Review items marked FAIL or WARNING. Address color contrast and unsubscribe issues as priority.\n");

        try (OutputStream os = Files.newOutputStream(pdfPath)) {
            writeSimplePdf(os, content.toString());
        }

        Path axeJson = targetDir.resolve("axe-results.json");
        try (Writer w = Files.newBufferedWriter(axeJson, StandardCharsets.UTF_8)) {
            w.write(context.getAxeViolations().toString());
        }

        Path blJson = targetDir.resolve("broken-links.json");
        try (Writer w = Files.newBufferedWriter(blJson, StandardCharsets.UTF_8)) {
            w.write(context.getBrokenLinks().toString());
        }

        Path htmlOut = targetDir.resolve("email-html.html");
        try (Writer w = Files.newBufferedWriter(htmlOut, StandardCharsets.UTF_8)) {
            w.write(emailHtml == null ? "" : emailHtml);
        }

        try (InputStream is = Files.newInputStream(pdfPath)) {
            Allure.addAttachment("Email Audit Report", is);
        }
        try (InputStream is = Files.newInputStream(axeJson)) {
            Allure.addAttachment("axe-results.json", is);
        }
        try (InputStream is = Files.newInputStream(blJson)) {
            Allure.addAttachment("broken-links.json", is);
        }
        try (InputStream is = Files.newInputStream(htmlOut)) {
            Allure.addAttachment("email-html.html", is);
        }

        byte[] pdfBytes = Files.readAllBytes(pdfPath);
        logger.info("Saved PDF report to {}", pdfPath.toAbsolutePath());
        return pdfBytes;
    }

    private void appendField(StringBuilder sb, String label, ValidationResultsModel.FieldResult f) {
        if (f == null) {
            sb.append(String.format("- %s : %s -- %s\n", label, "WARN", "Not evaluated"));
        } else {
            sb.append(String.format("- %s : %s -- %s\n", label, f.getStatus(), f.getNotes()));
            if (f.getEvidence() != null && !f.getEvidence().isEmpty()) sb.append(String.format("  Evidence: %s\n", f.getEvidence()));
        }
    }

    private void writeSimplePdf(OutputStream os, String text) throws IOException {
        String utf8Text = text.replace("\r\n", "\n");
        String[] lines = utf8Text.split("\n");
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/Helv 12 Tf\n72 720 Td\n");
        for (String line : lines) {
            String esc = line.replace("(", "\\(").replace(")", "\\)");
            stream.append("0 0 0 rg\n(").append(esc).append(") Tj\n0 -14 Td\n");
        }
        stream.append("ET\n");
        byte[] streamBytes = stream.toString().getBytes(StandardCharsets.UTF_8);
        String header = "%PDF-1.1\n%âãÏÓ\n";
        os.write(header.getBytes(StandardCharsets.US_ASCII));
        os.write(streamBytes);
        os.flush();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

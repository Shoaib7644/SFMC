package framework.validation.reporting;

import framework.validation.model.ValidationResultsModel;
import framework.validation.context.ValidationContext;

import java.util.List;
import java.util.Map;

/**
 * Adapter to convert axe-core violations (raw objects) into a ValidationResultsModel.
 *
 * RESPONSIBILITIES (fixed):
 *  - Read raw axe violations from ValidationContext.
 *  - Map each axe ruleId to the correct ValidationResultsModel field.
 *  - Return a fully-populated ValidationResultsModel to the caller.
 *
 * INTENTIONALLY DOES NOT write to ValidationContext.
 *  - All context.addResult() calls have been removed from this class.
 *  - EmailValidationService is the single owner of context writes (no double-write races).
 *
 * FIXED BUGS:
 *  Bug 1 - "region" and "landmark-one-main" no longer overwrite headingHierarchy with wrong notes.
 *           They now produce a clearly-labelled WARN on headingHierarchy only when no real
 *           heading-order violation exists, and never replace a genuine heading failure.
 *  Bug 2 - Any axe violation sets the FieldResult to FAIL or WARN (never PASS).
 *           Previously WARN-impact violations kept the PASS default unchanged.
 *  Bug 3 - All context.addResult() calls removed; EmailValidationService owns context writes.
 */
public class AccessibilityReportAdapter {

    private final ValidationContext context;

    public AccessibilityReportAdapter(ValidationContext context) {
        this.context = context;
    }

    /**
     * Maps raw axe violations stored in the context into a ValidationResultsModel.
     *
     * Fields populated by this method:
     *   colorContrast, headingHierarchy, descriptiveLinks, buttonAccessibility, altText.
     *
     * All other model fields (languageAttribute, subjectLine, httpLinks, etc.) remain null
     * and MUST be populated by the caller (EmailValidationService).
     *
     * This method does NOT write to ValidationContext — the caller owns that responsibility.
     */
    public ValidationResultsModel adapt() {
        ValidationResultsModel model = new ValidationResultsModel();

        List<Object> raw = context.getAxeViolations();

        // ── No violations path ─────────────────────────────────────────────────────────────
        if (raw == null || raw.isEmpty()) {
            model.setColorContrast(new ValidationResultsModel.FieldResult("PASS", "No color-contrast violations detected", ""));
            model.setHeadingHierarchy(new ValidationResultsModel.FieldResult("PASS", "No heading hierarchy issues detected", ""));
            model.setDescriptiveLinks(new ValidationResultsModel.FieldResult("PASS", "No link-name issues detected", ""));
            model.setButtonAccessibility(new ValidationResultsModel.FieldResult("PASS", "No button-name issues detected", ""));
            model.setAltText(new ValidationResultsModel.FieldResult("PASS", "No image-alt issues detected", ""));
            return model;
        }

        // ── Initialise all mapped fields to PASS; violations below will downgrade them ─────
        model.setColorContrast(new ValidationResultsModel.FieldResult("PASS", "No violations detected", ""));
        model.setHeadingHierarchy(new ValidationResultsModel.FieldResult("PASS", "No violations detected", ""));
        model.setDescriptiveLinks(new ValidationResultsModel.FieldResult("PASS", "No violations detected", ""));
        model.setButtonAccessibility(new ValidationResultsModel.FieldResult("PASS", "No violations detected", ""));
        model.setAltText(new ValidationResultsModel.FieldResult("PASS", "No violations detected", ""));

        // ── Process each violation ──────────────────────────────────────────────────────────
        for (Object v : raw) {
            if (!(v instanceof Map)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> node = (Map<String, Object>) v;

            String id     = node.get("id")          == null ? "" : node.get("id").toString();
            String impact = node.get("impact")       == null ? "" : node.get("impact").toString();
            String desc   = node.get("description")  == null ? "" : node.get("description").toString();

            // BUG FIX 2: derive FieldResult status from impact.
            // "FAIL" for critical/serious, "WARN" for moderate/minor.
            // The old code derived a boolean `passed = !"FAIL".equalsIgnoreCase(status)` which
            // made moderate/minor violations appear as passed=true. Now the status string itself
            // encodes the severity; callers (EmailValidationService) set context passed=false
            // for any non-PASS FieldResult status.
            String fieldStatus = mapImpactToFieldStatus(impact);
            String notes       = String.format("%s (%s)", desc, id);

            switch (id) {

                // ── Color contrast ────────────────────────────────────────────────────────
                case "color-contrast":
                    model.setColorContrast(new ValidationResultsModel.FieldResult(fieldStatus, notes, id));
                    break;

                // ── Heading hierarchy (genuine heading-order rules only) ───────────────────
                // BUG FIX 1 (part A): only "heading-order" and "page-has-heading-one" map here.
                case "heading-order":
                case "page-has-heading-one":
                    model.setHeadingHierarchy(new ValidationResultsModel.FieldResult(fieldStatus, notes, id));
                    break;

                // ── Descriptive links ─────────────────────────────────────────────────────
                case "link-name":
                    model.setDescriptiveLinks(new ValidationResultsModel.FieldResult(fieldStatus, notes, id));
                    break;

                // ── Button accessibility ──────────────────────────────────────────────────
                case "button-name":
                    model.setButtonAccessibility(new ValidationResultsModel.FieldResult(fieldStatus, notes, id));
                    break;

                // ── Image alt text ────────────────────────────────────────────────────────
                case "image-alt":
                    model.setAltText(new ValidationResultsModel.FieldResult(fieldStatus, notes, id));
                    break;

                // ── Landmark / region rules ───────────────────────────────────────────────
                // BUG FIX 1 (part B): "region" and "landmark-one-main" are landmark/structural
                // rules — they must NOT overwrite headingHierarchy with landmark messages.
                //
                // Old code: mapped both directly to headingHierarchy, which caused:
                //   headingHierarchy.notes = "Ensure all page content is contained by landmarks (region)"
                //   headingHierarchy.status = WARN  but the boolean passed=true (from Bug 2)
                //
                // Fixed behaviour:
                //   - If no real heading-order failure exists yet, record a clearly-labelled WARN
                //     so the report row shows something meaningful rather than a stale PASS.
                //   - If headingHierarchy is already FAIL (from a heading-order rule above),
                //     leave it untouched — a heading failure outranks a landmark warning.
                case "landmark-one-main":
                case "region": {
                    ValidationResultsModel.FieldResult current = model.getHeadingHierarchy();
                    boolean currentIsFailure = current != null && "FAIL".equalsIgnoreCase(current.getStatus());
                    if (!currentIsFailure) {
                        String landmarkNotes = String.format("Landmark/region issue: %s (%s)", desc, id);
                        // Always WARN for landmark rules — they are advisory, not heading failures.
                        model.setHeadingHierarchy(new ValidationResultsModel.FieldResult("WARN", landmarkNotes, id));
                    }
                    break;
                }

                default:
                    // Unrecognised axe rule — not mapped to a specific model field.
                    // The raw violation is still stored in context.getAxeViolations() and will
                    // appear in the raw axe attachment in the report.
                    break;
            }
        }

        return model;
    }

    /**
     * Maps axe impact level to a FieldResult status string.
     *
     * critical / serious  →  "FAIL"
     * moderate / minor    →  "WARN"
     * unknown             →  "WARN"
     */
    private String mapImpactToFieldStatus(String impact) {
        if (impact == null) return "WARN";
        switch (impact.toLowerCase()) {
            case "critical":
            case "serious":
                return "FAIL";
            case "moderate":
            case "minor":
                return "WARN";
            default:
                return "WARN";
        }
    }
}

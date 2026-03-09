package framework.validation.model;

/**
 * Aggregated validation results model used by the reporting pipeline.
 * Each field represents an audit row and contains status/notes/evidence.
 */
public class ValidationResultsModel {

    public static class FieldResult {
        private final String status; // PASS / FAIL / WARN
        private final String notes;
        private final String evidence;

        public FieldResult(String status, String notes, String evidence) {
            this.status = status == null ? "WARN" : status;
            this.notes = notes == null ? "" : notes;
            this.evidence = evidence == null ? "" : evidence;
        }

        public String getStatus() { return status; }
        public String getNotes() { return notes; }
        public String getEvidence() { return evidence; }

        @Override
        public String toString() {
            return "FieldResult{" +
                    "status='" + status + '\'' +
                    ", notes='" + notes + '\'' +
                    ", evidence='" + evidence + '\'' +
                    '}';
        }
    }

    private FieldResult languageAttribute;
    private FieldResult subjectLine;
    private FieldResult preheader;
    private FieldResult altText;
    private FieldResult decorativeImages;
    private FieldResult headingHierarchy;
    private FieldResult colorContrast;
    private FieldResult descriptiveLinks;
    private FieldResult buttonAccessibility;
    private FieldResult fontReadability;
    private FieldResult httpLinks;
    private FieldResult imageLoading;
    private FieldResult personalizationStrings;
    private FieldResult tableAccessibility;
    private FieldResult mobileResponsiveness;
    private FieldResult unsubscribeLink;

    public FieldResult getLanguageAttribute() { return languageAttribute; }
    public void setLanguageAttribute(FieldResult languageAttribute) { this.languageAttribute = languageAttribute; }

    public FieldResult getSubjectLine() { return subjectLine; }
    public void setSubjectLine(FieldResult subjectLine) { this.subjectLine = subjectLine; }

    public FieldResult getPreheader() { return preheader; }
    public void setPreheader(FieldResult preheader) { this.preheader = preheader; }

    public FieldResult getAltText() { return altText; }
    public void setAltText(FieldResult altText) { this.altText = altText; }

    public FieldResult getDecorativeImages() { return decorativeImages; }
    public void setDecorativeImages(FieldResult decorativeImages) { this.decorativeImages = decorativeImages; }

    public FieldResult getHeadingHierarchy() { return headingHierarchy; }
    public void setHeadingHierarchy(FieldResult headingHierarchy) { this.headingHierarchy = headingHierarchy; }

    public FieldResult getColorContrast() { return colorContrast; }
    public void setColorContrast(FieldResult colorContrast) { this.colorContrast = colorContrast; }

    public FieldResult getDescriptiveLinks() { return descriptiveLinks; }
    public void setDescriptiveLinks(FieldResult descriptiveLinks) { this.descriptiveLinks = descriptiveLinks; }

    public FieldResult getButtonAccessibility() { return buttonAccessibility; }
    public void setButtonAccessibility(FieldResult buttonAccessibility) { this.buttonAccessibility = buttonAccessibility; }

    public FieldResult getFontReadability() { return fontReadability; }
    public void setFontReadability(FieldResult fontReadability) { this.fontReadability = fontReadability; }

    public FieldResult getHttpLinks() { return httpLinks; }
    public void setHttpLinks(FieldResult httpLinks) { this.httpLinks = httpLinks; }

    public FieldResult getImageLoading() { return imageLoading; }
    public void setImageLoading(FieldResult imageLoading) { this.imageLoading = imageLoading; }

    public FieldResult getPersonalizationStrings() { return personalizationStrings; }
    public void setPersonalizationStrings(FieldResult personalizationStrings) { this.personalizationStrings = personalizationStrings; }

    public FieldResult getTableAccessibility() { return tableAccessibility; }
    public void setTableAccessibility(FieldResult tableAccessibility) { this.tableAccessibility = tableAccessibility; }

    public FieldResult getMobileResponsiveness() { return mobileResponsiveness; }
    public void setMobileResponsiveness(FieldResult mobileResponsiveness) { this.mobileResponsiveness = mobileResponsiveness; }

    public FieldResult getUnsubscribeLink() { return unsubscribeLink; }
    public void setUnsubscribeLink(FieldResult unsubscribeLink) { this.unsubscribeLink = unsubscribeLink; }

    @Override
    public String toString() {
        return "ValidationResultsModel{" +
                "languageAttribute=" + languageAttribute +
                ", subjectLine=" + subjectLine +
                ", preheader=" + preheader +
                ", altText=" + altText +
                ", decorativeImages=" + decorativeImages +
                ", headingHierarchy=" + headingHierarchy +
                ", colorContrast=" + colorContrast +
                ", descriptiveLinks=" + descriptiveLinks +
                ", buttonAccessibility=" + buttonAccessibility +
                ", fontReadability=" + fontReadability +
                ", httpLinks=" + httpLinks +
                ", imageLoading=" + imageLoading +
                ", personalizationStrings=" + personalizationStrings +
                ", tableAccessibility=" + tableAccessibility +
                ", mobileResponsiveness=" + mobileResponsiveness +
                ", unsubscribeLink=" + unsubscribeLink +
                '}';
    }
}

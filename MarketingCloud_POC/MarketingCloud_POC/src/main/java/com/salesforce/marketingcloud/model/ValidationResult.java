package com.salesforce.marketingcloud.model;

public class ValidationResult {
    private final String item;
    private final boolean passed;
    private final String notes;
    private final String severity;

    public ValidationResult(String item, boolean passed, String notes, String severity) {
        this.item = item;
        this.passed = passed;
        this.notes = notes == null ? "" : notes;
        this.severity = severity == null ? "INFO" : severity;
    }

    public String getItem() {
        return item;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getNotes() {
        return notes;
    }

    public String getSeverity() {
        return severity;
    }

    public String getStatus() {
        return passed ? "PASS" : "FAIL";
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "item='" + item + '\'' +
                ", passed=" + passed +
                ", severity='" + severity + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}

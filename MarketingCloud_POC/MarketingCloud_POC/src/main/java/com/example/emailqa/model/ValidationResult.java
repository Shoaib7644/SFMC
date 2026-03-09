package com.example.emailqa.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private final String testName;
    private final boolean passed;
    private final String severity;
    private final String notes;
    private final List<String> issues;
    private final List<String> affectedElements;

    private ValidationResult(Builder builder) {
        this.testName = builder.testName;
        this.passed = builder.passed;
        this.severity = builder.severity;
        this.notes = builder.notes;
        this.issues = builder.issues == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.issues));
        this.affectedElements = builder.affectedElements == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.affectedElements));
    }

    public String getTestName() {
        return testName;
    }

    // Compatibility alias for earlier code that used `item` as the field name
    public String getItem() {
        return testName;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getSeverity() {
        return severity;
    }

    public String getNotes() {
        return notes;
    }

    public List<String> getIssues() {
        return issues;
    }

    public List<String> getAffectedElements() {
        return affectedElements;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "testName='" + testName + '\'' +
                ", passed=" + passed +
                ", severity='" + severity + '\'' +
                ", notes='" + notes + '\'' +
                ", issues=" + issues +
                ", affectedElements=" + affectedElements +
                '}';
    }

    public static Builder builder(String testName) {
        return new Builder(testName);
    }

    /**
     * Convert from framework.validation.model.ValidationResult into this legacy model.
     * This keeps adapters simple and preserves notes as an issues entry when present.
     */
    public static ValidationResult fromFramework(framework.validation.model.ValidationResult fr) {
        if (fr == null) return null;
        List<String> issues = new ArrayList<>();
        if (!fr.isPassed() && fr.getNotes() != null && !fr.getNotes().isEmpty()) {
            issues.add(fr.getNotes());
        }
        return ValidationResult.builder(fr.getItem())
                .passed(fr.isPassed())
                .severity(fr.getSeverity())
                .notes(fr.getNotes())
                .issues(issues)
                .build();
    }

    public static class Builder {
        private final String testName;
        private boolean passed = true;
        private String severity = "INFO";
        private String notes = "";
        private List<String> issues;
        private List<String> affectedElements;

        public Builder(String testName) {
            this.testName = testName;
        }

        public Builder passed(boolean passed) {
            this.passed = passed;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder issues(List<String> issues) {
            this.issues = issues;
            return this;
        }

        public Builder affectedElements(List<String> affectedElements) {
            this.affectedElements = affectedElements;
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }
}
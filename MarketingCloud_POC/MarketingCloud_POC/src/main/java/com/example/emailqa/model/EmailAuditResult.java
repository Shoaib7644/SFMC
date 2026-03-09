package com.example.emailqa.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EmailAuditResult {
    private String emailSubject;
    private String emailPreheader;
    private final List<ValidationResult> validations = new ArrayList<>();

    public EmailAuditResult() {
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailPreheader() {
        return emailPreheader;
    }

    public void setEmailPreheader(String emailPreheader) {
        this.emailPreheader = emailPreheader;
    }

    public void addValidationResult(ValidationResult result) {
        if (result != null) {
            validations.add(result);
        }
    }

    public List<ValidationResult> getValidations() {
        return Collections.unmodifiableList(validations);
    }

    public long getPassedCount() {
        return validations.stream().filter(ValidationResult::isPassed).count();
    }

    public long getFailedCount() {
        return validations.stream().filter(v -> !v.isPassed()).count();
    }

    public String generateSummary() {
        int total = validations.size();
        long passed = getPassedCount();
        long failed = getFailedCount();
        List<String> failedNames = validations.stream().filter(v -> !v.isPassed()).map(ValidationResult::getTestName).collect(Collectors.toList());
        return String.format("Total: %d, Passed: %d, Failed: %d, FailedChecks: %s", total, passed, failed, failedNames);
    }
}

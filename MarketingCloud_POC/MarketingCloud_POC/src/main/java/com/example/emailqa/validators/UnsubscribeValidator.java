package com.example.emailqa.validators;

import com.example.emailqa.model.ValidationResult;
import framework.validation.context.ValidationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that delegates to framework UnsubscribeValidator and maps the result
 * to the legacy com.example.emailqa.model.ValidationResult type.
 */
public class UnsubscribeValidator implements EmailValidator {
    private final framework.validation.validators.UnsubscribeValidator delegate;

    public UnsubscribeValidator() {
        this.delegate = new framework.validation.validators.UnsubscribeValidator(ValidationContext.getInstance());
    }

    @Override
    public ValidationResult validate(String emailHtml) {
        framework.validation.model.ValidationResult r = delegate.validate(emailHtml);
        List<String> issues = new ArrayList<>();
        List<String> affected = new ArrayList<>();
        // framework.ValidationResult stores notes and severity; legacy model has lists too but they're optional
        if (!r.isPassed()) {
            issues.add(r.getNotes());
        }
        // Build legacy ValidationResult
        ValidationResult res = ValidationResult.builder("Unsubscribe Link")
                .passed(r.isPassed())
                .severity(r.getSeverity())
                .notes(r.getNotes())
                .issues(issues)
                .affectedElements(affected)
                .build();
        return res;
    }
}
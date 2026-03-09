package com.example.emailqa;

import com.example.emailqa.model.EmailAuditResult;
import com.example.emailqa.model.ValidationResult;
import com.example.emailqa.validators.*;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EmailAuditRunner {
    private static final Logger logger = LoggerFactory.getLogger(EmailAuditRunner.class);

    private final List<com.example.emailqa.validators.EmailValidator> validators = new ArrayList<>();

    public EmailAuditRunner() {
        // default set
        validators.add(new SubjectPreheaderValidator());
        validators.add(new HeadingHierarchyValidator());
        validators.add(new BrokenLinkValidator());
        // Inline adapter for unsubscribe that delegates to framework UnsubscribeValidator
        validators.add(new com.example.emailqa.validators.EmailValidator() {
            private final framework.validation.validators.UnsubscribeValidator delegate = new framework.validation.validators.UnsubscribeValidator(framework.validation.context.ValidationContext.getInstance());
            @Override
            public ValidationResult validate(String emailHtml) {
                framework.validation.model.ValidationResult r = delegate.validate(emailHtml);
                List<String> issues = new ArrayList<>();
                List<String> affected = new ArrayList<>();
                if (!r.isPassed()) issues.add(r.getNotes());
                return ValidationResult.builder("Unsubscribe Link")
                        .passed(r.isPassed())
                        .severity(r.getSeverity())
                        .notes(r.getNotes())
                        .issues(issues)
                        .affectedElements(affected)
                        .build();
            }
        });
        validators.add(new PersonalizationValidator());
        validators.add(new MobileResponsivenessValidator());
        validators.add(new AccessibilityValidator());
    }

    public EmailAuditRunner(List<com.example.emailqa.validators.EmailValidator> customValidators) {
        if (customValidators != null && !customValidators.isEmpty()) {
            validators.addAll(customValidators);
        } else {
            // fallback to defaults
            validators.add(new SubjectPreheaderValidator());
            validators.add(new HeadingHierarchyValidator());
            validators.add(new BrokenLinkValidator());
            // Inline adapter for unsubscribe
            validators.add(new com.example.emailqa.validators.EmailValidator() {
                private final framework.validation.validators.UnsubscribeValidator delegate = new framework.validation.validators.UnsubscribeValidator(framework.validation.context.ValidationContext.getInstance());
                @Override
                public ValidationResult validate(String emailHtml) {
                    framework.validation.model.ValidationResult r = delegate.validate(emailHtml);
                    List<String> issues = new ArrayList<>();
                    List<String> affected = new ArrayList<>();
                    if (!r.isPassed()) issues.add(r.getNotes());
                    return ValidationResult.builder("Unsubscribe Link")
                            .passed(r.isPassed())
                            .severity(r.getSeverity())
                            .notes(r.getNotes())
                            .issues(issues)
                            .affectedElements(affected)
                            .build();
                }
            });
            validators.add(new PersonalizationValidator());
            validators.add(new MobileResponsivenessValidator());
            validators.add(new AccessibilityValidator());
        }
    }

    public EmailAuditResult run(String emailHtml, String subject, String preheader) {
        logger.info("Starting email audit run for subject='{}'", subject == null ? "(empty)" : subject);
        EmailAuditResult auditResult = new EmailAuditResult();
        auditResult.setEmailSubject(subject);
        auditResult.setEmailPreheader(preheader);

        for (com.example.emailqa.validators.EmailValidator v : validators) {
            String validatorName = v.getClass().getSimpleName();
            try {
                logger.info("Running validator: {}", validatorName);
                ValidationResult r = v.validate(emailHtml);

                // Attach individual validator output to Allure as plain text
                String attachName = String.format("Validator - %s", r.getTestName());
                String details = String.format("Result: %s\nSeverity: %s\nNotes: %s\nIssues: %s\nAffected: %s",
                        r.isPassed() ? "PASS" : "FAIL",
                        r.getSeverity(),
                        r.getNotes(),
                        r.getIssues(),
                        r.getAffectedElements());
                Allure.addAttachment(attachName, new ByteArrayInputStream(details.getBytes(StandardCharsets.UTF_8)));

                auditResult.addValidationResult(r);
                logger.info("Validator {} completed: {}", validatorName, r.isPassed() ? "PASS" : "FAIL");
            } catch (Exception e) {
                // Validators must not fail the audit; log and continue
                logger.error("Validator {} threw exception: {}", validatorName, e.getMessage(), e);
                ValidationResult r = ValidationResult.builder(validatorName)
                        .passed(false)
                        .severity("CRITICAL")
                        .notes("Validator exception: " + e.getMessage())
                        .issues(List.of(e.getMessage()))
                        .build();

                Allure.addAttachment("Validator - Exception: " + validatorName, e.toString());
                auditResult.addValidationResult(r);
            }
        }

        logger.info("Completed email audit run: {}", auditResult.generateSummary());
        // Attach final summary
        Allure.addAttachment("Email Audit Summary", auditResult.generateSummary());
        return auditResult;
    }
}
package com.example.emailqa.validators;

import com.example.emailqa.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonalizationValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(PersonalizationValidator.class);

    private static final Pattern SFMC_TOKEN_PATTERN = Pattern.compile("%%[^%]+%%");
    private static final Pattern SFMC_V_FUNCTION = Pattern.compile("%%=v\\([^%]+\\)%%");

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Starting Personalization validation");
        List<String> issues = new ArrayList<>();
        List<String> affected = new ArrayList<>();

        if (emailHtml != null) {
            Matcher m = SFMC_TOKEN_PATTERN.matcher(emailHtml);
            while (m.find()) {
                String token = m.group();
                issues.add("Unresolved token: " + token);
                affected.add(token);
            }

            Matcher m2 = SFMC_V_FUNCTION.matcher(emailHtml);
            while (m2.find()) {
                String token = m2.group();
                issues.add("Unresolved v() token: " + token);
                affected.add(token);
            }
        }

        boolean passed = issues.isEmpty();
        String notes = passed ? "No unresolved personalization tokens found" : String.join("; ", issues);
        ValidationResult result = ValidationResult.builder("SFMC Personalization")
                .passed(passed)
                .severity(passed ? "INFO" : "MAJOR")
                .notes(notes)
                .issues(issues)
                .affectedElements(affected)
                .build();
        logger.info("Completed Personalization validation: {}", result.isPassed() ? "PASS" : "FAIL");
        return result;
    }
}

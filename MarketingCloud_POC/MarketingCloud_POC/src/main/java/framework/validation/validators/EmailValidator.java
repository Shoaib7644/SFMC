package framework.validation.validators;

import framework.validation.model.ValidationResult;

public interface EmailValidator {
    ValidationResult validate(String emailHtml);
}

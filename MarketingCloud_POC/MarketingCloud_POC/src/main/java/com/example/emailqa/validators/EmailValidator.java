package com.example.emailqa.validators;

import com.example.emailqa.model.ValidationResult;

public interface EmailValidator {
    ValidationResult validate(String emailHtml);
}

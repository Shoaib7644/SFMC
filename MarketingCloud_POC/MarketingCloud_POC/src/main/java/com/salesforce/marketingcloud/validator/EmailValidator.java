package com.salesforce.marketingcloud.validator;

import com.salesforce.marketingcloud.model.ValidationResult;

public interface EmailValidator {
    ValidationResult validate(String emailHtml);
}

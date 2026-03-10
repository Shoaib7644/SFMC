package com.salesforce.marketingcloud.services;


import java.util.ArrayList;
import java.util.List;

import com.salesforce.marketingcloud.context.ValidationContext;
import com.salesforce.marketingcloud.model.ValidationResult;
import com.salesforce.marketingcloud.validator.AccessibilityValidator;
import com.salesforce.marketingcloud.validator.BrokenLinkValidator;
import com.salesforce.marketingcloud.validator.EmailMetadataValidator;
import com.salesforce.marketingcloud.validator.EmailValidator;
import com.salesforce.marketingcloud.validator.HeadingHierarchyValidator;
import com.salesforce.marketingcloud.validator.ImageValidator;
import com.salesforce.marketingcloud.validator.MobileResponsivenessValidator;
import com.salesforce.marketingcloud.validator.PersonalizationValidator;
import com.salesforce.marketingcloud.validator.UnsubscribeValidator;

public class ValidationService {
    private final ValidationContext context;

    public ValidationService(ValidationContext context) {
        this.context = context;
    }

    public List<ValidationResult> runAll(String emailHtml, Object mailosaurMessage) {
        List<ValidationResult> results = new ArrayList<>();

        // validators
        EmailValidator accessibility = new AccessibilityValidator(context);
        EmailValidator brokenLinks = new BrokenLinkValidator(context);
        EmailValidator images = new ImageValidator(context);
        EmailValidator metadata = new EmailMetadataValidator(context, (com.mailosaur.models.Message) mailosaurMessage);
        EmailValidator heading = new HeadingHierarchyValidator(context);
        EmailValidator unsubscribe = new UnsubscribeValidator(context);
        EmailValidator personalization = new PersonalizationValidator(context);
        EmailValidator mobile = new MobileResponsivenessValidator(context);

        results.add(accessibility.validate(emailHtml));
        results.add(brokenLinks.validate(emailHtml));
        results.add(images.validate(emailHtml));
        results.add(metadata.validate(emailHtml));
        results.add(heading.validate(emailHtml));
        results.add(unsubscribe.validate(emailHtml));
        results.add(personalization.validate(emailHtml));
        results.add(mobile.validate(emailHtml));

        return results;
    }
}

package framework.validation.services;

import framework.validation.context.ValidationContext;
import framework.validation.validators.*;
import framework.validation.model.ValidationResult;

import java.util.ArrayList;
import java.util.List;

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

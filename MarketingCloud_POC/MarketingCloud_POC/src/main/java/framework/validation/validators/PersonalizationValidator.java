package framework.validation.validators;

import framework.validation.context.ValidationContext;
import framework.validation.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonalizationValidator implements EmailValidator {
    private static final Logger logger = LoggerFactory.getLogger(PersonalizationValidator.class);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("%%.*?%%");
    private final ValidationContext context;

    public PersonalizationValidator(ValidationContext context) {
        this.context = context;
    }

    @Override
    public ValidationResult validate(String emailHtml) {
        logger.info("Running PersonalizationValidator");
        String body = emailHtml == null ? "" : emailHtml;
        Matcher m = TOKEN_PATTERN.matcher(body);
        boolean found = false;
        StringBuilder notes = new StringBuilder();
        while (m.find()) {
            String token = m.group();
            found = true;
            notes.append(token).append(";");
        }
        boolean passed = !found;
        String noteText = passed ? "No unresolved SFMC tokens detected" : "Unresolved tokens detected: " + notes.toString();
        context.addResult("SFMC Personalization Strings", passed, noteText, passed ? "INFO" : "MAJOR");
        return new ValidationResult("SFMC Personalization Strings", passed, noteText, passed ? "INFO" : "MAJOR");
    }
}
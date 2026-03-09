package framework.validation.validators;

import com.mailosaur.models.Message;
import framework.validation.model.ValidationResultsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates email headers for unsubscribe related headers.
 */
public class EmailHeaderValidator {
    private static final Logger logger = LoggerFactory.getLogger(EmailHeaderValidator.class);

    public ValidationResultsModel.FieldResult validate(Message message) {
        try {
            if (message == null) {
                return new ValidationResultsModel.FieldResult("WARN", "No message object available to inspect headers", "");
            }
            // Mailosaur Message implementations vary; fall back to string inspection for header tokens
            try {
                String headersStr = message.toString();
                if (headersStr == null) return new ValidationResultsModel.FieldResult("WARN", "No headers available", "");
                headersStr = headersStr.toLowerCase();
                if (headersStr.contains("list-unsubscribe") || headersStr.contains("list-unsubscribe-post")) {
                    return new ValidationResultsModel.FieldResult("PASS", "List-Unsubscribe header present", "list-unsubscribe");
                } else {
                    return new ValidationResultsModel.FieldResult("FAIL", "No List-Unsubscribe header present", "");
                }
            } catch (Exception e) {
                logger.warn("Error reading message headers: {}", e.getMessage());
                return new ValidationResultsModel.FieldResult("WARN", "Error reading headers: " + e.getMessage(), "");
            }
        } catch (Exception e) {
            logger.warn("EmailHeaderValidator failure: {}", e.getMessage());
            return new ValidationResultsModel.FieldResult("WARN", "Header validation failed: " + e.getMessage(), "");
        }
    }
}
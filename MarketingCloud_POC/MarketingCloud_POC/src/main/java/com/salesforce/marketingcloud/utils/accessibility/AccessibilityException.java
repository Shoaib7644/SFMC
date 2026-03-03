package com.salesforce.marketingcloud.utils.accessibility;

/**
 * Checked exception for accessibility check failures and utility errors.
 */
public class AccessibilityException extends Exception {
    public AccessibilityException(String message) {
        super(message);
    }

    public AccessibilityException(String message, Throwable cause) {
        super(message, cause);
    }
}

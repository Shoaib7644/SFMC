package com.salesforce.marketingcloud.context;

// no import aliasing - reference framework context by fully qualified name

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Backwards-compatible test ValidationContext shim.
 * Preserves the existing static API used by step classes while delegating
 * to the framework ValidationContext singleton so reporting remains deterministic.
 */
public final class ValidationContext {

    private ValidationContext() {}

    private static final ThreadLocal<List<String>> validationErrors = ThreadLocal.withInitial(ArrayList::new);

    public static void addError(String message) {
        if (message == null) return;
        // store in thread-local list (backwards compat)
        List<String> list = validationErrors.get();
        list.add(message);
        // also register as a framework validation result so it shows up in deterministic report mapping
        try {
            framework.validation.context.ValidationContext.getInstance().addResult("Runtime Error", false, message, "CRITICAL");
        } catch (Throwable t) {
            // swallow to avoid cascading failures in test hooks
        }
    }

    public static List<String> getErrors() {
        List<String> list = validationErrors.get();
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    public static boolean hasErrors() {
        List<String> list = validationErrors.get();
        return list != null && !list.isEmpty();
    }

    public static void clear() {
        List<String> list = validationErrors.get();
        list.clear();
        // clear framework validation context as well to avoid cross-scenario leakage
        try {
            framework.validation.context.ValidationContext.getInstance().clear();
        } catch (Throwable t) {
            // ignore
        }
    }

    public static String combinedMessage() {
        List<String> list = validationErrors.get();
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(i + 1).append(") ").append(list.get(i));
            if (i < list.size() - 1) sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
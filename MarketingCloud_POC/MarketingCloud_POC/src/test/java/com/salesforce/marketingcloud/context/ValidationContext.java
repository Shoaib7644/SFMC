package com.salesforce.marketingcloud.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Thread-safe validation context for accumulating soft assertion failures per scenario/thread.
 */
public final class ValidationContext {

    private ValidationContext() {}

    private static final ThreadLocal<List<String>> validationErrors = ThreadLocal.withInitial(ArrayList::new);

    public static void addError(String message) {
        if (message == null) return;
        List<String> list = validationErrors.get();
        list.add(message);
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

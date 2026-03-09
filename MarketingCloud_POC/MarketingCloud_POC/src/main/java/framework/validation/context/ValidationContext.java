package framework.validation.context;

import framework.validation.model.ValidationResult;
import framework.validation.model.ValidationResultsModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic validation context using LinkedHashMap to preserve insertion order.
 * Exposes a singleton instance for cross-step sharing.
 */
public class ValidationContext {
    // singleton instance to be shared across step classes in the same JVM/thread
    private static final ValidationContext INSTANCE = new ValidationContext();

    public static ValidationContext getInstance() {
        return INSTANCE;
    }

    // preserve insertion order and deterministic iteration
    private final Map<String, ValidationResult> results = new LinkedHashMap<>();
    private final List<String> brokenLinks = new ArrayList<>();
    private final List<String> brokenImages = new ArrayList<>();
    private List<Object> axeViolations = new ArrayList<>();
    private final Map<String, Integer> axeSummary = new LinkedHashMap<>();
    // stores total affected element/node counts per impact (informational)
    private final Map<String, Integer> axeNodeCounts = new LinkedHashMap<>();

    // aggregated model for report consumption
    private ValidationResultsModel aggregatedModel = new ValidationResultsModel();

    // keep default constructor private to encourage use of getInstance()
    private ValidationContext() {}

    /**
     * Add or replace a validation result. Use this everywhere to ensure deterministic mapping.
     */
    public synchronized void addResult(String validationName, boolean passed, String notes, String severity) {
        if (validationName == null) validationName = "Unknown";
        ValidationResult vr = new ValidationResult(validationName, passed, notes, severity);
        results.put(validationName, vr);
    }

    public synchronized List<ValidationResult> getResults() {
        return Collections.unmodifiableList(new ArrayList<>(results.values()));
    }

    public synchronized ValidationResult getResultByName(String name) {
        return results.get(name);
    }

    public synchronized void addBrokenLink(String url) {
        brokenLinks.add(url);
    }

    public synchronized List<String> getBrokenLinks() {
        return Collections.unmodifiableList(brokenLinks);
    }

    public synchronized void addBrokenImage(String src) {
        brokenImages.add(src);
    }

    public synchronized List<String> getBrokenImages() {
        return Collections.unmodifiableList(brokenImages);
    }

    public synchronized void setAxeViolations(List<Object> violations) {
        this.axeViolations = violations == null ? new ArrayList<>() : new ArrayList<>(violations);
    }

    public synchronized List<Object> getAxeViolations() {
        return Collections.unmodifiableList(axeViolations);
    }

    public synchronized void setAxeSummary(Map<String, Integer> summary) {
        this.axeSummary.clear();
        if (summary != null) this.axeSummary.putAll(summary);
    }

    public synchronized Map<String, Integer> getAxeSummary() {
        return Collections.unmodifiableMap(axeSummary);
    }

    /**
     * Store total affected element/node counts per impact returned from axe.
     * This mirrors setAxeSummary/getAxeSummary but keeps node counts (informational)
     * separate from the rule-count summary.
     */
    public synchronized void setAxeNodeCounts(Map<String, Integer> nodeCounts) {
        this.axeNodeCounts.clear();
        if (nodeCounts != null) this.axeNodeCounts.putAll(nodeCounts);
    }

    public synchronized Map<String, Integer> getAxeNodeCounts() {
        return Collections.unmodifiableMap(axeNodeCounts);
    }

    public synchronized void clear() {
        results.clear();
        brokenLinks.clear();
        brokenImages.clear();
        axeViolations.clear();
        axeSummary.clear();
        axeNodeCounts.clear();
        aggregatedModel = new ValidationResultsModel();
    }

    public synchronized ValidationResultsModel getAggregatedModel() {
        return aggregatedModel;
    }

    public synchronized void setAggregatedModel(ValidationResultsModel model) {
        this.aggregatedModel = model == null ? new ValidationResultsModel() : model;
    }
}
package com.salesforce.marketingcloud.model.accessibility;

public class AccessibilitySummary {

    private int totalViolations;
    private int criticalCount;
    private int seriousCount;
    private int moderateCount;
    private int minorCount;
    private String rawJson;
    private AxeResult axeResult;

    // --- Constructors ---

    public AccessibilitySummary() {
    }

    public AccessibilitySummary(int totalViolations, int criticalCount, int seriousCount, 
                                int moderateCount, int minorCount, String rawJson, AxeResult axeResult) {
        this.totalViolations = totalViolations;
        this.criticalCount = criticalCount;
        this.seriousCount = seriousCount;
        this.moderateCount = moderateCount;
        this.minorCount = minorCount;
        this.rawJson = rawJson;
        this.axeResult = axeResult;
    }

    // --- Getters and Setters ---

    public int getTotalViolations() {
        return totalViolations;
    }

    public void setTotalViolations(int totalViolations) {
        this.totalViolations = totalViolations;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(int criticalCount) {
        this.criticalCount = criticalCount;
    }

    public int getSeriousCount() {
        return seriousCount;
    }

    public void setSeriousCount(int seriousCount) {
        this.seriousCount = seriousCount;
    }

    public int getModerateCount() {
        return moderateCount;
    }

    public void setModerateCount(int moderateCount) {
        this.moderateCount = moderateCount;
    }

    public int getMinorCount() {
        return minorCount;
    }

    public void setMinorCount(int minorCount) {
        this.minorCount = minorCount;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public AxeResult getAxeResult() {
        return axeResult;
    }

    public void setAxeResult(AxeResult axeResult) {
        this.axeResult = axeResult;
    }

    // --- Logic Methods ---

    public int getBlockingCount() {
        return criticalCount + seriousCount;
    }

    /**
     * Backward compatibility method (used by older step definitions)
     */
    public String getViolationsJson() {
        return rawJson;
    }
}
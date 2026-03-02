package com.salesforce.marketingcloud.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregated validation result for an email.
 */
public final class EmailValidationResult {

    private final String emailSubject;
    private final List<LinkResult> linkResults;

    public EmailValidationResult(String emailSubject, List<LinkResult> linkResults) {
        this.emailSubject = emailSubject;
        this.linkResults = linkResults == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(linkResults));
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public int getTotalLinks() {
        return linkResults.size();
    }

    public int getBrokenLinks() {
        return (int) linkResults.stream().filter(LinkResult::isBroken).count();
    }

    public List<LinkResult> getLinkResults() {
        return linkResults;
    }

    public List<LinkResult> getOnlyBrokenLinks() {
        return linkResults.stream().filter(LinkResult::isBroken).collect(Collectors.toUnmodifiableList());
    }

    public String generateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Email Subject: ").append(emailSubject).append("\n");
        sb.append("Total Links: ").append(getTotalLinks()).append("\n");
        sb.append("Broken Links: ").append(getBrokenLinks()).append("\n");
        if (getBrokenLinks() > 0) {
            sb.append("Broken link details:\n");
            for (LinkResult lr : getOnlyBrokenLinks()) {
                sb.append(lr.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EmailValidationResult{" +
                "emailSubject='" + emailSubject + '\'' +
                ", totalLinks=" + getTotalLinks() +
                ", brokenLinks=" + getBrokenLinks() +
                ", linkResults=" + linkResults +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailValidationResult)) return false;
        EmailValidationResult that = (EmailValidationResult) o;
        return Objects.equals(emailSubject, that.emailSubject) && Objects.equals(linkResults, that.linkResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailSubject, linkResults);
    }
}

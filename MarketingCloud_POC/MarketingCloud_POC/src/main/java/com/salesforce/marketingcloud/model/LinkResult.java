package com.salesforce.marketingcloud.model;

import java.util.Objects;

/**
 * Represents the result of validating a single link.
 */
public final class LinkResult {
    private final String url;
    private final int statusCode;
    private final String errorMessage;
    private final boolean broken;

    public LinkResult(String url, int statusCode, String errorMessage, boolean broken) {
        this.url = url;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.broken = broken;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isBroken() {
        return broken;
    }

    @Override
    public String toString() {
        return "LinkResult{" +
                "url='" + url + '\'' +
                ", statusCode=" + statusCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", broken=" + broken +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LinkResult)) return false;
        LinkResult that = (LinkResult) o;
        return statusCode == that.statusCode && broken == that.broken && Objects.equals(url, that.url) && Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, statusCode, errorMessage, broken);
    }
}

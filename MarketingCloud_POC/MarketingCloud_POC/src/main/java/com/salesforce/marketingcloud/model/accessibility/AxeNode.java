package com.salesforce.marketingcloud.model.accessibility;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single node entry from an axe-core violation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AxeNode {

    /**
     * The list of selectors that identify the target element(s) for this node.
     */
    @SerializedName("target")
    private List<String> target;

    /**
     * The outer HTML of the node as returned by axe.
     */
    @SerializedName("html")
    private String html;

    /**
     * A human-friendly summary of the failing assertions for this node.
     */
    @SerializedName("failureSummary")
    private String failureSummary;

    // explicit getters
    public List<String> getTarget() { return target; }
    public String getHtml() { return html; }
    public String getFailureSummary() { return failureSummary; }
}
package com.salesforce.marketingcloud.model.accessibility;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single axe-core violation entry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AxeViolation {

    @SerializedName("id")
    private String id;

    @SerializedName("impact")
    private String impact;

    @SerializedName("description")
    private String description;

    @SerializedName("help")
    private String help;

    @SerializedName("helpUrl")
    private String helpUrl;

    @SerializedName("nodes")
    private List<AxeNode> nodes;

    // explicit getters
    public String getId() { return id; }
    public String getImpact() { return impact; }
    public String getDescription() { return description; }
    public String getHelp() { return help; }
    public String getHelpUrl() { return helpUrl; }
    public List<AxeNode> getNodes() { return nodes; }
}
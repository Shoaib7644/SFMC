package com.salesforce.marketingcloud.model.accessibility;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level model for axe-core result JSON.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AxeResult {

    @SerializedName("violations")
    private List<AxeViolation> violations;

    // Other axe result fields (incomplete) can be added as needed, such as 'passes', 'inapplicable', 'toolOptions'

    // explicit getter to ensure compatibility
    public List<AxeViolation> getViolations() {
        return this.violations;
    }
}
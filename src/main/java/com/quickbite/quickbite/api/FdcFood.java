package com.quickbite.quickbite.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** One food matched by a USDA FoodData Central search. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FdcFood {

    @JsonProperty("description")
    private String description;

    @JsonProperty("foodNutrients")
    private List<FdcNutrient> foodNutrients;

    public String getDescription() { return description; }
    public List<FdcNutrient> getFoodNutrients() { return foodNutrients; }
}
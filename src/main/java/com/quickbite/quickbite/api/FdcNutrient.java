package com.quickbite.quickbite.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One nutrient line of a USDA FoodData Central search result (e.g. Protein, 11.4, G). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FdcNutrient {

    @JsonProperty("nutrientNumber")
    private String number;

    @JsonProperty("nutrientName")
    private String name;

    @JsonProperty("unitName")
    private String unit;

    @JsonProperty("value")
    private double value;

    public String getNumber() { return number; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public double getValue() { return value; }
}
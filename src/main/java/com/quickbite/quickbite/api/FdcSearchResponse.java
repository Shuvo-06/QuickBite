package com.quickbite.quickbite.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Top-level shape of a /foods/search response: {"foods": [ ... ], ...}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FdcSearchResponse {

    @JsonProperty("foods")
    private List<FdcFood> foods;

    public List<FdcFood> getFoods() { return foods; }
}
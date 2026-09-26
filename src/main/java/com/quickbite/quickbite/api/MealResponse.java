package com.quickbite.quickbite.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Matches the top-level shape of TheMealDB's JSON: {"meals": [ ... ]}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MealResponse {

    private List<MealDto> meals;

    public List<MealDto> getMeals() { return meals; }
    public void setMeals(List<MealDto> meals) { this.meals = meals; }
}

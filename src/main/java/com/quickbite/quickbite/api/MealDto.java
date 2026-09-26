package com.quickbite.quickbite.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One dish as returned by TheMealDB (https://www.themealdb.com), a free public API that needs
 * no API key. Only the fields QuickBite actually uses are declared here; @JsonIgnoreProperties
 * tells Jackson to silently skip everything else in the real response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MealDto {

    @JsonProperty("idMeal")
    private String id;

    @JsonProperty("strMeal")
    private String name;

    @JsonProperty("strCategory")
    private String category;

    @JsonProperty("strArea")
    private String area;

    @JsonProperty("strInstructions")
    private String instructions;

    @JsonProperty("strMealThumb")
    private String imageUrl;

    public MealDto() {
        // Jackson needs a no-argument constructor so it can create the object before filling in fields.
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}

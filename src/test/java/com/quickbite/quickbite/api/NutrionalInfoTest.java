package com.quickbite.quickbite.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NutritionInfoTest {

    // Shaped like a real /foods/search response (numbers are sample values, not real USDA data).
    private static final String SAMPLE_JSON = """
            {"totalHits": 1, "foods": [{
              "fdcId": 1, "description": "Pizza, cheese", "dataType": "SR Legacy",
              "foodNutrients": [
                {"nutrientId": 1008, "nutrientName": "Energy", "nutrientNumber": "208", "unitName": "KCAL", "value": 266},
                {"nutrientId": 1003, "nutrientName": "Protein", "nutrientNumber": "203", "unitName": "G", "value": 11.4},
                {"nutrientId": 1004, "nutrientName": "Total lipid (fat)", "nutrientNumber": "204", "unitName": "G", "value": 9.7},
                {"nutrientId": 1005, "nutrientName": "Carbohydrate, by difference", "nutrientNumber": "205", "unitName": "G", "value": 33},
                {"nutrientId": 1093, "nutrientName": "Sodium, Na", "nutrientNumber": "307", "unitName": "MG", "value": 598}
              ]}]}
            """;

    @Test
    void jacksonParsesUsdaJsonIntoNutritionInfo() throws Exception {
        FdcSearchResponse response = new ObjectMapper().readValue(SAMPLE_JSON, FdcSearchResponse.class);
        NutritionInfo info = NutritionInfo.fromFood(response.getFoods().get(0));

        assertEquals("Pizza, cheese", info.getMatchedName());
        assertEquals(266, info.getCalories(), 0.001);
        assertEquals(11.4, info.getProtein(), 0.001);
        assertEquals(598, info.getSodiumMg(), 0.001);
    }

    @Test
    void missingNutrientsAreReportedAsNotAvailable() throws Exception {
        FdcSearchResponse response = new ObjectMapper().readValue(SAMPLE_JSON, FdcSearchResponse.class);
        NutritionInfo info = NutritionInfo.fromFood(response.getFoods().get(0));

        assertTrue(info.getFiber() < 0);
        assertEquals("not available", NutritionInfo.describe(info.getFiber(), "g"));
    }
}
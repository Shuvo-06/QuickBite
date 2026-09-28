package com.quickbite.quickbite.util;

import com.quickbite.quickbite.util.BulkImportParser.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkImportParserTest {

    @Test
    void parsesRestaurantsWithTheirFoods() {
        Result result = BulkImportParser.parse("""
                # a comment
                RESTAURANT: Kebab Corner | Grilled kebabs | 4.5 | kebab123 | kebab.jpg
                Seekh Kebab | Minced beef skewers | 220
                Chicken Roll | Paratha roll | 150 | roll.png
                RESTAURANT: Pasta Place | Fresh pasta | 4.2 | pasta123
                Penne | Tomato sauce | 260
                """);

        assertTrue(result.errors().isEmpty());
        assertEquals(2, result.restaurants().size());
        assertEquals(2, result.restaurants().get(0).foods.size());
        assertEquals("kebab.jpg", result.restaurants().get(0).image);
        assertEquals(1, result.restaurants().get(1).foods.size());
        assertTrue(result.orphanFoods().isEmpty());
    }

    @Test
    void foodLinesBeforeAnyRestaurantAreOrphans() {
        Result result = BulkImportParser.parse("Samosa | Crispy pastry | 30");
        assertEquals(1, result.orphanFoods().size());
        assertTrue(result.restaurants().isEmpty());
    }

    @Test
    void badLinesAreReportedWithTheirLineNumber() {
        Result result = BulkImportParser.parse("""
                RESTAURANT: Bad Place | desc | 9 | pw1234
                Fine Food | desc | abc
                """);
        assertFalse(result.errors().isEmpty());
        assertTrue(result.errors().get(0).startsWith("Line 1"));
        assertTrue(result.errors().get(1).startsWith("Line 2"));
    }
}
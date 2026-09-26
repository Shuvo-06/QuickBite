package com.quickbite.quickbite.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceFormatterTest {

    @Test
    void formatsWholeNumbersWithoutDecimals() {
        assertEquals("৳250", PriceFormatter.format(250.0));
    }

    @Test
    void formatsFractionalAmountsWithTwoDecimals() {
        assertEquals("৳99.50", PriceFormatter.format(99.5));
    }

    @Test
    void formatsZero() {
        assertEquals("৳0", PriceFormatter.format(0));
    }
}

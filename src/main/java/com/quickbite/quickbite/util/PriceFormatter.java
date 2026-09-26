package com.quickbite.quickbite.util;

/** Formats prices the same way everywhere. To change the currency, edit only this class. */
public class PriceFormatter {

    private static final String CURRENCY_SYMBOL = "৳";

    private PriceFormatter() {
    }

    public static String format(double amount) {
        if (amount == Math.floor(amount)) {
            return CURRENCY_SYMBOL + (long) amount;      // 250.0 -> ৳250
        }
        return CURRENCY_SYMBOL + String.format("%.2f", amount);
    }
}

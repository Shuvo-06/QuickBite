package com.quickbite.quickbite.model;

/**
 * Anything that can reduce an order's subtotal by some amount. Coupon is the only
 * implementation today, but OrderService only ever calls discountFor(...) — it never checks
 * "is this a Coupon?" — so a future discount type (loyalty points, a first-order promo) could
 * implement this interface and plug straight into checkout with no change to OrderService.
 */
public interface Discountable {
    /** Returns the amount to subtract from the given subtotal, or 0 if it doesn't apply right now. */
    double discountFor(double subtotal);
}
package com.quickbite.quickbite.model;

import java.time.LocalDate;

/** A discount coupon created by the admin, valid for a percentage off between two dates. */
public class Coupon implements Discountable {
    private final int id;
    private final String code;
    private final int discountPercent;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final boolean active;

    public Coupon(int id, String code, int discountPercent, LocalDate startDate, LocalDate endDate, boolean active) {
        this.id = id;
        this.code = code;
        this.discountPercent = discountPercent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public int getDiscountPercent() { return discountPercent; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isActive() { return active; }

    /** True when the admin has enabled this coupon AND today falls inside its date range. */
    public boolean isCurrentlyValid() {
        LocalDate today = LocalDate.now();
        return active && !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /** Discountable implementation: how much this coupon takes off a given subtotal right now. */
    @Override
    public double discountFor(double subtotal) {
        return isCurrentlyValid() ? subtotal * discountPercent / 100.0 : 0;
    }
}

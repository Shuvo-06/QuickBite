package com.quickbite.quickbite.service;

import com.quickbite.quickbite.dao.CouponDAO;
import com.quickbite.quickbite.dao.OrderDAO;
import com.quickbite.quickbite.model.Coupon;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.model.OrderStatus;
import com.quickbite.quickbite.model.Restaurant;

import java.sql.SQLException;
import java.util.List;

/** Business logic for orders (kept out of the controllers). */
public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final CouponDAO couponDAO = new CouponDAO();

    /**
     * Creates a new order and saves it (with its items) to the database. Status starts at PLACED.
     * couponCode may be null/blank for no coupon. Only a coupon the admin has enabled AND whose
     * date range covers today is applied; anything else is silently treated as "no coupon" here —
     * the controller is responsible for telling the customer when a code doesn't work.
     */
    public Order createOrder(String customerName, Restaurant restaurant, List<OrderItem> items, String couponCode)
            throws SQLException {
        String appliedCode = null;
        double discountAmount = 0;

        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponDAO.findByCode(couponCode);
            if (coupon != null && coupon.isCurrentlyValid()) {
                double subtotal = 0;
                for (OrderItem item : items) {
                    subtotal += item.getSubtotal();
                }
                discountAmount = subtotal * coupon.getDiscountPercent() / 100.0;
                appliedCode = coupon.getCode();
            }
        }

        return orderDAO.insertOrder(customerName, restaurant, items, appliedCode, discountAmount);
    }

    /** Checks a coupon code without placing an order, so the UI can show the discount before checkout. */
    public Coupon findValidCoupon(String couponCode) throws SQLException {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        Coupon coupon = couponDAO.findByCode(couponCode);
        return (coupon != null && coupon.isCurrentlyValid()) ? coupon : null;
    }

    /**
     * Moves the order to its next status in the normal flow, in memory AND in the database.
     * Called by OrderTrackingService on a background thread once an order is CONFIRMED or later.
     */
    public void advanceStatus(Order order) throws SQLException {
        OrderStatus next = order.getStatus().next();
        if (next != null) {
            order.setStatus(next);
            orderDAO.updateStatus(order.getId(), next);
        }
    }

    /** Called by the restaurant dashboard when the restaurant accepts a PLACED order. */
    public void acceptOrder(Order order) throws SQLException {
        order.setStatus(OrderStatus.CONFIRMED);
        orderDAO.updateStatus(order.getId(), OrderStatus.CONFIRMED);
    }

    /** Called by the restaurant dashboard when the restaurant rejects a PLACED order. */
    public void rejectOrder(Order order) throws SQLException {
        order.setStatus(OrderStatus.REJECTED);
        orderDAO.updateStatus(order.getId(), OrderStatus.REJECTED);
    }
}

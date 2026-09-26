package com.quickbite.quickbite.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemTest {

    @Test
    void subtotalMultipliesPriceByQuantity() {
        OrderItem item = new OrderItem("Chicken Biryani", 3, 280.0);
        assertEquals(840.0, item.getSubtotal(), 0.001);
    }

    @Test
    void orderTotalIsSumOfAllItemSubtotals() {
        Order order = new Order(1, "Shuvo", 1, "Pizza Palace");
        order.addItem(new OrderItem("Margherita Pizza", 1, 250.0));
        order.addItem(new OrderItem("Garlic Bread", 2, 120.0));

        assertEquals(490.0, order.getTotal(), 0.001);
    }
}

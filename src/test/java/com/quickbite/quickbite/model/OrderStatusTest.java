package com.quickbite.quickbite.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void normalFlowMovesForwardOneStepAtATime() {
        assertEquals(OrderStatus.CONFIRMED, OrderStatus.PLACED.next());
        assertEquals(OrderStatus.PREPARING, OrderStatus.CONFIRMED.next());
        assertEquals(OrderStatus.READY, OrderStatus.PREPARING.next());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.READY.next());
        assertEquals(OrderStatus.DELIVERED, OrderStatus.OUT_FOR_DELIVERY.next());
    }

    @Test
    void deliveredAndRejectedHaveNoNextStep() {
        assertNull(OrderStatus.DELIVERED.next());
        assertNull(OrderStatus.REJECTED.next());
    }

    @Test
    void onlyDeliveredAndRejectedAreFinished() {
        assertTrue(OrderStatus.DELIVERED.isFinished());
        assertTrue(OrderStatus.REJECTED.isFinished());
        assertFalse(OrderStatus.PLACED.isFinished());
        assertFalse(OrderStatus.PREPARING.isFinished());
    }

    @Test
    void mainSequenceExcludesRejected() {
        for (OrderStatus status : OrderStatus.mainSequence()) {
            assertFalse(status == OrderStatus.REJECTED);
        }
    }
}

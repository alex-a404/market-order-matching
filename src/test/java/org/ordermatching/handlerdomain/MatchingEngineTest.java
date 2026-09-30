package org.ordermatching.handlerdomain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ordermatching.core.OrderSide;
import org.ordermatching.core.OrderType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine(List.of("ABC", "XYZ"), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private SubmitResult submit(String account, String symbol, OrderSide side, long qty, long price) {
        return engine.submit(account, new OrderRequest(symbol, side, OrderType.LIMIT, qty, price));
    }

    private OrderStatus status(String account, long orderId) {
        return engine.getOrder(account, orderId).orElseThrow().status();
    }

    @Test
    void assignsSequentialOrderIds() {
        assertEquals(1, submit("alice", "ABC", OrderSide.SELL, 10, 100).order().orderId());
        assertEquals(2, submit("alice", "ABC", OrderSide.SELL, 10, 101).order().orderId());
    }

    @Test
    void tradeRecordsBothSidesAtRestingPrice() {
        submit("alice", "ABC", OrderSide.SELL, 10, 100);
        SubmitResult r = submit("bob", "ABC", OrderSide.BUY, 4, 105);

        assertEquals(List.of(new Trade(1, 2, "bob", 1, "alice", 100, 4, NOW)), r.fills());
        assertEquals(OrderStatus.FILLED, r.order().status());
        assertEquals(0, r.order().remainingQuantity());
    }

    @Test
    void statusMovesThroughLifecycle() {
        submit("alice", "ABC", OrderSide.SELL, 10, 100);
        assertEquals(OrderStatus.OPEN, status("alice", 1));

        submit("bob", "ABC", OrderSide.BUY, 4, 100);
        assertEquals(OrderStatus.PARTIALLY_FILLED, status("alice", 1));

        assertTrue(engine.cancel("alice", 1));
        assertEquals(OrderStatus.CANCELLED, status("alice", 1));
        assertEquals(6, engine.getOrder("alice", 1).orElseThrow().remainingQuantity());
    }

    @Test
    void booksAreSeparatePerSymbol() {
        submit("alice", "ABC", OrderSide.SELL, 10, 100);
        SubmitResult r = submit("bob", "XYZ", OrderSide.BUY, 10, 100);

        assertTrue(r.fills().isEmpty());
        assertEquals(OrderStatus.OPEN, r.order().status());
    }

    @Test
    void otherAccountsCannotSeeOrCancelAnOrder() {
        submit("alice", "ABC", OrderSide.SELL, 10, 100);

        assertTrue(engine.getOrder("bob", 1).isEmpty());
        assertFalse(engine.cancel("bob", 1));
        assertEquals(OrderStatus.OPEN, status("alice", 1));
    }

    @Test
    void cancelOfUnknownOrFilledOrderReturnsFalse() {
        assertFalse(engine.cancel("alice", 99));

        submit("alice", "ABC", OrderSide.SELL, 10, 100);
        submit("bob", "ABC", OrderSide.BUY, 10, 100);
        assertFalse(engine.cancel("alice", 1));
        assertEquals(OrderStatus.FILLED, status("alice", 1));
    }

    @Test
    void rejectsInvalidRequests() {
        assertThrows(IllegalArgumentException.class, () -> submit("alice", "NOPE", OrderSide.BUY, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> submit(" ", "ABC", OrderSide.BUY, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> submit(null, "ABC", OrderSide.BUY, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> submit("alice", "ABC", OrderSide.BUY, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> submit("alice", "ABC", OrderSide.BUY, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> submit("alice", "ABC", null, 1, 1));
        assertThrows(UnsupportedOperationException.class,
                () -> engine.submit("alice", new OrderRequest("ABC", OrderSide.BUY, OrderType.MARKET, 1, 1)));
    }

    @Test
    void rejectedRequestDoesNotConsumeAnOrderId() {
        assertThrows(IllegalArgumentException.class, () -> submit("alice", "NOPE", OrderSide.BUY, 1, 1));
        assertEquals(1, submit("alice", "ABC", OrderSide.BUY, 1, 1).order().orderId());
    }
}

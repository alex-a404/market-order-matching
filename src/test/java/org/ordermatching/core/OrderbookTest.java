package org.ordermatching.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderbookTest {

    private Orderbook book;

    @BeforeEach
    void setUp() {
        book = new Orderbook("ABC");   // fresh book for every test
    }

    private Order buy(long id, long qty, long price) {
        return new Order(id, "","ABC", qty, price, OrderSide.BUY, OrderType.LIMIT, Instant.EPOCH);
    }

    private Order sell(long id, long qty, long price) {
        return new Order(id, "","ABC", qty, price, OrderSide.SELL, OrderType.LIMIT, Instant.EPOCH);
    }

    @Test
    void buyThatDoesNotCrossRestsOnBook() {
        book.submit(sell(1, 10, 101));
        List<OrderMatchedData> trades = book.submit(buy(2, 10, 100));

        assertTrue(trades.isEmpty());
        assertEquals(100L, book.bestBid());
        assertEquals(101L, book.bestAsk());
    }

    @Test
    void exactMatchFillsBothAndEmptiesBook() {
        book.submit(sell(1, 10, 100));
        List<OrderMatchedData> trades = book.submit(buy(2, 10, 100));

        assertEquals(1, trades.size());
        assertNull(book.bestBid());
        assertNull(book.bestAsk());
        assertFalse(book.contains(1));
    }

    @Test
    void partialFillRestsRemainder() {
        book.submit(sell(1, 30, 100));
        Order incoming = buy(2, 100, 100);
        book.submit(incoming);

        assertEquals(70, incoming.quantity);
        assertTrue(book.contains(2));
        assertEquals(100L, book.bestBid());
        assertNull(book.bestAsk());
    }

    @Test
    void aggressiveBuySweepsSeveralLevelsAtRestingPrices() {
        book.submit(sell(1, 10, 100));
        book.submit(sell(2, 10, 101));
        book.submit(sell(3, 10, 105));

        List<OrderMatchedData> trades = book.submit(buy(4, 20, 102));

        assertEquals(2, trades.size());
        assertEquals(100, trades.get(0).price());   // rename to match your record's fields
        assertEquals(101, trades.get(1).price());
        assertEquals(105L, book.bestAsk());          // level beyond 102 is untouched
    }

    @Test
    void samePriceFillsInArrivalOrder() {
        book.submit(sell(1, 10, 100));
        book.submit(sell(2, 10, 100));

        List<OrderMatchedData> trades = book.submit(buy(3, 10, 100));

        assertEquals(1, trades.size());
        assertFalse(book.contains(1));   // first seller filled
        assertTrue(book.contains(2));    // second seller still waiting
    }

    @Test
    void cannotDecrementBelowZero() {
        Order o = buy(1, 5, 100);
        assertThrows(IllegalStateException.class, () -> o.mustDecrementQty(6));
    }
}

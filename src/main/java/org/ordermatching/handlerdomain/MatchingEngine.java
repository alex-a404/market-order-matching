package org.ordermatching.handlerdomain;

import org.ordermatching.core.Order;
import org.ordermatching.core.OrderMatchedData;
import org.ordermatching.core.OrderType;
import org.ordermatching.core.Orderbook;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

/**
 * MatchingEngine is the main class, an instance of which will maintain order books for symbols.
 * not thread-safe
 */
public class MatchingEngine {
    private final Map<String, Orderbook> orderBooks = new HashMap<>();
    // every order ever submitted, including filled and cancelled ones, so their status can still be queried.
    // grows without bound; fine for now, needs eviction or persistence later.
    private final Map<Long, Order> orders = new HashMap<>();
    private final Clock clock;
    private long nextOrderId = 1;
    private long nextTradeId = 1;

    public MatchingEngine(Collection<String> symbols) {
        this(symbols, Clock.systemUTC());
    }

    public MatchingEngine(Collection<String> symbols, Clock clock) {
        this.clock = clock;
        for (String symbol : symbols) {
            orderBooks.put(symbol, new Orderbook(symbol));
        }
    }

    public SubmitResult submit(String accountId, OrderRequest request) {
        if (accountId == null || accountId.isBlank()) throw new IllegalArgumentException("Account is required");
        Orderbook book = orderBooks.get(request.symbol());
        if (book == null) throw new IllegalArgumentException("Unknown symbol: " + request.symbol());
        if (request.side() == null) throw new IllegalArgumentException("Side is required");
        if (request.type() != OrderType.LIMIT) throw new UnsupportedOperationException("Only LIMIT orders are supported");
        if (request.quantity() <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (request.price() <= 0) throw new IllegalArgumentException("Price must be positive");

        Instant now = clock.instant();
        Order order = new Order(nextOrderId++, accountId, request.symbol(), request.quantity(), request.price(),
                request.side(), request.type(), now);
        orders.put(order.id, order);

        List<Trade> fills = new ArrayList<>();
        for (OrderMatchedData match : book.submit(order)) {
            Order maker = orders.get(match.openOrderID());
            fills.add(new Trade(nextTradeId++, order.id, order.accountID, maker.id, maker.accountID,
                    match.price(), match.quantity(), now));
        }
        return new SubmitResult(view(order), fills);
    }

    /**
     * @return true if the order was open and is now cancelled, false if it is unknown, owned by another account,
     * or no longer on the book
     */
    public boolean cancel(String accountId, long orderId) {
        Order order = ownedOrder(accountId, orderId);
        if (order == null) return false;
        return orderBooks.get(order.symbol).cancel(orderId);
    }

    /**
     * @return empty if the order is unknown or owned by another account
     */
    public Optional<OrderView> getOrder(String accountId, long orderId) {
        return Optional.ofNullable(ownedOrder(accountId, orderId)).map(this::view);
    }

    // another account's order is treated exactly like a missing one, so callers can't probe which ids exist
    private Order ownedOrder(String accountId, long orderId) {
        Order order = orders.get(orderId);
        if (order == null || !Objects.equals(accountId, order.accountID)) return null;
        return order;
    }

    private OrderView view(Order order) {
        return new OrderView(order.id, order.symbol, order.side, order.type, order.price,
                order.originalQuantity, order.quantity, statusOf(order));
    }

    private OrderStatus statusOf(Order order) {
        if (order.quantity == 0) return OrderStatus.FILLED;
        if (!orderBooks.get(order.symbol).contains(order.id)) return OrderStatus.CANCELLED;

        return order.quantity == order.originalQuantity ? OrderStatus.OPEN : OrderStatus.PARTIALLY_FILLED;
    }
}

package org.ordermatching.core;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Orderbook is a per-symbol registry of bids/asks that matches them together.
 */
public class Orderbook {
    private final String symbol;
    private final TreeMap<Long, Deque<Order>> bids;
    private final TreeMap<Long, Deque<Order>> asks;
    private final HashMap<Long, Order> orders; // for lookup

    /**
     * match will attempt to match an incoming order with an order awaiting in the book
     * @param incoming New order to match
     * @param open Iterated order from book
     * @return OrderMatchedData
     */
    private OrderMatchedData match(@NotNull Order incoming,
                                  @NotNull Order open){

        // must be opposite sides
        if (incoming.side == open.side) {
            return orderNotMatched();
        }

        // awaiting seller wants to sell for more
        if (incoming.side == OrderSide.BUY){
            if (incoming.price < open.price){
                return orderNotMatched();
            }
        }

        // awaiting buyer wants to buy for less
        if (incoming.side == OrderSide.SELL){
            if (incoming.price > open.price){
                return orderNotMatched();
            }
        }

        long tradedQuantity = Math.min(incoming.quantity,open.quantity);
        incoming.mustDecrementQty(tradedQuantity);
        open.mustDecrementQty(tradedQuantity);

        return new OrderMatchedData(
            true,
            incoming.id,
            open.id,
            tradedQuantity,
            open.price
        );
    }

    private OrderMatchedData orderNotMatched(){return new OrderMatchedData(false,0,0,0,0);}

    public Orderbook(String symbol){
        this.symbol=symbol;
        this.bids = new TreeMap<>(Comparator.reverseOrder());
        this.asks = new TreeMap<>();
        this.orders = new HashMap<>();
    }

    /**
     * Submit an order to the order matching engine
     * @param incoming Order to submit
     * @return List of matched orders
     */
    public List<OrderMatchedData> submit(@NotNull Order incoming){
        List<OrderMatchedData> trades = new ArrayList<>();
        TreeMap<Long, Deque<Order>> opposite = incoming.side == OrderSide.BUY ? asks : bids;

        while(incoming.quantity > 0 && !opposite.isEmpty()){
            Map.Entry<Long, Deque<Order>> bestLevel = opposite.firstEntry();
            Deque<Order> queue = bestLevel.getValue();
            Order resting = queue.peekFirst();

            assert resting != null;
            OrderMatchedData result = match(incoming, resting);
            if (!result.matched()) break;   // best price doesn't cross, so nothing else will
            trades.add(result);

            if (resting.quantity == 0) {
                queue.pollFirst();
                orders.remove(resting.id);
            }
            if (queue.isEmpty()) {
                opposite.remove(bestLevel.getKey());
            }

        }
        if (incoming.quantity > 0) {
            rest(incoming);
        }
        return trades;
    }

    private void rest(Order o) {
        TreeMap<Long, Deque<Order>> side = o.side == OrderSide.BUY ? bids : asks;
        side.computeIfAbsent(o.price, p -> new ArrayDeque<>()).addLast(o);
        orders.put(o.id, o);
    }

    public Long bestBid() { return bids.isEmpty() ? null : bids.firstKey(); }
    public Long bestAsk() { return asks.isEmpty() ? null : asks.firstKey(); }
    public boolean contains(long orderId) { return orders.containsKey(orderId); }

    public boolean cancel(long orderId) {
        Order o = orders.remove(orderId);
        if (o == null) return false;
        TreeMap<Long, Deque<Order>> side = o.side == OrderSide.BUY ? bids : asks;
        Deque<Order> level = side.get(o.price);
        level.remove(o);
        if (level.isEmpty()) side.remove(o.price);
        return true;
    }
}


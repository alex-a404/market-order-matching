package org.ordermatching.core;

import java.time.Instant;

// main dataclass for orders in the system
public final class Order {
    public final long id;
    public final String accountID;
    public final String symbol;
    public final long originalQuantity;
    public long quantity; // remaining, decremented as the order fills
    public final long price;
    public final OrderSide side;
    public final OrderType type;
    public final Instant timestamp;

    public Order(long id, String accountID, String symbol, long quantity, long price, OrderSide side, OrderType type, Instant timestamp) {
        this.id = id;
        this.accountID = accountID;
        this.symbol = symbol;
        this.originalQuantity = quantity;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.type = type;
        this.timestamp = timestamp;
    }

    public void mustDecrementQty(long quantity){
        if (this.quantity-quantity<0){ throw new IllegalStateException("Cannot decrement order quantity below zero");}
        this.quantity-=quantity;
    }
}

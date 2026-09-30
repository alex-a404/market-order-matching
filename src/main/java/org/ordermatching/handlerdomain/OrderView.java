package org.ordermatching.handlerdomain;

import org.ordermatching.core.OrderSide;
import org.ordermatching.core.OrderType;

/**
 * OrderView is a snapshot of an order at a given time. Immutable.
 */
public record OrderView(long orderId, String symbol, OrderSide side, OrderType type, long price,
                        long originalQuantity, long remainingQuantity, OrderStatus status) {}

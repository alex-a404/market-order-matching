package org.ordermatching.api.dto;

import org.ordermatching.core.OrderSide;
import org.ordermatching.core.OrderType;
import org.ordermatching.handlerdomain.OrderStatus;
import org.ordermatching.handlerdomain.OrderView;

public record OrderResponse(long orderId, String symbol, OrderSide side, OrderType type, long price,
                            long originalQuantity, long remainingQuantity, OrderStatus status) {

    public static OrderResponse from(OrderView v) {
        return new OrderResponse(v.orderId(), v.symbol(), v.side(), v.type(), v.price(),
                v.originalQuantity(), v.remainingQuantity(), v.status());
    }
}

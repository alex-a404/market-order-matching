package org.ordermatching.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.ordermatching.core.OrderSide;
import org.ordermatching.core.OrderType;
import org.ordermatching.handlerdomain.OrderRequest;

public record PlaceOrderRequest(@NotBlank String symbol,
                                @NotNull OrderSide side,
                                @Positive long quantity,
                                @Positive long price) {

    public OrderRequest toDomain() {
        return new OrderRequest(symbol, side, OrderType.LIMIT, quantity, price);
    }
}

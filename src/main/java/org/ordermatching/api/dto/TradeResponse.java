package org.ordermatching.api.dto;

import org.ordermatching.handlerdomain.Trade;

import java.time.Instant;

public record TradeResponse(long tradeId, long orderId, long price, long quantity, Instant timestamp) {

    public static TradeResponse forTaker(Trade t) {
        return new TradeResponse(t.tradeId(), t.takerOrderId(), t.price(), t.quantity(), t.timestamp());
    }
}

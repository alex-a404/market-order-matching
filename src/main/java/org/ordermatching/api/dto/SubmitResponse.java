package org.ordermatching.api.dto;

import org.ordermatching.handlerdomain.SubmitResult;

import java.util.List;

public record SubmitResponse(OrderResponse order, List<TradeResponse> trades) {

    public static SubmitResponse from(SubmitResult r) {
        return new SubmitResponse(OrderResponse.from(r.order()),
                r.fills().stream().map(TradeResponse::forTaker).toList());
    }
}

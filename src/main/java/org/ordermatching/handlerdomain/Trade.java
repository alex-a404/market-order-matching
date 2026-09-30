package org.ordermatching.handlerdomain;

import java.time.Instant;

// one trade between 2 parties on the book. taker is the incoming order, maker is the order that was resting.
public record Trade(long tradeId,
                    long takerOrderId, String takerAccountId,
                    long makerOrderId, String makerAccountId,
                    long price, long quantity, Instant timestamp) {}

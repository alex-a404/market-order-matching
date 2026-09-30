package org.ordermatching.handlerdomain;

import org.ordermatching.core.OrderSide;
import org.ordermatching.core.OrderType;

public record OrderRequest(String symbol, OrderSide side, OrderType type, long quantity, long price) {}

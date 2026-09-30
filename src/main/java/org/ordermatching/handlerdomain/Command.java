package org.ordermatching.handlerdomain;

// sealed for switch in EngineRunner to be exhaustive
public sealed interface Command permits NewOrder, Cancel, GetOrderStatus {}

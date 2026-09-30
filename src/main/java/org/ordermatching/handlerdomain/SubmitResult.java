package org.ordermatching.handlerdomain;

import java.util.List;

public record SubmitResult(OrderView order, List<Trade> fills) {}

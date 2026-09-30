package org.ordermatching.handlerdomain;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record GetOrderStatus(String accountId, long orderId,
                             CompletableFuture<Optional<OrderView>> reply) implements Command {}

package org.ordermatching.handlerdomain;

import java.util.concurrent.CompletableFuture;

public record Cancel(String accountId, long orderId,
                     CompletableFuture<Boolean> reply) implements Command {}

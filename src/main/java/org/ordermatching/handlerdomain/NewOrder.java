package org.ordermatching.handlerdomain;


import java.util.concurrent.CompletableFuture;

public record NewOrder(String accountId, OrderRequest request,
                       CompletableFuture<SubmitResult> reply) implements Command {}

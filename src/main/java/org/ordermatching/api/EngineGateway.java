package org.ordermatching.api;

import org.ordermatching.handlerdomain.Cancel;
import org.ordermatching.handlerdomain.Command;
import org.ordermatching.handlerdomain.EngineRunner;
import org.ordermatching.handlerdomain.GetOrderStatus;
import org.ordermatching.handlerdomain.NewOrder;
import org.ordermatching.handlerdomain.OrderRequest;
import org.ordermatching.handlerdomain.OrderView;
import org.ordermatching.handlerdomain.SubmitResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
class EngineGateway {
    private final EngineRunner runner;
    private final Duration timeout;

    EngineGateway(EngineRunner runner, EngineProperties props) {
        this.runner = runner;
        this.timeout = props.replyTimeout();
    }

    CompletableFuture<SubmitResult> submit(String accountId, OrderRequest req) {
        return send(f -> new NewOrder(accountId, req, f));
    }

    CompletableFuture<Boolean> cancel(String accountId, long orderId) {
        return send(f -> new Cancel(accountId, orderId, f));
    }

    CompletableFuture<Optional<OrderView>> getOrder(String accountId, long orderId) {
        return send(f -> new GetOrderStatus(accountId, orderId, f));
    }

    /**
     * @throws EngineBusyException if the command could not be queued
     */
    private <T> CompletableFuture<T> send(Function<CompletableFuture<T>, Command> build) {
        var reply = new CompletableFuture<T>();
        if (!runner.offer(build.apply(reply))) throw new EngineBusyException();
        // also covers commands left in the queue when the engine stops, whose replies never complete
        return reply.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}

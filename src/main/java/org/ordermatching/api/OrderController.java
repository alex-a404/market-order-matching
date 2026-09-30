package org.ordermatching.api;

import jakarta.validation.Valid;
import org.ordermatching.api.dto.OrderResponse;
import org.ordermatching.api.dto.PlaceOrderRequest;
import org.ordermatching.api.dto.SubmitResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Returns CompletableFutures so the request thread is released while the engine thread does the work.
 */
@RestController
@RequestMapping("/orders")
class OrderController {

    static final String ACCOUNT_HEADER = "X-Account-Id";

    private final EngineGateway gateway;

    OrderController(EngineGateway gateway) { this.gateway = gateway; }

    @PostMapping
    CompletableFuture<ResponseEntity<SubmitResponse>> place(@RequestHeader(ACCOUNT_HEADER) String accountId,
                                                            @Valid @RequestBody PlaceOrderRequest body) {
        return gateway.submit(accountId, body.toDomain())
                .thenApply(r -> ResponseEntity.created(URI.create("/orders/" + r.order().orderId()))
                        .body(SubmitResponse.from(r)));
    }

    @GetMapping("/{orderId}")
    CompletableFuture<ResponseEntity<OrderResponse>> get(@RequestHeader(ACCOUNT_HEADER) String accountId,
                                                         @PathVariable long orderId) {
        return gateway.getOrder(accountId, orderId)
                .thenApply(v -> v.map(OrderResponse::from)
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{orderId}")
    CompletableFuture<ResponseEntity<Void>> cancel(@RequestHeader(ACCOUNT_HEADER) String accountId,
                                                   @PathVariable long orderId) {
        return gateway.cancel(accountId, orderId)
                .thenApply(cancelled -> cancelled
                        ? ResponseEntity.noContent().<Void>build()
                        : ResponseEntity.notFound().<Void>build());
    }
}

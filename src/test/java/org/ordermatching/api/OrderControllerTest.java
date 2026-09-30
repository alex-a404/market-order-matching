package org.ordermatching.api;

import org.junit.jupiter.api.Test;
import org.ordermatching.core.OrderSide;
import org.ordermatching.core.OrderType;
import org.ordermatching.handlerdomain.OrderRequest;
import org.ordermatching.handlerdomain.OrderStatus;
import org.ordermatching.handlerdomain.OrderView;
import org.ordermatching.handlerdomain.SubmitResult;
import org.ordermatching.handlerdomain.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HTTP contract only: the gateway is mocked, so no engine runs.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String BUY_JSON = """
            {"symbol":"ABC","side":"BUY","quantity":4,"price":105}""";

    @Autowired MockMvcTester mvc;
    @MockitoBean EngineGateway gateway;

    private MockMvcTester.MockMvcRequestBuilder post(String json) {
        return mvc.post().uri("/orders").header("X-Account-Id", "bob")
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    @Test
    void placeReturnsCreatedWithTradesAndHidesCounterparty() {
        var view = new OrderView(2, "ABC", OrderSide.BUY, OrderType.LIMIT, 105, 4, 0, OrderStatus.FILLED);
        var trade = new Trade(1, 2, "bob", 1, "alice", 100, 4, Instant.parse("2026-01-01T00:00:00Z"));
        when(gateway.submit("bob", new OrderRequest("ABC", OrderSide.BUY, OrderType.LIMIT, 4, 105)))
                .thenReturn(CompletableFuture.completedFuture(new SubmitResult(view, List.of(trade))));

        var result = post(BUY_JSON).exchange();

        assertThat(result).hasStatus(201).hasHeader("Location", "/orders/2");
        assertThat(result).bodyJson().extractingPath("$.order.status").isEqualTo("FILLED");
        assertThat(result).bodyJson().extractingPath("$.trades[0].price").isEqualTo(100);
        assertThat(result).bodyJson().extractingPath("$.trades[0].timestamp").isEqualTo("2026-01-01T00:00:00Z");
        assertThat(result).bodyText().doesNotContain("alice").doesNotContain("maker");
    }

    @Test
    void invalidBodyIsRejectedBeforeReachingEngine() {
        assertThat(post("""
                {"symbol":"ABC","side":"BUY","quantity":0,"price":105}""").exchange()).hasStatus(400);
        assertThat(post("""
                {"symbol":"","side":"BUY","quantity":1,"price":105}""").exchange()).hasStatus(400);
        assertThat(post("""
                {"symbol":"ABC","side":"HOLD","quantity":1,"price":105}""").exchange()).hasStatus(400);
        verifyNoInteractions(gateway);
    }

    @Test
    void missingAccountHeaderIsUnauthorized() {
        assertThat(mvc.post().uri("/orders").contentType(MediaType.APPLICATION_JSON).content(BUY_JSON).exchange())
                .hasStatus(401);
        verifyNoInteractions(gateway);
    }

    @Test
    void engineRejectionIsBadRequest() {
        when(gateway.submit(anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalArgumentException("Unknown symbol: ABC")));

        var result = post(BUY_JSON).exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Unknown symbol: ABC");
    }

    @Test
    void fullQueueIsServiceUnavailable() {
        when(gateway.submit(anyString(), any())).thenThrow(new EngineBusyException());

        assertThat(post(BUY_JSON).exchange()).hasStatus(503);
    }

    @Test
    void engineTimeoutIsServiceUnavailable() {
        when(gateway.submit(anyString(), any())).thenReturn(CompletableFuture.failedFuture(new TimeoutException()));

        assertThat(post(BUY_JSON).exchange()).hasStatus(503);
    }

    @Test
    void getMissingOrderIsNotFound() {
        when(gateway.getOrder("bob", 7)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThat(mvc.get().uri("/orders/7").header("X-Account-Id", "bob").exchange()).hasStatus(404);
    }

    @Test
    void cancelReturnsNoContentOrNotFound() {
        when(gateway.cancel("bob", 1)).thenReturn(CompletableFuture.completedFuture(true));
        when(gateway.cancel("bob", 2)).thenReturn(CompletableFuture.completedFuture(false));

        assertThat(mvc.delete().uri("/orders/1").header("X-Account-Id", "bob").exchange()).hasStatus(204);
        assertThat(mvc.delete().uri("/orders/2").header("X-Account-Id", "bob").exchange()).hasStatus(404);
    }

    @Test
    void nonNumericOrderIdIsBadRequest() {
        assertThat(mvc.get().uri("/orders/abc").header("X-Account-Id", "bob").exchange()).hasStatus(400);
        verifyNoInteractions(gateway);
    }
}

package org.ordermatching.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full stack: HTTP -> gateway -> engine thread -> order book and back.
 */
@SpringBootTest(properties = "engine.symbols=ABC,XYZ")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // fresh engine and books per test
class OrderApiIntegrationTest {

    @Autowired MockMvcTester mvc;

    private MvcTestResult place(String account, String symbol, String side, long qty, long price) {
        return mvc.post().uri("/orders").header("X-Account-Id", account).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"symbol":"%s","side":"%s","quantity":%d,"price":%d}""".formatted(symbol, side, qty, price))
                .exchange();
    }

    private MvcTestResult get(String account, long orderId) {
        return mvc.get().uri("/orders/{id}", orderId).header("X-Account-Id", account).exchange();
    }

    private MvcTestResult cancel(String account, long orderId) {
        return mvc.delete().uri("/orders/{id}", orderId).header("X-Account-Id", account).exchange();
    }

    @Test
    void crossingOrdersTradeAndBothSidesSeeTheResult() {
        assertThat(place("alice", "ABC", "SELL", 10, 100)).hasStatus(201)
                .bodyJson().extractingPath("$.order.orderId").isEqualTo(1);

        var bob = place("bob", "ABC", "BUY", 4, 105);
        assertThat(bob).hasStatus(201);
        assertThat(bob).bodyJson().extractingPath("$.order.status").isEqualTo("FILLED");
        assertThat(bob).bodyJson().extractingPath("$.trades[0].price").isEqualTo(100);
        assertThat(bob).bodyJson().extractingPath("$.trades[0].quantity").isEqualTo(4);
        assertThat(bob).bodyText().doesNotContain("alice");

        var alice = get("alice", 1);
        assertThat(alice).hasStatus(200);
        assertThat(alice).bodyJson().extractingPath("$.status").isEqualTo("PARTIALLY_FILLED");
        assertThat(alice).bodyJson().extractingPath("$.remainingQuantity").isEqualTo(6);
    }

    @Test
    void ordersAreInvisibleToOtherAccounts() {
        place("alice", "ABC", "SELL", 10, 100);

        assertThat(get("bob", 1)).hasStatus(404);
        assertThat(cancel("bob", 1)).hasStatus(404);
        assertThat(get("alice", 1)).bodyJson().extractingPath("$.status").isEqualTo("OPEN");
    }

    @Test
    void cancelledOrderNoLongerTrades() {
        place("alice", "ABC", "SELL", 10, 100);
        assertThat(cancel("alice", 1)).hasStatus(204);
        assertThat(cancel("alice", 1)).hasStatus(404);

        assertThat(get("alice", 1)).bodyJson().extractingPath("$.status").isEqualTo("CANCELLED");
        assertThat(place("bob", "ABC", "BUY", 10, 100)).bodyJson().extractingPath("$.trades").asArray().isEmpty();
    }

    @Test
    void unknownSymbolIsBadRequest() {
        var result = place("alice", "NOPE", "BUY", 1, 1);

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Unknown symbol: NOPE");
    }
}

package com.bettingengine.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class LocalK8sRedisIdempotencyIT extends LocalKubernetesTestSupport {

    @Test
    void shouldAvoidDuplicateSettlementPublishesForCompetingOutcomeProcessing() throws Exception {
        assumeLocalKubernetesEnabled();

        String firstCorrelationId = uniqueCorrelationId("local-redis-claim-1");
        String secondCorrelationId = uniqueCorrelationId("local-redis-claim-2");

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<java.net.http.HttpResponse<String>> firstResponse = executor.submit(
                    () -> publishOutcome(6006L, "Idempotency Showcase", 51L, firstCorrelationId)
            );
            Future<java.net.http.HttpResponse<String>> secondResponse = executor.submit(
                    () -> publishOutcome(6006L, "Idempotency Showcase", 51L, secondCorrelationId)
            );

            assertAccepted(firstResponse);
            assertAccepted(secondResponse);
        }
        waitForAsyncProcessing();

        long totalSettlementPublishes = settlementLogCount(firstCorrelationId) + settlementLogCount(secondCorrelationId);
        long totalClaimLogs = logLineCount(firstCorrelationId, "Redis settlement claim")
                + logLineCount(secondCorrelationId, "Redis settlement claim");
        long totalRejectedClaims = logLineCount(firstCorrelationId, "claimed=false")
                + logLineCount(secondCorrelationId, "claimed=false");
        long totalDuplicateSkips = logLineCount(firstCorrelationId, "Skipping duplicate settlement")
                + logLineCount(secondCorrelationId, "Skipping duplicate settlement");

        assertThat(totalSettlementPublishes).isEqualTo(2);
        assertThat(totalClaimLogs).isEqualTo(4);
        assertThat(totalRejectedClaims).isEqualTo(2);
        assertThat(totalDuplicateSkips).isEqualTo(2);
        assertRedisClaimCount(6006L, 2);
    }

    private void assertAccepted(Future<java.net.http.HttpResponse<String>> response)
            throws InterruptedException, ExecutionException {
        assertThat(response.get().statusCode()).isEqualTo(202);
    }
}

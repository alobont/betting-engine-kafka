package com.bettingengine.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class LocalK8sHighAvailabilityIT extends LocalKubernetesTestSupport {

    @Test
    void shouldKeepHttpEntryAvailableWhileStatelessPodsRecover() throws Exception {
        assumeLocalKubernetesEnabled();
        assumeLocalHighAvailabilityEnabled();

        assertDeploymentReady("betting-engine", 2);
        assertDeploymentReady("nginx", 2);

        deletePod(firstPodName("app=nginx"));
        awaitHttpOk("/actuator/health");
        waitForDeploymentReady("nginx", 2);

        deletePod(firstPodName("app=betting-engine"));
        awaitHttpOk("/actuator/health");
        waitForDeploymentReady("betting-engine", 2);

        clearSettlementClaims(5005L);
        String correlationId = uniqueCorrelationId("local-ha-http");
        var response = publishOutcome(5005L, "HA Local HTTP Recovery", 41L, correlationId);

        assertThat(response.statusCode()).isEqualTo(202);
        waitForAsyncProcessing();
        assertLogContains(correlationId, "Matched bets for settlement eventId=5005 correlationId=" + correlationId + " matchedCount=1");
        assertSettlementLogCount(correlationId, 1);
    }

    @Test
    void shouldRecoverKafkaAfterBrokerRestartAndContinueProcessing() throws Exception {
        assumeLocalKubernetesEnabled();
        assumeLocalHighAvailabilityEnabled();

        assertStatefulSetReady("kafka", 3);

        deletePod("kafka-2");
        waitForStatefulSetReady("kafka", 3);

        clearSettlementClaims(2002L);
        String correlationId = uniqueCorrelationId("local-ha-kafka");
        var response = publishOutcome(2002L, "HA Kafka Recovery", 12L, correlationId);

        assertThat(response.statusCode()).isEqualTo(202);
        waitForAsyncProcessing();
        assertLogContains(correlationId, "Consumed event outcome from Kafka topic=event-outcomes eventId=2002");
        assertSettlementLogCount(correlationId, 1);
    }

    @Test
    void shouldKeepProcessingAvailableAfterIgniteNodeLoss() throws Exception {
        assumeLocalKubernetesEnabled();
        assumeLocalHighAvailabilityEnabled();

        assertStatefulSetReady("ignite", 3);

        deletePod("ignite-2");
        waitForStatefulSetReady("ignite", 3);

        clearSettlementClaims(3003L);
        String correlationId = uniqueCorrelationId("local-ha-ignite");
        var response = publishOutcome(3003L, "HA Ignite Node Recovery", 21L, correlationId);

        assertThat(response.statusCode()).isEqualTo(202);
        waitForAsyncProcessing();
        assertLogContains(correlationId, "Matched bets for settlement eventId=3003 correlationId=" + correlationId + " matchedCount=2");
        assertLogContains(correlationId, "Ignite settlement claim eventId=3003");
        assertIgniteClaimCount(3003L, 2);
    }

    @Test
    void shouldKeepIgniteDuplicateProtectionCorrectAfterNodeRecovery() throws Exception {
        assumeLocalKubernetesEnabled();
        assumeLocalHighAvailabilityEnabled();

        assertStatefulSetReady("ignite", 3);

        deletePod("ignite-1");
        waitForStatefulSetReady("ignite", 3);

        clearSettlementClaims(6006L);
        String firstCorrelationId = uniqueCorrelationId("local-ha-ignite-claim-1");
        String secondCorrelationId = uniqueCorrelationId("local-ha-ignite-claim-2");

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<java.net.http.HttpResponse<String>> firstResponse = executor.submit(
                    () -> publishOutcome(6006L, "HA Ignite Idempotency", 51L, firstCorrelationId)
            );
            Future<java.net.http.HttpResponse<String>> secondResponse = executor.submit(
                    () -> publishOutcome(6006L, "HA Ignite Idempotency", 51L, secondCorrelationId)
            );

            assertAccepted(firstResponse);
            assertAccepted(secondResponse);
        }

        waitForAsyncProcessing();

        long totalSettlementPublishes = settlementLogCount(firstCorrelationId) + settlementLogCount(secondCorrelationId);
        long totalRejectedClaims = logLineCount(firstCorrelationId, "claimed=false")
                + logLineCount(secondCorrelationId, "claimed=false");
        long totalDuplicateSkips = logLineCount(firstCorrelationId, "Skipping duplicate settlement")
                + logLineCount(secondCorrelationId, "Skipping duplicate settlement");

        assertThat(totalSettlementPublishes).isEqualTo(2);
        assertThat(totalRejectedClaims).isEqualTo(2);
        assertThat(totalDuplicateSkips).isEqualTo(2);
        assertIgniteClaimCount(6006L, 2);
    }

    @Test
    void shouldRecoverRocketMqComponentsAndContinuePublishingSettlements() throws Exception {
        assumeLocalKubernetesEnabled();
        assumeLocalHighAvailabilityEnabled();

        assertStatefulSetReady("rocketmq-nameserver", 2);
        assertDeploymentReady("rocketmq-broker-a-master", 1);

        deletePod(firstPodName("app=rocketmq-nameserver"));
        waitForStatefulSetReady("rocketmq-nameserver", 2);

        deletePod(firstPodName("app=rocketmq-broker-a-master"));
        waitForDeploymentReady("rocketmq-broker-a-master", 1);

        clearSettlementClaims(4004L);
        String correlationId = uniqueCorrelationId("local-ha-rocketmq");
        var response = publishOutcome(4004L, "HA RocketMQ Recovery", 31L, correlationId);

        assertThat(response.statusCode()).isEqualTo(202);
        waitForAsyncProcessing();
        assertLogContains(correlationId, "Published settlement message to RocketMQ topic=bet-settlements betId=6");
        assertSettlementLogCount(correlationId, 1);
    }

    private void assertAccepted(Future<java.net.http.HttpResponse<String>> response)
            throws InterruptedException, ExecutionException {
        assertThat(response.get().statusCode()).isEqualTo(202);
    }
}

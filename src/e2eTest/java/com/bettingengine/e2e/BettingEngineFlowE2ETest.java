package com.bettingengine.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.bettingengine.integration.LocalKubernetesTestSupport;
import org.junit.jupiter.api.Test;

class BettingEngineFlowE2ETest extends LocalKubernetesTestSupport {

    @Test
    void shouldProcessAnOutcomeEndToEndThroughTheLocalKindDeployment() throws Exception {
        assumeLocalKubernetesEnabled();

        if (localHaEnabled()) {
            assertStatefulSetReady("kafka", 3);
            assertStatefulSetReady("ignite", 3);
            assertStatefulSetReady("rocketmq-nameserver", 2);
            assertDeploymentReady("rocketmq-broker-a-master", 1);
            assertDeploymentReady("rocketmq-broker-a-slave", 1);
            assertDeploymentReady("rocketmq-broker-b-master", 1);
            assertDeploymentReady("rocketmq-broker-b-slave", 1);
            assertDeploymentReady("betting-engine", 2);
            assertDeploymentReady("nginx", 2);
        } else {
            assertDeploymentReady("kafka");
            assertDeploymentReady("rocketmq-nameserver");
            assertDeploymentReady("rocketmq-broker");
            assertStatefulSetReady("ignite", 1);
            assertDeploymentReady("betting-engine");
            assertDeploymentReady("nginx");
        }

        var health = get("/actuator/health");
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("\"status\":\"UP\"");

        clearSettlementClaims(4004L);
        String correlationId = uniqueCorrelationId("local-e2e");
        var publishResponse = publishOutcome(4004L, "League Decider", 31L, correlationId);

        assertThat(publishResponse.statusCode()).isEqualTo(202);
        waitForAsyncProcessing();
        assertLogContains(correlationId, "Published event outcome to Kafka topic=event-outcomes eventId=4004");
        assertLogContains(correlationId, "Consumed event outcome from Kafka topic=event-outcomes eventId=4004");
        assertLogContains(correlationId, "Matched bets for settlement eventId=4004 correlationId=" + correlationId + " matchedCount=1");
        assertLogContains(correlationId, "Ignite settlement claim eventId=4004 betId=6 correlationId=" + correlationId + " settlementKey=6:4004 claimed=true");
        assertSettlementLogCount(correlationId, 1);
    }
}

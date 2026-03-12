package com.bettingengine.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocalK8sKafkaConsumeToBetLookupIT extends LocalKubernetesTestSupport {

    @Test
    void shouldConsumeKafkaMessageAndMatchBetsInsideLocalKindCluster() throws Exception {
        assumeLocalKubernetesEnabled();

        String correlationId = uniqueCorrelationId("local-kafka-match");
        var response = publishOutcome(2002L, "Regional Final", 12L, correlationId);

        assertThat(response.statusCode()).isEqualTo(202);
        waitForAsyncProcessing();
        assertLogContains(correlationId, "Consumed event outcome from Kafka topic=event-outcomes eventId=2002");
        assertLogContains(correlationId, "Matched bets for settlement eventId=2002 correlationId=" + correlationId + " matchedCount=1");
        assertLogContains(correlationId, "Redis settlement claim eventId=2002 betId=3 correlationId=" + correlationId + " settlementKey=3:2002 claimed=true");
    }
}

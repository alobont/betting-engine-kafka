package com.bettingengine.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocalK8sBetLookupToRocketMqPublishIT extends LocalKubernetesTestSupport {

    @Test
    void shouldPublishSettlementMessagesInsideLocalKindCluster() throws Exception {
        assumeLocalKubernetesEnabled();

        String correlationId = uniqueCorrelationId("local-rocketmq");
        var response = publishOutcome(3003L, "Cup Semi Final", 21L, correlationId);

        assertThat(response.statusCode()).isEqualTo(202);
        waitForAsyncProcessing();
        assertLogContains(correlationId, "Redis settlement claim eventId=3003");
        assertSettlementLogCount(correlationId, 2);
    }
}

package com.bettingengine.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocalK8sApiToKafkaPublishIT extends LocalKubernetesTestSupport {

    @Test
    void shouldPublishEventOutcomeFromApiToKafkaInLocalKindCluster() throws Exception {
        assumeLocalKubernetesEnabled();

        String correlationId = uniqueCorrelationId("local-api-kafka");
        var response = publishOutcome(1001L, "Championship Final", 10L, correlationId);

        assertThat(response.statusCode()).isEqualTo(202);
        waitForAsyncProcessing();
        assertLogContains(correlationId, "Published event outcome to Kafka topic=event-outcomes eventId=1001");
    }
}

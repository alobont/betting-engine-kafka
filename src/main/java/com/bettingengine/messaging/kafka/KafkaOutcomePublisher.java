package com.bettingengine.messaging.kafka;

import com.bettingengine.config.TopicProperties;
import com.bettingengine.dto.api.EventOutcomeRequest;
import com.bettingengine.dto.messaging.EventOutcomeMessage;
import com.bettingengine.exception.MessagingPublishException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOutcomePublisher {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private static final Logger log = LoggerFactory.getLogger(KafkaOutcomePublisher.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final TopicProperties topicProperties;

    public KafkaOutcomePublisher(KafkaTemplate<String, byte[]> kafkaTemplate, TopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    public void publish(EventOutcomeRequest request, String correlationId) {
        EventOutcomeMessage message = EventOutcomeMessage.newBuilder()
                .setEventId(request.eventId())
                .setEventName(request.eventName())
                .setEventWinnerId(request.eventWinnerId())
                .build();

        ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                topicProperties.getEventOutcomes(),
                request.eventId().toString(),
                message.toByteArray()
        );
        record.headers().add(CORRELATION_HEADER, correlationId.getBytes(StandardCharsets.UTF_8));

        try {
            kafkaTemplate.send(record).get(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
            log.info(
                    "Published event outcome to Kafka topic={} eventId={} correlationId={}",
                    topicProperties.getEventOutcomes(),
                    request.eventId(),
                    correlationId
            );
        } catch (Exception exception) {
            throw new MessagingPublishException("Failed to publish event outcome to Kafka.", exception);
        }
    }

    public String topic() {
        return topicProperties.getEventOutcomes();
    }
}

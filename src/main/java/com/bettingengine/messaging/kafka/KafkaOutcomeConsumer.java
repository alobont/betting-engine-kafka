package com.bettingengine.messaging.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.bettingengine.dto.messaging.EventOutcomeMessage;
import com.bettingengine.service.EventOutcomeProcessingService;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

@Component
public class KafkaOutcomeConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutcomeConsumer.class);

    private final EventOutcomeProcessingService eventOutcomeProcessingService;

    public KafkaOutcomeConsumer(EventOutcomeProcessingService eventOutcomeProcessingService) {
        this.eventOutcomeProcessingService = eventOutcomeProcessingService;
    }

    @KafkaListener(topics = "${app.messaging.topics.event-outcomes}")
    public void consume(
            byte[] payload,
            @org.springframework.messaging.handler.annotation.Header(name = KafkaHeaders.RECEIVED_KEY, required = false)
            String eventKey,
            @org.springframework.messaging.handler.annotation.Header(name = KafkaHeaders.RECEIVED_TOPIC)
            String topic,
            @org.springframework.messaging.handler.annotation.Header(name = KafkaOutcomePublisher.CORRELATION_HEADER, required = false)
            byte[] correlationHeader) {

        String correlationId = correlationHeader == null ? "unknown" : new String(correlationHeader, StandardCharsets.UTF_8);

        try {
            EventOutcomeMessage message = EventOutcomeMessage.parseFrom(payload);
            log.info(
                    "Consumed event outcome from Kafka topic={} eventId={} kafkaKey={} correlationId={}",
                    topic,
                    message.getEventId(),
                    eventKey,
                    correlationId
            );
            int publishedCount = eventOutcomeProcessingService.process(message, correlationId);
            log.info(
                    "Finished processing event outcome eventId={} correlationId={} publishedSettlementCount={}",
                    message.getEventId(),
                    correlationId,
                    publishedCount
            );
        } catch (InvalidProtocolBufferException exception) {
            log.error("Invalid Protobuf payload received from Kafka topic={} correlationId={}", topic, correlationId, exception);
            throw new IllegalArgumentException("Failed to deserialize Protobuf event outcome payload.", exception);
        }
    }
}

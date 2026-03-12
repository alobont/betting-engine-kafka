package com.bettingengine.service;

import com.bettingengine.dto.api.EventOutcomeRequest;
import com.bettingengine.dto.api.PublishEventOutcomeResponse;
import com.bettingengine.messaging.kafka.KafkaOutcomePublisher;
import org.springframework.stereotype.Service;

@Service
public class EventOutcomePublishingService {

    private final KafkaOutcomePublisher kafkaOutcomePublisher;

    public EventOutcomePublishingService(KafkaOutcomePublisher kafkaOutcomePublisher) {
        this.kafkaOutcomePublisher = kafkaOutcomePublisher;
    }

    public PublishEventOutcomeResponse publish(EventOutcomeRequest request, String correlationId) {
        kafkaOutcomePublisher.publish(request, correlationId);
        return new PublishEventOutcomeResponse("PUBLISHED", request.eventId(), kafkaOutcomePublisher.topic(), correlationId);
    }
}

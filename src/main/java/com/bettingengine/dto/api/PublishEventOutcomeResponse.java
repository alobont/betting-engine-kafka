package com.bettingengine.dto.api;

public record PublishEventOutcomeResponse(
        String status,
        Long eventId,
        String topic,
        String correlationId
) {
}

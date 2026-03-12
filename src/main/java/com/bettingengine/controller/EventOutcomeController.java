package com.bettingengine.controller;

import com.bettingengine.dto.api.EventOutcomeRequest;
import com.bettingengine.dto.api.PublishEventOutcomeResponse;
import com.bettingengine.service.EventOutcomePublishingService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/event-outcomes")
public class EventOutcomeController {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final EventOutcomePublishingService publishingService;

    public EventOutcomeController(EventOutcomePublishingService publishingService) {
        this.publishingService = publishingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublishEventOutcomeResponse publishEventOutcome(
            @Valid @RequestBody EventOutcomeRequest request,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlationId,
            HttpServletResponse response) {

        String resolvedCorrelationId = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
        response.setHeader(CORRELATION_HEADER, resolvedCorrelationId);
        return publishingService.publish(request, resolvedCorrelationId);
    }
}

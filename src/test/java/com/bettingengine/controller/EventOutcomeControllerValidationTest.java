package com.bettingengine.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bettingengine.dto.api.PublishEventOutcomeResponse;
import com.bettingengine.exception.ApiExceptionHandler;
import com.bettingengine.service.EventOutcomePublishingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventOutcomeController.class)
@Import(ApiExceptionHandler.class)
class EventOutcomeControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventOutcomePublishingService publishingService;

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": 0,
                                  "eventName": "",
                                  "eventWinnerId": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldAcceptValidPayloadAndReturnCorrelationIdHeader() throws Exception {
        when(publishingService.publish(any(), anyString()))
                .thenReturn(new PublishEventOutcomeResponse("PUBLISHED", 1001L, "event-outcomes", "corr-1"));

        mockMvc.perform(post("/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": 1001,
                                  "eventName": "Championship Final",
                                  "eventWinnerId": 10
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.eventId").value(1001));
    }
}

package com.bettingengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bettingengine.dto.messaging.EventOutcomeMessage;
import com.bettingengine.entity.BetEntity;
import com.bettingengine.messaging.rocketmq.RocketMqSettlementPublisher;
import com.bettingengine.repository.BetRepository;
import com.bettingengine.repository.SettlementClaimRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventOutcomeProcessingServiceTest {

    private final BetRepository betRepository = mock(BetRepository.class);
    private final RocketMqSettlementPublisher settlementPublisher = mock(RocketMqSettlementPublisher.class);
    private final SettlementClaimRepository settlementClaimRepository = mock(SettlementClaimRepository.class);

    private final EventOutcomeProcessingService processingService = new EventOutcomeProcessingService(
            betRepository,
            settlementClaimRepository,
            settlementPublisher
    );

    @Test
    void shouldMatchByEventIdAndPublishAllMatchingBets() {
        when(betRepository.findByEventId(1001L)).thenReturn(List.of(
                bet(1L, 101L, 1001L, 501L, 10L, "12.50"),
                bet(2L, 102L, 1001L, 502L, 11L, "20.00")
        ));
        when(settlementClaimRepository.claim("1:1001")).thenReturn(true);
        when(settlementClaimRepository.claim("2:1001")).thenReturn(true);

        int publishedCount = processingService.process(eventOutcome(1001L), "corr-1001");

        assertThat(publishedCount).isEqualTo(2);
        verify(settlementPublisher, times(2)).publish(any(), org.mockito.ArgumentMatchers.eq("corr-1001"), anyString());
    }

    @Test
    void shouldSkipDuplicateSettlementKeysWhenRedisClaimAlreadyExists() {
        when(betRepository.findByEventId(1001L)).thenReturn(List.of(
                bet(1L, 101L, 1001L, 501L, 10L, "12.50")
        ));
        when(settlementClaimRepository.claim("1:1001")).thenReturn(false);

        int publishedCount = processingService.process(eventOutcome(1001L), "corr-1");

        assertThat(publishedCount).isZero();
        verify(settlementPublisher, never()).publish(any(), anyString(), anyString());
    }

    @Test
    void shouldReleaseRedisClaimWhenSettlementPublishFails() {
        when(betRepository.findByEventId(1001L)).thenReturn(List.of(
                bet(1L, 101L, 1001L, 501L, 10L, "12.50")
        ));
        when(settlementClaimRepository.claim("1:1001")).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("rocketmq unavailable"))
                .when(settlementPublisher)
                .publish(any(), anyString(), anyString());

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> processingService.process(eventOutcome(1001L), "corr-1")
        );

        verify(settlementClaimRepository).release("1:1001");
    }

    private EventOutcomeMessage eventOutcome(Long eventId) {
        return EventOutcomeMessage.newBuilder()
                .setEventId(eventId)
                .setEventName("Final")
                .setEventWinnerId(10L)
                .build();
    }

    private BetEntity bet(Long betId, Long userId, Long eventId, Long marketId, Long winnerId, String amount) {
        BetEntity bet = new BetEntity();
        bet.setBetId(betId);
        bet.setUserId(userId);
        bet.setEventId(eventId);
        bet.setEventMarketId(marketId);
        bet.setEventWinnerId(winnerId);
        bet.setBetAmount(new BigDecimal(amount));
        return bet;
    }
}

package com.bettingengine.service;

import com.bettingengine.dto.messaging.EventOutcomeMessage;
import com.bettingengine.entity.BetEntity;
import com.bettingengine.messaging.rocketmq.RocketMqSettlementPublisher;
import com.bettingengine.repository.BetRepository;
import com.bettingengine.repository.SettlementClaimRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EventOutcomeProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EventOutcomeProcessingService.class);

    private final BetRepository betRepository;
    private final SettlementClaimRepository settlementClaimRepository;
    private final RocketMqSettlementPublisher rocketMqSettlementPublisher;

    public EventOutcomeProcessingService(
            BetRepository betRepository,
            SettlementClaimRepository settlementClaimRepository,
            RocketMqSettlementPublisher rocketMqSettlementPublisher) {
        this.betRepository = betRepository;
        this.settlementClaimRepository = settlementClaimRepository;
        this.rocketMqSettlementPublisher = rocketMqSettlementPublisher;
    }

    public int process(EventOutcomeMessage message, String correlationId) {
        List<BetEntity> matchingBets = betRepository.findByEventId(message.getEventId());
        log.info(
                "Matched bets for settlement eventId={} correlationId={} matchedCount={}",
                message.getEventId(),
                correlationId,
                matchingBets.size()
        );

        int publishedCount = 0;
        for (BetEntity bet : matchingBets) {
            String settlementKey = settlementKey(bet);
            boolean claimed = settlementClaimRepository.claim(settlementKey);
            log.info(
                    "Ignite settlement claim eventId={} betId={} correlationId={} settlementKey={} claimed={}",
                    bet.getEventId(),
                    bet.getBetId(),
                    correlationId,
                    settlementKey,
                    claimed
            );
            if (!claimed) {
                log.info(
                        "Skipping duplicate settlement eventId={} betId={} correlationId={} settlementKey={}",
                        bet.getEventId(),
                        bet.getBetId(),
                        correlationId,
                        settlementKey
                );
                continue;
            }

            try {
                rocketMqSettlementPublisher.publish(bet, correlationId, settlementKey);
                publishedCount++;
            } catch (RuntimeException exception) {
                releaseClaim(settlementKey, correlationId, bet, exception);
                throw exception;
            }
        }

        return publishedCount;
    }

    private String settlementKey(BetEntity bet) {
        return bet.getBetId() + ":" + bet.getEventId();
    }

    private void releaseClaim(String settlementKey, String correlationId, BetEntity bet, RuntimeException exception) {
        try {
            settlementClaimRepository.release(settlementKey);
            log.warn(
                    "Released Ignite settlement claim after publish failure eventId={} betId={} correlationId={} settlementKey={}",
                    bet.getEventId(),
                    bet.getBetId(),
                    correlationId,
                    settlementKey
            );
        } catch (RuntimeException releaseException) {
            log.error(
                    "Failed to release Ignite settlement claim eventId={} betId={} correlationId={} settlementKey={}",
                    bet.getEventId(),
                    bet.getBetId(),
                    correlationId,
                    settlementKey,
                    releaseException
            );
            exception.addSuppressed(releaseException);
        }
    }
}

package com.bettingengine.entity;

import java.math.BigDecimal;

public class BetEntity {

    private Long betId;

    private Long userId;

    private Long eventId;

    private Long eventMarketId;

    private Long eventWinnerId;

    private BigDecimal betAmount;

    public BetEntity() {
    }

    public BetEntity(
            Long betId,
            Long userId,
            Long eventId,
            Long eventMarketId,
            Long eventWinnerId,
            BigDecimal betAmount) {
        this.betId = betId;
        this.userId = userId;
        this.eventId = eventId;
        this.eventMarketId = eventMarketId;
        this.eventWinnerId = eventWinnerId;
        this.betAmount = betAmount;
    }

    public Long getBetId() {
        return betId;
    }

    public void setBetId(Long betId) {
        this.betId = betId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getEventMarketId() {
        return eventMarketId;
    }

    public void setEventMarketId(Long eventMarketId) {
        this.eventMarketId = eventMarketId;
    }

    public Long getEventWinnerId() {
        return eventWinnerId;
    }

    public void setEventWinnerId(Long eventWinnerId) {
        this.eventWinnerId = eventWinnerId;
    }

    public BigDecimal getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(BigDecimal betAmount) {
        this.betAmount = betAmount;
    }
}

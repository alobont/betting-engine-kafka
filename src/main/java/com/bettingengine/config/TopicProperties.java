package com.bettingengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.topics")
public class TopicProperties {

    private String eventOutcomes;
    private String betSettlements;

    public String getEventOutcomes() {
        return eventOutcomes;
    }

    public void setEventOutcomes(String eventOutcomes) {
        this.eventOutcomes = eventOutcomes;
    }

    public String getBetSettlements() {
        return betSettlements;
    }

    public void setBetSettlements(String betSettlements) {
        this.betSettlements = betSettlements;
    }
}

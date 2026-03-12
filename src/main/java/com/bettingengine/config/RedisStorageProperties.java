package com.bettingengine.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public class RedisStorageProperties {

    private String betKeyPrefix;
    private String eventIndexPrefix;
    private String settlementKeyPrefix;
    private Duration settlementClaimTtl;

    public String getBetKeyPrefix() {
        return betKeyPrefix;
    }

    public void setBetKeyPrefix(String betKeyPrefix) {
        this.betKeyPrefix = betKeyPrefix;
    }

    public String getEventIndexPrefix() {
        return eventIndexPrefix;
    }

    public void setEventIndexPrefix(String eventIndexPrefix) {
        this.eventIndexPrefix = eventIndexPrefix;
    }

    public String getSettlementKeyPrefix() {
        return settlementKeyPrefix;
    }

    public void setSettlementKeyPrefix(String settlementKeyPrefix) {
        this.settlementKeyPrefix = settlementKeyPrefix;
    }

    public Duration getSettlementClaimTtl() {
        return settlementClaimTtl;
    }

    public void setSettlementClaimTtl(Duration settlementClaimTtl) {
        this.settlementClaimTtl = settlementClaimTtl;
    }
}

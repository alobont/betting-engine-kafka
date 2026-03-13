package com.bettingengine.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ignite")
public class IgniteStorageProperties {

    private List<String> addresses = new ArrayList<>();
    private String zoneName;
    private String storageProfile;
    private int zonePartitions;
    private int zoneReplicas;
    private String betTableName;
    private String settlementClaimTableName;
    private Duration settlementClaimTtl;

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getStorageProfile() {
        return storageProfile;
    }

    public void setStorageProfile(String storageProfile) {
        this.storageProfile = storageProfile;
    }

    public int getZonePartitions() {
        return zonePartitions;
    }

    public void setZonePartitions(int zonePartitions) {
        this.zonePartitions = zonePartitions;
    }

    public int getZoneReplicas() {
        return zoneReplicas;
    }

    public void setZoneReplicas(int zoneReplicas) {
        this.zoneReplicas = zoneReplicas;
    }

    public String getBetTableName() {
        return betTableName;
    }

    public void setBetTableName(String betTableName) {
        this.betTableName = betTableName;
    }

    public String getSettlementClaimTableName() {
        return settlementClaimTableName;
    }

    public void setSettlementClaimTableName(String settlementClaimTableName) {
        this.settlementClaimTableName = settlementClaimTableName;
    }

    public Duration getSettlementClaimTtl() {
        return settlementClaimTtl;
    }

    public void setSettlementClaimTtl(Duration settlementClaimTtl) {
        this.settlementClaimTtl = settlementClaimTtl;
    }
}

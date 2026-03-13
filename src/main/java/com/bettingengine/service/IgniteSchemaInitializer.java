package com.bettingengine.service;

import com.bettingengine.config.IgniteStorageProperties;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class IgniteSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IgniteSchemaInitializer.class);
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final int MAX_SCHEMA_INIT_ATTEMPTS = 5;
    private static final Duration SCHEMA_INIT_RETRY_DELAY = Duration.ofSeconds(5);

    private final IgniteClient igniteClient;
    private final IgniteStorageProperties igniteStorageProperties;

    public IgniteSchemaInitializer(IgniteClient igniteClient, IgniteStorageProperties igniteStorageProperties) {
        this.igniteClient = igniteClient;
        this.igniteStorageProperties = igniteStorageProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String zoneName = identifier(igniteStorageProperties.getZoneName());
        String betTableName = identifier(igniteStorageProperties.getBetTableName());
        String settlementClaimTableName = identifier(igniteStorageProperties.getSettlementClaimTableName());
        String storageProfile = identifier(igniteStorageProperties.getStorageProfile()).toLowerCase(Locale.ROOT);
        String eventIndexName = identifier("IDX_" + betTableName + "_EVENT_ID");

        String ddl = """
                CREATE ZONE IF NOT EXISTS %s WITH partitions=%d, replicas=%d, storage_profiles='%s';
                CREATE TABLE IF NOT EXISTS %s (
                    EVENT_ID BIGINT NOT NULL,
                    BET_ID BIGINT NOT NULL,
                    USER_ID BIGINT NOT NULL,
                    EVENT_MARKET_ID BIGINT NOT NULL,
                    EVENT_WINNER_ID BIGINT NOT NULL,
                    BET_AMOUNT DECIMAL(19, 4) NOT NULL,
                    PRIMARY KEY (EVENT_ID, BET_ID)
                ) COLOCATE BY (EVENT_ID) ZONE %s;
                CREATE INDEX IF NOT EXISTS %s ON %s (EVENT_ID);
                CREATE TABLE IF NOT EXISTS %s (
                    SETTLEMENT_KEY VARCHAR PRIMARY KEY,
                    EXPIRES_AT TIMESTAMP NOT NULL
                ) ZONE %s;
                """.formatted(
                zoneName,
                igniteStorageProperties.getZonePartitions(),
                igniteStorageProperties.getZoneReplicas(),
                storageProfile,
                betTableName,
                zoneName,
                eventIndexName,
                betTableName,
                settlementClaimTableName,
                zoneName
        );

        initializeSchemaWithRetry(ddl, zoneName, betTableName, settlementClaimTableName, storageProfile);
    }

    private String identifier(String value) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new IllegalStateException("Invalid Ignite SQL identifier: " + value);
        }

        return value.toUpperCase(Locale.ROOT);
    }

    private void initializeSchemaWithRetry(
            String ddl,
            String zoneName,
            String betTableName,
            String settlementClaimTableName,
            String storageProfile
    ) {
        for (int attempt = 1; attempt <= MAX_SCHEMA_INIT_ATTEMPTS; attempt++) {
            if (schemaReady(betTableName, settlementClaimTableName)) {
                log.info(
                        "Ignite storage schema already available zone={} betTable={} settlementClaimTable={}",
                        zoneName,
                        betTableName,
                        settlementClaimTableName
                );
                return;
            }

            try {
                igniteClient.sql().executeScript(ddl);
                log.info(
                        "Initialized Ignite storage zone={} betTable={} settlementClaimTable={} replicas={} partitions={} storageProfile={}",
                        zoneName,
                        betTableName,
                        settlementClaimTableName,
                        igniteStorageProperties.getZoneReplicas(),
                        igniteStorageProperties.getZonePartitions(),
                        storageProfile
                );
                return;
            } catch (RuntimeException exception) {
                if (schemaReady(betTableName, settlementClaimTableName)) {
                    log.info(
                            "Ignite storage schema became available after concurrent initialization zone={} betTable={} settlementClaimTable={}",
                            zoneName,
                            betTableName,
                            settlementClaimTableName
                    );
                    return;
                }

                if (attempt == MAX_SCHEMA_INIT_ATTEMPTS) {
                    throw exception;
                }

                log.warn(
                        "Ignite schema initialization attempt {} failed; retrying in {} seconds.",
                        attempt,
                        SCHEMA_INIT_RETRY_DELAY.toSeconds(),
                        exception
                );
                sleepBeforeRetry();
            }
        }
    }

    private boolean schemaReady(String betTableName, String settlementClaimTableName) {
        try {
            return igniteClient.tables().table(betTableName) != null
                    && igniteClient.tables().table(settlementClaimTableName) != null;
        } catch (RuntimeException exception) {
            log.warn("Ignite schema readiness check failed; treating schema as not ready yet.", exception);
            return false;
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(SCHEMA_INIT_RETRY_DELAY.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry Ignite schema initialization.", exception);
        }
    }
}

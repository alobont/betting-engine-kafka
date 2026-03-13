package com.bettingengine.repository;

import com.bettingengine.config.IgniteStorageProperties;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementClaimRepository {

    private static final int MAX_CLAIM_ATTEMPTS = 3;

    private final IgniteClient igniteClient;
    private final IgniteStorageProperties igniteStorageProperties;

    public SettlementClaimRepository(IgniteClient igniteClient, IgniteStorageProperties igniteStorageProperties) {
        this.igniteClient = igniteClient;
        this.igniteStorageProperties = igniteStorageProperties;
    }

    public boolean claim(String settlementKey) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiresAt = now.plus(igniteStorageProperties.getSettlementClaimTtl());

        for (int attempt = 1; attempt <= MAX_CLAIM_ATTEMPTS; attempt++) {
            Transaction transaction = igniteClient.transactions().begin();
            try {
                KeyValueView<Tuple, Tuple> keyValueView = claimKeyValueView();
                Tuple claimKey = claimKey(settlementKey);
                Tuple requestedClaim = claimValue(expiresAt);

                if (keyValueView.putIfAbsent(transaction, claimKey, requestedClaim)) {
                    transaction.commit();
                    return true;
                }

                Tuple currentClaim = keyValueView.get(transaction, claimKey);
                if (currentClaim == null) {
                    transaction.rollback();
                    continue;
                }

                if (currentClaim.datetimeValue("EXPIRES_AT").isAfter(now)) {
                    transaction.rollback();
                    return false;
                }

                if (keyValueView.replace(transaction, claimKey, currentClaim, requestedClaim)) {
                    transaction.commit();
                    return true;
                }

                transaction.rollback();
            } catch (RuntimeException exception) {
                rollbackQuietly(transaction, exception);
                if (attempt == MAX_CLAIM_ATTEMPTS) {
                    throw exception;
                }
            }
        }

        return false;
    }

    public void release(String settlementKey) {
        claimKeyValueView().remove(null, claimKey(settlementKey));
    }

    private KeyValueView<Tuple, Tuple> claimKeyValueView() {
        return requireTable(igniteStorageProperties.getSettlementClaimTableName()).keyValueView();
    }

    private Table requireTable(String tableName) {
        Table table = igniteClient.tables().table(tableName);
        if (table == null) {
            throw new IllegalStateException("Ignite table is not available: " + tableName);
        }
        return table;
    }

    private Tuple claimKey(String settlementKey) {
        return Tuple.create().set("SETTLEMENT_KEY", settlementKey);
    }

    private Tuple claimValue(LocalDateTime expiresAt) {
        return Tuple.create().set("EXPIRES_AT", expiresAt);
    }

    private void rollbackQuietly(Transaction transaction, RuntimeException originalException) {
        try {
            transaction.rollback();
        } catch (RuntimeException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}

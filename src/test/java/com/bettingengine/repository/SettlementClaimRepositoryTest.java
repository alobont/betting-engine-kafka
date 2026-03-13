package com.bettingengine.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bettingengine.config.IgniteStorageProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.IgniteTables;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.IgniteTransactions;
import org.apache.ignite.tx.Transaction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SettlementClaimRepositoryTest {

    private final IgniteClient igniteClient = mock(IgniteClient.class);
    private final IgniteTransactions igniteTransactions = mock(IgniteTransactions.class);
    private final IgniteTables igniteTables = mock(IgniteTables.class);
    private final Table table = mock(Table.class);
    private final KeyValueView<Tuple, Tuple> keyValueView = mock(KeyValueView.class);
    private final Transaction transaction = mock(Transaction.class);

    private final SettlementClaimRepository settlementClaimRepository =
            new SettlementClaimRepository(igniteClient, igniteProperties());

    @Test
    void shouldClaimSettlementKeyInsideIgniteTransaction() {
        when(igniteClient.transactions()).thenReturn(igniteTransactions);
        when(igniteTransactions.begin()).thenReturn(transaction);
        when(igniteClient.tables()).thenReturn(igniteTables);
        when(igniteTables.table("SETTLEMENT_CLAIMS")).thenReturn(table);
        when(table.keyValueView()).thenReturn(keyValueView);
        when(keyValueView.putIfAbsent(eq(transaction), any(Tuple.class), any(Tuple.class))).thenReturn(true);

        boolean claimed = settlementClaimRepository.claim("1:1001");

        assertThat(claimed).isTrue();
        verify(transaction).commit();
    }

    @Test
    void shouldRefreshExpiredSettlementKeyClaimInsideIgniteTransaction() {
        Tuple expiredClaim = Tuple.create()
                .set("SETTLEMENT_KEY", "1:1001")
                .set("EXPIRES_AT", LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));

        when(igniteClient.transactions()).thenReturn(igniteTransactions);
        when(igniteTransactions.begin()).thenReturn(transaction);
        when(igniteClient.tables()).thenReturn(igniteTables);
        when(igniteTables.table("SETTLEMENT_CLAIMS")).thenReturn(table);
        when(table.keyValueView()).thenReturn(keyValueView);
        when(keyValueView.putIfAbsent(eq(transaction), any(Tuple.class), any(Tuple.class))).thenReturn(false);
        when(keyValueView.get(eq(transaction), any(Tuple.class))).thenReturn(expiredClaim);
        when(keyValueView.replace(eq(transaction), any(Tuple.class), eq(expiredClaim), any(Tuple.class))).thenReturn(true);

        boolean claimed = settlementClaimRepository.claim("1:1001");

        assertThat(claimed).isTrue();
        verify(transaction).commit();
    }

    @Test
    void shouldNotClaimUnexpiredSettlementKeyFromIgnite() {
        Tuple activeClaim = Tuple.create()
                .set("SETTLEMENT_KEY", "1:1001")
                .set("EXPIRES_AT", LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));

        when(igniteClient.transactions()).thenReturn(igniteTransactions);
        when(igniteTransactions.begin()).thenReturn(transaction);
        when(igniteClient.tables()).thenReturn(igniteTables);
        when(igniteTables.table("SETTLEMENT_CLAIMS")).thenReturn(table);
        when(table.keyValueView()).thenReturn(keyValueView);
        when(keyValueView.putIfAbsent(eq(transaction), any(Tuple.class), any(Tuple.class))).thenReturn(false);
        when(keyValueView.get(eq(transaction), any(Tuple.class))).thenReturn(activeClaim);

        boolean claimed = settlementClaimRepository.claim("1:1001");

        assertThat(claimed).isFalse();
        verify(transaction).rollback();
    }

    @Test
    void shouldReleaseSettlementKeyFromIgnite() {
        when(igniteClient.tables()).thenReturn(igniteTables);
        when(igniteTables.table("SETTLEMENT_CLAIMS")).thenReturn(table);
        when(table.keyValueView()).thenReturn(keyValueView);

        settlementClaimRepository.release("1:1001");

        ArgumentCaptor<Tuple> tupleCaptor = ArgumentCaptor.forClass(Tuple.class);
        verify(keyValueView).remove(isNull(), tupleCaptor.capture());
        assertThat(tupleCaptor.getValue().stringValue("SETTLEMENT_KEY")).isEqualTo("1:1001");
    }

    private IgniteStorageProperties igniteProperties() {
        IgniteStorageProperties properties = new IgniteStorageProperties();
        properties.setAddresses(List.of("127.0.0.1:10800"));
        properties.setZoneName("BETTING_ENGINE_ZONE");
        properties.setStorageProfile("default_aimem");
        properties.setZonePartitions(8);
        properties.setZoneReplicas(1);
        properties.setBetTableName("BETS");
        properties.setSettlementClaimTableName("SETTLEMENT_CLAIMS");
        properties.setSettlementClaimTtl(Duration.ofMinutes(30));
        return properties;
    }
}

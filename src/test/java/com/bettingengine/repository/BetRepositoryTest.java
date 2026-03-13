package com.bettingengine.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bettingengine.config.IgniteStorageProperties;
import com.bettingengine.entity.BetEntity;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.lang.Cursor;
import org.apache.ignite.table.IgniteTables;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BetRepositoryTest {

    private final IgniteClient igniteClient = mock(IgniteClient.class);
    private final IgniteTables igniteTables = mock(IgniteTables.class);
    private final Table table = mock(Table.class);
    private final RecordView<Tuple> recordView = mock(RecordView.class);
    private final Cursor<Tuple> cursor = mock(Cursor.class);

    private final BetRepository betRepository = new BetRepository(igniteClient, igniteProperties());

    @Test
    void shouldFindBetsByEventIdFromIgnite() {
        Tuple firstRow = betTuple(2L, 102L, 1001L, 502L, 11L, "20.00");
        Tuple secondRow = betTuple(1L, 101L, 1001L, 501L, 10L, "12.50");

        when(igniteClient.tables()).thenReturn(igniteTables);
        when(igniteTables.table("BETS")).thenReturn(table);
        when(table.recordView()).thenReturn(recordView);
        when(recordView.query(isNull(), any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(firstRow, secondRow);

        var bets = betRepository.findByEventId(1001L);

        assertThat(bets).extracting(BetEntity::getBetId).containsExactly(1L, 2L);
        assertThat(bets).extracting(bet -> bet.getBetAmount().toPlainString()).containsExactly("12.50", "20.00");
    }

    @Test
    void shouldStoreBetsInIgniteRows() {
        when(igniteClient.tables()).thenReturn(igniteTables);
        when(igniteTables.table("BETS")).thenReturn(table);
        when(table.recordView()).thenReturn(recordView);

        betRepository.saveAll(Set.of(new BetEntity(
                9L,
                109L,
                9009L,
                509L,
                49L,
                new BigDecimal("17.35")
        )));

        ArgumentCaptor<List<Tuple>> tuplesCaptor = ArgumentCaptor.forClass(List.class);
        verify(recordView).upsertAll(isNull(), tuplesCaptor.capture());

        assertThat(tuplesCaptor.getValue()).singleElement().satisfies(tuple -> {
            assertThat(tuple.longValue("BET_ID")).isEqualTo(9L);
            assertThat(tuple.longValue("USER_ID")).isEqualTo(109L);
            assertThat(tuple.longValue("EVENT_ID")).isEqualTo(9009L);
            assertThat(tuple.longValue("EVENT_MARKET_ID")).isEqualTo(509L);
            assertThat(tuple.longValue("EVENT_WINNER_ID")).isEqualTo(49L);
            assertThat(tuple.decimalValue("BET_AMOUNT")).isEqualByComparingTo("17.35");
        });
    }

    private Tuple betTuple(long betId, long userId, long eventId, long marketId, long winnerId, String amount) {
        return Tuple.create()
                .set("BET_ID", betId)
                .set("USER_ID", userId)
                .set("EVENT_ID", eventId)
                .set("EVENT_MARKET_ID", marketId)
                .set("EVENT_WINNER_ID", winnerId)
                .set("BET_AMOUNT", new BigDecimal(amount));
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
